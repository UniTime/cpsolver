package org.cpsolver.exam.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.ConstraintListener;
import org.cpsolver.ifs.util.DistanceMetric;


/**
 * A room. Only one exam can use a room at a time (period). <br>
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
public class ExamRoom extends ConstraintWithContext<Exam, ExamPlacement, ExamRoom.ExamRoomContext> {
    private boolean[] iAvailable;
    private int[] iPenalty;
    private String iName;
    private int iSize, iAltSize;
    private Double iCoordX, iCoordY;
    private boolean iHard = true;
    
    private ExamRoom iParentRoom;
    private List<ExamRoom> iPartitions;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     * @param id
     *            unique id
     * @param name room name
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
        iId = id;
        iName = name;
        iCoordX = coordX;
        iCoordY = coordY;
        iSize = size;
        iAltSize = altSize;
        iAvailable = new boolean[model.getNrPeriods()];
        iPenalty = new int[model.getNrPeriods()];
        for (int i = 0; i < iAvailable.length; i++) {
            iAvailable[i] = true;
            iPenalty[i] = 0;
        }
    }
    
    public void setHard(boolean hard) { iHard = hard; }
    
    @Override
    public boolean isHard() { return iHard; }
    
    private Map<Long, Double> iDistanceCache = new HashMap<Long, Double>();
    /**
     * Distance between two rooms. See {@link DistanceMetric}
     * 
     * @param other
     *            another room
     * @return distance between this and the given room
     */
    public double getDistanceInMeters(ExamRoom other) {
        synchronized (iDistanceCache) {
            Double distance = iDistanceCache.get(other.getId());
            if (distance == null) {
                distance = ((ExamModel)getModel()).getDistanceMetric().getDistanceInMeters(getId(), getCoordX(), getCoordY(), other.getId(), other.getCoordX(), other.getCoordY());
                iDistanceCache.put(other.getId(), distance);
            }
            return distance;
        }
    }

    /**
     * Normal seating capacity (to be used when {@link Exam#hasAltSeating()} is
     * false)
     * @return room normal seating capacity
     */
    public int getSize() {
        return iSize;
    }

    /**
     * Alternating seating capacity (to be used when
     * {@link Exam#hasAltSeating()} is true)
     * @return room examination seating capacity
     */
    public int getAltSize() {
        return iAltSize;
    }

    /**
     * X coordinate
     * @return X-coordinate (latitude)
     */
    public Double getCoordX() {
        return iCoordX;
    }

    /**
     * Y coordinate
     * @return Y-coordinate (longitude)
     */
    public Double getCoordY() {
        return iCoordY;
    }

    /**
     * Exams placed at the given period
     * 
     * @param assignment current assignment
     * @param period
     *            a period
     * @return a placement of an exam in this room at the given period, null if
     *         unused (multiple placements can be returned if the room is shared between
     *         two or more exams)
     */
    public List<ExamPlacement> getPlacements(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        return getContext(assignment).getPlacements(period.getIndex());
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
     * True if the room is available during at least one period,
     * @return true if there is an examination period at which the room is available
     */
    public boolean isAvailable() {
        for (boolean a: iAvailable)
            if (a) return true;
        return false;
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

    /** Return room penalty for given period 
     * @param period given period
     * @return room penalty for the given period
     **/
    public int getPenalty(ExamPeriod period) {
        return iPenalty[period.getIndex()];
    }

    public int getPenalty(int period) {
        return iPenalty[period];
    }

    /** Set room penalty for given period 
     * @param period given period
     * @param penalty penalty for the given period
     **/
    public void setPenalty(ExamPeriod period, int penalty) {
        iPenalty[period.getIndex()] = penalty;
    }

    public void setPenalty(int period, int penalty) {
        iPenalty[period] = penalty;
    }
    
    
    public ExamRoomSharing getRoomSharing() {
        return ((ExamModel)getModel()).getRoomSharing();
    }
    
    /**
     * Compute conflicts if the given exam is assigned in this room and the given period
     */
    public void computeConflicts(Assignment<Exam, ExamPlacement> assignment, Exam exam, ExamPeriod period, Set<ExamPlacement> conflicts) {
        boolean single = exam.getSize() <= (exam.hasAltSeating() ? getAltSize() : getSize());
        if (getRoomSharing() == null || !single) {
            for (ExamPlacement conflict: getContext(assignment).getPlacements(period.getIndex()))
                if (!conflict.variable().equals(exam))
                    conflicts.add(conflict);
            if (getParentRoom() != null && getParentRoom().isHard()) {
                for (ExamPlacement conflict: getParentRoom().getContext(assignment).getPlacements(period.getIndex()))
                    if (!conflict.variable().equals(exam))
                        conflicts.add(conflict);
            }
            if (getPartitions() != null) {
                for (ExamRoom partition: getPartitions()) {
                    if (partition.isHard())
                        for (ExamPlacement conflict: partition.getContext(assignment).getPlacements(period.getIndex()))
                            if (!conflict.variable().equals(exam))
                                conflicts.add(conflict);
                }
            }
        } else {
            if (getParentRoom() != null && getParentRoom().isHard()) {
                for (ExamPlacement conflict: getParentRoom().getContext(assignment).getPlacements(period.getIndex()))
                    if (!conflict.variable().equals(exam))
                        conflicts.add(conflict);
            }
            if (getPartitions() != null) {
                for (ExamRoom partition: getPartitions()) {
                    if (partition.isHard())
                        for (ExamPlacement conflict: partition.getContext(assignment).getPlacements(period.getIndex()))
                            if (!conflict.variable().equals(exam))
                                conflicts.add(conflict);
                }
            }
            getRoomSharing().computeConflicts(exam, getContext(assignment).getPlacements(period.getIndex()), this, conflicts);
        }
    }

    /**
     * Check for conflicts if the given exam is assigned in this room and the given period
     */
    public boolean inConflict(Assignment<Exam, ExamPlacement> assignment, Exam exam, ExamPeriod period) {
        boolean single = exam.getSize() <= (exam.hasAltSeating() ? getAltSize() : getSize());
        if (getRoomSharing() == null || !single) {
            for (ExamPlacement conflict: getContext(assignment).getPlacements(period.getIndex()))
                if (!conflict.variable().equals(exam)) return true;
            if (getParentRoom() != null && getParentRoom().isHard()) {
                for (ExamPlacement conflict: getParentRoom().getContext(assignment).getPlacements(period.getIndex()))
                    if (!conflict.variable().equals(exam)) return true;
            }
            if (getPartitions() != null) {
                for (ExamRoom partition: getPartitions()) {
                    if (partition.isHard())
                        for (ExamPlacement conflict: partition.getContext(assignment).getPlacements(period.getIndex()))
                            if (!conflict.variable().equals(exam)) return true;
                }
            }
            return false;
        } else {
            if (getParentRoom() != null && getParentRoom().isHard()) {
                for (ExamPlacement conflict: getParentRoom().getContext(assignment).getPlacements(period.getIndex()))
                    if (!conflict.variable().equals(exam)) return true;
            }
            if (getPartitions() != null) {
                for (ExamRoom partition: getPartitions()) {
                    if (partition.isHard())
                        for (ExamPlacement conflict: partition.getContext(assignment).getPlacements(period.getIndex()))
                            if (!conflict.variable().equals(exam)) return true;
                }
            }
            return getRoomSharing().inConflict(exam, getContext(assignment).getPlacements(period.getIndex()), this);
        }
    }

    /**
     * Compute conflicts between the given assignment of an exam and all the
     * current assignments (of this room)
     */
    @Override
    public void computeConflicts(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p, Set<ExamPlacement> conflicts) {
        if (!isHard()) return;
        if (!p.contains(this)) return;
        
        if (getParentRoom() != null && p.contains(getParentRoom())) {
            conflicts.add(p); return;
        }
        
        computeConflicts(assignment, p.variable(), p.getPeriod(), conflicts);
    }

    /**
     * Checks whether there is a conflict between the given assignment of an
     * exam and all the current assignments (of this room)
     */
    @Override
    public boolean inConflict(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p) {
        if (!isHard()) return false;
        if (!p.contains(this)) return false;
        if (getParentRoom() != null && p.contains(getParentRoom())) return false;
        
        return inConflict(assignment, p.variable(), p.getPeriod());
    }

    /**
     * False if the given two assignments are using this room at the same period
     */
    @Override
    public boolean isConsistent(ExamPlacement p1, ExamPlacement p2) {
        return !isHard() || (p1.getPeriod() != p2.getPeriod() || !p1.contains(this) || !p2.contains(this));
    }

    /**
     * An exam was assigned, update room assignment table
     */
    @Override
    public void assigned(Assignment<Exam, ExamPlacement> assignment, long iteration, ExamPlacement p) {
        if (p.contains(this)) {
            if (!getContext(assignment).getPlacements(p.getPeriod().getIndex()).isEmpty() || getParentRoom() != null || getPartitions() != null) {
                HashSet<ExamPlacement> confs = new HashSet<ExamPlacement>();
                computeConflicts(assignment, p, confs);
                for (ExamPlacement conf: confs)
                    assignment.unassign(iteration, conf.variable());
                if (iConstraintListeners != null) {
                    for (ConstraintListener<Exam, ExamPlacement> listener : iConstraintListeners)
                        listener.constraintAfterAssigned(assignment, iteration, this, p, confs);
                }
            }
            getContext(assignment).assigned(assignment, p);
        }
    }

    /**
     * An exam was unassigned, update room assignment table
     */
    @Override
    public void unassigned(Assignment<Exam, ExamPlacement> assignment, long iteration, ExamPlacement p) {
        if (p.contains(this))
            getContext(assignment).unassigned(assignment, p);
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
     * @return true if the room name is set and not empty
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
     * Add partition of this room. This room is unavailable at a time when one of the partition 
     * is not available and vice versa.
     * @param room room partition
     */
    public void addPartition(ExamRoom room) {
        room.iParentRoom = this;
        if (iPartitions == null) iPartitions = new ArrayList<ExamRoom>();
        iPartitions.add(room);
    }
    
    /**
     * If this room is a partition of some other room, returns the parent room (which is partitioned).
     * @return parent room
     */
    public ExamRoom getParentRoom() { return iParentRoom; }
    
    /**
     * If this room is partitioned into multiple rooms, return room partitions
     * @return room partitions
     */
    public List<ExamRoom> getPartitions() { return iPartitions; }


    /**
     * Compare two rooms (by unique id)
     */
    @Override
    public int compareTo(Constraint<Exam, ExamPlacement> o) {
        return toString().compareTo(o.toString());
    }
    
    @Override
    public ExamRoomContext createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new ExamRoomContext(assignment);
    }
    
    public class ExamRoomContext implements AssignmentConstraintContext<Exam, ExamPlacement> {
        private List<ExamPlacement>[] iTable;
        
        @SuppressWarnings("unchecked")
        public ExamRoomContext(Assignment<Exam, ExamPlacement> assignment) {
            ExamModel model = (ExamModel)getModel();
            iTable = new List[model.getNrPeriods()];
            for (int i = 0; i < iTable.length; i++)
                iTable[i] = new ArrayList<ExamPlacement>();
            for (Exam exam: variables()) {
                ExamPlacement placement = assignment.getValue(exam);
                if (placement != null && placement.contains(ExamRoom.this))
                    iTable[placement.getPeriod().getIndex()].add(placement);
            }
        }

        @Override
        public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            if (placement.contains(ExamRoom.this))
                iTable[placement.getPeriod().getIndex()].add(placement);
        }
        
        @Override
        public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            if (placement.contains(ExamRoom.this))
                iTable[placement.getPeriod().getIndex()].remove(placement);
        }
        
        public List<ExamPlacement> getPlacements(int period) { return iTable[period]; }
    }
    
    
    /**
     * Check that the room and its parent are not used at the same time
     */
    public static boolean checkParents(Collection<ExamRoomPlacement> roomsSoFar, ExamRoomPlacement room) {
        if (room.getRoom().getParentRoom() != null) {
            // check if already lists the parent
            for (ExamRoomPlacement r: roomsSoFar)
                if (r.getRoom().equals(room.getRoom().getParentRoom())) return false;
        }
        if (room.getRoom().getPartitions() != null) {
            // check if has any of the partitions
            for (ExamRoomPlacement r: roomsSoFar)
                if (room.getRoom().getPartitions().contains(r.getRoom())) return false;
        }
        return true;
    }
}
