package net.sf.cpsolver.ifs.algorithms;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Step counting hill climber. Unlike with the ordinary hill climber, there is a bound.
 * The bound is updated (to the value of the current solution) after a given number of
 * moves. Based on the given mode, either all moves, only accepted moves, or only improving
 * moves are counted. 
 * <br>
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
public class StepCountingHillClimber<V extends Variable<V, T>, T extends Value<V, T>> extends HillClimber<V, T> {
    public static enum Mode {
        ALL,
        ACCEPTED,
        IMPROVING
    }
    protected Double iBound = null;
    protected int iCounter = 0;
    protected int iCounterLimit = 100;
    protected Mode iCounterMode = Mode.ACCEPTED;

    /**
     * Constructor
     * <ul>
     * <li>HillClimber.CounterLimit ... number of moves after which the bound is reset (defaults to 1000)
     * <li>HillClimber.CounterMode ... counter mode (all: count all moves, accepted: count accepted moves, improving: count improving moves)
     * </ul>
     */
    public StepCountingHillClimber(DataProperties properties, String name) {
        super(properties);
        iSetHCMode = false;
    }
    
    /**
     * Reset the bound and the steps counter.
     */
    @Override
    public void activate(Solution<V, T> solution) {
        super.activate(solution);
        iBound = solution.getModel().getTotalValue();
        iCounter = 0;
    }
    
    /**
     * Increase iteration number, also update bound when the given number of steps is reached.
     */
    @Override
    public void incIteration(Solution<V, T> solution) {
        iIter++;
        if (iIter % 10000 == 0) {
            iLog.info("Iter=" + (iIter / 1000)+"k, NonImpIter=" + iDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+iDF2.format(1000.0*iIter/getTimeMillis())+" it/s, Bound=" + iDF2.format(iBound));
            logNeibourStatus();
        }
        iProgress.setProgress(Math.round(100.0 * (iIter - iLastImprovingIter) / iMaxIdleIters));
        if (iCounter >= iCounterLimit) {
            iBound = solution.getModel().getTotalValue(); 
            iCounter = 0;
        }
    }

    /**
     * Accept any move that does not worsen the solution (value <= 0) or that is below the bound. Also increase the step counter.
     */
    @Override
    protected boolean accept(Model<V,T> model, Neighbour<V, T> neighbour, double value, boolean lazy) {
        boolean accept = (value <= 0.0 || (lazy ? model.getTotalValue() : value + model.getTotalValue()) < iBound);
        switch (iCounterMode) {
            case ALL:
                iCounter ++;
                break;
            case ACCEPTED:
                if (accept) iCounter ++;
                break;
            case IMPROVING:
                if (value < 0) iCounter ++;
                break;
        }
        return accept;
    }
    
    /**
     * Stop the search when the number of idle iterations is reached and the bound is no longer decreasing
     */
    @Override
    protected boolean canContinue(Solution<V, T> solution) {
        return super.canContinue(solution) || iCounter < iCounterLimit || solution.getModel().getTotalValue() < iBound;
    }
}