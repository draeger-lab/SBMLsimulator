/*
 * $Id:  FluxBalanceAnalysis.java 16:07:08 Meike Aichele$
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
package org.sbml.simulator.fba.controller;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.simulator.math.odes.MultiTable;

/**
 * Contains all components for flux balance analysis and solves the optimization problem
 * for the incoming target function and constraints with quadratic programming.
 * It also contains a MultiTable for the visualization of the computed fluxes.
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxBalanceAnalysis {

	/**
	 * Can be a {@link FluxMinimization}-object 
	 * or an other function for which the network has to be optimized
	 */
	public TargetFunction targetFunction;

	/**
	 * Contains e.g. Gibbs-energies
	 */
	public Constraints constraints;

	/**
	 * lower bound for cplex variables
	 */
	public double[] lb;

	/**
	 * upper bounds for cplex variables
	 */
	public double[] ub;

	/**
	 * Contains the target array for cplex.
	 */
	private double[] target;

	/**
	 * Contains the concentrations computed in the targetFunc.
	 */
	private double[] concentrations;

	/**
	 * Contains the solutions of cplex for the concentrations.
	 */
	public double[] solutionConcentrations;

	/**
	 * Contains the solutions of cplex for the fluxes.
	 */
	public double[] solutionFluxVector;

	/**
	 * Contains the {@link# solution_fluxVector} in a {@link MultiTable} for visualization.
	 */
	public MultiTable fluxesForVisualization;

	private static final transient Logger logger = Logger.getLogger(FluxBalanceAnalysis.class);

	private boolean constraintJG = true;

	private boolean constraintJ_rmaxG = true;

	private boolean constraintJ0 = true;

	private int cplexIterations = 600;

	// CONSTRUCTORS:	


	/**
	 * Constructor that gets a {@link Constraints}-Object and a {@link SBMLDocument} 
	 * and that sets the {@link# linearProgramming} true.
	 * @param originalDocument
	 * @param constraints
	 * 
	 * @throws Exception 
	 */
	public FluxBalanceAnalysis(SBMLDocument originalDocument, Constraints constraints, String[] targetFluxes) throws Exception {
		this(new FluxMinimization(originalDocument, constraints, targetFluxes), constraints);
	}

	/**
	 * 
	 * @param originalDocument
	 * @throws Exception
	 */
	public FluxBalanceAnalysis(SBMLDocument originalDocument) throws Exception {
		this(new FluxMinimization(originalDocument, new Constraints(originalDocument), FluxMinimizationUtils.SBMLDocToStoichMatrix(originalDocument), null), new Constraints(originalDocument));
	}


	/**
	 * Constructor that get's {@link TargetFunction} and  {@link Constraints} and initializes the arrays for 
	 * solution of FBA.
	 * 
	 * @param targetfunc
	 * @param constraints
	 */
	public FluxBalanceAnalysis(TargetFunction targetfunc, Constraints constraints) {
		super();
		this.targetFunction = targetfunc;
		this.constraints = constraints;
		SBMLDocument modifiedDocument = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(constraints.originalDocument);
		int target_array_length = modifiedDocument.getModel().getReactionCount()*4;
		this.target = new double[target_array_length];
		
		lb = new double[target_array_length + targetFunction.getConcentrations().length];
		ub = new double[target_array_length + targetFunction.getConcentrations().length];
		// TODO sysout entfernen
		System.out.println("lb: " + lb.length + " ub: " + ub.length);
		
		// initialize the concentrations
		concentrations = targetFunction.getConcentrations();
		
		// initialize default upper bounds (ub) and lower bounds (lb) for the variables in cplex-call
		int[] counter = targetFunction.getCounterArray();
		// counter[1] contains the length of flux vector
		for (int i = 0; i< counter[1]; i++) {
			lb[i]= -100000; 
			ub[i] = 100000;
		}
		for (int g = counter[1]; g < target_array_length; g++) {
			lb[g] = -100000;
			ub[g] = 100000;
		}
		// bounds for concentrations
		for(int j = target_array_length; j < (concentrations.length + target_array_length); j++) {
			lb[j] = Math.pow(10, -10);
			ub[j] = Math.pow(10, -1);
		}
		
		
		// init solution arrays
		this.solutionFluxVector = new double[targetfunc.getFluxVector().length];
		this.solutionConcentrations = new double[targetFunction.getConcentrations().length];
	}


	// METHODS FOR SOLVING FBA:


	/**
	 * Calls a method to solve the problem with linear programming if the boolean
	 * {@link# linearProgramming} is set and true, else it calls a method for quadratic programming.
	 * @return double[]
	 * @throws IloException 
	 */
	public double[] solve() throws IloException {
		target = targetFunction.computeTargetFunctionForQuadraticProgramming();
		return solveWithQuadraticProgramming();
	}


	/**
	 * Calls CPLEX to solve the problem with quadratic programming and sets in the end
	 * the computed solutions for concentrations and fluxes.
	 * @throws IloException 
	 */
	private double[] solveWithQuadraticProgramming() throws IloException {
		// create the cplex solver
		IloCplex cplex = new IloCplex();

		// TARGET
		// create upper bounds (ub) and lower bounds (lb) for the variables x
		int[] counter = targetFunction.getCounterArray();
		
		// create variables with upper bounds and lower bounds
		IloNumVar[] x = cplex.numVarArray((target.length + concentrations.length), lb, ub);

		//compute the target function for cplex with the scalar product (lin)
		IloNumExpr lin = cplex.prod(target[0], x[0]);
		for (int i = 1; i < target.length; i++) {
			IloNumExpr temp = lin;
			lin = cplex.sum(temp, cplex.prod(target[i], x[i]));
		}


		//lambda1*sum((c_i - c_eq)^2)
		double[] c_eq = constraints.getEquilibriumConcentrations();
		IloNumExpr sum = cplex.numExpr();
		for (int i = 0; i< concentrations.length; i++) {
			IloNumExpr temp = sum;
			IloNumExpr c_i = cplex.prod(x[i + target.length], concentrations[i]);
			if(!Double.isNaN(concentrations[i]) && !Double.isNaN(c_eq[i])) {
				// here the sum (c_i - c_eq)^2 is performed by cplex. To make it simpler for cplex, the binomial formula is used
				// where cplex.square(x) = x^2
				sum = cplex.sum(temp, cplex.square(c_i), cplex.sum(cplex.prod(c_i, ((-2) * c_eq[i])), cplex.square(cplex.constant(c_eq[i]))));
			} else if(!Double.isNaN(c_eq[i])){
				sum = cplex.sum(temp, cplex.square(x[i + target.length]),cplex.sum(cplex.prod(x[i + target.length], ((-2) * c_eq[i])), cplex.square(cplex.constant(c_eq[i]))));
			} else {
				sum = cplex.sum(temp ,cplex.square(x[i + target.length]));
			}
		}
		// now put the variables together  
		IloNumExpr cplex_target = cplex.sum(cplex.prod(TargetFunction.lambda1, sum),lin);

		// only for FluxMinimization the target function has to be minimized
		if (targetFunction instanceof FluxMinimization) {
			cplex.addMinimize(cplex_target);
		} else {
			cplex.addMaximize(cplex_target);
		}

		//		
		//		for (int i = 0; i < x.length; i++) {
		//			if (i < counter[1]) {
		//				System.out.print(FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(constraints.document).getModel().getReaction(i).getId() + ": " + target[i] + "    x: "+ x[i]);
		//				System.out.println("  flux");
		//			} else if (i < counter[2]) {
		//				System.out.print(i + ": " + target[i] + "    x: "+ x[i]);
		//				System.out.println("  E");
		//			} else if (i < counter[3]) {
		//				System.out.print(i + ": " + target[i] + "    x: "+ x[i]);
		//				System.out.println("  L");
		//			} else if (i < target.length) {
		//				System.out.print(i + ": " + target[i] + "    x: "+ x[i]);
		//				System.out.println("  gibbs");
		//			} else {
		//				System.out.print(i + ": " + concentrations[i-target.length] + "    x: "+ x[i]);
		//				System.out.println("  conc");
		//			}
		//		}

		//CONSTRAINTS

		SBMLDocument modifiedDocument = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(constraints.originalDocument);
		double[] steadyStateFluxes = targetFunction.getFluxVector();
		double[] compGibbs = constraints.getGibbsEnergies();
		double[] r_max = constraints.computeR_max(steadyStateFluxes);
		
		for (int j = 0; j< counter[1]; j++) {
			//jg is the expression for J_j * G_j
			//and j_rmaxG the expression for |J_j| - r_max * |G_j|
			int k = j + counter[3];
			
			IloNumExpr j_j = cplex.prod(cplex.abs(cplex.constant(steadyStateFluxes[j])), x[j]);
			
			// constraint |J_j| - r_max * |G_j| < 0
			if (isConstraintJ_rmaxG()) {
				IloNumExpr rmaxG = cplex.numExpr();
				if (!Double.isNaN(compGibbs[j]) && !Double.isInfinite(compGibbs[j])) {
					rmaxG = cplex.prod(r_max[j], cplex.abs(cplex.constant(compGibbs[j])));
				} else {
					rmaxG = cplex.prod(r_max[j], x[k]);
				}
				
				cplex.addLe(cplex.diff(j_j, rmaxG), -Double.MIN_VALUE);
				logger.log(Level.DEBUG, String.format("constraint |J_j| - r_max * |G_j| < 0:  "+ cplex.diff(j_j, rmaxG)+ " < " + 0 ));
			}
			
			// constraint J_j >= 0
			if (isConstraintJ0()) {
				cplex.addGe(cplex.prod(cplex.constant(steadyStateFluxes[j]), x[j]), 0);
			}
			
			// constraint J_j * G_j < 0
			if (isConstraintJG()) {
				IloNumExpr jg = cplex.numExpr();
				if (!Double.isNaN(compGibbs[j]) && !Double.isInfinite(compGibbs[j])) {
					jg = cplex.prod(cplex.prod(steadyStateFluxes[j], x[j]),compGibbs[j]);
				} else {
					jg = cplex.prod(cplex.prod(steadyStateFluxes[j], x[j]),x[k]);
				}
				
				cplex.addLe(jg, -Double.MIN_VALUE);
				// TODO sysout
				System.out.println(modifiedDocument.getModel().getReaction(j) + ": " + jg + " < " + 0);
				logger.log(Level.DEBUG, String.format("constraint J_j * G_j: " + jg + " < " + 0));
			}
		}

		// now solve the problem and get the solution array for the variables x,
		// cplex.setOut(null) stops the logger massages of cplex
		double[] solution = null;
		//		cplex.setOut(null);

		//set the iteration limit: without doing this, cplex iterates 2100000000 times...
		cplex.setParam(IloCplex.IntParam.BarItLim, cplexIterations );

		cplex.solve();
		// get the from cplex computed values for the variables x
		solution = cplex.getValues(x);
		for (int i = 0; i < counter[1]; i++) {
			// the first counter[1]-values are corresponding to the fluxes
			solutionFluxVector[i] = steadyStateFluxes[i] * solution[i];
		}
		for (int j = 0; j < concentrations.length; j++) {
			// the last values in x are corresponding to the concentrations
			if (!Double.isNaN(concentrations[j])) {
				//correct the computed solutions by multiply them with the computed variable-value
				solutionConcentrations[j] = concentrations[j] * solution[j + target.length];
			} else {
				solutionConcentrations[j] = solution[j + target.length];
			}
		}
		cplex.end();

		// create the MultiTable for visualization
		//fluxesForVisualization = createMultiTableForVisualizing();
		return solution;

	}

	/**
	 * Puts for {@link Reaction} j the upper bound on the given ubValue
	 * @param ubValue
	 * @param j (index of reaction)
	 * @return true if ubValue was set successfully
	 */
	public boolean setUbOfReactionJ(double ubValue, int j) {
		if (j < targetFunction.getFluxVector().length) {
			ub[j] = ubValue;
			return true;
		} else {
			return false;
		}
	}


	/**
	 * Puts for {@link Reaction} j the lower bound on the given lbValue.
	 * @param lbValue
	 * @param j (index of reaction)
	 * @return true if lbValue was set successfully
	 */
	public boolean setLbOfReactionJ(double lbValue, int j) {
		if (j < targetFunction.getFluxVector().length) {
			lb[j] = lbValue;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Puts for {@link Species} i the lower bound on the given lbValue.
	 * @param lbValue
	 * @param i (index of species)
	 * @return true if lbValue was set successfully
	 */
	public boolean setLbOfConcentrationI(double lbValue, int i) {
		if (i < concentrations.length) {
			lb[i + target.length] = lbValue;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Puts for {@link Species} i the upper bound on the given ubValue.
	 * @param ubValue
	 * @param i (index of species)
	 * @return true if ubValue was set successfully
	 */
	public boolean setUbOfConcentrationI(double ubValue, int i) {
		if (i < concentrations.length) {
			ub[i + target.length] = ubValue;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a {@link MultiTable} that contains the computed fluxes for visualization.
	 * @return MultiTable
	 */
	private MultiTable createMultiTableForVisualizing(){
		double[] time = {0};
		String[] idsOfReactions = new String[constraints.originalDocument.getModel().getReactionCount()];
		for (int i = 0; i < constraints.originalDocument.getModel().getReactionCount(); i++){
			idsOfReactions[i] = constraints.originalDocument.getModel().getReaction(i).getId();
		}
		double[][] fluxes = new double[solutionFluxVector.length][1];
		for (int j = 0; j < solutionFluxVector.length; j++) {
			fluxes[j][0] = solutionFluxVector[j];
		}
		MultiTable mt = new MultiTable(time, fluxes, idsOfReactions);

		return mt;
	}


	public void showIfTheFluxesAddToNull(SBMLDocument doc){
		for (int s = 0; s < doc.getModel().getSpeciesCount(); s++) {
			double sum = 0;
			for(int i = 0; i < solutionFluxVector.length; i++) {
				if(doc.getModel().getReaction(i).hasProduct(doc.getModel().getSpecies(s)) || doc.getModel().getReaction(i).hasReactant(doc.getModel().getSpecies(s))) {
					sum += solutionFluxVector[i];
				}
			}
			System.out.print(sum + "  ");
		}
	}

	/**
	 * Sets {@link TargetFunction#lambda1}
	 * @param lambda1
	 */
	public void setLambda1(double lambda1) {
		TargetFunction.lambda1 = lambda1;
	}

	/**
	 * Sets {@link TargetFunction#lambda2}
	 * @param lambda2
	 */
	public void setLambda2(double lambda2) {
		TargetFunction.lambda2 = lambda2;
	}

	/**
	 * Sets {@link TargetFunction#lambda3}
	 * @param lambda3
	 */
	public void setLambda3(double lambda3) {
		TargetFunction.lambda3 = lambda3;
	}

	/**
	 * Sets {@link TargetFunction#lambda4} 
	 * @param lambda4
	 */
	public void setLambda4(double lambda4) {
		TargetFunction.lambda4 = lambda4;
	}

	/**
	 * constraint J_j * G_j < 0
	 * @param constraintJG
	 */
	public void setConstraintJG(boolean constraintJG) {
		this.constraintJG = constraintJG;
	}

	/**
	 * constraint J_j * G_j < 0
	 * @return the constraintJG
	 */
	public boolean isConstraintJG() {
		return constraintJG;
	}

	/**
	 * constraint |J_j| - r_max * |G_j| < 0
	 * @param constraintJ_rmaxG
	 */
	public void setConstraintJ_rmaxG(boolean constraintJ_rmaxG) {
		this.constraintJ_rmaxG = constraintJ_rmaxG;
	}

	/**
	 * constraint |J_j| - r_max * |G_j| < 0
	 * @return the constraintJr_maxG
	 */
	public boolean isConstraintJ_rmaxG() {
		return constraintJ_rmaxG;
	}
	
	/**
	 * constraint J_j >= 0
	 * @param constraintJ0
	 */
	public void setConstraintJ0(boolean constraintJ0) {
		this.constraintJ0 = constraintJ0;
	}

	/**
	 * constraint J_j >= 0
	 * @return the constraintJ0
	 */
	public boolean isConstraintJ0() {
		return constraintJ0;
	}

	/**
	 * @return the cplexIterations
	 */
	public int getCplexIterations() {
		return cplexIterations;
	}

	/**
	 * @param cplexIterations the cplexIterations to set
	 */
	public void setCplexIterations(int cplexIterations) {
		this.cplexIterations = cplexIterations;
	}

	/**
	 * Method to set the whole lower bounds for concentrations.
	 * @param concLowerBound
	 */
	public void setLbOfConcentrations(double[] concLowerBound) {
		for (int j = 0; j < concLowerBound.length; j++) {
			lb[j + target.length] = concLowerBound[j];
		}
	}

	/**
	 * Method to set the whole upper bounds of concentrations.
	 * @param concUpperBound
	 */
	public void setUbOfConcentrations(double[] concUpperBound) {
		for (int j = 0; j < concUpperBound.length; j++) {
			ub[j + target.length] = concUpperBound[j];
		}
	}

	/**
	 * Method to set the whole lower bounds of fluxes.
	 * @param fluxLowerBound
	 */
	public void setLbOfReactions(double[] fluxLowerBound) {
		for (int j = 0; j < fluxLowerBound.length; j++) {
			lb[j] = fluxLowerBound[j];
		}
	}

	/**
	 * Method to set the whole upper bounds of fluxes.
	 * @param fluxUpperBound
	 */
	public void setUbOfReactions(double[] fluxUpperBound) {
		for (int j = 0; j < fluxUpperBound.length; j++) {
			ub[j] = fluxUpperBound[j];
		}
	}
}
