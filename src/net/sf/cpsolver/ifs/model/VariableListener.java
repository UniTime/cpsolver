package net.sf.cpsolver.ifs.model;

/**
 * IFS variable listener.
 * 
 * @see Variable
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public interface VariableListener<T extends Value<?, T>> {
    /**
     * Called by the variable when a value is assigned to it
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            assigned to the variable
     */
    public void variableAssigned(long iteration, T value);

    /**
     * Called by the variable when a value is unassigned from it
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            unassigned from the variable
     */
    public void variableUnassigned(long iteration, T value);

    /**
     * Called by the variable when a value is permanently removed from its
     * domain
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            removed from the variable's domain
     */
    public void valueRemoved(long iteration, T value);
}
