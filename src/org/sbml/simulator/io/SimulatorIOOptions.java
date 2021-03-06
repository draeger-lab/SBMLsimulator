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

import java.io.File;
import java.util.ResourceBundle;

import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * A collection of options and user preferences for reading files, including
 * model files (SBML) and experimental or simulation data.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-10-22
 * @since 1.0
 */
public interface SimulatorIOOptions extends KeyProvider {

  /**
   * 
   */
  public static final ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");

  /**
   * SBML input file.
   */
  public static final Option<File> SBML_INPUT_FILE = new Option<File>(
      "SBML_INPUT_FILE", File.class, bundle, new Range<File>(File.class,
          SBFileFilter.createSBMLFileFilter()), new File(
            System.getProperty("user.dir")));

  /**
   * SBML output file.
   */
  public static final Option<File> SBML_OUTPUT_FILE = new Option<File>(
      "SBML_OUTPUT_FILE", File.class, bundle, new Range<File>(File.class,
          SBFileFilter.createSBMLFileFilter()), new File(
            System.getProperty("user.dir")));

  /**
   * Output of a simulation.
   */
  public static final Option<File> SIMULATION_OUTPUT_FILE = new Option<File>(
      "SIMULATION_OUTPUT_FILE", File.class, bundle, new Range<File>(File.class,
          SBFileFilter.createCSVFileFilter()), new File(
            System.getProperty("user.home")));

  /**
   * Path to a file with a time series of species/compartment/parameter
   * values.
   */
  public static final Option<File> TIME_SERIES_FILE = new Option<File>(
      "TIME_SERIES_FILE", File.class, bundle, new Range<File>(File.class,
          SBFileFilter.createCSVFileFilter()), new File(
            System.getProperty("user.home")));

  /**
   * Select input files for simulation.
   */
  @SuppressWarnings("unchecked")
  public final static OptionGroup<File> INPUT_FILES = new OptionGroup<File>(
      "INPUT_FILES", bundle, SBML_INPUT_FILE, TIME_SERIES_FILE);

  /**
   * Select output files for the results of simulation and parameter estimation.
   */
  @SuppressWarnings("unchecked")
  public final static OptionGroup<File> OUTPUT_FILES = new OptionGroup<File>(
      "OUTPUT_FILES", bundle, SBML_OUTPUT_FILE, SIMULATION_OUTPUT_FILE);

}
