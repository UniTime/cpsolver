package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.coursett.constraint.FlexibleConstraint;
import net.sf.cpsolver.coursett.constraint.FlexibleConstraint.FlexibleConstraintType;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * The class encapsulates various flexible constraints concerning compact timetables of
 * instructors. 
 * 
 * <br>
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2012 Matej Lukac<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class FlexibleConstraintCriterion extends TimetablingCriterion  {
    
    private boolean iDebug;
    
    public FlexibleConstraintCriterion(){       
        iValueUpdateType = ValueUpdateType.NoUpdate;
    }

    @Override
    public boolean init(Solver<Lecture, Placement> solver) {   
        super.init(solver);
        
        iWeight = solver.getProperties().getPropertyDouble("FlexibleConstraint.Weight", 1.0d); 
        iDebug = solver.getProperties().getPropertyBoolean("FlexibleConstraint.Debug", true); 
        return true; 
    }

    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.FlexibleConstrPreferenceWeight";
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        TimetableModel m = (TimetableModel)getModel();
        if (m.getFlexibleConstraints().isEmpty()) return;
        
        for (FlexibleConstraintType type: FlexibleConstraintType.values()) {
            StringBuilder debug = null;
            int violated = 0, constraints = 0;

            for (FlexibleConstraint c : m.getFlexibleConstraints()) {
                if (type.equals(c.getType())) {
                    constraints ++;
                    if (!c.isConsistent(null, null)) {
                        violated++;
                        if (iDebug) {
                            if (debug == null)
                                debug = new StringBuilder(c.getOwner() + " (" + sDoubleFormat.format(c.getNrViolations(new HashSet<Placement>(), null)) + ")");
                            else
                                debug.append("; " + c.getOwner() + " (" + sDoubleFormat.format(c.getNrViolations(new HashSet<Placement>(), null)) + ")");
                        }
                    }
                }
            }
            
            if (constraints > 0) {
                info.put(type.getName() + " Constraints", getPerc(violated, 0, constraints) + "% (" + violated + ")");
                if (iDebug && violated > 0) info.put(type.getName() + " Violations", debug.toString());
            }
        }
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
        for (FlexibleConstraintType type: FlexibleConstraintType.values()) {
            
            Set<FlexibleConstraint> constraints = new HashSet<FlexibleConstraint>();
            for (Lecture lecture : variables) {
                for (FlexibleConstraint c : lecture.getFlexibleGroupConstraints()) {
                    if (type.equals(c.getType())) constraints.add(c);
                }
            }
            
            if (!constraints.isEmpty()) {
                int violated = 0;
                StringBuilder debug = null;
                for (FlexibleConstraint c : constraints) {            
                    if (!c.isConsistent(null, null)) {
                        violated++;
                        if (iDebug) {
                            if (debug == null)
                                debug = new StringBuilder(c.getOwner());
                            else
                                debug.append("; " + c.getOwner());
                        }
                    }        
                }
                info.put(type.getName() + " Constraints", getPerc(violated, 0, constraints.size()) + "% (" + violated + ")");
                if (iDebug && violated > 0) info.put(type.getName() + " Violations", debug.toString());
            }
        }
    }
    
    @Override
    public double getValue(Collection<Lecture> variables) { 
        Set<FlexibleConstraint> flexibleConstraints = new HashSet<FlexibleConstraint>();
        for (Lecture lecture: variables){
            flexibleConstraints.addAll(lecture.getFlexibleGroupConstraints());
        }
        int ret = 0;
        for (FlexibleConstraint gc: flexibleConstraints){
            ret += gc.getCurrentPreference(null, null);
        }       
        return ret;
    }  
    
    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
        assignments.put(value.variable(), value);      
        
        double ret = 0.0;        
        for (FlexibleConstraint gc : value.variable().getFlexibleGroupConstraints())
            ret += gc.getCurrentPreference(conflicts, assignments);
        
        assignments.put(value.variable(), null);
        for (FlexibleConstraint gc : value.variable().getFlexibleGroupConstraints())
            ret -= gc.getCurrentPreference(conflicts, assignments);
        
        return ret;
    }   
}
