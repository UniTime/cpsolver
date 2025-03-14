package org.cpsolver.studentsct.online.expectations;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.online.OnlineConfig;
import org.cpsolver.studentsct.online.OnlineSection;

/**
 * A class is considered over-expected, when there less space available than expected. The
 * expectations can be increased by the given percentage (parameter OverExpected.Percentage,
 * defaults to 1.0).
 * Expectation rounding can be defined by OverExpected.Rounding parameter, defaults to round
 * (other values are none, ceil, and floor).<br><br>
 * Unlimited classes are never over-expected. A class is over-expected when the number of
 * enrolled students (including the student in question) + expectations (multiplied by
 * OverExpected.Percentage) is greater or equal the section limit.
 *  
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
public class PercentageOverExpected implements OverExpectedCriterion {
    private Double iPercentage = null;
    private Rounding iRounding = Rounding.ROUND;

    /**
     * Expectations rounding
     */
    public static enum Rounding {
        /** no rounding */
        NONE,
        /** ceiling, using {@link Math#ceil(double)} */
        CEIL,
        /** floor, using {@link Math#floor(double)} */
        FLOOR,
        /** rounding, using {@link Math#round(double)} */
        ROUND,
    }

    public PercentageOverExpected(DataProperties config) {
        iPercentage = config.getPropertyDouble("OverExpected.Percentage", iPercentage);
        iRounding = Rounding.valueOf(config.getProperty("OverExpected.Rounding", iRounding.name()).toUpperCase());
    }

    public PercentageOverExpected(Double percentage) {
        super();
        iPercentage = percentage;
    }

    public PercentageOverExpected() {
        this((Double) null);
    }

    /**
     * Over-expected percentage, defaults to 1.0
     * @return expectations adjustment
     */
    public double getPercentage() {
        return iPercentage == null ? 1.0 : iPercentage;
    }

    /**
     * Over-expected percentage, defaults to 1.0
     * @param percentage expectations adjustment
     */
    public void setPercentage(Double percentage) {
        iPercentage = percentage;
    }

    /**
     * Round the given value using the rounding from OverExpected.Rounding parameter
     * @param value given value
     * @return rounded value
     */
    protected double round(double value) {
        switch (iRounding) {
            case CEIL:
                return Math.ceil(value);
            case FLOOR:
                return Math.floor(value);
            case ROUND:
                return Math.round(value);
            default:
                return value;
        }
    }

    /**
     * Check if there are expectations on any of the sections of the given subpart
     * @param subpart given subpart
     * @return true if there is at least one section with positive {@link Section#getSpaceExpected()}
     */
    protected boolean hasExpectations(Subpart subpart) {
        for (Section section : subpart.getSections())
            if (round(section.getSpaceExpected()) > 0.0)
                return true;
        return false;
    }

    /**
     * Config enrollment (using {@link OnlineConfig#getEnrollment()} if applicable}, {@link Config#getEnrollmentWeight(Assignment, Request)} otherwise)
     * @param assignment current assignment
     * @param config given configuration
     * @param request given request
     * @return current enrollment of the section, excluding the request
     */
    protected double getEnrollment(Assignment<Request, Enrollment> assignment, Config config, Request request) {
        if (config instanceof OnlineConfig) {
            return ((OnlineConfig) config).getEnrollment();
        } else {
            return config.getEnrollmentWeight(assignment, request);
        }
    }

    /**
     * Section enrollment (using {@link OnlineSection#getEnrollment()} if applicable}, {@link Section#getEnrollmentWeight(Assignment, Request)} otherwise)
     * @param assignment current assignment
     * @param section given section
     * @param request given request
     * @return current enrollment of the section, excluding the request
     */
    protected double getEnrollment(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        if (section instanceof OnlineSection) {
            return ((OnlineSection) section).getEnrollment();
        } else {
            return section.getEnrollmentWeight(assignment, request);
        }
    }

    /**
     * Section limit (using {@link OnlineSection#getEnrollment()} if applicable}, {@link Section#getLimit()} otherwise)
     * @param section given section
     * @return limit of the given section
     */
    protected int getLimit(Section section) {
        if (section.getLimit() < 0)
            return section.getLimit();
        if (section instanceof OnlineSection) {
            return section.getLimit() + ((OnlineSection) section).getEnrollment();
        } else {
            return section.getLimit();
        }
    }

    /**
     * Subpart limit (using {@link OnlineConfig#getEnrollment()} if applicable}, {@link Subpart#getLimit()} otherwise)
     * @param subpart given subpart
     * @return limit of the given subpart
     */
    protected int getLimit(Subpart subpart) {
        int limit = subpart.getLimit();
        if (limit < 0)
            return limit;
        if (subpart.getConfig() instanceof OnlineConfig)
            limit += ((OnlineConfig) subpart.getConfig()).getEnrollment();
        return limit;
    }

    @Override
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        if (section.getLimit() <= 0)
            return 0.0; // ignore unlimited & not available

        double expected = round(getPercentage() * section.getSpaceExpected());
        double enrolled = getEnrollment(assignment, section, request) + request.getWeight();
        double limit = getLimit(section);
        int subparts = section.getSubpart().getConfig().getSubparts().size();

        return expected + enrolled > limit ? 1.0 / subparts : 0.0;
    }

    @Override
    public Integer getExpected(int sectionLimit, double expectedSpace) {
        if (sectionLimit <= 0)
            return null;

        double expected = round(getPercentage() * expectedSpace);
        if (expected > 0.0)
            return (int) Math.floor(expected);

        return null;
    }

    @Override
    public String toString() {
        return "perc(" + getPercentage() + ")";
    }

}
