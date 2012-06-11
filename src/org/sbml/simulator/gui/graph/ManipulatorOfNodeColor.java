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
import de.zbit.gui.ColorPalette;


/**
 * This class is a {@link GraphManipulator}. It changes the color of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ManipulatorOfNodeColor extends AbstractGraphManipulator{
    
    /**
     * Field for the first color (high concentration), second color (mid
     * concentration), third color (low concentration).
     */
    private int[] RGBcolor1, RGBcolor2, RGBcolor3;
    
    /**
     * Slope of linear regression m, and yintercept c.
     */
    private double m, c;
    
    /**
     * Constructs node-color manipulator with default gradient camine red (high)
     * -> white (mid) -> gold (low) and default node size and default reactions
     * linewidths.
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
        RGBcolor1 = new int[] { ColorPalette.CAMINE_RED.getRed(),
                ColorPalette.CAMINE_RED.getGreen(),
                ColorPalette.CAMINE_RED.getBlue() };
        RGBcolor2 = new int[]{255, 255, 255}; //white
        RGBcolor3 = new int[]{ ColorPalette.GOLD.getRed(),
                ColorPalette.GOLD.getGreen(),
                ColorPalette.GOLD.getBlue() };
        computeSpeciesAdjusting(minMaxOfSpeciesData[0], minMaxOfSpeciesData[1]);
    }
    
    /**
     * Constructs node-color manipulator with the given RGB colors as color1 (high) ->
     * color2 (mid) -> color3 (low) and a constant node size.
     * @param graph
     * @param document
     * @param minMaxOfSpeciesData
     * @param minMaxOfReactionsData
     * @param nodeSize
     * @param color1
     * @param color2
     * @param color3
     * @param reactionsMinLineWidth
     * @param reactionsMaxLineWidth
     */
    public ManipulatorOfNodeColor(SBML2GraphML graph, SBMLDocument document,
            double[] minMaxOfSpeciesData, double[] minMaxOfReactionsData,
            double nodeSize, Color color1, Color color2, Color color3, float reactionsMinLineWidth, float reactionsMaxLineWidth) {
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
        r = color3.getRed();
        g = color3.getGreen();
        b = color3.getBlue();
        RGBcolor3 = new int[] { r, g, b }; 
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
    
    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfNode(java.lang.String, double, boolean)
     */
    @Override
    public void dynamicChangeOfNode(String id, double value, boolean labels) {
        NodeRealizer nr = graph.getSimpleGraph()
                .getRealizer(graph.getId2node().get(id));
        double ratio = nr.getHeight() / nr.getWidth(); //keep ratio in case of elliptic nodes
        nr.setSize(DEFAULT_NODE_SIZE, DEFAULT_NODE_SIZE*ratio); //standard node size
        int[] RGBinterpolated = linearColorInterpolationForThree(value*m+c);
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
    
    /**
     * Linear interpolation over two given colors.
     * @param percent
     * @param color1
     * @param color2
     * @return
     */
    private int[] linearColorInterpolation(double percent, int[] color1, int[] color2){
        int[] outcolor = {0, 0, 0};
        if (percent >= 0 && percent <= 1) {
            for (int i = 0; i < outcolor.length; i++) {
                outcolor[i] = (int) (color1[i] * percent + color2[i]
                        * (1 - percent));
            }
            return outcolor;
        } else if (percent > 1) {
            //maybe round-off error
            return color1;
        } else {
            //maybe round-off error
            return color2;
        }
    }

    /**
     * Linear interpolation over three internally stored colors.
     * @param percent
     * @return
     */
    private int[] linearColorInterpolationForThree(double percent){
        if (percent >= 0 && percent <= 0.5) {
            // color interpolation between color2 (mid concentration) and color3
            // (low concentration)
            double[] tmpRegr = computeBIAS(0, 0.5, 0, 1);
            return linearColorInterpolation(percent*tmpRegr[0]+tmpRegr[1], RGBcolor2, RGBcolor3);
        } else if (percent > 0.5 && percent < 1.0){
            //color interpolation between color1 (high concentration) and color2 (mid concentration)
            double[] tmpRegr = computeBIAS(0.5, 1, 0, 1);
            return linearColorInterpolation(percent*tmpRegr[0]+tmpRegr[1], RGBcolor1, RGBcolor2);
        } else if (percent > 1) {
            // maybe round-off error
            return RGBcolor1;
        } else {
            // maybe round-off error
            return RGBcolor3;
        }
    }
}
