package net.sf.cpsolver.coursett.model;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.FastVector;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

/** 
 * Student sectioning (after a solution is found).
 * <br><br>
 * In the current implementation, students are not re-sectioned during the
 * search, but a student re-sectioning algorithm is called after the solver is finished
 * or upon the user’s request. The re-sectioning is based on a local search algorithm
 * where the neighboring assignment is obtained from the current assignment by
 * applying one of the following moves:<ul>
 * <li>Two students enrolled in the same course swap all of their class assignments.
 * <li>A student is re-enrolled into classes associated with a course such that the
 * number of conflicts involving that student is minimized.
 * </ul>
 * The solver maintains a queue, initially containing all courses with multiple
 * classes. During each iteration, an improving move (i.e., a move decreasing the
 * overall number of student conflicts) is applied once discovered. Re-sectioning is
 * complete once no more improving moves are possible. Only consistent moves
 * (i.e., moves that respect class limits and other constraints) are considered. Any
 * additional courses having student conflicts after a move is accepted are added
 * to the queue.
 * <br>
 * Since students are not re-sectioned during the timetabling search, the computed
 * number of student conflicts is really an upper bound on the actual number
 * that may exist afterward. To compensate for this during the search, student conflicts
 * between subparts with multiple classes are weighted lower than conflicts
 * between classes that meet at a single time (i.e., having student conflicts that
 * cannot be avoided by re-sectioning).
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

public class FinalSectioning implements Runnable {
	private TimetableModel iModel = null;
	private Progress iProgress = null;
	public static double sEps = 0.0001;
	
	public FinalSectioning(TimetableModel model) {
		iModel = model;
		iProgress = Progress.getInstance(iModel);
	}
	
	public void run() {
		iProgress.setStatus("Student Sectioning...");
        Collection variables = iModel.variables();
        while (!variables.isEmpty()) {
        	//sLogger.debug("Shifting students ...");
            iProgress.setPhase("moving students ...",iModel.variables().size());
            HashSet lecturesToRecompute = new HashSet(variables.size());
            
            for (Iterator i=variables.iterator();i.hasNext();) {
                Lecture lecture = (Lecture)i.next();
                if (lecture.getParent()==null) {
                	Configuration cfg = lecture.getConfiguration();
                	if (cfg!=null && cfg.getAltConfigurations().size()>1)
                		findAndPerformMoves(cfg, lecturesToRecompute);
                }
                //sLogger.debug("Shifting students for "+lecture);
                findAndPerformMoves(lecture,lecturesToRecompute);
                //sLogger.debug("Lectures to recompute: "+lects);
                iProgress.incProgress();
            }
            //sLogger.debug("Shifting done, "+getViolatedStudentConflictsCounter().get()+" conflicts.");
            variables = lecturesToRecompute;
        }
	}
    
    /** Perform sectioning on the given lecture
     * @param lecture given lecture
     * @param recursive recursively resection lectures affected by a student swap
     * @param configAsWell resection students between configurations as well
     **/
    public void resection(Lecture lecture, boolean recursive, boolean configAsWell) {
        HashSet variables = new HashSet();
        findAndPerformMoves(lecture, variables);
        if (configAsWell) {
            Configuration cfg = lecture.getConfiguration();
            if (cfg!=null && cfg.getAltConfigurations().size()>1)
                findAndPerformMoves(cfg, variables);
        }
        if (recursive) {
            while (!variables.isEmpty()) {
                HashSet lecturesToRecompute = new HashSet();
                for (Iterator i=variables.iterator();i.hasNext();) {
                    Lecture l = (Lecture)i.next();
                    if (configAsWell && l.getParent()==null) {
                        Configuration cfg = l.getConfiguration();
                        if (cfg!=null && cfg.getAltConfigurations().size()>1)
                            findAndPerformMoves(cfg, lecturesToRecompute);
                    }
                    findAndPerformMoves(l,lecturesToRecompute);
                }
                variables = lecturesToRecompute;
            }
        }
    }
	
    /** Swap students between this and the same lectures (lectures which differ only in the section) */
    public void findAndPerformMoves(Lecture lecture, HashSet lecturesToRecompute) {
    	if (lecture.sameStudentsLectures()==null || lecture.getAssignment()==null) return;
    	
    	if (lecture.getClassLimitConstraint()!=null) {
        	while (lecture.nrWeightedStudents()>sEps+lecture.minClassLimit()) {
        		Move m = findAwayMove(lecture);
        		if (m==null) break;
    			lecturesToRecompute.add(m.secondLecture());
   				m.perform();
        	}
    	}
    	
    	Set conflictStudents = lecture.conflictStudents();
        if (conflictStudents==null || conflictStudents.isEmpty()) return;
        //sLogger.debug("  conflicts:"+conflictStudents.size()+"/"+conflictStudents);
        //sLogger.debug("Solution before swap is "+iModel.getInfo()+".");
        if (lecture.sameStudentsLectures().size()>1) {
        	for (Iterator i1=conflictStudents.iterator(); i1.hasNext();) {
	        	Student student = (Student)i1.next();
	        	if (lecture.getAssignment()==null) continue;
        		Move m = findMove(lecture,student);
        		if (m!=null) {
        			lecturesToRecompute.add(m.secondLecture());
       				m.perform();
        		}
        	}
        } else {
        	for (Iterator i1=conflictStudents.iterator(); i1.hasNext();) {
	        	Student student = (Student)i1.next();
	            for (Enumeration i2=lecture.conflictLectures(student).elements(); i2.hasMoreElements();) {
	                Lecture anotherLecture = (Lecture)i2.nextElement();
	                if (anotherLecture.equals(lecture) || anotherLecture.sameStudentsLectures()==null || anotherLecture.getAssignment()==null || anotherLecture.sameStudentsLectures().size()<=1) continue;
	                lecturesToRecompute.add(anotherLecture);
	            }
        	}
        }
    }

    public void findAndPerformMoves(Configuration configuration, HashSet lecturesToRecompute) {
    	for (Iterator i=configuration.students().iterator();i.hasNext();) {
    		Student student = (Student)i.next();
    		if (!configuration.hasConflict(student)) continue;
    		
    		MoveBetweenCfgs m = findMove(configuration, student);
    		
    		if (m!=null) {
    			lecturesToRecompute.addAll(m.secondLectures());
   				m.perform();
    		}
    	}
    }
    
	public Move findAwayMove(Lecture lecture) {
        Vector bestMoves=null;
        double bestDelta=0;
		for (Iterator i1=lecture.students().iterator();i1.hasNext();) {
			Student student = (Student)i1.next();
	        for (Enumeration i2=lecture.sameStudentsLectures().elements();i2.hasMoreElements();) {
	            Lecture sameLecture = (Lecture)i2.nextElement();
	            double studentWeight = student.getOfferingWeight(sameLecture.getConfiguration());
	            if (!student.canEnroll(sameLecture)) continue;
	            if (sameLecture.equals(lecture) || sameLecture.getAssignment()==null) continue;
	            if (sameLecture.nrWeightedStudents()+studentWeight<=sEps+sameLecture.classLimit()) {
	            	Move m = createMove(lecture,student,sameLecture,null);
	            	if (m==null || m.isTabu()) continue;
	            	double delta = m.getDelta();
	            	if (delta<bestDelta) {
	                    if (bestMoves==null)
	                    	bestMoves=new FastVector();
	                    else
	                        bestMoves.clear();
	                    bestMoves.addElement(m);
	                    bestDelta=delta;
	                } else if (delta==bestDelta) {
	                    if (bestMoves==null)
	                    	bestMoves=new FastVector();
	                    bestMoves.addElement(m);
	                }                	
	            }
	        }
		}
        if (bestDelta<-sEps && bestMoves!=null) {
            Move m = (Move)ToolBox.random(bestMoves);
            return m;
        }
        return null;
	}

	public Move findMove(Lecture lecture, Student student) {
        double bestDelta=0;
        Vector bestMoves=null;
        double studentWeight = student.getOfferingWeight(lecture.getConfiguration());
        for (Enumeration i1=lecture.sameStudentsLectures().elements();i1.hasMoreElements();) {
            Lecture sameLecture = (Lecture)i1.nextElement();
            if (!student.canEnroll(sameLecture)) continue;
            if (sameLecture.equals(lecture) || sameLecture.getAssignment()==null) continue;
            if (sameLecture.nrWeightedStudents()+studentWeight<=sEps+sameLecture.classLimit()) {
            	Move m = createMove(lecture,student,sameLecture,null);
            	if (m==null || m.isTabu()) continue;
            	double delta = m.getDelta();
            	if (delta<bestDelta) {
                    if (bestMoves==null)
                    	bestMoves=new FastVector();
                    else
                        bestMoves.clear();
                    bestMoves.addElement(m);
                    bestDelta=delta;
                } else if (delta==bestDelta) {
                    if (bestMoves==null)
                    	bestMoves=new FastVector();
                    bestMoves.addElement(m);
                }                	
            }
            for (Iterator i2=sameLecture.students().iterator();i2.hasNext();) {
                Student anotherStudent = (Student)i2.next();
                double anotherStudentWeight = anotherStudent.getOfferingWeight(lecture.getConfiguration());
                if (!anotherStudent.canEnroll(lecture)) continue;
                if (anotherStudentWeight!=studentWeight) {
                	if (sameLecture.nrWeightedStudents()-anotherStudentWeight+studentWeight>sEps+sameLecture.classLimit()) continue;
                	if (lecture.nrWeightedStudents()-studentWeight+anotherStudentWeight>sEps+lecture.classLimit()) continue;
                }
                if (bestDelta<-sEps && bestMoves!=null && bestMoves.size()>10) break;
                Move m = createMove(lecture,student,sameLecture,anotherStudent);
                if (m==null || m.isTabu()) continue;
                double delta = m.getDelta();
            	if (delta<bestDelta) {
                    if (bestMoves==null)
                    	bestMoves=new FastVector();
                    else
                        bestMoves.clear();
                    bestMoves.addElement(m);
                    bestDelta=delta;
                } else if (delta==bestDelta) {
                    if (bestMoves==null)
                    	bestMoves=new FastVector();
                    bestMoves.addElement(m);
                }                	
            }
            if (Math.abs(bestDelta)<sEps && bestMoves!=null && bestMoves.size()>10) break;
        }
        if (bestDelta<-sEps && bestMoves!=null) return (Move)ToolBox.random(bestMoves);
        return null;
	}
	
	public MoveBetweenCfgs findMove(Configuration config, Student student) {
        double bestDelta=0;
        Vector bestMoves=null;
        for (Enumeration i1=config.getAltConfigurations().elements();i1.hasMoreElements();) {
        	Configuration altConfig = (Configuration) i1.nextElement();
        	if (altConfig.equals(config)) continue;
        	
        	MoveBetweenCfgs m = createMove(config, student, altConfig, null);
        	if (m!=null && !m.isTabu()) {
        		double delta = m.getDelta();
            	if (delta<bestDelta) {
                    if (bestMoves==null)
                    	bestMoves=new FastVector();
                    else
                        bestMoves.clear();
                    bestMoves.addElement(m);
                    bestDelta=delta;
                } else if (delta==bestDelta) {
                    if (bestMoves==null)
                    	bestMoves=new FastVector();
                    bestMoves.addElement(m);
                }                	
            }
        	
        	for (Iterator i2=config.students().iterator();i2.hasNext();) {
        		Student anotherStudent = (Student)i2.next();
        		if (bestDelta<-sEps && bestMoves!=null && bestMoves.size()>10) break;
                m = createMove(config,student,altConfig,anotherStudent);
                if (m!=null && !m.isTabu()) {
                	double delta = m.getDelta();
                	if (delta<bestDelta) {
                		if (bestMoves==null)
                			bestMoves=new FastVector();
                		else
                			bestMoves.clear();
                		bestMoves.addElement(m);
                		bestDelta=delta;
                	} else if (delta==bestDelta) {
                		if (bestMoves==null)
                			bestMoves=new FastVector();
                		bestMoves.addElement(m);
                	}
                }
        	}
            if (Math.abs(bestDelta)<sEps && bestMoves!=null && bestMoves.size()>10) break;
        }
        if (bestDelta<-sEps && bestMoves!=null) return (MoveBetweenCfgs)ToolBox.random(bestMoves);
        return null;
	}	
	
	public Move createMove(Lecture firstLecture, Student firstStudent, Lecture secondLecture, Student secondStudent) {
		if (!firstStudent.canEnroll(secondLecture)) return null;
		if (secondStudent!=null && !secondStudent.canEnroll(firstLecture)) return null;
        if (firstLecture.getParent()!=null && secondLecture.getParent()==null) return null;
        if (firstLecture.getParent()==null && secondLecture.getParent()!=null) return null;
		
		Move move = new Move(firstLecture, firstStudent, secondLecture, secondStudent);
		if (firstLecture.hasAnyChildren()!=secondLecture.hasAnyChildren()) return null;
		if (firstLecture.hasAnyChildren()) {
			if (secondStudent!=null) {
				for (Enumeration e=firstLecture.getChildrenSubpartIds();e.hasMoreElements();) {
					Long subpartId = (Long)e.nextElement();
					Lecture firstChildLecture = firstLecture.getChild(firstStudent, subpartId);
					Lecture secondChildLecture = secondLecture.getChild(secondStudent, subpartId);
                    if (firstChildLecture==null || secondChildLecture==null) return null;
					double firstStudentWeight = firstStudent.getOfferingWeight(firstChildLecture.getConfiguration());
					double secondStudentWeight = secondStudent.getOfferingWeight(secondChildLecture.getConfiguration());
					if (firstStudentWeight!=secondStudentWeight) {
						if (firstChildLecture.nrWeightedStudents()-firstStudentWeight+secondStudentWeight>sEps+firstChildLecture.classLimit()) return null;
						if (secondChildLecture.nrWeightedStudents()-secondStudentWeight+firstStudentWeight>sEps+secondChildLecture.classLimit()) return null;
					}
					if (firstChildLecture!=null && firstChildLecture.getAssignment()!=null && secondChildLecture!=null && secondChildLecture.getAssignment()!=null) {
						Move m = createMove(firstChildLecture,firstStudent,secondChildLecture,secondStudent);
						if (m==null) return null;
						move.addChildMove(m);
					} else
						return null;
				}
			} else {
				for (Enumeration e1=firstLecture.getChildrenSubpartIds();e1.hasMoreElements();) {
					Long subpartId = (Long)e1.nextElement();
					Lecture firstChildLecture = firstLecture.getChild(firstStudent, subpartId);
					double firstStudentWeight = firstStudent.getOfferingWeight(firstChildLecture.getConfiguration());
					if (firstChildLecture==null || firstChildLecture.getAssignment()==null) return null;
					Vector secondChildLectures = secondLecture.getChildren(subpartId);
					if (secondChildLectures==null || secondChildLectures.isEmpty()) return null;
					Vector bestMoves=null;
					double bestDelta=0;
					for (Enumeration e2=secondChildLectures.elements();e2.hasMoreElements();) {
						Lecture secondChildLecture = (Lecture)e2.nextElement();
						if (secondChildLecture.getAssignment()==null) continue;
						if (secondChildLecture.nrWeightedStudents()+firstStudentWeight>sEps+secondChildLecture.classLimit()) continue;
						Move m = createMove(firstChildLecture,firstStudent,secondChildLecture,secondStudent);
						if (m==null) continue;
						double delta = m.getDelta();
						if (bestMoves==null || delta<bestDelta) {
							if (bestMoves==null)
								bestMoves=new FastVector();
							else
								bestMoves.clear();
							bestMoves.addElement(m);
							bestDelta=delta;
						} else if (delta==bestDelta) {
							if (bestMoves==null)
								bestMoves=new FastVector();
							bestMoves.addElement(m);
						}                	
					}
					if (bestDelta>=0 || bestMoves==null) return null;
					Move m = (Move)ToolBox.random(bestMoves);
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
		Vector iChildMoves = null; 
		
		private Move(Lecture firstLecture, Student firstStudent, Lecture secondLecture, Student secondStudent) {
			iFirstLecture = firstLecture;
			iFirstStudent = firstStudent;
			iSecondLecture = secondLecture;
			iSecondStudent = secondStudent;
		}
		
		public Lecture firstLecture() { return iFirstLecture; }
		public Student firstStudent() { return iFirstStudent; }
		public Lecture secondLecture() { return iSecondLecture; }
		public Student secondStudent() { return iSecondStudent; }
		
		public void addChildMove(Move move) {
			if (iChildMoves==null)
				iChildMoves = new FastVector();
			iChildMoves.addElement(move);
		}
		public Vector getChildMoves() { return iChildMoves; }
		
		public void perform() {
	        for (Iterator i=firstStudent().getLectures().iterator();i.hasNext();) {
	        	Lecture lecture = (Lecture)i.next();
	        	if (lecture.equals(firstLecture())) continue;
	        	JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                if (jenrl==null) continue;
	        	jenrl.decJenrl(firstStudent());
	        	if (jenrl.getNrStudents()==0) {
	        		Object[] vars = jenrl.variables().toArray();
	                for (int j=0;j<vars.length;j++)
	                	jenrl.removeVariable((Variable)vars[j]);
	        		iModel.removeConstraint(jenrl);
	            }
	        }
	        if (secondStudent()!=null) {
		        for (Iterator i=secondStudent().getLectures().iterator();i.hasNext();) {
		        	Lecture lecture = (Lecture)i.next();
		        	if (lecture.equals(secondLecture())) continue;
		        	JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
		        	if (jenrl==null) continue;
		        	jenrl.decJenrl(secondStudent());
		        	if (jenrl.getNrStudents()==0) {
		        		Object[] vars = jenrl.variables().toArray();
		                for (int j=0;j<vars.length;j++)
		                	jenrl.removeVariable((Variable)vars[j]);
		        		iModel.removeConstraint(jenrl);
		            }
		        }
	        }
	        
        	firstLecture().removeStudent(firstStudent());
	        firstStudent().removeLecture(firstLecture());
	        secondLecture().addStudent(firstStudent());
        	firstStudent().addLecture(secondLecture());
	        if (secondStudent()!=null) {
	        	secondLecture().removeStudent(secondStudent());
	        	secondStudent().removeLecture(secondLecture());
	        	firstLecture().addStudent(secondStudent());
	        	secondStudent().addLecture(firstLecture());
	        }
	        
	        for (Iterator i=firstStudent().getLectures().iterator();i.hasNext();) {
	        	Lecture lecture = (Lecture)i.next();
	        	if (lecture.equals(secondLecture())) continue;
	        	JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
	        	if (jenrl==null) {
        			jenrl = new JenrlConstraint();
	        		iModel.addConstraint(jenrl);
        			jenrl.addVariable(secondLecture());
        			jenrl.addVariable(lecture);
        			//sLogger.debug(getName()+": add jenr {conf="+jenrl.isInConflict()+", lect="+anotherLecture.getName()+", jenr="+jenrl+"}");
	        	}
	        	jenrl.incJenrl(firstStudent());
	        }
	        if (secondStudent()!=null) {
		        for (Iterator i=secondStudent().getLectures().iterator();i.hasNext();) {
		        	Lecture lecture = (Lecture)i.next();
		        	if (lecture.equals(firstLecture())) continue;
		        	JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
		        	if (jenrl==null) {
	        			jenrl = new JenrlConstraint();
		        		iModel.addConstraint(jenrl);
	        			jenrl.addVariable(lecture);
	        			jenrl.addVariable(firstLecture());
	        			//sLogger.debug(getName()+": add jenr {conf="+jenrl.isInConflict()+", lect="+anotherLecture.getName()+", jenr="+jenrl+"}");
		        	}
		        	jenrl.incJenrl(secondStudent());
		        }
	        }
	        
	        if (getChildMoves()!=null) {
	        	for (Enumeration e=getChildMoves().elements();e.hasMoreElements();) {
	        		((Move)e.nextElement()).perform();
	        	}
	        }
	        //sLogger.debug("Solution after swap is "+iModel.getInfo()+".");
		}

		public double getDelta() {
			double delta = 0;
			for (Iterator i=firstStudent().getLectures().iterator();i.hasNext();) {
	        	Lecture lecture = (Lecture)i.next();
	        	if (lecture.getAssignment()==null || lecture.equals(firstLecture())) continue;
	        	JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
                if (jenrl==null) continue;
	        	if (jenrl.isInConflict()) delta-=jenrl.getJenrlWeight(firstStudent());
			}
	        if (secondStudent()!=null) {
		        for (Iterator i=secondStudent().getLectures().iterator();i.hasNext();) {
		        	Lecture lecture = (Lecture)i.next();
		        	if (lecture.getAssignment()==null || lecture.equals(secondLecture())) continue;
		        	JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
                    if (jenrl==null) continue;
		        	if (jenrl.isInConflict()) delta-=jenrl.getJenrlWeight(secondStudent());
		        }
	        }
	        
	        for (Iterator i=firstStudent().getLectures().iterator();i.hasNext();) {
	        	Lecture lecture = (Lecture)i.next();
	        	if (lecture.getAssignment()==null || lecture.equals(firstLecture())) continue;
	        	JenrlConstraint jenrl = secondLecture().jenrlConstraint(lecture);
	        	if (jenrl!=null) {
	        		if (jenrl.isInConflict()) delta+=jenrl.getJenrlWeight(firstStudent());
	        	} else {
	        		if (JenrlConstraint.isInConflict((Placement)secondLecture().getAssignment(),(Placement)lecture.getAssignment())) 
	        			delta+=firstStudent().getJenrlWeight(secondLecture(), lecture);
	        	}
	        }
	        if (secondStudent()!=null) {
		        for (Iterator i=secondStudent().getLectures().iterator();i.hasNext();) {
		        	Lecture lecture = (Lecture)i.next();
		        	if (lecture.getAssignment()==null || lecture.equals(secondLecture())) continue;
		        	JenrlConstraint jenrl = firstLecture().jenrlConstraint(lecture);
		        	if (jenrl!=null) {
		        		if (jenrl.isInConflict()) delta+=jenrl.getJenrlWeight(secondStudent());
		        	} else {
		        		if (JenrlConstraint.isInConflict((Placement)firstLecture().getAssignment(),(Placement)lecture.getAssignment())) 
		        			delta+=secondStudent().getJenrlWeight(firstLecture(),lecture);
		        	}
		        }
	        }
	        
			Placement p1 = (Placement)firstLecture().getAssignment();
			Placement p2 = (Placement)secondLecture().getAssignment();
			delta += firstStudent().countConflictPlacements(p2) - firstStudent().countConflictPlacements(p1);
	        if (secondStudent()!=null)
	        	delta += secondStudent().countConflictPlacements(p1) - secondStudent().countConflictPlacements(p2);
	        
	        if (getChildMoves()!=null) {
	        	for (Enumeration e=getChildMoves().elements();e.hasMoreElements();) {
	        		delta += ((Move)e.nextElement()).getDelta();
	        	}
	        }
			return delta;
		}
		
		public boolean isTabu() { 
			return false;
		}
		
		public String toString() {
			return "Move{"+firstStudent()+"/"+firstLecture()+" <-> "+secondStudent()+"/"+secondLecture()+", d="+getDelta()+", ch="+getChildMoves()+"}";
			
		}
		
	}
	
	public  MoveBetweenCfgs createMove(Configuration firstConfig, Student firstStudent, Configuration secondConfig, Student secondStudent) {
		MoveBetweenCfgs m = new MoveBetweenCfgs(firstConfig, firstStudent, secondConfig, secondStudent);
		
		for (Enumeration e=firstConfig.getTopSubpartIds();e.hasMoreElements();) {
			Long subpartId = (Long)e.nextElement();
			if (!addLectures(firstStudent, secondStudent, m.firstLectures(), firstConfig.getTopLectures(subpartId))) return null;
		}
		
		for (Enumeration e=secondConfig.getTopSubpartIds();e.hasMoreElements();) {
			Long subpartId = (Long)e.nextElement();
			if (!addLectures(secondStudent, firstStudent, m.secondLectures(), secondConfig.getTopLectures(subpartId))) return null;
		}
		
		return m;
	}
	
	private boolean addLectures(Student student, Student newStudent, Set studentLectures, Collection lectures) {
		Lecture lecture = null;
		if (lectures==null) return false;

		if (student!=null) {
			for (Iterator i=lectures.iterator();i.hasNext();) {
				Lecture l = (Lecture)i.next();
				if (l.students().contains(student)) {
					lecture = l; break;
				}
			}
		} else {
			int bestValue=0;
			Lecture bestLecture = null; 
			for (Iterator i=lectures.iterator();i.hasNext();) {
				Lecture l = (Lecture)i.next();
				int val = test(newStudent,l);
				if (val<0) continue;
				if (bestLecture==null || bestValue>val) {
					bestValue = val; bestLecture = l;
				}
			}
			lecture = bestLecture;
		}

		if (lecture==null) return false;
		if (newStudent!=null && !newStudent.canEnroll(lecture)) return false;
		studentLectures.add(lecture);
		if (lecture.getChildrenSubpartIds()!=null) {
			for (Enumeration e=lecture.getChildrenSubpartIds();e.hasMoreElements();) {
				Long subpartId = (Long)e.nextElement();
				if (!addLectures(student, newStudent, studentLectures, lecture.getChildren(subpartId))) return false;
			}
		}	

		return true;
	}
	
	public int test(Student student, Lecture lecture) {
		if (lecture.getAssignment()==null) return -1;
		double studentWeight = student.getOfferingWeight(lecture.getConfiguration());
		if (lecture.nrWeightedStudents()+studentWeight>sEps+lecture.classLimit()) return -1;
		if (!student.canEnroll(lecture)) return -1;
		
		int test = 0; 
		for (Iterator i=student.getLectures().iterator();i.hasNext();) {
			Lecture x = (Lecture)i.next();
			if (x.getAssignment()==null) continue;
			if (JenrlConstraint.isInConflict((Placement)lecture.getAssignment(),(Placement)x.getAssignment())) test++;
		}
		test += student.countConflictPlacements((Placement)lecture.getAssignment());
		
		if (lecture.getChildrenSubpartIds()!=null) {
			for (Enumeration e=lecture.getChildrenSubpartIds();e.hasMoreElements();) {
				Long subpartId = (Long)e.nextElement();
				int bestTest = -1;
				for (Enumeration f=lecture.getChildren(subpartId).elements();f.hasMoreElements();) {
					Lecture child = (Lecture)f.nextElement();
					int t = test(student, child);
					if (t<0) continue;
					if (bestTest<0 || bestTest>t)
						bestTest = t;
				}
				if (bestTest<0) return -1;
				test += bestTest;
			}
		}
		return test;
	}
	
	public class MoveBetweenCfgs {
		Configuration iFirstConfig = null;
		Set iFirstLectures = new HashSet();
		Student iFirstStudent = null;
		Configuration iSecondConfig = null;
		Set iSecondLectures = new HashSet();
		Student iSecondStudent = null;
		
		public MoveBetweenCfgs(Configuration firstConfig, Student firstStudent, Configuration secondConfig, Student secondStudent) {
			iFirstConfig = firstConfig;
			iFirstStudent = firstStudent;
			iSecondConfig = secondConfig;
			iSecondStudent = secondStudent;
		}
		
		public Configuration firstConfiguration() { return iFirstConfig; }
		public Student firstStudent() { return iFirstStudent; }
		public Set firstLectures() { return iFirstLectures; }
		public Configuration secondConfiguration() { return iSecondConfig; }
		public Student secondStudent() { return iSecondStudent; }
		public Set secondLectures() { return iSecondLectures; }
		
		public void perform() {
			firstStudent().removeConfiguration(firstConfiguration());
			firstStudent().addConfiguration(secondConfiguration());
			for (Iterator i=firstStudent().getLectures().iterator();i.hasNext();) {
	        	Lecture lecture = (Lecture)i.next();
	        	
	        	for (Iterator j=firstLectures().iterator();j.hasNext();) {
	        		Lecture firstLecture = (Lecture)j.next();
	        		if (firstLecture.equals(lecture)) continue;
	        		if (firstLectures().contains(lecture) && firstLecture.getClassId().compareTo(lecture.getClassId())>0) continue;
	        		
		        	JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                    if (jenrl==null) continue;
		        	jenrl.decJenrl(firstStudent());
		        	if (jenrl.getNrStudents()==0) {
		        		Object[] vars = jenrl.variables().toArray();
		                for (int k=0;k<vars.length;k++)
		                	jenrl.removeVariable((Variable)vars[k]);
		        		iModel.removeConstraint(jenrl);
		            }
	        	}
			}
			
			if (secondStudent()!=null) {
				secondStudent().removeConfiguration(secondConfiguration());
				secondStudent().addConfiguration(firstConfiguration());
				for (Iterator i=secondStudent().getLectures().iterator();i.hasNext();) {
		        	Lecture lecture = (Lecture)i.next();
		        	
		        	for (Iterator j=secondLectures().iterator();j.hasNext();) {
		        		Lecture secondLecture = (Lecture)j.next();
		        		if (secondLecture.equals(lecture)) continue;
		        		if (secondLectures().contains(lecture) && secondLecture.getClassId().compareTo(lecture.getClassId())>0) continue;
		        		
			        	JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                        if (jenrl==null) continue;
			        	jenrl.decJenrl(secondStudent());
			        	if (jenrl.getNrStudents()==0) {
			        		Object[] vars = jenrl.variables().toArray();
			                for (int k=0;k<vars.length;k++)
			                	jenrl.removeVariable((Variable)vars[k]);
			        		iModel.removeConstraint(jenrl);
			            }
		        	}
				}
			}
			
        	for (Iterator i=firstLectures().iterator();i.hasNext();) {
        		Lecture firstLecture = (Lecture)i.next();
        		firstLecture.removeStudent(firstStudent());
        		firstStudent().removeLecture(firstLecture);
        		if (secondStudent()!=null) {
    	        	firstLecture.addStudent(secondStudent());
    	        	secondStudent().addLecture(firstLecture);
        		}
        	}
        	for (Iterator i=secondLectures().iterator();i.hasNext();) {
        		Lecture secondLecture = (Lecture)i.next();
    	        secondLecture.addStudent(firstStudent());
            	firstStudent().addLecture(secondLecture);
    	        if (secondStudent()!=null) {
    	        	secondLecture.removeStudent(secondStudent());
    	        	secondStudent().removeLecture(secondLecture);
    	        }
        	}
        	
			for (Iterator i=firstStudent().getLectures().iterator();i.hasNext();) {
	        	Lecture lecture = (Lecture)i.next();
	        	
	        	for (Iterator j=secondLectures().iterator();j.hasNext();) {
	        		Lecture secondLecture = (Lecture)j.next();
	        		if (secondLecture.equals(lecture)) continue;
	        		if (secondLectures().contains(lecture) && secondLecture.getClassId().compareTo(lecture.getClassId())>0) continue;
	        		
		        	JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
		        	if (jenrl==null) {
	        			jenrl = new JenrlConstraint();
		        		iModel.addConstraint(jenrl);
	        			jenrl.addVariable(secondLecture);
	        			jenrl.addVariable(lecture);
		        	}
		        	jenrl.incJenrl(firstStudent());
	        	}
			}
			
			if (secondStudent()!=null) {
				for (Iterator i=secondStudent().getLectures().iterator();i.hasNext();) {
		        	Lecture lecture = (Lecture)i.next();
		        	
		        	for (Iterator j=firstLectures().iterator();j.hasNext();) {
		        		Lecture firstLecture = (Lecture)j.next();
		        		if (firstLecture.equals(lecture)) continue;
		        		if (firstLectures().contains(lecture) && firstLecture.getClassId().compareTo(lecture.getClassId())>0) continue;
		        		
			        	JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
			        	if (jenrl==null) {
		        			jenrl = new JenrlConstraint();
			        		iModel.addConstraint(jenrl);
		        			jenrl.addVariable(firstLecture);
		        			jenrl.addVariable(lecture);
			        	}
			        	jenrl.incJenrl(secondStudent());
		        	}
				}
			}
		}
		
		public double getDelta() {
			double delta = 0;
			
			for (Iterator i=firstStudent().getLectures().iterator();i.hasNext();) {
	        	Lecture lecture = (Lecture)i.next();
	        	if (lecture.getAssignment()==null) continue;
	        	
	        	for (Iterator j=firstLectures().iterator();j.hasNext();) {
	        		Lecture firstLecture = (Lecture)j.next();
	        		if (firstLecture.getAssignment()==null || firstLecture.equals(lecture)) continue;
	        		JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
                    if (jenrl==null) continue;
		        	if (jenrl.isInConflict()) delta-=jenrl.getJenrlWeight(firstStudent());
				}
	        	
	        	for (Iterator j=secondLectures().iterator();j.hasNext();) {
	        		Lecture secondLecture = (Lecture)j.next();
	        		if (secondLecture.getAssignment()==null || secondLecture.equals(lecture)) continue;
	        		JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
		        	if (jenrl!=null) {
		        		if (jenrl.isInConflict()) delta+=jenrl.getJenrlWeight(firstStudent());;
		        	} else {
		        		if (JenrlConstraint.isInConflict((Placement)secondLecture.getAssignment(),(Placement)lecture.getAssignment())) 
		        			delta+=firstStudent().getJenrlWeight(secondLecture, lecture);
		        	}
				}
			}
			
			if (secondStudent()!=null) {
				for (Iterator i=secondStudent().getLectures().iterator();i.hasNext();) {
		        	Lecture lecture = (Lecture)i.next();
		        	if (lecture.getAssignment()==null) continue;

		        	for (Iterator j=secondLectures().iterator();j.hasNext();) {
		        		Lecture secondLecture = (Lecture)j.next();
		        		if (secondLecture.getAssignment()==null || secondLecture.equals(lecture)) continue;
		        		JenrlConstraint jenrl = secondLecture.jenrlConstraint(lecture);
                        if (jenrl==null) continue;
			        	if (jenrl.isInConflict()) delta-=jenrl.getJenrlWeight(secondStudent());
					}
		        	
		        	for (Iterator j=firstLectures().iterator();j.hasNext();) {
		        		Lecture firstLecture = (Lecture)j.next();
		        		if (firstLecture.getAssignment()==null || firstLecture.equals(lecture)) continue;
		        		JenrlConstraint jenrl = firstLecture.jenrlConstraint(lecture);
			        	if (jenrl!=null) {
			        		if (jenrl.isInConflict()) delta+=jenrl.getJenrlWeight(secondStudent());
			        	} else {
			        		if (JenrlConstraint.isInConflict((Placement)firstLecture.getAssignment(),(Placement)lecture.getAssignment())) 
			        			delta+=secondStudent().getJenrlWeight(firstLecture, lecture);
			        	}
					}
				}
			}
			
        	for (Iterator j=firstLectures().iterator();j.hasNext();) {
        		Lecture firstLecture = (Lecture)j.next();
    			Placement p1 = (Placement)firstLecture.getAssignment();
    			if (p1==null) continue;
    			delta -= firstStudent().countConflictPlacements(p1);
    	        if (secondStudent()!=null) delta += secondStudent().countConflictPlacements(p1);
        	}			
			
        	for (Iterator j=secondLectures().iterator();j.hasNext();) {
        		Lecture secondLecture = (Lecture)j.next();
    			Placement p2 = (Placement)secondLecture.getAssignment();
    			if (p2==null) continue;
    			delta += firstStudent().countConflictPlacements(p2);
    	        if (secondStudent()!=null) delta -= secondStudent().countConflictPlacements(p2);
        	}

        	return delta;
		}
		
		public boolean isTabu() {
			return false;
		}
		
		public String toString() {
			return "Move{"+firstStudent()+"/"+firstConfiguration().getConfigId()+" <-> "+secondStudent()+"/"+secondConfiguration().getConfigId()+", d="+getDelta()+"}";
		}

		
	}
}
