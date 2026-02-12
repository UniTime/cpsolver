package org.cpsolver.exam.criteria.additional.workload;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.cpsolver.coursett.criteria.additional.InstructorFairness.InstructorFairnessContext;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.exam.criteria.ExamCriterion;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion.ValueContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solver.Solver; 

/**
 * Base class for implementing a workload criterion.
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
 */
public abstract class WorkloadBaseCriterion extends ExamCriterion {

    private static Logger slog = org.apache.logging.log4j.LogManager.getLogger(WorkloadBaseCriterion.class);
  
    public WorkloadBaseCriterion() {
        super();
        setValueUpdateType(ValueUpdateType.AfterUnassignedBeforeAssigned);
    }
         
    @Override
    public void setModel(Model<Exam, ExamPlacement> model) {
        super.setModel(model);
    }

    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        boolean retval = super.init(solver);
        return retval;
    }
              
    @Override
    public abstract double getValue(Assignment<Exam, ExamPlacement> assignment,
                                    Collection<Exam> variables);

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment) {
        WorkloadContext workLoadContext= getWorkLoadContext(assignment);
        double totalValue = workLoadContext.calcTotalFromAssignment(assignment);
        // slog.info("Method getValue(assignment) called: " + String.valueOf(totalValue));
        return totalValue;
    }
       
    /**
     * Tests if there are exam periods
     * @return
     */
    protected boolean examPeriodsAreNotEmpty() {
        ExamModel examMpdel = (ExamModel)this.getModel();
        List<ExamPeriod> examPeriods = examMpdel.getPeriods();
        if ( (examPeriods != null ) &&  ( examPeriods.size() > 0 )  ) {
            return true;
        }
        return false;
    }
        
    /**
     * Calculates changes in the criterion value.
     * <br>
     * Step 1: Calculate workload for all entities before the assignment 
     * of the placement <code>value</code>. 
     * <br> 
     * Step 2: Calculate the workload for all entities including the 
     * placement <code>value</code>.
     * <br>
     * Step 3: Return the difference
     * <br>
     * see {@link org.cpsolver.ifs.criteria.AbstractCriterion}
     */
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, 
                           ExamPlacement value,
                           Set<ExamPlacement> conflicts) {
        
        double penalty = 0.0;
        
        // if (assignment.assignedValues().contains(value)) {
        //     slog.warn("Value is in assignment.");
        // }
        
        if ( examPeriodsAreNotEmpty() ) {
            
            WorkloadContext workloadContext  = getWorkLoadContext(assignment);
            
            setWorkloadFromAssignment(assignment, value);
            setDaysToInkrementWorkloadFromValue(assignment, value);
            
            penalty = workloadContext.calcPenalty();
            
            workloadContext.resetDaysToInkrementWorkload();
            
            // slog.info("Workload Criterion (penalty/value before): (" + penalty + "/" + workloadContext.getTotal() + ")" );
        }
        
        return penalty;
    }
    
    @Override
    public double[] getBounds(Assignment<Exam, ExamPlacement> assignment, 
                              Collection<Exam> exams) {
        double[] bounds = new double[] { 0.0, 0.0 };
        return bounds;
    } 
    
    /**
     * Set the entities and days to increment the workload.
     */
    protected abstract void setDaysToInkrementWorkloadFromValue(
            Assignment<Exam, ExamPlacement> assignment, 
            ExamPlacement value);
      
    /**
     * Sets the workload of all entities.
     * @param assignment
     */
    protected abstract void setWorkloadFromAssignment(
            Assignment<Exam, ExamPlacement> assignment, 
            ExamPlacement value);
       
    /**
     * Creates workload entities.
     * @return
     */
    protected abstract List<WorkloadEntity> createWorkLoadEntities();
    
    /**
     * String representation of all workloads
     * @return Lines containing <code>Name,number of exams first day,...,workload value</code> 
     */
    public String toCSVString(Assignment<Exam, ExamPlacement> assignment) {
        String retval = "";
        List<WorkloadEntity> entities = getWorkLoadContext(assignment).getEntityList();
        if (entities != null) {
            for (Iterator<WorkloadEntity> iterator = entities.iterator(); iterator.hasNext();) {
                WorkloadEntity workloadEntity = iterator.next();
                if (workloadEntity.getLoadPerDay() != null) {
                    retval += workloadEntity.toCSVString() + "\n";
                }
            }
        }
        return retval;
    }
           
    @Override
    public ValueContext createAssignmentContext(
            Assignment<Exam, ExamPlacement> assignment) {
        return new  WorkloadContext(assignment);
    }
    
    protected WorkloadContext getWorkLoadContext(
            Assignment<Exam, ExamPlacement> assignment) {
        return (WorkloadContext) getContext(assignment);
    }
    
    /*
     *  Assignment related attributes
     */
    public class WorkloadContext extends ValueContext {
        
        private List<WorkloadEntity> entityList;
        private Map<WorkloadEntity, Set<Integer>> daysToInkrementWorkload;
        
        protected WorkloadContext() {}
        
        public WorkloadContext(Assignment<Exam, ExamPlacement> assignment) {}
        
        protected Map<WorkloadEntity, Set<Integer>> getDaysToInkrementWorkload() {
            return daysToInkrementWorkload;
        }
        
        /**
         * Getter attribute entityList
         */
        protected List<WorkloadEntity> getEntityList() {
            return entityList;
        }

        /**
         * Setter attribute entityList
         */
        protected void setEntityList(List<WorkloadEntity> entityList) {
            this.entityList = entityList;
        }
        
        /**
         * Marks days on which the workload is to be incremented.
         * @param entity
         * @param daysToInkrement
         */
        protected void setDaysToInkrementWorkloadForEntity(WorkloadEntity entity, Set<Integer> daysToInkrement) {
           if ( (this.getDaysToInkrementWorkload() == null) || (this.getDaysToInkrementWorkload().size() == 0) ) {
               this.initDaysToIncrementWorkload();
           }
           this.daysToInkrementWorkload.put(entity, daysToInkrement); 
        } 
        
        protected void resetDaysToInkrementWorkload() {
            
            if ( (this.getDaysToInkrementWorkload() != null) && (this.getDaysToInkrementWorkload().size() > 0) ) {
                for (Iterator<WorkloadEntity> iterator =
                        this.getDaysToInkrementWorkload().keySet().iterator(); iterator.hasNext();) {
                    
                    WorkloadEntity entity = iterator.next();
                    Set<Integer> daysToInkrement = this.getDaysToInkrementWorkload().get(entity);
                    if (daysToInkrement == null) {
                        this.getDaysToInkrementWorkload().put(entity, new HashSet<Integer>());
                    } else {
                        daysToInkrement.clear();
                    }
                }
            } else {
                initDaysToIncrementWorkload();
            }
        }
                
        private void initDaysToIncrementWorkload() {
            this.daysToInkrementWorkload = new HashMap<WorkloadEntity, Set<Integer>>();
            if (entityList != null) {
                for (Iterator<WorkloadEntity> iterator = entityList.iterator(); iterator.hasNext();) {
                    WorkloadEntity workloadEntity = iterator.next();
                    this.daysToInkrementWorkload.put(workloadEntity, new HashSet<Integer>());
                }
            }
        }
        
        public double calcTotalFromAssignment(Assignment<Exam, ExamPlacement> assignment) {
            double retval = 0.0;
            if (WorkloadBaseCriterion.this.examPeriodsAreNotEmpty()) {

                if ((getEntityList() == null) || (getEntityList().size() == 0)) {
                    List<WorkloadEntity> newEntities = WorkloadBaseCriterion.this.createWorkLoadEntities();
                    if (newEntities != null) {
                        this.setEntityList(newEntities);
                    }
                }

                if (getEntityList() != null) {
                    setWorkloadFromAssignment(assignment, null);

                    for (Iterator<WorkloadEntity> iterator = entityList.iterator(); iterator.hasNext();) {
                        WorkloadEntity workloadEntity = iterator.next();
                        retval += workloadEntity.getWorkload();
                    }

                }
            }
            // this.setTotal(retval);
            return retval;
        }
        
        /**
         * Calculates the value change in the criterion
         * @return
         */
        protected double calcPenalty() {
            double penalty = 0.0;
            if (this.entityList != null) {  
                Iterator<WorkloadEntity> entityIter = this.getEntityList().iterator();
                while(entityIter.hasNext()) {
                    WorkloadEntity entity = entityIter.next(); 
                    Set<Integer> daysToInkrement = this.getDaysToInkrementWorkload().get(entity);
                    if (daysToInkrement != null ) { 
                        penalty += calcPenaltyForEntity(entity, daysToInkrement);
                    }
                }
            }
            return penalty;
        }
        
        /**
         * Calculates the criterion's value change for a single workload entity
         * @param entity
         * @param daysToIncrement
         * @return
         */
        protected int calcPenaltyForEntity(WorkloadEntity entity, Set<Integer> daysToIncrement) {
            int penaltyBeforeAssignment = entity.getWorkload() ;

//            String debugIncrements = "";
//            if (daysToIncrement.size() > 0) {
//                slog.debug( entity.toCSVString() );
//                debugIncrements = "Days to increment: ";
//                for (Iterator<Integer> iterator = daysToIncrement.iterator(); iterator.hasNext();) {
//                    Integer dayIndex = iterator.next();
//                    debugIncrements += dayIndex.toString() + ", ";
//                }
//            }
            
            for (Iterator<Integer> dayIter = daysToIncrement.iterator(); dayIter.hasNext();) {
                int dayIndex = dayIter.next();
                entity.incrementItemWorkloadForDay(dayIndex);
            }
     
            int penaltyAfterAssignment = entity.getWorkload();
            int penalty = penaltyAfterAssignment - penaltyBeforeAssignment;
            
//            if (daysToIncrement.size() > 0) {
//                debugIncrements += " Penalty: " + String.valueOf(penalty);
//                slog.debug( debugIncrements );  
//                slog.debug( entity.toCSVString() );
//            }
            
            return penalty;       
        }
    }
}
