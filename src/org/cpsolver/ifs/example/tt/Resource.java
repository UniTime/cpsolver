package org.cpsolver.ifs.example.tt;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;


/**
 * Resource constraint
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
public class Resource extends ConstraintWithContext<Activity, Location, Resource.Context> {
    private String iName = null;
    private String iResourceId = null;
    private Set<Integer> iProhibitedSlots = new HashSet<Integer>();
    private Set<Integer> iDiscouragedSlots = new HashSet<Integer>();
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
    
    public String getResourceId() { return iResourceId; }
    @Override
    public String getName() { return iName; }
    public int getType() { return iType; }
    public Set<Integer> getProhibitedSlots() { return iProhibitedSlots; }
    public Set<Integer> getDiscouragedSlots() { return iDiscouragedSlots; }
    public void addProhibitedSlot(int day, int hour) {
        iProhibitedSlots.add(((TimetableModel)getModel()).getNrHours()*day+hour);
    }
    public void addDiscouragedSlot(int day, int hour) {
        iDiscouragedSlots.add(((TimetableModel)getModel()).getNrHours()*day+hour);
    }
    public boolean isProhibitedSlot(int day, int hour) {
        return iProhibitedSlots.contains(((TimetableModel)getModel()).getNrHours()*day+hour);
    }
    public boolean isDiscouragedSlot(int day, int hour) {
        return iDiscouragedSlots.contains(((TimetableModel)getModel()).getNrHours()*day+hour);
    }
    public void addProhibitedSlot(int slot) {
        iProhibitedSlots.add(slot);
    }
    public void addDiscouragedSlot(int slot) {
        iDiscouragedSlots.add(slot);
    }
    public boolean isProhibitedSlot(int slot) {
        return iProhibitedSlots.contains(slot);
    }
    public boolean isDiscouragedSlot(int slot) {
        return iDiscouragedSlots.contains(slot);
    }    
    public boolean isProhibited(int day, int hour, int length) {
        int slot = ((TimetableModel)getModel()).getNrHours()*day+hour;
        for (int i=0;i<length;i++)
            if (iProhibitedSlots.contains(slot+i)) return true;
        return false;
    }
    
    @Override
    public void computeConflicts(Assignment<Activity, Location> assignment, Location location, Set<Location> conflicts) {
        Activity activity = location.variable();
        if (!location.containResource(this)) return;
        Context context = getContext(assignment);
        for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
            Activity conf = context.getActivity(i);
            if (conf!=null && !activity.equals(conf)) 
		conflicts.add(assignment.getValue(conf));
        }
    }
    
    @Override
	public boolean inConflict(Assignment<Activity, Location> assignment, Location location) {
        Activity activity = location.variable();
        if (!location.containResource(this)) return false;
        Context context = getContext(assignment);
        for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
            if (context.getActivity(i) != null) return true;
        }
        return false;
    }
        
    @Override
	public boolean isConsistent(Location l1, Location l2) {
        return !l1.containResource(this) || !l2.containResource(this) || !l1.hasIntersection(l2);
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Activity, Location> assignment) {
        return new Context(assignment);
    }
    
    /**
     * Assignment context
     */
    public class Context implements AssignmentConstraintContext<Activity, Location> {
        private Activity[] iResource;
        
        public Context(Assignment<Activity, Location> assignment) {
            TimetableModel model = (TimetableModel)getModel();
            iResource = new Activity[model.getNrDays() * model.getNrHours()];
            for (int i=0;i<iResource.length;i++)
                    iResource[i] = null;
            for (Location location: assignment.assignedValues())
                assigned(assignment, location);
        }

        @Override
        public void assigned(Assignment<Activity, Location> assignment, Location location) {
            Activity activity = location.variable();
            if (!location.containResource(Resource.this)) return;
            for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
                iResource[i] = activity;
            }
        }
        
        public Activity getActivity(int slot) {
            return iResource[slot];
        }

        @Override
        public void unassigned(Assignment<Activity, Location> assignment, Location location) {
            Activity activity = location.variable();
            if (!location.containResource(Resource.this)) return;
            for (int i=location.getSlot(); i<location.getSlot()+activity.getLength(); i++) {
                iResource[i] = null;
            }
        }
    }
}
