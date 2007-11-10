package net.sf.cpsolver.exam.model;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;

public class ExamPlacement extends Value {
    private ExamPeriod iPeriod;
    private Set iRooms;
    private int iHashCode;
    
    public ExamPlacement(Exam exam, ExamPeriod period, Set rooms) {
        super(exam);
        iPeriod = period;
        iRooms = rooms;
        iHashCode = getName().hashCode();
    }
    
    public ExamPeriod getPeriod() { return iPeriod; }
    public Set getRooms() { return iRooms; }
    
    public int getNrDirectConflicts() {
        Exam exam = (Exam)variable();
        if (!exam.isAllowDirectConflicts()) return 0;
        int penalty = 0;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            Set exams = s.getExams(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam)?0:1);
            if (nrExams>1) penalty++;
        }
        return penalty;
    }
    
    public int getNrBackToBackConflicts() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        int penalty = 0;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            if (getPeriod().prev()!=null) {
                if (!model.isDayBreakBackToBack() || getPeriod().prev().getDay()==getPeriod().getDay()) {
                    Set exams = s.getExams(getPeriod().prev());
                    int nrExams = exams.size() + (exams.contains(exam)?-1:0);
                    penalty += nrExams;
                }
            }
            if (getPeriod().next()!=null) {
                if (!model.isDayBreakBackToBack() || getPeriod().next().getDay()==getPeriod().getDay()) {
                    Set exams = s.getExams(getPeriod().next());
                    int nrExams = exams.size() + (exams.contains(exam)?-1:0);
                    penalty += nrExams;
                }
            }
        }
        return penalty;
    }

    public int getNrMoreThanTwoADayConflicts() {
        Exam exam = (Exam)variable();
        int penalty = 0;
        for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
            ExamStudent s = (ExamStudent)e.nextElement();
            Set exams = s.getExamsADay(getPeriod());
            int nrExams = exams.size() + (exams.contains(exam)?0:1);
            if (nrExams>2) penalty++;
        }
        return penalty;
    }
    
    private Double iRoomLocationPenalty = null;
    public double getRoomLocationPenalty() {
        if (iRoomLocationPenalty==null)
            iRoomLocationPenalty = new Double(((Exam)variable()).getRoomLocationPenalty(getRooms()));
        return iRoomLocationPenalty.doubleValue();
    }

    private Double iRoomSizePenalty = null;
    public double getRoomSizePenalty() {
        if (iRoomSizePenalty==null)
            iRoomSizePenalty = new Double(((Exam)variable()).getRoomSizePenalty(getRooms()));
        return iRoomSizePenalty.doubleValue();
    }
    
    private Double iRoomSplitPenalty = null;
    public double getRoomSplitPenalty() {
        if (iRoomSplitPenalty==null)
            iRoomSplitPenalty = new Double(((Exam)variable()).getRoomSplitPenalty(getRooms()));
        return iRoomSplitPenalty.doubleValue();
    }
    
    private Double iNotOriginalRoomPenalty = null;
    public double getNotOriginalRoomPenalty() {
        if (iNotOriginalRoomPenalty==null) 
            iNotOriginalRoomPenalty = new Double(((Exam)variable()).getNotOriginalRoomPenalty(getRooms()));
        return iNotOriginalRoomPenalty.doubleValue();
    }

    private Double iPeriodPenalty = null;
    public double getPeriodPenalty() {
        if (iPeriodPenalty==null) 
            iPeriodPenalty = new Double(((Exam)variable()).getPeriodPenalty(getPeriod()));
        return iPeriodPenalty.doubleValue();
    }

    public double toDouble() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return 
            model.getDirectConflictWeight()*getNrDirectConflicts()+
            model.getBackToBackConflictWeight()*getNrBackToBackConflicts()+
            model.getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts()+
            model.getPeriodWeight()*getPeriodPenalty()+ 
            model.getRoomSizeWeight()*getRoomSizePenalty()+
            model.getRoomLocationWeight()*getRoomLocationPenalty()+
            model.getRoomSplitWeight()*getRoomSplitPenalty()+
            model.getNotOriginalRoomWeight()*getNotOriginalRoomPenalty();
    }
    
    public double[] toDoubleArray() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return new double[] {
            model.getDirectConflictWeight()*getNrDirectConflicts(),
            model.getBackToBackConflictWeight()*getNrBackToBackConflicts(),
            model.getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts(),
            model.getPeriodWeight()*getPeriodPenalty(),
            model.getRoomSizeWeight()*getRoomSizePenalty(),
            model.getRoomLocationWeight()*getRoomLocationPenalty(),
            model.getRoomSplitWeight()*getRoomSplitPenalty(),
            model.getNotOriginalRoomWeight()*getNotOriginalRoomPenalty()
        };
    }    
    
    public double getTimeCost() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return 
            model.getDirectConflictWeight()*getNrDirectConflicts()+
            model.getBackToBackConflictWeight()*getNrBackToBackConflicts()+
            model.getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts()+
            model.getPeriodWeight()*getPeriodPenalty(); 
    }
    
    public double getRoomCost() {
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return 
            model.getRoomSizeWeight()*getRoomSizePenalty()+
            model.getRoomSplitWeight()*getRoomSplitPenalty()+
            model.getRoomLocationWeight()*getRoomLocationPenalty()+
            model.getNotOriginalRoomWeight()*getNotOriginalRoomPenalty();
    }
    
    public String getName() {
        return getPeriod()+"/"+getRooms();
    }
    
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00");
        Exam exam = (Exam)variable();
        ExamModel model = (ExamModel)exam.getModel();
        return variable().getName()+" = "+getName()+" ("+
            df.format(toDouble())+"/"+
            "DC:"+df.format(model.getDirectConflictWeight()*getNrDirectConflicts())+","+
            "BTB:"+df.format(model.getBackToBackConflictWeight()*getNrBackToBackConflicts())+","+
            "M2D:"+df.format(model.getMoreThanTwoADayWeight()*getNrMoreThanTwoADayConflicts())+","+
            "PP:"+df.format(model.getPeriodWeight()*getPeriodPenalty())+","+
            "RSz:"+df.format(model.getRoomSizeWeight()*getRoomSizePenalty())+","+
            "RLc:"+df.format(model.getRoomLocationWeight()*getRoomLocationPenalty())+","+
            "RSp:"+df.format(model.getRoomSplitWeight()*getRoomSplitPenalty())+","+
            "ROg:"+df.format(model.getNotOriginalRoomWeight()*getNotOriginalRoomPenalty())+
            ")";
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamPlacement)) return false;
        ExamPlacement p = (ExamPlacement)o;
        return p.variable().equals(variable()) && p.getPeriod()==getPeriod() && p.getRooms().equals(getRooms());
    }
    
    public int hashCode() {
        return iHashCode;
    }
}
