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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.gui.LegendPanel;
import org.simulator.math.odes.MultiTable;

import de.zbit.graph.gui.TranslatorSBMLgraphPanel;

/**
 * This class brings all elements concerning the dynamic visualization together
 * and makes sure that every part is consistent with each other, i.e. search bar
 * with visualized values. (This is ensured by the implementation of
 * {@link DynamicGraph}. Every change of the slider wents through this class).
 * It represents the view in MVC-Pattern and therefore awaits change notifies of
 * associated {@link DynamicCore}.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicView extends JSplitPane implements DynamicGraph,
        PropertyChangeListener{
    private static final long serialVersionUID = 4111494340467647183L;

    /**
     * Panel for the graph representation.
     */
    private TranslatorSBMLgraphPanel graphPanel;
    
    /**
     * Legend. TODO without colors column
     */
    private LegendPanel legend;
    
    /**
     * Field for the splitpane wrapping graph and legend.
     */
    private JSplitPane graphWithLegend;

    /**
     * Panel for the controls of the dynamic visualization.
     */
    private DynamicControlPanel controlPanel;

    /**
     * Pointer to the used {@link DynamicCore}.
     */
    private DynamicCore core;
    
    /**
     * Used SBMLDocument.
     */
    private SBMLDocument document;

    /**
     * list of species (to save computing time). TODO: "find" faster in hashmap?
     */
    private ListOf<Species> speciesIDs;

    /**
     * list of reactions (to save computing time).
     */
     private ListOf<Reaction> reactionIDs;

    /**
     * Parameter to adjust values used in dynamic update of the graph. Slope by
     * linear regression to ensure that optimal values will be passed.
     * y = m * x + c
     * TODO save in own object together with all other values concerning the mapping
     */
    private double m;

    /**
     * Parameter to adjust values used in dynamic update of the graph.
     * y-intercept by linear regression to ensure that optimal values will be
     * passed.
     * y = m * x + c
     * TODO save in own object together with all other values concerning the mapping
     */
    private double c;

    /**
     * Constructs all necessary elements.
     * 
     * @param document
     */
    public DynamicView(SBMLDocument document){
        super(JSplitPane.VERTICAL_SPLIT, false);
        graphPanel = new TranslatorSBMLgraphPanel(document, false);
        legend = new LegendPanel(document.getModel(), true);
        graphWithLegend = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, legend, graphPanel);
        graphWithLegend.setDividerLocation(330);
        controlPanel = new DynamicControlPanel();

        add(graphWithLegend);
        add(controlPanel);

        setResizeWeight(1.0); //control panel has fixed size
        
        this.document = document;
        speciesIDs = document.getModel().getListOfSpecies();
        reactionIDs = document.getModel().getListOfReactions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sbml.simulator.gui.graph.DynamicGraph#updateGraph(double,
     * org.simulator.math.odes.MultiTable)
     */
    @Override
    public void updateGraph(double timepoint, MultiTable updateThem){
        controlPanel.setTimepoint(timepoint);
        /*
         * To ensure that the dynamic change of graph rendering will make a
         * noticible change in size or color the passed values have to be
         * conditioned. Therefore not absolute values will be passed (TODO).
         */
        /**
         * incompleted test with TestModel.xml
         */
        for (int i = 1; i <= updateThem.getColumnCount(); i++){
            String id = updateThem.getColumnIdentifier(i);
            if(legend.getLegendTableModel().isSelected(id)){
                //only display dynamic features when selected
                if (speciesIDs.get(id) != null){
                    // System.out.println(updateThem.getColumnIdentifier(i));
                    /*
                     * There's just one row because the core passes only the
                     * necessary data for the particular timepoint
                     */
                    double tmpValue = updateThem.getValueAt(0, i);
                    graphPanel.dynamicChangeOfNode(id, tmpValue * m + c,
                            tmpValue, controlPanel.getSelectionStateOfLabels());

                }else if (reactionIDs.get(id) != null){
                    // TODO
                    graphPanel.dynamicChangeOfReaction(id, 10);
                }

            }else{
                graphPanel.notSelected(id);
            }
        }

        /*
         * Notifiy that graph update is finished. Ensures that play-thread in
         * core doesn't overtravel the drawing.
         */
        core.graphUpdateFinished();
    }

    /*
     * (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
     * PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent e){
        if (e.getPropertyName().equals("done")){
            // simulation done
            MultiTable data = (MultiTable) e.getNewValue();
            if (data != null){
                /*
                 * As a new core is constructed every time the simulation is
                 * finished, the control panel is consistent with the simulated
                 * data.
                 */
                core = new DynamicCore(this, data);
                
                /*
                 * Computation of limits in own swingworker because of O(n^2).
                 * Ensures that UI doesn't get blocked in case of bigger data sets.
                 */
                SwingWorker<Void, Void> computationOfLimits = new SwingWorker<Void, Void>(){

                    @Override
                    protected Void doInBackground() throws Exception{
                        core.computeLimits(document);
                        return null;
                    }

                    /* (non-Javadoc)
                     * @see javax.swing.SwingWorker#done()
                     */
                    @Override
                    protected void done(){
                        super.done();
                        //activate controlpanel after computation of limits
                        //TODO mapping
                        m = BIASComputation.computeBIAS(core.getMinDataSpecies(), core.getMaxDataSpecies());
                        c = BIASComputation.getYintercept();
                        controlPanel.setCore(core);
                    }
                };
                computationOfLimits.execute();
            }
        }
    }

}
