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
    public static double sDelta = 0.5;
    
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
            double enrlWeight = section.getEnrollmentWeight(enrollment.getRequest()) + enrollment.getRequest().getWeight();
            
            //below limit -> ok
            if (enrlWeight-sDelta<=section.getLimit()) continue;
            
            //exclude all conflicts
            for (Iterator j=conflicts.iterator();j.hasNext();) {
                Enrollment conflict = (Enrollment)j.next();
                if (conflict.getRequest().equals(enrollment.getRequest()))
                    continue;
                if (conflict.getAssignments().contains(section))
                    enrlWeight -= conflict.getRequest().getWeight();
            }
            
            //below limit -> ok
            if (enrlWeight-sDelta<=section.getLimit()) continue;

            //above limit -> compute adepts (current assignments that are not yet conflicting)
            Vector adepts = new Vector();
            for (Iterator j=section.getEnrollments().iterator();j.hasNext();) {
                Enrollment e = (Enrollment)j.next();
                if (conflicts.contains(e) || e.getRequest().equals(enrollment.getRequest()))
                    continue;
                adepts.addElement(e);
            }
            
            //while above limit -> pick an adept and make it conflicting
            while (enrlWeight-sDelta>section.getLimit()) {
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
            double enrlWeight = section.getEnrollmentWeight(enrollment.getRequest()) + enrollment.getRequest().getWeight();
            
            //above limit -> conflict
            if (enrlWeight-sDelta>section.getLimit()) return true;
        }
        
        //no conflicting section -> no conflict
        return false;
    }    
    
    public String toString() {
        return "SectioningLimit";
    }
}
