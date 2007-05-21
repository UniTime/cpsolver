package net.sf.cpsolver.studentsct.check;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;

import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Subpart;

public class SectionLimitCheck {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(SectionLimitCheck.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private StudentSectioningModel iModel;
    
    public SectionLimitCheck(StudentSectioningModel model) {
        iModel = model;
    }
    
    public StudentSectioningModel getModel() {
        return iModel;
    }
    
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
                        double maxWeight = 0;
                        for (Iterator i=section.getEnrollments().iterator();i.hasNext();) {
                            Enrollment enrollment = (Enrollment)i.next();
                            maxWeight = Math.max(maxWeight, enrollment.getRequest().getWeight());
                        }
                        if (used-maxWeight>section.getLimit()) {
                            sLog.error("Section "+section.getName()+" exceeds its limit "+sDF.format(used)+">"+section.getLimit()+" for more than one student (W:"+maxWeight+")");
                            ret = false;
                        } else if (Math.round(used)>section.getLimit()) {
                            sLog.debug("Section "+section.getName()+" exceeds its limit "+sDF.format(used)+">"+section.getLimit()+" for less than one student (W:"+maxWeight+")");
                        }
                    }
                }
            }
        }
        return ret;
    }
    

}
