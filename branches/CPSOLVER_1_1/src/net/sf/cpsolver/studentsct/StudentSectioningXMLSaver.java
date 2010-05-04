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
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;
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

/**
 * Save student sectioning solution into an XML file.
 *
 * <br><br>
 * Parameters:
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>General.Output</td><td>{@link String}</td><td>Folder with the output solution in XML format (solution.xml)</td></tr>
 * <tr><td>Xml.ConvertIds</td><td>{@link Boolean}</td><td>If true, ids are converted (to be able to make input data public)</td></tr>
 * <tr><td>Xml.ShowNames</td><td>{@link Boolean}</td><td>If false, names are not exported (to be able to make input data public)</td></tr>
 * <tr><td>Xml.SaveBest</td><td>{@link Boolean}</td><td>If true, best solution is saved.</td></tr>
 * <tr><td>Xml.SaveInitial</td><td>{@link Boolean}</td><td>If true, initial solution is saved.</td></tr>
 * <tr><td>Xml.SaveCurrent</td><td>{@link Boolean}</td><td>If true, current solution is saved.</td></tr>
 * <tr><td>Xml.SaveOnlineSectioningInfo</td><td>{@link Boolean}</td><td>If true, save online sectioning info (i.e., expected and held space of each section)</td></tr>
 * <tr><td>Xml.SaveStudentInfo</td><td>{@link Boolean}</td><td>If true, save student information (i.e., academic area classification, major, minor)</td></tr>
 * </table>
 * <br><br>
 * Usage:<br>
 * <code>
 * new StudentSectioningXMLSaver(solver).save(new File("solution.xml"));<br> 
 * </code>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

public class StudentSectioningXMLSaver extends StudentSectioningSaver {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(StudentSectioningXMLSaver.class);
    private static DecimalFormat[] sDF = {new DecimalFormat(""),new DecimalFormat("0"),new DecimalFormat("00"),new DecimalFormat("000"),new DecimalFormat("0000"),new DecimalFormat("00000"),new DecimalFormat("000000"),new DecimalFormat("0000000")};
    private static DecimalFormat sStudentWeightFormat = new DecimalFormat("0.0000");
    private File iOutputFolder = null;
    
    private boolean iSaveBest = false;
    private boolean iSaveInitial = false;
    private boolean iSaveCurrent = false;
    private boolean iSaveOnlineSectioningInfo = false;
    private boolean iSaveStudentInfo = true;
    
    private boolean iConvertIds = false;
    private boolean iShowNames = false;

    /**
     * Constructor
     * @param solver student sectioning solver
     */
    public StudentSectioningXMLSaver(Solver solver) {
        super(solver);
        iOutputFolder = new File(getModel().getProperties().getProperty("General.Output","."+File.separator+"output"));
        iSaveBest = getModel().getProperties().getPropertyBoolean("Xml.SaveBest", true);
        iSaveInitial = getModel().getProperties().getPropertyBoolean("Xml.SaveInitial", true);
        iSaveCurrent = getModel().getProperties().getPropertyBoolean("Xml.SaveCurrent", false);
        iSaveOnlineSectioningInfo = getModel().getProperties().getPropertyBoolean("Xml.SaveOnlineSectioningInfo", true);
        iSaveStudentInfo = getModel().getProperties().getPropertyBoolean("Xml.SaveStudentInfo", true);
        iShowNames = getModel().getProperties().getPropertyBoolean("Xml.ShowNames",true);
        iConvertIds = getModel().getProperties().getPropertyBoolean("Xml.ConvertIds",false);
    }

    /** Convert bitset to a bit string */
    private static String bitset2string(BitSet b) {
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<b.length();i++)
            sb.append(b.get(i)?"1":"0");
        return sb.toString();
    }
    
    /** Generate id for given object with the given id */
    private String getId(String type, String id) {
        if (!iConvertIds) return id.toString();
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
    public void save() throws Exception {
        save(null);
    }
    
    /** Save an XML file 
     * @param outFile output file 
     */
    public void save(File outFile) throws Exception {
        if (outFile==null)
            outFile = new File(iOutputFolder,"solution.xml");
        outFile.getParentFile().mkdirs();
        sLogger.debug("Writting XML data to:"+outFile);
        
        Document document = DocumentHelper.createDocument();
        document.addComment("Student Sectioning");
        
        if ((iSaveCurrent || iSaveBest)) { // && !getModel().assignedVariables().isEmpty()
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Dictionary solutionInfo=(getSolution()==null?getModel().getExtendedInfo():getSolution().getExtendedInfo());
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
                if (iShowNames && course.getLimit()!=0)
                    courseEl.addAttribute("limit", String.valueOf(course.getLimit()));
                if (iShowNames && course.getProjected()!=0)
                    courseEl.addAttribute("projected", String.valueOf(course.getProjected()));
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
                        if (iShowNames && section.getChoice().getInstructorIds()!=null)
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
                                if (iShowNames && tl.getDatePatternId()!=null)
                                    timeLocationEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                                if (iShowNames && tl.getDatePatternName()!=null && tl.getDatePatternName().length()>0)
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
            if (iSaveStudentInfo) {
                for (Enumeration f=student.getAcademicAreaClasiffications().elements();f.hasMoreElements();) {
                    AcademicAreaCode aac = (AcademicAreaCode)f.nextElement();
                    Element aacEl = studentEl.addElement("classification");
                    if (aac.getArea()!=null)
                        aacEl.addAttribute("area", aac.getArea());
                    if (aac.getCode()!=null)
                        aacEl.addAttribute("code", aac.getCode());
                }
                for (Enumeration f=student.getMajors().elements();f.hasMoreElements();) {
                    AcademicAreaCode aac = (AcademicAreaCode)f.nextElement();
                    Element aacEl = studentEl.addElement("major");
                    if (aac.getArea()!=null)
                        aacEl.addAttribute("area", aac.getArea());
                    if (aac.getCode()!=null)
                        aacEl.addAttribute("code", aac.getCode());
                }
                for (Enumeration f=student.getMinors().elements();f.hasMoreElements();) {
                    AcademicAreaCode aac = (AcademicAreaCode)f.nextElement();
                    Element aacEl = studentEl.addElement("minor");
                    if (aac.getArea()!=null)
                        aacEl.addAttribute("area", aac.getArea());
                    if (aac.getCode()!=null)
                        aacEl.addAttribute("code", aac.getCode());
                }
            }
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
                        if (iShowNames && tl.getDatePatternId()!=null)
                            requestEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                        requestEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                        if (iShowNames)
                            requestEl.setText(tl.getLongName());
                    }
                    if (iSaveInitial && request.getInitialAssignment()!=null) {
                        requestEl.addElement("initial");
                    }
                    if (iSaveCurrent && request.getAssignment()!=null) {
                        requestEl.addElement("current");
                    }
                    if (iSaveBest && request.getBestAssignment()!=null) {
                        requestEl.addElement("best");
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
        
        if (iShowNames) {
            Progress.getInstance(getModel()).save(root);
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
