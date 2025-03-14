package org.cpsolver.ifs.util;

import java.io.PrintStream;

/**
 * Prints current progres to {@link PrintStream}. <br>
 * <br>
 * Example usage:
 * <pre>
 * <code>
 * Progress.getInstance().addProgressListener(new ProgressWriter(System.out));<br>
 * </code>
 * </pre>
 * <br>
 * Example output:
 * <pre>
 * <code>
 * Reading course.pl ...       :
 * Reading altcourse.pl ...    :
 * Reading room.pl ...         :
 * Creating rooms ...          : ................................................
 * Creating variables ...      : ................................................
 * Reading students.pl ...     :
 * Reading jenr.pl ...         :
 * Creating jenrl constraints .: ................................................
 * Reading add.pl ...          :
 * Creating group constraints .: ................................................
 * Creating initial assignment : ................................................
 * Creating dept. spread constr: ................................................
 * Input data loaded           : ................................................
 * Initializing solver         :
 * Searching for initial soluti: ................................................
 * Improving found solution ...: ................................................
 * Improving found solution ...: ................................................
 * Improving found solution ...: ...................................
 * </code>
 * </pre>
 * 
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
public class ProgressWriter implements ProgressListener {
    private PrintStream iTextOut = null;
    private static int TEXT_LENGTH = 28;
    private static int DOTS_LENGTH = 48;
    private int iPrintedDots = -1;

    public ProgressWriter(PrintStream out) {
        iTextOut = out;
    }

    @Override
    public void statusChanged(String status) {
        // iTextOut.println("Status: "+status);
    }

    @Override
    public void phaseChanged(String phase) {
        if (iPrintedDots > 0) {
            while (iPrintedDots < DOTS_LENGTH) {
                iTextOut.print(".");
                iPrintedDots++;
            }
        }
        iTextOut.println();
        iTextOut.print(expand(phase, TEXT_LENGTH, ' ', false) + ": ");
        iPrintedDots = 0;
        iTextOut.flush();
    }

    @Override
    public void progressChanged(long currentProgress, long maxProgress) {
        int dotsToPrint = (maxProgress == 0 ? 0 : (int) ((DOTS_LENGTH * currentProgress) / maxProgress));
        while (iPrintedDots < dotsToPrint) {
            iTextOut.print(".");
            iPrintedDots++;
        }
        iTextOut.flush();
    }

    @Override
    public void progressSaved() {
    }

    @Override
    public void progressRestored() {
    }

    @Override
    public void progressMessagePrinted(Progress.Message msg) {
    }

    private static String expand(String source, int length, char ch, boolean beg) {
        StringBuffer sb = new StringBuffer(source == null ? "" : source.length() > length ? (beg ? source
                .substring(source.length() - length) : source.substring(0, length)) : source);
        while (sb.length() < length) {
            if (beg)
                sb.insert(0, ch);
            else
                sb.append(ch);
        }
        return sb.toString();
    }
}
