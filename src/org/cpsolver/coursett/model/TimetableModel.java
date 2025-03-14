package org.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.ClassLimitConstraint;
import org.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import org.cpsolver.coursett.constraint.FlexibleConstraint;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.constraint.RoomConstraint;
import org.cpsolver.coursett.constraint.SpreadConstraint;
import org.cpsolver.coursett.criteria.BackToBackInstructorPreferences;
import org.cpsolver.coursett.criteria.BrokenTimePatterns;
import org.cpsolver.coursett.criteria.DepartmentBalancingPenalty;
import org.cpsolver.coursett.criteria.DistributionPreferences;
import org.cpsolver.coursett.criteria.FlexibleConstraintCriterion;
import org.cpsolver.coursett.criteria.Perturbations;
import org.cpsolver.coursett.criteria.RoomPreferences;
import org.cpsolver.coursett.criteria.RoomViolations;
import org.cpsolver.coursett.criteria.SameSubpartBalancingPenalty;
import org.cpsolver.coursett.criteria.StudentCommittedConflict;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.criteria.StudentDistanceConflict;
import org.cpsolver.coursett.criteria.StudentHardConflict;
import org.cpsolver.coursett.criteria.StudentOverlapConflict;
import org.cpsolver.coursett.criteria.StudentWorkdayConflict;
import org.cpsolver.coursett.criteria.TimePreferences;
import org.cpsolver.coursett.criteria.TimeViolations;
import org.cpsolver.coursett.criteria.TooBigRooms;
import org.cpsolver.coursett.criteria.UselessHalfHours;
import org.cpsolver.coursett.criteria.additional.InstructorConflict;
import org.cpsolver.coursett.criteria.placement.DeltaTimePreference;
import org.cpsolver.coursett.criteria.placement.HardConflicts;
import org.cpsolver.coursett.criteria.placement.PotentialHardConflicts;
import org.cpsolver.coursett.criteria.placement.WeightedHardConflicts;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.constant.ConstantModel;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.termination.TerminationCondition;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;


/**
 * Timetable model.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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

public class TimetableModel extends ConstantModel<Lecture, Placement> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(TimetableModel.class);
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));

    private List<InstructorConstraint> iInstructorConstraints = new ArrayList<InstructorConstraint>();
    private List<JenrlConstraint> iJenrlConstraints = new ArrayList<JenrlConstraint>();
    private List<RoomConstraint> iRoomConstraints = new ArrayList<RoomConstraint>();
    private List<DepartmentSpreadConstraint> iDepartmentSpreadConstraints = new ArrayList<DepartmentSpreadConstraint>();
    private List<SpreadConstraint> iSpreadConstraints = new ArrayList<SpreadConstraint>();
    private List<GroupConstraint> iGroupConstraints = new ArrayList<GroupConstraint>();
    private List<ClassLimitConstraint> iClassLimitConstraints = new ArrayList<ClassLimitConstraint>();
    private List<FlexibleConstraint> iFlexibleConstraints = new ArrayList<FlexibleConstraint>();
    private DataProperties iProperties = null;
    private int iYear = -1;
    private List<BitSet> iWeeks = null;
    private boolean iOnFlySectioning = false;
    private int iStudentWorkDayLimit = -1;
    private boolean iAllowBreakHard = false;

    private HashSet<Student> iAllStudents = new HashSet<Student>();
    
    private DistanceMetric iDistanceMetric = null;
    
    private StudentSectioning iStudentSectioning = null;
    private List<StudentGroup> iStudentGroups = new ArrayList<StudentGroup>();
    
    private boolean iUseCriteria = true;
    private List<StudentConflict> iStudentConflictCriteria = null;

    @SuppressWarnings("unchecked")
    public TimetableModel(DataProperties properties) {
        super();
        iProperties = properties;
        iDistanceMetric = new DistanceMetric(properties);
        if (properties.getPropertyBoolean("OnFlySectioning.Enabled", false)) {
            addModelListener(new OnFlySectioning(this)); iOnFlySectioning = true;
        }
        iStudentWorkDayLimit = properties.getPropertyInt("StudentConflict.WorkDayLimit", -1);
        iAllowBreakHard = properties.getPropertyBoolean("General.AllowBreakHard", false);
        String criteria = properties.getProperty("General.Criteria",
                // Objectives
                StudentConflict.class.getName() + ";" +
                StudentDistanceConflict.class.getName() + ";" +
                StudentHardConflict.class.getName() + ";" +
                StudentCommittedConflict.class.getName() + ";" +
                StudentOverlapConflict.class.getName() + ";" +
                UselessHalfHours.class.getName() + ";" +
                BrokenTimePatterns.class.getName() + ";" +
                TooBigRooms.class.getName() + ";" +
                TimePreferences.class.getName() + ";" +
                RoomPreferences.class.getName() + ";" +
                DistributionPreferences.class.getName() + ";" +
                SameSubpartBalancingPenalty.class.getName() + ";" +
                DepartmentBalancingPenalty.class.getName() + ";" +
                BackToBackInstructorPreferences.class.getName() + ";" +
                Perturbations.class.getName() + ";" +
                // Additional placement selection criteria
                // AssignmentCount.class.getName() + ";" +
                DeltaTimePreference.class.getName() + ";" +
                HardConflicts.class.getName() + ";" +
                PotentialHardConflicts.class.getName() + ";" +
                FlexibleConstraintCriterion.class.getName() + ";" +
                WeightedHardConflicts.class.getName());
        if (iStudentWorkDayLimit > 0)
            criteria += ";" + StudentWorkdayConflict.class.getName();
        // Interactive mode -- count time / room violations
        if (properties.getPropertyBoolean("General.InteractiveMode", false))
            criteria += ";" + TimeViolations.class.getName() + ";" + RoomViolations.class.getName();
        else if (properties.getPropertyBoolean("General.AllowProhibitedRooms", false)) {
            criteria += ";" + RoomViolations.class.getName();
            iAllowBreakHard = true;
        }
        // Additional (custom) criteria
        criteria += ";" + properties.getProperty("General.AdditionalCriteria", "");
        for (String criterion: criteria.split("\\;")) {
            if (criterion == null || criterion.isEmpty()) continue;
            try {
                Class<Criterion<Lecture, Placement>> clazz = (Class<Criterion<Lecture, Placement>>)Class.forName(criterion);
                Criterion<Lecture, Placement> c = clazz.newInstance();
                c.configure(properties);
                addCriterion(c);
            } catch (Exception e) {
                sLogger.error("Unable to use " + criterion + ": " + e.getMessage());
            }
        }
        if (properties.getPropertyBoolean("General.SoftInstructorConstraints", false)) {
            InstructorConflict ic = new InstructorConflict(); ic.configure(properties);
            addCriterion(ic);
        }
        try {
            String studentSectioningClassName = properties.getProperty("StudentSectioning.Class", DefaultStudentSectioning.class.getName());
            Class<?> studentSectioningClass = Class.forName(studentSectioningClassName);
            iStudentSectioning = (StudentSectioning)studentSectioningClass.getConstructor(TimetableModel.class).newInstance(this);
        } catch (Exception e) {
            sLogger.error("Failed to load custom student sectioning class: " + e.getMessage());
            iStudentSectioning = new DefaultStudentSectioning(this);
        }
        if (iStudentSectioning instanceof InfoProvider<?, ?>) {
            getInfoProviders().add((InfoProvider<Lecture, Placement>)iStudentSectioning);
        }
        String constraints = properties.getProperty("General.GlobalConstraints", "");
        for (String constraint: constraints.split("\\;")) {
            if (constraint == null || constraint.isEmpty()) continue;
            try {
                Class<GlobalConstraint<Lecture, Placement>> clazz = (Class<GlobalConstraint<Lecture, Placement>>)Class.forName(constraint);
                GlobalConstraint<Lecture, Placement> c = clazz.newInstance();
                addGlobalConstraint(c);
            } catch (Exception e) {
                sLogger.error("Unable to use " + constraint + ": " + e.getMessage());
            }
        }
        iUseCriteria = properties.getPropertyBoolean("SctSectioning.UseCriteria", true);
    }

    public DistanceMetric getDistanceMetric() {
        return iDistanceMetric;
    }
    
    public int getStudentWorkDayLimit() {
        return iStudentWorkDayLimit;
    }
    
    /**
     * Returns interface to the student sectioning functions needed during course timetabling.
     * Defaults to an instance of {@link DefaultStudentSectioning}, can be changed using the StudentSectioning.Class parameter.
     * @return student sectioning
     */
    public StudentSectioning getStudentSectioning() {
        return iStudentSectioning;
    }

    public DataProperties getProperties() {
        return iProperties;
    }

    /**
     * Student final sectioning (switching students between sections of the same
     * class in order to minimize overall number of student conflicts)
     * @param assignment current assignment
     * @param termination optional termination condition
     */
    public void switchStudents(Assignment<Lecture, Placement> assignment, TerminationCondition<Lecture, Placement> termination) {
        getStudentSectioning().switchStudents(new Solution<Lecture, Placement>(this, assignment), termination);
    }
    
    /**
     * Student final sectioning (switching students between sections of the same
     * class in order to minimize overall number of student conflicts)
     * @param assignment current assignment
     */
    public void switchStudents(Assignment<Lecture, Placement> assignment) {
        getStudentSectioning().switchStudents(new Solution<Lecture, Placement>(this, assignment), null);
    }

    public Map<String, String> getBounds(Assignment<Lecture, Placement> assignment) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("Room preferences min", "" + getCriterion(RoomPreferences.class).getBounds(assignment)[0]);
        ret.put("Room preferences max", "" + getCriterion(RoomPreferences.class).getBounds(assignment)[1]);
        ret.put("Time preferences min", "" + getCriterion(TimePreferences.class).getBounds(assignment)[0]);
        ret.put("Time preferences max", "" + getCriterion(TimePreferences.class).getBounds(assignment)[1]);
        ret.put("Distribution preferences min", "" + getCriterion(DistributionPreferences.class).getBounds(assignment)[0]);
        ret.put("Distribution preferences max", "" + getCriterion(DistributionPreferences.class).getBounds(assignment)[1]);
        if (getProperties().getPropertyBoolean("General.UseDistanceConstraints", false)) {
            ret.put("Back-to-back instructor preferences max", "" + getCriterion(BackToBackInstructorPreferences.class).getBounds(assignment)[1]);
        }
        ret.put("Too big rooms max", "" + getCriterion(TooBigRooms.class).getBounds(assignment)[0]);
        ret.put("Useless half-hours", "" + getCriterion(UselessHalfHours.class).getBounds(assignment)[0]);
        return ret;
    }

    /** Global info */
    @Override
    public Map<String, String> getInfo(Assignment<Lecture, Placement> assignment) {
        Map<String, String> ret = super.getInfo(assignment);
        ret.put("Memory usage", getMem());
        
        Criterion<Lecture, Placement> rp = getCriterion(RoomPreferences.class);
        Criterion<Lecture, Placement> rv = getCriterion(RoomViolations.class);
        ret.put("Room preferences", getPerc(rp.getValue(assignment), rp.getBounds(assignment)[0], rp.getBounds(assignment)[1]) + "% (" + Math.round(rp.getValue(assignment)) + ")"
                + (rv != null && rv.getValue(assignment) >= 0.5 ? " [hard:" + Math.round(rv.getValue(assignment)) + "]" : ""));
        
        Criterion<Lecture, Placement> tp = getCriterion(TimePreferences.class);
        Criterion<Lecture, Placement> tv = getCriterion(TimeViolations.class);
        ret.put("Time preferences", getPerc(tp.getValue(assignment), tp.getBounds(assignment)[0], tp.getBounds(assignment)[1]) + "% (" + sDoubleFormat.format(tp.getValue(assignment)) + ")"
                + (tv != null && tv.getValue(assignment) >= 0.5 ? " [hard:" + Math.round(tv.getValue(assignment)) + "]" : ""));

        Criterion<Lecture, Placement> dp = getCriterion(DistributionPreferences.class);
        ret.put("Distribution preferences", getPerc(dp.getValue(assignment), dp.getBounds(assignment)[0], dp.getBounds(assignment)[1]) + "% (" + sDoubleFormat.format(dp.getValue(assignment)) + ")");
        
        Criterion<Lecture, Placement> sc = getCriterion(StudentConflict.class);
        Criterion<Lecture, Placement> shc = getCriterion(StudentHardConflict.class);
        Criterion<Lecture, Placement> sdc = getCriterion(StudentDistanceConflict.class);
        Criterion<Lecture, Placement> scc = getCriterion(StudentCommittedConflict.class);
        ret.put("Student conflicts", Math.round(scc.getValue(assignment) + sc.getValue(assignment)) +
                " [committed:" + Math.round(scc.getValue(assignment)) +
                ", distance:" + Math.round(sdc.getValue(assignment)) +
                ", hard:" + Math.round(shc.getValue(assignment)) + "]");
        
        if (!getSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> ip = getCriterion(BackToBackInstructorPreferences.class);
            ret.put("Back-to-back instructor preferences", getPerc(ip.getValue(assignment), ip.getBounds(assignment)[0], ip.getBounds(assignment)[1]) + "% (" + Math.round(ip.getValue(assignment)) + ")");
        }

        if (!getDepartmentSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> dbp = getCriterion(DepartmentBalancingPenalty.class);
            ret.put("Department balancing penalty", sDoubleFormat.format(dbp.getValue(assignment)));
        }
        
        Criterion<Lecture, Placement> sbp = getCriterion(SameSubpartBalancingPenalty.class);
        ret.put("Same subpart balancing penalty", sDoubleFormat.format(sbp.getValue(assignment)));
        
        Criterion<Lecture, Placement> tbr = getCriterion(TooBigRooms.class);
        ret.put("Too big rooms", getPercRev(tbr.getValue(assignment), tbr.getBounds(assignment)[1], tbr.getBounds(assignment)[0]) + "% (" + Math.round(tbr.getValue(assignment)) + ")");
        
        Criterion<Lecture, Placement> uh = getCriterion(UselessHalfHours.class);
        Criterion<Lecture, Placement> bt = getCriterion(BrokenTimePatterns.class);

        ret.put("Useless half-hours", getPercRev(uh.getValue(assignment) + bt.getValue(assignment), 0, Constants.sPreferenceLevelStronglyDiscouraged * bt.getBounds(assignment)[0]) +
                "% (" + Math.round(uh.getValue(assignment)) + " + " + Math.round(bt.getValue(assignment)) + ")");
        return ret;
    }

    @Override
    public Map<String, String> getInfo(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        Map<String, String> ret = super.getInfo(assignment, variables);
        
        ret.put("Memory usage", getMem());
        
        Criterion<Lecture, Placement> rp = getCriterion(RoomPreferences.class);
        ret.put("Room preferences", getPerc(rp.getValue(assignment, variables), rp.getBounds(assignment, variables)[0], rp.getBounds(assignment, variables)[1]) + "% (" + Math.round(rp.getValue(assignment, variables)) + ")");
        
        Criterion<Lecture, Placement> tp = getCriterion(TimePreferences.class);
        ret.put("Time preferences", getPerc(tp.getValue(assignment, variables), tp.getBounds(assignment, variables)[0], tp.getBounds(assignment, variables)[1]) + "% (" + sDoubleFormat.format(tp.getValue(assignment, variables)) + ")"); 

        Criterion<Lecture, Placement> dp = getCriterion(DistributionPreferences.class);
        ret.put("Distribution preferences", getPerc(dp.getValue(assignment, variables), dp.getBounds(assignment, variables)[0], dp.getBounds(assignment, variables)[1]) + "% (" + sDoubleFormat.format(dp.getValue(assignment, variables)) + ")");
        
        Criterion<Lecture, Placement> sc = getCriterion(StudentConflict.class);
        Criterion<Lecture, Placement> shc = getCriterion(StudentHardConflict.class);
        Criterion<Lecture, Placement> sdc = getCriterion(StudentDistanceConflict.class);
        Criterion<Lecture, Placement> scc = getCriterion(StudentCommittedConflict.class);
        ret.put("Student conflicts", Math.round(scc.getValue(assignment, variables) + sc.getValue(assignment, variables)) +
                " [committed:" + Math.round(scc.getValue(assignment, variables)) +
                ", distance:" + Math.round(sdc.getValue(assignment, variables)) +
                ", hard:" + Math.round(shc.getValue(assignment, variables)) + "]");
        
        if (!getSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> ip = getCriterion(BackToBackInstructorPreferences.class);
            ret.put("Back-to-back instructor preferences", getPerc(ip.getValue(assignment, variables), ip.getBounds(assignment, variables)[0], ip.getBounds(assignment, variables)[1]) + "% (" + Math.round(ip.getValue(assignment, variables)) + ")");
        }

        if (!getDepartmentSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> dbp = getCriterion(DepartmentBalancingPenalty.class);
            ret.put("Department balancing penalty", sDoubleFormat.format(dbp.getValue(assignment, variables)));
        }
        
        Criterion<Lecture, Placement> sbp = getCriterion(SameSubpartBalancingPenalty.class);
        ret.put("Same subpart balancing penalty", sDoubleFormat.format(sbp.getValue(assignment, variables)));
        
        Criterion<Lecture, Placement> tbr = getCriterion(TooBigRooms.class);
        ret.put("Too big rooms", getPercRev(tbr.getValue(assignment, variables), tbr.getBounds(assignment, variables)[1], tbr.getBounds(assignment, variables)[0]) + "% (" + Math.round(tbr.getValue(assignment, variables)) + ")");
        
        Criterion<Lecture, Placement> uh = getCriterion(UselessHalfHours.class);
        Criterion<Lecture, Placement> bt = getCriterion(BrokenTimePatterns.class);

        ret.put("Useless half-hours", getPercRev(uh.getValue(assignment, variables) + bt.getValue(assignment, variables), 0, Constants.sPreferenceLevelStronglyDiscouraged * bt.getBounds(assignment, variables)[0]) +
                "% (" + Math.round(uh.getValue(assignment, variables)) + " + " + Math.round(bt.getValue(assignment, variables)) + ")");
        return ret;
    }

    @Override
    public void addConstraint(Constraint<Lecture, Placement> constraint) {
        super.addConstraint(constraint);
        if (constraint instanceof InstructorConstraint) {
            iInstructorConstraints.add((InstructorConstraint) constraint);
        } else if (constraint instanceof JenrlConstraint) {
            iJenrlConstraints.add((JenrlConstraint) constraint);
        } else if (constraint instanceof RoomConstraint) {
            iRoomConstraints.add((RoomConstraint) constraint);
        } else if (constraint instanceof DepartmentSpreadConstraint) {
            iDepartmentSpreadConstraints.add((DepartmentSpreadConstraint) constraint);
        } else if (constraint instanceof SpreadConstraint) {
            iSpreadConstraints.add((SpreadConstraint) constraint);
        } else if (constraint instanceof ClassLimitConstraint) {
            iClassLimitConstraints.add((ClassLimitConstraint) constraint);
        } else if (constraint instanceof GroupConstraint) {
            iGroupConstraints.add((GroupConstraint) constraint);
        } else if (constraint instanceof FlexibleConstraint) {
            iFlexibleConstraints.add((FlexibleConstraint) constraint);
        }
    }

    @Override
    public void removeConstraint(Constraint<Lecture, Placement> constraint) {
        super.removeConstraint(constraint);
        if (constraint instanceof InstructorConstraint) {
            iInstructorConstraints.remove(constraint);
        } else if (constraint instanceof JenrlConstraint) {
            iJenrlConstraints.remove(constraint);
        } else if (constraint instanceof RoomConstraint) {
            iRoomConstraints.remove(constraint);
        } else if (constraint instanceof DepartmentSpreadConstraint) {
            iDepartmentSpreadConstraints.remove(constraint);
        } else if (constraint instanceof SpreadConstraint) {
            iSpreadConstraints.remove(constraint);
        } else if (constraint instanceof ClassLimitConstraint) {
            iClassLimitConstraints.remove(constraint);
        } else if (constraint instanceof GroupConstraint) {
            iGroupConstraints.remove(constraint);
        } else if (constraint instanceof FlexibleConstraint) {
            iFlexibleConstraints.remove(constraint);
        }
    }

    /** The list of all instructor constraints 
     * @return list of instructor constraints
     **/
    public List<InstructorConstraint> getInstructorConstraints() {
        return iInstructorConstraints;
    }

    /** The list of all group constraints
     * @return list of group (distribution) constraints
     **/
    public List<GroupConstraint> getGroupConstraints() {
        return iGroupConstraints;
    }

    /** The list of all jenrl constraints
     * @return list of join enrollment constraints
     **/
    public List<JenrlConstraint> getJenrlConstraints() {
        return iJenrlConstraints;
    }

    /** The list of all room constraints 
     * @return list of room constraints
     **/
    public List<RoomConstraint> getRoomConstraints() {
        return iRoomConstraints;
    }

    /** The list of all departmental spread constraints 
     * @return list of department spread constraints
     **/
    public List<DepartmentSpreadConstraint> getDepartmentSpreadConstraints() {
        return iDepartmentSpreadConstraints;
    }

    public List<SpreadConstraint> getSpreadConstraints() {
        return iSpreadConstraints;
    }

    public List<ClassLimitConstraint> getClassLimitConstraints() {
        return iClassLimitConstraints;
    }
    
    public List<FlexibleConstraint> getFlexibleConstraints() {
        return iFlexibleConstraints;
    }
    
    @Override
    public double getTotalValue(Assignment<Lecture, Placement> assignment) {
        double ret = 0;
        for (Criterion<Lecture, Placement> criterion: getCriteria())
            ret += criterion.getWeightedValue(assignment);
        return ret;
    }

    @Override
    public double getTotalValue(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double ret = 0;
        for (Criterion<Lecture, Placement> criterion: getCriteria())
            ret += criterion.getWeightedValue(assignment, variables);
        return ret;
    }

    public int getYear() {
        return iYear;
    }

    public void setYear(int year) {
        iYear = year;
    }

    public Set<Student> getAllStudents() {
        return iAllStudents;
    }

    public void addStudent(Student student) {
        iAllStudents.add(student);
    }

    public void removeStudent(Student student) {
        iAllStudents.remove(student);
    }

    /**
     * Returns amount of allocated memory.
     * 
     * @return amount of allocated memory to be written in the log
     */
    public static synchronized String getMem() {
        Runtime rt = Runtime.getRuntime();
        return sDoubleFormat.format(((double) (rt.totalMemory() - rt.freeMemory())) / 1048576) + "M";
    }
    
    
    /**
     * Returns the set of conflicting variables with this value, if it is
     * assigned to its variable. Conflicts with constraints that implement
     * {@link WeakeningConstraint} are ignored.
     * @param assignment current assignment
     * @param value placement that is being considered
     * @return computed conflicting assignments
     */
    public Set<Placement> conflictValuesSkipWeakeningConstraints(Assignment<Lecture, Placement> assignment, Placement value) {
        Set<Placement> conflictValues = new HashSet<Placement>();
        for (Constraint<Lecture, Placement> constraint : value.variable().hardConstraints()) {
            if (constraint instanceof WeakeningConstraint) continue;
            if (constraint instanceof GroupConstraint)
                ((GroupConstraint)constraint).computeConflictsNoForwardCheck(assignment, value, conflictValues);
            else
                constraint.computeConflicts(assignment, value, conflictValues);
        }
        for (GlobalConstraint<Lecture, Placement> constraint : globalConstraints()) {
            if (constraint instanceof WeakeningConstraint) continue;
            constraint.computeConflicts(assignment, value, conflictValues);
        }
        return conflictValues;
    }
    
    /**
     * The method creates date patterns (bitsets) which represent the weeks of a
     * semester.
     *      
     * @return a list of BitSets which represents the weeks of a semester.
     */
    public List<BitSet> getWeeks() {
        if (iWeeks == null) {
            String defaultDatePattern = getProperties().getProperty("DatePattern.CustomDatePattern", null);
            if (defaultDatePattern == null){                
                defaultDatePattern = getProperties().getProperty("DatePattern.Default");
            }
            BitSet fullTerm = null;
            if (defaultDatePattern == null) {
                // Take the date pattern that is being used most often
                Map<Long, Integer> counter = new HashMap<Long, Integer>();
                int max = 0; String name = null; Long id = null;
                for (Lecture lecture: variables()) {
                    if (lecture.isCommitted()) continue;
                    for (TimeLocation time: lecture.timeLocations()) {
                        if (time.getWeekCode() != null && time.getDatePatternId() != null) {
                            int count = 1;
                            if (counter.containsKey(time.getDatePatternId()))
                                count += counter.get(time.getDatePatternId());
                            counter.put(time.getDatePatternId(), count);
                            if (count > max) {
                                max = count; fullTerm = time.getWeekCode(); name = time.getDatePatternName(); id = time.getDatePatternId();
                            }
                        }
                    }
                }
                sLogger.info("Using date pattern " + name + " (id " + id + ") as the default.");
            } else {
                // Create default date pattern
                fullTerm = new BitSet(defaultDatePattern.length());
                for (int i = 0; i < defaultDatePattern.length(); i++) {
                    if (defaultDatePattern.charAt(i) == 49) {
                        fullTerm.set(i);
                    }
                }
            }
            
            if (fullTerm == null) return null;
            
            iWeeks = new ArrayList<BitSet>();
            if (getProperties().getPropertyBoolean("DatePattern.ShiftWeeks", false)) {
                // Cut date pattern into weeks (each week takes 7 consecutive bits, starting on the next positive bit)
                for (int i = fullTerm.nextSetBit(0); i < fullTerm.length(); ) {
                    if (!fullTerm.get(i)) {
                        i++; continue;
                    }
                    BitSet w = new BitSet(i + 7);
                    for (int j = 0; j < 7; j++)
                        if (fullTerm.get(i + j)) w.set(i + j);
                    iWeeks.add(w);
                    i += 7;
                }                
            } else {
                // Cut date pattern into weeks (each week takes 7 consecutive bits starting on the first bit of the default date pattern, no pauses between weeks)
                for (int i = fullTerm.nextSetBit(0); i < fullTerm.length(); ) {
                    BitSet w = new BitSet(i + 7);
                    for (int j = 0; j < 7; j++)
                        if (fullTerm.get(i + j)) w.set(i + j);
                    iWeeks.add(w);
                    i += 7;
                }
            }
        }
        return iWeeks;
    }
    
    public List<StudentGroup> getStudentGroups() { return iStudentGroups; }
    public void addStudentGroup(StudentGroup group) { iStudentGroups.add(group); }
    
    Map<Student, Set<Lecture>> iBestEnrollment = null;
    @Override
    public void saveBest(Assignment<Lecture, Placement> assignment) {
        super.saveBest(assignment);
        if (iOnFlySectioning) {
            if (iBestEnrollment == null)
                iBestEnrollment = new HashMap<Student, Set<Lecture>>();
            else
                iBestEnrollment.clear();
            for (Student student: getAllStudents())
                iBestEnrollment.put(student, new HashSet<Lecture>(student.getLectures()));
        }
    }
    
    /**
     * Increment {@link JenrlConstraint} between the given two classes by the given student
     */
    protected void incJenrl(Assignment<Lecture, Placement> assignment, Student student, Lecture l1, Lecture l2) {
        if (l1.equals(l2)) return;
        JenrlConstraint jenrl = l1.jenrlConstraint(l2);
        if (jenrl == null) {
            jenrl = new JenrlConstraint();
            jenrl.addVariable(l1);
            jenrl.addVariable(l2);
            addConstraint(jenrl);
        }
        jenrl.incJenrl(assignment, student);
    }
    
    /**
     * Decrement {@link JenrlConstraint} between the given two classes by the given student
     */
    protected void decJenrl(Assignment<Lecture, Placement> assignment, Student student, Lecture l1, Lecture l2) {
        if (l1.equals(l2)) return;
        JenrlConstraint jenrl = l1.jenrlConstraint(l2);
        if (jenrl != null) {
            jenrl.decJenrl(assignment, student);
        }
    }
    
    @Override
    public void restoreBest(Assignment<Lecture, Placement> assignment) {
        if (iOnFlySectioning && iBestEnrollment != null) {
            
            // unassign changed classes
            for (Lecture lecture: variables()) {
                Placement placement = assignment.getValue(lecture);
                if (placement != null && !placement.equals(lecture.getBestAssignment()))
                    assignment.unassign(0, lecture);
            }
            
            for (Map.Entry<Student, Set<Lecture>> entry: iBestEnrollment.entrySet()) {
                Student student = entry.getKey();
                Set<Lecture> lectures = entry.getValue();
                Set<Configuration> configs = new HashSet<Configuration>();
                for (Lecture lecture: lectures)
                    if (lecture.getConfiguration() != null) configs.add(lecture.getConfiguration());
                
                // drop student from classes that are not in the best enrollment
                for (Lecture lecture: new ArrayList<Lecture>(student.getLectures())) {
                    if (lectures.contains(lecture)) continue; // included in best
                    for (Lecture other: student.getLectures())
                        decJenrl(assignment, student, lecture, other);
                    lecture.removeStudent(assignment, student);
                    student.removeLecture(lecture);
                    if (lecture.getConfiguration() != null && !configs.contains(lecture.getConfiguration()))
                        student.removeConfiguration(lecture.getConfiguration());
                }
                
                // add student to classes that are in the best enrollment
                for (Lecture lecture: lectures) {
                    if (student.getLectures().contains(lecture)) continue; // already in
                    for (Lecture other: student.getLectures())
                        incJenrl(assignment, student, lecture, other);
                    lecture.addStudent(assignment, student);
                    student.addLecture(lecture);
                    student.addConfiguration(lecture.getConfiguration());
                }
            }
            // remove empty joint enrollments
            for (Iterator<JenrlConstraint> i = iJenrlConstraints.iterator(); i.hasNext(); ) {
                JenrlConstraint jenrl = i.next();
                if (jenrl.getNrStudents() == 0) {
                    jenrl.getContext(assignment).unassigned(assignment, null);
                    Object[] vars = jenrl.variables().toArray();
                    for (int k = 0; k < vars.length; k++)
                        jenrl.removeVariable((Lecture) vars[k]);
                    i.remove();
                }
            }
            for (Iterator<Constraint<Lecture, Placement>> i = constraints().iterator(); i.hasNext(); ) {
                Constraint<Lecture, Placement> c = i.next();
                if (c instanceof JenrlConstraint && ((JenrlConstraint)c).getNrStudents() == 0) {
                    removeReference((JenrlConstraint)c);
                    i.remove();
                }
            }
            /*
            for (JenrlConstraint jenrl: new ArrayList<JenrlConstraint>(getJenrlConstraints())) {
                if (jenrl.getNrStudents() == 0) {
                    jenrl.getContext(assignment).unassigned(assignment, null);
                    Object[] vars = jenrl.variables().toArray();
                    for (int k = 0; k < vars.length; k++)
                        jenrl.removeVariable((Lecture) vars[k]);
                    removeConstraint(jenrl);
                }
            }
            */
        }
        super.restoreBest(assignment);
    }
    
    public boolean isAllowBreakHard() { return iAllowBreakHard; }
    
    public boolean isOnFlySectioningEnabled() { return iOnFlySectioning; }
    public void setOnFlySectioningEnabled(boolean onFlySectioning) { iOnFlySectioning = onFlySectioning; }
    
    @Override
    public void addCriterion(Criterion<Lecture, Placement> criterion) {
        super.addCriterion(criterion);
        iStudentConflictCriteria = null;
    }
    
    @Override
    public void removeCriterion(Criterion<Lecture, Placement> criterion) {
        super.removeCriterion(criterion);
        iStudentConflictCriteria = null;
    }
    
    /**
     * List of student conflict criteria
     */
    public List<StudentConflict> getStudentConflictCriteria() {
        if (!iUseCriteria) return null;
        if (iStudentConflictCriteria == null) {
            iStudentConflictCriteria = new ArrayList<StudentConflict>();
            for (Criterion<Lecture, Placement> criterion: getCriteria())
                if (criterion instanceof StudentConflict)
                    iStudentConflictCriteria.add((StudentConflict)criterion);
        }
        return iStudentConflictCriteria;
    }
}
