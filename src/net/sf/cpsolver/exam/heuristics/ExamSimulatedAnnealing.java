package net.sf.cpsolver.exam.heuristics;

import java.text.DecimalFormat;

import net.sf.cpsolver.exam.neighbours.ExamRandomMove;
import net.sf.cpsolver.exam.neighbours.ExamRoomMove;
import net.sf.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import net.sf.cpsolver.exam.neighbours.ExamTimeMove;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;


import org.apache.log4j.Logger;

/**
 * Simulated annealing. In each iteration, one of the following three neighbourhoods is selected first
 * <ul>
 * <li>random move ({@link ExamRandomMove})
 * <li>period swap ({@link ExamTimeMove})
 * <li>room swap ({@link ExamRoomMove})
 * </ul>,
 * then a neighbour is generated and it is accepted with probability {@link ExamSimulatedAnnealing#prob(double)}.
 * The search is guided by the temperature, which starts at <i>SimulatedAnnealing.InitialTemperature</i>. 
 * After each <i>SimulatedAnnealing.TemperatureLength</i> iterations, the temperature is reduced 
 * by <i>SimulatedAnnealing.CoolingRate</i>. If there was no improvement in the past 
 * <i>SimulatedAnnealing.ReheatLengthCoef * SimulatedAnnealing.TemperatureLength</i> iterations, 
 * the temperature is increased by <i>SimulatedAnnealing.ReheatRate</i>.
 * If there was no improvement in the past 
 * <i>SimulatedAnnealing.RestoreBestLengthCoef * SimulatedAnnealing.TemperatureLength</i> iterations,
 * the best ever found solution is restored.
 * <br><br>
 * If <i>SimulatedAnnealing.StochasticHC</i> is true, the acceptance probability is computed using
 * stochastic hill climbing criterion, i.e., <code>1.0 / (1.0 + Math.exp(value/temperature))</code>,
 * otherwise it is cumputed using simlated annealing criterion, i.e.,
 * <code>(value<=0.0?1.0:Math.exp(-value/temperature))</code>.
 * If <i>SimulatedAnnealing.RelativeAcceptance</i> neighbour value {@link ExamSimpleNeighbour#value()} is taken
 * as the value of the selected neighbour (difference between the new and the current solution, if the
 * neighbour is accepted), otherwise the value is computed as the difference
 * between the value of the current solution if the neighbour is accepted and the best ever found solution.
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ExamSimulatedAnnealing implements NeighbourSelection, SolutionListener {
    private static Logger sLog = Logger.getLogger(ExamSimulatedAnnealing.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private static DecimalFormat sDF10 = new DecimalFormat("0.0000000000");
    private double iInitialTemperature = 1.5;
    private double iCoolingRate = 0.95;
    private double iReheatRate = -1;   
    private long iTemperatureLength = 250000;
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
    private double iBestValue = 0;
    
    private NeighbourSelection[] iNeighbours = null;
    
    private boolean iRelativeAcceptance = true;
    
    /**
     * Constructor. Following problem properties are considered:
     * <ul>
     * <li>SimulatedAnnealing.InitialTemperature ... initial temperature (default 1.5)
     * <li>SimulatedAnnealing.TemperatureLength ... temperature length (number of iterations between temperature decrements, default 25000)
     * <li>SimulatedAnnealing.CoolingRate ... temperature cooling rate (default 0.95)
     * <li>SimulatedAnnealing.ReheatLengthCoef ... temperature re-heat length coefficient (multiple of temperature length, default 5)
     * <li>SimulatedAnnealing.ReheatRate ... temperature re-heating rate (default (1/coolingRate)^(reheatLengthCoef*1.7))
     * <li>SimulatedAnnealing.RestoreBestLengthCoef ... restore best length coefficient (multiple of re-heat length, default reheatLengthCoef^2)
     * <li>SimulatedAnnealing.StochasticHC ... true for stochastic search acceptance criterion, false for simulated annealing acceptance (default false)
     * <li>SimulatedAnnealing.RelativeAcceptance ... true for relative acceptance (different between the new and the current solutions, if the neighbour is accepted), false for absolute acceptance (difference between the new and the best solutions, if the neighbour is accepted)
     * </ul>
     * @param properties problem properties
     */
    public ExamSimulatedAnnealing(DataProperties properties) {
        iInitialTemperature = properties.getPropertyDouble("SimulatedAnnealing.InitialTemperature", iInitialTemperature);
        iReheatRate = properties.getPropertyDouble("SimulatedAnnealing.ReheatRate", iReheatRate);
        iCoolingRate = properties.getPropertyDouble("SimulatedAnnealing.CoolingRate", iCoolingRate);
        iRelativeAcceptance = properties.getPropertyBoolean("SimulatedAnnealing.RelativeAcceptance", iRelativeAcceptance);
        iStochasticHC = properties.getPropertyBoolean("SimulatedAnnealing.StochasticHC", iStochasticHC);
        iTemperatureLength = properties.getPropertyLong("SimulatedAnnealing.TemperatureLength", iTemperatureLength);
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
    
    /**
     * Initialization
     */
    public void init(Solver solver) {
        iTemperature = iInitialTemperature;
        iReheatLength = Math.round(iReheatLengthCoef*iTemperatureLength);
        iRestoreBestLength = Math.round(iRestoreBestLengthCoef*iTemperatureLength);
        solver.currentSolution().addSolutionListener(this);
        for (int i=0;i<iNeighbours.length;i++)
            iNeighbours[i].init(solver);
    }
    
    /**
     * Cool temperature
     */
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
    
    /**
     * Reheat temperature
     */
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
    
    /**
     * restore best ever found solution 
     */
    protected void restoreBest(Solution solution) {
        sLog.info("Best restored");
        iLastImprovingIter=iIter;
    }
    
    /**
     * Generate neighbour -- select neighbourhood randomly, select neighbour
     */
    public Neighbour genMove(Solution solution) {
        while (true) {
            incIter(solution);
            NeighbourSelection ns = iNeighbours[ToolBox.random(iNeighbours.length)];
            Neighbour n = ns.selectNeighbour(solution);
            if (n!=null) return n;
        }
    }
    
    /**
     * Neighbour acceptance probability
     * @param value absolute or relative value of the proposed change (neighbour)
     * @return probability of acceptance of a change (neighbour)
     */
    protected double prob(double value) {
        if (iStochasticHC)
            return 1.0 / (1.0 + Math.exp(value/iTemperature));
        else
            return (value<=0.0?1.0:Math.exp(-value/iTemperature));
    }
    
    /**
     * True if the given neighboir is to be be accepted
     * @param solution current solution
     * @param neighbour proposed move
     * @return true if generated random number is below {@link ExamSimulatedAnnealing#prob(double)}
     */
    protected boolean accept(Solution solution, Neighbour neighbour) {
        double value = (iRelativeAcceptance?neighbour.value():solution.getModel().getTotalValue()+neighbour.value()-solution.getBestValue());
        double prob = prob(value);
        if (prob>=1.0 || ToolBox.random()<prob) {
            iAcceptIter[neighbour.value()<0.0?0:neighbour.value()>0.0?2:1]++;
            return true;
        }
        return false;
    }
    
    /**
     * Increment iteration counter, cool/reheat/restoreBest if necessary
     */
    protected void incIter(Solution solution) {
        if (iT0<0) iT0 = System.currentTimeMillis();
        iIter++;
        if (iIter>iLastImprovingIter+iRestoreBestLength) restoreBest(solution);
        if (iIter>Math.max(iLastReheatIter,iLastImprovingIter)+iReheatLength) reheat(solution);
        if (iIter>iLastCoolingIter+iTemperatureLength) cool(solution);
    }
    
    /**
     * Select neighbour -- generate a move {@link ExamSimulatedAnnealing#genMove(Solution)} until an acceptable neighbour
     * is found {@link ExamSimulatedAnnealing#accept(Solution, Neighbour)}, keep increasing iteration 
     * {@link ExamSimulatedAnnealing#incIter(Solution)}. 
     */
    public Neighbour selectNeighbour(Solution solution) {
        Neighbour neighbour = null;
        while ((neighbour=genMove(solution))!=null) {
            iMoves++; iAbsValue += neighbour.value() * neighbour.value();
            if (accept(solution,neighbour)) break;
        }
        return (neighbour==null?null:neighbour);
    }
    
    /**
     * Memorize the iteration when the last best solution was found.
     */
    public void bestSaved(Solution solution) {
        if (Math.abs(iBestValue-solution.getBestValue())>=1.0) {
            iLastImprovingIter = iIter;
            iBestValue = solution.getBestValue();
        }
        iLastImprovingIter = iIter;
    }
    public void solutionUpdated(Solution solution) {}
    public void getInfo(Solution solution, java.util.Dictionary info) {}
    public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
    public void bestCleared(Solution solution) {}
    public void bestRestored(Solution solution){}    
    


}
