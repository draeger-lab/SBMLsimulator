/*
 * $Id: StabilityPanel.java 634 2011-08-12 07:56:18Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/SBMLsqueezer/trunk/src/org/sbml/squeezer/gui/StabilityPanel.java $
 * ---------------------------------------------------------------------
 * This file is part of SBMLsqueezer, a Java program that creates rate 
 * equations for reactions in SBML files (http://sbml.org).
 *
 * Copyright (C) 2006-2016 by the University of Tuebingen, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.stability.gui;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.zbit.gui.layout.LayoutHelper;
import de.zbit.util.StringUtil;

/**
 * @author Alexander D&ouml;rr
 * @date 2009-12-10
 * @since 1.3
 * @version $Rev: 634 $
 */
public class StabilityPanel extends JPanel {
	
	/**
	 * 
	 */
	private JTextField jTextFieldDelta;
	/**
	 * 
	 */
	private JTextField jTextFieldStable;

	/**
	 * Generated serial version id.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private Properties settings;	
	
	/**
	 * 
	 * @param properties
	 */
	public StabilityPanel(Properties properties) {
		super();
		this.settings = properties;
		
		for (Object key : settings.keySet()) {
			String k = key.toString();
			if (k.startsWith("STABILITY_"))
				this.settings.put(key, settings.get(key));
		}

		init();
	}

	/**
	 * 
	 */
	private void init() {		
		GridBagLayout layout = new GridBagLayout();
		jTextFieldDelta = new JTextField();
		// TODO!!!
//		jTextFieldDelta.setText(settings.get(CfgKeys.SqueezerOptions)
//				.toString());
		jTextFieldDelta.setEnabled(false);
		jTextFieldStable = new JTextField("Stability undefined");
		jTextFieldStable.setEnabled(false);
		jTextFieldStable.setBackground(Color.gray);
		
		LayoutHelper.addComponent(this, layout, new JLabel(
				StringUtil.toHTML("Value for numerical differentiation:", 30)),
				0, 0, 1, 1, 1, 0);
		LayoutHelper.addComponent(this, layout,
				jTextFieldDelta, 1, 0, 1, 1, 1, 0);
		LayoutHelper.addComponent(this, layout,
				jTextFieldStable, 0, 1, 2, 1, 2, 2);		
		this.setLayout(layout);
		
	}

}
