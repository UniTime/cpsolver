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
import net.sf.cpsolver.ifs.solution.SolutionComparator;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
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
 * New weighting model. It tries to obey the following principles:
 * <ul>
 *      <li> Total student weight is between zero and one (one means student got the best schedule)
 *      <li> Weight of the given priority course is higher than sum of the remaining weights the student can get
 *      <li> First alternative is better than the following course
 *      <li> Second alternative is better than the second following course
 *      <li> Distance conflicts are considered secondary (priorities should be maximized first)
 * </ul>
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

public class PriorityStudentWeights implements StudentWeights, SolutionComparator<Request, Enrollment> {
    private static double iPriorityFactor = 0.501;
    private static double iFirstAlternativeFactor = 0.501;
    private static double iSecondAlternativeFactor = 0.251;
    private static double iDistanceConflict = 0.990;
    
    public PriorityStudentWeights(DataProperties config) {
        iPriorityFactor = config.getPropertyDouble("StudentWeights.Priority", iPriorityFactor);
        iFirstAlternativeFactor = config.getPropertyDouble("StudentWeights.FirstAlternative", iFirstAlternativeFactor);
        iSecondAlternativeFactor = config.getPropertyDouble("StudentWeights.SecondAlternative", iSecondAlternativeFactor);
        iDistanceConflict = config.getPropertyDouble("StudentWeights.DistanceConflict", iDistanceConflict);
    }
    
    public double getWeight(Request request) {
        double total = 1000.0;
        int nrReq = request.getStudent().nrRequests();
        double remain = Math.floor(1000.0 * Math.pow(iPriorityFactor, nrReq) / nrReq);
        for (int idx = 0; idx < request.getStudent().getRequests().size(); idx++) {
            Request r = request.getStudent().getRequests().get(idx);
            boolean last = (idx + 1 == request.getStudent().getRequests().size());
            boolean lastNotAlt = !r.isAlternative() && (last || request.getStudent().getRequests().get(1 + idx).isAlternative());
            double w = Math.ceil(iPriorityFactor * total) + remain;
            if (lastNotAlt || last) {
                w = total;
            } else {
                total -= w;
            }
            if (r.equals(request)) {
                w *= Math.floor(request.getWeight());
                return w / 1000.0;
            }
        }
        return 0.0;
    }
    
    public double getCachedWeight(Request request) {
        Double w = (Double)request.getExtra();
        if (w == null) {
            w = getWeight(request);
            request.setExtra(w);
        }
        return w;
    }

    @Override
    public double getBound(Request request) {
        return getWeight(request);
    }
    
    public double getWeight(Enrollment enrollment, double requestWeight, int nrDistanceConflicts, int timeOverlappingConflicts) {
        double w = Math.ceil(1000.0 * requestWeight);
        switch (enrollment.getPriority()) {
            case 1: w = Math.ceil(w * iFirstAlternativeFactor); break;
            case 2: w = Math.ceil(w * iSecondAlternativeFactor); break;
        }
        if (nrDistanceConflicts > 0)
            w = Math.floor(w * Math.pow(iDistanceConflict, nrDistanceConflicts));
        if (timeOverlappingConflicts > 0) {
            double share = ((double)timeOverlappingConflicts) / enrollment.getNrSlots();
            w *= Math.max(1.0 - share / 2.0, 0.5);
        }
        return w / 1000.0;
    }
    
    @Override
    public double getWeight(Enrollment enrollment, int nrDistanceConflicts, int timeOverlappingConflicts) {
        return getWeight(enrollment, getCachedWeight(enrollment.getRequest()), nrDistanceConflicts, timeOverlappingConflicts);
    }
    
    @Override
    public boolean isBetterThanBestSolution(Solution<Request, Enrollment> currentSolution) {
        return currentSolution.getModel().getTotalValue() < currentSolution.getBestValue();
    }
    
    /**
     * Test case -- run to see the weights for a few courses
     */
    public static void main(String[] args) {
        PriorityStudentWeights pw = new PriorityStudentWeights(new DataProperties());
        DecimalFormat df = new DecimalFormat("0.000");
        Student s = new Student(0l);
        new CourseRequest(1l, 0, false, s, ToolBox.toList(
                new Course(1, "A", "1", new Offering(0, "A")),
                new Course(1, "A", "2", new Offering(0, "A")),
                new Course(1, "A", "3", new Offering(0, "A"))), false);
        new CourseRequest(2l, 1, false, s, ToolBox.toList(
                new Course(1, "B", "1", new Offering(0, "B")),
                new Course(1, "B", "2", new Offering(0, "B")),
                new Course(1, "B", "3", new Offering(0, "B"))), false);
        new CourseRequest(3l, 2, false, s, ToolBox.toList(
                new Course(1, "C", "1", new Offering(0, "C")),
                new Course(1, "C", "2", new Offering(0, "C")),
                new Course(1, "C", "3", new Offering(0, "C"))), false);
        new CourseRequest(4l, 3, false, s, ToolBox.toList(
                new Course(1, "D", "1", new Offering(0, "D")),
                new Course(1, "D", "2", new Offering(0, "D")),
                new Course(1, "D", "3", new Offering(0, "D"))), false);
        new CourseRequest(5l, 4, false, s, ToolBox.toList(
                new Course(1, "E", "1", new Offering(0, "E")),
                new Course(1, "E", "2", new Offering(0, "E")),
                new Course(1, "E", "3", new Offering(0, "E"))), false);
        new CourseRequest(6l, 5, true, s, ToolBox.toList(
                new Course(1, "F", "1", new Offering(0, "F")),
                new Course(1, "F", "2", new Offering(0, "F")),
                new Course(1, "F", "3", new Offering(0, "F"))), false);
        new CourseRequest(7l, 6, true, s, ToolBox.toList(
                new Course(1, "G", "1", new Offering(0, "G")),
                new Course(1, "G", "2", new Offering(0, "G")),
                new Course(1, "G", "3", new Offering(0, "G"))), false);
        
        Placement p = new Placement(null, new TimeLocation(1, 90, 12, 0, 0, null, null, new BitSet(), 10), new ArrayList<RoomLocation>());
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                w[i] = pw.getWeight(e, 0, 0);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With one distance conflict:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                w[i] = pw.getWeight(e, 1, 0);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With two distance conflicts:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                w[i] = pw.getWeight(e, 2, 0);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With 25% time overlapping conflict:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, "", cr.getCourses().get(i).getOffering());
                Set<Assignment> sections = new HashSet<Assignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, null, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections);
                w[i] = pw.getWeight(e, 0, 3);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
    }
}
