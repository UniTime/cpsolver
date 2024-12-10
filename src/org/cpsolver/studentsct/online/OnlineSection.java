package org.cpsolver.studentsct.online;

import java.util.List;

import org.cpsolver.coursett.model.Placement;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;

/**
 * An online section. A simple extension of the {@link Section} class that allows to set the current section enrollment.
 * This class is particularly useful when a model containing only the given student is constructed (to provide him/her with a schedule or suggestions).
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class OnlineSection extends Section {
    private int iEnrollment = 0;
    private boolean iAlwaysEnabled = false;
    private Integer iDayOfWeekOffset = null;

    public OnlineSection(long id, int limit, String name, Subpart subpart, Placement placement, List<Instructor> instructors, Section parent) {
        super(id, limit, name, subpart, placement, instructors, parent);
    }
 
    @Deprecated
    public OnlineSection(long id, int limit, String name, Subpart subpart, Placement placement, String instructorIds, String instructorNames, Section parent) {
        super(id, limit, name, subpart, placement, instructorIds, instructorNames, parent);
    }
    
    /**
     * Set current enrollment
     * @param enrollment current enrollment
     */
    public void setEnrollment(int enrollment) { iEnrollment = enrollment; }
    
    /**
     * Get current enrollment
     * @return current enrollment
     */
    public int getEnrollment() { return iEnrollment; }
    
    public void setAlwaysEnabled(boolean alwaysEnabled) { iAlwaysEnabled = alwaysEnabled; }
    public boolean isAlwaysEnabled() { return iAlwaysEnabled; }
    
    @Override
    public boolean isEnabled() {
        if (iAlwaysEnabled) return true;
        return super.isEnabled();
    }
    
    @Override
    public boolean isEnabled(Student student) {
        if (iAlwaysEnabled) return true;
        return super.isEnabled(student);
    }
    
    @Override
    protected int getDayOfWeekOffset() {
        if (iDayOfWeekOffset != null) return iDayOfWeekOffset;
        return super.getDayOfWeekOffset();
    }
    public void setDayOfWeekOffset(Integer dayOfWeekOffset) {
        iDayOfWeekOffset = dayOfWeekOffset;
    }
    
}