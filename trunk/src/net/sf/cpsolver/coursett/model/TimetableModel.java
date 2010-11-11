package net.sf.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Collection;
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
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.heuristics.TimetableComparator;
import net.sf.cpsolver.coursett.heuristics.UniversalPerturbationsCounter;
import net.sf.cpsolver.ifs.constant.ConstantModel;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.perturbations.PerturbationsCounter;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.Counter;
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */

public class TimetableModel extends ConstantModel<Lecture, Placement> {
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));
    private long iGlobalRoomPreference = 0;
    private double iGlobalTimePreference = 0;
    private long iMinRoomPreference = 0;
    private long iMaxRoomPreference = 0;
    private long iBestInstructorDistancePreference = 0;
    private double iMinTimePreference = 0;
    private double iMaxTimePreference = 0;
    private int iBestDepartmentSpreadPenalty = 0;
    private int iBestSpreadPenalty = 0;
    private int iBestCommitedStudentConflicts = 0;
    private int iMaxGroupConstraintPreference = 0;
    private int iMinGroupConstraintPreference = 0;
    private Counter iGlobalGroupConstraintPreference = new Counter();
    private Counter iViolatedStudentConflicts = new Counter();
    private Counter iViolatedHardStudentConflicts = new Counter();
    private Counter iViolatedDistanceStudentConflicts = new Counter();
    private Counter iViolatedCommittedStudentConflicts = new Counter();
    private Counter iCommittedStudentConflictsCounter = new Counter();
    private List<InstructorConstraint> iInstructorConstraints = new ArrayList<InstructorConstraint>();
    private List<JenrlConstraint> iJenrlConstraints = new ArrayList<JenrlConstraint>();
    private List<RoomConstraint> iRoomConstraints = new ArrayList<RoomConstraint>();
    private List<DepartmentSpreadConstraint> iDepartmentSpreadConstraints = new ArrayList<DepartmentSpreadConstraint>();
    private List<SpreadConstraint> iSpreadConstraints = new ArrayList<SpreadConstraint>();
    private List<GroupConstraint> iGroupConstraints = new ArrayList<GroupConstraint>();
    private List<ClassLimitConstraint> iClassLimitConstraints = new ArrayList<ClassLimitConstraint>();
    private DataProperties iProperties = null;
    private TimetableComparator iCmp = null;
    private UniversalPerturbationsCounter iPertCnt = null;
    private int iYear = -1;

    private HashSet<Student> iAllStudents = new HashSet<Student>();
    
    private DistanceMetric iDistanceMetric = null;


    public TimetableModel(DataProperties properties) {
        super();
        iProperties = properties;
        iDistanceMetric = new DistanceMetric(properties);
        iCmp = new TimetableComparator(properties);
        iPertCnt = new UniversalPerturbationsCounter(properties);
        if (properties.getPropertyBoolean("OnFlySectioning.Enabled", false))
            addModelListener(new OnFlySectioning(this));
    }

    public DistanceMetric getDistanceMetric() {
        return iDistanceMetric;
    }

    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        iCmp = new TimetableComparator(solver.getProperties());
        iPertCnt = new UniversalPerturbationsCounter(solver.getProperties());
        return super.init(solver);
    }

    @Override
    public void addVariable(Lecture lecture) {
        super.addVariable(lecture);
        if (lecture.isCommitted())
            return;
        double[] minMaxTimePref = lecture.getMinMaxTimePreference();
        iMinTimePreference += minMaxTimePref[0];
        iMaxTimePreference += minMaxTimePref[1];
        int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
        iMinRoomPreference += minMaxRoomPref[0];
        iMaxRoomPreference += minMaxRoomPref[1];
    }

    @Override
    public void removeVariable(Lecture lecture) {
        super.removeVariable(lecture);
        if (lecture.isCommitted())
            return;
        double[] minMaxTimePref = lecture.getMinMaxTimePreference();
        iMinTimePreference -= minMaxTimePref[0];
        iMaxTimePreference -= minMaxTimePref[1];
        int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
        iMinRoomPreference -= minMaxRoomPref[0];
        iMaxRoomPreference -= minMaxRoomPref[1];
    }

    public DataProperties getProperties() {
        return iProperties;
    }

    /** Overall room preference */
    public long getGlobalRoomPreference() {
        return iGlobalRoomPreference;
    }

    /** Overall time preference */
    public double getGlobalTimePreference() {
        return iGlobalTimePreference;
    }

    /** Number of student conflicts */
    public long getViolatedStudentConflicts() {
        return iViolatedStudentConflicts.get();
    }

    /** Number of student conflicts */
    public long countViolatedStudentConflicts() {
        long studentConflicts = 0;
        for (JenrlConstraint jenrl : iJenrlConstraints) {
            if (jenrl.isInConflict())
                studentConflicts += jenrl.getJenrl();
        }
        return studentConflicts;
    }

    /** Number of student conflicts */
    public Counter getViolatedStudentConflictsCounter() {
        return iViolatedStudentConflicts;
    }

    public Counter getViolatedHardStudentConflictsCounter() {
        return iViolatedHardStudentConflicts;
    }

    public Counter getViolatedCommitttedStudentConflictsCounter() {
        return iViolatedCommittedStudentConflicts;
    }

    public Counter getViolatedDistanceStudentConflictsCounter() {
        return iViolatedDistanceStudentConflicts;
    }

    /** Overall group constraint preference */
    public long getGlobalGroupConstraintPreference() {
        return iGlobalGroupConstraintPreference.get();
    }

    /** Overall group constraint preference */
    public Counter getGlobalGroupConstraintPreferenceCounter() {
        return iGlobalGroupConstraintPreference;
    }

    /** Overall instructor distance (back-to-back) preference */
    public long getInstructorDistancePreference() {
        long pref = 0;
        for (InstructorConstraint constraint : iInstructorConstraints) {
            pref += constraint.getPreference();
        }
        return pref;
    }

    /** The worst instructor distance (back-to-back) preference */
    public long getInstructorWorstDistancePreference() {
        long pref = 0;
        for (InstructorConstraint constraint : iInstructorConstraints) {
            pref += constraint.getWorstPreference();
        }
        return pref;
    }

    /** Overall number of useless time slots */
    public long getUselessSlots() {
        long uselessSlots = 0;
        for (RoomConstraint constraint : iRoomConstraints) {
            uselessSlots += (constraint).countUselessSlots();
        }
        return uselessSlots;
    }

    /** Overall number of useless time slots */
    public long getUselessHalfHours() {
        long uselessSlots = 0;
        for (RoomConstraint constraint : iRoomConstraints) {
            uselessSlots += (constraint).countUselessSlotsHalfHours();
        }
        return uselessSlots;
    }

    /** Overall number of useless time slots */
    public long getBrokenTimePatterns() {
        long uselessSlots = 0;
        for (RoomConstraint constraint : iRoomConstraints) {
            uselessSlots += (constraint).countUselessSlotsBrokenTimePatterns();
        }
        return uselessSlots;
    }

    /**
     * Overall number of student conflicts caused by distancies (back-to-back
     * classes are too far)
     */
    public long getStudentDistanceConflicts() {
        /*
         * if
         * (iViolatedDistanceStudentConflicts.get()!=countStudentDistanceConflicts
         * ()) {System.err.println(
         * "TimetableModel.getStudentDistanceConflicts() is not working properly"
         * ); }
         */
        return iViolatedDistanceStudentConflicts.get();
    }

    public long countStudentDistanceConflicts() {
        long nrConflicts = 0;
        for (JenrlConstraint jenrl : iJenrlConstraints) {
            if (jenrl.isInConflict()
                    && !(jenrl.first().getAssignment()).getTimeLocation().hasIntersection(
                            (jenrl.second().getAssignment()).getTimeLocation()))
                nrConflicts += jenrl.getJenrl();
        }
        return nrConflicts;
    }

    /**
     * Overall hard student conflicts (student conflict between single section
     * classes)
     */
    public long getHardStudentConflicts() {
        /*
         * if (iViolatedHardStudentConflicts.get()!=countHardStudentConflicts())
         * {System.err.println(
         * "TimetableModel.getHardStudentConflicts() is not working properly");
         * }
         */
        return iViolatedHardStudentConflicts.get();
    }

    public long countHardStudentConflicts() {
        long hardStudentConflicts = 0;
        for (JenrlConstraint jenrl : iJenrlConstraints) {
            if (jenrl.isInConflict()) {
                Lecture l1 = jenrl.first();
                Lecture l2 = jenrl.second();
                if (l1.areStudentConflictsHard(l2))
                    hardStudentConflicts += jenrl.getJenrl();
            }
        }
        return hardStudentConflicts;
    }

    public Counter getCommittedStudentConflictsCounter() {
        return iCommittedStudentConflictsCounter;
    }

    public int getCommitedStudentConflicts() {
        /*
         * if
         * (iCommittedStudentConflictsCounter.get()!=countCommitedStudentConflicts
         * ()) {System.err.println(
         * "TimetableModel.getCommitedStudentConflicts() is not working properly"
         * ); }
         */
        return (int)(iViolatedCommittedStudentConflicts.get() + iCommittedStudentConflictsCounter.get());
    }

    public int countCommitedStudentConflicts(boolean includeJenrl) {
        int commitedStudentConflicts = 0;
        for (Lecture lecture : assignedVariables()) {
            commitedStudentConflicts += lecture.getCommitedConflicts(lecture.getAssignment());
        }
        if (hasConstantVariables())
            for (Lecture lecture : constantVariables()) {
                if (lecture.getAssignment() != null)
                    commitedStudentConflicts += lecture.getCommitedConflicts(lecture.getAssignment());
            }
        if (includeJenrl)
            for (JenrlConstraint jenrl : iJenrlConstraints) {
                if (jenrl.isInConflict() && jenrl.areStudentConflictsCommitted())
                    commitedStudentConflicts += jenrl.getJenrl();
            }
        return commitedStudentConflicts;
    }

    /** When a value is assigned to a variable -- update gloval preferences */
    @Override
    public void afterAssigned(long iteration, Placement placement) {
        super.afterAssigned(iteration, placement);
        if (placement == null)
            return;
        iGlobalRoomPreference += placement.sumRoomPreference();
        iGlobalTimePreference += placement.getTimeLocation().getNormalizedPreference();
    }

    /** When a value is unassigned from a variable -- update gloval preferences */
    @Override
    public void afterUnassigned(long iteration, Placement placement) {
        super.afterUnassigned(iteration, placement);
        if (placement == null)
            return;
        iGlobalRoomPreference -= placement.sumRoomPreference();
        iGlobalTimePreference -= placement.getTimeLocation().getNormalizedPreference();
    }

    /**
     * Student final sectioning (switching students between sections of the same
     * class in order to minimize overall number of student conflicts)
     */
    public void switchStudents() {
        FinalSectioning sect = new FinalSectioning(this);
        sect.run();
    }

    @Override
    public String toString() {
        return "TimetableModel{" + "\n  super=" + super.toString()
                + "\n  studentConflicts="
                + iViolatedStudentConflicts.get()
                +
                // "\n  studentConflicts(violated room distance)="+iViolatedRoomDistanceStudentConflicts.get()+
                // "\n  studentPreferences="+iRoomDistanceStudentPreference.get()+
                "\n  roomPreferences=" + iGlobalRoomPreference + "\n  timePreferences=" + iGlobalTimePreference
                + "\n  groupConstraintPreferences=" + iGlobalGroupConstraintPreference.get() + "\n}";
    }

    /**
     * Overall number of too big rooms (rooms with more than 3/2 seats than
     * needed)
     */
    public int countTooBigRooms() {
        int tooBigRooms = 0;
        for (Lecture lecture : assignedVariables()) {
            if (lecture.getAssignment() == null)
                continue;
            Placement placement = lecture.getAssignment();
            tooBigRooms += placement.getTooBigRoomPreference();
        }
        return tooBigRooms;
    }

    /** Overall departmental spread penalty */
    public int getDepartmentSpreadPenalty() {
        if (iDepartmentSpreadConstraints.isEmpty())
            return 0;
        int penalty = 0;
        for (DepartmentSpreadConstraint c : iDepartmentSpreadConstraints) {
            penalty += (c).getPenalty();
        }
        return penalty;
    }

    /** Overall spread penalty */
    public int getSpreadPenalty() {
        if (iSpreadConstraints.isEmpty())
            return 0;
        int penalty = 0;
        for (SpreadConstraint c : iSpreadConstraints) {
            penalty += (c).getPenalty();
        }
        return penalty;
    }

    public Map<String, String> getBounds() {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("Room preferences min", "" + iMinRoomPreference);
        ret.put("Room preferences max", "" + iMaxRoomPreference);
        ret.put("Time preferences min", "" + iMinTimePreference);
        ret.put("Time preferences max", "" + iMaxTimePreference);
        ret.put("Distribution preferences min", "" + iMinGroupConstraintPreference);
        ret.put("Distribution preferences max", "" + iMaxGroupConstraintPreference);
        if (getProperties().getPropertyBoolean("General.UseDistanceConstraints", false)) {
            ret.put("Back-to-back instructor preferences max", "" + getInstructorWorstDistancePreference());
        }
        ret.put("Too big rooms max", "" + (Constants.sPreferenceLevelStronglyDiscouraged * variables().size()));
        ret.put("Useless half-hours", ""
                + (Constants.sPreferenceLevelStronglyDiscouraged * getRoomConstraints().size()
                        * Constants.SLOTS_PER_DAY_NO_EVENINGS * Constants.NR_DAYS_WEEK));
        return ret;
    }

    /** Global info */
    @Override
    public Map<String, String> getInfo() {
        Map<String, String> ret = super.getInfo();
        ret.put("Memory usage", getMem());
        ret.put("Room preferences", getPerc(iGlobalRoomPreference, iMinRoomPreference, iMaxRoomPreference) + "% ("
                + iGlobalRoomPreference + ")");
        ret.put("Time preferences", getPerc(iGlobalTimePreference, iMinTimePreference, iMaxTimePreference) + "% ("
                + sDoubleFormat.format(iGlobalTimePreference) + ")");
        ret.put("Distribution preferences", getPerc(iGlobalGroupConstraintPreference.get(),
                iMinGroupConstraintPreference, iMaxGroupConstraintPreference)
                + "% (" + iGlobalGroupConstraintPreference.get() + ")");
        int commitedStudentConflicts = (int)iCommittedStudentConflictsCounter.get();
        ret.put("Student conflicts", (commitedStudentConflicts + getViolatedStudentConflicts()) + " [committed:" +
                (iViolatedCommittedStudentConflicts.get() == 0 ? "" + iCommittedStudentConflictsCounter.get() : 
                 iViolatedCommittedStudentConflicts.get() + (iCommittedStudentConflictsCounter.get() == 0 ? "" : " + " + iCommittedStudentConflictsCounter.get())) +
                ", hard:" + getHardStudentConflicts() + "]");
        if (getProperties().getPropertyBoolean("General.UseDistanceConstraints", false)) {
            ret.put("Student conflicts", (commitedStudentConflicts + getViolatedStudentConflicts()) + " [committed:" +
                    (iViolatedCommittedStudentConflicts.get() == 0 ? "" + iCommittedStudentConflictsCounter.get() : 
                     iViolatedCommittedStudentConflicts.get() + (iCommittedStudentConflictsCounter.get() == 0 ? "" : " + " + iCommittedStudentConflictsCounter.get())) +
                    ", distance:" + getStudentDistanceConflicts() + ", hard:" + getHardStudentConflicts() + "]");
            ret.put("Back-to-back instructor preferences", getPerc(getInstructorDistancePreference(), 0,
                    getInstructorWorstDistancePreference())
                    + "% (" + getInstructorDistancePreference() + ")");
        }
        if (getProperties().getPropertyBoolean("General.DeptBalancing", false)) {
            ret.put("Department balancing penalty", sDoubleFormat.format((getDepartmentSpreadPenalty()) / 12.0));
        }
        ret.put("Same subpart balancing penalty", sDoubleFormat.format((getSpreadPenalty()) / 12.0));
        ret.put("Too big rooms", getPercRev(countTooBigRooms(), 0, Constants.sPreferenceLevelStronglyDiscouraged
                * variables().size())
                + "% (" + countTooBigRooms() + ")");
        ret.put("Useless half-hours", getPercRev(getUselessSlots(), 0, Constants.sPreferenceLevelStronglyDiscouraged
                * getRoomConstraints().size() * Constants.SLOTS_PER_DAY_NO_EVENINGS * Constants.NR_DAYS_WEEK)
                + "% (" + getUselessHalfHours() + " + " + getBrokenTimePatterns() + ")");
        return ret;
    }

    @Override
    public Map<String, String> getInfo(Collection<Lecture> variables) {
        Map<String, String> ret = super.getInfo(variables);
        ret.put("Memory usage", getMem());

        int roomPref = 0, minRoomPref = 0, maxRoomPref = 0;
        double timePref = 0, minTimePref = 0, maxTimePref = 0;
        double grPref = 0, minGrPref = 0, maxGrPref = 0;
        long allSC = 0, hardSC = 0, distSC = 0;
        int instPref = 0, worstInstrPref = 0;
        int spreadPen = 0, deptSpreadPen = 0;
        int tooBigRooms = 0;
        int rcs = 0, uselessSlots = 0, uselessSlotsHH = 0, uselessSlotsBTP = 0;

        HashSet<Constraint<Lecture, Placement>> used = new HashSet<Constraint<Lecture, Placement>>();

        for (Lecture lecture : variables) {
            if (lecture.isCommitted())
                continue;
            Placement placement = lecture.getAssignment();

            int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
            minRoomPref += minMaxRoomPref[0];
            maxRoomPref += minMaxRoomPref[1];

            double[] minMaxTimePref = lecture.getMinMaxTimePreference();
            minTimePref += minMaxTimePref[0];
            maxTimePref += minMaxTimePref[1];

            if (placement != null) {
                roomPref += placement.getRoomPreference();
                timePref += placement.getTimeLocation().getNormalizedPreference();
                tooBigRooms += placement.getTooBigRoomPreference();
            }

            for (Constraint<Lecture, Placement> c : lecture.constraints()) {
                if (!used.add(c))
                    continue;

                if (c instanceof InstructorConstraint) {
                    InstructorConstraint ic = (InstructorConstraint) c;
                    instPref += ic.getPreference();
                    worstInstrPref += ic.getWorstPreference();
                }

                if (c instanceof DepartmentSpreadConstraint) {
                    DepartmentSpreadConstraint dsc = (DepartmentSpreadConstraint) c;
                    deptSpreadPen += dsc.getPenalty();
                } else if (c instanceof SpreadConstraint) {
                    SpreadConstraint sc = (SpreadConstraint) c;
                    spreadPen += sc.getPenalty();
                }

                if (c instanceof GroupConstraint) {
                    GroupConstraint gc = (GroupConstraint) c;
                    if (gc.isHard())
                        continue;
                    minGrPref += Math.min(gc.getPreference(), 0);
                    maxGrPref += Math.max(gc.getPreference(), 0);
                    grPref += gc.getCurrentPreference();
                }

                if (c instanceof JenrlConstraint) {
                    JenrlConstraint jc = (JenrlConstraint) c;
                    if (!jc.isInConflict() || !jc.isOfTheSameProblem())
                        continue;
                    Lecture l1 = jc.first();
                    Lecture l2 = jc.second();
                    allSC += jc.getJenrl();
                    if (l1.areStudentConflictsHard(l2))
                        hardSC += jc.getJenrl();
                    Placement p1 = l1.getAssignment();
                    Placement p2 = l2.getAssignment();
                    if (!p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
                        distSC += jc.getJenrl();
                }

                if (c instanceof RoomConstraint) {
                    RoomConstraint rc = (RoomConstraint) c;
                    uselessSlots += rc.countUselessSlots();
                    uselessSlotsHH += rc.countUselessSlotsHalfHours();
                    uselessSlotsBTP += rc.countUselessSlotsBrokenTimePatterns();
                    rcs++;
                }
            }
        }

        ret.put("Room preferences", getPerc(roomPref, minRoomPref, maxRoomPref) + "% (" + roomPref + ")");
        ret.put("Time preferences", getPerc(timePref, minTimePref, maxTimePref) + "% ("
                + sDoubleFormat.format(timePref) + ")");
        ret.put("Distribution preferences", getPerc(grPref, minGrPref, maxGrPref) + "% (" + grPref + ")");
        ret.put("Student conflicts", allSC + " [committed:" + 0 + ", hard:" + hardSC + "]");
        if (getProperties().getPropertyBoolean("General.UseDistanceConstraints", false)) {
            ret
                    .put("Student conflicts", allSC + " [committed:" + 0 + ", distance:" + distSC + ", hard:" + hardSC
                            + "]");
            ret.put("Back-to-back instructor preferences", getPerc(instPref, 0, worstInstrPref) + "% (" + instPref
                    + ")");
        }
        if (getProperties().getPropertyBoolean("General.DeptBalancing", false)) {
            ret.put("Department balancing penalty", sDoubleFormat.format((deptSpreadPen) / 12.0));
        }
        ret.put("Same subpart balancing penalty", sDoubleFormat.format((spreadPen) / 12.0));
        ret.put("Too big rooms", getPercRev(tooBigRooms, 0, Constants.sPreferenceLevelStronglyDiscouraged
                * variables.size())
                + "% (" + tooBigRooms + ")");
        ret.put("Useless half-hours", getPercRev(uselessSlots, 0, Constants.sPreferenceLevelStronglyDiscouraged * rcs
                * Constants.SLOTS_PER_DAY_NO_EVENINGS * Constants.NR_DAYS_WEEK)
                + "% (" + uselessSlotsHH + " + " + uselessSlotsBTP + ")");

        return ret;
    }

    private int iBestTooBigRooms;
    private long iBestUselessSlots;
    private double iBestGlobalTimePreference;
    private long iBestGlobalRoomPreference;
    private long iBestGlobalGroupConstraintPreference;
    private long iBestViolatedStudentConflicts;
    private long iBestHardStudentConflicts;

    /** Overall number of too big rooms of the best solution ever found */
    public int bestTooBigRooms() {
        return iBestTooBigRooms;
    }

    /** Overall number of useless slots of the best solution ever found */
    public long bestUselessSlots() {
        return iBestUselessSlots;
    }

    /** Overall time preference of the best solution ever found */
    public double bestGlobalTimePreference() {
        return iBestGlobalTimePreference;
    }

    /** Overall room preference of the best solution ever found */
    public long bestGlobalRoomPreference() {
        return iBestGlobalRoomPreference;
    }

    /** Overall group constraint preference of the best solution ever found */
    public long bestGlobalGroupConstraintPreference() {
        return iBestGlobalGroupConstraintPreference;
    }

    /** Overall number of student conflicts of the best solution ever found */
    public long bestViolatedStudentConflicts() {
        return iBestViolatedStudentConflicts;
    }

    /**
     * Overall number of student conflicts between single section classes of the
     * best solution ever found
     */
    public long bestHardStudentConflicts() {
        return iBestHardStudentConflicts;
    }

    /** Overall instructor distance preference of the best solution ever found */
    public long bestInstructorDistancePreference() {
        return iBestInstructorDistancePreference;
    }

    /** Overall departmental spread penalty of the best solution ever found */
    public int bestDepartmentSpreadPenalty() {
        return iBestDepartmentSpreadPenalty;
    }

    public int bestSpreadPenalty() {
        return iBestSpreadPenalty;
    }

    public int bestCommitedStudentConflicts() {
        return iBestCommitedStudentConflicts;
    }

    @Override
    public void saveBest() {
        super.saveBest();
        iBestTooBigRooms = countTooBigRooms();
        iBestUselessSlots = getUselessSlots();
        iBestGlobalTimePreference = getGlobalTimePreference();
        iBestGlobalRoomPreference = getGlobalRoomPreference();
        iBestGlobalGroupConstraintPreference = getGlobalGroupConstraintPreference();
        iBestViolatedStudentConflicts = getViolatedStudentConflicts();
        iBestHardStudentConflicts = getHardStudentConflicts();
        iBestInstructorDistancePreference = getInstructorDistancePreference();
        iBestDepartmentSpreadPenalty = getDepartmentSpreadPenalty();
        iBestSpreadPenalty = getSpreadPenalty();
        iBestCommitedStudentConflicts = getCommitedStudentConflicts();
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
            if (!constraint.isHard()) {
                GroupConstraint gc = (GroupConstraint) constraint;
                iMinGroupConstraintPreference += Math.min(gc.getPreference(), 0);
                iMaxGroupConstraintPreference += Math.max(gc.getPreference(), 0);
            }
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

    /** Max capacity for too big rooms (3/2 of the number of students) */
    @Override
    public double getTotalValue() {
        return iCmp.currentValue(this, iPertCnt);
    }

    @Override
    public double getTotalValue(Collection<Lecture> variables) {
        return iCmp.currentValue(this, iPertCnt, variables);
    }

    public int getYear() {
        return iYear;
    }

    public void setYear(int year) {
        iYear = year;
    }

    public TimetableComparator getTimetableComparator() {
        return iCmp;
    }

    public PerturbationsCounter<Lecture, Placement> getPerturbationsCounter() {
        return iPertCnt;
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
}
