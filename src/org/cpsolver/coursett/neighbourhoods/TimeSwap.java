package org.cpsolver.coursett.neighbourhoods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Try to assign a class with a new time. A class is selected randomly, a
 * different time is randomly selected for the class -- the class is
 * assigned into the new time. If the time is used or there is some other conflict, 
 * it tries to resolve these conflicts by assigning conflicting classes to other
 * times as well.
 * <br>
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
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
        Assignment<Lecture, Placement> assignment = solution.getAssignment();
        double total = model.getTotalValue(assignment);
        int varIdx = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            Lecture lecture = model.variables().get((i + varIdx) % model.variables().size());
            Placement old = lecture.getAssignment(assignment);
            if (old == null) continue;

            List<TimeLocation> values = lecture.timeLocations();
            if (values.isEmpty()) continue;
            
            Lock lock = solution.getLock().writeLock();
            lock.lock();
            try {
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

                    Set<Placement> conflicts = model.conflictValues(assignment, placement);
                    if (conflicts.contains(placement)) continue;
                    if (conflicts.isEmpty()) {
                        SimpleNeighbour<Lecture, Placement> n = new SimpleNeighbour<Lecture, Placement>(lecture, placement);
                        if (!iHC || n.value(assignment) <= 0) return n;
                        else continue;
                    }
                    
                    Map<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
                    assignments.put(lecture, placement);
                    
                    for (Placement conflict: conflicts)
                        assignment.unassign(solution.getIteration(), conflict.variable());
                    assignment.assign(solution.getIteration(), placement);
                    
                    Double v = resolve(solution, total, startTime, assignments, new ArrayList<Placement>(conflicts), 0);
                    if (!conflicts.isEmpty())
                        attempts ++;
                    
                    assignment.unassign(solution.getIteration(), lecture);
                    for (Placement conflict: conflicts)
                        assignment.assign(solution.getIteration(), conflict);
                    assignment.assign(solution.getIteration(), old);
                    
                    if (v != null)
                        return new SwapNeighbour(assignments.values(), v);
                    
                    if (attempts >= iMaxAttempts) break;
                }
            } finally {
                lock.unlock();
            }
        }
        return null;
    }
    
    
    @Override
    public Double resolve(Solution<Lecture, Placement> solution, double total, long startTime, Map<Lecture, Placement> assignments, List<Placement> conflicts, int index) {
        Assignment<Lecture, Placement> assignment = solution.getAssignment();
        
        if (index == conflicts.size()) return solution.getModel().getTotalValue(assignment) - total;
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
            if (!value.isValid() || solution.getModel().inConflict(assignment, value)) continue;
            
            assignment.assign(solution.getIteration(), value);
            Double v = resolve(solution, total, startTime, assignments, conflicts, 1 + index);
            assignment.unassign(solution.getIteration(), variable);
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