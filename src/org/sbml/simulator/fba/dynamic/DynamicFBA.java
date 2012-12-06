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

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.simulator.io.CSVDataImporter;
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
	 * The SBML document on which the dynamic FBA performs
	 */
	private SBMLDocument model;
	
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
	 * Saves each species (position 0), flux (position 1) and reaction
	 * (position 2) identifiers in a String array
	 * TODO how to get the identifiers?
	 */
	private String[][] identifierMatrix;
	
	
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
	
	/*
	 * Initialize the solution {@link MultiTable} for visualization.
	 * This {@link MultiTable} contains multiple {@link MultiTable.Block}s.
	 */
	private void initializeSolutionMultiTable() {
		this.solutionMultiTable = new MultiTable();
		
		//this.solutionMultiTable.setName(name);
		//this.solutionMultiTable.setTimeName(timeName);
		this.solutionMultiTable.setTimePoints(this.timePoints);
		
		// Add concentrations, fluxes and gibbs energies each in a new Block
		this.solutionMultiTable.addBlock(this.identifierMatrix[0]); // concentrations Block
		this.solutionMultiTable.addBlock(this.identifierMatrix[1]); // fluxes Block
		this.solutionMultiTable.addBlock(this.identifierMatrix[2]); // gibbs energies Block
	}
	
	/**
	 * CPLEX solves the flux minimization problem.
	 * @param cplex
	 * @param fluxMin
	 * @throws IloException
	 */
	public void minimizeFlux(IloCplex cplex, FluxMinimization fluxMin) throws IloException {
		/* These steps of CPLEX operations should find a solution to the flux
		 * minimization problem */
		fluxMin.prepareCplex(cplex);
		IloNumExpr targetFunction = fluxMin.createTargetFunction(cplex);
		fluxMin.optimizeTargetFunction(cplex, targetFunction);
		fluxMin.addConstraintsToTargetFunction(cplex);
		fluxMin.solveCplex(cplex);
	}
	
	/**
	 * A dynamic flux balance analysis for the flux minimization problem will be performed.
	 * @param fluxMin
	 * @throws IloException
	 */
	public void runDynamicFBA(FluxMinimization fluxMin) throws IloException {
		// Initialize a new CPLEX object
		IloCplex cplex = new IloCplex();
		
		// Initialize the solution MultiTable
		initializeSolutionMultiTable();
		
		for (int i=0; i<this.timePoints.length; i++) {
			/* TODO new FluxMinimization object with new 
			 * concentrations of the next point in time */
			fluxMin = new FluxMinimization();
			minimizeFlux(cplex, fluxMin);
			fluxMin.assignOptimizedSolution(this.model);
			
			double[] currentOptimizedConcentrations = fluxMin.getOptimizedConcentrations();
			double[] currentOptimizedFluxVector = fluxMin.getOptimizedFluxVector();
			double[] currentOptimizedGibbsEnergies = fluxMin.getOptimizedGibbsEnergies();

			this.solutionMultiTable.getBlock(0).setRowData(i, currentOptimizedConcentrations);
			this.solutionMultiTable.getBlock(1).setRowData(i, currentOptimizedFluxVector);
			this.solutionMultiTable.getBlock(2).setRowData(i, currentOptimizedGibbsEnergies);
		}

		// Stop the CPLEX stream
		cplex.end();
	}
	
	
	public static void main(String[] args) throws Exception {
		
		/* Read concentrations*/
		SBMLReader reader = new SBMLReader();
		Model testModel = reader.readSBML(args[0]).getModel();
		String concFile = args[1];
		CSVDataImporter importer = new CSVDataImporter();
		
		MultiTable concMT = importer.convert(testModel, concFile);
		double[][] completeConcentrations = new double[concMT.getTimePoints().length][];
		int speciesCount = testModel.getSpeciesCount();
		double[] currentConcentrations = new double[speciesCount];
		// The multitable does not contain all species
		ListOf<Species> listOfSpecies = testModel.getListOfSpecies();
		for (int i=0; i<concMT.getTimePoints().length; i++) {
			for (int j=0; j<speciesCount; j++) {
				String currentSpeciesId = listOfSpecies.get(j).getId();
				int columnIndexMT = concMT.getColumnIndex(currentSpeciesId);
				if (columnIndexMT == -1) {
					currentConcentrations[j] = Double.NaN;
					} else {
						currentConcentrations[j] = concMT.getValueAt(j, columnIndexMT);
						}
				}
			completeConcentrations[i] = currentConcentrations;
		}
		
		for (int i=0; i< completeConcentrations.length; i++) {
			System.out.println(Arrays.toString(completeConcentrations[i]));
		}
	}
	
}
