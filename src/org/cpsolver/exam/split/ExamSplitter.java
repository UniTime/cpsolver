package org.cpsolver.exam.split;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.exam.criteria.ExamCriterion;
import org.cpsolver.exam.criteria.StudentBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentDirectConflicts;
import org.cpsolver.exam.criteria.StudentMoreThan2ADayConflicts;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Experimental criterion that allows an exam to be split
 * into two if it decreases the number of student conflicts.
 * <br><br>
 * An examination split is improving (and is considered) if the weighted
 * number of student conflicts that will be removed by the split is bigger 
 * than the weight of the splitter criterion {@link ExamSplitter#getWeight()}.
 * <br><br>
 * To enable examination splitting, following parameters needs to be set:
 * <ul>
 *      <li>HillClimber.AdditionalNeighbours=org.cpsolver.exam.split.ExamSplitMoves
 *      <li>GreatDeluge.AdditionalNeighbours=org.cpsolver.exam.split.ExamSplitMoves
 *      <li>Exams.AdditionalCriteria=org.cpsolver.exam.split.ExamSplitter
 *      <li>Exams.ExamSplitWeight=500
 * </ul>
 * The Exams.ExamSplitWeight represents the weight of a split. For instance, to
 * allow only splits that decrease the number of student direct conflicts,
 * half of the weight of a direct student conflict is a good value for this
 * weight. 
 * 
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamSplitter extends ExamCriterion {
    private long iLastSplitId = 0;
    private Map<Exam, List<Exam>> iChildren = new HashMap<Exam, List<Exam>>();
    private Map<Exam, Exam> iParent = new HashMap<Exam, Exam>();
    private Criterion<Exam, ExamPlacement> iStudentDirectConflicts, iStudentMoreThan2ADayConflicts, iStudentBackToBackConflicts;
    private double iValue = 0.0;
    
    private Map<Exam, List<ExamPlacement>> iBestSplit = null;
    
    /** Examination splitter criterion. */
    public ExamSplitter() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    /** Initialization */
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        boolean ret = super.init(solver);
        iStudentDirectConflicts = solver.currentSolution().getModel().getCriterion(StudentDirectConflicts.class);
        iStudentMoreThan2ADayConflicts = solver.currentSolution().getModel().getCriterion(StudentMoreThan2ADayConflicts.class);
        iStudentBackToBackConflicts = solver.currentSolution().getModel().getCriterion(StudentBackToBackConflicts.class);
        return ret;
    }
    
    /** Returns Exams.ExamSplitWeight */
    @Override
    public String getWeightName() {
        return "Exams.ExamSplitWeight";
    }
    
    /** Returns examSplitWeight */
    @Override
    public String getXmlWeightName() {
        return "examSplitWeight";
    }
    
    /** Returns half of a student direct conflict weight */
    @Override
    public double getWeightDefault(DataProperties config) {
        return (iStudentDirectConflicts != null ? iStudentDirectConflicts.getWeight() / 2 : 500.0);
    }
    
    private boolean isDayBreakBackToBack() {
        return ((StudentBackToBackConflicts)iStudentBackToBackConflicts).isDayBreakBackToBack();
    }
    
    /** True, if an exam can be split 
     * @param exam given exam
     * @return true if the given exam can be split
     **/
    public boolean canSplit(Exam exam) {
        if (iParent.containsKey(exam)) return false; // already split
        return true;
    }
    
    /**
     * Parent of an exam that has been split.
     * @param exam an exam in question
     * @return parent exam if the exam has been split, or the exam itself otherwise (each non-split exam is its own parent)
     */
    public Exam parent(Exam exam) {
        return (iParent.containsKey(exam) ? iParent.get(exam) : exam);
    }
    
    /**
     * Children exams of an exam that has been split. These are all the exams that the parent exam has been split into.
     * @param parent an exam in question
     * @return all children exams, or null of the exam has not been split yet
     */
    public List<Exam> children(Exam parent) {
        return iChildren.get(parent);
    }
    
    /**
     * Split an exam
     * @param assignment current assignment
     * @param parent an exam to be split
     * @param iteration solver iteration
     * @param placement placement of the new exam
     * @return new exam assigned to the given placement with students moved into it; null if the given exam cannot be split
     */
    public Exam split(Assignment<Exam, ExamPlacement> assignment, Exam parent, long iteration, ExamPlacement placement) {
        if (!canSplit(parent)) return null;

        // Create the child exam
        Exam child = new Exam(--iLastSplitId, parent.getName(), parent.getLength(), parent.hasAltSeating(), parent.getMaxRooms(), parent.getMinSize(), parent.getPeriodPlacements(), parent.getRoomPlacements());
        child.setSizeOverride(parent.getSizeOverride());
        child.setPrintOffset(parent.getPrintOffset());
        child.setAveragePeriod(parent.getAveragePeriod());
        child.getOwners().addAll(parent.getOwners());
        
        // Update the parent and children structures
        iParent.put(child, parent);
        List<Exam> children = iChildren.get(parent);
        if (children == null) {
            children = new ArrayList<Exam>();
            iChildren.put(parent, children);
        }
        children.add(child);
        iValue += 1.0;
        
        // Add into model
        parent.getModel().addVariable(child);
        for (ExamRoomPlacement room : child.getRoomPlacements()) 
            room.getRoom().addVariable(child);
        if (placement != null) assignment.assign(iteration, new ExamPlacement(child, placement.getPeriodPlacement(), placement.getRoomPlacements()));
        
        
        // Shuffle students between parent exam and its children
        shuffle(assignment, parent, iteration);

        // Return the new exam
        return child;
    }
    
    /** True, if the given exam can be merged (it has been split) 
     * @param exam given exam
     * @return true if the given exam can be merged back 
     **/
    public boolean canMerge(Exam exam) {
        if (!iParent.containsKey(exam)) return false; // not split
        return true;
    }
    
    /**
     * Merge an exam
     * @param assignment current assignment
     * @param child an exam to be merged
     * @param iteration solver iteration
     * @return parent exam of the exam that has been deleted; null if the given exam cannot be merged
     */
    public Exam merge(Assignment<Exam, ExamPlacement> assignment, Exam child, long iteration) {
        if (!canMerge(child)) return null;
        
        // Update the parent and children structures
        Exam parent = iParent.get(child);
        iParent.remove(child);
        List<Exam> children = iChildren.get(parent);
        children.remove(child);
        iValue -= 1.0;
        
        // Unassign parent and the given exam
        ExamPlacement parentPlacement = assignment.getValue(parent);
        if (parentPlacement != null) assignment.unassign(iteration, parent);
        if (assignment.getValue(child) != null) assignment.unassign(iteration, child);

        // Move students back from the given exam
        for (ExamStudent student: new ArrayList<ExamStudent>(child.getStudents())) {
            student.removeVariable(child);
            student.addVariable(parent);
        }
        
        // Remove the given exam from the model
        for (ExamRoomPlacement room : child.getRoomPlacements()) 
            room.getRoom().removeVariable(child);
        parent.getModel().removeVariable(child);
        
        // Assign parent exam back
        if (parentPlacement != null) assignment.assign(iteration, parentPlacement);
        
        
        // Shuffle students between parent exam and its remaining children
        shuffle(assignment, parent, iteration);
        
        // Return parent exam
        return parent;
    }
    
    /**
     * Difference in the total weighted student conflicts (including {@link StudentDirectConflicts},
     * {@link StudentMoreThan2ADayConflicts}, and {@link StudentBackToBackConflicts}) if a student
     * is moved from an exam with one placement into an exam with another placement.
     * @param assignment current assignment
     * @param student a student in question
     * @param oldPlacement placement of the exam in which the student is now
     * @param newPlacement placement of the exam into which the student would be moved
     * @return difference in the student conflict weight
     */
    public double delta(Assignment<Exam, ExamPlacement> assignment, ExamStudent student, ExamPlacement oldPlacement, ExamPlacement newPlacement) {
        double delta = 0;
        
        // Weights of removing student form the old placement 
        if (oldPlacement != null) {
            Exam exam = oldPlacement.variable();
            ExamPeriod period = oldPlacement.getPeriod();
            Set<Exam> examsThisPeriod = student.getExams(assignment, period);
            if (examsThisPeriod.size() > (examsThisPeriod.contains(exam) ? 1 : 0))
                delta -= iStudentDirectConflicts.getWeight(); // will remove a direct conflict
            ExamPeriod prev = period.prev();
            if (prev != null && (prev.getDay() == period.getDay() || isDayBreakBackToBack())) {
                Set<Exam> examsPrevPeriod = student.getExams(assignment, prev);
                if (examsPrevPeriod.size() > (examsPrevPeriod.contains(exam) ? 1 : 0))
                    delta -= iStudentBackToBackConflicts.getWeight(); // will remove a back-to-back conflict
            }
            ExamPeriod next = period.next();
            if (next != null && (next.getDay() == period.getDay() || isDayBreakBackToBack())) {
                Set<Exam> examsNextPeriod = student.getExams(assignment, next);
                if (examsNextPeriod.size() > (examsNextPeriod.contains(exam) ? 1 : 0))
                    delta -= iStudentBackToBackConflicts.getWeight(); // will remove a back-to-back conflict
            }
            Set<Exam> examsInADay = student.getExamsADay(assignment, period);
            if (examsInADay.size() > (examsInADay.contains(exam) ? 3 : 2))
                delta -= iStudentMoreThan2ADayConflicts.getWeight(); // will remove a more than 2 on a day conflict
        }
        
        // Weights of moving student into the new placement
        if (newPlacement != null) {
            Exam exam = newPlacement.variable();
            ExamPeriod period = newPlacement.getPeriod();
            Set<Exam> examsThisPeriod = student.getExams(assignment, period);
            if (examsThisPeriod.size() > (examsThisPeriod.contains(exam) ? 1 : 0))
                delta += iStudentDirectConflicts.getWeight(); // will add a direct conflict
            ExamPeriod prev = period.prev();
            if (prev != null && (prev.getDay() == period.getDay() || isDayBreakBackToBack())) {
                Set<Exam> examsPrevPeriod = student.getExams(assignment, prev);
                if (examsPrevPeriod.size() > (examsPrevPeriod.contains(exam) ? 1 : 0))
                    delta += iStudentBackToBackConflicts.getWeight(); // will add a back-to-back conflict
            }
            ExamPeriod next = period.next();
            if (next != null && (next.getDay() == period.getDay() || isDayBreakBackToBack())) {
                Set<Exam> examsNextPeriod = student.getExams(assignment, next);
                if (examsNextPeriod.size() > (examsNextPeriod.contains(exam) ? 1 : 0))
                    delta += iStudentBackToBackConflicts.getWeight(); // will add a back-to-back conflict
            }
            Set<Exam> examsInADay = student.getExamsADay(assignment, period);
            if (examsInADay.size() > (examsInADay.contains(exam) ? 3 : 2))
                delta += iStudentMoreThan2ADayConflicts.getWeight(); // will add a more than 2 on a day conflict
        }
        
        return delta;
    }
    
    /**
     * Shuffle students between the given exam and all the other exams in the split (if there are any).
     * Only moves between exams that improve {@link ExamSplitter#delta(Assignment, ExamStudent, ExamPlacement, ExamPlacement)} are
     * considered.
     * @param assignment current assignment
     * @param exam an exam in question
     * @param iteration solver iteration
     */
    public void shuffle(Assignment<Exam, ExamPlacement> assignment, Exam exam, long iteration) {
        // Parent exam (its either the exam itself, or its parent if it has been already split)
        Exam parent = (iParent.containsKey(exam) ? iParent.get(exam) : exam);
        // Its children (if already split)
        List<Exam> children = iChildren.get(parent);
        
        if (children != null && !children.isEmpty()) {
            // Unassign all involved exams
            Map<Exam, ExamPlacement> assignments = new HashMap<Exam, ExamPlacement>();
            if (assignment.getValue(parent) != null) {
                assignments.put(parent, assignment.getValue(parent));
                assignment.unassign(iteration, parent);
            }
            for (Exam child: children) {
                if (assignment.getValue(child) != null) {
                    assignments.put(child, assignment.getValue(child));
                    assignment.unassign(iteration, child);
                }
            }
            
            // Move away from parent
            for (ExamStudent student: new ArrayList<ExamStudent>(parent.getStudents())) {
                Exam child = null; double delta = 0;
                for (Exam x: children) {
                    double d = delta(assignment, student, assignments.get(parent), assignments.get(x));
                    if (child == null || d < delta) {
                        delta = d; child = x;
                    }
                }
                if (child != null && delta < 0) {
                    student.removeVariable(parent);
                    student.addVariable(child);
                }
            }
            
            // Move students away from a child
            for (Exam child: children) {
                for (ExamStudent student: new ArrayList<ExamStudent>(child.getStudents())) {
                    Exam other = parent; double delta = delta(assignment, student, assignments.get(child), assignments.get(parent));
                    for (Exam x: children) {
                        if (x.equals(child)) continue;
                        double d = delta(assignment, student, assignments.get(child), assignments.get(x));
                        if (d < delta) {
                            delta = d; other = x;
                        }
                    }
                    if (other != null && delta < 0) {
                        student.removeVariable(child);
                        student.addVariable(other);
                    }
                }
            }
            
            // Assign everything back
            ExamPlacement parentPlacement = assignments.get(parent);
            if (parentPlacement != null) assignment.assign(iteration, parentPlacement);
            for (Exam child: children) {
                ExamPlacement placement = assignments.get(child);
                if (placement != null) assignment.assign(iteration, placement);
            }
        }
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment) {
        return iValue;
    }

    /** Not used */
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value, Set<ExamPlacement> conflicts) {
        return 0.0;
    }
    
    /** Not used */
    @Override
    public double[] getBounds(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> exams) {
        return new double[] { 0.0, 0.0 };
    }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "XX:" + sDoubleFormat.format(getValue(assignment));
    }
    
    /** Lists the split */
    @Override
    public void getInfo(Assignment<Exam, ExamPlacement> assignment, Map<String, String> info) {
        if (!iChildren.isEmpty()) {
            int parents = 0;
            String split = "";
            for (Exam parent: new TreeSet<Exam>(iChildren.keySet())) {
                List<Exam> children = iChildren.get(parent);
                if (children.isEmpty()) continue;
                split += "\n  ";
                parents ++;
                split += parent.getName() + ": " + parent.getStudents().size() + " (" + (assignment.getValue(parent) == null ? "N/A" : assignment.getValue(parent).getPeriod()) + ")";
                for (Exam child: children)
                    split += " + " + child.getStudents().size() + " (" + (assignment.getValue(child) == null ? "N/A" : assignment.getValue(child).getPeriod()) + ")";
            }
            if (parents > 0)
                info.put("Examination Splits", parents + split);
        }
    }

    /** Best solution was saved, remember the current splits */
    @Override
    public void bestSaved(Assignment<Exam, ExamPlacement> assignment) {
        super.bestSaved(assignment);
        
        if (iBestSplit == null)
            iBestSplit = new Hashtable<Exam, List<ExamPlacement>>();
        else
            iBestSplit.clear();
        
        for (Map.Entry<Exam, List<Exam>> entry: iChildren.entrySet()) {
            Exam parent = entry.getKey();
            List<ExamPlacement> placements = new ArrayList<ExamPlacement>();
            for (Exam child: entry.getValue()) {
                if (assignment.getValue(child) != null)
                    placements.add(assignment.getValue(child));
            }
            if (!placements.isEmpty())
                iBestSplit.put(parent, placements);
        }
    }

    /** Best solution was restored, change the splits back to what it was in the best solution */
    @Override
    public void bestRestored(Assignment<Exam, ExamPlacement> assignment) {
        super.bestRestored(assignment);
        
        // Merge those that are not split
        for (Exam parent: new ArrayList<Exam>(iChildren.keySet())) {
            List<Exam> children = new ArrayList<Exam>(iChildren.get(parent));
            List<ExamPlacement> placements = iBestSplit.get(parent);
            for (int i = (placements == null ? 0 : placements.size()); i < children.size(); i++)
                merge(assignment, children.get(i), 0);
        }
        
        // Assign best placements to all children, create children if needed
        iValue = 0;
        for (Exam parent: iBestSplit.keySet()) {
            List<ExamPlacement> placements = iBestSplit.get(parent);
            for (int i = 0; i < placements.size(); i++) {
                List<Exam> children = iChildren.get(parent);
                if (children == null || children.size() <= i) { // create a child if needed
                    split(assignment, parent, 0, placements.get(i));
                } else { // otherwise, just assign the placement
                    assignment.assign(0l, new ExamPlacement(children.get(i), placements.get(i).getPeriodPlacement(), placements.get(i).getRoomPlacements()));
                }
            }
            iValue += placements.size();
        }
    }
}
