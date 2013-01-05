package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Number of direct student conflicts. I.e., number of cases when an
 * exam is attended by a student that attends some other exam at the
 * same period.
 * <br><br>
 * Direct student conflict weight can be set by problem property
 * Exams.DirectConflictWeight, or in the input xml file, property
 * directConflictWeight.
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
public class StudentDirectConflicts extends ExamCriterion {

    @Override
    public String getWeightName() {
        return "Exams.DirectConflictWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "directConflictWeight";
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }
    
    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            Set<Exam> exams = s.getExams(value.getPeriod());
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 1)
                penalty++;
        }
        return penalty;
    }

    @Override
    public String getName() {
        return "Direct Conflicts";
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        StudentNotAvailableConflicts na = (StudentNotAvailableConflicts)getModel().getCriterion(StudentNotAvailableConflicts.class);
        if (getValue() != 0.0 || (na != null && na.getValue() != 0.0))
            info.put(getName(), sDoubleFormat.format(getValue() + (na == null ? 0.0 : na.getValue())) +
                    (na == null || na.getValue() == 0.0 ? "" : " (" + sDoubleFormat.format(na.getValue()) + " N/A)"));
    }

    @Override
    public String toString() {
        return "DC:" + sDoubleFormat.format(getValue());
    }
}
