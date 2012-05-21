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

import java.awt.Color;

import org.sbml.jsbml.SBMLDocument;

import y.view.NodeRealizer;
import de.zbit.graph.io.SBML2GraphML;


/**
 * This class is a {@link GraphManipulator}. It changes the color of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ManipulatorOfNodeColor extends AbstractGraphManipulator{
    
    /**
     * Field for the first color (high concentration) and second color (low concentration).
     */
    private int[] RGBcolor1, RGBcolor2;
    
    /**
     * Slope of linear regression m, and yintercept c.
     */
    private double m, c;
    
    /**
     * Constructs node-color manipulator with default gradient red (high) ->
     * blue (low) and default node size and default reactions linewidths.
     * 
     * @param graph
     * @param document
     * @param minMaxOfSpeciesData
     * @param minMaxOfReactionsData
     */
    public ManipulatorOfNodeColor(SBML2GraphML graph, SBMLDocument document,
            double[] minMaxOfSpeciesData,
            double[] minMaxOfReactionsData) {
        super(graph, document, minMaxOfReactionsData, (float)0.1, 6);
        DEFAULT_NODE_SIZE = 30;
        RGBcolor1 = new int[]{255, 0, 0};
        RGBcolor2 = new int[]{0, 0, 255};
        computeSpeciesAdjusting(minMaxOfSpeciesData[0], minMaxOfSpeciesData[1]);
    }
    
    /**
     * Constructs node-color manipulator with the given RGB colors as color1 (high) ->
     * color2 (low) and a constant node size.
     * @param graph
     * @param document
     * @param color1
     * @param color2
     * @param minMaxOfSpeciesData
     * @param minMaxOfReactionsData
     * @param nodeSize
     */
    public ManipulatorOfNodeColor(SBML2GraphML graph, SBMLDocument document,
            double[] minMaxOfSpeciesData, double[] minMaxOfReactionsData,
            double nodeSize, Color color1, Color color2, float reactionsMinLineWidth, float reactionsMaxLineWidth) {
        super(graph, document, minMaxOfReactionsData, reactionsMinLineWidth, reactionsMaxLineWidth);
        DEFAULT_NODE_SIZE = nodeSize;
        int r = color1.getRed();
        int g = color1.getGreen();
        int b = color1.getBlue();
        RGBcolor1 = new int[] { r, g, b };
        r = color2.getRed();
        g = color2.getGreen();
        b = color2.getBlue();
        RGBcolor2 = new int[] { r, g, b };
        computeSpeciesAdjusting(minMaxOfSpeciesData[0], minMaxOfSpeciesData[1]);
    }
    
    /**
     * Computes adjusting values for given limits.
     * @param lowerSpeciesLimit
     * @param upperSpeciesLimit
     */
    private void computeSpeciesAdjusting(double lowerSpeciesLimit, double upperSpeciesLimit){
        double[] linearRegression = computeBIAS(lowerSpeciesLimit, upperSpeciesLimit, 0, 1);
        m = linearRegression[0];
        c = linearRegression[1];
    }
    
    /**
     * Linear interpolation over two internally stored colors.
     * @param percent double between 0 and 1
     * @return color of the linear interpolation, null on false input.
     */
    private int[] linearColorInterpolation(double percent){
        int[] outcolor = {0, 0, 0};
        if (percent >= 0 && percent <= 1) {
            for (int i = 0; i < outcolor.length; i++) {
                outcolor[i] = (int) (RGBcolor1[i] * percent + RGBcolor2[i]
                        * (1 - percent));
            }
            return outcolor;
        } else if (percent > 1) {
            //maybe round-off error
            return RGBcolor1;
        } else {
            //maybe round-off error
            return RGBcolor2;
        }
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfNode(java.lang.String, double, boolean)
     */
    @Override
    public void dynamicChangeOfNode(String id, double value, boolean labels) {
        NodeRealizer nr = graph.getSimpleGraph()
                .getRealizer(graph.getId2node().get(id));
        nr.setSize(DEFAULT_NODE_SIZE, DEFAULT_NODE_SIZE); //standard node size
        int[] RGBinterpolated = linearColorInterpolation(value*m+c);
        nr.setFillColor(new Color(RGBinterpolated[0], RGBinterpolated[1], RGBinterpolated[2]));
        
        /*
         * Label Node with ID and real value at this timepoint. Last label will
         * be treated as dynamic label
         */
        if (labels) {
            labelNode(nr, id, value);
        } else if (nr.labelCount() > 1) {
            // labels switched off, therefore remove them, if there are any
            nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
        }
        graph.getSimpleGraph().updateViews();
    }
}
