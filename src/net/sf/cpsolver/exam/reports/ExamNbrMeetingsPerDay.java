package net.sf.cpsolver.exam.reports;


import java.util.ArrayList;
import java.util.List;

import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export distribution of number of students by number of meetings per day into
 * a CSV file. <br>
 * <br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamNbrMeetingsPerDay(model).report().save(file);
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
public class ExamNbrMeetingsPerDay {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamNbrMeetingsPerDay(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        List<CSVField> header = new ArrayList<CSVField>();
        header.add(new CSVField("Date"));
        header.add(new CSVField("None"));
        for (int i = 1; i <= 5; i++)
            header.add(new CSVField(i == 5 ? "5+" : String.valueOf(i)));
        header.add(new CSVField("Back-To-Back"));
        csv.setHeader(header);
        int[] nrExamsTotal = new int[6];
        for (int i = 0; i <= 5; i++)
            nrExamsTotal[i] = 0;
        int btbTotal = 0;
        for (int d = 0; d < iModel.getNrDays(); d++) {
            ExamPeriod period = null;
            for (ExamPeriod p : iModel.getPeriods()) {
                if (p.getDay() == d) {
                    period = p;
                    break;
                }
            }
            int[] nrExams = new int[6];
            for (int i = 0; i <= 5; i++)
                nrExams[i] = 0;
            int btb = 0;
            for (ExamStudent student : iModel.getStudents()) {
                int ex = student.getExamsADay(d).size();
                nrExams[ex <= 5 ? ex : 5]++;
                ExamPeriod p = period;
                while (p.next() != null && (iModel.isDayBreakBackToBack() ? p : p.next()).getDay() == d) {
                    btb += student.getExams(p).size() * student.getExams(p.next()).size();
                    p = p.next();
                }
            }
            List<CSVField> line = new ArrayList<CSVField>();
            line.add(new CSVField(period.getDayStr()));
            for (int i = 0; i <= 5; i++) {
                line.add(new CSVField(nrExams[i]));
                nrExamsTotal[i] += nrExams[i];
            }
            line.add(new CSVField(btb));
            btbTotal += btb;
            csv.addLine(line);
        }
        List<CSVField> line = new ArrayList<CSVField>();
        line.add(new CSVField("Total"));
        for (int i = 0; i <= 5; i++)
            line.add(new CSVField(nrExamsTotal[i]));
        line.add(new CSVField(btbTotal));
        csv.addLine(line);
        return csv;
    }
}
