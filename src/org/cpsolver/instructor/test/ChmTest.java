package org.cpsolver.instructor.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.TreeSet;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.Test;
import org.cpsolver.instructor.model.Attribute;
import org.cpsolver.instructor.model.Course;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.Preference;
import org.cpsolver.instructor.model.Section;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * General chemistry teaching assistants test. No soft constraints at the moment.
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
public class ChmTest extends Test {
    
    public ChmTest(DataProperties properties) {
        super(properties);
    }
    
    public String getAttributes(TeachingRequest req, String type) {
        TreeSet<String> attributes = new TreeSet<String>();
        for (Preference<Attribute> attribute: req.getAttributePreferences())
            if (attribute.getTarget().getType().getTypeName().equals(type)) {
                attributes.add(attribute.getTarget().getAttributeName());// + (attribute.isRequired() ? "" : ":" + Constants.preferenceLevel2preference(attribute.getPreference())));
            }
        StringBuffer s = new StringBuffer();
        for (String attribute: attributes) {
            if (s.length() > 0) s.append(",");
            s.append(attribute);
        }
        return s.toString();
    }
    
    public String getAttributes(Instructor instructor, String type) {
        TreeSet<String> attributes = new TreeSet<String>();
        for (Attribute attribute: instructor.getAttributes())
            if (attribute.getType().getTypeName().equals(type)) {
                attributes.add(attribute.getAttributeName());
            }
        StringBuffer s = new StringBuffer();
        if ("Qualification".equals(type))
            for (Preference<Course> p: instructor.getCoursePreferences())
                attributes.remove(p.getTarget().getCourseName().substring(4));
        for (String attribute: attributes) {
            // if ("00000".equals(attribute)) continue;
            if (s.length() > 0) s.append(",");
            s.append(attribute);
        }
        return s.toString();
    }
    
    public String getCoursePrefs(Instructor instructor) {
        TreeSet<String> attributes = new TreeSet<String>();
        for (Preference<Course> p: instructor.getCoursePreferences())
            attributes.add(p.getTarget().getCourseName().substring(4));
        StringBuffer s = new StringBuffer();
        for (String attribute: attributes) {
            if (s.length() > 0) s.append(",");
            s.append(attribute);
        }
        return s.toString();
    }
    
    public String getCoursePreference(TeachingRequest req, Instructor instructor) {
        Preference<Course> p = instructor.getCoursePreference(req.getCourse());
        if (p.getPreference() == 0) return "";
        String pref = Constants.preferenceLevel2preference(p.getPreference());
        if ("R".equals(pref)) return "Yes (SUPER)";
        if ("-2".equals(pref)) return "Yes";
        if ("-1".equals(pref)) return "Organic Lab";
        return pref;
    }
    
    public String getAttributes(TeachingRequest req, Instructor instructor, String type) {
        TreeSet<String> attributes = new TreeSet<String>();
        for (Preference<Attribute> attribute: req.getAttributePreferences())
            if (instructor.getAttributes().contains(attribute.getTarget()))
                if (attribute.getTarget().getType().getTypeName().equals(type)) {
                    attributes.add(attribute.getTarget().getAttributeName());
                }
        StringBuffer s = new StringBuffer();
        for (String attribute: attributes) {
            // if ("00000".equals(attribute)) continue;
            if (s.length() > 0) s.append(",");
            s.append(attribute);
        }
        if (attributes.isEmpty())
            s.append("no match");
        return s.toString();
    }
    
    @Override
    protected void generateReports(File dir, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) throws IOException {
        PrintWriter out = new PrintWriter(new File(dir, "solution-assignments.csv"));
        out.println("Course,Sections,Time,Room,Skill,Qualification,Performance,Load,Student,Name,Not Available,Max Load,Skill,Qualification,Performance,Requested");
        for (TeachingRequest.Variable request : variables()) {
            out.print(request.getCourse().getCourseName());
            String sect = "", time = "", room = "";
            if (request.getId() < 0) {
                out.print(",\"SUPER\",,");
            } else {
                for (Iterator<Section> i = request.getSections().iterator(); i.hasNext(); ) {
                    Section section = i.next();
                    // if (section.isCommon() && section.isAllowOverlap()) continue;
                    if (!sect.isEmpty()) { sect += ", "; time += ", "; room += ", "; }
                    sect += (section.isCommon() ? "(" : "") + section.getSectionType() + " " + section.getExternalId() + (section.isCommon() ? ")" : "") ;
                    time += (section.getTime() == null ? "-" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(true) + "-" + section.getTime().getEndTimeHeader(true));
                    room += (section.getRoom() == null ? "-" : section.getRoom());
                }
                out.print(",\"" + sect + "\",\"" + time + "\",\"" + room + "\"");
            }
            out.print(",\"" + getAttributes(request.getRequest(), "Skill") + "\"");
            out.print(",\"" + getAttributes(request.getRequest(), "Qualification") + "\"");
            out.print(",\"" + getAttributes(request.getRequest(), "Performance Level") + "\"");
            out.print("," + new DecimalFormat("0.0").format(request.getRequest().getLoad()));
            TeachingAssignment ta = assignment.getValue(request);
            if (ta != null) {
                Instructor instructor = ta.getInstructor();
                out.print("," + instructor.getExternalId());
                out.print(",\"" + instructor.getName() + "\"");
                out.print(",\"" + instructor.getAvailable() + "\"");
                out.print("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
                out.print(",\"" + getAttributes(request.getRequest(), instructor, "Skill") + "\"");
                out.print(",\"" + getAttributes(request.getRequest(), instructor, "Qualification") + "\"");
                out.print(",\"" + getAttributes(request.getRequest(), instructor, "Performance Level") + "\"");
                out.print(",\"" + getCoursePreference(request.getRequest(), instructor) + "\"");
            }
            out.println();
        }
        out.flush();
        out.close();

        out = new PrintWriter(new File(dir, "solution-students.csv"));
        out.println("Student,Name,Not Available,Skill,Qualification,Performance,Requests,Max Load,Assigned Load,1st Assignment,2nd Assignment,Skill,Qualification,Performance,Requested");
        for (Instructor instructor: getInstructors()) {
            out.print(instructor.getExternalId());
            out.print(",\"" + instructor.getName() + "\"");
            out.print(",\"" + instructor.getAvailable() + "\"");
            out.print(",\"" + getAttributes(instructor, "Skill") + "\"");
            out.print(",\"" + getAttributes(instructor, "Qualification") + "\"");
            out.print(",\"" + getAttributes(instructor, "Performance Level") + "\"");
            out.print(",\"" + getCoursePrefs(instructor) + "\"");
            out.print("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
            Instructor.Context context = instructor.getContext(assignment);
            out.print("," + new DecimalFormat("0.0").format(context.getLoad()));
            /*
            out.print("," + (context.countBackToBackPercentage() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countBackToBackPercentage())));
            out.print("," + (context.countDifferentLectures() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countDifferentLectures())));
            int share = 0;
            for (TeachingAssignment ta : context.getAssignments()) {
                for (Preference<TimeLocation> p: instructor.getTimePreferences()) {
                    if (!p.isProhibited())
                        share += ta.variable().getRequest().share(p.getTarget());
                }
            }
            out.print("," + (share == 0 ? "" : new DecimalFormat("0.#").format(share / 12.0)));
            */
            TeachingRequest req = null;
            for (TeachingAssignment ta : context.getAssignments()) {
                String sect = "";
                if (req == null || req.getRequestId() < 0) req = ta.variable().getRequest();
                if (ta.variable().getId() < 0) {
                    sect = "SUPER";
                } else {
                    for (Iterator<Section> i = ta.variable().getSections().iterator(); i.hasNext(); ) {
                        Section section = i.next();
                        if (section.isCommon() && section.isAllowOverlap()) continue;
                        sect += (sect.isEmpty() ? "" : ", ") + (section.isCommon() ? "(" : "") +
                                section.getSectionType() + " " + section.getExternalId() +
                                (section.getTime() == null ? "" : " " + section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(true) +
                                "-" + section.getTime().getEndTimeHeader(true))+
                                (section.isCommon() ? ")" : "");
                    }
                }
                out.print(",\"" + ta.variable().getCourse() + " " + sect + "\"");
            }
            if (req != null) {
                for (int i = context.getAssignments().size(); i < 2; i++) out.print(",");
                out.print(",\"" + getAttributes(req, instructor, "Skill") + "\"");
                out.print(",\"" + getAttributes(req, instructor, "Qualification") + "\"");
                out.print(",\"" + getAttributes(req, instructor, "Performance Level") + "\"");
                out.print(",\"" + getCoursePreference(req, instructor) + "\"");
            }
            out.println();
        }
        out.flush();
        out.close();
    }
    
    public static void main(String[] args) throws Exception {
        DataProperties config = new DataProperties();
        config.load(ChmTest.class.getClass().getResourceAsStream("/org/cpsolver/instructor/test/chm.properties"));
        config.putAll(System.getProperties());
        ToolBox.configureLogging();
        
        new ChmTest(config).execute();
    }
}
