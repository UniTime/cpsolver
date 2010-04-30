package net.sf.cpsolver.exam.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Representation of an exam (problem variable).
 * Each exam has defined a length (in minutes), type (whether it is a section or a course exam),
 * seating type (whether it requires normal or alternate seating) and a maximal number of rooms.
 * If the maximal number of rooms is zero, the exam will be timetabled only in time (it does not
 * require a room).  
 * <br><br>
 * An exam can be only assigned to a period {@link ExamPeriod} that is long enough (see {@link ExamPeriod#getLength()})
 * and that is available for the exam (see {@link Exam#getPeriodPlacements()}).
 * <br><br>
 * A set of rooms that are available in the given period (see {@link ExamRoom#isAvailable(ExamPeriod)}, {@link ExamRoomPlacement#isAvailable(ExamPeriod)}), 
 * and which together cover the size of exam (number of students attending the exam) has to be
 * assigned to an exam. Based on the type of seating (see {@link Exam#hasAltSeating()}), either
 * room sizes (see {@link ExamRoom#getSize()}) or alternative seating sizes (see {@link ExamRoom#getAltSize()}) 
 * are used. An exam has a list of available rooms with their penalties assiciated with 
 * (see {@link Exam#getRoomPlacements()}).
 * <br><br>
 * Various penalties for an assignment of a period or a set of rooms may apply. See {@link ExamPlacement} 
 * for more details.
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
public class Exam extends Variable {
    private static boolean sAlterMaxSize = false;
    private static Logger sLog = Logger.getLogger(Exam.class);
    protected static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",new java.text.DecimalFormatSymbols(Locale.US));
    
    private Vector iStudents = new Vector();
    private Vector iInstructors = new Vector();
    private Vector iDistConstraints = new Vector();
    
    private boolean iAllowDirectConflicts = true;
    
    private String iName = null;
    private int iLength = 0;
    private int iMaxRooms = 0;
    private int iMinSize = 0;
    private boolean iAltSeating = false;
    private int iAveragePeriod = -1;
    private Integer iSize = null;
    private Integer iPrintOffset = null;
    
    private Vector iOwners = new Vector();
    private Vector iPeriodPlacements = null;
    private Vector iRoomPlacements = null;
    
    /**
     * Constructor
     * @param id exam unique id
     * @param length exam length in minutes
     * @param altSeating true if alternative seating is requested
     * @param maxRooms maximum number of rooms to be used
     * @param minSize minimal size of rooms into which an exam can be assigned (see {@link Exam#getSize()})
     * @param periodPlacements list of periods and their penalties {@link ExamPeriodPlacement} into which an exam can be assigned
     * @param roomPlacements list of rooms and their penalties {@link ExamRoomPlacement} into which an exam can be assigned
     */
    public Exam(long id, String name, int length, boolean altSeating, int maxRooms, int minSize, Vector periodPlacements, Vector roomPlacements) {
        super();
        iId = id;
        iName = name;
        iLength = length;
        iAltSeating = altSeating;
        iMaxRooms = maxRooms;
        iMinSize = minSize;
        iPeriodPlacements = periodPlacements;
        Collections.sort(iPeriodPlacements, new Comparator() {
            public int compare(Object o1, Object o2) {
                ExamPeriodPlacement p1 = (ExamPeriodPlacement)o1;
                ExamPeriodPlacement p2 = (ExamPeriodPlacement)o2;
                return p1.getPeriod().compareTo(p2.getPeriod());
            }
        });
        iRoomPlacements = roomPlacements;
        Collections.sort(iRoomPlacements, new Comparator() {
            public int compare(Object o1, Object o2) {
                ExamRoomPlacement p1 = (ExamRoomPlacement)o1;
                ExamRoomPlacement p2 = (ExamRoomPlacement)o2;
                int cmp = -Double.compare(p1.getSize(hasAltSeating()),p2.getSize(hasAltSeating()));
                if (cmp!=0) return cmp;
                return p1.getRoom().compareTo(p2.getRoom());
            }
        });
    }
    
    /**
     * Exam size, it is bigger from {@link Exam#getMinSize()} and the number of students enrolled into the exam {@link Exam#getStudents()}.
     * If {@link Exam#getMaxRooms()} is greater than zero, an exam must be assigned into rooms which overall size (or alternative seating size if 
     * {@link Exam#hasAltSeating()}) must be equal or greater than this size.
     */
    public int getSize() {
        return (iSize==null?Math.max(iMinSize,getStudents().size()):iSize.intValue());
    }
    
    /**
     * Override exam size with given value (revert to default when null)
     */
    public void setSizeOverride(Integer size) {
        iSize = size;
    }
    
    /**
     * Override exam size with given value (revert to default when null)
     */
    public Integer getSizeOverride() {
        return iSize;
    }
    
    /**
     * Print offset -- for reporting purposes
     */
    public Integer getPrintOffset() {
        return iPrintOffset;
    }
    
    /**
     * Print offset -- for reporting purposes
     */
    public void setPrintOffset(Integer printOffset) {
        iPrintOffset = printOffset;
    }

    /**
     * Minimal exam size, see {@link Exam#getSize()} 
     */
    public int getMinSize() {
        return iMinSize;
    }
    
    /**
     * Minimal exam size, see {@link Exam#getSize()} 
     */
    public void setMinSize(int minSize) {
        iMinSize = minSize;
    }

    
    /**
     * Values (assignment of a period and a set of rooms)
     * @return list of {@link ExamPlacement}
     */
    public Vector values() {
        if (super.values()==null) init();
        return super.values();
    }
    
    /**
     * Return list of possible room placements.
     * @return list of {@link ExamRoomPlacement}
     */
    public Vector getRoomPlacements() {
        return iRoomPlacements;
    }
    
    /**
     * Return list of possible period placements.
     * @return list of {@link ExamPeriodPlacement}
     */
    public Vector getPeriodPlacements() {
        return iPeriodPlacements;
    }
    
    /** 
     * Initialize exam's domain. 
     */ 
    private boolean init() {
        if (sAlterMaxSize && iRoomPlacements.size()>50) {
            ExamRoomPlacement med = (ExamRoomPlacement)iRoomPlacements.elementAt(Math.min(50, iRoomPlacements.size()/2));
            setMaxRooms(Math.min(getMaxRooms(),1+getSize()/med.getSize(hasAltSeating())));
        }
        Vector values = new Vector();
        if (getMaxRooms()==0) {
            for (Enumeration e=getPeriodPlacements().elements();e.hasMoreElements();) {
                ExamPeriodPlacement periodPlacement = (ExamPeriodPlacement)e.nextElement();
                values.addElement(new ExamPlacement(this, periodPlacement, new HashSet()));
            }
        } else {
            if (getRoomPlacements().isEmpty()) {
                sLog.error("  Exam "+getName()+" has no rooms.");
                setValues(new Vector(0));
                return false;
            }
            for (Enumeration e=getPeriodPlacements().elements();e.hasMoreElements();) {
                ExamPeriodPlacement periodPlacement = (ExamPeriodPlacement)e.nextElement();
                TreeSet roomSets = new TreeSet();
                genRoomSets(periodPlacement.getPeriod(), Math.min(100,iRoomPlacements.size()), roomSets, 0, getMaxRooms(), new HashSet(), 0, 0);
                for (Iterator i=roomSets.iterator();i.hasNext();) {
                    RoomSet roomSet = (RoomSet)i.next();
                    values.addElement(new ExamPlacement(this, periodPlacement, roomSet.rooms()));
                }
            }
        }
        if (values.isEmpty()) sLog.error("Exam "+getName()+" has no placement.");
        setValues(values);
        return !values.isEmpty();
    }
    
    private void genRoomSets(ExamPeriod period, int maxRoomSets, TreeSet roomSets, int roomIdx, int maxRooms, Set roomsSoFar, int sizeSoFar, int penaltySoFar) {
        ExamModel model = (ExamModel)getModel();
        if (sizeSoFar>=getSize()) {
            double penalty = 
                model.getRoomSplitWeight() * (roomsSoFar.size()-1) * (roomsSoFar.size()-1) +
                model.getRoomSizeWeight() * (sizeSoFar-getSize()) +
                model.getRoomWeight() * penaltySoFar;
            if (roomSets.size()>=maxRoomSets) {
                RoomSet last = (RoomSet)roomSets.last();
                if (penalty<last.penalty()) {
                    roomSets.remove(last);
                    roomSets.add(new RoomSet(roomsSoFar,penalty));
                }
            } else
                roomSets.add(new RoomSet(roomsSoFar,penalty));
            return;
        }
        if (!roomSets.isEmpty()) {
            RoomSet roomSet = (RoomSet)roomSets.first();
            maxRooms = Math.min(maxRooms, (1+roomSet.rooms().size())-roomsSoFar.size());
        }
        if (maxRooms==0) return;
        int sizeBound = sizeSoFar;
        for (int i=0;i<maxRooms && roomIdx+i<iRoomPlacements.size();i++)
            sizeBound += ((ExamRoomPlacement)iRoomPlacements.elementAt(roomIdx+i)).getSize(hasAltSeating());
        while (roomIdx<iRoomPlacements.size()) {
            if (sizeBound<getSize()) break;
            ExamRoomPlacement room = (ExamRoomPlacement)iRoomPlacements.elementAt(roomIdx);
            if (!room.isAvailable(period)) continue;
            roomsSoFar.add(room);
            genRoomSets(
                    period, maxRoomSets, roomSets, roomIdx+1, maxRooms-1, 
                    roomsSoFar, sizeSoFar+room.getSize(hasAltSeating()), penaltySoFar + room.getPenalty(period));
            roomsSoFar.remove(room);
            sizeBound -= room.getSize(hasAltSeating());
            if (roomIdx+maxRooms<iRoomPlacements.size())
                sizeBound += ((ExamRoomPlacement)iRoomPlacements.elementAt(roomIdx+maxRooms)).getSize(hasAltSeating());
            roomIdx++;
            if (roomSets.size()==maxRoomSets) {
                RoomSet last = (RoomSet)roomSets.last();
                if (last.rooms().size()<roomsSoFar.size()+1) return;
            }
        }
    }
    
    private class RoomSet implements Comparable {
        private Set iRooms;
        private double iPenalty;
        public RoomSet(Set rooms, double penalty) {
            iRooms = new HashSet(rooms);
            iPenalty = penalty; 
        }
        public Set rooms() { return iRooms; }
        public double penalty() { return iPenalty; }
        public int compareTo(Set rooms, double penalty) {
            int cmp = Double.compare(iRooms.size(), rooms.size());
            if (cmp!=0) return cmp;
            cmp = Double.compare(penalty(), penalty);
            if (cmp!=0) return cmp;
            return rooms().toString().compareTo(rooms.toString());
        }
        public int compareTo(Object o) {
            RoomSet r = (RoomSet)o;
            return compareTo(r.rooms(),r.penalty());
        }
    }
    
    /**
     * True if alternative seating is required ({@link ExamRoom#getAltSize()} is to be used),
     * false if normal seating is required ({@link ExamRoom#getSize()} is to be used).
     * @return true if alternative seating is required, false otherwise
     */
    public boolean hasAltSeating() {
        return iAltSeating;
    }
    
    /**
     * Length of the exam in minutes. The assigned period has to be of the same or greater length. 
     * @return length of the exam in minutes
     */
    public int getLength() {
        return iLength;
    }
    
    /**
     * Set average period. This represents an average period that the exam was assigned to in the past.
     * If set, it is used in exam rotation penalty {@link ExamPlacement#getRotationPenalty()} in order to
     * put more weight on exams that were badly assigned last time(s) and ensuring some form of
     * fairness.
     * @param period average period
     */
    public void setAveragePeriod(int period) {
        iAveragePeriod = period;
    }
    
    /**
     * Average period. This represents an average period that the exam was assigned to in the past.
     * If set, it is used in exam rotation penalty {@link ExamPlacement#getRotationPenalty()} in order to
     * put more weight on exams that were badly assigned last time(s) and ensuring some form of
     * fairness.
     * @return average period
     */
    public int getAveragePeriod() {
        return iAveragePeriod;
    }
    
    /**
     * True if there is an average period assigned to the exam. This represents an average period 
     * that the exam was assigned to in the past.
     * If set, it is used in exam rotation penalty {@link ExamPlacement#getRotationPenalty()} in order to
     * put more weight on exams that were badly assigned last time(s) and ensuring some form of
     * fairness.
     */
    public boolean hasAveragePeriod() {
        return iAveragePeriod>=0;
    }
    
    /**
     * True if a direct student conflict is allowed, see {@link ExamStudent#canConflict(Exam, Exam)} 
     * @return true if a direct student conflict is allowed
     */
    public boolean isAllowDirectConflicts() {
        return iAllowDirectConflicts;
    }
    
    /**
     * Set whether a direct student conflict is allowed, see {@link ExamStudent#canConflict(Exam, Exam)} 
     * @param allowDirectConflicts true if a direct student conflict is allowed
     */
    public void setAllowDirectConflicts(boolean allowDirectConflicts) {
        iAllowDirectConflicts = allowDirectConflicts;
    }
    
    /** Adds a constraint. Called automatically when the constraint is added to the model, i.e.,
     * {@link Model#addConstraint(Constraint)} is called.
     * @param constraint added constraint
     */
    public void addContstraint(Constraint constraint) {
        if (constraint instanceof ExamStudent) iStudents.add(constraint);
        if (constraint instanceof ExamDistributionConstraint) iDistConstraints.add(constraint);
        if (constraint instanceof ExamInstructor) iInstructors.add(constraint);
        super.addContstraint(constraint);
    }
    
    /** Removes a constraint. Called automatically when the constraint is removed from the model, i.e.,
     * {@link Model#removeConstraint(Constraint)} is called.
     * @param constraint added constraint
     */
    public void removeContstraint(Constraint constraint) {
        if (constraint instanceof ExamStudent) iStudents.remove(constraint);
        if (constraint instanceof ExamDistributionConstraint) iDistConstraints.remove(constraint);
        if (constraint instanceof ExamInstructor) iInstructors.remove(constraint);
        super.removeContstraint(constraint);
    }
    
    /** 
     * List of students that are enrolled in the exam
     * @return list of {@link ExamStudent}
     */
    public Vector getStudents() { return iStudents; }
    
    
    /** 
     * List of distribution constraints that this exam is involved in
     * @return list of {@link ExamDistributionConstraint}
     */
    public Vector getDistributionConstraints() { return iDistConstraints; }
    
    /** 
     * List of instructors that are assigned to this exam
     * @return list of {@link ExamInstructor}
     */
    public Vector getInstructors() { return iInstructors; }

    /** 
     * Check all distribution constraint that this exam is involved in 
     * @param period a period to be assigned to this exam
     * @return true, if there is no assignment of some other exam in conflict with the given period
     */
    public boolean checkDistributionConstraints(ExamPeriodPlacement period) {
        for (Enumeration e=iDistConstraints.elements();e.hasMoreElements();) {
            ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
            if (!dc.isHard()) continue;
            boolean before = true;
            for (Enumeration f=dc.variables().elements();f.hasMoreElements();) {
                Exam exam = (Exam)f.nextElement();
                if (exam.equals(this)) {
                    before = false; continue;
                }
                ExamPlacement placement = (ExamPlacement)exam.getAssignment();
                if (placement==null) continue;
                switch (dc.getType()) {
                    case ExamDistributionConstraint.sDistSamePeriod :
                        if (period.getIndex()!=placement.getPeriod().getIndex()) return false;
                        break;
                    case ExamDistributionConstraint.sDistDifferentPeriod :
                        if (period.getIndex()==placement.getPeriod().getIndex()) return false;
                        break;
                    case ExamDistributionConstraint.sDistPrecedence :
                        if (before) {
                            if (period.getIndex()<=placement.getPeriod().getIndex()) return false;
                        } else {
                            if (period.getIndex()>=placement.getPeriod().getIndex()) return false;
                        }
                        break;
                    case ExamDistributionConstraint.sDistPrecedenceRev :
                        if (before) {
                            if (period.getIndex()>=placement.getPeriod().getIndex()) return false;
                        } else {
                            if (period.getIndex()<=placement.getPeriod().getIndex()) return false;
                        }
                        break;
                }
            }
        }
        return true;
    }

    /** 
     * Check all distribution constraint that this exam is involved in 
     * @param room a room to be assigned to this exam
     * @return true, if there is no assignment of some other exam in conflict with the given room
     */
    public boolean checkDistributionConstraints(ExamRoomPlacement room) {
        for (Enumeration e=iDistConstraints.elements();e.hasMoreElements();) {
            ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
            if (!dc.isHard()) continue;
            for (Enumeration f=dc.variables().elements();f.hasMoreElements();) {
                Exam exam = (Exam)f.nextElement();
                if (exam.equals(this)) continue;
                ExamPlacement placement = (ExamPlacement)exam.getAssignment();
                if (placement==null) continue;
                switch (dc.getType()) {
                    case ExamDistributionConstraint.sDistSameRoom :
                        if (!placement.getRoomPlacements().contains(room)) return false;
                        break;
                    case ExamDistributionConstraint.sDistDifferentRoom :
                        if (placement.getRoomPlacements().contains(room)) return false;
                        break;
                }
            }
        }
        return true;
    }
    
    /** 
     * Check all soft distribution constraint that this exam is involved in 
     * @param room a room to be assigned to this exam
     * @return sum of penalties of violated distribution constraints
     */
    public int getDistributionConstraintPenalty(ExamRoomPlacement room) {
        int penalty = 0;
        for (Enumeration e=iDistConstraints.elements();e.hasMoreElements();) {
            ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
            if (dc.isHard()) continue;
            for (Enumeration f=dc.variables().elements();f.hasMoreElements();) {
                Exam exam = (Exam)f.nextElement();
                if (exam.equals(this)) continue;
                ExamPlacement placement = (ExamPlacement)exam.getAssignment();
                if (placement==null) continue;
                switch (dc.getType()) {
                    case ExamDistributionConstraint.sDistSameRoom :
                        if (!placement.getRoomPlacements().contains(room)) penalty+=dc.getWeight();
                        break;
                    case ExamDistributionConstraint.sDistDifferentRoom :
                        if (placement.getRoomPlacements().contains(room)) penalty+=dc.getWeight();
                        break;
                }
            }
        }
        return penalty;
    }
    
    /**
     * Maximal number of rooms that can be assigned to the exam 
     * @return maximal number of rooms that can be assigned to the exam
     */
    public int getMaxRooms() {
        return iMaxRooms;
    }
    /**
     * Set maximal number of rooms that can be assigned to the exam 
     * @param maxRooms maximal number of rooms that can be assigned to the exam
     */
    public void setMaxRooms(int maxRooms) {
        iMaxRooms = maxRooms;
    }
    
    /**
     * Find best available rooms for the exam in the given period. First of all, it tries to find the minimal
     * number of rooms that cover the size of the exam. Among these, a set of rooms of 
     * total smallest size is preferred. If the original room is available and of enough size, it is returned. 
     * All necessary checks are made (availability of rooms, room penalties, room sizes etc.).
     * @param period given period.
     * @return best available rooms for the exam in the given period, null if there is no valid assignment
     */
    public Set findBestAvailableRooms(ExamPeriodPlacement period) {
        if (getMaxRooms()==0) return new HashSet();
        double sw = ((ExamModel)getModel()).getRoomSizeWeight();
        //double dw = ((ExamModel)getModel()).getRoomSplitDistanceWeight();
        double pw = ((ExamModel)getModel()).getRoomWeight();
        double cw = ((ExamModel)getModel()).getDistributionWeight();
        loop: for (int nrRooms=1;nrRooms<=getMaxRooms();nrRooms++) {
            HashSet rooms = new HashSet(); int size = 0;
            while (rooms.size()<nrRooms && size<getSize()) {
                int minSize = (getSize()-size)/(nrRooms-rooms.size());
                ExamRoomPlacement best = null; double bestWeight = 0; int bestSize = 0;
                for (Enumeration e=getRoomPlacements().elements();e.hasMoreElements();) {
                    ExamRoomPlacement room = (ExamRoomPlacement)e.nextElement();
                    if (!room.isAvailable(period.getPeriod())) continue;
                    if (room.getRoom().getPlacement(period.getPeriod())!=null) continue;
                    if (rooms.contains(room)) continue;
                    if (!checkDistributionConstraints(room)) continue;
                    int s = room.getSize(hasAltSeating());
                    if (s<minSize) break;
                    int p = room.getPenalty(period.getPeriod());
                    double w = pw * p + sw * (s-minSize) + cw * getDistributionConstraintPenalty(room);
                    double d = 0;
                    if (!rooms.isEmpty()) {
                        for (Iterator i=rooms.iterator();i.hasNext();) {
                            ExamRoomPlacement r = (ExamRoomPlacement)i.next();
                            d += r.getDistance(room);
                        }
                        w += d / rooms.size();
                    }
                    if (best==null || bestWeight>w) {
                        best = room;
                        bestSize = s;
                        bestWeight = w;
                    }
                }
                if (best==null) continue loop;
                rooms.add(best); size+=bestSize;
            }
            if (size>=getSize()) return rooms;
        }
        return null;
    }
    
    /**
     * Randomly find a set of available rooms for the exam in the given period. First of all, it tries to find the minimal
     * number of rooms that cover the size of the exam. Among these, a set of rooms of 
     * total smallest size is preferred.
     * All necessary checks are made (availability of rooms, room penalties, room sizes etc.). 
     * @param period given period.
     * @return randomly computed set of available rooms for the exam in the given period, null if there is no valid assignment
     */
    public Set findRoomsRandom(ExamPeriodPlacement period) {
        return findRoomsRandom(period,true);
    }
    
    /**
     * Randomly find a set of available rooms for the exam in the given period. First of all, it tries to find the minimal
     * number of rooms that cover the size of the exam. Among these, a set of rooms of 
     * total smallest size is preferred.
     * All necessary checks are made (availability of rooms, room penalties, room sizes etc.). 
     * @param period given period.
     * @param checkConflicts if false, room and distribution conflicts are not checked 
     * @return randomly computed set of available rooms for the exam in the given period, null if there is no valid assignment
     */
    public Set findRoomsRandom(ExamPeriodPlacement period, boolean checkConflicts) {
        if (getMaxRooms()==0) return new HashSet();
        HashSet rooms = new HashSet(); int size = 0;
        loop: while (rooms.size()<getMaxRooms()) {
            int rx = ToolBox.random(getRoomPlacements().size());
            int minSize = (getSize()-size+(getMaxRooms()-rooms.size()-1)) / (getMaxRooms()-rooms.size());
            for (int r=0;r<getRoomPlacements().size();r++) {
                ExamRoomPlacement room = (ExamRoomPlacement)getRoomPlacements().elementAt((r+rx)%getRoomPlacements().size());
                int s = room.getSize(hasAltSeating());
                if (s<minSize) continue;
                if (!room.isAvailable(period.getPeriod())) continue;
                if (checkConflicts && room.getRoom().getPlacement(period.getPeriod())!=null) continue;
                if (rooms.contains(room)) continue;
                if (checkConflicts && !checkDistributionConstraints(room)) continue;
                size += s; rooms.add(room);
                if (size>=getSize()) {
                    for (Iterator j=rooms.iterator();j.hasNext();) {
                        ExamRoomPlacement rp = (ExamRoomPlacement)j.next();
                        if (size-rp.getSize(hasAltSeating())>=getSize()) { j.remove(); size-=rp.getSize(hasAltSeating()); }
                    }
                    return rooms;
                }
                continue loop;
            }
            break;
        }
        return null;
    }
    
    private HashSet iCorrelatedExams = null;
    /**
     * Number of exams that are correlated with this exam (there is at least one student attending both exams). 
     * @return number of correlated exams
     */
    public int nrStudentCorrelatedExams() {
        if (iCorrelatedExams==null) { 
            iCorrelatedExams = new HashSet();
            for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                iCorrelatedExams.addAll(student.variables());
            }
            iCorrelatedExams.remove(this);
        }
        return iCorrelatedExams.size();
    }
    
    private Integer iEstimatedDomainSize = null;
    private int estimatedDomainSize() {
        if (iEstimatedDomainSize==null) {
            int periods = getPeriodPlacements().size();
            int rooms = -1;
            int split = 0;
            while (rooms<split && split<=getMaxRooms()) {
                rooms=0; split++;
                for (Enumeration e=getRoomPlacements().elements();e.hasMoreElements();) {
                    ExamRoomPlacement room = (ExamRoomPlacement)e.nextElement();
                    if (room.getSize(hasAltSeating())>=(getSize()/split)) rooms++;
                }
            }
            iEstimatedDomainSize = new Integer(periods * rooms / split);
        }
        return iEstimatedDomainSize.intValue();
    }
    
    /** 
     * An exam with more correlated exams is preferred ({@link Exam#nrStudentCorrelatedExams()}).
     * If it is the same, ratio number of students / number of available periods is used.
     * If the same, exam ids are used.
     */
    public int compareTo(Object o) {
        Exam e = (Exam)o;
        int cmp = Double.compare(estimatedDomainSize(), e.estimatedDomainSize());
        if (cmp!=0) return cmp;
        cmp = -Double.compare(nrStudentCorrelatedExams(),e.nrStudentCorrelatedExams());
        if (cmp!=0) return cmp;
        cmp = -Double.compare(((double)getSize())/getPeriodPlacements().size(),((double)e.getSize())/e.getPeriodPlacements().size());
        if (cmp!=0) return cmp;
        return super.compareTo(o);
    }
    
    /**
     * True, if there is a student of this exam 
     * (that does not have direct conflicts allowed, see {@link ExamStudent#canConflict(Exam, Exam)}) 
     * that attends some other exam in the given period.
     * @param period a period
     * @return true if there is a student conflict
     */
    public boolean hasStudentConflictWithPreAssigned(ExamPeriod period) {
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            for (Iterator i=s.getExams(period).iterator();i.hasNext();) {
                Exam exam = (Exam)i.next();
                if (exam.equals(this)) continue;
                if (s.canConflict(this, exam)) continue;
            }
        }
        return false;
    }
    
    /**
     * Number of students of this exam 
     * (that does not have direct conflicts allowed, see {@link ExamStudent#canConflict(Exam, Exam)}) 
     * that attend some other exam in the given period.
     * @param period a period
     * @return number of direct student conflicts that are prohibited 
     */
    public int countStudentConflicts(ExamPeriodPlacement period) {
        int conf = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            for (Iterator i=s.getExams(period.getPeriod()).iterator();i.hasNext();) {
                Exam exam = (Exam)i.next();
                if (exam.equals(this)) continue;
                if (s.canConflict(this, exam)) continue;
                conf++;
            }
        }
        return conf;
    }
    
    /**
     * List of exams that are assigned to the given period and share one or more students with this exam 
     * (that does not have direct conflicts allowed, see {@link ExamStudent#canConflict(Exam, Exam)}).
     * @param period a period
     * @return list of {@link Exam} (other than this exam, that are placed in the given period and create
     * prohibited direct conflicts) 
     */
    public HashSet getStudentConflicts(ExamPeriod period) {
        HashSet conf = new HashSet();
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            for (Iterator i=s.getExams(period).iterator();i.hasNext();) {
                Exam exam = (Exam)i.next();
                if (exam.equals(this)) continue;
                if (!s.canConflict(this, exam)) conf.add(exam);
            }
        }
        return conf;
    }
    
    /**
     * Allow all direct student conflict for the given period (see {@link ExamStudent#canConflict(Exam, Exam)}).
     * @param period a period
     */
    public void allowAllStudentConflicts(ExamPeriod period) {
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            for (Iterator i=s.getExams(period).iterator();i.hasNext();) {
                Exam exam = (Exam)i.next();
                if (exam.equals(this)) continue;
                exam.setAllowDirectConflicts(true);
                setAllowDirectConflicts(true);
                s.setAllowDirectConflicts(true);
            }
        }
    }
    
    /**
     * String representation 
     * @return exam id  (periods: number of periods, rooms: number of rooms, student: number of students, maxRooms: max rooms[, alt if alternate seating is required]) 
     */
    public String toString() {
        return getName()+" (periods:"+getPeriodPlacements().size()+", rooms:"+getRoomPlacements().size()+", size:"+getSize()+" ,maxRooms:"+getMaxRooms()+(hasAltSeating()?", alt":"")+")";
    }
    
    /** Exam name */
    public String getName() { return (hasName()?iName:String.valueOf(getId())); }
    /** Exam name */
    public void setName(String name) { iName = name; }
    /** Exam name */
    public boolean hasName() { return iName != null && iName.length()>0; }
    
    private Hashtable iJenrls = null;
    /**
     * Joint enrollments 
     * @return table {@link Exam} (an exam that has at least one student in common with this exam) -> {@link Vector} (list of students in common)  
     */
    public Hashtable getJointEnrollments() {
        if (iJenrls!=null) return iJenrls;
        iJenrls = new Hashtable();
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (Enumeration f=student.variables().elements();f.hasMoreElements();) {
                Exam other = (Exam)f.nextElement();
                if (other.equals(this)) continue;
                Vector students = (Vector)iJenrls.get(other);
                if (students==null) {
                    students = new Vector();
                    iJenrls.put(other, students);
                }
                students.add(student);
            }
        }
        return iJenrls;
    }
    
    /**
     * Courses and/or sections that are having this exam
     * @return list of {@link ExamOwner} 
     */
    public Vector getOwners() {
        return iOwners;
    }

    /**
     * Courses/sections of this exam into which the given student is enrolled into
     * @param student a student that is enrolled into this exam
     * @return list of courses/sections {@link ExamOwner} which are having this exam with the given student enrolled in  
     */
    public Vector getOwners(ExamStudent student) {
        Vector ret = new Vector();
        for (Enumeration e=iOwners.elements();e.hasMoreElements();) {
            ExamOwner owner = (ExamOwner)e.nextElement();
            if (owner.getStudents().contains(student)) ret.add(owner);
        }
        return ret;
    }

    /**
     * Courses/sections of this exam into which the given instructor is enrolled into
     * @param instructor an instructor that is enrolled into this exam
     * @return list of courses/sections {@link ExamOwner} which are having this exam with the given instructor enrolled in  
     */
    public Vector getOwners(ExamInstructor instructor) {
        Vector ret = new Vector();
        for (Enumeration e=iOwners.elements();e.hasMoreElements();) {
            ExamOwner owner = (ExamOwner)e.nextElement();
            if (owner.getIntructors().contains(instructor)) ret.add(owner);
        }
        return ret;
    }
    
    /**
     * Returns appropriate {@link ExamPeriodPlacement} for the given period, if it is available for this exam, null otherwise.
     */
    public ExamPeriodPlacement getPeriodPlacement(Long periodId) {
        for (Enumeration e=iPeriodPlacements.elements();e.hasMoreElements();) {
            ExamPeriodPlacement periodPlacement = (ExamPeriodPlacement)e.nextElement();
            if (periodPlacement.getId().equals(periodId)) return periodPlacement;
        }
        return null;
    }
    
    /**
     * Returns appropriate {@link ExamRoomPlacement} for the given room, if it is available for this exam, null otherwise.
     */
    public ExamRoomPlacement getRoomPlacement(long roomId) {
        for (Enumeration e=iRoomPlacements.elements();e.hasMoreElements();) {
            ExamRoomPlacement roomPlacement = (ExamRoomPlacement)e.nextElement();
            if (roomPlacement.getId()==roomId) return roomPlacement;
        }
        return null;
    }


    /**
     * Returns appropriate {@link ExamPeriodPlacement} for the given period, if it is available for this exam, null otherwise.
     */
    public ExamPeriodPlacement getPeriodPlacement(ExamPeriod period) {
        for (Enumeration e=getPeriodPlacements().elements();e.hasMoreElements();) {
            ExamPeriodPlacement periodPlacement = (ExamPeriodPlacement)e.nextElement();
            if (periodPlacement.getPeriod().equals(period)) return periodPlacement;
        }
        return null;
    }

    /**
     * Returns appropriate {@link ExamRoomPlacement} for the given room, if it is available for this exam, null otherwise.
     */
    public ExamRoomPlacement getRoomPlacement(ExamRoom room) {
        for (Enumeration e=getRoomPlacements().elements();e.hasMoreElements();) {
            ExamRoomPlacement roomPlacement = (ExamRoomPlacement)e.nextElement();
            if (roomPlacement.getRoom().equals(room)) return roomPlacement;
        }
        return null;
    }
 
    /** Return true if there are some values in the domain of this variable */
    public boolean hasValues() {
        return !getPeriodPlacements().isEmpty() && (getMaxRooms()==0 || !getRoomPlacements().isEmpty());
    }
    
    public void assign(long iteration, Value value) {
        if (getMaxRooms()>0) {
            ExamPlacement placement = (ExamPlacement)value;
            int size = 0;
            for (Iterator i=placement.getRoomPlacements().iterator();i.hasNext();) {
                ExamRoomPlacement room = (ExamRoomPlacement)i.next();
                size += room.getSize(hasAltSeating());
            }
            if (size<getSize() && !placement.getRoomPlacements().isEmpty()) {
                Progress.getInstance(getModel()).warn("Selected room(s) are too small "+size+"<"+getSize()+" ("+getName()+" "+placement.getName()+")");
            }
        }
        super.assign(iteration, value);
    }
}
