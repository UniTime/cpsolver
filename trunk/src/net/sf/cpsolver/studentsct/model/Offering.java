package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * Representation of an instructional offering. An offering contains id, name, the list of course offerings, and the list of 
 * possible configurations. See {@link Config} and {@link Course}.
 *  
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
public class Offering {
    private long iId = -1;
    private String iName = null;
    private Vector iConfigs = new Vector();
    private Vector iCourses = new Vector();
    
    /** Constructor
     * @param id instructional offering unique id
     * @param name instructional offering name (this is usually the name of the controlling course)
     */
    public Offering(long id, String name) {
        iId = id; iName = name;
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
    public Vector getConfigs() {
        return iConfigs;
    }

    /** List of courses. One instructional offering can contain multiple courses (names under which it is offered) */
    public Vector getCourses() {
        return iCourses;
    }
    
    /** Return section of the given id, if it is part of one of this offering configurations. */
    public Section getSection(long sectionId) {
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                for (Enumeration g=subpart.getSections().elements();g.hasMoreElements();) {
                    Section section = (Section)g.nextElement();
                    if (section.getId()==sectionId)
                        return section;
                }
            }
        }
        return null;
    }
    
    /** Return course, under which the given student enrolls into this offering. */
    public Course getCourse(Student student) {
        if (getCourses().size()==0)
            return null;
        if (getCourses().size()==1)
            return (Course)getCourses().firstElement();
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request instanceof CourseRequest) {
                for (Enumeration f=((CourseRequest)request).getCourses().elements();f.hasMoreElements();) {
                    Course course = (Course)f.nextElement();
                    if (getCourses().contains(course)) return course;
                }
            }
        }
        return (Course)getCourses().firstElement();
    }
    
    /** Return set of instructional types, union over all configurations. */
    public HashSet getInstructionalTypes() {
        HashSet instructionalTypes = new HashSet();
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                instructionalTypes.add(subpart.getInstructionalType());
            }
        }
        return instructionalTypes;
    }
    
    /** Return the list of all possible choices of the given instructional type for this offering. */
    public HashSet getChoices(String instructionalType) {
        HashSet choices = new HashSet();
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                if (!instructionalType.equals(subpart.getInstructionalType())) continue;
                choices.addAll(subpart.getChoices());
            }
        }
        return choices;
    }
    
    /** Return list of all subparts of the given isntructional type for this offering. */
    public HashSet getSubparts(String instructionalType) {
        HashSet subparts = new HashSet();
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                if (instructionalType.equals(subpart.getInstructionalType())) 
                    subparts.add(subpart);
            }
        }
        return subparts;
    }
}
