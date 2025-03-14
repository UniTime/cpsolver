package org.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.ifs.assignment.Assignment;


/**
 * Configuration. Each course can have multiple configurations. A student needs
 * to be enrolled into classes of one of the configurations.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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

public class Configuration {
    private Long iConfigId = null;
    private Long iOfferingId = null;
    private HashMap<Long, Set<Lecture>> iTopLectures = new HashMap<Long, Set<Lecture>>();
    private List<Configuration> iAltConfigurations = null;
    private int iLimit = -1;
    private Set<Long> iSubpartIds = null;

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
        iSubpartIds = null;
    }
    
    public Map<Long, Set<Lecture>> getTopLectures() {
        return iTopLectures;
    }

    public Set<Long> getTopSubpartIds() {
        return iTopLectures.keySet();
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
        for (Set<Lecture> lectures: iTopLectures.values()) {
            for (Lecture l : lectures) {
                students.addAll(l.students());
            }
        }
        return students;
    }

    public boolean hasConflict(Assignment<Lecture, Placement> assignment, Student student) {
        for (Lecture lecture : student.getLectures()) {
            Placement placement = assignment.getValue(lecture);
            if (placement == null || !this.equals(lecture.getConfiguration()))
                continue;
            if (student.countConflictPlacements(placement) > 0)
                return true;
            for (Lecture x : student.getLectures()) {
                if (assignment.getValue(x) == null || x.equals(lecture))
                    continue;
                JenrlConstraint jenrl = lecture.jenrlConstraint(x);
                if (jenrl != null && jenrl.isInConflict(assignment))
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
    
    public Set<Long> getSubpartIds() {
        if (iSubpartIds == null) {
            Set<Long> subparts = new HashSet<Long>();
            Queue<Lecture> queue = new LinkedList<Lecture>();
            for (Map.Entry<Long, Set<Lecture>> e: getTopLectures().entrySet()) {
                subparts.add(e.getKey());
                queue.addAll(e.getValue());
            }
            Lecture lecture = null;
            while ((lecture = queue.poll()) != null) {
                if (lecture.getChildren() != null)
                    for (Map.Entry<Long, List<Lecture>> e: lecture.getChildren().entrySet()) {
                        subparts.add(e.getKey());
                        queue.addAll(e.getValue());
                    }
            }
            iSubpartIds = subparts;
        }
        return iSubpartIds;
    }
    
    public int countSubparts() {
        return getSubpartIds().size();
    }
}