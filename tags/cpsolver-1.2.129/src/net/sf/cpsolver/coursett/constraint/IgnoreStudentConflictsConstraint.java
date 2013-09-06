package net.sf.cpsolver.coursett.constraint;

import java.util.Set;

import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.model.Constraint;

/**
 * Ignore student conflicts constraint. <br>
 * This constraint keeps track of classes between which student conflicts of any kind are to be ignored.
 * This constraint is used by {@link Lecture#isToIgnoreStudentConflictsWith(Lecture)} and translates to
 * {@link StudentConflict#ignore(Lecture, Lecture)} that is true when the two classes are connected by
 * this constraint.
 *   
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2013 Tomas Muller<br>
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
public class IgnoreStudentConflictsConstraint extends Constraint<Lecture, Placement> {
    
    public static final String REFERENCE = "NO_CONFLICT";
    
    @Override
    public void addVariable(Lecture variable) {
        super.addVariable(variable);
        variable.clearIgnoreStudentConflictsWithCache();
    }

    @Override
    public void computeConflicts(Placement value, Set<Placement> conflicts) {
    }
    
    @Override
    public boolean isHard() {
        return false;
    }

}
