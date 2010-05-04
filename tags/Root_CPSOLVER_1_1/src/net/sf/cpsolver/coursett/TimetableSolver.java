package net.sf.cpsolver.coursett;

import java.util.Enumeration;

import net.sf.cpsolver.coursett.heuristics.NeighbourSelectionWithSuggestions;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * University course timetabling solver.
 * <br><br>
 * When a complete solution is found, it is improved by limited depth backtracking search.
 * This way it is ensured that the fund solution is at least locally optimal.  
 *
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
 * Copyright (C) 2006 Tomas Muller<br>
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

public class TimetableSolver extends Solver {
    
    public TimetableSolver(DataProperties properties) {
        super(properties);
    }
    
    protected void onAssigned(double startTime) {
        // Check if the solution is the best ever found one
        if (iCurrentSolution.getModel().unassignedVariables().isEmpty() && getSolutionComparator().isBetterThanBestSolution(iCurrentSolution)) {
            fixCompleteSolution(startTime);
        } /* else {
            // If the solver is not able to improve solution in the last 5000 iterations, take the best one and try to fix it
            if (iCurrentSolution.getBestInfo()!=null && iCurrentSolution.getModel().getBestUnassignedVariables()>0 && iCurrentSolution.getIteration()==iCurrentSolution.getBestIteration()+5000) {
                iCurrentSolution.restoreBest();
                fixCompleteSolution(startTime);
            }
        } */
    }
    
    /** Try to improve existing solution by backtracking search of very limited depth. 
     * See {@link NeighbourSelectionWithSuggestions} for more details.*/ 
    protected void fixCompleteSolution(double startTime) {
        Progress progress = Progress.getInstance(currentSolution().getModel());
        
        Model model = iCurrentSolution.getModel();
        progress.save();
        double solutionValue = 0.0, newSolutionValue = model.getTotalValue();
        do {
            solutionValue = newSolutionValue;
            progress.setPhase("Fixing solution",model.variables().size());
            for (Enumeration e=model.variables().elements();e.hasMoreElements();) {
                Variable variable = (Variable)e.nextElement();
                Value bestValue = null;
                double bestVal = 0.0;
                Value currentValue = variable.getAssignment();
                if (currentValue==null) continue;
                double currentVal = currentValue.toDouble();
                for (Enumeration f=variable.values().elements();f.hasMoreElements();) {
                    Value value = (Value)f.nextElement();
                    if (value.equals(currentValue)) continue;
                    if (model.conflictValues(value).isEmpty()) {
                        double val = value.toDouble();
                        if (bestValue==null || val<bestVal) {
                            bestValue = value; bestVal = val;
                        }
                    }
                }
                if (bestValue!=null && bestVal<currentVal)
                    variable.assign(0, bestValue);
                iCurrentSolution.update(JProf.currentTimeSec()-startTime);
                progress.incProgress();
                if (iStop) break;
            }
            newSolutionValue = model.getTotalValue();
            if (newSolutionValue<solutionValue) {
                progress.debug("New solution value is  "+newSolutionValue);
            }
        } while (!iStop && newSolutionValue<solutionValue && getTerminationCondition().canContinue(iCurrentSolution));
        progress.restore();

        if (!iCurrentSolution.getModel().unassignedVariables().isEmpty()) return;
        progress.save();
        try {
            progress.setPhase("Fixing solution [2]",model.variables().size());
            NeighbourSelectionWithSuggestions ns = new NeighbourSelectionWithSuggestions(this); 
            for (Enumeration e=model.variables().elements();e.hasMoreElements();) {
                Lecture lecture = (Lecture)e.nextElement();
                Neighbour n = ns.selectNeighbourWithSuggestions(iCurrentSolution, lecture,2);
                if (n!=null)
                    n.assign(0);
                iCurrentSolution.update(JProf.currentTimeSec()-startTime);
                progress.incProgress();
                if (iStop) break;
            }
        } catch (Exception e) {
            sLogger.debug(e.getMessage(),e);
        } finally {
            progress.restore();
        }
        
    }
}
