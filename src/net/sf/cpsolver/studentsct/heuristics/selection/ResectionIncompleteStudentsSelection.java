package net.sf.cpsolver.studentsct.heuristics.selection;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Resection incomplete studends
 */

public class ResectionIncompleteStudentsSelection extends BranchBoundSelection {

    public ResectionIncompleteStudentsSelection(DataProperties properties) {
        super(properties);
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        while (iStudentsEnumeration.hasMoreElements()) {
            Student student = (Student)iStudentsEnumeration.nextElement();
            if (student.nrAssignedRequests()==0 || student.isComplete()) continue;
            Neighbour neighbour = getSelection(student).select();
            if (neighbour!=null) return neighbour;
        }
        return null;
    }
    
}
