package net.sf.cpsolver.exam.criteria.additional;

import java.util.Collection;
import java.util.Set;

import net.sf.cpsolver.exam.criteria.ExamCriterion;
import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;

/**
 * Experimental criterion counting violations of room assignments. If this
 * criterion is enabled, any room can be assigned to an exam (not only those that are
 * in the domain of the exam).
 * <br><br>
 * To enable assignment of prohibited rooms, set parameter Exam.SoftRooms to
 * a weight that should be inferred by a prohibited room assignment.
 * 
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2012 Tomas Muller<br>
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
public class RoomViolation extends ExamCriterion {
    
    @Override
    public String getWeightName() {
        return "Exam.SoftRooms";
    }
    
    @Override
    public String getXmlWeightName() {
        return "softRooms";
    }

    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        double penalty = 0.0;
        if (value.getRoomPlacements() != null)
            for (ExamRoomPlacement r : value.getRoomPlacements()) {
                penalty += (getWeight() == r.getPenalty() || getWeight() == r.getRoom().getPenalty(value.getPeriod()) ? 1.0 / value.getRoomPlacements().size() : 0.0);
            }
        return penalty;
    }
    
    @Override
    public double[] getBounds(Collection<Exam> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Exam exam : variables) {
            if (!exam.getRoomPlacements().isEmpty()) {
                rooms: for (ExamRoomPlacement roomPlacement : exam.getRoomPlacements()) {
                    if (getWeight() == roomPlacement.getPenalty() && roomPlacement.getRoom().isAvailable()) {
                        bounds[1] ++; break rooms;
                    }
                }
            }
        }
        return bounds;
    }

    @Override
    public String toString() {
        return (getValue() <= 0.0 ? "" : "!R:" + sDoubleFormat.format(getValue()));
    }
    
    @Override
    public boolean isPeriodCriterion() { return false; }
}
