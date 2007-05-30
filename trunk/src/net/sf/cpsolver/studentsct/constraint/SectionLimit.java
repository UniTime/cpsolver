package net.sf.cpsolver.studentsct.constraint;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;

/**
 * Section limit constraint. This global constraint ensures that 
 * a limit of each section is not exceeded. This means that the total
 * sum of weights of course requests (see {@link Request#getWeight()}) enrolled 
 * into a section is below the section's limit (see {@link Section#getLimit()}).  
 * 
 * <br><br>
 * Sections with negative limit are considered unlimited, and therefore
 * completely ignored by this constraint.
 * 
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
public class SectionLimit extends GlobalConstraint {
    private static double sMinWeight = 0.0;
    private static double sMaxWeight = 0.0001;
    private static double sRatio = 1.0;
    
    /** 
     * A wait of a request that is going to be enrolled into a section. 
     * Some operations may be applied in order to overcome the rounding 
     * problem with last-like students (e.g., 5 students are projected to
     * two sections of limit 2 -- each section can have up to 3
     * of these last-like students).
     * @param request a request of a student
     * @return request's weight 
     */
    public static double getWeight(Request request) {
        return Math.min(sMinWeight, Math.max(sMaxWeight, sRatio * request.getWeight())); 
    }

    /**
     * A given enrollment is conflicting, if there is a section which limit (excluding the 
     * particular request of the given enrollment -- see {@link Section#getEnrollmentWeight(Request)}) 
     * plus the weight computed by {@link SectionLimit#getWeight(Request)} exceeds the section limit.
     * <br>
     * For each of such sections, one or more existing enrollments are (randomly) 
     * selected as conflicting untill the overall weight is under the limit.
     * 
     * @param value {@link Enrollment} that is being considered
     * @return conflicts all computed conflicting requests are added into this set
     */
    public void computeConflicts(Value value, Set conflicts) {
        //get enrollment
        Enrollment enrollment = (Enrollment)value;
        
        //exclude free time requests
        if (!enrollment.isCourseRequest()) return;
        
        //for each section
        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
            
            Section section = (Section)i.next();
            
            //unlimited section
            if (section.getLimit()<0) continue;
            
            //new enrollment weight
            double enrlWeight = section.getEnrollmentWeight(enrollment.getRequest()) + getWeight(enrollment.getRequest());
            
            //below limit -> ok
            if (enrlWeight<=section.getLimit()) continue;
            
            //above limit -> compute adepts (current assignments that are not yet conflicting)
            //exclude all conflicts as well
            Vector adepts = new Vector(section.getEnrollments().size());
            for (Iterator j=section.getEnrollments().iterator();j.hasNext();) {
                Enrollment e = (Enrollment)j.next();
                if (e.getRequest().equals(enrollment.getRequest())) continue;
                if (conflicts.contains(e))
                    enrlWeight -= e.getRequest().getWeight();
                else
                    adepts.addElement(e);
            }
            
            //while above limit -> pick an adept and make it conflicting
            while (enrlWeight>section.getLimit()) {
                //no adepts -> enrollment cannot be assigned
                if (adepts.isEmpty()) {
                    conflicts.add(enrollment); break;
                }
                //pick adept, decrease enrollment weight, make conflict
                Enrollment conflict = (Enrollment)ToolBox.random(adepts);
                adepts.remove(conflict);
                enrlWeight -= conflict.getRequest().getWeight();
                conflicts.add(conflict);
            }
        }
    }
    
    /**
     * A given enrollment is conflicting, if there is a section which limit (excluding the 
     * particular request of the given enrollment -- see {@link Section#getEnrollmentWeight(Request)}) 
     * plus the weight computed by {@link SectionLimit#getWeight(Request)} exceeds the section limit.
     * 
     * @param value {@link Enrollment} that is being considered
     * @return true, if there is a section which will exceed its limit when the given
     * enrollment is assigned 
     */
    public boolean inConflict(Value value) {
        //get enrollment
        Enrollment enrollment = (Enrollment)value;
        
        //exclude free time requests
        if (!enrollment.isCourseRequest()) return false;
        
        //for each section
        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
            
            Section section = (Section)i.next();
            
            //unlimited section
            if (section.getLimit()<0) continue;

            //new enrollment weight
            double enrlWeight = section.getEnrollmentWeight(enrollment.getRequest()) + getWeight(enrollment.getRequest());
            
            //above limit -> conflict
            if (enrlWeight>section.getLimit()) return true;
        }
        
        //no conflicting section -> no conflict
        return false;
    }    
    
    public String toString() {
        return "SectioningLimit";
    }
}
