package net.sf.cpsolver.ifs.solver;

import net.sf.cpsolver.ifs.model.*;

/**
 * IFS Solver Listener.
 *
 * @see Solver
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
**/
public interface SolverListener {
    
    /** A variable was selected 
     * @param iteration current iteration
     * @param variable selected variable
     */
    public boolean variableSelected(long iteration, Variable variable);
    
    /** A value was selected 
     * @param iteration current iteration
     * @param variable selected variable
     * @param value selected variable
     */
    public boolean valueSelected(long iteration, Variable variable, Value value);
    
    /** A neighbour was selected
     * @param iteration current iteration
     * @param neighbour neighbour
     */
    public boolean neighbourSelected(long iteration, Neighbour neighbour);
    
}
