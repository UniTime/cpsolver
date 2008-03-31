package net.sf.cpsolver.exam.heuristics;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.neighbours.ExamRandomMove;
import net.sf.cpsolver.exam.neighbours.ExamRoomMove;
import net.sf.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import net.sf.cpsolver.exam.neighbours.ExamTimeMove;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Hill climber. In each iteration, one of the following three neighbourhoods is selected first
 * <ul>
 * <li>random move ({@link ExamRandomMove})
 * <li>period swap ({@link ExamTimeMove})
 * <li>room swap ({@link ExamRoomMove})
 * </ul>,
 * then a neighbour is generated and it is accepted if its value {@link ExamSimpleNeighbour#value()} is 
 * below or equal to zero. The search is stopped after a given amount of idle iterations (
 * can be defined by problem property  HillClimber.MaxIdle).
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
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
public class ExamHillClimbing implements NeighbourSelection, SolutionListener {
    private static Logger sLog = Logger.getLogger(ExamHillClimbing.class);
    private NeighbourSelection[] iNeighbours = null;
    private int iMaxIdleIters = 25000;
    private int iLastImprovingIter = 0;
    private double iBestValue = 0;
    private int iIter = 0;
    private Progress iProgress = null;
    private boolean iActive;

    /**
     * Constructor
     * @param properties problem properties (use HillClimber.MaxIdle to set maximum number of idle iterations)
     */
    public ExamHillClimbing(DataProperties properties) {
        iMaxIdleIters = properties.getPropertyInt("HillClimber.MaxIdle", iMaxIdleIters);
        iNeighbours = new NeighbourSelection[] {
                new ExamRandomMove(properties),
                new ExamRoomMove(properties),
                new ExamTimeMove(properties)
        };
    }
    
    /**
     * Initialization
     */
    public void init(Solver solver) {
        solver.currentSolution().addSolutionListener(this);
        for (int i=0;i<iNeighbours.length;i++)
            iNeighbours[i].init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iActive = false;
    }
    
    /**
     * Select one of the given neighbourhoods randomly, select neighbour, return it if 
     * its value is below or equal to zero (continue with the next selection otherwise).
     * Return null when the given number of idle iterations is reached.
     */
    public Neighbour selectNeighbour(Solution solution) {
        if (!iActive) {
            iProgress.setPhase("Hill climbing...");
            iActive = true;
        }
        Model model = (Model)solution.getModel();
        while (true) {
            iIter ++;
            iProgress.setProgress(Math.round(100.0*(iIter-iLastImprovingIter)/iMaxIdleIters));
            if (iIter-iLastImprovingIter>=iMaxIdleIters) break;
            NeighbourSelection ns = iNeighbours[ToolBox.random(iNeighbours.length)];
            Neighbour n = ns.selectNeighbour(solution);
            if (n!=null && n.value()<=0) return n;
        }
        iIter = 0; iLastImprovingIter = 0;
        iActive=false;
        return null;
    }
    
    /**
     * Memorize the iteration when the last best solution was found.
     */
    public void bestSaved(Solution solution) {
        if (Math.abs(iBestValue-solution.getBestValue())>=1.0) {
            iLastImprovingIter = iIter;
            iBestValue = solution.getBestValue();
        }
    }
    public void solutionUpdated(Solution solution) {}
    public void getInfo(Solution solution, java.util.Dictionary info) {}
    public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
    public void bestCleared(Solution solution) {}
    public void bestRestored(Solution solution){}   }
