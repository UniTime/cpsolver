package org.cpsolver.coursett.sectioning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.StudentGroup;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.JProf;

/**
 * A model representing student enrollments of a course. Branch &amp; bound algorithm
 * is used to propose best possible enrollment of all students into the given course.
 *  
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2017 Tomas Muller<br>
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
public class SctModel {
    public static double sEps = 0.0001;
    private long iTimeOut = 1000;
    private boolean iUseCriteria = true;
    private TimetableModel iModel;
    private Assignment<Lecture, Placement> iAssignment;
    private List<StudentConflict> iStudentConflictCriteria = null;
    private List<Configuration> iConfigurations = null;
    private List<SctStudent> iStudents = null;
    private Long iOfferingId = null;
    private Map<Long, Double> iLimits = new HashMap<Long, Double>();
    private Map<Long, Map<Long, Set<Lecture>>> iSubparts = new HashMap<Long, Map<Long, Set<Lecture>>>();
    private boolean iTimeOutReached = false;
    private boolean iGroupFirst = false;
    
    /**
     * Constructor
     * @param model course timetabling model
     * @param assignment current assignment
     */
    public SctModel(TimetableModel model, Assignment<Lecture, Placement> assignment) {
        iModel = model;
        iAssignment = assignment;
        iTimeOut = model.getProperties().getPropertyLong("SctSectioning.TimeOut", 1000);
        iUseCriteria = model.getProperties().getPropertyBoolean("SctSectioning.UseCriteria", true);
        iGroupFirst = model.getProperties().getPropertyBoolean("SctSectioning.GroupFirst", false);
    }
    
    /**
     * List of student conflict criteria
     */
    public List<StudentConflict> getStudentConflictCriteria() {
        if (!iUseCriteria) return null;
        if (iStudentConflictCriteria == null && iModel != null) {
            iStudentConflictCriteria = new ArrayList<StudentConflict>();
            for (Criterion<Lecture, Placement> criterion: iModel.getCriteria())
                if (criterion instanceof StudentConflict)
                    iStudentConflictCriteria.add((StudentConflict)criterion);
        }
        return iStudentConflictCriteria;
    }
    
    /**
     * All configuration of the selected offering
     */
    public List<Configuration> getConfigurations() { return iConfigurations; }
    
    /**
     * Select an offering for the model
     */
    public void setConfiguration(Configuration config) {
        iConfigurations = new ArrayList<Configuration>();
        iConfigurations.add(config);
        iOfferingId = config.getOfferingId();
        if (config.getAltConfigurations() != null)
            for (Configuration alt: config.getAltConfigurations())
                if (!alt.equals(config)) iConfigurations.add(alt);
        iStudents = new ArrayList<SctStudent>();
        Set<Long> studentIds = new HashSet<Long>();
        for (Configuration c: iConfigurations)
            for (Lecture l: c.getTopLectures(c.getTopSubpartIds().iterator().next())) {
                for (Student s: l.students()) {
                    if (studentIds.add(s.getId()))
                        iStudents.add(new SctStudent(this, s));
                }
            }
        for (Student student: getTimetableModel().getAllStudents())
            if (student.hasOffering(getOfferingId()))
                if (studentIds.add(student.getId()))
                    iStudents.add(new SctStudent(this, student));
        Collections.sort(iStudents);
    }
    
    /**
     * List of scheduling subparts and their classes of the given configuration
     */
    public Map<Long, Set<Lecture>> getSubparts(Configuration configuration) {
        Map<Long, Set<Lecture>> subparts = iSubparts.get(configuration.getConfigId());
        if (subparts == null) {
            subparts = new HashMap<Long, Set<Lecture>>();
            Queue<Lecture> queue = new LinkedList<Lecture>();
            for (Map.Entry<Long, Set<Lecture>> e: configuration.getTopLectures().entrySet()) {
                subparts.put(e.getKey(), e.getValue());
                queue.addAll(e.getValue());
            }
            Lecture lecture = null;
            while ((lecture = queue.poll()) != null) {
                if (lecture.getChildren() != null)
                    for (Map.Entry<Long, List<Lecture>> e: lecture.getChildren().entrySet()) {
                        Set<Lecture> lectures = subparts.get(e.getKey());
                        if (lectures == null) {
                            lectures = new HashSet<Lecture>(e.getValue());
                            subparts.put(e.getKey(), lectures);
                        } else {
                            lectures.addAll(e.getValue());
                        }
                        queue.addAll(e.getValue());
                    }
            }
            iSubparts.put(configuration.getConfigId(), subparts);
        }
        return subparts;
    }
    
    /**
     * Selected offering id
     */
    public Long getOfferingId() { return iOfferingId; }
    
    /**
     * Course timetabling model
     */
    public TimetableModel getTimetableModel() { return iModel; }
    
    /**
     * Current assignment
     */
    public Assignment<Lecture, Placement> getAssignment() { return iAssignment; }
    
    /**
     * Enrollment of the given class
     */
    private double getEnrollment(Lecture lecture, Map<Long, Double> limits) {
        Double enrollment = limits.get(lecture.getClassId());
        return enrollment == null ? 0.0 : enrollment.doubleValue();
    }
    
    /**
     * Increment enrollment of the given class
     */
    private void incEnrollment(Lecture lecture, Map<Long, Double> limits, double weight) {
        Double enrollment = limits.get(lecture.getClassId());
        limits.put(lecture.getClassId(), (enrollment == null ? 0.0 : enrollment.doubleValue()) + weight);
    }
    
    /**
     * Increment enrollment of all classes of the given classes
     */
    private void incEnrollment(SctStudent student, SctEnrollment enrollment, Map<Long, Double> limits, Map<Long, Map<Long, Match>> matches) {
        for (Lecture lecture: enrollment.getLectures())
            incEnrollment(lecture, limits, student.getOfferingWeight());
        for (StudentGroup group: student.getStudent().getGroups()) {
            Map<Long, Match> match = matches.get(group.getId());
            if (match == null) { match = new HashMap<Long, Match>(); matches.put(group.getId(), match); }
            for (Lecture lecture: enrollment.getLectures()) {
                Match m = match.get(lecture.getSchedulingSubpartId());
                if (m == null) { m = new Match(group, lecture); match.put(lecture.getSchedulingSubpartId(), m); }
                m.inc(lecture);
            }
        }
    }

    /**
     * Decrement enrollment of all classes of the given classes
     */
    private void decEnrollment(SctStudent student, SctEnrollment enrollment, Map<Long, Double> limits, Map<Long, Map<Long, Match>> matches) {
        for (Lecture lecture: enrollment.getLectures())
            incEnrollment(lecture, limits, -student.getOfferingWeight());
        for (StudentGroup group: student.getStudent().getGroups()) {
            Map<Long, Match> match = matches.get(group.getId());
            if (match == null) { match = new HashMap<Long, Match>(); matches.put(group.getId(), match); }
            for (Lecture lecture: enrollment.getLectures()) {
                Match m = match.get(lecture.getSchedulingSubpartId());
                if (m == null) { m = new Match(group, lecture); match.put(lecture.getSchedulingSubpartId(), m); }
                m.dec(lecture);
            }
        }
    }
    
    /**
     * Class limit
     */
    private double getLimit(Lecture lecture) {
        Double limit = iLimits.get(lecture.getClassId());
        if (limit == null) {
            limit = Math.max(lecture.classLimit(getAssignment()), lecture.nrWeightedStudents()) - sEps;
            iLimits.put(lecture.getClassId(), limit);
        }
        return limit;
    }

    /**
     * Check if all classes of the given enrollment are available (the limit is not breached)
     */
    private boolean isAvailable(SctStudent student, SctEnrollment enrollment, Map<Long, Double> limits) {
        for (Lecture lecture: enrollment.getLectures())
            if (getEnrollment(lecture, limits) > getLimit(lecture)) return false;
        return true;
    }
    
    /**
     * Group weight of the given enrollments
     */
    private double group(SctEnrollment[] enrollments) {
        Map<Long, Map<Long, Match>> matches = new HashMap<Long, Map<Long, Match>>();
        for (SctEnrollment enrollment: enrollments) {
            if (enrollment == null) continue;
            for (StudentGroup group: enrollment.getStudent().getStudent().getGroups()) {
                Map<Long, Match> match = matches.get(group.getId());
                if (match == null) { match = new HashMap<Long, Match>(); matches.put(group.getId(), match); }
                for (Lecture lecture: enrollment.getLectures()) {
                    Match m = match.get(lecture.getSchedulingSubpartId());
                    if (m == null) { m = new Match(group, lecture.getConfiguration()); match.put(lecture.getSchedulingSubpartId(), m); }
                    m.inc(lecture);
                }
            }
        }
        double ret = 0.0;
        for (Map<Long, Match> match: matches.values()) {
            for (Match m: match.values())
                ret += m.value();
        }
        return ret;
    }
    
    /**
     * Group weight of the given enrollments (up until the given index, computing bound for students above the index)
     */
    protected double group(SctEnrollment[] enrollments, int index,  Map<Long, Double> limits, Map<Long, Map<Long, Match>> matches) {
        // Map<Long, Map<Long, Match>> matches = new HashMap<Long, Map<Long, Match>>();
        Map<Long, UnMatched> unmatched = new HashMap<Long, UnMatched>();
        for (int i = index; i < iStudents.size(); i++) {
            SctStudent student = iStudents.get(i);
            for (StudentGroup group: student.getStudent().getGroups()) {
                UnMatched m = unmatched.get(group.getId());
                if (m == null) { m = new UnMatched(group); unmatched.put(group.getId(), m); }
                m.incBound(student);
            }
            /*
            if (i < index) {
                SctEnrollment enrollment = enrollments[i];
                if (enrollment == null) continue;
                for (StudentGroup group: student.getStudent().getGroups()) {
                    Map<Long, Match> match = matches.get(group.getId());
                    if (match == null) { match = new HashMap<Long, Match>(); matches.put(group.getId(), match); }
                    for (Lecture lecture: enrollment.getLectures()) {
                        Match m = match.get(lecture.getSchedulingSubpartId());
                        if (m == null) { m = new Match(group, lecture.getConfiguration()); match.put(lecture.getSchedulingSubpartId(), m); }
                        m.inc(lecture);
                    }
                }
            } else {
                for (StudentGroup group: student.getStudent().getGroups()) {
                    Map<Long, Match> match = matches.get(group.getId());
                    if (match == null) {
                        UnMatched m = unmatched.get(group.getId());
                        if (m == null) { m = new UnMatched(group); unmatched.put(group.getId(), m); }
                        m.incBound();
                        continue;
                    }
                    boolean increased = false;
                    for (Configuration configuration: iConfigurations) {
                        for (Long subpartId: getSubparts(configuration).keySet()) {
                            Match m = match.get(subpartId);
                            if (m != null && m.incBound()) increased = true;
                        }
                        if (increased) break;
                    }
                    if (!increased) {
                        UnMatched m = unmatched.get(group.getId());
                        if (m == null) { m = new UnMatched(group); unmatched.put(group.getId(), m); }
                        m.incBound();
                    }
                }
            }*/
        }
        double ret = 0.0;
        for (Map.Entry<Long, Map<Long, Match>> match: matches.entrySet()) {
            for (Match m: match.getValue().values())
                ret += m.value(unmatched.remove(match.getKey()), limits);
        }
        for (UnMatched m: unmatched.values()) {
            ret += m.value();
        }
        return ret;
    }
    
    /**
     * Compute best possible enrollment of students into the given offering
     */
    public void computeSolution(SctSolution solution, int index, SctEnrollment[] enrollments, Map<Long, Double> limits, Map<Long, Map<Long, Match>> match, double totalConflicts, long t0) {
        if (iTimeOutReached) return;
        if (JProf.currentTimeMillis() - t0 > iTimeOut) {
            iTimeOutReached = true; return;
        }
        if (index < iStudents.size()) {
            if (!solution.checkBound(index, enrollments, totalConflicts, limits, match)) return;
            SctStudent student = iStudents.get(index);
            for (SctEnrollment enrollment: student.getEnrollments(new SctEnrollmentComparator(limits, match, index))) {
                if (!isAvailable(student, enrollment, limits)) continue;
                enrollments[index] = enrollment;
                incEnrollment(student, enrollment, limits, match);
                computeSolution(solution, index + 1, enrollments, limits, match, totalConflicts + enrollment.getConflictWeight(), t0);
                decEnrollment(student, enrollment, limits, match);
            }
        } else {
            if (solution.isBetter(enrollments, totalConflicts))
                solution.record(enrollments, totalConflicts);
        }
    }
    
    /**
     * Compute best possible enrollment of students into the given offering
     */
    public SctSolution computeSolution() {
        SctSolution solution = currentSolution();
        iTimeOutReached = false;
        computeSolution(solution, 0, new SctEnrollment[iStudents.size()], new HashMap<Long, Double>(), new HashMap<Long, Map<Long, Match>>(), 0.0, JProf.currentTimeMillis());
        return solution;
    }
    
    /**
     * Was time out reached while {@link SctModel#computeSolution()} was called?
     */
    public boolean isTimeOutReached() { return iTimeOutReached; }
    
    public SctSolution currentSolution() {
        SctEnrollment[] enrollments = new SctEnrollment[iStudents.size()];
        for (int index = 0; index < iStudents.size(); index++)
            enrollments[index] = iStudents.get(index).getCurrentEnrollment(true);
        return new SctSolution(enrollments);
    }
    
    /**
     * Decrement {@link JenrlConstraint} between the given two classes by the given student
     */
    protected void decJenrl(Assignment<Lecture, Placement> assignment, Student student, Lecture l1, Lecture l2) {
        if (l1.equals(l2)) return;
        JenrlConstraint jenrl = l1.jenrlConstraint(l2);
        if (jenrl != null) {
            jenrl.decJenrl(assignment, student);
            /*
            if (jenrl.getNrStudents() == 0) {
                jenrl.getContext(assignment).unassigned(assignment, null);
                Object[] vars = jenrl.variables().toArray();
                for (int k = 0; k < vars.length; k++)
                    jenrl.removeVariable((Lecture) vars[k]);
                iModel.removeConstraint(jenrl);
            }
            */
        }
    }
    
    /**
     * Increment {@link JenrlConstraint} between the given two classes by the given student
     */
    protected void incJenrl(Assignment<Lecture, Placement> assignment, Student student, Lecture l1, Lecture l2) {
        if (l1.equals(l2)) return;
        JenrlConstraint jenrl = l1.jenrlConstraint(l2);
        if (jenrl == null) {
            jenrl = new JenrlConstraint();
            jenrl.addVariable(l1);
            jenrl.addVariable(l2);
            iModel.addConstraint(jenrl);
        }
        jenrl.incJenrl(assignment, student);
    }
    
    /**
     * Unassign all previous enrollments into the given offering
     */
    public void unassign() {
        for (SctStudent student: iStudents) {
            Configuration configuration = null;
            for (Lecture lecture: student.getCurrentEnrollment(false).getLectures()) {
                if (configuration == null) configuration = lecture.getConfiguration();
                for (Lecture other: student.getStudent().getLectures())
                    decJenrl(getAssignment(), student.getStudent(), lecture, other);
                lecture.removeStudent(getAssignment(), student.getStudent());
                student.getStudent().removeLecture(lecture);
            }
            if (configuration != null)
                student.getStudent().removeConfiguration(configuration);
        }
    }
    
    /**
     * Assign given solution
     */
    public void assign(SctSolution solution) {
        for (int index = 0; index < iStudents.size(); index++) {
            SctStudent student = iStudents.get(index);
            Configuration configuration = null;
            SctEnrollment enrollment = solution.iEnrollments[index];
            if (enrollment == null) continue;
            for (Lecture lecture: enrollment.getLectures()) {
                if (configuration == null) configuration = lecture.getConfiguration();
                for (Lecture other: student.getStudent().getLectures())
                    incJenrl(getAssignment(), student.getStudent(), lecture, other);
                lecture.addStudent(getAssignment(), student.getStudent());
                student.getStudent().addLecture(lecture);
            }
            if (configuration != null)
                student.getStudent().addConfiguration(configuration);
        }
    }
    
    /**
     * Enrollment solution. Represent enrollments of all students into the given course.
     */
    class SctSolution {
        private double iWeight = 0.0;
        private double iGroup = 0.0;
        private SctEnrollment[] iEnrollments = null;
        
        /**
         * Constructor (for empty solution)
         */
        public SctSolution() {}
        
        /**
         * Constructor (for given solution)
         * @param enrollments given solution
         */
        public SctSolution(SctEnrollment[] enrollments) {
            iEnrollments = enrollments;
            iWeight = 0.0;
            for (SctEnrollment enrollment: enrollments)
                if (enrollment != null) iWeight += enrollment.getConflictWeight();
            iGroup = group(enrollments);
        }
        
        /**
         * Compare two solutions
         */
        public boolean isBetter(SctEnrollment[] solution, double weight) {
            if (iGroupFirst) {
                if (iEnrollments == null) return true;
                double gr = group(solution);
                return gr > iGroup || (gr == iGroup && weight < iWeight);
            } else {
                return iEnrollments == null || weight < iWeight || (weight == iWeight && group(solution) > iGroup);
            }
        }
        
        /**
         * Compare two solutions
         */
        public boolean isBetter(SctSolution other) {
            if (iGroupFirst) {
                if (iEnrollments == null) return true;
                return other.getGroup() < iGroup || (other.getGroup() == iGroup && iWeight < other.getWeight());
            } else {
                return iEnrollments != null && (iWeight < other.getWeight() || (other.getWeight() == iWeight && other.getGroup() < iGroup));
            }
        }
        
        /**
         * Record given solution 
         */
        public void record(SctEnrollment[] solution, double weight) {
            iEnrollments = Arrays.copyOf(solution, solution.length);
            iWeight = weight;
            iGroup = group(solution);
        }
        
        /**
         * Check bounds (false means no better solution exists by extending the given solution) 
         */
        public boolean checkBound(int index, SctEnrollment[] solution, double weight, Map<Long, Double> limits, Map<Long, Map<Long, Match>> match) {
            if (iEnrollments == null) return true;
            if (iGroupFirst) {
                double gr = group(solution, index, limits, match);
                if (gr == iGroup) {
                    double guess = weight;
                    for (int i = index; i < iStudents.size(); i++) {
                        SctStudent student = iStudents.get(i);
                        SctEnrollment enrollment = null;
                        for (SctEnrollment e: student.getEnrollments()) {
                            if (isAvailable(student, e, limits)) { enrollment = e; break; }
                        }
                        if (enrollment == null) return false;
                        guess += enrollment.getConflictWeight();
                        if (guess >= iWeight) break;
                    }
                    return guess < iWeight;
                }
                return gr > iGroup;
            } else {
                double guess = weight;
                for (int i = index; i < iStudents.size(); i++) {
                    SctStudent student = iStudents.get(i);
                    SctEnrollment enrollment = null;
                    for (SctEnrollment e: student.getEnrollments()) {
                        if (isAvailable(student, e, limits)) { enrollment = e; break; }
                    }
                    if (enrollment == null) return false;
                    guess += enrollment.getConflictWeight();
                    if (guess > iWeight) break;
                }
                return (guess < iWeight || (guess == iWeight && group(solution, index, limits, match) > iGroup));
            }
        }
        
        /**
         * Individual student enrollments
         */
        public SctEnrollment[] getEnrollments() { return iEnrollments; }
        /**
         * Overall conflict weight
         */
        public double getWeight() { return iWeight; }
        /**
         * Overall group weight
         */
        public double getGroup() { return iGroup; }
        /**
         * False if empty
         */
        public boolean isValid() { return iEnrollments != null; }
    }
    
    /**
     * Matching students within a scheduling subpart (for student group weight computation)
     */
    private class Match { 
        private int iTotal = 0;
        private Map<Lecture, Integer> iMatch = new HashMap<Lecture, Integer>();
        private double iFraction = 1.0;
        
        /**
         * Constructor
         * @param group student group
         */
        Match(StudentGroup group, Lecture lecture) {
            this(group, lecture.getConfiguration());
            for (Lecture l: lecture.sameSubpartLectures())
                iMatch.put(l, 0);
        }
        
        Match(StudentGroup group, Configuration config) {
            iTotal = group.countStudents(getOfferingId());
            iFraction = 1.0 / getSubparts(config).size();
        }
        
        /**
         * Increment given lecture
         */
        void inc(Lecture lecture) {
            Integer val = iMatch.get(lecture);
            iMatch.put(lecture, 1 + (val == null ? 0 : val.intValue()));
        }
        
        /**
         * Decrement given lecture
         */
        void dec(Lecture lecture) {
            Integer val = iMatch.get(lecture);
            iMatch.put(lecture, (val == null ? 0 : val.intValue()) - 1);
        }
        
        /**
         * Returns counter for the given lecture
         */
        int get(Lecture lecture) {
            Integer val = iMatch.get(lecture);
            return val == null ? 0 : val.intValue();
        }
        
        /**
         * Value (an overall probability of two students being in the same lecture) 
         */
        double value(UnMatched u, final Map<Long, Double> limits) {
            if (iTotal <= 1) return iFraction;
            if (u == null || u.getNotMatched() == 0) return value();
            double value = 0.0;
            int unmatched = u.getNotMatched();
            double remains = u.getEnrollmentWeight();
            double avgWeight = remains / unmatched;
            TreeSet<Map.Entry<Lecture, Integer>> entries = new TreeSet<Map.Entry<Lecture, Integer>>(new Comparator<Map.Entry<Lecture, Integer>>() {
                @Override
                public int compare(Entry<Lecture, Integer> e1, Entry<Lecture, Integer> e2) {
                    if (e1.getValue() > e2.getValue()) return -1;
                    if (e1.getValue() < e2.getValue()) return 1;
                    double r1 = getLimit(e1.getKey()) - getEnrollment(e1.getKey(), limits);
                    double r2 = getLimit(e2.getKey()) - getEnrollment(e2.getKey(), limits);
                    int cmp = Double.compare(r2, r1);
                    if (cmp != 0) return cmp;
                    return e1.getKey().compareTo(e2.getKey());
                }
            });
            entries.addAll(iMatch.entrySet());
            for (Map.Entry<Lecture, Integer> entry: entries) {
                Integer m = entry.getValue();
                if (unmatched > 0) {
                    double enroll = Math.min(remains, getLimit(entry.getKey()) - getEnrollment(entry.getKey(), limits));
                    int inc = (int)Math.round(enroll / avgWeight);
                    if (inc > 0) {
                        m += inc;
                        unmatched -= inc;
                        remains -= enroll;
                    }
                }
                if (m > 1)
                    value += (m * (m - 1.0)) / (iTotal * (iTotal - 1.0));
            }
            if (unmatched > 1)
                value += (unmatched * (unmatched - 1.0)) / (iTotal * (iTotal - 1.0));
            return value * iFraction;
        }
        
        double value() {
            if (iTotal <= 1) return iFraction;
            double value = 0.0;
            for (Integer m: iMatch.values())
                if (m > 1) {
                    value += (m * (m - 1.0)) / (iTotal * (iTotal - 1.0));
                }
            return value * iFraction;
        }
        
        @Override
        public String toString() {
            return iTotal + "/" + iMatch + "[" + value() + "]";
        }
    }
    
    /**
     * Matching students within a scheduling subpart (for student group weight computation)
     */
    private class UnMatched { 
        private int iTotal = 0;
        private int iNotMatched = 0;
        private double iEnrollmentWeight = 0.0;
        
        /**
         * Constructor
         * @param group student group
         */
        UnMatched(StudentGroup group) {
            iTotal = group.countStudents(getOfferingId());
        }
        
        /**
         * Increment bound (a student gets into the best possible class)
         */
        void incBound(SctStudent student) {
            iNotMatched ++;
            iEnrollmentWeight += student.getStudent().getOfferingWeight(getOfferingId());
        }
        
        /**
         * Value (an overall probability of two students being in the same lecture) 
         */
        double value() {
            if (iTotal <= 1) return 1.0;
            if (iNotMatched > 1)
                return (iNotMatched * (iNotMatched - 1.0)) / (iTotal * (iTotal - 1.0));
            return 0.0;
        }
        
        int getNotMatched() { return iNotMatched; }
        
        public double getEnrollmentWeight() { return iEnrollmentWeight; }
        
        @Override
        public String toString() {
            return iTotal + "/" + iNotMatched;
        }
    }
    
    private class SctEnrollmentComparator implements Comparator<SctEnrollment> {
        private Map<Long, Double> limits;
        private Map<Long, Map<Long, Match>> matches;
        private int index;
        
        SctEnrollmentComparator(Map<Long, Double> limits, Map<Long, Map<Long, Match>> match, int index) {
            this.limits = limits; this.matches = match; this.index = index;
        }
        
        public int compareByGroup(SctEnrollment e1, SctEnrollment e2) {
            double m1 = 0, m2 = 0;
            for (StudentGroup g: e1.getStudent().getStudent().getGroups()) {
                int remaining = 0;
                int total = g.countStudents(getOfferingId());
                double remainingWeight = 0.0;
                for (int i = index; i < iStudents.size(); i++) {
                    SctStudent student = iStudents.get(i);
                    if (student.getStudent().hasGroup(g)) {
                        remaining++;
                        remainingWeight += student.getStudent().getOfferingWeight(getOfferingId());
                    }
                }
                double avgWeight = remainingWeight / remaining;
                Map<Long, Match> match = matches.get(g.getId());
                for (Lecture lecture: e1.getLectures()) {
                    Match m = (match == null ? null : match.get(lecture.getSchedulingSubpartId()));
                    double fraction = 1.0 / getSubparts(lecture.getConfiguration()).size();
                    int a = (m == null ? 0 : m.get(lecture));
                    double enroll = Math.min(remainingWeight, getLimit(lecture) - getEnrollment(lecture, limits));
                    a += (int)Math.round(enroll / avgWeight);
                    m1 += fraction * (a * (a - 1)) / ((total * (total - 1))); 
                }
                for (Lecture lecture: e2.getLectures()) {
                    Match m = (match == null ? null : match.get(lecture.getSchedulingSubpartId()));
                    double fraction = 1.0 / getSubparts(lecture.getConfiguration()).size();
                    int a = (m == null ? 0 : m.get(lecture));
                    double enroll = Math.min(remainingWeight, getLimit(lecture) - getEnrollment(lecture, limits));
                    a += (int)Math.round(enroll / avgWeight);
                    m2 += fraction * (a * (a - 1)) / ((total * (total - 1))); 
                }
            }
            if (m1 != m2) return m1 > m2 ? -1 : 1;
            return 0;
        }

        @Override
        public int compare(SctEnrollment e1, SctEnrollment e2) {
            if (iGroupFirst) {
                int cmp = compareByGroup(e1, e2);
                if (cmp != 0) return cmp;
                cmp = Double.compare(e1.getConflictWeight(), e2.getConflictWeight());
                if (cmp != 0) return cmp;
            } else {
                int cmp = Double.compare(e1.getConflictWeight(), e2.getConflictWeight());
                if (cmp != 0) return cmp;
                cmp = compareByGroup(e1, e2);
                if (cmp != 0) return cmp;
            }
            return e1.getId().compareTo(e2.getId());
        }
        
    }
}