/**
 * Random Binary CSP with uniform distribution.
 * <br>
 * <br>
 * A random CSP is defined by a four-tuple (n, d, p1, p2), where n
 * denotes the number of variables and d denotes the domain size of each
 * variable, p1 and p2 are two probabilities. They are used to generate
 * randomly the binary constraints among the variables. p1 represents the
 * probability that a constraint exists between two different variables
 * and p2 represents the probability that a pair of values in the domains
 * of two variables connected by a constraint are incompatible.
 * <br>
 * <br>
 * We use a so called model B of Random CSP (n, d, n1, n2) where n1 =
 * p1*n*(n-1)/2 pairs of variables are randomly and uniformly selected
 * and binary constraints are posted between them. For each constraint,
 * n2 = p1*d^2 randomly and uniformly selected pairs of values are picked
 * as incompatible.
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
package org.cpsolver.ifs.example.csp;