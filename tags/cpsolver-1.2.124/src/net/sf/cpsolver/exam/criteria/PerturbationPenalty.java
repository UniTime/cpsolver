package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Perturbation penalty. I.e., penalty for using a different examination period than
 * initial. Only applicable when {@link PerturbationPenalty#isMPP()} is true (minimal
 * perturbation problem).
 * <br><br>
 * A weight of perturbations (i.e., a penalty for an
 * assignment of an exam to a place different from the initial one) can be
 * set by problem property Exams.PerturbationWeight, or in the input xml
 * file, property perturbationWeight).
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
public class PerturbationPenalty extends ExamCriterion {
    private boolean iMPP = false;
    
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        boolean ret = super.init(solver);
        iMPP = solver.getProperties().getPropertyBoolean("General.MPP", iMPP);
        return ret;
    }
    
    @Override
    public String getWeightName() {
        return "Exams.PerturbationWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "perturbationWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.01;
    }
    
    public boolean isMPP() {
        return iMPP;
    }
    
    @Override
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
        params.put("mpp", isMPP() ? "true" : "false");
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        try {
            setWeight(Double.valueOf(params.get(getXmlWeightName())));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            iMPP = "true".equals(params.get("mpp"));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
    }

    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        if (!isMPP()) return 0;
        Exam exam = value.variable();
        ExamPlacement initial = exam.getInitialAssignment();
        if (initial == null) return 0;
        return Math.abs(initial.getPeriod().getIndex() - value.getPeriod().getIndex()) * (1 + exam.getSize());
    }

    @Override
    public String toString() {
        return (isMPP() ? "IP:" + sDoubleFormat.format(getValue()) : "");
    }
}