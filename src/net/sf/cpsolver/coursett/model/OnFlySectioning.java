package net.sf.cpsolver.coursett.model;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

public class OnFlySectioning implements ModelListener {
    private FinalSectioning iFinalSectioning;
    private boolean iRecursive = true;
    private boolean iConfigAsWell = false;
    
    public OnFlySectioning(TimetableModel model) {
        iFinalSectioning = new FinalSectioning(model);
    }

    public void variableAdded(Variable variable) {}
    
    public void variableRemoved(Variable variable) {}
    
    public void constraintAdded(Constraint constraint) {}
    
    public void constraintRemoved(Constraint constraint) {}
    
    public void beforeAssigned(long iteration, Value value) {}

    public void beforeUnassigned(long iteration, Value value) {}
    
    public void afterAssigned(long iteration, Value value) {
        if (iteration>0)
            iFinalSectioning.resection((Lecture)value.variable(), iRecursive, iConfigAsWell);
    }

    public void afterUnassigned(long iteration, Value value) {}

    public boolean init(net.sf.cpsolver.ifs.solver.Solver solver) {
        iRecursive = solver.getProperties().getPropertyBoolean("OnFlySectioning.Recursive", true);
        iConfigAsWell = solver.getProperties().getPropertyBoolean("OnFlySectioning.ConfigAsWell", false);
        return true;
    }
}
