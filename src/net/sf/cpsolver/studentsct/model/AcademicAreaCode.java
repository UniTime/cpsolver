package net.sf.cpsolver.studentsct.model;

import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Academic area and code. This class is used for
 * {@link Student#getAcademicAreaClasiffications()}, {@link Student#getMajors()}
 * , and {@link Student#getMinors()}. <br>
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
public class AcademicAreaCode {
    private String iArea, iCode;

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

    /** Academic area */
    public String getArea() {
        return iArea;
    }

    /** Code */
    public String getCode() {
        return iCode;
    }

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
