package net.sf.cpsolver.exam.model;

import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

public class ExamRoom extends Constraint implements Comparable {
    private ExamPlacement[] iTable;
    private boolean[] iAvailable;
    private String iId;
    private int iSize, iAltSize;
    private int iCoordX, iCoordY;
    private int iHashCode;
    
    public ExamRoom(ExamModel model, String id, int size, int altSize, int coordX, int coordY) {
        super();
        iAssignedVariables = null;
        iId = id;
        iHashCode = id.hashCode();
        iCoordX = coordX; iCoordY = coordY; 
        iSize = size; iAltSize = altSize;
        iTable = new ExamPlacement[model.getNrPeriods()];
        iAvailable = new boolean[model.getNrPeriods()];
        for (int i=0;i<iTable.length;i++) {
            iTable[i]=null;
            iAvailable[i]=true;
        }
    }
    
    public String getRoomId() { return iId; }
    public int getSize() { return iSize; }
    public int getAltSize() { return iAltSize; }
    public int getCoordX() { return iCoordX; }
    public int getCoordY() { return iCoordY; }
    public ExamPlacement getPlacement(ExamPeriod period) { return iTable[period.getIndex()]; }
    public boolean isAvailable(ExamPeriod period) { return iAvailable[period.getIndex()]; }
    public void setAvailable(int period, boolean available) { iAvailable[period]=available; }
    
    public void computeConflicts(Value value, Set conflicts) {
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return; 
        if (iTable[p.getPeriod().getIndex()]!=null && !iTable[p.getPeriod().getIndex()].variable().equals(value.variable()))
            conflicts.add(iTable[p.getPeriod().getIndex()]);
    }
    
    public boolean inConflict(Value value) {
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return false; 
        return iTable[p.getPeriod().getIndex()]!=null && !iTable[p.getPeriod().getIndex()].variable().equals(value.variable());
    }
    
    public boolean isConsistent(Value value1, Value value2) {
        ExamPlacement p1 = (ExamPlacement)value1;
        ExamPlacement p2 = (ExamPlacement)value2;
        return (p1.getPeriod()!=p2.getPeriod() || !p1.getRooms().contains(this) || !p2.getRooms().contains(this));
    }
    
    public void assigned(long iteration, Value value) {
        super.assigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return;
            iTable[p.getPeriod().getIndex()] = p;
    }
    
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return;
            iTable[p.getPeriod().getIndex()] = null;
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamRoom)) return false;
        ExamRoom r = (ExamRoom)o;
        return getRoomId().equals(r.getRoomId());
    }
    
    public int hashCode() {
        return iHashCode;
    }
    
    public String toString() {
        return getRoomId();
    }
    
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
}
