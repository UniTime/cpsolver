package net.sf.cpsolver.exam.neighbours;

import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;

public class ExamSimpleNeighbour extends SimpleNeighbour {
    private static Logger sLog = Logger.getLogger(ExamSimpleNeighbour.class);
    private static boolean sCheck = true;
    public double iValue = 0;
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
            iDxM = placement.toDoubleArray();
            if (placement.variable().getAssignment()!=null) {
                iDx -= placement.variable().getAssignment().toDouble();
                double[] x = ((ExamPlacement)placement.variable().getAssignment()).toDoubleArray();
                for (int i=0;i<iDxM.length;i++) iDxM[i] -= x[i];
            }
        }
    }
    
    public void assign(long iteration) {
        if (sCheck) {
            int before = getVariable().getModel().unassignedVariables().size();
            double beforeVal = getVariable().getModel().getTotalValue();
            double[] beforeValM = ((ExamModel)getVariable().getModel()).getTotalMultiValue(); 
            String n = toString();
            getVariable().assign(iteration, getValue());
            int after = getVariable().getModel().unassignedVariables().size();
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
