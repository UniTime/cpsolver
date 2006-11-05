package net.sf.cpsolver.ifs.model;

/**
 * IFS model listener.
 *
 * @see Model
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
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
public interface ModelListener {
    /** Variable is added to the model
     * @param variable added variable
     */
    public void variableAdded(Variable variable);
    
    /** Variable is removed from the model
     * @param variable removed variable
     */
    public void variableRemoved(Variable variable);
    
    /** Constraint is added to the model
     * @param constraint added constraint
     */
    public void constraintAdded(Constraint constraint);
    
    /** Constraint is removed from the model
     * @param constraint removed constraint
     */
    public void constraintRemoved(Constraint constraint);
    
    /** Called before a value is assigned to its variable ({@link Value#variable()}). 
     * @param iteration current iteration
     * @param value value to be assigned
     */
    public void beforeAssigned(long iteration, Value value);

    /** Called before a value is unassigned from its variable ({@link Value#variable()}). 
     * @param iteration current iteration
     * @param value value to be unassigned
     */
    public void beforeUnassigned(long iteration, Value value);
    
    /** Called after a value is assigned to its variable ({@link Value#variable()}). 
     * @param iteration current iteration
     * @param value value to be assigned
     */
    public void afterAssigned(long iteration, Value value);

    /** Called after a value is unassigned from its variable ({@link Value#variable()}). 
     * @param iteration current iteration
     * @param value value to be unassigned
     */
    public void afterUnassigned(long iteration, Value value);
    
    /** Query for info about the model. A listener can also add some its info.
     * @param anInfo resultant table with informations (key, value).
     */
    public void getInfo(java.util.Hashtable anInfo);

    /** Notification that the model was initialized by the solver. 
     * @param solver IFS solver
     */
    public boolean init(net.sf.cpsolver.ifs.solver.Solver solver);
}
