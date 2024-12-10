package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Model;

/**
 * An interface marking an assignment context. The idea is that each
 * class that needs to keep some assignment dependent data will implements
 * {@link HasAssignmentContext} interface and the data will be wrapped by this class.
 * The {@link HasAssignmentContext} will only contain a reference to this
 * assignment context, created by calling {@link Model#createReference(HasAssignmentContext)}
 * during its initialization. The assignment context can be than accessed by calling
 * {@link Assignment#getAssignmentContext(AssignmentContextReference)}.<br><br>
 * 
 * These assignment contexts are being held in memory by a class implementing the
 * {@link AssignmentContextHolder} interface. For constraints, criteria, extensions, and 
 * neighborhood selections an existing class implementing the context can be used, see
 * {@link ConstraintWithContext}, {@link AbstractCriterion}, {@link ExtensionWithContext},
 * and {@link NeighbourSelectionWithContext} respectively.<br><br>
 * 
 * For instance, when implementing {@link ConstraintWithContext}, only the method
 * {@link ConstraintWithContext#createAssignmentContext(Assignment)} needs to be implemented and the 
 * assignment context can be accessed within the constraint using the method
 * {@link ConstraintWithContext#getContext(Assignment)}.
 * 
 * @see AssignmentContextHolder
 * @see HasAssignmentContext
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 **/
public interface AssignmentContext {

}
