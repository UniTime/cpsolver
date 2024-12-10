package org.cpsolver.studentsct.weights;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;


/**
 * Original weighting that was used before this student weightings model was introduced
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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

public class OriginalStudentWeights implements StudentWeights {
    private double iPriorityWeight = 0.90;
    private double iAlterativeWeight = 1.0;
    private double iInitialWeight = 1.2;
    private double iSelectedWeight = 1.1;
    private double iWaitlistedWeight = 1.01;
    private double iDistConfWeight = 0.95;
    protected double[] iQalityWeights;
    /**
     * Enrollment value: value * sAltValue ^ index, where index is zero for the
     * first course, one for the second course etc.
     */
    private double iAltValue = 0.5;
    private double iDummyStudentWeight = 0.5;
    private double iNormPenalty = 5.0;
    
    public OriginalStudentWeights(DataProperties config) {
        iDummyStudentWeight = config.getPropertyDouble("Student.DummyStudentWeight", iDummyStudentWeight);
        iQalityWeights = new double[StudentQuality.Type.values().length];
        for (StudentQuality.Type type: StudentQuality.Type.values()) {
            iQalityWeights[type.ordinal()] = config.getPropertyDouble(type.getWeightName(), type.getWeightDefault());
        }
    }

    /**
     * Normalized enrollment penalty -- to be used in
     * {@link Enrollment#toDouble(Assignment)}
     * @param penalty given penalty
     * @return normalized penalty
     */
    public double normalizePenalty(double penalty) {
        return iNormPenalty / (iNormPenalty + penalty);
    }


    public double getWeight(Request request) {
        return   Math.pow(iPriorityWeight, request.getPriority())
               * (request.isAlternative() ? iAlterativeWeight : 1.0)
               * (request.getStudent().isDummy() ? iDummyStudentWeight : 1.0);
    }

    @Override
    public double getBound(Request request) {
        double w = getWeight(request) * Math.pow(iInitialWeight, (request.getInitialAssignment() == null ? 0 : 1));
        if (request instanceof CourseRequest) {
            CourseRequest cr = (CourseRequest)request;
            w *= Math.pow(iSelectedWeight, cr.getSelectedChoices().isEmpty() ? 0 : 1);
            w *= Math.pow(iWaitlistedWeight, cr.getWaitlistedChoices().isEmpty() ? 0 : 1);
            w *= normalizePenalty(cr.getMinPenalty());
        }
        return w;
    }
    
    @Override
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        return  getWeight(enrollment.getRequest())
                * Math.pow(iAltValue, enrollment.getPriority())
                * Math.pow(iInitialWeight, enrollment.percentInitial())
                * Math.pow(iSelectedWeight, enrollment.percentSelected())
                * Math.pow(iWaitlistedWeight, enrollment.percentWaitlisted())
                * normalizePenalty(enrollment.getPenalty());
    }
    
    @Override
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<DistanceConflict.Conflict> distanceConflicts, Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts) {
        int share = 0;
        if (timeOverlappingConflicts != null) 
            for (TimeOverlapsCounter.Conflict c: timeOverlappingConflicts)
                share += c.getShare();
        return getWeight(assignment, enrollment)
               * (distanceConflicts == null || distanceConflicts.isEmpty() ? 1.0 : Math.pow(iDistConfWeight, distanceConflicts.size()))
               * Math.max(share == 0 ? 1.0 : 1.0 - (((double)share) / enrollment.getNrSlots()) / 2.0, 0.5);
    }
    
    @Override
    public double getDistanceConflictWeight(Assignment<Request, Enrollment> assignment, DistanceConflict.Conflict c) {
        if (c.getR1().getPriority() < c.getR2().getPriority()) {
            return (1.0 - iDistConfWeight) * getWeight(assignment, c.getE1());
        } else {
            return (1.0 - iDistConfWeight) * getWeight(assignment, c.getE2());
        }
    }
    
    @Override
    public double getTimeOverlapConflictWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, TimeOverlapsCounter.Conflict timeOverlap) {
        return Math.min(0.5 * timeOverlap.getShare() / enrollment.getNrSlots(), 0.5) * getWeight(assignment, enrollment);
    }
    
    @Override
    public boolean isBetterThanBestSolution(Solution<Request, Enrollment> currentSolution) {
        if (currentSolution.getBestInfo() == null) return true;
        int unassigned = currentSolution.getModel().nrUnassignedVariables(currentSolution.getAssignment());
        if (currentSolution.getModel().getBestUnassignedVariables() != unassigned)
            return currentSolution.getModel().getBestUnassignedVariables() > unassigned;
        return currentSolution.getModel().getTotalValue(currentSolution.getAssignment()) < currentSolution.getBestValue();
    }

    /**
     * Test case -- run to see the weights for a few courses
     * @param args program arguments
     */
    public static void main(String[] args) {
        OriginalStudentWeights pw = new OriginalStudentWeights(new DataProperties());
        DecimalFormat df = new DecimalFormat("0.000");
        Student s = new Student(0l);
        new CourseRequest(1l, 0, false, s, ToolBox.toList(
                new Course(1, "A", "1", new Offering(0, "A")),
                new Course(1, "A", "2", new Offering(0, "A")),
                new Course(1, "A", "3", new Offering(0, "A"))), false, null);
        new CourseRequest(2l, 1, false, s, ToolBox.toList(
                new Course(1, "B", "1", new Offering(0, "B")),
                new Course(1, "B", "2", new Offering(0, "B")),
                new Course(1, "B", "3", new Offering(0, "B"))), false, null);
        new CourseRequest(3l, 2, false, s, ToolBox.toList(
                new Course(1, "C", "1", new Offering(0, "C")),
                new Course(1, "C", "2", new Offering(0, "C")),
                new Course(1, "C", "3", new Offering(0, "C"))), false, null);
        new CourseRequest(4l, 3, false, s, ToolBox.toList(
                new Course(1, "D", "1", new Offering(0, "D")),
                new Course(1, "D", "2", new Offering(0, "D")),
                new Course(1, "D", "3", new Offering(0, "D"))), false, null);
        new CourseRequest(5l, 4, false, s, ToolBox.toList(
                new Course(1, "E", "1", new Offering(0, "E")),
                new Course(1, "E", "2", new Offering(0, "E")),
                new Course(1, "E", "3", new Offering(0, "E"))), false, null);
        new CourseRequest(6l, 5, true, s, ToolBox.toList(
                new Course(1, "F", "1", new Offering(0, "F")),
                new Course(1, "F", "2", new Offering(0, "F")),
                new Course(1, "F", "3", new Offering(0, "F"))), false, null);
        new CourseRequest(7l, 6, true, s, ToolBox.toList(
                new Course(1, "G", "1", new Offering(0, "G")),
                new Course(1, "G", "2", new Offering(0, "G")),
                new Course(1, "G", "3", new Offering(0, "G"))), false, null);
        
        Assignment<Request, Enrollment> assignment = new DefaultSingleAssignment<Request, Enrollment>();
        Placement p = new Placement(null, new TimeLocation(1, 90, 12, 0, 0, null, null, new BitSet(), 10), new ArrayList<RoomLocation>());
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With one distance conflict:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<DistanceConflict.Conflict> dc = new HashSet<DistanceConflict.Conflict>();
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e, (Section)sections.iterator().next()));
                w[i] = pw.getWeight(assignment, e, dc, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With two distance conflicts:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<DistanceConflict.Conflict> dc = new HashSet<DistanceConflict.Conflict>();
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e, (Section)sections.iterator().next()));
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e, 
                        new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null)));
                w[i] = pw.getWeight(assignment, e, dc, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With 25% time overlapping conflicts:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<TimeOverlapsCounter.Conflict> toc = new HashSet<TimeOverlapsCounter.Conflict>();
                toc.add(new TimeOverlapsCounter.Conflict(s, 3, e, sections.iterator().next(), e, sections.iterator().next()));
                w[i] = pw.getWeight(assignment, e, null, toc);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
    }

    @Override
    public boolean isFreeTimeAllowOverlaps() {
        return true;
    }

    @Override
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<StudentQuality.Conflict> qualityConflicts) {
        double base = getWeight(assignment, enrollment);
        double penalty = 0.0;
        if (qualityConflicts != null) {
            for (StudentQuality.Conflict c: qualityConflicts) {
                Enrollment other = c.getOther(enrollment);
                switch (c.getType().getType()) {
                    case REQUEST:
                        if (enrollment.isCourseRequest())
                            penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                        else if (other.isCourseRequest())
                            penalty += getWeight(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                        break;
                    case BOTH:
                        penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                        if (other.getRequest() != null)
                            penalty += getWeight(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                        break;
                    case LOWER:
                        other = c.getOther(enrollment);
                        if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                            penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                        else if (other.getRequest() != null)
                            penalty += getWeight(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                        break;
                    case HIGHER:
                        other = c.getOther(enrollment);
                        if (other.getRequest().getPriority() >= enrollment.getRequest().getPriority())
                            penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                        else if (other.getRequest() != null)
                            penalty += getWeight(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                }
            }
        }
        return base - penalty;
    }

    @Override
    public double getStudentQualityConflictWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, StudentQuality.Conflict conflict) {
        switch (conflict.getType().getType()) {
            case BOTH:
                if (enrollment == null || enrollment.getRequest() == null) return 0.0;
                return getWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(enrollment);
            case REQUEST:
                if (enrollment == null || enrollment.getRequest() == null || !enrollment.isCourseRequest()) return 0.0;
                return getWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(enrollment);
            case LOWER:
                if (conflict.getR1().getPriority() < conflict.getR2().getPriority()) {
                    return getWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE2());
                } else {
                    return getWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE1());
                }
            case HIGHER:
                if (conflict.getR1().getPriority() > conflict.getR2().getPriority()) {
                    return getWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE2());
                } else {
                    return getWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE1());
                }
            default:
                return 0.0;
        }
    }
}
