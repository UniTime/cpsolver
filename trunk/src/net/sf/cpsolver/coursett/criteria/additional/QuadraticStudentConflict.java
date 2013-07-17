package net.sf.cpsolver.coursett.criteria.additional;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Quadratic student conflicts. Same as {@link StudentConflict}, however,
 * student joint enrollments are squared (1 conflict counts as 1, 2 as 4, 3 as 9, etc.).
 * 
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2011 Tomas Muller<br>
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

public class QuadraticStudentConflict extends StudentConflict {
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return applicable(l1, l2);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrStudConfsWeight";
    }

    @Override
    protected double jointEnrollment(JenrlConstraint jenrl) {
        return jenrl.jenrl() * jenrl.jenrl();
    }
    
    @Override
    public void incJenrl(JenrlConstraint jenrl, double studentWeight, Double conflictPriority, Student student) {
        if (inConflict(jenrl.first().getAssignment(), jenrl.second().getAssignment())) {
            iValue += (jenrl.jenrl() * jenrl.jenrl()) - (jenrl.jenrl() - studentWeight) * (jenrl.jenrl() - studentWeight);
        }
    }
    
}
