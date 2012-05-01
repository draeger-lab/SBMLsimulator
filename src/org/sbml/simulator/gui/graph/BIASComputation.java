/*
 * $Id$
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
package org.sbml.simulator.gui.graph;

/**
 * work in progress
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class BIASComputation{
    
    /**
     * Minimum size of a node
     */
    private static final int MIN_OUT_NODE = 7;
    
    /**
     * Maximum size of a node
     */
    private static final int MAX_OUT_NODE = 50;
    
    /**
     * yintercept of linear regression
     */
    private static double yintercept;

    /**
     * Compute slope of linear regression
     * 
     * @param lowerLimit
     * @param upperLimit
     * @return
     */
    public static double computeBIAS(double lowerLimit, double upperLimit){
        double slope = (MAX_OUT_NODE-MIN_OUT_NODE) / (upperLimit - lowerLimit);
        yintercept = MIN_OUT_NODE-slope*lowerLimit;
//        System.out.println("m = " +slope +" y = " + yintercept);
        return slope;
    }
    
    /**
     * Returns the yintercept.
     * Only to use after {@link computeBIAS}.
     * @return yintercept if already computed, otherwise 0
     */
    public static double getYintercept(){
        return yintercept;
    }
    
}
