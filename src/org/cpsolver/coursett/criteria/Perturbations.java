package org.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.perturbations.PerturbationsCounter;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Perturbations. This criterion counts differences (perturbations) between initial and the current
 * solution. It is based on {@link PerturbationsCounter}.
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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

public class Perturbations extends TimetablingCriterion {
    private PerturbationsCounter<Lecture, Placement> iPerturbationsCounter = null;
    
    public Perturbations() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.MPP_DeltaInitialAssignmentWeight";
    }

    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        iPerturbationsCounter = solver.getPerturbationsCounter();
        return super.init(solver);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.PerturbationPenaltyWeight", 1.0);
    }
    
    public PerturbationsCounter<Lecture, Placement> getPerturbationsCounter() {
        return iPerturbationsCounter;
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        return getPerturbationsCounter().getPerturbationPenalty(assignment, getModel(), value, conflicts);
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment) {
        return getPerturbationsCounter().getPerturbationPenalty(assignment, getModel());
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        return getPerturbationsCounter().getPerturbationPenalty(assignment, getModel(), variables);
    }

    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment) {
        return new double[] { 0.0, 0.0 };
    }
    
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        return new double[] { 0.0, 0.0 };
    }
}
