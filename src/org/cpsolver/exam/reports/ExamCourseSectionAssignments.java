package org.cpsolver.exam.reports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamOwner;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.CSVFile.CSVField;


/**
 * Export exam time and room assignments into a CSV file. Similar to
 * {@link ExamAssignments}, however, a line is created for each course/section. <br>
 * <br>
 * Usage:
 * <pre><code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamCourseSectionAssignments(model).report().save(file);
 * </code></pre>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamCourseSectionAssignments {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamCourseSectionAssignments(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     * @param assignment current assignment
     * @return resultant report
     */
    public CSVFile report(Assignment<Exam, ExamPlacement> assignment) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Section/Course"), new CSVField("Enrl"), new CSVField("Alt"),
                new CSVField("Period"), new CSVField("Date"), new CSVField("Time"), new CSVField("Room"),
                new CSVField("Cap") });
        for (Exam exam : iModel.variables()) {
            ExamPlacement placement = assignment.getValue(exam);
            for (ExamOwner owner : exam.getOwners()) {
                List<CSVField> fields = new ArrayList<CSVField>();
                fields.add(new CSVField(owner.getName()));
                fields.add(new CSVField(owner.getStudents().size()));
                fields.add(new CSVField(exam.hasAltSeating() ? "Yes" : "No"));
                if (placement == null) {
                    fields.add(new CSVField(""));
                    fields.add(new CSVField(""));
                    fields.add(new CSVField(""));
                    fields.add(new CSVField(""));
                    fields.add(new CSVField(""));
                } else {
                    fields.add(new CSVField(placement.getPeriod().getIndex() + 1));
                    fields.add(new CSVField(placement.getPeriod().getDayStr()));
                    fields.add(new CSVField(placement.getPeriod().getTimeStr()));
                    String rooms = "";
                    String roomSizes = "";
                    for (Iterator<ExamRoomPlacement> i = placement.getRoomPlacements().iterator(); i.hasNext();) {
                        ExamRoomPlacement room = i.next();
                        rooms += room.getName();
                        roomSizes += room.getSize(exam.hasAltSeating());
                        if (i.hasNext()) {
                            rooms += ", ";
                            roomSizes += ", ";
                        }
                    }
                    fields.add(new CSVField(rooms));
                    fields.add(new CSVField(roomSizes));
                }
                csv.addLine(fields);
            }
        }
        return csv;
    }
}
