package net.sf.cpsolver.ifs;

/**
 * IFS common constants. <br>
 * <br>
 * Build number and release date are to be set by apache ant.
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
public class Constants {
    protected static final String VERSION = "1.2";
    protected static final int BLD_NUMBER = -1;
    protected static final String REL_DATE = "Unknown";

    /**
     * Version
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Build number
     */
    public static int getBuildNumber() {
        return BLD_NUMBER;
    }

    /**
     * Release date
     */
    public static String getReleaseDate() {
        return REL_DATE;
    }
}