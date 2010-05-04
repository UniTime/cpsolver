package net.sf.cpsolver.ifs.util;

import java.io.PrintStream;

/** Prints current progres to {@link PrintStream}.
 * <br><br>
 * Example usage:<ul><code>
 * Progress.getInstance().addProgressListener(new ProgressWriter(System.out));<br>
 * </code></ul>
 * <br>
 * Example output:<ul><code>
 * Reading course.pl ...       :<br>
 * Reading altcourse.pl ...    :<br>
 * Reading room.pl ...         :<br>
 * Creating rooms ...          : ................................................<br>
 * Creating variables ...      : ................................................<br>
 * Reading students.pl ...     :<br>
 * Reading jenr.pl ...         :<br>
 * Creating jenrl constraints .: ................................................<br>
 * Reading add.pl ...          :<br>
 * Creating group constraints .: ................................................<br>
 * Creating initial assignment : ................................................<br>
 * Creating dept. spread constr: ................................................<br>
 * Input data loaded           : ................................................<br>
 * Initializing solver         :<br>
 * Searching for initial soluti: ................................................<br>
 * Improving found solution ...: ................................................<br>
 * Improving found solution ...: ................................................<br>
 * Improving found solution ...: ...................................
 * </code></ul>
 *
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
public class ProgressWriter implements ProgressListener {
    private PrintStream iTextOut = null;
    private static int TEXT_LENGTH = 28;
    private static int DOTS_LENGTH = 48;
    private int iPrintedDots = -1;

    public ProgressWriter(PrintStream out) {
        iTextOut = out;
    }
    
    public void statusChanged(String status) {
        //iTextOut.println("Status: "+status);
    }

    public void phaseChanged(String phase) {
        if (iPrintedDots>0) {
            while (iPrintedDots<DOTS_LENGTH) { iTextOut.print("."); iPrintedDots++; }
        }
        iTextOut.println();
        iTextOut.print(expand(phase,TEXT_LENGTH,' ',false)+": ");
        iPrintedDots = 0;
        iTextOut.flush();
    }
    
    public void progressChanged(long currentProgress, long maxProgress) {
        int dotsToPrint = (maxProgress==0?0:(int)((DOTS_LENGTH*currentProgress)/maxProgress));
        while (iPrintedDots<dotsToPrint) {
            iTextOut.print(".");
            iPrintedDots++;
        }
        iTextOut.flush();
    }

    public void progressSaved() {}
    public void progressRestored() {}    
    public void progressMessagePrinted(Progress.Message msg) {} 
    
    private static String expand(String source, int length, char ch, boolean beg) {
        StringBuffer sb = new StringBuffer(source==null?"":source.length()>length?(beg?source.substring(source.length()-length):source.substring(0,length)):source);
        while (sb.length()<length) {
            if (beg) sb.insert(0,ch); else sb.append(ch);
        }
        return sb.toString();
    }
}
