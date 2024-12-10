package org.cpsolver.exam.reports;

import java.util.Comparator;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamRoomPlacement;


/**
 * Compare two rooms by size. Either normal seating size or alternative sating
 * size is used, based on the given exam (see {@link Exam#hasAltSeating()}. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamRoomComparator implements Comparator<ExamRoomPlacement> {
    private boolean iAsc;
    private Exam iExam;

    /**
     * Constructor
     * 
     * @param exam
     *            exam for which rooms are to be compared
     * @param asc room order
     */
    public ExamRoomComparator(Exam exam, boolean asc) {
        iExam = exam;
        iAsc = asc;
    }

    /**
     * Compare two rooms based on their normal/alternative seating size
     */
    @Override
    public int compare(ExamRoomPlacement r1, ExamRoomPlacement r2) {
        int cmp = (iAsc ? 1 : -1)
                * Double.compare(r1.getSize(iExam.hasAltSeating()), r2.getSize(iExam.hasAltSeating()));
        if (cmp != 0)
            return cmp;
        return r1.getRoom().compareTo(r2.getRoom());
    }
}
