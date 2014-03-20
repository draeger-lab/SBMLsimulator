/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui.plot;

import org.jfree.data.xy.XYDataset;

/**
 * With the help of this interface an abstraction layer between the
 * {@link XYDataset} from the underlying plot library and the
 * {@link org.simulator.math.odes.MultiTable} from the simulation core library
 * is created.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public interface MetaDataset extends XYDataset {

  /**
   * Returns the identifier of the given series.
   * 
   * @param series
   * @return
   */
  public String getSeriesIdentifier(int series);

  /**
   * 
   * @param identifier
   * @return
   */
  public int getSeriesIndex(String identifier);

}
