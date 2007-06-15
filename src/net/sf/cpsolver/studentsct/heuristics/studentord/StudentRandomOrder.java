package net.sf.cpsolver.studentsct.heuristics.studentord;

import java.util.Collections;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.DataProperties;

/** 
 * Return the given set of students in a random order 
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
public class StudentRandomOrder implements StudentOrder {
    
    public StudentRandomOrder(DataProperties config) {}

    /** Return the given set of students in a random order */
    public Vector order(Vector students) {
        Vector ret = new Vector(students);
        Collections.shuffle(ret);
        return ret;
    }

}
