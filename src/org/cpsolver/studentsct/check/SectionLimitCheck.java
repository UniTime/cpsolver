package org.cpsolver.studentsct.check;

import java.text.DecimalFormat;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Subpart;


/**
 * This class looks and reports cases when a section limit is exceeded.
 * 
 * <br>
 * <br>
 * 
 * Usage: if (new SectionLimitCheck(model).check()) ...
 * 
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class SectionLimitCheck {
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(SectionLimitCheck.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private StudentSectioningModel iModel;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public SectionLimitCheck(StudentSectioningModel model) {
        iModel = model;
    }

    /** Return student sectioning model 
     * @return problem model
     **/
    public StudentSectioningModel getModel() {
        return iModel;
    }

    /**
     * Check for sections that have more students enrolled than it is allowed,
     * i.e., the sum of requests weights is above the section limit
     * @param assignment current assignment
     * @return false, if there is such a case
     */
    public boolean check(Assignment<Request, Enrollment> assignment) {
        sLog.info("Checking section limits...");
        boolean ret = true;
        for (Offering offering : getModel().getOfferings()) {
            if (offering.isDummy()) continue;
            for (Config config : offering.getConfigs()) {
                for (Subpart subpart : config.getSubparts()) {
                    for (Section section : subpart.getSections()) {
                        if (section.getLimit() < 0)
                            continue;
                        double used = section.getEnrollmentWeight(assignment, null);
                        if (used - section.getMaxEnrollmentWeight(assignment) > section.getLimit()) {
                            sLog.error("Section " + section.getName() + " exceeds its limit " + sDF.format(used) + ">"
                                    + section.getLimit() + " for more than one student (W:"
                                    + section.getMaxEnrollmentWeight(assignment) + ")");
                            ret = false;
                        } else if (Math.round(used) > section.getLimit()) {
                            sLog.debug("Section " + section.getName() + " exceeds its limit " + sDF.format(used) + ">"
                                    + section.getLimit() + " for less than one student (W:"
                                    + section.getMaxEnrollmentWeight(assignment) + ")");
                        }
                    }
                }
            }
        }
        return ret;
    }

}
