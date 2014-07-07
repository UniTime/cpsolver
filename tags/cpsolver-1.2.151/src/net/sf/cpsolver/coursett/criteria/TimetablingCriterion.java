package net.sf.cpsolver.coursett.criteria;

import net.sf.cpsolver.coursett.heuristics.PlacementSelection;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.criteria.AbstractCriterion;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Abstract class for all timetabling criteria. On top of the {@link AbstractCriterion}, it provides
 * weights for the {@link PlacementSelection} heuristics.
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
public abstract class TimetablingCriterion extends AbstractCriterion<Lecture, Placement> {
    private double[] iPlacementSelectionWeight = null;

    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        super.init(solver);
        if (getPlacementSelectionWeightName() != null) {
            iPlacementSelectionWeight = new double[] { 0.0, 0.0, 0.0 };
            for (int i = 0; i < 3; i++)
                iPlacementSelectionWeight[i] = solver.getProperties().getPropertyDouble(getPlacementSelectionWeightName() + (1 + i), getPlacementSelectionWeightDefault(i));
        }
        return true;
    }
    
    public String getPlacementSelectionWeightName() {
        return "Placement." + getClass().getName().substring(1 + getClass().getName().lastIndexOf('.')) + "Weight";
    }
    
    public double getPlacementSelectionWeight(int level) {
        return iPlacementSelectionWeight == null ? 0.0 : iPlacementSelectionWeight[level];
    }
    
    public double getPlacementSelectionWeightDefault(int level) {
        return (level <= 1 ? getWeight() : 0.0);
    }

    /** Abbreviated name of the criterion for {@link TimetablingCriterion#toString()}. */
    public String getAbbreviation() {
        return getName().replaceAll("[a-z ]","");
    }
    
    @Override
    public String toString() {
        double val = getValue();
        if (Math.abs(val) < 0.005 || getWeight() <= 0.01) return "";
        double[] bounds = getBounds();
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1] && getName().endsWith(" Preferences"))
            return getAbbreviation() + ":" + getPerc(val, bounds[0], bounds[1]) + "%";
        else if (bounds[1] <= val && val <= bounds[0] && bounds[1] < bounds[0] && getName().endsWith(" Preferences"))
            return getAbbreviation() + ":" + getPercRev(val, bounds[1], bounds[0]) + "%";
        else if (bounds[0] != val || val != bounds[1])
            return getAbbreviation() + ":" + sDoubleFormat.format(getValue());
        else
            return "";
    }
}
