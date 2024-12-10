package org.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.BacktrackNeighbourSelection;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.RouletteWheelSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.RequestGroup;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Subpart;

/**
 * Shuffle students along request groups. This selection tries to improve the
 * ability to keep students of the same request group together by moving students
 * of a request group that are spread over multiple sections into a single section
 * or into a fewer number of sections.
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
public class ShuffleStudentsSelection implements NeighbourSelection<Request, Enrollment> {
    private Queue<Shuffle> iQueue = null;
    private ShuffleBacktrackNeighbourSelection iBacktrack;
    
    /**
     * Constructor
     * @param properties the selection has no properties at the moment 
     */
    public ShuffleStudentsSelection(DataProperties properties) {
    }

    @Override
    public void init(Solver<Request, Enrollment> solver) {
        StudentSectioningModel model = (StudentSectioningModel)solver.currentSolution().getModel();
        iQueue = new LinkedList<Shuffle>();
        Assignment<Request, Enrollment> assignment = solver.currentSolution().getAssignment();
        // Check all request groups that have a spread < 1.0 
        RouletteWheelSelection<RequestGroup> groups = new RouletteWheelSelection<RequestGroup>();
        for (Offering offering: model.getOfferings()) {
            if (offering.isDummy()) continue;
            for (Course course: offering.getCourses()) {
                for (RequestGroup group: course.getRequestGroups()) {
                    double spread = group.getAverageSpread(solver.currentSolution().getAssignment()); 
                    if (spread >= 1.0) continue;
                    groups.add(group, 1.0 - spread);
                }
            }
        }
        // If there are some, pick one randomly (using roulette wheel selection)
        if (groups.hasMoreElements()) {
            RequestGroup group = groups.nextElement();
            RouletteWheelSelection<Subpart> subparts = new RouletteWheelSelection<Subpart>();
            for (CourseRequest cr: group.getRequests()) {
                Enrollment e = assignment.getValue(cr);
                if (e != null)
                    for (Section section: e.getSections())
                        if (group.getSectionSpread(assignment, section) < 1.0)
                            subparts.addExisting(section.getSubpart(), 1.0);
            }
            if (subparts.hasMoreElements()) {
                // Pick a subpart that has sections with a section spread < 1.0
                Subpart subpart = subparts.nextElement();
                RouletteWheelSelection<Section> sections = new RouletteWheelSelection<Section>();
                section: for (Section section: subpart.getSections()) {
                    // Only take sections that all requests can use
                    for (CourseRequest cr: group.getRequests()) {
                        boolean match = false;
                        for (Enrollment e: cr.values(assignment))
                            if (e.getSections().contains(section)) { match = true; break; }
                        if (!match) continue section;
                    }
                    // Take sections with conflicts with lower probability
                    int nrConflicts = 0;
                    if (!section.isAllowOverlap())
                        requests: for (CourseRequest cr: group.getRequests()) {
                            for (Request r: cr.getStudent().getRequests()) {
                                if (r.equals(cr)) continue;
                                Enrollment e = assignment.getValue(r);
                                if (e != null && !e.isAllowOverlap() && section.isOverlapping(e.getSections())) {
                                    nrConflicts++; continue requests;
                                }
                            }
                        }
                    sections.add(section, 1 + group.getRequests().size() - nrConflicts);
                }
                Set<Section> filter = new HashSet<Section>(); double space = 0.0;
                // Pick enough sections
                while (sections.hasMoreElements()) {
                    Section section = sections.nextElement();
                    if (filter.add(section)) {
                        if (section.getLimit() < 0) break;
                        space += section.getLimit();
                    }
                    if (space >= group.getTotalWeight()) break;
                }
                // Add all requests that should be moved into the queue
                for (CourseRequest cr: group.getRequests()) {
                    Shuffle shuffle = new Shuffle(group, cr, filter);
                    Enrollment e = assignment.getValue(cr);
                    if (e != null && shuffle.matchFilter(e)) continue;
                    iQueue.add(shuffle);
                }
            } else {
                // No subpart -> no section filter
                for (CourseRequest cr: group.getRequests())
                    iQueue.add(new Shuffle(group, cr, null));
            }
        }
        // Shuffle the queue
        Collections.shuffle((LinkedList<Shuffle>)iQueue);
        // Initialize the backtrack selection, if needed
        if (iBacktrack == null) {
            try {
                iBacktrack = new ShuffleBacktrackNeighbourSelection(solver.getProperties());
                iBacktrack.init(solver);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        // Change progress
        Progress.getInstance(solver.currentSolution().getModel()).setPhase("Shuffling students along request groups...", iQueue.size());
    }

    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        Shuffle shuffle = null;
        while ((shuffle = iQueue.poll()) != null) {
            Progress.getInstance(solution.getModel()).incProgress();
            // Take an item from the queue, try backtrack first
            Neighbour<Request, Enrollment> n = iBacktrack.selectNeighbour(solution, shuffle);
            if (n != null) return n;
            // If fails, just assign the request randomly and hope for the best
            List<Enrollment> adepts = new ArrayList<Enrollment>();
            for (Enrollment e: shuffle.getRequest().values(solution.getAssignment())) {
                if (shuffle.matchFilter(e)) adepts.add(e);
            }
            if (!adepts.isEmpty()) {
                return new SimpleNeighbour<Request, Enrollment>(shuffle.getRequest(), ToolBox.random(adepts));
            }
        }
        return null;
    }

    /**
     * One item to be shuffled
     */
    private static class Shuffle {
        RequestGroup iGroup;
        CourseRequest iRequest;
        Set<Section> iFilter;
        
        /**
         * Constructor
         * @param group selected request group
         * @param request selected request of a request group
         * @param filter section filter (if used, only enrollments that contain one of the given sections can be used)
         */
        Shuffle(RequestGroup group, CourseRequest request, Set<Section> filter) {
            iGroup = group;
            iRequest = request;
            iFilter = filter;
        }
        
        /**
         * Selected request group
         * @return a request group
         */
        public CourseRequest getRequest() { return iRequest; }
        
        /**
         * Is there a section filter set?
         * @return true, if only enrollments with matching sections can be used
         */
        public boolean hasFilter() { return iFilter != null && !iFilter.isEmpty(); }
        
        /**
         * Is the given request matching this object?
         * @param variable a request
         * @return true if the given variable is a course request for a matching course
         */
        public boolean matchRequest(Request variable) {
            return variable instanceof CourseRequest && ((CourseRequest)variable).getCourses().contains(iGroup.getCourse());
        }
        
        /**
         * Is the given enrollment matching the section filter
         * @param e an enrollment
         * @return true if the enrollment contains at least one section of the filter
         */
        public boolean matchFilter(Enrollment e) {
            if (iFilter == null || iFilter.isEmpty()) return true;
            for (Section s: e.getSections())
                if (iFilter.contains(s)) return true;
            return false;
        }
    }
    
    /**
     * A special version of the {@link BacktrackNeighbourSelection} that filters the enrollments with the
     * provided section filter.
     *
     */
    public static class ShuffleBacktrackNeighbourSelection extends BacktrackNeighbourSelection<Request, Enrollment> {

        /**
         * Constructor
         * @param properties configuration
         * @throws Exception thrown when initialization fails
         */
        ShuffleBacktrackNeighbourSelection(DataProperties properties) throws Exception {
            super(properties);
            setTimeout(properties.getPropertyInt("Shuffle.BackTrackTimeout", getTimeout()));
            setDepth(properties.getPropertyInt("Shuffle.BackTrackDepth", getDepth()));
            setMaxIters(properties.getPropertyInt("Shuffle.BackTrackMaxIters", getMaxIters()));
        }
        
        /**
         * List of values of the given variable that will be considered (filtered using {@link ShuffleStudentsSelection.Shuffle#matchFilter(Enrollment)} if applicable).
         * @param context assignment context
         * @param variable given variable
         * @return values of the given variable that will be considered
         */
        @Override
        protected Iterator<Enrollment> values(BacktrackNeighbourSelectionContext context, Request variable) {
            Shuffle shuffle = ((ShuffleBacktrackNeighbourSelectionContext)context).getShuffle();
            if (shuffle.matchRequest(variable) && shuffle.hasFilter()) {
                List<Enrollment> values = new ArrayList<Enrollment>();
                for (Iterator<Enrollment> i = super.values(context, variable); i.hasNext(); ) {
                    Enrollment e = i.next();
                    if (shuffle.matchFilter(e)) values.add(e);
                }
                return values.iterator();
            } else {
                return super.values(context, variable);
            }
        }
        
        protected Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution, Shuffle shuffle) {
            BacktrackNeighbourSelectionContext context = new ShuffleBacktrackNeighbourSelectionContext(solution, shuffle);
            selectNeighbour(solution, shuffle.getRequest(), context);
            return context.getBackTrackNeighbour();
        }
        
        private class ShuffleBacktrackNeighbourSelectionContext extends BacktrackNeighbourSelectionContext {
            private Shuffle iShuffle = null;
            
            private ShuffleBacktrackNeighbourSelectionContext(Solution<Request, Enrollment> solution, Shuffle shuffle) {
                super(solution);
                iShuffle = shuffle; 
            }
            
            private Shuffle getShuffle() { return iShuffle; }
        }
    }

}
