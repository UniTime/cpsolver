package net.sf.cpsolver.ifs.example.rpp;

import net.sf.cpsolver.ifs.model.*;

/**
 * Location (value, i.e., a single placement of the rectangle). Location encodes X and Y 
 * coordinate.
 * 
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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
public class Location extends Value {
    private int iX, iY;
    /**
     * Constructor
     * @param rectangle parent variable
     * @param x x coordinate
     * @param y y coordinate
     */
    public Location(Rectangle rectangle, int x, int y) {
        super(rectangle);
        iX = x;
        iY = y;
    }
    
    /** Gets x coordinate */
    public int getX() {
        return  iX;
    }
    
    /** Gets y coordinate */
    public int getY() {
        return iY;
    }
    
    /** Compare two coordinates. It is based on comparison of the parent rectangle and x,y coordinates */
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Location)) {
            return false;
        }
        Location location = (Location) object;
        
        return (variable().equals(location.variable()) && location.getX() == getX() && location.getY() == getY());
    }
    
    /** String representation (for debugging and printing purposes). 
     * For example, rect43=[12,10] where rect43 is the name of the parent rectangle and [12,10] is the location.
     */
    public String toString() {
        return variable().getName() + "=[" + getX() + "," + getY() + "]";
    }
    
    /** Location's name. E.g., [12,10] where x=12 and y=10.*/
    public String getName() {
        return "[" + getX() + "," + getY() + "]";
    }
    
    /** Returns true if the given location intersects with this location */
    public boolean hasIntersection(Location anotherLocation) {
        if (getX() + ((Rectangle) variable()).getWidth() <= anotherLocation.getX()) {
            return false;
        }
        if (getY() + ((Rectangle) variable()).getHeight() <= anotherLocation.getY()) {
            return false;
        }
        if (anotherLocation.getX() + ((Rectangle) anotherLocation.variable()).getWidth() <= getX()) {
            return false;
        }
        if (anotherLocation.getY() + ((Rectangle) anotherLocation.variable()).getHeight() <= getY()) {
            return false;
        }
        return true;
    }
}
