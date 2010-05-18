package net.sf.cpsolver.exam.reports;

import java.util.Comparator;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;

/**
 * Compare two rooms by size. Either normal seating size or alternative sating
 * size is used, based on the given exam (see {@link Exam#hasAltSeating()}. <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class ExamRoomComparator implements Comparator<ExamRoomPlacement> {
    private boolean iAsc;
    private Exam iExam;

    /**
     * Constructor
     * 
     * @param exam
     *            exam for which rooms are to be compared
     */
    public ExamRoomComparator(Exam exam, boolean asc) {
        iExam = exam;
        iAsc = asc;
    }

    /**
     * Compare two rooms based on their normal/alternative seating size
     */
    public int compare(ExamRoomPlacement r1, ExamRoomPlacement r2) {
        int cmp = (iAsc ? 1 : -1)
                * Double.compare(r1.getSize(iExam.hasAltSeating()), r2.getSize(iExam.hasAltSeating()));
        if (cmp != 0)
            return cmp;
        return r1.getRoom().compareTo(r2.getRoom());
    }
}
