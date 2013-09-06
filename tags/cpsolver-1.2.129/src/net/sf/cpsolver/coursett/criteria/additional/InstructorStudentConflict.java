package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Instructor student conflicts. This criterion penalizes cases when an instructor (of a class) is attending some 
 * other class as a student and there is a conflict between the two classes.
 * <br>
 * To enable instructor student conflicts, set solver parameter Global.LoadStudentInstructorConflicts to true. Also
 * student course requests should be used in this case (to be able to match an instructor external id to a student 
 * external id).
 * <br>
 * Instructor student conflicts are weighted by Comparator.InstructorStudentConflictWeight.
 *   
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2013 Tomas Muller<br>
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
public class InstructorStudentConflict extends StudentConflict {
    
    /**
     * Only count students that are instructors assigned to one of the two classes and enrolled in the other.
     */
    @Override
    protected double jointEnrollment(JenrlConstraint jenrl) {
        double ret = 0.0;
        for (Student student: jenrl.getInstructors())
            ret += student.getJenrlWeight(jenrl.first(), jenrl.second());
        return ret;
    }
    
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return super.inConflict(p1, p2) && instructor(p1, p2); 
    }
    
    /**
     * True if there is at least one student teaching one of the two placements and enrolled in the other.
     */
    public boolean instructor(Placement p1, Placement p2) {
        JenrlConstraint jenrl = (p1 == null || p2 == null ? null : p1.variable().jenrlConstraint(p2.variable()));
        if (jenrl == null) return false;
        return jenrl.getNrInstructors() > 0;
    }
    
    /**
     * 
     */
    @Override
    public void incJenrl(JenrlConstraint jenrl, double studentWeight, Double conflictPriority, Student student) {
        if (super.inConflict(jenrl.first().getAssignment(), jenrl.second().getAssignment()) && student.getInstructor() != null
                && (student.getInstructor().variables().contains(jenrl.first()) || student.getInstructor().variables().contains(jenrl.second())))
            iValue += studentWeight; 
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.InstructorStudentConflictWeight", 10.0 * config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrInstructorStudConfsWeight";
    }

    @Override
    public void getInfo(Map<String, String> info) {
        super.getInfo(info);
        double conf = getValue();
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(InstructorStudentHardConflict.class);
            double hard = (c == null ? 0.0 : c.getValue());
            info.put("Instructor student conflicts", sDoubleFormat.format(conf) + (hard > 0.0 ? " [hard: " + sDoubleFormat.format(hard) + "]" : ""));
        }
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(info, variables);
        double conf = getValue(variables);
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(InstructorStudentHardConflict.class);
            double hard = (c == null ? 0.0 : c.getValue(variables));
            info.put("Instructor student conflicts", sDoubleFormat.format(conf) + (hard > 0.0 ? " [hard: " + sDoubleFormat.format(hard) + "]" : ""));
        }
    }

}
