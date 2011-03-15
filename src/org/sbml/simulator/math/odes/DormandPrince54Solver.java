package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.nonstiff.DormandPrince54Integrator;

public class DormandPrince54Solver extends FirstOrderSolver{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2601862472447650296L;

	public DormandPrince54Solver() {
		super();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public DormandPrince54Solver(double stepSize) {
		super(stepSize);
	}
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public DormandPrince54Solver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
	}
	
	/**
	 * 
	 * @param dormandPrinceSolver
	 */
	public DormandPrince54Solver(DormandPrince54Solver princeSolver) {
		super(princeSolver);
		this.integrator=princeSolver.getIntegrator();
	}
	@Override
	public AbstractDESSolver clone() {
		return new DormandPrince54Solver(this);
	}

	@Override
	public String getName() {
		return "DormandPrince54Solver";
	}

	@Override
	public void createIntegrator() {
		integrator=new DormandPrince54Integrator(Math.min(1e-8,Math.min(1.0,getStepSize())), Math.min(1.0,getStepSize()),0.00001, 0.00001);
	}
}
