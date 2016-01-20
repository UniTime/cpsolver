/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cpsolver.coursett.criteria.additional;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.criteria.TimePreferences;
import org.cpsolver.coursett.criteria.TimetablingCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;

/**
 *  This class represent fairness criterion for instructors
 * @author Rostislav Burget <BurgetRostislav@gmail.com>
 * <br>
 * implemented criterion: Instructor Fairness
 * <br>
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2015 Rostislav Burget <BurgetRostislav@gmail.com><br>
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
public class InstructorFairness extends TimetablingCriterion implements SolutionListener<Lecture, Placement> {
        
    public InstructorFairness() {
        setValueUpdateType(ValueUpdateType.BeforeUnassignedAfterAssigned);
    }

    /*
    * After initialization of criterion initial set of values for instructors 
    * is created and  SolutionListener is created
    */
    @Override
    public boolean init(Solver<Lecture, Placement> solver) {   
        super.init(solver);
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(solver.currentSolution().getAssignment());
        context.initialSetOfBestInstructorVal();
        if (solver.currentSolution().isComplete()){
            context.setCompleteSolutionFound();
        }else{
            solver.currentSolution().addSolutionListener(this);
        }
        return true; 
    }
    
    /*
    * The method check if Complete solution was found
    */
    @Override
    public void solutionUpdated(Solution<Lecture, Placement> solution) {
        if (solution.isComplete()){
            InstructorFairnessContext context = (InstructorFairnessContext)getContext(solution.getAssignment());
            context.setCompleteSolutionFound();
        }
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.InstructorFairnessPreferenceWeight", 1.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.InstructorFairnessPreferenceWeight";
    }
    
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
        if (!context.isActive()) {return ret;}
        if (context.allInstructorsAssigned()&&!(value.variable().getInstructorConstraints().isEmpty())){
            List<InstructorConstraint> insConstraints = value.variable().getInstructorConstraints();
            ret = (context.getDiffInstrValue(insConstraints,context.fairnessDouble(assignment,value)))/insConstraints.size();
            if (conflicts!=null){
                for(Placement conflict:conflicts){
                    if (context.allInstructorsAssigned()&&!(conflict.variable().getInstructorNames().isEmpty())){
                        List<String> instructors2 = conflict.variable().getInstructorNames();
                        ret -= ((context.getDiffInstrValue(instructors2,context.fairnessDouble(assignment,conflict)))/instructors2.size());
                    }
                }
            }
        }
        
        return ret;
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment) {
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
        if (context.allInstructorsAssigned()){
            return context.getObjectiveValue();
        }
        return 0.0;

    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0){
            info.put(getName(), sDoubleFormat.format(getValue(assignment)));
        }
    }
    
     @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        if (getValue(assignment) != 0.0){
            double total = 0;
            for (Lecture lec:variables){
                total+=getValue(assignment,lec.getAssignment(assignment),null);
            }
            info.put(getName(), sDoubleFormat.format(total));
        }
    }
    
    @Override
    public void getExtendedInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        if (iDebug) {
            InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
            String[] fairnessInfo = context.testFairness(assignment);
            info.put("[C] " + getName(), 
                    " Avarage ins pen: " + fairnessInfo[0]+ 
                    ", Sum of squared pen: " + fairnessInfo[1]+ 
                    ", Pmax: " + fairnessInfo[2] +
                    ", Pdev: " + fairnessInfo[3] +
                    ", Perror: " + fairnessInfo[4] +
                    ", Pss: " + fairnessInfo[5] +
                    ", Jain's index: " + fairnessInfo[6] +
                    ", Worst inst pen: " + fairnessInfo[7] +
                    ", Instructor fairness: " + fairnessInfo[8]);    
        }
    }    
    
    @Override
    public void beforeUnassigned(Assignment<Lecture, Placement> assignment, long iteration, Placement value) {
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
        if (context.isFirstIterDone()){
            countInstructorUnassignedFair(assignment,value);
        }else{
            countInstructorFair(assignment);
            countInstructorUnassignedFair(assignment,value);
        }
    }

    @Override
    public void afterAssigned(Assignment<Lecture, Placement> assignment, long iteration, Placement value) {
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
        if (context.isFirstIterDone()){
            countInstructorAssignedFair(assignment,value);
        }else{
            countInstructorFair(assignment);
        }
    }
    
    
    /**
     *  This method set fairness values to all instructors
     * 
     * @param assignment current assignment
     */
    
    
    public void countInstructorFair(Assignment<Lecture, Placement> assignment){
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
        if (context.allInstructorsAssigned()){
            Collection<Lecture> assignedLectures = assignment.assignedVariables();
            context.refreshInstructors();
            for (Lecture lec:assignedLectures){
                if (lec.getInstructorConstraints() !=null){
                    List<InstructorConstraint> insConstraints = lec.getInstructorConstraints();

                    double critValue = 0.0;
                    //critValue += lec.getModel().getCriterion(RoomPreferences.class).getWeightedValue(assignment, lec.getAssignment(assignment),null);
                    critValue += lec.getModel().getCriterion(TimePreferences.class).getWeightedValue(assignment, lec.getAssignment(assignment),null);

                    for(InstructorConstraint ic:insConstraints){
                        context.addInstructorValue(ic.getResourceId(), critValue);
                    }
                }
            }
            context.countInstrMeanFairValue();
            context.setFirstIterDone();
        }
    }


    /**
     *  Method actualize values of instructors whose lecture was just assigned
     * 
     * @param assignment
     * @param value
     */
    
    public void countInstructorAssignedFair(Assignment<Lecture, Placement> assignment, Placement value){
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
        Lecture lec = value.variable();
        if (lec.getInstructorConstraints() !=null){
            List<InstructorConstraint> insConstraints = lec.getInstructorConstraints();
            
            double critValue = 0.0;
            //critValue += lec.getModel().getCriterion(RoomPreferences.class).getWeightedValue(assignment, value,null);
            critValue += lec.getModel().getCriterion(TimePreferences.class).getWeightedValue(assignment, value,null);
            
            for(InstructorConstraint ic:insConstraints){
                if (!context.addInstructorValue(ic.getResourceId(), critValue)){
                    throw new IllegalArgumentException("Couldnt add value to instructor (instructor is not added in context)"); 
                }
            }
        }
        context.countInstrMeanFairValue();  
    }


    /**
     *  Method actualize values of instructors whose lecture will be unassigned
     * 
     * @param assignment
     * @param value
     */
    
    public void countInstructorUnassignedFair(Assignment<Lecture, Placement> assignment, Placement value){
        InstructorFairnessContext context = (InstructorFairnessContext)getContext(assignment);
        Lecture lec = value.variable();
        if (lec.getInstructorConstraints() !=null){
            List<InstructorConstraint> insConstraints = lec.getInstructorConstraints();
            
            double critValue = 0.0;
            //critValue += lec.getModel().getCriterion(RoomPreferences.class).getWeightedValue(assignment, value,null);
            critValue += lec.getModel().getCriterion(TimePreferences.class).getWeightedValue(assignment, value,null);
          
            for(InstructorConstraint ic:insConstraints){
                if (!context.decreaseInstructorValue(ic.getResourceId(), critValue)){
                    throw new IllegalArgumentException("Couldnt decrease value of instructor (instructor is not added in context)"); 
                }
            }
        }
        context.countInstrMeanFairValue();
    }

    @Override
    public void getInfo(Solution<Lecture, Placement> solution, Map<String, String> info) {
    }

    @Override
    public void getInfo(Solution<Lecture, Placement> solution, Map<String, String> info, Collection<Lecture> variables) {
    }

    @Override
    public void bestCleared(Solution<Lecture, Placement> solution) {
    }

    @Override
    public void bestSaved(Solution<Lecture, Placement> solution) {
    }

    @Override
    public void bestRestored(Solution<Lecture, Placement> solution) {
    }
    
    /**
     *  Context for InstructorFairness
     * 
     * @param <V>
     * @param <T>
     */
    public class InstructorFairnessContext<V extends Variable<V, T>, T extends Value<V, T>> extends ValueContext{

        /**
         *   
         * @param assignment current assignment
         * @param weight weight of InstructorFairness
         */
        public InstructorFairnessContext(Assignment<Lecture, Placement> assignment,double weight) {
            initialSetOfBestInstructorVal();
            if (assignment.nrUnassignedVariables(getModel())==0){
                setCompleteSolutionFound();
            }
            this.weight = weight;
        }

        private Solution<V, T> bestSolution;
        private TreeMap<Long,Instructor> stringInstMap = new TreeMap<Long,Instructor>();


        DecimalFormat df = new DecimalFormat("####0.00");
        private double[] instructorsValue;
        private boolean active = false;
        private double instrMeanFairValue = 0.0;
        private boolean completeSolutionFound = false;
        private boolean fullTreeMap = false;
        private boolean firstIterDone = false;
        private double weight;

        public void setCompleteSolutionFound() {
            this.completeSolutionFound = true;
        }

        public double getInstrMeanFairValue() {
            return instrMeanFairValue;
        }

        public boolean isFirstIterDone() {
            return firstIterDone;
        }

        public void setFirstIterDone() {
            this.firstIterDone = true;
        }

        public Solution<V, T> getBestSolution() {
            return bestSolution;
        }

        public int getNumOfIstructors(){
            return stringInstMap.size();
        }

        public Collection<Instructor> getInstructorsWithAssig() {
            return stringInstMap.values();
        }

        public void setBestSolution(Solution<V, T> bestSolution) {
            this.bestSolution = bestSolution;
        }

        public void activate() {
            active = true;
        }

        public void deActivate() {
            active = false;
        }

        public boolean isActive() {
            return active;
        }

        public double[] getInstructorsValue(){
            return instructorsValue;
        }  


        /**
         * Set best values for all instructors 
         * 
         */
        
        public void initialSetOfBestInstructorVal(){
            deActivate();
            if (getModel().getEmptyAssignment().unassignedVariables(getModel()).iterator().hasNext() &&
                    getModel().getEmptyAssignment().unassignedVariables(getModel()).iterator().next() instanceof Lecture){

                getInstructors((Collection<Lecture>) getModel().getEmptyAssignment().unassignedVariables(getModel()));
                for (Lecture lecture : (Collection<Lecture>) getModel().getEmptyAssignment().unassignedVariables(getModel())) {
                    double bestPossibleValue = 1000.0;

                    for (Placement t : lecture.computeValues((Assignment<Lecture, Placement>) getModel().getEmptyAssignment(),true)) {
                        if (fairnessDouble((Assignment<Lecture, Placement>) getModel().getEmptyAssignment(),t) < bestPossibleValue) {
                            bestPossibleValue = fairnessDouble((Assignment<Lecture, Placement>) getModel().getEmptyAssignment(),t);
                        }
                    }

                    for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
                        Instructor s = stringInstMap.get(ic.getResourceId());
                        s.addBestValue(bestPossibleValue);
                    }
                    
                }
            }  
        activate();            
        }

        /**
         * add all instructors in variables to TreeMap<Long,Instructor> stringInstMap
         * 
         * @param variables Collection of Lectures 
         */
        
        public void getInstructors(Collection<Lecture> variables) {
            for (Lecture lecture : variables) {
                for (InstructorConstraint ic: lecture.getInstructorConstraints()){
                    stringInstMap.put(ic.getResourceId(), new Instructor(ic.getResourceId()));
                }
            }
        }

        public boolean allInstructorsAssigned(){
            if (!fullTreeMap) {
                fullTreeMap = completeSolutionFound;
            }
            return fullTreeMap;
        }

        /**
         * adding value to instructor in stringInstMap
         *
         * @param insID
         * @param value
         * @return false if instructor not in map
         */
        
        public boolean addInstructorValue(Long insID,double value){
            if (stringInstMap.get(insID)!=null){
                Instructor s = stringInstMap.get(insID);
                s.addValue(value);
                s.incNumOfClasses();
                stringInstMap.put(insID, s);
                return true;
            }else{
                return false;
            }
        }


        /**
         * adding value to instructor in stringInstMap
         * 
         * @param insID
         * @param value
         * @return
         */
        
        public boolean decreaseInstructorValue(Long insID,double value){
            if (stringInstMap.get(insID)!=null){
                Instructor s = stringInstMap.get(insID);
                s.removeValue(value);
                s.decNumOfClasses();
                stringInstMap.put(insID, s);
                return true;
            }else{
                return false;
            }
        }

        /**
         *  compute and return mean fair value of instructors in stringInstMap
         */
        
        public void countInstrMeanFairValue(){
            if (stringInstMap.size()!=0){
                double sum=0.0;
                for(Map.Entry<Long,Instructor> entry : stringInstMap.entrySet()) {
                    Instructor ins = entry.getValue();
                    sum += ins.getFinalValue();
                }
                instrMeanFairValue = sum/stringInstMap.size();
            }
        }

                
        /**
         * Method compute and return difference between value of instructor and mean 
         *   fair value for all instrutors in entry list
         * 
         * @param instructorsList
         * @param placementValue
         * @return
         */
        
        
        public double getDiffInstrValue(List<InstructorConstraint> instructorsList,double placementValue){
            double ret = 0.0;
            for (InstructorConstraint ic:instructorsList){
                if (stringInstMap.get(ic.getResourceId()) != null){
                    Instructor i = stringInstMap.get(ic.getResourceId());
                    if (i.getFinalValue()> instrMeanFairValue) {
                        ret +=((i.getFinalValue()-instrMeanFairValue)/i.getNumOfClasses())+(placementValue-instrMeanFairValue);
                    }else{
                        ret -=((instrMeanFairValue-i.getFinalValue())/i.getNumOfClasses())+(instrMeanFairValue-placementValue);
                    }
                }
            }
            return ret;
        }


        /**
         * fairness value based on pdev (pdev sec. part)
         * 
         * @return 
         */
        
        public double getObjectiveValue(){
            double ret = 0.0;
            for(Map.Entry<Long,Instructor> entry : stringInstMap.entrySet()) {
                Instructor ins = entry.getValue();
                ret += Math.abs(ins.getFinalValue() - instrMeanFairValue);
            }
            return ret;
        }


        /**
         * fairness value with squared P and avg.P 
         * 
         * @return
         */
        
        public double getDiffAllInstrValueSquared(){
            double ret = 0.0;
            for(Map.Entry<Long,Instructor> entry : stringInstMap.entrySet()) {
                Instructor ins = entry.getValue();
                ret += Math.sqrt(Math.abs(ins.getFinalValue()*ins.getFinalValue() - instrMeanFairValue*instrMeanFairValue));
            }
            return ret;
        }


        /**
         * refresh of all instructors in stringInstMap
         *
         */
                
        public void refreshInstructors(){
            for(Map.Entry<Long,Instructor> entry : stringInstMap.entrySet()) {
                Instructor ins = entry.getValue();
                ins.refresh();
            }
        }

        /**
         * Metod count and return time preferences in placement
         *
         * @param assignment
         * @param placement
         * @return
         */
        
        public double fairnessDouble(Assignment<Lecture, Placement> assignment, Placement placement){
            double critValue = 0.0;
                //critValue += variable().getModel().getCriterion(RoomPreferences.class).getWeightedValue(assignment,placement ,null);
                critValue += placement.variable().getModel().getCriterion(TimePreferences.class).getWeightedValue(assignment,placement ,null);
            return critValue;
        }

        /**
         *  Method for whole evaluation of fairness criteria
         * 
         * @param assignment
         * @return String[] with informations about solution [0-Avarage instructor penalty,
         *   1-Sum of squared penalities, 2-Pmax, 3-Pdev, 4-Perror, 5-Pss, 6-Jain,
         *   7-worst instructor fairness value,8-Instructors fairness value]
         */
        
        public String[] testFairness(Assignment<Lecture, Placement> assignment){
            String[] DataForEval = new String[9]; 
            Collection<Lecture> assignedLectures = assignment.assignedVariables();
            refreshInstructors();
            for (Lecture lec:assignedLectures){
                if (lec.getInstructorConstraints() !=null){
                    List<InstructorConstraint> insConstraints = lec.getInstructorConstraints();
                    double critValue = 0.0;

                    //critValue += lec.getModel().getCriterion(RoomPreferences.class).getWeightedValue(assignment, lec.getAssignment(assignment),null);
                    critValue += lec.getModel().getCriterion(TimePreferences.class).getWeightedValue(assignment, lec.getAssignment(assignment),null);

                    for(InstructorConstraint ic:insConstraints){
                        addInstructorValue(ic.getResourceId(), critValue);
                    }
                }
            }
            countInstrMeanFairValue();
            DataForEval[8] = df.format(getObjectiveValue());

            instructorsValue = new double[stringInstMap.values().size()];
            int counter = 0;
            double sumOfSquaredPen = 0.0;
            double min = 100000;
            double max = -100000;
            double sum = 0.0;
            double pdevSecPart = 0.0;
            double pssSecPart = 0.0;

            for (Map.Entry<Long, Instructor> entry : stringInstMap.entrySet()) {
                Long insID = entry.getKey();
                Instructor s = entry.getValue();
                instructorsValue[counter] = s.getFinalValue();
                sumOfSquaredPen = sumOfSquaredPen + (s.getFinalValue() * s.getFinalValue());
                if (min > s.getFinalValue()) {
                    min = s.getFinalValue();
                }
                if (max < s.getFinalValue()) {
                    max = s.getFinalValue();
                }
                sum += s.getFinalValue();            
                counter++;
            }
            Arrays.sort(instructorsValue);

            for (double d : instructorsValue) {
                pdevSecPart = pdevSecPart + Math.abs(d - sum / instructorsValue.length);
                pssSecPart = pssSecPart + d * d;
            }

            // Worst instructor penalty:
            DataForEval[7] = df.format(max);
            // "Avarage instructor penalty:
            DataForEval[0] = df.format(sum / instructorsValue.length);
            // Sum of squared penalities:
            DataForEval[1] = df.format(sumOfSquaredPen);
            // Fairness W1:
            // Pmax
            DataForEval[2] = df.format(getBestSolution().getModel().getTotalValue(getBestSolution().getAssignment())- pdevSecPart*weight + (instructorsValue.length) * max);
            // Pdev
            DataForEval[3] = df.format(getBestSolution().getModel().getTotalValue(getBestSolution().getAssignment()));
            // PError
            DataForEval[4] = df.format(getBestSolution().getModel().getTotalValue(getBestSolution().getAssignment())- pdevSecPart*weight + sum + ((instructorsValue.length) * (max - min)));
            //  Pss: 
            DataForEval[5] = df.format(Math.sqrt(((getBestSolution().getModel().getTotalValue(getBestSolution().getAssignment())-pdevSecPart*weight) * (getBestSolution().getModel().getTotalValue(getBestSolution().getAssignment()))-pdevSecPart*weight) + pssSecPart));

            if (sumOfSquaredPen != 0.0) {
            //  Jain's index:
                DataForEval[6] = df.format((sum * sum) / ((instructorsValue.length) * sumOfSquaredPen));
            } else {
                DataForEval[6] = df.format(1);
            }
        return DataForEval;
        }

        /**
         * Class representing instructor 
         * 
         */
        public class Instructor {

            private Long insID;
            private Double value = 0.0;
            private Double bestValue = 0.0;
            private int numOfClasses = 0;

            //could be used to change how important instructors requiremets are
            private double coef = 1;

            /**
             *
             * @param insID
             */
            public Instructor(Long insID) {
                this.insID = insID;
            }

            /**
             * reprezentation how well instructors requirements are met
             * 
             * @return
             */
            
            public Double getFinalValue() {
                if (numOfClasses == 0){
                    return 0.0;
                }
                return ((value - bestValue) / numOfClasses)*coef;
            }

            public Long getInsResId() {
                return insID;
            }

            public void setInsResId(Long insID) {
                this.insID = insID;
            }

            public Double getValue() {
                return value;
            }

            public void addValue(Double value) {
                this.value += value;
            }

            public void removeValue(Double value) {
                this.value -= value;
            }

            public Double getBestValue() {
                return bestValue;
            }

            public void addBestValue(Double bestValue) {
                this.bestValue += bestValue;
            }

            public int getNumOfClasses() {
                return numOfClasses;
            }

            public void incNumOfClasses() {
                this.numOfClasses += 1;
            }

            public void decNumOfClasses() {
                this.numOfClasses -= 1;
            }

            public double getCoef() {
                return coef;
            }

            public void setCoef(double coef) {
                this.coef = coef;
            }

            public void refresh() {
                value = 0.0;
                numOfClasses = 0;
            }

            @Override
            public int hashCode() {
                int hash = 3;
                hash = 71 * hash + (this.insID != null ? this.insID.hashCode() : 0);
                return hash;
            }


            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final Instructor other = (Instructor) obj;
                return !((this.insID == null) ? (other.insID != null) : !this.insID.equals(other.insID));
            }

        }
    }
    
    @Override
    public ValueContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new InstructorFairnessContext(assignment,getWeight());
    }
}