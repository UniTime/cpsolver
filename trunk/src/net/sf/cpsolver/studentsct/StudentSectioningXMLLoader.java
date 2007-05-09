package net.sf.cpsolver.studentsct;

import java.io.File;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
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

public class StudentSectioningXMLLoader extends StudentSectioningLoader {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(StudentSectioningXMLLoader.class);

    private File iInputFile;
    
    public StudentSectioningXMLLoader(StudentSectioningModel model) {
        super(model);
        iInputFile = new File(getModel().getProperties().getProperty("General.Input","."+File.separator+"solution.xml"));
    }
    
    private Solver iSolver = null;
    
    public void setSolver(Solver solver) {
        iSolver = solver;
    }
    public Solver getSolver() {
        return iSolver;
    }

    public void setInputFile(File inputFile) {
        iInputFile=inputFile;
    }
     
    private static BitSet createBitSet(String bitString) {
        BitSet ret = new BitSet(bitString.length());
        for (int i=0;i<bitString.length();i++)
            if (bitString.charAt(i)=='1') ret.set(i);
        return ret;
    }

    public void load() throws Exception {
        load(null);
    }
    
    public void load(Solution currentSolution) throws Exception {
        sLogger.debug("Reading XML data from "+iInputFile);
        
        Document document = (new SAXReader()).read(iInputFile);
        Element root = document.getRootElement();
        sLogger.debug("Root element: "+root.getName());
        if (!"sectioning".equals(root.getName())) {
            sLogger.error("Given XML file is not large lecture room timetabling problem.");
            return;
        }
        
        if (root.attributeValue("term")!=null)
            getModel().getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attributeValue("year")!=null)
            getModel().getProperties().setProperty("Data.Year", root.attributeValue("year"));
        if (root.attributeValue("initiative")!=null)
            getModel().getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));
        String version = root.attributeValue("version");
        
        Hashtable subpartTable = new Hashtable();
        Hashtable offeringTable = new Hashtable();
        Hashtable sectionTable = new Hashtable();
        Hashtable courseTable = new Hashtable();
        
        for (Iterator i=root.element("offerings").elementIterator("offering");i.hasNext();) {
            Element offeringEl = (Element)i.next();
            Offering offering = new Offering(
                    Long.parseLong(offeringEl.attributeValue("id")),
                    offeringEl.attributeValue("name","O"+offeringEl.attributeValue("id")));
            offeringTable.put(new Long(offering.getId()), offering);
            getModel().addOffering(offering);
            for (Iterator j=offeringEl.elementIterator("course");j.hasNext();) {
                Element courseEl = (Element)j.next();
                Course course = new Course(
                        Long.parseLong(courseEl.attributeValue("id")),
                        courseEl.attributeValue("subjectArea",""),
                        courseEl.attributeValue("courseNbr","C"+courseEl.attributeValue("id")),
                        offering);
                courseTable.put(new Long(course.getId()), course);
            }
            for (Iterator j=offeringEl.elementIterator("config");j.hasNext();) {
                Element configEl = (Element)j.next();
                Config config = new Config(
                        Long.parseLong(configEl.attributeValue("id")),
                        configEl.attributeValue("name","G"+configEl.attributeValue("id")),
                        offering);
                for (Iterator k=configEl.elementIterator("subpart");k.hasNext();) {
                    Element subpartEl = (Element)k.next();
                    Subpart parentSubpart = null;
                    if (subpartEl.attributeValue("parent")!=null)
                        parentSubpart = (Subpart)subpartTable.get(Long.valueOf(subpartEl.attributeValue("parent")));
                    Subpart subpart = new Subpart(
                            Long.parseLong(subpartEl.attributeValue("id")),
                            subpartEl.attributeValue("itype"),
                            subpartEl.attributeValue("name","P"+subpartEl.attributeValue("id")),
                            config,
                            parentSubpart);
                    subpartTable.put(new Long(subpart.getId()),subpart);
                    for (Iterator l=subpartEl.elementIterator("section");l.hasNext();) {
                        Element sectionEl = (Element)l.next();
                        Section parentSection = null;
                        if (sectionEl.attributeValue("parent")!=null)
                            parentSection = (Section)sectionTable.get(Long.valueOf(sectionEl.attributeValue("parent")));
                        TimeLocation time = null;
                        Element timeEl = sectionEl.element("time");
                        if (timeEl!=null) {
                            time = new TimeLocation(
                                Integer.parseInt(timeEl.attributeValue("days"),2),
                                Integer.parseInt(timeEl.attributeValue("start")),
                                Integer.parseInt(timeEl.attributeValue("length")),
                                0, 0, 
                                timeEl.attributeValue("datePattern")==null?null:Long.valueOf(timeEl.attributeValue("datePattern")),
                                timeEl.attributeValue("datePatternName",""),
                                createBitSet(timeEl.attributeValue("dates")),
                                Integer.parseInt(timeEl.attributeValue("breakTime","0")));
                           if (timeEl.attributeValue("pattern")!=null)
                                time.setTimePatternId(Long.valueOf(timeEl.attributeValue("pattern")));
                        }
                        Vector rooms = new Vector();
                        for (Iterator m=sectionEl.elementIterator("room");m.hasNext();) {
                            Element roomEl = (Element)m.next();
                            int posX=-1, posY=-1;
                            if (roomEl.attributeValue("location")!=null) {
                                String loc = roomEl.attributeValue("location");
                                posX = Integer.parseInt(loc.substring(0,loc.indexOf(',')));
                                posY = Integer.parseInt(loc.substring(loc.indexOf(',')+1));
                            }
                            RoomLocation room = new RoomLocation(
                                Long.valueOf(roomEl.attributeValue("id")),
                                roomEl.attributeValue("name","R"+roomEl.attributeValue("id")),
                                roomEl.attributeValue("building")==null?null:Long.valueOf(roomEl.attributeValue("building")),
                                0,
                                Integer.parseInt(roomEl.attributeValue("capacity")),
                                posX, posY, 
                                "true".equals(roomEl.attributeValue("ignoreTooFar")),
                                null);
                            rooms.add(room);                                
                        }
                        Placement placement = (time==null?null:new Placement(null, time, rooms));
                        Section section = new Section(
                            Long.parseLong(sectionEl.attributeValue("id")),
                            Integer.parseInt(sectionEl.attributeValue("limit")),
                            sectionEl.attributeValue("name","S"+sectionEl.attributeValue("id")),
                            subpart,
                            placement,
                            sectionEl.attributeValue("instructorIds"),
                            sectionEl.attributeValue("instructorNames"),
                            parentSection);
                        sectionTable.put(new Long(section.getId()), section);
                        section.setSpaceHeld(Double.parseDouble(sectionEl.attributeValue("hold", "0.0")));
                        section.setSpaceExpected(Double.parseDouble(sectionEl.attributeValue("expect", "0.0")));
                    }
                }
            }
        }
        
        Vector bestEnrollments = new Vector();
        Vector currentEnrollments = new Vector();
        
        for (Iterator i=root.element("students").elementIterator("student");i.hasNext();) {
            Element studentEl = (Element)i.next();
            Student student = new Student(
                Long.parseLong(studentEl.attributeValue("id")),
                "true".equals(studentEl.attributeValue("dummy")));
            for (Iterator j=studentEl.elementIterator();j.hasNext();) {
                Element requestEl = (Element)j.next();
                if ("freeTime".equals(requestEl.getName())) {
                    TimeLocation time = new TimeLocation(
                                Integer.parseInt(requestEl.attributeValue("days"),2),
                                Integer.parseInt(requestEl.attributeValue("start")),
                                Integer.parseInt(requestEl.attributeValue("length")),
                                0, 0, 
                                requestEl.attributeValue("datePattern")==null?null:Long.valueOf(requestEl.attributeValue("datePattern")),
                                "",
                                createBitSet(requestEl.attributeValue("dates")),
                                0);
                    FreeTimeRequest request = new FreeTimeRequest(
                        Long.parseLong(requestEl.attributeValue("id")),
                        Integer.parseInt(requestEl.attributeValue("priority")),
                        "true".equals(requestEl.attribute("alternative")),
                        student,
                        time);
                    if (requestEl.element("best")!=null)
                        bestEnrollments.add((Enrollment)request.computeEnrollments().firstElement());
                    if (requestEl.element("initial")!=null)
                        request.setInitialAssignment((Enrollment)request.computeEnrollments().firstElement());
                    if (requestEl.element("current")!=null)
                        currentEnrollments.add((Enrollment)request.computeEnrollments().firstElement());
                } else if ("course".equals(requestEl.getName())) {
                    Vector courses = new Vector();
                    courses.add(courseTable.get(Long.valueOf(requestEl.attributeValue("course"))));
                    for (Iterator k=requestEl.elementIterator("alternative");k.hasNext();)
                        courses.add(courseTable.get(Long.valueOf(((Element)k.next()).attributeValue("course"))));
                    CourseRequest courseRequest = new CourseRequest(
                        Long.parseLong(requestEl.attributeValue("id")),
                        Integer.parseInt(requestEl.attributeValue("priority")),
                        "true".equals(requestEl.attribute("alternative")),
                        student,
                        courses,
                        "true".equals(requestEl.attribute("waitlist")));
                    for (Iterator k=requestEl.elementIterator("waitlisted");k.hasNext();) {
                        Element choiceEl = (Element)k.next();
                        courseRequest.getWaitlistedChoices().add(new Choice(
                            (Offering)offeringTable.get(Long.valueOf(choiceEl.attributeValue("offering"))),
                            choiceEl.getText()));
                    }
                    for (Iterator k=requestEl.elementIterator("selected");k.hasNext();) {
                        Element choiceEl = (Element)k.next();
                        courseRequest.getSelectedChoices().add(new Choice(
                            (Offering)offeringTable.get(Long.valueOf(choiceEl.attributeValue("offering"))),
                            choiceEl.getText()));
                    }
                    Element initialEl = requestEl.element("initial");
                    if (initialEl!=null) {
                        HashSet sections = new HashSet();
                        Config config = null;
                        for (Iterator k=initialEl.elementIterator("section");k.hasNext();) {
                            Element sectionEl = (Element)k.next();
                            Section section = courseRequest.getSection(Long.parseLong(sectionEl.attributeValue("id")));
                            if (config==null) config = section.getSubpart().getConfig();
                            sections.add(section);
                        }
                        Enrollment enrollment = new Enrollment(courseRequest, 0, config, sections);
                        courseRequest.setInitialAssignment(enrollment);
                    }
                    Element currentEl = requestEl.element("current");
                    if (currentEl!=null) {
                        HashSet sections = new HashSet();
                        Config config = null;
                        for (Iterator k=currentEl.elementIterator("section");k.hasNext();) {
                            Element sectionEl = (Element)k.next();
                            Section section = courseRequest.getSection(Long.parseLong(sectionEl.attributeValue("id")));
                            if (config==null) config = section.getSubpart().getConfig();
                            sections.add(section);
                        }
                        Enrollment enrollment = new Enrollment(courseRequest, 0, config, sections);
                        currentEnrollments.add(enrollment);
                    }
                    Element bestEl = requestEl.element("best");
                    if (bestEl!=null) {
                        HashSet sections = new HashSet();
                        Config config = null;
                        for (Iterator k=bestEl.elementIterator("section");k.hasNext();) {
                            Element sectionEl = (Element)k.next();
                            Section section = courseRequest.getSection(Long.parseLong(sectionEl.attributeValue("id")));
                            if (config==null) config = section.getSubpart().getConfig();
                            sections.add(section);
                        }
                        Enrollment enrollment = new Enrollment(courseRequest, 0, config, sections);
                        bestEnrollments.add(enrollment);
                    }
                }
            }
            getModel().addStudent(student);
        }
        
        if (!bestEnrollments.isEmpty()) {
            for (Enumeration e=bestEnrollments.elements();e.hasMoreElements();) {
                Enrollment enrollment = (Enrollment)e.nextElement();
                enrollment.variable().assign(0, enrollment);
            }
            getModel().saveBest();
        } 
        
        if (!currentEnrollments.isEmpty()) {
            for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
                Request request = (Request)e.nextElement();
                if (request.getAssignment()!=null)
                    request.unassign(0);
            }
            for (Enumeration e=currentEnrollments.elements();e.hasMoreElements();) {
                Enrollment enrollment = (Enrollment)e.nextElement();
                enrollment.variable().assign(0, enrollment);
            }
        }
        
        sLogger.debug("Model successfully loaded.");
    }
    
        
}
