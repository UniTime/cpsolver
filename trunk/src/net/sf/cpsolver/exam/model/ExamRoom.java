package net.sf.cpsolver.exam.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.util.DistanceMetric;

/**
 * A room. Only one exam can use a room at a time (period). <br>
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
public class ExamRoom extends Constraint<Exam, ExamPlacement> {
    private ExamPlacement[] iTable;
    private boolean[] iAvailable;
    private int[] iPenalty;
    private String iName;
    private int iSize, iAltSize;
    private Double iCoordX, iCoordY;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     * @param id
     *            unique id
     * @param size
     *            room (normal) seating capacity
     * @param altSize
     *            room alternating seating capacity (to be used when
     *            {@link Exam#hasAltSeating()} is true)
     * @param coordX
     *            x coordinate
     * @param coordY
     *            y coordinate
     */
    public ExamRoom(ExamModel model, long id, String name, int size, int altSize, Double coordX, Double coordY) {
        super();
        iAssignedVariables = null;
        iId = id;
        iName = name;
        iCoordX = coordX;
        iCoordY = coordY;
        iSize = size;
        iAltSize = altSize;
        iTable = new ExamPlacement[model.getNrPeriods()];
        iAvailable = new boolean[model.getNrPeriods()];
        iPenalty = new int[model.getNrPeriods()];
        for (int i = 0; i < iTable.length; i++) {
            iTable[i] = null;
            iAvailable[i] = true;
            iPenalty[i] = 0;
        }
    }

    /**
     * Distance between two rooms. See {@link DistanceMetric}
     * 
     * @param other
     *            another room
     * @return distance between this and the given room
     */
    public double getDistanceInMeters(ExamRoom other) {
        return ((ExamModel)getModel()).getDistanceMetric().getDistanceInMeters(getCoordX(), getCoordY(), other.getCoordX(), other.getCoordY());
    }

    /**
     * Normal seating capacity (to be used when {@link Exam#hasAltSeating()} is
     * false)
     */
    public int getSize() {
        return iSize;
    }

    /**
     * Alternating seating capacity (to be used when
     * {@link Exam#hasAltSeating()} is true)
     */
    public int getAltSize() {
        return iAltSize;
    }

    /**
     * X coordinate
     */
    public Double getCoordX() {
        return iCoordX;
    }

    /**
     * Y coordinate
     */
    public Double getCoordY() {
        return iCoordY;
    }

    /**
     * An exam placed at the given period
     * 
     * @param period
     *            a period
     * @return a placement of an exam in this room at the given period, null if
     *         unused
     */
    public ExamPlacement getPlacement(ExamPeriod period) {
        return iTable[period.getIndex()];
    }

    /**
     * True if the room is available (for examination timetabling) during the
     * given period
     * 
     * @param period
     *            a period
     * @return true if an exam can be scheduled into this room at the given
     *         period, false if otherwise
     */
    public boolean isAvailable(ExamPeriod period) {
        return iAvailable[period.getIndex()];
    }

    public boolean isAvailable(int period) {
        return iAvailable[period];
    }

    /**
     * Set whether the room is available (for examination timetabling) during
     * the given period
     * 
     * @param period
     *            a period
     * @param available
     *            true if an exam can be scheduled into this room at the given
     *            period, false if otherwise
     */
    public void setAvailable(ExamPeriod period, boolean available) {
        iAvailable[period.getIndex()] = available;
    }

    public void setAvailable(int period, boolean available) {
        iAvailable[period] = available;
    }

    /** Return room penalty for given period */
    public int getPenalty(ExamPeriod period) {
        return iPenalty[period.getIndex()];
    }

    public int getPenalty(int period) {
        return iPenalty[period];
    }

    /** Set room penalty for given period */
    public void setPenalty(ExamPeriod period, int penalty) {
        iPenalty[period.getIndex()] = penalty;
    }

    public void setPenalty(int period, int penalty) {
        iPenalty[period] = penalty;
    }

    /**
     * Compute conflicts between the given assignment of an exam and all the
     * current assignments (of this room)
     */
    @Override
    public void computeConflicts(ExamPlacement p, Set<ExamPlacement> conflicts) {
        if (!p.contains(this))
            return;
        if (iTable[p.getPeriod().getIndex()] != null
                && !iTable[p.getPeriod().getIndex()].variable().equals(p.variable()))
            conflicts.add(iTable[p.getPeriod().getIndex()]);
    }

    /**
     * Checks whether there is a conflict between the given assignment of an
     * exam and all the current assignments (of this room)
     */
    @Override
    public boolean inConflict(ExamPlacement p) {
        if (!p.contains(this))
            return false;
        return iTable[p.getPeriod().getIndex()] != null
                && !iTable[p.getPeriod().getIndex()].variable().equals(p.variable());
    }

    /**
     * False if the given two assignments are using this room at the same period
     */
    @Override
    public boolean isConsistent(ExamPlacement p1, ExamPlacement p2) {
        return (p1.getPeriod() != p2.getPeriod() || !p1.contains(this) || !p2.contains(this));
    }

    /**
     * An exam was assigned, update room assignment table
     */
    @Override
    public void assigned(long iteration, ExamPlacement p) {
        if (p.contains(this) && iTable[p.getPeriod().getIndex()] != null) {
            if (iConstraintListeners != null) {
                HashSet<ExamPlacement> confs = new HashSet<ExamPlacement>();
                confs.add(iTable[p.getPeriod().getIndex()]);
                for (ConstraintListener<ExamPlacement> listener : iConstraintListeners)
                    listener.constraintAfterAssigned(iteration, this, p, confs);
            }
            iTable[p.getPeriod().getIndex()].variable().unassign(iteration);
        }
    }

    /**
     * An exam was assigned, update room assignment table
     */
    public void afterAssigned(long iteration, ExamPlacement p) {
        if (p.contains(this))
            iTable[p.getPeriod().getIndex()] = p;
    }

    /**
     * An exam was unassigned, update room assignment table
     */
    @Override
    public void unassigned(long iteration, ExamPlacement p) {
        // super.unassigned(iteration, p);
    }

    /**
     * An exam was unassigned, update room assignment table
     */
    public void afterUnassigned(long iteration, ExamPlacement p) {
        // super.unassigned(iteration, p);
        if (p.contains(this)) {
            iTable[p.getPeriod().getIndex()] = null;
        }
    }

    /**
     * Checks two rooms for equality
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ExamRoom))
            return false;
        ExamRoom r = (ExamRoom) o;
        return getId() == r.getId();
    }

    /**
     * Hash code
     */
    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    /**
     * Room name
     */
    @Override
    public String getName() {
        return (hasName() ? iName : String.valueOf(getId()));
    }

    /**
     * Room name
     */
    public boolean hasName() {
        return (iName != null && iName.length() > 0);
    }

    /**
     * Room unique id
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Compare two rooms (by unique id)
     */
    @Override
    public int compareTo(Constraint<Exam, ExamPlacement> o) {
        return toString().compareTo(o.toString());
    }
}
