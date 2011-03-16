package org.sbml.simulator.math.odes;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.ode.AbstractIntegrator;
import org.apache.commons.math.util.FastMath;

import eva2.tools.math.Mathematics;

public abstract class FirstOrderSolver extends AbstractDESSolver{
	
	private static final Logger logger = Logger.getLogger(FirstOrderSolver.class.getName());
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected AbstractIntegrator integrator;
	
	
	protected abstract void createIntegrator();
	public FirstOrderSolver() {
		super();
		createIntegrator();
		addHandler();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public FirstOrderSolver(double stepSize) {
		super(stepSize);
		createIntegrator();
		addHandler();
	}
	
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public FirstOrderSolver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
		createIntegrator();
		addHandler();
	}
	
	/**
	 * 
	 * @param firstOrderSolver
	 */
	public FirstOrderSolver(FirstOrderSolver firstOrderSolver) {
		super(firstOrderSolver);
		createIntegrator();
		addHandler();
		
	}
	
	private void addHandler() {
		integrator.addEventHandler(this, 1, 1, 1);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.simulator.math.odes.AbstractDESSolver#computeChange(org.sbml
	 * .simulator.math.odes.DESystem, double[], double, double, double[])
	 */
	@Override
	public double[] computeChange(DESystem DES, double[] y, double t,
			double stepSize, double[] change) throws IntegrationException {

		double[] result = new double[y.length];

		double tstart = t;
		double tend = t + stepSize;
		if (FastMath.abs(tstart - tend) <= (1.0e-12 * FastMath.max(
				FastMath.abs(tstart), FastMath.abs(tend)))) {
			for (int i = 0; i != change.length; i++) {
				change[i] = 0;
			}
		} else {
			try {
				integrator.integrate(DES, tstart, y, tend, result);
				Mathematics.vvSub(result, y, change);
			} catch (Exception e) {
				setUnstableFlag(true);
				logger.log(Level.WARNING, e.getMessage());
			}
		}
		return change;
	}
	
	/**
	 * 
	 * @return integrator
	 */
	public AbstractIntegrator getIntegrator() {
		return integrator;
	}

}
