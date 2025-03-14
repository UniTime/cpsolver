package org.cpsolver.exam.heuristics;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


import org.apache.logging.log4j.Logger;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.neighbours.ExamRandomMove;
import org.cpsolver.exam.neighbours.ExamRoomMove;
import org.cpsolver.exam.neighbours.ExamTimeMove;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.LazyNeighbour;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.LazyNeighbour.LazyNeighbourAcceptanceCriterion;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;

/**
 * Greate deluge. In each iteration, one of the following three neighbourhoods
 * is selected first
 * <ul>
 * <li>random move ({@link ExamRandomMove})
 * <li>period swap ({@link ExamTimeMove})
 * <li>room swap ({@link ExamRoomMove})
 * </ul>
 * , then a neighbour is generated and it is accepted if the value of the new
 * solution is below certain bound. This bound is initialized to the
 * GreatDeluge.UpperBoundRate &times; value of the best solution ever found.
 * After each iteration, the bound is decreased by GreatDeluge.CoolRate (new
 * bound equals to old bound &times; GreatDeluge.CoolRate). If the bound gets
 * bellow GreatDeluge.LowerBoundRate &times; value of the best solution ever
 * found, it is changed back to GreatDeluge.UpperBoundRate &times; value of the
 * best solution ever found.
 * 
 * If there was no improvement found between the bounds, the new bounds are
 * changed to GreatDeluge.UpperBoundRate^2 and GreatDeluge.LowerBoundRate^2,
 * GreatDeluge.UpperBoundRate^3 and GreatDeluge.LowerBoundRate^3, etc. till
 * there is an improvement found. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
@Deprecated
public class ExamGreatDeluge extends NeighbourSelectionWithContext<Exam, ExamPlacement, ExamGreatDeluge.Context> implements SolutionListener<Exam, ExamPlacement>, LazyNeighbourAcceptanceCriterion<Exam, ExamPlacement> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ExamGreatDeluge.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private double iCoolRate = 0.99999995;
    private double iUpperBoundRate = 1.05;
    private double iLowerBoundRate = 0.95;
    private Progress iProgress = null;

    private List<NeighbourSelection<Exam, ExamPlacement>> iNeighbours = null;

    /**
     * Constructor. Following problem properties are considered:
     * <ul>
     * <li>GreatDeluge.CoolRate ... bound cooling rate (default 0.99999995)
     * <li>GreatDeluge.UpperBoundRate ... bound upper bound relative to best
     * solution ever found (default 1.05)
     * <li>GreatDeluge.LowerBoundRate ... bound lower bound relative to best
     * solution ever found (default 0.95)
     * </ul>
     * 
     * @param properties
     *            problem properties
     */
    @SuppressWarnings("unchecked")
    public ExamGreatDeluge(DataProperties properties) {
        iCoolRate = properties.getPropertyDouble("GreatDeluge.CoolRate", iCoolRate);
        iUpperBoundRate = properties.getPropertyDouble("GreatDeluge.UpperBoundRate", iUpperBoundRate);
        iLowerBoundRate = properties.getPropertyDouble("GreatDeluge.LowerBoundRate", iLowerBoundRate);
        String neighbours = properties.getProperty("GreatDeluge.Neighbours", 
                ExamRandomMove.class.getName() + ";" +
                ExamRoomMove.class.getName() + ";" +
                ExamTimeMove.class.getName());
        neighbours += ";" + properties.getProperty("GreatDeluge.AdditionalNeighbours", "");
        iNeighbours = new ArrayList<NeighbourSelection<Exam,ExamPlacement>>();
        for (String neighbour: neighbours.split("\\;")) {
            if (neighbour == null || neighbour.isEmpty()) continue;
            try {
                Class<NeighbourSelection<Exam, ExamPlacement>> clazz = (Class<NeighbourSelection<Exam, ExamPlacement>>)Class.forName(neighbour);
                iNeighbours.add(clazz.getConstructor(DataProperties.class).newInstance(properties));
            } catch (Exception e) {
                sLog.error("Unable to use " + neighbour + ": " + e.getMessage());
            }
        }
    }

    /** Initialization */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        super.init(solver);
        solver.currentSolution().addSolutionListener(this);
        for (NeighbourSelection<Exam, ExamPlacement> neighbour: iNeighbours)
            neighbour.init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        getContext(solver.currentSolution().getAssignment()).reset();
    }

    /** Print some information 
     * @param solution current solution
     **/
    protected void info(Solution<Exam, ExamPlacement> solution) {
        Assignment<Exam, ExamPlacement> assignment = solution.getAssignment();
        getContext(assignment).info(solution);
    }

    /**
     * Generate neighbour -- select neighbourhood randomly, select neighbour
     * @param solution current solution
     * @return a neigbour that was selected
     */
    public Neighbour<Exam, ExamPlacement> genMove(Solution<Exam, ExamPlacement> solution) {
        while (true) {
            incIter(solution);
            NeighbourSelection<Exam, ExamPlacement> ns = iNeighbours.get(ToolBox.random(iNeighbours.size()));
            Neighbour<Exam, ExamPlacement> n = ns.selectNeighbour(solution);
            if (n != null)
                return n;
        }
    }

    /** Accept neighbour 
     * @param solution current solution
     * @param neighbour a neighbour in question
     * @return true if the neighbour should be accepted
     **/
    protected boolean accept(Solution<Exam, ExamPlacement> solution, Neighbour<Exam, ExamPlacement> neighbour) {
        if (neighbour instanceof LazyNeighbour) {
            ((LazyNeighbour<Exam, ExamPlacement>)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        Assignment<Exam, ExamPlacement> assignment = solution.getAssignment();
        return (neighbour.value(assignment) <= 0 || solution.getModel().getTotalValue(assignment) + neighbour.value(assignment) <= getContext(assignment).getBound());
    }
    
    /** Accept lazy neighbour */
    @Override
    public boolean accept(Assignment<Exam, ExamPlacement> assignment, LazyNeighbour<Exam, ExamPlacement> neighbour, double value) {
        return (value <= 0.0 || neighbour.getModel().getTotalValue(assignment) <= getContext(assignment).getBound());
    }

    /** Increment iteration count, update bound 
     * @param solution current solution
     **/
    protected void incIter(Solution<Exam, ExamPlacement> solution) {
        getContext(solution.getAssignment()).incIter(solution);
    }

    /**
     * A neighbour is generated randomly untill an acceptable one is found.
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        Neighbour<Exam, ExamPlacement> neighbour = null;
        while ((neighbour = genMove(solution)) != null) {
            getContext(solution.getAssignment()).incMoves();
            if (accept(solution, neighbour)) {
                getContext(solution.getAssignment()).incAcceptedMoves();
                break;
            }
        }
        return (neighbour == null ? null : neighbour);
    }

    /** Update last improving iteration count */
    @Override
    public void bestSaved(Solution<Exam, ExamPlacement> solution) {
        getContext(solution.getAssignment()).bestSaved(solution.getModel());
    }

    @Override
    public void solutionUpdated(Solution<Exam, ExamPlacement> solution) {
    }

    @Override
    public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info) {
    }

    @Override
    public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info, Collection<Exam> variables) {
    }

    @Override
    public void bestCleared(Solution<Exam, ExamPlacement> solution) {
    }

    @Override
    public void bestRestored(Solution<Exam, ExamPlacement> solution) {
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new Context();
    }
    
    public class Context implements AssignmentContext {
        private double iUpperBound;
        private int iMoves = 0;
        private int iAcceptedMoves = 0;
        private int iNrIdle = 0;
        private long iT0 = -1;
        private long iIter = -1;
        private double iBound = 0.0;
        private long iLastImprovingIter = 0;
        private double iBestValue = 0;
        
        protected void reset() {
            iIter = -1;
        }

        protected void incIter(Solution<Exam, ExamPlacement> solution) {
            double best = solution.getModel().getBestValue();
            if (iIter < 0) {
                iIter = 0;
                iLastImprovingIter = 0;
                iT0 = JProf.currentTimeMillis();
                iBound = (best > 0.0 ? iUpperBoundRate * best : best / iUpperBoundRate);
                iUpperBound = iBound;
                iNrIdle = 0;
                solution.restoreBest();
                iProgress.setPhase("Great deluge [" + (1 + iNrIdle) + "]...");
            } else {
                iIter++;
                if (best >= 0.0)
                    iBound *= iCoolRate;
                else
                    iBound /= iCoolRate;
            }
            if (iIter % 1000 == 0 && iBound > (best > 0.0 ? iUpperBoundRate * best : best / iUpperBoundRate)) {
                // some other thread lowered the upper bound over my bound -> adjust my bound
                iBound = (best > 0.0 ? iUpperBoundRate * best : best / iUpperBoundRate);
            }
            if (iIter % 100000 == 0) {
                info(solution);
            }
            double lowerBound = (best >= 0.0 ? Math.pow(iLowerBoundRate, 1 + iNrIdle) * best : best / Math.pow(iLowerBoundRate, 1 + iNrIdle));
            if (iBound < lowerBound) {
                iNrIdle++;
                sLog.info(" -<[" + iNrIdle + "]>- ");
                iBound = Math.max(best + 2.0, (best >= 0.0 ? Math.pow(iUpperBoundRate, iNrIdle) * best : best / Math.pow(iUpperBoundRate, iNrIdle)));
                iUpperBound = iBound;
                iProgress.setPhase("Great deluge [" + (1 + iNrIdle) + "]...");
                solution.restoreBest();
            }
            iProgress.setProgress(100 - Math.round(100.0 * (iBound - lowerBound) / (iUpperBound - lowerBound)));
        }
        
        protected void info(Solution<Exam, ExamPlacement> solution) {
            sLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                    + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
            sLog.info("Bound is " + sDF2.format(iBound) + ", " + "best value is " + sDF2.format(solution.getModel().getBestValue())
                    + " (" + sDF2.format(100.0 * iBound / solution.getModel().getBestValue()) + "%), " + "current value is "
                    + sDF2.format(solution.getModel().getTotalValue(solution.getAssignment())) + " ("
                    + sDF2.format(100.0 * iBound / solution.getModel().getTotalValue(solution.getAssignment())) + "%), " + "#idle=" + iNrIdle
                    + ", " + "Pacc=" + sDF5.format(100.0 * iAcceptedMoves / iMoves) + "%");
            iAcceptedMoves = iMoves = 0;
        }
        
        protected void bestSaved(Model<Exam, ExamPlacement> model) {
            if (Math.abs(iBestValue - model.getBestValue()) >= 1.0) {
                iLastImprovingIter = iIter;
                iNrIdle = 0;
                iBestValue = model.getBestValue();
            }
        }
        
        public double getBound() { return iBound; }
        
        public void incMoves() { iMoves ++; }
        
        public void incAcceptedMoves() { iAcceptedMoves ++; }
    }
}
