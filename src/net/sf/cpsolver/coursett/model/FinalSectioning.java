package net.sf.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Student sectioning (after a solution is found). <br>
 * <br>
 * In the current implementation, students are not re-sectioned during the
 * search, but a student re-sectioning algorithm is called after the solver is
 * finished or upon the user’s request. The re-sectioning is based on a local
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
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class FinalSectioning implements Runnable {
    private TimetableModel iModel = null;
    private Progress iProgress = null;
    public static double sEps = 0.0001;
    private boolean iWeighStudents = false;

    public FinalSectioning(TimetableModel model) {
        iModel = model;
        iProgress = Progress.getInstance(iModel);
        iWeighStudents = model.getProperties().getPropertyBoolean("General.WeightStudents", iWeighStudents);
    }

    public void run() {
        iProgress.setStatus("Student Sectioning...");
        Collection<Lecture> variables = new ArrayList<Lecture>(iModel.variables());
        // include committed classes that have structure
        if (iModel.hasConstantVariables())
            for (Lecture lecture: iModel.constantVariables()) {
                if (lecture.getParent() != null || (lecture.sameStudentsLectures()!= null && !lecture.sameStudentsLectures().isEmpty()))
                    variables.add(lecture);
            }
        while (!variables.isEmpty()) {
            // sLogger.debug("Shifting students ...");
            iProgress.setPhase("moving students ...", variables.size());
            HashSet<Lecture> lecturesToRecompute = new HashSet<Lecture>(variables.size());

            for (Lecture lecture : variables) {
                if (lecture.getParent() == null) {
                    Configuration cfg = lecture.getConfiguration();
                    if (cfg != null && cfg.getAltConfigurations().size() > 1)
                        findAndPerformMoves(cfg, lecturesToRecompute);
                }
                // sLogger.debug("Shifting students for "+lecture);
                findAndPerformMoves(lecture, lecturesToRecompute);
                // sLogger.debug("Lectures to recompute: "+lects);
                iProgress.incProgress();
            }
            // sLogger.debug("Shifting done, "+getViolatedStudentConflictsCounter().get()+" conflicts.");
            variables = lecturesToRecompute;
        }
    }

    /**
     * Perform sectioning on the given lecture
     * 
     * @param lecture
     *            given lecture
     * @param recursive
     *            recursively resection lectures affected by a student swap
     * @param configAsWell
     *            resection students between configurations as well
     **/
    public void resection(Lecture lecture, boolean recursive, boolean configAsWell) {
        HashSet<Lecture> variables = new HashSet<Lecture>();
        findAndPerformMoves(lecture, variables);
        if (configAsWell) {
            Configuration cfg = lecture.getConfiguration();
            if (cfg != null && cfg.getAltConfigurations().size() > 1)
                findAndPerformMoves(cfg, variables);
        }
        if (recursive) {
            while (!variables.isEmpty()) {
                HashSet<Lecture> lecturesToRecompute = new HashSet<Lecture>();
                for (Lecture l : variables) {
                    if (configAsWell && l.getParent() == null) {
                        Configuration cfg = l.getConfiguration();
                        if (cfg != null && cfg.getAltConfigurations().size() > 1)
                            findAndPerformMoves(cfg, lecturesToRecompute);
                    }
                    findAndPerformMoves(l, lecturesToRecompute);
                }
                variables = lecturesToRecompute;
            }
        }
    }

    /**
     * Swap students between this and the same lectures (lectures which differ
     * only in the section)
     */
    public void findAndPerformMoves(Lecture lecture, HashSet<Lecture> lecturesToRecompute) {
        if (lecture.sameStudentsLectures() == null || lecture.getAssignment() == null)
            return;

        if (lecture.getClassLimitConstraint() != null) {
            while (lecture.nrWeightedStudents() > sEps + lecture.minClassLimit()) {
                Move m = findAwayMove(lecture);
                if (m == null)
                    break;
                if (m.perform())
                    lecturesToRecompute.add(m.secondLecture());
            }
        } else if (!iWeighStudents) {
            while (true) {
                Move m = findAwayMove(lecture);
                if (m == null)
                    break;
                if (m.perform())
                    lecturesToRecompute.add(m.secondLecture());
            }
        }

        Set<Student> conflictStudents = lecture.conflictStudents();
        if (conflictStudents == null || conflictStudents.isEmpty())
            return;
        // sLogger.debug("  conflicts:"+conflictStudents.size()+"/"+conflictStudents);
        // sLogger.debug("Solution before swap is "+iModel.getInfo()+".");
        if (lecture.sameStudentsLectures().size() > 1) {
            for (Student student : conflictStudents) {
                if (lecture.getAssignment() == null)
                    continue;
                Move m = findMove(lecture, student);
                if (m != null) {
                    if (m.perform())
                        lecturesToRecompute.add(m.secondLecture());
                }
            }
        } else {
            for (Student student : conflictStudents) {
                for (Lecture anotherLecture : lecture.conflictLectures(student)) {
                    if (anotherLecture.equals(lecture) || anotherLecture.sameStudentsLectures() == null
                            || anotherLecture.getAssignment() == null
                            || anotherLecture.sameStudentsLectures().size() <= 1)
                        continue;
                    lecturesToRecompute.add(anotherLecture);
                }
            }
        }
    }

    public void findAndPerformMoves(Configuration configuration, HashSet<Lecture> lecturesToRecompute) {
        for (Student student : configuration.students()) {
            if (!configuration.hasConflict(student))
                continue;

            MoveBetweenCfgs m = findMove(configuration, student);

            if (m != null) {
                if (m.perform())
                    lecturesToRecompute.addAll(m.secondLectures());
            }
        }
    }

    public Move findAwayMove(Lecture lecture) {
        List<Move> bestMoves = null;
        double bestDelta = 0;
        for (Student student : lecture.students()) {
            for (Lecture sameLecture : lecture.sameStudentsLectures()) {
                double studentWeight = student.getOfferingWeight(sameLecture.getConfiguration());
                if (!student.canEnroll(sameLecture))
                    continue;
                if (sameLecture.equals(lecture) || sameLecture.getAssignment() == null)
                    continue;
                if (sameLecture.nrWeightedStudents() + studentWeight <= sEps + sameLecture.classLimit()) {
                    Move m = createMove(lecture, student, sameLecture, null);
                    if (m == null || m.isTabu())
                        continue;
                    double delta = m.getDelta();
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

    public Move findMove(Lecture lecture, Student student) {
        double bestDelta = 0;
        List<Move> bestMoves = null;
        double studentWeight = student.getOfferingWeight(lecture.getConfiguration());
        for (Lecture sameLecture : lecture.sameStudentsLectures()) {
            if (!student.canEnroll(sameLecture))
                continue;
            if (sameLecture.equals(lecture) || sameLecture.getAssignment() == null)
                continue;
            if (sameLecture.nrWeightedStudents() + studentWeight <= sEps + sameLecture.classLimit()) {
                Move m = createMove(lecture, student, sameLecture, null);
                if (m == null || m.isTabu())
                    continue;
                double delta = m.getDelta();
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
                double anotherStudentWeight = anotherStudent.getOfferingWeight(lecture.getConfiguration());
                if (!anotherStudent.canEnroll(lecture))
                    continue;
                if (anotherStudentWeight != studentWeight) {
                    if (sameLecture.nrWeightedStudents() - anotherStudentWeight + studentWeight > sEps
                            + sameLecture.classLimit())
                        continue;
                    if (lecture.nrWeightedStudents() - studentWeight + anotherStudentWeight > sEps
                            + lecture.classLimit())
                        continue;
                }
                if (bestDelta < -sEps && bestMoves != null && bestMoves.size() > 10)
                    break;
                Move m = createMove(lecture, student, sameLecture, anotherStudent);
                if (m == null || m.isTabu())
                    continue;
                double delta = m.getDelta();
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

    public MoveBetweenCfgs findMove(Configuration config, Student student) {
        double bestDelta = 0;
        List<MoveBetweenCfgs> bestMoves = null;
        for (Configuration altConfig : config.getAltConfigurations()) {
            if (altConfig.equals(config))
                continue;

            MoveBetweenCfgs m = createMove(config, student, altConfig, null);
            if (m != null && !m.isTabu()) {
                double delta = m.getDelta();
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

            for (Student anotherStudent : config.students()) {
                if (bestDelta < -sEps && bestMoves != null && bestMoves.size() > 10)
                    break;
                m = createMove(config, student, altConfig, anotherStudent);
                if (m != null && !m.isTabu()) {
                    double delta = m.getDelta();
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

    public Move createMove(Lecture firstLecture, Student firstStudent, Lecture secondLecture, Student secondStudent) {
        if (!firstStudent.canEnroll(secondLecture))
            return null;
        if (secondStudent != null && !secondStudent.canEnroll(firstLecture))
            return null;
        if (firstLecture.getParent() != null && secondLecture.getParent() == null)
            return null;
        if (firstLecture.getParent() == null && secondLecture.getParent() != null)
            return null;

        Move move = new Move(firstLecture, firstStudent, secondLecture, secondStudent);
        if (firstLecture.hasAnyChildren() != secondLecture.hasAnyChildren())
            return null;
        if (firstLecture.hasAnyChildren()) {
            if (secondStudent != null) {
                for (Enumeration<Long> e = firstLecture.getChildrenSubpartIds(); e.hasMoreElements();) {
                    Long subpartId = e.nextElement();
                    Lecture firstChildLecture = firstLecture.getChild(firstStudent, subpartId);
                    Lecture secondChildLecture = secondLecture.getChild(secondStudent, subpartId);
                    if (firstChildLecture == null || secondChildLecture == null)
                        return null;
                    double firstStudentWeight = firstStudent.getOfferingWeight(firstChildLecture.getConfiguration());
                    double secondStudentWeight = secondStudent.getOfferingWeight(secondChildLecture.getConfiguration());
                    if (firstStudentWeight != secondStudentWeight) {
                        if (firstChildLecture.nrWeightedStudents() - firstStudentWeight + secondStudentWeight > sEps
                                + firstChildLecture.classLimit())
                            return null;
                        if (secondChildLecture.nrWeightedStudents() - secondStudentWeight + firstStudentWeight > sEps
                                + secondChildLecture.classLimit())
                            return null;
                    }
                    if (firstChildLecture.getAssignment() != null && secondChildLecture.getAssignment() != null) {
                        Move m = createMove(firstChildLecture, firstStudent, secondChildLecture, secondStudent);
                        if (m == null)
                            return null;
                        move.addChildMove(m);
                    } else
                        return null;
                }
            } else {
                for (Enumeration<Long> e1 = firstLecture.getChildrenSubpartIds(); e1.hasMoreElements();) {
                    Long subpartId = e1.nextElement();
                    Lecture firstChildLecture = firstLecture.getChild(firstStudent, subpartId);
                    if (firstChildLecture == null || firstChildLecture.getAssignment() == null)
                        return null;
                    double firstStudentWeight = firstStudent.getOfferingWeight(firstChildLecture.getConfiguration());
                    List<Lecture> secondChildLectures = secondLecture.getChildren(subpartId);
                    if (secondChildLectures == null || secondChildLectures.isEmpty())
                        return null;
                    List<Move> bestMoves = null;
                    double bestDelta = 0;
                    for (Lecture secondChildLecture : secondChildLectures) {
                        if (secondChildLecture.getAssignment() == null)
                            continue;
                        if (secondChildLecture.nrWeightedStudents() + firstStudentWeight > sEps
                                + secondChildLecture.classLimit())
                            continue;
                        Move m = createMove(firstChildLecture, firstStudent, secondChildLecture, secondStudent);
                        if (m == null)
                            continue;
                        double delta = m.getDelta();
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

        public boolean perform() {
            long conflicts = ((TimetableModel)firstLecture().getModel()).getViolatedStudentConflicts();
            for (Lecture lecture : firstStudent().getLectures()) {
                if (lecture.equals(firstLecture()))
                    continue;
                JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                if (jenrl == null)
                    continue;
                jenrl.decJenrl(firstStudent());
                if (jenrl.getNrStudents() == 0) {
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
                    jenrl.decJenrl(secondStudent());
                    if (jenrl.getNrStudents() == 0) {
                        Object[] vars = jenrl.variables().toArray();
                        for (int j = 0; j < vars.length; j++)
                            jenrl.removeVariable((Lecture) vars[j]);
                        iModel.removeConstraint(jenrl);
                    }
                }
            }

            firstLecture().removeStudent(firstStudent());
            firstStudent().removeLecture(firstLecture());
            secondLecture().addStudent(firstStudent());
            firstStudent().addLecture(secondLecture());
            if (secondStudent() != null) {
                secondLecture().removeStudent(secondStudent());
                secondStudent().removeLecture(secondLecture());
                firstLecture().addStudent(secondStudent());
                secondStudent().addLecture(firstLecture());
            }

            for (Lecture lecture : firstStudent().getLectures()) {
                if (lecture.equals(secondLecture()))
                    continue;
                JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                if (jenrl == null) {
                    jenrl = new JenrlConstraint();
                    iModel.addConstraint(jenrl);
                    jenrl.addVariable(secondLecture());
                    jenrl.addVariable(lecture);
                    // sLogger.debug(getName()+": add jenr {conf="+jenrl.isInConflict()+", lect="+anotherLecture.getName()+", jenr="+jenrl+"}");
                }
                jenrl.incJenrl(firstStudent());
            }
            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (lecture.equals(firstLecture()))
                        continue;
                    JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                    if (jenrl == null) {
                        jenrl = new JenrlConstraint();
                        iModel.addConstraint(jenrl);
                        jenrl.addVariable(lecture);
                        jenrl.addVariable(firstLecture());
                        // sLogger.debug(getName()+": add jenr {conf="+jenrl.isInConflict()+", lect="+anotherLecture.getName()+", jenr="+jenrl+"}");
                    }
                    jenrl.incJenrl(secondStudent());
                }
            }

            if (getChildMoves() != null) {
                for (Move move : getChildMoves()) {
                    move.perform();
                }
            }
            // sLogger.debug("Solution after swap is "+iModel.getInfo()+".");
            return (((TimetableModel)firstLecture().getModel()).getViolatedStudentConflicts() < conflicts);
        }

        public double getDelta() {
            double delta = 0;
            for (Lecture lecture : firstStudent().getLectures()) {
                if (lecture.getAssignment() == null || lecture.equals(firstLecture()))
                    continue;
                JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                if (jenrl == null)
                    continue;
                if (jenrl.isInConflict())
                    delta -= jenrl.getJenrlWeight(firstStudent());
            }
            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (lecture.getAssignment() == null || lecture.equals(secondLecture()))
                        continue;
                    JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                    if (jenrl == null)
                        continue;
                    if (jenrl.isInConflict())
                        delta -= jenrl.getJenrlWeight(secondStudent());
                }
            }

            for (Lecture lecture : firstStudent().getLectures()) {
                if (lecture.getAssignment() == null || lecture.equals(firstLecture()))
                    continue;
                JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                if (jenrl != null) {
                    if (jenrl.isInConflict())
                        delta += jenrl.getJenrlWeight(firstStudent());
                } else {
                    if (JenrlConstraint.isInConflict(secondLecture().getAssignment(), lecture.getAssignment(), iModel.getDistanceMetric()))
                        delta += firstStudent().getJenrlWeight(secondLecture(), lecture);
                }
            }
            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (lecture.getAssignment() == null || lecture.equals(secondLecture()))
                        continue;
                    JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                    if (jenrl != null) {
                        if (jenrl.isInConflict())
                            delta += jenrl.getJenrlWeight(secondStudent());
                    } else {
                        if (JenrlConstraint.isInConflict(firstLecture().getAssignment(), lecture.getAssignment(), iModel.getDistanceMetric()))
                            delta += secondStudent().getJenrlWeight(firstLecture(), lecture);
                    }
                }
            }

            Placement p1 = firstLecture().getAssignment();
            Placement p2 = secondLecture().getAssignment();
            delta += firstStudent().countConflictPlacements(p2) - firstStudent().countConflictPlacements(p1);
            if (secondStudent() != null)
                delta += secondStudent().countConflictPlacements(p1) - secondStudent().countConflictPlacements(p2);

            if (getChildMoves() != null) {
                for (Move move : getChildMoves()) {
                    delta += move.getDelta();
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
                    + ", d=" + getDelta() + ", ch=" + getChildMoves() + "}";

        }

    }

    public MoveBetweenCfgs createMove(Configuration firstConfig, Student firstStudent, Configuration secondConfig,
            Student secondStudent) {
        MoveBetweenCfgs m = new MoveBetweenCfgs(firstConfig, firstStudent, secondConfig, secondStudent);

        for (Enumeration<Long> e = firstConfig.getTopSubpartIds(); e.hasMoreElements();) {
            Long subpartId = e.nextElement();
            if (!addLectures(firstStudent, secondStudent, m.firstLectures(), firstConfig.getTopLectures(subpartId)))
                return null;
        }

        for (Enumeration<Long> e = secondConfig.getTopSubpartIds(); e.hasMoreElements();) {
            Long subpartId = e.nextElement();
            if (!addLectures(secondStudent, firstStudent, m.secondLectures(), secondConfig.getTopLectures(subpartId)))
                return null;
        }

        return m;
    }

    private boolean addLectures(Student student, Student newStudent, Set<Lecture> studentLectures,
            Collection<Lecture> lectures) {
        Lecture lecture = null;
        if (lectures == null)
            return false;

        if (student != null) {
            for (Lecture l : lectures) {
                if (l.students().contains(student)) {
                    lecture = l;
                    break;
                }
            }
        } else {
            int bestValue = 0;
            Lecture bestLecture = null;
            for (Lecture l : lectures) {
                int val = test(newStudent, l);
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
        studentLectures.add(lecture);
        if (lecture.getChildrenSubpartIds() != null) {
            for (Enumeration<Long> e = lecture.getChildrenSubpartIds(); e.hasMoreElements();) {
                Long subpartId = e.nextElement();
                if (!addLectures(student, newStudent, studentLectures, lecture.getChildren(subpartId)))
                    return false;
            }
        }

        return true;
    }

    public int test(Student student, Lecture lecture) {
        if (lecture.getAssignment() == null)
            return -1;
        double studentWeight = student.getOfferingWeight(lecture.getConfiguration());
        if (lecture.nrWeightedStudents() + studentWeight > sEps + lecture.classLimit())
            return -1;
        if (!student.canEnroll(lecture))
            return -1;

        int test = 0;
        for (Lecture x : student.getLectures()) {
            if (x.getAssignment() == null)
                continue;
            if (JenrlConstraint.isInConflict(lecture.getAssignment(), x.getAssignment(), iModel.getDistanceMetric()))
                test++;
        }
        test += student.countConflictPlacements(lecture.getAssignment());

        if (lecture.getChildrenSubpartIds() != null) {
            for (Enumeration<Long> e = lecture.getChildrenSubpartIds(); e.hasMoreElements();) {
                Long subpartId = e.nextElement();
                int bestTest = -1;
                for (Lecture child : lecture.getChildren(subpartId)) {
                    int t = test(student, child);
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

        public boolean perform() {
            long conflicts = ((TimetableModel)firstLectures().iterator().next().getModel()).getViolatedStudentConflicts();
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
                    jenrl.decJenrl(firstStudent());
                    if (jenrl.getNrStudents() == 0) {
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
                        jenrl.decJenrl(secondStudent());
                        if (jenrl.getNrStudents() == 0) {
                            Object[] vars = jenrl.variables().toArray();
                            for (int k = 0; k < vars.length; k++)
                                jenrl.removeVariable((Lecture) vars[k]);
                            iModel.removeConstraint(jenrl);
                        }
                    }
                }
            }

            for (Lecture firstLecture : firstLectures()) {
                firstLecture.removeStudent(firstStudent());
                firstStudent().removeLecture(firstLecture);
                if (secondStudent() != null) {
                    firstLecture.addStudent(secondStudent());
                    secondStudent().addLecture(firstLecture);
                }
            }
            for (Lecture secondLecture : secondLectures()) {
                secondLecture.addStudent(firstStudent());
                firstStudent().addLecture(secondLecture);
                if (secondStudent() != null) {
                    secondLecture.removeStudent(secondStudent());
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
                        iModel.addConstraint(jenrl);
                        jenrl.addVariable(secondLecture);
                        jenrl.addVariable(lecture);
                    }
                    jenrl.incJenrl(firstStudent());
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
                            iModel.addConstraint(jenrl);
                            jenrl.addVariable(firstLecture);
                            jenrl.addVariable(lecture);
                        }
                        jenrl.incJenrl(secondStudent());
                    }
                }
            }
            return (((TimetableModel)firstLectures().iterator().next().getModel()).getViolatedStudentConflicts() < conflicts);
        }

        public double getDelta() {
            double delta = 0;

            for (Lecture lecture : firstStudent().getLectures()) {
                if (lecture.getAssignment() == null)
                    continue;

                for (Lecture firstLecture : firstLectures()) {
                    if (firstLecture.getAssignment() == null || firstLecture.equals(lecture))
                        continue;
                    JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                    if (jenrl == null)
                        continue;
                    if (jenrl.isInConflict())
                        delta -= jenrl.getJenrlWeight(firstStudent());
                }

                for (Lecture secondLecture : secondLectures()) {
                    if (secondLecture.getAssignment() == null || secondLecture.equals(lecture))
                        continue;
                    JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                    if (jenrl != null) {
                        if (jenrl.isInConflict())
                            delta += jenrl.getJenrlWeight(firstStudent());
                    } else {
                        if (JenrlConstraint.isInConflict(secondLecture.getAssignment(), lecture.getAssignment(), iModel.getDistanceMetric()))
                            delta += firstStudent().getJenrlWeight(secondLecture, lecture);
                    }
                }
            }

            if (secondStudent() != null) {
                for (Lecture lecture : secondStudent().getLectures()) {
                    if (lecture.getAssignment() == null)
                        continue;

                    for (Lecture secondLecture : secondLectures()) {
                        if (secondLecture.getAssignment() == null || secondLecture.equals(lecture))
                            continue;
                        JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                        if (jenrl == null)
                            continue;
                        if (jenrl.isInConflict())
                            delta -= jenrl.getJenrlWeight(secondStudent());
                    }

                    for (Lecture firstLecture : firstLectures()) {
                        if (firstLecture.getAssignment() == null || firstLecture.equals(lecture))
                            continue;
                        JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                        if (jenrl != null) {
                            if (jenrl.isInConflict())
                                delta += jenrl.getJenrlWeight(secondStudent());
                        } else {
                            if (JenrlConstraint.isInConflict(firstLecture.getAssignment(), lecture.getAssignment(), iModel.getDistanceMetric()))
                                delta += secondStudent().getJenrlWeight(firstLecture, lecture);
                        }
                    }
                }
            }

            for (Lecture firstLecture : firstLectures()) {
                Placement p1 = firstLecture.getAssignment();
                if (p1 == null)
                    continue;
                delta -= firstStudent().countConflictPlacements(p1);
                if (secondStudent() != null)
                    delta += secondStudent().countConflictPlacements(p1);
            }

            for (Lecture secondLecture : secondLectures()) {
                Placement p2 = secondLecture.getAssignment();
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
                    + "/" + secondConfiguration().getConfigId() + ", d=" + getDelta() + "}";
        }

    }
}
