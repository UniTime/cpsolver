package org.cpsolver.ifs.assignment;

import java.util.Arrays;

import org.cpsolver.ifs.assignment.context.InheritedAssignmentContextHolder;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;

public class InheritedAssignmentArray<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentArray<V, T> implements InheritedAssignment<V, T> {
    private Assignment<V, T> iParent;
    private long iVersion = -1;
    private int iIndex = -1;

    public InheritedAssignmentArray(Solution<V, T> parent, int index) {
        super(new InheritedAssignmentContextHolder<V, T>(index, parent.getIteration()));
        iIndex = index;
        iAssignments = Arrays.copyOf(((AssignmentArray<V, T>)parent.getAssignment()).iAssignments, parent.getModel().variables().size());
        iIteration = Arrays.copyOf(((AssignmentArray<V, T>)parent.getAssignment()).iIteration, parent.getModel().variables().size());
    }
    
    @Override
    public int getIndex() {
        return iIndex;
    }

    @Override
    public Assignment<V, T> getParentAssignment() {
        return iParent;
    }

    @Override
    public long getVersion() {
        return iVersion;
    }
}
