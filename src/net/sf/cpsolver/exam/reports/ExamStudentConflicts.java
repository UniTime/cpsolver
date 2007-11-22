package net.sf.cpsolver.exam.reports;

import java.util.Enumeration;
import java.util.Iterator;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamCourseSection;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export student direct, back-to-back, and more than two exams a day conflicts 
 * into a CSV file.
 * <br><br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamStudentConflicts(model).report().save(file);
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
public class ExamStudentConflicts {
    private ExamModel iModel = null;
    
    /**
     * Constructor
     * @param model examination timetabling model
     */
    public ExamStudentConflicts(ExamModel model) {
        iModel = model;
    }
    
    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] {
                new CSVField("Student"),
                new CSVField("Type"),
                new CSVField("Section/Course"),
                new CSVField("Period"),
                new CSVField("Day"),
                new CSVField("Time"),
                new CSVField("Room"),
                new CSVField("Distance")
        });
        for (Enumeration e=iModel.getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (Enumeration f=iModel.getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                int nrExams = student.getExams(period).size();
                if (nrExams>1) {
                    String sections = "";
                    String rooms = "";
                    String periods = String.valueOf(period.getIndex()+1);
                    String periodDays = period.getDayStr();
                    String periodTimes = period.getTimeStr();
                    for (Iterator i=student.getExams(period).iterator();i.hasNext();) {
                        Exam exam = (Exam)i.next();
                        ExamPlacement placement = (ExamPlacement)exam.getAssignment();
                        String roomsThisExam = "";
                        for (Iterator j=placement.getRooms().iterator();j.hasNext();) {
                            ExamRoom room = (ExamRoom)j.next();
                            if (roomsThisExam.length()>0) roomsThisExam+=", ";
                            roomsThisExam+=room.getName();
                        }
                        boolean first = true;
                        for (Enumeration g=exam.getCourseSections(student).elements();g.hasMoreElements();){
                            ExamCourseSection cs = (ExamCourseSection)g.nextElement();
                            if (sections.length()>0) { 
                                sections+="\n"; rooms+="\n";
                                periods+="\n"; periodDays+="\n"; periodTimes+="\n";
                            }
                            sections += cs.getName();
                            if (first) rooms += roomsThisExam;
                            first = false;
                        }
                        if (exam.getCourseSections(student).isEmpty()) {
                            sections += exam.getName();
                            rooms += roomsThisExam;
                        }
                    }
                    csv.addLine(new CSVField[] {
                            new CSVField(student.getName()),
                            new CSVField("direct"),
                            new CSVField(sections),
                            new CSVField(periods),
                            new CSVField(periodDays),
                            new CSVField(periodTimes),
                            new CSVField(rooms)
                    });
                }
                if (nrExams>0) {
                    if (period.next()!=null && !student.getExams(period.next()).isEmpty() && (!iModel.isDayBreakBackToBack() || period.next().getDay()==period.getDay())) {
                        for (Iterator i=student.getExams(period).iterator();i.hasNext();) {
                            Exam ex1 = (Exam)i.next();
                            for (Iterator j=student.getExams(period.next()).iterator();j.hasNext();) {
                                Exam ex2 = (Exam)j.next();
                                ExamPlacement placement = (ExamPlacement)ex1.getAssignment();
                                String sections = "";
                                String rooms = "";
                                String roomsThisExam = "";
                                String periods = String.valueOf(period.getIndex()+1);
                                String periodDays = period.getDayStr();
                                String periodTimes = period.getTimeStr();
                                for (Iterator k=placement.getRooms().iterator();k.hasNext();) {
                                    ExamRoom room = (ExamRoom)k.next();
                                    if (roomsThisExam.length()>0) roomsThisExam+=", ";
                                    roomsThisExam+=room.getName();
                                }
                                boolean first = true;
                                for (Enumeration g=ex1.getCourseSections(student).elements();g.hasMoreElements();){
                                    ExamCourseSection cs = (ExamCourseSection)g.nextElement();
                                    if (sections.length()>0) {
                                        sections+="\n"; rooms+="\n";
                                        periods+="\n"; periodDays+="\n"; periodTimes+="\n";
                                    }
                                    sections += cs.getName();
                                    if (first) rooms += roomsThisExam;
                                    first = false;
                                }
                                if (ex1.getCourseSections(student).isEmpty()) {
                                    sections += ex1.getName();
                                    rooms += roomsThisExam;
                                }
                                placement = (ExamPlacement)ex2.getAssignment();
                                roomsThisExam = "";
                                for (Iterator k=placement.getRooms().iterator();k.hasNext();) {
                                    ExamRoom room = (ExamRoom)k.next();
                                    if (roomsThisExam.length()>0) roomsThisExam+=", ";
                                    roomsThisExam+=room.getName();
                                }
                                first = true;
                                for (Enumeration g=ex2.getCourseSections(student).elements();g.hasMoreElements();){
                                    ExamCourseSection cs = (ExamCourseSection)g.nextElement();
                                    sections+="\n"; rooms+="\n"; periods+="\n"; periodDays+="\n"; periodTimes+="\n";
                                    sections += cs.getName();
                                    if (first) {
                                        rooms += roomsThisExam;
                                        periods += String.valueOf(period.next().getIndex()+1);
                                        periodDays += period.next().getDayStr();
                                        periodTimes += period.next().getTimeStr();
                                    }
                                    first = false;
                                }
                                if (ex2.getCourseSections(student).isEmpty()) {
                                    sections+="\n"; rooms+="\n"; periods+="\n"; periodDays+="\n"; periodTimes+="\n";
                                    sections += ex2.getName();
                                    rooms += roomsThisExam;
                                    periods += String.valueOf(period.next().getIndex()+1);
                                    periodDays += period.next().getDayStr();
                                    periodTimes += period.next().getTimeStr();
                                    rooms += roomsThisExam;
                                }
                                String distStr="";
                                if (iModel.getBackToBackDistance()>=0) {
                                    int dist = ((ExamPlacement)ex1.getAssignment()).getDistance((ExamPlacement)ex2.getAssignment());
                                    if (dist>0) distStr = String.valueOf(dist);
                                }
                                csv.addLine(new CSVField[] {
                                        new CSVField(student.getName()),
                                        new CSVField("back-to-back"),
                                        new CSVField(sections),
                                        new CSVField(periods),
                                        new CSVField(periodDays),
                                        new CSVField(periodTimes),
                                        new CSVField(rooms),
                                        new CSVField(distStr)
                                });
                            }
                        }
                    }
                }
                if (period.next()==null || period.next().getDay()!=period.getDay()) {
                    int nrExamsADay = student.getExamsADay(period.getDay()).size();
                    if (nrExamsADay>2) {
                        String sections = "";
                        String periods = "";
                        String periodDays = "";
                        String periodTimes = "";
                        String rooms = "";
                        for (Iterator i=student.getExamsADay(period.getDay()).iterator();i.hasNext();) {
                            Exam exam = (Exam)i.next();
                            ExamPlacement placement = (ExamPlacement)exam.getAssignment();
                            String roomsThisExam = "";
                            for (Iterator k=placement.getRooms().iterator();k.hasNext();) {
                                ExamRoom room = (ExamRoom)k.next();
                                if (roomsThisExam.length()>0) roomsThisExam+=", ";
                                roomsThisExam+=room.getName();
                            }
                            boolean first = true;
                            for (Enumeration g=exam.getCourseSections(student).elements();g.hasMoreElements();){
                                ExamCourseSection cs = (ExamCourseSection)g.nextElement();
                                if (sections.length()>0) {
                                    sections+="\n"; rooms+="\n";
                                    periods+="\n"; periodDays+="\n"; periodTimes+="\n";
                                }
                                sections += cs.getName();
                                if (first) {
                                    periods += (placement.getPeriod().getIndex()+1);
                                    periodDays += placement.getPeriod().getDayStr();
                                    periodTimes += placement.getPeriod().getTimeStr();
                                    rooms += roomsThisExam;
                                }
                                first = false;
                            }
                            if (exam.getCourseSections(student).isEmpty()) {
                                if (sections.length()>0) {
                                    sections+="\n"; rooms+="\n";
                                    periods+="\n"; periodDays+="\n"; periodTimes+="\n";
                                }
                                sections += exam.getName();
                                periods += (placement.getPeriod().getIndex()+1);
                                periodDays += placement.getPeriod().getDayStr();
                                periodTimes += placement.getPeriod().getTimeStr();
                                rooms += roomsThisExam;
                            }
                        }
                        csv.addLine(new CSVField[] {
                                new CSVField(student.getName()),
                                new CSVField("more-2-day"),
                                new CSVField(sections),
                                new CSVField(periods),
                                new CSVField(periodDays),
                                new CSVField(periodTimes),
                                new CSVField(rooms)
                        });
                    }
                }
            }
        }
        return csv;
    }
}
