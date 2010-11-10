package net.sf.cpsolver.exam.heuristics;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.neighbours.ExamRandomMove;
import net.sf.cpsolver.exam.neighbours.ExamRoomMove;
import net.sf.cpsolver.exam.neighbours.ExamTimeMove;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.apache.log4j.Logger;

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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class ExamGreatDeluge implements NeighbourSelection<Exam, ExamPlacement>, SolutionListener<Exam, ExamPlacement> {
    private static Logger sLog = Logger.getLogger(ExamGreatDeluge.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private double iBound = 0.0;
    private double iCoolRate = 0.99999995;
    private long iIter;
    private double iUpperBound;
    private double iUpperBoundRate = 1.05;
    private double iLowerBoundRate = 0.95;
    private int iMoves = 0;
    private int iAcceptedMoves = 0;
    private int iNrIdle = 0;
    private long iT0 = -1;
    private long iLastImprovingIter = 0;
    private double iBestValue = 0;
    private Progress iProgress = null;

    private NeighbourSelection<Exam, ExamPlacement>[] iNeighbours = null;

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
        iNeighbours = new NeighbourSelection[] { new ExamRandomMove(properties), new ExamRoomMove(properties),
                new ExamTimeMove(properties) };
    }

    /** Initialization */
    public void init(Solver<Exam, ExamPlacement> solver) {
        iIter = -1;
        solver.currentSolution().addSolutionListener(this);
        for (int i = 0; i < iNeighbours.length; i++)
            iNeighbours[i].init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
    }

    /** Print some information */
    protected void info(Solution<Exam, ExamPlacement> solution) {
        sLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
        sLog.info("Bound is " + sDF2.format(iBound) + ", " + "best value is " + sDF2.format(solution.getBestValue())
                + " (" + sDF2.format(100.0 * iBound / solution.getBestValue()) + "%), " + "current value is "
                + sDF2.format(solution.getModel().getTotalValue()) + " ("
                + sDF2.format(100.0 * iBound / solution.getModel().getTotalValue()) + "%), " + "#idle=" + iNrIdle
                + ", " + "Pacc=" + sDF5.format(100.0 * iAcceptedMoves / iMoves) + "%");
        iAcceptedMoves = iMoves = 0;
    }

    /**
     * Generate neighbour -- select neighbourhood randomly, select neighbour
     */
    public Neighbour<Exam, ExamPlacement> genMove(Solution<Exam, ExamPlacement> solution) {
        while (true) {
            incIter(solution);
            NeighbourSelection<Exam, ExamPlacement> ns = iNeighbours[ToolBox.random(iNeighbours.length)];
            Neighbour<Exam, ExamPlacement> n = ns.selectNeighbour(solution);
            if (n != null)
                return n;
        }
    }

    /** Accept neighbour */
    protected boolean accept(Solution<Exam, ExamPlacement> solution, Neighbour<Exam, ExamPlacement> neighbour) {
        return (neighbour.value() <= 0 || solution.getModel().getTotalValue() + neighbour.value() <= iBound);
    }

    /** Increment iteration count, update bound */
    protected void incIter(Solution<Exam, ExamPlacement> solution) {
        if (iIter < 0) {
            iIter = 0;
            iLastImprovingIter = 0;
            iT0 = JProf.currentTimeMillis();
            iBound = (solution.getBestValue() > 0.0 ? iUpperBoundRate * solution.getBestValue() : solution
                    .getBestValue()
                    / iUpperBoundRate);
            iUpperBound = iBound;
            iNrIdle = 0;
            iProgress.setPhase("Great deluge [" + (1 + iNrIdle) + "]...");
        } else {
            iIter++;
            if (solution.getBestValue() >= 0.0)
                iBound *= iCoolRate;
            else
                iBound /= iCoolRate;
        }
        if (iIter % 100000 == 0) {
            info(solution);
        }
        double lowerBound = (solution.getBestValue() >= 0.0 ? Math.pow(iLowerBoundRate, 1 + iNrIdle)
                * solution.getBestValue() : solution.getBestValue() / Math.pow(iLowerBoundRate, 1 + iNrIdle));
        if (iBound < lowerBound) {
            iNrIdle++;
            sLog.info(" -<[" + iNrIdle + "]>- ");
            iBound = Math.max(solution.getBestValue() + 2.0, (solution.getBestValue() >= 0.0 ? Math.pow(
                    iUpperBoundRate, iNrIdle)
                    * solution.getBestValue() : solution.getBestValue() / Math.pow(iUpperBoundRate, iNrIdle)));
            iUpperBound = iBound;
            iProgress.setPhase("Great deluge [" + (1 + iNrIdle) + "]...");
        }
        iProgress.setProgress(100 - Math.round(100.0 * (iBound - lowerBound) / (iUpperBound - lowerBound)));
    }

    /**
     * A neighbour is generated randomly untill an acceptable one is found.
     */
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        Neighbour<Exam, ExamPlacement> neighbour = null;
        while ((neighbour = genMove(solution)) != null) {
            iMoves++;
            if (accept(solution, neighbour)) {
                iAcceptedMoves++;
                break;
            }
        }
        return (neighbour == null ? null : neighbour);
    }

    /** Update last improving iteration count */
    public void bestSaved(Solution<Exam, ExamPlacement> solution) {
        if (Math.abs(iBestValue - solution.getBestValue()) >= 1.0) {
            iLastImprovingIter = iIter;
            iNrIdle = 0;
            iBestValue = solution.getBestValue();
        }
    }

    public void solutionUpdated(Solution<Exam, ExamPlacement> solution) {
    }

    public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info) {
    }

    public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info, Collection<Exam> variables) {
    }

    public void bestCleared(Solution<Exam, ExamPlacement> solution) {
    }

    public void bestRestored(Solution<Exam, ExamPlacement> solution) {
    }

}
