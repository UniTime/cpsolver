package net.sf.cpsolver.coursett;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import net.sf.cpsolver.coursett.constraint.ClassLimitConstraint;
import net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import net.sf.cpsolver.coursett.constraint.DiscouragedRoomConstraint;
import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.constraint.MinimizeNumberOfUsedGroupsOfTime;
import net.sf.cpsolver.coursett.constraint.MinimizeNumberOfUsedRoomsConstraint;
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.model.Configuration;
import net.sf.cpsolver.coursett.model.InitialSectioning;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.RoomSharingModel;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.FastVector;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;


/**
 * This class loads the input model from XML file.
 * <br><br>
 * Parameters:
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>General.Input</td><td>{@link String}</td><td>Input XML file</td></tr>
 * <tr><td>General.DeptBalancing</td><td>{@link Boolean}</td><td>Use {@link DepartmentSpreadConstraint}</td></tr>
 * <tr><td>General.InteractiveMode</td><td>{@link Boolean}</td><td>Interactive mode (see {@link Lecture#purgeInvalidValues(boolean)})</td></tr>
 * <tr><td>General.ForcedPerturbances</td><td>{@link Integer}</td><td>For testing of MPP: number of input perturbations, i.e., classes with prohibited intial assignment</td></tr>
 * <tr><td>General.UseDistanceConstraints</td><td>{@link Boolean}</td><td>Consider distances between buildings</td></tr>
 * </table>
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

public class TimetableXMLLoader extends TimetableLoader {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(TimetableXMLLoader.class);
    private static SimpleDateFormat sDF = new SimpleDateFormat("MM/dd");

    private boolean iDeptBalancing = true;
    private int iForcedPerturbances = 0;
    
    private boolean iUseDistanceConstraints;
    private boolean iInteractiveMode = false;
    private File iInputFile;
    
    private Progress iProgress = null;
    
    public TimetableXMLLoader(TimetableModel model) {
        super(model);
        iProgress = Progress.getInstance(getModel());
        iInputFile                 = new File(getModel().getProperties().getProperty("General.Input","."+File.separator+"solution.xml"));
        iForcedPerturbances        = getModel().getProperties().getPropertyInt("General.ForcedPerturbances",0);
        iDeptBalancing = getModel().getProperties().getPropertyBoolean("General.DeptBalancing",true);
        iUseDistanceConstraints = getModel().getProperties().getPropertyBoolean("General.UseDistanceConstraints", true);
        iInteractiveMode = getModel().getProperties().getPropertyBoolean("General.InteractiveMode", iInteractiveMode);
    }
    
    private Solver iSolver = null;
    public void setSolver(Solver solver) {
    	iSolver = solver;
    }
    public Solver getSolver() {
    	return iSolver;
    }

    public void setInputFile(File inputFile) {
    	iInputFile=inputFile;
    }
     
    public void load() throws Exception {
    	load(null);
    }
    
    /*
	public static boolean match(TimePatternModel model, TimeLocation time) {
		if (model.getNrMeetings()!=time.getNrMeetings()) return false;
		if (model.getSlotsPerMtg()!=6*time.getLength()) return false;
		int matchTime = -1, matchDays = -1;
		for (int i=0;i<model.getNrDays();i++) {
			if (time.getDayCode()==model.getDayCode(i))
				matchDays = i;
		}
		for (int i=0;i<model.getNrTimes();i++) {
			if (90+time.getStartSlot()*6==model.getStartSlot(i))
				matchTime = i;
		}
		return matchTime>=0 && matchDays>=0;
	}

	Vector iAllTimePatterns = null;
	Hashtable iClasses = new Hashtable();
	private TimeLocation transformTimePattern(Long classId, TimeLocation oldLocation) {
		if (iAllTimePatterns==null) {
			iAllTimePatterns = TimePattern.findAll(getModel().getProperties().getProperty("Data.Initiative"), getModel().getProperties().getProperty("Data.Term").substring(4), null);
			Collections.sort(iAllTimePatterns);
		}
		TimeLocation newLocation = new TimeLocation(
				oldLocation.getDayCode(),
				90+oldLocation.getStartSlot()*6,
				oldLocation.getLength()*6,
				oldLocation.getPreference(),
				oldLocation.getNormalizedPreference(),
				oldLocation.getDatePatternId(),
				oldLocation.getDatePatternName(),
				oldLocation.getWeekCode(),
				oldLocation.getBreakTime());

		if (classId!=null) {
			Class_ clazz = (Class_)iClasses.get(classId);
			if (clazz==null) {
				clazz = (new Class_DAO()).get(classId);
				if (clazz!=null)
					iClasses.put(classId, clazz);
			}
			if (clazz!=null) {
				for (Iterator i=clazz.effectiveTimePatterns().iterator();i.hasNext();) {
					TimePattern tp = (TimePattern)i.next();
					if (LoadTimePreferences.match(tp.getTimePatternModel(),oldLocation)) {
						newLocation.setTimePatternId(tp.getUniqueId());
						break;
					}
				}
			}
		}
		if (newLocation.getTimePatternId()==null) {
			for (Enumeration e=iAllTimePatterns.elements();e.hasMoreElements();) {
				TimePattern tp = (TimePattern)e.nextElement();
				if (LoadTimePreferences.match(tp.getTimePatternModel(),oldLocation)) {
					newLocation.setTimePatternId(tp.getUniqueId());
					break;
				}
			}
		}
		return newLocation;
    }
	*/
    
	private static BitSet createBitSet(String bitString) {
		BitSet ret = new BitSet(bitString.length());
		for (int i=0;i<bitString.length();i++)
			if (bitString.charAt(i)=='1') ret.set(i);
		return ret;
	}

    public void load(Solution currentSolution) throws Exception {
        sLogger.debug("Reading XML data from "+iInputFile);
        iProgress.setPhase("Reading "+iInputFile.getName()+" ...");
        
        Document document = (new SAXReader()).read(iInputFile);
        Element root = document.getRootElement();
        sLogger.debug("Root element: "+root.getName());
        if (!"llrt".equals(root.getName()) && !"timetable".equals(root.getName())) {
            sLogger.error("Given XML file is not large lecture room timetabling problem.");
            return;
        }
        
        iProgress.load(root, true);
        iProgress.message(Progress.MSGLEVEL_STAGE, "Restoring from backup ...");
        
        if (root.element("input")!=null) root = root.element("input");
        
        if (root.attributeValue("term")!=null)
        	getModel().getProperties().setProperty("Data.Term", root.attributeValue("term"));
        if (root.attributeValue("year")!=null)
        	getModel().setYear(Integer.parseInt(root.attributeValue("year")));
        else if (root.attributeValue("term")!=null)
        	getModel().setYear(Integer.parseInt(root.attributeValue("term").substring(0,4)));
        if (root.attributeValue("initiative")!=null)
        	getModel().getProperties().setProperty("Data.Initiative", root.attributeValue("initiative"));
        if (root.attributeValue("semester")!=null && root.attributeValue("year")!=null)
        	getModel().getProperties().setProperty("Data.Term", root.attributeValue("semester")+root.attributeValue("year"));
        if (root.attributeValue("session")!=null)
        	getModel().getProperties().setProperty("General.SessionId", root.attributeValue("session"));
        if (root.attributeValue("solverGroup")!=null)
        	getModel().getProperties().setProperty("General.SolverGroupId", root.attributeValue("solverGroup"));
        String version = root.attributeValue("version");
        //boolean timePatternTransform = "1.0".equals(version) || "2.0".equals(version);
        
        iProgress.setPhase("Creating rooms ...",root.element("rooms").elements("room").size());
        Hashtable roomElements = new Hashtable();
        Hashtable roomConstraints = new Hashtable();
        Hashtable sameLectures = new Hashtable();
        for (Iterator i=root.element("rooms").elementIterator("room");i.hasNext();) {
            Element roomEl = (Element)i.next();
            iProgress.incProgress();
            roomElements.put(roomEl.attributeValue("id"), roomEl);
            if ("false".equals(roomEl.attributeValue("constraint"))) continue;
            RoomSharingModel sharingModel = null;
            Element sharingEl = roomEl.element("sharing");
            if (sharingEl!=null) {
            	String pattern = sharingEl.element("pattern").getText();
            	List depts = sharingEl.elements("department");
            	Long departmentIds[] = new Long[depts.size()];
            	for (int j=0;j<departmentIds.length;j++)
            		departmentIds[j] = Long.valueOf(((Element)depts.get(j)).attributeValue("id"));
            	sharingModel = new RoomSharingModel(departmentIds, pattern);
            }
            boolean ignoreTooFar = false;
            if ("true".equals(roomEl.attributeValue("ignoreTooFar")))
            	ignoreTooFar = true;
            boolean fake=false;
            if ("true".equals(roomEl.attributeValue("fake")))
            	fake = true;
            int posX = -1, posY = -1;
            if (roomEl.attributeValue("location")!=null) {
                String loc = roomEl.attributeValue("location");
                posX = Integer.parseInt(loc.substring(0,loc.indexOf(',')));
                posY = Integer.parseInt(loc.substring(loc.indexOf(',')+1));
            }
            boolean discouraged = "true".equals(roomEl.attributeValue("discouraged"));
            RoomConstraint constraint = (discouraged ?
            			new DiscouragedRoomConstraint(getModel().getProperties(),
            					Long.valueOf(roomEl.attributeValue("id")), 
            					(roomEl.attributeValue("name")!=null?roomEl.attributeValue("name"):"r"+roomEl.attributeValue("id")), 
            					(roomEl.attributeValue("building")==null?null:Long.valueOf(roomEl.attributeValue("building"))),
            					Integer.parseInt(roomEl.attributeValue("capacity")), sharingModel, posX, posY, ignoreTooFar, !fake)
                    :
                    	new RoomConstraint(
                    			Long.valueOf(roomEl.attributeValue("id")), 
                    			(roomEl.attributeValue("name")!=null?roomEl.attributeValue("name"):"r"+roomEl.attributeValue("id")), 
                    			(roomEl.attributeValue("building")==null?null:Long.valueOf(roomEl.attributeValue("building"))),
                    			Integer.parseInt(roomEl.attributeValue("capacity")), sharingModel, posX, posY, ignoreTooFar, !fake)
                    );
            if (roomEl.attributeValue("type")!=null)
                constraint.setType(Integer.valueOf(roomEl.attributeValue("type")));
            getModel().addConstraint(constraint);
            roomConstraints.put(roomEl.attributeValue("id"), constraint);
        }
        
        Hashtable instructorConstraints = new Hashtable();
        if (root.element("instructors")!=null) {
            for (Iterator i=root.element("instructors").elementIterator("instructor");i.hasNext();) {
                Element instructorEl = (Element)i.next();
                InstructorConstraint instructorConstraint = new InstructorConstraint(Long.valueOf(instructorEl.attributeValue("id")), instructorEl.attributeValue("puid"), (instructorEl.attributeValue("name")!=null?instructorEl.attributeValue("name"):"i"+instructorEl.attributeValue("id")), "true".equals(instructorEl.attributeValue("ignDist")));
                if (instructorEl.attributeValue("type")!=null)
                    instructorConstraint.setType(Integer.valueOf(instructorEl.attributeValue("type")));
                instructorConstraints.put(instructorEl.attributeValue("id"), instructorConstraint);
                
                getModel().addConstraint(instructorConstraint);
            }
        }
        Hashtable depts = new Hashtable();
        if (root.element("departments")!=null) {
        	for (Iterator i=root.element("departments").elementIterator("department");i.hasNext();) {
        		Element deptEl = (Element)i.next();
        		depts.put(Long.valueOf(deptEl.attributeValue("id")),(deptEl.attributeValue("name")!=null?deptEl.attributeValue("name"):"d"+deptEl.attributeValue("id")));
        	}
        }
        
        Hashtable configs = new Hashtable();
        Hashtable alternativeConfigurations = new Hashtable();
        if (root.element("configurations")!=null) {
        	for (Iterator i=root.element("configurations").elementIterator("config");i.hasNext();) {
        		Element configEl = (Element)i.next();
        		Long configId = Long.valueOf(configEl.attributeValue("id"));
        		int limit = Integer.parseInt(configEl.attributeValue("limit"));
        		Long offeringId = Long.valueOf(configEl.attributeValue("offering"));
        		Configuration config = new Configuration(offeringId, configId, limit);
        		configs.put(configId, config);
            	Vector altConfigs = (Vector)alternativeConfigurations.get(offeringId);
            	if (altConfigs==null) {
            		altConfigs = new FastVector();
            		alternativeConfigurations.put(offeringId, altConfigs);
            	}
                altConfigs.add(config);
            	config.setAltConfigurations(altConfigs);
        	}
        }
        
        iProgress.setPhase("Creating variables ...",root.element("classes").elements("class").size());
        
        int nrClasses = root.element("classes").elements("class").size();
        
        Hashtable classElements = new Hashtable();
        Hashtable lectures = new Hashtable();
        Hashtable assignedPlacements = new Hashtable();
        Hashtable parents = new Hashtable();
        int ord = 0;
        for (Iterator i1=root.element("classes").elementIterator("class");i1.hasNext();) {
            Element classEl = (Element)i1.next();
            
            Configuration config = null;
            if (classEl.attributeValue("config")!=null) {
            	config = (Configuration)configs.get(Integer.valueOf(classEl.attributeValue("config")));
            }
            if (config==null && classEl.attributeValue("offering")!=null) {
            	Long offeringId = Long.valueOf(classEl.attributeValue("offering"));
            	Long configId = Long.valueOf(classEl.attributeValue("config"));
            	Vector altConfigs = (Vector)alternativeConfigurations.get(offeringId);
            	if (altConfigs==null) {
            		altConfigs = new FastVector();
            		alternativeConfigurations.put(offeringId, altConfigs);
            	}
            	config = new Configuration(offeringId,configId,0);
                altConfigs.add(config);
            	config.setAltConfigurations(altConfigs);
            }
            
            Long datePatternId = null;
            String datePatternName = null;
            BitSet weekCode = null;
            if (classEl.attributeValue("dates")==null) {
            	int startDay = Integer.parseInt(classEl.attributeValue("startDay","0"));
            	int endDay = Integer.parseInt(classEl.attributeValue("endDay","1"));
    	        weekCode = new BitSet(366);
    	        for (int d=startDay;d<=endDay;d++)
    	        	weekCode.set(d);
            	datePatternName = sDF.format(getDate(getModel().getYear(),startDay))+"-"+sDF.format(getDate(getModel().getYear(),endDay));
            } else {
            	datePatternId = (classEl.attributeValue("datePattern")==null?null:Long.valueOf(classEl.attributeValue("datePattern")));
            	datePatternName = classEl.attributeValue("datePatternName");
            	weekCode = createBitSet(classEl.attributeValue("dates"));
            }
            classElements.put(classEl.attributeValue("id"),classEl);
            Vector ics = new Vector();
            for (Iterator i2=classEl.elementIterator("instructor");i2.hasNext();) {
            	Element instructorEl = (Element)i2.next();
            	InstructorConstraint instructorConstraint = (InstructorConstraint)instructorConstraints.get(instructorEl.attributeValue("id"));
           		if (instructorConstraint==null) {
           			instructorConstraint = new InstructorConstraint(Long.valueOf(instructorEl.attributeValue("id")), instructorEl.attributeValue("puid"), (instructorEl.attributeValue("name")!=null?instructorEl.attributeValue("name"):"i"+instructorEl.attributeValue("id")), "true".equals(instructorEl.attributeValue("ignDist")));
           			instructorConstraints.put(instructorEl.attributeValue("id"), instructorConstraint);
           			getModel().addConstraint(instructorConstraint);
                }
           		ics.add(instructorConstraint);
            }
            Vector roomLocations = new FastVector();
            Vector roomConstraintsThisClass = new FastVector();
            Vector initialRoomLocations = new Vector();
            Vector assignedRoomLocations = new Vector();
            Vector bestRoomLocations = new Vector();
            for (Iterator i2=classEl.elementIterator("room");i2.hasNext();) {
                Element roomLocationEl = (Element)i2.next();
                Element roomEl = (Element)roomElements.get(roomLocationEl.attributeValue("id"));
                RoomConstraint roomConstraint = (RoomConstraint)roomConstraints.get(roomLocationEl.attributeValue("id"));
                
                Long roomId = null;
        		String roomName = null; 
        		Long bldgId = null;
        		
                if (roomConstraint!=null) { 
                	roomConstraintsThisClass.add(roomConstraint);
                    roomId = roomConstraint.getResourceId();
            		roomName = roomConstraint.getRoomName(); 
            		bldgId = roomConstraint.getBuildingId();
                } else {
                    roomId = Long.valueOf(roomEl.attributeValue("id"));
            		roomName = (roomEl.attributeValue("name")!=null?roomEl.attributeValue("name"):"r"+roomEl.attributeValue("id")); 
            		bldgId = (roomEl.attributeValue("building")==null?null:Long.valueOf(roomEl.attributeValue("building")));
                }
                
                boolean ignoreTooFar = false;
                if ("true".equals(roomEl.attributeValue("ignoreTooFar")))
                	ignoreTooFar = true;
                int posX = -1, posY = -1;
                if (roomEl.attributeValue("location")!=null) {
                    String loc = roomEl.attributeValue("location");
                    posX = Integer.parseInt(loc.substring(0,loc.indexOf(',')));
                    posY = Integer.parseInt(loc.substring(loc.indexOf(',')+1));
                }
                RoomLocation rl = new RoomLocation(roomId,roomName,bldgId,Integer.parseInt(roomLocationEl.attributeValue("pref")),Integer.parseInt(roomEl.attributeValue("capacity")),posX,posY,ignoreTooFar, roomConstraint);
                if ("true".equals(roomLocationEl.attributeValue("initial")))
                	initialRoomLocations.addElement(rl);
                if ("true".equals(roomLocationEl.attributeValue("solution")))
                	assignedRoomLocations.addElement(rl);
                if ("true".equals(roomLocationEl.attributeValue("best")))
                	bestRoomLocations.addElement(rl);
                roomLocations.add(rl);
            }
            Vector timeLocations = new FastVector();
            TimeLocation initialTimeLocation = null;
            TimeLocation assignedTimeLocation = null;
            TimeLocation bestTimeLocation = null;
            for (Iterator i2=classEl.elementIterator("time");i2.hasNext();) {
                Element timeLocationEl = (Element)i2.next();
                TimeLocation tl = new TimeLocation(
                    Integer.parseInt(timeLocationEl.attributeValue("days"),2),
                    Integer.parseInt(timeLocationEl.attributeValue("start")),
                    Integer.parseInt(timeLocationEl.attributeValue("length")),
                    (int)Double.parseDouble(timeLocationEl.attributeValue("pref")), 
                    Double.parseDouble(timeLocationEl.attributeValue("npref",timeLocationEl.attributeValue("pref"))),
                    datePatternId, datePatternName, weekCode,
                    Integer.parseInt(timeLocationEl.attributeValue("breakTime")==null?"0":timeLocationEl.attributeValue("breakTime"))
                    );
                if (timeLocationEl.attributeValue("pattern")!=null)
                	tl.setTimePatternId(Long.valueOf(timeLocationEl.attributeValue("pattern")));
                /*
                if (timePatternTransform)
                	tl = transformTimePattern(Long.valueOf(classEl.attributeValue("id")),tl);
                */
                if ("true".equals(timeLocationEl.attributeValue("solution")))
                	assignedTimeLocation = tl;
                if ("true".equals(timeLocationEl.attributeValue("initial")))
                    initialTimeLocation = tl;
                if ("true".equals(timeLocationEl.attributeValue("best")))
                    bestTimeLocation = tl;
                timeLocations.add(tl);
            }
            if (timeLocations.isEmpty()) {
                sLogger.error("  ERROR: No time.");
                continue;
            }
            
            int minClassLimit = 0;
            int maxClassLimit = 0;
            double room2limitRatio = 1.0;
            if (!"true".equals(classEl.attributeValue("committed"))) {
            	if (classEl.attributeValue("expectedCapacity")!=null) {
            		minClassLimit = maxClassLimit = Integer.parseInt(classEl.attributeValue("expectedCapacity"));
            		int roomCapacity = Integer.parseInt(classEl.attributeValue("roomCapacity",classEl.attributeValue("expectedCapacity")));
            		if (minClassLimit==0) minClassLimit = maxClassLimit = roomCapacity;
            		room2limitRatio = (minClassLimit==0?1.0:((double)roomCapacity)/minClassLimit);
            	} else {
            		if (classEl.attribute("classLimit")!=null) {
            			minClassLimit = maxClassLimit = Integer.parseInt(classEl.attributeValue("classLimit"));
            		} else {
            			minClassLimit = Integer.parseInt(classEl.attributeValue("minClassLimit"));
            			maxClassLimit = Integer.parseInt(classEl.attributeValue("maxClassLimit"));
            		}
            		room2limitRatio = Double.parseDouble(classEl.attributeValue("roomToLimitRatio","1.0"));
            	}
            }

            Lecture lecture = new Lecture(
            		Long.valueOf(classEl.attributeValue("id")),
            		(classEl.attributeValue("solverGroup")!=null?Long.valueOf(classEl.attributeValue("solverGroup")):null),
            		Long.valueOf(classEl.attributeValue("subpart", classEl.attributeValue("course","-1"))),
            		(classEl.attributeValue("name")!=null?classEl.attributeValue("name"):"c"+classEl.attributeValue("id")),
            		timeLocations,
            		roomLocations, 
            		Integer.parseInt(classEl.attributeValue("nrRooms","1")), 
            		null, 
            		minClassLimit,maxClassLimit,room2limitRatio);
            lecture.setNote(classEl.attributeValue("note"));
            
            if ("true".equals(classEl.attributeValue("committed")))
            	lecture.setCommitted(true);

            if (!lecture.isCommitted() && classEl.attributeValue("ord")!=null)
            	lecture.setOrd(Integer.parseInt(classEl.attributeValue("ord")));
            else
            	lecture.setOrd(ord++);
            
            if (config!=null)
            	lecture.setConfiguration(config);
            
            if (initialTimeLocation!=null && initialRoomLocations!=null && initialRoomLocations.size()==lecture.getNrRooms()) {
                lecture.setInitialAssignment(new Placement(lecture,initialTimeLocation, initialRoomLocations));
            }
            if (assignedTimeLocation!=null && assignedRoomLocations!=null && assignedRoomLocations.size()==lecture.getNrRooms()) {
            	assignedPlacements.put(lecture, new Placement(lecture,assignedTimeLocation, assignedRoomLocations));
            } else if (lecture.getInitialAssignment()!=null) {
            	assignedPlacements.put(lecture, lecture.getInitialAssignment());
            }
            if (bestTimeLocation!=null && bestRoomLocations!=null && bestRoomLocations.size()==lecture.getNrRooms()) {
            	lecture.setBestAssignment(new Placement(lecture,bestTimeLocation, bestRoomLocations));
            } else if (assignedTimeLocation!=null && assignedRoomLocations!=null && assignedRoomLocations.size()==lecture.getNrRooms()) {
            	lecture.setBestAssignment((Placement)assignedPlacements.get(lecture));
            }
            
            lectures.put(classEl.attributeValue("id"), lecture);
            if (classEl.attributeValue("department")!=null)
            	lecture.setDepartment(Long.valueOf(classEl.attributeValue("department")));
            if (classEl.attribute("scheduler")!=null)
            	lecture.setScheduler(Long.valueOf(classEl.attributeValue("scheduler")));
            if (!lecture.isCommitted() && classEl.attributeValue("subpart",classEl.attributeValue("course"))!=null) {
            	Long subpartId = Long.valueOf(classEl.attributeValue("subpart",classEl.attributeValue("course")));
            	Vector sames = (Vector)sameLectures.get(subpartId);
            	if (sames==null) {
            		sames = new FastVector();
            		sameLectures.put(subpartId, sames);
            	}
            	sames.addElement(lecture);
            }
            String parent = classEl.attributeValue("parent");
            if (parent!=null)
            	parents.put(lecture, parent);

            getModel().addVariable(lecture);
            
            if (lecture.isCommitted()) {
            	Placement placement = (Placement)assignedPlacements.get(lecture);
                if (classEl.attribute("assignment")!=null)
                	placement.setAssignmentId(Long.valueOf(classEl.attributeValue("assignment")));
            	for (Enumeration e2=ics.elements();e2.hasMoreElements();)
            		((InstructorConstraint)e2.nextElement()).setNotAvailable(placement);
                for (Enumeration e2=roomConstraintsThisClass.elements(); e2.hasMoreElements();)
                    ((RoomConstraint)e2.nextElement()).setNotAvailable(placement);
            } else {
            	for (Enumeration e2=ics.elements();e2.hasMoreElements();)
            		((InstructorConstraint)e2.nextElement()).addVariable(lecture);
                for (Enumeration e2=roomConstraintsThisClass.elements(); e2.hasMoreElements();)
                    ((Constraint)e2.nextElement()).addVariable(lecture);
            }

            iProgress.incProgress();
        }
        
        for (Iterator i=parents.entrySet().iterator();i.hasNext();) {
        	Map.Entry entry = (Map.Entry)i.next();
        	Lecture lecture = (Lecture)entry.getKey();
        	Lecture parent = (Lecture)lectures.get(entry.getValue());
        	lecture.setParent(parent);
        }
        
        iProgress.setPhase("Creating constraints ...",root.element("groupConstraints").elements("constraint").size());
        Hashtable grConstraintElements = new Hashtable();
        Hashtable groupConstraints = new Hashtable();
        for (Iterator i1=root.element("groupConstraints").elementIterator("constraint");i1.hasNext();) {
            Element grConstraintEl = (Element)i1.next();
            Constraint c = null;
            if ("SPREAD".equals(grConstraintEl.attributeValue("type"))) {
            	c = new SpreadConstraint(getModel().getProperties(),grConstraintEl.attributeValue("name","spread"));
            } else if ("MIN_ROOM_USE".equals(grConstraintEl.attributeValue("type"))){
            	c = new MinimizeNumberOfUsedRoomsConstraint(getModel().getProperties());
            } else if ("CLASS_LIMIT".equals(grConstraintEl.attributeValue("type"))){
            	if (grConstraintEl.element("parentClass")==null) {
            		c = new ClassLimitConstraint(Integer.parseInt(grConstraintEl.attributeValue("courseLimit")),grConstraintEl.attributeValue("name","class-limit"));
            	} else {
            		String classId = grConstraintEl.element("parentClass").attributeValue("id");
            		c = new ClassLimitConstraint((Lecture)lectures.get(classId),grConstraintEl.attributeValue("name","class-limit"));
            	}
        		if (grConstraintEl.attributeValue("delta")!=null)
        			((ClassLimitConstraint)c).setClassLimitDelta(Integer.parseInt(grConstraintEl.attributeValue("delta")));
        	} else if ("MIN_GRUSE(10x1h)".equals(grConstraintEl.attributeValue("type"))) {
        		c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(),"10x1h",MinimizeNumberOfUsedGroupsOfTime.sGroups10of1h);
        	} else if ("MIN_GRUSE(5x2h)".equals(grConstraintEl.attributeValue("type"))) {
        		c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(),"5x2h",MinimizeNumberOfUsedGroupsOfTime.sGroups5of2h);
        	} else if ("MIN_GRUSE(3x3h)".equals(grConstraintEl.attributeValue("type"))) {
        		c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(),"3x3h",MinimizeNumberOfUsedGroupsOfTime.sGroups3of3h);
        	} else if ("MIN_GRUSE(2x5h)".equals(grConstraintEl.attributeValue("type"))) {
        		c = new MinimizeNumberOfUsedGroupsOfTime(getModel().getProperties(),"2x5h",MinimizeNumberOfUsedGroupsOfTime.sGroups2of5h);
            } else {
            	c = new GroupConstraint(Long.valueOf(grConstraintEl.attributeValue("id")), grConstraintEl.attributeValue("type"), grConstraintEl.attributeValue("pref"));
            }
            getModel().addConstraint(c);
            for (Iterator i2=grConstraintEl.elementIterator("class");i2.hasNext();) {
                String classId = ((Element)i2.next()).attributeValue("id");
                c.addVariable((Lecture)lectures.get(classId));
            }
            grConstraintElements.put(grConstraintEl.attributeValue("id"),grConstraintEl);
            groupConstraints.put(grConstraintEl.attributeValue("id"),c);
            iProgress.incProgress();
        }
        
        iProgress.setPhase("Loading students ...",root.element("students").elements("student").size());
        boolean initialSectioning = true;
        Hashtable jenrlConstraints = new Hashtable();
        Hashtable students = new Hashtable();
        Hashtable offering2students = new Hashtable();
        for (Iterator i1=root.element("students").elementIterator("student");i1.hasNext();) {
            Element studentEl = (Element)i1.next();
            Vector lecturesThisStudent = new Vector();
            Long studentId = Long.valueOf(studentEl.attributeValue("id"));
            Student student = (Student)students.get(studentId);
            if (student==null) {
            	student = new Student(studentId);
            	students.put(studentId, student);
            }
            for (Iterator i2=studentEl.elementIterator("offering");i2.hasNext();) {
            	Element ofEl = (Element)i2.next(); 
            	Long offeringId = Long.valueOf(ofEl.attributeValue("id"));
            	student.addOffering(offeringId, Double.parseDouble(ofEl.attributeValue("weight", "1.0")));
            	Set studentsThisOffering = (Set)offering2students.get(offeringId);
            	if (studentsThisOffering==null) {
            		studentsThisOffering = new HashSet();
            		offering2students.put(offeringId, studentsThisOffering);
            	}
            	studentsThisOffering.add(student);
            }
            for (Iterator i2=studentEl.elementIterator("class");i2.hasNext();) {
                String classId = ((Element)i2.next()).attributeValue("id");
                Lecture lecture = (Lecture)lectures.get(classId);
                if (lecture.isCommitted()) {
                	Placement placement = (Placement)assignedPlacements.get(lecture);
                	student.addCommitedPlacement(placement);
                } else {
                	student.addLecture(lecture);
                	lecture.addStudent(student);
                	lecturesThisStudent.add(lecture);
                	initialSectioning = false;
                }
            }
            
            if (student!=null) {
                for (Iterator i2=studentEl.elementIterator("prohibited-class");i2.hasNext();) {
                    String classId = ((Element)i2.next()).attributeValue("id");
                    Lecture lecture = (Lecture)lectures.get(classId);
                    student.addCanNotEnroll(lecture);
                }
            }
            
            iProgress.incProgress();
        }
        
        for (Enumeration e1=sameLectures.elements();e1.hasMoreElements();) {
            Vector sames = (Vector)e1.nextElement();
            for (Enumeration e2=sames.elements(); e2.hasMoreElements();) {
                Lecture lect = (Lecture)e2.nextElement();
                lect.setSameSubpartLectures(sames);
            }
        }

        if (initialSectioning) {
        	iProgress.setPhase("Initial sectioning ...", offering2students.size());
        	for (Iterator i1=offering2students.entrySet().iterator();i1.hasNext();) {
        		Map.Entry entry = (Map.Entry)i1.next();
        		Long offeringId = (Long)entry.getKey();
        		Set studentsThisOffering = (Set)entry.getValue();
        		Vector altConfigs = (Vector)alternativeConfigurations.get(offeringId);
        		InitialSectioning.initialSectioningCfg(iProgress, offeringId, String.valueOf(offeringId), studentsThisOffering, altConfigs);
        		iProgress.incProgress();
        	}
        	for (Enumeration e=students.elements();e.hasMoreElements();) {
        		((Student)e.nextElement()).clearDistanceCache();
        	}
        }

        iProgress.setPhase("Computing jenrl ...",students.size());
        Hashtable jenrls = new Hashtable();
        for (Iterator i1=students.values().iterator();i1.hasNext();) {
            Student st = (Student)i1.next();
            for (Iterator i2=st.getLectures().iterator();i2.hasNext();) {
                Lecture l1 = (Lecture)i2.next();
                for (Iterator i3=st.getLectures().iterator();i3.hasNext();) {
                    Lecture l2 = (Lecture)i3.next();
                    if (l1.getId()>=l2.getId()) continue;
                    Hashtable x = (Hashtable)jenrls.get(l1);
                    if (x==null) { x = new Hashtable(); jenrls.put(l1, x); }
                    JenrlConstraint jenrl = (JenrlConstraint)x.get(l2);
                    if (jenrl==null) {
                        jenrl = new JenrlConstraint();
                        jenrl.addVariable(l1);
                        jenrl.addVariable(l2);
                        getModel().addConstraint(jenrl);
                        x.put(l2, jenrl);
                    }
                    jenrl.incJenrl(st);
                }
            }
            iProgress.incProgress();
        }

        if (iDeptBalancing) {
        	iProgress.setPhase("Creating dept. spread constraints ...",getModel().variables().size());
            Hashtable depSpreadConstraints = new Hashtable();
            for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
                Lecture lecture = (Lecture)e.nextElement();
                if (lecture.getDepartment()==null) continue;
                DepartmentSpreadConstraint deptConstr = (DepartmentSpreadConstraint)depSpreadConstraints.get(lecture.getDepartment());
                if (deptConstr==null) {
                	String name = (String)depts.get(lecture.getDepartment());
                    deptConstr = new DepartmentSpreadConstraint(getModel().getProperties(),lecture.getDepartment(),(name!=null?name:"d"+lecture.getDepartment()));
                    depSpreadConstraints.put(lecture.getDepartment(),deptConstr);
                    getModel().addConstraint(deptConstr);
                }
                deptConstr.addVariable(lecture);
                iProgress.incProgress();
            }
        }

    	iProgress.setPhase("Purging invalid placements ...", getModel().variables().size());
    	for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
    		Lecture lecture = (Lecture)e.nextElement();
    		lecture.purgeInvalidValues(iInteractiveMode);
    		iProgress.incProgress();
    	}

    	if (currentSolution!=null) {
        	iProgress.setPhase("Creating best assignment ...",2*getModel().variables().size());
        	for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
        		Lecture lecture = (Lecture)e.nextElement();
        		iProgress.incProgress();
        		Placement placement = (Placement)lecture.getBestAssignment();
        		if (placement==null) continue;
        		lecture.assign(0,placement);
        	}
        	
        	currentSolution.saveBest();
        	for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
        		Lecture lecture = (Lecture)e.nextElement();
        		iProgress.incProgress();
        		if (lecture.getAssignment()!=null)
        			lecture.unassign(0);
        	}
        }
        
    	iProgress.setPhase("Creating initial assignment ...",assignedPlacements.size());
        for (Iterator ip=assignedPlacements.entrySet().iterator();ip.hasNext();) {
            Map.Entry entry = (Map.Entry)ip.next();
            Lecture lecture = (Lecture)entry.getKey();
            Placement placement = (Placement)entry.getValue();
            Hashtable conflictConstraints = getModel().conflictConstraints(placement);
            if (conflictConstraints.isEmpty()) {
                lecture.assign(0,placement);
            } else {
                sLogger.warn("WARNING: Unable to assign "+lecture.getName()+" := "+placement.getName());
                sLogger.debug("  Reason:");
                for (Enumeration ex=conflictConstraints.keys();ex.hasMoreElements();) {
                    Constraint c = (Constraint)ex.nextElement();
                    Collection vals = (Collection)conflictConstraints.get(c);
                    for (Iterator i=vals.iterator();i.hasNext();) {
                        Value v = (Value) i.next();
                        sLogger.debug("    "+v.variable().getName()+" = "+v.getName());
                    }
                    sLogger.debug("    in constraint "+c);
                }
            }
            iProgress.incProgress();
        }
        
		if (initialSectioning && !getModel().assignedVariables().isEmpty() && !getModel().getProperties().getPropertyBoolean("Global.LoadStudentEnrlsFromSolution", false))
			getModel().switchStudents();

        if (iForcedPerturbances>0) {
        	iProgress.setPhase("Forcing perturbances",iForcedPerturbances);
            for (int i=0;i<iForcedPerturbances;i++) {
            	iProgress.setProgress(i);
                Variable var = null;
                do {
                    var = (Variable)ToolBox.random(getModel().variables());
                } while (var.getInitialAssignment()==null || var.values().size()<=1);
                var.removeInitialValue();
            }
        }

		for (Enumeration e=getModel().constraints().elements();e.hasMoreElements();) {
			Constraint c = (Constraint)e.nextElement();
			if (c instanceof SpreadConstraint)
				((SpreadConstraint)c).init();
			if (c instanceof DiscouragedRoomConstraint)
				((DiscouragedRoomConstraint)c).setEnabled(true);
			if (c instanceof MinimizeNumberOfUsedRoomsConstraint)
				((MinimizeNumberOfUsedRoomsConstraint)c).setEnabled(true);
			if (c instanceof MinimizeNumberOfUsedGroupsOfTime)
				((MinimizeNumberOfUsedGroupsOfTime)c).setEnabled(true);
		}
		
        try {
            getSolver().getClass().getMethod("load", new Class[] {Element.class}).invoke(getSolver(), new Object[]{root});
        } catch (Exception e) {}

		iProgress.setPhase("Done",1);iProgress.incProgress();

        sLogger.debug("Model successfully loaded.");
        iProgress.info("Model successfully loaded.");
    }
    
    public static Date getDate(int year, int dayOfYear) {
        Calendar c = Calendar.getInstance(Locale.US);
        c.set(year,1,1,0,0,0);
        c.set(Calendar.DAY_OF_YEAR,dayOfYear);
        return c.getTime();
    }
}
