package net.sf.cpsolver.exam.model;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of an exam placement (problem value), i.e., assignment of an
 * exam to period and room(s). Each placement has defined a period and a set of
 * rooms. The exam as well as the rooms have to be available during the given
 * period (see {@link Exam#getPeriodPlacements()} and
 * {@link Exam#getRoomPlacements()}). The total size of rooms have to be equal
 * or greater than the number of students enrolled in the exam
 * {@link Exam#getSize()}, using either {@link ExamRoom#getSize()} or
 * {@link ExamRoom#getAltSize()}, depending on {@link Exam#hasAltSeating()}.
 * Also, the number of rooms has to be smaller or equal to
 * {@link Exam#getMaxRooms()}. If {@link Exam#getMaxRooms()} is zero, the exam
 * is only to be assigned to period (the set of rooms is empty). <br>
 * <br>
 * The cost of an assignment consists of the following criteria:
 * <ul>
 * <li>Direct student conflicts {@link ExamPlacement#getNrDirectConflicts()},
 * weighted by {@link ExamModel#getDirectConflictWeight()}
 * <li>More than two exams a day student conflicts
 * {@link ExamPlacement#getNrMoreThanTwoADayConflicts()}, weighted by
 * {@link ExamModel#getMoreThanTwoADayWeight()}
 * <li>Back-to-back student conflicts
 * {@link ExamPlacement#getNrBackToBackConflicts()}, weighted by
 * {@link ExamModel#getBackToBackConflictWeight()}
 * <li>Distance back-to-back student conflicts
 * {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, weighted by
 * {@link ExamModel#getDistanceBackToBackConflictWeight()}
 * <li>Period penalty {@link ExamPlacement#getPeriodPenalty()}, weighted by
 * {@link ExamModel#getPeriodWeight()}
 * <li>Room size penalty {@link ExamPlacement#getRoomSizePenalty()}, weighted by
 * {@link ExamModel#getRoomSizeWeight()}
 * <li>Room split penalty {@link ExamPlacement#getRoomSplitPenalty()}, weighted
 * by {@link ExamModel#getRoomSplitWeight()}
 * <li>Room penalty {@link ExamPlacement#getRoomPenalty()}, weighted by
 * {@link ExamModel#getRoomWeight()}
 * <li>Exam rotation penalty {@link ExamPlacement#getRotationPenalty()},
 * weighted by {@link ExamModel#getExamRotationWeight()}
 * <li>Direct instructor conflicts
 * {@link ExamPlacement#getNrInstructorDirectConflicts()}, weighted by
 * {@link ExamModel#getInstructorDirectConflictWeight()}
 * <li>More than two exams a day instructor conflicts
 * {@link ExamPlacement#getNrInstructorMoreThanTwoADayConflicts()}, weighted by
 * {@link ExamModel#getInstructorMoreThanTwoADayWeight()}
 * <li>Back-to-back instructor conflicts
 * {@link ExamPlacement#getNrInstructorBackToBackConflicts()}, weighted by
 * {@link ExamModel#getInstructorBackToBackConflictWeight()}
 * <li>Distance back-to-back instructor conflicts
 * {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()}, weighted
 * by {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
 * </ul>
 * <br>
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
public class ExamPlacement extends Value<Exam, ExamPlacement> {
    private ExamPeriodPlacement iPeriodPlacement;
    private Set<ExamRoomPlacement> iRoomPlacements;
    private int iSize;
    private int iRoomPenalty;
    private double iRoomSplitDistance;

    private int iHashCode;

    /**
     * Constructor
     * 
     * @param exam
     *            an exam
     * @param periodPlacement
     *            period placement
     * @param roomPlacements
     *            a set of room placements {@link ExamRoomPlacement}
     */
    public ExamPlacement(Exam exam, ExamPeriodPlacement periodPlacement, Set<ExamRoomPlacement> roomPlacements) {
        super(exam);
        iPeriodPlacement = periodPlacement;
        if (roomPlacements == null)
            iRoomPlacements = new HashSet<ExamRoomPlacement>();
        else
            iRoomPlacements = roomPlacements;
        iSize = 0;
        iRoomPenalty = 0;
        iRoomSplitDistance = 0.0;
        for (ExamRoomPlacement r : iRoomPlacements) {
            iSize += r.getSize(exam.hasAltSeating());
            iRoomPenalty += r.getPenalty(periodPlacement.getPeriod());
            if (iRoomPlacements.size() > 1) {
                for (ExamRoomPlacement w : iRoomPlacements) {
                    if (r.getRoom().getId() < w.getRoom().getId())
                        iRoomSplitDistance += r.getRoom().getDistanceInMeters(w.getRoom());
                }
            }
        }
        if (iRoomPlacements.size() > 2) {
            iRoomSplitDistance /= iRoomPlacements.size() * (iRoomPlacements.size() - 1) / 2;
        }
        iHashCode = getName().hashCode();
    }

    /**
     * Assigned period
     */
    public ExamPeriod getPeriod() {
        return iPeriodPlacement.getPeriod();
    }

    /**
     * Assigned period placement
     */
    public ExamPeriodPlacement getPeriodPlacement() {
        return iPeriodPlacement;
    }

    /**
     * Assigned rooms (it is empty when {@link Exam#getMaxRooms()} is zero)
     * 
     * @return list of {@link ExamRoomPlacement}
     */
    public Set<ExamRoomPlacement> getRoomPlacements() {
        return iRoomPlacements;
    }

    /**
     * Overall size of assigned rooms
     */
    public int getSize() {
        return iSize;
    }

    /**
     * Number of direct student conflicts, i.e., number of cases when this exam
     * is attended by a student that attends some other exam at the same period
     */
    public int getNrDirectConflicts() {
        Exam exam = variable();
        // if (!exam.isAllowDirectConflicts()) return 0;
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            Set<Exam> exams = s.getExams(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 1)
                penalty++;
            else if (!s.isAvailable(getPeriod()))
                penalty++;
        }
        return penalty;
    }

    /**
     * Number of direct student conflicts caused by the fact that a student is
     * not available
     */
    public int getNrNotAvailableConflicts() {
        Exam exam = variable();
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            if (!s.isAvailable(getPeriod()))
                penalty++;
        }
        return penalty;
    }

    /**
     * Number of back-to-back student conflicts, i.e., number of cases when this
     * exam is attended by a student that attends some other exam at the
     * previous {@link ExamPeriod#prev()} or following {@link ExamPeriod#next()}
     * period. If {@link ExamModel#isDayBreakBackToBack()} is false,
     * back-to-back conflicts are only considered between consecutive periods
     * that are of the same day.
     */
    public int getNrBackToBackConflicts() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            if (getPeriod().prev() != null) {
                if (model.isDayBreakBackToBack() || getPeriod().prev().getDay() == getPeriod().getDay()) {
                    Set<Exam> exams = s.getExams(getPeriod().prev());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
            if (getPeriod().next() != null) {
                if (model.isDayBreakBackToBack() || getPeriod().next().getDay() == getPeriod().getDay()) {
                    Set<Exam> exams = s.getExams(getPeriod().next());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
        }
        return penalty;
    }

    /**
     * Distance between two placements, i.e., maximal distance between a room of
     * this placement and a room of the given placement. Method
     * {@link ExamRoom#getDistanceInMeters(ExamRoom)} is used to get a distance between
     * two rooms.
     */
    public double getDistanceInMeters(ExamPlacement other) {
        if (getRoomPlacements().isEmpty() || other.getRoomPlacements().isEmpty())
            return 0;
        double maxDistance = 0;
        for (ExamRoomPlacement r1 : getRoomPlacements()) {
            for (ExamRoomPlacement r2 : other.getRoomPlacements()) {
                maxDistance = Math.max(maxDistance, r1.getDistanceInMeters(r2));
            }
        }
        return maxDistance;
    }

    /**
     * Number of back-to-back distance student conflicts, i.e., number of cases
     * when this exam is attended by a student that attends some other exam at
     * the previous {@link ExamPeriod#prev()} or following
     * {@link ExamPeriod#next()} period and the distance
     * {@link ExamPlacement#getDistanceInMeters(ExamPlacement)} between these two exams
     * is greater than {@link ExamModel#getBackToBackDistance()}. Distance
     * back-to-back conflicts are only considered between consecutive periods
     * that are of the same day.
     */
    public int getNrDistanceBackToBackConflicts() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        double btbDist = model.getBackToBackDistance();
        if (btbDist < 0)
            return 0;
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            if (getPeriod().prev() != null) {
                if (getPeriod().prev().getDay() == getPeriod().getDay()) {
                    for (Exam x : s.getExams(getPeriod().prev())) {
                        if (x.equals(exam))
                            continue;
                        if (getDistanceInMeters(x.getAssignment()) > btbDist)
                            penalty++;
                    }
                }
            }
            if (getPeriod().next() != null) {
                if (getPeriod().next().getDay() == getPeriod().getDay()) {
                    for (Exam x : s.getExams(getPeriod().next())) {
                        if (x.equals(exam))
                            continue;
                        if (getDistanceInMeters(x.getAssignment()) > btbDist)
                            penalty++;
                    }
                }
            }
        }
        return penalty;
    }

    /**
     * Number of more than two exams a day student conflicts, i.e., when this
     * exam is attended by a student that attends two or more other exams at the
     * same day.
     */
    public int getNrMoreThanTwoADayConflicts() {
        Exam exam = variable();
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            Set<Exam> exams = s.getExamsADay(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 2)
                penalty++;
        }
        return penalty;
    }

    /**
     * Number of direct instructor conflicts, i.e., number of cases when this
     * exam is attended by an instructor that attends some other exam at the
     * same period
     */
    public int getNrInstructorDirectConflicts() {
        Exam exam = variable();
        // if (!exam.isAllowDirectConflicts()) return 0;
        int penalty = 0;
        for (ExamStudent s : exam.getStudents()) {
            Set<Exam> exams = s.getExams(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 1)
                penalty++;
            else if (!s.isAvailable(getPeriod()))
                penalty++;
        }
        return penalty;
    }

    /**
     * Number of direct instructor conflicts caused by the fact that a student
     * is not available
     */
    public int getNrInstructorNotAvailableConflicts() {
        Exam exam = variable();
        int penalty = 0;
        for (ExamInstructor s : exam.getInstructors()) {
            if (!s.isAvailable(getPeriod()))
                penalty++;
        }
        return penalty;
    }

    /**
     * Number of back-to-back instructor conflicts, i.e., number of cases when
     * this exam is attended by an instructor that attends some other exam at
     * the previous {@link ExamPeriod#prev()} or following
     * {@link ExamPeriod#next()} period. If
     * {@link ExamModel#isDayBreakBackToBack()} is false, back-to-back conflicts
     * are only considered between consecutive periods that are of the same day.
     */
    public int getNrInstructorBackToBackConflicts() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        int penalty = 0;
        for (ExamInstructor s : exam.getInstructors()) {
            if (getPeriod().prev() != null) {
                if (model.isDayBreakBackToBack() || getPeriod().prev().getDay() == getPeriod().getDay()) {
                    Set<Exam> exams = s.getExams(getPeriod().prev());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
            if (getPeriod().next() != null) {
                if (model.isDayBreakBackToBack() || getPeriod().next().getDay() == getPeriod().getDay()) {
                    Set<Exam> exams = s.getExams(getPeriod().next());
                    int nrExams = exams.size() + (exams.contains(exam) ? -1 : 0);
                    penalty += nrExams;
                }
            }
        }
        return penalty;
    }

    /**
     * Number of back-to-back distance instructor conflicts, i.e., number of
     * cases when this exam is attended by an instructor that attends some other
     * exam at the previous {@link ExamPeriod#prev()} or following
     * {@link ExamPeriod#next()} period and the distance
     * {@link ExamPlacement#getDistanceInMeters(ExamPlacement)} between these two exams
     * is greater than {@link ExamModel#getBackToBackDistance()}. Distance
     * back-to-back conflicts are only considered between consecutive periods
     * that are of the same day.
     */
    public int getNrInstructorDistanceBackToBackConflicts() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        double btbDist = model.getBackToBackDistance();
        if (btbDist < 0)
            return 0;
        int penalty = 0;
        for (ExamInstructor s : exam.getInstructors()) {
            if (getPeriod().prev() != null) {
                if (getPeriod().prev().getDay() == getPeriod().getDay()) {
                    for (Exam x : s.getExams(getPeriod().prev())) {
                        if (x.equals(exam))
                            continue;
                        if (getDistanceInMeters(x.getAssignment()) > btbDist)
                            penalty++;
                    }
                }
            }
            if (getPeriod().next() != null) {
                if (getPeriod().next().getDay() == getPeriod().getDay()) {
                    for (Exam x : s.getExams(getPeriod().next())) {
                        if (x.equals(exam))
                            continue;
                        if (getDistanceInMeters(x.getAssignment()) > btbDist)
                            penalty++;
                    }
                }
            }
        }
        return penalty;
    }

    /**
     * Number of more than two exams a day instructor conflicts, i.e., when this
     * exam is attended by an instructor student that attends two or more other
     * exams at the same day.
     */
    public int getNrInstructorMoreThanTwoADayConflicts() {
        Exam exam = variable();
        int penalty = 0;
        for (ExamInstructor s : exam.getInstructors()) {
            Set<Exam> exams = s.getExamsADay(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam) ? 0 : 1);
            if (nrExams > 2)
                penalty++;
        }
        return penalty;
    }

    /**
     * Cost for using room(s) that are too big
     * 
     * @return difference between total room size (computed using either
     *         {@link ExamRoom#getSize()} or {@link ExamRoom#getAltSize()} based
     *         on {@link Exam#hasAltSeating()}) and the number of students
     *         {@link Exam#getSize()}
     */
    public int getRoomSizePenalty() {
        Exam exam = variable();
        int diff = getSize() - exam.getSize();
        return (diff < 0 ? 0 : diff);
    }

    /**
     * Cost for using more than one room (nrSplits^2).
     * 
     * @return penalty (1 for 2 rooms, 4 for 3 rooms, 9 for 4 rooms, etc.)
     */
    public int getRoomSplitPenalty() {
        return (iRoomPlacements.size() <= 1 ? 0 : (iRoomPlacements.size() - 1) * (iRoomPlacements.size() - 1));
    }

    /**
     * Cost for using a period, i.e., {@link ExamPeriodPlacement#getPenalty()}
     */
    public int getPeriodPenalty() {
        return iPeriodPlacement.getPenalty();
    }

    /**
     * Rotation penalty (an exam that has been in later period last times tries
     * to be in an earlier period)
     */
    public int getRotationPenalty() {
        Exam exam = variable();
        if (exam.getAveragePeriod() < 0)
            return 0;
        return (1 + getPeriod().getIndex()) * (1 + exam.getAveragePeriod());
    }

    /**
     * Front load penalty (large exam is discouraged to be placed on or after a
     * certain period)
     * 
     * @return zero if not large exam or if before
     *         {@link ExamModel#getLargePeriod()}, one otherwise
     */
    public int getLargePenalty() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        if (model.getLargeSize() < 0 || exam.getSize() < model.getLargeSize())
            return 0;
        int periodIdx = (int) Math.round(model.getPeriods().size() * model.getLargePeriod());
        return (getPeriod().getIndex() < periodIdx ? 0 : 1);
    }

    /**
     * Room penalty (penalty for using given rooms), i.e., sum of
     * {@link ExamRoomPlacement#getPenalty(ExamPeriod)} of assigned rooms
     */
    public int getRoomPenalty() {
        return iRoomPenalty;
    }

    /**
     * Perturbation penalty, i.e., penalty for using a different assignment than
     * initial. Only applicable when {@link ExamModel#isMPP()} is true (minimal
     * perturbation problem).
     * 
     * @return |period index - initial period index | * exam size
     */
    public int getPerturbationPenalty() {
        Exam exam = variable();
        if (!((ExamModel) exam.getModel()).isMPP())
            return 0;
        ExamPlacement initial = exam.getInitialAssignment();
        if (initial == null)
            return 0;
        return Math.abs(initial.getPeriod().getIndex() - getPeriod().getIndex()) * (1 + exam.getSize());
    }

    /**
     * Room perturbation penalty, i.e., number of assigned rooms different from
     * initial. Only applicable when {@link ExamModel#isMPP()} is true (minimal
     * perturbation problem).
     * 
     * @return |period index - initial period index | * exam size
     */
    public int getRoomPerturbationPenalty() {
        Exam exam = variable();
        if (!((ExamModel) exam.getModel()).isMPP())
            return 0;
        ExamPlacement initial = exam.getInitialAssignment();
        if (initial == null)
            return 0;
        int penalty = 0;
        for (ExamRoomPlacement rp : getRoomPlacements()) {
            if (!initial.getRoomPlacements().contains(rp))
                penalty++;
        }
        return penalty;
    }

    /**
     * Room split distance penalty, i.e., average distance between two rooms of
     * this placement
     */
    public double getRoomSplitDistancePenalty() {
        return iRoomSplitDistance;
    }

    /**
     * Distribution penalty, i.e., sum weights of violated distribution
     * constraints
     */
    public double getDistributionPenalty() {
        int penalty = 0;
        for (ExamDistributionConstraint dc : variable().getDistributionConstraints()) {
            if (dc.isHard())
                continue;
            boolean sat = dc.isSatisfied(this);
            if (sat != dc.isSatisfied())
                penalty += (sat ? -dc.getWeight() : dc.getWeight());
        }
        return penalty;
    }

    /**
     * Room related distribution penalty, i.e., sum weights of violated
     * distribution constraints
     */
    public double getRoomDistributionPenalty() {
        int penalty = 0;
        for (ExamDistributionConstraint dc : variable().getDistributionConstraints()) {
            if (dc.isHard() || !dc.isRoomRelated())
                continue;
            boolean sat = dc.isSatisfied(this);
            if (sat != dc.isSatisfied())
                penalty += (sat ? -dc.getWeight() : dc.getWeight());
        }
        return penalty;
    }

    /**
     * Period related distribution penalty, i.e., sum weights of violated
     * distribution constraints
     */
    public double getPeriodDistributionPenalty() {
        int penalty = 0;
        for (ExamDistributionConstraint dc : variable().getDistributionConstraints()) {
            if (dc.isHard() || !dc.isPeriodRelated())
                continue;
            boolean sat = dc.isSatisfied(this);
            if (sat != dc.isSatisfied())
                penalty += (sat ? -dc.getWeight() : dc.getWeight());
        }
        return penalty;
    }

    /**
     * Overall cost of using this placement. The cost of an assignment consists
     * of the following criteria:
     * <ul>
     * <li>Direct student conflicts {@link ExamPlacement#getNrDirectConflicts()}
     * , weighted by {@link ExamModel#getDirectConflictWeight()}
     * <li>More than two exams a day student conflicts
     * {@link ExamPlacement#getNrMoreThanTwoADayConflicts()}, weighted by
     * {@link ExamModel#getMoreThanTwoADayWeight()}
     * <li>Back-to-back student conflicts
     * {@link ExamPlacement#getNrBackToBackConflicts()}, weighted by
     * {@link ExamModel#getBackToBackConflictWeight()}
     * <li>Distance back-to-back student conflicts
     * {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, weighted by
     * {@link ExamModel#getDistanceBackToBackConflictWeight()}
     * <li>Period penalty {@link ExamPlacement#getPeriodPenalty()}, weighted by
     * {@link ExamModel#getPeriodWeight()}
     * <li>Room size penalty {@link ExamPlacement#getRoomSizePenalty()},
     * weighted by {@link ExamModel#getRoomSizeWeight()}
     * <li>Room split penalty {@link ExamPlacement#getRoomSplitPenalty()},
     * weighted by {@link ExamModel#getRoomSplitWeight()}
     * <li>Room split distance penalty
     * {@link ExamPlacement#getRoomSplitDistancePenalty()}, weighted by
     * {@link ExamModel#getRoomSplitDistanceWeight()}
     * <li>Room penalty {@link ExamPlacement#getRoomPenalty()}, weighted by
     * {@link ExamModel#getRoomWeight()}
     * <li>Exam rotation penalty {@link ExamPlacement#getRotationPenalty()},
     * weighted by {@link ExamModel#getExamRotationWeight()}
     * <li>Direct instructor conflicts
     * {@link ExamPlacement#getNrInstructorDirectConflicts()}, weighted by
     * {@link ExamModel#getInstructorDirectConflictWeight()}
     * <li>More than two exams a day instructor conflicts
     * {@link ExamPlacement#getNrInstructorMoreThanTwoADayConflicts()}, weighted
     * by {@link ExamModel#getInstructorMoreThanTwoADayWeight()}
     * <li>Back-to-back instructor conflicts
     * {@link ExamPlacement#getNrInstructorBackToBackConflicts()}, weighted by
     * {@link ExamModel#getInstructorBackToBackConflictWeight()}
     * <li>Distance back-to-back instructor conflicts
     * {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()},
     * weighted by
     * {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
     * <li>Front load penalty {@link ExamPlacement#getLargePenalty()}, weighted
     * by {@link ExamModel#getLargeWeight()}
     * </ul>
     */
    @Override
    public double toDouble() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        return model.getDirectConflictWeight() * getNrDirectConflicts() + model.getMoreThanTwoADayWeight()
                * getNrMoreThanTwoADayConflicts() + model.getBackToBackConflictWeight() * getNrBackToBackConflicts()
                + model.getDistanceBackToBackConflictWeight() * getNrDistanceBackToBackConflicts()
                + model.getPeriodWeight() * getPeriodPenalty() + model.getPeriodSizeWeight() * getPeriodPenalty()
                * (exam.getSize() + 1) + model.getPeriodIndexWeight() * getPeriod().getIndex()
                + model.getRoomSizeWeight() * getRoomSizePenalty() + model.getRoomSplitWeight() * getRoomSplitPenalty()
                + model.getExamRotationWeight() * getRotationPenalty() + model.getRoomWeight() * getRoomPenalty()
                + model.getInstructorDirectConflictWeight() * getNrInstructorDirectConflicts()
                + model.getInstructorMoreThanTwoADayWeight() * getNrInstructorMoreThanTwoADayConflicts()
                + model.getInstructorBackToBackConflictWeight() * getNrInstructorBackToBackConflicts()
                + model.getInstructorDistanceBackToBackConflictWeight() * getNrInstructorDistanceBackToBackConflicts()
                + model.getRoomSplitDistanceWeight() * getRoomSplitDistancePenalty() + model.getPerturbationWeight()
                * getPerturbationPenalty() + model.getRoomPerturbationWeight() * getRoomPerturbationPenalty()
                + model.getDistributionWeight() * getDistributionPenalty() + model.getLargeWeight() * getLargePenalty();
    }

    /**
     * Overall cost of using this period. The time cost of an assignment
     * consists of the following criteria:
     * <ul>
     * <li>Direct student conflicts {@link ExamPlacement#getNrDirectConflicts()}
     * , weighted by {@link ExamModel#getDirectConflictWeight()}
     * <li>More than two exams a day student conflicts
     * {@link ExamPlacement#getNrMoreThanTwoADayConflicts()}, weighted by
     * {@link ExamModel#getMoreThanTwoADayWeight()}
     * <li>Back-to-back student conflicts
     * {@link ExamPlacement#getNrBackToBackConflicts()}, weighted by
     * {@link ExamModel#getBackToBackConflictWeight()}
     * <li>Period penalty {@link ExamPlacement#getPeriodPenalty()}, weighted by
     * {@link ExamModel#getPeriodWeight()}
     * <li>Exam rotation penalty {@link ExamPlacement#getRotationPenalty()},
     * weighted by {@link ExamModel#getExamRotationWeight()}
     * <li>Direct instructor conflicts
     * {@link ExamPlacement#getNrInstructorDirectConflicts()}, weighted by
     * {@link ExamModel#getInstructorDirectConflictWeight()}
     * <li>More than two exams a day instructor conflicts
     * {@link ExamPlacement#getNrInstructorMoreThanTwoADayConflicts()}, weighted
     * by {@link ExamModel#getInstructorMoreThanTwoADayWeight()}
     * <li>Back-to-back instructor conflicts
     * {@link ExamPlacement#getNrInstructorBackToBackConflicts()}, weighted by
     * {@link ExamModel#getInstructorBackToBackConflictWeight()}
     * <li>Distance back-to-back instructor conflicts
     * {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()},
     * weighted by
     * {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
     * <li>Front load penalty {@link ExamPlacement#getLargePenalty()}, weighted
     * by {@link ExamModel#getLargeWeight()}
     * </ul>
     */
    public double getTimeCost() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        return model.getDirectConflictWeight() * getNrDirectConflicts() + model.getBackToBackConflictWeight()
                * getNrBackToBackConflicts() + model.getMoreThanTwoADayWeight() * getNrMoreThanTwoADayConflicts()
                + model.getPeriodWeight() * getPeriodPenalty() + model.getPeriodSizeWeight() * getPeriodPenalty()
                * (exam.getSize() + 1) + model.getPeriodIndexWeight() * getPeriod().getIndex()
                + model.getExamRotationWeight() * getRotationPenalty() + model.getInstructorDirectConflictWeight()
                * getNrInstructorDirectConflicts() + model.getInstructorMoreThanTwoADayWeight()
                * getNrInstructorMoreThanTwoADayConflicts() + model.getInstructorBackToBackConflictWeight()
                * getNrInstructorBackToBackConflicts() + model.getPerturbationWeight() * getPerturbationPenalty()
                + model.getDistributionWeight() * getPeriodDistributionPenalty() + model.getLargeWeight()
                * getLargePenalty();
    }

    /**
     * Overall cost of using this set or rooms. The room cost of an assignment
     * consists of the following criteria:
     * <ul>
     * <li>Distance back-to-back student conflicts
     * {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, weighted by
     * {@link ExamModel#getDistanceBackToBackConflictWeight()}
     * <li>Distance back-to-back instructor conflicts
     * {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()},
     * weighted by
     * {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
     * <li>Room size penalty {@link ExamPlacement#getRoomSizePenalty()},
     * weighted by {@link ExamModel#getRoomSizeWeight()}
     * <li>Room split penalty {@link ExamPlacement#getRoomSplitPenalty()},
     * weighted by {@link ExamModel#getRoomSplitWeight()}
     * <li>Room split distance penalty
     * {@link ExamPlacement#getRoomSplitDistancePenalty()}, weighted by
     * {@link ExamModel#getRoomSplitDistanceWeight()}
     * <li>Room penalty {@link ExamPlacement#getRoomPenalty()}, weighted by
     * {@link ExamModel#getRoomWeight()}
     * </ul>
     */
    public double getRoomCost() {
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        return model.getDistanceBackToBackConflictWeight() * getNrDistanceBackToBackConflicts()
                + model.getRoomSizeWeight() * getRoomSizePenalty() + model.getRoomSplitWeight() * getRoomSplitPenalty()
                + model.getRoomWeight() * getRoomPenalty() + model.getInstructorDistanceBackToBackConflictWeight()
                * getNrInstructorDistanceBackToBackConflicts() + model.getRoomSplitDistanceWeight()
                * getRoomSizePenalty() + model.getDistributionWeight() * getRoomDistributionPenalty()
                + model.getRoomPerturbationWeight() * getRoomPerturbationPenalty();
    }

    /**
     * Room names separated with the given delimiter
     */
    public String getRoomName(String delim) {
        String roomName = "";
        for (Iterator<ExamRoomPlacement> i = getRoomPlacements().iterator(); i.hasNext();) {
            ExamRoomPlacement r = i.next();
            roomName += r.getRoom().getName();
            if (i.hasNext())
                roomName += delim;
        }
        return roomName;
    }

    /**
     * Assignment name (period / room(s))
     */
    @Override
    public String getName() {
        return getPeriod() + "/" + getRoomName(",");
    }

    /**
     * String representation -- returns a list of assignment costs
     */
    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00");
        Exam exam = variable();
        ExamModel model = (ExamModel) exam.getModel();
        return variable().getName() + " = " + getName() + " (" + df.format(toDouble()) + "/" + "DC:"
                + getNrDirectConflicts() + "," + "M2D:" + getNrMoreThanTwoADayConflicts() + "," + "BTB:"
                + getNrBackToBackConflicts() + ","
                + (model.getBackToBackDistance() < 0 ? "" : "dBTB:" + getNrDistanceBackToBackConflicts() + ",") + "PP:"
                + getPeriodPenalty() + "," + "@P:" + getRotationPenalty() + "," + "RSz:" + getRoomSizePenalty() + ","
                + "RSp:" + getRoomSplitPenalty() + "," + "RD:" + df.format(getRoomSplitDistancePenalty()) + "," + "RP:"
                + getRoomPenalty()
                + (model.isMPP() ? ",IP:" + getPerturbationPenalty() + ",IRP:" + getRoomPerturbationPenalty() : "")
                + ")";
    }

    /**
     * Compare two assignments for equality
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ExamPlacement))
            return false;
        ExamPlacement p = (ExamPlacement) o;
        return p.variable().equals(variable()) && p.getPeriod().equals(getPeriod())
                && p.getRoomPlacements().equals(getRoomPlacements());
    }

    /**
     * Hash code
     */
    @Override
    public int hashCode() {
        return iHashCode;
    }

    /**
     * True if given room is between {@link ExamPlacement#getRoomPlacements()}
     */
    public boolean contains(ExamRoom room) {
        return getRoomPlacements().contains(new ExamRoomPlacement(room));
    }
}
