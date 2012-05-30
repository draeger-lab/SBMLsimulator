/*
 * $Id:  Constraints.java 16:17:07 Meike Aichele$
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.sbml.jsbml.SBMLDocument;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class Constraints {
	
	/**
	 * Contains the Gibbs energies 
	 */
	private double[] gibbsEnergies;
	
	/**
	 * Contains the opened {@link SBMLDocument}
	 */
	public SBMLDocument document;
	
	
	/**
	 * Constructor, that gets a {@link SBMLDocument} and creates a new
	 * array of Gibbs energies, that stays empty if the user doesn't check
	 * in a Gibbs energy file.
	 * 
	 * @param doc
	 */
	public Constraints (SBMLDocument doc) {
		this.document = doc;
		this.setGibbsEnergies(new double[doc.getModel().getReactionCount()]);
	}
	
	/**
	 * 
	 * @param doc
	 * @param gibbs
	 */
	public Constraints (SBMLDocument doc, double[] gibbs) {
		this.document = doc;
		this.setGibbsEnergies(gibbs);
	}

	
	/**
	 * @param gibbsEnergies the gibbsEnergies to set
	 */
	public void setGibbsEnergies(double[] gibbsEnergies) {
		this.gibbsEnergies = gibbsEnergies;
	}

	/**
	 * @return the gibbsEnergies
	 */
	public double[] getGibbsEnergies() {
		return gibbsEnergies;
	}
	
}
