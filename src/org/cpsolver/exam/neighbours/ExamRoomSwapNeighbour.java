package org.cpsolver.exam.neighbours;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;


/**
 * Swap a room between two assigned exams. <br>
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
public class ExamRoomSwapNeighbour implements Neighbour<Exam, ExamPlacement> {
    private double iValue = 0;
    private ExamPlacement iX1, iX2 = null;
    private ExamRoomPlacement iR1, iR2 = null;

    public ExamRoomSwapNeighbour(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement, ExamRoomPlacement current, ExamRoomPlacement swap) {
        if (placement.getRoomPlacements().contains(swap))
            return; // not an actual swap
        if (!checkParents(placement.getRoomPlacements(), current, swap))
            return; // room partition violation
        Exam exam = placement.variable();
        if (!swap.isAvailable(placement.getPeriod()))
            return; // room not available
        if (!exam.checkDistributionConstraints(assignment, swap))
            return; // a distribution constraint is violated
        int size = 0;
        for (ExamRoomPlacement r : placement.getRoomPlacements())
            size += r.getSize(exam.hasAltSeating());
        size -= current.getSize(exam.hasAltSeating());
        size += swap.getSize(exam.hasAltSeating());
        if (size < exam.getSize())
            return; // new room is too small
        ExamPlacement conflict = null;
        Set<ExamPlacement> conf = new HashSet<ExamPlacement>();
        swap.getRoom().computeConflicts(assignment, exam, placement.getPeriod(), conf);
        if (conf.size() > 1) return;
        else if (conf.size() == 1) conflict = conf.iterator().next();
        if (conflict == null) {
            Set<ExamRoomPlacement> newRooms = new HashSet<ExamRoomPlacement>(placement.getRoomPlacements());
            newRooms.remove(current);
            newRooms.add(swap);
            for (Iterator<ExamRoomPlacement> i = newRooms.iterator(); i.hasNext();) {
                ExamRoomPlacement r = i.next();
                if (r.equals(swap))
                    continue;
                if (size - r.getSize(exam.hasAltSeating()) >= exam.getSize()) {
                    i.remove();
                    size -= r.getSize(exam.hasAltSeating());
                }
            }
            iX1 = new ExamPlacement(exam, placement.getPeriodPlacement(), newRooms);
            ExamPlacement p = assignment.getValue(exam);
            iValue = iX1.toDouble(assignment) - (p == null ? 0.0 : p.toDouble(assignment));
        } else {
            Exam x = conflict.variable();
            ExamRoomPlacement xNew = x.getRoomPlacement(current.getRoom());
            if (xNew == null)
                return; // conflicting exam cannot be assigned in the current
                        // room
            if (!conflict.contains(current.getRoom()))
                return; // conflict due to the room partitioning
            if (swap.getRoom().equals(current.getRoom().getParentRoom()) || current.getRoom().equals(swap.getRoom().getParentRoom()))
                return; // conflict due to the room partitioning
            if (!x.checkDistributionConstraints(assignment, xNew))
                return; // conflicting exam has a distribution constraint
                        // violated
            if (!checkParents(placement.getRoomPlacements(), swap, xNew))
                return; // room partition violation
            int xSize = 0;
            for (ExamRoomPlacement r : conflict.getRoomPlacements()) {
                xSize += r.getSize(x.hasAltSeating());
            }
            xSize -= swap.getSize(x.hasAltSeating());
            xSize += current.getSize(x.hasAltSeating());
            if (xSize < x.getSize())
                return; // current room is too small for the conflicting exam
            Set<ExamPlacement> otherConf = new HashSet<ExamPlacement>(); otherConf.add(placement);
            current.getRoom().computeConflicts(assignment, x, placement.getPeriod(), otherConf);
            if (otherConf.size() > 1)
                return; // other placements are conflicting than the current one 
            Set<ExamRoomPlacement> newRooms = new HashSet<ExamRoomPlacement>(placement.getRoomPlacements());
            newRooms.remove(current);
            newRooms.add(swap);
            for (Iterator<ExamRoomPlacement> i = newRooms.iterator(); i.hasNext();) {
                ExamRoomPlacement r = i.next();
                if (r.equals(swap))
                    continue;
                if (size - r.getSize(exam.hasAltSeating()) >= exam.getSize()) {
                    i.remove();
                    size -= r.getSize(exam.hasAltSeating());
                }
            }
            iX1 = new ExamPlacement(exam, placement.getPeriodPlacement(), newRooms);
            Set<ExamRoomPlacement> xRooms = new HashSet<ExamRoomPlacement>(conflict.getRoomPlacements());
            xRooms.remove(x.getRoomPlacement(swap.getRoom()));
            xRooms.add(xNew);
            for (Iterator<ExamRoomPlacement> i = xRooms.iterator(); i.hasNext();) {
                ExamRoomPlacement r = i.next();
                if (r.equals(swap))
                    continue;
                if (xSize - r.getSize(x.hasAltSeating()) >= x.getSize()) {
                    i.remove();
                    xSize -= r.getSize(x.hasAltSeating());
                }
            }
            iX2 = new ExamPlacement(x, conflict.getPeriodPlacement(), xRooms);
            ExamPlacement p = assignment.getValue(exam);
            iValue = iX1.toDouble(assignment) - (p == null ? 0.0 : p.toDouble(assignment)) + iX2.toDouble(assignment) - conflict.toDouble(assignment);
        }
        iR1 = current;
        iR2 = swap;
    }

    public boolean canDo() {
        return iX1 != null;
    }

    @Override
    public void assign(Assignment<Exam, ExamPlacement> assignment, long iteration) {
        if (iX2 == null) {
            assignment.assign(iteration, iX1);
        } else {
            assignment.unassign(iteration, iX1.variable());
            assignment.unassign(iteration, iX2.variable());
            assignment.assign(iteration, iX1);
            assignment.assign(iteration, iX2);
        }
    }

    @Override
    public String toString() {
        if (iX2 == null) {
            return iX1.variable() + " := " + iX1.toString() + " / " + " (value:" + value(null) + ")";
        } else {
            return iX1.variable().getName() + ": " + iR1.getRoom() + " <-> " + iR2.getRoom() + " (w " + iX2.variable().getName() + ", value:" + value(null) + ")";
        }
    }

    protected static String toString(double[] x, double[] y) {
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < x.length; i++) {
            if (i > 0)
                s.append(",");
            s.append(df.format(x[i] - y[i]));
        }
        return "[" + s.toString() + "]";
    }

    @Override
    public double value(Assignment<Exam, ExamPlacement> assignment) {
        return iValue;
    }

    @Override
    public Map<Exam, ExamPlacement> assignments() {
        Map<Exam, ExamPlacement> ret = new HashMap<Exam, ExamPlacement>();
        ret.put(iX1.variable(), iX1);
        if (iX2 != null)
            ret.put(iX2.variable(), iX2);
        return ret;
    }
    
    private static boolean checkParents(Collection<ExamRoomPlacement> rooms, ExamRoomPlacement current, ExamRoomPlacement swap) {
        if (swap.getRoom().getParentRoom() != null) {
            // check if already lists the parent
            for (ExamRoomPlacement r: rooms)
                if (!r.equals(current) && r.getRoom().equals(swap.getRoom().getParentRoom())) return false;
        }
        if (swap.getRoom().getPartitions() != null) {
            // check if has any of the partitions
            for (ExamRoomPlacement r: rooms)
                if (!r.equals(current) && swap.getRoom().getPartitions().contains(r.getRoom())) return false;
        }
        return true;
    }
}
