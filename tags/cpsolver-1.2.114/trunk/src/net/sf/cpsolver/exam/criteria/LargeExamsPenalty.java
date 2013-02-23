package net.sf.cpsolver.exam.criteria;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Front load penalty. I.e., large exam is discouraged to be placed on or after a
 * certain period.
 * <br><br>
 * <b>largeSize</b>: An exam is considered large, if its size is greater or equal to 
 * this number. Value -1 means all exams are small. It can be set by problem
 * property Exams.LargeSize, or in the input xml file, property largeSize.
 * <br><br>
 * <b>largePeriod</b>: Period index (number of periods multiplied by this number) for front load
 * criteria for large exams. Can be set by problem property
 * Exams.LargePeriod, or in the input xml file, property largePeriod.
 * <br><br>
 * Weight of the front load criterion, i.e., a weight for assigning a large exam
 * after large period can be set by problem property Exams.LargeWeight, or
 * in the input xml file, property largeWeight.
 * 
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2012 Tomas Muller<br>
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
public class LargeExamsPenalty extends ExamCriterion {
    private int iLargeSize = -1;
    private double iLargePeriod = 0.67;
    
    @Override
    public String getWeightName() {
        return "Exams.LargeWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "largeWeight";
    }
    
    @Override
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
        params.put("largeSize", String.valueOf(getLargeSize()));
        params.put("largePeriod", String.valueOf(getLargePeriod()));
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        try {
            setWeight(Double.valueOf(params.get(getXmlWeightName())));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setLargeSize(Integer.valueOf(params.get("largeSize")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setLargePeriod(Double.valueOf(params.get("largePeriod")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        boolean ret = super.init(solver);
        iLargeSize = solver.getProperties().getPropertyInt("Exams.LargeSize", iLargeSize);
        iLargePeriod = solver.getProperties().getPropertyDouble("Exams.LargePeriod", iLargePeriod);
        return ret;
    }
    
    /**
     * An exam is considered large, if its size is greater or equal to this
     * large size. Value -1 means all exams are small. Can be set by problem
     * property Exams.LargeSize, or in the input xml file, property largeSize)
     **/
    public int getLargeSize() {
        return iLargeSize;
    }
    
    /**
     * An exam is considered large, if its size is greater or equal to this
     * large size. Value -1 means all exams are small. Can be set by problem
     * property Exams.LargeSize, or in the input xml file, property largeSize)
     **/
    public void setLargeSize(int largeSize) {
        iLargeSize = largeSize;
    }
    
    /**
     * Period index (number of periods multiplied by this number) for front load
     * criteria for large exams. Can be set by problem property
     * Exams.LargePeriod, or in the input xml file, property largePeriod)
     **/
    public double getLargePeriod() {
        return iLargePeriod;
    }
    
    /**
     * Period index (number of periods multiplied by this number) for front load
     * criteria for large exams. Can be set by problem property
     * Exams.LargePeriod, or in the input xml file, property largePeriod)
     **/
    public void setLargePeriod(double largePeriod) {
        iLargePeriod = largePeriod;
    }

    
    public int getLargePeriodIndex() {
        return (int) Math.round(((ExamModel)getModel()).getPeriods().size() * iLargePeriod);
    }

    @Override
    public double getValue(ExamPlacement value, Set<ExamPlacement> conflicts) {
        Exam exam = value.variable();
        if (getLargeSize() < 0 || exam.getSize() < getLargeSize()) return 0;
        return (value.getPeriod().getIndex() < getLargePeriodIndex() ? 0 : 1);
    }

    @Override
    public double[] getBounds(Collection<Exam> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Exam exam : variables) {
            if (getLargeSize() >= 0 && exam.getSize() >= getLargeSize())
                bounds[1] += 1.0;
        }
        return bounds;
    }

    @Override
    public String toString() {
        return (getValue() <= 0.0 ? "" : "LP:" + sDoubleFormat.format(getValue()));
    }
}