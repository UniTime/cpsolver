package org.cpsolver.studentsct;

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

import org.cpsolver.coursett.IdConvertor;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.constraint.LinkedSections;
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
import org.cpsolver.studentsct.model.RequestGroup;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Student.BackToBackPreference;
import org.cpsolver.studentsct.model.Student.ModalityPreference;
import org.cpsolver.studentsct.model.Student.StudentPriority;
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
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;


/**
 * Save student sectioning solution into an XML file.
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
 * Usage:
 * <pre><code>
 * new StudentSectioningXMLSaver(solver).save(new File("solution.xml")); 
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

public class StudentSectioningXMLSaver extends StudentSectioningSaver {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(StudentSectioningXMLSaver.class);
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
     * @throws Exception thrown when the save fails
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
    
    public Document saveDocument() {
        Document document = DocumentHelper.createDocument();
        document.addComment("Student Sectioning");
        
        populate(document);

        return document;
    }

    /**
     * Fill in all the data into the given document
     * @param document document to be populated
     */
    protected void populate(Document document) {
        if (iSaveCurrent || iSaveBest) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (getSolution() == null ? getModel().getExtendedInfo(getAssignment()) : getSolution().getExtendedInfo());
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
            saveRestrictions(offeringEl, offering);
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
        if (offering.isDummy())
            offeringEl.addAttribute("dummy", "true");
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
        if (iShowNames && course.getCredit() != null)
            courseEl.addAttribute("credit", course.getCredit());
        if (course.hasCreditValue())
            courseEl.addAttribute("credits", course.getCreditValue().toString());
        if (iShowNames && course.getType() != null)
            courseEl.addAttribute("type", course.getType());
        if (iShowNames && course.getTitle() != null)
            courseEl.addAttribute("title", course.getTitle());
        if (iShowNames && course.getNote() != null)
            courseEl.addAttribute("note", course.getNote());
        
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
        if (config.getInstructionalMethodId() != null) {
            Element imEl = configEl.addElement("instructional-method");
            imEl.addAttribute("id", getId("instructional-method", config.getInstructionalMethodId()));
            if (iShowNames && config.getInstructionalMethodName() != null)
                imEl.addAttribute("name", config.getInstructionalMethodName());
            if (iShowNames && config.getInstructionalMethodReference() != null)
                imEl.addAttribute("reference", config.getInstructionalMethodReference());
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
        if (iShowNames) {
            subpartEl.addAttribute("name", subpart.getName());
            if (subpart.getCredit() != null)
                subpartEl.addAttribute("credit", subpart.getCredit());
            if (subpart.hasCreditValue())
                subpartEl.addAttribute("credits", subpart.getCreditValue().toString());
        }
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
        if (section.isCancelled())
            sectionEl.addAttribute("cancelled", "true");
        if (!section.isEnabled())
            sectionEl.addAttribute("enabled", "false");
        if (section.isOnline())
            sectionEl.addAttribute("online", "true");
        if (section.isPast())
            sectionEl.addAttribute("past", "true");
        if (iShowNames && section.getNameByCourse() != null)
            for (Map.Entry<Long, String> entry: section.getNameByCourse().entrySet())
                sectionEl.addElement("cname").addAttribute("id", getId("course", entry.getKey())).setText(entry.getValue());
        if (section.getParent() != null)
            sectionEl.addAttribute("parent", getId("section", section.getParent().getId()));
        if (section.hasInstructors()) {
            for (Instructor instructor: section.getInstructors()) {
                Element instructorEl = sectionEl.addElement("instructor");
                instructorEl.addAttribute("id", getId("instructor", instructor.getId()));
                if (iShowNames && instructor.getName() != null)
                    instructorEl.addAttribute("name", instructor.getName());
                if (iShowNames && instructor.getExternalId() != null)
                    instructorEl.addAttribute("externalId", instructor.getExternalId());
                if (iShowNames && instructor.getEmail() != null)
                    instructorEl.addAttribute("email", instructor.getExternalId());
            }
        }
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
                    timeLocationEl.setText(tl.getLongName(true));
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
        if (reservation instanceof LearningCommunityReservation) {
            LearningCommunityReservation lc = (LearningCommunityReservation)reservation;
            reservationEl.addAttribute("type", "lc");
            for (Long studentId: lc.getStudentIds())
                reservationEl.addElement("student").addAttribute("id", getId("student", studentId));
            if (lc.getReservationLimit() >= 0.0)
                reservationEl.addAttribute("limit", String.valueOf(lc.getReservationLimit()));
            reservationEl.addAttribute("course", getId("course",lc.getCourse().getId()));
        } else if (reservation instanceof GroupReservation) {
            GroupReservation gr = (GroupReservation)reservation;
            reservationEl.addAttribute("type", "group");
            for (Long studentId: gr.getStudentIds())
                reservationEl.addElement("student").addAttribute("id", getId("student", studentId));
            if (gr.getReservationLimit() >= 0.0)
                reservationEl.addAttribute("limit", String.valueOf(gr.getReservationLimit()));
        } else if (reservation instanceof ReservationOverride) {
            reservationEl.addAttribute("type", "override");
            ReservationOverride o = (ReservationOverride)reservation;
            for (Long studentId: o.getStudentIds())
                reservationEl.addElement("student").addAttribute("id", getId("student", studentId));
        } else if (reservation instanceof IndividualReservation) {
            reservationEl.addAttribute("type", "individual");
            for (Long studentId: ((IndividualReservation)reservation).getStudentIds())
                reservationEl.addElement("student").addAttribute("id", getId("student", studentId));
        } else if (reservation instanceof CurriculumReservation) {
            reservationEl.addAttribute("type", (reservation instanceof CurriculumOverride ? "curriculum-override" : "curriculum"));
            CurriculumReservation cr = (CurriculumReservation)reservation;
            if (cr.getReservationLimit() >= 0.0)
                reservationEl.addAttribute("limit", String.valueOf(cr.getReservationLimit()));
            if (cr.getAcademicAreas().size() == 1)
                reservationEl.addAttribute("area", cr.getAcademicAreas().iterator().next());
            else {
                for (String area: cr.getAcademicAreas())
                    reservationEl.addElement("area").addAttribute("code", area);
            }
            for (String clasf: cr.getClassifications())
                reservationEl.addElement("classification").addAttribute("code", clasf);
            for (String major: cr.getMajors()) {
                Element majorEl = reservationEl.addElement("major").addAttribute("code", major);
                Set<String> concentrations = cr.getConcentrations(major);
                if (concentrations != null)
                    for (String conc: concentrations)
                        majorEl.addElement("concentration").addAttribute("code", conc);
            }
            for (String minor: cr.getMinors())
                reservationEl.addElement("minor").addAttribute("code", minor);
        } else if (reservation instanceof CourseReservation) {
            reservationEl.addAttribute("type", "course");
            CourseReservation cr = (CourseReservation)reservation;
            reservationEl.addAttribute("course", getId("course",cr.getCourse().getId()));
        } else if (reservation instanceof DummyReservation) {
            reservationEl.addAttribute("type", "dummy");
        } else if (reservation instanceof UniversalOverride) {
            reservationEl.addAttribute("type", "universal");
            UniversalOverride ur = (UniversalOverride)reservation;
            if (ur.getFilter() != null)
                reservationEl.addAttribute("filter", ur.getFilter());
            reservationEl.addAttribute("override", ur.isOverride() ? "true" : "false");
            if (ur.getReservationLimit() >= 0.0)
                reservationEl.addAttribute("limit", String.valueOf(ur.getReservationLimit()));
        }
        reservationEl.addAttribute("priority", String.valueOf(reservation.getPriority()));
        reservationEl.addAttribute("mustBeUsed", reservation.mustBeUsed() ? "true" : "false");
        reservationEl.addAttribute("allowOverlap", reservation.isAllowOverlap() ? "true" : "false");
        reservationEl.addAttribute("canAssignOverLimit", reservation.canAssignOverLimit() ? "true" : "false");
        reservationEl.addAttribute("allowDisabled", reservation.isAllowDisabled() ? "true" : "false");
        if (reservation.neverIncluded()) reservationEl.addAttribute("neverIncluded", "true");
        if (reservation.canBreakLinkedSections()) reservationEl.addAttribute("breakLinkedSections", "true");
        for (Config config: reservation.getConfigs())
            reservationEl.addElement("config").addAttribute("id", getId("config", config.getId()));
        for (Map.Entry<Subpart, Set<Section>> entry: reservation.getSections().entrySet()) {
            for (Section section: entry.getValue()) {
                reservationEl.addElement("section").addAttribute("id", getId("section", section.getId()));
            }
        }
    }
    
    /**
     * Save restrictions of the given offering
     * @param offeringEl offering element to be populated with restrictions
     * @param offering offering which restrictions are to be saved
     */
    protected void saveRestrictions(Element offeringEl, Offering offering) {
        if (!offering.getRestrictions().isEmpty()) {
            for (Restriction r: offering.getRestrictions()) {
                saveRestriction(offeringEl.addElement("restriction"), r);
            }
        }
    }
    
    /**
     * Save restriction
     * @param restrictionEl restriction element to be populated
     * @param restriction restriction to be saved
     */
    protected void saveRestriction(Element restrictionEl, Restriction restriction) {
        restrictionEl.addAttribute("id", getId("restriction", restriction.getId()));
        if (restriction instanceof IndividualRestriction) {
            restrictionEl.addAttribute("type", "individual");
            for (Long studentId: ((IndividualRestriction)restriction).getStudentIds())
                restrictionEl.addElement("student").addAttribute("id", getId("student", studentId));
        } else if (restriction instanceof CurriculumRestriction) {
            restrictionEl.addAttribute("type", "curriculum");
            CurriculumRestriction cr = (CurriculumRestriction)restriction;
            if (cr.getAcademicAreas().size() == 1)
                restrictionEl.addAttribute("area", cr.getAcademicAreas().iterator().next());
            else {
                for (String area: cr.getAcademicAreas())
                    restrictionEl.addElement("area").addAttribute("code", area);
            }
            for (String clasf: cr.getClassifications())
                restrictionEl.addElement("classification").addAttribute("code", clasf);
            for (String major: cr.getMajors()) {
                Element majorEl = restrictionEl.addElement("major").addAttribute("code", major);
                Set<String> concentrations = cr.getConcentrations(major);
                if (concentrations != null)
                    for (String conc: concentrations)
                        majorEl.addElement("concentration").addAttribute("code", conc);
            }
            for (String minor: cr.getMinors())
                restrictionEl.addElement("minor").addAttribute("code", minor);
        } else if (restriction instanceof CourseRestriction) {
            restrictionEl.addAttribute("type", "course");
            CourseRestriction cr = (CourseRestriction)restriction;
            restrictionEl.addAttribute("course", getId("course",cr.getCourse().getId()));
        }
        for (Config config: restriction.getConfigs())
            restrictionEl.addElement("config").addAttribute("id", getId("config", config.getId()));
        for (Map.Entry<Subpart, Set<Section>> entry: restriction.getSections().entrySet()) {
            for (Section section: entry.getValue()) {
                restrictionEl.addElement("section").addAttribute("id", getId("section", section.getId()));
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
        if (student.getPriority().ordinal() < StudentPriority.Normal.ordinal())
            studentEl.addAttribute("priority", student.getPriority().name());
        if (student.isNeedShortDistances())
            studentEl.addAttribute("shortDistances", "true");
        if (student.isAllowDisabled())
            studentEl.addAttribute("allowDisabled", "true");
        if (student.hasMinCredit())
            studentEl.addAttribute("minCredit", String.valueOf(student.getMinCredit()));
        if (student.hasMaxCredit())
            studentEl.addAttribute("maxCredit", String.valueOf(student.getMaxCredit()));
        if (student.getClassFirstDate() != null)
            studentEl.addAttribute("classFirstDate", String.valueOf(student.getClassFirstDate()));
        if (student.getClassLastDate() != null)
            studentEl.addAttribute("classLastDate", String.valueOf(student.getClassLastDate()));
        if (student.getModalityPreference() != null && student.getModalityPreference() != ModalityPreference.NO_PREFERENCE)
            studentEl.addAttribute("modality", student.getModalityPreference().name());
        if (student.getBackToBackPreference() != null && student.getBackToBackPreference() != BackToBackPreference.NO_PREFERENCE)
            studentEl.addAttribute("btb", student.getBackToBackPreference().name());
        if (iSaveStudentInfo) {
            for (AreaClassificationMajor acm : student.getAreaClassificationMajors()) {
                Element acmEl = studentEl.addElement("acm");
                if (acm.getArea() != null)
                    acmEl.addAttribute("area", acm.getArea());
                if (acm.getClassification() != null)
                    acmEl.addAttribute("classification", acm.getClassification());
                if (acm.getMajor() != null)
                    acmEl.addAttribute("major", acm.getMajor());
                if (acm.getConcentration() != null)
                    acmEl.addAttribute("concentration", acm.getConcentration());
                if (acm.getDegree() != null)
                    acmEl.addAttribute("degree", acm.getDegree());
                if (acm.getProgram() != null)
                    acmEl.addAttribute("program", acm.getProgram());
                if (acm.getAreaName() != null && iShowNames)
                    acmEl.addAttribute("areaName", acm.getAreaName());
                if (acm.getClassificationName() != null && iShowNames)
                    acmEl.addAttribute("classificationName", acm.getClassificationName());
                if (acm.getMajorName() != null && iShowNames)
                    acmEl.addAttribute("majorName", acm.getMajorName());
                if (acm.getConcentrationName() != null && iShowNames)
                    acmEl.addAttribute("concentrationName", acm.getConcentrationName());
                if (acm.getDegreeName() != null && iShowNames)
                    acmEl.addAttribute("degreeName", acm.getDegreeName());
                if (acm.getProgramName() != null && iShowNames)
                    acmEl.addAttribute("programName", acm.getProgramName());
                if (acm.getWeight() != 1.0)
                    acmEl.addAttribute("weight", String.valueOf(acm.getWeight()));
                if (acm.getCampus() != null)
                    acmEl.addAttribute("campus", acm.getCampus());
                if (acm.getCampusName() != null && iShowNames)
                    acmEl.addAttribute("campusName", acm.getCampusName());
            }
            for (AreaClassificationMajor acm : student.getAreaClassificationMinors()) {
                Element acmEl = studentEl.addElement("acm");
                if (acm.getArea() != null)
                    acmEl.addAttribute("area", acm.getArea());
                if (acm.getClassification() != null)
                    acmEl.addAttribute("classification", acm.getClassification());
                if (acm.getMajor() != null)
                    acmEl.addAttribute("minor", acm.getMajor());
                if (acm.getConcentration() != null)
                    acmEl.addAttribute("concentration", acm.getConcentration());
                if (acm.getDegree() != null)
                    acmEl.addAttribute("degree", acm.getDegree());
                if (acm.getProgram() != null)
                    acmEl.addAttribute("program", acm.getProgram());
                if (acm.getAreaName() != null && iShowNames)
                    acmEl.addAttribute("areaName", acm.getAreaName());
                if (acm.getClassificationName() != null && iShowNames)
                    acmEl.addAttribute("classificationName", acm.getClassificationName());
                if (acm.getMajorName() != null && iShowNames)
                    acmEl.addAttribute("minorName", acm.getMajorName());
                if (acm.getConcentrationName() != null && iShowNames)
                    acmEl.addAttribute("concentrationName", acm.getConcentrationName());
                if (acm.getDegreeName() != null && iShowNames)
                    acmEl.addAttribute("degreeName", acm.getDegreeName());
                if (acm.getProgramName() != null && iShowNames)
                    acmEl.addAttribute("programName", acm.getProgramName());
                if (acm.getWeight() != 1.0)
                    acmEl.addAttribute("weight", String.valueOf(acm.getWeight()));
                if (acm.getCampus() != null)
                    acmEl.addAttribute("campus", acm.getCampus());
                if (acm.getCampusName() != null && iShowNames)
                    acmEl.addAttribute("campusName", acm.getCampusName());
            }
            for (StudentGroup g : student.getGroups()) {
                Element grEl = studentEl.addElement("group");
                if (g.getType() != null && !g.getType().isEmpty())
                    grEl.addAttribute("type", g.getType());
                if (g.getReference() != null)
                    grEl.addAttribute("reference", g.getReference());
                if (g.getName() != null)
                    grEl.addAttribute("name", g.getName());
            }
            for (String acc: student.getAccommodations())
                studentEl.addElement("accommodation").addAttribute("reference", acc);
        }
        if (iShowNames && iSaveStudentInfo) {
            for (Instructor adv: student.getAdvisors()) {
                Element advEl = studentEl.addElement("advisor");
                if (adv.getExternalId() != null)
                    advEl.addAttribute("externalId", adv.getExternalId());
                if (adv.getName() != null)
                    advEl.addAttribute("name", adv.getName());
                if (adv.getEmail() != null)
                    advEl.addAttribute("email", adv.getEmail());
            }
        }
        for (Unavailability unavailability: student.getUnavailabilities()) {
            Element unavEl = studentEl.addElement("unavailability");
            unavEl.addAttribute("offering", getId("offering", unavailability.getSection().getSubpart().getConfig().getOffering().getId()));
            unavEl.addAttribute("section", getId("section", unavailability.getSection().getId()));
            unavEl.addAttribute("ta", unavailability.isTeachingAssignment() ? "true" : "false");
            if (unavailability.getCourseId() != null)
                unavEl.addAttribute("course", getId("course", unavailability.getCourseId()));
            if (unavailability.isAllowOverlap()) unavEl.addAttribute("allowOverlap", "true");
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
                requestEl.setText(tl.getLongName(true));
        }
        if (iSaveInitial && request.getInitialAssignment() != null) {
            requestEl.addElement("initial");
        }
        if (iSaveCurrent && getAssignment().getValue(request) != null) {
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
        if (request.getRequestPriority() != RequestPriority.Normal)
            requestEl.addAttribute("importance", request.getRequestPriority().name());
        if (request.getRequestPriority() == RequestPriority.Critical)
            requestEl.addAttribute("critical", "true");
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
        for (Choice choice : request.getRequiredChoices()) {
            Element choiceEl = requestEl.addElement("required");
            choiceEl.addAttribute("offering", getId("offering", choice.getOffering().getId()));
            choiceEl.setText(choice.getId());
        }
        if (iSaveInitial && request.getInitialAssignment() != null) {
            saveEnrollment(requestEl.addElement("initial"), request.getInitialAssignment());
        }
        if (iSaveCurrent && getAssignment().getValue(request) != null) {
            saveEnrollment(requestEl.addElement("current"), getAssignment().getValue(request));
        }
        if (iSaveBest && request.getBestAssignment() != null) {
            saveEnrollment(requestEl.addElement("best"), request.getBestAssignment());
        }
        if (request.isFixed())
            saveEnrollment(requestEl.addElement("fixed"), request.getFixedValue());
        for (RequestGroup g: request.getRequestGroups()) {
            Element groupEl = requestEl.addElement("group").addAttribute("id", getId("group", g.getId())).addAttribute("course", getId("course", g.getCourse().getId()));
            if (iShowNames)
                groupEl.addAttribute("name", g.getName());
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
        if (enrollment.getCourse() != null)
            assignmentEl.addAttribute("course", getId("course", enrollment.getCourse().getId()));
        for (Section section : enrollment.getSections()) {
            Element sectionEl = assignmentEl.addElement("section").addAttribute("id",
                    getId("section", section.getId()));
            if (iShowNames)
                sectionEl.setText(section.getName() + " " +
                        (section.getTime() == null ? " Arr Hrs" : " " + section.getTime().getLongName(true)) +
                        (section.getNrRooms() == 0 ? "" : " " + section.getPlacement().getRoomName(",")) +
                        (section.hasInstructors() ? " " + section.getInstructorNames(",") : ""));
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
            linkEl.addAttribute("mustBeUsed", linkedSections.isMustBeUsed() ? "true" : "false");
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
        if (getModel().getDistanceMetric() != null) {
            Map<Long, Map<Long, Integer>> travelTimes = getModel().getDistanceMetric().getTravelTimes();
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
