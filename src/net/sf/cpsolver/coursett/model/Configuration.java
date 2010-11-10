package net.sf.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * Configuration. Each course can have multiple configurations. A student needs
 * to be enrolled into classes of one of the configurations.
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class Configuration {
    private Long iConfigId = null;
    private Long iOfferingId = null;
    private Hashtable<Long, Set<Lecture>> iTopLectures = new Hashtable<Long, Set<Lecture>>();
    private List<Configuration> iAltConfigurations = null;
    private int iLimit = -1;

    public Configuration(Long offeringId, Long configId, int limit) {
        iOfferingId = offeringId;
        iConfigId = configId;
        iLimit = limit;
    }

    public Long getOfferingId() {
        return iOfferingId;
    }

    public Long getConfigId() {
        return iConfigId;
    }

    public void addTopLecture(Lecture lecture) {
        Set<Lecture> lectures = iTopLectures.get(lecture.getSchedulingSubpartId());
        if (lectures == null) {
            lectures = new HashSet<Lecture>();
            iTopLectures.put(lecture.getSchedulingSubpartId(), lectures);
        }
        lectures.add(lecture);
    }
    
    public Hashtable<Long, Set<Lecture>> getTopLectures() {
        return iTopLectures;
    }

    public Enumeration<Long> getTopSubpartIds() {
        return iTopLectures.keys();
    }

    public Set<Lecture> getTopLectures(Long subpartId) {
        return iTopLectures.get(subpartId);
    }

    public void setAltConfigurations(List<Configuration> altConfigurations) {
        iAltConfigurations = altConfigurations;
    }

    public void addAltConfiguration(Configuration configuration) {
        if (iAltConfigurations == null)
            iAltConfigurations = new ArrayList<Configuration>();
        iAltConfigurations.add(configuration);
    }

    public List<Configuration> getAltConfigurations() {
        return iAltConfigurations;
    }

    public Set<Student> students() {
        Set<Student> students = new HashSet<Student>();
        for (Enumeration<Set<Lecture>> e = iTopLectures.elements(); e.hasMoreElements();) {
            Set<Lecture> lectures = e.nextElement();
            for (Lecture l : lectures) {
                students.addAll(l.students());
            }
        }
        return students;
    }

    public boolean hasConflict(Student student) {
        for (Lecture lecture : student.getLectures()) {
            if (lecture.getAssignment() == null || !this.equals(lecture.getConfiguration()))
                continue;
            if (student.countConflictPlacements(lecture.getAssignment()) > 0)
                return true;
            for (Lecture x : student.getLectures()) {
                if (x.getAssignment() == null || x.equals(lecture))
                    continue;
                if (lecture.jenrlConstraint(x).isInConflict())
                    return true;
            }
        }
        return false;
    }

    public int getLimit() {
        if (iLimit < 0) {
            double totalWeight = 0.0;
            for (Student s : students()) {
                totalWeight += s.getOfferingWeight(getOfferingId());
            }
            iLimit = (int) Math.round(totalWeight);
        }
        return iLimit;
    }

    @Override
    public int hashCode() {
        return getConfigId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Configuration))
            return false;
        return getConfigId().equals(((Configuration) o).getConfigId());
    }
}