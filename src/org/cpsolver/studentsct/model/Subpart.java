package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.studentsct.reservation.Reservation;


/**
 * Representation of a scheduling subpart. Each scheduling subpart contains id,
 * instructional type, name, instructional offering configuration, and a list of
 * sections. Optionally, parent-child relation between subparts can be defined. <br>
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
public class Subpart implements Comparable<Subpart> {
    private long iId = -1;
    private String iInstructionalType = null;
    private String iName = null;
    private List<Section> iSections = new ArrayList<Section>();
    private Config iConfig = null;
    private Subpart iParent = null;
    private boolean iAllowOverlap = false;
    private String iCredit = null;
    private Float iCreditValue = null;

    /**
     * Constructor
     * 
     * @param id
     *            scheduling subpart unique id
     * @param itype
     *            instructional type
     * @param name
     *            subpart name
     * @param config
     *            instructional offering configuration to which this subpart
     *            belongs
     * @param parent
     *            parent subpart, if parent-child relation is defined between
     *            subparts
     */
    public Subpart(long id, String itype, String name, Config config, Subpart parent) {
        iId = id;
        iInstructionalType = itype;
        iName = name;
        iConfig = config;
        iParent = parent;
        iConfig.getSubparts().add(this);
    }

    /** Subpart id 
     * @return scheduling subpart unique id
     **/
    public long getId() {
        return iId;
    }

    /** Instructional type, e.g., Lecture, Recitation or Laboratory 
     * @return instructional type
     **/
    public String getInstructionalType() {
        return iInstructionalType;
    }

    /** Subpart name 
     * @return scheduling subpart name
     **/
    public String getName() {
        return iName;
    }

    /** Instructional offering configuration to which this subpart belongs 
     * @return instructional offering configuration
     **/
    public Config getConfig() {
        return iConfig;
    }

    /** List of sections 
     * @return classes of this scheduling supart
     **/
    public List<Section> getSections() {
        return iSections;
    }

    /** Parent subpart, if parent-child relation is defined between subparts 
     * @return parent scheduling subpart
     **/
    public Subpart getParent() {
        return iParent;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * True, if this subpart is parent (or parent of a parent etc.) of the given
     * subpart
     * @param subpart parent scheduling subpart
     * @return true if parent (even indirect)
     */
    public boolean isParentOf(Subpart subpart) {
        if (subpart.getParent() == null)
            return false;
        if (subpart.getParent().equals(this))
            return true;
        return isParentOf(subpart.getParent());
    }

    /**
     * Compare two subparts: put parents first, use ids if there is no
     * parent-child relation
     */
    @Override
    public int compareTo(Subpart s) {
        if (isParentOf(s))
            return -1;
        if (s.isParentOf(this))
            return 1;
        int cmp = getInstructionalType().compareTo(s.getInstructionalType());
        if (cmp != 0)
            return cmp;
        return Double.compare(getId(), s.getId());
    }

    /** List of available choices of the sections of this subpart. 
     * @return set of available choices
     **/
    public Set<Choice> getChoices() {
        Set<Choice> choices = new HashSet<Choice>();
        for (Section section : getSections()) {
            choices.add(new Choice(section));
        }
        return choices;
    }

    /** Minimal penalty from {@link Section#getPenalty()} 
     * @return minimal penalty
     **/
    public double getMinPenalty() {
        double min = Double.MAX_VALUE;
        for (Section section : getSections()) {
            min = Math.min(min, section.getPenalty());
        }
        return (min == Double.MAX_VALUE ? 0.0 : min);
    }

    /** Maximal penalty from {@link Section#getPenalty()} 
     * @return maximal penalty
     **/
    public double getMaxPenalty() {
        double max = Double.MIN_VALUE;
        for (Section section : getSections()) {
            max = Math.max(max, section.getPenalty());
        }
        return (max == Double.MIN_VALUE ? 0.0 : max);
    }

    /** Return children subparts 
     * @return children scheduling subparts
     **/
    public List<Subpart> getChildren() {
        List<Subpart> ret = new ArrayList<Subpart>(getConfig().getSubparts().size());
        for (Subpart s : getConfig().getSubparts()) {
            if (this.equals(s.getParent()))
                ret.add(s);
        }
        return ret;
    }
    
    /** Return true if overlaps are allowed, but the number of overlapping slots should be minimized. 
     * @return true if overlaps of classes of this scheduling subpart are allowed
     **/
    public boolean isAllowOverlap() {
        return iAllowOverlap;
    }
    
    /** Set to true if overlaps are allowed, but the number of overlapping slots should be minimized (defaults to false). 
     * @param allowOverlap are overlaps of classes of this scheduling subpart allowed
     **/
    public void setAllowOverlap(boolean allowOverlap) {
        iAllowOverlap = allowOverlap;
    }
    
    /**
     * Get reservations that require sections of this subpart
     * @return reservations that require a class of this scheduling subpart
     */
    public synchronized List<Reservation> getSectionReservations() {
        if (iSectionReservations == null) {
            iSectionReservations = new ArrayList<Reservation>();
            for (Reservation r: getConfig().getOffering().getReservations()) {
                if (r.getSections(this) != null)
                    iSectionReservations.add(r);
            }
        }
        return iSectionReservations;
    }
    private List<Reservation> iSectionReservations = null;
    
    /**
     * Clear reservation information that was cached on this subpart or below
     */
    public synchronized void clearReservationCache() {
        for (Section s: getSections())
            s.clearReservationCache();
        iSectionReservations = null;
    }
    
    /**
     * Sum of the section limits (unlimited, if one or more sections are unlimited)
     * @return total class limit
     */
    public int getLimit() {
        if (iLimit == null)
            iLimit = getLimitNoCache();
        return iLimit;
    }
    private int getLimitNoCache() {
        int limit = 0;
        for (Section section: getSections()) {
            if (section.getLimit() < 0) return -1;
            limit += section.getLimit();
        }
        return limit;
    }
    private Integer iLimit = null; 
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Subpart)) return false;
        return getId() == ((Subpart)o).getId();
    }
    
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32));
    }
    
    /**
     * Set credit (Online Student Scheduling only)
     * @param credit scheduling subpart credit
     */
    public void setCredit(String credit) {
        iCredit = credit;
        if (iCreditValue == null && credit != null) {
            int split = credit.indexOf('|');
            String abbv = null;
            if (split >= 0) {
                abbv = credit.substring(0, split);
            } else {
                abbv = credit;
            }
            Matcher m = Pattern.compile("(^| )(\\d+\\.?\\d*)([,-]?(\\d+\\.?\\d*))?($| )").matcher(abbv);
            if (m.find())
                iCreditValue = Float.parseFloat(m.group(2));
        }
    }
    
    /**
     * Get credit (Online Student Scheduling only)
     * @return scheduling subpart credit
     */
    public String getCredit() { return iCredit; }

    /**
     * True if this subpart has a credit value defined
     * @return true if a credit value is set
     */
    public boolean hasCreditValue() { return iCreditValue != null; }
    
    /**
     * Set subpart's credit value (null if not set)
     * @param creditValue subpart's credit value
     */
    public void setCreditValue(Float creditValue) { iCreditValue = creditValue; }
    
    /**
     * Get subpart's credit value (null if not set)
     * return subpart's credit value
     */
    public Float getCreditValue() { return iCreditValue; }
    
    public boolean isOnline() {
        for (Section section: getSections())
            if (!section.isOnline()) return false;
        return true;
    }
    
    public boolean hasTime() {
        for (Section section: getSections())
            if (section.hasTime()) return true;
        return false;
    }
    
    public boolean isPast() {
        for (Section section: getSections())
            if (!section.isPast()) return false;
        return true;
    }
}
