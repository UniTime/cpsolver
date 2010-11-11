package net.sf.cpsolver.studentsct.heuristics.studentord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * Return the given set of students in an order of average number of choices of
 * each student (students with more choices first).
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class StudentChoiceOrder implements StudentOrder, Comparator<Student> {
    private boolean iReverse = false;
    private HashMap<Config, Integer> iCache = new HashMap<Config, Integer>();

    public StudentChoiceOrder(DataProperties config) {
        iReverse = config.getPropertyBoolean("StudentChoiceOrder.Reverse", iReverse);
    }

    /** Is order reversed */
    public boolean isReverse() {
        return iReverse;
    }

    /** Set reverse order */
    public void setReverse(boolean reverse) {
        iReverse = reverse;
    }

    /** Order the given list of students */
    public List<Student> order(List<Student> students) {
        List<Student> ret = new ArrayList<Student>(students);
        Collections.sort(ret, this);
        return ret;
    }

    public int compare(Student s1, Student s2) {
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

    /** Average number of choices for each student */
    public double avgNrChoices(Student student) {
        int nrRequests = 0;
        int nrChoices = 0;
        for (Request request : student.getRequests()) {
            if (request instanceof CourseRequest) {
                CourseRequest cr = (CourseRequest) request;
                for (Course course : cr.getCourses()) {
                    for (Config config : course.getOffering().getConfigs()) {
                        Integer nrChoicesThisCfg = iCache.get(config);
                        if (nrChoicesThisCfg == null) {
                            nrChoicesThisCfg = new Integer(nrChoices(config, 0, new HashSet<Section>()));
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
