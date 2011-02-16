package net.sf.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Configuration limit constraint. This global constraint ensures that a limit of each
 * configuration is not exceeded. This means that the total sum of weights of course
 * requests (see {@link Request#getWeight()}) enrolled into a configuration is below
 * the configuration's limit (see {@link Config#getLimit()}).
 * 
 * <br>
 * <br>
 * Configurations with negative limit are considered unlimited, and therefore
 * completely ignored by this constraint.
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>ConfigLimit.PreferDummyStudents</td>
 * <td>{@link Boolean}</td>
 * <td>If true, requests of dummy (last-like) students are preferred to be
 * selected as conflicting.</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
public class ConfigLimit extends GlobalConstraint<Request, Enrollment> {
    
    private static double sNominalWeight = 0.00001;
    private boolean iPreferDummyStudents = false;

    /**
     * Constructor
     * 
     * @param cfg
     *            solver configuration
     */
    public ConfigLimit(DataProperties cfg) {
        super();
        iPreferDummyStudents = cfg.getPropertyBoolean("ConfigLimit.PreferDummyStudents", false);
    }


    /**
     * Enrollment weight of a config if the given request is assigned. In order
     * to overcome rounding problems with last-like students ( e.g., 5 students
     * are projected to two configs of limit 2 -- each section can have up to 3
     * of these last-like students), the weight of the request with the highest
     * weight in the config is changed to a small nominal weight.
     * 
     * @param config
     *            a config that is of concern
     * @param request
     *            a request of a student to be assigned containing the given
     *            section
     * @return section's new weight
     */
    public static double getEnrollmentWeight(Config config, Request request) {
        return config.getEnrollmentWeight(request) + request.getWeight()
                - Math.max(config.getMaxEnrollmentWeight(), request.getWeight()) + sNominalWeight;
    }

    /**
     * A given enrollment is conflicting, if the config's enrollment
     * (computed by {@link ConfigLimit#getEnrollmentWeight(Config, Request)})
     * exceeds the limit. <br>
     * If the limit is breached, one or more existing enrollments are
     * (randomly) selected as conflicting until the overall weight is under the
     * limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @param conflicts
     *            all computed conflicting requests are added into this set
     */
    @Override
    public void computeConflicts(Enrollment enrollment, Set<Enrollment> conflicts) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return;
        

        // unlimited config
        if (config.getLimit() < 0)
            return;
        
        // new enrollment weight
        double enrlWeight = getEnrollmentWeight(config, enrollment.getRequest());

        // below limit -> ok
        if (enrlWeight <= config.getLimit())
            return;

        // above limit -> compute adepts (current assignments that are not
        // yet conflicting)
        // exclude all conflicts as well
        List<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments().size());
        for (Enrollment e : config.getEnrollments()) {
            if (e.getRequest().equals(enrollment.getRequest()))
                continue;
            if (conflicts.contains(e))
                enrlWeight -= e.getRequest().getWeight();
            else
                adepts.add(e);
        }

        // while above limit -> pick an adept and make it conflicting
        while (enrlWeight > config.getLimit()) {
            if (adepts.isEmpty()) {
                // no adepts -> enrollment cannot be assigned
                conflicts.add(enrollment);
                return;
            }
            
            // pick adept (prefer dummy students & students w/o reservation), decrease enrollment
            // weight, make conflict
            List<Enrollment> best = new ArrayList<Enrollment>();
            boolean bestDummy = false;
            double bestValue = 0;
            boolean bestRes = false;
            for (Enrollment adept: adepts) {
                boolean dummy = adept.getStudent().isDummy();
                double value = adept.toDouble();
                boolean res = (adept.getReservation() != null);
                
                if (iPreferDummyStudents && dummy != bestDummy) {
                    if (dummy) {
                        best.clear();
                        best.add(adept);
                        bestDummy = dummy;
                        bestValue = value;
                        bestRes = res;
                    }
                    continue;
                }
                
                if (bestRes != res) {
                    if (!res) {
                        best.clear();
                        bestDummy = dummy;
                        bestValue = value;
                        bestRes = res;
                    }
                    continue;
                }

                if (best.isEmpty() || value > bestValue) {
                    if (best.isEmpty()) best.clear();
                    best.add(adept);
                    bestDummy = dummy;
                    bestValue = value;
                    bestRes = res;
                } else if (bestValue == value) {
                    best.add(adept);
                }
            }
            
            Enrollment conflict = ToolBox.random(best);
            adepts.remove(conflict);
            enrlWeight -= conflict.getRequest().getWeight();
            conflicts.add(conflict);
        }
    }

    /**
     * A given enrollment is conflicting, if the config's enrollment (computed by
     * {@link ConfigLimit#getEnrollmentWeight(Config, Request)}) exceeds the
     * limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @return true, if the enrollment cannot be assigned without exceeding the limit
     */
    @Override
    public boolean inConflict(Enrollment enrollment) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return false;

        // unlimited config
        if (config.getLimit() < 0)
            return false;


        // new enrollment weight
        double enrlWeight = getEnrollmentWeight(config, enrollment.getRequest());
        
        // above limit -> conflict
        return (enrlWeight > config.getLimit());
    }
    
    @Override
    public String toString() {
        return "ConfigLimit";
    }

}
