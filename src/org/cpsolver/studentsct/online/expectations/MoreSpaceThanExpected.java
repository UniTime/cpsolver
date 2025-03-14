package org.cpsolver.studentsct.online.expectations;

import org.cpsolver.ifs.util.DataProperties;

/**
 * A class is considered over-expected, when there less space available than expected.
 * Much like the {@link PercentageOverExpected}, but with no ability to adjust the expectations.
 * Expectation rounding can be defined by OverExpected.Rounding parameter, defaults to round
 * (other values are none, ceil, and floor).<br><br>
 * Unlimited classes are never over-expected. A class is over-expected when the number of
 * enrolled students (including the student in question) + expectations is greater or equal
 * the section limit.
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a
 *          href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class MoreSpaceThanExpected extends PercentageOverExpected {
	
	public MoreSpaceThanExpected(DataProperties config) {
		super(config);
		setPercentage(1.0);
	}
	
	public MoreSpaceThanExpected() {
		super();
	}

}
