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
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.coursett.preference.MinMaxPreferenceCombination;

/**
 * Representation of an exam (problem variable).
 * Each exam has defined a length (in minutes), type (whether it is a section or a course exam),
 * seating type (whether it requires normal or alternate seating) and a maximal number of rooms.
 * If the maximal number of rooms is zero, the exam will be timetabled only in time (it does not
 * require a room).  
 * <br><br>
 * An exam can be only assigned to a period {@link ExamPeriod} that is long enough (see {@link ExamPeriod#getLength()}) 
 * that is available (see {@link Exam#setAvailable(int, boolean)}, {@link Exam#isAvailable(ExamPeriod)}).
 * An exam can have one period pre-assigned (see {@link Exam#setPreAssignedPeriod(ExamPeriod)}, 
 * {@link Exam#hasPreAssignedPeriod()}, {@link Exam#getPreAssignedPeriod()}). In such a case, the exam
 * can only be assigned to the pre-assigned period. 
 * <br><br>
 * A set of rooms that are available in the given period (see {@link ExamRoom#isAvailable(ExamPeriod)}), 
 * and which together cover the size of exam (number of students attending the exam) has to be
 * assigned to an exam. Based on the type of seating (see {@link Exam#hasAltSeating()}), either
 * room sizes (see {@link ExamRoom#getSize()}) or alternative seating sizes (see {@link ExamRoom#getAltSize()}) 
 * are used. An exam has one or more room groups associated with it (see {@link Exam#addRoomGroup(ExamRoomGroup)},
 * {@link Exam#getRoomGroups()}). Only rooms of room groups that are associated with the exam can be used
 * for room assignments. The only exception to this rule is the original room of a section exam
 * (i.e., the room where the section was timetabled). If such room is provided (see
 * {@link Exam#setOriginalRoom(ExamRoom)}, {@link Exam#getOriginalRoom()}), and it is of enough 
 * size, an exam is preferred to be assigned into this original room.  
 * An exam can have a set of rooms pre-assiged (see {@link Exam#setPreAssignedPeriod(ExamPeriod)},
 * {@link Exam#hasPreAssignedRooms()}, {@link Exam#getPreassignedRooms()}). In such a case, the exam
 * can only be assigned to the provided pre-assigned rooms (all of them).
 * <br><br>
 * Various penalties for an assignment of a period or a set of rooms may apply. See {@link ExamPlacement} 
 * for more details.
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
public class Exam extends Variable {
    private static boolean sAlterMaxSize = false;
    private static Logger sLog = Logger.getLogger(Exam.class);
    protected static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",new java.text.DecimalFormatSymbols(Locale.US));
    private Vector iStudents = new Vector();
    private Vector iInstructors = new Vector();
    private Vector iDistConstraints = new Vector();
    private boolean iAllowDirectConflicts = true;
    private boolean iSectionExam = false;
    private boolean iAltSeating = false;
    private int iAveragePeriod = -1;
    private ExamPeriod iPreAssignedPeriod = null;
    private int iLength = 0;
    private int iMaxRooms = 0;
    private Vector iRoomGroups = new Vector();
    private ExamRoom iOriginalRoom = null;
    private Vector iPreassignedRooms = new Vector();
    private boolean iAvailable[] = null;
    private int iWeight[] = null;
    private Vector iRooms = null;
    private Vector iPeriods = null;
    private Integer sPenaltyFactor = null;
    private String iName = null;
    private Vector iCourseSections = new Vector();
    private Hashtable iRoomWeights = null;
    
    /**
     * Constructor
     * @param id exam unique id
     * @param length exam length in minutes
     * @param sectionExam true if section exam, false if course exam
     * @param altSeating true if alternative seating is requested
     * @param maxRooms maximum number of rooms to be used
     */
    public Exam(long id, String name, int length, boolean sectionExam, boolean altSeating, int maxRooms) {
        super();
        iId = id;
        iName = name;
        iLength = length;
        iSectionExam = sectionExam;
        iAltSeating = altSeating;
        iMaxRooms = maxRooms;
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
     * Return list of possible rooms. If an exam is pre-assigned in space, the resultant list contains only the 
     * pre-assigned room(s). Otherwise, a union of all rooms of all room groups {@link Exam#getRoomGroups()} are 
     * returned. If the exam has an original room (see {@link Exam#getOriginalRoom()}) and it is large 
     * enough to take the exam, it is also included in the resultant list even if it is not from on of
     * exam's room groups.
     * @return list of {@link ExamRoom} ordered from the largest to the smallest one 
     * (using either {@link ExamRoom#getSize()} or {@link ExamRoom#getAltSize()} based on {@link Exam#hasAltSeating()})
     */
    public Vector getRooms() {
        if (iRooms==null) {
            if (getMaxRooms()==0) iRooms=new Vector(0);
            else if (hasPreAssignedRooms()) {
                iRooms = new Vector(getPreassignedRooms());
            } else if (iRoomWeights!=null) {
                iRooms=new Vector(iRoomWeights.size());
                iRooms.addAll(iRoomWeights.keySet());
            } else {
                HashSet rooms = new HashSet();
                for (Enumeration e=getRoomGroups().elements();e.hasMoreElements();) {
                    ExamRoomGroup rg = (ExamRoomGroup)e.nextElement();
                    rooms.addAll(rg.getRooms());
                }
                if (getOriginalRoom()!=null && !rooms.contains(getOriginalRoom())) {
                    if ((hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size())
                        rooms.add(getOriginalRoom());
                }
                iRooms = new Vector(rooms);
            }
            Collections.sort(iRooms, new Comparator() {
                public int compare(Object o1, Object o2) {
                    ExamRoom r1 = (ExamRoom)o1;
                    ExamRoom r2 = (ExamRoom)o2;
                    int s1 = (hasAltSeating()?r1.getAltSize():r1.getSize());
                    int s2 = (hasAltSeating()?r2.getAltSize():r2.getSize());
                    int cmp = -Double.compare(s1, s2);
                    if (cmp!=0) return cmp;
                    return r1.compareTo(r2);
                }
            });
            if (sAlterMaxSize && iRooms.size()>50) {
                ExamRoom med = (ExamRoom)iRooms.elementAt(Math.min(50, iRooms.size()/2));
                int medSize = (hasAltSeating()?med.getAltSize():med.getSize());
                setMaxRooms(Math.min(getMaxRooms(),1+(getStudents().size()/medSize)));
            }
        }
        return iRooms;
    }
    
    /**
     * Return list of possible periods. If the exam has a period pre-assigned (see {@link Exam#hasPreAssignedPeriod()}),
     * the resultant list only contains the pre-assigned period {@link Exam#getPreAssignedPeriod()}). 
     * Periods, that are not available ({@link Exam#isAvailable(ExamPeriod)} is false) are excluded from the list.
     * @return list of {@link ExamPeriod}, ordered by {@link ExamPeriod#getIndex()}
     */
    public Vector getPeriods() {
        if (iPeriods==null) {
            if (hasPreAssignedPeriod()) {
                iPeriods=new Vector(1);
                iPeriods.add(getPreAssignedPeriod());
            } else {
                iPeriods = new Vector();
                for (Enumeration e=((ExamModel)getModel()).getPeriods().elements();e.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)e.nextElement();
                    if (period.getLength()<getLength()) continue;
                    if (isAvailable(period)) iPeriods.add(period);
                }
            }
        }
        return iPeriods;
    }
    
    /** 
     * Initialize exam's domain. 
     */ 
    private boolean init() {
        ExamModel model = (ExamModel)getModel();
        Vector values = new Vector();
        if (getMaxRooms()==0) {
            if (hasPreAssignedPeriod()) {
                values.addElement(new ExamPlacement(this, getPreAssignedPeriod(), new HashSet()));
            } else {
                for (Enumeration e=model.getPeriods().elements();e.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)e.nextElement();
                    if (isAvailable(period))
                        values.addElement(new ExamPlacement(this, period, new HashSet()));
                }
            }
        } else if (hasPreAssignedRooms()) {
            if (hasPreAssignedPeriod()) {
                values.addElement(new ExamPlacement(this, getPreAssignedPeriod(), new HashSet(getPreassignedRooms())));
            } else {
                for (Enumeration e=model.getPeriods().elements();e.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)e.nextElement();
                    if (period.getLength()<getLength()) continue;
                    if (isAvailable(period))
                        values.addElement(new ExamPlacement(this, period, new HashSet(getPreassignedRooms())));
                }
            }
        } else {
            sLog.debug("Processing exam "+getName()+" ("+getStudents().size()+" students"+(hasAltSeating()?", alt":"")+") ...");
            if (getRooms().isEmpty()) {
                sLog.error("  Exam "+getName()+" has no rooms.");
                setValues(new Vector(0));
                return false;
            }
            TreeSet roomSets = new TreeSet();
            boolean norp = (getOriginalRoom()!=null && (hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size());
            genRoomSets(roomSets, 0, getRooms(), getMaxRooms(), new HashSet(), 0, norp);
            if (roomSets.isEmpty()) {
                sLog.error("  Exam "+getName()+" has no room placements.");
                setValues(new Vector(0));
                return false;
            }
            RoomSet first = (RoomSet)roomSets.first();
            RoomSet last = (RoomSet)roomSets.last();
            sLog.debug("  Exam "+getName()+" ("+getStudents().size()+" students, max rooms is "+getMaxRooms()+(hasAltSeating()?", alt":"")+") has "+roomSets.size()+" room placements ("+first.rooms().size()+"/"+sDoubleFormat.format(first.penalty())+"..."+last.rooms().size()+"/"+sDoubleFormat.format(last.penalty())+").");
            for (Iterator i=roomSets.iterator();i.hasNext();) {
                RoomSet roomSet = (RoomSet)i.next();
                if (hasPreAssignedPeriod()) {
                    if (isAvailable(getPreAssignedPeriod(), roomSet.rooms()))
                        values.addElement(new ExamPlacement(this, getPreAssignedPeriod(), roomSet.rooms()));
                } else {
                    for (Enumeration f=model.getPeriods().elements();f.hasMoreElements();) {
                        ExamPeriod period = (ExamPeriod)f.nextElement();
                        if (isAvailable(period, roomSet.rooms()))
                            values.addElement(new ExamPlacement(this, period, roomSet.rooms()));
                    }
                }
            }
        }
        if (values.isEmpty()) sLog.error("Exam "+getName()+" has no placement.");
        setValues(values);
        return !values.isEmpty();
    }
    
    private void genRoomSets(TreeSet roomSets, int roomIdx, Vector rooms, int maxRooms, Set roomsSoFar, int sizeSoFar, boolean norp) {
        ExamModel model = (ExamModel)getModel();
        if (sizeSoFar>=getStudents().size()) {
            int nrRooms = roomsSoFar.size();
            int nrRooms2 = (nrRooms>1?nrRooms*(nrRooms-1)/2:1);
            double penalty = 
                model.getRoomSplitWeight() * getRoomSplitPenalty(roomsSoFar) +
                model.getRoomSizeWeight() * getRoomSizePenalty(sizeSoFar) +
                (norp?model.getNotOriginalRoomWeight() * getNotOriginalRoomPenalty(roomsSoFar):0);
            if (roomSets.size()>=rooms.size()) {
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
        for (int i=0;i<maxRooms && roomIdx+i<rooms.size();i++)
            sizeBound += (hasAltSeating()?((ExamRoom)rooms.elementAt(roomIdx+i)).getAltSize():((ExamRoom)rooms.elementAt(roomIdx+i)).getSize());
        while (roomIdx<rooms.size()) {
            if (sizeBound<getStudents().size()) break;
            ExamRoom room = (ExamRoom)rooms.elementAt(roomIdx);
            roomsSoFar.add(room);
            genRoomSets(
                    roomSets, roomIdx+1, rooms, maxRooms-1, 
                    roomsSoFar, sizeSoFar+(hasAltSeating()?room.getAltSize():room.getSize()), 
                    norp
                    );
            roomsSoFar.remove(room);
            sizeBound -= (hasAltSeating()?room.getAltSize():room.getSize());
            if (roomIdx+maxRooms<rooms.size())
                sizeBound += (hasAltSeating()?((ExamRoom)rooms.elementAt(roomIdx+maxRooms)).getAltSize():((ExamRoom)rooms.elementAt(roomIdx+maxRooms)).getSize());
            roomIdx++;
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
            int cmp = Double.compare(penalty(), penalty);
            if (cmp!=0) return cmp;
            return rooms().toString().compareTo(rooms.toString());
        }
        public int compareTo(Object o) {
            RoomSet r = (RoomSet)o;
            return compareTo(r.rooms(),r.penalty());
        }
    }
    
    /**
     * Compute room penalty of a set of rooms. 
     * @param rooms set of rooms to be assigned to the exam
     * @return penalty
     */
    public double getRoomPenalty(Set rooms) {
        ExamModel model = (ExamModel)getModel();
        return
            model.getRoomSizeWeight() * getRoomSizePenalty(rooms) +
            model.getRoomSplitWeight() * getRoomSplitPenalty(rooms)+
            model.getNotOriginalRoomWeight() * getNotOriginalRoomPenalty(rooms);
    }
    
    /**
     * Cost for using more than one room.
     * @param rooms set of rooms to be assigned to the exam
     * @return penalty (1 for 2 rooms, 2 for 3 rooms, 4 for 4 rooms, etc.)
     */
    public int getRoomSplitPenalty(Set rooms) {
        if (rooms.size()<=1) return 0;
        return (1 << (rooms.size()-2));
    }
    
    /**
     * Cost for using room(s) different from the original room
     * @param rooms set of rooms to be assigned to the exam
     * @return 1 if there is an original room and it is of enough capacity, 
     * but the given set of rooms is not composed of the original room; zero otherwise
     */
    public int getNotOriginalRoomPenalty(Set rooms) {
        if (getOriginalRoom()==null) return 0;
        if (getMaxRooms()==0) return 0;
        if ((hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size()) return 0;
        return (rooms.size()==1 && rooms.contains(getOriginalRoom())?0:1);
    }
    
    /**
     * Cost for using a period
     * @param period a period to be assigned to the exam
     * @return {@link ExamPeriod#getWeight()}
     */
    public int getPeriodPenalty(ExamPeriod period, Set rooms) {
        MinMaxPreferenceCombination comb = new MinMaxPreferenceCombination();
        comb.addPreferenceInt(getWeight(period));
        if (rooms!=null && !rooms.isEmpty())
            for (Iterator i=rooms.iterator();i.hasNext();) {
                ExamRoom r = (ExamRoom)i.next();
                comb.addPreferenceInt(r.getWeight(period));
            }
        comb.addPreferenceInt(period.getWeight());
        return comb.getPreferenceInt();
    }
    
    /**
     * Rotation penalty (an exam that has been in later period last times tries to be in an earlier period)
     * @param period a period to be assigned to the exam
     * @return period index &times; average period
     */
    public int getRotationPenalty(ExamPeriod period) {
        if (iAveragePeriod<0) return 0;
        //return (1+period.getWeight())*iAveragePeriod;
        return period.getIndex()*iAveragePeriod;
    }
    
    /**
     * Cost for using room(s) that are too big
     * @param size total size of rooms to be assigned to the exam (using either {@link ExamRoom#getSize()} or 
     * {@link ExamRoom#getAltSize()} based on {@link Exam#hasAltSeating()})
     * @return difference between given size and the number of students
     */
    public int getRoomSizePenalty(int size) {
        int diff = size-getStudents().size();
        return (diff<0?0:diff);
    }
    
    /**
     * Cost for using room(s) that are too big
     * @param rooms set of rooms to be assigned to the exam 
     * @return difference between total room size (computed using either {@link ExamRoom#getSize()} or 
     * {@link ExamRoom#getAltSize()} based on {@link Exam#hasAltSeating()}) and the number of students
     */
    public int getRoomSizePenalty(Set rooms) {
        int size = 0;
        for (Iterator i=rooms.iterator();i.hasNext();) {
            ExamRoom room = (ExamRoom)i.next();
            size += (hasAltSeating()?room.getAltSize():room.getSize());
        }
        return getRoomSizePenalty(size);
    }
    
    /**
     * Sets whether the exam is available (can be timetabled) in the given period 
     * @param period a period
     * @param avail true if the exam can be timetable, false otherwise
     */
    public void setAvailable(int period, boolean avail) {
        if (iAvailable==null) {
            iAvailable = new boolean[((ExamModel)getModel()).getNrPeriods()];
            for (int i=0;i<iAvailable.length;i++)
                iAvailable[i]=true;
        }
        iAvailable[period]=avail;
    }

    /**
     * True if the exam can be timetable in the given period (this method does not check pre-assigned period,
     * or the length of the period)
     * @param period
     * @return true, if the given period is available for this exam 
     */
    public boolean isAvailable(ExamPeriod period) {
        int periodProhibitedWeight = ((ExamModel)getModel()).getPeriodProhibitedWeight();
        if (periodProhibitedWeight>0 && !period.equals(iPreAssignedPeriod) && period.getWeight()>=periodProhibitedWeight) return false;
        return (iAvailable==null?true:iAvailable[period.getIndex()]); 
    }
    
    /** Set period weight */
    public void setWeight(int period, int weight) {
        if (iWeight==null) {
            iWeight = new int[((ExamModel)getModel()).getNrPeriods()];
            for (int i=0;i<iWeight.length;i++)
                iWeight[i]=0;
        }
        iWeight[period]=weight;
    }

    /** Return period weight */
    public int getWeight(ExamPeriod period) {
        if (iWeight==null) return 0;
        return iWeight[period.getIndex()];
    }
    
    /**
     * True if the exam can be timetable in the given period and the given set of rooms (i.e., 
     * {@link Exam#isAvailable(ExamPeriod)} and {@link ExamRoom#isAvailable(ExamPeriod)} for each
     * room in the set)
     * @param period a period to be assigned to the exam
     * @param rooms a set of rooms to be assigned to the exam
     * @return true if the exam is available and all the given rooms are available 
     */
    public boolean isAvailable(ExamPeriod period, Set rooms) {
        if (!isAvailable(period)) return false;
        for (Iterator i=rooms.iterator();i.hasNext();) {
            ExamRoom room = (ExamRoom)i.next();
            if (!room.isAvailable(period)) return false;
        }
        return true;
    }
    
    /**
     * True if the exam is section exam, false if it is course exam
     * @return true if the exam is section exam, false if it is course exam
     */
    public boolean isSectionExam() {
        return iSectionExam;
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
     * Set pre-assigned period. If a period is pre-assigned, the exam can only be assigned to the 
     * pre-assigned period.
     * @param period pre-assigned period or null for unsetting the pre-assigned period
     */
    public void setPreAssignedPeriod(ExamPeriod period) {
        iPreAssignedPeriod = period;
    }
    /** 
     * Pre-assigned period. If a period is pre-assigned, the exam can only be assigned to the 
     * pre-assigned period.
     * @return pre-assigned period or null if there is none
     */
    public ExamPeriod getPreAssignedPeriod() {
        return iPreAssignedPeriod;
    }
    /**
     * True if there is a period pre-assigned for the exam
     * @return true if there is a period pre-assigned for the exam, false otherwise
     */
    public boolean hasPreAssignedPeriod() {
        return iPreAssignedPeriod!=null;
    }
    
    /**
     * Set average period. This represents an average period that the exam was assigned to in the past.
     * It may be used to weight period penalty {@link Exam#getPeriodPenalty(ExamPeriod, Set)} in order to
     * put more weight on exams that were badly assigned last time(s) and ensuring some form of
     * fairness.
     * @param period average period
     */
    public void setAveragePeriod(int period) {
        iAveragePeriod = period;
    }
    
    /**
     * Average period. This represents an average period that the exam was assigned to in the past.
     * It may be used to weight period penalty {@link Exam#getPeriodPenalty(ExamPeriod, Set)} in order to
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
     * It is used to weight period penalty {@link Exam#getPeriodPenalty(ExamPeriod, Set)} in order to
     * put more weight on exams that were badly assigned last time(s) and ensuring some form of
     * fairness. If there is no average period, number of periods /2 is to be used in the
     * period penalty {@link Exam#getPeriodPenalty(ExamPeriod, Set)}. 
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
    public boolean checkDistributionConstraints(ExamPeriod period) {
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
    public boolean checkDistributionConstraints(ExamRoom room) {
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
                        if (!placement.getRooms().contains(room)) return false;
                        break;
                    case ExamDistributionConstraint.sDistDifferentRoom :
                        if (placement.getRooms().contains(room)) return false;
                        break;
                }
            }
        }
        return true;
    }
        
    /**
     * List of room groups associated with the exam. Only rooms of the given room groups can be used 
     * for placing this exam.
     * @return list of {@link ExamRoomGroup}
     */
    public Vector getRoomGroups() { return iRoomGroups; }
    public void addRoomGroup(ExamRoomGroup rg) { 
        if (!iRoomGroups.contains(rg)) iRoomGroups.add(rg);
    }
    
    /**
     * Original room of a section exam. If the original room is set and it is of enough size, 
     * it is preferred to assign the exam into the original room. 
     * @return original room
     */
    public ExamRoom getOriginalRoom() {
        return iOriginalRoom;
    }
    
    /**
     * Set original room of a section exam. If the original room is set and it is of enough size, 
     * it is preferred to assign the exam into the original room.
     * @param room original room or null to unset original room
     */
    public void setOriginalRoom(ExamRoom room) {
        iOriginalRoom = room;
    }
    
    /** 
     * List of rooms that are pre-assigned to the exam. If an exam has one or more rooms pre-assigned,
     * it has to be assigned to pre-assigned rooms (all of them). The room capacity check is omnited
     * in such a case.
     * @return list of {@link ExamRoom}
     */
    public Vector getPreassignedRooms() {
        return iPreassignedRooms;
    }

    /** 
     * True, if there are one or more rooms that are pre-assigned to the exam. 
     * @return true if there are pre-assigned rooms, false otherwise
     */
    public boolean hasPreAssignedRooms() {
        return !iPreassignedRooms.isEmpty();
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
     * total smallest size is preferred. If the original room is available and of enough size, it is retuned. 
     * All necessary checks are made (avaiability of rooms, pre-assigned rooms, room sizes etc.).
     * @param period given period.
     * @return best available rooms for the exam in the given period, null if there is no valid assignment
     */
    public Set findBestAvailableRooms(ExamPeriod period) {
        if (!isAvailable(period)) return null;
        if (hasPreAssignedPeriod() && !period.equals(getPreAssignedPeriod())) return null;
        if (getMaxRooms()==0) return new HashSet();
        if (hasPreAssignedRooms()) {
            for (Enumeration e=getPreassignedRooms().elements();e.hasMoreElements();) {
                ExamRoom room = (ExamRoom)e.nextElement();
                if (room.getPlacement(period)!=null) return null;
                if (!checkDistributionConstraints(room)) return null;
            }
            return new HashSet(getPreassignedRooms());
        }
        if (getOriginalRoom()!=null && getOriginalRoom().isAvailable(period) &&
            getOriginalRoom().getPlacement(period)==null &&
           (hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size() &&
           checkDistributionConstraints(getOriginalRoom())) {
                HashSet rooms = new HashSet(); rooms.add(getOriginalRoom());
                return rooms;
        }
        loop: for (int nrRooms=1;nrRooms<=getMaxRooms();nrRooms++) {
            HashSet rooms = new HashSet(); int size = 0;
            while (rooms.size()<nrRooms && size<getStudents().size()) {
                int minSize = (getStudents().size()-size)/(nrRooms-rooms.size());
                ExamRoom best = null; int bestSize = 0;
                for (Enumeration e=getRooms().elements();e.hasMoreElements();) {
                    ExamRoom room = (ExamRoom)e.nextElement();
                    if (!room.isAvailable(period)) continue;
                    if (room.getPlacement(period)!=null) continue;
                    if (rooms.contains(room)) continue;
                    if (!checkDistributionConstraints(room)) continue;
                    int s = (hasAltSeating()?room.getAltSize():room.getSize());
                    if (s<minSize) break;
                    if (best==null || bestSize>s) {
                        best = room;
                        bestSize = s;
                    }
                }
                if (best==null) continue loop;
                rooms.add(best); size+=bestSize;
            }
            if (size>=getStudents().size()) return rooms;
        }
        return null;
    }
    
    /**
     * Randomly find a set of available rooms for the exam in the given period. First of all, it tries to find the minimal
     * number of rooms that cover the size of the exam. Among these, a set of rooms of 
     * total smallest size is preferred.
     * All necessary checks are made (avaiability of rooms, pre-assigned rooms, room sizes etc.). 
     * @param period given period.
     * @return randomly computed set of available rooms for the exam in the given period, null if there is no valid assignment
     */
    public Set findRoomsRandom(ExamPeriod period) {
        if (!isAvailable(period)) return null;
        if (hasPreAssignedPeriod() && !period.equals(getPreAssignedPeriod())) return null;
        if (getMaxRooms()==0) return new HashSet();
        if (hasPreAssignedRooms()) {
            for (Enumeration e=getPreassignedRooms().elements();e.hasMoreElements();) {
                ExamRoom room = (ExamRoom)e.nextElement();
                if (room.getPlacement(period)!=null) return null;
                if (!checkDistributionConstraints(room)) return null;
            }
            return new HashSet(getPreassignedRooms());
        }
        int exSize = getStudents().size();
        HashSet rooms = new HashSet(); int size = 0; int minSize = Integer.MAX_VALUE;
        if (getOriginalRoom()!=null && getOriginalRoom().isAvailable(period) &&
            getOriginalRoom().getPlacement(period)==null &&
            (hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size() &&
            checkDistributionConstraints(getOriginalRoom()) &&
            ToolBox.random()<0.5) {
            rooms.add(getOriginalRoom());
            return rooms;
        }
        loop: while (true) {
            int rx = ToolBox.random(getRooms().size()); 
            for (int r=0;r<getRooms().size();r++) {
                ExamRoom room = (ExamRoom)getRooms().elementAt((r+rx)%getRooms().size());
                if (!room.isAvailable(period)) continue;
                if (room.getPlacement(period)!=null) continue;
                if (rooms.contains(room)) continue;
                if (!checkDistributionConstraints(room)) continue;
                int s = (hasAltSeating()?room.getAltSize():room.getSize());
                if (size+s>=exSize && size+s-exSize<minSize) {
                    rooms.add(room); return rooms;
                }
                if (rooms.size()+1<getMaxRooms()) {
                    minSize = Math.min(minSize, s);
                    rooms.add(room); continue loop;
                }
            }
            return null;
        }
    }
    
    private HashSet iCorrelatedExams = null;
    /**
     * Number of exams that are correlated with this exam (there is at least one student attending both exams). 
     * @return number of correlated exams
     */
    public int nrStudentCorrelatedExams() {
        if (iCorrelatedExams==null) { 
            iCorrelatedExams = new HashSet();
            int weightedNrCorrelatedEvents = 0; 
            for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                iCorrelatedExams.addAll(student.variables());
            }
            iCorrelatedExams.remove(this);
        }
        return iCorrelatedExams.size();
    }
    
    /** 
     * An exam with more correlated exams is preferred ({@link Exam#nrStudentCorrelatedExams()}).
     * If it is the same, ratio number of students / number of available periods is used.
     * If the same, exam ids are used.
     */
    public int compareTo(Object o) {
        Exam e = (Exam)o;
        int cmp = -Double.compare(nrStudentCorrelatedExams(),e.nrStudentCorrelatedExams());
        if (cmp!=0) return cmp;
        cmp = -Double.compare(((double)getStudents().size())/getPeriods().size(),((double)e.getStudents().size())/e.getPeriods().size());
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
                if (exam.hasPreAssignedPeriod()) return true;
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
    public int countStudentConflicts(ExamPeriod period) {
        int conf = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            for (Iterator i=s.getExams(period).iterator();i.hasNext();) {
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
        int inc = (getAssignment()!=null && ((ExamPlacement)getAssignment()).getPeriod().equals(period)?0:1);
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
        return getName()+" (periods:"+getPeriods().size()+", rooms:"+getRooms().size()+", student:"+getStudents().size()+" ,maxRooms:"+getMaxRooms()+(hasAltSeating()?", alt":"")+")";
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
     * @return list of {@link ExamCourseSection} 
     */
    public Vector getCourseSections() {
        return iCourseSections;
    }

    /**
     * Courses/sections of this exam into which the given student is enrolled into
     * @param student a student that is enrolled into this exam
     * @return list of courses/sections {@link ExamCourseSection} which are having this exam with the given student enrolled in  
     */
    public Vector getCourseSections(ExamStudent student) {
        Vector ret = new Vector();
        for (Enumeration e=iCourseSections.elements();e.hasMoreElements();) {
            ExamCourseSection cs = (ExamCourseSection)e.nextElement();
            if (cs.getStudents().contains(student)) ret.add(cs);
        }
        return ret;
    }

    /**
     * Courses/sections of this exam into which the given instructor is enrolled into
     * @param instructor an instructor that is enrolled into this exam
     * @return list of courses/sections {@link ExamCourseSection} which are having this exam with the given instructor enrolled in  
     */
    public Vector getCourseSections(ExamInstructor instructor) {
        Vector ret = new Vector();
        for (Enumeration e=iCourseSections.elements();e.hasMoreElements();) {
            ExamCourseSection cs = (ExamCourseSection)e.nextElement();
            if (cs.getIntructors().contains(instructor)) ret.add(cs);
        }
        return ret;
    }
    
    /** Provide list of available rooms with their weights
     * @param roomWeights table {@link ExamRoom} : weight {@link Integer}
     **/
    public void setRoomWeights(Hashtable roomWeights) {
        iRoomWeights = roomWeights;
    }
    
    /** Get list of available rooms with their weights (if it was provided)
     * @return table {@link ExamRoom} : weight {@link Integer}
     **/
    public Hashtable getRoomWeights() {
        return iRoomWeights;
    }
    
    /** Get room weight */
    public int getWeight(ExamRoom room) {
        if (iRoomWeights==null) return 0;
        Integer weight = (Integer)iRoomWeights.get(room);
        return (weight==null?0:weight.intValue());
    }
    
    /**
     * Combined room weight for the case when multiple rooms are used 
     */
    public int getRoomWeight(Set rooms) {
        MinMaxPreferenceCombination comb = new MinMaxPreferenceCombination();
        for (Iterator i=rooms.iterator();i.hasNext();) {
            ExamRoom r = (ExamRoom)i.next();
            comb.addPreferenceInt(getWeight(r));
        }
        return comb.getPreferenceInt();
    }

}
