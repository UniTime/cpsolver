package net.sf.cpsolver.ifs.example.tt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Variable;

/**
 * Activity (variable). It encodes a name, length
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
public class Activity extends Variable<Activity, Location> {
    private int iLength = 1;
    private String iActivityId = null;
    private String iName = null;
    private ArrayList<List<Resource>> iResorces = new ArrayList<List<Resource>>();
    private Set<Integer> iProhibitedSlots = new HashSet<Integer>();
    private Set<Integer> iDiscouragedSlots = new HashSet<Integer>();

    public Activity(int length, String id, String name) {
        super(null);
        iLength = length;
        iActivityId = id;
        iName = name;
    }

    @Override
    public String getName() {
        return iName;
    }

    public String getActivityId() {
        return iActivityId;
    }

    public int getLength() {
        return iLength;
    }

    public void addResourceGroup(List<Resource> resources) {
        for (Resource r : resources)
            r.addVariable(this);
        iResorces.add(resources);
    }

    public void addResourceGroup(Resource[] resources) {
        ArrayList<Resource> rg = new ArrayList<Resource>(resources.length);
        for (int i = 0; i < resources.length; i++) {
            rg.add(resources[i]);
            resources[i].addVariable(this);
        }
        iResorces.add(rg);
    }

    public void addResourceGroup(Resource resource) {
        ArrayList<Resource> rg = new ArrayList<Resource>(1);
        rg.add(resource);
        iResorces.add(rg);
        resource.addVariable(this);
    }

    public List<Resource> getResourceGroup(int idx) {
        return iResorces.get(idx);
    }

    public List<List<Resource>> getResourceGroups() {
        return iResorces;
    }

    public Set<Integer> getProhibitedSlots() {
        return iProhibitedSlots;
    }

    public Set<Integer> getDiscouragedSlots() {
        return iDiscouragedSlots;
    }

    public void addProhibitedSlot(int day, int hour) {
        iProhibitedSlots.add(((TimetableModel) getModel()).getNrHours() * day + hour);
    }

    public void addDiscouragedSlot(int day, int hour) {
        iDiscouragedSlots.add(((TimetableModel) getModel()).getNrHours() * day + hour);
    }

    public boolean isProhibitedSlot(int day, int hour) {
        return iProhibitedSlots.contains(((TimetableModel) getModel()).getNrHours() * day + hour);
    }

    public boolean isDiscouragedSlot(int day, int hour) {
        return iDiscouragedSlots.contains(((TimetableModel) getModel()).getNrHours() * day + hour);
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
        int slot = ((TimetableModel) getModel()).getNrHours() * day + hour;
        for (int i = 0; i < length; i++)
            if (iProhibitedSlots.contains(slot + i))
                return true;
        return false;
    }

    public void init() {
        setValues(computeValues());
    }

    private void addValues(Collection<Location> values, int day, int hour, int level, Resource[] resources) {
        if (level == getResourceGroups().size()) {
            values.add(new Location(this, day, hour, resources.clone()));
            return;
        }
        Collection<Resource> rg = getResourceGroups().get(level);
        for (Resource r : rg) {
            if (r.isProhibited(day, hour, getLength()))
                continue;
            resources[level] = r;
            addValues(values, day, hour, level + 1, resources);
        }
    }

    public List<Location> computeValues() {
        List<Location> values = new ArrayList<Location>();
        Resource[] res = new Resource[getResourceGroups().size()];
        for (int day = 0; day < ((TimetableModel) getModel()).getNrDays(); day++)
            for (int hour = 0; hour <= ((TimetableModel) getModel()).getNrHours() - getLength(); hour++) {
                if (isProhibited(day, hour, getLength()))
                    continue;
                addValues(values, day, hour, 0, res);
            }
        return values;
    }
}
