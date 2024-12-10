package org.cpsolver.exam.criteria;

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
 * Number of back-to-back distance instructor conflicts. I.e., number of
 * cases when an exam is attended by an instructor that attends some other
 * exam at the previous {@link ExamPeriod#prev()} or following
 * {@link ExamPeriod#next()} period and the distance
 * {@link ExamPlacement#getDistanceInMeters(ExamPlacement)} between these two exams
 * is greater than {@link ExamModel#getBackToBackDistance()}. Distance
 * back-to-back conflicts are only considered between consecutive periods
 * that are of the same day.
 * <br><br>
 * Distance back-to-back instructor conflict weight can be set by problem
 * property Exams.InstructorDistanceBackToBackConflictWeight, or in the
 * input xml file, property instructorDistanceBackToBackConflictWeight.
 * 
 * <br>
 * 
 * @author  Tomas Muller
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
public class InstructorDistanceBackToBackConflicts extends StudentDistanceBackToBackConflicts {
    
    @Override
    public String getWeightName() {
        return "Exams.InstructorDistanceBackToBackConflictWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "instructorDistanceBackToBackConflictWeight";
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 25.0;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        double btbDist = getBackToBackDistance();
        if (btbDist < 0)
            return 0;
        int penalty = 0;
        ExamPeriod period = value.getPeriod();
        Map<ExamInstructor, Set<Exam>> prev = (period.prev() != null && period.prev().getDay() == period.getDay() ? ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, period.prev()) : null);
        Map<ExamInstructor, Set<Exam>> next = (period.next() != null && period.next().getDay() == period.getDay() ? ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, period.next()) : null);
        for (ExamInstructor s : exam.getInstructors()) {
            if (prev != null) {
                Set<Exam> exams = prev.get(s);
                if (exams != null)
                    for (Exam x : exams) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > getBackToBackDistance())
                            penalty++;
                    }
            }
            if (next != null) {
                Set<Exam> exams = next.get(s);
                if (exams != null)
                    for (Exam x : exams) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > getBackToBackDistance())
                            penalty++;
                    }
            }
        }
        /*
        for (ExamInstructor s : exam.getInstructors()) {
            if (period.prev() != null) {
                if (period.prev().getDay() == period.getDay()) {
                    for (Exam x : s.getExams(assignment, period.prev())) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > btbDist)
                            penalty++;
                    }
                }
            }
            if (period.next() != null) {
                if (period.next().getDay() == period.getDay()) {
                    for (Exam x : s.getExams(assignment, period.next())) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > btbDist)
                            penalty++;
                    }
                }
            }
        }
        */
        return penalty;
    }

    @Override
    public String getName() {
        return "Instructor Distance Back-To-Back Conflicts";
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return (getValue(assignment) <= 0.0 ? "" : "iBTBd:" + sDoubleFormat.format(getValue(assignment)));
    }
    
    @Override
    public boolean isPeriodCriterion() { return false; }
}
