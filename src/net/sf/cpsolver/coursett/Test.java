package net.sf.cpsolver.coursett;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.heuristics.UniversalPerturbationsCounter;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.extension.MacPropagation;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ProgressWriter;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * A main class for running of the solver from command line.
 * <br><br>
 * Usage:<br>
 * java -Xmx1024m -jar coursett1.1.jar config.properties [input_file] [output_folder]<br>
 * <br>
 * See http://www.unitime.org for example configuration files and banchmark data sets.<br><br>
 * 
 * The test does the following steps:<ul>
 * <li>Provided property file is loaded (see {@link DataProperties}).
 * <li>Output folder is created (General.Output property) and loggings is setup (using log4j).
 * <li>Input data are loaded (calling {@link TimetableLoader#load()}).
 * <li>Solver is executed (see {@link Solver}).
 * <li>Resultant solution is saved (calling {@link TimetableSaver#save()}, when General.Save property is set to true.
 * </ul>
 * Also, a log and a CSV (comma separated text file) is created in the output folder. 
 *
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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

public class Test implements SolutionListener {
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyMMdd_HHmmss",java.util.Locale.US);
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.000",new java.text.DecimalFormatSymbols(Locale.US));
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Test.class);
    
    private PrintWriter iCSVFile = null;
    
    private MacPropagation iProp = null;
    private int iLastNotified = -1;
 
    private boolean initialized = false;
    private Solver iSolver =null;
    
    /** Current version */
    public static String getVersionString() {
        return "IFS Timetable Solver v"+Constants.getVersion()+" build"+Constants.getBuildNumber()+", "+Constants.getReleaseDate();
    }
    
    /** Solver initialization*/
    public void init(Solver solver) {
    	iSolver = solver;
        solver.currentSolution().addSolutionListener(this);
    }
    
    /** 
     * Return name of the class that is used for loading the data. 
     * This class needs to extend class {@link TimetableLoader}.
     * It can be also defined in configuration, using TimetableLoader property.
     **/
    private String getTimetableLoaderClass(DataProperties properties) {
        String loader = properties.getProperty("TimetableLoader");
        if (loader!=null) return loader;
        if (properties.getPropertyInt("General.InputVersion",-1)>=0)
            return "org.unitime.timetable.solver.TimetableDatabaseLoader";
        else
            return "net.sf.cpsolver.coursett.TimetableXMLLoader";
    }
    
    /** 
     * Return name of the class that is used for loading the data. 
     * This class needs to extend class {@link TimetableSaver}. 
     * It can be also defined in configuration, using TimetableSaver property. 
     **/
    private String getTimetableSaverClass(DataProperties properties) {
        String saver = properties.getProperty("TimetableSaver");
        if (saver!=null) return saver;
        if (properties.getPropertyInt("General.InputVersion",-1)>=0)
            return "org.unitime.timetable.solver.TimetableDatabaseSaver";
        else
            return "net.sf.cpsolver.coursett.TimetableXMLSaver";
    }

    /** Solver Test
     * @param args command line arguments
     */ 
	public Test(String[] args) {
        try {
            DataProperties properties = ToolBox.loadProperties(new java.io.File(args[0]));
            properties.putAll(System.getProperties());
            properties.setProperty("General.Output", properties.getProperty("General.Output",".")+File.separator+(sDateFormat.format(new Date())));
            if (args.length>1)
                properties.setProperty("General.Input", args[1]);
            if (args.length>2)
                properties.setProperty("General.Output", args[2]+File.separator+(sDateFormat.format(new Date())));
            System.out.println("Output folder: "+properties.getProperty("General.Output"));
            ToolBox.configureLogging(properties.getProperty("General.Output"),properties,false,false);
            
            File outDir = new File(properties.getProperty("General.Output","."));
            outDir.mkdirs();
            
            Solver solver = new TimetableSolver(properties);
            TimetableModel model = new TimetableModel(properties);
            Progress.getInstance(model).addProgressListener(new ProgressWriter(System.out));
            
            TimetableLoader loader = (TimetableLoader)Class.forName(getTimetableLoaderClass(properties)).getConstructor(new Class[] {TimetableModel.class}).newInstance(new Object[] {model});
            loader.load();
            
            solver.setInitalSolution(model);
            init(solver);
            
            iCSVFile = new PrintWriter(new FileWriter(outDir.toString()+File.separator+"stat.csv"));
            String colSeparator = ";";
            iCSVFile.println(
                    "Assigned"+colSeparator+
                    "Assigned[%]"+colSeparator+
                    "Time[min]"+colSeparator+
                    "Iter"+colSeparator+
                    "IterYield[%]"+colSeparator+
                    "Speed[it/s]"+colSeparator+
                    "AddedPert"+colSeparator+
                    "AddedPert[%]"+colSeparator+
                    "HardStudentConf"+colSeparator+
                    "StudentConf"+colSeparator+
                    "DistStudentConf"+colSeparator+
                    "TimePref"+colSeparator+
                    "RoomPref"+colSeparator+
                    "DistInstrPref"+colSeparator+
                    "GrConstPref"+colSeparator+
                    "UselessSlots"+colSeparator+
                    "TooBigRooms"+
                    (iProp!=null?colSeparator+"GoodVars"+colSeparator+
                    "GoodVars[%]"+colSeparator+
                    "GoodVals"+colSeparator+
                    "GoodVals[%]":"")
                    );
            iCSVFile.flush();
            
            solver.start();
            solver.getSolverThread().join();
            
            
            long lastIt = solver.lastSolution().getIteration();
            double lastTime = solver.lastSolution().getTime();
            
            if (solver.lastSolution().getBestInfo()!=null) {
                Solution bestSolution = solver.lastSolution();//.cloneBest();
                sLogger.info("Last solution: "+ToolBox.dict2string(bestSolution.getInfo(),1));
                bestSolution.restoreBest();
                sLogger.info("Best solution: "+ToolBox.dict2string(bestSolution.getInfo(),1));
                if (properties.getPropertyBoolean("General.SwitchStudents", true))
                    ((TimetableModel)bestSolution.getModel()).switchStudents();
                sLogger.info("Best solution: "+ToolBox.dict2string(bestSolution.getInfo(),1));
                saveOutputCSV(bestSolution,new File(outDir,"output.csv"));
                
                printSomeStuff(bestSolution);
                
                if (properties.getPropertyBoolean("General.Save",false)) {
                	TimetableSaver saver = (TimetableSaver)Class.forName(getTimetableSaverClass(properties)).getConstructor(new Class[] {Solver.class}).newInstance(new Object[] {solver});
                	if ((saver instanceof TimetableXMLSaver) && properties.getProperty("General.SolutionFile")!=null)
                	    ((TimetableXMLSaver)saver).save(new File(properties.getProperty("General.SolutionFile")));
                	else
                	    saver.save();
                }
            } else sLogger.info("Last solution:"+ToolBox.dict2string(solver.lastSolution().getInfo(),1));
            
            iCSVFile.close();
            
            sLogger.info("Total number of done iteration steps:"+lastIt);
            sLogger.info("Achieved speed: "+sDoubleFormat.format(lastIt/lastTime)+" iterations/second");
            
            PrintWriter out = new PrintWriter(new FileWriter(new File(outDir, "solver.html")));
            out.println("<html><title>Save log</title><body>");
            out.println(Progress.getInstance(model).getHtmlLog(Progress.MSGLEVEL_TRACE, true));
            out.println("</html>");
            out.flush(); out.close();
            Progress.removeInstance(model);
            
            System.out.println("Unassigned variables: "+model.nrUnassignedVariables());
            System.exit(model.nrUnassignedVariables());
        } catch (Throwable t) {
            sLogger.error("Test failed.",t);
        }
    }
    
    public static void main(String[] args) {
        new Test(args);
    }
    
    public void bestCleared(Solution solution) {
    }
    
    public void bestRestored(Solution solution) {
    }
    
    public void bestSaved(Solution solution) {
        notify(solution);
    }
    
    public void getInfo(Solution solution, java.util.Dictionary info) {
    }

    public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {
    }
    
    public void solutionUpdated(Solution solution) {
    	if (!initialized) {
    	    for (Enumeration i=iSolver.getExtensions().elements();i.hasMoreElements();) {
    	        Extension extension = (Extension)i.nextElement();
    	        if (extension instanceof MacPropagation)
    	            iProp = (MacPropagation)extension;
    	    }
    	}
        /*
    	if ((solution.getIteration()%10000)==0 && iStat!=null && iStat.getNoGoods()!=null && !iStat.getNoGoods().isEmpty()) {
    		try {
    			ConflictStatisticsInfo info = new ConflictStatisticsInfo();
    			info.load(iStat);
    			
    			File outDir = new File(((TimetableModel)solution.getModel()).getProperties().getProperty("General.Output","."));
    			PrintWriter pw = new PrintWriter(new FileWriter(outDir.toString()+File.separator+"cbs_"+(solution.getIteration()/1000)+"k.html"));
    			
    			pw.println("<html><head>");
    			ConflictStatisticsInfo.printHtmlHeader(pw,true);
    			pw.println("</head><body>");
    			info.printHtml(pw,1.00,ConflictStatisticsInfo.TYPE_CONSTRAINT_BASED, true);
    			pw.println("<br><hr>");
    			info.printHtml(pw,1.00,ConflictStatisticsInfo.TYPE_VARIABLE_BASED, true);
    			pw.println("</body></html>");
    		
    			pw.flush();
    			pw.close();
    		} catch (Exception e) {
    			sLogger.error(e.getMessage(),e);
    		}
    	}
        */
    }
    
    /** Add a line into the output CSV file when a enw best solution is found. */
    public void notify(Solution solution) {
        String colSeparator = ";";
        if (!solution.getModel().unassignedVariables().isEmpty() && iLastNotified==solution.getModel().assignedVariables().size()) return;
        iLastNotified=solution.getModel().assignedVariables().size();
        if (iCSVFile!=null) {
            TimetableModel model = (TimetableModel) solution.getModel();
            iCSVFile.print(model.variables().size()-model.unassignedVariables().size());
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(100.0*(model.variables().size()-model.unassignedVariables().size())/model.variables().size()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(((double)solution.getTime())/60.0));
            iCSVFile.print(colSeparator);
            iCSVFile.print(solution.getIteration());
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(100.0*(model.variables().size()-model.unassignedVariables().size())/solution.getIteration()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(((double)solution.getIteration())/(double)solution.getTime()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(model.perturbVariables().size());
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(100.0*model.perturbVariables().size()/model.variables().size()));
            
            long studentConflicts = 0;
            long hardStudentConflicts = 0;
            long uselessSlots = 0;
            for (Enumeration it1=((TimetableModel)solution.getModel()).getRoomConstraints().elements();it1.hasMoreElements();) {
                RoomConstraint constraint = (RoomConstraint)it1.nextElement();
                uselessSlots+=constraint.countUselessSlots();
            }
            for (Enumeration it1=((TimetableModel)solution.getModel()).getJenrlConstraints().elements();it1.hasMoreElements();) {
                JenrlConstraint jenrl = (JenrlConstraint) it1.nextElement();
                if (jenrl.isInConflict()) {
                    studentConflicts+=jenrl.getJenrl();
                    Lecture l1 = (Lecture)jenrl.first();
                    Lecture l2 = (Lecture)jenrl.second();
                    if (l1.areStudentConflictsHard(l2))
                        hardStudentConflicts+=jenrl.getJenrl();
                }
            }
            iCSVFile.print(colSeparator);
            iCSVFile.print(hardStudentConflicts);
            iCSVFile.print(colSeparator);
            iCSVFile.print(studentConflicts);
            iCSVFile.print(colSeparator);
            iCSVFile.print(((TimetableModel)solution.getModel()).getStudentDistanceConflicts());
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(((TimetableModel)solution.getModel()).getGlobalTimePreference()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(((TimetableModel)solution.getModel()).getGlobalRoomPreference());
            iCSVFile.print(colSeparator);
            iCSVFile.print(((TimetableModel)solution.getModel()).getInstructorDistancePreference());
            iCSVFile.print(colSeparator);
            iCSVFile.print(((TimetableModel)solution.getModel()).getGlobalGroupConstraintPreference());
            iCSVFile.print(colSeparator);
            iCSVFile.print(uselessSlots);
            iCSVFile.print(colSeparator);
            iCSVFile.print(((TimetableModel)solution.getModel()).countTooBigRooms());
            if (iProp!=null) {
                if (solution.getModel().unassignedVariables().size()>0) {
                    int goodVariables=0;
                    long goodValues=0;
                    long allValues=0;
                    for (Enumeration i=solution.getModel().unassignedVariables().elements();i.hasMoreElements();) {
                        Lecture variable = (Lecture)i.nextElement();
                        goodValues += iProp.goodValues(variable).size();
                        allValues += variable.values().size();
                        if (!iProp.goodValues(variable).isEmpty()) goodVariables++;
                    }
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(goodVariables);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(sDoubleFormat.format(100.0*goodVariables/solution.getModel().unassignedVariables().size()));
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(goodValues);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(sDoubleFormat.format(100.0*goodValues/allValues));
                } else {
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(colSeparator);
                }
            }
            iCSVFile.println();
            iCSVFile.flush();
        }
    }
    
    /** Print room utilization */
    public static void printRoomInfo(PrintWriter pw, TimetableModel model) {
    	pw.println("Room info:");
    	pw.println("id, name, size, used_day, used_total");
    	for (Enumeration e=model.getRoomConstraints().elements();e.hasMoreElements();) {
    		RoomConstraint rc = (RoomConstraint)e.nextElement();
    		int used_day = 0;
    		int used_total = 0;
    		for (int day=0;day<Constants.NR_DAYS_WEEK;day++) {
    			for (int time=0;time<Constants.SLOTS_PER_DAY_NO_EVENINGS;time++) {
    				if (!rc.getResource(day*Constants.SLOTS_PER_DAY+time+Constants.DAY_SLOTS_FIRST).isEmpty())
    					used_day++;
    			}
    		}
    		for (int day=0;day<Constants.DAY_CODES.length;day++) {
    			for (int time=0;time<Constants.SLOTS_PER_DAY;time++) {
    				if (!rc.getResource(day*Constants.SLOTS_PER_DAY+time).isEmpty())
    					used_total++;
    			}
    		}
    		pw.println(rc.getResourceId()+","+rc.getName()+","+rc.getCapacity()+","+used_day+","+used_total);
    	}
    }
    
    /** Class information */
    public static void printClassInfo(PrintWriter pw, TimetableModel model) {
    	pw.println("Class info:");
    	pw.println("id, name, min_class_limit, max_class_limit, room2limit_ratio, half_hours");
    	for (Enumeration e=model.variables().elements();e.hasMoreElements();) {
    		Lecture lecture = (Lecture)e.nextElement();
    		TimeLocation time = (TimeLocation)lecture.timeLocations().firstElement();
    		pw.println(lecture.getClassId()+","+lecture.getName()+","+lecture.minClassLimit()+","+lecture.maxClassLimit()+","+lecture.roomToLimitRatio()+","+(time.getNrSlotsPerMeeting()*time.getNrMeetings()));
    	}
    }
    
    /** Create info.txt with some more information about the problem */
    public static void printSomeStuff(Solution solution) throws IOException {
        TimetableModel model = (TimetableModel)solution.getModel();
        File outDir = new File(model.getProperties().getProperty("General.Output","."));
        PrintWriter pw = new PrintWriter(new FileWriter(outDir.toString()+File.separator+"info.txt"));
        PrintWriter pwi = new PrintWriter(new FileWriter(outDir.toString()+File.separator+"info.csv"));
        String name = new File(model.getProperties().getProperty("General.Input")).getName();
        pwi.println("Instance,"+name.substring(0, name.lastIndexOf('.')));
        pw.println("Solution info: "+ToolBox.dict2string(solution.getInfo(),1));
        pw.println("Bounds: "+ToolBox.dict2string(model.getBounds(),1));
        Hashtable info = solution.getInfo();
        for (Enumeration e=ToolBox.sortEnumeration(info.keys());e.hasMoreElements();) {
            String key = (String)e.nextElement();
            if (key.equals("Memory usage")) continue;
            if (key.equals("Iteration")) continue;
            if (key.equals("Time")) continue;
            String value = (String)info.get(key);
            if (value.indexOf(' ')>0) value=value.substring(0,value.indexOf(' '));
            pwi.println(key+","+value);
        }
    	printRoomInfo(pw, model);
    	printClassInfo(pw, model);
        long nrValues = 0;
        long nrTimes = 0;
        long nrRooms = 0;
        double totalMaxNormTimePref = 0.0;
        double totalMinNormTimePref = 0.0;
        double totalNormTimePref = 0.0;
        int totalMaxRoomPref = 0;
        int totalMinRoomPref = 0;
        int totalRoomPref = 0;
        long nrStudentEnrls = 0;
        long nrInevitableStudentConflicts = 0;
        long nrJenrls = 0;
        int nrHalfHours = 0;
        int nrMeetings = 0;
        int totalMinLimit = 0;
        int totalMaxLimit = 0;
        long nrReqRooms = 0;
        int nrSingleValueVariables = 0;
        int nrSingleTimeVariables = 0;
        int nrSingleRoomVariables = 0;
        long totalAvailableMinRoomSize = 0;
        long totalAvailableMaxRoomSize = 0;
        long totalRoomSize = 0;
        long nrOneOrMoreRoomVariables = 0;
        long nrOneRoomVariables = 0;
        HashSet students = new HashSet();
        HashSet offerings = new HashSet();
        HashSet configs = new HashSet();
        HashSet subparts = new HashSet();
        int[] sizeLimits = new int[] {0, 25, 50, 75, 100, 150, 200, 400};
        int[] nrRoomsOfSize = new int[sizeLimits.length];
        int[] minRoomOfSize = new int[sizeLimits.length];
        int[] maxRoomOfSize = new int[sizeLimits.length];
        int[] totalUsedSlots = new int[sizeLimits.length];
        int[] totalUsedSeats = new int[sizeLimits.length];
        int[] totalUsedSeats2 = new int[sizeLimits.length];
        for (Enumeration e1=model.variables().elements();e1.hasMoreElements();) {
            Lecture lect = (Lecture)e1.nextElement();
            if (lect.getConfiguration()!=null) {
                offerings.add(lect.getConfiguration().getOfferingId());
                configs.add(lect.getConfiguration().getConfigId());
            }
            subparts.add(lect.getSchedulingSubpartId());
            nrStudentEnrls += (lect.students()==null?0:lect.students().size());
            students.addAll(lect.students());
            nrValues += lect.values().size();
            nrReqRooms += lect.getNrRooms();
            nrTimes += lect.timeLocations().size();
            nrRooms += lect.roomLocations().size();
            totalMinLimit += lect.minClassLimit();
            totalMaxLimit += lect.maxClassLimit();
            if (!lect.values().isEmpty()) {
                Placement p = (Placement)lect.values().firstElement();
                nrMeetings += p.getTimeLocation().getNrMeetings();
                nrHalfHours += p.getTimeLocation().getNrMeetings() * p.getTimeLocation().getNrSlotsPerMeeting();
                totalMaxNormTimePref += lect.getMinMaxTimePreference()[1];
                totalMinNormTimePref += lect.getMinMaxTimePreference()[0];
                totalNormTimePref += Math.abs(lect.getMinMaxTimePreference()[1]-lect.getMinMaxTimePreference()[0]);
                totalMaxRoomPref += lect.getMinMaxRoomPreference()[1];
                totalMinRoomPref += lect.getMinMaxRoomPreference()[0];
                totalRoomPref += Math.abs(lect.getMinMaxRoomPreference()[1]-lect.getMinMaxRoomPreference()[0]);
                TimeLocation time = p.getTimeLocation();
                boolean hasRoomConstraint = false;
                for (Enumeration g=lect.roomLocations().elements();g.hasMoreElements();) {
                    RoomLocation roomLocation = (RoomLocation)g.nextElement();
                    if (roomLocation.getRoomConstraint().getConstraint())
                        hasRoomConstraint = true;
                }
                if (hasRoomConstraint && lect.getNrRooms()>0) {
                    for (int d=0;d<Constants.NR_DAYS_WEEK;d++) {
                        if ((time.getDayCode() & Constants.DAY_CODES[d]) == 0) continue;
                        for (int t=Math.max(time.getStartSlot(),Constants.DAY_SLOTS_FIRST);t<=Math.min(time.getStartSlot()+time.getLength()-1, Constants.DAY_SLOTS_LAST);t++) {
                            for (int l=0;l<sizeLimits.length;l++) {
                                if (sizeLimits[l]<=lect.minRoomSize()) {
                                    totalUsedSlots[l]+=lect.getNrRooms();
                                    totalUsedSeats[l]+=lect.classLimit();
                                    totalUsedSeats2[l]+=lect.minRoomSize()*lect.getNrRooms();
                                }
                            }
                        }
                    }
                }
            }
            if (lect.values().size()==1) {
                nrSingleValueVariables++;
            }
            if (lect.timeLocations().size()==1) {
            	nrSingleTimeVariables++;
            }
            if (lect.roomLocations().size()==1) {
            	nrSingleRoomVariables++;
            }
            if (lect.getNrRooms()==1) {
            	nrOneRoomVariables++;
            }
            if (lect.getNrRooms()>0) {
            	nrOneOrMoreRoomVariables++;
            }
            if (!lect.roomLocations().isEmpty()) {
            	int minRoomSize = Integer.MAX_VALUE;
            	int maxRoomSize = Integer.MIN_VALUE;
            	for (Enumeration e2=lect.roomLocations().elements();e2.hasMoreElements();) {
            		RoomLocation rl = (RoomLocation)e2.nextElement();
            		minRoomSize = Math.min(minRoomSize, rl.getRoomSize());
            		maxRoomSize = Math.max(maxRoomSize, rl.getRoomSize());
            		totalRoomSize += rl.getRoomSize();
            	}
            	totalAvailableMinRoomSize += minRoomSize;
            	totalAvailableMaxRoomSize += maxRoomSize;
            }
        }
        for (Enumeration e=model.getJenrlConstraints().elements();e.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)e.nextElement();
            nrJenrls += jenrl.getJenrl();
            if (((Lecture)jenrl.first()).timeLocations().size()==1 && ((Lecture)jenrl.second()).timeLocations().size()==1) {
            	TimeLocation t1 = (TimeLocation)(((Lecture)jenrl.first()).timeLocations()).firstElement();
            	TimeLocation t2 = (TimeLocation)(((Lecture)jenrl.second()).timeLocations()).firstElement();
            	if (t1.hasIntersection(t2)) {
            		nrInevitableStudentConflicts += jenrl.getJenrl();
            		pw.println("Inevitable "+jenrl.getJenrl()+" student conflicts between "+jenrl.first()+" "+t1+" and "+jenrl.second()+" "+t2);
            	} else if (jenrl.first().values().size()==1 && jenrl.second().values().size()==1) {
            		Placement p1 = (Placement)jenrl.first().values().firstElement();
            		Placement p2 = (Placement)jenrl.second().values().firstElement();
            		if (JenrlConstraint.isInConflict(p1,p2)) {
            			nrInevitableStudentConflicts += jenrl.getJenrl();
            			pw.println("Inevitable "+jenrl.getJenrl()+(p1.getTimeLocation().hasIntersection(p2.getTimeLocation())?"":" distance")+" student conflicts between "+p1+" and "+p2);
            		}
            	}
            }
        }
        int totalCommitedPlacements = 0;
        for (Iterator i=students.iterator();i.hasNext();) {
            Student student = (Student)i.next();
            if (student.getCommitedPlacements()!=null)
                totalCommitedPlacements += student.getCommitedPlacements().size();
        }
        pw.println("Total number of classes: "+model.variables().size());
        pwi.println("Number of classes,"+model.variables().size());
        pw.println("Total number of instructional offerings: "+offerings.size()+" ("+sDoubleFormat.format(100.0*offerings.size()/model.variables().size())+"%)");
        //pwi.println("Number of instructional offerings,"+offerings.size());
        pw.println("Total number of configurations: "+configs.size()+" ("+sDoubleFormat.format(100.0*configs.size()/model.variables().size())+"%)");
        pw.println("Total number of scheduling subparts: "+subparts.size()+" ("+sDoubleFormat.format(100.0*subparts.size()/model.variables().size())+"%)");
        //pwi.println("Number of scheduling subparts,"+subparts.size());
        pw.println("Average number classes per subpart: "+sDoubleFormat.format(1.0*model.variables().size()/subparts.size()));
        pwi.println("Avg. classes per instruction,"+sDoubleFormat.format(1.0*model.variables().size()/subparts.size()));
        pw.println("Average number classes per config: "+sDoubleFormat.format(1.0*model.variables().size()/configs.size()));
        pw.println("Average number classes per offering: "+sDoubleFormat.format(1.0*model.variables().size()/offerings.size()));
        pw.println("Total number of classes with only one value: "+nrSingleValueVariables+" ("+sDoubleFormat.format(100.0*nrSingleValueVariables/model.variables().size())+"%)");
        pw.println("Total number of classes with only one time: "+nrSingleTimeVariables+" ("+sDoubleFormat.format(100.0*nrSingleTimeVariables/model.variables().size())+"%)");
        pw.println("Total number of classes with only one room: "+nrSingleRoomVariables+" ("+sDoubleFormat.format(100.0*nrSingleRoomVariables/model.variables().size())+"%)");
        pwi.println("Classes with single value,"+nrSingleValueVariables);
        //pwi.println("Classes with only one time/room,"+nrSingleTimeVariables+"/"+nrSingleRoomVariables);
        pw.println("Total number of classes requesting no room: "+(model.variables().size()-nrOneOrMoreRoomVariables)+" ("+sDoubleFormat.format(100.0*(model.variables().size()-nrOneOrMoreRoomVariables)/model.variables().size())+"%)");
        pw.println("Total number of classes requesting one room: "+nrOneRoomVariables+" ("+sDoubleFormat.format(100.0*nrOneRoomVariables/model.variables().size())+"%)");
        pw.println("Total number of classes requesting one or more rooms: "+nrOneOrMoreRoomVariables+" ("+sDoubleFormat.format(100.0*nrOneOrMoreRoomVariables/model.variables().size())+"%)");
        //pwi.println("% classes requesting no room,"+sDoubleFormat.format(100.0*(model.variables().size()-nrOneOrMoreRoomVariables)/model.variables().size())+"%");
        //pwi.println("% classes requesting one room,"+sDoubleFormat.format(100.0*nrOneRoomVariables/model.variables().size())+"%");
        //pwi.println("% classes requesting two or more rooms,"+sDoubleFormat.format(100.0*(nrOneOrMoreRoomVariables-nrOneRoomVariables)/model.variables().size())+"%");
        pw.println("Average number of requested rooms: "+sDoubleFormat.format(1.0*nrReqRooms/model.variables().size()));
        pw.println("Average minimal class limit: "+sDoubleFormat.format(1.0*totalMinLimit/model.variables().size()));
        pw.println("Average maximal class limit: "+sDoubleFormat.format(1.0*totalMaxLimit/model.variables().size()));
        //pwi.println("Average class limit,"+sDoubleFormat.format(1.0*(totalMinLimit+totalMaxLimit)/(2*model.variables().size())));
        pw.println("Average number of placements: "+sDoubleFormat.format(1.0*nrValues/model.variables().size()));
        //pwi.println("Average domain size,"+sDoubleFormat.format(1.0*nrValues/model.variables().size()));
        pwi.println("Avg. domain size,"+sDoubleFormat.format(1.0*nrValues/model.variables().size()));
        pw.println("Average number of time locations: "+sDoubleFormat.format(1.0*nrTimes/model.variables().size()));
        pwi.println("Avg. number of avail. times/rooms,"+sDoubleFormat.format(1.0*nrTimes/model.variables().size())+"/"+sDoubleFormat.format(1.0*nrRooms/model.variables().size()));
        pw.println("Average number of room locations: "+sDoubleFormat.format(1.0*nrRooms/model.variables().size()));
        pw.println("Average minimal requested room size: "+sDoubleFormat.format(1.0*totalAvailableMinRoomSize/nrOneOrMoreRoomVariables));
        pw.println("Average maximal requested room size: "+sDoubleFormat.format(1.0*totalAvailableMaxRoomSize/nrOneOrMoreRoomVariables));
        pw.println("Average requested room sizes: "+sDoubleFormat.format(1.0*totalRoomSize/nrRooms));
        pw.println("Average requested room size,"+sDoubleFormat.format(1.0*totalRoomSize/nrRooms));
        pw.println("Average maximum normalized time preference: "+sDoubleFormat.format(totalMaxNormTimePref/model.variables().size()));
        pw.println("Average minimum normalized time preference: "+sDoubleFormat.format(totalMinNormTimePref/model.variables().size()));
        pw.println("Average normalized time preference,"+sDoubleFormat.format(totalNormTimePref/model.variables().size()));
        pw.println("Average maximum room preferences: "+sDoubleFormat.format(1.0*totalMaxRoomPref/nrOneOrMoreRoomVariables));
        pw.println("Average minimum room preferences: "+sDoubleFormat.format(1.0*totalMinRoomPref/nrOneOrMoreRoomVariables));
        pw.println("Average room preferences,"+sDoubleFormat.format(1.0*totalRoomPref/nrOneOrMoreRoomVariables));
        pw.println("Total number of students:"+students.size());
        pwi.println("Number of students,"+students.size());
        pw.println("Number of inevitable student conflicts,"+nrInevitableStudentConflicts);
        pw.println("Total amount of student enrollments: "+nrStudentEnrls);
        pw.println("Number of student enrollments,"+nrStudentEnrls);
        pw.println("Total amount of joined enrollments: "+nrJenrls);
        pw.println("Number of joint student enrollments,"+nrJenrls);
        pw.println("Average number of students: "+sDoubleFormat.format(1.0*students.size()/model.variables().size()));
        pw.println("Average number of enrollemnts (per student): "+sDoubleFormat.format(1.0*nrStudentEnrls/students.size()));
        pwi.println("Avg. number of classes per student,"+sDoubleFormat.format(1.0*nrStudentEnrls/students.size()));
        pwi.println("Avg. number of committed classes per student,"+sDoubleFormat.format(1.0*totalCommitedPlacements/students.size()));
        pw.println("Total amount of inevitable student conflicts: "+nrInevitableStudentConflicts+" ("+sDoubleFormat.format(100.0*nrInevitableStudentConflicts/nrStudentEnrls)+"%)");
        pw.println("Average number of meetings (per class): "+sDoubleFormat.format(1.0*nrMeetings/model.variables().size()));
        pw.println("Average number of hours per class: "+sDoubleFormat.format(1.0*nrHalfHours/model.variables().size()/12.0));
        pwi.println("Avg. number of meetings per class,"+sDoubleFormat.format(1.0*nrMeetings/model.variables().size()));
        pwi.println("Avg. number of hours per class,"+sDoubleFormat.format(1.0*nrHalfHours/model.variables().size()/12.0));
        int minRoomSize = Integer.MAX_VALUE;
        int maxRoomSize = Integer.MIN_VALUE;
        int nrDistancePairs = 0;
        double maxRoomDistance = Double.MIN_VALUE;
        double totalRoomDistance = 0.0;
        int[] totalAvailableSlots = new int[sizeLimits.length];
        int[] totalAvailableSeats = new int[sizeLimits.length];
        int totalAllSlots = 0, nrOfRooms = 0;
        totalRoomSize = 0;
        for (Enumeration e1=model.getRoomConstraints().elements();e1.hasMoreElements();) {
        	RoomConstraint rc = (RoomConstraint)e1.nextElement();
        	if (!rc.getConstraint() || rc.variables().isEmpty()) continue;
        	nrOfRooms++;
        	minRoomSize = Math.min(minRoomSize, rc.getCapacity());
        	maxRoomSize = Math.max(maxRoomSize, rc.getCapacity());
        	for (int l=0;l<sizeLimits.length;l++) {
        	    if (sizeLimits[l]<=rc.getCapacity() && (l+1==sizeLimits.length || rc.getCapacity()<sizeLimits[l+1])) {
        	        nrRoomsOfSize[l]++;
        	        if (minRoomOfSize[l]==0)
        	            minRoomOfSize[l]=rc.getCapacity();
        	        else
        	            minRoomOfSize[l]=Math.min(minRoomOfSize[l],rc.getCapacity());
                    if (maxRoomOfSize[l]==0)
                        maxRoomOfSize[l]=rc.getCapacity();
                    else
                        maxRoomOfSize[l]=Math.max(maxRoomOfSize[l],rc.getCapacity());
        	    }
        	}
        	totalRoomSize += rc.getCapacity();
        	if (rc.getPosX()>=0 && rc.getPosY()>=0) {
        		for (Enumeration e2=model.getRoomConstraints().elements();e2.hasMoreElements();) {
        			RoomConstraint rc2 = (RoomConstraint)e2.nextElement();
        			if (rc2.getResourceId().compareTo(rc.getResourceId())>0 && rc2.getPosX()>=0 && rc2.getPosY()>=0) {
        				double distance = Math.sqrt((rc2.getPosX()-rc.getPosX())*(rc2.getPosX()-rc.getPosX()) + (rc2.getPosY()-rc.getPosY())*(rc2.getPosY()-rc.getPosY()));
        				totalRoomDistance += distance;
        				nrDistancePairs ++;
        				maxRoomDistance = Math.max(maxRoomDistance, distance);
        			}
        		}
        	}
            for (int d=0;d<Constants.NR_DAYS_WEEK;d++) {
                for (int t=Constants.DAY_SLOTS_FIRST;t<=Constants.DAY_SLOTS_LAST;t++) {
                    totalAllSlots++;
                    if (rc.isAvailable(d*Constants.SLOTS_PER_DAY+t)) {
                        for (int l=0;l<sizeLimits.length;l++) {
                            if (sizeLimits[l]<=rc.getCapacity()) {
                                totalAvailableSlots[l]++;
                                totalAvailableSeats[l]+=rc.getCapacity();
                            }
                        }
                    }
                }
            }
        }
        pw.println("Total number of rooms: "+nrOfRooms);
        pwi.println("Number of rooms,"+nrOfRooms);
        pw.println("Minimal room size: "+minRoomSize);
        pw.println("Maximal room size: "+maxRoomSize);
        pwi.println("Room size min/max,"+minRoomSize+"/"+maxRoomSize);
        pw.println("Average room size: "+sDoubleFormat.format(1.0*totalRoomSize/model.getRoomConstraints().size()));
        pw.println("Maximal distance between two rooms: "+sDoubleFormat.format(5.0*maxRoomDistance));
        pw.println("Average distance between two rooms: "+sDoubleFormat.format(5.0*totalRoomDistance/nrDistancePairs));
        pw.println("Average distance between two rooms [m],"+sDoubleFormat.format(5.0*totalRoomDistance/nrDistancePairs));
        pw.println("Maximal distance between two rooms [m],"+sDoubleFormat.format(5.0*maxRoomDistance));
        for (int l=0;l<1;l++) {//sizeLimits.length;l++) {
            pwi.println("\"Room frequency (size>="+sizeLimits[l]+", used/avaiable times)\","+sDoubleFormat.format(100.0*totalUsedSlots[l]/totalAvailableSlots[l])+"%");
            pwi.println("\"Room utilization (size>="+sizeLimits[l]+", used/available seats)\","+sDoubleFormat.format(100.0*totalUsedSeats[l]/totalAvailableSeats[l])+"%");
            pwi.println("\"Number of rooms (size>="+sizeLimits[l]+")\","+nrRoomsOfSize[l]);
            pwi.println("\"Min/max room size (size>="+sizeLimits[l]+")\","+minRoomOfSize[l]+"-"+maxRoomOfSize[l]);
            //pwi.println("\"Room utilization (size>="+sizeLimits[l]+", minRoomSize)\","+sDoubleFormat.format(100.0*totalUsedSeats2[l]/totalAvailableSeats[l])+"%");
        }
        pw.println("Average hours available: "+sDoubleFormat.format(1.0*totalAvailableSlots[0]/model.getRoomConstraints().size()/12.0));
        totalAllSlots = 0;
        int totalInstructedClasses = 0;
        for (Enumeration e1=model.getInstructorConstraints().elements();e1.hasMoreElements();) {
        	InstructorConstraint ic = (InstructorConstraint)e1.nextElement();
        	totalInstructedClasses += ic.variables().size();
        	for (int d=0;d<Constants.NR_DAYS_WEEK;d++) {
        		for (int t=Constants.DAY_SLOTS_FIRST;t<=Constants.DAY_SLOTS_LAST;t++) {
        			totalAllSlots++;
        		}
        	}
        }
        pw.println("Total number of instructors: "+model.getInstructorConstraints().size());
        pwi.println("Number of instructors,"+model.getInstructorConstraints().size());
        pw.println("Total class-instructor assignments: "+totalInstructedClasses+" ("+sDoubleFormat.format(100.0*totalInstructedClasses/model.variables().size())+"%)");
        pwi.println("Number of class-instructor assignments,"+totalInstructedClasses);
        pw.println("Average classes per instructor: "+sDoubleFormat.format(1.0*totalInstructedClasses/model.getInstructorConstraints().size()));
        pwi.println("Average classes per instructor,"+sDoubleFormat.format(1.0*totalInstructedClasses/model.getInstructorConstraints().size()));
        //pw.println("Average hours available: "+sDoubleFormat.format(1.0*totalAvailableSlots/model.getInstructorConstraints().size()/12.0));
        //pwi.println("Instructor availability [h],"+sDoubleFormat.format(1.0*totalAvailableSlots/model.getInstructorConstraints().size()/12.0));
        int nrGroupConstraints = model.getGroupConstraints().size() + model.getSpreadConstraints().size();
        int nrHardGroupConstraints = 0;
        int nrVarsInGroupConstraints = 0;
        for (Enumeration e1=model.getGroupConstraints().elements();e1.hasMoreElements();) {
        	GroupConstraint gc = (GroupConstraint)e1.nextElement();
        	if (gc.isHard()) nrHardGroupConstraints++;
        	nrVarsInGroupConstraints += gc.variables().size();
        }
        for (Enumeration e1=model.getSpreadConstraints().elements();e1.hasMoreElements();) {
        	SpreadConstraint sc = (SpreadConstraint)e1.nextElement();
        	nrVarsInGroupConstraints += sc.variables().size();
        }
        pw.println("Total number of group constraints: "+nrGroupConstraints+" ("+sDoubleFormat.format(100.0*nrGroupConstraints/model.variables().size())+"%)");
        //pwi.println("Number of group constraints,"+nrGroupConstraints);
        pw.println("Total number of hard group constraints: "+nrHardGroupConstraints+" ("+sDoubleFormat.format(100.0*nrHardGroupConstraints/model.variables().size())+"%)");
        //pwi.println("Number of hard group constraints,"+nrHardGroupConstraints);
        pw.println("Average classes per group constraint: "+sDoubleFormat.format(1.0*nrVarsInGroupConstraints/nrGroupConstraints));
        //pwi.println("Average classes per group constraint,"+sDoubleFormat.format(1.0*nrVarsInGroupConstraints/nrGroupConstraints));
        pwi.println("Avg. number distribution constraints per class,"+sDoubleFormat.format(1.0*nrVarsInGroupConstraints/model.variables().size()));
        pwi.println("Joint enrollment constraints,"+model.getJenrlConstraints().size());
        pw.flush();pw.close();
        pwi.flush();pwi.close();
    }
    
    public static void saveOutputCSV(Solution s,File file) {
        try {
            DecimalFormat dx = new DecimalFormat("000");
            PrintWriter w = new PrintWriter(new FileWriter(file));
            TimetableModel m = (TimetableModel)s.getModel();
            int idx = 1;
            w.println("000."+dx.format(idx++)+" Assigned variables,"+m.assignedVariables().size());
            w.println("000."+dx.format(idx++)+" Time [sec],"+sDoubleFormat.format(s.getBestTime()));
            w.println("000."+dx.format(idx++)+" Hard student conflicts,"+m.getHardStudentConflicts());
            if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                w.println("000."+dx.format(idx++)+" Distance student conf.,"+m.getStudentDistanceConflicts());
            w.println("000."+dx.format(idx++)+" Student conflicts,"+m.getViolatedStudentConflicts());
            w.println("000."+dx.format(idx++)+" Committed student conflicts,"+m.getCommitedStudentConflicts());
            w.println("000."+dx.format(idx++)+" All Student conflicts,"+(m.getViolatedStudentConflicts()+m.getCommitedStudentConflicts()));
            w.println("000."+dx.format(idx++)+" Time preferences,"+sDoubleFormat.format(m.getGlobalTimePreference()));
            w.println("000."+dx.format(idx++)+" Room preferences,"+m.getGlobalRoomPreference());
            w.println("000."+dx.format(idx++)+" Useless half-hours,"+m.getUselessSlots());
            w.println("000."+dx.format(idx++)+" Too big room,"+m.countTooBigRooms());
            w.println("000."+dx.format(idx++)+" Distribution preferences,"+sDoubleFormat.format(m.getGlobalGroupConstraintPreference()));
            if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                w.println("000."+dx.format(idx++)+" Back-to-back instructor pref.,"+m.getInstructorDistancePreference());
            if (m.getProperties().getPropertyBoolean("General.DeptBalancing",true)) {
                w.println("000."+dx.format(idx++)+" Dept. balancing penalty,"+sDoubleFormat.format(m.getDepartmentSpreadPenalty()));
            }
            w.println("000."+dx.format(idx++)+" Same subpart balancing penalty,"+sDoubleFormat.format(m.getSpreadPenalty()));
            if (m.getProperties().getPropertyBoolean("General.MPP",false)) {
                Hashtable mppInfo = ((UniversalPerturbationsCounter)m.getPerturbationsCounter()).getCompactInfo(m, false, false);
                int pidx = 51;
                w.println("000."+dx.format(pidx++)+" Perturbation penalty,"+sDoubleFormat.format(m.getPerturbationsCounter().getPerturbationPenalty(m)));
                w.println("000."+dx.format(pidx++)+" Additional perturbations,"+m.perturbVariables().size());
                int nrPert = 0, nrStudentPert = 0;
                for (Enumeration e=m.variables().elements();e.hasMoreElements();) { 
                    Lecture lecture = (Lecture)e.nextElement();
                    if (lecture.getInitialAssignment()!=null) continue;
                    nrPert++;
                    nrStudentPert+=lecture.classLimit();
                }
                w.println("000."+dx.format(pidx++)+" Given perturbations,"+nrPert);
                w.println("000."+dx.format(pidx++)+" Given student perturbations,"+nrStudentPert);
                for (Enumeration e=ToolBox.sortEnumeration(mppInfo.keys());e.hasMoreElements();) {
                    String key = (String)e.nextElement();
                    Number value = (Number)mppInfo.get(key);
                    w.println("000."+dx.format(pidx++)+" "+key+","+sDoubleFormat.format(value));
                }
            }
            HashSet students = new HashSet();
            int enrls = 0;
            int minRoomPref = 0, maxRoomPref = 0;
            int minGrPref = 0, maxGrPref = 0;
            int minTimePref = 0, maxTimePref = 0;
            int worstInstrPref = 0;
            HashSet used = new HashSet();
            for (Enumeration e1=m.variables().elements();e1.hasMoreElements();) {
                Lecture lecture = (Lecture)e1.nextElement();
                enrls += (lecture.students()==null?0:lecture.students().size());
                students.addAll(lecture.students());
                
                int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
                minRoomPref += minMaxRoomPref[0];
                maxRoomPref += minMaxRoomPref[1];
                
                double[] minMaxTimePref = lecture.getMinMaxTimePreference();
                minTimePref += minMaxTimePref[0];
                maxTimePref += minMaxTimePref[1];
                for (Enumeration g=lecture.constraints().elements();g.hasMoreElements();) {
                    Constraint c = (Constraint)g.nextElement();
                    if (!used.add(c)) continue;
                    
                    if (c instanceof InstructorConstraint) {
                        InstructorConstraint ic = (InstructorConstraint)c;
                        worstInstrPref += ic.getWorstPreference();
                    }
                    
                    if (c instanceof GroupConstraint) {
                        GroupConstraint gc = (GroupConstraint)c;
                        if (gc.isHard()) continue;
                        minGrPref += Math.min(gc.getPreference(),0);
                        maxGrPref += Math.max(gc.getPreference(),0);
                    }
                }
            }
            int totalCommitedPlacements = 0;
            for (Iterator i=students.iterator();i.hasNext();) {
                Student student = (Student)i.next();
                if (student.getCommitedPlacements()!=null)
                    totalCommitedPlacements += student.getCommitedPlacements().size();
            }
            Hashtable subs = new Hashtable();
            for (Enumeration e=m.variables().elements();e.hasMoreElements();) {
                Lecture lecture = (Lecture)e.nextElement();
                if (lecture.isCommitted()) continue;
                Vector vars = (Vector)subs.get(lecture.getScheduler());
                if (vars==null) {
                    vars=new Vector();
                    subs.put(lecture.getScheduler(),vars);
                }
                vars.add(lecture);
            }
            int bidx = 101;
            w.println("000."+dx.format(bidx++)+" Assigned variables max,"+m.variables().size());
            w.println("000."+dx.format(bidx++)+" Student enrollments,"+enrls);
            w.println("000."+dx.format(bidx++)+" Student commited enrollments,"+totalCommitedPlacements);
            w.println("000."+dx.format(bidx++)+" All student enrollments,"+(enrls+totalCommitedPlacements));
            w.println("000."+dx.format(bidx++)+" Time preferences min,"+minTimePref);
            w.println("000."+dx.format(bidx++)+" Time preferences max,"+maxTimePref);
            w.println("000."+dx.format(bidx++)+" Room preferences min,"+minRoomPref);
            w.println("000."+dx.format(bidx++)+" Room preferences max,"+maxRoomPref);
            w.println("000."+dx.format(bidx++)+" Useless half-hours max,"+(Constants.sPreferenceLevelStronglyDiscouraged*m.getRoomConstraints().size()*Constants.SLOTS_PER_DAY_NO_EVENINGS*Constants.NR_DAYS_WEEK));
            w.println("000."+dx.format(bidx++)+" Too big room max,"+(Constants.sPreferenceLevelStronglyDiscouraged*m.variables().size()));
            w.println("000."+dx.format(bidx++)+" Distribution preferences min,"+minGrPref);
            w.println("000."+dx.format(bidx++)+" Distribution preferences max,"+maxGrPref);
            w.println("000."+dx.format(bidx++)+" Back-to-back instructor pref max,"+worstInstrPref);
            for (Enumeration e=ToolBox.sortEnumeration(subs.keys());e.hasMoreElements();) {
                Long scheduler = (Long)e.nextElement();
                Vector vars = (Vector)subs.get(scheduler);
                idx = 001; bidx = 101;
                int nrAssg = 0; enrls = 0;
                int roomPref = 0; minRoomPref = 0; maxRoomPref = 0;
                double timePref = 0; minTimePref = 0; maxTimePref = 0;
                double grPref = 0; minGrPref = 0; maxGrPref = 0;
                long allSC = 0, hardSC = 0, distSC = 0, comSC = 0;
                int instPref = 0; worstInstrPref = 0;
                int spreadPen = 0, deptSpreadPen = 0;
                int tooBigRooms = 0;
                int rcs = 0, uselessSlots = 0, uselessSlotsHH = 0, uselessSlotsBTP = 0;
                used = new HashSet();
                for (Enumeration f=vars.elements();f.hasMoreElements();) {
                    Lecture lecture = (Lecture)f.nextElement();
                    if (lecture.isCommitted()) continue;
                    enrls+= lecture.students().size();
                    Placement placement = (Placement)lecture.getAssignment();
                    if (placement!=null) {
                        nrAssg++;
                        comSC += lecture.getCommitedConflicts(placement);
                    }
                    
                    int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
                    minRoomPref += minMaxRoomPref[0];
                    maxRoomPref += minMaxRoomPref[1];
                    
                    double[] minMaxTimePref = lecture.getMinMaxTimePreference();
                    minTimePref += minMaxTimePref[0];
                    maxTimePref += minMaxTimePref[1];
                    
                    if (placement!=null) {
                        roomPref += placement.getRoomPreference();
                        timePref += placement.getTimeLocation().getNormalizedPreference();
                        tooBigRooms += placement.getTooBigRoomPreference();
                    }
                    
                    for (Enumeration g=lecture.constraints().elements();g.hasMoreElements();) {
                        Constraint c = (Constraint)g.nextElement();
                        if (!used.add(c)) continue;
                        
                        if (c instanceof InstructorConstraint) {
                            InstructorConstraint ic = (InstructorConstraint)c;
                            instPref += ic.getPreference();
                            worstInstrPref += ic.getWorstPreference();
                        }
                        
                        if (c instanceof DepartmentSpreadConstraint) {
                            DepartmentSpreadConstraint dsc = (DepartmentSpreadConstraint)c;
                            deptSpreadPen += dsc.getPenalty();
                        } else if (c instanceof SpreadConstraint) {
                            SpreadConstraint sc = (SpreadConstraint)c;
                            spreadPen += sc.getPenalty();
                        }
                        
                        if (c instanceof GroupConstraint) {
                            GroupConstraint gc = (GroupConstraint)c;
                            if (gc.isHard()) continue;
                            minGrPref += Math.min(gc.getPreference(),0);
                            maxGrPref += Math.max(gc.getPreference(),0);
                            grPref += gc.getCurrentPreference();
                        }
                        
                        if (c instanceof JenrlConstraint) {
                            JenrlConstraint jc = (JenrlConstraint)c;
                            if (!jc.isInConflict() || !jc.isOfTheSameProblem()) continue;
                            Lecture l1 = (Lecture)jc.first();
                            Lecture l2 = (Lecture)jc.second();
                            allSC += jc.getJenrl();
                            if (l1.areStudentConflictsHard(l2))
                                hardSC += jc.getJenrl();
                            Placement p1 = (Placement)l1.getAssignment();
                            Placement p2 = (Placement)l2.getAssignment();
                            if (!p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
                                distSC += jc.getJenrl();
                        }
                        
                        if (c instanceof RoomConstraint) {
                            RoomConstraint rc = (RoomConstraint)c;
                            uselessSlots+=rc.countUselessSlots();
                            uselessSlotsHH+=rc.countUselessSlotsHalfHours();
                            uselessSlotsBTP+=rc.countUselessSlotsBrokenTimePatterns();
                            rcs ++;
                        }
                    }
                }
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Assigned variables,"+nrAssg);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Assigned variables max,"+vars.size());
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Hard student conflicts,"+hardSC);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Student enrollments,"+enrls);
                if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                    w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Distance student conf.,"+distSC);
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Student conflicts,"+allSC);
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Time preferences,"+timePref);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Time preferences min,"+minTimePref);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Time preferences max,"+maxTimePref);
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Room preferences,"+roomPref);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Room preferences min,"+minRoomPref);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Room preferences max,"+maxRoomPref);
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Useless half-hours,"+uselessSlots);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Useless half-hours max,"+(Constants.sPreferenceLevelStronglyDiscouraged*rcs*Constants.SLOTS_PER_DAY_NO_EVENINGS*Constants.NR_DAYS_WEEK));
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Too big room,"+tooBigRooms);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Too big room max,"+(Constants.sPreferenceLevelStronglyDiscouraged*vars.size()));
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Distribution preferences,"+grPref);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Distribution preferences min,"+minGrPref);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Distribution preferences max,"+maxGrPref);
                if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                    w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Back-to-back instructor pref,"+instPref);
                w.println(dx.format(scheduler)+"."+dx.format(bidx++)+" Back-to-back instructor pref max,"+worstInstrPref);
                if (m.getProperties().getPropertyBoolean("General.DeptBalancing",true)) {
                    w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Department balancing penalty,"+sDoubleFormat.format(((double)deptSpreadPen)/12.0));
                }
                w.println(dx.format(scheduler)+"."+dx.format(idx++)+" Same subpart balancing penalty,"+sDoubleFormat.format(((double)spreadPen)/12.0));
            }
            w.flush();w.close();
        } catch (java.io.IOException io) {
            sLogger.error(io.getMessage(),io);
        }
    }
}
