package net.sf.cpsolver.exam.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;

/**
 * A student. <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class ExamStudent extends Constraint<Exam, ExamPlacement> {
    private Set<Exam>[] iTable;
    private Set<Exam>[] iDayTable;
    private boolean iAllowDirectConflicts = true;
    private List<ExamOwner> iOwners = new ArrayList<ExamOwner>();
    private boolean[] iAvailable = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     * @param id
     *            student unique id
     */
    @SuppressWarnings("unchecked")
    public ExamStudent(ExamModel model, long id) {
        super();
        iAllowDirectConflicts = model.getProperties().getPropertyBoolean("Student.AllowDirectConflicts",
                iAllowDirectConflicts);
        iAssignedVariables = null;
        iId = id;
        iTable = new Set[model.getNrPeriods()];
        for (int i = 0; i < iTable.length; i++)
            iTable[i] = new HashSet<Exam>();
        iDayTable = new Set[model.getNrDays()];
        for (int i = 0; i < iDayTable.length; i++)
            iDayTable[i] = new HashSet<Exam>();
    }

    /**
     * True if direct student conflicts are allowed for this student
     */
    public boolean isAllowDirectConflicts() {
        return iAllowDirectConflicts;
    }

    /**
     * Set whether direct student conflicts are allowed for this student
     */
    public void setAllowDirectConflicts(boolean allowDirectConflicts) {
        iAllowDirectConflicts = allowDirectConflicts;
    }

    /**
     * True if the given two exams can have a direct student conflict with this
     * student, i.e., they can be placed at the same period.
     * 
     * @param ex1
     *            an exam
     * @param ex2
     *            an exam
     * @return {@link ExamStudent#isAllowDirectConflicts()} and
     *         {@link Exam#isAllowDirectConflicts()} for both exams
     */
    public boolean canConflict(Exam ex1, Exam ex2) {
        return isAllowDirectConflicts() && ex1.isAllowDirectConflicts() && ex2.isAllowDirectConflicts();
    }

    /**
     * Exam(s) enrolled by the student that are scheduled in the given period
     */
    public Set<Exam> getExams(ExamPeriod period) {
        return iTable[period.getIndex()];
    }

    /**
     * Exam(s) enrolled by the student that are scheduled in the given day
     */
    public Set<Exam> getExamsADay(ExamPeriod period) {
        return iDayTable[period.getDay()];
    }

    /**
     * Exam(s) enrolled by the student that are scheduled in the given day
     */
    public Set<Exam> getExamsADay(int day) {
        return iDayTable[day];
    }

    /**
     * Compute conflicts between the given assignment of an exam and all the
     * current assignments (of this student). Only not-allowed conflicts (see
     * {@link ExamStudent#canConflict(Exam, Exam)} are considered.
     */
    @Override
    public void computeConflicts(ExamPlacement p, Set<ExamPlacement> conflicts) {
        Exam ex = p.variable();
        for (Exam exam : iTable[p.getPeriod().getIndex()]) {
            if (!canConflict(ex, exam))
                conflicts.add(exam.getAssignment());
        }
    }

    /**
     * Check whether there is a conflict between the given assignment of an exam
     * and all the current assignments (of this student). Only not-allowed
     * conflicts (see {@link ExamStudent#canConflict(Exam, Exam)} are
     * considered.
     */
    @Override
    public boolean inConflict(ExamPlacement p) {
        Exam ex = p.variable();
        for (Exam exam : iTable[p.getPeriod().getIndex()]) {
            if (!canConflict(ex, exam))
                return true;
        }
        return false;
    }

    /**
     * True if the given exams can conflict (see
     * {@link ExamStudent#canConflict(Exam, Exam)}), or if they are placed at
     * different periods.
     */
    @Override
    public boolean isConsistent(ExamPlacement p1, ExamPlacement p2) {
        return (p1.getPeriod() != p2.getPeriod() || canConflict(p1.variable(), p2.variable()));
    }

    /**
     * An exam was assigned, update student assignment table
     */
    public void afterAssigned(long iteration, ExamPlacement value) {
        ExamPlacement p = value;
        iTable[p.getPeriod().getIndex()].add(value.variable());
        iDayTable[p.getPeriod().getDay()].add(value.variable());
    }

    /**
     * An exam was unassigned, update student assignment table
     */
    public void afterUnassigned(long iteration, ExamPlacement value) {
        ExamPlacement p = value;
        iTable[p.getPeriod().getIndex()].remove(value.variable());
        iDayTable[p.getPeriod().getDay()].remove(value.variable());
    }

    /**
     * Compare two student for equality
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ExamStudent))
            return false;
        ExamStudent s = (ExamStudent) o;
        return getId() == s.getId();
    }

    /**
     * Hash code
     */
    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    /**
     * Student unique id
     */
    @Override
    public String toString() {
        return String.valueOf(getId());
    }

    /**
     * Compare two students (by student ids)
     */
    @Override
    public int compareTo(Constraint<Exam, ExamPlacement> o) {
        return toString().compareTo(o.toString());
    }

    /**
     * Constraint is hard if {@link ExamStudent#isAllowDirectConflicts()} is
     * false.
     */
    @Override
    public boolean isHard() {
        return !isAllowDirectConflicts();
    }

    /**
     * Courses and/or sections that this student is enrolled to
     * 
     * @return list of {@link ExamOwner}
     */
    public List<ExamOwner> getOwners() {
        return iOwners;
    }

    /**
     * True if the student is available (for examination timetabling) during the
     * given period
     * 
     * @param period
     *            a period
     * @return true if a student can attend an exam at the given period, false
     *         if otherwise
     */
    public boolean isAvailable(ExamPeriod period) {
        return (iAvailable == null ? true : iAvailable[period.getIndex()]);
    }

    /**
     * Set whether the student is available (for examination timetabling) during
     * the given period
     * 
     * @param period
     *            a period
     * @param available
     *            true if a student can attend an exam at the given period,
     *            false if otherwise
     */
    public void setAvailable(int period, boolean available) {
        if (iAvailable == null) {
            iAvailable = new boolean[iTable.length];
            for (int i = 0; i < iTable.length; i++)
                iAvailable[i] = true;
        }
        iAvailable[period] = available;
    }

}
