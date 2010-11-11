package net.sf.cpsolver.coursett.constraint;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.model.Constraint;

/**
 * Class limit constraint.
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
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

public class ClassLimitConstraint extends Constraint<Lecture, Placement> {
    private int iClassLimit = 0;
    private Lecture iParent = null;
    private String iName = null;
    boolean iEnabled = true;

    private int iClassLimitDelta = 0;

    public ClassLimitConstraint(int classLimit, String name) {
        iClassLimit = classLimit;
        iName = name;
    }

    public ClassLimitConstraint(Lecture parent, String name) {
        iParent = parent;
        iName = name;
    }

    public int getClassLimitDelta() {
        return iClassLimitDelta;
    }

    public void setClassLimitDelta(int classLimitDelta) {
        iClassLimitDelta = classLimitDelta;
    }

    public int classLimit() {
        return (iParent == null ? iClassLimit + iClassLimitDelta : iParent.minClassLimit() + iClassLimitDelta);
    }

    public Lecture getParentLecture() {
        return iParent;
    }

    public int currentClassLimit(Placement value, Set<Placement> conflicts) {
        int limit = 0;
        for (Lecture lecture : variables()) {
            limit += lecture.classLimit(value, conflicts);
        }
        return limit;
    }

    @Override
    public void computeConflicts(Placement value, Set<Placement> conflicts) {
        if (!iEnabled)
            return;
        int currentLimit = currentClassLimit(value, conflicts);
        int classLimit = classLimit();
        if (currentLimit < classLimit) {
            // System.out.println(getName()+"> "+currentLimit+"<"+classLimit+" ("+value+")");
            TreeSet<Placement> adepts = new TreeSet<Placement>(new ClassLimitComparator());
            computeAdepts(adepts, variables(), value, conflicts);
            addParentAdepts(adepts, iParent, value, conflicts);
            // System.out.println(" -- found "+adepts.size()+" adepts");
            for (Placement adept : adepts) {
                // System.out.println("   -- selected "+adept);
                conflicts.add(adept);
                currentLimit = currentClassLimit(value, conflicts);
                // System.out.println("   -- new current limit "+currentLimit);
                if (currentLimit >= classLimit)
                    break;
            }
            // System.out.println(" -- done (currentLimit="+currentLimit+", classLimit="+classLimit+")");
        }

        if (currentLimit < classLimit)
            conflicts.add(value);

        if (iParent != null && iParent.getClassLimitConstraint() != null)
            iParent.getClassLimitConstraint().computeConflicts(value, conflicts);
    }

    public void computeAdepts(Collection<Placement> adepts, List<Lecture> variables, Placement value,
            Set<Placement> conflicts) {
        for (Lecture lecture : variables) {
            if (lecture.isCommitted()) continue;
            Placement placement = lecture.getAssignment();
            if (placement != null && !placement.equals(value) && !conflicts.contains(placement)) {
                adepts.add(placement);
            }
            if (lecture.hasAnyChildren()) {
                for (Long subpartId: lecture.getChildrenSubpartIds()) {
                    computeAdepts(adepts, lecture.getChildren(subpartId), value, conflicts);
                }
            }

        }
    }

    public void addParentAdepts(Collection<Placement> adepts, Lecture parent, Placement value, Set<Placement> conflicts) {
        if (parent == null || parent.isCommitted() || parent.minClassLimit() == parent.maxClassLimit())
            return;
        Placement placement = parent.getAssignment();
        if (placement != null && !placement.equals(value) && !conflicts.contains(placement)) {
            adepts.add(placement);
        }
        addParentAdepts(adepts, parent.getParent(), value, conflicts);
    }

    @Override
    public boolean inConflict(Placement value) {
        if (!iEnabled)
            return false;
        int currentLimit = currentClassLimit(value, null);
        int classLimit = classLimit();
        if (currentLimit < classLimit)
            return true;

        if (iParent != null && iParent.getClassLimitConstraint() != null)
            return iParent.getClassLimitConstraint().inConflict(value);

        return false;
    }

    @Override
    public String getName() {
        return iName;
    }

    private static class ClassLimitComparator implements Comparator<Placement> {
        public int compare(Placement p1, Placement p2) {
            Lecture l1 = p1.variable();
            Lecture l2 = p2.variable();
            int cl1 = Math.min(l1.maxClassLimit(), (int) Math.ceil(p1.minRoomSize() / l1.roomToLimitRatio()));
            int cl2 = Math.min(l2.maxClassLimit(), (int) Math.ceil(p2.minRoomSize() / l2.roomToLimitRatio()));
            int cmp = -Double.compare(l1.maxAchievableClassLimit() - cl1, l2.maxAchievableClassLimit() - cl2);
            if (cmp != 0)
                return cmp;
            return l1.getClassId().compareTo(l2.getClassId());
        }
    }

    public void setEnabled(boolean enabled) {
        iEnabled = enabled;
    }

    public boolean isEnabled() {
        return iEnabled;
    }

    @Override
    public String toString() {
        return "Class-limit " + getName();
    }

}
