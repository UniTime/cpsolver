package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Important student conflicts. Some student conflicts can be counted differently,
 * using Comparator.ImportantStudentConflictWeight. "Important" conflicts are
 * between two classes that are managed by one of the schedulers (managing departments)
 * given by General.ImportantSchedulers parameter (comma separated list of department
 * ids).
 * .  
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2011 Tomas Muller<br>
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
public class ImportantStudentConflict extends StudentConflict {
    private Set<Long> iImportantOwners = new HashSet<Long>();
    
    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        for (Long id: solver.getProperties().getPropertyLongArry("General.ImportantSchedulers", new Long[] {}))
            iImportantOwners.add(id);
        return super.init(solver);
    }

    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return iImportantOwners.contains(l1.getScheduler()) && iImportantOwners.contains(l2.getScheduler());
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.ImportantStudentConflictWeight",
                3.0 * config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrImportantStudConfsWeight";
    }

    @Override
    public void getInfo(Map<String, String> info) {
        super.getInfo(info);
        double conf = getValue();
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(ImportantStudentHardConflict.class);
            double hard = (c == null ? 0.0 : c.getValue());
            info.put("Important student conflicts", Math.round(conf) + (hard > 0.0 ? " [hard: " + Math.round(hard) + "]" : ""));
        }
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(info, variables);
        double conf = getValue(variables);
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(ImportantStudentHardConflict.class);
            double hard = (c == null ? 0.0 : c.getValue(variables));
            info.put("Important student conflicts", Math.round(conf) + (hard > 0.0 ? " [hard: " + Math.round(hard) + "]" : ""));
        }
    }


}
