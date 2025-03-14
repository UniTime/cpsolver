package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.ModelListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;

/**
 * An experimental global constraint that does not allow any two classes that can be attended
 * by the same student to have a conflict. The constraint checks any two classes of different
 * offerings that share at least one student and that the student is allowed to take (not restricted
 * by reservations). Class pairs included in the Ignore Student Conflicts constraints are ignored.
 * Some classes may be excluded by using ExtendedStudentConflicts.IgnoreClasses parameter which may
 * contain a regular expression matching class name(s).
 * <br>
 * Pairs of classes of the same offering are checked, too. In this case, the course structure must
 * allow the two classes to be attended together (e.g., they are from the same configuration), and at
 * least one student in the offering is allowed to take both classes. This feature can be disabled by
 * setting ExtendedStudentConflicts.CheckSameCourse to false.
 * <br>
 * @author Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2013 - 2024 Tomas Muller<br>
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
public class ExtendedStudentConflicts extends GlobalConstraint<Lecture, Placement> implements ModelListener<Lecture, Placement>{
    private String iIgnoreClasses = null;
    private Map<Long, Map<Long, List<Student>>> iCommonStudents = null;
    private Set<Long> iIgnoreClassIds = null;
    private Map<Long, Map<Long, Boolean>> iClassCache = new ConcurrentHashMap<Long, Map<Long, Boolean>>();
    private boolean iCheckSameCourse = true;
    
    @Override
    public void setModel(Model<Lecture, Placement> model) {
        super.setModel(model);
        if (model != null && model instanceof TimetableModel) {
            DataProperties config = ((TimetableModel)model).getProperties();
            iIgnoreClasses = config.getProperty("ExtendedStudentConflicts.IgnoreClasses");
            iCheckSameCourse = config.getPropertyBoolean("ExtendedStudentConflicts.CheckSameCourse", true);
        }
    }
    
    protected void clearCache() {
        iClassCache.clear();
        iCommonStudents = null;
        iIgnoreClassIds = null;
    }
    
    private DistanceMetric getDistanceMetric() {
        return (getModel() == null ? null : ((TimetableModel)getModel()).getDistanceMetric());
    }
    
    protected List<Student> getCommonStudents(Long offeringId1, Long offeringId2) {
        if (iCommonStudents == null) {
            iCommonStudents = new ConcurrentHashMap<Long, Map<Long, List<Student>>>();
            for (Lecture lecture: getModel().variables()) {
                if (lecture.isCommitted() || lecture.getConfiguration() == null) continue;
                Map<Long, List<Student>> commonStudents = iCommonStudents.get(lecture.getConfiguration().getOfferingId());
                if (commonStudents != null) continue;
                commonStudents = new ConcurrentHashMap<Long, List<Student>>();
                iCommonStudents.put(lecture.getConfiguration().getOfferingId(), commonStudents);
                for (Lecture other: getModel().variables()) {
                    if (other.isCommitted() || other.getConfiguration() == null) continue;
                    // if (other.getConfiguration().getOfferingId().equals(lecture.getConfiguration().getOfferingId())) continue;
                    if (commonStudents.containsKey(other.getConfiguration().getOfferingId())) continue;
                    List<Student> students = new ArrayList<Student>();
                    for (Student student: ((TimetableModel)getModel()).getAllStudents()) {
                        if (student.getOfferings().contains(lecture.getConfiguration().getOfferingId()) && student.getOfferings().contains(other.getConfiguration().getOfferingId()))
                            students.add(student);
                    }
                    commonStudents.put(other.getConfiguration().getOfferingId(), students);
                }
            }
        }
        Map<Long, List<Student>> offeringIds = iCommonStudents.get(offeringId1);
        return (offeringIds == null ? null :  offeringIds.get(offeringId2));
    }
    
    protected boolean isIgnoreClass(Lecture lecture) {
        if (iIgnoreClassIds == null) {
            iIgnoreClassIds = new HashSet<Long>();
            if (iIgnoreClasses != null && !iIgnoreClasses.isEmpty())
                for (Lecture l: getModel().variables()) {
                    if (l.getName().matches(iIgnoreClasses)) iIgnoreClassIds.add(l.getClassId());
                }
        }
        return iIgnoreClassIds.contains(lecture.getClassId());
        
    }
    
    private Boolean getCachedPair(Lecture l1, Lecture l2) {
        if (l1.getClassId() < l2.getClassId()) {
            Map<Long, Boolean> cache = iClassCache.get(l1.getClassId());
            return (cache == null ? null : cache.get(l2.getClassId()));
        } else {
            Map<Long, Boolean> cache = iClassCache.get(l2.getClassId());
            return (cache == null ? null : cache.get(l1.getClassId()));
        }
    }
    
    private void setCachedPair(Lecture l1, Lecture l2, boolean value) {
        if (l1.getClassId() < l2.getClassId()) {
            Map<Long, Boolean> cache = iClassCache.get(l1.getClassId());
            if (cache == null) {
                cache = new ConcurrentHashMap<Long, Boolean>();
                iClassCache.put(l1.getClassId(), cache);
            }
            cache.put(l2.getClassId(), value);
        } else {
            Map<Long, Boolean> cache = iClassCache.get(l2.getClassId());
            if (cache == null) {
                cache = new ConcurrentHashMap<Long, Boolean>();
                iClassCache.put(l2.getClassId(), cache);
            }
            cache.put(l1.getClassId(), value);
        }
    }
    
    private boolean checkSameCourseCanTakeTogether(Lecture l1, Lecture l2) {
        // check if the feature is disabled
        if (!iCheckSameCourse) return false;
        // same subpart -> cannot take together
        if (l1.getSchedulingSubpartId().equals(l2.getSchedulingSubpartId())) return false;
        // different config -> cannot take together
        if (!l1.getConfiguration().equals(l2.getConfiguration())) return false;
        // subpart id > class id (classes that are given by class l1 and its parents)
        Map<Long, Long> mustTake = new HashMap<Long, Long>();
        for (Lecture l = l1; l != null; l = l.getParent()) {
            mustTake.put(l.getSchedulingSubpartId(), l.getClassId());
        }
        // also include top-level subparts of the same configuration that have only one class 
        for (Map.Entry<Long, Set<Lecture>> e: l1.getConfiguration().getTopLectures().entrySet()) {
            if (e.getValue().size() == 1) {
                Lecture l = e.getValue().iterator().next();
                mustTake.put(l.getSchedulingSubpartId(), l.getClassId());
            }
        }
        // check l2 and its parents, if any of them does not follow mustTake -> cannot take together
        for (Lecture l = l2; l != null; l = l.getParent()) {
            Long id = mustTake.get(l.getSchedulingSubpartId());
            if (id != null && !l.getClassId().equals(id)) return false;
        }
        // no issue found -> can take together
        return true;
    }
    
    protected boolean checkStudentForStudentConflicts(Lecture l1, Lecture l2) {
        // are student conflicts between the two classes to be ignored ?
        if (l1.isToIgnoreStudentConflictsWith(l2)) return false;
        // check the cache
        Boolean cache = getCachedPair(l1, l2);
        if (cache != null) return cache.booleanValue();
        // classes of the same offering that cannot be taken together
        if (l1.getConfiguration().getOfferingId().equals(l2.getConfiguration().getOfferingId()) && !checkSameCourseCanTakeTogether(l1, l2)) {
            setCachedPair(l1, l2, false);
            return false;
        }
        // ignore matching class pairs
        if (isIgnoreClass(l1) && isIgnoreClass(l2)) {
            setCachedPair(l1, l2, false);
            return false;
        }
        // check offerings
        List<Student> commonStudents = getCommonStudents(l1.getConfiguration().getOfferingId(), l2.getConfiguration().getOfferingId());
        // less then two students in common > do not check for conflicts
        if (commonStudents == null || commonStudents.size() <= 1) {
            setCachedPair(l1, l2, false);
            return false;
        }
        // check if there is a student that can attend l1 and l2 together
        for (Student student: commonStudents)
            if (student.canEnroll(l1) && student.canEnroll(l2)) {
                setCachedPair(l1, l2, true);
                return true;
            }
        // no common students that can attend both classes
        setCachedPair(l1, l2, false);
        return false;
    }
    
    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement placement, Set<Placement> conflicts) {
        Lecture lecture = placement.variable();
        for (Lecture other: getModel().assignedVariables(assignment)) {
            Placement otherPlacement = assignment.getValue(other);
            if (checkStudentForStudentConflicts(lecture, other) && JenrlConstraint.isInConflict(placement, otherPlacement, getDistanceMetric(), 0))
                conflicts.add(otherPlacement);
        }
    }
    
    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
        Lecture lecture = placement.variable();
        for (Lecture other: getModel().assignedVariables(assignment)) {
            Placement otherPlacement = assignment.getValue(other);
            if (checkStudentForStudentConflicts(lecture, other) && JenrlConstraint.isInConflict(placement, otherPlacement, getDistanceMetric(), 0))
                return true;
        }
        return false;
    }

    @Override
    public boolean isConsistent(Placement p1, Placement p2) {
        return p1 != null && p2 != null &&
                checkStudentForStudentConflicts(p1.variable(), p2.variable()) &&
                JenrlConstraint.isInConflict(p1, p2, getDistanceMetric(), 0);
    }
    
    @Override
    public String getName() {
        return "Extended Student Conflicts";
    }
    
    @Override
    public String toString() {
        return "Extended Student Conflicts";
    }

    @Override
    public void variableAdded(Lecture variable) {
        clearCache();
    }

    @Override
    public void variableRemoved(Lecture variable) {
        clearCache();
    }

    @Override
    public void constraintAdded(Constraint<Lecture, Placement> constraint) {}

    @Override
    public void constraintRemoved(Constraint<Lecture, Placement> constraint) {}

    @Override
    public void beforeAssigned(Assignment<Lecture, Placement> assignment, long iteration, Placement value) {}

    @Override
    public void beforeUnassigned(Assignment<Lecture, Placement> assignment, long iteration, Placement value) {}

    @Override
    public void afterAssigned(Assignment<Lecture, Placement> assignment, long iteration, Placement value) {}

    @Override
    public void afterUnassigned(Assignment<Lecture, Placement> assignment, long iteration, Placement value) {}

    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        clearCache();
        return true;
    }
}