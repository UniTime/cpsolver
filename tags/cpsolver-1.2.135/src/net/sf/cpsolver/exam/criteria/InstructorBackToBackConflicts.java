package net.sf.cpsolver.exam.criteria;

import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamInstructor;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.DataProperties;

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
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2012 Tomas Muller<br>
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
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int penalty = 0;
        for (ExamInstructor s : exam.getInstructors()) {
            if (value.getPeriod().prev() != null) {
                if (isDayBreakBackToBack() || value.getPeriod().prev().getDay() == value.getPeriod().getDay()) {
                    Set<Exam> exams = s.getExams(value.getPeriod().prev());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
            if (value.getPeriod().next() != null) {
                if (isDayBreakBackToBack() || value.getPeriod().next().getDay() == value.getPeriod().getDay()) {
                    Set<Exam> exams = s.getExams(value.getPeriod().next());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
        }
        return penalty;
    }

    @Override
    public String getName() {
        return "Instructor Back-To-Back Conflicts";
    }
    
    @Override
    public String toString() {
        return "iBTB:" + sDoubleFormat.format(getValue());
    }

}
