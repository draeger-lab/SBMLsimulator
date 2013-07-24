/*
 * $Id$
 * $URL$ 
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
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
import java.text.MessageFormat;
import java.util.logging.Logger;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.simulator.gui.table.LegendTableModel;

import y.view.NodeRealizer;
import de.zbit.graph.io.SBML2GraphML;

/**
 * This class is a {@link IGraphManipulator}. It changes the size of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ManipulatorOfNodeSize extends AGraphManipulator{   
    
    /**
     * A {@link Logger} for this class.
     */
    private static final transient Logger logger = Logger.getLogger(ManipulatorOfNodeSize.class.getName());
    
    /**
     * If provided, colors of {@link LegendTableModel} are used as node colors.
     */
    private LegendTableModel legendTable = null;
    
    /**
     * determines default node color.
     */
    private Color DEFAULT_NODE_COLOR = new Color(176, 226, 255);
    
    /**
     * Saves minimum and maximum value of selected Species to save computation
     * time.
     */
    private double[] minMaxOfSelectedSpecies;
    
    /**
     * Minimum and maximum Node Size with default initialization.
     */
    private double minNodeSize = DEFAULT_MIN_NODE_SIZE,
            maxNodeSize = DEFAULT_MAX_NODE_SIZE;
    
    /**
     * Concentration changes relativ or absolute? Per default false.
     */
    private boolean relativeConcentrations = false;
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size as given. If minimum node size greater than
     * maximum node size, default values will be used. Reactions line widths as
     * given. Node colors uniform as user given.
     * Concentration changes as user given.
     * 
     * @param graph
     * @param document
     * @param core
     * @param selectedSpecies
     * @param selectedReactions
     * @param minNodeSize
     * @param maxNodeSize
     * @param relativeConcentrations
     * @param reactionsMinLinewidth
     * @param reactionsMaxLineWidth
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document,
            DynamicCore core, Color uniformNodeColor, String[] selectedSpecies,
            String[] selectedReactions, double minNodeSize, double maxNodeSize,
            boolean relativeConcentrations, float reactionsMinLineWidth,
            float reactionsMaxLineWidth) {
        
        // no use of this() because of other super constructor
        super(graph, document, core, selectedReactions, reactionsMinLineWidth,
                reactionsMaxLineWidth);
        
        if (minNodeSize < maxNodeSize) {
            this.minNodeSize = minNodeSize;
            this.maxNodeSize = maxNodeSize;
        } // else ignore input and use default node sizes
        DEFAULT_NODE_COLOR = uniformNodeColor;
        REVERT_NODE_SIZE = DEFAULT_MIN_NODE_SIZE;
        this.relativeConcentrations = relativeConcentrations;
        /*
         * Store min/max once to save computation time in case of absolute
         * concentration changes.
         */
        minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
    }
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size as given. If minimum node size greater than
     * maximum node size, default values will be used. Reactions line widths as
     * given.
     * Nodecolors as in given {@link LegendTableModel}.
     * Concentration changes as user given.
     * 
     * @param graph
     * @param document
     * @param core
     * @param legendTableModel
     * @param selectedSpecies
     * @param selectedReactions
     * @param minNodeSize
     * @param maxNodeSize
     * @param relativeConcentrations
     * @param reactionsMinLinewidth
     * @param reactionsMaxLineWidth
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document,
            DynamicCore core, LegendTableModel legendTableModel,
            String[] selectedSpecies, String[] selectedReactions,
            double minNodeSize, double maxNodeSize,
            boolean relativeConcentrations, float reactionsMinLinewidth,
            float reactionsMaxLineWidth) {
        
        super(graph, document, core, selectedReactions, reactionsMinLinewidth,
                reactionsMaxLineWidth);
        
        this.legendTable = legendTableModel;
        if (minNodeSize < maxNodeSize) {
            this.minNodeSize = minNodeSize;
            this.maxNodeSize = maxNodeSize;
        } // else ignore input and use default node sizes
        this.relativeConcentrations = relativeConcentrations;
        
        /*
         * Store min/max once to save computation time in case of absolute
         * concentration changes.
         */
        minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
    }
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size per default and reactions line widths per
     * default.
     * Node colors per default value.
     * Concentration changes absolute per default.
     * 
     * @param graph
     * @param document
     * @param core
     * @param selectedSpecies
     * @param selectedReactions
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document,
            DynamicCore core, String[] selectedSpecies,
            String[] selectedReactions) {
        
        super(graph, document, core, selectedReactions, DEFAULT_MIN_LINEWIDTH,
                DEFAULT_MAX_LINEWIDTH);
        
        REVERT_NODE_SIZE = DEFAULT_MIN_NODE_SIZE;
        
        /*
         * Store min/max once to save computation time in case of absolute
         * concentration changes.
         */
        minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.IGraphManipulator#dynamicChangeOfNode(java.lang.String, double, double, boolean)
     */
    @Override
    public void dynamicChangeOfNode(String id, double value, boolean labels) {
        if (id2speciesNode.get(id) != null) {
            double minValue, maxValue; // values to compute adjusting 
            if (relativeConcentrations) {
                /*
                 * Realtive changes. Use species specific min/max values.
                 */
                minValue = id2minMaxData.get(id)[0];
                maxValue = id2minMaxData.get(id)[1];
            } else {
                /*
                 * Absolute changes. Use min/max values of all selected species.
                 */
                minValue = minMaxOfSelectedSpecies[0];
                maxValue = minMaxOfSelectedSpecies[1];
            }
            
            // compute adusting
            double size = adjustValue(minValue, maxValue, minNodeSize,
                    maxNodeSize, value);
            logger.finer(MessageFormat.format(
                    "Species {0}: value={1}, results in node size={2}",
                    new Object[] { id, value, size }));
            
            // visualize
            NodeRealizer nr = graph.getSimpleGraph()
                    .getRealizer(graph.getId2node().get(id));
            double ratio = nr.getHeight() / nr.getWidth(); //keep ratio in case of elliptic nodes
            nr.setSize(size, size*ratio);
            //use standard color if no legendTableModel is provided
            Color color = null;
            if (legendTable != null) {
                color = legendTable.getColorFor(id);
            } else if (color == null) {
                //ensure that color is never null in case of wrong LegendTableModel
                color = DEFAULT_NODE_COLOR;
            }
            nr.setFillColor(color);
    
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
            
            // update view
            graph.getSimpleGraph().updateViews();
        }
    }
}
