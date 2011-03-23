package org.sbml.simulator.math.odes;

import org.apache.commons.math.ode.nonstiff.AdamsBashforthIntegrator;

/**
 * 
 * @author Roland Keller
 * @version $Rev$
 * @since 1.0
 */
public class AdamsBashforthSolver extends FirstOrderSolver{
	
        /**
	 * Generated serial version identifier. 
	 */
	private static final long serialVersionUID = -2601862472447650296L;

	/**
	 * 
	 */
	public AdamsBashforthSolver() {
	    super();
	}
	
	/**
	 * 
	 * @param adamsSolver
	 */
	public AdamsBashforthSolver(AdamsBashforthSolver adamsSolver) {
		super(adamsSolver);
		this.integrator=adamsSolver.getIntegrator();
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
	
	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.math.odes.FirstOrderSolver#clone()
	 */
	@Override
	public AdamsBashforthSolver clone() {
		return new AdamsBashforthSolver(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.math.odes.FirstOrderSolver#createIntegrator()
	 */
	@Override
	public void createIntegrator() {
		integrator=new AdamsBashforthIntegrator(5,  Math.min(1e-8,Math.min(1.0,getStepSize())), Math.min(1.0,getStepSize()),0.00001, 0.00001);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.math.odes.AbstractDESSolver#getName()
	 */
	@Override
	public String getName() {
		return "Adams-Bashforth solver";
	}

	

}
