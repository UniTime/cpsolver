package org.cpsolver.exam.criteria;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamInstructor;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;


/**
 * Number of direct instructor conflicts caused by the fact that an instructor is
 * not available.
 * <br><br>
 * Direct instructor conflict weight can be set by problem property
 * Exams.InstructorDirectConflictWeight, or in the input xml file, property
 * instructorDirectConflictWeight.
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
public class InstructorNotAvailableConflicts extends InstructorDirectConflicts {

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        // if (!exam.isAllowDirectConflicts()) return 0;
        int penalty = 0;
        for (ExamInstructor s : exam.getInstructors()) {
            if (!s.isAvailable(value.getPeriod()))
                penalty++;
        }
        return penalty;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> exams) {
        double ret = 0;
        for (Exam exam: exams) {
            ExamPlacement placement = assignment.getValue(exam);
            if (placement != null) ret += getValue(assignment, placement, null);
        }
        return ret;
    }

    @Override
    public String getName() {
        return "Instructor Not Available Conflicts";
    }
    
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
    }

    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return (getValue(assignment) <= 0.0 ? "" : "iNA:" + sDoubleFormat.format(getValue(assignment)));
    }
}