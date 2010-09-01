/**
 * 
 */
package org.sbml.simulator.math.odes;

import eva2.tools.math.Mathematics;

/**
 * For tests
 * 
 * @author draeger
 * 
 */
public class RKEventSolver extends AbstractDESSolver {

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = -2034495479346567501L;

	/**
	 * Stores temporary results for the fourth-order Runge-Kutta method.
	 */
	transient protected double[][] kVals = null;
	/**
	 * Helper variable for the k values.
	 */
	transient protected double[] kHelp;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.tools.math.des.AbstractDESSolver#computeChange(eva2.tools.math.des
	 * .DESystem, double[], double, double, double[])
	 */
	@Override
	public double[] computeChange(DESystem DES, double[] yTemp, double t,
			double h, double[] change) throws IntegrationException {
		int dim = DES.getDESystemDimension();
		if ((kVals == null) || (kVals.length != 4) || (kVals[0].length != dim)) {
			// "static" vectors which are allocated only once
			kVals = new double[4][dim];
			kHelp = new double[dim];
		}

		// k0
		DES.getValue(t, yTemp, kVals[0]);
		Mathematics.svMult(h, kVals[0], kVals[0]);

		// k1
		Mathematics.svvAddScaled(0.5, kVals[0], yTemp, kHelp);
		DES.getValue(t + h / 2, kHelp, kVals[1]);
		Mathematics.svMult(h, kVals[1], kVals[1]);

		// k2
		Mathematics.svvAddScaled(0.5, kVals[1], yTemp, kHelp);
		DES.getValue(t + h / 2, kHelp, kVals[2]);
		Mathematics.svMult(h, kVals[2], kVals[2]);

		// k3
		Mathematics.vvAdd(yTemp, kVals[2], kHelp);
		DES.getValue(t + h, kHelp, kVals[3]);
		Mathematics.svMult(h, kVals[3], kVals[3]);

		// combining all k's
		Mathematics.svvAddScaled(2, kVals[2], kVals[3], kVals[3]);
		Mathematics.svvAddScaled(2, kVals[1], kVals[3], kVals[2]);
		Mathematics.svvAddAndScale(1d / 6d, kVals[0], kVals[2], change);

		return change;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.AbstractDESSolver#getName()
	 */
	@Override
	public String getName() {
		return "Simple RK event solver";
	}

}
