package org.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.ClassLimitConstraint;
import org.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import org.cpsolver.coursett.constraint.FlexibleConstraint;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.constraint.IgnoreStudentConflictsConstraint;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.constraint.RoomConstraint;
import org.cpsolver.coursett.constraint.SpreadConstraint;
import org.cpsolver.coursett.criteria.StudentCommittedConflict;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.VariableWithContext;
import org.cpsolver.ifs.constant.ConstantVariable;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Lecture (variable).
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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

public class Lecture extends VariableWithContext<Lecture, Placement, Lecture.LectureContext> implements ConstantVariable<Placement> {
    private Long iClassId;
    private Long iSolverGroupId;
    private Long iSchedulingSubpartId;
    private String iName;
    private Long iDept;
    private Long iScheduler;
    private List<TimeLocation> iTimeLocations;
    private List<RoomLocation> iRoomLocations;
    private String iNote = null;

    private int iMinClassLimit;
    private int iMaxClassLimit;
    private float iRoomToLimitRatio;
    private int iNrRooms;
    private int iOrd;
    private double iWeight = 1.0;
    private boolean iSplitAttendance = false;

    private Set<Student> iStudents = new HashSet<Student>();
    private DepartmentSpreadConstraint iDeptSpreadConstraint = null;
    private Set<SpreadConstraint> iSpreadConstraints = new HashSet<SpreadConstraint>();
    private Set<Constraint<Lecture, Placement>> iWeakeningConstraints = new HashSet<Constraint<Lecture, Placement>>();
    private List<InstructorConstraint> iInstructorConstraints = new ArrayList<InstructorConstraint>();
    private AtomicReference<Set<Long>> iIgnoreStudentConflictsWith = new AtomicReference<Set<Long>>();
    private ClassLimitConstraint iClassLimitConstraint = null;

    private Lecture iParent = null;
    private HashMap<Long, List<Lecture>> iChildren = null;
    private java.util.List<Lecture> iSameSubpartLectures = null;
    private Configuration iParentConfiguration = null;

    private List<JenrlConstraint> iJenrlConstraints = new ArrayList<JenrlConstraint>();
    private HashMap<Lecture, JenrlConstraint> iJenrlConstraintsHash = new HashMap<Lecture, JenrlConstraint>();
    private HashMap<Placement, Integer> iCommitedConflicts = new HashMap<Placement, Integer>();
    private Set<GroupConstraint> iGroupConstraints = new HashSet<GroupConstraint>();
    private Set<GroupConstraint> iHardGroupSoftConstraints = new HashSet<GroupConstraint>();
    private Set<GroupConstraint> iCanShareRoomGroupConstraints = new HashSet<GroupConstraint>();
    private Set<FlexibleConstraint> iFlexibleGroupConstraints = new HashSet<FlexibleConstraint>();    

    public boolean iCommitted = false;

    public static boolean sSaveMemory = false;
    private int iMaxRoomCombinations = -1;

    private Integer iCacheMinRoomSize = null;
    private Integer iCacheMaxRoomSize = null;
    private Integer iCacheMaxAchievableClassLimit = null;
    
    private final ReentrantReadWriteLock iLock = new ReentrantReadWriteLock();

    /**
     * Constructor
     * 
     * @param id class unique id
     * @param solverGroupId solver group unique id 
     * @param schedulingSubpartId  scheduling subpart unique id
     * @param name class name
     * @param timeLocations set of time locations
     * @param roomLocations set of room location
     * @param nrRooms number of rooms into which the class is to be assigned
     * @param initialPlacement initial placement
     * @param minClassLimit minimum class limit
     * @param maxClassLimit maximum class limit
     * @param room2limitRatio room ratio
     */
    public Lecture(Long id, Long solverGroupId, Long schedulingSubpartId, String name,
            Collection<TimeLocation> timeLocations, Collection<RoomLocation> roomLocations, int nrRooms,
            Placement initialPlacement, int minClassLimit, int maxClassLimit, double room2limitRatio) {
        super(initialPlacement);
        iClassId = id;
        iSchedulingSubpartId = schedulingSubpartId;
        iTimeLocations = new ArrayList<TimeLocation>(timeLocations);
        iRoomLocations = new ArrayList<RoomLocation>(roomLocations);
        iName = name;
        iMinClassLimit = minClassLimit;
        iMaxClassLimit = maxClassLimit;
        iRoomToLimitRatio = (float)room2limitRatio;
        iNrRooms = nrRooms;
        iSolverGroupId = solverGroupId;
    }

    public Lecture(Long id, Long solverGroupId, String name) {
        super(null);
        iClassId = id;
        iSolverGroupId = solverGroupId;
        iName = name;
    }

    public Long getSolverGroupId() {
        return iSolverGroupId;
    }

    /**
     * Add active jenrl constraint (active mean that there is at least one
     * student between its classes)
     * @param assignment current assignment
     * @param constr an active jenrl constraint
     */
    public void addActiveJenrl(Assignment<Lecture, Placement> assignment, JenrlConstraint constr) {
        getContext(assignment).addActiveJenrl(constr);
    }

    /**
     * Active jenrl constraints (active mean that there is at least one student
     * between its classes)
     * @param assignment current assignment
     * @return set of active jenrl constraints
     */
    public Set<JenrlConstraint> activeJenrls(Assignment<Lecture, Placement> assignment) {
        return getContext(assignment).activeJenrls();
    }

    /**
     * Remove active jenrl constraint (active mean that there is at least one
     * student between its classes)
     * @param assignment current assignment
     * @param constr an active jenrl constraint
     */
    public void removeActiveJenrl(Assignment<Lecture, Placement> assignment, JenrlConstraint constr) {
        getContext(assignment).removeActiveJenrl(constr);
    }

    /** Class id 
     * @return class unique id
     **/
    public Long getClassId() {
        return iClassId;
    }

    /**
     * Scheduling subpart id
     * @return scheduling subpart unique id
     */
    public Long getSchedulingSubpartId() {
        return iSchedulingSubpartId;
    }

    /** Class name */
    @Override
    public String getName() {
        return iName;
    }

    /** Class id */
    @Override
    public long getId() {
        return iClassId.longValue();
    }

    /** Instructor name 
     * @return list of instructor names
     **/
    public List<String> getInstructorNames() {
        List<String> ret = new ArrayList<String>();
        for (InstructorConstraint ic : iInstructorConstraints) {
            ret.add(ic.getName());
        }
        return ret;
    }

    public String getInstructorName() {
        StringBuffer sb = new StringBuffer();
        for (InstructorConstraint ic : iInstructorConstraints) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(ic.getName());
        }
        return sb.toString();
    }

    /** List of enrolled students 
     * @return list of enrolled students
     **/
    public Set<Student> students() {
        return iStudents;
    }

    /**
     * Total weight of all enrolled students
     * @return sum of {@link Student#getOfferingWeight(Configuration)} of each enrolled student
     */
    public double nrWeightedStudents() {
        double w = 0.0;
        for (Student s : iStudents) {
            w += s.getOfferingWeight(getConfiguration());
        }
        return w;
    }

    /** Add an enrolled student 
     * @param assignment current assignment
     * @param student a student to add
     **/
    public void addStudent(Assignment<Lecture, Placement> assignment, Student student) {
        if (!iStudents.add(student))
            return;
        Placement value = (assignment == null ? null : assignment.getValue(this));
        if (value != null && getModel() != null)
            getModel().getCriterion(StudentCommittedConflict.class).inc(assignment, student.countConflictPlacements(value));
        iLock.writeLock().lock();
        iCommitedConflicts.clear();
        iLock.writeLock().unlock();
    }

    /** 
     * Remove an enrolled student
     * @param assignment current assignment
     * @param student a student to remove
     */
    public void removeStudent(Assignment<Lecture, Placement> assignment, Student student) {
        if (!iStudents.remove(student))
            return;
        Placement value = (assignment == null ? null : assignment.getValue(this));
        if (value != null && getModel() != null)
            getModel().getCriterion(StudentCommittedConflict.class).inc(assignment, -student.countConflictPlacements(value));
        iLock.writeLock().lock();
        iCommitedConflicts.clear();
        iLock.writeLock().unlock();
    }

    /** Returns true if the given student is enrolled 
     * @param student a student
     * @return true if the given student is enrolled in this class
     **/
    public boolean hasStudent(Student student) {
        return iStudents.contains(student);
    }

    /** Set of lectures of the same class (only section is different) 
     * @param sameSubpartLectures list of lectures of the same scheduling subpart 
     **/
    public void setSameSubpartLectures(List<Lecture> sameSubpartLectures) {
        iSameSubpartLectures = sameSubpartLectures;
    }

    /** Set of lectures of the same class (only section is different) 
     * @return list of lectures of the same scheduling subpart
     **/
    public List<Lecture> sameSubpartLectures() {
        return iSameSubpartLectures;
    }

    /** List of students enrolled in this class as well as in the given class 
     * @param lecture a lecture
     * @return a set of students that are enrolled in both lectures 
     **/
    public Set<Student> sameStudents(Lecture lecture) {
        JenrlConstraint jenrl = jenrlConstraint(lecture);
        return (jenrl == null ? new HashSet<Student>() : jenrl.getStudents());
    }

    /** List of students of this class in conflict with the given assignment 
     * @param assignment current assignment
     * @param value given placement
     * @return list of student conflicts
     **/
    public Set<Student> conflictStudents(Assignment<Lecture, Placement> assignment, Placement value) {
        if (value == null)
            return new HashSet<Student>();
        if (value.equals(assignment.getValue(this)))
            return conflictStudents(assignment);
        Set<Student> ret = new HashSet<Student>();
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (jenrl.jenrl(assignment, this, value) > 0)
                ret.addAll(sameStudents(jenrl.another(this)));
        }
        return ret;
    }

    /**
     * List of students of this class which are in conflict with any other
     * assignment
     * @param assignment current assignment
     * @return list of student conflicts
     */
    public Set<Student> conflictStudents(Assignment<Lecture, Placement> assignment) {
        Set<Student> ret = new HashSet<Student>();
        Placement placement = assignment.getValue(this);
        if (placement == null)
            return ret;
        for (JenrlConstraint jenrl : activeJenrls(assignment)) {
            ret.addAll(sameStudents(jenrl.another(this)));
        }
        for (Student student : students()) {
            if (student.countConflictPlacements(placement) > 0)
                ret.add(student);
        }
        return ret;
    }

    /**
     * Lectures different from this one, where it is student conflict of the
     * given student between this and the lecture
     * @param assignment current assignment
     * @param student a student
     * @return list of lectures with a student conflict 
     */
    public List<Lecture> conflictLectures(Assignment<Lecture, Placement> assignment, Student student) {
        List<Lecture> ret = new ArrayList<Lecture>();
        if (assignment.getValue(this) == null)
            return ret;
        for (JenrlConstraint jenrl : activeJenrls(assignment)) {
            Lecture lect = jenrl.another(this);
            if (lect.students().contains(student))
                ret.add(lect);
        }
        return ret;
    }

    /** True if this lecture is in a student conflict with the given student 
     * @param assignment current assignment
     * @param student a student
     * @return number of other lectures with a student conflict
     **/
    public int isInConflict(Assignment<Lecture, Placement> assignment, Student student) {
        if (assignment.getValue(this) == null)
            return 0;
        int ret = 0;
        for (JenrlConstraint jenrl : activeJenrls(assignment)) {
            Lecture lect = jenrl.another(this);
            if (lect.students().contains(student))
                ret++;
        }
        return ret;
    }
    
    private boolean isCacheDomain() {
        return isCommitted() || (!sSaveMemory && (iNrRooms <= 1 || getMaxRoomCombinations() <= 0 || ToolBox.binomial(iRoomLocations.size(), iNrRooms) <= getMaxRoomCombinations()));
    }
    
    /** Domain -- all combinations of room and time locations 
     * @param assignment current assignment
     * @param allowBreakHard breaking of hard constraints is allowed
     * @return list of possible placements
     **/
    public List<Placement> computeValues(Assignment<Lecture, Placement> assignment, boolean allowBreakHard) {
        List<Placement> values = new ArrayList<Placement>(iRoomLocations.size() * iTimeLocations.size());
        for (TimeLocation timeLocation : iTimeLocations) {
            if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(timeLocation.getPreference())))
                continue;
            if (timeLocation.getPreference() > 500)
                continue;
            boolean notAvailable = false;
            for (InstructorConstraint ic : getInstructorConstraints()) {
                if (!ic.isAvailable(this, timeLocation) && ic.isHard()) {
                    notAvailable = true;
                    break;
                }
            }
            if (notAvailable)
                continue;
            if (iNrRooms == 0) {
                Placement p = new Placement(this, timeLocation, (RoomLocation) null);
                for (InstructorConstraint ic : getInstructorConstraints()) {
                    if (!ic.isAvailable(this, p) && ic.isHard()) {
                        notAvailable = true;
                        break;
                    }
                }
                if (notAvailable)
                    continue;
                p.setVariable(this);
                if (sSaveMemory && !isValid(p))
                    continue;
                if (getInitialAssignment() != null && p.equals(getInitialAssignment()))
                    setInitialAssignment(p);
                // if (getAssignment() != null && getAssignment().equals(p)) iValue = getAssignment();
                if (getBestAssignment() != null && getBestAssignment().equals(p))
                    setBestAssignment(p, getBestAssignmentIteration());
                values.add(p);
            } else if (iNrRooms == 1) {
                rooms: for (RoomLocation roomLocation : iRoomLocations) {
                    if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(roomLocation.getPreference()))) continue;
                    if (roomLocation.getPreference() > 500) continue;
                    if (roomLocation.getRoomConstraint() != null && !roomLocation.getRoomConstraint().isAvailable(this, timeLocation, getScheduler())) continue;
                    Placement p = new Placement(this, timeLocation, roomLocation);
                    p.setVariable(this);
                    for (InstructorConstraint ic : getInstructorConstraints())
                        if (!ic.isAvailable(this, p) && ic.isHard()) continue rooms;
                    if (sSaveMemory && !isValid(p)) continue;
                    if (getInitialAssignment() != null && p.equals(getInitialAssignment())) setInitialAssignment(p);
                    if (getBestAssignment() != null && getBestAssignment().equals(p)) setBestAssignment(p, getBestAssignmentIteration());
                    values.add(p);
                }
            } else {
                if (getMaxRoomCombinations() > 0 && ToolBox.binomial(iRoomLocations.size(), iNrRooms) > getMaxRoomCombinations()) {
                    if (getInitialAssignment() != null && getInitialAssignment().getNrRooms() == getNrRooms()) {
                        boolean av = true;
                        for (RoomLocation room: getInitialAssignment().getRoomLocations()) {
                            if (room.getRoomConstraint() != null && !room.getRoomConstraint().isAvailable(this, timeLocation, getScheduler())) { av = false; break; }
                            if (room.getMinPreference() > 500) { av = false; break; }
                            if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getMinPreference()))) { av = false; break; }
                        }
                        Placement p = new Placement(this, timeLocation, new ArrayList<RoomLocation>(getInitialAssignment().getRoomLocations()));
                        if (!allowBreakHard && p.isRoomProhibited()) av = false;
                        for (InstructorConstraint ic : getInstructorConstraints())
                            if (!ic.isAvailable(this, p) && ic.isHard()) { av = false; break; }
                        if (av && !isTooSmall(p) && !checkParents(p) && (!sSaveMemory || isValid(p))) {
                            p.setVariable(this);
                            if (p.equals(getInitialAssignment())) setInitialAssignment(p);
                            if (getBestAssignment() != null && getBestAssignment().equals(p)) setBestAssignment(p, getBestAssignmentIteration());
                            values.add(p);
                        }
                    }
                    List<RoomLocation> available = new ArrayList<RoomLocation>(iRoomLocations.size());
                    List<RoomLocation> other = new ArrayList<RoomLocation>(iRoomLocations.size());
                    rooms: for (RoomLocation room: iRoomLocations) {
                        if (room.getRoomConstraint() != null && !room.getRoomConstraint().isAvailable(this, timeLocation, getScheduler())) continue;
                        if (room.getMinPreference() > 500) continue;
                        if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getMinPreference()))) continue;
                        for (InstructorConstraint ic : getInstructorConstraints())
                            if (!ic.isAvailable(this, new Placement(this, timeLocation, room)) && ic.isHard()) continue rooms;
                        if (assignment != null && room.getRoomConstraint() != null && !room.getRoomConstraint().getContext(assignment).inConflict(this, timeLocation) &&
                            !Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getMinPreference()))) {
                            available.add(room);
                        } else {
                            other.add(room);
                        }
                    }
                    if (available.size() + other.size() < iNrRooms) continue;
                    for (Enumeration<Collection<RoomLocation>> e = ToolBox.sample(available, other, iNrRooms, isSplitAttendance() ? -1 : getMaxRoomCombinations()); e.hasMoreElements(); ) {
                        Collection<RoomLocation> rm = e.nextElement();
                        if (isTooSmall(rm)) continue;
                        Placement p = new Placement(this, timeLocation, new ArrayList<RoomLocation>(rm));
                        if (!checkParents(p)) continue;
                        if (!allowBreakHard && p.isRoomProhibited()) continue;
                        if (getInitialAssignment() != null && p.sameRooms(getInitialAssignment())) continue;
                        p.setVariable(this);
                        if (sSaveMemory && !isValid(p)) continue;
                        if (getBestAssignment() != null && getBestAssignment().equals(p)) setBestAssignment(p, getBestAssignmentIteration());
                        values.add(p);
                        if (values.size() >= getMaxRoomCombinations()) break;
                    }
                } else {
                    List<RoomLocation> rooms = new ArrayList<RoomLocation>(iRoomLocations.size());
                    rooms: for (RoomLocation room: iRoomLocations) {
                        if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getMinPreference()))) continue;
                        if (room.getMinPreference() > 500) continue;
                        if (room.getRoomConstraint() != null && !room.getRoomConstraint().isAvailable(this, timeLocation, getScheduler())) continue;
                        for (InstructorConstraint ic : getInstructorConstraints())
                            if (!ic.isAvailable(this, new Placement(this, timeLocation, room)) && ic.isHard()) continue rooms;
                        rooms.add(room);
                    }
                    if (rooms.size() < iNrRooms) continue;
                    for (Enumeration<Collection<RoomLocation>> e = ToolBox.permutations(rooms, iNrRooms); e.hasMoreElements(); ) {
                        Collection<RoomLocation> rm = e.nextElement();
                        if (isTooSmall(rm)) continue;
                        Placement p = new Placement(this, timeLocation, new ArrayList<RoomLocation>(rm));
                        if (!checkParents(p)) continue;
                        if (!allowBreakHard && p.isRoomProhibited()) continue;
                        p.setVariable(this);
                        if (sSaveMemory && !isValid(p)) continue;
                        if (getInitialAssignment() != null && p.equals(getInitialAssignment())) setInitialAssignment(p);
                        if (getBestAssignment() != null && getBestAssignment().equals(p)) setBestAssignment(p, getBestAssignmentIteration());
                        values.add(p);
                    }
                }
            }
        }
        return values;
    }
    
    public void clearValueCache() {
        super.setValues(null);
    }
    
    /** All values */
    @Override
    public List<Placement> values(Assignment<Lecture, Placement> assignment) {
        if (super.values(assignment) == null) {
            if (getInitialAssignment() != null && iTimeLocations.size() == 1 && iRoomLocations.size() == getNrRooms()) {
                List<Placement> values = new ArrayList<Placement>(1);
                values.add(getInitialAssignment());
                setValues(values);
                return values;
            } else if (isCacheDomain()) {
                List<Placement> values = computeValues(null, allowBreakHard()); 
                setValues(values);
                return values;
            } else {
                return computeValues(assignment, allowBreakHard());
            }
        } else {
            return super.values(assignment);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Lecture))
            return false;
        return getClassId().equals(((Lecture) o).getClassId());
    }

    /** Best time preference of this lecture */
    private Double iBestTimePreferenceCache = null;

    public double getBestTimePreference() {
        if (iBestTimePreferenceCache == null) {
            double ret = Double.MAX_VALUE;
            for (TimeLocation time : iTimeLocations) {
                ret = Math.min(ret, time.getNormalizedPreference());
            }
            iBestTimePreferenceCache = Double.valueOf(ret);
        }
        return iBestTimePreferenceCache.doubleValue();
    }

    /** Best room preference of this lecture 
     * @return best room preference
     **/
    public int getBestRoomPreference() {
        int ret = Integer.MAX_VALUE;
        for (RoomLocation room : iRoomLocations) {
            ret = Math.min(ret, room.getMinPreference());
        }
        return ret;
    }

    /**
     * Number of student conflicts caused by the given assignment of this
     * lecture
     * @param assignment current assignment
     * @param value a placement
     * @return number of student conflicts if assigned
     */
    public int countStudentConflicts(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }

    public int countStudentConflictsOfTheSameProblem(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }

    public int countHardStudentConflicts(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        if (!isSingleSection())
            return 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.areStudentConflictsHard())
                continue;
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }

    public int countCommittedStudentConflictsOfTheSameProblem(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            if (!jenrl.areStudentConflictsCommitted())
                continue;
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }
    
    public int countCommittedStudentConflicts(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.areStudentConflictsCommitted())
                continue;
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }

    public int countHardStudentConflictsOfTheSameProblem(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            if (!jenrl.areStudentConflictsHard())
                continue;
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }

    public int countDistanceStudentConflicts(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.areStudentConflictsDistance(assignment, value))
                continue;
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }

    public int countDistanceStudentConflictsOfTheSameProblem(Assignment<Lecture, Placement> assignment, Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            if (!jenrl.areStudentConflictsDistance(assignment, value))
                continue;
            studentConflictsSum += jenrl.jenrl(assignment, this, value);
        }
        return studentConflictsSum;
    }
    
    private DistanceMetric getDistanceMetric() {
        return ((TimetableModel)getModel()).getDistanceMetric();
    }
    
    private int getStudentWorkDayLimit() {
        return ((TimetableModel)getModel()).getStudentWorkDayLimit();
    }

    /**
     * Number of student conflicts caused by the initial assignment of this
     * lecture
     * @return number of student conflicts with the initial assignment of this class
     */
    public int countInitialStudentConflicts() {
        Placement value = getInitialAssignment();
        if (value == null)
            return 0;
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            Lecture another = jenrl.another(this);
            if (another.getInitialAssignment() != null)
                if (JenrlConstraint.isInConflict(value, another.getInitialAssignment(), getDistanceMetric(), getStudentWorkDayLimit()))
                    studentConflictsSum += jenrl.getJenrl();
        }
        return studentConflictsSum;
    }

    /**
     * Table of student conflicts caused by the initial assignment of this
     * lecture in format (another lecture, number)
     * @return table of student conflicts with the initial assignment of this class
     */
    public Map<Lecture, Long> getInitialStudentConflicts() {
        Placement value = getInitialAssignment();
        if (value == null)
            return null;
        Map<Lecture, Long> ret = new HashMap<Lecture, Long>();
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            Lecture another = jenrl.another(this);
            if (another.getInitialAssignment() != null)
                if (JenrlConstraint.isInConflict(value, another.getInitialAssignment(), getDistanceMetric(), getStudentWorkDayLimit()))
                    ret.put(another, jenrl.getJenrl());
        }
        return ret;
    }

    /**
     * List of student conflicts caused by the initial assignment of this
     * lecture
     * @return a set of students in a conflict with the initial assignment of this class
     */
    public Set<Student> initialStudentConflicts() {
        Placement value = getInitialAssignment();
        if (value == null)
            return null;
        HashSet<Student> ret = new HashSet<Student>();
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            Lecture another = jenrl.another(this);
            if (another.getInitialAssignment() != null)
                if (JenrlConstraint.isInConflict(value, another.getInitialAssignment(), getDistanceMetric(), getStudentWorkDayLimit()))
                    ret.addAll(sameStudents(another));
        }
        return ret;
    }

    @Override
    public void addContstraint(Constraint<Lecture, Placement> constraint) {
        super.addContstraint(constraint);

        if (constraint instanceof WeakeningConstraint)
            iWeakeningConstraints.add(constraint);
        
        if (constraint instanceof FlexibleConstraint)
            iFlexibleGroupConstraints.add((FlexibleConstraint) constraint);

        if (constraint instanceof JenrlConstraint) {
            JenrlConstraint jenrl = (JenrlConstraint) constraint;
            Lecture another = jenrl.another(this);
            if (another != null) {
                iJenrlConstraints.add(jenrl);
                another.iJenrlConstraints.add(jenrl);
                iJenrlConstraintsHash.put(another, (JenrlConstraint) constraint);
                another.iJenrlConstraintsHash.put(this, (JenrlConstraint) constraint);
            }
        } else if (constraint instanceof DepartmentSpreadConstraint)
            iDeptSpreadConstraint = (DepartmentSpreadConstraint) constraint;
        else if (constraint instanceof SpreadConstraint)
            iSpreadConstraints.add((SpreadConstraint) constraint);
        else if (constraint instanceof InstructorConstraint) {
            InstructorConstraint ic = (InstructorConstraint) constraint;
            if (ic.getResourceId() != null && ic.getResourceId().longValue() > 0)
                iInstructorConstraints.add(ic);
        } else if (constraint instanceof ClassLimitConstraint)
            iClassLimitConstraint = (ClassLimitConstraint) constraint;
        else if (constraint instanceof GroupConstraint) {
            GroupConstraint gc = (GroupConstraint) constraint;
            if (gc.canShareRoom()) {
                iCanShareRoomGroupConstraints.add((GroupConstraint) constraint);
            } else {
                iGroupConstraints.add((GroupConstraint) constraint);
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(gc.getPreference()))
                        || Constants.sPreferenceRequired.equals(Constants
                                .preferenceLevel2preference(gc.getPreference())))
                    iHardGroupSoftConstraints.add((GroupConstraint) constraint);
            }
        }
    }

    @Override
    public void removeContstraint(Constraint<Lecture, Placement> constraint) {
        super.removeContstraint(constraint);

        if (constraint instanceof WeakeningConstraint)
            iWeakeningConstraints.remove(constraint);
        
        if (constraint instanceof FlexibleConstraint)
            iFlexibleGroupConstraints.remove(constraint);

        if (constraint instanceof JenrlConstraint) {
            JenrlConstraint jenrl = (JenrlConstraint) constraint;
            Lecture another = jenrl.another(this);
            if (another != null) {
                iJenrlConstraints.remove(jenrl);
                another.iJenrlConstraints.remove(jenrl);
                iJenrlConstraintsHash.remove(another);
                another.iJenrlConstraintsHash.remove(this);
            }
        } else if (constraint instanceof GroupConstraint) {
            iCanShareRoomGroupConstraints.remove(constraint);
            iHardGroupSoftConstraints.remove(constraint);
            iGroupConstraints.remove(constraint);
        } else if (constraint instanceof DepartmentSpreadConstraint)
            iDeptSpreadConstraint = null;
        else if (constraint instanceof SpreadConstraint)
            iSpreadConstraints.remove(constraint);
        else if (constraint instanceof InstructorConstraint)
            iInstructorConstraints.remove(constraint);
        else if (constraint instanceof ClassLimitConstraint)
            iClassLimitConstraint = null;
    }

    /** All JENRL constraints of this lecture 
     * @param another another class
     * @return a join enrollment constraint between this and the given class, if there is one 
     **/
    public JenrlConstraint jenrlConstraint(Lecture another) {
        /*
         * for (Enumeration e=iJenrlConstraints.elements();e.hasMoreElements();)
         * { JenrlConstraint jenrl = (JenrlConstraint)e.nextElement(); if
         * (jenrl.another(this).equals(another)) return jenrl; } return null;
         */
        return iJenrlConstraintsHash.get(another);
    }

    /** All JENRL constraints of this lecture
     * @return list of all join enrollment constraints in which this lecture is involved
     **/
    public List<JenrlConstraint> jenrlConstraints() {
        return iJenrlConstraints;
    }

    public int minClassLimit() {
        return iMinClassLimit;
    }

    public int maxClassLimit() {
        return iMaxClassLimit;
    }

    public int maxAchievableClassLimit() {
        iLock.readLock().lock();
        try {
            if (iCacheMaxAchievableClassLimit != null) return iCacheMaxAchievableClassLimit.intValue();
        } finally {
            iLock.readLock().unlock();
        }
        iLock.writeLock().lock();
        try {
            if (iCacheMaxAchievableClassLimit != null) return iCacheMaxAchievableClassLimit.intValue();

            int maxAchievableClassLimit = Math.min(maxClassLimit(), (int) Math.floor(maxRoomSize() / roomToLimitRatio()));

            if (hasAnyChildren()) {

                for (Long subpartId: getChildrenSubpartIds()) {
                    int maxAchievableChildrenLimit = 0;

                    for (Lecture child : getChildren(subpartId)) {
                        maxAchievableChildrenLimit += child.maxAchievableClassLimit();
                    }

                    maxAchievableClassLimit = Math.min(maxAchievableClassLimit, maxAchievableChildrenLimit);
                }
            }

            maxAchievableClassLimit = Math.max(minClassLimit(), maxAchievableClassLimit);
            iCacheMaxAchievableClassLimit = Integer.valueOf(maxAchievableClassLimit);
            return maxAchievableClassLimit;
        } finally {
            iLock.writeLock().unlock();
        }
    }

    public int classLimit(Assignment<Lecture, Placement> assignment) {
        if (minClassLimit() == maxClassLimit())
            return minClassLimit();
        return classLimit(assignment, null, null);
    }

    public int classLimit(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        Placement a = (assignment == null ? null : assignment.getValue(this));
        if (value != null && value.variable().equals(this))
            a = value;
        if (conflicts != null && a != null && conflicts.contains(a))
            a = null;
        int classLimit = (a == null ? maxAchievableClassLimit() : Math.min(maxClassLimit(), (int) Math.floor(a.getRoomSize() / roomToLimitRatio())));

        if (!hasAnyChildren())
            return classLimit;

        for (Long subpartId: getChildrenSubpartIds()) {
            int childrenClassLimit = 0;

            for (Lecture child : getChildren(subpartId)) {
                childrenClassLimit += child.classLimit(assignment, value, conflicts);
            }

            classLimit = Math.min(classLimit, childrenClassLimit);
        }

        return Math.max(minClassLimit(), classLimit);
    }

    public double roomToLimitRatio() {
        return iRoomToLimitRatio;
    }

    public int minRoomUse() {
        return iNrRooms == 0 ? 0 : Math.round(iMinClassLimit * iRoomToLimitRatio);
    }

    public int maxRoomUse() {
        return iNrRooms == 0 ? 0 : Math.round(iMaxClassLimit * iRoomToLimitRatio);
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Controlling Course Offering Department 
     * @return department unique id
     **/
    public Long getDepartment() {
        return iDept;
    }

    /** Controlling Course Offering Department 
     * @param dept department unique id
     **/
    public void setDepartment(Long dept) {
        iDept = dept;
    }

    /** Scheduler (Managing Department) 
     * @return solver group unique id
     **/
    public Long getScheduler() {
        return iScheduler;
    }

    /** Scheduler (Managing Department)
     * @param scheduler solver group unique id 
     **/
    public void setScheduler(Long scheduler) {
        iScheduler = scheduler;
    }

    /** Departmental spreading constraint 
     * @return department spread constraint of this class, if any
     **/
    public DepartmentSpreadConstraint getDeptSpreadConstraint() {
        return iDeptSpreadConstraint;
    }

    /** Instructor constraint 
     * @return instructors of this class
     **/
    public List<InstructorConstraint> getInstructorConstraints() {
        return iInstructorConstraints;
    }

    public ClassLimitConstraint getClassLimitConstraint() {
        return iClassLimitConstraint;
    }

    public Set<SpreadConstraint> getSpreadConstraints() {
        return iSpreadConstraints;
    }
    
    public Set<FlexibleConstraint> getFlexibleGroupConstraints() {
        return iFlexibleGroupConstraints;
    }

    public Set<Constraint<Lecture, Placement>> getWeakeningConstraints() {
        return iWeakeningConstraints;
    }

    /** All room locations 
     * @return possible rooms of this class
     **/
    public List<RoomLocation> roomLocations() {
        return iRoomLocations;
    }

    /** All time locations 
     * @return possible times of this class
     **/
    public List<TimeLocation> timeLocations() {
        return iTimeLocations;
    }

    public int nrTimeLocations() {
        int ret = 0;
        for (TimeLocation time : iTimeLocations) {
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference())))
                ret++;
        }
        return ret;
    }

    public int nrRoomLocations() {
        int ret = 0;
        for (RoomLocation room : iRoomLocations) {
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getMinPreference())))
                ret++;
        }
        return ret;
    }

    public long nrValues() {
        int nrTimes = 0;
        for (TimeLocation time: timeLocations())
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference())))
                nrTimes ++;
        int nrRooms = 0;
        for (RoomLocation room : iRoomLocations)
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getMinPreference())))
                nrRooms ++;
        long estNrValues = nrTimes;
        if (getNrRooms() > 1 && getMaxRoomCombinations() > 0)
            estNrValues *= Math.min(getMaxRoomCombinations(), ToolBox.binomial(nrRooms, getNrRooms()));
        else
            estNrValues *= ToolBox.binomial(nrRooms, getNrRooms());
        return estNrValues;
    }

    public int nrValues(TimeLocation time) {
        int ret = 0;
        for (RoomLocation room : iRoomLocations) {
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getMinPreference()))
                    && (room.getRoomConstraint() == null || room.getRoomConstraint().isAvailable(this, time, getScheduler())))
                ret++;
        }
        return ret;
    }

    public int nrValues(RoomLocation room) {
        int ret = 0;
        for (TimeLocation time : iTimeLocations) {
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference()))
                    && (room.getRoomConstraint() == null || room.getRoomConstraint().isAvailable(this, time,getScheduler())))
                ret++;
        }
        return ret;
    }

    public int nrValues(List<RoomLocation> rooms) {
        int ret = 0;
        for (TimeLocation time : iTimeLocations) {
            boolean available = true;
            for (RoomLocation room : rooms) {
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference()))
                        || (room.getRoomConstraint() != null && !room.getRoomConstraint().isAvailable(this, time,
                                getScheduler())))
                    available = false;
            }
            if (available)
                ret++;
        }
        return ret;
    }

    public boolean allowBreakHard() {
        return (getModel() == null ? false : ((TimetableModel)getModel()).isAllowBreakHard());
    }

    public int getNrRooms() {
        return iNrRooms;
    }

    public Lecture getParent() {
        return iParent;
    }

    public void setParent(Lecture parent) {
        iParent = parent;
        iParent.addChild(this);
    }

    public boolean hasParent() {
        return (iParent != null);
    }

    public boolean hasChildren(Long subpartId) {
        return (iChildren != null && iChildren.get(subpartId) != null && !iChildren.get(subpartId).isEmpty());
    }

    public boolean hasAnyChildren() {
        return (iChildren != null && !iChildren.isEmpty());
    }

    public List<Lecture> getChildren(Long subpartId) {
        return iChildren.get(subpartId);
    }

    public Set<Long> getChildrenSubpartIds() {
        return (iChildren == null ? null : iChildren.keySet());
    }
    
    public Map<Long, List<Lecture>> getChildren() {
        return iChildren;
    }

    private void addChild(Lecture child) {
        if (iChildren == null)
            iChildren = new HashMap<Long, List<Lecture>>();
        List<Lecture> childrenThisSubpart = iChildren.get(child.getSchedulingSubpartId());
        if (childrenThisSubpart == null) {
            childrenThisSubpart = new ArrayList<Lecture>();
            iChildren.put(child.getSchedulingSubpartId(), childrenThisSubpart);
        }
        childrenThisSubpart.add(child);
    }

    public boolean isSingleSection() {
        return (iSameSubpartLectures == null || iSameSubpartLectures.size() <= 1);
        /*
        if (iParent == null)
            return (iSameSubpartLectures == null || iSameSubpartLectures.size() <= 1);
        return (iParent.getChildren(getSchedulingSubpartId()).size() <= 1);
        */
    }

    public java.util.List<Lecture> sameStudentsLectures() {
        return (hasParent() ? getParent().getChildren(getSchedulingSubpartId()) : sameSubpartLectures());
    }

    public Lecture getChild(Student student, Long subpartId) {
        if (!hasAnyChildren())
            return null;
        List<Lecture> children = getChildren(subpartId);
        if (children == null)
            return null;
        for (Lecture child : children) {
            if (child.students().contains(student))
                return child;
        }
        return null;
    }

    public int getCommitedConflicts(Placement placement) {
        iLock.readLock().lock();
        try {
            Integer ret = iCommitedConflicts.get(placement);
            if (ret != null) return ret;
        } finally {
            iLock.readLock().unlock();
        }
        iLock.writeLock().lock();
        try {
            int ret = placement.getCommitedConflicts();
            iCommitedConflicts.put(placement, ret);
            return ret;
        } finally {
            iLock.writeLock().unlock();
        }
    }

    public Set<GroupConstraint> hardGroupSoftConstraints() {
        return iHardGroupSoftConstraints;
    }

    public Set<GroupConstraint> groupConstraints() {
        return iGroupConstraints;
    }

    public int minRoomSize() {
        iLock.readLock().lock();
        try {
            if (iCacheMinRoomSize != null) return iCacheMinRoomSize.intValue();
        } finally {
            iLock.readLock().unlock();
        }
        iLock.writeLock().lock();
        try {
            if (iCacheMinRoomSize != null) return iCacheMinRoomSize.intValue();
            if (getNrRooms() <= 1) {
                int min = Integer.MAX_VALUE;
                for (RoomLocation r : roomLocations()) {
                    if (r.getMinPreference() <= Constants.sPreferenceLevelProhibited / 2)
                        min = Math.min(min, r.getRoomSize());
                }
                iCacheMinRoomSize = Integer.valueOf(min);
                return min;
            } else {
                List<RoomLocation> rooms = new ArrayList<RoomLocation>();
                for (RoomLocation r: roomLocations())
                    if (r.getMinPreference() <= Constants.sPreferenceLevelProhibited / 2)
                        rooms.add(r);
                Collections.sort(rooms, new Comparator<RoomLocation>() {
                    @Override
                    public int compare(RoomLocation r1, RoomLocation r2) {
                        if (r1.getRoomSize() < r2.getRoomSize()) return -1;
                        if (r1.getRoomSize() > r2.getRoomSize()) return 1;
                        return r1.compareTo(r2);
                    }
                });
                int min = rooms.isEmpty() ? 0 : rooms.get(Math.min(getNrRooms(), rooms.size()) - 1).getRoomSize();
                iCacheMinRoomSize = Integer.valueOf(min);
                return min;
            }
        } finally {
            iLock.writeLock().unlock();
        }
    }

    public int maxRoomSize() {
        iLock.readLock().lock();
        try {
            if (iCacheMaxRoomSize != null) return iCacheMaxRoomSize.intValue();
        } finally {
            iLock.readLock().unlock();
        }
        iLock.writeLock().lock();
        try {
            if (iCacheMaxRoomSize != null) return iCacheMaxRoomSize.intValue();
            if (getNrRooms() <= 1) {
                int max = Integer.MIN_VALUE;
                for (RoomLocation r : roomLocations()) {
                    if (r.getMinPreference() <= Constants.sPreferenceLevelProhibited / 2) 
                        max = Math.max(max, r.getRoomSize());
                }
                iCacheMaxRoomSize = Integer.valueOf(max);
                return max;
            } else {
                List<RoomLocation> rooms = new ArrayList<RoomLocation>();
                for (RoomLocation r: roomLocations())
                    if (r.getMinPreference() <= Constants.sPreferenceLevelProhibited / 2) rooms.add(r);
                Collections.sort(rooms, new Comparator<RoomLocation>() {
                    @Override
                    public int compare(RoomLocation r1, RoomLocation r2) {
                        if (r1.getRoomSize() > r2.getRoomSize()) return -1;
                        if (r1.getRoomSize() < r2.getRoomSize()) return 1;
                        return r1.compareTo(r2);
                    }
                });
                int max = rooms.isEmpty() ? 0 : rooms.get(Math.min(getNrRooms(), rooms.size()) - 1).getRoomSize();
                iCacheMaxRoomSize = Integer.valueOf(max);
                return max;
            }
        } finally {
            iLock.writeLock().unlock();
        }
    }

    public boolean canShareRoom() {
        return (!iCanShareRoomGroupConstraints.isEmpty());
    }

    public boolean canShareRoom(Lecture other) {
        if (other.equals(this))
            return true;
        for (GroupConstraint gc : iCanShareRoomGroupConstraints) {
            if (gc.variables().contains(other))
                return true;
        }
        return false;
    }

    public Set<GroupConstraint> canShareRoomConstraints() {
        return iCanShareRoomGroupConstraints;
    }

    public boolean isSingleton() {
        return getNrRooms() == roomLocations().size() && timeLocations().size() == 1;
    }

    public boolean isValid(Placement placement) {
        TimetableModel model = (TimetableModel) getModel();
        if (model == null)
            return true;
        if (!model.isAllowBreakHard() && !model.getProperties().getPropertyBoolean("General.InteractiveMode", false)) {
            if (getNrRooms() > 1 && isSplitAttendance() && placement.getRoomSize() < minRoomUse()) return false;
            if (placement.isRoomProhibited()) return false;
        }
        if (!checkParents(placement)) return false;
        if (model.hasConstantVariables()) {
            for (Placement confPlacement : model.conflictValuesSkipWeakeningConstraints(model.getEmptyAssignment(), placement)) {
                Lecture lecture = confPlacement.variable();
                if (lecture.isCommitted())
                    return false;
                if (confPlacement.equals(placement))
                    return false;
            }
        } else {
            Set<Placement> conflicts = new HashSet<Placement>();
            for (Constraint<Lecture, Placement> constraint : hardConstraints()) {
                if (constraint instanceof WeakeningConstraint) continue;
                constraint.computeConflicts(model.getEmptyAssignment(), placement, conflicts);
            }
            for (GlobalConstraint<Lecture, Placement> constraint : model.globalConstraints()) {
                if (constraint instanceof WeakeningConstraint) continue;
                constraint.computeConflicts(model.getEmptyAssignment(), placement, conflicts);
            }
            if (conflicts.contains(placement))
                return false;
        }
        return true;
    }

    public String getNotValidReason(Assignment<Lecture, Placement> assignment, Placement placement, boolean useAmPm) {
        TimetableModel model = (TimetableModel) getModel();
        if (model == null)
            return "no model for class " + getName();
        Map<Constraint<Lecture, Placement>, Set<Placement>> conflictConstraints = model.conflictConstraints(assignment, placement);
        for (Map.Entry<Constraint<Lecture, Placement>, Set<Placement>> entry : conflictConstraints.entrySet()) {
            Constraint<Lecture, Placement> constraint = entry.getKey();
            Set<Placement> conflicts = entry.getValue();
            String cname = constraint.getName();
            if (constraint instanceof RoomConstraint) {
                cname = "Room " + constraint.getName();
            } else if (constraint instanceof InstructorConstraint) {
                cname = "Instructor " + constraint.getName();
            } else if (constraint instanceof GroupConstraint) {
                cname = "Distribution " + constraint.getName();
            } else if (constraint instanceof DepartmentSpreadConstraint) {
                cname = "Balancing of department " + constraint.getName();
            } else if (constraint instanceof SpreadConstraint) {
                cname = "Same subpart spread " + constraint.getName();
            } else if (constraint instanceof ClassLimitConstraint) {
                cname = "Class limit " + constraint.getName();
            }
            for (Placement confPlacement : conflicts) {
                Lecture lecture = confPlacement.variable();
                if (lecture.isCommitted()) {
                    return placement.getLongName(useAmPm) + " conflicts with " + lecture.getName() + " "
                            + confPlacement.getLongName(useAmPm) + " due to constraint " + cname;
                }
                if (confPlacement.equals(placement)) {
                    return placement.getLongName(useAmPm) + " is not valid due to constraint " + cname;
                }
            }
        }
        if (getNrRooms() > 1 && isSplitAttendance() && placement.getRoomSize() < minRoomUse())
            return "selected rooms are too small (" + placement.getRoomSize() + " < " + minRoomUse() + ")";
        if (placement.isMultiRoom() && placement.isRoomProhibited()) {
            int roomIndex = 0;
            for (RoomLocation r : placement.getRoomLocations()) {
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference(roomIndex))))
                    return "invalid room combination (" + r.getName() + " is prohibited as " + (roomIndex + 1) + ". room)";
                roomIndex++;
            }
        }
        if (placement.isMultiRoom()) {
            for (RoomLocation r1: placement.getRoomLocations())
                for (RoomLocation r2: placement.getRoomLocations())
                    if (r2.getRoomConstraint() != null && r2.getRoomConstraint().getParentRoom() != null && r2.getRoomConstraint().getParentRoom().equals(r1.getRoomConstraint()))
                        return "invalid room combination (" + r2.getName() + " is a partition of " + r1.getName() + ")";   
                        
        }
        return null;
    }
    
    @Deprecated
    public String getNotValidReason(Assignment<Lecture, Placement> assignment, Placement placement) {
        return getNotValidReason(assignment, placement, true);
    }

    public void purgeInvalidValues(boolean interactiveMode) {
    	if (isCommitted() || sSaveMemory) return;
        TimetableModel model = (TimetableModel) getModel();
        if (model == null)
            return;
        List<Placement> newValues = new ArrayList<Placement>(values(null).size());
        for (Placement placement : values(null)) {
            if (placement.isValid())
                newValues.add(placement);
        }
        if (!interactiveMode && newValues.size() != values(null).size()) {
            for (Iterator<TimeLocation> i = timeLocations().iterator(); i.hasNext();) {
                TimeLocation timeLocation = i.next();
                boolean hasPlacement = false;
                for (Placement placement : newValues) {
                    if (timeLocation.equals(placement.getTimeLocation())) {
                        hasPlacement = true;
                        break;
                    }
                }
                if (!hasPlacement)
                    i.remove();
            }
            for (Iterator<RoomLocation> i = roomLocations().iterator(); i.hasNext();) {
                RoomLocation roomLocation = i.next();
                boolean hasPlacement = false;
                for (Placement placement : newValues) {
                    if (placement.isMultiRoom()) {
                        if (placement.getRoomLocations().contains(roomLocation)) {
                            hasPlacement = true;
                            break;
                        }
                    } else {
                        if (roomLocation.equals(placement.getRoomLocation())) {
                            hasPlacement = true;
                            break;
                        }
                    }
                }
                if (!hasPlacement)
                    i.remove();
            }
        }
        setValues(newValues);
    }

    public void setCommitted(boolean committed) {
        iCommitted = committed;
    }

    public boolean isCommitted() {
        return iCommitted;
    }

    @Override
    public boolean isConstant() {
        return iCommitted;
    }
    
    @Override
    public Placement getConstantValue() {
        return (isCommitted() ? getInitialAssignment() : null);
    }
    
    public void setConstantValue(Placement value) {
        setInitialAssignment(value);
    }

    public int getSpreadPenalty(Assignment<Lecture, Placement> assignment) {
        int spread = 0;
        for (SpreadConstraint sc : getSpreadConstraints()) {
            spread += sc.getPenalty(assignment);
        }
        return spread;
    }

    @Override
    public int hashCode() {
        return getClassId().hashCode();
    }

    public Configuration getConfiguration() {
        Lecture lecture = this;
        while (lecture.getParent() != null)
            lecture = lecture.getParent();
        return lecture.iParentConfiguration;
    }

    public void setConfiguration(Configuration configuration) {
        Lecture lecture = this;
        while (lecture.getParent() != null)
            lecture = lecture.getParent();
        lecture.iParentConfiguration = configuration;
        configuration.addTopLecture(lecture);
    }

    private int[] iMinMaxRoomPreference = null;

    public int[] getMinMaxRoomPreference() {
        iLock.readLock().lock();
        try {
            if (iMinMaxRoomPreference != null) return iMinMaxRoomPreference;
        } finally {
            iLock.readLock().unlock();
        }
        iLock.writeLock().lock();
        try {
            if (iMinMaxRoomPreference != null) return iMinMaxRoomPreference;

            if (getNrRooms() <= 0 || roomLocations().isEmpty()) {
                iMinMaxRoomPreference = new int[] { 0, 0 };
            } else {
                Integer minRoomPref = null, maxRoomPref = null;
                for (RoomLocation r : roomLocations()) {
                    int pref = r.getMinPreference();
                    if (pref >= Constants.sPreferenceLevelRequired / 2 && pref <= Constants.sPreferenceLevelProhibited / 2) {
                        minRoomPref = (minRoomPref == null ? pref : Math.min(minRoomPref, pref));
                        maxRoomPref = (maxRoomPref == null ? pref : Math.max(maxRoomPref, pref));
                    }
                    pref = r.getMaxPreference();
                    if (pref >= Constants.sPreferenceLevelRequired / 2 && pref <= Constants.sPreferenceLevelProhibited / 2) {
                        minRoomPref = (minRoomPref == null ? pref : Math.min(minRoomPref, pref));
                        maxRoomPref = (maxRoomPref == null ? pref : Math.max(maxRoomPref, pref));
                    }
                }
                iMinMaxRoomPreference = new int[] { minRoomPref == null ? 0 : minRoomPref, maxRoomPref == null ? 0 : maxRoomPref };
            }

            return iMinMaxRoomPreference;
        } finally {
            iLock.writeLock().unlock();
        }
    }

    private double[] iMinMaxTimePreference = null;

    public double[] getMinMaxTimePreference() {
        iLock.readLock().lock();
        try {
            if (iMinMaxTimePreference != null) return iMinMaxTimePreference;
        } finally {
            iLock.readLock().unlock();
        }
        iLock.writeLock().lock();
        try {
            if (iMinMaxTimePreference != null) return iMinMaxTimePreference;

            Double minTimePref = null, maxTimePref = null;
            for (TimeLocation t : timeLocations()) {
                double npref = t.getNormalizedPreference();
                int pref = t.getPreference();
                if (pref >= Constants.sPreferenceLevelRequired / 2 && pref <= Constants.sPreferenceLevelProhibited / 2) {
                    minTimePref = (minTimePref == null ? npref : Math.min(minTimePref, npref));
                    maxTimePref = (maxTimePref == null ? npref : Math.max(maxTimePref, npref));
                }
            }
            iMinMaxTimePreference = new double[] { minTimePref == null ? 0.0 : minTimePref, maxTimePref == null ? 0.0 : maxTimePref };
            
            return iMinMaxTimePreference;
        } finally {
            iLock.writeLock().unlock();
        }
    }

    public void setOrd(int ord) {
        iOrd = ord;
    }

    public int getOrd() {
        return iOrd;
    }

    @Override
    public int compareTo(Lecture o) {
        int cmp = Double.compare(getOrd(), o.getOrd());
        if (cmp != 0)
            return cmp;
        return super.compareTo(o);
    }

    public String getNote() {
        return iNote;
    }

    public void setNote(String note) {
        iNote = note;
    }
    
    public boolean areStudentConflictsHard(Lecture other) {
        return StudentConflict.hard(this, other);
    }
    
    public void clearIgnoreStudentConflictsWithCache() {
        iIgnoreStudentConflictsWith.set(null);
    }
    
    /**
     * Returns true if there is {@link IgnoreStudentConflictsConstraint} between the two lectures.
     * @param other another class
     * @return true if student conflicts between this and the given calls are to be ignored
     */
   public boolean isToIgnoreStudentConflictsWith(Lecture other) {
       Set<Long> cache = iIgnoreStudentConflictsWith.get();
       if (cache != null)
           return cache.contains(other.getClassId());
       cache = new HashSet<Long>();
       for (Constraint<Lecture, Placement> constraint: constraints()) {
           if (constraint instanceof IgnoreStudentConflictsConstraint)
               for (Lecture x: constraint.variables()) {
                   if (!x.equals(this)) cache.add(x.getClassId());
               }
           if (constraint instanceof GroupConstraint && ((GroupConstraint)constraint).getType().is(GroupConstraint.Flag.IGNORE_STUDENTS))
               for (Lecture x: constraint.variables()) {
                   if (!x.equals(this)) cache.add(x.getClassId());
               }
       }
       iIgnoreStudentConflictsWith.set(cache);
       return cache.contains(other.getClassId());
    }
   
   /**
    * Get class weight. This weight is used with the criteria. E.g., class that is not meeting all the
    * semester can have a lower weight. Defaults to 1.0
    * @return class weight
    */
   public double getWeight() { return iWeight; }
   /**
    * Set class weight. This weight is used with the criteria. E.g., class that is not meeting all the
    * semester can have a lower weight.
    * @param weight class weight
    */
   public void setWeight(double weight) { iWeight = weight; }
   
   @Override
   public LectureContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
       return new LectureContext();
   }

   public class LectureContext implements AssignmentContext {
       private Set<JenrlConstraint> iActiveJenrls = new HashSet<JenrlConstraint>();
 
       /**
        * Add active jenrl constraint (active mean that there is at least one
        * student between its classes)
        * @param constr active join enrollment constraint 
        */
       public void addActiveJenrl(JenrlConstraint constr) {
           iActiveJenrls.add(constr);
       }

       /**
        * Active jenrl constraints (active mean that there is at least one student
        * between its classes)
        * @return set of active join enrollment constraints
        */
       public Set<JenrlConstraint> activeJenrls() {
           return iActiveJenrls;
       }

       /**
        * Remove active jenrl constraint (active mean that there is at least one
        * student between its classes)
        * @param constr active join enrollment constraint
        */
       public void removeActiveJenrl(JenrlConstraint constr) {
           iActiveJenrls.remove(constr);
       }
   }
   
   public int getMaxRoomCombinations() {
       return iMaxRoomCombinations;
   }
   
   public void setMaxRoomCombinations(int maxRoomCombinations) {
       iMaxRoomCombinations = maxRoomCombinations;
   }
   
   private Integer iMinWeeks = null;
   public int getMinWeeks() {
       if (iMinWeeks == null) {
           iMinWeeks = 0;
           for (TimeLocation t: timeLocations()) {
               int s = t.getWeekCode().nextSetBit(0);
               int e = t.getWeekCode().previousSetBit(t.getWeekCode().size());
               int weeks = 1 + (e - s) / 7;
               if (iMinWeeks == 0 || weeks < iMinWeeks) iMinWeeks = weeks;
           }
       }
       return iMinWeeks;
   }
   
   /**
    * When using multiple rooms, split attendance means that the class is split among the given rooms.
    * @return true means class is split among rooms, false means each room must be big enough for the class
    */
   public boolean isSplitAttendance() { return iSplitAttendance; }

   /**
    * When using multiple rooms, split attendance means that the class is split among the given rooms.
    * @param splitAttendance true means class is split among rooms, false means each room must be big enough for the class
    */
   public void setSplitAttendance(boolean splitAttendance) { iSplitAttendance = splitAttendance; }
   
   /**
    * Check if the assigned rooms are too small for a class with multiple rooms and split attendace.
    */
   private boolean isTooSmall(Collection<RoomLocation> rooms) {
       if (getNrRooms() <= 1 && !isSplitAttendance()) return false;
       int size = 0;
       for (RoomLocation room: rooms)
           size += room.getRoomSize();
       return size < minRoomSize();
   }
   
   /**
    * Check if the assigned rooms are too small for a class with multiple rooms and split attendace.
    */
   private boolean isTooSmall(Placement p) {
       return getNrRooms() > 1 && isSplitAttendance() && p.getRoomSize() < minRoomUse();
   }
   
   /**
    * Check that the room and its parent are not used at the same time
    */
   private boolean checkParents(Placement p) {
       if (p.isMultiRoom()) {
           for (RoomLocation r1: p.getRoomLocations())
               for (RoomLocation r2: p.getRoomLocations())
                   if (r2.getRoomConstraint() != null && r2.getRoomConstraint().getParentRoom() != null && r2.getRoomConstraint().getParentRoom().equals(r1.getRoomConstraint()))
                       return false;
       }
       return true;
   }
}