package net.sf.cpsolver.coursett;

import java.io.File;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Create domain chart of the given input problem as CSV file (3 dimensions:
 * #rooms, #times, #variables with the given number of rooms/times)
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
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
public class DomainChart {
    protected int iSizeX = 60, iSizeY = 100;
    protected TimetableModel iModel;
    protected double[][] iTable = null;
    protected boolean iShowZero = false;
    protected String iName = null;
    protected String[] iHeader = null;
    protected String[] iTitle = null;

    public DomainChart(String name, TimetableModel model, int sizeX, int sizeY) {
        iModel = model;
        iName = name;
        iSizeX = sizeX;
        iSizeY = sizeY;
    }

    public DomainChart(File xmlFile, int sizeX, int sizeY) throws Exception {
        this(xmlFile.getName().substring(0, xmlFile.getName().lastIndexOf('.')), new TimetableModel(
                new DataProperties()), sizeX, sizeY);
        TimetableXMLLoader loader = new TimetableXMLLoader(iModel);
        loader.setInputFile(xmlFile);
        loader.load();
    }

    protected void clearTable() {
        iTable = new double[2 + iSizeX][2 + iSizeY];
        for (int i = 0; i < iTable.length; i++)
            for (int j = 0; j < iTable[i].length; j++)
                iTable[i][j] = 0;
        iHeader = new String[iSizeX + 2];
        for (int i = 0; i <= iSizeX; i++)
            iHeader[i] = String.valueOf(i);
        iHeader[iSizeX + 1] = (iSizeX + 1) + "+";
        iTitle = new String[iSizeY + 2];
        for (int i = 0; i <= iSizeY; i++)
            iTitle[i] = String.valueOf(i);
        iTitle[iSizeY + 1] = (iSizeY + 1) + "+";
    }

    protected void add(int x, int y, double val) {
        iTable[x <= iSizeX ? x : 1 + iSizeX][y <= iSizeY ? y : 1 + iSizeY] += val;
    }

    protected void computeTable() {
        clearTable();
        for (Lecture lecture : iModel.variables()) {
            if (lecture.getNrRooms() > 1)
                add(lecture.nrTimeLocations(), (int) Math.round(Math.pow(lecture.nrRoomLocations(), 1.0 / lecture
                        .getNrRooms())), 1);
            else
                add(lecture.nrTimeLocations(), lecture.nrRoomLocations(), 1);
        }
    }

    public CSVFile createTable() {
        computeTable();
        CSVFile csv = new CSVFile();
        CSVFile.CSVField[] header = new CSVFile.CSVField[2 + iSizeX + (iShowZero ? 1 : 0)];
        header[0] = new CSVFile.CSVField(iName);
        for (int i = (iShowZero ? 0 : 1); i <= iSizeX + 1; i++)
            header[(iShowZero ? 1 : 0) + i] = new CSVFile.CSVField(iHeader[i]);
        csv.setHeader(header);
        for (int y = (iShowZero ? 0 : 1); y <= 1 + iSizeY; y++) {
            CSVFile.CSVField[] line = new CSVFile.CSVField[2 + iSizeX + (iShowZero ? 1 : 0)];
            line[0] = new CSVFile.CSVField(iTitle[y]);
            if (y == 1 + iSizeY)
                line[0] = new CSVFile.CSVField((1 + iSizeY) + "+");
            for (int x = (iShowZero ? 0 : 1); x <= 1 + iSizeX; x++)
                line[(iShowZero ? 1 : 0) + x] = new CSVFile.CSVField(iTable[x][y]);
            csv.addLine(line);
        }
        return csv;
    }

    public static void main(String args[]) {
        try {
            ToolBox.configureLogging();
            File input = new File(args[0]);
            int sizeX = Integer.parseInt(args[1]);
            int sizeY = Integer.parseInt(args[2]);
            File output = null;
            if (args.length > 3) {
                output = new File(args[3]);
                if (output.exists() && output.isDirectory())
                    output = new File(output, input.getName().substring(0, input.getName().lastIndexOf('.'))
                            + "_domain.csv");
            } else {
                output = new File(input.getParentFile(), input.getName().substring(0, input.getName().lastIndexOf('.'))
                        + "_domain.csv");
            }
            new DomainChart(input, sizeX, sizeY).createTable().save(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
