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
	 * A {@link MultiTable} with all solved values (flux, concentration, gibbs energy),
	 * optimized by CPLEX, for each point in time of the dynamic flux balance analysis.
	 */
	public MultiTable solutionMultiTable;
	
	/*
	 * Point in time when the dynamic flux balance analysis is started.
	 */
	private double startTimePoint;
	
	/*
	 * Point in time when the dynamic flux balance analysis is finished.
	 */
	private double endTimePoint;
	
	
	/**
	 * 
	 * @param fluxMinimization
	 * @throws IloException
	 */
	public void minimizeFlux(FluxMinimization fluxMinimization) throws IloException {
		// Initialize a new CPLEX object
		IloCplex cplex = new IloCplex();
		
		/*
		 * These steps of CPLEX operations should find a solution to the flux
		 * minimization problem
		 */
		fluxMinimization.prepareCplex(cplex);
		IloNumExpr targetFunction = fluxMinimization.createTargetFunction(cplex);
		fluxMinimization.optimizeTargetFunction(cplex, targetFunction);
		fluxMinimization.addConstraintsToTargetFunction(cplex);
		fluxMinimization.solveCplex(cplex);
		
		// Stop CPLEX stream
		cplex.end();
	}
	
}
