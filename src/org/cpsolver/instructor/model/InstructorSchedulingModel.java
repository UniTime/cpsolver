package org.cpsolver.instructor.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;
import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.constraints.GroupConstraint;
import org.cpsolver.instructor.constraints.GroupConstraint.Distribution;
import org.cpsolver.instructor.constraints.InstructorConstraint;
import org.cpsolver.instructor.constraints.SameInstructorConstraint;
import org.cpsolver.instructor.constraints.SameLinkConstraint;
import org.cpsolver.instructor.criteria.UnusedInstructorLoad;
import org.cpsolver.instructor.criteria.AttributePreferences;
import org.cpsolver.instructor.criteria.BackToBack;
import org.cpsolver.instructor.criteria.CoursePreferences;
import org.cpsolver.instructor.criteria.InstructorPreferences;
import org.cpsolver.instructor.criteria.SameInstructor;
import org.cpsolver.instructor.criteria.DifferentLecture;
import org.cpsolver.instructor.criteria.Distributions;
import org.cpsolver.instructor.criteria.OriginalInstructor;
import org.cpsolver.instructor.criteria.SameCommon;
import org.cpsolver.instructor.criteria.SameCourse;
import org.cpsolver.instructor.criteria.SameDays;
import org.cpsolver.instructor.criteria.SameLink;
import org.cpsolver.instructor.criteria.SameRoom;
import org.cpsolver.instructor.criteria.TeachingPreferences;
import org.cpsolver.instructor.criteria.TimeOverlaps;
import org.cpsolver.instructor.criteria.TimePreferences;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Instructor Scheduling Model. Variables are {@link org.cpsolver.instructor.model.TeachingRequest}, values are {@link org.cpsolver.instructor.model.TeachingAssignment}.
 * Each teaching request has a course (see {@link org.cpsolver.instructor.model.Course}) and one or more sections (see {link {@link org.cpsolver.instructor.model.Section}}).
 * Each assignment assigns one instructor (see {@link org.cpsolver.instructor.model.Instructor}) to a single teaching request.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class InstructorSchedulingModel extends Model<TeachingRequest.Variable, TeachingAssignment> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(InstructorSchedulingModel.class);
    private DataProperties iProperties;
    private Set<Attribute.Type> iTypes = new HashSet<Attribute.Type>();
    private List<Instructor> iInstructors = new ArrayList<Instructor>();
    private List<TeachingRequest> iRequests = new ArrayList<TeachingRequest>();

    /**
     * Constructor
     * @param properties data properties
     */
    public InstructorSchedulingModel(DataProperties properties) {
        super();
        iProperties = properties;
        addCriterion(new AttributePreferences());
        addCriterion(new InstructorPreferences());
        addCriterion(new TeachingPreferences());
        addCriterion(new TimePreferences());
        addCriterion(new CoursePreferences());
        addCriterion(new BackToBack());
        addCriterion(new SameInstructor());
        addCriterion(new TimeOverlaps());
        addCriterion(new DifferentLecture());
        addCriterion(new SameLink());
        addCriterion(new OriginalInstructor());
        addCriterion(new UnusedInstructorLoad());
        addCriterion(new SameCourse());
        addCriterion(new SameCommon());
        addCriterion(new SameDays());
        addCriterion(new SameRoom());
        addCriterion(new Distributions());
        addGlobalConstraint(new InstructorConstraint());
        addGlobalConstraint(new GroupConstraint());
    }
    
    /**
     * Return solver configuration
     * @return data properties given in the constructor
     */
    public DataProperties getProperties() {
        return iProperties;
    }
    
    /**
     * Add instructor
     * @param instructor an instructor
     */
    public void addInstructor(Instructor instructor) {
        instructor.setModel(this);
        iInstructors.add(instructor);
        for (Attribute attribute: instructor.getAttributes())
            addAttributeType(attribute.getType());
    }
    
    /**
     * All instructors
     * @return all instructors in the model
     */
    public List<Instructor> getInstructors() {
        return iInstructors;
    }

    /**
     * Add teaching request and the related variables
     * @param request teaching request
     */
    public void addRequest(TeachingRequest request) {
        iRequests.add(request);
        for (TeachingRequest.Variable variable: request.getVariables())
            addVariable(variable);
    }
    
    /**
     * All teaching requests
     * @return all teaching requests in the model
     */
    public List<TeachingRequest> getRequests() {
        return iRequests;
    }

    
    /**
     * Return registered attribute types
     * @return attribute types in the problem
     */
    public Set<Attribute.Type> getAttributeTypes() { return iTypes; }
    
    /**
     * Register an attribute type
     * @param type attribute type
     */
    public void addAttributeType(Attribute.Type type) { iTypes.add(type); }

    @Override
    public Map<String, String> getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        Map<String, String> info = super.getInfo(assignment);

        double totalLoad = 0.0;
        double assignedLoad = 0.0;
        for (TeachingRequest.Variable clazz : variables()) {
            totalLoad += clazz.getRequest().getLoad();
            if (assignment.getValue(clazz) != null)
                assignedLoad += clazz.getRequest().getLoad();
        }
        info.put("Assigned Load", getPerc(assignedLoad, totalLoad, 0) + "% (" + sDoubleFormat.format(assignedLoad) + " / " + sDoubleFormat.format(totalLoad) + ")");

        return info;
    }

    @Override
    public double getTotalValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        double ret = 0;
        for (Criterion<TeachingRequest.Variable, TeachingAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment);
        return ret;
    }

    @Override
    public double getTotalValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double ret = 0;
        for (Criterion<TeachingRequest.Variable, TeachingAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment, variables);
        return ret;
    }
    
    /**
     * Store the problem (together with its solution) in an XML format
     * @param assignment current assignment
     * @return XML document with the problem
     */
    public Document save(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        DecimalFormat sDF7 = new DecimalFormat("0000000");
        boolean saveInitial = getProperties().getPropertyBoolean("Xml.SaveInitial", false);
        boolean saveBest = getProperties().getPropertyBoolean("Xml.SaveBest", false);
        boolean saveSolution = getProperties().getPropertyBoolean("Xml.SaveSolution", true);
        Document document = DocumentHelper.createDocument();
        if (assignment != null && assignment.nrAssignedVariables() > 0) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Map<String, String> solutionInfo = (getProperties().getPropertyBoolean("Xml.ExtendedInfo", true) ? getExtendedInfo(assignment) : getInfo(assignment));
            for (String key : new TreeSet<String>(solutionInfo.keySet())) {
                String value = solutionInfo.get(key);
                comments.append("    " + key + ": " + value + "\n");
            }
            document.addComment(comments.toString());
        }
        Element root = document.addElement("instructor-schedule");
        root.addAttribute("version", "1.0");
        root.addAttribute("created", String.valueOf(new Date()));
        Element typesEl = root.addElement("attributes");
        for (Attribute.Type type: getAttributeTypes()) {
            Element typeEl = typesEl.addElement("type");
            if (type.getTypeId() != null)
                typeEl.addAttribute("id", String.valueOf(type.getTypeId()));
            typeEl.addAttribute("name", type.getTypeName());
            typeEl.addAttribute("conjunctive", type.isConjunctive() ? "true" : "false");
            typeEl.addAttribute("required", type.isRequired() ? "true": "false");
            Set<Attribute> attributes = new HashSet<Attribute>();
            for (TeachingRequest request: getRequests()) {
                for (Preference<Attribute> pref: request.getAttributePreferences()) {
                    Attribute attribute = pref.getTarget();
                    if (type.equals(attribute.getType()) && attributes.add(attribute)) {
                        Element attributeEl = typeEl.addElement("attribute");
                        if (attribute.getAttributeId() != null)
                            attributeEl.addAttribute("id", String.valueOf(attribute.getAttributeId()));
                        attributeEl.addAttribute("name", attribute.getAttributeName());
                        if (attribute.getParentAttribute() != null && attribute.getParentAttribute().getAttributeId() != null)
                            attributeEl.addAttribute("parent", String.valueOf(attribute.getParentAttribute().getAttributeId()));
                    }
                }
                for (Instructor instructor: getInstructors()) {
                    for (Attribute attribute: instructor.getAttributes()) {
                        if (type.equals(attribute.getType()) && attributes.add(attribute)) {
                            Element attributeEl = typeEl.addElement("attribute");
                            if (attribute.getAttributeId() != null)
                                attributeEl.addAttribute("id", String.valueOf(attribute.getAttributeId()));
                            attributeEl.addAttribute("name", attribute.getAttributeName());
                            if (attribute.getParentAttribute() != null && attribute.getParentAttribute().getAttributeId() != null)
                                attributeEl.addAttribute("parent", String.valueOf(attribute.getParentAttribute().getAttributeId()));
                        }
                    }
                }
            }
        }
        Element requestsEl = root.addElement("teaching-requests");
        for (TeachingRequest request: getRequests()) {
            Element requestEl = requestsEl.addElement("request");
            requestEl.addAttribute("id", String.valueOf(request.getRequestId()));
            if (request.getNrInstructors() != 1)
                requestEl.addAttribute("nrInstructors", String.valueOf(request.getNrInstructors()));
            Course course = request.getCourse();
            Element courseEl = requestEl.addElement("course");
            if (course.getCourseId() != null)
                courseEl.addAttribute("id", String.valueOf(course.getCourseId()));
            if (course.getCourseName() != null)
                courseEl.addAttribute("name", String.valueOf(course.getCourseName()));
            for (Section section: request.getSections()) {
                Element sectionEl = requestEl.addElement("section");
                sectionEl.addAttribute("id", String.valueOf(section.getSectionId()));
                if (section.getExternalId() != null && !section.getExternalId().isEmpty()) sectionEl.addAttribute("externalId", section.getExternalId());
                if (section.getSectionType() != null && !section.getSectionType().isEmpty()) sectionEl.addAttribute("type", section.getSectionType());
                if (section.getSectionName() != null && !section.getSectionName().isEmpty()) sectionEl.addAttribute("name", section.getSectionName());
                if (section.hasTime()) {
                    TimeLocation tl = section.getTime();
                    Element timeEl = sectionEl.addElement("time");
                    timeEl.addAttribute("days", sDF7.format(Long.parseLong(Integer.toBinaryString(tl.getDayCode()))));
                    timeEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                    timeEl.addAttribute("length", String.valueOf(tl.getLength()));
                    if (tl.getBreakTime() != 0)
                        timeEl.addAttribute("breakTime", String.valueOf(tl.getBreakTime()));
                    if (tl.getTimePatternId() != null)
                        timeEl.addAttribute("pattern", tl.getTimePatternId().toString());
                    if (tl.getDatePatternId() != null)
                        timeEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                    if (tl.getDatePatternName() != null && !tl.getDatePatternName().isEmpty())
                        timeEl.addAttribute("datePatternName", tl.getDatePatternName());
                    if (tl.getWeekCode() != null)
                        timeEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                    timeEl.setText(tl.getLongName(false));
                }
                if (section.hasRoom()) sectionEl.addAttribute("room", section.getRoom());
                if (section.isAllowOverlap()) sectionEl.addAttribute("canOverlap", "true");
                if (section.isCommon()) sectionEl.addAttribute("common", "true");
            }
            requestEl.addAttribute("load", sDoubleFormat.format(request.getLoad()));
            requestEl.addAttribute("sameCourse", Constants.preferenceLevel2preference(request.getSameCoursePreference()));
            requestEl.addAttribute("sameCommon", Constants.preferenceLevel2preference(request.getSameCommonPreference()));
            for (Preference<Attribute> pref: request.getAttributePreferences()) {
                Element attributeEl = requestEl.addElement("attribute");
                if (pref.getTarget().getAttributeId() != null)
                    attributeEl.addAttribute("id", String.valueOf(pref.getTarget().getAttributeId()));
                attributeEl.addAttribute("name", pref.getTarget().getAttributeName());
                attributeEl.addAttribute("type", pref.getTarget().getType().getTypeName());
                attributeEl.addAttribute("preference", (pref.isRequired() ? "R" : pref.isProhibited() ? "P" : String.valueOf(pref.getPreference())));
                if (pref.getTarget().getParentAttribute() != null && pref.getTarget().getParentAttribute().getAttributeId() != null)
                    attributeEl.addAttribute("parent", String.valueOf(pref.getTarget().getParentAttribute().getAttributeId()));
            }
            for (Preference<Instructor> pref: request.getInstructorPreferences()) {
                Element instructorEl = requestEl.addElement("instructor");
                instructorEl.addAttribute("id", String.valueOf(pref.getTarget().getInstructorId()));
                if (pref.getTarget().hasExternalId())
                    instructorEl.addAttribute("externalId", pref.getTarget().getExternalId());
                if (pref.getTarget().hasName())
                    instructorEl.addAttribute("name", pref.getTarget().getName());
                instructorEl.addAttribute("preference", (pref.isRequired() ? "R" : pref.isProhibited() ? "P" : String.valueOf(pref.getPreference())));
            }
            if (saveBest)
                for (TeachingRequest.Variable variable: request.getVariables()) {
                    if (variable.getBestAssignment() != null) {
                        Instructor instructor = variable.getBestAssignment().getInstructor();
                        Element instructorEl = requestEl.addElement("best-instructor");
                        instructorEl.addAttribute("id", String.valueOf(instructor.getInstructorId()));
                        if (request.getNrInstructors() != 1)
                            instructorEl.addAttribute("index", String.valueOf(variable.getInstructorIndex()));
                        if (instructor.hasExternalId())
                            instructorEl.addAttribute("externalId", instructor.getExternalId());
                        if (instructor.hasName())
                            instructorEl.addAttribute("name", instructor.getName());
                    }                    
                }
            if (saveInitial)
                for (TeachingRequest.Variable variable: request.getVariables()) {
                    if (variable.getInitialAssignment() != null) {
                        Instructor instructor = variable.getInitialAssignment().getInstructor();
                        Element instructorEl = requestEl.addElement("initial-instructor");
                        instructorEl.addAttribute("id", String.valueOf(instructor.getInstructorId()));
                        if (request.getNrInstructors() != 1)
                            instructorEl.addAttribute("index", String.valueOf(variable.getInstructorIndex()));
                        if (instructor.hasExternalId())
                            instructorEl.addAttribute("externalId", instructor.getExternalId());
                        if (instructor.hasName())
                            instructorEl.addAttribute("name", instructor.getName());
                    }
                }
            if (saveSolution)
                for (TeachingRequest.Variable variable: request.getVariables()) {
                    TeachingAssignment ta = assignment.getValue(variable);
                    if (ta != null) {
                        Instructor instructor = ta.getInstructor();
                        Element instructorEl = requestEl.addElement("assigned-instructor");
                        instructorEl.addAttribute("id", String.valueOf(instructor.getInstructorId()));
                        if (request.getNrInstructors() != 1)
                            instructorEl.addAttribute("index", String.valueOf(variable.getInstructorIndex()));
                        if (instructor.hasExternalId())
                            instructorEl.addAttribute("externalId", instructor.getExternalId());
                        if (instructor.hasName())
                            instructorEl.addAttribute("name", instructor.getName());
                    }
                }
        }
        Element instructorsEl = root.addElement("instructors");
        for (Instructor instructor: getInstructors()) {
            Element instructorEl = instructorsEl.addElement("instructor");
            instructorEl.addAttribute("id", String.valueOf(instructor.getInstructorId()));
            if (instructor.hasExternalId())
                instructorEl.addAttribute("externalId", instructor.getExternalId());
            if (instructor.hasName())
                instructorEl.addAttribute("name", instructor.getName());
            if (instructor.getPreference() != 0)
                instructorEl.addAttribute("preference", String.valueOf(instructor.getPreference()));
            if (instructor.getBackToBackPreference() != 0)
                instructorEl.addAttribute("btb", String.valueOf(instructor.getBackToBackPreference()));
            if (instructor.getSameDaysPreference() != 0)
                instructorEl.addAttribute("same-days", String.valueOf(instructor.getSameDaysPreference()));
            if (instructor.getSameRoomPreference() != 0)
                instructorEl.addAttribute("same-room", String.valueOf(instructor.getSameRoomPreference()));
            for (Attribute attribute: instructor.getAttributes()) {
                Element attributeEl = instructorEl.addElement("attribute");
                if (attribute.getAttributeId() != null)
                    attributeEl.addAttribute("id", String.valueOf(attribute.getAttributeId()));
                attributeEl.addAttribute("name", attribute.getAttributeName());
                attributeEl.addAttribute("type", attribute.getType().getTypeName());
                if (attribute.getParentAttribute() != null && attribute.getParentAttribute().getAttributeId() != null)
                    attributeEl.addAttribute("parent", String.valueOf(attribute.getParentAttribute().getAttributeId()));
            }
            instructorEl.addAttribute("maxLoad", sDoubleFormat.format(instructor.getMaxLoad()));
            for (Distribution d: instructor.getDistributions()) {
                Element dEl = instructorEl.addElement("distribution");
                dEl.addAttribute("type", d.getType().reference());
                dEl.addAttribute("preference", d.getPreference());
                dEl.addAttribute("name", d.getType().getName());
            }
            for (Preference<TimeLocation> tp: instructor.getTimePreferences()) {
                
                Element timeEl = instructorEl.addElement("time");
                TimeLocation tl = tp.getTarget();
                timeEl.addAttribute("days", sDF7.format(Long.parseLong(Integer.toBinaryString(tl.getDayCode()))));
                timeEl.addAttribute("start", String.valueOf(tl.getStartSlot()));
                timeEl.addAttribute("length", String.valueOf(tl.getLength()));
                if (tl.getBreakTime() != 0)
                    timeEl.addAttribute("breakTime", String.valueOf(tl.getBreakTime()));
                if (tl.getTimePatternId() != null)
                    timeEl.addAttribute("pattern", tl.getTimePatternId().toString());
                if (tl.getDatePatternId() != null)
                    timeEl.addAttribute("datePattern", tl.getDatePatternId().toString());
                if (tl.getDatePatternName() != null && !tl.getDatePatternName().isEmpty())
                    timeEl.addAttribute("datePatternName", tl.getDatePatternName());
                if (tl.getWeekCode() != null)
                    timeEl.addAttribute("dates", bitset2string(tl.getWeekCode()));
                timeEl.addAttribute("preference", tp.isProhibited() ? "P" : tp.isRequired() ? "R" : String.valueOf(tp.getPreference()));
                if (tp.getTarget() instanceof EnrolledClass) {
                    Element classEl = timeEl.addElement("section");
                    Element courseEl = null;
                    EnrolledClass ec = (EnrolledClass)tp.getTarget();
                    if (ec.getCourseId() != null || ec.getCourse() != null) {
                        courseEl = timeEl.addElement("course");
                        if (ec.getCourseId() != null) courseEl.addAttribute("id", String.valueOf(ec.getCourseId()));
                        if (ec.getCourse() != null) courseEl.addAttribute("name", ec.getCourse());
                    }
                    if (ec.getClassId() != null) classEl.addAttribute("id", String.valueOf(ec.getClassId()));
                    if (ec.getType() != null) classEl.addAttribute("type", ec.getType());
                    if (ec.getSection() != null) classEl.addAttribute("name", ec.getSection());
                    if (ec.getExternalId() != null) classEl.addAttribute("externalId", ec.getExternalId());
                    if (ec.getRoom() != null) classEl.addAttribute("room", ec.getRoom());
                    classEl.addAttribute("role", ec.isInstructor() ? "instructor" : "student");
                } else {
                    timeEl.setText(tl.getLongName(false));
                }
            }
            for (Preference<Course> cp: instructor.getCoursePreferences()) {
                Element courseEl = instructorEl.addElement("course");
                Course course = cp.getTarget();
                if (course.getCourseId() != null)
                    courseEl.addAttribute("id", String.valueOf(course.getCourseId()));
                if (course.getCourseName() != null)
                    courseEl.addAttribute("name", String.valueOf(course.getCourseName()));
                courseEl.addAttribute("preference", cp.isProhibited() ? "P" : cp.isRequired() ? "R" : String.valueOf(cp.getPreference()));
            }
        }
        Element constraintsEl = root.addElement("constraints");
        for (Constraint<TeachingRequest.Variable, TeachingAssignment> c: constraints()) {
            if (c instanceof SameInstructorConstraint) {
                SameInstructorConstraint si = (SameInstructorConstraint) c;
                Element sameInstEl = constraintsEl.addElement("same-instructor");
                if (si.getConstraintId() != null)
                    sameInstEl.addAttribute("id", String.valueOf(si.getConstraintId()));
                if (si.getName() != null)
                    sameInstEl.addAttribute("name", si.getName());
                sameInstEl.addAttribute("preference", Constants.preferenceLevel2preference(si.getPreference()));
                for (TeachingRequest.Variable request: c.variables()) {
                    Element assignmentEl = sameInstEl.addElement("request");
                    assignmentEl.addAttribute("id", String.valueOf(request.getRequest().getRequestId()));
                    if (request.getRequest().getNrInstructors() != 1)
                        assignmentEl.addAttribute("index", String.valueOf(request.getInstructorIndex()));
                }
            } else if (c instanceof SameLinkConstraint) {
                SameLinkConstraint si = (SameLinkConstraint) c;
                Element sameInstEl = constraintsEl.addElement("same-link");
                if (si.getConstraintId() != null)
                    sameInstEl.addAttribute("id", String.valueOf(si.getConstraintId()));
                if (si.getName() != null)
                    sameInstEl.addAttribute("name", si.getName());
                sameInstEl.addAttribute("preference", Constants.preferenceLevel2preference(si.getPreference()));
                for (TeachingRequest.Variable request: c.variables()) {
                    Element assignmentEl = sameInstEl.addElement("request");
                    assignmentEl.addAttribute("id", String.valueOf(request.getRequest().getRequestId()));
                    if (request.getRequest().getNrInstructors() != 1)
                        assignmentEl.addAttribute("index", String.valueOf(request.getInstructorIndex()));
                }
            }
        }
        return document;
    }
    
    /**
     * Load the problem (and its solution) from an XML format
     * @param document XML document
     * @param assignment current assignment
     * @return true, if the problem was successfully loaded in
     */
    public boolean load(Document document, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        boolean loadInitial = getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        boolean loadBest = getProperties().getPropertyBoolean("Xml.LoadBest", true);
        boolean loadSolution = getProperties().getPropertyBoolean("Xml.LoadSolution", true);
        String defaultBtb = getProperties().getProperty("Defaults.BackToBack", "0");
        String defaultSameDays = getProperties().getProperty("Defaults.SameDays", "0");
        String defaultSameRoom = getProperties().getProperty("Defaults.SameRoom", "0");
        String defaultConjunctive = getProperties().getProperty("Defaults.Conjunctive", "false");
        String defaultRequired = getProperties().getProperty("Defaults.Required", "false");
        String defaultSameCourse = getProperties().getProperty("Defaults.SameCourse", "R");
        String defaultSameCommon = getProperties().getProperty("Defaults.SameCommon", "R");
        Element root = document.getRootElement();
        if (!"instructor-schedule".equals(root.getName()))
            return false;
        Map<String, Attribute.Type> types = new HashMap<String, Attribute.Type>();
        Map<Long, Attribute> attributes = new HashMap<Long, Attribute>();
        Map<Long, Long> parents = new HashMap<Long, Long>();
        if (root.element("attributes") != null) {
            for (Iterator<?> i = root.element("attributes").elementIterator("type"); i.hasNext();) {
                Element typeEl = (Element) i.next();
                Attribute.Type type = new Attribute.Type(
                        Long.parseLong(typeEl.attributeValue("id")),
                        typeEl.attributeValue("name"),
                        "true".equalsIgnoreCase(typeEl.attributeValue("conjunctive", defaultConjunctive)),
                        "true".equalsIgnoreCase(typeEl.attributeValue("required", defaultRequired)));
                addAttributeType(type);
                if (type.getTypeName() != null)
                    types.put(type.getTypeName(), type);
                for (Iterator<?> j = typeEl.elementIterator("attribute"); j.hasNext();) {
                    Element attributeEl = (Element) j.next();
                    Attribute attribute = new Attribute(
                            Long.parseLong(attributeEl.attributeValue("id")),
                            attributeEl.attributeValue("name"),
                            type);
                    attributes.put(attribute.getAttributeId(), attribute);
                    if (attributeEl.attributeValue("parent") != null)
                        parents.put(attribute.getAttributeId(), Long.parseLong(attributeEl.attributeValue("parent")));
                }
            }
        }
        Map<Long, Course> courses = new HashMap<Long, Course>();
        if (root.element("courses") != null) {
            for (Iterator<?> i = root.element("courses").elementIterator("course"); i.hasNext();) {
                Element courseEl = (Element) i.next();
                Course course = new Course(
                        Long.parseLong(courseEl.attributeValue("id")),
                        courseEl.attributeValue("name"));
                courses.put(course.getCourseId(), course);
            }
        }
        Map<Long, Instructor> instructors = new HashMap<Long, Instructor>();
        for (Iterator<?> i = root.element("instructors").elementIterator("instructor"); i.hasNext();) {
            Element instructorEl = (Element) i.next();
            Instructor instructor = new Instructor(
                    Long.parseLong(instructorEl.attributeValue("id")),
                    instructorEl.attributeValue("externalId"),
                    instructorEl.attributeValue("name"),
                    string2preference(instructorEl.attributeValue("preference")),
                    Float.parseFloat(instructorEl.attributeValue("maxLoad", "0")));
            instructor.setBackToBackPreference(Integer.valueOf(instructorEl.attributeValue("btb", defaultBtb)));
            instructor.setSameDaysPreference(Integer.valueOf(instructorEl.attributeValue("same-days", defaultSameDays)));
            instructor.setSameRoomPreference(Integer.valueOf(instructorEl.attributeValue("same-room", defaultSameRoom)));
            for (Iterator<?> j = instructorEl.elementIterator("attribute"); j.hasNext();) {
                Element f = (Element) j.next();
                Long attributeId = Long.valueOf(f.attributeValue("id"));
                Attribute attribute = attributes.get(attributeId);
                if (attribute == null) {
                    Attribute.Type type = types.get(f.attributeValue("type"));
                    if (type == null) {
                        type = new Attribute.Type(types.size(), f.attributeValue("type"),
                                "true".equalsIgnoreCase(f.attributeValue("conjunctive", defaultConjunctive)),
                                "true".equalsIgnoreCase(f.attributeValue("required", defaultRequired)));
                        types.put(type.getTypeName(), type);
                    }
                    attribute = new Attribute(attributeId, f.attributeValue("name"), type);
                    attributes.put(attributeId, attribute);
                    if (f.attributeValue("parent") != null)
                        parents.put(attribute.getAttributeId(), Long.parseLong(f.attributeValue("parent")));
                }
                instructor.addAttribute(attribute);
            }
            for (Iterator<?> j = instructorEl.elementIterator("distribution"); j.hasNext();) {
                Element dEl = (Element) j.next();
                instructor.addDistribution(dEl.attributeValue("type"), dEl.attributeValue("preference", "0"), dEl.attributeValue("name"));
            }
            for (Iterator<?> j = instructorEl.elementIterator("time"); j.hasNext();) {
                Element f = (Element) j.next();
                Element classEl = f.element("section");
                Element courseEl = f.element("course");
                TimeLocation time = null;
                if (classEl != null) {
                    time = new EnrolledClass(
                            courseEl == null || courseEl.attributeValue("id") == null ? null : Long.valueOf(courseEl.attributeValue("id")),
                            classEl.attributeValue("id") == null ? null : Long.valueOf(classEl.attributeValue("id")),
                            courseEl == null ? null : courseEl.attributeValue("name"),
                            classEl.attributeValue("type"),
                            classEl.attributeValue("name"),
                            classEl.attributeValue("externalId"),
                            Integer.parseInt(f.attributeValue("days"), 2),
                            Integer.parseInt(f.attributeValue("start")),
                            Integer.parseInt(f.attributeValue("length")),
                            f.attributeValue("datePattern") == null ? null : Long.valueOf(f.attributeValue("datePattern")),
                            f.attributeValue("datePatternName", ""),
                            createBitSet(f.attributeValue("dates")),
                            Integer.parseInt(f.attributeValue("breakTime", "0")),
                            classEl.attributeValue("room"),
                            "instructor".equalsIgnoreCase(classEl.attributeValue("role", "instructor")));
                } else {
                    time = new TimeLocation(
                            Integer.parseInt(f.attributeValue("days"), 2),
                            Integer.parseInt(f.attributeValue("start")),
                            Integer.parseInt(f.attributeValue("length")), 0, 0,
                            f.attributeValue("datePattern") == null ? null : Long.valueOf(f.attributeValue("datePattern")),
                            f.attributeValue("datePatternName", ""),
                            createBitSet(f.attributeValue("dates")),
                            Integer.parseInt(f.attributeValue("breakTime", "0")));
                }
                if (f.attributeValue("pattern") != null)
                    time.setTimePatternId(Long.valueOf(f.attributeValue("pattern")));
                instructor.addTimePreference(new Preference<TimeLocation>(time, string2preference(f.attributeValue("preference"))));
            }
            for (Iterator<?> j = instructorEl.elementIterator("course"); j.hasNext();) {
                Element f = (Element) j.next();
                instructor.addCoursePreference(new Preference<Course>(new Course(Long.parseLong(f.attributeValue("id")), f.attributeValue("name")), string2preference(f.attributeValue("preference"))));
            }
            addInstructor(instructor);
            instructors.put(instructor.getInstructorId(), instructor);
        }
        Map<Long, TeachingRequest> requests = new HashMap<Long, TeachingRequest>();
        Map<TeachingRequest, Map<Integer, Instructor>> current = new HashMap<TeachingRequest, Map<Integer, Instructor>>();
        Map<TeachingRequest, Map<Integer, Instructor>> best = new HashMap<TeachingRequest, Map<Integer, Instructor>>();
        Map<TeachingRequest, Map<Integer, Instructor>> initial = new HashMap<TeachingRequest, Map<Integer, Instructor>>();
        for (Iterator<?> i = root.element("teaching-requests").elementIterator("request"); i.hasNext();) {
            Element requestEl = (Element) i.next();
            Element courseEl = requestEl.element("course");
            Course course = null;
            if (courseEl != null) {
                Long courseId = Long.valueOf(courseEl.attributeValue("id"));
                course = courses.get(courseId);
                if (course == null) {
                    course = new Course(courseId, courseEl.attributeValue("name"));
                }
            } else {
                course = courses.get(Long.valueOf(requestEl.attributeValue("course")));
            }
            List<Section> sections = new ArrayList<Section>();
            for (Iterator<?> j = requestEl.elementIterator("section"); j.hasNext();) {
                Element f = (Element) j.next();
                TimeLocation time = null;
                Element timeEl = f.element("time");
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
                Section section = new Section(
                        Long.valueOf(f.attributeValue("id")),
                        f.attributeValue("externalId"),
                        f.attributeValue("type"),
                        f.attributeValue("name"),
                        time,
                        f.attributeValue("room"),
                        "true".equalsIgnoreCase(f.attributeValue("canOverlap", "false")),
                        "true".equalsIgnoreCase(f.attributeValue("common", "false")));
                sections.add(section);
            }
            TeachingRequest request = new TeachingRequest(
                    Long.parseLong(requestEl.attributeValue("id")),
                    Integer.parseInt(requestEl.attributeValue("nrInstructors", "1")),
                    course,
                    Float.valueOf(requestEl.attributeValue("load", "0")),
                    sections,
                    Constants.preference2preferenceLevel(requestEl.attributeValue("sameCourse", defaultSameCourse)),
                    Constants.preference2preferenceLevel(requestEl.attributeValue("sameCommon", defaultSameCommon)));
            requests.put(request.getRequestId(), request);
            for (Iterator<?> j = requestEl.elementIterator("attribute"); j.hasNext();) {
                Element f = (Element) j.next();
                Long attributeId = Long.valueOf(f.attributeValue("id"));
                Attribute attribute = attributes.get(attributeId);
                if (attribute == null) {
                    Attribute.Type type = types.get(f.attributeValue("type"));
                    if (type == null) {
                        type = new Attribute.Type(types.size(), f.attributeValue("type"),
                                "true".equalsIgnoreCase(f.attributeValue("conjunctive", defaultConjunctive)),
                                "true".equalsIgnoreCase(f.attributeValue("required", defaultRequired)));
                        types.put(type.getTypeName(), type);
                    }
                    attribute = new Attribute(attributeId, f.attributeValue("name"), type);
                    attributes.put(attributeId, attribute);
                    if (f.attributeValue("parent") != null)
                        parents.put(attribute.getAttributeId(), Long.parseLong(f.attributeValue("parent")));
                }
                request.addAttributePreference(new Preference<Attribute>(attribute, string2preference(f.attributeValue("preference"))));
            }
            for (Iterator<?> j = requestEl.elementIterator("instructor"); j.hasNext();) {
                Element f = (Element) j.next();
                Long instructorId = Long.valueOf(f.attributeValue("id"));
                Instructor instructor = instructors.get(instructorId);
                if (instructor != null)
                    request.addInstructorPreference(new Preference<Instructor>(instructor, string2preference(f.attributeValue("preference"))));
            }
            if (loadBest) {
                for (Iterator<?> j = requestEl.elementIterator("best-instructor"); j.hasNext();) {
                    Element f = (Element) j.next();
                    Map<Integer, Instructor> idx2inst = best.get(request);
                    if (idx2inst == null) {
                        idx2inst = new HashMap<Integer, Instructor>();
                        best.put(request, idx2inst);
                    }
                    int index = 1 + Integer.parseInt(f.attributeValue("index", String.valueOf(idx2inst.size())));
                    Instructor instructor = instructors.get(Long.valueOf(f.attributeValue("id")));
                    if (instructor != null)
                        idx2inst.put(index, instructor);
                }
            }
            if (loadInitial) {
                for (Iterator<?> j = requestEl.elementIterator("initial-instructor"); j.hasNext();) {
                    Element f = (Element) j.next();
                    Map<Integer, Instructor> idx2inst = initial.get(request);
                    if (idx2inst == null) {
                        idx2inst = new HashMap<Integer, Instructor>();
                        initial.put(request, idx2inst);
                    }
                    int index = 1 + Integer.parseInt(f.attributeValue("index", String.valueOf(idx2inst.size())));
                    Instructor instructor = instructors.get(Long.valueOf(f.attributeValue("id")));
                    if (instructor != null)
                        idx2inst.put(index, instructor);
                }
            }
            if (loadSolution) {
                for (Iterator<?> j = requestEl.elementIterator("assigned-instructor"); j.hasNext();) {
                    Element f = (Element) j.next();
                    Map<Integer, Instructor> idx2inst = current.get(request);
                    if (idx2inst == null) {
                        idx2inst = new HashMap<Integer, Instructor>();
                        current.put(request, idx2inst);
                    }
                    int index = Integer.parseInt(f.attributeValue("index", String.valueOf(idx2inst.size())));
                    Instructor instructor = instructors.get(Long.valueOf(f.attributeValue("id")));
                    if (instructor != null)
                        idx2inst.put(index, instructor);
                }
            }
            addRequest(request);
        }
        if (root.element("constraints") != null) {
            for (Iterator<?> i = root.element("constraints").elementIterator(); i.hasNext();) {
                Element constraintEl = (Element) i.next();
                Constraint<TeachingRequest.Variable, TeachingAssignment> constraint = null;
                if ("same-link".equals(constraintEl.getName())) {
                    constraint = new SameLinkConstraint(
                            (constraintEl.attributeValue("id") == null ? null : Long.valueOf(constraintEl.attributeValue("id"))),
                            constraintEl.attributeValue("name"),
                            constraintEl.attributeValue("preference"));
                } else if ("same-instructor".equals(constraintEl.getName())) {
                    constraint = new SameInstructorConstraint(
                            (constraintEl.attributeValue("id") == null ? null : Long.valueOf(constraintEl.attributeValue("id"))),
                            constraintEl.attributeValue("name"),
                            constraintEl.attributeValue("preference"));
                }
                if (constraint != null) {
                    for (Iterator<?> j = constraintEl.elementIterator("request"); j.hasNext();) {
                        Element f = (Element) j.next();
                        TeachingRequest request = requests.get(Long.valueOf(f.attributeValue("id")));
                        if (request != null) {
                            int index = Integer.valueOf(f.attributeValue("index", "0"));
                            if (index >= 0 && index < request.getNrInstructors())
                                constraint.addVariable(request.getVariables()[index]);
                        }
                    }
                    addConstraint(constraint);
                }
            }            
        }
        for (Map.Entry<Long, Long> e: parents.entrySet())
            attributes.get(e.getKey()).setParentAttribute(attributes.get(e.getValue()));
        for (Map.Entry<TeachingRequest, Map<Integer, Instructor>> e1: best.entrySet())
            for (Map.Entry<Integer, Instructor> e2: e1.getValue().entrySet())
                if (e2.getKey() >= 0 && e2.getKey() < e1.getKey().getNrInstructors()) {
                    TeachingRequest.Variable variable = e1.getKey().getVariables()[e2.getKey()];
                    variable.setBestAssignment(new TeachingAssignment(variable, e2.getValue()), 0l);
                }

        for (Map.Entry<TeachingRequest, Map<Integer, Instructor>> e1: initial.entrySet())
            for (Map.Entry<Integer, Instructor> e2: e1.getValue().entrySet())
                if (e2.getKey() >= 0 && e2.getKey() < e1.getKey().getNrInstructors()) {
                    TeachingRequest.Variable variable = e1.getKey().getVariables()[e2.getKey()];
                    variable.setInitialAssignment(new TeachingAssignment(variable, e2.getValue()));
                }
        
        if (!current.isEmpty()) {
            for (Map.Entry<TeachingRequest, Map<Integer, Instructor>> e1: current.entrySet())
                for (Map.Entry<Integer, Instructor> e2: e1.getValue().entrySet())
                    if (e2.getKey() >= 0 && e2.getKey() < e1.getKey().getNrInstructors()) {
                        TeachingRequest.Variable variable = e1.getKey().getVariables()[e2.getKey()];
                        TeachingAssignment ta = new TeachingAssignment(variable, e2.getValue());
                        Set<TeachingAssignment> conf = conflictValues(assignment, ta);
                        if (conf.isEmpty()) {
                            assignment.assign(0, ta);
                        } else {
                            sLog.error("Unable to assign " + ta.getName() + " to " + variable.getName());
                            sLog.error("Conflicts:" + ToolBox.dict2string(conflictConstraints(assignment, ta), 2));
                        }
                    }
        }
        
        return true;
    }
    
    /** Convert bitset to a bit string */
    protected static String bitset2string(BitSet b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length(); i++)
            sb.append(b.get(i) ? "1" : "0");
        return sb.toString();
    }

    /** Create BitSet from a bit string */
    protected static BitSet createBitSet(String bitString) {
        if (bitString == null) return null;
        BitSet ret = new BitSet(bitString.length());
        for (int i = 0; i < bitString.length(); i++)
            if (bitString.charAt(i) == '1')
                ret.set(i);
        return ret;
    }
    
    /** Convert preference string to a preference value */
    protected static int string2preference(String pref) {
        if (pref == null || pref.isEmpty()) return 0;
        if (Constants.sPreferenceRequired.equals(pref))
            return Constants.sPreferenceLevelRequired;
        if (Constants.sPreferenceProhibited.equals(pref))
            return Constants.sPreferenceLevelProhibited;
        return Integer.valueOf(pref);
    }
    
    private List<BitSet> iWeeks = null;
    /**
     * The method creates date patterns (bitsets) which represent the weeks of a
     * semester.
     *      
     * @return a list of BitSets which represents the weeks of a semester.
     */
    public List<BitSet> getWeeks() {
        if (iWeeks == null) {
            String defaultDatePattern = getProperties().getProperty("DatePattern.CustomDatePattern", null);
            if (defaultDatePattern == null){                
                defaultDatePattern = getProperties().getProperty("DatePattern.Default");
            }
            BitSet fullTerm = null;
            if (defaultDatePattern == null) {
                // Take the date pattern that is being used most often
                Map<Long, Integer> counter = new HashMap<Long, Integer>();
                int max = 0; String name = null; Long id = null;
                for (TeachingRequest.Variable tr: variables()) {
                    for (Section section: tr.getSections()) {
                        TimeLocation time = section.getTime();
                        if (time.getWeekCode() != null && time.getDatePatternId() != null) {
                            int count = 1;
                            if (counter.containsKey(time.getDatePatternId()))
                                count += counter.get(time.getDatePatternId());
                            counter.put(time.getDatePatternId(), count);
                            if (count > max) {
                                max = count; fullTerm = time.getWeekCode(); name = time.getDatePatternName(); id = time.getDatePatternId();
                            }
                        }
                    }
                }
                sLog.info("Using date pattern " + name + " (id " + id + ") as the default.");
            } else {
                // Create default date pattern
                fullTerm = new BitSet(defaultDatePattern.length());
                for (int i = 0; i < defaultDatePattern.length(); i++) {
                    if (defaultDatePattern.charAt(i) == 49) {
                        fullTerm.set(i);
                    }
                }
            }
            
            if (fullTerm == null) return null;
            
            iWeeks = new ArrayList<BitSet>();
            if (getProperties().getPropertyBoolean("DatePattern.ShiftWeeks", false)) {
                // Cut date pattern into weeks (each week takes 7 consecutive bits, starting on the next positive bit)
                for (int i = fullTerm.nextSetBit(0); i < fullTerm.length(); ) {
                    if (!fullTerm.get(i)) {
                        i++; continue;
                    }
                    BitSet w = new BitSet(i + 7);
                    for (int j = 0; j < 7; j++)
                        if (fullTerm.get(i + j)) w.set(i + j);
                    iWeeks.add(w);
                    i += 7;
                }                
            } else {
                // Cut date pattern into weeks (each week takes 7 consecutive bits starting on the first bit of the default date pattern, no pauses between weeks)
                for (int i = fullTerm.nextSetBit(0); i < fullTerm.length(); ) {
                    BitSet w = new BitSet(i + 7);
                    for (int j = 0; j < 7; j++)
                        if (fullTerm.get(i + j)) w.set(i + j);
                    iWeeks.add(w);
                    i += 7;
                }
            }
        }
        return iWeeks;
    }
}
