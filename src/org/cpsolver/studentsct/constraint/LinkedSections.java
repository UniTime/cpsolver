package org.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;


/**
 * Linked sections are sections (of different courses) that should be attended by the
 * same students. If there are multiple sections of the same subpart, one or can be
 * chosen randomly. For instance, if section A1 (of a course A) and section B1 (of a course
 * B) are linked, a student requesting both courses must attend A1 if and only if he
 * also attends B1. 
 * 
 * @author  Tomas Muller
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
public class LinkedSections {
    private Map<Offering, Map<Subpart, Set<Section>>> iSections = new HashMap<Offering, Map<Subpart,Set<Section>>>();
    private boolean iMustBeUsed;
    
    /**
     * Constructor
     * @param sections sections that are to be linked
     */
    public LinkedSections(Section... sections) {
        for (Section section: sections)
            addSection(section);
        
    }
    
    /**
     * Constructor
     * @param sections sections that are to be linked
     */
    public LinkedSections(Collection<Section> sections) {
        for (Section section: sections)
            addSection(section);
    }

    /**
     * Add a section to this link
     * @param section
     */
    private void addSection(Section section) {
        Map<Subpart, Set<Section>> subparts = iSections.get(section.getSubpart().getConfig().getOffering());
        if (subparts == null) {
            subparts = new HashMap<Subpart, Set<Section>>();
            iSections.put(section.getSubpart().getConfig().getOffering(), subparts);
        }
        Set<Section> sections = subparts.get(section.getSubpart());
        if (sections == null) {
            sections = new HashSet<Section>();
            subparts.put(section.getSubpart(), sections);
        }
        sections.add(section);

    }
    
    /**
     * Return offerings of this link
     * @return offerings of this link
     */
    public Set<Offering> getOfferings() { return iSections.keySet(); }
    
    /**
     * Return subpart (or subparts) of an offering of this link
     * @param offering an offering of this link
     * @return subpart (or subparts) of this offering in this link
     */
    public Set<Subpart> getSubparts(Offering offering) { return iSections.get(offering).keySet(); }
    
    /**
     * Return section (or sections) of a subpart of this link
     * @param subpart subpart of this link
     * @return section (or sections) of this subpart in this link
     */
    public Set<Section> getSections(Subpart subpart) { return iSections.get(subpart.getConfig().getOffering()).get(subpart); }
    
    /**
     * Create linked-section constraints for a given student
     */
    private LinkedSectionsConstraint createConstraint(Student student) {
        List<Request> requests = new ArrayList<Request>();
        int nrOfferings = 0;
        requests: for (Request request: student.getRequests()) {
            if (request instanceof CourseRequest) {
                for (Course course: ((CourseRequest)request).getCourses()) {
                    Map<Subpart, Set<Section>> subpartsThisOffering = iSections.get(course.getOffering());
                    if (subpartsThisOffering != null) {
                        requests.add(request);
                        nrOfferings++;
                        continue requests;
                    }
                }
            }
        }
        if (nrOfferings <= 1) return null;
        LinkedSectionsConstraint constraint = new LinkedSectionsConstraint(student, requests);
        student.getRequests().get(0).getModel().addConstraint(constraint);
        return constraint;
    }
    
    /**
     * Create linked-section constraints for this link. A constraint is created for each
     * student that has two or more offerings of this link.
     */
    public void createConstraints() {
        Set<Student> students = new HashSet<Student>();
        for (Offering offering: iSections.keySet())
            for (Course course: offering.getCourses())
                for (Request request: course.getRequests())
                    if (students.add(request.getStudent())) {
                        if (createConstraint(request.getStudent()) != null)
                            request.getStudent().getLinkedSections().add(this);
                    }
    }
    
    /**
     * Compute conflicting enrollments. If the given enrollment contains sections of this link
     * (one for each subpart in {@link LinkedSections#getSubparts(Offering)}), another assignment
     * of this student is in a conflict, if it does not contain the appropriate sections from
     * {@link LinkedSections#getSubparts(Offering)} and {@link LinkedSections#getSections(Subpart)}.
     * 
     * @param assignment current assignment
     * @param enrollment given enrollment 
     * @param conflicts found conflicts are given to this interface, see {@link ConflictHandler#onConflict(Enrollment)}
     */
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, ConflictHandler conflicts) {
        computeConflicts(enrollment, new CurrentAssignment(assignment), conflicts);
    }
    
    /**
     * Compute conflicting enrollments. If the given enrollment contains sections of this link
     * (one for each subpart in {@link LinkedSections#getSubparts(Offering)}), another assignment
     * of this student is in a conflict, if it does not contain the appropriate sections from
     * {@link LinkedSections#getSubparts(Offering)} and {@link LinkedSections#getSections(Subpart)}.
     * 
     * @param enrollment given enrollment
     * @param assignment custom assignment 
     * @param conflicts found conflicts are given to this interface, see {@link ConflictHandler#onConflict(Enrollment)}
     */
    public void computeConflicts(Enrollment enrollment, EnrollmentAssignment assignment, ConflictHandler conflicts) {
        if (enrollment == null || enrollment.getCourse() == null) return;
        if (enrollment.getReservation() != null && enrollment.getReservation().canBreakLinkedSections()) return;
        Map<Subpart, Set<Section>> subparts = iSections.get(enrollment.getCourse().getOffering());
        if (subparts == null || subparts.isEmpty()) return;
        boolean match = false, partial = false;
        for (Section section: enrollment.getSections()) {
            Set<Section> sections = subparts.get(section.getSubpart());
            if (sections != null) {
                if (sections.contains(section))
                    match = true;
                else
                    partial = true;
            }
        }
        boolean full = match && !partial;
        if (isMustBeUsed()) {
            if (!full) { // not full match -> conflict if there is no other linked section constraint with a full match
                // check if there is some other constraint taking care of this case
                boolean hasOtherMatch = false;
                for (LinkedSections other: enrollment.getStudent().getLinkedSections()) {
                    if (other.hasFullMatch(enrollment) && nrSharedOfferings(other) > 1) { hasOtherMatch = true; break; }
                }
                // no other match -> problem
                if (!hasOtherMatch && !conflicts.onConflict(enrollment)) return;
            }
        }
        if (full) { // full match -> check other enrollments
            for (int i = 0; i < enrollment.getStudent().getRequests().size(); i++) {
                Request request = enrollment.getStudent().getRequests().get(i);
                if (request.equals(enrollment.getRequest())) continue; // given enrollment
                Enrollment otherEnrollment = assignment.getEnrollment(request, i);
                if (otherEnrollment == null || otherEnrollment.getCourse() == null) continue; // not assigned or not course request
                if (otherEnrollment.getReservation() != null && otherEnrollment.getReservation().canBreakLinkedSections()) continue;
                Map<Subpart, Set<Section>> otherSubparts = iSections.get(otherEnrollment.getCourse().getOffering());
                if (otherSubparts == null || otherSubparts.isEmpty()) continue; // offering is not in the link
                boolean otherMatch = false, otherPartial = false;
                for (Section section: otherEnrollment.getSections()) {
                    Set<Section> otherSections = otherSubparts.get(section.getSubpart());
                    if (otherSections != null) {
                        if (otherSections.contains(section))
                            otherMatch = true;
                        else
                            otherPartial = true;
                    }
                }
                boolean otherFull = otherMatch && !otherPartial;
                // not full match -> conflict
                if (!otherFull) {
                    // unless there is some other matching distribution for the same offering pair
                    boolean hasOtherMatch = false;
                    for (LinkedSections other: enrollment.getStudent().getLinkedSections()) {
                        if (other.hasFullMatch(enrollment) && other.hasFullMatch(otherEnrollment))
                            { hasOtherMatch = true; break; }
                    }
                    if (!hasOtherMatch && !conflicts.onConflict(otherEnrollment)) return;
                }
            }
        } else { // no or only partial match -> there should be no match in other offerings too
            for (int i = 0; i < enrollment.getStudent().getRequests().size(); i++) {
                Request request = enrollment.getStudent().getRequests().get(i);
                if (request.equals(enrollment.getRequest())) continue; // given enrollment
                Enrollment otherEnrollment = assignment.getEnrollment(request, i);
                if (otherEnrollment == null || otherEnrollment.getCourse() == null) continue; // not assigned or not course request
                if (otherEnrollment.getReservation() != null && otherEnrollment.getReservation().canBreakLinkedSections()) continue;
                Map<Subpart, Set<Section>> otherSubparts = iSections.get(otherEnrollment.getCourse().getOffering());
                if (otherSubparts == null || otherSubparts.isEmpty()) continue; // offering is not in the link
                boolean otherMatch = false, otherPartial = false;
                for (Section section: otherEnrollment.getSections()) {
                    Set<Section> otherSections = otherSubparts.get(section.getSubpart());
                    if (otherSections != null) {
                        if (otherSections.contains(section))
                            otherMatch = true;
                        else
                            otherPartial = true;
                    }
                }
                boolean otherFull = otherMatch && !otherPartial;
                // full match -> conflict
                if (otherFull) {
                    // unless there is some other matching distribution for the same offering pair
                    boolean hasOtherMatch = false;
                    for (LinkedSections other: enrollment.getStudent().getLinkedSections()) {
                        if (other.hasFullMatch(enrollment) && other.hasFullMatch(otherEnrollment))
                            { hasOtherMatch = true; break; }
                    }
                    if (!hasOtherMatch && !conflicts.onConflict(otherEnrollment)) return;
                }
            }
        }
    }
    
    /**
     * Check if the given enrollment fully matches this constraint
     * @param enrollment an enrollment
     * @return true, if there is a full match
     */
    protected boolean hasFullMatch(Enrollment enrollment) {
        if (enrollment == null || enrollment.getCourse() == null) return false; // not assigned or not course request
        Map<Subpart, Set<Section>> subparts = iSections.get(enrollment.getCourse().getOffering());
        if (subparts == null || subparts.isEmpty()) return false; // offering is not in the link
        boolean match = false, partial = false;
        for (Section section: enrollment.getSections()) {
            Set<Section> sections = subparts.get(section.getSubpart());
            if (sections != null) {
                if (sections.contains(section))
                    match = true;
                else
                    partial = true;
            }
        }
        return match && !partial;
    }
    
    /**
     * Number of offerings that are shared with some other linked sections constraint
     * @param other the other constraint
     * @return number of offerings in common
     */
    protected int nrSharedOfferings(LinkedSections other) {
        int shared = 0;
        for (Offering offering: other.getOfferings())
            if (iSections.containsKey(offering)) shared ++;
        return shared;
    }
    
    /**
     * Check for conflicts. If the given enrollment contains sections of this link
     * (one for each subpart in {@link LinkedSections#getSubparts(Offering)}), another assignment
     * of this student is in a conflict, if it does not contain the appropriate sections from
     * {@link LinkedSections#getSubparts(Offering)} and {@link LinkedSections#getSections(Subpart)}.
     * 
     * @param assignment current assignment
     * @param enrollment given enrollment 
     * @return conflicting enrollment
     */
    public Enrollment inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        return inConflict(enrollment, new CurrentAssignment(assignment));
    }
    
    /**
     * Check for conflicts. If the given enrollment contains sections of this link
     * (one for each subpart in {@link LinkedSections#getSubparts(Offering)}), another assignment
     * of this student is in a conflict, if it does not contain the appropriate sections from
     * {@link LinkedSections#getSubparts(Offering)} and {@link LinkedSections#getSections(Subpart)}.
     * 
     * @param enrollment given enrollment
     * @param assignment custom assignment 
     * @return conflicting enrollment
     */
    public Enrollment inConflict(Enrollment enrollment, EnrollmentAssignment assignment) {
        final Toggle<Enrollment> ret = new Toggle<Enrollment>(null);
        computeConflicts(enrollment, assignment, new ConflictHandler() {
            @Override
            public boolean onConflict(Enrollment conflict) {
                ret.set(conflict);
                return false;
            }
        });
        return ret.get();
    }
    
    /**
     * Interface to be able to provide a custom assignment to {@link LinkedSections#computeConflicts(Enrollment, EnrollmentAssignment, ConflictHandler)}
     */
    public static interface EnrollmentAssignment {
        /**
         * Return enrollment of the given request
         * @param request given request
         * @param index index of the request
         * @return an enrollment
         */
        public Enrollment getEnrollment(Request request, int index);
    }
    
    /**
     * Helper interface to process conflicts in {@link LinkedSections#computeConflicts(Enrollment, EnrollmentAssignment, ConflictHandler)}
     */
    public static interface ConflictHandler {
        /**
         * Called when there is a conflict, if false the computation of other conflicts is stopped.
         * @param conflict a conflicting enrollment
         * @return stop the computation when false
         */
        public boolean onConflict(Enrollment conflict);
    }
    
    /**
     * Current assignment -- default for {@link LinkedSections#computeConflicts(Enrollment, EnrollmentAssignment, ConflictHandler)}
     */
    public static class CurrentAssignment implements EnrollmentAssignment {
        protected Assignment<Request, Enrollment> iAssignment;
        
        public CurrentAssignment(Assignment<Request, Enrollment> assignment) {
            iAssignment = assignment;
        }
        
        /**
         * Return {@link Request#getAssignment(Assignment)}
         */
        @Override
        public Enrollment getEnrollment(Request request, int index) {
            return iAssignment.getValue(request);
        }
    }
    
    /**
     * Return whether this constraint must be used
     * @return if true, a pair of linked sections must be used when a student requests both courses
     */
    public boolean isMustBeUsed() { return iMustBeUsed; }

    /**
     * Set whether this constraint must be used
     * @param mustBeUsed if true,  a pair of linked sections must be used when a student requests both courses
     */
    public void setMustBeUsed(boolean mustBeUsed) { iMustBeUsed = mustBeUsed; }
    
    @Override
    public String toString() {
        String sections = "";
        for (Map.Entry<Offering, Map<Subpart, Set<Section>>> e: iSections.entrySet()) {
            sections += (sections.isEmpty() ? "" : "; ") + e.getKey().getName();
            Set<String> ids = new TreeSet<String>();
            for (Map.Entry<Subpart, Set<Section>> f: e.getValue().entrySet()) {
                for (Section s: f.getValue())
                    ids.add(s.getName());
                sections += ":" + ids;
            }
        }
        return "LinkedSections{" + sections + "}";
    }
    
    private static class Toggle<T> {
        private T iValue;
        Toggle(T value) { set(value); }
        void set(T value) { iValue = value; }
        T get() { return iValue; }
    }
    
    /**
     * Linked sections constraint -- to be created for each student that requests two
     * or more offerings of this link
     */
    public class LinkedSectionsConstraint extends Constraint<Request, Enrollment> {
        private Student iStudent;
        
        /**
         * Constructor
         * @param student a student
         * @param requests sub-set of student requests {@link Student#getRequests()} that contains offerings of this link
         */
        protected LinkedSectionsConstraint(Student student, Collection<Request> requests) {
            iStudent = student;
            for (Request request: requests)
                addVariable(request);
        }
        
        /**
         * Return student
         * @return student
         */
        public Student getStudent() { return iStudent; }

        /**
         * Return linked section
         * @return linked sections constraint
         */
        public LinkedSections getLinkedSections() { return LinkedSections.this; }

        /**
         * Compute conflicts using {@link LinkedSections#computeConflicts(Assignment, Enrollment, ConflictHandler)}
         */
        @Override
        public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment value, final Set<Enrollment> conflicts) {
            getLinkedSections().computeConflicts(assignment, value, new ConflictHandler() {
                @Override
                public boolean onConflict(Enrollment conflict) {
                    conflicts.add(conflict);
                    return true;
                }
            });
        }
        
        /**
         * Check consistency using {@link LinkedSections#inConflict(Enrollment, EnrollmentAssignment)}
         */
        @Override
        public boolean isConsistent(Enrollment enrollment, final Enrollment other) {
            return getLinkedSections().inConflict(enrollment, new LinkedSections.EnrollmentAssignment() {
                @Override
                public Enrollment getEnrollment(Request request, int indext) {
                    return (request.equals(other.getRequest()) ? other : null);
                }
            }) == null;
        }
        
        /**
         * Check for conflict using {@link LinkedSections#inConflict(Assignment, Enrollment)}
         */
        @Override
        public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment value) {
            return getLinkedSections().inConflict(assignment, value) != null;
        }
        
        @Override
        public String toString() {
            return getLinkedSections().toString();
        }
    }
}
