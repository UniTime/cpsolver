package net.sf.cpsolver.ifs.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.apache.log4j.Logger;

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
public class JProf {
    private static Mode sMode = Mode.cpu;
    private static enum Mode {
        cpu, wall, user
    }
    private static boolean sInitialized = false;
    
    /** Enable / disable the thread CPU timing, if needed */
    private static void init() {
        if (sInitialized) return;
        sMode = Mode.valueOf(System.getProperty("jprof", sMode.name()));
        if (sMode != Mode.wall) {
            try {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (!bean.isCurrentThreadCpuTimeSupported()) {
                    Logger.getLogger(JProf.class).warn("Measuring " + sMode.name() + " time is not supported, falling back to wall time.");
                    sMode = Mode.wall;
                }
                if (!bean.isThreadCpuTimeEnabled())
                    bean.setThreadCpuTimeEnabled(true);
            } catch (UnsupportedOperationException e) {
                Logger.getLogger(JProf.class).error("Unable to measure " + sMode.name() + " time, falling back to wall time: " + e.getMessage());
                sMode = Mode.wall;
                sMode = Mode.wall;
            }
        }
        Logger.getLogger(JProf.class).info("Using " + sMode.name() + " time.");
        sInitialized = true;
    }
    
    /** Current CPU time of this thread in seconds */
    public static double currentTimeSec() {
        init();
        try {
            switch (sMode) {
                case cpu :
                    return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 1e9;
                case user :
                    return ManagementFactory.getThreadMXBean().getCurrentThreadUserTime() / 1e9;
                case wall :
                default:
                    return System.nanoTime() / 1e9;
            }
        } catch (UnsupportedOperationException e) {
            Logger.getLogger(JProf.class).error("Unable to measure " + sMode.name() + " time, falling back to wall time: " + e.getMessage());
            sMode = Mode.wall;
            return System.nanoTime() / 1e9;
        }
    }
    
    /** Current CPU time of this thread in milliseconds */
    public static long currentTimeMillis() {
        init();
        try {
            switch (sMode) {
                case cpu :
                    return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 1000000;
                case user :
                    return ManagementFactory.getThreadMXBean().getCurrentThreadUserTime() / 1000000;
                case wall :
                default:
                    return System.currentTimeMillis();
            }
        } catch (UnsupportedOperationException e) {
            Logger.getLogger(JProf.class).error("Unable to measure " + sMode.name() + " time, falling back to wall time: " + e.getMessage());
            sMode = Mode.wall;
            return System.currentTimeMillis();
        }
    }
}
