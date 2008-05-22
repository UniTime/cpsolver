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
import net.sf.cpsolver.ifs.util.Callback;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Examination timetabling model. Exams {@link Exam} are modeled as
 * variables, rooms {@link ExamRoom} and students {@link ExamStudent}
 * as constraints. Assignment of an exam to time (modeled as non-overlapping 
 * periods {@link ExamPeriod}) and space (set of rooms) is modeled 
 * using values {@link ExamPlacement}. In order to be able to model individual period and
 * room preferences, period and room assignments are wrapped with
 * {@link ExamPeriodPlacement} and {@link ExamRoomPlacement} classes respectively.
 * Moreover, additional distribution constraint {@link ExamDistributionConstraint} can be defined 
 * in the model.
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
 *  <li>Period penalty (total of period penalties {@link ExamPlacement#getPeriodPenalty()} of all assigned exams,
 *  weighted by Exams.PeriodWeight).
 *  <li>Room size penalty (total of room size penalties {@link ExamPlacement#getRoomSizePenalty()} of all assigned exams,
 *  weighted by Exams.RoomSizeWeight).
 *  <li>Room split penalty (total of room split penalties {@link ExamPlacement#getRoomSplitPenalty()}
 *  of all assigned exams, weighted by Exams.RoomSplitWeight).
 *  <li>Room penalty (total of room penalties {@link ExamPlacement#getRoomPenalty()} of all assigned exams,
 *  weighted by Exams.RoomWeight).
 *  <li>Distribution penalty (total of distribution constraint weights {@link ExamDistributionConstraint#getWeight()} of all 
 *  soft distribution constraints that are not satisfied, i.e., {@link ExamDistributionConstraint#isSatisfied()} = false; 
 *  weighted by Exams.DistributionWeight).
 *  <li>Direct instructor conflicts (an instructor is enrolled in two exams that are scheduled at the same period, 
 *  weighted by Exams.InstructorDirectConflictWeight)
 *  <li>Back-to-Back instructor conflicts (an instructor is enrolled in two exams that are scheduled in consecutive 
 *  periods, weighted by Exams.InstructorBackToBackConflictWeight).
 *  If Exams.IsDayBreakBackToBack is false, there is no conflict between the last 
 *  period and the first period of consecutive days. 
 *  <li>Distance Back-to-Back instructor conflicts (same as Back-to-Back instructor conflict,
 *  but the maximum distance between rooms in which both exam take place
 *  is greater than Exams.BackToBackDistance, weighted by Exams.InstructorDistanceBackToBackConflictWeight).
 *  <li>Room split distance penalty (if an examination is assigned between two or three rooms, 
 *  distance between these rooms can be minimized using this criterion) 
 *  <li>Front load penalty (large exams can be penalized if assigned on or after a certain period) 
 * </ul>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
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

    
    private boolean iDayBreakBackToBack = false;
    private double iDirectConflictWeight = 1000.0;
    private double iMoreThanTwoADayWeight = 100.0;
    private double iBackToBackConflictWeight = 10.0;
    private double iDistanceBackToBackConflictWeight = 25.0;
    private double iPeriodWeight = 1.0;
    private double iExamRotationWeight = 0.001;
    private double iRoomSizeWeight = 0.0001;
    private double iRoomSplitWeight = 10.0;
    private double iRoomWeight = 0.1;
    private double iDistributionWeight = 1.0;
    private int iBackToBackDistance = -1; //67
    private double iInstructorDirectConflictWeight = 1000.0;
    private double iInstructorMoreThanTwoADayWeight = 100.0;
    private double iInstructorBackToBackConflictWeight = 10.0;
    private double iInstructorDistanceBackToBackConflictWeight = 25.0;
    private boolean iMPP = false;
    private double iPerturbationWeight = 0.01;
    private double iRoomSplitDistanceWeight = 0.01;
    private int iLargeSize = -1;
    private double iLargePeriod = 0.67;
    private double iLargeWeight = 1.0;

    private int iNrDirectConflicts = 0;
    private int iNrBackToBackConflicts = 0;
    private int iNrDistanceBackToBackConflicts = 0;
    private int iNrMoreThanTwoADayConflicts = 0;
    private int iRoomSizePenalty = 0;
    private int iRoomSplitPenalty = 0;
    private int iRoomSplits = 0;
    private int iRoomSplitPenalties[] = new int[] {0,0,0,0,0,0,0,0,0,0,0};
    private int iRoomPenalty = 0;
    private int iDistributionPenalty = 0;
    private int iPeriodPenalty = 0;
    private int iExamRotationPenalty = 0;
    private int iPerturbationPenalty = 0;
    private int iNrInstructorDirectConflicts = 0;
    private int iNrInstructorBackToBackConflicts = 0;
    private int iNrInstructorDistanceBackToBackConflicts = 0;
    private int iNrInstructorMoreThanTwoADayConflicts = 0;
    private int iLargePenalty = 0;
    private double iRoomSplitDistancePenalty = 0;
    private int iNrLargeExams;

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
        iRoomWeight = properties.getPropertyDouble("Exams.RoomWeight", iRoomWeight);
        iRoomSplitWeight = properties.getPropertyDouble("Exams.RoomSplitWeight", iRoomSplitWeight);
        iBackToBackDistance = properties.getPropertyInt("Exams.BackToBackDistance", iBackToBackDistance);
        iDistributionWeight = properties.getPropertyDouble("Exams.DistributionWeight", iDistributionWeight);
        iInstructorDirectConflictWeight = properties.getPropertyDouble("Exams.InstructorDirectConflictWeight", iInstructorDirectConflictWeight);
        iInstructorBackToBackConflictWeight = properties.getPropertyDouble("Exams.InstructorBackToBackConflictWeight", iInstructorBackToBackConflictWeight);
        iInstructorDistanceBackToBackConflictWeight = properties.getPropertyDouble("Exams.InstructorDistanceBackToBackConflictWeight", iInstructorDistanceBackToBackConflictWeight);
        iInstructorMoreThanTwoADayWeight = properties.getPropertyDouble("Exams.InstructorMoreThanTwoADayWeight", iInstructorMoreThanTwoADayWeight);
        iMPP = properties.getPropertyBoolean("General.MPP", iMPP);
        iPerturbationWeight = properties.getPropertyDouble("Exams.PerturbationWeight", iPerturbationWeight);
        iRoomSplitDistanceWeight = properties.getPropertyDouble("Exams.RoomSplitDistanceWeight", iRoomSplitDistanceWeight);
        iLargeSize = properties.getPropertyInt("Exams.LargeSize", iLargeSize);
        iLargePeriod = properties.getPropertyDouble("Exams.LargePeriod", iLargePeriod);
        iLargeWeight = properties.getPropertyDouble("Exams.LargeWeight", iLargeWeight);
    }
    
    /**
     * Initialization of the model
     */
    public void init() {
        iNrLargeExams = 0;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (getLargeSize()>=0 && exam.getSize()>=getLargeSize()) iNrLargeExams++;
            for (Enumeration f=exam.getRoomPlacements().elements();f.hasMoreElements();) {
                ExamRoomPlacement room = (ExamRoomPlacement)f.nextElement();
                room.getRoom().addVariable(exam);
            }
        }
        iLimits = null; iMaxDistributionPenalty = null;
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
     * @param id period unique identifier
     * @param day day (e.g., 07/12/10)
     * @param time (e.g., 8:00am-10:00am)
     * @param length length of period in minutes
     * @param penalty period penalty
     */
    public ExamPeriod addPeriod(Long id, String day, String time, int length, int penalty) {
        ExamPeriod lastPeriod = (iPeriods.isEmpty()?null:(ExamPeriod)iPeriods.lastElement());
        ExamPeriod p = new ExamPeriod(id, day, time, length, penalty);
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
    
    /** Period of given unique id */
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
     * Direct instructor conflict weight (can be set by problem property Exams.InstructorDirectConflictWeight, 
     * or in the input xml file, property instructorDirectConflictWeight)
     */
    public double getInstructorDirectConflictWeight() {
        return iInstructorDirectConflictWeight;
    }
    /**
     * Direct instructor conflict weight (can be set by problem property Exams.InstructorDirectConflictWeight, 
     * or in the input xml file, property instructorDirectConflictWeight)
     */
    public void setInstructorDirectConflictWeight(double directConflictWeight) {
        iInstructorDirectConflictWeight = directConflictWeight;
    }
    /**
     * Back-to-back instructor conflict weight (can be set by problem property Exams.InstructorBackToBackConflictWeight, 
     * or in the input xml file, property instructorBackToBackConflictWeight)
     */
    public double getInstructorBackToBackConflictWeight() {
        return iInstructorBackToBackConflictWeight;
    }
    /**
     * Back-to-back instructor conflict weight (can be set by problem property Exams.InstructorBackToBackConflictWeight, 
     * or in the input xml file, property instructorBackToBackConflictWeight)
     */
    public void setInstructorBackToBackConflictWeight(double backToBackConflictWeight) {
        iInstructorBackToBackConflictWeight = backToBackConflictWeight;
    }
    /**
     * Distance back-to-back instructor conflict weight (can be set by problem property Exams.InstructorDistanceBackToBackConflictWeight, 
     * or in the input xml file, property instructorDistanceBackToBackConflictWeight)
     */
    public double getInstructorDistanceBackToBackConflictWeight() {
        return iInstructorDistanceBackToBackConflictWeight;
    }
    /**
     * Distance back-to-back instructor conflict weight (can be set by problem property Exams.InstructorDistanceBackToBackConflictWeight, 
     * or in the input xml file, property instructorDistanceBackToBackConflictWeight)
     */
    public void setInstructorDistanceBackToBackConflictWeight(double distanceBackToBackConflictWeight) {
        iInstructorDistanceBackToBackConflictWeight = distanceBackToBackConflictWeight;
    }
    /**
     * More than two exams a day instructor conflict weight (can be set by problem 
     * property Exams.InstructorMoreThanTwoADayWeight, or in the input xml file, property instructorMoreThanTwoADayWeight)
     */
    public double getInstructorMoreThanTwoADayWeight() {
        return iInstructorMoreThanTwoADayWeight;
    }
    /**
     * More than two exams a day instructor conflict weight (can be set by problem 
     * property Exams.InstructorMoreThanTwoADayWeight, or in the input xml file, property instructorMoreThanTwoADayWeight)
     */
    public void setInstructorMoreThanTwoADayWeight(double moreThanTwoADayWeight) {
        iInstructorMoreThanTwoADayWeight = moreThanTwoADayWeight;
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
     * A weight for period penalty (used in {@link ExamPlacement#getPeriodPenalty()}, 
     * can be set by problem property Exams.PeriodWeight, or in the input xml file,
     * property periodWeight)
     * 
     */
    public double getPeriodWeight() {
        return iPeriodWeight;
    }
    /**
     * A weight for period penalty (used in {@link ExamPlacement#getPeriodPenalty()}, 
     * can be set by problem property Exams.PeriodWeight, or in the input xml file,
     * property periodWeight)
     * 
     */
    public void setPeriodWeight(double periodWeight) {
        iPeriodWeight = periodWeight;
    }
    /**
     * A weight for exam rotation penalty (used in {@link ExamPlacement#getRotationPenalty()} 
     * can be set by problem property Exams.RotationWeight, or in the input xml file,
     * property examRotationWeight)
     * 
     */
    public double getExamRotationWeight() {
        return iExamRotationWeight;
    }
    /**
     * A weight for period penalty (used in {@link ExamPlacement#getRotationPenalty()}, 
     * can be set by problem property Exams.RotationWeight, or in the input xml file,
     * property examRotationWeight)
     * 
     */
    public void setExamRotationWeight(double examRotationWeight) {
        iExamRotationWeight = examRotationWeight;
    }
    /**
     * A weight for room size penalty (used in {@link ExamPlacement#getRoomSizePenalty()}, 
     * can be set by problem property Exams.RoomSizeWeight, or in the input xml file,
     * property roomSizeWeight)
     * 
     */
    public double getRoomSizeWeight() {
        return iRoomSizeWeight;
    }
    /**
     * A weight for room size penalty (used in {@link ExamPlacement#getRoomSizePenalty()}, 
     * can be set by problem property Exams.RoomSizeWeight, or in the input xml file,
     * property roomSizeWeight)
     * 
     */
    public void setRoomSizeWeight(double roomSizeWeight) {
        iRoomSizeWeight = roomSizeWeight;
    }
    /**
     * A weight for room penalty weight (used in {@link ExamPlacement#getRoomPenalty()}, 
     * can be set by problem property Exams.RoomPreferenceWeight, or in the input xml file,
     * property roomPreferenceWeight)
     * 
     */
    public double getRoomWeight() {
        return iRoomWeight;
    }
    /**
     * A weight for room penalty weight (used in {@link ExamPlacement#getRoomPenalty()}, 
     * can be set by problem property Exams.RoomWeight, or in the input xml file,
     * property roomWeight)
     * 
     */
    public void setRoomWeight(double roomWeight) {
        iRoomWeight = roomWeight;
    }
    /**
     * A weight for room split penalty (used in {@link ExamPlacement#getRoomSplitPenalty()}, 
     * can be set by problem property Exams.RoomSplitWeight, or in the input xml file,
     * property roomSplitWeight)
     * 
     */
    public double getRoomSplitWeight() {
        return iRoomSplitWeight;
    }
    /**
     * A weight for room split penalty (used in {@link ExamPlacement#getRoomSplitPenalty()}, 
     * can be set by problem property Exams.RoomSplitWeight, or in the input xml file,
     * property roomSplitWeight)
     * 
     */
    public void setRoomSplitWeight(double roomSplitWeight) {
        iRoomSplitWeight = roomSplitWeight;
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
     * A weight of violated distribution soft constraints (see {@link ExamDistributionConstraint}, 
     * can be set by problem property Exams.RoomDistributionWeight, or in the input xml file,
     * property roomDistributionWeight)
     */
    public double getDistributionWeight() {
        return iDistributionWeight;
    }
    /**
     * A weight of violated distribution soft constraints (see {@link ExamDistributionConstraint}, 
     * can be set by problem property Exams.RoomDistributionWeight, or in the input xml file,
     * property roomDistributionWeight)
     * 
     */
    public void setDistributionWeight(double distributionWeight) {
        iDistributionWeight = distributionWeight;
    }
    
    /**
     * A weight of perturbations (see {@link ExamPlacement#getPerturbationPenalty()}), i.e., 
     * a penalty for an assignment of an exam to a place different from the initial one. 
     * Can by set by problem property Exams.PerturbationWeight, or in the input xml file, property perturbationWeight)
     */
    public double getPerturbationWeight() {
        return iPerturbationWeight;
    }
    
    /**
     * A weight of perturbations (see {@link ExamPlacement#getPerturbationPenalty()}), i.e., 
     * a penalty for an assignment of an exam to a place different from the initial one. 
     * Can by set by problem property Exams.PerturbationWeight, or in the input xml file, property perturbationWeight)
     */
    public void setPerturbationWeight(double perturbationWeight) {
        iPerturbationWeight = perturbationWeight;
    }
    
    /**
     * A weight for distance between two or more rooms into which an exam is split.
     * Can by set by problem property Exams.RoomSplitDistanceWeight, or in the input xml file, property roomSplitDistanceWeight)
     **/
    public double getRoomSplitDistanceWeight() {
        return iRoomSplitDistanceWeight;
    }
    
    /**
     * A weight for distance between two or more rooms into which an exam is split.
     * Can by set by problem property Exams.RoomSplitDistanceWeight, or in the input xml file, property roomSplitDistanceWeight)
     **/
    public void setRoomSplitDistanceWeight(double roomSplitDistanceWeight) {
        iRoomSplitDistanceWeight = roomSplitDistanceWeight;
    }
    
    /**
     * An exam is considered large, if its size is greater or equal to this large size. Value -1 means all exams are small.
     * Can by set by problem property Exams.LargeSize, or in the input xml file, property largeSize)
     **/
    public int getLargeSize() {
        return iLargeSize;
    }
    
    /**
     * An exam is considered large, if its size is greater or equal to this large size. Value -1 means all exams are small.
     * Can by set by problem property Exams.LargeSize, or in the input xml file, property largeSize)
     **/
    public void setLargeSize(int largeSize) {
        iLargeSize = largeSize;
    }
    
    /**
     * Period index (number of periods multiplied by this number) for front load criteria for large exams
     * Can by set by problem property Exams.LargePeriod, or in the input xml file, property largePeriod)
     **/
    public double getLargePeriod() {
        return iLargePeriod;
    }

    /**
     * Period index (number of periods multiplied by this number) for front load criteria for large exams
     * Can by set by problem property Exams.LargePeriod, or in the input xml file, property largePeriod)
     **/
    public void setLargePeriod(double largePeriod) {
        iLargePeriod = largePeriod;
    }

    /**
     * Weight of front load criteria, i.e., a weight for assigning a large exam after large period
     * Can by set by problem property Exams.LargeWeight, or in the input xml file, property largeWeight)
     **/
    public double getLargeWeight() {
        return iLargeWeight;
    }
    
    /**
     * Weight of front load criteria, i.e., a weight for assigning a large exam after large period
     * Can by set by problem property Exams.LargeWeight, or in the input xml file, property largeWeight)
     **/
    public void setLargeWeight(double largeWeight) {
        iLargeWeight = largeWeight;
    }
    
    /** Called before a value is unassigned from its variable, optimization criteria are updated */
    public void beforeUnassigned(long iteration, Value value) {
        super.beforeUnassigned(iteration, value);
        ExamPlacement placement = (ExamPlacement)value;
        Exam exam = (Exam)placement.variable();
        iNrDirectConflicts -= placement.getNrDirectConflicts();
        iNrBackToBackConflicts -= placement.getNrBackToBackConflicts();
        iNrMoreThanTwoADayConflicts -= placement.getNrMoreThanTwoADayConflicts();
        iRoomSizePenalty -= placement.getRoomSizePenalty();
        iNrDistanceBackToBackConflicts -= placement.getNrDistanceBackToBackConflicts();
        iRoomSplitPenalty -= placement.getRoomSplitPenalty();
        iRoomSplitPenalties[placement.getRoomPlacements().size()]--;
        iPeriodPenalty -= placement.getPeriodPenalty();
        iExamRotationPenalty -= placement.getRotationPenalty();
        iRoomPenalty -= placement.getRoomPenalty();
        iNrInstructorDirectConflicts -= placement.getNrInstructorDirectConflicts();
        iNrInstructorBackToBackConflicts -= placement.getNrInstructorBackToBackConflicts();
        iNrInstructorMoreThanTwoADayConflicts -= placement.getNrInstructorMoreThanTwoADayConflicts();
        iNrInstructorDistanceBackToBackConflicts -= placement.getNrInstructorDistanceBackToBackConflicts();
        iPerturbationPenalty -= placement.getPerturbationPenalty();
        iRoomSplitDistancePenalty -= placement.getRoomSplitDistancePenalty();
        iLargePenalty -= placement.getLargePenalty();
        if (placement.getRoomPlacements().size()>1) iRoomSplits--;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) 
            ((ExamStudent)e.nextElement()).afterUnassigned(iteration, value);
        for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) 
            ((ExamInstructor)e.nextElement()).afterUnassigned(iteration, value);
        for (Iterator i=placement.getRoomPlacements().iterator();i.hasNext();)
            ((ExamRoomPlacement)i.next()).getRoom().afterUnassigned(iteration, value);
    }
    
    /** Called after a value is assigned to its variable, optimization criteria are updated */
    public void afterAssigned(long iteration, Value value) {
        super.afterAssigned(iteration, value);
        ExamPlacement placement = (ExamPlacement)value;
        Exam exam = (Exam)placement.variable();
        iNrDirectConflicts += placement.getNrDirectConflicts();
        iNrBackToBackConflicts += placement.getNrBackToBackConflicts();
        iNrMoreThanTwoADayConflicts += placement.getNrMoreThanTwoADayConflicts();
        iRoomSizePenalty += placement.getRoomSizePenalty();
        iNrDistanceBackToBackConflicts += placement.getNrDistanceBackToBackConflicts();
        iRoomSplitPenalty += placement.getRoomSplitPenalty();
        iRoomSplitPenalties[placement.getRoomPlacements().size()]++;
        iPeriodPenalty += placement.getPeriodPenalty();
        iExamRotationPenalty += placement.getRotationPenalty();
        iRoomPenalty += placement.getRoomPenalty();
        iNrInstructorDirectConflicts += placement.getNrInstructorDirectConflicts();
        iNrInstructorBackToBackConflicts += placement.getNrInstructorBackToBackConflicts();
        iNrInstructorMoreThanTwoADayConflicts += placement.getNrInstructorMoreThanTwoADayConflicts();
        iNrInstructorDistanceBackToBackConflicts += placement.getNrInstructorDistanceBackToBackConflicts();
        iPerturbationPenalty += placement.getPerturbationPenalty();
        iRoomSplitDistancePenalty += placement.getRoomSplitDistancePenalty();
        iLargePenalty += placement.getLargePenalty();
        if (placement.getRoomPlacements().size()>1) iRoomSplits++;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) 
            ((ExamStudent)e.nextElement()).afterAssigned(iteration, value);
        for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) 
            ((ExamInstructor)e.nextElement()).afterAssigned(iteration, value);
        for (Iterator i=placement.getRoomPlacements().iterator();i.hasNext();)
            ((ExamRoomPlacement)i.next()).getRoom().afterAssigned(iteration, value);
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
     *  <li>Period penalty (total of period penalties {@link ExamPlacement#getPeriodPenalty()} of all assigned exams,
     *  weighted by Exams.PeriodWeight).
     *  <li>Room size penalty (total of room size penalties {@link ExamPlacement#getRoomSizePenalty()} of all assigned exams,
     *  weighted by Exams.RoomSizeWeight).
     *  <li>Room split penalty (total of room split penalties {@link ExamPlacement#getRoomSplitPenalty()}
     *  of all assigned exams, weighted by Exams.RoomSplitWeight).
     *  <li>Room split distance penalty {@link ExamPlacement#getRoomSplitDistancePenalty()}, 
     *  of all assigned exams, weighted by {@link ExamModel#getRoomSplitDistanceWeight()}
     *  <li>Room penalty (total of room penalties {@link ExamPlacement#getRoomPenalty()}
     *  of all assigned exams, weighted by Exams.RoomWeight).
     *  <li>Distribution penalty (total of room split penalties {@link ExamDistributionConstraint#getWeight()}
     *  of all soft violated distribution constraints, weighted by Exams.DistributionWeight).
     *  <li>Direct instructor conflicts (an instructor is enrolled in two exams that are 
     *  scheduled at the same period, weighted by Exams.InstructorDirectConflictWeight)
     *  <li>Back-to-Back instructor conflicts (an instructor is enrolled in two exams that
     *  are scheduled in consecutive periods, weighted by Exams.InstructorBackToBackConflictWeight).
     *  If Exams.IsDayBreakBackToBack is false, there is no conflict between the last 
     *  period and the first period of consecutive days. 
     *  <li>Distance Back-to-Back instructor conflicts (same as Back-to-Back instructor conflict,
     *  but the maximum distance between rooms in which both exam take place
     *  is greater than Exams.BackToBackDistance, weighted by Exams.InstructorDistanceBackToBackConflictWeight).
     *  <li>More than two exams a day (an instructor is enrolled in three exams that are
     *  scheduled at the same day, weighted by Exams.InstructorMoreThanTwoADayWeight).
     *  <li>Perturbation penalty (total of period penalties {@link ExamPlacement#getPerturbationPenalty()} of all assigned exams,
     *  weighted by Exams.PerturbationWeight).
     *  <li> Front load penalty {@link ExamPlacement#getLargePenalty()} of all assigned exams, weighted by Exam.LargeWeight
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
            getRoomWeight()*getRoomPenalty(false)+
            getDistributionWeight()*getDistributionPenalty(false)+
            getInstructorDirectConflictWeight()*getNrInstructorDirectConflicts(false)+
            getInstructorMoreThanTwoADayWeight()*getNrInstructorMoreThanTwoADayConflicts(false)+
            getInstructorBackToBackConflictWeight()*getNrInstructorBackToBackConflicts(false)+
            getInstructorDistanceBackToBackConflictWeight()*getNrInstructorDistanceBackToBackConflicts(false)+
            getExamRotationWeight()*getExamRotationPenalty(false)+
            getPerturbationWeight()*getPerturbationPenalty(false)+
            getRoomSplitDistanceWeight()*getRoomSplitDistancePenalty(false)+
            getLargeWeight()*getLargePenalty(false);
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
     *  <li>Period penalty (total of period penalties {@link ExamPlacement#getPeriodPenalty()} of all assigned exams,
     *  weighted by Exams.PeriodWeight).
     *  <li>Room size penalty (total of room size penalties {@link ExamPlacement#getRoomSizePenalty()} of all assigned exams,
     *  weighted by Exams.RoomSizeWeight).
     *  <li>Room split penalty (total of room split penalties {@link ExamPlacement#getRoomSplitPenalty()}
     *  of all assigned exams, weighted by Exams.RoomSplitWeight).
     *  <li>Room split distance penalty {@link ExamPlacement#getRoomSplitDistancePenalty()}, 
     *  of all assigned exams, weighted by {@link ExamModel#getRoomSplitDistanceWeight()}
     *  <li>Room penalty (total of room penalties {@link ExamPlacement#getRoomPenalty()}
     *  of all assigned exams, weighted by Exams.RoomWeight).
     *  <li>Distribution penalty (total of room split penalties {@link ExamDistributionConstraint#getWeight()}
     *  of all soft violated distribution constraints, weighted by Exams.DistributionWeight).
     *  <li>Direct instructor conflicts (an instructor is enrolled in two exams that are 
     *  scheduled at the same period, weighted by Exams.InstructorDirectConflictWeight)
     *  <li>Back-to-Back instructor conflicts (an instructor is enrolled in two exams that
     *  are scheduled in consecutive periods, weighted by Exams.InstructorBackToBackConflictWeight).
     *  If Exams.IsDayBreakBackToBack is false, there is no conflict between the last 
     *  period and the first period of consecutive days. 
     *  <li>Distance Back-to-Back instructor conflicts (same as Back-to-Back instructor conflict,
     *  but the maximum distance between rooms in which both exam take place
     *  is greater than Exams.BackToBackDistance, weighted by Exams.InstructorDistanceBackToBackConflictWeight).
     *  <li>More than two exams a day (an instructor is enrolled in three exams that are
     *  scheduled at the same day, weighted by Exams.InstructorMoreThanTwoADayWeight).
     *  <li>Perturbation penalty (total of period penalties {@link ExamPlacement#getPerturbationPenalty()} of all assigned exams,
     *  weighted by Exams.PerturbationWeight).
     *  <li> Front load penalty {@link ExamPlacement#getLargePenalty()} of all assigned exams, weighted by Exam.LargeWeight
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
                getRoomSplitDistanceWeight()*getRoomSplitDistancePenalty(false),
                getRoomWeight()*getRoomPenalty(false),
                getDistributionWeight()*getDistributionPenalty(false),
                getInstructorDirectConflictWeight()*getNrInstructorDirectConflicts(false),
                getInstructorMoreThanTwoADayWeight()*getNrInstructorMoreThanTwoADayConflicts(false),
                getInstructorBackToBackConflictWeight()*getNrInstructorBackToBackConflicts(false),
                getInstructorDistanceBackToBackConflictWeight()*getNrInstructorDistanceBackToBackConflicts(false),
                getExamRotationWeight()*getExamRotationPenalty(false),
                getPerturbationWeight()*getPerturbationPenalty(false),
                getLargeWeight()*getLargePenalty(false)
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
            "@P:"+getExamRotationPenalty(false)+","+
            "RSz:"+getRoomSizePenalty(false)+","+
            "RSp:"+getRoomSplitPenalty(false)+","+
            "RD:"+sDoubleFormat.format(getRoomSplitDistancePenalty(false))+","+
            "RP:"+getRoomPenalty(false)+","+
            "DP:"+getDistributionPenalty(false)+
            (getLargeSize()>=0?",LP:"+getLargePenalty(false):"")+
            (isMPP()?",IP:"+getPerturbationPenalty(false):"");
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
                if (!student.isAvailable(period)) conflicts += nrExams;
                else if (nrExams>1) conflicts += nrExams-1;
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
     * Return number of direct instructor conflicts, i.e., the total number of cases where an instructor is enrolled
     * into two exams that are scheduled at the same period.
     * @param precise if false, the cached value is used
     * @return number of direct instructor conflicts
     */
    public int getNrInstructorDirectConflicts(boolean precise) {
        if (!precise) return iNrInstructorDirectConflicts;
        int conflicts = 0;
        for (Enumeration e=getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor instructor = (ExamInstructor)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                int nrExams = instructor.getExams(period).size();
                if (!instructor.isAvailable(period)) conflicts += nrExams;
                else if (nrExams>1) conflicts += nrExams-1;
            }
        }
        return conflicts;
    }
    
    /**
     * Return number of back-to-back instructor conflicts, i.e., the total number of cases where an instructor is enrolled
     * into two exams that are scheduled at consecutive periods. If {@link ExamModel#isDayBreakBackToBack()} is false,
     * the last period of one day and the first period of the following day are not considered as consecutive periods.
     * @param precise if false, the cached value is used
     * @return number of back-to-back instructor conflicts
     */
    public int getNrInstructorBackToBackConflicts(boolean precise) {
        if (!precise) return iNrInstructorBackToBackConflicts;
        int conflicts = 0;
        for (Enumeration e=getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor instructor = (ExamInstructor)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                int nrExams = instructor.getExams(period).size();
                if (nrExams==0) continue;
                if (period.next()!=null && !instructor.getExams(period.next()).isEmpty() && (isDayBreakBackToBack() || period.next().getDay()==period.getDay())) 
                    conflicts += nrExams*instructor.getExams(period.next()).size();
            }
        }
        return conflicts;
    }
    
    /**
     * Return number of distance back-to-back instructor conflicts, i.e., the total number of back-to-back instructor conflicts
     * where the two exam take place in rooms that are too far a part (i.e., {@link ExamPlacement#getDistance(ExamPlacement)} is
     * greater than {@link ExamModel#getBackToBackDistance()}).
     * @param precise if false, the cached value is used
     * @return number of distance back-to-back student conflicts
     */
    public int getNrInstructorDistanceBackToBackConflicts(boolean precise) {
        if (getBackToBackDistance()<0) return 0;
        if (!precise) return iNrInstructorDistanceBackToBackConflicts;
        int conflicts = 0;
        for (Enumeration e=getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor instructor = (ExamInstructor)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                Set exams = instructor.getExams(period);
                if (exams.isEmpty()) continue;
                if (period.next()!=null && !instructor.getExams(period.next()).isEmpty() && period.next().getDay()==period.getDay()) {
                    for (Iterator i1=exams.iterator();i1.hasNext();) {
                        Exam x1 = (Exam)i1.next();
                        ExamPlacement p1 =(ExamPlacement)x1.getAssignment();
                        for (Iterator i2=instructor.getExams(period.next()).iterator();i2.hasNext();) {
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
     * Return number of more than two exams a day instructor conflicts, i.e., the total number of cases where an instructor 
     * is enrolled into three exams that are scheduled at the same day (i.e., {@link ExamPeriod#getDay()} is the same).
     * @param precise if false, the cached value is used
     * @return number of more than two exams a day student conflicts
     */
    public int getNrInstructorMoreThanTwoADayConflicts(boolean precise) {
        if (!precise) return iNrInstructorMoreThanTwoADayConflicts;
        int conflicts = 0;
        for (Enumeration e=getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor instructor = (ExamInstructor)e.nextElement();
            for (int d=0;d<getNrDays();d++) {
                int nrExams = instructor.getExamsADay(d).size();
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
     * Return total room (weight) penalty, i.e., the sum of {@link ExamPlacement#getRoomPenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total room penalty
     */
    public int getRoomPenalty(boolean precise) {
        if (!precise) return iRoomPenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomPenalty();
        }
        return penalty;
    }

    /**
     * Return total distribution penalty, i.e., the sum of {@link ExamDistributionConstraint#getWeight()} of all
     * violated soft distribution constraints.
     * @param precise if false, the cached value is used
     * @return total distribution penalty
     */
    public int getDistributionPenalty(boolean precise) {
        if (!precise) return iDistributionPenalty;
        int penalty = 0;
        for (Enumeration e=getDistributionConstraints().elements();e.hasMoreElements();) {
            ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
            if (!dc.isSatisfied())
                penalty += dc.getWeight();
        }
        return penalty;
    }
    
    /**
     * Return total room split distance penalty, i.e., the sum of {@link ExamPlacement#getRoomSplitDistancePenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total room split distance penalty
     */
    public double getRoomSplitDistancePenalty(boolean precise) {
        if (!precise) return iRoomSplitDistancePenalty;
        double penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomSplitDistancePenalty();
        }
        return penalty;
    }
    
    /**
     * Count exam placements with a room split.
     * @param precise if false, the cached value is used
     * @return total number of exams that are assigned into two or more rooms
     */
    public double getNrRoomSplits(boolean precise) {
        if (!precise) return iRoomSplits;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += (((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomPlacements().size()>1?1:0);
        }
        return penalty;
    }


    /** To be called by soft {@link ExamDistributionConstraint} when satisfaction changes. */
    protected void addDistributionPenalty(int inc) {
        iDistributionPenalty += inc;
    }
    
    private Integer iMaxDistributionPenalty = null;
    private int getMaxDistributionPenalty() {
        if (iMaxDistributionPenalty==null) {
            int maxDistributionPenalty = 0;
            for (Enumeration e=getDistributionConstraints().elements();e.hasMoreElements();) {
                ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
                if (dc.isHard()) continue;
                maxDistributionPenalty += dc.getWeight();
            }
            iMaxDistributionPenalty = new Integer(maxDistributionPenalty);
        }
        return iMaxDistributionPenalty.intValue();
    }
    
    private int[] iLimits = null;
    private int getMinPenalty(ExamRoom r) {
        boolean av = false; int min = Integer.MAX_VALUE;
        for (Enumeration e=getPeriods().elements();e.hasMoreElements();) {
            ExamPeriod p = (ExamPeriod)e.nextElement();
            if (r.isAvailable(p)) {
                av=true;
                min = Math.min(min, r.getPenalty(p));
            }
        }
        return min;
    }
    private int getMaxPenalty(ExamRoom r) {
        boolean av = false; int max = Integer.MIN_VALUE;
        for (Enumeration e=getPeriods().elements();e.hasMoreElements();) {
            ExamPeriod p = (ExamPeriod)e.nextElement();
            if (r.isAvailable(p)) {
                av=true;
                max = Math.max(max, r.getPenalty(p));
            }
        }
        return max;
    }
    private int[] getLimits() {
        if (iLimits==null) {
            int minPeriodPenalty = 0, maxPeriodPenalty = 0;
            int minRoomPenalty = 0, maxRoomPenalty = 0;
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                if (!exam.getPeriodPlacements().isEmpty()) {
                    int minPenalty = Integer.MAX_VALUE, maxPenalty = Integer.MIN_VALUE;
                    for (Enumeration f=exam.getPeriodPlacements().elements();f.hasMoreElements();) {
                        ExamPeriodPlacement periodPlacement = (ExamPeriodPlacement)f.nextElement();
                        minPenalty = Math.min(minPenalty, periodPlacement.getPenalty());
                        maxPenalty = Math.max(maxPenalty, periodPlacement.getPenalty());
                    }
                    minPeriodPenalty += minPenalty;
                    maxPeriodPenalty += maxPenalty;
                }
                if (!exam.getRoomPlacements().isEmpty()) {
                    int minPenalty = Integer.MAX_VALUE, maxPenalty = Integer.MIN_VALUE;
                    for (Enumeration f=exam.getRoomPlacements().elements();f.hasMoreElements();) {
                        ExamRoomPlacement roomPlacement = (ExamRoomPlacement)f.nextElement();
                        minPenalty = Math.min(minPenalty, roomPlacement.getPenalty()+getMinPenalty(roomPlacement.getRoom()));
                        maxPenalty = Math.max(maxPenalty, roomPlacement.getPenalty()+getMaxPenalty(roomPlacement.getRoom()));
                    }
                    minRoomPenalty += minPenalty;
                    maxRoomPenalty += maxPenalty;
                }
            }
            iLimits = new int[] {minPeriodPenalty,maxPeriodPenalty,minRoomPenalty,maxRoomPenalty};
        }
        return iLimits;
    }
    
    /**
     * Return total perturbation penalty, i.e., the sum of {@link ExamPlacement#getPerturbationPenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total period penalty
     */
    public int getPerturbationPenalty(boolean precise) {
        if (!precise) return iPerturbationPenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getPerturbationPenalty();
        }
        return penalty;
    }
    
    /**
     * Return total front load penalty, i.e., the sum of {@link ExamPlacement#getLargePenalty()} of all
     * assigned placements.
     * @param precise if false, the cached value is used
     * @return total period penalty
     */
    public int getLargePenalty(boolean precise) {
        if (!precise) return iLargePenalty;
        int penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getLargePenalty();
        }
        return penalty;
    }
    
    /**
     * Info table
     */
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("Direct Conflicts",String.valueOf(getNrDirectConflicts(false)));
        info.put("More Than 2 A Day Conflicts",String.valueOf(getNrMoreThanTwoADayConflicts(false)));
        info.put("Back-To-Back Conflicts",String.valueOf(getNrBackToBackConflicts(false)));
        if (getNrInstructorDirectConflicts(false)>0)
            info.put("Instructor Direct Conflicts",String.valueOf(getNrInstructorDirectConflicts(false)));
        if (getNrInstructorMoreThanTwoADayConflicts(false)>0)
            info.put("Instructor More Than 2 A Day Conflicts",String.valueOf(getNrInstructorMoreThanTwoADayConflicts(false)));
        if (getNrInstructorBackToBackConflicts(false)>0)
            info.put("Instructor Back-To-Back Conflicts",String.valueOf(getNrInstructorBackToBackConflicts(false)));
        if (getBackToBackDistance()>=0 && getNrDistanceBackToBackConflicts(false)>0)
            info.put("Distance Back-To-Back Conflicts",String.valueOf(getNrDistanceBackToBackConflicts(false)));
        if (getBackToBackDistance()>=0 && getNrInstructorDistanceBackToBackConflicts(false)>0)
            info.put("Instructor Distance Back-To-Back Conflicts",String.valueOf(getNrInstructorDistanceBackToBackConflicts(false)));
        if (nrAssignedVariables()>0 && getRoomSizePenalty(false)>0)
            info.put("Room Size Penalty", sDoubleFormat.format(((double)getRoomSizePenalty(false))/nrAssignedVariables()));
        if (getRoomSplitPenalty(false)>0) {
            String split = "";
            for (int i=2;i<getMaxRooms();i++)
                if (iRoomSplitPenalties[i]>0) {
                    if (split.length()>0) split+=", ";
                    split+=iRoomSplitPenalties[i]+"&times;"+i;
                }
            info.put("Room Split Penalty", getRoomSplitPenalty(false)+" ("+split+")");
        }
        info.put("Period Penalty", getPerc(getPeriodPenalty(false), getLimits()[0], getLimits()[1])+"% ("+getPeriodPenalty(false)+")");
        info.put("Room Penalty", getPerc(getRoomPenalty(false), getLimits()[2], getLimits()[3])+"% ("+getRoomPenalty(false)+")");
        info.put("Distribution Penalty", getPerc(getDistributionPenalty(false), 0, getMaxDistributionPenalty())+"% ("+getDistributionPenalty(false)+")");
        if (getNrRoomSplits(false)>0)
            info.put("Room Split Distance Penalty", sDoubleFormat.format(getRoomSplitDistancePenalty(false)/getNrRoomSplits(false))); 
        if (getExamRotationPenalty(false)>0) 
            info.put("Exam Rotation Penalty",String.valueOf(getExamRotationPenalty(false)));
        if (isMPP())
            info.put("Perturbation Penalty", sDoubleFormat.format(((double)getPerturbationPenalty(false))/nrAssignedVariables()));
        if (getLargeSize()>=0)
            info.put("Large Exams Penalty", getPerc(getLargePenalty(false), 0, iNrLargeExams)+"% ("+getLargePenalty(false)+")");
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
        info.put("Instructor Direct Conflicts [p]",String.valueOf(getNrInstructorDirectConflicts(true)));
        info.put("Instructor More Than 2 A Day Conflicts [p]",String.valueOf(getNrInstructorMoreThanTwoADayConflicts(true)));
        info.put("Instructor Back-To-Back Conflicts [p]",String.valueOf(getNrInstructorBackToBackConflicts(true)));
        info.put("Instructor Distance Back-To-Back Conflicts [p]",String.valueOf(getNrInstructorDistanceBackToBackConflicts(true)));
        info.put("Room Size Penalty [p]",String.valueOf(getRoomSizePenalty(true)));
        info.put("Room Split Penalty [p]",String.valueOf(getRoomSplitPenalty(true)));
        info.put("Period Penalty [p]",String.valueOf(getPeriodPenalty(true)));
        info.put("Room Penalty [p]",String.valueOf(getRoomPenalty(true)));
        info.put("Distribution Penalty [p]",String.valueOf(getDistributionPenalty(true)));
        info.put("Perturbation Penalty [p]", String.valueOf(getPerturbationPenalty(true)));
        info.put("Room Split Distance Penalty [p]", sDoubleFormat.format(getRoomSplitDistancePenalty(true))+" / "+getNrRoomSplits(true)); 
        info.put("Number of Periods",String.valueOf(getPeriods().size()));
        info.put("Number of Exams",String.valueOf(variables().size()));
        info.put("Number of Rooms",String.valueOf(getRooms().size()));
        int avail = 0, availAlt = 0;
        for (Enumeration e=getRooms().elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            for (Enumeration g=getPeriods().elements();g.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)g.nextElement();
                if (room.isAvailable(period)) {
                    avail+=room.getSize();
                    availAlt+=room.getAltSize();
                }
            }
        }
        info.put("Number of Students",String.valueOf(getStudents().size()));
        int nrStudentExams = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            nrStudentExams+=student.getOwners().size();
        }
        info.put("Number of Student Exams",String.valueOf(nrStudentExams));
        int nrAltExams = 0, nrOrigRoomExams = 0, nrSmallExams = 0;
        double altRatio = ((double)avail)/availAlt;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (exam.hasAltSeating()) nrAltExams++;
            if (exam.getMaxRooms()==0) nrSmallExams++;
        }
        info.put("Number of Exams Requiring Alt Seating",String.valueOf(nrAltExams));
        info.put("Number of Small Exams (Exams W/O Room)",String.valueOf(nrSmallExams));
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
        boolean saveBest = getProperties().getPropertyBoolean("Xml.SaveBest", true);
        boolean saveSolution = getProperties().getPropertyBoolean("Xml.SaveSolution", true);
        boolean saveConflictTable = getProperties().getPropertyBoolean("Xml.SaveConflictTable", false);
        boolean saveParams = getProperties().getPropertyBoolean("Xml.SaveParameters",true);
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
        if (saveParams) {
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
            params.addElement("property").addAttribute("name", "roomWeight").addAttribute("value", String.valueOf(getRoomWeight()));
            params.addElement("property").addAttribute("name", "distributionWeight").addAttribute("value", String.valueOf(getDistributionWeight()));
            params.addElement("property").addAttribute("name", "instructorDirectConflictWeight").addAttribute("value", String.valueOf(getInstructorDirectConflictWeight()));
            params.addElement("property").addAttribute("name", "instructorMoreThanTwoADayWeight").addAttribute("value", String.valueOf(getInstructorMoreThanTwoADayWeight()));
            params.addElement("property").addAttribute("name", "instructorBackToBackConflictWeight").addAttribute("value", String.valueOf(getInstructorBackToBackConflictWeight()));
            params.addElement("property").addAttribute("name", "instructorDistanceBackToBackConflictWeight").addAttribute("value", String.valueOf(getInstructorDistanceBackToBackConflictWeight()));
            params.addElement("property").addAttribute("name", "perturbationWeight").addAttribute("value", String.valueOf(getPerturbationWeight()));
            params.addElement("property").addAttribute("name", "roomSplitDistanceWeight").addAttribute("value", String.valueOf(getRoomSplitDistanceWeight()));
            params.addElement("property").addAttribute("name", "largeSize").addAttribute("value", String.valueOf(getLargeSize()));
            params.addElement("property").addAttribute("name", "largePeriod").addAttribute("value", String.valueOf(getLargePeriod()));
            params.addElement("property").addAttribute("name", "largeWeight").addAttribute("value", String.valueOf(getLargeWeight()));
        }
        Element periods = root.addElement("periods");
        for (Enumeration e=getPeriods().elements();e.hasMoreElements();) {
            ExamPeriod period = (ExamPeriod)e.nextElement();
            periods.addElement("period").
                addAttribute("id", String.valueOf(period.getId())).
                addAttribute("length", String.valueOf(period.getLength())).
                addAttribute("day", period.getDayStr()).
                addAttribute("time", period.getTimeStr()).
                addAttribute("penalty", String.valueOf(period.getPenalty()));
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
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                if (!room.isAvailable(period))
                    r.addElement("period").addAttribute("id", String.valueOf(period.getId())).addAttribute("available", "false");
                else if (room.getPenalty(period)!=0)
                    r.addElement("period").addAttribute("id", String.valueOf(period.getId())).addAttribute("penalty", String.valueOf(room.getPenalty(period)));
            }
        }
        Element exams = root.addElement("exams");
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            Element ex = exams.addElement("exam");
            ex.addAttribute("id", String.valueOf(exam.getId()));
            if (exam.hasName())
                ex.addAttribute("name", exam.getName());
            ex.addAttribute("length", String.valueOf(exam.getLength()));
            if (exam.getMinSize()!=0)
                ex.addAttribute("minSize", String.valueOf(exam.getMinSize()));
            ex.addAttribute("alt", (exam.hasAltSeating()?"true":"false"));
            if (exam.getMaxRooms()!=getMaxRooms())
                ex.addAttribute("maxRooms", String.valueOf(exam.getMaxRooms()));
            ex.addAttribute("enrl", String.valueOf(exam.getStudents().size()));
            for (Enumeration f=exam.getOwners().elements();f.hasMoreElements();) {
                ExamOwner owner = (ExamOwner)f.nextElement();
                ex.addElement("owner").addAttribute("id", String.valueOf(owner.getId())).addAttribute("name", owner.getName());
            }
            for (Enumeration f=exam.getPeriodPlacements().elements();f.hasMoreElements();) {
                ExamPeriodPlacement period = (ExamPeriodPlacement)f.nextElement();
                Element pe = ex.addElement("period"). addAttribute("id", String.valueOf(period.getId()));
                int penalty = period.getPenalty()-period.getPeriod().getPenalty();
                if (penalty!=0)
                    pe.addAttribute("penalty", String.valueOf(penalty));
            }
            for (Iterator j=exam.getRoomPlacements().iterator();j.hasNext();) {
                ExamRoomPlacement room = (ExamRoomPlacement)j.next();
                Element re = ex.addElement("room").addAttribute("id", String.valueOf(room.getId()));
                if (room.getPenalty()!=0) re.addAttribute("penalty", String.valueOf(room.getPenalty()));
                if (room.getMaxPenalty()!=100) re.addAttribute("maxPenalty", String.valueOf(room.getMaxPenalty()));
            }
            if (exam.hasAveragePeriod())
                ex.addAttribute("average", String.valueOf(exam.getAveragePeriod()));
            ExamPlacement p = (ExamPlacement)exam.getAssignment();
            if (p!=null && saveSolution) {
                Element asg = ex.addElement("assignment");
                asg.addElement("period").addAttribute("id", String.valueOf(p.getPeriod().getId()));
                for (Iterator i=p.getRoomPlacements().iterator();i.hasNext();) {
                    ExamRoomPlacement r = (ExamRoomPlacement)i.next();
                    asg.addElement("room").addAttribute("id", String.valueOf(r.getId()));
                }            }
            p = (ExamPlacement)exam.getInitialAssignment();
            if (p!=null && saveInitial) {
                Element ini = ex.addElement("initial");
                ini.addElement("period").addAttribute("id", String.valueOf(p.getPeriod().getId()));
                for (Iterator i=p.getRoomPlacements().iterator();i.hasNext();) {
                    ExamRoomPlacement r = (ExamRoomPlacement)i.next();
                    ini.addElement("room").addAttribute("id", String.valueOf(r.getId()));
                }
            }
            p = (ExamPlacement)exam.getBestAssignment();
            if (p!=null && saveInitial) {
                Element ini = ex.addElement("best");
                ini.addElement("period").addAttribute("id", String.valueOf(p.getPeriod().getId()));
                for (Iterator i=p.getRoomPlacements().iterator();i.hasNext();) {
                    ExamRoomPlacement r = (ExamRoomPlacement)i.next();
                    ini.addElement("room").addAttribute("id", String.valueOf(r.getId()));
                }
            }
        }
        Element students = root.addElement("students");
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            Element s = students.addElement("student");
            s.addAttribute("id", String.valueOf(student.getId()));
            for (Enumeration f=student.variables().elements();f.hasMoreElements();) {
                Exam ex = (Exam)f.nextElement();
                Element x = s.addElement("exam").addAttribute("id", String.valueOf(ex.getId()));
                for (Enumeration g=ex.getOwners(student).elements();g.hasMoreElements();) {
                    ExamOwner owner = (ExamOwner)g.nextElement();
                    x.addElement("owner").addAttribute("id", String.valueOf(owner.getId()));
                }
            }
            String available = "";
            boolean allAvail = true;
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                available += student.isAvailable(period)?"1":"0";
                if (!student.isAvailable(period)) allAvail=false;
            }
            if (!allAvail) s.addAttribute("available", available);
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
                for (Enumeration g=ex.getOwners(instructor).elements();g.hasMoreElements();) {
                    ExamOwner owner = (ExamOwner)g.nextElement();
                    x.addElement("owner").addAttribute("id", String.valueOf(owner.getId()));
                }
            }
            String available = "";
            boolean allAvail = true;
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                available += instructor.isAvailable(period)?"1":"0";
                if (!instructor.isAvailable(period)) allAvail=false;
            }
            if (!allAvail) i.addAttribute("available", available);
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
        return load(document, null);
    }

    /**
     * Load model (including its solution) from XML.
     */
public boolean load(Document document, Callback saveBest) {
        boolean loadInitial = getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        boolean loadBest = getProperties().getPropertyBoolean("Xml.LoadBest", true);
        boolean loadSolution = getProperties().getPropertyBoolean("Xml.LoadSolution", true);
        boolean loadParams = getProperties().getPropertyBoolean("Xml.LoadParameters",false);
        Element root=document.getRootElement();
        if (!"examtt".equals(root.getName())) return false;
        if (root.attribute("initiative")!=null)
            getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));
        if (root.attribute("term")!=null)
            getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attribute("year")!=null)
            getProperties().setProperty("Data.Year", root.attributeValue("year"));
        if (loadParams && root.element("parameters")!=null)
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
                else if ("roomWeight".equals(name)) setRoomWeight(Double.parseDouble(value));
                else if ("distributionWeight".equals(name)) setDistributionWeight(Double.parseDouble(value));
                else if ("instructorDirectConflictWeight".equals(name)) setInstructorDirectConflictWeight(Double.parseDouble(value));
                else if ("instructorMoreThanTwoADayWeight".equals(name)) setInstructorMoreThanTwoADayWeight(Double.parseDouble(value));
                else if ("instructorBackToBackConflictWeight".equals(name)) setInstructorBackToBackConflictWeight(Double.parseDouble(value));
                else if ("instructorDistanceBackToBackConflictWeight".equals(name)) setInstructorDistanceBackToBackConflictWeight(Double.parseDouble(value));
                else if ("perturbationWeight".equals(name)) setPerturbationWeight(Double.parseDouble(value));
                else if ("roomSplitDistanceWeight".equals(name)) setRoomSplitDistanceWeight(Double.parseDouble(value));
                else if ("largeSize".equals(name)) setLargeSize(Integer.parseInt(value));
                else if ("largePeriod".equals(name)) setLargePeriod(Double.parseDouble(value));
                else if ("largeWeight".equals(name)) setLargeWeight(Double.parseDouble(value));
                else getProperties().setProperty(name, value);
            }
        for (Iterator i=root.element("periods").elementIterator("period");i.hasNext();) {
            Element e = (Element)i.next();
            addPeriod(
                    Long.valueOf(e.attributeValue("id")), 
                    e.attributeValue("day"), 
                    e.attributeValue("time"), 
                    Integer.parseInt(e.attributeValue("length")), 
                    Integer.parseInt(e.attributeValue("penalty")==null?e.attributeValue("weight","0"):e.attributeValue("penalty")));
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
            for (Iterator j=e.elementIterator("period");j.hasNext();) {
                Element pe = (Element)j.next();
                ExamPeriod period = getPeriod(Long.valueOf(pe.attributeValue("id"))); 
                if ("false".equals(pe.attributeValue("available"))) room.setAvailable(period, false);
                else room.setPenalty(period,Integer.parseInt(pe.attributeValue("penalty")));
            }
            String av = e.attributeValue("available");
            if (av!=null) {
                for (int j=0;j<getPeriods().size();j++)
                    if ('0'==av.charAt(j)) room.setAvailable((ExamPeriod)getPeriods().elementAt(j), false);
            }
            String g = e.attributeValue("groups");
            if (g!=null) {
                for (StringTokenizer s=new StringTokenizer(g,",");s.hasMoreTokens();) {
                    String gr = s.nextToken();
                    Vector roomsThisGrop = (Vector)roomGroups.get(gr);
                    if (roomsThisGrop==null) { roomsThisGrop = new Vector(); roomGroups.put(gr, roomsThisGrop); }
                    roomsThisGrop.add(room);
                }
            }
        }
        Vector assignments = new Vector();
        Hashtable exams = new Hashtable();
        Hashtable courseSections = new Hashtable();
        for (Iterator i=root.element("exams").elementIterator("exam");i.hasNext();) {
            Element e = (Element)i.next();
            Vector periodPlacements = new Vector();
            for (Iterator j=e.elementIterator("period");j.hasNext();) {
                Element pe = (Element)j.next();
                periodPlacements.add(new ExamPeriodPlacement(
                        getPeriod(Long.valueOf(pe.attributeValue("id"))),
                        Integer.parseInt(pe.attributeValue("penalty","0"))));
            }
            Vector roomPlacements = new Vector();
            for (Iterator j=e.elementIterator("room");j.hasNext();) {
                Element re = (Element)j.next();
                ExamRoomPlacement room = new ExamRoomPlacement((ExamRoom)rooms.get(Long.valueOf(re.attributeValue("id"))),
                        Integer.parseInt(re.attributeValue("penalty","0")),
                        Integer.parseInt(re.attributeValue("maxPenalty","100")));
                roomPlacements.add(room);
            }
            String g = e.attributeValue("groups");
            if (g!=null) {
                Hashtable allRooms = new Hashtable();
                for (StringTokenizer s=new StringTokenizer(g,",");s.hasMoreTokens();) {
                    String gr = s.nextToken();
                    Vector roomsThisGrop = (Vector)roomGroups.get(gr);
                    if (roomsThisGrop!=null) 
                        for (Enumeration f=roomsThisGrop.elements();f.hasMoreElements();)
                            allRooms.put((ExamRoom)f.nextElement(),new Integer(0));
                }
                for (Iterator j=e.elementIterator("original-room");j.hasNext();) {
                    allRooms.put(((ExamRoom)rooms.get(Long.valueOf(((Element)j.next()).attributeValue("id")))), new Integer(-1));
                }
                for (Iterator j=allRooms.entrySet().iterator();j.hasNext();) {
                    Map.Entry entry = (Map.Entry)j.next();
                    ExamRoomPlacement room = new ExamRoomPlacement((ExamRoom)entry.getKey(),((Integer)entry.getValue()).intValue(),100);
                    roomPlacements.add(room);
                }
                if (periodPlacements.isEmpty()) {
                    for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                        ExamPeriod p = (ExamPeriod)f.nextElement();
                        periodPlacements.add(new ExamPeriodPlacement(p,0));
                    }
                }
            }
            Exam exam = new Exam(
                    Long.parseLong(e.attributeValue("id")),
                    e.attributeValue("name"),
                    Integer.parseInt(e.attributeValue("length")),
                    "true".equals(e.attributeValue("alt")),
                    (e.attribute("maxRooms")==null?getMaxRooms():Integer.parseInt(e.attributeValue("maxRooms"))),
                    Integer.parseInt(e.attributeValue("minSize","0")),
                    periodPlacements, roomPlacements);
            exams.put(new Long(exam.getId()),exam);
            addVariable(exam);
            if (e.attribute("average")!=null) exam.setAveragePeriod(Integer.parseInt(e.attributeValue("average")));
            Element asg = e.element("assignment");
            if (asg!=null && loadSolution) {
                Element per = asg.element("period");
                if (per!=null) {
                    HashSet rp = new HashSet();
                    for (Iterator j=asg.elementIterator("room");j.hasNext();)
                        rp.add(exam.getRoomPlacement(Long.parseLong(((Element)j.next()).attributeValue("id"))));
                    ExamPlacement p = new ExamPlacement(exam, exam.getPeriodPlacement(Long.valueOf(per.attributeValue("id"))), rp);
                    assignments.add(p);
                }
            }
            Element ini = e.element("initial");
            if (ini!=null && loadInitial) {
                Element per = ini.element("period");
                if (per!=null) {
                    HashSet rp = new HashSet();
                    for (Iterator j=ini.elementIterator("room");j.hasNext();)
                        rp.add(exam.getRoomPlacement(Long.parseLong(((Element)j.next()).attributeValue("id"))));
                    ExamPlacement p = new ExamPlacement(exam, exam.getPeriodPlacement(Long.valueOf(per.attributeValue("id"))), rp);
                    exam.setInitialAssignment(p);
                }
            }
            Element best = e.element("best");
            if (best!=null && loadBest) {
                Element per = best.element("period");
                if (per!=null) {
                    HashSet rp = new HashSet();
                    for (Iterator j=best.elementIterator("room");j.hasNext();)
                        rp.add(exam.getRoomPlacement(Long.parseLong(((Element)j.next()).attributeValue("id"))));
                    ExamPlacement p = new ExamPlacement(exam, exam.getPeriodPlacement(Long.valueOf(per.attributeValue("id"))), rp);
                    exam.setBestAssignment(p);
                }
            }
            for (Iterator j=e.elementIterator("owner");j.hasNext();) {
                Element f = (Element)j.next();
                ExamOwner owner = new ExamOwner(exam,Long.parseLong(f.attributeValue("id")),f.attributeValue("name"));
                exam.getOwners().add(owner);
                courseSections.put(new Long(owner.getId()),owner);
            }
        }
        for (Iterator i=root.element("students").elementIterator("student");i.hasNext();) {
            Element e = (Element)i.next();
            ExamStudent student = new ExamStudent(this,Long.parseLong(e.attributeValue("id")));
            for (Iterator j=e.elementIterator("exam");j.hasNext();) {
                Element x = (Element)j.next();
                Exam ex = (Exam)exams.get(Long.valueOf(x.attributeValue("id")));
                student.addVariable(ex);
                for (Iterator k=x.elementIterator("owner");k.hasNext();) {
                    Element f = (Element)k.next();
                    ExamOwner owner = (ExamOwner)courseSections.get(Long.valueOf(f.attributeValue("id")));
                    student.getOwners().add(owner);
                    owner.getStudents().add(student);
                }
            }
            String available = e.attributeValue("available");
            if (available!=null)
                for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)f.nextElement();
                    if (available.charAt(period.getIndex())=='0') student.setAvailable(period.getIndex(), false);
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
                    for (Iterator k=x.elementIterator("owner");k.hasNext();) {
                        Element f = (Element)k.next();
                        ExamOwner owner = (ExamOwner)courseSections.get(Long.valueOf(f.attributeValue("id")));
                        instructor.getOwners().add(owner);
                        owner.getIntructors().add(instructor);
                    }
                }
                String available = e.attributeValue("available");
                if (available!=null)
                    for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                        ExamPeriod period = (ExamPeriod)f.nextElement();
                        if (available.charAt(period.getIndex())=='0') instructor.setAvailable(period.getIndex(), false);
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
        if (loadBest && saveBest!=null) {
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                ExamPlacement placement = (ExamPlacement)exam.getBestAssignment();
                if (placement==null) continue;
                exam.assign(0,placement);
            }
            saveBest.execute();
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                if (exam.getAssignment()!=null) exam.unassign(0);
            }
        }
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
        return true;
    }
    
    public boolean isMPP() { return iMPP;}
}
