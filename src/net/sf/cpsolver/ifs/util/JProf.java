package net.sf.cpsolver.ifs.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * CPU time measurement. <s>JAVA profiling extension is used. Java needs to be
 * executed with -Xrunjprof. When the java is executed outside this profiler,
 * {@link System#currentTimeMillis()} is used.</s> Using {@link ThreadMXBean}
 * to get the current thread CPU time, if supported. Using {@link System#nanoTime()} 
 * otherwise.
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
public class JProf {
    /** Enable the thread CPU timing, if needed */
    static {
        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            if (bean.isCurrentThreadCpuTimeSupported() && !bean.isThreadCpuTimeEnabled())
                bean.setThreadCpuTimeEnabled(true);
        } catch (UnsupportedOperationException e) {}
    }
    
    /** Current CPU (user) time of this thread in seconds */
    public static double currentTimeSec() {
        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            if (bean.isCurrentThreadCpuTimeSupported() && bean.isThreadCpuTimeEnabled())
                return bean.getCurrentThreadUserTime() / 1e9;
            else
                return System.nanoTime() / 1e9;
        } catch (UnsupportedOperationException e) {
            return System.nanoTime() / 1e9;
        }
    }
    
    /** Current CPU (user) time of this thread in milliseconds */
    public static long currentTimeMillis() {
        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            if (bean.isCurrentThreadCpuTimeSupported() && bean.isThreadCpuTimeEnabled())
                return bean.getCurrentThreadUserTime() / 1000000;
            else
                return System.currentTimeMillis();
        } catch (UnsupportedOperationException e) {
            return System.currentTimeMillis();
        }
    }
}
