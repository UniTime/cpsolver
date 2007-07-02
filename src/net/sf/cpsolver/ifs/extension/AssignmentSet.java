package net.sf.cpsolver.ifs.extension;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * This class describing a set of assignment (used by CBS).
 *
 * It also contains a counter, name, description and a constraint (for printing purposes).
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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

public class AssignmentSet {
    private Vector iSet = new FastVector();
    private int iCounter = 1;
    private String iName = null;
    private String iDescription = null;
    private Constraint iConstraint = null;
    
    public AssignmentSet() {}
    public AssignmentSet(Assignment[] assignments) {
        for (int i = 0; i < assignments.length; i++)
            iSet.addElement(assignments[i]);
    }
    public AssignmentSet(Collection assignments) {
        for (Iterator i = assignments.iterator(); i.hasNext();) {
            iSet.addElement((Assignment)i.next());
        }
    }
    
    /** Create set of assignments from the list of Assignments, Values or (assigned) Variables */
    public static AssignmentSet createAssignmentSet(long iteration, Collection assignments, double ageing) {
        AssignmentSet set = new AssignmentSet();
        for (Iterator i = assignments.iterator(); i.hasNext();) {
            Object o = (Object)i.next();
            if (o instanceof Assignment)
                set.addAssignment((Assignment)o);
            if (o instanceof Value)
                set.addAssignment(new Assignment(iteration, ((Value)o), ageing));
            if (o instanceof Variable && ((Variable)o).getAssignment() != null)
                set.addAssignment(new Assignment(iteration, ((Variable)o).getAssignment(), ageing));
        }
        return set;
    }
    
    /** Increment counter*/
    public void incCounter() {
        iCounter++;
    }
    /** Returns counter */
    public int getCounter() {
        return iCounter;
    }
    /** Returns set of assignments */
    public Vector getSet() {
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
    public Constraint getConstraint() {
        return iConstraint;
    }
    /** Sets constraint */
    public void setConstraint(Constraint constraint) {
        iConstraint = constraint;
    }
    /** Returns true if it contains the given assignment */
    public boolean contains(Assignment assignment) {
        return iSet.contains(assignment);
    }
    /* Returns true if it contains all of the given assignments */
    public boolean contains(AssignmentSet assignmentSet) {
        return iSet.containsAll(assignmentSet.getSet());
    }
    /** Returns true if it contains the given assignment */
    public boolean contains(Value value) {
        return iSet.contains(new Assignment(0l, value, 1.0));
    }
    /** Returns true if it contains the given assignment (assigned variable) */
    public boolean contains(Variable variable) {
        return (variable.getAssignment() == null ? false : iSet.contains(new Assignment(0l, variable.getAssignment(), 1.0)));
    }
    /* Returns true if it contains all of the given assignments */
    public boolean contains(Collection assignments) {
        for (Iterator i = assignments.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o == null)
                return false;
            if (o instanceof Assignment && !iSet.contains((Assignment)o))
                return false;
            if (o instanceof Value || !iSet.contains(new Assignment(0l, ((Value)o), 1.0)))
                return false;
            if (o instanceof Variable && (((Variable)o).getAssignment() == null || !iSet.contains(new Assignment(0l, ((Variable)o).getAssignment(), 1.0))))
                return false;
        }
        return true;
    }
    
    /** Adds an assignment */
    public void addAssignment(Assignment assignment) {
        if (!contains(assignment))
            iSet.addElement(assignment);
    }
    /** Adds an assignment */
    public void addAssignment(long iteration, Value value, double ageing) {
        addAssignment(new Assignment(iteration, value, ageing));
    }
    /** Returns assignment that corresponds to the given value (if it is present in the set) */
    public Assignment getAssignment(Value value) {
        for (Enumeration i = getSet().elements(); i.hasMoreElements();) {
            Assignment a = (Assignment)i.nextElement();
            if (a.getValue().getId() == value.getId())
                return a;
        }
        return null;
    }
    /** Returns number of assignments in the set*/    
    public int size() {
        return getSet().size();
    }
    /** Compares two assignment sets -- name, size and content (assignments) has to match. */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof AssignmentSet) {
            AssignmentSet as = (AssignmentSet)o;
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
        if (o instanceof Collection) {
            Collection c = (Collection)o;
            if (c.size() != getSet().size())
                return false;
            return contains(c);
        }
        return false;
    }
    
    public static int xor(int a, int b) {
        return (a | b) & (~a | ~b);
    }
    
    public int hashCode() {
        int ret = getSet().size();
        for (Enumeration i = getSet().elements(); i.hasMoreElements();) {
            Assignment a = (Assignment)i.nextElement();
            ret = xor(ret, a.hashCode());
        }
        return ret;
    }
}
