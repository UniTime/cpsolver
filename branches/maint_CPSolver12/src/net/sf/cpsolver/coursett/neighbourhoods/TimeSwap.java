package net.sf.cpsolver.coursett.neighbourhoods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Try to assign a class with a new time. A class is selected randomly, a
 * different time is randomly selected for the class -- the class is
 * assigned into the new time. If the time is used or there is some other conflict, 
 * it tries to resolve these conflicts by assigning conflicting classes to other
 * times as well.
 * <br>
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
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
public class TimeSwap extends RandomSwapMove<Lecture, Placement> {

    public TimeSwap(DataProperties config) {
        super(config);
    }

    @Override
    public void init(Solver<Lecture, Placement> solver) {
    }

    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        TimetableModel model = (TimetableModel)solution.getModel();
        double total = model.getTotalValue();
        int varIdx = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            Lecture lecture = model.variables().get((i + varIdx) % model.variables().size());
            Placement old = lecture.getAssignment();
            if (old == null) continue;

            List<TimeLocation> values = lecture.timeLocations();
            if (values.isEmpty()) continue;
            
            int attempts = 0;
            long startTime = JProf.currentTimeMillis();
            int valIdx = ToolBox.random(values.size());
            for (int j = 0; j < values.size(); j++) {
                TimeLocation time = values.get((j + valIdx) % values.size());
                if (time.getPreference() > 50) continue;
                if (time.equals(old.getTimeLocation())) continue;
                
                Placement placement = null;
                if (lecture.getNrRooms() == 0)
                    placement = new Placement(lecture, time, (RoomLocation) null);
                else if (lecture.getNrRooms() == 1)
                    placement = new Placement(lecture, time, old.getRoomLocation());
                else
                    placement = new Placement(lecture, time, old.getRoomLocations());
                if (!placement.isValid()) continue;

                Set<Placement> conflicts = model.conflictValues(placement);
                if (conflicts.contains(placement)) continue;
                if (conflicts.isEmpty()) {
                    SimpleNeighbour<Lecture, Placement> n = new SimpleNeighbour<Lecture, Placement>(lecture, placement);
                    if (!iHC || n.value() <= 0) return n;
                    else continue;
                }
                
                Map<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
                assignments.put(lecture, placement);
                
                for (Placement conflict: conflicts)
                    conflict.variable().unassign(solution.getIteration());
                lecture.assign(solution.getIteration(), placement);
                
                Double v = resolve(solution, total, startTime, assignments, new ArrayList<Placement>(conflicts), 0);
                if (!conflicts.isEmpty())
                    attempts ++;
                
                lecture.unassign(solution.getIteration());
                for (Placement conflict: conflicts)
                    conflict.variable().assign(solution.getIteration(), conflict);
                lecture.assign(solution.getIteration(), old);
                
                if (v != null)
                    return new SwapNeighbour(assignments.values(), v);
                
                if (attempts >= iMaxAttempts) break;
            }
        }
        return null;
    }
    
    
    @Override
    public Double resolve(Solution<Lecture, Placement> solution, double total, long startTime, Map<Lecture, Placement> assignments, List<Placement> conflicts, int index) {
        if (index == conflicts.size()) return solution.getModel().getTotalValue() - total;
        Placement conflict = conflicts.get(index);
        Lecture variable = conflict.variable();
        
        List<TimeLocation> values = variable.timeLocations();
        if (values.isEmpty()) return null;
        
        int valIdx = ToolBox.random(values.size());
        int attempts = 0;
        for (int i = 0; i < values.size(); i++) {
            TimeLocation time = values.get((i + valIdx) % values.size());
            if (time.getPreference() > 50) continue;
            if (time.equals(conflict.getTimeLocation())) continue;
            
            Placement value = null;
            if (variable.getNrRooms() == 0)
                value = new Placement(variable, time, (RoomLocation) null);
            else if (variable.getNrRooms() == 1)
                value = new Placement(variable, time, conflict.getRoomLocation());
            else
                value = new Placement(variable, time, conflict.getRoomLocations());
            if (!value.isValid() || solution.getModel().inConflict(value)) continue;
            
            variable.assign(solution.getIteration(), value);
            Double v = resolve(solution, total, startTime, assignments, conflicts, 1 + index);
            variable.unassign(solution.getIteration());
            attempts ++;
            
            if (v != null && (!iHC || v <= 0)) {
                assignments.put(variable, value);
                return v;
            }
            if (attempts >= iMaxAttempts || isTimeLimitReached(startTime)) break;
        }
            
        return null;
    }
}