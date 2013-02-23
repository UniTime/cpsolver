package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Rotation penalty. I.e., an exam that has been in later period last times tries
 * to be in an earlier period.
 * <br><br>
 * A weight for exam rotation penalty can be set by problem property
 * Exams.RotationWeight, or in the input xml file, property examRotationWeight.
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
public class ExamRotationPenalty extends ExamCriterion {
    private int iAssignedExamsWithAvgPeriod = 0;
    private double iAveragePeriod = 0.0;
    
    @Override
    public String getWeightName() {
        return "Exams.RotationWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "examRotationWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.001;
    }

    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        if (value.variable().getAveragePeriod() < 0) return 0;
        return (1 + value.getPeriod().getIndex()) * (1 + value.variable().getAveragePeriod());
    }
    
    @Override
    public void beforeUnassigned(long iteration, ExamPlacement value) {
        super.beforeUnassigned(iteration, value);
        if (value.variable().getAveragePeriod() >= 0) {
            iAssignedExamsWithAvgPeriod --;
            iAveragePeriod -= value.variable().getAveragePeriod();
        }
    }
    
    @Override
    public void afterAssigned(long iteration, ExamPlacement value) {
        super.afterAssigned(iteration, value);
        if (value.variable().getAveragePeriod() >= 0) {
            iAssignedExamsWithAvgPeriod ++;
            iAveragePeriod += value.variable().getAveragePeriod();
        }
    }
    
    public int nrAssignedExamsWithAvgPeriod() {
        return iAssignedExamsWithAvgPeriod;
    }
    
    public double averagePeriod() {
        return iAveragePeriod / iAssignedExamsWithAvgPeriod;
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        if (getValue() != 0.0) {
            info.put(getName(), sDoubleFormat.format(Math.sqrt(getValue() / iAssignedExamsWithAvgPeriod) - 1));
        }
    }
    
    @Override
    public String toString() {
        return "@P:" + sDoubleFormat.format((Math.sqrt(getValue() / iAssignedExamsWithAvgPeriod) - 1));
    }
}
