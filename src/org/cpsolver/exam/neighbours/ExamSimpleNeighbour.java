package org.cpsolver.exam.neighbours;

import java.text.DecimalFormat;

import org.apache.logging.log4j.Logger;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.SimpleNeighbour;


/**
 * Extension of {@link SimpleNeighbour}. The only difference is that the value (
 * {@link SimpleNeighbour#value(Assignment)}) is decreased by 1000 if the selected
 * variable has no current assignment. <br>
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
public class ExamSimpleNeighbour extends SimpleNeighbour<Exam, ExamPlacement> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ExamSimpleNeighbour.class);
    private static boolean sCheck = false;
    private double iValue = 0;
    private double iDx;

    public ExamSimpleNeighbour(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
        super(placement.variable(), placement);
        iValue = placement.toDouble(assignment);
        ExamPlacement current = assignment.getValue(placement.variable()); 
        if (current != null)
            iValue -= current.toDouble(assignment);
        else
            iValue -= 1000;
        if (sCheck) {
            iDx = placement.toDouble(assignment);
            if (current != null)
                iDx -= current.toDouble(assignment);
        }
    }

    @Override
    public void assign(Assignment<Exam, ExamPlacement> assignment, long iteration) {
        if (sCheck) {
            double beforeVal = getVariable().getModel().getTotalValue(assignment);
            double[] beforeValM = ((ExamModel) getVariable().getModel()).getTotalMultiValue(assignment);
            String n = toString();
            assignment.assign(iteration, getValue());
            double afterVal = getVariable().getModel().getTotalValue(assignment);
            double[] afterValM = ((ExamModel) getVariable().getModel()).getTotalMultiValue(assignment);
            /*
             * int before = getVariable().getModel().nrUnassignedVariables();
             * int after = getVariable().getModel().nrUnassignedVariables(); if
             * (after>before) {
             * sLog.error("-- assignment mischmatch (delta:"+(after
             * -before)+")"); sLog.error("  -- neighbour: "+n); }
             */
            if (Math.abs(afterVal - beforeVal - iDx) >= 0.0000001) {
                sLog.error("-- value mischmatch (delta:" + (afterVal - beforeVal) + ", value:" + iDx + ")");
                sLog.error("  -- neighbour: " + n);
                sLog.error("  -- solution:  " + toString(afterValM, beforeValM));
            }
        } else {
            assignment.assign(iteration, getValue());
        }
    }

    protected static String toString(double[] x) {
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < x.length; i++) {
            if (i > 0)
                s.append(",");
            s.append(df.format(x[i]));
        }
        return "[" + s.toString() + "]";
    }

    protected static String toString(double[] x, double[] y) {
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < x.length; i++) {
            if (i > 0)
                s.append(",");
            s.append(df.format(x[i] - y[i]));
        }
        return "[" + s.toString() + "]";
    }

    @Override
    public String toString() {
        return getVariable() + " := " + getValue().toString() + " / " + " (value:" + value(null) + ")";
    }

    @Override
    public double value(Assignment<Exam, ExamPlacement> assignment) {
        return iValue;
    }
}
