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

import org.dom4j.Document;
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
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
        sLogger.debug("Root element: " + root.getName());
        if (!"sectioning".equals(root.getName())) {
            sLogger.error("Given XML file is not student sectioning problem.");
            return;
        }
        
        HashMap<Long, Placement> timetable = null;
        if (iTimetableFile != null) {
            sLogger.info("Reading timetable from " + iTimetableFile + " ...");
            Document timetableDocument = (new SAXReader()).read(iTimetableFile);
            Element timetableRoot = timetableDocument.getRootElement();
            if (!"timetable".equals(timetableRoot.getName())) {
                sLogger.error("Given XML file is not course timetabling problem.");
                return;
            }
            timetable = new HashMap<Long, Placement>();
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
                RoomLocation room = new RoomLocation(Long.valueOf(roomEl.attributeValue("id")), roomEl
                        .attributeValue("name", "R" + roomEl.attributeValue("id")), roomEl
                        .attributeValue("building") == null ? null : Long.valueOf(roomEl
                        .attributeValue("building")), 0, Integer.parseInt(roomEl
                        .attributeValue("capacity")), posX, posY, "true".equals(roomEl
                        .attributeValue("ignoreTooFar")), null);
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
                    time = new TimeLocation(Integer.parseInt(timeEl.attributeValue("days"), 2), Integer
                            .parseInt(timeEl.attributeValue("start")), Integer.parseInt(timeEl
                            .attributeValue("length")), 0, 0,
                            classEl.attributeValue("datePattern") == null ? null : Long.valueOf(classEl
                                    .attributeValue("datePattern")), classEl.attributeValue(
                                    "datePatternName", ""), createBitSet(classEl.attributeValue("dates")),
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
        }

        Progress.getInstance(getModel()).load(root, true);
        Progress.getInstance(getModel()).message(Progress.MSGLEVEL_STAGE, "Restoring from backup ...");

        if (root.attributeValue("term") != null)
            getModel().getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attributeValue("year") != null)
            getModel().getProperties().setProperty("Data.Year", root.attributeValue("year"));
        if (root.attributeValue("initiative") != null)
            getModel().getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));

        HashMap<Long, Offering> offeringTable = new HashMap<Long, Offering>();
        HashMap<Long, Course> courseTable = new HashMap<Long, Course>();

        if (iLoadOfferings && root.element("offerings") != null) {
            HashMap<Long, Subpart> subpartTable = new HashMap<Long, Subpart>();
            HashMap<Long, Section> sectionTable = new HashMap<Long, Section>();
            for (Iterator<?> i = root.element("offerings").elementIterator("offering"); i.hasNext();) {
                Element offeringEl = (Element) i.next();
                Offering offering = new Offering(Long.parseLong(offeringEl.attributeValue("id")), offeringEl
                        .attributeValue("name", "O" + offeringEl.attributeValue("id")));
                offeringTable.put(new Long(offering.getId()), offering);
                getModel().addOffering(offering);
                for (Iterator<?> j = offeringEl.elementIterator("course"); j.hasNext();) {
                    Element courseEl = (Element) j.next();
                    Course course = new Course(Long.parseLong(courseEl.attributeValue("id")), courseEl.attributeValue(
                            "subjectArea", ""), courseEl.attributeValue("courseNbr", "C"
                            + courseEl.attributeValue("id")), offering, Integer.parseInt(courseEl.attributeValue(
                            "limit", "0")), Integer.parseInt(courseEl.attributeValue("projected", "0")));
                    courseTable.put(new Long(course.getId()), course);
                }
                for (Iterator<?> j = offeringEl.elementIterator("config"); j.hasNext();) {
                    Element configEl = (Element) j.next();
                    Config config = new Config(Long.parseLong(configEl.attributeValue("id")), configEl.attributeValue(
                            "name", "G" + configEl.attributeValue("id")), offering);
                    for (Iterator<?> k = configEl.elementIterator("subpart"); k.hasNext();) {
                        Element subpartEl = (Element) k.next();
                        Subpart parentSubpart = null;
                        if (subpartEl.attributeValue("parent") != null)
                            parentSubpart = subpartTable.get(Long.valueOf(subpartEl.attributeValue("parent")));
                        Subpart subpart = new Subpart(Long.parseLong(subpartEl.attributeValue("id")), subpartEl
                                .attributeValue("itype"), subpartEl.attributeValue("name", "P"
                                + subpartEl.attributeValue("id")), config, parentSubpart);
                        subpartTable.put(new Long(subpart.getId()), subpart);
                        for (Iterator<?> l = subpartEl.elementIterator("section"); l.hasNext();) {
                            Element sectionEl = (Element) l.next();
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
                                    time = new TimeLocation(Integer.parseInt(timeEl.attributeValue("days"), 2), Integer
                                            .parseInt(timeEl.attributeValue("start")), Integer.parseInt(timeEl
                                            .attributeValue("length")), 0, 0,
                                            timeEl.attributeValue("datePattern") == null ? null : Long.valueOf(timeEl
                                                    .attributeValue("datePattern")), timeEl.attributeValue(
                                                    "datePatternName", ""), createBitSet(timeEl.attributeValue("dates")),
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
                                    RoomLocation room = new RoomLocation(Long.valueOf(roomEl.attributeValue("id")), roomEl
                                            .attributeValue("name", "R" + roomEl.attributeValue("id")), roomEl
                                            .attributeValue("building") == null ? null : Long.valueOf(roomEl
                                            .attributeValue("building")), 0, Integer.parseInt(roomEl
                                            .attributeValue("capacity")), posX, posY, "true".equals(roomEl
                                            .attributeValue("ignoreTooFar")), null);
                                    rooms.add(room);
                                }
                                placement = (time == null ? null : new Placement(null, time, rooms));
                            }
                            Section section = new Section(Long.parseLong(sectionEl.attributeValue("id")), Integer
                                    .parseInt(sectionEl.attributeValue("limit")), sectionEl.attributeValue("name", "S"
                                    + sectionEl.attributeValue("id")), subpart, placement, sectionEl
                                    .attributeValue("instructorIds"), sectionEl.attributeValue("instructorNames"),
                                    parentSection);
                            sectionTable.put(new Long(section.getId()), section);
                            section.setSpaceHeld(Double.parseDouble(sectionEl.attributeValue("hold", "0.0")));
                            section.setSpaceExpected(Double.parseDouble(sectionEl.attributeValue("expect", "0.0")));
                        }
                    }
                }
            }
        } else {
            for (Offering offering : getModel().getOfferings()) {
                offeringTable.put(new Long(offering.getId()), offering);
                for (Course course : offering.getCourses()) {
                    courseTable.put(new Long(course.getId()), course);
                }
            }
        }

        if (iLoadStudents && root.element("students") != null) {
            List<Enrollment> bestEnrollments = new ArrayList<Enrollment>();
            List<Enrollment> currentEnrollments = new ArrayList<Enrollment>();
            for (Iterator<?> i = root.element("students").elementIterator("student"); i.hasNext();) {
                Element studentEl = (Element) i.next();
                Student student = new Student(Long.parseLong(studentEl.attributeValue("id")), "true".equals(studentEl
                        .attributeValue("dummy")));
                for (Iterator<?> j = studentEl.elementIterator(); j.hasNext();) {
                    Element requestEl = (Element) j.next();
                    if ("classification".equals(requestEl.getName())) {
                        student.getAcademicAreaClasiffications()
                                .add(
                                        new AcademicAreaCode(requestEl.attributeValue("area"), requestEl
                                                .attributeValue("code")));
                    } else if ("major".equals(requestEl.getName())) {
                        student.getMajors()
                                .add(
                                        new AcademicAreaCode(requestEl.attributeValue("area"), requestEl
                                                .attributeValue("code")));
                    } else if ("minor".equals(requestEl.getName())) {
                        student.getMinors()
                                .add(
                                        new AcademicAreaCode(requestEl.attributeValue("area"), requestEl
                                                .attributeValue("code")));
                    }
                }
                if (iStudentFilter != null && !iStudentFilter.accept(student))
                    continue;
                for (Iterator<?> j = studentEl.elementIterator(); j.hasNext();) {
                    Element requestEl = (Element) j.next();
                    if ("freeTime".equals(requestEl.getName())) {
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
                        if (iLoadBest && requestEl.element("best") != null)
                            bestEnrollments.add(request.createEnrollment());
                        if (iLoadInitial && requestEl.element("initial") != null)
                            request.setInitialAssignment(request.createEnrollment());
                        if (iLoadCurrent && requestEl.element("current") != null)
                            currentEnrollments.add(request.createEnrollment());
                    } else if ("course".equals(requestEl.getName())) {
                        List<Course> courses = new ArrayList<Course>();
                        courses.add(courseTable.get(Long.valueOf(requestEl.attributeValue("course"))));
                        for (Iterator<?> k = requestEl.elementIterator("alternative"); k.hasNext();)
                            courses.add(courseTable.get(Long.valueOf(((Element) k.next()).attributeValue("course"))));
                        CourseRequest courseRequest = new CourseRequest(Long.parseLong(requestEl.attributeValue("id")),
                                Integer.parseInt(requestEl.attributeValue("priority")), "true".equals(requestEl
                                        .attributeValue("alternative")), student, courses, "true".equals(requestEl
                                        .attributeValue("waitlist")));
                        if (requestEl.attributeValue("weight") != null)
                            courseRequest.setWeight(Double.parseDouble(requestEl.attributeValue("weight")));
                        for (Iterator<?> k = requestEl.elementIterator("waitlisted"); k.hasNext();) {
                            Element choiceEl = (Element) k.next();
                            courseRequest.getWaitlistedChoices().add(
                                    new Choice(offeringTable.get(Long.valueOf(choiceEl.attributeValue("offering"))),
                                            choiceEl.getText()));
                        }
                        for (Iterator<?> k = requestEl.elementIterator("selected"); k.hasNext();) {
                            Element choiceEl = (Element) k.next();
                            courseRequest.getSelectedChoices().add(
                                    new Choice(offeringTable.get(Long.valueOf(choiceEl.attributeValue("offering"))),
                                            choiceEl.getText()));
                        }
                        Element initialEl = requestEl.element("initial");
                        if (iLoadInitial && initialEl != null) {
                            HashSet<Section> sections = new HashSet<Section>();
                            for (Iterator<?> k = initialEl.elementIterator("section"); k.hasNext();) {
                                Element sectionEl = (Element) k.next();
                                Section section = courseRequest.getSection(Long.parseLong(sectionEl
                                        .attributeValue("id")));
                                sections.add(section);
                            }
                            if (!sections.isEmpty())
                                courseRequest.setInitialAssignment(courseRequest.createEnrollment(sections));
                        }
                        Element currentEl = requestEl.element("current");
                        if (iLoadCurrent && currentEl != null) {
                            HashSet<Section> sections = new HashSet<Section>();
                            for (Iterator<?> k = currentEl.elementIterator("section"); k.hasNext();) {
                                Element sectionEl = (Element) k.next();
                                Section section = courseRequest.getSection(Long.parseLong(sectionEl
                                        .attributeValue("id")));
                                sections.add(section);
                            }
                            if (!sections.isEmpty())
                                currentEnrollments.add(courseRequest.createEnrollment(sections));
                        }
                        Element bestEl = requestEl.element("best");
                        if (iLoadBest && bestEl != null) {
                            HashSet<Section> sections = new HashSet<Section>();
                            for (Iterator<?> k = bestEl.elementIterator("section"); k.hasNext();) {
                                Element sectionEl = (Element) k.next();
                                Section section = courseRequest.getSection(Long.parseLong(sectionEl
                                        .attributeValue("id")));
                                sections.add(section);
                            }
                            if (!sections.isEmpty())
                                bestEnrollments.add(courseRequest.createEnrollment(sections));
                        }
                    }
                }
                getModel().addStudent(student);
            }

            if (!bestEnrollments.isEmpty()) {
                for (Enrollment enrollment : bestEnrollments) {
                    Map<Constraint<Request, Enrollment>, Set<Enrollment>> conflicts = getModel().conflictConstraints(
                            enrollment);
                    if (conflicts.isEmpty())
                        enrollment.variable().assign(0, enrollment);
                    else {
                        sLogger.warn("Enrollment " + enrollment + " conflicts with " + conflicts);
                    }
                }
                getModel().saveBest();
            }

            if (!currentEnrollments.isEmpty()) {
                for (Request request : getModel().variables()) {
                    if (request.getAssignment() != null)
                        request.unassign(0);
                }
                for (Enrollment enrollment : currentEnrollments) {
                    enrollment.variable().assign(0, enrollment);
                }
            }
        }

        sLogger.debug("Model successfully loaded.");
    }

}
