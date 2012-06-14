package net.sf.cpsolver.exam.reports;

import java.text.DecimalFormat;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export student direct, back-to-back, and more than two exams a day conflicts
 * summarized for each exam into a CSV file. <br>
 * <br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamStudentConflictsPerExam(model).report().save(file);
 * </code> <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
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
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Exam"), new CSVField("Enrl"), new CSVField("Direct"),
                new CSVField("Direct [%]"), new CSVField("More-2-Day"), new CSVField("More-2-Day [%]"),
                new CSVField("Back-To-Back"), new CSVField("Back-To-Back [%]"), new CSVField("Dist Back-To-Back"),
                new CSVField("Dist Back-To-Back [%]") });
        DecimalFormat df = new DecimalFormat("0.0");
        for (Exam exam : iModel.variables()) {
            ExamPlacement placement = exam.getAssignment();
            if (placement == null)
                continue;
            if (placement.getNrDirectConflicts() == 0 && placement.getNrMoreThanTwoADayConflicts() == 0
                    && placement.getNrBackToBackConflicts() == 0 && placement.getNrDistanceBackToBackConflicts() == 0)
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
                    new CSVField(placement.getNrDirectConflicts()),
                    new CSVField(df.format(100.0 * placement.getNrDirectConflicts() / exam.getStudents().size())),
                    new CSVField(placement.getNrMoreThanTwoADayConflicts()),
                    new CSVField(df.format(100.0 * placement.getNrMoreThanTwoADayConflicts()
                            / exam.getStudents().size())),
                    new CSVField(placement.getNrBackToBackConflicts()),
                    new CSVField(df.format(100.0 * placement.getNrBackToBackConflicts() / exam.getStudents().size())),
                    new CSVField(placement.getNrDistanceBackToBackConflicts()),
                    new CSVField(df.format(100.0 * placement.getNrDistanceBackToBackConflicts()
                            / exam.getStudents().size())) });
        }
        return csv;
    }
}
