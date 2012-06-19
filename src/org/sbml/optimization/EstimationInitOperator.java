/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models of biochemical processes encoded in the modeling language SBML.
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
package org.sbml.optimization;

import eva2.server.go.individuals.AbstractEAIndividual;
import eva2.server.go.individuals.ESIndividualDoubleData;
import eva2.server.go.operators.initialization.InterfaceInitialization;
import eva2.server.go.problems.InterfaceOptimizationProblem;
import eva2.tools.math.RNG;

/**
 * @author Roland Keller
 * @version $Rev$
 */
public class EstimationInitOperator implements InterfaceInitialization {
	
	/*
	 * 
	 */
	private QuantityRange[] ranges;

	/*
	 * (non-Javadoc)
	 * @see eva2.server.go.operators.initialization.InterfaceInitialization#initialize(eva2.server.go.individuals.AbstractEAIndividual, eva2.server.go.problems.InterfaceOptimizationProblem)
	 */
	public void initialize(AbstractEAIndividual indy,
		InterfaceOptimizationProblem problem) {
		double[] data = ((ESIndividualDoubleData)indy).getDoubleData();
		for (int i = 0; i < data.length; i++) {
      if(ranges[i].isGaussianInitialization() && (ranges[i].getInitialMinimum() <= ranges[i].getInitialGaussianValue()) && (ranges[i].getInitialMaximum() >= ranges[i].getInitialGaussianValue())) {
      	double stdDev = (ranges[i].getGaussianStandardDeviation() / 100) * ranges[i].getInitialGaussianValue();
      	if(stdDev == 0) {
      		stdDev = ranges[i].getGaussianStandardDeviation();
      	}
      	data[i] = RNG.gaussianDouble(stdDev) + ranges[i].getInitialGaussianValue();
      	
      	if(data[i] < ranges[i].getInitialMinimum()) {
      		data[i] = ranges[i].getInitialMinimum();
      	}
      	if(data[i] > ranges[i].getInitialMaximum()) {
      		data[i] = ranges[i].getInitialMaximum();
      	}
      }
      else {
      	data[i] = RNG.randomDouble(ranges[i].getInitialMinimum(), ranges[i].getInitialMaximum());
      }
		}
	}
	
	/**
	 * 
	 */
	public InterfaceInitialization clone() {
		return new EstimationInitOperator(ranges);
	}
	
	/**
	 * 
	 * @param ranges
	 */
	public EstimationInitOperator(QuantityRange[] ranges) {
		this.ranges = ranges;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getName() {
		return "Initialization for parameter estimation of models";
	}
	
}
