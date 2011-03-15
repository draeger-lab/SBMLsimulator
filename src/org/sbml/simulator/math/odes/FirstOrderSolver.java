package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.AbstractIntegrator;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.util.FastMath;

import eva2.tools.math.Mathematics;

public abstract class FirstOrderSolver extends AbstractDESSolver{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected AbstractIntegrator integrator;
	
	
	public abstract void createIntegrator();
	public FirstOrderSolver() {
		super();
		createIntegrator();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public FirstOrderSolver(double stepSize) {
		super(stepSize);
		createIntegrator();
	}
	
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public FirstOrderSolver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
		createIntegrator();
	}
	
	/**
	 * 
	 * @param firstOrderSolver
	 */
	public FirstOrderSolver(FirstOrderSolver firstOrderSolver) {
		super(firstOrderSolver);
		createIntegrator();
	}
	
	@Override
	public double[] computeChange(DESystem DES, double[] y, double t,
			double stepSize, double[] change) throws IntegrationException {
		
		double[] result = new double[y.length];
		
		try {
			double tstart=t;
			double tend=t+stepSize;
			if(FastMath.abs(tstart - tend) <= 1.0e-12 * FastMath.max(FastMath.abs(tstart), FastMath.abs(tend))) {
				for(int i=0;i!=change.length;i++) {
					change[i]=0;
				}
			}
			else {
				integrator.integrate(DES, tstart, y, tend, result);
				Mathematics.vvSub(result, y, change);
			}
		} catch (DerivativeException e) {
			e.printStackTrace();
		} catch (IntegratorException e) {
			e.printStackTrace();
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
