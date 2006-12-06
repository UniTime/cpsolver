package net.sf.cpsolver.ifs.solution;

/**
 * IFS solution listener.
 *
 * @see Solution
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
public interface SolutionListener {
    /** Called by the solution when it is updated, see {@link Solution#update(double)}. 
     * @param solution source solution
     */
    public void solutionUpdated(Solution solution);
    
    /** Called by the solution when it is asked to produce info table, see {@link Solution#getInfo()}.
     * A listener can also add some its info into this table.
     * @param solution source solution
     * @param info produced info table
     */
    public void getInfo(Solution solution, java.util.Dictionary info);
    /** Called by the solution when it is asked to produce info table, see {@link Solution#getInfo()}.
     * A listener can also add some its info into this table.
     * @param solution source solution
     * @param info produced info table
     * @param variables only variables from this set are included
     */
    public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables);
    
    /** Called by the solution when method {@link Solution#clearBest()} is called.
     * @param solution source solution
     */
    public void bestCleared(Solution solution);

    /** Called by the solution when method {@link Solution#saveBest()} is called.
     * @param solution source solution
     */
    public void bestSaved(Solution solution);

    /** Called by the solution when method {@link Solution#restoreBest()} is called.
     * @param solution source solution
     */
    public void bestRestored(Solution solution);
}
