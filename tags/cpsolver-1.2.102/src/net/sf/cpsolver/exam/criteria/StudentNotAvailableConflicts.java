package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;

/**
 * Number of direct student conflicts caused by the fact that a student is
 * not available.
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
public class StudentNotAvailableConflicts extends StudentDirectConflicts {

    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            if (!s.isAvailable(value.getPeriod()))
                penalty++;
        }
        return penalty;
    }

    @Override
    public String getName() {
        return "Not Available Conflicts";
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
    }

    @Override
    public String toString() {
        return "NA:" + sDoubleFormat.format(getWeightedValue());
    }

}