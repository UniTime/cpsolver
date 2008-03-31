package net.sf.cpsolver.exam.reports;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export exam time and room assignments into a CSV file.
 * <br><br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamAssignments(model).report().save(file);
 * </code>
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ExamAssignments {
    private ExamModel iModel = null;

    /**
     * Constructor
     * @param model examination timetabling model
     */
    public ExamAssignments(ExamModel model) {
        iModel = model;
    }
    
    /**
     * generate report
     */
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] {
                new CSVField("Exam"),
                new CSVField("Enrl"),
                new CSVField("Alt"),
                new CSVField("Period"),
                new CSVField("Date"),
                new CSVField("Time"),
                new CSVField("Room"),
                new CSVField("Cap")
        });
        for (Enumeration e=iModel.variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            ExamPlacement placement = (ExamPlacement)exam.getAssignment();
            Vector fields = new Vector();
            fields.addElement(new CSVField(exam.getName()));
            fields.addElement(new CSVField(exam.getStudents().size()));
            fields.addElement(new CSVField(exam.hasAltSeating()?"Yes":"No"));
            if (placement==null) {
                fields.addElement(new CSVField(""));
                fields.addElement(new CSVField(""));
                fields.addElement(new CSVField(""));
                fields.addElement(new CSVField(""));
                fields.addElement(new CSVField(""));
            } else {
                fields.addElement(new CSVField(placement.getPeriod().getIndex()+1));
                fields.addElement(new CSVField(placement.getPeriod().getDayStr()));
                fields.addElement(new CSVField(placement.getPeriod().getTimeStr()));
                String rooms = "";
                String roomSizes = "";
                for (Iterator i=placement.getRoomPlacements().iterator();i.hasNext();) {
                    ExamRoomPlacement room = (ExamRoomPlacement)i.next();
                    rooms += room.getRoom().getName();
                    roomSizes += room.getSize(exam.hasAltSeating());
                    if (i.hasNext()) {
                        rooms+=", ";
                        roomSizes+=", ";
                    }
                }
                fields.addElement(new CSVField(rooms));
                fields.addElement(new CSVField(roomSizes));
            }
            csv.addLine(fields);
        }
        return csv;
    }
}
