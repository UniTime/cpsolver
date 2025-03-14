package org.cpsolver.ifs.example.rpp;

import org.cpsolver.ifs.model.Value;

/**
 * Location (value, i.e., a single placement of the rectangle). Location encodes
 * X and Y coordinate.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
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
public class Location extends Value<Rectangle, Location> {
    private int iX, iY;

    /**
     * Constructor
     * 
     * @param rectangle
     *            parent variable
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    public Location(Rectangle rectangle, int x, int y) {
        super(rectangle);
        iX = x;
        iY = y;
    }

    /** Gets x coordinate
     * @return x coordinate
     **/
    public int getX() {
        return iX;
    }

    /** Gets y coordinate
     * @return y coordinate
     **/
    public int getY() {
        return iY;
    }

    /**
     * Compare two coordinates. It is based on comparison of the parent
     * rectangle and x,y coordinates
     */
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Location)) {
            return false;
        }
        Location location = (Location) object;

        return (variable().equals(location.variable()) && location.getX() == getX() && location.getY() == getY());
    }

    /**
     * String representation (for debugging and printing purposes). For example,
     * rect43=[12,10] where rect43 is the name of the parent rectangle and
     * [12,10] is the location.
     */
    @Override
    public String toString() {
        return variable().getName() + "=[" + getX() + "," + getY() + "]";
    }

    /** Location's name. E.g., [12,10] where x=12 and y=10. */
    @Override
    public String getName() {
        return "[" + getX() + "," + getY() + "]";
    }

    /** Returns true if the given location intersects with this location 
     * @param anotherLocation given location
     * @return true if the given location intersects with this location
     **/
    public boolean hasIntersection(Location anotherLocation) {
        if (getX() + (variable()).getWidth() <= anotherLocation.getX()) {
            return false;
        }
        if (getY() + (variable()).getHeight() <= anotherLocation.getY()) {
            return false;
        }
        if (anotherLocation.getX() + (anotherLocation.variable()).getWidth() <= getX()) {
            return false;
        }
        if (anotherLocation.getY() + (anotherLocation.variable()).getHeight() <= getY()) {
            return false;
        }
        return true;
    }
}
