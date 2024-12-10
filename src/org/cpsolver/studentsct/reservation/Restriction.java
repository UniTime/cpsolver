package org.cpsolver.studentsct.reservation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;



/**
 * Abstract restriction. Restrictions are like reservations, that must be used
 * and that do not reserve any space. Except, there can be more than one restriction
 * on an offering and the student must meet at least one that applies to her/him. 
 * 
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2020 Tomas Muller<br>
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
public abstract class Restriction {
    /** Restriction unique id */
    private long iId = 0;
    
    /** Instructional offering on which the restriction is set, required */
    private Offering iOffering;

    /** One or more configurations, if applicable */ 
    private Set<Config> iConfigs = new HashSet<Config>();
    
    /** One or more sections, if applicable */
    private Map<Subpart, Set<Section>> iSections = new HashMap<Subpart, Set<Section>>();
    
    /**
     * Constructor
     * @param id restriction unique id
     * @param offering instructional offering on which the restriction is set
     */
    public Restriction(long id, Offering offering) {
        iId = id;
        iOffering = offering;
        iOffering.getRestrictions().add(this);
        iOffering.clearRestrictionCache();
    }
    
    /**
     * Restriction  id
     * @return restriction unique id
     */
    public long getId() { return iId; }
    
    /**
     * Returns true if the student is applicable for the restriction
     * @param student a student 
     * @return true if student can use the restriction to get into the course / configuration / section
     */
    public abstract boolean isApplicable(Student student);

    /**
     * Instructional offering on which the restriction is set.
     * @return instructional offering
     */
    public Offering getOffering() { return iOffering; }
    
    /**
     * One or more configurations on which the restriction is set.
     * @return instructional offering configurations
     */
    public Set<Config> getConfigs() { return iConfigs; }
    
    /**
     * Add a configuration (of the offering {@link Restriction#getOffering()}) to this restriction
     * @param config instructional offering configuration
     */
    public void addConfig(Config config) {
        iConfigs.add(config);
    }
    
    /**
     * One or more sections on which the restriction is set.
     * @return class class restrictions
     */
    public Map<Subpart, Set<Section>> getSections() { return iSections; }
    
    /**
     * One or more sections on which the restriction is set (optional).
     * @param subpart scheduling subpart
     * @return class restrictions for the given scheduling subpart
     */
    public Set<Section> getSections(Subpart subpart) {
        return iSections.get(subpart);
    }
    
    /**
     * Add a section (of the offering {@link Restriction#getOffering()}) to this restriction.
     * This will also add all parent sections and the appropriate configuration to the offering.
     * @param section a class restriction
     */
    public void addSection(Section section) {
        addConfig(section.getSubpart().getConfig());
        while (section != null) {
            Set<Section> sections = iSections.get(section.getSubpart());
            if (sections == null) {
                sections = new HashSet<Section>();
                iSections.put(section.getSubpart(), sections);
            }
            sections.add(section);
            section = section.getParent();
        }
    }
    
    /**
     * Return true if the given enrollment meets the restriction.
     * @param enrollment given enrollment
     * @return true if the given enrollment meets the restriction
     */
    public boolean isIncluded(Enrollment enrollment) {
        // Free time request are never included
        if (enrollment.getConfig() == null) return false;
        
        // Check the offering
        if (!iOffering.equals(enrollment.getConfig().getOffering())) return false;
        
        // no restrictions -> not included
        if (iConfigs.isEmpty() && iSections.isEmpty()) return false;
        
        // If there are configurations, check the configuration
        if (!iConfigs.isEmpty() && !iConfigs.contains(enrollment.getConfig())) return false;
        
        // Check all the sections of the enrollment
        for (Section section: enrollment.getSections()) {
            Set<Section> sections = iSections.get(section.getSubpart());
            if (sections != null && !sections.contains(section))
                return false;
        }
        return true;
    }
    
    public boolean isIncluded(Config config) {
        // Check the offering
        if (!iOffering.equals(config.getOffering())) return false;
        
        // no restrictions -> not included
        if (iConfigs.isEmpty() && iSections.isEmpty()) return false;
        
        // if there are configurations, check the configuration
        if (!iConfigs.isEmpty() && !iConfigs.contains(config)) return false;
        return true;
    }
    
    public boolean isIncluded(Section section) {
        // Check the offering
        if (!iOffering.equals(section.getSubpart().getConfig().getOffering())) return false;
        
        // no restrictions -> not included
        if (iConfigs.isEmpty() && iSections.isEmpty()) return false;
        
        // if there are configurations, check the configuration
        if (!iConfigs.isEmpty() && !iConfigs.contains(section.getSubpart().getConfig())) return false;
        
        // check the given section
        Set<Section> sections = iSections.get(section.getSubpart());
        if (sections != null && !sections.contains(section))
            return false;
        
        return true;
    }
}
