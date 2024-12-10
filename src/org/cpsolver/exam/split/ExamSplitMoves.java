package org.cpsolver.exam.split;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.logging.log4j.Logger;
import org.cpsolver.exam.criteria.RoomPenalty;
import org.cpsolver.exam.criteria.RoomSizePenalty;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPeriodPlacement;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

/**
 * Experimental neighbor selection that allows an exam to be split
 * into two if it decreases the number of student conflicts.
 * <br><br>
 * An examination split is improving (and is considered) if the weighted
 * number of student conflicts that will be removed by the split is bigger 
 * than the weight of the splitter criterion {@link ExamSplitter#getWeight()}.
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
public class ExamSplitMoves implements NeighbourSelection<Exam, ExamPlacement> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ExamSplitMoves.class);
    private ExamSplitter iSplitter = null;

    /** Constructor 
     * @param properties solver configuration
     **/
    public ExamSplitMoves(DataProperties properties) {}

    /** Initialization */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        iSplitter = (ExamSplitter)solver.currentSolution().getModel().getCriterion(ExamSplitter.class);
        if (iSplitter == null) throw new RuntimeException("ExamSplitter criterion needs to be used as well.");
    }
    
    /**
     * Find best available rooms for a new exam (that is to be split from the given one),
     * if is is assigned into the given examination period.
     * 
     * @param assignment current assignment
     * @param exam an exam to be split
     * @param period a period to be assigned to the new exam
     * @param examSize size of the new exam (i.e., the number of students that will be moved from the given exam to the new one)
     * @return best room placement for the given exam and period 
     */
    public Set<ExamRoomPlacement> findBestAvailableRooms(Assignment<Exam, ExamPlacement> assignment, Exam exam, ExamPeriodPlacement period, int examSize) {
        if (exam.getMaxRooms() == 0) return new HashSet<ExamRoomPlacement>();
        double sw = exam.getModel().getCriterion(RoomSizePenalty.class).getWeight();
        double pw = exam.getModel().getCriterion(RoomPenalty.class).getWeight();
        loop: for (int nrRooms = 1; nrRooms <= exam.getMaxRooms(); nrRooms++) {
            HashSet<ExamRoomPlacement> rooms = new HashSet<ExamRoomPlacement>();
            int size = 0;
            while (rooms.size() < nrRooms && size < examSize) {
                int minSize = (examSize - size) / (nrRooms - rooms.size());
                ExamRoomPlacement best = null;
                double bestWeight = 0;
                int bestSize = 0;
                for (ExamRoomPlacement room : exam.getRoomPlacements()) {
                    if (!room.isAvailable(period.getPeriod()))
                        continue;
                    if (!room.getRoom().getPlacements(assignment, period.getPeriod()).isEmpty())
                        continue;
                    if (rooms.contains(room))
                        continue;
                    int s = room.getSize(exam.hasAltSeating());
                    if (s < minSize)
                        break;
                    int p = room.getPenalty(period.getPeriod());
                    double w = pw * p + sw * (s - minSize);
                    double d = 0;
                    if (!rooms.isEmpty()) {
                        for (ExamRoomPlacement r : rooms) {
                            d += r.getDistanceInMeters(room);
                        }
                        w += d / rooms.size();
                    }
                    if (best == null || bestWeight > w) {
                        best = room;
                        bestSize = s;
                        bestWeight = w;
                    }
                }
                if (best == null)
                    continue loop;
                rooms.add(best);
                size += bestSize;
            }
            if (size >= examSize)
                return rooms;
        }
        return null;
    }
    
    /**
     * Find a best split for the given exam. Only improving neighbors are considered. 
     * @param assignment current assignment
     * @param exam an exam to be split
     * @return best neighbor that will do the split
     */
    public ExamSplitNeighbour bestSplit(Assignment<Exam, ExamPlacement> assignment, Exam exam) {
        ExamSplitNeighbour split = null;
        ExamPlacement placement = assignment.getValue(exam);
        int px = ToolBox.random(exam.getPeriodPlacements().size());
        for (int p = 0; p < exam.getPeriodPlacements().size(); p++) { // Iterate over possible periods
            ExamPeriodPlacement period = exam.getPeriodPlacements().get((p + px) % exam.getPeriodPlacements().size());
            if (placement != null && placement.getPeriod().equals(period.getPeriod())) continue;
            // Try to create a neighbor
            ExamSplitNeighbour s = new ExamSplitNeighbour(assignment, exam, new ExamPlacement(exam, period, null));
            if (split == null || s.value(assignment) < split.value(assignment)) {
                // If improving, try to find available rooms
                Set<ExamRoomPlacement> rooms = findBestAvailableRooms(assignment, exam, period, s.nrStudents());
                if (rooms != null) {
                    // Remember as best split
                    s.placement().getRoomPlacements().addAll(rooms);
                    split = s;
                }
            }
        }
        return split;
    }

    /**
     * Select a split (split an exam into two), a merge (merge two split exams back together) or 
     * shuffle operation (move students between two exams that has been split before).
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        // Randomly select an exam
        Exam exam = ToolBox.random(solution.getAssignment().assignedVariables());
        Assignment<Exam, ExamPlacement> assignment = solution.getAssignment();
        
        // Parent exam (its either the exam itself, or its parent if it has been already split)
        Exam parent = iSplitter.parent(exam);
        // Its children (if already split)
        List<Exam> children = iSplitter.children(parent);
        
        // Already split -> try shuffle
        if (children != null && !children.isEmpty()) {
            ExamShuffleNeighbour shuffle = new ExamShuffleNeighbour(assignment, exam);
            if (shuffle.value(assignment) < 0.0) return shuffle;
        }
        
        // Can split -> try a split
        if (iSplitter.canSplit(exam)) {
            ExamSplitNeighbour split = bestSplit(solution.getAssignment(), exam);
            if (split != null && split.value(assignment) < 0.0) return split;
        }
        
        // Can merge -> try to merge
        if (iSplitter.canMerge(exam)) {
            ExamMergeNeighbour merge = new ExamMergeNeighbour(assignment, exam);
            if (merge.value(assignment) < 0.0) return merge;
        }

        return null;
    }
    
    /**
     * Split an exam into two
     */
    protected class ExamSplitNeighbour implements Neighbour<Exam, ExamPlacement> {
        private Exam iExam;
        private ExamPlacement iPlacement;
        private double iValue = 0.0;
        private int iNrStudents = 0;
        
        /**
         * Split an exam into two, assign the new exam into the given placement.
         * @param assignment current assignment
         * @param exam an exam to be split
         * @param placement a placement to be assigned to the new exam
         */
        public ExamSplitNeighbour(Assignment<Exam, ExamPlacement> assignment, Exam exam, ExamPlacement placement) {
            iExam = exam;
            iPlacement = placement;
            
            // Parent exam (its either the exam itself, or its parent if it has been already split)
            Exam parent = iSplitter.parent(exam);
            // Its children (if already split)
            List<Exam> children = iSplitter.children(parent);
            
            // Compute improvement
            // Consider moving all students of the parent exam to the new placement
            for (ExamStudent student: parent.getStudents()) {
                double delta = iSplitter.delta(assignment, student, assignment.getValue(parent), placement);
                if (delta < 0.0) {
                    iValue += delta;
                    iNrStudents ++;
                }
            }
            // If there already are other children, consider moving students of these children to the
            // new placement as well
            if (children != null)
                for (Exam child: children)
                    for (ExamStudent student: child.getStudents()) {
                        double delta = iSplitter.delta(assignment, student, assignment.getValue(child), placement);
                        if (delta < 0.0) {
                            iValue += delta;
                            iNrStudents ++;
                        }
                    }
            
            // Increase the weight by the splitter criterion weight
            iValue += iSplitter.getWeight();
        }

        /**
         * Perform the split.
         */
        @Override
        public void assign(Assignment<Exam, ExamPlacement> assignment, long iteration) {
            sLog.info("Splitting " + iExam.getName() + " (" + assignment.getValue(iExam).getName() + ", " + iPlacement.getName() + ", value: " + iValue + ")");
            iSplitter.split(assignment, iExam, iteration, iPlacement);
        }

        /**
         * Value of the split. This is the weight of the splitter criterion minus the weighted sum of
         * all student conflicts that will be removed by the split.
         */
        @Override
        public double value(Assignment<Exam, ExamPlacement> assignment) {
            return iValue;
        }

        /**
         * Number of students that will be moved into the new exam.
         * @return number of students
         */
        public int nrStudents() {
            return iNrStudents;
        }

        /**
         * Exam to be split.
         * @return exam to be split
         */
        public Exam exam() {
            return iExam;
        }

        /**
         * Placement of the new exam.
         * @return placement of the new exam
         */
        public ExamPlacement placement() {
            return iPlacement;
        }

        @Override
        public Map<Exam, ExamPlacement> assignments() {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Merge two exams that have been split before back into one. This moves
     * the students from the child exam back to its parent and removes the
     * child exam from the problem.
     */
    protected class ExamMergeNeighbour implements Neighbour<Exam, ExamPlacement> {
        private Exam iExam;
        private double iValue = 0.0;
        
        /**
         * Child exam to be removed. 
         * @param assignment current assignment
         * @param exam child exam to be merged back 
         */
        public ExamMergeNeighbour(Assignment<Exam, ExamPlacement> assignment, Exam exam) {
            iExam = exam;
            
            // Parent exam (its either the exam itself, or its parent if it has been already split)
            Exam parent = iSplitter.parent(exam);
            // Its children (if already split)
            List<Exam> children = iSplitter.children(parent);

            // Compute improvement
            for (ExamStudent student: exam.getStudents()) {
                // Try to move each student either back to the parent exam or to any of the other
                // children exams, if there are any
                double delta = iSplitter.delta(assignment, student, assignment.getValue(exam), assignment.getValue(parent));
                for (Exam child: children) {
                    if (child.equals(exam)) continue;
                    double d = iSplitter.delta(assignment, student, assignment.getValue(exam), assignment.getValue(child));
                    if (d < delta) delta = d;
                }
                iValue += delta;
            }
            // Decrease the weight by the splitter criterion weight
            iValue -= iSplitter.getWeight();
        }

        /**
         * Perform the merge.
         */        
        @Override
        public void assign(Assignment<Exam, ExamPlacement> assignment, long iteration) {
            sLog.info("Mergning " + iExam.getName() + " (" + assignment.getValue(iExam).getName() + ", value: " + iValue + ")");
            iSplitter.merge(assignment, iExam, iteration);
        }

        /**
         * Value of the merge. This is the weighted sum of all student conflicts that will be added by the merge,
         * minus the weight of the splitter criterion.
         */
        @Override
        public double value(Assignment<Exam, ExamPlacement> assignment) {
            return iValue;
        }
        
        /**
         * Number of students that will be moved back to the parent exam or to some other child (if there are any).
         * @return number of students
         */
        public int nrStudents() {
            return iExam.getStudents().size();
        }

        /**
         * Exam to be merged.
         * @return exam to be merged
         */
        public Exam exam() {
            return iExam;
        }

        @Override
        public Map<Exam, ExamPlacement> assignments() {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Shuffle students between the parent exam and all of its children. Only swaps
     * that are decreasing the weighted sum of student conflicts are considered.
     */
    protected class ExamShuffleNeighbour implements Neighbour<Exam, ExamPlacement> {
        private Exam iExam;
        private double iValue = 0.0;
        
        /**
         * Exam to be shuffled.
         * @param assignment current exam
         * @param exam child exam to be shuffled
         */
        public ExamShuffleNeighbour(Assignment<Exam, ExamPlacement> assignment, Exam exam) {
            iExam = exam;

            // Parent exam (its either the exam itself, or its parent if it has been already split)
            Exam parent = iSplitter.parent(exam);
            // Its children (if already split)
            List<Exam> children = iSplitter.children(parent);

            // Compute improvement
            // Try moving students away from parent
            for (ExamStudent student: parent.getStudents()) {
                Double delta = null;
                for (Exam x: children) {
                    double d = iSplitter.delta(assignment, student, assignment.getValue(parent), assignment.getValue(x));
                    if (delta == null || d < delta) delta = d;
                }
                if (delta != null && delta < 0) iValue += delta;
            }
            
            // Try moving students away from any child
            for (Exam child: children) {
                for (ExamStudent student: child.getStudents()) {
                    double delta = iSplitter.delta(assignment, student, assignment.getValue(child), assignment.getValue(parent));
                    for (Exam x: children) {
                        if (x.equals(child)) continue;
                        double d = iSplitter.delta(assignment, student, assignment.getValue(child), assignment.getValue(x));
                        if (d < delta) delta = d;
                    }
                    if (delta < 0) iValue += delta;
                }
            }
        }

        /**
         * Perform the shuffle.
         */        
        @Override
        public void assign(Assignment<Exam, ExamPlacement> assignment, long iteration) {
            sLog.info("Shuffling " + iExam.getName() + " (" + assignment.getValue(iExam).getName() + ", value: " + iValue + ")");
            iSplitter.shuffle(assignment, iExam, iteration);
        }

        /**
         * Value of the shuffle. This is the weighted sum of all student conflicts that will be removed by the shuffle.
         */
        @Override
        public double value(Assignment<Exam, ExamPlacement> assignment) {
            return iValue;
        }
        
        /**
         * Exam to be shuffled.
         * @return exam to be shuffled
         */
        public Exam exam() {
            return iExam;
        }

        @Override
        public Map<Exam, ExamPlacement> assignments() {
            throw new UnsupportedOperationException();
        }
    }
}
