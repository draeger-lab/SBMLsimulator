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

import org.simulator.math.odes.MultiTable;

import eva2.tools.math.interpolation.InterpolationException;
import eva2.tools.math.interpolation.SplineInterpolation;


/**
 * work in progress...
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ExperimentalDataHandler {
    
//    private static SplineInterpolation spline = new SplineInterpolation(); //TODO ??
    
    public ExperimentalDataHandler() throws InterpolationException{
    }
    
    public static MultiTable getInterpolateData(MultiTable expData, int steps) {
        MultiTable interpolateData = expData;
        //set timepoints
        interpolateData.setTimePoints(generateTimepoints(expData.getTimePoints(), steps));
        //TODO possible change of timepoints array? if not possible => new table from scratch
        
        //set interpolate data
        for(int i = 1; i < interpolateData.getColumnCount(); i++){ //each column
            //TODO compute interpolation for this particular column
            //maybe just for reactions and species? --> sbmldocument needed
            
            /*
             * Eingabewert/Rückgabewert double[]?
            spline.spline(x, y, yp0, ypn);
            */
            
            for(int j = 0; j < interpolateData.getTimePoints().length; j++){ //each timepoint
                //interpolateData.setValueAt(spline.get(j), j, i);
                //TODO spline
            }
        }
        
        return interpolateData;
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

//    public static void main(String args[]){
//        double[] testTimepoints = generateTimepoints(new double[]{2}, 3);
//        for(double d : testTimepoints){
//            System.out.print(d + " ");
//        }
//    }
    
}
