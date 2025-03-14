package org.cpsolver.studentsct.heuristics.studentord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;

import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;


/**
 * Return the given set of students in an order of average number of choices of
 * each student (students with more choices first).
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
public class StudentChoiceOrder implements StudentOrder, Comparator<Student> {
    private boolean iReverse = false;
    private boolean iFast = true;
    private boolean iCriticalOnly = false;
    private RequestPriority iPriority = RequestPriority.Critical;
    private HashMap<Config, Integer> iCache = new HashMap<Config, Integer>();

    public StudentChoiceOrder(DataProperties config) {
        iReverse = config.getPropertyBoolean("StudentChoiceOrder.Reverse", iReverse);
        iFast = config.getPropertyBoolean("StudentChoiceOrder.Fast", iFast);
        iCriticalOnly = config.getPropertyBoolean("StudentChoiceOrder.CriticalOnly", iCriticalOnly);
    }

    /** Is order reversed 
     * @return true if the order is reversed
     **/
    public boolean isReverse() {
        return iReverse;
    }

    /** Set reverse order 
     * @param reverse true if students are to be in a reversed order
     **/
    public void setReverse(boolean reverse) {
        iReverse = reverse;
    }
    
    public boolean isCriticalOnly() { return iCriticalOnly; }
    public void setCriticalOnly(boolean critOnly) { iCriticalOnly = critOnly; }
    public void setRequestPriority(RequestPriority priority) { iPriority = priority; }
    public RequestPriority getRequestPriority() { return iPriority; }

    /** Order the given list of students */
    @Override
    public List<Student> order(List<Student> students) {
        List<Student> ret = new ArrayList<Student>(students);
        Collections.sort(ret, this);
        return ret;
    }

    @Override
    public int compare(Student s1, Student s2) {
        if (s1.getPriority() != s2.getPriority()) return (s1.getPriority().ordinal() < s2.getPriority().ordinal() ? -1 : 1);
        try {
            int cmp = -Double.compare(avgNrChoices(s1), avgNrChoices(s2));
            if (cmp != 0)
                return (iReverse ? -1 : 1) * cmp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (iReverse ? -1 : 1) * Double.compare(s1.getId(), s2.getId());
    }

    private int nrChoices(Config config, int idx, HashSet<Section> sections) {
        if (iFast) {
            int nrChoices = 1;
            for (Subpart subpart: config.getSubparts()) {
                if (subpart.getChildren().isEmpty())
                    nrChoices *= subpart.getSections().size();
            }
            return nrChoices;
        }
        if (config.getSubparts().size() == idx) {
            return 1;
        } else {
            Subpart subpart = config.getSubparts().get(idx);
            int choicesThisSubpart = 0;
            for (Section section : subpart.getSections()) {
                if (section.getParent() != null && !sections.contains(section.getParent()))
                    continue;
                if (section.isOverlapping(sections))
                    continue;
                sections.add(section);
                choicesThisSubpart += nrChoices(config, idx + 1, sections);
                sections.remove(section);
            }
            return choicesThisSubpart;
        }
    }
    
    /** Average number of choices for each student 
     * @param student given student
     * @return average number of choices of the given student
     **/
    public double avgNrChoices(Student student) {
        int nrRequests = 0;
        int nrChoices = 0;
        for (Request request : student.getRequests()) {
            if (request instanceof CourseRequest) {
                CourseRequest cr = (CourseRequest) request;
                if (iCriticalOnly && (!iPriority.isCritical(cr) || cr.isAlternative())) continue;
                for (Course course : cr.getCourses()) {
                    for (Config config : course.getOffering().getConfigs()) {
                        Integer nrChoicesThisCfg = iCache.get(config);
                        if (nrChoicesThisCfg == null) {
                            nrChoicesThisCfg = Integer.valueOf(nrChoices(config, 0, new HashSet<Section>()));
                            iCache.put(config, nrChoicesThisCfg);
                        }
                        nrChoices += nrChoicesThisCfg.intValue();
                    }
                }
                nrRequests++;
            }
        }
        return (nrRequests == 0 ? 0.0 : ((double) nrChoices) / nrRequests);
    }
}
