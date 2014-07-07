package net.sf.cpsolver.coursett.criteria.additional;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.criteria.AbstractCriterion;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * The class represents various criteria concerning compact timetables of
 * instructors. The criteria are checked and updated when a variable is
 * (un)assigned.
 * <br>
 * implemented criterion: lunch break
 * <br>
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2012 Matej Lukac<br>
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
public class InstructorLunchBreak extends AbstractCriterion<Lecture, Placement> {
    // lunch attributes
    private double iMultiplier;
    private int iLunchStart, iLunchEnd, iLunchLength;
    private boolean iFullInfo;
    private List<BitSet> iWeeks = null;
    
    private Map<InstructorConstraint, CompactInfo> iCompactInfos = new HashMap<InstructorConstraint, CompactInfo>();

    public InstructorLunchBreak() {
        iValueUpdateType = ValueUpdateType.NoUpdate;
    }

    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        super.init(solver);

        iWeight = solver.getProperties().getPropertyDouble("InstructorLunch.Weight", 0.3d);

        // lunch parameters
        iLunchStart = solver.getProperties().getPropertyInt("InstructorLunch.StartSlot", (11 * 60) / 5);
        iLunchEnd = solver.getProperties().getPropertyInt("InstructorLunch.EndSlot", (13 * 60 + 30) / 5);
        iLunchLength = solver.getProperties().getPropertyInt("InstructorLunch.Length", 30 / 5);
        iMultiplier = solver.getProperties().getPropertyDouble("InstructorLunch.Multiplier", 1.2d);
        iFullInfo = solver.getProperties().getPropertyBoolean("InstructorLunch.InfoShowViolations", false);

        return true;
    }
    
    /**
     * Get compact info that is associated with an instructor constraint.
     * Create a new one if none has been created yet.
     */
    protected CompactInfo getCompactInfo(InstructorConstraint constraint) {
        CompactInfo info = iCompactInfos.get(constraint);
        if (info == null) {
            info = new CompactInfo();
            iCompactInfos.put(constraint, info);
        }
        return info;
    }    
    
    /**
     * Update criterion after an assignment.
     */
    @Override
    public void afterAssigned(long iteration, Placement value) {
        super.afterAssigned(iteration, value);
        for (InstructorConstraint constraint: value.variable().getInstructorConstraints())
            updateCriterion(constraint, value);
    }

    /**
     * Update criterion after an unassignment
     */
    @Override
    public void afterUnassigned(long iteration, Placement value) {
        super.afterUnassigned(iteration, value);
        for (InstructorConstraint constraint: value.variable().getInstructorConstraints())
            updateCriterion(constraint, value);
    }

    /**
     * The method creates date patterns (bitsets) which represent the weeks of a
     * semester.
     * 
     * @return a list of BitSets which represents the weeks of a semester.
     */
    protected List<BitSet> getWeeks() {
        if (iWeeks == null) {
            TimetableModel model = (TimetableModel) getModel();
            iWeeks = model.getWeeks();
        }
        return iWeeks;            
    }

    /**
     * Method updates number of violations in days (Mo, Tue, Wed,..) considering
     * each week in the semester separately. The current number of violations
     * for a day is stored in the CompactInfo.lunchDayViolations of the
     * constraint, which must be set properly before the calling of the method.
     * 
     * @param constraint
     *            the Instructor constraint of an instructor checked for a lunch
     *            break
     * @param p
     *            placement of a lecture currently (un)assigned
     */
    public void updateLunchPenalty(InstructorConstraint constraint, Placement p) {
        // checks only placements in the lunch time
        if (p.getTimeLocation().getStartSlot() <= iLunchEnd && p.getTimeLocation().getStartSlot() + p.getTimeLocation().getLength() > iLunchStart) {
            CompactInfo compactInfo = getCompactInfo(constraint);
            for (int i = 0; i < Constants.NR_DAYS; i++) {
                // checks only days affected by the placement
                if ((p.getTimeLocation().getDayCode() & Constants.DAY_CODES[i]) != 0) {
                    int currentLunchStartSlot = Constants.SLOTS_PER_DAY * i + iLunchStart;
                    int currentLunchEndSlot = Constants.SLOTS_PER_DAY * i + iLunchEnd;
                    int semesterViolations = 0;
                    for (BitSet week : getWeeks()) {
                        int maxBreak = 0;
                        int currentBreak = 0;
                        for (int slot = currentLunchStartSlot; slot < currentLunchEndSlot; slot++) {
                            if (constraint.getPlacements(slot, week).isEmpty()) {
                                currentBreak++;
                                if (maxBreak < currentBreak) {
                                    maxBreak = currentBreak;
                                }
                            } else {
                                currentBreak = 0;
                            }
                        }
                        if (maxBreak < iLunchLength) {
                            semesterViolations++;
                        }
                    }
                    // saving the result in the CompactInfo of the
                    // InstructorConstraint
                    compactInfo.getLunchDayViolations()[i] = semesterViolations;
                }
            }
        }
    }

    /**
     * Method checks or sets the CompactInfo of an InstructorConstraint. It
     * updates the preference of chosen criteria. The update consists of
     * decrementing the criterion value by previous preference, finding the
     * current preference and incrementing the criterion value by the current
     * preference.
     * 
     * @param instructorConstraint
     *            the Instructor constraint of an instructor checked for
     *            criteria
     * @param placement
     *            placement of a lecture currently (un)assigned
     */
    public void updateCriterion(InstructorConstraint instructorConstraint, Placement placement) {        
            iValue -= getLunchPreference(instructorConstraint);
            updateLunchPenalty(instructorConstraint, placement);
            iValue += getLunchPreference(instructorConstraint);       
    }
    
    /**
     * Method uses the CompactInfo of the InstructorConstraint and returns the
     * lunch preference for this constraint. Calculation formula does not use
     * linear function, the number of violations is multiplied by a power of
     * iMultiplier.
     * 
     * @param instructorConstraint
     *            the Instructor constraint of an instructor checked for a lunch
     *            break
     * @return the lunch preference for this constraint
     */
    private double getLunchPreference(InstructorConstraint instructorConstraint) {
        double violations = 0d;
        CompactInfo info = getCompactInfo(instructorConstraint);
        for (int i = 0; i < Constants.NR_DAYS; i++)
            violations += info.getLunchDayViolations()[i];
        return Math.pow(violations, iMultiplier); 
    }

    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        return iValue;
    }

    @Override
    public double getWeightedValue(Placement value, Set<Placement> conflicts) {
        return iValue * iWeight;
    }

    @Override
    public double getValue(Collection<Lecture> variables) {
        double lunchValue = 0.0d;
        Set<InstructorConstraint> constraints = new HashSet<InstructorConstraint>();
        for (Lecture lecture : variables) {
            constraints.addAll(lecture.getInstructorConstraints());
        }
        for (InstructorConstraint instructor : constraints) {
            lunchValue += getLunchPreference(instructor);
        }
        return lunchValue;
    }

    @Override
    public void getInfo(Map<String, String> info) {
        Set<String> violatedLunchBreaks = new TreeSet<String>();
        int lunchViolations = 0;
        for (InstructorConstraint c : ((TimetableModel)getModel()).getInstructorConstraints()) {
            String days = "";
            CompactInfo compactInfo = getCompactInfo(c);
            for (int i = 0; i < Constants.NR_DAYS; i++) {
                if (compactInfo.getLunchDayViolations()[i] > 0) {
                    if (iFullInfo)
                        days += (days.isEmpty() ? "" : ", ") + compactInfo.getLunchDayViolations()[i] + " &times; " + Constants.DAY_NAMES_SHORT[i];
                    lunchViolations += compactInfo.getLunchDayViolations()[i];
                }
            }
            if (iFullInfo && !days.isEmpty())
                violatedLunchBreaks.add(c.getName() + ": " + days);
        }
        if (lunchViolations > 0) {
            info.put("Lunch breaks", getPerc(lunchViolations, 0, ((TimetableModel)getModel()).getInstructorConstraints().size() * Constants.NR_DAYS * getWeeks().size()) + "% (" + lunchViolations + ")");
            if (iFullInfo && !violatedLunchBreaks.isEmpty()) {
                String message = "";
                for (String s: violatedLunchBreaks)
                    message += (message.isEmpty() ? "" : "<br>") + s;
                info.put("Lunch break violations", message);
            }
        }
    }

    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
        Set<InstructorConstraint> constraints = new HashSet<InstructorConstraint>();
        for (Lecture lecture : variables) {
            for (InstructorConstraint c : lecture.getInstructorConstraints()) {
                constraints.add(c);
            }
        }
        Set<String> violatedLunchBreaks = new TreeSet<String>();
        int lunchViolations = 0;
        for (InstructorConstraint c : constraints) {
            String days = "";
            CompactInfo compactInfo = getCompactInfo(c);
            for (int i = 0; i < Constants.NR_DAYS; i++) {
                if (compactInfo.getLunchDayViolations()[i] > 0) {
                    if (iFullInfo)
                        days += (days.isEmpty() ? "" : ", ") + compactInfo.getLunchDayViolations()[i] + " &times; " + Constants.DAY_NAMES_SHORT[i];
                    lunchViolations += compactInfo.getLunchDayViolations()[i];
                }
            }
            if (iFullInfo && !days.isEmpty())
                violatedLunchBreaks.add(c.getName() + ": " + days);
        }
        if (lunchViolations > 0) {
            info.put("Lunch breaks", getPerc(lunchViolations, 0, constraints.size() * Constants.NR_DAYS * getWeeks().size()) + "% (" + lunchViolations + ")");
            if (iFullInfo && !violatedLunchBreaks.isEmpty()) {
                String message = "";
                for (String s: violatedLunchBreaks)
                    message += (message.isEmpty() ? "" : "; ") + s;
                info.put("Lunch break violations", message);
            }
        }
    }
    
    /**
     * The class is used as a container of information concerning lunch break
     * of instructors. It is designed as an attribute of an
     * InstructorConstraint.
     */
    public static class CompactInfo {
        // lunch attributes
        private int[] iLunchDayViolations = new int[Constants.NR_DAYS];

        public CompactInfo() {
        }
        
        public int[] getLunchDayViolations() { return iLunchDayViolations; }
    }
}
