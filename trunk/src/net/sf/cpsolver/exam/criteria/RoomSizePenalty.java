package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * 
 * Cost for using room(s) that are too big. I.e., a difference between total room size
 * (computed using either {@link ExamRoom#getSize()} or {@link ExamRoom#getAltSize()} based
 * on {@link Exam#hasAltSeating()}) and the number of students {@link Exam#getSize()}.
 * <br><br>
 * A weight for room size penalty can be set by problem
 * property Exams.RoomSizeWeight, or in the input xml file, property
 * roomSizeWeight).
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
public class RoomSizePenalty extends ExamCriterion {
    
    @Override
    public String getWeightName() {
        return "Exams.RoomSizeWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.0001;
    }

    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int size = 0;
        if (value.getRoomPlacements() != null)
            for (ExamRoomPlacement r : value.getRoomPlacements()) {
                size += r.getSize(exam.hasAltSeating());
            }
        int diff = size - exam.getSize();
        return (diff < 0 ? 0 : diff);
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        if (getValue() != 0.0) {
            info.put(getName(), sDoubleFormat.format(getValue() / getModel().nrAssignedVariables()));
        }
    }

    @Override
    public String toString() {
        return "RSz:" + sDoubleFormat.format(getWeightedValue());
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}