/*
 * $Id:  FluxMinimizationII.java 00:24:09 faehnrich$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2012 by the University of Tuebingen, Germany.
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

import java.util.logging.Logger;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class FluxMinimizationII extends TargetFunction {
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(FluxMinimizationII.class.getName());
	
	/*
	 * The expanded SBML document, in fact:
	 * - transport reactions eliminated and reversible reactions split,
	 * - compensated reactions added (computed from system boundaries)
	 */
	private SBMLDocument expandedDocument;
	
	/*
	 * The complete intern {@link StoichiometricMatrix} N (with system
	 * boundaries) that is essential for the Tableau algorithm
	 */
	private StoichiometricMatrix N_int_sys;
	
	/*
	 * This vector contains the fluxes that are computed by the Tableau algorithm
	 * applied to N_int_sys
	 */
	private double[] computedFluxVector;
	
	/*
	 * The array contains the current (t_i+1) interpolated concentrations
	 * for this step of optimization by CPLEX
	 */
	private double[] currentConcentrations;
	
	/*
	 * The array contains the concentrations of the last point in time (t_i)
	 */
	private double[] lastConcentrations = null;
	
	/*
	 * The array contains the read system boundaries
	 */
	private double[] readSystemBoundaries;
	
	/*
	 * If the system boundaries are read from file, set system boundaries and
	 * <CODE>true</CODE>
	 */
	private boolean isSystemBoundaries = false;
	
	/*
	 * This object saves the optimized solution, in fact:
	 * - the optimized fluxes in a double[]
	 * - the optimized concentrations in a double[]
	 */
	private double[][] optimizedSolution;
	
	/*
	 * Lower bounds for CPLEX variables saved in a double array
	 */
	private double[] lowerBounds;
	
	/*
	 * Upper bounds for CPLEX variables saved in a double array
	 */
	private double[] upperBounds;
	
	/*
	 * Constraint J_j >= 0
	 */
	private boolean constraintJ0 = true;
	
	/*
	 * Constraint z_m (t_i+1) >= 0
	 */
	private boolean constraintZm = true;

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#setCurrentConcentrations(double[])
	 */
	@Override
	public void setCurrentConcentrations(double[] concentrations) {
		this.currentConcentrations = concentrations;
	}
	
	/**
	 * Set the read system boundaries.
	 * 
	 * @param systemBoundaries
	 */
	public void setReadSystemBoundaries(double[] systemBoundaries) {
		this.readSystemBoundaries = systemBoundaries;
		this.isSystemBoundaries = true;
	}
	
	/**
	 * Prepare the FluxMinimization by setting the:
	 * - expanded SBML document
	 * - matrix N_int_sys
	 * - computed flux vector (Tableau algorithm)
	 * all from the (read) system boundaries.
	 * 
	 * @throws Exception
	 */
	private void prepareFluxMinimizationII() throws Exception {
		// If system boundaries are read from file...
		if (this.isSystemBoundaries) {
			this.expandedDocument = FluxMinimizationUtils.getExpandedDocument(DynamicFBA.originalDocument, this.readSystemBoundaries);
			this.N_int_sys = FluxMinimizationUtils.getExpandedStoichiometricMatrix(DynamicFBA.originalDocument, this.readSystemBoundaries);
			// Compute (with Tableau algorithm) and set flux vector
			this.computedFluxVector = FluxMinimizationUtils.computeFluxVector(this.N_int_sys, null, this.expandedDocument);
		} else {
			//... or aren't available
			this.expandedDocument = FluxMinimizationUtils.getExpandedDocument(DynamicFBA.originalDocument);
			this.N_int_sys = FluxMinimizationUtils.getExpandedStoichiometricMatrix(DynamicFBA.originalDocument);
			// Compute (with Tableau algorithm) and set flux vector
			this.computedFluxVector = FluxMinimizationUtils.computeFluxVector(this.N_int_sys, null, this.expandedDocument);
		}
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
	
	/**
	 * Enable/Disable constraint J_j >= 0
	 * 
	 * @param constraintJ0
	 */
	public void setConstraintJ0(boolean constraintJ0) {
		this.constraintJ0 = constraintJ0;
	}
	
	/**
	 * Enable/Disable constraint z_m (t_i+1) >= 0
	 * 
	 * @param constraintZm
	 */
	public void setConstraintZm(boolean constraintZm) {
		this.constraintZm = constraintZm;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getTargetVariablesIds()
	 */
	@Override
	public String[][] getTargetVariablesIds() {
		// Species Ids for the concentrations block
		ListOf<Species> listOfSpecies = this.expandedDocument.getModel().getListOfSpecies();
		int speciesCount = this.expandedDocument.getModel().getSpeciesCount();
		String[] speciesIds = new String[speciesCount];
		for (int i = 0; i < speciesCount; i++) {
			speciesIds[i] = listOfSpecies.get(i).getId();
		}

		// Reaction Ids for the fluxes and gibbs energies block
		ListOf<Reaction> listOfReactions = this.expandedDocument.getModel().getListOfReactions();
		int reactionCount = this.expandedDocument.getModel().getReactionCount();
		String[] reactionIds = new String[reactionCount];
		for (int i = 0; i < reactionCount; i++) {
			reactionIds[i] = listOfReactions.get(i).getId();
		}

		String[][] targetVariablesIds = new String[2][];
		targetVariablesIds[0] = speciesIds; // The concentration ID's
		targetVariablesIds[1] = reactionIds; // The flux vector ID's

		return targetVariablesIds;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getTargetVariablesLengths()
	 */
	@Override
	public int[] getTargetVariablesLengths() {
		int speciesCount = this.expandedDocument.getModel().getSpeciesCount();
		
		int[] targetVariablesLength = new int[2];
		targetVariablesLength[0] = 1; //1 variable for the flux vector
		targetVariablesLength[1] = speciesCount; //variables for the concentrations
		
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
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#initCplexVariables(ilog.cplex.IloCplex)
	 */
	@Override
	public void initCplexVariables(IloCplex cplex) throws IloException {
		// Prepare the FluxMinimization
		try {
			prepareFluxMinimizationII();
		} catch (Exception e) {
			e.printStackTrace();
		}

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
		IloNumExpr function = cplex.numExpr();
		
		// One variable for all fluxes in the flux vector
		IloNumExpr flux = cplex.numExpr();
		int fluxPosition = getTargetVariablesLengths()[0] - 1;
		// Manhattan norm included
		for (int i = 0; i < this.computedFluxVector.length; i++) {
			flux = cplex.sum(flux, cplex.abs(cplex.prod(this.computedFluxVector[i], getVariables()[0])));
		}
		
		// Concentrations 
		IloNumExpr concentrations = cplex.numExpr();
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		double[] c_m_measured = this.currentConcentrations;
		
		for (int n = 0; n < getTargetVariablesLengths()[1]; n++) {
			IloNumExpr optimizingConcentration = cplex.numExpr();
			
			if (!Double.isNaN(c_m_measured[n])) {
				optimizingConcentration = cplex.diff(c_m_measured[n], getVariables()[n + concentrationPosition]);
			} else {
				// TODO if c_m_measured[n] is NaN???
			}
			
			concentrations = cplex.sum(concentrations, optimizingConcentration);
		}
		
		// Sum up each term
		function = cplex.sum(flux, concentrations);
		
		return function;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#addConstraintsToTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public void addConstraintsToTargetFunction(IloCplex cplex) throws IloException {
		int reactionCount = this.expandedDocument.getModel().getReactionCount();
		int speciesCount = this.expandedDocument.getModel().getSpeciesCount();

		int fluxPosition = getTargetVariablesLengths()[0] - 1;
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		
		// Constraint J_j >= 0
		if (this.constraintJ0 == true) {
			for (int j = 0; j < reactionCount; j++) {
				// Flux J_j
				IloNumExpr j_j = cplex.prod(this.computedFluxVector[j], getVariables()[fluxPosition]);

				cplex.addGe(j_j, 0);
			}
		}
		
		// Constraint z_m (t_i+1) >= 0
		if (this.constraintZm == true) {
			for (int m = 0; m < speciesCount; m++) {
				// Concentration z_m (t_i+1)
				IloNumExpr z_m = getVariables()[m + concentrationPosition];
				
				cplex.addGe(z_m, 0);
			}
		}
		
		// Computation of z_m (t_i+1)
		
		// Use this computation of delta_t:
		// Only if each timepoint has the same distance to its neighboring timepoint
		double delta_t = DynamicFBA.dFBATimePoints[1] - DynamicFBA.dFBATimePoints[0];
		
		for (int n = 0; n < getTargetVariablesLengths()[1]; n++) {
			
			if (this.lastConcentrations == null) {
				// The array only is empty for the very first point in time
				if (!Double.isNaN(this.currentConcentrations[n])) {
					cplex.addEq(getVariables()[n + concentrationPosition], cplex.constant(this.currentConcentrations[n]));
					} else {
						// TODO if currentConcentrations[n] is NaN???
					}
				} else {
				// In all other cases
				if (!Double.isNaN(this.lastConcentrations[n])) {
					IloNumExpr computedConcentration = cplex.numExpr();
					IloNumExpr NJ = cplex.numExpr();
					
					for (int col = 0; col < this.N_int_sys.getColumnDimension(); col++) {
						double[] currentN_row = this.N_int_sys.getRow(n);
						NJ = cplex.sum(NJ, cplex.prod(cplex.constant(currentN_row[col]), cplex.prod(this.computedFluxVector[col], getVariables()[fluxPosition])));
					}
					// TODO check if concentration n is compatible with currentN_row
					computedConcentration = cplex.sum(cplex.constant(this.lastConcentrations[n]), cplex.prod(NJ, cplex.constant(delta_t)));
					cplex.addEq(getVariables()[n + concentrationPosition], computedConcentration);
				} else {
					// TODO if lastConcentrations[n] is NaN???
				}
			}
			
		}
		
		this.lastConcentrations = this.currentConcentrations;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedSolution()
	 */
	@Override
	public double[][] getOptimizedSolution() {
		double[] solution = getSolution();
		this.optimizedSolution = new double[2][];
		
		if (solution == null) {
			logger.warning("Can't get optimized solution! Solution doesn't exist!");
		} else {
			// 1. Flux vector assignment
			int fluxPosition = getTargetVariablesLengths()[0] - 1;
			double[] optimizedFluxVector = new double[this.computedFluxVector.length];
			// solution[0] contains the optimized value for the flux vector
			for (int i = 0; i < this.computedFluxVector.length; i++) {
				optimizedFluxVector[i] = solution[fluxPosition] * this.computedFluxVector[i];
			}
			this.optimizedSolution[1] = optimizedFluxVector; // 2nd position: flux vector
			
			// 2. Concentration vector assignment
			int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
			double[] optimizedConcentrations = new double[getTargetVariablesLengths()[1]];
			for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
				optimizedConcentrations[i] = solution[i + concentrationPosition];
			}
			this.optimizedSolution[0] = optimizedConcentrations; // 1st position: concentrations
		}
		
		return this.optimizedSolution;
	}
	
}
