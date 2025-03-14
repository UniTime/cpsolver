package org.cpsolver.studentsct.weights;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solution.SolutionComparator;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;


/**
 * Interface to model various student weightings
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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

public interface StudentWeights extends SolutionComparator<Request, Enrollment> {
    /**
     * Return lower bound for the given request
     * @param request given request
     * @return weight of the best value
     */
    public double getBound(Request request);
    
    /**
     * Return base weight of the given enrollment 
     * @param assignment current assignment
     * @param enrollment given enrollment
     * @return weight (higher weight means better value)
     */
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment);
    
    /**
     * Return weight of the given enrollment 
     * @param assignment current assignment
     * @param enrollment given enrollment
     * @param distanceConflicts distance conflicts
     * @param timeOverlappingConflicts time overlapping conflicts
     * @return weight (higher weight means better value)
     */
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<DistanceConflict.Conflict> distanceConflicts, Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts);
    
    /**
     * Return weight of the given enrollment 
     * @param assignment current assignment
     * @param enrollment given enrollment
     * @param qualityConflicts student quality conflicts
     * @return weight (higher weight means better value)
     */
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<StudentQuality.Conflict> qualityConflicts);
    
    /**
     * Return weight of a distance conflict
     * @param assignment current assignment
     * @param distanceConflict distance conflict
     * @return weight of the conflict
     */
    public double getDistanceConflictWeight(Assignment<Request, Enrollment> assignment, DistanceConflict.Conflict distanceConflict);
    
    /**
     * Return weight of a time overlapping conflict
     * @param assignment current assignment
     * @param enrollment enrollment in question
     * @param timeOverlap time overlapping conflict
     * @return weight of the conflict
     */
    public double getTimeOverlapConflictWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, TimeOverlapsCounter.Conflict timeOverlap);
    
    /**
     * Return weight of a student quality conflict
     * @param assignment current assignment
     * @param enrollment enrollment in question
     * @param conflict student quality conflict
     * @return weight of the conflict
     */
    public double getStudentQualityConflictWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, StudentQuality.Conflict conflict);
    
    /**
     * Return true if free time requests allow overlaps
     * @return true if free time overlaps are allowed, but the overlapping time is minimized
     */
    public boolean isFreeTimeAllowOverlaps();
    
    /**
     * Registered implementation
     */
    public static enum Implementation {
        Priority(PriorityStudentWeights.class),
        Equal(EqualStudentWeights.class),
        Legacy(OriginalStudentWeights.class);
        
        private Class<? extends StudentWeights> iImplementation;
        Implementation(Class<? extends StudentWeights> implementation) {
            iImplementation = implementation;
        }
        
        public Class<? extends StudentWeights> getImplementation() { return iImplementation; }
    }
}
