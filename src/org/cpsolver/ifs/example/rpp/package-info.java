/**
 * Random Placement Problem.
 * <br>
 * <br>
 * The random placement problem (RPP; for more details, see
 * <a href='http://www.fi.muni.cz/~hanka/rpp/'>http://www.fi.muni.cz/~hanka/rpp</a>)
 * seeks to place a set of randomly generated rectangles (called objects)
 * of different sizes into a larger rectangle (called placement area) in
 * such a way that no objects overlap and all objects' borders are
 * parallel to the border of the placement area. In addition, a set of
 * allowable placements can be randomly generated for each object. The
 * ratio between the total area of all objects and the size of the
 * placement area will be denoted as the filled area ratio.
 * <br>
 * <br>
 * RPP allows us to generate various instances of the problem similar to
 * a trivial timetabling problem. The correspondence is as follows: the
 * object corresponds to a course to be timetabled; the x-coordinate to
 * its time, the y-coordinate to its classroom. For example, a course
 * taking three hours corresponds to an object with dimensions 3x1 (the
 * course should be taught in one classroom only). Each course can be
 * placed only in a classroom of sufficient capacity; we can expect that
 * the classrooms are ordered increasingly in their size so each object
 * will have a lower bound on its y-coordinate.
 * <br>
 * <br>
 * MPP instances were generated as follows: First, the initial solution
 * was computed. The changed problem differs from the initial problem by
 * input perturbations. An input perturbation means that both x
 * coordinate and y coordinate of a rectangle must differ from the
 * initial values, i.e., x!=xinitial and y!=yinitial. For a single initial
 * problem and for a given number of input perturbations, we can randomly
 * generate various changed problems. In particular, for a given number
 * of input perturbations, we randomly select a set of objects which
 * should have input perturbations. The solution to MPP can be evaluated
 * by the number of additional perturbations. They are given by
 * subtraction of the final number of perturbations and the number of
 * input perturbations.
 * 
 * @author  Tomas Muller
 * @version IFS 1.4 (Instructor Sectioning)<br>
 *          Copyright (C) 2024 Tomas Muller<br>
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
package org.cpsolver.ifs.example.rpp;