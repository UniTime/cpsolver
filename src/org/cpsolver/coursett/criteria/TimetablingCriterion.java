package org.cpsolver.coursett.criteria;

import org.cpsolver.coursett.heuristics.PlacementSelection;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;

/**
 * Abstract class for all timetabling criteria. On top of the {@link AbstractCriterion}, it provides
 * weights for the {@link PlacementSelection} heuristics.
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public abstract class TimetablingCriterion extends AbstractCriterion<Lecture, Placement> {
    private double[] iPlacementSelectionWeight = null;
    private Double[][] iPlacementSelectionAdjusts = null;

    @Override
    public void configure(DataProperties properties) {
        super.configure(properties);
        if (getPlacementSelectionWeightName() != null) {
            iPlacementSelectionWeight = new double[] { 0.0, 0.0, 0.0 };
            for (int i = 0; i < 3; i++)
                iPlacementSelectionWeight[i] = properties.getPropertyDouble(getPlacementSelectionWeightName() + (1 + i), getPlacementSelectionWeightDefault(i));
            iPlacementSelectionAdjusts = new Double[][] { null, null, null};
            for (int i = 0; i < 3; i++) {
                iPlacementSelectionAdjusts[i] = properties.getPropertyDoubleArry(getPlacementSelectionAdjustmentsName() + (1 + i), properties.getPropertyDoubleArry(getPlacementSelectionAdjustmentsName(), null));
            }
        }
    }
    
    public String getPlacementSelectionWeightName() {
        return "Placement." + getClass().getName().substring(1 + getClass().getName().lastIndexOf('.')) + "Weight";
    }
    
    public String getPlacementSelectionAdjustmentsName() {
        return getPlacementSelectionWeightName() + "Adjustments";
    }
    
    public double getPlacementSelectionWeight(int level, int idx) {
        double w = (iPlacementSelectionWeight == null ? 0.0 : iPlacementSelectionWeight[level]);
        if (idx < 0 || iPlacementSelectionAdjusts == null || iPlacementSelectionAdjusts[level] == null || idx >= iPlacementSelectionAdjusts[level].length || iPlacementSelectionAdjusts[level][idx] == null) return w;
        return w * iPlacementSelectionAdjusts[level][idx];
    }
    
    public double getPlacementSelectionWeightDefault(int level) {
        return (level <= 1 ? getWeight() : 0.0);
    }
}
