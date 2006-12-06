package net.sf.cpsolver.ifs.multi;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * A value of {@link MultiVariable}. Such value contains a value for every "normal" variable of {@link MultiVariable}. 
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

public class MultiValue extends Value {
	private Value[] iValues = null;
	private Double iDoubleValue = null;
	private int iHashCode = 0;
	
	/** Constructor
	 * @param variable multi variable
	 * @param values a value for every "normal" variable of the multi variable
	 */
	public MultiValue(MultiVariable variable, Value[] values) {
		this(variable, values, null, false);
	}
	
	/** Constructor
	 * @param variable multi variable
	 * @param values a value for every "normal" variable of the multi variable
	 * @param doubleValue valute to be returned as {@link MultiValue#toDouble()}
	 * @param correctOrder indicates whether the givan values are in the correct order (first value of the first variable etc.)
	 */
	public MultiValue(MultiVariable variable, Value[] values, Double doubleValue, boolean correctOrder) {
		super(variable);
		iValues = new Value[values.length];
		for (int i=0;i<values.length;i++) {
			iValues[i] = values[i];
			if (values[i]!=null) values[i].setExtra(this);
		}
		iDoubleValue = doubleValue;
		if (correctOrder) {
			for (int i=0;i<iValues.length;i++) {
				Value val = iValues[i];
				int idx = variable.variables().indexOf(val.variable());
				if (i!=idx) {
					iValues[i]=iValues[idx]; iValues[idx]=val;
				}
			}
		}
		iHashCode = getName().hashCode();
	}
	
	public Value[] values() { return iValues; }
	public int size() { return iValues.length; }
	public int nrAssigned() {
		int ret = 0;
		for (int i=0;i<iValues.length;i++)
			if (iValues[i]!=null) ret++;
		return ret;
	}
	
	public String getName() {
		StringBuffer sb = new StringBuffer("[");
		for (int i=0;i<iValues.length;i++) {
			if (i>0) sb.append(",");
			sb.append(iValues[i]==null?"null":iValues[i].getName());
		}
		sb.append("]");
		return sb.toString();
	}
	
	public String toString() {
		return getName();
	}
	
	public double toDouble() {
		if (iDoubleValue!=null) return iDoubleValue.intValue();
		int ret = 0;
		for (int i=0;i<iValues.length;i++) {
			ret += (iValues[i]==null?0:iValues[i].toDouble());
		}
		return ret;
	}
	
	public int hashCode() {
		return iHashCode;
	}

	public boolean equals(Object o) {
		if (o==null || !(o instanceof MultiValue)) return false;
		MultiValue m = (MultiValue)o;
		if (!m.variable().equals(variable())) return false;
		for (int i=0;i<iValues.length;i++)
			if (!ToolBox.equals(iValues[i],m.values()[i])) return false;
		return true;
	}
	
	public Object clone() {
		return new MultiValue((MultiVariable)variable(), iValues, iDoubleValue, false);
	}
}
