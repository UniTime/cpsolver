package org.cpsolver.exam.criteria;

import java.util.Map;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoom;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;


/**
 * 
 * Cost for using room(s) that are too big. I.e., a difference between total room size
 * (computed using either {@link ExamRoom#getSize()} or {@link ExamRoom#getAltSize()} based
 * on {@link Exam#hasAltSeating()}) and the number of students {@link Exam#getSize()}.
 * <br><br>
 * A weight for room size penalty can be set by problem
 * property Exams.RoomSizeWeight, or in the input xml file, property
 * roomSizeWeight).
 * <br><br>
 * The difference function can be made polynomial by using Exams.RoomSizeFactor parameter
 * (defaults to 1.0). The value of this criteria is then cubed by the power of this room
 * size factor. This is to be able to favor a room swap between two exams at the same period,
 * in which a smaller exam takes a smaller room. To do this, set Exams.RoomSizeFactor to
 * a number bigger than one that is close to one (e.g., 1.05).
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
public class RoomSizePenalty extends ExamCriterion {
    private double iRoomSizeFactor = 1.0;
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iRoomSizeFactor = properties.getPropertyDouble("Exams.RoomSizeFactor", 1.0);
    }
    
    @Override
    public void setModel(Model<Exam, ExamPlacement> model) {
        super.setModel(model);
        iRoomSizeFactor = ((ExamModel)model).getProperties().getPropertyDouble("Exams.RoomSizeFactor", 1.0);
    }
    
    @Override
    public String getWeightName() {
        return "Exams.RoomSizeWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "roomSizeWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.0001;
    }
    
    @Override
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
        params.put("roomSizeFactor", String.valueOf(iRoomSizeFactor));
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        try {
            setWeight(Double.valueOf(params.get(getXmlWeightName())));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            iRoomSizeFactor = Double.valueOf(params.get("roomSizeFactor"));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        int size = 0;
        if (value.getRoomPlacements() != null)
            for (ExamRoomPlacement r : value.getRoomPlacements()) {
                size += r.getSize(exam.hasAltSeating());
            }
        int diff = size - exam.getSize();
        return (diff < 0 ? 0 : Math.pow(diff, iRoomSizeFactor));
    }
    
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0) {
            info.put(getName(), sDoubleFormat.format(getValue(assignment) / assignment.nrAssignedVariables()));
        }
    }

    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "RSz:" + sDoubleFormat.format(getValue(assignment) / assignment.nrAssignedVariables());
    }

    @Override
    public boolean isPeriodCriterion() { return false; }
}