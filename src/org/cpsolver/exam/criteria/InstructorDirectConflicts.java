package org.cpsolver.exam.criteria;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamInstructor;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Number of direct instructor conflicts. I.e., number of cases when an
 * exam is attended by an instructor that attends some other exam at the
 * same period.
 * <br><br>
 * Direct instructor conflict weight can be set by problem property
 * Exams.InstructorDirectConflictWeight, or in the input xml file, property
 * instructorDirectConflictWeight.
 * 
 * <br>
 * 
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class InstructorDirectConflicts extends StudentDirectConflicts {

    @Override
    public String getWeightName() {
        return "Exams.InstructorDirectConflictWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "instructorDirectConflictWeight";
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int penalty = 0;
        Map<ExamInstructor, Set<Exam>> instructors = ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, value.getPeriod());
        for (ExamInstructor s : exam.getInstructors()) {
            Set<Exam> exams = instructors.get(s);
            if (exams == null) continue;
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 1)
                penalty++;
        }
        /*
        for (ExamInstructor s : exam.getInstructors()) {
            Set<Exam> exams = s.getExams(assignment, value.getPeriod());
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 1)
                penalty++;
        }
        */
        return penalty;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> variables) {
        int ret = 0;
        ExamModel m = (ExamModel)getModel();
        for (ExamPeriod p: m.getPeriods()) {
            Map<ExamInstructor, Set<Exam>> instructors = ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, p);
            for (Set<Exam> exams: instructors.values()) {
                int nrExams = exams.size();
                if (nrExams > 1)
                    ret += nrExams - 1;
            }
        }
        return ret;
    }

    @Override
    public String getName() {
        return "Instructor Direct Conflicts";
    }
    
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        InstructorNotAvailableConflicts na = (InstructorNotAvailableConflicts)getModel().getCriterion(InstructorNotAvailableConflicts.class);
        if (getValue(assignment) != 0.0 || (na != null && na.getValue(assignment) != 0.0))
            info.put(getName(), sDoubleFormat.format(getValue(assignment) + (na == null ? 0.0 : na.getValue(assignment))) +
                    (na == null || na.getValue(assignment) == 0.0 ? "" : " (" + sDoubleFormat.format(na.getValue(assignment)) + " N/A)"));
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "iDC:" + sDoubleFormat.format(getValue(assignment));
    }
}