/*
 * $Id:  ValueHolder.java 15:57:31 draeger$
 * $URL: ValueHolder.java $
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
package org.sbml.simulator.math;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public interface ValueHolder {

    /**
     * 
     * @param id
     * @return
     */
    public double getCurrentCompartmentSize(String id);
    
    /**
     * 
     * @param id
     * @return
     */
    public double getCurrentParameterValue(String id);
    
    /**
     * 
     * @param id
     * @return
     */
    public double getCurrentSpeciesValue(String id);
    
    /**
     * 
     * @param id
     * @return
     */
    public double getCurrentStoichiometry(String id);

    /**
     * 
     * @param id
     * @return
     */
	public double getCompartmentValueOf(String id);
	
	/**
     * 
     * @param id
     * @return
     */
	public double getValueOf(String id);

	/**
     * 
     * @param time
     * @return
     */
	public double getTime();

    
}