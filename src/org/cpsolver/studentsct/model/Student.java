package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.List;

import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.constraint.LinkedSections;



/**
 * Representation of a student. Each student contains id, and a list of
 * requests. <br>
 * <br>
 * Last-like semester students are mark as dummy. Dummy students have lower
 * value and generally should not block "real" students from getting requested
 * courses. <br>
 * <br>
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
public class Student implements Comparable<Student> {
    private long iId;
    private String iExternalId = null, iName = null;
    private boolean iDummy = false;
    private List<Request> iRequests = new ArrayList<Request>();
    private List<AcademicAreaCode> iAcadAreaClassifs = new ArrayList<AcademicAreaCode>();
    private List<AcademicAreaCode> iMajors = new ArrayList<AcademicAreaCode>();
    private List<AcademicAreaCode> iMinors = new ArrayList<AcademicAreaCode>();
    private List<AreaClassificationMajor> iACM = new ArrayList<AreaClassificationMajor>();
    private List<LinkedSections> iLinkedSections = new ArrayList<LinkedSections>();
    private String iStatus = null;
    private Long iEmailTimeStamp = null;
    private List<Unavailability> iUnavailabilities = new ArrayList<Unavailability>();
    private boolean iNeedShortDistances = false;
    private boolean iAllowDisabled = false;
    private Float iMaxCredit = null;

    /**
     * Constructor
     * 
     * @param id
     *            student unique id
     */
    public Student(long id) {
        iId = id;
    }

    /**
     * Constructor
     * 
     * @param id
     *            student unique id
     * @param dummy
     *            dummy flag
     */
    public Student(long id, boolean dummy) {
        iId = id;
        iDummy = dummy;
    }

    /** Student unique id 
     * @return student unique id
     **/
    public long getId() {
        return iId;
    }

    /** Set student unique id 
     * @param id student unique id
     **/
    public void setId(long id) {
        iId = id;
    }

    /** Student's course and free time requests 
     * @return student requests
     **/
    public List<Request> getRequests() {
        return iRequests;
    }

    /** Number of requests (alternative requests are ignored) 
     * @return number of non alternative student requests
     **/
    public int nrRequests() {
        int ret = 0;
        for (Request r : getRequests()) {
            if (!r.isAlternative())
                ret++;
        }
        return ret;
    }

    /** Number of alternative requests 
     * @return number of alternative student requests 
     **/
    public int nrAlternativeRequests() {
        int ret = 0;
        for (Request r : getRequests()) {
            if (r.isAlternative())
                ret++;
        }
        return ret;
    }

    /**
     * True if the given request can be assigned to the student. A request
     * cannot be assigned to a student when the student already has the desired
     * number of requests assigned (i.e., number of non-alternative course
     * requests).
     * @param assignment current assignment
     * @param request given request of this student
     * @return true if the given request can be assigned
     **/
    public boolean canAssign(Assignment<Request, Enrollment> assignment, Request request) {
        if (request.isAssigned(assignment))
            return true;
        int alt = 0;
        float credit = 0f;
        boolean found = false;
        for (Request r : getRequests()) {
            if (r.equals(request))
                found = true;
            boolean assigned = (r.isAssigned(assignment) || r.equals(request));
            boolean course = (r instanceof CourseRequest);
            boolean waitlist = (course && ((CourseRequest) r).isWaitlist());
            if (r.isAlternative()) {
                if (assigned || (!found && waitlist))
                    alt--;
            } else {
                if (course && !waitlist && !assigned)
                    alt++;
            }
            if (r.equals(request))
                credit += r.getMinCredit();
            else {
                Enrollment e = r.getAssignment(assignment);
                if (e != null) credit += e.getCredit();
            }
        }
        return (alt >= 0 && credit <= getMaxCredit());
    }

    /**
     * True if the student has assigned the desired number of requests (i.e.,
     * number of non-alternative course requests).
     * @param assignment current assignment
     * @return true if this student has a complete schedule
     */
    public boolean isComplete(Assignment<Request, Enrollment> assignment) {
        int nrRequests = 0;
        int nrAssignedRequests = 0;
        float credit = 0f;
        Float minCredit = null;
        for (Request r : getRequests()) {
            if (!(r instanceof CourseRequest))
                continue; // ignore free times
            if (!r.isAlternative())
                nrRequests++;
            if (r.isAssigned(assignment))
                nrAssignedRequests++;
            Enrollment e = r.getAssignment(assignment);
            if (e != null) {
                credit += e.getCredit();
            } else if (r instanceof CourseRequest) {
                minCredit = (minCredit == null ? r.getMinCredit() : Math.min(minCredit, r.getMinCredit()));
            }
        }
        return nrAssignedRequests == nrRequests || credit + (minCredit == null ? 0f : minCredit.floatValue()) > getMaxCredit();
    }

    /** Number of assigned COURSE requests 
     * @param assignment current assignment
     * @return number of assigned course requests
     **/
    public int nrAssignedRequests(Assignment<Request, Enrollment> assignment) {
        int nrAssignedRequests = 0;
        for (Request r : getRequests()) {
            if (!(r instanceof CourseRequest))
                continue; // ignore free times
            if (r.isAssigned(assignment))
                nrAssignedRequests++;
        }
        return nrAssignedRequests;
    }

    @Override
    public String toString() {
        return (isDummy() ? "D" : "") + "S[" + getId() + "]";
    }

    /**
     * Student's dummy flag. Dummy students have lower value and generally
     * should not block "real" students from getting requested courses.
     * @return true if projected student
     */
    public boolean isDummy() {
        return iDummy;
    }

    /**
     * Set student's dummy flag. Dummy students have lower value and generally
     * should not block "real" students from getting requested courses.
     * @param dummy projected student
     */
    public void setDummy(boolean dummy) {
        iDummy = dummy;
    }

    /**
     * List of academic area - classification codes ({@link AcademicAreaCode})
     * for the given student
     * @return list of academic area abbreviation &amp; classification code pairs
     */
    public List<AcademicAreaCode> getAcademicAreaClasiffications() {
        return iAcadAreaClassifs;
    }

    /**
     * List of major codes ({@link AcademicAreaCode}) for the given student
     * @return list of academic area abbreviation &amp; major code pairs
     */
    public List<AcademicAreaCode> getMajors() {
        return iMajors;
    }

    /**
     * List of major codes ({@link AcademicAreaCode}) for the given student
     * @return list of academic area abbreviation &amp; minor code pairs
     */
    public List<AcademicAreaCode> getMinors() {
        return iMinors;
    }
    
    /**
     * List of academic area, classification, and major codes ({@link AreaClassificationMajor}) for the given student
     * @return list of academic area, classification, and major codes
     */
    public List<AreaClassificationMajor> getAreaClassificationMajors() {
        return iACM;
    }

    /**
     * Compare two students for equality. Two students are considered equal if
     * they have the same id.
     */
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Student))
            return false;
        return getId() == ((Student) object).getId() && isDummy() == ((Student) object).isDummy();
    }

    /**
     * Hash code (base only on student id)
     */
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32));
    }
    
    /**
     * Count number of free time slots overlapping with the given enrollment
     * @param enrollment given enrollment
     * @return number of slots overlapping with a free time request
     */
    public int countFreeTimeOverlaps(Enrollment enrollment) {
        if (!enrollment.isCourseRequest()) return 0;
        int ret = 0;
        for (Section section: enrollment.getSections()) {
            TimeLocation time = section.getTime();
            if (time != null)
                ret += countFreeTimeOverlaps(time);
        }
        return ret;
    }
    
    /**
     * Count number of free time slots overlapping with the given time
     * @param time given time
     * @return number of time slots overlapping with a free time request
     */
    public int countFreeTimeOverlaps(TimeLocation time) {
        int ret = 0;
        for (Request r: iRequests) {
            if (r instanceof FreeTimeRequest) {
                TimeLocation freeTime = ((FreeTimeRequest)r).getTime();
                if (time.hasIntersection(freeTime))
                    ret += freeTime.nrSharedHours(time) * freeTime.nrSharedDays(time);
            }
        }
        return ret;
    }
    
    /**
     * Get student external id
     * @return student external unique id
     */
    public String getExternalId() { return iExternalId; }
    /**
     * Set student external id
     * @param externalId student external id
     */
    public void setExternalId(String externalId) { iExternalId = externalId; }

    /**
     * Get student name
     * @return student name
     */
    public String getName() { return iName; }
    /**
     * Set student name
     * @param name student name
     */
    public void setName(String name) { iName = name; }
    
    /**
     * Linked sections of this student
     * @return linked sections of this student
     */
    public List<LinkedSections> getLinkedSections() { return iLinkedSections; }
    
    /**
     * Get student status (online sectioning only)
     * @return student sectioning status
     */
    public String getStatus() { return iStatus; }
    /**
     * Set student status
     * @param status student sectioning status
     */
    public void setStatus(String status) { iStatus = status; }
    
    /**
     * Get last email time stamp (online sectioning only)
     * @return student email time stamp
     */
    public Long getEmailTimeStamp() { return iEmailTimeStamp; }
    /**
     * Set last email time stamp
     * @param emailTimeStamp student email time stamp
     */
    public void setEmailTimeStamp(Long emailTimeStamp) { iEmailTimeStamp = emailTimeStamp; }

    @Override
    public int compareTo(Student s) {
        // real students first
        if (isDummy()) {
            if (!s.isDummy()) return 1;
        } else if (s.isDummy()) return -1;
        // then id
        return new Long(getId()).compareTo(s.getId());
    }
    
    /**
     * List of student unavailabilities
     * @return student unavailabilities
     */
    public List<Unavailability> getUnavailabilities() { return iUnavailabilities; }
    
    /**
     * Check if student is available during the given section
     * @param section given section
     * @return true, if available (the section cannot overlap and there is no overlapping unavailability that cannot overlap) 
     */
    public boolean isAvailable(Section section) {
        if (section.isAllowOverlap() || section.getTime() == null) return true;
        for (Unavailability unavailability: getUnavailabilities())
            if (unavailability.isOverlapping(section)) return false;
        return true;
    }
    
    /**
     * Check if student is available during the given enrollment
     * @param enrollment given enrollment
     * @return true, if available
     */
    public boolean isAvailable(Enrollment enrollment) {
        if (enrollment != null && enrollment.isCourseRequest() && !enrollment.isAllowOverlap())
            for (Section section: enrollment.getSections())
                if (!isAvailable(section)) return false;
        return true;
    }
    
    /**
     * Return true if the student needs short distances. A different distance conflict checking is employed for such students.
     * @return true if the student needs short distances
     */
    public boolean isNeedShortDistances() {
        return iNeedShortDistances;
    }
    
    /**
     * Set true if the student needs short distances. A different distance conflict checking is employed for such students.
     * @param needShortDistances true if the student needs short distances (default is false)
     */
    public void setNeedShortDistances(boolean needShortDistances) {
        iNeedShortDistances = needShortDistances;
    }
    
    /**
     * True if student can be enrolled in disabled sections, regardless if his/her reservations 
     * @return does this student allow for disabled sections
     */
    public boolean isAllowDisabled() {
        return iAllowDisabled;
    }
    
    /**
     * Set to true  if student can be enrolled in disabled sections, regardless if his/her reservations
     * @param allowDisabled does this student allow for disabled sections
     */
    public void setAllowDisabled(boolean allowDisabled) {
        iAllowDisabled = allowDisabled;
    }
    
    /**
     * True if student has max credit defined
     * @return true if max credit is set
     */
    public boolean hasMaxCredit() { return iMaxCredit != null; }
    
    /**
     * Get student max credit ({@link Float#MAX_VALUE} if not set)
     * return student max credit
     */
    public float getMaxCredit() { return (iMaxCredit == null ? Float.MAX_VALUE : iMaxCredit.floatValue()); }
    
    /**
     * Set student max credit (null if not set)
     * @param maxCredit student max credit
     */
    public void setMaxCredit(Float maxCredit) { iMaxCredit = maxCredit; }
}
