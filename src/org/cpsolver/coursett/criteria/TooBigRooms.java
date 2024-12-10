package org.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.preference.PreferenceCombination;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Too big rooms. This criterion counts cases where a class is placed in a room
 * that is too big for the class. In general, a room is discouraged (in this
 * criterion) if it has 25% more space than needed, strongly discouraged
 * if it has more than 50% space than needed. Needed space is counted as the space
 * of the smallest room in which a class can take place.
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
public class TooBigRooms extends TimetablingCriterion {
    private double iDiscouragedRoomSize, iStronglyDiscouragedRoomSize;

    @Override
    public void configure(DataProperties properties) {
        super.configure(properties);
        iDiscouragedRoomSize = properties.getPropertyDouble("TooBigRooms.DiscouragedRoomSize", 1.25);
        iStronglyDiscouragedRoomSize = properties.getPropertyDouble("TooBigRooms.StronglyDiscouragedRoomSize", 1.5);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.TooBigRoomWeight", 0.1);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.TooBigRoomWeight";
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        double ret = getPreference(value);
        if (conflicts != null)
            for (Placement conflict: conflicts)
                ret -= getPreference(conflict);
        return ret;
    }
    
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Lecture lect: variables) {
            if (lect.getNrRooms() > 0)
                bounds[0] += Constants.sPreferenceLevelStronglyDiscouraged;
        }
        return bounds;
    }
    
    private long getDiscouragedRoomSize(Placement value) {
        return Math.round(iDiscouragedRoomSize * value.variable().minRoomSize());
    }

    private long getStronglyDiscouragedRoomSize(Placement value) {
        return Math.round(iStronglyDiscouragedRoomSize * value.variable().minRoomSize());
    }
    
    public int getPreference(Placement value) {
        if (value.isMultiRoom()) {
            PreferenceCombination pref = PreferenceCombination.getDefault();
            for (RoomLocation r : value.getRoomLocations()) {
                if (r.getRoomSize() > getStronglyDiscouragedRoomSize(value))
                    pref.addPreferenceInt(Constants.sPreferenceLevelStronglyDiscouraged);
                else if (r.getRoomSize() > getDiscouragedRoomSize(value))
                    pref.addPreferenceInt(Constants.sPreferenceLevelDiscouraged);
            }
            return pref.getPreferenceInt();
        } else {
            if (value.getRoomLocation().getRoomSize() > getStronglyDiscouragedRoomSize(value))
                return Constants.sPreferenceLevelStronglyDiscouraged;
            else if (value.getRoomLocation().getRoomSize() > getDiscouragedRoomSize(value))
                return Constants.sPreferenceLevelDiscouraged;
            else
                return Constants.sPreferenceLevelNeutral;
        }
    }
    
    /** Use {@link TooBigRooms#getPreference(Placement)} instead. */
    @Deprecated
    public static int getTooBigRoomPreference(Placement value) {
        if (value.isMultiRoom()) {
            PreferenceCombination pref = PreferenceCombination.getDefault();
            for (RoomLocation r : value.getRoomLocations()) {
                if (r.getRoomSize() > Math.round(1.50 * value.variable().minRoomSize()))
                    pref.addPreferenceInt(Constants.sPreferenceLevelStronglyDiscouraged);
                else if (r.getRoomSize() > Math.round(1.25 * value.variable().minRoomSize()))
                    pref.addPreferenceInt(Constants.sPreferenceLevelDiscouraged);
            }
            return pref.getPreferenceInt();
        } else {
            if (value.getRoomLocation().getRoomSize() > Math.round(1.50 * value.variable().minRoomSize()))
                return Constants.sPreferenceLevelStronglyDiscouraged;
            else if (value.getRoomLocation().getRoomSize() > Math.round(1.25 * value.variable().minRoomSize()))
                return Constants.sPreferenceLevelDiscouraged;
            else
                return Constants.sPreferenceLevelNeutral;
        }
    }
}