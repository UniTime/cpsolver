package net.sf.cpsolver.ifs.util;

import java.util.Collection;
import java.util.Vector;

/**
 * Only used for backward compatibility.
 * 
 * Use {@link java.util.ArrayList} whenever possible as this class will go away in future.
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
@Deprecated
public class FastVector<E> extends Vector<E> {
    private static final long serialVersionUID = -3163198412759471953L;

    public FastVector() {
        super();
    }
    
    public FastVector(int initialCapacity) {
        super(initialCapacity);
    }
    
    public FastVector(Collection<? extends E> c) {
        super(c);
    }

}
