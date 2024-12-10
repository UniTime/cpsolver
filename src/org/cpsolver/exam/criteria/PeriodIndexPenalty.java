package org.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Average index of the assigned period.
 * <br><br>
 * A weight for period index can be set by problem property
 * Exams.PeriodIndexWeight, or in the input xml file, property periodIndexWeight.
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
public class PeriodIndexPenalty extends ExamCriterion {
    
    @Override
    public String getWeightName() {
        return "Exams.PeriodIndexWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "periodIndexWeight";
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.0000001;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        return value.getPeriod().getIndex();
    }
    
    @Override
    public String getName() {
        return "Average Period";
    }

    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0) {
            info.put(getName(), sDoubleFormat.format(getValue(assignment) / assignment.nrAssignedVariables()));
        }
    }

    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "PI:" + sDoubleFormat.format(getValue(assignment) / assignment.nrAssignedVariables());
    }
}