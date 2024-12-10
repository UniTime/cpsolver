package org.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentCommittedConflict;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.termination.TerminationCondition;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Student sectioning (after a solution is found). <br>
 * <br>
 * In the current implementation, students are not re-sectioned during the
 * search, but a student re-sectioning algorithm is called after the solver is
 * finished or upon the user's request. The re-sectioning is based on a local
 * search algorithm where the neighboring assignment is obtained from the
 * current assignment by applying one of the following moves:
 * <ul>
 * <li>Two students enrolled in the same course swap all of their class
 * assignments.
 * <li>A student is re-enrolled into classes associated with a course such that
 * the number of conflicts involving that student is minimized.
 * </ul>
 * The solver maintains a queue, initially containing all courses with multiple
 * classes. During each iteration, an improving move (i.e., a move decreasing
 * the overall number of student conflicts) is applied once discovered.
 * Re-sectioning is complete once no more improving moves are possible. Only
 * consistent moves (i.e., moves that respect class limits and other
 * constraints) are considered. Any additional courses having student conflicts
 * after a move is accepted are added to the queue. <br>
 * Since students are not re-sectioned during the timetabling search, the
 * computed number of student conflicts is really an upper bound on the actual
 * number that may exist afterward. To compensate for this during the search,
 * student conflicts between subparts with multiple classes are weighted lower
 * than conflicts between classes that meet at a single time (i.e., having
 * student conflicts that cannot be avoided by re-sectioning).
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

public class FinalSectioning {
    private TimetableModel iModel = null;
    public static double sEps = 0.0001;
    private boolean iWeighStudents = false;

    public FinalSectioning(TimetableModel model) {
        iModel = model;
        iWeighStudents = model.getProperties().getPropertyBoolean("General.WeightStudents", iWeighStudents);
    }
    
    public void execute(Solution<Lecture, Placement> solution, TerminationCondition<Lecture, Placement> termination) {
        Progress p = Progress.getInstance(iModel);
        p.setStatus("Student Sectioning...");
        Collection<Lecture> variables = new ArrayList<Lecture>(iModel.variables());
        // include committed classes that have structure
        if (iModel.hasConstantVariables())
            for (Lecture lecture: iModel.constantVariables()) {
                // if (lecture.getParent() != null || (lecture.sameStudentsLectures()!= null && !lecture.sameStudentsLectures().isEmpty()))
                variables.add(lecture);
            }
        while (!variables.isEmpty() && (termination == null || termination.canContinue(solution))) {
            // sLogger.debug("Shifting students ...");
            p.setPhase("moving students ...", variables.size());
            HashSet<Lecture> lecturesToRecompute = new HashSet<Lecture>(variables.size());

            for (Lecture lecture : variables) {
                if (lecture.getParent() == null) {
                    Configuration cfg = lecture.getConfiguration();
                    if (cfg != null && cfg.getAltConfigurations().size() > 1)
                        findAndPerformMoves(solution.getAssignment(), cfg, lecturesToRecompute);
                }
                // sLogger.debug("Shifting students for "+lecture);
                findAndPerformMoves(solution.getAssignment(), lecture, lecturesToRecompute);
                // sLogger.debug("Lectures to recompute: "+lects);
                p.incProgress();
            }
            // sLogger.debug("Shifting done, "+getViolatedStudentConflictsCounter().get()+" conflicts.");
            variables = lecturesToRecompute;
        }
    }

    /**
     * Perform sectioning on the given lecture
     * 
     * @param assignment current assignment
     * @param lecture
     *            given lecture
     * @param recursive
     *            recursively resection lectures affected by a student swap
     * @param configAsWell
     *            resection students between configurations as well
     **/
    public void resection(Assignment<Lecture, Placement> assignment, Lecture lecture, boolean recursive, boolean configAsWell) {
        HashSet<Lecture> variables = new HashSet<Lecture>();
        findAndPerformMoves(assignment, lecture, variables);
        if (configAsWell) {
            Configuration cfg = lecture.getConfiguration();
            if (cfg != null && cfg.getAltConfigurations().size() > 1)
                findAndPerformMoves(assignment, cfg, variables);
        }
        if (recursive) {
            while (!variables.isEmpty()) {
                HashSet<Lecture> lecturesToRecompute = new HashSet<Lecture>();
                for (Lecture l : variables) {
                    if (configAsWell && l.getParent() == null) {
                        Configuration cfg = l.getConfiguration();
                        if (cfg != null && cfg.getAltConfigurations().size() > 1)
                            findAndPerformMoves(assignment, cfg, lecturesToRecompute);
                    }
                    findAndPerformMoves(assignment, l, lecturesToRecompute);
                }
                variables = lecturesToRecompute;
            }
        }
    }

    /**
     * Swap students between this and the same lectures (lectures which differ
     * only in the section)
     * @param assignment current assignment
     * @param lecture a class that is being considered
     * @param lecturesToRecompute set of classes that may need to be re-considered
     */
    public void findAndPerformMoves(Assignment<Lecture, Placement> assignment, Lecture lecture, HashSet<Lecture> lecturesToRecompute) {
        if (lecture.sameSubpartLectures() == null || assignment.getValue(lecture) == null)
            return;

        if (lecture.getClassLimitConstraint() != null) {
            while (lecture.nrWeightedStudents() > sEps + lecture.minClassLimit()) {
                Move m = findAwayMove(assignment, lecture);
                if (m == null)
                    break;
                if (m.perform(assignment))
                    lecturesToRecompute.add(m.secondLecture());
                else
                    m.getUndoMove().perform(assignment);
            }
        } else if (!iWeighStudents) {
            while (true) {
                Move m = findAwayMove(assignment, lecture);
                if (m == null)
                    break;
                if (m.perform(assignment))
                    lecturesToRecompute.add(m.secondLecture());
                else
                    m.getUndoMove().perform(assignment);
            }
        }

        Set<Student> conflictStudents = lecture.conflictStudents(assignment);
        if (conflictStudents == null || conflictStudents.isEmpty())
            return;
        // sLogger.debug("  conflicts:"+conflictStudents.size()+"/"+conflictStudents);
        // sLogger.debug("Solution before swap is "+iModel.getInfo()+".");
        if (lecture.sameSubpartLectures().size() > 1) {
            for (Student student : conflictStudents) {
                if (assignment.getValue(lecture) == null)
                	continue;
                Move m = findMove(assignment, lecture, student);
                if (m != null) {
                    if (m.perform(assignment))
                        lecturesToRecompute.add(m.secondLecture());
                    else
                        m.getUndoMove().perform(assignment);
                }
            }
        } else {
            for (Student student : conflictStudents) {
                for (Lecture anotherLecture : lecture.conflictLectures(assignment, student)) {
                    if (anotherLecture.equals(lecture) || anotherLecture.sameSubpartLectures() == null
                            || assignment.getValue(anotherLecture) == null
                            || anotherLecture.sameSubpartLectures().size() <= 1)
                        continue;
                    lecturesToRecompute.add(anotherLecture);
                }
            }
        }
    }

    public void findAndPerformMoves(Assignment<Lecture, Placement> assignment, Configuration configuration, HashSet<Lecture> lecturesToRecompute) {
        for (Student student : configuration.students()) {
            if (!configuration.hasConflict(assignment, student))
                continue;
            
            MoveBetweenCfgs m = findMove(assignment, configuration, student);

            if (m != null) {
                if (m.perform(assignment))
                    lecturesToRecompute.addAll(m.secondLectures());
                else
                    m.getUndoMove().perform(assignment);
            }
        }
    }

    public Move findAwayMove(Assignment<Lecture, Placement> assignment, Lecture lecture) {
        List<Move> bestMoves = null;
        double bestDelta = 0;
        for (Student student : lecture.students()) {
            if (!student.canUnenroll(lecture))
                continue;
            for (Lecture sameLecture : lecture.sameSubpartLectures()) {
                double studentWeight = student.getOfferingWeight(sameLecture.getConfiguration());
                if (!student.canEnroll(sameLecture))
                    continue;
                if (sameLecture.equals(lecture) || assignment.getValue(sameLecture) == null)
                    continue;
                if (sameLecture.nrWeightedStudents() + studentWeight <= sEps + sameLecture.classLimit(assignment)) {
                    Move m = createMove(assignment, lecture, student, sameLecture, null);
                    if (m == null || m.isTabu())
                        continue;
                    double delta = m.getDelta(assignment);
                    if (delta < bestDelta) {
                        if (bestMoves == null)
                            bestMoves = new ArrayList<Move>();
                        else
                            bestMoves.clear();
                        bestMoves.add(m);
                        bestDelta = delta;
                    } else if (delta == bestDelta) {
                        if (bestMoves == null)
                            bestMoves = new ArrayList<Move>();
                        bestMoves.add(m);
                    }
                }
            }
        }
        if (bestDelta < -sEps && bestMoves != null) {
            Move m = ToolBox.random(bestMoves);
            return m;
        }
        return null;
    }

    public Move findMove(Assignment<Lecture, Placement> assignment, Lecture lecture, Student student) {
        if (!student.canUnenroll(lecture)) return null;
        double bestDelta = 0;
        List<Move> bestMoves = null;
        double studentWeight = student.getOfferingWeight(lecture.getConfiguration());
        for (Lecture sameLecture : lecture.sameSubpartLectures()) { // sameStudentLectures
            if (!student.canEnroll(sameLecture))
                continue;
            if (sameLecture.equals(lecture) || assignment.getValue(sameLecture) == null)
                continue;
            if (sameLecture.nrWeightedStudents() + studentWeight <= sEps + sameLecture.classLimit(assignment)) {
                Move m = createMove(assignment, lecture, student, sameLecture, null);
                if (m == null || m.isTabu())
                    continue;
                double delta = m.getDelta(assignment);
                if (delta < bestDelta) {
                    if (bestMoves == null)
                        bestMoves = new ArrayList<Move>();
                    else
                        bestMoves.clear();
                    bestMoves.add(m);
                    bestDelta = delta;
                } else if (delta == bestDelta) {
                    if (bestMoves == null)
                        bestMoves = new ArrayList<Move>();
                    bestMoves.add(m);
                }
            }
            for (Student anotherStudent : sameLecture.students()) {
                if (!anotherStudent.canUnenroll(sameLecture) || !anotherStudent.canEnroll(lecture))
                    continue;
                double anotherStudentWeight = anotherStudent.getOfferingWeight(lecture.getConfiguration());
                if (anotherStudentWeight != studentWeight) {
                    if (sameLecture.nrWeightedStudents() - anotherStudentWeight + studentWeight > sEps
                            + sameLecture.classLimit(assignment))
                        continue;
                    if (lecture.nrWeightedStudents() - studentWeight + anotherStudentWeight > sEps
                            + lecture.classLimit(assignment))
                        continue;
                }
                if (bestDelta < -sEps && bestMoves != null && bestMoves.size() > 10)
                    break;
                Move m = createMove(assignment, lecture, student, sameLecture, anotherStudent);
                if (m == null || m.isTabu())
                    continue;
                double delta = m.getDelta(assignment);
                if (delta < bestDelta) {
                    if (bestMoves == null)
                        bestMoves = new ArrayList<Move>();
                    else
                        bestMoves.clear();
                    bestMoves.add(m);
                    bestDelta = delta;
                } else if (delta == bestDelta) {
                    if (bestMoves == null)
                        bestMoves = new ArrayList<Move>();
                    bestMoves.add(m);
                }
            }
            if (Math.abs(bestDelta) < sEps && bestMoves != null && bestMoves.size() > 10)
                break;
        }
        if (bestDelta < -sEps && bestMoves != null)
            return ToolBox.random(bestMoves);
        return null;
    }

    public MoveBetweenCfgs findMove(Assignment<Lecture, Placement> assignment, Configuration config, Student student) {
        double bestDelta = 0;
        List<MoveBetweenCfgs> bestMoves = null;
        for (Configuration altConfig : config.getAltConfigurations()) {
            if (altConfig.equals(config))
                continue;

            MoveBetweenCfgs m = createMove(assignment, config, student, altConfig, null);
            if (m != null && !m.isTabu()) {
                double delta = m.getDelta(assignment);
                if (delta < bestDelta) {
                    if (bestMoves == null)
                        bestMoves = new ArrayList<MoveBetweenCfgs>();
                    else
                        bestMoves.clear();
                    bestMoves.add(m);
                    bestDelta = delta;
                } else if (delta == bestDelta) {
                    if (bestMoves == null)
                        bestMoves = new ArrayList<MoveBetweenCfgs>();
                    bestMoves.add(m);
                }
            }

            for (Student anotherStudent : altConfig.students()) {
                if (bestDelta < -sEps && bestMoves != null && bestMoves.size() > 10)
                    break;

                m = createMove(assignment, config, student, altConfig, anotherStudent);
                if (m != null && !m.isTabu()) {
                    double delta = m.getDelta(assignment);
                    if (delta < bestDelta) {
                        if (bestMoves == null)
                            bestMoves = new ArrayList<MoveBetweenCfgs>();
                        else
                            bestMoves.clear();
                        bestMoves.add(m);
                        bestDelta = delta;
                    } else if (delta == bestDelta) {
                        if (bestMoves == null)
                            bestMoves = new ArrayList<MoveBetweenCfgs>();
                        bestMoves.add(m);
                    }
                }
            }
            if (Math.abs(bestDelta) < sEps && bestMoves != null && bestMoves.size() > 10)
                break;
        }
        if (bestDelta < -sEps && bestMoves != null)
            return ToolBox.random(bestMoves);
        return null;
    }
    
    public Move createMove(Assignment<Lecture, Placement> assignment, Lecture firstLecture, Student firstStudent, Lecture secondLecture, Student secondStudent) {
        return createMove(assignment, firstLecture, firstStudent, secondLecture, secondStudent, null);
    }

    public Move createMove(Assignment<Lecture, Placement> assignment, Lecture firstLecture, Student firstStudent, Lecture secondLecture, Student secondStudent, Move parentMove) {
        if (!firstStudent.canUnenroll(firstLecture) || !firstStudent.canEnroll(secondLecture))
            return null;
        if (secondStudent != null && (!secondStudent.canUnenroll(secondLecture) || !secondStudent.canEnroll(firstLecture)))
            return null;
        if (firstLecture.getParent() != null && secondLecture.getParent() == null)
            return null;
        if (firstLecture.getParent() == null && secondLecture.getParent() != null)
            return null;
        
        Move move = new Move(firstLecture, firstStudent, secondLecture, secondStudent);

        if (parentMove == null) {
            Lecture l1 = firstLecture, l2 = secondLecture;
            while (l1.getParent() != null && l2.getParent() != null && !l1.getParent().equals(l2.getParent())) {
                Lecture p1 = l1.getParent();
                Lecture p2 = l2.getParent();
                if (assignment.getValue(p1) == null || assignment.getValue(p2) == null) return null;
                double w1 = firstStudent.getOfferingWeight(p1.getConfiguration());
                double w2 = (secondStudent == null ? 0.0 : secondStudent.getOfferingWeight(p2.getConfiguration()));
                if (w1 != w2) {
                    if (p1.nrWeightedStudents() - w1 + w2 > sEps + p1.classLimit(assignment))
                        return null;
                    if (p2.nrWeightedStudents() - w2 + w1 > sEps + p2.classLimit(assignment))
                        return null;
                }
                if (firstStudent.canUnenroll(p2) && firstStudent.canEnroll(p1) && (secondStudent == null || (secondStudent.canUnenroll(p1) && secondStudent.canEnroll(p2)))) {
                    move.addChildMove(new Move(p1, firstStudent, p2, secondStudent));
                } else {
                    return null;
                }
                l1 = p1; l2 = p2;
            }
        }

        if (firstLecture.hasAnyChildren() != secondLecture.hasAnyChildren())
            return null;
        if (firstLecture.hasAnyChildren()) {
            if (secondStudent != null) {
                for (Long subpartId: firstLecture.getChildrenSubpartIds()) {
                    Lecture firstChildLecture = firstLecture.getChild(firstStudent, subpartId);
                    Lecture secondChildLecture = secondLecture.getChild(secondStudent, subpartId);
                    if (firstChildLecture == null || secondChildLecture == null)
                        return null;
                    double firstStudentWeight = firstStudent.getOfferingWeight(firstChildLecture.getConfiguration());
                    double secondStudentWeight = secondStudent.getOfferingWeight(secondChildLecture.getConfiguration());
                    if (firstStudentWeight != secondStudentWeight) {
                        if (firstChildLecture.nrWeightedStudents() - firstStudentWeight + secondStudentWeight > sEps
                                + firstChildLecture.classLimit(assignment))
                            return null;
                        if (secondChildLecture.nrWeightedStudents() - secondStudentWeight + firstStudentWeight > sEps
                                + secondChildLecture.classLimit(assignment))
                            return null;
                    }
                    if (assignment.getValue(firstChildLecture) != null && assignment.getValue(secondChildLecture) != null) {
                        Move m = createMove(assignment, firstChildLecture, firstStudent, secondChildLecture, secondStudent, move);
                        if (m == null)
                            return null;
                        move.addChildMove(m);
                    } else
                        return null;
                }
            } else {
                for (Long subpartId: firstLecture.getChildrenSubpartIds()) {
                    Lecture firstChildLecture = firstLecture.getChild(firstStudent, subpartId);
                    if (firstChildLecture == null || assignment.getValue(firstChildLecture) == null)
                        return null;
                    double firstStudentWeight = firstStudent.getOfferingWeight(firstChildLecture.getConfiguration());
                    List<Lecture> secondChildLectures = secondLecture.getChildren(subpartId);
                    if (secondChildLectures == null || secondChildLectures.isEmpty())
                        return null;
                    List<Move> bestMoves = null;
                    double bestDelta = 0;
                    for (Lecture secondChildLecture : secondChildLectures) {
                        if (assignment.getValue(secondChildLecture) == null)
                            continue;
                        if (secondChildLecture.nrWeightedStudents() + firstStudentWeight > sEps
                                + secondChildLecture.classLimit(assignment))
                            continue;
                        Move m = createMove(assignment, firstChildLecture, firstStudent, secondChildLecture, secondStudent, move);
                        if (m == null)
                            continue;
                        double delta = m.getDelta(assignment);
                        if (bestMoves == null || delta < bestDelta) {
                            if (bestMoves == null)
                                bestMoves = new ArrayList<Move>();
                            else
                                bestMoves.clear();
                            bestMoves.add(m);
                            bestDelta = delta;
                        } else if (delta == bestDelta) {
                            bestMoves.add(m);
                        }
                    }
                    if (bestDelta >= 0 || bestMoves == null)
                        return null;
                    Move m = ToolBox.random(bestMoves);
                    move.addChildMove(m);
                }
            }
        }
        return move;
    }

    public class Move {
        Lecture iFirstLecture = null;
        Student iFirstStudent = null;
        Lecture iSecondLecture = null;
        Student iSecondStudent = null;
        List<Move> iChildMoves = null;

        private Move(Lecture firstLecture, Student firstStudent, Lecture secondLecture, Student secondStudent) {
            iFirstLecture = firstLecture;
            iFirstStudent = firstStudent;
            iSecondLecture = secondLecture;
            iSecondStudent = secondStudent;
        }

        public Lecture firstLecture() {
            return iFirstLecture;
        }

        public Student firstStudent() {
            return iFirstStudent;
        }

        public Lecture secondLecture() {
            return iSecondLecture;
        }

        public Student secondStudent() {
            return iSecondStudent;
        }

        public void addChildMove(Move move) {
            if (iChildMoves == null)
                iChildMoves = new ArrayList<Move>();
            iChildMoves.add(move);
        }

        public List<Move> getChildMoves() {
            return iChildMoves;
        }
        
        public Move getUndoMove() {
            Move ret = new Move(iSecondLecture, iFirstStudent, iFirstLecture, iSecondStudent);
            if (iChildMoves != null)
                for (Move move: iChildMoves)
                    ret.addChildMove(move.getUndoMove());
            return ret;
        }

        public boolean perform(Assignment<Lecture, Placement> assignment) {
            double conflicts = firstLecture().getModel().getCriterion(StudentConflict.class).getValue(assignment) +
                    firstLecture().getModel().getCriterion(StudentCommittedConflict.class).getValue(assignment);
            for (Lecture lecture : firstStudent().getLectures()) {
                if (lecture.equals(firstLecture()))
                    continue;
                JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                if (jenrl == null)
                    continue;
                jenrl.decJenrl(assignment, firstStudent());
                if (jenrl.getNrStudents() == 0) {
                    jenrl.getContext(assignment).unassigned(assignment, null);
                    Object[] vars = jenrl.variables().toArray();
                    for (int j = 0; j < vars.length; j++)
                        jenrl.removeVariable((Lecture) vars[j]);
                    iModel.removeConstraint(jenrl);
                }
            }
            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (lecture.equals(secondLecture()))
                        continue;
                    JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                    if (jenrl == null)
                        continue;
                    jenrl.decJenrl(assignment, secondStudent());
                    if (jenrl.getNrStudents() == 0) {
                        jenrl.getContext(assignment).unassigned(assignment, null);
                        Object[] vars = jenrl.variables().toArray();
                        for (int j = 0; j < vars.length; j++)
                            jenrl.removeVariable((Lecture) vars[j]);
                        iModel.removeConstraint(jenrl);
                    }
                }
            }

            firstLecture().removeStudent(assignment, firstStudent());
            firstStudent().removeLecture(firstLecture());
            secondLecture().addStudent(assignment, firstStudent());
            firstStudent().addLecture(secondLecture());
            if (secondStudent() != null) {
                secondLecture().removeStudent(assignment, secondStudent());
                secondStudent().removeLecture(secondLecture());
                firstLecture().addStudent(assignment, secondStudent());
                secondStudent().addLecture(firstLecture());
            }

            for (Lecture lecture : firstStudent().getLectures()) {
                if (lecture.equals(secondLecture()))
                    continue;
                JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                if (jenrl == null) {
                    jenrl = new JenrlConstraint();
                    jenrl.addVariable(secondLecture());
                    jenrl.addVariable(lecture);
                    iModel.addConstraint(jenrl);
                    // sLogger.debug(getName()+": add jenr {conf="+jenrl.isInConflict()+", lect="+anotherLecture.getName()+", jenr="+jenrl+"}");
                }
                jenrl.incJenrl(assignment, firstStudent());
            }
            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (lecture.equals(firstLecture()))
                        continue;
                    JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                    if (jenrl == null) {
                        jenrl = new JenrlConstraint();
                        jenrl.addVariable(lecture);
                        jenrl.addVariable(firstLecture());
                        iModel.addConstraint(jenrl);
                        // sLogger.debug(getName()+": add jenr {conf="+jenrl.isInConflict()+", lect="+anotherLecture.getName()+", jenr="+jenrl+"}");
                    }
                    jenrl.incJenrl(assignment, secondStudent());
                }
            }

            if (getChildMoves() != null) {
                for (Move move : getChildMoves()) {
                    move.perform(assignment);
                }
            }
            // sLogger.debug("Solution after swap is "+iModel.getInfo()+".");
            return firstLecture().getModel().getCriterion(StudentConflict.class).getValue(assignment) + firstLecture().getModel().getCriterion(StudentCommittedConflict.class).getValue(assignment) < conflicts;
        }

        public double getDelta(Assignment<Lecture, Placement> assignment) {
            double delta = 0;
            for (Lecture lecture : firstStudent().getLectures()) {
                if (assignment.getValue(lecture) == null || lecture.equals(firstLecture()))
                    continue;
                JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                if (jenrl == null)
                    continue;
                if (jenrl.isInConflict(assignment))
                    delta -= jenrl.getJenrlWeight(firstStudent());
            }
            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (assignment.getValue(lecture) == null || lecture.equals(secondLecture()))
                        continue;
                    JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                    if (jenrl == null)
                        continue;
                    if (jenrl.isInConflict(assignment))
                        delta -= jenrl.getJenrlWeight(secondStudent());
                }
            }

            for (Lecture lecture : firstStudent().getLectures()) {
                if (assignment.getValue(lecture) == null || lecture.equals(firstLecture()))
                    continue;
                JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                if (jenrl != null) {
                    if (jenrl.isInConflict(assignment))
                        delta += jenrl.getJenrlWeight(firstStudent());
                } else {
                    if (JenrlConstraint.isInConflict(assignment.getValue(secondLecture()), assignment.getValue(lecture), iModel.getDistanceMetric(), iModel.getStudentWorkDayLimit()))
                        delta += firstStudent().getJenrlWeight(secondLecture(), lecture);
                }
            }
            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (assignment.getValue(lecture) == null || lecture.equals(secondLecture()))
                        continue;
                    JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                    if (jenrl != null) {
                        if (jenrl.isInConflict(assignment))
                            delta += jenrl.getJenrlWeight(secondStudent());
                    } else {
                        if (JenrlConstraint.isInConflict(assignment.getValue(firstLecture()), assignment.getValue(lecture), iModel.getDistanceMetric(), iModel.getStudentWorkDayLimit()))
                            delta += secondStudent().getJenrlWeight(firstLecture(), lecture);
                    }
                }
            }

            Placement p1 = assignment.getValue(firstLecture());
            Placement p2 = assignment.getValue(secondLecture());
            delta += firstStudent().countConflictPlacements(p2) - firstStudent().countConflictPlacements(p1);
            if (secondStudent() != null)
                delta += secondStudent().countConflictPlacements(p1) - secondStudent().countConflictPlacements(p2);

            if (getChildMoves() != null) {
                for (Move move : getChildMoves()) {
                    delta += move.getDelta(assignment);
                }
            }
            return delta;
        }

        public boolean isTabu() {
            return false;
        }

        @Override
        public String toString() {
            return "Move{" + firstStudent() + "/" + firstLecture() + " <-> " + secondStudent() + "/" + secondLecture()
                    + ", ch=" + getChildMoves() + "}";

        }

    }

    public MoveBetweenCfgs createMove(Assignment<Lecture, Placement> assignment, Configuration firstConfig, Student firstStudent, Configuration secondConfig,
            Student secondStudent) {
        MoveBetweenCfgs m = new MoveBetweenCfgs(firstConfig, firstStudent, secondConfig, secondStudent);

        for (Long subpartId: firstConfig.getTopSubpartIds()) {
            if (!addLectures(assignment, firstStudent, secondStudent, m.firstLectures(), firstConfig.getTopLectures(subpartId)))
                return null;
        }

        for (Long subpartId: secondConfig.getTopSubpartIds()) {
            if (!addLectures(assignment, secondStudent, firstStudent, m.secondLectures(), secondConfig.getTopLectures(subpartId)))
                return null;
        }
        
        if (m.firstLectures().isEmpty() || m.secondLectures().isEmpty()) return null;

        return m;
    }

    private boolean addLectures(Assignment<Lecture, Placement> assignment, Student student, Student newStudent, Set<Lecture> studentLectures,
            Collection<Lecture> lectures) {
        Lecture lecture = null;
        if (lectures == null)
            return false;

        if (student != null) {
            for (Lecture l : lectures) {
                if (l.students().contains(student)) {
                    lecture = l;
                    if (!student.canUnenroll(lecture)) return false;
                    break;
                }
            }
        } else {
            int bestValue = 0;
            Lecture bestLecture = null;
            for (Lecture l : lectures) {
                int val = test(assignment, newStudent, l);
                if (val < 0)
                    continue;
                if (bestLecture == null || bestValue > val) {
                    bestValue = val;
                    bestLecture = l;
                }
            }
            lecture = bestLecture;
        }

        if (lecture == null)
            return false;
        if (newStudent != null && !newStudent.canEnroll(lecture))
            return false;
        if (lecture.getModel() == null) return false;
        studentLectures.add(lecture);
        if (lecture.getChildrenSubpartIds() != null) {
            for (Long subpartId: lecture.getChildrenSubpartIds()) {
                if (!addLectures(assignment, student, newStudent, studentLectures, lecture.getChildren(subpartId)))
                    return false;
            }
        }

        return true;
    }

    public int test(Assignment<Lecture, Placement> assignment, Student student, Lecture lecture) {
        if (assignment.getValue(lecture) == null)
            return -1;
        double studentWeight = student.getOfferingWeight(lecture.getConfiguration());
        if (lecture.nrWeightedStudents() + studentWeight > sEps + lecture.classLimit(assignment))
            return -1;
        if (!student.canEnroll(lecture))
            return -1;

        int test = 0;
        for (Lecture x : student.getLectures()) {
            if (assignment.getValue(x) == null)
                continue;
            if (JenrlConstraint.isInConflict(assignment.getValue(lecture), assignment.getValue(x), iModel.getDistanceMetric(), iModel.getStudentWorkDayLimit()))
                test++;
        }
        test += student.countConflictPlacements(assignment.getValue(lecture));

        if (lecture.getChildrenSubpartIds() != null) {
            for (Long subpartId: lecture.getChildrenSubpartIds()) {
                int bestTest = -1;
                for (Lecture child : lecture.getChildren(subpartId)) {
                    int t = test(assignment, student, child);
                    if (t < 0)
                        continue;
                    if (bestTest < 0 || bestTest > t)
                        bestTest = t;
                }
                if (bestTest < 0)
                    return -1;
                test += bestTest;
            }
        }
        return test;
    }

    public class MoveBetweenCfgs {
        Configuration iFirstConfig = null;
        Set<Lecture> iFirstLectures = new HashSet<Lecture>();
        Student iFirstStudent = null;
        Configuration iSecondConfig = null;
        Set<Lecture> iSecondLectures = new HashSet<Lecture>();
        Student iSecondStudent = null;

        public MoveBetweenCfgs(Configuration firstConfig, Student firstStudent, Configuration secondConfig,
                Student secondStudent) {
            iFirstConfig = firstConfig;
            iFirstStudent = firstStudent;
            iSecondConfig = secondConfig;
            iSecondStudent = secondStudent;
        }

        public Configuration firstConfiguration() {
            return iFirstConfig;
        }

        public Student firstStudent() {
            return iFirstStudent;
        }

        public Set<Lecture> firstLectures() {
            return iFirstLectures;
        }

        public Configuration secondConfiguration() {
            return iSecondConfig;
        }

        public Student secondStudent() {
            return iSecondStudent;
        }

        public Set<Lecture> secondLectures() {
            return iSecondLectures;
        }
        
        public MoveBetweenCfgs getUndoMove() {
            MoveBetweenCfgs ret = new MoveBetweenCfgs(iSecondConfig, iFirstStudent, iFirstConfig, iSecondStudent);
            ret.secondLectures().addAll(iFirstLectures);
            ret.firstLectures().addAll(iSecondLectures);
            return ret;
        }

        public boolean perform(Assignment<Lecture, Placement> assignment) {
            double conflicts = firstLectures().iterator().next().getModel().getCriterion(StudentConflict.class).getValue(assignment) +
                    firstLectures().iterator().next().getModel().getCriterion(StudentCommittedConflict.class).getValue(assignment);
            firstStudent().removeConfiguration(firstConfiguration());
            firstStudent().addConfiguration(secondConfiguration());
            for (Lecture lecture : firstStudent().getLectures()) {

                for (Lecture firstLecture : firstLectures()) {
                    if (firstLecture.equals(lecture))
                        continue;
                    if (firstLectures().contains(lecture)
                            && firstLecture.getClassId().compareTo(lecture.getClassId()) > 0)
                        continue;

                    JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                    if (jenrl == null)
                        continue;
                    jenrl.decJenrl(assignment, firstStudent());
                    if (jenrl.getNrStudents() == 0) {
                        jenrl.getContext(assignment).unassigned(assignment, null);
                        Object[] vars = jenrl.variables().toArray();
                        for (int k = 0; k < vars.length; k++)
                            jenrl.removeVariable((Lecture) vars[k]);
                        iModel.removeConstraint(jenrl);
                    }
                }
            }

            if (secondStudent() != null) {
                secondStudent().removeConfiguration(secondConfiguration());
                secondStudent().addConfiguration(firstConfiguration());
                for (Lecture lecture : secondStudent().getLectures()) {

                    for (Lecture secondLecture : secondLectures()) {
                        if (secondLecture.equals(lecture))
                            continue;
                        if (secondLectures().contains(lecture)
                                && secondLecture.getClassId().compareTo(lecture.getClassId()) > 0)
                            continue;

                        JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                        if (jenrl == null)
                            continue;
                        jenrl.decJenrl(assignment, secondStudent());
                        if (jenrl.getNrStudents() == 0) {
                            jenrl.getContext(assignment).unassigned(assignment, null);
                            Object[] vars = jenrl.variables().toArray();
                            for (int k = 0; k < vars.length; k++)
                                jenrl.removeVariable((Lecture) vars[k]);
                            iModel.removeConstraint(jenrl);
                        }
                    }
                }
            }

            for (Lecture firstLecture : firstLectures()) {
                firstLecture.removeStudent(assignment, firstStudent());
                firstStudent().removeLecture(firstLecture);
                if (secondStudent() != null) {
                    firstLecture.addStudent(assignment, secondStudent());
                    secondStudent().addLecture(firstLecture);
                }
            }
            for (Lecture secondLecture : secondLectures()) {
                secondLecture.addStudent(assignment, firstStudent());
                firstStudent().addLecture(secondLecture);
                if (secondStudent() != null) {
                    secondLecture.removeStudent(assignment, secondStudent());
                    secondStudent().removeLecture(secondLecture);
                }
            }

            for (Lecture lecture : firstStudent().getLectures()) {

                for (Lecture secondLecture : secondLectures()) {
                    if (secondLecture.equals(lecture))
                        continue;
                    if (secondLectures().contains(lecture)
                            && secondLecture.getClassId().compareTo(lecture.getClassId()) > 0)
                        continue;

                    JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                    if (jenrl == null) {
                        jenrl = new JenrlConstraint();
                        jenrl.addVariable(secondLecture);
                        jenrl.addVariable(lecture);
                        iModel.addConstraint(jenrl);
                    }
                    jenrl.incJenrl(assignment, firstStudent());
                }
            }

            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {

                    for (Lecture firstLecture : firstLectures()) {
                        if (firstLecture.equals(lecture))
                            continue;
                        if (firstLectures().contains(lecture)
                                && firstLecture.getClassId().compareTo(lecture.getClassId()) > 0)
                            continue;

                        JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                        if (jenrl == null) {
                            jenrl = new JenrlConstraint();
                            jenrl.addVariable(firstLecture);
                            jenrl.addVariable(lecture);
                            iModel.addConstraint(jenrl);
                        }
                        jenrl.incJenrl(assignment, secondStudent());
                    }
                }
            }
            return firstLectures().iterator().next().getModel().getCriterion(StudentConflict.class).getValue(assignment) + 
                    firstLectures().iterator().next().getModel().getCriterion(StudentCommittedConflict.class).getValue(assignment) < conflicts;
        }

        public double getDelta(Assignment<Lecture, Placement> assignment) {
            double delta = 0;

            for (Lecture lecture : firstStudent().getLectures()) {
                if (assignment.getValue(lecture) == null)
                    continue;

                for (Lecture firstLecture : firstLectures()) {
                    if (assignment.getValue(firstLecture) == null || firstLecture.equals(lecture))
                        continue;
                    JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                    if (jenrl == null)
                        continue;
                    if (jenrl.isInConflict(assignment))
                        delta -= jenrl.getJenrlWeight(firstStudent());
                }

                for (Lecture secondLecture : secondLectures()) {
                    if (assignment.getValue(secondLecture) == null || secondLecture.equals(lecture))
                        continue;
                    JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                    if (jenrl != null) {
                        if (jenrl.isInConflict(assignment))
                            delta += jenrl.getJenrlWeight(firstStudent());
                    } else {
                        if (JenrlConstraint.isInConflict(assignment.getValue(secondLecture), assignment.getValue(lecture), iModel.getDistanceMetric(), iModel.getStudentWorkDayLimit()))
                            delta += firstStudent().getJenrlWeight(secondLecture, lecture);
                    }
                }
            }

            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (assignment.getValue(lecture) == null)
                        continue;

                    for (Lecture secondLecture : secondLectures()) {
                        if (assignment.getValue(secondLecture) == null || secondLecture.equals(lecture))
                            continue;
                        JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                        if (jenrl == null)
                            continue;
                        if (jenrl.isInConflict(assignment))
                            delta -= jenrl.getJenrlWeight(secondStudent());
                    }

                    for (Lecture firstLecture : firstLectures()) {
                        if (assignment.getValue(firstLecture) == null || firstLecture.equals(lecture))
                            continue;
                        JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                        if (jenrl != null) {
                            if (jenrl.isInConflict(assignment))
                                delta += jenrl.getJenrlWeight(secondStudent());
                        } else {
                            if (JenrlConstraint.isInConflict(assignment.getValue(firstLecture), assignment.getValue(lecture), iModel.getDistanceMetric(), iModel.getStudentWorkDayLimit()))
                                delta += secondStudent().getJenrlWeight(firstLecture, lecture);
                        }
                    }
                }
            }

            for (Lecture firstLecture : firstLectures()) {
                Placement p1 = assignment.getValue(firstLecture);
                if (p1 == null)
                    continue;
                delta -= firstStudent().countConflictPlacements(p1);
                if (secondStudent() != null)
                    delta += secondStudent().countConflictPlacements(p1);
            }

            for (Lecture secondLecture : secondLectures()) {
                Placement p2 = assignment.getValue(secondLecture);
                if (p2 == null)
                    continue;
                delta += firstStudent().countConflictPlacements(p2);
                if (secondStudent() != null)
                    delta -= secondStudent().countConflictPlacements(p2);
            }

            return delta;
        }

        public boolean isTabu() {
            return false;
        }

        @Override
        public String toString() {
            return "Move{" + firstStudent() + "/" + firstConfiguration().getConfigId() + " <-> " + secondStudent()
                    + "/" + secondConfiguration().getConfigId() + "}";
        }

    }
}
