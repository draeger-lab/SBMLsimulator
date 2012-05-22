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
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.gui.LegendPanel;
import org.simulator.math.odes.MultiTable;

import de.zbit.graph.gui.TranslatorSBMLgraphPanel;
import de.zbit.graph.io.SBML2GraphML;

/**
 * This class gathers all elements concerning the dynamic visualization 
 * and ensures that every part is consistent with each other, i.e. search bar
 * with visualized values. (This is ensured by the implementation of
 * {@link DynamicGraph}. Every change of the slider goes through this class).
 * It represents the view in MVC-Pattern and therefore listenes for
 * change notifies of associated {@link DynamicCore}.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicView extends JSplitPane implements DynamicGraph,
        PropertyChangeListener{
    
    /**
     * 
     */
    private static final long serialVersionUID = 4111494340467647183L;

    /**
     * Panel for the graph representation.
     */
    private TranslatorSBMLgraphPanel graphPanel;
    
    /**
     * Legend.
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
     * Pointer to associated {@link DynamicController}.
     */
    private DynamicController controller;

    /**
     * Pointer to the used {@link DynamicCore}.
     */
    private DynamicCore core;
    
    /**
     * Used {@link SBMLDocument}.
     */
    private SBMLDocument document;
     
     /**
      * Saves the currently displayed time.
      */
     private double currTime;
     
     /**
      * Saves the currently displayed data.
      */
     private MultiTable currData;
     
     /**
      * This field contains the current {@link GraphManipulator}.
      */
     private GraphManipulator graphManipulator;

    /**
     * Constructs all necessary elements.
     * 
     * @param document
     */
    public DynamicView(SBMLDocument document) {
        super(JSplitPane.VERTICAL_SPLIT, false);
        
        //init
        controller = new DynamicController(this);
        graphPanel = new TranslatorSBMLgraphPanel(document, false, false);
        graphPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        legend = new LegendPanel(document.getModel(), true);
        legend.addTableModelListener(controller);
        graphWithLegend = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, legend, graphPanel);
        graphWithLegend.setDividerLocation(330);
        controlPanel = new DynamicControlPanel(this, controller);

        add(graphWithLegend);
        add(controlPanel);

        setResizeWeight(1.0); //control panel has fixed size
        
        this.document = document;
    }
    
    /**
     * Sets the {@link GraphManipulator} for this {@link DynamicView}.
     * @param gm
     */
    public void setGraphManipulator(GraphManipulator gm){
        graphManipulator = gm;
        updateGraph();
    }
    
    /**
     * Returns the registered {@link DynamicController}.
     * @return {@link DynamicController} or null
     */
    public DynamicController getDynamicController(){
        return controller != null ? controller : null; 
    }
    
    /**
     * Updates the displayed graph with respect to the user chosen settings
     * (i.e. turning on/off labels).
     */
    public void updateGraph() {
        if(core != null){
            updateGraph(currTime, currData);
        }
    }
    
    /**
     * Workaround to get selected species.
     * There's no way of getting selected items?!
     * TODO user objects and hashmaps
     * @return
     */
    public String[] getSelectedSpecies(){
        ArrayList<String> selectedSpecies = new ArrayList<String>();
        for(Species s : document.getModel().getListOfSpecies()){
            if(legend.getLegendTableModel().isSelected(s.getId())){
                selectedSpecies.add(s.getId());
            }
        }
        return selectedSpecies.toArray(new String[selectedSpecies.size()]);
    }
    
    /**
     * Workaround to get selected reactions.
     * There's no way of getting selected items?!
     * TODO user objects and hashmaps
     * @return
     */
    public String[] getSelectedReactions(){
        ArrayList<String> selectedReactions = new ArrayList<String>();
        for(Reaction r : document.getModel().getListOfReactions()){
            if(legend.getLegendTableModel().isSelected(r.getId())){
                selectedReactions.add(r.getId());
            }
        }
        return selectedReactions.toArray(new String[selectedReactions.size()]);
    }
    
    /**
     * Returns {@link SBML2GraphML} of this {@link DynamicView}.
     * @return
     */
    public SBML2GraphML getGraph(){
        return graphPanel.getConverter();
    }
    
    /**
     * Returns {@link SBMLDocument} of this {@link DynamicView}.
     * @return
     */
    public SBMLDocument getSBMLDocument(){
        return document;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sbml.simulator.gui.graph.DynamicGraph#updateGraph(double,
     * org.simulator.math.odes.MultiTable)
     */
    @Override
    public void updateGraph(double timepoint, MultiTable updateThem) {
        //save currently displayed properties
        currTime = timepoint;
        currData = updateThem;
        //update JSlider (in case of "play")
        controlPanel.setTimepoint(timepoint);
        
        if (graphManipulator != null) {
            for (int i = 1; i <= updateThem.getColumnCount(); i++) {
                String id = updateThem.getColumnIdentifier(i);
                if (legend.getLegendTableModel().isSelected(id)) {
                    // only display dynamic features when selected
                    if (document.getModel().getSpecies(id) != null) {
                        /*
                         * There's just one row because the core passes only the
                         * necessary data for the particular timepoint
                         */
                        graphManipulator.dynamicChangeOfNode(id,
                                updateThem.getValueAt(0, i),
                                controlPanel.getSelectionStateOfNodeLabels());
                    } else if (document.getModel().getReaction(id) != null) {
                        if (timepoint == 0.0){
                            //there's no initial reaction data.
                            graphManipulator.revertChanges(id);
                        } else {
                            graphManipulator.dynamicChangeOfReaction(id,
                                    updateThem.getValueAt(0, i),
                                    controlPanel.getSelectionStateOfReactionLabels());
                        }
                    }
                } else {
                    graphManipulator.revertChanges(id);
                }
            }
        }

        /*
         * Notifiy that graph update is finished. Ensures that play-thread in
         * core doesn't overtravel drawing.
         */
        if(core != null) {
            core.graphUpdateFinished();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
     * PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("done")) {
            // simulation done
            MultiTable data = (MultiTable) e.getNewValue();
            if (data != null) {
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
                SwingWorker<Void, Void> computationOfLimits = new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() throws Exception{
                        core.computeSpecificLimits(document);
                        return null;
                    }

                    /* (non-Javadoc)
                     * @see javax.swing.SwingWorker#done()
                     */
                    @Override
                    protected void done() {
                        super.done();
                        legend.getLegendTableModel().setSelected(Species.class, true);
                        legend.getLegendTableModel().setSelected(Reaction.class, true);
                        //activate controlpanel after computation of limits
                        controlPanel.setCore(core);
                        graphManipulator = controller.getSelectedGraphManipulator();
                        updateGraph();
                    }
                };
                computationOfLimits.execute();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.DynamicGraph#donePlay()
     */
    @Override
    public void donePlay() {
        controlPanel.setStopStatus();
        double[] allTimepoints = core.getTimepoints();
        //only if last timepoint reached, switch to first timepoint.
        if(currTime == allTimepoints[allTimepoints.length-1]){
            core.setCurrTimepoint(0);
        }
    }
}
