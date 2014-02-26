package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Important student conflicts. Some student conflicts can be counted differently,
 * using Comparator.ImportantStudentConflictWeight. Importance of a conflict is
 * defined by the student - offering request priority {@link Student#getPriority(Long)}.
 *   
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
    
    @Override
    protected double jointEnrollment(JenrlConstraint jenrl) {
        return jenrl.priority();
    }
    
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return super.inConflict(p1, p2) && important(p1, p2); 
    }
    
    public boolean important(Placement p1, Placement p2) {
        JenrlConstraint jenrl = (p1 == null || p2 == null ? null : p1.variable().jenrlConstraint(p2.variable()));
        return jenrl != null && jenrl.priority() > 0.0; 
    }
    
    @Override
    public void incJenrl(JenrlConstraint jenrl, double studentWeight, Double conflictPriority, Student student) {
        if (super.inConflict(jenrl.first().getAssignment(), jenrl.second().getAssignment()) && conflictPriority != null)
            iValue += studentWeight * conflictPriority;
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
            info.put("Important student conflicts", sDoubleFormat.format(conf) + (hard > 0.0 ? " [hard: " + sDoubleFormat.format(hard) + "]" : ""));
        }
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(info, variables);
        double conf = getValue(variables);
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(ImportantStudentHardConflict.class);
            double hard = (c == null ? 0.0 : c.getValue(variables));
            info.put("Important student conflicts", sDoubleFormat.format(conf) + (hard > 0.0 ? " [hard: " + sDoubleFormat.format(hard) + "]" : ""));
        }
    }

}
