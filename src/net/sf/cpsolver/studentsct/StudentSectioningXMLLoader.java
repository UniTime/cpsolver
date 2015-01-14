package net.sf.cpsolver.studentsct;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.studentsct.filter.StudentFilter;
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
 * <table border='1'>
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
 * Usage:<br>
 * <code>
 * StudentSectioningModel model = new StudentSectioningModel(cfg);<br>
 * new StudentSectioningXMLLoader(model).load();<br>
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

public class StudentSectioningXMLLoader extends StudentSectioningLoader {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger
            .getLogger(StudentSectioningXMLLoader.class);

    private File iInputFile;
    private File iTimetableFile = null;
    private boolean iLoadBest = false;
    private boolean iLoadInitial = false;
    private boolean iLoadCurrent = false;
    private boolean iLoadOfferings = true;
    private boolean iLoadStudents = true;
    private StudentFilter iStudentFilter = null;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public StudentSectioningXMLLoader(StudentSectioningModel model) {
        super(model);
        iInputFile = new File(getModel().getProperties().getProperty("General.Input",
                "." + File.separator + "solution.xml"));
        if (getModel().getProperties().getProperty("General.InputTimetable") != null)
            iTimetableFile = new File(getModel().getProperties().getProperty("General.InputTimetable"));
        iLoadBest = getModel().getProperties().getPropertyBoolean("Xml.LoadBest", true);
        iLoadInitial = getModel().getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        iLoadCurrent = getModel().getProperties().getPropertyBoolean("Xml.LoadCurrent", true);
        iLoadOfferings = getModel().getProperties().getPropertyBoolean("Xml.LoadOfferings", true);
        iLoadStudents = getModel().getProperties().getPropertyBoolean("Xml.LoadStudents", true);
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

    /** Set input file (e.g., if it is not set by General.Input property) */
    public void setInputFile(File inputFile) {
        iInputFile = inputFile;
    }

    /** Set student filter */
    public void setStudentFilter(StudentFilter filter) {
        iStudentFilter = filter;
    }

    /** Set whether to load students */
    public void setLoadStudents(boolean loadStudents) {
        iLoadStudents = loadStudents;
    }

    /** Set whether to load offerings */
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
    
    /**
     * Load data from the given XML root
     * @param root document root
     * @throws DocumentException
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
                offeringTable.put(new Long(offering.getId()), offering);
                for (Course course : offering.getCourses()) {
                    courseTable.put(new Long(course.getId()), course);
                }
            }
        }

        if (iLoadStudents && root.element("students") != null) {
            loadStudents(root.element("students"), offeringTable, courseTable);
        }
        
        if (iLoadOfferings && root.element("constraints") != null) 
            loadLinkedSectiond(root.element("constraints"), offeringTable);
        
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
            offeringTable.put(new Long(offering.getId()), offering);
            getModel().addOffering(offering);
            
            for (Iterator<?> j = offeringEl.elementIterator("course"); j.hasNext();) {
                Element courseEl = (Element) j.next();
                Course course = loadCourse(courseEl, offering);
                courseTable.put(new Long(course.getId()), course);
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
        for (Iterator<?> k = configEl.elementIterator("subpart"); k.hasNext();) {
            Element subpartEl = (Element) k.next();
            Subpart subpart = loadSubpart(subpartEl, config, subpartTable, sectionTable, timetable);
            subpartTable.put(new Long(subpart.getId()), subpart);
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
        
        
        for (Iterator<?> l = subpartEl.elementIterator("section"); l.hasNext();) {
            Element sectionEl = (Element) l.next();
            Section section = loadSection(sectionEl, subpart, sectionTable, timetable);
            sectionTable.put(new Long(section.getId()), section);
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
        
        Section section = new Section(
                Long.parseLong(sectionEl.attributeValue("id")),
                Integer.parseInt(sectionEl.attributeValue("limit")),
                sectionEl.attributeValue("name", "S" + sectionEl.attributeValue("id")),
                subpart, placement, sectionEl.attributeValue("instructorIds"),
                sectionEl.attributeValue("instructorNames"), parentSection);
        
        section.setSpaceHeld(Double.parseDouble(sectionEl.attributeValue("hold", "0.0")));
        section.setSpaceExpected(Double.parseDouble(sectionEl.attributeValue("expect", "0.0")));
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
        } else if ("curriculum".equals(reservationEl.attributeValue("type"))) {
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
            r = new CurriculumReservation(Long.valueOf(reservationEl.attributeValue("id")),
                    Double.parseDouble(reservationEl.attributeValue("limit", "-1")),
                    offering,
                    reservationEl.attributeValue("area"),
                    classifications, majors);
        } else if ("course".equals(reservationEl.attributeValue("type"))) {
            long courseId = Long.parseLong(reservationEl.attributeValue("course"));
            for (Course course: offering.getCourses()) {
                if (course.getId() == courseId)
                    r = new CourseReservation(Long.valueOf(reservationEl.attributeValue("id")), course);
            }
        } else if ("dummy".equals(reservationEl.attributeValue("type"))) {
            r = new DummyReservation(offering);
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
    protected void loadLinkedSectiond(Element constraintsEl, Map<Long, Offering> offeringTable) {
        for (Iterator<?> i = constraintsEl.elementIterator("linked-sections"); i.hasNext();) {
            Element linkedEl = (Element) i.next();
            List<Section> sections = new ArrayList<Section>();
            for (Iterator<?> j = linkedEl.elementIterator("section"); j.hasNext();) {
                Element sectionEl = (Element) j.next();
                Offering offering = offeringTable.get(Long.valueOf(sectionEl.attributeValue("offering")));
                sections.add(offering.getSection(Long.valueOf(sectionEl.attributeValue("id"))));
            }
            getModel().addLinkedSections(sections);
        }
    }
    
    /**
     * Load students
     * @param studentsEl students element
     * @param offeringTable offering table
     * @param courseTable course table
     */
    protected void loadStudents(Element studentsEl, Map<Long, Offering> offeringTable, Map<Long, Course> courseTable) {
        List<Enrollment> bestEnrollments = new ArrayList<Enrollment>();
        List<Enrollment> currentEnrollments = new ArrayList<Enrollment>();
        for (Iterator<?> i = studentsEl.elementIterator("student"); i.hasNext();) {
            Element studentEl = (Element) i.next();
            Student student = loadStudent(studentEl);
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
            }
            getModel().addStudent(student);
        }

        if (!bestEnrollments.isEmpty()) {
            // Enrollments with a reservation must go first
            for (Enrollment enrollment : bestEnrollments) {
                if (enrollment.getReservation() == null) continue;
                Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(enrollment);
                if (conflicts.isEmpty())
                    enrollment.variable().assign(0, enrollment);
                else
                    sLogger.warn("Enrollment " + enrollment + " conflicts with " + conflicts);
            }
            for (Enrollment enrollment : bestEnrollments) {
                if (enrollment.getReservation() != null) continue;
                Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(enrollment);
                if (conflicts.isEmpty())
                    enrollment.variable().assign(0, enrollment);
                else
                    sLogger.warn("Enrollment " + enrollment + " conflicts with " + conflicts);
            }
            getModel().saveBest();
        }

        if (!currentEnrollments.isEmpty()) {
            for (Request request : getModel().variables()) {
                if (request.getAssignment() != null)
                    request.unassign(0);
            }
            // Enrollments with a reservation must go first
            for (Enrollment enrollment : currentEnrollments) {
                if (enrollment.getReservation() == null) continue;
                Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(enrollment);
                if (conflicts.isEmpty())
                    enrollment.variable().assign(0, enrollment);
                else
                    sLogger.warn("Enrollment " + enrollment + " conflicts with " + conflicts);
            }
            for (Enrollment enrollment : currentEnrollments) {
                if (enrollment.getReservation() != null) continue;
                Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(enrollment);
                if (conflicts.isEmpty())
                    enrollment.variable().assign(0, enrollment);
                else
                    sLogger.warn("Enrollment " + enrollment + " conflicts with " + conflicts);
            }
        }
    }
    
    /**
     * Load student
     * @param studentEl student element
     * @return loaded student
     */
    protected Student loadStudent(Element studentEl) {
        Student student = new Student(Long.parseLong(studentEl.attributeValue("id")), "true".equals(studentEl.attributeValue("dummy")));
        student.setExternalId(studentEl.attributeValue("externalId"));
        student.setName(studentEl.attributeValue("name"));
        student.setStatus(studentEl.attributeValue("status"));
        for (Iterator<?> j = studentEl.elementIterator(); j.hasNext();) {
            Element requestEl = (Element) j.next();
            if ("classification".equals(requestEl.getName())) {
                student.getAcademicAreaClasiffications().add(
                        new AcademicAreaCode(requestEl.attributeValue("area"), requestEl.attributeValue("code")));
            } else if ("major".equals(requestEl.getName())) {
                student.getMajors().add(
                        new AcademicAreaCode(requestEl.attributeValue("area"), requestEl.attributeValue("code")));
            } else if ("minor".equals(requestEl.getName())) {
                student.getMinors().add(
                        new AcademicAreaCode(requestEl.attributeValue("area"), requestEl.attributeValue("code")));
            }
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
                "true".equals(requestEl.attributeValue("waitlist", "false")), timeStamp);
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
                Section section = courseRequest.getSection(Long.parseLong(sectionEl.attributeValue("id")));
                sections.add(section);
            }
            Reservation reservation = null;
            if (enrollmentEl.attributeValue("reservation", null) != null) {
                long reservationId = Long.valueOf(enrollmentEl.attributeValue("reservation"));
                for (Course course: courseRequest.getCourses())
                    for (Reservation r: course.getOffering().getReservations())
                        if (r.getId() == reservationId) { reservation = r; break; }
            }
            if (!sections.isEmpty())
                return courseRequest.createEnrollment(sections, reservation);
        } else if (request instanceof FreeTimeRequest) {
            return ((FreeTimeRequest)request).createEnrollment();
        }
        return null;
    }

}
