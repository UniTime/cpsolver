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
import org.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import org.cpsolver.exam.neighbours.ExamTimeMove;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.LazyNeighbour;
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
 * Simulated annealing. In each iteration, one of the following three
 * neighbourhoods is selected first
 * <ul>
 * <li>random move ({@link ExamRandomMove})
 * <li>period swap ({@link ExamTimeMove})
 * <li>room swap ({@link ExamRoomMove})
 * </ul>
 * , then a neighbour is generated and it is accepted with probability
 * {@link ExamSimulatedAnnealing.Context#prob(double)}. The search is guided by the
 * temperature, which starts at <i>SimulatedAnnealing.InitialTemperature</i>.
 * After each <i>SimulatedAnnealing.TemperatureLength</i> iterations, the
 * temperature is reduced by <i>SimulatedAnnealing.CoolingRate</i>. If there was
 * no improvement in the past <i>SimulatedAnnealing.ReheatLengthCoef *
 * SimulatedAnnealing.TemperatureLength</i> iterations, the temperature is
 * increased by <i>SimulatedAnnealing.ReheatRate</i>. If there was no
 * improvement in the past <i>SimulatedAnnealing.RestoreBestLengthCoef *
 * SimulatedAnnealing.TemperatureLength</i> iterations, the best ever found
 * solution is restored. <br>
 * <br>
 * If <i>SimulatedAnnealing.StochasticHC</i> is true, the acceptance probability
 * is computed using stochastic hill climbing criterion, i.e.,
 * <pre><code>1.0 / (1.0 + Math.exp(value/temperature))</code></pre>, otherwise it is
 * cumputed using simlated annealing criterion, i.e.,
 * <pre><code>(value&lt;=0.0?1.0:Math.exp(-value/temperature))</code></pre>. If
 * <i>SimulatedAnnealing.RelativeAcceptance</i> neighbour value
 * {@link ExamSimpleNeighbour#value(Assignment)} is taken as the value of the selected
 * neighbour (difference between the new and the current solution, if the
 * neighbour is accepted), otherwise the value is computed as the difference
 * between the value of the current solution if the neighbour is accepted and
 * the best ever found solution. <br>
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
public class ExamSimulatedAnnealing extends NeighbourSelectionWithContext<Exam, ExamPlacement, ExamSimulatedAnnealing.Context> implements SolutionListener<Exam, ExamPlacement>, LazyNeighbourAcceptanceCriterion<Exam, ExamPlacement> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ExamSimulatedAnnealing.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private static DecimalFormat sDF10 = new DecimalFormat("0.0000000000");
    private double iInitialTemperature = 1.5;
    private double iCoolingRate = 0.95;
    private double iReheatRate = -1;
    private long iTemperatureLength = 250000;
    private long iReheatLength = 0;
    private long iRestoreBestLength = 0;
    private double iReheatLengthCoef = 5.0;
    private double iRestoreBestLengthCoef = -1;
    private boolean iStochasticHC = false;
    private double iBestValue = 0;
    private Progress iProgress = null;

    private List<NeighbourSelection<Exam, ExamPlacement>> iNeighbours = null;

    private boolean iRelativeAcceptance = true;

    /**
     * Constructor. Following problem properties are considered:
     * <ul>
     * <li>SimulatedAnnealing.InitialTemperature ... initial temperature
     * (default 1.5)
     * <li>SimulatedAnnealing.TemperatureLength ... temperature length (number
     * of iterations between temperature decrements, default 25000)
     * <li>SimulatedAnnealing.CoolingRate ... temperature cooling rate (default
     * 0.95)
     * <li>SimulatedAnnealing.ReheatLengthCoef ... temperature re-heat length
     * coefficient (multiple of temperature length, default 5)
     * <li>SimulatedAnnealing.ReheatRate ... temperature re-heating rate
     * (default (1/coolingRate)^(reheatLengthCoef*1.7))
     * <li>SimulatedAnnealing.RestoreBestLengthCoef ... restore best length
     * coefficient (multiple of re-heat length, default reheatLengthCoef^2)
     * <li>SimulatedAnnealing.StochasticHC ... true for stochastic search
     * acceptance criterion, false for simulated annealing acceptance (default
     * false)
     * <li>SimulatedAnnealing.RelativeAcceptance ... true for relative
     * acceptance (different between the new and the current solutions, if the
     * neighbour is accepted), false for absolute acceptance (difference between
     * the new and the best solutions, if the neighbour is accepted)
     * </ul>
     * 
     * @param properties
     *            problem properties
     */
    @SuppressWarnings("unchecked")
    public ExamSimulatedAnnealing(DataProperties properties) {
        iInitialTemperature = properties
                .getPropertyDouble("SimulatedAnnealing.InitialTemperature", iInitialTemperature);
        iReheatRate = properties.getPropertyDouble("SimulatedAnnealing.ReheatRate", iReheatRate);
        iCoolingRate = properties.getPropertyDouble("SimulatedAnnealing.CoolingRate", iCoolingRate);
        iRelativeAcceptance = properties.getPropertyBoolean("SimulatedAnnealing.RelativeAcceptance",
                iRelativeAcceptance);
        iStochasticHC = properties.getPropertyBoolean("SimulatedAnnealing.StochasticHC", iStochasticHC);
        iTemperatureLength = properties.getPropertyLong("SimulatedAnnealing.TemperatureLength", iTemperatureLength);
        iReheatLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.ReheatLengthCoef", iReheatLengthCoef);
        iRestoreBestLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.RestoreBestLengthCoef",
                iRestoreBestLengthCoef);
        if (iReheatRate < 0)
            iReheatRate = Math.pow(1 / iCoolingRate, iReheatLengthCoef * 1.7);
        if (iRestoreBestLengthCoef < 0)
            iRestoreBestLengthCoef = iReheatLengthCoef * iReheatLengthCoef;
        String neighbours = properties.getProperty("SimulatedAnnealing.Neighbours", 
                ExamRandomMove.class.getName() + ";" +
                ExamRoomMove.class.getName() + ";" +
                ExamTimeMove.class.getName());
        neighbours += ";" + properties.getProperty("SimulatedAnnealing.AdditionalNeighbours", "");
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

    /**
     * Initialization
     */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        super.init(solver);
        iReheatLength = Math.round(iReheatLengthCoef * iTemperatureLength);
        iRestoreBestLength = Math.round(iRestoreBestLengthCoef * iTemperatureLength);
        solver.currentSolution().addSolutionListener(this);
        for (NeighbourSelection<Exam, ExamPlacement> neighbour: iNeighbours)
            neighbour.init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
    }


    /**
     * Generate neighbour -- select neighbourhood randomly, select neighbour
     * @param solution current solutoon
     * @return generated neighbour
     */
    public Neighbour<Exam, ExamPlacement> genMove(Solution<Exam, ExamPlacement> solution) {
        while (true) {
            getContext(solution.getAssignment()).incIter(solution);
            NeighbourSelection<Exam, ExamPlacement> ns = iNeighbours.get(ToolBox.random(iNeighbours.size()));
            Neighbour<Exam, ExamPlacement> n = ns.selectNeighbour(solution);
            if (n != null)
                return n;
        }
    }

    /**
     * True if the given neighboir is to be be accepted
     * 
     * @param solution
     *            current solution
     * @param neighbour
     *            proposed move
     * @return true if generated random number is below
     *         {@link ExamSimulatedAnnealing.Context#prob(double)}
     */
    protected boolean accept(Solution<Exam, ExamPlacement> solution, Neighbour<Exam, ExamPlacement> neighbour) {
        if (neighbour instanceof LazyNeighbour) {
            ((LazyNeighbour<Exam, ExamPlacement>)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        Assignment<Exam, ExamPlacement> assignment = solution.getAssignment();
        double value = (iRelativeAcceptance ? neighbour.value(assignment) : solution.getModel().getTotalValue(assignment) + neighbour.value(assignment) - solution.getBestValue());
        Context context = getContext(solution.getAssignment());
        double prob = context.prob(value);
        if (prob >= 1.0 || ToolBox.random() < prob) {
            context.accepted(neighbour.value(assignment));
            return true;
        }
        return false;
    }
    
    /** Accept lazy neighbour */
    @Override
    public boolean accept(Assignment<Exam, ExamPlacement> assignment, LazyNeighbour<Exam, ExamPlacement> neighbour, double value) {
        Context context = getContext(assignment);
        double prob = context.prob(value);
        if (prob >= 1.0 || ToolBox.random() < prob) {
            context.accepted(value);
            return true;
        }
        return false;
    }

    /**
     * Select neighbour -- generate a move
     * {@link ExamSimulatedAnnealing#genMove(Solution)} until an acceptable
     * neighbour is found
     * {@link ExamSimulatedAnnealing#accept(Solution, Neighbour)}, keep
     * increasing iteration {@link ExamSimulatedAnnealing.Context#incIter(Solution)}.
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        Context context = getContext(solution.getAssignment());
        context.activateIfNeeded();
        Neighbour<Exam, ExamPlacement> neighbour = null;
        while ((neighbour = genMove(solution)) != null) {
            context.incMoves(neighbour.value(solution.getAssignment()));
            if (accept(solution, neighbour))
                break;
        }
        if (neighbour == null)
            context.reset();
        return (neighbour == null ? null : neighbour);
    }

    @Override
    public void bestSaved(Solution<Exam, ExamPlacement> solution) {
        getContext(solution.getAssignment()).bestSaved(solution);
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
    
    public class Context implements AssignmentContext {
        private boolean iActive = false;
        private double iTemperature = 0.0;
        private long iIter = 0;
        private long iLastImprovingIter = 0;
        private long iLastReheatIter = 0;
        private long iLastCoolingIter = 0;
        private int iAcceptIter[] = new int[] { 0, 0, 0 };
        private int iMoves = 0;
        private long iT0 = -1;
        private double iAbsValue = 0;
        
        /**
         * Increment iteration counter, cool/reheat/restoreBest if necessary
         * @param solution current solution
         */
        protected void incIter(Solution<Exam, ExamPlacement> solution) {
            if (iT0 < 0) {
                iT0 = JProf.currentTimeMillis();
                iTemperature = iInitialTemperature;
            }
            iIter++;
            if (iIter > iLastImprovingIter + iRestoreBestLength) {
                solution.restoreBest();
                iLastImprovingIter = iIter;
            }
            if (iIter > Math.max(iLastReheatIter, iLastImprovingIter) + iReheatLength)
                reheat(solution);
            if (iIter > iLastCoolingIter + iTemperatureLength)
                cool(solution);
            iProgress.setProgress(Math.round(100.0 * (iIter - Math.max(iLastReheatIter, iLastImprovingIter)) / iReheatLength));
        }

        /**
         * Cool temperature
         * @param solution current solution
         */
        protected void cool(Solution<Exam, ExamPlacement> solution) {
            iTemperature *= iCoolingRate;
            sLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                    + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
            sLog.info("Temperature decreased to " + sDF5.format(iTemperature) + " " + "(#moves=" + iMoves + ", rms(value)="
                    + sDF2.format(Math.sqrt(iAbsValue / iMoves)) + ", " + "accept=-"
                    + sDF2.format(100.0 * iAcceptIter[0] / iTemperatureLength) + "/"
                    + sDF2.format(100.0 * iAcceptIter[1] / iTemperatureLength) + "/+"
                    + sDF2.format(100.0 * iAcceptIter[2] / iTemperatureLength) + "%, "
                    + (prob(-1) < 1.0 ? "p(-1)=" + sDF2.format(100.0 * prob(-1)) + "%, " : "") + "p(+1)="
                    + sDF2.format(100.0 * prob(1)) + "%, " + "p(+10)=" + sDF5.format(100.0 * prob(10)) + "%)");
            iLastCoolingIter = iIter;
            iAcceptIter = new int[] { 0, 0, 0 };
            iMoves = 0;
            iAbsValue = 0;
        }

        /**
         * Reheat temperature
         * @param solution current solution
         */
        protected void reheat(Solution<Exam, ExamPlacement> solution) {
            iTemperature *= iReheatRate;
            sLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                    + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
            sLog.info("Temperature increased to " + sDF5.format(iTemperature) + " "
                    + (prob(-1) < 1.0 ? "p(-1)=" + sDF2.format(100.0 * prob(-1)) + "%, " : "") + "p(+1)="
                    + sDF2.format(100.0 * prob(1)) + "%, " + "p(+10)=" + sDF5.format(100.0 * prob(10)) + "%, " + "p(+100)="
                    + sDF10.format(100.0 * prob(100)) + "%)");
            iLastReheatIter = iIter;
            iProgress.setPhase("Simulated Annealing [" + sDF2.format(iTemperature) + "]...");
        }
        
        /**
         * Memorize the iteration when the last best solution was found.
         * @param solution current solution
         */
        protected void bestSaved(Solution<Exam, ExamPlacement> solution) {
            if (Math.abs(iBestValue - solution.getBestValue()) >= 1.0) {
                iLastImprovingIter = iIter;
                iBestValue = solution.getBestValue();
            }
            iLastImprovingIter = iIter;
        }
        
        /**
         * Neighbour acceptance probability
         * 
         * @param value
         *            absolute or relative value of the proposed change (neighbour)
         * @return probability of acceptance of a change (neighbour)
         */
        protected double prob(double value) {
            if (iStochasticHC)
                return 1.0 / (1.0 + Math.exp(value / iTemperature));
            else
                return (value <= 0.0 ? 1.0 : Math.exp(-value / iTemperature));
        }
        
        /** A neighbor with the given value was accepted. 
         * @param value change in the solution value
         **/ 
        protected void accepted(double value) {
            iAcceptIter[value < 0.0 ? 0 : value > 0.0 ? 2 : 1]++;
        }
        
        protected void activateIfNeeded() {
            if (!iActive) {
                iProgress.setPhase("Simulated Annealing [" + sDF2.format(iTemperature) + "]...");
                iActive = true;
            }
        }
        
        protected void incMoves(double value) {
            iMoves ++;
            iAbsValue += value * value;
        }
        
        protected void reset() {
            iActive = false;
        }
    }

    @Override
    public Context createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new Context();
    }
}
