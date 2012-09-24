package net.sf.cpsolver.exam.criteria;

import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Cost for using a period. See {@link ExamPeriodPlacement#getPenalty()} for more details.
 * <br><br>
 * A weight for period penalty can be set by problem property
 * Exams.PeriodWeight, or in the input xml file, property periodWeight.
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
public class PeriodPenalty extends ExamCriterion {
    
    @Override
    public String getWeightName() {
        return "Exams.PeriodWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        return value.getPeriodPlacement().getPenalty();
    }

    @Override
    protected void computeBounds() {
        iBounds = new double[] { 0.0, 0.0 };
        for (Exam exam : getModel().variables()) {
            if (!exam.getPeriodPlacements().isEmpty()) {
                int minPenalty = Integer.MAX_VALUE, maxPenalty = Integer.MIN_VALUE;
                for (ExamPeriodPlacement periodPlacement : exam.getPeriodPlacements()) {
                    minPenalty = Math.min(minPenalty, periodPlacement.getPenalty());
                    maxPenalty = Math.max(maxPenalty, periodPlacement.getPenalty());
                }
                iBounds[0] += minPenalty;
                iBounds[1] += maxPenalty;
            }
        }
    }
    
    @Override
    public String toString() {
        return "PP:" + sDoubleFormat.format(getWeightedValue());
    }
}
