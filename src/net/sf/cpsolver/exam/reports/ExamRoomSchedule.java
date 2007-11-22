package net.sf.cpsolver.exam.reports;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

/**
 * Export schedule for each room into a CSV file.
 * <br><br>
 * Usage:<br>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExamRoomSchedule(model).report().save(file);
 * </code>
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class ExamRoomSchedule {
    ExamModel iModel = null;
    
    /**
     * Constructor
     * @param model examination timetabling model
     */
    public ExamRoomSchedule(ExamModel model) {
        iModel = model;
    }
    
    public CSVFile report() {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVField[] {
                new CSVField("Room"),
                new CSVField("Cap"),
                new CSVField("AltCap"),
                new CSVField("Period"),
                new CSVField("Date"),
                new CSVField("Time"),
                new CSVField("Exam"),
                new CSVField("Enrl")
        });
        Vector rooms = new Vector(iModel.getRooms());
        Collections.sort(rooms, new Comparator() {
            public int compare(Object o1, Object o2) {
                ExamRoom r1 = (ExamRoom)o1;
                ExamRoom r2 = (ExamRoom)o2;
                int cmp = -Double.compare(r1.getSize(),r2.getSize());
                if (cmp!=0) return cmp;
                cmp = -Double.compare(r1.getAltSize(),r2.getAltSize());
                if (cmp!=0) return cmp;
                return r1.compareTo(r2);
            }
        });
        for (Enumeration e=rooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            boolean first = true;
            int day = -1;
            for (Enumeration f=iModel.getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                ExamPlacement placement = room.getPlacement(period);
                if (placement==null) continue;
                Exam exam = (Exam)placement.variable();
                csv.addLine(new CSVField[] {
                   new CSVField(first?room.getName():""),
                   new CSVField(first?""+room.getSize():""),
                   new CSVField(first?""+room.getAltSize():""),
                   new CSVField(period.getIndex()+1),
                   new CSVField(day==period.getDay()?"":period.getDayStr()),
                   new CSVField(period.getTimeStr()),
                   new CSVField(exam.getName()),
                   new CSVField(exam.getStudents().size())
                });
                first=false;
                day = period.getDay();
            }
        }
        return csv;
    }
}
