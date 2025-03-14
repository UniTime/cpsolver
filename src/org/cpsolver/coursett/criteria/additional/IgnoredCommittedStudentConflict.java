package org.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Ignored committed student conflicts. This criterion counts committed student conflicts (both overlapping and distance) between classes
 * which are connected by a {@link IgnoredStudentConflict} constraint. This criterion was created mostly for debugging
 * as these student conflicts are to be ignored.
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2013 - 2014 Tomas Muller<br>
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
public class IgnoredCommittedStudentConflict extends StudentConflict {

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.IgnoredCommitedStudentConflictWeight", 0.0);
    }

    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrIgnoredCommitedStudConfsWeight";
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && ignore(l1, l2) && committed(l1, l2);
    }

    public int countCommittedConflicts(Student student, Placement placement) {
        if (student.getCommitedPlacements() == null) return 0;
        int conflicts = 0;
        Lecture lecture = placement.variable();
        for (Placement commitedPlacement : student.getCommitedPlacements()) {
            Lecture commitedLecture = commitedPlacement.variable();
            if (lecture.getSchedulingSubpartId() != null && lecture.getSchedulingSubpartId().equals(commitedLecture.getSchedulingSubpartId())) continue;
            if (ignore(lecture, commitedLecture) && (overlaps(placement, commitedPlacement) || distance(getMetrics(), placement, commitedPlacement)))
                conflicts ++;
        }
        if (conflicts == 0) return 0;
        double w = student.getOfferingWeight((placement.variable()).getConfiguration());
        return (int) Math.round(student.avg(w, 1.0) * conflicts);
    }
    
    public double countCommittedConflicts(Placement placement) {
        double ret = 0;
        Lecture lecture = placement.variable();
        for (Student student : lecture.students()) {
            ret += countCommittedConflicts(student, placement);
        }
        return ret;
    }
        
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double[] bounds = super.getBounds(assignment, variables);
        for (Lecture lecture: variables) {
            Double max = null;
            for (Placement placement: lecture.values(assignment)) {
                if (max == null) { max = Double.valueOf(countCommittedConflicts(placement)); continue; }
                max = Math.max(max, countCommittedConflicts(placement));
            }
            if (max != null) bounds[0] += max;
        }
        return bounds;
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        double ret = super.getValue(assignment, value, conflicts);
        ret += countCommittedConflicts(value);
        if (iIncludeConflicts && conflicts != null)
            for (Placement conflict: conflicts)
                ret -= countCommittedConflicts(conflict);
        return ret;
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double ret = super.getValue(assignment, variables);
        for (Lecture lect: variables) {
            Placement plac = assignment.getValue(lect);
            if (plac != null)
                ret += countCommittedConflicts(plac);
        }
        return Math.round(ret);
    }
}
