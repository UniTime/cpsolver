package net.sf.cpsolver.ifs.multi;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.model.VariableListener;

/**
 * A variable containing multiple "normal" variables.
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

public class MultiVariable extends Variable implements VariableListener {
	private Vector iVariables;
	public static boolean sCacheValues = false;
	
	/** Constructor
	 * @param variables list of "normal" variables
	 */
	public MultiVariable(Vector variables) {
		super();
		iVariables = variables;
		setValues(null);
	}
	public MultiVariable() {
		this(new Vector());
	}
	
	/** Add "normal" variable */
	public void addVariable(Variable variable) { variables().add(variable); setValues(null); }
	/** Remove "normal" variable */
	public void removeVariable(Variable variable) { variables().remove(variable); setValues(null); }
	/** List of "normal" variables */
	public Vector variables() { return iVariables; }
	/** Number of "normal" variables */
	public int size() { return variables().size(); }
	
	private void computeValues(Vector ret, Value[] values, int idx) {
		if (idx==ret.size()) {
			ret.addElement(new MultiValue(this, (Value[])values.clone()));
			return;
		}
		Variable var = (Variable)variables().elementAt(idx);
		for (Enumeration e=var.values().elements();e.hasMoreElements();) {
			Value value = (Value)e.nextElement();
			boolean inConflict = false;
			for (int f=0;f<idx;f++) {
				if (values[f]!=null && !values[f].isConsistent(value)) {
					inConflict = true; break;
				}
			}
			if (inConflict) continue;
			values[idx]=value;
			computeValues(ret, values, idx+1);
			values[idx]=null;
		}
	}

	/** Variable domain -- Cartesian product of the domains of the "normal" variables */ 
	public Vector computeValues() {
		Vector values = new Vector();
		computeValues(values, new Value[size()], 0);
		return values;
	}
	
	public Vector values() {
		if (sCacheValues) {
			Vector ret = super.values();
			if (ret==null) {
				ret = computeValues();
				setValues(ret);
			}
			return ret;
		} else
			return computeValues();
	}
	
	public void assign(long iteration, Value value) {
		MultiValue m = (MultiValue)value;
		for (int i=0;i<size();i++) {
			Variable var = (Variable)variables().elementAt(i);
			if (var.getAssignment()!=null) var.unassign(iteration);
		}
		for (int i=0;i<size();i++) {
			Variable var = (Variable)variables().elementAt(i);
			if (m.values()[i]!=null)
				var.assign(iteration, m.values()[i]);
		}
		super.assign(iteration, m);
	}
	
	public void unassign(long iteration) {
		for (Enumeration e=variables().elements();e.hasMoreElements();) {
			Variable var = (Variable)e.nextElement();
			if (var.getAssignment()!=null)
				var.unassign(iteration);
		}
		super.unassign(iteration);
	}
	
	public String getName() {
		StringBuffer sb = new StringBuffer("[");
		for (Enumeration e=variables().elements();e.hasMoreElements();) {
			Variable v = (Variable)e.nextElement();
			sb.append(v==null?"null":v.getName());
			if (e.hasMoreElements()) sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
	
	
	public void variableAssigned(long iteration, Value value) {}
	public void variableUnassigned(long iteration, Value value) {
		MultiValue mValue = (MultiValue)value.getExtra();
		if (mValue!=null && mValue.variable().getAssignment()!=null)
			mValue.variable().unassign(iteration);
	}
	public void valueRemoved(long iteration, Value value) {}
}
