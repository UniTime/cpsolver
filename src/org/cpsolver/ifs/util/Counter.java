package org.cpsolver.ifs.util;

/**
 * Counter.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class Counter {
    private long iValue = 0;

    /** Set counter 
     * @param value counter value
     **/
    public void set(long value) {
        iValue = value;
    }

    /** Returns current value 
     * @return counter value
     **/
    public long get() {
        return iValue;
    }

    /** Increment counter
     * @param value counter increment
     **/
    public void inc(long value) {
        iValue += value;
    }

    /** Decrement counter
     * @param value counter decrement
     **/
    public void dec(long value) {
        iValue -= value;
    }
}
