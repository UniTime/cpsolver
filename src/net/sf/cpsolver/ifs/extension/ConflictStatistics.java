package net.sf.cpsolver.ifs.extension;


import java.util.*;

import net.sf.cpsolver.ifs.heuristics.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Conflict-based statistics.
 * <br><br>
 * The idea behind it is to memorize conflicts and to avoid their potential repetition. When a value v0 is assigned to a 
 * variable V0, hard conflicts with previously assigned variables (e.g., V1 = v1, V2 = v2, ... Vm = vm) may occur. 
 * These variables V1,...,Vm have to be unassigned before the value v0 is assigned to the variable V0. These unassignments, 
 * together with the reason for their unassignment (i.e., the assignment V0 = v0), and a counter tracking how many times 
 * such an event occurred in the past, is stored in memory.
 * <br><br>
 * Later, if a variable is selected for assignment again, the stored information about repetition of past hard conflicts
 * can be taken into account, e.g., in the value selection heuristics. Assume that the variable V0 is selected for an
 * assignment again (e.g., because it became unassigned as a result of a later assignment), we can weight the number of
 * hard conflicts created in the past for each possible value of this variable. In the above example, the existing
 * assignment V1 = v1 can prohibit the selection of value v0 for variable V0 if there is again a conflict with the
 * assignment V1 = v1.
 * <br><br>
 * Conflict-based statistics are a data structure which memorizes the number of hard conflicts that have occurred 
 * during the search (e.g., that assignment V0 = v0 resulted c1 times in an unassignment of V1 = v1, c2 times of 
 * V2 = v2, . . . and cm times of Vm = vm). More precisely, they form an array
 * <ul>
 * CBS[Va = va, Vb != vb] = cab,
 * </ul>
 * stating that the assignment Va = va caused the unassignment of Vb = vb a total of cab times in the past. Note that 
 * in case of n-ary constraints (where n > 2), this does not imply that the assignments Va = va and Vb = vb cannot be used
 * together. The proposed conflict-based statistics do not actually work with any constraint, they only memorize 
 * unassignments and the assignment that caused them. Let us consider a variable Va selected by the 
 * {@link VariableSelection#selectVariable(Solution)} function and a value va selected by 
 * {@link ValueSelection#selectValue(Solution, Variable)}. Once the assignment Vb = vb is selected by 
 * {@link Model#conflictValues(Value)} to be unassigned, the array cell CBS[Va = va, Vb != vb] is incremented by one.
 * <br><br>
 * The data structure is implemented as a hash table, storing information for conflict-based statistics. A counter is 
 * maintained for the tuple A = a and B != b. This counter is increased when the value a is assigned to the variable A 
 * and b is unassigned from B. The example of this structure
 * <ul>
 * A = a &nbsp;&nbsp;&nbsp; &#8594; &nbsp;&nbsp;&nbsp; 3 x B != b, &nbsp; 4 x B != c, &nbsp; 2 x C != a, &nbsp; 120 x D != a
 * </ul>
 * expresses that variable B lost its assignment b three times and its assignment c four times, variable C lost its 
 * assignment a two times, and D lost its assignment a 120 times, all because of later assignments of value a to 
 * variable A. This structure is being used in the value selection heuristics to evaluate existing conflicts with
 * the assigned variables. For example, if there is a variable A selected and if the value a is in conflict with the 
 * assignment B = b, we know that a similar problem has already occurred 3x in the past, and hence the conflict A = a is 
 * weighted with the number 3.
 * <br><br>
 * Then, a min-conflict value selection criterion, which selects a value with the minimal number of conflicts with the 
 * existing assignments, can be easily adapted to a weighted min-conflict criterion. The value with the smallest sum of the
 * number of conflicts multiplied by their frequencies is selected. Stated in another way, the weighted min-conflict 
 * approach helps the value selection heuristics to select a value that might cause more conflicts than another
 * value, but these conflicts occurred less frequently, and therefore they have a lower weighted sum.
 * <br><br>
 * The conflict-based statistics has also implemented the following extensions: <ul>
 * <li> If a variable is selected for an assignment, the above presented structure can also tell how many potential
 * conflicts a value can cause in the future. In the above example, we already know that four times a later assignment
 * of A=a caused that value c was unassigned from B. We can try to minimize such future conflicts by selecting
 * a different value of the variable B while A is still unbound.
 * <li> The memorized conflicts can be aged according to how far they have occurred in the past. For example, a conflict
 * which occurred 1000 iterations ago can have half the weight of a conflict which occurred during the last iteration
 * or it can be forgotten at all.
 * </ul>
 * Furthermore, the presented conflict-based statistics can be used not only inside the solving mechanism. The 
 * constructed "implications" together with the information about frequency of their occurrences can be easily accessed 
 * by users or by some add-on deductive engine to identify inconsistencies1 and/or hard parts of the input problem. 
 * The user can then modify the input requirements in order to eliminate problems found and let the solver continue the 
 * search with this modified input problem.
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>ConflictStatistics.Ageing</td><td>{@link Double}</td><td>Ageing of the conflict-based statistics. Every memorized conflict is aged (multiplited) by this factor for every iteration which passed from the time it was memorized. For instance, if there was a conflict 10 iterations ago, its value is ageing^10 (default is 1.0 -- no ageing).</td></tr>
 * <tr><td>ConflictStatistics.AgeingHalfTime</td><td>{@link Integer}</td><td>Another way how to express ageing: number of iterations to decrease a conflict to 1/2 (default is 0 -- no ageing)</td></tr>
 * </table>
 *
 * @see Solver
 * @see Model
 * @see ValueSelection
 * @see VariableSelection
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
public class ConflictStatistics extends Extension implements ConstraintListener, InfoProvider {
    private static final String PARAM_AGEING = "ConflictStatistics.Ageing";
    private static final String PARAM_HALF_AGE = "ConflictStatistics.AgeingHalfTime";
    private static final String PARAM_PRINT = "ConflictStatistics.Print";
    
    private double iAgeing = 1.0;
    private boolean iPrint = false;
    
    private Hashtable iAssignments = new Hashtable();
    private Hashtable iUnassignedVariables = new Hashtable();
    private Hashtable iNoGoods = new Hashtable();
    
    public ConflictStatistics(Solver solver, DataProperties properties) {
        super(solver, properties);
        iAgeing = properties.getPropertyDouble(PARAM_AGEING, iAgeing);
        int halfAge = properties.getPropertyInt(PARAM_HALF_AGE, 0);
        if (halfAge > 0) iAgeing = Math.exp(Math.log(0.5) / ((double)halfAge));
        iPrint = properties.getPropertyBoolean(PARAM_PRINT, iPrint);
    }
    
    public void register(Model model) {
        super.register(model);
    }
    
    public void unregister(Model model) {
        super.unregister(model);
    }
    
    private void variableUnassigned( long iteration, Value unassignedValue, Assignment noGood) {
    	if (iteration<=0) return;
        Assignment unass = new Assignment(iteration, unassignedValue, iAgeing);
        Vector noGoodsForUnassignment = (Vector)iNoGoods.get(unass);
        if (noGoodsForUnassignment != null) {
            if (noGoodsForUnassignment.contains(noGood)) {
                ((Assignment)noGoodsForUnassignment.elementAt(noGoodsForUnassignment.indexOf(noGood))).incCounter(iteration);
            } else {
                noGoodsForUnassignment.addElement(noGood);
            }
        } else {
            noGoodsForUnassignment = new FastVector();
            noGoodsForUnassignment.addElement(noGood);
            iNoGoods.put(unass, noGoodsForUnassignment);
        }
    }
    
    public void reset() {
        iUnassignedVariables.clear();
        iAssignments.clear();
    }
    
    public Hashtable getNoGoods() {
    	return iNoGoods;
    }
    
    private void variableUnassigned(long iteration, Value unassignedValue, Value assignedValue) {
    	if (iteration<=0) return;
        Assignment ass = new Assignment(iteration, assignedValue, iAgeing);
        Assignment unass = new Assignment(iteration, unassignedValue, iAgeing);
        if (iAssignments.containsKey(unass)) {
            Vector asss = (Vector)iAssignments.get(unass);
            if (asss.contains(ass)) {
                ((Assignment)asss.elementAt(asss.indexOf(ass))).incCounter(iteration);
            } else {
                asss.addElement(ass);
            }
        } else {
            Vector asss = new FastVector();
            asss.addElement(ass);
            iAssignments.put(unass, asss);
        }
        if (iUnassignedVariables.containsKey(unassignedValue.variable())) {
            Vector asss = (Vector)iUnassignedVariables.get(unassignedValue.variable());
            if (asss.contains(ass)) {
                ((Assignment)asss.elementAt(asss.indexOf(ass))).incCounter( iteration);
            } else {
                asss.addElement(ass);
            }
        }
        else {
            Vector asss = new FastVector();
            asss.addElement(ass);
            iUnassignedVariables.put(unassignedValue.variable(), asss);
        }
    }
    
    /** Counts number of unassignments of the given conflicting values caused by the assignment
     * of the given value.
     */
    public double countRemovals(long iteration, Collection conflictValues, Value value) {
        long ret = 0;
        for (Iterator i = conflictValues.iterator(); i.hasNext();) {
            Value conflictValue = (Value)i.next();
            ret += countRemovals(iteration, conflictValue, value);
            // tady bylo +1
        }
        return ret;
    }
    
    /** Counts number of unassignments of the given conflicting value caused by the assignment
     * of the given value.
     */
    public double countRemovals(long iteration, Value conflictValue, Value value) {
        Vector asss = (Vector)iUnassignedVariables.get(conflictValue.variable());
        if (asss == null)
            return 0;
        Assignment ass = new Assignment(iteration, value, iAgeing);
        int idx = asss.indexOf(ass);
        if (idx < 0)
            return 0;
        return ((Assignment)asss.elementAt(idx)).getCounter(iteration);
    }
    
    /** Counts potential number of unassignments of if the given value is selected.
     */
    public long countPotentialConflicts(long iteration, Value value, int limit) {
        Vector asss = (Vector)iAssignments.get(new Assignment(iteration, value, iAgeing));
        if (asss == null) return 0;
        long count = 0;
        for (Enumeration i = asss.elements(); i.hasMoreElements();) {
            Assignment ass = (Assignment)i.nextElement();
            if (ass.getValue().variable().getAssignment() == null) {
                if (limit >= 0) {
                    count += ass.getCounter(iteration) * Math.max(0,1+limit - value.variable().getModel().conflictValues(ass.getValue()).size());
                }
                else {
                    count += ass.getCounter(iteration);
                }
            }
        }
        return count;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("Statistics{");
        for (Enumeration e1 = ToolBox.sortEnumeration(iUnassignedVariables.keys(), Assignment.getComparator(0));e1.hasMoreElements();) {
            Variable variable = (Variable)e1.nextElement();
            if (variable.countAssignments() < 100) continue;
            sb.append("\n      ").append(variable.countAssignments() + "x ").append(variable.getName()).append(" <= {");
            for (Enumeration e2 = ToolBox.sortEnumeration(((Vector)iUnassignedVariables.get(variable)).elements(),Assignment.getComparator(0));e2.hasMoreElements();) {
                Assignment x = (Assignment)e2.nextElement();
                if (x.getCounter(0) >= 10)
                    sb.append("\n        ").append(x.toString(0, true)).append(e2.hasMoreElements() ? "," : "");
            }
            sb.append("\n      }");
        }
        sb.append("\n    }");
        return sb.toString();
    }
    
    public void getInfo(Dictionary info) {
        //info.put("Statistics: IsGood.Time",sTimeFormat.format(((double)iIsGoodTime)/60000.0)+" min");
        //info.put("Statistics: NoGood.Time",sTimeFormat.format(((double)iNoGoodTime)/60000.0)+" min");
        /*info.put("Statistics: VariableAssigned.Time",sTimeFormat.format(((double)iVariableAssignedTime)/60000.0)+" min");
         *info.put("Statistics: VariableUnassigned.Time",sTimeFormat.format(((double)iVariableUnassignedTime)/60000.0)+" min");
         *info.put("Statistics: Bad assignments:", String.valueOf(iBadAssignments.size()));*/
    }
    
    public void getInfo(Dictionary info, Vector variables) {
    }

    public void constraintBeforeAssigned( long iteration, Constraint constraint, Value assigned, Set unassigned) {
    }
    
    /** Increments appropriate counters when there is a value unassigned */
    public void constraintAfterAssigned(long iteration, Constraint constraint, Value assigned, Set unassigned) {
    	if (iteration<=0) return;
        if (unassigned == null || unassigned.isEmpty())
            return;
        if (iPrint) {
            //AssignmentSet noGoods = AssignmentSet.createAssignmentSet(iteration,unassigned, iAgeing);
            //noGoods.addAssignment(iteration, assigned, iAgeing);
            //noGoods.setConstraint(constraint);
        	Assignment noGood = new Assignment(iteration,assigned, iAgeing);
        	noGood.setConstraint(constraint);
            for (Iterator i = unassigned.iterator(); i.hasNext();) {
                Value unassignedValue = (Value)i.next();
                variableUnassigned(iteration, unassignedValue, noGood);
                variableUnassigned(iteration, unassignedValue, assigned);
            }
        } else {
            for (Iterator i = unassigned.iterator(); i.hasNext();) {
                Value unassignedValue = (Value)i.next();
                variableUnassigned(iteration, unassignedValue, assigned);
            }
        }
    }
    
    public void constraintAdded(Constraint constraint) {
        constraint.addConstraintListener(this);
    }
    public void constraintRemoved(Constraint constraint) {
        constraint.removeConstraintListener(this);
    }
}
