package net.sf.cpsolver.studentsct.constraint;

import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Section;

public class SectionLimit extends GlobalConstraint {

    public void computeConflicts(Value value, Set conflicts) {
        Enrollment enrollment = (Enrollment)value;
        if (!enrollment.isCourseRequest()) return;
        Enrollment current = (Enrollment)enrollment.variable().getAssignment();
        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (section.getEnrollments().size()<section.getLimit()) continue;
            if (current!=null && current.getAssignments().contains(section)) continue;
            boolean hasConflict = false;
            for (Iterator j=conflicts.iterator();j.hasNext();) {
                Enrollment conflict = (Enrollment)j.next();
                if (conflict.getAssignments().contains(section)) {
                    hasConflict = true; break;
                }
            }
            if (hasConflict) continue;
            if (section.getEnrollments().isEmpty())
                conflicts.add(enrollment);
            else
                conflicts.add(ToolBox.random(section.getEnrollments()));
        }
    }
}
