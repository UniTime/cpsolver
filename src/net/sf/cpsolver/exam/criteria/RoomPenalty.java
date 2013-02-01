package net.sf.cpsolver.exam.criteria;

import java.util.Collection;
import java.util.Set;

import net.sf.cpsolver.exam.criteria.additional.RoomViolation;
import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Room penalty (penalty for using given rooms). I.e., sum of
 * {@link ExamRoomPlacement#getPenalty(ExamPeriod)} of assigned rooms.
 * <br><br>
 * A weight for room penalty can be set by problem property
 * Exams.RoomPreferenceWeight, or in the input xml file, property
 * roomWeight).
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
public class RoomPenalty extends ExamCriterion {
    protected Integer iSoftRooms = null;
    
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        if (super.init(solver)) {
            iSoftRooms = solver.getProperties().getPropertyInteger("Exam.SoftRooms", null);
            if (iSoftRooms != null) {
                RoomViolation rv = new RoomViolation();
                getModel().addCriterion(rv);
                return rv.init(solver);
            }
        }
        return true;
    }
    
    @Override
    public String getWeightName() {
        return "Exams.RoomWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "roomWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.1;
    }
    
    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        double penalty = 0.0;
        if (value.getRoomPlacements() != null)
            for (ExamRoomPlacement r : value.getRoomPlacements()) {
                penalty += (iSoftRooms != null && (iSoftRooms == r.getPenalty() || iSoftRooms == r.getPenalty(value.getPeriod())) ? 0.0 : r.getPenalty(value.getPeriod()));
            }
        return penalty;
    }
    
    private int getMinPenalty(ExamRoom r) {
        int min = Integer.MAX_VALUE;
        for (ExamPeriod p : ((ExamModel)getModel()).getPeriods()) {
            if (r.isAvailable(p) && (iSoftRooms == null || r.getPenalty(p) != iSoftRooms)) {
                min = Math.min(min, r.getPenalty(p));
            }
        }
        return min;
    }

    private int getMaxPenalty(ExamRoom r) {
        int max = Integer.MIN_VALUE;
        for (ExamPeriod p : ((ExamModel)getModel()).getPeriods()) {
            if (r.isAvailable(p) && (iSoftRooms == null || r.getPenalty(p) != iSoftRooms)) {
                max = Math.max(max, r.getPenalty(p));
            }
        }
        return max;
    }
    
    private boolean isAvailable(ExamRoom r) {
        for (ExamPeriod p : ((ExamModel)getModel()).getPeriods()) {
            if (r.isAvailable(p) && (iSoftRooms == null || r.getPenalty(p) != iSoftRooms))
                return true;
        }
        return false;
    }

    @Override
    public double[] getBounds(Collection<Exam> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Exam exam : variables) {
            if (!exam.getRoomPlacements().isEmpty()) {
                int minPenalty = Integer.MAX_VALUE, maxPenalty = Integer.MIN_VALUE;
                for (ExamRoomPlacement roomPlacement : exam.getRoomPlacements()) {
                    if (iSoftRooms != null && iSoftRooms == roomPlacement.getPenalty()) continue;
                    if (!isAvailable(roomPlacement.getRoom())) continue;
                    minPenalty = Math.min(minPenalty, 2 * roomPlacement.getPenalty() + getMinPenalty(roomPlacement.getRoom()));
                    maxPenalty = Math.max(maxPenalty, 2 * roomPlacement.getPenalty() + getMaxPenalty(roomPlacement.getRoom()));
                }
                bounds[0] += minPenalty;
                bounds[1] += maxPenalty;
            }
        }
        return bounds;
    }
    
    @Override
    public String toString() {
        return "RP:" + sDoubleFormat.format(getValue());
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}
