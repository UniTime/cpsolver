package org.cpsolver.studentsct.online.expectations;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;

/**
 * Over-expected space depends on how much is the class over-expected. It works
 * like the {@link PercentageOverExpected}, however, the returned over-expected
 * penalty is based on the over-expected space (enrollment + expectations -
 * section limit), divided by the section limit. The over-expectations can be
 * capped by the OverExpected.Maximum parameter (defaults to section limit).<br>
 * <br>
 * Unlike the {@link PercentageOverExpected}, this criterion offers the ability
 * to prefer less over-expected sections among the sections that are
 * over-expected.
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
public class FractionallyOverExpected extends PercentageOverExpected {
    private Double iMaximum = null;

    public FractionallyOverExpected(DataProperties config) {
        super(config);
        iMaximum = config.getPropertyDouble("OverExpected.Maximum", iMaximum);
    }

    public FractionallyOverExpected(Double percentage, Double maximum) {
        super(percentage);
        iMaximum = maximum;
    }

    public FractionallyOverExpected(Double percentage) {
        this(percentage, 1.0);
    }

    public FractionallyOverExpected() {
        this(null, 1.0);
    }

    /**
     * Maximum, null if not set
     * @return maximum
     */
    public Double getMaximum() {
        return iMaximum;
    }

    /**
     * Maximum, section limit if not set
     * @param section given section
     * @return maximum
     */
    public double getMaximum(Section section) {
        return iMaximum == null || iMaximum <= 0.0 ? getLimit(section) : iMaximum;
    }

    @Override
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        if (section.getLimit() <= 0)
            return 0.0; // ignore unlimited & not available

        double expected = round(getPercentage() * section.getSpaceExpected());
        double enrolled = getEnrollment(assignment, section, request) + request.getWeight();
        double limit = getLimit(section);
        int subparts = section.getSubpart().getConfig().getSubparts().size();
        double max = getMaximum(section);

        return expected + enrolled > limit ? (Math.min(max, expected + enrolled - limit) / max) / subparts : 0.0;
    }

    @Override
    public String toString() {
        return "frac(" + getPercentage() + "," + getMaximum() + ")";
    }

}
