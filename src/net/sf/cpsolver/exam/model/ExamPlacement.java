package net.sf.cpsolver.exam.model;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of an exam placement (problem value), i.e., assignment of an exam to period and room(s).
 * Each placement has defined a period and a set of rooms. The exam as well as the rooms have to be available
 * during the given period (see {@link Exam#getPeriodPlacements()} and {@link Exam#getRoomPlacements()}). 
 * The total size of rooms have to be equal or greater than the number of students enrolled in the exam 
 * {@link Exam#getSize()}, using either
 * {@link ExamRoom#getSize()} or {@link ExamRoom#getAltSize()}, depending on {@link Exam#hasAltSeating()}.
 * Also, the number of rooms has to be smaller or equal to {@link Exam#getMaxRooms()}. If 
 * {@link Exam#getMaxRooms()} is zero, the exam is only to be assigned to period (the set of rooms is empty).
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
 *  <li> Room penalty {@link ExamPlacement#getRoomPenalty()}, weighted by {@link ExamModel#getRoomWeight()}
 *  <li> Exam rotation penalty {@link ExamPlacement#getRotationPenalty()}, weighted by {@link ExamModel#getExamRotationWeight()}
 *  <li> Direct instructor conflicts {@link ExamPlacement#getNrInstructorDirectConflicts()}, weighted by {@link ExamModel#getInstructorDirectConflictWeight()}
 *  <li> More than two exams a day instructor conflicts {@link ExamPlacement#getNrInstructorMoreThanTwoADayConflicts()}, weighted by {@link ExamModel#getInstructorMoreThanTwoADayWeight()}
 *  <li> Back-to-back instructor conflicts {@link ExamPlacement#getNrInstructorBackToBackConflicts()}, weighted by {@link ExamModel#getInstructorBackToBackConflictWeight()}
 *  <li> Distance back-to-back instructor conflicts {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
 * </ul>
 * <br><br>
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
public class ExamPlacement extends Value {
    private ExamPeriodPlacement iPeriodPlacement;
    private Set iRoomPlacements;
    private int iSize;
    private int iRoomPenalty;
    private double iRoomSplitDistance;
    
    private int iHashCode;
    
    /**
     * Constructor
     * @param exam an exam
     * @param periodPlacement period placement
     * @param roomPlacements a set of room placements {@link ExamRoomPlacement}
     */
    public ExamPlacement(Exam exam, ExamPeriodPlacement periodPlacement, Set roomPlacements) {
        super(exam);
        iPeriodPlacement = periodPlacement;
        if (roomPlacements==null)
            iRoomPlacements = new HashSet();
        else
            iRoomPlacements = roomPlacements;
        iSize = 0;
        iRoomPenalty = 0;
        iRoomSplitDistance = 0.0;
        for (Iterator i=iRoomPlacements.iterator();i.hasNext();) {
            ExamRoomPlacement r = (ExamRoomPlacement)i.next();
            iSize += r.getSize(exam.hasAltSeating());
            iRoomPenalty += r.getPenalty(periodPlacement.getPeriod());
            if (iRoomPlacements.size()>1) {
                for (Iterator j=iRoomPlacements.iterator();j.hasNext();) {
                    ExamRoomPlacement w = (ExamRoomPlacement)j.next();
                    if (r.getRoom().getId()<w.getRoom().getId())
                        iRoomSplitDistance += r.getRoom().getDistance(w.getRoom());
                }
            }
        }
        if (iRoomPlacements.size()>2) {
            iRoomSplitDistance /= iRoomPlacements.size()*(iRoomPlacements.size()-1)/2;
        }
        iHashCode = getName().hashCode();
    }
    
    /**
     * Assigned period
     */
    public ExamPeriod getPeriod() { return iPeriodPlacement.getPeriod(); }
    
    /**
     * Assigned period placement
     */
    public ExamPeriodPlacement getPeriodPlacement() { return iPeriodPlacement; }
    
    /**
     * Assigned rooms (it is empty when {@link Exam#getMaxRooms()} is zero)
     * @return list of {@link ExamRoomPlacement}
     */
    public Set getRoomPlacements() { return iRoomPlacements; }
    
    /**
     * Overall size of assigned rooms
     */
    public int getSize() { return iSize; }
    
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
            else if (!s.isAvailable(getPeriod())) penalty++;
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
        if (getRoomPlacements().isEmpty() || other.getRoomPlacements().isEmpty()) return 0;
        int maxDistance = 0;
        for (Iterator i1=getRoomPlacements().iterator();i1.hasNext();) {
            ExamRoomPlacement r1 = (ExamRoomPlacement)i1.next();
            for (Iterator i2=other.getRoomPlacements().iterator();i2.hasNext();) {
                ExamRoomPlacement r2 = (ExamRoomPlacement)i2.next();
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
    
    /**
     * Number of direct instructor conflicts, i.e., number of cases when this exam
     * is attended by an instructor that attends some other exam at the same period 
     */
    public int getNrInstructorDirectConflicts() {
        Exam exam = (Exam)variable();
        //if (!exam.isAllowDirectConflicts()) return 0;
        int penalty = 0;
        for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor s = (ExamInstructor)e.nextElement();
            Set exams = s.getExams(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam)?0:1);
            if (nrExams>1) penalty++;
            else if (!s.isAvailable(getPeriod())) penalty++;
        }
        return penalty;
    }
    
    /**
     * Number of back-to-back instructor conflicts, i.e., number of cases when this exam
     * is attended by an instructor that attends some other exam at the previous {@link ExamPeriod#prev()}
     * or following {@link ExamPeriod#next()} period. If {@link ExamModel#isDayBreakBackToBack()} is false,
     * back-to-back conflicts are only considered between consecutive periods that are of the 
     * same day.
     */
    public int getNrInstructorBackToBackConflicts() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        int penalty = 0;
        for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor s = (ExamInstructor)e.nextElement();
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
     * Number of back-to-back distance instructor conflicts, i.e., number of cases when this exam
     * is attended by an instructor that attends some other exam at the previous {@link ExamPeriod#prev()}
     * or following {@link ExamPeriod#next()} period and the distance {@link ExamPlacement#getDistance(ExamPlacement)}
     * between these two exams is greater than {@link ExamModel#getBackToBackDistance()}. 
     * Distance back-to-back conflicts are only 
     * considered between consecutive periods that are of the 
     * same day.
     */
    public int getNrInstructorDistanceBackToBackConflicts() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        int btbDist = model.getBackToBackDistance();
        if (btbDist<0) return 0;
        int penalty = 0;
        for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor s = (ExamInstructor)e.nextElement();
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
     * Number of more than two exams a day instructor conflicts, i.e., when this exam
     * is attended by an instructor student that attends two or more other exams at the same day. 
     */
    public int getNrInstructorMoreThanTwoADayConflicts() {
        Exam exam = (Exam)variable();
        int penalty = 0;
        for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
            ExamInstructor s = (ExamInstructor)e.nextElement();
            Set exams = s.getExamsADay(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam)?0:1);
            if (nrExams>2) penalty++;
        }
        return penalty;
    }
    
    /**
     * Cost for using room(s) that are too big
     * @return difference between total room size (computed using either {@link ExamRoom#getSize()} or 
     * {@link ExamRoom#getAltSize()} based on {@link Exam#hasAltSeating()}) and the number of students {@link Exam#getSize()}
     */
    public int getRoomSizePenalty() {
        Exam exam = (Exam)variable();
        int diff = getSize()-exam.getSize();
        return (diff<0?0:diff);
    }
    
    /**
     * Cost for using more than one room (nrSplits^2).
     * @return penalty (1 for 2 rooms, 4 for 3 rooms, 9 for 4 rooms, etc.)
     */
    public int getRoomSplitPenalty() {
        return (iRoomPlacements.size()<=1?0:(iRoomPlacements.size()-1)*(iRoomPlacements.size()-1));
    }

    /**
     * Cost for using a period, i.e., {@link ExamPeriodPlacement#getPenalty()}
     */
    public int getPeriodPenalty() {
        return iPeriodPlacement.getPenalty();
    }
    
    
    /**
     * Rotation penalty (an exam that has been in later period last times tries to be in an earlier period)
     */
    public int getRotationPenalty() {
        Exam exam = (Exam)variable();
        if (exam.getAveragePeriod()<0) return 0;
        return (1+getPeriod().getIndex())*(1+exam.getAveragePeriod());
    }
    
    /**
     * Front load penalty (large exam is discouraged to be placed on or after a certain period)
     * @return zero if not large exam or if before {@link ExamModel#getLargePeriod()}, one otherwise 
     */
    public int getLargePenalty() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        if (model.getLargeSize()<0 || exam.getSize()<model.getLargeSize()) return 0;
        int periodIdx = (int)Math.round(model.getPeriods().size() * model.getLargePeriod());
        return (getPeriod().getIndex()<periodIdx?0:1);
    }
    
    /**
     * Room penalty (penalty for using given rooms), i.e., sum of {@link ExamRoomPlacement#getPenalty(ExamPeriod)} of assigned rooms 
     */
    public int getRoomPenalty() {
        return iRoomPenalty;
    }
    
    /**
     * Perturbation penalty, i.e., penalty for using a different assignment than initial. 
     * Only applicable when {@link ExamModel#isMPP()} is true (minimal perturbation problem).
     * @return |period index - initial period index | * exam size 
     */
    public int getPerturbationPenalty() {
        Exam exam = (Exam)variable();
        if (!((ExamModel)exam.getModel()).isMPP()) return 0;
        ExamPlacement initial = (ExamPlacement)exam.getInitialAssignment();
        if (initial==null) return 0;
        return Math.abs(initial.getPeriod().getIndex()-getPeriod().getIndex())*exam.getSize();
    }
    
    /**
     * Room split distance penalty, i.e., average distance between two rooms of this placement 
     */
    public double getRoomSplitDistancePenalty() {
        return iRoomSplitDistance;
    }
    
    /**
     * Distribution penalty, i.e., sum weights of violated distribution constraints 
     */
    public double getDistributionPenalty() {
        int penalty = 0;
        for (Enumeration e=((Exam)variable()).getDistributionConstraints().elements();e.hasMoreElements();) {
            ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
            if (dc.isHard()) continue;
            boolean sat = dc.isSatisfied(this); 
            if (sat!=dc.isSatisfied())
                penalty += (sat?-dc.getWeight():dc.getWeight());
        }
        return penalty;
    }
    
    /**
     * Room related distribution penalty, i.e., sum weights of violated distribution constraints 
     */
    public double getRoomDistributionPenalty() {
        int penalty = 0;
        for (Enumeration e=((Exam)variable()).getDistributionConstraints().elements();e.hasMoreElements();) {
            ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
            if (dc.isHard() || !dc.isRoomRelated()) continue;
            boolean sat = dc.isSatisfied(this); 
            if (sat!=dc.isSatisfied())
                penalty += (sat?-dc.getWeight():dc.getWeight());
        }
        return penalty;
    }

    /**
     * Period related distribution penalty, i.e., sum weights of violated distribution constraints 
     */
    public double getPeriodDistributionPenalty() {
        int penalty = 0;
        for (Enumeration e=((Exam)variable()).getDistributionConstraints().elements();e.hasMoreElements();) {
            ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
            if (dc.isHard() || !dc.isPeriodRelated()) continue;
            boolean sat = dc.isSatisfied(this); 
            if (sat!=dc.isSatisfied())
                penalty += (sat?-dc.getWeight():dc.getWeight());
        }
        return penalty;
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
     *  <li> Room split distance penalty {@link ExamPlacement#getRoomSplitDistancePenalty()}, weighted by {@link ExamModel#getRoomSplitDistanceWeight()}
     *  <li> Room penalty {@link ExamPlacement#getRoomPenalty()}, weighted by {@link ExamModel#getRoomWeight()}
     *  <li> Exam rotation penalty {@link ExamPlacement#getRotationPenalty()}, weighted by {@link ExamModel#getExamRotationWeight()}
     *  <li> Direct instructor conflicts {@link ExamPlacement#getNrInstructorDirectConflicts()}, weighted by {@link ExamModel#getInstructorDirectConflictWeight()}
     *  <li> More than two exams a day instructor conflicts {@link ExamPlacement#getNrInstructorMoreThanTwoADayConflicts()}, weighted by {@link ExamModel#getInstructorMoreThanTwoADayWeight()}
     *  <li> Back-to-back instructor conflicts {@link ExamPlacement#getNrInstructorBackToBackConflicts()}, weighted by {@link ExamModel#getInstructorBackToBackConflictWeight()}
     *  <li> Distance back-to-back instructor conflicts {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
     *  <li> Front load penalty {@link ExamPlacement#getLargePenalty()}, weighted by {@link ExamModel#getLargeWeight()}
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
            model.getExamRotationWeight()*getRotationPenalty()+
            model.getRoomWeight()*getRoomPenalty()+
            model.getInstructorDirectConflictWeight()*getNrInstructorDirectConflicts()+
            model.getInstructorMoreThanTwoADayWeight()*getNrInstructorMoreThanTwoADayConflicts()+
            model.getInstructorBackToBackConflictWeight()*getNrInstructorBackToBackConflicts()+
            model.getInstructorDistanceBackToBackConflictWeight()*getNrInstructorDistanceBackToBackConflicts()+
            model.getRoomSplitDistanceWeight()*getRoomSplitDistancePenalty()+
            model.getPerturbationWeight()*getPerturbationPenalty()+
            model.getDistributionWeight()*getDistributionPenalty()+
            model.getLargeWeight()*getLargePenalty();
    }
    
    /**
     * Overall cost of using this period. 
     * The time cost of an assignment consists of the following criteria:
     * <ul>
     *  <li> Direct student conflicts {@link ExamPlacement#getNrDirectConflicts()}, weighted by {@link ExamModel#getDirectConflictWeight()}
     *  <li> More than two exams a day student conflicts {@link ExamPlacement#getNrMoreThanTwoADayConflicts()}, weighted by {@link ExamModel#getMoreThanTwoADayWeight()}
     *  <li> Back-to-back student conflicts {@link ExamPlacement#getNrBackToBackConflicts()}, weighted by {@link ExamModel#getBackToBackConflictWeight()}
     *  <li> Period penalty {@link ExamPlacement#getPeriodPenalty()}, weighted by {@link ExamModel#getPeriodWeight()}
     *  <li> Exam rotation penalty {@link ExamPlacement#getRotationPenalty()}, weighted by {@link ExamModel#getExamRotationWeight()}
     *  <li> Direct instructor conflicts {@link ExamPlacement#getNrInstructorDirectConflicts()}, weighted by {@link ExamModel#getInstructorDirectConflictWeight()}
     *  <li> More than two exams a day instructor conflicts {@link ExamPlacement#getNrInstructorMoreThanTwoADayConflicts()}, weighted by {@link ExamModel#getInstructorMoreThanTwoADayWeight()}
     *  <li> Back-to-back instructor conflicts {@link ExamPlacement#getNrInstructorBackToBackConflicts()}, weighted by {@link ExamModel#getInstructorBackToBackConflictWeight()}
     *  <li> Distance back-to-back instructor conflicts {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
     *  <li> Front load penalty {@link ExamPlacement#getLargePenalty()}, weighted by {@link ExamModel#getLargeWeight()}
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
            model.getExamRotationWeight()*getRotationPenalty()+
            model.getInstructorDirectConflictWeight()*getNrInstructorDirectConflicts()+
            model.getInstructorMoreThanTwoADayWeight()*getNrInstructorMoreThanTwoADayConflicts()+
            model.getInstructorBackToBackConflictWeight()*getNrInstructorBackToBackConflicts()+
            model.getDistributionWeight()*getPeriodDistributionPenalty()+
            model.getLargeWeight()*getLargePenalty();
    }
    
    /**
     * Overall cost of using this set or rooms. 
     * The room cost of an assignment consists of the following criteria:
     * <ul>
     *  <li> Distance back-to-back student conflicts {@link ExamPlacement#getNrDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getDistanceBackToBackConflictWeight()}
     *  <li> Distance back-to-back instructor conflicts {@link ExamPlacement#getNrInstructorDistanceBackToBackConflicts()}, weighted by {@link ExamModel#getInstructorDistanceBackToBackConflictWeight()}
     *  <li> Room size penalty {@link ExamPlacement#getRoomSizePenalty()}, weighted by {@link ExamModel#getRoomSizeWeight()}
     *  <li> Room split penalty {@link ExamPlacement#getRoomSplitPenalty()}, weighted by {@link ExamModel#getRoomSplitWeight()}
     *  <li> Room split distance penalty {@link ExamPlacement#getRoomSplitDistancePenalty()}, weighted by {@link ExamModel#getRoomSplitDistanceWeight()}
     *  <li> Room penalty {@link ExamPlacement#getRoomPenalty()}, weighted by {@link ExamModel#getRoomWeight()}
     * </ul>
     */
    public double getRoomCost() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return 
            model.getDistanceBackToBackConflictWeight()*getNrDistanceBackToBackConflicts()+
            model.getRoomSizeWeight()*getRoomSizePenalty()+
            model.getRoomSplitWeight()*getRoomSplitPenalty()+
            model.getRoomWeight()*getRoomPenalty()+
            model.getInstructorDistanceBackToBackConflictWeight()*getNrInstructorDistanceBackToBackConflicts()+
            model.getRoomSplitDistanceWeight()*getRoomSizePenalty()+
            model.getDistributionWeight()*getRoomDistributionPenalty();
    }
    
    /**
     * Room names separated with the given delimiter
     */
    public String getRoomName(String delim) {
        String roomName = "";
        for (Iterator i=getRoomPlacements().iterator();i.hasNext();) {
            ExamRoomPlacement r = (ExamRoomPlacement)i.next();
            roomName += r.getRoom().getName();
            if (i.hasNext()) roomName += delim;
        }
        return roomName;
    }
    
    /**
     * Assignment name (period / room(s))
     */
    public String getName() {
        return getPeriod()+"/"+getRoomName(",");
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
            (model.getBackToBackDistance()<0?"":"dBTB:"+getNrDistanceBackToBackConflicts()+",")+
            "PP:"+getPeriodPenalty()+","+
            "@P:"+getRotationPenalty()+","+
            "RSz:"+getRoomSizePenalty()+","+
            "RSp:"+getRoomSplitPenalty()+","+
            "RD:"+df.format(getRoomSplitDistancePenalty())+","+
            "RP:"+getRoomPenalty()+
            (model.isMPP()?",IP:"+getPerturbationPenalty():"")+
            ")";
    }
    
    /**
     * Compare two assignments for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamPlacement)) return false;
        ExamPlacement p = (ExamPlacement)o;
        return p.variable().equals(variable()) && p.getPeriod().equals(getPeriod()) && p.getRoomPlacements().equals(getRoomPlacements());
    }
    
    /**
     * Hash code
     */
    public int hashCode() {
        return iHashCode;
    }
    
    /**
     * True if given room is between {@link ExamPlacement#getRoomPlacements()}
     */
    public boolean contains(ExamRoom room) {
        return getRoomPlacements().contains(new ExamRoomPlacement(room));
    }
}
