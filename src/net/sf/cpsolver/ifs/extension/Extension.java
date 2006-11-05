package net.sf.cpsolver.ifs.extension;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Generic extension of IFS solver.
 * <br><br>
 * All extensions should extend this class.
 * <br><br>
 * An extension may use extra information associated with a variable or a value
 * (see {@link Variable#setExtra(Object)}, {@link Variable#getExtra()}, {@link Value#setExtra(Object)}, {@link Value#getExtra()})
 * but there can be only one extension using these extra objects used during the search.
 * For instance, {@link MacPropagation} is using these extra objects to memorize explanations.
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
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
public class Extension implements ModelListener {
    private Model iModel = null;
    private Solver iSolver = null;
    private DataProperties iProperties = null;
    
    /** Constructor
     * @param solver IFS solver
     * @param properties input configuration
     */
    public Extension(Solver solver, DataProperties properties) {
        iSolver = solver;
        iProperties = properties;
    }
    
    /** Registration of a model. This is called by the solver before start. */
    public void register(Model model) {
        iModel = model;
        iModel.addModelListener(this);
    }
    
    /** Unregistration of a model. This is called by the solver when extension is removed. */
    public void unregister(Model model) {
        iModel.removeModelListener(this);
        iModel = null;
    }
    
    /** Returns true if there is a model registered to this extension, i.e., when extension is registered. */
    public boolean isRegistered() {
        return iModel != null;
    }
    
    /** Returns the model */
    public Model getModel() {
        return iModel;
    }
    
    /** Returns the solver */
    public Solver getSolver() {
        return iSolver;
    }
    
    /** Returns input configuration */
    public DataProperties getProperties() {
        return iProperties;
    }
    
    /** Called after a value is assigned to a variable */
    public void afterAssigned(long iteration, Value value) {
    }
    /** Called after a value is unassigned from a variable */
    public void afterUnassigned(long iteration, Value value) {
    }
    /** Called before a value is assigned to a variable */
    public void beforeAssigned(long iteration, Value value) {
    }
    /** Called after a value is unassigned from a variable */
    public void beforeUnassigned(long iteration, Value value) {
    }
    /** Called when a constraint is added to the model */
    public void constraintAdded(Constraint constraint) {
    }
    /** Called when a constraint is removed from the model */
    public void constraintRemoved(Constraint constraint) {
    }
    /** Called when a variable is added to the model */
    public void variableAdded(Variable variable) {
    }
    /** Called when a variable is removed from the model */
    public void variableRemoved(Variable variable) {
    }
    
    /** Put some information from the extension to the solution info table*/
    public void getInfo(java.util.Hashtable anInfo) {
    }
    
    /** Initialization -- called before the solver is started */
    public boolean init(Solver solver) {
        return true;
    }
    
    /** Should return true when {@link Value#setExtra(Object)}, {@link Value#getExtra()} are used by the extension */
    public boolean useValueExtra() {
        return false;
    }
    
    /** Should return true when {@link Variable#setExtra(Object)}, {@link Variable#getExtra()} are used by the extension */
    public boolean useVariableExtra() {
        return false;
    }
}