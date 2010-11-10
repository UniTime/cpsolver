package net.sf.cpsolver.studentsct;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.HashSet;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

import org.apache.log4j.Logger;

/**
 * Student sectioning model.
 * 
 * <br>
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
public class StudentSectioningModel extends Model<Request, Enrollment> {
    private static Logger sLog = Logger.getLogger(StudentSectioningModel.class);
    private List<Student> iStudents = new ArrayList<Student>();
    private List<Offering> iOfferings = new ArrayList<Offering>();
    private Set<Student> iCompleteStudents = new java.util.HashSet<Student>();
    private double iTotalValue = 0.0;
    private DataProperties iProperties;
    private DistanceConflict iDistanceConflict = null;
    private int iNrDummyStudents = 0, iNrDummyRequests = 0, iNrAssignedDummyRequests = 0, iNrCompleteDummyStudents = 0;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    public StudentSectioningModel(DataProperties properties) {
        super();
        iAssignedVariables = new HashSet<Request>();
        iUnassignedVariables = new HashSet<Request>();
        iPerturbVariables = new HashSet<Request>();
        SectionLimit sectionLimit = new SectionLimit(properties);
        addGlobalConstraint(sectionLimit);
        sectionLimit.addConstraintListener(new ConstraintListener<Enrollment>() {
            public void constraintBeforeAssigned(long iteration, Constraint<?, Enrollment> constraint,
                    Enrollment enrollment, Set<Enrollment> unassigned) {
                if (enrollment.getStudent().isDummy())
                    for (Enrollment conflict : unassigned) {
                        if (!conflict.getStudent().isDummy()) {
                            sLog.warn("Enrolment of a real student " + conflict.getStudent() + " is unassigned "
                                    + "\n  -- " + conflict + "\ndue to an enrollment of a dummy student "
                                    + enrollment.getStudent() + " " + "\n  -- " + enrollment);
                        }
                    }
            }

            public void constraintAfterAssigned(long iteration, Constraint<?, Enrollment> constraint,
                    Enrollment assigned, Set<Enrollment> unassigned) {
            }
        });
        iProperties = properties;
    }

    /**
     * Students
     */
    public List<Student> getStudents() {
        return iStudents;
    }

    /**
     * Students with complete schedules (see {@link Student#isComplete()})
     */
    public Set<Student> getCompleteStudents() {
        return iCompleteStudents;
    }

    /**
     * Add a student into the model
     */
    public void addStudent(Student student) {
        iStudents.add(student);
        if (student.isDummy())
            iNrDummyStudents++;
        StudentConflict conflict = new StudentConflict();
        for (Request request : student.getRequests()) {
            conflict.addVariable(request);
            addVariable(request);
            if (student.isDummy())
                iNrDummyRequests++;
        }
        addConstraint(conflict);
        if (student.isComplete())
            iCompleteStudents.add(student);
    }

    /**
     * Remove a student from the model
     */
    public void removeStudent(Student student) {
        iStudents.remove(student);
        if (student.isDummy())
            iNrDummyStudents--;
        if (student.isComplete())
            iCompleteStudents.remove(student);
        StudentConflict conflict = null;
        for (Request request : student.getRequests()) {
            for (Constraint<Request, Enrollment> c : request.constraints()) {
                if (c instanceof StudentConflict) {
                    conflict = (StudentConflict) c;
                    break;
                }
            }
            conflict.removeVariable(request);
            removeVariable(request);
            if (student.isDummy())
                iNrDummyRequests--;
        }
        removeConstraint(conflict);
    }

    /**
     * List of offerings
     */
    public List<Offering> getOfferings() {
        return iOfferings;
    }

    /**
     * Add an offering into the model
     */
    public void addOffering(Offering offering) {
        iOfferings.add(offering);
    }

    /**
     * Number of students with complete schedule
     */
    public int nrComplete() {
        return getCompleteStudents().size();
    }

    /**
     * Model info
     */
    @Override
    public Hashtable<String, String> getInfo() {
        Hashtable<String, String> info = super.getInfo();
        info.put("Students with complete schedule", sDoubleFormat.format(100.0 * nrComplete() / getStudents().size())
                + "% (" + nrComplete() + "/" + getStudents().size() + ")");
        if (getDistanceConflict() != null)
            info.put("Student distance conflicts", sDoubleFormat.format(getDistanceConflict().getTotalNrConflicts()));
        return info;
    }

    /**
     * Overall solution value
     */
    @Override
    public double getTotalValue() {
        return iTotalValue;
    }

    /**
     * Called after an enrollment was assigned to a request. The list of
     * complete students and the overall solution value are updated.
     */
    @Override
    public void afterAssigned(long iteration, Enrollment enrollment) {
        super.afterAssigned(iteration, enrollment);
        Student student = enrollment.getStudent();
        if (student.isComplete())
            iCompleteStudents.add(student);
        iTotalValue += enrollment.toDouble();
        if (student.isDummy()) {
            iNrAssignedDummyRequests++;
            if (student.isComplete())
                iNrCompleteDummyStudents++;
        }
    }

    /**
     * Called before an enrollment was unassigned from a request. The list of
     * complete students and the overall solution value are updated.
     */
    @Override
    public void afterUnassigned(long iteration, Enrollment enrollment) {
        super.afterUnassigned(iteration, enrollment);
        Student student = enrollment.getStudent();
        if (iCompleteStudents.contains(student) && !student.isComplete()) {
            iCompleteStudents.remove(student);
            if (student.isDummy())
                iNrCompleteDummyStudents--;
        }
        iTotalValue -= enrollment.toDouble();
        if (student.isDummy()) {
            iNrAssignedDummyRequests--;
        }
    }

    /**
     * Configuration
     */
    public DataProperties getProperties() {
        return iProperties;
    }

    /**
     * Empty online student sectioning infos for all sections (see
     * {@link Section#getSpaceExpected()} and {@link Section#getSpaceHeld()}).
     */
    public void clearOnlineSectioningInfos() {
        for (Offering offering : iOfferings) {
            for (Config config : offering.getConfigs()) {
                for (Subpart subpart : config.getSubparts()) {
                    for (Section section : subpart.getSections()) {
                        section.setSpaceExpected(0);
                        section.setSpaceHeld(0);
                    }
                }
            }
        }
    }

    /**
     * Compute online student sectioning infos for all sections (see
     * {@link Section#getSpaceExpected()} and {@link Section#getSpaceHeld()}).
     */
    public void computeOnlineSectioningInfos() {
        clearOnlineSectioningInfos();
        for (Student student : getStudents()) {
            if (!student.isDummy())
                continue;
            for (Request request : student.getRequests()) {
                if (!(request instanceof CourseRequest))
                    continue;
                CourseRequest courseRequest = (CourseRequest) request;
                Enrollment enrollment = courseRequest.getAssignment();
                if (enrollment != null) {
                    for (Section section : enrollment.getSections()) {
                        section.setSpaceHeld(courseRequest.getWeight() + section.getSpaceHeld());
                    }
                }
                List<Enrollment> feasibleEnrollments = new ArrayList<Enrollment>();
                for (Enrollment enrl : courseRequest.values()) {
                    boolean overlaps = false;
                    for (Request otherRequest : student.getRequests()) {
                        if (otherRequest.equals(courseRequest) || !(otherRequest instanceof CourseRequest))
                            continue;
                        Enrollment otherErollment = otherRequest.getAssignment();
                        if (otherErollment == null)
                            continue;
                        if (enrl.isOverlapping(otherErollment)) {
                            overlaps = true;
                            break;
                        }
                    }
                    if (!overlaps)
                        feasibleEnrollments.add(enrl);
                }
                double increment = courseRequest.getWeight() / feasibleEnrollments.size();
                for (Enrollment feasibleEnrollment : feasibleEnrollments) {
                    for (Section section : feasibleEnrollment.getSections()) {
                        section.setSpaceExpected(section.getSpaceExpected() + increment);
                    }
                }
            }
        }
    }

    /**
     * Sum of weights of all requests that are not assigned (see
     * {@link Request#getWeight()}).
     */
    public double getUnassignedRequestWeight() {
        double weight = 0.0;
        for (Request request : unassignedVariables()) {
            weight += request.getWeight();
        }
        return weight;
    }

    /**
     * Sum of weights of all requests (see {@link Request#getWeight()}).
     */
    public double getTotalRequestWeight() {
        double weight = 0.0;
        for (Request request : unassignedVariables()) {
            weight += request.getWeight();
        }
        return weight;
    }

    /**
     * Set distance conflict extension
     */
    public void setDistanceConflict(DistanceConflict dc) {
        iDistanceConflict = dc;
    }

    /**
     * Return distance conflict extension
     */
    public DistanceConflict getDistanceConflict() {
        return iDistanceConflict;
    }

    /**
     * Average priority of unassigned requests (see
     * {@link Request#getPriority()})
     */
    public double avgUnassignPriority() {
        double totalPriority = 0.0;
        for (Request request : unassignedVariables()) {
            if (request.isAlternative())
                continue;
            totalPriority += request.getPriority();
        }
        return 1.0 + totalPriority / unassignedVariables().size();
    }

    /**
     * Average number of requests per student (see {@link Student#getRequests()}
     * )
     */
    public double avgNrRequests() {
        double totalRequests = 0.0;
        int totalStudents = 0;
        for (Student student : getStudents()) {
            if (student.nrRequests() == 0)
                continue;
            totalRequests += student.nrRequests();
            totalStudents++;
        }
        return totalRequests / totalStudents;
    }

    /** Number of last like ({@link Student#isDummy()} equals true) students. */
    public int getNrLastLikeStudents(boolean precise) {
        if (!precise)
            return iNrDummyStudents;
        int nrLastLikeStudents = 0;
        for (Student student : getStudents()) {
            if (student.isDummy())
                nrLastLikeStudents++;
        }
        return nrLastLikeStudents;
    }

    /** Number of real ({@link Student#isDummy()} equals false) students. */
    public int getNrRealStudents(boolean precise) {
        if (!precise)
            return getStudents().size() - iNrDummyStudents;
        int nrRealStudents = 0;
        for (Student student : getStudents()) {
            if (!student.isDummy())
                nrRealStudents++;
        }
        return nrRealStudents;
    }

    /**
     * Number of last like ({@link Student#isDummy()} equals true) students with
     * a complete schedule ({@link Student#isComplete()} equals true).
     */
    public int getNrCompleteLastLikeStudents(boolean precise) {
        if (!precise)
            return iNrCompleteDummyStudents;
        int nrLastLikeStudents = 0;
        for (Student student : getCompleteStudents()) {
            if (student.isDummy())
                nrLastLikeStudents++;
        }
        return nrLastLikeStudents;
    }

    /**
     * Number of real ({@link Student#isDummy()} equals false) students with a
     * complete schedule ({@link Student#isComplete()} equals true).
     */
    public int getNrCompleteRealStudents(boolean precise) {
        if (!precise)
            return getCompleteStudents().size() - iNrCompleteDummyStudents;
        int nrRealStudents = 0;
        for (Student student : getCompleteStudents()) {
            if (!student.isDummy())
                nrRealStudents++;
        }
        return nrRealStudents;
    }

    /**
     * Number of requests from last-like ({@link Student#isDummy()} equals true)
     * students.
     */
    public int getNrLastLikeRequests(boolean precise) {
        if (!precise)
            return iNrDummyRequests;
        int nrLastLikeRequests = 0;
        for (Request request : variables()) {
            if (request.getStudent().isDummy())
                nrLastLikeRequests++;
        }
        return nrLastLikeRequests;
    }

    /**
     * Number of requests from real ({@link Student#isDummy()} equals false)
     * students.
     */
    public int getNrRealRequests(boolean precise) {
        if (!precise)
            return variables().size() - iNrDummyRequests;
        int nrRealRequests = 0;
        for (Request request : variables()) {
            if (!request.getStudent().isDummy())
                nrRealRequests++;
        }
        return nrRealRequests;
    }

    /**
     * Number of requests from last-like ({@link Student#isDummy()} equals true)
     * students that are assigned.
     */
    public int getNrAssignedLastLikeRequests(boolean precise) {
        if (!precise)
            return iNrAssignedDummyRequests;
        int nrLastLikeRequests = 0;
        for (Request request : assignedVariables()) {
            if (request.getStudent().isDummy())
                nrLastLikeRequests++;
        }
        return nrLastLikeRequests;
    }

    /**
     * Number of requests from real ({@link Student#isDummy()} equals false)
     * students that are assigned.
     */
    public int getNrAssignedRealRequests(boolean precise) {
        if (!precise)
            return assignedVariables().size() - iNrAssignedDummyRequests;
        int nrRealRequests = 0;
        for (Request request : assignedVariables()) {
            if (!request.getStudent().isDummy())
                nrRealRequests++;
        }
        return nrRealRequests;
    }

    /**
     * Model extended info. Some more information (that is more expensive to
     * compute) is added to an ordinary {@link Model#getInfo()}.
     */
    @Override
    public Hashtable<String, String> getExtendedInfo() {
        Hashtable<String, String> info = getInfo();
        int nrLastLikeStudents = getNrLastLikeStudents(true);
        if (nrLastLikeStudents != 0 && nrLastLikeStudents != getStudents().size()) {
            int nrRealStudents = getStudents().size() - nrLastLikeStudents;
            int nrLastLikeCompleteStudents = getNrCompleteLastLikeStudents(true);
            int nrRealCompleteStudents = getCompleteStudents().size() - nrLastLikeCompleteStudents;
            info.put("Last-like students with complete schedule", sDoubleFormat.format(100.0
                    * nrLastLikeCompleteStudents / nrLastLikeStudents)
                    + "% (" + nrLastLikeCompleteStudents + "/" + nrLastLikeStudents + ")");
            info.put("Real students with complete schedule", sDoubleFormat.format(100.0 * nrRealCompleteStudents
                    / nrRealStudents)
                    + "% (" + nrRealCompleteStudents + "/" + nrRealStudents + ")");
            int nrLastLikeRequests = getNrLastLikeRequests(true);
            int nrRealRequests = variables().size() - nrLastLikeRequests;
            int nrLastLikeAssignedRequests = getNrAssignedLastLikeRequests(true);
            int nrRealAssignedRequests = assignedVariables().size() - nrLastLikeAssignedRequests;
            info.put("Last-like assigned requests", sDoubleFormat.format(100.0 * nrLastLikeAssignedRequests
                    / nrLastLikeRequests)
                    + "% (" + nrLastLikeAssignedRequests + "/" + nrLastLikeRequests + ")");
            info.put("Real assigned requests", sDoubleFormat.format(100.0 * nrRealAssignedRequests / nrRealRequests)
                    + "% (" + nrRealAssignedRequests + "/" + nrRealRequests + ")");
        }
        info.put("Average unassigned priority", sDoubleFormat.format(avgUnassignPriority()));
        info.put("Average number of requests", sDoubleFormat.format(avgNrRequests()));
        info.put("Unassigned request weight", sDoubleFormat.format(getUnassignedRequestWeight()) + " / "
                + sDoubleFormat.format(getTotalRequestWeight()));
        return info;
    }

}
