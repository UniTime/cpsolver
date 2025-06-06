package org.cpsolver.studentsct.online.expectations;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;

/**
 * Class is over-expected when {@link Section#getPenalty()} is not negative.
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
public class PenaltyNotNegative implements OverExpectedCriterion {

    public PenaltyNotNegative(DataProperties config) {
    }

    @Override
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        if (section.getPenalty() < 0 || section.getLimit() <= 0.0)
            return 0.0;
        int subparts = section.getSubpart().getConfig().getSubparts().size();
        return 1.0 / subparts;
    }

    @Override
    public String toString() {
        return "not-negative";
    }

    @Override
    public Integer getExpected(int sectionLimit, double expectedSpace) {
        if (sectionLimit <= 0.0)
            return null;

        int expected = (int) Math.round(expectedSpace);
        if (expected > 0)
            return expected;

        return null;
    }
}
