package org.cpsolver.exam.reports;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.exam.reports.ExamModelReportGenerator;

/**
 * Export distribution of number of students by number of meetings per day into
 * a CSV file. <br>
 * <br>
 * Usage:
 * 
 * <pre>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamNbrMeetingsPerDay(model).report().save(file);
 * </code>
 * </pre>
 * 
 * <br>
 * 
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href=
 *          "http://muller.unitime.org">http://muller.unitime.org</a><br>
 *          <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 *          <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 *          <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see <a href=
 *          'http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class ExamNbrMeetingsPerDay {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *                  examination timetabling model
     */
    public ExamNbrMeetingsPerDay(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     * 
     * @param assignment
     *                       current assignment
     * @return resultant report
     */
    public CSVFile report(Assignment<Exam, ExamPlacement> assignment) {
        ExamModelReportGenerator reportGenerator = new ExamModelReportGenerator(iModel);
        return reportGenerator.generateReport(assignment);
    }
}