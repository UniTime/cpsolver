package net.sf.cpsolver.studentsct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Process all choice files (files choices.csv) in all subfolders of the given folder 
 * and create a CSV (comma separated values text file) combining all choices (one column for each
 * choice file) of the found choices files.
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
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
public class GetChoices {
    
    public static void getChoicesFile(File folder, Vector choices, String prefix) {
        File choicesFile = new File(folder, "choices.csv");
        if (choicesFile.exists()) {
            System.out.println("Reading "+choicesFile+" ...");
            try {
                Vector prefixes = null;
                if (choices.isEmpty()) {
                    prefixes = new Vector();
                    choices.add(prefixes);
                } else { 
                    prefixes = (Vector)choices.firstElement();
                }
                prefixes.add(prefix);
                BufferedReader reader = new BufferedReader(new FileReader(choicesFile));
                String line = null;
                for (int idx=1; (line=reader.readLine())!=null; idx++) {
                    Vector cx = null;
                    if (choices.size()<=idx) {
                        cx = new Vector();
                        choices.add(cx);
                    } else {
                        cx = (Vector)choices.elementAt(idx);
                    }
                    cx.add(line);
                }
                reader.close();
            } catch (Exception e) {
                System.err.println("Error reading file "+choicesFile+", message: "+e.getMessage());
            }
        }
    }
    
    public static void getChoices(File folder, Vector choices, String prefix) {
        System.out.println("Checking "+folder+" ...");
        File[] files = folder.listFiles();
        getChoicesFile(folder, choices, (prefix==null?"":prefix));
        for (int i=0;i<files.length;i++)
            if (files[i].isDirectory())
                getChoices(files[i], choices, (prefix==null?"":prefix+"/")+files[i].getName());
    }
    
    public static void writeChoices(Vector chocies, File file) {
        try {
            System.out.println("Writing "+file+" ...");
            PrintWriter writer = new PrintWriter(new FileWriter(file,false));
            for (Enumeration e=chocies.elements();e.hasMoreElements();) {
                Vector cx = (Vector)e.nextElement();
                for (Enumeration f=cx.elements();f.hasMoreElements();) {
                    String s = (String)f.nextElement();
                    writer.print(s);
                    if (f.hasMoreElements()) writer.print(",");
                }
                writer.println();
            }
            writer.flush(); writer.close();
        } catch (Exception e) {
            System.err.println("Error writing file "+file+", message: "+e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            File folder = new File(args[0]);
            Vector choices = new Vector();
            getChoices(folder, choices, null);
            if (!choices.isEmpty())
                writeChoices(choices,new File(folder,"all-choices.csv"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
