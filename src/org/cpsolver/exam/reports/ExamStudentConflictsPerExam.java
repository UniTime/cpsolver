package org.cpsolver.exam.reports;

import java.text.DecimalFormat;

import org.cpsolver.exam.criteria.StudentBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentDirectConflicts;
import org.cpsolver.exam.criteria.StudentDistanceBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentMoreThan2ADayConflicts;
import org.cpsolver.exam.criteria.StudentNotAvailableConflicts;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.CSVFile.CSVField;


/**
 * Export student direct, back-to-back, and more than two exams a day conflicts
 * summarized for each exam into a CSV file. <br>
 * <br>
 * Usage:
 * <pre><code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamStudentConflictsPerExam(model).report().save(file);
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
public class ExamStudentConflictsPerExam {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamStudentConflictsPerExam(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     * @param assignment current assignment
     * @return resultant report
     */
    public CSVFile report(Assignment<Exam, ExamPlacement> assignment) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Exam"), new CSVField("Enrl"), new CSVField("Direct"),
                new CSVField("Direct [%]"), new CSVField("More-2-Day"), new CSVField("More-2-Day [%]"),
                new CSVField("Back-To-Back"), new CSVField("Back-To-Back [%]"), new CSVField("Dist Back-To-Back"),
                new CSVField("Dist Back-To-Back [%]") });
        DecimalFormat df = new DecimalFormat("0.0");
        for (Exam exam : iModel.variables()) {
            ExamPlacement placement = assignment.getValue(exam);
            if (placement == null)
                continue;
            int dc = (int)iModel.getCriterion(StudentDirectConflicts.class).getValue(assignment, placement, null) +
                     (int)iModel.getCriterion(StudentNotAvailableConflicts.class).getValue(assignment, placement, null);
            int btb = (int)iModel.getCriterion(StudentBackToBackConflicts.class).getValue(assignment, placement, null);
            int dbtb = (int)iModel.getCriterion(StudentDistanceBackToBackConflicts.class).getValue(assignment, placement, null);
            int m2d = (int)iModel.getCriterion(StudentMoreThan2ADayConflicts.class).getValue(assignment, placement, null);
            if (dc == 0 && m2d == 0 && btb == 0 && dbtb == 0)
                continue;
            /*
             * String section = ""; for (Enumeration
             * f=exam.getCourseSections().elements();f.hasMoreElements();) {
             * ExamCourseSection cs = (ExamCourseSection)f.nextElement(); if
             * (section.length()>0) section+="\n"; section += cs.getName(); }
             */
            csv.addLine(new CSVField[] {
                    new CSVField(exam.getName()),
                    new CSVField(exam.getStudents().size()),
                    new CSVField(dc),
                    new CSVField(df.format(100.0 * dc / exam.getStudents().size())),
                    new CSVField(m2d),
                    new CSVField(df.format(100.0 * m2d / exam.getStudents().size())),
                    new CSVField(btb),
                    new CSVField(df.format(100.0 * btb / exam.getStudents().size())),
                    new CSVField(dbtb),
                    new CSVField(df.format(100.0 * dbtb / exam.getStudents().size())) });
        }
        return csv;
    }
}
