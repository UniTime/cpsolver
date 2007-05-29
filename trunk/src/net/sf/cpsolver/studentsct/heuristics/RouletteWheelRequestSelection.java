package net.sf.cpsolver.studentsct.heuristics;

import java.util.Enumeration;

import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.heuristics.general.RouletteWheelSelection;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

public class RouletteWheelRequestSelection implements VariableSelection {
    
    public RouletteWheelRequestSelection(DataProperties properties) {
        super();
    }
    
    public void init(Solver solver) {
        
    }
    
    public Variable selectVariable(Solution solution) {
        RouletteWheelSelection roulette = new RouletteWheelSelection();
        for (Enumeration e=solution.getModel().variables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            double points = 0;
            if (request.getAssignment()==null)
                points +=10;
            else {
                Enrollment enrollment = (Enrollment)request.getAssignment();
                if (enrollment.toDouble()>request.getBound())
                    points +=1;
            }
            if (points>0)
                roulette.add(request, points);
        }
        return (Variable)roulette.nextElement();
    }
}
