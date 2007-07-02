package net.sf.cpsolver.coursett.heuristics;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/** 
 * Neighbour selection which does the standard time neighbour selection most of the time,
 * however, the very best neighbour is selected time to time (using backtracking based search).
 * 
 * @see StandardNeighbourSelection
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
 * Copyright (C) 2006 Tomas Muller<br>
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

public class NeighbourSelectionWithSuggestions extends StandardNeighbourSelection {
	private double iSuggestionProbability = 0.1;
	private double iSuggestionProbabilityAllAssigned = 0.5;
	private int iSuggestionTimeout = 500;
	private int iSuggestionDepth = 4;
	
	private Solution iSolution = null;
	private SuggestionNeighbour iSuggestionNeighbour = null;
	private TimetableComparator iCmp = null;
	private double iValue = 0;
	private int iNrAssigned = 0;
	
	public NeighbourSelectionWithSuggestions(DataProperties properties) throws Exception {
		super(properties);
		iSuggestionProbability = properties.getPropertyDouble("Neighbour.SuggestionProbability", iSuggestionProbability);
		iSuggestionProbabilityAllAssigned = properties.getPropertyDouble("Neighbour.SuggestionProbabilityAllAssigned", iSuggestionProbabilityAllAssigned);
		iSuggestionTimeout = properties.getPropertyInt("Neighbour.SuggestionTimeout", iSuggestionTimeout);
		iSuggestionDepth = properties.getPropertyInt("Neighbour.SuggestionDepth", iSuggestionDepth);
	}
	
	public NeighbourSelectionWithSuggestions(Solver solver) throws Exception {
		this(solver.getProperties());
		init(solver);
	}
	
	public void init(Solver solver) {
		super.init(solver);
		iCmp = (TimetableComparator)solver.getSolutionComparator();
	}

	public Neighbour selectNeighbour(Solution solution) {
		Neighbour neighbour = null;
		if (solution.getModel().unassignedVariables().isEmpty()) {
			for (int d=iSuggestionDepth;d>1;d--) {
				if (ToolBox.random()<Math.pow(iSuggestionProbabilityAllAssigned,d-1)) {
					neighbour = selectNeighbourWithSuggestions(solution,(Lecture)selectVariable(solution),d);
					break;
				}
			}
		} else {
			for (int d=iSuggestionDepth;d>1;d--) {
				if (ToolBox.random()<Math.pow(iSuggestionProbability,d-1)) {
					neighbour = selectNeighbourWithSuggestions(solution,(Lecture)selectVariable(solution),d);
					break;
				}
			}
		}
		return (neighbour!=null?neighbour:super.selectNeighbour(solution));
	}
	
	public synchronized Neighbour selectNeighbourWithSuggestions(Solution solution, Lecture lecture, int depth) {
        if (lecture==null) return null;
        
        iSolution = solution;
        iSuggestionNeighbour = null;
        iValue = iCmp.currentValue(solution);
        iNrAssigned = solution.getModel().assignedVariables().size();
        
        synchronized (solution) {
        	Model model = solution.getModel();
            //System.out.println("BEFORE BT ("+lecture.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
            
            Vector initialLectures = new Vector(1); 
            initialLectures.add(lecture);
            backtrack(System.currentTimeMillis(), initialLectures, new Vector(), new Hashtable(), depth);
            
            //System.out.println("AFTER  BT ("+lecture.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
        }
        
        return iSuggestionNeighbour;
	}
	
    private boolean containsCommited(Collection values) {
    	if (((TimetableModel)iSolution.getModel()).hasConstantVariables()) {
        	for (Iterator i=values.iterator();i.hasNext();) {
        		Placement placement = (Placement)i.next();
        		Lecture lecture = (Lecture)placement.variable();
        		if (lecture.isCommitted()) return true;
        	}
    	}
    	return false;
    }    

    private void backtrack(long startTime, Vector initialLectures, Vector resolvedLectures, Hashtable conflictsToResolve, int depth) {
        int nrUnassigned = conflictsToResolve.size();
        if ((initialLectures==null || initialLectures.isEmpty()) && nrUnassigned==0) {
        	if (iSolution.getModel().assignedVariables().size()>iNrAssigned || (iSolution.getModel().assignedVariables().size()==iNrAssigned && iValue>iCmp.currentValue(iSolution))) {
        		if (iSuggestionNeighbour==null || iSuggestionNeighbour.compareTo(iSolution)>=0)
        			iSuggestionNeighbour=new SuggestionNeighbour(resolvedLectures);
        	}
        	return;
        }
        if (depth<=0) return;
        if (iSuggestionTimeout>0 && System.currentTimeMillis()-startTime>iSuggestionTimeout) {
            return;
        }
        for (Enumeration e1=(initialLectures!=null && !initialLectures.isEmpty()?initialLectures.elements():conflictsToResolve.keys());e1.hasMoreElements();) {
            Lecture lecture = (Lecture)e1.nextElement();
            if (resolvedLectures.contains(lecture)) continue;
            resolvedLectures.add(lecture);
            for (Enumeration e2=lecture.values().elements();e2.hasMoreElements();) {
                Placement placement = (Placement)e2.nextElement();
                if (placement.equals(lecture.getAssignment())) continue;
                if (placement.isHard()) continue;
                Set conflicts = iSolution.getModel().conflictValues(placement);
                if (conflicts!=null && (nrUnassigned+conflicts.size()>depth)) continue;
                if (conflicts!=null && conflicts.contains(placement)) continue;
                if (containsCommited(conflicts)) continue;
                boolean containException = false;
                if (conflicts!=null) {
                    for (Iterator i=conflicts.iterator();!containException && i.hasNext();) {
                        Placement c = (Placement)i.next();
                        if (resolvedLectures.contains(((Lecture)c.variable()).getClassId())) containException = true;
                    }
                }
                if (containException) continue;
                Placement cur = (Placement)lecture.getAssignment();
                if (conflicts!=null) {
                    for (Iterator i=conflicts.iterator();!containException && i.hasNext();) {
                        Placement c = (Placement)i.next();
                        c.variable().unassign(0);
                    }
                }
                if (cur!=null) cur.variable().unassign(0);
                Vector un = new Vector(lecture.getModel().unassignedVariables());
                for (Iterator i=conflicts.iterator();!containException && i.hasNext();) {
                    Placement c = (Placement)i.next();
                    conflictsToResolve.put(c.variable(),c);
                }
                Placement resolvedConf = (Placement)conflictsToResolve.remove(lecture);
                backtrack(startTime, null, resolvedLectures, conflictsToResolve, depth-1);
                if (cur==null)
                    lecture.unassign(0);
                else
                    lecture.assign(0, cur);
                if (conflicts!=null) {
                    for (Iterator i=conflicts.iterator();i.hasNext();) {
                        Placement p = (Placement)i.next();
                        p.variable().assign(0, p);
                        conflictsToResolve.remove(p.variable());
                    }
                }
                if (resolvedConf!=null)
                    conflictsToResolve.put(lecture, resolvedConf);
            }
            resolvedLectures.remove(lecture);
        }
    }
	
	
	public class SuggestionNeighbour extends Neighbour {
		private double iValue = 0;
		private Vector iDifferentAssignments = null;
		
		public SuggestionNeighbour(Vector resolvedLectures) {
			iValue = iCmp.currentValue(iSolution);
            iDifferentAssignments = new Vector();
        	for (Enumeration e=resolvedLectures.elements();e.hasMoreElements();) {
        		Lecture lecture = (Lecture)e.nextElement();
        		Placement p = (Placement)lecture.getAssignment();
        		iDifferentAssignments.add(p);
        	}
		}
		
		public void assign(long iteration) {
			//System.out.println("START ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
			//System.out.println("  "+this);
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
				Placement p = (Placement)e.nextElement();
				if (p.variable().getAssignment()!=null)
					p.variable().unassign(iteration);
			}
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
				Placement p = (Placement)e.nextElement();
				p.variable().assign(iteration, p);
			}
			//System.out.println("END ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
		}
		
	    public int compareTo(Solution solution) {
	        return Double.compare(iValue, iCmp.currentValue(solution));
	    }
	    
	    public String toString() {
	    	StringBuffer sb = new StringBuffer("Suggestion{value="+(iValue-iCmp.currentValue(iSolution))+": ");
	    	for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
				Placement p = (Placement)e.nextElement();
				sb.append("\n    "+p.variable().getName()+" "+p.getName()+(e.hasMoreElements()?",":""));
	    	}
	    	sb.append("}");
	    	return sb.toString();
	    }
	}
}
