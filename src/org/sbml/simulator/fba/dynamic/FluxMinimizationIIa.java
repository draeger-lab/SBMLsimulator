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
	


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.simulator.fba.dynamic.TargetFunction#createTargetFunction(ilog
	 * .cplex.IloCplex)
	 */
	@Override
	public IloNumExpr createTargetFunction(IloCplex cplex) throws IloException {
		double delta_t = DynamicFBA.dFBATimePoints[1]
				- DynamicFBA.dFBATimePoints[0];
		
		double[] logarithms = new double[this.completeConcentrations[this.getTimePointStep()].length];
		IloNumExpr function = cplex.numExpr();
		
		// One variable for all fluxes in the flux vector
		IloNumExpr flux = cplex.numExpr();
		int fluxPosition = 0;
		// Manhattan norm included
		for (int i = 0; i < getTargetVariablesLengths()[0]; i++) {
			flux = cplex.sum(flux, cplex.prod(cplex.constant(this.lambda_1), cplex.abs(getVariables()[fluxPosition + i])));
		}
		
		// Concentrations 
		IloNumExpr concentrations = cplex.numExpr();
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		double[] c_m_measured = this.completeConcentrations[this.getTimePointStep()];
		
		for (int n = 0; n < getTargetVariablesLengths()[1]; n++) {
			IloNumExpr optimizingConcentration = cplex.numExpr();
			
			if (!Double.isNaN(c_m_measured[n])) {
				
				logarithms[n] = 0;
				
				if(c_m_measured[n] > 0) {
					logarithms[n] = Math.max(-1 * Math.log10(c_m_measured[n]), logarithms[n]);
				}
				
				if(this.getTimePointStep() > 0) {
					double previousValue = completeConcentrations[this.getTimePointStep()-1][n-1];
					if((!Double.isNaN(previousValue)) && (previousValue > 0))  {
						logarithms[n] = Math.min(Math.max(-1 * Math.log10(previousValue), 0), logarithms[n]);
					}
				}
				double volume = 1/factors[n];
				optimizingConcentration = cplex.prod(Math.pow(10, logarithms[n]), cplex.abs(cplex.diff(c_m_measured[n], getVariables()[concentrationPosition + n])));
				concentrations = cplex.sum(concentrations, cplex.prod(cplex.constant(this.lambda_2), cplex.prod(volume, cplex.prod(1 / delta_t, optimizingConcentration))));
			} else {
				// TODO if c_m_measured[n] is NaN???
			}
		}
		
		// Sum up each term
		function = cplex.sum(flux, concentrations);
		
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
		if (this.constraintJ0 == true) {
			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
				cplex.addGe(getVariables()[fluxPosition + j], 0);
			}
		}
		
		for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
			// Flux J_j
			if (knownFluxes.containsKey(j)) {
				double eightyPercent = 0.8 * knownFluxes.get(j);
				
				IloNumExpr j_j_min = cplex.numExpr();
				if (FluxMinimizationUtils.reverseReaction.containsKey(j)) {
					j_j_min = cplex.diff(
						getVariables()[fluxPosition + j],
						getVariables()[fluxPosition
								+ FluxMinimizationUtils.reverseReaction.get(j)]);
				} else {
					j_j_min = getVariables()[fluxPosition + j];
				}
				
				if (eightyPercent >= 0) {
					cplex.addGe(j_j_min, eightyPercent);
				} else {
					cplex.addLe(j_j_min, eightyPercent);
				}
			}
		}
		
		// Constraint flux pairs
		if (fluxPairs != null) {
			for (int j = 0; j < fluxPairs.length; j++) {
//				System.out.println(fluxPairs[j]);
				if (fluxPairs[j].replaceAll("[\\+\\-\\=\\d]", "").length() > 0){
					continue;
				}
				String[] hEq = fluxPairs[j].split("=");
				int iLeft = Integer.parseInt(hEq[0]);
				IloNumExpr leftReaction = getVariables()[fluxPosition + iLeft];
				IloNumExpr sameFluxes = null;
				
				Pattern pPlus = Pattern.compile("\\+(\\d+)");
				Pattern pMinus = Pattern.compile("\\-(\\d+)");
				
				Matcher mPlus = pPlus.matcher(hEq[1]);
				while (mPlus.find()) {
					int iPlus = Integer.parseInt(mPlus.group(1));
					if (sameFluxes == null) {
						sameFluxes = getVariables()[fluxPosition + iPlus];
					}
					else {
						sameFluxes = cplex.sum(sameFluxes, getVariables()[fluxPosition + iPlus]);
					}
				}
				
				Matcher mMinus = pMinus.matcher(hEq[1]);
				while (mMinus.find()) {
					int iMinus = Integer.parseInt(mMinus.group(1));
					if (sameFluxes == null) {
						sameFluxes = cplex.negative(getVariables()[fluxPosition + iMinus]);
					}
					else {
						sameFluxes = cplex.diff(sameFluxes, getVariables()[fluxPosition + iMinus]);
					}
				}
				
				cplex.addEq(leftReaction, sameFluxes);
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
				
				if(sign * deltaConc >= 0) {
					cplex.addGe(netFlux, cplex.constant(sign * this.conversionFactor * deltaConc));
				}
				else {
					cplex.addLe(netFlux, cplex.constant(sign * this.conversionFactor * deltaConc));
				}
			
			}
		}
		
		// Constraint z_m (t_i+1) >= 0
		if (this.constraintZm == true) {
			for (int m = 0; m < speciesCount; m++) {
				// Concentration z_m (t_i+1)
				cplex.addGe(getVariables()[concentrationPosition + m], 0);
			}
		}
		
		// Computation of z_m (t_i+1)
		
		// Use this computation of delta_t:
		// Only if each timepoint has the same distance to its neighboring timepoint
		double delta_t = DynamicFBA.dFBATimePoints[1] - DynamicFBA.dFBATimePoints[0];
		
		for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
			
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
					c_k_ti_1 = previousEstimatedConcentrations;
				}
				if (!Double.isNaN(completeConcentrations[this.getTimePointStep() - 1][i])) {
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
