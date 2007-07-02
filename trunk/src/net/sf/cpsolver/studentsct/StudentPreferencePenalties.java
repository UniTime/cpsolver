package net.sf.cpsolver.studentsct;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimeLocation.IntEnumeration;
import net.sf.cpsolver.studentsct.heuristics.general.RouletteWheelSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * An attempt to empirically test the case when students can choose their sections (section times).
 * <br><br>
 * Each student has his/her own order of possible times of the week 
 * (selection of a day and an hour starting 7:30, 8:30, etc.) -- 
 * this order is computed using roulette wheel selection with the distribution of 
 * possible times defined in {@link StudentPreferencePenalties#sStudentRequestDistribution}.
 * <br><br>
 * A penalty for each section is computed proportionally based on this order 
 * (and the number of slots that falls into each time frame), 
 * the existing branch&bound selection is used to section each student one by one (in a random order).
 * <br><br>
 * Usage:<br>
 * <code>
 * for (Enumeration e=students.elements();e.hasMoreElements();) {<br>
 * &nbsp;&nbsp;// take a student (one by one)<br>
 * &nbsp;&nbsp;Student student = (Student)e.nextElement();<br>
 * <br>
 * &nbsp;&nbsp;// compute and apply penalties using this class<br>
 * &nbsp;&nbsp;new StudentPreferencePenalties().setPenalties(student);<br>
 * <br>
 * &nbsp;&nbsp;// section a student<br>
 * &nbsp;&nbsp;// for instance, {@link BranchBoundSelection} can be used (with Neighbour.BranchAndBoundMinimizePenalty set to true)<br>
 * &nbsp;&nbsp;Neighbour neighbour = new BranchBoundSelection(config).getSelection(student).select();<br>
 * &nbsp;&nbsp;if (neighbour!=null) neighbour.assign(iteration++);<br>
 * };
 * </code>
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class StudentPreferencePenalties {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(StudentPreferencePenalties.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private static boolean sDebug = false;
    public static int sDistTypeUniform = 0;
    public static int sDistTypePreference = 1;
    public static int sDistTypePreferenceQuadratic = 2;
    public static int sDistTypePreferenceReverse = 3;
    
    public static int[][] sStudentRequestDistribution = new int[][] {
        //morning, 7:30a, 8:30a, 9:30a, 10:30a, 11:30a, 12:30p, 1:30p, 2:30p, 3:30p, 4:30p, evening
        {       1,     1,     4,     7,     10,     10,      5,     8,     8,     6,     3,      1 }, //Monday
        {       1,     2,     4,     7,     10,     10,      5,     8,     8,     6,     3,      1 }, //Tuesday
        {       1,     2,     4,     7,     10,     10,      5,     8,     8,     6,     3,      1 }, //Wednesday
        {       1,     2,     4,     7,     10,     10,      5,     8,     8,     6,     3,      1 }, //Thursday
        {       1,     2,     4,     7,     10,     10,      5,     4,     3,     2,     1,      1 }, //Friday
        {       1,     1,     1,     1,      1,      1,      1,     1,     1,     1,     1,      1 }, //Saturday
        {       1,     1,     1,     1,      1,      1,      1,     1,     1,     1,     1,      1 }  //Sunday
    };
    private Hashtable iWeight = new Hashtable();
    
    /** 
     * Constructor. 
     * All possible times are ordered based on the distribution defined by
     * {@link StudentPreferencePenalties#sStudentRequestDistribution}.
     * The first time gets zero penalty, the second 1/nrTimes, the third 2/nrTimes etc. where 
     * nrTimes is the number of times in {@link StudentPreferencePenalties#sStudentRequestDistribution}.
     */
    public StudentPreferencePenalties(int disributionType) {
        RouletteWheelSelection roulette = new RouletteWheelSelection();
        for (int d=0;d<sStudentRequestDistribution.length;d++)
            for (int t=0;t<sStudentRequestDistribution[d].length;t++) {
                if (disributionType==sDistTypeUniform) {
                    roulette.add(new int[]{d,t}, 1);
                } else if (disributionType==sDistTypePreference) {
                    roulette.add(new int[]{d,t}, sStudentRequestDistribution[d][t]);
                } else if (disributionType==sDistTypePreferenceQuadratic) {
                    roulette.add(new int[]{d,t}, sStudentRequestDistribution[d][t]*sStudentRequestDistribution[d][t]);
                } else if (disributionType==sDistTypePreferenceReverse) {
                    roulette.add(new int[]{d,t}, 11-sStudentRequestDistribution[d][t]);
                } else {
                    roulette.add(new int[]{d,t}, 1);
                }
            }
        int idx = 0;
        while (roulette.hasMoreElements()) {
            int[] dt = (int[])roulette.nextElement();
            iWeight.put(dt[0]+"."+dt[1], new Double(((double)idx)/(roulette.size()-1)));
            if (sDebug) sLog.debug("  -- "+(idx+1)+". preference is "+toString(dt[0],dt[1])+" (P:"+sDF.format(((double)idx)/(roulette.size()-1))+")");
            idx++;
        }
    }
    
    /** Return day index in {@link StudentPreferencePenalties#sStudentRequestDistribution} for the given slot. */ 
    public static int day(int slot) {
        return slot / Constants.SLOTS_PER_DAY;
    }
    
    /** Return time index in {@link StudentPreferencePenalties#sStudentRequestDistribution} for the given slot. */
    public static int time(int slot) {
        int s = slot % Constants.SLOTS_PER_DAY;
        int min =  (s * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN);
        if (min<450) return 0; //morning
        int idx = 1+(min-450)/60;
        return (idx>11?11:idx); //11+ is evening
    }
    
    /** Return time of the given day and time index of {@link StudentPreferencePenalties#sStudentRequestDistribution}. */
    public String toString(int day, int time) {
        if (time==0) return Constants.DAY_NAMES_SHORT[day]+" morning";
        if (time==11) return Constants.DAY_NAMES_SHORT[day]+" evening";
        return Constants.DAY_NAMES_SHORT[day]+" "+(6+time)+":30";
    }
    
    /** 
     * Return penalty of the given time. It is comuted as average of the penalty for each time slot of the time. 
     **/
    public double getPenalty(TimeLocation time) {
        int nrSlots = 0;
        double penalty = 0.0;
        for (IntEnumeration e=time.getSlots();e.hasMoreElements();) {
            int slot = e.nextInt();
            nrSlots++;
            penalty += ((Double)iWeight.get(day(slot)+"."+time(slot))).doubleValue();
        }
        return penalty/nrSlots;
    }
    
    /**
     * Return penalty of an assignment. It is a penalty of its time (see {@link Assignment#getTime()}) or zero
     * if the time is null.
     */
    public double getPenalty(Assignment assignment) {
        return (assignment.getTime()==null?0.0:getPenalty(assignment.getTime()));
    }

    /**
     * Set the computed penalties to all sections of all requests of the given student
     */
    public static void setPenalties(Student student, int distributionType) {
        if (sDebug) sLog.debug("Setting penalties for "+student);
        StudentPreferencePenalties penalties = new StudentPreferencePenalties(distributionType);
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (!(request instanceof CourseRequest)) continue;
            CourseRequest courseRequest = (CourseRequest)request;
            if (sDebug) sLog.debug("-- "+courseRequest);
            for (Enumeration f=courseRequest.getCourses().elements();f.hasMoreElements();) {
                Course course = (Course)f.nextElement();
                if (sDebug) sLog.debug("  -- "+course.getName());
                for (Enumeration g=course.getOffering().getConfigs().elements();g.hasMoreElements();) {
                    Config config = (Config)g.nextElement();
                    if (sDebug) sLog.debug("    -- "+config.getName());
                    for (Enumeration h=config.getSubparts().elements();h.hasMoreElements();) {
                        Subpart subpart = (Subpart)h.nextElement();
                        if (sDebug) sLog.debug("      -- "+subpart.getName());
                        for (Enumeration i=subpart.getSections().elements();i.hasMoreElements();) {
                            Section section = (Section)i.nextElement();
                            section.setPenalty(penalties.getPenalty(section));
                            if (sDebug) sLog.debug("        -- "+section);
                        }
                    }
                }
            }
            courseRequest.clearCache();
        }
    }
    
}
