package org.cpsolver.ifs.util;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

/**
 * Progress bar. <br>
 * <br>
 * Single instance class for recording the current state. It also allows
 * recursive storing/restoring of the progress.
 * 
 * <br>
 * <br>
 * Use:
 * <pre>
 * <code>
 * Progress.getInstance().setStatus("Loading input data");
 * Progress.getInstance().setPhase("Creating variables ...", nrVariables);
 * for (int i=0;i&lt;nrVariables;i++) {
 * &nbsp;&nbsp;&nbsp;&nbsp;//load variable here
 * &nbsp;&nbsp;&nbsp;&nbsp;Progress.getInstance().incProgress();
 * }
 * Progress.getInstance().setPhase("Creating constraints ...", nrConstraints);
 * for (int i=0;i&lt;nrConstraints;i++) {
 * &nbsp;&nbsp;&nbsp;&nbsp;//load constraint here
 * &nbsp;&nbsp;&nbsp;&nbsp;Progress.getInstance().incProgress();
 * }
 * Progress.getInstance().setStatus("Solving problem");
 * ...
 * </code>
 * </pre>
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
public class Progress {
    public static boolean sTraceEnabled = false;
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(Progress.class);
    public static SimpleDateFormat sDF = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
    public static final int MSGLEVEL_TRACE = 0;
    public static final int MSGLEVEL_DEBUG = 1;
    public static final int MSGLEVEL_PROGRESS = 2;
    public static final int MSGLEVEL_INFO = 3;
    public static final int MSGLEVEL_STAGE = 4;
    public static final int MSGLEVEL_WARN = 5;
    public static final int MSGLEVEL_ERROR = 6;
    public static final int MSGLEVEL_FATAL = 7;

    private String iStatus = "";
    private String iPhase = "";
    private long iProgressMax = 0;
    private long iProgressCurrent = 0;
    private List<ProgressListener> iListeners = new ArrayList<ProgressListener>(5);
    private List<Object[]> iSave = new ArrayList<Object[]>(5);
    private List<Message> iLog = new ArrayList<Message>(1000);
    private boolean iDisposed = false;

    private static HashMap<Object, Progress> sInstances = new HashMap<Object, Progress>();

    private Progress() {
    }

    /** Progress default instance 
     * @return progress instance
     **/
    public static Progress getInstance() {
        return getInstance("--DEFAULT--");
    }

    /** Progress instance
     * @param key an object (typically a problem model) for which the progress is to be returned
     * @return progress instance
     **/
    public static Progress getInstance(Object key) {
        Progress progress = sInstances.get(key);
        if (progress == null) {
            progress = new Progress();
            sInstances.put(key, progress);
        }
        return progress;
    }

    /** Change progress instance for the given key 
     * @param oldKey old instance
     * @param newKey new instance
     **/
    public static void changeInstance(Object oldKey, Object newKey) {
        removeInstance(newKey);
        Progress progress = sInstances.get(oldKey);
        if (progress != null) {
            sInstances.remove(oldKey);
            sInstances.put(newKey, progress);
        }
    }

    /** Remove progress instance for the given key 
     * @param key old instance
     **/
    public static void removeInstance(Object key) {
        Progress progress = sInstances.get(key);
        if (progress != null) {
            progress.iListeners.clear();
            progress.iDisposed = true;
            sInstances.remove(key);
        }
    }

    /** Current status 
     * @return current status
     **/
    public String getStatus() {
        return iStatus;
    }

    /** Sets current status 
     * @param status current status
     **/
    public void setStatus(String status) {
        message(MSGLEVEL_STAGE, status);
        if (!status.equals(iStatus)) {
            iPhase = "";
            iProgressMax = 0;
            iProgressCurrent = 0;
            iStatus = status;
            fireStatusChanged();
        }
    }

    /** Current phase 
     * @return current phase
     **/
    public String getPhase() {
        return iPhase;
    }

    /**
     * Sets current phase
     * 
     * @param phase
     *            phase name
     * @param progressMax
     *            maximum of progress bar
     */
    public void setPhase(String phase, long progressMax) {
        if (iSave.isEmpty() && !phase.equals(iPhase))
            message(MSGLEVEL_PROGRESS, phase);
        iPhase = phase;
        iProgressMax = progressMax;
        iProgressCurrent = 0;
        firePhaseChanged();
    }

    /**
     * Sets current phase. Maximum of progress bar is set to 100.
     * 
     * @param phase
     *            phase name
     */
    public void setPhase(String phase) {
        setPhase(phase, 100);
    }

    /**
     * Update progress bar.
     * 
     * @param progress
     *            progress between 0 and progressMax
     */
    public void setProgress(long progress) {
        if (iProgressCurrent != progress) {
            iProgressCurrent = progress;
            fireProgressChanged();
        }
    }

    /** Current progress 
     * @return current progress
     **/
    public long getProgress() {
        return iProgressCurrent;
    }

    /** Maximum of current progress
     * @return current progress maximum
     **/
    public long getProgressMax() {
        return iProgressMax;
    }

    /** Increment current progress */
    public void incProgress() {
        iProgressCurrent++;
        fireProgressChanged();
    }

    /** Adds progress listener 
     * @param listener a progress listener
     **/
    public void addProgressListener(ProgressListener listener) {
        iListeners.add(listener);
    }

    /** Remove progress listener
     * @param listener a progress listener
     **/
    public void removeProgressListener(ProgressListener listener) {
        iListeners.remove(listener);
    }

    /** Remove all progress listeners */
    public void clearProgressListeners() {
        iListeners.clear();
    }

    /** Save current progress to the heap memory */
    public synchronized void save() {
        iSave.add(new Object[] { iStatus, iPhase, Long.valueOf(iProgressMax), Long.valueOf(iProgressCurrent) });
        fireProgressSaved();
    }

    /** Resore the progress from the heap memory */
    public synchronized void restore() {
        if (iSave.isEmpty())
            return;
        Object[] o = iSave.get(iSave.size() - 1);
        iSave.remove(iSave.size() - 1);
        iStatus = (String) o[0];
        iPhase = (String) o[1];
        iProgressMax = ((Long) o[2]).longValue();
        iProgressCurrent = ((Long) o[3]).longValue();
        fireProgressRestored();
    }

    /** Prints a message 
     * @param level logging level
     * @param message message to log
     * @param t an exception, if any
     **/
    public void message(int level, String message, Throwable t) {
        if (iDisposed) throw new RuntimeException("This solver is killed.");
        Message m = new Message(level, message, t);
        switch (level) {
            case MSGLEVEL_TRACE:
                sLogger.debug("    -- " + message, t);
                break;
            case MSGLEVEL_DEBUG:
                sLogger.debug("  -- " + message, t);
                break;
            case MSGLEVEL_PROGRESS:
                sLogger.debug("[" + message + "]", t);
                break;
            case MSGLEVEL_INFO:
                sLogger.info(message, t);
                break;
            case MSGLEVEL_STAGE:
                sLogger.info("[" + message + "]", t);
                break;
            case MSGLEVEL_WARN:
                sLogger.warn(message, t);
                break;
            case MSGLEVEL_ERROR:
                sLogger.error(message, t);
                break;
            case MSGLEVEL_FATAL:
                sLogger.fatal(message, t);
                break;
        }
        synchronized (iLog) {
            iLog.add(m);
        }
        fireMessagePrinted(m);
    }

    /** Prints a message 
     * @param level logging level
     * @param message message to log
     **/
    public void message(int level, String message) {
        message(level, message, null);
    }

    /** Prints a trace message 
     * @param message trace message
     **/
    public void trace(String message) {
        if (!sTraceEnabled)
            return;
        message(MSGLEVEL_TRACE, message);
    }

    /** Prints a debug message
     * @param message debug message
     **/
    public void debug(String message) {
        message(MSGLEVEL_DEBUG, message);
    }

    /** Prints an info message
     * @param message info message
     **/
    public void info(String message) {
        message(MSGLEVEL_INFO, message);
    }

    /** Prints a warning message
     * @param message warning message
     **/
    public void warn(String message) {
        message(MSGLEVEL_WARN, message);
    }

    /** Prints an error message
     * @param message error message
     **/
    public void error(String message) {
        message(MSGLEVEL_ERROR, message);
    }

    /** Prints a fatal message
     * @param message fatal message
     **/
    public void fatal(String message) {
        message(MSGLEVEL_FATAL, message);
    }

    /** Prints a trace message
     * @param message trace message
     * @param e an exception, if any
     **/
    public void trace(String message, Throwable e) {
        if (!sTraceEnabled)
            return;
        message(MSGLEVEL_TRACE, message, e);
    }

    /** Prints a debug message
     * @param message debug message
     * @param e an exception, if any
     **/
    public void debug(String message, Throwable e) {
        message(MSGLEVEL_DEBUG, message, e);
    }

    /** Prints an info message
     * @param message info message
     * @param e an exception, if any
     **/
    public void info(String message, Throwable e) {
        message(MSGLEVEL_INFO, message, e);
    }

    /** Prints a warning message
    * @param message warning message
    * @param e an exception, if any
    **/
    public void warn(String message, Throwable e) {
        message(MSGLEVEL_WARN, message, e);
    }

    /** Prints an error message
     * @param message error message
     * @param e an exception, if any
     **/
    public void error(String message, Throwable e) {
        message(MSGLEVEL_ERROR, message, e);
    }

    /** Prints a fatal message
     * @param message fatal message
     * @param e an exception, if any
     **/
    public void fatal(String message, Throwable e) {
        message(MSGLEVEL_FATAL, message, e);
    }

    /** Returns log (list of messages) 
     * @return list of logged messages
     **/
    public List<Message> getLog() {
        return iLog;
    }

    /**
     * Returns log (list of messages). Only messages with the given level or
     * higher are included.
     * @param level minimum level
     * @return list of messages
     */
    public String getLog(int level) {
        StringBuffer sb = new StringBuffer();
        synchronized (iLog) {
            for (Message m : iLog) {
                String s = m.toString(level);
                if (s != null)
                    sb.append(s + "\n");
            }
        }
        return sb.toString();
    }

    /** Returns log in HTML format
     * @param level minimum level
     * @param includeDate include message date and time in the result
     * @return html list of messages
     */
    public String getHtmlLog(int level, boolean includeDate) {
        StringBuffer sb = new StringBuffer();
        synchronized (iLog) {
            for (Message m : iLog) {
                String s = m.toHtmlString(level, includeDate);
                if (s != null)
                    sb.append(s + "<br>");
            }
        }
        return sb.toString();
    }

    /**
     * Returns log in HTML format (only messages with the given level or higher
     * are included)
     * @param level minimum level
     * @param includeDate include message date and time in the result
     * @param fromStage last stage from which the log should begin
     * @return html list of messages
     */
    public String getHtmlLog(int level, boolean includeDate, String fromStage) {
        StringBuffer sb = new StringBuffer();
        synchronized (iLog) {
            for (Message m : iLog) {
                if (m.getLevel() == MSGLEVEL_STAGE && m.getMessage().equals(fromStage))
                    sb = new StringBuffer();
                String s = m.toHtmlString(level, includeDate);
                if (s != null)
                    sb.append(s + "<br>");
            }
        }
        return sb.toString();
    }

    /** Clear the log */
    public void clear() {
        synchronized (iLog) {
            iLog.clear();
        }
    }

    private void fireStatusChanged() {
        for (ProgressListener listener : iListeners) {
            listener.statusChanged(iStatus);
        }
    }

    private void firePhaseChanged() {
        for (ProgressListener listener : iListeners) {
            listener.phaseChanged(iPhase);
        }
    }

    private void fireProgressChanged() {
        for (ProgressListener listener : iListeners) {
            listener.progressChanged(iProgressCurrent, iProgressMax);
        }
    }

    private void fireProgressSaved() {
        for (ProgressListener listener : iListeners) {
            listener.progressSaved();
        }
    }

    private void fireProgressRestored() {
        for (ProgressListener listener : iListeners) {
            listener.progressRestored();
        }
    }

    private void fireMessagePrinted(Message message) {
        for (ProgressListener listener : iListeners) {
            listener.progressMessagePrinted(message);
        }
    }

    /** Log nessage */
    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private int iLevel = 0;
        private String iMessage;
        private Date iDate = null;
        private String[] iStakTrace = null;

        private Message(int level, String message, Throwable e) {
            iLevel = level;
            iMessage = message;
            iDate = new Date();
            if (e != null) {
                StackTraceElement trace[] = e.getStackTrace();
                if (trace != null) {
                    iStakTrace = new String[trace.length + 1];
                    iStakTrace[0] = e.getClass().getName() + ": " + e.getMessage();
                    for (int i = 0; i < trace.length; i++)
                        iStakTrace[i + 1] = trace[i].getClassName()
                                + "."
                                + trace[i].getMethodName()
                                + (trace[i].getFileName() == null ? "" : "(" + trace[i].getFileName()
                                        + (trace[i].getLineNumber() >= 0 ? ":" + trace[i].getLineNumber() : "") + ")");
                }
            }
        }

        /** Creates message out of XML element 
         * @param element XML element with the message
         **/
        public Message(Element element) {
            iLevel = Integer.parseInt(element.attributeValue("level", "0"));
            iMessage = element.attributeValue("msg");
            iDate = new Date(Long.parseLong(element.attributeValue("date", "0")));
            java.util.List<?> tr = element.elements("trace");
            if (tr != null && !tr.isEmpty()) {
                iStakTrace = new String[tr.size()];
                for (int i = 0; i < tr.size(); i++)
                    iStakTrace[i] = ((Element) tr.get(i)).getText();
            }
        }

        /** Message 
         * @return message text
         **/
        public String getMessage() {
            return iMessage;
        }

        /** Debug level 
         * @return logging level
         **/
        public int getLevel() {
            return iLevel;
        }

        /** Time stamp 
         * @return message date and time
         **/
        public Date getDate() {
            return iDate;
        }

        /** Tracelog */
        private String getTraceLog() {
            if (iStakTrace == null)
                return "";
            StringBuffer ret = new StringBuffer("\n" + iStakTrace[0]);
            for (int i = 1; i < iStakTrace.length; i++)
                ret.append("\n    at " + iStakTrace[i]);
            return ret.toString();
        }

        /** Tracelog as HTML */
        private String getHtmlTraceLog() {
            if (iStakTrace == null)
                return "";
            StringBuffer ret = new StringBuffer("<BR>" + iStakTrace[0]);
            for (int i = 1; i < iStakTrace.length; i++)
                ret.append("<BR>&nbsp;&nbsp;&nbsp;&nbsp;at " + iStakTrace[i]);
            return ret.toString();
        }
        
        /** Tracelog */
        public String[] getTrace() {
            return iStakTrace;
        }

        /**
         * String representation of the message (null if the message level is
         * below the given level)
         * @param level minimum level
         * @return message log as string
         */
        public String toString(int level) {
            if (iLevel < level)
                return null;
            switch (iLevel) {
                case MSGLEVEL_TRACE:
                    return sDF.format(iDate) + "    -- " + iMessage + getTraceLog();
                case MSGLEVEL_DEBUG:
                    return sDF.format(iDate) + "  -- " + iMessage + getTraceLog();
                case MSGLEVEL_PROGRESS:
                    return sDF.format(iDate) + " [" + iMessage + "]" + getTraceLog();
                case MSGLEVEL_INFO:
                    return sDF.format(iDate) + " " + iMessage + getTraceLog();
                case MSGLEVEL_STAGE:
                    return sDF.format(iDate) + " >>> " + iMessage + " <<<" + getTraceLog();
                case MSGLEVEL_WARN:
                    return sDF.format(iDate) + " WARNING: " + iMessage + getTraceLog();
                case MSGLEVEL_ERROR:
                    return sDF.format(iDate) + " ERROR: " + iMessage + getTraceLog();
                case MSGLEVEL_FATAL:
                    return sDF.format(iDate) + " >>>FATAL: " + iMessage + " <<<" + getTraceLog();
            }
            return null;
        }

        /** String representation of the message */
        @Override
        public String toString() {
            return toString(MSGLEVEL_TRACE);
        }

        /** HTML representation of the message
         * @param level level minimum level
         * @param includeDate true if message date and time is to be included in the log
         * @return message log as HTML
         */
        public String toHtmlString(int level, boolean includeDate) {
            if (iLevel < level)
                return null;
            switch (iLevel) {
                case MSGLEVEL_TRACE:
                    return (includeDate ? sDF.format(iDate) : "") + " &nbsp;&nbsp;&nbsp;&nbsp;-- " + iMessage
                            + getHtmlTraceLog();
                case MSGLEVEL_DEBUG:
                    return (includeDate ? sDF.format(iDate) : "") + " &nbsp;&nbsp;-- " + iMessage + getHtmlTraceLog();
                case MSGLEVEL_PROGRESS:
                    return (includeDate ? sDF.format(iDate) : "") + " " + iMessage + getHtmlTraceLog();
                case MSGLEVEL_INFO:
                    return (includeDate ? sDF.format(iDate) : "") + " " + iMessage + getHtmlTraceLog();
                case MSGLEVEL_STAGE:
                    return "<br>" + (includeDate ? sDF.format(iDate) : "") + " <span style='font-weight:bold;'>"
                            + iMessage + "</span>" + getHtmlTraceLog();
                case MSGLEVEL_WARN:
                    return (includeDate ? sDF.format(iDate) : "")
                            + " <span style='color:orange;font-weight:bold;'>WARNING:</span> " + iMessage
                            + getHtmlTraceLog();
                case MSGLEVEL_ERROR:
                    return (includeDate ? sDF.format(iDate) : "")
                            + " <span style='color:red;font-weight:bold;'>ERROR:</span> " + iMessage
                            + getHtmlTraceLog();
                case MSGLEVEL_FATAL:
                    return (includeDate ? sDF.format(iDate) : "")
                            + " <span style='color:red;font-weight:bold;'>&gt;&gt;&gt;FATAL: " + iMessage
                            + " &lt;&lt;&lt;</span>" + getHtmlTraceLog();
            }
            return null;
        }

        /**
         * HTML representation of the message (null if the message level is
         * below the given level)
         * @param level level minimum level
         * @return message log as HTML
         */
        public String toHtmlString(int level) {
            return toHtmlString(level, true);
        }

        /** HTML representation of the message
         * @param includeDate true if message date and time is to be included in the log
         * @return message log as HTML
         */
        public String toHtmlString(boolean includeDate) {
            return toHtmlString(MSGLEVEL_TRACE, includeDate);
        }

        /** HTML representation of the message
         * @return message log as HTML
         */
        public String toHtmlString() {
            return toHtmlString(MSGLEVEL_TRACE, true);
        }

        /** Saves message into an XML element
         * @param element an XML element to which the message is to be saved (as attributes)
         **/
        public void save(Element element) {
            element.addAttribute("level", String.valueOf(iLevel));
            element.addAttribute("msg", iMessage);
            element.addAttribute("date", String.valueOf(iDate.getTime()));
            if (iStakTrace != null) {
                for (int i = 0; i < iStakTrace.length; i++)
                    element.addElement("trace").setText(iStakTrace[i]);
            }
        }
    }

    /** Saves the message log into the given XML element 
     * @param root XML root
     **/
    public void save(Element root) {
        Element log = root.addElement("log");
        synchronized (iLog) {
            for (Message m : iLog) {
                m.save(log.addElement("msg"));
            }
        }
    }

    /** Restores the message log from the given XML element 
     * @param root XML root
     * @param clear clear the log first
     **/
    public void load(Element root, boolean clear) {
        synchronized (iLog) {
            if (clear)
                iLog.clear();
            Element log = root.element("log");
            if (log != null) {
                for (Iterator<?> i = log.elementIterator("msg"); i.hasNext();)
                    iLog.add(new Message((Element) i.next()));
            }
        }
    }
}
