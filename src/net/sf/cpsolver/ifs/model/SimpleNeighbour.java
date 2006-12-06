package net.sf.cpsolver.ifs.model;

/**
 * A neighbour consisting of a change (either assignment or unassignment) of a single variable. 
 *
 * @see net.sf.cpsolver.ifs.heuristics.NeighbourSelection
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

public class SimpleNeighbour extends Neighbour {
	private Variable iVariable = null;
	private Value iValue = null;
	
	/** Model
	 * @param variable variable to be assigned
	 * @param value value to be assigned to the given variable, null if the variable should be unassigned
	 */
	public SimpleNeighbour(Variable variable, Value value) {
		iVariable = variable;
		iValue = value;
	}
	
	/** Perform assignment */ 
	public void assign(long iteration) {
		if (iVariable==null) return;
		if (iValue!=null)
			iVariable.assign(iteration, iValue);
		else
			iVariable.unassign(iteration);
	}
	
	public String toString() {
		return iVariable.getName()+(iValue==null?"":" "+iValue.getName());
	}
}
