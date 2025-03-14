package org.cpsolver.coursett.constraint;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.SameSubpartBalancingPenalty;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Spread given set of classes in time as much as possible. See
 * {@link DepartmentSpreadConstraint} for more details.
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

public class SpreadConstraint extends ConstraintWithContext<Lecture, Placement, SpreadConstraint.SpreadConstraintContext> implements WeakeningConstraint<Lecture, Placement> {
    private boolean iInteractive = false;
    private double iSpreadFactor = 1.20;
    private int iUnassignmentsToWeaken = 250;
    private String iName = null;
    private int iFirstDaySlot, iLastDaySlot, iFirstWorkDay, iLastWorkDay;

    public static boolean USE_MOST_IMPROVEMENT_ADEPTS = false;

    public SpreadConstraint(String name, double spreadFactor, int unassignmentsToWeaken, boolean interactiveMode, int firstDaySlot, int lastDaySlot, int firstWorkDay, int lastWorkDay) {
        iName = name;
        iSpreadFactor = spreadFactor;
        iUnassignmentsToWeaken = unassignmentsToWeaken;
        iInteractive = interactiveMode;
        iFirstDaySlot = firstDaySlot;
        iLastDaySlot = lastDaySlot;
        iFirstWorkDay = firstWorkDay;
        iLastWorkDay = lastWorkDay;
        if (iLastWorkDay < iFirstWorkDay) iLastWorkDay += 7;
    }

    public SpreadConstraint(DataProperties config, String name) {
        this(name,
                config.getPropertyDouble("Spread.SpreadFactor", 1.20),
                config.getPropertyInt("Spread.Unassignments2Weaken", 250),
                config.getPropertyBoolean("General.InteractiveMode", false),
                config.getPropertyInt("General.FirstDaySlot", Constants.DAY_SLOTS_FIRST),
                config.getPropertyInt("General.LastDaySlot", Constants.DAY_SLOTS_LAST),
                config.getPropertyInt("General.FirstWorkDay", 0),
                config.getPropertyInt("General.LastWorkDay", Constants.NR_DAYS_WEEK - 1)
                );
    }
    
    
    protected Criterion<Lecture, Placement> getCriterion() {
        return getModel().getCriterion(SameSubpartBalancingPenalty.class);
    }

    public Placement getAdept(Assignment<Lecture, Placement> assignment, Placement placement, int[][] nrCourses, Set<Placement> conflicts) {
        Placement adept = null;
        int improvement = 0;

        // take uncommitted placements first
        for (Lecture lect : variables()) {
            if (lect.isCommitted())
                continue;
            Placement plac = assignment.getValue(lect);
            if (plac == null || plac.equals(placement) || placement.variable().equals(plac.variable())
                    || conflicts.contains(plac))
                continue;
            int imp = getPenaltyIfUnassigned(assignment, plac, nrCourses);
            if (imp == 0)
                continue;
            if (adept == null || imp > improvement) {
                adept = plac;
                improvement = imp;
            }
        }
        if (adept != null)
            return adept;

        // no uncommitted placement found -- take committed one
        for (Lecture lect : variables()) {
            if (!lect.isCommitted())
                continue;
            Placement plac = assignment.getValue(lect);
            if (plac == null || plac.equals(placement) || conflicts.contains(plac))
                continue;
            int imp = getPenaltyIfUnassigned(assignment, plac, nrCourses);
            if (imp == 0)
                continue;
            if (adept == null || imp > improvement) {
                adept = plac;
                improvement = imp;
            }
        }

        return adept;
    }

    @SuppressWarnings("unchecked")
    private Set<Placement>[] getAdepts(Assignment<Lecture, Placement> assignment, Placement placement, int[][] nrCourses, Set<Placement> conflicts) {
        SpreadConstraintContext context = getContext(assignment);
        int firstSlot = placement.getTimeLocation().getStartSlot();
        if (firstSlot > iLastDaySlot)
            return null;
        int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
        if (endSlot < iFirstDaySlot)
            return null;
        HashSet<Placement> adepts[] = new HashSet[] { new HashSet<Placement>(), new HashSet<Placement>() };
        for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
            for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                int dayCode = Constants.DAY_CODES[j % 7];
                if ((dayCode & placement.getTimeLocation().getDayCode()) != 0 && nrCourses[i - iFirstDaySlot][j - iFirstWorkDay] >= context.getMaxCourses(i, j)) {
                    for (Placement p : context.getCourses(i, j)) {
                        if (conflicts.contains(p))
                            continue;
                        if (p.equals(placement))
                            continue;
                        if (p.variable().equals(placement.variable()))
                            continue;
                        adepts[(p.variable()).isCommitted() ? 1 : 0].add(p);
                    }
                }
            }
        }
        return adepts;
        // sLogger.debug("  -- adept "+adept+" selected, penalty will be decreased by "+improvement);
    }

    private int getPenaltyIfUnassigned(Assignment<Lecture, Placement> assignment, Placement placement, int[][] nrCourses) {
        SpreadConstraintContext context = getContext(assignment);
        int firstSlot = placement.getTimeLocation().getStartSlot();
        if (firstSlot > iLastDaySlot)
            return 0;
        int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
        if (endSlot < iFirstDaySlot)
            return 0;
        int penalty = 0;
        for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
            for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                int dayCode = Constants.DAY_CODES[j % 7];
                if ((dayCode & placement.getTimeLocation().getDayCode()) != 0 && nrCourses[i - iFirstDaySlot][j - iFirstWorkDay] > context.getMaxCourses(i, j))
                    penalty++;
            }
        }
        return penalty;
    }

    private int tryUnassign(Assignment<Lecture, Placement> assignment, Placement placement, int[][] nrCourses) {
        SpreadConstraintContext context = getContext(assignment);
        // sLogger.debug("  -- trying to unassign "+placement);
        int firstSlot = placement.getTimeLocation().getStartSlot();
        if (firstSlot > iLastDaySlot)
            return 0;
        int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
        if (endSlot < iFirstDaySlot)
            return 0;
        int improvement = 0;
        for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
            for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                int dayCode = Constants.DAY_CODES[j % 7];
                if ((dayCode & placement.getTimeLocation().getDayCode()) != 0) {
                    if (nrCourses[i - iFirstDaySlot][j - iFirstWorkDay] > context.getMaxCourses(i, j))
                        improvement++;
                    nrCourses[i - iFirstDaySlot][j - iFirstWorkDay]--;
                }
            }
        }
        // sLogger.debug("  -- penalty is decreased by "+improvement);
        return improvement;
    }

    private int tryAssign(Assignment<Lecture, Placement> assignment, Placement placement, int[][] nrCourses) {
        SpreadConstraintContext context = getContext(assignment);
        // sLogger.debug("  -- trying to assign "+placement);
        int firstSlot = placement.getTimeLocation().getStartSlot();
        if (firstSlot > iLastDaySlot)
            return 0;
        int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
        if (endSlot < iFirstDaySlot)
            return 0;
        int penalty = 0;
        for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
            for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                int dayCode = Constants.DAY_CODES[j % 7];
                if ((dayCode & placement.getTimeLocation().getDayCode()) != 0) {
                    nrCourses[i - iFirstDaySlot][j - iFirstWorkDay]++;
                    if (nrCourses[i - iFirstDaySlot][j - iFirstWorkDay] > context.getMaxCourses(i, j))
                        penalty++;
                }
            }
        }
        // sLogger.debug("  -- penalty is incremented by "+penalty);
        return penalty;
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement placement, Set<Placement> conflicts) {
        SpreadConstraintContext context = getContext(assignment);
        if (context.getUnassignmentsToWeaken() == 0)
            return;
        int penalty = context.getCurrentPenalty() + getPenalty(assignment, placement);
        if (penalty <= context.getMaxAllowedPenalty())
            return;
        int firstSlot = placement.getTimeLocation().getStartSlot();
        if (firstSlot > iLastDaySlot)
            return;
        int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
        if (endSlot < iFirstDaySlot)
            return;
        // sLogger.debug("-- computing conflict for value "+value+" ... (penalty="+iCurrentPenalty+", penalty with the value="+penalty+", max="+iMaxAllowedPenalty+")");
        int[][] nrCourses = new int[iLastDaySlot - iFirstDaySlot + 1][iLastWorkDay - iFirstWorkDay + 1];
        for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++)
            for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++)
                nrCourses[i][j] = context.getNrCourses(i + iFirstDaySlot, j + iFirstWorkDay, placement);
        tryAssign(assignment, placement, nrCourses);
        // sLogger.debug("  -- nrCurses="+fmt(nrCourses));
        for (Lecture lect : variables()) {
            if (lect.equals(placement.variable())) continue;
            Placement p = assignment.getValue(lect);
            if (p != null && conflicts.contains(p)) {
                penalty -= tryUnassign(assignment, p, nrCourses);
            }
            if (penalty <= context.getMaxAllowedPenalty())
                return;
        }
        if (USE_MOST_IMPROVEMENT_ADEPTS) {
            while (penalty > context.getMaxAllowedPenalty()) {
                Placement plac = getAdept(assignment, placement, nrCourses, conflicts);
                if (plac == null)
                    break;
                conflicts.add(plac);
                penalty -= tryUnassign(assignment, plac, nrCourses);
            }
        } else {
            if (penalty > context.getMaxAllowedPenalty()) {
                Set<Placement> adepts[] = getAdepts(assignment, placement, nrCourses, conflicts);
                for (int i = 0; penalty > context.getMaxAllowedPenalty() && i < adepts.length; i++) {
                    while (!adepts[i].isEmpty() && penalty > context.getMaxAllowedPenalty()) {
                        Placement plac = ToolBox.random(adepts[i]);
                        adepts[i].remove(plac);
                        conflicts.add(plac);
                        // sLogger.debug("  -- conflict "+lect.getAssignment()+" added");
                        penalty -= tryUnassign(assignment, plac, nrCourses);
                    }
                }
            }
        }
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
        SpreadConstraintContext context = getContext(assignment);
        if (context.getUnassignmentsToWeaken() == 0) return false;
        return getPenalty(assignment, placement) + context.getCurrentPenalty() > context.getMaxAllowedPenalty();
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment) {
        getContext(assignment).weaken();
    }

    @Override
    public String getName() {
        return iName;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Time Spread between ");
        for (Iterator<Lecture> e = variables().iterator(); e.hasNext();) {
            Lecture v = e.next();
            sb.append(v.getName());
            if (e.hasNext())
                sb.append(", ");
        }
        return sb.toString();
    }

    /** Department balancing penalty for this department 
     * @param assignment current assignment
     * @return current penalty
     **/
    public int getPenalty(Assignment<Lecture, Placement> assignment) {
        return getContext(assignment).getCurrentPenalty();
    }

    public int getPenaltyEstimate(Assignment<Lecture, Placement> assignment) {
        double histogramPerDay[][] = new double[iLastDaySlot - iFirstDaySlot + 1][iLastWorkDay - iFirstWorkDay + 1];
        int maxCourses[][] = new int[iLastDaySlot - iFirstDaySlot + 1][iLastWorkDay - iFirstWorkDay + 1];
        int nrCourses[][] = new int[iLastDaySlot - iFirstDaySlot + 1][iLastWorkDay - iFirstWorkDay + 1];
        for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++)
            for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++)
                histogramPerDay[i][j] = 0.0;
        int totalUsedSlots = 0;
        for (Lecture lecture : variables()) {
            List<Placement>  values = lecture.values(assignment);
            Placement firstPlacement = (values.isEmpty() ? null : values.get(0));
            if (firstPlacement != null) {
                totalUsedSlots += firstPlacement.getTimeLocation().getNrSlotsPerMeeting()
                        * firstPlacement.getTimeLocation().getNrMeetings();
            }
            for (Placement p : values) {
                int firstSlot = p.getTimeLocation().getStartSlot();
                if (firstSlot > iLastDaySlot)
                    continue;
                int endSlot = firstSlot + p.getTimeLocation().getNrSlotsPerMeeting() - 1;
                if (endSlot < iFirstDaySlot)
                    continue;
                for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
                    int dayCode = p.getTimeLocation().getDayCode();
                    for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                        if ((dayCode & Constants.DAY_CODES[j % 7]) != 0) {
                            histogramPerDay[i - iFirstDaySlot][j - iFirstWorkDay] += 1.0 / values.size();
                        }
                    }
                }
            }
        }
        double threshold = iSpreadFactor * ((double) totalUsedSlots / ((iLastWorkDay - iFirstWorkDay + 1) * (iLastDaySlot - iFirstDaySlot + 1)));
        for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++) {
            for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++) {
                nrCourses[i][j] = 0;
                maxCourses[i][j] = (int) (0.999 + (histogramPerDay[i][j] <= threshold ? iSpreadFactor * histogramPerDay[i][j] : histogramPerDay[i][j]));
            }
        }
        int currentPenalty = 0;
        for (Lecture lecture : variables()) {
            Placement placement = assignment.getValue(lecture);
            if (placement == null)
                continue;
            int firstSlot = placement.getTimeLocation().getStartSlot();
            if (firstSlot > iLastDaySlot)
                continue;
            int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
            if (endSlot < iFirstDaySlot)
                continue;
            for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
                for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                    int dayCode = Constants.DAY_CODES[j % 7];
                    if ((dayCode & placement.getTimeLocation().getDayCode()) != 0) {
                        nrCourses[i - iFirstDaySlot][j - iFirstWorkDay]++;
                    }
                }
            }
        }
        for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++) {
            for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++) {
                currentPenalty += Math.max(0, nrCourses[i][j] - maxCourses[i][j]);
            }
        }
        return currentPenalty;
    }

    public int getMaxPenalty(Assignment<Lecture, Placement> assignment, Placement placement) {
        SpreadConstraintContext context = getContext(assignment);
        int penalty = 0;
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            int day = slot / Constants.SLOTS_PER_DAY;
            int time = slot % Constants.SLOTS_PER_DAY;
            if (time < iFirstDaySlot || time > iLastDaySlot)
                continue;
            if (iLastWorkDay < 7) {
                if (day < iFirstWorkDay || day > iLastWorkDay)
                    continue;
            } else {
                if (day < iFirstWorkDay && day > iLastWorkDay - 7)
                    continue;
                if (day < iFirstWorkDay) day += 7;
            }
            int dif = 1 + context.getNrCourses(time, day, placement) - context.getMaxCourses(time, day);
            if (dif > penalty)
                penalty = dif;
        }
        return penalty;
    }

    /** Department balancing penalty of the given placement 
     * @param assignment current assignment
     * @param placement a placement that is being considered
     * @return change in the penalty if assigned
     **/
    public int getPenalty(Assignment<Lecture, Placement> assignment, Placement placement) {
        SpreadConstraintContext context = getContext(assignment);
        int firstSlot = placement.getTimeLocation().getStartSlot();
        if (firstSlot > iLastDaySlot)
            return 0;
        int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
        if (endSlot < iFirstDaySlot)
            return 0;
        int penalty = 0;
        int min = Math.max(firstSlot, iFirstDaySlot);
        int max = Math.min(endSlot, iLastDaySlot);
        for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
            int dayCode = Constants.DAY_CODES[j % 7];
            if ((dayCode & placement.getTimeLocation().getDayCode()) == 0)
                continue;
            for (int i = min; i <= max; i++) {
                if (context.getNrCourses(i, j, placement) >= context.getMaxCourses(i, j))
                    penalty++;
            }
        }
        return penalty;
    }

    @Override
    public void addVariable(Lecture lecture) {
        if (lecture.canShareRoom()) {
            for (GroupConstraint gc : lecture.groupConstraints()) {
                if (gc.getType() == GroupConstraint.ConstraintType.MEET_WITH) {
                    if (gc.variables().indexOf(lecture) > 0)
                        return;
                }
            }
        }
        super.addVariable(lecture);
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
        while (inConflict(assignment, value))
            getContext(assignment).weaken(getContext(assignment).getCurrentPenalty() + getPenalty(assignment, value));
    }
    
    @Override
    public SpreadConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new SpreadConstraintContext(assignment);
    }

    public class SpreadConstraintContext implements AssignmentConstraintContext<Lecture, Placement> {
        private int iMaxAllowedPenalty = 0;
        private long iUnassignment = 0;
        private Set<Placement>[][] iCourses = null;
        private int iMaxCourses[][] = null;
        private int iCurrentPenalty = 0;

        @SuppressWarnings("unchecked")
        public SpreadConstraintContext(Assignment<Lecture, Placement> assignment) {
            iCourses = new Set[iLastDaySlot - iFirstDaySlot + 1][(iLastWorkDay - iFirstWorkDay) % 7 + 1];
            if (iInteractive)
                iUnassignmentsToWeaken = 0;
            for (int i = 0; i < iCourses.length; i++) {
                for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++) {
                    iCourses[i][j] = new HashSet<Placement>(10);
                }
            }
            double histogramPerDay[][] = new double[iLastDaySlot - iFirstDaySlot + 1][iLastWorkDay - iFirstWorkDay + 1];
            iMaxCourses = new int[iLastDaySlot - iFirstDaySlot + 1][iLastWorkDay - iFirstWorkDay + 1];
            for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++)
                for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++)
                    histogramPerDay[i][j] = 0.0;
            int totalUsedSlots = 0;
            for (Lecture lecture : variables()) {
                List<Placement>  values = lecture.values(assignment);
                Placement firstPlacement = (values.isEmpty() ? null : values.get(0));
                if (firstPlacement != null) {
                    totalUsedSlots += firstPlacement.getTimeLocation().getNrSlotsPerMeeting() * firstPlacement.getTimeLocation().getNrMeetings();
                }
                for (Placement p : values) {
                    int firstSlot = p.getTimeLocation().getStartSlot();
                    if (firstSlot > iLastDaySlot)
                        continue;
                    int endSlot = firstSlot + p.getTimeLocation().getNrSlotsPerMeeting() - 1;
                    if (endSlot < iFirstDaySlot)
                        continue;
                    for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
                        int dayCode = p.getTimeLocation().getDayCode();
                        for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                            if ((dayCode & Constants.DAY_CODES[j % 7]) != 0) {
                                histogramPerDay[i - iFirstDaySlot][j - iFirstWorkDay] += 1.0 / values.size();
                            }
                        }
                    }
                }
            }
            // System.out.println("Histogram for department "+iDepartment+":");
            double threshold = iSpreadFactor * ((double) totalUsedSlots / ((iLastWorkDay - iFirstWorkDay + 1) * (iLastDaySlot - iFirstDaySlot + 1)));
            // System.out.println("Threshold["+iDepartment+"] = "+threshold);
            for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++) {
                // System.out.println("  "+fmt(i+1)+": "+fmt(histogramPerDay[i]));
                for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++) {
                    iMaxCourses[i][j] = (int) (0.999 + (histogramPerDay[i][j] <= threshold ? iSpreadFactor * histogramPerDay[i][j] : histogramPerDay[i][j]));
                }
            }
            for (Lecture lecture : variables()) {
                Placement placement = assignment.getValue(lecture);
                if (placement == null)
                    continue;
                int firstSlot = placement.getTimeLocation().getStartSlot();
                if (firstSlot > iLastDaySlot)
                    continue;
                int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
                if (endSlot < iFirstDaySlot)
                    continue;
                for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
                    for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                        int dayCode = Constants.DAY_CODES[j % 7];
                        if ((dayCode & placement.getTimeLocation().getDayCode()) != 0) {
                            iCourses[i - iFirstDaySlot][j - iFirstWorkDay].add(placement);
                        }
                    }
                }
            }
            iCurrentPenalty = 0;
            for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++) {
                for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++) {
                    iCurrentPenalty += Math.max(0, iCourses[i][j].size() - iMaxCourses[i][j]);
                }
            }
            iMaxAllowedPenalty = iCurrentPenalty;
            getCriterion().inc(assignment, iCurrentPenalty);
        }
        

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            int firstSlot = placement.getTimeLocation().getStartSlot();
            if (firstSlot > iLastDaySlot)
                return;
            int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
            if (endSlot < iFirstDaySlot)
                return;
            getCriterion().inc(assignment, -iCurrentPenalty);
            for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
                for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                    int dayCode = Constants.DAY_CODES[j % 7];
                    if ((dayCode & placement.getTimeLocation().getDayCode()) != 0) {
                        iCourses[i - iFirstDaySlot][j - iFirstWorkDay].add(placement);
                        if (iCourses[i - iFirstDaySlot][j - iFirstWorkDay].size() > iMaxCourses[i - iFirstDaySlot][j - iFirstWorkDay])
                            iCurrentPenalty++;
                    }
                }
            }
            getCriterion().inc(assignment, iCurrentPenalty);
        }

        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            int firstSlot = placement.getTimeLocation().getStartSlot();
            if (firstSlot > iLastDaySlot)
                return;
            int endSlot = firstSlot + placement.getTimeLocation().getNrSlotsPerMeeting() - 1;
            if (endSlot < iFirstDaySlot)
                return;
            getCriterion().inc(assignment, -iCurrentPenalty);
            for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
                for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                    int dayCode = Constants.DAY_CODES[j % 7];
                    if ((dayCode & placement.getTimeLocation().getDayCode()) != 0) {
                        if (iCourses[i - iFirstDaySlot][j - iFirstWorkDay].size() > iMaxCourses[i - iFirstDaySlot][j - iFirstWorkDay])
                            iCurrentPenalty--;
                        iCourses[i - iFirstDaySlot][j - iFirstWorkDay].remove(placement);
                    }
                }
            }
            getCriterion().inc(assignment, iCurrentPenalty);
        }
        
        public int[][] getMaxCourses() {
            return iMaxCourses;
        }

        public int getMaxCourses(int time, int day) {
            return iMaxCourses[time - iFirstDaySlot][day - iFirstWorkDay];
        }

        public int getNrCourses(int time, int day, Placement placement) {
            if (placement == null) return getCourses(time, day).size();
            int nrCourses = 0;
            for (Placement p: getCourses(time, day))
                if (!p.variable().equals(placement.variable())) 
                    nrCourses ++;
            return nrCourses;
        }
        
        public Set<Placement> getCourses(int time, int day) {
            return iCourses[time - iFirstDaySlot][day - iFirstWorkDay];
        }
        
        public int getUnassignmentsToWeaken() {
            return iUnassignmentsToWeaken;
        }
        
        public int getCurrentPenalty() {
            return iCurrentPenalty;
        }
        
        public int getMaxAllowedPenalty() {
            return iMaxAllowedPenalty;
        }
        
        public void weaken() {
            if (iUnassignmentsToWeaken == 0) return;
            iUnassignment++;
            if (iUnassignment % iUnassignmentsToWeaken == 0)
                iMaxAllowedPenalty++;
        }
        
        public void weaken(int penalty) {
            if (penalty > iMaxAllowedPenalty)
                iMaxAllowedPenalty = penalty; 
        }

    }
}
