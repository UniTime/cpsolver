package net.sf.cpsolver.ifs.assignment;

import java.util.Comparator;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * A simple class comparing two values. Using {@link Value#compareTo(Assignment, Value)}.
 * This is to replace the {@link Comparable} interface on the {@link Value} which is
 * using the deprecated method {@link Value#compareTo(Value)}.
 * 
 * @see Assignment
 * @see Solver
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
public class ValueComparator<V extends Variable<V, T>, T extends Value<V, T>> implements Comparator<T> {
    private Assignment<V, T> iAssignment;
    
    public ValueComparator(Assignment<V, T> assignment) {
        iAssignment = assignment;
    }
    
    @Override
    public int compare(T t1, T t2) {
        return t1.compareTo(iAssignment, t2);
    }

}
