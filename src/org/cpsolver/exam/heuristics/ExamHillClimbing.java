package org.cpsolver.exam.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.neighbours.ExamRandomMove;
import org.cpsolver.exam.neighbours.ExamRoomMove;
import org.cpsolver.exam.neighbours.ExamSimpleNeighbour;
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
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Hill climber. In each iteration, one of the following three neighbourhoods is
 * selected first
 * <ul>
 * <li>random move ({@link ExamRandomMove})
 * <li>period swap ({@link ExamTimeMove})
 * <li>room swap ({@link ExamRoomMove})
 * </ul>
 * , then a neighbour is generated and it is accepted if its value
 * {@link ExamSimpleNeighbour#value(Assignment)} is below or equal to zero. The search is
 * stopped after a given amount of idle iterations ( can be defined by problem
 * property HillClimber.MaxIdle). <br>
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
public class ExamHillClimbing extends NeighbourSelectionWithContext<Exam, ExamPlacement, ExamHillClimbing.Context> implements SolutionListener<Exam, ExamPlacement>, LazyNeighbourAcceptanceCriterion<Exam, ExamPlacement> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ExamHillClimbing.class);
    private List<NeighbourSelection<Exam, ExamPlacement>> iNeighbours = null;
    private int iMaxIdleIters = 25000;
    private Progress iProgress = null;
    private String iName = "Hill climbing";

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties (use HillClimber.MaxIdle to set maximum
     *            number of idle iterations)
     */
    public ExamHillClimbing(DataProperties properties) {
        this(properties, "Hill Climbing");
    }

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties (use HillClimber.MaxIdle to set maximum
     *            number of idle iterations)
     * @param name solver search phase name
     */
    @SuppressWarnings("unchecked")
    public ExamHillClimbing(DataProperties properties, String name) {
        iMaxIdleIters = properties.getPropertyInt("HillClimber.MaxIdle", iMaxIdleIters);
        String neighbours = properties.getProperty("HillClimber.Neighbours", 
                ExamRandomMove.class.getName() + ";" +
                ExamRoomMove.class.getName() + ";" +
                ExamTimeMove.class.getName());
        neighbours += ";" + properties.getProperty("HillClimber.AdditionalNeighbours", "");
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
        iName = name;
    }

    /**
     * Initialization
     */
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

    /**
     * Select one of the given neighbourhoods randomly, select neighbour, return
     * it if its value is below or equal to zero (continue with the next
     * selection otherwise). Return null when the given number of idle
     * iterations is reached.
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        Context context = getContext(solution.getAssignment());
        context.activateIfNeeded();
        while (true) {
            if (context.incIter(solution)) break;
            NeighbourSelection<Exam, ExamPlacement> ns = iNeighbours.get(ToolBox.random(iNeighbours.size()));
            Neighbour<Exam, ExamPlacement> n = ns.selectNeighbour(solution);
            if (n != null) {
                if (n instanceof LazyNeighbour) {
                    ((LazyNeighbour<Exam,ExamPlacement>)n).setAcceptanceCriterion(this);
                    return n;
                } else if (n.value(solution.getAssignment()) <= 0.0) return n;
            }
        }
        context.reset();
        return null;
    }

    /**
     * Memorize the iteration when the last best solution was found.
     */
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

    /** Accept lazy neighbour */
    @Override
    public boolean accept(Assignment<Exam, ExamPlacement> assignment, LazyNeighbour<Exam, ExamPlacement> neighbour, double value) {
        return value <= 0.0;
    }

    @Override
    public Context createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new Context();
    }

    public class Context implements AssignmentContext {
        private int iLastImprovingIter = 0;
        private double iBestValue = 0;
        private int iIter = 0;
        private boolean iActive;

        protected void reset() {
            iIter = 0;
            iLastImprovingIter = 0;
            iActive = false;
        }

        protected void activateIfNeeded() {
            if (!iActive) {
                iProgress.setPhase(iName + "...");
                iActive = true;
            }
        }
        
        protected boolean incIter(Solution<Exam, ExamPlacement> solution) {
            iIter++;
            iProgress.setProgress(Math.round(100.0 * (iIter - iLastImprovingIter) / iMaxIdleIters));
            if (iIter - iLastImprovingIter >= iMaxIdleIters) return true;
            return false;
        }
        
        protected void bestSaved(Model<Exam, ExamPlacement> model) {
            if (Math.abs(iBestValue - model.getBestValue()) >= 1.0) {
                iLastImprovingIter = iIter;
                iBestValue = model.getBestValue();
            }
        }
    }
}
