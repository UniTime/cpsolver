package net.sf.cpsolver.exam.heuristics;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
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
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Hill climber. In each iteration, one of the following three neighbourhoods is
 * selected first
 * <ul>
 * <li>random move ({@link ExamRandomMove})
 * <li>period swap ({@link ExamTimeMove})
 * <li>room swap ({@link ExamRoomMove})
 * </ul>
 * , then a neighbour is generated and it is accepted if its value
 * {@link ExamSimpleNeighbour#value()} is below or equal to zero. The search is
 * stopped after a given amount of idle iterations ( can be defined by problem
 * property HillClimber.MaxIdle). <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
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
public class ExamHillClimbing implements NeighbourSelection<Exam, ExamPlacement>, SolutionListener<Exam, ExamPlacement> {
    private NeighbourSelection<Exam, ExamPlacement>[] iNeighbours = null;
    private int iMaxIdleIters = 25000;
    private int iLastImprovingIter = 0;
    private double iBestValue = 0;
    private int iIter = 0;
    private Progress iProgress = null;
    private boolean iActive;
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
     */
    @SuppressWarnings("unchecked")
    public ExamHillClimbing(DataProperties properties, String name) {
        iMaxIdleIters = properties.getPropertyInt("HillClimber.MaxIdle", iMaxIdleIters);
        iNeighbours = new NeighbourSelection[] { new ExamRandomMove(properties), new ExamRoomMove(properties),
                new ExamTimeMove(properties) };
        iName = name;
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        solver.currentSolution().addSolutionListener(this);
        for (int i = 0; i < iNeighbours.length; i++)
            iNeighbours[i].init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iActive = false;
    }

    /**
     * Select one of the given neighbourhoods randomly, select neighbour, return
     * it if its value is below or equal to zero (continue with the next
     * selection otherwise). Return null when the given number of idle
     * iterations is reached.
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        if (!iActive) {
            iProgress.setPhase(iName + "...");
            iActive = true;
        }
        while (true) {
            iIter++;
            iProgress.setProgress(Math.round(100.0 * (iIter - iLastImprovingIter) / iMaxIdleIters));
            if (iIter - iLastImprovingIter >= iMaxIdleIters)
                break;
            NeighbourSelection<Exam, ExamPlacement> ns = iNeighbours[ToolBox.random(iNeighbours.length)];
            Neighbour<Exam, ExamPlacement> n = ns.selectNeighbour(solution);
            if (n != null && n.value() <= 0)
                return n;
        }
        iIter = 0;
        iLastImprovingIter = 0;
        iActive = false;
        return null;
    }

    /**
     * Memorize the iteration when the last best solution was found.
     */
    @Override
    public void bestSaved(Solution<Exam, ExamPlacement> solution) {
        if (Math.abs(iBestValue - solution.getBestValue()) >= 1.0) {
            iLastImprovingIter = iIter;
            iBestValue = solution.getBestValue();
        }
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
}
