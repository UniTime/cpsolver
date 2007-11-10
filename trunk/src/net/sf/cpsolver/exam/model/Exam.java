package net.sf.cpsolver.exam.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.ToolBox;

public class Exam extends Variable {
    private static boolean sAlterMaxSize = false;
    private static Logger sLog = Logger.getLogger(Exam.class);
    protected static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",new java.text.DecimalFormatSymbols(Locale.US));
    private Vector iStudents = new Vector();
    private boolean iAllowDirectConflicts = true;
    private boolean iSectionExam = false;
    private boolean iAltSeating = false;
    private int iAveragePeriod = -1;
    private ExamPeriod iPreAssignedPeriod = null;
    private int iLength = 0;
    private int iMaxRooms = 0;
    private Vector iRoomGroups = new Vector();
    private ExamRoom iOriginalRoom = null;
    private Vector iPreassignedRooms = new Vector();
    private boolean iAvailable[] = null;
    private Vector iRooms = null;
    private Vector iPeriods = null;
    
    public Exam(long id, int length, boolean sectionExam, boolean altSeating, int maxRooms) {
        super();
        iId = id;
        iLength = length;
        iSectionExam = sectionExam;
        iAltSeating = altSeating;
        iMaxRooms = maxRooms;
    }
    
    public Vector values() {
        if (super.values()==null) init();
        return super.values();
    }
    
    public Vector getRooms() {
        if (iRooms==null) {
            if (getMaxRooms()==0) iRooms=new Vector(0);
            else if (hasPreAssignedRooms()) {
                iRooms = new Vector(getPreassignedRooms());
            } else {
                HashSet rooms = new HashSet();
                for (Enumeration e=getRoomGroups().elements();e.hasMoreElements();) {
                    ExamRoomGroup rg = (ExamRoomGroup)e.nextElement();
                    rooms.addAll(rg.getRooms());
                }
                if (getOriginalRoom()!=null && !rooms.contains(getOriginalRoom())) {
                    if ((hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size())
                        rooms.add(getOriginalRoom());
                }
                iRooms = new Vector(rooms);
                Collections.sort(iRooms, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        ExamRoom r1 = (ExamRoom)o1;
                        ExamRoom r2 = (ExamRoom)o2;
                        int s1 = (hasAltSeating()?r1.getAltSize():r1.getSize());
                        int s2 = (hasAltSeating()?r2.getAltSize():r2.getSize());
                        int cmp = -Double.compare(s1, s2);
                        if (cmp!=0) return cmp;
                        return r1.compareTo(r2);
                    }
                });
                if (sAlterMaxSize && iRooms.size()>50) {
                    ExamRoom med = (ExamRoom)iRooms.elementAt(Math.min(50, iRooms.size()/2));
                    int medSize = (hasAltSeating()?med.getAltSize():med.getSize());
                    setMaxRooms(Math.min(getMaxRooms(),1+(getStudents().size()/medSize)));
                }
            }
        }
        return iRooms;
    }
    
    public Vector getPeriods() {
        if (iPeriods==null) {
            if (hasPreAssignedPeriod()) {
                iPeriods=new Vector(1);
                iPeriods.add(getPreAssignedPeriod());
            } else {
                iPeriods = new Vector();
                for (Enumeration e=((ExamModel)getModel()).getPeriods().elements();e.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)e.nextElement();
                    if (isAvailable(period)) iPeriods.add(period);
                }
            }
        }
        return iPeriods;
    }
    
    public boolean init() {
        ExamModel model = (ExamModel)getModel();
        Vector values = new Vector();
        if (getMaxRooms()==0) {
            if (hasPreAssignedPeriod()) {
                values.addElement(new ExamPlacement(this, getPreAssignedPeriod(), new HashSet()));
            } else {
                for (Enumeration e=model.getPeriods().elements();e.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)e.nextElement();
                    if (isAvailable(period))
                        values.addElement(new ExamPlacement(this, period, new HashSet()));
                }
            }
        } else if (hasPreAssignedRooms()) {
            if (hasPreAssignedPeriod()) {
                values.addElement(new ExamPlacement(this, getPreAssignedPeriod(), new HashSet(getPreassignedRooms())));
            } else {
                for (Enumeration e=model.getPeriods().elements();e.hasMoreElements();) {
                    ExamPeriod period = (ExamPeriod)e.nextElement();
                    if (isAvailable(period))
                        values.addElement(new ExamPlacement(this, period, new HashSet(getPreassignedRooms())));
                }
            }
        } else {
            sLog.debug("Processing exam "+getName()+" ("+getStudents().size()+" students"+(hasAltSeating()?", alt":"")+") ...");
            if (getRooms().isEmpty()) {
                sLog.error("  Exam "+getName()+" has no rooms.");
                setValues(new Vector(0));
                return false;
            }
            TreeSet roomSets = new TreeSet();
            boolean norp = (getOriginalRoom()!=null && (hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size());
            genRoomSets(roomSets, 0, getRooms(), getMaxRooms(), new HashSet(), 0, 0, 0, norp);
            if (roomSets.isEmpty()) {
                sLog.error("  Exam "+getName()+" has no room placements.");
                setValues(new Vector(0));
                return false;
            }
            RoomSet first = (RoomSet)roomSets.first();
            RoomSet last = (RoomSet)roomSets.last();
            sLog.debug("  Exam "+getName()+" ("+getStudents().size()+" students, max rooms is "+getMaxRooms()+(hasAltSeating()?", alt":"")+") has "+roomSets.size()+" room placements ("+first.rooms().size()+"/"+sDoubleFormat.format(first.penalty())+"..."+last.rooms().size()+"/"+sDoubleFormat.format(last.penalty())+").");
            for (Iterator i=roomSets.iterator();i.hasNext();) {
                RoomSet roomSet = (RoomSet)i.next();
                if (hasPreAssignedPeriod()) {
                    if (isAvailable(getPreAssignedPeriod(), roomSet.rooms()))
                        values.addElement(new ExamPlacement(this, getPreAssignedPeriod(), roomSet.rooms()));
                } else {
                    for (Enumeration f=model.getPeriods().elements();f.hasMoreElements();) {
                        ExamPeriod period = (ExamPeriod)f.nextElement();
                        if (isAvailable(period, roomSet.rooms()))
                            values.addElement(new ExamPlacement(this, period, roomSet.rooms()));
                    }
                }
            }
        }
        if (values.isEmpty()) sLog.error("Exam "+getName()+" has no placement.");
        setValues(values);
        return !values.isEmpty();
    }
    
    private void genRoomSets(TreeSet roomSets, int roomIdx, Vector rooms, int maxRooms, Set roomsSoFar, int sizeSoFar, double distPen, double innerDistPen, boolean norp) {
        ExamModel model = (ExamModel)getModel();
        if (sizeSoFar>=getStudents().size()) {
            int nrRooms = roomsSoFar.size();
            int nrRooms2 = (nrRooms>1?nrRooms*(nrRooms-1)/2:1);
            double penalty = 
                model.getRoomSplitWeight() * getRoomSplitPenalty(roomsSoFar) +
                model.getRoomSizeWeight() * getRoomSizePenalty(sizeSoFar) +
                model.getRoomLocationWeight() * getPenaltyFactor() * (distPen/nrRooms + 2*innerDistPen/nrRooms2) +
                (norp?model.getNotOriginalRoomWeight() * getNotOriginalRoomPenalty(roomsSoFar):0);
            if (roomSets.size()>=rooms.size()) {
                RoomSet last = (RoomSet)roomSets.last();
                if (penalty<last.penalty()) {
                    roomSets.remove(last);
                    roomSets.add(new RoomSet(roomsSoFar,penalty));
                }
            } else
                roomSets.add(new RoomSet(roomsSoFar,penalty));
            return;
        }
        if (!roomSets.isEmpty()) {
            RoomSet roomSet = (RoomSet)roomSets.first();
            maxRooms = Math.min(maxRooms, (1+roomSet.rooms().size())-roomsSoFar.size());
        }
        if (maxRooms==0) return;
        int sizeBound = sizeSoFar;
        for (int i=0;i<maxRooms && roomIdx+i<rooms.size();i++)
            sizeBound += (hasAltSeating()?((ExamRoom)rooms.elementAt(roomIdx+i)).getAltSize():((ExamRoom)rooms.elementAt(roomIdx+i)).getSize());
        while (roomIdx<rooms.size()) {
            if (sizeBound<getStudents().size()) break;
            ExamRoom room = (ExamRoom)rooms.elementAt(roomIdx);
            roomsSoFar.add(room);
            genRoomSets(
                    roomSets, roomIdx+1, rooms, maxRooms-1, 
                    roomsSoFar, sizeSoFar+(hasAltSeating()?room.getAltSize():room.getSize()), 
                    distPen+getDistance(room), innerDistPen+getDistance(roomsSoFar, room),
                    norp
                    );
            roomsSoFar.remove(room);
            sizeBound -= (hasAltSeating()?room.getAltSize():room.getSize());
            if (roomIdx+maxRooms<rooms.size())
                sizeBound += (hasAltSeating()?((ExamRoom)rooms.elementAt(roomIdx+maxRooms)).getAltSize():((ExamRoom)rooms.elementAt(roomIdx+maxRooms)).getSize());
            roomIdx++;
        }
    }
    
    public class RoomSet implements Comparable {
        private Set iRooms;
        private double iPenalty;
        public RoomSet(Set rooms, double penalty) {
            iRooms = new HashSet(rooms);
            iPenalty = penalty; 
        }
        public Set rooms() { return iRooms; }
        public double penalty() { return iPenalty; }
        public int compareTo(Set rooms, double penalty) {
            int cmp = Double.compare(penalty(), penalty);
            if (cmp!=0) return cmp;
            return rooms().toString().compareTo(rooms.toString());
        }
        public int compareTo(Object o) {
            RoomSet r = (RoomSet)o;
            return compareTo(r.rooms(),r.penalty());
        }
    }
    
    public double getRoomPenalty(Set rooms) {
        ExamModel model = (ExamModel)getModel();
        return
            model.getRoomSizeWeight() * getRoomSizePenalty(rooms) +
            model.getRoomLocationWeight() * getRoomLocationPenalty(rooms) +
            model.getRoomSplitWeight() * getRoomSplitPenalty(rooms)+
            model.getNotOriginalRoomWeight() * getNotOriginalRoomPenalty(rooms);
    }
    
    public double getRoomSplitPenalty(Set rooms) {
        if (rooms.size()<=1) return 0;
        return getPenaltyFactor()*(1 << (rooms.size()-2));
    }
    
    public double getNotOriginalRoomPenalty(Set rooms) {
        if (getOriginalRoom()==null) return 0;
        if (getMaxRooms()==0) return 0;
        if ((hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size()) return 0;
        return (rooms.size()==1 && rooms.contains(getOriginalRoom())?0.0:getPenaltyFactor());
    }
    
    private double getPenaltyFactor() {
        return getStudents().size()/((ExamModel)getModel()).getEnrollmentFactor();
    }
    
    public double getPeriodPenalty(ExamPeriod period) {
        return period.getWeight()*getPenaltyFactor();
    }
    
    public double getRoomSizePenalty(int size) {
        int diff = size-getStudents().size();
        return getPenaltyFactor()*(diff<0?0:diff);
    }
    
    public double getRoomSizePenalty(Set rooms) {
        int size = 0;
        for (Iterator i=rooms.iterator();i.hasNext();) {
            ExamRoom room = (ExamRoom)i.next();
            size += (hasAltSeating()?room.getAltSize():room.getSize());
        }
        return getRoomSizePenalty(size);
    }
    
    public double getDistance(ExamRoom room) {
        ExamModel model = (ExamModel)getModel();
        int x1 = (getOriginalRoom()!=null?getOriginalRoom().getCoordX():-1);
        int y1 = (getOriginalRoom()!=null?getOriginalRoom().getCoordY():-1);
        if (x1<0) x1 = model.getCentralCoordX();
        if (y1<0) y1 = model.getCentralCoordY();
        int x2 = room.getCoordX();
        int y2 = room.getCoordY();
        if (x2<0) x2 = model.getCentralCoordX();
        if (y2<0) y2 = model.getCentralCoordY();
        return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
    }
    
    public double getDistance(Set rooms, ExamRoom room) {
        double dist = 0;
        for (Iterator i=rooms.iterator();i.hasNext();) {
            ExamRoom r = (ExamRoom)i.next();
            dist += getDistance(r,room);
        }
        return dist;
    }
    
    public double getDistance(ExamRoom r1, ExamRoom r2) {
        ExamModel model = (ExamModel)getModel();
        int x1 = r1.getCoordX();
        int y1 = r1.getCoordY();
        if (x1<0) x1 = model.getCentralCoordX();
        if (y1<0) y1 = model.getCentralCoordY();
        int x2 = r2.getCoordX();
        int y2 = r2.getCoordY();
        if (x2<0) x2 = model.getCentralCoordX();
        if (y2<0) y2 = model.getCentralCoordY();
        return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
    }

    public double getRoomLocationPenalty(Set rooms) {
        if (rooms.isEmpty()) return 0;
        int size = 0;
        int dist = 0;
        for (Iterator i=rooms.iterator();i.hasNext();) {
            ExamRoom room = (ExamRoom)i.next();
            dist += getDistance(room);
        }
        int innerDist = 0;
        if (rooms.size()>1) {
            dist /= rooms.size();
            for (Iterator i=rooms.iterator();i.hasNext();) {
                ExamRoom r1 = (ExamRoom)i.next();
                for (Iterator j=rooms.iterator();j.hasNext();) {
                    ExamRoom r2 = (ExamRoom)j.next();
                    if (r1.getId()<r2.getId()) innerDist += getDistance(r1,r2);
                }
            }
            innerDist /= rooms.size()*(rooms.size()-1)/2;
        }
        return getPenaltyFactor()*(dist+2*innerDist);
    }
    

    public void setAvailable(int period, boolean avail) {
        if (iAvailable==null) {
            iAvailable = new boolean[((ExamModel)getModel()).getNrPeriods()];
            for (int i=0;i<iAvailable.length;i++)
                iAvailable[i]=true;
        }
        iAvailable[period]=avail;
    }
    
    public boolean isAvailable(ExamPeriod period) {
        return (iAvailable==null?true:iAvailable[period.getIndex()]); 
    }
    
    public boolean isAvailable(ExamPeriod period, Set rooms) {
        if (!isAvailable(period)) return false;
        for (Iterator i=rooms.iterator();i.hasNext();) {
            ExamRoom room = (ExamRoom)i.next();
            if (!room.isAvailable(period)) return false;
        }
        return true;
    }

    public boolean isSectionExam() {
        return iSectionExam;
    }
    
    public boolean hasAltSeating() {
        return iAltSeating;
    }
    
    public int getLength() {
        return iLength;
    }
    
    public void setPreAssignedPeriod(ExamPeriod period) {
        iPreAssignedPeriod = period;
    }
    public ExamPeriod getPreAssignedPeriod() {
        return iPreAssignedPeriod;
    }
    public boolean hasPreAssignedPeriod() {
        return iPreAssignedPeriod!=null;
    }
    
    public void setAveragePeriod(int period) {
        iAveragePeriod = period;
    }
    public int getAveragePeriod() {
        return iAveragePeriod;
    }
    public boolean hasAveragePeriod() {
        return iAveragePeriod>=0;
    }
    
    public boolean isAllowDirectConflicts() {
        return iAllowDirectConflicts;
    }
    
    public void setAllowDirectConflicts(boolean allowDirectConflicts) {
        iAllowDirectConflicts = allowDirectConflicts;
    }
    
    public void addContstraint(Constraint constraint) {
        if (constraint instanceof ExamStudent) iStudents.add((ExamStudent)constraint);
        super.addContstraint(constraint);
    }
    
    public void removeContstraint(Constraint constraint) {
        if (constraint instanceof ExamStudent) iStudents.remove((ExamStudent)constraint);
        super.removeContstraint(constraint);
    }
    
    public Vector getStudents() { return iStudents; }
    
    public Vector getRoomGroups() { return iRoomGroups; }
    public void addRoomGroup(ExamRoomGroup rg) { 
        if (!iRoomGroups.contains(rg)) iRoomGroups.add(rg);
    }
    
    public ExamRoom getOriginalRoom() {
        return iOriginalRoom;
    }
    public void setOriginalRoom(ExamRoom room) {
        iOriginalRoom = room;
    }
    public Vector getPreassignedRooms() {
        return iPreassignedRooms;
    }
    public boolean hasPreAssignedRooms() {
        return !iPreassignedRooms.isEmpty();
    }
    
    public int getMaxRooms() {
        return iMaxRooms;
    }
    public void setMaxRooms(int maxRooms) {
        iMaxRooms = maxRooms;
    }
    
    public Set findRooms(ExamPeriod period) {
        return findBestAvailableRooms(period);
    }
    
    public Set findBestAvailableRooms(ExamPeriod period) {
        if (!isAvailable(period)) return null;
        if (hasPreAssignedPeriod() && !period.equals(getPreAssignedPeriod())) return null;
        if (getMaxRooms()==0) return new HashSet();
        if (hasPreAssignedRooms()) {
            for (Enumeration e=getPreassignedRooms().elements();e.hasMoreElements();) {
                ExamRoom room = (ExamRoom)e.nextElement();
                if (room.getPlacement(period)!=null) return null;
            }
            return new HashSet(getPreassignedRooms());
        }
        if (getOriginalRoom()!=null && getOriginalRoom().isAvailable(period) &&
            getOriginalRoom().getPlacement(period)==null &&
           (hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size()) {
                HashSet rooms = new HashSet(); rooms.add(getOriginalRoom());
                return rooms;
        }
        loop: for (int nrRooms=1;nrRooms<=getMaxRooms();nrRooms++) {
            HashSet rooms = new HashSet(); int size = 0;
            while (rooms.size()<nrRooms && size<getStudents().size()) {
                int minSize = (getStudents().size()-size)/nrRooms;
                ExamRoom best = null; int bestSize = 0;
                for (Enumeration e=getRooms().elements();e.hasMoreElements();) {
                    ExamRoom room = (ExamRoom)e.nextElement();
                    if (!room.isAvailable(period)) continue;
                    if (room.getPlacement(period)!=null) continue;
                    if (rooms.contains(room)) continue;
                    int s = (hasAltSeating()?room.getAltSize():room.getSize());
                    if (s<minSize) break;
                    if (best==null || bestSize>s) {
                        best = room;
                        bestSize = s;
                    }
                }
                if (best==null) continue loop;
                rooms.add(best); size+=bestSize;
            }
            if (size>=getStudents().size()) return rooms;
        }
        return null;
    }
    
    public Set findRoomsRandom(ExamPeriod period) {
        if (!isAvailable(period)) return null;
        if (hasPreAssignedPeriod() && !period.equals(getPreAssignedPeriod())) return null;
        if (getMaxRooms()==0) return new HashSet();
        if (hasPreAssignedRooms()) {
            for (Enumeration e=getPreassignedRooms().elements();e.hasMoreElements();) {
                ExamRoom room = (ExamRoom)e.nextElement();
                if (room.getPlacement(period)!=null) return null;
            }
            return new HashSet(getPreassignedRooms());
        }
        int exSize = getStudents().size();
        HashSet rooms = new HashSet(); int size = 0; int minSize = Integer.MAX_VALUE;
        if (getOriginalRoom()!=null && getOriginalRoom().isAvailable(period) &&
            getOriginalRoom().getPlacement(period)==null &&
            (hasAltSeating()?getOriginalRoom().getAltSize():getOriginalRoom().getSize())>=getStudents().size() &&
            ToolBox.random()<0.5) {
            rooms.add(getOriginalRoom());
            return rooms;
        }
        loop: while (true) {
            int rx = ToolBox.random(getRooms().size()); 
            for (int r=0;r<getRooms().size();r++) {
                ExamRoom room = (ExamRoom)getRooms().elementAt((r+rx)%getRooms().size());
                if (!room.isAvailable(period)) continue;
                if (room.getPlacement(period)!=null) continue;
                if (rooms.contains(room)) continue;
                int s = (hasAltSeating()?room.getAltSize():room.getSize());
                if (size+s>=exSize && size+s-exSize<minSize) {
                    rooms.add(room); return rooms;
                }
                if (rooms.size()+1<getMaxRooms()) {
                    minSize = Math.min(minSize, s);
                    rooms.add(room); continue loop;
                }
            }
            return null;
        }
    }
    
    private HashSet iCorrelatedExams = null;
    
    public int nrStudentCorrelatedExams() {
        if (iCorrelatedExams==null) { 
            iCorrelatedExams = new HashSet();
            int weightedNrCorrelatedEvents = 0; 
            for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                iCorrelatedExams.addAll(student.variables());
            }
            iCorrelatedExams.remove(this);
        }
        return iCorrelatedExams.size();
    }
    
    public int compareTo(Object o) {
        Exam e = (Exam)o;
        int cmp = -Double.compare(nrStudentCorrelatedExams(),e.nrStudentCorrelatedExams());
        if (cmp!=0) return cmp;
        cmp = -Double.compare(((double)getStudents().size())/getPeriods().size(),((double)e.getStudents().size())/e.getPeriods().size());
        if (cmp!=0) return cmp;
        return super.compareTo(o);
    }
    
    public String toString() {
        return getName()+" (periods:"+getPeriods().size()+", rooms:"+getRooms().size()+", student:"+getStudents().size()+", corrEx:"+nrStudentCorrelatedExams()+" ,maxRooms:"+getMaxRooms()+(hasAltSeating()?", alt":"")+")";
    }
}
