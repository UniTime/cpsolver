package net.sf.cpsolver.exam.model;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Simple room sharing model. Any two exams of the same length ({@link Exam#getLength()}) can be put into any room, if the room is big enough.<br><br>
 * To enable this model, set property Exams.RoomSharingClass to net.sf.cpsolver.exam.model.SimpleExamRoomSharing
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
public class SimpleExamRoomSharing extends ExamRoomSharing {

    public SimpleExamRoomSharing(Model<Exam, ExamPlacement> model, DataProperties config) {
        super(model, config);
    }

    @Override
    public boolean canShareRoom(Exam x1, Exam x2) {
        return x1.getLength() == x2.getLength();
    }
}
