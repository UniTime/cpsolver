package net.sf.cpsolver.ifs.util;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import org.dom4j.Element;

/**
 * Progress bar.
 * <br><br>
 * Single instance class for recording the current state. It also allows recursive storing/restoring of the progress.
 *
 * <br><br>
 * Use:<ul><code>
 * Progress.getInstance().setStatus("Loading input data");<br>
 * Progress.getInstance().setPhase("Creating variables ...", nrVariables);<br>
 * for (int i=0;i<nrVariables;i++) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;//load variable here<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Progress.getInstance().incProgress();<br>
 * }<br>
 * Progress.getInstance().setPhase("Creating constraints ...", nrConstraints);<br>
 * for (int i=0;i<nrConstraints;i++) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;//load constraint here<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Progress.getInstance().incProgress();<br>
 * }<br>
 * Progress.getInstance().setStatus("Solving problem");<br>
 * ...<br>
 * </code></ul>
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
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
public class Progress {
	public static boolean sTraceEnabled = false; 
	private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Progress.class);
	public static SimpleDateFormat sDF = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
	public static final int MSGLEVEL_TRACE = 0;
    public static final int MSGLEVEL_DEBUG = 1;
    public static final int MSGLEVEL_PROGRESS = 2;
    public static final int MSGLEVEL_INFO = 3;
    public static final int MSGLEVEL_STAGE = 4;
    public static final int MSGLEVEL_WARN = 5;
    public static final int MSGLEVEL_ERROR = 6;
    public static final int MSGLEVEL_FATAL = 7;

    private String iStatus="";
    private String iPhase="";
    private long iProgressMax=0;
    private long iProgressCurrent=0;
    private Vector iListeners = new FastVector(5);
    private Vector iSave = new FastVector(5);
    private Vector iLog = new FastVector(1000,1000);
    
    private static Hashtable sInstances = new Hashtable();

    private Progress() {}
    
    /** Progress default instance */
    public static Progress getInstance() {
    	return getInstance("--DEFAULT--");
    }
    /** Progress instance */
    public static Progress getInstance(Object key) { 
    	Progress progress = (Progress)sInstances.get(key);
    	if (progress==null) {
    		progress = new Progress();
    		sInstances.put(key, progress);
    	}
    	return progress;
    }
    
    /** Change progress instance for the given key */
    public static void changeInstance(Object oldKey, Object newKey) {
    	removeInstance(newKey);
    	Progress progress = (Progress)sInstances.get(oldKey);
    	if (progress!=null) {
    		sInstances.remove(oldKey);
    		sInstances.put(newKey, progress);
    	}
    }
    
    /** Remove progress instance for the given key */
    public static void removeInstance(Object key) {
    	Progress progress = (Progress)sInstances.get(key);
    	if (progress!=null) {
    		progress.iListeners.clear();
    		sInstances.remove(key);
    	}
    }
    
    /** Current status */
    public String getStatus() { return iStatus; }
    /** Sets current status */
    public void setStatus(String status) { 
    	message(MSGLEVEL_STAGE, status);
        if (!status.equals(iStatus)) {
        	iPhase = ""; iProgressMax=0; iProgressCurrent=0;
            iStatus = status; fireStatusChanged(); 
        }
    }
    /** Current phase */
    public String getPhase() { return iPhase; }
    /** Sets current phase 
     * @param phase phase name
     * @param progressMax maximum of progress bar
     */
    public void setPhase(String phase, long progressMax) {
    	if (iSave.isEmpty() && !phase.equals(iPhase)) message(MSGLEVEL_PROGRESS,phase);
        iPhase = phase; iProgressMax=progressMax; iProgressCurrent=0; firePhaseChanged();
    }
    /** Sets current phase. Maximum of progress bar is set to 100.
     * @param phase phase name
     */
    public void setPhase(String phase) { setPhase(phase, 100); }
    /** Update progress bar. 
     * @param progress progress between 0 and progressMax
     */
    public void setProgress(long progress) { 
        if (iProgressCurrent!=progress) {
            iProgressCurrent = progress; fireProgressChanged();
        }
    }
    /** Current progress */
    public long getProgress() { return iProgressCurrent; }
    /** Maximum of current progress */
    public long getProgressMax() { return iProgressMax; }
    /** Increment current progress */
    public void incProgress() {
        iProgressCurrent++; 
        fireProgressChanged();
    }
    /** Adds progress listener */
    public void addProgressListener(ProgressListener listener) { iListeners.addElement(listener); }
    /** Remove progress listener */
    public void removeProgressListener(ProgressListener listener) { iListeners.removeElement(listener); }
    /** Remove all progress listeners */
    public void clearProgressListeners() { iListeners.clear(); }
    
    /** Save current progress to the heap memory */
    public synchronized void save() { 
        iSave.addElement(new Object[] { iStatus, iPhase, new Long(iProgressMax), new Long(iProgressCurrent) }); 
        fireProgressSaved();
    }
    /** Resore the progress from the heap memory */
    public synchronized void restore() { 
        if (iSave.isEmpty()) return;
        Object[] o = (Object[]) iSave.lastElement();
        iSave.removeElementAt(iSave.size()-1);
        iStatus = (String)o[0];
        iPhase = (String)o[1];
        iProgressMax = ((Long)o[2]).longValue();
        iProgressCurrent = ((Long)o[3]).longValue();
        fireProgressRestored();
    }
    /** Prints a message */
    public void message(int level, String message, Throwable t) {
    	Message m = new Message(level, message,t);
    	switch (level) {
    		case MSGLEVEL_TRACE : sLogger.debug("    -- "+message,t); break;
    		case MSGLEVEL_DEBUG : sLogger.debug("  -- "+message,t); break;
    		case MSGLEVEL_PROGRESS : sLogger.debug("["+message+"]",t); break;
    		case MSGLEVEL_INFO : sLogger.info(message,t); break;
    		case MSGLEVEL_STAGE : sLogger.info("["+message+"]",t); break;
    		case MSGLEVEL_WARN : sLogger.warn(message,t); break;
    		case MSGLEVEL_ERROR : sLogger.error(message,t); break;
    		case MSGLEVEL_FATAL : sLogger.fatal(message,t); break;
    	}
    	iLog.addElement(m);
    	fireMessagePrinted(m);
    }
    /** Prints a message */
    public void message(int level, String message) {
    	message(level, message,null);
    }
    /** Prints a trace message */
    public void trace(String message) {
    	if (!sTraceEnabled) return;
    	message(MSGLEVEL_TRACE, message);
    }
    /** Prints a debug message */
    public void debug(String message) {
    	message(MSGLEVEL_DEBUG, message);
    }
    /** Prints an info message */
    public void info(String message) {
    	message(MSGLEVEL_INFO, message);
    }
    /** Prints a warning message */
    public void warn(String message) {
    	message(MSGLEVEL_WARN, message);
    }
    /** Prints an error message */
    public void error(String message) {
    	message(MSGLEVEL_ERROR, message);
    }
    /** Prints a fatal message */
    public void fatal(String message) {
    	message(MSGLEVEL_FATAL, message);
    }
    /** Prints a trace message */
    public void trace(String message, Throwable e) {
    	if (!sTraceEnabled) return;
    	message(MSGLEVEL_TRACE, message, e);
    }
    /** Prints a debug message */
    public void debug(String message, Throwable e) {
    	message(MSGLEVEL_DEBUG, message, e);
    }
    /** Prints an info message */
    public void info(String message, Throwable e) {
    	message(MSGLEVEL_INFO, message, e);
    }
    /** Prints a warning message */
    public void warn(String message, Throwable e) {
    	message(MSGLEVEL_WARN, message, e);
    }
    /** Prints an error message */
    public void error(String message, Throwable e) {
    	message(MSGLEVEL_ERROR, message, e);
    }
    /** Prints a fatal message */
    public void fatal(String message, Throwable e) {
    	message(MSGLEVEL_FATAL, message, e);
    }
    /** Returns log (list of messages) */
    public Vector getLog() { return iLog; }
    /** Returns log (list of messages). Only messages with the given level or higher are included. */
    public String getLog(int level) {
    	StringBuffer sb = new StringBuffer(); 
    	for (Enumeration e=iLog.elements();e.hasMoreElements();) {
    		Message m = (Message)e.nextElement();
    		String s = m.toString(level);
    		if (s!=null) sb.append(s+"\n");
    	}
    	return sb.toString();
    }
    /** Returns log in HTML format */
    public String getHtmlLog(int level, boolean includeDate) {
    	StringBuffer sb = new StringBuffer(); 
    	for (Enumeration e=iLog.elements();e.hasMoreElements();) {
    		Message m = (Message)e.nextElement();
    		String s = m.toHtmlString(level, includeDate);
    		if (s!=null) sb.append(s+"<br>");
    	}
    	return sb.toString();
    }
    /** Returns log in HTML format (only messages with the given level or higher are included) */
    public String getHtmlLog(int level, boolean includeDate, String fromStage) {
    	StringBuffer sb = new StringBuffer(); 
    	for (Enumeration e=iLog.elements();e.hasMoreElements();) {
    		Message m = (Message)e.nextElement();
    		if (m.getLevel()==MSGLEVEL_STAGE && m.getMessage().equals(fromStage))
    			sb = new StringBuffer();
    		String s = m.toHtmlString(level, includeDate);
    		if (s!=null) sb.append(s+"<br>");
    	}
    	return sb.toString();
    }
    /** Clear the log */
    public void clear() {
    	iLog.clear();
    }

    private void fireStatusChanged() {
        for (Enumeration e=iListeners.elements();e.hasMoreElements();) {
            ProgressListener listener = (ProgressListener)e.nextElement();
            listener.statusChanged(iStatus);
        }
    }
    private void firePhaseChanged() {
        for (Enumeration e=iListeners.elements();e.hasMoreElements();) {
            ProgressListener listener = (ProgressListener)e.nextElement();
            listener.phaseChanged(iPhase);
        }
    }
    private void fireProgressChanged() {
        for (Enumeration e=iListeners.elements();e.hasMoreElements();) {
            ProgressListener listener = (ProgressListener)e.nextElement();
            listener.progressChanged(iProgressCurrent, iProgressMax);
        }
    }
    private void fireProgressSaved() {
        for (Enumeration e=iListeners.elements();e.hasMoreElements();) {
            ProgressListener listener = (ProgressListener)e.nextElement();
            listener.progressSaved();
        }
    }
    private void fireProgressRestored() {
        for (Enumeration e=iListeners.elements();e.hasMoreElements();) {
            ProgressListener listener = (ProgressListener)e.nextElement();
            listener.progressRestored();
        }
    }

    private void fireMessagePrinted(Message message) {
        for (Enumeration e=iListeners.elements();e.hasMoreElements();) {
            ProgressListener listener = (ProgressListener)e.nextElement();
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
    		iLevel = level; iMessage = message; iDate = new Date();
    		if (e!=null) {
    			StackTraceElement trace[] = e.getStackTrace();
    			if (trace!=null) {
    				iStakTrace = new String[trace.length+1];
    				iStakTrace[0]=e.getClass().getName()+": "+e.getMessage();
    				for (int i=0;i<trace.length;i++)
    					iStakTrace[i+1]=trace[i].getClassName()+"."+trace[i].getMethodName()+(trace[i].getFileName()==null?"":"("+trace[i].getFileName()+(trace[i].getLineNumber()>=0?":"+trace[i].getLineNumber():"")+")");
    			}
    		}
    	}
    	/** Creates message out of XML element */
    	public Message(Element element) {
    		iLevel = Integer.parseInt(element.attributeValue("level","0"));
    		iMessage = element.attributeValue("msg");
    		iDate = new Date(Long.parseLong(element.attributeValue("date","0")));
    		List tr = element.elements("trace");
    		if (tr!=null && !tr.isEmpty()) {
    			iStakTrace = new String[tr.size()];
    			for (int i=0;i<tr.size();i++)
    				iStakTrace[i] = ((Element)tr.get(i)).getText();
    		}
    	}
    	/** Message */
    	public String getMessage() { return iMessage; }
    	/** Debug level */
    	public int getLevel() { return iLevel; }
    	/** Time stamp */
    	public Date getDate() { return iDate; }
    	/** Tracelog */
    	private String getTraceLog() {
    		if (iStakTrace==null) return "";
    		StringBuffer ret = new StringBuffer("\n"+iStakTrace[0]); 
    		for (int i=1;i<iStakTrace.length;i++)
    			ret.append("\n    at "+iStakTrace[i]);
    		return ret.toString();
    	}
    	/** Tracelog as HTML */
    	private String getHtmlTraceLog() {
    		if (iStakTrace==null) return "";
    		StringBuffer ret = new StringBuffer("<BR>"+iStakTrace[0]); 
    		for (int i=1;i<iStakTrace.length;i++)
    			ret.append("<BR>&nbsp;&nbsp;&nbsp;&nbsp;at "+iStakTrace[i]);
    		return ret.toString();
    	}
    	/** String representation of the message (null if the message level is below the given level) */
    	public String toString(int level) {
    		if (iLevel<level) return null;
    		switch (iLevel) {
    			case MSGLEVEL_TRACE 	: return sDF.format(iDate)+"    -- "+iMessage+getTraceLog();
    			case MSGLEVEL_DEBUG 	: return sDF.format(iDate)+"  -- "+iMessage+getTraceLog();
    			case MSGLEVEL_PROGRESS 	: return sDF.format(iDate)+" ["+iMessage+"]"+getTraceLog();
    			case MSGLEVEL_INFO  	: return sDF.format(iDate)+" "+iMessage+getTraceLog();
    			case MSGLEVEL_STAGE  	: return sDF.format(iDate)+" >>> "+iMessage+" <<<"+getTraceLog();
    			case MSGLEVEL_WARN  	: return sDF.format(iDate)+" WARNING: "+iMessage+getTraceLog();
    			case MSGLEVEL_ERROR 	: return sDF.format(iDate)+" ERROR: "+iMessage+getTraceLog();
    			case MSGLEVEL_FATAL 	: return sDF.format(iDate)+" >>>FATAL: "+iMessage+" <<<"+getTraceLog();
    		}
    		return null;
    	}
    	/** String representation of the message */
    	public String toString() { return toString(MSGLEVEL_TRACE); }
    	/** HTML representation of the message */
    	public String toHtmlString(int level, boolean includeDate) {
    		if (iLevel<level) return null;
    		switch (iLevel) {
    			case MSGLEVEL_TRACE 	: return (includeDate?sDF.format(iDate):"")+" &nbsp;&nbsp;&nbsp;&nbsp;-- "+iMessage+getHtmlTraceLog();
    			case MSGLEVEL_DEBUG 	: return (includeDate?sDF.format(iDate):"")+" &nbsp;&nbsp;-- "+iMessage+getHtmlTraceLog();
    			case MSGLEVEL_PROGRESS  : return (includeDate?sDF.format(iDate):"")+" "+iMessage+getHtmlTraceLog();
    			case MSGLEVEL_INFO  	: return (includeDate?sDF.format(iDate):"")+" "+iMessage+getHtmlTraceLog();
    			case MSGLEVEL_STAGE		: return "<br>"+(includeDate?sDF.format(iDate):"")+" <span style='font-weight:bold;'>"+iMessage+"</span>"+getHtmlTraceLog();
    			case MSGLEVEL_WARN  	: return (includeDate?sDF.format(iDate):"")+" <span style='color:orange;font-weight:bold;'>WARNING:</span> "+iMessage+getHtmlTraceLog();
    			case MSGLEVEL_ERROR 	: return (includeDate?sDF.format(iDate):"")+" <span style='color:red;font-weight:bold;'>ERROR:</span> "+iMessage+getHtmlTraceLog();
    			case MSGLEVEL_FATAL 	: return (includeDate?sDF.format(iDate):"")+" <span style='color:red;font-weight:bold;'>&gt;&gt;&gt;FATAL: "+iMessage+" &lt;&lt;&lt;</span>"+getHtmlTraceLog();
    		}
    		return null;
    	}
    	/** HTML representation of the message (null if the message level is below the given level)*/
    	public String toHtmlString(int level) { return toHtmlString(level, true);}
    	/** HTML representation of the message*/
    	public String toHtmlString(boolean includeDate) { return toHtmlString(MSGLEVEL_TRACE, includeDate); }
    	/** HTML representation of the message*/
    	public String toHtmlString() { return toHtmlString(MSGLEVEL_TRACE, true); }
    	/** Saves message into an XML element */
    	public void save(Element element) {
    		element.addAttribute("level", String.valueOf(iLevel));
    		element.addAttribute("msg", iMessage);
    		element.addAttribute("date", String.valueOf(iDate.getTime()));
    		if (iStakTrace!=null) {
    			for (int i=0;i<iStakTrace.length;i++)
    				element.addElement("trace").setText(iStakTrace[i]);
    		}
    	}
    }
    /** Saves the message log into the given XML element */
    public void save(Element root) {
    	Element log = root.addElement("log");
    	for (Enumeration e=iLog.elements();e.hasMoreElements();) {
    		Message m = (Message)e.nextElement();
    		m.save(log.addElement("msg"));
    	}
    }
    /** Restores the message log from the given XML element */
    public void load(Element root, boolean clear) {
    	if (clear) iLog.clear();
    	Element log = root.element("log");
    	if (log!=null) {
    		for (Iterator i=log.elementIterator("msg");i.hasNext();)
    			iLog.addElement(new Message((Element)i.next()));
    	}
    }
}
