package net.sf.cpsolver.exam.reports;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export distribution of number of students by number of meetings per day into a CSV file.
 * <br><br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamNbrMeetingsPerDay(model).report().save(file);
 * </code>
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
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
public class ExamNbrMeetingsPerDay {
    private ExamModel iModel = null;
    
    /**
     * Constructor
     * @param model examination timetabling model
     */
    public ExamNbrMeetingsPerDay(ExamModel model) {
        iModel = model;
    }
    
    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        Vector header = new Vector();
        header.add(new CSVField("Date"));
        header.add(new CSVField("None"));
        for (int i=1;i<=5;i++) 
            header.add(new CSVField(i==5?"5+":String.valueOf(i)));
        header.add(new CSVField("Back-To-Back"));
        csv.setHeader(header);
        DecimalFormat df = new DecimalFormat("0.0");
        int[] nrExamsTotal = new int[6];
        for (int i=0;i<=5;i++) nrExamsTotal[i]=0;
        int btbTotal = 0;
        for (int d=0;d<iModel.getNrDays();d++) {
            ExamPeriod period = null; 
            for (Enumeration e=iModel.getPeriods().elements();e.hasMoreElements();) {
                period = (ExamPeriod)e.nextElement();
                if (period.getDay()==d) break;
            }
            int[] nrExams = new int[6];
            for (int i=0;i<=5;i++) nrExams[i]=0;
            int btb = 0;
            for (Enumeration f=iModel.getStudents().elements();f.hasMoreElements();) {
                ExamStudent student = (ExamStudent)f.nextElement();
                int ex = student.getExamsADay(d).size();
                nrExams[ex<=5?ex:5]++;
                ExamPeriod p = period;
                while (p.next()!=null && (iModel.isDayBreakBackToBack()?p:p.next()).getDay()==d) {
                    btb+=student.getExams(p).size()*student.getExams(p.next()).size();
                    p = p.next();
                }
            }
            Vector line = new Vector();
            line.add(new CSVField(period.getDayStr()));
            for (int i=0;i<=5;i++) {
                line.add(new CSVField(nrExams[i]));
                nrExamsTotal[i]+=nrExams[i];
            }
            line.add(new CSVField(btb));
            btbTotal+=btb;
            csv.addLine(line);
        }
        Vector line = new Vector();
        line.add(new CSVField("Total"));
        for (int i=0;i<=5;i++)
            line.add(new CSVField(nrExamsTotal[i]));
        line.add(new CSVField(btbTotal));
        csv.addLine(line);
        return csv;
    }
}
