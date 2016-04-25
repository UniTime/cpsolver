package org.cpsolver.instructor.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.preference.PreferenceCombination;
import org.cpsolver.coursett.preference.SumPreferenceCombination;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.instructor.constraints.InstructorConstraint;

public class TeachingRequest extends Variable<TeachingRequest, TeachingAssignment> {
    private Long iRequestId;
    private Course iCourse;
    private float iLoad;
    private List<Section> iSections = new ArrayList<Section>();
    private List<Preference<Attribute>> iAttributePreferences = new ArrayList<Preference<Attribute>>();
    private List<Preference<Instructor>> iInstructorPreferences = new ArrayList<Preference<Instructor>>();

    public TeachingRequest(long requestId, Course course, float load, Collection<Section> sections) {
        super();
        iRequestId = requestId;
        iCourse = course;
        iLoad = load;
        iSections.addAll(sections);
    }

    public Long getRequestId() {
        return iRequestId;
    }

    @Override
    public List<TeachingAssignment> values(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        List<TeachingAssignment> values = super.values(assignment);
        if (values == null) {
            values = new ArrayList<TeachingAssignment>();
            for (Constraint<TeachingRequest, TeachingAssignment> constraint: getModel().constraints()) {
                if (constraint instanceof InstructorConstraint) {
                    InstructorConstraint ic = (InstructorConstraint) constraint;
                    Instructor instructor = ic.getInstructor();
                    if (instructor.canTeach(this)) {
                        PreferenceCombination attributePref = getAttributePreference(instructor);
                        if (attributePref.isProhibited()) continue;
                        values.add(new TeachingAssignment(this, instructor, attributePref.getPreferenceInt()));
                    }
                }
            }
            setValues(values);
        }
        return values;
    }
    
    public List<Preference<Attribute>> getAttributePreferences() { return iAttributePreferences; }
    public void addAttributePreference(Preference<Attribute> pref) { iAttributePreferences.add(pref); }
    protected int getAttributePreference(Instructor instructor, Attribute.Type type) {
        Set<Attribute> attributes = instructor.getAttributes(type);
        boolean hasReq = false, hasPref = false, needReq = false;
        PreferenceCombination ret = new SumPreferenceCombination();
        for (Preference<Attribute> pref: iAttributePreferences) {
            if (!type.equals(pref.getTarget().getType())) continue;
            if (pref.isRequired()) needReq = true;
            if (attributes.contains(pref.getTarget())) {
                if (pref.isProhibited()) return Constants.sPreferenceLevelProhibited;
                else if (pref.isRequired()) hasReq = true;
                else ret.addPreferenceInt(pref.getPreference());
                hasPref = true;
            } else {
                if (pref.isRequired() && type.isConjunctive()) return Constants.sPreferenceLevelProhibited;
            }
        }
        if (needReq && !hasReq) return Constants.sPreferenceLevelProhibited;
        if (type.isRequired() && !hasPref) return Constants.sPreferenceLevelProhibited;
        return ret.getPreferenceInt();
    }
    public PreferenceCombination getAttributePreference(Instructor instructor) {
        PreferenceCombination preference = new SumPreferenceCombination();
        for (Attribute.Type type: ((InstructorSchedulingModel)getModel()).getAttributeTypes())
            preference.addPreferenceInt(getAttributePreference(instructor, type));
        return preference;
    }

    public List<Preference<Instructor>> getInstructorPreferences() { return iInstructorPreferences; }
    public void addInstructorPreference(Preference<Instructor> pref) { iInstructorPreferences.add(pref); }
    public Preference<Instructor> getInstructorPreference(Instructor instructor) {
        boolean hasRequired = false;
        for (Preference<Instructor> pref: iInstructorPreferences)
            if (pref.isRequired()) { hasRequired = true; break; }
        for (Preference<Instructor> pref: iInstructorPreferences)
            if (pref.getTarget().equals(instructor)) {
                if (hasRequired && !pref.isRequired()) continue;
                return pref;
            }
        if (hasRequired)
            return new Preference<Instructor>(instructor, Constants.sPreferenceLevelProhibited);
        return new Preference<Instructor>(instructor, Constants.sPreferenceLevelNeutral);
    }
    
    public Course getCourse() {
        return iCourse;
    }

    @Override
    public String getName() {
        return iCourse.getCourseName() + " " + getSections();
    }
    
    public List<Section> getSections() { return iSections; }

    public float getLoad() { return iLoad; }
    public void setLoad(float load) { iLoad = load; }

    @Override
    public String toString() {
        return getName();
    }
    
    public boolean sameCommon(TeachingRequest request) {
        for (Section section: getSections())
            if (section.isCommon() && !request.getSections().contains(section))
                return false;
        return true;
    }
    
    public double nrSameLectures(TeachingRequest request) {
        if (!sameCourse(request)) return 0.0;
        double same = 0; int common = 0;
        for (Section section: getSections())
            if (section.isCommon()) {
                common ++;
                if (request.getSections().contains(section)) same++;
            }
        return (common == 0 ? 0.0 : same / common);
    }

    public boolean sameCourse(TeachingRequest request) {
        return getCourse().equals(request.getCourse());
    }

    public boolean overlaps(TeachingRequest request) {
        for (Section section: getSections()) {
            if (section.isAllowOverlap() || section.getTime() == null || request.getSections().contains(section)) continue;
            for (Section other: request.getSections()) {
                if (other.isAllowOverlap() || other.getTime() == null || getSections().contains(other)) continue;
                if (section.getTime().hasIntersection(other.getTime())) return true;
            }
        }
        return false;
    }
    
    public int share(TeachingRequest request) {
        int ret = 0;
        for (Section section: getSections())
            ret += section.share(request.getSections());
        return ret;
    }
    
    public int share(TimeLocation time) {
        int ret = 0;
        for (Section section: getSections())
            ret += section.share(time);
        return ret;
    }

    public double countBackToBacks(TeachingRequest request, double diffRoomWeight, double diffTypeWeight) {
        double b2b = 0.0;
        int count = 0;
        for (Section section: getSections()) {
            if (!section.isCommon() || !sameCourse(request) || !request.getSections().contains(section)) {
                b2b += section.countBackToBacks(request.getSections(), diffRoomWeight, diffTypeWeight);
                count ++;
            }
        }
        return (count == 0 ? 0.0 : b2b / count);
    }
}
