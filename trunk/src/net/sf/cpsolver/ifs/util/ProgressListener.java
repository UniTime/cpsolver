package net.sf.cpsolver.ifs.util;

/**
 * Progress bar listener.
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
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
public interface ProgressListener {
    /** Progress status is changed 
     * @param status current status name
     */
    public void statusChanged(String status);
    /** Progress phase is changed 
     * @param phase current phase name
     */
    public void phaseChanged(String phase);
    /** Progress bar is changed
     * @param currentProgress current progress
     * @param maxProgress maximum progress in this phase
     */
    public void progressChanged(long currentProgress, long maxProgress);
    /** Progress is saved */
    public void progressSaved();
    /** Progress is restored */
    public void progressRestored();
    /** Progress message is printed */
    public void progressMessagePrinted(Progress.Message message);
}
