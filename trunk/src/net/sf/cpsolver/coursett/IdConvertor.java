package net.sf.cpsolver.coursett;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Conversion of ids to sequential numbers. This class is used by
 * {@link TimetableXMLSaver} to anonymise benchmark data sets. Conversion file
 * can be provided by IdConvertor.File system property (e.g.
 * -DIdConvertor.File=.\idconf.xml). <br>
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class IdConvertor {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(IdConvertor.class);
    private static IdConvertor sInstance = null;
    private HashMap<String, HashMap<String, String>> iConversion = new HashMap<String, HashMap<String, String>>();
    private String iFile = null;

    /**
     * Constructor -- use {@link IdConvertor#getInstance} to get an instance of
     * this class.
     */
    protected IdConvertor(String file) {
        iFile = file;
        load();
    }

    /** Get an instance of IdConvertor class. */
    public static IdConvertor getInstance() {
        if (sInstance == null)
            sInstance = new IdConvertor(null);
        return sInstance;
    }

    /** Convert id of given type. */
    public String convert(String type, String id) {
        synchronized (iConversion) {
            HashMap<String, String> conversion = iConversion.get(type);
            if (conversion == null) {
                conversion = new HashMap<String, String>();
                iConversion.put(type, conversion);
            }
            String newId = conversion.get(id);
            if (newId == null) {
                newId = String.valueOf(conversion.size() + 1);
                conversion.put(id, newId);
            }
            return newId;
        }
    }

    /**
     * Save id conversion file. Name of the file needs to be provided by system
     * property IdConvertor.File
     */
    public void save() {
        (new File(iFile)).getParentFile().mkdirs();
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("id-convertor");
        synchronized (iConversion) {
            for (Map.Entry<String, HashMap<String, String>> entry : iConversion.entrySet()) {
                String type = entry.getKey();
                HashMap<String, String> conversion = entry.getValue();
                Element convEl = root.addElement(type);
                for (Map.Entry<String, String> idConv : conversion.entrySet()) {
                    convEl.addElement("conv").addAttribute("old", idConv.getKey()).addAttribute("new",
                            idConv.getValue());
                }
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(iFile);
            (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(document);
            fos.flush();
            fos.close();
            fos = null;
        } catch (Exception e) {
            sLogger.error("Unable to save id conversions, reason: " + e.getMessage(), e);
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Load id conversion file. Name of the file needs to be provided by system
     * property IdConvertor.File
     */
    public void load() {
        try {
            if (iFile == null)
                iFile = System.getProperty("IdConvertor.File", "idconv.xml");
            if (iFile == null || !(new File(iFile)).exists())
                return;
            Document document = (new SAXReader()).read(iFile);
            Element root = document.getRootElement();
            synchronized (iConversion) {
                iConversion.clear();
                for (Iterator<?> i = root.elementIterator(); i.hasNext();) {
                    Element convEl = (Element) i.next();
                    HashMap<String, String> conversion = new HashMap<String, String>();
                    iConversion.put(convEl.getName(), conversion);
                    for (Iterator<?> j = convEl.elementIterator("conv"); j.hasNext();) {
                        Element e = (Element) j.next();
                        conversion.put(e.attributeValue("old"), e.attributeValue("new"));
                    }
                }
            }
        } catch (Exception e) {
            sLogger.error("Unable to load id conversions, reason: " + e.getMessage(), e);
        }
    }
}
