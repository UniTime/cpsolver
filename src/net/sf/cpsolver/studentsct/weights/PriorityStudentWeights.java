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
 * New weighting model. It tries to obey the following principles:
 * <ul>
 *      <li> Total student weight is between zero and one (one means student got the best schedule)
 *      <li> Weight of the given priority course is higher than sum of the remaining weights the student can get
 *      <li> First alternative is better than the following course
 *      <li> Second alternative is better than the second following course
 *      <li> Distance conflicts are considered secondary (priorities should be maximized first)
 *      <li> If alternative sections are otherwise equal, use the better balanced one
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

public class PriorityStudentWeights implements StudentWeights {
    protected double iPriorityFactor = 0.5010;
    protected double iFirstAlternativeFactor = 0.5010;
    protected double iSecondAlternativeFactor = 0.2510;
    protected double iDistanceConflict = 0.0100;
    protected double iTimeOverlapFactor = 0.5000;
    protected double iTimeOverlapMaxLimit = 0.5000;
    protected boolean iLeftoverSpread = false;
    protected double iBalancingFactor = 0.0050;
    protected double iAlternativeRequestFactor = 0.1260;
    protected double iProjectedStudentWeight = 0.0100;
    
    public PriorityStudentWeights(DataProperties config) {
        iPriorityFactor = config.getPropertyDouble("StudentWeights.Priority", iPriorityFactor);
        iFirstAlternativeFactor = config.getPropertyDouble("StudentWeights.FirstAlternative", iFirstAlternativeFactor);
        iSecondAlternativeFactor = config.getPropertyDouble("StudentWeights.SecondAlternative", iSecondAlternativeFactor);
        iDistanceConflict = config.getPropertyDouble("StudentWeights.DistanceConflict", iDistanceConflict);
        iTimeOverlapFactor = config.getPropertyDouble("StudentWeights.TimeOverlapFactor", iTimeOverlapFactor);
        iTimeOverlapMaxLimit = config.getPropertyDouble("StudentWeights.TimeOverlapMaxLimit", iTimeOverlapMaxLimit);
        iLeftoverSpread = config.getPropertyBoolean("StudentWeights.LeftoverSpread", iLeftoverSpread);
        iBalancingFactor = config.getPropertyDouble("StudentWeights.BalancingFactor", iBalancingFactor);
        iAlternativeRequestFactor = config.getPropertyDouble("StudentWeights.AlternativeRequestFactor", iAlternativeRequestFactor);
        iProjectedStudentWeight = config.getPropertyDouble("StudentWeights.ProjectedStudentWeight", iProjectedStudentWeight);
    }
        
    public double getWeight(Request request) {
        if (request.getStudent().isDummy() && iProjectedStudentWeight >= 0.0) {
            double weight = iProjectedStudentWeight;
            if (request.isAlternative())
                weight *= iAlternativeRequestFactor;
            return weight;
        }
        double total = 10000.0;
        int nrReq = request.getStudent().nrRequests();
        double remain = (iLeftoverSpread ? Math.floor(10000.0 * Math.pow(iPriorityFactor, nrReq) / nrReq) : 0.0);
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
                return w / 10000.0;
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
        return getCachedWeight(request);
    }
    
    protected double round(double value) {
        return Math.ceil(10000.0 * value) / 10000.0;
    }
    
    @Override
    public double getWeight(Enrollment enrollment) {
        double weight = getCachedWeight(enrollment.getRequest());
        switch (enrollment.getPriority()) {
            case 1: weight *= iFirstAlternativeFactor; break;
            case 2: weight *= iSecondAlternativeFactor; break;
        }
        if (enrollment.isCourseRequest() && iBalancingFactor != 0.0) {
            double configUsed = enrollment.getConfig().getEnrollmentWeight(enrollment.getRequest()) + enrollment.getRequest().getWeight();
            double disbalanced = 0;
            double total = 0;
            for (Section section: enrollment.getSections()) {
                Subpart subpart = section.getSubpart();
                if (subpart.getSections().size() <= 1) continue;
                double used = section.getEnrollmentWeight(enrollment.getRequest()) + enrollment.getRequest().getWeight();
                // sections have limits -> desired size is section limit x (total enrollment / total limit)
                // unlimited sections -> desired size is total enrollment / number of sections
                double desired = (subpart.getLimit() > 0
                        ? section.getLimit() * (configUsed / subpart.getLimit())
                        : configUsed / subpart.getSections().size());
                if (used > desired)
                    disbalanced += Math.min(enrollment.getRequest().getWeight(), used - desired) / enrollment.getRequest().getWeight();
                else
                    disbalanced -= Math.min(enrollment.getRequest().getWeight(), desired - used) / enrollment.getRequest().getWeight();
                total ++;
            }
            if (disbalanced > 0)
                weight *= (1.0 - disbalanced / total * iBalancingFactor);
        }
        return round(weight);
    }
    
    @Override
    public double getDistanceConflictWeight(DistanceConflict.Conflict c) {
        if (c.getR1().getPriority() < c.getR2().getPriority()) {
            return round(getWeight(c.getE2()) * iDistanceConflict);
        } else {
            return round(getWeight(c.getE1()) * iDistanceConflict);
        }
    }
    
    @Override
    public double getTimeOverlapConflictWeight(Enrollment e, TimeOverlapsCounter.Conflict c) {
        double toc = Math.min(iTimeOverlapMaxLimit * c.getShare() / e.getNrSlots(), iTimeOverlapMaxLimit);
        return round(getWeight(e) * toc);
    }
    
    public double getWeight(Enrollment enrollment, Set<DistanceConflict.Conflict> distanceConflicts, Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts) {
        double base = getWeight(enrollment);
        double dc = 0.0;
        if (distanceConflicts != null) {
            for (DistanceConflict.Conflict c: distanceConflicts) {
                Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                    dc += base * iDistanceConflict;
                else
                    dc += getWeight(other) * iDistanceConflict;
            }
        }
        double toc = 0.0;
        if (timeOverlappingConflicts != null) {
            for (TimeOverlapsCounter.Conflict c: timeOverlappingConflicts) {
                toc += base * Math.min(iTimeOverlapFactor * c.getShare() / enrollment.getNrSlots(), iTimeOverlapMaxLimit);
                Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                toc += getWeight(other) * Math.min(iTimeOverlapFactor * c.getShare() / other.getNrSlots(), iTimeOverlapMaxLimit);
            }
        }
        return round(base - dc - toc);
    }
    
    
    @Override
    public boolean isBetterThanBestSolution(Solution<Request, Enrollment> currentSolution) {
        return currentSolution.getBestInfo() == null || currentSolution.getModel().getTotalValue() < currentSolution.getBestValue();
    }
    
    /**
     * Test case -- run to see the weights for a few courses
     */
    public static void main(String[] args) {
        PriorityStudentWeights pw = new PriorityStudentWeights(new DataProperties());
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
