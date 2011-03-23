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
package org.sbml.simulator.gui;

import java.awt.Toolkit;

import javax.swing.ImageIcon;

import org.sbml.simulator.resources.Resource;

import eva2.EvAInfo;
import eva2.tools.BasicResourceLoader;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class GUITools extends de.zbit.gui.GUITools {

    /**
     * @return
     */
    public static ImageIcon getIconCamera() {
	return new ImageIcon(Resource.class.getResource("img/camera_16.png"));
    }

    /**
     * @return
     */
    public static ImageIcon getIconEvA2() {
	BasicResourceLoader rl = BasicResourceLoader.instance();
	byte[] bytes = rl.getBytesFromResourceLocation(EvAInfo.iconLocation,
	    true);
	return new ImageIcon(Toolkit.getDefaultToolkit().createImage(bytes));
    }
}
