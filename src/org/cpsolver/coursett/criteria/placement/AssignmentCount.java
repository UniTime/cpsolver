package org.cpsolver.coursett.criteria.placement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.Counter;


/**
 * Count number of past assignments of a value. Avoid repetition by penalizing
 * values that have been assigned too many times already. 
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
@Deprecated
public class AssignmentCount extends PlacementSelectionCriterion {
    
    public AssignmentCount() {
        setValueUpdateType(ValueUpdateType.BeforeUnassignedAfterAssigned);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrAssignmentsWeight";
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        return ((AssignmentCountContext)getContext(assignment)).countAssignments(value);
    }
    
    @Override
    public ValueContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new AssignmentCountContext(assignment);
    }

    public class AssignmentCountContext extends ValueContext {
        Map<Placement, Counter> iCounter = new HashMap<Placement, Counter>();

        protected AssignmentCountContext(Assignment<Lecture, Placement> assignment) {
            super(assignment);
        }
        
        @Override
        protected void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            Counter c = iCounter.get(value);
            if (c == null) {
                c = new Counter();
                iCounter.put(value, c);
            }
            c.inc(1);
        }
        
        @Override
        protected void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
        }
        
        public long countAssignments(Placement value) {
            Counter c = iCounter.get(value);
            return c == null ? 0 : c.get();
        }
    }
}
