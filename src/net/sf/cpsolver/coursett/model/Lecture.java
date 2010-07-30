package net.sf.cpsolver.coursett.model;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.ClassLimitConstraint;
import net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.constraint.WeakeningConstraint;
import net.sf.cpsolver.ifs.constant.ConstantVariable;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.ArrayList;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.ifs.util.List;

/**
 * Lecture (variable).
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class Lecture extends Variable<Lecture, Placement> implements ConstantVariable {
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
    private double iRoomToLimitRatio;
    private int iNrRooms;
    private int iOrd;

    private Set<Student> iStudents = new HashSet<Student>();
    private DepartmentSpreadConstraint iDeptSpreadConstraint = null;
    private Set<SpreadConstraint> iSpreadConstraints = new HashSet<SpreadConstraint>();
    private Set<Constraint<Lecture, Placement>> iWeakeningConstraints = new HashSet<Constraint<Lecture, Placement>>();
    private List<InstructorConstraint> iInstructorConstraints = new ArrayList<InstructorConstraint>();
    private ClassLimitConstraint iClassLimitConstraint = null;

    private Lecture iParent = null;
    private Hashtable<Long, List<Lecture>> iChildren = null;
    private java.util.List<Lecture> iSameSubpartLectures = null;
    private Configuration iParentConfiguration = null;

    private Hashtable<Lecture, Set<Student>> iSameStudents = new Hashtable<Lecture, Set<Student>>(10);
    private Set<JenrlConstraint> iActiveJenrls = new HashSet<JenrlConstraint>();
    private List<JenrlConstraint> iJenrlConstraints = new ArrayList<JenrlConstraint>();
    private Hashtable<Lecture, JenrlConstraint> iJenrlConstraintsHash = new Hashtable<Lecture, JenrlConstraint>();
    private Hashtable<Placement, Integer> iCommitedConflicts = new Hashtable<Placement, Integer>();
    private Set<GroupConstraint> iGroupConstraints = new HashSet<GroupConstraint>();
    private Set<GroupConstraint> iHardGroupSoftConstraints = new HashSet<GroupConstraint>();
    private Set<GroupConstraint> iCanShareRoomGroupConstraints = new HashSet<GroupConstraint>();

    public boolean iCommitted = false;

    public static boolean sSaveMemory = false;
    public static boolean sAllowBreakHard = false;

    private Integer iCacheMinRoomSize = null;
    private Integer iCacheMaxRoomSize = null;
    private Integer iCacheMaxAchievableClassLimit = null;

    /**
     * Constructor
     * 
     * @param id
     *            unique identification
     * @param name
     *            class name
     * @param timeLocations
     *            set of time locations
     * @param roomLocations
     *            set of room location
     * @param initialPlacement
     *            initial placement
     */
    public Lecture(Long id, Long solverGroupId, Long schedulingSubpartId, String name,
            java.util.List<TimeLocation> timeLocations, java.util.List<RoomLocation> roomLocations, int nrRooms,
            Placement initialPlacement, int minClassLimit, int maxClassLimit, double room2limitRatio) {
        super(initialPlacement);
        iClassId = id;
        iSchedulingSubpartId = schedulingSubpartId;
        iTimeLocations = new ArrayList<TimeLocation>(timeLocations);
        iRoomLocations = new ArrayList<RoomLocation>(roomLocations);
        iName = name;
        iMinClassLimit = minClassLimit;
        iMaxClassLimit = maxClassLimit;
        iRoomToLimitRatio = room2limitRatio;
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
     */
    public void addActiveJenrl(JenrlConstraint constr) {
        iActiveJenrls.add(constr);
    }

    /**
     * Active jenrl constraints (active mean that there is at least one student
     * between its classes)
     */
    public Set<JenrlConstraint> activeJenrls() {
        return iActiveJenrls;
    }

    /**
     * Remove active jenrl constraint (active mean that there is at least one
     * student between its classes)
     */
    public void removeActiveJenrl(JenrlConstraint constr) {
        iActiveJenrls.remove(constr);
    }

    /** Class id */
    public Long getClassId() {
        return iClassId;
    }

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

    /** Instructor name */
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

    /** List of enrolled students */
    public Set<Student> students() {
        return iStudents;
    }

    public double nrWeightedStudents() {
        double w = 0.0;
        for (Student s : iStudents) {
            w += s.getOfferingWeight(getConfiguration());
        }
        return w;
    }

    /** Add an enrolled student */
    public void addStudent(Student student) {
        if (!iStudents.add(student))
            return;
        if (getAssignment() != null && getModel() != null)
            ((TimetableModel) getModel()).getCommittedStudentConflictsCounter().inc(
                    student.countConflictPlacements(getAssignment()));
        iSameStudents.clear();
        iCommitedConflicts.clear();
    }

    public void removeStudent(Student student) {
        if (!iStudents.remove(student))
            return;
        if (getAssignment() != null && getModel() != null)
            ((TimetableModel) getModel()).getCommittedStudentConflictsCounter().dec(
                    student.countConflictPlacements(getAssignment()));
        iSameStudents.clear();
        iCommitedConflicts.clear();
    }

    /** Returns true if the given student is enrolled */
    public boolean hasStudent(Student student) {
        return iStudents.contains(student);
    }

    /** Set of lectures of the same class (only section is different) */
    public void setSameSubpartLectures(java.util.List<Lecture> sameSubpartLectures) {
        iSameSubpartLectures = sameSubpartLectures;
    }

    /** Set of lectures of the same class (only section is different) */
    public java.util.List<Lecture> sameSubpartLectures() {
        return iSameSubpartLectures;
    }

    /** List of students enrolled in this class as well as in the given class */
    public Set<Student> sameStudents(Lecture lecture) {
        if (iSameStudents.containsKey(lecture))
            return iSameStudents.get(lecture);
        Set<Student> ret = new HashSet<Student>(students());
        ret.retainAll(lecture.students());
        iSameStudents.put(lecture, ret);
        return ret;
    }

    /** List of students of this class in conflict with the given assignment */
    public Set<Student> conflictStudents(Placement value) {
        if (value == null)
            return new HashSet<Student>();
        if (value.equals(getAssignment()))
            return conflictStudents();
        Set<Student> ret = new HashSet<Student>();
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (jenrl.jenrl(this, value) > 0)
                ret.addAll(sameStudents(jenrl.another(this)));
        }
        return ret;
    }

    /**
     * List of students of this class which are in conflict with any other
     * assignment
     */
    public Set<Student> conflictStudents() {
        Set<Student> ret = new HashSet<Student>();
        if (getAssignment() == null)
            return ret;
        for (JenrlConstraint jenrl : activeJenrls()) {
            ret.addAll(sameStudents(jenrl.another(this)));
        }
        Placement placement = getAssignment();
        for (Student student : students()) {
            if (student.countConflictPlacements(placement) > 0)
                ret.add(student);
        }
        return ret;
    }

    /**
     * Lectures different from this one, where it is student conflict of the
     * given student between this and the lecture
     */
    public List<Lecture> conflictLectures(Student student) {
        List<Lecture> ret = new ArrayList<Lecture>();
        if (getAssignment() == null)
            return ret;
        for (JenrlConstraint jenrl : activeJenrls()) {
            Lecture lect = jenrl.another(this);
            if (lect.students().contains(student))
                ret.add(lect);
        }
        return ret;
    }

    /** True if this lecture is in a student conflict with the given student */
    public int isInConflict(Student student) {
        if (getAssignment() == null)
            return 0;
        int ret = 0;
        for (JenrlConstraint jenrl : activeJenrls()) {
            Lecture lect = jenrl.another(this);
            if (lect.students().contains(student))
                ret++;
        }
        return ret;
    }

    private void computeValues(List<Placement> values, boolean allowBreakHard, TimeLocation timeLocation,
            List<RoomLocation> roomLocations, int idx) {
        if (roomLocations.size() == iNrRooms) {
            Placement p = new Placement(this, timeLocation, roomLocations);
            p.setVariable(this);
            if (sSaveMemory && !isValid(p))
                return;
            if (getInitialAssignment() != null && p.equals(getInitialAssignment()))
                setInitialAssignment(p);
            if (getAssignment() != null && getAssignment().equals(p))
                iValue = getAssignment();
            if (getBestAssignment() != null && getBestAssignment().equals(p))
                setBestAssignment(p);
            values.add(p);
            return;
        }
        for (int i = idx; i < iRoomLocations.size(); i++) {
            RoomLocation roomLocation = iRoomLocations.get(i);
            if (!allowBreakHard
                    && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(roomLocation
                            .getPreference())))
                continue;

            if (roomLocation.getRoomConstraint() != null
                    && !roomLocation.getRoomConstraint().isAvailable(this, timeLocation, getScheduler()))
                continue;
            roomLocations.add(roomLocation);
            computeValues(values, allowBreakHard, timeLocation, roomLocations, i + 1);
            roomLocations.remove(roomLocations.size() - 1);
        }
    }

    /** Domain -- all combinations of room and time locations */
    public List<Placement> computeValues(boolean allowBreakHard) {
        List<Placement> values = new ArrayList<Placement>(iRoomLocations.size() * iTimeLocations.size());
        for (TimeLocation timeLocation : iTimeLocations) {
            if (!allowBreakHard
                    && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(timeLocation
                            .getPreference())))
                continue;
            if (timeLocation.getPreference() > 500)
                continue;
            boolean notAvailable = false;
            for (InstructorConstraint ic : getInstructorConstraints()) {
                if (!ic.isAvailable(this, timeLocation)) {
                    notAvailable = true;
                    break;
                }
            }
            if (notAvailable)
                continue;
            if (iNrRooms == 0) {
                Placement p = new Placement(this, timeLocation, (RoomLocation) null);
                for (InstructorConstraint ic : getInstructorConstraints()) {
                    if (!ic.isAvailable(this, p)) {
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
                if (getAssignment() != null && getAssignment().equals(p))
                    iValue = getAssignment();
                if (getBestAssignment() != null && getBestAssignment().equals(p))
                    setBestAssignment(p);
                values.add(p);
            } else if (iNrRooms == 1) {
                for (RoomLocation roomLocation : iRoomLocations) {
                    if (!allowBreakHard
                            && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(roomLocation
                                    .getPreference())))
                        continue;
                    if (roomLocation.getPreference() > 500)
                        continue;
                    if (roomLocation.getRoomConstraint() != null
                            && !roomLocation.getRoomConstraint().isAvailable(this, timeLocation, getScheduler()))
                        continue;
                    Placement p = new Placement(this, timeLocation, roomLocation);
                    p.setVariable(this);
                    if (sSaveMemory && !isValid(p))
                        continue;
                    if (getInitialAssignment() != null && p.equals(getInitialAssignment()))
                        setInitialAssignment(p);
                    if (getAssignment() != null && getAssignment().equals(p))
                        iValue = getAssignment();
                    if (getBestAssignment() != null && getBestAssignment().equals(p))
                        setBestAssignment(p);
                    values.add(p);
                }
            } else {
                computeValues(values, allowBreakHard, timeLocation, new ArrayList<RoomLocation>(iNrRooms), 0);
            }
        }
        return values;
    }

    /** All values */
    @Override
    public List<Placement> values() {
        if (super.values() == null) {
            if (getInitialAssignment() != null && iTimeLocations.size() == 1 && iRoomLocations.size() == getNrRooms()) {
                List<Placement> values = new ArrayList<Placement>(1);
                values.add(getInitialAssignment());
                setValues(values);
            } else {
                if (isCommitted() || !sSaveMemory)
                    setValues(computeValues(sAllowBreakHard));
            }
        }
        if (isCommitted())
            return super.values();
        if (sSaveMemory) {
            return computeValues(sAllowBreakHard);
        } else
            return super.values();
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
            iBestTimePreferenceCache = new Double(ret);
        }
        return iBestTimePreferenceCache.doubleValue();
    }

    /** Best room preference of this lecture */
    public int getBestRoomPreference() {
        int ret = Integer.MAX_VALUE;
        for (RoomLocation room : iRoomLocations) {
            ret = Math.min(ret, room.getPreference());
        }
        return ret;
    }

    /**
     * Number of student conflicts caused by the given assignment of this
     * lecture
     */
    public int countStudentConflicts(Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countStudentConflictsOfTheSameProblem(Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countHardStudentConflicts(Placement value) {
        int studentConflictsSum = 0;
        if (!isSingleSection())
            return 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.areStudentConflictsHard())
                continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countCommittedStudentConflictsOfTheSameProblem(Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            if (!jenrl.areStudentConflictsCommitted())
                continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }
    
    public int countCommittedStudentConflicts(Placement value) {
        int studentConflictsSum = 0;
        if (!isSingleSection())
            return 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.areStudentConflictsCommitted())
                continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countHardStudentConflictsOfTheSameProblem(Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            if (!jenrl.areStudentConflictsHard())
                continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countDistanceStudentConflicts(Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.areStudentConflictsDistance(value))
                continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countDistanceStudentConflictsOfTheSameProblem(Placement value) {
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            if (!jenrl.isOfTheSameProblem())
                continue;
            if (!jenrl.areStudentConflictsDistance(value))
                continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }
    
    private DistanceMetric getDistanceMetric() {
        return ((TimetableModel)getModel()).getDistanceMetric();
    }

    /**
     * Number of student conflicts caused by the initial assignment of this
     * lecture
     */
    public int countInitialStudentConflicts() {
        Placement value = getInitialAssignment();
        if (value == null)
            return 0;
        int studentConflictsSum = 0;
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            Lecture another = jenrl.another(this);
            if (another.getInitialAssignment() != null)
                if (JenrlConstraint.isInConflict(value, another.getInitialAssignment(), getDistanceMetric()))
                    studentConflictsSum += jenrl.getJenrl();
        }
        return studentConflictsSum;
    }

    /**
     * Table of student conflicts caused by the initial assignment of this
     * lecture in format (another lecture, number)
     */
    public Hashtable<Lecture, Long> getInitialStudentConflicts() {
        Placement value = getInitialAssignment();
        if (value == null)
            return null;
        Hashtable<Lecture, Long> ret = new Hashtable<Lecture, Long>();
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            Lecture another = jenrl.another(this);
            if (another.getInitialAssignment() != null)
                if (JenrlConstraint.isInConflict(value, another.getInitialAssignment(), getDistanceMetric()))
                    ret.put(another, jenrl.getJenrl());
        }
        return ret;
    }

    /**
     * List of student conflicts caused by the initial assignment of this
     * lecture
     */
    public Set<Student> initialStudentConflicts() {
        Placement value = getInitialAssignment();
        if (value == null)
            return null;
        HashSet<Student> ret = new HashSet<Student>();
        for (JenrlConstraint jenrl : jenrlConstraints()) {
            Lecture another = jenrl.another(this);
            if (another.getInitialAssignment() != null)
                if (JenrlConstraint.isInConflict(value, another.getInitialAssignment(), getDistanceMetric()))
                    ret.addAll(sameStudents(another));
        }
        return ret;
    }

    @Override
    public void addContstraint(Constraint<Lecture, Placement> constraint) {
        super.addContstraint(constraint);

        if (constraint instanceof WeakeningConstraint)
            iWeakeningConstraints.add(constraint);

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
            if (GroupConstraint.canShareRooms(gc.getType())) {
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

    /** All JENRL constraints of this lecture */
    public JenrlConstraint jenrlConstraint(Lecture another) {
        /*
         * for (Enumeration e=iJenrlConstraints.elements();e.hasMoreElements();)
         * { JenrlConstraint jenrl = (JenrlConstraint)e.nextElement(); if
         * (jenrl.another(this).equals(another)) return jenrl; } return null;
         */
        return iJenrlConstraintsHash.get(another);
    }

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
        if (iCacheMaxAchievableClassLimit != null)
            return iCacheMaxAchievableClassLimit.intValue();

        int maxAchievableClassLimit = Math.min(maxClassLimit(), (int) Math.floor(maxRoomSize() / roomToLimitRatio()));

        if (hasAnyChildren()) {

            for (Enumeration<Long> e1 = getChildrenSubpartIds(); e1.hasMoreElements();) {
                Long subpartId = e1.nextElement();
                int maxAchievableChildrenLimit = 0;

                for (Lecture child : getChildren(subpartId)) {
                    maxAchievableChildrenLimit += child.maxAchievableClassLimit();
                }

                maxAchievableClassLimit = Math.min(maxAchievableClassLimit, maxAchievableChildrenLimit);
            }
        }

        maxAchievableClassLimit = Math.max(minClassLimit(), maxAchievableClassLimit);
        iCacheMaxAchievableClassLimit = new Integer(maxAchievableClassLimit);
        return maxAchievableClassLimit;
    }

    public int classLimit() {
        if (minClassLimit() == maxClassLimit())
            return minClassLimit();
        return classLimit(null, null);
    }

    public int classLimit(Placement assignment, Set<Placement> conflicts) {
        Placement a = getAssignment();
        if (assignment != null && assignment.variable().equals(this))
            a = assignment;
        if (conflicts != null && a != null && conflicts.contains(a))
            a = null;
        int classLimit = (a == null ? maxAchievableClassLimit() : Math.min(maxClassLimit(), (int) Math.floor(a
                .minRoomSize()
                / roomToLimitRatio())));

        if (!hasAnyChildren())
            return classLimit;

        for (Enumeration<Long> e1 = getChildrenSubpartIds(); e1.hasMoreElements();) {
            Long subpartId = e1.nextElement();
            int childrenClassLimit = 0;

            for (Lecture child : getChildren(subpartId)) {
                childrenClassLimit += child.classLimit(assignment, conflicts);
            }

            classLimit = Math.min(classLimit, childrenClassLimit);
        }

        return Math.max(minClassLimit(), classLimit);
    }

    public double roomToLimitRatio() {
        return iRoomToLimitRatio;
    }

    public int minRoomUse() {
        return (int) Math.ceil(iMinClassLimit * iRoomToLimitRatio);
    }

    public int maxRoomUse() {
        return (int) Math.ceil(iMaxClassLimit * iRoomToLimitRatio);
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getValuesString() {
        StringBuffer sb = new StringBuffer();
        for (Placement p : values()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(p.getName());
        }
        return sb.toString();
    }

    /** Controlling Course Offering Department */
    public Long getDepartment() {
        return iDept;
    }

    /** Controlling Course Offering Department */
    public void setDepartment(Long dept) {
        iDept = dept;
    }

    /** Scheduler (Managing Department) */
    public Long getScheduler() {
        return iScheduler;
    }

    /** Scheduler (Managing Department) */
    public void setScheduler(Long scheduler) {
        iScheduler = scheduler;
    }

    /** Departmental spreading constraint */
    public DepartmentSpreadConstraint getDeptSpreadConstraint() {
        return iDeptSpreadConstraint;
    }

    /** Instructor constraint */
    public List<InstructorConstraint> getInstructorConstraints() {
        return iInstructorConstraints;
    }

    public ClassLimitConstraint getClassLimitConstraint() {
        return iClassLimitConstraint;
    }

    public Set<SpreadConstraint> getSpreadConstraints() {
        return iSpreadConstraints;
    }

    public Set<Constraint<Lecture, Placement>> getWeakeningConstraints() {
        return iWeakeningConstraints;
    }

    /** All room locations */
    public List<RoomLocation> roomLocations() {
        return iRoomLocations;
    }

    /** All time locations */
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
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getPreference())))
                ret++;
        }
        return ret;
    }

    public int nrValues() {
        int ret = 0;
        for (Placement placement : values()) {
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(placement
                    .getRoomPreference()))
                    && !Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(placement
                            .getTimeLocation().getPreference())))
                ret++;
        }
        return ret;
    }

    public int nrValues(TimeLocation time) {
        int ret = 0;
        for (RoomLocation room : iRoomLocations) {
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getPreference()))
                    && (room.getRoomConstraint() == null || room.getRoomConstraint().isAvailable(this, time,
                            getScheduler())))
                ret++;
        }
        return ret;
    }

    public int nrValues(RoomLocation room) {
        int ret = 0;
        for (TimeLocation time : iTimeLocations) {
            if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference()))
                    && (room.getRoomConstraint() == null || room.getRoomConstraint().isAvailable(this, time,
                            getScheduler())))
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
        return sAllowBreakHard;
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

    public Enumeration<Long> getChildrenSubpartIds() {
        return (iChildren == null ? null : iChildren.keys());
    }

    private void addChild(Lecture child) {
        if (iChildren == null)
            iChildren = new Hashtable<Long, List<Lecture>>();
        List<Lecture> childrenThisSubpart = iChildren.get(child.getSchedulingSubpartId());
        if (childrenThisSubpart == null) {
            childrenThisSubpart = new ArrayList<Lecture>();
            iChildren.put(child.getSchedulingSubpartId(), childrenThisSubpart);
        }
        childrenThisSubpart.add(child);
    }

    public boolean isSingleSection() {
        if (iParent == null)
            return (iSameSubpartLectures == null || iSameSubpartLectures.size() <= 1);
        return (iParent.getChildren(getSchedulingSubpartId()).size() <= 1);
    }

    public boolean areStudentConflictsHard(Lecture lecture) {
        return isSingleSection() && lecture.isSingleSection();
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
        Integer ret = iCommitedConflicts.get(placement);
        if (ret == null) {
            ret = new Integer(placement.getCommitedConflicts());
            iCommitedConflicts.put(placement, ret);
        }
        return ret.intValue();
    }

    @Override
    public void assign(long iteration, Placement value) {
        if (value != null && getModel() != null) {
            ((TimetableModel) getModel()).getCommittedStudentConflictsCounter().inc(getCommitedConflicts(value));
        }
        super.assign(iteration, value);
    }

    @Override
    public void unassign(long iteration) {
        if (getAssignment() != null && isCommitted())
            throw new RuntimeException("Unable to unassign committed variable (" + getName() + " "
                    + getAssignment().getName() + ")");
        if (getAssignment() != null && getModel() != null) {
            ((TimetableModel) getModel()).getCommittedStudentConflictsCounter().dec(
                    getCommitedConflicts(getAssignment()));
        }
        super.unassign(iteration);
    }

    public Set<GroupConstraint> hardGroupSoftConstraints() {
        return iHardGroupSoftConstraints;
    }

    public Set<GroupConstraint> groupConstraints() {
        return iGroupConstraints;
    }

    public int minRoomSize() {
        if (iCacheMinRoomSize != null)
            return iCacheMinRoomSize.intValue();
        if (getNrRooms() <= 1) {
            int min = Integer.MAX_VALUE;
            for (RoomLocation r : roomLocations()) {
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference())))
                    continue;
                min = Math.min(min, r.getRoomSize());
            }
            iCacheMinRoomSize = new Integer(min);
            return min;
        } else {
            TreeSet<RoomLocation> rl = new TreeSet<RoomLocation>(roomLocations());
            int min = 0;
            int i = 0;
            for (Iterator<RoomLocation> e = rl.iterator(); e.hasNext() && i < getNrRooms();) {
                RoomLocation r = e.next();
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference())))
                    continue;
                min += r.getRoomSize();
                i++;
            }
            iCacheMinRoomSize = new Integer(min);
            return min;
        }
    }

    public int maxRoomSize() {
        if (iCacheMaxRoomSize != null)
            return iCacheMaxRoomSize.intValue();
        if (getNrRooms() <= 1) {
            int max = Integer.MIN_VALUE;
            for (RoomLocation r : roomLocations()) {
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference())))
                    continue;
                max = Math.max(max, r.getRoomSize());
            }
            iCacheMaxRoomSize = new Integer(max);
            return max;
        } else {
            List<RoomLocation> rl = new ArrayList<RoomLocation>(roomLocations());
            Collections.sort(rl, Collections.reverseOrder());
            int max = 0;
            int i = 0;
            for (Iterator<RoomLocation> e = rl.iterator(); e.hasNext() && i < getNrRooms();) {
                RoomLocation r = e.next();
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference())))
                    continue;
                max += r.getRoomSize();
                i++;
            }
            iCacheMaxRoomSize = new Integer(max);
            return max;
        }
    }

    public long getDiscouragedRoomSize() {
        return Math.round(1.25 * minRoomSize());
    }

    public long getStronglyDiscouragedRoomSize() {
        return Math.round(1.5 * minRoomSize());
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
        return values().size() == 1;
    }

    public boolean isValid(Placement placement) {
        TimetableModel model = (TimetableModel) getModel();
        if (model == null)
            return true;
        if (model.hasConstantVariables()) {
            for (Placement confPlacement : model.conflictValues(placement)) {
                Lecture lecture = confPlacement.variable();
                if (lecture.isCommitted())
                    return false;
                if (confPlacement.equals(placement))
                    return false;
            }
        } else {
            if (model.conflictValues(placement).contains(placement))
                return false;
        }
        return true;
    }

    public String getNotValidReason(Placement placement) {
        TimetableModel model = (TimetableModel) getModel();
        if (model == null)
            return "no model for class " + getName();
        Map<Constraint<Lecture, Placement>, Set<Placement>> conflictConstraints = model.conflictConstraints(placement);
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
                    return placement.getLongName() + " conflicts with " + lecture.getName() + " "
                            + confPlacement.getLongName() + " due to constraint " + cname;
                }
                if (confPlacement.equals(placement)) {
                    return placement.getLongName() + " is not valid due to constraint " + cname;
                }
            }
        }
        return null;
    }

    public void purgeInvalidValues(boolean interactiveMode) {
        if (isCommitted() || Lecture.sSaveMemory)
            return;
        TimetableModel model = (TimetableModel) getModel();
        if (model == null)
            return;
        if (!model.hasConstantVariables())
            return;
        List<Placement> newValues = new ArrayList<Placement>(values().size());
        for (Placement placement : values()) {
            if (placement.isValid())
                newValues.add(placement);
        }
        if (!interactiveMode && newValues.size() != values().size()) {
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

    public boolean isConstant() {
        return iCommitted;
    }

    public int getSpreadPenalty() {
        int spread = 0;
        for (SpreadConstraint sc : getSpreadConstraints()) {
            spread += sc.getPenalty();
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
        if (iMinMaxRoomPreference == null) {
            if (getNrRooms() <= 0 || roomLocations().isEmpty()) {
                iMinMaxRoomPreference = new int[] { 0, 0 };
            } else {
                int minRoomPref = Integer.MAX_VALUE;
                int maxRoomPref = Integer.MIN_VALUE;
                for (RoomLocation r : roomLocations()) {
                    int pref = r.getPreference();
                    if (pref > Constants.sPreferenceLevelRequired / 2)
                        minRoomPref = Math.min(minRoomPref, pref);
                    if (pref < Constants.sPreferenceLevelProhibited / 2)
                        maxRoomPref = Math.max(maxRoomPref, pref);
                }
                iMinMaxRoomPreference = new int[] { minRoomPref, maxRoomPref };
            }
        }
        return iMinMaxRoomPreference;
    }

    private double[] iMinMaxTimePreference = null;

    public double[] getMinMaxTimePreference() {
        if (iMinMaxTimePreference == null) {
            double minTimePref = Double.MAX_VALUE;
            double maxTimePref = -Double.MAX_VALUE;
            for (TimeLocation t : timeLocations()) {
                double pref = t.getNormalizedPreference();
                if (pref > Constants.sPreferenceLevelRequired / 2)
                    minTimePref = Math.min(minTimePref, pref);
                if (pref < Constants.sPreferenceLevelProhibited / 2)
                    maxTimePref = Math.max(maxTimePref, pref);
            }
            iMinMaxTimePreference = new double[] { minTimePref, maxTimePref };
        }
        return iMinMaxTimePreference;
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
}