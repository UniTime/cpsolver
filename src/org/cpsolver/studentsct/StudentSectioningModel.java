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

import org.apache.logging.log4j.Logger;
import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.assignment.OptimisticInheritedAssignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.assignment.context.ModelWithContext;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.ConstraintListener;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.studentsct.constraint.CancelledSections;
import org.cpsolver.studentsct.constraint.ConfigLimit;
import org.cpsolver.studentsct.constraint.CourseLimit;
import org.cpsolver.studentsct.constraint.DisabledSections;
import org.cpsolver.studentsct.constraint.FixInitialAssignments;
import org.cpsolver.studentsct.constraint.HardDistanceConflicts;
import org.cpsolver.studentsct.constraint.LinkedSections;
import org.cpsolver.studentsct.constraint.RequiredReservation;
import org.cpsolver.studentsct.constraint.RequiredRestrictions;
import org.cpsolver.studentsct.constraint.RequiredSections;
import org.cpsolver.studentsct.constraint.ReservationLimit;
import org.cpsolver.studentsct.constraint.SectionLimit;
import org.cpsolver.studentsct.constraint.StudentConflict;
import org.cpsolver.studentsct.constraint.StudentNotAvailable;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.RequestGroup;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.model.Unavailability;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Student.BackToBackPreference;
import org.cpsolver.studentsct.model.Student.ModalityPreference;
import org.cpsolver.studentsct.model.Student.StudentPriority;
import org.cpsolver.studentsct.reservation.Reservation;
import org.cpsolver.studentsct.weights.PriorityStudentWeights;
import org.cpsolver.studentsct.weights.StudentWeights;

/**
 * Student sectioning model.
 * 
 * <br>
 * <br>
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
public class StudentSectioningModel extends ModelWithContext<Request, Enrollment, StudentSectioningModel.StudentSectioningModelContext> implements CanInheritContext<Request, Enrollment, StudentSectioningModel.StudentSectioningModelContext> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(StudentSectioningModel.class);
    protected static DecimalFormat sDecimalFormat = new DecimalFormat("0.00");
    private List<Student> iStudents = new ArrayList<Student>();
    private List<Offering> iOfferings = new ArrayList<Offering>();
    private List<LinkedSections> iLinkedSections = new ArrayList<LinkedSections>();
    private DataProperties iProperties;
    private DistanceConflict iDistanceConflict = null;
    private TimeOverlapsCounter iTimeOverlaps = null;
    private StudentQuality iStudentQuality = null;
    private int iNrDummyStudents = 0, iNrDummyRequests = 0;
    private int[] iNrPriorityStudents = null;
    private double iTotalDummyWeight = 0.0;
    private double iTotalCRWeight = 0.0, iTotalDummyCRWeight = 0.0;
    private double[] iTotalPriorityCRWeight = null;
    private double[] iTotalCriticalCRWeight;
    private double[][] iTotalPriorityCriticalCRWeight;
    private double iTotalMPPCRWeight = 0.0;
    private double iTotalSelCRWeight = 0.0;
    private double iBestAssignedCourseRequestWeight = 0.0;
    private StudentWeights iStudentWeights = null;
    private boolean iReservationCanAssignOverTheLimit;
    private boolean iMPP;
    private boolean iKeepInitials;
    protected double iProjectedStudentWeight = 0.0100;
    private int iMaxDomainSize = -1; 
    private int iDayOfWeekOffset = 0;


    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    @SuppressWarnings("unchecked")
    public StudentSectioningModel(DataProperties properties) {
        super();
        iTotalCriticalCRWeight = new double[RequestPriority.values().length];
        iTotalPriorityCriticalCRWeight = new double[RequestPriority.values().length][StudentPriority.values().length];
        for (int i = 0; i < RequestPriority.values().length; i++) {
            iTotalCriticalCRWeight[i] = 0.0;
            for (int j = 0; j < StudentPriority.values().length; j++) {
                iTotalPriorityCriticalCRWeight[i][j] = 0.0;
            }
        }
        iNrPriorityStudents = new int[StudentPriority.values().length];
        iTotalPriorityCRWeight = new double[StudentPriority.values().length];
        for (int i = 0; i < StudentPriority.values().length; i++) {
            iNrPriorityStudents[i] = 0;
            iTotalPriorityCRWeight[i] = 0.0;
        }
        iReservationCanAssignOverTheLimit =  properties.getPropertyBoolean("Reservation.CanAssignOverTheLimit", false);
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        iKeepInitials = properties.getPropertyBoolean("Sectioning.KeepInitialAssignments", false);
        iStudentWeights = new PriorityStudentWeights(properties);
        iMaxDomainSize = properties.getPropertyInt("Sectioning.MaxDomainSize", iMaxDomainSize);
        iDayOfWeekOffset = properties.getPropertyInt("DatePattern.DayOfWeekOffset", 0);
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
        if (properties.getPropertyBoolean("Sectioning.CancelledSections", true)) {
            CancelledSections cancelledSections = new CancelledSections();
            addGlobalConstraint(cancelledSections);
        }
        if (properties.getPropertyBoolean("Sectioning.StudentNotAvailable", true)) {
            StudentNotAvailable studentNotAvailable = new StudentNotAvailable();
            addGlobalConstraint(studentNotAvailable);
        }
        if (properties.getPropertyBoolean("Sectioning.DisabledSections", true)) {
            DisabledSections disabledSections = new DisabledSections();
            addGlobalConstraint(disabledSections);
        }
        if (properties.getPropertyBoolean("Sectioning.RequiredSections", true)) {
            RequiredSections requiredSections = new RequiredSections();
            addGlobalConstraint(requiredSections);
        }
        if (properties.getPropertyBoolean("Sectioning.RequiredRestrictions", true)) {
            RequiredRestrictions requiredRestrictions = new RequiredRestrictions();
            addGlobalConstraint(requiredRestrictions);
        }
        if (properties.getPropertyBoolean("Sectioning.HardDistanceConflict", false)) {
            HardDistanceConflicts hardDistanceConflicts = new HardDistanceConflicts();
            addGlobalConstraint(hardDistanceConflicts);
        }
        if (iMPP && iKeepInitials) {
            addGlobalConstraint(new FixInitialAssignments());
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
     * Return true if the problem is minimal perturbation problem 
     * @return true if MPP is enabled
     */
    public boolean isMPP() {
        return iMPP;
    }
    
    /**
     * Return true if the inital assignments are to be kept unchanged 
     * @return true if the initial assignments are to be kept at all cost
     */
    public boolean getKeepInitialAssignments() {
        return iKeepInitials;
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
        iNrPriorityStudents[student.getPriority().ordinal()]++;
        for (Request request : student.getRequests())
            addVariable(request);
        if (getProperties().getPropertyBoolean("Sectioning.StudentConflict", true)) {
            addConstraint(new StudentConflict(student));
        }
    }
    
    public int getNbrStudents(StudentPriority priority) {
        return iNrPriorityStudents[priority.ordinal()];
    }
    
    @Override
    public void addVariable(Request request) {
        super.addVariable(request);
        if (request instanceof CourseRequest && !request.isAlternative())
            iTotalCRWeight += request.getWeight();
        if (request instanceof CourseRequest && request.getRequestPriority() != RequestPriority.Normal && !request.getStudent().isDummy() && !request.isAlternative())
            iTotalCriticalCRWeight[request.getRequestPriority().ordinal()] += request.getWeight();
        if (request instanceof CourseRequest && request.getRequestPriority() != RequestPriority.Normal && !request.isAlternative())
            iTotalPriorityCriticalCRWeight[request.getRequestPriority().ordinal()][request.getStudent().getPriority().ordinal()] += request.getWeight();
        if (request.getStudent().isDummy()) {
            iNrDummyRequests++;
            iTotalDummyWeight += request.getWeight();
            if (request instanceof CourseRequest && !request.isAlternative())
                iTotalDummyCRWeight += request.getWeight();
        }
        if (request instanceof CourseRequest && !request.isAlternative())
            iTotalPriorityCRWeight[request.getStudent().getPriority().ordinal()] += request.getWeight();
        if (request.isMPP())
            iTotalMPPCRWeight += request.getWeight();
        if (request.hasSelection())
            iTotalSelCRWeight += request.getWeight();
    }
    
    public void setCourseRequestPriority(CourseRequest request, RequestPriority priority) {
        if (request.getRequestPriority() != RequestPriority.Normal && !request.getStudent().isDummy() && !request.isAlternative())
            iTotalCriticalCRWeight[request.getRequestPriority().ordinal()] -= request.getWeight();
        if (request.getRequestPriority() != RequestPriority.Normal && !request.isAlternative())
            iTotalPriorityCriticalCRWeight[request.getRequestPriority().ordinal()][request.getStudent().getPriority().ordinal()] -= request.getWeight();
        request.setRequestPriority(priority);
        if (request.getRequestPriority() != RequestPriority.Normal && !request.getStudent().isDummy() && !request.isAlternative())
            iTotalCriticalCRWeight[request.getRequestPriority().ordinal()] += request.getWeight();
        if (request.getRequestPriority() != RequestPriority.Normal && !request.isAlternative())
            iTotalPriorityCriticalCRWeight[request.getRequestPriority().ordinal()][request.getStudent().getPriority().ordinal()] += request.getWeight();
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
        iNrPriorityStudents[student.getPriority().ordinal()]--;
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
            if (request instanceof CourseRequest && !request.isAlternative())
                iTotalDummyCRWeight -= request.getWeight();
        }
        if (request instanceof CourseRequest && !request.isAlternative())
            iTotalPriorityCRWeight[request.getStudent().getPriority().ordinal()] -= request.getWeight();
        if (request.isMPP())
            iTotalMPPCRWeight -= request.getWeight();
        if (request.hasSelection())
            iTotalSelCRWeight -= request.getWeight();
        if (request instanceof CourseRequest && !request.isAlternative())
            iTotalCRWeight -= request.getWeight();
        if (request instanceof CourseRequest && request.getRequestPriority() != RequestPriority.Normal && !request.getStudent().isDummy() && !request.isAlternative())
            iTotalCriticalCRWeight[request.getRequestPriority().ordinal()] -= request.getWeight();
        if (request instanceof CourseRequest && request.getRequestPriority() != RequestPriority.Normal && !request.isAlternative())
            iTotalPriorityCriticalCRWeight[request.getRequestPriority().ordinal()][request.getStudent().getPriority().ordinal()] -= request.getWeight();
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
     * @param mustBeUsed if true,  a pair of linked sections must be used when a student requests both courses 
     * @param sections a linked section constraint to be added into the problem
     */
    public void addLinkedSections(boolean mustBeUsed, Section... sections) {
        LinkedSections constraint = new LinkedSections(sections);
        constraint.setMustBeUsed(mustBeUsed);
        iLinkedSections.add(constraint);
        constraint.createConstraints();
    }
    
    /**
     * Link sections using {@link LinkedSections}
     * @param sections a linked section constraint to be added into the problem
     */
    @Deprecated
    public void addLinkedSections(Section... sections) {
        addLinkedSections(false, sections);
    }

    /**
     * Link sections using {@link LinkedSections}
     * @param mustBeUsed if true,  a pair of linked sections must be used when a student requests both courses 
     * @param sections a linked section constraint to be added into the problem
     */
    public void addLinkedSections(boolean mustBeUsed, Collection<Section> sections) {
        LinkedSections constraint = new LinkedSections(sections);
        constraint.setMustBeUsed(mustBeUsed);
        iLinkedSections.add(constraint);
        constraint.createConstraints();
    }
    
    /**
     * Link sections using {@link LinkedSections}
     * @param sections a linked section constraint to be added into the problem
     */
    @Deprecated
    public void addLinkedSections(Collection<Section> sections) {
        addLinkedSections(false, sections);
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
        String priorityComplete = "";
        for (StudentPriority sp: StudentPriority.values()) {
            if (sp != StudentPriority.Dummy && iNrPriorityStudents[sp.ordinal()] > 0)
                priorityComplete += (priorityComplete.isEmpty() ? "" : "\n") +
                    sp.name() + ": " + sDoubleFormat.format(100.0 * context.iNrCompletePriorityStudents[sp.ordinal()] / iNrPriorityStudents[sp.ordinal()]) + "% (" + context.iNrCompletePriorityStudents[sp.ordinal()] + "/" + iNrPriorityStudents[sp.ordinal()] + ")";
        }
        if (!priorityComplete.isEmpty())
            info.put("Students with complete schedule (priority students)", priorityComplete);
        if (getStudentQuality() != null) {
            int confs = getStudentQuality().getTotalPenalty(StudentQuality.Type.Distance, assignment);
            int shortConfs = getStudentQuality().getTotalPenalty(StudentQuality.Type.ShortDistance, assignment);
            int unavConfs = getStudentQuality().getTotalPenalty(StudentQuality.Type.UnavailabilityDistance, assignment);
            if (confs > 0 || shortConfs > 0) {
                info.put("Student distance conflicts", confs + (shortConfs == 0 ? "" : " (" + getDistanceMetric().getShortDistanceAccommodationReference() + ": " + shortConfs + ")"));
            }
            if (unavConfs > 0) {
                info.put("Unavailabilities: Distance conflicts", String.valueOf(unavConfs));
            }
        } else if (getDistanceConflict() != null) {
            int confs = getDistanceConflict().getTotalNrConflicts(assignment);
            if (confs > 0) {
                int shortConfs = getDistanceConflict().getTotalNrShortConflicts(assignment);
                info.put("Student distance conflicts", confs + (shortConfs == 0 ? "" : " (" + getDistanceConflict().getDistanceMetric().getShortDistanceAccommodationReference() + ": " + shortConfs + ")"));
            }
        }
        if (getStudentQuality() != null) {
            int shareCR = getStudentQuality().getContext(assignment).countTotalPenalty(StudentQuality.Type.CourseTimeOverlap, assignment);
            int shareFT = getStudentQuality().getContext(assignment).countTotalPenalty(StudentQuality.Type.FreeTimeOverlap, assignment);
            int shareUN = getStudentQuality().getContext(assignment).countTotalPenalty(StudentQuality.Type.Unavailability, assignment);
            if (shareCR + shareFT + shareUN > 0)
                info.put("Time overlapping conflicts", sDoubleFormat.format((5.0 * (shareCR + shareFT + shareUN)) / iStudents.size()) + " mins per student\n" + 
                        "(" + sDoubleFormat.format(5.0 * shareCR / iStudents.size()) + " between courses, " + sDoubleFormat.format(5.0 * shareFT / iStudents.size()) + " free time" +
                        (shareUN == 0 ? "" : ", " + sDoubleFormat.format(5.0 * shareUN / iStudents.size()) + " teaching assignments & unavailabilities") + "; " + sDoubleFormat.format((shareCR + shareFT + shareUN) / 12.0) + " hours total)");
        } else if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts(assignment) != 0) {
            info.put("Time overlapping conflicts", sDoubleFormat.format(5.0 * getTimeOverlaps().getTotalNrConflicts(assignment) / iStudents.size()) + " mins per student (" + sDoubleFormat.format(getTimeOverlaps().getTotalNrConflicts(assignment) / 12.0) + " hours total)");
        }
        if (getStudentQuality() != null) {
            int confLunch = getStudentQuality().getTotalPenalty(StudentQuality.Type.LunchBreak, assignment);
            if (confLunch > 0)
                info.put("Schedule Quality: Lunch conflicts", sDoubleFormat.format(20.0 * confLunch / getNrRealStudents(false)) + "% (" + confLunch + ")");
            int confTravel = getStudentQuality().getTotalPenalty(StudentQuality.Type.TravelTime, assignment);
            if (confTravel > 0)
                info.put("Schedule Quality: Travel time", sDoubleFormat.format(((double)confTravel) / getNrRealStudents(false)) + " mins per student (" + sDecimalFormat.format(confTravel / 60.0) + " hours total)");
            int confBtB = getStudentQuality().getTotalPenalty(StudentQuality.Type.BackToBack, assignment);
            if (confBtB != 0)
                info.put("Schedule Quality: Back-to-back classes", sDoubleFormat.format(((double)confBtB) / getNrRealStudents(false)) + " per student (" + confBtB + ")");
            int confMod = getStudentQuality().getTotalPenalty(StudentQuality.Type.Modality, assignment);
            if (confMod > 0)
                info.put("Schedule Quality: Online class preference", sDoubleFormat.format(((double)confMod) / getNrRealStudents(false)) + " per student (" + confMod + ")");
            int confWorkDay = getStudentQuality().getTotalPenalty(StudentQuality.Type.WorkDay, assignment);
            if (confWorkDay > 0)
                info.put("Schedule Quality: Work day", sDoubleFormat.format(5.0 * confWorkDay / getNrRealStudents(false)) + " mins over " +
                        new DecimalFormat("0.#").format(getProperties().getPropertyInt("WorkDay.WorkDayLimit", 6*12) / 12.0) + " hours a day per student\n(from start to end, " + sDoubleFormat.format(confWorkDay / 12.0) + " hours total)");
            int early = getStudentQuality().getTotalPenalty(StudentQuality.Type.TooEarly, assignment);
            if (early > 0) {
                int min = getProperties().getPropertyInt("WorkDay.EarlySlot", 102) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
                int h = min / 60;
                int m = min % 60;
                String time = (getProperties().getPropertyBoolean("General.UseAmPm", true) ? (h > 12 ? h - 12 : h) + ":" + (m < 10 ? "0" : "") + m + (h >= 12 ? "p" : "a") : h + ":" + (m < 10 ? "0" : "") + m);
                info.put("Schedule Quality: Early classes", sDoubleFormat.format(5.0 * early / iStudents.size()) + " mins before " + time + " per student (" + sDoubleFormat.format(early / 12.0) + " hours total)");
            }
            int late = getStudentQuality().getTotalPenalty(StudentQuality.Type.TooLate, assignment);
            if (late > 0) {
                int min = getProperties().getPropertyInt("WorkDay.LateSlot", 210) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
                int h = min / 60;
                int m = min % 60;
                String time = (getProperties().getPropertyBoolean("General.UseAmPm", true) ? (h > 12 ? h - 12 : h) + ":" + (m < 10 ? "0" : "") + m + (h >= 12 ? "p" : "a") : h + ":" + (m < 10 ? "0" : "") + m);
                info.put("Schedule Quality: Late classes", sDoubleFormat.format(5.0 * late / iStudents.size()) + " mins after " + time + " per student (" + sDoubleFormat.format(late / 12.0) + " hours total)");
            }
            int accFT = getStudentQuality().getTotalPenalty(StudentQuality.Type.AccFreeTimeOverlap, assignment);
            if (accFT > 0) {
                info.put("Accommodations: Free time conflicts", sDoubleFormat.format(5.0 * accFT / getStudentsWithAccommodation(getStudentQuality().getStudentQualityContext().getFreeTimeAccommodation())) + " mins per student, " + sDoubleFormat.format(accFT / 12.0) + " hours total");
            }
            int accBtB = getStudentQuality().getTotalPenalty(StudentQuality.Type.AccBackToBack, assignment);
            if (accBtB > 0) {
                info.put("Accommodations: Back-to-back classes", sDoubleFormat.format(((double)accBtB) / getStudentsWithAccommodation(getStudentQuality().getStudentQualityContext().getBackToBackAccommodation())) + " non-BTB classes per student, " + accBtB + " total");
            }
            int accBbc = getStudentQuality().getTotalPenalty(StudentQuality.Type.AccBreaksBetweenClasses, assignment);
            if (accBbc > 0) {
                info.put("Accommodations: Break between classes", sDoubleFormat.format(((double)accBbc) / getStudentsWithAccommodation(getStudentQuality().getStudentQualityContext().getBreakBetweenClassesAccommodation())) + " BTB classes per student, " + accBbc + " total");
            }
            int shortConfs = getStudentQuality().getTotalPenalty(StudentQuality.Type.ShortDistance, assignment);
            if (shortConfs > 0) {
                info.put("Accommodations: Distance conflicts", sDoubleFormat.format(((double)shortConfs) / getStudentsWithAccommodation(getStudentQuality().getDistanceMetric().getShortDistanceAccommodationReference())) + " short distance conflicts per student, " + shortConfs + " total");
            }
        }
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
        }
        context.getInfo(assignment, info);
        
        double groupSpread = 0.0; double groupCount = 0;
        for (Offering offering: iOfferings) {
            for (Course course: offering.getCourses()) {
                for (RequestGroup group: course.getRequestGroups()) {
                    groupSpread += group.getAverageSpread(assignment) * group.getEnrollmentWeight(assignment, null);
                    groupCount += group.getEnrollmentWeight(assignment, null);
                }
            }
        }
        if (groupCount > 0)
            info.put("Same group", sDecimalFormat.format(100.0 * groupSpread / groupCount) + "%");

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
                    if (c.getR1() != null) total -= c.getR1Weight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE1(), c);
                    if (c.getR2() != null) total -= c.getR2Weight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE2(), c);
                }
            if (iStudentQuality != null)
                for (StudentQuality.Type t: StudentQuality.Type.values()) {
                    for (StudentQuality.Conflict c: iStudentQuality.getContext(assignment).computeAllConflicts(t, assignment)) {
                        switch (c.getType().getType()) {
                            case REQUEST:
                                if (c.getR1() instanceof CourseRequest)
                                    total -= c.getR1Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                                else
                                    total -= c.getR2Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE2(), c);
                                break;
                            case BOTH:
                                total -= c.getR1Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);  
                                total -= c.getR2Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE2(), c);
                                break;
                            case LOWER:
                                total -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                                break;
                            case HIGHER:
                                total -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                                break;
                        }
                    }    
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
    
    public StudentQuality getStudentQuality() { return iStudentQuality; }
    public void setStudentQuality(StudentQuality q, boolean register) {
        if (iStudentQuality != null)
            getInfoProviders().remove(iStudentQuality);
        iStudentQuality = q;
        if (iStudentQuality != null)
            getInfoProviders().add(iStudentQuality);
        if (register) {
            iStudentQuality.setAssignmentContextReference(createReference(iStudentQuality));
            iStudentQuality.register(this);
        }
    }
    
    public void setStudentQuality(StudentQuality q) {
        setStudentQuality(q, true);
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
     * Count students with given accommodation
     */
    public int getStudentsWithAccommodation(String acc) {
        int nrAccStudents = 0;
        for (Student student : getStudents()) {
            if (student.hasAccommodation(acc))
                nrAccStudents++;
        }
        return nrAccStudents;
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
        /*
        double dc = 0;
        if (getDistanceConflict() != null && getDistanceConflict().getTotalNrConflicts(assignment) != 0) {
            Set<DistanceConflict.Conflict> conf = getDistanceConflict().getAllConflicts(assignment);
            int sdc = 0;
            for (DistanceConflict.Conflict c: conf) {
                dc += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(assignment, c);
                if (c.getStudent().isNeedShortDistances()) sdc ++;
            }
            if (!conf.isEmpty())
                info.put("Student distance conflicts", conf.size() + (sdc > 0 ? " (" + getDistanceConflict().getDistanceMetric().getShortDistanceAccommodationReference() + ": " + sdc + ", weighted: " : " (weighted: ") + sDecimalFormat.format(dc) + ")");
        }
        */
        if (getStudentQuality() == null && getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts(assignment) != 0) {
            Set<TimeOverlapsCounter.Conflict> conf = getTimeOverlaps().getContext(assignment).computeAllConflicts(assignment);
            int share = 0, crShare = 0;
            for (TimeOverlapsCounter.Conflict c: conf) {
                share += c.getShare();
                if (c.getR1() instanceof CourseRequest && c.getR2() instanceof CourseRequest)
                    crShare += c.getShare();
            }
            if (share > 0)
                info.put("Time overlapping conflicts", sDoubleFormat.format(5.0 * share / iStudents.size()) + " mins per student\n(" + sDoubleFormat.format(5.0 * crShare / iStudents.size()) + " between courses; " + sDoubleFormat.format(getTimeOverlaps().getTotalNrConflicts(assignment) / 12.0) + " hours total)");
        }
        if (getStudentQuality() != null) {
            int confBtB = getStudentQuality().getTotalPenalty(StudentQuality.Type.BackToBack, assignment);
            if (confBtB != 0) {
                int prefBtb = 0, discBtb = 0;
                int prefStd = 0, discStd = 0;
                int prefPairs = 0, discPairs = 0;
                for (Student s: getStudents()) {
                    if (s.isDummy() || s.getBackToBackPreference() == BackToBackPreference.NO_PREFERENCE) continue;
                    int[] classesPerDay = new int[] {0, 0, 0, 0, 0, 0, 0};
                    for (Request r: s.getRequests()) {
                        Enrollment e = r.getAssignment(assignment);
                        if (e == null || !e.isCourseRequest()) continue;
                        for (Section x: e.getSections()) {
                            if (x.getTime() != null)
                                for (int i = 0; i < Constants.DAY_CODES.length; i++)
                                    if ((x.getTime().getDayCode() & Constants.DAY_CODES[i]) != 0)
                                        classesPerDay[i] ++;
                        }
                    }
                    int max = 0;
                    for (int c: classesPerDay)
                        if (c > 1) max += c - 1;
                    int btb = getStudentQuality().getContext(assignment).allPenalty(StudentQuality.Type.BackToBack, assignment, s);
                    if (s.getBackToBackPreference() == BackToBackPreference.BTB_PREFERRED) {
                        prefStd ++;
                        prefBtb += btb;
                        prefPairs += Math.max(btb, max);
                    } else if (s.getBackToBackPreference() == BackToBackPreference.BTB_DISCOURAGED) {
                        discStd ++;
                        discBtb -= btb;
                        discPairs += Math.max(btb, max);
                    }
                }
                if (prefStd > 0)
                    info.put("Schedule Quality: Back-to-back preferred", sDoubleFormat.format((100.0 * prefBtb) / prefPairs) + "% back-to-backs on average (" + prefBtb + "/" + prefPairs + " BTBs for " + prefStd + " students)");
                if (discStd > 0)
                    info.put("Schedule Quality: Back-to-back discouraged", sDoubleFormat.format(100.0 - (100.0 * discBtb) / discPairs) + "% non back-to-backs on average (" + discBtb + "/" + discPairs + " BTBs for " + discStd + " students)");
            }
            int confMod = getStudentQuality().getTotalPenalty(StudentQuality.Type.Modality, assignment);
            if (confMod > 0) {
                int prefOnl = 0, discOnl = 0;
                int prefStd = 0, discStd = 0;
                int prefCls = 0, discCls = 0;
                for (Student s: getStudents()) {
                    if (s.isDummy()) continue;
                    if (s.isDummy() || s.getModalityPreference() == ModalityPreference.NO_PREFERENCE || s.getModalityPreference() == ModalityPreference.ONLINE_REQUIRED) continue;
                    int classes = 0;
                    for (Request r: s.getRequests()) {
                        Enrollment e = r.getAssignment(assignment);
                        if (e == null || !e.isCourseRequest()) continue;
                        classes += e.getSections().size();
                    }
                    if (s.getModalityPreference() == ModalityPreference.ONLINE_PREFERRED) {
                        prefStd ++;
                        prefOnl += getStudentQuality().getContext(assignment).allPenalty(StudentQuality.Type.Modality, assignment, s);
                        prefCls += classes;
                    } else if (s.getModalityPreference() == ModalityPreference.ONILNE_DISCOURAGED) {
                        discStd ++;
                        discOnl += getStudentQuality().getContext(assignment).allPenalty(StudentQuality.Type.Modality, assignment, s);
                        discCls += classes;
                    }
                }
                if (prefStd > 0)
                    info.put("Schedule Quality: Online preferred", sDoubleFormat.format(100.0 - (100.0 * prefOnl) / prefCls) + "% online classes on average (" + prefOnl + "/" + prefCls + " classes for " + prefStd + " students)");
                if (discStd > 0)
                    info.put("Schedule Quality: Online discouraged", sDoubleFormat.format(100.0 - (100.0 * discOnl) / discCls) + "% face-to-face classes on average (" + discOnl + "/" + discCls + " classes for " + discStd + " students)");
            }
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
            if (offering.isDummy()) continue;
            for (Config config: offering.getConfigs()) {
                double enrl = config.getEnrollmentTotalWeight(assignment, null);
                for (Subpart subpart: config.getSubparts()) {
                    if (subpart.getSections().size() <= 1) continue;
                    if (subpart.getLimit() > 0) {
                        // sections have limits -> desired size is section limit x (total enrollment / total limit)
                        double ratio = enrl / subpart.getLimit();
                        for (Section section: subpart.getSections()) {
                            double desired = ratio * section.getLimit();
                            disbWeight += Math.abs(section.getEnrollmentTotalWeight(assignment, null) - desired);
                            disbSections ++;
                            if (Math.abs(desired - section.getEnrollmentTotalWeight(assignment, null)) >= Math.max(1.0, 0.1 * section.getLimit())) {
                                disb10Sections++;
                                if (disb10SectionList != null)
                                	disb10SectionList.add(section.getSubpart().getConfig().getOffering().getName() + " " + section.getSubpart().getName() + " " + section.getName()); 
                            }
                        }
                    } else {
                        // unlimited sections -> desired size is total enrollment / number of sections
                        for (Section section: subpart.getSections()) {
                            double desired = enrl / subpart.getSections().size();
                            disbWeight += Math.abs(section.getEnrollmentTotalWeight(assignment, null) - desired);
                            disbSections ++;
                            if (Math.abs(desired - section.getEnrollmentTotalWeight(assignment, null)) >= Math.max(1.0, 0.1 * desired)) {
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
            info.put("Average disbalance", sDecimalFormat.format(assignedCRWeight == 0 ? 0.0 : 100.0 * disbWeight / assignedCRWeight) + "% (" + sDecimalFormat.format(disbWeight / disbSections) + ")");
            String list = "";
            if (disb10SectionList != null) {
                int i = 0;
                for (String section: disb10SectionList) {
                    if (i == disb10Limit) {
                        list += "\n...";
                        break;
                    }
                    list += "\n" + section;
                    i++;
                }
            }
            info.put("Sections disbalanced by 10% or more", sDecimalFormat.format(disbSections == 0 ? 0.0 : 100.0 * disb10Sections / disbSections) + "% (" + disb10Sections + ")" + (list.isEmpty() ? "" : "\n" + list));
        }
        
        int assCR = 0, priCR = 0;
        for (Request r: variables()) {
            if (r instanceof CourseRequest && !r.getStudent().isDummy()) {
                CourseRequest cr = (CourseRequest)r;
                Enrollment e = assignment.getValue(cr);
                if (e != null) {
                    assCR ++;
                    if (!cr.isAlternative() && cr.getCourses().get(0).equals(e.getCourse())) priCR ++;
                }
            }
        }
        if (assCR > 0)
            info.put("Assigned priority course requests", sDoubleFormat.format(100.0 * priCR / assCR) + "% (" + priCR + "/" + assCR + ")");
        int[] missing = new int[] {0, 0, 0, 0, 0};
        int incomplete = 0;
        for (Student student: getStudents()) {
            if (student.isDummy()) continue;
            int nrRequests = 0;
            int nrAssignedRequests = 0;
            for (Request r : student.getRequests()) {
                if (!(r instanceof CourseRequest)) continue; // ignore free times
                if (!r.isAlternative()) nrRequests++;
                if (r.isAssigned(assignment)) nrAssignedRequests++;
            }
            if (nrAssignedRequests < nrRequests) {
                missing[Math.min(nrRequests - nrAssignedRequests, missing.length) - 1] ++;
                incomplete ++;
            }
        }

        for (int i = 0; i < missing.length; i++)
            if (missing[i] > 0)
                info.put("Students missing " + (i == 0 ? "1 course" : i + 1 == missing.length ? (i + 1) + " or more courses" : (i + 1) + " courses"), sDecimalFormat.format(100.0 * missing[i] / incomplete) + "% (" + missing[i] + ")");

        info.put("Overall solution value", sDoubleFormat.format(getTotalValue(assignment)));// + " [precise: " + sDoubleFormat.format(getTotalValue(assignment, true)) + "]");
        
        int nrStudentsBelowMinCredit = 0, nrStudents = 0;
        for (Student student: getStudents()) {
            if (student.isDummy()) continue;
            if (student.hasMinCredit()) {
                nrStudents++;
                float credit = student.getAssignedCredit(assignment); 
                if (credit < student.getMinCredit() && !student.isComplete(assignment))
                    nrStudentsBelowMinCredit ++;
            }
        }
        if (nrStudentsBelowMinCredit > 0)
            info.put("Students below min credit", sDoubleFormat.format(100.0 * nrStudentsBelowMinCredit / nrStudents) + "% (" + nrStudentsBelowMinCredit + "/" + nrStudents + ")");
        
        int[] notAssignedPriority = new int[] {0, 0, 0, 0, 0, 0, 0};
        int[] assignedChoice = new int[] {0, 0, 0, 0, 0};
        int notAssignedTotal = 0, assignedChoiceTotal = 0;
        int avgPriority = 0, avgChoice = 0;
        for (Student student: getStudents()) {
            if (student.isDummy()) continue;
            for (Request r : student.getRequests()) {
                if (!(r instanceof CourseRequest)) continue; // ignore free times
                Enrollment e = r.getAssignment(assignment);
                if (e == null) {
                    if (!r.isAlternative()) {
                        notAssignedPriority[Math.min(r.getPriority(), notAssignedPriority.length - 1)] ++;
                        notAssignedTotal ++;
                        avgPriority += r.getPriority();
                    }
                } else {
                    assignedChoice[Math.min(e.getTruePriority(), assignedChoice.length - 1)] ++;
                    assignedChoiceTotal ++;
                    avgChoice += e.getTruePriority();
                }
            }
        }
        for (int i = 0; i < notAssignedPriority.length; i++)
            if (notAssignedPriority[i] > 0)
                info.put("Priority: Not-assigned priority " + (i + 1 == notAssignedPriority.length ? (i + 1) + "+" : (i + 1)) + " course requests", sDecimalFormat.format(100.0 * notAssignedPriority[i] / notAssignedTotal) + "% (" + notAssignedPriority[i] + ")");
        if (notAssignedTotal > 0)
            info.put("Priority: Average not-assigned priority", sDecimalFormat.format(1.0 + ((double)avgPriority) / notAssignedTotal));
        for (int i = 0; i < assignedChoice.length; i++)
            if (assignedChoice[i] > 0)
                info.put("Choice: assigned " + (i == 0 ? "1st": i == 1 ? "2nd" : i == 2 ? "3rd" : i + 1 == assignedChoice.length ? (i + 1) + "th+" : (i + 1) + "th") + " course choice", sDecimalFormat.format(100.0 * assignedChoice[i] / assignedChoiceTotal) + "% (" + assignedChoice[i] + ")");
        if (assignedChoiceTotal > 0)
            info.put("Choice: Average assigned choice", sDecimalFormat.format(1.0 + ((double)avgChoice) / assignedChoiceTotal));
        
        int nbrSections = 0, nbrFullSections = 0, nbrSections98 = 0, nbrSections95 = 0, nbrSections90 = 0, nbrSectionsDis = 0;
        int enrlSections = 0, enrlFullSections = 0, enrlSections98 = 0, enrlSections95 = 0, enrlSections90 = 0, enrlSectionsDis = 0;
        int nbrOfferings = 0, nbrFullOfferings = 0, nbrOfferings98 = 0, nbrOfferings95 = 0, nbrOfferings90 = 0;
        int enrlOfferings = 0, enrlOfferingsFull = 0, enrlOfferings98 = 0, enrlOfferings95 = 0, enrlOfferings90 = 0;
        for (Offering offering: getOfferings()) {
            if (offering.isDummy()) continue;
            int offeringLimit = 0, offeringEnrollment = 0;
            for (Config config: offering.getConfigs()) {
                int configLimit = config.getLimit();
                for (Subpart subpart: config.getSubparts()) {
                    int subpartLimit = 0;
                    for (Section section: subpart.getSections()) {
                        if (section.isCancelled()) continue;
                        int enrl = section.getEnrollments(assignment).size();
                        if (section.getLimit() < 0 || subpartLimit < 0)
                            subpartLimit = -1;
                        else
                            subpartLimit += (section.isEnabled() ? section.getLimit() : enrl);
                        nbrSections ++;
                        enrlSections += enrl;
                        if (section.getLimit() >= 0 && section.getLimit() <= enrl) {
                            nbrFullSections ++;
                            enrlFullSections += enrl;
                        }
                        if (!section.isEnabled() && (enrl > 0 || section.getLimit() >= 0)) {
                            nbrSectionsDis ++;
                            enrlSectionsDis += enrl;
                        }
                        if (section.getLimit() >= 0 && (section.getLimit() - enrl) <= Math.round(0.02 * section.getLimit())) {
                            nbrSections98 ++;
                            enrlSections98 += enrl;
                        }
                        if (section.getLimit() >= 0 && (section.getLimit() - enrl) <= Math.round(0.05 * section.getLimit())) {
                            nbrSections95 ++;
                            enrlSections95 += enrl;
                        }
                        if (section.getLimit() >= 0 && (section.getLimit() - enrl) <= Math.round(0.10 * section.getLimit())) {
                            nbrSections90 ++;
                            enrlSections90 += enrl;
                        }
                    }
                    if (configLimit < 0 || subpartLimit < 0)
                        configLimit = -1;
                    else
                        configLimit = Math.min(configLimit, subpartLimit);
                }
                if (offeringLimit < 0 || configLimit < 0)
                    offeringLimit = -1;
                else
                    offeringLimit += configLimit;
                offeringEnrollment += config.getEnrollments(assignment).size();
            }
            nbrOfferings ++;
            enrlOfferings += offeringEnrollment;
            
            if (offeringLimit >=0 && offeringEnrollment >= offeringLimit) {
                nbrFullOfferings ++;
                enrlOfferingsFull += offeringEnrollment;
            }
            if (offeringLimit >= 0 && (offeringLimit - offeringEnrollment) <= Math.round(0.02 * offeringLimit)) {
                nbrOfferings98++;
                enrlOfferings98 += offeringEnrollment;
            }
            if (offeringLimit >= 0 && (offeringLimit - offeringEnrollment) <= Math.round(0.05 * offeringLimit)) {
                nbrOfferings95++;
                enrlOfferings95 += offeringEnrollment;
            }
            if (offeringLimit >= 0 && (offeringLimit - offeringEnrollment) <= Math.round(0.10 * offeringLimit)) {
                nbrOfferings90++;
                enrlOfferings90 += offeringEnrollment;
            }
        }
        if (enrlOfferings90 > 0 && enrlOfferings > 0) 
            info.put("Full Offerings", (nbrFullOfferings > 0 ? nbrFullOfferings + " with no space (" + sDecimalFormat.format(100.0 * nbrFullOfferings / nbrOfferings) + "% of all offerings, " +
                    sDecimalFormat.format(100.0 * enrlOfferingsFull / enrlOfferings) + "% assignments)\n" : "")+
                    (nbrOfferings98 > nbrFullOfferings ? nbrOfferings98 + " with &leq; 2% available (" + sDecimalFormat.format(100.0 * nbrOfferings98 / nbrOfferings) + "% of all offerings, " +
                    sDecimalFormat.format(100.0 * enrlOfferings98 / enrlOfferings) + "% assignments)\n" : "")+
                    (nbrOfferings95 > nbrOfferings98 ? nbrOfferings95 + " with &leq; 5% available (" + sDecimalFormat.format(100.0 * nbrOfferings95 / nbrOfferings) + "% of all offerings, " +
                    sDecimalFormat.format(100.0 * enrlOfferings95 / enrlOfferings) + "% assignments)\n" : "")+
                    (nbrOfferings90 > nbrOfferings95 ? nbrOfferings90 + " with &leq; 10% available (" + sDecimalFormat.format(100.0 * nbrOfferings90 / nbrOfferings) + "% of all offerings, " +
                    sDecimalFormat.format(100.0 * enrlOfferings90 / enrlOfferings) + "% assignments)" : ""));
        if ((enrlSections90 > 0 || nbrSectionsDis > 0) && enrlSections > 0)
            info.put("Full Sections", (nbrFullSections > 0 ? nbrFullSections + " with no space (" + sDecimalFormat.format(100.0 * nbrFullSections / nbrSections) + "% of all sections, "+
                    sDecimalFormat.format(100.0 * enrlFullSections / enrlSections) + "% assignments)\n" : "") +
                    (nbrSectionsDis > 0 ? nbrSectionsDis + " disabled (" + sDecimalFormat.format(100.0 * nbrSectionsDis / nbrSections) + "% of all sections, "+
                    sDecimalFormat.format(100.0 * enrlSectionsDis / enrlSections) + "% assignments)\n" : "") +
                    (enrlSections98 > nbrFullSections ? nbrSections98 + " with &leq; 2% available (" + sDecimalFormat.format(100.0 * nbrSections98 / nbrSections) + "% of all sections, " +
                    sDecimalFormat.format(100.0 * enrlSections98 / enrlSections) + "% assignments)\n" : "") +
                    (nbrSections95 > enrlSections98 ? nbrSections95 + " with &leq; 5% available (" + sDecimalFormat.format(100.0 * nbrSections95 / nbrSections) + "% of all sections, " +
                    sDecimalFormat.format(100.0 * enrlSections95 / enrlSections) + "% assignments)\n" : "") +
                    (nbrSections90 > nbrSections95 ? nbrSections90 + " with &leq; 10% available (" + sDecimalFormat.format(100.0 * nbrSections90 / nbrSections) + "% of all sections, " +
                    sDecimalFormat.format(100.0 * enrlSections90 / enrlSections) + "% assignments)" : ""));
        if (getStudentQuality() != null) {
            int shareCR = getStudentQuality().getContext(assignment).countTotalPenalty(StudentQuality.Type.CourseTimeOverlap, assignment);
            int shareFT = getStudentQuality().getContext(assignment).countTotalPenalty(StudentQuality.Type.FreeTimeOverlap, assignment);
            int shareUN = getStudentQuality().getContext(assignment).countTotalPenalty(StudentQuality.Type.Unavailability, assignment);
            int shareUND = getStudentQuality().getContext(assignment).countTotalPenalty(StudentQuality.Type.UnavailabilityDistance, assignment);
            if (shareCR > 0) {
                Set<Student> students = new HashSet<Student>();
                for (StudentQuality.Conflict c: getStudentQuality().getContext(assignment).computeAllConflicts(StudentQuality.Type.CourseTimeOverlap, assignment)) {
                    students.add(c.getStudent());
                }
                info.put("Time overlaps: courses", students.size() + " students (avg " + sDoubleFormat.format(5.0 * shareCR / students.size()) + " mins)");
            }
            if (shareFT > 0) {
                Set<Student> students = new HashSet<Student>();
                for (StudentQuality.Conflict c: getStudentQuality().getContext(assignment).computeAllConflicts(StudentQuality.Type.FreeTimeOverlap, assignment)) {
                    students.add(c.getStudent());
                }
                info.put("Time overlaps: free times", students.size() + " students (avg " + sDoubleFormat.format(5.0 * shareFT / students.size()) + " mins)");
            }
            if (shareUN > 0) {
                Set<Student> students = new HashSet<Student>();
                for (StudentQuality.Conflict c: getStudentQuality().getContext(assignment).computeAllConflicts(StudentQuality.Type.Unavailability, assignment)) {
                    students.add(c.getStudent());
                }
                info.put("Unavailabilities: Time conflicts", students.size() + " students (avg " + sDoubleFormat.format(5.0 * shareUN / students.size()) + " mins)");
            }
            if (shareUND > 0) {
                Set<Student> students = new HashSet<Student>();
                for (StudentQuality.Conflict c: getStudentQuality().getContext(assignment).computeAllConflicts(StudentQuality.Type.UnavailabilityDistance, assignment)) {
                    students.add(c.getStudent());
                }
                info.put("Unavailabilities: Distance conflicts", students.size() + " students (avg " + sDoubleFormat.format(shareUND / students.size()) + " travels)");
            }
        } else if (getTimeOverlaps() != null && getTimeOverlaps().getTotalNrConflicts(assignment) != 0) {
            Set<TimeOverlapsCounter.Conflict> conf = getTimeOverlaps().getContext(assignment).computeAllConflicts(assignment);
            int shareCR = 0, shareFT = 0, shareUN = 0;
            Set<Student> studentsCR = new HashSet<Student>();
            Set<Student> studentsFT = new HashSet<Student>();
            Set<Student> studentsUN = new HashSet<Student>();
            for (TimeOverlapsCounter.Conflict c: conf) {
                if (c.getR1() instanceof CourseRequest && c.getR2() instanceof CourseRequest) {
                    shareCR += c.getShare(); studentsCR.add(c.getStudent());
                } else if (c.getS2() instanceof Unavailability) {
                    shareUN += c.getShare(); studentsUN.add(c.getStudent());
                } else {
                    shareFT += c.getShare(); studentsFT.add(c.getStudent());
                }
            }
            if (shareCR > 0)
                info.put("Time overlaps: courses", studentsCR.size() + " students (avg " + sDoubleFormat.format(5.0 * shareCR / studentsCR.size()) + " mins)");
            if (shareFT > 0)
                info.put("Time overlaps: free times", studentsFT.size() + " students (avg " + sDoubleFormat.format(5.0 * shareFT / studentsFT.size()) + " mins)");
            if (shareUN > 0)
                info.put("Time overlaps: teaching assignments", studentsUN.size() + " students (avg " + sDoubleFormat.format(5.0 * shareUN / studentsUN.size()) + " mins)");
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
        recomputeTotalValue(assignment);
    }
    
    public void recomputeTotalValue(Assignment<Request, Enrollment> assignment) {
        getContext(assignment).iTotalValue = getTotalValue(assignment, true);
    }
    
    @Override
    public void saveBest(Assignment<Request, Enrollment> assignment) {
        recomputeTotalValue(assignment);
        iBestAssignedCourseRequestWeight = getContext(assignment).getAssignedCourseRequestWeight();
        super.saveBest(assignment);
    }
    
    public double getBestAssignedCourseRequestWeight() {
        return iBestAssignedCourseRequestWeight;
    }
        
    @Override
    public String toString(Assignment<Request, Enrollment> assignment) {
        double groupSpread = 0.0; double groupCount = 0;
        for (Offering offering: iOfferings) {
            for (Course course: offering.getCourses()) {
                for (RequestGroup group: course.getRequestGroups()) {
                    groupSpread += group.getAverageSpread(assignment) * group.getEnrollmentWeight(assignment, null);
                    groupCount += group.getEnrollmentWeight(assignment, null);
                }
            }
        }
        String priority = "";
        for (StudentPriority sp: StudentPriority.values()) {
            if (sp.ordinal() < StudentPriority.Normal.ordinal()) {
                if (iTotalPriorityCRWeight[sp.ordinal()] > 0.0)
                    priority += sp.code() + "PCR:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedPriorityCRWeight[sp.ordinal()] / iTotalPriorityCRWeight[sp.ordinal()]) + "%, ";
                if (iTotalPriorityCriticalCRWeight[RequestPriority.LC.ordinal()][sp.ordinal()] > 0.0)
                    priority += sp.code() + "PCL:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedPriorityCriticalCRWeight[RequestPriority.LC.ordinal()][sp.ordinal()] / iTotalPriorityCriticalCRWeight[RequestPriority.LC.ordinal()][sp.ordinal()]) + "%, ";
                if (iTotalPriorityCriticalCRWeight[RequestPriority.Critical.ordinal()][sp.ordinal()] > 0.0)
                    priority += sp.code() + "PCC:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedPriorityCriticalCRWeight[RequestPriority.Critical.ordinal()][sp.ordinal()] / iTotalPriorityCriticalCRWeight[RequestPriority.Critical.ordinal()][sp.ordinal()]) + "%, ";
                if (iTotalPriorityCriticalCRWeight[RequestPriority.Important.ordinal()][sp.ordinal()] > 0.0)
                    priority += sp.code() + "PCI:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedPriorityCriticalCRWeight[RequestPriority.Important.ordinal()][sp.ordinal()] / iTotalPriorityCriticalCRWeight[RequestPriority.Important.ordinal()][sp.ordinal()]) + "%, ";
                if (iTotalPriorityCriticalCRWeight[RequestPriority.Vital.ordinal()][sp.ordinal()] > 0.0)
                    priority += sp.code() + "PCV:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedPriorityCriticalCRWeight[RequestPriority.Vital.ordinal()][sp.ordinal()] / iTotalPriorityCriticalCRWeight[RequestPriority.Vital.ordinal()][sp.ordinal()]) + "%, ";
                if (iTotalPriorityCriticalCRWeight[RequestPriority.VisitingF2F.ordinal()][sp.ordinal()] > 0.0)
                    priority += sp.code() + "PCVF:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedPriorityCriticalCRWeight[RequestPriority.VisitingF2F.ordinal()][sp.ordinal()] / iTotalPriorityCriticalCRWeight[RequestPriority.VisitingF2F.ordinal()][sp.ordinal()]) + "%, ";
            }
        }
        return   (getNrRealStudents(false) > 0 ? "RRq:" + getNrAssignedRealRequests(assignment, false) + "/" + getNrRealRequests(false) + ", " : "")
                + (getNrLastLikeStudents(false) > 0 ? "DRq:" + getNrAssignedLastLikeRequests(assignment, false) + "/" + getNrLastLikeRequests(false) + ", " : "")
                + (getNrRealStudents(false) > 0 ? "RS:" + getNrCompleteRealStudents(assignment, false) + "/" + getNrRealStudents(false) + ", " : "")
                + (getNrLastLikeStudents(false) > 0 ? "DS:" + getNrCompleteLastLikeStudents(assignment, false) + "/" + getNrLastLikeStudents(false) + ", " : "")
                + (iTotalCRWeight > 0.0 ? "CR:" + sDecimalFormat.format(100.0 * getContext(assignment).getAssignedCourseRequestWeight() / iTotalCRWeight) + "%, " : "")
                + (iTotalSelCRWeight > 0.0 ? "S:" + sDoubleFormat.format(100.0 * (0.3 * getContext(assignment).iAssignedSelectedConfigWeight + 0.7 * getContext(assignment).iAssignedSelectedSectionWeight) / iTotalSelCRWeight) + "%, ": "")
                + (iTotalCriticalCRWeight[RequestPriority.LC.ordinal()] > 0.0 ? "LC:" + sDecimalFormat.format(100.0 * getContext(assignment).getAssignedCriticalCourseRequestWeight(RequestPriority.LC) / iTotalCriticalCRWeight[RequestPriority.LC.ordinal()]) + "%, " : "")
                + (iTotalCriticalCRWeight[RequestPriority.Critical.ordinal()] > 0.0 ? "CC:" + sDecimalFormat.format(100.0 * getContext(assignment).getAssignedCriticalCourseRequestWeight(RequestPriority.Critical) / iTotalCriticalCRWeight[RequestPriority.Critical.ordinal()]) + "%, " : "")
                + (iTotalCriticalCRWeight[RequestPriority.Important.ordinal()] > 0.0 ? "IC:" + sDecimalFormat.format(100.0 * getContext(assignment).getAssignedCriticalCourseRequestWeight(RequestPriority.Important) / iTotalCriticalCRWeight[RequestPriority.Important.ordinal()]) + "%, " : "")
                + (iTotalCriticalCRWeight[RequestPriority.Vital.ordinal()] > 0.0 ? "VC:" + sDecimalFormat.format(100.0 * getContext(assignment).getAssignedCriticalCourseRequestWeight(RequestPriority.Vital) / iTotalCriticalCRWeight[RequestPriority.Vital.ordinal()]) + "%, " : "")
                + (iTotalCriticalCRWeight[RequestPriority.VisitingF2F.ordinal()] > 0.0 ? "VFC:" + sDecimalFormat.format(100.0 * getContext(assignment).getAssignedCriticalCourseRequestWeight(RequestPriority.VisitingF2F) / iTotalCriticalCRWeight[RequestPriority.VisitingF2F.ordinal()]) + "%, " : "")
                + priority
                + "V:" + sDecimalFormat.format(-getTotalValue(assignment))
                + (getDistanceConflict() == null ? "" : ", DC:" + getDistanceConflict().getTotalNrConflicts(assignment))
                + (getTimeOverlaps() == null ? "" : ", TOC:" + getTimeOverlaps().getTotalNrConflicts(assignment))
                + (iMPP ? ", IS:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedSameSectionWeight / iTotalMPPCRWeight) + "%" : "")
                + (iMPP ? ", IT:" + sDecimalFormat.format(100.0 * getContext(assignment).iAssignedSameTimeWeight / iTotalMPPCRWeight) + "%" : "")
                + ", %:" + sDecimalFormat.format(-100.0 * getTotalValue(assignment) / (getStudents().size() - iNrDummyStudents + 
                        (iProjectedStudentWeight < 0.0 ? iNrDummyStudents * (iTotalDummyWeight / iNrDummyRequests) :iProjectedStudentWeight * iTotalDummyWeight)))
                + (groupCount > 0 ? ", SG:" + sDecimalFormat.format(100.0 * groupSpread / groupCount) + "%" : "")
                + (getStudentQuality() == null ? "" : ", SQ:{" + getStudentQuality().toString(assignment) + "}");
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
    
    public int getDayOfWeekOffset() { return iDayOfWeekOffset; }
    public void setDayOfWeekOffset(int dayOfWeekOffset) {
        iDayOfWeekOffset = dayOfWeekOffset;
        if (iProperties != null)
            iProperties.setProperty("DatePattern.DayOfWeekOffset", Integer.toString(dayOfWeekOffset));
    }

    @Override
    public StudentSectioningModelContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new StudentSectioningModelContext(assignment);
    }
    
    public class StudentSectioningModelContext implements AssignmentConstraintContext<Request, Enrollment>, InfoProvider<Request, Enrollment>{
        private Set<Student> iCompleteStudents = new HashSet<Student>();
        private double iTotalValue = 0.0;
        private int iNrAssignedDummyRequests = 0, iNrCompleteDummyStudents = 0;
        private double iAssignedCRWeight = 0.0, iAssignedDummyCRWeight = 0.0;
        private double[] iAssignedCriticalCRWeight;
        private double[][] iAssignedPriorityCriticalCRWeight;
        private double iReservedSpace = 0.0, iTotalReservedSpace = 0.0;
        private double iAssignedSameSectionWeight = 0.0, iAssignedSameChoiceWeight = 0.0, iAssignedSameTimeWeight = 0.0;
        private double iAssignedSelectedSectionWeight = 0.0, iAssignedSelectedConfigWeight = 0.0;
        private double iAssignedNoTimeSectionWeight = 0.0;
        private double iAssignedOnlineSectionWeight = 0.0;
        private double iAssignedPastSectionWeight = 0.0;
        private int[] iNrCompletePriorityStudents = null;
        private double[] iAssignedPriorityCRWeight = null;
        
        public StudentSectioningModelContext(StudentSectioningModelContext parent) {
            iCompleteStudents = new HashSet<Student>(parent.iCompleteStudents);
            iTotalValue = parent.iTotalValue;
            iNrAssignedDummyRequests = parent.iNrAssignedDummyRequests;
            iNrCompleteDummyStudents = parent.iNrCompleteDummyStudents;
            iAssignedCRWeight = parent.iAssignedCRWeight;
            iAssignedDummyCRWeight = parent.iAssignedDummyCRWeight;
            iReservedSpace = parent.iReservedSpace;
            iTotalReservedSpace = parent.iTotalReservedSpace;
            iAssignedSameSectionWeight = parent.iAssignedSameSectionWeight;
            iAssignedSameChoiceWeight = parent.iAssignedSameChoiceWeight;
            iAssignedSameTimeWeight = parent.iAssignedSameTimeWeight;
            iAssignedSelectedSectionWeight = parent.iAssignedSelectedSectionWeight;
            iAssignedSelectedConfigWeight = parent.iAssignedSelectedConfigWeight;
            iAssignedNoTimeSectionWeight = parent.iAssignedNoTimeSectionWeight;
            iAssignedOnlineSectionWeight = parent.iAssignedOnlineSectionWeight;
            iAssignedPastSectionWeight = parent.iAssignedPastSectionWeight;
            iAssignedCriticalCRWeight = new double[RequestPriority.values().length];
            iAssignedPriorityCriticalCRWeight = new double[RequestPriority.values().length][StudentPriority.values().length];
            for (int i = 0; i < RequestPriority.values().length; i++) {
                iAssignedCriticalCRWeight[i] = parent.iAssignedCriticalCRWeight[i];
                for (int j = 0; j < StudentPriority.values().length; j++) {
                    iAssignedPriorityCriticalCRWeight[i][j] = parent.iAssignedPriorityCriticalCRWeight[i][j];
                }
            }   
            iNrCompletePriorityStudents = new int[StudentPriority.values().length];
            iAssignedPriorityCRWeight = new double[StudentPriority.values().length];
            for (int i = 0; i < StudentPriority.values().length; i++) {
                iNrCompletePriorityStudents[i] = parent.iNrCompletePriorityStudents[i];
                iAssignedPriorityCRWeight[i] = parent.iAssignedPriorityCRWeight[i];
            }
        }

        public StudentSectioningModelContext(Assignment<Request, Enrollment> assignment) {
            iAssignedCriticalCRWeight = new double[RequestPriority.values().length];
            iAssignedPriorityCriticalCRWeight = new double[RequestPriority.values().length][StudentPriority.values().length];
            for (int i = 0; i < RequestPriority.values().length; i++) {
                iAssignedCriticalCRWeight[i] = 0.0;
                for (int j = 0; j < StudentPriority.values().length; j++) {
                    iAssignedPriorityCriticalCRWeight[i][j] = 0.0;
                }
            }
            iNrCompletePriorityStudents = new int[StudentPriority.values().length];
            iAssignedPriorityCRWeight = new double[StudentPriority.values().length];
            for (int i = 0; i < StudentPriority.values().length; i++) {
                iNrCompletePriorityStudents[i] = 0;
                iAssignedPriorityCRWeight[i] = 0.0;
            }
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
            if (student.isComplete(assignment) && iCompleteStudents.add(student)) {
                if (student.isDummy()) iNrCompleteDummyStudents++;
                iNrCompletePriorityStudents[student.getPriority().ordinal()]++;
            }
            double value = enrollment.getRequest().getWeight() * iStudentWeights.getWeight(assignment, enrollment);
            iTotalValue -= value;
            enrollment.variable().getContext(assignment).setLastWeight(value);
            if (enrollment.isCourseRequest())
                iAssignedCRWeight += enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && enrollment.getRequest().getRequestPriority() != RequestPriority.Normal && !enrollment.getStudent().isDummy() && !enrollment.getRequest().isAlternative())
                iAssignedCriticalCRWeight[enrollment.getRequest().getRequestPriority().ordinal()] += enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && enrollment.getRequest().getRequestPriority() != RequestPriority.Normal && !enrollment.getRequest().isAlternative())
                iAssignedPriorityCriticalCRWeight[enrollment.getRequest().getRequestPriority().ordinal()][enrollment.getStudent().getPriority().ordinal()] += enrollment.getRequest().getWeight();
            if (enrollment.getRequest().isMPP()) {
                iAssignedSameSectionWeight += enrollment.getRequest().getWeight() * enrollment.percentInitial();
                iAssignedSameChoiceWeight += enrollment.getRequest().getWeight() * enrollment.percentSelected();
                iAssignedSameTimeWeight += enrollment.getRequest().getWeight() * enrollment.percentSameTime();
            }
            if (enrollment.getRequest().hasSelection()) {
                iAssignedSelectedSectionWeight += enrollment.getRequest().getWeight() * enrollment.percentSelectedSameSection();
                iAssignedSelectedConfigWeight += enrollment.getRequest().getWeight() * enrollment.percentSelectedSameConfig();
            }
            if (enrollment.getReservation() != null)
                iReservedSpace += enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && ((CourseRequest)enrollment.getRequest()).hasReservations())
                iTotalReservedSpace += enrollment.getRequest().getWeight();
            if (student.isDummy()) {
                iNrAssignedDummyRequests++;
                if (enrollment.isCourseRequest())
                    iAssignedDummyCRWeight += enrollment.getRequest().getWeight();
            }
            if (enrollment.isCourseRequest())
                iAssignedPriorityCRWeight[enrollment.getStudent().getPriority().ordinal()] += enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest()) {
                int noTime = 0;
                int online = 0;
                int past = 0;
                for (Section section: enrollment.getSections()) {
                    if (!section.hasTime()) noTime ++;
                    if (section.isOnline()) online ++;
                    if (section.isPast()) past ++;
                }
                if (noTime > 0)
                    iAssignedNoTimeSectionWeight += enrollment.getRequest().getWeight() * noTime / enrollment.getSections().size();
                if (online > 0)
                    iAssignedOnlineSectionWeight += enrollment.getRequest().getWeight() * online / enrollment.getSections().size();
                if (past > 0)
                    iAssignedPastSectionWeight += enrollment.getRequest().getWeight() * past / enrollment.getSections().size();
            }
        }

        /**
         * Called before an enrollment was unassigned from a request. The list of
         * complete students and the overall solution value are updated.
         */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            Student student = enrollment.getStudent();
            if (enrollment.isCourseRequest() && iCompleteStudents.contains(student)) {
                iCompleteStudents.remove(student);
                if (student.isDummy())
                    iNrCompleteDummyStudents--;
                iNrCompletePriorityStudents[student.getPriority().ordinal()]--;
            }
            Request.RequestContext cx = enrollment.variable().getContext(assignment);
            Double value = cx.getLastWeight();
            if (value == null)
                value = enrollment.getRequest().getWeight() * iStudentWeights.getWeight(assignment, enrollment);
            iTotalValue += value;
            cx.setLastWeight(null);
            if (enrollment.isCourseRequest())
                iAssignedCRWeight -= enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && enrollment.getRequest().getRequestPriority() != RequestPriority.Normal && !enrollment.getStudent().isDummy() && !enrollment.getRequest().isAlternative())
                iAssignedCriticalCRWeight[enrollment.getRequest().getRequestPriority().ordinal()] -= enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && enrollment.getRequest().getRequestPriority() != RequestPriority.Normal && !enrollment.getRequest().isAlternative())
                iAssignedPriorityCriticalCRWeight[enrollment.getRequest().getRequestPriority().ordinal()][enrollment.getStudent().getPriority().ordinal()] -= enrollment.getRequest().getWeight();
            if (enrollment.getRequest().isMPP()) {
                iAssignedSameSectionWeight -= enrollment.getRequest().getWeight() * enrollment.percentInitial();
                iAssignedSameChoiceWeight -= enrollment.getRequest().getWeight() * enrollment.percentSelected();
                iAssignedSameTimeWeight -= enrollment.getRequest().getWeight() * enrollment.percentSameTime();
            }
            if (enrollment.getRequest().hasSelection()) {
                iAssignedSelectedSectionWeight -= enrollment.getRequest().getWeight() * enrollment.percentSelectedSameSection();
                iAssignedSelectedConfigWeight -= enrollment.getRequest().getWeight() * enrollment.percentSelectedSameConfig();
            }
            if (enrollment.getReservation() != null)
                iReservedSpace -= enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest() && ((CourseRequest)enrollment.getRequest()).hasReservations())
                iTotalReservedSpace -= enrollment.getRequest().getWeight();
            if (student.isDummy()) {
                iNrAssignedDummyRequests--;
                if (enrollment.isCourseRequest())
                    iAssignedDummyCRWeight -= enrollment.getRequest().getWeight();
            }
            if (enrollment.isCourseRequest())
                iAssignedPriorityCRWeight[enrollment.getStudent().getPriority().ordinal()] -= enrollment.getRequest().getWeight();
            if (enrollment.isCourseRequest()) {
                int noTime = 0;
                int online = 0;
                int past = 0;
                for (Section section: enrollment.getSections()) {
                    if (!section.hasTime()) noTime ++;
                    if (section.isOnline()) online ++;
                    if (section.isPast()) past ++;
                }
                if (noTime > 0)
                    iAssignedNoTimeSectionWeight -= enrollment.getRequest().getWeight() * noTime / enrollment.getSections().size();
                if (online > 0)
                    iAssignedOnlineSectionWeight -= enrollment.getRequest().getWeight() * online / enrollment.getSections().size();
                if (past > 0)
                    iAssignedPastSectionWeight -= enrollment.getRequest().getWeight() * past / enrollment.getSections().size();
            }
        }
        
        public void add(Assignment<Request, Enrollment> assignment, DistanceConflict.Conflict c) {
            iTotalValue += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(assignment, c);
        }

        public void remove(Assignment<Request, Enrollment> assignment, DistanceConflict.Conflict c) {
            iTotalValue -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getDistanceConflictWeight(assignment, c);
        }
        
        public void add(Assignment<Request, Enrollment> assignment, TimeOverlapsCounter.Conflict c) {
            if (c.getR1() != null) iTotalValue += c.getR1Weight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE1(), c);
            if (c.getR2() != null) iTotalValue += c.getR2Weight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE2(), c);
        }

        public void remove(Assignment<Request, Enrollment> assignment, TimeOverlapsCounter.Conflict c) {
            if (c.getR1() != null) iTotalValue -= c.getR1Weight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE1(), c);
            if (c.getR2() != null) iTotalValue -= c.getR2Weight() * iStudentWeights.getTimeOverlapConflictWeight(assignment, c.getE2(), c);
        }
        
        public void add(Assignment<Request, Enrollment> assignment, StudentQuality.Conflict c) {
            switch (c.getType().getType()) {
                case REQUEST:
                    if (c.getR1() instanceof CourseRequest)
                        iTotalValue += c.getR1Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                    else
                        iTotalValue += c.getR2Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE2(), c);
                    break;
                case BOTH:
                    iTotalValue += c.getR1Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);  
                    iTotalValue += c.getR2Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE2(), c);
                    break;
                case LOWER:
                    iTotalValue += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                    break;
                case HIGHER:
                    iTotalValue += avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                    break;
            }
        }

        public void remove(Assignment<Request, Enrollment> assignment, StudentQuality.Conflict c) {
            switch (c.getType().getType()) {
                case REQUEST:
                    if (c.getR1() instanceof CourseRequest)
                        iTotalValue -= c.getR1Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                    else
                        iTotalValue -= c.getR2Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE2(), c);
                    break;
                case BOTH:
                    iTotalValue -= c.getR1Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);  
                    iTotalValue -= c.getR2Weight() * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE2(), c);
                    break;
                case LOWER:
                    iTotalValue -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                    break;
                case HIGHER:
                    iTotalValue -= avg(c.getR1().getWeight(), c.getR2().getWeight()) * iStudentWeights.getStudentQualityConflictWeight(assignment, c.getE1(), c);
                    break;
            }
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
            iTotalPriorityCRWeight = new double[StudentPriority.values().length];
            iAssignedCRWeight = 0.0;
            iAssignedDummyCRWeight = 0.0;
            iAssignedCriticalCRWeight = new double[RequestPriority.values().length];
            iAssignedPriorityCriticalCRWeight = new double[RequestPriority.values().length][StudentPriority.values().length];
            for (int i = 0; i < RequestPriority.values().length; i++) {
                iAssignedCriticalCRWeight[i] = 0.0;
                for (int j = 0; j < StudentPriority.values().length; j++) {
                    iAssignedPriorityCriticalCRWeight[i][j] = 0.0;
                }
            }
            iAssignedPriorityCRWeight = new double[StudentPriority.values().length];
            for (int i = 0; i < StudentPriority.values().length; i++) {
                iAssignedPriorityCRWeight[i] = 0.0;
            }
            iNrDummyRequests = 0; iNrAssignedDummyRequests = 0;
            iTotalReservedSpace = 0.0; iReservedSpace = 0.0;
            iTotalMPPCRWeight = 0.0;
            iTotalSelCRWeight = 0.0;
            iAssignedNoTimeSectionWeight = 0.0;
            iAssignedOnlineSectionWeight = 0.0;
            iAssignedPastSectionWeight = 0.0;
            for (Request request: variables()) {
                boolean cr = (request instanceof CourseRequest);
                if (cr && !request.isAlternative())
                    iTotalCRWeight += request.getWeight();
                if (request.getStudent().isDummy()) {
                    iTotalDummyWeight += request.getWeight();
                    iNrDummyRequests ++;
                    if (cr && !request.isAlternative())
                        iTotalDummyCRWeight += request.getWeight();
                }
                if (cr && !request.isAlternative()) {
                    iTotalPriorityCRWeight[request.getStudent().getPriority().ordinal()] += request.getWeight();
                }
                if (request.isMPP())
                    iTotalMPPCRWeight += request.getWeight();
                if (request.hasSelection())
                    iTotalSelCRWeight += request.getWeight();
                Enrollment e = assignment.getValue(request);
                if (e != null) {
                    if (cr)
                        iAssignedCRWeight += request.getWeight();
                    if (cr && request.getRequestPriority() != RequestPriority.Normal && !request.getStudent().isDummy() && !request.isAlternative())
                        iAssignedCriticalCRWeight[request.getRequestPriority().ordinal()] += request.getWeight();
                    if (cr && request.getRequestPriority() != RequestPriority.Normal && !request.isAlternative())
                        iAssignedPriorityCriticalCRWeight[request.getRequestPriority().ordinal()][request.getStudent().getPriority().ordinal()] += request.getWeight();
                    if (request.isMPP()) {
                        iAssignedSameSectionWeight += request.getWeight() * e.percentInitial();
                        iAssignedSameChoiceWeight += request.getWeight() * e.percentSelected();
                        iAssignedSameTimeWeight += request.getWeight() * e.percentSameTime();
                    }
                    if (request.hasSelection()) {
                        iAssignedSelectedSectionWeight += request.getWeight() * e.percentSelectedSameSection();
                        iAssignedSelectedConfigWeight += request.getWeight() * e.percentSelectedSameConfig();
                    }
                    if (e.getReservation() != null)
                        iReservedSpace += request.getWeight();
                    if (cr && ((CourseRequest)request).hasReservations())
                        iTotalReservedSpace += request.getWeight();
                    if (request.getStudent().isDummy()) {
                        iNrAssignedDummyRequests ++;
                        if (cr)
                            iAssignedDummyCRWeight += request.getWeight();
                    }
                    if (cr) {
                        iAssignedPriorityCRWeight[request.getStudent().getPriority().ordinal()] += request.getWeight();
                    }
                    if (cr) {
                        int noTime = 0;
                        int online = 0;
                        int past = 0;
                        for (Section section: e.getSections()) {
                            if (!section.hasTime()) noTime ++;
                            if (section.isOnline()) online ++;
                            if (section.isPast()) past ++;
                        }
                        if (noTime > 0)
                            iAssignedNoTimeSectionWeight += request.getWeight() * noTime / e.getSections().size();
                        if (online > 0)
                            iAssignedOnlineSectionWeight += request.getWeight() * online / e.getSections().size();
                        if (past > 0)
                            iAssignedPastSectionWeight += request.getWeight() * past / e.getSections().size();
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
                if (iNrDummyStudents > 0 && iNrDummyStudents != getStudents().size() && iTotalCRWeight != iTotalDummyCRWeight) {
                    if (iTotalDummyCRWeight > 0.0)
                        info.put("Projected assigned course requests", sDecimalFormat.format(100.0 * iAssignedDummyCRWeight / iTotalDummyCRWeight) + "% (" + (int)Math.round(iAssignedDummyCRWeight) + "/" + (int)Math.round(iTotalDummyCRWeight) + ")");
                    info.put("Real assigned course requests", sDecimalFormat.format(100.0 * (iAssignedCRWeight - iAssignedDummyCRWeight) / (iTotalCRWeight - iTotalDummyCRWeight)) +
                            "% (" + (int)Math.round(iAssignedCRWeight - iAssignedDummyCRWeight) + "/" + (int)Math.round(iTotalCRWeight - iTotalDummyCRWeight) + ")");
                }
                if (iAssignedNoTimeSectionWeight > 0.0) {
                    info.put("Using classes w/o time", sDecimalFormat.format(100.0 * iAssignedNoTimeSectionWeight / iAssignedCRWeight) + "% (" + sDecimalFormat.format(iAssignedNoTimeSectionWeight) + ")"); 
                }
                if (iAssignedOnlineSectionWeight > 0.0) {
                    info.put("Using online classes", sDecimalFormat.format(100.0 * iAssignedOnlineSectionWeight / iAssignedCRWeight) + "% (" + sDecimalFormat.format(iAssignedOnlineSectionWeight) + ")"); 
                }
                if (iAssignedPastSectionWeight > 0.0) {
                    info.put("Using past classes", sDecimalFormat.format(100.0 * iAssignedPastSectionWeight / iAssignedCRWeight) + "% (" + sDecimalFormat.format(iAssignedPastSectionWeight) + ")");
                }
            }
            String priorityAssignedCR = "";
            for (StudentPriority sp: StudentPriority.values()) {
                if (sp != StudentPriority.Dummy && iTotalPriorityCRWeight[sp.ordinal()] > 0.0) {
                    priorityAssignedCR += (priorityAssignedCR.isEmpty() ? "" : "\n") +
                            sp.name() + ": " + sDecimalFormat.format(100.0 * iAssignedPriorityCRWeight[sp.ordinal()] / iTotalPriorityCRWeight[sp.ordinal()]) + "% (" + (int)Math.round(iAssignedPriorityCRWeight[sp.ordinal()]) + "/" + (int)Math.round(iTotalPriorityCRWeight[sp.ordinal()]) + ")";
                }
            }
            if (!priorityAssignedCR.isEmpty())
                info.put("Assigned course requests (priority students)", priorityAssignedCR);
            for (RequestPriority rp: RequestPriority.values()) {
                if (rp == RequestPriority.Normal) continue;
                if (iTotalCriticalCRWeight[rp.ordinal()] > 0.0) {
                    info.put("Assigned " + rp.name().toLowerCase() + " course requests", sDoubleFormat.format(100.0 * iAssignedCriticalCRWeight[rp.ordinal()] / iTotalCriticalCRWeight[rp.ordinal()]) + "% (" + (int)Math.round(iAssignedCriticalCRWeight[rp.ordinal()]) + "/" + (int)Math.round(iTotalCriticalCRWeight[rp.ordinal()]) + ")");
                }
                priorityAssignedCR = "";
                for (StudentPriority sp: StudentPriority.values()) {
                    if (sp != StudentPriority.Dummy && iTotalPriorityCriticalCRWeight[rp.ordinal()][sp.ordinal()] > 0.0) {
                        priorityAssignedCR += (priorityAssignedCR.isEmpty() ? "" : "\n") +
                                sp.name() + ": " + sDoubleFormat.format(100.0 * iAssignedPriorityCriticalCRWeight[rp.ordinal()][sp.ordinal()] / iTotalPriorityCriticalCRWeight[rp.ordinal()][sp.ordinal()]) + "% (" + (int)Math.round(iAssignedPriorityCriticalCRWeight[rp.ordinal()][sp.ordinal()]) + "/" + (int)Math.round(iTotalPriorityCriticalCRWeight[rp.ordinal()][sp.ordinal()]) + ")";
                    }
                }
                if (!priorityAssignedCR.isEmpty())
                    info.put("Assigned " + rp.name().toLowerCase() + " course requests (priority students)", priorityAssignedCR);
            }
            if (iTotalReservedSpace > 0.0)
                info.put("Reservations", sDoubleFormat.format(100.0 * iReservedSpace / iTotalReservedSpace) + "% (" + Math.round(iReservedSpace) + "/" + Math.round(iTotalReservedSpace) + ")");
            if (iMPP && iTotalMPPCRWeight > 0.0) {
                info.put("Perturbations: same section", sDoubleFormat.format(100.0 * iAssignedSameSectionWeight / iTotalMPPCRWeight) + "% (" + Math.round(iAssignedSameSectionWeight) + "/" + Math.round(iTotalMPPCRWeight) + ")");
                if (iAssignedSameChoiceWeight > iAssignedSameSectionWeight)
                    info.put("Perturbations: same choice",sDoubleFormat.format(100.0 * iAssignedSameChoiceWeight / iTotalMPPCRWeight) + "% (" + Math.round(iAssignedSameChoiceWeight) + "/" + Math.round(iTotalMPPCRWeight) + ")");
                if (iAssignedSameTimeWeight > iAssignedSameChoiceWeight)
                    info.put("Perturbations: same time", sDoubleFormat.format(100.0 * iAssignedSameTimeWeight / iTotalMPPCRWeight) + "% (" + Math.round(iAssignedSameTimeWeight) + "/" + Math.round(iTotalMPPCRWeight) + ")");
            }
            if (iTotalSelCRWeight > 0.0) {
                info.put("Selection",sDoubleFormat.format(100.0 * (0.3 * iAssignedSelectedConfigWeight + 0.7 * iAssignedSelectedSectionWeight) / iTotalSelCRWeight) +
                        "% (" + Math.round(0.3 * iAssignedSelectedConfigWeight + 0.7 * iAssignedSelectedSectionWeight) + "/" + Math.round(iTotalSelCRWeight) + ")");
            }
        }

        @Override
        public void getInfo(Assignment<Request, Enrollment> assignment, Map<String, String> info, Collection<Request> variables) {
        }
        
        public double getAssignedCourseRequestWeight() {
            return iAssignedCRWeight;
        }
        
        public double getAssignedCriticalCourseRequestWeight(RequestPriority rp) {
            return iAssignedCriticalCRWeight[rp.ordinal()];
        }
    }
    
    @Override
    public InheritedAssignment<Request, Enrollment> createInheritedAssignment(Solution<Request, Enrollment> solution, int index) {
        return new OptimisticInheritedAssignment<Request, Enrollment>(solution, index);
    }
    
    public DistanceMetric getDistanceMetric() {
        return (iStudentQuality != null ? iStudentQuality.getDistanceMetric() : iDistanceConflict != null ? iDistanceConflict.getDistanceMetric() : null);
    }

    @Override
    public StudentSectioningModelContext inheritAssignmentContext(Assignment<Request, Enrollment> assignment, StudentSectioningModelContext parentContext) {
        return new StudentSectioningModelContext(parentContext);
    }

}
