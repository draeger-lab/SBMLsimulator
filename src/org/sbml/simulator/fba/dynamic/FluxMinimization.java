/*
 * $Id:  FluxMinimization.java 11:34:24 faehnrich$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.fba.dynamic;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

import java.util.logging.Logger;

/**
 * 
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class FluxMinimization extends TargetFunction {
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(FluxMinimization.class.getName());
	
	/*
	 * Save the optimized concentrations vector in a double array
	 */
	private double[] optimizedConcentrations;
	
	/*
	 * Save the optimized flux vector in a double array
	 */
	private double[] optimizedFluxVector;
	
	/*
	 * Save the optimized gibbs energies vector in a double array
	 */
	private double[] optimizedGibbsEnergies;
	
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedConcentrations()
	 */
	@Override
	public double[] getOptimizedConcentrations() {
		return this.optimizedConcentrations;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedFluxVector()
	 */
	@Override
	public double[] getOptimizedFluxVector() {
		return this.optimizedFluxVector;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedGibbsEnergies()
	 */
	@Override
	public double[] getOptimizedGibbsEnergies() {
		return this.optimizedGibbsEnergies;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#isMinProblem()
	 */
	@Override
	public boolean isMinProblem() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#isMaxProblem()
	 */
	@Override
	public boolean isMaxProblem() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#prepareCplex(ilog.cplex.IloCplex)
	 */
	@Override
	public void prepareCplex(IloCplex cplex) throws IloException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#createTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public IloNumExpr createTargetFunction(IloCplex cplex) throws IloException {
		IloNumExpr targetFunction = cplex.numExpr();
		// TODO Auto-generated method stub
		return targetFunction;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#addConstraintsToTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public void addConstraintsToTargetFunction(IloCplex cplex) throws IloException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Assign the solved values to the actual arrays, in fact to the concentrations,
	 * the flux vector and the gibbs energies array.
	 */
	public void assignOptimizedSolution() {
		double[] solution = getSolution();
		/* getSolution() and assign the variables to: optimizedConc., optimizedFluxV. etc.
		 * problem: which variables to which array? */
	}
	
}
