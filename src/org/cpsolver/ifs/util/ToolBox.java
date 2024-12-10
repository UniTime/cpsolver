package org.cpsolver.ifs.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Several auxiliary static methods.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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

    /** Returns random number (int) from the set 0 .. limit - 1 
     * @param limit a limit 
     * @return a random number between 0 and limit - 1
     **/
    public static int random(int limit) {
        return (int) (random() * limit);
    }

    /** Returns random element from the given set of elements 
     * @param set collection of objects
     * @param <E> some type
     * @return randomly selected object
     **/
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
                int index = random(set.size());
                if (set instanceof List<?>) return ((List<E>)set).get(index);
                Iterator<E> it = set.iterator();
                for (int j = 0; j < index; j++) it.next();
                return it.next();
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
     * @param <E> some type
     * @return randomly selected subset
     */
    public static <E> Collection<E> subSet(Collection<E> set, double part) {
        return subSet(set, part, 1);
    }

    /** Swaps two elements in the list*/
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
     * @param <E> some type
     * @return randomly selected subset
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

    /** Trim a string to have given length
     * @param s a string to trim
     * @param length a length to trim to
     * @return trimmed and padded string of the given length
     **/
    public static String trim(String s, int length) {
        if (s.length() > length)
            return s.substring(0, length);
        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < length)
            sb.append(" ");
        return sb.toString();
    }

    /** Multiline representation of a collection 
     * @param col a collection
     * @param tab tab size
     * @return string representation
     **/
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

    /** Multiline representation of a dictionary
     * @param dict a map
     * @param tab tab size
     * @param <K> a key class
     * @param <V> a value class
     * @return string representation
     */
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
     * @return root mean square
     */
    public static double rms(int n, double x, double x2) {
        double var = x2 / n;
        double mean = x / n;
        return Math.sqrt(Math.abs(var - mean * mean));
    }

    /** Merge source with target 
     * @param target target list
     * @param source source list
     * @param <E> some object
     **/
    public static <E> void merge(List<E> target, Collection<E> source) {
        for (E o : source) {
            if (!target.contains(o))
                target.add(o);
        }
    }

    /** Returns intersection of two collections
     * @param source1 first collection
     * @param source2 second collection
     * @param <E> some object
     * @return intersection
     */
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
     * @param seed random seed
     */
    public static void setSeed(long seed) {
        sSeed = seed;
        sRandom = new Random(sSeed);
    }

    /** Gets current seed 
     * @return random seed
     **/
    public static long getSeed() {
        return sSeed;
    }

    /** Gets random number generator 
     * @return random number generator
     **/
    public static Random getRandom() {
        return sRandom;
    }

    /** Generates random double number 
     * @return random number
     **/
    public static double random() {
        return sRandom.nextDouble();
    }

    /** Configurates log4j loging */
    public static void configureLogging() {
        if ("true".equalsIgnoreCase(System.getProperty("org.cpsolver.debug", "false"))) {
            Configurator.setLevel("org.cpsolver", Level.DEBUG);
        }
    }
    
    /**
     * Setup log4j logging
     * 
     * @param logFile  log file
     * @param debug true if debug messages should be logged (use -Ddebug=true to enable debug message)
     */
    public static void setupLogging(File logFile, boolean debug) {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        
        AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
        console.addAttribute("target", "SYSTEM_OUT");
        LayoutComponentBuilder consoleLayout = builder.newLayout("PatternLayout");
        consoleLayout.addAttribute("pattern", "%-5p %c{2}: %m%n");
        console.add(consoleLayout);
        builder.add(console);
        
        RootLoggerComponentBuilder root = builder.newRootLogger(Level.INFO);
        root.add(builder.newAppenderRef("stdout"));
        builder.add(root);
        
        if (logFile != null) {
            AppenderComponentBuilder file = builder.newAppender("log", "File");
            file.addAttribute("fileName", logFile.getPath());
            
            LayoutComponentBuilder filePattern = builder.newLayout("PatternLayout");
            filePattern.addAttribute("pattern", "%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n");
            file.add(filePattern);
            builder.add(file);
            
            LoggerComponentBuilder logger = builder.newLogger("org.cpsolver", debug ? Level.DEBUG : Level.INFO);
            logger.add(builder.newAppenderRef("log"));
            logger.addAttribute("additivity", true);
            builder.add(logger);
        }
        Configurator.reconfigure(builder.build());
    }

    /**
     * Configure log4j logging
     * 
     * @param logDir
     *            output folder
     * @param properties
     *            some other log4j properties
     * @return name of the log file
     */
    public static String configureLogging(String logDir, Properties properties) {
        return configureLogging(logDir, properties, false);
    }

    /**
     * Configure log4j logging
     * 
     * @param logDir
     *            output folder
     * @param properties
     *            some other log4j properties
     * @param timeInFileName 
     *            if true log file is named debug_yyyy-MM-dd_(HH.mm.ss).log, it
     *            is named debug.log otherwise
     * @return name of the log file
     */
    public static String configureLogging(String logDir, Properties properties, boolean timeInFileName) {
        return configureLogging(logDir, properties, timeInFileName, true);
    }

    /**
     * Configure log4j logging
     * 
     * @param logDir
     *            output folder
     * @param properties
     *            some other log4j properties
     * @param timeInFileName
     *            if true log file is named debug_yyyy-MM-dd_(HH.mm.ss).log, it
     *            is named debug.log otherwise
     * @param includeSystemOuts include system out and error in the log 
     * @return name of the log file
     */
    public static String configureLogging(String logDir, Properties properties, boolean timeInFileName, boolean includeSystemOuts) {
        (new File(logDir)).mkdirs();

        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        
        AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
        LayoutComponentBuilder consoleLayout = builder.newLayout("PatternLayout");
        consoleLayout.addAttribute("pattern", "[%t] %m%n");
        console.add(consoleLayout);
        builder.add(console);
        
        AppenderComponentBuilder file = null;
        String fileName = null;
        if (timeInFileName) {
            file = builder.newAppender("log", "File");
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd_(HH.mm.ss)", java.util.Locale.US).format(new Date());
            fileName = logDir + File.separator + "debug_" + time + ".log";
            file.addAttribute("fileName", fileName);
        } else {
            file = builder.newAppender("log", "RollingFile");
            fileName = logDir + File.separator + "debug.log";
            file.addAttribute("fileName", fileName);
            file.addAttribute("filePattern", logDir + File.separator + "debug-%d{MM-dd-yy}-%i.log.gz");
            ComponentBuilder<?> triggeringPolicies = builder.newComponent("Policies")
                    .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
                    .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
            file.addComponent(triggeringPolicies);
        }
        LayoutComponentBuilder filePattern = builder.newLayout("PatternLayout");
        filePattern.addAttribute("pattern", "%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n");
        file.add(filePattern);
        builder.add(file);
        
        RootLoggerComponentBuilder root = builder.newRootLogger("true".equals(System.getProperty("org.cpsolver.debug", "false")) ? Level.DEBUG : Level.INFO);
        root.add(builder.newAppenderRef("log"));
        builder.add(root);
        
        LoggerComponentBuilder info = builder.newLogger("org.cpsolver", Level.INFO);
        info.add(builder.newAppenderRef("stdout"));
        info.addAttribute("additivity", true);
        builder.add(info);
        
        if (properties != null) {
            for (Map.Entry<Object, Object> e: properties.entrySet()) {
                String property = (String)e.getKey();
                String value = (String)e.getValue();
                if (property.startsWith("log4j.logger.")) {
                    if (value.indexOf(',') < 0) {
                        LoggerComponentBuilder logger = builder.newLogger(property.substring("log4j.logger.".length()), Level.getLevel(value));
                        builder.add(logger);
                    } else {
                        String level = value.substring(0, value.indexOf(','));
                        String appender = value.substring(value.indexOf(',') + 1);
                        LoggerComponentBuilder logger = builder.newLogger(property.substring("log4j.logger.".length()), Level.getLevel(level));
                        for (String a: appender.split(","))
                            logger.add(builder.newAppenderRef(a));
                        builder.add(logger);
                    }
                }
            }
        }
        Configurator.reconfigure(builder.build());
        
        Logger log = LogManager.getRootLogger();
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
            System.setErr(new PrintStream(new LogOutputStream(System.err, org.apache.logging.log4j.LogManager.getLogger("STDERR"), Level.ERROR)));
            System.setOut(new PrintStream(new LogOutputStream(System.out, org.apache.logging.log4j.LogManager.getLogger("STDOUT"), Level.DEBUG)));
        }
        return fileName;
    }

    /**
     * Loads data properties. If there is INCLUDE property available, it is
     * interpreted as semi-colon separated list of porperty files which should
     * be also loaded (works recursively).
     * @param propertyFile a file to read
     * @return solver configuration
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
                    else if ((new File(propertyFile.getParent() + File.separator + aFile)).exists())
                        is = new FileInputStream(propertyFile.getParent() + File.separator + aFile);
                    else
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
    
    /**
     * Convert array of elements into a list
     * @param obj array of elements
     * @return list of elements
     */
    public static <E> List<E> toList(@SuppressWarnings("unchecked") E... obj) {
        List<E> ret = new ArrayList<E>(obj == null ? 0 : obj.length);
        if (obj != null)
            for (E e: obj)
                ret.add(e);
        return ret;
    }
    
    /**
     * Compute number of K-tuples of N elements
     * @param N number of elements (e.g., number of room locations in a domain)
     * @param K size of a tuple (e.g., number of rooms a class needs)
     * @return number of different K-tupples of N elements
     */
    public static long binomial(int N, int K) {
        long ret = 1;
        for (int k = 0; k < K; k++)
            ret = ret * (N-k) / (k+1);
        return ret;
    }
    
    /**
     * Create random sample (m-tuple) of given list of elements
     * @param items list of elements
     * @param m size of a tuple
     * @return random subset of the list of size m
     */
    public static <E> Set<E> sample(List<E> items, int m) {
        HashSet<E> res = new HashSet<E>(m);
        int n = items.size();
        for(int i = n - m; i < n; i++){
            int pos = getRandom().nextInt(i+1);
            E item = items.get(pos);
            if (res.contains(item))
                res.add(items.get(i));
            else
                res.add(item);
        }
        return res;
    }
    
    /**
     * Generate given permutation
     * @param items list of elements
     * @param m size of a tuple (permutation)
     * @param id position of the permutation in the list of all permutations of m-tuples of the given list of elements
     * @return given subset of the list of size m
     */
    public static <E> List<E> permutation(final List<E> items, int m, long id) {
        List<E> ret = new ArrayList<E>();
        int n = items.size();
        int p = -1;
        for (int i = 0; i < m - 1; i++) {
            p ++;
            for (long r = binomial(n - p - 1, m - i - 1); r <= id; r = binomial(n - p - 1, m - i - 1))  {
                id -= r; p ++;
            }
            ret.add(items.get(p));
        }
        ret.add(items.get(p + 1 + (int)id));
        return ret;
    }
    
    /**
     * Generate a list of samples of the given list
     * @param items list of elements
     * @param m size of a sample
     * @param count number of samples
     * @return list of samples (m-tuples) of the given items 
     */
    public static <E> Enumeration<Collection<E>> sample(final List<E> items, final int m, final int count) {
        final long limit = binomial(items.size(), m);
        if (count >= limit) return permutations(items, m);
        return new Enumeration<Collection<E>>() {
            int el = 0; 
            Set<Long> used = new HashSet<Long>();

            @Override
            public boolean hasMoreElements() {
                return el < count && el < limit;
            }
            
            @Override
            public Set<E> nextElement() {
                int n = items.size();
                for (;;) {
                    HashSet<E> res = new HashSet<E>(m);
                    TreeSet<Integer> ids = new TreeSet<Integer>();
                    for(int i = n - m; i < n; i++){
                        int pos = getRandom().nextInt(i+1);
                        E item = items.get(pos);
                        if (res.contains(item)) {
                            res.add(items.get(i));
                            ids.add(i);
                        } else {
                            res.add(item);
                            ids.add(pos);
                        }
                    }
                    long fp = 0;
                    for (Integer id: ids) {
                        fp = (n * fp) + id;
                    }
                    if (used.add(fp)) {
                        el ++;
                        return res;
                    }
                }
            }
        };
    }
    
    /**
     * Generate a list of all permutations of size m of the given list
     * @param items list of elements
     * @param m size of a permutation
     * @return list of all permutations (m-tuples) of the given items 
     */
    public static <E> Enumeration<Collection<E>> permutations(final List<E> items, final int m) {
        return new Enumeration<Collection<E>>() {
            int n = items.size();
            int[] p = null;
            
            @Override
            public boolean hasMoreElements() {
                return p == null || p[0] < n - m;
            }
            
            @Override
            public Collection<E> nextElement() {
                if (p == null) {
                    p = new int[m];
                    for (int i = 0; i < m; i++)
                        p[i] = i;
                } else {
                    for (int i = m - 1; i >= 0; i--) {
                        p[i] = p[i] + 1;
                        for (int j = i + 1; j < m; j++)
                            p[j] = p[j - 1] + 1;
                        if (i == 0 || p[i] <= n - (m - i)) break;
                    }
                }
                List<E> ret = new ArrayList<E>();
                for (int i = 0; i < m; i++)
                    ret.add(items.get(p[i]));
                return ret;
            }
        };
    }
    
    /**
     * Generate a list of random samples combined of the given two lists
     * @param items1 list of first elements
     * @param m1 size of a first sample
     * @param items2 list of second elements
     * @param m2 size of a second sample
     * @param count number of samples
     * @return list of samples where each sample contains m1 elements of the first list and m2 elements of the second list 
     */
    private static <E> Enumeration<Collection<E>> sample(final List<E> items1, final int m1, final List<E> items2, final int m2, final int count) {
        final long c1 = binomial(items1.size(), m1);
        final long c2 = binomial(items2.size(), m2);
        final long limit = c1 * c2;
        if (limit <= 10l * count && 10l * count < Integer.MAX_VALUE) {
            return new Enumeration<Collection<E>>() {
                Set<Integer> used = new HashSet<Integer>();

                @Override
                public boolean hasMoreElements() {
                    return used.size() < count && used.size() < limit;
                }
                
                @Override
                public Collection<E> nextElement() {
                    int id;
                    do {
                        id = getRandom().nextInt((int)limit);
                    } while (!used.add(id));
                    List<E> res = new ArrayList<E>(m1 + m2);
                    if (m1 > 0)
                        res.addAll(permutation(items1, m1, id / c2));
                    if (m2 > 0)
                        res.addAll(permutation(items2, m2, id % c2));
                    return res;
                }
            };            
        } else {
            return new Enumeration<Collection<E>>() {
                int n1 = items1.size(), n2 = items2.size();
                int el = 0; 
                Set<Long> used = new HashSet<Long>();

                @Override
                public boolean hasMoreElements() {
                    return (count < 0 || el < count) && el < limit;
                }
                
                @Override
                public Collection<E> nextElement() {
                    for (;;) {
                        HashSet<E> res = new HashSet<E>(m1 + m2);
                        TreeSet<Integer> ids1 = new TreeSet<Integer>();
                        if (m1 == n1) {
                            // Special case 1: first permutation contains all elements
                            res.addAll(items1);
                        } else if (m1 + 1 == n1) {
                            // Special case 2: first permutation contains all elements but one
                            int pos = getRandom().nextInt(n1);
                            for (int i = 0; i < n1; i++)
                                if (i != pos) res.add(items1.get(i));
                            ids1.add(pos);
                        } else {
                            for(int i = n1 - m1; i < n1; i++){
                                int pos = getRandom().nextInt(i+1);
                                E item = items1.get(pos);
                                if (res.contains(item)) {
                                    res.add(items1.get(i));
                                    ids1.add(i);
                                } else {
                                    res.add(item);
                                    ids1.add(pos);
                                }
                            }
                        }
                        TreeSet<Integer> ids2 = new TreeSet<Integer>();
                        if (m2 == n2) {
                            // Special case 1: second permutation contains all elements
                            res.addAll(items2);
                        } else if (m2 + 1 == n2) {
                            // Special case 2: second permutation contains all elements but one
                            int pos = getRandom().nextInt(n2);
                            for (int i = 0; i < n2; i++)
                                if (i != pos) res.add(items2.get(i));
                            ids2.add(pos);
                        } else {
                            for(int i = n2 - m2; i < n2; i++){
                                int pos = getRandom().nextInt(i+1);
                                E item = items2.get(pos);
                                if (res.contains(item)) {
                                    res.add(items2.get(i));
                                    ids2.add(n1 + i);
                                } else {
                                    res.add(item);
                                    ids2.add(n1 + pos);
                                }
                            }
                        }
                        long fp = 0;
                        for (Integer id: ids1) fp = (n1 * fp) + id;
                        for (Integer id: ids2) fp = (n2 * fp) + id;
                        if (used.add(fp)) {
                            el ++;
                            return res;
                        }
                    }
                }
            };
        }
    }

    /**
     * Generate a list of random samples combined of the given two lists
     * @param preferred list of preferred elements
     * @param additional list of additional elements
     * @param m size of a sample
     * @param count number of samples
     * @return list of samples of size m, preferring as many elements of the preferred list as possible 
     */
    public static <E> Enumeration<Collection<E>> sample(final List<E> preferred, final List<E> additional, final int m, final int count) {
        return new Enumeration<Collection<E>>() {
            long limit = (count < 0 ? binomial(preferred.size() + additional.size(), m) : Math.min(count, binomial(preferred.size() + additional.size(), m)));
            int k = Math.min(m, preferred.size());
            int el = 0;
            Enumeration<Collection<E>> e = sample(preferred, k, additional, m - k, count);
            
            @Override
            public boolean hasMoreElements() {
                return el < limit;
            }
            
            @Override
            public Collection<E> nextElement() {
                if (e.hasMoreElements()) {
                    el ++;
                    return e.nextElement();
                }
                k = Math.max(Math.min(k - 1, preferred.size() - 1), 0);
                e = sample(preferred, k, additional, m - k, count);
                el ++;
                return e.nextElement();
            }
        };
    }
}
