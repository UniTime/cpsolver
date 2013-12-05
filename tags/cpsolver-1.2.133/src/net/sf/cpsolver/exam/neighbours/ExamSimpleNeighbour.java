package net.sf.cpsolver.exam.neighbours;

import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;

/**
 * Extension of {@link SimpleNeighbour}. The only difference is that the value (
 * {@link SimpleNeighbour#value()}) is decreased by 1000 if the selected
 * variable has no current assignment. <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
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
    private static Logger sLog = Logger.getLogger(ExamSimpleNeighbour.class);
    private static boolean sCheck = false;
    private double iValue = 0;
    private double iDx;

    public ExamSimpleNeighbour(ExamPlacement placement) {
        super(placement.variable(), placement);
        iValue = placement.toDouble();
        if (placement.variable().getAssignment() != null)
            iValue -= placement.variable().getAssignment().toDouble();
        else
            iValue -= 1000;
        if (sCheck) {
            iDx = placement.toDouble();
            if (placement.variable().getAssignment() != null)
                iDx -= placement.variable().getAssignment().toDouble();
        }
    }

    @Override
    public void assign(long iteration) {
        if (sCheck) {
            double beforeVal = getVariable().getModel().getTotalValue();
            double[] beforeValM = ((ExamModel) getVariable().getModel()).getTotalMultiValue();
            String n = toString();
            getVariable().assign(iteration, getValue());
            double afterVal = getVariable().getModel().getTotalValue();
            double[] afterValM = ((ExamModel) getVariable().getModel()).getTotalMultiValue();
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
            getVariable().assign(iteration, getValue());
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
        return getVariable().getAssignment() + " -> " + getValue().toString() + " / " + " (value:" + value() + ")";
    }

    @Override
    public double value() {
        return iValue;
    }
}
