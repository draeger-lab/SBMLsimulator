/*
 * $Id:  FluxMinimization.java 16:15:22 Meike Aichele$
 * $URL: FluxMinimization.java $
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

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimization extends TargetFunction {
	
	/**
	 * to compute a flux minimization, we need a flux vector v and an error-value, 
	 * which will be computed in Errorobject-class
	 */
	private Errorobject error;
	private FluxVector v;
	
	/*
	 * default constructor
	 */
	public FluxMinimization() {
		super();
		// TODO generate Errorobject error and FluxVector v
		this.error = null;
		this.v = null;
	}
	
	/**
	 * constructor that gets an Errorobject error and a FluxVector-object fluxvec.
	 * 
	 * @param error
	 * @param fluxvec
	 */
	public FluxMinimization(Errorobject error, FluxVector fluxvec) {
		this.error = error;
		this.v = fluxvec;
	}
	

	/**
	 * @param error the error to set
	 */
	public void setError(Errorobject error) {
		this.error = error;
	}

	/**
	 * @return the error
	 */
	public Errorobject getError() {
		return error;
	}

	/**
	 * @param v the v to set
	 */
	public void setFluxVector(FluxVector v) {
		this.v = v;
	}

	/**
	 * @return the v
	 */
	public FluxVector getFluxVector() {
		return v;
	}

}
