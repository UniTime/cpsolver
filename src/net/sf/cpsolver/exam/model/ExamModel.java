package net.sf.cpsolver.exam.model;

import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Examination timetabling model. Exams {@link Exam} are modeled as
 * variables, rooms {@link ExamRoom} and students {@link ExamStudent}
 * as constraints. Assignment of an exam to time (modeled as non-overlapping 
 * periods {@link ExamPeriod}) and space (set of rooms) is modeled 
 * using values {@link ExamPlacement}.
 * <br><br>
 * Rooms are grouped into room groups {@link ExamRoomGroup} where
 * an exam can only be assigned into rooms from room groups 
 * that are associated with the exam.  
 * <br><br>
 * The objective function consists of the following criteria:
 * <ul>
 *  <li>Direct student conflicts (a student is enrolled in two exams that are 
 *  scheduled at the same period, weighted by Exams.DirectConflictWeight)
 *  <li>Back-to-Back student conflicts (a student is enrolled in two exams that
 *  are scheduled in consecutive periods, weighted by Exams.BackToBackConflictWeight).
 *  If Exams.IsDayBreakBackToBack is false, there is no conflict between the last 
 *  period and the first period of consecutive days. 
 *  <li>Distance Back-to-Back student conflicts (same as Back-to-Back student conflict,
 *  but the maximum distance between rooms in which both exam take place
 *  is greater than Exams.BackToBackDistance, weighted by Exams.DistanceBackToBackConflictWeight).
 *  <li>More than two exams a day (a student is enrolled in three exams that are
 *  scheduled at the same day, weighted by Exams.MoreThanTwoADayWeight).
 *  <li>Period penalty (total of period penalties {@link Exam#getPeriodPenalty(ExamPeriod, Set)} of all exams,
 *  weighted by Exams.DirectConflictWeight).
 *  <li>Room size penalty (total of room size penalties {@link Exam#getRoomSizePenalty(Set)} of all exams,
 *  weighted by Exams.RoomSizeWeight).
 *  <li>Room split penalty (total of room split penalties {@link Exam#getRoomSplitPenalty(Set)}
 *  of all exams, weighted by Exams.RoomSplitWeight).
 *  <li>Non-original room penalty (total of non-original room penalties {@link Exam#getNotOriginalRoomPenalty(Set)}
 *  of all exams, weighted by Exams.NotOriginalRoomWeight).
 * </ul>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ExamModel extends Model {
    private static Logger sLog = Logger.getLogger(ExamModel.class); 
    private DataProperties iProperties = null;
    private int iMaxRooms = 4;
    private Vector iPeriods = new Vector();
    private Vector iRooms = new Vector();
    private Vector iStudents = new Vector();
    private Vector iDistributionConstraints = new Vector();
    private Vector iInstructors = new Vector();

    
    private boolean iDayBreakBackToBack = true;
    private double iDirectConflictWeight = 1000.0;
    private double iMoreThanTwoADayWeight = 100.0;
    private double iBackToBackConflictWeight = 10.0;
    private double iDistanceBackToBackConflictWeight = 25.0;
    private double iPeriodWeight = 1.0;
    private double iExamRotationWeight = 0.001;
    private double iRoomSizeWeight = 0.001;
    private double iRoomSplitWeight = 10.0;
    private double iNotOriginalRoomWeight = 1.0;
    private int iBackToBackDistance = 67;
    private int iPeriodProhibitedWeight = 99;

    private int iNrDirectConflicts = 0;
    private int iNrBackToBackConflicts = 0;
    private int iNrDistanceBackToBackConflicts = 0;
    private int iNrMoreThanTwoADayConflicts = 0;
    private int iRoomSizePenalty = 0;
    private int iRoomSplitPenalty = 0;
    private int iPeriodPenalty = 0;
    private int iExamRotationPenalty = 0;
    private int iNotOriginalRoomPenalty = 0;
    
    private Vector iRoomGroups = new Vector();
    
    /**
     * Constructor
     * @param properties problem properties
     */
    public ExamModel(DataProperties properties) {
        iAssignedVariables = null;
        iUnassignedVariables = null;
        iPerturbVariables = null;
        iProperties = properties;
        iMaxRooms = properties.getPropertyInt("Exams.MaxRooms", iMaxRooms);
        iDayBreakBackToBack = properties.getPropertyBoolean("Exams.IsDayBreakBackToBack", iDayBreakBackToBack);
        iDirectConflictWeight = properties.getPropertyDouble("Exams.DirectConflictWeight", iDirectConflictWeight);
        iBackToBackConflictWeight = properties.getPropertyDouble("Exams.BackToBackConflictWeight", iBackToBackConflictWeight);
        iDistanceBackToBackConflictWeight = properties.getPropertyDouble("Exams.DistanceBackToBackConflictWeight", iDistanceBackToBackConflictWeight);
        iMoreThanTwoADayWeight = properties.getPropertyDouble("Exams.MoreThanTwoADayWeight", iMoreThanTwoADayWeight);
        iPeriodWeight = properties.getPropertyDouble("Exams.PeriodWeight", iPeriodWeight);
        iExamRotationWeight = properties.getPropertyDouble("Exams.RotationWeight", iExamRotationWeight);
        iRoomSizeWeight = properties.getPropertyDouble("Exams.RoomSizeWeight", iRoomSizeWeight);
        iRoomSplitWeight = properties.getPropertyDouble("Exams.RoomSplitWeight", iRoomSplitWeight);
        iNotOriginalRoomWeight = properties.getPropertyDouble("Exams.NotOriginalRoomWeight", iNotOriginalRoomWeight);
        iBackToBackDistance = properties.getPropertyInt("Exams.BackToBackDistance", iBackToBackDistance);
        iPeriodProhibitedWeight = properties.getPropertyInt("Exams.PeriodProhibitedWeight", iPeriodProhibitedWeight);
    }
    
    /**
     * Initialization of the model
     */
    public void init() {
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            for (Enumeration f=exam.getRooms().elements();f.hasMoreElements();) {
                ExamRoom room = (ExamRoom)f.nextElement();
                room.addVariable(exam);
            }
        }
    }
    
    /**
     * Default maximum number of rooms (can be set by problem property Exams.MaxRooms, 
     * or in the input xml file, property maxRooms)
     */
    public int getMaxRooms() {
        return iMaxRooms;
    }

    /**
     * Default maximum number of rooms (can be set by problem property Exams.MaxRooms, 
     * or in the input xml file, property maxRooms)
     */
    public void setMaxRooms(int maxRooms) {
        iMaxRooms = maxRooms;
    }
    
    /**
     * Add a period
     * @param day day (e.g., 07/12/10)
     * @param time (e.g., 8:00am-10:00am)
     * @param length length of period in minutes
     * @param weight penalization of using this period
     */
    public ExamPeriod addPeriod(Long id, String day, String time, int length, int weight) {
        ExamPeriod lastPeriod = (iPeriods.isEmpty()?null:(ExamPeriod)iPeriods.lastElement());
        ExamPeriod p = new ExamPeriod(id, day, time, length, weight);
        if (lastPeriod==null)
            p.setIndex(iPeriods.size(),0,0);
        else if (lastPeriod.getDayStr().equals(day)) {
            p.setIndex(iPeriods.size(), lastPeriod.getDay(), lastPeriod.getTime()+1);
        } else
            p.setIndex(iPeriods.size(), lastPeriod.getDay()+1, 0);
        if (lastPeriod!=null) {
            lastPeriod.setNext(p);
            p.setPrev(lastPeriod);
        }
        iPeriods.add(p);
        return p;
    }
    
    /**
     * Number of days
     */
    public int getNrDays() {
        return ((ExamPeriod)iPeriods.lastElement()).getDay()+1;
    }
    /**
     * Number of periods
     */
    public int getNrPeriods() {
        return iPeriods.size();
    }
    /**
     * List of periods, use {@link ExamModel#addPeriod(Long, String, String, int, int)} to add a period
     * @return list of {@link ExamPeriod}
     */
    public Vector getPeriods() {
        return iPeriods;
    }
    
    /** Period of given id */
    public ExamPeriod getPeriod(Long id) {
        for (Enumeration e=iPeriods.elements();e.hasMoreElements();) {
            ExamPeriod period=(ExamPeriod)e.nextElement();
            if (period.getId().equals(id)) return period;
        }
        return null;
    }
    
    /**
     * Direct student conflict weight (can be set by problem property Exams.DirectConflictWeight, 
     * or in the input xml file, property directConflictWeight)
     */
    public double getDirectConflictWeight() {
        return iDirectConflictWeight;
    }
    /**
     * Direct student conflict weight (can be set by problem property Exams.DirectConflictWeight, 
     * or in the input xml file, property directConflictWeight)
     */
    public void setDirectConflictWeight(double directConflictWeight) {
        iDirectConflictWeight = directConflictWeight;
    }
    /**
     * Back-to-back student conflict weight (can be set by problem property Exams.BackToBackConflictWeight, 
     * or in the input xml file, property backToBackConflictWeight)
     */
    public double getBackToBackConflictWeight() {
        return iBackToBackConflictWeight;
    }
    /**
     * Back-to-back student conflict weight (can be set by problem property Exams.BackToBackConflictWeight, 
     * or in the input xml file, property backToBackConflictWeight)
     */
    public void setBackToBackConflictWeight(double backToBackConflictWeight) {
        iBackToBackConflictWeight = backToBackConflictWeight;
    }
    /**
     * Distance back-to-back student conflict weight (can be set by problem property Exams.DistanceBackToBackConflictWeight, 
     * or in the input xml file, property distanceBackToBackConflictWeight)
     */
    public double getDistanceBackToBackConflictWeight() {
        return iDistanceBackToBackConflictWeight;
    }
    /**
     * Distance back-to-back student conflict weight (can be set by problem property Exams.DistanceBackToBackConflictWeight, 
     * or in the input xml file, property distanceBackToBackConflictWeight)
     */
    public void setDistanceBackToBackConflictWeight(double distanceBackToBackConflictWeight) {
        iDistanceBackToBackConflictWeight = distanceBackToBackConflictWeight;
    }
    /**
     * More than two exams a day student conflict weight (can be set by problem 
     * property Exams.MoreThanTwoADayWeight, or in the input xml file, property moreThanTwoADayWeight)
     */
    public double getMoreThanTwoADayWeight() {
        return iMoreThanTwoADayWeight;
    }
    /**
     * More than two exams a day student conflict weight (can be set by problem 
     * property Exams.MoreThanTwoADayWeight, or in the input xml file, property moreThanTwoADayWeight)
     */
    public void setMoreThanTwoADayWeight(double moreThanTwoADayWeight) {
        iMoreThanTwoADayWeight = moreThanTwoADayWeight;
    }
    /**
     * True when back-to-back student conflict is to be encountered when a student
     * is enrolled into an exam that is on the last period of one day and another
     * exam that is on the first period of the consecutive day. It can be set by
     * problem property Exams.IsDayBreakBackToBack, or in the input xml file,
     * property isDayBreakBackToBack)
     * 
     */
    public boolean isDayBreakBackToBack() {
        return iDayBreakBackToBack;
    }
    /**
     * True when back-to-back student conflict is to be encountered when a student
     * is enrolled into an exam that is on the last period of one day and another
     * exam that is on the first period of the consecutive day. It can be set by
     * problem property Exams.IsDayBreakBackToBack, or in the input xml file,
     * property isDayBreakBackToBack)
     * 
     */
    public void setDayBreakBackToBack(boolean dayBreakBackToBack) {
        iDayBreakBackToBack = dayBreakBackToBack;
    }
    /**
     * A weight for period penalty (used in {@link Exam#getPeriodPenalty(ExamPeriod, Set)}, 
     * can be set by problem property Exams.PeriodWeight, or in the input xml file,
     * property periodWeight)
     * 
     */
    public double getPeriodWeight() {
        return iPeriodWeight;
    }
    /**
     * A weight for period penalty (used in {@link Exam#getPeriodPenalty(ExamPeriod, Set)}, 
     * can be set by problem property Exams.PeriodWeight, or in the input xml file,
     * property periodWeight)
     * 
     */
    public void setPeriodWeight(double periodWeight) {
        iPeriodWeight = periodWeight;
    }
    /**
     * A weight for exam rotation penalty (used in {@link Exam#getRotationPenalty(ExamPeriod)} 
     * can be set by problem property Exams.RotationWeight, or in the input xml file,
     * property examRotationWeight)
     * 
     */
    public double getExamRotationWeight() {
        return iExamRotationWeight;
    }
    /**
     * A weight for period penalty (used in {@link Exam#getRotationPenalty(ExamPeriod)}, 
     * can be set by problem property Exams.RotationWeight, or in the input xml file,
     * property examRotationWeight)
     * 
     */
    public void setExamRotationWeight(double examRotationWeight) {
        iExamRotationWeight = examRotationWeight;
    }
    /**
     * A weight for room size penalty (used in {@link Exam#getRoomSizePenalty(Set)}, 
     * can be set by problem property Exams.RoomSizeWeight, or in the input xml file,
     * property roomSizeWeight)
     * 
     */
    public double getRoomSizeWeight() {
        return iRoomSizeWeight;
    }
    /**
     * A weight for room size penalty (used in {@link Exam#getRoomSizePenalty(Set)}, 
     * can be set by problem property Exams.RoomSizeWeight, or in the input xml file,
     * property roomSizeWeight)
     * 
     */
    public void setRoomSizeWeight(double roomSizeWeight) {
        iRoomSizeWeight = roomSizeWeight;
    }
    /**
     * A weight for room split penalty (used in {@link Exam#getRoomSplitPenalty(Set)}, 
     * can be set by problem property Exams.RoomSplitWeight, or in the input xml file,
     * property roomSplitWeight)
     * 
     */
    public double getRoomSplitWeight() {
        return iRoomSplitWeight;
    }
    /**
     * A weight for room split penalty (used in {@link Exam#getRoomSplitPenalty(Set)}, 
     * can be set by problem property Exams.RoomSplitWeight, or in the input xml file,
     * property roomSplitWeight)
     * 
     */
    public void setRoomSplitWeight(double roomSplitWeight) {
        iRoomSplitWeight = roomSplitWeight;
    }

    /**
     * A weight for using room that is not original (used in {@link Exam#getNotOriginalRoomPenalty(Set)}, 
     * can be set by problem property Exams.NotOriginalRoomPenalty, or in the input xml file,
     * property notOriginalRoomPenalty)
     * 
     */
    public double getNotOriginalRoomWeight() {
        return iNotOriginalRoomWeight;
    }
    /**
     * A weight for using room that is not original (used in {@link Exam#getNotOriginalRoomPenalty(Set)}, 
     * can be set by problem property Exams.NotOriginalRoomPenalty, or in the input xml file,
     * property notOriginalRoomPenalty)
     * 
     */
    public void setNotOriginalRoomWeight(double notOriginalRoomWeight) {
        iNotOriginalRoomWeight = notOriginalRoomWeight;
    }
    /**
     * Back-to-back distance (used in {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, 
     * can be set by problem property Exams.BackToBackDistance, or in the input xml file,
     * property backToBackDistance)
     */
    public int getBackToBackDistance() {
        return iBackToBackDistance;
    }
    /**
     * Back-to-back distance (used in {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, 
     * can be set by problem property Exams.BackToBackDistance, or in the input xml file,
     * property backToBackDistance)
     */
    public void setBackToBackDistance(int backToBackDistance) {
        iBackToBackDistance = backToBackDistance;
    }
    /**
     * A period with weight {@link ExamPeriod#getWeight()} equal or greater this weight is
     * considered prohibited, for all exams that do not have this period pre-assigned. Set to
     * -1 if there is no limit. Can be set by propery Exams.PeriodProhibitedWeight, or in the 
     * input xml file, property periodProhibitedWeight).
     */
    public int getPeriodProhibitedWeight() {
        return iPeriodProhibitedWeight;
    }
    /**
     * A period with weight {@link ExamPeriod#getWeight()} equal or greater this weight is
     * considered prohibited, for all exams that do not have this period pre-assigned. Set to
     * -1 if there is no limit. Can be set by propery Exams.PeriodProhibitedWeight, or in the 
     * input xml file, property periodProhibitedWeight).
     */
    public void setPeriodProhibitedWeight(int periodProhibitedWeight) {
        iPeriodProhibitedWeight = periodProhibitedWeight;
    }
    
    
    /** Called before a value is unassigned from its variable, optimization criteria are updated */
    public void beforeUnassigned(long iteration, Value value) {
        super.beforeUnassigned(iteration, value);
        ExamPlacement placement = (ExamPlacement)value;
        iNrDirectConflicts -= placement.getNrDirectConflicts();
        iNrBackToBackConflicts -= placement.getNrBackToBackConflicts();
        iNrMoreThanTwoADayConflicts -= placement.getNrMoreThanTwoADayConflicts();
        iRoomSizePenalty -= placement.getRoomSizePenalty();
        iNrDistanceBackToBackConflicts -= placement.getNrDistanceBackToBackConflicts();
        iRoomSplitPenalty -= placement.getRoomSplitPenalty();
        iPeriodPenalty -= placement.getPeriodPenalty();
        iExamRotationPenalty -= placement.getRotationPenalty();
        iNotOriginalRoomPenalty -= placement.getNotOriginalRoomPenalty();
    }
    
    /** Called after a value is assigned to its variable, optimization criteria are updated */
    public void afterAssigned(long iteration, Value value) {
        super.afterAssigned(iteration, value);
        ExamPlacement placement = (ExamPlacement)value;
        iNrDirectConflicts += placement.getNrDirectConflicts();
        iNrBackToBackConflicts += placement.getNrBackToBackConflicts();
        iNrMoreThanTwoADayConflicts += placement.getNrMoreThanTwoADayConflicts();
        iRoomSizePenalty += placement.getRoomSizePenalty();
        iNrDistanceBackToBackConflicts += placement.getNrDistanceBackToBackConflicts();
        iRoomSplitPenalty += placement.getRoomSplitPenalty();
        iPeriodPenalty += placement.getPeriodPenalty();
        iExamRotationPenalty += placement.getRotationPenalty();
        iNotOriginalRoomPenalty += placement.getNotOriginalRoomPenalty();
    }

    /**
     * Objective function. The objective function consists of the following criteria:
     * <ul>
     *  <li>Direct student conflicts (a student is enrolled in two exams that are 
     *  scheduled at the same period, weighted by Exams.DirectConflictWeight)
     *  <li>Back-to-Back student conflicts (a student is enrolled in two exams that
     *  are scheduled in consecutive periods, weighted by Exams.BackToBackConflictWeight).
     *  If Exams.IsDayBreakBackToBack is false, there is no conflict between the last 
     *  period and the first period of consecutive days. 
     *  <li>Distance Back-to-Back student conflicts (same as Back-to-Back student conflict,
     *  but the maximum distance between rooms in which both exam take place
     *  is greater than Exams.BackToBackDistance, weighted by Exams.DistanceBackToBackConflictWeight).
     *  <li>More than two exams a day (a student is enrolled in three exams that are
     *  scheduled at the same day, weighted by Exams.MoreThanTwoADayWeight).
     *  <li>Period penalty (total of period penalties {@link Exam#getPeriodPenalty(ExamPeriod, Set)} of all exams,
     *  weighted by Exams.DirectConflictWeight).
     *  <li>Room size penalty (total of room size penalties {@link Exam#getRoomSizePenalty(Set)} of all exams,
     *  weighted by Exams.RoomSizeWeight).
     *  <li>Room split penalty (total of room split penalties {@link Exam#getRoomSplitPenalty(Set)}
     *  of all exams, weighted by Exams.RoomSplitWeight).
     *  <li>Non-original room penalty (total of non-original room penalties {@link Exam#getNotOriginalRoomPenalty(Set)}
     *  of all exams, weighted by Exams.NotOriginalRoomWeight).
     * </ul>
     * @return weighted sum of objective criteria
     */
    public double getTotalValue() {
        return 
            getDirectConflictWeight()*getNrDirectConflicts(false)+
            getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts(false)+
            getBackToBackConflictWeight()*getNrBackToBackConflicts(false)+
            getDistanceBackToBackConflictWeight()*getNrDistanceBackToBackConflicts(false)+
            getPeriodWeight()*getPeriodPenalty(false)+
            getRoomSizeWeight()*getRoomSizePenalty(false)+
            getRoomSplitWeight()*getRoomSplitPenalty(false)+
            getNotOriginalRoomWeight()*getNotOriginalRoomPenalty(false);
    }
    
    /**
     * Return weighted individual objective criteria. The objective function consists of the following criteria:
     * <ul>
     *  <li>Direct student conflicts (a student is enrolled in two exams that are 
     *  scheduled at the same period, weighted by Exams.DirectConflictWeight)
     *  <li>Back-to-Back student conflicts (a student is enrolled in two exams that
     *  are scheduled in consecutive periods, weighted by Exams.BackToBackConflictWeight).
     *  If Exams.IsDayBreakBackToBack is false, there is no conflict between the last 
     *  period and the first period of consecutive days. 
     *  <li>Distance Back-to-Back student conflicts (same as Back-to-Back student conflict,
     *  but the maximum distance between rooms in which both exam take place
     *  is greater than Exams.BackToBackDistance, weighted by Exams.DistanceBackToBackConflictWeight).
     *  <li>More than two exams a day (a student is enrolled in three exams that are
     *  scheduled at the same day, weighted by Exams.MoreThanTwoADayWeight).
     *  <li>Period penalty (total of period penalties {@link Exam#getPeriodPenalty(ExamPeriod, Set)} of all exams,
     *  weighted by Exams.DirectConflictWeight).
     *  <li>Room size penalty (total of room size penalties {@link Exam#getRoomSizePenalty(Set)} of all exams,
     *  weighted by Exams.RoomSizeWeight).
     *  <li>Room split penalty (total of room split penalties {@link Exam#getRoomSplitPenalty(Set)}
     *  of all exams, weighted by Exams.RoomSplitWeight).
     *  <li>Non-original room penalty (total of non-original room penalties {@link Exam#getNotOriginalRoomPenalty(Set)}
     *  of all exams, weighted by Exams.NotOriginalRoomWeight).
     * </ul>
     * @return an array of weighted objective criteria
     */
    public double[] getTotalMultiValue() {
        return new double[] {
                getDirectConflictWeight()*getNrDirectConflicts(false),
                getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts(false),
                getBackToBackConflictWeight()*getNrBackToBackConflicts(false),
                getDistanceBackToBackConflictWeight()*getNrDistanceBackToBackConflicts(false),
                getPeriodWeight()*getPeriodPenalty(false),
                getRoomSizeWeight()*getRoomSizePenalty(false),
                getRoomSplitWeight()*getRoomSplitPenalty(false),
                getNotOriginalRoomWeight()*getNotOriginalRoomPenalty(false)
        };
    }
    
    /**
     * String representation -- returns a list of values of objective criteria 
     */
    public String toString() {
        return 
            "DC:"+getNrDirectConflicts(false)+","+
            "M2D:"+getNrMoreThanTwoADayConflicts(false)+","+
            "BTB:"+getNrBackToBackConflicts(false)+","+
            (getBackToBackDistance()<0?"":"dBTB:"+getNrDistanceBackToBackConflicts(false)+",")+
            "PP:"+getPeriodPenalty(false)+","+
            "RP:"+getExamRotationPenalty(false)+","+
            "RSz:"+getRoomSizePenalty(false)+","+
            "RSp:"+getRoomSplitPenalty(false)+","+
            "ROg:"+getNotOriginalRoomPenalty(false);
    }

    /**
     * Return number of direct student conflicts, i.e., the total number of cases where a student is enrolled
     * into two exams that are scheduled at the same period.
     * @param precise if false, the cached value is used
     * @return number of direct student conflicts
     */
    public int getNrDirectConflicts(boolean precise) {
        if (!precise) return iNrDirectConflicts;
        int conflicts = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                int nrExams = student.getExams(period).size();
                if (nrExams>1) conflicts += nrExams-1;
            }
        }
        return conflicts;
    }
    
    /**
     * Return number of back-to-back student conflicts, i.e., the total number of cases where a student is enrolled
     * into two exams that are scheduled at consecutive periods. If {@link ExamModel#isDayBreakBackToBack()} is false,
     * the last period of one day and the first period of the following day are not considered as consecutive periods.
     * @param precise if false, the cached value is used
     * @return number of back-to-back student conflicts
     */
    public int getNrBackToBackConflicts(boolean precise) {
        if (!precise) return iNrBackToBackConflicts;
        int conflicts = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                int nrExams = student.getExams(period).size();
                if (nrExams==0) continue;
                if (period.next()!=null && !student.getExams(period.next()).isEmpty() && (isDayBreakBackToBack() || period.next().getDay()==period.getDay())) 
                    conflicts += nrExams*student.getExams(period.next()).size();
            }
        }
        return conflicts;
    }
    
    /**
     * Return number of distance back-to-back student conflicts, i.e., the total number of back-to-back student conflicts
     * where the two exam take place in rooms that are too far a part (i.e., {@link ExamPlacement#getDistance(ExamPlacement)} is
     * greater than {@link ExamModel#getBackToBackDistance()}).
     * @param precise if false, the cached value is used
     * @return number of distance back-to-back student conflicts
     */
    public int getNrDistanceBackToBackConflicts(boolean precise) {
        if (getBackToBackDistance()<0) return 0;
        if (!precise) return iNrDistanceBackToBackConflicts;
        int conflicts = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                Set exams = student.getExams(period);
                if (exams.isEmpty()) continue;
                if (period.next()!=null && !student.getExams(period.next()).isEmpty() && period.next().getDay()==period.getDay()) {
                    for (Iterator i1=exams.iterator();i1.hasNext();) {
                        Exam x1 = (Exam)i1.next();
                        ExamPlacement p1 =(ExamPlacement)x1.getAssignment();
                        for (Iterator i2=student.getExams(period.next()).iterator();i2.hasNext();) {
                            Exam x2 = (Exam)i2.next();
                            ExamPlacement p2 =(ExamPlacement)x2.getAssignment();
                            if (p1.getDistance(p2)>getBackToBackDistance()) conflicts++;
                        }
                    }
                }
            }
        }
        return conflicts;
    }

    /**
     * Return number of more than two exams a day student conflicts, i.e., the total number of cases where a student 
     * is enrolled into three exams that are scheduled at the same day (i.e., {@link ExamPeriod#getDay()} is the same).
     * @param precise if false, the cached value is used
     * @return number of more than two exams a day student conflicts
     */
    public int getNrMoreThanTwoADayConflicts(boolean precise) {
        if (!precise) return iNrMoreThanTwoADayConflicts;
        int conflicts = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (int d=0;d<getNrDays();d++) {
                int nrExams = student.getExamsADay(d).size();
                if (nrExams>2)
                    conflicts += nrExams-2;
            }
        }
        return conflicts;
    }
    
    /**
     * Return total room size penalty, i.e., the sum of {@link ExamPlacement#getRoomSizePenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total room size penalty
     */
    public int getRoomSizePenalty(boolean precise) {
        if (!precise) return iRoomSizePenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomSizePenalty();
        }
        return penalty;
    }
    
    /**
     * Return total room split penalty, i.e., the sum of {@link ExamPlacement#getRoomSplitPenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total room split penalty
     */
    public int getRoomSplitPenalty(boolean precise) {
        if (!precise) return iRoomSplitPenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomSplitPenalty();
        }
        return penalty;
    }

    /**
     * Return total period penalty, i.e., the sum of {@link ExamPlacement#getPeriodPenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total period penalty
     */
    public int getPeriodPenalty(boolean precise) {
        if (!precise) return iPeriodPenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getPeriodPenalty();
        }
        return penalty;
    }
    

    /**
     * Return total exam rotation penalty, i.e., the sum of {@link ExamPlacement#getRotationPenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total period penalty
     */
    public int getExamRotationPenalty(boolean precise) {
        if (!precise) return iExamRotationPenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRotationPenalty();
        }
        return penalty;
    }

    /**
     * Return total non-original room penalty, i.e., the sum of {@link ExamPlacement#getNotOriginalRoomPenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total non-original room penalty
     */
    public int getNotOriginalRoomPenalty(boolean precise) {
        if (!precise) return iNotOriginalRoomPenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getNotOriginalRoomPenalty();
        }
        return penalty;
    }

    /**
     * Info table
     */
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("Direct Conflicts",
                getNrDirectConflicts(false)+" ("+
                sDoubleFormat.format(getDirectConflictWeight()*getNrDirectConflicts(false))+")");
        info.put("More Than 2 A Day Conflicts",
                getNrMoreThanTwoADayConflicts(false)+" ("+
                sDoubleFormat.format(getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts(false))+")");
        info.put("Back-To-Back Conflicts",
                getNrBackToBackConflicts(false)+" ("+
                sDoubleFormat.format(getBackToBackConflictWeight()*getNrBackToBackConflicts(false))+")");
        info.put("Distance Back-To-Back Conflicts",
                getNrDistanceBackToBackConflicts(false)+" ("+
                sDoubleFormat.format(getDistanceBackToBackConflictWeight()*getNrDistanceBackToBackConflicts(false))+")");
        info.put("Room Size Penalty",
                getRoomSizePenalty(false)+" ("+
                sDoubleFormat.format(getRoomSizeWeight()*getRoomSizePenalty(false))+")");
        info.put("Room Split Penalty",
                getRoomSplitPenalty(false)+" ("+
                sDoubleFormat.format(getRoomSplitWeight()*getRoomSplitPenalty(false))+")");
        info.put("Period Penalty",
                getPeriodPenalty(false)+" ("+
                sDoubleFormat.format(getPeriodWeight()*getPeriodPenalty(false))+")");
        info.put("Exam Rotation Penalty",
                getExamRotationPenalty(false)+" ("+
                sDoubleFormat.format(getExamRotationWeight()*getExamRotationPenalty(false))+")");
        info.put("Not-Original Room Penalty",
                getNotOriginalRoomPenalty(false)+" ("+
                sDoubleFormat.format(getNotOriginalRoomWeight()*getNotOriginalRoomPenalty(false))+")");
        return info;
    }
    
    /**
     * Extended info table
     */
    public Hashtable getExtendedInfo() {
        Hashtable info = super.getExtendedInfo();
        info.put("Direct Conflicts [p]",String.valueOf(getNrDirectConflicts(true)));
        info.put("More Than 2 A Day Conflicts [p]",String.valueOf(getNrMoreThanTwoADayConflicts(true)));
        info.put("Back-To-Back Conflicts [p]",String.valueOf(getNrBackToBackConflicts(true)));
        info.put("Distance Back-To-Back Conflicts [p]",String.valueOf(getNrDistanceBackToBackConflicts(true)));
        info.put("Room Size Penalty [p]",String.valueOf(getRoomSizePenalty(true)));
        info.put("Room Split Penalty [p]",String.valueOf(getRoomSplitPenalty(true)));
        info.put("Period Penalty [p]",String.valueOf(getPeriodPenalty(true)));
        info.put("Not-Original Room Penalty [p]",String.valueOf(getNotOriginalRoomPenalty(true)));
        info.put("Number of Periods",String.valueOf(getPeriods().size()));
        info.put("Number of Exams",String.valueOf(variables().size()));
        info.put("Number of Rooms",String.valueOf(getRooms().size()));
        String rgAllId = "A";
        int avail = 0, availAlt = 0;
        for (Enumeration e=getRoomGroups().elements();e.hasMoreElements();) {
            ExamRoomGroup rg = (ExamRoomGroup)e.nextElement();
            if (rg.getRooms().isEmpty()) continue;
            info.put("Number of Rooms (group "+rg.getId()+")",String.valueOf(rg.getRooms().size()));
            if (rgAllId.equals(rg.getId())) {
                for (Enumeration f=rg.getRooms().elements();f.hasMoreElements();) {
                    ExamRoom room = (ExamRoom)f.nextElement();
                    for (Enumeration g=getPeriods().elements();g.hasMoreElements();) {
                        ExamPeriod period = (ExamPeriod)g.nextElement();
                        if (room.isAvailable(period)) {
                            avail+=room.getSize();
                            availAlt+=room.getAltSize();
                        }
                    }
                }
            }
            info.put("Space in Rooms (Group "+rg.getId()+")",
                    rg.getSpace()+
                    " (min:"+rg.getMinSize()+", max:"+rg.getMaxSize()+
                    ", avg:"+rg.getAvgSize()+", med:"+rg.getMedSize()+")");
            info.put("Space in Rooms (Alternative, Group "+rg.getId()+")",
                    rg.getAltSpace()+
                    " (min:"+rg.getMinAltSize()+", max:"+rg.getMaxAltSize()+
                    ", avg:"+rg.getAvgAltSize()+", med:"+rg.getMedAltSize()+")");
        }
        info.put("Number of Students",String.valueOf(getStudents().size()));
        int nrStudentExams = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            nrStudentExams += student.variables().size();
        }
        info.put("Number of Student Exams",String.valueOf(nrStudentExams));
        int nrAltExams = 0, nrSectionExams = 0, nrOrigRoomExams = 0, nrPreassignedTime = 0, nrPreassignedRoom = 0, nrSmallExams = 0;
        double fill = 0;
        double altRatio = ((double)avail)/availAlt;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (exam.getMaxRooms()>0)
                fill += (exam.hasAltSeating()?altRatio:1.0)*exam.getStudents().size();
            if (exam.isSectionExam()) nrSectionExams++;
            if (exam.hasAltSeating()) nrAltExams++;
            if (exam.getOriginalRoom()!=null) nrOrigRoomExams++;
            if (exam.hasPreAssignedPeriod()) nrPreassignedTime++;
            if (exam.hasPreAssignedRooms()) nrPreassignedRoom++;
            if (exam.getMaxRooms()==0) nrSmallExams++;
        }
        info.put("Estimated Schedule Infilling (Group "+rgAllId+")", sDoubleFormat.format(100.0*fill/avail)+"% ("+Math.round(fill)+" of "+avail+")");
        info.put("Number of Exams Requiring Alt Seating",String.valueOf(nrAltExams));
        info.put("Number of Small Exams (Exams W/O Room)",String.valueOf(nrSmallExams));
        info.put("Number of Section Exams",String.valueOf(nrSectionExams));
        info.put("Number of Course Exams",String.valueOf(variables().size()-nrSectionExams));
        info.put("Number of Exams With Original Room",String.valueOf(nrOrigRoomExams));
        info.put("Number of Exams With Pre-Assigned Time",String.valueOf(nrPreassignedTime));
        info.put("Number of Exams With Pre-Assigned Room",String.valueOf(nrPreassignedRoom));
        int[] nbrMtgs = new int[11];
        for (int i=0;i<=10;i++) nbrMtgs[i]=0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            nbrMtgs[Math.min(10,student.variables().size())]++;
        }
        for (int i=0;i<=10;i++) {
            if (nbrMtgs[i]==0) continue;
            info.put("Number of Students with "+(i==0?"no":String.valueOf(i))+(i==10?" or more":"")+" meeting"+(i!=1?"s":""),String.valueOf(nbrMtgs[i]));
        }
        return info;
    }

    /**
     * Problem room groups
     * @return list of {@link ExamRoomGroup}
     */
    public Vector getRoomGroups() {
        return iRoomGroups;
    }
    
    /**
     * Problem properties
     */
    public DataProperties getProperties() {
        return iProperties;
    }

    /**
     * Problem rooms
     * @return list of {@link ExamRoom}
     */
    public Vector getRooms() { return iRooms; }
    
    /**
     * Problem students
     * @return list of {@link ExamStudent}
     */
    public Vector getStudents() { return iStudents; }
    
    /**
     * Problem instructors
     * @return list of {@link ExamInstructor}
     */
    public Vector getInstructors() { return iInstructors; }

    /**
     * Distribution constraints
     * @return list of {@link ExamDistributionConstraint} 
     */
    public Vector getDistributionConstraints() { return iDistributionConstraints;}
    
    /**
     * Save model (including its solution) into XML.
     */
    public Document save() {
        boolean saveInitial = getProperties().getPropertyBoolean("Xml.SaveInitial", true);
        boolean saveSolution = getProperties().getPropertyBoolean("Xml.SaveSolution", true);
        boolean saveConflictTable = getProperties().getPropertyBoolean("Xml.SaveConflictTable", true);
        Document document = DocumentHelper.createDocument();
        document.addComment("Examination Timetable");
        if (nrAssignedVariables()>0) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Dictionary solutionInfo=getExtendedInfo();
            for (Enumeration e=ToolBox.sortEnumeration(solutionInfo.keys());e.hasMoreElements();) {
                String key = (String)e.nextElement();
                Object value = solutionInfo.get(key);
                comments.append("    "+key+": "+value+"\n");
            }
            document.addComment(comments.toString());
        }
        Element root = document.addElement("examtt");
        root.addAttribute("version","1.0");
        root.addAttribute("initiative", getProperties().getProperty("Data.Initiative"));
        root.addAttribute("term", getProperties().getProperty("Data.Term"));
        root.addAttribute("year", getProperties().getProperty("Data.Year"));
        root.addAttribute("created", String.valueOf(new Date()));
        Element params = root.addElement("parameters");
        params.addElement("property").addAttribute("name", "isDayBreakBackToBack").addAttribute("value", (isDayBreakBackToBack()?"true":"false"));
        params.addElement("property").addAttribute("name", "directConflictWeight").addAttribute("value", String.valueOf(getDirectConflictWeight()));
        params.addElement("property").addAttribute("name", "moreThanTwoADayWeight").addAttribute("value", String.valueOf(getMoreThanTwoADayWeight()));
        params.addElement("property").addAttribute("name", "backToBackConflictWeight").addAttribute("value", String.valueOf(getBackToBackConflictWeight()));
        params.addElement("property").addAttribute("name", "distanceBackToBackConflictWeight").addAttribute("value", String.valueOf(getDistanceBackToBackConflictWeight()));
        params.addElement("property").addAttribute("name", "backToBackDistance").addAttribute("value", String.valueOf(getBackToBackDistance()));
        params.addElement("property").addAttribute("name", "maxRooms").addAttribute("value", String.valueOf(getMaxRooms()));
        params.addElement("property").addAttribute("name", "periodWeight").addAttribute("value", String.valueOf(getPeriodWeight()));
        params.addElement("property").addAttribute("name", "examRotationWeight").addAttribute("value", String.valueOf(getExamRotationWeight()));
        params.addElement("property").addAttribute("name", "roomSizeWeight").addAttribute("value", String.valueOf(getRoomSizeWeight()));
        params.addElement("property").addAttribute("name", "roomSplitWeight").addAttribute("value", String.valueOf(getRoomSplitWeight()));
        params.addElement("property").addAttribute("name", "notOriginalRoomWeight").addAttribute("value", String.valueOf(getNotOriginalRoomWeight()));
        params.addElement("property").addAttribute("name", "periodProhibitedWeight").addAttribute("value", String.valueOf(getPeriodProhibitedWeight()));
        Element periods = root.addElement("periods");
        for (Enumeration e=getPeriods().elements();e.hasMoreElements();) {
            ExamPeriod period = (ExamPeriod)e.nextElement();
            periods.addElement("period").
                addAttribute("id", String.valueOf(period.getId())).
                addAttribute("length", String.valueOf(period.getLength())).
                addAttribute("day", period.getDayStr()).
                addAttribute("time", period.getTimeStr()).
                addAttribute("weight", String.valueOf(period.getWeight()));
        }
        Element rooms = root.addElement("rooms");
        for (Enumeration e=getRooms().elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            Element r = rooms.addElement("room");
            r.addAttribute("id", String.valueOf(room.getId()));
            if (room.hasName())
                r.addAttribute("name", room.getName());
            r.addAttribute("size", String.valueOf(room.getSize()));
            r.addAttribute("alt", String.valueOf(room.getAltSize()));
            if (room.getCoordX()>=0 && room.getCoordY()>=0)
                r.addAttribute("coordinates", room.getCoordX()+","+room.getCoordY());
            String gr = "";
            for (Enumeration f=getRoomGroups().elements();f.hasMoreElements();) {
                ExamRoomGroup rg = (ExamRoomGroup)f.nextElement();
                if (rg.getRooms().contains(room)) {
                    if (gr.length()>0) gr+=",";
                    gr+=rg.getId();
                }
            }
            if (gr.length()>0)
                r.addAttribute("groups", gr);
            String available = "";
            boolean allAvail = true;
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                available += room.isAvailable(period)?"1":"0";
                if (!room.isAvailable(period)) allAvail=false;
                if (room.getWeight(period)!=0)
                    r.addElement("period").addAttribute("id", String.valueOf(period.getId())).addAttribute("weight", String.valueOf(room.getWeight(period)));
            }
            if (!allAvail)
                r.addAttribute("available", available);
        }
        int perProhW = getPeriodProhibitedWeight();
        setPeriodProhibitedWeight(-1);
        Element exams = root.addElement("exams");
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            Element ex = exams.addElement("exam");
            ex.addAttribute("id", String.valueOf(exam.getId()));
            if (exam.hasName())
                ex.addAttribute("name", exam.getName());
            ex.addAttribute("length", String.valueOf(exam.getLength()));
            ex.addAttribute("type", (exam.isSectionExam()?"section":"course"));
            ex.addAttribute("alt", (exam.hasAltSeating()?"true":"false"));
            if (exam.getMaxRooms()!=getMaxRooms())
                ex.addAttribute("maxRooms", String.valueOf(exam.getMaxRooms()));
            ex.addAttribute("enrl", String.valueOf(exam.getStudents().size()));
            for (Enumeration f=exam.getCourseSections().elements();f.hasMoreElements();) {
                ExamCourseSection cs = (ExamCourseSection)f.nextElement();
                ex.addElement(cs.isSection()?"section":"course").addAttribute("id", String.valueOf(cs.getId())).addAttribute("name", cs.getName());
            }
            if (exam.getOriginalRoom()!=null)
                ex.addElement("original-room").addAttribute("id", String.valueOf(exam.getOriginalRoom().getId()));
            if (exam.hasPreAssignedPeriod() || !exam.getPreassignedRooms().isEmpty()) {
                Element pre = ex.addElement("pre-assigned");
                if (exam.hasPreAssignedPeriod())
                    pre.addElement("period").addAttribute("id", String.valueOf(exam.getPreAssignedPeriod().getId()));
                for (Iterator i=exam.getPreassignedRooms().iterator();i.hasNext();) {
                    ExamRoom r = (ExamRoom)i.next();
                    pre.addElement("room").addAttribute("id", String.valueOf(r.getId()));
                }
            }
            String available = "";
            boolean allAvail = true;
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                available += exam.isAvailable(period)?"1":"0";
                if (!exam.isAvailable(period)) allAvail=false;
                if (exam.getWeight(period)!=0)
                    ex.addElement("period").addAttribute("id", String.valueOf(period.getId())).addAttribute("weight", String.valueOf(exam.getWeight(period)));
            }
            if (!allAvail)
                ex.addAttribute("available", available);
            if (exam.hasAveragePeriod())
                ex.addAttribute("average", String.valueOf(exam.getAveragePeriod()));
            String rgs = "";
            for (Enumeration f=exam.getRoomGroups().elements();f.hasMoreElements();) {
                ExamRoomGroup rg = (ExamRoomGroup)f.nextElement();
                if (rg.getRooms().isEmpty()) continue;
                if (rgs.length()>0) rgs+=",";
                rgs+=rg.getId();
            }
            if (rgs.length()>0) ex.addAttribute("groups", rgs);
            ExamPlacement p = (ExamPlacement)exam.getAssignment();
            if (p!=null && saveSolution) {
                Element asg = ex.addElement("assignment");
                asg.addElement("period").addAttribute("id", String.valueOf(p.getPeriod().getId()));
                for (Iterator i=p.getRooms().iterator();i.hasNext();) {
                    ExamRoom r = (ExamRoom)i.next();
                    asg.addElement("room").addAttribute("id", String.valueOf(r.getId()));
                }            }
            p = (ExamPlacement)exam.getInitialAssignment();
            if (p!=null && saveInitial) {
                Element ini = ex.addElement("initial");
                ini.addElement("period").addAttribute("id", String.valueOf(p.getPeriod().getId()));
                for (Iterator i=p.getRooms().iterator();i.hasNext();) {
                    ExamRoom r = (ExamRoom)i.next();
                    ini.addElement("room").addAttribute("id", String.valueOf(r.getId()));
                }
            }
            if (exam.getRoomWeights()!=null) {
                for (Iterator j=exam.getRoomWeights().entrySet().iterator();j.hasNext();) {
                    Map.Entry entry = (Map.Entry)j.next();
                    ExamRoom room = (ExamRoom)entry.getKey();
                    int weight = ((Integer)entry.getValue()).intValue();
                    Element re = ex.addElement("room").addAttribute("id", String.valueOf(room.getId()));
                    if (weight!=0)
                        re.addAttribute("weight", String.valueOf(weight));
                }
            }
        }
        setPeriodProhibitedWeight(perProhW);
        Element students = root.addElement("students");
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            Element s = students.addElement("student");
            s.addAttribute("id", String.valueOf(student.getId()));
            for (Enumeration f=student.variables().elements();f.hasMoreElements();) {
                Exam ex = (Exam)f.nextElement();
                Element x = s.addElement("exam").addAttribute("id", String.valueOf(ex.getId()));
                for (Enumeration g=ex.getCourseSections(student).elements();g.hasMoreElements();) {
                    ExamCourseSection cs = (ExamCourseSection)g.nextElement();
                    x.addElement(cs.isSection()?"section":"course").addAttribute("id", String.valueOf(cs.getId()));
                }
            }
        }
        Element instructors = root.addElement("instructors");
        for (Enumeration e=getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor instructor = (ExamInstructor)e.nextElement();
            Element i = instructors.addElement("instructor");
            i.addAttribute("id", String.valueOf(instructor.getId()));
            if (instructor.hasName())
                i.addAttribute("name", instructor.getName());
            for (Enumeration f=instructor.variables().elements();f.hasMoreElements();) {
                Exam ex = (Exam)f.nextElement();
                Element x = i.addElement("exam").addAttribute("id", String.valueOf(ex.getId()));
                for (Enumeration g=ex.getCourseSections(instructor).elements();g.hasMoreElements();) {
                    ExamCourseSection cs = (ExamCourseSection)g.nextElement();
                    x.addElement(cs.isSection()?"section":"course").addAttribute("id", String.valueOf(cs.getId()));
                }
            }
        }
        Element distConstraints = root.addElement("constraints");
        for (Enumeration e=getDistributionConstraints().elements();e.hasMoreElements();) {
            ExamDistributionConstraint distConstraint = (ExamDistributionConstraint)e.nextElement();
            Element dc = distConstraints.addElement(distConstraint.getTypeString());
            dc.addAttribute("id", String.valueOf(distConstraint.getId()));
            if (!distConstraint.isHard()) {
                dc.addAttribute("hard","false");
                dc.addAttribute("weight", String.valueOf(distConstraint.getWeight()));
            }
            for (Enumeration f=distConstraint.variables().elements();f.hasMoreElements();) {
                dc.addElement("exam").addAttribute("id", String.valueOf(((Exam)f.nextElement()).getId()));
            }
        }
        if (saveConflictTable) {
            Element conflicts = root.addElement("conflicts");
            for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)f.nextElement();
                    int nrExams = student.getExams(period).size();
                    if (nrExams>1) {
                        Element dir = conflicts.addElement("direct").addAttribute("student", String.valueOf(student.getId()));
                        for (Iterator i=student.getExams(period).iterator();i.hasNext();) {
                            Exam exam = (Exam)i.next();
                            dir.addElement("exam").addAttribute("id",String.valueOf(exam.getId()));
                        }
                    }
                    if (nrExams>0) {
                        if (period.next()!=null && !student.getExams(period.next()).isEmpty() && (!isDayBreakBackToBack() || period.next().getDay()==period.getDay())) {
                            for (Iterator i=student.getExams(period).iterator();i.hasNext();) {
                                Exam ex1 = (Exam)i.next();
                                for (Iterator j=student.getExams(period.next()).iterator();j.hasNext();) {
                                    Exam ex2 = (Exam)j.next();
                                    Element btb = conflicts.addElement("back-to-back").addAttribute("student", String.valueOf(student.getId()));
                                    btb.addElement("exam").addAttribute("id",String.valueOf(ex1.getId()));
                                    btb.addElement("exam").addAttribute("id",String.valueOf(ex2.getId()));
                                    if (getBackToBackDistance()>=0) {
                                        int dist = ((ExamPlacement)ex1.getAssignment()).getDistance((ExamPlacement)ex2.getAssignment());
                                        if (dist>0) btb.addAttribute("distance", String.valueOf(dist));
                                    }
                                }
                            }
                        }
                    }
                    if (period.next()==null || period.next().getDay()!=period.getDay()) {
                        int nrExamsADay = student.getExamsADay(period.getDay()).size();
                        if (nrExamsADay>2) {
                            Element mt2 = conflicts.addElement("more-2-day").addAttribute("student", String.valueOf(student.getId()));
                            for (Iterator i=student.getExamsADay(period.getDay()).iterator();i.hasNext();) {
                                Exam exam = (Exam)i.next();
                                mt2.addElement("exam").addAttribute("id",String.valueOf(exam.getId()));
                            }
                        }
                    }
                }
            }
            
        }
        return document;
    }
    
    /**
     * Load model (including its solution) from XML.
     */
    public boolean load(Document document) {
        boolean loadInitial = getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        boolean loadSolution = getProperties().getPropertyBoolean("Xml.LoadSolution", true);
        boolean loadPreassignedTimes = getProperties().getPropertyBoolean("Xml.LoadPreassignedTimes", true);
        boolean loadPreassignedRooms = getProperties().getPropertyBoolean("Xml.LoadPreassignedRooms", true);
        Element root=document.getRootElement();
        if (!"examtt".equals(root.getName())) return false;
        if (root.attribute("initiative")!=null)
            getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));
        if (root.attribute("term")!=null)
            getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attribute("year")!=null)
            getProperties().setProperty("Data.Year", root.attributeValue("year"));
        if (root.element("parameters")!=null)
            for (Iterator i=root.element("parameters").elementIterator("property");i.hasNext();) {
                Element e = (Element)i.next();
                String name = e.attributeValue("name");
                String value = e.attributeValue("value");
                if ("isDayBreakBackToBack".equals(name)) setDayBreakBackToBack("true".equals(value));
                else if ("directConflictWeight".equals(name)) setDirectConflictWeight(Double.parseDouble(value));
                else if ("moreThanTwoADayWeight".equals(name)) setMoreThanTwoADayWeight(Double.parseDouble(value));
                else if ("backToBackConflictWeight".equals(name)) setBackToBackConflictWeight(Double.parseDouble(value));
                else if ("distanceBackToBackConflictWeight".equals(name)) setDistanceBackToBackConflictWeight(Double.parseDouble(value));
                else if ("backToBackDistance".equals(name)) setBackToBackDistance(Integer.parseInt(value));
                else if ("maxRooms".equals(name)) setMaxRooms(Integer.parseInt(value));
                else if ("periodWeight".equals(name)) setPeriodWeight(Double.parseDouble(value));
                else if ("examRotationWeight".equals(name)) setExamRotationWeight(Double.parseDouble(value));
                else if ("roomSizeWeight".equals(name)) setRoomSizeWeight(Double.parseDouble(value));
                else if ("roomSplitWeight".equals(name)) setRoomSplitWeight(Double.parseDouble(value));
                else if ("notOriginalRoomWeight".equals(name)) setNotOriginalRoomWeight(Double.parseDouble(value));
                else if ("periodProhibitedWeight".equals(name)) setPeriodProhibitedWeight(Integer.parseInt(value));
                else getProperties().setProperty(name, value);
            }
        for (Iterator i=root.element("periods").elementIterator("period");i.hasNext();) {
            Element e = (Element)i.next();
            addPeriod(Long.valueOf(e.attributeValue("id")), e.attributeValue("day"), e.attributeValue("time"), Integer.parseInt(e.attributeValue("length")), Integer.parseInt(e.attributeValue("weight")));
        }
        Hashtable rooms = new Hashtable();
        Hashtable roomGroups = new Hashtable();
        for (Iterator i=root.element("rooms").elementIterator("room");i.hasNext();) {
            Element e = (Element)i.next();
            String coords = e.attributeValue("coordinates");
            ExamRoom room = new ExamRoom(this,
                    Long.parseLong(e.attributeValue("id")),
                    e.attributeValue("name"),
                    Integer.parseInt(e.attributeValue("size")),
                    Integer.parseInt(e.attributeValue("alt")),
                    (coords==null?-1:Integer.parseInt(coords.substring(0,coords.indexOf(',')))),
                    (coords==null?-1:Integer.parseInt(coords.substring(coords.indexOf(',')+1))));
            addConstraint(room);
            getRooms().add(room);
            rooms.put(new Long(room.getId()),room);
            String available = e.attributeValue("available");
            if (available!=null)
                for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)f.nextElement();
                    if (available.charAt(period.getIndex())=='0') room.setAvailable(period.getIndex(), false);
                }
            for (Iterator j=e.elementIterator("period");j.hasNext();) {
                Element pe = (Element)j.next();
                room.setWeight(getPeriod(Long.valueOf(pe.attributeValue("id"))).getIndex(),Integer.parseInt(pe.attributeValue("weight")));
            }
            String rg = e.attributeValue("groups");
            if (rg!=null) for (StringTokenizer stk=new StringTokenizer(rg,",");stk.hasMoreTokens();) {
                String roomGroupId = (String)stk.nextToken();
                ExamRoomGroup gr = (ExamRoomGroup)roomGroups.get(roomGroupId);
                if (gr==null) {
                    gr = new ExamRoomGroup(roomGroupId);
                    getRoomGroups().add(gr);
                    roomGroups.put(roomGroupId,gr);
                }
                gr.addRoom(room);
            }
        }
        Vector assignments = new Vector();
        Hashtable exams = new Hashtable();
        Hashtable courseSections = new Hashtable();
        for (Iterator i=root.element("exams").elementIterator("exam");i.hasNext();) {
            Element e = (Element)i.next();
            Exam exam = new Exam(
                    Long.parseLong(e.attributeValue("id")),
                    e.attributeValue("name"),
                    Integer.parseInt(e.attributeValue("length")),
                    "section".equals(e.attributeValue("type")),
                    "true".equals(e.attributeValue("alt")),
                    (e.attribute("maxRooms")==null?getMaxRooms():Integer.parseInt(e.attributeValue("maxRooms"))));
            exams.put(new Long(exam.getId()),exam);
            if (e.element("original-room")!=null)
                exam.setOriginalRoom((ExamRoom)rooms.get(Long.valueOf(e.element("original-room").attributeValue("id"))));
            Element pre = e.element("pre-assigned");
            if (pre!=null) {
                Element per = pre.element("period");
                if (per!=null && loadPreassignedTimes)
                    exam.setPreAssignedPeriod(getPeriod(Long.valueOf(per.attributeValue("id"))));
                if (loadPreassignedRooms)
                    for (Iterator j=pre.elementIterator("room");j.hasNext();) {
                        Long roomId = Long.valueOf(((Element)j.next()).attributeValue("id"));
                        exam.getPreassignedRooms().add((ExamRoom)rooms.get(roomId));
                    }
            }
            addVariable(exam);
            String available = e.attributeValue("available");
            if (available!=null)
                for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)f.nextElement();
                    if (available.charAt(period.getIndex())=='0') exam.setAvailable(period.getIndex(), false);
                }
            for (Iterator j=e.elementIterator("period");j.hasNext();) {
                Element pe = (Element)j.next();
                exam.setWeight(getPeriod(Long.valueOf(pe.attributeValue("id"))).getIndex(),Integer.parseInt(pe.attributeValue("weight")));
            }
            for (Iterator j=e.elementIterator("room");j.hasNext();) {
                Element re = (Element)j.next();
                if (exam.getRoomWeights()==null) exam.setRoomWeights(new Hashtable());
                exam.getRoomWeights().put(rooms.get(Long.valueOf(re.attributeValue("id"))), Integer.valueOf(re.attributeValue("weight","0")));
            }
            if (e.attribute("average")!=null)
                exam.setAveragePeriod(Integer.parseInt(e.attributeValue("average")));
            String rgs = e.attributeValue("groups");
            if (rgs!=null)
                for (StringTokenizer s=new StringTokenizer(rgs,",");s.hasMoreTokens();) {
                    exam.addRoomGroup((ExamRoomGroup)roomGroups.get(s.nextToken()));
                }
            Element asg = e.element("assignment");
            if (asg!=null && loadSolution) {
                Element per = asg.element("period");
                if (per!=null) {
                    ExamPlacement p = new ExamPlacement(exam, getPeriod(Long.valueOf(per.attributeValue("id"))), new HashSet());
                    for (Iterator j=asg.elementIterator("room");j.hasNext();) {
                        Long roomId = Long.valueOf(((Element)j.next()).attributeValue("id"));
                        p.getRooms().add((ExamRoom)rooms.get(roomId));
                    }
                    assignments.add(p);
                }
            }
            Element ini = e.element("initial");
            if (ini!=null && loadInitial) {
                Element per = ini.element("period");
                if (per!=null) {
                    ExamPlacement p = new ExamPlacement(exam, getPeriod(Long.valueOf(per.attributeValue("id"))), new HashSet());
                    for (Iterator j=ini.elementIterator("room");j.hasNext();) {
                        Long roomId = Long.valueOf(((Element)j.next()).attributeValue("id"));
                        p.getRooms().add((ExamRoom)rooms.get(roomId));
                    }
                    exam.setInitialAssignment(p);
                }
            }
            for (Iterator j=e.elementIterator("course");j.hasNext();) {
                Element f = (Element)j.next();
                ExamCourseSection cs = new ExamCourseSection(exam, Long.parseLong(f.attributeValue("id")),f.attributeValue("name"), false);
                exam.getCourseSections().add(cs);
                courseSections.put(new Long(cs.getId()),cs);
            }
            for (Iterator j=e.elementIterator("section");j.hasNext();) {
                Element f = (Element)j.next();
                ExamCourseSection cs = new ExamCourseSection(exam, Long.parseLong(f.attributeValue("id")),f.attributeValue("name"), true);
                exam.getCourseSections().add(cs);
                courseSections.put(new Long(cs.getId()),cs);
            }
        }
        for (Iterator i=root.element("students").elementIterator("student");i.hasNext();) {
            Element e = (Element)i.next();
            ExamStudent student = new ExamStudent(this,Long.parseLong(e.attributeValue("id")));
            for (Iterator j=e.elementIterator("exam");j.hasNext();) {
                Element x = (Element)j.next();
                Exam ex = (Exam)exams.get(Long.valueOf(x.attributeValue("id")));
                student.addVariable(ex);
                for (Iterator k=x.elementIterator("course");k.hasNext();) {
                    Element f = (Element)k.next();
                    ExamCourseSection cs = (ExamCourseSection)courseSections.get(Long.valueOf(f.attributeValue("id")));
                    student.getCourseSections().add(cs);
                    cs.getStudents().add(student);
                }
                for (Iterator k=x.elementIterator("section");k.hasNext();) {
                    Element f = (Element)k.next();
                    ExamCourseSection cs = (ExamCourseSection)courseSections.get(Long.valueOf(f.attributeValue("id")));
                    student.getCourseSections().add(cs);
                    cs.getStudents().add(student);
                }
            }
            addConstraint(student);
            getStudents().add(student);
        }        
        if (root.element("instructors")!=null)
            for (Iterator i=root.element("instructors").elementIterator("instructor");i.hasNext();) {
                Element e = (Element)i.next();
                ExamInstructor instructor = new ExamInstructor(this,Long.parseLong(e.attributeValue("id")),e.attributeValue("name"));
                for (Iterator j=e.elementIterator("exam");j.hasNext();) {
                    Element x = (Element)j.next();
                    Exam ex = (Exam)exams.get(Long.valueOf(x.attributeValue("id")));
                    instructor.addVariable(ex);
                    for (Iterator k=x.elementIterator("course");k.hasNext();) {
                        Element f = (Element)k.next();
                        ExamCourseSection cs = (ExamCourseSection)courseSections.get(Long.valueOf(f.attributeValue("id")));
                        instructor.getCourseSections().add(cs);
                        cs.getIntructors().add(instructor);
                    }
                    for (Iterator k=x.elementIterator("section");k.hasNext();) {
                        Element f = (Element)k.next();
                        ExamCourseSection cs = (ExamCourseSection)courseSections.get(Long.valueOf(f.attributeValue("id")));
                        instructor.getCourseSections().add(cs);
                        cs.getIntructors().add(instructor);
                    }
                }
                addConstraint(instructor);
                getInstructors().add(instructor);
            }        
        if (root.element("constraints")!=null)
            for (Iterator i=root.element("constraints").elementIterator();i.hasNext();) {
                Element e = (Element)i.next();
                ExamDistributionConstraint dc = new ExamDistributionConstraint(Long.parseLong(e.attributeValue("id")), e.getName(),
                        "true".equals(e.attributeValue("hard","true")), Integer.parseInt(e.attributeValue("weight","0")));
                for (Iterator j=e.elementIterator("exam");j.hasNext();) {
                    dc.addVariable((Exam)exams.get(Long.valueOf(((Element)j.next()).attributeValue("id"))));
                }
                addConstraint(dc);
                getDistributionConstraints().add(dc);
            }
        init();
        for (Enumeration e=assignments.elements();e.hasMoreElements();) {
            ExamPlacement placement = (ExamPlacement)e.nextElement();
            Exam exam = (Exam)placement.variable();
            Set conf = conflictValues(placement);
            if (!conf.isEmpty()) {
                for (Iterator i=conflictConstraints(placement).entrySet().iterator();i.hasNext();) {
                    Map.Entry entry = (Map.Entry)i.next();
                    Constraint constraint = (Constraint)entry.getKey();
                    Set values = (Set)entry.getValue();
                    if (constraint instanceof ExamStudent) {
                        ((ExamStudent)constraint).setAllowDirectConflicts(true);
                        exam.setAllowDirectConflicts(true);
                        for (Iterator j=values.iterator();j.hasNext();)
                            ((Exam)((ExamPlacement)j.next()).variable()).setAllowDirectConflicts(true);
                    }
                }
                conf = conflictValues(placement);
            }
            if (conf.isEmpty()) {
                exam.assign(0, placement);
            } else {
                sLog.error("Unable to assign "+exam.getInitialAssignment().getName()+" to exam "+exam.getName());
                sLog.error("Conflicts:"+ToolBox.dict2string(conflictConstraints(exam.getInitialAssignment()), 2));
            }
        }
        for (Enumeration e=new Vector(unassignedVariables()).elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (!exam.hasPreAssignedPeriod()) continue;
            ExamPlacement placement = null;
            if (exam.hasPreAssignedRooms()) {
                placement = new ExamPlacement(exam, exam.getPreAssignedPeriod(), new HashSet(exam.getPreassignedRooms()));
            } else {
                Set bestRooms = exam.findBestAvailableRooms(exam.getPreAssignedPeriod());
                if (bestRooms==null) {
                    sLog.error("Unable to assign "+exam.getPreAssignedPeriod()+" to exam "+exam.getName()+" -- no suitable room found.");
                    continue;
                }
                placement = new ExamPlacement(exam, exam.getPreAssignedPeriod(), bestRooms);
            }
            Set conflicts = conflictValues(placement);
            if (!conflicts.isEmpty()) {
                for (Iterator i=conflictConstraints(placement).entrySet().iterator();i.hasNext();) {
                    Map.Entry entry = (Map.Entry)i.next();
                    Constraint constraint = (Constraint)entry.getKey();
                    Set values = (Set)entry.getValue();
                    if (constraint instanceof ExamStudent) {
                        ((ExamStudent)constraint).setAllowDirectConflicts(true);
                        exam.setAllowDirectConflicts(true);
                        for (Iterator j=values.iterator();j.hasNext();)
                            ((Exam)((ExamPlacement)j.next()).variable()).setAllowDirectConflicts(true);
                    }
                }
                conflicts = conflictValues(placement);
            }
            if (conflicts.isEmpty()) {
                exam.assign(0, placement);
            } else {
                sLog.error("Unable to assign "+placement.getName()+" to exam "+exam.getName());
                sLog.error("Conflicts:"+ToolBox.dict2string(conflictConstraints(exam.getInitialAssignment()), 2));
            }
        }
        return true;
    }
}
