package net.sf.cpsolver.studentsct.check;

import java.text.DecimalFormat;
import java.util.Enumeration;

import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * This class looks and reports cases when a section limit is exceeded.  
 *  
 * <br><br>
 * 
 * Usage: if (new SectionLimitCheck(model).check()) ...
 * 
 * <br><br>
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
public class SectionLimitCheck {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(SectionLimitCheck.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private StudentSectioningModel iModel;
    
    /** Constructor
     * @param model student sectioning model
     */
    public SectionLimitCheck(StudentSectioningModel model) {
        iModel = model;
    }
    
    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
    }
    
    /** Check for sections that have more students enrolled than it is allowed, i.e.,
     * the sum of requests weights is above the section limit
     * @return false, if there is such a case
     */
    public boolean check() {
        sLog.info("Checking section limits...");
        boolean ret = true;
        for (Enumeration e=getModel().getOfferings().elements();e.hasMoreElements();) {
            Offering offering = (Offering)e.nextElement();
            for (Enumeration f=offering.getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                for (Enumeration g=config.getSubparts().elements();g.hasMoreElements();) {
                    Subpart subpart = (Subpart)g.nextElement();
                    for (Enumeration h=subpart.getSections().elements();h.hasMoreElements();) {
                        Section section = (Section)h.nextElement();
                        if (section.getLimit()<0) continue;
                        double used = section.getEnrollmentWeight(null);
                        if (used-section.getMaxEnrollmentWeight()>section.getLimit()) {
                            sLog.error("Section "+section.getName()+" exceeds its limit "+sDF.format(used)+">"+section.getLimit()+" for more than one student (W:"+section.getMaxEnrollmentWeight()+")");
                            ret = false;
                        } else if (Math.round(used)>section.getLimit()) {
                            sLog.debug("Section "+section.getName()+" exceeds its limit "+sDF.format(used)+">"+section.getLimit()+" for less than one student (W:"+section.getMaxEnrollmentWeight()+")");
                        }
                    }
                }
            }
        }
        return ret;
    }
    

}
