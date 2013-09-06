package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Number of back-to-back distance student conflicts. I.e., number of
 * cases when an exam is attended by a student that attends some other
 * exam at the previous {@link ExamPeriod#prev()} or following
 * {@link ExamPeriod#next()} period and the distance
 * {@link ExamPlacement#getDistanceInMeters(ExamPlacement)} between these two exams
 * is greater than {@link ExamModel#getBackToBackDistance()}. Distance
 * back-to-back conflicts are only considered between consecutive periods
 * that are of the same day.
 * <br><br>
 * Distance back-to-back student conflict weight can be set by problem
 * property Exams.DistanceBackToBackConflictWeight, or in the
 * input xml file, property distanceBackToBackConflictWeight.
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
public class StudentDistanceBackToBackConflicts extends ExamCriterion {
    private double iBackToBackDistance = -1;
    
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        boolean ret = super.init(solver);
        iBackToBackDistance = solver.getProperties().getPropertyDouble("Exams.BackToBackDistance", iBackToBackDistance);
        return ret;
    }
    
    @Override
    public String getWeightName() {
        return "Exams.DistanceBackToBackConflictWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "distanceBackToBackConflictWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 25.0;
    }
    
    /**
     * Back-to-back distance. Can be set by
     * problem property Exams.BackToBackDistance, or in the input xml file,
     * property backToBackDistance)
     */
    public double getBackToBackDistance() {
        return iBackToBackDistance;
    }
    
    /**
     * Back-to-back distance. Can be set by
     * problem property Exams.BackToBackDistance, or in the input xml file,
     * property backToBackDistance)
     */
    public void setBackToBackDistance(double backToBackDistance) {
        iBackToBackDistance = backToBackDistance;
    }

    @Override
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
        params.put("backToBackDistance", String.valueOf(getBackToBackDistance()));
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        try {
            setWeight(Double.valueOf(params.get(getXmlWeightName())));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setBackToBackDistance(Double.valueOf(params.get("backToBackDistance")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
    }
    
    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        if (getBackToBackDistance() < 0) return 0;
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            if (value.getPeriod().prev() != null) {
                if (value.getPeriod().prev().getDay() == value.getPeriod().getDay()) {
                    for (Exam x : s.getExams(value.getPeriod().prev())) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(x.getAssignment()) > getBackToBackDistance())
                            penalty++;
                    }
                }
            }
            if (value.getPeriod().next() != null) {
                if (value.getPeriod().next().getDay() == value.getPeriod().getDay()) {
                    for (Exam x : s.getExams(value.getPeriod().next())) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(x.getAssignment()) > getBackToBackDistance())
                            penalty++;
                    }
                }
            }
        }
        return penalty;
    }
    
    @Override
    public String getName() {
        return "Distance Back-To-Back Conflicts";
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        if (getBackToBackDistance() >= 0.0 && getValue() != 0.0)
            info.put(getName(), sDoubleFormat.format(getValue()));
    }
    
    @Override
    public String toString() {
        return (getValue() <= 0.0 ? "" : "BTBd:" + sDoubleFormat.format(getValue()));
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}
