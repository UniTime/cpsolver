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
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.instructor.criteria.BackToBack;
import org.cpsolver.instructor.criteria.DifferentLecture;
import org.cpsolver.instructor.criteria.TimeOverlaps;

/**
 * Instructor. An instructor has an id, a name, a teaching preference, a maximal teaching load, a back-to-back preference.
 * It can also have a set of attributes and course and time preferences. Availability is modeled with prohibited time preferences.
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
public class Instructor extends AbstractClassWithContext<TeachingRequest.Variable, TeachingAssignment, Instructor.Context> implements CanInheritContext<TeachingRequest.Variable, TeachingAssignment, Instructor.Context> {
    private List<Attribute> iAttributes = new ArrayList<Attribute>();
    private List<Preference<TimeLocation>> iTimePreferences = new ArrayList<Preference<TimeLocation>>();
    private List<Preference<Course>> iCoursePreferences = new ArrayList<Preference<Course>>();
    private InstructorSchedulingModel iModel;
    private long iInstructorId;
    private String iExternalId;
    private String iName;
    private int iPreference;
    private float iMaxLoad;
    private int iBackToBackPreference;
    
    /**
     * Constructor
     * @param id instructor unique id
     * @param externalId instructor external id
     * @param name instructor name
     * @param preference teaching preference
     * @param maxLoad maximal teaching load
     */
    public Instructor(long id, String externalId, String name, int preference, float maxLoad) {
        iInstructorId = id; iExternalId = externalId; iName = name; iPreference = preference; iMaxLoad = maxLoad;
    }
    
    @Override
    public InstructorSchedulingModel getModel() { return iModel; }
    
    /**
     * Set current model
     * @param model instructional scheduling model
     */
    public void setModel(InstructorSchedulingModel model) { iModel = model; }
    
    /**
     * Instructor unique id that was provided in the constructor
     * @return instructor unique id
     */
    public long getInstructorId() { return iInstructorId; }
    
    /**
     * Has instructor external id?
     * @return true, if the instructor has an external id set
     */
    public boolean hasExternalId() { return iExternalId != null && !iExternalId.isEmpty(); }
    
    /**
     * Instructor external Id that was provided in the constructor
     * @return external id
     */
    public String getExternalId() { return iExternalId; }
    
    /**
     * Has instructor name?
     * @return true, if the instructor name is set
     */
    public boolean hasName() { return iName != null && !iName.isEmpty(); }
    
    /**
     * Instructor name that was provided in the constructor
     * @return instructor name
     */
    public String getName() { return iName != null ? iName : iExternalId != null ? iExternalId : ("I" + iInstructorId); }
    
    /**
     * Set back-to-back preference (only soft preference can be set at the moment)
     * @param backToBackPreference back-to-back preference (e.g., -1 for preferred, 1 for discouraged)
     */
    public void setBackToBackPreference(int backToBackPreference) { iBackToBackPreference = backToBackPreference; }
    
    /**
     * Return back-to-back preference (only soft preference can be set at the moment)
     * @return back-to-back preference (e.g., -1 for preferred, 1 for discouraged)
     */
    public int getBackToBackPreference() { return iBackToBackPreference; }
    
    /**
     * Is back-to-back preferred?
     * @return true if the back-to-back preference is negative
     */
    public boolean isBackToBackPreferred() { return iBackToBackPreference < 0; }
    
    /**
     * Is back-to-back discouraged?
     * @return true if the back-to-back preference is positive
     */
    public boolean isBackToBackDiscouraged() { return iBackToBackPreference > 0; }
    
    /**
     * Instructor unavailability string generated from prohibited time preferences
     * @return comma separated list of times during which the instructor is not available
     */
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
    
    /**
     * Return instructor attributes
     * @return list of instructor attributes
     */
    public List<Attribute> getAttributes() { return iAttributes; }
    
    /**
     * Add instructor attribute
     * @param attribute instructor attribute
     */
    public void addAttribute(Attribute attribute) { iAttributes.add(attribute); }
    
    /**
     * Return instructor attributes of given type
     * @param type attribute type
     * @return attributes of this instructor that are of the given type
     */
    public Set<Attribute> getAttributes(Attribute.Type type) {
        Set<Attribute> attributes = new HashSet<Attribute>();
        for (Attribute attribute: iAttributes)
            if (type.equals(attribute.getType())) attributes.add(attribute);
        return attributes;
    }
    
    /**
     * Return instructor preferences
     * @return list of instructor time preferences
     */
    public List<Preference<TimeLocation>> getTimePreferences() { return iTimePreferences; }
    
    /**
     * Add instructor time preference
     * @param pref instructor time preference
     */
    public void addTimePreference(Preference<TimeLocation> pref) { iTimePreferences.add(pref); }
    
    /**
     * Compute time preference for a given time. This is using the {@link MinMaxPreferenceCombination} for all time preferences that are overlapping with the given time.
     * @param time given time
     * @return computed preference for the given time
     */
    public PreferenceCombination getTimePreference(TimeLocation time) {
        if (iTimePreferences.isEmpty()) return null;
        PreferenceCombination comb = new MinMaxPreferenceCombination();
        for (Preference<TimeLocation> pref: iTimePreferences)
            if (pref.getTarget().hasIntersection(time))
                comb.addPreferenceInt(pref.getPreference());
        return comb;
    }
    
    /**
     * Compute time preference for a given teaching request. This is using the {@link MinMaxPreferenceCombination} for all time preferences that are overlapping with the given teaching request.
     * When a section that allows for overlaps (see {@link Section#isAllowOverlap()}) overlap with a prohibited time preference, this is only counted as strongly discouraged. 
     * @param request teaching request that is being considered
     * @return computed time preference
     */
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

    /**
     * Return course preferences
     * @return list of instructor course preferences
     */
    public List<Preference<Course>> getCoursePreferences() { return iCoursePreferences; }
    
    /**
     * Add course preference
     * @param pref instructor course preference
     */
    public void addCoursePreference(Preference<Course> pref) { iCoursePreferences.add(pref); }
    
    /**
     * Return preference for the given course
     * @param course course that is being considered
     * @return course preference for the given course
     */
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

    /**
     * Return teaching preference
     * @return teaching preference of this instructor
     */
    public int getPreference() { return iPreference; }
    
    /**
     * Maximal load
     * @return maximal load of this instructor
     */
    public float getMaxLoad() { return iMaxLoad; }
    
    /**
     * Check if this instructor can teach the given request. This means that the given request is below the maximal teaching load, 
     * the instructor is available (time preference is not prohibited), the instructor does not prohibit the course (there is no 
     * prohibited course preference for the given course), and the request's instructor preference is also not prohibited.
     * So, the only thing that is not checked are the attribute preferences.
     * @param request teaching request that is being considered
     * @return true, if the instructor can be assigned to the given teaching request
     */
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
        return new Long(iInstructorId).hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Instructor)) return false;
        Instructor i = (Instructor)o;
        return getInstructorId() == i.getInstructorId();
    }
    
    /**
     * Compute time overlaps with instructor availability
     * @param request teaching request that is being considered
     * @return number of slots during which the instructor has a prohibited time preferences that are overlapping with a section of the request that is allowing for overlaps
     */
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
    
    /**
     * Compute time overlaps with instructor availability and other teaching assignments of the instructor
     * @param assignment current assignment
     * @param value teaching assignment that is being considered
     * @return number of overlapping time slots (of the requested assignment) during which the overlaps are allowed
     */
    public int share(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
        int share = 0;
        if (value.getInstructor().equals(this)) {
            for (TeachingAssignment other : value.getInstructor().getContext(assignment).getAssignments()) {
                if (other.variable().equals(value.variable()))
                    continue;
                share += value.variable().getRequest().share(other.variable().getRequest());
            }
            share += share(value.variable().getRequest());
        }
        return share;
    }
    
    /**
     * Compute different common sections of the given teaching assignment and the other assignments of the instructor 
     * @param assignment current assignment
     * @param value teaching assignment that is being considered
     * @return average {@link TeachingRequest#nrSameLectures(TeachingRequest)} between the given and the other existing assignments of the instructor
     */
    public double differentLectures(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
        double same = 0; int count = 0;
        if (value.getInstructor().equals(this)) {
            for (TeachingAssignment other : value.getInstructor().getContext(assignment).getAssignments()) {
                if (other.variable().equals(value.variable()))
                    continue;
                same += value.variable().getRequest().nrSameLectures(other.variable().getRequest());
                count ++;
            }
        }
        return (count == 0 ? 0.0 : (count - same) / count);
    }
    
    /**
     * Compute number of back-to-back assignments (weighted by the preference) of the given teaching assignment and the other assignments of the instructor 
     * @param assignment current assignment
     * @param value teaching assignment that is being considered
     * @param diffRoomWeight different room penalty
     * @param diffTypeWeight different instructional type penalty
     * @return weighted back-to-back preference, using {@link TeachingRequest#countBackToBacks(TeachingRequest, double, double)}
     */
    public double countBackToBacks(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, double diffRoomWeight, double diffTypeWeight) {
        double b2b = 0.0;
        if (value.getInstructor().equals(this) && getBackToBackPreference() != 0) {
            for (TeachingAssignment other : value.getInstructor().getContext(assignment).getAssignments()) {
                if (other.variable().equals(value.variable()))
                    continue;
                if (getBackToBackPreference() < 0) { // preferred
                    b2b += (value.variable().getRequest().countBackToBacks(other.variable().getRequest(), diffRoomWeight, diffTypeWeight) - 1.0) * getBackToBackPreference();
                } else {
                    b2b += value.variable().getRequest().countBackToBacks(other.variable().getRequest(), diffRoomWeight, diffTypeWeight) * getBackToBackPreference();
                }
            }
        }
        return b2b;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public Context createAssignmentContext(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        return new Context(assignment);
    }
    

    @Override
    public Context inheritAssignmentContext(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Context parentContext) {
        return new Context(assignment, parentContext);
    }

    
    /**
     * Instructor Constraint Context. It keeps the list of current assignments of an instructor.
     */
    public class Context implements AssignmentConstraintContext<TeachingRequest.Variable, TeachingAssignment> {
        private HashSet<TeachingAssignment> iAssignments = new HashSet<TeachingAssignment>();
        private int iTimeOverlaps;
        private double iBackToBacks;
        private double iDifferentLectures;
        
        /**
         * Constructor
         * @param assignment current assignment
         */
        public Context(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
            for (TeachingRequest.Variable request: getModel().variables()) {
                TeachingAssignment value = assignment.getValue(request);
                if (value != null && value.getInstructor().equals(getInstructor()))
                    iAssignments.add(value);
            }
            if (!iAssignments.isEmpty())
                updateCriteria(assignment);
        }
        
        /**
         * Constructor
         * @param assignment current assignment
         * @param parentContext parent context
         */
        public Context(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Context parentContext) {
            iAssignments = new HashSet<TeachingAssignment>(parentContext.getAssignments());
            if (!iAssignments.isEmpty())
                updateCriteria(assignment);
        }
        
        /**
         * Instructor
         * @return instructor of this context
         */
        public Instructor getInstructor() { return Instructor.this; }

        @Override
        public void assigned(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
            if (value.getInstructor().equals(getInstructor())) {
                iAssignments.add(value);
                updateCriteria(assignment);
            }
        }
        
        @Override
        public void unassigned(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
            if (value.getInstructor().equals(getInstructor())) {
                iAssignments.remove(value);
                updateCriteria(assignment);
            }
        }
        
        /**
         * Update optimization criteria
         * @param assignment current assignment
         */
        private void updateCriteria(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
            // update back-to-backs
            BackToBack b2b = (BackToBack)getModel().getCriterion(BackToBack.class);
            if (b2b != null) {
                b2b.inc(assignment, -iBackToBacks);
                iBackToBacks = countBackToBackPreference(b2b.getDifferentRoomWeight(), b2b.getDifferentTypeWeight());
                b2b.inc(assignment, iBackToBacks);
            }
            
            // update time overlaps
            Criterion<TeachingRequest.Variable, TeachingAssignment> overlaps = getModel().getCriterion(TimeOverlaps.class);
            if (overlaps != null) {
                overlaps.inc(assignment, -iTimeOverlaps);
                iTimeOverlaps = countTimeOverlaps();
                overlaps.inc(assignment, iTimeOverlaps);
            }
            
            // update same lectures
            Criterion<TeachingRequest.Variable, TeachingAssignment> diff = getModel().getCriterion(DifferentLecture.class);
            if (diff != null) {
                diff.inc(assignment, -iDifferentLectures);
                iDifferentLectures = countDifferentLectures();
                diff.inc(assignment, iDifferentLectures);
            }

        }
        
        /**
         * Current assignments of this instructor
         * @return current teaching assignments
         */
        public Set<TeachingAssignment> getAssignments() { return iAssignments; }
        
        /**
         * Current load of this instructor
         * @return current load
         */
        public float getLoad() {
            float load = 0;
            for (TeachingAssignment assignment : iAssignments)
                load += assignment.variable().getRequest().getLoad();
            return load;
        }
        
        /**
         * If there are classes that allow for overlap, the number of such overlapping slots of this instructor
         * @return current time overlaps (number of overlapping slots)
         */
        public int countTimeOverlaps() {
            int share = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() < a2.getId())
                        share += a1.variable().getRequest().share(a2.variable().getRequest());
                }
                share += getInstructor().share(a1.variable().getRequest());
            }
            return share;
        }

        /**
         * Number of teaching assignments that have a time assignment of this instructor
         * @return current number of teaching assignments that have a time
         */
        public int countAssignmentsWithTime() {
            int ret = 0;
            a1: for (TeachingAssignment a1 : iAssignments) {
                for (Section s1: a1.variable().getSections())
                    if (s1.hasTime()) {
                        ret++; continue a1;
                    }
            }
            return ret;
        }
        
        /**
         * Percentage of common sections that are not same for the instructor (using {@link TeachingRequest#nrSameLectures(TeachingRequest)})
         * @return percentage of pairs of common sections that are not the same
         */
        public double countDifferentLectures() {
            double same = 0;
            int pairs = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() < a2.getId()) {
                        same += a1.variable().getRequest().nrSameLectures(a2.variable().getRequest());
                        pairs++;
                    }
                }
            }
            return (pairs == 0 ? 0.0 : (pairs - same) / pairs);
        }
        
        /**
         * Current back-to-back preference of the instructor (using {@link TeachingRequest#countBackToBacks(TeachingRequest, double, double)})
         * @param diffRoomWeight different room weight
         * @param diffTypeWeight different instructional type weight
         * @return current back-to-back preference
         */
        public double countBackToBackPreference(double diffRoomWeight, double diffTypeWeight) {
            double b2b = 0;
            if (getInstructor().isBackToBackPreferred() || getInstructor().isBackToBackDiscouraged())
                for (TeachingAssignment a1 : iAssignments) {
                    for (TeachingAssignment a2 : iAssignments) {
                        if (a1.getId() >= a2.getId()) continue;
                        if (getInstructor().getBackToBackPreference() < 0) { // preferred
                            b2b += (a1.variable().getRequest().countBackToBacks(a2.variable().getRequest(), diffRoomWeight, diffTypeWeight) - 1.0) * getInstructor().getBackToBackPreference();
                        } else {
                            b2b += a1.variable().getRequest().countBackToBacks(a2.variable().getRequest(), diffRoomWeight, diffTypeWeight) * getInstructor().getBackToBackPreference();
                        }
                    }
                }
            return b2b;
        }
        
        /**
         * Current back-to-back percentage for this instructor
         * @return percentage of assignments that are back-to-back
         */
        public double countBackToBackPercentage() {
            BackToBack c = (BackToBack)getModel().getCriterion(BackToBack.class);
            if (c == null) return 0.0;
            double b2b = 0.0;
            int pairs = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() >= a2.getId()) continue;
                    b2b += a1.variable().getRequest().countBackToBacks(a2.variable().getRequest(), c.getDifferentRoomWeight(), c.getDifferentTypeWeight());
                    pairs ++;
                }
            }
            return (pairs == 0 ? 0.0 : b2b / pairs);
        }
    }
}
