package net.sf.cpsolver.ifs.example.tt;

import net.sf.cpsolver.ifs.model.*;

/**
 * Location (value, i.e., a single placement of the activity).
 * Location encodes a slot and a selection of resources.
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
public class Location extends Value {
    private int iSlot;
    private Resource[] iResources;
    private int iNrOfDiscouragedSlots = -1;
    
    /** 
     * Constructor.
     * @param activity parent activity
     * @param slot starting time
     * @param resources selection of resources
     */
    public Location(Activity activity, int slot, Resource[] resources) {
        super(activity);
        iSlot = slot;
        iResources = resources;
        iNrOfDiscouragedSlots = computeNrOfDiscouragedSlots();
    }
    
    /** 
     * Constructor. slot = nrHours * day + hour
     * @param activity parent activity
     * @param day day
     * @param hour starting hour
     * @param resources required resources
     */
    public Location(Activity activity, int day, int hour, Resource[] resources) {
        super(activity);
        iSlot = ((TimetableModel)activity.getModel()).getNrHours()*day + hour;
        iResources = resources;
        iNrOfDiscouragedSlots = computeNrOfDiscouragedSlots();
    }

    /** Gets slot */
    public int getSlot() { return iSlot; }
    /** Gets selection of resources */
    public Resource[] getResources() { return iResources; }
    /** Gets given resource */
    public Resource getResource(int idx) { return iResources[idx]; }
    /** Returns true if the given resource is used by this location */ 
    public boolean containResource(Resource resource) {
        for (int i=0;i<iResources.length;i++)
            if (iResources[i].equals(resource)) return true;
        return false;
    }
    
    /** Number of slots (over all resources) which are discouraged */
    public int getNrOfDiscouragedSlots() { return iNrOfDiscouragedSlots; }
    /** Int value (for optimization) -- getNrOfDiscouragedSlots() is returned */
    public double toDouble() { return iNrOfDiscouragedSlots; }
    /** Computes number of discouraged slots (over all resources and the activity) */
    public int computeNrOfDiscouragedSlots() {
        Activity a = (Activity)variable();
        int ret = 0;
        for (int i=getSlot();i<getSlot()+a.getLength();i++) {
            if (a.isDiscouragedSlot(i)) ret++;
            for (int j=0;j<getResources().length;j++)
                if (getResource(j).isDiscouragedSlot(i)) ret++;
        }
        return ret;
    }
    /**
     * Returns true if the location intersects with another location.
     * This means the same resource is used in the same time.
     */
    public boolean hasIntersection(Location location) {
        int s1 = getSlot();
        int l1 = ((Activity)variable()).getLength();
        int s2 = location.getSlot();
        int l2 = ((Activity)location.variable()).getLength();
        return !(s1+l1<=s2 || s2+l2<=s1);
    }
    /**
     * Returns true if the location is prohibited.
     * This means that the activity or a required resource has a time slot which is used by this location prohibited.
     */
    public boolean isProhibited() {
        Activity a = (Activity)variable();
        for (int i=getSlot();i<getSlot()+a.getLength();i++) {
            if (a.isProhibitedSlot(i)) return true;
            for (int j=0;j<getResources().length;j++)
                if (getResource(j).isProhibitedSlot(i)) return true;
        }
        return false;
    }
    
    public String getName() {
        StringBuffer sb = new StringBuffer(getSlot()+"/");
        for (int i=0;i<iResources.length;i++) {
            if (i>0) sb.append(",");
            sb.append(iResources[i].getName());
        }
        return sb.toString();
    }
}
