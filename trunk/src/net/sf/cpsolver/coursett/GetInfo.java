package net.sf.cpsolver.coursett;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;

import org.apache.log4j.BasicConfigurator;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Process all solutions (files solution.xml or output.csv) in all subfolders of
 * the given folder and create a CSV (comma separated values text file) with
 * solution infos of the found solutions.
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
public class GetInfo {

    public static HashMap<String, String> getInfoOfASolution(File file) {
        try {
            DataProperties properties = new DataProperties();
            properties.setProperty("General.Input", file.getPath());
            TimetableXMLLoader loader = new TimetableXMLLoader(new TimetableModel(properties));
            loader.load();
            File newOutputFile = new File(file.getParentFile(), "new-output.csv");
            Test.saveOutputCSV(new Solution<Lecture, Placement>(loader.getModel()), newOutputFile);
            Progress.removeInstance(loader.getModel());
            System.out.println("  Reading " + newOutputFile + " ...");
            HashMap<String, String> info = getInfo(newOutputFile);
            File outputFile = new File(file.getParentFile(), "output.csv");
            if (outputFile.exists()) {
                System.out.println("  Reading " + outputFile + " ...");
                HashMap<String, String> info2 = getInfo(outputFile);
                if (info2.containsKey("000.002 Time [sec]"))
                    info.put("000.002 Time [sec]", info2.get("000.002 Time [sec]"));
            }
            return info;
        } catch (Exception e) {
            System.err.println("Error reading info, message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static HashMap<String, String> getInfo(String comment) {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(comment));
            String line = null;
            HashMap<String, String> info = new HashMap<String, String>();
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf(':');
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

    public static HashMap<String, String> getInfo(File outputFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(outputFile));
            String line = null;
            HashMap<String, String> info = new HashMap<String, String>();
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

    public static HashMap<String, String> getInfo(Element root) {
        try {
            HashMap<String, String> info = new HashMap<String, String>();
            for (Iterator<?> i = root.elementIterator("property"); i.hasNext();) {
                Element property = (Element) i.next();
                String key = property.attributeValue("name");
                String value = property.getText();
                if (key == null || value == null)
                    continue;
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
            return info;
        } catch (Exception e) {
            System.err.println("Error reading info, message: " + e.getMessage());
            return null;
        }
    }

    public static void getInfo(File folder, List<Info> infos, String prefix) {
        File infoFile = new File(folder, "info.xml");
        if (infoFile.exists()) {
            System.out.println("Reading " + infoFile + " ...");
            try {
                Document document = (new SAXReader()).read(infoFile);
                HashMap<String, String> info = getInfo(document.getRootElement());
                if (info != null && !info.isEmpty()) {
                    infos.add(new Info(prefix, info));
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error reading file " + infoFile + ", message: " + e.getMessage());
            }
        }
        File solutionFile = new File(folder, "solution.xml");
        if (solutionFile.exists()) {
            /*
             * File newOutputFile = new File(folder, "new-output.csv"); if
             * (newOutputFile.exists()) {
             * System.out.println("Reading "+newOutputFile+" ..."); try {
             * HashMap info = getInfo(newOutputFile); if (info!=null &&
             * !info.isEmpty()) { infos.addElement(new Object[]{prefix,info});
             * return; } } catch (Exception e) {
             * System.err.println("Error reading file "
             * +infoFile+", message: "+e.getMessage()); } }
             */
            System.out.println("Reading " + solutionFile + " ...");
            try {
                HashMap<String, String> info = getInfoOfASolution(solutionFile);
                if (info != null && !info.isEmpty()) {
                    infos.add(new Info(prefix, info));
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error reading file " + infoFile + ", message: " + e.getMessage());
            }
        }
        File outputFile = new File(folder, "output.csv");
        if (outputFile.exists()) {
            System.out.println("Reading " + outputFile + " ...");
            try {
                HashMap<String, String> info = getInfo(outputFile);
                if (info != null && !info.isEmpty()) {
                    infos.add(new Info(prefix, info));
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error reading file " + infoFile + ", message: " + e.getMessage());
            }
        }
    }

    public static void getInfos(File folder, List<Info> infos, String prefix) {
        System.out.println("Checking " + folder + " ...");
        File[] files = folder.listFiles();
        getInfo(folder, infos, (prefix == null ? "" : prefix));
        for (int i = 0; i < files.length; i++)
            if (files[i].isDirectory())
                getInfos(files[i], infos, (prefix == null ? "" : prefix + "/") + files[i].getName());
    }

    public static void writeInfos(List<Info> infos, File file) {
        try {
            System.out.println("Writing " + file + " ...");
            TreeSet<String> keys = new TreeSet<String>();
            ArrayList<CSVFile.CSVField> headers = new ArrayList<CSVFile.CSVField>();
            headers.add(new CSVFile.CSVField(""));
            for (Info o : infos) {
                keys.addAll(o.getInfo().keySet());
                headers.add(new CSVFile.CSVField(o.getPrefix()));
            }
            CSVFile csvFile = new CSVFile();
            csvFile.setHeader(headers);
            for (String key : keys) {
                ArrayList<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                line.add(new CSVFile.CSVField(key));
                for (Info o : infos) {
                    Map<String, String> info = o.getInfo();
                    String value = info.get(key);
                    line.add(new CSVFile.CSVField(value == null ? "" : value));
                }
                csvFile.addLine(line);
            }
            csvFile.save(file);
        } catch (Exception e) {
            System.err.println("Error writing file " + file + ", message: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();
            File folder = new File(args[0]);
            List<Info> infos = new ArrayList<Info>();
            getInfos(folder, infos, null);
            if (!infos.isEmpty())
                writeInfos(infos, new File(folder, "info.csv"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Info {
        private String iPrefix;
        private HashMap<String, String> iInfo;

        public Info(String prefix, HashMap<String, String> info) {
            iPrefix = prefix;
            iInfo = info;
        }

        public String getPrefix() {
            return iPrefix;
        }

        public Map<String, String> getInfo() {
            return iInfo;
        }
    }
}
