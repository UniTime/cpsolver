package net.sf.cpsolver.studentsct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import net.sf.cpsolver.coursett.IdConvertor;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.studentsct.constraint.LinkedSections;
import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Choice;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;
import net.sf.cpsolver.studentsct.reservation.CourseReservation;
import net.sf.cpsolver.studentsct.reservation.CurriculumReservation;
import net.sf.cpsolver.studentsct.reservation.DummyReservation;
import net.sf.cpsolver.studentsct.reservation.GroupReservation;
import net.sf.cpsolver.studentsct.reservation.IndividualReservation;
import net.sf.cpsolver.studentsct.reservation.Reservation;

/**
 * Save student sectioning solution into an XML file.
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'>
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
 * <td>Xml.SaveOnlineSectioningInfo</td>
 * <td>{@link Boolean}</td>
 * <td>If true, save online sectioning info (i.e., expected and held space of
 * each section)</td>
 * </tr>
 * <tr>
 * <td>Xml.SaveStudentInfo</td>
 * <td>{@link Boolean}</td>
 * <td>If true, save student information (i.e., academic area classification,
 * major, minor)</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * Usage:<br>
 * <code>
 * new StudentSectioningXMLSaver(solver).save(new File("solution.xml"));<br> 
 * </code>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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

public class StudentSectioningXMLSaver extends StudentSectioningSaver {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(StudentSectioningXMLSaver.class);
    private static DecimalFormat[] sDF = { new DecimalFormat(""), new DecimalFormat("0"), new DecimalFormat("00"),
            new DecimalFormat("000"), new DecimalFormat("0000"), new DecimalFormat("00000"),
            new DecimalFormat("000000"), new DecimalFormat("0000000") };
    private static DecimalFormat sStudentWeightFormat = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
    private File iOutputFolder = null;

    private boolean iSaveBest = false;
    private boolean iSaveInitial = false;
    private boolean iSaveCurrent = false;
    private boolean iSaveOnlineSectioningInfo = false;
    private boolean iSaveStudentInfo = true;

    private boolean iConvertIds = false;
    private boolean iShowNames = false;
    
    static {
        sStudentWeightFormat.setRoundingMode(RoundingMode.DOWN);
    }

    /**
     * Constructor
     * 
     * @param solver
     *            student sectioning solver
     */
    public StudentSectioningXMLSaver(Solver<Request, Enrollment> solver) {
        super(solver);
        iOutputFolder = new File(getModel().getProperties().getProperty("General.Output",
                "." + File.separator + "output"));
        iSaveBest = getModel().getProperties().getPropertyBoolean("Xml.SaveBest", true);
        iSaveInitial = getModel().getProperties().getPropertyBoolean("Xml.SaveInitial", true);
        iSaveCurrent = getModel().getProperties().getPropertyBoolean("Xml.SaveCurrent", false);
        iSaveOnlineSectioningInfo = getModel().getProperties().getPropertyBoolean("Xml.SaveOnlineSectioningInfo", true);
        iSaveStudentInfo = getModel().getProperties().getPropertyBoolean("Xml.SaveStudentInfo", true);
        iShowNames = getModel().getProperties().getPropertyBoolean("Xml.ShowNames", true);
        iConvertIds = getModel().getProperties().getPropertyBoolean("Xml.ConvertIds", false);
    }

    /** Convert bitset to a bit string */
    private static String bitset2string(BitSet b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length(); i++)
            sb.append(b.get(i) ? "1" : "0");
        return sb.toString();
    }

    /** Generate id for given object with the given id */
    private String getId(String type, String id) {
        if (!iConvertIds)
            return id.toString();
        return IdConvertor.getInstance().convert(type, id);
    }

    /** Generate id for given object with the given id */
    private String getId(String type, Number id) {
        return getId(type, id.toString());
    }

    /** Generate id for given object with the given id */
    private String getId(String type, long id) {
        return getId(type, String.valueOf(id));
    }

    /** Save an XML file */
    @Override
    public void save() throws Exception {
        save(null);
    }

    /**
     * Save an XML file
     * 
     * @param outFile
     *            output file
     */
    public void save(File outFile) throws Exception {
        if (outFile == null) {
            outFile = new File(iOutputFolder, "solution.xml");
        } else if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }
        sLogger.debug("Writting XML data to:" + outFile);

        Document document = DocumentHelper.createDocument();
        document.addComment("Student Sectioning");
        
        populate(document);

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
            IdConvertor.getInstance().save();
    }
    
    /**
     * Fill in all the data into the given document
     * @param document document to be populated
     */
    protected void populate(Document document) {
        if ((iSaveCurrent || iSaveBest)) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (getSolution() == null ? getModel().getExtendedInfo() : getSolution()
                    .getExtendedInfo());
            for (String key : new TreeSet<String>(solutionInfo.keySet())) {
                String value = solutionInfo.get(key);
                comments.append("    " + key + ": " + value + "\n");
            }
            document.addComment(comments.toString());
        }

        Element root = document.addElement("sectioning");
        root.addAttribute("version", "1.0");
        root.addAttribute("initiative", getModel().getProperties().getProperty("Data.Initiative"));
        root.addAttribute("term", getModel().getProperties().getProperty("Data.Term"));
        root.addAttribute("year", getModel().getProperties().getProperty("Data.Year"));
        root.addAttribute("created", String.valueOf(new Date()));

        saveOfferings(root);

        saveStudents(root);
        
        saveLinkedSections(root);
        
        saveTravelTimes(root);

        if (iShowNames) {
            Progress.getInstance(getModel()).save(root);
        }
    }
    
    /**
     * Save offerings
     * @param root document root
     */
    protected void saveOfferings(Element root) {
        Element offeringsEl = root.addElement("offerings");
        for (Offering offering : getModel().getOfferings()) {
            Element offeringEl = offeringsEl.addElement("offering");
            saveOffering(offeringEl, offering);
            saveReservations(offeringEl, offering);
        }
    }
    
    /**
     * Save given offering
     * @param offeringEl offering element to be populated
     * @param offering offering to be saved
     */
    protected void saveOffering(Element offeringEl, Offering offering) {
        offeringEl.addAttribute("id", getId("offering", offering.getId()));
        if (iShowNames)
            offeringEl.addAttribute("name", offering.getName());
        for (Course course : offering.getCourses()) {
            Element courseEl = offeringEl.addElement("course");
            saveCourse(courseEl, course);
        }
        for (Config config : offering.getConfigs()) {
            Element configEl = offeringEl.addElement("config");
            saveConfig(configEl, config);
        }
    }
    
    /**
     * Save given course
     * @param courseEl course element to be populated
     * @param course course to be saved
     */
    protected void saveCourse(Element courseEl, Course course) {
        courseEl.addAttribute("id", getId("course", course.getId()));
        if (iShowNames)
            courseEl.addAttribute("subjectArea", course.getSubjectArea());
        if (iShowNames)
            courseEl.addAttribute("courseNbr", course.getCourseNumber());
        if (iShowNames && course.getLimit() >= 0)
            courseEl.addAttribute("limit", String.valueOf(course.getLimit()));
        if (iShowNames && course.getProjected() != 0)
            courseEl.addAttribute("projected", String.valueOf(course.getProjected()));
    }
    
    /**
     * Save given config
     * @param configEl config element to be populated
     * @param config config to be saved
     */
    protected void saveConfig(Element configEl, Config config) {
        configEl.addAttribute("id", getId("config", config.getId()));
        if (config.getLimit() >= 0)
            configEl.addAttribute("limit", String.valueOf(config.getLimit()));
        if (iShowNames)
            configEl.addAttribute("name", config.getName());
        for (Subpart subpart : config.getSubparts()) {
            Element subpartEl = configEl.addElement("subpart");
            saveSubpart(subpartEl, subpart);
        }
    }
    
    /**
     * Save scheduling subpart
     * @param subpartEl subpart element to be populated
     * @param subpart subpart to be saved
     */
    protected void saveSubpart(Element subpartEl, Subpart subpart) {
        subpartEl.addAttribute("id", getId("subpart", subpart.getId()));
        subpartEl.addAttribute("itype", subpart.getInstructionalType());
        if (subpart.getParent() != null)
            subpartEl.addAttribute("parent", getId("subpart", subpart.getParent().getId()));
        if (iShowNames)
            subpartEl.addAttribute("name", subpart.getName());
        if (subpart.isAllowOverlap())
            subpartEl.addAttribute("allowOverlap", "true");
        for (Section section : subpart.getSections()) {
            Element sectionEl = subpartEl.addElement("section");
            saveSection(sectionEl, section);
        }
    }
    
    /**
     * Save section
     * @param sectionEl section element to be populated
     * @param section section to be saved
     */
    protected void saveSection(Element sectionEl, Section section) {
        sectionEl.addAttribute("id", getId("section", section.getId()));
        sectionEl.addAttribute("limit", String.valueOf(section.getLimit()));
        if (section.getNameByCourse() != null)
            for (Map.Entry<Long, String> entry: section.getNameByCourse().entrySet())
                sectionEl.addElement("cname").addAttribute("id", entry.getKey().toString()).setText(entry.getValue());
        if (section.getParent() != null)
            sectionEl.addAttribute("parent", getId("section", section.getParent().getId()));
        if (iShowNames && section.getChoice().getInstructorIds() != null)
            sectionEl.addAttribute("instructorIds", section.getChoice().getInstructorIds());
        if (iShowNames && section.getChoice().getInstructorNames() != null)
            sectionEl.addAttribute("instructorNames", section.getChoice().getInstructorNames());
        if (iShowNames)
            sectionEl.addAttribute("name", section.getName());
        if (section.getPlacement() != null) {
            TimeLocation tl = section.getPlacement().getTimeLocation();
            if (tl != null) {
                Element timeLocationEl = sectionEl.addElement("time");
                timeLocationEl.addAttribute("days", sDF[7].format(Long.parseLong(Integer
                        .toBinaryString(tl.getDayCode()))));
                timeLocationEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                timeLocationEl.addAttribute("length", String.valueOf(tl.getLength()));
                if (tl.getBreakTime() != 0)
                    timeLocationEl.addAttribute("breakTime", String.valueOf(tl.getBreakTime()));
                if (iShowNames && tl.getTimePatternId() != null)
                    timeLocationEl.addAttribute("pattern", getId("timePattern", tl.getTimePatternId()));
                if (iShowNames && tl.getDatePatternId() != null)
                    timeLocationEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                if (iShowNames && tl.getDatePatternName() != null
                        && tl.getDatePatternName().length() > 0)
                    timeLocationEl.addAttribute("datePatternName", tl.getDatePatternName());
                timeLocationEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                if (iShowNames)
                    timeLocationEl.setText(tl.getLongName());
            }
            for (RoomLocation rl : section.getRooms()) {
                Element roomLocationEl = sectionEl.addElement("room");
                roomLocationEl.addAttribute("id", getId("room", rl.getId()));
                if (iShowNames && rl.getBuildingId() != null)
                    roomLocationEl.addAttribute("building", getId("building", rl.getBuildingId()));
                if (iShowNames && rl.getName() != null)
                    roomLocationEl.addAttribute("name", rl.getName());
                roomLocationEl.addAttribute("capacity", String.valueOf(rl.getRoomSize()));
                if (rl.getPosX() != null && rl.getPosY() != null)
                    roomLocationEl.addAttribute("location", rl.getPosX() + "," + rl.getPosY());
                if (rl.getIgnoreTooFar())
                    roomLocationEl.addAttribute("ignoreTooFar", "true");
            }
        }
        if (iSaveOnlineSectioningInfo) {
            if (section.getSpaceHeld() != 0.0)
                sectionEl.addAttribute("hold", sStudentWeightFormat.format(section.getSpaceHeld()));
            if (section.getSpaceExpected() != 0.0)
                sectionEl.addAttribute("expect", sStudentWeightFormat
                        .format(section.getSpaceExpected()));
        }
        if (section.getIgnoreConflictWithSectionIds() != null && !section.getIgnoreConflictWithSectionIds().isEmpty()) {
            Element ignoreEl = sectionEl.addElement("no-conflicts");
            for (Long sectionId: section.getIgnoreConflictWithSectionIds())
                ignoreEl.addElement("section").addAttribute("id", getId("section", sectionId));
        }
    }
    
    /**
     * Save reservations of the given offering
     * @param offeringEl offering element to be populated with reservations
     * @param offering offering which reservations are to be saved
     */
    protected void saveReservations(Element offeringEl, Offering offering) {
        if (!offering.getReservations().isEmpty()) {
            for (Reservation r: offering.getReservations()) {
                saveReservation(offeringEl.addElement("reservation"), r);
            }
        }
    }
    
    /**
     * Save reservation
     * @param reservationEl reservation element to be populated
     * @param reservation reservation to be saved
     */
    protected void saveReservation(Element reservationEl, Reservation reservation) {
        reservationEl.addAttribute("id", getId("reservation", reservation.getId()));
        reservationEl.addAttribute("expired", reservation.isExpired() ? "true" : "false");
        if (reservation instanceof GroupReservation) {
            GroupReservation gr = (GroupReservation)reservation;
            reservationEl.addAttribute("type", "group");
            for (Long studentId: gr.getStudentIds())
                reservationEl.addElement("student").addAttribute("id", getId("student", studentId));
            if (gr.getReservationLimit() >= 0.0)
                reservationEl.addAttribute("limit", String.valueOf(gr.getReservationLimit()));
        } else if (reservation instanceof IndividualReservation) {
            reservationEl.addAttribute("type", "individual");
            for (Long studentId: ((IndividualReservation)reservation).getStudentIds())
                reservationEl.addElement("student").addAttribute("id", getId("student", studentId));
        } else if (reservation instanceof CurriculumReservation) {
            reservationEl.addAttribute("type", "curriculum");
            CurriculumReservation cr = (CurriculumReservation)reservation;
            if (cr.getReservationLimit() >= 0.0)
                reservationEl.addAttribute("limit", String.valueOf(cr.getReservationLimit()));
            reservationEl.addAttribute("area", cr.getAcademicArea());
            for (String clasf: cr.getClassifications())
                reservationEl.addElement("classification").addAttribute("code", clasf);
            for (String major: cr.getMajors())
                reservationEl.addElement("major").addAttribute("code", major);
        } else if (reservation instanceof CourseReservation) {
            reservationEl.addAttribute("type", "course");
            CourseReservation cr = (CourseReservation)reservation;
            reservationEl.addAttribute("course", getId("course",cr.getCourse().getId()));
        } else if (reservation instanceof DummyReservation) {
            reservationEl.addAttribute("type", "dummy");
        }
        for (Config config: reservation.getConfigs())
            reservationEl.addElement("config").addAttribute("id", getId("config", config.getId()));
        for (Map.Entry<Subpart, Set<Section>> entry: reservation.getSections().entrySet()) {
            for (Section section: entry.getValue()) {
                reservationEl.addElement("section").addAttribute("id", getId("section", section.getId()));
            }
        }
    }
    
    /**
     * Save students
     * @param root document root
     */
    protected void saveStudents(Element root) {
        Element studentsEl = root.addElement("students");
        for (Student student : getModel().getStudents()) {
            Element studentEl = studentsEl.addElement("student");
            saveStudent(studentEl, student);
            for (Request request : student.getRequests()) {
                saveRequest(studentEl, request);
            }
        }
    }
    
    /**
     * Save student
     * @param studentEl student element to be populated
     * @param student student to be saved
     */
    protected void saveStudent(Element studentEl, Student student) {
        studentEl.addAttribute("id", getId("student", student.getId()));
        if (iShowNames) {
            if (student.getExternalId() != null && !student.getExternalId().isEmpty())
                studentEl.addAttribute("externalId", student.getExternalId());
            if (student.getName() != null && !student.getName().isEmpty())
                studentEl.addAttribute("name", student.getName());
            if (student.getStatus() != null && !student.getStatus().isEmpty())
                studentEl.addAttribute("status", student.getStatus());
        }
        if (student.isDummy())
            studentEl.addAttribute("dummy", "true");
        if (iSaveStudentInfo) {
            for (AcademicAreaCode aac : student.getAcademicAreaClasiffications()) {
                Element aacEl = studentEl.addElement("classification");
                if (aac.getArea() != null)
                    aacEl.addAttribute("area", aac.getArea());
                if (aac.getCode() != null)
                    aacEl.addAttribute("code", aac.getCode());
            }
            for (AcademicAreaCode aac : student.getMajors()) {
                Element aacEl = studentEl.addElement("major");
                if (aac.getArea() != null)
                    aacEl.addAttribute("area", aac.getArea());
                if (aac.getCode() != null)
                    aacEl.addAttribute("code", aac.getCode());
            }
            for (AcademicAreaCode aac : student.getMinors()) {
                Element aacEl = studentEl.addElement("minor");
                if (aac.getArea() != null)
                    aacEl.addAttribute("area", aac.getArea());
                if (aac.getCode() != null)
                    aacEl.addAttribute("code", aac.getCode());
            }
        }
    }
    
    /**
     * Save request
     * @param studentEl student element to be populated
     * @param request request to be saved
     */
    protected void saveRequest(Element studentEl, Request request) {
        if (request instanceof FreeTimeRequest) {
            saveFreeTimeRequest(studentEl.addElement("freeTime"), (FreeTimeRequest) request);
        } else if (request instanceof CourseRequest) {
            saveCourseRequest(studentEl.addElement("course"), (CourseRequest) request);
        }
    }
    
    /**
     * Save free time request
     * @param requestEl request element to be populated
     * @param request free time request to be saved 
     */
    protected void saveFreeTimeRequest(Element requestEl, FreeTimeRequest request) {
        requestEl.addAttribute("id", getId("request", request.getId()));
        requestEl.addAttribute("priority", String.valueOf(request.getPriority()));
        if (request.isAlternative())
            requestEl.addAttribute("alternative", "true");
        if (request.getWeight() != 1.0)
            requestEl.addAttribute("weight", sStudentWeightFormat.format(request.getWeight()));
        TimeLocation tl = request.getTime();
        if (tl != null) {
            requestEl.addAttribute("days", sDF[7].format(Long.parseLong(Integer.toBinaryString(tl
                    .getDayCode()))));
            requestEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
            requestEl.addAttribute("length", String.valueOf(tl.getLength()));
            if (iShowNames && tl.getDatePatternId() != null)
                requestEl.addAttribute("datePattern", tl.getDatePatternId().toString());
            requestEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
            if (iShowNames)
                requestEl.setText(tl.getLongName());
        }
        if (iSaveInitial && request.getInitialAssignment() != null) {
            requestEl.addElement("initial");
        }
        if (iSaveCurrent && request.getAssignment() != null) {
            requestEl.addElement("current");
        }
        if (iSaveBest && request.getBestAssignment() != null) {
            requestEl.addElement("best");
        }
    }
    
    /**
     * Save course request 
     * @param requestEl request element to be populated
     * @param request course request to be saved
     */
    protected void saveCourseRequest(Element requestEl, CourseRequest request) {
        requestEl.addAttribute("id", getId("request", request.getId()));
        requestEl.addAttribute("priority", String.valueOf(request.getPriority()));
        if (request.isAlternative())
            requestEl.addAttribute("alternative", "true");
        if (request.getWeight() != 1.0)
            requestEl.addAttribute("weight", sStudentWeightFormat.format(request.getWeight()));
        requestEl.addAttribute("waitlist", request.isWaitlist() ? "true" : "false");
        if (request.getTimeStamp() != null)
            requestEl.addAttribute("timeStamp", request.getTimeStamp().toString());
        boolean first = true;
        for (Course course : request.getCourses()) {
            if (first)
                requestEl.addAttribute("course", getId("course", course.getId()));
            else
                requestEl.addElement("alternative").addAttribute("course", getId("course", course.getId()));
            first = false;
        }
        for (Choice choice : request.getWaitlistedChoices()) {
            Element choiceEl = requestEl.addElement("waitlisted");
            choiceEl.addAttribute("offering", getId("offering", choice.getOffering().getId()));
            choiceEl.setText(choice.getId());
        }
        for (Choice choice : request.getSelectedChoices()) {
            Element choiceEl = requestEl.addElement("selected");
            choiceEl.addAttribute("offering", getId("offering", choice.getOffering().getId()));
            choiceEl.setText(choice.getId());
        }
        if (iSaveInitial && request.getInitialAssignment() != null) {
            saveEnrollment(requestEl.addElement("initial"), request.getInitialAssignment());
        }
        if (iSaveCurrent && request.getAssignment() != null) {
            saveEnrollment(requestEl.addElement("current"), request.getAssignment());
        }
        if (iSaveBest && request.getBestAssignment() != null) {
            saveEnrollment(requestEl.addElement("best"), request.getBestAssignment());
        }
    }
    
    /**
     * Save enrollment
     * @param assignmentEl assignment element to be populated
     * @param enrollment enrollment to be saved
     */
    protected void saveEnrollment(Element assignmentEl, Enrollment enrollment) {
        if (enrollment.getReservation() != null)
            assignmentEl.addAttribute("reservation", getId("reservation", enrollment.getReservation().getId()));
        for (Section section : enrollment.getSections()) {
            Element sectionEl = assignmentEl.addElement("section").addAttribute("id",
                    getId("section", section.getId()));
            if (iShowNames)
                sectionEl.setText(section.getName() + " " +
                        (section.getTime() == null ? " Arr Hrs" : " " + section.getTime().getLongName()) +
                        (section.getNrRooms() == 0 ? "" : " " + section.getPlacement().getRoomName(",")) +
                        (section.getChoice().getInstructorNames() == null ? "" : " " + section.getChoice().getInstructorNames()));
        }
    }
    
    /**
     * Save linked sections
     * @param root document root
     */
    protected void saveLinkedSections(Element root) {
        Element constrainstEl = root.addElement("constraints");
        for (LinkedSections linkedSections: getModel().getLinkedSections()) {
            Element linkEl = constrainstEl.addElement("linked-sections");
            for (Offering offering: linkedSections.getOfferings())
                for (Subpart subpart: linkedSections.getSubparts(offering))
                    for (Section section: linkedSections.getSections(subpart))
                        linkEl.addElement("section")
                            .addAttribute("offering", getId("offering", offering.getId()))
                            .addAttribute("id", getId("section", section.getId()));
        }
    }
    
    /**
     * Save travel times
     * @param root document root
     */
    protected void saveTravelTimes(Element root) {
        if (getModel().getDistanceConflict() != null) {
            Map<Long, Map<Long, Integer>> travelTimes = getModel().getDistanceConflict().getDistanceMetric().getTravelTimes();
            if (travelTimes != null) {
                Element travelTimesEl = root.addElement("travel-times");
                for (Map.Entry<Long, Map<Long, Integer>> e1: travelTimes.entrySet())
                    for (Map.Entry<Long, Integer> e2: e1.getValue().entrySet())
                        travelTimesEl.addElement("travel-time")
                            .addAttribute("id1", getId("room", e1.getKey().toString()))
                            .addAttribute("id2", getId("room", e2.getKey().toString()))
                            .addAttribute("minutes", e2.getValue().toString());
            }
        }
    }

}
