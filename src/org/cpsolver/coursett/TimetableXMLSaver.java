package org.cpsolver.coursett;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import org.cpsolver.coursett.constraint.ClassLimitConstraint;
import org.cpsolver.coursett.constraint.DiscouragedRoomConstraint;
import org.cpsolver.coursett.constraint.FlexibleConstraint;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.constraint.IgnoreStudentConflictsConstraint;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.MinimizeNumberOfUsedGroupsOfTime;
import org.cpsolver.coursett.constraint.MinimizeNumberOfUsedRoomsConstraint;
import org.cpsolver.coursett.constraint.RoomConstraint;
import org.cpsolver.coursett.constraint.SoftInstructorConstraint;
import org.cpsolver.coursett.constraint.SpreadConstraint;
import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.RoomSharingModel;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.StudentGroup;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * This class saves the resultant solution in the XML format. <br>
 * <br>
 * Parameters:
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>General.Output</td>
 * <td>{@link String}</td>
 * <td>Folder with the output solution in XML format (solution.xml)</td>
 * </tr>
 * <tr>
 * <td>Xml.ConvertIds</td>
 * <td>{@link Boolean}</td>
 * <td>If true, ids are converted (to be able to make input data public)</td>
 * </tr>
 * <tr>
 * <td>Xml.ShowNames</td>
 * <td>{@link Boolean}</td>
 * <td>If false, names are not exported (to be able to make input data public)</td>
 * </tr>
 * <tr>
 * <td>Xml.SaveBest</td>
 * <td>{@link Boolean}</td>
 * <td>If true, best solution is saved.</td>
 * </tr>
 * <tr>
 * <td>Xml.SaveInitial</td>
 * <td>{@link Boolean}</td>
 * <td>If true, initial solution is saved.</td>
 * </tr>
 * <tr>
 * <td>Xml.SaveCurrent</td>
 * <td>{@link Boolean}</td>
 * <td>If true, current solution is saved.</td>
 * </tr>
 * <tr>
 * <td>Xml.ExportStudentSectioning</td>
 * <td>{@link Boolean}</td>
 * <td>If true, student sectioning is saved even when there is no solution.</td>
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

public class TimetableXMLSaver extends TimetableSaver {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(TimetableXMLSaver.class);
    private static DecimalFormat[] sDF = { new DecimalFormat(""), new DecimalFormat("0"), new DecimalFormat("00"),
            new DecimalFormat("000"), new DecimalFormat("0000"), new DecimalFormat("00000"),
            new DecimalFormat("000000"), new DecimalFormat("0000000") };
    private static DecimalFormat sStudentWeightFormat = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
    public static boolean ANONYMISE = false;

    private boolean iConvertIds = false;
    private boolean iShowNames = false;
    private File iOutputFolder = null;
    private boolean iSaveBest = false;
    private boolean iSaveInitial = false;
    private boolean iSaveCurrent = false;
    private boolean iExportStudentSectioning = false;
    private boolean iSaveConfig = false;

    private IdConvertor iIdConvertor = null;

    public TimetableXMLSaver(Solver<Lecture, Placement> solver) {
        super(solver);
        
        
        iOutputFolder = new File(getModel().getProperties().getProperty("General.Output",
                "." + File.separator + "output"));
        iShowNames = getModel().getProperties().getPropertyBoolean("Xml.ShowNames", false);
        iExportStudentSectioning = getModel().getProperties().getPropertyBoolean("Xml.ExportStudentSectioning", false);
        if (ANONYMISE) {
            // anonymise saved XML file -- if not set otherwise in the
            // configuration
            iConvertIds = getModel().getProperties().getPropertyBoolean("Xml.ConvertIds", true);
            iSaveBest = getModel().getProperties().getPropertyBoolean("Xml.SaveBest", false);
            iSaveInitial = getModel().getProperties().getPropertyBoolean("Xml.SaveInitial", false);
            iSaveCurrent = getModel().getProperties().getPropertyBoolean("Xml.SaveCurrent", true);
        } else {
            // normal operation -- if not set otherwise in the configuration
            iConvertIds = getModel().getProperties().getPropertyBoolean("Xml.ConvertIds", false);
            iSaveBest = getModel().getProperties().getPropertyBoolean("Xml.SaveBest", true);
            iSaveInitial = getModel().getProperties().getPropertyBoolean("Xml.SaveInitial", true);
            iSaveCurrent = getModel().getProperties().getPropertyBoolean("Xml.SaveCurrent", true);
        }
        iSaveConfig = getModel().getProperties().getPropertyBoolean("Xml.SaveConfig", false);
    }

    private String getId(String type, String id) {
        if (!iConvertIds)
            return id.toString();
        if (iIdConvertor == null)
            iIdConvertor = new IdConvertor(getModel().getProperties().getProperty("Xml.IdConv"));
        return iIdConvertor.convert(type, id);
    }

    private String getId(String type, Number id) {
        return getId(type, id.toString());
    }

    private static String bitset2string(BitSet b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length(); i++)
            sb.append(b.get(i) ? "1" : "0");
        return sb.toString();
    }

    @Override
    public void save() throws Exception {
        save(null);
    }
    
    public Document saveDocument() {
        Document document = DocumentHelper.createDocument();
        document.addComment("University Course Timetabling");

        if (iSaveCurrent && getAssignment().nrAssignedVariables() != 0) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (getSolution() == null ? getModel().getExtendedInfo(getAssignment()) : getSolution().getExtendedInfo());
            for (String key : new TreeSet<String>(solutionInfo.keySet())) {
                String value = solutionInfo.get(key);
                comments.append("    " + key + ": " + value + "\n");
            }
            document.addComment(comments.toString());
        }

        Element root = document.addElement("timetable");

        doSave(root);

        return document;
    }

    public void save(File outFile) throws Exception {
        if (outFile == null)
            outFile = new File(iOutputFolder, "solution.xml");
        outFile.getParentFile().mkdirs();
        sLogger.debug("Writting XML data to:" + outFile);

        Document document = DocumentHelper.createDocument();
        document.addComment("University Course Timetabling");

        if (iSaveCurrent && getAssignment().nrAssignedVariables() != 0) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (getSolution() == null ? getModel().getExtendedInfo(getAssignment()) : getSolution().getExtendedInfo());
            for (String key : new TreeSet<String>(solutionInfo.keySet())) {
                String value = solutionInfo.get(key);
                comments.append("    " + key + ": " + value + "\n");
            }
            document.addComment(comments.toString());
        }

        Element root = document.addElement("timetable");

        doSave(root);

        if (iShowNames) {
            Progress.getInstance(getModel()).save(root);

            try {
                getSolver().getClass().getMethod("save", new Class[] { Element.class }).invoke(getSolver(),
                		new Object[] { root });
            } catch (Exception e) {
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(document);
            fos.flush();
            fos.close();
            fos = null;
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }

        if (iConvertIds)
            iIdConvertor.save();
    }
    
    protected void doSave(Element root) {
        root.addAttribute("version", "2.5");
        root.addAttribute("initiative", getModel().getProperties().getProperty("Data.Initiative"));
        root.addAttribute("term", getModel().getProperties().getProperty("Data.Term"));
        root.addAttribute("year", String.valueOf(getModel().getYear()));
        root.addAttribute("created", String.valueOf(new Date()));
        root.addAttribute("nrDays", String.valueOf(Constants.DAY_CODES.length));
        root.addAttribute("slotsPerDay", String.valueOf(Constants.SLOTS_PER_DAY));
        if (!iConvertIds && getModel().getProperties().getProperty("General.SessionId") != null)
            root.addAttribute("session", getModel().getProperties().getProperty("General.SessionId"));
        if (iShowNames && !iConvertIds && getModel().getProperties().getProperty("General.SolverGroupId") != null)
            root.addAttribute("solverGroup", getId("solverGroup", getModel().getProperties().getProperty(
                    "General.SolverGroupId")));

        HashMap<String, Element> roomElements = new HashMap<String, Element>();

        Element roomsEl = root.addElement("rooms");
        for (RoomConstraint roomConstraint : getModel().getRoomConstraints()) {
            Element roomEl = roomsEl.addElement("room").addAttribute("id",
                    getId("room", roomConstraint.getResourceId()));
            roomEl.addAttribute("constraint", "true");
            if (roomConstraint instanceof DiscouragedRoomConstraint)
                roomEl.addAttribute("discouraged", "true");
            if (iShowNames) {
                roomEl.addAttribute("name", roomConstraint.getRoomName());
            }
            if (!iConvertIds && roomConstraint.getBuildingId() != null)
                roomEl.addAttribute("building", getId("bldg", roomConstraint.getBuildingId()));
            if (roomConstraint.getParentRoom() != null)
                roomEl.addAttribute("parentId", getId("room", roomConstraint.getParentRoom().getResourceId()));
            roomElements.put(getId("room", roomConstraint.getResourceId()), roomEl);
            roomEl.addAttribute("capacity", String.valueOf(roomConstraint.getCapacity()));
            if (roomConstraint.getPosX() != null && roomConstraint.getPosY() != null)
                roomEl.addAttribute("location", roomConstraint.getPosX() + "," + roomConstraint.getPosY());
            if (roomConstraint.getIgnoreTooFar())
                roomEl.addAttribute("ignoreTooFar", "true");
            if (!roomConstraint.getConstraint())
                roomEl.addAttribute("fake", "true");
            if (roomConstraint.getSharingModel() != null) {
                RoomSharingModel sharingModel = roomConstraint.getSharingModel();
                Element sharingEl = roomEl.addElement("sharing");
                sharingEl.addElement("pattern").addAttribute("unit", String.valueOf(sharingModel.getStep())).setText(sharingModel.getPreferences());
                sharingEl.addElement("freeForAll").addAttribute("value",
                        String.valueOf(sharingModel.getFreeForAllPrefChar()));
                sharingEl.addElement("notAvailable").addAttribute("value",
                        String.valueOf(sharingModel.getNotAvailablePrefChar()));
                for (Long id: sharingModel.getDepartmentIds()) {
                    sharingEl.addElement("department")
                        .addAttribute("value", String.valueOf(sharingModel.getCharacter(id)))
                        .addAttribute("id", getId("dept", id));
                }
            }
            if (roomConstraint.getType() != null && iShowNames)
                roomEl.addAttribute("type", roomConstraint.getType().toString());
            
            Map<Long, Integer> travelTimes = getModel().getDistanceMetric().getTravelTimes().get(roomConstraint.getResourceId());
            if (travelTimes != null)
                for (Map.Entry<Long, Integer> time: travelTimes.entrySet())
                    roomEl.addElement("travel-time").addAttribute("id", getId("room", time.getKey())).addAttribute("minutes", time.getValue().toString());
        }

        Element instructorsEl = root.addElement("instructors");

        Element departmentsEl = root.addElement("departments");
        HashMap<Long, String> depts = new HashMap<Long, String>();

        Element configsEl = (iShowNames ? root.addElement("configurations") : null);
        HashSet<Configuration> configs = new HashSet<Configuration>();

        Element classesEl = root.addElement("classes");
        HashMap<Long, Element> classElements = new HashMap<Long, Element>();
        List<Lecture> vars = new ArrayList<Lecture>(getModel().variables());
        if (getModel().hasConstantVariables())
            vars.addAll(getModel().constantVariables());
        for (Lecture lecture : vars) {
            Placement placement = getAssignment().getValue(lecture);
            if (lecture.isCommitted() && placement == null)
                placement = lecture.getInitialAssignment();
            Placement initialPlacement = lecture.getInitialAssignment();
            // if (initialPlacement==null) initialPlacement =
            // (Placement)lecture.getAssignment();
            Placement bestPlacement = lecture.getBestAssignment();
            Element classEl = classesEl.addElement("class").addAttribute("id", getId("class", lecture.getClassId()));
            classElements.put(lecture.getClassId(), classEl);
            if (iShowNames && lecture.getNote() != null)
                classEl.addAttribute("note", lecture.getNote());
            if (iShowNames && !lecture.isCommitted())
                classEl.addAttribute("ord", String.valueOf(lecture.getOrd()));
            if (lecture.getWeight() != 1.0)
                classEl.addAttribute("weight", String.valueOf(lecture.getWeight()));
            if (iShowNames && lecture.getSolverGroupId() != null)
                classEl.addAttribute("solverGroup", getId("solverGroup", lecture.getSolverGroupId()));
            if (lecture.getParent() == null && lecture.getConfiguration() != null) {
                if (!iShowNames)
                    classEl.addAttribute("offering", getId("offering", lecture.getConfiguration().getOfferingId()
                            .toString()));
                classEl.addAttribute("config", getId("config", lecture.getConfiguration().getConfigId().toString()));
                if (iShowNames && configs.add(lecture.getConfiguration())) {
                    configsEl.addElement("config").addAttribute("id",
                            getId("config", lecture.getConfiguration().getConfigId().toString())).addAttribute("limit",
                            String.valueOf(lecture.getConfiguration().getLimit())).addAttribute("offering",
                            getId("offering", lecture.getConfiguration().getOfferingId().toString()));
                }
            }
            classEl.addAttribute("committed", (lecture.isCommitted() ? "true" : "false"));
            if (lecture.getParent() != null)
                classEl.addAttribute("parent", getId("class", lecture.getParent().getClassId()));
            if (lecture.getSchedulingSubpartId() != null)
                classEl.addAttribute("subpart", getId("subpart", lecture.getSchedulingSubpartId()));
            if (iShowNames && lecture.isCommitted() && placement != null && placement.getAssignmentId() != null) {
                classEl.addAttribute("assignment", getId("assignment", placement.getAssignmentId()));
            }
            if (!lecture.isCommitted()) {
                if (lecture.minClassLimit() == lecture.maxClassLimit()) {
                    classEl.addAttribute("classLimit", String.valueOf(lecture.maxClassLimit()));
                } else {
                    classEl.addAttribute("minClassLimit", String.valueOf(lecture.minClassLimit()));
                    classEl.addAttribute("maxClassLimit", String.valueOf(lecture.maxClassLimit()));
                }
                if (lecture.roomToLimitRatio() != 1.0f)
                    classEl.addAttribute("roomToLimitRatio", sStudentWeightFormat.format(lecture.roomToLimitRatio()));
            }
            if (lecture.getNrRooms() != 1)
                classEl.addAttribute("nrRooms", String.valueOf(lecture.getNrRooms()));
            if (lecture.getNrRooms() > 1)
                classEl.addAttribute("splitAttandance", lecture.isSplitAttendance() ? "true" : "false");
            if (lecture.getNrRooms() > 1 && lecture.getMaxRoomCombinations() > 0)
                classEl.addAttribute("maxRoomCombinations", String.valueOf(lecture.getMaxRoomCombinations()));
            if (iShowNames)
                classEl.addAttribute("name", lecture.getName());
            if (lecture.getDeptSpreadConstraint() != null) {
                classEl.addAttribute("department", getId("dept", lecture.getDeptSpreadConstraint().getDepartmentId()));
                depts.put(lecture.getDeptSpreadConstraint().getDepartmentId(), lecture.getDeptSpreadConstraint()
                        .getName());
            } else if (lecture.getDepartment() != null) {
                classEl.addAttribute("department", getId("dept", lecture.getDepartment()));
            }
            if (lecture.getScheduler() != null)
                classEl.addAttribute("scheduler", getId("dept", lecture.getScheduler()));
            for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
                Element instrEl = classEl.addElement("instructor")
                        .addAttribute("id", getId("inst", ic.getResourceId()));
                if ((lecture.isCommitted() || iSaveCurrent) && placement != null)
                    instrEl.addAttribute("solution", "true");
                if (iSaveInitial && initialPlacement != null)
                    instrEl.addAttribute("initial", "true");
                if (iSaveBest && bestPlacement != null)
                    instrEl.addAttribute("best", "true");
            }
            for (RoomLocation rl : lecture.roomLocations()) {
                Element roomLocationEl = classEl.addElement("room");
                roomLocationEl.addAttribute("id", getId("room", rl.getId()));
                roomLocationEl.addAttribute("pref", String.valueOf(rl.getPreference()));
                if ((lecture.isCommitted() || iSaveCurrent) && placement != null
                        && placement.hasRoomLocation(rl.getId()))
                    roomLocationEl.addAttribute("solution", "true");
                if (iSaveInitial && initialPlacement != null && initialPlacement.hasRoomLocation(rl.getId()))
                    roomLocationEl.addAttribute("initial", "true");
                if (iSaveBest && bestPlacement != null && bestPlacement.hasRoomLocation(rl.getId()))
                    roomLocationEl.addAttribute("best", "true");
                if (rl.hasPreferenceByIndex()) {
                    for (Map.Entry<Integer, Integer> e: rl.getPreferenceByIndex().entrySet()) {
                        roomLocationEl.addElement("preference").addAttribute("index", e.getKey().toString()).addAttribute("pref", e.getValue().toString());
                    }
                }
                if (!roomElements.containsKey(getId("room", rl.getId()))) {
                    // room location without room constraint
                    Element roomEl = roomsEl.addElement("room").addAttribute("id", getId("room", rl.getId()));
                    roomEl.addAttribute("constraint", "false");
                    if (!iConvertIds && rl.getBuildingId() != null)
                        roomEl.addAttribute("building", getId("bldg", rl.getBuildingId()));
                    if (rl.getRoomConstraint() != null && rl.getRoomConstraint().getParentRoom() != null)
                        roomEl.addAttribute("parentId", getId("room", rl.getRoomConstraint().getParentRoom().getResourceId()));
                    if (iShowNames) {
                        roomEl.addAttribute("name", rl.getName());
                    }
                    roomElements.put(getId("room", rl.getId()), roomEl);
                    roomEl.addAttribute("capacity", String.valueOf(rl.getRoomSize()));
                    if (rl.getPosX() != null && rl.getPosY() != null)
                        roomEl.addAttribute("location", rl.getPosX() + "," + rl.getPosY());
                    if (rl.getIgnoreTooFar())
                        roomEl.addAttribute("ignoreTooFar", "true");
                }
            }
            boolean first = true;
            Set<Long> dp = new HashSet<Long>();
            for (TimeLocation tl : lecture.timeLocations()) {
                Element timeLocationEl = classEl.addElement("time");
                timeLocationEl.addAttribute("days", sDF[7].format(Long.parseLong(Integer
                        .toBinaryString(tl.getDayCode()))));
                timeLocationEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                timeLocationEl.addAttribute("length", String.valueOf(tl.getLength()));
                timeLocationEl.addAttribute("breakTime", String.valueOf(tl.getBreakTime()));
                if (iShowNames) {
                    timeLocationEl.addAttribute("pref", String.valueOf(tl.getPreference()));
                    timeLocationEl.addAttribute("npref", String.valueOf(tl.getNormalizedPreference()));
                } else {
                    timeLocationEl.addAttribute("pref", String.valueOf(tl.getNormalizedPreference()));
                }
                if (!iConvertIds && tl.getTimePatternId() != null)
                    timeLocationEl.addAttribute("pattern", getId("pat", tl.getTimePatternId()));
                if (tl.getDatePatternId() != null && dp.add(tl.getDatePatternId())) {
                    Element dateEl = classEl.addElement("date");
                    dateEl.addAttribute("id", getId("dpat", String.valueOf(tl.getDatePatternId())));
                    if (iShowNames)
                        dateEl.addAttribute("name", tl.getDatePatternName());
                    dateEl.addAttribute("pattern", bitset2string(tl.getWeekCode()));
                }
                if (tl.getDatePatternPreference() != 0)
                    timeLocationEl.addAttribute("datePref", String.valueOf(tl.getDatePatternPreference()));
                if (tl.getTimePatternId() == null && first) {
                    if (iShowNames)
                        classEl.addAttribute("datePatternName", tl.getDatePatternName());
                    classEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                    first = false;
                }
                if (tl.getDatePatternId() != null) {
                    timeLocationEl.addAttribute("date", getId("dpat", String.valueOf(tl.getDatePatternId())));
                }
                if ((lecture.isCommitted() || iSaveCurrent) && placement != null
                        && placement.getTimeLocation().equals(tl))
                    timeLocationEl.addAttribute("solution", "true");
                if (iSaveInitial && initialPlacement != null && initialPlacement.getTimeLocation().equals(tl))
                    timeLocationEl.addAttribute("initial", "true");
                if (iSaveBest && bestPlacement != null && bestPlacement.getTimeLocation().equals(tl))
                    timeLocationEl.addAttribute("best", "true");
            }
        }

        for (InstructorConstraint ic : getModel().getInstructorConstraints()) {
            if (iShowNames || ic.isIgnoreDistances() || ic instanceof SoftInstructorConstraint) {
                Element instrEl = instructorsEl.addElement("instructor").addAttribute("id",
                        getId("inst", ic.getResourceId()));
                if (iShowNames) {
                    if (ic.getPuid() != null && ic.getPuid().length() > 0)
                        instrEl.addAttribute("puid", ic.getPuid());
                    instrEl.addAttribute("name", ic.getName());
                    if (ic.getType() != null && iShowNames)
                        instrEl.addAttribute("type", ic.getType().toString());
                }
                if (ic.isIgnoreDistances()) {
                    instrEl.addAttribute("ignDist", "true");
                }
                if (ic instanceof SoftInstructorConstraint) instrEl.addAttribute("soft", "true");
            }
            if (ic.getUnavailabilities() != null) {
                for (Placement placement: ic.getUnavailabilities()) {
                    Lecture lecture = placement.variable();
                    Element classEl = classElements.get(lecture.getClassId());
                    classEl.addElement("instructor").addAttribute("id", getId("inst", ic.getResourceId())).addAttribute("solution", "true");
                }
            }
        }
        if (instructorsEl.elements().isEmpty())
            root.remove(instructorsEl);

        Element grConstraintsEl = root.addElement("groupConstraints");
        for (GroupConstraint gc : getModel().getGroupConstraints()) {
            Element grEl = grConstraintsEl.addElement("constraint").addAttribute("id",
                    getId("gr", String.valueOf(gc.getId())));
            grEl.addAttribute("type", gc.getType().reference());
            grEl.addAttribute("pref", gc.getPrologPreference());
            for (Lecture l : gc.variables()) {
                grEl.addElement("class").addAttribute("id", getId("class", l.getClassId()));
            }
        }       
        for (SpreadConstraint spread : getModel().getSpreadConstraints()) {
            Element grEl = grConstraintsEl.addElement("constraint").addAttribute("id",
                    getId("gr", String.valueOf(spread.getId())));
            grEl.addAttribute("type", "SPREAD");
            grEl.addAttribute("pref", Constants.sPreferenceRequired);
            if (iShowNames)
                grEl.addAttribute("name", spread.getName());
            for (Lecture l : spread.variables()) {
                grEl.addElement("class").addAttribute("id", getId("class", l.getClassId()));
            }
        }
        for (Constraint<Lecture, Placement> c : getModel().constraints()) {
            if (c instanceof MinimizeNumberOfUsedRoomsConstraint) {
                Element grEl = grConstraintsEl.addElement("constraint").addAttribute("id",
                        getId("gr", String.valueOf(c.getId())));
                grEl.addAttribute("type", "MIN_ROOM_USE");
                grEl.addAttribute("pref", Constants.sPreferenceRequired);
                for (Lecture l : c.variables()) {
                    grEl.addElement("class").addAttribute("id", getId("class", l.getClassId()));
                }
            }
            if (c instanceof MinimizeNumberOfUsedGroupsOfTime) {
                Element grEl = grConstraintsEl.addElement("constraint").addAttribute("id",
                        getId("gr", String.valueOf(c.getId())));
                grEl.addAttribute("type", ((MinimizeNumberOfUsedGroupsOfTime) c).getConstraintName());
                grEl.addAttribute("pref", Constants.sPreferenceRequired);
                for (Lecture l : c.variables()) {
                    grEl.addElement("class").addAttribute("id", getId("class", l.getClassId()));
                }
            }
            if (c instanceof IgnoreStudentConflictsConstraint) {
                Element grEl = grConstraintsEl.addElement("constraint").addAttribute("id", getId("gr", String.valueOf(c.getId())));
                grEl.addAttribute("type", IgnoreStudentConflictsConstraint.REFERENCE);
                grEl.addAttribute("pref", Constants.sPreferenceRequired);
                for (Lecture l : c.variables()) {
                    grEl.addElement("class").addAttribute("id", getId("class", l.getClassId()));
                }
            }
        }
        for (ClassLimitConstraint clc : getModel().getClassLimitConstraints()) {
            Element grEl = grConstraintsEl.addElement("constraint").addAttribute("id",
                    getId("gr", String.valueOf(clc.getId())));
            grEl.addAttribute("type", "CLASS_LIMIT");
            grEl.addAttribute("pref", Constants.sPreferenceRequired);
            if (clc.getParentLecture() != null) {
                grEl.addElement("parentClass").addAttribute("id", getId("class", clc.getParentLecture().getClassId()));
            } else
                grEl.addAttribute("courseLimit", String.valueOf(clc.classLimit() - clc.getClassLimitDelta()));
            if (clc.getClassLimitDelta() != 0)
                grEl.addAttribute("delta", String.valueOf(clc.getClassLimitDelta()));
            if (iShowNames)
                grEl.addAttribute("name", clc.getName());
            for (Lecture l : clc.variables()) {
                grEl.addElement("class").addAttribute("id", getId("class", l.getClassId()));
            }
        }
        for (FlexibleConstraint gc : getModel().getFlexibleConstraints()) {
            Element flEl = grConstraintsEl.addElement("constraint").addAttribute("id",
                    getId("gr", String.valueOf(gc.getId())));
            flEl.addAttribute("reference", gc.getReference());
            flEl.addAttribute("owner", gc.getOwner());
            flEl.addAttribute("pref", gc.getPrologPreference());  
            flEl.addAttribute("type", gc.getType().toString());  
            for (Lecture l : gc.variables()) {
                flEl.addElement("class").addAttribute("id", getId("class", l.getClassId()));
            }
        }

        HashMap<Student, List<String>> students = new HashMap<Student, List<String>>();
        for (Lecture lecture : vars) {
            for (Student student : lecture.students()) {
                List<String> enrls = students.get(student);
                if (enrls == null) {
                    enrls = new ArrayList<String>();
                    students.put(student, enrls);
                }
                enrls.add(getId("class", lecture.getClassId()));
            }
        }

        Element studentsEl = root.addElement("students");
        Element groupsEl = root.addElement("groups");
        Map<StudentGroup, Element> groups = new HashMap<StudentGroup, Element>();
        for (Student student: new TreeSet<Student>(students.keySet())) {
            Element stEl = studentsEl.addElement("student").addAttribute("id", getId("student", student.getId()));
            if (iShowNames) {
                if (student.getAcademicArea() != null)
                    stEl.addAttribute("area", student.getAcademicArea());
                if (student.getAcademicClassification() != null)
                    stEl.addAttribute("classification", student.getAcademicClassification());
                if (student.getMajor() != null)
                    stEl.addAttribute("major", student.getMajor());
                if (student.getCurriculum() != null)
                    stEl.addAttribute("curriculum", student.getCurriculum());
            }
            for (Map.Entry<Long, Double> entry : student.getOfferingsMap().entrySet()) {
                Long offeringId = entry.getKey();
                Double weight = entry.getValue();
                Element offEl = stEl.addElement("offering")
                        .addAttribute("id", getId("offering", offeringId.toString()));
                if (weight.doubleValue() != 1.0)
                    offEl.addAttribute("weight", sStudentWeightFormat.format(weight));
                Double priority = student.getPriority(offeringId);
                if (priority != null)
                    offEl.addAttribute("priority", priority.toString());
                Long altId = student.getAlternative(offeringId);
                if (altId != null)
                    offEl.addAttribute("alternative", altId.toString());
            }
            if (iExportStudentSectioning || getModel().nrUnassignedVariables(getAssignment()) == 0 || student.getOfferingsMap().isEmpty()) {
                List<String> lectures = students.get(student);
                Collections.sort(lectures);
                for (String classId : lectures) {
                    stEl.addElement("class").addAttribute("id", classId);
                }
            }
            Map<Long, Set<Lecture>> canNotEnroll = student.canNotEnrollSections();
            if (canNotEnroll != null) {
                for (Set<Lecture> canNotEnrollLects: canNotEnroll.values()) {
                    for (Iterator<Lecture> i3 = canNotEnrollLects.iterator(); i3.hasNext();) {
                        stEl.addElement("prohibited-class")
                                .addAttribute("id", getId("class", (i3.next()).getClassId()));
                    }
                }
            }

            if (student.getCommitedPlacements() != null) {
                for (Placement placement : student.getCommitedPlacements()) {
                    stEl.addElement("class").addAttribute("id", getId("class", placement.variable().getClassId()));
                }
            }
            
            if (student.getInstructor() != null)
                stEl.addAttribute("instructor", getId("inst", student.getInstructor().getResourceId()));
            
            for (StudentGroup group: student.getGroups()) {
                Element groupEl = groups.get(group);
                if (groupEl == null) {
                    groupEl = groupsEl.addElement("group");
                    groupEl.addAttribute("id", getId("group", group.getId()));
                    if (group.getWeight() != 1.0)
                        groupEl.addAttribute("weight", String.valueOf(group.getWeight()));
                    if (iShowNames && group.getName() != null)
                        groupEl.addAttribute("name", group.getName());
                    groups.put(group, groupEl);
                }
                groupEl.addElement("student").addAttribute("id", getId("student", student.getId()));
            }
        }

        if (getModel().getProperties().getPropertyInt("MPP.GenTimePert", 0) > 0) {
            Element perturbationsEl = root.addElement("perturbations");
            int nrChanges = getModel().getProperties().getPropertyInt("MPP.GenTimePert", 0);
            List<Lecture> lectures = new ArrayList<Lecture>();
            while (lectures.size() < nrChanges) {
                Lecture lecture = ToolBox.random(getAssignment().assignedVariables());
                if (lecture.isCommitted() || lecture.timeLocations().size() <= 1 || lectures.contains(lecture))
                    continue;
                Placement placement = getAssignment().getValue(lecture);
                TimeLocation tl = placement.getTimeLocation();
                perturbationsEl.addElement("class").addAttribute("id", getId("class", lecture.getClassId()))
                        .addAttribute("days", sDF[7].format(Long.parseLong(Integer.toBinaryString(tl.getDayCode()))))
                        .addAttribute("start", String.valueOf(tl.getStartSlot())).addAttribute("length",
                                String.valueOf(tl.getLength()));
                lectures.add(lecture);
            }
        }

        for (Map.Entry<Long, String> entry : depts.entrySet()) {
            Long id = entry.getKey();
            String name = entry.getValue();
            if (iShowNames) {
                departmentsEl.addElement("department").addAttribute("id", getId("dept", id.toString())).addAttribute(
                        "name", name);
            }
        }
        if (departmentsEl.elements().isEmpty())
            root.remove(departmentsEl);
        
        if (iSaveConfig) {
            Element configuration = root.addElement("configuration");
            for (Map.Entry<Object, Object> e: getModel().getProperties().entrySet()) {
                    configuration.addElement("property").addAttribute("name", e.getKey().toString()).setText(e.getValue().toString());
            }
        }
    }
}