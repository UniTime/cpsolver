package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Important student hard conflicts. Same as {@link ImportantStudentConflict}, but
 * only hard student conflicts are counted (between classes that do not have alternatives,
 * see {@link Lecture#isSingleSection()}). Weighted by Comparator.ImportantHardStudentConflictWeight
 * parameter.
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
public class ImportantStudentHardConflict extends ImportantStudentConflict {

    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return super.isApplicable(l1, l2) && hard(l1, l2);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.ImportantHardStudentConflictWeight",
                3.0 * config.getPropertyDouble("Comparator.HardStudentConflictWeight", 5.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrImportantHardStudConfsWeight";
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
    }
}
