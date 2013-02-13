package net.sf.cpsolver.exam.model;

/**
 * Representation of a room placement of an exam. It contains a room
 * {@link ExamRoom} and a penalty associated with a placement of an exam into
 * the given room. <br>
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
public class ExamRoomPlacement implements Comparable<ExamRoomPlacement> {
    private ExamRoom iRoom;
    private int iPenalty = 0;
    private int iMaxPenalty = 100;

    /**
     * Constructor
     * 
     * @param room
     *            examination room
     */
    public ExamRoomPlacement(ExamRoom room) {
        iRoom = room;
    }

    /**
     * Constructor
     * 
     * @param room
     *            examination room
     * @param penalty
     *            penalty for using this room
     */
    public ExamRoomPlacement(ExamRoom room, int penalty) {
        this(room);
        iPenalty = penalty;
    }

    /**
     * Constructor
     * 
     * @param room
     *            examination room
     * @param penalty
     *            penalty for using this room
     * @param maxPenalty
     *            maximal penalty imposed of
     *            {@link ExamRoom#getPenalty(ExamPeriod)}, i.e., a placement
     *            with greater penalty is not allowed to be made
     */
    public ExamRoomPlacement(ExamRoom room, int penalty, int maxPenalty) {
        this(room, penalty);
        iMaxPenalty = maxPenalty;
    }

    /** Examination room */
    public ExamRoom getRoom() {
        return iRoom;
    }

    /** Examination room id */
    public long getId() {
        return getRoom().getId();
    }

    /** Examination room name */
    public String getName() {
        return getRoom().getName();
    }

    /** Examination room availability */
    public boolean isAvailable(ExamPeriod period) {
        return iRoom.isAvailable(period) && iRoom.getPenalty(period) <= iMaxPenalty;
    }

    /**
     * Penalty for assignment of an exam into this room
     * {@link Exam#getRoomPlacements()}
     */
    public int getPenalty() {
        return iPenalty;
    }

    /**
     * Maximal penalty imposed of {@link ExamRoom#getPenalty(ExamPeriod)}, i.e.,
     * a placement with greater penalty is not allowed to be made
     */
    public int getMaxPenalty() {
        return iMaxPenalty;
    }

    /**
     * Penalty for assignment of an exam into this room
     * {@link Exam#getRoomPlacements()}
     */
    public void setPenalty(int penalty) {
        iPenalty = penalty;
    }

    /**
     * Maximal penalty imposed of {@link ExamRoom#getPenalty(ExamPeriod)}, i.e.,
     * a placement with greater penalty is not allowed to be made
     */
    public void setMaxPenalty(int maxPenalty) {
        iMaxPenalty = maxPenalty;
    }

    /**
     * Penalty for assignment of an exam into this room
     * {@link Exam#getRoomPlacements()} and the given examination period
     * 
     * @return {@link ExamRoomPlacement#getPenalty()} +
     *         {@link ExamRoom#getPenalty(ExamPeriod)}
     */
    public int getPenalty(ExamPeriod period) {
        return (iPenalty != 0 ? iPenalty : iRoom.getPenalty(period));
    }

    /**
     * Room size
     * 
     * @param altSeating
     *            examination seeting (pass {@link Exam#hasAltSeating()})
     * @return room size or room alternative size, based on given seating
     */
    public int getSize(boolean altSeating) {
        return (altSeating ? getRoom().getAltSize() : getRoom().getSize());
    }

    /**
     * Room distance
     * 
     * @return appropriate {@link ExamRoom#getDistanceInMeters(ExamRoom)}
     */
    public double getDistanceInMeters(ExamRoomPlacement other) {
        return getRoom().getDistanceInMeters(other.getRoom());
    }

    /**
     * Hash code
     */
    @Override
    public int hashCode() {
        return getRoom().hashCode();
    }

    @Override
    public String toString() {
        return getRoom().toString() + (getPenalty() == 0 ? "" : "/" + getPenalty());
    }

    /** Compare two room placements for equality */
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof ExamRoomPlacement) {
            return getRoom().equals(((ExamRoomPlacement) o).getRoom());
        } else if (o instanceof ExamRoom) {
            return getRoom().equals(o);
        }
        return false;
    }

    /** Compare two room placements */
    @Override
    public int compareTo(ExamRoomPlacement o) {
        return getRoom().compareTo(o.getRoom());
    }
}
