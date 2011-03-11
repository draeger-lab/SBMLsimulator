/*
 * $Id:  ModelHistory.java 17:37:12 draeger$
 * $URL: ModelHistory.java $
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
package org.sbml.simulator.gui;

import de.zbit.gui.prefs.FileHistory;

/**
 * This interface is only needed to distinguish between files that have been
 * loaded by SBMLsimulator from those files that have been used by other
 * programs. With the help of this interface the correct keys will be loaded
 * from the user's configuration, because of the specific package in which
 * this extension of {@link FileHistory} is located.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public interface ModelHistory extends FileHistory {

}
