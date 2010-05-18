package net.sf.cpsolver.ifs.extension;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;

/**
 * This class describing a set of assignment (used by CBS).
 * 
 * It also contains a counter, name, description and a constraint (for printing
 * purposes).
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

public class AssignmentSet<T extends Value<?, T>> {
    private List<Assignment<T>> iSet = new ArrayList<Assignment<T>>();
    private int iCounter = 1;
    private String iName = null;
    private String iDescription = null;
    private Constraint<?, T> iConstraint = null;

    public AssignmentSet() {
    }

    public AssignmentSet(Assignment<T>[] assignments) {
        for (Assignment<T> a : assignments)
            iSet.add(a);
    }

    public AssignmentSet(Collection<Assignment<T>> assignments) {
        for (Assignment<T> a : assignments)
            iSet.add(a);
    }

    /**
     * Create set of assignments from the list of Assignments, Values or
     * (assigned) Variables
     */
    public static <T extends Value<?, T>> AssignmentSet<T> createAssignmentSet(Collection<Assignment<T>> assignments) {
        AssignmentSet<T> set = new AssignmentSet<T>();
        for (Assignment<T> a : assignments)
            set.addAssignment(a);
        return set;
    }

    /**
     * Create set of assignments from the list of Assignments, Values or
     * (assigned) Variables
     */
    public static <T extends Value<?, T>> AssignmentSet<T> createAssignmentSetForValues(Collection<T> assignments) {
        AssignmentSet<T> set = new AssignmentSet<T>();
        for (T a : assignments)
            set.addAssignment(0l, a, 1.0);
        return set;
    }

    /**
     * Create set of assignments from the list of Assignments, Values or
     * (assigned) Variables
     */
    public static <T extends Value<?, T>> AssignmentSet<T> createAssignmentSetForVariables(
            Collection<Variable<?, T>> assignments) {
        AssignmentSet<T> set = new AssignmentSet<T>();
        for (Variable<?, T> a : assignments)
            if (a.getAssignment() != null)
                set.addAssignment(0, a.getAssignment(), 1.0);
        return set;
    }

    /** Increment counter */
    public void incCounter() {
        iCounter++;
    }

    /** Returns counter */
    public int getCounter() {
        return iCounter;
    }

    /** Returns set of assignments */
    public List<Assignment<T>> getSet() {
        return iSet;
    }

    /** Returns name */
    public String getName() {
        return iName;
    }

    /** Sets name */
    public void setName(String name) {
        iName = name;
    }

    /** Returns description */
    public String getDescription() {
        return iDescription;
    }

    /** Sets description */
    public void setDescription(String description) {
        iDescription = description;
    }

    /** Returns constraint */
    public Constraint<?, T> getConstraint() {
        return iConstraint;
    }

    /** Sets constraint */
    public void setConstraint(Constraint<?, T> constraint) {
        iConstraint = constraint;
    }

    /** Returns true if it contains the given assignment */
    public boolean contains(Assignment<T> assignment) {
        return iSet.contains(assignment);
    }

    /** Returns true if it contains all of the given assignments */
    public boolean contains(AssignmentSet<T> assignmentSet) {
        return iSet.containsAll(assignmentSet.getSet());
    }

    /** Returns true if it contains the given assignment */
    public boolean contains(T value) {
        return iSet.contains(new Assignment<T>(0l, value, 1.0));
    }

    /** Returns true if it contains the given assignment (assigned variable) */
    public boolean contains(Variable<?, T> variable) {
        return (variable.getAssignment() == null ? false : iSet.contains(new Assignment<T>(0l,
                variable.getAssignment(), 1.0)));
    }

    /** Returns true if it contains all of the given assignments */
    public boolean contains(Collection<Assignment<T>> assignments) {
        for (Assignment<T> a : assignments)
            if (!iSet.contains(a))
                return false;
        return true;
    }

    /** Returns true if it contains all of the given assignments */
    public boolean containsValues(Collection<T> assignments) {
        for (T a : assignments)
            if (!iSet.contains(new Assignment<T>(0l, a, 1.0)))
                return false;
        return true;
    }

    /** Returns true if it contains all of the given assignments */
    public boolean containsVariables(Collection<Variable<?, T>> assignments) {
        for (Variable<?, T> a : assignments)
            if (a.getAssignment() == null || !iSet.contains(new Assignment<T>(0l, a.getAssignment(), 1.0)))
                return false;
        return true;
    }

    /** Adds an assignment */
    public void addAssignment(Assignment<T> assignment) {
        if (!contains(assignment))
            iSet.add(assignment);
    }

    /** Adds an assignment */
    public void addAssignment(long iteration, T value, double ageing) {
        addAssignment(new Assignment<T>(iteration, value, ageing));
    }

    /**
     * Returns assignment that corresponds to the given value (if it is present
     * in the set)
     */
    public Assignment<T> getAssignment(T value) {
        for (Assignment<T> a : iSet)
            if (a.getValue().getId() == value.getId())
                return a;
        return null;
    }

    /** Returns number of assignments in the set */
    public int size() {
        return getSet().size();
    }

    /**
     * Compares two assignment sets -- name, size and content (assignments) has
     * to match.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof AssignmentSet<?>) {
            AssignmentSet<T> as = (AssignmentSet<T>) o;
            if (getName() == null && as.getName() != null)
                return false;
            if (getName() != null && as.getName() == null)
                return false;
            if (getName() != null && !getName().equals(as.getName()))
                return false;
            if (as.getSet().size() != getSet().size())
                return false;
            return contains(as);
        }
        if (o instanceof Collection<?>) {
            Collection<Assignment<T>> c = (Collection<Assignment<T>>) o;
            if (c.size() != getSet().size())
                return false;
            return contains(c);
        }
        return false;
    }

    public static int xor(int a, int b) {
        return (a | b) & (~a | ~b);
    }

    @Override
    public int hashCode() {
        int ret = getSet().size();
        for (Assignment<T> a : iSet)
            ret = xor(ret, a.hashCode());
        return ret;
    }
}
