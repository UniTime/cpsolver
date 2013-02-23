package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Set;

import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Ignored committed student conflicts. This criterion counts committed student conflicts (both overlapping and distance) between classes
 * which are connected by a {@link IgnoredStudentConflict} constraint. This criterion was created mostly for debugging
 * as these student conflicts are to be ignored.
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2013 Tomas Muller<br>
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
        return ignore(l1, l2) && committed(l1, l2); // only committed student conflicts
    }


    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return ignore(p1, p2) && committed(p1, p2) && super.inConflict(p1, p2);
    }
    
    public int countCommittedConflicts(Student student, Placement placement) {
        if (student.getCommitedPlacements() == null) return 0;
        int conflicts = 0;
        Lecture lecture = placement.variable();
        for (Placement commitedPlacement : student.getCommitedPlacements()) {
            Lecture commitedLecture = commitedPlacement.variable();
            if (lecture.getSchedulingSubpartId() != null && lecture.getSchedulingSubpartId().equals(commitedLecture.getSchedulingSubpartId())) continue;
            if (ignore(placement, commitedPlacement) && (overlaps(placement, commitedPlacement) || distance(getMetrics(), placement, commitedPlacement)))
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
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = super.getBounds(variables);
        for (Lecture lecture: variables) {
            Double max = null;
            for (Placement placement: lecture.values()) {
                if (max == null) { max = new Double(countCommittedConflicts(placement)); continue; }
                max = Math.max(max, countCommittedConflicts(placement));
            }
            if (max != null) bounds[0] += max;
        }
        return bounds;
    }
    
    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = super.getValue(value, conflicts);
        ret += countCommittedConflicts(value);
        if (iIncludeConflicts && conflicts != null)
            for (Placement conflict: conflicts)
                ret -= countCommittedConflicts(conflict);
        return ret;
    }
    
    @Override
    public double getValue(Collection<Lecture> variables) {
        double ret = super.getValue(variables);
        for (Lecture lect: variables)
            if (lect.getAssignment() != null)
                ret += countCommittedConflicts(lect.getAssignment());
        return Math.round(ret);
    }
}
