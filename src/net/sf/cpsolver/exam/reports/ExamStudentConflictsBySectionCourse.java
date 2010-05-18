package net.sf.cpsolver.exam.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamOwner;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export student direct, back-to-back, and more than two exams a day conflicts
 * into a CSV file. <br>
 * <br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamStudentConflictsBySectionCourse(model).report().save(file);
 * </code> <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class ExamStudentConflictsBySectionCourse {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamStudentConflictsBySectionCourse(ExamModel model) {
        iModel = model;
    }

    private List<ExamOwner> getOwners(Exam exam) {
        if (!exam.getOwners().isEmpty())
            return exam.getOwners();
        ExamOwner cs = new ExamOwner(exam, exam.getId(), exam.getName());
        cs.getStudents().addAll(exam.getStudents());
        List<ExamOwner> ret = new ArrayList<ExamOwner>(1);
        ret.add(cs);
        return ret;
    }

    private List<ExamOwner> getOwners(Exam exam, ExamStudent student) {
        List<ExamOwner> ret = new ArrayList<ExamOwner>(exam.getOwners(student));
        if (ret.isEmpty()) {
            ExamOwner cs = new ExamOwner(exam, exam.getId(), exam.getName());
            cs.getStudents().add(student);
            ret.add(cs);
        }
        Collections.sort(ret);
        return ret;
    }

    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Section/Course"), new CSVField("Period"), new CSVField("Day"),
                new CSVField("Time"), new CSVField("Room"), new CSVField("Student"), new CSVField("Type"),
                new CSVField("Section/Course"), new CSVField("Period"), new CSVField("Time"), new CSVField("Room"),
                new CSVField("Distance") });
        TreeSet<ExamOwner> courseSections = new TreeSet<ExamOwner>();
        for (Exam exam : iModel.variables()) {
            courseSections.addAll(getOwners(exam));
        }
        for (ExamOwner cs : courseSections) {
            Exam exam = cs.getExam();
            ExamPlacement placement = exam.getAssignment();
            if (placement == null)
                continue;
            String roomsThisExam = "";
            for (ExamRoomPlacement room : placement.getRoomPlacements()) {
                if (roomsThisExam.length() > 0)
                    roomsThisExam += ", ";
                roomsThisExam += room.getName();
            }
            ExamPeriod period = placement.getPeriod();
            boolean csPrinted = false;
            List<ExamStudent> students = new ArrayList<ExamStudent>(cs.getStudents());
            Collections.sort(students, new Comparator<ExamStudent>() {
                public int compare(ExamStudent s1, ExamStudent s2) {
                    int cmp = s1.getName().compareTo(s2.getName());
                    if (cmp != 0)
                        return cmp;
                    return Double.compare(s1.getId(), s2.getId());
                }
            });
            for (ExamStudent student : students) {
                boolean stdPrinted = false;
                int nrExams = student.getExams(period).size();
                if (nrExams > 1) {
                    boolean typePrinted = false;
                    for (Exam otherExam : student.getExams(period)) {
                        if (otherExam.equals(exam))
                            continue;
                        ExamPlacement otherPlacement = otherExam.getAssignment();
                        ExamPeriod otherPeriod = otherPlacement.getPeriod();
                        String roomsOtherExam = "";
                        for (ExamRoomPlacement room : otherPlacement.getRoomPlacements()) {
                            if (roomsOtherExam.length() > 0)
                                roomsOtherExam += ", ";
                            roomsOtherExam += room.getName();
                        }
                        boolean otherPrinted = false;
                        for (ExamOwner ocs : getOwners(otherExam, student)) {
                            csv.addLine(new CSVField[] { new CSVField(csPrinted ? "" : cs.getName()),
                                    new CSVField(csPrinted ? "" : String.valueOf(1 + period.getIndex())),
                                    new CSVField(csPrinted ? "" : period.getDayStr()),
                                    new CSVField(csPrinted ? "" : period.getTimeStr()),
                                    new CSVField(csPrinted ? "" : roomsThisExam),
                                    new CSVField(stdPrinted ? "" : student.getName()),
                                    new CSVField(typePrinted ? "" : "direct"), new CSVField(ocs.getName()),
                                    new CSVField(otherPrinted ? "" : String.valueOf(1 + otherPeriod.getIndex())),
                                    new CSVField(otherPrinted ? "" : otherPeriod.getTimeStr()),
                                    new CSVField(otherPrinted ? "" : roomsOtherExam) });
                            csPrinted = true;
                            stdPrinted = true;
                            typePrinted = true;
                            otherPrinted = true;
                        }
                    }
                }
                if (nrExams > 0) {
                    boolean typePrinted = false;
                    List<ExamPeriod> periods = new ArrayList<ExamPeriod>(2);
                    if (period.prev() != null && !student.getExams(period.prev()).isEmpty()
                            && (!iModel.isDayBreakBackToBack() || period.prev().getDay() == period.getDay()))
                        periods.add(period.prev());
                    if (period.next() != null && !student.getExams(period.next()).isEmpty()
                            && (!iModel.isDayBreakBackToBack() || period.next().getDay() == period.getDay()))
                        periods.add(period.next());
                    for (ExamPeriod otherPeriod : periods) {
                        for (Exam otherExam : student.getExams(otherPeriod)) {
                            ExamPlacement otherPlacement = otherExam.getAssignment();
                            String roomsOtherExam = "";
                            for (ExamRoomPlacement room : otherPlacement.getRoomPlacements()) {
                                if (roomsOtherExam.length() > 0)
                                    roomsOtherExam += ", ";
                                roomsOtherExam += room.getName();
                            }
                            String distStr = "";
                            if (iModel.getBackToBackDistance() >= 0) {
                                int dist = placement.getDistance(otherPlacement);
                                if (dist > 0)
                                    distStr = String.valueOf(dist);
                            }
                            boolean otherPrinted = false;
                            for (ExamOwner ocs : getOwners(otherExam, student)) {
                                csv.addLine(new CSVField[] { new CSVField(csPrinted ? "" : cs.getName()),
                                        new CSVField(csPrinted ? "" : String.valueOf(1 + period.getIndex())),
                                        new CSVField(csPrinted ? "" : period.getDayStr()),
                                        new CSVField(csPrinted ? "" : period.getTimeStr()),
                                        new CSVField(csPrinted ? "" : roomsThisExam),
                                        new CSVField(stdPrinted ? "" : student.getName()),
                                        new CSVField(typePrinted ? "" : "back-to-back"), new CSVField(ocs.getName()),
                                        new CSVField(otherPrinted ? "" : String.valueOf(1 + otherPeriod.getIndex())),
                                        new CSVField(otherPrinted ? "" : otherPeriod.getTimeStr()),
                                        new CSVField(otherPrinted ? "" : roomsOtherExam),
                                        new CSVField(otherPrinted ? "" : distStr), });
                                csPrinted = true;
                                stdPrinted = true;
                                typePrinted = true;
                                otherPrinted = true;
                            }
                        }
                    }
                }
                int nrExamsADay = student.getExamsADay(period.getDay()).size();
                if (nrExamsADay > 2) {
                    boolean typePrinted = false;
                    for (Exam otherExam : student.getExamsADay(period.getDay())) {
                        if (otherExam.equals(exam))
                            continue;
                        ExamPlacement otherPlacement = otherExam.getAssignment();
                        ExamPeriod otherPeriod = otherPlacement.getPeriod();
                        String roomsOtherExam = "";
                        for (ExamRoomPlacement room : otherPlacement.getRoomPlacements()) {
                            if (roomsOtherExam.length() > 0)
                                roomsOtherExam += ", ";
                            roomsOtherExam += room.getName();
                        }
                        boolean otherPrinted = false;
                        for (ExamOwner ocs : getOwners(otherExam, student)) {
                            csv.addLine(new CSVField[] { new CSVField(csPrinted ? "" : cs.getName()),
                                    new CSVField(csPrinted ? "" : String.valueOf(1 + period.getIndex())),
                                    new CSVField(csPrinted ? "" : period.getDayStr()),
                                    new CSVField(csPrinted ? "" : period.getTimeStr()),
                                    new CSVField(csPrinted ? "" : roomsThisExam),
                                    new CSVField(stdPrinted ? "" : student.getName()),
                                    new CSVField(typePrinted ? "" : "more-2-day"), new CSVField(ocs.getName()),
                                    new CSVField(otherPrinted ? "" : String.valueOf(1 + otherPeriod.getIndex())),
                                    new CSVField(otherPrinted ? "" : otherPeriod.getTimeStr()),
                                    new CSVField(otherPrinted ? "" : roomsOtherExam) });
                            csPrinted = true;
                            stdPrinted = true;
                            typePrinted = true;
                            otherPrinted = true;
                        }
                    }
                }
            }
        }
        return csv;
    }
}
