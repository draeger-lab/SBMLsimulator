/*
 * $Id:  DynamicFBA.java 13:11:23 faehnrich$
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

import java.util.Arrays;
import java.util.logging.Logger;

import org.simulator.math.odes.MultiTable;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

/**
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class DynamicFBA {
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(DynamicFBA.class.getName());
	
	/*
	 * A {@link MultiTable} with all solved values (flux, concentration, gibbs energy),
	 * optimized by CPLEX, for each point in time of the dynamic flux balance analysis
	 */
	private MultiTable solutionMultiTable;
	
	/*
	 * Saves all read point in times of the dynamic flux balance analysis
	 */
	private double[] timePoints;
	
	/*
	 * Saves each data values of the fluxes (position 0), concentrations
	 * (position 1) and gibbs energies (position 2) in a double matrix
	 */
	private double[][][] data = new double[3][][];
	
	/*
	 * Saves each flux (position 0), species (position 1) and reaction
	 * (position 2) identifiers in a String array
	 */
	private String[][] columnIdentifiers;
	
	/**
	 * 
	 */
	public DynamicFBA() {
		// TODO read concentration file to get point in times
		// TODO read files to get the column identifier...
	}
	
	
	/**
	 * @return The solution MultiTable for visualization
	 */
	public MultiTable getSolutionMultiTable() {
		return this.solutionMultiTable;
	}

	/**
	 * @return The point in times in which the dynamic FBA will be performed
	 */
	public double[] getTimePoints() {
		return this.timePoints;
	}
	
	/**
	 * Create the solution {@link MultiTable} for visualization.
	 * This {@link MultiTable} contains multiple {@link MultiTable.Block}s.
	 */
	public void createSolutionMultiTable() {
		/* Create MultiTable only for the fluxes that are 
		 * saved in the first place of each data structure */ 
		this.solutionMultiTable = new MultiTable(this.timePoints, data[0], columnIdentifiers[0]);
		
		// Add Block for each new column identifier
		for (int i=1; i<this.columnIdentifiers.length; i++) {
			this.solutionMultiTable.addBlock(this.columnIdentifiers[i]);
		}
		
		// Add data that is saved in the next matrix for each new Block
		for (int i=1; i<this.data.length; i++) {
			this.solutionMultiTable.getBlock(i).setData(this.data[i]);
		}
	}
	
	/**
	 * CPLEX solves the flux minimization problem.
	 * @param fluxMinimization
	 * @throws IloException
	 */
	public void minimizeFlux(FluxMinimization fluxMinimization) throws IloException {
		// Initialize a new CPLEX object
		IloCplex cplex = new IloCplex();
		
		/* These steps of CPLEX operations should find a solution to the flux
		 * minimization problem */
		fluxMinimization.prepareCplex(cplex);
		IloNumExpr targetFunction = fluxMinimization.createTargetFunction(cplex);
		fluxMinimization.optimizeTargetFunction(cplex, targetFunction);
		fluxMinimization.addConstraintsToTargetFunction(cplex);
		fluxMinimization.solveCplex(cplex);
		
		// Stop the CPLEX stream
		cplex.end();
	}
	
	// TODO move this method to another invoking class
	public void runDynamicFBA() throws IloException {
		// DynamicFBA dfba = new DynamicFBA(...);
		FluxMinimization fluxMin;
		
		double[][] fluxValues = new double[getTimePoints().length][];
		double[][] concValues = new double[getTimePoints().length][];
		double[][] gibbsValues = new double[getTimePoints().length][];
		
		for (int i=0; i<getTimePoints().length; i++) {
			/* TODO new FluxMinimization object with new 
			 * concentrations of the next point in time */
			fluxMin = new FluxMinimization();
			minimizeFlux(fluxMin);
			fluxMin.assignOptimizedSolution();
			
			double[] flux = fluxMin.getOptimizedFluxVector();
			double[] gibbs = fluxMin.getOptimizedGibbsEnergies();
			double[] conc = fluxMin.getOptimizedConcentrations();
			
			fluxValues[i] = flux;
			concValues[i] = conc;
			gibbsValues[i] = gibbs;
		}
		
		this.data[0] = fluxValues;
		this.data[1] = concValues;
		this.data[2] = gibbsValues;
		
		createSolutionMultiTable();
	}
	
}
