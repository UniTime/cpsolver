package org.cpsolver.studentsct.online;

import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Offering;

/**
 * An online configuration. A simple extension of the {@link Config} class that allows to set the current configuration enrollment.
 * This class is particularly useful when a model containing only the given student is constructed (to provide him/her with a schedule or suggestions).
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class OnlineConfig extends Config {
    private int iEnrollment = 0;
    
    public OnlineConfig(long id, int limit, String name, Offering offering) {
            super(id, limit, name, offering);
    }
    
    /**
     * Set current enrollment
     * @param enrollment current enrollment
     */
    public void setEnrollment(int enrollment) { iEnrollment = enrollment; }
    
    /**
     * Get current enrollment
     * @return current enrollment
     */
    public int getEnrollment() { return iEnrollment; }
}