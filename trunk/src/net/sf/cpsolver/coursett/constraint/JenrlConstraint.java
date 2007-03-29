package net.sf.cpsolver.coursett.constraint;

import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.BinaryConstraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Join student enrollment constraint.
 * <br>
 * This constraint is placed between all pairs of classes where there is at least one student attending both classes.
 * It represents a number of student conflicts (number of joined enrollments), if the given two classes overlap in time.
 * <br>
 * Also, it dynamically maintains the counter of all student conflicts. It is a soft constraint.
 *
 *
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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

public class JenrlConstraint extends BinaryConstraint {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(JenrlConstraint.class);
    private double iJenrl = 0.0;
    private int iNrStrudents = 0;
    private boolean iAdded = false;
    private boolean iAddedDistance = false;
    
    /** Constructor
     */
    public JenrlConstraint() {
        super();
    }

    public void computeConflicts(Value value, Set conflicts) {
    }
    
    public boolean inConflict(Value value) {
        return false;
    }
    
    public boolean isConsistent(Value value1, Value value2) {
        return true;
    }
    
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        if (iAdded) {
        	((TimetableModel)getModel()).getViolatedStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
            if (areStudentConflictsHard())
            	((TimetableModel)getModel()).getViolatedHardStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
            iAdded=false;
            ((Lecture)first()).removeActiveJenrl(this);
            ((Lecture)second()).removeActiveJenrl(this);
        }
        if (iAddedDistance) {
        	((TimetableModel)getModel()).getViolatedDistanceStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
        	iAddedDistance = false;
        }
    }
    
    /** Returns true if the given placements are overlapping or they are back-to-back and too far for students.*/
    public static boolean isInConflict(Placement p1, Placement p2) {
    	return isInConflict(p1, p2, true);
    }
    
   public static boolean isInConflict(Placement p1, Placement p2, boolean useDistances) {
       if (p1==null || p2==null) return false;
       TimeLocation t1=p1.getTimeLocation(), t2=p2.getTimeLocation();
       if (!t1.shareDays(t2)) return false;
       if (!t1.shareWeeks(t2)) return false;
       if (t1.shareHours(t2)) return true;
       if (!useDistances) return false;
       int s1 = t1.getStartSlot(), s2 = t2.getStartSlot();
       if (s1+t1.getNrSlotsPerMeeting()!=s2 &&
           s2+t2.getNrSlotsPerMeeting()!=s1) return false;
       double distance = Placement.getDistance(p1,p2);
       TimetableModel m = (TimetableModel)p1.variable().getModel();
       if (m==null) {
    	   if (distance <= 67.0) return false;
    	   if (distance <= 100.0 && (
    			   (t1.getLength()==18 && s1+t1.getLength()==s2) ||
    			   (t2.getLength()==18 && s2+t2.getLength()==s1)))
    		   return false;
    	   return true;
       } else {
    	   if (distance <= m.getStudentDistanceLimit()) return false;
    	   if (distance <= m.getStudentDistanceLimit75min() && (
    			   (t1.getLength()==18 && s1+t1.getLength()==s2) ||
    			   (t2.getLength()==18 && s2+t2.getLength()==s1)))
    		   return false;
    	   return true;
       }
   }

   public void assigned(long iteration, Value value) {
       super.assigned(iteration, value);
       if (second()==null || first().getAssignment()==null || second().getAssignment()==null) return;
        //if (v1.getInitialAssignment()!=null && v2.getInitialAssignment()!=null && v1.getAssignment().equals(v1.getInitialAssignment()) && v2.getAssignment().equals(v2.getInitialAssignment())) return;
        if (isInConflict((Placement)first().getAssignment(),(Placement)second().getAssignment())) {
            iAdded=true;
            ((TimetableModel)getModel()).getViolatedStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
            if (areStudentConflictsHard())
            	((TimetableModel)getModel()).getViolatedHardStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
            if (areStudentConflictsDistance()) {
            	((TimetableModel)getModel()).getViolatedDistanceStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
            	iAddedDistance = true;
            }
            ((Lecture)first()).addActiveJenrl(this);
            ((Lecture)second()).addActiveJenrl(this);
        }
    }

   /** Number of joined enrollments if the given value is assigned to the given variable */
    public long jenrl(Variable variable, Value value) {
        Lecture anotherLecture = (Lecture)(first().equals(variable)?second():first());
        if (anotherLecture.getAssignment()==null) return 0;
        Lecture lecture = (Lecture) variable;
        return (isInConflict((Placement)anotherLecture.getAssignment(),(Placement)value)?(long)Math.ceil(iJenrl):0);
    }

    /** True if the given two lectures overlap in time */
    public boolean isInConflict() {
        return iAdded;
    }

    /** True if the given two lectures overlap in time */
    public boolean isInConflictPrecise() {
        return isInConflict((Placement)first().getAssignment(),(Placement)second().getAssignment());
    }

    /** Increment the number of joined enrollments (during student final sectioning) */
    public void incJenrl(Student student ) {
        if (iAdded) {
        	((TimetableModel)getModel()).getViolatedStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
        	if (areStudentConflictsHard())
        		((TimetableModel)getModel()).getViolatedHardStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
            if (iAddedDistance)
            	((TimetableModel)getModel()).getViolatedDistanceStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
        }
        iJenrl+=student.getJenrlWeight((Lecture)first(),(Lecture)second());
        iNrStrudents++;
        if (iAdded) {
        	((TimetableModel)getModel()).getViolatedStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
        	if (areStudentConflictsHard())
        		((TimetableModel)getModel()).getViolatedHardStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
            if (iAddedDistance)
            	((TimetableModel)getModel()).getViolatedDistanceStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
        }
    }
    
    public double getJenrlWeight(Student student) {
    	return student.getJenrlWeight((Lecture)first(),(Lecture)second());
    }
    
    /** Decrement the number of joined enrollments (during student final sectioning) */
    public void decJenrl(Student student) {
        if (iAdded) {
        	((TimetableModel)getModel()).getViolatedStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
        	if (areStudentConflictsHard())
        		((TimetableModel)getModel()).getViolatedHardStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
            if (iAddedDistance)
            	((TimetableModel)getModel()).getViolatedDistanceStudentConflictsCounter().dec((long)Math.ceil(iJenrl));
        }
        iJenrl-=student.getJenrlWeight((Lecture)first(),(Lecture)second());
        iNrStrudents--;
        if (iAdded) {
        	((TimetableModel)getModel()).getViolatedStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
        	if (areStudentConflictsHard())
        		((TimetableModel)getModel()).getViolatedHardStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
            if (iAddedDistance)
            	((TimetableModel)getModel()).getViolatedDistanceStudentConflictsCounter().inc((long)Math.ceil(iJenrl));
        }
    }

    /** Number of joined enrollments (during student final sectioning) */
    public long getJenrl() { return (long)Math.ceil(iJenrl); }
    public int getNrStudents() { return iNrStrudents; }
    public boolean isHard() { return false; }
    
    public String toString() {
        return "Joint Enrollment between "+first().getName()+" and "+second().getName();
    }
    
    public boolean areStudentConflictsHard() {
    	return ((Lecture)first()).areStudentConflictsHard((Lecture)second());
    }
    
    public boolean areStudentConflictsDistance() {
    	return !((Placement)first().getAssignment()).getTimeLocation().hasIntersection(((Placement)second().getAssignment()).getTimeLocation());
    }
       
    public boolean areStudentConflictsDistance(Value value) {
    	Placement first = (Placement)(first().equals(value.variable())?value:first().getAssignment());
    	Placement second = (Placement)(second().equals(value.variable())?value:second().getAssignment());
    	if (first==null || second==null) return false;
    	return !first.getTimeLocation().hasIntersection(second.getTimeLocation());
    }
    
    public boolean isOfTheSameProblem() {
    	Lecture first = (Lecture)first();
    	Lecture second = (Lecture)second();
    	return ToolBox.equals(first.getSolverGroupId(),second.getSolverGroupId());
    }

}
