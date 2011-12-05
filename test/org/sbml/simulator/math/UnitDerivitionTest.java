/*
 * $Id:  UnitDerivitionTest.java 10:56:10 draeger$
 * $URL: UnitDerivitionTest.java $
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

package org.sbml.simulator.math;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.UnitDefinition;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */

public class UnitDerivitionTest {
  
  /**
   * @param args
   * @throws IOException 
   * @throws XMLStreamException 
   * @throws SBMLException 
   */
  public static void main(String[] args) throws XMLStreamException, IOException, SBMLException {
    SBMLDocument doc = SBMLReader.read(new File(args[0]));
    for (Species s : doc.getModel().getListOfSpecies()) {
      UnitDefinition ud = s.getDerivedUnitDefinition();
      System.out.printf("%s\t%s\t%s\n", s, UnitDefinition.printUnits(ud, true), ud);
    }
  }
  
}
