package org.cpsolver.studentsct;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.assignment.OptimisticInheritedAssignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ModelWithContext;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.ConstraintListener;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.constraint.ConfigLimit;
import org.cpsolver.studentsct.constraint.CourseLimit;
import org.cpsolver.studentsct.constraint.LinkedSections;
import org.cpsolver.studentsct.constraint.RequiredReservation;
import org.cpsolver.studentsct.constraint.ReservationLimit;
import org.cpsolver.studentsct.constraint.SectionLimit;
import org.cpsolver.studentsct.constraint.StudentConflict;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.reservation.Reservation;
import org.cpsolver.studentsct.weights.PriorityStudentWeights;
import org.cpsolver.studentsct.weights.StudentWeights;

/**
 * Student sectioning model.
 * 
 * <br>
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
public class StudentSectioningModel extends ModelWithContext<Request, Enrollment, StudentSectioningModel.StudentSectioningModelContext> {
    private static Logger sLog = Logger.getLogger(StudentSectioningModel.class);
    protected static DecimalFormat sDecimalFormat = new DecimalFormat("0.000");
    private List<Student> iStudents = new ArrayList<Student>();
    private List<Offering> iOfferings = new ArrayList<Offering>();
    private List<LinkedSections> iLinkedSections = new ArrayList<LinkedSections>();
    private DataProperties iProperties;
    private DistanceConflict iDistanceConflict = null;
    private TimeOverlapsCounter iTimeOverlaps = null;
    private int iNrDummyStudents = 0, iNrDummyRequests = 0;
    private double iTotalDummyWeight = 0.0;
    private double iTotalCRWeight = 0.0, iTotalDummyCRWeight = 0.0;
    private double iTotalReservedSpace = 0.0;
    private StudentWeights iStudentWeights = null;
    private boolean iReservationCanAssignOverTheLimit;
    protected double iProjectedStudentWeight = 0.0100;
    private int iMaxDomainSize = -1; 


    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    @SuppressWarnings("unchecked")
    public StudentSectioningModel(DataProperties properties) {
        super();
        iReservationCanAssignOverTheLimit =  properties.getPropertyBoolean("Reservation.CanAssignOverTheLimit", false);
        iStudentWeights = new PriorityStudentWeights(properties);
        iMaxDomainSize = properties.getPropertyInt("Sectioning.MaxDomainSize", iMaxDomainSize);
        if (properties.getPropertyBoolean("Sectioning.SectionLimit", true)) {
            SectionLimit sectionLimit = new SectionLimit(properties);
            addGlobalConstraint(sectionLimit);
            if (properties.getPropertyBoolean("Sectioning.SectionLimit.Debug", false)) {
                sectionLimit.addConstraintListener(new ConstraintListener<Request, Enrollment>() {
                    @Override
                    public void constraintBeforeAssigned(Assignment<Request, Enrollment> assignment, long iteration, Constraint<Request, Enrollment> constraint, Enrollment enrollment, Set<Enrollment> unassigned) {
                        if (enrollment.getStudent().isDummy())
                            for (Enrollment conflict : unassigned) {
                                if (!conflict.getStudent().isDummy()) {
                                    sLog.warn("Enrolment of a real student " + conflict.getStudent() + " is unassigned "
                                            + "\n  -- " + conflict + "\ndue to an enrollment of a dummy student "
                                            + enrollment.getStudent() + " " + "\n  -- " + enrollment);
                                }
                            }
                    }

                    @Override
                    public void constraintAfterAssigned(Assignment<Request, Enrollment> assignment, long iteration, Constraint<Request, Enrollment> constraint, Enrollment assigned, Set<Enrollment> unassigned) {
                    }
                });
            }
        }
        if (properties.getPropertyBoolean("Sectioning.ConfigLimit", true)) {
            ConfigLimit configLimit = new ConfigLimit(properties);
            addGlobalConstraint(configLimit);
        }
        if (properties.getPropertyBoolean("Sectioning.CourseLimit", true)) {
            CourseLimit courseLimit = new CourseLimit(properties);
            addGlobalConstraint(courseLimit);
        }
        if (properties.getPropertyBoolean("Sectioning.ReservationLimit", true)) {
            ReservationLimit reservationLimit = new ReservationLimit(properties);
            addGlobalConstraint(reservationLimit);
        }
        if (properties.getPropertyBoolean("Sectioning.RequiredReservations", true)) {
            RequiredReservation requiredReservation = new RequiredReservation();
            addGlobalConstraint(requiredReservation);
        }
        try {
            Class<StudentWeights> studentWeightsClass = (Class<StudentWeights>)Class.forName(properties.getProperty("StudentWeights.Class", PriorityStudentWeights.class.getName()));
            iStudentWeights = studentWeightsClass.getConstructor(DataProperties.class).newInstance(properties);
        } catch (Exception e) {
            sLog.error("Unable to create custom student weighting model (" + e.getMessage() + "), using default.", e);
            iStudentWeights = new PriorityStudentWeights(properties);
        }
        iProjectedStudentWeight = properties.getPropertyDouble("StudentWeights.ProjectedStudentWeight", iProjectedStudentWeight);
        iProperties = properties;
    }
    
    /**
     * Return true if reservation that has {@link Reservation#canAssignOverLimit()} can assign enrollments over the limit
     * @return true if reservation that has {@link Reservation#canAssignOverLimit()} can assign enrollments over the limit
     */
    public boolean getReservationCanAssignOverTheLimit() {
        return iReservationCanAssignOverTheLimit;
    }
    
    /**
     * Return student weighting model
     * @return student weighting model
     */
    public StudentWeights getStudentWeights() {
        return iStudentWeights;
    }

    /**
     * Set student weighting model
     * @param weights student weighting model
     */
    public void setStudentWeights(StudentWeights weights) {
        iStudentWeights = weights;
    }

    /**
     * Students
     * @return all students in the problem
     */
    public List<Student> getStudents() {
        return iStudents;
    }

    /**
     * Add a student into the model
     * @param student a student to be added into the problem
     */
    public void addStudent(Student student) {
        iStudents.add(student);
        if (student.isDummy())
            iNrDummyStudents++;
        for (Request request : student.getRequests())
            addVariable(request);
        if (getProperties().getPropertyBoolean("Sectioning.StudentConflict", true)) {
            addConstraint(new StudentConflict(student));
        }
    }
    
    @Override
    public void addVariable(Request request) {
        super.addVariable(request);
        if (request instanceof CourseRequest)
            iTotalCRWeight += request.getWeight();
        if (request.getStudent().isDummy()) {
            iNrDummyRequests++;
            iTotalDummyWeight += request.getWeight();
            if (request instanceof CourseRequest)
                iTotalDummyCRWeight += request.getWeight();
        }
    }
    
    /** 
     * Recompute cached request weights
     * @param assignment current assignment
     */
    public void requestWeightsChanged(Assignment<Request, Enrollment> assignment) {
        getContext(assignment).requestWeightsChanged(assignment);
    }

    /**
     * Remove a student from the model
     * @param student a student to be removed from the problem
     */
    public void removeStudent(Student student) {
        iStudents.remove(student);
        if (student.isDummy())
            iNrDummyStudents--;
        StudentConflict conflict = null;
        for (Request request : student.getRequests()) {
            for (Constraint<Request, Enrollment> c : request.constraints()) {
                if (c instanceof StudentConflict) {
                    conflict = (StudentConflict) c;
                    break;
                }
            }
            if (conflict != null) 
                conflict.removeVariable(request);
            removeVariable(request);
        }
        if (conflict != null) 
            removeConstraint(conflict);
    }
    
    @Override
    public void removeVariable(Request request) {
        super.removeVariable(request);
        if (request instanceof CourseRequest) {
            CourseRequest cr = (CourseRequest)request;
            for (Course course: cr.getCourses())
                course.getRequests().remove(request);
        }
        if (request.getStudent().isDummy()) {
            iNrDummyRequests--;
            iTotalDummyWeight -= request.getWeight();
            if (request instanceof CourseRequest)
                iTotalDummyCRWeight -= request.getWeight();
        }
        if (request instanceof CourseRequest)
            iTotalCRWeight -= request.getWeight();
    }


    /**
     * List of offerings
     * @return all instructional offerings of the problem
     */
    public List<Offering> getOfferings() {
        return iOfferings;
    }

    /**
     * Add an offering into the model
     * @param offering an instructional offering to be added into the problem
     */
    public void addOffering(Offering offering) {
        iOfferings.add(offering);
        offering.setModel(this);
    }
    
    /**
     * Link sections using {@link LinkedSections}
     * @param sections a linked section constraint to be added into the problem
     */
    public void addLinkedSections(Section... sections) {
        LinkedSections constraint = new LinkedSections(sections);
        iLinkedSections.add(constraint);
        constraint.createConstraints();
    }

    /**
     * Link sections using {@link LinkedSections}
     * @param sections a linked section constraint to be added into the problem
     */
    public void addLinkedSections(Collection<Section> sections) {
        LinkedSections constraint = new LinkedSections(sections);
        iLinkedSections.add(constraint);
        constraint.createConstraints();
    }

    /**
     * List of linked sections
     * @return all linked section constraints of the problem
     */
    public List<LinkedSections> getLinkedSections() {
        return iLinkedSections;
    }

    /**
     * Model info
     */
    @Override
    public Map<String, String> getInfo(Assignment<Request, Enrollment> assignment) {
        Map<String, String> info = super.getInfo(assignment);
        StudentSectioningModelContext context = getContext(assignment);
        if (!getStudents().isEmpty())
            info.put("Students with complete schedule", sDoubleFormat.format(100.0 * context.nrComplete() / getStudents().size()) + "% (" + context.nrComplete() + "/" + getStudents().size() + ")");
        if (getDistanceConflict() != null && getDistanceConflict().getTotalNrConflicts(assignment) != 0)
            info.put("Student distance conflicts", String.valueOf(getDistanceConflict().getTotalNrConflicts(assignment)));
        if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts(assignment) != 0)
            info.put("Time overlapping conflicts", String.valueOf(getTimeOverlaps().getTotalNrConflicts(assignment)));
        int nrLastLikeStudents = getNrLastLikeStudents(false);
        if (nrLastLikeStudents != 0 && nrLastLikeStudents != getStudents().size()) {
            int nrRealStudents = getStudents().size() - nrLastLikeStudents;
            int nrLastLikeCompleteStudents = getNrCompleteLastLikeStudents(assignment, false);
            int nrRealCompleteStudents = context.nrComplete() - nrLastLikeCompleteStudents;
            if (nrLastLikeStudents > 0)
                info.put("Projected students with complete schedule", sDecimalFormat.format(100.0
                        * nrLastLikeCompleteStudents / nrLastLikeStudents)
                        + "% (" + nrLastLikeCompleteStudents + "/" + nrLastLikeStudents + ")");
            if (nrRealStudents > 0)
                info.put("Real students with complete schedule", sDecimalFormat.format(100.0 * nrRealCompleteStudents
                        / nrRealStudents)
                        + "% (" + nrRealCompleteStudents + "/" + nrRealStudents + ")");
            int nrLastLikeRequests = getNrLastLikeRequests(false);
            int nrRealRequests = variables().size() - nrLastLikeRequests;
            int nrLastLikeAssignedRequests = context.getNrAssignedLastLikeRequests();
            int nrRealAssignedRequests = assignment.nrAssignedVariables() - nrLastLikeAssignedRequests;
            if (nrLastLikeRequests > 0)
                info.put("Projected assigned requests", sDecimalFormat.format(100.0 * nrLastLikeAssignedRequests / nrLastLikeRequests)
                        + "% (" + nrLastLikeAssignedRequests + "/" + nrLastLikeRequests + ")");
            if (nrRealRequests > 0)
                info.put("Real assigned requests", sDecimalFormat.format(100.0 * nrRealAssignedRequests / nrRealRequests)
                        + "% (" + nrRealAssignedRequests + "/" + nrRealRequests + ")");
            if (getDistanceConflict() != null && getDistanceConflict().getTotalNrConflicts(assignment) > 0)
                info.put("Student distance conflicts", String.valueOf(getDistanceConflict().getTotalNrConflicts(assignment)));
            if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts(assignment) > 0)
                info.put("Time overlapping conflicts", String.valueOf(getTimeOverlaps().getTotalNrConflicts(assignment)));
        }
        context.getInfo(assignment, info);

        return info;
    }

    /**
     * Overall solution value
     * @param assignment current assignment
     * @param precise true if should be computed
     * @return solution value
     */
    public double getTotalValue(Assignment<Request, Enrollment> assignment, boolean precise) {
        if (precise) {
            double total = 0;
            for (Request r: assignment.assignedVariables())
                total += r.getWeight() * iStudentWeights.getWeight(assignment, assignment.getValue(r));
            if (iDistanceConflict != null)
                for (DistanceConflict.Conflict c: iDistanceConflict.computeAllConflicts(assignment))
                    total -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(assignment, c);
            if (iTimeOverlaps != null)
                for (TimeOverlapsCounter.Conflict c: iTimeOverlaps.getContext(assignment).computeAllConflicts(assignment)) {
                    total -= c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE1(), c);
                    total -= c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE2(), c);
                }
            return -total;
        }
        return getContext(assignment).getTotalValue();
    }
    
    /**
     * Overall solution value
     */
    @Override
    public double getTotalValue(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getTotalValue();
    }

    /**
     * Configuration
     * @return solver configuration
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
     * @param assignment current assignment
     */
    public void computeOnlineSectioningInfos(Assignment<Request, Enrollment> assignment) {
        clearOnlineSectioningInfos();
        for (Student student : getStudents()) {
            if (!student.isDummy())
                continue;
            for (Request request : student.getRequests()) {
                if (!(request instanceof CourseRequest))
                    continue;
                CourseRequest courseRequest = (CourseRequest) request;
                Enrollment enrollment = assignment.getValue(courseRequest);
                if (enrollment != null) {
                    for (Section section : enrollment.getSections()) {
                        section.setSpaceHeld(courseRequest.getWeight() + section.getSpaceHeld());
                    }
                }
                List<Enrollment> feasibleEnrollments = new ArrayList<Enrollment>();
                int totalLimit = 0;
                for (Enrollment enrl : courseRequest.values(assignment)) {
                    boolean overlaps = false;
                    for (Request otherRequest : student.getRequests()) {
                        if (otherRequest.equals(courseRequest) || !(otherRequest instanceof CourseRequest))
                            continue;
                        Enrollment otherErollment = assignment.getValue(otherRequest);
                        if (otherErollment == null)
                            continue;
                        if (enrl.isOverlapping(otherErollment)) {
                            overlaps = true;
                            break;
                        }
                    }
                    if (!overlaps) {
                        feasibleEnrollments.add(enrl);
                        if (totalLimit >= 0) {
                            int limit = enrl.getLimit();
                            if (limit < 0) totalLimit = -1;
                            else totalLimit += limit;
                        }
                    }
                }
                double increment = courseRequest.getWeight() / (totalLimit > 0 ? totalLimit : feasibleEnrollments.size());
                for (Enrollment feasibleEnrollment : feasibleEnrollments) {
                    for (Section section : feasibleEnrollment.getSections()) {
                        if (totalLimit > 0) {
                            section.setSpaceExpected(section.getSpaceExpected() + increment * feasibleEnrollment.getLimit());
                        } else {
                            section.setSpaceExpected(section.getSpaceExpected() + increment);
                        }
                    }
                }
            }
        }
    }

    /**
     * Sum of weights of all requests that are not assigned (see
     * {@link Request#getWeight()}).
     * @param assignment current assignment
     * @return unassigned request weight
     */
    public double getUnassignedRequestWeight(Assignment<Request, Enrollment> assignment) {
        double weight = 0.0;
        for (Request request : assignment.unassignedVariables(this)) {
            weight += request.getWeight();
        }
        return weight;
    }

    /**
     * Sum of weights of all requests (see {@link Request#getWeight()}).
     * @return total request weight
     */
    public double getTotalRequestWeight() {
        double weight = 0.0;
        for (Request request : variables()) {
            weight += request.getWeight();
        }
        return weight;
    }

    /**
     * Set distance conflict extension
     * @param dc distance conflicts extension
     */
    public void setDistanceConflict(DistanceConflict dc) {
        iDistanceConflict = dc;
    }

    /**
     * Return distance conflict extension
     * @return distance conflicts extension
     */
    public DistanceConflict getDistanceConflict() {
        return iDistanceConflict;
    }

    /**
     * Set time overlaps extension
     * @param toc time overlapping conflicts extension
     */
    public void setTimeOverlaps(TimeOverlapsCounter toc) {
        iTimeOverlaps = toc;
    }

    /**
     * Return time overlaps extension
     * @return time overlapping conflicts extension
     */
    public TimeOverlapsCounter getTimeOverlaps() {
        return iTimeOverlaps;
    }

    /**
     * Average priority of unassigned requests (see
     * {@link Request#getPriority()})
     * @param assignment current assignment
     * @return average priority of unassigned requests
     */
    public double avgUnassignPriority(Assignment<Request, Enrollment> assignment) {
        double totalPriority = 0.0;
        for (Request request : assignment.unassignedVariables(this)) {
            if (request.isAlternative())
                continue;
            totalPriority += request.getPriority();
        }
        return 1.0 + totalPriority / assignment.nrUnassignedVariables(this);
    }

    /**
     * Average number of requests per student (see {@link Student#getRequests()}
     * )
     * @return average number of requests per student
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

    /** Number of last like ({@link Student#isDummy()} equals true) students. 
     * @param precise true if to be computed
     * @return number of last like (projected) students
     **/
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

    /** Number of real ({@link Student#isDummy()} equals false) students. 
     * @param precise true if to be computed
     * @return number of real students
     **/
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
     * a complete schedule ({@link Student#isComplete(Assignment)} equals true).
     * @param assignment current assignment
     * @param precise true if to be computed
     * @return number of last like (projected) students with a complete schedule
     */
    public int getNrCompleteLastLikeStudents(Assignment<Request, Enrollment> assignment, boolean precise) {
        if (!precise)
            return getContext(assignment).getNrCompleteLastLikeStudents();
        int nrLastLikeStudents = 0;
        for (Student student : getStudents()) {
            if (student.isComplete(assignment) && student.isDummy())
                nrLastLikeStudents++;
        }
        return nrLastLikeStudents;
    }

    /**
     * Number of real ({@link Student#isDummy()} equals false) students with a
     * complete schedule ({@link Student#isComplete(Assignment)} equals true).
     * @param assignment current assignment
     * @param precise true if to be computed
     * @return number of real students with a complete schedule
     */
    public int getNrCompleteRealStudents(Assignment<Request, Enrollment> assignment, boolean precise) {
        if (!precise)
            return getContext(assignment).nrComplete() - getContext(assignment).getNrCompleteLastLikeStudents();
        int nrRealStudents = 0;
        for (Student student : getStudents()) {
            if (student.isComplete(assignment) && !student.isDummy())
                nrRealStudents++;
        }
        return nrRealStudents;
    }

    /**
     * Number of requests from projected ({@link Student#isDummy()} equals true)
     * students.
     * @param precise true if to be computed
     * @return number of requests from projected students 
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
     * @param precise true if to be computed
     * @return number of requests from real students 
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
     * Number of requests from projected ({@link Student#isDummy()} equals true)
     * students that are assigned.
     * @param assignment current assignment
     * @param precise true if to be computed
     * @return number of requests from projected students that are assigned
     */
    public int getNrAssignedLastLikeRequests(Assignment<Request, Enrollment> assignment, boolean precise) {
        if (!precise)
            return getContext(assignment).getNrAssignedLastLikeRequests();
        int nrLastLikeRequests = 0;
        for (Request request : assignment.assignedVariables()) {
            if (request.getStudent().isDummy())
                nrLastLikeRequests++;
        }
        return nrLastLikeRequests;
    }

    /**
     * Number of requests from real ({@link Student#isDummy()} equals false)
     * students that are assigned.
     * @param assignment current assignment
     * @param precise true if to be computed
     * @return number of requests from real students that are assigned
     */
    public int getNrAssignedRealRequests(Assignment<Request, Enrollment> assignment, boolean precise) {
        if (!precise)
            return assignment.nrAssignedVariables() - getContext(assignment).getNrAssignedLastLikeRequests();
        int nrRealRequests = 0;
        for (Request request : assignment.assignedVariables()) {
            if (!request.getStudent().isDummy())
                nrRealRequests++;
        }
        return nrRealRequests;
    }

    /**
     * Model extended info. Some more information (that is more expensive to
     * compute) is added to an ordinary {@link Model#getInfo(Assignment)}.
     */
    @Override
    public Map<String, String> getExtendedInfo(Assignment<Request, Enrollment> assignment) {
        Map<String, String> info = getInfo(assignment);
        /*
        int nrLastLikeStudents = getNrLastLikeStudents(true);
        if (nrLastLikeStudents != 0 && nrLastLikeStudents != getStudents().size()) {
            int nrRealStudents = getStudents().size() - nrLastLikeStudents;
            int nrLastLikeCompleteStudents = getNrCompleteLastLikeStudents(true);
            int nrRealCompleteStudents = getCompleteStudents().size() - nrLastLikeCompleteStudents;
            info.put("Projected students with complete schedule", sDecimalFormat.format(100.0
                    * nrLastLikeCompleteStudents / nrLastLikeStudents)
                    + "% (" + nrLastLikeCompleteStudents + "/" + nrLastLikeStudents + ")");
            info.put("Real students with complete schedule", sDecimalFormat.format(100.0 * nrRealCompleteStudents
                    / nrRealStudents)
                    + "% (" + nrRealCompleteStudents + "/" + nrRealStudents + ")");
            int nrLastLikeRequests = getNrLastLikeRequests(true);
            int nrRealRequests = variables().size() - nrLastLikeRequests;
            int nrLastLikeAssignedRequests = getNrAssignedLastLikeRequests(true);
            int nrRealAssignedRequests = assignedVariables().size() - nrLastLikeAssignedRequests;
            info.put("Projected assigned requests", sDecimalFormat.format(100.0 * nrLastLikeAssignedRequests
                    / nrLastLikeRequests)
                    + "% (" + nrLastLikeAssignedRequests + "/" + nrLastLikeRequests + ")");
            info.put("Real assigned requests", sDecimalFormat.format(100.0 * nrRealAssignedRequests / nrRealRequests)
                    + "% (" + nrRealAssignedRequests + "/" + nrRealRequests + ")");
        }
        */
        // info.put("Average unassigned priority", sDecimalFormat.format(avgUnassignPriority()));
        // info.put("Average number of requests", sDecimalFormat.format(avgNrRequests()));
        
        /*
        double total = 0;
        for (Request r: variables())
            if (r.getAssignment() != null)
                total += r.getWeight() * iStudentWeights.getWeight(r.getAssignment());
        */
        double dc = 0;
        if (getDistanceConflict() != null && getDistanceConflict().getTotalNrConflicts(assignment) != 0) {
            Set<DistanceConflict.Conflict> conf = getDistanceConflict().getAllConflicts(assignment);
            for (DistanceConflict.Conflict c: conf)
                dc += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(assignment, c);
            if (!conf.isEmpty())
                info.put("Student distance conflicts", conf.size() + " (weighted: " + sDecimalFormat.format(dc) + ")");
        }
        double toc = 0;
        if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts(assignment) != 0) {
            Set<TimeOverlapsCounter.Conflict> conf = getTimeOverlaps().getAllConflicts(assignment);
            int share = 0;
            for (TimeOverlapsCounter.Conflict c: conf) {
                toc += c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE1(), c);
                toc += c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE2(), c);
                share += c.getShare();
            }
            if (toc != 0.0)
                info.put("Time overlapping conflicts", share + " (average: " + sDecimalFormat.format(5.0 * share / getStudents().size()) + " min, weighted: " + sDoubleFormat.format(toc) + ")");
        }
        /*
        info.put("Overall solution value", sDecimalFormat.format(total - dc - toc) + (dc == 0.0 && toc == 0.0 ? "" :
            " (" + (dc != 0.0 ? "distance: " + sDecimalFormat.format(dc): "") + (dc != 0.0 && toc != 0.0 ? ", " : "") + 
            (toc != 0.0 ? "overlap: " + sDecimalFormat.format(toc) : "") + ")")
            );
        */
        
        double disbWeight = 0;
        int disbSections = 0;
        int disb10Sections = 0;
        int disb10Limit = getProperties().getPropertyInt("Info.ListDisbalancedSections", 0);
        Set<String> disb10SectionList = (disb10Limit == 0 ? null : new TreeSet<String>()); 
        for (Offering offering: getOfferings()) {
            for (Config config: offering.getConfigs()) {
                double enrl = config.getEnrollmentWeight(assignment, null);
                for (Subpart subpart: config.getSubparts()) {
                    if (subpart.getSections().size() <= 1) continue;
                    if (subpart.getLimit() > 0) {
                        // sections have limits -> desired size is section limit x (total enrollment / total limit)
                        double ratio = enrl / subpart.getLimit();
                        for (Section section: subpart.getSections()) {
                            double desired = ratio * section.getLimit();
                            disbWeight += Math.abs(section.getEnrollmentWeight(assignment, null) - desired);
                            disbSections ++;
                            if (Math.abs(desired - section.getEnrollmentWeight(assignment, null)) >= Math.max(1.0, 0.1 * section.getLimit())) {
                                disb10Sections++;
                                if (disb10SectionList != null)
                                	disb10SectionList.add(section.getSubpart().getConfig().getOffering().getName() + " " + section.getSubpart().getName() + " " + section.getName()); 
                            }
                        }
                    } else {
                        // unlimited sections -> desired size is total enrollment / number of sections
                        for (Section section: subpart.getSections()) {
                            double desired = enrl / subpart.getSections().size();
                            disbWeight += Math.abs(section.getEnrollmentWeight(assignment, null) - desired);
                            disbSections ++;
                            if (Math.abs(desired - section.getEnrollmentWeight(assignment, null)) >= Math.max(1.0, 0.1 * desired)) {
                                disb10Sections++;
                                if (disb10SectionList != null)
                                	disb10SectionList.add(section.getSubpart().getConfig().getOffering().getName() + " " + section.getSubpart().getName() + " " + section.getName());
                            }
                        }
                    }
                }
            }
        }
        if (disbSections != 0) {
            double assignedCRWeight = getContext(assignment).getAssignedCourseRequestWeight();
            info.put("Average disbalance", sDecimalFormat.format(disbWeight / disbSections) + " (" + sDecimalFormat.format(assignedCRWeight == 0 ? 0.0 : 100.0 * disbWeight / assignedCRWeight) + "%)");
            String list = "";
            if (disb10SectionList != null) {
                int i = 0;
                for (String section: disb10SectionList) {
                    if (i == disb10Limit) {
                        list += "<br>...";
                        break;
                    }
                    list += "<br>" + section;
                    i++;
                }
            }
            info.put("Sections disbalanced by 10% or more", disb10Sections + " (" + sDecimalFormat.format(disbSections == 0 ? 0.0 : 100.0 * disb10Sections / disbSections) + "%)" + list);
        }
        return info;
    }
    
    @Override
    public void restoreBest(Assignment<Request, Enrollment> assignment) {
        restoreBest(assignment, new Comparator<Request>() {
            @Override
            public int compare(Request r1, Request r2) {
                Enrollment e1 = r1.getBestAssignment();
                Enrollment e2 = r2.getBestAssignment();
                // Reservations first
                if (e1.getReservation() != null && e2.getReservation() == null) return -1;
                if (e1.getReservation() == null && e2.getReservation() != null) return 1;
                // Then assignment iteration (i.e., order in which assignments were made)
                if (r1.getBestAssignmentIteration() != r2.getBestAssignmentIteration())
                    return (r1.getBestAssignmentIteration() < r2.getBestAssignmentIteration() ? -1 : 1);
                // Then student and priority
                return r1.compareTo(r2);
            }
        });
    }
        
    @Override
    public String toString(Assignment<Request, Enrollment> assignment) {
        return   (getNrRealStudents(false) > 0 ? "RRq:" + getNrAssignedRealRequests(assignment, false) + "/" + getNrRealRequests(false) + ", " : "")
                + (getNrLastLikeStudents(false) > 0 ? "DRq:" + getNrAssignedLastLikeRequests(assignment, false) + "/" + getNrLastLikeRequests(false) + ", " : "")
                + (getNrRealStudents(false) > 0 ? "RS:" + getNrCompleteRealStudents(assignment, false) + "/" + getNrRealStudents(false) + ", " : "")
                + (getNrLastLikeStudents(false) > 0 ? "DS:" + getNrCompleteLastLikeStudents(assignment, false) + "/" + getNrLastLikeStudents(false) + ", " : "")
                + "V:"
                + sDecimalFormat.format(-getTotalValue(assignment))
                + (getDistanceConflict() == null ? "" : ", DC:" + getDistanceConflict().getTotalNrConflicts(assignment))
                + (getTimeOverlaps() == null ? "" : ", TOC:" + getTimeOverlaps().getTotalNrConflicts(assignment))
                + ", %:" + sDecimalFormat.format(-100.0 * getTotalValue(assignment) / (getStudents().size() - iNrDummyStudents + 
                        (iProjectedStudentWeight < 0.0 ? iNrDummyStudents * (iTotalDummyWeight / iNrDummyRequests) :iProjectedStudentWeight * iTotalDummyWeight)));

    }
    
    /**
     * Quadratic average of two weights.
     * @param w1 first weight
     * @param w2 second weight
     * @return average of the two weights
     */
    public double avg(double w1, double w2) {
        return Math.sqrt(w1 * w2);
    }

    /**
     * Maximal domain size (i.e., number of enrollments of a course request), -1 if there is no limit.
     * @return maximal domain size, -1 if unlimited
     */
    public int getMaxDomainSize() { return iMaxDomainSize; }

    /**
     * Maximal domain size (i.e., number of enrollments of a course request), -1 if there is no limit.
     * @param maxDomainSize maximal domain size, -1 if unlimited
     */
    public void setMaxDomainSize(int maxDomainSize) { iMaxDomainSize = maxDomainSize; }
    

    @Override
    public StudentSectioningModelContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new StudentSectioningModelContext(assignment);
    }
    
    public class StudentSectioningModelContext implements AssignmentConstraintContext<Request, Enrollment>, InfoProvider<Request, Enrollment>{
        private Set<Student> iCompleteStudents = new HashSet<Student>();
        private double iTotalValue = 0.0;
        private int iNrAssignedDummyRequests = 0, iNrCompleteDummyStudents = 0;
        private double iAssignedCRWeight = 0.0, iAssignedDummyCRWeight = 0.0;
        private double iReservedSpace = 0.0;

        public StudentSectioningModelContext(Assignment<Request, Enrollment> assignment) {
            for (Request request: variables()) {
                Enrollment enrollment = assignment.getValue(request);
                if (enrollment != null)
                    assigned(assignment, enrollment);
            }
        }

        /**
         * Called after an enrollment was assigned to a request. The list of
         * complete students and the overall solution value are updated.
         */
        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            Student student = enrollment.getStudent();
            if (student.isComplete(assignment))
                iCompleteStudents.add(student);
            double value = enrollment.getRequest().getWeight() * iStudentWeights.getWeight(assignment, enrollment);
            iTotalValue -= value;
            enrollment.variable().getContext(assignment).setLastWeight(value);
            if (enrollment.isCourseRequest())
                iAssignedCRWeight += enrollment.getRequest().getWeight();
            if (enrollment.getReservation() != null)
                iReservedSpace += enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && ((CourseRequest)enrollment.getRequest()).hasReservations())
                iTotalReservedSpace += enrollment.getRequest().getWeight();
            if (student.isDummy()) {
                iNrAssignedDummyRequests++;
                if (enrollment.isCourseRequest())
                    iAssignedDummyCRWeight += enrollment.getRequest().getWeight();
                if (student.isComplete(assignment))
                    iNrCompleteDummyStudents++;
            }
        }

        /**
         * Called before an enrollment was unassigned from a request. The list of
         * complete students and the overall solution value are updated.
         */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            Student student = enrollment.getStudent();
            if (iCompleteStudents.contains(student) && !student.isComplete(assignment)) {
                iCompleteStudents.remove(student);
                if (student.isDummy())
                    iNrCompleteDummyStudents--;
            }
            Request.RequestContext cx = enrollment.variable().getContext(assignment);
            Double value = cx.getLastWeight();
            if (value == null)
                value = enrollment.getRequest().getWeight() * iStudentWeights.getWeight(assignment, enrollment);
            iTotalValue += value;
            cx.setLastWeight(null);
            if (enrollment.isCourseRequest())
                iAssignedCRWeight -= enrollment.getRequest().getWeight();
            if (enrollment.getReservation() != null)
                iReservedSpace -= enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && ((CourseRequest)enrollment.getRequest()).hasReservations())
                iTotalReservedSpace -= enrollment.getRequest().getWeight();
            if (student.isDummy()) {
                iNrAssignedDummyRequests--;
                if (enrollment.isCourseRequest())
                    iAssignedDummyCRWeight -= enrollment.getRequest().getWeight();
            }
        }
        
        public void add(Assignment<Request, Enrollment> assignment, DistanceConflict.Conflict c) {
            iTotalValue += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(assignment, c);
        }

        public void remove(Assignment<Request, Enrollment> assignment, DistanceConflict.Conflict c) {
            iTotalValue -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(assignment, c);
        }
        
        public void add(Assignment<Request, Enrollment> assignment, TimeOverlapsCounter.Conflict c) {
            iTotalValue += c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE1(), c);
            iTotalValue += c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE2(), c);
        }

        public void remove(Assignment<Request, Enrollment> assignment, TimeOverlapsCounter.Conflict c) {
            iTotalValue -= c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE1(), c);
            iTotalValue -= c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE2(), c);
        }
        
        /**
         * Students with complete schedules (see {@link Student#isComplete(Assignment)})
         * @return students with complete schedule
         */
        public Set<Student> getCompleteStudents() {
            return iCompleteStudents;
        }
        
        /**
         * Number of students with complete schedule
         * @return number of students with complete schedule
         */
        public int nrComplete() {
            return getCompleteStudents().size();
        }
        
        /** 
         * Recompute cached request weights
         * @param assignment curent assignment
         */
        public void requestWeightsChanged(Assignment<Request, Enrollment> assignment) {
            iTotalCRWeight = 0.0;
            iTotalDummyWeight = 0.0; iTotalDummyCRWeight = 0.0;
            iAssignedCRWeight = 0.0;
            iAssignedDummyCRWeight = 0.0;
            iNrDummyRequests = 0; iNrAssignedDummyRequests = 0;
            iTotalReservedSpace = 0.0; iReservedSpace = 0.0;
            for (Request request: variables()) {
                boolean cr = (request instanceof CourseRequest);
                if (cr)
                    iTotalCRWeight += request.getWeight();
                if (request.getStudent().isDummy()) {
                    iTotalDummyWeight += request.getWeight();
                    iNrDummyRequests ++;
                    if (cr)
                        iTotalDummyCRWeight += request.getWeight();
                }
                if (assignment.getValue(request) != null) {
                    if (cr)
                        iAssignedCRWeight += request.getWeight();
                    if (assignment.getValue(request).getReservation() != null)
                        iReservedSpace += request.getWeight();
                    if (cr && ((CourseRequest)request).hasReservations())
                        iTotalReservedSpace += request.getWeight();
                    if (request.getStudent().isDummy()) {
                        iNrAssignedDummyRequests ++;
                        if (cr)
                            iAssignedDummyCRWeight += request.getWeight();
                    }
                }
            }
        }
        
        /**
         * Overall solution value
         * @return solution value
         */
        public double getTotalValue() {
            return iTotalValue;
        }
        
        /**
         * Number of last like ({@link Student#isDummy()} equals true) students with
         * a complete schedule ({@link Student#isComplete(Assignment)} equals true).
         * @return number of last like (projected) students with a complete schedule
         */
        public int getNrCompleteLastLikeStudents() {
            return iNrCompleteDummyStudents;
        }
        
        /**
         * Number of requests from projected ({@link Student#isDummy()} equals true)
         * students that are assigned.
         * @return number of real students with a complete schedule
         */
        public int getNrAssignedLastLikeRequests() {
            return iNrAssignedDummyRequests;
        }

        @Override
        public void getInfo(Assignment<Request, Enrollment> assignment, Map<String, String> info) {
            if (iTotalCRWeight > 0.0) {
                info.put("Assigned course requests", sDecimalFormat.format(100.0 * iAssignedCRWeight / iTotalCRWeight) + "% (" + (int)Math.round(iAssignedCRWeight) + "/" + (int)Math.round(iTotalCRWeight) + ")");
                if (getNrLastLikeStudents(false) != getStudents().size() && iTotalCRWeight != iTotalDummyCRWeight) {
                    if (iTotalDummyCRWeight > 0.0)
                        info.put("Projected assigned course requests", sDecimalFormat.format(100.0 * iAssignedDummyCRWeight / iTotalDummyCRWeight) + "% (" + (int)Math.round(iAssignedDummyCRWeight) + "/" + (int)Math.round(iTotalDummyCRWeight) + ")");
                    info.put("Real assigned course requests", sDecimalFormat.format(100.0 * (iAssignedCRWeight - iAssignedDummyCRWeight) / (iTotalCRWeight - iTotalDummyCRWeight)) +
                            "% (" + (int)Math.round(iAssignedCRWeight - iAssignedDummyCRWeight) + "/" + (int)Math.round(iTotalCRWeight - iTotalDummyCRWeight) + ")");
                }
            }
            if (iTotalReservedSpace > 0.0)
                info.put("Reservations", sDoubleFormat.format(100.0 * iReservedSpace / iTotalReservedSpace) + "% (" + Math.round(iReservedSpace) + "/" + Math.round(iTotalReservedSpace) + ")"); 
        }

        @Override
        public void getInfo(Assignment<Request, Enrollment> assignment, Map<String, String> info, Collection<Request> variables) {
        }
        
        public double getAssignedCourseRequestWeight() {
            return iAssignedCRWeight;
        }
    }
    
    @Override
    public InheritedAssignment<Request, Enrollment> createInheritedAssignment(Solution<Request, Enrollment> solution, int index) {
        return new OptimisticInheritedAssignment<>(solution, index);
    }

}
