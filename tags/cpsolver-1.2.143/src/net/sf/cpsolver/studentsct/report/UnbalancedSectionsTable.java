package net.sf.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * This class lists all unbalanced sections. Each line includes the class, its meeting time,
 * number of enrolled students, desired section size, and the limit. The Target column show
 * the ideal number of students the section (if all the sections were filled equally) and the
 * Disbalance shows the % between the target and the current enrollment.
 * 
 * <br>
 * <br>
 * 
 * Usage: new UnbalancedSectionsTable(model),createTable(true, true).save(aFile);
 * 
 * <br>
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
public class UnbalancedSectionsTable implements StudentSectioningReport {
    private static DecimalFormat sDF1 = new DecimalFormat("0.####");
    private static DecimalFormat sDF2 = new DecimalFormat("0.0000");

    private StudentSectioningModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public UnbalancedSectionsTable(StudentSectioningModel model) {
        iModel = model;
    }

    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
    }

    /**
     * Create report
     * 
     * @param includeLastLikeStudents
     *            true, if last-like students should be included (i.e.,
     *            {@link Student#isDummy()} is true)
     * @param includeRealStudents
     *            true, if real students should be included (i.e.,
     *            {@link Student#isDummy()} is false)
     * @return report as comma separated text file
     */
    public CSVFile createTable(boolean includeLastLikeStudents, boolean includeRealStudents) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("Course"), new CSVFile.CSVField("Class"),
                new CSVFile.CSVField("Meeting Time"), new CSVFile.CSVField("Enrollment"),
                new CSVFile.CSVField("Target"), new CSVFile.CSVField("Limit"), new CSVFile.CSVField("Disbalance [%]") });
        
        TreeSet<Offering> offerings = new TreeSet<Offering>(new Comparator<Offering>() {
            @Override
            public int compare(Offering o1, Offering o2) {
                int cmp = o1.getName().compareToIgnoreCase(o2.getName());
                if (cmp != 0) return cmp;
                return o1.getId() < o2.getId() ? -1 : o2.getId() == o2.getId() ? 0 : 1;
            }
        });
        offerings.addAll(getModel().getOfferings());
        
        Offering last = null;
        for (Offering offering: offerings) {
            for (Config config: offering.getConfigs()) {
                double configEnrl = 0;
                for (Enrollment e: config.getEnrollments()) {
                    if (e.getStudent().isDummy() && !includeLastLikeStudents) continue;
                    if (!e.getStudent().isDummy() && !includeRealStudents) continue;
                    configEnrl += e.getRequest().getWeight();
                }
                config.getEnrollmentWeight(null);
                for (Subpart subpart: config.getSubparts()) {
                    if (subpart.getSections().size() <= 1) continue;
                    if (subpart.getLimit() > 0) {
                        // sections have limits -> desired size is section limit x (total enrollment / total limit)
                        double ratio = configEnrl / subpart.getLimit();
                        for (Section section: subpart.getSections()) {
                            double enrl = 0.0;
                            for (Enrollment e: section.getEnrollments()) {
                                if (e.getStudent().isDummy() && !includeLastLikeStudents) continue;
                                if (!e.getStudent().isDummy() && !includeRealStudents) continue;
                                enrl += e.getRequest().getWeight();
                            }
                            double desired = ratio * section.getLimit();
                            if (Math.abs(desired - enrl) >= Math.max(1.0, 0.1 * section.getLimit())) {
                                if (last != null && !offering.equals(last)) csv.addLine();
                                csv.addLine(new CSVFile.CSVField[] {
                                        new CSVFile.CSVField(offering.equals(last) ? "" : offering.getName()),
                                        new CSVFile.CSVField(section.getSubpart().getName() + " " + section.getName()),
                                        new CSVFile.CSVField(section.getTime() == null ? "" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader() + " - " + section.getTime().getEndTimeHeader()),
                                        new CSVFile.CSVField(sDF1.format(enrl)),
                                        new CSVFile.CSVField(sDF2.format(desired)),
                                        new CSVFile.CSVField(sDF1.format(section.getLimit())),
                                        new CSVFile.CSVField(sDF2.format(Math.min(1.0, Math.max(-1.0, (enrl - desired) / section.getLimit()))))
                                });
                                last = offering;
                            }
                        }
                    } else {
                        // unlimited sections -> desired size is total enrollment / number of sections
                        for (Section section: subpart.getSections()) {
                            double enrl = 0.0;
                            for (Enrollment e: section.getEnrollments()) {
                                if (e.getStudent().isDummy() && !includeLastLikeStudents) continue;
                                if (!e.getStudent().isDummy() && !includeRealStudents) continue;
                                enrl += e.getRequest().getWeight();
                            }
                            double desired = configEnrl / subpart.getSections().size();
                            if (Math.abs(desired - enrl) >= Math.max(1.0, 0.1 * desired)) {
                                if (last != null && !offering.equals(last)) csv.addLine();
                                csv.addLine(new CSVFile.CSVField[] {
                                        new CSVFile.CSVField(offering.equals(last) ? "" : offering.getName()),
                                        new CSVFile.CSVField(section.getSubpart().getName() + " " + section.getName()),
                                        new CSVFile.CSVField(section.getTime() == null ? "" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader() + " - " + section.getTime().getEndTimeHeader()),
                                        new CSVFile.CSVField(sDF1.format(enrl)),
                                        new CSVFile.CSVField(sDF2.format(desired)),
                                        new CSVFile.CSVField(""),
                                        new CSVFile.CSVField(sDF2.format(Math.min(1.0, Math.max(-1.0, (enrl - desired) / desired))))
                                });
                                last = offering;
                            }
                        }
                    }
                }
            }
        }
        return csv;
    }
    
    @Override
    public CSVFile create(DataProperties properties) {
        return createTable(properties.getPropertyBoolean("lastlike", false), properties.getPropertyBoolean("real", true));
    }

}
