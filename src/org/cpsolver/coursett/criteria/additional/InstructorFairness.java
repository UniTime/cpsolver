package org.cpsolver.coursett.criteria.additional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import org.cpsolver.ifs.util.DataProperties;

/**
 * This class represent fairness criterion for instructors.
 *  Criterion iteratively evaluate instructors fairness, based on absolute 
 *  deviation of the individual satisfaction of instructors time (or time and 
 *  room) requirements.
 * @author Rostislav Burget<br>
 *         implemented criterion: Instructor Fairness <br>
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2015 Rostislav Burget<br>
 *          <a href="mailto:BurgetRostislav@gmail.com">BurgetRostislav@gmail.com</a><br>
 *          <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 *          <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 *          <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/
 *          </a>.
 */
public class InstructorFairness extends TimetablingCriterion {

    /**
     *
     */
    public InstructorFairness() {
        setValueUpdateType(ValueUpdateType.BeforeUnassignedAfterAssigned);
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
    public void bestSaved(Assignment<Lecture, Placement> assignment) {
        iBest = getValue(assignment);
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        InstructorFairnessContext context = (InstructorFairnessContext) getContext(assignment);
        if (context.allInstructorsAssigned(assignment) && !value.variable().getInstructorConstraints().isEmpty()) {
            List<InstructorConstraint> insConstraints = value.variable().getInstructorConstraints();
            ret = (context.getDiffInstrValue(insConstraints, context.fairnessDouble(assignment, value))) / insConstraints.size();
            if (conflicts != null) {
                for (Placement conflict : conflicts) {
                    if (!conflict.variable().getInstructorConstraints().isEmpty()) {
                        List<InstructorConstraint> insConstraints2 = conflict.variable().getInstructorConstraints();
                        ret -= (context.getDiffInstrValue(insConstraints2, context.fairnessDouble(assignment, conflict))) / insConstraints2.size();
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment) {
        InstructorFairnessContext context = (InstructorFairnessContext) getContext(assignment);
        if (context.allInstructorsAssigned(assignment))
            return context.getObjectiveValue();
        return 0.0;
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        InstructorFairnessContext context = (InstructorFairnessContext) getContext(assignment);
        if (context.allInstructorsAssigned(assignment)) {
            Set<InstructorConstraint> constraints = new HashSet<InstructorConstraint>();
            for (Lecture lecture : variables) {
                constraints.addAll(lecture.getInstructorConstraints());
            }
            return context.getObjectiveValue(constraints);
        }
        return 0.0;
    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        double value = getValue(assignment);
        if (value != 0.0)
            info.put(getName(), sDoubleFormat.format(value));
    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        double value = getValue(assignment, variables);
        if (value != 0.0)
            info.put(getName(), sDoubleFormat.format(value));
    }

    @Override
    public void getExtendedInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        if (iDebug) {
            InstructorFairnessContext context = (InstructorFairnessContext) getContext(assignment);
            String[] fairnessInfo = context.testFairness(assignment);
            info.put(getName() + " Details",
                    fairnessInfo[8] + " (avg: " + fairnessInfo[0] + ", rms: " + fairnessInfo[1] +
                    ", Pmax: " + fairnessInfo[2] + ", Pdev: " + fairnessInfo[3] + ", Perror: " + fairnessInfo[4] +
                    ", Pss: " + fairnessInfo[5] + ", Jain's index: " + fairnessInfo[6] + ", max: " + fairnessInfo[7] + ")");
        }
    }

    /**
     * Context for InstructorFairness
     */
    public class InstructorFairnessContext extends ValueContext {
        private TreeMap<Long, Instructor> iId2Instructor = new TreeMap<Long, Instructor>();
        private double iInstrMeanFairValue = 0.0;
        private boolean iFullTreeMap = false;
        private boolean iFirstIterDone = false;

        /**
         * 
         * @param assignment
         *            current assignment
         */
        public InstructorFairnessContext(Assignment<Lecture, Placement> assignment) {
            countInstructorFair(assignment);
        }

        @Override
        protected void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            if (isFirstIterDone()) {
                countInstructorAssignedFair(assignment, value);
            } else {
                countInstructorFair(assignment);
            }
        }

        @Override
        protected void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
            if (isFirstIterDone()) {
                countInstructorUnassignedFair(assignment, value);
            } else {
                if (countInstructorFair(assignment))
                    countInstructorUnassignedFair(assignment, value);
            }
        }

        /**
         * This method set fairness values to all instructors
         * 
         * @param assignment current assignment
         * @return false if complete solution wasn't found
         */
        public boolean countInstructorFair(Assignment<Lecture, Placement> assignment) {
            if (allInstructorsAssigned(assignment)) {
                iId2Instructor.clear();
                for (Lecture lecture : getModel().variables()) {
                    Double bestPossibleValue = null;

                    for (Placement t : lecture.values(assignment)) {
                        double f = fairnessDouble(assignment, t);
                        if (bestPossibleValue == null || f < bestPossibleValue)
                            bestPossibleValue = f;
                    }

                    Placement placement = assignment.getValue(lecture);
                    for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
                        Instructor s = iId2Instructor.get(ic.getResourceId());
                        if (s == null) {
                            s = new Instructor(ic.getResourceId());
                            iId2Instructor.put(ic.getResourceId(), s);
                        }
                        if (bestPossibleValue != null)
                            s.addBestValue(bestPossibleValue);
                        if (placement != null) {
                            s.addValue(fairnessDouble(assignment, placement));
                            s.incNumOfClasses();
                        }
                    }
                }
                countInstrMeanFairValue();
                setFirstIterDone();
                return true;
            } else {
                return false;
            }
        }

        /**
         * Method actualize values of instructors whose lecture was just
         * assigned
         * 
         * @param assignment current assignment 
         * @param value placement of lecture
         */

        public void countInstructorAssignedFair(Assignment<Lecture, Placement> assignment, Placement value) {
            Lecture lec = value.variable();
            if (lec.getInstructorConstraints() != null) {
                List<InstructorConstraint> insConstraints = lec.getInstructorConstraints();
                double critValue = fairnessDouble(assignment, lec.getAssignment(assignment));
                for (InstructorConstraint ic : insConstraints) {
                    if (!addInstructorValue(ic.getResourceId(), critValue)) {
                        throw new IllegalArgumentException("Instructor " + ic.getResourceId() + " is not present in the context.");
                    }
                }
            }
            countInstrMeanFairValue();
        }

        /**
         * Method actualize values of instructors whose lecture will be
         * unassigned
         * 
         * @param assignment current assignment
         * @param value placement of lecture
         */

        public void countInstructorUnassignedFair(Assignment<Lecture, Placement> assignment, Placement value) {
            Lecture lec = value.variable();
            if (lec.getInstructorConstraints() != null) {
                List<InstructorConstraint> insConstraints = lec.getInstructorConstraints();
                double critValue = fairnessDouble(assignment, lec.getAssignment(assignment));
                for (InstructorConstraint ic : insConstraints) {
                    if (!decreaseInstructorValue(ic.getResourceId(), critValue)) {
                        throw new IllegalArgumentException("Instructor " + ic.getResourceId() + " is not present in the context.");
                    }
                }
            }
            countInstrMeanFairValue();
        }

        /**
         *
         * @return instructor mean fair value 
         */
        public double getInstrMeanFairValue() {
            return iInstrMeanFairValue;
        }

        /**
         *
         * @return if first iteration was done
         */
        public boolean isFirstIterDone() {
            return iFirstIterDone;
        }

        /**
         * set first iteration done to true
         */
        public void setFirstIterDone() {
            this.iFirstIterDone = true;
        }

        /**
         *
         * @return number of instructors
         */
        public int getNumOfIstructors() {
            return iId2Instructor.size();
        }

        /**
         *
         * @return Collection of instructors in context
         */
        public Collection<Instructor> getInstructorsWithAssig() {
            return iId2Instructor.values();
        }

        /**
         * Return if complete solution (all variables assigned) was found 
         * in this context
         * @param assignment current assignment
         * @return true if in this context were all variables assigned 
         * false otherwise
         */
        public boolean allInstructorsAssigned(Assignment<Lecture, Placement> assignment) {
            if (!iFullTreeMap) {
                iFullTreeMap = (assignment.nrAssignedVariables() > 0 && assignment.nrUnassignedVariables(getModel()) == 0 && getModel().getBestUnassignedVariables() == 0);
            }
            return iFullTreeMap;
        }

        /**
         * adding value to instructor in stringInstMap
         *
         * @param insID instructor ID
         * @param value that should be added
         * @return false if instructor is not in iId2Instructor
         */

        public boolean addInstructorValue(Long insID, double value) {
            Instructor s = iId2Instructor.get(insID);
            if (s != null) {
                s.addValue(value);
                s.incNumOfClasses();
                return true;
            } else {
                return false;
            }
        }

        /**
         * adding value to instructor in stringInstMap
         * 
         * @param insID instructor ID
         * @param value value that should be subtracted
         * @return false if instructor is not iId2Instructor
         */

        public boolean decreaseInstructorValue(Long insID, double value) {
            Instructor s = iId2Instructor.get(insID);
            if (s != null) {
                s.removeValue(value);
                s.decNumOfClasses();
                return true;
            } else {
                return false;
            }
        }

        /**
         * compute and return mean fair value of instructors in iId2Instructor
         */

        public void countInstrMeanFairValue() {
            if (!iId2Instructor.isEmpty()) {
                double sum = 0.0;
                for (Instructor ins: iId2Instructor.values()) {
                    sum += ins.getFinalValue();
                }
                iInstrMeanFairValue = sum / iId2Instructor.size();
            }
        }

        /**
         * Method estimates value of placement for instructors in entry list
         * 
         * @param instructorsList instructor list
         * @param placementValue given placement
         * @return estimated value of placement for instructors in entry list
         */

        public double getDiffInstrValue(List<InstructorConstraint> instructorsList, double placementValue) {
            double ret = 0.0;
            for (InstructorConstraint ic : instructorsList) {
                Instructor i = iId2Instructor.get(ic.getResourceId());
                if (i != null) {
                    if (i.getFinalValue() > iInstrMeanFairValue) {
                        ret += ((i.getFinalValue() - iInstrMeanFairValue) / i.getNumOfClasses()) + (placementValue - iInstrMeanFairValue);
                    } else {
                        ret -= ((iInstrMeanFairValue - i.getFinalValue()) / i.getNumOfClasses()) + (iInstrMeanFairValue - placementValue);
                    }
                }
            }
            return ret;
        }

        /**
         * Fairness value based on pdev (pdev sec. part) of all instructors
         * 
         * @return Objective value of all instructors
         */

        public double getObjectiveValue() {
            double ret = 0.0;
            for (Map.Entry<Long, Instructor> entry : iId2Instructor.entrySet()) {
                Instructor ins = entry.getValue();
                ret += Math.abs(ins.getFinalValue() - iInstrMeanFairValue);
            }
            return ret;
        }

        /**
         * Fairness value based on pdev (pdev sec. part) of instructors
         * @param instructors instructor list
         * @return Objective value of instructors 
         */
        public double getObjectiveValue(Collection<InstructorConstraint> instructors) {
            double ret = 0.0;
            for (InstructorConstraint ic : instructors) {
                Instructor ins = iId2Instructor.get(ic.getResourceId());
                if (ins != null)
                    ret += Math.abs(ins.getFinalValue() - iInstrMeanFairValue);
            }
            return ret;
        }

        /**
         * fairness value with squared P and avg.P
         * 
         * @return fairness value with squared P and avg.P
         */

        public double getDiffAllInstrValueSquared() {
            double ret = 0.0;
            for (Map.Entry<Long, Instructor> entry : iId2Instructor.entrySet()) {
                Instructor ins = entry.getValue();
                ret += Math.sqrt(Math.abs(ins.getFinalValue() * ins.getFinalValue() - iInstrMeanFairValue * iInstrMeanFairValue));
            }
            return ret;
        }

        /**
         * refresh of all instructors in iId2Instructor
         *
         */

        public void refreshInstructors() {
            for (Map.Entry<Long, Instructor> entry : iId2Instructor.entrySet()) {
                Instructor ins = entry.getValue();
                ins.refresh();
            }
        }

        /**
         * Metod count and return time (and room) preferences in placement
         *
         * @param assignment current assignment 
         * @param placement given placement
         * @return time (and room) preferences in placement
         */

        public double fairnessDouble(Assignment<Lecture, Placement> assignment, Placement placement) {
            double critValue = 0.0;
            // critValue += getModel().getCriterion(RoomPreferences.class).getWeightedValue(assignment,placement,null);
            critValue += getModel().getCriterion(TimePreferences.class).getWeightedValue(assignment, placement, null);
            return critValue;
        }

        /**
         * Method for whole evaluation of fairness criteria
         * 
         * @param assignment current assignment
         * @return String[] with informations about solution [0-Avarage
         *         instructor penalty, 1-Sum of squared penalities, 2-Pmax,
         *         3-Pdev, 4-Perror, 5-Pss, 6-Jain, 7-worst instructor fairness
         *         value,8-Instructors fairness value]
         */

        public String[] testFairness(Assignment<Lecture, Placement> assignment) {
            String[] dataForEval = new String[9];
            Collection<Lecture> assignedLectures = assignment.assignedVariables();
            refreshInstructors();
            for (Lecture lec : assignedLectures) {
                if (lec.getInstructorConstraints() != null) {
                    List<InstructorConstraint> insConstraints = lec.getInstructorConstraints();
                    double critValue = fairnessDouble(assignment, lec.getAssignment(assignment));
                    for (InstructorConstraint ic : insConstraints) {
                        addInstructorValue(ic.getResourceId(), critValue);
                    }
                }
            }
            countInstrMeanFairValue();
            dataForEval[8] = sDoubleFormat.format(getObjectiveValue());

            double[] instructorsValue = new double[iId2Instructor.values().size()];
            int counter = 0;
            double sumOfSquaredPen = 0.0;
            double min = 100000;
            double max = -100000;
            double sum = 0.0;
            double pdevSecPart = 0.0;
            double pssSecPart = 0.0;

            for (Instructor s : iId2Instructor.values()) {
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
                pdevSecPart += Math.abs(d - sum / instructorsValue.length);
                pssSecPart += d * d;
            }

            // Worst instructor penalty:
            dataForEval[7] = sDoubleFormat.format(max);
            // "Avarage instructor penalty:
            dataForEval[0] = sDoubleFormat.format(sum / instructorsValue.length);
            // Sum of squared penalities:
            dataForEval[1] = sDoubleFormat.format(Math.sqrt(sumOfSquaredPen));
            // Fairness W1:
            // Pmax
            dataForEval[2] = sDoubleFormat.format(iBest - pdevSecPart * iWeight + (instructorsValue.length) * max);
            // Pdev
            dataForEval[3] = sDoubleFormat.format(iBest);
            // PError
            dataForEval[4] = sDoubleFormat.format(iBest - pdevSecPart * iWeight + sum + ((instructorsValue.length) * (max - min)));
            // Pss:
            dataForEval[5] = sDoubleFormat.format(Math.sqrt(((iBest - pdevSecPart * iWeight) * iBest - pdevSecPart * iWeight) + pssSecPart));

            if (sumOfSquaredPen != 0.0) {
                // Jain's index:
                dataForEval[6] = sDoubleFormat.format((sum * sum) / ((instructorsValue.length) * sumOfSquaredPen));
            } else {
                dataForEval[6] = sDoubleFormat.format(1);
            }
            return dataForEval;
        }

        /**
         * Class representing instructor
         * 
         */
        public class Instructor {
            private Long iId;
            private double iValue = 0.0;
            private double iBestValue = 0.0;
            private int iNumOfClasses = 0;

            // could be used to change how important instructors requiremets are
            private double coef = 1;

            /**
             * Create instructor with ID(instructorId)
             * @param instructorId instructor ID
             */
            public Instructor(Long instructorId) {
                iId = instructorId;
            }

            /**
             * representation how well instructors requirements are met
             * 
             * @return Instructor final value
             */

            public double getFinalValue() {
                if (iNumOfClasses == 0) return 0.0;
                return ((iValue - iBestValue) / iNumOfClasses) * coef;
            }

            /**
             *
             * @return iId - instructor ID
             */
            public Long getId() {
                return iId;
            }

            /**
             * 
             * @return iValue - instructor value
             */
            public double getValue() {
                return iValue;
            }

            /**
             * Add value to instructor value
             * @param value value that should be added to instructor value
             */
            public void addValue(double value) {
                iValue += value;
            }

            /**
             * Subtract value from instructor value
             * @param value value that should be subtracted from instructor value
             */
            public void removeValue(double value) {
                iValue -= value;
            }

            /**
             *
             * @return iBestValue - instructor best value
             */
            public double getBestValue() {
                return iBestValue;
            }

            /**
             * Add value to instructor best value
             * @param bestValue value that should be added to instructor best value
             */
            public void addBestValue(double bestValue) {
                iBestValue += bestValue;
            }

            /**
             * 
             * @return iNumOfClasses - number of instructor classes
             */
            public int getNumOfClasses() {
                return iNumOfClasses;
            }

            /**
             * Increase number of classes by 1
             */
            public void incNumOfClasses() {
                this.iNumOfClasses += 1;
            }

            /**
             * Decrease number of instructor classes by 1
             */
            public void decNumOfClasses() {
                this.iNumOfClasses -= 1;
            }

            /**
             * 
             * @return coef - coefficient of instructor
             */
            public double getCoef() {
                return coef;
            }

            /**
             * Set instructor coefficient to double value
             * @param coef coefficient of instructor
             */
            public void setCoef(double coef) {
                this.coef = coef;
            }

            /**
             * Set instructor value and number of classes to 0
             */
            public void refresh() {
                iValue = 0.0;
                iNumOfClasses = 0;
            }

            @Override
            public int hashCode() {
                return iId.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null || !(obj instanceof Instructor)) return false;
                return iId.equals(((Instructor) obj).iId);
            }
        }
    }

    @Override
    public ValueContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new InstructorFairnessContext(assignment);
    }
}