package net.sf.cpsolver.studentsct.check;

import java.text.DecimalFormat;
import java.util.Enumeration;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * This class looks and reports cases when there are more students requesting a course than the course limit.  
 *  
 * <br><br>
 * 
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp; CourseLimitCheck ch = new CourseLimitCheck(model);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; if (!ch.check()) ch.getCSVFile().save(new File("limits.csv"));
 * </code>
 * 
 * <br><br>
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
public class CourseLimitCheck {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(OverlapCheck.class);
    private static DecimalFormat sDF = new DecimalFormat("0.0");
    private StudentSectioningModel iModel;
    private CSVFile iCSVFile = null;
    
    /** Constructor
     * @param model student sectioning model
     */
    public CourseLimitCheck(StudentSectioningModel model) {
        iModel = model;
        iCSVFile = new CSVFile();
        iCSVFile.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("Course"),
                new CSVFile.CSVField("Limit"),
                new CSVFile.CSVField("Students"),
                new CSVFile.CSVField("Real"),
                new CSVFile.CSVField("Last-like")
        });
    }
    
    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
    }
    
    /** Return report */
    public CSVFile getCSVFile() { 
        return iCSVFile; 
    }
    
    /** Check for courses where the limit is below the number of students that request the course
     * @return false, if there is such a case
     */
    public boolean check() {
        sLog.info("Checking for course limits...");
        boolean ret = true;
        for (Enumeration e=getModel().getOfferings().elements();e.hasMoreElements();) {
            Offering offering = (Offering)e.nextElement();
            for (Enumeration f=offering.getCourses().elements();f.hasMoreElements();) {
                Course course = (Course)f.nextElement();
                double total = 0;
                double lastLike = 0, real = 0;
                for (Enumeration g=getModel().variables().elements();g.hasMoreElements();) {
                    Request request = (Request)g.nextElement();
                    if (request instanceof CourseRequest && ((CourseRequest)request).getCourses().contains(course)) {
                        total += request.getWeight();
                        if (request.getStudent().isDummy())
                            lastLike += request.getWeight();
                        else
                            real += request.getWeight();
                    }
                }
                if (Math.round(total)>course.getLimit()) {
                    sLog.error("Course "+course+" is requested by "+sDF.format(total)+" students, but its limit is only "+course.getLimit());
                    ret = false;
                    iCSVFile.addLine(new CSVFile.CSVField[] {
                       new CSVFile.CSVField(course.getName()),
                       new CSVFile.CSVField(course.getLimit()),
                       new CSVFile.CSVField(total),
                       new CSVFile.CSVField(real),
                       new CSVFile.CSVField(lastLike)
                    });
                }
            }
        }
        return ret;
    }

}
