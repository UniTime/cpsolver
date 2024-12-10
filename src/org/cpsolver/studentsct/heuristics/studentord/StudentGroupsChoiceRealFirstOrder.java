package org.cpsolver.studentsct.heuristics.studentord;

import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Return the given set of students in an order of average number of request groups of
 * each student. If two students are involved in the same number of groups, the comparison
 * falls down to the average number of choices of each student (students with less choices first). 
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2015 Tomas Muller<br>
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
public class StudentGroupsChoiceRealFirstOrder extends StudentChoiceOrder {

    public StudentGroupsChoiceRealFirstOrder(DataProperties config) {
        super(config);
    }
    
    public StudentGroupsChoiceRealFirstOrder() {
        super(new DataProperties());
        setReverse(true);
    }
    
    @Override
    public int compare(Student s1, Student s2) {
        if (s1.getPriority() != s2.getPriority()) return (s1.getPriority().ordinal() < s2.getPriority().ordinal() ? -1 : 1);
        if (s1.isDummy()) {
            if (!s2.isDummy())
                return 1;
        } else if (s2.isDummy())
            return -1;
        
        int cmp = -Double.compare(nrGroups(s1), nrGroups(s2));
        if (cmp != 0)
            return cmp;
        return super.compare(s1, s2);
    }
    
    /**
     * Average number of request groups that the student is involved in
     * @return number of groups / number of requests
     */
    public double nrGroups(Student s) {
        double nrGroups = 0.0;
        for (Request r: s.getRequests()) {
            if (r instanceof CourseRequest)
                nrGroups += ((CourseRequest)r).getRequestGroups().size();
        }
        return nrGroups / s.getRequests().size();
    }
}
