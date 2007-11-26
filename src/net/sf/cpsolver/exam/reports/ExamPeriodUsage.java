package net.sf.cpsolver.exam.reports;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export period usage into CSV file.
 * <br><br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamPeriodUsage(model).report().save(file);
 * </code>
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ExamPeriodUsage {
    private ExamModel iModel = null;
    /** Exam enrollment limits */
    public static int[] sLimits = new int[] {10, 50, 100, 200}; 
    
    /**
     * Constructor
     * @param model examination timetabling model
     */
    public ExamPeriodUsage(ExamModel model) {
        iModel = model;
    }
    
    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        Vector header = new Vector();
        header.add(new CSVField("Period"));
        header.add(new CSVField("Date"));
        header.add(new CSVField("Time"));
        header.add(new CSVField("Weight"));
        header.add(new CSVField("NrExams"));
        header.add(new CSVField("Students"));
        for (int i=0;i<sLimits.length;i++) {
            header.add(new CSVField("NrExams>="+sLimits[i]));
        }
        csv.setHeader(header);
        DecimalFormat df = new DecimalFormat("0.0");
        for (Enumeration e=iModel.getPeriods().elements();e.hasMoreElements();) {
            ExamPeriod period = (ExamPeriod)e.nextElement();
            int nrExams = 0;
            int nrStudents = 0;
            int[] nrExamsLim = new int[sLimits.length];
            for (int i=0;i<sLimits.length;i++) nrExamsLim[i]=0;
            for (Enumeration f=iModel.variables().elements();f.hasMoreElements();) {
                Exam exam = (Exam)f.nextElement();
                ExamPlacement placement = (ExamPlacement)exam.getAssignment();
                if (placement==null || !(placement.getPeriod().equals(period))) continue;
                nrExams++;
                nrStudents+=exam.getStudents().size();
                for (int i=0;i<sLimits.length;i++)
                    if (exam.getStudents().size()>=sLimits[i]) nrExamsLim[i]++;
            }
            Vector line = new Vector();
            line.add(new CSVField(period.getIndex()+1));
            line.add(new CSVField(period.getDayStr()));
            line.add(new CSVField(period.getTimeStr()));
            line.add(new CSVField(period.getWeight()));
            line.add(new CSVField(nrExams));
            line.add(new CSVField(nrStudents));
            for (int i=0;i<sLimits.length;i++)
                line.add(new CSVField(nrExamsLim[i]));
            csv.addLine(line);
        }
        return csv;
    }
}
