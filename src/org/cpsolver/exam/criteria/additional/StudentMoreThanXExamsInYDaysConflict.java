package org.cpsolver.exam.criteria.additional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year; 
import java.time.format.DateTimeFormatter; 

import org.cpsolver.exam.criteria.ExamCriterion;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamOwner;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPeriodPlacement;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.exam.model.ExamStudent; 
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
/**
 * Students not more than X exams in Y consecutive days.
 * <p>It is assumed that all exam periods are within a single year. The information about day and month 
 * of an exam period is read from the day attribute of the period tag (see solver input xml-file).
 * The weight of the criterion can be set by problem property Exams.StudentsMoreThanXExamsInXDaysWeight,
 * or in the input xml file, property moreThanXExamsInYDaysWeight</p>
 * <p><b>studentStressNbrExams</b>: Maximum number of exams a student should have within a given 
 * number of consecutive days. Can be set by problem property
 * Exams.StudentStressNbrExams, or in the input xml file, property studentStressNbrExams. 
 * If set to -1 this criterion is disabled. Default value: -1.
 * </p>
 * <p><b>studentStressNbrDays</b>: Number of consecutive days. Can be set by problem property
 * Exams.StudentStressNbrDays, or in the input xml file, property studentStressNbrDays.
 * If set to -1 this criterion is disabled. Default value: -1.
 * </p> 
 * <p><b>studentStressPeriodDateFormat</b>: Format of the period day string. The default value is "E M/d". 
 * <br>It can be set by problem property Exams.StudentStressPeriodDateFormat, or in the input xml file, 
 * property studentStressPeriodDateFormat. 
 * See also <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns">DateTimeFormatter documentation</a>
 * for valid format strings.</p>
 * <p><b>studentStressExaminationYear</b> Year in which the examinations take place (default: "2023"). 
 * It is used for date computations. It should be set if the examination year is a leap year. 
 * <br>It can be set by problem property Exams.StudentStressExaminationYear, or in the input xml file, 
 * property studentStressExaminationYear. </p> 
 * @author Alexander Kreim
 */
public class StudentMoreThanXExamsInYDaysConflict extends ExamCriterion {

    private int iNbrOfDays=-1;
    private int iNbrOfExams=-1;
    private String iPeriodDateFormatString="E M/d";
    private Locale iLocale = Locale.ENGLISH;
    private Year iExaminationYear = Year.parse("2023");
    private DateTimeFormatter iDateFormatter = 
            DateTimeFormatter.ofPattern(iPeriodDateFormatString, iLocale);
    
    @Override
    public String getName() {
        return "More Than " 
         + String.valueOf(getNbrOfExams()) 
         + " in "
         + String.valueOf(getNbrOfDays())
         + " Days";
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, ExamPlacement value,
            Set<ExamPlacement> conflicts) {
        if ((iNbrOfExams > 0) && (iNbrOfDays > 0)) {
            List<ExamStudent> examStudents = getExamStudents(value);  
            return calcPenalty(assignment, examStudents);
        }  else {
            return 0;
        }
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment) {
        if ((iNbrOfExams > 0) && (iNbrOfDays > 0)) {
            List<ExamStudent> examStudents = ((ExamModel)getModel()).getStudents();  
            return calcPenalty(assignment, examStudents);
        }  else {
            return 0;
        } 
    }
    
    @Override
    public double getValue(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> variables) {
        if ((iNbrOfExams > 0) && (iNbrOfDays > 0)) {
            List<ExamStudent> studentsInSelection = new ArrayList<ExamStudent>();
            for (Iterator<Exam> examIter = variables.iterator(); examIter.hasNext();) {
                Exam exam = examIter.next();
                List<ExamStudent> examStudents = exam.getStudents();
                for (Iterator<ExamStudent> studentIter = examStudents.iterator(); studentIter.hasNext();) {
                    ExamStudent examStudent = studentIter.next();
                    if (!studentsInSelection.contains(examStudent)) {
                        studentsInSelection.add(examStudent);
                    }
                }
            }
            return calcPenalty(assignment, studentsInSelection);
        }  else {
            return 0;
        }       
    }  
 
    /** 
     * Calculates the penalty for a given set or students.
     * 
     * <pre><code>
     * for each student 
     *   get all days where the student has exams
     *   determine first and last days of student's exams
     *   start day = first day
     *   while start day <= last day 
     *      create test period based on iNbrOfDays beginning at start day
     *      count number of exams in the test period
     *      if number of exams in test period > iNbrOfExams 
     *         penalty++
     *      increment start day by one day
     * </code></pre>     
     * @param assignment
     * @param examStudents
     * @return
     */
    private int calcPenalty(Assignment<Exam, ExamPlacement> assignment, List<ExamStudent> examStudents) {
        int  penalty = 0;
        for (Iterator<ExamStudent> iterator = examStudents.iterator(); iterator.hasNext();) {
            ExamStudent student = iterator.next();
            Map<String, Integer> nrExamsPerDay = getNrExamsPerDay(student, assignment);
            MonthDay firstDay = getFirstDay(nrExamsPerDay.keySet());
            if (firstDay == null) continue;
            MonthDay lastDay = getLastDay(nrExamsPerDay.keySet());
            MonthDay startDay = MonthDay.from(firstDay);
            while (!startDay.isAfter(lastDay)) {
                int nbrOfExams = countNbrOfExamsInPeriod(startDay, addDays(startDay, getNbrOfDays()), nrExamsPerDay);
                if (nbrOfExams > getNbrOfExams()) penalty++;
                startDay = addDays(startDay, 1);
            }
        }
        return penalty;
    }
    
    @Override
    /*
     * Estimation of the upper bound.
     * 
     * It is assumed that all enrolled exams of a student are in a row. 
     */
    public double[] getBounds(Assignment<Exam, ExamPlacement> assignment, Collection<Exam> exams) {
        double[] bounds = new double[] { 0.0, 0.0 };
        // get all exam placements for each student enrolled in the given exams
        Map<ExamStudent, Set<ExamPlacement>> allExamPlacements = getStudentExamPlacements(assignment, exams);
        // Calculate maximum penalty, assuming all student's exams are in a row.
        bounds[1] =  calcMaxPenalty(allExamPlacements);
        return bounds;
    }
    
    private int calcMaxPenalty(Map<ExamStudent, Set<ExamPlacement>> studentExamPlacements) {
    	int nbrOfStudentExams = 0;
    	int penalty = 0;
        for (Map.Entry<ExamStudent, Set<ExamPlacement>> studentEnrollments : studentExamPlacements.entrySet()) {
            // ExamStudent student = studentEnrollments.getKey();
            Set<ExamPlacement> enrollments = studentEnrollments.getValue();
            nbrOfStudentExams = enrollments.size();
            if (nbrOfStudentExams > iNbrOfExams) {
                penalty += calcMaxStudentPenalty(nbrOfStudentExams);
            }
        }
        return penalty;
    }
    
    private int calcMaxStudentPenalty(int nbrOfStudentExams) {
        // p + iNbrOfExams - 1 >= nbrOfStudentExams
        // p .. penalty, use min p
        int p = nbrOfStudentExams + 1 - iNbrOfExams;
        return p;
    }
    
    private HashMap<ExamStudent, Set<ExamPlacement>>  getStudentExamPlacements(
            Assignment<Exam, ExamPlacement> assignment, Collection<Exam> exams) {
         
        HashMap<ExamStudent, Set<ExamPlacement>> studentExamPlacements = 
                new  HashMap<ExamStudent, Set<ExamPlacement>>();
        
        for (Iterator<Exam> examIter = exams.iterator(); examIter.hasNext();) {
             Exam exam = examIter.next();
             ExamPlacement placement = assignment.getValue(exam);
             List<ExamStudent> students = exam.getStudents();
             for (Iterator<ExamStudent> studentIter = students.iterator(); studentIter.hasNext();) {
                 ExamStudent student = studentIter.next();
                 if (studentExamPlacements.keySet().isEmpty()) {
                     HashSet<ExamPlacement> placementsAsSet = new HashSet<ExamPlacement>();
                     placementsAsSet.add(placement);
                     studentExamPlacements.put(student, placementsAsSet);
                     continue;
                 }
                 if (!studentExamPlacements.keySet().contains(student)) {
                     HashSet<ExamPlacement> placementsAsSet = new HashSet<ExamPlacement>();
                     placementsAsSet.add(placement);
                     studentExamPlacements.put(student, placementsAsSet);
                     continue;                  
                 }
                 if (studentExamPlacements.keySet().contains(student)) {
                     studentExamPlacements.get(student).add(placement);
                 }
             }
         }
         return studentExamPlacements;
    }
        
    private MonthDay addDays(MonthDay day, int nbrOfDays) {
        LocalDate newDate = day.atYear(iExaminationYear.getValue()).plusDays(nbrOfDays);
        return MonthDay.from(newDate);   
    }
    
    private int countNbrOfExamsInPeriod(MonthDay startDay, MonthDay endDay, Map<String, Integer> nrOfExamsPerDay) {
        int totalNbrOfExams=0;
        for (Map.Entry<String, Integer> entry : nrOfExamsPerDay.entrySet()) {
            String dayString = entry.getKey();
            Integer nbrOfExams = entry.getValue();
            MonthDay currentDate = MonthDay.parse(dayString, iDateFormatter);
            if ((currentDate.isAfter(startDay)) && (currentDate.isBefore(endDay))) {
                totalNbrOfExams = totalNbrOfExams + nbrOfExams;
            }
           if (currentDate.equals(endDay)) totalNbrOfExams = totalNbrOfExams + nbrOfExams;
           if (currentDate.equals(startDay)) totalNbrOfExams = totalNbrOfExams + nbrOfExams;
        }
        return totalNbrOfExams;
    }
    
    private MonthDay getFirstDay(Set<String> dayStrings) {
        MonthDay firstDay=null;
        
        for (Iterator<String> iterator = dayStrings.iterator(); iterator.hasNext();) {
            String dayString = iterator.next();
            if (firstDay == null) {
                firstDay =  MonthDay.parse(dayString, iDateFormatter);
            } else {
               MonthDay currentDay = MonthDay.parse(dayString, iDateFormatter);
               if (currentDay.isBefore(firstDay)) firstDay=currentDay;
            }
        }
        return firstDay;
    }
    
    private MonthDay getLastDay(Set<String> dayStrings) {
        MonthDay lastDay=null; 
        
        for (Iterator<String> iterator = dayStrings.iterator(); iterator.hasNext();) {
            String dayString = iterator.next();
            if (lastDay == null) {
                lastDay =  MonthDay.parse(dayString, iDateFormatter);
            } else {
               MonthDay currentDay = MonthDay.parse(dayString, iDateFormatter);
               if (currentDay.isAfter(lastDay)) lastDay=currentDay;
            }
        }
        return lastDay;
    }
    
    private List<ExamPeriod> getAllExamPeriods() {
    	return ((ExamModel)getModel()).getPeriods();
    }
       
    private Map<String, Integer> getNrExamsPerDay(ExamStudent student, Assignment<Exam, ExamPlacement> assignment) {
    	List<ExamPeriod> examPeriods= getAllExamPeriods();
    	HashMap<String, Integer> nrExamsPerDay = new HashMap<String, Integer>();
    	for (Iterator<ExamPeriod> iterator = examPeriods.iterator(); iterator.hasNext();) {
    	    ExamPeriod examPeriod = iterator.next();
    	    Set<Exam> enrolledExams = student.getExamsADay(assignment, examPeriod);
    	    if (! enrolledExams.isEmpty()) {
    	        String periodDayStr = examPeriod.getDayStr();
		if (! nrExamsPerDay.keySet().contains(periodDayStr)) {
		    nrExamsPerDay.put(periodDayStr, enrolledExams.size());
		} 
    	    }
	}
    	return nrExamsPerDay;
    }
    
    private List<ExamStudent> getExamStudents(ExamPlacement value) {
    	return value.variable().getStudents();
    }
    
    @Override
    public String getWeightName() {
        return "Exams.StudentsMoreThanXExamsInXDaysWeight";
    }
    
    @Override
    public String getXmlWeightName() {
        return "moreThanXExamsInYDaysWeight";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        setNbrOfDays(properties.getPropertyInt("Exams.StudentStressNbrDays", iNbrOfDays));
        setNbrOfExams(properties.getPropertyInt("Exams.StudentStressNbrExams", iNbrOfExams));
        setPeriodDateFormatString(properties.getProperty("Exams.StudentStressPeriodDateFormat", iPeriodDateFormatString));
        setExaminationYear(properties.getProperty("Exams.StudentStressExaminationYear", iPeriodDateFormatString));
    }
    
    @Override
    public void getXmlParameters(Map<String, String> params) {
        params.put(getXmlWeightName(), String.valueOf(getWeight()));
        params.put("studentStressNbrExams", String.valueOf(getNbrOfExams()));
        params.put("studentStressNbrDays", String.valueOf(getNbrOfDays()));
        params.put("studentStressPeriodDateFormat", getPeriodDateFormatString());
        params.put("studentStressExaminationYear", getExaminationYear().toString());
    }
    
    @Override
    public void setXmlParameters(Map<String, String> params) {
        try {
            setWeight(Double.valueOf(params.get(getXmlWeightName())));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setNbrOfExams(Integer.valueOf(params.get("studentStressNbrExams")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setNbrOfDays(Integer.valueOf(params.get("studentStressNbrDays")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setPeriodDateFormatString(String.valueOf(params.get("studentStressPeriodDateFormat")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
        try {
            setExaminationYear(String.valueOf(params.get("studentStressExaminationYear")));
        } catch (NumberFormatException e) {} catch (NullPointerException e) {}
    }
    
    public int getNbrOfDays() {
        return iNbrOfDays;
    }
  
    public int getNbrOfExams() {
        return iNbrOfExams;
    }
    
    public String getPeriodDateFormatString() {
        return iPeriodDateFormatString;
    }
    
    public void setPeriodDateFormatString(String dateFormatString) {
        iPeriodDateFormatString = dateFormatString;
        iDateFormatter = DateTimeFormatter.ofPattern(iPeriodDateFormatString, iLocale);
    }
    
    public void setNbrOfDays(int nbrOfDays) {
        iNbrOfDays=nbrOfDays;
    }
  
    public void setNbrOfExams(int nbrOfExams) {
        iNbrOfExams=nbrOfExams;
    }
    
    public void setExaminationYear(String year) {
        iExaminationYear = Year.parse(year);
    }
    
    public Year getExaminationYear() {
        return iExaminationYear;
    }
   
    @Override
    public String toString(Assignment<Exam, ExamPlacement> assignment) {
        return "M" 
               + String.valueOf(getNbrOfExams())
               + "I"
               + String.valueOf(getNbrOfDays())
               + "D:" + sDoubleFormat.format(getValue(assignment));
    }
}
