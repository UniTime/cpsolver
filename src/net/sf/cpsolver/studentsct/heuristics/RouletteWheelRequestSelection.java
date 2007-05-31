package net.sf.cpsolver.studentsct.heuristics;

import java.util.Enumeration;

import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.heuristics.general.RouletteWheelSelection;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Variable ({@link Request}) selection using {@link RouletteWheelSelection}.
 * Unassigned request has 10 points, an assigned request has 1 point for 
 * each section that exceeds its bound. 
 * 
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
public class RouletteWheelRequestSelection implements VariableSelection {
    
    /**
     * Constructor
     * @param properties configuration
     */
    public RouletteWheelRequestSelection(DataProperties properties) {
        super();
    }
    
    /** Initialization */
    public void init(Solver solver) {
        
    }
    
    /** 
     * Variable selection. {@link RouletteWheelSelection} is used.
     * Unassigned request has 10 points, an assigned request has 1 point for
     * each section that exceeds its bound. 
     */
    public Variable selectVariable(Solution solution) {
        RouletteWheelSelection roulette = new RouletteWheelSelection();
        for (Enumeration e=solution.getModel().variables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            double points = 0;
            if (request.getAssignment()==null)
                points +=10;
            else {
                Enrollment enrollment = (Enrollment)request.getAssignment();
                if (enrollment.toDouble()>request.getBound())
                    points +=1;
            }
            if (points>0)
                roulette.add(request, points);
        }
        return (Variable)roulette.nextElement();
    }
}
