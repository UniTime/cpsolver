package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Instructor student conflicts. This criterion penalizes cases when an instructor (of a class) is attending some 
 * other class as a student and there is a conflict between the two classes. Also, there is no alternative for the
 * student class (the conflict cannot be sectioned away).
 * <br>
 * To enable instructor student conflicts, set solver parameter Global.LoadStudentInstructorConflicts to true. Also
 * student course requests should be used in this case (to be able to match an instructor external id to a student 
 * external id).
 * <br>
 * Hard instructor student conflicts are weighted by Comparator.InstructorHardStudentConflictWeight.
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
public class InstructorStudentHardConflict extends InstructorStudentConflict {
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return super.isApplicable(l1, l2) && oneInstructorOtherHard(l1, l2);
    }
    
    /**
     * One of the lectures is hard, there is a joint enrollment constraint between them, and 
     * there is at least one student that is instructor for one lecture and the other lecture
     * is singleton.
     */
    public static boolean oneInstructorOtherHard(Lecture l1, Lecture l2) {
        if (!hard(l1, l2)) return false;
        JenrlConstraint jenrl = l1.jenrlConstraint(l2);
        if (jenrl == null) return false;
        for (Student student: jenrl.getInstructors()) {
            if ((l1.isSingleSection() || student.getInstructor().variables().contains(jenrl.second())) &&
                (l2.isSingleSection() || student.getInstructor().variables().contains(jenrl.first())))
                return true;
        }
        return false;
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.InstructorHardStudentConflictWeight", 10.0 * config.getPropertyDouble("Comparator.HardStudentConflictWeight", 5.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrInstructorHardStudConfsWeight";
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
    }

}
