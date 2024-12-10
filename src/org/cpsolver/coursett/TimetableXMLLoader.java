package org.cpsolver.coursett;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


import org.cpsolver.coursett.constraint.ClassLimitConstraint;
import org.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import org.cpsolver.coursett.constraint.DiscouragedRoomConstraint;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.constraint.IgnoreStudentConflictsConstraint;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.constraint.MinimizeNumberOfUsedGroupsOfTime;
import org.cpsolver.coursett.constraint.MinimizeNumberOfUsedRoomsConstraint;
import org.cpsolver.coursett.constraint.RoomConstraint;
import org.cpsolver.coursett.constraint.SoftInstructorConstraint;
import org.cpsolver.coursett.constraint.SpreadConstraint;
import org.cpsolver.coursett.constraint.FlexibleConstraint.FlexibleConstraintType;
import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.RoomSharingModel;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.StudentGroup;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * This class loads the input model from XML file. <br>
 * <br>
 * Parameters:
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>General.Input</td>
 * <td>{@link String}</td>
 * <td>Input XML file</td>
 * </tr>
 * <tr>
 * <td>General.DeptBalancing</td>
 * <td>{@link Boolean}</td>
 * <td>Use {@link DepartmentSpreadConstraint}</td>
 * </tr>
 * <tr>
 * <td>General.InteractiveMode</td>
 * <td>{@link Boolean}</td>
 * <td>Interactive mode (see {@link Lecture#purgeInvalidValues(boolean)})</td>
 * </tr>
 * <tr>
 * <td>General.ForcedPerturbances</td>
 * <td>{@link Integer}</td>
 * <td>For testing of MPP: number of input perturbations, i.e., classes with
 * prohibited intial assignment</td>
 * </tr>
 * <tr>
 * <td>General.UseDistanceConstraints</td>
 * <td>{@link Boolean}</td>
 * <td>Consider distances between buildings</td>
 * </tr>
 * </table>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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

public class TimetableXMLLoader extends TimetableLoader {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(TimetableXMLLoader.class);
    private static SimpleDateFormat sDF = new SimpleDateFormat("MM/dd");

    private boolean iDeptBalancing = true;
    private int iForcedPerturbances = 0;

    private boolean iInteractiveMode = false;
    private File iInputFile;

    private Progress iProgress = null;

    public TimetableXMLLoader(TimetableModel model, Assignment<Lecture, Placement> assignment) {
        super(model, assignment);
        iProgress = Progress.getInstance(getModel());
        iInputFile = new File(getModel().getProperties().getProperty("General.Input",
                "." + File.separator + "solution.xml"));
        iForcedPerturbances = getModel().getProperties().getPropertyInt("General.ForcedPerturbances", 0);
        iDeptBalancing = getModel().getProperties().getPropertyBoolean("General.DeptBalancing", true);
        iInteractiveMode = getModel().getProperties().getPropertyBoolean("General.InteractiveMode", iInteractiveMode);
    }

    private Solver<Lecture, Placement> iSolver = null;

    public void setSolver(Solver<Lecture, Placement> solver) {
        iSolver = solver;
    }

    public Solver<Lecture, Placement> getSolver() {
        return iSolver;
    }
    
    public void setInputFile(File inputFile) {
        iInputFile = inputFile;
    }

    @Override
    public void load() throws Exception {
        load(null);
    }
    
    public void load(Solution<Lecture, Placement> currentSolution) throws Exception {
        sLogger.debug("Reading XML data from " + iInputFile);
        iProgress.setPhase("Reading " + iInputFile.getName() + " ...");

        Document document = (new SAXReader()).read(iInputFile);
        Element root = document.getRootElement();

        sLogger.debug("Root element: " + root.getName());
        if (!"llrt".equals(root.getName()) && !"timetable".equals(root.getName())) {
            throw new IllegalArgumentException("Given XML file is not large lecture room timetabling problem.");
        }

        if (root.element("input") != null)
            root = root.element("input");

        iProgress.load(root, true);
        iProgress.message(Progress.MSGLEVEL_STAGE, "Restoring from backup ...");

        doLoad(currentSolution, root);

        try {
            getSolver().getClass().getMethod("load", new Class[] { Element.class }).invoke(getSolver(), new Object[] { root });
        } catch (Exception e) {
        }

        iProgress.setPhase("Done", 1);
        iProgress.incProgress();

        sLogger.debug("Model successfully loaded.");
        iProgress.info("Model successfully loaded.");
    }
    
    public void load(Solution<Lecture, Placement> currentSolution, Document document) {
        iProgress.setPhase("Reading solution file ...");

        Element root = document.getRootElement();

        sLogger.debug("Root element: " + root.getName());
        if (!"llrt".equals(root.getName()) && !"timetable".equals(root.getName())) {
            throw new IllegalArgumentException("Given XML file is not large lecture room timetabling problem.");
        }

        if (root.element("input") != null)
            root = root.element("input");

        iProgress.load(root, true);
        iProgress.message(Progress.MSGLEVEL_STAGE, "Restoring from backup ...");

        doLoad(currentSolution, root);

        iProgress.setPhase("Done", 1);
        iProgress.incProgress();

        iProgress.info("Model successfully loaded.");
    }
    
    protected void doLoad(Solution<Lecture, Placement> currentSolution, Element root) {
        if (root.attributeValue("term") != null)
            getModel().getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attributeValue("year") != null)
            getModel().setYear(Integer.parseInt(root.attributeValue("year")));
        else if (root.attributeValue("term") != null)
            getModel().setYear(Integer.parseInt(root.attributeValue("term").substring(0, 4)));
        if (root.attributeValue("initiative") != null)
            getModel().getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));
        if (root.attributeValue("semester") != null && root.attributeValue("year") != null)
            getModel().getProperties().setProperty("Data.Term",
                    root.attributeValue("semester") + root.attributeValue("year"));
        if (root.attributeValue("session") != null)
            getModel().getProperties().setProperty("General.SessionId", root.attributeValue("session"));
        if (root.attributeValue("solverGroup") != null)
            getModel().getProperties().setProperty("General.SolverGroupId", root.attributeValue("solverGroup"));
        String version = root.attributeValue("version");
       
        // Student sectioning considers the whole course (including committed classes), since 2.5
        boolean sectionWholeCourse = true;
        
        if (version != null && version.indexOf('.') >= 0) {
            int majorVersion = Integer.parseInt(version.substring(0, version.indexOf('.')));
            int minorVersion = Integer.parseInt(version.substring(1 + version.indexOf('.')));
            
            sectionWholeCourse = (majorVersion == 2 && minorVersion >= 5) || majorVersion > 2;
        }
        
        HashMap<Long, TimeLocation> perts = new HashMap<Long, TimeLocation>();
        if (getModel().getProperties().getPropertyInt("MPP.TimePert", 0) > 0) {
            int nrChanges = getModel().getProperties().getPropertyInt("MPP.TimePert", 0);
            int idx = 0;
            for (Iterator<?> i = root.element("perturbations").elementIterator("class"); i.hasNext() && idx < nrChanges; idx++) {
                Element pertEl = (Element) i.next();
                Long classId = Long.valueOf(pertEl.attributeValue("id"));
                TimeLocation tl = new TimeLocation(Integer.parseInt(pertEl.attributeValue("days"), 2), Integer
                        .parseInt(pertEl.attributeValue("start")), Integer.parseInt(pertEl.attributeValue("length")),
                        0, 0.0, 0, null, null, null, 0);
                perts.put(classId, tl);
            }
        }

        iProgress.setPhase("Creating rooms ...", root.element("rooms").elements("room").size());
        HashMap<String, Element> roomElements = new HashMap<String, Element>();
        HashMap<String, RoomConstraint> roomConstraints = new HashMap<String, RoomConstraint>();
        HashMap<Long, List<Lecture>> sameLectures = new HashMap<Long, List<Lecture>>();
        HashMap<RoomConstraint, String> roomPartitions = new HashMap<RoomConstraint, String>();
        for (Iterator<?> i = root.element("rooms").elementIterator("room"); i.hasNext();) {
            Element roomEl = (Element) i.next();
            iProgress.incProgress();
            roomElements.put(roomEl.attributeValue("id"), roomEl);
            if ("false".equals(roomEl.attributeValue("constraint")))
                continue;
            RoomSharingModel sharingModel = null;
            Element sharingEl = roomEl.element("sharing");
            if (sharingEl != null) {
                Character freeForAllPrefChar = null;
                Element freeForAllEl = sharingEl.element("freeForAll");
                if (freeForAllEl != null)
                    freeForAllPrefChar = freeForAllEl.attributeValue("value", "F").charAt(0);
                Character notAvailablePrefChar = null;
                Element notAvailableEl = sharingEl.element("notAvailable");
                if (notAvailableEl != null)
                    notAvailablePrefChar = notAvailableEl.attributeValue("value", "X").charAt(0);
                String pattern = sharingEl.element("pattern").getText();
                int unit = Integer.parseInt(sharingEl.element("pattern").attributeValue("unit", "1"));
                Map<Character, Long> departments = new HashMap<Character, Long>();
                for (Iterator<?> j = sharingEl.elementIterator("department"); j.hasNext(); ) {
                    Element deptEl = (Element)j.next();
                    char value = deptEl.attributeValue("value", String.valueOf((char)('0' + departments.size()))).charAt(0);
                    Long id = Long.valueOf(deptEl.attributeValue("id")); 
                    departments.put(value, id);
                }
                sharingModel = new RoomSharingModel(unit, departments, pattern, freeForAllPrefChar, notAvailablePrefChar);
            }
            boolean ignoreTooFar = false;
            if ("true".equals(roomEl.attributeValue("ignoreTooFar")))
                ignoreTooFar = true;
            boolean fake = false;
            if ("true".equals(roomEl.attributeValue("fake")))
                fake = true;
            Double posX = null, posY = null;
            if (roomEl.attributeValue("location") != null) {
                String loc = roomEl.attributeValue("location");
                posX = Double.valueOf(loc.substring(0, loc.indexOf(',')));
                posY = Double.valueOf(loc.substring(loc.indexOf(',') + 1));
            }
            boolean discouraged = "true".equals(roomEl.attributeValue("discouraged"));
            RoomConstraint constraint = (discouraged ? new DiscouragedRoomConstraint(
                    getModel().getProperties(),
                    Long.valueOf(roomEl.attributeValue("id")),
                    (roomEl.attributeValue("name") != null ? roomEl.attributeValue("name") : "r"
                            + roomEl.attributeValue("id")),
                    (roomEl.attributeValue("building") == null ? null : Long.valueOf(roomEl.attributeValue("building"))),
                    Integer.parseInt(roomEl.attributeValue("capacity")), sharingModel, posX, posY, ignoreTooFar, !fake)
                    : new RoomConstraint(Long.valueOf(roomEl.attributeValue("id")),
                            (roomEl.attributeValue("name") != null ? roomEl.attributeValue("name") : "r"
                                    + roomEl.attributeValue("id")), (roomEl.attributeValue("building") == null ? null
                                    : Long.valueOf(roomEl.attributeValue("building"))), Integer.parseInt(roomEl
                                    .attributeValue("capacity")), sharingModel, posX, posY, ignoreTooFar, !fake));
            if (roomEl.attributeValue("type") != null)
                constraint.setType(Long.valueOf(roomEl.attributeValue("type")));
            getModel().addConstraint(constraint);
            roomConstraints.put(roomEl.attributeValue("id"), constraint);
            if (roomEl.attributeValue("parentId") != null)
                roomPartitions.put(constraint, roomEl.attributeValue("parentId"));
            
            for (Iterator<?> j = roomEl.elementIterator("travel-time"); j.hasNext();) {
                Element travelTimeEl = (Element)j.next();
                getModel().getDistanceMetric().addTravelTime(constraint.getResourceId(),
                        Long.valueOf(travelTimeEl.attributeValue("id")),
                        Integer.valueOf(travelTimeEl.attributeValue("minutes")));
            }
        }
        for (Map.Entry<RoomConstraint, String> partition: roomPartitions.entrySet()) {
            RoomConstraint parent = roomConstraints.get(partition.getValue());
            if (parent != null)
                parent.addPartition(partition.getKey());
        }

        HashMap<String, InstructorConstraint> instructorConstraints = new HashMap<String, InstructorConstraint>();
        if (root.element("instructors") != null) {
            for (Iterator<?> i = root.element("instructors").elementIterator("instructor"); i.hasNext();) {
                Element instructorEl = (Element) i.next();
                InstructorConstraint instructorConstraint = null;
                if ("true".equalsIgnoreCase(instructorEl.attributeValue("soft", "false"))) {
                    instructorConstraint = new SoftInstructorConstraint(Long.valueOf(instructorEl
                            .attributeValue("id")), instructorEl.attributeValue("puid"), (instructorEl
                            .attributeValue("name") != null ? instructorEl.attributeValue("name") : "i"
                            + instructorEl.attributeValue("id")), "true".equals(instructorEl.attributeValue("ignDist")));
                } else {
                    instructorConstraint = new InstructorConstraint(Long.valueOf(instructorEl
                        .attributeValue("id")), instructorEl.attributeValue("puid"), (instructorEl
                        .attributeValue("name") != null ? instructorEl.attributeValue("name") : "i"
                        + instructorEl.attributeValue("id")), "true".equals(instructorEl.attributeValue("ignDist")));
                }
                if (instructorEl.attributeValue("type") != null)
                    instructorConstraint.setType(Long.valueOf(instructorEl.attributeValue("type")));
                instructorConstraints.put(instructorEl.attributeValue("id"), instructorConstraint);

                getModel().addConstraint(instructorConstraint);
            }
        }
        HashMap<Long, String> depts = new HashMap<Long, String>();
        if (root.element("departments") != null) {
            for (Iterator<?> i = root.element("departments").elementIterator("department"); i.hasNext();) {
                Element deptEl = (Element) i.next();
                depts.put(Long.valueOf(deptEl.attributeValue("id")), (deptEl.attributeValue("name") != null ? deptEl
                        .attributeValue("name") : "d" + deptEl.attributeValue("id")));
            }
        }

        HashMap<Long, Configuration> configs = new HashMap<Long, Configuration>();
        HashMap<Long, List<Configuration>> alternativeConfigurations = new HashMap<Long, List<Configuration>>();
        if (root.element("configurations") != null) {
            for (Iterator<?> i = root.element("configurations").elementIterator("config"); i.hasNext();) {
                Element configEl = (Element) i.next();
                Long configId = Long.valueOf(configEl.attributeValue("id"));
                int limit = Integer.parseInt(configEl.attributeValue("limit"));
                Long offeringId = Long.valueOf(configEl.attributeValue("offering"));
                Configuration config = new Configuration(offeringId, configId, limit);
                configs.put(configId, config);
                List<Configuration> altConfigs = alternativeConfigurations.get(offeringId);
                if (altConfigs == null) {
                    altConfigs = new ArrayList<Configuration>();
                    alternativeConfigurations.put(offeringId, altConfigs);
                }
                altConfigs.add(config);
                config.setAltConfigurations(altConfigs);
            }
        }

        iProgress.setPhase("Creating variables ...", root.element("classes").elements("class").size());

        HashMap<String, Element> classElements = new HashMap<String, Element>();
        HashMap<String, Lecture> lectures = new HashMap<String, Lecture>();
        HashMap<Lecture, Placement> assignedPlacements = new HashMap<Lecture, Placement>();
        HashMap<Lecture, String> parents = new HashMap<Lecture, String>();
        int ord = 0;
        for (Iterator<?> i1 = root.element("classes").elementIterator("class"); i1.hasNext();) {
            Element classEl = (Element) i1.next();

            Configuration config = null;
            if (classEl.attributeValue("config") != null) {
                config = configs.get(Long.valueOf(classEl.attributeValue("config")));
            }
            if (config == null && classEl.attributeValue("offering") != null) {
                Long offeringId = Long.valueOf(classEl.attributeValue("offering"));
                Long configId = Long.valueOf(classEl.attributeValue("config"));
                List<Configuration> altConfigs = alternativeConfigurations.get(offeringId);
                if (altConfigs == null) {
                    altConfigs = new ArrayList<Configuration>();
                    alternativeConfigurations.put(offeringId, altConfigs);
                }
                for (Configuration c : altConfigs) {
                    if (c.getConfigId().equals(configId)) {
                        config = c;
                        break;
                    }
                }
                if (config == null) {
                    config = new Configuration(offeringId, configId, -1);
                    altConfigs.add(config);
                    config.setAltConfigurations(altConfigs);
                    configs.put(config.getConfigId(), config);
                }
            }

            DatePattern defaultDatePattern = new DatePattern();
            if (classEl.attributeValue("dates") == null) {
                int startDay = Integer.parseInt(classEl.attributeValue("startDay", "0"));
                int endDay = Integer.parseInt(classEl.attributeValue("endDay", "1"));
                defaultDatePattern.setPattern(startDay, endDay);
                defaultDatePattern.setName(sDF.format(getDate(getModel().getYear(), startDay)) + "-" + sDF.format(getDate(getModel().getYear(), endDay)));
            } else {
                defaultDatePattern.setId(classEl.attributeValue("datePattern") == null ? null : Long.valueOf(classEl.attributeValue("datePattern")));
                defaultDatePattern.setName(classEl.attributeValue("datePatternName"));
                defaultDatePattern.setPattern(classEl.attributeValue("dates"));
            }
            Hashtable<Long, DatePattern> datePatterns = new Hashtable<Long, TimetableXMLLoader.DatePattern>();
            for (Iterator<?> i2 = classEl.elementIterator("date"); i2.hasNext();) {
                Element dateEl = (Element) i2.next();
                Long id = Long.valueOf(dateEl.attributeValue("id"));
                datePatterns.put(id, new DatePattern(
                        id,
                        dateEl.attributeValue("name"),
                        dateEl.attributeValue("pattern")));
            }
            classElements.put(classEl.attributeValue("id"), classEl);
            List<InstructorConstraint> ics = new ArrayList<InstructorConstraint>();
            for (Iterator<?> i2 = classEl.elementIterator("instructor"); i2.hasNext();) {
                Element instructorEl = (Element) i2.next();
                InstructorConstraint instructorConstraint = instructorConstraints
                        .get(instructorEl.attributeValue("id"));
                if (instructorConstraint == null) {
                    instructorConstraint = new InstructorConstraint(Long.valueOf(instructorEl.attributeValue("id")),
                            instructorEl.attributeValue("puid"),
                            (instructorEl.attributeValue("name") != null ? instructorEl.attributeValue("name") : "i"
                                    + instructorEl.attributeValue("id")), "true".equals(instructorEl
                                    .attributeValue("ignDist")));
                    instructorConstraints.put(instructorEl.attributeValue("id"), instructorConstraint);
                    getModel().addConstraint(instructorConstraint);
                }
                ics.add(instructorConstraint);
            }
            List<RoomLocation> roomLocations = new ArrayList<RoomLocation>();
            List<RoomConstraint> roomConstraintsThisClass = new ArrayList<RoomConstraint>();
            List<RoomLocation> initialRoomLocations = new ArrayList<RoomLocation>();
            List<RoomLocation> assignedRoomLocations = new ArrayList<RoomLocation>();
            List<RoomLocation> bestRoomLocations = new ArrayList<RoomLocation>();
            for (Iterator<?> i2 = classEl.elementIterator("room"); i2.hasNext();) {
                Element roomLocationEl = (Element) i2.next();
                Element roomEl = roomElements.get(roomLocationEl.attributeValue("id"));
                RoomConstraint roomConstraint = roomConstraints.get(roomLocationEl.attributeValue("id"));

                Long roomId = null;
                String roomName = null;
                Long bldgId = null;

                if (roomConstraint != null) {
                    roomConstraintsThisClass.add(roomConstraint);
                    roomId = roomConstraint.getResourceId();
                    roomName = roomConstraint.getRoomName();
                    bldgId = roomConstraint.getBuildingId();
                } else {
                    roomId = Long.valueOf(roomEl.attributeValue("id"));
                    roomName = (roomEl.attributeValue("name") != null ? roomEl.attributeValue("name") : "r"
                            + roomEl.attributeValue("id"));
                    bldgId = (roomEl.attributeValue("building") == null ? null : Long.valueOf(roomEl
                            .attributeValue("building")));
                }

                boolean ignoreTooFar = false;
                if ("true".equals(roomEl.attributeValue("ignoreTooFar")))
                    ignoreTooFar = true;
                Double posX = null, posY = null;
                if (roomEl.attributeValue("location") != null) {
                    String loc = roomEl.attributeValue("location");
                    posX = Double.valueOf(loc.substring(0, loc.indexOf(',')));
                    posY = Double.valueOf(loc.substring(loc.indexOf(',') + 1));
                }
                RoomLocation rl = new RoomLocation(roomId, roomName, bldgId, Integer.parseInt(roomLocationEl
                        .attributeValue("pref")), Integer.parseInt(roomEl.attributeValue("capacity")), posX, posY,
                        ignoreTooFar, roomConstraint);
                if ("true".equals(roomLocationEl.attributeValue("initial")))
                    initialRoomLocations.add(rl);
                if ("true".equals(roomLocationEl.attributeValue("solution")))
                    assignedRoomLocations.add(rl);
                if ("true".equals(roomLocationEl.attributeValue("best")))
                    bestRoomLocations.add(rl);
                for (Iterator<?> i3 = roomLocationEl.elementIterator("preference"); i3.hasNext(); ) {
                    Element prefEl = (Element) i3.next();
                    rl.setPreference(Integer.valueOf(prefEl.attributeValue("index", "0")), Integer.valueOf(prefEl.attributeValue("pref", "0")));
                }
                roomLocations.add(rl);
            }
            List<TimeLocation> timeLocations = new ArrayList<TimeLocation>();
            TimeLocation initialTimeLocation = null;
            TimeLocation assignedTimeLocation = null;
            TimeLocation bestTimeLocation = null;
            TimeLocation prohibitedTime = perts.get(Long.valueOf(classEl.attributeValue("id")));
            
            for (Iterator<?> i2 = classEl.elementIterator("time"); i2.hasNext();) {
                Element timeLocationEl = (Element) i2.next();
                DatePattern dp = defaultDatePattern;
                if (timeLocationEl.attributeValue("date") != null)
                    dp = datePatterns.get(Long.valueOf(timeLocationEl.attributeValue("date")));
                TimeLocation tl = new TimeLocation(
                        Integer.parseInt(timeLocationEl.attributeValue("days"), 2),
                        Integer.parseInt(timeLocationEl.attributeValue("start")),
                        Integer.parseInt(timeLocationEl.attributeValue("length")),
                        (int) Double.parseDouble(timeLocationEl.attributeValue("pref")),
                        Double.parseDouble(timeLocationEl.attributeValue("npref", timeLocationEl.attributeValue("pref"))),
                        Integer.parseInt(timeLocationEl.attributeValue("datePref", "0")),
                        dp.getId(), dp.getName(), dp.getPattern(),
                        Integer.parseInt(timeLocationEl.attributeValue("breakTime") == null ? "-1" : timeLocationEl.attributeValue("breakTime")));
                if (tl.getBreakTime() < 0) tl.setBreakTime(tl.getLength() == 18 ? 15 : 10);
                if (timeLocationEl.attributeValue("pattern") != null)
                    tl.setTimePatternId(Long.valueOf(timeLocationEl.attributeValue("pattern")));
                /*
                 * if (timePatternTransform) tl =
                 * transformTimePattern(Long.valueOf
                 * (classEl.attributeValue("id")),tl);
                 */
                if (prohibitedTime != null && prohibitedTime.getDayCode() == tl.getDayCode()
                        && prohibitedTime.getStartSlot() == tl.getStartSlot()
                        && prohibitedTime.getLength() == tl.getLength()) {
                    sLogger.info("Time " + tl.getLongName(true) + " is prohibited for class " + classEl.attributeValue("id"));
                    continue;
                }
                if ("true".equals(timeLocationEl.attributeValue("solution")))
                    assignedTimeLocation = tl;
                if ("true".equals(timeLocationEl.attributeValue("initial")))
                    initialTimeLocation = tl;
                if ("true".equals(timeLocationEl.attributeValue("best")))
                    bestTimeLocation = tl;
                timeLocations.add(tl);
            }
            if (timeLocations.isEmpty()) {
                sLogger.error("  ERROR: No time.");
                continue;
            }

            int minClassLimit = 0;
            int maxClassLimit = 0;
            float room2limitRatio = 1.0f;
            if (!"true".equals(classEl.attributeValue("committed"))) {
                if (classEl.attributeValue("expectedCapacity") != null) {
                    minClassLimit = maxClassLimit = Integer.parseInt(classEl.attributeValue("expectedCapacity"));
                    int roomCapacity = Integer.parseInt(classEl.attributeValue("roomCapacity", classEl
                            .attributeValue("expectedCapacity")));
                    if (minClassLimit == 0)
                        minClassLimit = maxClassLimit = roomCapacity;
                    room2limitRatio = (minClassLimit == 0 ? 1.0f : ((float) roomCapacity) / minClassLimit);
                } else {
                    if (classEl.attribute("classLimit") != null) {
                        minClassLimit = maxClassLimit = Integer.parseInt(classEl.attributeValue("classLimit"));
                    } else {
                        minClassLimit = Integer.parseInt(classEl.attributeValue("minClassLimit"));
                        maxClassLimit = Integer.parseInt(classEl.attributeValue("maxClassLimit"));
                    }
                    room2limitRatio = Float.parseFloat(classEl.attributeValue("roomToLimitRatio", "1.0"));
                }
            }

            Lecture lecture = new Lecture(Long.valueOf(classEl.attributeValue("id")),
                    (classEl.attributeValue("solverGroup") != null ? Long
                            .valueOf(classEl.attributeValue("solverGroup")) : null), Long.valueOf(classEl
                            .attributeValue("subpart", classEl.attributeValue("course", "-1"))), (classEl
                            .attributeValue("name") != null ? classEl.attributeValue("name") : "c"
                            + classEl.attributeValue("id")), timeLocations, roomLocations, Integer.parseInt(classEl
                            .attributeValue("nrRooms", roomLocations.isEmpty() ? "0" : "1")), null, minClassLimit, maxClassLimit, room2limitRatio);
            lecture.setNote(classEl.attributeValue("note"));

            if ("true".equals(classEl.attributeValue("committed")))
                lecture.setCommitted(true);

            if (!lecture.isCommitted() && classEl.attributeValue("ord") != null)
                lecture.setOrd(Integer.parseInt(classEl.attributeValue("ord")));
            else
                lecture.setOrd(ord++);

            lecture.setWeight(Double.parseDouble(classEl.attributeValue("weight", "1.0")));
            
            if (lecture.getNrRooms() > 1)
                lecture.setMaxRoomCombinations(Integer.parseInt(classEl.attributeValue("maxRoomCombinations", "-1")));
            
            lecture.setSplitAttendance("true".equals(classEl.attributeValue("splitAttandance")));

            if (config != null)
                lecture.setConfiguration(config);

            if (initialTimeLocation != null && initialRoomLocations.size() == lecture.getNrRooms()) {
                lecture.setInitialAssignment(new Placement(lecture, initialTimeLocation, initialRoomLocations));
            }
            if (assignedTimeLocation != null && assignedRoomLocations.size() == lecture.getNrRooms()) {
                assignedPlacements.put(lecture, new Placement(lecture, assignedTimeLocation, assignedRoomLocations));
            } else if (lecture.getInitialAssignment() != null) {
                // assignedPlacements.put(lecture, lecture.getInitialAssignment());
            }
            if (bestTimeLocation != null && bestRoomLocations.size() == lecture.getNrRooms()) {
                lecture.setBestAssignment(new Placement(lecture, bestTimeLocation, bestRoomLocations), 0);
            } else if (assignedTimeLocation != null && assignedRoomLocations.size() == lecture.getNrRooms()) {
                // lecture.setBestAssignment(assignedPlacements.get(lecture), 0);
            }

            lectures.put(classEl.attributeValue("id"), lecture);
            if (classEl.attributeValue("department") != null)
                lecture.setDepartment(Long.valueOf(classEl.attributeValue("department")));
            if (classEl.attribute("scheduler") != null)
                lecture.setScheduler(Long.valueOf(classEl.attributeValue("scheduler")));
            if ((sectionWholeCourse || !lecture.isCommitted()) && classEl.attributeValue("subpart", classEl.attributeValue("course")) != null) {
                Long subpartId = Long.valueOf(classEl.attributeValue("subpart", classEl.attributeValue("course")));
                List<Lecture> sames = sameLectures.get(subpartId);
                if (sames == null) {
                    sames = new ArrayList<Lecture>();
                    sameLectures.put(subpartId, sames);
                }
                sames.add(lecture);
            }
            String parent = classEl.attributeValue("parent");
            if (parent != null)
                parents.put(lecture, parent);

            getModel().addVariable(lecture);

            if (lecture.isCommitted()) {
                Placement placement = assignedPlacements.get(lecture);
                if (classEl.attribute("assignment") != null)
                    placement.setAssignmentId(Long.valueOf(classEl.attributeValue("assignment")));
                for (InstructorConstraint ic : ics)
                    ic.setNotAvailable(placement);
                for (RoomConstraint rc : roomConstraintsThisClass)
                    if (rc.getConstraint())
                        rc.setNotAvailable(placement);
            } else {
                for (InstructorConstraint ic : ics)
                    ic.addVariable(lecture);
                for (RoomConstraint rc : roomConstraintsThisClass)
                    rc.addVariable(lecture);
            }

            iProgress.incProgress();
        }

        for (Map.Entry<Lecture, String> entry : parents.entrySet()) {
            Lecture lecture = entry.getKey();
            Lecture parent = lectures.get(entry.getValue());
            if (parent == null) {
                iProgress.warn("Parent class " + entry.getValue() + " does not exists.");
            } else {
                lecture.setParent(parent);
            }
        }

        iProgress.setPhase("Creating constraints ...", root.element("groupConstraints").elements("constraint").size());
        HashMap<String, Element> grConstraintElements = new HashMap<String, Element>();
        HashMap<String, Constraint<Lecture, Placement>> groupConstraints = new HashMap<String, Constraint<Lecture, Placement>>();
        for (Iterator<?> i1 = root.element("groupConstraints").elementIterator("constraint"); i1.hasNext();) {
            Element grConstraintEl = (Element) i1.next();
            Constraint<Lecture, Placement> c = null;
            if ("SPREAD".equals(grConstraintEl.attributeValue("type"))) {
                c = new SpreadConstraint(getModel().getProperties(), grConstraintEl.attributeValue("name", "spread"));
            } else if ("MIN_ROOM_USE".equals(grConstraintEl.attributeValue("type"))) {
                c = new MinimizeNumberOfUsedRoomsConstraint(getModel().getProperties());
            } else if ("CLASS_LIMIT".equals(grConstraintEl.attributeValue("type"))) {
                if (grConstraintEl.element("parentClass") == null) {
                    c = new ClassLimitConstraint(Integer.parseInt(grConstraintEl.attributeValue("courseLimit")),
                            grConstraintEl.attributeValue("name", "class-limit"));
                } else {
                    String classId = grConstraintEl.element("parentClass").attributeValue("id");
                    c = new ClassLimitConstraint(lectures.get(classId), grConstraintEl.attributeValue("name",
                            "class-limit"));
                }
                if (grConstraintEl.attributeValue("delta") != null)
                    ((ClassLimitConstraint) c).setClassLimitDelta(Integer.parseInt(grConstraintEl
                            .attributeValue("delta")));
            } else if ("MIN_GRUSE(10x1h)".equals(grConstraintEl.attributeValue("type"))) {
                c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(), "10x1h",
                        MinimizeNumberOfUsedGroupsOfTime.sGroups10of1h);
            } else if ("MIN_GRUSE(5x2h)".equals(grConstraintEl.attributeValue("type"))) {
                c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(), "5x2h",
                        MinimizeNumberOfUsedGroupsOfTime.sGroups5of2h);
            } else if ("MIN_GRUSE(3x3h)".equals(grConstraintEl.attributeValue("type"))) {
                c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(), "3x3h",
                        MinimizeNumberOfUsedGroupsOfTime.sGroups3of3h);
            } else if ("MIN_GRUSE(2x5h)".equals(grConstraintEl.attributeValue("type"))) {
                c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(), "2x5h",
                        MinimizeNumberOfUsedGroupsOfTime.sGroups2of5h);
            } else if (IgnoreStudentConflictsConstraint.REFERENCE.equals(grConstraintEl.attributeValue("type"))) {
                c = new IgnoreStudentConflictsConstraint();
            } else {
                try {
                    FlexibleConstraintType f = FlexibleConstraintType.valueOf(grConstraintEl.attributeValue("type"));
                    try {
                        c = f.create(
                                Long.valueOf(grConstraintEl.attributeValue("id")),
                                grConstraintEl.attributeValue("owner"),
                                grConstraintEl.attributeValue("pref"),
                                grConstraintEl.attributeValue("reference"));
                    } catch (IllegalArgumentException e) {
                            iProgress.warn("Failed to create flexible constraint " + grConstraintEl.attributeValue("type") + ": " + e.getMessage(), e);
                            continue;
                    }
                } catch (IllegalArgumentException e) {
                    // type did not match, continue with group constraint types
                    c = new GroupConstraint(
                            Long.valueOf(grConstraintEl.attributeValue("id")),
                            GroupConstraint.getConstraintType(grConstraintEl.attributeValue("type")),
                            grConstraintEl.attributeValue("pref"));
                }
            }
            getModel().addConstraint(c);
            for (Iterator<?> i2 = grConstraintEl.elementIterator("class"); i2.hasNext();) {
                String classId = ((Element) i2.next()).attributeValue("id");
                Lecture other = lectures.get(classId);
                if (other != null)
                    c.addVariable(other);
                else
                    iProgress.warn("Class " + classId + " does not exists, but it is referred from group constraint " + c.getId() + " (" + c.getName() + ")");
            }
            grConstraintElements.put(grConstraintEl.attributeValue("id"), grConstraintEl);
            groupConstraints.put(grConstraintEl.attributeValue("id"), c);
            iProgress.incProgress();
        }   

        iProgress.setPhase("Loading students ...", root.element("students").elements("student").size());
        boolean initialSectioning = true;
        HashMap<Long, Student> students = new HashMap<Long, Student>();
        HashMap<Long, Set<Student>> offering2students = new HashMap<Long, Set<Student>>();
        for (Iterator<?> i1 = root.element("students").elementIterator("student"); i1.hasNext();) {
            Element studentEl = (Element) i1.next();
            List<Lecture> lecturesThisStudent = new ArrayList<Lecture>();
            Long studentId = Long.valueOf(studentEl.attributeValue("id"));
            Student student = students.get(studentId);
            if (student == null) {
                student = new Student(studentId);
                students.put(studentId, student);
                getModel().addStudent(student);
            }
            student.setAcademicArea(studentEl.attributeValue("area"));
            student.setAcademicClassification(studentEl.attributeValue("classification"));
            student.setMajor(studentEl.attributeValue("major"));
            student.setCurriculum(studentEl.attributeValue("curriculum"));
            for (Iterator<?> i2 = studentEl.elementIterator("offering"); i2.hasNext();) {
                Element ofEl = (Element) i2.next();
                Long offeringId = Long.valueOf(ofEl.attributeValue("id"));
                String priority = ofEl.attributeValue("priority");
                student.addOffering(offeringId, Double.parseDouble(ofEl.attributeValue("weight", "1.0")), priority == null ? null : Double.valueOf(priority));
                Set<Student> studentsThisOffering = offering2students.get(offeringId);
                if (studentsThisOffering == null) {
                    studentsThisOffering = new HashSet<Student>();
                    offering2students.put(offeringId, studentsThisOffering);
                }
                studentsThisOffering.add(student);
                String altId = ofEl.attributeValue("alternative");
                if (altId != null)
                    student.addAlternatives(Long.valueOf(altId), offeringId);
            }
            for (Iterator<?> i2 = studentEl.elementIterator("class"); i2.hasNext();) {
                String classId = ((Element) i2.next()).attributeValue("id");
                Lecture lecture = lectures.get(classId);
                if (lecture == null) {
                    iProgress.warn("Class " + classId + " does not exists, but it is referred from student " + student.getId());
                    continue;
                }
                if (lecture.isCommitted()) {
                    if (sectionWholeCourse && (lecture.getParent() != null || lecture.getConfiguration() != null)) {
                        // committed, but with course structure -- sectioning can be used
                        student.addLecture(lecture);
                        student.addConfiguration(lecture.getConfiguration());
                        lecture.addStudent(getAssignment(), student);
                        lecturesThisStudent.add(lecture);
                        initialSectioning = false;
                    } else {
                        Placement placement = assignedPlacements.get(lecture);
                        student.addCommitedPlacement(placement);
                    }
                } else {
                    student.addLecture(lecture);
                    student.addConfiguration(lecture.getConfiguration());
                    lecture.addStudent(getAssignment(), student);
                    lecturesThisStudent.add(lecture);
                    initialSectioning = false;
                }
            }

            for (Iterator<?> i2 = studentEl.elementIterator("prohibited-class"); i2.hasNext();) {
                String classId = ((Element) i2.next()).attributeValue("id");
                Lecture lecture = lectures.get(classId);
                if (lecture != null)
                    student.addCanNotEnroll(lecture);
                else
                    iProgress.warn("Class " + classId + " does not exists, but it is referred from student " + student.getId());
            }
            
            if (studentEl.attributeValue("instructor") != null)
                student.setInstructor(instructorConstraints.get(studentEl.attributeValue("instructor")));

            iProgress.incProgress();
        }
        
        if (root.element("groups") != null) {
            iProgress.setPhase("Loading student groups ...", root.element("groups").elements("group").size());
            for (Iterator<?> i1 = root.element("groups").elementIterator("group"); i1.hasNext();) {
                Element groupEl = (Element)i1.next();
                long groupId = Long.parseLong(groupEl.attributeValue("id"));
                StudentGroup group = new StudentGroup(groupId, Double.parseDouble(groupEl.attributeValue("weight", "1.0")), groupEl.attributeValue("name", "Group-" + groupId));
                getModel().addStudentGroup(group);
                for (Iterator<?> i2 = groupEl.elementIterator("student"); i2.hasNext();) {
                    Element studentEl = (Element)i2.next();
                    Student student = students.get(Long.valueOf(studentEl.attributeValue("id")));
                    if (student != null) {
                        group.addStudent(student); student.addGroup(group);
                    }
                }
            }
        }

        for (List<Lecture> sames: sameLectures.values()) {
            for (Lecture lect : sames) {
                lect.setSameSubpartLectures(sames);
            }
        }

        if (initialSectioning) {
            iProgress.setPhase("Initial sectioning ...", offering2students.size());
            for (Map.Entry<Long, Set<Student>> entry : offering2students.entrySet()) {
                Long offeringId = entry.getKey();
                Set<Student> studentsThisOffering = entry.getValue();
                List<Configuration> altConfigs = alternativeConfigurations.get(offeringId);
                getModel().getStudentSectioning().initialSectioning(getAssignment(), offeringId, String.valueOf(offeringId), studentsThisOffering, altConfigs);
                iProgress.incProgress();
            }
            for (Student student: students.values()) {
                student.clearDistanceCache();
                if (student.getInstructor() != null)
                    for (Lecture lecture: student.getInstructor().variables()) {
                        student.addLecture(lecture);
                        student.addConfiguration(lecture.getConfiguration());
                        lecture.addStudent(getAssignment(), student);
                    }
            }
        }

        iProgress.setPhase("Computing jenrl ...", students.size());
        HashMap<Lecture, HashMap<Lecture, JenrlConstraint>> jenrls = new HashMap<Lecture, HashMap<Lecture, JenrlConstraint>>();
        for (Iterator<Student> i1 = students.values().iterator(); i1.hasNext();) {
            Student st = i1.next();
            for (Iterator<Lecture> i2 = st.getLectures().iterator(); i2.hasNext();) {
                Lecture l1 = i2.next();
                for (Iterator<Lecture> i3 = st.getLectures().iterator(); i3.hasNext();) {
                    Lecture l2 = i3.next();
                    if (l1.getId() >= l2.getId())
                        continue;
                    HashMap<Lecture, JenrlConstraint> x = jenrls.get(l1);
                    if (x == null) {
                        x = new HashMap<Lecture, JenrlConstraint>();
                        jenrls.put(l1, x);
                    }
                    JenrlConstraint jenrl = x.get(l2);
                    if (jenrl == null) {
                        jenrl = new JenrlConstraint();
                        jenrl.addVariable(l1);
                        jenrl.addVariable(l2);
                        getModel().addConstraint(jenrl);
                        x.put(l2, jenrl);
                    }
                    jenrl.incJenrl(getAssignment(), st);
                }
            }
            iProgress.incProgress();
        }

        if (iDeptBalancing) {
            iProgress.setPhase("Creating dept. spread constraints ...", getModel().variables().size());
            HashMap<Long, DepartmentSpreadConstraint> depSpreadConstraints = new HashMap<Long, DepartmentSpreadConstraint>();
            for (Lecture lecture : getModel().variables()) {
                if (lecture.getDepartment() == null)
                    continue;
                DepartmentSpreadConstraint deptConstr = depSpreadConstraints.get(lecture.getDepartment());
                if (deptConstr == null) {
                    String name = depts.get(lecture.getDepartment());
                    deptConstr = new DepartmentSpreadConstraint(getModel().getProperties(), lecture.getDepartment(),
                            (name != null ? name : "d" + lecture.getDepartment()));
                    depSpreadConstraints.put(lecture.getDepartment(), deptConstr);
                    getModel().addConstraint(deptConstr);
                }
                deptConstr.addVariable(lecture);
                iProgress.incProgress();
            }
        }

        if (getModel().getProperties().getPropertyBoolean("General.PurgeInvalidPlacements", true)) {
            iProgress.setPhase("Purging invalid placements ...", getModel().variables().size());
            for (Lecture lecture : getModel().variables()) {
                lecture.purgeInvalidValues(iInteractiveMode);
                iProgress.incProgress();
            }            
        }
        
        if (getModel().hasConstantVariables() && getModel().constantVariables().size() > 0) {
            iProgress.setPhase("Assigning committed classes ...", assignedPlacements.size());
            for (Map.Entry<Lecture, Placement> entry : assignedPlacements.entrySet()) {
                Lecture lecture = entry.getKey();
                Placement placement = entry.getValue();
                if (!lecture.isCommitted()) { iProgress.incProgress(); continue; }
                lecture.setConstantValue(placement);
                getModel().weaken(getAssignment(), placement);
                Map<Constraint<Lecture, Placement>, Set<Placement>> conflictConstraints = getModel().conflictConstraints(getAssignment(), placement);
                if (conflictConstraints.isEmpty()) {
                    getAssignment().assign(0, placement);
                } else {
                    iProgress.warn("WARNING: Unable to assign " + lecture.getName() + " := " + placement.getName());
                    iProgress.debug("  Reason:");
                    for (Constraint<Lecture, Placement> c : conflictConstraints.keySet()) {
                        Set<Placement> vals = conflictConstraints.get(c);
                        for (Placement v : vals) {
                            iProgress.debug("    " + v.variable().getName() + " = " + v.getName());
                        }
                        iProgress.debug("    in constraint " + c);
                    }
                }
                iProgress.incProgress();
            }
        }

        if (currentSolution != null) {
            iProgress.setPhase("Creating best assignment ...", 2 * getModel().variables().size());
            for (Lecture lecture : getModel().variables()) {
                iProgress.incProgress();
                Placement placement = lecture.getBestAssignment();
                if (placement == null) continue;
                getModel().weaken(getAssignment(), placement);
                getAssignment().assign(0, placement);
            }

            currentSolution.saveBest();
            for (Lecture lecture : getModel().variables()) {
                iProgress.incProgress();
                getAssignment().unassign(0, lecture);
            }
        }

        iProgress.setPhase("Creating initial assignment ...", assignedPlacements.size());
        for (Map.Entry<Lecture, Placement> entry : assignedPlacements.entrySet()) {
            Lecture lecture = entry.getKey();
            Placement placement = entry.getValue();
            if (lecture.isCommitted()) { iProgress.incProgress(); continue; }
            getModel().weaken(getAssignment(), placement);
            Map<Constraint<Lecture, Placement>, Set<Placement>> conflictConstraints = getModel().conflictConstraints(getAssignment(), placement);
            if (conflictConstraints.isEmpty()) {
                if (!placement.isValid()) {
                    iProgress.warn("WARNING: Lecture " + lecture.getName() + " does not contain assignment "
                            + placement.getLongName(true) + " in its domain (" + placement.getNotValidReason(getAssignment(), true) + ").");
                } else
                    getAssignment().assign(0, placement);
            } else {
                iProgress.warn("WARNING: Unable to assign " + lecture.getName() + " := " + placement.getName());
                iProgress.debug("  Reason:");
                for (Constraint<Lecture, Placement> c : conflictConstraints.keySet()) {
                    Set<Placement> vals = conflictConstraints.get(c);
                    for (Placement v : vals) {
                        iProgress.debug("    " + v.variable().getName() + " = " + v.getName());
                    }
                    iProgress.debug("    in constraint " + c);
                }
            }
            iProgress.incProgress();
        }

        if (initialSectioning && getAssignment().nrAssignedVariables() != 0 && !getModel().getProperties().getPropertyBoolean("Global.LoadStudentEnrlsFromSolution", false))
            getModel().switchStudents(getAssignment());

        if (iForcedPerturbances > 0) {
            iProgress.setPhase("Forcing perturbances", iForcedPerturbances);
            for (int i = 0; i < iForcedPerturbances; i++) {
                iProgress.setProgress(i);
                Lecture var = null;
                do {
                    var = ToolBox.random(getModel().variables());
                } while (var.getInitialAssignment() == null || var.values(getAssignment()).size() <= 1);
                var.removeInitialValue();
            }
        }

        /*
        for (Constraint<Lecture, Placement> c : getModel().constraints()) {
            if (c instanceof SpreadConstraint)
                ((SpreadConstraint) c).init();
            if (c instanceof DiscouragedRoomConstraint)
                ((DiscouragedRoomConstraint) c).setEnabled(true);
            if (c instanceof MinimizeNumberOfUsedRoomsConstraint)
                ((MinimizeNumberOfUsedRoomsConstraint) c).setEnabled(true);
            if (c instanceof MinimizeNumberOfUsedGroupsOfTime)
                ((MinimizeNumberOfUsedGroupsOfTime) c).setEnabled(true);
        }
         */
    }

    public static Date getDate(int year, int dayOfYear) {
        Calendar c = Calendar.getInstance(Locale.US);
        c.set(year, 1, 1, 0, 0, 0);
        c.set(Calendar.DAY_OF_YEAR, dayOfYear);
        return c.getTime();
    }
    
    public static class DatePattern {
        Long iId;
        String iName;
        BitSet iPattern;
        public DatePattern() {}
        public DatePattern(Long id, String name, BitSet pattern) {
            setId(id); setName(name); setPattern(pattern);
        }
        public DatePattern(Long id, String name, String pattern) {
            setId(id); setName(name); setPattern(pattern);
        }
        public Long getId() { return iId; }
        public void setId(Long id) { iId = id; }
        public String getName() { return iName; }
        public void setName(String name) { iName = name; }
        public BitSet getPattern() { return iPattern; }
        public void setPattern(BitSet pattern) { iPattern = pattern; }
        public void setPattern(String pattern) {
            iPattern = new BitSet(pattern.length());
            for (int i = 0; i < pattern.length(); i++)
                if (pattern.charAt(i) == '1')
                    iPattern.set(i);
        }
        public void setPattern(int startDay, int endDay) {
            iPattern = new BitSet(366);
            for (int d = startDay; d <= endDay; d++)
                iPattern.set(d);
        }
    }
}
