package net.sf.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representation of a scheduling subpart. Each scheduling subpart contains id,
 * instructional type, name, instructional offering configuration, and a list of
 * sections. Optionally, parent-child relation between subparts can be defined. <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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

    /** Subpart id */
    public long getId() {
        return iId;
    }

    /** Instructional type, e.g., Lecture, Recitation or Laboratory */
    public String getInstructionalType() {
        return iInstructionalType;
    }

    /** Subpart name */
    public String getName() {
        return iName;
    }

    /** Instructional offering configuration to which this subpart belongs */
    public Config getConfig() {
        return iConfig;
    }

    /** List of sections */
    public List<Section> getSections() {
        return iSections;
    }

    /** Parent subpart, if parent-child relation is defined between subparts */
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

    /** List of available choices of the sections of this subpart. */
    public Set<Choice> getChoices() {
        Set<Choice> choices = new HashSet<Choice>();
        for (Section section : getSections()) {
            choices.add(section.getChoice());
        }
        return choices;
    }

    /** Minimal penalty from {@link Section#getPenalty()} */
    public double getMinPenalty() {
        double min = Double.MAX_VALUE;
        for (Section section : getSections()) {
            min = Math.min(min, section.getPenalty());
        }
        return (min == Double.MAX_VALUE ? 0.0 : min);
    }

    /** Maximal penalty from {@link Section#getPenalty()} */
    public double getMaxPenalty() {
        double max = Double.MIN_VALUE;
        for (Section section : getSections()) {
            max = Math.max(max, section.getPenalty());
        }
        return (max == Double.MIN_VALUE ? 0.0 : max);
    }

    /** Return children subparts */
    public List<Subpart> getChildren() {
        List<Subpart> ret = new ArrayList<Subpart>(getConfig().getSubparts().size());
        for (Subpart s : getConfig().getSubparts()) {
            if (this.equals(s.getParent()))
                ret.add(s);
        }
        return ret;
    }
    
    /** Return true if overlaps are allowed, but the number of overlapping slots should be minimized. */
    public boolean isAllowOverlap() {
        return iAllowOverlap;
    }
    
    /** Set to true if overlaps are allowed, but the number of overlapping slots should be minimized (defaults to false). */
    public void setAllowOverlap(boolean allowOverlap) {
        iAllowOverlap = allowOverlap;
    }
}
