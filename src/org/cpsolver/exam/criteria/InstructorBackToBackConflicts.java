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
 * Number of back-to-back instructor conflicts. I.e., number of cases when
 * an exam is attended by an instructor that attends some other exam at
 * the previous {@link ExamPeriod#prev()} or following
 * {@link ExamPeriod#next()} period. If
 * {@link StudentBackToBackConflicts#isDayBreakBackToBack()} is false, back-to-back conflicts
 * are only considered between consecutive periods that are of the same day.
 * <br><br>
 * Back-to-back instructor conflict weight can be set by problem property
 * Exams.InstructorBackToBackConflictWeight, or in the input xml file,
 * property instructorBackToBackConflictWeight.
 * 
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
public class InstructorBackToBackConflicts extends StudentBackToBackConflicts {

    @Override
    public String getWeightName() {
        return "Exams.InstructorBackToBackConflictWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "instructorBackToBackConflictWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 10.0;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int penalty = 0;
        ExamPeriod period = value.getPeriod();
        Map<ExamInstructor, Set<Exam>> prev = (period.prev() != null && (isDayBreakBackToBack() || period.prev().getDay() == period.getDay()) ? ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, period.prev()) : null);
        Map<ExamInstructor, Set<Exam>> next = (period.next() != null && (isDayBreakBackToBack() || period.next().getDay() == period.getDay()) ? ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, period.next()) : null);
        for (ExamInstructor s : exam.getInstructors()) {
            if (prev != null) {
                Set<Exam> exams = prev.get(s);
                if (exams != null) {
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
            if (next != null) {
                Set<Exam> exams = next.get(s);
                if (exams != null) {
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
        }
        /*
        for (ExamInstructor s : exam.getInstructors()) {
            if (period.prev() != null) {
                if (isDayBreakBackToBack() || period.prev().getDay() == period.getDay()) {
                    Set<Exam> exams = s.getExams(assignment, period.prev());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
            if (period.next() != null) {
                if (isDayBreakBackToBack() || period.next().getDay() == period.getDay()) {
                    Set<Exam> exams = s.getExams(assignment, period.next());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
        }
        */
        return penalty;
    }

    @Override
    public String getName() {
        return "Instructor Back-To-Back Conflicts";
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "iBTB:" + sDoubleFormat.format(getValue(assignment));
    }

}
