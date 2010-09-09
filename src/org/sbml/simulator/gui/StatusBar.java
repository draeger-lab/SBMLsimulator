/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 */
public class StatusBar extends JPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -719932485499514514L;

	private JLabel label;

	/**
	 * 
	 */
	public StatusBar() {
		super(new BorderLayout());

		setPreferredSize(new Dimension(100, 23));

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setOpaque(false);

		add(rightPanel, BorderLayout.EAST);

		label = new JLabel();
		add(label, BorderLayout.CENTER);

		setMessage("Ready");
		setBorder(BorderFactory.createLoweredBevelBorder());
	}

	/**
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		label.setText(" " + message);
	}
	
	/**
	 * @param text
	 */
	public StatusBar(String text) {
		this();
		setMessage(text);
	}

}
