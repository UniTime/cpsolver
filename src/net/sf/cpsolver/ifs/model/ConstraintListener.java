package net.sf.cpsolver.ifs.model;

import java.util.*;

/**
 * IFS constraint listener.
 *
 * @see Constraint
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public interface ConstraintListener {
    /** Called by the constraint, before a value is assigned to its variable.
     * @param iteration current iteration
     * @param constraint source constraint
     * @param assigned value which will be assigned to its variable ({@link Value#variable()})
     * @param unassigned set of conflicting values which will be unassigned by the constraint before it assigns the given value
     */
    public void constraintBeforeAssigned(long iteration, Constraint constraint, Value assigned, Set unassigned);

    /** Called by the constraint, after a value is assigned to its variable.
     * @param iteration current iteration
     * @param constraint source constraint
     * @param assigned value which was assigned to its variable ({@link Value#variable()})
     * @param unassigned set of conflicting values which were unassigned by the constraint before it assigned the given value
     */
    public void constraintAfterAssigned(long iteration, Constraint constraint, Value assigned, Set unassigned);
}