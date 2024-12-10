package org.cpsolver.studentsct.online;

import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.constraint.FixInitialAssignments;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.online.expectations.AvoidUnbalancedWhenNoExpectations;
import org.cpsolver.studentsct.online.expectations.OverExpectedCriterion;
import org.cpsolver.studentsct.online.expectations.PercentageOverExpected;

/**
 * An online model. A simple extension of the {@link OnlineSectioningModel} class that allows to set the over-expected
 * criterion (see {@link OverExpectedCriterion}). This class is particularly useful in passing the over-expected criterion to the 
 * online sectioning algorithms and heuristics.<br><br>
 * The over-expected criterion can be passed as a constructor parameter, or given using the OverExpectedCriterion.Class parameter.
 * It defaults to {@link AvoidUnbalancedWhenNoExpectations}.
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class OnlineSectioningModel extends StudentSectioningModel {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(OnlineSectioningModel.class);
    private OverExpectedCriterion iOverExpectedCriterion;

    public OnlineSectioningModel(DataProperties properties) {
        this(properties, null);
        try {
            @SuppressWarnings("unchecked")
            Class<OverExpectedCriterion> overExpectedCriterionClass = (Class<OverExpectedCriterion>)
                Class.forName(properties.getProperty("OverExpectedCriterion.Class", AvoidUnbalancedWhenNoExpectations.class.getName()));
            iOverExpectedCriterion = overExpectedCriterionClass.getConstructor(DataProperties.class).newInstance(properties);
        } catch (Exception e) {
                sLog.error("Unable to create custom over-expected criterion (" + e.getMessage() + "), using default.", e);
                iOverExpectedCriterion = new PercentageOverExpected(properties);
        }
    }

    public OnlineSectioningModel(DataProperties config, OverExpectedCriterion criterion) {
        super(config);
        iOverExpectedCriterion = criterion;
        if (isMPP() && super.getKeepInitialAssignments()) {
            for (GlobalConstraint<Request, Enrollment> c: globalConstraints()) {
                if (c instanceof FixInitialAssignments) {
                    removeGlobalConstraint(c); break;
                }
            }
        }
    }
    
    @Override
    public boolean getKeepInitialAssignments() {
        return false;
    }
    
    /**
     * Get over-expected criterion
     * @return over-expected criterion
     */
    public OverExpectedCriterion getOverExpectedCriterion() { return iOverExpectedCriterion; }
    
    /**
     * Set over-expected criterion
     * @param overExpectedCriterion over-expected criterion
     */
    public void setOverExpectedCriterion(OverExpectedCriterion overExpectedCriterion) { iOverExpectedCriterion = overExpectedCriterion; }
    
    /**
     * Expectation penalty, to be minimized (computed using {@link OverExpectedCriterion#getOverExpected(Assignment, Section, Request)})
     * @param assignment current assignment
     * @param section section in question
     * @param request student course request
     * @return expectation penalty (typically 1.0 / number of subparts when over-expected, 0.0 otherwise)
     */
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        return getOverExpectedCriterion().getOverExpected(assignment, section, request);
    }
    
    /**
     * Expectation penalty, to be minimized
     * @param assignment current assignment
     * @param enrollment current enrollment of the student
     * @param index only use enrollments 0 .. index - 1 from the assignment array
     * @param section section in question
     * @param request student course request
     * @return expectation penalty (typically 1.0 / number of subparts when over-expected, 0.0 otherwise)
     */
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Enrollment[] enrollment, int index, Section section, Request request) {
        if (getOverExpectedCriterion() instanceof OverExpectedCriterion.HasContext)
            return ((OverExpectedCriterion.HasContext)getOverExpectedCriterion()).getOverExpected(assignment, enrollment, index, section, request);
        else
            return getOverExpectedCriterion().getOverExpected(assignment, section, request);
    }
    
    /**
     * Expectation penalty, to be minimized.
     * A variant of the {@link OverExpectedCriterion#getOverExpected(Assignment, Section, Request)} method that can be called from {@link Constraint#computeConflicts(Assignment, Value, Set)}.
     * @param assignment current assignment
     * @param selection selected enrollment question
     * @param value an enrollment to be assigned
     * @param conflicts enrollments that have been already identified as conflicting
     * @return expectation penalty (typically 1.0 / number of subparts when over-expected, 0.0 otherwise)
     */
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Enrollment selection, Enrollment value, Set<Enrollment> conflicts) {
        if (getOverExpectedCriterion() instanceof OverExpectedCriterion.HasContext)
            return ((OverExpectedCriterion.HasContext)getOverExpectedCriterion()).getOverExpected(assignment, selection, value, conflicts);
        else {
            if (selection == null || !selection.isCourseRequest()) return 0.0;
            double penalty = 0.0;
            for (Section section: selection.getSections())
                penalty += getOverExpectedCriterion().getOverExpected(assignment, section, selection.getRequest());
            return penalty;
        }
    }

}