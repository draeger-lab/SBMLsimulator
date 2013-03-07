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
				optimizingConcentration = cplex.prod(Math.pow(10, logarithms[n]), cplex.abs(cplex.diff(c_m_measured[n], getVariables()[concentrationPosition + n])));
			} else {
				// TODO if c_m_measured[n] is NaN???
			}
			
			concentrations = cplex.sum(concentrations, cplex.prod(cplex.constant(this.lambda_2), cplex.prod(1 / delta_t, optimizingConcentration)));
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
		
		// Constraint flux pairs
		if (fluxPairs != null) {
			for (int j = 0; j < fluxPairs.length; j++) {
				if (this.splittedDocument.getModel().getReaction(fluxPairs[j]) != null) {
					cplex.addEq(getVariables()[fluxPosition + j], getVariables()[fluxPosition + fluxPairs[j]]);
				}
			}
		}
				
		// Constraint J_j >= 0
		if (this.constraintJ0 == true) {
			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
				// Flux J_j
				cplex.addGe(getVariables()[fluxPosition + j], 0);
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
				if (!Double.isNaN(c_k_ti_1[i])) {
					IloNumExpr computedConcentration = cplex.numExpr();
					IloNumExpr fNJ = cplex.numExpr();
					
					for (int j = 0; j < this.N_all.getColumnDimension(); j++) {
						double factor = factors[i][j] * this.N_all.get(i, j);
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
