package net.sf.cpsolver.exam.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export schedule for each room into a CSV file. <br>
 * <br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamRoomSchedule(model).report().save(file);
 * </code> <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
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

    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] { new CSVField("Room"), new CSVField("Cap"), new CSVField("AltCap"),
                new CSVField("Period"), new CSVField("Date"), new CSVField("Time"), new CSVField("Exam"),
                new CSVField("Enrl") });
        List<ExamRoom> rooms = new ArrayList<ExamRoom>(iModel.getRooms());
        Collections.sort(rooms, new Comparator<ExamRoom>() {
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
                ExamPlacement placement = room.getPlacement(period);
                if (placement == null)
                    continue;
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
        return csv;
    }
}
