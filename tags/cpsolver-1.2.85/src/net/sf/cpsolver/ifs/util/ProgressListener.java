package net.sf.cpsolver.ifs.util;

/**
 * Progress bar listener.
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
public interface ProgressListener {
    /**
     * Progress status is changed
     * 
     * @param status
     *            current status name
     */
    public void statusChanged(String status);

    /**
     * Progress phase is changed
     * 
     * @param phase
     *            current phase name
     */
    public void phaseChanged(String phase);

    /**
     * Progress bar is changed
     * 
     * @param currentProgress
     *            current progress
     * @param maxProgress
     *            maximum progress in this phase
     */
    public void progressChanged(long currentProgress, long maxProgress);

    /** Progress is saved */
    public void progressSaved();

    /** Progress is restored */
    public void progressRestored();

    /** Progress message is printed */
    public void progressMessagePrinted(Progress.Message message);
}
