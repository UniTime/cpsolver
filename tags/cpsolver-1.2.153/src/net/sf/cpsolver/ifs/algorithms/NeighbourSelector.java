package net.sf.cpsolver.ifs.algorithms;

import java.text.DecimalFormat;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * A wrapper for {@link NeighbourSelection} that keeps some stats about the 
 * given neighbour selector.
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
public class NeighbourSelector<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T> {
    protected static DecimalFormat sDF = new DecimalFormat("0.00");
    private boolean iUpdate = false;
    private NeighbourSelection<V,T> iSelection;
    private int iNrCalls = 0;
    private int iNrNotNull = 0;
    private int iNrSideMoves = 0;
    private int iNrImprovingMoves = 0;
    private double iBonus = 1.0;
    private double iPoints = 0;
    private long iTime = 0;
    
    /**
     * Constructor 
     * @param sel neighbour selector
     * @param bonus initial bonus (default is 1, can be changed by &nbsp;@n parameter after 
     * the name of the selector in Xxx.Neigbours, e.g., net.sf.cpsolver.itc.tim.neighbours.TimPrecedenceMove@0.1
     * for initial bonus 0.1 
     * @param update update selector bonus after each iteration
     */
    public NeighbourSelector(NeighbourSelection<V,T> sel, double bonus, boolean update) {
        iSelection = sel;
        iBonus = bonus;
        iUpdate = update;
    }
    
    /** Initialization */
    @Override
    public void init(Solver<V,T> solver) {
        iSelection.init(solver);
    }
    
    /** Neighbour selection -- use {@link NeighbourSelection#selectNeighbour(Solution)} 
     * update stats if desired.
     */
    @Override
    public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
        if (iUpdate) {
            long t0 = System.currentTimeMillis();
            Neighbour<V,T> n = iSelection.selectNeighbour(solution);
            long t1 = System.currentTimeMillis();
            update(n, t1-t0);
            return n;
        } else
            return iSelection.selectNeighbour(solution);
    }

    /**
     * Update stats
     * @param n generated move
     * @param time time needed to generate the move (in milliseconds)
     */
    public void update(Neighbour<V,T> n, long time) {
        iNrCalls ++;
        iTime += time;
        if (n!=null) {
            iNrNotNull++;
            if (n.value()==0) {
                iNrSideMoves++;
                iPoints += 0.1;
            } else if (n.value()<0) {
                iNrImprovingMoves++;
                iPoints -= n.value();
            } else {
                iPoints *= 0.9999;
            }
        } else {
            iPoints *= 0.999;
        }
    }
    
    /** Weight of the selector in the roulette wheel selection of neighbour selectors */
    public double getPoints() { return iBonus * Math.min(100.0, 0.1+iPoints); }
    /** Initial bonus */
    public double getBonus() { return iBonus; }
    /** Given neighbour selection */
    public NeighbourSelection<V,T> selection() { return iSelection; }
    /** Number of calls of {@link NeighbourSelection#selectNeighbour(Solution)} */
    public int nrCalls() { return iNrCalls; }
    /** Number of returned not-null moves */
    public int nrNotNull() { return iNrNotNull; }
    /** Number of returned moves with zero improvement of the solution (i.e., {@link Neighbour#value()} = 0)*/
    public int nrSideMoves() { return iNrSideMoves; }
    /** Number of returned improving moves (i.e., {@link Neighbour#value()} < 0)*/
    public int nrImprovingMoves() { return iNrImprovingMoves; }
    /** Total time spend in {@link NeighbourSelection#selectNeighbour(Solution)} (in milliseconds) */
    public long time() { return iTime; }
    /** Average number of iterations per second (calls of {@link NeighbourSelection#selectNeighbour(Solution)}) */
    public double speed() { return 1000.0*nrCalls()/time(); }
    /** String representation */
    @Override
    public String toString() {
        return iSelection.getClass().getName().substring(iSelection.getClass().getName().lastIndexOf('.')+1)+" "+
            nrCalls()+"x, "+
            sDF.format(100.0*(nrCalls()-nrNotNull())/nrCalls())+"% null, "+
            sDF.format(100.0*nrSideMoves()/nrCalls())+"% side, "+
            sDF.format(100.0*nrImprovingMoves()/nrCalls())+"% imp, "+
            sDF.format(speed())+" it/s";
    }
}
