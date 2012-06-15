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

import java.awt.Point;
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

import y.io.JPGIOHandler;
import y.io.ViewPortConfigurator;
import y.view.Graph2D;
import y.view.Graph2DView;
import de.zbit.graph.gui.TranslatorSBMLgraphPanel;
import de.zbit.graph.io.SBML2GraphML;
import de.zbit.util.ResourceManager;

/**
 * This class gathers all elements concerning the dynamic visualization and
 * ensures that every part is consistent with each other, i.e. search bar with
 * visualized values. (This is ensured by the implementation of
 * {@link DynamicGraph}. Every change of the slider goes through this class). It
 * represents the view in MVC-Pattern and therefore listenes for change notifies
 * of associated {@link DynamicCore}.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicView extends JSplitPane implements DynamicGraph,
        PropertyChangeListener {

    /**
     * Stores {@link GraphManipulator}s.
     * 
     * @author Fabian Schwarzkopf
     * @version $Rev$
     */
    public enum Manipulators {
        NODESIZE, NODECOLOR;

        /**
         * Returns a string array of all manipulators.
         * 
         * @return
         */
        public static String[] getAllManipulators() {
            return new String[] { NODESIZE.getName(), NODECOLOR.getName() };
        }
        
        /**
         * Returns {@link Manipulators} to given ManipulatorName.
         * @param manipulatorName
         * @return
         */
        public static Manipulators getManipulator(String manipulatorName){
            if(manipulatorName.equals(bundle.getString("NODESIZE"))){
                return NODESIZE;
            }else if(manipulatorName.equals(bundle.getString("NODECOLOR"))){
                return NODECOLOR;
            }
            return null;
        }

        /**
         * Returns localized name of this Item.
         * 
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
     * Internal variable (fixpoint for video).
     */
    private Point viewPoint;
    
    /**
     * Internal variable (fixpoint for video).
     */
    private double zoomLevel;
    
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
        controlPanel = new DynamicControlPanel(this, controller);
        
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
            currData = null;
            currTime = 0;

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

    /**
     * This function determines the fixpoints to generate videos. It ensures
     * that graph elements will stay on their location even if the graphsize
     * changes during visualization (i. e. nodes getting bigger/smaller).
     * 
     * @param width
     *            of the video stream
     */
    public void determineFixPoints(int width){
        Graph2D graph = graphPanel.getConverter().getSimpleGraph();

        //create dedicated view
        JPGIOHandler ioh = new JPGIOHandler();
        Graph2DView imageView = ioh.createDefaultGraph2DView(graph);
        
        //configure imageView such that whole graph is contained
        ViewPortConfigurator vpc = new ViewPortConfigurator();
        vpc.setGraph2D(imageView.getGraph2D());
        vpc.setClipType(ViewPortConfigurator.CLIP_GRAPH);
        vpc.setSizeType(ViewPortConfigurator.SIZE_USE_CUSTOM_WIDTH);
        vpc.setCustomWidth(width);
        vpc.configure(imageView);
        
        //save initial viewpoint
        viewPoint = imageView.getViewPoint();
        //save initial zoomlevel
        zoomLevel = imageView.getZoom();
        
        logger.fine("Fixpoints set. Viewpoint: " + viewPoint + "; Zoomlevel: "
                + zoomLevel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sbml.simulator.gui.graph.DynamicGraph#donePlay()
     */
    @Override
    public void donePlay() {
        controlPanel.setStopStatus();
        setEnabled(true); //ensure that view is enabled
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
     * Returns an array {width, height} of the graph size independent of current
     * view. Returned width and height represent the raw resolution of this graph. 
     * @return {width, height}
     */
    public int[] getScreenshotResolution(){
        Graph2D graph = graphPanel.getConverter().getSimpleGraph();

        //create dedicated view
        JPGIOHandler ioh = new JPGIOHandler();
        Graph2DView imageView = ioh.createDefaultGraph2DView(graph);
        
        //configure imageView such that whole graph is contained
        ViewPortConfigurator vpc = new ViewPortConfigurator();
        vpc.setGraph2D(imageView.getGraph2D());
        vpc.setClipType(ViewPortConfigurator.CLIP_GRAPH);
        vpc.setSizeType(ViewPortConfigurator.SIZE_USE_ORIGINAL);
        vpc.configure(imageView);
        
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        
        return new int[]{width, height};
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
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
     * PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("done")) {
            final MultiTable data = (MultiTable) e.getNewValue();
            final DynamicView thisView = this;
            // simulation done
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
        } else if (e.getPropertyName().equals("measurements")){
            //experimental data added
            MultiTable expData = (MultiTable) e.getNewValue();
            if (expData != null){
                final MultiTable data;
                final DynamicView thisView = this;
                if (expData.getTimePoints().length < 100) {
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
     * Sets the {@link GraphManipulator} for this {@link DynamicView}.
     * 
     * @param gm
     */
    public void setGraphManipulator(GraphManipulator gm) {
        graphManipulator = gm;
        updateGraph();
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.DynamicGraph#takeGraphshot()
     */
    @Override
    public BufferedImage takeGraphshot(int width, int height) {
        /*
         * Take screenshot in dedicated view to ensure that nothing is cut off
         * and to change screenshotresolution independent of actual view (this
         * allows higher screenshot resolutions without users noticing the
         * necessary scaling).
         */
        Graph2D graph = graphPanel.getConverter().getSimpleGraph();
        
        //save current view
        Graph2DView originalViewPort = (Graph2DView) graph.getCurrentView();
        
        //create dedicated view for graphshot
        JPGIOHandler ioh = new JPGIOHandler();
        Graph2DView imageView = ioh.createDefaultGraph2DView(graph);
        
        //use original render settings
        imageView.setGraph2DRenderer(originalViewPort.getGraph2DRenderer());
        imageView.setRenderingHints(originalViewPort.getRenderingHints());
        
        //set dedicated view
        graph.setCurrentView(imageView);
        
        //settings for dedicated view
        ViewPortConfigurator vpc = new ViewPortConfigurator();
        vpc.setGraph2D(imageView.getGraph2D());
        //do not cut off anything not in view
        vpc.setClipType(ViewPortConfigurator.CLIP_GRAPH);
        //scale image to video size
        vpc.setSizeType(ViewPortConfigurator.SIZE_USE_CUSTOM_WIDTH);
        vpc.setCustomWidth(width);
        //configure dedicated view with settings
        vpc.configure(imageView);
        
        /*
         * fix zoomlevel and viewpoint to prevent pixel jumping during video
         * generating. Make sure to call determineFixePoints(int width) before calling
         * this function.
         */
        if (zoomLevel != 0 && viewPoint != null) {
            imageView.setZoom(zoomLevel);
            imageView.setViewPoint(viewPoint.x, viewPoint.y);
        } else {
            logger.fine("No Fixpoints available. There might be some pixel jumping in videos.");
        }
        
        //take screenshot
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        imageView.paintVisibleContent(image.createGraphics());
        
        //restore original view
        graph.removeView(graph.getCurrentView());
        graph.setCurrentView(originalViewPort);
        
        return image;
    }

    /**
     * Updates the displayed graph with respect to the user chosen settings.
     * (i.e. turning on/off labels).
     */
    public void updateGraph() {
        if ((visualizedCore != null) && (currData != null)) {
            updateGraph(currTime, currData);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.sbml.simulator.gui.graph.DynamicGraph#updateGraph(double,
     * org.simulator.math.odes.MultiTable)
     */
    @Override
    public void updateGraph(double timepoint, MultiTable updateThem) {
        // save currently displayed properties
        currTime = timepoint;
        currData = updateThem;
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
                        if (timepoint == 0.0) {
                            // there's no initial reaction data.
                            graphManipulator.revertChanges(id);
                        } else {
                            graphManipulator
                                    .dynamicChangeOfReaction(
                                            id,
                                            updateThem.getValueAt(0, i),
                                            controlPanel
                                                    .getSelectionStateOfReactionLabels());
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
