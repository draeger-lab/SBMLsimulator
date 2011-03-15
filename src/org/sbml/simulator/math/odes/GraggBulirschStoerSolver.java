package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegrator;

public class GraggBulirschStoerSolver extends FirstOrderSolver{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2601862472447650296L;

	public GraggBulirschStoerSolver() {
		super();
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public GraggBulirschStoerSolver(double stepSize) {
		super(stepSize);
	}
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public GraggBulirschStoerSolver(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
		
	}
	
	/**
	 * 
	 * @param graggBulirschStoerSolver
	 */
	public GraggBulirschStoerSolver(GraggBulirschStoerSolver solver) {
		super(solver);
		this.integrator=solver.getIntegrator();
	}
	@Override
	public AbstractDESSolver clone() {
		return new GraggBulirschStoerSolver(this);
	}

	@Override
	public String getName() {
		return "GraggBulirschStoerSolver";
	}

	@Override
	public void createIntegrator() {
		integrator=new GraggBulirschStoerIntegrator(Math.min(1e-8,Math.min(1.0,getStepSize())), Math.min(1.0,getStepSize()),0.00001, 0.00001);
	}

}
