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
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.model.Choice;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;


/**
 * Student weight is spread equally among student's course requests. Only alternatives have lower weight.
 * The rest is inherited from {@link PriorityStudentWeights}.
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

public class EqualStudentWeights extends PriorityStudentWeights {

    public EqualStudentWeights(DataProperties config) {
        super(config);
    }

    @Override
    public double getWeight(Request request) {
        if (request.getStudent().isDummy() && iProjectedStudentWeight >= 0.0) {
            double weight = iProjectedStudentWeight;
            if (request.isAlternative())
                weight *= iAlternativeRequestFactor;
            return weight;
        }
        double weight = 1.0 / request.getStudent().nrRequests();
        if (request.isAlternative())
            weight *= iAlternativeRequestFactor;
        return round(weight);
    }
    
    @Override
    public boolean isBetterThanBestSolution(Solution<Request, Enrollment> currentSolution) {
        if (currentSolution.getBestInfo() == null) return true;
        if (iMPP) return super.isBetterThanBestSolution(currentSolution);
        int unassigned = currentSolution.getModel().nrUnassignedVariables(currentSolution.getAssignment());
        if (currentSolution.getModel().getBestUnassignedVariables() != unassigned)
            return currentSolution.getModel().getBestUnassignedVariables() > unassigned;
        return ((StudentSectioningModel)currentSolution.getModel()).getTotalValue(currentSolution.getAssignment(), iPreciseComparison) < currentSolution.getBestValue();
    }
    
    @Override
    public boolean isFreeTimeAllowOverlaps() {
        return true;
    }
    
    /**
     * Test case -- run to see the weights for a few courses
     * @param args program arguments
     */
    public static void main(String[] args) {
        EqualStudentWeights pw = new EqualStudentWeights(new DataProperties());
        DecimalFormat df = new DecimalFormat("0.0000");
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

        System.out.println("With 25% time overlapping conflict:");
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

        System.out.println("Disbalanced sections (by 2 / 10 students):");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                Subpart x = new Subpart(0, "Lec", "Lec", cfg, null);
                Section a = new Section(0, 10, "x", x, p, null);
                new Section(1, 10, "y", x, p, null);
                sections.add(a);
                a.assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                a.assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                cfg.getContext(assignment).assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                cfg.getContext(assignment).assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
        
        System.out.println("Same sections:");
        pw.iMPP = true;
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            double dif = 0;
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, sections, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
                dif = pw.getDifference(e);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]) + " (" + df.format(dif) + ")");
        }
        
        System.out.println("Same choice sections:");
        pw.iMPP = true;
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            double dif = 0;
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
                dif = pw.getDifference(e);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]) + " (" + df.format(dif) + ")");
        }
        
        System.out.println("Same time sections:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double dif = 0;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, new Instructor(1, null, "Josef Novak", null)));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
                dif = pw.getDifference(e);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]) + " (" + df.format(dif) + ")");
        }
        
        System.out.println("Same configuration sections:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            double dif = 0;
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                cr.getSelectedChoices().add(new Choice(cfg));
                cr.setInitialAssignment(null);
                w[i] = pw.getWeight(assignment, e, null, null);
                dif = pw.getDifference(e);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]) + " (" + df.format(dif) + ")");
        }
        
        System.out.println("Different time sections:");
        Placement q = new Placement(null, new TimeLocation(1, 102, 12, 0, 0, null, null, new BitSet(), 10), new ArrayList<RoomLocation>());
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            double dif = 0;
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), q, null));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
                dif = pw.getDifference(e);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]) + " (" + df.format(dif) + ")");
        }
        
        System.out.println("Two sections, one same choice, one same time:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            double dif = 0;
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                sections.add(new Section(1, 1, "y", new Subpart(1, "Rec", "Rec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(2, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                other.add(new Section(3, 1, "y", new Subpart(1, "Rec", "Rec", cfg, null), p, null, new Instructor(1, null, "Josef Novak", null)));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
                dif = pw.getDifference(e);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]) + " (" + df.format(dif) + ")");
        }
    }
}
