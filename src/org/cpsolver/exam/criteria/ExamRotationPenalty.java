package org.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


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
public class ExamRotationPenalty extends ExamCriterion {
    
    @Override
    public ValueContext createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new RotationContext(assignment);
    }
        
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
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        if (value.variable().getAveragePeriod() < 0) return 0;
        return (1 + value.getPeriod().getIndex()) * (1 + value.variable().getAveragePeriod());
    }
    
    public int nrAssignedExamsWithAvgPeriod(Assignment<Exam, ExamPlacement> assignment) {
        return ((RotationContext)getContext(assignment)).nrAssignedExamsWithAvgPeriod();
    }
    
    public double averagePeriod(Assignment<Exam, ExamPlacement> assignment) {
        return ((RotationContext)getContext(assignment)).averagePeriod();
    }
    
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0) {
            info.put(getName(), sDoubleFormat.format(Math.sqrt(getValue(assignment) / nrAssignedExamsWithAvgPeriod(assignment)) - 1));
        }
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "@P:" + sDoubleFormat.format((Math.sqrt(getValue(assignment) / nrAssignedExamsWithAvgPeriod(assignment)) - 1));
    }
    
    protected class RotationContext extends ValueContext {
        private int iAssignedExamsWithAvgPeriod = 0;
        private double iAveragePeriod = 0.0;

        public RotationContext(Assignment<Exam, ExamPlacement> assignment) {
            super(assignment);
            for (Exam exam: getModel().variables())
                if (exam.getAveragePeriod() > 0) {
                    ExamPlacement placement = assignment.getValue(exam);
                    if (placement != null) {
                        iAssignedExamsWithAvgPeriod ++;
                        iAveragePeriod += exam.getAveragePeriod();
                    }
                }
        }

        @Override
        public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
            super.assigned(assignment, value);
            if (value.variable().getAveragePeriod() >= 0) {
                iAssignedExamsWithAvgPeriod ++;
                iAveragePeriod += value.variable().getAveragePeriod();
            }
        }

        @Override
        public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
            super.unassigned(assignment, value);
            if (value.variable().getAveragePeriod() >= 0) {
                iAssignedExamsWithAvgPeriod --;
                iAveragePeriod -= value.variable().getAveragePeriod();
            }
        }
        
        public int nrAssignedExamsWithAvgPeriod() {
            return iAssignedExamsWithAvgPeriod;
        }
        
        public double averagePeriod() {
            return iAveragePeriod / iAssignedExamsWithAvgPeriod;
        }
    }
}
