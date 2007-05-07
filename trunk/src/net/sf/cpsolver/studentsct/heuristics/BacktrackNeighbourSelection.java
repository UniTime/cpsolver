package net.sf.cpsolver.studentsct.heuristics;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.constant.ConstantVariable;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

public class BacktrackNeighbourSelection extends StandardNeighbourSelection {
    private static Logger sLog = Logger.getLogger(BacktrackNeighbourSelection.class);
	private int iTimeout = 1000;
	private int iDepth = 4;
    public static boolean sDebug = false;
	
	private Solution iSolution = null;
	private BackTrackNeighbour iBackTrackNeighbour = null;
	private double iValue = 0;
	private int iNrAssigned = 0;
    private long iT0,iT1;
    private boolean iTimeoutReached = false; 
	
	public BacktrackNeighbourSelection(DataProperties properties) throws Exception {
		super(properties);
		iTimeout = properties.getPropertyInt("Neighbour.BackTrackTimeout", iTimeout);
		iDepth = properties.getPropertyInt("Neighbour.BackTrackDepth", iDepth);
	}
	
	public BacktrackNeighbourSelection(Solver solver) throws Exception {
		this(solver.getProperties());
		init(solver);
	}
	
	public void init(Solver solver) {
		super.init(solver);
	}
    
    public Neighbour selectNeighbour(Solution solution) {
        return selectNeighbour(solution, getVariableSelection().selectVariable(solution));
    }

	public synchronized Neighbour selectNeighbour(Solution solution, Variable variable) {
        if (variable==null) return null;
        
        iSolution = solution;
        iBackTrackNeighbour = null;
        iValue = solution.getModel().getTotalValue();
        iNrAssigned = solution.getModel().assignedVariables().size();
        iT0 = System.currentTimeMillis();
        iTimeoutReached = false;
        
        synchronized (solution) {
            Model model = solution.getModel();
            if (sDebug) sLog.debug("-- before BT ("+variable.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
            
            Vector variables2resolve = new Vector(1); 
            variables2resolve.add(variable);
            backtrack(variables2resolve, 0, iDepth);
            
            if (sDebug) sLog.debug("-- after  BT ("+variable.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
        }
        
        iT1 = System.currentTimeMillis();
        
        if (sDebug) sLog.debug("-- selected neighbour: "+iBackTrackNeighbour);
        return iBackTrackNeighbour;
	}
    
    public long getTime() { return iT1 - iT0; }
    
    public boolean isTimeoutReched() { return iTimeoutReached; }
    
    private boolean containsConstantValues(Collection values) {
        for (Iterator i=values.iterator();i.hasNext();) {
            Value value = (Value)i.next();
            if (value.variable() instanceof ConstantVariable && ((ConstantVariable)value.variable()).isConstant())
                return true;
    	}
    	return false;
    }    
    
    protected Vector values(Variable variable) {
        return variable.values();
    }

    private void backtrack(Vector variables2resolve, int idx, int depth) {
        if (sDebug) sLog.debug("  -- bt["+depth+"]: "+idx+" of "+variables2resolve.size()+" "+variables2resolve);
        if (!iTimeoutReached && iTimeout>0 && System.currentTimeMillis()-iT0>iTimeout)
            iTimeoutReached = true;
        int nrUnassigned = variables2resolve.size()-idx;
        if (nrUnassigned==0) {
            if (sDebug) sLog.debug("    -- all assigned");
        	if (iSolution.getModel().assignedVariables().size()>iNrAssigned || (iSolution.getModel().assignedVariables().size()==iNrAssigned && iValue>iSolution.getModel().getTotalValue())) {
                if (sDebug) sLog.debug("    -- better than current");
        		if (iBackTrackNeighbour==null || iBackTrackNeighbour.compareTo(iSolution)>=0) {
                    if (sDebug) sLog.debug("      -- better than best");
        			iBackTrackNeighbour=new BackTrackNeighbour(variables2resolve);
                }
        	}
        	return;
        }
        if (depth<=0) {
            if (sDebug) sLog.debug("    -- depth reached");
            return;
        }
        if (iTimeoutReached) {
            if (sDebug) sLog.debug("    -- timeout reached");
            return;
        }
        Variable variable = (Variable)variables2resolve.elementAt(idx);
        if (sDebug) sLog.debug("    -- variable "+variable);
        for (Enumeration e=values(variable).elements();!iTimeoutReached && e.hasMoreElements();) {
            Value value = (Value)e.nextElement();
            if (value.equals(variable.getAssignment())) continue;
            if (sDebug) sLog.debug("      -- value "+value);
            Set conflicts = iSolution.getModel().conflictValues(value);
            if (sDebug) sLog.debug("      -- conflicts "+conflicts);
            if ((nrUnassigned+conflicts.size()>depth)) {
                if (sDebug) sLog.debug("        -- too deap");
                continue;
            }
            if (containsConstantValues(conflicts)) {
                if (sDebug) sLog.debug("        -- contains constants values");
                continue;
            }
            boolean containAssigned = false;
            for (Iterator i=conflicts.iterator();!containAssigned && i.hasNext();) {
                Value conflict = (Value)i.next();
                int confIdx = variables2resolve.indexOf(conflict.variable());
                if (confIdx>=0 && confIdx<=idx) {
                    if (sDebug) sLog.debug("        -- contains resolved variable "+conflict.variable());
                    containAssigned = true;
                }
            }
            if (containAssigned) continue;
            Value current = variable.getAssignment();
            Vector newVariables2resolve = new Vector(variables2resolve);
            for (Iterator i=conflicts.iterator();i.hasNext();) {
                Value conflict = (Value)i.next();
                conflict.variable().unassign(0);
                if (!newVariables2resolve.contains(conflict.variable()))
                    newVariables2resolve.addElement(conflict.variable());
            }
            if (current!=null) current.variable().unassign(0);
            value.variable().assign(0, value);
            backtrack(newVariables2resolve, idx+1, depth-1);
            if (current==null)
                variable.unassign(0);
            else
                variable.assign(0, current);
            for (Iterator i=conflicts.iterator();i.hasNext();) {
                Value conflict = (Value)i.next();
                conflict.variable().assign(0, conflict);
            }
        }
    }
	
	
	public class BackTrackNeighbour extends Neighbour {
		private double iValue = 0;
		private Vector iDifferentAssignments = null;
		
		public BackTrackNeighbour(Vector resolvedVariables) {
			iValue = iSolution.getModel().getTotalValue();
            iDifferentAssignments = new Vector();
        	for (Enumeration e=resolvedVariables.elements();e.hasMoreElements();) {
        		Variable variable = (Variable)e.nextElement();
        		Value value = variable.getAssignment();
        		iDifferentAssignments.add(value);
        	}
		}
		
		public void assign(long iteration) {
			if (sDebug) sLog.debug("-- before assignment: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
			if (sDebug) sLog.debug("  "+this);
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
				Value p = (Value)e.nextElement();
				if (p.variable().getAssignment()!=null)
					p.variable().unassign(iteration);
			}
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
                Value p = (Value)e.nextElement();
				p.variable().assign(iteration, p);
			}
			if (sDebug) sLog.debug("-- after assignment: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
		}
		
	    public int compareTo(Solution solution) {
	        return Double.compare(iValue, solution.getModel().getTotalValue());
	    }
	    
	    public String toString() {
	    	StringBuffer sb = new StringBuffer("BT{value="+(iValue-iSolution.getModel().getTotalValue())+": ");
	    	for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
                Value p = (Value)e.nextElement();
				sb.append("\n    "+p.variable().getName()+" "+p.getName()+(e.hasMoreElements()?",":""));
	    	}
	    	sb.append("}");
	    	return sb.toString();
	    }
	}
}
