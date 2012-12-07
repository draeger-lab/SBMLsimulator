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

import java.util.logging.Logger;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.math.SplineCalculation;
import org.simulator.math.odes.MultiTable;

import ilog.concert.IloException;
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
	 * The SBML document on which the dynamic FBA performs
	 */
	private SBMLDocument document;
	
	/*
	 * A {@link MultiTable} with all linearly interpolated concentration values of
	 * the set dynamic FBA point in times
	 */
	private MultiTable dFBAconcentrations;
	
	/*
	 * A {@link MultiTable} with all solved values (flux, concentration, gibbs energy),
	 * optimized by CPLEX, for each point in time of the dynamic flux balance analysis
	 */
	private MultiTable solutionMultiTable;
	
	/*
	 * Saves all set point in times of the dynamic flux balance analysis
	 */
	private double[] dFBAtimePoints;
	
	
	/**
	 * 
	 * @param function
	 * @param table - The concentrations in a {@link MultiTable}
	 * @param document
	 */
	public DynamicFBA(SBMLDocument document, MultiTable table, int timePointCount) {
		// Fit SBML document
		this.document = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(document);
		
		// Interpolate linearly
		this.dFBAconcentrations = calculateLinearInterpolation(table, timePointCount);
		
		// Initialize the solution MultiTable
		initializeSolutionMultiTable(table);
		
		// ...
		
	}
	
	 /**
	  * 
	  * @param document
	  * @param table
	  */
	public DynamicFBA(SBMLDocument document, MultiTable table) {
		this(document, table, table.getTimePoints().length);
	}
	
	/**
	 * TODO consider hiding this constructor
	 */
	public DynamicFBA() {
		
	}
	
	
	/**
	 * @return The concentrations of each point in time in a {@link MultiTable}
	 */
	public MultiTable getDFBAconcentrations() {
		return this.dFBAconcentrations;
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
	public double[] getDFBATimePoints() {
		return this.dFBAtimePoints;
	}
	
	/*
	 * Initialize the solution {@link MultiTable} for visualization.
	 * This {@link MultiTable} contains multiple {@link MultiTable.Block}s.
	 */
	private void initializeSolutionMultiTable(MultiTable table) {
		this.solutionMultiTable = new MultiTable();
		
		this.solutionMultiTable.setTimeName(table.getTimeName());
		this.solutionMultiTable.setTimePoints(getDFBATimePoints());
		
		// Species Ids fors the concentrations block
		ListOf<Species> listOfSpecies = this.document.getModel().getListOfSpecies();
		int speciesCount = this.document.getModel().getSpeciesCount();
		String[] speciesIds = new String[speciesCount];
		for (int i = 0; i < speciesCount; i++) {
			speciesIds[i] = listOfSpecies.get(i).getId();
		}
		
		// Reaction Ids for the fluxes and gibbs energies block
		ListOf<Reaction> listOfReactions = this.document.getModel().getListOfReactions();
		int reactionCount = this.document.getModel().getReactionCount();
		String[] reactionIds = new String[reactionCount];
		for (int i = 0; i < reactionCount; i++) {
			reactionIds[i] = listOfReactions.get(i).getId();
		}
		
		// Add concentrations, fluxes and gibbs energies each in a new block
		this.solutionMultiTable.addBlock(speciesIds); // concentrations block
		this.solutionMultiTable.addBlock(reactionIds); // fluxes block
		this.solutionMultiTable.addBlock(reactionIds); // gibbs energies block
	}
	
	/**
	 * A dynamic flux balance analysis will be performed.
	 * @param fluxMin
	 * @throws IloException
	 */
	public void runDynamicFBA(TargetFunction function) throws IloException {
		// Initialize a new CPLEX object
		IloCplex cplex = new IloCplex();
		
		for (int i=0; i<this.dFBAtimePoints.length; i++) {
			int speciesCount = this.document.getModel().getSpeciesCount();
			double[] currentConcentrations = new double[speciesCount];
			for (int j = 0; j < speciesCount; j++) {
				currentConcentrations[j] = this.dFBAconcentrations.getValueAt(i, j);
			}
			
			function.setCurrentConcentrations(currentConcentrations);
			function.optimizeProblem(cplex);
			function.assignOptimizedSolution(this.document);
			
			double[] currentOptimizedConcentrations = function.getOptimizedConcentrations();
			double[] currentOptimizedFluxVector = function.getOptimizedFluxVector();
			double[] currentOptimizedGibbsEnergies = function.getOptimizedGibbsEnergies();

			this.solutionMultiTable.getBlock(0).setRowData(i, currentOptimizedConcentrations);
			this.solutionMultiTable.getBlock(1).setRowData(i, currentOptimizedFluxVector);
			this.solutionMultiTable.getBlock(2).setRowData(i, currentOptimizedGibbsEnergies);
		}

		// Stop the CPLEX stream
		cplex.end();
	}
	
	/**
	 * (Use it only if each point in time has the same distance from its neighboring point in time).
	 * @param table
	 * @param timePointCount
	 * @return
	 */
	public MultiTable calculateLinearInterpolation(MultiTable table, int timePointCount) {
		// Start initialize new MultiTable
		MultiTable fullSpeciesMultiTable = new MultiTable();
		int speciesCount = this.document.getModel().getSpeciesCount();
		fullSpeciesMultiTable.setTimeName(table.getTimeName()); //TODO time name gets lost! check class SplineCalculation
		fullSpeciesMultiTable.setTimePoints(table.getTimePoints());
		
		ListOf<Species> listOfSpecies = this.document.getModel().getListOfSpecies();
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
		if ((Integer.MIN_VALUE < timePointCount) && (timePointCount < givenTimePointLength)) {
			logger.warning("TimePointCount (" + timePointCount + ") has to be greater than the given timePoint length (" + givenTimePointLength + ")!");
			return fullSpeciesMultiTable;
		}
		
		// ... then interpolate linearly
		MultiTable fullTimePointMultiTable;
		if (((timePointCount - 1) % (givenTimePointLength - 1)) == 0) {
			int inBetweenTimePoints = (timePointCount - givenTimePointLength) / (givenTimePointLength - 1);
			fullTimePointMultiTable = SplineCalculation.calculateSplineValues(fullSpeciesMultiTable, inBetweenTimePoints);
		} else {
			int multiplyFactor = (int) ((timePointCount - 1) / (givenTimePointLength - 1));
			int inBetweenTimePoints = (((multiplyFactor * (givenTimePointLength - 1)) + 1) - givenTimePointLength) / (givenTimePointLength - 1);
			logger.warning("TimePointCount (" + timePointCount + ") for better calculating set to: " + ((inBetweenTimePoints * (givenTimePointLength - 1)) + givenTimePointLength));
			fullTimePointMultiTable = SplineCalculation.calculateSplineValues(fullSpeciesMultiTable, inBetweenTimePoints);
		}
		
		// Set the dynamic FBA point in times
		this.dFBAtimePoints = fullTimePointMultiTable.getTimePoints();
		
		return fullTimePointMultiTable;
	}
	
}
