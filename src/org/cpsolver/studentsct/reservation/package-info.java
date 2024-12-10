/**
 * Student Sectioning: Reservations.
 * <br>
 * <br>
 * Few notes:
 * <ul>
 *         <li>All reservations are set on instructional offerings and may either have a finite limit or be unlimited.</li>
 *         <li>A reservation may also restrict what configurations and sections can be used (multiple configurations / sections are possible)</li>
 *         <li>Reservations may have different priorities, higher priority reservations take precedence.</li>
 *         <li>Only one reservation is associated with an enrollment
 *                 <ul>
 *                         <li>This means, that at most one reservation will count towards each student course enrollment</li>
 *                         <li>The reservation with highest priority is taken first, then more restrictive (if matching), then more available, then reservation id</li>
 *                 </ul>
 *         </li>
 *         <li>It is possible not to use reservation, if there is enough space in the course / configuration / sections
 *                 <ul>
 *                         <li>Such assignments have lower weight than if reservation is used (+1 on course alternativity)</li>
 *                 </ul>
 *         </li>
 *         <li>Strategy for course / section reservations: do not allow students over the reserved available space into the course / section,
 *                 i.e., do not block students with reservation from having a choice</li>
 * </ul>
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
package org.cpsolver.studentsct.reservation;