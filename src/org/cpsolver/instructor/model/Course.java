package org.cpsolver.instructor.model;

public class Course {
    private Long iCourseId;
    private String iCourseName;
    private boolean iExclusive;
    private boolean iCommon;
    
    public Course(long courseId, String courseName, boolean exclusive, boolean sameCommon) {
        iCourseId = courseId; iCourseName = courseName;
        iExclusive = exclusive; iCommon = sameCommon;
    }
    
    public Long getCourseId() { return iCourseId; }
    public String getCourseName() { return iCourseName == null ? "C" + iCourseId : iCourseName; }
    
    public boolean isExclusive() { return iExclusive; }
    public boolean isSameCommon() { return iCommon; }
    
    @Override
    public int hashCode() {
        return getCourseId().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Course)) return false;
        Course c = (Course)o;
        return getCourseId().equals(c.getCourseId());            
    }
    
    @Override
    public String toString() { return getCourseName(); }
}
