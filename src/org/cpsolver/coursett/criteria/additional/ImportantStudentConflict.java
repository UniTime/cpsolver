package org.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Important student conflicts. Some student conflicts can be counted differently,
 * using Comparator.ImportantStudentConflictWeight. Importance of a conflict is
 * defined by the student - offering request priority {@link Student#getPriority(Long)}.
 *   
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
public class ImportantStudentConflict extends StudentConflict {
    
    @Override
    protected double jointEnrollment(JenrlConstraint jenrl) {
        return jenrl.priority();
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !ignore(l1, l2) && applicable(l1, l2) && important(l1, l2);
    }
    
    @Override
    public boolean isApplicable(Student student, Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !ignore(l1, l2) && applicable(l1, l2) && student.getConflictingPriorty(l1, l2) != null;
    }
    
    public boolean important(Lecture l1, Lecture l2) {
        JenrlConstraint jenrl = (l1 == null || l2 == null ? null : l1.jenrlConstraint(l2));
        return jenrl != null && jenrl.priority() > 0.0; 
    }
    
    @Override
    public void incJenrl(Assignment<Lecture, Placement> assignment, JenrlConstraint jenrl, double studentWeight, Double conflictPriority, Student student) {
        if (isApplicable(student, jenrl.first(), jenrl.second()) && inConflict(assignment.getValue(jenrl.first()), assignment.getValue(jenrl.second())) && conflictPriority != null)
            inc(assignment, studentWeight * conflictPriority);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.ImportantStudentConflictWeight", 3.0 * config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrImportantStudConfsWeight";
    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        double conf = getValue(assignment);
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(ImportantStudentHardConflict.class);
            double hard = (c == null ? 0.0 : c.getValue(assignment));
            info.put("Important student conflicts", sDoubleFormat.format(conf) + (hard > 0.0 ? " [hard: " + sDoubleFormat.format(hard) + "]" : ""));
        }
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(assignment, info, variables);
        double conf = getValue(assignment, variables);
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(ImportantStudentHardConflict.class);
            double hard = (c == null ? 0.0 : c.getValue(assignment, variables));
            info.put("Important student conflicts", sDoubleFormat.format(conf) + (hard > 0.0 ? " [hard: " + sDoubleFormat.format(hard) + "]" : ""));
        }
    }

}
