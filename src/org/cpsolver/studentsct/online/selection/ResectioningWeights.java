package org.cpsolver.studentsct.online.selection;

import java.util.List;

import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.model.Choice;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.weights.StudentWeights;

/**
 * Re-scheduling variant of {@link StudentWeights} model. It is based on the
 * {@link StudentSchedulingAssistantWeights}. This model is used when a single
 * course of a student is rescheduled. Following criteria are included:
 * <ul>
 * <li>StudentWeights.SameChoiceFactor .. penalization of selecting a different
 * choice (see {@link Choice})
 * <li>StudentWeights.SameRoomsFactor .. penalization of selecting different
 * room
 * <li>StudentWeights.SameTimeFactor .. penalization of selecting different time
 * <li>StudentWeights.SameNameFactor .. penalization of selecting different
 * section (section name / external id does not match)
 * </ul>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
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
 *          License along with this library; if not see <a
 *          href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class ResectioningWeights extends StudentSchedulingAssistantWeights {
    private double iSameChoiceFactor = 0.125;
    private double iSameRoomsFactor = 0.007;
    private double iSameTimeFactor = 0.070;
    private double iSameNameFactor = 0.014;
    private LastSectionProvider iLastSectionProvider = null;

    public ResectioningWeights(DataProperties properties) {
        super(properties);
        iSameChoiceFactor = properties.getPropertyDouble("StudentWeights.SameChoiceFactor", iSameChoiceFactor);
        iSameRoomsFactor = properties.getPropertyDouble("StudentWeights.SameRoomsFactor", iSameRoomsFactor);
        iSameTimeFactor = properties.getPropertyDouble("StudentWeights.SameTimeFactor", iSameTimeFactor);
        iSameNameFactor = properties.getPropertyDouble("StudentWeights.SameNameFactor", iSameNameFactor);
    }

    public void setLastSectionProvider(LastSectionProvider lastSectionProvider) {
        iLastSectionProvider = lastSectionProvider;
    }

    @Override
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        double weight = super.getWeight(assignment, enrollment);

        if (enrollment.isCourseRequest() && enrollment.getAssignments() != null && iLastSectionProvider != null) {
            int sameChoice = 0;
            int sameTime = 0;
            int sameRooms = 0;
            int sameName = 0;
            for (Section section : enrollment.getSections()) {
                if (iLastSectionProvider.sameLastChoice(section))
                    sameChoice++;
                if (iLastSectionProvider.sameLastTime(section))
                    sameTime++;
                if (iLastSectionProvider.sameLastRoom(section))
                    sameRooms++;
                if (iLastSectionProvider.sameLastName(section, enrollment.getCourse()))
                    sameName++;
            }
            CourseRequest cr = (CourseRequest) enrollment.getRequest();
            if (sameChoice == 0 && !cr.getSelectedChoices().isEmpty()) {
                for (Section section : enrollment.getSections()) {
                    if (cr.isSelected(section)) {
                        sameChoice++;
                        continue;
                    }
                }
            }
            double size = enrollment.getAssignments().size();
            double sameChoiceFraction = (size - sameChoice) / size;
            double sameTimeFraction = (size - sameTime) / size;
            double sameRoomsFraction = (size - sameRooms) / size;
            double sameNameFraction = (size - sameName) / size;
            double base = getBaseWeight(assignment, enrollment);
            weight -= sameChoiceFraction * base * iSameChoiceFactor;
            weight -= sameTimeFraction * base * iSameTimeFactor;
            weight -= sameRoomsFraction * base * iSameRoomsFactor;
            weight -= sameNameFraction * base * iSameNameFactor;
        }

        return weight;
    }

    public static boolean isSame(Enrollment e1, Enrollment e2) {
        if (e1.getSections().size() != e2.getSections().size())
            return false;
        s1: for (Section s1 : e1.getSections()) {
            for (Section s2 : e2.getSections())
                if (s1.sameChoice(s2))
                    continue s1;
            return false;
        }
        return true;
    }

    public static boolean sameRooms(Section s, List<RoomLocation> rooms) {
        if (s.getRooms() == null && rooms == null)
            return true;
        if (s.getRooms() == null || rooms == null)
            return false;
        return s.getRooms().size() == rooms.size() && s.getRooms().containsAll(rooms);
    }

    public static boolean sameTime(Section s, TimeLocation t) {
        if (s.getTime() == null && t == null)
            return true;
        if (s.getTime() == null || t == null)
            return false;
        return s.getTime().getStartSlot() == t.getStartSlot() && s.getTime().getLength() == t.getLength()
                && s.getTime().getDayCode() == t.getDayCode()
                && ToolBox.equals(s.getTime().getDatePatternName(), t.getDatePatternName());
    }

    public static boolean sameName(Long courseId, Section s1, Section s2) {
        return s1.getName(courseId).equals(s2.getName(courseId));
    }
    
    public static boolean sameChoice(Section section, Choice choice) {
        return choice.sameChoice(section);
    }

    /**
     * Compare the old assignment with the current one
     */
    public static interface LastSectionProvider {
        /**
         * Check the choice (see {@link Choice})
         * 
         * @param current
         *            current section
         * @return true if the choice matches
         */
        public boolean sameLastChoice(Section current);

        /**
         * Check the time
         * 
         * @param current
         *            current section
         * @return true if the time matches
         */
        public boolean sameLastTime(Section current);

        /**
         * Check the room
         * 
         * @param current
         *            current section
         * @return true if the room matches
         */
        public boolean sameLastRoom(Section current);

        /**
         * Check section name (external id)
         * 
         * @param current
         *            current section
         * @param course
         *            current course
         * @return true if the section name matches
         */
        public boolean sameLastName(Section current, Course course);
    }
}
