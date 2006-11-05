package net.sf.cpsolver.ifs.util;

import java.util.*;

/** Data properties.
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
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
public class DataProperties extends Properties {
    private boolean iSaveDefaults = false;
    
    /** Constructor */
    public DataProperties() {
        super();
    }

    /** Constructor 
     * @param defaults default properties
     */
    public DataProperties(Properties defaults) {
        super(defaults);
        iSaveDefaults = getPropertyBoolean("General.SaveDefaultProperties", false);
    }

    /** Constructor 
     * @param properties default properties
     */
    public DataProperties(Dictionary properties) {
        super();
        for (Enumeration e=properties.keys();e.hasMoreElements();) {
            Object key = e.nextElement();
            Object value = properties.get(key);
            setProperty(key.toString(), value.toString());
        }
        iSaveDefaults = getPropertyBoolean("General.SaveDefaultProperties", false);
    }
    
    /** Returns string property
     * @param key key
     * @param defaultValue default value to be returned when such property is not present
     */
    public String getProperty(String key, String defaultValue) {
        if (!iSaveDefaults || containsPropery(key)) return super.getProperty(key, defaultValue);
        if (defaultValue!=null) setProperty(key, defaultValue);
        return defaultValue;
    }
    
    /** Sets string property
     * @param key key
     * @param value value
     */
    public Object setProperty(String key, String value) {
        Object ret = super.setProperty(key, value);
        if ("General.SaveDefaultProperties".equals(key))
            iSaveDefaults = getPropertyBoolean("General.SaveDefaultProperties", false);
        return ret;
    }
    
    /** Returns int property
     * @param key key
     * @param defaultValue default value to be returned when such property is not present
     */
    public int getPropertyInt(String key, int defaultValue) {
        try {
            if (containsPropery(key)) return Integer.parseInt(getProperty(key));
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        } catch (NumberFormatException nfe) {
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        }
    }
    
    /** Returns long property
     * @param key key
     * @param defaultValue default value to be returned when such property is not present
     */
    public long getPropertyLong(String key, long defaultValue) {
        try {
            if (containsPropery(key)) return Long.parseLong(getProperty(key));
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        } catch (NumberFormatException nfe) {
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        }
    }
    
    /** Returns true if there is such property
     * @param key key
     */
    public boolean containsPropery(String key) {
        return getProperty(key)!=null;
    }

    /** Returns boolean property
     * @param key key
     * @param defaultValue default value to be returned when such property is not present
     */
    public boolean getPropertyBoolean(String key, boolean defaultValue) {
        try {
            if (containsPropery(key)) return getProperty(key).equalsIgnoreCase("true");
            if (iSaveDefaults) setProperty(key, (defaultValue?"true":"false"));
            return defaultValue;
        } catch (Exception nfe) {
            if (iSaveDefaults) setProperty(key, (defaultValue?"true":"false"));
            return defaultValue;
        }
    }

    /** Returns double property
     * @param key key
     * @param defaultValue default value to be returned when such property is not present
     */
    public double getPropertyDouble(String key, double defaultValue) {
        try {
            if (containsPropery(key)) return Double.parseDouble(getProperty(key));
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        } catch (NumberFormatException nfe) {
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /** Returns float property
     * @param key key
     * @param defaultValue default value to be returned when such property is not present
     */
    public float getPropertyFloat(String key, float defaultValue) {
        try {
            if (containsPropery(key)) return Float.parseFloat(getProperty(key));
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        } catch (NumberFormatException nfe) {
            if (iSaveDefaults) setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /** Returns properties as dictionary.
     */
    public Dictionary toDict() {
        Hashtable ret = new Hashtable();
        for (Enumeration e=propertyNames();e.hasMoreElements();) {
            String key = (String)e.nextElement();
            String prop = getProperty(key);
            if (key!=null && prop!=null) ret.put(key,prop);
        }
        return ret;
    }

    private void expand(String key) {
        String value = getProperty(key);
        if (value==null) return;
        int done = -1, idx = -1;
        while ((idx=value.indexOf('%', done+1))>=0) {
            int idx2 = value.indexOf('%', idx+1);
            if (idx2<0) return;
            String subString = value.substring(idx+1,idx2);
            if (containsPropery(subString))
                value = value.substring(0,idx)+getProperty(subString)+value.substring(idx2+1);
            else
                done=idx;
        }
    }
    
    /** Loads properties from an input stream*/
    public void load(java.io.InputStream inputStream) throws java.io.IOException {
        super.load(inputStream);
        for (Enumeration e=keys();e.hasMoreElements();) {
            expand((String)e.nextElement());
        }
        iSaveDefaults = getPropertyBoolean("General.SaveDefaultProperties", false);
    }
}
