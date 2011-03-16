package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.nonstiff.AdamsBashforthIntegrator;

public class AdamsBashforthSolver extends FirstOrderSolver{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2601862472447650296L;

	public AdamsBashforthSolver() {
		super();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public AdamsBashforthSolver(double stepSize) {
		super(stepSize);
	}
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public AdamsBashforthSolver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
	}
	
	/**
	 * 
	 * @param adamsSolver
	 */
	public AdamsBashforthSolver(AdamsBashforthSolver adamsSolver) {
		super(adamsSolver);
		this.integrator=adamsSolver.getIntegrator();
	}
	
	@Override
	public AbstractDESSolver clone() {
		return new AdamsBashforthSolver(this);
	}

	@Override
	public String getName() {
		return "AdamsBashforthSolver";
	}

	@Override
	public void createIntegrator() {
		integrator=new AdamsBashforthIntegrator(5,  Math.min(1e-8,Math.min(1.0,getStepSize())), Math.min(1.0,getStepSize()),0.00001, 0.00001);
	}

	

}
