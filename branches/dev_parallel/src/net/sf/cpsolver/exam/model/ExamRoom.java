package net.sf.cpsolver.exam.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import net.sf.cpsolver.ifs.assignment.context.ConstraintWithContext;
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
public class ExamRoom extends ConstraintWithContext<Exam, ExamPlacement, ExamRoom.ExamRoomContext> {
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
     * Exams placed at the given period
     * 
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
    
    
    public ExamRoomSharing getRoomSharing() {
        return ((ExamModel)getModel()).getRoomSharing();
    }

    /**
     * Compute conflicts between the given assignment of an exam and all the
     * current assignments (of this room)
     */
    @Override
    public void computeConflicts(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p, Set<ExamPlacement> conflicts) {
        if (!p.contains(this)) return;
        
        if (getRoomSharing() == null) {
            for (ExamPlacement conflict: getContext(assignment).getPlacements(p.getPeriod().getIndex()))
                if (!conflict.variable().equals(p.variable()))
                    conflicts.add(conflict);
        } else {
            getRoomSharing().computeConflicts(p, getContext(assignment).getPlacements(p.getPeriod().getIndex()), this, conflicts);
        }
    }

    /**
     * Checks whether there is a conflict between the given assignment of an
     * exam and all the current assignments (of this room)
     */
    @Override
    public boolean inConflict(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p) {
        if (!p.contains(this)) return false;
        
        if (getRoomSharing() == null) {
            for (ExamPlacement conflict: getContext(assignment).getPlacements(p.getPeriod().getIndex()))
                if (!conflict.variable().equals(p.variable())) return true;
            return false;
        } else {
            return getRoomSharing().inConflict(p, getContext(assignment).getPlacements(p.getPeriod().getIndex()), this);
        }
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
    public void assigned(Assignment<Exam, ExamPlacement> assignment, long iteration, ExamPlacement p) {
        if (p.contains(this)) {
            if (!getContext(assignment).getPlacements(p.getPeriod().getIndex()).isEmpty()) {
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
            for (ExamPlacement placement: assignment.assignedValues())
                assigned(assignment, placement);
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
}
