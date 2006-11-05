package net.sf.cpsolver.ifs.util;

import java.util.*;

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
 * IFS 1.0 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
    private String iStatus="";
    private String iPhase="";
    private long iProgressMax=0;
    private long iProgressCurrent=0;
    private Vector iListeners = new FastVector(5);
    private Vector iSave = new FastVector(5);
    
    private static Progress sInstance = new Progress();

    private Progress() {}
    
    /** Progress instance */
    public static Progress getInstance() { return sInstance; }
    
    /** Current status */
    public String getStatus() { return iStatus; }
    /** Sets current status */
    public void setStatus(String status) { 
        if (!status.equals(iStatus)) {
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
        String status = (String)o[0];
        String phase = (String)o[1];
        long progressCurrent = ((Long)o[2]).longValue();
        long progressMax = ((Long)o[3]).longValue();
        fireProgressRestored();
        setStatus(status);
        setPhase(phase,progressMax);
        setProgress(progressCurrent);
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
}
