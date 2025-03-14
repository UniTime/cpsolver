package org.cpsolver.coursett.sectioning;

import java.util.List;

import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;

/**
 * A simple interface representing a student enrollment change.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2017 Tomas Muller<br>
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
public interface StudentMove extends Neighbour<Lecture, Placement> {
    public static double sEps = 0.0001;

    /**
     * Is the change valid / allowed?
     */
    public boolean isAllowed();
    
    /**
     * Student conflict weight change
     */
    public double value(List<StudentConflict> criteria, Assignment<Lecture, Placement> assignment);
    
    /**
     * Student group weight change
     */
    public double group(List<StudentConflict> criteria, Assignment<Lecture, Placement> assignment);
}
