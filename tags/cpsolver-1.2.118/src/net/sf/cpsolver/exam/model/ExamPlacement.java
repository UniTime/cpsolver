package net.sf.cpsolver.exam.model;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.exam.criteria.ExamCriterion;
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

    private Integer iHashCode = null;

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
     * Overall cost of using this placement.
     */
    @Override
    public double toDouble() {
        double ret = 0.0;
        for (Criterion<Exam, ExamPlacement> criterion: variable().getModel().getCriteria())
            ret += criterion.getWeightedValue(this, null);
        return ret;
    }

    /**
     * Overall cost of using this period.
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
     * Overall cost of using this set or rooms.
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
        if (iHashCode == null) iHashCode = getName().hashCode();
        return iHashCode;
    }

    /**
     * True if given room is between {@link ExamPlacement#getRoomPlacements()}
     */
    public boolean contains(ExamRoom room) {
        return getRoomPlacements().contains(new ExamRoomPlacement(room));
    }
}
