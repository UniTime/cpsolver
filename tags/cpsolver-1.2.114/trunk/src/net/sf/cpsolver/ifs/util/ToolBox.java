package net.sf.cpsolver.ifs.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Several auxiliary static methods.
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
public class ToolBox {
    private static long sSeed = System.currentTimeMillis();
    private static Random sRandom = new Random(sSeed);

    /** Returns random number (int) from the set 0 .. limit - 1 */
    public static int random(int limit) {
        return (int) (random() * limit);
    }

    /** Returns random element from the given set of elements */
    public static <E> E random(Collection<E> set) {
        switch (set == null ? 0 : set.size()) {
            case 0:
                return null;
            case 1:
                return set.iterator().next();
            case 2:
                Iterator<E> i = set.iterator();
                if (sRandom.nextBoolean()) i.next();
                return i.next();
            default:
                List<E> v = (set instanceof List<?> ? (List<E>) set : new ArrayList<E>(set));
                return v.get(random(v.size()));
        }
    }

    /**
     * Returns a randomly generated subset of the given set
     * 
     * @param set
     *            set
     * @param part
     *            probability of selection of an element into the resultant
     *            subset
     */
    public static <E> Collection<E> subSet(Collection<E> set, double part) {
        return subSet(set, part, 1);
    }

    /** Swaps two elements in the list */
    private static <E> void swap(List<E> list, int first, int second) {
        E o = list.get(first);
        list.set(first, list.get(second));
        list.set(second, o);
    }

    /**
     * Returns a randomly generated subset of the given set
     * 
     * @param set
     *            set
     * @param part
     *            probability of selection of an element into the resultant
     *            subset
     * @param minSize
     *            minimal size of the returned subset
     */
    public static <E> Collection<E> subSet(Collection<E> set, double part, int minSize) {
        if (set.size() <= minSize || part >= 1.0)
            return set;
        ArrayList<E> subSet = new ArrayList<E>(set);
        int size = set.size();
        int numberToSelect = Math.max(minSize, (int) (part * set.size()));
        for (int idx = 0; idx < numberToSelect; idx++) {
            swap(subSet, idx, idx + (int) (random() * (size - idx)));
        }
        return subSet.subList(0, numberToSelect);
    }

    /** Trim a string to have given length */
    public static String trim(String s, int length) {
        if (s.length() > length)
            return s.substring(0, length);
        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < length)
            sb.append(" ");
        return sb.toString();
    }

    /** Multiline representation of a colection */
    public static String col2string(Collection<?> col, int tab) {
        StringBuffer tabsb = new StringBuffer();
        while (tabsb.length() < 2 * tab)
            tabsb.append("  ");
        StringBuffer sb = new StringBuffer("[\n");
        for (Iterator<?> i = col.iterator(); i.hasNext();) {
            sb.append(tabsb + "  " + i.next() + (i.hasNext() ? "," : "") + "\n");
        }
        sb.append(tabsb + "]");
        return sb.toString();
    }

    /** Multiline representation of a dictionary */
    public static <K, V> String dict2string(Map<K, V> dict, int tab) {
        StringBuffer tabsb = new StringBuffer();
        while (tabsb.length() < 2 * tab)
            tabsb.append("  ");
        StringBuffer sb = new StringBuffer("[\n");
        TreeSet<K> keys = new TreeSet<K>(dict.keySet());
        for (K key : keys) {
            V value = dict.get(key);
            sb.append(tabsb + "  " + key + ": " + value + "\n");
        }
        sb.append(tabsb + "]");
        return sb.toString();
    }

    /**
     * Root mean square
     * 
     * @param n
     *            number of tests
     * @param x
     *            total value of all tests
     * @param x2
     *            total value^2 of all tests
     */
    public static double rms(int n, double x, double x2) {
        double var = x2 / n;
        double mean = x / n;
        return Math.sqrt(Math.abs(var - mean * mean));
    }

    /** Merge source with target */
    public static <E> void merge(List<E> target, Collection<E> source) {
        for (E o : source) {
            if (!target.contains(o))
                target.add(o);
        }
    }

    /** Returns intersection of two collections */
    public static <E> List<E> intersect(Collection<E> source1, Collection<E> source2) {
        List<E> target = new ArrayList<E>();
        for (E o : source1) {
            if (!source2.contains(o))
                target.add(o);
        }
        return target;
    }

    /**
     * Sets seeds for {@link ToolBox#getRandom()} and {@link ToolBox#random()}
     * methods.
     */
    public static void setSeed(long seed) {
        sSeed = seed;
        sRandom = new Random(sSeed);
    }

    /** Gets current seed */
    public static long getSeed() {
        return sSeed;
    }

    /** Gets random number generator */
    public static Random getRandom() {
        return sRandom;
    }

    /** Generates random double number */
    public static double random() {
        return sRandom.nextDouble();
    }

    /** Configurates log4j loging */
    public static void configureLogging() {
        Properties props = new Properties();
        props.setProperty("log4j.rootLogger", "DEBUG, A1");
        props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.A1.layout.ConversionPattern", "%-5p %c{2}: %m%n");
        props.setProperty("log4j.logger.net", "INFO");
        props.setProperty("log4j.logger.net.sf.cpsolver", "DEBUG");
        props.setProperty("log4j.logger.org", "INFO");
        PropertyConfigurator.configure(props);
    }

    /**
     * Configurates log4j loging
     * 
     * @param logDir
     *            output folder
     * @param properties
     *            some other log4j properties
     */
    public static String configureLogging(String logDir, Properties properties) {
        return configureLogging(logDir, properties, false);
    }

    public static String configureLogging(String logDir, Properties properties, boolean timeInFileName) {
        return configureLogging(logDir, properties, timeInFileName, true);
    }

    /**
     * Configurates log4j loging
     * 
     * @param logDir
     *            output folder
     * @param properties
     *            some other log4j properties
     * @param timeInFileName
     *            if true log file is named debug_yyyy-MM-dd_(HH.mm.ss).log, it
     *            is named debug.log otherwise
     */
    public static String configureLogging(String logDir, Properties properties, boolean timeInFileName,
            boolean includeSystemOuts) {
        String time = new java.text.SimpleDateFormat("yyyy-MM-dd_(HH.mm.ss)", java.util.Locale.US).format(new Date());
        (new File(logDir)).mkdirs();
        String fileName = logDir + File.separator + (timeInFileName ? "debug_" + time : "debug") + ".log";
        Properties props = (properties != null ? properties : new Properties());
        if (!props.containsKey("log4j.rootLogger")) {
            props.setProperty("log4j.rootLogger", "debug, LogFile");
            if (timeInFileName)
                props.setProperty("log4j.appender.LogFile", "org.apache.log4j.FileAppender");
            else {
                props.setProperty("log4j.appender.LogFile", "org.apache.log4j.DailyRollingFileAppender");
                props.setProperty("log4j.appender.LogFile.DatePattern", "'.'yyyy-MM-dd");
            }
            props.setProperty("log4j.appender.LogFile.File", fileName);
            props.setProperty("log4j.appender.LogFile.layout", "org.apache.log4j.PatternLayout");
            props.setProperty("log4j.appender.LogFile.layout.ConversionPattern",
                    "%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n");
        }
        PropertyConfigurator.configure(props);
        Logger log = Logger.getRootLogger();
        log.info("-----------------------------------------------------------------------");
        log.info("IFS debug file");
        log.info("");
        log.info("Created: " + new Date());
        log.info("");
        log.info("System info:");
        log.info("System:      " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
                + System.getProperty("os.arch"));
        log.info("CPU:         " + System.getProperty("sun.cpu.isalist") + " endian:"
                + System.getProperty("sun.cpu.endian") + " encoding:" + System.getProperty("sun.io.unicode.encoding"));
        log.info("Java:        " + System.getProperty("java.vendor") + ", " + System.getProperty("java.runtime.name")
                + " " + System.getProperty("java.runtime.version", System.getProperty("java.version")));
        log.info("User:        " + System.getProperty("user.name"));
        log.info("Timezone:    " + System.getProperty("user.timezone"));
        log.info("Working dir: " + System.getProperty("user.dir"));
        log.info("Classpath:   " + System.getProperty("java.class.path"));
        log.info("");
        if (includeSystemOuts) {
            System.setErr(new PrintStream(new LogOutputStream(System.err, Logger.getLogger("STDERR"), Level.ERROR)));
            System.setOut(new PrintStream(new LogOutputStream(System.out, Logger.getLogger("STDOUT"), Level.DEBUG)));
        }
        return fileName;
    }

    /**
     * Loads data properties. If there is INCLUDE property available, it is
     * interpreted as semi-colon separated list of porperty files which should
     * be also loaded (works recursively).
     * 
     */
    public static DataProperties loadProperties(File propertyFile) {
        FileInputStream is = null;
        try {
            DataProperties ret = new DataProperties();
            is = new FileInputStream(propertyFile);
            ret.load(is);
            is.close();
            is = null;
            if (ret.getProperty("INCLUDE") != null) {

                StringTokenizer stk = new StringTokenizer(ret.getProperty("INCLUDE"), ";");
                while (stk.hasMoreTokens()) {
                    String aFile = stk.nextToken();
                    System.out.println("  Loading included file '" + aFile + "' ... ");
                    if ((new File(aFile)).exists())
                        is = new FileInputStream(aFile);
                    if ((new File(propertyFile.getParent() + File.separator + aFile)).exists())
                        is = new FileInputStream(propertyFile.getParent() + File.separator + aFile);
                    if (is == null)
                        System.err.println("Unable to find include file '" + aFile + "'.");
                    ret.load(is);
                    is.close();
                    is = null;
                }
                ret.remove("INCLUDE");
            }
            return ret;
        } catch (Exception e) {
            System.err.println("Unable to load property file " + propertyFile);
            e.printStackTrace();
            return new DataProperties();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
        }
    }

    public static boolean equals(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    private static class LogOutputStream extends OutputStream {
        private Logger iLogger = null;
        private Level iLevel = null;
        private OutputStream iOldOutputStream;
        private ByteArrayOutputStream iOut = new ByteArrayOutputStream();

        public LogOutputStream(OutputStream oldOutputStream, Logger logger, Level level) {
            iLogger = logger;
            iLevel = level;
            iOldOutputStream = oldOutputStream;
        }

        @Override
        public void write(int b) throws IOException {
            iOldOutputStream.write(b);
            if (b == '\r')
                return;
            if (b == '\n') {
                iOut.flush();
                iLogger.log(iLevel, new String(iOut.toByteArray()));
                iOut.reset();
            } else
                iOut.write(b);
        }
    }
    
    public static <E> List<E> toList(E... obj) {
        List<E> ret = new ArrayList<E>(obj == null ? 0 : obj.length);
        if (obj != null)
            for (E e: obj)
                ret.add(e);
        return ret;
    }
}
