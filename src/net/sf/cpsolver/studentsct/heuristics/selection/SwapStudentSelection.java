package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentChoiceRealFirstOrder;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Pick a student (one by one) with an incomplete schedule, try to find an improvement, identify problematic students.
 * <br><br>
 * For each request that does not have an assignment and that can be assined (see {@link Student#canAssign(Request)})
 * a randomly selected sub-domain is visited. For every such enrollment, a set of conflicting enrollments 
 * is computed and a possible student that can get an alternative enrollment is identified (if there is any). For each
 * such move a value (the cost of moving the problematic student somewhere else) is computed and the best possible move
 * is selected at the end. If there is no such move, a set of problematic students is identified, i.e., the students 
 * whose enrollments are preventing this student to get a request.
 * <br><br>
 * Each student can be selected for this swap move multiple times, but only if there is still a request that can be resolved.
 * At the end (when there is no other neighbour), the set of all such problematic students can be returned using the 
 * {@link ProblemStudentsProvider} interface.
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Neighbour.SwapStudentsTimeout</td><td>{@link Integer}</td><td>Timeout for each neighbour selection (in milliseconds).</td></tr>
 * <tr><td>Neighbour.SwapStudentsMaxValues</td><td>{@link Integer}</td><td>Limit for the number of considered values for each course request (see {@link CourseRequest#computeRandomEnrollments(int)}).</td></tr>
 * </table>
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

public class SwapStudentSelection implements NeighbourSelection, ProblemStudentsProvider {
    private static Logger sLog = Logger.getLogger(SwapStudentSelection.class);
    private HashSet iProblemStudents = new HashSet();
    private Student iStudent = null;
    private Enumeration iStudentsEnumeration = null;
    private int iTimeout = 5000;
    private int iMaxValues = 100;
    public static boolean sDebug = false;
    protected StudentOrder iOrder = new StudentChoiceRealFirstOrder();
    
    /**
     * Constructor
     * @param properties configuration
     */
    public SwapStudentSelection(DataProperties properties) {
        iTimeout = properties.getPropertyInt("Neighbour.SwapStudentsTimeout", iTimeout);
        iMaxValues = properties.getPropertyInt("Neighbour.SwapStudentsMaxValues", iMaxValues);
        if (properties.getProperty("Neighbour.SwapStudentsOrder")!=null) {
            try {
                iOrder = (StudentOrder)Class.forName(properties.getProperty("Neighbour.SwapStudentsOrder")).
                    getConstructor(new Class[] {DataProperties.class}).
                    newInstance(new Object[] {properties});
            } catch (Exception e) {
                sLog.error("Unable to set student order, reason:"+e.getMessage(),e);
            }
        }
    }
    
    /** Initialization */
    public void init(Solver solver) {
        Vector students = iOrder.order(((StudentSectioningModel)solver.currentSolution().getModel()).getStudents());
        iStudentsEnumeration = students.elements();
        iProblemStudents.clear();
    }
    
    /** 
     * For each student that does not have a complete schedule, try to find a request and a 
     * student that can be moved out of an enrollment so that the selected student
     * can be assigned to the selected request.
     */
    public Neighbour selectNeighbour(Solution solution) {
        if (iStudent!=null && !iStudent.isComplete()) {
            Selection selection = getSelection(iStudent);
            Neighbour neighbour = selection.select();
            if (neighbour!=null) return neighbour;
            else
                iProblemStudents.addAll(selection.getProblemStudents());
        }
        iStudent = null;
        while (iStudentsEnumeration.hasMoreElements()) {
            Student student = (Student)iStudentsEnumeration.nextElement();
            if (student.isComplete() || student.nrAssignedRequests()==0) continue;
            Selection selection = getSelection(student);
            Neighbour neighbour = selection.select();
            if (neighbour!=null) {
                iStudent = student;
                return neighbour;
            } else
                iProblemStudents.addAll(selection.getProblemStudents());
        }
        return null;
    }
    
    /** List of problematic students */
    public HashSet getProblemStudents() {
        return iProblemStudents;
    }
    
    /** Selection subclass for a student */
    public Selection getSelection(Student student) {
        return new Selection(student);
    }

    /** This class looks for a possible swap move for the given student */
    public class Selection {
        private Student iStudent;
        private long iT0, iT1;
        private boolean iTimeoutReached;
        private Enrollment iBestEnrollment;
        private double iBestValue;
        private HashSet iProblemStudents;
        
        /**
         * Constructor
         * @param student given student
         */
        public Selection(Student student) {
            iStudent = student;
        }
        
        /**
         * The actual selection
         */
        public SwapStudentNeighbour select() {
            if (sDebug) sLog.debug("select(S"+iStudent.getId()+")");
            iT0 = System.currentTimeMillis();
            iTimeoutReached = false;
            iBestEnrollment = null;
            iProblemStudents = new HashSet();
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
                if (iTimeout>0 && (System.currentTimeMillis()-iT0)>iTimeout) {
                    if (!iTimeoutReached) {
                        if (sDebug) sLog.debug("  -- timeout reached");
                        iTimeoutReached=true; 
                    }
                    break;
                }
                Request request = (Request)e.nextElement();
                if (request.getAssignment()!=null) continue;
                if (!iStudent.canAssign(request)) continue;
                if (sDebug) sLog.debug("  -- checking request "+request);
                Vector values = null;
                if (iMaxValues>0 && request instanceof CourseRequest) {
                    values = ((CourseRequest)request).computeRandomEnrollments(iMaxValues);
                } else values = request.values();
                for (Enumeration f=values.elements();f.hasMoreElements();) {
                    if (iTimeout>0 && (System.currentTimeMillis()-iT0)>iTimeout) {
                        if (!iTimeoutReached) {
                            if (sDebug) sLog.debug("  -- timeout reached");
                            iTimeoutReached=true; 
                        }
                        break;
                    }
                    Enrollment enrollment = (Enrollment)f.nextElement();
                    if (sDebug) sLog.debug("      -- enrollment "+enrollment);
                    Set conflicts = enrollment.variable().getModel().conflictValues(enrollment);
                    if (conflicts.contains(enrollment)) continue;
                    double value = enrollment.toDouble();
                    boolean unresolvedConflict = false;
                    for (Iterator j=conflicts.iterator();j.hasNext();) {
                        Enrollment conflict = (Enrollment)j.next();
                        if (sDebug) sLog.debug("        -- conflict "+conflict);
                        Enrollment other = bestSwap(conflict, enrollment, iProblemStudents);
                        if (other==null) {
                            if (sDebug) sLog.debug("          -- unable to resolve");
                            unresolvedConflict = true; break;
                        }
                        if (sDebug) sLog.debug("          -- can be resolved by switching to "+other.getName());
                        value -= conflict.toDouble();
                        value += other.toDouble();
                    }
                    if (unresolvedConflict) continue;
                    if (iBestEnrollment==null || iBestEnrollment.toDouble()>value) {
                        iBestEnrollment = enrollment; iBestValue = value;
                    };
                }
            }
            iT1 = System.currentTimeMillis();
            if (sDebug) sLog.debug("  -- done, best enrollment is "+iBestEnrollment);
            if (iBestEnrollment==null) {
                if (iProblemStudents.isEmpty()) iProblemStudents.add(iStudent);
                if (sDebug) sLog.debug("  -- problem students are: "+iProblemStudents);
                return null;
            }
            if (sDebug) sLog.debug("  -- value "+iBestValue);
            Enrollment[] assignment = new Enrollment[iStudent.getRequests().size()];
            int idx = 0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
                Request request = (Request)e.nextElement();
                assignment[idx++] = (iBestEnrollment.getRequest().equals(request)?iBestEnrollment:(Enrollment)request.getAssignment());
            }
            return new SwapStudentNeighbour(iBestValue, iBestEnrollment);
        }
        
        /** Was timeout reached during the selection */
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }

        /** Time spent in the last selection */
        public long getTime() {
            return iT1 - iT0;
        }
        
        /** The best enrollment found. */ 
        public Enrollment getBestEnrollment() {
            return iBestEnrollment;
        }
        
        /** Cost of the best enrollment found */
        public double getBestValue() {
            return iBestValue;
        }
        
        /** Set of problematic students computed in the last selection */
        public Set getProblemStudents() {
            return iProblemStudents;
        }
        
    }

    /**
     * Identify the best swap for the given student
     * @param conflict conflicting enrollment
     * @param enrl enrollment that is visited (to be assigned to the given student)
     * @param problematicStudents the current set of problematic students
     * @return best alternative enrollment for the student of the conflicting enrollment
     */
    public static Enrollment bestSwap(Enrollment conflict, Enrollment enrl, Set problematicStudents) {
        Enrollment bestEnrollment = null;
        for (Iterator i=conflict.getRequest().values().iterator();i.hasNext();) {
            Enrollment enrollment = (Enrollment)i.next();
            if (enrollment.equals(conflict)) continue;
            if (!enrl.isConsistent(enrollment)) continue;
            if (conflict.variable().getModel().conflictValues(enrollment).isEmpty()) {
                if (bestEnrollment==null || bestEnrollment.toDouble()>enrollment.toDouble())
                    bestEnrollment = enrollment;
            }
        }
        if (bestEnrollment==null && problematicStudents!=null) {
            boolean added = false;
            for (Iterator i=conflict.getRequest().values().iterator();i.hasNext();) {
                Enrollment enrollment = (Enrollment)i.next();
                if (enrollment.equals(conflict)) continue;
                if (!enrl.isConsistent(enrollment)) continue;
                Set conflicts = conflict.variable().getModel().conflictValues(enrollment);
                for (Iterator j=conflicts.iterator();j.hasNext();) {
                    Enrollment c = (Enrollment)j.next();
                    if (!enrl.getStudent().equals(c.getStudent()) && !conflict.getStudent().equals(c.getStudent()))
                        problematicStudents.add(c.getStudent());
                }
            }
            if (!added && !enrl.getStudent().equals(conflict.getStudent()))
                problematicStudents.add(conflict.getStudent());
        }
        return bestEnrollment;
    }
    
    /** Neighbour that contains the swap */
    public static class SwapStudentNeighbour extends Neighbour {
        private double iValue;
        private Enrollment iEnrollment;
        
        /**
         * Constructor
         * @param value cost of the move
         * @param enrollment the enrollment which is to be assigned to the given student 
         */
        public SwapStudentNeighbour(double value, Enrollment enrollment) {
            iValue = value;
            iEnrollment = enrollment;
        }
        
        /** 
         * Perform the move. 
         * All the requeired swaps are identified and performed as well.
         **/
        public void assign(long iteration) {
            if (iEnrollment.variable().getAssignment()!=null)
                iEnrollment.variable().unassign(iteration);
            Set conflicts = iEnrollment.variable().getModel().conflictValues(iEnrollment);
            for (Iterator i=conflicts.iterator();i.hasNext();) {
                Enrollment conflict = (Enrollment)i.next();
                Enrollment switchEnrl = bestSwap(conflict, iEnrollment, null);
                conflict.variable().unassign(iteration);
                if (switchEnrl!=null)
                    switchEnrl.variable().assign(iteration, switchEnrl);
            }
            iEnrollment.variable().assign(iteration, iEnrollment);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer("SwSt{");
            sb.append(" "+iEnrollment.getRequest().getStudent());
            sb.append(" ("+iValue+")");
            sb.append("\n "+iEnrollment.getRequest());
            sb.append(" "+iEnrollment);
            sb.append("\n}");
            return sb.toString();
        }
    }
}
