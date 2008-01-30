package net.sf.cpsolver.exam.neighbours;

import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
/**
 * Extension of {@link SimpleNeighbour}. The only difference is that
 * the value ({@link SimpleNeighbour#value()}) is decreased by 1000 if the
 * selected variable has no current assignment.
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class ExamSimpleNeighbour extends SimpleNeighbour {
    private static Logger sLog = Logger.getLogger(ExamSimpleNeighbour.class);
    private static boolean sCheck = false;
    private double iValue = 0;
    private double iDx;
    private double[] iDxM;
    
    public ExamSimpleNeighbour(ExamPlacement placement) {
        super(placement.variable(),placement);
        iValue = placement.toDouble();
        if (placement.variable().getAssignment()!=null)
            iValue -= placement.variable().getAssignment().toDouble();
        else
            iValue -= 1000;
        if (sCheck) {
            iDx = placement.toDouble(); 
            if (placement.variable().getAssignment()!=null)
                iDx -= placement.variable().getAssignment().toDouble();
        }
    }
    
    public void assign(long iteration) {
        if (sCheck) {
            int before = getVariable().getModel().nrUnassignedVariables();
            double beforeVal = getVariable().getModel().getTotalValue();
            double[] beforeValM = ((ExamModel)getVariable().getModel()).getTotalMultiValue(); 
            String n = toString();
            getVariable().assign(iteration, getValue());
            int after = getVariable().getModel().nrUnassignedVariables();
            double afterVal = getVariable().getModel().getTotalValue();
            double[] afterValM = ((ExamModel)getVariable().getModel()).getTotalMultiValue();
            if (after>before) {
                sLog.error("-- assignment mischmatch (delta:"+(after-before)+")");
                sLog.error("  -- neighbour: "+n);
            }
            if (Math.abs(afterVal-beforeVal-iDx)>=0.0000001) {
                sLog.error("-- value mischmatch (delta:"+(afterVal-beforeVal)+")");
                sLog.error("  -- neighbour: "+n);
                sLog.error("  -- solution: "+toString(afterValM, beforeValM));
                sLog.error("  -- value:    "+toString(iDxM));
            }
        } else {
            getVariable().assign(iteration, getValue());
        }
    }
    
    
    protected static String toString(double[] x) {
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuffer s = new StringBuffer();
        for (int i=0;i<x.length;i++) {
            if (i>0) s.append(",");
            s.append(df.format(x[i]));
        }
        return "["+s.toString()+"]";
    }
    
    protected static String toString(double[] x, double[] y) {
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuffer s = new StringBuffer();
        for (int i=0;i<x.length;i++) {
            if (i>0) s.append(",");
            s.append(df.format(x[i]-y[i]));
        }
        return "["+s.toString()+"]";
    }

    public String toString() {
        return 
            getVariable().getAssignment()+
            " -> "+
            getValue().toString()+
            " / "+" (value:"+value()+")";
    }
    
    public double value() {
        return iValue;
    }
}
