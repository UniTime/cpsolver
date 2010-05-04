package net.sf.cpsolver.ifs.util;

import java.util.*;

/** Queue.
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
public class Queue {
    private Object iElementData[];
    private int iMaxSize, iFirst, iLast;
    private HashSet iHashedElementData;
    
    /** Constructor
     * @param maxSize maximal size of the queue
     */
    public Queue(int maxSize) {
        iMaxSize = maxSize+2;
        iFirst = iLast = 0;
        iElementData = new Object[iMaxSize];
        iHashedElementData = new HashSet(2*iMaxSize,0.5f);
    }
    
    /** Constructor
     * @param maxSize maximal size of the queue
     * @param initials initial content
     */
    public Queue(int maxSize, Collection initials) {
        this(maxSize);
        for (Iterator i=initials.iterator();i.hasNext();)
            put(i.next());
    }

    /** Puts object at the end of the queue */
    public void put(Object object) throws ArrayIndexOutOfBoundsException {
        iElementData[iLast]=object;
        iHashedElementData.add(object);
        iLast = (iLast + 1) % iMaxSize;
        if (iFirst==iLast) throw new ArrayIndexOutOfBoundsException("Queue is full.");
    }
    
    /** Returns true if queue contains the given object */
    public boolean contains(Object object) {
        return iHashedElementData.contains(object);
    }
    
    /** Gets first element of the queue */
    public Object get() {
        if (iLast==iFirst) return null;
        Object ret = iElementData[iFirst];
        iFirst = (iFirst + 1) % iMaxSize;
        iHashedElementData.remove(ret);
        return ret;
    }
    
    /** Returns size of the queue */
    public int size() {
        return (iLast>=iFirst?iLast-iFirst:iMaxSize+iLast-iFirst);
    }
    
    /** Returns true if the queue is empty */
    public boolean isEmpty() {
        return iFirst==iLast;
    }
}
