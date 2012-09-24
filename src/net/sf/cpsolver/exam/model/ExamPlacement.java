package net.sf.cpsolver.exam.model;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.exam.criteria.DistributionPenalty;
import net.sf.cpsolver.exam.criteria.ExamCriterion;
import net.sf.cpsolver.exam.criteria.ExamRotationPenalty;
import net.sf.cpsolver.exam.criteria.InstructorBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.InstructorDirectConflicts;
import net.sf.cpsolver.exam.criteria.InstructorDistanceBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.InstructorMoreThan2ADayConflicts;
import net.sf.cpsolver.exam.criteria.InstructorNotAvailableConflicts;
import net.sf.cpsolver.exam.criteria.LargeExamsPenalty;
import net.sf.cpsolver.exam.criteria.PeriodPenalty;
import net.sf.cpsolver.exam.criteria.PerturbationPenalty;
import net.sf.cpsolver.exam.criteria.RoomPenalty;
import net.sf.cpsolver.exam.criteria.RoomPerturbationPenalty;
import net.sf.cpsolver.exam.criteria.RoomSizePenalty;
import net.sf.cpsolver.exam.criteria.RoomSplitDistancePenalty;
import net.sf.cpsolver.exam.criteria.RoomSplitPenalty;
import net.sf.cpsolver.exam.criteria.StudentBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.StudentDirectConflicts;
import net.sf.cpsolver.exam.criteria.StudentDistanceBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.StudentMoreThan2ADayConflicts;
import net.sf.cpsolver.exam.criteria.StudentNotAvailableConflicts;
import net.sf.cpsolver.ifs.criteria.Criterion;
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
        for (ExamRoomPlacement r : iRoomPlacements)
            iSize += r.getSize(exam.hasAltSeating());
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
    @Deprecated
    public int getSize() {
        return iSize;
    }

    /**
     * Number of direct student conflicts, i.e., number of cases when this exam
     * is attended by a student that attends some other exam at the same period
     */
    @Deprecated
    public int getNrDirectConflicts() {
        return (int)variable().getModel().getCriterion(StudentDirectConflicts.class).getValue(this, null) +
               (int)variable().getModel().getCriterion(StudentNotAvailableConflicts.class).getValue(this, null);
    }

    /**
     * Number of direct student conflicts caused by the fact that a student is
     * not available
     */
    @Deprecated
    public int getNrNotAvailableConflicts() {
        return (int)variable().getModel().getCriterion(StudentNotAvailableConflicts.class).getValue(this, null);
    }

    /**
     * Number of back-to-back student conflicts, i.e., number of cases when this
     * exam is attended by a student that attends some other exam at the
     * previous {@link ExamPeriod#prev()} or following {@link ExamPeriod#next()}
     * period. If {@link ExamModel#isDayBreakBackToBack()} is false,
     * back-to-back conflicts are only considered between consecutive periods
     * that are of the same day.
     */
    @Deprecated
    public int getNrBackToBackConflicts() {
        return (int)variable().getModel().getCriterion(StudentBackToBackConflicts.class).getValue(this, null);
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
    @Deprecated
    public int getNrDistanceBackToBackConflicts() {
        return (int)variable().getModel().getCriterion(StudentDistanceBackToBackConflicts.class).getValue(this, null);
    }

    /**
     * Number of more than two exams a day student conflicts, i.e., when this
     * exam is attended by a student that attends two or more other exams at the
     * same day.
     */
    @Deprecated
    public int getNrMoreThanTwoADayConflicts() {
        return (int)variable().getModel().getCriterion(StudentMoreThan2ADayConflicts.class).getValue(this, null);
    }

    /**
     * Number of direct instructor conflicts, i.e., number of cases when this
     * exam is attended by an instructor that attends some other exam at the
     * same period
     */
    @Deprecated
    public int getNrInstructorDirectConflicts() {
        return (int)variable().getModel().getCriterion(InstructorDirectConflicts.class).getValue(this, null) +
               (int)variable().getModel().getCriterion(InstructorNotAvailableConflicts.class).getValue(this, null);
    }

    /**
     * Number of direct instructor conflicts caused by the fact that a student
     * is not available
     */
    @Deprecated
    public int getNrInstructorNotAvailableConflicts() {
        return (int)variable().getModel().getCriterion(InstructorNotAvailableConflicts.class).getValue(this, null);
    }

    /**
     * Number of back-to-back instructor conflicts, i.e., number of cases when
     * this exam is attended by an instructor that attends some other exam at
     * the previous {@link ExamPeriod#prev()} or following
     * {@link ExamPeriod#next()} period. If
     * {@link ExamModel#isDayBreakBackToBack()} is false, back-to-back conflicts
     * are only considered between consecutive periods that are of the same day.
     */
    @Deprecated
    public int getNrInstructorBackToBackConflicts() {
        return (int)variable().getModel().getCriterion(InstructorBackToBackConflicts.class).getValue(this, null);
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
    @Deprecated
    public int getNrInstructorDistanceBackToBackConflicts() {
        return (int)variable().getModel().getCriterion(InstructorDistanceBackToBackConflicts.class).getValue(this, null);
    }

    /**
     * Number of more than two exams a day instructor conflicts, i.e., when this
     * exam is attended by an instructor student that attends two or more other
     * exams at the same day.
     */
    @Deprecated
    public int getNrInstructorMoreThanTwoADayConflicts() {
        return (int)variable().getModel().getCriterion(InstructorMoreThan2ADayConflicts.class).getValue(this, null);
    }

    /**
     * Cost for using room(s) that are too big
     * 
     * @return difference between total room size (computed using either
     *         {@link ExamRoom#getSize()} or {@link ExamRoom#getAltSize()} based
     *         on {@link Exam#hasAltSeating()}) and the number of students
     *         {@link Exam#getSize()}
     */
    @Deprecated
    public int getRoomSizePenalty() {
        return (int)variable().getModel().getCriterion(RoomSizePenalty.class).getValue(this, null);
    }

    /**
     * Cost for using more than one room (nrSplits^2).
     * 
     * @return penalty (1 for 2 rooms, 4 for 3 rooms, 9 for 4 rooms, etc.)
     */
    @Deprecated
    public int getRoomSplitPenalty() {
        return (int)variable().getModel().getCriterion(RoomSplitPenalty.class).getValue(this, null);
    }

    /**
     * Cost for using a period, i.e., {@link ExamPeriodPlacement#getPenalty()}
     */
    @Deprecated
    public int getPeriodPenalty() {
        return (int)variable().getModel().getCriterion(PeriodPenalty.class).getValue(this, null);
    }

    /**
     * Rotation penalty (an exam that has been in later period last times tries
     * to be in an earlier period)
     */
    @Deprecated
    public int getRotationPenalty() {
        return (int)variable().getModel().getCriterion(ExamRotationPenalty.class).getValue(this, null);
    }

    /**
     * Front load penalty (large exam is discouraged to be placed on or after a
     * certain period)
     * 
     * @return zero if not large exam or if before
     *         {@link ExamModel#getLargePeriod()}, one otherwise
     */
    @Deprecated
    public int getLargePenalty() {
        return (int)variable().getModel().getCriterion(LargeExamsPenalty.class).getValue(this, null);
    }

    /**
     * Room penalty (penalty for using given rooms), i.e., sum of
     * {@link ExamRoomPlacement#getPenalty(ExamPeriod)} of assigned rooms
     */
    @Deprecated
    public int getRoomPenalty() {
        return (int)variable().getModel().getCriterion(RoomPenalty.class).getValue(this, null);
    }

    /**
     * Perturbation penalty, i.e., penalty for using a different assignment than
     * initial. Only applicable when {@link ExamModel#isMPP()} is true (minimal
     * perturbation problem).
     * 
     * @return |period index - initial period index | * exam size
     */
    @Deprecated
    public int getPerturbationPenalty() {
        return (int)variable().getModel().getCriterion(PerturbationPenalty.class).getValue(this, null);
    }

    /**
     * Room perturbation penalty, i.e., number of assigned rooms different from
     * initial. Only applicable when {@link ExamModel#isMPP()} is true (minimal
     * perturbation problem).
     * 
     * @return |period index - initial period index | * exam size
     */
    @Deprecated
    public int getRoomPerturbationPenalty() {
        return (int)variable().getModel().getCriterion(RoomPerturbationPenalty.class).getValue(this, null);
    }

    /**
     * Room split distance penalty, i.e., average distance between two rooms of
     * this placement
     */
    @Deprecated
    public double getRoomSplitDistancePenalty() {
        return variable().getModel().getCriterion(RoomSplitDistancePenalty.class).getValue(this, null);
    }

    /**
     * Distribution penalty, i.e., sum weights of violated distribution
     * constraints
     */
    @Deprecated
    public double getDistributionPenalty() {
        return variable().getModel().getCriterion(DistributionPenalty.class).getValue(this, null);
    }

    /**
     * Room related distribution penalty, i.e., sum weights of violated
     * distribution constraints
     */
    @Deprecated
    public double getRoomDistributionPenalty() {
        return ((DistributionPenalty)variable().getModel().getCriterion(DistributionPenalty.class)).getRoomValue(this);
    }

    /**
     * Period related distribution penalty, i.e., sum weights of violated
     * distribution constraints
     */
    @Deprecated
    public double getPeriodDistributionPenalty() {
        return ((DistributionPenalty)variable().getModel().getCriterion(DistributionPenalty.class)).getPeriodValue(this);
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
        double ret = 0.0;
        for (Criterion<Exam, ExamPlacement> criterion: variable().getModel().getCriteria())
            ret += criterion.getWeightedValue(this, null);
        return ret;
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
        double weight = 0.0;
        for (Criterion<Exam, ExamPlacement> criterion: variable().getModel().getCriteria()) {
            if (((ExamCriterion)criterion).isPeriodCriterion())
                weight += criterion.getWeight() * ((ExamCriterion)criterion).getPeriodValue(this);
        }
        return weight;
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
        double weight = 0.0;
        for (Criterion<Exam, ExamPlacement> criterion: variable().getModel().getCriteria()) {
            if (((ExamCriterion)criterion).isRoomCriterion())
                weight += criterion.getWeight() * ((ExamCriterion)criterion).getRoomValue(this);
        }
        return weight;
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
        return getPeriod() + " " + getRoomName(",");
    }

    /**
     * String representation -- returns a list of assignment costs
     */
    @Override
    public String toString() {
        String ret = "";
        for (Criterion<Exam, ExamPlacement> criterion: variable().getModel().getCriteria()) {
            String val = criterion.toString();
            if (!val.isEmpty())
                ret += (!ret.isEmpty() && !ret.endsWith(",") ? "," : "") + val;
        }
        return variable().getName() + " = " + getName() + " (" + new DecimalFormat("0.00").format(toDouble()) + "/" + ret + ")";
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
