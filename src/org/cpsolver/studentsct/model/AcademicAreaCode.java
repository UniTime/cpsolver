package org.cpsolver.studentsct.model;

import org.cpsolver.ifs.util.ToolBox;

/**
 * Academic area and code. This class is used for
 * {@link Student#getAcademicAreaClasiffications()}, {@link Student#getMajors()}
 * , and {@link Student#getMinors()}. <br>
 * <br>
 * 
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
public class AcademicAreaCode {
    private String iArea, iCode, iLabel;

    /**
     * Constructor
     * 
     * @param area
     *            academic area
     * @param code
     *            code
     */
    public AcademicAreaCode(String area, String code) {
        iArea = area;
        iCode = code;
    }
    
    public AcademicAreaCode(String area, String code, String label) {
        iArea = area;
        iCode = code;
        iLabel = label;
    }

    /** Academic area 
     * @return academic area abbreviation
     **/
    public String getArea() {
        return iArea;
    }

    /** Code 
     * @return classification code
     **/
    public String getCode() {
        return iCode;
    }
    
    public String getLabel() { return iLabel; }

    @Override
    public int hashCode() {
        return (iArea + ":" + iCode).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AcademicAreaCode))
            return false;
        AcademicAreaCode aac = (AcademicAreaCode) o;
        return ToolBox.equals(aac.getArea(), getArea()) && ToolBox.equals(aac.getCode(), getCode());
    }

    @Override
    public String toString() {
        return getArea() + ":" + getCode();
    }
}
