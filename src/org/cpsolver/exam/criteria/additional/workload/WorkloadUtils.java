package org.cpsolver.exam.criteria.additional.workload;

import java.util.ArrayList;
import java.util.List;

/**
 * Tools for calculating the workload.
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
public class WorkloadUtils {
    
    /**
     * Calculate rolling sums
     * @param listOfIntValues
     * @param windowLength
     * @return List with the rolling totals
     */
    public static List<Integer> rollingSumInt(List<Integer> listOfIntValues, int windowLength) {
        int sum = 0;
        List<Integer> rollingSums = new ArrayList<Integer>();
        for (int i = 0; i < listOfIntValues.size(); i++) {
            sum += listOfIntValues.get(i);
            if (i >= windowLength) {
                rollingSums.add(sum);
                sum -= listOfIntValues.get(i - windowLength);
            }
        }
        return rollingSums;
    }
    
    /**
     * Used to calculate the workload.
     * @param listOfIntValues
     * @param threshold
     * @return number of values larger than the threshold
     */
    public static int numberOfValuesLargerThanThreshold(List<Integer> listOfIntValues, int threshold) {
        int count = 0;
        for (int number: listOfIntValues) {
            if (number > threshold) {
                count++;
            }
        }
        return count;
    }
}
