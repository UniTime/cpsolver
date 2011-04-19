package net.sf.cpsolver.studentsct;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.constraint.ConfigLimit;
import net.sf.cpsolver.studentsct.constraint.CourseLimit;
import net.sf.cpsolver.studentsct.constraint.ReservationLimit;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;
import net.sf.cpsolver.studentsct.reservation.Reservation;
import net.sf.cpsolver.studentsct.weights.PriorityStudentWeights;
import net.sf.cpsolver.studentsct.weights.StudentWeights;

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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class StudentSectioningModel extends Model<Request, Enrollment> {
    private static Logger sLog = Logger.getLogger(StudentSectioningModel.class);
    protected static DecimalFormat sDecimalFormat = new DecimalFormat("0.000");
    private List<Student> iStudents = new ArrayList<Student>();
    private List<Offering> iOfferings = new ArrayList<Offering>();
    private Set<Student> iCompleteStudents = new java.util.HashSet<Student>();
    private double iTotalValue = 0.0;
    private DataProperties iProperties;
    private DistanceConflict iDistanceConflict = null;
    private TimeOverlapsCounter iTimeOverlaps = null;
    private int iNrDummyStudents = 0, iNrDummyRequests = 0, iNrAssignedDummyRequests = 0, iNrCompleteDummyStudents = 0;
    private double iTotalWeight = 0.0, iTotalDummyWeight = 0.0, iAssignedWeight = 0.0, iAssignedDummyWeight = 0.0;
    private double iTotalCRWeight = 0.0, iTotalDummyCRWeight = 0.0, iAssignedCRWeight = 0.0, iAssignedDummyCRWeight = 0.0;
    private StudentWeights iStudentWeights = null;
    private boolean iReservationCanAssignOverTheLimit;
    protected double iProjectedStudentWeight = 0.0100;


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
        iAssignedVariables = new HashSet<Request>();
        iUnassignedVariables = new HashSet<Request>();
        iPerturbVariables = new HashSet<Request>();
        iStudentWeights = new PriorityStudentWeights(properties);
        if (properties.getPropertyBoolean("Sectioning.SectionLimit", true)) {
            SectionLimit sectionLimit = new SectionLimit(properties);
            addGlobalConstraint(sectionLimit);
            if (properties.getPropertyBoolean("Sectioning.SectionLimit.Debug", false)) {
                sectionLimit.addConstraintListener(new ConstraintListener<Enrollment>() {
                    @Override
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

                    @Override
                    public void constraintAfterAssigned(long iteration, Constraint<?, Enrollment> constraint,
                            Enrollment assigned, Set<Enrollment> unassigned) {
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
     */
    public boolean getReservationCanAssignOverTheLimit() {
        return iReservationCanAssignOverTheLimit;
    }
    
    /**
     * Return student weighting model
     */
    public StudentWeights getStudentWeights() {
        return iStudentWeights;
    }

    /**
     * Set student weighting model
     */
    public void setStudentWeights(StudentWeights weights) {
        iStudentWeights = weights;
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
        for (Request request : student.getRequests())
            addVariable(request);
        if (getProperties().getPropertyBoolean("Sectioning.StudentConflict", true)) {
            addConstraint(new StudentConflict(student));
        }
        if (student.isComplete())
            iCompleteStudents.add(student);
    }
    
    @Override
    public void addVariable(Request request) {
        super.addVariable(request);
        iTotalWeight += request.getWeight();
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
     */
    public void requestWeightsChanged() {
        iTotalWeight = 0.0; iTotalCRWeight = 0.0;
        iTotalDummyWeight = 0.0; iTotalDummyCRWeight = 0.0;
        iAssignedWeight = 0.0; iAssignedCRWeight = 0.0;
        iAssignedDummyWeight = 0.0; iAssignedDummyCRWeight = 0.0;
        iNrDummyRequests = 0; iNrAssignedDummyRequests = 0;
        for (Request request: variables()) {
            boolean cr = (request instanceof CourseRequest);
            iTotalWeight += request.getWeight();
            if (cr)
                iTotalCRWeight += request.getWeight();
            if (request.getStudent().isDummy()) {
                iTotalDummyWeight += request.getWeight();
                iNrDummyRequests ++;
                if (cr)
                    iTotalDummyCRWeight += request.getWeight();
            }
            if (request.getAssignment() != null) {
                iAssignedWeight += request.getWeight();
                if (cr)
                    iAssignedCRWeight += request.getWeight();
                if (request.getStudent().isDummy()) {
                    iNrAssignedDummyRequests ++;
                    iAssignedDummyWeight += request.getWeight();
                    if (cr)
                        iAssignedDummyCRWeight += request.getWeight();
                }
            }
        }
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
        iTotalWeight -= request.getWeight();
        if (request instanceof CourseRequest)
            iTotalCRWeight -= request.getWeight();
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
    public Map<String, String> getInfo() {
        Map<String, String> info = super.getInfo();
        if (!getStudents().isEmpty())
            info.put("Students with complete schedule", sDoubleFormat.format(100.0 * nrComplete() / getStudents().size())
                    + "% (" + nrComplete() + "/" + getStudents().size() + ")");
        if (getDistanceConflict() != null && getDistanceConflict().getTotalNrConflicts() != 0)
            info.put("Student distance conflicts", String.valueOf(getDistanceConflict().getTotalNrConflicts()));
        if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts() != 0)
            info.put("Time overlapping conflicts", String.valueOf(getTimeOverlaps().getTotalNrConflicts()));
        int nrLastLikeStudents = getNrLastLikeStudents(false);
        if (nrLastLikeStudents != 0 && nrLastLikeStudents != getStudents().size()) {
            int nrRealStudents = getStudents().size() - nrLastLikeStudents;
            int nrLastLikeCompleteStudents = getNrCompleteLastLikeStudents(false);
            int nrRealCompleteStudents = getCompleteStudents().size() - nrLastLikeCompleteStudents;
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
            int nrLastLikeAssignedRequests = getNrAssignedLastLikeRequests(false);
            int nrRealAssignedRequests = assignedVariables().size() - nrLastLikeAssignedRequests;
            if (nrLastLikeRequests > 0)
                info.put("Projected assigned requests", sDecimalFormat.format(100.0 * nrLastLikeAssignedRequests / nrLastLikeRequests)
                        + "% (" + nrLastLikeAssignedRequests + "/" + nrLastLikeRequests + ")");
            if (nrRealRequests > 0)
                info.put("Real assigned requests", sDecimalFormat.format(100.0 * nrRealAssignedRequests / nrRealRequests)
                        + "% (" + nrRealAssignedRequests + "/" + nrRealRequests + ")");
            if (iTotalCRWeight > 0.0) {
                info.put("Assigned course requests", sDecimalFormat.format(100.0 * iAssignedCRWeight / iTotalCRWeight) + "% (" + (int)Math.round(iAssignedCRWeight) + "/" + (int)Math.round(iTotalCRWeight) + ")");
                if (iTotalDummyCRWeight != iTotalCRWeight) {
                    if (iTotalDummyCRWeight > 0.0)
                        info.put("Projected assigned course requests", sDecimalFormat.format(100.0 * iAssignedDummyCRWeight / iTotalDummyCRWeight) + "% (" + (int)Math.round(iAssignedDummyCRWeight) + "/" + (int)Math.round(iTotalDummyCRWeight) + ")");
                    info.put("Real assigned course requests", sDecimalFormat.format(100.0 * (iAssignedCRWeight - iAssignedDummyCRWeight) / (iTotalCRWeight - iTotalDummyCRWeight)) +
                            "% (" + (int)Math.round(iAssignedCRWeight - iAssignedDummyCRWeight) + "/" + (int)Math.round(iTotalCRWeight - iTotalDummyCRWeight) + ")");
                }
            }
            if (getDistanceConflict() != null && getDistanceConflict().getTotalNrConflicts() > 0)
                info.put("Student distance conflicts", String.valueOf(getDistanceConflict().getTotalNrConflicts()));
            if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts() > 0)
                info.put("Time overlapping conflicts", String.valueOf(getTimeOverlaps().getTotalNrConflicts()));
        }
        return info;
    }

    /**
     * Overall solution value
     */
    public double getTotalValue(boolean precise) {
        if (precise) {
            double total = 0;
            for (Request r: assignedVariables())
                total += r.getWeight() * iStudentWeights.getWeight(r.getAssignment());
            if (iDistanceConflict != null)
                for (DistanceConflict.Conflict c: iDistanceConflict.computeAllConflicts())
                    total -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(c);
            if (iTimeOverlaps != null)
                for (TimeOverlapsCounter.Conflict c: iTimeOverlaps.computeAllConflicts()) {
                    total -= c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE1(), c);
                    total -= c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE2(), c);
                }
            return -total;
        }
        return iTotalValue;
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
        double value = enrollment.getRequest().getWeight() * iStudentWeights.getWeight(enrollment);
        iTotalValue -= value;
        enrollment.setExtra(value);
        iAssignedWeight += enrollment.getRequest().getWeight();
        if (enrollment.isCourseRequest())
            iAssignedCRWeight += enrollment.getRequest().getWeight();
        if (student.isDummy()) {
            iNrAssignedDummyRequests++;
            iAssignedDummyWeight += enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest())
                iAssignedDummyCRWeight += enrollment.getRequest().getWeight();
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
        Double value = (Double)enrollment.getExtra();
        if (value == null)
            value = enrollment.getRequest().getWeight() * iStudentWeights.getWeight(enrollment);
        iTotalValue += value;
        enrollment.setExtra(null);
        iAssignedWeight -= enrollment.getRequest().getWeight();
        if (enrollment.isCourseRequest())
            iAssignedCRWeight -= enrollment.getRequest().getWeight();
        if (student.isDummy()) {
            iNrAssignedDummyRequests--;
            iAssignedDummyWeight -= enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest())
                iAssignedDummyCRWeight -= enrollment.getRequest().getWeight();
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
     * Set time overlaps extension
     */
    public void setTimeOverlaps(TimeOverlapsCounter toc) {
        iTimeOverlaps = toc;
    }

    /**
     * Return time overlaps extension
     */
    public TimeOverlapsCounter getTimeOverlaps() {
        return iTimeOverlaps;
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
     * Number of requests from projected ({@link Student#isDummy()} equals true)
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
     * Number of requests from projected ({@link Student#isDummy()} equals true)
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
    public Map<String, String> getExtendedInfo() {
        Map<String, String> info = getInfo();
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
        if (getDistanceConflict() != null && getDistanceConflict().getTotalNrConflicts() != 0) {
            Set<DistanceConflict.Conflict> conf = getDistanceConflict().getAllConflicts();
            for (DistanceConflict.Conflict c: conf)
                dc += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(c);
            if (!conf.isEmpty())
                info.put("Student distance conflicts", conf.size() + " (weighted: " + sDecimalFormat.format(dc) + ")");
        }
        double toc = 0;
        if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts() != 0) {
            Set<TimeOverlapsCounter.Conflict> conf = getTimeOverlaps().getAllConflicts();
            int share = 0;
            for (TimeOverlapsCounter.Conflict c: conf) {
                toc += c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE1(), c);
                toc += c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE2(), c);
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
        for (Offering offering: getOfferings()) {
            for (Config config: offering.getConfigs()) {
                double enrl = config.getEnrollmentWeight(null);
                for (Subpart subpart: config.getSubparts()) {
                    if (subpart.getSections().size() <= 1) continue;
                    if (subpart.getLimit() > 0) {
                        // sections have limits -> desired size is section limit x (total enrollment / total limit)
                        double ratio = enrl / subpart.getLimit();
                        for (Section section: subpart.getSections()) {
                            double desired = ratio * section.getLimit();
                            disbWeight += Math.abs(section.getEnrollmentWeight(null) - desired);
                            disbSections ++;
                            if (Math.abs(desired - section.getEnrollmentWeight(null)) >= Math.max(1.0, 0.1 * section.getLimit()))
                                disb10Sections++;
                        }
                    } else {
                        // unlimited sections -> desired size is total enrollment / number of sections
                        for (Section section: subpart.getSections()) {
                            double desired = enrl / subpart.getSections().size();
                            disbWeight += Math.abs(section.getEnrollmentWeight(null) - desired);
                            disbSections ++;
                            if (Math.abs(desired - section.getEnrollmentWeight(null)) >= Math.max(1.0, 0.1 * desired))
                                disb10Sections++;
                        }
                    }
                }
            }
        }
        if (disbSections != 0) {
            info.put("Average disbalance", sDecimalFormat.format(disbWeight / disbSections) +
                    " (" + sDecimalFormat.format(iAssignedCRWeight == 0 ? 0.0 : 100.0 * disbWeight / iAssignedCRWeight) + "%)");
            info.put("Sections disbalanced by 10% or more", disb10Sections + " (" + sDecimalFormat.format(disbSections == 0 ? 0.0 : 100.0 * disb10Sections / disbSections) + "%)");
        }
        return info;
    }
    
    @Override
    public void restoreBest() {
        for (Request r: variables())
            if (r.getAssignment() != null) r.unassign(0);
        for (Student s: getStudents())
            for (Request r: s.getRequests())
                if (r.getBestAssignment() != null) r.assign(0, r.getBestAssignment());
    }
        
    @Override
    public String toString() {
        return   (getNrRealStudents(false) > 0 ? "RRq:" + getNrAssignedRealRequests(false) + "/" + getNrRealRequests(false) + ", " : "")
                + (getNrLastLikeStudents(false) > 0 ? "DRq:" + getNrAssignedLastLikeRequests(false) + "/" + getNrLastLikeRequests(false) + ", " : "")
                + (getNrRealStudents(false) > 0 ? "RS:" + getNrCompleteRealStudents(false) + "/" + getNrRealStudents(false) + ", " : "")
                + (getNrLastLikeStudents(false) > 0 ? "DS:" + getNrCompleteLastLikeStudents(false) + "/" + getNrLastLikeStudents(false) + ", " : "")
                + "V:"
                + sDecimalFormat.format(-getTotalValue())
                + (getDistanceConflict() == null ? "" : ", DC:" + getDistanceConflict().getTotalNrConflicts())
                + (getTimeOverlaps() == null ? "" : ", TOC:" + getTimeOverlaps().getTotalNrConflicts())
                + ", %:" + sDecimalFormat.format(-100.0 * getTotalValue() / (getStudents().size() - iNrDummyStudents + 
                        (iProjectedStudentWeight < 0.0 ? iNrDummyStudents * (iTotalDummyWeight / iNrDummyRequests) :iProjectedStudentWeight * iTotalDummyWeight)));

    }
    
    /**
     * Quadratic average of two weights.
     */
    public double avg(double w1, double w2) {
        return Math.sqrt(w1 * w2);
    }

    public void add(DistanceConflict.Conflict c) {
        iTotalValue += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(c);
    }

    public void remove(DistanceConflict.Conflict c) {
        iTotalValue -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(c);
    }
    
    public void add(TimeOverlapsCounter.Conflict c) {
        iTotalValue += c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE1(), c);
        iTotalValue += c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE2(), c);
    }

    public void remove(TimeOverlapsCounter.Conflict c) {
        iTotalValue -= c.getR1().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE1(), c);
        iTotalValue -= c.getR2().getWeight() * iStudentWeights.getTimeOverlapConflictWeight(c.getE2(), c);
    }
}
