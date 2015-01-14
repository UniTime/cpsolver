package net.sf.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.ClassLimitConstraint;
import net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.constraint.FlexibleConstraint;
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.criteria.BackToBackInstructorPreferences;
import net.sf.cpsolver.coursett.criteria.BrokenTimePatterns;
import net.sf.cpsolver.coursett.criteria.DepartmentBalancingPenalty;
import net.sf.cpsolver.coursett.criteria.DistributionPreferences;
import net.sf.cpsolver.coursett.criteria.FlexibleConstraintCriterion;
import net.sf.cpsolver.coursett.criteria.Perturbations;
import net.sf.cpsolver.coursett.criteria.RoomPreferences;
import net.sf.cpsolver.coursett.criteria.RoomViolations;
import net.sf.cpsolver.coursett.criteria.SameSubpartBalancingPenalty;
import net.sf.cpsolver.coursett.criteria.StudentCommittedConflict;
import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.criteria.StudentDistanceConflict;
import net.sf.cpsolver.coursett.criteria.StudentHardConflict;
import net.sf.cpsolver.coursett.criteria.StudentOverlapConflict;
import net.sf.cpsolver.coursett.criteria.TimePreferences;
import net.sf.cpsolver.coursett.criteria.TimeViolations;
import net.sf.cpsolver.coursett.criteria.TooBigRooms;
import net.sf.cpsolver.coursett.criteria.UselessHalfHours;
import net.sf.cpsolver.coursett.criteria.placement.AssignmentCount;
import net.sf.cpsolver.coursett.criteria.placement.DeltaTimePreference;
import net.sf.cpsolver.coursett.criteria.placement.HardConflicts;
import net.sf.cpsolver.coursett.criteria.placement.PotentialHardConflicts;
import net.sf.cpsolver.coursett.criteria.placement.WeightedHardConflicts;
import net.sf.cpsolver.ifs.constant.ConstantModel;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.model.WeakeningConstraint;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.DistanceMetric;

/**
 * Timetable model.
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(TimetableModel.class);
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

    private HashSet<Student> iAllStudents = new HashSet<Student>();
    
    private DistanceMetric iDistanceMetric = null;
    
    private StudentSectioning iStudentSectioning = null;

    public TimetableModel(DataProperties properties) {
        super();
        iProperties = properties;
        iDistanceMetric = new DistanceMetric(properties);
        if (properties.getPropertyBoolean("OnFlySectioning.Enabled", false))
            addModelListener(new OnFlySectioning(this));
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
                AssignmentCount.class.getName() + ";" +
                DeltaTimePreference.class.getName() + ";" +
                HardConflicts.class.getName() + ";" +
                PotentialHardConflicts.class.getName() + ";" +
                FlexibleConstraintCriterion.class.getName() + ";" +
                WeightedHardConflicts.class.getName());                
        // Interactive mode -- count time / room violations
        if (properties.getPropertyBoolean("General.InteractiveMode", false))
            criteria += ";" + TimeViolations.class.getName() + ";" + RoomViolations.class.getName();
        // Additional (custom) criteria
        criteria += ";" + properties.getProperty("General.AdditionalCriteria", "");
        for (String criterion: criteria.split("\\;")) {
            if (criterion == null || criterion.isEmpty()) continue;
            try {
                @SuppressWarnings("unchecked")
                Class<Criterion<Lecture, Placement>> clazz = (Class<Criterion<Lecture, Placement>>)Class.forName(criterion);
                addCriterion(clazz.newInstance());
            } catch (Exception e) {
                sLogger.error("Unable to use " + criterion + ": " + e.getMessage());
            }
        }
        try {
            String studentSectioningClassName = properties.getProperty("StudentSectioning.Class", DefaultStudentSectioning.class.getName());
            Class<?> studentSectioningClass = Class.forName(studentSectioningClassName);
            iStudentSectioning = (StudentSectioning)studentSectioningClass.getConstructor(TimetableModel.class).newInstance(this);
        } catch (Exception e) {
            sLogger.error("Failed to load custom student sectioning class: " + e.getMessage());
            iStudentSectioning = new DefaultStudentSectioning(this);
        }
    }

    public DistanceMetric getDistanceMetric() {
        return iDistanceMetric;
    }
    
    /**
     * Returns interface to the student sectioning functions needed during course timetabling.
     * Defaults to an instance of {@link DefaultStudentSectioning}, can be changed using the StudentSectioning.Class parameter.
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
     */
    public void switchStudents() {
        getStudentSectioning().switchStudents(this);
    }

    /**
     * String representation -- returns a list of values of objective criteria
     */
    @Override
    public String toString() {
        List<Criterion<Lecture, Placement>> criteria = new ArrayList<Criterion<Lecture,Placement>>(getCriteria());
        Collections.sort(criteria, new Comparator<Criterion<Lecture, Placement>>() {
            @Override
            public int compare(Criterion<Lecture, Placement> c1, Criterion<Lecture, Placement> c2) {
                int cmp = -Double.compare(c1.getWeight(), c2.getWeight());
                if (cmp != 0) return cmp;
                return c1.getName().compareTo(c2.getName());
            }
        });
        String ret = "";
        for (Criterion<Lecture, Placement> criterion: criteria) {
            String val = criterion.toString();
            if (!val.isEmpty())
                ret += ", " + val;
        }
        return (nrUnassignedVariables() == 0 ? "" : "V:" + nrAssignedVariables() + "/" + variables().size() + ", ") + 
                "T:" + sDoubleFormat.format(getTotalValue()) + ret;
    }

    public Map<String, String> getBounds() {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("Room preferences min", "" + getCriterion(RoomPreferences.class).getBounds()[0]);
        ret.put("Room preferences max", "" + getCriterion(RoomPreferences.class).getBounds()[1]);
        ret.put("Time preferences min", "" + getCriterion(TimePreferences.class).getBounds()[0]);
        ret.put("Time preferences max", "" + getCriterion(TimePreferences.class).getBounds()[1]);
        ret.put("Distribution preferences min", "" + getCriterion(DistributionPreferences.class).getBounds()[0]);
        ret.put("Distribution preferences max", "" + getCriterion(DistributionPreferences.class).getBounds()[1]);
        if (getProperties().getPropertyBoolean("General.UseDistanceConstraints", false)) {
            ret.put("Back-to-back instructor preferences max", "" + getCriterion(BackToBackInstructorPreferences.class).getBounds()[1]);
        }
        ret.put("Too big rooms max", "" + getCriterion(TooBigRooms.class).getBounds()[0]);
        ret.put("Useless half-hours", "" + getCriterion(UselessHalfHours.class).getBounds()[0]);
        return ret;
    }

    /** Global info */
    @Override
    public Map<String, String> getInfo() {
        Map<String, String> ret = super.getInfo();
        ret.put("Memory usage", getMem());
        
        Criterion<Lecture, Placement> rp = getCriterion(RoomPreferences.class);
        Criterion<Lecture, Placement> rv = getCriterion(RoomViolations.class);
        ret.put("Room preferences", getPerc(rp.getValue(), rp.getBounds()[0], rp.getBounds()[1]) + "% (" + Math.round(rp.getValue()) + ")"
                + (rv != null && rv.getValue() >= 0.5 ? " [hard:" + Math.round(rv.getValue()) + "]" : ""));
        
        Criterion<Lecture, Placement> tp = getCriterion(TimePreferences.class);
        Criterion<Lecture, Placement> tv = getCriterion(TimeViolations.class);
        ret.put("Time preferences", getPerc(tp.getValue(), tp.getBounds()[0], tp.getBounds()[1]) + "% (" + sDoubleFormat.format(tp.getValue()) + ")"
                + (tv != null && tv.getValue() >= 0.5 ? " [hard:" + Math.round(tv.getValue()) + "]" : ""));

        Criterion<Lecture, Placement> dp = getCriterion(DistributionPreferences.class);
        ret.put("Distribution preferences", getPerc(dp.getValue(), dp.getBounds()[0], dp.getBounds()[1]) + "% (" + sDoubleFormat.format(dp.getValue()) + ")");
        
        Criterion<Lecture, Placement> sc = getCriterion(StudentConflict.class);
        Criterion<Lecture, Placement> shc = getCriterion(StudentHardConflict.class);
        Criterion<Lecture, Placement> sdc = getCriterion(StudentDistanceConflict.class);
        Criterion<Lecture, Placement> scc = getCriterion(StudentCommittedConflict.class);
        ret.put("Student conflicts", Math.round(scc.getValue() + sc.getValue()) +
                " [committed:" + Math.round(scc.getValue()) +
                ", distance:" + Math.round(sdc.getValue()) +
                ", hard:" + Math.round(shc.getValue()) + "]");
        
        if (!getSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> ip = getCriterion(BackToBackInstructorPreferences.class);
            ret.put("Back-to-back instructor preferences", getPerc(ip.getValue(), ip.getBounds()[0], ip.getBounds()[1]) + "% (" + Math.round(ip.getValue()) + ")");
        }

        if (!getDepartmentSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> dbp = getCriterion(DepartmentBalancingPenalty.class);
            ret.put("Department balancing penalty", sDoubleFormat.format(dbp.getValue()));
        }
        
        Criterion<Lecture, Placement> sbp = getCriterion(SameSubpartBalancingPenalty.class);
        ret.put("Same subpart balancing penalty", sDoubleFormat.format(sbp.getValue()));
        
        Criterion<Lecture, Placement> tbr = getCriterion(TooBigRooms.class);
        ret.put("Too big rooms", getPercRev(tbr.getValue(), tbr.getBounds()[1], tbr.getBounds()[0]) + "% (" + Math.round(tbr.getValue()) + ")");
        
        Criterion<Lecture, Placement> uh = getCriterion(UselessHalfHours.class);
        Criterion<Lecture, Placement> bt = getCriterion(BrokenTimePatterns.class);

        ret.put("Useless half-hours", getPercRev(uh.getValue() + bt.getValue(), 0, Constants.sPreferenceLevelStronglyDiscouraged * bt.getBounds()[0]) +
                "% (" + Math.round(uh.getValue()) + " + " + Math.round(bt.getValue()) + ")");
        return ret;
    }

    @Override
    public Map<String, String> getInfo(Collection<Lecture> variables) {
        Map<String, String> ret = super.getInfo(variables);
        
        ret.put("Memory usage", getMem());
        
        Criterion<Lecture, Placement> rp = getCriterion(RoomPreferences.class);
        ret.put("Room preferences", getPerc(rp.getValue(variables), rp.getBounds(variables)[0], rp.getBounds(variables)[1]) + "% (" + Math.round(rp.getValue(variables)) + ")");
        
        Criterion<Lecture, Placement> tp = getCriterion(TimePreferences.class);
        ret.put("Time preferences", getPerc(tp.getValue(variables), tp.getBounds(variables)[0], tp.getBounds(variables)[1]) + "% (" + sDoubleFormat.format(tp.getValue(variables)) + ")"); 

        Criterion<Lecture, Placement> dp = getCriterion(DistributionPreferences.class);
        ret.put("Distribution preferences", getPerc(dp.getValue(variables), dp.getBounds(variables)[0], dp.getBounds(variables)[1]) + "% (" + sDoubleFormat.format(dp.getValue(variables)) + ")");
        
        Criterion<Lecture, Placement> sc = getCriterion(StudentConflict.class);
        Criterion<Lecture, Placement> shc = getCriterion(StudentHardConflict.class);
        Criterion<Lecture, Placement> sdc = getCriterion(StudentDistanceConflict.class);
        Criterion<Lecture, Placement> scc = getCriterion(StudentCommittedConflict.class);
        ret.put("Student conflicts", Math.round(scc.getValue(variables) + sc.getValue(variables)) +
                " [committed:" + Math.round(scc.getValue(variables)) +
                ", distance:" + Math.round(sdc.getValue(variables)) +
                ", hard:" + Math.round(shc.getValue(variables)) + "]");
        
        if (!getSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> ip = getCriterion(BackToBackInstructorPreferences.class);
            ret.put("Back-to-back instructor preferences", getPerc(ip.getValue(variables), ip.getBounds(variables)[0], ip.getBounds(variables)[1]) + "% (" + Math.round(ip.getValue(variables)) + ")");
        }

        if (!getDepartmentSpreadConstraints().isEmpty()) {
            Criterion<Lecture, Placement> dbp = getCriterion(DepartmentBalancingPenalty.class);
            ret.put("Department balancing penalty", sDoubleFormat.format(dbp.getValue(variables)));
        }
        
        Criterion<Lecture, Placement> sbp = getCriterion(SameSubpartBalancingPenalty.class);
        ret.put("Same subpart balancing penalty", sDoubleFormat.format(sbp.getValue(variables)));
        
        Criterion<Lecture, Placement> tbr = getCriterion(TooBigRooms.class);
        ret.put("Too big rooms", getPercRev(tbr.getValue(variables), tbr.getBounds(variables)[1], tbr.getBounds(variables)[0]) + "% (" + Math.round(tbr.getValue(variables)) + ")");
        
        Criterion<Lecture, Placement> uh = getCriterion(UselessHalfHours.class);
        Criterion<Lecture, Placement> bt = getCriterion(BrokenTimePatterns.class);

        ret.put("Useless half-hours", getPercRev(uh.getValue(variables) + bt.getValue(variables), 0, Constants.sPreferenceLevelStronglyDiscouraged * bt.getBounds(variables)[0]) +
                "% (" + Math.round(uh.getValue(variables)) + " + " + Math.round(bt.getValue(variables)) + ")");
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

    /** The list of all instructor constraints */
    public List<InstructorConstraint> getInstructorConstraints() {
        return iInstructorConstraints;
    }

    /** The list of all group constraints */
    public List<GroupConstraint> getGroupConstraints() {
        return iGroupConstraints;
    }

    /** The list of all jenrl constraints */
    public List<JenrlConstraint> getJenrlConstraints() {
        return iJenrlConstraints;
    }

    /** The list of all room constraints */
    public List<RoomConstraint> getRoomConstraints() {
        return iRoomConstraints;
    }

    /** The list of all departmental spread constraints */
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
    public double getTotalValue() {
        double ret = 0;
        for (Criterion<Lecture, Placement> criterion: getCriteria())
            ret += criterion.getWeightedValue();
        return ret;
    }

    @Override
    public double getTotalValue(Collection<Lecture> variables) {
        double ret = 0;
        for (Criterion<Lecture, Placement> criterion: getCriteria())
            ret += criterion.getWeightedValue(variables);
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
     */
    public Set<Placement> conflictValuesSkipWeakeningConstraints(Placement value) {
        Set<Placement> conflictValues = new HashSet<Placement>();
        for (Constraint<Lecture, Placement> constraint : value.variable().hardConstraints()) {
            if (constraint instanceof WeakeningConstraint) continue;
            constraint.computeConflicts(value, conflictValues);
        }
        for (GlobalConstraint<Lecture, Placement> constraint : globalConstraints()) {
            if (constraint instanceof WeakeningConstraint) continue;
            constraint.computeConflicts(value, conflictValues);
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
            if (defaultDatePattern == null) return null;
            
            // Create default date pattern
            BitSet fullTerm = new BitSet(defaultDatePattern.length());
            for (int i = 0; i < defaultDatePattern.length(); i++) {
                if (defaultDatePattern.charAt(i) == 49) {
                    fullTerm.set(i);
                }
            }
            
            // Cut date pattern into weeks (every week contains 7 positive bits)
            iWeeks = new ArrayList<BitSet>();
            int cnt = 0;
            for (int i = 0; i < fullTerm.length(); i++) {
                if (fullTerm.get(i)) {
                    int w = (cnt++) / 7;
                    if (iWeeks.size() == w) {
                        iWeeks.add(new BitSet(fullTerm.length()));
                    }
                    iWeeks.get(w).set(i);
                }
            }
        }
        return iWeeks;            
    }
}
