package net.sf.cpsolver.studentsct;

import java.io.File;

import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * A simple class that converts a solution (with real students) into an empty solution with
 * expectations computed based on the assignments. It has two parameters, the input solution
 * (XML file) and the output solution (also an XML file).
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2013 Tomas Muller<br>
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

public class Solution2Expectations {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(Solution2Expectations.class);
    
    public static void main(String[] args) {
        try {
            ToolBox.configureLogging();
            
            DataProperties config = new DataProperties();
            
            String command = args[0];
            if ("real2exp".equals(command)) {
                StudentSectioningModel model = new StudentSectioningModel(config);
                StudentSectioningXMLLoader loader = new StudentSectioningXMLLoader(model);
                loader.setInputFile(new File(args[1]));
                loader.load();
                
                sLog.info("Loaded: " + ToolBox.dict2string(model.getExtendedInfo(), 2));
                
                for (Student s: model.getStudents()) s.setDummy(true);
                model.computeOnlineSectioningInfos();
                for (Student s: model.getStudents()) s.setDummy(false);
                for (Request request: model.variables())
                    if (request.getAssignment() != null) request.unassign(0);
                
                Solution<Request, Enrollment> solution = new Solution<Request, Enrollment>(model, 0, 0);
                Solver<Request, Enrollment> solver = new Solver<Request, Enrollment>(config);
                solver.setInitalSolution(solution);
                new StudentSectioningXMLSaver(solver).save(new File(args[2]));
                
                sLog.info("Saved: " + ToolBox.dict2string(model.getExtendedInfo(), 2));                
            } else if ("ll2exp".equals(command)) {
                StudentSectioningModel model = new StudentSectioningModel(config);
                StudentSectioningXMLLoader loader = new StudentSectioningXMLLoader(model);
                loader.setInputFile(new File(args[1]));
                loader.load();
                
                sLog.info("Loaded: " + ToolBox.dict2string(model.getExtendedInfo(), 2));
                
                model.computeOnlineSectioningInfos();
                for (Request request: model.variables())
                    if (request.getAssignment() != null) request.unassign(0);
                
                Solution<Request, Enrollment> solution = new Solution<Request, Enrollment>(model, 0, 0);
                Solver<Request, Enrollment> solver = new Solver<Request, Enrollment>(config);
                solver.setInitalSolution(solution);
                new StudentSectioningXMLSaver(solver).save(new File(args[2]));
                
                sLog.info("Saved: " + ToolBox.dict2string(model.getExtendedInfo(), 2));    
            } else if ("students".equals(command)) {
                StudentSectioningModel model = new StudentSectioningModel(config);
                StudentSectioningXMLLoader loader = new StudentSectioningXMLLoader(model);
                loader.setInputFile(new File(args[1]));
                loader.setLoadStudents(false);
                loader.load();
                
                sLog.info("Loaded [1]: " + ToolBox.dict2string(model.getExtendedInfo(), 2));

                StudentSectioningXMLLoader loder2 = new StudentSectioningXMLLoader(model);
                loder2.setInputFile(new File(args[2]));
                loder2.setLoadOfferings(false);
                loder2.setLoadStudents(true);
                loder2.load();
                
                sLog.info("Loaded [2]: " + ToolBox.dict2string(model.getExtendedInfo(), 2));

                Solution<Request, Enrollment> solution = new Solution<Request, Enrollment>(model, 0, 0);
                Solver<Request, Enrollment> solver = new Solver<Request, Enrollment>(config);
                solver.setInitalSolution(solution);
                new StudentSectioningXMLSaver(solver).save(new File(args[3]));
                
                sLog.info("Saved [3]: " + ToolBox.dict2string(model.getExtendedInfo(), 2));
            } else {
                sLog.error("Unknown command: " + command);
            }
        } catch (Exception e) {
            sLog.error(e.getMessage(), e);
        }
    }

}
