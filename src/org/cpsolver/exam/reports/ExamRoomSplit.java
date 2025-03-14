package org.cpsolver.exam.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.CSVFile.CSVField;


/**
 * Export room splitting into a CSV file. <br>
 * <br>
 * Usage:
 * <pre><code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamRoomSplit(model).report().save(file);
 * </code></pre>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
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
public class ExamRoomSplit {
    private ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamRoomSplit(ExamModel model) {
        iModel = model;
    }

    /**
     * generate report
     * @param assignment current assignment
     * @return resultant report
     */
    public CSVFile report(Assignment<Exam, ExamPlacement> assignment) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Exam"), new CSVField("Enrl"), new CSVField("Period"),
                new CSVField("Date"), new CSVField("Time"), new CSVField("Room 1"), new CSVField("Cap 1"),
                new CSVField("Room 2"), new CSVField("Cap 2"), new CSVField("Room 3"), new CSVField("Cap 3"),
                new CSVField("Room 4"), new CSVField("Cap 4") });
        for (Exam exam : iModel.variables()) {
            ExamPlacement placement = assignment.getValue(exam);
            if (placement == null || placement.getRoomPlacements().size() <= 1)
                continue;
            List<CSVField> fields = new ArrayList<CSVField>();
            fields.add(new CSVField(exam.getName()));
            fields.add(new CSVField(exam.getStudents().size()));
            fields.add(new CSVField(placement.getPeriod().getIndex() + 1));
            fields.add(new CSVField(placement.getPeriod().getDayStr()));
            fields.add(new CSVField(placement.getPeriod().getTimeStr()));
            TreeSet<ExamRoomPlacement> rooms = new TreeSet<ExamRoomPlacement>(new ExamRoomComparator(exam, false));
            rooms.addAll(placement.getRoomPlacements());
            for (ExamRoomPlacement room : rooms) {
                fields.add(new CSVField(room.getName()));
                fields.add(new CSVField(room.getSize(exam.hasAltSeating())));
            }
            csv.addLine(fields);
        }
        return csv;
    }
}
