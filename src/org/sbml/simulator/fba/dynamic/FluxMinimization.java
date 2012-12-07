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

import org.sbml.jsbml.SBMLDocument;

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
	 * The array contains the current interpolated concentrations for this step
	 * of optimization by CPLEX
	 */
	private double[] currentConcentrations;
	
	/*
	 * This vector contains the fluxes that are computed by the Tableau algorithm applied to S
	 */
	private double[] fluxVector;
	
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
	
	/*
	 * Lower bounds for CPLEX variables saved in a double array
	 */
	private double[] lowerBounds;
	
	/*
	 * Upper bounds for CPLEX variables saved in a double array
	 */
	private double[] upperBounds;
	
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#setCurrenConcentrations(double[])
	 */
	@Override
	public void setCurrentConcentrations(double[] currentConcentrations) {
		this.currentConcentrations = currentConcentrations;
	}
	
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
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#assignOptimizedSolution(org.sbml.jsbml.SBMLDocument)
	 */
	@Override
	public void assignOptimizedSolution(SBMLDocument document) {
		// TODO method is incomplete! Check for other assignments!
		double[] solution = getSolution();
		
		this.optimizedFluxVector = new double[this.fluxVector.length];
		// solution[0] contains the optimized value for the flux vector
		for (int i=0; i<this.fluxVector.length; i++) {
			this.optimizedFluxVector[i] = solution[0] * this.fluxVector[i];
		}
		
		int concLength = document.getModel().getSpeciesCount();
		this.optimizedConcentrations = new double[concLength];
		for (int i=0; i<concLength; i++) {
			this.optimizedConcentrations[i] = solution[i+1];
		}
		
		int gibbsLength = document.getModel().getReactionCount();
		this.optimizedGibbsEnergies = new double[gibbsLength];
		for (int i=0; i<gibbsLength; i++) {
			this.optimizedGibbsEnergies[i] = solution[i+1+concLength];
		}
		
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#prepareCplex(ilog.cplex.IloCplex)
	 */
	@Override
	public void prepareCplex(IloCplex cplex) throws IloException {
		// TODO Auto-generated method stub
		// IloNumVar[] variables = cplex.numVarArray(...length, this.lowerBounds, this.upperBounds);
		// ...
		// (set variables with setVariables(IloNumVar[] vars))
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
	
}
