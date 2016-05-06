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

import org.apache.log4j.Logger;
import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.constraints.InstructorConstraint;
import org.cpsolver.instructor.constraints.SameInstructorConstraint;
import org.cpsolver.instructor.constraints.SameLinkConstraint;
import org.cpsolver.instructor.criteria.AttributePreferences;
import org.cpsolver.instructor.criteria.BackToBack;
import org.cpsolver.instructor.criteria.CoursePreferences;
import org.cpsolver.instructor.criteria.InstructorPreferences;
import org.cpsolver.instructor.criteria.SameInstructor;
import org.cpsolver.instructor.criteria.DifferentLecture;
import org.cpsolver.instructor.criteria.SameLink;
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
public class InstructorSchedulingModel extends Model<TeachingRequest, TeachingAssignment> {
    private static Logger sLog = Logger.getLogger(InstructorSchedulingModel.class);
    private DataProperties iProperties;
    private Set<Attribute.Type> iTypes = new HashSet<Attribute.Type>();

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
    }
    
    /**
     * Return solver configuration
     * @return data properties given in the constructor
     */
    public DataProperties getProperties() {
        return iProperties;
    }
    
    @Override
    public void addConstraint(Constraint<TeachingRequest, TeachingAssignment> constraint) {
        super.addConstraint(constraint);
        if (constraint instanceof InstructorConstraint) {
            InstructorConstraint ic = (InstructorConstraint) constraint;
            for (Attribute attribute: ic.getInstructor().getAttributes())
                addAttributeType(attribute.getType());
        }
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
    public Map<String, String> getInfo(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        Map<String, String> info = super.getInfo(assignment);

        double totalLoad = 0.0;
        double assignedLoad = 0.0;
        for (TeachingRequest clazz : variables()) {
            totalLoad += clazz.getLoad();
            if (assignment.getValue(clazz) != null)
                assignedLoad += clazz.getLoad();
        }
        info.put("Assigned load", getPerc(assignedLoad, totalLoad, 0) + "% (" + sDoubleFormat.format(assignedLoad) + " / " + sDoubleFormat.format(totalLoad) + ")");

        return info;
    }

    @Override
    public double getTotalValue(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        double ret = 0;
        for (Criterion<TeachingRequest, TeachingAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment);
        return ret;
    }

    @Override
    public double getTotalValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double ret = 0;
        for (Criterion<TeachingRequest, TeachingAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment, variables);
        return ret;
    }
    
    /**
     * Store the problem (together with its solution) in an XML format
     * @param assignment current assignment
     * @return XML document with the problem
     */
    public Document save(Assignment<TeachingRequest, TeachingAssignment> assignment) {
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
            for (TeachingRequest request: variables()) {
                for (Preference<Attribute> pref: request.getAttributePreferences()) {
                    Attribute attribute = pref.getTarget();
                    if (type.equals(attribute.getType()) && attributes.add(attribute)) {
                        Element attributeEl = typeEl.addElement("attribute");
                        if (attribute.getAttributeId() != null)
                            attributeEl.addAttribute("id", String.valueOf(attribute.getAttributeId()));
                        attributeEl.addAttribute("name", attribute.getAttributeName());
                    }
                }
                for (Constraint<TeachingRequest, TeachingAssignment> c: constraints()) {
                    if (c instanceof InstructorConstraint) {
                        Instructor instructor = ((InstructorConstraint)c).getInstructor();
                        for (Attribute attribute: instructor.getAttributes()) {
                            if (type.equals(attribute.getType()) && attributes.add(attribute)) {
                                Element attributeEl = typeEl.addElement("attribute");
                                if (attribute.getAttributeId() != null)
                                    attributeEl.addAttribute("id", String.valueOf(attribute.getAttributeId()));
                                attributeEl.addAttribute("name", attribute.getAttributeName());
                            }
                        }
                    }
                }
            }
        }
        Set<Course> courses = new HashSet<Course>();
        Element coursesEl = root.addElement("courses");
        Element requestsEl = root.addElement("teaching-requests");
        for (TeachingRequest request: variables()) {
            Element requestEl = requestsEl.addElement("request");
            requestEl.addAttribute("id", String.valueOf(request.getRequestId()));
            if (request.getInstructorIndex() != 0)
                requestEl.addAttribute("index", String.valueOf(request.getInstructorIndex()));
            Course course = request.getCourse();
            if (courses.add(course)) {
                Element courseEl = coursesEl.addElement("course");
                if (course.getCourseId() != null)
                    courseEl.addAttribute("id", String.valueOf(course.getCourseId()));
                if (course.getCourseName() != null)
                    courseEl.addAttribute("name", String.valueOf(course.getCourseName()));
                if (course.isExclusive())
                    courseEl.addAttribute("exclusive", "true");
                if (course.isSameCommon())
                    courseEl.addAttribute("common", "true");                
            }
            requestEl.addAttribute("course", String.valueOf(request.getCourse().getCourseId()));
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
            for (Preference<Attribute> pref: request.getAttributePreferences()) {
                Element attributeEl = requestEl.addElement("attribute");
                if (pref.getTarget().getAttributeId() != null)
                    attributeEl.addAttribute("id", String.valueOf(pref.getTarget().getAttributeId()));
                attributeEl.addAttribute("name", pref.getTarget().getAttributeName());
                attributeEl.addAttribute("type", pref.getTarget().getType().getTypeName());
                attributeEl.addAttribute("preference", (pref.isRequired() ? "R" : pref.isProhibited() ? "P" : String.valueOf(pref.getPreference())));
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
            if (saveBest && request.getBestAssignment() != null) {
                Instructor instructor = request.getBestAssignment().getInstructor();
                Element instructorEl = requestEl.addElement("best-instructor");
                instructorEl.addAttribute("id", String.valueOf(instructor.getInstructorId()));
                if (instructor.hasExternalId())
                    instructorEl.addAttribute("externalId", instructor.getExternalId());
                if (instructor.hasName())
                    instructorEl.addAttribute("name", instructor.getName());
            }
            if (saveInitial && request.getInitialAssignment() != null) {
                Instructor instructor = request.getInitialAssignment().getInstructor();
                Element instructorEl = requestEl.addElement("initial-instructor");
                instructorEl.addAttribute("id", String.valueOf(instructor.getInstructorId()));
                if (instructor.hasExternalId())
                    instructorEl.addAttribute("externalId", instructor.getExternalId());
                if (instructor.hasName())
                    instructorEl.addAttribute("name", instructor.getName());
            }
            if (saveSolution) {
                TeachingAssignment ta = assignment.getValue(request);
                if (ta != null) {
                    Instructor instructor = ta.getInstructor();
                    Element instructorEl = requestEl.addElement("assigned-instructor");
                    instructorEl.addAttribute("id", String.valueOf(instructor.getInstructorId()));
                    if (instructor.hasExternalId())
                        instructorEl.addAttribute("externalId", instructor.getExternalId());
                    if (instructor.hasName())
                        instructorEl.addAttribute("name", instructor.getName());
                }
            }
        }
        Element instructorsEl = root.addElement("instructors");
        Element constraintsEl = root.addElement("constraints");
        for (Constraint<TeachingRequest, TeachingAssignment> c: constraints()) {
            if (c instanceof InstructorConstraint) {
                InstructorConstraint ic = (InstructorConstraint)c;
                Instructor instructor = ic.getInstructor();
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
                for (Attribute attribute: instructor.getAttributes()) {
                    Element attributeEl = instructorEl.addElement("attribute");
                    if (attribute.getAttributeId() != null)
                        attributeEl.addAttribute("id", String.valueOf(attribute.getAttributeId()));
                    attributeEl.addAttribute("name", attribute.getAttributeName());
                    attributeEl.addAttribute("type", attribute.getType().getTypeName());
                }
                instructorEl.addAttribute("maxLoad", sDoubleFormat.format(instructor.getMaxLoad()));
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
                    timeEl.setText(tl.getLongName(false));
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
            } else if (c instanceof SameInstructorConstraint) {
                SameInstructorConstraint si = (SameInstructorConstraint) c;
                Element sameInstEl = constraintsEl.addElement("same-instructor");
                if (si.getConstraintId() != null)
                    sameInstEl.addAttribute("id", String.valueOf(si.getConstraintId()));
                if (si.getName() != null)
                    sameInstEl.addAttribute("name", si.getName());
                sameInstEl.addAttribute("preference", Constants.preferenceLevel2preference(si.getPreference()));
                for (TeachingRequest request: c.variables()) {
                    Element assignmentEl = sameInstEl.addElement("request");
                    assignmentEl.addAttribute("id", String.valueOf(request.getRequestId()));
                    if (request.getInstructorIndex() != 0)
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
                for (TeachingRequest request: c.variables()) {
                    Element assignmentEl = sameInstEl.addElement("request");
                    assignmentEl.addAttribute("id", String.valueOf(request.getRequestId()));
                    if (request.getInstructorIndex() != 0)
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
    public boolean load(Document document, Assignment<TeachingRequest, TeachingAssignment> assignment) {
        boolean loadInitial = getProperties().getPropertyBoolean("Xml.LoadInitial", true);
        boolean loadBest = getProperties().getPropertyBoolean("Xml.LoadBest", true);
        boolean loadSolution = getProperties().getPropertyBoolean("Xml.LoadSolution", true);
        String defaultBtb = getProperties().getProperty("Defaults.BackToBack", "0");
        String defaultConjunctive = getProperties().getProperty("Defaults.Conjunctive", "false");
        String defaultRequired = getProperties().getProperty("Defaults.Required", "false");
        String defaultExclusive = getProperties().getProperty("Defaults.Exclusive", "false");
        String defaultCommon = getProperties().getProperty("Defaults.SameCommon", "false");
        Element root = document.getRootElement();
        if (!"instructor-schedule".equals(root.getName()))
            return false;
        Map<String, Attribute.Type> types = new HashMap<String, Attribute.Type>();
        Map<Long, Attribute> attributes = new HashMap<Long, Attribute>();
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
                }
            }
        }
        Map<Long, Course> courses = new HashMap<Long, Course>();
        if (root.element("courses") != null) {
            for (Iterator<?> i = root.element("courses").elementIterator("course"); i.hasNext();) {
                Element courseEl = (Element) i.next();
                Course course = new Course(
                        Long.parseLong(courseEl.attributeValue("id")),
                        courseEl.attributeValue("name"),
                        "true".equalsIgnoreCase(courseEl.attributeValue("exclusive", defaultExclusive)),
                        "true".equalsIgnoreCase(courseEl.attributeValue("common", defaultCommon)));
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
                }
                instructor.addAttribute(attribute);
            }
            for (Iterator<?> j = instructorEl.elementIterator("time"); j.hasNext();) {
                Element f = (Element) j.next();
                TimeLocation time = new TimeLocation(
                        Integer.parseInt(f.attributeValue("days"), 2),
                        Integer.parseInt(f.attributeValue("start")),
                        Integer.parseInt(f.attributeValue("length")), 0, 0,
                        f.attributeValue("datePattern") == null ? null : Long.valueOf(f.attributeValue("datePattern")),
                        f.attributeValue("datePatternName", ""),
                        createBitSet(f.attributeValue("dates")),
                        Integer.parseInt(f.attributeValue("breakTime", "0")));
                if (f.attributeValue("pattern") != null)
                    time.setTimePatternId(Long.valueOf(f.attributeValue("pattern")));
                instructor.addTimePreference(new Preference<TimeLocation>(time, string2preference(f.attributeValue("preference"))));
            }
            for (Iterator<?> j = instructorEl.elementIterator("course"); j.hasNext();) {
                Element f = (Element) j.next();
                Long courseId = Long.parseLong(f.attributeValue("id"));
                Course course = courses.get(courseId);
                if (course == null) {
                    course = new Course(courseId,
                            f.attributeValue("name"),
                            "true".equalsIgnoreCase(f.attributeValue("exclusive", defaultExclusive)),
                            "true".equalsIgnoreCase(f.attributeValue("common", defaultCommon)));
                    courses.put(course.getCourseId(), course);
                }
                instructor.addCoursePreference(new Preference<Course>(course, string2preference(f.attributeValue("preference"))));
            }
            addConstraint(instructor.getConstraint());
            instructors.put(instructor.getInstructorId(), instructor);
        }
        Map<Long, Map<Integer, TeachingRequest>> requests = new HashMap<Long, Map<Integer, TeachingRequest>>();
        Map<TeachingRequest, Instructor> current = new HashMap<TeachingRequest, Instructor>();
        Map<TeachingRequest, Instructor> best = new HashMap<TeachingRequest, Instructor>();
        Map<TeachingRequest, Instructor> initial = new HashMap<TeachingRequest, Instructor>();
        for (Iterator<?> i = root.element("teaching-requests").elementIterator("request"); i.hasNext();) {
            Element requestEl = (Element) i.next();
            Element courseEl = requestEl.element("course");
            Course course = null;
            if (courseEl != null) {
                Long courseId = Long.valueOf(courseEl.attributeValue("id"));
                course = courses.get(courseId);
                if (course == null) {
                    course = new Course(courseId,
                            courseEl.attributeValue("name"),
                            "true".equalsIgnoreCase(courseEl.attributeValue("exclusive", defaultExclusive)),
                            "true".equalsIgnoreCase(courseEl.attributeValue("common", defaultCommon)));
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
                    Integer.parseInt(requestEl.attributeValue("index", "0")),
                    course,
                    Float.valueOf(requestEl.attributeValue("load", "0")),
                    sections);
            Map<Integer, TeachingRequest> requestsSameId = requests.get(request.getRequestId());
            if (requestsSameId == null) {
                requestsSameId = new HashMap<Integer, TeachingRequest>();
                requests.put(request.getRequestId(), requestsSameId);
            }
            requestsSameId.put(request.getInstructorIndex(), request);
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
            if (loadBest && requestEl.element("best-instructor") != null)
                best.put(request, instructors.get(Long.valueOf(requestEl.element("best-instructor").attributeValue("id"))));
            if (loadInitial && requestEl.element("initial-instructor") != null)
                initial.put(request, instructors.get(Long.valueOf(requestEl.element("initial-instructor").attributeValue("id"))));
            if (loadSolution && requestEl.element("assigned-instructor") != null)
                current.put(request, instructors.get(Long.valueOf(requestEl.element("assigned-instructor").attributeValue("id"))));
            addVariable(request);
        }
        if (root.element("constraints") != null) {
            for (Iterator<?> i = root.element("constraints").elementIterator(); i.hasNext();) {
                Element constraintEl = (Element) i.next();
                Constraint<TeachingRequest, TeachingAssignment> constraint = null;
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
                        Map<Integer, TeachingRequest> requestsSameId = requests.get(Long.valueOf(f.attributeValue("id")));
                        if (requestsSameId != null) {
                            TeachingRequest request = requestsSameId.get(Integer.valueOf(f.attributeValue("index", "0")));
                            if (request != null)
                                constraint.addVariable(request);
                        }
                    }
                    addConstraint(constraint);
                }
            }            
        }
        for (Instructor instructor: instructors.values()) {
            for (TeachingRequest clazz : variables()) {
                if (instructor.canTeach(clazz) && !clazz.getAttributePreference(instructor).isProhibited())
                    instructor.getConstraint().addVariable(clazz);
            }
        }
        for (Map.Entry<TeachingRequest, Instructor> entry: best.entrySet())
            entry.getKey().setBestAssignment(new TeachingAssignment(entry.getKey(), entry.getValue()), 0l);

        for (Map.Entry<TeachingRequest, Instructor> entry: initial.entrySet())
            entry.getKey().setInitialAssignment(new TeachingAssignment(entry.getKey(), entry.getValue()));
        
        if (!current.isEmpty()) {
            for (Map.Entry<TeachingRequest, Instructor> entry: current.entrySet()) {
                TeachingRequest request = entry.getKey();
                TeachingAssignment ta = new TeachingAssignment(request, entry.getValue());
                Set<TeachingAssignment> conf = conflictValues(assignment, ta);
                if (conf.isEmpty()) {
                    assignment.assign(0, ta);
                } else {
                    sLog.error("Unable to assign " + ta.getName() + " to " + request.getName());
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
}
