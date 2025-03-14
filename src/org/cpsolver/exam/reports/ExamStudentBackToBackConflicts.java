package org.cpsolver.exam.reports;

import java.text.DecimalFormat;
import java.util.List;

import org.cpsolver.exam.criteria.StudentBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentDistanceBackToBackConflicts;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.CSVFile.CSVField;


/**
 * Export student back-to-back conflicts between pairs of exams into a CSV file. <br>
 * <br>
 * Usage:
 * <pre><code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamStudentBackToBackConflicts(model).report().save(file);
 * </code></pre>
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
public class ExamStudentBackToBackConflicts {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamStudentBackToBackConflicts(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     * @param assignment current assignment
     * @return resultant report
     */
    public CSVFile report(Assignment<Exam, ExamPlacement> assignment) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Exam 1"), new CSVField("Enrl 1"), new CSVField("Period 1"),
                new CSVField("Date 1"), new CSVField("Time 1"), new CSVField("Exam 2"), new CSVField("Enrl 2"),
                new CSVField("Back-To-Back"), new CSVField("Back-To-Back [%]"), new CSVField("Distance") });
        DecimalFormat df = new DecimalFormat("0.0");
        boolean isDayBreakBackToBack = ((StudentBackToBackConflicts)iModel.getCriterion(StudentBackToBackConflicts.class)).isDayBreakBackToBack();
        double backToBackDistance = ((StudentDistanceBackToBackConflicts)iModel.getCriterion(StudentDistanceBackToBackConflicts.class)).getBackToBackDistance();
        for (Exam ex1 : iModel.variables()) {
            ExamPlacement p1 = assignment.getValue(ex1);
            if (p1 == null || p1.getPeriod().next() == null)
                continue;
            if (!isDayBreakBackToBack && p1.getPeriod().getDay() != p1.getPeriod().next().getDay())
                continue;
            for (Exam ex2 : iModel.variables()) {
                ExamPlacement p2 = assignment.getValue(ex2);
                if (p2 == null || !p2.getPeriod().equals(p1.getPeriod().next()))
                    continue;
                List<ExamStudent> students = ex1.getJointEnrollments().get(ex2);
                if (students == null || students.isEmpty())
                    continue;
                String distStr = "";
                if (backToBackDistance >= 0) {
                    double dist = p1.getDistanceInMeters(p2);
                    if (dist > 0)
                        distStr = String.valueOf(dist);
                }
                csv
                        .addLine(new CSVField[] {
                                new CSVField(ex1.getName()),
                                new CSVField(ex1.getStudents().size()),
                                new CSVField(p1.getPeriod().getIndex() + 1),
                                new CSVField(p1.getPeriod().getDayStr()),
                                new CSVField(p1.getPeriod().getTimeStr()),
                                new CSVField(ex2.getName()),
                                new CSVField(ex2.getStudents().size()),
                                new CSVField(students.size()),
                                new CSVField(df.format(100.0 * students.size()
                                        / Math.min(ex1.getStudents().size(), ex2.getStudents().size()))),
                                new CSVField(distStr) });
            }
        }
        return csv;
    }
}
