package net.sf.cpsolver.ifs.example.tt;

import java.util.Set;

import net.sf.cpsolver.ifs.model.*;

/**
 * Binary dependence between two activities.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class Dependence extends BinaryConstraint<Activity, Location> {
    public static final int TYPE_NO_DEPENDENCE = 0;
    public static final int TYPE_BEFORE = 1;
    public static final int TYPE_CLOSELY_BEFORE = 2;
    public static final int TYPE_AFTER = 3;
    public static final int TYPE_CLOSELY_AFTER = 4;
    public static final int TYPE_CONCURRENCY = 5;
    private int iType = TYPE_NO_DEPENDENCE;
    private String iResourceId = null;

    public Dependence(String id, int type) {
        super();
        iType = type;
        iResourceId = id;
    }

    public int getType() {
        return iType;
    }

    public String getResourceId() {
        return iResourceId;
    }

    @Override
    public void computeConflicts(Location location, Set<Location> conflicts) {
        Activity activity = location.variable();
        Activity another = another(activity);
        Location anotherLocation = another.getAssignment();
        if (anotherLocation == null)
            return;
        if (isFirst(activity)) {
            if (!isConsistent(location.getSlot(), activity.getLength(), anotherLocation.getSlot(), another.getLength()))
                conflicts.add(anotherLocation);
        } else {
            if (!isConsistent(anotherLocation.getSlot(), another.getLength(), location.getSlot(), activity.getLength()))
                conflicts.add(anotherLocation);
        }
    }

    public boolean isConsistent(int s1, int l1, int s2, int l2) {
        switch (iType) {
            case TYPE_BEFORE:
                return s1 + l1 <= s2;
            case TYPE_CLOSELY_BEFORE:
                return s1 + l1 == s2;
            case TYPE_AFTER:
                return s2 + l2 <= s1;
            case TYPE_CLOSELY_AFTER:
                return s2 + l2 == s1;
            case TYPE_CONCURRENCY:
                return (s1 <= s2 && s2 + l2 <= s1 + l1) || (s2 <= s1 && s1 + l1 <= s2 + l2);
            default:
                return true;
        }
    }

    @Override
    public boolean inConflict(Location location) {
        Activity activity = location.variable();
        Activity another = another(activity);
        Location anotherLocation = another.getAssignment();
        if (anotherLocation == null)
            return false;
        if (isFirst(activity)) {
            return !isConsistent(location.getSlot(), activity.getLength(), anotherLocation.getSlot(), another
                    .getLength());
        } else {
            return !isConsistent(anotherLocation.getSlot(), another.getLength(), location.getSlot(), activity
                    .getLength());
        }
    }

    @Override
    public boolean isConsistent(Location l1, Location l2) {
        Activity a1 = l1.variable();
        Activity a2 = l2.variable();
        if (isFirst(a1)) {
            return !isConsistent(l1.getSlot(), a1.getLength(), l2.getSlot(), a2.getLength());
        } else {
            return !isConsistent(l2.getSlot(), a2.getLength(), l1.getSlot(), a1.getLength());
        }
    }

    @Override
    public String getName() {
        switch (iType) {
            case TYPE_BEFORE:
                return first().getName() + "<" + second().getName();
            case TYPE_CLOSELY_BEFORE:
                return first().getName() + "<|" + second().getName();
            case TYPE_AFTER:
                return first().getName() + ">" + second().getName();
            case TYPE_CLOSELY_AFTER:
                return first().getName() + "|>" + second().getName();
            case TYPE_CONCURRENCY:
                return first().getName() + "||" + second().getName();
            default:
                return first().getName() + "?" + second().getName();
        }
    }

}
