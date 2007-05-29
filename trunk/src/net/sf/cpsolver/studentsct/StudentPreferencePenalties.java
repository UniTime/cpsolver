package net.sf.cpsolver.studentsct;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimeLocation.IntEnumeration;
import net.sf.cpsolver.studentsct.heuristics.general.RouletteWheelSelection;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

public class StudentPreferencePenalties {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(StudentPreferencePenalties.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private static boolean sDebug = false;
    
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
    
    public StudentPreferencePenalties() {
        RouletteWheelSelection roulette = new RouletteWheelSelection();
        for (int d=0;d<sStudentRequestDistribution.length;d++)
            for (int t=0;t<sStudentRequestDistribution[d].length;t++)
                roulette.add(new int[]{d,t}, sStudentRequestDistribution[d][t]);
        int idx = 0;
        while (roulette.hasMoreElements()) {
            int[] dt = (int[])roulette.nextElement();
            iWeight.put(dt[0]+"."+dt[1], new Double(((double)idx)/(roulette.size()-1)));
            if (sDebug) sLog.debug("  -- "+(idx+1)+". preference is "+toString(dt[0],dt[1])+" (P:"+sDF.format(((double)idx)/(roulette.size()-1))+")");
            idx++;
        }
    }
    
    public static int day(int slot) {
        return slot / Constants.SLOTS_PER_DAY;
    }
    
    public static int time(int slot) {
        int s = slot % Constants.SLOTS_PER_DAY;
        int min =  (s * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN);
        if (min<450) return 0; //morning
        int idx = 1+(min-450)/60;
        return (idx>11?11:idx); //11+ is evening
    }
    
    public String toString(int day, int time) {
        if (time==0) return Constants.DAY_NAMES_SHORT[day]+" morning";
        if (time==11) return Constants.DAY_NAMES_SHORT[day]+" evening";
        return Constants.DAY_NAMES_SHORT[day]+" "+(6+time)+":30";
    }
    
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
    
    public double getPenalty(Assignment assignment) {
        return (assignment.getTime()==null?0.0:getPenalty(assignment.getTime()));
    }

    public static void setPenalties(Student student) {
        if (sDebug) sLog.debug("Setting penalties for "+student);
        StudentPreferencePenalties penalties = new StudentPreferencePenalties();
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
        }
    }
    
}
