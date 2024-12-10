package org.cpsolver.exam.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoom;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.CSVFile.CSVField;


/**
 * Export schedule for each room into a CSV file. <br>
 * <br>
 * Usage:
 * <pre><code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamRoomSchedule(model).report().save(file);
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
public class ExamRoomSchedule {
    ExamModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     */
    public ExamRoomSchedule(ExamModel model) {
        iModel = model;
    }

    public CSVFile report(Assignment<Exam, ExamPlacement> assignment) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Room"), new CSVField("Cap"), new CSVField("AltCap"),
                new CSVField("Period"), new CSVField("Date"), new CSVField("Time"), new CSVField("Exam"),
                new CSVField("Enrl") });
        List<ExamRoom> rooms = new ArrayList<ExamRoom>(iModel.getRooms());
        Collections.sort(rooms, new Comparator<ExamRoom>() {
            @Override
            public int compare(ExamRoom r1, ExamRoom r2) {
                int cmp = -Double.compare(r1.getSize(), r2.getSize());
                if (cmp != 0)
                    return cmp;
                cmp = -Double.compare(r1.getAltSize(), r2.getAltSize());
                if (cmp != 0)
                    return cmp;
                return r1.compareTo(r2);
            }
        });
        for (ExamRoom room : rooms) {
            boolean first = true;
            int day = -1;
            for (ExamPeriod period : iModel.getPeriods()) {
                for (ExamPlacement placement: room.getPlacements(assignment, period)) {
                    Exam exam = placement.variable();
                    csv.addLine(new CSVField[] { new CSVField(first ? room.getName() : ""),
                            new CSVField(first ? "" + room.getSize() : ""),
                            new CSVField(first ? "" + room.getAltSize() : ""), new CSVField(period.getIndex() + 1),
                            new CSVField(day == period.getDay() ? "" : period.getDayStr()),
                            new CSVField(period.getTimeStr()), new CSVField(exam.getName()),
                            new CSVField(exam.getStudents().size()) });
                    first = false;
                    day = period.getDay();
                }
            }
        }
        return csv;
    }
}
