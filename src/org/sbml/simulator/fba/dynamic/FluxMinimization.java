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
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.logging.Logger;

import org.sbml.simulator.stability.math.StoichiometricMatrix;

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
	 * The complete stoichiometric matrix S that is essential for the Tableau algorithm
	 */
	private StoichiometricMatrix S;
	
	/*
	 * The array contains the current interpolated concentrations for this step
	 * of optimization by CPLEX
	 */
	private double[] currentConcentrations;
	
	/*
	 * This vector contains the fluxes that are computed by the Tableau algorithm
	 * applied to S
	 */
	private double[] fluxVector;
	
	/*
	 * The array contains the (read) gibbs energies for the SBML document reactions
	 */
	private double[] gibbsEnergies;
	
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
	
	/*
	 * FluxMinimization target function:
	 * minimize (
	 * ||J||
	 * + lambda_1 * Sum_i^m (c_i - c_eq)²
	 * + lambda_2 * ||L||
	 * + lambda_3 * ||E||
	 * + lambda_4 * ||delta_r G||)
	 */
	
	/**
	 * @return The length of each variable vector in an int array (attend the order!)
	 */
	public int[] getTargetVariablesLengths() {
		int reactionCount = DynamicFBA.expandedDocument.getModel().getReactionCount();
		int speciesCount = DynamicFBA.expandedDocument.getModel().getSpeciesCount();
		
		int[] targetVariablesLength = new int[5];
		targetVariablesLength[0] = 1; //1 variable for the flux vector
		targetVariablesLength[1] = speciesCount; //variables for the concentrations
		targetVariablesLength[2] = reactionCount; //variables for the L vector
		targetVariablesLength[3] = reactionCount; //variables for the error vector
		targetVariablesLength[4] = reactionCount; //variables for the gibbs energies
		
		return targetVariablesLength;
	}
	
	/**
	 * Set default bounds for each part of the target function that will be optimized.
	 */
	public void setDefaultBounds() {
		int fullVariableLength = 0;
		for (int i = 0; i < getTargetVariablesLengths().length; i++) {
			fullVariableLength += getTargetVariablesLengths()[i];
		}
		
		this.lowerBounds = new double[fullVariableLength];
		this.upperBounds = new double[fullVariableLength];
		
		// 1. Flux value bounds
		int fluxPosition = getTargetVariablesLengths()[0] - 1;
		this.lowerBounds[fluxPosition] = 1.0;
		this.upperBounds[fluxPosition] = 10.0;
		
		// 2. Concentration bounds
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		for (int i = concentrationPosition; i < concentrationPosition + getTargetVariablesLengths()[1]; i++) {
			this.lowerBounds[i] = Math.pow(10, -10);
			this.upperBounds[i] = Math.pow(10, -1);
		}
		
		// 3. L vector bounds
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		for (int i = lPosition; i < lPosition + getTargetVariablesLengths()[2]; i++) {
			this.lowerBounds[i] = 0.0;
			this.upperBounds[i] = 100000.0;
		}
		
		// 4. Error bounds
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		for (int i = errorPosition; i < errorPosition + getTargetVariablesLengths()[3]; i++) {
			this.lowerBounds[i] = 0.0;
			this.upperBounds[i] = 100000.0;
		}
		
		// 5. Gibbs energy bounds
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		for (int i = gibbsPosition; i < gibbsPosition + getTargetVariablesLengths()[4]; i++) {
			this.lowerBounds[i] = -100000;
			this.upperBounds[i] = 100;
		}
	}
	
	//TODO write for each bound part a new set method to set bounds!

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#prepareCplex(ilog.cplex.IloCplex)
	 */
	@Override
	public void prepareCplex(IloCplex cplex) throws IloException {
		int fullVariableLength = 0;
		for (int i = 0; i < getTargetVariablesLengths().length; i++) {
			fullVariableLength += getTargetVariablesLengths()[i];
		}
		
		// Set default bounds
		setDefaultBounds();
				
		IloNumVar[] variables = cplex.numVarArray(fullVariableLength, this.lowerBounds, this.upperBounds);
		
		// Set the variables
		setVariables(variables);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#createTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public IloNumExpr createTargetFunction(IloCplex cplex) throws IloException {
		IloNumExpr targetFunction = cplex.numExpr();
		// TODO Auto-generated method stub
		
		// lambda_1 = 10
		
		// lambda_2 = 10
		
		// lambda_3 = 0.01
		
		// lambda_4 = 1.0
		
		return targetFunction;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#addConstraintsToTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public void addConstraintsToTargetFunction(IloCplex cplex) throws IloException {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#assignOptimizedSolution(org.sbml.jsbml.SBMLDocument)
	 */
	@Override
	public void assignOptimizedSolution() {
		double[] solution = getSolution();
		
		// 1. Flux vector assignment
		int fluxPosition = getTargetVariablesLengths()[0] - 1;
		this.optimizedFluxVector = new double[this.fluxVector.length];
		// solution[0] contains the optimized value for the flux vector
		for (int i = 0; i < this.fluxVector.length; i++) {
			this.optimizedFluxVector[i] = solution[fluxPosition] * this.fluxVector[i];
		}
		
		// 2. Concentration vector assignment
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		this.optimizedConcentrations = new double[getTargetVariablesLengths()[1]];
		for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
			this.optimizedConcentrations[i] = solution[concentrationPosition];
		}
		
		// TODO 3. L vector assignment?
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		
		// TODO 4. Error vector assignment?
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		
		// 5. Gibbs energy vector assignment
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		this.optimizedGibbsEnergies = new double[getTargetVariablesLengths()[4]];
		for (int i = 0; i < getTargetVariablesLengths()[4]; i++) {
			this.optimizedGibbsEnergies[i] = solution[gibbsPosition];
		}
		
	}
	
}
