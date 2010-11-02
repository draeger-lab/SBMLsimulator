/*
 *  SBMLsqueezer creates rate equations for reactions in SBML files
 *  (http://sbml.org).
 *  Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Properties;
import java.util.prefs.BackingStoreException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.jsbml.xml.stax.SBMLWriter;
import org.sbml.optimization.EvA2GUIStarter;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulatorCfgKeys;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.resources.Resource;

import de.zbit.gui.cfg.PreferencesDialog;
import de.zbit.io.SBFileFilter;
import de.zbit.util.SBProperties;
import eva2.client.EvAClient;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-15
 * 
 */
public class SimulatorUI extends JFrame implements ActionListener,
		ItemListener, WindowListener {

	/**
	 * Commands that can be understood by this dialog.
	 * 
	 * @author Andreas Dr&auml;ger
	 * 
	 */
	public static enum Command {
		/**
		 * To exit this program.
		 */
		EXIT,
		/**
		 * Show about message, i.e., information about the authors of this
		 * program.
		 */
		HELP_ABOUT,
		/**
		 * Displays the software license.
		 */
		HELP_LICENSE,
		/**
		 * Starts the help web page.
		 */
		HELP_ONLINE,
		/**
		 * Open file with experimental data or the model file.
		 */
		OPEN,
		/**
		 * Close experimental data or SBML file.
		 */
		CLOSE_DATA,
		/**
		 * Starts the optimization of the model with respect to given data.
		 */
		OPTIMIZATION,
		/**
		 * Save the plot as an image.
		 */
		SAVE_PLOT_IMAGE,
		/**
		 * Save the results of the simulation to a CSV file.
		 */
		SAVE_SIMULATION,
		/**
		 * Save the optimized model in an SBML file
		 */
		SAVE_MODEL,
		/**
		 * Adjust user's preferences
		 */
		SETTINGS,
		/**
		 * Start a new simulation with the current settings.
		 */
		SIMULATION_START,
		/**
		 * Whether or not to display a tool bar below the plot.
		 */
		SHOW_OPTIONS
	}

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = -5289766427756813972L;

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception exc) {
			exc.printStackTrace();
			GUITools.showErrorMessage(null, exc);
		}
	}

	/**
	 * GUI element that lets the user run the simulation.
	 */
	private SimulationPanel simPanel;

	/**
	 * The tool bar of this element.
	 */
	private JToolBar toolbar;

	/**
	 * 
	 */
	public static final String defaultTitle = "SBML simulator "
			+ SBMLsimulator.getVersionNumber();

	/**
	 * 
	 */
	public SimulatorUI() {
		super(defaultTitle);

		setProperties(SBMLsimulator.getProperties());

		/*
		 * Add menubar and toolbar
		 */
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setJMenuBar(createJMenuBar());
		toolbar = createToolBar();

		/*
		 * Init layout
		 */
		getContentPane().add(toolbar, BorderLayout.NORTH);
		// getContentPane().add(new StatusBar(), BorderLayout.SOUTH);
		setOptimalSize();
		setLocationRelativeTo(null);
	}

	/**
	 * 
	 * @param owner
	 * @param model
	 */
	public SimulatorUI(Model model) {
		this(model, new SimulationPanel(model));
	}

	/**
	 * 
	 * @param owner
	 * @param model
	 * @param settings
	 */
	public SimulatorUI(Model model, SBProperties settings) {
		this(model, new SimulationPanel(model, settings));
	}

	/**
	 * 
	 * @param owner
	 * @param model
	 * @param simulationPanel
	 */
	public SimulatorUI(Model model, SimulationPanel simulationPanel) {
		this();
		simPanel = simulationPanel;
		getContentPane().add(simPanel, BorderLayout.CENTER);
		setOptimalSize();
		setLocationRelativeTo(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == null) {
			return;
		}
		switch (Command.valueOf(e.getActionCommand())) {
		case SIMULATION_START:
			simulate();
			break;
		case OPEN:
			if (simPanel == null) {
				openModel();
			} else {
				openExperimentalData();
			}
			break;
		case CLOSE_DATA:
			close();
			break;
		case SAVE_PLOT_IMAGE:
			simPanel.savePlotImage();
			break;
		case SAVE_SIMULATION:
			simPanel.saveSimulationResults();
			break;
		case SAVE_MODEL:
			saveModel(simPanel.getModel());
			break;
		case SETTINGS:
			adjustPreferences();
			break;
		case EXIT:
			try {
				if (simPanel != null) {
					SBMLsimulator.getProperties().putAll(
							simPanel.getProperties());
				}
				SBMLsimulator.saveProperties();
			} catch (BackingStoreException exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
			System.exit(0);
		case HELP_ONLINE:
			GUITools.showMessage(Resource.class
					.getResource("html/online-help.html"), "SBMLsimulator "
					+ SBMLsimulator.getVersionNumber() + " online help", this,
					GUITools.getIconHelp48());
			break;
		case HELP_ABOUT:
			GUITools.showMessage(Resource.class.getResource("html/about.html"),
					"About SBMLsimulator " + SBMLsimulator.getVersionNumber(),
					this);
			break;
		case HELP_LICENSE:
			GUITools.showMessage(Resource.class
					.getResource("html/License.html"),
					"License of SBMLsimulator "
							+ SBMLsimulator.getVersionNumber(), this, GUITools
							.getIconLicense48());
			break;
		case OPTIMIZATION:
			new Thread(new Runnable() {
				public void run() {
					optimize();
				}
			}).start();
			break;
		case SHOW_OPTIONS:
			// treated in {@link #itemStateChanged(ItemEvent)
			break;
		default:
			JOptionPane.showMessageDialog(this, "Invalid option "
					+ e.getActionCommand(), "Warning",
					JOptionPane.WARNING_MESSAGE);
			break;
		}
	}

	/**
	 * 
	 * @param model
	 */
	public void saveModel(Model model) {
		Properties props = SBMLsimulator.getProperties();
		File f = GUITools.saveFileDialog(this, props.get(
				SimulatorCfgKeys.SAVE_DIR).toString(), false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.SBML_FILE_FILTER);
		if (f != null) {
			try {
				SBMLWriter.write(model.getSBMLDocument(), f,
						SBMLsimulator.class.getSimpleName(), SBMLsimulator
								.getVersionNumber());
				// TODO Just for debugging:
				SBMLWriter.write(model.getSBMLDocument(), System.out);
				props.put(SimulatorCfgKeys.SAVE_DIR, f.getParent());
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/**
	 * 
	 */
	private void adjustPreferences() {
		try {
			PreferencesDialog dialog = new PreferencesDialog(this);
			if (dialog.showSettingsDialog(SBMLsimulator.getProperties()) == PreferencesDialog.APPROVE_OPTION) {
				SBProperties p = dialog.getProperties();
				SBMLsimulator.getProperties().putAll(p);
				if (simPanel != null) {
					simPanel.setProperties(p);
				}
				SBMLsimulator.saveProperties();
			}
		} catch (Exception exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * 
	 */
	public void close() {
		if (simPanel != null) {
			if (simPanel.isSetExperimentalData()) {
				simPanel.closeExperimentalData();
				GUITools.setEnabled(false, getJMenuBar(), toolbar,
						Command.OPTIMIZATION, Command.SAVE_PLOT_IMAGE,
						Command.SAVE_SIMULATION, Command.SIMULATION_START);
			} else {
				getContentPane().remove(simPanel);
				simPanel = null;
				GUITools.setEnabled(false, getJMenuBar(), toolbar,
						Command.CLOSE_DATA, Command.SAVE_PLOT_IMAGE,
						Command.SIMULATION_START);
				setOptimalSize();
				setTitle(defaultTitle);
			}
			GUITools.setEnabled(true, getJMenuBar(), toolbar, Command.OPEN);
		}
		if (simPanel == null) {
			GUITools.setEnabled(false, getJMenuBar(), toolbar,
					Command.SHOW_OPTIONS, Command.SAVE_MODEL);
		}
	}

	/**
	 * 
	 * @return
	 */
	private JMenuBar createJMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		/*
		 * File
		 */
		JMenuItem openItem = GUITools.createJMenuItem("Open", this,
				Command.OPEN, GUITools.getIconFolder(), KeyStroke.getKeyStroke(
						'O', InputEvent.CTRL_DOWN_MASK));
		JMenuItem closeItem = GUITools.createJMenuItem("Close", this,
				Command.CLOSE_DATA, GUITools.getIconTrash(), KeyStroke
						.getKeyStroke('W', KeyEvent.CTRL_DOWN_MASK));
		JMenuItem saveModel = GUITools.createJMenuItem("Save model", this,
				Command.SAVE_MODEL, GUITools.getIconSave(), KeyStroke
						.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK), 'S',
				false);
		JMenuItem saveSimItem = GUITools.createJMenuItem("Save simulation",
				this, Command.SAVE_SIMULATION, GUITools.getIconSave(),
				KeyStroke.getKeyStroke('E', InputEvent.CTRL_DOWN_MASK));
		JMenuItem savePlotItem = GUITools.createJMenuItem("Save plot", this,
				Command.SAVE_PLOT_IMAGE, GUITools.getIconCamera(), KeyStroke
						.getKeyStroke('S', KeyEvent.ALT_DOWN_MASK));
		JMenuItem exitItem = GUITools.createJMenuItem("Exit", this,
				Command.EXIT, KeyStroke.getKeyStroke(KeyEvent.VK_F4,
						InputEvent.ALT_DOWN_MASK));

		JMenu fileMenu = GUITools.createJMenu("File", openItem, closeItem,
				saveModel, saveSimItem, savePlotItem, exitItem);

		/*
		 * Edit
		 */
		JMenuItem simulation = GUITools.createJMenuItem("Start simulation",
				this, Command.SIMULATION_START, GUITools.getIconGear(), 'S');
		JMenuItem optimization = GUITools.createJMenuItem("Optimization", this,
				Command.OPTIMIZATION, GUITools.getIconEvA2(), 'O');
		optimization.setEnabled(false);
		JMenu editMenu = GUITools.createJMenu("Edit", simulation, optimization);
		editMenu.addSeparator();

		JCheckBoxMenuItem item = new JCheckBoxMenuItem("Show options",
				simPanel != null ? simPanel.isShowSettingsPanel() : true);
		if (simPanel == null) {
			item.setEnabled(false);
		}
		item.setActionCommand(Command.SHOW_OPTIONS.toString());
		item.addItemListener(this);
		editMenu.add(item);

		JMenuItem settings = GUITools.createJMenuItem("Settings", this,
				Command.SETTINGS, GUITools.getIconSettings(), KeyStroke
						.getKeyStroke('P', InputEvent.ALT_DOWN_MASK));
		editMenu.add(settings);

		/*
		 * Help
		 */
		JMenuItem help = GUITools.createJMenuItem("Online help", this,
				Command.HELP_ONLINE, GUITools.getIconHelp(), KeyStroke
						.getKeyStroke(KeyEvent.VK_F1, 0));
		JMenuItem about = GUITools.createJMenuItem("About", this,
				Command.HELP_ABOUT, GUITools.getIconInfo(), KeyStroke
						.getKeyStroke(KeyEvent.VK_F2, 0));
		JMenuItem license = GUITools.createJMenuItem("License", this,
				Command.HELP_LICENSE, GUITools.getIconLicense(), KeyStroke
						.getKeyStroke('L', InputEvent.CTRL_DOWN_MASK), 'L');
		JMenu helpMenu = GUITools.createJMenu("Help", help, about, license);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);

		try {
			menuBar.setHelpMenu(helpMenu);
		} catch (Error err) {
			menuBar.add(helpMenu);
		}

		return menuBar;
	}

	/**
	 * 
	 * @return
	 */
	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar("Tools");

		/*
		 * File tools
		 */
		ImageIcon icon = GUITools.getIconFolder();
		if (icon != null)
			toolbar.add(GUITools.createButton(icon, this, Command.OPEN,
					"Load  experimental data from file."));
		icon = GUITools.getIconTrash();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this, Command.CLOSE_DATA,
					"Close the current model or the experimental data."));
		}
		icon = GUITools.getIconSave();
		if (icon != null)
			toolbar.add(GUITools
					.createButton(icon, this, Command.SAVE_SIMULATION,
							"Save simulation results to file."));
		icon = GUITools.getIconCamera();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this,
					Command.SAVE_PLOT_IMAGE, "Save plot in an image."));
		}
		toolbar.add(new JSeparator());

		/*
		 * Edit tools
		 */
		icon = GUITools.getIconGear();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this,
					Command.SIMULATION_START,
					"Perform a simulation run with the current settings."));
		}
		icon = GUITools.getIconEvA2();
		if (icon != null) {
			JButton optimization = GUITools
					.createButton(icon, this, Command.OPTIMIZATION,
							"Starts the optimization of the model with respect to given experimental data.");
			optimization.setEnabled(false);
			toolbar.add(optimization);
		}
		icon = GUITools.getIconSettings();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this, Command.SETTINGS,
					"Adjust your preferences"));
		}
		toolbar.add(new JSeparator());

		/*
		 * Help tools
		 */
		icon = GUITools.getIconHelp();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this, Command.HELP_ONLINE,
					"Opens the online help."));
		}
		icon = GUITools.getIconInfo();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this, Command.HELP_ABOUT,
					"Displays information about the authors of this program."));
		}
		icon = GUITools.getIconLicense();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this, Command.HELP_LICENSE,
					"Shows the software license of this product."));
		}

		GUITools.setEnabled(simPanel != null, getJMenuBar(), toolbar,
				Command.SIMULATION_START, Command.SAVE_PLOT_IMAGE,
				Command.CLOSE_DATA);
		GUITools.setEnabled(false, getJMenuBar(), toolbar,
				Command.SAVE_SIMULATION);

		return toolbar;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBoxMenuItem) {
			JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
			if ((item.getActionCommand() != null)
					&& (item.getActionCommand().equals(Command.SHOW_OPTIONS
							.toString()))) {
				simPanel.setShowSettingsPanel(item.isSelected());
			}
		}
	}

	/**
	 * 
	 */
	public void openExperimentalData() {
		JFileChooser chooser = GUITools.createJFileChooser(SBMLsimulator
				.getProperties().get(SimulatorCfgKeys.OPEN_DIR).toString(),
				false, false, JFileChooser.FILES_ONLY,
				SBFileFilter.CSV_FILE_FILTER);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			openExperimentalData(chooser.getSelectedFile());
		}
	}

	/**
	 * 
	 * @param file
	 */
	public void openExperimentalData(File file) {
		CSVDataImporter importer = new CSVDataImporter();
		if (simPanel != null) {
			Model model = simPanel.getModel();
			try {
				MultiBlockTable data = importer.convert(model, file
						.getAbsolutePath());
				if (data != null) {
					simPanel.setExperimentalData(data);
					GUITools.setEnabled(false, getJMenuBar(), toolbar,
							Command.OPEN);
					GUITools.setEnabled(true, getJMenuBar(), toolbar,
							Command.CLOSE_DATA);
					// Optimization should not be available if there is nothing
					// to
					// optimize.
					GUITools.setEnabled(model.getNumQuantities()
							- model.getNumSpeciesReferences() > 0,
							getJMenuBar(), toolbar, Command.OPTIMIZATION);
				}
				SBMLsimulator.getProperties().put(SimulatorCfgKeys.OPEN_DIR,
						file.getParent());
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/**
	 * 
	 * @param path
	 */
	public void openExperimentalData(String path) {
		try {
			openExperimentalData(new File(path));
		} catch (Exception exc) {
			exc.printStackTrace();
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * 
	 */
	public void openModel() {
		openModel(GUITools.openFileDialog(this, SBMLsimulator.getProperties()
				.get(SimulatorCfgKeys.OPEN_DIR).toString(), false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.SBML_FILE_FILTER));
	}

	/**
	 * 
	 * @param file
	 */
	public void openModel(File file) {
		if (file != null) {
			try {
				SBMLDocument doc = SBMLReader.readSBML(file);
				if ((doc != null) && (doc.isSetModel())) {
					SBProperties props = SBMLsimulator.getProperties();
					props.put(SimulatorCfgKeys.OPEN_DIR, file.getParent());
					simPanel = new SimulationPanel(doc.getModel(), props);
					if (GUITools.contains(getContentPane(), simPanel)) {
						getContentPane().remove(simPanel);
					}
					getContentPane().add(simPanel, BorderLayout.CENTER);
					setOptimalSize();
					setTitle(defaultTitle + " - " + file.getName());
					GUITools.setEnabled(true, getJMenuBar(), toolbar,
							Command.SAVE_PLOT_IMAGE, Command.SAVE_MODEL,
							Command.SIMULATION_START, Command.CLOSE_DATA,
							Command.SHOW_OPTIONS);
				} else {
					JOptionPane.showMessageDialog(this, GUITools.toHTML(
							"Could not open model " + file.getAbsolutePath(),
							60));
				}
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/**
	 * Resizes this {@link JFrame} to an apropriate size.
	 */
	private void setOptimalSize() {
		pack();
		int maxSize = 700;
		if (getWidth() > 1.5 * maxSize) {
			this.setSize((int) Math.round(1.5 * maxSize), getHeight());
		}
		if (getHeight() > maxSize) {
			this.setSize(getWidth(), maxSize);
		}
	}

	/**
	 * 
	 * @param p
	 */
	public void setProperties(SBProperties p) {
		if (simPanel != null) {
			try {
				simPanel.setProperties(p);
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/**
	 * 
	 * @param ids
	 */
	public void setSelectedQuantities(String... ids) {
		simPanel.setSelectedQuantities(ids);
	}

	/**
	 * 
	 */
	public void simulate() {
		try {
			GUITools.setEnabled(false, getJMenuBar(), toolbar,
					Command.SIMULATION_START);
			simPanel.simulate();
			GUITools.setEnabled(true, getJMenuBar(), toolbar,
					Command.SAVE_SIMULATION, Command.SAVE_PLOT_IMAGE,
					Command.SIMULATION_START);
		} catch (Exception exc) {
			exc.printStackTrace();
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * 
	 */
	public void optimize() {
		Model model = simPanel.getModel().getSBMLDocument().clone().getModel();
		QuantitySelectionPanel panel = new QuantitySelectionPanel(model);
		if (JOptionPane.showConfirmDialog(this, panel,
				"Select quantities for optimization",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
			simPanel.setAllEnabled(false);
			try {
				simPanel.notifyQuantitiesSelected(panel
						.getSelectedQuantityIds());
				GUITools.setEnabled(false, getJMenuBar(), toolbar,
						Command.SIMULATION_START, Command.CLOSE_DATA,
						Command.OPTIMIZATION, Command.SETTINGS);
				EvA2GUIStarter.init(new EstimationProblem(simPanel.getSolver(),
						simPanel.getDistance(), model, simPanel
								.getExperimentalData(),
						((Boolean) SBMLsimulator.getProperties().get(
								SimulatorCfgKeys.EST_MULTI_SHOOT))
								.booleanValue(), panel
								.getSelectedQuantityRanges()), this, simPanel,
						this);
			} catch (Throwable exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
				simPanel.setAllEnabled(true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
	 */
	public void windowActivated(WindowEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
	 */
	public void windowClosed(WindowEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
	 */
	public void windowClosing(WindowEvent e) {
		if (e.getSource() instanceof JFrame) {
			JFrame frame = (JFrame) e.getSource();
			if ((frame.getName() != null)
					&& (frame.getName().equals(EvAClient.class.getSimpleName()))) {
				simPanel.setAllEnabled(true);
				GUITools.setEnabled(true, getJMenuBar(), toolbar,
						Command.SAVE_MODEL, Command.SIMULATION_START,
						Command.CLOSE_DATA, Command.OPTIMIZATION,
						Command.SETTINGS);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent
	 * )
	 */
	public void windowDeactivated(WindowEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent
	 * )
	 */
	public void windowDeiconified(WindowEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
	 */
	public void windowIconified(WindowEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
	 */
	public void windowOpened(WindowEvent e) {
	}
}
