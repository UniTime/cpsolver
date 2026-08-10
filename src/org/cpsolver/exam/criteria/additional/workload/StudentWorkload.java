package org.cpsolver.exam.criteria.additional.workload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year; 
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.Logger;
import org.cpsolver.exam.criteria.ExamCriterion;
import org.cpsolver.exam.criteria.additional.workload.WorkloadBaseCriterion.WorkloadContext;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamInstructor;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamOwner;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPeriodPlacement;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.exam.model.ExamStudent; 
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
/**
 * Students should not have more than <code>nbrOfExams</code> exams in <code>nbrOfDays</code> 
 * consecutive days.
 * 
 * The goal of this criterion is to minimize student workload during exams.
 * 
 * The number of days <code>nbrOfDays</code> can be set by problem property 
 * Exams.Workload.Students.NbrDays, or in the input xml file, property examsWorkloadStudentsNbrDays.
 * 
 * The number of exams <code>nbrOfExams</code> can be set by problem property Exams.Workload.Students.NbrExams,
 * or in the input xml file, property examsWorkloadStudentsNbrExams.
 * 
 * The weight of the criterion can be set by problem property Exams.Workload.Student.Weight,
 * or in the input xml file, property examsWorkloadStudentsWeight.
 * 
 * @author Alexander Kreim
 * 
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
 *          
 * 
 */
public class StudentWorkload extends WorkloadBaseCriterion {
    
    private static Logger slog = org.apache.logging.log4j.LogManager.getLogger(StudentWorkload.class);
    
    private int nbrOfDays;
    private int nbrOfExams;
    private int nbrOfDaysDefault = 6;
    private int nbrOfExamsDefault = 2;
 
    public StudentWorkload() {
        super();
    }
    
    @Override
    public String getName() {
        return "Student Workload";
    }
        
    @Override
    public String getWeightName() {
        return "Exams.Workload.Student.Weight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "examsWorkloadStudentsWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        setNbrOfDays(properties.getPropertyInt("Exams.Workload.Students.NbrDays", getNbrOfDaysDefault()));
        setNbrOfExams(properties.getPropertyInt("Exams.Workload.Students.NbrExams", getNbrOfExamsDefault()));
    }
    
   
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        // TODO Auto-generated method stub
        super.init(solver);
        configure( solver.getProperties() );
        return true;
    }

    @Override
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
        params.put("examsWorkloadStudentsNbrExams", String.valueOf(getNbrOfExams()));
        params.put("examsWorkloadStudentNbrDays", String.valueOf(getNbrOfDays()));
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        super.setXmlParameters(params);
        try {
            setNbrOfExams(Integer.valueOf(params.get("examsWorkloadStudentsNbrExams")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setNbrOfDays(Integer.valueOf(params.get("examsWorkloadStudentNbrDays")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
    }
       
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "S WL:" + sDoubleFormat.format(getValue(assignment));
    }

    public int getNbrOfDays() {
        if (nbrOfDays == 0) {
            return getNbrOfDaysDefault();
        }
        return nbrOfDays;
    }

    public void setNbrOfDays(int nbrOfDays) {
        this.nbrOfDays = nbrOfDays;
    }

    public int getNbrOfExams() {
        if (nbrOfExams == 0) {
            return getNbrOfExamsDefault();
        }
        return nbrOfExams;
    }

    public void setNbrOfExams(int nbrOfExams) {
        this.nbrOfExams = nbrOfExams;
    }
    
    public int getNbrOfExamsDefault() {
         return nbrOfExamsDefault;
    }
    
    public int getNbrOfDaysDefault() {
        return nbrOfDaysDefault;
    }

    @Override
    protected void setDaysToInkrementWorkloadFromValue(Assignment<Exam, ExamPlacement> assignment,
                                                       ExamPlacement value) {
       
        if (examPeriodsAreNotEmpty() && (value != null) ) {
            
            Exam exam = value.variable();
            List<ExamStudent> examStudents = exam.getStudents();
            WorkloadContext workloadContext = getWorkLoadContext(assignment);
            workloadContext.resetDaysToInkrementWorkload();
            
            for (Iterator<ExamStudent> iterator = examStudents.iterator(); iterator.hasNext();) {
                ExamStudent examStudent = iterator.next();
                WorkloadEntity entity = getWorkloadEntityByStudent(assignment,
                                                                   examStudent);
                
                if (entity != null) {
                    Set<Integer> daysToInkrement = 
                            (workloadContext.getDaysToInkrementWorkload()).get(entity);
                    daysToInkrement.add(value.getPeriod().getDay());    
                }
            }
        }
    }
    
    @Override
    protected List<WorkloadEntity> createWorkLoadEntities() {
        
        if (examPeriodsAreNotEmpty() ) {
            List<WorkloadEntity> entities = new ArrayList<WorkloadEntity>();

            ExamModel model = (ExamModel) getModel();
            List<ExamStudent> students = model.getStudents();

            int nbrOfExamDays = 0;
            if (examPeriodsAreNotEmpty()) {
                nbrOfExamDays = model.getNrDays();
            }

            if (students.size() == 0) {
                slog.debug("The model contains no students");
            }

            for (Iterator<ExamStudent> iterator = students.iterator(); iterator.hasNext();) {
                ExamStudent student = iterator.next();
                WorkloadEntity entity = new WorkloadEntity();
                entity.setName(student.getName());
                entity.setNbrOfDays(getNbrOfDays());
                entity.setNbrOfExams(getNbrOfExams());
                entity.initLoadPerDay(nbrOfExamDays);
                entities.add(entity);

                // slog.debug("Added new workload entity: " + entity.getName());
            }
            slog.info("Student Workload: Added " + entities.size()
                    + " entities. This should happen only once (per solver).");
            
            return entities;
        }
        return null;
    }

    @Override
    protected void setWorkloadFromAssignment(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
        
        if (examPeriodsAreNotEmpty() && (assignment != null)) {

            List<ExamStudent> allExamStudents = ((ExamModel) getModel()).getStudents();
            int nbrOfExamDays = ((ExamModel) getModel()).getNrDays();

            WorkloadContext workloadContext = getWorkLoadContext(assignment);
            List<WorkloadEntity> entities = workloadContext.getEntityList();
            if (entities == null) {
                List<WorkloadEntity> newEntities = createWorkLoadEntities();
                workloadContext.setEntityList(newEntities);
                workloadContext.calcTotalFromAssignment(assignment);
            }

            for (Iterator<ExamStudent> examStudIter = allExamStudents.iterator(); examStudIter.hasNext();) {
                ExamStudent examStudent = examStudIter.next();
                WorkloadEntity entity = getWorkloadEntityByStudent(assignment, examStudent);
                if (entity != null) {
                    entity.resetLoadPerDay(nbrOfExamDays);
                    List<Integer> loadPerDay = entity.getLoadPerDay();
                    for (int dayIndex = 0; dayIndex < nbrOfExamDays; dayIndex++) {
                        Set<Exam> examsADay = examStudent.getExamsADay(assignment, dayIndex);
                        int nbrOfExamsADay = examsADay.size();
                        if (value != null) {
                            Exam examInValue = value.variable();
                            if (examsADay.contains(examInValue)) {
                                nbrOfExamsADay = nbrOfExamsADay - 1;
                            }
                        }
                        loadPerDay.set(dayIndex, nbrOfExamsADay);
                    }
                }
            }
        }
    }

    public WorkloadEntity getWorkloadEntityByStudent(Assignment<Exam, ExamPlacement>  assignment,
                                                     ExamStudent examStudent) {
        
        WorkloadContext workloadContext = getWorkLoadContext(assignment);
        
        List<WorkloadEntity> entityList = workloadContext.getEntityList();
        if (entityList != null) {
            for (Iterator<WorkloadEntity> iterator = entityList.iterator(); iterator.hasNext();) {
                WorkloadEntity entity = iterator.next();
                if (entity != null ) {
                    if (entity.getName().equals(examStudent.getName())) {
                        return entity;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> variables) {
        double penalty = 0.0;
        if ( (getModel() != null) && (assignment != null) ) {
            if (examPeriodsAreNotEmpty()) {
                Set<ExamStudent> examStudents = getExamStudentsFromVariables(variables);
                setWorkloadFromAssignment(assignment, null);
        
                for (Iterator<ExamStudent> iterator = examStudents.iterator(); iterator.hasNext();) {
                    ExamStudent examStudent = iterator.next();
                    WorkloadEntity entity = getWorkloadEntityByStudent(assignment,
                                                                       examStudent);
                    if (entity != null) {
                        penalty += entity.getWorkload();
                    }
                }
            }
            // slog.info("Method getValue(assignment, variables) called: " + String.valueOf(penalty));
        } 
        return penalty;
    }

    private Set<ExamStudent> getExamStudentsFromVariables(Collection<Exam> variables) {
        Set<ExamStudent> examStudents = new HashSet<ExamStudent>();
        for (Iterator<Exam> iterator = variables.iterator(); iterator.hasNext();) {
            Exam exam = iterator.next();
            List<ExamStudent> students = exam.getStudents();
            examStudents.addAll(students);
        }
        return examStudents;
    }
}