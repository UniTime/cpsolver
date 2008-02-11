package net.sf.cpsolver.exam.model;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of an exam placement (problem value), i.e., assignment of an exam to period and room(s).
 * Each placement has defined a period and a set of rooms. The exam as well as the rooms have to be available
 * during the given period (see {@link Exam#isAvailable(ExamPeriod, Set)}). The total size of rooms 
 * have to be equal or greater than the number of students enrolled in the exam, using either
 * {@link ExamRoom#getSize()} or {@link ExamRoom#getAltSize()}, depending on {@link Exam#hasAltSeating()}.
 * Also, the number of rooms has to be smaller or equal to {@link Exam#getMaxRooms()}. If 
 * {@link Exam#getMaxRooms()} is zero, the exam is only to be assigned to period (the set of rooms is empty).
 * If the exam has a period or a set of room pre-assigned ({@link Exam#getPreAssignedPeriod()}, 
 * {@link Exam#getPreassignedRooms()}), the pre-assigned period and/or set of rooms has to be used. 
 * <br><br>
 * The cost of an assignment consists of the following criteria:
 * <ul>
 *  <li> Direct student conflicts {@link ExamPlacement#getNrDirectConflicts()}, weighted by {@link ExamModel#getDirectConflictWeight()}
 *  <li> More than two exams a day student conflicts {@link ExamPlacement#getNrMoreThanTwoADayConflicts()}, weighted by {@link ExamModel#getMoreThanTwoADayWeight()}
 *  <li> Back-to-back student conflicts {@link ExamPlacement#getNrBackToBackConflicts()}, weighted by {@link ExamModel#getBackToBackConflictWeight()}
 *  <li> Distance back-to-back student conflicts {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getDistanceBackToBackConflictWeight()}
 *  <li> Period penalty {@link ExamPlacement#getPeriodPenalty()}, weighted by {@link ExamModel#getPeriodWeight()}
 *  <li> Room size penalty {@link ExamPlacement#getRoomSizePenalty()}, weighted by {@link ExamModel#getRoomSizeWeight()}
 *  <li> Room split penalty {@link ExamPlacement#getRoomSplitPenalty()}, weighted by {@link ExamModel#getRoomSplitWeight()}
 *  <li> Non-original room penalty {@link ExamPlacement#getNotOriginalRoomPenalty()}, weighted by {@link ExamModel#getNotOriginalRoomWeight()}
 * </ul>
 * <br><br>
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
public class ExamPlacement extends Value {
    private ExamPeriod iPeriod;
    private Set iRooms;
    private int iHashCode;
    
    /**
     * Constructor
     * @param exam an exam
     * @param period a period that is available for an exam (see {@link Exam#isAvailable(ExamPeriod, Set)}
     * @param rooms a set of rooms of enough size (it is empty when {@link Exam#getMaxRooms()} is zero)
     */
    public ExamPlacement(Exam exam, ExamPeriod period, Set rooms) {
        super(exam);
        iPeriod = period;
        iRooms = rooms;
        iHashCode = getName().hashCode();
    }
    
    /**
     * Assigned period
     */
    public ExamPeriod getPeriod() { return iPeriod; }
    /**
     * Assigned rooms (it is empty when {@link Exam#getMaxRooms()} is zero)
     */
    public Set getRooms() { return iRooms; }
    
    /**
     * Number of direct student conflicts, i.e., number of cases when this exam
     * is attended by a student that attends some other exam at the same period 
     */
    public int getNrDirectConflicts() {
        Exam exam = (Exam)variable();
        //if (!exam.isAllowDirectConflicts()) return 0;
        int penalty = 0;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            Set exams = s.getExams(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam)?0:1);
            if (nrExams>1) penalty++;
        }
        return penalty;
    }
    
    /**
     * Number of back-to-back student conflicts, i.e., number of cases when this exam
     * is attended by a student that attends some other exam at the previous {@link ExamPeriod#prev()}
     * or following {@link ExamPeriod#next()} period. If {@link ExamModel#isDayBreakBackToBack()} is false,
     * back-to-back conflicts are only considered between consecutive periods that are of the 
     * same day.
     */
    public int getNrBackToBackConflicts() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        int penalty = 0;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            if (getPeriod().prev()!=null) {
                if (model.isDayBreakBackToBack() || getPeriod().prev().getDay()==getPeriod().getDay()) {
                    Set exams = s.getExams(getPeriod().prev());
                    int nrExams = exams.size() + (exams.contains(exam)?-1:0);
                    penalty += nrExams;
                }
            }
            if (getPeriod().next()!=null) {
                if (model.isDayBreakBackToBack() || getPeriod().next().getDay()==getPeriod().getDay()) {
                    Set exams = s.getExams(getPeriod().next());
                    int nrExams = exams.size() + (exams.contains(exam)?-1:0);
                    penalty += nrExams;
                }
            }
        }
        return penalty;
    }
    
    /**
     * Distance between two placements, i.e., maximal distance between a room 
     * of this placement and a room of the given placement. 
     * Method {@link ExamRoom#getDistance(ExamRoom)} is used to get a distance between two rooms.
     */
    public int getDistance(ExamPlacement other) {
        if (getRooms().isEmpty() || other.getRooms().isEmpty()) return 0;
        int maxDistance = 0;
        for (Iterator i1=getRooms().iterator();i1.hasNext();) {
            ExamRoom r1 = (ExamRoom)i1.next();
            for (Iterator i2=other.getRooms().iterator();i2.hasNext();) {
                ExamRoom r2 = (ExamRoom)i2.next();
                maxDistance = Math.max(maxDistance, r1.getDistance(r2));
            }
        }
        return maxDistance;
    }

    /**
     * Number of back-to-back distance student conflicts, i.e., number of cases when this exam
     * is attended by a student that attends some other exam at the previous {@link ExamPeriod#prev()}
     * or following {@link ExamPeriod#next()} period and the distance {@link ExamPlacement#getDistance(ExamPlacement)}
     * between these two exams is greater than {@link ExamModel#getBackToBackDistance()}. 
     * Distance back-to-back conflicts are only 
     * considered between consecutive periods that are of the 
     * same day.
     */
    public int getNrDistanceBackToBackConflicts() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        int btbDist = model.getBackToBackDistance();
        if (btbDist<0) return 0;
        int penalty = 0;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            if (getPeriod().prev()!=null) {
                if (getPeriod().prev().getDay()==getPeriod().getDay()) {
                    for (Iterator i=s.getExams(getPeriod().prev()).iterator();i.hasNext();) {
                        Exam x = (Exam)i.next();
                        if (x.equals(exam)) continue;
                        if (getDistance((ExamPlacement)x.getAssignment())>btbDist) penalty++;
                    }
                }
            }
            if (getPeriod().next()!=null) {
                if (getPeriod().next().getDay()==getPeriod().getDay()) {
                    for (Iterator i=s.getExams(getPeriod().next()).iterator();i.hasNext();) {
                        Exam x = (Exam)i.next();
                        if (x.equals(exam)) continue;
                        if (getDistance((ExamPlacement)x.getAssignment())>btbDist) penalty++;
                    }
                }
            }
        }
        return penalty;
    }
    
    /**
     * Number of more than two exams a day student conflicts, i.e., when this exam
     * is attended by a student that attends two or more other exams at the same day. 
     */
    public int getNrMoreThanTwoADayConflicts() {
        Exam exam = (Exam)variable();
        int penalty = 0;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            Set exams = s.getExamsADay(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam)?0:1);
            if (nrExams>2) penalty++;
        }
        return penalty;
    }
    
    private Integer iRoomSizePenalty = null;
    /**
     * Cost for using room(s) that are too big
     * @return difference between total room size (computed using either {@link ExamRoom#getSize()} or 
     * {@link ExamRoom#getAltSize()} based on {@link Exam#hasAltSeating()}) and the number of students
     */
    public int getRoomSizePenalty() {
        if (iRoomSizePenalty==null)
            iRoomSizePenalty = new Integer(((Exam)variable()).getRoomSizePenalty(getRooms()));
        return iRoomSizePenalty.intValue();
    }
    
    private Integer iRoomSplitPenalty = null;
    /**
     * Cost for using more than one room.
     * @return penalty (1 for 2 rooms, 2 for 3 rooms, 4 for 4 rooms, etc.)
     */
    public int getRoomSplitPenalty() {
        if (iRoomSplitPenalty==null)
            iRoomSplitPenalty = new Integer(((Exam)variable()).getRoomSplitPenalty(getRooms()));
        return iRoomSplitPenalty.intValue();
    }
    
    private Integer iNotOriginalRoomPenalty = null;
    /**
     * Cost for using room(s) different from the original room
     * @return 1 if there is an original room and it is of enough capacity, 
     * but the given set of rooms is not composed of the original room; zero otherwise
     */
    public int getNotOriginalRoomPenalty() {
        if (iNotOriginalRoomPenalty==null) 
            iNotOriginalRoomPenalty = new Integer(((Exam)variable()).getNotOriginalRoomPenalty(getRooms()));
        return iNotOriginalRoomPenalty.intValue();
    }

    private Integer iPeriodPenalty = null;
    /**
     * Cost for using a period
     * @return {@link ExamPeriod#getWeight()}
     */
    public int getPeriodPenalty() {
        if (iPeriodPenalty==null) 
            iPeriodPenalty = new Integer(((Exam)variable()).getPeriodPenalty(getPeriod(),getRooms()));
        return iPeriodPenalty.intValue();
    }
    
    
    private Integer iRotationPenalty = null;
    /**
     * Rotation penalty (an exam that has been in later period last times tries to be in an earlier period)
     * @return  {@link Exam#getRotationPenalty(ExamPeriod)}
     */
    public int getRotationPenalty() {
        if (iRotationPenalty==null) 
            iRotationPenalty = new Integer(((Exam)variable()).getRotationPenalty(getPeriod()));
        return iRotationPenalty.intValue();
    }
    
    private Integer iRoomPenalty = null;
    /**
     * Room weight (penalty for using given rooms) 
     * @return {@link Exam#getRoomWeight(Set)}
     */
    public int getRoomPenalty() {
        if (iRoomPenalty==null)
            iRoomPenalty = new Integer(((Exam)variable()).getRoomWeight(getRooms()));
        return iRoomPenalty.intValue();
    }


    /**
     * Overall cost of using this placement. 
     * The cost of an assignment consists of the following criteria:
     * <ul>
     *  <li> Direct student conflicts {@link ExamPlacement#getNrDirectConflicts()}, weighted by {@link ExamModel#getDirectConflictWeight()}
     *  <li> More than two exams a day student conflicts {@link ExamPlacement#getNrMoreThanTwoADayConflicts()}, weighted by {@link ExamModel#getMoreThanTwoADayWeight()}
     *  <li> Back-to-back student conflicts {@link ExamPlacement#getNrBackToBackConflicts()}, weighted by {@link ExamModel#getBackToBackConflictWeight()}
     *  <li> Distance back-to-back student conflicts {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getDistanceBackToBackConflictWeight()}
     *  <li> Period penalty {@link ExamPlacement#getPeriodPenalty()}, weighted by {@link ExamModel#getPeriodWeight()}
     *  <li> Room size penalty {@link ExamPlacement#getRoomSizePenalty()}, weighted by {@link ExamModel#getRoomSizeWeight()}
     *  <li> Room split penalty {@link ExamPlacement#getRoomSplitPenalty()}, weighted by {@link ExamModel#getRoomSplitWeight()}
     *  <li> Non-original room penalty {@link ExamPlacement#getNotOriginalRoomPenalty()}, weighted by {@link ExamModel#getNotOriginalRoomWeight()}
     * </ul>
     */
    public double toDouble() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return 
            model.getDirectConflictWeight()*getNrDirectConflicts()+
            model.getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts()+
            model.getBackToBackConflictWeight()*getNrBackToBackConflicts()+
            model.getDistanceBackToBackConflictWeight()*getNrDistanceBackToBackConflicts()+
            model.getPeriodWeight()*getPeriodPenalty()+ 
            model.getRoomSizeWeight()*getRoomSizePenalty()+
            model.getRoomSplitWeight()*getRoomSplitPenalty()+
            model.getNotOriginalRoomWeight()*getNotOriginalRoomPenalty()+
            model.getExamRotationWeight()*getRotationPenalty()+
            model.getRoomWeight()*getRoomPenalty();
    }
    
    /**
     * Overall cost of using this period. 
     * The time cost of an assignment consists of the following criteria:
     * <ul>
     *  <li> Direct student conflicts {@link ExamPlacement#getNrDirectConflicts()}, weighted by {@link ExamModel#getDirectConflictWeight()}
     *  <li> More than two exams a day student conflicts {@link ExamPlacement#getNrMoreThanTwoADayConflicts()}, weighted by {@link ExamModel#getMoreThanTwoADayWeight()}
     *  <li> Back-to-back student conflicts {@link ExamPlacement#getNrBackToBackConflicts()}, weighted by {@link ExamModel#getBackToBackConflictWeight()}
     *  <li> Period penalty {@link ExamPlacement#getPeriodPenalty()}, weighted by {@link ExamModel#getPeriodWeight()}
     * </ul>
     */
    public double getTimeCost() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return 
            model.getDirectConflictWeight()*getNrDirectConflicts()+
            model.getBackToBackConflictWeight()*getNrBackToBackConflicts()+
            model.getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts()+
            model.getPeriodWeight()*getPeriodPenalty()+
            model.getExamRotationWeight()*getRotationPenalty(); 
    }
    
    /**
     * Overall cost of using this set or rooms. 
     * The room cost of an assignment consists of the following criteria:
     * <ul>
     *  <li> Distance back-to-back student conflicts {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getDistanceBackToBackConflictWeight()}
     *  <li> Room size penalty {@link ExamPlacement#getRoomSizePenalty()}, weighted by {@link ExamModel#getRoomSizeWeight()}
     *  <li> Room split penalty {@link ExamPlacement#getRoomSplitPenalty()}, weighted by {@link ExamModel#getRoomSplitWeight()}
     *  <li> Non-original room penalty {@link ExamPlacement#getNotOriginalRoomPenalty()}, weighted by {@link ExamModel#getNotOriginalRoomWeight()}
     * </ul>
     */
    public double getRoomCost() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return 
            model.getDistanceBackToBackConflictWeight()*getNrDistanceBackToBackConflicts()+
            model.getRoomSizeWeight()*getRoomSizePenalty()+
            model.getRoomSplitWeight()*getRoomSplitPenalty()+
            model.getNotOriginalRoomWeight()*getNotOriginalRoomPenalty()+
            model.getRoomWeight()*getRoomPenalty();
    }
    
    /**
     * Assignment name (period / room(s))
     */
    public String getName() {
        return getPeriod()+"/"+getRooms();
    }
    
    /**
     * String representation -- returns a list of assignment costs 
     */
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00");
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return variable().getName()+" = "+getName()+" ("+
            df.format(toDouble())+"/"+
            "DC:"+getNrDirectConflicts()+","+
            "M2D:"+getNrMoreThanTwoADayConflicts()+","+
            "BTB:"+getNrBackToBackConflicts()+","+
            "dBTB:"+getNrDistanceBackToBackConflicts()+","+
            "PP:"+getPeriodPenalty()+","+
            "RSz:"+getRoomSizePenalty()+","+
            "RSp:"+getRoomSplitPenalty()+","+
            "ROg:"+getNotOriginalRoomPenalty()+
            ")";
    }
    
    /**
     * Compare two assignments for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamPlacement)) return false;
        ExamPlacement p = (ExamPlacement)o;
        return p.variable().equals(variable()) && p.getPeriod()==getPeriod() && p.getRooms().equals(getRooms());
    }
    
    /**
     * Hash code
     */
    public int hashCode() {
        return iHashCode;
    }
}
