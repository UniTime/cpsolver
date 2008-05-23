package net.sf.cpsolver.exam.neighbours;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.model.Neighbour;
/**
 * Swap a room between two assigned exams.
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ExamRoomSwapNeighbour extends Neighbour {
    private static Logger sLog = Logger.getLogger(ExamRoomSwapNeighbour.class);
    private double iValue = 0;
    private ExamPlacement iX1, iX2 = null;
    private ExamRoomPlacement iR1, iR2 = null;
    
    public ExamRoomSwapNeighbour(ExamPlacement placement, ExamRoomPlacement current, ExamRoomPlacement swap) {
        if (placement.getRoomPlacements().contains(swap)) return; //not an actual swap
        Exam exam = (Exam)placement.variable();
        if (!swap.isAvailable(placement.getPeriod())) return; //room not available
        if (!exam.checkDistributionConstraints(swap)) return; //a distribution constraint is violated
        int size = 0;
        for (Iterator i=placement.getRoomPlacements().iterator();i.hasNext();)
            size += ((ExamRoomPlacement)i.next()).getSize(exam.hasAltSeating()); 
        size -= current.getSize(exam.hasAltSeating());
        size += swap.getSize(exam.hasAltSeating());
        if (size<exam.getSize()) return; //new room is too small
        ExamPlacement conflict = swap.getRoom().getPlacement(placement.getPeriod());
        if (conflict==null) {
            Set newRooms = new HashSet(placement.getRoomPlacements());
            newRooms.remove(current);
            newRooms.add(swap);
            iX1 = new ExamPlacement(exam, placement.getPeriodPlacement(), newRooms);
            iValue = iX1.toDouble() - (exam.getAssignment()==null?0.0:exam.getAssignment().toDouble());
        } else {
            Exam x = (Exam)conflict.variable();
            ExamRoomPlacement xNew = x.getRoomPlacement(current.getRoom());
            if (xNew==null) return; //conflicting exam cannot be assigned in the current room
            if (!x.checkDistributionConstraints(xNew)) return; //conflicting exam has a distribution constraint violated
            int xSize = 0;
            for (Iterator i=conflict.getRoomPlacements().iterator();i.hasNext();) {
                xSize += ((ExamRoomPlacement)i.next()).getSize(x.hasAltSeating());
            }
            if (xSize - swap.getSize(x.hasAltSeating()) + current.getSize(x.hasAltSeating()) < x.getSize()) return; //current room is too small for the conflicting exam
            Set newRooms = new HashSet(placement.getRoomPlacements());
            newRooms.remove(current);
            newRooms.add(swap);
            iX1 = new ExamPlacement(exam, placement.getPeriodPlacement(), newRooms);
            Set xRooms = new HashSet(conflict.getRoomPlacements());
            xRooms.remove(x.getRoomPlacement(swap.getRoom()));
            xRooms.add(xNew);
            iX2 = new ExamPlacement(x, conflict.getPeriodPlacement(), xRooms);
            iValue = iX1.toDouble() - (exam.getAssignment()==null?0.0:exam.getAssignment().toDouble()) +
                     iX2.toDouble() - conflict.toDouble();
        }
        iR1 = current; iR2 = swap;
    }
    
    public boolean canDo() {
        return iX1!=null;
    }
    
    public void assign(long iteration) {
        if (iX2==null) {
            iX1.variable().assign(iteration, iX1);
        } else {
            if (iX1.variable().getAssignment()!=null) iX1.variable().unassign(iteration);
            iX2.variable().unassign(iteration);
            iX1.variable().assign(iteration, iX1);
            iX2.variable().assign(iteration, iX2);
        }
    }
    
    public String toString() {
        if (iX2==null) {
            return iX1.variable().getAssignment()+ " -> "+ iX1.toString()+" / "+" (value:"+value()+")";
        } else {
            return iX1.variable().getName()+": "+iR1.getRoom()+" <-> "+iR2.getRoom()+" (w "+iX2.variable().getName()+", value:"+value()+")";
        }
    }
    
    protected static String toString(double[] x, double[] y) {
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuffer s = new StringBuffer();
        for (int i=0;i<x.length;i++) {
            if (i>0) s.append(",");
            s.append(df.format(x[i]-y[i]));
        }
        return "["+s.toString()+"]";
    }
    
    public double value() {
        return iValue;
    }
}
