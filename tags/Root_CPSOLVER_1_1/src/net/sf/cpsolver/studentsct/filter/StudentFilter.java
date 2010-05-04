package net.sf.cpsolver.studentsct.filter;

import net.sf.cpsolver.studentsct.StudentSectioningXMLLoader;
import net.sf.cpsolver.studentsct.Test;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Interface for filter students based on academic area classifications, majors, or minors.
 * This interface can be used by {@link StudentSectioningXMLLoader} to load only subset
 * of all students, and it is used by {@link Test} to combine last-like and real students from two
 * XML files.  
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

public interface StudentFilter {
    /** Accept student */
    public boolean accept(Student student);
}
