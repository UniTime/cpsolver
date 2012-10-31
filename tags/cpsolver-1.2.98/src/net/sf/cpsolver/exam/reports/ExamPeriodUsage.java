package net.sf.cpsolver.exam.reports;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export period usage into CSV file. <br>
 * <br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamPeriodUsage(model).report().save(file);
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
public class ExamPeriodUsage {
    private ExamModel iModel = null;
    /** Exam enrollment limits */
    public static int[] sLimits = new int[] { 10, 50, 100, 200 };
    private static DecimalFormat sDF = new DecimalFormat("0.00");

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamPeriodUsage(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        List<CSVField> header = new ArrayList<CSVField>();
        header.add(new CSVField("Period"));
        header.add(new CSVField("Date"));
        header.add(new CSVField("Time"));
        header.add(new CSVField("Weight"));
        header.add(new CSVField("NrExams"));
        header.add(new CSVField("Students"));
        for (int i = 0; i < sLimits.length; i++) {
            header.add(new CSVField("NrExams>=" + sLimits[i]));
        }
        header.add(new CSVField("AvgPeriod"));
        header.add(new CSVField("WgAvgPeriod"));
        csv.setHeader(header);
        for (ExamPeriod period : iModel.getPeriods()) {
            int nrExams = 0;
            int nrStudents = 0;
            int[] nrExamsLim = new int[sLimits.length];
            int totAvgPer = 0, nrAvgPer = 0, totWgAvgPer = 0;
            for (int i = 0; i < sLimits.length; i++)
                nrExamsLim[i] = 0;
            for (Exam exam : iModel.variables()) {
                ExamPlacement placement = exam.getAssignment();
                if (placement == null || !(placement.getPeriod().equals(period)))
                    continue;
                nrExams++;
                nrStudents += exam.getStudents().size();
                if (exam.getAveragePeriod() >= 0) {
                    totAvgPer += exam.getAveragePeriod();
                    nrAvgPer++;
                    totWgAvgPer += exam.getAveragePeriod() * exam.getStudents().size();
                }
                for (int i = 0; i < sLimits.length; i++)
                    if (exam.getStudents().size() >= sLimits[i])
                        nrExamsLim[i]++;
            }
            List<CSVField> line = new ArrayList<CSVField>();
            line.add(new CSVField(period.getIndex() + 1));
            line.add(new CSVField(period.getDayStr()));
            line.add(new CSVField(period.getTimeStr()));
            line.add(new CSVField(period.getPenalty()));
            line.add(new CSVField(nrExams));
            line.add(new CSVField(nrStudents));
            for (int i = 0; i < sLimits.length; i++)
                line.add(new CSVField(nrExamsLim[i]));
            if (nrAvgPer > 0) {
                line.add(new CSVField(sDF.format(((double) totAvgPer) / nrAvgPer)));
                line.add(new CSVField(sDF.format(((double) totWgAvgPer) / nrAvgPer)));
            }
            csv.addLine(line);
        }
        return csv;
    }
}
