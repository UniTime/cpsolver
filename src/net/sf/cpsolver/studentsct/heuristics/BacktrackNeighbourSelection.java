package net.sf.cpsolver.studentsct.heuristics;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

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
	private int iSuggestionTimeout = 1000;
	private int iSuggestionDepth = 4;
	
	private Solution iSolution = null;
	private SuggestionNeighbour iSuggestionNeighbour = null;
	private double iValue = 0;
	private int iNrAssigned = 0;
	
	public BacktrackNeighbourSelection(DataProperties properties) throws Exception {
		super(properties);
		iSuggestionTimeout = properties.getPropertyInt("Neighbour.SuggestionTimeout", iSuggestionTimeout);
		iSuggestionDepth = properties.getPropertyInt("Neighbour.SuggestionDepth", iSuggestionDepth);
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

	public Neighbour selectNeighbour(Solution solution, Variable variable) {
        if (variable==null) return null;
        
        iSolution = solution;
        iSuggestionNeighbour = null;
        iValue = solution.getModel().getTotalValue();
        iNrAssigned = solution.getModel().assignedVariables().size();
        
        synchronized (solution) {
            Model model = solution.getModel();
            //System.out.println("BEFORE BT ("+variable.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
            
            Vector initialVariables = new Vector(1); 
            initialVariables.add(variable);
            backtrack(System.currentTimeMillis(), initialVariables, new Vector(), new Hashtable(), iSuggestionDepth);
            
            //System.out.println("AFTER  BT ("+variable.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
        }
        
        return iSuggestionNeighbour;
	}
    private boolean containsConstantValues(Collection values) {
        for (Iterator i=values.iterator();i.hasNext();) {
            Value value = (Value)i.next();
            if (value.variable() instanceof ConstantVariable && ((ConstantVariable)value.variable()).isConstant())
                return true;
    	}
    	return false;
    }    

    private void backtrack(long startTime, Vector initialVariables, Vector resolvedVariables, Hashtable conflictsToResolve, int depth) {
        int nrUnassigned = conflictsToResolve.size();
        if ((initialVariables==null || initialVariables.isEmpty()) && nrUnassigned==0) {
        	if (iSolution.getModel().assignedVariables().size()>iNrAssigned || (iSolution.getModel().assignedVariables().size()==iNrAssigned && iValue>iSolution.getModel().getTotalValue())) {
        		if (iSuggestionNeighbour==null || iSuggestionNeighbour.compareTo(iSolution)>=0)
        			iSuggestionNeighbour=new SuggestionNeighbour(resolvedVariables);
        	}
        	return;
        }
        if (depth<=0) return;
        if (iSuggestionTimeout>0 && System.currentTimeMillis()-startTime>iSuggestionTimeout) {
            return;
        }
        for (Enumeration e1=(initialVariables!=null && !initialVariables.isEmpty()?initialVariables.elements():conflictsToResolve.keys());e1.hasMoreElements();) {
            Variable variable = (Variable)e1.nextElement();
            if (resolvedVariables.contains(variable)) continue;
            resolvedVariables.add(variable);
            for (Enumeration e2=variable.values().elements();e2.hasMoreElements();) {
                Value value = (Value)e2.nextElement();
                if (value.equals(variable.getAssignment())) continue;
                Set conflicts = iSolution.getModel().conflictValues(value);
                if (conflicts!=null && (nrUnassigned+conflicts.size()>depth)) continue;
                if (conflicts!=null && conflicts.contains(value)) continue;
                if (containsConstantValues(conflicts)) continue;
                boolean containException = false;
                if (conflicts!=null) {
                    for (Iterator i=conflicts.iterator();!containException && i.hasNext();) {
                        Value c = (Value)i.next();
                        if (resolvedVariables.contains(c.variable())) containException = true;
                    }
                }
                if (containException) continue;
                Value cur = variable.getAssignment();
                if (conflicts!=null) {
                    for (Iterator i=conflicts.iterator();i.hasNext();) {
                        Value c = (Value)i.next();
                        c.variable().unassign(0);
                    }
                }
                if (cur!=null) cur.variable().unassign(0);
                Vector un = new Vector(variable.getModel().unassignedVariables());
                for (Iterator i=conflicts.iterator();i.hasNext();) {
                    Value c = (Value)i.next();
                    conflictsToResolve.put(c.variable(),c);
                }
                Value resolvedConf = (Value)conflictsToResolve.remove(variable);
                backtrack(startTime, null, resolvedVariables, conflictsToResolve, depth-1);
                if (cur==null)
                    variable.unassign(0);
                else
                    variable.assign(0, cur);
                if (conflicts!=null) {
                    for (Iterator i=conflicts.iterator();i.hasNext();) {
                        Value p = (Value)i.next();
                        p.variable().assign(0, p);
                        conflictsToResolve.remove(p.variable());
                    }
                }
                if (resolvedConf!=null)
                    conflictsToResolve.put(variable, resolvedConf);
            }
            resolvedVariables.remove(variable);
        }
    }
	
	
	public class SuggestionNeighbour extends Neighbour {
		private double iValue = 0;
		private Vector iDifferentAssignments = null;
		
		public SuggestionNeighbour(Vector resolvedVariables) {
			iValue = iSolution.getModel().getTotalValue();
            iDifferentAssignments = new Vector();
        	for (Enumeration e=resolvedVariables.elements();e.hasMoreElements();) {
        		Variable variable = (Variable)e.nextElement();
        		Value value = variable.getAssignment();
        		iDifferentAssignments.add(value);
        	}
		}
		
		public void assign(long iteration) {
			//System.out.println("START ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
			//System.out.println("  "+this);
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
				Value p = (Value)e.nextElement();
				if (p.variable().getAssignment()!=null)
					p.variable().unassign(iteration);
			}
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
                Value p = (Value)e.nextElement();
				p.variable().assign(iteration, p);
			}
			//System.out.println("END ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
		}
		
	    public int compareTo(Solution solution) {
	        return Double.compare(iValue, solution.getModel().getTotalValue());
	    }
	    
	    public String toString() {
	    	StringBuffer sb = new StringBuffer("Suggestion{value="+(iValue-iSolution.getModel().getTotalValue())+": ");
	    	for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
                Value p = (Value)e.nextElement();
				sb.append("\n    "+p.variable().getName()+" "+p.getName()+(e.hasMoreElements()?",":""));
	    	}
	    	sb.append("}");
	    	return sb.toString();
	    }
	}
}
