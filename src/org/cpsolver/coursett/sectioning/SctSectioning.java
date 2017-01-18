package org.cpsolver.coursett.sectioning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.DefaultStudentSectioning;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.coursett.sectioning.SctModel.SctSolution;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.solution.Solution;

/**
 * 
 * Student sectioning implementation based on branch & bound. This sectioning takes
 * each offering one by one and it is using a branch & bound algorithm to find
 * the best possible enrollment of all students into the given course. The sectioning
 * considers both student conflict weights and student groups.
 * 
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2017 Tomas Muller<br>
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
public class SctSectioning extends DefaultStudentSectioning implements InfoProvider<Lecture, Placement> {
    private boolean iUseCriteria = true;
    private int iNrRounds = 3;
    private List<StudentConflict> iStudentConflictCriteria = null;
    private static java.text.DecimalFormat sDF2 = new java.text.DecimalFormat("0.00", new java.text.DecimalFormatSymbols(Locale.US));

    public SctSectioning(TimetableModel model) {
        super(model);
        iUseCriteria = model.getProperties().getPropertyBoolean("SctSectioning.UseCriteria", true);
        iNrRounds = model.getProperties().getPropertyInt("SctSectioning.NrRounds", 3);
    }
    
    @Override
    public boolean hasFinalSectioning() {
        return true;
    }
    
    /**
     * List of student conflict criteria
     */
    protected List<StudentConflict> getStudentConflictCriteria() {
        if (!iUseCriteria) return null;
        if (iStudentConflictCriteria == null && iModel != null) {
            iStudentConflictCriteria = new ArrayList<StudentConflict>();
            for (Criterion<Lecture, Placement> criterion: iModel.getCriteria())
                if (criterion instanceof StudentConflict)
                    iStudentConflictCriteria.add((StudentConflict)criterion);
        }
        return iStudentConflictCriteria;
    }
    
    /**
     * Student conflict weight for the given solution
     */
    protected double value(Solution<Lecture, Placement> solution) {
        List<StudentConflict> criteria = getStudentConflictCriteria();
        
        if (criteria == null) {
            double value = 0.0;
            for (JenrlConstraint constraint: ((TimetableModel)solution.getModel()).getJenrlConstraints()) {
                if (constraint.isInConflict(solution.getAssignment())) value += constraint.jenrl();
            }
            return value;
        }
        
        double value = 0.0;
        for (StudentConflict criterion: criteria)
            value += criterion.getWeightedValue(solution.getAssignment());
        return value;
    }
    
    @Override
    public void switchStudents(Solution<Lecture, Placement> solution) {
        getProgress().setStatus("Student Sectioning...");
        getProgress().info("Student Conflicts: " + sDF2.format(value(solution)) + " (group: " + sDF2.format(StudentSwapSectioning.gp(solution)) + "%)");

        for (int i = 1; i <= iNrRounds; i++) {
            getProgress().setPhase("Swapping students [" + i + "]...", iModel.variables().size());
            Set<Long> offeringIds = new HashSet<Long>();
            for (Lecture lecture: iModel.variables()) {
                getProgress().incProgress();
                if (lecture.students().isEmpty() || lecture.isSingleSection()) continue;
                if (offeringIds.add(lecture.getConfiguration().getOfferingId())) {
                    SctModel model = new SctModel(iModel, solution.getAssignment());
                    model.setConfiguration(lecture.getConfiguration());
                    SctSolution s1 = model.currentSolution();
                    SctSolution s2 = model.computeSolution();
                    if (model.isTimeOutReached())
                        getProgress().info("Timeout reached for " + lecture.getName());
                    if (s2.isBetter(s1)) {
                        model.unassign();
                        model.assign(s2);
                        getProgress().info("Student Conflicts: " + sDF2.format(value(solution)) + " (group: " + sDF2.format(StudentSwapSectioning.gp(solution)) + "%)");
                    }
                }
            }
            getProgress().info("Student Conflicts: " + sDF2.format(value(solution)) + " (group: " + sDF2.format(StudentSwapSectioning.gp(solution)) + "%)");
        }
    }

    @Override
    public void resection(Assignment<Lecture, Placement> assignment, Lecture lecture, boolean recursive, boolean configAsWell) {
        SctModel model = new SctModel(iModel, assignment);
        model.setConfiguration(lecture.getConfiguration());
        SctSolution s1 = model.currentSolution();
        SctSolution s2 = model.computeSolution();
        if (s2.isBetter(s1)) {
            model.unassign();
            model.assign(s2);
        }
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        if (!iModel.getStudentGroups().isEmpty())
            info.put("Student groups", sDF2.format(100.0 * StudentSwapSectioning.group(iModel) / iModel.getStudentGroups().size()) + "%");
    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        if (!iModel.getStudentGroups().isEmpty())
            info.put("Student groups", sDF2.format(StudentSwapSectioning.gp(iModel, variables)) + "%");
    }
}