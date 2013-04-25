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

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.Set;
import java.util.logging.Logger;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.math.SplineCalculation;
import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.MultiTable.Block;
import org.simulator.math.odes.MultiTable.Block.Column;

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
	
	/**
	 * The original SBML document
	 */
	protected static SBMLDocument originalDocument;
	
	/**
	 * The splitted SBML document on which the dynamic FBA will be performed
	 */
	protected static SBMLDocument splittedDocument;
	
	/**
	 * The starting {@link MultiTable} with all given and then interpolated concentration and flux values.
	 * Contains all columns according to the original {@link SBMLDocument}.
	 */
	private MultiTable dFBAStartingMultiTable;
	
	/**
	 * A {@link MultiTable} with all solved values (flux, concentration, etc),
	 * optimized by CPLEX, for each point in time of the dynamic flux balance analysis.
	 * Contains all columns according to the splitted {@link SBMLDocument}.
	 */
	private MultiTable workingSolutionMultiTable;

	/**
	 * A {@link MultiTable} with all solved values,
	 * optimized by CPLEX, for each point in time of the dynamic flux balance analysis.
	 * Contains all columns according to the original {@link SBMLDocument}.
	 */
	private MultiTable finalSolutionMultiTable = null;

	/**
	 * The target function
	 */
	private TargetFunction function;
	
	/**
	 * contains the default Time Name for the Multitable
	 */
	private static String defaultTimeName = "Time";
	
	/**
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

		// Interpolate concentrations and fluxes
		this.dFBAStartingMultiTable = calculateSplineInterpolation(table, timePointCount);		
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
	 * @return The non sparse {@link MultiTable} of each time point, 
	 * with all given concentration and flux information according to the originalDocument
	 */
	public MultiTable getDFBAStartingMultiTable() {
		return this.dFBAStartingMultiTable;
	}
	
	/**
	 * @return The temporarily solution {@link MultiTable} according to the splittedDocument
	 */
	public MultiTable getWorkingSolutionMultiTable() {
		return this.workingSolutionMultiTable;
	}
	
	/**
	 * @return The final solution {@link MultiTable} for visualization according to the originalDocument
	 */
	public MultiTable getFinalMultiTable() {
		if (this.finalSolutionMultiTable == null) {
			finalizeSolutionMultiTable();
		}
		return this.finalSolutionMultiTable;
	}
	
	/**
	 * Initialize the solution {@link MultiTable} for visualization. This
	 * {@link MultiTable} contains multiple {@link MultiTable.Block}s.
	 * 
	 * @param function
	 */
	public void initializeWorkingSolutionMultiTable(TargetFunction function) {
		this.workingSolutionMultiTable = new MultiTable();
		
		this.workingSolutionMultiTable.setTimeName(getDefaultTimeName());
		this.workingSolutionMultiTable.setTimePoints(dFBATimePoints);
		
		// Add specific blocks with each specific identifiers
		String[][] identifiers = function.getTargetVariablesIds();
		for (int idsNr = 0; idsNr < identifiers.length; idsNr++) {
			this.workingSolutionMultiTable.addBlock(identifiers[idsNr]);
		}
	}
	
	/**
	 * A dynamic flux balance analysis will be performed.
	 * 
	 * @param fluxMin
	 * @throws IloException
	 */
	public void runDynamicFBA(TargetFunction function) throws IloException {
		this.function = function;
		
		// Initialize a new CPLEX object
		IloCplex cplex = new IloCplex();
		// Initialize the CPLEX variables
		function.initCplexVariables(cplex);
		
		// Initialize the solution MultiTable
		initializeWorkingSolutionMultiTable(function);
		
		Model m = originalDocument.getModel();
		// Set the complete interpolated concentrations
		int speciesCount = m.getSpeciesCount();
		double[][] concentrations = new double[dFBATimePoints.length][speciesCount];
		Block conc = dFBAStartingMultiTable.getBlock(0);

		int reactionCount = originalDocument.getModel().getReactionCount();
		double[][] netFluxes = new double[dFBATimePoints.length][reactionCount];
		Block flux = dFBAStartingMultiTable.getBlock(1);
		
		
		for (int t = 0; t < dFBATimePoints.length; t++) {
			for (int i = 0; i < speciesCount; i++) {
				// Remember: first column of each MultiTable/block contains the timePoints -> i + 1
				concentrations[t][i] = conc.getValueAt(t, i + 1);
			}
			for (int j = 0; j < reactionCount; j++) {
				// Remember: first column of each MultiTable/block contains the timePoints -> j + 1
				netFluxes[t][j] = flux.getValueAt(t, j + 1);
			}
		}
		function.setInterpolatedConcentrations(concentrations);
		function.setInterpolatedFluxes(netFluxes);
		
		
		// Iterate over the complete points in time of the dynamic FBA
		for (int i = 0; i < dFBATimePoints.length; i++) {
			// Let CPLEX solve the optimization problem...
			function.setTimePointStep(i);
			function.optimizeProblem(cplex);
			// method must be called to transfer the solution from cplex into a double[][]
			function.getOptimizedSolution();
			// (Reset the CPLEX object! If not, a MultipleObjectiveException is waiting!)
			cplex.clearModel();
			// transfer the solution into the working MT
			function.saveValuesForCurrentTimePoint(this.workingSolutionMultiTable);
		}

		// Stop the CPLEX stream
		cplex.end();
		
		finalizeSolutionMultiTable();
		
		
	}
	
	/**
	 * Finalizes the Solution {@link MultiTable} e.g. eliminating temporarily backward reactions of reversible reaction
	 */
	private void finalizeSolutionMultiTable() {
		this.finalSolutionMultiTable = dFBAStartingMultiTable;
		
		Model m = originalDocument.getModel();

		int speciesCount = m.getSpeciesCount();
		for (int i = 0; i < speciesCount; i++) {
			String currentSpeciesId = m.getSpecies(i).getId();
			Column workingReactionCol = this.workingSolutionMultiTable.getColumn(currentSpeciesId);
			int specificColumn = this.finalSolutionMultiTable.findColumn(currentSpeciesId);
			for (int timePoint = 0; timePoint < this.workingSolutionMultiTable.getRowCount(); timePoint++) {
				double concentration = workingReactionCol.getValue(timePoint);
				// Overwrite the specific column value with the new net flux
				this.finalSolutionMultiTable.setValueAt(concentration, timePoint, specificColumn);
			}
		}
		
		int reactionCount = m.getReactionCount();
		for (int j = 0; j < reactionCount; j++) {
			String currentReactionId = m.getReaction(j).getId();
			Column workingReactionCol = this.workingSolutionMultiTable.getColumn(currentReactionId);
			int specificColumn = this.finalSolutionMultiTable.findColumn(currentReactionId);
			for (int timePoint = 0; timePoint < this.workingSolutionMultiTable.getRowCount(); timePoint++) {
				double netFlux = workingReactionCol.getValue(timePoint);
				// Overwrite the specific column value with the new net flux
				this.finalSolutionMultiTable.setValueAt(netFlux, timePoint, specificColumn);
			}
		}
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
		Model m = originalDocument.getModel();
		int speciesCount = m.getSpeciesCount();
		int reactionCount = m.getReactionCount();
		
		String[] speciesIds = new String[speciesCount];
		for (int i = 0; i < speciesCount; i++) {
			speciesIds[i] = m.getSpecies(i).getId();
		}
		String[] reactionIds = new String[reactionCount];
		for (int i = 0; i < reactionCount; i++) {
			reactionIds[i] = m.getReaction(i).getId();
		}
		MultiTable fullMT = new MultiTable();
		fullMT.setTimeName(table.getTimeName());
		fullMT.setTimePoints(table.getTimePoints());
		fullMT.addBlock(speciesIds);  // block 0
		fullMT.addBlock(reactionIds); // block 1
		
		for (int t = 0; t < table.getTimePoints().length; t++) {
			double[] currentConcentrations = new double[speciesCount];
			for (int i = 0; i < speciesCount; i++) {
				String currentSpeciesId = m.getSpecies(i).getId();
				int columnIndexMT = table.getColumnIndex(currentSpeciesId);
				if (columnIndexMT == -1) { // no entry in the original multi table
					currentConcentrations[i] = Double.NaN;
				} else {
					currentConcentrations[i] = table.getValueAt(t, columnIndexMT);
				}
			}
			fullMT.getBlock(0).setRowData(t, currentConcentrations);
			
			double[] currentFluxes = new double[reactionCount];
			for (int j = 0; j < reactionCount; j++) {
				String currentReactionId = m.getReaction(j).getId();
				int columnIndexMT = table.getColumnIndex(currentReactionId);
				if (columnIndexMT == -1) { // no entry in the original multi table
					currentFluxes[j] = Double.NaN;
				} else {
					currentFluxes[j] = table.getValueAt(t, columnIndexMT);
				}
			}
			fullMT.getBlock(1).setRowData(t, currentFluxes);
		}
		// Finish initialize new MultiTable

		
		// Set the dynamic FBA points in time
		dFBATimePoints = fullMT.getTimePoints();
		
		// First, check if timePointCount is a valid number...
		int givenTimePointLength = table.getTimePoints().length;
		if (timePointCount < givenTimePointLength) {
			logger.warning("TimePointCount (" + timePointCount + ") has to be greater than the given timePoint length (" + givenTimePointLength + ")!");
			return fullMT;
		}
		else if (timePointCount == givenTimePointLength) {
			return fullMT;
		}
		
		// ... then interpolate
		MultiTable fullTimePointMultiTable = null;
		int inBetweenTimePoints = 0;
		if (((timePointCount - 1) % (givenTimePointLength - 1)) == 0) {
			inBetweenTimePoints = (timePointCount - givenTimePointLength) / (givenTimePointLength - 1);
			
		} else {
			int multiplyFactor = (int) ((timePointCount - 1) / (givenTimePointLength - 1));
			inBetweenTimePoints = (((multiplyFactor * (givenTimePointLength - 1)) + 1) - givenTimePointLength) / (givenTimePointLength - 1);
			logger.info("TimePointCount (" + timePointCount + ") for better calculating set to: " + ((inBetweenTimePoints * (givenTimePointLength - 1)) + givenTimePointLength));
		}
		
		//Also interpolate the given fluxes
		for (int b = 0; b < fullMT.getBlockCount(); b++) {
			MultiTable mt = SplineCalculation.calculateSplineValues(fullMT, b, inBetweenTimePoints, false);
			if (fullTimePointMultiTable == null) {
				
				fullTimePointMultiTable = new MultiTable();
				fullTimePointMultiTable.setTimePoints(mt.getTimePoints());
			}
			fullTimePointMultiTable.addBlock(mt.getBlock(0).getIdentifiers());
			fullTimePointMultiTable.getBlock(b).setData(mt.getBlock(0).getData());
		}
		
		// Set the dynamic FBA points in time
		dFBATimePoints = fullTimePointMultiTable.getTimePoints();
		
		return fullTimePointMultiTable;
	}

	/**
	 * @return the defaultTimeName
	 */
	public static String getDefaultTimeName() {
		return defaultTimeName;
	}

	/**
	 * @param defaultTimeName the defaultTimeName to set
	 */
	public static void setDefaultTimeName(String defaultTimeName) {
		DynamicFBA.defaultTimeName = defaultTimeName;
	}
	
}
