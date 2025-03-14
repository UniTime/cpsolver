package org.cpsolver.exam.criteria.additional;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.exam.criteria.ExamCriterion;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPeriodPlacement;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;


/**
 * Experimental criterion counting violations of periods assignments. If this
 * criterion is enabled, any period can be assigned to an exam (not only those that are
 * in the domain of the exam).
 * <br><br>
 * To enable assignment of prohibited periods, set parameter Exam.SoftPeriods to
 * a weight that should be inferred by a prohibited period assignment.
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
public class PeriodViolation extends ExamCriterion {
    
    @Override
    public String getWeightName() {
        return "Exam.SoftPeriods";
    }
    
    @Override
    public String getXmlWeightName() {
        return "softPeriods";
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        return (value.getPeriodPlacement().getExamPenalty() == getWeight() || value.getPeriodPlacement().getPeriod().getPenalty() == getWeight() ? 1.0 : 0.0);
    }
    
    @Override
    public double[] getBounds(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Exam exam : variables) {
            if (!exam.getPeriodPlacements().isEmpty()) {
                for (ExamPeriodPlacement periodPlacement : exam.getPeriodPlacements()) {
                    if (periodPlacement.getExamPenalty() == getWeight() || periodPlacement.getPeriod().getPenalty() == getWeight()) {
                        bounds[1] ++; break;
                    }
                }
            }
        }
        return bounds;
    }

    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return (getValue(assignment) <= 0.0 ? "" : "!P:" + sDoubleFormat.format(getValue(assignment)));
    }

}