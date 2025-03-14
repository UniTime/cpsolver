package org.cpsolver.coursett.heuristics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.perturbations.DefaultPerturbationsCounter;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;


/**
 * Perturbation penalty computation. <br>
 * <br>
 * In practice, the strategy for computing perturbations needs to be extended.
 * For example, a change in time is usually much worse than a movement to a
 * different classroom. The number of enrolled/involved students should also be
 * taken into account. Another factor is whether the solution has already been
 * published or not. <br>
 * The priorities for evaluating perturbations are as follows. Before publishing
 * timetable:
 * <ul>
 * <li>minimize number of classes with time changes,
 * <li>minimize number of student conflicts,
 * <li>optimize satisfaction of problem soft constraints.
 * </ul>
 * <br>
 * After publishing the timetable (class time changes are not allowed):
 * <ul>
 * <li>minimize number of additional (new) student conflicts,
 * <li>minimize number of students with time changes,
 * <li>minimize number of classes with time changes,
 * <li>optimize satisfaction of problem soft constraints.
 * </ul>
 * In both cases, the number of classes with room change is not significant at
 * all. Before the timetable is published, minimizing the number of classes with
 * time changes is the most important criteria for the MPP as long as it does
 * not create too many additional student conflicts in the process. Therefore,
 * as a compromise, the cost (in equivalent conflicts) of changing the time
 * assigned to a class equals a number like 5% of the students enrolled in that
 * class. Otherwise none of our other criteria would have any importance. <br>
 * <br>
 * Similar properties apply between other criteria as well. To fulfill all these
 * needs we have created a function (called perturbations penalty) which can be
 * computed over a partial solution. This is a weighted sum of various
 * perturbations criteria like the number of classes with time changes or the
 * number of additional student conflicts. This perturbation penalty is added as
 * an extra optimization criterion to the solution comparator and to value
 * selection criterion, so we can also setup the weights between this
 * perturbation penalty and other (initial) soft constraints. <br>
 * <br>
 * Parameters:
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Perturbations.DifferentPlacement</td>
 * <td>{@link Double}</td>
 * <td>Different value than initial is assigned</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedStudentWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of students which are enrolled in a class which is placed to a
 * different location than initial (a student can be included twice or more)</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedInstructorWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of instructors which are assigned to classes which are placed to
 * different locations than initial (an instructor can be included twice or
 * more)</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DifferentRoomWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of classes which are placed to a different room than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DifferentBuildingWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of classes which are placed to a different building than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DifferentTimeWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of classes which are placed in a different time than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DifferentDayWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of classes which are placed in a different days than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DifferentHourWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of classes which are placed in a different hours than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DeltaStudentConflictsWeight</td>
 * <td>{@link Double}</td>
 * <td>Difference of student conflicts of classes assigned to current placements
 * instead of initial placements. It is a difference between number of students
 * conflicts which are in the initial solution and the current one. Student
 * conflicts created by classes without initial placement are not taken into
 * account</td>
 * </tr>
 * <tr>
 * <td>Perturbations.NewStudentConflictsWeight</td>
 * <td>{@link Double}</td>
 * <td>New created student conflicts -- particular students are taken into
 * account. Student conflicts created by classes without initial placement are
 * not taken into account</td>
 * </tr>
 * <tr>
 * <td>Perturbations.TooFarForInstructorsWeight</td>
 * <td>{@link Double}</td>
 * <td>New placement of a class is too far from the intial placement
 * (instructor-wise). It is computed only when the class has an instructor
 * assigned, moreover:
 * <ul>
 * <li>0 &lt; distance(currentPlacement,initialPlacement) &lt;= 5 .. weight is taken
 * once
 * <li>5 &lt; distance(currentPlacement,initialPlacement) &lt;= 20 .. weight is taken
 * twice
 * <li>20 &lt; distance(currentPlacement,initialPlacement) .. weight is taken ten
 * times
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td>Perturbations.TooFarForStudentsWeight</td>
 * <td>{@link Double}</td>
 * <td>New placement of a class is too far from the intial placement
 * (instructor-student). It is weighted by the number of students enrolled in
 * the class when distance(currentPlacement,initialPlacement) &gt; 67</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DeltaInstructorDistancePreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Difference between number of instructor distance preferences of the
 * initial (but maybe inconsistent) solution and the current solution.
 * Instructor distance preferences of classes without initial placement are not
 * taken into account</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DeltaRoomPreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Difference between room preferences of the initial and the current
 * solution. Room preferences of classes without initial placement are not taken
 * into account</td>
 * </tr>
 * <tr>
 * <td>Perturbations.DeltaTimePreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Difference between time preferences of the initial and the current
 * solution. Time preferences of classes without initial placement are not taken
 * into account</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedStudentByTimeWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of students which are enrolled in a class which is placed to a
 * different time than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedInstructorByTimeWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of instructors which are assigned to classes which are placed to
 * different time than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedStudentByRoomWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of students which are enrolled in a class which is placed to a
 * different room than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedInstructorByRoomWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of instructors which are assigned to classes which are placed to
 * different room than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedStudentByBldgWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of students which are enrolled in a class which is placed to a
 * different building than initial</td>
 * </tr>
 * <tr>
 * <td>Perturbations.AffectedInstructorByBldgWeight</td>
 * <td>{@link Double}</td>
 * <td>Number of instructors which are assigned to classes which are placed to
 * different building than initial</td>
 * </tr>
 * </table>
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

public class UniversalPerturbationsCounter extends DefaultPerturbationsCounter<Lecture, Placement> {
    private double iDifferentPlacement = 1.0;
    private double iAffectedStudentWeight = 0.0;
    private double iAffectedInstructorWeight = 0.0;
    private double iAffectedStudentByTimeWeight = 0.0;
    private double iAffectedInstructorByTimeWeight = 0.0;
    private double iAffectedStudentByRoomWeight = 0.0;
    private double iAffectedInstructorByRoomWeight = 0.0;
    private double iAffectedStudentByBldgWeight = 0.0;
    private double iAffectedInstructorByBldgWeight = 0.0;
    private double iDifferentRoomWeight = 0.0;
    private double iDifferentBuildingWeight = 0.0;
    private double iDifferentTimeWeight = 0.0;
    private double iDifferentDayWeight = 0.0;
    private double iDifferentHourWeight = 0.0;
    private double iNewStudentConflictsWeight = 0.0;
    private double iDeltaStudentConflictsWeight = 0.0;
    private double iTooFarForInstructorsWeight = 0.0;
    private double iTooFarForStudentsWeight = 0.0;
    private double iDeltaInstructorDistancePreferenceWeight = 0.0;
    private double iDeltaRoomPreferenceWeight = 0.0;
    private double iDeltaTimePreferenceWeight = 0.0;
    private boolean iMPP = false;
    private DistanceMetric iDistanceMetric = null;

    public UniversalPerturbationsCounter(DataProperties properties) {
        super(properties);
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        iDifferentPlacement = properties.getPropertyDouble("Perturbations.DifferentPlacement", iDifferentPlacement);
        iAffectedStudentWeight = properties.getPropertyDouble("Perturbations.AffectedStudentWeight",
                iAffectedStudentWeight);
        iAffectedInstructorWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorWeight",
                iAffectedInstructorWeight);
        iAffectedStudentByTimeWeight = properties.getPropertyDouble("Perturbations.AffectedStudentByTimeWeight",
                iAffectedStudentByTimeWeight);
        iAffectedInstructorByTimeWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorByTimeWeight",
                iAffectedInstructorByTimeWeight);
        iAffectedStudentByRoomWeight = properties.getPropertyDouble("Perturbations.AffectedStudentByRoomWeight",
                iAffectedStudentByRoomWeight);
        iAffectedInstructorByRoomWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorByRoomWeight",
                iAffectedInstructorByRoomWeight);
        iAffectedStudentByBldgWeight = properties.getPropertyDouble("Perturbations.AffectedStudentByBldgWeight",
                iAffectedStudentByBldgWeight);
        iAffectedInstructorByBldgWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorByBldgWeight",
                iAffectedInstructorByBldgWeight);
        iDifferentRoomWeight = properties.getPropertyDouble("Perturbations.DifferentRoomWeight", iDifferentRoomWeight);
        iDifferentBuildingWeight = properties.getPropertyDouble("Perturbations.DifferentBuildingWeight",
                iDifferentBuildingWeight);
        iDifferentTimeWeight = properties.getPropertyDouble("Perturbations.DifferentTimeWeight", iDifferentTimeWeight);
        iDifferentDayWeight = properties.getPropertyDouble("Perturbations.DifferentDayWeight", iDifferentDayWeight);
        iDifferentHourWeight = properties.getPropertyDouble("Perturbations.DifferentHourWeight", iDifferentHourWeight);
        iDeltaStudentConflictsWeight = properties.getPropertyDouble("Perturbations.DeltaStudentConflictsWeight",
                iDeltaStudentConflictsWeight);
        iNewStudentConflictsWeight = properties.getPropertyDouble("Perturbations.NewStudentConflictsWeight",
                iNewStudentConflictsWeight);
        iTooFarForInstructorsWeight = properties.getPropertyDouble("Perturbations.TooFarForInstructorsWeight",
                iTooFarForInstructorsWeight);
        iTooFarForStudentsWeight = properties.getPropertyDouble("Perturbations.TooFarForStudentsWeight",
                iTooFarForStudentsWeight);
        iDeltaInstructorDistancePreferenceWeight = properties.getPropertyDouble(
                "Perturbations.DeltaInstructorDistancePreferenceWeight", iDeltaInstructorDistancePreferenceWeight);
        iDeltaRoomPreferenceWeight = properties.getPropertyDouble("Perturbations.DeltaRoomPreferenceWeight",
                iDeltaRoomPreferenceWeight);
        iDeltaTimePreferenceWeight = properties.getPropertyDouble("Perturbations.DeltaTimePreferenceWeight",
                iDeltaTimePreferenceWeight);
        iDistanceMetric = new DistanceMetric(properties);
    }

    @Override
    protected double getPenalty(Assignment<Lecture, Placement> assignment, Placement assignedPlacement, Placement initialPlacement) {
        // assigned and initial value of the same lecture
        // assigned might be null
        Lecture lecture = initialPlacement.variable();
        double penalty = 0.0;
        if (iDifferentPlacement != 0.0)
            penalty += iDifferentPlacement;
        if (iAffectedStudentWeight != 0.0)
            penalty += iAffectedStudentWeight * lecture.classLimit(assignment);
        if (iAffectedInstructorWeight != 0.0)
            penalty += iAffectedInstructorWeight * lecture.getInstructorConstraints().size();
        if (assignedPlacement != null) {
            if ((iDifferentRoomWeight != 0.0 || iAffectedInstructorByRoomWeight != 0.0 || iAffectedStudentByRoomWeight != 0.0)) {
                int nrDiff = initialPlacement.nrDifferentRooms(assignedPlacement);
                penalty += nrDiff * iDifferentRoomWeight;
                penalty += nrDiff * iAffectedInstructorByRoomWeight * lecture.getInstructorConstraints().size();
                penalty += nrDiff * iAffectedStudentByRoomWeight * lecture.classLimit(assignment);
            }
            if ((iDifferentBuildingWeight != 0.0 || iAffectedInstructorByBldgWeight != 0.0 || iAffectedStudentByBldgWeight != 0.0)) {
                int nrDiff = initialPlacement.nrDifferentBuildings(assignedPlacement);
                penalty += nrDiff * iDifferentBuildingWeight;
                penalty += nrDiff * iAffectedInstructorByBldgWeight * lecture.getInstructorConstraints().size();
                penalty += nrDiff * iAffectedStudentByBldgWeight * lecture.classLimit(assignment);
            }
            if ((iDifferentTimeWeight != 0.0 || iAffectedInstructorByTimeWeight != 0.0 || iAffectedStudentByTimeWeight != 0.0)
                    && !initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                penalty += iDifferentTimeWeight;
                penalty += iAffectedInstructorByTimeWeight * lecture.getInstructorConstraints().size();
                penalty += iAffectedStudentByTimeWeight * lecture.classLimit(assignment);
            }
            if (iDifferentDayWeight != 0.0
                    && initialPlacement.getTimeLocation().getDayCode() != assignedPlacement.getTimeLocation()
                            .getDayCode())
                penalty += iDifferentDayWeight;
            if (iDifferentHourWeight != 0.0
                    && initialPlacement.getTimeLocation().getStartSlot() != assignedPlacement.getTimeLocation()
                            .getStartSlot())
                penalty += iDifferentHourWeight;
            if ((iTooFarForInstructorsWeight != 0.0 || iTooFarForStudentsWeight != 0.0)
                    && !initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                double distance = Placement.getDistanceInMeters(iDistanceMetric, initialPlacement, assignedPlacement);
                if (!lecture.getInstructorConstraints().isEmpty() && iTooFarForInstructorsWeight != 0.0) {
                    if (distance > iDistanceMetric.getInstructorNoPreferenceLimit() && distance <= iDistanceMetric.getInstructorDiscouragedLimit()) {
                        penalty += Constants.sPreferenceLevelDiscouraged * iTooFarForInstructorsWeight
                                * lecture.getInstructorConstraints().size();
                    } else if (distance > iDistanceMetric.getInstructorDiscouragedLimit() && distance <= iDistanceMetric.getInstructorProhibitedLimit()) {
                        penalty += Constants.sPreferenceLevelStronglyDiscouraged * iTooFarForInstructorsWeight
                                * lecture.getInstructorConstraints().size();
                    } else if (distance > iDistanceMetric.getInstructorProhibitedLimit()) {
                        penalty += Constants.sPreferenceLevelProhibited * iTooFarForInstructorsWeight
                                * lecture.getInstructorConstraints().size();
                    }
                }
                if (iTooFarForStudentsWeight != 0.0
                        && distance > iDistanceMetric.minutes2meters(10))
                    penalty += iTooFarForStudentsWeight * lecture.classLimit(assignment);
            }
            if (iDeltaStudentConflictsWeight != 0.0) {
                int newStudentConflicts = lecture.countStudentConflicts(assignment, assignedPlacement);
                int oldStudentConflicts = lecture.countInitialStudentConflicts();
                penalty += iDeltaStudentConflictsWeight * (newStudentConflicts - oldStudentConflicts);
            }
            if (iNewStudentConflictsWeight != 0.0) {
                Set<Student> newStudentConflicts = lecture.conflictStudents(assignment, assignedPlacement);
                Set<Student> initialStudentConflicts = lecture.initialStudentConflicts();
                for (Iterator<Student> i = newStudentConflicts.iterator(); i.hasNext();)
                    if (!initialStudentConflicts.contains(i.next()))
                        penalty += iNewStudentConflictsWeight;
            }
            if (iDeltaTimePreferenceWeight != 0.0) {
                penalty += iDeltaTimePreferenceWeight * (assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference());
            }
            if (iDeltaRoomPreferenceWeight != 0.0) {
                penalty += iDeltaRoomPreferenceWeight * (assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference());
            }
            if (iDeltaInstructorDistancePreferenceWeight != 0.0) {
                for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
                    for (Lecture lect : ic.variables()) {
                        if (lect.equals(lecture))
                            continue;
                        int initialPreference = (lect.getInitialAssignment() == null ? Constants.sPreferenceLevelNeutral : ic.getDistancePreference(initialPlacement, lect.getInitialAssignment()));
                        int assignedPreference = (assignment.getValue(lect) == null ? Constants.sPreferenceLevelNeutral : ic.getDistancePreference(assignedPlacement, assignment.getValue(lect)));
                        penalty += iDeltaInstructorDistancePreferenceWeight * (assignedPreference - initialPreference);
                    }
                }
            }
        }
        return penalty;
    }

    public void getInfo(Assignment<Lecture, Placement> assignment, TimetableModel model, Map<String, String> info) {
        getInfo(assignment, model, info, null);
    }

    public void getInfo(Assignment<Lecture, Placement> assignment, TimetableModel model, Map<String, String> info, List<Lecture> variables) {
        if (variables == null)
            super.getInfo(assignment, model, info);
        else
            super.getInfo(assignment, model, info, variables);
        if (!iMPP)
            return;
        int perts = 0;
        long affectedStudents = 0;
        int affectedInstructors = 0;
        long affectedStudentsByTime = 0;
        int affectedInstructorsByTime = 0;
        long affectedStudentsByRoom = 0;
        int affectedInstructorsByRoom = 0;
        long affectedStudentsByBldg = 0;
        int affectedInstructorsByBldg = 0;
        int differentRoom = 0;
        int differentBuilding = 0;
        int differentTime = 0;
        int differentDay = 0;
        int differentHour = 0;
        int tooFarForInstructors = 0;
        int tooFarForStudents = 0;
        int deltaStudentConflicts = 0;
        int newStudentConflicts = 0;
        double deltaTimePreferences = 0;
        int deltaRoomPreferences = 0;
        int deltaInstructorDistancePreferences = 0;
        for (Lecture lecture : (variables == null ? model.perturbVariables(assignment, model.variablesWithInitialValue(), false) : model.perturbVariables(assignment, variables, false))) {
            if (assignment.getValue(lecture) == null || lecture.getInitialAssignment() == null || assignment.getValue(lecture).equals(lecture.getInitialAssignment()))
                continue;
            perts++;
            Placement assignedPlacement = assignment.getValue(lecture);
            Placement initialPlacement = lecture.getInitialAssignment();
            affectedStudents += lecture.classLimit(assignment);
            affectedInstructors += lecture.getInstructorConstraints().size();

            int nrDiff = initialPlacement.nrDifferentRooms(assignedPlacement);
            differentRoom += nrDiff;
            affectedInstructorsByRoom += nrDiff * lecture.getInstructorConstraints().size();
            affectedStudentsByRoom += nrDiff * lecture.classLimit(assignment);

            nrDiff = initialPlacement.nrDifferentBuildings(assignedPlacement);
            differentBuilding += nrDiff;
            affectedInstructorsByBldg += nrDiff * lecture.getInstructorConstraints().size();
            affectedStudentsByBldg += nrDiff * lecture.classLimit(assignment);

            deltaRoomPreferences += assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference();

            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                differentTime++;
                affectedInstructorsByTime += lecture.getInstructorConstraints().size();
                affectedStudentsByTime += lecture.classLimit(assignment);
            }
            if (initialPlacement.getTimeLocation().getDayCode() != assignedPlacement.getTimeLocation().getDayCode())
                differentDay++;
            if (initialPlacement.getTimeLocation().getStartSlot() != assignedPlacement.getTimeLocation().getStartSlot())
                differentHour++;
            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                double distance = Placement.getDistanceInMeters(iDistanceMetric, initialPlacement, assignedPlacement);
                if (!lecture.getInstructorConstraints().isEmpty()) {
                    if (distance > iDistanceMetric.getInstructorNoPreferenceLimit() && distance <= iDistanceMetric.getInstructorDiscouragedLimit()) {
                        tooFarForInstructors += Constants.sPreferenceLevelDiscouraged * lecture.getInstructorConstraints().size();
                    } else if (distance > iDistanceMetric.getInstructorDiscouragedLimit() && distance <= iDistanceMetric.getInstructorProhibitedLimit()) {
                        tooFarForInstructors += Constants.sPreferenceLevelStronglyDiscouraged * lecture.getInstructorConstraints().size();
                    } else if (distance > iDistanceMetric.getInstructorProhibitedLimit()) {
                        tooFarForInstructors += Constants.sPreferenceLevelProhibited * lecture.getInstructorConstraints().size();
                    }
                }
                if (distance > iDistanceMetric.minutes2meters(10))
                    tooFarForStudents += lecture.classLimit(assignment);
            }
            deltaStudentConflicts += lecture.countStudentConflicts(assignment, assignedPlacement) - lecture.countInitialStudentConflicts();
            Set<Student> newStudentConflictsSet = lecture.conflictStudents(assignment, assignedPlacement);
            Set<Student> initialStudentConflicts = lecture.initialStudentConflicts();
            for (Iterator<Student> e1 = newStudentConflictsSet.iterator(); e1.hasNext();)
                if (!initialStudentConflicts.contains(e1.next()))
                    newStudentConflicts++;
            deltaTimePreferences += assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference();
            for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
                for (Lecture lect : ic.variables()) {
                    if (lect.equals(lecture))
                        continue;
                    int initialPreference = (lect.getInitialAssignment() == null ? Constants.sPreferenceLevelNeutral
                            : ic.getDistancePreference(initialPlacement, lect.getInitialAssignment()));
                    int assignedPreference = (assignedPlacement == null ? Constants.sPreferenceLevelNeutral : ic.getDistancePreference(assignedPlacement, assignedPlacement));
                    deltaInstructorDistancePreferences += assignedPreference - initialPreference;
                }
            }
        }
        if (perts != 0)
            info.put("Perturbations: Different placement", String.valueOf(perts) + " (weighted " + sDoubleFormat.format(iDifferentPlacement * perts) + ")");
        if (affectedStudents != 0)
            info.put("Perturbations: Number of affected students", String.valueOf(affectedStudents) + " (weighted " + sDoubleFormat.format(iAffectedStudentWeight * affectedStudents) + ")");
        if (affectedInstructors != 0)
            info.put("Perturbations: Number of affected instructors", String.valueOf(affectedInstructors) + " (weighted " + sDoubleFormat.format(iAffectedInstructorWeight * affectedInstructors) + ")");
        if (affectedStudentsByTime != 0)
            info.put("Perturbations: Number of affected students [time]", String.valueOf(affectedStudentsByTime) +
                    " (weighted " + sDoubleFormat.format(iAffectedStudentByTimeWeight * affectedStudentsByTime) + ")");
        if (affectedInstructorsByTime != 0)
            info.put("Perturbations: Number of affected instructors [time]", String.valueOf(affectedInstructorsByTime) +
                    " (weighted " + sDoubleFormat.format(iAffectedInstructorByTimeWeight * affectedInstructorsByTime) + ")");
        if (affectedStudentsByRoom != 0)
            info.put("Perturbations: Number of affected students [room]", String.valueOf(affectedStudentsByRoom) + 
                    " (weighted " + sDoubleFormat.format(iAffectedStudentByRoomWeight * affectedStudentsByRoom) + ")");
        if (affectedInstructorsByRoom != 0)
            info.put("Perturbations: Number of affected instructors [room]", String.valueOf(affectedInstructorsByRoom) +
                    " (weighted " + sDoubleFormat.format(iAffectedInstructorByRoomWeight * affectedInstructorsByRoom) + ")");
        if (affectedStudentsByBldg != 0)
            info.put("Perturbations: Number of affected students [bldg]", String.valueOf(affectedStudentsByBldg) +
                    " (weighted " + sDoubleFormat.format(iAffectedStudentByBldgWeight * affectedStudentsByBldg) + ")");
        if (affectedInstructorsByBldg != 0)
            info.put("Perturbations: Number of affected instructors [bldg]", String.valueOf(affectedInstructorsByBldg) +
                    " (weighted " + sDoubleFormat.format(iAffectedInstructorByBldgWeight * affectedInstructorsByBldg) + ")");
        if (differentRoom != 0)
            info.put("Perturbations: Different room", String.valueOf(differentRoom) + " (weighted " + sDoubleFormat.format(iDifferentRoomWeight * differentRoom) + ")");
        if (differentBuilding != 0)
            info.put("Perturbations: Different building", String.valueOf(differentBuilding) + " (weighted " + sDoubleFormat.format(iDifferentBuildingWeight * differentBuilding) + ")");
        if (differentTime != 0)
            info.put("Perturbations: Different time", String.valueOf(differentTime) + " (weighted " + sDoubleFormat.format(iDifferentTimeWeight * differentTime) + ")");
        if (differentDay != 0)
            info.put("Perturbations: Different day", String.valueOf(differentDay) + " (weighted " + sDoubleFormat.format(iDifferentDayWeight * differentDay) + ")");
        if (differentHour != 0)
            info.put("Perturbations: Different hour", String.valueOf(differentHour) + " (weighted " + sDoubleFormat.format(iDifferentHourWeight * differentHour) + ")");
        if (tooFarForInstructors != 0)
            info.put("Perturbations: New placement too far from initial [instructors]", String.valueOf(tooFarForInstructors) + 
                    " (weighted " + sDoubleFormat.format(iTooFarForInstructorsWeight * tooFarForInstructors) + ")");
        if (tooFarForStudents != 0)
            info.put("Perturbations: New placement too far from initial [students]", String.valueOf(tooFarForStudents) +
                    " (weighted " + sDoubleFormat.format(iTooFarForStudentsWeight * tooFarForStudents) + ")");
        if (deltaStudentConflicts != 0)
            info.put("Perturbations: Delta student conflicts", String.valueOf(deltaStudentConflicts) + " (weighted " + sDoubleFormat.format(iDeltaStudentConflictsWeight * deltaStudentConflicts) + ")");
        if (newStudentConflicts != 0)
            info.put("Perturbations: New student conflicts", String.valueOf(newStudentConflicts) + " (weighted " + sDoubleFormat.format(iNewStudentConflictsWeight * newStudentConflicts) + ")");
        if (deltaTimePreferences != 0)
            info.put("Perturbations: Delta time preferences", String.valueOf(deltaTimePreferences) + " (weighted " + sDoubleFormat.format(iDeltaTimePreferenceWeight * deltaTimePreferences) + ")");
        if (deltaRoomPreferences != 0)
            info.put("Perturbations: Delta room preferences", String.valueOf(deltaRoomPreferences) + " (weighted " + sDoubleFormat.format(iDeltaRoomPreferenceWeight * deltaRoomPreferences) + ")");
        if (deltaInstructorDistancePreferences != 0)
            info.put("Perturbations: Delta instructor distance preferences", String.valueOf(deltaInstructorDistancePreferences) +
                    " (weighted " + sDoubleFormat.format(iDeltaInstructorDistancePreferenceWeight * deltaInstructorDistancePreferences) + ")");
    }

    public Map<String, Double> getCompactInfo(Assignment<Lecture, Placement> assignment, TimetableModel model, boolean includeZero, boolean weighted) {
        Map<String, Double> info = new HashMap<String, Double>();
        if (!iMPP)
            return info;
        int perts = 0;
        long affectedStudents = 0;
        int affectedInstructors = 0;
        long affectedStudentsByTime = 0;
        int affectedInstructorsByTime = 0;
        long affectedStudentsByRoom = 0;
        int affectedInstructorsByRoom = 0;
        long affectedStudentsByBldg = 0;
        int affectedInstructorsByBldg = 0;
        int differentRoom = 0;
        int differentBuilding = 0;
        int differentTime = 0;
        int differentDay = 0;
        int differentHour = 0;
        int tooFarForInstructors = 0;
        int tooFarForStudents = 0;
        int deltaStudentConflicts = 0;
        int newStudentConflicts = 0;
        double deltaTimePreferences = 0;
        int deltaRoomPreferences = 0;
        int deltaInstructorDistancePreferences = 0;
        for (Lecture lecture : model.perturbVariables(assignment)) {
            if (assignment.getValue(lecture) == null || lecture.getInitialAssignment() == null || assignment.getValue(lecture).equals(lecture.getInitialAssignment()))
                continue;
            perts++;
            Placement assignedPlacement = assignment.getValue(lecture);
            Placement initialPlacement = lecture.getInitialAssignment();
            affectedStudents += lecture.classLimit(assignment);
            affectedInstructors += lecture.getInstructorConstraints().size();

            int nrDiff = initialPlacement.nrDifferentRooms(assignedPlacement);
            differentRoom += nrDiff;
            affectedInstructorsByRoom += nrDiff * lecture.getInstructorConstraints().size();
            affectedStudentsByRoom += nrDiff * lecture.classLimit(assignment);

            nrDiff = initialPlacement.nrDifferentBuildings(initialPlacement);
            differentBuilding += nrDiff;
            affectedInstructorsByBldg += nrDiff * lecture.getInstructorConstraints().size();
            affectedStudentsByBldg += nrDiff * lecture.classLimit(assignment);

            deltaRoomPreferences += assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference();

            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                differentTime++;
                affectedInstructorsByTime += lecture.getInstructorConstraints().size();
                affectedStudentsByTime += lecture.classLimit(assignment);
            }
            if (initialPlacement.getTimeLocation().getDayCode() != assignedPlacement.getTimeLocation().getDayCode())
                differentDay++;
            if (initialPlacement.getTimeLocation().getStartSlot() != assignedPlacement.getTimeLocation().getStartSlot())
                differentHour++;
            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                double distance = Placement.getDistanceInMeters(iDistanceMetric, initialPlacement, assignedPlacement);
                if (!lecture.getInstructorConstraints().isEmpty()) {
                    if (distance > iDistanceMetric.getInstructorNoPreferenceLimit() && distance <= iDistanceMetric.getInstructorDiscouragedLimit()) {
                        tooFarForInstructors += Constants.sPreferenceLevelDiscouraged;
                    } else if (distance > iDistanceMetric.getInstructorDiscouragedLimit() && distance <= iDistanceMetric.getInstructorProhibitedLimit()) {
                        tooFarForInstructors += Constants.sPreferenceLevelStronglyDiscouraged;
                    } else if (distance > iDistanceMetric.getInstructorProhibitedLimit()) {
                        tooFarForInstructors += Constants.sPreferenceLevelProhibited;
                    }
                }
                if (distance > iDistanceMetric.minutes2meters(10))
                    tooFarForStudents += lecture.classLimit(assignment);
            }
            deltaStudentConflicts += lecture.countStudentConflicts(assignment, assignedPlacement) - lecture.countInitialStudentConflicts();
            Set<Student> newStudentConflictsSet = lecture.conflictStudents(assignment, assignedPlacement);
            Set<Student> initialStudentConflicts = lecture.initialStudentConflicts();
            for (Iterator<Student> e1 = newStudentConflictsSet.iterator(); e1.hasNext();)
                if (!initialStudentConflicts.contains(e1.next()))
                    newStudentConflicts++;
            deltaTimePreferences += assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference();
            for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
                if (ic != null)
                    for (Lecture lect : ic.variables()) {
                        if (lect.equals(lecture))
                            continue;
                        int initialPreference = (lect.getInitialAssignment() == null ? Constants.sPreferenceLevelNeutral
                                : ic.getDistancePreference(initialPlacement, lect.getInitialAssignment()));
                        int assignedPreference = (assignedPlacement == null ? Constants.sPreferenceLevelNeutral : ic.getDistancePreference(assignedPlacement, assignedPlacement));
                        deltaInstructorDistancePreferences += assignedPreference - initialPreference;
                    }
            }
        }
        if (includeZero || iDifferentPlacement != 0.0)
            info.put("Different placement", Double.valueOf(weighted ? iDifferentPlacement * perts : perts));
        if (includeZero || iAffectedStudentWeight != 0.0)
            info.put("Affected students", Double.valueOf(weighted ? iAffectedStudentWeight * affectedStudents : affectedStudents));
        if (includeZero || iAffectedInstructorWeight != 0.0)
            info.put("Affected instructors", Double.valueOf(weighted ? iAffectedInstructorWeight * affectedInstructors : affectedInstructors));
        if (includeZero || iAffectedStudentByTimeWeight != 0.0)
            info.put("Affected students [time]", Double.valueOf(weighted ? iAffectedStudentByTimeWeight * affectedStudentsByTime : affectedStudentsByTime));
        if (includeZero || iAffectedInstructorByTimeWeight != 0.0)
            info.put("Affected instructors [time]", Double.valueOf(weighted ? iAffectedInstructorByTimeWeight * affectedInstructorsByTime : affectedInstructorsByTime));
        if (includeZero || iAffectedStudentByRoomWeight != 0.0)
            info.put("Affected students [room]", Double.valueOf(weighted ? iAffectedStudentByRoomWeight * affectedStudentsByRoom : affectedStudentsByRoom));
        if (includeZero || iAffectedInstructorByRoomWeight != 0.0)
            info.put("Affected instructors [room]", Double.valueOf(weighted ? iAffectedInstructorByRoomWeight * affectedInstructorsByRoom : affectedInstructorsByRoom));
        if (includeZero || iAffectedStudentByBldgWeight != 0.0)
            info.put("Affected students [bldg]", Double.valueOf(weighted ? iAffectedStudentByBldgWeight * affectedStudentsByBldg : affectedStudentsByBldg));
        if (includeZero || iAffectedInstructorByBldgWeight != 0.0)
            info.put("Affected instructors [bldg]", Double.valueOf(weighted ? iAffectedInstructorByBldgWeight * affectedInstructorsByBldg : affectedInstructorsByBldg));
        if (includeZero || iDifferentRoomWeight != 0.0)
            info.put("Different room", Double.valueOf(weighted ? iDifferentRoomWeight * differentRoom : differentRoom));
        if (includeZero || iDifferentBuildingWeight != 0.0)
            info.put("Different building", Double.valueOf(weighted ? iDifferentBuildingWeight * differentBuilding : differentBuilding));
        if (includeZero || iDifferentTimeWeight != 0.0)
            info.put("Different time", Double.valueOf(weighted ? iDifferentTimeWeight * differentTime : differentTime));
        if (includeZero || iDifferentDayWeight != 0.0)
            info.put("Different day", Double.valueOf(weighted ? iDifferentDayWeight * differentDay : differentDay));
        if (includeZero || iDifferentHourWeight != 0.0)
            info.put("Different hour", Double.valueOf(weighted ? iDifferentHourWeight * differentHour : differentHour));
        if (includeZero || iTooFarForInstructorsWeight != 0.0)
            info.put("New placement too far for initial [instructors]", Double.valueOf(weighted ? iTooFarForInstructorsWeight * tooFarForInstructors : tooFarForInstructors));
        if (includeZero || iTooFarForStudentsWeight != 0.0)
            info.put("New placement too far for initial [students]", Double.valueOf(weighted ? iTooFarForStudentsWeight * tooFarForStudents : tooFarForStudents));
        if (includeZero || iDeltaStudentConflictsWeight != 0.0)
            info.put("Delta student conflicts", Double.valueOf(weighted ? iDeltaStudentConflictsWeight * deltaStudentConflicts : deltaStudentConflicts));
        if (includeZero || iNewStudentConflictsWeight != 0.0)
            info.put("New student conflicts", Double.valueOf(weighted ? iNewStudentConflictsWeight * newStudentConflicts : newStudentConflicts));
        if (includeZero || iDeltaTimePreferenceWeight != 0.0)
            info.put("Delta time preferences", Double.valueOf(weighted ? iDeltaTimePreferenceWeight * deltaTimePreferences : deltaTimePreferences));
        if (includeZero || iDeltaRoomPreferenceWeight != 0.0)
            info.put("Delta room preferences", Double.valueOf(weighted ? iDeltaRoomPreferenceWeight * deltaRoomPreferences : deltaRoomPreferences));
        if (includeZero || iDeltaInstructorDistancePreferenceWeight != 0.0)
            info.put("Delta instructor distance preferences", Double.valueOf(weighted ? iDeltaInstructorDistancePreferenceWeight * deltaInstructorDistancePreferences : deltaInstructorDistancePreferences));
        return info;
    }

    public Map<String, Double> getCompactInfo(Assignment<Lecture, Placement> assignment, TimetableModel model, Placement assignedPlacement, boolean includeZero,
            boolean weighted) {
        Map<String, Double> info = new HashMap<String, Double>();
        if (!iMPP)
            return info;
        Lecture lecture = assignedPlacement.variable();
        Placement initialPlacement = lecture.getInitialAssignment();
        if (initialPlacement == null || initialPlacement.equals(assignedPlacement))
            return info;
        int perts = 1;
        long affectedStudents = lecture.classLimit(assignment);
        int affectedInstructors = lecture.getInstructorConstraints().size();
        long affectedStudentsByTime = (initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation()) ? 0 : lecture.classLimit(assignment));
        int affectedInstructorsByTime = (initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation()) ? 0 : lecture.getInstructorConstraints().size());

        int differentRoom = initialPlacement.nrDifferentRooms(assignedPlacement);
        int affectedInstructorsByRoom = differentRoom * lecture.getInstructorConstraints().size();
        long affectedStudentsByRoom = differentRoom * lecture.classLimit(assignment);

        int differentBuilding = initialPlacement.nrDifferentBuildings(initialPlacement);
        int affectedInstructorsByBldg = differentBuilding * lecture.getInstructorConstraints().size();
        long affectedStudentsByBldg = differentBuilding * lecture.classLimit(assignment);

        int deltaRoomPreferences = assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference();

        int differentTime = (initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation()) ? 0 : 1);
        int differentDay = (initialPlacement.getTimeLocation().getDayCode() != assignedPlacement.getTimeLocation().getDayCode() ? 1 : 0);
        int differentHour = (initialPlacement.getTimeLocation().getStartSlot() != assignedPlacement.getTimeLocation().getStartSlot() ? 1 : 0);
        int tooFarForInstructors = 0;
        int tooFarForStudents = 0;
        int deltaStudentConflicts = lecture.countStudentConflicts(assignment, assignedPlacement) - lecture.countInitialStudentConflicts();
        int newStudentConflicts = 0;
        double deltaTimePreferences = (assignedPlacement.getTimeLocation().getNormalizedPreference() -
                initialPlacement.getTimeLocation().getNormalizedPreference());
        int deltaInstructorDistancePreferences = 0;

        double distance = Placement.getDistanceInMeters(iDistanceMetric, initialPlacement, assignedPlacement);
        if (!lecture.getInstructorConstraints().isEmpty()) {
            if (distance > iDistanceMetric.getInstructorNoPreferenceLimit() && distance <= iDistanceMetric.getInstructorDiscouragedLimit()) {
                tooFarForInstructors += lecture.getInstructorConstraints().size();
            } else if (distance > iDistanceMetric.getInstructorDiscouragedLimit() && distance <= iDistanceMetric.getInstructorProhibitedLimit()) {
                tooFarForInstructors += 2 * lecture.getInstructorConstraints().size();
            } else if (distance > iDistanceMetric.getInstructorProhibitedLimit()) {
                tooFarForInstructors += 10 * lecture.getInstructorConstraints().size();
            }
        }
        if (distance > iDistanceMetric.minutes2meters(10))
            tooFarForStudents = lecture.classLimit(assignment);

        Set<Student> newStudentConflictsVect = lecture.conflictStudents(assignment, assignedPlacement);
        Set<Student> initialStudentConflicts = lecture.initialStudentConflicts();
        for (Iterator<Student> e = newStudentConflictsVect.iterator(); e.hasNext();)
            if (!initialStudentConflicts.contains(e.next()))
                newStudentConflicts++;

        for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
            for (Lecture lect : ic.variables()) {
                if (lect.equals(lecture))
                    continue;
                int initialPreference = (lect.getInitialAssignment() == null ? Constants.sPreferenceLevelNeutral : ic.getDistancePreference(initialPlacement, lect.getInitialAssignment()));
                int assignedPreference = (assignment.getValue(lect) == null ? Constants.sPreferenceLevelNeutral : ic.getDistancePreference(assignedPlacement, assignment.getValue(lect)));
                deltaInstructorDistancePreferences += (assignedPreference - initialPreference);
            }
        }

        if (includeZero || iDifferentPlacement != 0.0)
            info.put("Different placement", Double.valueOf(weighted ? iDifferentPlacement * perts : perts));
        if (includeZero || iAffectedStudentWeight != 0.0)
            info.put("Affected students", Double.valueOf(weighted ? iAffectedStudentWeight * affectedStudents : affectedStudents));
        if (includeZero || iAffectedInstructorWeight != 0.0)
            info.put("Affected instructors", Double.valueOf(weighted ? iAffectedInstructorWeight * affectedInstructors : affectedInstructors));
        if (includeZero || iAffectedStudentByTimeWeight != 0.0)
            info.put("Affected students [time]", Double.valueOf(weighted ? iAffectedStudentByTimeWeight * affectedStudentsByTime : affectedStudentsByTime));
        if (includeZero || iAffectedInstructorByTimeWeight != 0.0)
            info.put("Affected instructors [time]", Double.valueOf(weighted ? iAffectedInstructorByTimeWeight * affectedInstructorsByTime : affectedInstructorsByTime));
        if (includeZero || iAffectedStudentByRoomWeight != 0.0)
            info.put("Affected students [room]", Double.valueOf(weighted ? iAffectedStudentByRoomWeight * affectedStudentsByRoom : affectedStudentsByRoom));
        if (includeZero || iAffectedInstructorByRoomWeight != 0.0)
            info.put("Affected instructors [room]", Double.valueOf(weighted ? iAffectedInstructorByRoomWeight * affectedInstructorsByRoom : affectedInstructorsByRoom));
        if (includeZero || iAffectedStudentByBldgWeight != 0.0)
            info.put("Affected students [bldg]", Double.valueOf(weighted ? iAffectedStudentByBldgWeight * affectedStudentsByBldg : affectedStudentsByBldg));
        if (includeZero || iAffectedInstructorByBldgWeight != 0.0)
            info.put("Affected instructors [bldg]", Double.valueOf(weighted ? iAffectedInstructorByBldgWeight * affectedInstructorsByBldg : affectedInstructorsByBldg));
        if (includeZero || iDifferentRoomWeight != 0.0)
            info.put("Different room", Double.valueOf(weighted ? iDifferentRoomWeight * differentRoom : differentRoom));
        if (includeZero || iDifferentBuildingWeight != 0.0)
            info.put("Different building", Double.valueOf(weighted ? iDifferentBuildingWeight * differentBuilding : differentBuilding));
        if (includeZero || iDifferentTimeWeight != 0.0)
            info.put("Different time", Double.valueOf(weighted ? iDifferentTimeWeight * differentTime : differentTime));
        if (includeZero || iDifferentDayWeight != 0.0)
            info.put("Different day", Double.valueOf(weighted ? iDifferentDayWeight * differentDay : differentDay));
        if (includeZero || iDifferentHourWeight != 0.0)
            info.put("Different hour", Double.valueOf(weighted ? iDifferentHourWeight * differentHour : differentHour));
        if (includeZero || iTooFarForInstructorsWeight != 0.0)
            info.put("New placement too far for initial [instructors]", Double.valueOf(weighted ? iTooFarForInstructorsWeight * tooFarForInstructors : tooFarForInstructors));
        if (includeZero || iTooFarForStudentsWeight != 0.0)
            info.put("New placement too far for initial [students]", Double.valueOf(weighted ? iTooFarForStudentsWeight * tooFarForStudents : tooFarForStudents));
        if (includeZero || iDeltaStudentConflictsWeight != 0.0)
            info.put("Delta student conflicts", Double.valueOf(weighted ? iDeltaStudentConflictsWeight * deltaStudentConflicts : deltaStudentConflicts));
        if (includeZero || iNewStudentConflictsWeight != 0.0)
            info.put("New student conflicts", Double.valueOf(weighted ? iNewStudentConflictsWeight * newStudentConflicts : newStudentConflicts));
        if (includeZero || iDeltaTimePreferenceWeight != 0.0)
            info.put("Delta time preferences", Double.valueOf(weighted ? iDeltaTimePreferenceWeight * deltaTimePreferences : deltaTimePreferences));
        if (includeZero || iDeltaRoomPreferenceWeight != 0.0)
            info.put("Delta room preferences", Double.valueOf(weighted ? iDeltaRoomPreferenceWeight * deltaRoomPreferences : deltaRoomPreferences));
        if (includeZero || iDeltaInstructorDistancePreferenceWeight != 0.0)
            info.put("Delta instructor distance preferences", Double.valueOf(weighted ? iDeltaInstructorDistancePreferenceWeight * deltaInstructorDistancePreferences : deltaInstructorDistancePreferences));
        return info;
    }
}
