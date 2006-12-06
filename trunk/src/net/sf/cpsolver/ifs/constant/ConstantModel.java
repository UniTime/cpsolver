package net.sf.cpsolver.ifs.constant;

import java.util.Vector;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.FastVector;

/**
 * Extension of the model with constant variables.
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

public class ConstantModel extends Model {
	private Vector iConstantVariables = null;
	
	/** List of constant variables */
	public Vector constantVariables() { return iConstantVariables; }
	
	/** True, if the model contains at least one constant variable. */
	public boolean hasConstantVariables() { return iConstantVariables!=null && !iConstantVariables.isEmpty(); }
	
	/** True, if the given variable is constant. */
	public boolean isConstant(Variable variable) {
		return (iConstantVariables!=null && variable instanceof ConstantVariable && ((ConstantVariable)variable).isConstant());
	}
	
    /** Adds a variable to the model */
    public void addVariable(Variable variable) {
    	if (variable instanceof ConstantVariable && ((ConstantVariable)variable).isConstant()) {
    		if (iConstantVariables==null) iConstantVariables = new FastVector();
    		variable.setModel(this);
    		iConstantVariables.addElement(variable);
    		if (variable.getAssignment()!=null) variable.assign(0L,variable.getAssignment());
    	} else super.addVariable(variable);
    }
    
    /** Removes a variable from the model */
    public void removeVariable(Variable variable) {
    	if (isConstant(variable)) {
    		variable.setModel(null);
    		iConstantVariables.removeElement(variable);
    	} else super.removeVariable(variable);
    }
    
    /** Called before a value is assigned to its variable. Constant variables are excluded from (re)assignment. */
    public void beforeAssigned(long iteration, Value value) {
    	if (!isConstant(value.variable())) super.beforeAssigned(iteration, value);
    }
    
    /** Called before a value is unassigned from its variable. Constant variables are excluded from (re)assignment. */
    public void beforeUnassigned(long iteration, Value value) {
    	if (!isConstant(value.variable())) super.beforeUnassigned(iteration, value);
    }
    
    /** Called after a value is assigned to its variable. Constant variables are excluded from (re)assignment. */
    public void afterAssigned(long iteration, Value value) {
    	if (!isConstant(value.variable())) super.afterAssigned(iteration, value);
    }
    
    /** Called after a value is unassigned from its variable. Constant variables are excluded from (re)assignment. */
    public void afterUnassigned(long iteration, Value value) {
    	if (!isConstant(value.variable())) super.afterUnassigned(iteration, value);
    }
}
