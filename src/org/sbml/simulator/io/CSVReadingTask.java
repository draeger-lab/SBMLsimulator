/*
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.io;

import static de.zbit.util.Utils.getMessage;

import java.io.File;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import javax.swing.SwingWorker;

import org.sbml.jsbml.Model;
import org.sbml.simulator.gui.SimulatorUI;
import org.simulator.math.odes.MultiTable;

import de.zbit.gui.GUITools;
import de.zbit.io.FileTools;
import de.zbit.io.csv.CSVOptions;
import de.zbit.util.prefs.SBPreferences;

/**
 * A background job for reading character-separated values from a file.
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.0
 */
public class CSVReadingTask extends SwingWorker<SortedMap<String, MultiTable>, Void> {

  private SimulatorUI simulator;
  private File[] files;
  private CSVDataImporter importer;
  private File openDir = null;
  private SortedMap<String, MultiTable> resultMap;

  /**
   * 
   * @param ui
   * @param dataFiles
   */
  public CSVReadingTask(SimulatorUI ui, File... dataFiles) {
    super();
    simulator = ui;
    files = dataFiles;
    importer = new CSVDataImporter();
    resultMap = new TreeMap<String, MultiTable>();
  }

  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  protected SortedMap<String, MultiTable> doInBackground() throws Exception {
    Model model = simulator.getModel();
    if (model != null) {
      for (File dataFile : files) {
        try {
          MultiTable data = importer.convert(model, dataFile.getAbsolutePath(), simulator);
          if (data != null) {
            resultMap.put(FileTools.trimExtension(dataFile.getName()), data);
            if (openDir == null) {
              openDir = dataFile.getParentFile();
            }
          }
        } catch (Exception exc) {
          GUITools.showErrorMessage(simulator, exc);
        }
      }
    }
    return resultMap;
  }

  /**
   * A {@link Logger} for this class.
   */
  private static transient final Logger logger = Logger.getLogger(CSVReadingTask.class.getName());

  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#done()
   */
  @Override
  protected void done() {
    if (openDir != null) {
      SBPreferences prefs = SBPreferences.getPreferencesFor(SimulatorUI.class);
      prefs.put(CSVOptions.CSV_FILES_OPEN_DIR, openDir);
      try {
        prefs.flush();
      } catch (BackingStoreException exc) {
        logger.fine(getMessage(exc));
      }
    }
    firePropertyChange("done", null, resultMap);
  }

}
