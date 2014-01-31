package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Number of more than two exams a day student conflicts. I.e., when an
 * exam is attended by a student student that attends two or more other
 * exams at the same day.
 * <br><br>
 * More than two exams a day student conflict weight can be set by
 * problem property Exams.MoreThanTwoADayWeight, or in the input
 * xml file, property moreThanTwoADayWeight.
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
public class StudentMoreThan2ADayConflicts extends ExamCriterion {

    @Override
    public String getWeightName() {
        return "Exams.MoreThanTwoADayWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 100.0;
    }

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int penalty = 0;
        ExamPeriod period = value.getPeriod();
        Map<ExamStudent, Set<Exam>> students = ((ExamModel)getModel()).getStudentsOfDay(assignment, period);
        for (ExamStudent s : exam.getStudents()) {
            Set<Exam> exams = students.get(s);
            if (exams == null || exams.size() < 2) continue;
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 2)
                penalty++;
        }
        /*
        for (ExamStudent s : exam.getStudents()) {
            Set<Exam> exams = s.getExamsADay(assignment, period);
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 2)
                penalty++;
        }
        */
        return penalty;
    }
    
    @Override
    public String getName() {
        return "More Than 2 A Day Conflicts";
    }
    
    @Override
    public String getXmlWeightName() {
        return "moreThanTwoADayWeight";
    }

    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0)
            info.put(getName(), sDoubleFormat.format(getValue(assignment)));
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "M2D:" + sDoubleFormat.format(getValue(assignment));
    }
}
