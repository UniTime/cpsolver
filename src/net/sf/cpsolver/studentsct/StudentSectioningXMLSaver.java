package net.sf.cpsolver.studentsct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import net.sf.cpsolver.coursett.IdConvertor;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.ToolBox;
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

public class StudentSectioningXMLSaver extends StudentSectioningSaver {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(StudentSectioningXMLSaver.class);
    private static DecimalFormat[] sDF = {new DecimalFormat(""),new DecimalFormat("0"),new DecimalFormat("00"),new DecimalFormat("000"),new DecimalFormat("0000"),new DecimalFormat("00000"),new DecimalFormat("000000"),new DecimalFormat("0000000")};
    private static DecimalFormat sStudentWeightFormat = new DecimalFormat("0.0000");
    private Solver iSolver = null; 
    private File iOutputFolder = null;
    
    private boolean iSaveBest = false;
    private boolean iSaveInitial = false;
    private boolean iSaveCurrent = false;
    private boolean iSaveOnlineSectioningInfo = false;
    
    private boolean iConvertIds = false;
    private boolean iShowNames = false;

    public StudentSectioningXMLSaver(Solver solver) {
        super(solver);
        iOutputFolder = new File(getModel().getProperties().getProperty("General.Output","."+File.separator+"output"));
        iSaveBest = getModel().getProperties().getPropertyBoolean("Xml.SaveBest", true);
        iSaveInitial = getModel().getProperties().getPropertyBoolean("Xml.SaveInitial", true);
        iSaveCurrent = getModel().getProperties().getPropertyBoolean("Xml.SaveCurrent", false);
        iSaveOnlineSectioningInfo = getModel().getProperties().getPropertyBoolean("Xml.SaveOnlineSectioningInfo", true);
        iShowNames = getModel().getProperties().getPropertyBoolean("Xml.ShowNames",true);
        iConvertIds = getModel().getProperties().getPropertyBoolean("Xml.ConvertIds",false);
    }

    private static String bitset2string(BitSet b) {
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<b.length();i++)
            sb.append(b.get(i)?"1":"0");
        return sb.toString();
    }
    
    private String getId(String type, String id) {
        if (!iConvertIds) return id.toString();
        return IdConvertor.getInstance().convert(type, id);
    }
    
    private String getId(String type, Number id) {
        return getId(type, id.toString());
    }
    
    private String getId(String type, long id) {
        return getId(type, String.valueOf(id));
    }

    public void save() throws Exception {
        save(null);
    }
    
    public void save(File outFile) throws Exception {
        if (outFile==null)
            outFile = new File(iOutputFolder,"solution.xml");
        outFile.getParentFile().mkdirs();
        sLogger.debug("Writting XML data to:"+outFile);
        
        Document document = DocumentHelper.createDocument();
        document.addComment("Purdue University Student Sectioning");
        
        if ((iSaveCurrent || iSaveBest) && !getModel().assignedVariables().isEmpty()) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Dictionary solutionInfo=(getSolution()==null?getModel().getInfo():getSolution().getInfo());
            for (Enumeration e=ToolBox.sortEnumeration(solutionInfo.keys());e.hasMoreElements();) {
                String key = (String)e.nextElement();
                Object value = solutionInfo.get(key);
                comments.append("    "+key+": "+value+"\n");
            }
            document.addComment(comments.toString());
        }
        
        Element root = document.addElement("sectioning");
        root.addAttribute("version","1.0");
        root.addAttribute("initiative", getModel().getProperties().getProperty("Data.Initiative"));
        root.addAttribute("term", getModel().getProperties().getProperty("Data.Term"));
        root.addAttribute("year", getModel().getProperties().getProperty("Data.Year"));
        root.addAttribute("created", String.valueOf(new Date()));

        Element offeringsEl = root.addElement("offerings");
        for (Enumeration e=getModel().getOfferings().elements();e.hasMoreElements();) {
            Offering offering = (Offering)e.nextElement();
            Element offeringEl = offeringsEl.addElement("offering");
            offeringEl.addAttribute("id", getId("offering", offering.getId()));
            if (iShowNames)
                offeringEl.addAttribute("name", offering.getName());
            for (Enumeration f=offering.getCourses().elements();f.hasMoreElements();) {
                Course course = (Course)f.nextElement();
                Element courseEl = offeringEl.addElement("course");
                courseEl.addAttribute("id", getId("course", course.getId()));
                if (iShowNames)
                    courseEl.addAttribute("subjectArea", course.getSubjectArea());
                if (iShowNames)
                    courseEl.addAttribute("courseNbr", course.getCourseNumber());
            }
            for (Enumeration f=offering.getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                Element configEl = offeringEl.addElement("config");
                configEl.addAttribute("id", getId("config", config.getId()));
                if (iShowNames)
                    configEl.addAttribute("name", config.getName());
                for (Enumeration g=config.getSubparts().elements();g.hasMoreElements();) {
                    Subpart subpart = (Subpart)g.nextElement();
                    Element subpartEl = configEl.addElement("subpart");
                    subpartEl.addAttribute("id", getId("subpart", subpart.getId()));
                    subpartEl.addAttribute("itype", subpart.getInstructionalType());
                    if (subpart.getParent()!=null)
                        subpartEl.addAttribute("parent", getId("subpart", subpart.getParent().getId()));
                    if (iShowNames)
                        subpartEl.addAttribute("name", subpart.getName());
                    for (Enumeration h=subpart.getSections().elements();h.hasMoreElements();) {
                        Section section = (Section)h.nextElement();
                        Element sectionEl = subpartEl.addElement("section");
                        sectionEl.addAttribute("id", getId("section", section.getId()));
                        sectionEl.addAttribute("limit", String.valueOf(section.getLimit()));
                        if (section.getParent()!=null)
                            sectionEl.addAttribute("parent", getId("section", section.getParent().getId()));
                        if (section.getChoice().getInstructorIds()!=null)
                            sectionEl.addAttribute("instructorIds", section.getChoice().getInstructorIds());
                        if (iShowNames && section.getChoice().getInstructorNames()!=null)
                            sectionEl.addAttribute("instructorNames", section.getChoice().getInstructorNames());
                        if (iShowNames) sectionEl.addAttribute("name", section.getName());
                        if (section.getPlacement()!=null) {
                            TimeLocation tl = section.getPlacement().getTimeLocation();
                            if (tl!=null) {
                                Element timeLocationEl = (Element)sectionEl.addElement("time");
                                timeLocationEl.addAttribute("days", sDF[7].format(Long.parseLong(Integer.toBinaryString(tl.getDayCode()))));
                                timeLocationEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                                timeLocationEl.addAttribute("length", String.valueOf(tl.getLength()));
                                if (tl.getBreakTime()!=0)
                                    timeLocationEl.addAttribute("breakTime", String.valueOf(tl.getBreakTime()));
                                if (iShowNames && tl.getTimePatternId()!=null)
                                    timeLocationEl.addAttribute("pattern", getId("timePattern", tl.getTimePatternId()));
                                if (tl.getDatePatternId()!=null)
                                    timeLocationEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                                if (tl.getDatePatternName()!=null && tl.getDatePatternName().length()>0)
                                    timeLocationEl.addAttribute("datePatternName", tl.getDatePatternName());
                                timeLocationEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                                if (iShowNames)
                                    timeLocationEl.setText(tl.getLongName());
                            }
                            for (Enumeration i=section.getRooms().elements();i.hasMoreElements();) {
                                RoomLocation rl = (RoomLocation)i.nextElement();
                                Element roomLocationEl = (Element)sectionEl.addElement("room");
                                roomLocationEl.addAttribute("id", getId("room", rl.getId()));
                                if (iShowNames && rl.getBuildingId()!=null)
                                    roomLocationEl.addAttribute("building", getId("building", rl.getBuildingId()));
                                if (iShowNames && rl.getName()!=null)
                                    roomLocationEl.addAttribute("name",rl.getName());
                                roomLocationEl.addAttribute("capacity", String.valueOf(rl.getRoomSize()));
                                if (rl.getPosX()>0 || rl.getPosY()>0)
                                    roomLocationEl.addAttribute("location", rl.getPosX()+","+rl.getPosY());
                                if (rl.getIgnoreTooFar())
                                    roomLocationEl.addAttribute("ignoreTooFar", "true");
                            }
                        }
                        if (iSaveOnlineSectioningInfo) {
                            if (section.getSpaceHeld()!=0.0)
                                sectionEl.addAttribute("hold", sStudentWeightFormat.format(section.getSpaceHeld()));
                            if (section.getSpaceExpected()!=0.0)
                                sectionEl.addAttribute("expect", sStudentWeightFormat.format(section.getSpaceExpected()));
                        }
                    }
                }
            }
        }
        
        Element studentsEl = root.addElement("students");
        for (Enumeration e=getModel().getStudents().elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            Element studentEl = studentsEl.addElement("student");
            studentEl.addAttribute("id", getId("student", student.getId()));
            if (student.isDummy())
                studentEl.addAttribute("dummy", "true");
            for (Enumeration f=student.getRequests().elements();f.hasMoreElements();) {
                Request request = (Request)f.nextElement();
                if (request instanceof FreeTimeRequest) {
                    Element requestEl = studentEl.addElement("freeTime");
                    FreeTimeRequest ft = (FreeTimeRequest)request;
                    requestEl.addAttribute("id", getId("request", request.getId()));
                    requestEl.addAttribute("priority", String.valueOf(request.getPriority()));
                    if (request.isAlternative())
                        requestEl.addAttribute("alternative", "true");
                    if (request.getWeight()!=1.0)
                        requestEl.addAttribute("weight", sStudentWeightFormat.format(request.getWeight()));
                    TimeLocation tl = ft.getTime();
                    if (tl!=null) {
                        requestEl.addAttribute("days", sDF[7].format(Long.parseLong(Integer.toBinaryString(tl.getDayCode()))));
                        requestEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                        requestEl.addAttribute("length", String.valueOf(tl.getLength()));
                        if (tl.getDatePatternId()!=null)
                            requestEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                        requestEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                        if (iShowNames)
                            requestEl.setText(tl.getLongName());
                    }
                    if (iSaveInitial && request.getInitialAssignment()!=null) {
                        Element assignmentEl = requestEl.addElement("initial");
                    }
                    if (iSaveCurrent && request.getAssignment()!=null) {
                        Element assignmentEl = requestEl.addElement("current");
                    }
                    if (iSaveBest && request.getBestAssignment()!=null) {
                        Element assignmentEl = requestEl.addElement("best");
                    }
                } else if (request instanceof CourseRequest) {
                    CourseRequest cr = (CourseRequest)request;
                    Element requestEl = studentEl.addElement("course");
                    requestEl.addAttribute("id", getId("request", request.getId()));
                    requestEl.addAttribute("priority", String.valueOf(request.getPriority()));
                    if (request.isAlternative())
                        requestEl.addAttribute("alternative", "true");
                    if (request.getWeight()!=1.0)
                        requestEl.addAttribute("weight", sStudentWeightFormat.format(request.getWeight()));
                    if (cr.isWaitlist())
                        requestEl.addAttribute("waitlist", "true");
                    boolean first = true;
                    for (Enumeration g=cr.getCourses().elements();g.hasMoreElements();) {
                        Course course = (Course)g.nextElement();
                        if (first)
                            requestEl.addAttribute("course", getId("course", course.getId()));
                        else
                            requestEl.addElement("alternative").addAttribute("course", getId("course", course.getId()));
                        first = false;
                    }
                    for (Iterator i=cr.getWaitlistedChoices().iterator();i.hasNext();) {
                        Choice choice = (Choice)i.next();
                        Element choiceEl = requestEl.addElement("waitlisted");
                        choiceEl.addAttribute("offering", getId("offering", choice.getOffering().getId()));
                        choiceEl.setText(choice.getId());
                    }
                    for (Iterator i=cr.getSelectedChoices().iterator();i.hasNext();) {
                        Choice choice = (Choice)i.next();
                        Element choiceEl = requestEl.addElement("selected");
                        choiceEl.addAttribute("offering", getId("offering", choice.getOffering().getId()));
                        choiceEl.setText(choice.getId());
                    }
                    if (iSaveInitial && request.getInitialAssignment()!=null) {
                        Element assignmentEl = requestEl.addElement("initial");
                        Enrollment enrollment = (Enrollment)request.getInitialAssignment();
                        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
                            Section section = (Section)i.next();
                            Element sectionEl = assignmentEl.addElement("section").addAttribute("id", getId("section", section.getId()));
                            if (iShowNames)
                                sectionEl.setText(section.getName()+" "+
                                        (section.getTime()==null?" Arr Hrs":" "+section.getTime().getLongName())+
                                        (section.getNrRooms()==0?"":" "+section.getPlacement().getRoomName(","))+
                                        (section.getChoice().getInstructorNames()==null?"":" "+section.getChoice().getInstructorNames()));
                        }
                    }
                    if (iSaveCurrent && request.getAssignment()!=null) {
                        Element assignmentEl = requestEl.addElement("current");
                        Enrollment enrollment = (Enrollment)request.getAssignment();
                        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
                            Section section = (Section)i.next();
                            Element sectionEl = assignmentEl.addElement("section").addAttribute("id", getId("section", section.getId()));
                            if (iShowNames)
                                sectionEl.setText(section.getName()+" "+
                                        (section.getTime()==null?" Arr Hrs":" "+section.getTime().getLongName())+
                                        (section.getNrRooms()==0?"":" "+section.getPlacement().getRoomName(","))+
                                        (section.getChoice().getInstructorNames()==null?"":" "+section.getChoice().getInstructorNames()));
                        }
                    }
                    if (iSaveBest && request.getBestAssignment()!=null) {
                        Element assignmentEl = requestEl.addElement("best");
                        Enrollment enrollment = (Enrollment)request.getBestAssignment();
                        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
                            Section section = (Section)i.next();
                            Element sectionEl = assignmentEl.addElement("section").addAttribute("id", getId("section", section.getId()));
                            if (iShowNames)
                                sectionEl.setText(section.getName()+" "+
                                        (section.getTime()==null?" Arr Hrs":" "+section.getTime().getLongName())+
                                        (section.getNrRooms()==0?"":" "+section.getPlacement().getRoomName(","))+
                                        (section.getChoice().getInstructorNames()==null?"":" "+section.getChoice().getInstructorNames()));
                        }
                    }
                }
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            (new XMLWriter(fos,OutputFormat.createPrettyPrint())).write(document);
            fos.flush();fos.close();fos=null;
        } finally {
            try {
                if (fos!=null) fos.close();
            } catch (IOException e) {}
        }
        
        if (iConvertIds) IdConvertor.getInstance().save();
    }
    

}
