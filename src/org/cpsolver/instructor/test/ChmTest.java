package org.cpsolver.instructor.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Iterator;

import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.Test;
import org.cpsolver.instructor.constraints.InstructorConstraint;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.Preference;
import org.cpsolver.instructor.model.Section;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * General chemistry teaching assistants test. No soft constraints at the moment.
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
public class ChmTest extends Test {
    public ChmTest(DataProperties properties) {
        super(properties);
    }
    
    @Override
    protected void generateReports(File dir, Assignment<TeachingRequest, TeachingAssignment> assignment) throws IOException {
        PrintWriter out = new PrintWriter(new File(dir, "solution-assignments.csv"));
        out.println("Course,Section,Time,Room,Load,Student,Name,Not Available,Preference (Avoid),Max Load");
        for (TeachingRequest request : variables()) {
            out.print(request.getCourse().getCourseName());
            String sect = "", time = "", room = "";
            for (Iterator<Section> i = request.getSections().iterator(); i.hasNext(); ) {
                Section section = i.next();
                sect += section.getSectionName();
                time += (section.getTime() == null ? "-" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(true));
                room += (section.getRoom() == null ? "-" : section.getRoom());
                if (i.hasNext()) { sect += ", "; time += ", "; room += ", "; }
            }
            out.print(",\"" + sect + "\",\"" + time + "\",\"" + room + "\"");
            out.print("," + new DecimalFormat("0.0").format(request.getLoad()));
            TeachingAssignment ta = assignment.getValue(request);
            if (ta != null) {
                Instructor instructor = ta.getInstructor();
                out.print(instructor.getExternalId());
                out.print(",\"" + instructor.getName() + "\"");
                out.print(",\"" + instructor.getAvailable() + "\"");
                String timePref = "";
                for (Preference<TimeLocation> p: instructor.getTimePreferences()) {
                    if (!p.isProhibited()) {
                        if (!timePref.isEmpty()) timePref += ", ";
                        timePref += p.getTarget().getLongName(true).trim();
                    }
                }
                out.print(",\"" + timePref + "\"");
                out.print("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
            }
            out.println();
        }
        out.flush();
        out.close();

        out = new PrintWriter(new File(dir, "solution-students.csv"));
        out.println("Student,Name,Not Available,Preference (Avoid),Max Load,Assigned Load,Back-To-Back,Different Lecture,Overlap [h],1st Assignment,2nd Assignment, 3rd Assignment");
        for (Constraint<TeachingRequest, TeachingAssignment> constraint : constraints()) {
            if (constraint instanceof InstructorConstraint) {
                InstructorConstraint ic = (InstructorConstraint) constraint;
                Instructor instructor = ic.getInstructor();
                out.print(instructor.getExternalId());
                out.print(",\"" + instructor.getName() + "\"");
                out.print(",\"" + instructor.getAvailable() + "\"");
                String timePref = "";
                for (Preference<TimeLocation> p: instructor.getTimePreferences()) {
                    if (!p.isProhibited()) {
                        if (!timePref.isEmpty()) timePref += ", ";
                        timePref += p.getTarget().getLongName(true).trim();
                    }
                }
                out.print(",\"" + timePref + "\"");
                out.print("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
                InstructorConstraint.Context context = ic.getContext(assignment);
                out.print("," + new DecimalFormat("0.0").format(context.getLoad()));
                out.print("," + (context.countBackToBackPercentage() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countBackToBackPercentage())));
                out.print("," + (context.countDifferentLectures() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countDifferentLectures())));
                int share = 0;
                for (TeachingAssignment ta : context.getAssignments()) {
                    for (Preference<TimeLocation> p: instructor.getTimePreferences()) {
                        if (!p.isProhibited())
                            share += ta.variable().share(p.getTarget());
                    }
                }
                out.print("," + (share == 0 ? "" : new DecimalFormat("0.#").format(share / 12.0)));
                for (TeachingAssignment ta : context.getAssignments()) {
                    String sect = "";
                    for (Iterator<Section> i = ta.variable().getSections().iterator(); i.hasNext(); ) {
                        Section section = i.next();
                        sect += section.getSectionName() + (section.getTime() == null ? "" : " " + section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(true));
                        if (i.hasNext()) sect += ", ";
                    }
                    out.print(",\"" + ta.variable().getCourse() + " " + sect + "\"");
                }
                out.println();
            }
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
