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

	private static final transient Logger logger = Logger.getLogger(FluxBalanceAnalysis.class);

	/**
	 * Contains the concentrations computed in the targetFunc.
	 */
	private double[] concentrations;

	/**
	 * Contains if the constraint to compute the error is on.
	 */
	private boolean constraintError = true;

	/**
	 * Contains if the constraint to compute |J| - rmax*|G| <= 0 is on.
	 */
	private boolean constraintJ_rmaxG = true;

	/**
	 * Contains if the constraint to compute J >= 0 is on.
	 */
	private boolean constraintJ0 = true;

	/**
	 * Contains if the constraint to compute J * G <= 0 is on.
	 */
	private boolean constraintJG = true;

	/**
	 * Contains e.g. Gibbs-energies
	 */
	public Constraints constraints;

	/**
	 * Contains the number of iterations for the cplex call.
	 */
	private int cplexIterations = 600;

	/**
	 * Contains the {@link# solution_fluxVector} in a {@link MultiTable} for visualization.
	 */
	public MultiTable fluxesForVisualization;

	/**
	 * lower bound for cplex variables
	 */
	private double[] lb;

	/**
	 * Contains the solutions of cplex for the concentrations.
	 */
	public double[] solutionConcentrations;

	/**
	 * Contains the solutions of cplex for the errors.
	 */
	public double[] solutionErrors;

	/**
	 * Contains the solutions of cplex for the fluxes.
	 */
	public double[] solutionFluxVector;

	/**
	 * Contains the target array for cplex.
	 */
	private double[] target;

	/**
	 * contains the length of the variable array, where there is only one variable for all fluxes.
	 */
	private int target_length_without_all_flux_values;

	/**
	 * Can be a {@link FluxMinimization}-object 
	 * or an other function for which the network has to be optimized
	 */
	public TargetFunction targetFunction;

	/**
	 * upper bounds for cplex variables
	 */
	private double[] ub;


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
		this(new FluxMinimization(originalDocument, new Constraints(originalDocument), FluxMinimizationUtils.getExpandedStoichiometricMatrix(originalDocument), null), new Constraints(originalDocument));
	}


	/**
	 * Constructor that get's {@link TargetFunction} and  {@link Constraints} and initializes the arrays for 
	 * solution of FBA.
	 * 
	 * @param targetfunc
	 * @param constraints
	 * @throws Exception 
	 */
	public FluxBalanceAnalysis(TargetFunction targetfunc, Constraints constraints) throws Exception {
		super();
		this.targetFunction = targetfunc;
		this.constraints = constraints;
		SBMLDocument modDoc = FluxMinimizationUtils.getExpandedDocument(constraints.originalDocument);
		target_length_without_all_flux_values = modDoc.getModel().getReactionCount()*4 + 1 - targetfunc.getFluxVector().length;
		this.target = new double[modDoc.getModel().getReactionCount()*4];

		lb = new double[target_length_without_all_flux_values + targetFunction.getConcentrations().length];
		ub = new double[target_length_without_all_flux_values + targetFunction.getConcentrations().length];

		// initialize the concentrations
		concentrations = targetFunction.getConcentrations();

		// initialize default upper bounds (ub) and lower bounds (lb) for the variables in cplex-call
		// counter[1] contains the length of flux vector

		// lb and up for the variable of the fluxes
		ub[0] = 10;
		lb[0] = 1.0;

		// everything between counter[1] and the length of the target-array is: L-vector, Errorarray and computedGibbsArray
		for (int lAndE = 1; lAndE < target_length_without_all_flux_values -	targetfunc.getGibbs().length; lAndE++) {
			lb[lAndE] = -100000;
			ub[lAndE] = 100000;
		}

		// counter[3] is the index of the gibbs values
		for (int gibbsBounds = target_length_without_all_flux_values -	targetfunc.getGibbs().length; gibbsBounds < target_length_without_all_flux_values; gibbsBounds++) {
			lb[gibbsBounds] = -1000;
			ub[gibbsBounds] = -Double.MIN_VALUE;
		}

		// bounds for concentrations
		for(int j = target_length_without_all_flux_values; j < (concentrations.length + target_length_without_all_flux_values); j++) {
			lb[j] = Math.pow(10, -10);
			ub[j] = Math.pow(10, -1);
		}

		// init solution arrays
		this.solutionFluxVector = new double[targetfunc.getFluxVector().length];
		this.solutionConcentrations = new double[concentrations.length];
		this.solutionErrors = new double[constraints.getComputedGibbsEnergies().length];
	}


	// METHODS FOR SOLVING FBA:


	/**
	 * Creates a {@link MultiTable} that contains the computed fluxes for visualization.
	 * @return MultiTable
	 * @throws Exception 
	 */
	private MultiTable createMultiTableForVisualizing() throws Exception{
		double[] time = {0};
		String[] idsOfReactions = new String[FluxMinimizationUtils.getExpandedDocument(constraints.originalDocument).getModel().getReactionCount()+1];
		idsOfReactions[0] = "time";
		for (int i = 0; i < FluxMinimizationUtils.getExpandedDocument(constraints.originalDocument).getModel().getReactionCount(); i++){
			idsOfReactions[i+1] = FluxMinimizationUtils.getExpandedDocument(constraints.originalDocument).getModel().getReaction(i).getId();
		}
		double[][] fluxes = new double[1][solutionFluxVector.length];
		for (int j = 0; j < solutionFluxVector.length; j++) {
			fluxes[0][j] = solutionFluxVector[j];
		}
		MultiTable mt = new MultiTable(time, fluxes, idsOfReactions);
	
		return mt;
	}

	/**
	 * @return the cplexIterations
	 */
	public int getCplexIterations() {
		return cplexIterations;
	}


	/**
	 * @param i index of the {@link Species}
	 * @return the lower bound of the corresponding variable to this species
	 */
	public Object getLbOfConcentrationI(int i) {
		return lb[i + target.length];
	}

	/**
	 * @param i index of the {@link Reaction}
	 * @return the lower bound of the corresponding variable to this reaction
	 */
	public Object getLbOfReactionJ(int i) {
		return lb[i];
	}

	/**
	 * @param i index of the {@link Species}
	 * @return the upper bound of the corresponding variable to this species
	 */
	public Object getUbOfConcentrationI(int i) {
		return ub[i + target.length];
	}

	/**
	 * @param i index of the {@link Reaction}
	 * @return the upper bound of the corresponding variable to this reaction
	 */
	public Object getUbOfReactionJ(int i) {
		return ub[i];
	}

	/**
	 * constraint Delta_G = Delta_G - Error
	 * @return the constraintError
	 */
	public boolean isConctraintError() {
		return constraintError;
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
	 * @return the constraintJ0
	 */
	public boolean isConstraintJ0() {
		return constraintJ0;
	}

	/**
	 * constraint J_j * G_j < 0
	 * @return the constraintJG
	 */
	public boolean isConstraintJG() {
		return constraintJG;
	}

	/**
	 * constraint Delta_G = Delta_G - Error
	 * @param constraintError
	 */
	public void setConctraintError(boolean constraintError) {
		this.constraintError = constraintError;
	}

	/**
	 * constraint |J_j| - r_max * |G_j| < 0
	 * @param constraintJ_rmaxG
	 */
	public void setConstraintJ_rmaxG(boolean constraintJ_rmaxG) {
		this.constraintJ_rmaxG = constraintJ_rmaxG;
	}

	/**
	 * constraint J_j >= 0
	 * @param constraintJ0
	 */
	public void setConstraintJ0(boolean constraintJ0) {
		this.constraintJ0 = constraintJ0;
	}

	/**
	 * constraint J_j * G_j < 0
	 * @param constraintJG
	 */
	public void setConstraintJG(boolean constraintJG) {
		this.constraintJG = constraintJG;
	}

	/**
	 * @param cplexIterations the cplexIterations to set
	 */
	public void setCplexIterations(int cplexIterations) {
		this.cplexIterations = cplexIterations;
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
	 * Method to set the whole lower bounds for concentrations.
	 * @param concLowerBound
	 */
	public void setLbOfConcentrations(double[] concLowerBound) {
		for (int j = 0; j < concLowerBound.length; j++) {
			lb[j + target.length] = concLowerBound[j];
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
	 * Method to set the whole lower bounds of fluxes.
	 * @param fluxLowerBound
	 */
	public void setLbOfReactions(double[] fluxLowerBound) {
		for (int j = 0; j < fluxLowerBound.length; j++) {
			lb[j] = fluxLowerBound[j];
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
	 * Method to set the whole upper bounds of concentrations.
	 * @param concUpperBound
	 */
	public void setUbOfConcentrations(double[] concUpperBound) {
		for (int j = 0; j < concUpperBound.length; j++) {
			ub[j + target.length] = concUpperBound[j];
		}
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
	 * Method to set the whole upper bounds of fluxes.
	 * @param fluxUpperBound
	 */
	public void setUbOfReactions(double[] fluxUpperBound) {
		for (int j = 0; j < fluxUpperBound.length; j++) {
			ub[j] = fluxUpperBound[j];
		}
	}

	/**
	 * Method to show if the fluxes add to null.
	 * @param doc
	 */
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
	 * Calls a method to solve the problem with linear programming if the boolean
	 * {@link# linearProgramming} is set and true, else it calls a method for quadratic programming.
	 * @return double[]
	 * @throws IloException 
	 */
	public double[] solve() throws Exception {
		target = targetFunction.computeTargetFunctionForQuadraticProgramming();
		return solveWithQuadraticProgramming();
	}

	/**
	 * Calls CPLEX to solve the problem with quadratic programming and sets in the end
	 * the computed solutions for concentrations and fluxes.
	 * @throws Exception 
	 */
	private double[] solveWithQuadraticProgramming() throws Exception {
		// create the cplex solver
		IloCplex cplex = new IloCplex();
	
		// TARGET
		// create upper bounds (ub) and lower bounds (lb) for the variables x
		int[] counter = targetFunction.getCounterArray();
	
		// create variables with upper bounds and lower bounds
		IloNumVar[] x = cplex.numVarArray((target_length_without_all_flux_values  + concentrations.length), lb, ub);
	
		//compute the target function for cplex with the scalar product (lin)
		IloNumExpr lin = cplex.numExpr();
		for (int i = 0; i < counter[1]; i++) {
			//for every flux value only one variable
			lin = cplex.sum(lin, cplex.prod(target[i], x[0]));
		}
		// for all other values in the target-array
		for (int i = counter[1]; i < target.length; i++) {
			lin = cplex.sum(lin, cplex.prod(target[i], x[i + 1 - counter[1]]));
		}
	
	
		//lambda1*sum((c_i - c_eq)^2)
		double[] c_eq = constraints.getEquilibriumConcentrations();
		IloNumExpr sum = cplex.numExpr();
		for (int i = 0; i< concentrations.length; i++) {
			IloNumExpr temp = sum;
			IloNumExpr c_i = cplex.prod(x[i + target_length_without_all_flux_values], concentrations[i]);
			if(!Double.isNaN(concentrations[i]) && !Double.isNaN(c_eq[i])) {
				// here the sum (c_i - c_eq)^2 is performed by cplex. To make it simpler for cplex, the binomial formula is used
				// where cplex.square(x) = x^2
				sum = cplex.sum(temp, cplex.square(c_i), cplex.sum(cplex.prod(c_i, ((-2) * c_eq[i])), cplex.square(cplex.constant(c_eq[i]))));
			} else if(!Double.isNaN(c_eq[i])){
				sum = cplex.sum(temp, cplex.square(x[i + target_length_without_all_flux_values]),cplex.sum(cplex.prod(x[i + target_length_without_all_flux_values], ((-2) * c_eq[i])), cplex.square(cplex.constant(c_eq[i]))));
			} else {
				sum = cplex.sum(temp ,cplex.square(x[i + target_length_without_all_flux_values]));
			}
		}
		// now put the variables together  
		IloNumExpr cplex_target = cplex.sum(cplex.prod(TargetFunction.lambda1, sum),lin);
	
		// only for FluxMinimization the target function has to be minimized
		if (targetFunction instanceof FluxMinimization) {
			cplex.addMinimize(cplex_target);
		} else { //TODO: implement more target functions
			cplex.addMaximize(cplex_target);
		}
	
		//CONSTRAINTS
	
		SBMLDocument modifiedDocument = FluxMinimizationUtils.getExpandedDocument(constraints.originalDocument, constraints.getSystemBoundaries());
	
		double[] steadyStateFluxes = targetFunction.getFluxVector();
		double[] compGibbs = constraints.getComputedGibbsEnergies();
		double r_max = constraints.computeR_max(steadyStateFluxes);
	
		//TODO
		System.out.println("ConstraintJ_rmaxG<0: " + isConstraintJ_rmaxG() + " ConstraintJ>0: " + isConstraintJ0() + " ConstraintJG: " + isConstraintJG());
		for (int j = 0; j< counter[1]; j++) {
			//jg is the expression for J_j * G_j
			//and j_rmaxG the expression for |J_j| - r_max * |G_j|
			int k = j + target_length_without_all_flux_values -	compGibbs.length;
	
			IloNumExpr j_j = cplex.abs(cplex.prod(Math.abs(steadyStateFluxes[j]), x[0]));
	
			// constraint |J_j| - r_max * |G_j| < 0
			if (isConstraintJ_rmaxG()) {
				IloNumExpr rmaxG = cplex.numExpr();
				if (!Double.isNaN(compGibbs[j]) && !Double.isInfinite(compGibbs[j])) {
					rmaxG = cplex.prod(r_max, cplex.abs(cplex.constant(compGibbs[j])));
				} else {
					rmaxG = cplex.prod(r_max, cplex.abs(x[k]));
				}
	
				cplex.addLe(cplex.diff(j_j, rmaxG), -Double.MIN_VALUE);
				logger.log(Level.DEBUG, String.format("constraint |J_j| - r_max * |G_j| < 0:  "+ cplex.diff(j_j, rmaxG)+ " < " + 0 ));
			}
	
			// constraint J_j >= 0
			if (isConstraintJ0()) {
				cplex.addGe(cplex.prod(steadyStateFluxes[j], x[0]), 0);
			}
	
			// constraint to compute the error
			if (isConctraintError()) {
				if (!Double.isNaN(compGibbs[j])) {
	
					/*
					 *  x[j+1] are the variables for the error vector, because in the target-array
					 *  the error vector comes after the fluxvector and the fluxes have only one
					 *  variable to be optimized
					 */
					cplex.addEq(cplex.prod(compGibbs[j], x[k]), cplex.prod(compGibbs[j], x[j+1]));
				}
			}
	
	
			// constraint J_j * G_j < 0
			if (isConstraintJG()) {
				IloNumExpr jg = cplex.numExpr();
				if (!Double.isNaN(compGibbs[j]) && !Double.isInfinite(compGibbs[j])) {
					if(Math.signum(steadyStateFluxes[j]) == Math.signum(compGibbs[j])) {
						//the sign is equal, so the gibbs energy must be changed
						jg = cplex.prod(cplex.prod(steadyStateFluxes[j], x[0]),cplex.prod(compGibbs[j],x[k]));
					} else {
						jg = cplex.prod(cplex.prod(steadyStateFluxes[j], x[0]),compGibbs[j]);
					}
				} else {
					jg = cplex.prod(steadyStateFluxes[j],x[k]);
				}
	
				cplex.addLe(jg, 0);
				// TODO sysout
				System.out.println(modifiedDocument.getModel().getReaction(j) + ": " + jg + " =< " + 0);
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
			solutionFluxVector[i] = steadyStateFluxes[i] * solution[0];
		}
		for (int i = 1; i <= constraints.getComputedGibbsEnergies().length; i++) {
			// after the flux vector is the error vector in the target function
			if(!Double.isNaN(compGibbs[i-1])) {
				solutionErrors[i-1] = solution[i];
			}
		}
		for (int j = 0; j < concentrations.length; j++) {
			// the last values in x are corresponding to the concentrations
			if (!Double.isNaN(concentrations[j])) {
				//correct the computed solutions by multiply them with the computed variable-value
				solutionConcentrations[j] = concentrations[j] * solution[j + target_length_without_all_flux_values];
			} else {
				solutionConcentrations[j] = solution[j + target_length_without_all_flux_values];
			}
		}
		cplex.end();
	
		// create the MultiTable for visualization
		fluxesForVisualization = createMultiTableForVisualizing();
		
		return solution;
	}

}
