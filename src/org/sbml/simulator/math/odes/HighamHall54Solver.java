package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.nonstiff.HighamHall54Integrator;

public class HighamHall54Solver extends FirstOrderSolver{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2601862472447650296L;

	public HighamHall54Solver() {
		super();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public HighamHall54Solver(double stepSize) {
		super(stepSize);
	}
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public HighamHall54Solver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
	}
	
	/**
	 * 
	 * @param dormandPrinceSolver
	 */
	public HighamHall54Solver(HighamHall54Solver solver) {
		super(solver);
		this.integrator=solver.getIntegrator();
	}
	@Override
	public AbstractDESSolver clone() {
		return new HighamHall54Solver(this);
	}

	@Override
	public String getName() {
		return "HighamHall54Solver";
	}

	@Override
	protected void createIntegrator() {
		integrator=new HighamHall54Integrator(Math.min(1e-8,Math.min(1.0,getStepSize())), Math.min(1.0,getStepSize()),0.5, 0.01);
	}
}
