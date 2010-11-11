package net.sf.cpsolver.ifs.example.tt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Simple Timetabling Problem. <br>
 * <br>
 * The problem is modelled in such a way that every lecture was represented by a
 * variable, resource as a constraint and every possible location of an activity
 * in the time and space was represented by a single value. It means that a
 * value stands for a selection of the time (starting time slot), and one of the
 * available rooms. Binary dependencies are of course represented as constraints
 * as well.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class TimetableModel extends Model<Activity, Location> {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(TimetableModel.class);
    private int iNrDays, iNrHours;

    public TimetableModel(int nrDays, int nrHours) {
        super();
        iNrDays = nrDays;
        iNrHours = nrHours;
    }

    public int getNrDays() {
        return iNrDays;
    }

    public int getNrHours() {
        return iNrHours;
    }

    @SuppressWarnings("unchecked")
    public static TimetableModel generate(DataProperties cfg) {
        int nrDays = cfg.getPropertyInt("Generator.NrDays", 5);
        int nrHours = cfg.getPropertyInt("Generator.NrHours", 20);
        int nrSlots = nrDays * nrHours;
        TimetableModel m = new TimetableModel(nrDays, nrHours);

        int nrRooms = cfg.getPropertyInt("Generator.NrRooms", 20);
        int nrInstructors = cfg.getPropertyInt("Generator.NrInstructors", 20);
        int nrClasses = cfg.getPropertyInt("Generator.NrClasses", 20);
        int nrGroupsOfRooms = cfg.getPropertyInt("Generator.NrGroupsOfRooms", 20);
        int nrRoomsInGroupMin = cfg.getPropertyInt("Generator.NrRoomsInGroupMin", 1);
        int nrRoomsInGroupMax = cfg.getPropertyInt("Generator.NrRoomsInGroupMax", 10);
        int nrRoomInGroupMin = cfg.getPropertyInt("Generator.NrRoomInGroupMin", 1);
        double fillFactor = cfg.getPropertyDouble("Generator.FillFactor", 0.8);
        int maxLength = cfg.getPropertyInt("Generator.ActivityLengthMax", 5);
        double hardFreeResource = cfg.getPropertyDouble("Generator.HardFreeResource", 0.05);
        double softFreeResource = cfg.getPropertyDouble("Generator.SoftFreeResource", 0.3);
        double softUsedResource = cfg.getPropertyDouble("Generator.SoftUsedResource", 0.05);
        double softUsedActivity = cfg.getPropertyDouble("Generator.SoftUsedActivity", 0.05);
        double softFreeActivity = cfg.getPropertyDouble("Generator.SoftFreeActivity", 0.3);
        double hardFreeActivity = cfg.getPropertyDouble("Generator.HardFreeActivity", 0.05);
        int nrDependencies = cfg.getPropertyInt("Generator.NrDependencies", 50);

        Resource rooms[] = new Resource[nrRooms];
        ArrayList<ArrayList<Resource>> groupForRoom[] = new ArrayList[nrRooms];
        for (int i = 0; i < nrRooms; i++) {
            rooms[i] = new Resource("r" + (i + 1), Resource.TYPE_ROOM, "Room " + (i + 1));
            groupForRoom[i] = new ArrayList<ArrayList<Resource>>();
            m.addConstraint(rooms[i]);
        }
        ArrayList<Resource> groupOfRooms[] = new ArrayList[nrGroupsOfRooms];
        for (int i = 0; i < nrGroupsOfRooms; i++) {
            groupOfRooms[i] = new ArrayList<Resource>();
            for (int j = 0; j < ToolBox.random(1 + nrRoomsInGroupMax - nrRoomsInGroupMin) + nrRoomsInGroupMin; j++) {
                int r = 0;
                do {
                    r = ToolBox.random(nrRooms);
                } while (groupOfRooms[i].contains(rooms[r]));
                groupOfRooms[i].add(rooms[r]);
                groupForRoom[r].add(groupOfRooms[i]);
            }
        }
        for (int i = 0; i < nrRooms; i++) {
            int cnt = 0;
            for (int j = 0; j < nrGroupsOfRooms; j++)
                if (groupOfRooms[j].contains(rooms[i]))
                    cnt++;
            while (cnt < nrRoomInGroupMin) {
                int r = 0;
                do {
                    r = ToolBox.random(nrGroupsOfRooms);
                } while (groupOfRooms[r].contains(rooms[i]));
                groupOfRooms[r].add(rooms[i]);
                groupForRoom[i].add(groupOfRooms[r]);
                cnt++;
            }
        }
        Resource instructors[] = new Resource[nrInstructors];
        for (int i = 0; i < nrInstructors; i++) {
            instructors[i] = new Resource("t" + (i + 1), Resource.TYPE_INSTRUCTOR, "Teacher " + (i + 1));
            m.addConstraint(instructors[i]);
        }
        Resource classes[] = new Resource[nrClasses];
        for (int i = 0; i < nrClasses; i++) {
            classes[i] = new Resource("c" + (i + 1), Resource.TYPE_CLASS, "Class " + (i + 1));
            m.addConstraint(classes[i]);
        }

        int[][] timetable4room = new int[nrRooms][nrSlots];
        int[][] timetable4instr = new int[nrInstructors][nrSlots];
        int[][] timetable4class = new int[nrClasses][nrSlots];
        int act = 0;
        for (int i = 0; i < timetable4room.length; i++)
            for (int j = 0; j < timetable4room[i].length; j++)
                timetable4room[i][j] = 0;
        for (int i = 0; i < timetable4instr.length; i++)
            for (int j = 0; j < timetable4instr[i].length; j++)
                timetable4instr[i][j] = 0;
        for (int i = 0; i < timetable4class.length; i++)
            for (int j = 0; j < timetable4class[i].length; j++)
                timetable4class[i][j] = 0;

        int totalSlots = nrRooms * nrSlots;
        int usedSlots = 0;
        ArrayList<Integer> starts = new ArrayList<Integer>();
        ArrayList<Integer> arooms = new ArrayList<Integer>();
        while ((((double) usedSlots / ((double) totalSlots))) < fillFactor) {
            int attempt = 0;
            int slot = ToolBox.random(nrSlots);
            int room = ToolBox.random(nrRooms);
            while (attempt < 500 && timetable4room[room][slot] != 0) {
                slot = ToolBox.random(nrSlots);
                room = ToolBox.random(nrRooms);
            }
            if (attempt == 500) {
                int s = slot;
                int r = room;
                while (timetable4room[r][s] != 0) {
                    r++;
                    if (r == nrRooms)
                        r = 0;
                    if (r == room)
                        s++;
                    if (s == nrSlots)
                        s = 0;
                }
                slot = s;
                room = r;
            }
            int length = maxLength;// ToolBox.random(maxLength)+1;
            int aclass = ToolBox.random(nrClasses);
            int instr = ToolBox.random(nrInstructors);
            attempt = 0;
            while (attempt < 500 && (timetable4class[aclass][slot] != 0 || timetable4instr[instr][slot] != 0)) {
                aclass = ToolBox.random(nrClasses);
                instr = ToolBox.random(nrInstructors);
            }
            if (attempt == 500)
                continue;
            int len = 1;
            while (len < length) {
                if ((((slot + len) % nrHours) != 0) && timetable4room[room][slot + len] == 0
                        && timetable4instr[instr][slot + len] == 0 && timetable4class[aclass][slot + len] == 0)
                    len++;
                else
                    break;
            }
            ArrayList<Resource> roomGr = ToolBox.random(groupForRoom[room]);
            act++;
            usedSlots += len;
            Activity a = new Activity(len, "a" + act, "Activity " + act);
            a.addResourceGroup(roomGr);
            a.addResourceGroup(instructors[instr]);
            a.addResourceGroup(classes[aclass]);
            m.addVariable(a);
            starts.add(slot);
            arooms.add(room);
            for (int i = slot; i < slot + len; i++) {
                timetable4room[room][i] = act;
                timetable4instr[instr][i] = act;
                timetable4class[aclass][i] = act;
            }
        }
        int nrHardFreeRes = 0;
        int nrSoftFreeRes = 0;
        int nrSoftUsedRes = 0;
        for (int slot = 0; slot < nrSlots; slot++) {
            for (int room = 0; room < nrRooms; room++) {
                if (timetable4room[room][slot] == 0) {
                    if (ToolBox.random() < hardFreeResource) {
                        nrHardFreeRes++;
                        rooms[room].addProhibitedSlot(slot);
                    } else if (ToolBox.random() < softFreeResource / (1.0 - hardFreeResource)) {
                        nrSoftFreeRes++;
                        rooms[room].addDiscouragedSlot(slot);
                    }
                } else if (ToolBox.random() < softUsedResource) {
                    nrSoftUsedRes++;
                    rooms[room].addDiscouragedSlot(slot);
                }
            }
            for (int instr = 0; instr < nrInstructors; instr++) {
                if (timetable4instr[instr][slot] == 0) {
                    if (ToolBox.random() < hardFreeResource) {
                        nrHardFreeRes++;
                        instructors[instr].addProhibitedSlot(slot);
                    } else if (ToolBox.random() < softFreeResource / (1.0 - hardFreeResource)) {
                        nrSoftFreeRes++;
                        instructors[instr].addDiscouragedSlot(slot);
                    }
                } else if (ToolBox.random() < softUsedResource) {
                    nrSoftUsedRes++;
                    instructors[instr].addDiscouragedSlot(slot);
                }
            }
            for (int aclass = 0; aclass < nrClasses; aclass++) {
                if (timetable4class[aclass][slot] == 0) {
                    if (ToolBox.random() < hardFreeResource) {
                        nrHardFreeRes++;
                        classes[aclass].addProhibitedSlot(slot);
                    } else if (ToolBox.random() < softFreeResource / (1.0 - hardFreeResource)) {
                        nrSoftFreeRes++;
                        classes[aclass].addDiscouragedSlot(slot);
                    }
                } else if (ToolBox.random() < softUsedResource) {
                    nrSoftUsedRes++;
                    classes[aclass].addDiscouragedSlot(slot);
                }
            }
        }
        int nrSoftFreeAct = 0;
        int nrSoftUsedAct = 0;
        int nrHardFreeAct = 0;
        for (int i = 0; i < m.variables().size(); i++) {
            Activity activity = m.variables().get(i);
            for (int slot = 0; slot < nrSlots; slot++) {
                int start = starts.get(i);
                if (slot < start || slot >= start + activity.getLength()) {
                    if (ToolBox.random() < hardFreeActivity) {
                        nrHardFreeAct++;
                        activity.addProhibitedSlot(slot);
                    } else if (ToolBox.random() < (softFreeActivity / (1.0 - hardFreeActivity))) {
                        nrSoftFreeAct++;
                        activity.addDiscouragedSlot(slot);
                    }
                } else {
                    if (ToolBox.random() < softUsedActivity) {
                        nrSoftUsedAct++;
                        activity.addDiscouragedSlot(slot);
                    }
                }
            }
            activity.init();
        }
        for (int i = 0; i < nrDependencies;) {
            int ac1 = ToolBox.random(m.variables().size());
            int ac2 = ToolBox.random(m.variables().size());
            while (ac1 == ac2) {
                ac2 = ToolBox.random(m.variables().size());
            }
            int s1 = starts.get(ac1);
            int s2 = starts.get(ac2);
            Activity a1 = m.variables().get(ac1);
            Activity a2 = m.variables().get(ac2);
            Dependence dep = null;
            if (s1 < s2) {
                if (s1 + a1.getLength() == s2)
                    dep = new Dependence("d" + (i + 1), Dependence.TYPE_CLOSELY_BEFORE);
                else if (s1 + a1.getLength() < s2)
                    dep = new Dependence("d" + (i + 1), Dependence.TYPE_BEFORE);
            } else {
                if (s2 == s1 + a1.getLength())
                    dep = new Dependence("d" + (i + 1), Dependence.TYPE_CLOSELY_AFTER);
                else if (s2 > s1 + a1.getLength())
                    dep = new Dependence("d" + (i + 1), Dependence.TYPE_AFTER);
            }
            if (dep != null) {
                dep.addVariable(a1);
                dep.addVariable(a2);
                m.addConstraint(dep);
                i++;
            }
        }
        for (int i = 0; i < m.variables().size(); i++) {
            Activity activity = m.variables().get(i);
            // sLogger.debug("-- processing activity "+activity.getName());
            int start = starts.get(i);
            int room = arooms.get(i);
            Location location = null;
            for (Location l : activity.values()) {
                if (l.getSlot() == start && l.getResource(0).getResourceId().equals("r" + (room + 1))) {
                    location = l;
                    break;
                }
            }
            if (location != null) {
                Set<Location> conflicts = m.conflictValues(location);
                if (!conflicts.isEmpty()) {
                    sLogger.warn("Unable to assign " + location.getName() + " to " + activity.getName() + ", reason:");
                    for (Constraint<Activity, Location> c : activity.constraints()) {
                        Set<Location> cc = new HashSet<Location>();
                        c.computeConflicts(location, cc);
                        if (!cc.isEmpty())
                            sLogger.warn("  -- Constraint " + c.getName() + " causes conflicts " + cc);
                    }
                } else {
                    activity.assign(0, location);
                    activity.setInitialAssignment(location);
                }
                // sLogger.debug("  -- location "+location.getName()+" found");
                activity.setInitialAssignment(location);
            } else {
                sLogger.warn("Unable to assign " + activity.getName() + " -- no location matching slot=" + start
                        + " room='R" + (room + 1) + "'");
            }
        }
        if (!cfg.getPropertyBoolean("General.InitialAssignment", true)) {
            for (int i = 0; i < m.variables().size(); i++) {
                Activity activity = m.variables().get(i);
                activity.unassign(0);
            }
        }

        int forcedPerturbances = cfg.getPropertyInt("General.ForcedPerturbances", 0);
        if (forcedPerturbances > 0) {
            List<Activity> initialVariables = new ArrayList<Activity>();
            for (Activity v : m.variables()) {
                if (v.getInitialAssignment() != null)
                    initialVariables.add(v);
            }
            for (int i = 0; i < forcedPerturbances; i++) {
                if (initialVariables.isEmpty())
                    break;
                Activity var = ToolBox.random(initialVariables);
                initialVariables.remove(var);
                var.removeInitialValue();
            }
        }

        sLogger.debug("-- Generator Info ---------------------------------------------------------");
        sLogger.debug("  Total number of " + m.variables().size() + " activities generated.");
        sLogger.debug("  Total number of " + usedSlots + " slots are filled (" + ((100.0 * usedSlots) / totalSlots)
                + "% filled).");
        sLogger.debug("  Average length of an activity is " + (((double) usedSlots) / m.variables().size()));
        sLogger.debug("  Total number of hard constraints posted on free slots on activities: " + nrHardFreeAct);
        sLogger.debug("  Total number of soft constraints posted on free slots on activities: " + nrSoftFreeAct);
        sLogger.debug("  Total number of soft constraints posted on used slots on activities: " + nrSoftUsedAct);
        sLogger.debug("  Total number of hard constraints posted on free slots on resources: " + nrHardFreeRes);
        sLogger.debug("  Total number of soft constraints posted on free slots on resources: " + nrSoftFreeRes);
        sLogger.debug("  Total number of soft constraints posted on used slots on resources: " + nrSoftUsedRes);
        sLogger.debug("  Total number of " + nrDependencies + " dependencies generated.");
        sLogger.debug("---------------------------------------------------------------------------");

        return m;
    }

    public static void main(String[] args) {
        org.apache.log4j.BasicConfigurator.configure();
        TimetableModel model = generate(new DataProperties());
        System.out.println(model.getInfo());
    }

    public void saveAsXML(DataProperties cfg, boolean gen, Solution<Activity, Location> solution, File outFile)
            throws IOException {
        outFile.getParentFile().mkdirs();
        sLogger.debug("Writting XML data to:" + outFile);

        Document document = DocumentHelper.createDocument();
        document.addComment("Interactive Timetabling - University Timetable Generator (version 2.0)");

        if (!assignedVariables().isEmpty()) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (solution == null ? getInfo() : solution.getInfo());
            for (String key : new TreeSet<String>(solutionInfo.keySet())) {
                String value = solutionInfo.get(key);
                comments.append("    " + key + ": " + value + "\n");
            }
            document.addComment(comments.toString());
        }

        Element root = document.addElement("Timetable");
        if (gen) {
            Element generator = root.addElement("Generator");
            generator.addAttribute("version", "2.0");
            generator.addElement("DaysPerWeek").setText(String.valueOf(iNrDays));
            generator.addElement("SlotsPerDay").setText(String.valueOf(iNrHours));
            generator.addElement("NrRooms").setText(cfg.getProperty("Generator.NrRooms", "20"));
            generator.addElement("NrInstructors").setText(cfg.getProperty("Generator.NrInstructors", "20"));
            generator.addElement("NrClasses").setText(cfg.getProperty("Generator.NrClasses", "20"));
            generator.addElement("FillFactor").setText(cfg.getProperty("Generator.FillFactor", "0.8"));
            generator.addElement("ActivityLengthMax").setText(cfg.getProperty("Generator.ActivityLengthMax", "5"));
            generator.addElement("NrGroupsOfRooms").setText(cfg.getProperty("Generator.NrGroupsOfRooms", "20"));
            generator.addElement("NrRoomsInGroupMin").setText(cfg.getProperty("Generator.NrRoomsInGroupMin", "1"));
            generator.addElement("NrRoomsInGroupMax").setText(cfg.getProperty("Generator.NrRoomsInGroupMax", "10"));
            generator.addElement("NrRoomInGroupMin").setText(cfg.getProperty("Generator.NrRoomInGroupMin", "1"));
            generator.addElement("HardFreeResource").setText(cfg.getProperty("Generator.HardFreeResource", "0.05"));
            generator.addElement("SoftFreeResource").setText(cfg.getProperty("Generator.SoftFreeResource", "0.3"));
            generator.addElement("SoftUsedResource").setText(cfg.getProperty("Generator.SoftUsedResource", "0.05"));
            generator.addElement("SoftUsedActivity").setText(cfg.getProperty("Generator.SoftUsedActivity", "0.05"));
            generator.addElement("SoftFreeActivity").setText(cfg.getProperty("Generator.SoftFreeActivity", "0.3"));
            generator.addElement("HardFreeActivity").setText(cfg.getProperty("Generator.HardFreeActivity", "0.05"));
            generator.addElement("NrDependencies").setText(cfg.getProperty("Generator.NrDependencies", "50"));
        }

        ArrayList<Resource> rooms = new ArrayList<Resource>();
        ArrayList<Resource> classes = new ArrayList<Resource>();
        ArrayList<Resource> instructors = new ArrayList<Resource>();
        ArrayList<Resource> specials = new ArrayList<Resource>();
        ArrayList<Dependence> dependencies = new ArrayList<Dependence>();

        for (Constraint<Activity, Location> c : constraints()) {
            if (c instanceof Resource) {
                Resource r = (Resource) c;
                switch (r.getType()) {
                    case Resource.TYPE_ROOM:
                        rooms.add(r);
                        break;
                    case Resource.TYPE_CLASS:
                        classes.add(r);
                        break;
                    case Resource.TYPE_INSTRUCTOR:
                        instructors.add(r);
                        break;
                    default:
                        specials.add(r);
                }
            } else if (c instanceof Dependence) {
                dependencies.add((Dependence) c);
            }
        }

        Element problem = root.addElement("Problem");
        problem.addAttribute("version", "2.0");
        Element problemGen = problem.addElement("General");
        problemGen.addElement("DaysPerWeek").setText(String.valueOf(iNrDays));
        problemGen.addElement("SlotsPerDay").setText(String.valueOf(iNrHours));
        Element resourceGen = problemGen.addElement("Resources");
        resourceGen.addElement("Classrooms").setText(String.valueOf(rooms.size()));
        resourceGen.addElement("Teachers").setText(String.valueOf(instructors.size()));
        resourceGen.addElement("Classes").setText(String.valueOf(classes.size()));
        resourceGen.addElement("Special").setText(String.valueOf(specials.size()));
        problemGen.addElement("Activities").setText(String.valueOf(variables().size()));
        problemGen.addElement("Dependences").setText(String.valueOf(dependencies.size()));

        Element resources = problem.addElement("Resources");

        Element resEl = resources.addElement("Classrooms");
        for (Resource r : rooms) {
            Element el = resEl.addElement("Resource");
            el.addAttribute("id", r.getResourceId());
            el.addElement("Name").setText(r.getName());
            Element pref = el.addElement("TimePreferences");
            for (Integer slot : new TreeSet<Integer>(r.getDiscouragedSlots()))
                pref.addElement("Soft").setText(slot.toString());
            for (Integer slot : new TreeSet<Integer>(r.getProhibitedSlots()))
                pref.addElement("Hard").setText(slot.toString());
        }

        resEl = resources.addElement("Teachers");
        for (Resource r : instructors) {
            Element el = resEl.addElement("Resource");
            el.addAttribute("id", r.getResourceId());
            el.addElement("Name").setText(r.getName());
            Element pref = el.addElement("TimePreferences");
            for (Integer slot : new TreeSet<Integer>(r.getDiscouragedSlots()))
                pref.addElement("Soft").setText(slot.toString());
            for (Integer slot : new TreeSet<Integer>(r.getProhibitedSlots()))
                pref.addElement("Hard").setText(slot.toString());
        }

        resEl = resources.addElement("Classes");
        for (Resource r : classes) {
            Element el = resEl.addElement("Resource");
            el.addAttribute("id", r.getResourceId());
            el.addElement("Name").setText(r.getName());
            Element pref = el.addElement("TimePreferences");
            for (Integer slot : new TreeSet<Integer>(r.getDiscouragedSlots()))
                pref.addElement("Soft").setText(slot.toString());
            for (Integer slot : new TreeSet<Integer>(r.getProhibitedSlots()))
                pref.addElement("Hard").setText(slot.toString());
        }

        resEl = resources.addElement("Special");
        for (Resource r : specials) {
            Element el = resEl.addElement("Resource");
            el.addAttribute("id", r.getResourceId());
            el.addElement("Name").setText(r.getName());
            Element pref = el.addElement("TimePreferences");
            for (Integer slot : new TreeSet<Integer>(r.getDiscouragedSlots()))
                pref.addElement("Soft").setText(slot.toString());
            for (Integer slot : new TreeSet<Integer>(r.getProhibitedSlots()))
                pref.addElement("Hard").setText(slot.toString());
        }

        boolean hasSolution = false;
        Element actEl = problem.addElement("Activities");
        for (Activity a : variables()) {
            Element el = actEl.addElement("Activity");
            el.addAttribute("id", a.getActivityId());
            el.addElement("Name").setText(a.getName());
            el.addElement("Length").setText(String.valueOf(a.getLength()));
            if (a.getAssignment() != null)
                hasSolution = true;
            Element pref = el.addElement("TimePreferences");
            for (Integer slot : new TreeSet<Integer>(a.getDiscouragedSlots()))
                pref.addElement("Soft").setText(slot.toString());
            for (Integer slot : new TreeSet<Integer>(a.getProhibitedSlots()))
                pref.addElement("Hard").setText(slot.toString());
            Element reqRes = el.addElement("RequiredResources");
            for (List<Resource> gr : a.getResourceGroups()) {
                if (gr.size() == 1) {
                    reqRes.addElement("Resource").setText(gr.get(0).getResourceId());
                } else {
                    Element grEl = reqRes.addElement("Group").addAttribute("conjunctive", "no");
                    for (Resource r : gr)
                        grEl.addElement("Resource").setText(r.getResourceId());
                }
            }
        }

        Element depEl = problem.addElement("Dependences");
        for (Dependence d : dependencies) {
            Element el = depEl.addElement("Dependence");
            el.addAttribute("id", d.getResourceId());
            el.addElement("FirstActivity").setText((d.first()).getActivityId());
            el.addElement("SecondActivity").setText((d.second()).getActivityId());
            switch (d.getType()) {
                case Dependence.TYPE_AFTER:
                    el.addElement("Operator").setText("After");
                    break;
                case Dependence.TYPE_BEFORE:
                    el.addElement("Operator").setText("Before");
                    break;
                case Dependence.TYPE_CLOSELY_BEFORE:
                    el.addElement("Operator").setText("Closely before");
                    break;
                case Dependence.TYPE_CLOSELY_AFTER:
                    el.addElement("Operator").setText("Closely after");
                    break;
                case Dependence.TYPE_CONCURRENCY:
                    el.addElement("Operator").setText("Concurrently");
                    break;
                default:
                    el.addElement("Operator").setText("Unknown");
            }
        }

        if (hasSolution) {
            Element solutionEl = root.addElement("Solution");
            solutionEl.addAttribute("version", "2.0");
            for (Activity a : variables()) {
                Element el = solutionEl.addElement("Activity");
                el.addAttribute("id", a.getActivityId());
                if (a.getAssignment() != null) {
                    Location location = a.getAssignment();
                    el.addElement("StartTime").setText(String.valueOf(location.getSlot()));
                    Element res = el.addElement("UsedResources");
                    for (int i = 0; i < location.getResources().length; i++)
                        res.addElement("Resource").setText(location.getResources()[i].getResourceId());
                }
            }
        }

        FileOutputStream fos = new FileOutputStream(outFile);
        (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(document);
        fos.flush();
        fos.close();
    }

    public static TimetableModel loadFromXML(File inFile, boolean assign) throws IOException, DocumentException {
        Document document = (new SAXReader()).read(inFile);
        Element root = document.getRootElement();
        if (!"Timetable".equals(root.getName())) {
            sLogger.error("Given XML file is not interactive timetabling problem.");
            return null;
        }

        Element problem = root.element("Problem");
        Element problemGen = problem.element("General");
        TimetableModel m = new TimetableModel(Integer.parseInt(problemGen.elementText("DaysPerWeek")), Integer
                .parseInt(problemGen.elementText("SlotsPerDay")));

        Element resources = problem.element("Resources");

        HashMap<String, Resource> resTab = new HashMap<String, Resource>();

        Element resEl = resources.element("Classrooms");
        for (Iterator<?> i = resEl.elementIterator("Resource"); i.hasNext();) {
            Element el = (Element) i.next();
            Resource r = new Resource(el.attributeValue("id"), Resource.TYPE_ROOM, el.elementText("Name"));
            Element pref = el.element("TimePreferences");
            for (Iterator<?> j = pref.elementIterator("Soft"); j.hasNext();)
                r.addDiscouragedSlot(Integer.parseInt(((Element) j.next()).getText()));
            for (Iterator<?> j = pref.elementIterator("Hard"); j.hasNext();)
                r.addProhibitedSlot(Integer.parseInt(((Element) j.next()).getText()));
            m.addConstraint(r);
            resTab.put(r.getResourceId(), r);
        }

        resEl = resources.element("Teachers");
        for (Iterator<?> i = resEl.elementIterator("Resource"); i.hasNext();) {
            Element el = (Element) i.next();
            Resource r = new Resource(el.attributeValue("id"), Resource.TYPE_INSTRUCTOR, el.elementText("Name"));
            Element pref = el.element("TimePreferences");
            for (Iterator<?> j = pref.elementIterator("Soft"); j.hasNext();)
                r.addDiscouragedSlot(Integer.parseInt(((Element) j.next()).getText()));
            for (Iterator<?> j = pref.elementIterator("Hard"); j.hasNext();)
                r.addProhibitedSlot(Integer.parseInt(((Element) j.next()).getText()));
            m.addConstraint(r);
            resTab.put(r.getResourceId(), r);
        }

        resEl = resources.element("Classes");
        for (Iterator<?> i = resEl.elementIterator("Resource"); i.hasNext();) {
            Element el = (Element) i.next();
            Resource r = new Resource(el.attributeValue("id"), Resource.TYPE_CLASS, el.elementText("Name"));
            Element pref = el.element("TimePreferences");
            for (Iterator<?> j = pref.elementIterator("Soft"); j.hasNext();)
                r.addDiscouragedSlot(Integer.parseInt(((Element) j.next()).getText()));
            for (Iterator<?> j = pref.elementIterator("Hard"); j.hasNext();)
                r.addProhibitedSlot(Integer.parseInt(((Element) j.next()).getText()));
            m.addConstraint(r);
            resTab.put(r.getResourceId(), r);
        }

        resEl = resources.element("Special");
        for (Iterator<?> i = resEl.elementIterator("Resource"); i.hasNext();) {
            Element el = (Element) i.next();
            Resource r = new Resource(el.attributeValue("id"), Resource.TYPE_OTHER, el.elementText("Name"));
            Element pref = el.element("TimePreferences");
            for (Iterator<?> j = pref.elementIterator("Soft"); j.hasNext();)
                r.addDiscouragedSlot(Integer.parseInt(((Element) j.next()).getText()));
            for (Iterator<?> j = pref.elementIterator("Hard"); j.hasNext();)
                r.addProhibitedSlot(Integer.parseInt(((Element) j.next()).getText()));
            m.addConstraint(r);
            resTab.put(r.getResourceId(), r);
        }

        Element actEl = problem.element("Activities");
        HashMap<String, Activity> actTab = new HashMap<String, Activity>();
        for (Iterator<?> i = actEl.elementIterator("Activity"); i.hasNext();) {
            Element el = (Element) i.next();
            Activity a = new Activity(Integer.parseInt(el.elementText("Length")), el.attributeValue("id"), el
                    .elementText("Name"));
            Element pref = el.element("TimePreferences");
            for (Iterator<?> j = pref.elementIterator("Soft"); j.hasNext();)
                a.addDiscouragedSlot(Integer.parseInt(((Element) j.next()).getText()));
            for (Iterator<?> j = pref.elementIterator("Hard"); j.hasNext();)
                a.addProhibitedSlot(Integer.parseInt(((Element) j.next()).getText()));
            Element req = el.element("RequiredResources");
            for (Iterator<?> j = req.elementIterator(); j.hasNext();) {
                Element rqEl = (Element) j.next();
                if ("Resource".equals(rqEl.getName())) {
                    a.addResourceGroup(resTab.get(rqEl.getText()));
                } else if ("Group".equals(rqEl.getName())) {
                    if ("no".equalsIgnoreCase(rqEl.attributeValue("conjunctive"))
                            || "false".equalsIgnoreCase(rqEl.attributeValue("conjunctive"))) {
                        List<Resource> gr = new ArrayList<Resource>();
                        for (Iterator<?> k = rqEl.elementIterator("Resource"); k.hasNext();)
                            gr.add(resTab.get(((Element) k.next()).getText()));
                        a.addResourceGroup(gr);
                    } else {
                        for (Iterator<?> k = rqEl.elementIterator("Resource"); k.hasNext();)
                            a.addResourceGroup(resTab.get(((Element) k.next()).getText()));
                    }
                }
            }
            m.addVariable(a);
            a.init();
            actTab.put(a.getActivityId(), a);
        }

        Element depEl = problem.element("Dependences");
        for (Iterator<?> i = depEl.elementIterator("Dependence"); i.hasNext();) {
            Element el = (Element) i.next();
            int type = Dependence.TYPE_NO_DEPENDENCE;
            String typeStr = el.elementText("Operator");
            if ("After".equals(typeStr))
                type = Dependence.TYPE_AFTER;
            else if ("Before".equals(typeStr))
                type = Dependence.TYPE_BEFORE;
            else if ("After".equals(typeStr))
                type = Dependence.TYPE_AFTER;
            else if ("Closely before".equals(typeStr))
                type = Dependence.TYPE_CLOSELY_BEFORE;
            else if ("Closely after".equals(typeStr))
                type = Dependence.TYPE_CLOSELY_AFTER;
            else if ("Concurrently".equals(typeStr))
                type = Dependence.TYPE_CONCURRENCY;
            Dependence d = new Dependence(el.attributeValue("id"), type);
            d.addVariable(actTab.get(el.elementText("FirstActivity")));
            d.addVariable(actTab.get(el.elementText("SecondActivity")));
            m.addConstraint(d);
        }

        Element solEl = root.element("Solution");
        if (solEl != null) {
            for (Iterator<?> i = solEl.elementIterator("Activity"); i.hasNext();) {
                Element el = (Element) i.next();
                Activity a = actTab.get(el.attributeValue("id"));
                if (a == null)
                    continue;
                int slot = Integer.parseInt(el.elementText("StartTime"));
                Element usResEl = el.element("UsedResources");
                List<Resource> res = new ArrayList<Resource>();
                for (Iterator<?> j = usResEl.elementIterator("Resource"); j.hasNext();)
                    res.add(resTab.get(((Element) j.next()).getText()));
                for (Location loc : a.values()) {
                    if (loc.getSlot() != slot || loc.getResources().length != res.size())
                        continue;
                    boolean same = true;
                    for (int j = 0; j < loc.getResources().length && same; j++)
                        if (!res.get(j).equals(loc.getResources()[j]))
                            same = false;
                    if (!same)
                        continue;
                    a.setInitialAssignment(loc);
                    if (assign)
                        a.assign(0, loc);
                    break;
                }
            }
        }
        return m;
    }
}
