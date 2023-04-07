package org.cpsolver.exam.reports;

import java.util.ArrayList;
import java.util.List;

import org.cpsolver.exam.criteria.StudentBackToBackConflicts;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPeriod;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.CSVFile.CSVField;

public class ExamModelReportGenerator {
    private ExamModel iModel;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          el;

    public ExamModelReportGenerator(ExamModel model) {
        iModel = model;
    }

    public CSVFile generateReport(Assignment<Exam, ExamPlacement> assignment) {
        CSVFile csv = new CSVFile();
        List<CSVField> header = new ArrayList<CSVField>();
        header.add(new CSVField("Date"));
        header.add(new CSVField("None"));
        boolean isDayBreakBackToBack = ((StudentBackToBackConflicts)iModel.getCriterion(StudentBackToBackConflicts.class)).isDayBreakBackToBack();
        for (int i = 1; i <= 5; i++)
            header.add(new CSVField(i == 5 ? "5+" : String.valueOf(i)));
        header.add(new CSVField("Back-To-Back"));
        csv.setHeader(header);
        int[] nrExamsTotal = new int[6];
        for (int i = 0; i <= 5; i++)
            nrExamsTotal[i] = 0;
        int btbTotal = 0;
        for (int d = 0; d < iModel.getNrDays(); d++) {
            ExamPeriod period = null;
            for (ExamPeriod p : iModel.getPeriods()) {
                if (p.getDay() == d) {
                    period = p;
                    break;
                }
            }
            int[] nrExams = new int[6];
            for (int i = 0; i <= 5; i++)
                nrExams[i] = 0;
            int btb = 0;
            for (ExamStudent student : iModel.getStudents()) {
                int ex = student.getExamsADay(assignment, d).size();
                nrExams[ex <= 5 ? ex : 5]++;
                ExamPeriod p = period;
                while (p.next() != null && (isDayBreakBackToBack ? p : p.next()).getDay() == d) {
                    btb += student.getExams(assignment, p).size() * student.getExams(assignment, p.next()).size();
                    p = p.next();
                }
            }
            List<CSVField> line = new ArrayList<CSVField>();
            line.add(new CSVField(period.getDayStr()));
            for (int i = 0; i <= 5; i++) {
                line.add(new CSVField(nrExams[i]));
                nrExamsTotal[i] += nrExams[i];
            }
            line.add(new CSVField(btb));
            btbTotal += btb;
            csv.addLine(line);
        }
        List<CSVField> line = new ArrayList<CSVField>();
        line.add(new CSVField("Total"));
        for (int i = 0; i <= 5; i++)
            line.add(new CSVField(nrExamsTotal[i]));
        line.add(new CSVField(btbTotal));
        csv.addLine(line);
        return csv;
    }
    
}
