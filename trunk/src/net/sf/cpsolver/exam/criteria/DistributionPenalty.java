package net.sf.cpsolver.exam.criteria;

import java.util.Set;

import net.sf.cpsolver.exam.criteria.additional.DistributionViolation;
import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamDistributionConstraint;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

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
public class DistributionPenalty extends ExamCriterion {
    protected Integer iSoftDistributions = null;
    
    public DistributionPenalty() {
        iValueUpdateType = ValueUpdateType.NoUpdate; 
    }
    
    
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        if (super.init(solver)) {
            iSoftDistributions = solver.getProperties().getPropertyInteger("Exam.SoftDistributions", null);
            if (iSoftDistributions != null) {
                DistributionViolation dv = new DistributionViolation();
                getModel().addCriterion(dv);
                return dv.init(solver);
            }
        }
        return true;
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
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        int penalty = 0;
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()))
                continue;
            boolean sat = dc.isSatisfied(value);
            if (sat != dc.isSatisfied())
                penalty += (sat ? -dc.getWeight() : dc.getWeight());
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
    public double getRoomValue(ExamPlacement value) {
        int penalty = 0;
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()) || !dc.isRoomRelated())
                continue;
            boolean sat = dc.isSatisfied(value);
            if (sat != dc.isSatisfied())
                penalty += (sat ? -dc.getWeight() : dc.getWeight());
        }
        return penalty;
    }

    
    @Override
    public boolean isPeriodCriterion() { return true; }
    
    @Override
    public void inc(double value) {
        if (iSoftDistributions != null && iSoftDistributions == value) {
            getModel().getCriterion(DistributionViolation.class).inc(1.0);
        } else if (iSoftDistributions != null && iSoftDistributions == -value) {
            getModel().getCriterion(DistributionViolation.class).inc(-1.0);
        } else {
            super.inc(value);
        }
    }
    
    /**
     * Period related distribution penalty, i.e., sum weights of violated
     * distribution constraints
     */
    @Override
    public double getPeriodValue(ExamPlacement value) {
        int penalty = 0;
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()) || !dc.isPeriodRelated())
                continue;
            boolean sat = dc.isSatisfied(value);
            if (sat != dc.isSatisfied())
                penalty += (sat ? -dc.getWeight() : dc.getWeight());
        }
        return penalty;
    }
    
    @Override
    protected double[] computeBounds() {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (ExamDistributionConstraint dc : ((ExamModel)getModel()).getDistributionConstraints()) {
            if (dc.isHard() || (iSoftDistributions != null && iSoftDistributions == dc.getWeight()))
                continue;
            bounds[1] += dc.getWeight();
        }
        return bounds;
    }

    @Override
    public String toString() {
        return (getValue() <= 0.0 ? "" : "DP:" + sDoubleFormat.format(getValue()));
    }
}
