/*
 * $Id:  DynamicFBA.java 13:11:23 faehnrich$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
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
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.math.SplineCalculation;
import org.simulator.math.odes.MultiTable;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 * 
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
	 * The SBML document on which the dynamic FBA performs
	 */
	protected static SBMLDocument originalDocument;
	
	/*
	 * A {@link MultiTable} with all interpolated concentration values of the
	 * set dynamic FBA points in time
	 */
	private MultiTable dFBAConcentrations;
	
	/*
	 * A {@link MultiTable} with all solved values (flux, concentration, gibbs energy),
	 * optimized by CPLEX, for each point in time of the dynamic flux balance analysis
	 */
	private MultiTable solutionMultiTable;
	
	/*
	 * Saves all set points in time of the dynamic flux balance analysis
	 */
	protected static double[] dFBATimePoints;
	
	
	/**
	 * 
	 * @param document - The SBML document on which the dynamic FBA performs
	 * @param table - The concentrations in a {@link MultiTable}
	 * @param timePointCount - The number of points in time at which the dynamic FBA performs
	 */
	public DynamicFBA(SBMLDocument document, MultiTable table, int timePointCount) {
		// Save original SBML document
		originalDocument = document;

		// Interpolate concentrations
		this.dFBAConcentrations = calculateSplineInterpolation(table, timePointCount);		
	}
	
	 /**
	  * 
	  * @param document - The SBML document on which the dynamic FBA performs
	  * @param table - The concentrations in a {@link MultiTable}
	  */
	public DynamicFBA(SBMLDocument document, MultiTable table) {
		this(document, table, table.getTimePoints().length);
	}
	
	/**
	 * @return The concentrations of each point in time in a {@link MultiTable}
	 */
	public MultiTable getDFBAConcentrations() {
		return this.dFBAConcentrations;
	}
	
	/**
	 * @return The solution {@link MultiTable} for visualization
	 */
	public MultiTable getSolutionMultiTable() {
		return this.solutionMultiTable;
	}
	
	/**
	 * Initialize the solution {@link MultiTable} for visualization. This
	 * {@link MultiTable} contains multiple {@link MultiTable.Block}s.
	 * 
	 * @param function
	 */
	public void initializeSolutionMultiTable(TargetFunction function) {
		this.solutionMultiTable = new MultiTable();
		
		this.solutionMultiTable.setTimeName(this.dFBAConcentrations.getTimeName());
		this.solutionMultiTable.setTimePoints(dFBATimePoints);
		
		// Add specific blocks with each specific identifiers
		String[][] identifiers = function.getTargetVariablesIds();
		for (int idsNr = 0; idsNr < identifiers.length; idsNr++) {
			this.solutionMultiTable.addBlock(identifiers[idsNr]);
		}
	}
	
	/**
	 * A dynamic flux balance analysis will be performed.
	 * 
	 * @param fluxMin
	 * @throws IloException
	 */
	public void runDynamicFBA(TargetFunction function) throws IloException {
		// Initialize a new CPLEX object
		IloCplex cplex = new IloCplex();
		// Initialize the CPLEX variables
		function.initCplexVariables(cplex);
		
		// Initialize the solution MultiTable
		initializeSolutionMultiTable(function);
		
		for (int i = 0; i < dFBATimePoints.length; i++) {
			// Get current concentrations of the current point in time
			int speciesCount = originalDocument.getModel().getSpeciesCount();
			double[] currentConcentrations = new double[speciesCount];
			for (int j = 0; j < speciesCount; j++) {
				currentConcentrations[j] = this.dFBAConcentrations.getValueAt(i, j + 1);
			}
			
			// Let CPLEX solve the optimization problem...
			function.setCurrentConcentrations(currentConcentrations);
			function.optimizeProblem(cplex);
			double[][] optimizedSolution = function.getOptimizedSolution();
			// (Reset the CPLEX object! If not, a MultipleObjectiveException is waiting!)
			cplex.clearModel();
			
			// ... and fill the solution MultiTable
			for (int block = 0; block < this.solutionMultiTable.getBlockCount(); block++) {
				double[] currentSpecificSolution = optimizedSolution[block];
				this.solutionMultiTable.getBlock(block).setRowData(i, currentSpecificSolution);
			}
		}

		// Stop the CPLEX stream
		cplex.end();
	}
	
	/**
	 * (Use it only if each point in time has the same distance from its neighboring point in time).
	 * 
	 * @param table
	 * @param timePointCount
	 * @return The {@link MultiTable} with all interpolated values of the concentrations
	 */
	public MultiTable calculateSplineInterpolation(MultiTable table, int timePointCount) {
		// Start initialize new MultiTable
		MultiTable fullSpeciesMultiTable = new MultiTable();
		int speciesCount = originalDocument.getModel().getSpeciesCount();
		fullSpeciesMultiTable.setTimeName(table.getTimeName());
		fullSpeciesMultiTable.setTimePoints(table.getTimePoints());
		
		ListOf<Species> listOfSpecies = originalDocument.getModel().getListOfSpecies();
		String[] speciesIds = new String[speciesCount];
		for (int i = 0; i < speciesCount; i++) {
			speciesIds[i] = listOfSpecies.get(i).getId();
		}
		fullSpeciesMultiTable.addBlock(speciesIds);
		
		for (int i = 0; i < table.getTimePoints().length; i++) {
			double[] currentConcentrations = new double[speciesCount];
			for (int j = 0; j < speciesCount; j++) {
				String currentSpeciesId = listOfSpecies.get(j).getId();
				int columnIndexMT = table.getColumnIndex(currentSpeciesId);
				if (columnIndexMT == -1) {
					currentConcentrations[j] = Double.NaN;
				} else {
					currentConcentrations[j] = table.getValueAt(i, columnIndexMT);
				}
			}
			fullSpeciesMultiTable.getBlock(0).setRowData(i, currentConcentrations);
		}
		// Finish initialize new MultiTable
		
		// First, check if timePointCount is a valid number...
		int givenTimePointLength = table.getTimePoints().length;
		if (timePointCount < givenTimePointLength) {
			logger.warning("TimePointCount (" + timePointCount + ") has to be greater than the given timePoint length (" + givenTimePointLength + ")!");
			return fullSpeciesMultiTable;
		}
		
		// ... then interpolate
		MultiTable fullTimePointMultiTable;
		if (((timePointCount - 1) % (givenTimePointLength - 1)) == 0) {
			int inBetweenTimePoints = (timePointCount - givenTimePointLength) / (givenTimePointLength - 1);
			fullTimePointMultiTable = SplineCalculation.calculateSplineValues(fullSpeciesMultiTable, inBetweenTimePoints, false);
		} else {
			int multiplyFactor = (int) ((timePointCount - 1) / (givenTimePointLength - 1));
			int inBetweenTimePoints = (((multiplyFactor * (givenTimePointLength - 1)) + 1) - givenTimePointLength) / (givenTimePointLength - 1);
			logger.warning("TimePointCount (" + timePointCount + ") for better calculating set to: " + ((inBetweenTimePoints * (givenTimePointLength - 1)) + givenTimePointLength));
			fullTimePointMultiTable = SplineCalculation.calculateSplineValues(fullSpeciesMultiTable, inBetweenTimePoints, false);
		}
		
		// Set the dynamic FBA points in time
		dFBATimePoints = fullTimePointMultiTable.getTimePoints();
		
		return fullTimePointMultiTable;
	}
	
}
