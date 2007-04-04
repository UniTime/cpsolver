package net.sf.cpsolver.ifs.multi;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.constant.ConstantVariable;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.model.VariableListener;

/**
 * Multi-variable model.
 * 
 * Model containing {@link MultiVariable} variables.
 * 
 * Such variables are excluded from the solver process, however, 
 * they can be included in constraints. Such model can allow us to build a solution
 * on top of another solution (e.g., committed classes in the course timetabling).
 * 
 * Constant variable has to implement interface {@link ConstantVariable}, 
 * returning {@link ConstantVariable#isConstant()} true.
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
 *  
 */

public class MultiModel extends Model {
	public MultiModel() {
		super();
		
	}
	
	public void addVariable(Variable variable) {
		super.addVariable(variable);
		if (variable instanceof MultiVariable) {
			MultiVariable mVar = (MultiVariable)variable;
			for (Enumeration e=mVar.variables().elements();e.hasMoreElements();) {
				Variable var = (Variable)e.nextElement();
				var.addVariableListener(mVar);
			}
		}
	}
	
	public void removeVariable(Variable variable) {
		super.removeVariable(variable);
		if (variable instanceof MultiVariable) {
			MultiVariable mVar = (MultiVariable)variable;
			for (Enumeration e=mVar.variables().elements();e.hasMoreElements();) {
				Variable var = (Variable)e.nextElement();
				var.removeVariableListener(mVar);
			}
		}
	}
	
	/** Returns a {@link MultiVariable} for the given "normal" variable. */ 
	public MultiVariable getMultiVariable(Variable variable) {
		if (variable.getVariableListeners()==null || variable.getVariableListeners().isEmpty()) return null;
		for (Enumeration e=variable.getVariableListeners().elements();e.hasMoreElements();) {
			VariableListener l = (VariableListener)e.nextElement();
			if (l instanceof MultiVariable) return (MultiVariable)l;
		}
		return null;
	}

	public Set conflictValues(Value value) {
    	if (!(value instanceof MultiValue)) return super.conflictValues(value);
        HashSet conflictValues = new HashSet();
        MultiValue mValue = (MultiValue)value;
        int idx = 0;
        for (Enumeration e=((MultiVariable)value.variable()).variables().elements(); e.hasMoreElements();idx++) {
        	Variable var = (Variable)e.nextElement();
        	if (mValue.values()[idx]==null) continue;
        	for (Enumeration c=var.hardConstraints().elements(); c.hasMoreElements();)
                ((Constraint)c.nextElement()).computeConflicts(mValue.values()[idx], conflictValues);
        }
        for (Enumeration c=globalConstraints().elements(); c.hasMoreElements();)
            ((GlobalConstraint)c.nextElement()).computeConflicts(mValue.values()[idx], conflictValues);
        HashSet conflictMultiValues = new HashSet();
        for (Iterator i=conflictValues.iterator();i.hasNext();) {
        	Value confValue = (Value)i.next();
        	conflictMultiValues.add(confValue.getExtra());
        			//getMultiVariable(confValue.variable()).getAssignment());
        }
        return conflictValues;
    }
	
    public Hashtable conflictConstraints(Value value) {
    	if (!(value instanceof MultiValue)) return super.conflictConstraints(value);
        Hashtable conflictConstraints = new Hashtable();
        MultiValue mValue = (MultiValue)value;
        int idx = 0;
        for (Enumeration e=((MultiVariable)value.variable()).variables().elements(); e.hasMoreElements();idx++) {
        	Variable var = (Variable)e.nextElement();
        	if (mValue.values()[idx]==null) continue;
            for (Enumeration c=var.hardConstraints().elements(); c.hasMoreElements();) {
                Constraint constraint = (Constraint)c.nextElement();
                HashSet conflicts = new HashSet();
                constraint.computeConflicts(mValue.values()[idx], conflicts);
                if (conflicts!=null && !conflicts.isEmpty()) {
                    HashSet mConflicts = new HashSet();
                    for (Iterator i=conflicts.iterator();i.hasNext();) {
                    	Value confValue = (Value)i.next();
                    	mConflicts.add(confValue.getExtra());
                    }
                    conflictConstraints.put(constraint,mConflicts);
                }
            }
        }
        for (Enumeration c=globalConstraints().elements(); c.hasMoreElements();) {
            GlobalConstraint constraint = (GlobalConstraint)c.nextElement();
            HashSet conflicts = new HashSet();
            constraint.computeConflicts(mValue.values()[idx], conflicts);
            if (conflicts!=null && !conflicts.isEmpty()) {
                HashSet mConflicts = new HashSet();
                for (Iterator i=conflicts.iterator();i.hasNext();) {
                    Value confValue = (Value)i.next();
                    mConflicts.add(confValue.getExtra());
                }
                conflictConstraints.put(constraint,mConflicts);
            }
        }
        return conflictConstraints;
    }
}
