package net.sf.cpsolver.ifs.heuristics;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.constant.ConstantVariable;
import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Backtracking-based neighbour selection. A best neighbour that is found by
 * a backtracking-based algorithm within a limited depth from a selected variable
 * is returned.
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Neighbour.BackTrackTimeout</td><td>{@link Integer}</td><td>Timeout for each neighbour selection (in milliseconds).</td></tr>
 * <tr><td>Neighbour.BackTrackDepth</td><td>{@link Integer}</td><td>Limit of search depth.</td></tr>
 * </table>
 *
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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

public class BacktrackNeighbourSelection extends StandardNeighbourSelection {
    private ConflictStatistics iStat = null;
    private static Logger sLog = Logger.getLogger(BacktrackNeighbourSelection.class);
	private int iTimeout = 5000;
	private int iDepth = 4;
	
	protected Solution iSolution = null;
	protected BackTrackNeighbour iBackTrackNeighbour = null;
	protected double iValue = 0;
	private int iNrAssigned = 0;
    private long iT0,iT1;
    private boolean iTimeoutReached = false; 
    private int iMaxIters = -1, iNrIters = 0;
    private boolean iMaxItersReached = false;
	
    /**
     * Constructor
     * @param properties configuration
     * @throws Exception
     */
	public BacktrackNeighbourSelection(DataProperties properties) throws Exception {
		super(properties);
		iTimeout = properties.getPropertyInt("Neighbour.BackTrackTimeout", iTimeout);
		iDepth = properties.getPropertyInt("Neighbour.BackTrackDepth", iDepth);
        iMaxIters = properties.getPropertyInt("Neighbour.BackTrackMaxIters", iMaxIters);
	}
	
    /** Solver initialization */
	public void init(Solver solver) {
		super.init(solver);
        for (Enumeration i = solver.getExtensions().elements(); i.hasMoreElements();) {
            Extension extension = (Extension)i.nextElement();
            if (extension instanceof ConflictStatistics)
                iStat = (ConflictStatistics)extension;
        }
	}
    
    /** 
     * Select neighbour. The standard variable selection 
     * (see {@link StandardNeighbourSelection#getVariableSelection()}) is used to select
     * a variable. A backtracking of a limited depth is than employed from this variable.
     * The best assignment found is returned (see {@link BackTrackNeighbour}).
     **/
    public Neighbour selectNeighbour(Solution solution) {
        return selectNeighbour(solution, getVariableSelection().selectVariable(solution));
    }

    /** 
     * Select neighbour -- starts from the provided variable. A backtracking of a limited 
     * depth is employed from the given variable.
     * The best assignment found is returned (see {@link BackTrackNeighbour}).
     **/
	public synchronized Neighbour selectNeighbour(Solution solution, Variable variable) {
        if (variable==null) return null;
        
        iSolution = solution;
        iBackTrackNeighbour = null;
        iValue = solution.getModel().getTotalValue();
        iNrAssigned = solution.getModel().assignedVariables().size();
        iT0 = System.currentTimeMillis();
        iNrIters = 0;
        iTimeoutReached = false;
        iMaxItersReached = false;
        
        synchronized (solution) {
            if (sLog.isDebugEnabled()) sLog.debug("-- before BT ("+variable.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
            
            Vector variables2resolve = new Vector(1); 
            variables2resolve.add(variable);
            backtrack(variables2resolve, 0, iDepth);
            
            if (sLog.isDebugEnabled()) sLog.debug("-- after  BT ("+variable.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
        }
        
        iT1 = System.currentTimeMillis();
        
        if (sLog.isDebugEnabled()) sLog.debug("-- selected neighbour: "+iBackTrackNeighbour);
        return iBackTrackNeighbour;
	}
    
    /** Time needed to find a neighbour (last call of selectNeighbour method) */
    public long getTime() { return iT1 - iT0; }
    
    /** True, if timeout was reached during the last call of selectNeighbour method */
    public boolean isTimeoutReached() { return iTimeoutReached; }
    
    /** True, if the maximum number of iterations was reached by the last call of selectNeighbour method */
    public boolean isMaxItersReached() { return iMaxItersReached; }
    
    private boolean containsConstantValues(Collection values) {
        for (Iterator i=values.iterator();i.hasNext();) {
            Value value = (Value)i.next();
            if (value.variable() instanceof ConstantVariable && ((ConstantVariable)value.variable()).isConstant())
                return true;
    	}
    	return false;
    }    

    /** List of values of the given variable that will be considered */
    protected Enumeration values(Variable variable) {
        return variable.values().elements();
    }
    
    /** Check bound */
    protected boolean checkBound(Vector variables2resolve, int idx, int depth, Value value, Set conflicts) {
        int nrUnassigned = variables2resolve.size()-idx;
        if ((nrUnassigned+conflicts.size()>depth)) {
            if (sLog.isDebugEnabled()) sLog.debug("        -- too deap");
            return false;
        }
        if (containsConstantValues(conflicts)) {
            if (sLog.isDebugEnabled()) sLog.debug("        -- contains constants values");
            return false;
        }
        boolean containAssigned = false;
        for (Iterator i=conflicts.iterator();!containAssigned && i.hasNext();) {
            Value conflict = (Value)i.next();
            int confIdx = variables2resolve.indexOf(conflict.variable());
            if (confIdx>=0 && confIdx<=idx) {
                if (sLog.isDebugEnabled()) sLog.debug("        -- contains resolved variable "+conflict.variable());
                containAssigned = true;
            }
        }
        if (containAssigned) return false;
        return true;
    }
    
    /** Check whether backtrack can continue */
    protected boolean canContinue(Vector variables2resolve, int idx, int depth) {
        if (depth<=0) {
            if (sLog.isDebugEnabled()) sLog.debug("    -- depth reached");
            return false;
        }
        if (iTimeoutReached) {
            if (sLog.isDebugEnabled()) sLog.debug("    -- timeout reached");
            return false;
        }
        if (iMaxItersReached) {
            if (sLog.isDebugEnabled()) sLog.debug("    -- max number of iterations reached");
            return false;
        }
        return true;
    }
    
    protected boolean canContinueEvaluation() {
        return !iTimeoutReached && !iMaxItersReached;
    }
    
    /** Backtracking */
    protected void backtrack(Vector variables2resolve, int idx, int depth) {
        if (sLog.isDebugEnabled()) sLog.debug("  -- bt["+depth+"]: "+idx+" of "+variables2resolve.size()+" "+variables2resolve);
        if (!iTimeoutReached && iTimeout>0 && System.currentTimeMillis()-iT0>iTimeout)
            iTimeoutReached = true;
        if (!iMaxItersReached && iMaxIters>0 && iNrIters++>iMaxIters)
            iMaxItersReached = true;
        int nrUnassigned = variables2resolve.size()-idx;
        if (nrUnassigned==0) {
            if (sLog.isDebugEnabled()) sLog.debug("    -- all assigned");
        	if (iSolution.getModel().assignedVariables().size()>iNrAssigned || (iSolution.getModel().assignedVariables().size()==iNrAssigned && iValue>iSolution.getModel().getTotalValue())) {
                if (sLog.isDebugEnabled()) sLog.debug("    -- better than current");
        		if (iBackTrackNeighbour==null || iBackTrackNeighbour.compareTo(iSolution)>=0) {
                    if (sLog.isDebugEnabled()) sLog.debug("      -- better than best");
        			iBackTrackNeighbour=new BackTrackNeighbour(variables2resolve);
                }
        	}
        	return;
        }
        if (!canContinue(variables2resolve, idx, depth)) return;
        Variable variable = (Variable)variables2resolve.elementAt(idx);
        if (sLog.isDebugEnabled()) sLog.debug("    -- variable "+variable);
        for (Enumeration e=values(variable);canContinueEvaluation() && e.hasMoreElements();) {
            Value value = (Value)e.nextElement();
            if (value.equals(variable.getAssignment())) continue;
            if (sLog.isDebugEnabled()) sLog.debug("      -- value "+value);
            Set conflicts = iSolution.getModel().conflictValues(value);
            if (sLog.isDebugEnabled()) sLog.debug("      -- conflicts "+conflicts);
            if (!checkBound(variables2resolve,idx,depth,value,conflicts)) continue;
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
	
	
    /** Backtracking neighbour */
	public class BackTrackNeighbour extends Neighbour {
		private double iTotalValue = 0;
		private double iValue = 0;
		private Vector iDifferentAssignments = null;
		
        /**
         * Constructor
         * @param resolvedVariables variables that has been changed
         */
		public BackTrackNeighbour(Vector resolvedVariables) {
			iTotalValue = iSolution.getModel().getTotalValue();
			iValue = 0;
            iDifferentAssignments = new Vector();
        	for (Enumeration e=resolvedVariables.elements();e.hasMoreElements();) {
        		Variable variable = (Variable)e.nextElement();
        		Value value = variable.getAssignment();
        		iDifferentAssignments.add(value);
        		iValue += value.toDouble();
        	}
		}
		
		/** Neighbour value (solution total value if the neighbour is applied). */
		public double getTotalValue() {
		    return iTotalValue;
		}
		
		/** Sum of values of variables from the neighbour that change their values */ 
        public double value() {
            return iValue;
        }

        /** Neighbour assignments */
		public Vector getAssignments() {
		    return iDifferentAssignments;
		}
		
        /**
         * Assign the neighbour
         */
		public void assign(long iteration) {
			if (sLog.isDebugEnabled()) sLog.debug("-- before assignment: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
			if (sLog.isDebugEnabled()) sLog.debug("  "+this);
			int idx=0;
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();idx++) {
				Value p = (Value)e.nextElement();
				if (p.variable().getAssignment()!=null) {
                    if (idx>0 && iStat!=null)
                        iStat.variableUnassigned(iteration, p.variable().getAssignment(), (Value)iDifferentAssignments.firstElement());
					p.variable().unassign(iteration);
				}
			}
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
                Value p = (Value)e.nextElement();
				p.variable().assign(iteration, p);
			}
			if (sLog.isDebugEnabled()) sLog.debug("-- after assignment: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iSolution.getModel().getTotalValue());
		}
		
        /**
         * Compare two neighbours
         */
	    public int compareTo(Solution solution) {
	        return Double.compare(iTotalValue, solution.getModel().getTotalValue());
	    }
	    
        public String toString() {
	    	StringBuffer sb = new StringBuffer("BT{value="+(iTotalValue-iSolution.getModel().getTotalValue())+": ");
	    	for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
                Value p = (Value)e.nextElement();
				sb.append("\n    "+p.variable().getName()+" "+p.getName()+(e.hasMoreElements()?",":""));
	    	}
	    	sb.append("}");
	    	return sb.toString();
	    }
	}
	
	/** Return maximal depth */
	public int getDepth() {
	    return iDepth;
	}
	/** Set maximal depth */
	public void setDepth(int depth) {
	    iDepth = depth;
	}
	/** Return time limit */
	public int getTimeout() {
	    return iTimeout;
	}
	/** Set time limit */
	public void setTimeout(int timeout) {
	    iTimeout = timeout;
	}
    /** Return maximal number of iterations */
    public int getMaxIters() {
        return iMaxIters;
    }
    /** Set maximal number of iterations */
    public void setMaxIters(int maxIters) {
        iMaxIters = maxIters;
    }
}
