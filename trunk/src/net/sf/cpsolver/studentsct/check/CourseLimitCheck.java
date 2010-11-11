package net.sf.cpsolver.studentsct.check;

import java.text.DecimalFormat;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * This class looks and reports cases when there are more students requesting a
 * course than the course limit.
 * 
 * <br>
 * <br>
 * 
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp; CourseLimitCheck ch = new CourseLimitCheck(model);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; if (!ch.check()) ch.getCSVFile().save(new File("limits.csv"));
 * </code>
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>CourseLimitCheck.FixUnlimited</td>
 * <td>{@link Boolean}</td>
 * <td>
 * If true, courses with zero or positive limit, but with unlimited sections,
 * are made unlimited (course limit is set to -1).</td>
 * </tr>
 * <tr>
 * <td>CourseLimitCheck.UpZeroLimits</td>
 * <td>{@link Boolean}</td>
 * <td>
 * If true, courses with zero limit, requested by one or more students are
 * increased in limit in order to accomodate all students that request the
 * course. Section limits are increased to ( total weight of all requests for
 * the offering / sections in subpart).</td>
 * </tr>
 * <tr>
 * <td>CourseLimitCheck.UpNonZeroLimits</td>
 * <td>{@link Boolean}</td>
 * <td>
 * If true, courses with positive limit, requested by more students than allowed
 * by the limit are increased in limit in order to accomodate all students that
 * requests the course. Section limits are increased proportionally by ( total
 * weight of all requests in the offering / current offering limit), where
 * offering limit is the sum of limits of courses of the offering.</td>
 * </tr>
 * </table>
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
public class CourseLimitCheck {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(CourseLimitCheck.class);
    private static DecimalFormat sDF = new DecimalFormat("0.0");
    private StudentSectioningModel iModel;
    private CSVFile iCSVFile = null;
    private boolean iFixUnlimited = false;
    private boolean iUpZeroLimits = false;
    private boolean iUpNonZeroLimits = false;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public CourseLimitCheck(StudentSectioningModel model) {
        iModel = model;
        iCSVFile = new CSVFile();
        iCSVFile.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("Course"), new CSVFile.CSVField("Limit"),
                new CSVFile.CSVField("Students"), new CSVFile.CSVField("Real"), new CSVFile.CSVField("Last-like") });
        iFixUnlimited = model.getProperties().getPropertyBoolean("CourseLimitCheck.FixUnlimited", iFixUnlimited);
        iUpZeroLimits = model.getProperties().getPropertyBoolean("CourseLimitCheck.UpZeroLimits", iUpZeroLimits);
        iUpNonZeroLimits = model.getProperties().getPropertyBoolean("CourseLimitCheck.UpNonZeroLimits",
                iUpNonZeroLimits);
    }

    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
    }

    /** Return report */
    public CSVFile getCSVFile() {
        return iCSVFile;
    }

    /**
     * Check for courses where the limit is below the number of students that
     * request the course
     * 
     * @return false, if there is such a case
     */
    public boolean check() {
        sLog.info("Checking for course limits...");
        boolean ret = true;
        for (Offering offering : getModel().getOfferings()) {
            boolean hasUnlimitedSection = false;
            if (iFixUnlimited)
                for (Config config : offering.getConfigs()) {
                    for (Subpart subpart : config.getSubparts()) {
                        for (Section section : subpart.getSections()) {
                            if (section.getLimit() < 0)
                                hasUnlimitedSection = true;
                        }
                    }
                }
            int offeringLimit = 0;
            int nrStudents = 0;
            for (Course course : offering.getCourses()) {
                if (course.getLimit() < 0) {
                    offeringLimit = -1;
                    continue;
                }
                if (iFixUnlimited && hasUnlimitedSection) {
                    sLog.info("Course " + course + " made unlimited.");
                    course.setLimit(-1);
                    offeringLimit = -1;
                    continue;
                }
                double total = 0;
                double lastLike = 0, real = 0;
                for (Request request : getModel().variables()) {
                    if (request instanceof CourseRequest && ((CourseRequest) request).getCourses().contains(course)) {
                        total += request.getWeight();
                        if (request.getStudent().isDummy())
                            lastLike += request.getWeight();
                        else
                            real += request.getWeight();
                    }
                }
                nrStudents += Math.round(total);
                offeringLimit += course.getLimit();
                if (Math.round(total) > course.getLimit()) {
                    sLog.error("Course " + course + " is requested by " + sDF.format(total)
                            + " students, but its limit is only " + course.getLimit());
                    ret = false;
                    iCSVFile.addLine(new CSVFile.CSVField[] { new CSVFile.CSVField(course.getName()),
                            new CSVFile.CSVField(course.getLimit()), new CSVFile.CSVField(total),
                            new CSVFile.CSVField(real), new CSVFile.CSVField(lastLike) });
                    if (iUpZeroLimits && course.getLimit() == 0) {
                        int oldLimit = course.getLimit();
                        course.setLimit((int) Math.round(total));
                        sLog.info("  -- limit of course " + course + " increased to " + course.getLimit() + " (was "
                                + oldLimit + ")");
                    } else if (iUpNonZeroLimits && course.getLimit() > 0) {
                        int oldLimit = course.getLimit();
                        course.setLimit((int) Math.round(total));
                        sLog.info("  -- limit of course " + course + " increased to " + course.getLimit() + " (was "
                                + oldLimit + ")");
                    }
                }
            }
            if (iUpZeroLimits && offeringLimit == 0 && nrStudents > 0) {
                for (Config config : offering.getConfigs()) {
                    for (Subpart subpart : config.getSubparts()) {
                        for (Section section : subpart.getSections()) {
                            int oldLimit = section.getLimit();
                            section.setLimit(Math.max(section.getLimit(), (int) Math.ceil(nrStudents
                                    / subpart.getSections().size())));
                            sLog.info("    -- limit of section " + section + " increased to " + section.getLimit()
                                    + " (was " + oldLimit + ")");
                        }
                    }
                }
            } else if (iUpNonZeroLimits && offeringLimit >= 0 && nrStudents > offeringLimit) {
                double fact = ((double) nrStudents) / offeringLimit;
                for (Config config : offering.getConfigs()) {
                    for (Subpart subpart : config.getSubparts()) {
                        for (Section section : subpart.getSections()) {
                            int oldLimit = section.getLimit();
                            section.setLimit((int) Math.ceil(fact * section.getLimit()));
                            sLog.info("    -- limit of section " + section + " increased to " + section.getLimit()
                                    + " (was " + oldLimit + ")");
                        }
                    }
                }
            }

            if (offeringLimit >= 0) {
                int totalSectionLimit = 0;
                for (Config config : offering.getConfigs()) {
                    int configLimit = -1;
                    for (Subpart subpart : config.getSubparts()) {
                        int subpartLimit = 0;
                        for (Section section : subpart.getSections()) {
                            subpartLimit += section.getLimit();
                        }
                        if (configLimit < 0)
                            configLimit = subpartLimit;
                        else
                            configLimit = Math.min(configLimit, subpartLimit);
                    }
                    totalSectionLimit += configLimit;
                }
                if (totalSectionLimit < offeringLimit) {
                    sLog.error("Offering limit of " + offering + " is " + offeringLimit
                            + ", but total section limit is only " + totalSectionLimit);
                    if (iUpZeroLimits && totalSectionLimit == 0) {
                        for (Config config : offering.getConfigs()) {
                            for (Subpart subpart : config.getSubparts()) {
                                for (Section section : subpart.getSections()) {
                                    int oldLimit = section.getLimit();
                                    section.setLimit(Math.max(section.getLimit(), (int) Math
                                            .ceil(((double) offeringLimit) / subpart.getSections().size())));
                                    sLog.info("    -- limit of section " + section + " increased to "
                                            + section.getLimit() + " (was " + oldLimit + ")");
                                }
                            }
                        }
                    } else if (iUpNonZeroLimits && totalSectionLimit > 0) {
                        double fact = ((double) offeringLimit) / totalSectionLimit;
                        for (Config config : offering.getConfigs()) {
                            for (Subpart subpart : config.getSubparts()) {
                                for (Section section : subpart.getSections()) {
                                    int oldLimit = section.getLimit();
                                    section.setLimit((int) Math.ceil(fact * section.getLimit()));
                                    sLog.info("    -- limit of section " + section + " increased to "
                                            + section.getLimit() + " (was " + oldLimit + ")");
                                }
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

}
