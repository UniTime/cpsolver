package net.sf.cpsolver.ifs.example.tt;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Activity (variable).
 * It encodes a name, length 
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
public class Activity extends Variable {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Activity.class);
    private int iLength = 1;
    private String iActivityId = null;
    private String iName = null;
    private Vector iResorces = new FastVector();
    private Set iProhibitedSlots = new HashSet();
    private Set iDiscouragedSlots = new HashSet();
    
    public Activity(int length, String id, String name) {
        super(null);
        iLength = length;
        iActivityId = id;
        iName = name;
    }
    
    public String getName() { return iName; }
    public String getActivityId() { return iActivityId; }
    
    public int getLength() { return iLength; }
    
    public void addResourceGroup(Vector resources) {
        for (Enumeration e=resources.elements();e.hasMoreElements();) {
            ((Resource)e.nextElement()).addVariable(this);
        }
        iResorces.add(resources);
    }
    public void addResourceGroup(Resource[] resources) {
        Vector rg = new FastVector(resources.length);
        for (int i=0;i<resources.length;i++) {
            rg.addElement(resources[i]);
            resources[i].addVariable(this);
        }
        iResorces.add(rg);
    }
    public void addResourceGroup(Resource resource) {
        Vector rg = new FastVector(1);
        rg.addElement(resource);
        iResorces.add(rg);
        resource.addVariable(this);
    }
    public Vector getResourceGroup(int idx) {
        return (Vector)iResorces.elementAt(idx);
    }
    public Vector getResourceGroups() {
        return iResorces;
    }
    public Set getProhibitedSlots() { return iProhibitedSlots; }
    public Set getDiscouragedSlots() { return iDiscouragedSlots; }
    public void addProhibitedSlot(int day, int hour) {
        iProhibitedSlots.add(new Integer(((TimetableModel)getModel()).getNrHours()*day+hour));
    }
    public void addDiscouragedSlot(int day, int hour) {
        iDiscouragedSlots.add(new Integer(((TimetableModel)getModel()).getNrHours()*day+hour));
    }
    public boolean isProhibitedSlot(int day, int hour) {
        return iProhibitedSlots.contains(new Integer(((TimetableModel)getModel()).getNrHours()*day+hour));
    }
    public boolean isDiscouragedSlot(int day, int hour) {
        return iDiscouragedSlots.contains(new Integer(((TimetableModel)getModel()).getNrHours()*day+hour));
    }
    public void addProhibitedSlot(int slot) {
        iProhibitedSlots.add(new Integer(slot));
    }
    public void addDiscouragedSlot(int slot) {
        iDiscouragedSlots.add(new Integer(slot));
    }
    public boolean isProhibitedSlot(int slot) {
        return iProhibitedSlots.contains(new Integer(slot));
    }
    public boolean isDiscouragedSlot(int slot) {
        return iDiscouragedSlots.contains(new Integer(slot));
    }
    public boolean isProhibited(int day, int hour, int length) {
        int slot = ((TimetableModel)getModel()).getNrHours()*day+hour;
        for (int i=0;i<length;i++)
            if (iProhibitedSlots.contains(new Integer(slot+i))) return true;
        return false;
    }
    public void init() {
        setValues(computeValues());
    }
    
    private void addValues(Vector values, int day, int hour, int level, Resource[] resources) {
        if (level==getResourceGroups().size()) {
            values.addElement(new Location(this, day, hour, (Resource[])resources.clone()));
            return;
        }
        Vector rg = (Vector)getResourceGroups().elementAt(level);
        for (Enumeration f=rg.elements();f.hasMoreElements();) {
            Resource r = (Resource)f.nextElement();
            if (r.isProhibited(day, hour, getLength())) continue;
            resources[level]=r;
            addValues(values, day, hour, level+1, resources);
        }
    }
    
    public Vector computeValues() {
        TimetableModel m = (TimetableModel)getModel();
        Vector values = new FastVector();
        Resource[] res = new Resource[getResourceGroups().size()];
        for (int day=0;day<m.getNrDays();day++)
            for (int hour=0;hour<=m.getNrHours()-getLength();hour++) {
                if (isProhibited(day,hour,getLength())) continue;
                addValues(values, day, hour, 0, res);
            }
        return values;
    }
}
