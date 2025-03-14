package org.cpsolver.ifs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * IFS common constants. <br>
 * <br>
 * Build number and release date are to be set by apache ant.
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
public class Constants {
	private static Properties sProperties = null;
	
	private static Properties getProperties() {
		if (sProperties == null) {
			sProperties = new Properties();
			InputStream in = Constants.class.getClassLoader().getResourceAsStream("cpsolver.version");
			if (in != null) {
				try {
					sProperties.load(in);
					in.close();
				} catch (IOException e) {}
			}
		}
		return sProperties;
	}
	
    /**
     * Version
     * @return current solver version
     */
    public static String getVersion() {
    	return getProperties().getProperty("project.version", "1.3");
    }

    /**
     * Build number
     * @return current solver build number
     */
    public static String getBuildNumber() {
    	return getProperties().getProperty("cpsolver.build_nbr", "?");
    }

    /**
     * Release date
     * @return current solver release date
     */
    public static String getReleaseDate() {
    	return getProperties().getProperty("cpsolver.rel_date", "?");
    }
}