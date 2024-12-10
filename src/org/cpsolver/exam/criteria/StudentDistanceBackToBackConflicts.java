package org.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


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
public class StudentDistanceBackToBackConflicts extends ExamCriterion {
    private double iBackToBackDistance = -1;
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iBackToBackDistance = properties.getPropertyDouble("Exams.BackToBackDistance", iBackToBackDistance);
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
     * @return back-to-back distance in meters
     */
    public double getBackToBackDistance() {
        return iBackToBackDistance;
    }
    
    /**
     * Back-to-back distance. Can be set by
     * problem property Exams.BackToBackDistance, or in the input xml file,
     * property backToBackDistance)
     * @param backToBackDistance back-to-back distance in meters
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
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        if (getBackToBackDistance() < 0) return 0;
        int penalty = 0;
        ExamPeriod period = value.getPeriod();
        Map<ExamStudent, Set<Exam>> prev = (period.prev() != null && period.prev().getDay() == period.getDay() ? ((ExamModel)getModel()).getStudentsOfPeriod(assignment, period.prev()) : null);
        Map<ExamStudent, Set<Exam>> next = (period.next() != null && period.next().getDay() == period.getDay() ? ((ExamModel)getModel()).getStudentsOfPeriod(assignment, period.next()) : null);
        for (ExamStudent s : exam.getStudents()) {
            if (prev != null) {
                Set<Exam> exams = prev.get(s);
                if (exams != null)
                    for (Exam x : exams) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > getBackToBackDistance())
                            penalty++;
                    }
            }
            if (next != null) {
                Set<Exam> exams = next.get(s);
                if (exams != null)
                    for (Exam x : exams) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > getBackToBackDistance())
                            penalty++;
                    }
            }
        }
        /*
        for (ExamStudent s : exam.getStudents()) {
            if (period.prev() != null) {
                if (period.prev().getDay() == period.getDay()) {
                    for (Exam x : s.getExams(assignment, period.prev())) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > getBackToBackDistance())
                            penalty++;
                    }
                }
            }
            if (period.next() != null) {
                if (period.next().getDay() == period.getDay()) {
                    for (Exam x : s.getExams(assignment, period.next())) {
                        if (x.equals(exam))
                            continue;
                        if (value.getDistanceInMeters(assignment.getValue(x)) > getBackToBackDistance())
                            penalty++;
                    }
                }
            }
        }
        */
        return penalty;
    }
    
    @Override
    public String getName() {
        return "Distance Back-To-Back Conflicts";
    }
    
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getBackToBackDistance() >= 0.0 && getValue(assignment) != 0.0)
            info.put(getName(), sDoubleFormat.format(getValue(assignment)));
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return (getValue(assignment) <= 0.0 ? "" : "BTBd:" + sDoubleFormat.format(getValue(assignment)));
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}
