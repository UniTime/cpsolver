package net.sf.cpsolver.ifs.util;

/** CPU time measurement. JAVA profiling extension is used.
 * Java needs to be executed with -Xrunjprof. When the java is executed outside
 * this profiler, {@link System#currentTimeMillis()} is used.
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
public class JProf {
    private static boolean sPrecise = true;
    
    /** Current CPU time of this thread in seconds */
    public static double currentTimeSec() {
        return (sPrecise?((double)getCurrentThreadCpuTime())/1e9:((double)System.currentTimeMillis())/1e3);
    }
    /** Measurement is based on profiler extension (precise CPU time is returned). 
     * If false, {@link System#currentTimeMillis()} is used in {@link JProf#currentTimeSec()}.
     */
    public static boolean isPrecise() { return sPrecise; }

    /** Current CPU time of this thread (will fail when jprof is not loaded). Use {@link JProf#currentTimeSec()}.*/
    public static native long getCurrentThreadCpuTime();

    static {
        try {
            System.loadLibrary("jprof");
            if (getCurrentThreadCpuTime()==0l) {
            	int j=0;
            	for (int i=0;i<10000000;i++) j+=i;
            	if (getCurrentThreadCpuTime()==0l) {
            	    sPrecise = false;
            	    org.apache.log4j.Logger.getLogger(JProf.class).warn("Unable to mesure time in precise -- using System.currentTimeMillis().");
            	}
            }
        } catch (Throwable e) {
            org.apache.log4j.Logger.getLogger(JProf.class).warn("Unable to mesure time in precise -- using System.currentTimeMillis().");
            sPrecise = false;
        }	
    }
}
