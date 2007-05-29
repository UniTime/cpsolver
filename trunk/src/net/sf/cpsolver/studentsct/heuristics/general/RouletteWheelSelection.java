package net.sf.cpsolver.studentsct.heuristics.general;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.ToolBox;

public class RouletteWheelSelection implements Enumeration {
    private Vector iAdepts = new Vector(), iPoints = new Vector();
    private double iTotalPoints = 0;
    private int iFirst = 0;
    
    public void add(Object adept, double points) {
        iAdepts.add(adept);
        iPoints.add(new Double(points));
        iTotalPoints+=points;
    }
    
    protected void swap(int idx1, int idx2) {
        Object a1 = iAdepts.elementAt(idx1);
        Object a2 = iAdepts.elementAt(idx2);
        iAdepts.setElementAt(a2, idx1);
        iAdepts.setElementAt(a1, idx2);
        Object p1 = iPoints.elementAt(idx1);
        Object p2 = iPoints.elementAt(idx2);
        iPoints.setElementAt(p2, idx1);
        iPoints.setElementAt(p1, idx2);
    }
    
    public boolean hasMoreElements() {
        return iFirst<iAdepts.size();
    }
    
    public Object nextElement() {
        if (!hasMoreElements()) return null;
        double rx = ToolBox.random()*iTotalPoints;
        
        int iIdx = iFirst; rx -= ((Double)iPoints.elementAt(iIdx)).doubleValue();
        while (rx>0 && iIdx+1<iAdepts.size()) {
            iIdx++;
            rx -= ((Double)iPoints.elementAt(iIdx)).doubleValue();
        }
        
        Object selectedObject = iAdepts.elementAt(iIdx);
        iTotalPoints -= ((Double)iPoints.elementAt(iIdx)).doubleValue();
        swap(iFirst, iIdx);
        iFirst++;
        
        return selectedObject;
    }
    
    public int size() {
        return iAdepts.size();
    }
}
