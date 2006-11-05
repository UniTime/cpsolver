package net.sf.cpsolver.ifs.extension;


import java.io.*;
import java.util.*;

import net.sf.cpsolver.ifs.heuristics.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Conflict-based statistics.
 * <br><br>
 * The idea behind it is to memorize conflicts and to avoid their potential repetition. When a value v0 is assigned to a 
 * variable V0, hard conflicts with previously assigned variables (e.g., V1 = v1, V2 = v2, ... Vm = vm) may occur. 
 * These variables V1,...,Vm have to be unassigned before the value v0 is assigned to the variable V0. These unassignments, 
 * together with the reason for their unassignment (i.e., the assignment V0 = v0), and a counter tracking how many times 
 * such an event occurred in the past, is stored in memory.
 * <br><br>
 * Later, if a variable is selected for assignment again, the stored information about repetition of past hard conflicts
 * can be taken into account, e.g., in the value selection heuristics. Assume that the variable V0 is selected for an
 * assignment again (e.g., because it became unassigned as a result of a later assignment), we can weight the number of
 * hard conflicts created in the past for each possible value of this variable. In the above example, the existing
 * assignment V1 = v1 can prohibit the selection of value v0 for variable V0 if there is again a conflict with the
 * assignment V1 = v1.
 * <br><br>
 * Conflict-based statistics are a data structure which memorizes the number of hard conflicts that have occurred 
 * during the search (e.g., that assignment V0 = v0 resulted c1 times in an unassignment of V1 = v1, c2 times of 
 * V2 = v2, . . . and cm times of Vm = vm). More precisely, they form an array
 * <ul>
 * CBS[Va = va, Vb != vb] = cab,
 * </ul>
 * stating that the assignment Va = va caused the unassignment of Vb = vb a total of cab times in the past. Note that 
 * in case of n-ary constraints (where n > 2), this does not imply that the assignments Va = va and Vb = vb cannot be used
 * together. The proposed conflict-based statistics do not actually work with any constraint, they only memorize 
 * unassignments and the assignment that caused them. Let us consider a variable Va selected by the 
 * {@link VariableSelection#selectVariable(Solution)} function and a value va selected by 
 * {@link ValueSelection#selectValue(Solution, Variable)}. Once the assignment Vb = vb is selected by 
 * {@link Model#conflictValues(Value)} to be unassigned, the array cell CBS[Va = va, Vb != vb] is incremented by one.
 * <br><br>
 * The data structure is implemented as a hash table, storing information for conflict-based statistics. A counter is 
 * maintained for the tuple A = a and B != b. This counter is increased when the value a is assigned to the variable A 
 * and b is unassigned from B. The example of this structure
 * <ul>
 * A = a &nbsp;&nbsp;&nbsp; &#8594; &nbsp;&nbsp;&nbsp; 3 x B != b, &nbsp; 4 x B != c, &nbsp; 2 x C != a, &nbsp; 120 x D != a
 * </ul>
 * expresses that variable B lost its assignment b three times and its assignment c four times, variable C lost its 
 * assignment a two times, and D lost its assignment a 120 times, all because of later assignments of value a to 
 * variable A. This structure is being used in the value selection heuristics to evaluate existing conflicts with
 * the assigned variables. For example, if there is a variable A selected and if the value a is in conflict with the 
 * assignment B = b, we know that a similar problem has already occurred 3x in the past, and hence the conflict A = a is 
 * weighted with the number 3.
 * <br><br>
 * Then, a min-conflict value selection criterion, which selects a value with the minimal number of conflicts with the 
 * existing assignments, can be easily adapted to a weighted min-conflict criterion. The value with the smallest sum of the
 * number of conflicts multiplied by their frequencies is selected. Stated in another way, the weighted min-conflict 
 * approach helps the value selection heuristics to select a value that might cause more conflicts than another
 * value, but these conflicts occurred less frequently, and therefore they have a lower weighted sum.
 * <br><br>
 * The conflict-based statistics has also implemented the following extensions: <ul>
 * <li> If a variable is selected for an assignment, the above presented structure can also tell how many potential
 * conflicts a value can cause in the future. In the above example, we already know that four times a later assignment
 * of A=a caused that value c was unassigned from B. We can try to minimize such future conflicts by selecting
 * a different value of the variable B while A is still unbound.
 * <li> The memorized conflicts can be aged according to how far they have occurred in the past. For example, a conflict
 * which occurred 1000 iterations ago can have half the weight of a conflict which occurred during the last iteration
 * or it can be forgotten at all.
 * </ul>
 * Furthermore, the presented conflict-based statistics can be used not only inside the solving mechanism. The 
 * constructed "implications" together with the information about frequency of their occurrences can be easily accessed 
 * by users or by some add-on deductive engine to identify inconsistencies1 and/or hard parts of the input problem. 
 * The user can then modify the input requirements in order to eliminate problems found and let the solver continue the 
 * search with this modified input problem.
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>ConflictStatistics.Ageing</td><td>{@link Double}</td><td>Ageing of the conflict-based statistics. Every memorized conflict is aged (multiplited) by this factor for every iteration which passed from the time it was memorized. For instance, if there was a conflict 10 iterations ago, its value is ageing^10 (default is 1.0 -- no ageing).</td></tr>
 * <tr><td>ConflictStatistics.AgeingHalfTime</td><td>{@link Integer}</td><td>Another way how to express ageing: number of iterations to decrease a conflict to 1/2 (default is 0 -- no ageing)</td></tr>
 * </table>
 * <br>
 * Conflict-based statistics also allows printing to HTML files during the search, after each given number of iterations.
 * Example of such file:<br>
 * <iframe src="cbs-ex.html" width="98%" height="300" scrolling="auto" frameborder="1">
 * [Your user agent does not support frames or is currently configured  not to display frames. However, you may visit <A href="cbs-ex.html">the related document.</A>]
 * </iframe>
 * <br><br>
 * Conflict-based statistics allows two modes of printing: <ul>
 * <li>variable based: tree selected variable &#8594; selected value &#8594; constraint &#8594; conflicting assignment is printed
 * <li>constraint based: tree constraint &#8594; selected variable &#8594; selected value &#8594; conflicting assignment is printed
 * </ul>
 * Where constraint is the constraint involved in the unassignment of the conflicting assignmend when selected value is assigned to the selected variable.
 * HTML files are written in the output directory, named stat1.html, stat2.html, ...
 * <br><br>
 * Printing parameters:
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>ConflictStatistics.Print</td><td>{@link Boolean}</td><td>If true, conflict-based statistics is being printed to an HTML file during the search.</td></tr>
 * <tr><td>ConflictStatistics.PrintInterval</td><td>{@link Integer}</td><td>Interval (expressed in the number of iterations) for printing CBS</td></tr>
 * <tr><td>ConflictStatistics.Type</td><td>{@link Integer}</td><td>0 for variable based, 1 from constraint based</td></tr>
 * <tr><td>ConflictStatistics.MaxLines</td><td>{@link Integer}</td><td>Maximal number of lines in the first level of CBS</td></tr>
 * <tr><td>ConflictStatistics.MaxBranchingLev1</td><td>{@link Integer}</td><td>Maximal number of lines in the second level of CBS</td></tr>
 * <tr><td>ConflictStatistics.MaxBranchingLev2</td><td>{@link Integer}</td><td>Maximal number of lines in the third level of CBS</td></tr>
 * <tr><td>ConflictStatistics.ImageBase</td><td>{@link String}</td><td>Directory with images collapse.gif, expand.gif and end.gif relative to output directory</td></tr>
 * </table>
 *
 * @see Solver
 * @see Model
 * @see ValueSelection
 * @see VariableSelection
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
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
public class ConflictStatistics extends Extension implements ConstraintListener, SolutionListener {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(ConflictStatistics.class);
    private static final String PARAM_AGEING = "ConflictStatistics.Ageing";
    private static final String PARAM_HALF_AGE = "ConflictStatistics.AgeingHalfTime";
    private static final String PARAM_PRINT = "ConflictStatistics.Print";
    private static final String PARAM_PRINTINTERVAL = "ConflictStatistics.PrintInterval";
    
    private static final int TYPE_VARIABLE_BASED = 0;
    private static final int TYPE_CONSTRAINT_BASED = 1;
    
    private double iAgeing = 1.0;
    private long iPrintInterval = -1;
    private boolean iPrint = false;
    private int iPrintCounter = 0;
    
    private Hashtable iAssignments = new Hashtable();
    private Hashtable iUnassignedVariables = new Hashtable();
    private Hashtable iNoGoods = new Hashtable();
    
    public ConflictStatistics(Solver solver, DataProperties properties) {
        super(solver, properties);
        iAgeing = properties.getPropertyDouble(PARAM_AGEING, iAgeing);
        int halfAge = properties.getPropertyInt(PARAM_HALF_AGE, 0);
        if (halfAge > 0) iAgeing = Math.exp(Math.log(0.5) / ((double)halfAge));
        iPrint = properties.getPropertyBoolean(PARAM_PRINT, iPrint);
        iPrintInterval = properties.getPropertyLong(PARAM_PRINTINTERVAL, iPrintInterval);
    }
    
    public void register(Model model) {
        super.register(model);
        if (iPrint) {
            getSolver().currentSolution().addSolutionListener(this);
        }
    }
    
    public void unregister(Model model) {
        super.unregister(model);
        if (iPrint) {
            getSolver().currentSolution().removeSolutionListener(this);
        }
    }
    
    private void variableUnassigned( long iteration, Value unassignedValue, AssignmentSet noGoods) {
        Assignment unass = new Assignment(iteration, unassignedValue, iAgeing);
        Vector noGoodsForUnassignment = (Vector)iNoGoods.get(unass);
        if (noGoodsForUnassignment != null) {
            if (noGoodsForUnassignment.contains(noGoods)) {
                ((AssignmentSet)noGoodsForUnassignment.elementAt(noGoodsForUnassignment.indexOf(noGoods))).incCounter();
            } else {
                noGoodsForUnassignment.addElement(noGoods);
            }
        } else {
            noGoodsForUnassignment = new FastVector();
            noGoodsForUnassignment.addElement(noGoods);
            iNoGoods.put(unass, noGoodsForUnassignment);
        }
    }
    
    public void reset() {
        iUnassignedVariables.clear();
        iAssignments.clear();
    }
    
    private void variableUnassigned(long iteration, Value unassignedValue, Value assignedValue) {
        Assignment ass = new Assignment(iteration, assignedValue, iAgeing);
        Assignment unass = new Assignment(iteration, unassignedValue, iAgeing);
        if (iAssignments.containsKey(unass)) {
            Vector asss = (Vector)iAssignments.get(unass);
            if (asss.contains(ass)) {
                ((Assignment)asss.elementAt(asss.indexOf(ass))).incCounter(iteration);
            } else {
                asss.addElement(ass);
            }
        } else {
            Vector asss = new FastVector();
            asss.addElement(ass);
            iAssignments.put(unass, asss);
        }
        if (iUnassignedVariables.containsKey(unassignedValue.variable())) {
            Vector asss = (Vector)iUnassignedVariables.get(unassignedValue.variable());
            if (asss.contains(ass)) {
                ((Assignment)asss.elementAt(asss.indexOf(ass))).incCounter( iteration);
            } else {
                asss.addElement(ass);
            }
        }
        else {
            Vector asss = new FastVector();
            asss.addElement(ass);
            iUnassignedVariables.put(unassignedValue.variable(), asss);
        }
    }
    
    /** Counts number of unassignments of the given conflicting values caused by the assignment
     * of the given value.
     */
    public double countRemovals(long iteration, Collection conflictValues, Value value) {
        long ret = 0;
        for (Iterator i = conflictValues.iterator(); i.hasNext();) {
            Value conflictValue = (Value)i.next();
            ret += countRemovals(iteration, conflictValue, value);
            // tady bylo +1
        }
        return ret;
    }
    
    /** Counts number of unassignments of the given conflicting value caused by the assignment
     * of the given value.
     */
    public double countRemovals(long iteration, Value conflictValue, Value value) {
        Vector asss = (Vector)iUnassignedVariables.get(conflictValue.variable());
        if (asss == null)
            return 0;
        Assignment ass = new Assignment(iteration, value, iAgeing);
        int idx = asss.indexOf(ass);
        if (idx < 0)
            return 0;
        return ((Assignment)asss.elementAt(idx)).getCounter(iteration);
    }
    
    /** Counts potential number of unassignments of if the given value is selected.
     */
    public long countPotentialConflicts(long iteration, Value value, int limit) {
        Vector asss = (Vector)iAssignments.get(new Assignment(iteration, value, iAgeing));
        if (asss == null) return 0;
        long count = 0;
        for (Enumeration i = asss.elements(); i.hasMoreElements();) {
            Assignment ass = (Assignment)i.nextElement();
            if (ass.getValue().variable().getAssignment() == null) {
                if (limit >= 0) {
                    count += ass.getCounter(iteration) * Math.max(0,1+limit - value.variable().getModel().conflictValues(ass.getValue()).size());
                }
                else {
                    count += ass.getCounter(iteration);
                }
            }
        }
        return count;
    }
    
    private void menu_item(PrintWriter out, String imgBase, String id, String name, String title, String page, boolean isCollapsed) {
        out.println("<div style=\"margin-left:5px;\">");
        out.println("<A style=\"border:0;background:0\" id=\"__idMenu"+id+"\" href=\"javascript:toggle('"+id+"')\" name=\""+name+"\" title=\"Expand "+name+"\">");
        out.println("<img id=\"__idMenuImg"+id+"\" border=\"0\" src=\""+(imgBase == null ? "img/" : imgBase)+(isCollapsed ? "expand" : "collapse")+".gif\" align=\"absmiddle\"></A>");
        out.println("&nbsp;<A target=\"__idContentFrame\" "+(page == null ? "" : "href=\""+page+"\" ")+"title=\""+(title == null ? "" : title)+"\" >"+ name+(title == null?"":" <font color='gray'>[" + title + "]</font>")+"</A><br>");
        out.println("</div>");
        out.println("<div ID=\"__idMenuDiv"+id+"\" style=\"display:"+(isCollapsed ? "none" : "block")+";position:relative;margin-left:18px;\">");
    }
    
    private void leaf_item(PrintWriter out, String imgBase, String name, String title,String page) {
        out.println("<div style=\"margin-left:5px;\">");
        out.println("<img border=\"0\" src=\""+(imgBase == null ? "img/" : imgBase)+"end.gif\" align=\"absmiddle\">");
        out.println("&nbsp;<A target=\"__idContentFrame\" "+(page == null ? "" : "href=\"" + page + "\" ")+"title=\""+(title == null ? "" : title)+"\" >"+name+(title == null ? "" : " <font color='gray'>[" + title + "]</font>")+"</A><br>");
        out.println("</div>");
    }
    
    private void end_item(PrintWriter out) {
        out.println("</div>");
    }
    
    private void unassignedVariableMenuItem(PrintWriter out, String imgBase, String menuId, long counter, Variable variable) {
        menu_item(out, imgBase, menuId, counter + "x " + variable.getName(), variable.getDescription(), null, true);
    }
    
    private void unassignmentMenuItem(PrintWriter out, String imgBase, String menuId, double counter, Assignment unassignment) {
        menu_item(out, imgBase, menuId, Math.round(counter) + "x " + unassignment.getValue().getName(), unassignment.getValue().getDescription(), null, true);
    }
    
    private void constraintMenuItem(PrintWriter out, String imgBase, String menuId, long counter, Constraint constraint) {
        String name = (constraint == null ? null : constraint.getClass().getName().substring( constraint.getClass().getName().lastIndexOf('.') + 1) + (constraint.getName() == null ? "" : " " + constraint.getName()));
        menu_item(out, imgBase, menuId, counter + "x " + name, (constraint == null ? null : constraint.getDescription()), null, true);
    }
    
    private void assignmentsMenuItem(PrintWriter out, String imgBase, String menuId, AssignmentSet set) {
        StringBuffer names = new StringBuffer();
        for (Enumeration e = set.getSet().elements(); e.hasMoreElements();) {
            Assignment a = (Assignment)e.nextElement();
            names.append(names.length() == 0 ? "" : ", ").append(a.getValue().variable().getName());
        }
        menu_item(out, imgBase, menuId, set.getCounter() + "x [" + names + "]", null, null, true);
    }
    
    private void assignmentLeafItem(PrintWriter out, String imgBase, Assignment assignment) {
        leaf_item(out, imgBase, assignment.getValue().variable().getName()+" := "+assignment.getValue().getName(), null, null);
    }
    
    private void assignmentLeafItem(PrintWriter out, String imgBase, long counter,Assignment assignment) {
        leaf_item(out, imgBase, counter+"x "+assignment.getValue().variable().getName()+" := "+assignment.getValue().getName(), null, null);
    }
    
    /** Print conflict-based statistics in HTML format */
    public void printHtml(long iteration, PrintWriter out, long maxVariables, long unassignmentLimit, long assignmentLimit, int type) {
        printHtml(iteration, out, true, maxVariables, unassignmentLimit, assignmentLimit, null, type);
    }
    
    /** Print conflict-based statistics in HTML format */
    public void printHtml(long iteration, PrintWriter out, DataProperties params) {
        printHtml(iteration,out,
        params.getPropertyBoolean("ConflictStatistics.PringHeader", false),
        params.getPropertyInt("ConflictStatistics.MaxLines", 25),
        params.getPropertyInt("ConflictStatistics.MaxBranchingLev1", 100),
        params.getPropertyInt("ConflictStatistics.MaxBranchingLev2", 10),
        params.getProperty("ConflictStatistics.ImageBase", null),
        params.getPropertyInt("ConflictStatistics.Type",TYPE_VARIABLE_BASED));
    }
    
    /** Print conflict-based statistics in HTML format */
    public void printHtml(long iteration, PrintWriter out, boolean printHeader, long maxVariables, long unassignmentLimit, long assignmentLimit, String imgBase, int type) {
        if (printHeader) {
            out.println("<html><head>");
            out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
            out.println();
            out.println("<style type=\"text/css\">");
            out.println("<!--");
            out.println("A:link     { color: blue; text-decoration: none; border:0; background:0; }");
            out.println("A:visited  { color: blue; text-decoration: none; border:0; background:0; }");
            out.println("A:active   { color: blue; text-decoration: none; border:0; background:0; }");
            out.println("A:hover    { color: blue; text-decoration: none; border:0; background:0; }");
            out.println(".TextBody  { background-color: white; color:black; font-size: 12px; }");
            out.println(".WelcomeHead { color: black; margin-top: 0px; margin-left: 0px; font-weight: bold; text-align: right; font-size: 30px; font-family: Comic Sans MS}");
            out.println("-->");
            out.println("</style>");
            out.println();
            out.println("<script language=\"javascript\" type=\"text/javascript\">");
            out.println("function toggle(item) {");
            out.println("	obj=document.getElementById(\"__idMenuDiv\"+item);");
            out.println("	visible=(obj.style.display!=\"none\");");
            out.println("	img=document.getElementById(\"__idMenuImg\" + item);");
            out.println("	menu=document.getElementById(\"__idMenu\" + item);");
            out.println("	if (visible) {obj.style.display=\"none\";img.src=\""+(imgBase == null ? "img/" : imgBase)+"expand.gif\";menu.title='Expand '+menu.name;}");
            out.println("	else {obj.style.display=\"block\";img.src=\""+(imgBase == null ? "img/" : imgBase)+"collapse.gif\";menu.title='Collapse '+menu.name;}");
            out.println("}");
            out.println("</script>");
            out.println();
            out.println("</head>");
            out.println("<body class=\"TextBody\">");
            out.println("<br><table border='0' width='100%'>");
            out.println("<tr><td width=12>&nbsp;</td><td bgcolor='#BBCDE4' align='right'>");
            out.println("<div class=WelcomeHead>Conflict Statistics&nbsp;</div></td></tr></table><br>");
            out.println("<ul>");
        }
        if (type == TYPE_VARIABLE_BASED) {
            Hashtable variables = new Hashtable();
            Hashtable variable2Assignments = new Hashtable();
            for (Enumeration e1 = iNoGoods.keys(); e1.hasMoreElements();) {
                Assignment ass = (Assignment)e1.nextElement();
                long cnt = 0;
                for (Enumeration e2 = ((Vector)iNoGoods.get(ass)).elements();e2.hasMoreElements();)
                    cnt += ((AssignmentSet)e2.nextElement()).getCounter();
                ass.setCounter(cnt);
                cnt += (variables.containsKey(ass.getValue().variable())?((Long)variables.get(ass.getValue().variable())).longValue():0L);
                variables.put(ass.getValue().variable(), new Long(cnt));
                Vector assignments = (Vector)variable2Assignments.get(ass.getValue().variable());
                if (assignments == null) {
                    assignments = new FastVector();
                    variable2Assignments.put(ass.getValue().variable(),assignments);
                }
                assignments.addElement(ass);
            }
            int varCounter = 0;
            for (Enumeration e1 = ToolBox.sortEnumeration(variables.keys(),new VariableComparator(variables));e1.hasMoreElements();) {
                Variable variable = (Variable)e1.nextElement();
                varCounter++;
                if (varCounter > maxVariables)
                    break;
                long varCnt = ((Long)variables.get(variable)).longValue();
                unassignedVariableMenuItem(out,imgBase,String.valueOf(variable.getId()),varCnt,variable);
                for (Enumeration e2 = ToolBox.sortEnumeration(((Vector)variable2Assignments.get(variable)).elements(),Assignment.getComparator(iteration));e2.hasMoreElements();) {
                    Assignment ass = (Assignment)e2.nextElement();
                    if (!ass.getValue().variable().equals(variable))
                        continue;
                    double cntUnas = 0.0;
                    for (Enumeration e3 = ((Vector)iNoGoods.get(ass)).elements();e3.hasMoreElements();)
                        cntUnas += ((AssignmentSet)e3.nextElement()).getCounter();
                    if (Math.round(cntUnas) < unassignmentLimit) continue;
                    unassignmentMenuItem(out,imgBase,ass.getValue().variable().getId()+"."+ass.getValue().getId(),cntUnas,ass);
                    int id = 0;
                    Hashtable constr2counter = new Hashtable();
                    Hashtable constr2assignments = new Hashtable();
                    for (Enumeration e3 = ((Vector)iNoGoods.get(ass)).elements();e3.hasMoreElements();) {
                        AssignmentSet x = (AssignmentSet)e3.nextElement();
                        if (x.getConstraint() == null) continue;
                        Long cnter = (Long)constr2counter.get(x.getConstraint());
                        if (cnter == null)
                            constr2counter.put(x.getConstraint(), new Long(x.getCounter()));
                        else
                            constr2counter.put(x.getConstraint(), new Long(x.getCounter() + cnter.longValue()));
                        Vector aaa = (Vector)constr2assignments.get(x.getConstraint());
                        if (aaa == null) {
                            aaa = new FastVector();
                            constr2assignments.put(x.getConstraint(), aaa);
                        }
                        aaa.addElement(x);
                    }
                    for (Enumeration e3 = ToolBox.sortEnumeration(constr2counter.keys(),new ConstraintComparator(constr2counter));e3.hasMoreElements();) {
                        Constraint constraint = (Constraint)e3.nextElement();
                        Long cnter = (Long)constr2counter.get(constraint);
                        constraintMenuItem(out,imgBase,ass.getValue().variable().getId()+"."+ass.getValue().getId()+"."+constraint.getId(),cnter.longValue(),constraint);
                        if (cnter.longValue() >= assignmentLimit) {
                            for (Enumeration e4 = ((Vector)constr2assignments.get(constraint)).elements();e4.hasMoreElements();) {
                                AssignmentSet x = (AssignmentSet)e4.nextElement();
                                boolean printAssignmentsMenuItem = (x.getSet().size() > 2);
                                if (printAssignmentsMenuItem)
                                    assignmentsMenuItem(out,imgBase,ass.getValue().variable().getId()+"."+ass.getValue().getId()+"."+constraint.getId()+"."+(++id),x);
                                //menu_item(out, imgBase, ass.getValue().variable().getId()+"."+ass.getValue().getId()+"."+(++id), x.getCounter()+"x "+(x.getName()==null?null:x.getName()), x.getDescription(), null, true);
                                for (Enumeration e5 = ToolBox.sortEnumeration(x.getSet().elements(),Assignment.getComparator(iteration));e5.hasMoreElements();) {
                                    Assignment a = (Assignment)e5.nextElement();
                                    if (!ass.equals(a)) {
                                        if (printAssignmentsMenuItem)
                                            assignmentLeafItem(out, imgBase, a);
                                        else
                                            assignmentLeafItem(out, imgBase, x.getCounter(), a);
                                    }
                                }
                                if (printAssignmentsMenuItem)
                                    end_item(out);
                            }
                        }
                        end_item(out);
                    }
                    end_item(out);
                }
                end_item(out);
            }
        }
        else
            if (type == TYPE_CONSTRAINT_BASED) {
                Hashtable constraint2assignments = new Hashtable();
                Hashtable constraint2counter = new Hashtable();
                for (Enumeration e1 = iNoGoods.keys(); e1.hasMoreElements();) {
                    Assignment ass = (Assignment)e1.nextElement();
                    for (Enumeration e2 = ((Vector)iNoGoods.get(ass)).elements();e2.hasMoreElements();) {
                        AssignmentSet set = (AssignmentSet)e2.nextElement();
                        if (set.getConstraint() == null) continue;
                        Hashtable assignments = (Hashtable)constraint2assignments.get(set.getConstraint());
                        if (assignments == null) {
                            assignments = new Hashtable();
                            constraint2assignments.put(set.getConstraint(),assignments);
                        }
                        Vector unassignments = (Vector)assignments.get(ass);
                        if (unassignments == null) {
                            unassignments = new FastVector();
                            assignments.put(ass, unassignments);
                        }
                        unassignments.addElement(set);
                        Long cnt = (Long)constraint2counter.get(set.getConstraint());
                        constraint2counter.put(set.getConstraint(),new Long(set.getCounter()+(cnt == null ? 0 : cnt.longValue())));
                    }
                }
                int constrCounter = 0;
                for (Enumeration e1 = ToolBox.sortEnumeration(constraint2counter.keys(), new ConstraintComparator(constraint2counter)); e1.hasMoreElements();) {
                    Constraint constraint = (Constraint)e1.nextElement();
                    constrCounter++;
                    if (constrCounter > maxVariables)
                        break;
                    Long constrCnt = (Long)constraint2counter.get(constraint);
                    Hashtable constrAssignments = (Hashtable)constraint2assignments.get(constraint);
                    constraintMenuItem(out, imgBase, String.valueOf(constraint.getId()), constrCnt.longValue(), constraint);
                    
                    Hashtable variables = new Hashtable();
                    Hashtable variable2Assignments = new Hashtable();
                    for (Enumeration e2 = constrAssignments.keys(); e2.hasMoreElements(); ) {
                        Assignment ass = (Assignment)e2.nextElement();
                        long cnt = 0;
                        for (Enumeration e3 = ((Vector)constrAssignments.get(ass)).elements(); e3.hasMoreElements();)
                            cnt += ((AssignmentSet)e3.nextElement()).getCounter();
                        ass.setCounter(cnt);
                        cnt += (variables.containsKey(ass.getValue().variable()) ? ((Long)variables.get(ass.getValue().variable())).longValue() : 0L);
                        variables.put(ass.getValue().variable(), new Long(cnt));
                        Vector assignments = (Vector)variable2Assignments.get(ass.getValue().variable());
                        if (assignments == null) {
                            assignments = new FastVector();
                            variable2Assignments.put(ass.getValue().variable(),assignments);
                        }
                        assignments.addElement(ass);
                    }
                    
                    for (Enumeration e2 = ToolBox.sortEnumeration(variables.keys(),new VariableComparator(variables));e2.hasMoreElements();) {
                        Variable variable = (Variable)e2.nextElement();
                        long varCnt = ((Long)variables.get(variable)).longValue();
                        if (varCnt < unassignmentLimit)
                            continue;
                        unassignedVariableMenuItem(out,imgBase,constraint.getId() + "." + variable.getId(),varCnt,variable);
                        
                        for (Enumeration e3 = ToolBox.sortEnumeration(((Vector)variable2Assignments.get(variable)).elements(),Assignment.getComparator(iteration));e3.hasMoreElements();) {
                            Assignment ass = (Assignment)e3.nextElement();
                            if (!ass.getValue().variable().equals(variable))
                                continue;
                            double cntUnas = 0.0;
                            for (Enumeration e4 = ((Vector)iNoGoods.get(ass)).elements();e4.hasMoreElements();)
                                cntUnas += ((AssignmentSet)e4.nextElement()).getCounter();
                            if (Math.round(cntUnas) < assignmentLimit)
                                continue;
                            unassignmentMenuItem(out, imgBase, constraint.getId()+"."+ass.getValue().variable().getId()+"."+ass.getValue().getId(),cntUnas,ass);
                            
                            int id = 0;
                            for (Enumeration e4 = ((Vector)constrAssignments.get(ass)).elements();e4.hasMoreElements();) {
                                AssignmentSet x = (AssignmentSet)e4.nextElement();
                                boolean printAssignmentsMenuItem = (x.getSet().size() > 2);
                                if (printAssignmentsMenuItem)
                                    assignmentsMenuItem(out, imgBase, constraint.getId()+"."+ass.getValue().variable().getId()+"."+ass.getValue().getId()+"."+(++id), x);
                                for (Enumeration e5 = ToolBox.sortEnumeration(x.getSet().elements(), Assignment.getComparator(iteration)); e5.hasMoreElements();) {
                                    Assignment a = (Assignment)e5.nextElement();
                                    if (!ass.equals(a)) {
                                        if (printAssignmentsMenuItem)
                                            assignmentLeafItem(out, imgBase, a);
                                        else
                                            assignmentLeafItem(out, imgBase, x.getCounter(),a);
                                    }
                                }
                                if (printAssignmentsMenuItem)
                                    end_item(out);
                            }
                            
                            end_item(out);
                        }
                        end_item(out);
                    }
                    end_item(out);
                }
                
            }
        if (printHeader) {
            out.println("</ul>");
            out.println("</body></html>");
        }
    }
    
    /** Print conflict-based statistics */
    public void print(PrintWriter out, long iteration) {
        out.print("Statistics{");
        for (Enumeration e1 =ToolBox.sortEnumeration(iNoGoods.keys(),Assignment.getComparator(iteration));e1.hasMoreElements();) {
            Assignment ass = (Assignment)e1.nextElement();
            double cnt = 0.0;
            for (Enumeration e2 = ((Vector)iNoGoods.get(ass)).elements();e2.hasMoreElements();)
                cnt += ((AssignmentSet)e2.nextElement()).getCounter();
            if (cnt < 100) continue;
            out.print("\n      "+cnt+"x "+ass.getValue().variable().getName()+" != "+ass.getValue().getName()+" <= {");
            for (Enumeration e2 = ((Vector)iNoGoods.get(ass)).elements();e2.hasMoreElements();) {
                AssignmentSet x = (AssignmentSet)e2.nextElement();
                if (x.getCounter() >= 10) {
                    out.print("\n        "+x.getCounter()+"x "+(x.getName() == null ? null : x.getName())+ "{");
                    for (Enumeration e3 = ToolBox.sortEnumeration(x.getSet().elements(),Assignment.getComparator(iteration));e3.hasMoreElements();) {
                        Assignment a = (Assignment)e3.nextElement();
                        out.print(a.getValue().variable().getName()+" := "+a.getValue().getName()+(e3.hasMoreElements() ? "," : ""));
                    }
                    out.print(e2.hasMoreElements() ? "}," : "}");
                }
            }
            out.print("\n      }");
        }
        out.print("\n    }");
        out.flush();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("Statistics{");
        for (Enumeration e1 = ToolBox.sortEnumeration(iUnassignedVariables.keys(), Assignment.getComparator(0));e1.hasMoreElements();) {
            Variable variable = (Variable)e1.nextElement();
            if (variable.countAssignments() < 100) continue;
            sb.append("\n      ").append(variable.countAssignments() + "x ").append(variable.getName()).append(" <= {");
            for (Enumeration e2 = ToolBox.sortEnumeration(((Vector)iUnassignedVariables.get(variable)).elements(),Assignment.getComparator(0));e2.hasMoreElements();) {
                Assignment x = (Assignment)e2.nextElement();
                if (x.getCounter(0) >= 10)
                    sb.append("\n        ").append(x.toString(0, true)).append(e2.hasMoreElements() ? "," : "");
            }
            sb.append("\n      }");
        }
        sb.append("\n    }");
        return sb.toString();
    }
    
    public void getInfo(Hashtable info) {
        //info.put("Statistics: IsGood.Time",sTimeFormat.format(((double)iIsGoodTime)/60000.0)+" min");
        //info.put("Statistics: NoGood.Time",sTimeFormat.format(((double)iNoGoodTime)/60000.0)+" min");
        /*info.put("Statistics: VariableAssigned.Time",sTimeFormat.format(((double)iVariableAssignedTime)/60000.0)+" min");
         *info.put("Statistics: VariableUnassigned.Time",sTimeFormat.format(((double)iVariableUnassignedTime)/60000.0)+" min");
         *info.put("Statistics: Bad assignments:", String.valueOf(iBadAssignments.size()));*/
    }
    
    private class VariableComparator implements java.util.Comparator {
        Hashtable iVars = null;
        public VariableComparator(Hashtable vars) {
            iVars = vars;
        }
        public int compare(Object o1, Object o2) {
            long c1 = ((Long)iVars.get(o1)).longValue();
            long c2 = ((Long)iVars.get(o2)).longValue();
            if (c1 != c2)
                return (c1 < c2 ? 1 : -1);
                return ((Variable)o1).getName().compareTo(((Variable)o2).getName());
        }
    }
    
    private class ConstraintComparator implements java.util.Comparator {
        Hashtable iConstrs = null;
        public ConstraintComparator(Hashtable constrs) {
            iConstrs = constrs;
        }
        public int compare(Object o1, Object o2) {
            long c1 = ((Long)iConstrs.get(o1)).longValue();
            long c2 = ((Long)iConstrs.get(o2)).longValue();
            if (c1 != c2)
                return (c1 < c2 ? 1 : -1);
                return ((Constraint)o1).getName().compareTo(
                ((Constraint)o2).getName());
        }
    }
    
    public void constraintBeforeAssigned( long iteration, Constraint constraint, Value assigned, Set unassigned) {
    }
    
    /** Increments appropriate counters when there is a value unassigned */
    public void constraintAfterAssigned(long iteration, Constraint constraint, Value assigned, Set unassigned) {
        if (unassigned == null || unassigned.isEmpty())
            return;
        if (iPrint) {
            AssignmentSet noGoods = AssignmentSet.createAssignmentSet(iteration,unassigned, iAgeing);
            noGoods.addAssignment(iteration, assigned, iAgeing);
            noGoods.setConstraint(constraint);
            for (Iterator i = unassigned.iterator(); i.hasNext();) {
                Value unassignedValue = (Value)i.next();
                variableUnassigned(iteration, unassignedValue, noGoods);
                variableUnassigned(iteration, unassignedValue, assigned);
            }
        } else {
            for (Iterator i = unassigned.iterator(); i.hasNext();) {
                Value unassignedValue = (Value)i.next();
                variableUnassigned(iteration, unassignedValue, assigned);
            }
        }
    }
    
    public void constraintAdded(Constraint constraint) {
        constraint.addConstraintListener(this);
    }
    public void constraintRemoved(Constraint constraint) {
        constraint.removeConstraintListener(this);
    }
    
    /* Solution listener -- prints conflict-based statistics in HTML format.*/
    public void solutionUpdated(Solution solution) {
        if (iPrint && iPrintInterval>0 && (solution.getIteration()%iPrintInterval) == 0) {
            try {
                int maxLines = getProperties().getPropertyInt("ConflictStatistics.MaxLines", 25);
                int maxBr1 = getProperties().getPropertyInt("ConflictStatistics.MaxBranchingLev1", 100);
                int maxBr2 = getProperties().getPropertyInt("ConflictStatistics.MaxBranchingLev2", 10);
                String imgBase = getProperties().getProperty("ConflictStatistics.ImageBase", null);
                if (TYPE_VARIABLE_BASED == getProperties().getPropertyInt("ConflictStatistics.Type",TYPE_VARIABLE_BASED)) {
                    PrintWriter pw = new PrintWriter(new FileWriter(getProperties().getProperty("General.Output", ".")+File.separator+"stat"+(++iPrintCounter)+".html"));
                    printHtml(solution.getIteration(),pw,true,maxLines,maxBr1,maxBr2,imgBase,TYPE_VARIABLE_BASED);
                    pw.flush();
                    pw.close();
                } else {
                    PrintWriter pw = new PrintWriter(new FileWriter(getProperties().getProperty("General.Output", ".")+File.separator+"stat"+(++iPrintCounter)+".html"));
                    printHtml(solution.getIteration(),pw,true,maxLines,maxBr1,maxBr2,imgBase,TYPE_CONSTRAINT_BASED);
                    pw.flush();
                    pw.close();
                }
            }
            catch (Exception io) {
                io.printStackTrace();
                sLogger.error(io);
            }
        }
    }
    public void getInfo(Solution solution, Dictionary info) {
    }
    public void bestCleared(Solution solution) {
    }
    public void bestSaved(Solution solution) {
    }
    public void bestRestored(Solution solution) {
    }
}
