package net.sf.cpsolver.exam.criteria.additional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.exam.criteria.ExamCriterion;
import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamDistributionConstraint;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.assignment.Assignment;

/**
 * Experimental criterion counting violations of hard distribution constraints.
 * <br><br>
 * To enable breaking of hard distribution constraints, set parameter Exam.SoftDistributions to
 * a weight that should be inferred by a hard distribution constraint being broken.
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
public class DistributionViolation extends ExamCriterion {

    @Override
    public String getWeightName() {
        return "Exam.SoftDistributions";
    }
    
    @Override
    public String getXmlWeightName() {
        return "softDistributions";
    }

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        int penalty = 0;
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || getWeight() != dc.getWeight())
                continue;
            boolean sat = dc.isSatisfied(assignment, value);
            if (sat != dc.isSatisfied(assignment))
                penalty += (sat ? -1.0 : 1.0);
        }
        return penalty;
    }
    
    @Override
    protected double[] computeBounds(Assignment<Exam, ExamPlacement> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (ExamDistributionConstraint dc : ((ExamModel)getModel()).getDistributionConstraints()) {
            if (!dc.isHard() && getWeight() == dc.getWeight())
                bounds[1] ++;
        }
        return bounds;
    }
    
    @Override
    public boolean isRoomCriterion() { return true; }
    
    @Override
    public double getRoomValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
        int penalty = 0;
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || getWeight() != dc.getWeight() || !dc.isRoomRelated())
                continue;
            boolean sat = dc.isSatisfied(assignment, value);
            if (sat != dc.isSatisfied(assignment))
                penalty += (sat ? -1.0 : 1.0);
        }
        return penalty;
    }

    @Override
    public boolean isPeriodCriterion() { return true; }
    
    @Override
    public double getPeriodValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
        int penalty = 0;
        for (ExamDistributionConstraint dc : value.variable().getDistributionConstraints()) {
            if (dc.isHard() || getWeight() != dc.getWeight() || !dc.isPeriodRelated())
                continue;
            boolean sat = dc.isSatisfied(assignment, value);
            if (sat != dc.isSatisfied(assignment))
                penalty += (sat ? -1.0 : 1.0);
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
                    if (dc.isHard() || getWeight() != dc.getWeight())
                        continue;
                    if (!dc.isSatisfied(assignment))
                        penalty += 1;
                }
            }
        }
        return penalty;
    }

    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return (getValue(assignment) <= 0.0 ? "" : "!D:" + sDoubleFormat.format(getValue(assignment)));
    }
}
