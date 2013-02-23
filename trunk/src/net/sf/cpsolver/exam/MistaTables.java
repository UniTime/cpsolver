package net.sf.cpsolver.exam;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.dom4j.io.SAXReader;

/**
 * A simple program that prints a few statistics about the given examination problem in the format of the MISTA 2013 paper
 * (entitled Real-life Examination Timetabling).
 * It outputs data for the Table 1 (characteristics of the data sets) and Table 2 (number of rooms and exams of a certain size). 
 * <br>
 * Usage:
 * <code>java -cp cpsolver-all-1.2.jar net.sf.cpsolver.exam.MistaTables problem1.xml problem2.xml ...</code> 
 * <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
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
public class MistaTables {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(MistaTables.class);
    private static java.text.DecimalFormat sNF = new java.text.DecimalFormat("###,##0", new java.text.DecimalFormatSymbols(Locale.US));
    private static java.text.DecimalFormat sDF = new java.text.DecimalFormat("###,##0.000", new java.text.DecimalFormatSymbols(Locale.US));
    
    public static void main(String[] args) {
        try {
            ToolBox.configureLogging();
            DataProperties config = new DataProperties();
            
            Table[] tables = new Table[] { new Problems(), new Rooms() };
            
            for (int i = 0; i < args.length; i++) {
                File file = new File(args[i]);
                sLog.info("Loading " + file);
                ExamModel model = new ExamModel(config);
                model.load(new SAXReader().read(file));
                
                String name = file.getName();
                if (name.contains("."))
                    name = name.substring(0, name.indexOf('.'));
                
                for (Table table: tables)
                    table.add(name, model);
                
                Progress.removeInstance(model);
            }
            
            sLog.info("Saving tables...");
            File output = new File("tables"); output.mkdir();
            for (Table table: tables)
                table.save(output);
            
            sLog.info("All done.");
        } catch (Exception e) {
            sLog.error(e.getMessage(), e);
        }
    }
    
    public static class Counter {
        private double iTotal = 0.0, iMin = 0.0, iMax = 0.0, iTotalSquare = 0.0;
        private int iCount = 0;
        
        public Counter() {
        }
        
        public void inc(double value) {
                if (iCount == 0) {
                        iTotal = value;
                        iMin = value;
                        iMax = value;
                        iTotalSquare = value * value;
                } else {
                        iTotal += value;
                        iMin = Math.min(iMin, value);
                        iMax = Math.max(iMax, value);
                        iTotalSquare += value * value;
                }
                iCount ++;
        }
        
        public int count() { return iCount; }
        public double sum() { return iTotal; }
        public double min() { return iMin; }
        public double max() { return iMax; }
        public double rms() { return (iCount == 0 ? 0.0 : Math.sqrt(iTotalSquare / iCount) - Math.abs(avg())); }
        public double avg() { return (iCount == 0 ? 0.0 : iTotal / iCount); }

        @Override
        public String toString() {
                return sDF.format(sum()) +
                " (min: " + sDF.format(min()) +
                ", max: " + sDF.format(max()) +
                ", avg: " + sDF.format(avg()) +
                ", rms: " + sDF.format(rms()) +
                ", cnt: " + count() + ")";
        }
    }
    
    public static abstract class Table {
        private List<String> iProblems = new ArrayList<String>();
        private List<String> iProperties = new ArrayList<String>();
        private Map<String, Map<String, String>> iData = new HashMap<String, Map<String, String>>();
        
        public abstract void add(String problem, ExamModel model);
        
        
        protected void add(String problem, String property, int value) {
            add(problem, property, sNF.format(value));
        }
        
        protected void add(String problem, String property, double value) {
            add(problem, property, sDF.format(value));
        }
        
        protected void add(String problem, String property, Counter value) {
            add(problem, property, sDF.format(value.avg()) + " ± " + sDF.format(value.rms()));
        }

        protected void add(String problem, String property, String value) {
            if (!iProblems.contains(problem)) iProblems.add(problem);
            if (!iProperties.contains(property)) iProperties.add(property);
            Map<String, String> table = iData.get(problem);
            if (table == null) {
                table = new HashMap<String, String>();
                iData.put(problem, table);
            }
            table.put(property, value);
        }
        
        public void save(File folder) throws IOException {
            PrintWriter pw = new PrintWriter(new File(folder, getClass().getSimpleName() + ".csv"));
            
            pw.print("Problem");
            for (String problem: iProblems) pw.print(",\"" + problem + "\"");
            pw.println();
            
            for (String property: iProperties) {
                pw.print("\"" + property + "\"");
                for (String problem: iProblems) {
                    String value = iData.get(problem).get(property);
                    pw.print("," + (value == null ? "" : "\"" + value + "\""));
                }
                pw.println();
            }

            pw.flush(); pw.close();
        }
    }
    
    public static class Problems extends Table {
        @Override
        public void add(String problem, ExamModel model) {
            int enrollments = 0;
            for (ExamStudent student: model.getStudents())
                enrollments += student.variables().size();
            
            int examSeating = 0;
            int examsFixedInTime = 0, examsFixedInRoom = 0, examsLarge = 0, examsToSplit = 0, examsWithOriginalRoom = 0;
            Counter avgPeriods = new Counter(), avgRooms = new Counter(), avgBigRooms = new Counter();
            double density = 0;
            
            for (Exam exam: model.variables()) {
                if (exam.hasAltSeating()) examSeating ++;

                if (exam.getPeriodPlacements().size() <= 2)
                    examsFixedInTime ++;
                if (exam.getRoomPlacements().size() <= 2)
                    examsFixedInRoom ++;
                
                for (ExamRoomPlacement room: exam.getRoomPlacements()) {
                    if (room.getPenalty() < -2) { examsWithOriginalRoom ++; break; }
                }
                
                int bigEnoughRooms = 0;
                for (ExamRoomPlacement room: exam.getRoomPlacements()) {
                    if (room.getSize(exam.hasAltSeating()) >= exam.getSize()) bigEnoughRooms ++;
                }
                
                if (bigEnoughRooms == 0)
                    examsToSplit ++;
                
                if (exam.getSize() >= 600)
                    examsLarge ++;
                
                avgPeriods.inc(exam.getPeriodPlacements().size());
                avgRooms.inc(exam.getRoomPlacements().size());
                avgBigRooms.inc(bigEnoughRooms);
                
                density += exam.nrStudentCorrelatedExams();
            }
            
            add(problem, "Exams", model.variables().size());
            add(problem, "   with exam seating", examSeating);
            add(problem, "Students", model.getStudents().size());
            add(problem, "Enrollments", enrollments);
            add(problem, "Distribution constraints", model.getDistributionConstraints().size());
            
            add(problem, "Exams fixed in time", examsFixedInTime);
            add(problem, "Exams fixed in room", examsFixedInRoom);
            add(problem, "Large exams (600+)", examsLarge);
            add(problem, "Exams needing a room split", examsToSplit);
            add(problem, "Exams with original room", examsWithOriginalRoom);
            add(problem, "Density", sDF.format(100.0 * density / (model.variables().size() * (model.variables().size() - 1))) + "%");
            
            add(problem, "Average periods", avgPeriods);
            add(problem, "Average rooms", avgRooms);
            add(problem, "   that are big enough", avgBigRooms);
        }
    }
    
    public static class Rooms extends Table {
        @Override
        public void add(String problem, ExamModel model) {
            int[] sizes = new int[] { 0, 100, 200, 400, 600 };
            int[] nrRooms = new int[] { 0, 0, 0, 0, 0 }, nrRoomsAlt = new int[] { 0, 0, 0, 0, 0 };
            int[] nrExams = new int[] { 0, 0, 0, 0, 0 }, nrExamsAlt = new int[] { 0, 0, 0, 0, 0 };
            double[] density = new double[] { 0, 0, 0, 0, 0 };
            
            Set<ExamRoom> rooms = new HashSet<ExamRoom>();
            for (Exam exam: model.variables()) {
                for (ExamRoomPlacement room: exam.getRoomPlacements()) {
                    if (rooms.add(room.getRoom())) {
                        for (int i = 0; i < sizes.length; i++) {
                            if (room.getRoom().getSize() >= sizes[i])
                                nrRooms[i] ++;
                            if (room.getRoom().getAltSize() >= sizes[i])
                                nrRoomsAlt[i] ++;
                        }
                    }
                }

                for (int i = 0; i < sizes.length; i++) {
                    if (exam.getSize() >= sizes[i]) {
                        nrExams[i] ++;
                        if (exam.hasAltSeating())
                            nrExamsAlt[i] ++;
                        for (Exam x: exam.getStudentCorrelatedExams())
                            if (x.getSize() >= sizes[i])
                                density[i] ++;
                    }
                }
            }
            
            for (int i = 0; i < sizes.length; i++) {
                add(problem, "Rooms" + (sizes[i] == 0 ? "" : " (≥ " + sizes[i] + " seats)"), sNF.format(nrRooms[i]) + (sizes[i] == 0 ? "" : " (" + sNF.format(nrRoomsAlt[i]) + ")"));
            }
            for (int i = 0; i < sizes.length; i++) {
                add(problem, "Exams" + (sizes[i] == 0 ? "" : " (≥ " + sizes[i] + " seats)"), sNF.format(nrExams[i]) + (sizes[i] == 0 ? "" : " (" + sNF.format(nrExamsAlt[i]) + ")"));
            }
            for (int i = 0; i < sizes.length; i++) {
                add(problem, "Density" + (sizes[i] == 0 ? "" : " (≥ " + sizes[i] + " seats)"), sDF.format(100.0 * density[i] / (nrExams[i] * (nrExams[i] - 1))) + "%");
            }
        }
    }

}
