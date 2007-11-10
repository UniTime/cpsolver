package net.sf.cpsolver.exam.heuristics;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

public class ExamNeighbourSelection implements NeighbourSelection {
    private static Logger sLog = Logger.getLogger(ExamNeighbourSelection.class); 
    private ExamConstruction iCon = null;
    private ExamSimulatedAnnealing iSA = null;
    private ExamHillClimbing iHC = null;
    private int iPhase = -1;
    
    public ExamNeighbourSelection(DataProperties properties) {
        iCon = new ExamConstruction(properties);
        iSA = new ExamSimulatedAnnealing(properties);
        iHC = new ExamHillClimbing(properties);
    }
    
    public void init(Solver solver){
        iCon.init(solver);
        iSA.init(solver);
        iHC.init(solver);
    }

    public Neighbour selectNeighbour(Solution solution) {
        Neighbour n = null;
        switch (iPhase) {
            case -1 :
                iPhase++;
                sLog.info("***** construction phase *****");
            case 0 : 
                n = iCon.selectNeighbour(solution);
                if (n!=null) return n;
                iPhase++;
                /*
                sLog.info("***** hill climbing phase *****");
            case 1 : 
                n = iHC.selectNeighbour(solution);
                if (n!=null) return n;
                iPhase++;
                */
                sLog.info("***** simulated annealing phase *****");
            default :
                return iSA.selectNeighbour(solution);
        }
    }
    

}
