package net.sf.cpsolver.studentsct;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class StudentSectioningModel extends Model {
    private Vector iStudents = new Vector();
    private HashSet iCompleteStudents = new HashSet();
    private double iTotalValue = 0.0;
    
    public StudentSectioningModel() {
        super();
        addGlobalConstraint(new SectionLimit());
    }
    
    public Vector getStudents() {
        return iStudents;
    }
    
    public Set getCompleteStudents() {
        return iCompleteStudents;
    }
    
    public void addStudent(Student student) {
        iStudents.addElement(student);
        student.setModel(this);
        StudentConflict conflict = new StudentConflict();
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            conflict.addVariable(request);
            addVariable(request);
        }
        addConstraint(conflict);
        if (student.isComplete())
            iCompleteStudents.add(student);
    }
    
    public int nrComplete() {
        return getCompleteStudents().size();
        /*
        int nrComplete = 0;
        for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            if (student.isComplete()) nrComplete++;
        }
        return nrComplete;
        */
    }
    
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("Students with complete schedule" , 
                sDoubleFormat.format(100.0*nrComplete()/getStudents().size())+"% ("+nrComplete()+"/"+getStudents().size()+")");
        return info;
    }
    
    public double getTotalValue() {
        return iTotalValue;
        //return super.getTotalValue();
    }
    
    public void afterAssigned(long iteration, Value value) {
        super.afterAssigned(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        Student student = enrollment.getStudent();
        if (student.isComplete())
            iCompleteStudents.add(student);
        iTotalValue += value.toDouble();
    }
    
    public void afterUnassigned(long iteration, Value value) {
        super.afterUnassigned(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        Student student = enrollment.getStudent();
        if (iCompleteStudents.contains(student) && !student.isComplete())
            iCompleteStudents.remove(student);
        iTotalValue -= value.toDouble();
    }
}
