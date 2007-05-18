package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Random unassignment of some problematic students
 */
public class RandomUnassignmentSelection implements NeighbourSelection {
    private Vector iStudents = null;
    protected double iRandom = 0.5;
    
    public RandomUnassignmentSelection(DataProperties properties) {
        iRandom = properties.getPropertyDouble("Neighbour.RandomUnassignmentProb", iRandom);
    }
    
    public void init(Solver solver) {
        iStudents = ((StudentSectioningModel)solver.currentSolution().getModel()).getStudents();
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        if (Math.random()<iRandom) {
            Student student = (Student)ToolBox.random(iStudents);
            return new UnassignStudentNeighbour(student);
        }
        return null;
    }
    
    public static class UnassignStudentNeighbour extends Neighbour {
        private Student iStudent = null;
        public UnassignStudentNeighbour(Student student) {
            iStudent = student;
        }
        
        public void assign(long iteration) {
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
                Request request = (Request)e.nextElement();
                if (request.getAssignment()!=null)
                    request.unassign(iteration);
            }
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer("Un{");
            sb.append(" "+iStudent);
            sb.append(" }");
            return sb.toString();
        }
        
    }
    
}
