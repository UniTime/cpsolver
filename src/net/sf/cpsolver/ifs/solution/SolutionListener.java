package net.sf.cpsolver.ifs.solution;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * IFS solution listener.
 * 
 * @see Solution
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
public interface SolutionListener<V extends Variable<V, T>, T extends Value<V, T>> {
    /**
     * Called by the solution when it is updated, see
     * {@link Solution#update(double)}.
     * 
     * @param solution
     *            source solution
     */
    public void solutionUpdated(Solution<V, T> solution);

    /**
     * Called by the solution when it is asked to produce info table, see
     * {@link Solution#getInfo()}. A listener can also add some its info into
     * this table.
     * 
     * @param solution
     *            source solution
     * @param info
     *            produced info table
     */
    public void getInfo(Solution<V, T> solution, Map<String, String> info);

    /**
     * Called by the solution when it is asked to produce info table, see
     * {@link Solution#getInfo()}. A listener can also add some its info into
     * this table.
     * 
     * @param solution
     *            source solution
     * @param info
     *            produced info table
     * @param variables
     *            only variables from this set are included
     */
    public void getInfo(Solution<V, T> solution, Map<String, String> info, Collection<V> variables);

    /**
     * Called by the solution when method {@link Solution#clearBest()} is
     * called.
     * 
     * @param solution
     *            source solution
     */
    public void bestCleared(Solution<V, T> solution);

    /**
     * Called by the solution when method {@link Solution#saveBest()} is called.
     * 
     * @param solution
     *            source solution
     */
    public void bestSaved(Solution<V, T> solution);

    /**
     * Called by the solution when method {@link Solution#restoreBest()} is
     * called.
     * 
     * @param solution
     *            source solution
     */
    public void bestRestored(Solution<V, T> solution);
}
