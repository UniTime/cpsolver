package org.cpsolver.studentsct.model;

import org.cpsolver.ifs.util.ToolBox;

/**
 * Student group type, reference and name. This class is used for
 * {@link Student#getGroups()}. <br>
 * <br>
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
public class StudentGroup {
    private String iType, iReference, iName;

    /**
     * Constructor
     * 
     * @param type group type (can be empty)
     * @param reference group reference
     * @param name group name
     */
    public StudentGroup(String type, String reference, String name) {
        iType = type;
        iReference = reference;
        iName = name;
    }

    /** Group type
     * @return group type reference
     **/
    public String getType() {
        return iType;
    }

    /** Group reference 
     * @return group reference
     **/
    public String getReference() {
        return iReference;
    }
    
    /** Group name 
     * @return group name
     **/
    public String getName() {
        return iName;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof StudentGroup))
            return false;
        StudentGroup acm = (StudentGroup) o;
        return ToolBox.equals(acm.getType(), getType()) && ToolBox.equals(acm.getReference(), getReference()) && ToolBox.equals(acm.getName(), getName());
    }

    @Override
    public String toString() {
        return getReference() + (getType() == null || getType().isEmpty() ? "" : " (" + getType() + ")");
    }
}
