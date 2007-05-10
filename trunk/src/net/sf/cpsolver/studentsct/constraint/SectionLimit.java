package net.sf.cpsolver.studentsct.constraint;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Section;

public class SectionLimit extends GlobalConstraint {
    public static double sMinWeight = 0.0;
    public static double sMaxWeight = 0.0001;
    public static double sRatio = 1.0;
    
    public static double getWeight(Enrollment enrollment) {
        return Math.min(sMinWeight, Math.max(sMaxWeight, sRatio * enrollment.getRequest().getWeight())); 
    }
    
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
            double enrlWeight = section.getEnrollmentWeight(enrollment.getRequest()) + getWeight(enrollment);
            
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
            double enrlWeight = section.getEnrollmentWeight(enrollment.getRequest()) + getWeight(enrollment);
            
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
