package org.cpsolver.ta.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ta.constraints.Student;

public class TeachingRequest extends Variable<TeachingRequest, TeachingAssignment> {
    private Long iAssignmentId;
    private String iName;
    private Map<String, Integer> iLevels = new HashMap<String, Integer>();
    private double iLoad = 0.0;
    private String iLink = null;
    private List<Section> iSections = new ArrayList<Section>();

    public TeachingRequest(long id, String name, int dayCode, int start, int length, String room, String link) {
        super();
        iAssignmentId = id;
        iName = name;
        iSections.add(new Section(id, name, new TimeLocation(dayCode, start, length, 0, 0.0, 0, null, "", null, 0), room, false));
        iLink = (link == null || link.isEmpty() ? null : link);
    }
    
    public TeachingRequest(long id, String name, Collection<Section> sections, String link) {
        super();
        iAssignmentId = id;
        iName = name;
        iSections.addAll(sections);
        iLink = (link == null || link.isEmpty() ? null : link);
    }

    public Long getAssignmentId() {
        return iAssignmentId;
    }

    @Override
    public List<TeachingAssignment> values(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        List<TeachingAssignment> values = super.values(assignment);
        if (values == null) {
            values = new ArrayList<TeachingAssignment>();
            for (Constraint<TeachingRequest, TeachingAssignment> constraint : getModel().constraints()) {
                if (constraint instanceof Student) {
                    Student student = (Student) constraint;
                    if (student.canTeach(this))
                        values.add(new TeachingAssignment(this, student));
                }
            }
            setValues(values);
        }
        return values;
    }

    public String getClassName() {
        return iName;
    }

    @Override
    public String getName() {
        return iName + " " + getSections() + (getLink() == null ? "" : " " + getLink());
    }
    
    public List<Section> getSections() { return iSections; }

    public Map<String, Integer> getLevels() {
        return iLevels;
    }

    public String getLink() {
        return iLink;
    }

    public void setLoad(double load) {
        iLoad = load;
    }

    public double getLoad() {
        return iLoad;
    }

    @Override
    public String toString() {
        String ret = getAssignmentId() + "," + getClassName() + ",";
        for (Iterator<Section> i = getSections().iterator(); i.hasNext(); ) {
            Section section = i.next();
            if (section.hasTime()) ret += section.getTime().getName(true);
            if (i.hasNext()) ret += "-";
        }
        ret += ",";
        for (Iterator<Section> i = getSections().iterator(); i.hasNext(); ) {
            Section section = i.next();
            if (section.hasRoom()) ret += section.getRoom();
            if (i.hasNext()) ret += "-";
        }
        ret += "," + (getLink() == null ? "" : getLink()) + ",\"" + (getLevels().isEmpty() ? "-" : getLevels()) + "\"," + new DecimalFormat("0.##").format(getLoad());
        return ret;
    }

    public boolean sameCourse(TeachingRequest request) {
        return iName.split(" ")[0].equals(request.iName.split(" ")[0]);
    }

    public boolean overlaps(TeachingRequest request) {
        for (Section section: getSections())
            if (section.isOverlapping(request.getSections())) return true;
        return false;
    }
    
    public int share(TeachingRequest request) {
        int ret = 0;
        for (Section section: getSections())
            ret += section.share(request.getSections());
        return ret;
    }

    public boolean isBackToBack(TeachingRequest request) {
        for (Section section: getSections())
            if (section.isBackToBack(request.getSections())) return true;
        return false;
    }

    public boolean isBackToBackSameRoom(TeachingRequest request) {
        for (Section section: getSections())
            if (section.isBackToBackSameRoom(request.getSections())) return true;
        return false;
    }
}
