package org.cpsolver.ifs.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Value;


/**
 * This class describing a set of assignment (used by CBS).
 * 
 * It also contains a counter, name, description and a constraint (for printing
 * purposes).
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
 * @param <T> Value
 */
public class AssignedValueSet<T extends Value<?, T>> {
    private List<AssignedValue<T>> iSet = new ArrayList<AssignedValue<T>>();
    private int iCounter = 1;
    private String iName = null;
    private String iDescription = null;
    private Constraint<?, T> iConstraint = null;

    public AssignedValueSet() {
    }

    public AssignedValueSet(AssignedValue<T>[] assignments) {
        for (AssignedValue<T> a : assignments)
            iSet.add(a);
    }

    public AssignedValueSet(Collection<AssignedValue<T>> assignments) {
        for (AssignedValue<T> a : assignments)
            iSet.add(a);
    }

    /**
     * Create set of assignments from the list of Assignments, Values or
     * (assigned) Variables
     * @param assignments list of assignments
     * @param <T> Value
     * @return a set of assignments
     */
    public static <T extends Value<?, T>> AssignedValueSet<T> createAssignmentSet(Collection<AssignedValue<T>> assignments) {
        AssignedValueSet<T> set = new AssignedValueSet<T>();
        for (AssignedValue<T> a : assignments)
            set.addAssignment(a);
        return set;
    }

    /**
     * Create set of assignments from the list of Assignments, Values or
     * (assigned) Variables
     * @param assignments list of assignments
     * @param <T> Value
     * @return a set of assignments
     */
    public static <T extends Value<?, T>> AssignedValueSet<T> createAssignmentSetForValues(Collection<T> assignments) {
        AssignedValueSet<T> set = new AssignedValueSet<T>();
        for (T a : assignments)
            set.addAssignment(0l, a, 1.0);
        return set;
    }

    /** Increment counter */
    public void incCounter() {
        iCounter++;
    }

    /** Returns counter 
     * @return counter
     **/
    public int getCounter() {
        return iCounter;
    }

    /** Returns set of assignments 
     * @return assignments in the set
     **/
    public List<AssignedValue<T>> getSet() {
        return iSet;
    }

    /** Returns name
     * @return a name
     **/
    public String getName() {
        return iName;
    }

    /** Sets name
     * @param name a name
     **/
    public void setName(String name) {
        iName = name;
    }

    /** Returns description
     * @return a description
     **/
    public String getDescription() {
        return iDescription;
    }

    /** Sets description
     * @param description a description
     **/
    public void setDescription(String description) {
        iDescription = description;
    }

    /** Returns constraint
     * @return a constraint 
     **/
    public Constraint<?, T> getConstraint() {
        return iConstraint;
    }

    /** Sets constraint
     * @param constraint a constraint 
     **/
    public void setConstraint(Constraint<?, T> constraint) {
        iConstraint = constraint;
    }

    /** Returns true if it contains the given assignment
     * @param assignment an assignment
     * @return true if in the set
     **/
    public boolean contains(AssignedValue<T> assignment) {
        return iSet.contains(assignment);
    }

    /** Returns true if it contains all of the given assignments
     * @param assignmentSet a set of assignments
     * @return true if all in the set
     **/
    public boolean contains(AssignedValueSet<T> assignmentSet) {
        return iSet.containsAll(assignmentSet.getSet());
    }

    /** Returns true if it contains the given assignment
     * @param value an assignment (of the value to its variable)
     * @return true if in the set
     **/
    public boolean contains(T value) {
        return iSet.contains(new AssignedValue<T>(0l, value, 1.0));
    }

    /** Returns true if it contains all of the given assignments
     * @param assignments a set of assignments
     * @return true if all in the set
     **/
    public boolean contains(Collection<AssignedValue<T>> assignments) {
        for (AssignedValue<T> a : assignments)
            if (!iSet.contains(a))
                return false;
        return true;
    }

    /** Returns true if it contains all of the given assignments
     * @param assignments a set of assignments (values to their variables)
     * @return true if all in the set
     **/
    public boolean containsValues(Collection<T> assignments) {
        for (T a : assignments)
            if (!iSet.contains(new AssignedValue<T>(0l, a, 1.0)))
                return false;
        return true;
    }

    /** Adds an assignment
     * @param assignment an assignment
     **/
    public void addAssignment(AssignedValue<T> assignment) {
        if (!contains(assignment))
            iSet.add(assignment);
    }

    /** Adds an assignment
     * @param iteration current iteration
     * @param value an assignment
     * @param aging aging factor
     **/
    public void addAssignment(long iteration, T value, double aging) {
        addAssignment(new AssignedValue<T>(iteration, value, aging));
    }

    /**
     * Returns assignment that corresponds to the given value (if it is present
     * in the set)
     * @param value an assignment
     * @return a corresponding assignment of the set, null if not present
     */
    public AssignedValue<T> getAssignment(T value) {
        for (AssignedValue<T> a : iSet)
            if (a.getValue().getId() == value.getId())
                return a;
        return null;
    }

    /** Returns number of assignments in the set 
     * @return number of assignments in the set
     **/
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
        if (o instanceof AssignedValueSet<?>) {
            AssignedValueSet<T> as = (AssignedValueSet<T>) o;
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
            Collection<AssignedValue<T>> c = (Collection<AssignedValue<T>>) o;
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
        for (AssignedValue<T> a : iSet)
            ret = xor(ret, a.hashCode());
        return ret;
    }
}
