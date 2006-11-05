package net.sf.cpsolver.ifs.perturbations;

import java.util.*;

import net.sf.cpsolver.ifs.extension.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Default computation of perturbation penalty (minimal perturbation problem).
 * <br><br>
 * A distance function can be defined with the help of perturbations. A perturbation is a variable that has a different
 * value in the solutions of the initial and the new problem. Some perturbations must be present in each new solution.
 * So called input perturbation means that a variable must have different values in the initial and changed problem
 * because of some input changes (e.g., a course must be scheduled at a different time in the changed problem).
 * The distance function can be defined as the number of additional perturbations. They are given by subtraction of
 * the final number of perturbations and the number of input perturbations (variables without initial assignments).
 * <br><br>
 * This implementation is easily extendable. It disassemble all the available cases into a comparison of the initial and
 * the assigned value different each other. So, the only method which is needed to be changed is
 * {@link DefaultPerturbationsCounter#getPenalty(Value, Value)}. Its current implementation is: <ul><code>
 * protected double getPenalty(Value assignedValue, Value initialValue) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;return 1.0;<br>
 * }<br>
 * </code></ul>
 * It is called only when assignedValue is different to initialValue.
 *
 * @see Solver
 * @see Solution
 * @see Variable
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

public class DefaultPerturbationsCounter implements PerturbationsCounter {
    private ViolatedInitials iViolatedInitials = null;
    protected static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",new java.text.DecimalFormatSymbols(Locale.US));
    
    /** Constructor
     * @param properties input configuration
     */
    public DefaultPerturbationsCounter(DataProperties properties) {
    }
    
    /** Initialization */
    public void init(Solver solver) {
        for (Enumeration i=solver.getExtensions().elements();i.hasMoreElements();) {
            Extension extension = (Extension)i.nextElement();
            if (extension instanceof ViolatedInitials)
                iViolatedInitials = (ViolatedInitials)extension;
        }
    }
    
    public double getPerturbationPenalty(Solution solution) {
        double penalty = 0.0;
        for (Enumeration e=solution.getModel().perturbVariables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getAssignment()!=null && variable.getInitialAssignment()!=null && !variable.getAssignment().equals(variable.getInitialAssignment()))
                penalty += getPenaltyD(variable.getAssignment(),variable.getInitialAssignment());
        }
        return penalty;
    }
    
    protected ViolatedInitials getViolatedInitials() { return iViolatedInitials; }
    
    /** Computes perturbation penalty between assigned and initial value of the same lecture. 
     * It is called only when assignedValue is different to initialValue.
     * @param assignedValue value assigned to a varuable (null when variable is unassigned)
     * @param initialValue initial value of the same varaible (always not null)
     */
    protected double getPenalty(Value assignedValue, Value initialValue) {
        return 1.0;
    }
    
    /** Case A: initial value of a different unassigned variable cannot be assigned (computed by {@link ViolatedInitials})
     * @param selectedValue value which is going to be assigned to its variable
     * @param initialValue value of a different variable, which is currently assigned but which need to be unassifned
     * Different variable, which is unassigned and whose initial value is in conflict with the selected value.*/
    protected double getPenaltyA(Value selectedValue, Value initialValue) {
        return getPenalty(null, initialValue);
    }
    
    /** Case B: initial value is unassigned from a conflicting variable.
     * @param selectedValue value which is going to be unassigned to its variable
     * @param assignedValue value currently assigned to a conflicting variable (different from the one of selectedVariable)
     * @param initialValue initial value of the conflicting variable of assignedValue
     */
    protected double getPenaltyB(Value selectedValue, Value assignedValue, Value initialValue) {
        return getPenalty(assignedValue, initialValue);
    }
    
    /** Case C: non-initial value is unassigned from a conflicting variable.
     * @param selectedValue value which is going to be unassigned to its variable
     * @param assignedValue value currently assigned to a conflicting variable (different from the one of selectedVariable)
     * @param initialValue initial value of the conflicting variable of assignedValue
     */
    protected double getPenaltyC(Value selectedValue, Value assignedValue, Value initialValue) {
        return -getPenalty(assignedValue, initialValue);
    }
    
    /** Case D: different than initial value is assigned to the varaible
     * @param selectedValue value which is going to be unassigned to its variable
     * @param initialValue initial value of the same variable
     */
    protected double getPenaltyD(Value selectedValue, Value initialValue) {
        return getPenalty(selectedValue, initialValue);
    }
    
    public double getPerturbationPenalty(Solution solution, Value selectedValue, Collection conflicts)  {
        double penalty = 0;
        Set violations = (getViolatedInitials()==null?null:getViolatedInitials().getViolatedInitials(selectedValue));
        if (violations!=null)
            for (Iterator it1=violations.iterator(); it1.hasNext(); ) {
            Value aValue = (Value)it1.next();
            if (aValue.variable().getAssignment()==null)
                penalty += getPenaltyA(selectedValue,aValue);
            }
        for (Iterator it1=conflicts.iterator(); it1.hasNext(); ) {
            Value conflictValue = (Value)it1.next();
            Value initialValue = conflictValue.variable().getInitialAssignment();
            if (initialValue!=null) {
                if (initialValue.equals(conflictValue))
                    penalty += getPenaltyB(selectedValue, conflictValue, initialValue);
                else {
                    if (violations==null || !violations.contains(initialValue))
                        penalty += getPenaltyC(selectedValue, conflictValue, initialValue);
                }
            }
        }
        if (selectedValue.variable().getInitialAssignment()!=null && !selectedValue.equals(selectedValue.variable().getInitialAssignment()))
            penalty += getPenaltyD(selectedValue, selectedValue.variable().getInitialAssignment());
        return penalty;
    }
    
    public void getInfo(Dictionary info, Solution solution) {
        info.put("Perturbations: Total penalty", sDoubleFormat.format(getPerturbationPenalty(solution)));
    }
    
}
