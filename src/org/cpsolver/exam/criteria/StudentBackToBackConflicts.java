package org.cpsolver.exam.criteria;

import java.util.Collection;
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
 * Number of back-to-back student conflicts. I.e., number of cases when
 * an exam is attended by a student that attends some other exam at
 * the previous {@link ExamPeriod#prev()} or following
 * {@link ExamPeriod#next()} period. If
 * {@link StudentBackToBackConflicts#isDayBreakBackToBack()} is false, back-to-back conflicts
 * are only considered between consecutive periods that are of the same day.
 * <br><br>
 * Back-to-back student conflict weight can be set by problem property
 * Exams.BackToBackConflictWeight, or in the input xml file,
 * property backToBackConflictWeight.
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
public class StudentBackToBackConflicts extends ExamCriterion {
    private boolean iDayBreakBackToBack = false;
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iDayBreakBackToBack = properties.getPropertyBoolean("Exams.IsDayBreakBackToBack", iDayBreakBackToBack);
    }
        
    @Override
    public String getWeightName() {
        return "Exams.BackToBackConflictWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "backToBackConflictWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 10.0;
    }

    /**
     * True when back-to-back student conflict is to be encountered when a
     * student is enrolled into an exam that is on the last period of one day
     * and another exam that is on the first period of the consecutive day. It
     * can be set by problem property Exams.IsDayBreakBackToBack, or in the
     * input xml file, property isDayBreakBackToBack)
     * @return true if last exam on one day is back-to-back to the first exam of the following day
     */
    public boolean isDayBreakBackToBack() {
        return iDayBreakBackToBack;
    }
    
    /**
     * True when back-to-back student conflict is to be encountered when a
     * student is enrolled into an exam that is on the last period of one day
     * and another exam that is on the first period of the consecutive day. It
     * can be set by problem property Exams.IsDayBreakBackToBack, or in the
     * input xml file, property isDayBreakBackToBack)
     * @param dayBreakBackToBack true if last exam on one day is back-to-back to the first exam of the following day
     * 
     */
    public void setDayBreakBackToBack(boolean dayBreakBackToBack) {
        iDayBreakBackToBack = dayBreakBackToBack;
    }
    
    @Override
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
        params.put("isDayBreakBackToBack", isDayBreakBackToBack() ? "true" : "false");
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        try {
            setWeight(Double.valueOf(params.get(getXmlWeightName())));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setDayBreakBackToBack("true".equals(params.get("isDayBreakBackToBack")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int penalty = 0;
        ExamPeriod period = value.getPeriod();
        Map<ExamStudent, Set<Exam>> prev = (period.prev() != null && (isDayBreakBackToBack() || period.prev().getDay() == period.getDay()) ? ((ExamModel)getModel()).getStudentsOfPeriod(assignment, period.prev()) : null);
        Map<ExamStudent, Set<Exam>> next = (period.next() != null && (isDayBreakBackToBack() || period.next().getDay() == period.getDay()) ? ((ExamModel)getModel()).getStudentsOfPeriod(assignment, period.next()) : null);
        for (ExamStudent s : exam.getStudents()) {
            if (prev != null) {
                Set<Exam> exams = prev.get(s);
                if (exams != null) {
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
            if (next != null) {
                Set<Exam> exams = next.get(s);
                if (exams != null) {
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
        }
        /*
        for (ExamStudent s : exam.getStudents()) {
            if (period.prev() != null) {
                if (isDayBreakBackToBack() || period.prev().getDay() == period.getDay()) {
                    Set<Exam> exams = s.getExams(assignment, period.prev());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
            if (period.next() != null) {
                if (isDayBreakBackToBack() || period.next().getDay() == period.getDay()) {
                    Set<Exam> exams = s.getExams(assignment, period.next());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
        }
        */
        return penalty;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> variables) {
        return super.getValue(assignment, variables) / 2.0;
    }
    
    @Override
    public String getName() {
        return "Back-To-Back Conflicts";
    }
    
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0)
            info.put(getName(), sDoubleFormat.format(getValue(assignment)));
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "BTB:" + sDoubleFormat.format(getValue(assignment));
    }
}
