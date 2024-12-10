package org.cpsolver.ifs.example.jobshop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Job Shop model. <br>
 * <br>
 * It contains the number of available time slots and all machines and jobs. <br>
 * <br>
 * It can also load the model from a file and save the solution. <br>
 * <br>
 * <b>Input file format:</b>
 * First line:
 * <pre><code>&lt;number of jobs&gt; &lt;number of machines&gt;</code></pre>
 * Following lines:
 * <pre>
 * space separated list (a line for each job) of operations, each operation
 * consist of machine number and operation processing time
 * </pre>
 * Example of 10 jobs, 10 machines:
 * <pre><code>
 * 10 10
 * 4 88 8 68 6 94 5 99 1 67 2 89 9 77 7 99 0 86 3 92
 * 5 72 3 50 6 69 4 75 2 94 8 66 0 92 1 82 7 94 9 63
 * 9 83 8 61 0 83 1 65 6 64 5 85 7 78 4 85 2 55 3 77
 * 7 94 2 68 1 61 4 99 3 54 6 75 5 66 0 76 9 63 8 67
 * 3 69 4 88 9 82 8 95 0 99 2 67 6 95 5 68 7 67 1 86
 * 1 99 4 81 5 64 6 66 8 80 2 80 7 69 9 62 3 79 0 88
 * 7 50 1 86 4 97 3 96 0 95 8 97 2 66 5 99 6 52 9 71
 * 4 98 6 73 3 82 2 51 1 71 5 94 7 85 0 62 8 95 9 79
 * 0 94 6 71 3 81 7 85 1 66 2 90 4 76 5 58 8 93 9 97
 * 3 50 0 59 1 82 8 67 7 56 9 96 6 58 4 81 5 59 2 96
 * </code></pre>
 * For instance, the first job is described as follows:
 * <pre>
 * 88 time units on machine 4, then 68 time units on machine 8, then 94 time
 * units on machine 6 ...
 * </pre>
 * <br>
 * <b>Output file firmat:</b>
 * <pre>
 * A line for each machine, in each line there is a space separated list of jobs
 * which the machine will process in the order they will be processed.
 * </pre>
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class JobShopModel extends Model<Operation, Location> {
    private int iTotalNumberOfSlots = 1250;
    private Machine[] iMachines;
    private Job[] iJobs;

    /**
     * Constructor �
     * 
     * @param nrMachines
     *            number of machines
     * @param nrJobs
     *            number of jobs
     */
    public JobShopModel(int nrMachines, int nrJobs) {
        super();
        iMachines = new Machine[nrMachines];
        iJobs = new Job[nrJobs];
    }

    /** Get total number of slots 
     * @return total number of slots
     **/
    public int getTotalNumberOfSlots() {
        return iTotalNumberOfSlots;
    }

    /** Get machine of the given number
     * @param machineNumber machine number
     * @return machine of the given number
     **/
    public Machine getMachine(int machineNumber) {
        return iMachines[machineNumber];
    }

    /** Count number of machines in the model 
     * @return number of machines in the model
     **/
    public int countMachines() {
        return iMachines.length;
    }

    /** Get job of the given number 
     * @param jobNumber job number
     * @return job of the given number
     **/
    public Job getJob(int jobNumber) {
        return iJobs[jobNumber];
    }

    /** Count number of jobs in the model 
     * @return number of jobs in the model
     **/
    public int countJobs() {
        return iJobs.length;
    }

    private void setJob(int jobNumber, Job job) {
        iJobs[jobNumber] = job;
    }

    private void setMachine(int machineNumber, Machine machine) {
        iMachines[machineNumber] = machine;
    }

    /** Loads the model from the given file 
     * @param file file to load
     * @return loaded model
     * @throws IOException thrown when there is a problem reading the input file */
    public static JobShopModel loadModel(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while (line.startsWith("#"))
            line = reader.readLine();
        StringTokenizer stk = new StringTokenizer(line, " ");
        int nrJobs = Integer.parseInt(stk.nextToken());
        int nrMachines = Integer.parseInt(stk.nextToken());
        JobShopModel model = new JobShopModel(nrMachines, nrJobs);
        Machine[] machine = new Machine[nrMachines];
        for (int i = 0; i < nrMachines; i++) {
            machine[i] = new Machine(i);
            model.addConstraint(machine[i]);
            model.setMachine(i, machine[i]);
        }
        for (int i = 0; i < nrJobs; i++) {
            Job job = new Job(i);
            model.addConstraint(job);
            model.setJob(i, job);
            line = reader.readLine();
            stk = new StringTokenizer(line, " ");
            for (int j = 0; j < nrMachines; j++) {
                int machineNumber = Integer.parseInt(stk.nextToken());
                int processingTime = Integer.parseInt(stk.nextToken());
                Operation operation = new Operation(job, machine[machineNumber], j, processingTime);
                model.addVariable(operation);
                job.addVariable(operation);
                machine[machineNumber].addVariable(operation);
            }
            if (stk.hasMoreTokens()) {
                job.setDueTime(Integer.parseInt(stk.nextToken()));
            }
        }
        reader.close();
        for (Operation o : model.variables())
            o.init();
        return model;
    }

    /** Get finishing time of the current (partial) solution 
     * @param assignment current assignment
     * @return finishing time of the current (partial) solution
     **/
    public int getFinishingTime(Assignment<Operation, Location> assignment) {
        int ret = 0;
        for (Operation op : assignment.assignedVariables()) {
            ret = Math.max(ret, assignment.getValue(op).getFinishingTime());
        }
        return ret;
    }

    /** Get information table */
    @Override
    public Map<String, String> getInfo(Assignment<Operation, Location> assignment) {
        Map<String, String> ret = super.getInfo(assignment);
        ret.put("Finishing time", String.valueOf(getFinishingTime(assignment)));
        return ret;
    }

    /** Save the solution into the given file 
     * @param assignment current assignment
     * @param file file to write
     * @throws java.io.IOException throw when there is a problem writing the file
     **/
    public void save(Assignment<Operation, Location> assignment, String file) throws java.io.IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        for (int i = 0; i < countMachines(); i++) {
            Machine m = getMachine(i);
            List<Operation> ops = new ArrayList<Operation>(m.variables());
            Collections.sort(ops, new OperationComparator(assignment));
            for (Operation var : ops) {
                Operation op = var;
                if (assignment.getValue(op) != null)
                    writer.print((op.getJobNumber() < 10 ? " " : "") + op.getJobNumber() + " ");
            }
            writer.println();
        }
        writer.println(";");
        Map<String, String> info = getInfo(assignment);
        for (String key : info.keySet()) {
            String value = info.get(key);
            writer.println("; " + key + ": " + value);
        }
        writer.println(";");
        for (int i = 0; i < countJobs(); i++) {
            Job job = getJob(i);
            writer.print("; ");
            for (Operation op : job.variables()) {
                Location loc = assignment.getValue(op);
                writer.print((loc == null ? "----" : ToolBox.trim(String.valueOf(loc.getStartTime()), 4)) + " ");
            }
            writer.println();
        }
        writer.flush();
        writer.close();
    }

    private static class OperationComparator implements Comparator<Operation> {
        Assignment<Operation, Location> iAssignment;
        
        public OperationComparator(Assignment<Operation, Location> assignment) {
            iAssignment = assignment;
        }
        
        @Override
        public int compare(Operation op1, Operation op2) {
            Location loc1 = iAssignment.getValue(op1);
            Location loc2 = iAssignment.getValue(op2);
            if (loc1 == null) {
                if (loc2 == null)
                    return 0;
                else
                    return -1;
            }
            if (loc2 == null)
                return 1;
            return (loc1.getStartTime() < loc2.getStartTime() ? -1 : loc1.getStartTime() == loc2.getStartTime() ? 0 : 1);
        }
    }
}
