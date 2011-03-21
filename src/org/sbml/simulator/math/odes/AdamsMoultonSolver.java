package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.nonstiff.AdamsMoultonIntegrator;

public class AdamsMoultonSolver extends FirstOrderSolver{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2601862472447650296L;

	public AdamsMoultonSolver() {
		super();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public AdamsMoultonSolver(double stepSize) {
		super(stepSize);
	}
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public AdamsMoultonSolver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
	}
	
	/**
	 * 
	 * @param adamsSolver
	 */
	public AdamsMoultonSolver(AdamsMoultonSolver adamsSolver) {
		super(adamsSolver);
		this.integrator=adamsSolver.getIntegrator();
	}
	@Override
	public AbstractDESSolver clone() {
		return new AdamsMoultonSolver(this);
	}

	@Override
	public String getName() {
		return "AdamsMoultonSolver";
	}

	@Override
	protected void createIntegrator() {
		integrator=new AdamsMoultonIntegrator(5, Math.min(1e-8,Math.min(1.0,getStepSize())), Math.min(1.0,getStepSize()), 0.00001, 0.00001);
	}

}
