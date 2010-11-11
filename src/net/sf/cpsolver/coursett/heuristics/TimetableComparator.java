package net.sf.cpsolver.coursett.heuristics;

import java.util.Collection;
import java.util.HashSet;

import net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.perturbations.PerturbationsCounter;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionComparator;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Timetable (solution) comparator. <br>
 * <br>
 * The quality of a solution is expressed as a weighted sum combining soft time
 * and classroom preferences, satisfied soft group constrains and the total
 * number of student conflicts. This allows us to express the importance of
 * different types of soft constraints. <br>
 * <br>
 * The solution comparator prefers a more complete solution (with a smaller
 * number of unassigned variables) and a solution with a smaller number of
 * perturbations among solutions with the same number of unassigned variables.
 * If both solutions have the same number of unassigned variables and
 * perturbations, the solution of better quality is selected. <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Comparator.HardStudentConflictWeight</td>
 * <td>{@link Double}</td>
 * <td>Weight of hard student conflict (conflict between single-section classes)
 * </td>
 * </tr>
 * <tr>
 * <td>Comparator.StudentConflictWeight</td>
 * <td>{@link Double}</td>
 * <td>Weight of student conflict</td>
 * </tr>
 * <tr>
 * <td>Comparator.TimePreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Time preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.ContrPreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Group constraint preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.RoomPreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Room preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.UselessSlotWeight</td>
 * <td>{@link Double}</td>
 * <td>Useless slots weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.TooBigRoomWeight</td>
 * <td>{@link Double}</td>
 * <td>Too big room weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.DistanceInstructorPreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Distance (of the rooms of the back-to-back classes) based instructor
 * preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.PerturbationPenaltyWeight</td>
 * <td>{@link Double}</td>
 * <td>Perturbation penalty (see {@link UniversalPerturbationsCounter})</td>
 * </tr>
 * <tr>
 * <td>Comparator.DeptSpreadPenaltyWeight</td>
 * <td>{@link Double}</td>
 * <td>Department balancing penalty (see
 * {@link net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint})</td>
 * </tr>
 * </table>
 * 
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

public class TimetableComparator implements SolutionComparator<Lecture, Placement> {
    protected static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(TimetableComparator.class);

    private double iEmptySingleSlotWeight;
    public static final String USELESS_SLOT_WEIGHT = "Comparator.UselessSlotWeight";
    private double iTimePreferencesWeight;
    public static final String TIME_PREFERENCE_WEIGHT = "Comparator.TimePreferenceWeight";
    private double iStudentConflictWeight;
    public static final String STUDENT_CONFLICT_WEIGHT = "Comparator.StudentConflictWeight";
    private double iRoomPreferencesWeight;
    public static final String ROOM_PREFERENCE_WEIGHT = "Comparator.RoomPreferenceWeight";
    private double iConstrPreferencesWeight;
    public static final String CONSTR_PREFERENCE_WEIGHT = "Comparator.ContrPreferenceWeight";
    private double iHardStudentConflictWeight;
    public static final String HARD_STUDENT_CONFLICT_WEIGHT = "Comparator.HardStudentConflictWeight";
    private double iTooBigRoomWeight;
    public static final String TOO_BIG_ROOM_WEIGHT = "Comparator.TooBigRoomWeight";
    private boolean iCompareMpp;
    public static final String DISTANCE_INSTRUCTOR_PREFERENCE_WEIGHT = "Comparator.DistanceInstructorPreferenceWeight";
    private double iDistanceInstructorPreferenceWeight;
    public static final String PERTURBATION_PENALTY_WEIGHT = "Comparator.PerturbationPenaltyWeight";
    private double iPerturbationPenaltyWeight;
    public static final String DEPT_SPREAD_PENALTY_WEIGHT = "Comparator.DeptSpreadPenaltyWeight";
    private double iDeptSpreadPenaltyWeight;
    public static final String SPREAD_PENALTY_WEIGHT = "Comparator.SpreadPenaltyWeight";
    private double iSpreadPenaltyWeight;
    public static final String COMMITED_STUDENT_CONFLICT_WEIGHT = "Comparator.CommitedStudentConflictWeight";
    private double iCommitedStudentConflictWeight;

    public TimetableComparator(DataProperties properties) {
        iEmptySingleSlotWeight = properties.getPropertyDouble(USELESS_SLOT_WEIGHT, 0.0);
        iTimePreferencesWeight = properties.getPropertyDouble(TIME_PREFERENCE_WEIGHT, 1.0);
        iRoomPreferencesWeight = properties.getPropertyDouble(ROOM_PREFERENCE_WEIGHT, 0.1);
        iConstrPreferencesWeight = properties.getPropertyDouble(CONSTR_PREFERENCE_WEIGHT, 1.0);
        if (properties.getPropertyBoolean("General.SwitchStudents", true)) {
            iHardStudentConflictWeight = properties.getPropertyDouble(HARD_STUDENT_CONFLICT_WEIGHT, 1.0);
            iStudentConflictWeight = properties.getPropertyDouble(STUDENT_CONFLICT_WEIGHT, 0.2);
        } else {
            iHardStudentConflictWeight = 0.0;
            iStudentConflictWeight = properties.getPropertyDouble(STUDENT_CONFLICT_WEIGHT, 1.0);
        }
        iDistanceInstructorPreferenceWeight = properties.getPropertyDouble(DISTANCE_INSTRUCTOR_PREFERENCE_WEIGHT, 1.0);
        iTooBigRoomWeight = properties.getPropertyDouble(TOO_BIG_ROOM_WEIGHT, 0.0);
        iCompareMpp = properties.getPropertyBoolean("General.MPP", false);
        iPerturbationPenaltyWeight = properties.getPropertyDouble(PERTURBATION_PENALTY_WEIGHT, 1.0);
        iDeptSpreadPenaltyWeight = properties.getPropertyDouble(DEPT_SPREAD_PENALTY_WEIGHT, 1.0);
        iSpreadPenaltyWeight = properties.getPropertyDouble(SPREAD_PENALTY_WEIGHT, 1.0);
        iCommitedStudentConflictWeight = properties.getPropertyDouble(COMMITED_STUDENT_CONFLICT_WEIGHT, 1.0);
    }

    public boolean isBetterThanBestSolution(Solution<Lecture, Placement> currentSolution) {
        if (currentSolution.getBestInfo() == null)
            return true;
        TimetableModel tm = (TimetableModel) currentSolution.getModel();
        int unassigned = tm.unassignedVariables().size();
        if (tm.getBestUnassignedVariables() != unassigned)
            return tm.getBestUnassignedVariables() > unassigned;

        return (currentValue(currentSolution) < bestValue(currentSolution));
    }

    public double currentValue(Solution<Lecture, Placement> currentSolution) {
        return currentValue((TimetableModel) currentSolution.getModel(), currentSolution.getPerturbationsCounter());
    }

    public double currentValue(TimetableModel tm, PerturbationsCounter<Lecture, Placement> cnt) {
        int tooBigCurr = 0;
        if (iTooBigRoomWeight != 0.0)
            tooBigCurr = tm.countTooBigRooms();
        long uselessSlotsCur = 0;
        if (iEmptySingleSlotWeight != 0.0)
            uselessSlotsCur = tm.getUselessSlots();
        long hardSCCurr = 0;
        if (iHardStudentConflictWeight != 0.0)
            hardSCCurr = tm.getHardStudentConflicts();
        double pertCurr = 0.0;
        if (iCompareMpp && iPerturbationPenaltyWeight != 0.0)
            pertCurr = cnt.getPerturbationPenalty(tm);
        double deptSpread = 0.0;
        if (iDeptSpreadPenaltyWeight != 0.0)
            deptSpread = tm.getDepartmentSpreadPenalty();
        double spread = 0.0;
        if (iSpreadPenaltyWeight != 0.0)
            spread = tm.getSpreadPenalty();
        int commitedStudentConflicts = 0;
        if (iCommitedStudentConflictWeight != 0.0)
            commitedStudentConflicts = tm.getCommitedStudentConflicts();

        double prefCurr = (iEmptySingleSlotWeight * uselessSlotsCur)
                + (iTimePreferencesWeight * tm.getGlobalTimePreference())
                + (iRoomPreferencesWeight * tm.getGlobalRoomPreference())
                + (iConstrPreferencesWeight * tm.getGlobalGroupConstraintPreference())
                + (iStudentConflictWeight * tm.getViolatedStudentConflicts())
                + (iHardStudentConflictWeight * hardSCCurr) + (iTooBigRoomWeight * tooBigCurr)
                + (iDistanceInstructorPreferenceWeight * tm.getInstructorDistancePreference())
                + (iPerturbationPenaltyWeight * pertCurr) + (iDeptSpreadPenaltyWeight * deptSpread)
                + (iSpreadPenaltyWeight * spread) + (iCommitedStudentConflictWeight * commitedStudentConflicts);
        return prefCurr;
    }

    public double currentValue(TimetableModel tm, PerturbationsCounter<Lecture, Placement> cnt, Collection<Lecture> variables) {
        int unassigned = 0;

        int roomPref = 0;
        double timePref = 0;
        double grPref = 0;
        long allSC = 0, comSC = 0, hardSC = 0;
        int instPref = 0;
        int spreadPen = 0, deptSpreadPen = 0;
        int tooBigRooms = 0;
        int uselessSlots = 0;
        double pertCurr = 0;

        if (iCompareMpp && iPerturbationPenaltyWeight != 0.0)
            pertCurr = cnt.getPerturbationPenalty(tm, variables);

        HashSet<Constraint<Lecture, Placement>> used = new HashSet<Constraint<Lecture, Placement>>();

        for (Lecture lecture : variables) {
            Placement placement = lecture.getAssignment();

            if (placement != null) {
                roomPref += placement.getRoomPreference();
                timePref += placement.getTimeLocation().getNormalizedPreference();
                comSC += lecture.getCommitedConflicts(placement);
                tooBigRooms += placement.getTooBigRoomPreference();
            } else {
                unassigned++;
            }

            for (Constraint<Lecture, Placement> c : lecture.constraints()) {
                if (!used.add(c))
                    continue;

                if (c instanceof InstructorConstraint) {
                    InstructorConstraint ic = (InstructorConstraint) c;
                    instPref += ic.getPreference();
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
                    grPref += gc.getCurrentPreference();
                }

                if (c instanceof JenrlConstraint) {
                    JenrlConstraint jc = (JenrlConstraint) c;
                    if (!jc.isInConflict())
                        continue;
                    Lecture l1 = jc.first();
                    Lecture l2 = jc.second();
                    allSC += jc.getJenrl();
                    if (l1.areStudentConflictsHard(l2))
                        hardSC += jc.getJenrl();
                }

                if (c instanceof RoomConstraint) {
                    RoomConstraint rc = (RoomConstraint) c;
                    uselessSlots += rc.countUselessSlots();
                }
            }
        }

        double prefCurr = (iEmptySingleSlotWeight * uselessSlots) + (iTimePreferencesWeight * timePref)
                + (iRoomPreferencesWeight * roomPref) + (iConstrPreferencesWeight * grPref)
                + (iStudentConflictWeight * allSC) + (iHardStudentConflictWeight * hardSC)
                + (iTooBigRoomWeight * tooBigRooms) + (iDistanceInstructorPreferenceWeight * instPref)
                + (iPerturbationPenaltyWeight * pertCurr) + (iDeptSpreadPenaltyWeight * deptSpreadPen)
                + (iSpreadPenaltyWeight * spreadPen) + (iCommitedStudentConflictWeight * comSC);
        return prefCurr;
    }

    public double bestValue(Solution<Lecture, Placement> currentSolution) {
        TimetableModel tm = (TimetableModel) currentSolution.getModel();

        int tooBigBest = 0;
        if (iTooBigRoomWeight != 0.0)
            tooBigBest = tm.bestTooBigRooms();
        long uselessSlotsBest = 0;
        if (iEmptySingleSlotWeight != 0.0)
            uselessSlotsBest = tm.bestUselessSlots();
        long hardSCBest = 0;
        if (iHardStudentConflictWeight != 0.0)
            hardSCBest = tm.bestHardStudentConflicts();
        double pertBest = 0.0;
        if (iCompareMpp && iPerturbationPenaltyWeight != 0.0)
            pertBest = currentSolution.getBestPerturbationsPenalty();
        double deptSpreadBest = 0.0;
        if (iDeptSpreadPenaltyWeight != 0.0)
            deptSpreadBest = tm.bestDepartmentSpreadPenalty();
        double spread = 0.0;
        if (iSpreadPenaltyWeight != 0.0)
            spread = tm.bestSpreadPenalty();
        int commitedStudentConflicts = 0;
        if (iCommitedStudentConflictWeight != 0.0)
            commitedStudentConflicts = tm.bestCommitedStudentConflicts();

        double prefBest = (iEmptySingleSlotWeight * uselessSlotsBest)
                + (iTimePreferencesWeight * tm.bestGlobalTimePreference())
                + (iRoomPreferencesWeight * tm.bestGlobalRoomPreference())
                + (iConstrPreferencesWeight * tm.bestGlobalGroupConstraintPreference())
                + (iStudentConflictWeight * tm.bestViolatedStudentConflicts())
                + (iHardStudentConflictWeight * hardSCBest) + (iTooBigRoomWeight * tooBigBest)
                + (iDistanceInstructorPreferenceWeight * tm.bestInstructorDistancePreference())
                + (iPerturbationPenaltyWeight * pertBest) + (iDeptSpreadPenaltyWeight * deptSpreadBest)
                + (iSpreadPenaltyWeight * spread) + (iCommitedStudentConflictWeight * commitedStudentConflicts);
        return prefBest;
    }

    public double value(Placement placement, PerturbationsCounter<Lecture, Placement> cnt) {
        Lecture lecture = placement.variable();

        int constrPreference = 0;
        if (iConstrPreferencesWeight != 0.0) {
            for (GroupConstraint gc : lecture.groupConstraints()) {
                constrPreference += gc.getCurrentPreference(placement);
            }
        }

        int distInstrPref = 0;
        if (iDistanceInstructorPreferenceWeight != 0.0) {
            for (Constraint<Lecture, Placement> constraint : lecture.constraints()) {
                if (constraint instanceof InstructorConstraint) {
                    distInstrPref += ((InstructorConstraint) constraint).getPreference(placement);
                }
            }
        }

        return (iEmptySingleSlotWeight == 0.0 ? 0.0 : iEmptySingleSlotWeight * placement.nrUselessHalfHours())
                + (iTimePreferencesWeight == 0.0 ? 0.0 : iTimePreferencesWeight
                        * placement.getTimeLocation().getNormalizedPreference())
                + (iRoomPreferencesWeight == 0.0 ? 0.0 : iRoomPreferencesWeight * placement.sumRoomPreference())
                + (iConstrPreferencesWeight * constrPreference)
                + (iStudentConflictWeight == 0.0 ? 0.0 : iStudentConflictWeight
                        * lecture.countStudentConflicts(placement))
                + (iHardStudentConflictWeight == 0.0 ? 0.0 : iHardStudentConflictWeight
                        * lecture.countHardStudentConflicts(placement))
                + (iTooBigRoomWeight == 0.0 ? 0.0 : iTooBigRoomWeight * placement.getTooBigRoomPreference())
                + (iDistanceInstructorPreferenceWeight * distInstrPref)
                + (iPerturbationPenaltyWeight == 0.0 || cnt == null ? 0.0 : iPerturbationPenaltyWeight
                        * cnt.getPerturbationPenalty(lecture.getModel(), placement, new HashSet<Placement>()))
                + (iDeptSpreadPenaltyWeight == 0.0 || lecture.getDeptSpreadConstraint() == null ? 0.0
                        : iDeptSpreadPenaltyWeight * lecture.getDeptSpreadConstraint().getPenalty(placement))
                + (iSpreadPenaltyWeight == 0.0 ? 0.0 : iSpreadPenaltyWeight * placement.getSpreadPenalty())
                + (iCommitedStudentConflictWeight == 0.0 ? 0.0 : iCommitedStudentConflictWeight
                        * (lecture.getCommitedConflicts(placement) + lecture.countCommittedStudentConflicts(placement)));
    }
}
