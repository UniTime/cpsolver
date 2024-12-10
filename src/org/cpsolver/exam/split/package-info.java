/**
 * This package contains an an experimental criterion that allows an exam to be split into two if it decreases the number of student conflicts.
 * <br><br>
 * An examination split is improving (and is considered) if the weighted number of student conflicts that will be removed by the split is bigger  than the weight of the splitter criterion.
 * <br><br>
 * To enable examination splitting, following parameters needs to be set:
 * <ul>
 *         <li>HillClimber.AdditionalNeighbours=org.cpsolver.exam.split.ExamSplitMoves
 *         <li>GreatDeluge.AdditionalNeighbours=org.cpsolver.exam.split.ExamSplitMoves
 *         <li>Exams.AdditionalCriteria=org.cpsolver.exam.split.ExamSplitter
 *         <li>Exams.ExamSplitWeight=500
 * </ul>
 * The Exams.ExamSplitWeight represents the weight of a split. For instance, to allow only splits that decrease the number of student direct conflicts,
 * half of the weight of a direct student conflict is a good value for this weight. 
 * 
 * @author  Tomas Muller
 * @version IFS 1.4 (Instructor Sectioning)<br>
 *          Copyright (C) 2024 Tomas Muller<br>
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
package org.cpsolver.exam.split;