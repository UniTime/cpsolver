package net.sf.cpsolver.exam.criteria;

import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Room perturbation penalty. I.e., number of assigned rooms different from
 * initial. Only applicable when {@link PerturbationPenalty#isMPP()} is true (minimal
 * perturbation problem).
 * <br><br>
 * A weight of room perturbations (i.e., a penalty for
 * an assignment of an exam to a room different from the initial one) can be
 * set by problem property Exams.RoomPerturbationWeight, or in the input xml
 * file, property roomPerturbationWeight).
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
public class RoomPerturbationPenalty extends PerturbationPenalty {
    
    @Override
    public String getWeightName() {
        return "Exams.RoomPerturbationWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "roomPerturbationWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.01;
    }

    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        if (!isMPP()) return 0;
        Exam exam = value.variable();
        ExamPlacement initial = exam.getInitialAssignment();
        if (initial == null) return 0;
        int penalty = 0;
        if (value.getRoomPlacements() != null)
            for (ExamRoomPlacement rp : value.getRoomPlacements()) {
                if (initial.getRoomPlacements() == null || !initial.getRoomPlacements().contains(rp))
                    penalty++;
            }
        return penalty;
    }


    @Override
    public String toString() {
        return (isMPP() ? "IRP:" + sDoubleFormat.format(getValue()) : "");
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}
