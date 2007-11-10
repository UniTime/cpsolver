package net.sf.cpsolver.exam.model;

import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.EnumerableHashSet;
import net.sf.cpsolver.ifs.util.ToolBox;

public class ExamModel extends Model {
    private static Logger sLog = Logger.getLogger(ExamModel.class); 
    private DataProperties iProperties = null;
    private int iMaxRooms = 4;
    private Vector iPeriods = new Vector();
    private Vector iRooms = new Vector();
    private Vector iStudents = new Vector();
    
    private boolean iDayBreakBackToBack = true;
    private double iDirectConflictWeight = 100.0;
    private double iBackToBackConflictWeight = 10.0;
    private double iMoreThanTwoADayWeight = 1.0;
    private double iEnrollmentFactor = 5.0;
    private double iPeriodWeight = 0.2;
    private double iRoomSizeWeight = 0.0001;
    private double iRoomLocationWeight = 0.0001;
    private double iRoomSplitWeight = 1.0;
    private double iNotOriginalRoomWeight = 0.1;

    private int iNrDirectConflicts = 0;
    private int iNrBackToBackConflicts = 0;
    private int iNrMoreThanTwoADayConflicts = 0;
    private int iCentrCoordX = -1, iCentrCoordY = -1;
    private double iRoomSizePenalty = 0;
    private double iRoomLocationPenalty = 0;
    private double iRoomSplitPenalty = 0;
    private double iPeriodPenalty = 0;
    private double iNotOriginalRoomPenalty = 0;
    
    public ExamModel(DataProperties properties) {
        iAssignedVariables = new EnumerableHashSet();
        iUnassignedVariables = new EnumerableHashSet();
        iPerturbVariables = new EnumerableHashSet();
        iProperties = properties;
    }
    
    public void init() {
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            for (Enumeration f=exam.getRooms().elements();f.hasMoreElements();) {
                ExamRoom room = (ExamRoom)f.nextElement();
                room.addVariable(exam);
            }
        }
    }
    
    private Vector iRoomGroups = new Vector();
    
    public int getMaxRooms() {
        return iMaxRooms;
    }
    public void setMaxRooms(int maxRooms) {
        iMaxRooms = maxRooms;
    }
    
    public void addPeriod(String day, String time, int length, double weight) {
        ExamPeriod lastPeriod = (iPeriods.isEmpty()?null:(ExamPeriod)iPeriods.lastElement());
        ExamPeriod p = new ExamPeriod(day, time, length, weight);
        if (lastPeriod==null)
            p.setIndex(iPeriods.size(),0,0);
        else if (lastPeriod.getDayStr().equals(day)) {
            p.setIndex(iPeriods.size(), lastPeriod.getDay(), lastPeriod.getTime()+1);
        } else
            p.setIndex(iPeriods.size(), lastPeriod.getDay()+1, 0);
        if (lastPeriod!=null) {
            lastPeriod.setNext(p);
            p.setPrev(lastPeriod);
        }
        iPeriods.add(p);
    }
    
    public int getNrDays() {
        return ((ExamPeriod)iPeriods.lastElement()).getDay()+1;
    }
    public int getNrPeriods() {
        return iPeriods.size();
    }
    public Vector getPeriods() {
        return iPeriods;
    }
    public ExamPeriod getPeriod(int period) {
        return (ExamPeriod)iPeriods.elementAt(period);
    }
    
    public double getDirectConflictWeight() {
        return iDirectConflictWeight;
    }
    public void setDirectConflictWeight(double directConflictWeight) {
        iDirectConflictWeight = directConflictWeight;
    }
    public double getBackToBackConflictWeight() {
        return iBackToBackConflictWeight;
    }
    public void setBackToBackConflictWeight(double backToBackConflictWeight) {
        iBackToBackConflictWeight = backToBackConflictWeight;
    }
    public double getMoreThanTwoADayWeight() {
        return iMoreThanTwoADayWeight;
    }
    public void setMoreThanTwoADayWeight(double moreThanTwoADayWeight) {
        iMoreThanTwoADayWeight = moreThanTwoADayWeight;
    }
    public double getEnrollmentFactor() {
        return iEnrollmentFactor;
    }
    public void setEnrollmentFactor(double enrollmentFactor) {
        iEnrollmentFactor = enrollmentFactor;
    }
    public boolean isDayBreakBackToBack() {
        return iDayBreakBackToBack;
    }
    public void setDayBreakBackToBack(boolean dayBreakBackToBack) {
        iDayBreakBackToBack = dayBreakBackToBack;
    }
    public int getCentralCoordX() {
        return iCentrCoordX;
    }
    public int getCentralCoordY() {
        return iCentrCoordY;
    }
    public void setCentralCoordinates(int coordX, int coordY) {
        iCentrCoordX = coordX;
        iCentrCoordY = coordY;
    }
    public double getPeriodWeight() {
        return iPeriodWeight;
    }
    public void setPeriodWeight(double periodWeight) {
        iPeriodWeight = periodWeight;
    }
    public double getRoomSizeWeight() {
        return iRoomSizeWeight;
    }
    public void setRoomSizeWeight(double roomSizeWeight) {
        iRoomSizeWeight = roomSizeWeight;
    }
    public double getRoomSplitWeight() {
        return iRoomSplitWeight;
    }
    public void setRoomSplitWeight(double roomSplitWeight) {
        iRoomSplitWeight = roomSplitWeight;
    }
    public double getRoomLocationWeight() {
        return iRoomLocationWeight;
    }
    public void setRoomLocationWeight(double roomLocationWeight) {
        iRoomLocationWeight = roomLocationWeight;
    }
    public double getNotOriginalRoomWeight() {
        return iNotOriginalRoomWeight;
    }
    public void setNotOriginalRoomWeight(double notOriginalRoomWeight) {
        iNotOriginalRoomWeight = notOriginalRoomWeight;
    }
    
    public void beforeUnassigned(long iteration, Value value) {
        super.beforeUnassigned(iteration, value);
        ExamPlacement placement = (ExamPlacement)value;
        iNrDirectConflicts -= placement.getNrDirectConflicts();
        iNrBackToBackConflicts -= placement.getNrBackToBackConflicts();
        iNrMoreThanTwoADayConflicts -= placement.getNrMoreThanTwoADayConflicts();
        iRoomSizePenalty -= placement.getRoomSizePenalty();
        iRoomLocationPenalty -= placement.getRoomLocationPenalty();
        iRoomSplitPenalty -= placement.getRoomSplitPenalty();
        iPeriodPenalty -= placement.getPeriodPenalty();
        iNotOriginalRoomPenalty -= placement.getNotOriginalRoomPenalty();
    }
    
    public void afterAssigned(long iteration, Value value) {
        super.afterAssigned(iteration, value);
        ExamPlacement placement = (ExamPlacement)value;
        iNrDirectConflicts += placement.getNrDirectConflicts();
        iNrBackToBackConflicts += placement.getNrBackToBackConflicts();
        iNrMoreThanTwoADayConflicts += placement.getNrMoreThanTwoADayConflicts();
        iRoomSizePenalty += placement.getRoomSizePenalty();
        iRoomLocationPenalty += placement.getRoomLocationPenalty();
        iRoomSplitPenalty += placement.getRoomSplitPenalty();
        iPeriodPenalty += placement.getPeriodPenalty();
        iNotOriginalRoomPenalty += placement.getNotOriginalRoomPenalty();
    }
    
    public double getTotalValue() {
        return 
            getDirectConflictWeight()*getNrDirectConflicts(false)+
            getBackToBackConflictWeight()*getNrBackToBackConflicts(false)+
            getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts(false)+
            getPeriodWeight()*getPeriodPenalty(false)+
            getRoomSizeWeight()*getRoomSizePenalty(false)+
            getRoomLocationWeight()*getRoomLocationPenalty(false)+
            getRoomSplitWeight()*getRoomSplitPenalty(false)+
            getNotOriginalRoomWeight()*getNotOriginalRoomPenalty(false);
    }
    
    public double[] getTotalMultiValue() {
        return new double[] {
                getDirectConflictWeight()*getNrDirectConflicts(false),
                getBackToBackConflictWeight()*getNrBackToBackConflicts(false),
                getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts(false),
                getPeriodWeight()*getPeriodPenalty(false),
                getRoomSizeWeight()*getRoomSizePenalty(false),
                getRoomLocationWeight()*getRoomLocationPenalty(false),
                getRoomSplitWeight()*getRoomSplitPenalty(false),
                getNotOriginalRoomWeight()*getNotOriginalRoomPenalty(false)
        };
    }
    
    public String toString() {
        return 
            "DC:"+getNrDirectConflicts(false)+","+
            "BTB:"+getNrBackToBackConflicts(false)+","+
            "M2D:"+getNrMoreThanTwoADayConflicts(false)+","+
            "PP:"+sDoubleFormat.format(getPeriodPenalty(false)/1000)+"k,"+
            "RSz:"+sDoubleFormat.format(getRoomSizePenalty(false)/1000)+"k,"+
            "RLc:"+sDoubleFormat.format(getRoomLocationPenalty(false)/1000)+"k,"+
            "RSp:"+sDoubleFormat.format(getRoomSplitPenalty(false)/1000)+"k,"+
            "ROg:"+sDoubleFormat.format(getNotOriginalRoomPenalty(false)/1000)+"k";
        /*
        return 
        sDoubleFormat.format(getTotalValue())+"/"+
        "DC:"+sDoubleFormat.format(getDirectConflictWeight()*getNrDirectConflicts(false))+","+
        "BTB:"+sDoubleFormat.format(getBackToBackConflictWeight()*getNrBackToBackConflicts(false))+","+
        "M2D:"+sDoubleFormat.format(getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts(false))+","+
        "PP:"+sDoubleFormat.format(getPeriodWeight()*getPeriodPenalty(false))+","+
        "RSz:"+sDoubleFormat.format(getRoomSizeWeight()*getRoomSizePenalty(false))+","+
        "RLc:"+sDoubleFormat.format(getRoomLocationWeight()*getRoomLocationPenalty(false))+","+
        "RSp:"+sDoubleFormat.format(getRoomSplitWeight()*getRoomSplitPenalty(false))+","+
        "ROg:"+sDoubleFormat.format(getNotOriginalRoomWeight()*getNotOriginalRoomPenalty(false));
        */
    }
    
    public int getNrDirectConflicts(boolean precise) {
        if (!precise) return iNrDirectConflicts;
        int conflicts = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                int nrExams = student.getExams(period).size();
                if (nrExams>1) conflicts += nrExams-1;
            }
        }
        return conflicts;
    }
    
    public int getNrBackToBackConflicts(boolean precise) {
        if (!precise) return iNrBackToBackConflicts;
        int conflicts = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                int nrExams = student.getExams(period).size();
                if (nrExams==0) continue;
                if (period.next()!=null && !student.getExams(period.next()).isEmpty() && (!isDayBreakBackToBack() || period.next().getDay()==period.getDay())) 
                    conflicts += nrExams*student.getExams(period.next()).size();
            }
        }
        return conflicts;
    }
    
    public int getNrMoreThanTwoADayConflicts(boolean precise) {
        if (!precise) return iNrMoreThanTwoADayConflicts;
        int conflicts = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            for (int d=0;d<getNrDays();d++) {
                int nrExams = student.getExamsADay(d).size();
                if (nrExams>2)
                    conflicts += nrExams-2;
            }
        }
        return conflicts;
    }
    
    public double getRoomSizePenalty(boolean precise) {
        if (!precise) return iRoomSizePenalty;
        double penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomSizePenalty();
        }
        return penalty;
    }
    
    public double getRoomLocationPenalty(boolean precise) {
        if (!precise) return iRoomLocationPenalty;
        double penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomLocationPenalty();
        }
        return penalty;
    }

    public double getRoomSplitPenalty(boolean precise) {
        if (!precise) return iRoomSplitPenalty;
        double penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getRoomSplitPenalty();
        }
        return penalty;
    }

    public double getPeriodPenalty(boolean precise) {
        if (!precise) return iPeriodPenalty;
        double penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getPeriodPenalty();
        }
        return penalty;
    }
    
    public double getNotOriginalRoomPenalty(boolean precise) {
        if (!precise) return iNotOriginalRoomPenalty;
        double penalty = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            penalty += ((ExamPlacement)((Exam)e.nextElement()).getAssignment()).getNotOriginalRoomPenalty();
        }
        return penalty;
    }

    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("Direct Conflicts",String.valueOf(getNrDirectConflicts(false)));
        info.put("Back-To-Back Conflicts",String.valueOf(getNrBackToBackConflicts(false)));
        info.put("More Than 2 A Day Conflicts",String.valueOf(getNrMoreThanTwoADayConflicts(false)));
        info.put("Room Size Penalty",sDoubleFormat.format(getRoomSizePenalty(false)));
        info.put("Room Split Penalty",sDoubleFormat.format(getRoomSplitPenalty(false)));
        info.put("Room Location Penalty",sDoubleFormat.format(getRoomLocationPenalty(false)));
        info.put("Period Penalty",sDoubleFormat.format(getPeriodPenalty(false)));
        info.put("Not-Original Room Penalty",sDoubleFormat.format(getNotOriginalRoomPenalty(false)));
        return info;
    }
    
    public Hashtable getExtendedInfo() {
        Hashtable info = super.getExtendedInfo();
        info.put("Direct Conflicts [p]",String.valueOf(getNrDirectConflicts(true)));
        info.put("Back-To-Back Conflicts [p]",String.valueOf(getNrBackToBackConflicts(true)));
        info.put("More Than 2 A Day Conflicts [p]",String.valueOf(getNrMoreThanTwoADayConflicts(true)));
        info.put("Room Size Penalty [p]",sDoubleFormat.format(getRoomSizePenalty(true)));
        info.put("Room Split Penalty [p]",sDoubleFormat.format(getRoomSplitPenalty(true)));
        info.put("Room Location Penalty [p]",sDoubleFormat.format(getRoomLocationPenalty(true)));
        info.put("Period Penalty [p]",sDoubleFormat.format(getPeriodPenalty(true)));
        info.put("Not-Original Room Penalty [p]",sDoubleFormat.format(getNotOriginalRoomPenalty(true)));
        info.put("Number of Periods",String.valueOf(getPeriods().size()));
        info.put("Number of Exams",String.valueOf(variables().size()));
        info.put("Number of Rooms",String.valueOf(getRooms().size()));
        String rgAllId = "A";
        int avail = 0, availAlt = 0;
        for (Enumeration e=getRoomGroups().elements();e.hasMoreElements();) {
            ExamRoomGroup rg = (ExamRoomGroup)e.nextElement();
            if (rg.getRooms().isEmpty()) continue;
            info.put("Number of Rooms (group "+rg.getId()+")",String.valueOf(rg.getRooms().size()));
            if (rgAllId.equals(rg.getId())) {
                for (Enumeration f=rg.getRooms().elements();f.hasMoreElements();) {
                    ExamRoom room = (ExamRoom)f.nextElement();
                    for (Enumeration g=getPeriods().elements();g.hasMoreElements();) {
                        ExamPeriod period = (ExamPeriod)g.nextElement();
                        if (room.isAvailable(period)) {
                            avail+=room.getSize();
                            availAlt+=room.getAltSize();
                        }
                    }
                }
            }
            info.put("Space in Rooms (Group "+rg.getId()+")",
                    rg.getSpace()+
                    " (min:"+rg.getMinSize()+", max:"+rg.getMaxSize()+
                    ", avg:"+rg.getAvgSize()+", med:"+rg.getMedSize()+")");
            info.put("Space in Rooms (Alternative, Group "+rg.getId()+")",
                    rg.getAltSpace()+
                    " (min:"+rg.getMinAltSize()+", max:"+rg.getMaxAltSize()+
                    ", avg:"+rg.getAvgAltSize()+", med:"+rg.getMedAltSize()+")");
        }
        info.put("Number of Students",String.valueOf(getStudents().size()));
        int nrStudentExams = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            nrStudentExams += student.variables().size();
        }
        info.put("Number of Student Exams",String.valueOf(nrStudentExams));
        int nrAltExams = 0, nrSectionExams = 0, nrOrigRoomExams = 0, nrPreassignedTime = 0, nrPreassignedRoom = 0, nrSmallExams = 0;
        double fill = 0;
        double altRatio = ((double)avail)/availAlt;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (exam.getMaxRooms()>0)
                fill += (exam.hasAltSeating()?altRatio:1.0)*exam.getStudents().size();
            if (exam.isSectionExam()) nrSectionExams++;
            if (exam.hasAltSeating()) nrAltExams++;
            if (exam.getOriginalRoom()!=null) nrOrigRoomExams++;
            if (exam.hasPreAssignedPeriod()) nrPreassignedTime++;
            if (exam.hasPreAssignedRooms()) nrPreassignedRoom++;
            if (exam.getMaxRooms()==0) nrSmallExams++;
        }
        info.put("Estimated Schedule Infilling (Group "+rgAllId+")", sDoubleFormat.format(100.0*fill/avail)+"% ("+Math.round(fill)+" of "+avail+")");
        info.put("Number of Exams Requiring Alt Seating",String.valueOf(nrAltExams));
        info.put("Number of Small Exams (Exams W/O Room)",String.valueOf(nrSmallExams));
        info.put("Number of Section Exams",String.valueOf(nrSectionExams));
        info.put("Number of Course Exams",String.valueOf(variables().size()-nrSectionExams));
        info.put("Number of Exams With Original Room",String.valueOf(nrOrigRoomExams));
        info.put("Number of Exams With Pre-Assigned Time",String.valueOf(nrPreassignedTime));
        info.put("Number of Exams With Pre-Assigned Room",String.valueOf(nrPreassignedRoom));
        
        return info;
    }

    public Vector getRoomGroups() {
        return iRoomGroups;
    }
    
    public DataProperties getProperties() {
        return iProperties;
    }
    
    public Vector getRooms() { return iRooms; }
    public Vector getStudents() { return iStudents; }
    
    public Document save(boolean saveInitial, boolean conflictTable) {
        Document document = DocumentHelper.createDocument();
        document.addComment("Examination Timetable");
        if (!assignedVariables().isEmpty()) {
            StringBuffer comments = new StringBuffer("Solution Info:\n");
            Dictionary solutionInfo=getExtendedInfo();
            for (Enumeration e=ToolBox.sortEnumeration(solutionInfo.keys());e.hasMoreElements();) {
                String key = (String)e.nextElement();
                Object value = solutionInfo.get(key);
                comments.append("    "+key+": "+value+"\n");
            }
            document.addComment(comments.toString());
        }
        Element root = document.addElement("examtt");
        root.addAttribute("version","1.0");
        root.addAttribute("initiative", getProperties().getProperty("Data.Initiative"));
        root.addAttribute("term", getProperties().getProperty("Data.Term"));
        root.addAttribute("year", getProperties().getProperty("Data.Year"));
        root.addAttribute("created", String.valueOf(new Date()));
        Element params = root.addElement("parameters");
        params.addElement("property").addAttribute("name", "isDayBreakBackToBack").addAttribute("value", (isDayBreakBackToBack()?"true":"false"));
        params.addElement("property").addAttribute("name", "directConflictWeight").addAttribute("value", String.valueOf(getDirectConflictWeight()));
        params.addElement("property").addAttribute("name", "backToBackConflictWeight").addAttribute("value", String.valueOf(getBackToBackConflictWeight()));
        params.addElement("property").addAttribute("name", "moreThanTwoADayWeight").addAttribute("value", String.valueOf(getMoreThanTwoADayWeight()));
        params.addElement("property").addAttribute("name", "enrollmentFactor").addAttribute("value", String.valueOf(getEnrollmentFactor()));
        params.addElement("property").addAttribute("name", "maxRooms").addAttribute("value", String.valueOf(getMaxRooms()));
        params.addElement("property").addAttribute("name", "periodWeight").addAttribute("value", String.valueOf(getPeriodWeight()));
        params.addElement("property").addAttribute("name", "roomSizeWeight").addAttribute("value", String.valueOf(getRoomSizeWeight()));
        params.addElement("property").addAttribute("name", "roomLocationWeight").addAttribute("value", String.valueOf(getRoomLocationWeight()));
        params.addElement("property").addAttribute("name", "roomSplitWeight").addAttribute("value", String.valueOf(getRoomSplitWeight()));
        params.addElement("property").addAttribute("name", "notOriginalRoomWeight").addAttribute("value", String.valueOf(getNotOriginalRoomWeight()));
        if (getCentralCoordX()>=0 && getCentralCoordY()>=0)
            params.addElement("property").addAttribute("name", "centralCoordinates").addAttribute("value", getCentralCoordX()+","+getCentralCoordY());
        Element periods = root.addElement("periods");
        for (Enumeration e=getPeriods().elements();e.hasMoreElements();) {
            ExamPeriod period = (ExamPeriod)e.nextElement();
            periods.addElement("period").
                addAttribute("id", String.valueOf(period.getIndex())).
                addAttribute("length", String.valueOf(period.getLength())).
                addAttribute("day", period.getDayStr()).
                addAttribute("time", period.getTimeStr()).
                addAttribute("weight", String.valueOf(period.getWeight()));
        }
        Element rooms = root.addElement("rooms");
        for (Enumeration e=getRooms().elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            Element r = rooms.addElement("room");
            r.addAttribute("id", room.getRoomId());
            r.addAttribute("size", String.valueOf(room.getSize()));
            r.addAttribute("alt", String.valueOf(room.getAltSize()));
            if (room.getCoordX()>=0 && room.getCoordY()>=0)
                r.addAttribute("coordinates", room.getCoordX()+","+room.getCoordY());
            String gr = "";
            for (Enumeration f=getRoomGroups().elements();f.hasMoreElements();) {
                ExamRoomGroup rg = (ExamRoomGroup)f.nextElement();
                if (rg.getRooms().contains(room)) {
                    if (gr.length()>0) gr+=",";
                    gr+=rg.getId();
                }
            }
            if (gr.length()>0)
                r.addAttribute("groups", gr);
            String available = "";
            boolean allAvail = true;
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                available += room.isAvailable(period)?"1":"0";
                if (!room.isAvailable(period)) allAvail=false;
            }
            if (!allAvail)
                r.addAttribute("available", available);
        }
        Element exams = root.addElement("exams");
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            Element ex = exams.addElement("exam");
            ex.addAttribute("id", String.valueOf(exam.getId()));
            ex.addAttribute("length", String.valueOf(exam.getLength()));
            ex.addAttribute("type", (exam.isSectionExam()?"section":"course"));
            ex.addAttribute("alt", (exam.hasAltSeating()?"true":"false"));
            if (exam.getMaxRooms()!=getMaxRooms())
                ex.addAttribute("maxRooms", String.valueOf(exam.getMaxRooms()));
            ex.addAttribute("enrl", String.valueOf(exam.getStudents().size()));
            if (exam.getOriginalRoom()!=null)
                ex.addElement("original-room").addAttribute("id", exam.getOriginalRoom().getRoomId());
            if (exam.hasPreAssignedPeriod() || !exam.getPreassignedRooms().isEmpty()) {
                Element pre = ex.addElement("pre-assigned");
                if (exam.hasPreAssignedPeriod())
                    pre.addElement("period").addAttribute("id", String.valueOf(exam.getPreAssignedPeriod().getIndex()));
                for (Iterator i=exam.getPreassignedRooms().iterator();i.hasNext();) {
                    ExamRoom r = (ExamRoom)i.next();
                    pre.addElement("room").addAttribute("id", r.getRoomId());
                }
            }
            String available = "";
            boolean allAvail = true;
            for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                available += exam.isAvailable(period)?"1":"0";
                if (!exam.isAvailable(period)) allAvail=false;
            }
            if (!allAvail)
                ex.addAttribute("available", available);
            if (exam.hasAveragePeriod())
                ex.addAttribute("average", String.valueOf(exam.getAveragePeriod()));
            String rgs = "";
            for (Enumeration f=exam.getRoomGroups().elements();f.hasMoreElements();) {
                ExamRoomGroup rg = (ExamRoomGroup)f.nextElement();
                if (rg.getRooms().isEmpty()) continue;
                if (rgs.length()>0) rgs+=",";
                rgs+=rg.getId();
            }
            if (rgs.length()>0) ex.addAttribute("groups", rgs);
            ExamPlacement p = (ExamPlacement)exam.getAssignment();
            if (p!=null) {
                Element asg = ex.addElement("assignment");
                asg.addElement("period").addAttribute("id", String.valueOf(p.getPeriod().getIndex()));
                for (Iterator i=p.getRooms().iterator();i.hasNext();) {
                    ExamRoom r = (ExamRoom)i.next();
                    asg.addElement("room").addAttribute("id", r.getRoomId());
                }            }
            p = (ExamPlacement)exam.getInitialAssignment();
            if (p!=null && saveInitial) {
                Element ini = ex.addElement("initial");
                ini.addElement("period").addAttribute("id", String.valueOf(p.getPeriod().getIndex()));
                for (Iterator i=p.getRooms().iterator();i.hasNext();) {
                    ExamRoom r = (ExamRoom)i.next();
                    ini.addElement("room").addAttribute("id", r.getRoomId());
                }
            }
        }
        Element students = root.addElement("students");
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            Element s = students.addElement("student");
            s.addAttribute("id", student.getStudentId());
            for (Enumeration f=student.variables().elements();f.hasMoreElements();) {
                Exam ex = (Exam)f.nextElement();
                s.addElement("exam").addAttribute("id", String.valueOf(ex.getId()));
            }
        }
        if (conflictTable) {
            Element conflicts = root.addElement("conflicts");
            for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)f.nextElement();
                    int nrExams = student.getExams(period).size();
                    if (nrExams>1) {
                        Element dir = conflicts.addElement("direct").addAttribute("student", student.getStudentId());
                        for (Iterator i=student.getExams(period).iterator();i.hasNext();) {
                            Exam exam = (Exam)i.next();
                            dir.addElement("exam").addAttribute("id",String.valueOf(exam.getId()));
                        }
                    }
                    if (nrExams>0) {
                        if (period.next()!=null && !student.getExams(period.next()).isEmpty() && (!isDayBreakBackToBack() || period.next().getDay()==period.getDay())) {
                            for (Iterator i=student.getExams(period).iterator();i.hasNext();) {
                                Exam ex1 = (Exam)i.next();
                                for (Iterator j=student.getExams(period.next()).iterator();j.hasNext();) {
                                    Exam ex2 = (Exam)j.next();
                                    Element btb = conflicts.addElement("back-to-back").addAttribute("student", student.getStudentId());
                                    btb.addElement("exam").addAttribute("id",String.valueOf(ex1.getId()));
                                    btb.addElement("exam").addAttribute("id",String.valueOf(ex2.getId()));
                                }
                            }
                        }
                    }
                    if (period.next()==null || period.next().getDay()!=period.getDay()) {
                        int nrExamsADay = student.getExamsADay(period.getDay()).size();
                        if (nrExamsADay>2) {
                            Element mt2 = conflicts.addElement("more-2-day").addAttribute("student", student.getStudentId());
                            for (Iterator i=student.getExamsADay(period.getDay()).iterator();i.hasNext();) {
                                Exam exam = (Exam)i.next();
                                mt2.addElement("exam").addAttribute("id",String.valueOf(exam.getId()));
                            }
                        }
                    }
                }
            }
            
        }
        return document;
    }
    
    public boolean load(Document document, boolean loadInitial, boolean loadAssignments) {
        Element root=document.getRootElement();
        if (!"examtt".equals(root.getName())) return false;
        if (root.attribute("initiative")!=null)
            getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));
        if (root.attribute("term")!=null)
            getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attribute("year")!=null)
            getProperties().setProperty("Data.Year", root.attributeValue("year"));
        for (Iterator i=root.element("parameters").elementIterator("property");i.hasNext();) {
            Element e = (Element)i.next();
            String name = e.attributeValue("name");
            String value = e.attributeValue("value");
            if ("isDayBreakBackToBack".equals(name)) setDayBreakBackToBack("true".equals(value));
            else if ("directConflictWeight".equals(name)) setDirectConflictWeight(Double.parseDouble(value));
            else if ("backToBackConflictWeight".equals(name)) setBackToBackConflictWeight(Double.parseDouble(value));
            else if ("moreThanTwoADayWeight".equals(name)) setMoreThanTwoADayWeight(Double.parseDouble(value));
            else if ("enrollmentFactor".equals(name)) setEnrollmentFactor(Double.parseDouble(value));
            else if ("maxRooms".equals(name)) setMaxRooms(Integer.parseInt(value));
            else if ("periodWeight".equals(name)) setPeriodWeight(Double.parseDouble(value));
            else if ("roomSizeWeight".equals(name)) setRoomSizeWeight(Double.parseDouble(value));
            else if ("roomLocationWeight".equals(name)) setRoomLocationWeight(Double.parseDouble(value));
            else if ("roomSplitWeight".equals(name)) setRoomSplitWeight(Double.parseDouble(value));
            else if ("notOriginalRoomWeight".equals(name)) setNotOriginalRoomWeight(Double.parseDouble(value));
            else if ("centralCoordinates".equals(name)) setCentralCoordinates(Integer.parseInt(value.substring(0,value.indexOf(','))), Integer.parseInt(value.substring(value.indexOf(',')+1)));
            else getProperties().setProperty(name, value);
        }
        for (Iterator i=root.element("periods").elementIterator("period");i.hasNext();) {
            Element e = (Element)i.next();
            addPeriod(e.attributeValue("day"), e.attributeValue("time"), Integer.parseInt(e.attributeValue("length")), Double.parseDouble(e.attributeValue("weight")));
        }
        Hashtable rooms = new Hashtable();
        Hashtable roomGroups = new Hashtable();
        for (Iterator i=root.element("rooms").elementIterator("room");i.hasNext();) {
            Element e = (Element)i.next();
            String coords = e.attributeValue("coordinates"); 
            ExamRoom room = new ExamRoom(this,
                    e.attributeValue("id"),
                    Integer.parseInt(e.attributeValue("size")),
                    Integer.parseInt(e.attributeValue("alt")),
                    (coords==null?-1:Integer.parseInt(coords.substring(0,coords.indexOf(',')))),
                    (coords==null?-1:Integer.parseInt(coords.substring(coords.indexOf(',')+1))));
            addConstraint(room);
            getRooms().add(room);
            rooms.put(room.getRoomId(),room);
            String available = e.attributeValue("available");
            if (available!=null)
                for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)f.nextElement();
                    if (available.charAt(period.getIndex())=='0') room.setAvailable(period.getIndex(), false);
                }
            String rg = e.attributeValue("groups");
            if (rg!=null) for (StringTokenizer stk=new StringTokenizer(rg,",");stk.hasMoreTokens();) {
                String roomGroupId = (String)stk.nextToken();
                ExamRoomGroup gr = (ExamRoomGroup)roomGroups.get(roomGroupId);
                if (gr==null) {
                    gr = new ExamRoomGroup(roomGroupId);
                    getRoomGroups().add(gr);
                    roomGroups.put(roomGroupId,gr);
                }
                gr.addRoom(room);
            }
        }
        Vector assignments = new Vector();
        Hashtable exams = new Hashtable();
        for (Iterator i=root.element("exams").elementIterator("exam");i.hasNext();) {
            Element e = (Element)i.next();
            Exam exam = new Exam(
                    Long.parseLong(e.attributeValue("id")),
                    Integer.parseInt(e.attributeValue("length")),
                    "section".equals(e.attributeValue("type")),
                    "true".equals(e.attributeValue("alt")),
                    (e.attribute("maxRooms")==null?getMaxRooms():Integer.parseInt(e.attributeValue("maxRooms"))));
            exams.put(new Long(exam.getId()),exam);
            if (e.element("original-room")!=null)
                exam.setOriginalRoom((ExamRoom)rooms.get(e.element("original-room").attributeValue("id")));
            Element pre = e.element("pre-assigned");
            if (pre!=null) {
                Element per = pre.element("period");
                if (per!=null)
                    exam.setPreAssignedPeriod(getPeriod(Integer.parseInt(per.attributeValue("id"))));
                Element rm = pre.element("room");
                for (Iterator j=pre.elementIterator("room");j.hasNext();) {
                    String roomId = (String)((Element)j.next()).attributeValue("id");
                    exam.getPreassignedRooms().add((ExamRoom)rooms.get(roomId));
                }
            }
            addVariable(exam);
            String available = e.attributeValue("available");
            if (available!=null)
                for (Enumeration f=getPeriods().elements();f.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)f.nextElement();
                    if (available.charAt(period.getIndex())=='0') exam.setAvailable(period.getIndex(), false);
                }
            if (e.attribute("average")!=null)
                exam.setAveragePeriod(Integer.parseInt(e.attributeValue("average")));
            String rgs = e.attributeValue("groups");
            if (rgs!=null)
                for (StringTokenizer s=new StringTokenizer(rgs,",");s.hasMoreTokens();) {
                    exam.addRoomGroup((ExamRoomGroup)roomGroups.get(s.nextToken()));
                }
            Element asg = e.element("assignment");
            if (asg!=null && loadAssignments) {
                Element per = asg.element("period");
                if (per!=null) {
                    ExamPlacement p = new ExamPlacement(exam, getPeriod(Integer.parseInt(per.attributeValue("id"))), new HashSet());
                    for (Iterator j=asg.elementIterator("room");j.hasNext();) {
                        String roomId = (String)((Element)j.next()).attributeValue("id");
                        p.getRooms().add((ExamRoom)rooms.get(roomId));
                    }
                    assignments.add(p);
                }
            }
            Element ini = e.element("initial");
            if (ini!=null && loadInitial) {
                Element per = ini.element("period");
                if (per!=null) {
                    ExamPlacement p = new ExamPlacement(exam, getPeriod(Integer.parseInt(per.attributeValue("id"))), new HashSet());
                    for (Iterator j=ini.elementIterator("room");j.hasNext();) {
                        String roomId = (String)((Element)j.next()).attributeValue("id");
                        p.getRooms().add((ExamRoom)rooms.get(roomId));
                    }
                    exam.setInitialAssignment(p);
                }
            }
        }
        for (Iterator i=root.element("students").elementIterator("student");i.hasNext();) {
            Element e = (Element)i.next();
            ExamStudent student = new ExamStudent(this,e.attributeValue("id"));
            for (Iterator j=e.elementIterator("exam");j.hasNext();) {
                student.addVariable((Exam)exams.get(Long.valueOf(((Element)j.next()).attributeValue("id"))));
            }
            addConstraint(student);
            getStudents().add(student);
        }        
        Element students = root.addElement("students");
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExamStudent student = (ExamStudent)e.nextElement();
            Element s = students.addElement("student");
            s.addAttribute("id", student.getStudentId());
            for (Enumeration f=student.variables().elements();f.hasMoreElements();) {
                Exam ex = (Exam)f.nextElement();
                s.addElement("exam").addAttribute("id", String.valueOf(ex.getId()));
            }
        }
        init();
        for (Enumeration e=assignments.elements();e.hasMoreElements();) {
            ExamPlacement placement = (ExamPlacement)e.nextElement();
            Exam exam = (Exam)placement.variable();
            Set conf = conflictValues(placement);
            if (!conf.isEmpty()) {
                for (Iterator i=conflictConstraints(placement).entrySet().iterator();i.hasNext();) {
                    Map.Entry entry = (Map.Entry)i.next();
                    Constraint constraint = (Constraint)entry.getKey();
                    Set values = (Set)entry.getValue();
                    if (constraint instanceof ExamStudent) {
                        ((ExamStudent)constraint).setAllowDirectConflicts(true);
                        exam.setAllowDirectConflicts(true);
                        for (Iterator j=values.iterator();j.hasNext();)
                            ((Exam)((ExamPlacement)j.next()).variable()).setAllowDirectConflicts(true);
                    }
                }
                conf = conflictValues(placement);
            }
            if (conf.isEmpty()) {
                exam.assign(0, exam.getInitialAssignment());
            } else {
                sLog.error("Unable to assign "+exam.getInitialAssignment().getName()+" to exam "+exam.getName());
                sLog.error("Conflicts:"+ToolBox.dict2string(conflictConstraints(exam.getInitialAssignment()), 2));
            }
        }
        for (Enumeration e=new Vector(unassignedVariables()).elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (!exam.hasPreAssignedPeriod()) continue;
            ExamPlacement placement = null;
            if (exam.hasPreAssignedRooms()) {
                placement = new ExamPlacement(exam, exam.getPreAssignedPeriod(), new HashSet(exam.getPreassignedRooms()));
            } else {
                Set bestRooms = exam.findBestAvailableRooms(exam.getPreAssignedPeriod());
                if (bestRooms==null) {
                    sLog.error("Unable to assign "+exam.getPreAssignedPeriod()+" to exam "+exam.getName()+" -- no suitable room found.");
                    continue;
                }
                placement = new ExamPlacement(exam, exam.getPreAssignedPeriod(), bestRooms);
            }
            Set conflicts = conflictValues(placement);
            if (!conflicts.isEmpty()) {
                for (Iterator i=conflictConstraints(placement).entrySet().iterator();i.hasNext();) {
                    Map.Entry entry = (Map.Entry)i.next();
                    Constraint constraint = (Constraint)entry.getKey();
                    Set values = (Set)entry.getValue();
                    if (constraint instanceof ExamStudent) {
                        ((ExamStudent)constraint).setAllowDirectConflicts(true);
                        exam.setAllowDirectConflicts(true);
                        for (Iterator j=values.iterator();j.hasNext();)
                            ((Exam)((ExamPlacement)j.next()).variable()).setAllowDirectConflicts(true);
                    }
                }
                conflicts = conflictValues(placement);
            }
            if (conflicts.isEmpty()) {
                exam.assign(0, placement);
            } else {
                sLog.error("Unable to assign "+placement.getName()+" to exam "+exam.getName());
                sLog.error("Conflicts:"+ToolBox.dict2string(conflictConstraints(exam.getInitialAssignment()), 2));
            }
        }
        return true;
    }
    
}
