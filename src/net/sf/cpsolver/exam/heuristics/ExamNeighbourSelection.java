package net.sf.cpsolver.exam.heuristics;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.termination.TerminationCondition;
import net.sf.cpsolver.ifs.util.Callback;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * Examination timetabling neighbour selection. <br>
 * <br>
 * It consists of the following three phases:
 * <ul>
 * <li>Construction phase ({@link ExamConstruction} until all exams are
 * assigned)
 * <li>Hill-climbing phase ({@link ExamHillClimbing} until the given number if
 * idle iterations)
 * <li>Simulated annealing phase ({@link ExamSimulatedAnnealing} until timeout
 * is reached)
 * <ul>
 * <li>Or greate deluge phase (when Exam.GreatDeluge is true,
 * {@link ExamGreatDeluge} until timeout is reached)
 * </ul>
 * <li>At the end (when {@link TerminationCondition#canContinue(Solution)} is
 * false), the search is finished with one sweep of final phase (
 * {@link ExamHillClimbing} until the given number if idle iterations).
 * </ul>
 * <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class ExamNeighbourSelection implements NeighbourSelection<Exam, ExamPlacement>,
        TerminationCondition<Exam, ExamPlacement> {
    private static Logger sLog = Logger.getLogger(ExamNeighbourSelection.class);
    private ExamColoringConstruction iColor = null;
    private ExamConstruction iCon = null;
    private StandardNeighbourSelection<Exam, ExamPlacement> iStd = null;
    private ExamSimulatedAnnealing iSA = null;
    private ExamHillClimbing iHC = null;
    private ExamHillClimbing iFin = null;
    private ExamGreatDeluge iGD = null;
    private int iPhase = -1;
    private boolean iUseGD = false;
    private Progress iProgress = null;
    private Callback iFinalPhaseFinished = null;
    private boolean iCanContinue = true;
    private TerminationCondition<Exam, ExamPlacement> iTerm = null;

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties
     */
    public ExamNeighbourSelection(DataProperties properties) {
        if (properties.getPropertyBoolean("Exam.ColoringConstruction", true))
            iColor = new ExamColoringConstruction(properties);
        iCon = new ExamConstruction(properties);
        try {
            iStd = new StandardNeighbourSelection<Exam, ExamPlacement>(properties);
            iStd.setVariableSelection(new ExamUnassignedVariableSelection(properties));
            iStd.setValueSelection(new ExamTabuSearch(properties));
        } catch (Exception e) {
            sLog.error("Unable to initialize standard selection, reason: " + e.getMessage(), e);
            iStd = null;
        }
        iSA = new ExamSimulatedAnnealing(properties);
        iHC = new ExamHillClimbing(properties, "Hill Climbing");
        iFin = new ExamHillClimbing(properties, "Finalization");
        iGD = new ExamGreatDeluge(properties);
        iUseGD = properties.getPropertyBoolean("Exam.GreatDeluge", iUseGD);
    }

    /**
     * Initialization
     */
    public void init(Solver<Exam, ExamPlacement> solver) {
        if (iColor != null)
            iColor.init(solver);
        iCon.init(solver);
        iStd.init(solver);
        iSA.init(solver);
        iHC.init(solver);
        iFin.init(solver);
        iGD.init(solver);
        if (iTerm == null) {
            iTerm = solver.getTerminationCondition();
            solver.setTerminalCondition(this);
        }
        iCanContinue = true;
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
    }

    /**
     * Neighbour selection. It consists of the following three phases:
     * <ul>
     * <li>Construction phase ({@link ExamConstruction} until all exams are
     * assigned)
     * <li>Hill-climbing phase ({@link ExamHillClimbing} until the given number
     * if idle iterations)
     * <li>Simulated annealing phase ({@link ExamSimulatedAnnealing} until
     * timeout is reached)
     * </ul>
     */
    @SuppressWarnings("fallthrough")
    public synchronized Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        Neighbour<Exam, ExamPlacement> n = null;
        if (!isFinalPhase() && !iTerm.canContinue(solution))
            setFinalPhase(null);
        switch (iPhase) {
            case -1:
                iPhase++;
                sLog.info("***** construction phase *****");
                if (iColor != null) {
                    n = iColor.selectNeighbour(solution);
                    if (n != null) return n;
                }
            case 0:
                n = iCon.selectNeighbour(solution);
                if (n != null)
                    return n;
                if (solution.getModel().nrUnassignedVariables() > 0)
                    iProgress.setPhase("Searching for initial solution...", solution.getModel().variables().size());
                iPhase++;
                sLog.info("***** cbs/tabu-search phase *****");
            case 1:
                if (iStd != null && solution.getModel().nrUnassignedVariables() > 0) {
                    iProgress.setProgress(solution.getModel().variables().size()
                            - solution.getModel().getBestUnassignedVariables());
                    n = iStd.selectNeighbour(solution);
                    if (n != null)
                        return n;
                }
                iPhase++;
                sLog.info("***** hill climbing phase *****");
            case 2:
                n = iHC.selectNeighbour(solution);
                if (n != null)
                    return n;
                iPhase++;
                sLog.info("***** " + (iUseGD ? "great deluge" : "simulated annealing") + " phase *****");
            case 3:
                if (iUseGD)
                    return iGD.selectNeighbour(solution);
                else
                    return iSA.selectNeighbour(solution);
            case 9999:
                n = iFin.selectNeighbour(solution);
                if (n != null)
                    return n;
                iPhase = -1;
                if (iFinalPhaseFinished != null)
                    iFinalPhaseFinished.execute();
                iCanContinue = false;
            default:
                return null;
        }
    }

    /**
     * Set final phase
     * 
     * @param finalPhaseFinished
     *            to be called when the final phase is finished
     **/
    public synchronized void setFinalPhase(Callback finalPhaseFinished) {
        sLog.info("***** final phase *****");
        iFinalPhaseFinished = finalPhaseFinished;
        iPhase = 9999;
    }

    /** Is final phase */
    public boolean isFinalPhase() {
        return iPhase == 9999;
    }

    /** Termination condition (i.e., has final phase finished) */
    public boolean canContinue(Solution<Exam, ExamPlacement> currentSolution) {
        return iCanContinue;
    }

}
