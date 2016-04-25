package org.cpsolver.instructor.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.Test;
import org.cpsolver.instructor.constraints.InstructorConstraint;
import org.cpsolver.instructor.constraints.SameInstructorConstraint;
import org.cpsolver.instructor.constraints.SameLinkConstraint;
import org.cpsolver.instructor.criteria.DifferentLecture;
import org.cpsolver.instructor.model.Course;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.Attribute;
import org.cpsolver.instructor.model.Preference;
import org.cpsolver.instructor.model.Section;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

public class MathTest extends Test {
    private static Logger sLog = Logger.getLogger(MathTest.class);
    
    public MathTest(DataProperties properties) {
        super(properties);
        removeCriterion(DifferentLecture.class);
    }
    
    public String toString(Instructor instructor) {
        StringBuffer sb = new StringBuffer();
        sb.append(instructor.getExternalId());
        sb.append(",\"" + instructor.getAvailable() + "\"");
        for (int i = 0; i < 3; i++) {
            Preference<Course> p = (i < instructor.getCoursePreferences().size() ? instructor.getCoursePreferences().get(i) : null);
            sb.append("," + (p == null ? "" : p.getTarget().getCourseName()));
        }
        sb.append("," + (instructor.getPreference() == 0 ? "Yes" : "No"));
        sb.append("," + (instructor.isBackToBackPreferred() ? "1" : instructor.isBackToBackDiscouraged() ? "-1" : "0"));
        sb.append("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
        sb.append("," + (instructor.getAttributes().isEmpty() ? "" : instructor.getAttributes().get(0).getAttributeName())); 
        return sb.toString();
    }
    
    public String getLink(TeachingRequest request) {
        for (Constraint<TeachingRequest, TeachingAssignment> c: request.constraints()) {
            if (c instanceof SameLinkConstraint)
                return c.getName().substring(c.getName().indexOf('-') + 1);
        }
        return null;
    }
    
    public Long getAssignmentId(TeachingRequest request) {
        for (Constraint<TeachingRequest, TeachingAssignment> c: request.constraints()) {
            if (c instanceof SameInstructorConstraint && ((SameInstructorConstraint) c).getConstraintId() != null)
                return ((SameInstructorConstraint) c).getConstraintId();
        }
        return null;
    }
    
    public int countDiffLinks(Set<TeachingAssignment> assignments) {
        Set<String> links = new HashSet<String>();
        for (TeachingAssignment assignment : assignments) {
            String link = getLink(assignment.variable());
            if (link != null)
                links.add(link);
        }
        return Math.max(0, links.size() - 1);
    }
    
    public String toString(TeachingRequest request) {
        StringBuffer sb = new StringBuffer();
        Long assId = getAssignmentId(request);
        sb.append(assId == null ? "" : assId);
        sb.append("," + request.getCourse().getCourseName());
        Section section = request.getSections().get(0);
        sb.append("," + section.getSectionName());
        sb.append("," + section.getTimeName(true));
        sb.append("," + (section.hasRoom() ? section.getRoom() : ""));
        String link = getLink(request);
        sb.append("," + (link == null ? "" : link));
        Map<String, Integer> levels = new HashMap<String, Integer>();
        for (Preference<Attribute> p: request.getAttributePreferences())
            levels.put(p.getTarget().getAttributeName(), - p.getPreference());
        sb.append(",\"" + levels + "\"");
        sb.append("," + new DecimalFormat("0.0").format(request.getLoad()));
        return sb.toString();
    }
    
    @Override
    protected boolean load(File dir, Assignment<TeachingRequest, TeachingAssignment> assignment) {
        if (!dir.isDirectory())
            return super.load(dir, assignment);
        try {
            String line = null;
            BufferedReader r = new BufferedReader(new FileReader(new File(dir, "courses.csv")));
            Map<String, Course> courses = new HashMap<String, Course>();
            Map<Long, List<TeachingRequest>> id2classes = new HashMap<Long, List<TeachingRequest>>();
            Map<String, List<TeachingRequest>> link2classes = new HashMap<String, List<TeachingRequest>>();
            long assId = 0, reqId = 0;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                String[] fields = line.split(",");
                Long id = Long.valueOf(fields[0]);
                String course = fields[1];
                String section = fields[2];
                int idx = 3;
                int dayCode = 0;
                idx: while (idx < fields.length && (idx == 3 || fields[idx].length() == 1)) {
                    for (int i = 0; i < fields[idx].length(); i++) {
                        switch (fields[idx].charAt(i)) {
                            case 'M':
                                dayCode += Constants.DAY_CODES[0];
                                break;
                            case 'T':
                                dayCode += Constants.DAY_CODES[1];
                                break;
                            case 'W':
                                dayCode += Constants.DAY_CODES[2];
                                break;
                            case 'R':
                                dayCode += Constants.DAY_CODES[3];
                                break;
                            case 'F':
                                dayCode += Constants.DAY_CODES[4];
                                break;
                            default:
                                break idx;
                        }
                    }
                    idx++;
                }
                int startSlot = 0;
                if (dayCode > 0) {
                    int time = Integer.parseInt(fields[idx++]);
                    startSlot = 12 * (time / 100) + (time % 100) / 5;
                }
                String room = null;
                if (idx < fields.length)
                    room = fields[idx++];
                String link = null;
                if (idx < fields.length)
                    link = fields[idx++];
                int length = 12;
                if (idx < fields.length) {
                    int time = Integer.parseInt(fields[idx++]);
                    int endSlot = 12 * (time / 100) + (time % 100) / 5;
                    length = endSlot - startSlot;
                    if (length == 10)
                        length = 12;
                    else if (length == 15)
                        length = 18;
                }
                List<Section> sections = new ArrayList<Section>();
                TimeLocation time = new TimeLocation(dayCode, startSlot, length, 0, 0.0, 0, null, "", null, (length == 18 ? 15 : 10));
                sections.add(new Section(assId++, id.toString(), section, course + " " + section + " " + time.getName(true) + (room == null ? "" : " " + room), time, room, false, false));
                Course c = courses.get(course);
                if (c == null) {
                    c = new Course(courses.size(), course, true, false);
                    courses.put(course, c);
                }
                TeachingRequest clazz = new TeachingRequest(reqId++, c, 0f, sections);
                addVariable(clazz);
                List<TeachingRequest> classes = id2classes.get(id);
                if (classes == null) {
                    classes = new ArrayList<TeachingRequest>();
                    id2classes.put(id, classes);
                }
                classes.add(clazz);
                if (link != null && !link.isEmpty()) {
                    List<TeachingRequest> linked = link2classes.get(course + "-" + link);
                    if (linked == null) {
                        linked = new ArrayList<TeachingRequest>();
                        link2classes.put(course + "-" + link, linked);
                    }
                    linked.add(clazz);
                }
            }

            for (Map.Entry<Long, List<TeachingRequest>> e : id2classes.entrySet()) {
                Long id = e.getKey();
                List<TeachingRequest> classes = e.getValue();
                if (classes.size() > 1) {
                    SameInstructorConstraint sa = new SameInstructorConstraint(id, "A" + id.toString(), Constants.sPreferenceRequired);
                    for (TeachingRequest c : classes)
                        sa.addVariable(c);
                    addConstraint(sa);
                }
            }
            for (Map.Entry<String, List<TeachingRequest>> e : link2classes.entrySet()) {
                String link = e.getKey();
                List<TeachingRequest> classes = e.getValue();
                if (classes.size() > 1) {
                    SameLinkConstraint sa = new SameLinkConstraint(null, link, Constants.sPreferencePreferred);
                    for (TeachingRequest c : classes)
                        sa.addVariable(c);
                    addConstraint(sa);
                }
            }
            
            Attribute.Type level = new Attribute.Type(0l, "Level", false, true);
            addAttributeType(level);
            Map<String, Attribute> code2attribute = new HashMap<String, Attribute>();

            r = new BufferedReader(new FileReader(new File(dir, "level_codes.csv")));
            String[] codes = r.readLine().split(",");
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                String[] fields = line.split(",");
                String code = fields[0];
                if (code.startsWith("\"") && code.endsWith("\""))
                    code = code.substring(1, code.length() - 1);
                Attribute attribute = code2attribute.get(code);
                if (attribute == null) {
                    attribute = new Attribute(code2attribute.size(), code, level);
                    code2attribute.put(code, attribute);
                }
                for (int i = 1; i < codes.length; i++) {
                    int pref = Integer.parseInt(fields[i]);
                    if (pref > 0)
                        for (TeachingRequest clazz : variables()) {
                            if (clazz.getName().contains(codes[i]))
                                clazz.addAttributePreference(new Preference<Attribute>(attribute, -pref));
                        }
                }
            }
            r = new BufferedReader(new FileReader(new File(dir, "hours_per_course.csv")));
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                String[] fields = line.split(",");
                for (TeachingRequest clazz : variables()) {
                    if (clazz.getName().contains(fields[0]))
                        clazz.setLoad(Float.parseFloat(fields[1]));
                }
            }

            String defaultCode = getProperties().getProperty("TA.DefaultLevelCode", "XXX");
            Attribute defaultAttribute = code2attribute.get(defaultCode);
            if (defaultAttribute == null) {
                defaultAttribute = new Attribute(code2attribute.size(), defaultCode, level);
                code2attribute.put(defaultCode, defaultAttribute);
            }
            for (TeachingRequest clazz : variables()) {
                sLog.info("Added class " + toString(clazz));
                if (clazz.getAttributePreferences().isEmpty()) {
                    sLog.error("No level: " + toString(clazz));
                    clazz.addAttributePreference(new Preference<Attribute>(defaultAttribute, -1));
                }
                if (clazz.getLoad() == 0.0) {
                    sLog.error("No load: " + toString(clazz));
                    clazz.setLoad(getProperties().getPropertyFloat("TA.DefaultLoad", 10f));
                }
            }

            r = new BufferedReader(new FileReader(new File(dir, "students.csv")));
            Set<String> studentIds = new HashSet<String>();
            double studentMaxLoad = 0.0;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                String[] fields = line.split(",");
                if ("puid".equals(fields[0]))
                    continue;
                int idx = 0;
                String id = fields[idx++];
                if (!studentIds.add(id)) {
                    sLog.error("Student " + id + " is two or more times in the file.");
                }
                boolean[] av = new boolean[50];
                for (int i = 0; i < 50; i++)
                    av[i] = "1".equals(fields[idx++]);
                List<String> prefs = new ArrayList<String>();
                for (int i = 0; i < 3; i++) {
                    String p = fields[idx++].replace("Large lecture", "LEC").replace("Lecture", "LEC").replace("Recitation", "REC");
                    if (p.startsWith("MA "))
                        p = p.substring(3);
                    if ("I have no preference".equals(p))
                        continue;
                    prefs.add(p);
                }
                boolean grad = "Yes".equals(fields[idx++]);
                int b2b = Integer.parseInt(fields[idx++]);
                float maxLoad = Float.parseFloat(fields[idx++]);
                if (maxLoad == 0)
                    maxLoad = getProperties().getPropertyFloat("TA.DefaultMaxLoad", 20f);
                String code = (idx < fields.length ? fields[idx++] : null);
                Instructor instructor = new Instructor(Long.valueOf(id.replace("-","")), id, null, grad ? Constants.sPreferenceLevelNeutral : Constants.sPreferenceLevelDiscouraged, maxLoad);
                for (int i = 0; i < prefs.size(); i++) {
                    String pref = prefs.get(i);
                    if (pref.indexOf(' ') > 0) pref = pref.substring(0, pref.indexOf(' '));
                    Course c = courses.get(pref);
                    if (c == null) {
                        c = new Course(courses.size(), pref, true, false);
                        courses.put(pref, c);
                    }
                    instructor.addCoursePreference(new Preference<Course>(c, i == 0 ? -10 : i == 1 ? -8 : -5));
                }
                if (code != null) {
                    Attribute attribute = code2attribute.get(code);
                    if (attribute == null) {
                        attribute = new Attribute(code2attribute.size(), code, level);
                        code2attribute.put(code, attribute);
                    }
                    instructor.addAttribute(attribute);
                }
                if (b2b == 1)
                    instructor.setBackToBackPreference(Constants.sPreferenceLevelPreferred);
                else if (b2b == -1)
                    instructor.setBackToBackPreference(Constants.sPreferenceLevelDiscouraged);
                for (int d = 0; d < 5; d++) {
                    int f = -1;
                    for (int t = 0; t < 10; t++) {
                        if (!av[10 * d + t]) {
                            if (f < 0) f = t;
                        } else {
                            if (f >= 0) {
                                instructor.addTimePreference(new Preference<TimeLocation>(new TimeLocation(Constants.DAY_CODES[d], 90 + 12 * f, (t - f) * 12, 0, 0.0, null, "", null, 0), Constants.sPreferenceLevelProhibited));
                                f = -1;
                            }
                        }
                    }
                    if (f >= 0) {
                        instructor.addTimePreference(new Preference<TimeLocation>(new TimeLocation(Constants.DAY_CODES[d], 90 + 12 * f, (10 - f) * 12, 0, 0.0, null, "", null, 0), Constants.sPreferenceLevelProhibited));
                        f = -1;
                    }
                }
                for (TeachingRequest clazz : variables()) {
                    if (instructor.canTeach(clazz) && !clazz.getAttributePreference(instructor).isProhibited())
                        instructor.getConstraint().addVariable(clazz);
                }
                if (instructor.getMaxLoad() > 0 && !instructor.getConstraint().variables().isEmpty()) {
                    addConstraint(instructor.getConstraint());
                    sLog.info("Added student " + toString(instructor));
                    int nrClasses = 0;
                    for (TeachingRequest c : instructor.getConstraint().variables()) {
                        if (c.getId() >= 0) {
                            sLog.info("  -- " + toString(c) + "," + (-c.getAttributePreference(instructor).getPreferenceInt()) + "," + (-instructor.getCoursePreference(c.getCourse()).getPreference()));
                            nrClasses++;
                        }
                    }
                    if (nrClasses == 0) {
                        sLog.info("  -- no courses available");
                    }
                    studentMaxLoad += instructor.getMaxLoad();
                } else {
                    sLog.info("Ignoring student " + instructor);
                    if (instructor.getMaxLoad() == 0)
                        sLog.info("  -- zero max load");
                    else
                        sLog.info("  -- no courses available");
                }
            }

            double totalLoad = 0.0;
            for (TeachingRequest clazz : variables()) {
                if (clazz.values(getEmptyAssignment()).isEmpty())
                    sLog.error("No values: " + toString(clazz));
                totalLoad += clazz.getLoad();
            }

            Map<String, Double> studentLevel2load = new HashMap<String, Double>();
            for (Constraint<TeachingRequest, TeachingAssignment> c : constraints()) {
                if (c instanceof InstructorConstraint) {
                    InstructorConstraint ic = (InstructorConstraint) c;
                    Set<Attribute> levels = ic.getInstructor().getAttributes(level);
                    String studentLevel = (levels.isEmpty() ? "null" : levels.iterator().next().getAttributeName());
                    Double load = studentLevel2load.get(studentLevel);
                    studentLevel2load.put(studentLevel, ic.getInstructor().getMaxLoad() + (load == null ? 0.0 : load));
                }
            }
            sLog.info("Student max loads: (total: " + sDoubleFormat.format(studentMaxLoad) + ")");
            for (String studentLevel : new TreeSet<String>(studentLevel2load.keySet())) {
                Double load = studentLevel2load.get(studentLevel);
                sLog.info("  " + studentLevel + ": " + sDoubleFormat.format(load));
            }
            Map<String, Double> clazzLevel2load = new HashMap<String, Double>();
            for (TeachingRequest clazz : variables()) {
                String classLevel = null;
                TreeSet<String> levels = new TreeSet<String>();
                for (Preference<Attribute> ap: clazz.getAttributePreferences())
                    levels.add(ap.getTarget().getAttributeName());
                for (String l : levels) {
                    classLevel = (classLevel == null ? "" : classLevel + ",") + l;
                }
                if (classLevel == null)
                    classLevel = "null";
                if (clazz.getId() < 0)
                    classLevel = clazz.getName();
                Double load = clazzLevel2load.get(level);
                clazzLevel2load.put(classLevel, clazz.getLoad() + (load == null ? 0.0 : load));
            }
            sLog.info("Class loads: (total: " + sDoubleFormat.format(totalLoad) + ")");
            for (String classLevel : new TreeSet<String>(clazzLevel2load.keySet())) {
                Double load = clazzLevel2load.get(classLevel);
                sLog.info("  " + level + ": " + sDoubleFormat.format(load));
            }
            return true;
        } catch (IOException e) {
            sLog.error("Failed to load the problem: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    protected void generateReports(File dir, Assignment<TeachingRequest, TeachingAssignment> assignment) throws IOException {
        PrintWriter out = new PrintWriter(new File(dir, "solution-assignments.csv"));
        out.println("Assignment Id,Course,Section,Time,Room,Link,Level,Load,Student,Availability,1st Preference,2nd Preference,3rd Preference,Graduate,Back-To-Back,Max Load,Level,Level,Preference");
        for (TeachingRequest request : variables()) {
            Long assId = getAssignmentId(request);
            out.print(assId == null ? "" : assId);
            out.print("," + request.getCourse().getCourseName());
            Section section = request.getSections().get(0);
            out.print("," + section.getSectionType());
            out.print("," + section.getTimeName(true));
            out.print("," + (section.hasRoom() ? section.getRoom() : ""));
            String link = getLink(request);
            out.print("," + (link == null ? "" : link));
            Map<String, Integer> levels = new HashMap<String, Integer>();
            for (Preference<Attribute> p: request.getAttributePreferences())
                levels.put(p.getTarget().getAttributeName(), - p.getPreference());
            out.print(",\"" + levels + "\"");
            out.print("," + new DecimalFormat("0.0").format(request.getLoad()));
            TeachingAssignment value = assignment.getValue(request);
            if (value != null) {
                out.print("," + toString(value.getInstructor()));
                out.print("," + (-value.getAttributePreference()));
                out.print("," + (value.getCoursePreference() == -10 ? "1" : value.getCoursePreference() == -8 ? "2" : value.getCoursePreference() == -5 ? "3" : ""));
            }
            out.println();
        }
        out.flush();
        out.close();

        out = new PrintWriter(new File(dir, "solution-students.csv"));
        out.println("Student,Availability,1st Preference,2nd Preference,3rd Preference,Graduate,Back-To-Back,Max Load,Level,Assigned Load,Avg Level,Avg Preference,Back-To-Back,Diff Links,1st Assignment,2nd Assignment, 3rd Assignment");
        for (Constraint<TeachingRequest, TeachingAssignment> constraint : constraints()) {
            if (constraint instanceof InstructorConstraint) {
                InstructorConstraint ic = (InstructorConstraint) constraint;
                Instructor instructor = ic.getInstructor();
                out.print(instructor.getExternalId());
                out.print(",\"" + instructor.getAvailable() + "\"");
                for (int i = 0; i < 3; i++) {
                    Preference<Course> p = (i < instructor.getCoursePreferences().size() ? instructor.getCoursePreferences().get(i) : null);
                    out.print("," + (p == null ? "" : p.getTarget().getCourseName()));
                }
                out.print("," + (instructor.getPreference() == 0 ? "Yes" : "No"));
                out.print("," + (instructor.isBackToBackPreferred() ? "1" : instructor.isBackToBackDiscouraged() ? "-1" : "0"));
                out.print("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
                out.print("," + (instructor.getAttributes().isEmpty() ? "" : instructor.getAttributes().get(0).getAttributeName())); 
                InstructorConstraint.Context context = ic.getContext(assignment);
                out.print("," + new DecimalFormat("0.0").format(context.getLoad()));
                double level = 0.0, pref = 0.0;
                for (TeachingAssignment ta : context.getAssignments()) {
                    level += Math.abs(ta.getAttributePreference());
                    pref += (ta.getCoursePreference() == -10 ? 1 : ta.getCoursePreference() == -8 ? 2 : ta.getCoursePreference() == -5 ? 3 : 0);
                }
                int diffLinks = countDiffLinks(context.getAssignments());
                out.print("," + (context.getAssignments().isEmpty() ? "" : new DecimalFormat("0.0").format(level / context.getAssignments().size())));
                out.print("," + (context.getAssignments().isEmpty() || pref <= 0.0 ? "" : new DecimalFormat("0.0").format(pref / context.getAssignments().size())));
                out.print("," + new DecimalFormat("0.0").format(100.0 * context.countBackToBackPercentage()));
                out.print("," + (diffLinks <= 0 ? "" : diffLinks));
                for (TeachingAssignment ta : context.getAssignments()) {
                    String link = getLink(ta.variable());
                    out.print("," + ta.variable().getCourse() + " " + ta.variable().getSections().get(0).getSectionType() + " " + ta.variable().getSections().get(0).getTime().getName(true) + (link == null ? "" : " " + link));
                }
                out.println();
            }
        }
        out.flush();
        out.close();
    }
    
    public static void main(String[] args) throws Exception {
        DataProperties config = new DataProperties();
        config.load(MathTest.class.getClass().getResourceAsStream("/org/cpsolver/instructor/test/math.properties"));
        config.putAll(System.getProperties());
        ToolBox.configureLogging();
        
        new MathTest(config).execute();
    }
}
