package net.sf.cpsolver.exam.heuristics;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * Examination timetabling neighbour selection.
 * <br><br>
 * It consists of the following three phases:
 * <ul>
 * <li>Construction phase ({@link ExamConstruction} until all exams are assigned) 
 * <li>Hill-climbing phase ({@link ExamHillClimbing} until the given number if idle iterations)
 * <li>Simulated annealing phase ({@link ExamSimulatedAnnealing} until timeout is reached)
 * <ul>
 *      <li>Or greate deluge phase (when Exam.GreatDeluge is true,{@link ExamGreatDeluge} until timeout is reached)
 * </ul>
 * </ul>
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
public class ExamNeighbourSelection implements NeighbourSelection {
    private static Logger sLog = Logger.getLogger(ExamNeighbourSelection.class); 
    private ExamConstruction iCon = null;
    private StandardNeighbourSelection iStd = null;
    private ExamSimulatedAnnealing iSA = null;
    private ExamHillClimbing iHC = null;
    private ExamGreatDeluge iGD = null;
    private int iPhase = -1;
    private boolean iUseGD = false;
    private Progress iProgress = null;
    
    /**
     * Constructor
     * @param properties problem properties
     */
    public ExamNeighbourSelection(DataProperties properties) {
        iCon = new ExamConstruction(properties);
        try {
            iStd = new StandardNeighbourSelection(properties);
            iStd.setVariableSelection(new ExamUnassignedVariableSelection(properties));
            iStd.setValueSelection(new ExamTabuSearch(properties));
        } catch (Exception e) {
            sLog.error("Unable to initialize standard selection, reason: "+e.getMessage(),e);
            iStd = null;
        }
        iSA = new ExamSimulatedAnnealing(properties);
        iHC = new ExamHillClimbing(properties);
        iGD = new ExamGreatDeluge(properties);
        iUseGD = properties.getPropertyBoolean("Exam.GreatDeluge", iUseGD);
    }
    
    /**
     * Initialization
     */
    public void init(Solver solver){
        iCon.init(solver);
        iStd.init(solver);
        iSA.init(solver);
        iHC.init(solver);
        iGD.init(solver);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
    }

    /**
     * Neighbour selection. It consists of the following three phases:
     * <ul>
     * <li>Construction phase ({@link ExamConstruction} until all exams are assigned) 
     * <li>Hill-climbing phase ({@link ExamHillClimbing} until the given number if idle iterations)
     * <li>Simulated annealing phase ({@link ExamSimulatedAnnealing} until timeout is reached)
     * </ul>
     */
    public Neighbour selectNeighbour(Solution solution) {
        Neighbour n = null;
        switch (iPhase) {
            case -1 :
                iPhase++;
                sLog.info("***** construction phase *****");
            case 0 : 
                n = iCon.selectNeighbour(solution);
                if (n!=null) return n;
                if (solution.getModel().nrUnassignedVariables()>0) 
                    iProgress.setPhase("Searching for initial solution...", solution.getModel().variables().size());
                iPhase++;
                sLog.info("***** cbs/tabu-search phase *****");
            case 1 :
                if (iStd!=null && solution.getModel().nrUnassignedVariables()>0) {
                    iProgress.setProgress(solution.getModel().variables().size()-solution.getModel().getBestUnassignedVariables());
                    n = iStd.selectNeighbour(solution);
                    if (n!=null) return n;
                }
                iPhase++;
                sLog.info("***** hill climbing phase *****");
            case 2 :
                n = iHC.selectNeighbour(solution);
                if (n!=null) return n;
                iPhase++;
                sLog.info("***** "+(iUseGD?"great deluge":"simulated annealing")+" phase *****");
            default :
                if (iUseGD)
                    return iGD.selectNeighbour(solution);
                else
                    return iSA.selectNeighbour(solution);
        }
    }
    

}
