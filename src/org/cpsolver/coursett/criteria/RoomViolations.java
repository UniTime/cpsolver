package org.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;


/**
 * Room violations. This criterion counts how many times a prohibited room is assigned
 * to a class in interactive timetabling.
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
public class RoomViolations extends TimetablingCriterion {

    protected boolean violation(Placement value) {
        int pref = value.getRoomPreference();
        return pref > Constants.sPreferenceLevelProhibited / 2;
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (value.variable().isCommitted()) return 0.0;
        double ret = (violation(value) ? 1.0 : 0.0);
        if (conflicts != null)
            for (Placement conflict: conflicts)
                ret -= (violation(conflict) ? 1.0 : 0.0);
        return ret;
    }
    
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment) {
        return new double[] { getModel().variables().size(), 0.0 };
    }
        
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        return new double[] { variables.size(), 0.0 };
    }

}
