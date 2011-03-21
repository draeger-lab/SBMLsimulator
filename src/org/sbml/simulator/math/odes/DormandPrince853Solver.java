package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;

public class DormandPrince853Solver extends FirstOrderSolver{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2601862472447650296L;

	public DormandPrince853Solver() {
		super();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public DormandPrince853Solver(double stepSize) {
		super(stepSize);
	}
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public DormandPrince853Solver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
	}
	
	/**
	 * 
	 * @param dormandPrinceSolver
	 */
	public DormandPrince853Solver(DormandPrince853Solver princeSolver) {
		super(princeSolver);
		this.integrator=princeSolver.getIntegrator();
	}
	@Override
	public AbstractDESSolver clone() {
		return new DormandPrince853Solver(this);
	}

	@Override
	public String getName() {
		return "DormandPrince853Solver";
	}

	@Override
	protected void createIntegrator() {
		integrator=new DormandPrince853Integrator(Math.min(1e-8,Math.min(1.0,getStepSize())), Math.min(1.0,getStepSize()),0.00001, 0.00001);
	}

}
