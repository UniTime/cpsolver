package net.sf.cpsolver.ifs.example.csp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Random Binary CSP with kernels. <br>
 * <br>
 * This class only implements the generation of Structured CSP problem.<br>
 * In Structured CSP, variables are divided into several kernels (some variables
 * may remain ouside kernels). Different constraints (in density and tightnes)
 * are generated according to whether variables are from the same kernel or not. <br>
 * <br>
 * Model parameters: <br>
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>CSP.NrVariables</td>
 * <td>{@link Integer}</td>
 * <td>Number of variables</td>
 * </tr>
 * <tr>
 * <td>CSP.DomainSize</td>
 * <td>{@link Integer}</td>
 * <td>Number of values for each variable</td>
 * </tr>
 * <tr>
 * <td>CSP.NrKernels</td>
 * <td>{@link Integer}</td>
 * <td>Number of kernels</td>
 * </tr>
 * <tr>
 * <td>CSP.KernelSize</td>
 * <td>{@link Integer}</td>
 * <td>Number of variables in each kernel</td>
 * </tr>
 * <tr>
 * <td>CSP.Tightness</td>
 * <td>{@link Double}</td>
 * <td>Tightness of constraints outside kernels</td>
 * </tr>
 * <tr>
 * <td>CSP.KernelTightness</td>
 * <td>{@link Double}</td>
 * <td>Tightness of constraints inside a kernel</td>
 * </tr>
 * <tr>
 * <td>CSP.Density</td>
 * <td>{@link Double}</td>
 * <td>Density of constraints outside kernels</td>
 * </tr>
 * <tr>
 * <td>CSP.KernelDensity</td>
 * <td>{@link Double}</td>
 * <td>Density of constraints inside a kernel</td>
 * </tr>
 * <tr>
 * <td>General.MPP</td>
 * <td>{@link String}</td>
 * <td>Minimal perturbation problem --> generate initial assignment</td>
 * </tr>
 * </table>
 * <br>
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
public class StructuredCSPModel extends Model<CSPVariable, CSPValue> {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(StructuredCSPModel.class);
    private DataProperties iProperties = null;

    /** Constructor */
    public StructuredCSPModel(DataProperties properties, long seed) {
        iProperties = properties;
        generate(seed);
    }

    private void swap(CSPVariable[][] allPairs, int first, int second) {
        CSPVariable[] a = allPairs[first];
        allPairs[first] = allPairs[second];
        allPairs[second] = a;
    }

    private void buildBinaryConstraintGraph(List<CSPVariable> variables, List<CSPBinaryConstraint> constraints,
            Random rnd) {
        int numberOfAllPairs = variables.size() * (variables.size() - 1) / 2;
        CSPVariable[][] allPairs = new CSPVariable[numberOfAllPairs][];
        int idx = 0;
        for (CSPVariable v1 : variables) {
            for (CSPVariable v2 : variables) {
                if (v1.getId() >= v2.getId())
                    continue;
                allPairs[idx++] = new CSPVariable[] { v1, v2 };
            }
        }
        for (idx = 0; idx < constraints.size(); idx++) {
            swap(allPairs, idx, idx + (int) (rnd.nextDouble() * (numberOfAllPairs - idx)));
        }
        idx = 0;
        for (CSPBinaryConstraint c : constraints) {
            c.addVariable(allPairs[idx][0]);
            c.addVariable(allPairs[idx][1]);
            idx++;
        }
    }

    private void buildBinaryConstraintGraph2(List<CSPVariable> variables, int numberOfAllPairs,
            List<CSPBinaryConstraint> constraints, Random rnd) {
        CSPVariable[][] allPairs = new CSPVariable[numberOfAllPairs][];
        int idx = 0;
        for (CSPVariable v1 : variables) {
            for (CSPVariable v2 : variables) {
                if (v1.getId() >= v2.getId())
                    continue;
                if (v1.getKernelId() >= 0 && v1.getKernelId() == v2.getKernelId())
                    continue;
                allPairs[idx++] = new CSPVariable[] { v1, v2 };
            }
        }
        for (idx = 0; idx < constraints.size(); idx++) {
            swap(allPairs, idx, idx + (int) (rnd.nextDouble() * (numberOfAllPairs - idx)));
        }
        idx = 0;
        for (CSPBinaryConstraint c : constraints) {
            c.addVariable(allPairs[idx][0]);
            c.addVariable(allPairs[idx][1]);
            idx++;
        }
    }

    @SuppressWarnings("unchecked")
    private void generate(long seed) {
        int nrVariables = iProperties.getPropertyInt("CSP.NrVariables", 60);
        int nrValues = iProperties.getPropertyInt("CSP.DomainSize", 15);
        int nrKernels = iProperties.getPropertyInt("CSP.NrKernels", 2);
        int nrKernelVariables = iProperties.getPropertyInt("CSP.KernelSize", 8);

        int nrPairValues = nrValues * nrValues;
        float tightnessPerc = iProperties.getPropertyFloat("CSP.Tightness", 0.01f);
        float kernelTightnessPerc = iProperties.getPropertyFloat("CSP.KernelTightness", 0.097f);
        int nrCompatiblePairs = (int) Math.round((1.0 - tightnessPerc) * nrPairValues);
        int kernelNrCompatiblePairs = (int) Math.round((1.0 - kernelTightnessPerc) * nrPairValues);

        int nrPairVariables = (nrVariables * (nrVariables - 1)) / 2;
        int nrPairKernelVariables = (nrKernelVariables * (nrKernelVariables - 1)) / 2;
        nrPairVariables -= nrKernels * nrPairKernelVariables;
        float densityPerc = iProperties.getPropertyFloat("CSP.Density", 0.01f);
        float densityKernelPerc = iProperties.getPropertyFloat("CSP.KernelDensity", 0.097f);
        int density = Math.round(densityPerc * nrPairVariables);
        int kernelDensity = Math.round(densityKernelPerc * nrPairKernelVariables);

        Random rnd = new Random(seed);
        List<CSPVariable> generalVariables = new ArrayList<CSPVariable>(nrVariables - (nrKernels * nrKernelVariables));
        int varId = 1;
        for (int i = 0; i < nrVariables - (nrKernels * nrKernelVariables); i++) {
            CSPVariable var = new CSPVariable(varId++, nrValues);
            generalVariables.add(var);
            addVariable(var);
        }
        sLogger.debug("Created " + generalVariables.size() + " general variables.");
        List<CSPVariable>[] kernelVariables = new ArrayList[nrKernels];
        for (int k = 0; k < nrKernels; k++) {
            kernelVariables[k] = new ArrayList<CSPVariable>(nrKernelVariables);
            for (int i = 0; i < nrKernelVariables; i++) {
                CSPVariable var = new CSPVariable(varId++, nrValues, k);
                kernelVariables[k].add(var);
                addVariable(var);
            }
            if (k == 0)
                sLogger.debug("Created " + kernelVariables[0].size() + " kernel variables (per kernel).");
        }
        sLogger.debug("Created " + variables().size() + " variables at total.");
        int constId = 1;
        List<CSPBinaryConstraint> generalConstraints = new ArrayList<CSPBinaryConstraint>(density);
        for (int i = 0; i < density; i++) {
            CSPBinaryConstraint c = new CSPBinaryConstraint(constId++, nrCompatiblePairs);
            generalConstraints.add(c);
            addConstraint(c);
        }
        sLogger.debug("Created " + generalConstraints.size() + " general constraints (tightness=" + tightnessPerc
                + ").");
        List<CSPBinaryConstraint>[] kernelConstraints = new List[nrKernels];
        for (int k = 0; k < nrKernels; k++) {
            kernelConstraints[k] = new ArrayList<CSPBinaryConstraint>(kernelDensity);
            for (int i = 0; i < kernelDensity; i++) {
                CSPBinaryConstraint c = new CSPBinaryConstraint(constId++, kernelNrCompatiblePairs);
                kernelConstraints[k].add(c);
                addConstraint(c);
            }
            if (k == 0)
                sLogger.debug("Created " + kernelConstraints[0].size() + " kernel constraints (per kernel, tightness="
                        + kernelTightnessPerc + ").");
        }
        sLogger.debug("Created " + constraints().size() + " constraints at total.");

        for (int k = 0; k < nrKernels; k++) {
            buildBinaryConstraintGraph(kernelVariables[k], kernelConstraints[k], rnd);
        }
        buildBinaryConstraintGraph2(variables(), nrPairVariables, generalConstraints, rnd);

        for (Constraint<CSPVariable, CSPValue> c : constraints()) {
            CSPBinaryConstraint constraint = (CSPBinaryConstraint) c;
            constraint.init(rnd);
        }

        if (iProperties.getPropertyBoolean("General.MPP", false)) {
            for (CSPVariable variable : variables()) {
                variable.generateInitialValue(rnd);
            }
        }
    }

    /** Return information table */
    @Override
    public Map<String, String> getInfo() {
        Map<String, String> ret = super.getInfo();
        ret.put("Solution value", String.valueOf(getTotalValue()));
        return ret;
    }
}
