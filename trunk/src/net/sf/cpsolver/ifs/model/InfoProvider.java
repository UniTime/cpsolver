package net.sf.cpsolver.ifs.model;

import java.util.Collection;
import java.util.Map;

/**
 * A class providing INFO table.
 * 
 * @see Model
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
 */
public interface InfoProvider<V extends Variable<V, ?>> {
    /** Adds some information into the table with information about the solution */
    public void getInfo(Map<String, String> info);

    /**
     * Adds some information into the table with information about the solution,
     * only consider variables from the given set
     */
    public void getInfo(Map<String, String> info, Collection<V> variables);
}