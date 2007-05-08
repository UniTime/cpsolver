package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

/**
 * Representation of a scheduling subpart. Each scheduling subpart contains id, instructional type, name, 
 * instructional offering configuration, and a list of sections. Optionally, parent-child relation between
 * subparts can be defined.
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class Subpart implements Comparable {
    private long iId = -1;
    private String iInstructionalType = null;
    private String iName = null;
    private Vector iSections = new Vector();
    private Config iConfig = null;
    private Subpart iParent = null;

    /** Constructor
     * @param id scheduling subpart unique id
     * @param itype instructional type
     * @param name subpart name
     * @param config instructional offering configuration to which this subpart belongs
     * @param parent parent subpart, if parent-child relation is defined between subparts
     */
    public Subpart(long id, String itype, String name, Config config, Subpart parent) {
        iId = id;
        iInstructionalType = itype;
        iName = name;
        iConfig = config;
        iParent = parent;
        iConfig.getSubparts().add(this);
    }
    
    /** Subpart id  */
    public long getId() {
        return iId;
    }
    
    /** Instructional type, e.g., Lecture, Recitation or Laboratory */
    public String getInstructionalType() {
        return iInstructionalType;
    }
    
    /** Subpart name */
    public String getName() {
        return iName;
    }
    
    /** Instructional offering configuration to which this subpart belongs */
    public Config getConfig() {
        return iConfig;
    }

    /** List of sections */
    public Vector getSections() {
        return iSections;
    }
    
    /** Parent subpart, if parent-child relation is defined between subparts */
    public Subpart getParent() {
        return iParent;
    }
    
    public String toString() {
        return getName();
    }
    
    /** True, if this subpart is parent (or parent of a parent etc.) of the given subpart */
    public boolean isParentOf(Subpart subpart) {
        if (subpart.getParent()==null) return false;
        if (subpart.getParent().equals(this)) return true;
        return isParentOf(subpart.getParent());
    }
    
    /** Compare two subparts: put parents first, use ids if there is no parent-child relation*/
    public int compareTo(Object o) {
        if (o==null || !(o instanceof Subpart)) {
            return -1;
        }
        Subpart s = (Subpart)o;
        if (isParentOf(s)) return -1;
        if (s.isParentOf(this)) return 1;
        return Double.compare(getId(),s.getId());
    }
    
    /** List of available choices of the sections of this subpart. */
    public HashSet getChoices() {
        HashSet choices = new HashSet();
        for (Enumeration e=getSections().elements();e.hasMoreElements();) {
            choices.add(((Section)e.nextElement()).getChoice());
        }
        return choices;
    }
}
