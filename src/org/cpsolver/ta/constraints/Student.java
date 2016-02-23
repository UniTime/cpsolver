package org.cpsolver.ta.constraints;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.ta.criteria.BackToBack;
import org.cpsolver.ta.criteria.DiffLink;
import org.cpsolver.ta.model.TeachingRequest;
import org.cpsolver.ta.model.TAModel;
import org.cpsolver.ta.model.TeachingAssignment;

public class Student extends ConstraintWithContext<TeachingRequest, TeachingAssignment, Student.Context> {
    private String iStudentId;
    private boolean[] iAvailable;
    private List<String> iPrefs;
    private boolean iGrad;
    private int iB2B;
    private double iMaxLoad;
    private String iLevel;
   
    public Student(String id, boolean[] available, List<String> preference, boolean grad, int b2b, double maxLoad, String level) {
        super();
        iStudentId = id;
        iAvailable = available;
        iPrefs = preference;
        iGrad = grad;
        iB2B = b2b;
        iMaxLoad = maxLoad;
        iLevel = (level == null || level.trim().isEmpty() ? null : level.trim());
    }

    public int getPreference(TeachingRequest request) {
        for (int i = 0; i < iPrefs.size(); i++) {
            if (request.getClassName().equals(iPrefs.get(i)))
                return i;
            if ("Grading".equals(iPrefs.get(i)) && request.getName().equals("Grade"))
                return i;
            if ("Proctoring".equals(iPrefs.get(i)) && request.getName().equals("Proctor"))
                return i;
        }
        return -1;
    }

    public boolean isAvaible(TeachingRequest request) {
        if (request.getDayCode() == 0)
            return true;
        for (int i = 0; i < 5; i++)
            if ((Constants.DAY_CODES[i] & request.getDayCode()) != 0) {
                int start = (request.getStartSlot() - 90) / 12;
                int end = (request.getStartSlot() + request.getLength() - 91) / 12;
                for (int time = start; time <= end; time++)
                    if (time >= 0 && time < 10 && !iAvailable[10 * i + time])
                        return false;
            }
        return true;
    }

    public String getAvailable() {
        String ret = "";
        for (int d = 0; d < 5; d++) {
            int f = -1;
            for (int t = 0; t < 10; t++) {
                if (iAvailable[10 * d + t]) {
                    if (f < 0)
                        f = t;
                } else {
                    if (f >= 0) {
                        if (!ret.isEmpty())
                            ret += ", ";
                        ret += TAModel.sDayCodes[d] + (7 + f) + "30-" + (7 + t) + "20";
                        f = -1;
                    }
                }
            }
            if (f >= 0) {
                if (!ret.isEmpty())
                    ret += ", ";
                ret += TAModel.sDayCodes[d] + (f == 0 ? "" : (7 + f) + "30-1720");
                f = -1;
            }
        }
        return ret.isEmpty() ? "-" : "[" + ret + "]";
    }

    public String getStudentId() {
        return iStudentId;
    }

    public String getLevel() {
        return iLevel;
    }

    public boolean isGrad() {
        return iGrad;
    }

    public boolean canTeach(TeachingRequest request) {
        if (!isAvaible(request))
            return false;
        if (request.getLoad() > getMaxLoad())
            return false;
        if (getLevel() != null && request.getLevels().containsKey(getLevel()))
            return true;
        if (request.getLevels().isEmpty())
            return true;
        // if (getPreference(request) >= 0) return true;
        return false;
    }

    public double getMaxLoad() {
        return iMaxLoad;
    }

    @Override
    public void computeConflicts(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (value.getStudent().equals(this)) {
            Context context = getContext(assignment);
            
            // Check availability
            if (!isAvaible(value.variable())) {
                conflicts.add(value);
                return;
            }

            // Check for overlaps
            for (TeachingAssignment ta : context.getAssignments()) {
                if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                    continue;

                if (ta.variable().overlaps(value.variable()))
                    conflicts.add(ta);
            }

            // Same course
            if (value.variable().getId() >= 0) {
                for (TeachingAssignment ta : context.getAssignments()) {
                    if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                        continue;

                    if (ta.variable().getId() >= 0 && !ta.variable().sameCourse(value.variable()))
                        conflicts.add(ta);
                }
            }

            // Check load
            double load = value.variable().getLoad();
            List<TeachingAssignment> adepts = new ArrayList<TeachingAssignment>();
            for (TeachingAssignment ta : context.getAssignments()) {
                if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                    continue;

                adepts.add(ta);
                load += ta.variable().getLoad();
            }
            while (load > getMaxLoad()) {
                if (adepts.isEmpty()) {
                    conflicts.add(value);
                    break;
                }
                TeachingAssignment conflict = ToolBox.random(adepts);
                load -= conflict.variable().getLoad();
                adepts.remove(conflict);
                conflicts.add(conflict);
            }
        }
    }

    public boolean isBackToBackPreferred() {
        return iB2B == 1;
    }

    public boolean isBackToBackDiscouraged() {
        return iB2B == -1;
    }

    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return getContext(assignment).toString();
    }

    public int backToBack(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
        int b2b = 0;
        if (value.getStudent().equals(this) && (isBackToBackPreferred() || isBackToBackDiscouraged())) {
            for (TeachingRequest other : value.getStudent().assignedVariables(assignment)) {
                if (other.equals(value.variable()))
                    continue;
                if (assignment.getValue(other).getStudent().equals(value.getStudent()) && value.variable().isBackToBack(other)) {
                    b2b += (isBackToBackPreferred() ? value.variable().isBackToBackSameRoom(other) ? -4 : -1 : 1);
                }
            }
        }
        return b2b;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Student)) return false;
        return getStudentId().equals(((Student) o).getStudentId());
    }

    @Override
    public int hashCode() {
        return getStudentId().hashCode();
    }
    
    @Override
    public Context createAssignmentContext(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return new Context(assignment);
    }

    public class Context implements AssignmentConstraintContext<TeachingRequest, TeachingAssignment> {
        private HashSet<TeachingAssignment> iAssignments = new HashSet<TeachingAssignment>();
        private int iDiffLinks = 0, iBackToBacks = 0;
        
        public Context(Assignment<TeachingRequest, TeachingAssignment> assignment) {
            for (TeachingRequest request: variables()) {
                TeachingAssignment value = assignment.getValue(request);
                if (value != null)
                    iAssignments.add(value);
            }
            if (!iAssignments.isEmpty())
                updateCriteria(assignment);
        }

        @Override
        public void assigned(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
            if (value.getStudent().equals(Student.this)) {
                iAssignments.add(value);
                updateCriteria(assignment);
            }
        }
        
        @Override
        public void unassigned(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
            if (value.getStudent().equals(Student.this)) {
                iAssignments.remove(value);
                updateCriteria(assignment);
            }
        }
        
        private void updateCriteria(Assignment<TeachingRequest, TeachingAssignment> assignment) {
            // update back-to-backs
            Criterion<TeachingRequest, TeachingAssignment> b2b = getModel().getCriterion(BackToBack.class);
            if (b2b != null) {
                b2b.inc(assignment, -iBackToBacks);
                iBackToBacks = countBackToBackPreference();
                b2b.inc(assignment, iBackToBacks);
            }

            // update diff links
            Criterion<TeachingRequest, TeachingAssignment> diffLink = getModel().getCriterion(DiffLink.class);
            if (diffLink != null) {
                diffLink.inc(assignment, -iDiffLinks);
                iDiffLinks = countDiffLinks();
                diffLink.inc(assignment, iDiffLinks);
            }
        }
        
        public Set<TeachingAssignment> getAssignments() { return iAssignments; }
        
        public double getLoad() {
            double load = 0;
            for (TeachingAssignment assignment : iAssignments)
                load += assignment.variable().getLoad();
            return load;
        }
        
        public int countBackToBackPreference() {
            int b2b = 0;
            if (isBackToBackPreferred() || isBackToBackDiscouraged())
                for (TeachingAssignment a1 : iAssignments) {
                    for (TeachingAssignment a2 : iAssignments) {
                        if (a1.getId() < a2.getId() && a1.variable().isBackToBack(a2.variable()))
                            b2b += (isBackToBackPreferred() ? a1.variable().isBackToBackSameRoom(a1.variable()) ? -4 : -1 : 1);
                    }
                }
            return b2b;
        }

        public int countDiffLinks() {
            Set<String> links = new HashSet<String>();
            for (TeachingAssignment assignment : iAssignments)
                if (assignment.variable().getLink() != null)
                    links.add(assignment.variable().getLink());
            return Math.max(0, links.size() - 1);
        }
        
        public int countBackToBacks() {
            int b2b = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() < a2.getId() && a1.variable().isBackToBack(a2.variable()))
                        b2b++;
                }
            }
            return b2b;
        }

        public int countAssignmentsWithTime() {
            int ret = 0;
            for (TeachingAssignment a1 : iAssignments) {
                if (a1.variable().getDayCode() != 0)
                    ret++;
            }
            return ret;
        }
        
        @Override
        public String toString() {
            String pref = "";
            for (int i = 0; i < 3; i++) {
                pref += (i > 0 ? "," : "") + (i < iPrefs.size() ? iPrefs.get(i) : "");
            }
            String ass = "";
            double level = 0.0, preference = 0.0;
            for (TeachingAssignment ta : iAssignments) {
                ass += "," + ta.toString();
                Integer l = ta.variable().getLevels().get(getLevel());
                if (l != null)
                    level += l;
                switch (getPreference(ta.variable())) {
                    case 0:
                        preference += 1.0;
                        break;
                    case 1:
                        preference += 0.8;
                        break;
                    case 3:
                        preference += 0.5;
                        break;
                }
            }
            return getStudentId() + ",\"" + getAvailable() + "\"," + pref + "," + (isGrad() ? "Yes" : "No") + ","
                    + (isBackToBackPreferred() ? "Yes" : isBackToBackDiscouraged() ? "No" : "") + ","
                    + (getLevel() == null ? "" : getLevel()) + ","
                    + new DecimalFormat("0.00").format(getLoad()) + ","
                    + (iAssignments.isEmpty() ? "" : new DecimalFormat("0.0").format(level / iAssignments.size())) + ","
                    + (iAssignments.isEmpty() ? "" : new DecimalFormat("0.0").format(100.0 * preference / iAssignments.size())) + ","
                    + (countAssignmentsWithTime() <= 1 ? "" : isBackToBackPreferred() ? new DecimalFormat("0.0").format(100.0 * countBackToBacks() / (countAssignmentsWithTime() - 1))
                            : isBackToBackDiscouraged() ? new DecimalFormat("0.0").format(100.0 - 100.0 * countBackToBacks() / (countAssignmentsWithTime() - 1)) : "")
                    + "," + (countDiffLinks() > 0 ? countDiffLinks() : "") + ass;
        }
    }
}
