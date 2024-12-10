package org.cpsolver.coursett;

import java.io.File;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

/**
 * A simple class that just loads a solution and saves information about it in a CSV file (output.csv format). <br>
 * <br>
 * Usage:<br>
 * java -Xmx1024m -jar coursett1.3.jar -cp org.cpsolver.coursett.SolutionEvaluator config.properties soultion.xml output.csv<br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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
public class SolutionEvaluator {
    
    public static void main(String[] args) throws Exception {
        ToolBox.configureLogging();
        DataProperties properties = ToolBox.loadProperties(new java.io.File(args[0]));
        properties.putAll(System.getProperties());
        
        TimetableModel model = new TimetableModel(properties);
        Assignment<Lecture, Placement> assignment = new DefaultSingleAssignment<Lecture, Placement>();
        TimetableXMLLoader loader = new TimetableXMLLoader(model, assignment);
        loader.setInputFile(new File(args[1]));
        loader.load();
        
        Solution<Lecture, Placement> solution = new Solution<Lecture, Placement>(model, assignment);
        Test.saveOutputCSV(solution, new File(args[2]));
    }

}
