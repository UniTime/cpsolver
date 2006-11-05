package net.sf.cpsolver.ifs.example.tt;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;

/**
 * Resource constraint
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
public class Resource extends Constraint {
    private String iName = null;
    private String iResourceId = null;
    private Activity[] iResource;
    private Set iProhibitedSlots = new HashSet();
    private Set iDiscouragedSlots = new HashSet();
    private int iType = TYPE_OTHER;
    
    public static final int TYPE_ROOM = 0;
    public static final int TYPE_INSTRUCTOR = 1;
    public static final int TYPE_CLASS = 2;
    public static final int TYPE_OTHER = 3;

    public Resource(String id, int type, String name) {
	super();
        iResourceId = id;
        iName = name;
        iType = type;
    }
    
    public void setModel(Model model) {
        super.setModel(model);
        iResource = new Activity[((TimetableModel)model).getNrDays()*((TimetableModel)model).getNrHours()];
        for (int i=0;i<iResource.length;i++)
                iResource[i] = null;
    }
    
    public String getResourceId() { return iResourceId; }
    public String getName() { return iName; }
    public int getType() { return iType; }
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
    
    public void computeConflicts(Value value, Set conflicts) {
        Activity activity = (Activity) value.variable();
        Location location = (Location) value;
        if (!location.containResource(this)) return;
        for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
            Activity conf = iResource[i];
            if (conf!=null && !activity.equals(conf)) 
		conflicts.add(conf.getAssignment());
        }
    }
    
    public boolean inConflict(Value value) {
        Activity activity = (Activity) value.variable();
        Location location = (Location) value;
        if (!location.containResource(this)) return false;
        for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
            if (iResource[i]!=null) return true;
        }
        return false;
    }
        
    public boolean isConsistent(Value value1, Value value2) {
        Location l1 = (Location) value1;
        Location l2 = (Location) value2;
        return !l1.containResource(this) || !l2.containResource(this) || !l1.hasIntersection(l2);
    }
    
    public void assigned(long iteration, Value value) {
        super.assigned(iteration, value);
        Activity activity = (Activity) value.variable();
        Location location = (Location) value;
        if (!location.containResource(this)) return;
        for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
            iResource[i] = activity;
        }
    }
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        Activity activity = (Activity) value.variable();
        Location location = (Location) value;
        if (!location.containResource(this)) return;
        for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
            iResource[i] = null;
        }
    }
}
