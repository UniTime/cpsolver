package net.sf.cpsolver.studentsct.heuristics;

import java.util.Vector;

import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.CourseRequest;

public class RandomizedBacktrackNeighbourSelection extends BacktrackNeighbourSelection {
    public int iMaxValues = 25;
    
    public RandomizedBacktrackNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iMaxValues = properties.getPropertyInt("Neighbour.MaxValues", iMaxValues);
    }
    
    protected Vector values(Variable variable) {
        if (iMaxValues>0 && variable instanceof CourseRequest) {
            return ((CourseRequest)variable).computeRandomEnrollments(iMaxValues);
        } 
        return variable.values();
    }
}
