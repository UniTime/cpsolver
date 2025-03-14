package org.cpsolver.studentsct.online.selection;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.online.OnlineSectioningModel;

/**
 * Online student sectioning algorithm based on the {@link BranchBoundSelection} of the batch solver. The 
 * selections adds the ability to provide required free times and sections and to prefer certain sections.
 * If a course request has preferred sections, StudentWeights.PreferenceFactor parameter is used
 * to penalize selection of a non-preferred section.
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
public class SuggestionSelection extends BranchBoundSelection implements OnlineSectioningSelection {
    protected Set<FreeTimeRequest> iRequiredFreeTimes;
    protected Hashtable<CourseRequest, Config> iRequiredConfig = null;
    protected Hashtable<CourseRequest, Hashtable<Subpart, Section>> iRequiredSection = null;
    protected Hashtable<CourseRequest, Set<Section>> iPreferredSections = null;
    protected Set<CourseRequest> iRequiredUnassinged = null;
    /** add up to 50% for preferred sections */
    private double iPreferenceFactor = 0.500;
    private double iMaxOverExpected = -1.0;

    public SuggestionSelection(DataProperties properties) {
        super(properties);
        iPreferenceFactor = properties.getPropertyDouble("StudentWeights.PreferenceFactor", iPreferenceFactor);
    }

    @Override
    public void setPreferredSections(Hashtable<CourseRequest, Set<Section>> preferredSections) {
        iPreferredSections = preferredSections;
    }

    @Override
    public void setRequiredSections(Hashtable<CourseRequest, Set<Section>> requiredSections) {
        iRequiredConfig = new Hashtable<CourseRequest, Config>();
        iRequiredSection = new Hashtable<CourseRequest, Hashtable<Subpart, Section>>();
        if (requiredSections != null) {
            for (Map.Entry<CourseRequest, Set<Section>> entry : requiredSections.entrySet()) {
                Hashtable<Subpart, Section> subSection = new Hashtable<Subpart, Section>();
                iRequiredSection.put(entry.getKey(), subSection);
                for (Section section : entry.getValue()) {
                    if (subSection.isEmpty())
                        iRequiredConfig.put(entry.getKey(), section.getSubpart().getConfig());
                    subSection.put(section.getSubpart(), section);
                }
            }
        }
    }

    @Override
    public void setRequiredFreeTimes(Set<FreeTimeRequest> requiredFreeTimes) {
        iRequiredFreeTimes = requiredFreeTimes;
    }

    @Override
    public BranchBoundNeighbour select(Assignment<Request, Enrollment> assignment, Student student) {
        return new Selection(student, assignment).select();
    }

    @Override
    public void setModel(OnlineSectioningModel model) {
        super.setModel(model);
    }

    /**
     * Extension of {@link org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection.Selection} including checking of
     * required free times and sections.
     *
     */
    public class Selection extends BranchBoundSelection.Selection {
        public Selection(Student student, Assignment<Request, Enrollment> assignment) {
            super(student, assignment);
        }

        /**
         * Check if the given enrollment is allowed
         * @param idx enrollment index
         * @param enrollment enrollment
         * @return true if allowed (there is no conflict with required sections or free times)
         */
        public boolean isAllowed(int idx, Enrollment enrollment) {
            if (enrollment.isCourseRequest()) {
                CourseRequest request = (CourseRequest) enrollment.getRequest();
                if (iRequiredUnassinged != null && iRequiredUnassinged.contains(request)) return false;
                Config reqConfig = iRequiredConfig.get(request);
                if (reqConfig != null) {
                    if (!reqConfig.equals(enrollment.getConfig()))
                        return false;
                    Hashtable<Subpart, Section> reqSections = iRequiredSection.get(request);
                    for (Section section : enrollment.getSections()) {
                        Section reqSection = reqSections.get(section.getSubpart());
                        if (reqSection == null)
                            continue;
                        if (!section.equals(reqSection))
                            return false;
                    }
                }
            } else if (iRequiredFreeTimes.contains(enrollment.getRequest())) {
                if (enrollment.getAssignments() == null || enrollment.getAssignments().isEmpty())
                    return false;
            }
            return true;
        }

        @Override
        public boolean inConflict(int idx, Enrollment enrollment) {
            if (iMaxOverExpected >= 0.0 && iModel instanceof OnlineSectioningModel) {
                double penalty = 0.0;
                for (int i = 0; i < idx; i++) {
                    if (iAssignment[i] != null && iAssignment[i].getAssignments() != null && iAssignment[i].isCourseRequest())
                        for (Section section: iAssignment[i].getSections())
                            penalty += ((OnlineSectioningModel)iModel).getOverExpected(iCurrentAssignment, iAssignment, i, section, iAssignment[i].getRequest());
                }
                if (enrollment.isCourseRequest())
                    for (Section section: enrollment.getSections())
                        penalty += ((OnlineSectioningModel)iModel).getOverExpected(iCurrentAssignment, iAssignment, idx, section, enrollment.getRequest());
                if (penalty > iMaxOverExpected) return true;
            }
            return super.inConflict(idx, enrollment) || !isAllowed(idx, enrollment);
        }

        @Override
        public Enrollment firstConflict(int idx, Enrollment enrollment) {
            Enrollment conflict = super.firstConflict(idx, enrollment);
            if (conflict != null)
                return conflict;
            return (isAllowed(idx, enrollment) ? null : enrollment);
        }

        @Override
        protected boolean canLeaveUnassigned(Request request) {
            if (request instanceof CourseRequest) {
                if (iRequiredConfig.get(request) != null)
                    return false;
            } else if (iRequiredFreeTimes.contains(request))
                return false;
            return true;
        }

        @Override
        protected List<Enrollment> values(final CourseRequest request) {
            if (iRequiredUnassinged != null && iRequiredUnassinged.contains(request))
                return new ArrayList<Enrollment>();
            return super.values(request);
        }

        @Override
        @Deprecated
        protected double getWeight(Enrollment enrollment, Set<DistanceConflict.Conflict> distanceConflicts,
                Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts) {
            double weight = super.getWeight(enrollment, distanceConflicts, timeOverlappingConflicts);
            if (enrollment.isCourseRequest() && iPreferredSections != null) {
                Set<Section> preferred = iPreferredSections.get(enrollment.getRequest());
                if (preferred != null && !preferred.isEmpty()) {
                    double nrPreferred = 0;
                    for (Section section : enrollment.getSections())
                        if (preferred.contains(section))
                            nrPreferred++;
                    double preferredFraction = nrPreferred / preferred.size();
                    weight *= 1.0 + iPreferenceFactor * preferredFraction;
                }
            }
            return weight;
        }

        @Override
        protected double getBound(Request r) {
            double bound = super.getBound(r);
            if (r instanceof CourseRequest) {
                Set<Section> preferred = iPreferredSections.get(r);
                if (preferred != null && !preferred.isEmpty())
                    bound *= (1.0 + iPreferenceFactor);
            }
            return bound;
        }

    }

    @Override
    public void setRequiredUnassinged(Set<CourseRequest> requiredUnassignedRequests) {
        iRequiredUnassinged = requiredUnassignedRequests;
    }
    
    @Override
    public void setMaxOverExpected(double maxOverExpected) {
        iMaxOverExpected = maxOverExpected;
    }
}