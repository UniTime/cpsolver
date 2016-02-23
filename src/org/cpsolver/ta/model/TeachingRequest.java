package org.cpsolver.ta.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ta.constraints.Student;

public class TeachingRequest extends Variable<TeachingRequest, TeachingAssignment> {
    private Long iAssignmentId;
    private String iName;
    private int iDayCode, iStartSlot, iLength;
    private String iRoom;
    private Map<String, Integer> iLevels = new HashMap<String, Integer>();
    private double iLoad = 0.0;
    private String iLink = null;

    public TeachingRequest(long id, String name, int dayCode, int start, int length, String room, String link) {
        super();
        iName = name;
        iAssignmentId = id;
        iDayCode = dayCode;
        iStartSlot = start;
        iLength = length;
        iRoom = room;
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
        return iName + (iDayCode == 0 ? "" : " " + getTime()) + (getRoom() == null ? "" : " " + getRoom())
                + (getLink() == null ? "" : " " + getLink());
    }

    public int getStartSlot() {
        return iStartSlot;
    }

    public int getLength() {
        return iLength;
    }

    public int getDayCode() {
        return iDayCode;
    }

    public String getTime() {
        if (iDayCode == 0)
            return "-";
        String ret = "";
        for (int i = 0; i < Constants.DAY_CODES.length; i++)
            if ((iDayCode & Constants.DAY_CODES[i]) != 0)
                ret += TAModel.sDayCodes[i];
        int min = iStartSlot * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
        int h = min / 60;
        int m = min % 60;
        ret += h + (m < 10 ? "0" : "") + m;
        if (iLength > 12) {
            int endmin = (iStartSlot + iLength) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
            int eh = endmin / 60;
            int em = endmin % 60;
            ret += "-" + eh + (em < 10 ? "0" : "") + em;
        }
        return ret;
    }

    public String getRoom() {
        return iRoom;
    }

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
        return getAssignmentId() + "," + getClassName() + "," + getTime() + "," + (getRoom() == null ? "-" : getRoom())
                + "," + (getLink() == null ? "" : getLink()) + ",\"" + (getLevels().isEmpty() ? "-" : getLevels())
                + "\"," + new DecimalFormat("0.##").format(getLoad());
    }

    public boolean sameCourse(TeachingRequest request) {
        return iName.split(" ")[0].equals(request.iName.split(" ")[0]);
    }

    public boolean shareDays(TeachingRequest request) {
        return ((iDayCode & request.iDayCode) != 0);
    }

    public boolean shareHours(TeachingRequest request) {
        return (iStartSlot + iLength > request.iStartSlot) && (request.iStartSlot + request.iLength > iStartSlot);
    }

    public boolean overlaps(TeachingRequest request) {
        return shareDays(request) && shareHours(request);
    }

    public boolean isBackToBack(TeachingRequest request) {
        return shareDays(request)
                && (iStartSlot + iLength == request.iStartSlot || request.iStartSlot + request.iLength == iStartSlot);
    }

    public boolean isBackToBackSameRoom(TeachingRequest request) {
        return isBackToBack(request) && iRoom != null && iRoom.equals(request.iRoom);
    }
}
