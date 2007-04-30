package net.sf.cpsolver.studentsct.constraint;

import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Section;

public abstract class ReservationOnSection extends Reservation {
    private Section iSection = null;
    
    public ReservationOnSection(Section section) {
        super();
        iSection = section;
    }
    
    public Section getSection() {
        return iSection;
    }
    
    public boolean isApplicable(Enrollment enrollment) {
        return enrollment.getAssignments().contains(iSection);
    }
    
    public String toString() {
        return "Reservation on "+iSection.getLongName();
    }
}
