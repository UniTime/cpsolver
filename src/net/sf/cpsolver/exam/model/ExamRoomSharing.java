package net.sf.cpsolver.exam.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.dom4j.Element;

import net.sf.cpsolver.coursett.IdConvertor;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Abstract room sharing model. Defines when and under what conditions two or more exams can share a room.<br>
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
public abstract class ExamRoomSharing  {
    
    public ExamRoomSharing(Model<Exam, ExamPlacement> model, DataProperties config) {}
        
    /**
     * True if given examination can not be placed in the same room at the same period as the other examinations
     * @param exam examination placement in question
     * @param other exams currently assigned in the room at the requested period
     * @param room examination room in questions
     * @return true if there is a conflict
     */
    public boolean inConflict(ExamPlacement exam, Collection<ExamPlacement> other, ExamRoom room) {
        if (exam.getRoomPlacements().size() != 1)
            return !other.isEmpty();
        
        return inConflict(exam.variable(), other, room);
    }
    
    /**
     * True if given examination can not be placed in the same room at the same period as the other examinations
     * @param exam examination in question
     * @param other exams currently assigned in the room at the requested period
     * @param room examination room in questions
     * @return true if there is a conflict
     */
    public boolean inConflict(Exam exam, Collection<ExamPlacement> other, ExamRoom room) {
        int total = exam.getSize();
        boolean altSeating = exam.hasAltSeating();
        for (ExamPlacement x: other) {
            if (x.variable().equals(exam)) continue;
            if (x.getRoomPlacements().size() != 1) return true; // already split into multiple rooms
            if (!canShareRoom(exam, x.variable())) return true; // sharing not allowed between the pair
            total += x.variable().getSize();
            if (x.variable().hasAltSeating()) altSeating = true;
        }
        
        return total > (altSeating ? room.getAltSize() : room.getSize()); // check size limit
    }
    
    /**
     * Compute conflicting placement for the case when a given examination needs to be placed in the same room at the same period as the other examinations
     * @param exam examination placement in question
     * @param other exams currently assigned in the room at the requested period
     * @param room examination room in questions
     */
    public void computeConflicts(ExamPlacement exam, Collection<ExamPlacement> other, ExamRoom room, Set<ExamPlacement> conflicts) {
        // more than one room is required -> no sharing
        if (exam.getRoomPlacements().size() != 1) {
            conflicts.addAll(other);
            return;
        }
        
        computeConflicts(exam.variable(), other, room, conflicts);
    }
    
    /**
     * Compute conflicting placement for the case when a given examination needs to be placed in the same room at the same period as the other examinations
     * @param exam examination in question
     * @param other exams currently assigned in the room at the requested period
     * @param room examination room in questions
     */
    public void computeConflicts(Exam exam, Collection<ExamPlacement> other, ExamRoom room, Set<ExamPlacement> conflicts) {
        int total = exam.getSize();
        boolean altSeating = exam.hasAltSeating();
        List<ExamPlacement> adepts = new ArrayList<ExamPlacement>();
        for (ExamPlacement x: other) {
            if (x.variable().equals(exam)) continue;
            // already split into multiple rooms
            if (x.getRoomPlacements().size() != 1) {
                conflicts.add(x); continue;
            }
            // sharing not allowed between the pair
            if (!canShareRoom(exam, x.variable())) {
                conflicts.add(x); continue;
            }
            if (x.variable().hasAltSeating()) altSeating = true;
            total += x.variable().getSize();
            adepts.add(x);
        }
        
        // fix the total size if needed
        while (total > (altSeating ? room.getAltSize() : room.getSize()) && !adepts.isEmpty()) {
            ExamPlacement x = ToolBox.random(adepts);
            adepts.remove(x);
            conflicts.add(x);
            total -= x.variable().getSize();
        }
    }

    
    /**
     * True if given two exams can share a room
     */
    public abstract boolean canShareRoom(Exam x1, Exam x2);
    
    /**
     * Save sharing information (if needed) for a given exam
     */
    public void save(Exam exam, Element element, IdConvertor idConvertor) {}
    
    /**
     * Load sharing information (if needed) for a given exam
     */
    public void load(Exam exam, Element element) {}
}
