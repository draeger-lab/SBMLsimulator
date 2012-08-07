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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;

import org.sbml.simulator.gui.graph.DynamicControlPanel.Items;
import org.sbml.simulator.gui.graph.DynamicView.Manipulators;
import org.sbml.simulator.gui.table.LegendTableModel;

import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;

/**
 * Controller class for {@link DynamicControlPanel}. It represents the
 * controller in MVC-pattern and controls any user generated event due to the
 * control panel.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicController implements ChangeListener, ActionListener,
        ItemListener, TableModelListener, PreferenceChangeListener {
    
    /**
     * A {@link Logger} for this class.
     */
    private static final transient Logger logger = Logger.getLogger(DynamicController.class.getName());
    
    /**
     * Localization support.
     */
    private static final transient ResourceBundle bundle = ResourceManager
            .getBundle("org.sbml.simulator.gui.graph.DynamicGraph");
    
    /**
     * Pointer to associated {@link DynamicCore}.
     */
    private DynamicCore core;

    /**
     * Pointer to associated {@link DynamicControlPanel}.
     */
    private DynamicControlPanel controlPanel;

    /**
     * Pointer to asociated {@link DynamicView}.
     */
    private DynamicView view;

    /**
     * Constructs a new {@link DynamicController} with the corresponding
     * {@link DynamicView}.
     * 
     * @param view
     */
    public DynamicController(DynamicView view) {
        this.view = view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (core != null) {
            SBPreferences prefs = SBPreferences
                    .getPreferencesFor(GraphOptions.class);
            if (e.getSource() instanceof JButton) {
                if (e.getActionCommand().equals("PLAY")) {
                    controlPanel.setPlayStatus();
                    core.setPlayspeed(controlPanel.getSimulationSpeed());
                    core.play();
                } else if (e.getActionCommand().equals("PAUSE")) {
                    core.pausePlay();
                    controlPanel.setPauseStatus();
                } else if (e.getActionCommand().equals("STOP")) {
                    core.stopPlay();
                    controlPanel.setStopStatus();
                } else if (e.getActionCommand().equals("TOVIDEO")) {
                    /*
                     * Resolution has to be even and can't be user chosen,
                     * otherwise video image will get stretched. Therefore just
                     * a resolution multiplier to get higher resolutions. Using
                     * Graphsize as base for video resolution ensures that the
                     * black margin in video is at minimum.
                     */
                    ImageGenerator imggen = new ImageGenerator(view.getGraph().getSimpleGraph(), prefs
                            .getBoolean(GraphOptions.VIDEO_DISPLAY_WINDOW));

                    // determine raw graph size
                    int[] size  = imggen.getScreenshotResolution();
                    int width = size[0];
                    int height = size[1];

                    // determine output resolution
                    if (prefs
                            .getBoolean(GraphOptions.VIDEO_FORCE_RESOLUTION_MULTIPLIER) || width < 1000 || height < 1000) {
                        int resolutionMultiplier = (int) prefs
                                .getDouble(GraphOptions.VIDEO_RESOLUTION_MULTIPLIER);
                        /*
                         * if resolution multiplier is forced by the user or if
                         * resolution is too small than scale it
                         */
                        width *= resolutionMultiplier;
                        height *= resolutionMultiplier;
                    }
                    
                    //determine fixpoint to prevent pixel jumping
                    imggen.determineFixPoints(width); 
                    
                    //assign image generator
                    view.setImgGenerator(imggen);

                    // even resolution
                    width = (width % 2) == 1 ? width - 1 : width;
                    height = (height % 2) == 1 ? height - 1 : height;
                    logger.fine("Video out resolution = " + width + "x"
                            + height);
                    
                    int captureStepSize = (int) prefs
                            .getDouble(GraphOptions.VIDEO_IMAGE_STEPSIZE);
                    
                    /*
                     * Determine the time (in miliseconds) which each frame will
                     * be visible in the video by the actual playspeed in the
                     * simulator. Because of the time it takes to draw one
                     * timestep (~3ms or something), the actual time of each
                     * frame has to be longer than the actual playspeed, such
                     * that the output video length and therefore the speed of
                     * the video itself is as close as the experienced speed in
                     * the simulator. Depending on the chosen numbers of images
                     * which will be skipped between each frame, the frametime
                     * need to be multiplied by this.
                     */
                    int timestamp = (controlPanel.getSimulationSpeed() + 3) * captureStepSize;
                    
                    //warning if computation could take very long
                    int numScreenshots = (core.getTimepoints().length-1) / captureStepSize;
                    if (width > 2500 || height > 2500 || numScreenshots > 150) {
                        GUITools.showMessage(
                                MessageFormat.format(
                                        bundle.getString("VIDEO_LONG_COMPUTATION_TIME"),
                                        new Object[] { width, height,
                                                numScreenshots }), bundle
                                        .getString("INFO_COMP"));
                    }
                    
                    /*
                     * Videofilefilter
                     */
                    FileFilter videoFiles = new FileFilter() {
                        
                        @Override
                        public String getDescription() {
                            return bundle.getString("VIDEO_FILEFILTER");
                        }
                        
                        @Override
                        public boolean accept(File f) {
                            String fileName = f.getName().toLowerCase();
                            if (fileName.endsWith(".mov")
                                    || fileName.endsWith(".avi")
                                    || fileName.endsWith(".mp4")
                                    || fileName.endsWith(".mpg")
                                    || fileName.endsWith(".wmv")
                                    || fileName.endsWith(".flv")) {
                                return true;
                            }
                            return false;
                        }
                    };
                    
                    File destinationFile = GUITools.saveFileDialog(view,
                            System.getProperty("user.home"), true, false,
                            JFileChooser.FILES_ONLY, videoFiles);
                    
                    if (destinationFile != null) {
                        controlPanel.setVideoStatus();
                        controlPanel.setStatusString("["
                                + MessageFormat.format(
                                        bundle.getString("GENERATE_VIDEO"),
                                        new Object[] { width, height }) + "]");
                        // ensure dividers don't get moved
                        view.setEnabled(false);
                        view.setEnableLegend(false);

                        // catch errors due to videoencoding
                        try {
                            core.generateVideo(view, width, height, timestamp,
                                    captureStepSize,
                                    destinationFile.getAbsolutePath());
                            
                            // view gets automatically enabled if its
                            // successful.
                        } catch (UnsupportedOperationException uoe) {
                            GUITools.showErrorMessage(
                                    view,
                                    bundle.getString("VIDEO_CODEC_NOT_FOUND")
                                            + "\n >> "
                                            + destinationFile.getName());
                            controlPanel.setStopStatus();
                            view.setEnabled(true);
                            view.setEnableLegend(true);
                            controlPanel.setStatusString(null);
                        } catch (IllegalArgumentException iae) {
                            GUITools.showErrorMessage(
                                    view,
                                    bundle.getString("VIDEO_CODEC_NOT_FOUND")
                                            + "\n >> "
                                            + destinationFile.getName());
                            controlPanel.setStopStatus();
                            view.setEnabled(true);
                            view.setEnableLegend(true);
                            controlPanel.setStatusString(null);
                        }
                    }
                } else if (e.getActionCommand().equals("GRAPHSHOT")) {
                	SBPreferences guiPrefs = SBPreferences
                			.getPreferencesFor(GUIOptions.class);
                	
                    int resolutionMultiplier = (int) prefs
                            .getDouble(GraphOptions.VIDEO_RESOLUTION_MULTIPLIER);
                    
                    
                    ImageGenerator imggen = new ImageGenerator(view.getGraph().getSimpleGraph(), prefs
                            .getBoolean(GraphOptions.VIDEO_DISPLAY_WINDOW));
                    
                    //assign image generator
                    view.setImgGenerator(imggen);
                    
                    // determine raw graph size
                    int[] size  = imggen.getScreenshotResolution();
                    int width = size[0] * resolutionMultiplier;
                    int height = size[1] * resolutionMultiplier;

                    File destinationFile = GUITools.saveFileDialog(view,
                            guiPrefs.get(GUIOptions.SAVE_DIR), false, false,
                            JFileChooser.FILES_ONLY,
                            SBFileFilter.createPNGFileFilter());

                    if (destinationFile != null) {

                        // add extenion if missing
                        if (!destinationFile.getName().toLowerCase()
                                .endsWith(".png")) {
                            destinationFile = new File(
                                    destinationFile.getAbsolutePath() + ".png");
                        }
                        
                        try {
                            if (!destinationFile.getName().toLowerCase()
                                    .endsWith(".png")) {
                                destinationFile = new File(
                                        destinationFile.getAbsolutePath()
                                                + ".png");
                            }
                            ImageIO.write(view.takeGraphshot(width, height),
                                    "png", destinationFile);
                            guiPrefs.put(GUIOptions.SAVE_DIR,
                                    destinationFile.getParent());
                            guiPrefs.flush();
                            logger.info(bundle.getString("SCREENSHOT_DONE"));
                        } catch (IOException ioe) {
                            logger.warning(bundle
                                    .getString("COULD_NOT_WRITE_SCREENSHOT"));
                        } catch (BackingStoreException exc) {
                            logger.warning(exc.getLocalizedMessage());
                        }
                    }
                }
            } else if (e.getSource() instanceof JCheckBox) {
                // labels switched on/off
                view.updateGraph();
            }
        }
    }

    /**
     * Returns user selected {@link IGraphManipulator} with selected options. If
     * there isn't yet a {@link DynamicCore} assigned, return is null.
     * 
     * @return
     */
    public IGraphManipulator getSelectedGraphManipulator() {
        if (core != null) {
            SBPreferences prefs = SBPreferences
                    .getPreferencesFor(GraphOptions.class);
            
            // get current options
            float reactionsMinLineWidth = prefs
                    .getFloat(GraphOptions.MIN_LINE_WIDTH);
            float reactionsMaxLineWidth = prefs
                    .getFloat(GraphOptions.MAX_LINE_WIDTH);
            boolean relativeConcentrations = prefs
                    .getBoolean(GraphOptions.RELATIVE_CONCENTRATION_CHANGES);
            
            logger.finer("#selected species: " + view.getSelectedSpecies().length);
            logger.finer("#selected reactions: " + view.getSelectedReactions().length);
            
            if (controlPanel.getSelectedManipulator().equals(Manipulators.NODESIZE.getName())) {
                
                // get current options
                double minNodeSize = prefs
                        .getDouble(GraphOptions.MIN_NODE_SIZE);
                double maxNodeSize = prefs
                        .getDouble(GraphOptions.MAX_NODE_SIZE);
                
                if (prefs.getBoolean(GraphOptions.USE_UNIFORM_NODE_COLOR)) {
                    return new ManipulatorOfNodeSize(
                            view.getGraph(),
                            view.getSBMLDocument(),
                            core,
                            Option.parseOrCast(Color.class,
                                    prefs.get(GraphOptions.UNIFORM_NODE_COLOR)),
                            view.getSelectedSpecies(), view
                                    .getSelectedReactions(), minNodeSize,
                            maxNodeSize, relativeConcentrations,
                            reactionsMinLineWidth, reactionsMaxLineWidth);
                } else {
                    return new ManipulatorOfNodeSize(view.getGraph(),
                            view.getSBMLDocument(), core, view.getLegendPanel()
                                    .getLegendTableModel(),
                            view.getSelectedSpecies(),
                            view.getSelectedReactions(), minNodeSize,
                            maxNodeSize, relativeConcentrations,
                            reactionsMinLineWidth, reactionsMaxLineWidth);
                }
            } else if (controlPanel.getSelectedManipulator().equals(Manipulators.NODECOLOR.getName())) {
                
                // get current options
                Color color1 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR1));
                Color color2 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR2));
                Color color3 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR3));
                double nodeSize = prefs.getDouble(GraphOptions.COLOR_NODE_SIZE);
                
                return new ManipulatorOfNodeColor(view.getGraph(),
                        view.getSBMLDocument(), core,
                        view.getSelectedSpecies(), view.getSelectedReactions(),
                        relativeConcentrations, nodeSize, color1, color2,
                        color3, reactionsMinLineWidth, reactionsMaxLineWidth);
            } else if (controlPanel.getSelectedManipulator().equals(Manipulators.NODESIZE_AND_COLOR.getName())) {
                
                // get current options
                Color color1 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR1));
                Color color2 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR2));
                Color color3 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR3));
                double minNodeSize = prefs
                        .getDouble(GraphOptions.MIN_NODE_SIZE);
                double maxNodeSize = prefs
                        .getDouble(GraphOptions.MAX_NODE_SIZE);
                
                return new ManipulatorOfNodeSizeAndColor(view.getGraph(),
                        view.getSBMLDocument(), core,
                        view.getSelectedSpecies(), view.getSelectedReactions(),
                        minNodeSize, maxNodeSize, color1, color2, color3,
                        reactionsMinLineWidth, reactionsMaxLineWidth);
            } else if (controlPanel.getSelectedManipulator().equals(Manipulators.NODECOLOR_AND_SIZE.getName())) {
                
                // get current options
                Color color1 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR1));
                Color color2 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR2));
                Color color3 = Option.parseOrCast(Color.class,
                        prefs.get(GraphOptions.COLOR3));
                double minNodeSize = prefs
                        .getDouble(GraphOptions.MIN_NODE_SIZE);
                double maxNodeSize = prefs
                        .getDouble(GraphOptions.MAX_NODE_SIZE);
                
                return new ManipulatorOfNodeColorAndSize(view.getGraph(),
                        view.getSBMLDocument(), core,
                        view.getSelectedSpecies(), view.getSelectedReactions(),
                        minNodeSize, maxNodeSize, color1, color2, color3,
                        reactionsMinLineWidth, reactionsMaxLineWidth);
            }

            // in any other case return nodesize manipulator per default.
            return new ManipulatorOfNodeSize(view.getGraph(),
                    view.getSBMLDocument(), core, view.getSelectedSpecies(),
                    view.getSelectedReactions());
        }
        return null; // do nothing if core isn't set yet.
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    @Override
    public void itemStateChanged(ItemEvent ie) {
        if ((ie.getSource() instanceof JComboBox)
                && (ie.getStateChange() == ItemEvent.SELECTED)) {
            JComboBox cb = (JComboBox) ie.getSource();
            if (cb.getName().equals(GraphOptions.SIM_SPEED_CHOOSER.toString())) {
                controlPanel.setSimVeloCombo(Items.getItem(ie.getItem()
                        .toString()));
                //update preferences on change
                SBPreferences.getPreferencesFor(GraphOptions.class).put(
                        cb.getName(),
                        Items.getItem(ie.getItem().toString()).getName()); 
            }else if (cb.getName().equals(controlPanel.MANIPULATORS_LIST)){
                view.setGraphManipulator(getSelectedGraphManipulator());
                //update preferences on change
                SBPreferences.getPreferencesFor(GraphOptions.class).put(
                        cb.getName(),
                        Manipulators.getManipulator(ie.getItem().toString()).getName());
            }else if (cb.getName().equals(controlPanel.DATA_LIST)){
                view.visualizeData(ie.getItem().toString());
            }
        } else if (ie.getSource() instanceof JCheckBox) {
            JCheckBox cb = (JCheckBox) ie.getSource();
            if (cb.getName() != null) {
                String name = cb.getName();
                if (KeyProvider.Tools.providesOption(GraphOptions.class, name)) {
                    SBPreferences prefs = SBPreferences
                            .getPreferencesFor(GraphOptions.class);
                    logger.fine(name + "=" + cb.isSelected());
                    prefs.put(name, cb.isSelected());
                    try {
                        prefs.flush();
                    } catch (BackingStoreException exc) {
                        logger.fine(exc.getLocalizedMessage());
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.util.prefs.PreferenceChangeListener#preferenceChange(java.util.prefs
     * .PreferenceChangeEvent)
     */
    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        // immediately change graph visualization
        SBPreferences prefs = SBPreferences.getPreferencesFor(GraphOptions.class);
        
        //Options concerning visualization.
        if (evt.getKey().equals("MAX_NODE_SIZE")
                || evt.getKey().equals("MIN_NODE_SIZE")
                || evt.getKey().equals("MIN_LINE_WIDTH")
                || evt.getKey().equals("MAX_LINE_WIDTH")
                || evt.getKey().equals("USE_UNIFORM_NODE_COLOR")
                || evt.getKey().equals("UNIFORM_NODE_COLOR")
                || evt.getKey().equals("RELATIVE_CONCENTRATION_CHANGES")
                || evt.getKey().equals("COLOR1")
                || evt.getKey().equals("COLOR2")
                || evt.getKey().equals("COLOR3")
                || evt.getKey().equals("COLOR_NODE_SIZE")) {
            
            //ensure correct user inputs
            if (prefs.getDouble(GraphOptions.MAX_NODE_SIZE) <= prefs
                    .getDouble(GraphOptions.MIN_NODE_SIZE)) {
                //node sizes wrong
                GUITools.showErrorMessage(view,
                        bundle.getString("EXC_MIN_MAX_NODE_SIZE"));
                prefs.put(GraphOptions.MIN_NODE_SIZE,
                        prefs.getDefault(GraphOptions.MIN_NODE_SIZE));
                prefs.put(GraphOptions.MAX_NODE_SIZE,
                        prefs.getDefault(GraphOptions.MAX_NODE_SIZE));
            }
            
            if (prefs.getDouble(GraphOptions.MAX_LINE_WIDTH) <= prefs
                    .getDouble(GraphOptions.MIN_LINE_WIDTH)) {
                // line widths wrong
                GUITools.showErrorMessage(view,
                        bundle.getString("EXC_MIN_MAX_LINE_WIDTH"));
                prefs.put(GraphOptions.MIN_LINE_WIDTH,
                        prefs.getDefault(GraphOptions.MIN_LINE_WIDTH));
                prefs.put(GraphOptions.MAX_LINE_WIDTH,
                        prefs.getDefault(GraphOptions.MAX_LINE_WIDTH));
            }
            
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
            
            view.setGraphManipulator(getSelectedGraphManipulator());
        }

        if (evt.getKey().equals("SHOW_NODE_LABELS")) {
            controlPanel.setSelectionStateOfNodeLabels(prefs
                    .getBoolean(GraphOptions.SHOW_NODE_LABELS));
            view.updateGraph();
        }

        if (evt.getKey().equals("SHOW_REACTION_LABELS")) {
            controlPanel.setSelectionStateOfReactionLabels(prefs
                    .getBoolean(GraphOptions.SHOW_REACTION_LABELS));
            view.updateGraph();
        }

        if (evt.getKey().equals("SIM_SPEED_FAST")) {
            Items.setSpeed(Items.FAST,
                    (int) prefs.getDouble(GraphOptions.SIM_SPEED_FAST));
            controlPanel.setSimVeloCombo(Items.FAST);
        }

        if (evt.getKey().equals("SIM_SPEED_NORMAL")) {
            Items.setSpeed(Items.NORMAL,
                    (int) prefs.getDouble(GraphOptions.SIM_SPEED_NORMAL));
            controlPanel.setSimVeloCombo(Items.NORMAL);
        }

        if (evt.getKey().equals("SIM_SPEED_SLOW")) {
            Items.setSpeed(Items.SLOW,
                    (int) prefs.getDouble(GraphOptions.SIM_SPEED_SLOW));
            controlPanel.setSimVeloCombo(Items.SLOW);
        }

        if (evt.getKey().equals("SIM_SPEED_CHOOSER")) {
            controlPanel.setSimVeloCombo(Items.getItem(prefs
                    .getString(GraphOptions.SIM_SPEED_CHOOSER)));
        }
        
        if (evt.getKey().equals("VISUALIZATION_STYLE")) {
            controlPanel.setSelectedManipulator(Manipulators
                    .getManipulator(prefs
                            .getString(GraphOptions.VISUALIZATION_STYLE)));
        }
    }

    /**
     * Sets the corresponding {@link DynamicControlPanel}.
     * 
     * @param controlPanel
     */
    public void setControlPanel(DynamicControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    /**
     * Sets the {@link DynamicCore} of this controller.
     * 
     * @param core
     *            {@link DynamicCore}
     */
    public void setCore(DynamicCore core) {
        this.core = core;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
     * )
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        if (core != null) {
            if (e.getSource() instanceof JSlider) {
                int timepoint = ((JSlider) e.getSource()).getValue();
                core.setCurrTimepoint(timepoint);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.
     * TableModelEvent)
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        //update selections
        if (e.getSource() instanceof LegendTableModel) {
            //save selection changes
            if (e.getFirstRow() == e.getLastRow()) {
                //just one element changed
                LegendTableModel ltm = (LegendTableModel) e.getSource();
                view.putSelectionState(ltm.getId(e.getFirstRow()),
                        ltm.isSelected(e.getFirstRow()));
            } else {
                /*
                 * more than one element changed simultaneous (i.e. de-select
                 * all)
                 */
                view.retrieveSelectionStates();
            }
        }
        
        //get new graphmanipulator
        if (core != null) {
            /*
             * Is not uselessly invoked many times while initial startup,
             * because core is set afterwards.
             */
            view.setGraphManipulator(getSelectedGraphManipulator());
            view.updateGraph();
        }
    }
}
