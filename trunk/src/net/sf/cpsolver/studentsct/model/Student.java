package net.sf.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.List;

import net.sf.cpsolver.coursett.model.TimeLocation;


/**
 * Representation of a student. Each student contains id, and a list of
 * requests. <br>
 * <br>
 * Last-like semester students are mark as dummy. Dummy students have lower
 * value and generally should not block "real" students from getting requested
 * courses. <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class Student {
    private long iId;
    private boolean iDummy = false;
    private List<Request> iRequests = new ArrayList<Request>();
    private List<AcademicAreaCode> iAcadAreaClassifs = new ArrayList<AcademicAreaCode>();
    private List<AcademicAreaCode> iMajors = new ArrayList<AcademicAreaCode>();
    private List<AcademicAreaCode> iMinors = new ArrayList<AcademicAreaCode>();

    public static double sDummyStudentWeight = 0.5;

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

    /** Student unique id */
    public long getId() {
        return iId;
    }

    /** Set student unique id */
    public void setId(long id) {
        iId = id;
    }

    /** Student's course and free time requests */
    public List<Request> getRequests() {
        return iRequests;
    }

    /** Number of requests (alternative requests are ignored) */
    public int nrRequests() {
        int ret = 0;
        for (Request r : getRequests()) {
            if (!r.isAlternative())
                ret++;
        }
        return ret;
    }

    /** Number of alternative requests */
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
     **/
    public boolean canAssign(Request request) {
        if (request.isAssigned())
            return true;
        int alt = 0;
        boolean found = false;
        for (Request r : getRequests()) {
            if (r.equals(request))
                found = true;
            boolean assigned = (r.isAssigned() || r.equals(request));
            boolean course = (r instanceof CourseRequest);
            boolean waitlist = (course && ((CourseRequest) r).isWaitlist());
            if (r.isAlternative()) {
                if (assigned || (!found && waitlist))
                    alt--;
            } else {
                if (course && !waitlist && !assigned)
                    alt++;
            }
        }
        return (alt >= 0);
    }

    /**
     * True if the student has assigned the desired number of requests (i.e.,
     * number of non-alternative course requests).
     */
    public boolean isComplete() {
        int nrRequests = 0;
        int nrAssignedRequests = 0;
        for (Request r : getRequests()) {
            if (!(r instanceof CourseRequest))
                continue; // ignore free times
            if (!r.isAlternative())
                nrRequests++;
            if (r.isAssigned())
                nrAssignedRequests++;
        }
        return nrAssignedRequests == nrRequests;
    }

    /** Number of assigned COURSE requests */
    public int nrAssignedRequests() {
        int nrAssignedRequests = 0;
        for (Request r : getRequests()) {
            if (!(r instanceof CourseRequest))
                continue; // ignore free times
            if (r.isAssigned())
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
     */
    public boolean isDummy() {
        return iDummy;
    }

    /**
     * Set student's dummy flag. Dummy students have lower value and generally
     * should not block "real" students from getting requested courses.
     */
    public void setDummy(boolean dummy) {
        iDummy = dummy;
    }

    /**
     * List of academic area - classification codes ({@link AcademicAreaCode})
     * for the given student
     */
    public List<AcademicAreaCode> getAcademicAreaClasiffications() {
        return iAcadAreaClassifs;
    }

    /**
     * List of major codes ({@link AcademicAreaCode}) for the given student
     */
    public List<AcademicAreaCode> getMajors() {
        return iMajors;
    }

    /**
     * List of major codes ({@link AcademicAreaCode}) for the given student
     */
    public List<AcademicAreaCode> getMinors() {
        return iMinors;
    }

    /**
     * Compare two students for equality. Two students are considered equal if
     * they have the same id.
     */
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Student))
            return false;
        return getId() == ((Student) object).getId();
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
}
