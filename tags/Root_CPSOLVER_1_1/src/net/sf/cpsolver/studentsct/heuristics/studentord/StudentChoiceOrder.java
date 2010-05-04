package net.sf.cpsolver.studentsct.heuristics.studentord;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

/** 
 * Return the given set of students in an order of average number 
 * of choices of each student (students with more choices first).
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
public class StudentChoiceOrder implements StudentOrder, Comparator {
    private boolean iReverse = false;
    private Hashtable iCache = new Hashtable();
    
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
    public Vector order(Vector students) {
        Vector ret = new Vector(students);
        Collections.sort(ret, this);
        return ret;
    }
    
    public int compare(Object o1, Object o2) {
        Student s1 = (Student)o1;
        Student s2 = (Student)o2;
        try {
            int cmp = -Double.compare(avgNrChoices(s1),avgNrChoices(s2));
            if (cmp!=0) return (iReverse?-1:1)*cmp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (iReverse?-1:1)*Double.compare(s1.getId(), s2.getId());
    }
    
    private int nrChoices(Config config, int idx, HashSet sections) {
        if (config.getSubparts().size()==idx) {
            return 1;
        } else {
            Subpart subpart = (Subpart)config.getSubparts().elementAt(idx);
            int choicesThisSubpart = 0;
            for (Enumeration e=subpart.getSections().elements();e.hasMoreElements();) {
                Section section = (Section)e.nextElement();
                if (section.getParent()!=null && !sections.contains(section.getParent())) continue;
                if (section.isOverlapping(sections)) continue;
                sections.add(section);
                choicesThisSubpart += nrChoices(config, idx+1, sections);
                sections.remove(section);
            }
            return choicesThisSubpart;
        }
    }   
    
    /** Average number of choices for each student */
    public double avgNrChoices(Student student) {
        int nrRequests = 0;
        int nrChoices = 0;
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request instanceof CourseRequest) {
                CourseRequest cr = (CourseRequest)request;
                for (Enumeration f=cr.getCourses().elements();f.hasMoreElements();) {
                    Course course = (Course)f.nextElement();
                    for (Enumeration g=course.getOffering().getConfigs().elements();g.hasMoreElements();) {
                        Config config = (Config)g.nextElement();
                        Integer nrChoicesThisCfg = (Integer)iCache.get(config);
                        if (nrChoicesThisCfg==null) {
                            nrChoicesThisCfg = new Integer(nrChoices(config, 0, new HashSet()));
                            iCache.put(config, nrChoicesThisCfg);
                        }
                        nrChoices += nrChoicesThisCfg.intValue();
                    }
                }
                nrRequests ++;
            }
        }
        return (nrRequests==0?0.0:((double)nrChoices)/nrRequests);
    }
}
