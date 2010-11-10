package net.sf.cpsolver.exam.reports;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamInstructor;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamOwner;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export instructor direct, back-to-back, and more than two exams a day
 * conflicts into a CSV file. <br>
 * <br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamInstructorConflicts(model).report().save(file);
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class ExamInstructorConflicts {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamInstructorConflicts(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Instructor"), new CSVField("Type"),
                new CSVField("Section/Course"), new CSVField("Period"), new CSVField("Day"), new CSVField("Time"),
                new CSVField("Room"), new CSVField("Distance") });
        for (ExamInstructor instructor : iModel.getInstructors()) {
            for (ExamPeriod period : iModel.getPeriods()) {
                int nrExams = instructor.getExams(period).size();
                if (nrExams > 1) {
                    String sections = "";
                    String rooms = "";
                    String periods = String.valueOf(period.getIndex() + 1);
                    String periodDays = period.getDayStr();
                    String periodTimes = period.getTimeStr();
                    for (Exam exam : instructor.getExams(period)) {
                        ExamPlacement placement = exam.getAssignment();
                        String roomsThisExam = "";
                        for (ExamRoomPlacement room : placement.getRoomPlacements()) {
                            if (roomsThisExam.length() > 0)
                                roomsThisExam += ", ";
                            roomsThisExam += room.getName();
                        }
                        boolean first = true;
                        for (ExamOwner owner : exam.getOwners(instructor)) {
                            if (sections.length() > 0) {
                                sections += "\n";
                                rooms += "\n";
                                periods += "\n";
                                periodDays += "\n";
                                periodTimes += "\n";
                            }
                            sections += owner.getName();
                            if (first)
                                rooms += roomsThisExam;
                            first = false;
                        }
                        if (exam.getOwners(instructor).isEmpty()) {
                            sections += exam.getName();
                            rooms += roomsThisExam;
                        }
                    }
                    csv.addLine(new CSVField[] { new CSVField(instructor.getName()), new CSVField("direct"),
                            new CSVField(sections), new CSVField(periods), new CSVField(periodDays),
                            new CSVField(periodTimes), new CSVField(rooms) });
                }
                if (nrExams > 0) {
                    if (period.next() != null && !instructor.getExams(period.next()).isEmpty()
                            && (!iModel.isDayBreakBackToBack() || period.next().getDay() == period.getDay())) {
                        for (Exam ex1 : instructor.getExams(period)) {
                            for (Exam ex2 : instructor.getExams(period.next())) {
                                ExamPlacement placement = ex1.getAssignment();
                                String sections = "";
                                String rooms = "";
                                String roomsThisExam = "";
                                String periods = String.valueOf(period.getIndex() + 1);
                                String periodDays = period.getDayStr();
                                String periodTimes = period.getTimeStr();
                                for (ExamRoomPlacement room : placement.getRoomPlacements()) {
                                    if (roomsThisExam.length() > 0)
                                        roomsThisExam += ", ";
                                    roomsThisExam += room.getName();
                                }
                                boolean first = true;
                                for (ExamOwner owner : ex1.getOwners(instructor)) {
                                    if (sections.length() > 0) {
                                        sections += "\n";
                                        rooms += "\n";
                                        periods += "\n";
                                        periodDays += "\n";
                                        periodTimes += "\n";
                                    }
                                    sections += owner.getName();
                                    if (first)
                                        rooms += roomsThisExam;
                                    first = false;
                                }
                                if (ex1.getOwners(instructor).isEmpty()) {
                                    sections += ex1.getName();
                                    rooms += roomsThisExam;
                                }
                                placement = ex2.getAssignment();
                                roomsThisExam = "";
                                for (ExamRoomPlacement room : placement.getRoomPlacements()) {
                                    if (roomsThisExam.length() > 0)
                                        roomsThisExam += ", ";
                                    roomsThisExam += room.getName();
                                }
                                first = true;
                                for (ExamOwner owner : ex2.getOwners(instructor)) {
                                    sections += "\n";
                                    rooms += "\n";
                                    periods += "\n";
                                    periodDays += "\n";
                                    periodTimes += "\n";
                                    sections += owner.getName();
                                    if (first) {
                                        rooms += roomsThisExam;
                                        periods += String.valueOf(period.next().getIndex() + 1);
                                        periodDays += period.next().getDayStr();
                                        periodTimes += period.next().getTimeStr();
                                    }
                                    first = false;
                                }
                                if (ex2.getOwners(instructor).isEmpty()) {
                                    sections += "\n";
                                    rooms += "\n";
                                    periods += "\n";
                                    periodDays += "\n";
                                    periodTimes += "\n";
                                    sections += ex2.getName();
                                    rooms += roomsThisExam;
                                    periods += String.valueOf(period.next().getIndex() + 1);
                                    periodDays += period.next().getDayStr();
                                    periodTimes += period.next().getTimeStr();
                                }
                                String distStr = "";
                                if (iModel.getBackToBackDistance() >= 0) {
                                    double dist = (ex1.getAssignment()).getDistanceInMeters(ex2.getAssignment());
                                    if (dist > 0)
                                        distStr = String.valueOf(dist);
                                }
                                csv.addLine(new CSVField[] { new CSVField(instructor.getName()),
                                        new CSVField("back-to-back"), new CSVField(sections), new CSVField(periods),
                                        new CSVField(periodDays), new CSVField(periodTimes), new CSVField(rooms),
                                        new CSVField(distStr) });
                            }
                        }
                    }
                }
                if (period.next() == null || period.next().getDay() != period.getDay()) {
                    int nrExamsADay = instructor.getExamsADay(period.getDay()).size();
                    if (nrExamsADay > 2) {
                        String sections = "";
                        String periods = "";
                        String periodDays = "";
                        String periodTimes = "";
                        String rooms = "";
                        for (Exam exam : instructor.getExamsADay(period.getDay())) {
                            ExamPlacement placement = exam.getAssignment();
                            String roomsThisExam = "";
                            for (ExamRoomPlacement room : placement.getRoomPlacements()) {
                                if (roomsThisExam.length() > 0)
                                    roomsThisExam += ", ";
                                roomsThisExam += room.getName();
                            }
                            boolean first = true;
                            for (ExamOwner owner : exam.getOwners(instructor)) {
                                if (sections.length() > 0) {
                                    sections += "\n";
                                    rooms += "\n";
                                    periods += "\n";
                                    periodDays += "\n";
                                    periodTimes += "\n";
                                }
                                sections += owner.getName();
                                if (first) {
                                    periods += (placement.getPeriod().getIndex() + 1);
                                    periodDays += placement.getPeriod().getDayStr();
                                    periodTimes += placement.getPeriod().getTimeStr();
                                    rooms += roomsThisExam;
                                }
                                first = false;
                            }
                            if (exam.getOwners(instructor).isEmpty()) {
                                if (sections.length() > 0) {
                                    sections += "\n";
                                    rooms += "\n";
                                    periods += "\n";
                                    periodDays += "\n";
                                    periodTimes += "\n";
                                }
                                sections += exam.getName();
                                periods += (placement.getPeriod().getIndex() + 1);
                                periodDays += placement.getPeriod().getDayStr();
                                periodTimes += placement.getPeriod().getTimeStr();
                                rooms += roomsThisExam;
                            }
                        }
                        csv.addLine(new CSVField[] { new CSVField(instructor.getName()), new CSVField("more-2-day"),
                                new CSVField(sections), new CSVField(periods), new CSVField(periodDays),
                                new CSVField(periodTimes), new CSVField(rooms) });
                    }
                }
            }
        }
        return csv;
    }
}
