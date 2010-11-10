package net.sf.cpsolver.studentsct.filter;

import java.util.HashSet;

import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This student filter accepts every student with the given probability. The
 * choice for each student is remembered, i.e., if the student is passed to the
 * filter multiple times the same answer is returned.
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
public class RandomStudentFilter implements StudentFilter {
    private double iProb = 1.0;
    private HashSet<Long> iAcceptedStudentIds = new HashSet<Long>();
    private HashSet<Long> iRejectedStudentIds = new HashSet<Long>();

    /**
     * Constructor
     * 
     * @param prob
     *            probability of acceptance of a student
     */
    public RandomStudentFilter(double prob) {
        iProb = prob;
    }

    /**
     * A student is accepted with the given probability
     */
    public boolean accept(Student student) {
        Long studentId = new Long(student.getId());
        if (iAcceptedStudentIds.contains(studentId))
            return true;
        if (iRejectedStudentIds.contains(studentId))
            return false;
        boolean accept = (Math.random() < iProb);
        if (accept)
            iAcceptedStudentIds.add(studentId);
        else
            iRejectedStudentIds.add(studentId);
        return accept;
    }

    /**
     * Set acceptance probability. Update the sets of accepted and rejected
     * students accordingly.
     * 
     * @param prob
     *            new acceptance probability
     */
    public void setProbability(double prob) {
        iProb = prob;
        int accept = (int) Math.round(prob * (iAcceptedStudentIds.size() + iRejectedStudentIds.size()));
        while (iAcceptedStudentIds.size() < accept && !iRejectedStudentIds.isEmpty()) {
            Long studentId = ToolBox.random(iRejectedStudentIds);
            iRejectedStudentIds.remove(studentId);
            iAcceptedStudentIds.add(studentId);
        }
        while (iAcceptedStudentIds.size() > accept && !iAcceptedStudentIds.isEmpty()) {
            Long studentId = ToolBox.random(iAcceptedStudentIds);
            iRejectedStudentIds.add(studentId);
            iAcceptedStudentIds.remove(studentId);
        }
    }

}
