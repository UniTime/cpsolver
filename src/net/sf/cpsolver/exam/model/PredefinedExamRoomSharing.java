package net.sf.cpsolver.exam.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

import net.sf.cpsolver.coursett.IdConvertor;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Room sharing model based on a pre-defined list of examination pairs. The relation needs to be populated
 * using {@link PredefinedExamRoomSharing#addPair(Exam, Exam)} and it is persisted with the solution XML (see
 * {@link ExamModel#save()}, canShareRoom element for each exam containing a comma separated list of exam ids).
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
public class PredefinedExamRoomSharing extends ExamRoomSharing {
    private Map<Long, Set<Long>> iSharingMatrix = new HashMap<Long, Set<Long>>();
    
    public PredefinedExamRoomSharing(Model<Exam, ExamPlacement> model, DataProperties config) {
        super(model, config);
    }

    @Override
    public boolean canShareRoom(Exam x1, Exam x2) {
        if (x1.getId() < x2.getId()) {
            Set<Long> exams = iSharingMatrix.get(x1.getId());
            return exams != null && exams.contains(x2.getId());
        } else {
            Set<Long> exams = iSharingMatrix.get(x2.getId());
            return exams != null && exams.contains(x1.getId());
        }
    }
    
    /** Add a pair of exams that are allowed to share a room */
    public void addPair(Exam x1, Exam x2) {
        addPair(x1.getId(), x2.getId());
    }
    
    /** Add a pair of exams that are allowed to share a room */
    public void addPair(Long examId1, Long examId2) {
        if (examId1 < examId2) {
            Set<Long> exams = iSharingMatrix.get(examId1);
            if (exams == null) { exams = new HashSet<Long>(); iSharingMatrix.put(examId1, exams); }
            exams.add(examId2);
        } else {
            Set<Long> exams = iSharingMatrix.get(examId2);
            if (exams == null) { exams = new HashSet<Long>(); iSharingMatrix.put(examId2, exams); }
            exams.add(examId1);
        }        
    }
    
    /** Clear examination pairs */
    public void clear() {
        iSharingMatrix.clear();
    }
    
    @Override
    public void save(Exam exam, Element element, IdConvertor idConvertor) {
        Set<Long> exams = iSharingMatrix.get(exam.getId());
        if (exams != null) {
            String ids = "";
            for (Long id: exams) {
                if (!ids.isEmpty()) ids += ",";
                ids += (idConvertor == null ? id.toString() : idConvertor.convert("exam", id.toString()));
            }
            element.addElement("canShareRoom").setText(ids);
        }
    }
    
    @Override
    public void load(Exam exam, Element element) {
        Element canShareRoom = element.element("canShareRoom");
        if (canShareRoom == null) return;
        for (String id: canShareRoom.getTextTrim().split(","))
            addPair(exam.getId(), Long.valueOf(id.trim()));
    }
}
