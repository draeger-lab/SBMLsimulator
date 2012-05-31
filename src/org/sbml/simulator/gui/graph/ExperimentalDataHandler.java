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

import org.sbml.simulator.math.SplineCalculation;
import org.simulator.math.odes.MultiTable;

import eva2.tools.math.interpolation.InterpolationException;


/**
 * work in progress... TODO not needed yet?!
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ExperimentalDataHandler {
    
    
    public ExperimentalDataHandler() throws InterpolationException{
    }
    
    /**
     * 
     * @param expData
     * @param steps
     * @return
     */
    public static MultiTable getInterpolateData(MultiTable expData, int steps) {
        return SplineCalculation.calculateSplineValues(expData, steps);
    }

    /**
     * Generate timepoints such that first timepoint of experimental data is
     * first timepoint of returned array and last timepoint of experimental
     * data corresponds to last timepoint in returned array. Eventually the
     * number of returned timepoints which are equally spaced, is equal to the
     * number of given steps.
     * 
     * @param expDataTimepoints
     * @param steps
     * @return array with #steps timepoints. 
     */
    private static double[] generateTimepoints(double[] expDataTimepoints, int steps) {
        double[] timepoints = new double[steps];
                
        //Given interval [a, b], (b-a)/n results in intervall length of n equally spaced intervals 
        double intervalLength = (expDataTimepoints[expDataTimepoints.length-1] - expDataTimepoints[0]) / (double)(steps-1);
        
        //first and last timepoint given
        timepoints[0] = expDataTimepoints[0];
        timepoints[timepoints.length-1] = expDataTimepoints[expDataTimepoints.length-1];
        
        for(int i = 1; i < timepoints.length-1; i++){
            timepoints[i] = timepoints[i-1] + intervalLength;
        }
        
        return timepoints;
    }
}
