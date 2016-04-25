package org.cpsolver.instructor.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.preference.MinMaxPreferenceCombination;
import org.cpsolver.coursett.preference.PreferenceCombination;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.instructor.constraints.InstructorConstraint;

/**
 * Instructor. This constraint encapsulates an instructor.
 * 
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class Instructor {
    private InstructorConstraint iConstraint;
    private List<Attribute> iAttributes = new ArrayList<Attribute>();
    private List<Preference<TimeLocation>> iTimePreferences = new ArrayList<Preference<TimeLocation>>();
    private List<Preference<Course>> iCoursePreferences = new ArrayList<Preference<Course>>();
    private Long iInstructorId;
    private String iExternalId;
    private String iName;
    private int iPreference;
    private float iMaxLoad;
    private int iBackToBackPreference;
    
    public Instructor(long id, String externalId, String name, int preference, float maxLoad) {
        iInstructorId = id; iExternalId = externalId; iName = name; iPreference = preference; iMaxLoad = maxLoad;
        iConstraint = new InstructorConstraint(this);
    }
    
    public InstructorConstraint getConstraint() { return iConstraint; }
    
    public Long getInstructorId() { return iInstructorId; }
    public boolean hasExternalId() { return iExternalId != null && !iExternalId.isEmpty(); }
    public String getExternalId() { return iExternalId; }
    public boolean hasName() { return iName != null && !iName.isEmpty(); }
    public String getName() { return iName != null ? iName : iExternalId != null ? iExternalId : ("I" + iInstructorId); }
    
    public void setBackToBackPreference(int backToBackPreference) { iBackToBackPreference = backToBackPreference; }
    public int getBackToBackPreference() { return iBackToBackPreference; }
    public boolean isBackToBackPreferred() { return iBackToBackPreference < 0; }
    public boolean isBackToBackDiscouraged() { return iBackToBackPreference > 0; }
    
    public String getAvailable() {
        if (iTimePreferences == null) return "";
        String ret = "";
        for (Preference<TimeLocation> tl: iTimePreferences) {
            if (tl.isProhibited()) {
                if (!ret.isEmpty()) ret += ", ";
                ret += tl.getTarget().getLongName(true).trim();
            }
        }
        return ret.isEmpty() ? "" : ret;
    }
    
    public List<Attribute> getAttributes() { return iAttributes; }
    public void addAttribute(Attribute attribute) { iAttributes.add(attribute); }
    public Set<Attribute> getAttributes(Attribute.Type type) {
        Set<Attribute> attributes = new HashSet<Attribute>();
        for (Attribute attribute: iAttributes)
            if (type.equals(attribute.getType())) attributes.add(attribute);
        return attributes;
    }
    
    public List<Preference<TimeLocation>> getTimePreferences() { return iTimePreferences; }
    public void addTimePreference(Preference<TimeLocation> pref) { iTimePreferences.add(pref); }
    public PreferenceCombination getTimePreference(TimeLocation time) {
        if (iTimePreferences.isEmpty()) return null;
        PreferenceCombination comb = new MinMaxPreferenceCombination();
        for (Preference<TimeLocation> pref: iTimePreferences)
            if (pref.getTarget().hasIntersection(time))
                comb.addPreferenceInt(pref.getPreference());
        return comb;
    }
    public PreferenceCombination getTimePreference(TeachingRequest request) {
        PreferenceCombination comb = new MinMaxPreferenceCombination();
        for (Preference<TimeLocation> pref: iTimePreferences)
            for (Section section: request.getSections())
                if (section.hasTime() && section.getTime().hasIntersection(pref.getTarget())) {
                    if (section.isAllowOverlap() && pref.isProhibited())
                        comb.addPreferenceInt(Constants.sPreferenceLevelStronglyDiscouraged);
                    else
                        comb.addPreferenceInt(pref.getPreference());
                }
        return comb;
    }

    public List<Preference<Course>> getCoursePreferences() { return iCoursePreferences; }
    public void addCoursePreference(Preference<Course> pref) { iCoursePreferences.add(pref); }
    public Preference<Course> getCoursePreference(Course course) {
        boolean hasRequired = false;
        for (Preference<Course> pref: iCoursePreferences)
            if (pref.isRequired()) { hasRequired = true; break; }
        for (Preference<Course> pref: iCoursePreferences)
            if (pref.getTarget().equals(course)) {
                if (hasRequired && !pref.isRequired()) continue;
                return pref;
            }
        if (hasRequired)
            return new Preference<Course>(course, Constants.sPreferenceLevelProhibited);
        return new Preference<Course>(course, Constants.sPreferenceLevelNeutral);
    }

    public int getPreference() { return iPreference; }
    
    public float getMaxLoad() { return iMaxLoad; }
    
    public boolean canTeach(TeachingRequest request) {
        if (request.getLoad() > getMaxLoad())
            return false;
        if (getTimePreference(request).isProhibited())
            return false;
        if (getCoursePreference(request.getCourse()).isProhibited())
            return false;
        if (request.getInstructorPreference(this).isProhibited())
            return false;
        return true;
    }
    
    @Override
    public int hashCode() {
        return (getInstructorId() == null ? getName().hashCode() : getInstructorId().hashCode());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Instructor)) return false;
        Instructor i = (Instructor)o;
        return getInstructorId() != null ? getInstructorId().equals(i.getInstructorId()) : getExternalId() != null ? getExternalId().equals(i.getExternalId()) : getName().equals(i.getName());
    }
    
    public int share(TeachingRequest request) {
        int share = 0;
        for (Section section: request.getSections()) {
            if (!section.hasTime() || !section.isAllowOverlap()) continue;
            for (Preference<TimeLocation> pref: iTimePreferences)
                if (pref.isProhibited() && section.getTime().shareWeeks(pref.getTarget()))
                    share += section.getTime().nrSharedDays(pref.getTarget()) * section.getTime().nrSharedHours(pref.getTarget());
        }
        return share;
    }
    
    public int share(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
        int share = 0;
        if (value.getInstructor().equals(this)) {
            for (TeachingAssignment other : value.getInstructor().getConstraint().getContext(assignment).getAssignments()) {
                if (other.variable().equals(value.variable()))
                    continue;
                share += value.variable().share(other.variable());
            }
            share += share(value.variable());
        }
        return share;
    }
    
    public double differentLectures(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
        double same = 0; int count = 0;
        if (value.getInstructor().equals(this)) {
            for (TeachingAssignment other : value.getInstructor().getConstraint().getContext(assignment).getAssignments()) {
                if (other.variable().equals(value.variable()))
                    continue;
                same += value.variable().nrSameLectures(other.variable());
                count ++;
            }
        }
        return (count == 0 ? 0.0 : (count - same) / count);
    }
    
    public double countBackToBacks(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, double diffRoomWeight, double diffTypeWeight) {
        double b2b = 0.0;
        if (value.getInstructor().equals(this) && getBackToBackPreference() != 0) {
            for (TeachingRequest other : value.getInstructorConstraint().assignedVariables(assignment)) {
                if (other.equals(value.variable()))
                    continue;
                if (assignment.getValue(other).getInstructor().equals(value.getInstructor())) {
                    if (getBackToBackPreference() < 0) { // preferred
                        b2b += (value.variable().countBackToBacks(other, diffRoomWeight, diffTypeWeight) - 1.0) * getBackToBackPreference();
                    } else {
                        b2b += value.variable().countBackToBacks(other, diffRoomWeight, diffTypeWeight) * getBackToBackPreference();
                    }
                }
            }
        }
        return b2b;
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
