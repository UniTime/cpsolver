package net.sf.cpsolver.exam.criteria;

import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
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
                penalty += r.getPenalty(value.getPeriod());
            }
        return penalty;
    }
    
    private int getMinPenalty(ExamRoom r) {
        int min = Integer.MAX_VALUE;
        for (ExamPeriod p : ((ExamModel)getModel()).getPeriods()) {
            if (r.isAvailable(p)) {
                min = Math.min(min, r.getPenalty(p));
            }
        }
        return min;
    }

    private int getMaxPenalty(ExamRoom r) {
        int max = Integer.MIN_VALUE;
        for (ExamPeriod p : ((ExamModel)getModel()).getPeriods()) {
            if (r.isAvailable(p)) {
                max = Math.max(max, r.getPenalty(p));
            }
        }
        return max;
    }

    @Override
    protected void computeBounds() {
        iBounds = new double[] { 0.0, 0.0 };
        for (Exam exam : getModel().variables()) {
            if (!exam.getRoomPlacements().isEmpty()) {
                int minPenalty = Integer.MAX_VALUE, maxPenalty = Integer.MIN_VALUE;
                for (ExamRoomPlacement roomPlacement : exam.getRoomPlacements()) {
                    minPenalty = Math.min(minPenalty, 2 * roomPlacement.getPenalty() + getMinPenalty(roomPlacement.getRoom()));
                    maxPenalty = Math.max(maxPenalty, 2 * roomPlacement.getPenalty() + getMaxPenalty(roomPlacement.getRoom()));
                }
                iBounds[0] += minPenalty;
                iBounds[1] += maxPenalty;
            }
        }
    }
    
    @Override
    public String toString() {
        return "RP:" + sDoubleFormat.format(getWeightedValue());
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}
