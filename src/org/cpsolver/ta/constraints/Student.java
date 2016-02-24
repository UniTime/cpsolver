package org.cpsolver.ta.constraints;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.ta.criteria.BackToBack;
import org.cpsolver.ta.criteria.DiffLink;
import org.cpsolver.ta.criteria.TimeOverlaps;
import org.cpsolver.ta.model.TeachingRequest;
import org.cpsolver.ta.model.Section;
import org.cpsolver.ta.model.TeachingAssignment;

public class Student extends ConstraintWithContext<TeachingRequest, TeachingAssignment, Student.Context> {
    private String iStudentId;
    private List<TimeLocation>[] iAvailable = null;
    private List<String> iPrefs;
    private boolean iGrad;
    private int iB2B;
    private double iMaxLoad;
    private String iLevel;
   
    public Student(String id, List<String> preference, boolean grad, int b2b, double maxLoad, String level) {
        super();
        iStudentId = id;
        iPrefs = preference;
        iGrad = grad;
        iB2B = b2b;
        iMaxLoad = maxLoad;
        iLevel = (level == null || level.trim().isEmpty() ? null : level.trim());
    }
    
    @SuppressWarnings("unchecked")
    public void setNotAvailable(TimeLocation time) {
        if (iAvailable == null) {
            iAvailable = new List[Constants.SLOTS_PER_DAY * Constants.NR_DAYS];
            for (int i = 0; i < iAvailable.length; i++)
                iAvailable[i] = null;
        }
        for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            if (iAvailable[slot] == null)
                iAvailable[slot] = new ArrayList<TimeLocation>(1);
            iAvailable[slot].add(time);
        }
    }

    public boolean isAvailable(int slot) {
        if (iAvailable != null && iAvailable[slot] != null && !iAvailable[slot].isEmpty())
            return false;
        return true;
    }

    public boolean isAvailable(TimeLocation time) {
        if (iAvailable != null) {
            for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                if (iAvailable[slot] != null) {
                    for (TimeLocation p : iAvailable[slot]) {
                        if (time.shareWeeks(p))
                            return false;
                    }
                }
            }
        }
        return true;
    }
    
    public boolean isAvaible(TeachingRequest request) {
        for (Section section: request.getSections()) {
            if (section.hasTime() && !section.isAllowOverlap() && !isAvailable(section.getTime())) return false;
        }
        return true;
    }

    public List<TimeLocation>[] getAvailableArray() {
        return iAvailable;
    }
    
    public List<String> getPreferences() {
        return iPrefs;
    }

    public int getPreference(TeachingRequest request) {
        for (int i = 0; i < iPrefs.size(); i++) {
            if (request.getCourseName().equals(iPrefs.get(i))) {
                return i;
            } else if (iPrefs.get(i).startsWith(request.getCourseName())) {
                for (Section section: request.getSections())
                    if (iPrefs.get(i).equals(request.getCourseName() + " " + section.getSectionName()))
                        return i;
            }
            if ("Grading".equals(iPrefs.get(i)) && request.getName().equals("Grade"))
                return i;
            if ("Proctoring".equals(iPrefs.get(i)) && request.getName().equals("Proctor"))
                return i;
        }
        return -1;
    }

    public int share(TeachingRequest request) {
        if (iAvailable != null) {
            int share = 0;
            for (Section section: request.getSections()) {
                if (!section.hasTime() || !section.isAllowOverlap()) continue;
                for (Enumeration<Integer> e = section.getTime().getSlots(); e.hasMoreElements();) {
                    int slot = e.nextElement();
                    if (iAvailable[slot] != null) {
                        for (TimeLocation p : iAvailable[slot]) {
                            if (section.getTime().shareWeeks(p))
                                share ++;
                        }
                    }
                }
            }
            return share;
        }
        return 0;
    }

    public String getAvailable() {
        String ret = "";
        for (TimeLocation tl: getUnavailability()) {
            if (!ret.isEmpty()) ret += ", ";
            ret += tl.getLongName(false).trim();
        }
        return ret.isEmpty() ? "-" : "[" + ret + "]";
    }
    
    public Set<TimeLocation> getUnavailability() {
        Set<TimeLocation> set = new HashSet<TimeLocation>();
        if (iAvailable != null) {
            for (List<TimeLocation> tl: iAvailable)
                if (tl != null) set.addAll(tl);
        }
        return set;
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
    
    public int getBackToBackPreference() {
        return iB2B;
    }

    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return getContext(assignment).toString();
    }
    
    @Override
    public String toString() {
        String pref = "";
        for (int i = 0; i < 3; i++) {
            pref += (i > 0 ? "," : "") + (i < iPrefs.size() ? iPrefs.get(i) : "");
        }
        return getStudentId() + ",\"" + getAvailable() + "\"," + pref + "," + (isGrad() ? "Yes" : "No") + ","
                + (isBackToBackPreferred() ? "Yes" : isBackToBackDiscouraged() ? "No" : "") + ","
                + (getLevel() == null ? "" : getLevel());
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
    
    public int share(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
        int share = 0;
        if (value.getStudent().equals(this)) {
            for (TeachingAssignment other : value.getStudent().getContext(assignment).getAssignments()) {
                if (other.variable().equals(value.variable()))
                    continue;
                share += value.variable().share(other.variable());
            }
            share += share(value.variable());
        }
        return share;
    }
    
    public int diffLinks(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
        int diff = 0;
        if (value.getStudent().equals(this) && value.variable().getLink() != null && value.variable().getAssignmentId() >= 0 ) {
            String link = value.variable().getLink();
            for (TeachingAssignment other : value.getStudent().getContext(assignment).getAssignments()) {
                if (!other.variable().equals(value.variable()) && other.variable().getLink() != null && other.variable().getAssignmentId() >= 0 && !link.equals(other.variable().getLink()))
                    diff ++;
            }
        }
        return diff;
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
        private int iDiffLinks = 0, iBackToBacks = 0, iTimeOverlaps = 0;
        
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
            
            // update time overlaps
            Criterion<TeachingRequest, TeachingAssignment> overlaps = getModel().getCriterion(TimeOverlaps.class);
            if (overlaps != null) {
                overlaps.inc(assignment, -iTimeOverlaps);
                iTimeOverlaps = countTimeOverlaps();
                overlaps.inc(assignment, iTimeOverlaps);
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
        
        public int countTimeOverlaps() {
            int share = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() < a2.getId())
                        share += a1.variable().share(a2.variable());
                }
                share += share(a1.variable());
            }
            return share;
        }

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
                    + "," + (countDiffLinks() > 0 ? countDiffLinks() : "") + "," + (countTimeOverlaps() > 0 ? countTimeOverlaps() : "") + ass;
        }
    }
}
