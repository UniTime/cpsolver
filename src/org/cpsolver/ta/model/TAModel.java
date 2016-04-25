package org.cpsolver.ta.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.ta.constraints.SameAssignment;
import org.cpsolver.ta.constraints.Student;
import org.cpsolver.ta.criteria.BackToBack;
import org.cpsolver.ta.criteria.DiffLink;
import org.cpsolver.ta.criteria.Graduate;
import org.cpsolver.ta.criteria.LevelCode;
import org.cpsolver.ta.criteria.Preference;
import org.cpsolver.ta.criteria.SameSections;
import org.cpsolver.ta.criteria.TimeOverlaps;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class TAModel extends Model<TeachingRequest, TeachingAssignment> {
    private static Logger sLog = Logger.getLogger(TAModel.class);
    public static String[] sDayCodes = new String[] { "M", "T", "W", "R", "F" };
    private DataProperties iProperties;

    public TAModel(DataProperties properties) {
        super();
        iProperties = properties;
        addCriterion(new Preference());
        addCriterion(new Graduate());
        addCriterion(new BackToBack());
        addCriterion(new LevelCode());
        addCriterion(new DiffLink());
        addCriterion(new TimeOverlaps());
        addCriterion(new SameSections());
    }

    public DataProperties getProperties() {
        return iProperties;
    }

    public void load(File dir) throws IOException {
        String line = null;
        BufferedReader r = new BufferedReader(new FileReader(new File(dir, "courses.csv")));
        Map<Long, List<TeachingRequest>> id2classes = new HashMap<Long, List<TeachingRequest>>();
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
            TeachingRequest clazz = new TeachingRequest(id, course, section, dayCode, startSlot, length, room, link);
            addVariable(clazz);
            List<TeachingRequest> classes = id2classes.get(id);
            if (classes == null) {
                classes = new ArrayList<TeachingRequest>();
                id2classes.put(id, classes);
            }
            classes.add(clazz);
        }

        for (Map.Entry<Long, List<TeachingRequest>> e : id2classes.entrySet()) {
            Long id = e.getKey();
            List<TeachingRequest> classes = e.getValue();
            if (classes.size() > 1) {
                SameAssignment sa = new SameAssignment(id);
                for (TeachingRequest c : classes)
                    sa.addVariable(c);
                addConstraint(sa);
            }
        }

        r = new BufferedReader(new FileReader(new File(dir, "level_codes.csv")));
        String[] codes = r.readLine().split(",");
        while ((line = r.readLine()) != null) {
            if (line.trim().isEmpty())
                continue;
            String[] fields = line.split(",");
            String code = fields[0];
            if (code.startsWith("\"") && code.endsWith("\""))
                code = code.substring(1, code.length() - 1);
            for (int i = 1; i < codes.length; i++) {
                int pref = Integer.parseInt(fields[i]);
                if (pref > 0)
                    for (TeachingRequest clazz : variables()) {
                        if (clazz.getName().contains(codes[i]))
                            clazz.getLevels().put(code, pref);
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
                    clazz.setLoad(Double.parseDouble(fields[1])); // /id2classes.get(clazz.getAssignmentId()).size());
            }
        }

        /*
         * r = new BufferedReader(new FileReader(new File(dir,
         * "assignments.csv"))); int a = 0; while ((line = r.readLine()) !=
         * null) { if (line.trim().isEmpty()) continue; String[] fields =
         * line.split(","); Clazz other = new Clazz(--a, fields[0], 0, 0, 0,
         * null, null); other.setLoad(Double.parseDouble(fields[1]));
         * addVariable(other); }
         */

        for (TeachingRequest clazz : variables()) {
            sLog.info("Added class " + clazz);
            if (clazz.getAssignmentId() >= 0 && clazz.getLevels().isEmpty()) {
                sLog.error("No level: " + clazz);
                clazz.getLevels().put(getProperties().getProperty("TA.DefaultLevelCode", "XXX"), 1);
            }
            if (clazz.getAssignmentId() >= 0 && clazz.getLoad() == 0.0) {
                sLog.error("No load: " + clazz);
                clazz.setLoad(getProperties().getPropertyDouble("TA.DefaultLoad", 10.0));
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
            double maxLoad = Double.parseDouble(fields[idx++]);
            if (maxLoad == 0)
                maxLoad = getProperties().getPropertyDouble("TA.DefaultMaxLoad", 20.0);
            String level = (idx < fields.length ? fields[idx++] : null);
            Student student = new Student(id, id, prefs, grad, b2b, maxLoad, level);
            for (int d = 0; d < 5; d++) {
                int f = -1;
                for (int t = 0; t < 10; t++) {
                    if (!av[10 * d + t]) {
                        if (f < 0) f = t;
                    } else {
                        if (f >= 0) {
                            student.setNotAvailable(new TimeLocation(Constants.DAY_CODES[d], 90 + 12 * f, (t - f) * 12, 0, 0.0, null, "", null, 0));
                            f = -1;
                        }
                    }
                }
                if (f >= 0) {
                    student.setNotAvailable(new TimeLocation(Constants.DAY_CODES[d], 90 + 12 * f, (10 - f) * 12, 0, 0.0, null, "", null, 0));
                    f = -1;
                }
            }
            for (TeachingRequest clazz : variables()) {
                if (student.canTeach(clazz))
                    student.addVariable(clazz);
            }
            if (student.getMaxLoad() > 0 && !student.variables().isEmpty()) {
                addConstraint(student);
                sLog.info("Added student " + student.toString());
                int nrClasses = 0;
                for (TeachingRequest c : student.variables()) {
                    if (c.getId() >= 0) {
                        sLog.info("  -- " + c);
                        nrClasses++;
                    }
                }
                if (nrClasses == 0) {
                    sLog.info("  -- no courses available");
                }
                studentMaxLoad += student.getMaxLoad();
            } else {
                sLog.info("Ignoring student " + student);
                if (student.getMaxLoad() == 0)
                    sLog.info("  -- zero max load");
                else
                    sLog.info("  -- no courses available");
            }
        }

        double totalLoad = 0.0;
        for (TeachingRequest clazz : variables()) {
            if (clazz.values(getEmptyAssignment()).isEmpty())
                sLog.error("No values: " + clazz);
            totalLoad += clazz.getLoad();
        }

        Map<String, Double> studentLevel2load = new HashMap<String, Double>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : constraints()) {
            if (c instanceof Student) {
                Student s = (Student) c;
                String level = (s.getLevel() == null ? "null" : s.getLevel());
                Double load = studentLevel2load.get(level);
                studentLevel2load.put(level, s.getMaxLoad() + (load == null ? 0.0 : load));
            }
        }
        sLog.info("Student max loads: (total: " + sDoubleFormat.format(studentMaxLoad) + ")");
        for (String level : new TreeSet<String>(studentLevel2load.keySet())) {
            Double load = studentLevel2load.get(level);
            sLog.info("  " + level + ": " + sDoubleFormat.format(load));
        }
        Map<String, Double> clazzLevel2load = new HashMap<String, Double>();
        for (TeachingRequest clazz : variables()) {
            String level = null;
            for (String l : new TreeSet<String>(clazz.getLevels().keySet())) {
                level = (level == null ? "" : level + ",") + l;
            }
            if (level == null)
                level = "null";
            if (clazz.getId() < 0)
                level = clazz.getName();
            Double load = clazzLevel2load.get(level);
            clazzLevel2load.put(level, clazz.getLoad() + (load == null ? 0.0 : load));
        }
        sLog.info("Class loads: (total: " + sDoubleFormat.format(totalLoad) + ")");
        for (String level : new TreeSet<String>(clazzLevel2load.keySet())) {
            Double load = clazzLevel2load.get(level);
            sLog.info("  " + level + ": " + sDoubleFormat.format(load));
        }
    }

    @Override
    public Map<String, String> getInfo(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        Map<String, String> info = super.getInfo(assignment);

        double totalLoad = 0.0;
        double assignedLoad = 0.0;
        for (TeachingRequest clazz : variables()) {
            totalLoad += clazz.getLoad();
            if (assignment.getValue(clazz) != null)
                assignedLoad += clazz.getLoad();
        }
        info.put("Assigned load", getPerc(assignedLoad, totalLoad, 0) + "% (" + sDoubleFormat.format(assignedLoad) + " / " + sDoubleFormat.format(totalLoad) + ")");

        return info;
    }

    @Override
    public double getTotalValue(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        double ret = 0;
        for (Criterion<TeachingRequest, TeachingAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment);
        return ret;
    }

    @Override
    public double getTotalValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double ret = 0;
        for (Criterion<TeachingRequest, TeachingAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment, variables);
        return ret;
    }

    @Override
    public String toString() {
        Set<String> props = new TreeSet<String>();
        for (Criterion<TeachingRequest, TeachingAssignment> criterion : getCriteria()) {
            String val = criterion.toString();
            if (!val.isEmpty())
                props.add(val);
        }
        return props.toString();
    }

    public void save(Assignment<TeachingRequest, TeachingAssignment> assignment, File dir) throws IOException {
        PrintWriter out = new PrintWriter(new File(dir, "solution-assignments.csv"));
        out.println("Assignment Id,Course,Section,Time,Room,Link,Level,Load,Student Id,Name");
        for (TeachingRequest v : variables()) {
            out.println(v.toString() + "," + (assignment.getValue(v) == null ? "" : assignment.getValue(v).getStudent().getStudentId()) +
                    ",\"" + (assignment.getValue(v) == null ? "" : assignment.getValue(v).getStudent().getStudentName()) + "\"");
        }
        out.flush();
        out.close();

        out = new PrintWriter(new File(dir, "solution-students.csv"));
        out.println("Student Id,Name,Not Available,1st Preference,2nd Preference,3rd Preference,Graduate,Back-To-Back,Level,Assigned Load,Avg Level,Avg Preference,Back-To-Back,Diff Links,Time Overlaps,Same Sections,1st Assignment,2nd Assignment, 3rd Assignment");
        for (Constraint<TeachingRequest, TeachingAssignment> constraint : constraints()) {
            if (constraint instanceof Student) {
                Student s = (Student) constraint;
                out.println(s.getContext(assignment).toString());
            }
        }
        out.flush();
        out.close();
    }
    
    public Document save(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        DecimalFormat sDF7 = new DecimalFormat("0000000");
        boolean saveInitial = getProperties().getPropertyBoolean("Xml.SaveInitial", false);
        boolean saveBest = getProperties().getPropertyBoolean("Xml.SaveBest", false);
        boolean saveSolution = getProperties().getPropertyBoolean("Xml.SaveSolution", true);
        Document document = DocumentHelper.createDocument();
        if (assignment != null && assignment.nrAssignedVariables() > 0) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (getProperties().getPropertyBoolean("Xml.ExtendedInfo", true) ? getExtendedInfo(assignment) : getInfo(assignment));
            for (String key : new TreeSet<String>(solutionInfo.keySet())) {
                String value = solutionInfo.get(key);
                comments.append("    " + key + ": " + value + "\n");
            }
            document.addComment(comments.toString());
        }
        Element root = document.addElement("ta");
        root.addAttribute("version", "1.0");
        root.addAttribute("created", String.valueOf(new Date()));
        Map<String, Double> course2load = new HashMap<String, Double>();
        Map<String, Map<String, Integer>> course2levels = new HashMap<String, Map<String, Integer>>();
        for (TeachingRequest request: variables()) {
            Double load = course2load.get(request.getCourseName());
            if (load == null) {
                course2load.put(request.getCourseName(), request.getLoad());
                course2levels.put(request.getCourseName(), request.getLevels());
            }
        }
        Element coursesEl = root.addElement("courses");
        for (String course: new TreeSet<String>(course2load.keySet())) {
            Double load = course2load.get(course);
            Element courseEl = coursesEl.addElement("course");
            courseEl.addAttribute("name", course);
            if (load != null)
                courseEl.addAttribute("load", sDoubleFormat.format(load));
            Map<String, Integer> levels = course2levels.get(course);
            if (levels != null)
                for (String level: new TreeSet<String>(levels.keySet())) {
                    courseEl.addElement("skill").addAttribute("level", level).addAttribute("preference", levels.get(level).toString());
                }
        }
        Element assignmentsEl = root.addElement("assignments");
        for (TeachingRequest request: variables()) {
            Double load = course2load.get(request.getCourseName());
            Map<String, Integer> levels = course2levels.get(request.getCourseName());
            Element assignmentEl = assignmentsEl.addElement("assignment");
            if (request.getAssignmentId() != null) assignmentEl.addAttribute("id", String.valueOf(request.getAssignmentId()));
            if (request.getCourseName() != null && !request.getCourseName().isEmpty()) assignmentEl.addAttribute("course", request.getCourseName());
            if (load == null || !load.equals(request.getLoad()))
                assignmentEl.addAttribute("load", sDoubleFormat.format(request.getLoad()));
            if (request.getLink() != null) assignmentEl.addAttribute("link", request.getLink());
            if (levels == null || !levels.equals(request.getLevels()))
                for (String level: new TreeSet<String>(request.getLevels().keySet())) {
                    assignmentEl.addElement("skill").addAttribute("level", level).addAttribute("preference", request.getLevels().get(level).toString());
                }
            for (Section section: request.getSections()) {
                Element sectionEl = assignmentEl.addElement("section");
                if (section.getSectionId() != null) sectionEl.addAttribute("id", String.valueOf(section.getSectionId()));
                if (section.getSectionName() != null && !section.getSectionName().isEmpty()) sectionEl.addAttribute("name", section.getSectionName());
                if (section.hasTime()) {
                    TimeLocation tl = section.getTime();
                    Element timeEl = sectionEl.addElement("time");
                    timeEl.addAttribute("days", sDF7.format(Long.parseLong(Integer.toBinaryString(tl.getDayCode()))));
                    timeEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                    timeEl.addAttribute("length", String.valueOf(tl.getLength()));
                    if (tl.getBreakTime() != 0)
                        timeEl.addAttribute("breakTime", String.valueOf(tl.getBreakTime()));
                    if (tl.getTimePatternId() != null)
                        timeEl.addAttribute("pattern", tl.getTimePatternId().toString());
                    if (tl.getDatePatternId() != null) {
                        timeEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                        if (tl.getDatePatternName() != null && !tl.getDatePatternName().isEmpty())
                            timeEl.addAttribute("datePatternName", tl.getDatePatternName());
                        timeEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                    }
                    timeEl.setText(tl.getLongName(false));
                }
                if (section.hasRoom()) sectionEl.addAttribute("room", section.getRoom());
                if (section.isAllowOverlap()) sectionEl.addAttribute("canOverlap", "true");
            }
            if (saveBest && request.getBestAssignment() != null)
                assignmentEl.addElement("best").addAttribute("student", request.getBestAssignment().getStudent().getStudentId());
            if (saveInitial && request.getInitialAssignment() != null)
                assignmentEl.addElement("initial").addAttribute("student", request.getInitialAssignment().getStudent().getStudentId());
            if (saveSolution) {
                TeachingAssignment ta = assignment.getValue(request);
                if (ta != null) {
                    Element solutionEl = assignmentEl.addElement("solution");
                    solutionEl.addAttribute("student", ta.getStudent().getStudentId());
                    Integer level = request.getLevels().get(ta.getStudent().getLevel());
                    if (level != null)
                        solutionEl.addAttribute("level", String.valueOf(level));
                    int pref = ta.getStudent().getPreference(request);
                    if (pref >= 0)
                        solutionEl.addAttribute("preference", String.valueOf(1 + pref));
                    int btb = ta.getStudent().backToBack(assignment, ta);
                    if (btb != 0)
                        solutionEl.addAttribute("btb", String.valueOf(btb));
                    int share = ta.getStudent().share(assignment, ta);
                    if (share != 0)
                        solutionEl.addAttribute("share", String.valueOf(share));
                    int dl = ta.getStudent().diffLinks(assignment, ta);
                    if (dl != 0)
                        solutionEl.addAttribute("link", String.valueOf(dl));
                }
            }
        }
        Element studentsEl = root.addElement("students");
        for (Constraint<TeachingRequest, TeachingAssignment> c: constraints()) {
            if (c instanceof Student) {
                Student student = (Student)c;
                Element studentEl = studentsEl.addElement("student");
                studentEl.addAttribute("id", student.getStudentId());
                studentEl.addAttribute("name", student.getStudentName());
                studentEl.addAttribute("grad", student.isGrad() ? "true" : "false");
                if (student.getBackToBackPreference() != 0)
                    studentEl.addAttribute("btb", String.valueOf(student.getBackToBackPreference()));
                if (student.getLevel() != null && !student.getLevel().isEmpty())
                    studentEl.addAttribute("level", student.getLevel());
                studentEl.addAttribute("maxLoad", sDoubleFormat.format(student.getMaxLoad()));
                for (String pref: student.getPreferences())
                    studentEl.addElement("preference").addAttribute("value", pref);
                for (TimeLocation tl: student.getUnavailability()) {
                    Element timeEl = studentEl.addElement("unavailable");
                    timeEl.addAttribute("days", sDF7.format(Long.parseLong(Integer.toBinaryString(tl.getDayCode()))));
                    timeEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                    timeEl.addAttribute("length", String.valueOf(tl.getLength()));
                    if (tl.getBreakTime() != 0)
                        timeEl.addAttribute("breakTime", String.valueOf(tl.getBreakTime()));
                    if (tl.getTimePatternId() != null)
                        timeEl.addAttribute("pattern", tl.getTimePatternId().toString());
                    if (tl.getDatePatternId() != null) {
                        timeEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                        if (tl.getDatePatternName() != null && !tl.getDatePatternName().isEmpty())
                            timeEl.addAttribute("datePatternName", tl.getDatePatternName());
                        timeEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                    }
                    timeEl.setText(tl.getLongName(false));
                }
            }
        }
        return document;
    }
    
    public boolean load(Document document, Assignment<TeachingRequest, TeachingAssignment> assignment) {
        boolean loadInitial = getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        boolean loadBest = getProperties().getPropertyBoolean("Xml.LoadBest", true);
        boolean loadSolution = getProperties().getPropertyBoolean("Xml.LoadSolution", true);
        Element root = document.getRootElement();
        if (!"ta".equals(root.getName()))
            return false;
        Map<String, Double> course2load = new HashMap<String, Double>();
        Map<String, Map<String, Integer>> course2levels = new HashMap<String, Map<String, Integer>>();
        if (root.element("courses") != null) {
            for (Iterator<?> i = root.element("courses").elementIterator("course"); i.hasNext();) {
                Element e = (Element) i.next();
                String course = e.attributeValue("name");
                String load = e.attributeValue("load"); 
                if (load != null)
                    course2load.put(course, Double.parseDouble(load));
                Map<String, Integer> levels = new HashMap<String, Integer>();
                for (Iterator<?> j = e.elementIterator("skill"); j.hasNext();) {
                    Element f = (Element) j.next();
                    levels.put(f.attributeValue("level"), Integer.parseInt(f.attributeValue("preference")));
                }
                course2levels.put(course, levels);
            }
        }

        Map<String, Student> students = new HashMap<String, Student>();
        double studentMaxLoad = 0.0;
        for (Iterator<?> i = root.element("students").elementIterator("student"); i.hasNext();) {
            Element e = (Element) i.next();
            List<String> preferences = new ArrayList<String>();
            for (Iterator<?> j = e.elementIterator("preference"); j.hasNext();) {
                Element f = (Element) j.next();
                preferences.add(f.attributeValue("value"));
            }
            Student student = new Student(e.attributeValue("id"), e.attributeValue("name"), preferences,
                    "true".equalsIgnoreCase(e.attributeValue("grad", "true")),
                    Integer.valueOf(e.attributeValue("btb", "1")),
                    Double.valueOf(e.attributeValue("maxLoad", "20")),
                    e.attributeValue("level"));
            for (Iterator<?> j = e.elementIterator("unavailable"); j.hasNext();) {
                Element f = (Element) j.next();
                TimeLocation time = new TimeLocation(
                        Integer.parseInt(f.attributeValue("days"), 2),
                        Integer.parseInt(f.attributeValue("start")),
                        Integer.parseInt(f.attributeValue("length")), 0, 0,
                        f.attributeValue("datePattern") == null ? null : Long.valueOf(f.attributeValue("datePattern")),
                        f.attributeValue("datePatternName", ""),
                        createBitSet(f.attributeValue("dates")),
                        Integer.parseInt(f.attributeValue("breakTime", "0")));
                if (f.attributeValue("pattern") != null)
                    time.setTimePatternId(Long.valueOf(f.attributeValue("pattern")));
                student.setNotAvailable(time);
            }
            addConstraint(student);
            students.put(student.getStudentId(), student);
            studentMaxLoad += student.getMaxLoad();
        }

        Map<Long, List<TeachingRequest>> id2classes = new HashMap<Long, List<TeachingRequest>>();
        Map<TeachingRequest, Student> best = new HashMap<TeachingRequest, Student>();
        Map<TeachingRequest, Student> initial = new HashMap<TeachingRequest, Student>();
        Map<TeachingRequest, Student> current = new HashMap<TeachingRequest, Student>();
        for (Iterator<?> i = root.element("assignments").elementIterator("assignment"); i.hasNext();) {
            Element e = (Element) i.next();
            List<Section> sections = new ArrayList<Section>();
            for (Iterator<?> j = e.elementIterator("section"); j.hasNext();) {
                Element f = (Element) j.next();
                TimeLocation time = null;
                Element timeEl = f.element("time");
                if (timeEl != null) {
                    time = new TimeLocation(
                            Integer.parseInt(timeEl.attributeValue("days"), 2),
                            Integer.parseInt(timeEl.attributeValue("start")),
                            Integer.parseInt(timeEl.attributeValue("length")), 0, 0,
                            timeEl.attributeValue("datePattern") == null ? null : Long.valueOf(timeEl.attributeValue("datePattern")),
                            timeEl.attributeValue("datePatternName", ""),
                            createBitSet(timeEl.attributeValue("dates")),
                            Integer.parseInt(timeEl.attributeValue("breakTime", "0")));
                    if (timeEl.attributeValue("pattern") != null)
                        time.setTimePatternId(Long.valueOf(timeEl.attributeValue("pattern")));
                }
                Section section = new Section(
                        (f.attributeValue("id") == null ? null : Long.valueOf(f.attributeValue("id"))),
                        f.attributeValue("name"),
                        time,
                        f.attributeValue("room"),
                        "true".contentEquals(f.attributeValue("canOverlap", "false")));
                sections.add(section);
            }
            TeachingRequest tr = new TeachingRequest(
                    (e.attributeValue("id") == null ? null : Long.valueOf(e.attributeValue("id"))),
                    e.attributeValue("course"),
                    sections,
                    e.attributeValue("link"));
            Double load = course2load.get(tr.getCourseName());
            if (e.attributeValue("load") != null)
                load = Double.parseDouble(e.attributeValue("load"));
            if (load != null)
                tr.setLoad(load);
            Map<String, Integer> levels = null;
            for (Iterator<?> j = e.elementIterator("skill"); j.hasNext();) {
                Element f = (Element) j.next();
                if (levels == null)
                    levels = new HashMap<String, Integer>();
                levels.put(f.attributeValue("level"), Integer.parseInt(f.attributeValue("preference")));
            }
            if (levels == null)
                levels = course2levels.get(tr.getCourseName());
            if (levels != null)
                tr.getLevels().putAll(levels);
            if (loadBest && e.element("best") != null)
                best.put(tr, students.get(e.element("best").attributeValue("student")));
            if (loadInitial && e.element("initial") != null)
                initial.put(tr, students.get(e.element("initial").attributeValue("student")));
            if (loadSolution && e.element("solution") != null)
                current.put(tr, students.get(e.element("solution").attributeValue("student")));
            addVariable(tr);
            if (tr.getAssignmentId() != null) {
                List<TeachingRequest> classes = id2classes.get(tr.getAssignmentId());
                if (classes == null) {
                    classes = new ArrayList<TeachingRequest>();
                    id2classes.put(tr.getAssignmentId(), classes);
                }
                classes.add(tr);
            }
        }

        for (Map.Entry<Long, List<TeachingRequest>> e : id2classes.entrySet()) {
            Long id = e.getKey();
            List<TeachingRequest> classes = e.getValue();
            if (classes.size() > 1) {
                SameAssignment sa = new SameAssignment(id);
                for (TeachingRequest c : classes)
                    sa.addVariable(c);
                addConstraint(sa);
            }
        }

        for (TeachingRequest clazz : variables()) {
            sLog.info("Added class " + clazz);
            if (clazz.getAssignmentId() >= 0 && clazz.getLevels().isEmpty()) {
                sLog.error("No level: " + clazz);
                clazz.getLevels().put(getProperties().getProperty("TA.DefaultLevelCode", "XXX"), 1);
            }
            if (clazz.getAssignmentId() >= 0 && clazz.getLoad() == 0.0) {
                sLog.error("No load: " + clazz);
                clazz.setLoad(getProperties().getPropertyDouble("TA.DefaultLoad", 10.0));
            }
        }
        
        for (Constraint<TeachingRequest, TeachingAssignment> constraint: constraints()) {
            if (constraint instanceof Student) {
                Student student = (Student)constraint;
                for (TeachingRequest request : variables()) {
                    if (student.canTeach(request))
                        student.addVariable(request);
                }
                if (student.getMaxLoad() > 0 && !student.variables().isEmpty()) {
                    sLog.info("Added student " + student.getContext(assignment).toString());
                    int nrClasses = 0;
                    for (TeachingRequest c : student.variables()) {
                        if (c.getId() >= 0) {
                            sLog.info("  -- " + c);
                            nrClasses++;
                        }
                    }
                    if (nrClasses == 0) {
                        sLog.info("  -- no courses available");
                    }
                } else {
                    sLog.info("Ignoring student " + student);
                    if (student.getMaxLoad() == 0)
                        sLog.info("  -- zero max load");
                    else
                        sLog.info("  -- no courses available");
                }
            }
        }
        
        for (Map.Entry<TeachingRequest, Student> entry: best.entrySet())
            entry.getKey().setBestAssignment(new TeachingAssignment(entry.getKey(), entry.getValue()), 0l);

        for (Map.Entry<TeachingRequest, Student> entry: initial.entrySet())
            entry.getKey().setInitialAssignment(new TeachingAssignment(entry.getKey(), entry.getValue()));
        
        if (!current.isEmpty()) {
            for (Map.Entry<TeachingRequest, Student> entry: current.entrySet()) {
                TeachingRequest request = entry.getKey();
                TeachingAssignment ta = new TeachingAssignment(request, entry.getValue());
                Set<TeachingAssignment> conf = conflictValues(assignment, ta);
                if (conf.isEmpty()) {
                    assignment.assign(0, ta);
                } else {
                    sLog.error("Unable to assign " + ta.getName() + " to " + request.getName());
                    sLog.error("Conflicts:" + ToolBox.dict2string(conflictConstraints(assignment, ta), 2));
                }
            }
        }

        double totalLoad = 0.0;
        for (TeachingRequest clazz : variables()) {
            if (clazz.values(getEmptyAssignment()).isEmpty())
                sLog.error("No values: " + clazz);
            totalLoad += clazz.getLoad();
        }

        Map<String, Double> studentLevel2load = new HashMap<String, Double>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : constraints()) {
            if (c instanceof Student) {
                Student s = (Student) c;
                String level = (s.getLevel() == null ? "null" : s.getLevel());
                Double load = studentLevel2load.get(level);
                studentLevel2load.put(level, s.getMaxLoad() + (load == null ? 0.0 : load));
            }
        }
        sLog.info("Student max loads: (total: " + sDoubleFormat.format(studentMaxLoad) + ")");
        for (String level : new TreeSet<String>(studentLevel2load.keySet())) {
            Double load = studentLevel2load.get(level);
            sLog.info("  " + level + ": " + sDoubleFormat.format(load));
        }
        Map<String, Double> clazzLevel2load = new HashMap<String, Double>();
        for (TeachingRequest clazz : variables()) {
            String level = null;
            for (String l : new TreeSet<String>(clazz.getLevels().keySet())) {
                level = (level == null ? "" : level + ",") + l;
            }
            if (level == null)
                level = "null";
            if (clazz.getId() < 0)
                level = clazz.getName();
            Double load = clazzLevel2load.get(level);
            clazzLevel2load.put(level, clazz.getLoad() + (load == null ? 0.0 : load));
        }
        sLog.info("Class loads: (total: " + sDoubleFormat.format(totalLoad) + ")");
        for (String level : new TreeSet<String>(clazzLevel2load.keySet())) {
            Double load = clazzLevel2load.get(level);
            sLog.info("  " + level + ": " + sDoubleFormat.format(load));
        }
        
        return true;
    }
    
    /** Convert bitset to a bit string */
    private static String bitset2string(BitSet b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length(); i++)
            sb.append(b.get(i) ? "1" : "0");
        return sb.toString();
    }

    /** Create BitSet from a bit string */
    private static BitSet createBitSet(String bitString) {
        if (bitString == null) return null;
        BitSet ret = new BitSet(bitString.length());
        for (int i = 0; i < bitString.length(); i++)
            if (bitString.charAt(i) == '1')
                ret.set(i);
        return ret;
    }
}
