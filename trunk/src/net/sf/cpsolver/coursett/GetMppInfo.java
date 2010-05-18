package net.sf.cpsolver.coursett;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Process all solutions (files output.csv) in all subfolders of the given
 * folder and create a CSV (comma separated values text file) combining all
 * minimal perturbation information of the found solutions.
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
public class GetMppInfo {
    public static Hashtable<String, String> getInfo(File outputFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(outputFile));
            String line = null;
            Hashtable<String, String> info = new Hashtable<String, String>();
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf(',');
                if (idx >= 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (value.indexOf('(') >= 0 && value.indexOf(')') >= 0) {
                        value = value.substring(value.indexOf('(') + 1, value.indexOf(')'));
                        if (value.indexOf('/') >= 0) {
                            String bound = value.substring(value.indexOf('/') + 1);
                            if (bound.indexOf("..") >= 0) {
                                String min = bound.substring(0, bound.indexOf(".."));
                                String max = bound.substring(bound.indexOf("..") + 2);
                                info.put(key + " Min", min);
                                info.put(key + " Max", max);
                            } else {
                                info.put(key + " Bound", bound);
                            }
                            value = value.substring(0, value.indexOf('/'));
                        }
                    }
                    if (value.length() > 0)
                        info.put(key, value);
                }
            }
            reader.close();
            return info;
        } catch (Exception e) {
            System.err.println("Error reading info, message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void getInfos(File file, Hashtable<String, Hashtable<String, Hashtable<Integer, double[]>>> infos,
            String instance) {
        Hashtable<String, String> info = getInfo(file);
        if (info == null || info.isEmpty() || !info.containsKey("000.053 Given perturbations"))
            return;
        Integer pert = Integer.valueOf(info.get("000.053 Given perturbations"));
        for (Map.Entry<String, String> entry : info.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!key.startsWith("000.") || key.equals("000.053 Given perturbations"))
                continue;
            Hashtable<String, Hashtable<Integer, double[]>> keyTable = infos.get(key);
            if (keyTable == null) {
                keyTable = new Hashtable<String, Hashtable<Integer, double[]>>();
                infos.put(key, keyTable);
            }
            Hashtable<Integer, double[]> instanceTable = keyTable.get(instance);
            if (instanceTable == null) {
                instanceTable = new Hashtable<Integer, double[]>();
                keyTable.put(instance, instanceTable);
            }
            double[] pertTable = instanceTable.get(pert);
            if (pertTable == null) {
                pertTable = new double[] { 0, 0 };
                instanceTable.put(pert, pertTable);
            }
            pertTable[0] += Double.parseDouble(value);
            pertTable[1] += 1;
        }
    }

    public static void writeInfos(Hashtable<String, Hashtable<String, Hashtable<Integer, double[]>>> infos, File file)
            throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        for (Enumeration<String> e = ToolBox.sortEnumeration(infos.keys()); e.hasMoreElements();) {
            String key = e.nextElement();
            out.println(key);
            Hashtable<String, Hashtable<Integer, double[]>> keyTable = infos.get(key);
            TreeSet<Integer> perts = new TreeSet<Integer>();
            for (Enumeration<String> f = ToolBox.sortEnumeration(keyTable.keys()); f.hasMoreElements();) {
                String instance = f.nextElement();
                Hashtable<Integer, double[]> instanceTable = keyTable.get(instance);
                perts.addAll(instanceTable.keySet());
            }
            out.print(",,");
            for (Iterator<Integer> i = perts.iterator(); i.hasNext();) {
                Integer pert = i.next();
                out.print(pert);
                if (i.hasNext())
                    out.print(",");
            }
            out.println();
            for (Enumeration<String> f = ToolBox.sortEnumeration(keyTable.keys()); f.hasMoreElements();) {
                String instance = f.nextElement();
                Hashtable<Integer, double[]> instanceTable = keyTable.get(instance);
                perts.addAll(instanceTable.keySet());
                out.print("," + instance + ",");
                for (Iterator<Integer> i = perts.iterator(); i.hasNext();) {
                    Integer pert = i.next();
                    double[] pertTable = instanceTable.get(pert);
                    if (pertTable != null)
                        out.print(pertTable[0] / pertTable[1]);
                    if (i.hasNext())
                        out.print(",");
                }
                out.println();
            }
        }
        out.flush();
        out.close();
    }

    public static void main(String args[]) {
        try {
            File folder = new File(".");
            if (args.length >= 1)
                folder = new File(args[0]);
            String config = "mpp";
            if (args.length >= 2)
                config = args[1];
            File[] instanceFolders = folder.listFiles();
            Hashtable<String, Hashtable<String, Hashtable<Integer, double[]>>> infos = new Hashtable<String, Hashtable<String, Hashtable<Integer, double[]>>>();
            for (int i = 0; i < instanceFolders.length; i++) {
                File instanceFolder = instanceFolders[i];
                if (!instanceFolder.exists() || !instanceFolder.isDirectory()
                        || !instanceFolder.getName().startsWith(config + "-"))
                    continue;
                System.out.println("Checking " + instanceFolder.getName() + " ...");
                File[] files = instanceFolder.listFiles();
                for (int j = 0; j < files.length; j++)
                    if (files[j].isDirectory()) {
                        File outputFile = new File(files[j], "output.csv");
                        if (outputFile.exists()) {
                            System.out.println("  Checking " + files[j].getName() + " ...");
                            getInfos(outputFile, infos, instanceFolder.getName().substring(config.length() + 1));
                        }
                    }
            }
            if (!infos.isEmpty())
                writeInfos(infos, new File(folder, "info.csv"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
