package org.cpsolver.ifs.example.rpp;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;


/**
 * Resource constraint (rectangular area where the rectangles are to be placed).
 * It prohibits overlapping of the placed rectangles.
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
public class ResourceConstraint extends ConstraintWithContext<Rectangle, Location, ResourceConstraint.Context> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(ResourceConstraint.class);
    private int iWidth, iHeight;

    /**
     * Constructor.
     * 
     * @param width
     *            area width
     * @param height
     *            area height
     */
    public ResourceConstraint(int width, int height) {
        super();
        iWidth = width;
        iHeight = height;
    }
    
    /**
     * Compute conflicts with the given placement of the rectangle. This means
     * the rectangles which are already placed and which are overlapping with
     * the given assignment.
     */
    @Override
    public void computeConflicts(Assignment<Rectangle, Location> assignment, Location placement, Set<Location> conflicts) {
        Rectangle rectangle = placement.variable();
        Context context = getContext(assignment);
        for (int x = placement.getX(); x < Math.min(iWidth, placement.getX() + rectangle.getWidth()); x++)
            for (int y = placement.getY(); y < Math.min(iHeight, placement.getY() + rectangle.getHeight()); y++)
                if (context.getRectangle(x, y) != null)
                    conflicts.add(context.getRectangle(x, y).getAssignment(assignment));
    }

    /**
     * Returns true if there is a rectangle which overlaps with the given
     * assignment.
     */
    @Override
    public boolean inConflict(Assignment<Rectangle, Location> assignment, Location placement) {
        Rectangle rectangle = placement.variable();
        Context context = getContext(assignment);
        for (int x = placement.getX(); x < Math.min(iWidth, placement.getX() + rectangle.getWidth()); x++)
            for (int y = placement.getY(); y < Math.min(iHeight, placement.getY() + rectangle.getHeight()); y++)
                if (context.getRectangle(x, y) != null)
                    return true;
        return false;
    }

    /**
     * Returns true if the given rectangles (assignments) do not overlap.
     */
    @Override
    public boolean isConsistent(Location p1, Location p2) {
        Rectangle r1 = p1.variable();
        Rectangle r2 = p2.variable();
        if (p2.getX() + r2.getWidth() <= p1.getX())
            return true;
        if (p2.getX() >= p1.getX() + r1.getWidth())
            return true;
        if (p2.getY() + r2.getHeight() <= p1.getY())
            return true;
        if (p2.getY() >= p1.getY() + r1.getHeight())
            return true;
        return false;
    }

    public void check(Assignment<Rectangle, Location> assignment) {
        sLogger.debug("check");
        Context context = getContext(assignment);
        for (Rectangle rectangle : variables()) {
            Location placement = assignment.getValue(rectangle);
            if (placement == null) {
                sLogger.warn("Rectangle " + rectangle.getName() + " is not assigned.");
                continue;
            }
            sLogger.debug("Checking " + rectangle.getName() + "    (assigned:" + placement.getName() + ", prohibited:"
                    + rectangle.isProhibited(placement.getX(), placement.getY()) + ", initial:"
                    + rectangle.getInitialAssignment() + ", prohibited:[" + rectangle.getProhibitedX() + ","
                    + rectangle.getProhibitedY() + "])");
            if (placement.getX() == rectangle.getProhibitedX() || placement.getY() == rectangle.getProhibitedY())
                sLogger.error("Placement is prohibited.");
            if (placement.getX() < rectangle.getMinX() || placement.getX() > rectangle.getMaxX()
                    || placement.getY() < rectangle.getMinY() || placement.getY() > rectangle.getMaxY())
                sLogger.error("Placement is outside bounds.");
            for (int x = placement.getX(); x < Math.min(iWidth, placement.getX() + rectangle.getWidth()); x++)
                for (int y = placement.getY(); y < Math.min(iHeight, placement.getY() + rectangle.getHeight()); y++) {
                    if (context.getRectangle(x, y) == null || !context.getRectangle(x, y).equals(rectangle))
                        sLogger.error("Problem at [" + x + "," + y + "], " + context.getRectangle(x, y) + " is assigned there.");
                }
        }
        sLogger.debug(toString());
    }

    /**
     * String representation of the constraint (for debugging and printing
     * purposes).
     */
    @Override
    public String toString() {
        return "ResourceConstraint{" + iWidth + "x" + iHeight + "}";
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Rectangle, Location> assignment) {
        return new Context(assignment);
    }
    
    /**
     * Assignment context
     */
    public class Context implements AssignmentConstraintContext<Rectangle, Location> {
        private Rectangle[][] iResource;
        
        public Context(Assignment<Rectangle, Location> assignment) {
            iResource = new Rectangle[iWidth][iHeight];
            for (int x = 0; x < iWidth; x++)
                for (int y = 0; y < iHeight; y++)
                    iResource[x][y] = null;
            for (Location location: assignment.assignedValues())
                assigned(assignment, location);
        }

        /**
         * Notification, when a rectangle is placed. It memorizes the rectangle's
         * new position in 2D ([0..width][0..height]) array. It is used for faster
         * lookup when computing conflicts.
         */
        @Override
        public void assigned(Assignment<Rectangle, Location> assignment, Location placement) {
            Rectangle rectangle = placement.variable();
            for (int x = placement.getX(); x < Math.min(iWidth, placement.getX() + rectangle.getWidth()); x++)
                for (int y = placement.getY(); y < Math.min(iHeight, placement.getY() + rectangle.getHeight()); y++) {
                    iResource[x][y] = rectangle;
                }
        }
        
        public Rectangle getRectangle(int x, int y) {
            return iResource[x][y];
        }

        /**
         * Notification, when a rectangle is unplaced. It removes the rectangle from
         * the 2D ([0..width][0..height]) array.
         */
        @Override
        public void unassigned(Assignment<Rectangle, Location> assignment, Location placement) {
            Rectangle rectangle = placement.variable();
            for (int x = placement.getX(); x < Math.min(iWidth, placement.getX() + rectangle.getWidth()); x++)
                for (int y = placement.getY(); y < Math.min(iHeight, placement.getY() + rectangle.getHeight()); y++) {
                    iResource[x][y] = null;
                }
        }
        
    }
}
