package net.sf.cpsolver.ifs.extension;


import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Computation of violated initial values (minimal perturbation problem).
 * <br><br>
 * It is using {@link Constraint#isConsistent(Value, Value)} to find out what 
 * initial values (of different variables) cannot be assigned when an arbitrary value is
 * assigned to a variable. This information is computed in advance, before the solver is
 * executed. It is used for better estimation of perturbation penalty (see 
 * {@link net.sf.cpsolver.ifs.perturbations.PerturbationsCounter}) when a value is to be assigned to a variable.
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
public class ViolatedInitials extends Extension {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(ViolatedInitials.class);
    private Hashtable iViolatedInitials = new Hashtable();
    
    public ViolatedInitials(Solver solver, DataProperties properties) {
        super(solver, properties);
    }
    
    /** Compute the violations between any value and all other initial values */
    public boolean init() {
        sLogger.info("Computation of violated initials enabled.");
        for (Enumeration i = getModel().variables().elements(); i.hasMoreElements();) {
            Variable variable = (Variable)i.nextElement();
            if (variable.getInitialAssignment() == null) continue;
            for (Enumeration i1 = variable.constraints().elements(); i1.hasMoreElements();) {
                Constraint constraint = (Constraint)i1.nextElement();
                Vector conflicts = conflictValues(constraint, variable.getInitialAssignment());
                for (Enumeration i2 = conflicts.elements(); i2.hasMoreElements(); ) {
                    Value value = (Value)i2.nextElement();
                    addViolatedInitial(value, variable.getInitialAssignment());
                }
            }
        }
        return true;
    }
    
    /** Initial values that cannot be assigned when the given value is assigned */
    public Set getViolatedInitials(Value value) {
        return (Set)iViolatedInitials.get(value);
    }
    
    private void addViolatedInitial(Value value, Value anotherValue) {
        Set violations = (Set)iViolatedInitials.get(value);
        if (violations == null) {
            violations = new HashSet();
            iViolatedInitials.put(value, violations);
        }
        violations.add(anotherValue);
    }
    
    private Vector conflictValues(Constraint constraint, Value aValue) {
        Vector ret = new FastVector();
        for (Enumeration i1 = constraint.variables().elements();i1.hasMoreElements();) {
            Variable variable = (Variable)i1.nextElement();
            if (variable.equals(aValue.variable())) continue;
            if (variable.getAssignment() != null) continue;
            for (Enumeration i2 = variable.values().elements(); i2.hasMoreElements(); ) {
                Value value = (Value)i2.nextElement();
                if (!constraint.isConsistent(aValue, value))
                    ret.addElement(value);
            }
        }
        return ret;
    }
}
