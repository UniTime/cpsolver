package net.sf.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.DistanceConflict.Conflict;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;

public class DistanceConflictTable {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(DistanceConflictTable.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    
    private StudentSectioningModel iModel = null;
    private DistanceConflict iDC = null;
    
    public DistanceConflictTable(StudentSectioningModel model) {
        iModel = model;
        iDC = model.getDistanceConflict();
        if (iDC==null)
            iDC = new DistanceConflict(null, model.getProperties());
    }
    
    public StudentSectioningModel getModel() {
        return iModel;
    }

    private boolean areInHardConfict(Request r1, Request r2) {
        for (Enumeration e=r1.values().elements();e.hasMoreElements();) {
            Enrollment e1 = (Enrollment)e.nextElement();
            for (Enumeration f=r2.values().elements();f.hasMoreElements();) {
                Enrollment e2 = (Enrollment)f.nextElement();
                if (!e1.isOverlapping(e2) && iDC.nrConflicts(e1, e2)==0) return false;
            }
        }
        return true;
    }
    
    public CSVFile createTable(boolean includeLastLikeStudents, boolean includeRealStudents) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("Course1"),
                new CSVFile.CSVField("Course2"),
                new CSVFile.CSVField("NrStud"),
                new CSVFile.CSVField("AvgDist"),
                new CSVFile.CSVField("NoAlt"),
                new CSVFile.CSVField("Reason")
        });
        HashSet confs = iDC.computeAllConflicts();
        Hashtable distConfTable = new Hashtable();
        for (Iterator i=confs.iterator();i.hasNext();) {
            Conflict conflict = (Conflict)i.next();
            if (conflict.getStudent().isDummy() && !includeLastLikeStudents) continue;
            if (!conflict.getStudent().isDummy() && !includeRealStudents) continue;
            Section s1=conflict.getS1(), s2=conflict.getS2();
            Course c1 = null, c2=null;
            Request r1 = null, r2=null;
            for (Enumeration e=conflict.getStudent().getRequests().elements();e.hasMoreElements();) {
                Request request = (Request)e.nextElement();
                Enrollment enrollment = (Enrollment)request.getAssignment();
                if (enrollment==null || !enrollment.isCourseRequest()) continue;
                if (c1==null && enrollment.getAssignments().contains(s1)) {
                    c1 = enrollment.getConfig().getOffering().getCourse(conflict.getStudent());
                    r1 = request;
                }
                if (c2==null && enrollment.getAssignments().contains(s2)) {
                    c2 = enrollment.getConfig().getOffering().getCourse(conflict.getStudent());
                    r2 = request;
                }
            }
            if (c1==null) {
                sLog.error("Unable to find a course for "+s1); continue;
            }
            if (c2==null) {
                sLog.error("Unable to find a course for "+s2); continue;
            }
            if (c1.getName().compareTo(c2.getName())>0) {
                Course x = c1; c1 = c2; c2 = x;
                Section y = s1; s1 = s2; s2 = y;
            }
            if (c1.equals(c2) && s1.getName().compareTo(s2.getName())>0) {
                Section y = s1; s1 = s2; s2 = y;
            }
            Hashtable firstCourseTable = (Hashtable)distConfTable.get(c1);
            if (firstCourseTable==null) {
                firstCourseTable = new Hashtable();
                distConfTable.put(c1, firstCourseTable);
            }
            Object[] secondCourseTable = (Object[])firstCourseTable.get(c2);
            double nrStud = (secondCourseTable==null?0.0:((Double)secondCourseTable[0]).doubleValue()) + conflict.getWeight();
            double dist = (secondCourseTable==null?0.0:((Double)secondCourseTable[1]).doubleValue()) + (conflict.getDistance() * conflict.getWeight());
            boolean hard = (secondCourseTable==null?areInHardConfict(r1,r2):((Boolean)secondCourseTable[2]).booleanValue());
            HashSet expl = (HashSet)(secondCourseTable==null?null:secondCourseTable[3]);
            if (expl==null) expl = new HashSet();
            expl.add(s1.getSubpart().getName()+" "+s1.getTime().getLongName()+" "+s1.getPlacement().getRoomName(",")+
                    " vs "+
                    s2.getSubpart().getName()+" "+s2.getTime().getLongName()+" "+s2.getPlacement().getRoomName(","));
            firstCourseTable.put(c2, new Object[] {new Double(nrStud), new Double(dist), new Boolean(hard), expl});
        }
        for (Iterator i=distConfTable.entrySet().iterator();i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Course c1 = (Course)entry.getKey();
            Hashtable firstCourseTable = (Hashtable)entry.getValue();
            for (Iterator j=firstCourseTable.entrySet().iterator();j.hasNext();) {
                Map.Entry entry2 = (Map.Entry)j.next();
                Course c2 = (Course)entry2.getKey();
                Object[] secondCourseTable = (Object[])entry2.getValue();
                HashSet expl = (HashSet)secondCourseTable[3];
                String explStr = "";
                for (Iterator k=new TreeSet(expl).iterator();k.hasNext();)
                    explStr += k.next() + (k.hasNext()?"\n":"");
                double nrStud = ((Double)secondCourseTable[0]).doubleValue();
                double dist = ((Double)secondCourseTable[1]).doubleValue()/nrStud;
                csv.addLine(new CSVFile.CSVField[] {
                   new CSVFile.CSVField(c1.getName()),
                   new CSVFile.CSVField(c2.getName()),
                   new CSVFile.CSVField(sDF.format(nrStud)),
                   new CSVFile.CSVField(sDF.format(dist)),
                   new CSVFile.CSVField(((Boolean)secondCourseTable[2]).booleanValue()?"Y":"N"),
                   new CSVFile.CSVField(explStr)
                });
             }
        }
        return csv;
    }
}
