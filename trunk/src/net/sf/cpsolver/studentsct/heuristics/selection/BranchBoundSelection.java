package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentChoiceRealFirstOrder;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Section all students using incremental branch & bound (no unassignments).
 * All students are taken in a random order, for each student a branch & bound
 * algorithm is used to find his/her best schedule on top of all other existing
 * student schedules (no enrollment of a different student is unassigned).
 *
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Neighbour.BranchAndBoundTimeout</td><td>{@link Integer}</td><td>Timeout for each neighbour selection (in milliseconds).</td></tr>
 * <tr><td>Neighbour.BranchAndBoundMinimizePenalty</td><td>{@link Boolean}</td><td>If true, section penalties (instead of section values) are minimized: 
 * overall penalty is minimized together with the maximization of the number of assigned requests and minimization of distance conflicts 
 * -- this variant is to better mimic the case when students can choose their sections (section times).</td></tr>
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

public class BranchBoundSelection implements NeighbourSelection {
    private static Logger sLog = Logger.getLogger(BranchBoundSelection.class); 
    protected int iTimeout = 10000;
    protected DistanceConflict iDistanceConflict = null;
    public static boolean sDebug = false;
    protected Enumeration iStudentsEnumeration = null;
    protected boolean iMinimizePenalty = false;
    protected StudentOrder iOrder = new StudentChoiceRealFirstOrder();
    
    /**
     * Constructor
     * @param properties configuration
     */
    public BranchBoundSelection(DataProperties properties) {
        iTimeout = properties.getPropertyInt("Neighbour.BranchAndBoundTimeout", iTimeout);
        iMinimizePenalty = properties.getPropertyBoolean("Neighbour.BranchAndBoundMinimizePenalty", iMinimizePenalty);
        if (iMinimizePenalty) sLog.info("Overall penalty is going to be minimized (together with the maximization of the number of assigned requests and minimization of distance conflicts).");
        if (properties.getProperty("Neighbour.BranchAndBoundOrder")!=null) {
            try {
                iOrder = (StudentOrder)Class.forName(properties.getProperty("Neighbour.BranchAndBoundOrder")).
                    getConstructor(new Class[] {DataProperties.class}).
                    newInstance(new Object[] {properties});
            } catch (Exception e) {
                sLog.error("Unable to set student order, reason:"+e.getMessage(),e);
            }
        }
    }
    
    /**
     * Initialize
     */
    public void init(Solver solver) {
        Vector students = iOrder.order(((StudentSectioningModel)solver.currentSolution().getModel()).getStudents());
        iStudentsEnumeration = students.elements();
        if (iDistanceConflict==null)
            for (Enumeration e=solver.getExtensions().elements();e.hasMoreElements();) {
                Extension ext = (Extension)e.nextElement();
                if (ext instanceof DistanceConflict)
                    iDistanceConflict = (DistanceConflict)ext;
            }
    }
    
    /** 
     * Select neighbour. All students are taken, one by one in a random order. 
     * For each student a branch & bound search is employed. 
     */
    public Neighbour selectNeighbour(Solution solution) {
        while (iStudentsEnumeration.hasMoreElements()) {
            Student student = (Student)iStudentsEnumeration.nextElement();
            Neighbour neighbour = getSelection(student).select();
            if (neighbour!=null) return neighbour;
        }
        return null;
    }
    
    /**
     * Branch & bound selection for a student
     */
    public Selection getSelection(Student student) {
        return new Selection(student);
    }
    
    /**
     * Branch & bound selection for a student
     */
    public class Selection {
        /** Student */
        protected Student iStudent;
        /** Start time */
        protected long iT0;
        /** End time */
        protected long iT1;
        /** Was timeout reached */
        protected boolean iTimeoutReached;
        /** Current assignment */
        protected Enrollment[] iAssignment;
        /** Best assignment */
        protected Enrollment[] iBestAssignment;
        /** Best value */
        protected double iBestValue;
        /** Value cache */
        protected Hashtable iValues;
        
        /**
         * Constructor
         * @param student selected student
         */
        public Selection(Student student) {
            iStudent = student;
        }
        
        /**
         * Execute branch & bound, return the best found schedule for the selected student.
         */
        public BranchBoundNeighbour select() {
            iT0 = System.currentTimeMillis();
            iTimeoutReached = false;
            iAssignment = new Enrollment[iStudent.getRequests().size()];
            iBestAssignment = null;
            iBestValue = 0;
            iValues = new Hashtable();
            backTrack(0);
            iT1 = System.currentTimeMillis();
            if (iBestAssignment==null) return null;
            return new BranchBoundNeighbour(iBestValue, iBestAssignment);
        }
        
        /** Was timeout reached */
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }
        
        /** Time (in milliseconds) the branch & bound did run */
        public long getTime() {
            return iT1 - iT0;
        }
        
        /** Best schedule */
        public Enrollment[] getBestAssignment() {
            return iBestAssignment;
        }
        
        /** Value of the best schedule */
        public double getBestValue() {
            return iBestValue;
        }
        
        /** Number of requests assigned in the best schedule */
        public int getBestNrAssigned() {
            int nrAssigned = 0;
            for (int i=0;i<iBestAssignment.length;i++)
                if (iBestAssignment[i]!=null) nrAssigned++;
            return nrAssigned;
        }

        /** Bound for the number of assigned requests in the current schedule */ 
        public int getNrAssignedBound(int idx) {
            int bound = 0;
            int i=0, alt=0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();i++) {
                Request r  = (Request)e.nextElement();
                if (i<idx) {
                    if (iAssignment[i]!=null) 
                        bound++;
                    if (r.isAlternative()) {
                        if (iAssignment[i]!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
                    } else {
                        if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && iAssignment[i]==null) alt++;
                    }
                } else {
                    if (!r.isAlternative())
                        bound ++;
                    else if (alt>0) {
                        bound ++; alt--;
                    }
                }
            }
            return bound;
        }
        
        /** Number of distance conflicts of idx-th assignment of the current schedule */
        public double getNrDistanceConflicts(int idx) {
            if (iDistanceConflict==null || iAssignment[idx]==null) return 0;
            double nrDist = iDistanceConflict.nrConflicts(iAssignment[idx]);
            for (int x=0;x<idx;x++)
                if (iAssignment[x]!=null)
                    nrDist+=iDistanceConflict.nrConflicts(iAssignment[x],iAssignment[idx]);
            return nrDist;
        }

        /** Bound for the current schedule */
        public double getBound(int idx) {
            double bound = 0.0;
            int i=0, alt=0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();i++) {
                Request r  = (Request)e.nextElement();
                if (i<idx) {
                    if (iAssignment[i]!=null) 
                        bound += iAssignment[i].toDouble(getNrDistanceConflicts(i));
                    if (r.isAlternative()) {
                        if (iAssignment[i]!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
                    } else {
                        if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && iAssignment[i]==null) alt++;
                    }
                } else {
                    if (!r.isAlternative())
                        bound += r.getBound();
                    else if (alt>0) {
                        bound += r.getBound(); alt--;
                    }
                }
            }
            return bound;
        }

        /** Value of the current schedule */
        public double getValue() {
            double value = 0.0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null)
                    value += iAssignment[i].toDouble(getNrDistanceConflicts(i));
            return value;
        }
        
        /** Assignment penalty */
        protected double getAssignmentPenalty(int i) {
            return iAssignment[i].getPenalty() + getNrDistanceConflicts(i);
        }
        
        /** Penalty of the current schedule */
        public double getPenalty() {
            double bestPenalty = 0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null) bestPenalty += getAssignmentPenalty(i);
            return bestPenalty;
        }
        
        
        /** Penalty bound of the current schedule */
        public double getPenaltyBound(int idx) {
            double bound = 0.0;
            int i=0, alt=0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();i++) {
                Request r  = (Request)e.nextElement();
                if (i<idx) {
                    if (iAssignment[i]!=null)
                        bound += getAssignmentPenalty(i);
                    if (r.isAlternative()) {
                        if (iAssignment[i]!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
                    } else {
                        if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && iAssignment[i]==null) alt++;
                    }
                } else {
                    if (!r.isAlternative()) {
                        if (r instanceof CourseRequest)
                            bound += ((CourseRequest)r).getMinPenalty();
                    } else if (alt>0) {
                        if (r instanceof CourseRequest)
                            bound += ((CourseRequest)r).getMinPenalty();
                        alt--;
                    }
                }
            }
            return bound;
        }
        
        /** Save the current schedule as the best */ 
        public void saveBest() {
            if (iBestAssignment==null)
                iBestAssignment = new Enrollment[iAssignment.length];
            for (int i=0;i<iAssignment.length;i++)
                iBestAssignment[i] = iAssignment[i];
            if (iMinimizePenalty)
                iBestValue = getPenalty();
            else
                iBestValue = getValue();
        }
        
        /** First conflicting enrollment */
        public Enrollment firstConflict(int idx, Enrollment enrollment) {
            Set conflicts = enrollment.variable().getModel().conflictValues(enrollment);
            if (conflicts.contains(enrollment)) return enrollment;
            if (conflicts!=null && !conflicts.isEmpty()) {
                for (Iterator i=conflicts.iterator();i.hasNext();) {
                    Enrollment conflict = (Enrollment)i.next();
                    if (!conflict.getStudent().equals(iStudent)) return conflict;
                }
            }
            for (int i=0;i<iAssignment.length;i++) {
                if (iAssignment[i]==null) continue;
                if (!iAssignment[i].isConsistent(enrollment)) return iAssignment[i];
            }
            return null;
        }
        
        /** True if the given request can be assigned */
        public boolean canAssign(Request request, int idx) {
            if (!request.isAlternative() || iAssignment[idx]!=null) return true;
            int alt = 0;
            int i = 0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();i++) {
                Request r  = (Request)e.nextElement();
                if (r.equals(request)) continue;
                if (r.isAlternative()) {
                    if (iAssignment[i]!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
                } else {
                    if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && iAssignment[i]==null) alt++;
                }
            }
            return (alt>0);
        }
        
        /** Number of assigned requests in the current schedule */
        public int getNrAssigned() {
            int assigned = 0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null) assigned++;            
            return assigned;
        }
        
        /** branch & bound search */
        public void backTrack(int idx) {
            if (sDebug) sLog.debug("backTrack("+getNrAssigned()+"/"+getValue()+","+idx+")");
            if (iTimeout>0 && (System.currentTimeMillis()-iT0)>iTimeout) {
                if (sDebug) sLog.debug("  -- timeout reached");
                iTimeoutReached=true; return;
            }
            if (iMinimizePenalty) {
                if (iBestAssignment!=null && (getNrAssignedBound(idx)<getBestNrAssigned() || (getNrAssignedBound(idx)==getBestNrAssigned() && getPenaltyBound(idx)>=iBestValue))) {
                    if (sDebug) sLog.debug("  -- branch number of assigned "+getNrAssignedBound(idx)+"<"+getBestNrAssigned()+", or penalty "+getPenaltyBound(idx)+">="+iBestValue);
                    return;
                }
                if (idx==iAssignment.length) {
                    if (iBestAssignment==null || (getNrAssigned()>getBestNrAssigned() || (getNrAssigned()==getBestNrAssigned() && getPenalty()<iBestValue))) {
                        if (sDebug) sLog.debug("  -- best solution found "+getNrAssigned()+"/"+getPenalty());
                        saveBest();
                    }
                    return;
                }
            } else {
                if (iBestAssignment!=null && getBound(idx)>=iBestValue) {
                    if (sDebug) sLog.debug("  -- branch "+getBound(idx)+" >= "+iBestValue);
                    return;
                }
                if (idx==iAssignment.length) {
                    if (iBestAssignment==null || getValue()<iBestValue) {
                        if (sDebug) sLog.debug("  -- best solution found "+getNrAssigned()+"/"+getValue());
                        saveBest();
                    }
                    return;
                }
            }
            
            Request request = (Request)iStudent.getRequests().elementAt(idx);
            if (sDebug) sLog.debug("  -- request: "+request);
            if (!canAssign(request, idx)) {
                if (sDebug) sLog.debug("    -- cannot assign");
                backTrack(idx+1);
                return;
            }
            Collection values = null;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest)request;
                if (!courseRequest.getSelectedChoices().isEmpty()) {
                    if (sDebug) sLog.debug("    -- selection among selected enrollments");
                    values = courseRequest.getSelectedEnrollments(true);
                    if (values!=null && !values.isEmpty()) { 
                        boolean hasNoConflictValue = false;
                        for (Iterator i=values.iterator();i.hasNext();) {
                            Enrollment enrollment = (Enrollment)i.next();
                            if (firstConflict(idx, enrollment)!=null) continue;
                            hasNoConflictValue = true;
                            if (sDebug) sLog.debug("      -- nonconflicting enrollment found: "+enrollment);
                            iAssignment[idx] = enrollment;
                            backTrack(idx+1);
                            iAssignment[idx] = null;
                        }
                        if (hasNoConflictValue) return;
                    }
                }
                values = (Collection)iValues.get(courseRequest);
                if (values==null) {
                    values = courseRequest.getAvaiableEnrollmentsSkipSameTime();
                    iValues.put(courseRequest, values);
                }
            } else {
                values = request.computeEnrollments();
            }    
            if (sDebug) {
                sLog.debug("  -- nrValues: "+values.size());
                int vIdx=1;
                for (Iterator i=values.iterator();i.hasNext();vIdx++) {
                    Enrollment enrollment = (Enrollment)i.next();
                    if (sDebug) sLog.debug("    -- ["+vIdx+"]: "+enrollment);
                }
            }
            boolean hasNoConflictValue = false;
            for (Iterator i=values.iterator();i.hasNext();) {
                Enrollment enrollment = (Enrollment)i.next();
                if (sDebug) sLog.debug("    -- enrollment: "+enrollment);
                Enrollment conflict = firstConflict(idx, enrollment);
                if (conflict!=null) {
                    if (sDebug) sLog.debug("        -- in conflict with: "+conflict);
                    continue;
                }
                hasNoConflictValue = true;
                iAssignment[idx] = enrollment;
                backTrack(idx+1);
                iAssignment[idx] = null;
            }
            if (!hasNoConflictValue || request instanceof CourseRequest) backTrack(idx+1);
        }
    }    
    
    /** Branch & bound neighbour -- a schedule of a student */
    public static class BranchBoundNeighbour extends Neighbour {
        private double iValue;
        private Enrollment[] iAssignment;
        
        /**
         * Constructor
         * @param value value of the schedule
         * @param assignment enrollments of student's requests
         */
        public BranchBoundNeighbour(double value, Enrollment[] assignment) {
            iValue = value;
            iAssignment = assignment;
        }
        
        public double value() {
            return iValue;
        }
        
        /** Assignment */
        public Enrollment[] getAssignment() {
            return iAssignment;
        }
        
        /** Assign the schedule */
        public void assign(long iteration) {
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null)
                    iAssignment[i].variable().assign(iteration, iAssignment[i]);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer("B&B{");
            Student student = null;
            for (int i=0;i<iAssignment.length;i++) {
                if (iAssignment[i]!=null) {
                    student = iAssignment[i].getRequest().getStudent();
                    sb.append(" "+student);
                    sb.append(" ("+iValue+")");
                    break;
                }
            }
            if (student!=null) {
                int idx=0;
                for (Enumeration e=student.getRequests().elements();e.hasMoreElements();idx++) {
                    Request request = (Request)e.nextElement();
                    sb.append("\n"+request);
                    Enrollment enrollment = iAssignment[idx];
                    if (enrollment==null)
                        sb.append("  -- not assigned");
                    else
                        sb.append("  -- "+enrollment);
                }
            }
            sb.append("\n}");
            return sb.toString();
        }
        
    }
}
