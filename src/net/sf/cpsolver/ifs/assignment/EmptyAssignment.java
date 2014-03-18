package net.sf.cpsolver.ifs.assignment;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.cpsolver.ifs.assignment.context.AssignmentContextHolderMap;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * An empty assignment. All variables are unassigned, any attempt to assign a 
 * variable will throw the {@link UnsupportedOperationException} exception.<br><br>  
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
public class EmptyAssignment<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentAbstract<V, T> {

    public EmptyAssignment() {
        super(new AssignmentContextHolderMap<V,T>());
    }

    @Override
    public long getIteration(V variable) {
        return 0;
    }

    @Override
    public Collection<V> assignedVariables() {
        return new ArrayList<V>();
    }

    @Override
    protected T getValueInternal(V variable) {
        return null;
    }

    @Override
    protected void setValueInternal(long iteration, V variable, T value) {
        throw new UnsupportedOperationException();
    }
}
