package org.cpsolver.exam.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.exam.criteria.additional.DistributionViolation;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamDistributionConstraint;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Distribution penalty. I.e., sum weights of violated distribution
 * constraints.
 * <br><br>
 * A weight of violated distribution soft constraints (see
 * {@link ExamDistributionConstraint}) can be set by problem property
 * Exams.RoomDistributionWeight, or in the input xml file, property
 * roomDistributionWeight.
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
public class DistributionPenalty extends ExamCriterion {
    protected Integer iSoftDistributions = null;
    
    public DistributionPenalty() {
        setValueUpdateType(ValueUpdateType.NoUpdate); 
    }
    
    @Override
    public void setModel(Model<Exam, ExamPlacement> model) {
        super.setModel(model);
        iSoftDistributions = ((ExamModel)model).getProperties().getPropertyInteger("Exam.SoftDistributions", null);
        if (iSoftDistributions != null && model.getCriterion(DistributionViolation.class) == null) { 
            DistributionViolation dv = new DistributionViolation();
            model.addCriterion(dv);
        }
    }
    
    @Override
    public String getWeightName() {
        return "Exams.DistributionWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "distributionWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        int penalty = 0;
        ExamPlacement original = assignment.getValue(value.variable());
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()))
                continue;
            penalty += dc.countViolations(assignment, value) * dc.getWeight();
            if (original != null) penalty -= dc.countViolations(assignment, original) * dc.getWeight();
        }
        return penalty;
    }
    
    @Override
    public boolean isRoomCriterion() { return true; }
    
    /**
     * Room related distribution penalty, i.e., sum weights of violated
     * distribution constraints
     */
    @Override
    public double getRoomValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
        int penalty = 0;
        ExamPlacement original = assignment.getValue(value.variable());
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()) || !dc.isRoomRelated())
                continue;
            penalty += dc.countViolations(assignment, value) * dc.getWeight();
            if (original != null) penalty -= dc.countViolations(assignment, original) * dc.getWeight();
        }
        return penalty;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> variables) {
        int penalty = 0;
        Set<ExamDistributionConstraint> added = new HashSet<ExamDistributionConstraint>();
        for (Exam exam: variables) {
            for (ExamDistributionConstraint dc : exam.getDistributionConstraints()) {
                if (added.add(dc)) {
                    if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()))
                        continue;
                    penalty += dc.countViolations(assignment) * dc.getWeight();
                }
            }
        }
        return penalty;
    }

    @Override
    public boolean isPeriodCriterion() { return true; }
    
    public void inc(Assignment<Exam, ExamPlacement> assignment, double value, ExamDistributionConstraint dc) {
        if (iSoftDistributions != null && iSoftDistributions == dc.getWeight()) {
            getModel().getCriterion(DistributionViolation.class).inc(assignment, value / dc.getWeight());
        } else {
            super.inc(assignment, value);
        }
    }
    
    /**
     * Period related distribution penalty, i.e., sum weights of violated
     * distribution constraints
     */
    @Override
    public double getPeriodValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
        int penalty = 0;
        ExamPlacement original = assignment.getValue(value.variable());
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()) || !dc.isPeriodRelated())
                continue;
            penalty += dc.countViolations(assignment, value) * dc.getWeight();
            if (original != null) penalty -= dc.countViolations(assignment, original) * dc.getWeight();
        }
        return penalty;
    }
    
    @Override
    protected double[] computeBounds(Assignment<Exam, ExamPlacement> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (ExamDistributionConstraint dc : ((ExamModel)getModel()).getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()))
                continue;
            bounds[1] += dc.getWeight() * dc.variables().size() * (dc.variables().size() - 1) / 2;
        }
        return bounds;
    }

    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return (getValue(assignment) <= 0.0 ? "" : "DP:" + sDoubleFormat.format(getValue(assignment)));
    }
}
