package net.sf.cpsolver.exam.reports;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export student more than two exams a day conflicts between triplets of exams into a CSV file.
 * <br><br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamStudentMoreTwoADay(model).report().save(file);
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
public class ExamStudentMoreTwoADay {
    private ExamModel iModel = null;
    
    /**
     * Constructor
     * @param model examination timetabling model
     */
    public ExamStudentMoreTwoADay(ExamModel model) {
        iModel = model;
    }
    
    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] {
                new CSVField("Exam 1"),
                new CSVField("Enrl 1"),
                new CSVField("Period 1"),
                new CSVField("Date 1"),
                new CSVField("Time 1"),
                new CSVField("Exam 2"),
                new CSVField("Enrl 2"),
                new CSVField("Period 2"),
                new CSVField("Time 2"),
                new CSVField("Exam 3"),
                new CSVField("Enrl 3"),
                new CSVField("Period 3"),
                new CSVField("Time 3"),
                new CSVField("More-2-Day"),
                new CSVField("More-2-Day [%]")
        });
        DecimalFormat df = new DecimalFormat("0.0");
        for (Enumeration e=iModel.variables().elements();e.hasMoreElements();) {
            Exam ex1 = (Exam)e.nextElement();
            ExamPlacement p1 = (ExamPlacement)ex1.getAssignment();
            if (p1==null) continue;
            for (Enumeration f=iModel.variables().elements();f.hasMoreElements();) {
                Exam ex2 = (Exam)f.nextElement();
                if (ex2.equals(ex1)) continue;
                ExamPlacement p2 = (ExamPlacement)ex2.getAssignment();
                if (p2==null || p2.getPeriod().getDay()!=p1.getPeriod().getDay() || p2.getPeriod().getIndex()<p1.getPeriod().getIndex()) continue;
                if (p1.getPeriod().equals(p2.getPeriod()) && ex1.getId()>=ex2.getId()) continue;
                Vector students = (Vector)ex1.getJointEnrollments().get(ex2);
                if (students==null || students.isEmpty()) continue;
                for (Enumeration g=iModel.variables().elements();g.hasMoreElements();) {
                    Exam ex3 = (Exam)g.nextElement();
                    if (ex3.equals(ex2) || ex3.equals(ex1)) continue;
                    ExamPlacement p3 = (ExamPlacement)ex3.getAssignment();
                    if (p3==null || p3.getPeriod().getDay()!=p2.getPeriod().getDay() || p3.getPeriod().getIndex()<p2.getPeriod().getIndex()) continue;
                    if (p1.getPeriod().equals(p3.getPeriod()) && ex1.getId()>=ex3.getId()) continue;
                    if (p2.getPeriod().equals(p3.getPeriod()) && ex2.getId()>=ex3.getId()) continue;
                    int m2d = 0;
                    for (Enumeration h=ex3.getStudents().elements();h.hasMoreElements();) 
                        if (students.contains(h.nextElement())) m2d++;
                    if (m2d==0) continue;
                    csv.addLine(new CSVField[] {
                            new CSVField(ex1.getName()),
                            new CSVField(ex1.getStudents().size()),
                            new CSVField(p1.getPeriod().getIndex()+1),
                            new CSVField(p1.getPeriod().getDayStr()),
                            new CSVField(p1.getPeriod().getTimeStr()),
                            new CSVField(ex2.getName()),
                            new CSVField(ex2.getStudents().size()),
                            new CSVField(p2.getPeriod().getIndex()+1),
                            new CSVField(p2.getPeriod().getTimeStr()),
                            new CSVField(ex3.getName()),
                            new CSVField(ex3.getStudents().size()),
                            new CSVField(p3.getPeriod().getIndex()+1),
                            new CSVField(p3.getPeriod().getTimeStr()),
                            new CSVField(m2d),
                            new CSVField(df.format(100.0*m2d/Math.min(Math.min(ex1.getStudents().size(),ex2.getStudents().size()),ex3.getStudents().size())))
                        });
                }
            }
        }
        return csv;
    }
}
