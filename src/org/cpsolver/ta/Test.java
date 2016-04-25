package org.cpsolver.ta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.ta.constraints.Student;
import org.cpsolver.ta.model.TAModel;
import org.cpsolver.ta.model.TeachingAssignment;
import org.cpsolver.ta.model.TeachingRequest;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class Test {

    public static void main(String[] args) {
        try {
            DataProperties config = new DataProperties();
            config.load(TAModel.class.getClass().getResourceAsStream("/org/cpsolver/ta/ta.properties"));
            config.putAll(System.getProperties());
            ToolBox.configureLogging(config.getProperty("output", "output"), config, false, true);

            int nrSolvers = config.getPropertyInt("Parallel.NrSolvers", 1);
            Solver<TeachingRequest, TeachingAssignment> solver = (nrSolvers == 1 ? new Solver<TeachingRequest, TeachingAssignment>(config) : new ParallelSolver<TeachingRequest, TeachingAssignment>(config));

            TAModel model = new TAModel(config);
            File inputDir = new File(config.getProperty("input", "input"));
            File inputSolution = new File(inputDir, "solution.xml");
            if (inputSolution.exists()) {
                Document document = (new SAXReader()).read(inputSolution);
                Assignment<TeachingRequest, TeachingAssignment> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<TeachingRequest, TeachingAssignment>() : new DefaultParallelAssignment<TeachingRequest, TeachingAssignment>());
                model.load(document, assignment);
                solver.setInitalSolution(new Solution<TeachingRequest, TeachingAssignment>(model, assignment));
            } else {
                model.load(inputDir);
                solver.setInitalSolution(model);
            }

            solver.currentSolution().addSolutionListener(new SolutionListener<TeachingRequest, TeachingAssignment>() {
                @Override
                public void solutionUpdated(Solution<TeachingRequest, TeachingAssignment> solution) {
                }

                @Override
                public void getInfo(Solution<TeachingRequest, TeachingAssignment> solution, Map<String, String> info) {
                }

                @Override
                public void getInfo(Solution<TeachingRequest, TeachingAssignment> solution, Map<String, String> info,
                        Collection<TeachingRequest> variables) {
                }

                @Override
                public void bestCleared(Solution<TeachingRequest, TeachingAssignment> solution) {
                }

                @Override
                public void bestSaved(Solution<TeachingRequest, TeachingAssignment> solution) {
                    Model<TeachingRequest, TeachingAssignment> m = solution.getModel();
                    Assignment<TeachingRequest, TeachingAssignment> a = solution.getAssignment();
                    System.out.println("**BEST[" + solution.getIteration() + "]** " + m.toString(a));
                }

                @Override
                public void bestRestored(Solution<TeachingRequest, TeachingAssignment> solution) {
                }
            });

            solver.start();
            try {
                solver.getSolverThread().join();
            } catch (InterruptedException e) {
            }

            Solution<TeachingRequest, TeachingAssignment> solution = solver.lastSolution();
            solution.restoreBest();

            System.out.println("Best solution found after " + solution.getBestTime() + " seconds (" + solution.getBestIteration() + " iterations).");
            System.out.println("Number of assigned variables is " + solution.getModel().assignedVariables(solution.getAssignment()).size());
            System.out.println("Total value of the solution is " + solution.getModel().getTotalValue(solution.getAssignment()));

            System.out.println("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));

            for (TeachingRequest v : solution.getModel().variables()) {
                System.out.println(v.toString() + ", " + (solution.getAssignment().getValue(v) == null ? "" : solution.getAssignment().getValue(v).getStudent().getStudentId()));
            }
            for (Constraint<TeachingRequest, TeachingAssignment> constraint : solution.getModel().constraints()) {
                if (constraint instanceof Student)
                    System.out.println(((Student)constraint).getContext(solution.getAssignment()).toString());
            }

            System.out.println("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));

            model.save(solution.getAssignment(), new File(config.getProperty("output", "output")));
            File outFile = new File(new File(config.getProperty("output", "output")), "solution.xml");
            FileOutputStream fos = new FileOutputStream(outFile);
            (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(model.save(solution.getAssignment()));
            fos.flush();
            fos.close();
            
            ConflictStatistics<TeachingRequest, TeachingAssignment> cbs = null;
            for (Extension<TeachingRequest, TeachingAssignment> extension : solver.getExtensions()) {
                if (ConflictStatistics.class.isInstance(extension)) {
                    cbs = (ConflictStatistics<TeachingRequest, TeachingAssignment>) extension;
                }
            }
            
            if (cbs != null) {
                PrintWriter out = new PrintWriter(new FileWriter(new File(new File(config.getProperty("output", "output")), "cbs.txt")));
                out.println(cbs.toString());
                out.flush(); out.close();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
