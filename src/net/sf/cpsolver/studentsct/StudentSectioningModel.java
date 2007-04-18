package net.sf.cpsolver.studentsct;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class StudentSectioningModel extends Model {
    private Vector iStudents = new Vector();
    
    public StudentSectioningModel() {
        super();
        addGlobalConstraint(new SectionLimit());
    }
    
    public Vector getStudents() {
        return iStudents;
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
    }
    
    public int nrComplete() {
        int nrComplete = 0;
        for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            if (student.isComplete()) nrComplete++;
        }
        return nrComplete;
    }
    
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        int nrComplete = nrComplete();
        info.put("Students with complete schedule" , 
                sDoubleFormat.format(100.0*nrComplete/getStudents().size())+"% ("+nrComplete()+"/"+getStudents().size()+")");
        return info;
    }
    
    public double getTotalValue() {
        double valCurrent = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();)
            valCurrent += ((Variable)e.nextElement()).getAssignment().toDouble();
        for (Enumeration e=iStudents.elements();e.hasMoreElements();)
            if (((Student)e.nextElement()).isComplete()) valCurrent += -1000;
        return valCurrent;
    }

}
