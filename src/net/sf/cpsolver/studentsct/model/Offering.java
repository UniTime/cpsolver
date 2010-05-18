package net.sf.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representation of an instructional offering. An offering contains id, name,
 * the list of course offerings, and the list of possible configurations. See
 * {@link Config} and {@link Course}.
 * 
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class Offering {
    private long iId = -1;
    private String iName = null;
    private List<Config> iConfigs = new ArrayList<Config>();
    private List<Course> iCourses = new ArrayList<Course>();

    /**
     * Constructor
     * 
     * @param id
     *            instructional offering unique id
     * @param name
     *            instructional offering name (this is usually the name of the
     *            controlling course)
     */
    public Offering(long id, String name) {
        iId = id;
        iName = name;
    }

    /** Offering id */
    public long getId() {
        return iId;
    }

    /** Offering name */
    public String getName() {
        return iName;
    }

    /** Possible configurations */
    public List<Config> getConfigs() {
        return iConfigs;
    }

    /**
     * List of courses. One instructional offering can contain multiple courses
     * (names under which it is offered)
     */
    public List<Course> getCourses() {
        return iCourses;
    }

    /**
     * Return section of the given id, if it is part of one of this offering
     * configurations.
     */
    public Section getSection(long sectionId) {
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                for (Section section : subpart.getSections()) {
                    if (section.getId() == sectionId)
                        return section;
                }
            }
        }
        return null;
    }

    /** Return course, under which the given student enrolls into this offering. */
    public Course getCourse(Student student) {
        if (getCourses().isEmpty())
            return null;
        if (getCourses().size() == 1)
            return getCourses().get(0);
        for (Request request : student.getRequests()) {
            if (request instanceof CourseRequest) {
                for (Course course : ((CourseRequest) request).getCourses()) {
                    if (getCourses().contains(course))
                        return course;
                }
            }
        }
        return getCourses().get(0);
    }

    /** Return set of instructional types, union over all configurations. */
    public Set<String> getInstructionalTypes() {
        Set<String> instructionalTypes = new HashSet<String>();
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                instructionalTypes.add(subpart.getInstructionalType());
            }
        }
        return instructionalTypes;
    }

    /**
     * Return the list of all possible choices of the given instructional type
     * for this offering.
     */
    public Set<Choice> getChoices(String instructionalType) {
        Set<Choice> choices = new HashSet<Choice>();
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                if (!instructionalType.equals(subpart.getInstructionalType()))
                    continue;
                choices.addAll(subpart.getChoices());
            }
        }
        return choices;
    }

    /**
     * Return list of all subparts of the given isntructional type for this
     * offering.
     */
    public Set<Subpart> getSubparts(String instructionalType) {
        Set<Subpart> subparts = new HashSet<Subpart>();
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                if (instructionalType.equals(subpart.getInstructionalType()))
                    subparts.add(subpart);
            }
        }
        return subparts;
    }

    /** Minimal penalty from {@link Config#getMinPenalty()} */
    public double getMinPenalty() {
        double min = Double.MAX_VALUE;
        for (Config config : getConfigs()) {
            min = Math.min(min, config.getMinPenalty());
        }
        return (min == Double.MAX_VALUE ? 0.0 : min);
    }

    /** Maximal penalty from {@link Config#getMaxPenalty()} */
    public double getMaxPenalty() {
        double max = Double.MIN_VALUE;
        for (Config config : getConfigs()) {
            max = Math.max(max, config.getMaxPenalty());
        }
        return (max == Double.MIN_VALUE ? 0.0 : max);
    }

    @Override
    public String toString() {
        return iName;
    }

}
