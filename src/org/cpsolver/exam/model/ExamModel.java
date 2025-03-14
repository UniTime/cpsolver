package org.cpsolver.exam.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;


import org.apache.logging.log4j.Logger;
import org.cpsolver.coursett.IdConvertor;
import org.cpsolver.exam.criteria.DistributionPenalty;
import org.cpsolver.exam.criteria.ExamCriterion;
import org.cpsolver.exam.criteria.ExamRotationPenalty;
import org.cpsolver.exam.criteria.InstructorBackToBackConflicts;
import org.cpsolver.exam.criteria.InstructorDirectConflicts;
import org.cpsolver.exam.criteria.InstructorDistanceBackToBackConflicts;
import org.cpsolver.exam.criteria.InstructorMoreThan2ADayConflicts;
import org.cpsolver.exam.criteria.InstructorNotAvailableConflicts;
import org.cpsolver.exam.criteria.LargeExamsPenalty;
import org.cpsolver.exam.criteria.PeriodIndexPenalty;
import org.cpsolver.exam.criteria.PeriodPenalty;
import org.cpsolver.exam.criteria.PeriodSizePenalty;
import org.cpsolver.exam.criteria.PerturbationPenalty;
import org.cpsolver.exam.criteria.RoomPenalty;
import org.cpsolver.exam.criteria.RoomPerturbationPenalty;
import org.cpsolver.exam.criteria.RoomSizePenalty;
import org.cpsolver.exam.criteria.RoomSplitDistancePenalty;
import org.cpsolver.exam.criteria.RoomSplitPenalty;
import org.cpsolver.exam.criteria.StudentBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentDirectConflicts;
import org.cpsolver.exam.criteria.StudentDistanceBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentMoreThan2ADayConflicts;
import org.cpsolver.exam.criteria.StudentNotAvailableConflicts;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.ModelWithContext;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.Callback;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.ifs.util.ToolBox;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Examination timetabling model. Exams {@link Exam} are modeled as variables,
 * rooms {@link ExamRoom} and students {@link ExamStudent} as constraints.
 * Assignment of an exam to time (modeled as non-overlapping periods
 * {@link ExamPeriod}) and space (set of rooms) is modeled using values
 * {@link ExamPlacement}. In order to be able to model individual period and
 * room preferences, period and room assignments are wrapped with
 * {@link ExamPeriodPlacement} and {@link ExamRoomPlacement} classes
 * respectively. Moreover, additional distribution constraint
 * {@link ExamDistributionConstraint} can be defined in the model. <br>
 * <br>
 * The objective function consists of the following criteria:
 * <ul>
 * <li>Direct student conflicts (a student is enrolled in two exams that are
 * scheduled at the same period, weighted by Exams.DirectConflictWeight)
 * <li>Back-to-Back student conflicts (a student is enrolled in two exams that
 * are scheduled in consecutive periods, weighted by
 * Exams.BackToBackConflictWeight). If Exams.IsDayBreakBackToBack is false,
 * there is no conflict between the last period and the first period of
 * consecutive days.
 * <li>Distance Back-to-Back student conflicts (same as Back-to-Back student
 * conflict, but the maximum distance between rooms in which both exam take
 * place is greater than Exams.BackToBackDistance, weighted by
 * Exams.DistanceBackToBackConflictWeight).
 * <li>More than two exams a day (a student is enrolled in three exams that are
 * scheduled at the same day, weighted by Exams.MoreThanTwoADayWeight).
 * <li>Period penalty (total of period penalties
 * {@link PeriodPenalty} of all assigned exams, weighted by
 * Exams.PeriodWeight).
 * <li>Room size penalty (total of room size penalties
 * {@link RoomSizePenalty} of all assigned exams, weighted by
 * Exams.RoomSizeWeight).
 * <li>Room split penalty (total of room split penalties
 * {@link RoomSplitPenalty} of all assigned exams, weighted
 * by Exams.RoomSplitWeight).
 * <li>Room penalty (total of room penalties
 * {@link RoomPenalty} of all assigned exams, weighted by
 * Exams.RoomWeight).
 * <li>Distribution penalty (total of distribution constraint weights
 * {@link ExamDistributionConstraint#getWeight()} of all soft distribution
 * constraints that are not satisfied, i.e.,
 * {@link ExamDistributionConstraint#isSatisfied(Assignment)} = false; weighted by
 * Exams.DistributionWeight).
 * <li>Direct instructor conflicts (an instructor is enrolled in two exams that
 * are scheduled at the same period, weighted by
 * Exams.InstructorDirectConflictWeight)
 * <li>Back-to-Back instructor conflicts (an instructor is enrolled in two exams
 * that are scheduled in consecutive periods, weighted by
 * Exams.InstructorBackToBackConflictWeight). If Exams.IsDayBreakBackToBack is
 * false, there is no conflict between the last period and the first period of
 * consecutive days.
 * <li>Distance Back-to-Back instructor conflicts (same as Back-to-Back
 * instructor conflict, but the maximum distance between rooms in which both
 * exam take place is greater than Exams.BackToBackDistance, weighted by
 * Exams.InstructorDistanceBackToBackConflictWeight).
 * <li>Room split distance penalty (if an examination is assigned between two or
 * three rooms, distance between these rooms can be minimized using this
 * criterion)
 * <li>Front load penalty (large exams can be penalized if assigned on or after
 * a certain period)
 * </ul>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamModel extends ModelWithContext<Exam, ExamPlacement, ExamContext> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ExamModel.class);
    private DataProperties iProperties = null;
    private int iMaxRooms = 4;
    private List<ExamPeriod> iPeriods = new ArrayList<ExamPeriod>();
    private List<ExamRoom> iRooms = new ArrayList<ExamRoom>();
    private List<ExamStudent> iStudents = new ArrayList<ExamStudent>();
    private List<ExamDistributionConstraint> iDistributionConstraints = new ArrayList<ExamDistributionConstraint>();
    private List<ExamInstructor> iInstructors = new ArrayList<ExamInstructor>();
    private ExamRoomSharing iRoomSharing = null;
    private boolean iCheckForPeriodOverlaps = false;

    private DistanceMetric iDistanceMetric = null;

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties
     */
    public ExamModel(DataProperties properties) {
        super();
        iProperties = properties;
        iMaxRooms = properties.getPropertyInt("Exams.MaxRooms", iMaxRooms);
        iCheckForPeriodOverlaps = properties.getPropertyBoolean("Exams.CheckForPeriodOverlaps", iCheckForPeriodOverlaps);
        iDistanceMetric = new DistanceMetric(properties);
        String roomSharingClass = properties.getProperty("Exams.RoomSharingClass");
        if (roomSharingClass != null) {
            try {
                iRoomSharing = (ExamRoomSharing)Class.forName(roomSharingClass).getConstructor(Model.class, DataProperties.class).newInstance(this, properties);
            } catch (Exception e) {
                sLog.error("Failed to instantiate room sharing class " + roomSharingClass + ", reason: " + e.getMessage());
            }
        }
        
        String criteria = properties.getProperty("Exams.Criteria",
                StudentDirectConflicts.class.getName() + ";" +
                StudentNotAvailableConflicts.class.getName() + ";" +
                StudentBackToBackConflicts.class.getName() + ";" +
                StudentDistanceBackToBackConflicts.class.getName() + ";" +
                StudentMoreThan2ADayConflicts.class.getName() + ";" +
                InstructorDirectConflicts.class.getName() + ";" +
                InstructorNotAvailableConflicts.class.getName() + ";" +
                InstructorBackToBackConflicts.class.getName() + ";" +
                InstructorDistanceBackToBackConflicts.class.getName() + ";" +
                InstructorMoreThan2ADayConflicts.class.getName() + ";" +
                PeriodPenalty.class.getName() + ";" +
                RoomPenalty.class.getName() + ";" +
                DistributionPenalty.class.getName() + ";" +
                RoomSplitPenalty.class.getName() + ";" +
                RoomSplitDistancePenalty.class.getName() + ";" +
                RoomSizePenalty.class.getName() + ";" +
                ExamRotationPenalty.class.getName() + ";" +
                LargeExamsPenalty.class.getName() + ";" +
                PeriodSizePenalty.class.getName() + ";" +
                PeriodIndexPenalty.class.getName() + ";" +
                PerturbationPenalty.class.getName() + ";" +
                RoomPerturbationPenalty.class.getName() + ";"
                );
        // Additional (custom) criteria
        criteria += ";" + properties.getProperty("Exams.AdditionalCriteria", "");
        for (String criterion: criteria.split("\\;")) {
            if (criterion == null || criterion.isEmpty()) continue;
            try {
                @SuppressWarnings("unchecked")
                Class<Criterion<Exam, ExamPlacement>> clazz = (Class<Criterion<Exam, ExamPlacement>>)Class.forName(criterion);
                addCriterion(clazz.newInstance());
            } catch (Exception e) {
                sLog.error("Unable to use " + criterion + ": " + e.getMessage());
            }
        }
    }
    
    public DistanceMetric getDistanceMetric() {
        return iDistanceMetric;
    }
    
    /**
     * True if there is an examination sharing model
     * @return true if there is an examination sharing model
     */
    public boolean hasRoomSharing() { return iRoomSharing != null; }
    
    /**
     * Return examination room sharing model
     * @return examination room sharing model, if set
     */
    public ExamRoomSharing getRoomSharing() { return iRoomSharing; }

    /**
     * Set examination sharing model
     * @param sharing examination sharing model
     */
    public void setRoomSharing(ExamRoomSharing sharing) {
        iRoomSharing = sharing;
    }

    /**
     * Initialization of the model
     */
    public void init() {
        for (Exam exam : variables()) {
            for (ExamRoomPlacement room : exam.getRoomPlacements()) {
                room.getRoom().addVariable(exam);
            }
        }
    }

    /**
     * Default maximum number of rooms (can be set by problem property
     * Exams.MaxRooms, or in the input xml file, property maxRooms)
     * @return default maximum number of rooms for an exam
     */
    public int getMaxRooms() {
        return iMaxRooms;
    }

    /**
     * Default maximum number of rooms (can be set by problem property
     * Exams.MaxRooms, or in the input xml file, property maxRooms)
     * @param maxRooms default maximum number of rooms for an exam
     */
    public void setMaxRooms(int maxRooms) {
        iMaxRooms = maxRooms;
    }
    
    /**
     * Check for examination periods that overlap with each other
     * @return true if examination periods can overlap with each other
     */
    public boolean isCheckForPeriodOverlaps() { return iCheckForPeriodOverlaps; }
    
    /**
     * Enable checking for period overlaps
     */
    public void setCheckForPeriodOverlaps(boolean check) {
        iCheckForPeriodOverlaps = check;
    }

    /**
     * Add a period
     * 
     * @param id
     *            period unique identifier
     * @param day
     *            day (e.g., 07/12/10)
     * @param time
     *            (e.g., 8:00am-10:00am)
     * @param length
     *            length of period in minutes
     * @param penalty
     *            period penalty
     * @return added period
     */
    public ExamPeriod addPeriod(Long id, String day, String time, int length, int penalty) {
        ExamPeriod lastPeriod = (iPeriods.isEmpty() ? null : (ExamPeriod) iPeriods.get(iPeriods.size() - 1));
        ExamPeriod p = new ExamPeriod(id, day, time, length, penalty);
        if (lastPeriod == null)
            p.setIndex(iPeriods.size(), 0, 0);
        else if (lastPeriod.getDayStr().equals(day)) {
            p.setIndex(iPeriods.size(), lastPeriod.getDay(), lastPeriod.getTime() + 1);
        } else
            p.setIndex(iPeriods.size(), lastPeriod.getDay() + 1, 0);
        if (lastPeriod != null) {
            lastPeriod.setNext(p);
            p.setPrev(lastPeriod);
        }
        iPeriods.add(p);
        return p;
    }

    /**
     * Number of days
     * @return number of days
     */
    public int getNrDays() {
        return (iPeriods.get(iPeriods.size() - 1)).getDay() + 1;
    }

    /**
     * Number of periods
     * @return number of periods
     */
    public int getNrPeriods() {
        return iPeriods.size();
    }

    /**
     * List of periods, use
     * {@link ExamModel#addPeriod(Long, String, String, int, int)} to add a
     * period
     * 
     * @return list of {@link ExamPeriod}
     */
    public List<ExamPeriod> getPeriods() {
        return iPeriods;
    }

    /** Period of given unique id 
     * @param id period unique id
     * @return the appropriate period
     **/
    public ExamPeriod getPeriod(Long id) {
        for (ExamPeriod period : iPeriods) {
            if (period.getId().equals(id))
                return period;
        }
        return null;
    }
    
    /**
     * True when back-to-back student conflict is to be encountered when a
     * student is enrolled into an exam that is on the last period of one day
     * and another exam that is on the first period of the consecutive day. It
     * can be set by problem property Exams.IsDayBreakBackToBack, or in the
     * input xml file, property isDayBreakBackToBack)
     * @return true if last exam on one day is back-to-back to the first exam of the following day
     * 
     */
    public boolean isDayBreakBackToBack() {
        return ((StudentBackToBackConflicts)getCriterion(StudentBackToBackConflicts.class)).isDayBreakBackToBack();
    }
    
    /**
     * Back-to-back distance, can be set by
     * problem property Exams.BackToBackDistance, or in the input xml file,
     * property backToBackDistance)
     * @return back-to-back distance in meters
     */
    public double getBackToBackDistance() {
        return ((StudentDistanceBackToBackConflicts)getCriterion(StudentDistanceBackToBackConflicts.class)).getBackToBackDistance();
    }

    /**
     * Objective function.
     * @return weighted sum of objective criteria
     */
    @Override
    public double getTotalValue(Assignment<Exam, ExamPlacement> assignment) {
        double total = 0;
        for (Criterion<Exam, ExamPlacement> criterion: getCriteria())
            total += criterion.getWeightedValue(assignment);
        return total;
    }

    /**
     * Return weighted individual objective criteria.
     * @param assignment current assignment
     * @return an array of weighted objective criteria
     */
    public double[] getTotalMultiValue(Assignment<Exam, ExamPlacement> assignment) {
        double[] total = new double[getCriteria().size()];
        int i = 0;
        for (Criterion<Exam, ExamPlacement> criterion: getCriteria())
            total[i++] = criterion.getWeightedValue(assignment);
        return total;
    }

    /**
     * String representation -- returns a list of values of objective criteria
     * @param assignment current assignment
     * @return comma separated list of {@link ExamCriterion#toString(Assignment)}
     */
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        Set<String> props = new TreeSet<String>();
        for (Criterion<Exam, ExamPlacement> criterion: getCriteria()) {
            String val = ((ExamCriterion)criterion).toString(assignment);
            if (!val.isEmpty())
                props.add(val);
        }
        return props.toString();
    }

    /**
     * Extended info table
     */
    @Override
    public Map<String, String> getExtendedInfo(Assignment<Exam, ExamPlacement> assignment) {
        Map<String, String> info = super.getExtendedInfo(assignment);
        /*
        info.put("Direct Conflicts [p]", String.valueOf(getNrDirectConflicts(true)));
        info.put("More Than 2 A Day Conflicts [p]", String.valueOf(getNrMoreThanTwoADayConflicts(true)));
        info.put("Back-To-Back Conflicts [p]", String.valueOf(getNrBackToBackConflicts(true)));
        info.put("Distance Back-To-Back Conflicts [p]", String.valueOf(getNrDistanceBackToBackConflicts(true)));
        info.put("Instructor Direct Conflicts [p]", String.valueOf(getNrInstructorDirectConflicts(true)));
        info.put("Instructor More Than 2 A Day Conflicts [p]", String.valueOf(getNrInstructorMoreThanTwoADayConflicts(true)));
        info.put("Instructor Back-To-Back Conflicts [p]", String.valueOf(getNrInstructorBackToBackConflicts(true)));
        info.put("Instructor Distance Back-To-Back Conflicts [p]", String.valueOf(getNrInstructorDistanceBackToBackConflicts(true)));
        info.put("Room Size Penalty [p]", String.valueOf(getRoomSizePenalty(true)));
        info.put("Room Split Penalty [p]", String.valueOf(getRoomSplitPenalty(true)));
        info.put("Period Penalty [p]", String.valueOf(getPeriodPenalty(true)));
        info.put("Period Size Penalty [p]", String.valueOf(getPeriodSizePenalty(true)));
        info.put("Period Index Penalty [p]", String.valueOf(getPeriodIndexPenalty(true)));
        info.put("Room Penalty [p]", String.valueOf(getRoomPenalty(true)));
        info.put("Distribution Penalty [p]", String.valueOf(getDistributionPenalty(true)));
        info.put("Perturbation Penalty [p]", String.valueOf(getPerturbationPenalty(true)));
        info.put("Room Perturbation Penalty [p]", String.valueOf(getRoomPerturbationPenalty(true)));
        info.put("Room Split Distance Penalty [p]", sDoubleFormat.format(getRoomSplitDistancePenalty(true)) + " / " + getNrRoomSplits(true));
        */
        info.put("Number of Periods", String.valueOf(getPeriods().size()));
        info.put("Number of Exams", String.valueOf(variables().size()));
        info.put("Number of Rooms", String.valueOf(getRooms().size()));
        info.put("Number of Students", String.valueOf(getStudents().size()));
        int nrStudentExams = 0;
        for (ExamStudent student : getStudents()) {
            nrStudentExams += student.getOwners().size();
        }
        info.put("Number of Student Exams", String.valueOf(nrStudentExams));
        int nrAltExams = 0, nrSmallExams = 0;
        for (Exam exam : variables()) {
            if (exam.hasAltSeating())
                nrAltExams++;
            if (exam.getMaxRooms() == 0)
                nrSmallExams++;
        }
        info.put("Number of Exams Requiring Alt Seating", String.valueOf(nrAltExams));
        info.put("Number of Small Exams (Exams W/O Room)", String.valueOf(nrSmallExams));
        int[] nbrMtgs = new int[11];
        for (int i = 0; i <= 10; i++)
            nbrMtgs[i] = 0;
        for (ExamStudent student : getStudents()) {
            nbrMtgs[Math.min(10, student.variables().size())]++;
        }
        for (int i = 0; i <= 10; i++) {
            if (nbrMtgs[i] == 0)
                continue;
            info.put("Number of Students with " + (i == 0 ? "no" : String.valueOf(i)) + (i == 10 ? " or more" : "")
                    + " meeting" + (i != 1 ? "s" : ""), String.valueOf(nbrMtgs[i]));
        }
        Map<Integer, Integer> penalty2count = new HashMap<Integer, Integer>();
        for (Exam exam: variables()) {
                ExamPlacement placement = assignment.getValue(exam);
                if (placement == null) continue;
                Integer preference = placement.getPeriodPlacement().getExamPenalty();
                Integer count = penalty2count.get(preference);
                penalty2count.put(preference, 1 + (count == null ? 0 : count.intValue())); 
        }
        if (!penalty2count.isEmpty()) {
            String value = null;
            for (Integer penalty: new TreeSet<Integer>(penalty2count.keySet())) {
                if (penalty == 0) continue;
                value = (value == null ? "" : value + ", ") + penalty2count.get(penalty) +  "&times; " + penalty;
            }
            if (value != null)
                info.put("Period Preferences", value);
        }
        return info;
    }

    /**
     * Problem properties
     * @return solver configuration
     */
    public DataProperties getProperties() {
        return iProperties;
    }

    /**
     * Problem rooms
     * 
     * @return list of {@link ExamRoom}
     */
    public List<ExamRoom> getRooms() {
        return iRooms;
    }

    /**
     * Problem students
     * 
     * @return list of {@link ExamStudent}
     */
    public List<ExamStudent> getStudents() {
        return iStudents;
    }

    /**
     * Problem instructors
     * 
     * @return list of {@link ExamInstructor}
     */
    public List<ExamInstructor> getInstructors() {
        return iInstructors;
    }

    /**
     * Distribution constraints
     * 
     * @return list of {@link ExamDistributionConstraint}
     */
    public List<ExamDistributionConstraint> getDistributionConstraints() {
        return iDistributionConstraints;
    }

    private String getId(boolean anonymize, String type, String id) {
        return (anonymize ? IdConvertor.getInstance().convert(type, id) : id);
    }

    /**
     * Save model (including its solution) into XML.
     * @param assignment current assignment
     * @return created XML document
     */
    public Document save(Assignment<Exam, ExamPlacement> assignment) {
        boolean saveInitial = getProperties().getPropertyBoolean("Xml.SaveInitial", true);
        boolean saveBest = getProperties().getPropertyBoolean("Xml.SaveBest", true);
        boolean saveSolution = getProperties().getPropertyBoolean("Xml.SaveSolution", true);
        boolean saveConflictTable = getProperties().getPropertyBoolean("Xml.SaveConflictTable", false);
        boolean saveParams = getProperties().getPropertyBoolean("Xml.SaveParameters", true);
        boolean anonymize = getProperties().getPropertyBoolean("Xml.Anonymize", false);
        boolean idconv = getProperties().getPropertyBoolean("Xml.ConvertIds", anonymize);
        Document document = DocumentHelper.createDocument();
        document.addComment("Examination Timetable");
        if (assignment != null && assignment.nrAssignedVariables() > 0) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (getProperties().getPropertyBoolean("Xml.ExtendedInfo", false) ? getExtendedInfo(assignment) : getInfo(assignment));
            for (String key : new TreeSet<String>(solutionInfo.keySet())) {
                String value = solutionInfo.get(key);
                comments.append("    " + key + ": " + value + "\n");
            }
            document.addComment(comments.toString());
        }
        Element root = document.addElement("examtt");
        root.addAttribute("version", "1.0");
        root.addAttribute("campus", getProperties().getProperty("Data.Initiative"));
        root.addAttribute("term", getProperties().getProperty("Data.Term"));
        root.addAttribute("year", getProperties().getProperty("Data.Year"));
        root.addAttribute("created", String.valueOf(new Date()));
        if (saveParams) {
            Map<String, String> params = new HashMap<String, String>();
            for (Criterion<Exam, ExamPlacement> criterion: getCriteria()) {
                if (criterion instanceof ExamCriterion)
                    ((ExamCriterion)criterion).getXmlParameters(params);
            }
            params.put("maxRooms", String.valueOf(getMaxRooms()));
            params.put("checkForPeriodOverlaps", isCheckForPeriodOverlaps() ? "true" : "false");
            Element parameters = root.addElement("parameters");
            for (String key: new TreeSet<String>(params.keySet())) {
                parameters.addElement("property").addAttribute("name", key).addAttribute("value", params.get(key));
            }
        }
        Element periods = root.addElement("periods");
        for (ExamPeriod period : getPeriods()) {
            Element periodEl = periods.addElement("period").addAttribute("id", getId(idconv, "period", String.valueOf(period.getId())))
                    .addAttribute("length", String.valueOf(period.getLength())).addAttribute("day", period.getDayStr())
                    .addAttribute("time", period.getTimeStr()).addAttribute("penalty",
                            String.valueOf(period.getPenalty()));
            if (period.getStartTime() != null)
                periodEl.addAttribute("start", period.getStartTime().toString());
        }
        Element rooms = root.addElement("rooms");
        for (ExamRoom room : getRooms()) {
            Element r = rooms.addElement("room");
            r.addAttribute("id", getId(idconv, "room", String.valueOf(room.getId())));
            if (!anonymize && room.hasName())
                r.addAttribute("name", room.getName());
            r.addAttribute("size", String.valueOf(room.getSize()));
            r.addAttribute("alt", String.valueOf(room.getAltSize()));
            if (!room.isHard())
                r.addAttribute("hard", "false");
            if (room.getCoordX() != null && room.getCoordY() != null)
                r.addAttribute("coordinates", room.getCoordX() + "," + room.getCoordY());
            if (room.getParentRoom() != null)
                r.addAttribute("parentId", getId(idconv, "room", String.valueOf(room.getParentRoom().getId())));
            for (ExamPeriod period : getPeriods()) {
                if (!room.isAvailable(period))
                    r.addElement("period").addAttribute("id",
                            getId(idconv, "period", String.valueOf(period.getId()))).addAttribute("available",
                            "false");
                else if (room.getPenalty(period) != 0)
                    r.addElement("period").addAttribute("id",
                            getId(idconv, "period", String.valueOf(period.getId()))).addAttribute("penalty",
                            String.valueOf(room.getPenalty(period)));
            }
            Map<Long, Integer> travelTimes = getDistanceMetric().getTravelTimes().get(room.getId());
            if (travelTimes != null)
                for (Map.Entry<Long, Integer> time: travelTimes.entrySet())
                    r.addElement("travel-time").addAttribute("id", getId(idconv, "room", time.getKey().toString())).addAttribute("minutes", time.getValue().toString());
        }
        Element exams = root.addElement("exams");
        for (Exam exam : variables()) {
            Element ex = exams.addElement("exam");
            ex.addAttribute("id", getId(idconv, "exam", String.valueOf(exam.getId())));
            if (!anonymize && exam.hasName())
                ex.addAttribute("name", exam.getName());
            ex.addAttribute("length", String.valueOf(exam.getLength()));
            if (exam.getSizeOverride() != null)
                ex.addAttribute("size", exam.getSizeOverride().toString());
            if (exam.getMinSize() != 0)
                ex.addAttribute("minSize", String.valueOf(exam.getMinSize()));
            ex.addAttribute("alt", (exam.hasAltSeating() ? "true" : "false"));
            if (exam.getMaxRooms() != getMaxRooms())
                ex.addAttribute("maxRooms", String.valueOf(exam.getMaxRooms()));
            if (exam.getPrintOffset() != null && !anonymize)
                ex.addAttribute("printOffset", exam.getPrintOffset().toString());
            if (!anonymize)
                ex.addAttribute("enrl", String.valueOf(exam.getStudents().size()));
            if (!anonymize)
                for (ExamOwner owner : exam.getOwners()) {
                    Element o = ex.addElement("owner");
                    o.addAttribute("id", getId(idconv, "owner", String.valueOf(owner.getId())));
                    o.addAttribute("name", owner.getName());
                }
            for (ExamPeriodPlacement period : exam.getPeriodPlacements()) {
                Element pe = ex.addElement("period").addAttribute("id",
                        getId(idconv, "period", String.valueOf(period.getId())));
                int penalty = period.getExamPenalty();
                if (penalty != 0)
                    pe.addAttribute("penalty", String.valueOf(penalty));
            }
            for (ExamRoomPlacement room : exam.getRoomPlacements()) {
                Element re = ex.addElement("room").addAttribute("id",
                        getId(idconv, "room", String.valueOf(room.getId())));
                if (room.getPenalty() != 0)
                    re.addAttribute("penalty", String.valueOf(room.getPenalty()));
                if (room.getMaxPenalty() != 100)
                    re.addAttribute("maxPenalty", String.valueOf(room.getMaxPenalty()));
            }
            if (exam.hasAveragePeriod())
                ex.addAttribute("average", String.valueOf(exam.getAveragePeriod()));
            ExamPlacement p = (assignment == null ? null : assignment.getValue(exam));
            if (p != null && saveSolution) {
                Element asg = ex.addElement("assignment");
                asg.addElement("period").addAttribute("id",
                        getId(idconv, "period", String.valueOf(p.getPeriod().getId())));
                for (ExamRoomPlacement r : p.getRoomPlacements()) {
                    asg.addElement("room").addAttribute("id", getId(idconv, "room", String.valueOf(r.getId())));
                }
            }
            p = exam.getInitialAssignment();
            if (p != null && saveInitial) {
                Element ini = ex.addElement("initial");
                ini.addElement("period").addAttribute("id",
                        getId(idconv, "period", String.valueOf(p.getPeriod().getId())));
                for (ExamRoomPlacement r : p.getRoomPlacements()) {
                    ini.addElement("room").addAttribute("id", getId(idconv, "room", String.valueOf(r.getId())));
                }
            }
            p = exam.getBestAssignment();
            if (p != null && saveBest) {
                Element ini = ex.addElement("best");
                ini.addElement("period").addAttribute("id",
                        getId(idconv, "period", String.valueOf(p.getPeriod().getId())));
                for (ExamRoomPlacement r : p.getRoomPlacements()) {
                    ini.addElement("room").addAttribute("id", getId(idconv, "room", String.valueOf(r.getId())));
                }
            }
            if (iRoomSharing != null)
                iRoomSharing.save(exam, ex, anonymize ? IdConvertor.getInstance() : null);
        }
        Element students = root.addElement("students");
        for (ExamStudent student : getStudents()) {
            Element s = students.addElement("student");
            s.addAttribute("id", getId(idconv, "student", String.valueOf(student.getId())));
            for (Exam ex : student.variables()) {
                Element x = s.addElement("exam").addAttribute("id",
                        getId(idconv, "exam", String.valueOf(ex.getId())));
                if (!anonymize)
                    for (ExamOwner owner : ex.getOwners(student)) {
                        x.addElement("owner").addAttribute("id",
                                getId(idconv, "owner", String.valueOf(owner.getId())));
                    }
            }
            for (ExamPeriod period : getPeriods()) {
                if (!student.isAvailable(period))
                    s.addElement("period").addAttribute("id",
                            getId(idconv, "period", String.valueOf(period.getId()))).addAttribute("available",
                            "false");
            }
        }
        Element instructors = root.addElement("instructors");
        for (ExamInstructor instructor : getInstructors()) {
            Element i = instructors.addElement("instructor");
            i.addAttribute("id", getId(idconv, "instructor", String.valueOf(instructor.getId())));
            if (!anonymize && instructor.hasName())
                i.addAttribute("name", instructor.getName());
            for (Exam ex : instructor.variables()) {
                Element x = i.addElement("exam").addAttribute("id",
                        getId(idconv, "exam", String.valueOf(ex.getId())));
                if (!anonymize)
                    for (ExamOwner owner : ex.getOwners(instructor)) {
                        x.addElement("owner").addAttribute("id",
                                getId(idconv, "owner", String.valueOf(owner.getId())));
                    }
            }
            for (ExamPeriod period : getPeriods()) {
                if (!instructor.isAvailable(period))
                    i.addElement("period").addAttribute("id",
                            getId(idconv, "period", String.valueOf(period.getId()))).addAttribute("available",
                            "false");
            }
        }
        Element distConstraints = root.addElement("constraints");
        for (ExamDistributionConstraint distConstraint : getDistributionConstraints()) {
            Element dc = distConstraints.addElement(distConstraint.getTypeString());
            dc.addAttribute("id", getId(idconv, "constraint", String.valueOf(distConstraint.getId())));
            if (!distConstraint.isHard()) {
                dc.addAttribute("hard", "false");
                dc.addAttribute("weight", String.valueOf(distConstraint.getWeight()));
            }
            for (Exam exam : distConstraint.variables()) {
                dc.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(exam.getId())));
            }
        }
        if (saveConflictTable && assignment != null) {
            Element conflicts = root.addElement("conflicts");
            Map<ExamStudent, Set<Exam>> studentsOfPreviousPeriod = null;
            for (ExamPeriod period : getPeriods()) {
                Map<ExamStudent, Set<Exam>> studentsOfPeriod = getStudentsOfPeriod(assignment, period);
                for (Map.Entry<ExamStudent, Set<Exam>> entry : studentsOfPeriod.entrySet()) {
                    ExamStudent student = entry.getKey();
                    Set<Exam> examsOfStudent = entry.getValue();
                    if (examsOfStudent.size() > 1) {
                        Element dir = conflicts.addElement("direct").addAttribute("student", getId(idconv, "student", String.valueOf(student.getId())));
                        for (Exam exam : examsOfStudent) {
                            dir.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(exam.getId())));
                        }
                    }
                    if (examsOfStudent.size() > 0 && studentsOfPreviousPeriod != null && (isDayBreakBackToBack() || period.prev().getDay() == period.getDay())) {
                        Set<Exam> previousExamsOfStudent = studentsOfPreviousPeriod.get(student);
                        if (previousExamsOfStudent != null) {
                            for (Exam ex1 : previousExamsOfStudent)
                                for (Exam ex2 : examsOfStudent) {
                                    Element btb = conflicts.addElement("back-to-back").addAttribute("student", getId(idconv, "student", String.valueOf(student.getId())));
                                    btb.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(ex1.getId())));
                                    btb.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(ex2.getId())));
                                    if (getBackToBackDistance() >= 0 && period.prev().getDay() == period.getDay()) {
                                        double dist = (assignment.getValue(ex1)).getDistanceInMeters(assignment.getValue(ex2));
                                        if (dist > 0)
                                            btb.addAttribute("distance", String.valueOf(dist));
                                    }
                                }
                        }
                    }
                }
                if (period.next() == null || period.next().getDay() != period.getDay()) {
                    Map<ExamStudent, Set<Exam>> studentsOfDay = getStudentsOfDay(assignment, period);
                    for (Map.Entry<ExamStudent, Set<Exam>> entry : studentsOfDay.entrySet()) {
                        ExamStudent student = entry.getKey();
                        Set<Exam> examsOfStudent = entry.getValue();
                        if (examsOfStudent.size() > 2) {
                            Element mt2 = conflicts.addElement("more-2-day").addAttribute("student", getId(idconv, "student", String.valueOf(student.getId())));
                            for (Exam exam : examsOfStudent) {
                                mt2.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(exam.getId())));
                            }
                        }
                    }
                }
                studentsOfPreviousPeriod = studentsOfPeriod;
            }
            /*
            Element conflicts = root.addElement("conflicts");
            for (ExamStudent student : getStudents()) {
                for (ExamPeriod period : getPeriods()) {
                    int nrExams = student.getExams(assignment, period).size();
                    if (nrExams > 1) {
                        Element dir = conflicts.addElement("direct").addAttribute("student", getId(idconv, "student", String.valueOf(student.getId())));
                        for (Exam exam : student.getExams(assignment, period)) {
                            dir.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(exam.getId())));
                        }
                    }
                    if (nrExams > 0) {
                        if (period.next() != null && !student.getExams(assignment, period.next()).isEmpty()
                                && (!isDayBreakBackToBack() || period.next().getDay() == period.getDay())) {
                            for (Exam ex1 : student.getExams(assignment, period)) {
                                for (Exam ex2 : student.getExams(assignment, period.next())) {
                                    Element btb = conflicts.addElement("back-to-back").addAttribute("student", getId(idconv, "student", String.valueOf(student.getId())));
                                    btb.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(ex1.getId())));
                                    btb.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(ex2.getId())));
                                    if (getBackToBackDistance() >= 0) {
                                        double dist = (assignment.getValue(ex1)).getDistanceInMeters(assignment.getValue(ex2));
                                        if (dist > 0)
                                            btb.addAttribute("distance", String.valueOf(dist));
                                    }
                                }
                            }
                        }
                    }
                    if (period.next() == null || period.next().getDay() != period.getDay()) {
                        int nrExamsADay = student.getExamsADay(assignment, period.getDay()).size();
                        if (nrExamsADay > 2) {
                            Element mt2 = conflicts.addElement("more-2-day").addAttribute("student", getId(idconv, "student", String.valueOf(student.getId())));
                            for (Exam exam : student.getExamsADay(assignment, period.getDay())) {
                                mt2.addElement("exam").addAttribute("id", getId(idconv, "exam", String.valueOf(exam.getId())));
                            }
                        }
                    }
                }
            }
            */
        }
        return document;
    }

    /**
     * Load model (including its solution) from XML.
     * @param document XML document
     * @param assignment assignment to be loaded
     * @return true if successfully loaded
     */
    public boolean load(Document document, Assignment<Exam, ExamPlacement> assignment) {
        return load(document, assignment, null);
    }

    /**
     * Load model (including its solution) from XML.
     * @param document XML document
     * @param assignment assignment to be loaded
     * @param saveBest callback executed once the best assignment is loaded and assigned
     * @return true if successfully loaded
     */
    public boolean load(Document document, Assignment<Exam, ExamPlacement> assignment, Callback saveBest) {
        boolean loadInitial = getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        boolean loadBest = getProperties().getPropertyBoolean("Xml.LoadBest", true);
        boolean loadSolution = getProperties().getPropertyBoolean("Xml.LoadSolution", true);
        boolean loadParams = getProperties().getPropertyBoolean("Xml.LoadParameters", false);
        Integer softPeriods = getProperties().getPropertyInteger("Exam.SoftPeriods", null);
        Integer softRooms = getProperties().getPropertyInteger("Exam.SoftRooms", null);
        Integer softDistributions = getProperties().getPropertyInteger("Exam.SoftDistributions", null);
        Element root = document.getRootElement();
        if (!"examtt".equals(root.getName()))
            return false;
        if (root.attribute("campus") != null)
            getProperties().setProperty("Data.Campus", root.attributeValue("campus"));
        else if (root.attribute("initiative") != null)
            getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));
        if (root.attribute("term") != null)
            getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attribute("year") != null)
            getProperties().setProperty("Data.Year", root.attributeValue("year"));
        if (loadParams && root.element("parameters") != null) {
            Map<String,String> params = new HashMap<String, String>();
            for (Iterator<?> i = root.element("parameters").elementIterator("property"); i.hasNext();) {
                Element e = (Element) i.next();
                params.put(e.attributeValue("name"), e.attributeValue("value"));
            }
            for (Criterion<Exam, ExamPlacement> criterion: getCriteria()) {
                if (criterion instanceof ExamCriterion)
                    ((ExamCriterion)criterion).setXmlParameters(params);
            }
            try {
                setMaxRooms(Integer.valueOf(params.get("maxRooms")));
            } catch (NumberFormatException e) {} catch (NullPointerException e) {}
            setCheckForPeriodOverlaps("true".equalsIgnoreCase(params.get("checkForPeriodOverlaps")));
        }
        for (Iterator<?> i = root.element("periods").elementIterator("period"); i.hasNext();) {
            Element e = (Element) i.next();
            ExamPeriod p = addPeriod(Long.valueOf(e.attributeValue("id")), e.attributeValue("day"), e.attributeValue("time"), Integer
                    .parseInt(e.attributeValue("length")), Integer.parseInt(e.attributeValue("penalty") == null ? e
                    .attributeValue("weight", "0") : e.attributeValue("penalty")));
            if (e.attributeValue("start") != null)
                p.setStartTime(Integer.valueOf(e.attributeValue("start")));
        }
        HashMap<Long, ExamRoom> rooms = new HashMap<Long, ExamRoom>();
        HashMap<String, ArrayList<ExamRoom>> roomGroups = new HashMap<String, ArrayList<ExamRoom>>();
        HashMap<ExamRoom, Long> roomPartitions = new HashMap<ExamRoom, Long>();
        for (Iterator<?> i = root.element("rooms").elementIterator("room"); i.hasNext();) {
            Element e = (Element) i.next();
            String coords = e.attributeValue("coordinates");
            ExamRoom room = new ExamRoom(this, Long.parseLong(e.attributeValue("id")), e.attributeValue("name"),
                    Integer.parseInt(e.attributeValue("size")), Integer.parseInt(e.attributeValue("alt")),
                    (coords == null ? null : Double.valueOf(coords.substring(0, coords.indexOf(',')))),
                    (coords == null ? null : Double.valueOf(coords.substring(coords.indexOf(',') + 1))));
            room.setHard("true".equalsIgnoreCase(e.attributeValue("hard", "true")));
            addConstraint(room);
            getRooms().add(room);
            rooms.put(Long.valueOf(room.getId()), room);
            for (Iterator<?> j = e.elementIterator("period"); j.hasNext();) {
                Element pe = (Element) j.next();
                ExamPeriod period = getPeriod(Long.valueOf(pe.attributeValue("id")));
                if (period == null) continue;
                if ("false".equals(pe.attributeValue("available"))) {
                    if (softRooms == null)
                        room.setAvailable(period, false);
                    else
                        room.setPenalty(period, softRooms);
                } else
                    room.setPenalty(period, Integer.parseInt(pe.attributeValue("penalty")));
            }
            String av = e.attributeValue("available");
            if (av != null) {
                for (int j = 0; j < getPeriods().size(); j++)
                    if ('0' == av.charAt(j))
                        room.setAvailable(getPeriods().get(j), false);
            }
            String g = e.attributeValue("groups");
            if (g != null) {
                for (StringTokenizer s = new StringTokenizer(g, ","); s.hasMoreTokens();) {
                    String gr = s.nextToken();
                    ArrayList<ExamRoom> roomsThisGrop = roomGroups.get(gr);
                    if (roomsThisGrop == null) {
                        roomsThisGrop = new ArrayList<ExamRoom>();
                        roomGroups.put(gr, roomsThisGrop);
                    }
                    roomsThisGrop.add(room);
                }
            }
            if (e.attributeValue("parentId") != null)
                roomPartitions.put(room, Long.valueOf(e.attributeValue("parentId")));
            for (Iterator<?> j = e.elementIterator("travel-time"); j.hasNext();) {
                Element travelTimeEl = (Element)j.next();
                getDistanceMetric().addTravelTime(room.getId(),
                        Long.valueOf(travelTimeEl.attributeValue("id")),
                        Integer.valueOf(travelTimeEl.attributeValue("minutes")));
            }
        }
        for (Map.Entry<ExamRoom, Long> partition: roomPartitions.entrySet()) {
            ExamRoom parent = rooms.get(partition.getValue());
            if (parent != null)
                parent.addPartition(partition.getKey());
        }
        ArrayList<ExamPlacement> assignments = new ArrayList<ExamPlacement>();
        HashMap<Long, Exam> exams = new HashMap<Long, Exam>();
        HashMap<Long, ExamOwner> courseSections = new HashMap<Long, ExamOwner>();
        for (Iterator<?> i = root.element("exams").elementIterator("exam"); i.hasNext();) {
            Element e = (Element) i.next();
            ArrayList<ExamPeriodPlacement> periodPlacements = new ArrayList<ExamPeriodPlacement>();
            if (softPeriods != null) {
                for (ExamPeriod period: getPeriods()) {
                    int penalty = softPeriods;
                    for (Iterator<?> j = e.elementIterator("period"); j.hasNext();) {
                        Element pe = (Element) j.next();
                        if (period.getId().equals(Long.valueOf(pe.attributeValue("id")))) {
                            penalty = Integer.parseInt(pe.attributeValue("penalty", "0"));
                            break;
                        }
                    }
                    periodPlacements.add(new ExamPeriodPlacement(period, penalty));
                }
            } else {
                for (Iterator<?> j = e.elementIterator("period"); j.hasNext();) {
                    Element pe = (Element) j.next();
                    ExamPeriod p = getPeriod(Long.valueOf(pe.attributeValue("id")));
                    if (p != null)
                        periodPlacements.add(new ExamPeriodPlacement(p, Integer.parseInt(pe.attributeValue("penalty", "0"))));
                }
            }
            ArrayList<ExamRoomPlacement> roomPlacements = new ArrayList<ExamRoomPlacement>();
            if (softRooms != null) {
                for (ExamRoom room: getRooms()) {
                    boolean av = false;
                    for (ExamPeriodPlacement p: periodPlacements) {
                        if (room.isAvailable(p.getPeriod()) && room.getPenalty(p.getPeriod()) != softRooms) { av = true; break; }
                    }
                    if (!av) continue;
                    int penalty = softRooms, maxPenalty = softRooms;
                    for (Iterator<?> j = e.elementIterator("room"); j.hasNext();) {
                        Element re = (Element) j.next();
                        if (room.getId() == Long.parseLong(re.attributeValue("id"))) {
                            penalty = Integer.parseInt(re.attributeValue("penalty", "0"));
                            maxPenalty = Integer.parseInt(re.attributeValue("maxPenalty", softRooms.toString()));
                        }
                    }
                    roomPlacements.add(new ExamRoomPlacement(room, penalty, maxPenalty));
                }                
            } else {
                for (Iterator<?> j = e.elementIterator("room"); j.hasNext();) {
                    Element re = (Element) j.next();
                    ExamRoomPlacement room = new ExamRoomPlacement(rooms.get(Long.valueOf(re.attributeValue("id"))),
                            Integer.parseInt(re.attributeValue("penalty", "0")),
                            Integer.parseInt(re.attributeValue("maxPenalty", "100")));
                    if (room.getRoom().isAvailable())
                        roomPlacements.add(room);
                }
            }
            String g = e.attributeValue("groups");
            if (g != null) {
                HashMap<ExamRoom, Integer> allRooms = new HashMap<ExamRoom, Integer>();
                for (StringTokenizer s = new StringTokenizer(g, ","); s.hasMoreTokens();) {
                    String gr = s.nextToken();
                    ArrayList<ExamRoom> roomsThisGrop = roomGroups.get(gr);
                    if (roomsThisGrop != null)
                        for (ExamRoom r : roomsThisGrop)
                            allRooms.put(r, 0);
                }
                for (Iterator<?> j = e.elementIterator("original-room"); j.hasNext();) {
                    allRooms.put((rooms.get(Long.valueOf(((Element) j.next()).attributeValue("id")))), Integer.valueOf(-1));
                }
                for (Map.Entry<ExamRoom, Integer> entry : allRooms.entrySet()) {
                    ExamRoomPlacement room = new ExamRoomPlacement(entry.getKey(), entry.getValue(), 100);
                    roomPlacements.add(room);
                }
                if (periodPlacements.isEmpty()) {
                    for (ExamPeriod p : getPeriods()) {
                        periodPlacements.add(new ExamPeriodPlacement(p, 0));
                    }
                }
            }
            Exam exam = new Exam(Long.parseLong(e.attributeValue("id")), e.attributeValue("name"), Integer.parseInt(e
                    .attributeValue("length")), "true".equals(e.attributeValue("alt")),
                    (e.attribute("maxRooms") == null ? getMaxRooms() : Integer.parseInt(e.attributeValue("maxRooms"))),
                    Integer.parseInt(e.attributeValue("minSize", "0")), periodPlacements, roomPlacements);
            if (e.attributeValue("size") != null)
                exam.setSizeOverride(Integer.valueOf(e.attributeValue("size")));
            if (e.attributeValue("printOffset") != null)
                exam.setPrintOffset(Integer.valueOf(e.attributeValue("printOffset")));
            exams.put(Long.valueOf(exam.getId()), exam);
            addVariable(exam);
            if (e.attribute("average") != null)
                exam.setAveragePeriod(Integer.parseInt(e.attributeValue("average")));
            Element asg = e.element("assignment");
            if (asg != null && loadSolution) {
                Element per = asg.element("period");
                if (per != null) {
                    HashSet<ExamRoomPlacement> rp = new HashSet<ExamRoomPlacement>();
                    for (Iterator<?> j = asg.elementIterator("room"); j.hasNext();)
                        rp.add(exam.getRoomPlacement(Long.parseLong(((Element) j.next()).attributeValue("id"))));
                    ExamPeriodPlacement pp = exam.getPeriodPlacement(Long.valueOf(per.attributeValue("id")));
                    if (pp != null)
                        assignments.add(new ExamPlacement(exam, pp, rp));
                }
            }
            Element ini = e.element("initial");
            if (ini != null && loadInitial) {
                Element per = ini.element("period");
                if (per != null) {
                    HashSet<ExamRoomPlacement> rp = new HashSet<ExamRoomPlacement>();
                    for (Iterator<?> j = ini.elementIterator("room"); j.hasNext();)
                        rp.add(exam.getRoomPlacement(Long.parseLong(((Element) j.next()).attributeValue("id"))));
                    ExamPeriodPlacement pp = exam.getPeriodPlacement(Long.valueOf(per.attributeValue("id")));
                    if (pp != null)
                        exam.setInitialAssignment(new ExamPlacement(exam, pp, rp));
                }
            }
            Element best = e.element("best");
            if (best != null && loadBest) {
                Element per = best.element("period");
                if (per != null) {
                    HashSet<ExamRoomPlacement> rp = new HashSet<ExamRoomPlacement>();
                    for (Iterator<?> j = best.elementIterator("room"); j.hasNext();)
                        rp.add(exam.getRoomPlacement(Long.parseLong(((Element) j.next()).attributeValue("id"))));
                    ExamPeriodPlacement pp = exam.getPeriodPlacement(Long.valueOf(per.attributeValue("id")));
                    if (pp != null)
                        exam.setBestAssignment(new ExamPlacement(exam, pp, rp), 0);
                }
            }
            for (Iterator<?> j = e.elementIterator("owner"); j.hasNext();) {
                Element f = (Element) j.next();
                ExamOwner owner = new ExamOwner(exam, Long.parseLong(f.attributeValue("id")), f.attributeValue("name"));
                exam.getOwners().add(owner);
                courseSections.put(Long.valueOf(owner.getId()), owner);
            }
            if (iRoomSharing != null)
                iRoomSharing.load(exam, e);
        }
        for (Iterator<?> i = root.element("students").elementIterator("student"); i.hasNext();) {
            Element e = (Element) i.next();
            ExamStudent student = new ExamStudent(this, Long.parseLong(e.attributeValue("id")));
            for (Iterator<?> j = e.elementIterator("exam"); j.hasNext();) {
                Element x = (Element) j.next();
                Exam ex = exams.get(Long.valueOf(x.attributeValue("id")));
                student.addVariable(ex);
                for (Iterator<?> k = x.elementIterator("owner"); k.hasNext();) {
                    Element f = (Element) k.next();
                    ExamOwner owner = courseSections.get(Long.valueOf(f.attributeValue("id")));
                    student.getOwners().add(owner);
                    owner.getStudents().add(student);
                }
            }
            String available = e.attributeValue("available");
            if (available != null)
                for (ExamPeriod period : getPeriods()) {
                    if (available.charAt(period.getIndex()) == '0')
                        student.setAvailable(period.getIndex(), false);
                }
            for (Iterator<?> j = e.elementIterator("period"); j.hasNext();) {
                Element pe = (Element) j.next();
                ExamPeriod period = getPeriod(Long.valueOf(pe.attributeValue("id")));
                if (period == null) continue;
                if ("false".equals(pe.attributeValue("available")))
                    student.setAvailable(period.getIndex(), false);
            }
            addConstraint(student);
            getStudents().add(student);
        }
        if (root.element("instructors") != null)
            for (Iterator<?> i = root.element("instructors").elementIterator("instructor"); i.hasNext();) {
                Element e = (Element) i.next();
                ExamInstructor instructor = new ExamInstructor(this, Long.parseLong(e.attributeValue("id")), e
                        .attributeValue("name"));
                for (Iterator<?> j = e.elementIterator("exam"); j.hasNext();) {
                    Element x = (Element) j.next();
                    Exam ex = exams.get(Long.valueOf(x.attributeValue("id")));
                    instructor.addVariable(ex);
                    for (Iterator<?> k = x.elementIterator("owner"); k.hasNext();) {
                        Element f = (Element) k.next();
                        ExamOwner owner = courseSections.get(Long.valueOf(f.attributeValue("id")));
                        instructor.getOwners().add(owner);
                        owner.getIntructors().add(instructor);
                    }
                }
                String available = e.attributeValue("available");
                if (available != null)
                    for (ExamPeriod period : getPeriods()) {
                        if (available.charAt(period.getIndex()) == '0')
                            instructor.setAvailable(period.getIndex(), false);
                    }
                for (Iterator<?> j = e.elementIterator("period"); j.hasNext();) {
                    Element pe = (Element) j.next();
                    ExamPeriod period = getPeriod(Long.valueOf(pe.attributeValue("id")));
                    if (period == null) continue;
                    if ("false".equals(pe.attributeValue("available")))
                        instructor.setAvailable(period.getIndex(), false);
                }
                addConstraint(instructor);
                getInstructors().add(instructor);
            }
        if (root.element("constraints") != null)
            for (Iterator<?> i = root.element("constraints").elementIterator(); i.hasNext();) {
                Element e = (Element) i.next();
                ExamDistributionConstraint dc = new ExamDistributionConstraint(Long.parseLong(e.attributeValue("id")),
                        e.getName(),
                        softDistributions != null ? false : "true".equals(e.attributeValue("hard", "true")),
                        (softDistributions != null && "true".equals(e.attributeValue("hard", "true")) ? softDistributions : Integer.parseInt(e.attributeValue("weight", "0"))));
                for (Iterator<?> j = e.elementIterator("exam"); j.hasNext();) {
                    dc.addVariable(exams.get(Long.valueOf(((Element) j.next()).attributeValue("id"))));
                }
                addConstraint(dc);
                getDistributionConstraints().add(dc);
            }
        init();
        if (loadBest && saveBest != null && assignment != null) {
            for (Exam exam : variables()) {
                ExamPlacement placement = exam.getBestAssignment();
                if (placement == null)
                    continue;
                assignment.assign(0, placement);
            }
            saveBest.execute();
            for (Exam exam : variables()) {
                if (assignment.getValue(exam) != null)
                    assignment.unassign(0, exam);
            }
        }
        if (assignment != null) {
            for (ExamPlacement placement : assignments) {
                Exam exam = placement.variable();
                Set<ExamPlacement> conf = conflictValues(assignment, placement);
                if (!conf.isEmpty()) {
                    for (Map.Entry<Constraint<Exam, ExamPlacement>, Set<ExamPlacement>> entry : conflictConstraints(assignment, placement).entrySet()) {
                        Constraint<Exam, ExamPlacement> constraint = entry.getKey();
                        Set<ExamPlacement> values = entry.getValue();
                        if (constraint instanceof ExamStudent) {
                            ((ExamStudent) constraint).setAllowDirectConflicts(true);
                            exam.setAllowDirectConflicts(true);
                            for (ExamPlacement p : values)
                                p.variable().setAllowDirectConflicts(true);
                        }
                    }
                    conf = conflictValues(assignment, placement);
                }
                if (conf.isEmpty()) {
                    assignment.assign(0, placement);
                } else {
                    sLog.error("Unable to assign " + exam.getInitialAssignment().getName() + " to exam " + exam.getName());
                    sLog.error("Conflicts:" + ToolBox.dict2string(conflictConstraints(assignment, exam.getInitialAssignment()), 2));
                }
            }
        }
        return true;
    }

    @Override
    public ExamContext createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new ExamContext(this, assignment);
    }
    
    public Map<ExamStudent, Set<Exam>> getStudentsOfPeriod(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        return getContext(assignment).getStudentsOfPeriod(period.getIndex());
    }
    
    public Map<ExamStudent, Set<Exam>> getStudentsOfDay(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        return getContext(assignment).getStudentsOfDay(period.getDay());
    }
    
    public Map<ExamStudent, Set<Exam>> getStudentsOfDay(Assignment<Exam, ExamPlacement> assignment, int day) {
        return getContext(assignment).getStudentsOfDay(day);
    }
    
    public Map<ExamInstructor, Set<Exam>> getInstructorsOfPeriod(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        return getContext(assignment).getInstructorsOfPeriod(period.getIndex());
    }
    
    public Map<ExamInstructor, Set<Exam>> getInstructorsOfDay(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        return getContext(assignment).getInstructorsOfDay(period.getDay());
    }
    
    public Map<ExamInstructor, Set<Exam>> getInstructorsOfDay(Assignment<Exam, ExamPlacement> assignment, int day) {
        return getContext(assignment).getInstructorsOfDay(day);
    }

}
