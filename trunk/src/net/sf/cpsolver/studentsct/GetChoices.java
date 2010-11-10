package net.sf.cpsolver.studentsct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Process all choice files (files choices.csv) in all subfolders of the given
 * folder and create a CSV (comma separated values text file) combining all
 * choices (one column for each choice file) of the found choices files.
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
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
public class GetChoices {

    public static void getChoicesFile(File folder, List<List<String>> choices, String prefix) {
        File choicesFile = new File(folder, "choices.csv");
        if (choicesFile.exists()) {
            System.out.println("Reading " + choicesFile + " ...");
            try {
                List<String> prefixes = null;
                if (choices.isEmpty()) {
                    prefixes = new ArrayList<String>();
                    choices.add(prefixes);
                } else {
                    prefixes = choices.get(0);
                }
                prefixes.add(prefix);
                BufferedReader reader = new BufferedReader(new FileReader(choicesFile));
                String line = null;
                for (int idx = 1; (line = reader.readLine()) != null; idx++) {
                    List<String> cx = null;
                    if (choices.size() <= idx) {
                        cx = new ArrayList<String>();
                        choices.add(cx);
                    } else {
                        cx = choices.get(idx);
                    }
                    cx.add(line);
                }
                reader.close();
            } catch (Exception e) {
                System.err.println("Error reading file " + choicesFile + ", message: " + e.getMessage());
            }
        }
    }

    public static void getChoices(File folder, List<List<String>> choices, String prefix) {
        System.out.println("Checking " + folder + " ...");
        File[] files = folder.listFiles();
        getChoicesFile(folder, choices, (prefix == null ? "" : prefix));
        for (int i = 0; i < files.length; i++)
            if (files[i].isDirectory())
                getChoices(files[i], choices, (prefix == null ? "" : prefix + "/") + files[i].getName());
    }

    public static void writeChoices(List<List<String>> choices, File file) {
        try {
            System.out.println("Writing " + file + " ...");
            PrintWriter writer = new PrintWriter(new FileWriter(file, false));
            for (List<String> cx : choices) {
                for (Iterator<String> f = cx.iterator(); f.hasNext();) {
                    String s = f.next();
                    writer.print(s);
                    if (f.hasNext())
                        writer.print(",");
                }
                writer.println();
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            System.err.println("Error writing file " + file + ", message: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            File folder = new File(args[0]);
            List<List<String>> choices = new ArrayList<List<String>>();
            getChoices(folder, choices, null);
            if (!choices.isEmpty())
                writeChoices(choices, new File(folder, "all-choices.csv"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
