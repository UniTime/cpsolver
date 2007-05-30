package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.Collection;
import java.util.Collections;
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
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Section all students using incremental branch & bound (no unassignments)
 */

public class BranchBoundSelection implements NeighbourSelection {
    private static Logger sLog = Logger.getLogger(BranchBoundSelection.class); 
    private int iTimeout = 10000;
    private DistanceConflict iDistanceConflict = null;
    public static boolean sDebug = false;
    protected Enumeration iStudentsEnumeration = null;
    private boolean iMinimizePenalty = false;
    
    public BranchBoundSelection(DataProperties properties) {
        iTimeout = properties.getPropertyInt("Neighbour.BranchAndBoundTimeout", iTimeout);
        iMinimizePenalty = properties.getPropertyBoolean("Neighbour.BranchAndBoundMinimizePenalty", iMinimizePenalty);
        if (iMinimizePenalty) sLog.info("Overall penalty is going to be minimized (together with the maximization of the number of assigned requests and minimization of distance conflicts).");
    }

    public void init(Solver solver) {
        Vector students = new Vector(((StudentSectioningModel)solver.currentSolution().getModel()).getStudents());
        Collections.shuffle(students);
        iStudentsEnumeration = students.elements();
        if (iDistanceConflict==null)
            for (Enumeration e=solver.getExtensions().elements();e.hasMoreElements();) {
                Extension ext = (Extension)e.nextElement();
                if (ext instanceof DistanceConflict)
                    iDistanceConflict = (DistanceConflict)ext;
            }
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        while (iStudentsEnumeration.hasMoreElements()) {
            Student student = (Student)iStudentsEnumeration.nextElement();
            Neighbour neighbour = getSelection(student).select();
            if (neighbour!=null) return neighbour;
        }
        return null;
    }
    
    public Selection getSelection(Student student) {
        return new Selection(student);
    }
    
    public class Selection {
        private Student iStudent;
        private long iT0, iT1;
        private boolean iTimeoutReached;
        private Enrollment[] iAssignment, iBestAssignment;
        private double iBestValue;
        private Hashtable iValues;
        
        public Selection(Student student) {
            iStudent = student;
        }
        
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
        
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }
        
        public long getTime() {
            return iT1 - iT0;
        }
        
        public Enrollment[] getBestAssignment() {
            return iBestAssignment;
        }
        
        public double getBestValue() {
            return iBestValue;
        }
        
        public int getBestNrAssigned() {
            int nrAssigned = 0;
            for (int i=0;i<iBestAssignment.length;i++)
                if (iBestAssignment[i]!=null) nrAssigned++;
            return nrAssigned;
        }

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
                        bound ++;
                    }
                }
            }
            return bound;
        }
        
        public double getNrDistanceConflicts(int idx) {
            if (iDistanceConflict==null || iAssignment[idx]==null) return 0;
            double nrDist = iDistanceConflict.nrConflicts(iAssignment[idx]);
            for (int x=0;x<idx;x++)
                if (iAssignment[x]!=null)
                    nrDist+=iDistanceConflict.nrConflicts(iAssignment[x],iAssignment[idx]);
            return nrDist;
        }

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

        public double getValue() {
            double value = 0.0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null)
                    value += iAssignment[i].toDouble(getNrDistanceConflicts(i));
            return value;
        }
        
        public double getPenalty() {
            double bestPenalty = 0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null) bestPenalty += iAssignment[i].getPenalty() + getNrDistanceConflicts(i);
            return bestPenalty;
        }
        
        
        public double getPenaltyBound(int idx) {
            double bound = 0.0;
            int i=0, alt=0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();i++) {
                Request r  = (Request)e.nextElement();
                if (i<idx) {
                    if (iAssignment[i]!=null)
                        bound += iAssignment[i].getPenalty() + getNrDistanceConflicts(i);
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
        
        public Enrollment firstConflict(Enrollment enrollment) {
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
        
        public int getNrAssigned() {
            int assigned = 0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null) assigned++;            
            return assigned;
        }
        
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
                            if (firstConflict(enrollment)!=null) continue;
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
                Enrollment conflict = firstConflict(enrollment);
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
    
    public static class BranchBoundNeighbour extends Neighbour {
        private double iValue;
        private Enrollment[] iAssignment;
        
        public BranchBoundNeighbour(double value, Enrollment[] assignment) {
            iValue = value;
            iAssignment = assignment;
        }
        
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
