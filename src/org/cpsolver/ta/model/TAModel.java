package org.cpsolver.ta.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ta.constraints.SameAssignment;
import org.cpsolver.ta.constraints.Student;
import org.cpsolver.ta.criteria.BackToBack;
import org.cpsolver.ta.criteria.DiffLink;
import org.cpsolver.ta.criteria.Graduate;
import org.cpsolver.ta.criteria.LevelCode;
import org.cpsolver.ta.criteria.Preference;
import org.cpsolver.ta.criteria.TimeOverlaps;

public class TAModel extends Model<TeachingRequest, TeachingAssignment> {
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
            String name = fields[1] + " " + fields[2];
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
            TeachingRequest clazz = new TeachingRequest(id, name, dayCode, startSlot, length, room, link);
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
            System.out.println("Added class " + clazz);
            if (clazz.getAssignmentId() >= 0 && clazz.getLevels().isEmpty()) {
                System.err.println("No level: " + clazz);
                clazz.getLevels().put(getProperties().getProperty("TA.DefaultLevelCode", "XXX"), 1);
            }
            if (clazz.getAssignmentId() >= 0 && clazz.getLoad() == 0.0) {
                System.err.println("No load: " + clazz);
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
                System.err.println("Student " + id + " is two or more times in the file.");
            }
            boolean[] av = new boolean[50];
            for (int i = 0; i < 50; i++)
                av[i] = "1".equals(fields[idx++]);
            List<String> prefs = new ArrayList<String>();
            for (int i = 0; i < 3; i++) {
                String p = fields[idx++].replace("Large lecture", "LEC").replace("Lecture", "LEC").replace("Recitation",
                        "REC");
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
            Student student = new Student(id, av, prefs, grad, b2b, maxLoad, level);
            for (TeachingRequest clazz : variables()) {
                if (student.canTeach(clazz))
                    student.addVariable(clazz);
            }
            if (student.getMaxLoad() > 0 && !student.variables().isEmpty()) {
                addConstraint(student);
                System.out.println("Added student " + student);
                int nrClasses = 0;
                for (TeachingRequest c : student.variables()) {
                    if (c.getId() >= 0) {
                        System.out.println("  -- " + c);
                        nrClasses++;
                    }
                }
                if (nrClasses == 0) {
                    System.out.println("  -- no courses available");
                }
                studentMaxLoad += student.getMaxLoad();
            } else {
                System.out.println("Ignoring student " + student);
                if (student.getMaxLoad() == 0)
                    System.out.println("  -- zero max load");
                else
                    System.out.println("  -- no courses available");
            }
        }

        double totalLoad = 0.0;
        for (TeachingRequest clazz : variables()) {
            if (clazz.values(getEmptyAssignment()).isEmpty())
                System.err.println("No values: " + clazz);
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
        System.out.println("Student max loads: (total: " + sDoubleFormat.format(studentMaxLoad) + ")");
        for (String level : new TreeSet<String>(studentLevel2load.keySet())) {
            Double load = studentLevel2load.get(level);
            System.out.println("  " + level + ": " + sDoubleFormat.format(load));
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
        System.out.println("Class loads: (total: " + sDoubleFormat.format(totalLoad) + ")");
        for (String level : new TreeSet<String>(clazzLevel2load.keySet())) {
            Double load = clazzLevel2load.get(level);
            System.out.println("  " + level + ": " + sDoubleFormat.format(load));
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
        out.println("Assignment Id,Course (IType),Time,Room,Link,Level,Load,Student");
        for (TeachingRequest v : variables()) {
            out.println(v.toString() + "," + (assignment.getValue(v) == null ? "" : assignment.getValue(v).getStudent().getStudentId()));
        }
        out.flush();
        out.close();

        out = new PrintWriter(new File(dir, "solution-students.csv"));
        out.println("Student,Availability,1st Preference,2nd Preference,3rd Preference,Graduate,Back-To-Back,Level,Assigned Load,Avg Level,Avg Preference,Back-To-Back,Diff Links,Time Overlaps,1st Assignment,2nd Assignment, 3rd Assignment");
        for (Constraint<TeachingRequest, TeachingAssignment> constraint : constraints()) {
            if (constraint instanceof Student) {
                Student s = (Student) constraint;
                out.println(s.getContext(assignment).toString());
            }
        }
        out.flush();
        out.close();
    }

}
