package org.cpsolver.studentsct.heuristics.studentord;

import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Return the given set of students in an order of average course request
 * time stamp {@link CourseRequest#getTimeStamp()}. If the time stamp
 * is the same (or not set), fall back to the number of choices
 * (student with fewer choices first).
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2019 Tomas Muller<br>
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
public class StudentByRequestDate extends StudentChoiceOrder {
    
    public StudentByRequestDate(DataProperties config) {
        super(config);
    }
    
    public Long getRequestDate(Student s) {
        long total = 0, cnt = 0;
        for (Request r: s.getRequests()) {
            if (r instanceof CourseRequest) {
                CourseRequest cr = (CourseRequest)r;
                if (cr.getTimeStamp() != null) {
                    total += cr.getTimeStamp();
                    cnt ++;
                }
            }
        }
        if (cnt > 0)
            return total / cnt;
        return Long.MAX_VALUE;
    }
    
    @Override
    public int compare(Student s1, Student s2) {
        int cmp = getRequestDate(s1).compareTo(getRequestDate(s2));
        if (cmp != 0) return (isReverse() ? -1 : 1) * cmp;
        return - super.compare(s1, s2);
    }
}
