package net.sf.cpsolver.ifs.util;

import java.io.File;
import java.io.PrintWriter;

import net.sf.cpsolver.ifs.util.CSVFile.CSVLine;

/**
 * A simple class converting CSV files to LaTeX tables.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class Csv2Html {
    
    public static void main(String args[]) {
        try {
            for (int i = 0; i < args.length; i++) {
                File file = new File(args[i]);
                
                String name = file.getName();
                if (name.contains(".")) name = name.substring(0, name.indexOf('.'));
                
                CSVFile csv = new CSVFile(file);
                
                PrintWriter pw = new PrintWriter(new File(file.getParentFile(), name + ".html"));

                pw.println("<table>");

                pw.println("\t<tr>");
                for (int j = 0; j < csv.getHeader().size(); j++) {
                    pw.println("\t\t<th style='border-bottom: 1px #8000AD dashed;' align='" + (j == 0 ? "left" : "center") + "'>" + csv.getHeader().getField(j).toString() + "</th>");
                }
                pw.println("\t</tr>");
                
                boolean header = false;
                for (CSVLine line: csv.getLines()) {
                    if (line.getField(0).isEmpty()) {
                        pw.println("\t<tr><td colspan='" + line.size() + "'>&nbsp;</td></tr>");
                        header = true;
                        continue;
                    }
                    pw.println("\t<tr>");
                    for (int j = 0; j < line.size(); j++) {
                        if (header) {
                            pw.println("\t\t<th style='border-bottom: 1px #8000AD dashed;' align='" + (j == 0 ? "left" : "center") + "'>" + line.getField(j).toString() + "</th>");
                        } else {
                            pw.println("\t\t<td align='" + (j == 0 ? "left" : "center") + "' nowrap>" + (line.getField(j) == null || line.getField(j).isEmpty() ? "-" : line.getField(j).toString().replace("+/-", "&plusmn;")) + "</td>");
                        }
                    }
                    pw.println("\t</tr>");
                    header = false;
                }
                
                pw.println("</table>");
                
                pw.flush(); pw.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
