package org.cpsolver.ifs.assignment;

import java.util.Comparator;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * Comparator for the {@link AssignmentComparable} objects.
 * 
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 **/
public class AssignmentComparator<X extends AssignmentComparable<X, V, T>, V extends Variable<V, T>, T extends Value<V, T>> implements Comparator<X> {
    protected Assignment<V, T> iAssignment;
    
    /**
     * Create comparator with the given assignment.
     */
    public AssignmentComparator(Assignment<V, T> assignment) {
        iAssignment = assignment;
    }

    @Override
    public int compare(X o1, X o2) {
        return o1.compareTo(iAssignment, o2);
    }

}
