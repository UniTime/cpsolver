package org.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Room split distance penalty. I.e., average distance between two rooms of a placement.
 * <br><br>
 * A weight for room split penalty can be set by problem
 * property Exams.RoomSplitWeight, or in the input xml file, property
 * roomSplitDistanceWeight).
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
public class RoomSplitDistancePenalty  extends ExamCriterion {

    @Override
    public ValueContext createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new RoomSplitContext(assignment);
    }
    
    @Override
    public String getWeightName() {
        return "Exams.RoomSplitDistanceWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "roomSplitDistanceWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.01;
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        if (value.getRoomPlacements() == null || value.getRoomPlacements().size() <= 1) return 0.0;
        double distance = 0.0;
        for (ExamRoomPlacement r : value.getRoomPlacements()) {
            for (ExamRoomPlacement w : value.getRoomPlacements()) {
                if (r.getRoom().getId() < w.getRoom().getId())
                    distance += r.getRoom().getDistanceInMeters(w.getRoom());
            }
        }
        int pairs = value.getRoomPlacements().size() * (value.getRoomPlacements().size() - 1) / 2;
        return distance / pairs;
    }
    
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0) {
            info.put(getName(), sDoubleFormat.format(getValue(assignment) / nrRoomSplits(assignment)) + " m");
        }
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "RSd:" + sDoubleFormat.format(getValue(assignment) / nrRoomSplits(assignment));
    }
    
    public int nrRoomSplits(Assignment<Exam, ExamPlacement> assignment) {
        return ((RoomSplitContext)getContext(assignment)).nrRoomSplits();
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
    
    protected class RoomSplitContext extends ValueContext {
        private int iRoomSplits = 0;
        
        public RoomSplitContext(Assignment<Exam, ExamPlacement> assignment) {
            super(assignment);
            for (Exam exam: getModel().variables()) {
                ExamPlacement placement = assignment.getValue(exam);
                if (placement != null && placement.getRoomPlacements() != null && placement.getRoomPlacements().size() > 1)
                    iRoomSplits ++;
            }
        }

        @Override
        public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
            super.assigned(assignment, value);
            if (value.getRoomPlacements() != null && value.getRoomPlacements().size() > 1)
                iRoomSplits ++;
        }

        @Override
        public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
            super.unassigned(assignment, value);
            if (value.getRoomPlacements() != null && value.getRoomPlacements().size() > 1)
                iRoomSplits --;
        }
        
        public int nrRoomSplits() {
            return iRoomSplits;
        }
    }
}
