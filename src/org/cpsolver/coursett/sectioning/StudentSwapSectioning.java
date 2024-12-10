package org.cpsolver.coursett.sectioning;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.DefaultStudentSectioning;
import org.cpsolver.coursett.model.InitialSectioning.Group;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.StudentGroup;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.coursett.sectioning.SctSectioning.GroupBasedInitialSectioning;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.termination.TerminationCondition;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;

/**
 * Student sectioning implementation based on local search. A student swap is
 * generated in each iteration using Hill Climbing and Great Deluge algorithms.
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
public class StudentSwapSectioning extends DefaultStudentSectioning implements InfoProvider<Lecture, Placement> {
    private static double sEps = 0.0001;
    private double iGroupWeight = 0.1;
    private int iMaxIdleResection = 1000;

    public StudentSwapSectioning(TimetableModel model) {
        super(model);
        iGroupWeight = model.getProperties().getPropertyDouble("StudentSwaps.GroupWeight", 10.0);
        iMaxIdleResection = model.getProperties().getPropertyInt("StudentSwaps.MaxIdleResection", 1000);
    }
    
    protected List<StudentConflict> getStudentConflictCriteria() {
        if (iModel != null) return iModel.getStudentConflictCriteria();
        return null;
    }
    
    @Override
    public boolean hasFinalSectioning() {
        return true;
    }
    
    /**
     * Student conflict weight change of a student swap 
     */
    protected double objective(Neighbour<Lecture, Placement> n, Assignment<Lecture, Placement> assignment) {
        if (n instanceof StudentMove)
            return ((StudentMove)n).value(getStudentConflictCriteria(), assignment);
        return n.value(assignment);
    }
    
    /**
     * Student group weight change of a student swap 
     */
    protected double group(Neighbour<Lecture, Placement> n, Assignment<Lecture, Placement> assignment) {
        if (n instanceof StudentMove)
            return ((StudentMove)n).group(getStudentConflictCriteria(), assignment);
        return 0.0;
    }
    
    /**
     * Combined weight change of a student swap 
     */
    protected double value(Neighbour<Lecture, Placement> n, Assignment<Lecture, Placement> assignment) {
        if (n instanceof StudentMove)
            return ((StudentMove)n).value(getStudentConflictCriteria(), assignment) - iGroupWeight * ((StudentMove)n).group(getStudentConflictCriteria(), assignment);
        return n.value(assignment);
    }
    
    /**
     * Student conflict weight of a solution 
     */
    protected double objective(Solution<Lecture, Placement> solution) {
        List<StudentConflict> criteria = getStudentConflictCriteria();
        
        if (criteria == null) {
            double value = 0.0;
            for (JenrlConstraint constraint: ((TimetableModel)solution.getModel()).getJenrlConstraints()) {
                if (constraint.isInConflict(solution.getAssignment())) value += constraint.jenrl();
            }
            return value;
        }
        
        double value = 0.0;
        for (StudentConflict criterion: criteria)
            value += criterion.getWeightedValue(solution.getAssignment());
        return value;
    }
    
    /**
     * Student group weight of a solution 
     */
    public static double group(TimetableModel model) {
        double ret = 0;
        for (StudentGroup group: model.getStudentGroups()) {
            Map<Long, Match> match = new HashMap<Long, Match>();
            Set<Long> offeringIds = new HashSet<Long>();
            for (Student student: group.getStudents())
                for (Lecture lecture: student.getLectures()) {
                    if (lecture.getConfiguration() == null) continue;
                    offeringIds.add(lecture.getConfiguration().getOfferingId());
                    Match m = match.get(lecture.getSchedulingSubpartId());
                    if (m == null) { m = new Match(group, lecture.getConfiguration()); match.put(lecture.getSchedulingSubpartId(), m); }
                    m.inc(lecture);
                }
            double value = 0.0;
            for (Match m: match.values())
                value += m.value();
            ret += value / offeringIds.size();
        }
        return ret;
    }
    
    /**
     * Student group percentage of a solution subset
     */
    public static double gp(TimetableModel model, Collection<Lecture> variables) {
        if (model.getStudentGroups().isEmpty()) return 0.0;
        double ret = 0; int count = 0;
        for (StudentGroup group: model.getStudentGroups()) {
            Map<Long, Match> match = new HashMap<Long, Match>();
            Set<Long> offeringIds = new HashSet<Long>();
            for (Student student: group.getStudents())
                for (Lecture lecture: student.getLectures()) {
                    if (lecture.getConfiguration() == null) continue;
                    if (variables != null && !variables.contains(lecture)) continue;
                    offeringIds.add(lecture.getConfiguration().getOfferingId());
                    Match m = match.get(lecture.getSchedulingSubpartId());
                    if (m == null) { m = new Match(group, lecture.getConfiguration()); match.put(lecture.getSchedulingSubpartId(), m); }
                    m.inc(lecture);
                }
            if (match.isEmpty()) continue;
            double value = 0.0;
            for (Match m: match.values())
                value += m.value();
            ret += value / offeringIds.size();
            count ++;
        }
        return 100.0 * ret / count;
    }
    
    /**
     * Student group percentage of a solution
     */
    public static double gp(TimetableModel model) {
        if (model.getStudentGroups().isEmpty()) return 0.0;
        return 100.0 * group(model) / model.getStudentGroups().size();
    }
    
    /**
     * Student group percentage of a solution
     */
    public static double gp(Solution<Lecture, Placement> solution) {
        return gp((TimetableModel)solution.getModel());
    }
    
    /**
     * Combined weight of a solution 
     */
    protected double value(Solution<Lecture, Placement> solution) {
        return objective(solution) + iGroupWeight * (iModel.getStudentGroups().size() - group(iModel));
    }

    @Override
    public void switchStudents(Solution<Lecture, Placement> solution, TerminationCondition<Lecture, Placement> termination) {
        long it = 0, lastImp = 0;
        double t0 = JProf.currentTimeMillis();
        DataProperties cfg = ((TimetableModel)solution.getModel()).getProperties(); 
        long maxIdle = cfg.getPropertyInt("StudentSwaps.MaxIdle", 100000);
        
        getProgress().setStatus("Student Sectioning...");
        getProgress().info("Student Conflicts: " + sDF2.format(objective(solution)) + " (group: " + sDF2.format(gp(solution)) + "%)");
        getProgress().setPhase("Swapping students [HC]...", 1000);
        StudentSwapGenerator g = new StudentSwapGenerator();
        while ((it - lastImp) < maxIdle && (termination == null || termination.canContinue(solution))) {
            it ++;
            if ((it % 1000) == 0) {
                long prg = Math.round(1000.0 * (it - lastImp) / maxIdle);
                if (getProgress().getProgress() < prg)
                    getProgress().setProgress(prg);
                if ((it % 10000) == 0)
                    getProgress().info("Iter=" + (it / 1000)+"k, Idle=" + sDF2.format((it - lastImp)/1000.0)+"k, Speed=" + sDF2.format(1000.0 * it / (JProf.currentTimeMillis() - t0))+" it/s" +
                            ", Value=" + sDF2.format(value(solution)) + ", Objective=" + sDF2.format(objective(solution)) + ", Group=" + sDF2.format(gp(solution)) + "%");
            }
            Neighbour<Lecture, Placement> n = g.selectNeighbour(solution);
            if (n == null) continue;
            double v = value(n, solution.getAssignment());
            if (v < -sEps) { lastImp = it; }
            if (v <= 0) { n.assign(solution.getAssignment(), it); }
        }
        getProgress().info("Student Conflicts: " + sDF2.format(objective(solution)) + " (group: " + sDF2.format(gp(solution)) + "%)");
        
        double f = cfg.getPropertyDouble("StudentSwaps.Deluge.Factor", 0.9999999);
        double ub = cfg.getPropertyDouble("StudentSwaps.Deluge.UpperBound", 1.10);
        double lb = cfg.getPropertyDouble("StudentSwaps.Deluge.LowerBound", 0.90);
        double total = value(solution);
        double bound = ub * total;
        double best = total;
        
        it = 0; lastImp = 0; t0 = JProf.currentTimeMillis();
        getProgress().setPhase("Swapping students [GD]...", 1000);
        while (bound > lb * total && total > 0 && (termination == null || termination.canContinue(solution))) {
            Neighbour<Lecture, Placement> n = g.selectNeighbour(solution);
            if (n != null) {
                double value = value(n, solution.getAssignment());
                if (value < 0) { lastImp = it; }
                if (value <= 0.0 || total + value < bound) {
                    n.assign(solution.getAssignment(), it);
                    if (total + value < best) {
                        best = total + value;
                    }
                    total += value;
                }
            }
            bound *= f;
            it++;
            if ((it % 1000) == 0) {
                long prg = 1000 - Math.round(1000.0 * (bound - lb * best) / (ub * best - lb * best));
                if (getProgress().getProgress() < prg)
                    getProgress().setProgress(prg);
                if ((it % 10000) == 0) {
                    getProgress().info("Iter=" + (it / 1000)+"k, Idle=" + sDF2.format((it - lastImp)/1000.0)+"k, Speed=" + sDF2.format(1000.0 * it / (JProf.currentTimeMillis() - t0))+" it/s" +
                            ", Value=" + sDF2.format(value(solution)) + ", Objective=" + sDF2.format(objective(solution)) + ", Group=" + sDF2.format(gp(solution)) + "%");
                    getProgress().info("Bound is " + sDF2.format(bound) + ", " + "best value is " + sDF2.format(best) + " (" + sDF2.format(100.0 * bound / best) + "%), " +
                            "current value is " + sDF2.format(total) + " (" + sDF2.format(100.0 * bound / total) + "%)");
                }
            }
        }
        getProgress().info("Student Conflicts: " + sDF2.format(objective(solution)) + " (group: " + sDF2.format(gp(solution)) + "%)");
    }

    @Override
    public void resection(Assignment<Lecture, Placement> assignment, Lecture lecture, boolean recursive, boolean configAsWell) {
        if (lecture.students().isEmpty()) return;
        StudentSwapGenerator g = new StudentSwapGenerator();
        long nrIdle = 0, it = 0;
        while (nrIdle < iMaxIdleResection) {
            nrIdle ++; it ++;
            Neighbour<Lecture, Placement> n = g.selectNeighbour(assignment, lecture);
            if (n == null) continue;
            double v = value(n, assignment);
            if (v < -sEps) nrIdle = 0;
            if (v <= 0.0) n.assign(assignment, it);
        }
    }
    
    /**
     * Matching students within a scheduling subpart (for student group weight computation)
     */
    private static class Match {
        private int iTotal = 0;
        private double iFraction = 1.0;
        private Map<Long, Integer> iMatch = new HashMap<Long, Integer>();
        
        /**
         * Constructor
         * @param group student group
         * @param offeringId offering id
         */
        Match(StudentGroup group, Configuration config) {
            iTotal = group.countStudents(config.getOfferingId());
            iFraction = 1.0 / config.countSubparts();
        }
        
        /**
         * Increment given lecture
         */
        void inc(Lecture lecture) {
            Integer val = iMatch.get(lecture.getClassId());
            iMatch.put(lecture.getClassId(), 1 + (val == null ? 0 : val.intValue()));
        }
        
        /**
         * Value (an overall probability of two students being in the same lecture) 
         */
        double value() {
            if (iTotal <= 1) return iFraction;
            double value = 0.0;
            for (Integer m: iMatch.values())
                if (m > 1)
                    value += (m * (m - 1.0)) / (iTotal * (iTotal - 1.0));
            return iFraction * value;
        }
        
        @Override
        public String toString() {
            return iTotal + "/" + iMatch;
        }
    }
    
    protected boolean hasStudentGroups(Collection<Student> students) {
        for (Student student: students)
            if (!student.getGroups().isEmpty()) return true;
        return false;
    }
    
    @Override
    protected Group[] studentsToConfigurations(Long offeringId, Collection<Student> students, Collection<Configuration> configurations) {
        if (hasStudentGroups(students)) {
            GroupBasedInitialSectioning sect = new GroupBasedInitialSectioning(getProgress(), offeringId, configurations, students);
            return sect.getGroups();
        } else {
            return super.studentsToConfigurations(offeringId, students, configurations);
        }
    }
    
    @Override
    protected Group[] studentsToLectures(Long offeringId, Collection<Student> students, Collection<Lecture> lectures) {
        if (hasStudentGroups(students)) {
            Set<Lecture> sortedLectures = new TreeSet<Lecture>(new Comparator<Lecture>() {
                @Override
                public int compare(Lecture l1, Lecture l2) {
                    return l1.getClassId().compareTo(l2.getClassId());
                }
            });
            sortedLectures.addAll(lectures);
            GroupBasedInitialSectioning sect = new GroupBasedInitialSectioning(getProgress(), offeringId, sortedLectures, students);
            return sect.getGroups();
        } else {
            return super.studentsToLectures(offeringId, students, lectures);
        }
    }
}
