package net.sf.cpsolver.exam.heuristics;

import java.text.DecimalFormat;
import java.util.Enumeration;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.neighbours.ExamRandomMove;
import net.sf.cpsolver.exam.neighbours.ExamRoomMove;
import net.sf.cpsolver.exam.neighbours.ExamTimeMove;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;


import org.apache.log4j.Logger;

public class ExamSimulatedAnnealing implements NeighbourSelection, SolutionListener {
    private static Logger sLog = Logger.getLogger(ExamSimulatedAnnealing.class);
    protected static DecimalFormat sDF2 = new DecimalFormat("0.00");
    protected static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    protected static DecimalFormat sDF10 = new DecimalFormat("0.0000000000");
    private double iInitialTemperature = 1.0;
    private double iCoolingRate = 0.95;
    private double iReheatRate = -1;   
    private long iTemperatureLength = 0;
    private long iReheatLength = 0;
    private long iRestoreBestLength = 0;
    private double iTemperature = 0.0;
    private double iTempLengthCoef = 1.0;
    private double iReheatLengthCoef = 5.0;
    private double iRestoreBestLengthCoef = -1;
    private long iIter = 0;
    private long iLastImprovingIter = 0;
    private long iLastReheatIter = 0;
    private long iLastCoolingIter = 0;
    private int iAcceptIter[] = new int[] {0,0,0};
    private boolean iStochasticHC = false;
    private int iMoves = 0;
    private double iAbsValue = 0;
    private long iT0 = -1;
    
    private NeighbourSelection[] iNeighbours = null;
    
    protected boolean iRelativeAcceptance = true;
    
    public ExamSimulatedAnnealing(DataProperties properties) {
        iInitialTemperature = properties.getPropertyDouble("SimulatedAnnealing.InitialTemperature", iInitialTemperature);
        iReheatRate = properties.getPropertyDouble("SimulatedAnnealing.ReheatRate", iReheatRate);
        iCoolingRate = properties.getPropertyDouble("SimulatedAnnealing.CoolingRate", iCoolingRate);
        iRelativeAcceptance = properties.getPropertyBoolean("SimulatedAnnealing.RelativeAcceptance", iRelativeAcceptance);
        iStochasticHC = properties.getPropertyBoolean("SimulatedAnnealing.StochasticHC", iStochasticHC);
        iTempLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.TempLengthCoef", iTempLengthCoef);
        iReheatLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.ReheatLengthCoef", iReheatLengthCoef);
        iRestoreBestLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.RestoreBestLengthCoef", iRestoreBestLengthCoef);
        if (iReheatRate<0) iReheatRate = Math.pow(1/iCoolingRate,iReheatLengthCoef*1.7);
        if (iRestoreBestLengthCoef<0) iRestoreBestLengthCoef = iReheatLengthCoef * iReheatLengthCoef;
        iNeighbours = new NeighbourSelection[] {
                new ExamRandomMove(properties),
                new ExamRoomMove(properties),
                new ExamTimeMove(properties)
        };
    }
    
    public void init(Solver solver) {
        iTemperature = iInitialTemperature;
        long tl = getTemperatureLength(solver);
        iTemperatureLength = Math.round(iTempLengthCoef*tl);
        iReheatLength = Math.round(iReheatLengthCoef*iTemperatureLength);
        iRestoreBestLength = Math.round(iRestoreBestLengthCoef*iTemperatureLength);
        solver.currentSolution().addSolutionListener(this);
        for (int i=0;i<iNeighbours.length;i++)
            iNeighbours[i].init(solver);
    }
    
    public long getTemperatureLength(Solver solver) {
        long len = 0;
        for (Enumeration e=solver.currentSolution().getModel().variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            len += exam.getStudents().size();
        }
        sLog.info("Temperature length "+len);
        return len;
    }
    
    public double getTemperature() {
        return iTemperature;
    }
    public void setTemperature(double temperature) {
        iTemperature = temperature;
    }
    public double getInitialTemperature() {
        return iInitialTemperature;
    }
    public void setInitialTemperature(int initialTemperature) {
        iInitialTemperature = initialTemperature;
    }
    public double getReheatRate() {
        return iReheatRate;
    }
    public void setReheatRate(int reheatRate) {
        iReheatRate = reheatRate;
    }
    public double getCoolingRate() {
        return iCoolingRate;
    }
    public void setCoolingRate(double coolingRate) {
        iCoolingRate = coolingRate;
    }
    public double getTempLengthCoef() {
        return iTempLengthCoef;
    }
    public void setTempLengthCoef(double tempLengthCoef) {
        iTempLengthCoef = tempLengthCoef;
    }
    public double getReheatLengthCoef() {
        return iReheatLengthCoef;
    }
    public void setReheatLengthCoef(double reheatLengthCoef) {
        iReheatLengthCoef = reheatLengthCoef;
    }
    public boolean isAcceptanceRelative() {
        return iRelativeAcceptance;
    }
    public void setAcceptance(boolean relative) {
        iRelativeAcceptance = relative;
    }
    public boolean isStochasticHC() {
        return iStochasticHC;
    }
    public boolean isSimulatedAnnealing() {
        return !iStochasticHC;
    }
    public void setStochasticHC(boolean stochasticHC) {
        iStochasticHC = stochasticHC;
    }
    
    protected void cool(Solution solution) {
        iTemperature *= iCoolingRate;
        sLog.info("Iter="+iIter/1000+"k, NonImpIter="+sDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+sDF2.format(1000.0*iIter/(System.currentTimeMillis()-iT0))+" it/s");
        sLog.info("Temperature decreased to "+sDF5.format(iTemperature)+" " +
                "(#moves="+iMoves+", rms(value)="+sDF2.format(Math.sqrt(iAbsValue/iMoves))+", "+
                "accept=-"+sDF2.format(100.0*iAcceptIter[0]/iTemperatureLength)+"/"+sDF2.format(100.0*iAcceptIter[1]/iTemperatureLength)+"/+"+sDF2.format(100.0*iAcceptIter[2]/iTemperatureLength)+"%, " +
                (prob(-1)<1.0?"p(-1)="+sDF2.format(100.0*prob(-1))+"%, ":"")+
                "p(+1)="+sDF2.format(100.0*prob(1))+"%, "+
                "p(+10)="+sDF5.format(100.0*prob(10))+"%)");
        iLastCoolingIter=iIter;
        iAcceptIter=new int[] {0,0,0};
        iMoves = 0; iAbsValue = 0;
    }
    
    protected void reheat(Solution solution) {
        iTemperature *= iReheatRate;
        sLog.info("Iter="+iIter/1000+"k, NonImpIter="+sDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+sDF2.format(1000.0*iIter/(System.currentTimeMillis()-iT0))+" it/s");
        sLog.info("Temperature increased to "+sDF5.format(iTemperature)+" "+
                (prob(-1)<1.0?"p(-1)="+sDF2.format(100.0*prob(-1))+"%, ":"")+
                "p(+1)="+sDF2.format(100.0*prob(1))+"%, "+
                "p(+10)="+sDF5.format(100.0*prob(10))+"%, "+
                "p(+100)="+sDF10.format(100.0*prob(100))+"%)");
        iLastReheatIter=iIter;
    }
    
    protected void restoreBest(Solution solution) {
        sLog.info("Best restored");
        iLastImprovingIter=iIter;
    }
    
    public Neighbour genMove(Solution solution) {
        while (true) {
            incIter(solution);
            NeighbourSelection ns = iNeighbours[ToolBox.random(iNeighbours.length)];
            Neighbour n = ns.selectNeighbour(solution);
            if (n!=null) return n;
        }
    }
    
    protected double prob(double value) {
        if (iStochasticHC)
            return 1.0 / (1.0 + Math.exp(value/iTemperature));
        else
            return (value<=0.0?1.0:Math.exp(-value/iTemperature));
    }
    
    protected boolean accept(Solution solution, Neighbour neighbour) {
        double value = (iRelativeAcceptance?neighbour.value():solution.getModel().getTotalValue()+neighbour.value()-solution.getBestValue());
        double prob = prob(value);
        if (prob>=1.0 || ToolBox.random()<prob) {
            iAcceptIter[neighbour.value()<0.0?0:neighbour.value()>0.0?2:1]++;
            return true;
        }
        return false;
    }
    
    protected void incIter(Solution solution) {
        if (iT0<0) iT0 = System.currentTimeMillis();
        iIter++;
        if (iIter>iLastImprovingIter+iRestoreBestLength) restoreBest(solution);
        if (iIter>Math.max(iLastReheatIter,iLastImprovingIter)+iReheatLength) reheat(solution);
        if (iIter>iLastCoolingIter+iTemperatureLength) cool(solution);
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        Neighbour neighbour = null;
        while ((neighbour=genMove(solution))!=null) {
            iMoves++; iAbsValue += neighbour.value() * neighbour.value();
            if (accept(solution,neighbour)) break;
        }
        return (neighbour==null?null:neighbour);
    }
    
    public void bestSaved(Solution solution) {
        iLastImprovingIter = iIter;
    }
    public void solutionUpdated(Solution solution) {}
    public void getInfo(Solution solution, java.util.Dictionary info) {}
    public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
    public void bestCleared(Solution solution) {}
    public void bestRestored(Solution solution){}    
    


}
