package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an instructor. Each instructor contains id, and a name. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2016 Tomas Muller<br>
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
public class Instructor {
    private long iId;
    private String iExternalId = null, iName = null;
    private String iEmail;
    
    /**
     * Constructor
     * @param id instructor unique id
     */
    public Instructor(long id) {
        this(id, null, null, null);
    }
    
    /**
     * Constructor
     * @param id instructor unique id
     * @param externalId instructor external id
     * @param name instructor name
     * @param email instructor email
     */
    public Instructor(long id, String externalId, String name, String email) {
        iId = id; iName = name; iExternalId = externalId; iEmail = email;
    }
    
    /** Instructor unique id 
     * @return instructor unique id
     **/
    public long getId() {
        return iId;
    }

    /** Set instructor unique id 
     * @param id instructor unique id
     **/
    public void setId(long id) {
        iId = id;
    }
    
    @Override
    public String toString() {
        return getName() == null ? "I" + getId() : getName();
    }
    
    /**
     * Compare two instructors for equality. Two instructors are considered equal if
     * they have the same id.
     */
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Instructor))
            return false;
        return getId() == ((Instructor) object).getId();
    }

    /**
     * Hash code (based only on instructor id)
     */
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32));
    }
    
    /**
     * Get instructor external id
     * @return instructor external unique id
     */
    public String getExternalId() { return iExternalId; }

    /**
     * Set instructor external id
     * @param externalId instructor external id
     */
    public void setExternalId(String externalId) { iExternalId = externalId; }

    /**
     * Get instructor name
     * @return instructor name
     */
    public String getName() { return iName; }

    /**
     * Set instructor name
     * @param name instructor name
     */
    public void setName(String name) { iName = name; }
    
    /**
     * Get instructor email
     * @return instructor email
     */
    public String getEmail() { return iEmail; }

    /**
     * Set instructor email
     * @param email instructor email
     */
    public void setEmail(String email) { iEmail = email; }
    
    @Deprecated
    public static List<Instructor> toInstructors(String instructorIds, String instructorNames) {
        if (instructorIds == null || instructorIds.isEmpty()) return null;
        String[] names = (instructorNames == null ? new String[] {}: instructorNames.split(":"));
        List<Instructor> instructors = new ArrayList<Instructor>();
        for (String id: instructorIds.split(":")) {
            Instructor instructor = new Instructor(Long.parseLong(id));
            if (instructors.size() < names.length) {
                String name = names[instructors.size()];
                if (name.indexOf('|') >= 0) {
                    instructor.setName(name.substring(0, name.indexOf('|')));
                    instructor.setEmail(name.substring(name.indexOf('|') + 1));
                } else {
                    instructor.setName(name);
                }
            }
            instructors.add(instructor);
        }
        return instructors;
    }
}
