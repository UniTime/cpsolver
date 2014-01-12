package net.sf.cpsolver.exam.criteria.additional;

import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.criteria.ExamCriterion;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Experimental criterion measuring average distance (in meters) to the
 * strongly preferred room (or rooms) of the examination. The idea is to
 * prefer rooms that are close to the strongly preference room (if there is
 * a strongly preferred room but it is not available).
 * <br><br>
 * A weight of the average distance between the assigned room(s) and the 
 * strongly preferred room or rooms can be set using
 * Exams.DistanceToStronglyPreferredRoomWeight property. 
 * <br><br>
 * To enable this criterion add this class name to Exams.AdditionalCriteria
 * parameter. For instance:<br>
 * Exams.AdditionalCriteria=net.sf.cpsolver.exam.criteria.additional.DistanceToStronglyPreferredRoom
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
public class DistanceToStronglyPreferredRoom extends ExamCriterion {
    
    @Override
    public String getWeightName() {
        return "Exams.DistanceToStronglyPreferredRoomWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "distanceToStronglyPreferredRoomWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.001;
    }
    
    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        Average ret = new Average();
        for (ExamRoomPlacement assigned: value.getRoomPlacements()) {
            for (ExamRoomPlacement preferred: value.variable().getRoomPlacements()) {
                if (preferred.getPenalty() < -2) // strongly preferred
                    ret.add(assigned.getDistanceInMeters(preferred));
            }
        }
        return ret.average();
    }
    
    @Override
    public String toString() {
        return "@D:" + sDoubleFormat.format(getValue() / getModel().assignedVariables().size());
    }
    
    @Override
    public double[] getBounds() {
        return new double[] { 0.0, 0.0 };
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        if (getValue() > 0.0)
            info.put(getName(), sDoubleFormat.format(getValue() / getModel().assignedVariables().size()) + " m");
    }
        
    private static class Average {
        double iValue = 0.0;
        int iCount = 0;
        
        private void add(double value) {
            iValue += value; iCount ++;
        }
        
        public double average() {
            return (iCount == 0 ? 0.0 : iValue / iCount);
        }
    }

}
