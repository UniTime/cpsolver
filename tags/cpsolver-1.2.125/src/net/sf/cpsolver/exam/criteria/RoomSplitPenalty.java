package net.sf.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Cost for using more than one room (nrSplits^2).
 * <br><br>
 * A weight for room split penalty can be set by problem
 * property Exams.RoomSplitWeight, or in the input xml file, property
 * roomSplitWeight.
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
public class RoomSplitPenalty extends ExamCriterion {
    private int iRoomSplits[] = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    
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
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        return (value.getRoomPlacements() == null || value.getRoomPlacements().size() <= 1 ? 0 : (value.getRoomPlacements().size() - 1) * (value.getRoomPlacements().size() - 1));
    }

    @Override
    public void beforeUnassigned(long iteration, ExamPlacement value) {
        super.beforeUnassigned(iteration, value);
        if (value.getRoomPlacements() == null || value.getRoomPlacements().size() > 1)
            iRoomSplits[value.getRoomPlacements().size() - 2] --;
    }
    
    @Override
    public void afterAssigned(long iteration, ExamPlacement value) {
        super.afterAssigned(iteration, value);
        if (value.getRoomPlacements() == null || value.getRoomPlacements().size() > 1)
            iRoomSplits[value.getRoomPlacements().size() - 2] ++;
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        if (getValue() != 0.0) {
            String split = "";
            for (int i = 0; i < iRoomSplits.length; i++) {
                if (iRoomSplits[i] > 0) {
                    if (split.length() > 0)
                        split += ", ";
                    split += iRoomSplits[i] + "&times;" + (i + 2);
                }
            }
            info.put(getName(), sDoubleFormat.format(getValue()) + " (" + split + ")");            
        }
    }

    @Override
    public String toString() {
        return "RSp:" + sDoubleFormat.format(getValue());
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}