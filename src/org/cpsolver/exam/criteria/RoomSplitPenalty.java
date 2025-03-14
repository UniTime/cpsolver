package org.cpsolver.exam.criteria;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Cost for using more than one room (nrSplits^2).
 * <br><br>
 * A weight for room split penalty can be set by problem
 * property Exams.RoomSplitWeight, or in the input xml file, property
 * roomSplitWeight.
 * 
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
public class RoomSplitPenalty extends ExamCriterion {
    
    @Override
    public ValueContext createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new RoomSplitContext(assignment);
    }
    
    @Override
    public String getWeightName() {
        return "Exams.RoomSplitWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "roomSplitWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 10.0;
    }

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        return (value.getRoomPlacements() == null || value.getRoomPlacements().size() <= 1 ? 0 : (value.getRoomPlacements().size() - 1) * (value.getRoomPlacements().size() - 1));
    }

    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0) {
            int[] roomSplits = ((RoomSplitContext)getContext(assignment)).getRoomSplits();
            String split = "";
            for (int i = 0; i < roomSplits.length; i++) {
                if (roomSplits[i] > 0) {
                    if (split.length() > 0)
                        split += ", ";
                    split += roomSplits[i] + "&times;" + (i + 2);
                }
            }
            info.put(getName(), sDoubleFormat.format(getValue(assignment)) + " (" + split + ")");            
        }
    }

    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "RSp:" + sDoubleFormat.format(getValue(assignment));
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
    
    protected class RoomSplitContext extends ValueContext {
        private int iRoomSplits[] = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        
        protected void ensureCapacity(int minCapacity) {
            if (minCapacity > iRoomSplits.length){
                int oldCapacity = iRoomSplits.length;
                int newCapacity = oldCapacity + (oldCapacity / 2);
                if (newCapacity < minCapacity)
                    newCapacity = minCapacity + (minCapacity / 2);
                iRoomSplits = Arrays.copyOf(iRoomSplits, newCapacity);
            }
        }

        public RoomSplitContext(Assignment<Exam, ExamPlacement> assignment) {
            super(assignment);
            for (Exam exam: getModel().variables()) {
                ExamPlacement placement = assignment.getValue(exam);
                if (placement != null && placement.getRoomPlacements() != null && placement.getRoomPlacements().size() > 1) {
                    ensureCapacity(placement.getRoomPlacements().size());
                    iRoomSplits[placement.getRoomPlacements().size() - 2] ++;
                }
            }
        }

        @Override
        public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
            super.assigned(assignment, value);
            if (value.getRoomPlacements() != null && value.getRoomPlacements().size() > 1) {
                ensureCapacity(value.getRoomPlacements().size());
                iRoomSplits[value.getRoomPlacements().size() - 2] ++;
            }
        }

        @Override
        public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
            super.unassigned(assignment, value);
            if (value.getRoomPlacements() != null && value.getRoomPlacements().size() > 1) {
                ensureCapacity(value.getRoomPlacements().size());
                iRoomSplits[value.getRoomPlacements().size() - 2] --;
            }
        }
        
        public int[] getRoomSplits() {
            return iRoomSplits;
        }
    }
}