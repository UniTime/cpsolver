package org.cpsolver.studentsct.heuristics;

import org.cpsolver.ifs.heuristics.RouletteWheelSelection;
import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;

/**
 * Variable ({@link Request}) selection using {@link RouletteWheelSelection}.
 * Unassigned request has 10 points, an assigned request has 1 point for each
 * section that exceeds its bound.
 * 
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class RouletteWheelRequestSelection implements VariableSelection<Request, Enrollment> {
    RouletteWheelSelection<Request> iRoulette = null;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    public RouletteWheelRequestSelection(DataProperties properties) {
        super();
    }

    /** Initialization */
    @Override
    public void init(Solver<Request, Enrollment> solver) {

    }

    /** Populate roulette wheel selection, if null or empty. 
     * @param solution current solution
     * @return selection
     **/
    protected RouletteWheelSelection<Request> getRoulette(Solution<Request, Enrollment> solution) {
        if (iRoulette != null && iRoulette.hasMoreElements()) {
            if (iRoulette.getUsedPoints() < 0.1 * iRoulette.getTotalPoints())
                return iRoulette;
        }
        iRoulette = new RouletteWheelSelection<Request>();
        for (Request request : ((StudentSectioningModel) solution.getModel()).variables()) {
            double points = 0;
            if (solution.getAssignment().getValue(request) == null)
                points += 10;
            else {
                Enrollment enrollment = solution.getAssignment().getValue(request);
                if (enrollment.toDouble(solution.getAssignment()) > request.getBound())
                    points += 1;
            }
            if (points > 0)
                iRoulette.add(request, points);
        }
        return iRoulette;
    }

    /**
     * Variable selection. {@link RouletteWheelSelection} is used. Unassigned
     * request has 10 points, an assigned request has 1 point for each section
     * that exceeds its bound.
     */
    @Override
    public synchronized Request selectVariable(Solution<Request, Enrollment> solution) {
        return getRoulette(solution).nextElement();
    }
}
