package net.sf.cpsolver.exam.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.criteria.AbstractCriterion;

/**
 * Abstract examination criterion. All examination criteria are inherited from this criterion.
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
public abstract class ExamCriterion extends AbstractCriterion<Exam, ExamPlacement> {
    
    public ExamCriterion() {
        super();
    }
    
    public void setWeight(double weight) { iWeight = weight; }
    
    @Override
    public String getWeightName() {
        return "Exams." + getClass().getName().substring(1 + getClass().getName().lastIndexOf('.')) + "Weight";
    }
    
    @Override
    public double[] getBounds(Collection<Exam> exams) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Exam exam: exams) {
            Double min = null, max = null;
            for (ExamPeriodPlacement period: exam.getPeriodPlacements()) {
                if (exam.getMaxRooms() == 0) {
                    double value = getValue(new ExamPlacement(exam, period, null), null);
                    if (min == null) { min = value; max = value; continue; }
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                } else {
                    for (ExamRoomPlacement room: exam.getRoomPlacements()) {
                        Set<ExamRoomPlacement> rooms = new HashSet<ExamRoomPlacement>();
                        rooms.add(room);
                        double value = getValue(new ExamPlacement(exam, period, rooms), null);
                        if (min == null) { min = value; max = value; continue; }
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                    }
                }
            }
            if (min != null) {
                bounds[0] += min;
                bounds[1] += max;
            }
        }
        return bounds;
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        double val = getValue();
        double[] bounds = getBounds();
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
            info.put(getName(), getPerc(val, bounds[0], bounds[1]) + "% (" + sDoubleFormat.format(val) + ")");
        else if (bounds[1] <= val && val <= bounds[0] && bounds[1] < bounds[0])
            info.put(getName(), getPercRev(val, bounds[1], bounds[0]) + "% (" + sDoubleFormat.format(val) + ")");
        else if (bounds[0] != val || val != bounds[1])
            info.put(getName(), sDoubleFormat.format(val));
    }
    
    /**
     * True if this criterion is based on period assignment. Used by {@link ExamPlacement#getTimeCost()}.
     **/
    public boolean isPeriodCriterion() { return true; }
    
    /**
     * Return impact of this criterion on period assignment (if this criterion is based on period assignment). Used by {@link ExamPlacement#getTimeCost()}.
     */
    public double getPeriodValue(ExamPlacement value) { return isPeriodCriterion() ? getValue(value, null) : 0.0; }
     
    /**
     * True if this criterion is based on room assignment. Used by {@link ExamPlacement#getRoomCost()}.
     **/
    public boolean isRoomCriterion() { return !isPeriodCriterion(); }
    
    /**
     * Return impact of this criterion on room assignment (if this criterion is based on room assignment). Used by {@link ExamPlacement#getRoomCost()}.
     */
    public double getRoomValue(ExamPlacement value) { return isRoomCriterion() ? getValue(value, null) : 0.0; }
    
    /**
     * Name of the weight parameter in the parameters section of the examination XML file.
     */
    public String getXmlWeightName() {
        String name = getClass().getName().substring(1 + getClass().getName().lastIndexOf('.'));
        return Character.toString(name.charAt(0)) + name.substring(1);
    }
    
    /**
     * Put all the parameters of this criterion into a map that is used to write parameters section of the examination XML file.
     */
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
    }
    
    /**
     * Set all the parameters of this criterion from a map that is read from the parameters section the examination XML file.
     */
    public void setXmlParameters(Map<String, String> params) {
        try {
            setWeight(Double.valueOf(params.get(getXmlWeightName())));
        } catch (NumberFormatException e) {
        } catch (NullPointerException e) {}
    }
}
