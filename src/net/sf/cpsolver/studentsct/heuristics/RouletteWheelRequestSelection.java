package net.sf.cpsolver.studentsct.heuristics;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
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
        return (Variable)roulette.select();
    }

    public static class RouletteWheelSelection {
        private Vector iAdepts = new Vector();
        private Vector iPoints = new Vector();
        private double iTotalPoints = 0;
        public void add(Object adept, double points) {
            iAdepts.add(adept); iPoints.add(new Double(points)); iTotalPoints+=points;
        }
        public Object select() {
            if (iAdepts.isEmpty()) return null;
            double rx = ToolBox.random()*iTotalPoints;
            int idx = 0;
            for (Enumeration e=iPoints.elements();e.hasMoreElements();idx++) {
                rx -= ((Double)e.nextElement()).doubleValue();
                if (rx<0) return iAdepts.elementAt(idx);
            }
            return iAdepts.lastElement();
        }
    }
}
