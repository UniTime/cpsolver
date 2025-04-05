package org.cpsolver.studentsct;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.EmptyAssignment;
import org.cpsolver.ifs.heuristics.RouletteWheelSelection;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;


/**
 * An attempt to empirically test the case when students can choose their
 * sections (section times). <br>
 * <br>
 * Each student has his/her own order of possible times of the week (selection
 * of a day and an hour starting 7:30, 8:30, etc.) -- this order is computed
 * using roulette wheel selection with the distribution of possible times
 * defined in {@link StudentPreferencePenalties#sStudentRequestDistribution}. <br>
 * <br>
 * A penalty for each section is computed proportionally based on this order
 * (and the number of slots that falls into each time frame), the existing
 * branch &amp; bound selection is used to section each student one by one (in a
 * random order). <br>
 * <br>
 * Usage:
 * <pre><code>
 * for (Enumeration e=students.elements();e.hasMoreElements();) {
 * &nbsp;&nbsp;// take a student (one by one)
 * &nbsp;&nbsp;Student student = (Student)e.nextElement();
 * 
 * &nbsp;&nbsp;// compute and apply penalties using this class
 * &nbsp;&nbsp;new StudentPreferencePenalties().setPenalties(student);
 * 
 * &nbsp;&nbsp;// section a student
 * &nbsp;&nbsp;// for instance, {@link BranchBoundSelection} can be used (with Neighbour.BranchAndBoundMinimizePenalty set to true)
 * &nbsp;&nbsp;Neighbour neighbour = new BranchBoundSelection(config).getSelection(student).select();
 * &nbsp;&nbsp;if (neighbour!=null) neighbour.assign(iteration++);
 * };
 * </code></pre>
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
public class StudentPreferencePenalties {
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(StudentPreferencePenalties.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private static boolean sDebug = false;
    public static int sDistTypeUniform = 0;
    public static int sDistTypePreference = 1;
    public static int sDistTypePreferenceQuadratic = 2;
    public static int sDistTypePreferenceReverse = 3;
    public static int sDistTypePlain = 4;

    public static int[][] sStudentRequestDistribution = new int[][] {
    // morning, 7:30a, 8:30a, 9:30a, 10:30a, 11:30a, 12:30p, 1:30p, 2:30p,
    // 3:30p, 4:30p, evening
            { 1, 1, 4, 7, 10, 10, 5, 8, 8, 6, 3, 1 }, // Monday
            { 1, 2, 4, 7, 10, 10, 5, 8, 8, 6, 3, 1 }, // Tuesday
            { 1, 2, 4, 7, 10, 10, 5, 8, 8, 6, 3, 1 }, // Wednesday
            { 1, 2, 4, 7, 10, 10, 5, 8, 8, 6, 3, 1 }, // Thursday
            { 1, 2, 4, 7, 10, 10, 5, 4, 3, 2, 1, 1 }, // Friday
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, // Saturday
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 } // Sunday
    };
    private HashMap<String, Double> iWeight = new HashMap<String, Double>();

    /**
     * Constructor. All possible times are ordered based on the distribution
     * defined by {@link StudentPreferencePenalties#sStudentRequestDistribution}
     * . The first time gets zero penalty, the second 1/nrTimes, the third
     * 2/nrTimes etc. where nrTimes is the number of times in
     * {@link StudentPreferencePenalties#sStudentRequestDistribution}.
     * @param disributionType distribution type
     */
    public StudentPreferencePenalties(int disributionType) {
        if (disributionType == sDistTypePlain) {
            int idx = 0;
            for (int d = 0; d < sStudentRequestDistribution.length; d++)
                for (int t = 0; t < sStudentRequestDistribution[d].length; t++) {
                    int w = sStudentRequestDistribution[d][t];
                    double p = (10 - w) / 9.0;
                    iWeight.put(d + "." + t, p);
                    idx ++;
                    if (sDebug)
                        sLog.debug("  -- " + (idx + 1) + ". preference is " + toString(d, t) + " (P:" + sDF.format(p) + ")");
                }
            return;
        }
        RouletteWheelSelection<int[]> roulette = new RouletteWheelSelection<int[]>();
        for (int d = 0; d < sStudentRequestDistribution.length; d++)
            for (int t = 0; t < sStudentRequestDistribution[d].length; t++) {
                if (disributionType == sDistTypeUniform) {
                    roulette.add(new int[] { d, t }, 1);
                } else if (disributionType == sDistTypePreference) {
                    roulette.add(new int[] { d, t }, sStudentRequestDistribution[d][t]);
                } else if (disributionType == sDistTypePreferenceQuadratic) {
                    roulette.add(new int[] { d, t }, sStudentRequestDistribution[d][t]
                            * sStudentRequestDistribution[d][t]);
                } else if (disributionType == sDistTypePreferenceReverse) {
                    roulette.add(new int[] { d, t }, 11 - sStudentRequestDistribution[d][t]);
                } else {
                    roulette.add(new int[] { d, t }, 1);
                }
            }
        int idx = 0;
        while (roulette.hasMoreElements()) {
            int[] dt = roulette.nextElement();
            iWeight.put(dt[0] + "." + dt[1], Double.valueOf(((double) idx) / (roulette.size() - 1)));
            if (sDebug)
                sLog.debug("  -- " + (idx + 1) + ". preference is " + toString(dt[0], dt[1]) + " (P:"
                        + sDF.format(((double) idx) / (roulette.size() - 1)) + ")");
            idx++;
        }
    }

    /**
     * Return day index in
     * {@link StudentPreferencePenalties#sStudentRequestDistribution} for the
     * given slot.
     * @param slot time slot
     * @return day index
     */
    public static int day(int slot) {
        return slot / Constants.SLOTS_PER_DAY;
    }

    /**
     * Return time index in
     * {@link StudentPreferencePenalties#sStudentRequestDistribution} for the
     * given slot.
     * @param slot time slot
     * @return time index
     */
    public static int time(int slot) {
        int s = slot % Constants.SLOTS_PER_DAY;
        int min = (s * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN);
        if (min < 450)
            return 0; // morning
        int idx = 1 + (min - 450) / 60;
        return (idx > 11 ? 11 : idx); // 11+ is evening
    }

    /**
     * Return time of the given day and time index of
     * {@link StudentPreferencePenalties#sStudentRequestDistribution}.
     * @param day day index
     * @param time time index
     * @return day and time as string
     */
    public String toString(int day, int time) {
        if (time == 0)
            return Constants.DAY_NAMES_SHORT[day] + " morning";
        if (time == 11)
            return Constants.DAY_NAMES_SHORT[day] + " evening";
        return Constants.DAY_NAMES_SHORT[day] + " " + (6 + time) + ":30";
    }

    /**
     * Return penalty of the given time. It is computed as average of the penalty
     * for each time slot of the time.
     * @param time time location
     * @return penalty
     **/
    public double getPenalty(TimeLocation time) {
        int nrSlots = 0;
        double penalty = 0.0;
        for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            nrSlots++;
            penalty += (iWeight.get(day(slot) + "." + time(slot))).doubleValue();
        }
        return penalty / nrSlots;
    }

    /**
     * Return penalty of an assignment. It is a penalty of its time (see
     * {@link SctAssignment#getTime()}) or zero if the time is null.
     * @param assignment section assignment
     * @return penalty
     */
    public double getPenalty(SctAssignment assignment) {
        return (assignment.getTime() == null ? 0.0 : getPenalty(assignment.getTime()));
    }

    /**
     * Return penalty of an enrollment. It is an average penalty of all its
     * assignments {@link Enrollment#getAssignments()}.
     * @param enrollment enrollment
     * @return penalty
     */
    public double getPenalty(Enrollment enrollment) {
        double penalty = 0;
        for (Section section : enrollment.getSections()) {
            penalty += getPenalty(section);
        }
        return penalty / enrollment.getAssignments().size();
    }

    /** Minimal penalty of a course request 
     * @param request student request
     * @return minimal penalty
     **/
    public double getMinPenalty(Request request) {
        if (request instanceof CourseRequest)
            return getMinPenalty((CourseRequest) request);
        else if (request instanceof FreeTimeRequest)
            return getPenalty(((FreeTimeRequest) request).getTime());
        return 0;
    }

    /** Minimal penalty of a course request 
     * @param request course request
     * @return minimal penalty
     **/
    public double getMinPenalty(CourseRequest request) {
        double min = Double.MAX_VALUE;
        for (Course course : request.getCourses()) {
            min = Math.min(min, getMinPenalty(course.getOffering()));
        }
        return (min == Double.MAX_VALUE ? 0.0 : min);
    }

    /** Minimal penalty of an offering 
     * @param offering instructional offering
     * @return minimal penalty
     **/
    public double getMinPenalty(Offering offering) {
        double min = Double.MAX_VALUE;
        for (Config config : offering.getConfigs()) {
            min = Math.min(min, getMinPenalty(config));
        }
        return (min == Double.MAX_VALUE ? 0.0 : min);
    }

    /** Minimal penalty of a config 
     * @param config instructional offering configuration
     * @return minimal penalty
     **/
    public double getMinPenalty(Config config) {
        double min = 0;
        for (Subpart subpart : config.getSubparts()) {
            min += getMinPenalty(subpart);
        }
        return min / config.getSubparts().size();
    }

    /** Minimal penalty of a subpart 
     * @param subpart scheduling subpart
     * @return minimal penalty
     **/
    public double getMinPenalty(Subpart subpart) {
        double min = Double.MAX_VALUE;
        for (Section section : subpart.getSections()) {
            min = Math.min(min, getPenalty(section));
        }
        return (min == Double.MAX_VALUE ? 0.0 : min);
    }

    /** Maximal penalty of a course request 
     * @param request student request
     * @return maximal penalty
     **/
    public double getMaxPenalty(Request request) {
        if (request instanceof CourseRequest)
            return getMaxPenalty((CourseRequest) request);
        else if (request instanceof FreeTimeRequest)
            return getPenalty(((FreeTimeRequest) request).getTime());
        return 0;
    }

    /** Maximal penalty of a course request 
     * @param request student course request
     * @return maximal penalty
     **/
    public double getMaxPenalty(CourseRequest request) {
        double max = Double.MIN_VALUE;
        for (Course course : request.getCourses()) {
            max = Math.max(max, getMaxPenalty(course.getOffering()));
        }
        return (max == Double.MIN_VALUE ? 0.0 : max);
    }

    /** Maximal penalty of an offering 
     * @param offering instructional offering
     * @return maximal penalty
     **/
    public double getMaxPenalty(Offering offering) {
        double max = Double.MIN_VALUE;
        for (Config config : offering.getConfigs()) {
            max = Math.max(max, getMaxPenalty(config));
        }
        return (max == Double.MIN_VALUE ? 0.0 : max);
    }

    /** Maximal penalty of a config 
     * @param config instructional offering config
     * @return maximal penalty
     **/
    public double getMaxPenalty(Config config) {
        double max = 0;
        for (Subpart subpart : config.getSubparts()) {
            max += getMaxPenalty(subpart);
        }
        return max / config.getSubparts().size();
    }

    /** Maximal penalty of a subpart 
     * @param subpart scheduling subpart
     * @return maximal penalty
     **/
    public double getMaxPenalty(Subpart subpart) {
        double max = Double.MIN_VALUE;
        for (Section section : subpart.getSections()) {
            max = Math.max(max, getPenalty(section));
        }
        return (max == Double.MIN_VALUE ? 0.0 : max);
    }

    /** Minimal and maximal available enrollment penalty of a request 
     * @param assignment current assignment
     * @param request student request
     * @return minimal and maximal available enrollment penalty
     **/
    public double[] getMinMaxAvailableEnrollmentPenalty(Assignment<Request, Enrollment> assignment, Request request) {
        if (request instanceof CourseRequest) {
            return getMinMaxAvailableEnrollmentPenalty(assignment, (CourseRequest) request);
        } else {
            double pen = getPenalty(((FreeTimeRequest) request).getTime());
            return new double[] { pen, pen };
        }
    }

    /** Minimal and maximal available enrollment penalty of a request 
     * @param assignment current assignment
     * @param request student course request
     * @return minimal and maximal available enrollment penalty
     **/
    public double[] getMinMaxAvailableEnrollmentPenalty(Assignment<Request, Enrollment> assignment, CourseRequest request) {
        List<Enrollment> enrollments = request.getAvaiableEnrollments(assignment);
        if (enrollments.isEmpty())
            return new double[] { 0, 0 };
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (Enrollment enrollment : enrollments) {
            double penalty = getPenalty(enrollment);
            min = Math.min(min, penalty);
            max = Math.max(max, penalty);
        }
        return new double[] { min, max };
    }

    /** Minimal and maximal available enrollment penalty of a request 
     * @param request student request
     * @return minimal and maximal penalty
     **/
    public double[] getMinMaxEnrollmentPenalty(Request request) {
        if (request instanceof CourseRequest) {
            return getMinMaxEnrollmentPenalty((CourseRequest) request);
        } else {
            double pen = getPenalty(((FreeTimeRequest) request).getTime());
            return new double[] { pen, pen };
        }
    }

    /** Minimal and maximal available enrollment penalty of a request 
     * @param request student course request
     * @return minimal and maximal penalty
     **/
    public double[] getMinMaxEnrollmentPenalty(CourseRequest request) {
        List<Enrollment> enrollments = request.values(new EmptyAssignment<Request, Enrollment>());
        if (enrollments.isEmpty())
            return new double[] { 0, 0 };
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (Enrollment enrollment : enrollments) {
            double penalty = getPenalty(enrollment);
            min = Math.min(min, penalty);
            max = Math.max(max, penalty);
        }
        return new double[] { min, max };
    }

    /**
     * Set the computed penalties to all sections of all requests of the given
     * student
     * @param student given student
     * @param distributionType penalty distribution type
     */
    public static void setPenalties(Student student, int distributionType) {
        if (sDebug)
            sLog.debug("Setting penalties for " + student);
        StudentPreferencePenalties penalties = new StudentPreferencePenalties(distributionType);
        for (Request request : student.getRequests()) {
            if (!(request instanceof CourseRequest))
                continue;
            CourseRequest courseRequest = (CourseRequest) request;
            if (sDebug)
                sLog.debug("-- " + courseRequest);
            for (Course course : courseRequest.getCourses()) {
                if (sDebug)
                    sLog.debug("  -- " + course.getName());
                for (Config config : course.getOffering().getConfigs()) {
                    if (sDebug)
                        sLog.debug("    -- " + config.getName());
                    for (Subpart subpart : config.getSubparts()) {
                        if (sDebug)
                            sLog.debug("      -- " + subpart.getName());
                        for (Section section : subpart.getSections()) {
                            section.setPenalty(penalties.getPenalty(section));
                            if (sDebug)
                                sLog.debug("        -- " + section);
                        }
                    }
                }
            }
            courseRequest.clearCache();
        }
    }
    
    public static void main(String[] args) {
        sDebug = true;
        ToolBox.configureLogging();
        new StudentPreferencePenalties(sDistTypePlain);
    }

}
