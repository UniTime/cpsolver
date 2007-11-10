package net.sf.cpsolver.exam.model;

public class ExamPeriod implements Comparable {
    private int iIndex = -1;
    private String iTimeStr;
    private String iDayStr;
    private int iLength;
    private int iDay, iTime;
    private double iWeight;
    private ExamPeriod iPrev, iNext;
    
    public ExamPeriod(String day, String time, int length, double weight) {
        iDayStr = day;
        iTimeStr = time;
        iLength = length;
        iWeight = weight;
    }
    
    public String getDayStr() {
        return iDayStr;
    }
    public int getDay() {
        return iDay;
    }
    public String getTimeStr() {
        return iTimeStr;
    }
    public int getTime() {
        return iTime;
    }
    public int getLength() {
        return iLength;
    }
    public int getIndex() {
        return iIndex;
    }
    public double getWeight() {
        return iWeight;
    }
    public ExamPeriod prev() { return iPrev; }
    public ExamPeriod next() { return iNext; }
    public void setIndex(int index, int day, int time) {
        iIndex = index;
        iDay = day;
        iTime = time;
    }
    public void setPrev(ExamPeriod prev) { iPrev = prev;}
    public void setNext(ExamPeriod next) { iNext = next;}
    public String toString() {
        return getDayStr()+" "+getTimeStr();
    }
    public String toDebugString() {
        return getDayStr()+" "+getTimeStr()+
        " (idx:"+getIndex()+", day:"+getDay()+", time:"+getTime()+", weight:"+getWeight()+
        (prev()==null?"":", prev:"+prev().getDayStr()+" "+prev().getTimeStr()+")")+
        (next()==null?"":", next:"+next().getDayStr()+" "+next().getTimeStr()+")");
    }
    public int hashCode() {
        return iIndex;
    }
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamPeriod)) return false;
        return getIndex()==((ExamPeriod)o).getIndex();
    }
    public int compareTo(Object o) {
        return Double.compare(getIndex(), ((ExamPeriod)o).getIndex());
    }
}
