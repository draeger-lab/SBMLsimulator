/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator;

import java.io.File;
import java.util.ResourceBundle;

import de.zbit.io.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-10-22
 * @version $Rev$
 * @since 1.0
 */
public interface SimulatorOptions extends KeyProvider {
    
    /**
     * 
     */
    public static final ResourceBundle bundle = ResourceManager
	    .getBundle("org.sbml.simulator.locales.Simulator");

    /**
     * SBML input file.
     */
    public static final Option<File> SBML_FILE = new Option<File>("SBML_FILE",
	File.class, "SBML input file", new Range<File>(File.class, SBFileFilter
		.createSBMLFileFilter()), new File(System
		.getProperty("user.dir")));
    /**
     * Path to a file with a time series of species/compartment/parameter
     * values.
     */
    public static final Option<File> TIME_SERIES_FILE = new Option<File>(
	"TIME_SERIES_FILE",
	File.class,
	"Path to a file with a time series of species/compartment/parameter values.",
	new Range<File>(File.class, SBFileFilter.createCSVFileFilter()),
	new File(System.getProperty("user.home")));
    
}
