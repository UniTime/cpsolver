package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.HashSet;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Random unassignment of some problematic students
 */
public class RndUnProblStudSelection extends RandomUnassignmentSelection {
    private ProblemStudentsProvider iProblemStudentsProvider = null;
    private HashSet iProblemStudents = null;
    
    public RndUnProblStudSelection(DataProperties properties, ProblemStudentsProvider psp) {
        super(properties);
        iProblemStudentsProvider = psp;
        iRandom = properties.getPropertyDouble("Neighbour.RandomUnassignmentOfProblemStudentProb", 0.9);
    }
    
    public void init(Solver solver) {
        iProblemStudents = iProblemStudentsProvider.getProblemStudents();
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        if (!iProblemStudents.isEmpty() && Math.random()<iRandom) {
            Student student = (Student)ToolBox.random(iProblemStudents);
            iProblemStudents.remove(student);
            return new UnassignStudentNeighbour(student);
        }
        return null;
    }
}
