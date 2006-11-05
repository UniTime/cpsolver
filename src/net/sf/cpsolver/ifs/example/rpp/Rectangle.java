package net.sf.cpsolver.ifs.example.rpp;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Rectangle (variable). 
 * It encodes the name, width and height of the rectangle, minimal and maximal position of the rectangle.
 * It also contains an information about prohibited X and Y coordinate (for MPP).
 * 
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class Rectangle extends Variable {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Rectangle.class);
    private String iName;
    private int iMinX, iMaxX, iMinY, iMaxY;
    private int iHeight, iWidth;
    private int iProhibitedX = -1, iProhibitedY = -1;
    
    /**
     * Constructor.
     * @param name variable's name
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param minX minimal X-coordinate
     * @param maxX maximal X-coordinate
     * @param minY minimal Y-coordinate
     * @param maxY maximal Y-coordinate
     * @param initialLocation initial location (null if none)
     */
    public Rectangle(String name, int width, int height, int minX, int maxX, int minY, int maxY,Location initialLocation) {
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
     */
    public void setProhibited(int x, int y) {
        iProhibitedX = x;
        iProhibitedY = y;
        setValues(computeValues());
        if (getInitialAssignment()!=null && !values().contains(getInitialAssignment()))
            setInitialAssignment(null);
    }
    
    /**
     * Prohibits given initial location (for MPP).
     */
    public void setProhibited() {
        if (getInitialAssignment()==null) return;
        setProhibited(((Location)getInitialAssignment()).getX(),((Location)getInitialAssignment()).getY());
    }
    
    /**
     * Returns true if the given location is prohibited. This means that either X or Y equals to the prohibited X or Y coordinate respectively.
     */
    public boolean isProhibited(int x, int y) {
        return (iProhibitedX == x || iProhibitedY == y);
    }
    
    public int getProhibitedX() { return iProhibitedX; }
    public int getProhibitedY() { return iProhibitedY; }
    public int getMinX() { return iMinX; }
    public int getMaxX() { return iMaxX; }
    public int getMinY() { return iMinY; }
    public int getMaxY() { return iMaxY; }
    
    /** Returns width of the rectangle */
    public int getWidth() {
        return iWidth;
    }
    
    /** Returns height of the rectangle */
    public int getHeight() {
        return iHeight;
    }
    
    /** Returns name of the rectangle */
    public String getName() {
        return iName;
    }
    
    /** Set the bounds (minimal and maximal values of X and Y coordinates). */
    public void setBounds(int minX, int maxX, int minY, int maxY) {
        iMinX = minX;
        iMaxX = maxX;
        iMinY = minY;
        iMaxY = maxY;
        if (getInitialAssignment() != null && !values().contains(getInitialAssignment()))
            setInitialAssignment(null);
    }
    
    private Vector computeValues() {
        Vector locations = new FastVector((iMaxX - iMinX) * (iMaxY - iMinY));
        for (int x = iMinX; x <= iMaxX; x++) {
            for (int y = iMinY; y <= iMaxY; y++) {
                if (!isProhibited(x, y)) {
                    Value val = new Location(this, x, y);
                    locations.addElement(val);
                    if (getInitialAssignment() != null && getInitialAssignment().equals(val))
                        setInitialAssignment(val);
                    if (getBestAssignment() != null && getBestAssignment().equals(val))
                        setBestAssignment(val);
                }
            }
        }
        return locations;
    }
    
    /** String representation (for printing and debugging purposes) */
    public String toString() {
        return "Rectangle{name='" + getName() + "', size=[" + getWidth() + "," + getHeight() + "], bounds=[" + iMinX + ".." + iMaxX + "," + iMinY + ".." + iMaxY + "], super=" + super.toString() + "}";
    }
    
    /** Compares two rectangles (based on rectangle names)*/
    public boolean equals(Object o) {
        return ((Rectangle)o).getName().equals(getName());
    }
}
