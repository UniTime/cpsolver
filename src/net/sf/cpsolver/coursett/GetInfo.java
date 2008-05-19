package net.sf.cpsolver.coursett;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

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
 * Process all solutions (files solution.xml or output.csv) in all subfolders of the given folder 
 * and create a CSV (comma separated values text file) with solution infos of the found
 * solutions.
 * 
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
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
public class GetInfo {
    
    public static Hashtable getInfoOfASolution(File file) {
        try {
            DataProperties properties = new DataProperties();
            properties.setProperty("General.Input", file.getPath());
            TimetableXMLLoader loader = new TimetableXMLLoader(new TimetableModel(properties));
            loader.load();
            File newOutputFile = new File(file.getParentFile(),"new-output.csv");
            Test.saveOutputCSV(new Solution(loader.getModel()), newOutputFile);
            Progress.removeInstance(loader.getModel());
            System.out.println("  Reading "+newOutputFile+" ...");
            Hashtable info = getInfo(newOutputFile);
            File outputFile = new File(file.getParentFile(),"output.csv");
            if (outputFile.exists()) {
                System.out.println("  Reading "+outputFile+" ...");
                Hashtable info2 = getInfo(outputFile);
                if (info2.containsKey("000.002 Time [sec]"))
                    info.put("000.002 Time [sec]", info2.get("000.002 Time [sec]"));
            }
            return info;
        } catch (Exception e) {
            System.err.println("Error reading info, message: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static Hashtable getInfo(String comment) {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(comment));
            String line = null;
            Hashtable info = new Hashtable();
            while ((line=reader.readLine())!=null) {
                int idx = line.indexOf(':'); 
                if (idx>=0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx+1).trim();
                    if (value.indexOf('(')>=0 && value.indexOf(')')>=0) {
                        value = value.substring(value.indexOf('(')+1, value.indexOf(')'));
                        if (value.indexOf('/')>=0) {
                            String bound = value.substring(value.indexOf('/')+1);
                            if (bound.indexOf("..")>=0) {
                                String min = bound.substring(0, bound.indexOf(".."));
                                String max = bound.substring(bound.indexOf("..")+2);
                                info.put(key+" Min",min);
                                info.put(key+" Max",max);
                            } else {
                                info.put(key+" Bound",bound);
                            }
                            value = value.substring(0, value.indexOf('/'));
                        }
                    }
                    if (value.length()>0) info.put(key, value);
                }
            }
            reader.close();
            return info;
        } catch (Exception e) {
            System.err.println("Error reading info, message: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static Hashtable getInfo(File outputFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(outputFile));
            String line = null;
            Hashtable info = new Hashtable();
            while ((line=reader.readLine())!=null) {
                int idx = line.indexOf(','); 
                if (idx>=0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx+1).trim();
                    if (value.indexOf('(')>=0 && value.indexOf(')')>=0) {
                        value = value.substring(value.indexOf('(')+1, value.indexOf(')'));
                        if (value.indexOf('/')>=0) {
                            String bound = value.substring(value.indexOf('/')+1);
                            if (bound.indexOf("..")>=0) {
                                String min = bound.substring(0, bound.indexOf(".."));
                                String max = bound.substring(bound.indexOf("..")+2);
                                info.put(key+" Min",min);
                                info.put(key+" Max",max);
                            } else {
                                info.put(key+" Bound",bound);
                            }
                            value = value.substring(0, value.indexOf('/'));
                        }
                    }
                    if (value.length()>0) info.put(key, value);
                }
            }
            reader.close();
            return info;
        } catch (Exception e) {
            System.err.println("Error reading info, message: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static Hashtable getInfo(Element root) {
        try {
            Hashtable info = new Hashtable();
            for (Iterator i=root.elementIterator("property");i.hasNext();) {
                Element property = (Element)i.next();
                String key = property.attributeValue("name");
                String value = property.getText();
                if (key==null || value==null) continue;
                if (value.indexOf('(')>=0 && value.indexOf(')')>=0) {
                    value = value.substring(value.indexOf('(')+1, value.indexOf(')'));
                    if (value.indexOf('/')>=0) {
                        String bound = value.substring(value.indexOf('/')+1);
                        if (bound.indexOf("..")>=0) {
                            String min = bound.substring(0, bound.indexOf(".."));
                            String max = bound.substring(bound.indexOf("..")+2);
                            info.put(key+" Min",min);
                            info.put(key+" Max",max);
                        } else {
                            info.put(key+" Bound",bound);
                        }
                        value = value.substring(0, value.indexOf('/'));
                    }
                }
                if (value.length()>0) info.put(key, value);
                
            }
            return info;
        } catch (Exception e) {
            System.err.println("Error reading info, message: "+e.getMessage());
            return null;
        }
    }
    
    
    public static void getInfo(File folder, Vector infos, String prefix) {
        File infoFile = new File(folder, "info.xml");
        if (infoFile.exists()) {
            System.out.println("Reading "+infoFile+" ...");
            try {
                Document document = (new SAXReader()).read(infoFile);
                Hashtable info = getInfo(document.getRootElement());
                if (info!=null && !info.isEmpty()) {
                    infos.addElement(new Object[]{prefix,info});
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error reading file "+infoFile+", message: "+e.getMessage());
            }
        }
        File solutionFile = new File(folder, "solution.xml");
        if (solutionFile.exists()) {
            /*
            File newOutputFile = new File(folder, "new-output.csv");
            if (newOutputFile.exists()) {
                System.out.println("Reading "+newOutputFile+" ...");
                try {
                    Hashtable info = getInfo(newOutputFile);
                    if (info!=null && !info.isEmpty()) {
                        infos.addElement(new Object[]{prefix,info});
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error reading file "+infoFile+", message: "+e.getMessage());
                }
            }*/
            System.out.println("Reading "+solutionFile+" ...");
            try {
                Hashtable info = getInfoOfASolution(solutionFile);
                if (info!=null && !info.isEmpty()) {
                    infos.addElement(new Object[]{prefix,info});
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error reading file "+infoFile+", message: "+e.getMessage());
            }
        }
        File outputFile = new File(folder, "output.csv");
        if (outputFile.exists()) {
            System.out.println("Reading "+outputFile+" ...");
            try {
                Hashtable info = getInfo(outputFile);
                if (info!=null && !info.isEmpty()) {
                    infos.addElement(new Object[]{prefix,info});
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error reading file "+infoFile+", message: "+e.getMessage());
            }
        }
    }
    
    public static void getInfos(File folder, Vector infos, String prefix) {
        System.out.println("Checking "+folder+" ...");
        File[] files = folder.listFiles();
        getInfo(folder, infos, (prefix==null?"":prefix));
        for (int i=0;i<files.length;i++)
            if (files[i].isDirectory())
                getInfos(files[i], infos, (prefix==null?"":prefix+"/")+files[i].getName());
    }
    
    public static void writeInfos(Vector infos, File file) {
        try {
            System.out.println("Writing "+file+" ...");
            TreeSet keys = new TreeSet();
            Vector headers = new Vector();
            headers.addElement(new CSVFile.CSVField(""));
            for (Enumeration e=infos.elements();e.hasMoreElements();) {
                Object[] o = (Object[])e.nextElement();
                String prefix = (String)o[0];
                Hashtable info = (Hashtable)o[1];
                keys.addAll(info.keySet());
                headers.addElement(new CSVFile.CSVField(prefix));
            }
            CSVFile csvFile = new CSVFile();
            csvFile.setHeader(headers);
            for (Iterator i=keys.iterator();i.hasNext();) {
                String key = (String)i.next();
                Vector line = new Vector();
                line.addElement(new CSVFile.CSVField(key));
                for (Enumeration e=infos.elements();e.hasMoreElements();) {
                    Object[] o = (Object[])e.nextElement();
                    String prefix = (String)o[0];
                    Hashtable info = (Hashtable)o[1];
                    String value = (String)info.get(key);
                    line.addElement(new CSVFile.CSVField(value==null?"":value));
                }
                csvFile.addLine(line);
            }
            csvFile.save(file);
        } catch (Exception e) {
            System.err.println("Error writing file "+file+", message: "+e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();
            File folder = new File(args[0]);
            Vector infos = new Vector();
            getInfos(folder, infos, null);
            if (!infos.isEmpty())
                writeInfos(infos,new File(folder,"info.csv"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
