package net.sf.cpsolver.ifs.util;

import java.util.*;

/** Vector extension with faster enumeration.
 * In {@link FastVector#elements()}, no unnecessary check is made.
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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

public class FastVector extends Vector {
	private static final long serialVersionUID = 1L;
    
    public FastVector() {
        super();
    }
    public FastVector(int initialCapacity) {
        super(initialCapacity);
    }
    public FastVector(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }
    public FastVector(Collection c) {
        super(c);
    }
 
    public Enumeration elements() {
	return new Enumeration() {
	    int count = 0;
	    public boolean hasMoreElements() { return count < elementCount; }
	    public Object nextElement() { return elementData[count++]; }
	};
    }
    
    public String toString() {
        return size()+"/"+super.toString();
    }
}
