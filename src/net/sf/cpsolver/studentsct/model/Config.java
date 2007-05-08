package net.sf.cpsolver.studentsct.model;

import java.util.Vector;

/**
 * Representation of a configuration of an offering. A configuration contains id, name, an offering and a list of subparts.
 * <br><br>
 * Each instructional offering (see {@link Offering}) contains one or more configurations. 
 * Each configuration contain one or more subparts. Each student has to take a class of each subpart of one of the 
 * possible configurations. Some restrictions might be defined using reservations (see {@link net.sf.cpsolver.studentsct.constraint.Reservation}). 
 *  
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
public class Config {
    private long iId = -1;
    private String iName = null;
    private Offering iOffering = null;
    private Vector iSubparts = new Vector();
    
    /** Constructor
     * @param id instructional offering configuration unique id 
     * @param name configuration name
     * @param offering instructional offering to which this configuration belongs
     */
    public Config(long id, String name, Offering offering) {
        iId = id;
        iName = name;
        iOffering = offering;
        iOffering.getConfigs().add(this);
    }
    
    /** Configuration id */
    public long getId() {
        return iId;
    }
    
    /** Configuration name */
    public String getName() {
        return iName;
    }
    
    /** Instructional offering to which this configuration belongs. */
    public Offering getOffering() {
        return iOffering;
    }
    
    /** List of subparts */
    public Vector getSubparts() {
        return iSubparts;
    }
    
    public String toString() {
        return getName();
    }
}
