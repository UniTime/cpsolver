package net.sf.cpsolver.exam.heuristics;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.neighbours.ExamRandomMove;
import net.sf.cpsolver.exam.neighbours.ExamRoomMove;
import net.sf.cpsolver.exam.neighbours.ExamTimeMove;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

public class ExamHillClimbing implements NeighbourSelection, SolutionListener {
    private static Logger sLog = Logger.getLogger(ExamHillClimbing.class);
    private NeighbourSelection[] iNeighbours = null;
    protected int iMaxIdleIters = 10000;
    protected int iLastImprovingIter = 0;
    protected int iIter = 0;

    public ExamHillClimbing(DataProperties properties) {
        iMaxIdleIters = properties.getPropertyInt("HillClimber.MaxIdle", iMaxIdleIters);
        iNeighbours = new NeighbourSelection[] {
                new ExamRandomMove(properties),
                new ExamRoomMove(properties),
                new ExamTimeMove(properties)
        };
    }
    
    public void init(Solver solver) {
        solver.currentSolution().addSolutionListener(this);
        for (int i=0;i<iNeighbours.length;i++)
            iNeighbours[i].init(solver);
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        Model model = (Model)solution.getModel();
        while (true) {
            iIter ++;
            if (!model.unassignedVariables().isEmpty())
                sLog.warn("Unassigned: "+model.unassignedVariables());
            if (iIter-iLastImprovingIter>=iMaxIdleIters) break;
            NeighbourSelection ns = iNeighbours[ToolBox.random(iNeighbours.length)];
            Neighbour n = ns.selectNeighbour(solution);
            if (n!=null && n.value()<=0) return n;
        }
        iIter = 0; iLastImprovingIter = 0;
        return null;
    }
    
    public void bestSaved(Solution solution) {
        iLastImprovingIter = iIter;
    }
    public void solutionUpdated(Solution solution) {}
    public void getInfo(Solution solution, java.util.Dictionary info) {}
    public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
    public void bestCleared(Solution solution) {}
    public void bestRestored(Solution solution){}   }
