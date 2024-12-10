package org.cpsolver.exam.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.cpsolver.exam.criteria.StudentBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentDistanceBackToBackConflicts;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamOwner;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.CSVFile.CSVField;


/**
 * Export student direct, back-to-back, and more than two exams a day conflicts
 * into a CSV file. <br>
 * <br>
 * Usage:
 * <pre><code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamStudentConflictsBySectionCourse(model).report().save(file);
 * </code></pre>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
     * @param assignment current assignment
     * @return resultant report
     */
    public CSVFile report(Assignment<Exam, ExamPlacement> assignment) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Section/Course"), new CSVField("Period"), new CSVField("Day"),
                new CSVField("Time"), new CSVField("Room"), new CSVField("Student"), new CSVField("Type"),
                new CSVField("Section/Course"), new CSVField("Period"), new CSVField("Time"), new CSVField("Room"),
                new CSVField("Distance") });
        boolean isDayBreakBackToBack = ((StudentBackToBackConflicts)iModel.getCriterion(StudentBackToBackConflicts.class)).isDayBreakBackToBack();
        double backToBackDistance = ((StudentDistanceBackToBackConflicts)iModel.getCriterion(StudentDistanceBackToBackConflicts.class)).getBackToBackDistance();
        TreeSet<ExamOwner> courseSections = new TreeSet<ExamOwner>();
        for (Exam exam : iModel.variables()) {
            courseSections.addAll(getOwners(exam));
        }
        for (ExamOwner cs : courseSections) {
            Exam exam = cs.getExam();
            ExamPlacement placement = assignment.getValue(exam);
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
                @Override
                public int compare(ExamStudent s1, ExamStudent s2) {
                    int cmp = s1.getName().compareTo(s2.getName());
                    if (cmp != 0)
                        return cmp;
                    return Double.compare(s1.getId(), s2.getId());
                }
            });
            for (ExamStudent student : students) {
                boolean stdPrinted = false;
                int nrExams = student.getExams(assignment, period).size();
                if (nrExams > 1) {
                    boolean typePrinted = false;
                    for (Exam otherExam : student.getExams(assignment, period)) {
                        if (otherExam.equals(exam))
                            continue;
                        ExamPlacement otherPlacement = assignment.getValue(otherExam);
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
                    if (period.prev() != null && !student.getExams(assignment, period.prev()).isEmpty()
                            && (!isDayBreakBackToBack || period.prev().getDay() == period.getDay()))
                        periods.add(period.prev());
                    if (period.next() != null && !student.getExams(assignment, period.next()).isEmpty()
                            && (!isDayBreakBackToBack || period.next().getDay() == period.getDay()))
                        periods.add(period.next());
                    for (ExamPeriod otherPeriod : periods) {
                        for (Exam otherExam : student.getExams(assignment, otherPeriod)) {
                            ExamPlacement otherPlacement = assignment.getValue(otherExam);
                            String roomsOtherExam = "";
                            for (ExamRoomPlacement room : otherPlacement.getRoomPlacements()) {
                                if (roomsOtherExam.length() > 0)
                                    roomsOtherExam += ", ";
                                roomsOtherExam += room.getName();
                            }
                            String distStr = "";
                            if (backToBackDistance >= 0) {
                                double dist = placement.getDistanceInMeters(otherPlacement);
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
                int nrExamsADay = student.getExamsADay(assignment, period.getDay()).size();
                if (nrExamsADay > 2) {
                    boolean typePrinted = false;
                    for (Exam otherExam : student.getExamsADay(assignment, period.getDay())) {
                        if (otherExam.equals(exam))
                            continue;
                        ExamPlacement otherPlacement = assignment.getValue(otherExam);
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
