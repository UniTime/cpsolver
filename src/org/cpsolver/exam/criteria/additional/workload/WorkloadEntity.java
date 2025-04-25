package org.cpsolver.exam.criteria.additional.workload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Entity for workload calculations.
 * 
 * Models entities with a daily workload.
 * 
 * @author Alexander Kreim
 * 
 *  <br>
 *         This library is free software; you can redistribute it and/or modify
 *         it under the terms of the GNU Lesser General Public License as
 *         published by the Free Software Foundation; either version 3 of the
 *         License, or (at your option) any later version. <br>
 *  <br>
 *         This library is distributed in the hope that it will be useful, but
 *         WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *         Lesser General Public License for more details. <br>
 *  <br>
 *         You should have received a copy of the GNU Lesser General Public
 *         License along with this library; if not see <a href=
 *         'http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class WorkloadEntity {
    
    private int nbrOfDays; 
    private int nbrOfExams;
    private String name; 
    private List<Integer> loadPerDay;
    
    public WorkloadEntity() {}
    
    public int getNbrOfDays() {
        return nbrOfDays;
    }
    public void setNbrOfDays(int nbrOfDays) {
        this.nbrOfDays = nbrOfDays;
    }
    public int getNbrOfExams() {
        return nbrOfExams;
    }
    public void setNbrOfExams(int nbrOfExams) {
        this.nbrOfExams = nbrOfExams;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<Integer> getLoadPerDay() {
        return loadPerDay;
    }
    
    public void setLoadPerDay(List<Integer> loadPerDay) {
        this.loadPerDay = loadPerDay;
    }
    
    public void initLoadPerDay(int nbrOfExamDays) {
        this.loadPerDay = new ArrayList<Integer>();
        for (int i = 0; i < nbrOfExamDays; i++) {
            loadPerDay.add(0);
        }
    }
    
    public void incrementItemWorkloadForDay(int dayIndex) {
        int newDayLoad = loadPerDay.get(dayIndex) + 1;
        loadPerDay.set(dayIndex, newDayLoad);
    }   
    
    public int getWorkload() {
        int workload = 0;
        if (loadPerDay != null ) {
            List<Integer> rollingSums = WorkloadUtils.rollingSumInt(loadPerDay, nbrOfDays);
            workload = WorkloadUtils.numberOfValuesLargerThanThreshold(rollingSums, nbrOfExams); 
        }
        return workload;
    }
    
    public String toCSVString() {
        if (loadPerDay != null) {
            String retval = "";
            retval += getName() + ",";
            for (Iterator<Integer> iterator = loadPerDay.iterator(); iterator.hasNext();) {
                Integer load = iterator.next();
                retval += load.toString() + ",";
            }
            retval += getWorkload();
            return retval;
        }
        return null;
    }

    public void resetLoadPerDay(int nbrOfExamDays) {
        if (this.getLoadPerDay() == null) {
            this.initLoadPerDay(nbrOfExamDays);
        } else {
            if (this.getLoadPerDay().size() != nbrOfExamDays) {
               this.initLoadPerDay(nbrOfExamDays);
            } else {
                for (int i = 0; i < getLoadPerDay().size(); i++) {
                    getLoadPerDay().set(i, 0);
                } 
            }
        }
    }
}
