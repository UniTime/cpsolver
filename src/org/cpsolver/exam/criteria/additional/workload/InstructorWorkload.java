package org.cpsolver.exam.criteria.additional.workload;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamInstructor;
import org.cpsolver.exam.model.ExamModel; 
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Instructors should not have more than <code>numberOfExams</code> exams in <code>numberOfDays</code>
 * consecutive days.
 * <br>
 * The goal of this criterion is to minimize instructor workload during exams.
 * <br>
 * Only the instructors specified in a xml-file are included.
 * 
 * <pre><code>
 * &lt;instructors&gt;
 *   &lt;instructor&gt;
 *     &lt;name&gt;John Doe&lt;/name&gt;
 *     &lt;numberOfDays&gt;5&lt;/numberOfDays&gt;
 *     &lt;numberOfExams&gt;3&lt;/numberOfExams&gt;
 *   &lt;/instructor&gt;
 *   ... more instructors ...
 * &lt;/instructors>
 * </code></pre>
 * 
 * The path to the xml-file can be set by the property 
 * Exams.Workload.Instructors.XMLFile or in the input xml-file, property 
 * examsWorkloadInstructorXMLFile.
 * <br>
 * The weight of the criterion can be set by problem property Exams.Workload.Instructors.Weight,
 * or in the problem xml-file (input xml file), property examWorkloadInstructorsWeight.
 * 
 * @author Alexander Kreim
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
 * 
 */
public class InstructorWorkload extends WorkloadBaseCriterion {

    private static Logger slog =
            org.apache.logging.log4j.LogManager.getLogger(InstructorWorkload.class);
    
    private String instructorXMLFile;
    
    public InstructorWorkload() {
        super();
    }
    
    public String getInstructorXMLFile() {
        return instructorXMLFile;
    }

    public void setInstructorXMLFile(String iInstructorXMLFile) {
        this.instructorXMLFile = iInstructorXMLFile;
    }
    
    @Override
    public boolean init(Solver<Exam, ExamPlacement> solver) {
        // TODO Auto-generated method stub
        super.init(solver);
        configure( solver.getProperties() );
        return true;
    }

    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> variables) {
        double penalty = 0.0;
        if ( (getModel() != null) && (assignment != null) ) {
            if (examPeriodsAreNotEmpty()) {
                Set<ExamInstructor> examInstructors = getExamInstructorsFromVariables(variables);
                setWorkloadFromAssignment(assignment, null);
                
                for (Iterator<ExamInstructor> iterator = examInstructors.iterator(); iterator.hasNext();) {
                    ExamInstructor examInstructor = iterator.next();
                    
                    WorkloadEntity entity = getWorkloadEntityByInstructor(assignment, 
                                                                          examInstructor);
                    if (entity != null) {
                        penalty += entity.getWorkload();
                    }
                }
            }
        }
        return penalty;
    }

    private Set<ExamInstructor> getExamInstructorsFromVariables(Collection<Exam> variables) {
        Set<ExamInstructor> examInstructors= new HashSet<ExamInstructor>();
        for (Iterator<Exam> iterator = variables.iterator(); iterator.hasNext();) {
            Exam exam = iterator.next();
            List<ExamInstructor> instructors = exam.getInstructors();
            examInstructors.addAll(instructors);
        }
        return examInstructors;
    }

    /**
     * Creates Workload Entities
     */
    @Override
    protected List<WorkloadEntity> createWorkLoadEntities() { 
        if (getInstructorXMLFile() != null ) {
            List<WorkloadEntity>  entities = readInstructorsFromXMLFile();
            slog.debug("Found " + entities.size() + " instructors.");
            if ( examPeriodsAreNotEmpty() ) {
                int numbrOfDays = ((ExamModel)getModel()).getNrDays();
                for (Iterator<WorkloadEntity> iterator = entities.iterator(); iterator.hasNext();) {
                    WorkloadEntity entity = iterator.next();
                    entity.resetLoadPerDay(numbrOfDays);
                }
            }
            slog.info("Instructor Workload: Created " + entities.size() + " entities. This should happen only once (per solver).");
            return entities;
        }
        // slog.debug("Instructors XML-File is not set.");
        return null;
    }
    
    private List<WorkloadEntity> readInstructorsFromXMLFile() {
        XmlParser xmlParser = new XmlParser();
        slog.debug("Read Instructors from file " + getInstructorXMLFile() );
        return xmlParser.parse(new File(instructorXMLFile));
    }

    @Override
    protected void setWorkloadFromAssignment(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value) {
        if (examPeriodsAreNotEmpty() && (assignment != null)) {

            List<ExamInstructor> allExamInstructors = ((ExamModel) getModel()).getInstructors();
            int nbrOfExamDays = ((ExamModel) getModel()).getNrDays();

            WorkloadContext workloadContext = getWorkLoadContext(assignment);
            List<WorkloadEntity> entities = workloadContext.getEntityList();
            if (entities == null) {
                List<WorkloadEntity> newEntities = createWorkLoadEntities();
                workloadContext.setEntityList(newEntities);
                workloadContext.calcTotalFromAssignment(assignment);
            }

            for (Iterator<ExamInstructor> examInsIter = allExamInstructors.iterator(); examInsIter.hasNext();) {
                ExamInstructor examInstructor = examInsIter.next();
                WorkloadEntity entity = getWorkloadEntityByInstructor(assignment, examInstructor);
                if (entity != null) {
                    entity.resetLoadPerDay(nbrOfExamDays);
                    List<Integer> loadPerDay = entity.getLoadPerDay();
                    for (int dayIndex = 0; dayIndex < nbrOfExamDays; dayIndex++) {
                        Set<Exam> examsADay = examInstructor.getExamsADay(assignment, dayIndex);
                        int nbrOfExamsADay = examsADay.size();
                        if (value != null) {
                            Exam examInValue = value.variable();
                            if (examsADay.contains(examInValue)) {
                                nbrOfExamsADay = nbrOfExamsADay - 1;
                            }
                        }
                        loadPerDay.set(dayIndex, nbrOfExamsADay);
                    }
                }
            }
        }
    }
    
    protected WorkloadEntity getWorkloadEntityByInstructor(Assignment<Exam, ExamPlacement>  assignment,
                                                           ExamInstructor examInstructor) { 
        
        WorkloadContext workloadContext = getWorkLoadContext(assignment);
        
        List<WorkloadEntity> entityList = workloadContext.getEntityList();
        if (entityList != null) {
            for (Iterator<WorkloadEntity> iterator = entityList.iterator(); iterator.hasNext();) {
                WorkloadEntity entity = iterator.next();
                if (entity.getName().equals(examInstructor.getName())) {
                    return entity;
                }
            }
        }
        return null;
    }
    
    @Override
    protected void setDaysToInkrementWorkloadFromValue(Assignment<Exam, ExamPlacement> assignment,
                                                       ExamPlacement value) {
        if ( examPeriodsAreNotEmpty() && (value != null) ) {
            Exam exam = value.variable();
            List<ExamInstructor> examInstructors = exam.getInstructors();
            WorkloadContext workloadContext = getWorkLoadContext(assignment);
            workloadContext.resetDaysToInkrementWorkload();
            for (Iterator<ExamInstructor> iterator = examInstructors.iterator(); iterator.hasNext();) {
                ExamInstructor examInstructor = iterator.next();
                WorkloadEntity entity = getWorkloadEntityByInstructor(assignment,
                                                                      examInstructor);
                if (entity != null) {
                    Set<Integer> daysToInkrement = 
                            (workloadContext.getDaysToInkrementWorkload()).get(entity);
                    daysToInkrement.add(value.getPeriod().getDay());    
                }
            }
        }
   }
    
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "I WL:" + sDoubleFormat.format(getValue(assignment));
    }   
    
    @Override
    public String getName() {
        return "Instructor Workload";
    }
            
    @Override
    public String getWeightName() {
        return "Exams.Workload.Instructors.Weight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "examsWorkloadInstructorsWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties); 
        setInstructorXMLFile(properties.getProperty("Exams.Workload.Instructors.XMLFile", null));
        if (getInstructorXMLFile() == null) {
            slog.warn("Instructor XML file not set");
        }
    }
    
    @Override
    public void getXmlParameters(Map<String, String> params) {
        super.getXmlParameters(params); 
        params.put("examsWorkloadInstructorXMLFile", getInstructorXMLFile());
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        super.setXmlParameters(params);
        try {
            setInstructorXMLFile(params.get("examsWorkloadInstructorXMLFile"));
        } 
        catch (NumberFormatException e) {}
        catch (NullPointerException e) {}
    }
    
    protected class XmlParser {
        public List<WorkloadEntity> parse(File xmlFile) {
            SAXReader reader = new SAXReader();
            List<WorkloadEntity> entities = new ArrayList<WorkloadEntity>();
            try {
                Document document = reader.read(xmlFile);
                Element rootElement = document.getRootElement();
                List<Element> instructorElements = rootElement.elements("instructor");
                
                for (Element instructorElement: instructorElements) {
                    WorkloadEntity entity = new WorkloadEntity();
                    entity.setName(instructorElement.elementText("name"));
                    entity.setNbrOfDays(Integer.parseInt(instructorElement.elementText("numberOfDays")));
                    entity.setNbrOfExams(Integer.parseInt(instructorElement.elementText("numberOfExams")));
                    entities.add(entity);
                    slog.debug("Added Instructor" + entity.getName() 
                               + " nbrOfDays: " + entity.getNbrOfDays()
                               + " nbrOfExams: " + entity.getNbrOfExams());
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            return entities;
        }
    }
}