package org.cpsolver.studentsct;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.constraint.FixedAssignments;
import org.cpsolver.studentsct.filter.StudentFilter;
import org.cpsolver.studentsct.model.AreaClassificationMajor;
import org.cpsolver.studentsct.model.Choice;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Student.BackToBackPreference;
import org.cpsolver.studentsct.model.Student.ModalityPreference;
import org.cpsolver.studentsct.model.Student.StudentPriority;
import org.cpsolver.studentsct.model.RequestGroup;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.StudentGroup;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.model.Unavailability;
import org.cpsolver.studentsct.reservation.CourseReservation;
import org.cpsolver.studentsct.reservation.CourseRestriction;
import org.cpsolver.studentsct.reservation.CurriculumOverride;
import org.cpsolver.studentsct.reservation.CurriculumReservation;
import org.cpsolver.studentsct.reservation.CurriculumRestriction;
import org.cpsolver.studentsct.reservation.DummyReservation;
import org.cpsolver.studentsct.reservation.GroupReservation;
import org.cpsolver.studentsct.reservation.IndividualReservation;
import org.cpsolver.studentsct.reservation.IndividualRestriction;
import org.cpsolver.studentsct.reservation.LearningCommunityReservation;
import org.cpsolver.studentsct.reservation.Reservation;
import org.cpsolver.studentsct.reservation.ReservationOverride;
import org.cpsolver.studentsct.reservation.Restriction;
import org.cpsolver.studentsct.reservation.UniversalOverride;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Load student sectioning model from an XML file.
 * 
 * <br>
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
 * <td>Path of an XML file to be loaded</td>
 * </tr>
 * <tr>
 * <td>Xml.LoadBest</td>
 * <td>{@link Boolean}</td>
 * <td>If true, load best assignments</td>
 * </tr>
 * <tr>
 * <td>Xml.LoadInitial</td>
 * <td>{@link Boolean}</td>
 * <td>If false, load initial assignments</td>
 * </tr>
 * <tr>
 * <td>Xml.LoadCurrent</td>
 * <td>{@link Boolean}</td>
 * <td>If true, load current assignments</td>
 * </tr>
 * <tr>
 * <td>Xml.LoadOfferings</td>
 * <td>{@link Boolean}</td>
 * <td>If true, load offerings (and their stucture, i.e., courses,
 * configurations, subparts and sections)</td>
 * </tr>
 * <tr>
 * <td>Xml.LoadStudents</td>
 * <td>{@link Boolean}</td>
 * <td>If true, load students (and their requests)</td>
 * </tr>
 * <tr>
 * <td>Xml.StudentFilter</td>
 * <td>{@link StudentFilter}</td>
 * <td>If provided, students are filtered by the given student filter</td>
 * </tr>
 * </table>
 * 
 * <br>
 * <br>
 * Usage:
 * <pre><code>
 * StudentSectioningModel model = new StudentSectioningModel(cfg);<br>
 * new StudentSectioningXMLLoader(model).load();<br>
 * </code></pre>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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

public class StudentSectioningXMLLoader extends StudentSectioningLoader {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(StudentSectioningXMLLoader.class);

    private File iInputFile;
    private File iTimetableFile = null;
    private boolean iLoadBest = false;
    private boolean iLoadInitial = false;
    private boolean iLoadCurrent = false;
    private boolean iLoadOfferings = true;
    private boolean iLoadStudents = true;
    private StudentFilter iStudentFilter = null;
    private boolean iWaitlistCritical = false;
    private boolean iMoveCriticalUp = false;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     * @param assignment current assignment
     */
    public StudentSectioningXMLLoader(StudentSectioningModel model, Assignment<Request, Enrollment> assignment) {
        super(model, assignment);
        iInputFile = new File(getModel().getProperties().getProperty("General.Input",
                "." + File.separator + "solution.xml"));
        if (getModel().getProperties().getProperty("General.InputTimetable") != null)
            iTimetableFile = new File(getModel().getProperties().getProperty("General.InputTimetable"));
        iLoadBest = getModel().getProperties().getPropertyBoolean("Xml.LoadBest", true);
        iLoadInitial = getModel().getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        iLoadCurrent = getModel().getProperties().getPropertyBoolean("Xml.LoadCurrent", true);
        iLoadOfferings = getModel().getProperties().getPropertyBoolean("Xml.LoadOfferings", true);
        iLoadStudents = getModel().getProperties().getPropertyBoolean("Xml.LoadStudents", true);
        iWaitlistCritical = getModel().getProperties().getPropertyBoolean("Xml.WaitlistCritical", false);
        iMoveCriticalUp = getModel().getProperties().getPropertyBoolean("Xml.MoveCriticalUp", false);
        if (getModel().getProperties().getProperty("Xml.StudentFilter") != null) {
            try {
                iStudentFilter = (StudentFilter) Class.forName(
                        getModel().getProperties().getProperty("Xml.StudentFilter")).getConstructor(new Class[] {})
                        .newInstance(new Object[] {});
            } catch (Exception e) {
                sLogger.error("Unable to create student filter, reason: " + e.getMessage(), e);
            }
        }
    }

    /** Set input file (e.g., if it is not set by General.Input property) 
     * @param inputFile input file
     **/
    public void setInputFile(File inputFile) {
        iInputFile = inputFile;
    }

    /** Set student filter 
     * @param filter student filter 
     **/
    public void setStudentFilter(StudentFilter filter) {
        iStudentFilter = filter;
    }

    /** Set whether to load students 
     * @param loadStudents true if students are to be loaded
     **/
    public void setLoadStudents(boolean loadStudents) {
        iLoadStudents = loadStudents;
    }

    /** Set whether to load offerings 
     * @param loadOfferings true if instructional offerings are to be loaded
     **/
    public void setLoadOfferings(boolean loadOfferings) {
        iLoadOfferings = loadOfferings;
    }

    /** Create BitSet from a bit string */
    private static BitSet createBitSet(String bitString) {
        BitSet ret = new BitSet(bitString.length());
        for (int i = 0; i < bitString.length(); i++)
            if (bitString.charAt(i) == '1')
                ret.set(i);
        return ret;
    }

    /** Load the file */
    @Override
    public void load() throws Exception {
        sLogger.debug("Reading XML data from " + iInputFile);

        Document document = (new SAXReader()).read(iInputFile);
        Element root = document.getRootElement();

        load(root);
    }
    
    public void load(Document document) {
        Element root = document.getRootElement();
        
        if (getModel() != null && root.element("travel-times") != null)
            loadTravelTimes(root.element("travel-times"), getModel().getDistanceMetric());
        
        Progress.getInstance(getModel()).load(root, true);
        Progress.getInstance(getModel()).message(Progress.MSGLEVEL_STAGE, "Restoring from backup ...");

        Map<Long, Offering> offeringTable = new HashMap<Long, Offering>();
        Map<Long, Course> courseTable = new HashMap<Long, Course>();

        if (root.element("offerings") != null) {
            loadOfferings(root.element("offerings"), offeringTable, courseTable, null);
        }
        
        List<Enrollment> bestEnrollments = new ArrayList<Enrollment>();
        List<Enrollment> currentEnrollments = new ArrayList<Enrollment>();
        if (root.element("students") != null) {
            loadStudents(root.element("students"), offeringTable, courseTable, bestEnrollments, currentEnrollments);
        }
        
        if (root.element("constraints") != null) 
            loadLinkedSections(root.element("constraints"), offeringTable);
        
        if (!bestEnrollments.isEmpty()) assignBest(bestEnrollments);
        if (!currentEnrollments.isEmpty()) assignCurrent(currentEnrollments);
        
        if (iMoveCriticalUp) moveCriticalRequestsUp();
        
        boolean hasFixed = false;
        for (Request r: getModel().variables()) {
            if (r instanceof CourseRequest && ((CourseRequest)r).isFixed()) {
                hasFixed = true; break;
            }
        }
        if (hasFixed)
            getModel().addGlobalConstraint(new FixedAssignments());
    }
    
    /**
     * Load data from the given XML root
     * @param root document root
     * @throws DocumentException when XML cannot be read or parsed
     */
    protected void load(Element root) throws DocumentException {
        sLogger.debug("Root element: " + root.getName());
        if (!"sectioning".equals(root.getName())) {
            sLogger.error("Given XML file is not student sectioning problem.");
            return;
        }
        
        if (iLoadOfferings && getModel().getDistanceConflict() != null && root.element("travel-times") != null)
            loadTravelTimes(root.element("travel-times"), getModel().getDistanceConflict().getDistanceMetric());
        
        Map<Long, Placement> timetable = null;
        if (iTimetableFile != null) {
            sLogger.info("Reading timetable from " + iTimetableFile + " ...");
            Document timetableDocument = (new SAXReader()).read(iTimetableFile);
            Element timetableRoot = timetableDocument.getRootElement();
            if (!"timetable".equals(timetableRoot.getName())) {
                sLogger.error("Given XML file is not course timetabling problem.");
                return;
            }
            timetable = loadTimetable(timetableRoot);
        }

        Progress.getInstance(getModel()).load(root, true);
        Progress.getInstance(getModel()).message(Progress.MSGLEVEL_STAGE, "Restoring from backup ...");

        if (root.attributeValue("term") != null)
            getModel().getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attributeValue("year") != null)
            getModel().getProperties().setProperty("Data.Year", root.attributeValue("year"));
        if (root.attributeValue("initiative") != null)
            getModel().getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));

        Map<Long, Offering> offeringTable = new HashMap<Long, Offering>();
        Map<Long, Course> courseTable = new HashMap<Long, Course>();

        if (iLoadOfferings && root.element("offerings") != null) {
            loadOfferings(root.element("offerings"), offeringTable, courseTable, timetable);
        } else {
            for (Offering offering : getModel().getOfferings()) {
                offeringTable.put(Long.valueOf(offering.getId()), offering);
                for (Course course : offering.getCourses()) {
                    courseTable.put(Long.valueOf(course.getId()), course);
                }
            }
        }

        List<Enrollment> bestEnrollments = new ArrayList<Enrollment>();
        List<Enrollment> currentEnrollments = new ArrayList<Enrollment>();
        if (iLoadStudents && root.element("students") != null) {
            loadStudents(root.element("students"), offeringTable, courseTable, bestEnrollments, currentEnrollments);
        }
        
        if (iLoadOfferings && root.element("constraints") != null) 
            loadLinkedSections(root.element("constraints"), offeringTable);
                
        if (!bestEnrollments.isEmpty()) assignBest(bestEnrollments);
        if (!currentEnrollments.isEmpty()) assignCurrent(currentEnrollments);
        
        if (iMoveCriticalUp) moveCriticalRequestsUp();

        sLogger.debug("Model successfully loaded.");
    }
    
    /**
     * Load offerings
     * @param offeringsEl offerings element
     * @param offeringTable offering table
     * @param courseTable course table
     * @param timetable provided timetable (null if to be loaded from the given document)
     */
    protected void loadOfferings(Element offeringsEl, Map<Long, Offering> offeringTable, Map<Long, Course> courseTable, Map<Long, Placement> timetable) {
        HashMap<Long, Config> configTable = new HashMap<Long, Config>();
        HashMap<Long, Subpart> subpartTable = new HashMap<Long, Subpart>();
        HashMap<Long, Section> sectionTable = new HashMap<Long, Section>();
        for (Iterator<?> i = offeringsEl.elementIterator("offering"); i.hasNext();) {
            Element offeringEl = (Element) i.next();
            Offering offering = new Offering(
                    Long.parseLong(offeringEl.attributeValue("id")),
                    offeringEl.attributeValue("name", "O" + offeringEl.attributeValue("id")));
            offering.setDummy("true".equals(offeringEl.attributeValue("dummy", "false")));
            offeringTable.put(Long.valueOf(offering.getId()), offering);
            getModel().addOffering(offering);
            
            for (Iterator<?> j = offeringEl.elementIterator("course"); j.hasNext();) {
                Element courseEl = (Element) j.next();
                Course course = loadCourse(courseEl, offering);
                courseTable.put(Long.valueOf(course.getId()), course);
            }
            
            for (Iterator<?> j = offeringEl.elementIterator("config"); j.hasNext();) {
                Element configEl = (Element) j.next();
                Config config = loadConfig(configEl, offering, subpartTable, sectionTable, timetable);
                configTable.put(config.getId(), config);
            }
            
            for (Iterator<?> j = offeringEl.elementIterator("reservation"); j.hasNext(); ) {
                Element reservationEl = (Element)j.next();
                loadReservation(reservationEl, offering, configTable, sectionTable);
            } 
            
            for (Iterator<?> j = offeringEl.elementIterator("restriction"); j.hasNext(); ) {
                Element restrictionEl = (Element)j.next();
                loadRestriction(restrictionEl, offering, configTable, sectionTable);
            }
        }
    }
    
    /**
     * Load course
     * @param courseEl course element
     * @param offering parent offering
     * @return loaded course
     */
    protected Course loadCourse(Element courseEl, Offering offering) {
        Course course = new Course(
                Long.parseLong(courseEl.attributeValue("id")),
                courseEl.attributeValue("subjectArea", ""),
                courseEl.attributeValue("courseNbr", "C" + courseEl.attributeValue("id")),
                offering, Integer.parseInt(courseEl.attributeValue("limit", "-1")),
                Integer.parseInt(courseEl.attributeValue("projected", "0")));
        course.setCredit(courseEl.attributeValue("credit"));
        String credits = courseEl.attributeValue("credits");
        if (credits != null)
            course.setCreditValue(Float.valueOf(credits));
        course.setNote(courseEl.attributeValue("note"));
        course.setType(courseEl.attributeValue("type"));
        course.setTitle(courseEl.attributeValue("title"));
        return course;
    }
    
    /**
     * Load config
     * @param configEl config element
     * @param offering parent offering
     * @param subpartTable subpart table (of the offering)
     * @param sectionTable section table (of the offering)
     * @param timetable provided timetable
     * @return loaded config
     */
    protected Config loadConfig(Element configEl, Offering offering, Map<Long, Subpart> subpartTable, Map<Long, Section> sectionTable, Map<Long, Placement> timetable) {
        Config config = new Config
                (Long.parseLong(configEl.attributeValue("id")),
                Integer.parseInt(configEl.attributeValue("limit", "-1")),
                configEl.attributeValue("name", "G" + configEl.attributeValue("id")),
                offering);
        Element imEl = configEl.element("instructional-method");
        if (imEl != null) {
            config.setInstructionalMethodId(Long.parseLong(imEl.attributeValue("id")));
            config.setInstructionalMethodName(imEl.attributeValue("name", "M" + imEl.attributeValue("id")));
            config.setInstructionalMethodReference(imEl.attributeValue("reference", config.getInstructionalMethodName()));
        }
        for (Iterator<?> k = configEl.elementIterator("subpart"); k.hasNext();) {
            Element subpartEl = (Element) k.next();
            Subpart subpart = loadSubpart(subpartEl, config, subpartTable, sectionTable, timetable);
            subpartTable.put(Long.valueOf(subpart.getId()), subpart);
        }
        return config;
    }
    
    /**
     * Load subpart
     * @param subpartEl supart element
     * @param config parent config
     * @param subpartTable subpart table (of the offering)
     * @param sectionTable section table (of the offering)
     * @param timetable provided timetable
     * @return loaded subpart
     */
    protected Subpart loadSubpart(Element subpartEl, Config config, Map<Long, Subpart> subpartTable, Map<Long, Section> sectionTable, Map<Long, Placement> timetable) {
        Subpart parentSubpart = null;
        if (subpartEl.attributeValue("parent") != null)
            parentSubpart = subpartTable.get(Long.valueOf(subpartEl.attributeValue("parent")));
        Subpart subpart = new Subpart(
                Long.parseLong(subpartEl.attributeValue("id")),
                subpartEl.attributeValue("itype"),
                subpartEl.attributeValue("name", "P" + subpartEl.attributeValue("id")),
                config,
                parentSubpart);
        subpart.setAllowOverlap("true".equals(subpartEl.attributeValue("allowOverlap", "false")));
        subpart.setCredit(subpartEl.attributeValue("credit"));
        String credits = subpartEl.attributeValue("credits");
        if (credits != null)
            subpart.setCreditValue(Float.valueOf(credits));
        
        for (Iterator<?> l = subpartEl.elementIterator("section"); l.hasNext();) {
            Element sectionEl = (Element) l.next();
            Section section = loadSection(sectionEl, subpart, sectionTable, timetable);
            sectionTable.put(Long.valueOf(section.getId()), section);
        }
        
        return subpart;
    }
    
    /**
     * Load section
     * @param sectionEl section element
     * @param subpart parent subpart
     * @param sectionTable section table (of the offering)
     * @param timetable provided timetable
     * @return loaded section
     */
    @SuppressWarnings("deprecation")
    protected Section loadSection(Element sectionEl, Subpart subpart, Map<Long, Section> sectionTable, Map<Long, Placement> timetable) {
        Section parentSection = null;
        if (sectionEl.attributeValue("parent") != null)
            parentSection = sectionTable.get(Long.valueOf(sectionEl.attributeValue("parent")));
        Placement placement = null;
        if (timetable != null) {
            placement = timetable.get(Long.parseLong(sectionEl.attributeValue("id")));
        } else {
            TimeLocation time = null;
            Element timeEl = sectionEl.element("time");
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
            List<RoomLocation> rooms = new ArrayList<RoomLocation>();
            for (Iterator<?> m = sectionEl.elementIterator("room"); m.hasNext();) {
                Element roomEl = (Element) m.next();
                Double posX = null, posY = null;
                if (roomEl.attributeValue("location") != null) {
                    String loc = roomEl.attributeValue("location");
                    posX = Double.valueOf(loc.substring(0, loc.indexOf(',')));
                    posY = Double.valueOf(loc.substring(loc.indexOf(',') + 1));
                }
                RoomLocation room = new RoomLocation(
                        Long.valueOf(roomEl.attributeValue("id")),
                        roomEl.attributeValue("name", "R" + roomEl.attributeValue("id")),
                        roomEl.attributeValue("building") == null ? null : Long.valueOf(roomEl.attributeValue("building")),
                        0, Integer.parseInt(roomEl.attributeValue("capacity")),
                        posX, posY, "true".equals(roomEl.attributeValue("ignoreTooFar")), null);
                rooms.add(room);
            }
            placement = (time == null ? null : new Placement(null, time, rooms));
        }
        
        List<Instructor> instructors = new ArrayList<Instructor>();
        for (Iterator<?> m = sectionEl.elementIterator("instructor"); m.hasNext(); ) {
            Element instructorEl = (Element)m.next();
            instructors.add(new Instructor(Long.parseLong(instructorEl.attributeValue("id")), instructorEl.attributeValue("externalId"), instructorEl.attributeValue("name"), instructorEl.attributeValue("email")));
        }
        if (instructors.isEmpty() && sectionEl.attributeValue("instructorIds") != null)
            instructors = Instructor.toInstructors(sectionEl.attributeValue("instructorIds"), sectionEl.attributeValue("instructorNames"));
        Section section = new Section(
                Long.parseLong(sectionEl.attributeValue("id")),
                Integer.parseInt(sectionEl.attributeValue("limit")),
                sectionEl.attributeValue("name", "S" + sectionEl.attributeValue("id")),
                subpart, placement, instructors, parentSection);
        
        section.setSpaceHeld(Double.parseDouble(sectionEl.attributeValue("hold", "0.0")));
        section.setSpaceExpected(Double.parseDouble(sectionEl.attributeValue("expect", "0.0")));
        section.setCancelled("true".equalsIgnoreCase(sectionEl.attributeValue("cancelled", "false")));
        section.setEnabled("true".equalsIgnoreCase(sectionEl.attributeValue("enabled", "true")));
        section.setOnline("true".equalsIgnoreCase(sectionEl.attributeValue("online", "false")));
        section.setPast("true".equalsIgnoreCase(sectionEl.attributeValue("past", "false")));
        for (Iterator<?> m = sectionEl.elementIterator("cname"); m.hasNext(); ) {
            Element cNameEl = (Element)m.next();
            section.setName(Long.parseLong(cNameEl.attributeValue("id")), cNameEl.getText());
        }
        Element ignoreEl = sectionEl.element("no-conflicts");
        if (ignoreEl != null) {
            for (Iterator<?> m = ignoreEl.elementIterator("section"); m.hasNext(); )
                section.addIgnoreConflictWith(Long.parseLong(((Element)m.next()).attributeValue("id")));
        }
        
        return section;
    }
    
    /**
     * Load reservation
     * @param reservationEl reservation element
     * @param offering parent offering
     * @param configTable config table (of the offering)
     * @param sectionTable section table (of the offering)
     * @return loaded reservation
     */
    protected Reservation loadReservation(Element reservationEl, Offering offering, HashMap<Long, Config> configTable, HashMap<Long, Section> sectionTable) {
        Reservation r = null;
        if ("individual".equals(reservationEl.attributeValue("type"))) {
            Set<Long> studentIds = new HashSet<Long>();
            for (Iterator<?> k = reservationEl.elementIterator("student"); k.hasNext(); ) {
                Element studentEl = (Element)k.next();
                studentIds.add(Long.parseLong(studentEl.attributeValue("id")));
            }
            r = new IndividualReservation(Long.valueOf(reservationEl.attributeValue("id")), offering, studentIds);
        } else if ("group".equals(reservationEl.attributeValue("type"))) {
            Set<Long> studentIds = new HashSet<Long>();
            for (Iterator<?> k = reservationEl.elementIterator("student"); k.hasNext(); ) {
                Element studentEl = (Element)k.next();
                studentIds.add(Long.parseLong(studentEl.attributeValue("id")));
            }
            r = new GroupReservation(Long.valueOf(reservationEl.attributeValue("id")),
                    Double.parseDouble(reservationEl.attributeValue("limit", "-1")),
                    offering, studentIds);
        } else if ("lc".equals(reservationEl.attributeValue("type"))) {
            Set<Long> studentIds = new HashSet<Long>();
            for (Iterator<?> k = reservationEl.elementIterator("student"); k.hasNext(); ) {
                Element studentEl = (Element)k.next();
                studentIds.add(Long.parseLong(studentEl.attributeValue("id")));
            }
            long courseId = Long.parseLong(reservationEl.attributeValue("course"));
            for (Course course: offering.getCourses()) {
                if (course.getId() == courseId)
                    r = new LearningCommunityReservation(Long.valueOf(reservationEl.attributeValue("id")),
                            Double.parseDouble(reservationEl.attributeValue("limit", "-1")),
                            course, studentIds);
            }
        } else if ("curriculum".equals(reservationEl.attributeValue("type")) || "curriculum-override".equals(reservationEl.attributeValue("type"))) {
            List<String> acadAreas = new ArrayList<String>();
            for (Iterator<?> k = reservationEl.elementIterator("area"); k.hasNext(); ) {
                Element areaEl = (Element)k.next();
                acadAreas.add(areaEl.attributeValue("code"));
            }
            if (acadAreas.isEmpty() && reservationEl.attributeValue("area") != null)
                acadAreas.add(reservationEl.attributeValue("area"));
            List<String> classifications = new ArrayList<String>();
            for (Iterator<?> k = reservationEl.elementIterator("classification"); k.hasNext(); ) {
                Element clasfEl = (Element)k.next();
                classifications.add(clasfEl.attributeValue("code"));
            }
            List<String> majors = new ArrayList<String>();
            for (Iterator<?> k = reservationEl.elementIterator("major"); k.hasNext(); ) {
                Element majorEl = (Element)k.next();
                majors.add(majorEl.attributeValue("code"));
            }
            List<String> minors = new ArrayList<String>();
            for (Iterator<?> k = reservationEl.elementIterator("minor"); k.hasNext(); ) {
                Element minorEl = (Element)k.next();
                minors.add(minorEl.attributeValue("code"));
            }
            if ("curriculum".equals(reservationEl.attributeValue("type")))
                r = new CurriculumReservation(Long.valueOf(reservationEl.attributeValue("id")),
                        Double.parseDouble(reservationEl.attributeValue("limit", "-1")),
                        offering,
                        acadAreas, classifications, majors, minors);
            else
                r = new CurriculumOverride(Long.valueOf(reservationEl.attributeValue("id")),
                        Double.parseDouble(reservationEl.attributeValue("limit", "-1")),
                        offering,
                        acadAreas, classifications, majors, minors);
            for (Iterator<?> k = reservationEl.elementIterator("major"); k.hasNext(); ) {
                Element majorEl = (Element)k.next();
                for (Iterator<?> l = majorEl.elementIterator("concentration"); l.hasNext(); ) {
                    Element concentrationEl = (Element)l.next();
                    ((CurriculumReservation)r).addConcentration(majorEl.attributeValue("code"), concentrationEl.attributeValue("code"));
                }
            }
        } else if ("course".equals(reservationEl.attributeValue("type"))) {
            long courseId = Long.parseLong(reservationEl.attributeValue("course"));
            for (Course course: offering.getCourses()) {
                if (course.getId() == courseId)
                    r = new CourseReservation(Long.valueOf(reservationEl.attributeValue("id")), course);
            }
        } else if ("dummy".equals(reservationEl.attributeValue("type"))) {
            r = new DummyReservation(offering);
        } else if ("universal".equals(reservationEl.attributeValue("type"))) {
            r = new UniversalOverride(
                    Long.valueOf(reservationEl.attributeValue("id")),
                    "true".equals(reservationEl.attributeValue("override", "false")),
                    Double.parseDouble(reservationEl.attributeValue("limit", "-1")),
                    offering,
                    reservationEl.attributeValue("filter"));
        } else if ("override".equals(reservationEl.attributeValue("type"))) {
            Set<Long> studentIds = new HashSet<Long>();
            for (Iterator<?> k = reservationEl.elementIterator("student"); k.hasNext(); ) {
                Element studentEl = (Element)k.next();
                studentIds.add(Long.parseLong(studentEl.attributeValue("id")));
            }
            r = new ReservationOverride(Long.valueOf(reservationEl.attributeValue("id")), offering, studentIds);
        }
        if (r == null) {
            sLogger.error("Unknown reservation type "+ reservationEl.attributeValue("type"));
            return null;
        }
        r.setExpired("true".equals(reservationEl.attributeValue("expired", "false")));
        for (Iterator<?> k = reservationEl.elementIterator("config"); k.hasNext(); ) {
            Element configEl = (Element)k.next();
            r.addConfig(configTable.get(Long.parseLong(configEl.attributeValue("id"))));
        }
        for (Iterator<?> k = reservationEl.elementIterator("section"); k.hasNext(); ) {
            Element sectionEl = (Element)k.next();
            r.addSection(sectionTable.get(Long.parseLong(sectionEl.attributeValue("id"))), false);
        }
        r.setPriority(Integer.parseInt(reservationEl.attributeValue("priority", String.valueOf(r.getPriority()))));
        r.setMustBeUsed("true".equals(reservationEl.attributeValue("mustBeUsed", r.mustBeUsed() ? "true" : "false")));
        r.setAllowOverlap("true".equals(reservationEl.attributeValue("allowOverlap", r.isAllowOverlap() ? "true" : "false")));
        r.setCanAssignOverLimit("true".equals(reservationEl.attributeValue("canAssignOverLimit", r.canAssignOverLimit() ? "true" : "false")));
        r.setAllowDisabled("true".equals(reservationEl.attributeValue("allowDisabled", r.isAllowDisabled() ? "true" : "false")));
        r.setNeverIncluded("true".equals(reservationEl.attributeValue("neverIncluded", "false")));
        r.setBreakLinkedSections("true".equals(reservationEl.attributeValue("breakLinkedSections", "false")));
        return r;
    }
    
    /**
     * Load restriction
     * @param restrictionEl restriction element
     * @param offering parent offering
     * @param configTable config table (of the offering)
     * @param sectionTable section table (of the offering)
     * @return loaded restriction
     */
    protected Restriction loadRestriction(Element restrictionEl, Offering offering, HashMap<Long, Config> configTable, HashMap<Long, Section> sectionTable) {
        Restriction r = null;
        if ("individual".equals(restrictionEl.attributeValue("type"))) {
            Set<Long> studentIds = new HashSet<Long>();
            for (Iterator<?> k = restrictionEl.elementIterator("student"); k.hasNext(); ) {
                Element studentEl = (Element)k.next();
                studentIds.add(Long.parseLong(studentEl.attributeValue("id")));
            }
            r = new IndividualRestriction(Long.valueOf(restrictionEl.attributeValue("id")), offering, studentIds);
        } else if ("curriculum".equals(restrictionEl.attributeValue("type"))) {
            List<String> acadAreas = new ArrayList<String>();
            for (Iterator<?> k = restrictionEl.elementIterator("area"); k.hasNext(); ) {
                Element areaEl = (Element)k.next();
                acadAreas.add(areaEl.attributeValue("code"));
            }
            if (acadAreas.isEmpty() && restrictionEl.attributeValue("area") != null)
                acadAreas.add(restrictionEl.attributeValue("area"));
            List<String> classifications = new ArrayList<String>();
            for (Iterator<?> k = restrictionEl.elementIterator("classification"); k.hasNext(); ) {
                Element clasfEl = (Element)k.next();
                classifications.add(clasfEl.attributeValue("code"));
            }
            List<String> majors = new ArrayList<String>();
            for (Iterator<?> k = restrictionEl.elementIterator("major"); k.hasNext(); ) {
                Element majorEl = (Element)k.next();
                majors.add(majorEl.attributeValue("code"));
            }
            List<String> minors = new ArrayList<String>();
            for (Iterator<?> k = restrictionEl.elementIterator("minor"); k.hasNext(); ) {
                Element minorEl = (Element)k.next();
                minors.add(minorEl.attributeValue("code"));
            }
            r = new CurriculumRestriction(Long.valueOf(restrictionEl.attributeValue("id")),
                    offering,
                    acadAreas, classifications, majors, minors);
            for (Iterator<?> k = restrictionEl.elementIterator("major"); k.hasNext(); ) {
                Element majorEl = (Element)k.next();
                for (Iterator<?> l = majorEl.elementIterator("concentration"); l.hasNext(); ) {
                    Element concentrationEl = (Element)l.next();
                    ((CurriculumRestriction)r).addConcentration(majorEl.attributeValue("code"), concentrationEl.attributeValue("code"));
                }
            }
        } else if ("course".equals(restrictionEl.attributeValue("type"))) {
            long courseId = Long.parseLong(restrictionEl.attributeValue("course"));
            for (Course course: offering.getCourses()) {
                if (course.getId() == courseId)
                    r = new CourseRestriction(Long.valueOf(restrictionEl.attributeValue("id")), course);
            }
        }
        if (r == null) {
            sLogger.error("Unknown reservation type "+ restrictionEl.attributeValue("type"));
            return null;
        }
        for (Iterator<?> k = restrictionEl.elementIterator("config"); k.hasNext(); ) {
            Element configEl = (Element)k.next();
            r.addConfig(configTable.get(Long.parseLong(configEl.attributeValue("id"))));
        }
        for (Iterator<?> k = restrictionEl.elementIterator("section"); k.hasNext(); ) {
            Element sectionEl = (Element)k.next();
            r.addSection(sectionTable.get(Long.parseLong(sectionEl.attributeValue("id"))));
        }
        return r;
    }
    
    /**
     * Load given timetable
     * @param timetableRoot document root in the course timetabling XML format
     * @return loaded timetable (map class id: assigned placement)
     */
    protected Map<Long, Placement> loadTimetable(Element timetableRoot) {
        Map<Long, Placement> timetable = new HashMap<Long, Placement>();
        HashMap<Long, RoomLocation> rooms = new HashMap<Long, RoomLocation>();
        for (Iterator<?> i = timetableRoot.element("rooms").elementIterator("room"); i.hasNext();) {
            Element roomEl = (Element)i.next();
            Long roomId = Long.valueOf(roomEl.attributeValue("id"));
            Double posX = null, posY = null;
            if (roomEl.attributeValue("location") != null) {
                String loc = roomEl.attributeValue("location");
                posX = Double.valueOf(loc.substring(0, loc.indexOf(',')));
                posY = Double.valueOf(loc.substring(loc.indexOf(',') + 1));
            }
            RoomLocation room = new RoomLocation(
                    Long.valueOf(roomEl.attributeValue("id")),
                    roomEl.attributeValue("name", "R" + roomEl.attributeValue("id")),
                    roomEl.attributeValue("building") == null ? null : Long.valueOf(roomEl.attributeValue("building")),
                    0, Integer.parseInt(roomEl.attributeValue("capacity")),
                    posX, posY, "true".equals(roomEl.attributeValue("ignoreTooFar")), null);
            rooms.put(roomId, room);
        }
        for (Iterator<?> i = timetableRoot.element("classes").elementIterator("class"); i.hasNext();) {
            Element classEl = (Element)i.next();
            Long classId = Long.valueOf(classEl.attributeValue("id"));
            TimeLocation time = null;
            Element timeEl = null;
            for (Iterator<?> j = classEl.elementIterator("time"); j.hasNext(); ) {
                Element e = (Element)j.next();
                if ("true".equals(e.attributeValue("solution", "false"))) { timeEl = e; break; }
            }
            if (timeEl != null) {
                time = new TimeLocation(
                        Integer.parseInt(timeEl.attributeValue("days"), 2),
                        Integer.parseInt(timeEl.attributeValue("start")),
                        Integer.parseInt(timeEl.attributeValue("length")), 0, 0,
                        classEl.attributeValue("datePattern") == null ? null : Long.valueOf(classEl.attributeValue("datePattern")),
                        classEl.attributeValue("datePatternName", ""), createBitSet(classEl.attributeValue("dates")),
                        Integer.parseInt(timeEl.attributeValue("breakTime", "0")));
                if (timeEl.attributeValue("pattern") != null)
                    time.setTimePatternId(Long.valueOf(timeEl.attributeValue("pattern")));
            }
            List<RoomLocation> room = new ArrayList<RoomLocation>();
            for (Iterator<?> j = classEl.elementIterator("room"); j.hasNext();) {
                Element roomEl = (Element) j.next();
                if (!"true".equals(roomEl.attributeValue("solution", "false"))) continue;
                room.add(rooms.get(Long.valueOf(roomEl.attributeValue("id"))));
            }
            Placement placement = (time == null ? null : new Placement(null, time, room));
            if (placement != null)
                timetable.put(classId, placement);
        }
        return timetable;
    }
    
    /**
     * Load travel times
     * @param travelTimesEl travel-time element
     * @param metric distance metric to be populated
     */
    protected void loadTravelTimes(Element travelTimesEl, DistanceMetric metric) {
        for (Iterator<?> i = travelTimesEl.elementIterator("travel-time"); i.hasNext();) {
            Element travelTimeEl = (Element)i.next();
            metric.addTravelTime(
                    Long.valueOf(travelTimeEl.attributeValue("id1")),
                    Long.valueOf(travelTimeEl.attributeValue("id2")),
                    Integer.valueOf(travelTimeEl.attributeValue("minutes")));
        }
    }
    
    /**
     * Load linked sections
     * @param constraintsEl constraints element
     * @param offeringTable offering table
     */
    protected void loadLinkedSections(Element constraintsEl, Map<Long, Offering> offeringTable) {
        for (Iterator<?> i = constraintsEl.elementIterator("linked-sections"); i.hasNext();) {
            Element linkedEl = (Element) i.next();
            List<Section> sections = new ArrayList<Section>();
            for (Iterator<?> j = linkedEl.elementIterator("section"); j.hasNext();) {
                Element sectionEl = (Element) j.next();
                Offering offering = offeringTable.get(Long.valueOf(sectionEl.attributeValue("offering")));
                sections.add(offering.getSection(Long.valueOf(sectionEl.attributeValue("id"))));
            }
            getModel().addLinkedSections("true".equals(linkedEl.attributeValue("mustBeUsed", "false")), sections);
        }
    }
    
    /**
     * Load students
     * @param studentsEl students element
     * @param offeringTable offering table
     * @param courseTable course table
     */
    protected void loadStudents(Element studentsEl, Map<Long, Offering> offeringTable, Map<Long, Course> courseTable, List<Enrollment> bestEnrollments, List<Enrollment> currentEnrollments) {
        for (Iterator<?> i = studentsEl.elementIterator("student"); i.hasNext();) {
            Element studentEl = (Element) i.next();
            Student student = loadStudent(studentEl, offeringTable);
            if (iStudentFilter != null && !iStudentFilter.accept(student))
                continue;
            for (Iterator<?> j = studentEl.elementIterator(); j.hasNext();) {
                Element requestEl = (Element) j.next();
                Request request = loadRequest(requestEl, student, offeringTable, courseTable);
                if (request == null) continue;
                
                Element initialEl = requestEl.element("initial");
                if (iLoadInitial && initialEl != null) {
                    Enrollment enrollment = loadEnrollment(initialEl, request);
                    if (enrollment != null)
                        request.setInitialAssignment(enrollment);
                }
                Element currentEl = requestEl.element("current");
                if (iLoadCurrent && currentEl != null) {
                    Enrollment enrollment = loadEnrollment(currentEl, request);
                    if (enrollment != null)
                        currentEnrollments.add(enrollment);
                }
                Element bestEl = requestEl.element("best");
                if (iLoadBest && bestEl != null) {
                    Enrollment enrollment = loadEnrollment(bestEl, request);
                    if (enrollment != null)
                        bestEnrollments.add(enrollment);
                }
                Element fixedEl = requestEl.element("fixed");
                if (fixedEl != null && request instanceof CourseRequest)
                    ((CourseRequest)request).setFixedValue(loadEnrollment(fixedEl, request));
            }
            getModel().addStudent(student);
        }
    }
    
    /**
     * Save best enrollments
     * @param bestEnrollments best enrollments
     */
    protected void assignBest(List<Enrollment> bestEnrollments) {
        // Enrollments with a reservation must go first, enrollments with an override go last
        for (Enrollment enrollment : bestEnrollments) {
            if (enrollment.getReservation() == null || enrollment.getReservation().isExpired()) continue;
            if (!enrollment.getStudent().isAvailable(enrollment)) {
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " is conflicting: student not available.");
                continue;
            }
            Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(getAssignment(), enrollment);
            if (conflicts.isEmpty())
                getAssignment().assign(0, enrollment);
            else
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " conflicts with " + conflicts);
        }
        for (Enrollment enrollment : bestEnrollments) {
            if (enrollment.getReservation() != null) continue;
            if (!enrollment.getStudent().isAvailable(enrollment)) {
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " is conflicting: student not available.");
                continue;
            }
            Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(getAssignment(), enrollment);
            if (conflicts.isEmpty())
                getAssignment().assign(0, enrollment);
            else
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " conflicts with " + conflicts);
        }
        for (Enrollment enrollment : bestEnrollments) {
            if (enrollment.getReservation() == null || !enrollment.getReservation().isExpired()) continue;
            if (!enrollment.getStudent().isAvailable(enrollment)) {
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " is conflicting: student not available.");
                continue;
            }
            Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(getAssignment(), enrollment);
            if (conflicts.isEmpty())
                getAssignment().assign(0, enrollment);
            else
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " conflicts with " + conflicts);
        }
        getModel().saveBest(getAssignment());
    }
    
    /**
     * Assign current enrollments
     * @param currentEnrollments current enrollments
     */
    protected void assignCurrent(List<Enrollment> currentEnrollments) {
        for (Request request : getModel().variables())
            getAssignment().unassign(0, request);
        // Enrollments with a reservation must go first, enrollments with an override go last
        for (Enrollment enrollment : currentEnrollments) {
            if (enrollment.getReservation() == null || enrollment.getReservation().isExpired()) continue;
            if (!enrollment.getStudent().isAvailable(enrollment)) {
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " is conflicting: student not available.");
                continue;
            }
            Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(getAssignment(), enrollment);
            if (conflicts.isEmpty())
                getAssignment().assign(0, enrollment);
            else
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " conflicts with " + conflicts);
        }
        for (Enrollment enrollment : currentEnrollments) {
            if (enrollment.getReservation() != null) continue;
            if (!enrollment.getStudent().isAvailable(enrollment)) {
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " is conflicting: student not available.");
                continue;
            }
            Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(getAssignment(), enrollment);
            if (conflicts.isEmpty())
                getAssignment().assign(0, enrollment);
            else
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " conflicts with " + conflicts);
        }
        for (Enrollment enrollment : currentEnrollments) {
            if (enrollment.getReservation() == null || !enrollment.getReservation().isExpired()) continue;
            if (!enrollment.getStudent().isAvailable(enrollment)) {
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " is conflicting: student not available.");
                continue;
            }
            Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(getAssignment(), enrollment);
            if (conflicts.isEmpty())
                getAssignment().assign(0, enrollment);
            else
                sLogger.warn("[" + enrollment.getStudent().getExternalId() + "] Enrollment " + enrollment + " conflicts with " + conflicts);
        }
    }
    
    /**
     * Load student
     * @param studentEl student element
     * @param offeringTable offering table
     * @return loaded student
     */
    protected Student loadStudent(Element studentEl, Map<Long, Offering> offeringTable) {
        Student student = new Student(Long.parseLong(studentEl.attributeValue("id")), "true".equals(studentEl.attributeValue("dummy")));
        if (studentEl.attributeValue("priority") != null)
            student.setPriority(StudentPriority.getPriority(studentEl.attributeValue("priority")));
        if ("true".equals(studentEl.attributeValue("shortDistances")))
            student.setNeedShortDistances(true);
        if ("true".equals(studentEl.attributeValue("allowDisabled")))
            student.setAllowDisabled(true);
        student.setExternalId(studentEl.attributeValue("externalId"));
        student.setName(studentEl.attributeValue("name"));
        student.setStatus(studentEl.attributeValue("status"));
        String minCredit = studentEl.attributeValue("minCredit");
        if (minCredit != null)
            student.setMinCredit(Float.parseFloat(minCredit));
        String maxCredit = studentEl.attributeValue("maxCredit");
        if (maxCredit != null)
            student.setMaxCredit(Float.parseFloat(maxCredit));
        String classFirstDate = studentEl.attributeValue("classFirstDate");
        if (classFirstDate != null)
            student.setClassFirstDate(Integer.parseInt(classFirstDate));
        String classLastDate = studentEl.attributeValue("classLastDate");
        if (classLastDate != null)
            student.setClassLastDate(Integer.parseInt(classLastDate));
        String modality = studentEl.attributeValue("modality");
        if (modality != null)
            student.setModalityPreference(ModalityPreference.valueOf(modality));
        String btb = studentEl.attributeValue("btb");
        if (btb != null)
            student.setBackToBackPreference(BackToBackPreference.valueOf(btb));
        
        List<String[]> clasf = new ArrayList<String[]>();
        List<String[]> major = new ArrayList<String[]>();
        for (Iterator<?> j = studentEl.elementIterator(); j.hasNext();) {
            Element requestEl = (Element) j.next();
            if ("classification".equals(requestEl.getName())) {
                clasf.add(new String[] {requestEl.attributeValue("area"), requestEl.attributeValue("code"), requestEl.attributeValue("label")});
            } else if ("major".equals(requestEl.getName())) {
                major.add(new String[] {requestEl.attributeValue("area"), requestEl.attributeValue("code"), requestEl.attributeValue("label")});
            } else if ("minor".equals(requestEl.getName())) {
                if ("A".equals(requestEl.attributeValue("area")))
                    student.getAccommodations().add(requestEl.attributeValue("code"));
                else
                    student.getGroups().add(new StudentGroup(requestEl.attributeValue("area"), requestEl.attributeValue("code"), requestEl.attributeValue("label")));
            } else if ("unavailability".equals(requestEl.getName())) {
                Offering offering = offeringTable.get(Long.parseLong(requestEl.attributeValue("offering")));
                Section section = (offering == null ? null : offering.getSection(Long.parseLong(requestEl.attributeValue("section"))));
                if (section != null) {
                    Unavailability ua = new Unavailability(student, section, "true".equals(requestEl.attributeValue("allowOverlap")));
                    ua.setTeachingAssignment("true".equals(requestEl.attributeValue("ta", "false")));
                    if (requestEl.attributeValue("course") != null)
                        ua.setCourseId(Long.valueOf(requestEl.attributeValue("course")));
                }
            } else if ("acm".equals(requestEl.getName())) {
                if (requestEl.attributeValue("minor") != null)
                    student.getAreaClassificationMinors().add(new AreaClassificationMajor(
                            requestEl.attributeValue("area"), requestEl.attributeValue("areaName"),
                            requestEl.attributeValue("classification"), requestEl.attributeValue("classificationName"),
                            requestEl.attributeValue("minor"), requestEl.attributeValue("minorName"),
                            requestEl.attributeValue("concentration"), requestEl.attributeValue("concentrationName"),
                            requestEl.attributeValue("degree"), requestEl.attributeValue("degreeName"),
                            requestEl.attributeValue("program"), requestEl.attributeValue("programName"),
                            requestEl.attributeValue("campus"), requestEl.attributeValue("campusName"),
                            requestEl.attributeValue("weight") == null ? null : Double.valueOf(requestEl.attributeValue("weight"))));
                else
                    student.getAreaClassificationMajors().add(new AreaClassificationMajor(
                            requestEl.attributeValue("area"), requestEl.attributeValue("areaName"),
                            requestEl.attributeValue("classification"), requestEl.attributeValue("classificationName"),
                            requestEl.attributeValue("major"), requestEl.attributeValue("majorName"),
                            requestEl.attributeValue("concentration"), requestEl.attributeValue("concentrationName"),
                            requestEl.attributeValue("degree"), requestEl.attributeValue("degreeName"),
                            requestEl.attributeValue("program"), requestEl.attributeValue("programName"),
                            requestEl.attributeValue("campus"), requestEl.attributeValue("campusName"),
                            requestEl.attributeValue("weight") == null ? null : Double.valueOf(requestEl.attributeValue("weight"))));
            } else if ("group".equals(requestEl.getName())) {
                student.getGroups().add(new StudentGroup(requestEl.attributeValue("type"), requestEl.attributeValue("reference"), requestEl.attributeValue("name")));
            } else if ("accommodation".equals(requestEl.getName())) {
                student.getAccommodations().add(requestEl.attributeValue("reference"));
            } else if ("advisor".equals(requestEl.getName())) {
                student.getAdvisors().add(new Instructor(0l, requestEl.attributeValue("externalId"), requestEl.attributeValue("name"), requestEl.attributeValue("email")));
            }
        }
        for (int i = 0; i < Math.min(clasf.size(), major.size()); i++) {
            student.getAreaClassificationMajors().add(new AreaClassificationMajor(clasf.get(i)[0],clasf.get(i)[1],major.get(i)[1]));
        }
        return student;
    }
    
    /**
     * Load request
     * @param requestEl request element
     * @param student parent student
     * @param offeringTable offering table
     * @param courseTable course table
     * @return loaded request
     */
    protected Request loadRequest(Element requestEl, Student student, Map<Long, Offering> offeringTable, Map<Long, Course> courseTable) {
        if ("freeTime".equals(requestEl.getName())) {
            return loadFreeTime(requestEl, student);
        } else if ("course".equals(requestEl.getName())) {
            return loadCourseRequest(requestEl, student, offeringTable, courseTable);
        } else {
            return null;
        }
    }
    
    /**
     * Load free time request
     * @param requestEl request element
     * @param student parent student
     * @return loaded free time request
     */
    public FreeTimeRequest loadFreeTime(Element requestEl, Student student) {
        TimeLocation time = new TimeLocation(Integer.parseInt(requestEl.attributeValue("days"), 2),
                Integer.parseInt(requestEl.attributeValue("start")), Integer.parseInt(requestEl
                        .attributeValue("length")), 0, 0,
                requestEl.attributeValue("datePattern") == null ? null : Long.valueOf(requestEl
                        .attributeValue("datePattern")), "", createBitSet(requestEl
                        .attributeValue("dates")), 0);
        FreeTimeRequest request = new FreeTimeRequest(Long.parseLong(requestEl.attributeValue("id")),
                Integer.parseInt(requestEl.attributeValue("priority")), "true".equals(requestEl
                        .attributeValue("alternative")), student, time);
        if (requestEl.attributeValue("weight") != null)
            request.setWeight(Double.parseDouble(requestEl.attributeValue("weight")));
        return request;
    }
    
    /**
     * Load course request
     * @param requestEl request element
     * @param student parent student
     * @param offeringTable offering table
     * @param courseTable course table
     * @return loaded course request
     */
    public CourseRequest loadCourseRequest(Element requestEl, Student student, Map<Long, Offering> offeringTable, Map<Long, Course> courseTable) {
        List<Course> courses = new ArrayList<Course>();
        courses.add(courseTable.get(Long.valueOf(requestEl.attributeValue("course"))));
        for (Iterator<?> k = requestEl.elementIterator("alternative"); k.hasNext();)
            courses.add(courseTable.get(Long.valueOf(((Element) k.next()).attributeValue("course"))));
        Long timeStamp = null;
        if (requestEl.attributeValue("timeStamp") != null)
            timeStamp = Long.valueOf(requestEl.attributeValue("timeStamp"));
        CourseRequest courseRequest = new CourseRequest(
                Long.parseLong(requestEl.attributeValue("id")),
                Integer.parseInt(requestEl.attributeValue("priority")),
                "true".equals(requestEl.attributeValue("alternative")), 
                student, courses,
                "true".equals(requestEl.attributeValue("waitlist", "false")),
                RequestPriority.valueOf(requestEl.attributeValue("importance",
                        "true".equals(requestEl.attributeValue("critical", "false")) ? RequestPriority.Critical.name() : RequestPriority.Normal.name())),
                timeStamp);
        if (iWaitlistCritical && RequestPriority.Critical.isCritical(courseRequest) && !courseRequest.isAlternative()) courseRequest.setWaitlist(true);
        if (requestEl.attributeValue("weight") != null)
            courseRequest.setWeight(Double.parseDouble(requestEl.attributeValue("weight")));
        for (Iterator<?> k = requestEl.elementIterator("waitlisted"); k.hasNext();) {
            Element choiceEl = (Element) k.next();
            courseRequest.getWaitlistedChoices().add(
                    new Choice(offeringTable.get(Long.valueOf(choiceEl.attributeValue("offering"))), choiceEl.getText()));
        }
        for (Iterator<?> k = requestEl.elementIterator("selected"); k.hasNext();) {
            Element choiceEl = (Element) k.next();
            courseRequest.getSelectedChoices().add(
                    new Choice(offeringTable.get(Long.valueOf(choiceEl.attributeValue("offering"))), choiceEl.getText()));
        }
        for (Iterator<?> k = requestEl.elementIterator("required"); k.hasNext();) {
            Element choiceEl = (Element) k.next();
            courseRequest.getRequiredChoices().add(
                    new Choice(offeringTable.get(Long.valueOf(choiceEl.attributeValue("offering"))), choiceEl.getText()));
        }
        groups: for (Iterator<?> k = requestEl.elementIterator("group"); k.hasNext();) {
            Element groupEl = (Element) k.next();
            long gid = Long.parseLong(groupEl.attributeValue("id"));
            String gname = groupEl.attributeValue("name", "g" + gid);
            Course course = courseTable.get(Long.valueOf(groupEl.attributeValue("course")));
            for (RequestGroup g: course.getRequestGroups()) {
                if (g.getId() == gid) {
                    courseRequest.addRequestGroup(g);
                    continue groups;
                }
            }
            courseRequest.addRequestGroup(new RequestGroup(gid, gname, course));
        }
        return courseRequest;
    }
    
    /**
     * Load enrollment
     * @param enrollmentEl enrollment element (current, best, or initial)
     * @param request parent request
     * @return loaded enrollment
     */
    protected Enrollment loadEnrollment(Element enrollmentEl, Request request) {
        if (request instanceof CourseRequest) {
            CourseRequest courseRequest = (CourseRequest) request;
            HashSet<Section> sections = new HashSet<Section>();
            for (Iterator<?> k = enrollmentEl.elementIterator("section"); k.hasNext();) {
                Element sectionEl = (Element) k.next();
                Section section = courseRequest.getSection(Long.parseLong(sectionEl
                        .attributeValue("id")));
                sections.add(section);
            }
            Reservation reservation = null;
            if (enrollmentEl.attributeValue("reservation", null) != null) {
                long reservationId = Long.valueOf(enrollmentEl.attributeValue("reservation"));
                for (Course course: courseRequest.getCourses())
                    for (Reservation r: course.getOffering().getReservations())
                        if (r.getId() == reservationId) { reservation = r; break; }
            }
            if (!sections.isEmpty()) {
                if (enrollmentEl.attributeValue("course") != null) {
                    Course course = courseRequest.getCourse(Long.valueOf(enrollmentEl.attributeValue("course")));
                    if (course != null)
                        return courseRequest.createEnrollment(course, sections, reservation); 
                }
                return courseRequest.createEnrollment(sections, reservation);
            }
        } else if (request instanceof FreeTimeRequest) {
            return ((FreeTimeRequest)request).createEnrollment();
        }
        return null;
    }

    protected void moveCriticalRequestsUp() {
        for (Student student: getModel().getStudents()) {
            int assigned = 0, critical = 0;
            for (Request r: student.getRequests()) {
                if (r instanceof CourseRequest) {
                    if (r.getInitialAssignment() != null) assigned ++;
                    if (r.getRequestPriority() != RequestPriority.Normal) critical ++;
                }
            }
            if ((getModel().getKeepInitialAssignments() && assigned > 0) || critical > 0) {
                Collections.sort(student.getRequests(), new Comparator<Request>() {
                    @Override
                    public int compare(Request r1, Request r2) {
                        if (r1.isAlternative() != r2.isAlternative()) return r1.isAlternative() ? 1 : -1;
                        if (getModel().getKeepInitialAssignments()) {
                            boolean a1 = (r1 instanceof CourseRequest && r1.getInitialAssignment() != null);
                            boolean a2 = (r2 instanceof CourseRequest && r2.getInitialAssignment() != null);
                            if (a1 != a2) return a1 ? -1 : 1;
                        }
                        int c1 = r1.getRequestPriority().ordinal();
                        int c2 = r2.getRequestPriority().ordinal();
                        if (c1 != c2) return c1 < c2 ? -1 : 1;
                        return r1.getPriority() < r2.getPriority() ? -1 : 1;
                    }
                });
                int p = 0;
                for (Request r: student.getRequests())
                    r.setPriority(p++);
            }
        }
    }

}
