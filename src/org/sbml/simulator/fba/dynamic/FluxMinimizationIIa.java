/*
 * $Id$ $URL:
 * FluxMinimizationIIa.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.fba.dynamic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sbml.simulator.fba.controller.FluxMinimizationUtils;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

/**
 * @author Roland Keller
 * @version $Rev$
 */
public class FluxMinimizationIIa extends FluxMinimizationII {
	
	private static double epsilon = 1E-12;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.simulator.fba.dynamic.TargetFunction#createTargetFunction(ilog
	 * .cplex.IloCplex)
	 */
	@Override
	public IloNumExpr createTargetFunction(IloCplex cplex) throws IloException {
		double delta_t = DynamicFBA.dFBATimePoints[1] - DynamicFBA.dFBATimePoints[0];
		if(this.getTimePointStep() > 0) {
			delta_t = DynamicFBA.dFBATimePoints[this.getTimePointStep()] - DynamicFBA.dFBATimePoints[this.getTimePointStep()-1];
		}
		
		IloNumExpr function = cplex.numExpr();
		
		// Fluxes term
		IloNumExpr flux = cplex.numExpr();
		int fluxPosition = 0;
		int reactionCount = getTargetVariablesLengths()[0];
		
//		double[][] lastSolution = getOptimizedSolution();
		
		// Manhattan norm included in cplex.abs()
		for (int j = 0; j < reactionCount; j++) {
			IloNumExpr optimizingFluxes = cplex.numExpr();
			
			// penalty to support non zero fluxes
			double notNullPenalty;
			double lastFlux = 0;
//			if (this.getTimePointStep() > 0) { // TODO use temporarmultitable 
//				// get last flux
//				if (!Double.isNaN(this.completeNetFluxes[this.getTimePointStep() - 1][j])) {
//					lastFlux = this.completeNetFluxes[this.getTimePointStep() - 1][j];
//				}
//				else {
//					lastFlux = lastSolution[1][j];
//				}
//			}
//			else {
//				if (!Double.isNaN(this.completeNetFluxes[this.getTimePointStep()][j])) {
//					lastFlux = this.completeNetFluxes[this.getTimePointStep()][j];
//				}
//			}
			notNullPenalty = 1/(lastFlux + epsilon);
			
			double delta_f = epsilon;
			if (this.getTimePointStep() > 1) {
				delta_f = Math.abs(this.tempSolutionMultiTable.getBlock(1).getValueAt(this.getTimePointStep() - 1, j)
						- this.tempSolutionMultiTable.getBlock(1).getValueAt(this.getTimePointStep() - 2, j));
			}
			
//			if (!this.splittedDocument.getModel().getReaction(j).isFast()) {
//				IloNumExpr j_j_min = cplex.numExpr();
//				if (FluxMinimizationUtils.reverseReaction.containsKey(j)) {
//					j_j_min = cplex.diff(
//						getVariables()[fluxPosition + j],
//						getVariables()[fluxPosition	+ FluxMinimizationUtils.reverseReaction.get(j)]);
//				} else {
//					j_j_min = getVariables()[fluxPosition + j];
//				}
//					cplex.diff(j_j_min, cplex.constant(this.tempSolutionMultiTable.getBlock(1).getValueAt(this.getTimePointStep() - 1, j)));
//			}

			
//			System.out.println(delta_f);
			
//			System.out.println("ncp:" + noChangePenalty);
//			optimizingFluxes = cplex.prod(notNullPenalty, cplex.abs(getVariables()[fluxPosition + j]));
//			optimizingFluxes = cplex.prod(1/delta_t, cplex.prod(notNullPenalty, getVariables()[fluxPosition + j]));
//			optimizingFluxes = cplex.prod(delta_f, cplex.prod(1/delta_t, cplex.prod(notNullPenalty, getVariables()[fluxPosition + j])));
//			optimizingFluxes = cplex.prod(1/delta_t, cplex.abs(getVariables()[fluxPosition + j]));
//			optimizingFluxes = getVariables()[fluxPosition + j];
			optimizingFluxes = cplex.abs(getVariables()[fluxPosition + j]);
			
			flux = cplex.sum(flux, optimizingFluxes);
		}
		
		// Concentrations term
		IloNumExpr concentrations = cplex.numExpr();
		int concentrationPosition = fluxPosition + reactionCount;
		double[] c_m_measured = this.completeConcentrations[this.getTimePointStep()];
		
		int speciesCount = getTargetVariablesLengths()[1];
		for (int i = 0; i < speciesCount; i++) {
			IloNumExpr optimizingConcentration = cplex.numExpr();
			
			if (!Double.isNaN(c_m_measured[i])) {
				// the bigger the change, the better it is (should supporting non zero concentrations)
				double div = epsilon; 
				// the bigger the concentration change, the more penalty it has
				double delta_c = epsilon;
				if(this.getTimePointStep() > 0) {
					double c_m_measured_previous = completeConcentrations[this.getTimePointStep()-1][i];
					if((!Double.isNaN(c_m_measured_previous)))  {
						div+= c_m_measured_previous;
						delta_c = (c_m_measured_previous - c_m_measured[i]) * (c_m_measured_previous - c_m_measured[i]);
					}
				}
				else {
					div+= c_m_measured[i];
					delta_c = epsilon;
				}
				
//				System.out.println(delta_c);
				double volume = 1/factors[i];
//				optimizingConcentration = cplex.abs(cplex.diff(c_m_measured[i], getVariables()[concentrationPosition + i]));
//				optimizingConcentration = cplex.prod(delta_c, cplex.abs(cplex.diff(c_m_measured[i], getVariables()[concentrationPosition + i])));
//				optimizingConcentration = cplex.prod(1/div, cplex.prod(concChange, cplex.abs(cplex.diff(c_m_measured[i], getVariables()[concentrationPosition + i]))));
				optimizingConcentration = cplex.prod(1/div, cplex.abs(cplex.diff(c_m_measured[i], getVariables()[concentrationPosition + i]))); // TODO roland
				
//				concentrations = cplex.sum(concentrations, cplex.prod(volume, optimizingConcentration));
				concentrations = cplex.sum(concentrations, cplex.prod(volume, cplex.prod(1 / delta_t, optimizingConcentration))); // TODO roland
//				concentrations = cplex.sum(concentrations, optimizingConcentration);
				
			} else {
				// TODO if c_m_measured[i] is NaN??? is it possible to calculate from stoichiometric matrix and flux?
			}
		}
		
		// Sum up each term
		function = cplex.sum(cplex.prod(cplex.constant(this.lambda_1), flux), cplex.prod(cplex.constant(this.lambda_2), concentrations));
		
//		System.out.println(function.toString());
		return function;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.simulator.fba.dynamic.TargetFunction#addConstraintsToTargetFunction
	 * (ilog.cplex.IloCplex)
	 */
	@Override
	public void addConstraintsToTargetFunction(IloCplex cplex)
		throws IloException {
		int speciesCount = this.splittedDocument.getModel().getSpeciesCount();
		
		int fluxPosition = 0;
		int concentrationPosition = getTargetVariablesLengths()[0];
		
		// TODO fixed assumptions 
//		cplex.addGe(getVariables()[fluxPosition + 5], Double.MIN_VALUE);
		
		// Constraint J_j >= 0
		if (isConstraintJ0()) {
			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
				cplex.addGe(getVariables()[fluxPosition + j], 0);
			}
		}
		
		// Constraint supporting little flux changes
		if (isLittleFluxChanges()) {
			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
				if (this.getTimePointStep() > 0) {
					if (!this.splittedDocument.getModel().getReaction(j).isFast()) {
						IloNumExpr j_j_min = cplex.numExpr();
						j_j_min = getVariables()[fluxPosition + j];
						cplex.diff(j_j_min, cplex.constant(this.tempSolutionMultiTable.getBlock(1).getValueAt(this.getTimePointStep() - 1, j)));
					}
					else {
						// TODO add what should happen if the reaction is a fast reaction
					}
				}
			}
		}
		
		// Constraint supporting net fluxes greater then zero
		if (isFluxDynamic()) {
			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
				IloNumExpr j_j_min = cplex.numExpr();
				if (FluxMinimizationUtils.reverseReaction.containsKey(j)) {
					j_j_min = cplex.diff(
							getVariables()[fluxPosition + j],
							getVariables()[fluxPosition	+ FluxMinimizationUtils.reverseReaction.get(j)]);
				} else {
					j_j_min = getVariables()[fluxPosition + j];
				}
				
//				cplex.addGe(cplex.constant(cplex.getValue(j_j_min)), epsilon);
				
				cplex.maximize(j_j_min);
				
				
//				cplex.le(cplex.negative(j_j_min), (-1.0 * epsilon));
//				cplex.or(cplex.addGe(j_j_min, epsilon), cplex.addLe(j_j_min, (-1.0 * epsilon)));
			}
		}
		
		// Constraint of using known fluxes from the given multitable
		if (this.useKnownFluxes) {
			for (int j = 0; j < DynamicFBA.originalDocument.getModel().getReactionCount(); j++) {
				// Flux J_j
				if ((this.getTimePointStep() > 0) && !Double.isNaN(this.completeNetFluxes[this.getTimePointStep()][j])) {
					double knownFluxValue = this.completeNetFluxes[this.getTimePointStep()][j];

					IloNumExpr j_j_min = cplex.numExpr();
					if (FluxMinimizationUtils.reverseReaction.containsKey(j)) {
						j_j_min = cplex.diff(
								getVariables()[fluxPosition + j],
								getVariables()[fluxPosition	+ FluxMinimizationUtils.reverseReaction.get(j)]);
					} else {
						j_j_min = getVariables()[fluxPosition + j];
					}
					cplex.addEq(j_j_min, knownFluxValue);
				}
			}
		}
		
//		if (!knownFluxes.isEmpty()) {
//			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
//				// Flux J_j
//				if ((knownFluxes.containsKey(j)) && (this.getTimePointStep() > 0) && !Double.isNaN(this.completeNetFluxes[this.getTimePointStep()][j])) {
//					double knownFluxValue = 1 * knownFluxes.get(j)[this.getTimePointStep()-1];
//
//					IloNumExpr j_j_min = cplex.numExpr();
//					if (FluxMinimizationUtils.reverseReaction.containsKey(j)) {
//						j_j_min = cplex.diff(
//								getVariables()[fluxPosition + j],
//								getVariables()[fluxPosition	+ FluxMinimizationUtils.reverseReaction.get(j)]);
//					} else {
//						j_j_min = getVariables()[fluxPosition + j];
//					}
//					cplex.addEq(j_j_min, knownFluxValue);
//				}
//			}
//		}
		
		// Constraint flux pairs
		if (fluxPairs != null) {
			for (int j = 0; j < fluxPairs.length; j++) {
//				System.out.println(fluxPairs[j]);
				if (fluxPairs[j].replaceAll("[\\+\\-\\=\\d]", "").length() > 0){
					continue;
				}
				String[] hEq = fluxPairs[j].split("=");
				IloNumExpr leftFluxes = null;
				IloNumExpr rightFluxes = null;
				
				Pattern pPlus = Pattern.compile("\\+(\\d+)");
				Pattern pMinus = Pattern.compile("\\-(\\d+)");
				
				Matcher mPlus = pPlus.matcher(hEq[1]);
				while (mPlus.find()) {
					int iPlus = Integer.parseInt(mPlus.group(1));
					if (rightFluxes == null) {
						rightFluxes = getVariables()[fluxPosition + iPlus];
					}
					else {
						rightFluxes = cplex.sum(rightFluxes, getVariables()[fluxPosition + iPlus]);
					}
				}

				mPlus = pPlus.matcher(hEq[1]);
				while (mPlus.find()) {
					int iPlus = Integer.parseInt(mPlus.group(1));
					if (leftFluxes == null) {
						leftFluxes = getVariables()[fluxPosition + iPlus];
					}
					else {
						leftFluxes = cplex.sum(leftFluxes, getVariables()[fluxPosition + iPlus]);
					}
				}

				Matcher mMinus = pMinus.matcher(hEq[1]);
				while (mMinus.find()) {
					int iMinus = Integer.parseInt(mMinus.group(1));
					if (rightFluxes == null) {
						rightFluxes = cplex.negative(getVariables()[fluxPosition + iMinus]);
					}
					else {
						rightFluxes = cplex.diff(rightFluxes, getVariables()[fluxPosition + iMinus]);
					}
				}
				
				mMinus = pMinus.matcher(hEq[1]);
				while (mMinus.find()) {
					int iMinus = Integer.parseInt(mMinus.group(1));
					if (leftFluxes == null) {
						leftFluxes = cplex.negative(getVariables()[fluxPosition + iMinus]);
					}
					else {
						leftFluxes = cplex.diff(leftFluxes, getVariables()[fluxPosition + iMinus]);
					}
				}
				
				cplex.addEq(leftFluxes, rightFluxes);
			}
		}
		
		// Constraint transport flux rules
		if (this.transportFluxes != null && conversionFactor != null && this.getTimePointStep() != 0) {
			for (int t = 0; t < transportFluxes.length; t++) {
				String current = transportFluxes[t];
				Double sign;
				if (current.contains("import:")) {
					current = current.replace("import:", "");
					sign = -1.0;
				}
				else {
					current = current.replace("export:", "");
					sign = +1.0;
				}
				String[] h = current.split("=");
				int forward = Integer.parseInt(h[0].split(",")[0]);
				int backward = Integer.parseInt(h[0].split(",")[1]);
				int species = Integer.parseInt(h[1]);
				IloNumExpr netFlux = cplex.diff(getVariables()[fluxPosition + forward], getVariables()[fluxPosition + backward]);
				double deltaConc = this.completeConcentrations[this.getTimePointStep()][species] - this.completeConcentrations[this.getTimePointStep() - 1][species];
				
//				if(sign * deltaConc >= 0) {
//					cplex.addGe(netFlux, cplex.constant(sign * this.conversionFactor * deltaConc));
//				}
//				else {
					cplex.addLe(netFlux, cplex.constant(sign * this.conversionFactor * deltaConc));
//				}
			
			}
		}
		
		// Constraint z_m (t_i+1) >= 0
		if (isConstraintZm()) {
			for (int m = 0; m < speciesCount; m++) {
				// Concentration z_m (t_i+1)
				cplex.addGe(getVariables()[concentrationPosition + m], 0);
			}
		}
		
		// Computation of z_m (t_i+1)
		
		// Use this computation of delta_t:
		// Only if each timepoint has the same distance to its neighboring timepoint
		double delta_t = DynamicFBA.dFBATimePoints[1] - DynamicFBA.dFBATimePoints[0];
		if(this.getTimePointStep() > 0) {
			delta_t = DynamicFBA.dFBATimePoints[this.getTimePointStep()] - DynamicFBA.dFBATimePoints[this.getTimePointStep()-1];
		}
		
		for (int i = 0; i < getTargetVariablesLengths()[1]; i++) { // species count
			
			// Concentrations 
			double[] c_k_ti = this.completeConcentrations[this.getTimePointStep()];
			
			if (this.getTimePointStep() == 0) {
				// In the first time point step 0, there is no c_k (t_i+1).
				// t_i+1 would be time point step 1, not 0!
				if (!Double.isNaN(c_k_ti[i])) {
					cplex.addEq(getVariables()[concentrationPosition + i], cplex.constant(c_k_ti[i]));
				} else {
					// TODO if currentConcentrations[n] is NaN???
				}
			} else {
				double[] c_k_ti_1 = completeConcentrations[this.getTimePointStep() - 1];
				if(this.usePreviousEstimations) {
					for (int k = 0; k < c_k_ti_1.length; k++) {
						double estimated = previousEstimatedConcentrations[k];
						double given = c_k_ti_1[k];
						// allowing precision or baseline errors of 10 % // TODO check error assumption
						if (Double.isNaN(given) || (Math.abs(given - estimated) < Math.abs(0.10 * given))) { // less than 10 % difference to the given value, than assume given value
							// if given is NaN or estimated fits better than given
							c_k_ti_1[k] = estimated;
						}
					}
				}
				if (!Double.isNaN(c_k_ti_1[i])) { // changed to c_k_ti_1[i] from completeConcentrations[this.getTimePointStep() - 1][i] in case of pre estimated concentrations
					IloNumExpr computedConcentration = cplex.numExpr();
					IloNumExpr fNJ = cplex.numExpr();
					
					for (int j = 0; j < this.N_all.getColumnDimension(); j++) {
						double factor = factors[i] * this.N_all.get(i, j);
						fNJ = cplex.sum(fNJ, cplex.prod(cplex.constant(factor),
							getVariables()[fluxPosition + j]));
					}
					
					computedConcentration = cplex.sum(cplex.constant(c_k_ti_1[i]), cplex.prod(fNJ, cplex.constant(delta_t)));
					cplex.addEq(getVariables()[concentrationPosition + i], computedConcentration);
//					if (!Double.isNaN(c_m_measured[n])) {
//						cplex.addLe(cplex.abs(cplex.diff(getVariables()[concentrationPosition + n],
//							c_m_measured[n])), 1E-5);
//					}
				} else {
					// TODO if last concentration (completeConcentrations[this.getTimePointStep()-1][n]) is NaN???
				}
			}
			
		}
	}

	
	
}
