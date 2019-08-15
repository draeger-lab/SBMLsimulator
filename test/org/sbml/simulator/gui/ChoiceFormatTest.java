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
package org.sbml.simulator.gui;

import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import de.zbit.util.ResourceManager;

/**
 * A test class to try some functions of Java's {@link ChoiceFormat}.
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.0
 */
public class ChoiceFormatTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
    String nameKey = "NAME_PARTS_SPECIES";
    Integer count = Integer.valueOf(2);
    double defaultValue = 0.3d;

    MessageFormat form = new MessageFormat(bundle.getString("REPLACEMENT_OF_UNDEFINED_VALUES"));
    double[] limits = { 1, 2 };
    String[] valuePart = bundle.getString("VALUE_PARTS").split(";");
    String[] namePart = bundle.getString(nameKey).split(";");
    String[] hasBeen = bundle.getString("HAS_BEEN_PARTS").split(";");
    form.setFormatByArgumentIndex(0, new ChoiceFormat(limits, valuePart));
    form.setFormatByArgumentIndex(1, new ChoiceFormat(limits, namePart));
    form.setFormatByArgumentIndex(2, new ChoiceFormat(limits, hasBeen));
    System.out.println(form.format(new Object[] { count, count, count, Double.valueOf(defaultValue) }));

  }

}
