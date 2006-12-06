package net.sf.cpsolver.ifs.heuristics;

import java.lang.reflect.Constructor;
import java.util.Enumeration;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.solver.SolverListener;
import net.sf.cpsolver.ifs.util.DataProperties;

public class StandardNeighbourSelection implements NeighbourSelection {
	protected static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(StandardNeighbourSelection.class);
	
    private ValueSelection iValueSelection = null;
    private VariableSelection iVariableSelection = null;
    private Solver iSolver = null;

    /** Sets value selection criterion */
    public void setValueSelection(ValueSelection valueSelection) { iValueSelection = valueSelection; }
    /** Sets variable selection criterion */
    public void setVariableSelection(VariableSelection variableSelection) { iVariableSelection = variableSelection; }
    
    /** Returns values selection criterion */
    public ValueSelection getValueSelection() { return iValueSelection; }
    /** Returns variable selection criterion */
    public VariableSelection getVariableSelection() { return iVariableSelection; }
    
    public StandardNeighbourSelection(DataProperties properties) throws Exception {
        String valueSelectionClassName = properties.getProperty("Value.Class","net.sf.cpsolver.ifs.heuristics.GeneralValueSelection");
        sLogger.info("Using "+valueSelectionClassName);
        Class valueSelectionClass = Class.forName(valueSelectionClassName);
        Constructor valueSelectionConstructor = valueSelectionClass.getConstructor(new Class[]{DataProperties.class});
        setValueSelection((ValueSelection)valueSelectionConstructor.newInstance(new Object[] {properties}));
        
        String variableSelectionClassName = properties.getProperty("Variable.Class","net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection");
        sLogger.info("Using "+variableSelectionClassName);
        Class variableSelectionClass = Class.forName(variableSelectionClassName);
        Constructor variableSelectionConstructor = variableSelectionClass.getConstructor(new Class[]{DataProperties.class});
        setVariableSelection((VariableSelection)variableSelectionConstructor.newInstance(new Object[] {properties}));
    }
    

    public void init(Solver solver) {
    	getValueSelection().init(solver);
    	getVariableSelection().init(solver);
    	iSolver = solver;
    }
    
    public Variable selectVariable(Solution solution) {
        // Variable selection
        Variable variable = getVariableSelection().selectVariable(solution);
        for (Enumeration i=iSolver.getSolverListeners().elements();i.hasMoreElements();)
            if (!((SolverListener)i.nextElement()).variableSelected(solution.getIteration(), variable)) return null;
        if (variable == null) sLogger.debug("No variable selected.");
        
        if (variable != null && !variable.hasValues()) {
        	sLogger.debug("Variable "+variable.getName()+" has no values.");
            return null;
        }
    	return variable;
    }
    
    public Value selectValue(Solution solution, Variable variable) {
        // Value selection
        Value value = getValueSelection().selectValue(solution, variable);
        for (Enumeration i=iSolver.getSolverListeners().elements();i.hasMoreElements();)
            if (!((SolverListener)i.nextElement()).valueSelected(solution.getIteration(), variable, value)) return null;
        
        if (value == null) {
        	sLogger.debug("No value selected for variable "+variable+".");
        }
        return value;
    }

    public Neighbour selectNeighbour(Solution solution) {
        Variable variable = selectVariable(solution);
    	return new SimpleNeighbour(variable, selectValue(solution, variable));
    }
}
