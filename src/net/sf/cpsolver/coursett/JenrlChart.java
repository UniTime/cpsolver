package net.sf.cpsolver.coursett;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Create joint enrollment chart of the given input problem as CSV file (3
 * dimensions: 1st variable, 2nd variable, number of students in common)
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class JenrlChart extends DomainChart {
    protected int iMax = 100;

    public JenrlChart(String name, TimetableModel model, int max) {
        super(name, model, Math.min(max, model.variables().size()), Math.min(max, model.variables().size()));
        iMax = max;
    }

    public JenrlChart(File xmlFile, int max) throws Exception {
        super(xmlFile, 0, 0);
        iMax = max;
        iSizeX = Math.min(iMax, iModel.variables().size());
        iSizeY = Math.min(iMax, iModel.variables().size());
    }

    @Override
    protected void computeTable() {
        clearTable();
        TreeSet<Lecture> vars = new TreeSet<Lecture>(new Comparator<Lecture>() {
            public int compare(Lecture l1, Lecture l2) {
                int cmp = -Double.compare(l1.students().size(), l2.students().size());
                if (cmp != 0)
                    return cmp;
                cmp = -Double.compare(l1.classLimit(), l2.classLimit());
                if (cmp != 0)
                    return cmp;
                return Double.compare(l1.getId(), l2.getId());
            }
        });
        vars.addAll(iModel.variables());
        int x = 1;
        for (Iterator<Lecture> i = vars.iterator(); i.hasNext(); x++) {
            Lecture l1 = i.next();
            if (x > iMax)
                continue;
            iHeader[x] = String.valueOf(l1.students().size());
            int y = 1;
            for (Iterator<Lecture> j = vars.iterator(); j.hasNext(); y++) {
                Lecture l2 = j.next();
                if (y > iMax)
                    continue;
                iTitle[y] = l2.getName();
                if (x >= y)
                    continue;
                add(x, y, l1.sameStudents(l2).size());
            }
        }
    }

    public static void main(String args[]) {
        try {
            ToolBox.configureLogging();
            File input = new File(args[0]);
            int max = Integer.parseInt(args[1]);
            File output = null;
            if (args.length > 2) {
                output = new File(args[2]);
                if (output.exists() && output.isDirectory())
                    output = new File(output, input.getName().substring(0, input.getName().lastIndexOf('.'))
                            + "_jenrl.csv");
            } else {
                output = new File(input.getParentFile(), input.getName().substring(0, input.getName().lastIndexOf('.'))
                        + "_jenrl.csv");
            }
            new JenrlChart(input, max).createTable().save(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
