package net.sf.cpsolver.studentsct.weights;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * Student weight is spread equally among student's course requests. Only alternatives have lower weight.
 * The rest is inherited from {@link PriorityStudentWeights}.
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
    protected double iAlternativeRequestFactor = 0.1260;
    
    public EqualStudentWeights(DataProperties config) {
        super(config);
        iAlternativeRequestFactor = config.getPropertyDouble("StudentWeights.AlternativeRequestFactor", iAlternativeRequestFactor);
    }

    @Override
    public double getWeight(Request request) {
        double weight = 1.0 / request.getStudent().nrRequests();
        if (request.isAlternative())
            weight *= iAlternativeRequestFactor;
        return round(weight);
    }
    
    @Override
    public boolean isBetterThanBestSolution(Solution<Request, Enrollment> currentSolution) {
        if (currentSolution.getBestInfo() == null) return true;
        int unassigned = currentSolution.getModel().nrUnassignedVariables();
        if (currentSolution.getModel().getBestUnassignedVariables() != unassigned)
            return currentSolution.getModel().getBestUnassignedVariables() > unassigned;
        return currentSolution.getModel().getTotalValue() < currentSolution.getBestValue();
    }
    
    /**
     * Test case -- run to see the weights for a few courses
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
        
        Placement p = new Placement(null, new TimeLocation(1, 90, 12, 0, 0, null, null, new BitSet(), 10), new ArrayList<RoomLocation>());
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                w[i] = pw.getWeight(e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With one distance conflict:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                Set<DistanceConflict.Conflict> dc = new HashSet<DistanceConflict.Conflict>();
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e, (Section)sections.iterator().next()));
                w[i] = pw.getWeight(e, dc, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With two distance conflicts:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                Set<DistanceConflict.Conflict> dc = new HashSet<DistanceConflict.Conflict>();
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e, (Section)sections.iterator().next()));
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e,
                        new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null)));
                w[i] = pw.getWeight(e, dc, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With 25% time overlapping conflict:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                Set<TimeOverlapsCounter.Conflict> toc = new HashSet<TimeOverlapsCounter.Conflict>();
                toc.add(new TimeOverlapsCounter.Conflict(s, 3, e, sections.iterator().next(), e, sections.iterator().next()));
                w[i] = pw.getWeight(e, null, toc);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("Disbalanced sections (by 2 / 10 students):");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                Subpart x = new Subpart(0, "Lec", "Lec", cfg, null);
                Section a = new Section(0, 10, "x", x, p, null, null, null);
                new Section(1, 10, "y", x, p, null, null, null);
                sections.add(a);
                a.assigned(new Enrollment(s.getRequests().get(0), i, cfg, sections));
                a.assigned(new Enrollment(s.getRequests().get(0), i, cfg, sections));
                cfg.assigned(new Enrollment(s.getRequests().get(0), i, cfg, sections));
                cfg.assigned(new Enrollment(s.getRequests().get(0), i, cfg, sections));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                w[i] = pw.getWeight(e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
    }
}
