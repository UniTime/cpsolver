package net.sf.cpsolver.studentsct.model;

public class Course {
    private long iId = -1;
    private String iSubjectArea = null;
    private String iCourseNumber = null;
    private Offering iOffering = null;
    
    public Course(long id, String subjectArea, String courseNumber, Offering offering) {
        iId = id; 
        iSubjectArea = subjectArea;
        iCourseNumber = courseNumber;
        iOffering = offering;
        iOffering.getCourses().add(this);
    }
    
    public long getId() {
        return iId;
    }
    
    public String getSubjectArea() {
        return iSubjectArea;
    }

    public String getCourseNumber() {
        return iCourseNumber;
    }

    public String getName() {
        return iSubjectArea + " " + iCourseNumber;
    }
    
    public String toString() {
        return getName();
    }
    
    public Offering getOffering() {
        return iOffering;
    }
}
