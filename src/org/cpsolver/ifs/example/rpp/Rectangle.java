package org.cpsolver.ifs.example.rpp;

import java.util.ArrayList;

import org.cpsolver.ifs.model.Variable;



/**
 * Rectangle (variable). It encodes the name, width and height of the rectangle,
 * minimal and maximal position of the rectangle. It also contains an
 * information about prohibited X and Y coordinate (for MPP).
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
public class Rectangle extends Variable<Rectangle, Location> {
    private String iName;
    private int iMinX, iMaxX, iMinY, iMaxY;
    private int iHeight, iWidth;
    private int iProhibitedX = -1, iProhibitedY = -1;

    /**
     * Constructor.
     * 
     * @param name
     *            variable's name
     * @param width
     *            width of the rectangle
     * @param height
     *            height of the rectangle
     * @param minX
     *            minimal X-coordinate
     * @param maxX
     *            maximal X-coordinate
     * @param minY
     *            minimal Y-coordinate
     * @param maxY
     *            maximal Y-coordinate
     * @param initialLocation
     *            initial location (null if none)
     */
    public Rectangle(String name, int width, int height, int minX, int maxX, int minY, int maxY,
            Location initialLocation) {
        super(initialLocation);
        iName = name;
        iWidth = width;
        iHeight = height;
        iMinX = minX;
        iMaxX = maxX;
        iMinY = minY;
        iMaxY = maxY;
        setValues(computeValues());
    }

    /**
     * Prohibits given X and Y coordinates (for MPP).
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void setProhibited(int x, int y) {
        iProhibitedX = x;
        iProhibitedY = y;
        setValues(computeValues());
        if (getInitialAssignment() != null && !values(null).contains(getInitialAssignment()))
            setInitialAssignment(null);
    }

    /**
     * Prohibits given initial location (for MPP).
     */
    public void setProhibited() {
        if (getInitialAssignment() == null)
            return;
        setProhibited((getInitialAssignment()).getX(), (getInitialAssignment()).getY());
    }

    /**
     * Returns true if the given location is prohibited. This means that either
     * X or Y equals to the prohibited X or Y coordinate respectively.
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if the given location is prohibited
     */
    public boolean isProhibited(int x, int y) {
        return (iProhibitedX == x || iProhibitedY == y);
    }

    public int getProhibitedX() {
        return iProhibitedX;
    }

    public int getProhibitedY() {
        return iProhibitedY;
    }

    public int getMinX() {
        return iMinX;
    }

    public int getMaxX() {
        return iMaxX;
    }

    public int getMinY() {
        return iMinY;
    }

    public int getMaxY() {
        return iMaxY;
    }

    /** Returns width of the rectangle 
     * @return width of the rectangle
     **/
    public int getWidth() {
        return iWidth;
    }

    /** Returns height of the rectangle
     * @return height of the rectangle
     **/
    public int getHeight() {
        return iHeight;
    }

    /** Returns name of the rectangle */
    @Override
    public String getName() {
        return iName;
    }

    /** Set the bounds (minimal and maximal values of X and Y coordinates). 
     * @param minX X minimum
     * @param maxX X maximum
     * @param minY Y minimum
     * @param maxY Y maximum
     **/
    public void setBounds(int minX, int maxX, int minY, int maxY) {
        iMinX = minX;
        iMaxX = maxX;
        iMinY = minY;
        iMaxY = maxY;
        if (getInitialAssignment() != null && !values(null).contains(getInitialAssignment()))
            setInitialAssignment(null);
    }

    private ArrayList<Location> computeValues() {
        ArrayList<Location> locations = new ArrayList<Location>((iMaxX - iMinX) * (iMaxY - iMinY));
        for (int x = iMinX; x <= iMaxX; x++) {
            for (int y = iMinY; y <= iMaxY; y++) {
                if (!isProhibited(x, y)) {
                    Location val = new Location(this, x, y);
                    locations.add(val);
                    if (getInitialAssignment() != null && getInitialAssignment().equals(val))
                        setInitialAssignment(val);
                    if (getBestAssignment() != null && getBestAssignment().equals(val))
                        setBestAssignment(val, 0l);
                }
            }
        }
        return locations;
    }

    /** String representation (for printing and debugging purposes) */
    @Override
    public String toString() {
        return "Rectangle{name='" + getName() + "', size=[" + getWidth() + "," + getHeight() + "], bounds=[" + iMinX
                + ".." + iMaxX + "," + iMinY + ".." + iMaxY + "], super=" + super.toString() + "}";
    }

    /** Compares two rectangles (based on rectangle names) */
    @Override
    public boolean equals(Object o) {
        return ((Rectangle) o).getName().equals(getName());
    }
}
