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

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.gui.LegendPanel;
import org.sbml.simulator.gui.table.LegendTableModel;
import org.sbml.simulator.math.SplineCalculation;
import org.simulator.math.odes.MultiTable;

import de.zbit.graph.gui.TranslatorSBMLgraphPanel;
import de.zbit.graph.io.SBML2GraphML;
import de.zbit.gui.GUITools;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.SBPreferences;

/**
 * This class gathers all elements concerning the dynamic visualization and
 * ensures that every part is consistent with each other, i.e. search bar with
 * visualized values. (This is ensured by the implementation of
 * {@link IDynamicGraph}. Every change of the slider goes through this class). It
 * represents the view in MVC-Pattern and therefore listenes for change notifies
 * of associated {@link DynamicCore}.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicView extends JSplitPane implements IDynamicGraph,
        PropertyChangeListener {

    /**
     * Stores all available {@link IGraphManipulator}s.
     * 
     * @author Fabian Schwarzkopf
     * @version $Rev$
     */
    public enum Manipulators {
        NODESIZE, NODECOLOR, NODESIZE_AND_COLOR, NODECOLOR_AND_SIZE;

        /**
         * Returns a string array of all manipulators.
         * 
         * @return
         */
        public static String[] getAllManipulators() {
            return new String[] { NODESIZE.getName(), NODECOLOR.getName(),
                    NODESIZE_AND_COLOR.getName(), NODECOLOR_AND_SIZE.getName() };
        }
        
        /**
         * Returns {@link Manipulators} to given ManipulatorName.
         * @param manipulatorName
         * @return
         */
        public static Manipulators getManipulator(String manipulatorName) {
            if (manipulatorName.equals(bundle.getString("NODESIZE"))) {
                return NODESIZE;
            } else if (manipulatorName.equals(bundle.getString("NODECOLOR"))) {
                return NODECOLOR;
            } else if (manipulatorName.equals(bundle.getString("NODESIZE_AND_COLOR"))) {
                return NODESIZE_AND_COLOR;
            } else if (manipulatorName.equals(bundle.getString("NODECOLOR_AND_SIZE"))) {
                return NODECOLOR_AND_SIZE;
            }
            return null;
        }

        /**
         * Returns localized name of this Item.
         * @return
         */
        public String getName() {
            return bundle.getString(this.toString());
        }
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 4111494340467647183L;

    /**
     * A {@link Logger} for this class.
     */
    private static final transient Logger logger = Logger.getLogger(DynamicView.class.getName());

    /**
     * Localization support.
     */
    private static final transient ResourceBundle bundle = ResourceManager
            .getBundle("org.sbml.simulator.gui.graph.DynamicGraph");

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
     * Pointer to actually visualized {@link DynamicCore}.
     */
    private DynamicCore visualizedCore;
    
    /**
     * Pointer to simulation data {@link DynamicCore}. Once computed, store that
     * core for immediate change of data set.
     */
    private ArrayList<DynamicCore> simulationCores;
    
    /**
     * Pointer to experimental data {@link DynamicCore}. Once computed, store that
     * core for immediate change of data set.
     */
    private ArrayList<DynamicCore> experimentalCores;
    
    /**
     * Pointer to fluxbilance data {@link DynamicCore}. Once computed, store that
     * core for immediate change of data set.
     */
    private ArrayList<DynamicCore> fluxbilanceCores;

    /**
     * Used {@link SBMLDocument}.
     */
    private SBMLDocument document;

    /**
     * Selection states of species IDs according to {@link LegendTableModel}.
     */
    private HashMap<String, Boolean> speciesSelectionStates = new HashMap<String, Boolean>();

    /**
     * Selection states of reactions IDs according to {@link LegendTableModel}.
     */
    private HashMap<String, Boolean> reactionsSelectionStates = new HashMap<String, Boolean>();

    /**
     * This field contains the current {@link IGraphManipulator}.
     */
    private IGraphManipulator graphManipulator;

    /**
     * {@link ImageGenerator} to handle image generating.
     */
    private ImageGenerator imgGenerator;
    
    /**
     * Constructs all necessary elements.
     * 
     * @param document
     */
    public DynamicView(SBMLDocument document) {
        super(JSplitPane.VERTICAL_SPLIT, false);

        // init
        controller = new DynamicController(this);
        graphPanel = new TranslatorSBMLgraphPanel(document, false, false);
        graphPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        legend = new LegendPanel(document.getModel(), true);
        legend.addTableModelListener(controller);
        graphWithLegend = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, legend,
                graphPanel);
        graphWithLegend.setDividerLocation(330);
        controlPanel = new DynamicControlPanel(controller);
        
        /*
         * initial capacity will get bigger, if there are more datasets added
         * than 5.
         */
        simulationCores = new ArrayList<DynamicCore>(5);
        experimentalCores = new ArrayList<DynamicCore>(5);
        
        add(graphWithLegend);
        add(controlPanel);

        setResizeWeight(1.0); // control panel has fixed size

        this.document = document;
    }
    
    /**
     * This methods activates {@link DynamicView} and implicit associated
     * {@link DynamicControlPanel} on the given {@link DynamicCore}.
     * @param core
     */
    private void activateView(DynamicCore core){
        //only activate if it is not activated yet
        if (visualizedCore != core) {
            /*
             * Clear graph view if there have been manipulations before.
             */
            if (graphManipulator != null) {
                for (int i = 0; i < document.getModel().getSpeciesCount(); i++) {
                    graphManipulator.revertChanges(document.getModel()
                            .getSpecies(i).getId());
                }

                for (int i = 0; i < document.getModel().getReactionCount(); i++) {
                    graphManipulator.revertChanges(document.getModel()
                            .getReaction(i).getId());
                }
            }
            
            //init view
            /*
             * Default selections. Selections are saved implicit in hashmaps due
             * to TableModelListener
             */
            legend.getLegendTableModel().setSelected(Species.class, true);
            legend.getLegendTableModel().setSelected(Reaction.class, true);

            visualizedCore = core;

            // activate controlpanel
            controlPanel.setCore(visualizedCore);
            
            // get user chosen graphmanipulator
            graphManipulator = controller.getSelectedGraphManipulator();
            //show core's current timepoint
            visualizedCore.fireTimepointChanged();
            logger.fine("Activated/changed visualized data.");
        }
    }
    
    /**
     * Add Fluxbilance data to display it in the graph.
     * @param data containing fluxbilance for this graph
     */
    public void addFluxbilance(MultiTable data) {
        final DynamicView thisView = this;
        final MultiTable fluxbilance = data;
        SwingWorker<Void, Void> computationOfLimits = new SwingWorker<Void, Void>() {
            
            /*
             * (non-Javadoc)
             * @see javax.swing.SwingWorker#doInBackground()
             */
            @Override
            protected Void doInBackground() throws Exception {
                /*
                 * As a new core is constructed and assigned every time
                 * the simulation is finished, the control panel is
                 * consistent with the simulated data.
                 */
                fluxbilanceCores.add(new DynamicCore(thisView, fluxbilance, document));
                return null;
            }

            /*
             * (non-Javadoc)
             * @see javax.swing.SwingWorker#done()
             */
            @Override
            protected void done() {
                super.done();
                String dataName = bundle
                        .getString("FLUXBILANCE_DATA")
                        + " "
                        + fluxbilanceCores.size();
                controlPanel.addToDataList(dataName);
                controlPanel.setSelectedVisualizationData(dataName);
            }
        };
        computationOfLimits.execute();
    }
    
    /*
     * (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.IDynamicGraph#donePlay()
     */
    @Override
    public void donePlay() {
        controlPanel.setStopStatus();
        setEnabled(true); //ensure that view is enabled
        setEnableLegend(true);
        controlPanel.setStatusString(null); //no displayed status
    }

    /**
     * Returns the registered {@link DynamicController}.
     * 
     * @return {@link DynamicController} or null
     */
    public DynamicController getDynamicController() {
        return controller != null ? controller : null;
    }

    /**
     * Returns {@link SBML2GraphML} of this {@link DynamicView}.
     * 
     * @return
     */
    public SBML2GraphML getGraph() {
        return graphPanel.getConverter();
    }

    /**
     * Returns legend panel of this {@link DynamicView}.
     * 
     * @return
     */
    public LegendPanel getLegendPanel() {
        return legend;
    }

    /**
     * Returns {@link SBMLDocument} of this {@link DynamicView}.
     * 
     * @return
     */
    public SBMLDocument getSBMLDocument() {
        return document;
    }

    /**
     * Returns selected reactions.
     * 
     * @return
     */
    public String[] getSelectedReactions() {
        ArrayList<String> selectedReactions = new ArrayList<String>();
        for (String id : reactionsSelectionStates.keySet()) {
            if (reactionsSelectionStates.get(id)) {
                selectedReactions.add(id);
            }
        }
        return selectedReactions.toArray(new String[selectedReactions.size()]);
    }

    /**
     * Returns selected species.
     * 
     * @return
     */
    public String[] getSelectedSpecies() {
        ArrayList<String> selectedSpecies = new ArrayList<String>();
        for (String id : speciesSelectionStates.keySet()) {
            if (speciesSelectionStates.get(id)) {
                selectedSpecies.add(id);
            }
        }
        return selectedSpecies.toArray(new String[selectedSpecies.size()]);
    }
    
    /*
     * (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
     * PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("done")) {
            //simulation done
            final MultiTable data = (MultiTable) e.getNewValue();
            final DynamicView thisView = this;
            if (data != null) {
                /*
                 * Computation of limits in own swingworker because of O(n^2).
                 * Ensures that UI doesn't get blocked in case of bigger data
                 * sets.
                 */
                SwingWorker<Void, Void> computationOfLimits = new SwingWorker<Void, Void>() {
                    
                    /*
                     * (non-Javadoc)
                     * @see javax.swing.SwingWorker#doInBackground()
                     */
                    @Override
                    protected Void doInBackground() throws Exception {
                        /*
                         * As a new core is constructed and assigned every time
                         * the simulation is finished, the control panel is
                         * consistent with the simulated data.
                         */
                        simulationCores.add(new DynamicCore(thisView, data, document));
                        return null;
                    }

                    /*
                     * (non-Javadoc)
                     * @see javax.swing.SwingWorker#done()
                     */
                    @Override
                    protected void done() {
                        super.done();
                        String dataName = bundle
                                .getString("SIMULATION_DATA")
                                + " "
                                + simulationCores.size();
                        controlPanel.addToDataList(dataName);
                        controlPanel.setSelectedVisualizationData(dataName);
                    }
                };
                computationOfLimits.execute();
            }
        } else if (e.getPropertyName().equals("measurements")) {
            //experimental data added
            MultiTable expData = (MultiTable) e.getNewValue();
            if (expData != null){
                final MultiTable data;
                final DynamicView thisView = this;
                if ((expData.getTimePoints().length < 100)
                        && SBPreferences.getPreferencesFor(GraphOptions.class)
                                .getBoolean(GraphOptions.INTERPOLATE_EXP_DATA)) {
                    /*
                     * Interpolate if there are less than 100 timepoints. CSV
                     * files with less than 2 timepoints can't be read by main
                     * program, thus there is no check for that.
                     */
                    data = SplineCalculation.calculateSplineValues(expData, 100);
                } else {
                    data = expData;
                }
                
                //computation of limits in own swingworker
                SwingWorker<Void, Void> computationOfLimits = new SwingWorker<Void, Void>() {
                    
                    /*
                     * (non-Javadoc)
                     * @see javax.swing.SwingWorker#doInBackground()
                     */
                    @Override
                    protected Void doInBackground() throws Exception {
                        experimentalCores.add(new DynamicCore(thisView, data, document));
                        return null;
                    }
                    
                    /*
                     * (non-Javadoc)
                     * @see javax.swing.SwingWorker#done()
                     */
                    @Override
                    protected void done() {
                        super.done();
                        String dataName = bundle
                                .getString("EXPERIMENTAL_DATA")
                                + " "
                                + experimentalCores.size();
                        controlPanel.addToDataList(dataName);
                        controlPanel.setSelectedVisualizationData(dataName);
                    }
                };
                computationOfLimits.execute();
            }
        } else if (e.getPropertyName().equals("video_progress")) {
            /*
             * progression of video generation.
             * Fire "progress" property such that the progressBar is updated
             */
            this.firePropertyChange("progress", null, e.getNewValue());
        } else if (e.getPropertyName().equals("video_done")) {
            //fire "done" property such that the progressBar is updated
            this.firePropertyChange("done", null, null);
        }
    }

    /**
     * Saves selection state of id, whether specie or reaction, in corresponding
     * hashmap. Method should be invoked on changes in {@link LegendTableModel}.
     * 
     * @param id
     * @param bool
     */
    public void putSelectionState(String id, boolean bool) {
        if (document.getModel().getSpecies(id) != null) {
            speciesSelectionStates.put(id, bool);
        } else if (document.getModel().getReaction(id) != null) {
            reactionsSelectionStates.put(id, bool);
        }
    }
    
    /**
     * updates all selection states of IDs saved in corresponding hashmap
     * (reactions & species)
     */
    public void retrieveSelectionStates(){
        //update species
        for(String id : speciesSelectionStates.keySet()){
            if(legend.getLegendTableModel().isSelected(id)){
                speciesSelectionStates.put(id, true);
            } else {
                speciesSelectionStates.put(id, false);
            }
        }
        //update reactions
        for(String id : reactionsSelectionStates.keySet()){
            if(legend.getLegendTableModel().isSelected(id)){
                reactionsSelectionStates.put(id, true);
            } else {
                reactionsSelectionStates.put(id, false);
            }
        }
    }

    /**
     * Enable/disable legendpanel e.g. in case of video generating
     * @param b
     */
    public void setEnableLegend(boolean b) {
//        legend.setEnabled(b);
        GUITools.setEnabledForAll(b, legend.getComponents());
    }
    
    /**
     * Sets the {@link IGraphManipulator} for this {@link DynamicView}.
     * 
     * @param gm
     */
    public void setGraphManipulator(IGraphManipulator gm) {
        graphManipulator = gm;
        updateGraph();
    }

    /**
     * Sets the current {@link ImageGenerator}.
     * @param imggen
     */
    public void setImgGenerator(ImageGenerator imggen) {
        this.imgGenerator = imggen;
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.IDynamicGraph#takeGraphshot()
     */
    @Override
    public BufferedImage takeGraphshot(int width, int height) {
        return imgGenerator != null ? imgGenerator.takeGraphshot(width, height) : null;
    }

    /**
     * Updates the displayed graph with respect to the user chosen settings.
     * (e.g. turning on/off labels).
     */
    public void updateGraph() {
        if (visualizedCore != null) {
            updateGraph(visualizedCore.getCurrTimepoint(),
                    visualizedCore.getCurrData());
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.sbml.simulator.gui.graph.IDynamicGraph#updateGraph(double,
     * org.simulator.math.odes.MultiTable)
     */
    @Override
    public void updateGraph(double timepoint, MultiTable updateThem) {
        // update JSlider (in case of "play")
        controlPanel.setSearchbarValue(timepoint);

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
                        graphManipulator.dynamicChangeOfReaction(id, updateThem
                                .getValueAt(0, i), controlPanel
                                .getSelectionStateOfReactionLabels());
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
        if (visualizedCore != null) {
            visualizedCore.operationsDone();
        }
    }
    
    /**
     * Changes visualization to given data set name.
     * 
     * @param dataName
     *            last character has to be an integer, representing the dataset
     *            number
     * @return true, if data could be visualized<br>
     *         false, if data could not be visualized.
     */
    public boolean visualizeData(String dataName){
        int index = Integer.valueOf(String.valueOf(dataName.charAt(dataName.length()-1))) - 1;
        if (dataName.contains(bundle.getString("SIMULATION_DATA"))) {
            if (!simulationCores.isEmpty()) {
                activateView(simulationCores.get(index));
                return true;
            }
            return false;
        } else if (dataName.contains(bundle.getString("EXPERIMENTAL_DATA"))) {
            if (!experimentalCores.isEmpty()) {
                activateView(experimentalCores.get(index));
                return true;
            }
            return false;
        } else if (dataName.contains(bundle.getString("FLUXBILANCE_DATA"))) {
            if (!fluxbilanceCores.isEmpty()) {
                activateView(fluxbilanceCores.get(index));
                return true;
            }
            return false;
        }
        
        return false;
    }
}
