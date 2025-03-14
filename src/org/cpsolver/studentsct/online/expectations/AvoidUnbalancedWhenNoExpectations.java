package org.cpsolver.studentsct.online.expectations;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Subpart;

/**
 * Avoid unbalanced sections when there are no expectations. When there are
 * expectations, the {@link PercentageOverExpected} is used. If there are no
 * expectations, sections that would have more students than given by the balance
 * are marked as over-expected. The target fill ratio is proportional to the section
 * size, a section is considered unbalanced, when the target fill is exceeded by
 * more than OverExpected.Disbalance percentage (defaults to 0.1). Unlimited
 * sections are also balanced, when General.BalanceUnlimited parameter is set
 * to true (defaults to false).<br><br>
 * A class of an offering with no expectations is over-expected when the number
 * of enrolled students (including the student in question) minus the target
 * fill is over the OverExpected.Disbalance portion of the section limit.
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
public class AvoidUnbalancedWhenNoExpectations extends PercentageOverExpected {
    private Double iDisbalance = 0.1;
    private boolean iBalanceUnlimited = false;

    public AvoidUnbalancedWhenNoExpectations(DataProperties config) {
        super(config);
        iDisbalance = config.getPropertyDouble("OverExpected.Disbalance", iDisbalance);
        iBalanceUnlimited = config.getPropertyBoolean("General.BalanceUnlimited", iBalanceUnlimited);
    }

    public AvoidUnbalancedWhenNoExpectations(Double percentage, Double disbalance) {
        super(percentage);
        iDisbalance = disbalance;
    }

    public AvoidUnbalancedWhenNoExpectations(Double percentage) {
        this(percentage, null);
    }

    public AvoidUnbalancedWhenNoExpectations() {
        this(null, null);
    }

    /**
     * Return allowed disbalance, defaults to 0.1 (parameter OverExpected.Disbalance)
     * @return allowed disbalance
     */
    public Double getDisbalance() {
        return iDisbalance;
    }

    /**
     * Is balancing of unlimited sections enabled (parameter General.BalanceUnlimited)
     * @return true if balancing of unlimited sections is enabled
     */
    public boolean isBalanceUnlimited() {
        return iBalanceUnlimited;
    }

    @Override
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        Subpart subpart = section.getSubpart();

        if (hasExpectations(subpart) && section.getLimit() > 0)
            return super.getOverExpected(assignment, section, request);

        if (getDisbalance() == null || getDisbalance() < 0.0)
            return 0.0;

        double enrlConfig = request.getWeight() + getEnrollment(assignment, subpart.getConfig(), request);
        int subparts = section.getSubpart().getConfig().getSubparts().size();
        int limit = getLimit(section);
        double enrl = request.getWeight() + getEnrollment(assignment, section, request);

        if (limit > 0) {
            // sections have limits -> desired size is section limit x (total
            // enrollment / total limit)
            double desired = (enrlConfig / getLimit(subpart)) * limit;
            if (enrl - desired >= Math.max(1.0, getDisbalance() * limit))
                return 1.0 / subparts;
        } else if (isBalanceUnlimited()) {
            // unlimited sections -> desired size is total enrollment / number
            // of sections
            double desired = enrlConfig / subpart.getSections().size();
            if (enrl - desired >= Math.max(1.0, getDisbalance() * desired))
                return 1.0 / subparts;
        }

        return 0.0;
    }

    @Override
    public String toString() {
        return "bal(" + getPercentage() + "," + getDisbalance() + ")";
    }

}
