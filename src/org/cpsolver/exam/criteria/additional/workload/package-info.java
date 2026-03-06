/**
 * Additional exam criteria to control the workload.
 *<br>
 * The workload is calculated as follows:
 *<br>
 * Step 1: Calculate the daily exam load. The result is a list of integers representing the daily exam load. 
 * This list's length is equal to the number of exam days. The entry at position i represents 
 * the number of exams on the i-th day.
 *<br>
 * Step 2: Calculate rolling sums of length <code>numberOfDays</code> using the list of daily exam loads. 
 * The result is a list of rolling sums.
 *<br>
 * Step 3: The workload equals the number of rolling sums that exceed the threshold <code>numberOfExams</code>.
 * 
 * @author  Alexander Kreim
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
package org.cpsolver.exam.criteria.additional.workload;