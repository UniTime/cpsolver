package net.sf.cpsolver.ifs.example.tt;


import java.io.*;
import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Test
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
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
public class Test {
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.000", new java.text.DecimalFormatSymbols(Locale.US));
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("dd-MMM-yy_HHmmss", java.util.Locale.US);
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Test.class);
    
    public static void test2(DataProperties properties) throws Exception {
        int nrTests = properties.getPropertyInt("Test.NrTests",1);
        PrintWriter logStat = new PrintWriter(new FileWriter(properties.getProperty("General.Output")+File.separator+"output.csv"));
        PrintWriter logAvgStat = new PrintWriter(new FileWriter(properties.getProperty("General.Output")+File.separator+"avg_stat.csv"));
        logStat.println("fillFact;nrResources;testNr;time[s];iters;speed[it/s];assigned;assigned[%];value;totalValue");
        logAvgStat.println("fillFact;nrResources;time[s];RMStime[s];iters;RMSiters;speed[it/s];assigned;RMSassigned;assigned[%];value;RMSvalue");
        
        int nrResourcesMin = properties.getPropertyInt("Test.NrResourcesMin", -1);
        int nrResourcesMax = properties.getPropertyInt("Test.NrResourcesMax", -1);
        int nrResourcesStep = properties.getPropertyInt("Test.NrResourcesStep", 1);
        double fillFactorMin = properties.getPropertyDouble("Test.FillFactorMin", -1.0);
        double fillFactorMax = properties.getPropertyDouble("Test.FillFactorMax", -1.0);
        double fillFactorStep = properties.getPropertyDouble("Test.FillFactorStep", 0.01);
        
        boolean saveInit = properties.getPropertyBoolean("General.SaveInitialXML", true);
        boolean saveSol = properties.getPropertyBoolean("General.SaveSolutionXML", true);
        
        for (int nrResources=nrResourcesMin;nrResources<=nrResourcesMax;nrResources+=nrResourcesStep) {
            for (double fillFactor=fillFactorMin;fillFactor<=fillFactorMax;fillFactor+=fillFactorStep) {
                double sumTime = 0;
                double sumTime2 = 0;
                int sumIters = 0;
                int sumIters2 = 0;
                int sumAssign = 0;
                int sumAssign2 = 0;
                int sumVal = 0;
                int sumVal2 = 0;
                int sumVar = 0;
                int sumVar2 = 0;
                for (int test=1;test<=nrTests;test++) {
                    if (nrResources>=0) {
                        properties.setProperty("Generator.NrRooms", String.valueOf(nrResources));
                        properties.setProperty("Generator.NrClasses", String.valueOf(nrResources));
                        properties.setProperty("Generator.NrInstructors", String.valueOf(nrResources));
                    }
                    if (fillFactor>=0.0) {
                        properties.setProperty("Generator.FillFactor", String.valueOf(fillFactor));
                    }
                    TimetableModel m = TimetableModel.generate(properties);
                    
                    Solver s = new Solver(properties);
                    if (saveInit) m.saveAsXML(properties, true, null,
                            new File(properties.getProperty("General.Output")+File.separator+"SimpleTT("+
                            (nrResources<0?properties.getPropertyInt("Generator.NrRooms",20):nrResources)+","+
                            ((int)(100.0*(fillFactor<0.0?properties.getPropertyDouble("Generator.FillFactor",0.8):fillFactor)+0.5))+","+
                            properties.getPropertyInt("Generator.NrDependencies",50)+")_"+test+".xml"));
                    s.setInitalSolution(m);
                    s.currentSolution().clearBest();
                    s.start();
                    try {
                        s.getSolverThread().join();
                    } catch (NullPointerException npe) {}
                    
                    
                    
                    if (s.lastSolution().getBestInfo()==null) sLogger.error("No solution found :-(");
                    sLogger.debug("Last solution:"+s.lastSolution().getInfo());
                    Solution best = s.lastSolution();
                    sLogger.debug("Best solution:"+s.lastSolution().getBestInfo());
                    best.restoreBest();
                    int val = 0;
                    for (Enumeration iv = best.getModel().assignedVariables().elements(); iv.hasMoreElements();)
                        val += (int)((Variable)iv.nextElement()).getAssignment().toDouble();
                    if (saveSol) m.saveAsXML(properties, true, best,
                            new File(properties.getProperty("General.Output")+File.separator+"SimpleTT("+
                            (nrResources<0?properties.getPropertyInt("Generator.NrRooms",20):nrResources)+","+
                            ((int)(100.0*(fillFactor<0.0?properties.getPropertyDouble("Generator.FillFactor",0.8):fillFactor)+0.5))+","+
                            properties.getPropertyInt("Generator.NrDependencies",50)+")_"+test+"_sol.xml"));
                    sLogger.debug("Last solution:"+best.getInfo());
                    logStat.println(
                            sDoubleFormat.format(properties.getPropertyDouble("Generator.FillFactor",0.0))+";"+
                            sDoubleFormat.format(properties.getPropertyInt("Generator.NrRooms",0))+";"+
                            test+";"+sDoubleFormat.format(best.getTime())+";"+best.getIteration()+";"+sDoubleFormat.format(((double)best.getIteration())/best.getTime())+";"+best.getModel().assignedVariables().size()+";"+sDoubleFormat.format(100.0 * best.getModel().assignedVariables().size() / best.getModel().variables().size())+";"+val);
                    sLogger.debug("    time:         "+sDoubleFormat.format(best.getTime())+" s");
                    sLogger.debug("    iteration:    "+best.getIteration());
                    sLogger.debug("    speed:        "+sDoubleFormat.format(((double)best.getIteration())/best.getTime())+" it/s");
                    sLogger.debug("    assigned:     "+best.getModel().assignedVariables().size()+" ("+sDoubleFormat.format(100.0 * best.getModel().assignedVariables().size() / best.getModel().variables().size())+"%)");
                    sLogger.debug("    value:        "+val);
                    sumTime += best.getTime();
                    sumTime2 += best.getTime()*best.getTime();
                    sumIters += best.getIteration();
                    sumIters2 += best.getIteration()*best.getIteration();
                    sumAssign += best.getModel().assignedVariables().size();
                    sumAssign2 += best.getModel().assignedVariables().size()*best.getModel().assignedVariables().size();
                    sumVal += val;
                    sumVal2 += val * val;
                    sumVar += m.variables().size();
                    sumVar2 += m.variables().size()*m.variables().size();
                    logStat.flush();
                }
                logAvgStat.println(
                        sDoubleFormat.format(properties.getPropertyDouble("Generator.FillFactor",0.0))+";"+
                        sDoubleFormat.format(properties.getPropertyInt("Generator.NrRooms",0))+";"+
                        sDoubleFormat.format(sumTime/nrTests)+";"+
                        sDoubleFormat.format(ToolBox.rms(nrTests,sumTime,sumTime2))+";"+
                        sDoubleFormat.format(((double)sumIters)/nrTests)+";"+
                        sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumIters,(double)sumIters2))+";"+
                        sDoubleFormat.format(((double)sumIters)/sumTime)+";"+
                        sDoubleFormat.format(((double)sumAssign)/nrTests)+";"+
                        sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumAssign,(double)sumAssign2))+";"+
                        sDoubleFormat.format(100.0 * ((double) sumAssign) / sumVar)+";"+
                        sDoubleFormat.format(((double)sumVal)/nrTests)+";"+
                        sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumVal,(double)sumVal2)));
                logAvgStat.flush();
            }
        }
        logStat.close();
        logAvgStat.close();
    }
    
    public static void test3(DataProperties properties, File xmlFile) throws Exception {
        int nrTests = properties.getPropertyInt("Test.NrTests",1);
        PrintWriter logStat = new PrintWriter(new FileWriter(properties.getProperty("General.Output")+File.separator+"output.csv"));
        logStat.println("fillFact;nrResources;testNr;time[s];iters;speed[it/s];assigned;assigned[%];value;totalValue");
        
        boolean saveSol = properties.getPropertyBoolean("General.SaveSolutionXML", true);
        boolean assign = properties.getPropertyBoolean("General.InitialAssignment", true);
        int forcedPerturbances = properties.getPropertyInt("General.ForcedPerturbances", 0);
        
        for (int test=1;test<=nrTests;test++) {
            TimetableModel m = TimetableModel.loadFromXML(xmlFile, assign);
            
            if (forcedPerturbances>0) {
                Vector initialVariables = new Vector();
                for (Enumeration e=m.variables().elements();e.hasMoreElements();) {
                    Variable v = (Variable)e.nextElement();
                    if (v.getInitialAssignment()!=null)
                        initialVariables.addElement(v);
                }
                for (int i=0;i<forcedPerturbances;i++) {
                    if (initialVariables.isEmpty()) break;
                    Variable var = (Variable)ToolBox.random(initialVariables);
                    initialVariables.remove(var);
                    var.removeInitialValue();
                }
            }
            
            Solver s = new Solver(properties);
            s.setInitalSolution(m);
            s.currentSolution().clearBest();
            s.start();
            try {
                s.getSolverThread().join();
            } catch (NullPointerException npe) {}
            
            if (s.lastSolution().getBestInfo()==null) sLogger.error("No solution found :-(");
            sLogger.debug("Last solution:"+s.lastSolution().getInfo());
            Solution best = s.lastSolution();
            sLogger.debug("Best solution:"+s.lastSolution().getBestInfo());
            best.restoreBest();
            int val = 0;
            for (Enumeration iv = best.getModel().assignedVariables().elements(); iv.hasMoreElements();)
                val += (int)((Variable)iv.nextElement()).getAssignment().toDouble();
            if (saveSol) m.saveAsXML(properties, false, best, new File(properties.getProperty("General.Output")+File.separator+"solution_"+test+".xml"));
            sLogger.debug("Last solution:"+best.getInfo());
            logStat.println(
                    sDoubleFormat.format(properties.getPropertyDouble("Generator.FillFactor",0.0))+";"+
                    sDoubleFormat.format(properties.getPropertyInt("Generator.NrRooms",0))+";"+
                    test+";"+sDoubleFormat.format(best.getTime())+";"+best.getIteration()+";"+sDoubleFormat.format(((double)best.getIteration())/best.getTime())+";"+best.getModel().assignedVariables().size()+";"+sDoubleFormat.format(100.0 * best.getModel().assignedVariables().size() / best.getModel().variables().size())+";"+val);
            sLogger.debug("    time:         "+sDoubleFormat.format(best.getTime())+" s");
            sLogger.debug("    iteration:    "+best.getIteration());
            sLogger.debug("    speed:        "+sDoubleFormat.format(((double)best.getIteration())/best.getTime())+" it/s");
            sLogger.debug("    assigned:     "+best.getModel().assignedVariables().size()+" ("+sDoubleFormat.format(100.0 * best.getModel().assignedVariables().size() / best.getModel().variables().size())+"%)");
            sLogger.debug("    value:        "+val);
            logStat.flush();
        }
        logStat.close();
    }
    
    public static void test(File inputCfg, String name, String include, String regexp, String outDir) throws Exception {
        if (regexp != null) {
            String incFile;
            
            if (regexp.indexOf(';') > 0) {
                incFile = regexp.substring(0, regexp.indexOf(';'));
                regexp = regexp.substring(regexp.indexOf(';') + 1);
            } else {
                incFile = regexp;
                regexp = null;
            }
            if (incFile.startsWith("[") && incFile.endsWith("]")) {
                test(inputCfg, name, include, regexp, outDir);
                incFile = incFile.substring(1, incFile.length() - 1);
            }
            if (incFile.indexOf('{') >= 0 && incFile.indexOf('}') >= 0) {
                String prefix = incFile.substring(0, incFile.indexOf('{'));
                StringTokenizer middle = new StringTokenizer(incFile.substring(incFile.indexOf('{') + 1, incFile.indexOf('}')), "|");
                String sufix = incFile.substring(incFile.indexOf('}') + 1);
                
                while (middle.hasMoreTokens()) {
                    String m = middle.nextToken();
                    
                    test(inputCfg, (name == null ? "" : name + "_") + m, (include == null ? "" : include + ";") + prefix + m + sufix, regexp, outDir);
                }
            } else {
                test(inputCfg, name, (include == null ? "" : include + ";") + incFile, regexp, outDir);
            }
        } else {
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            StringTokenizer inc = new StringTokenizer(include, ";");
            
            while (inc.hasMoreTokens()) {
                String aFile = inc.nextToken();
                
                System.out.println("  Loading included file '" + aFile + "' ... ");
                FileInputStream is = null;
                
                if ((new File(aFile)).exists()) {
                    is = new FileInputStream(aFile);
                }
                if ((new File(inputCfg.getParent() + File.separator + aFile)).exists()) {
                    is = new FileInputStream(inputCfg.getParent() + File.separator + aFile);
                }
                if (is == null) {
                    System.err.println("Unable to find include file '" + aFile + "'.");
                }
                properties.load(is);
                is.close();
            }
            String outDirThisTest = (outDir==null?properties.getProperty("General.Output","."):outDir)+File.separator + name+File.separator+sDateFormat.format(new Date());
            properties.setProperty("General.Output", outDirThisTest.toString());
            System.out.println("Output folder: "+properties.getProperty("General.Output"));
            (new File(outDirThisTest)).mkdirs();
            ToolBox.configureLogging(outDirThisTest, null);
            FileOutputStream fos = new FileOutputStream(outDirThisTest + File.separator + "rcsp.conf");
            
            properties.store(fos, "Random CSP problem configuration file");
            fos.flush(); fos.close();
            test2(properties);
        }
    }
    
    public static void main(String[] args) {
        try {
            Progress.getInstance().addProgressListener(new ProgressWriter(System.out));
            File inputCfg = new File(args[0]);
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            if (args.length==3) {
                File xmlFile = new File(args[1]);
                String outDir = args[2]+File.separator+(sDateFormat.format(new Date()));
                properties.setProperty("General.Output", outDir.toString());
                System.out.println("Input file: "+xmlFile);
                System.out.println("Output folder: "+properties.getProperty("General.Output"));
                (new File(outDir)).mkdirs();
                ToolBox.configureLogging(outDir, null);
                test3(properties, xmlFile);
            } else if (properties.getProperty("INCLUDE_REGEXP") != null) {
                test(inputCfg, null, null, properties.getProperty("INCLUDE_REGEXP"), (args.length>1?args[1]:null));
            } else {
                String outDir = properties.getProperty("General.Output", ".") + File.separator + inputCfg.getName().substring(0, inputCfg.getName().lastIndexOf('.')) + File.separator + sDateFormat.format(new Date());
                if (args.length>1)
                    outDir = args[1]+File.separator+(sDateFormat.format(new Date()));
                properties.setProperty("General.Output", outDir.toString());
                System.out.println("Output folder: "+properties.getProperty("General.Output"));
                (new File(outDir)).mkdirs();
                ToolBox.configureLogging(outDir, null);
                test2(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
