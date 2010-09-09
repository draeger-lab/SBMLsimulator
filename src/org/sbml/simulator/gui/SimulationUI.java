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
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.table.TableModel;

import org.sbml.jsbml.Model;
import org.sbml.optimization.EvA2GUIStarter;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.resources.Resource;
import org.sbml.squeezer.CfgKeys;
import org.sbml.squeezer.gui.SettingsDialog;

import de.zbit.gui.GUITools;
import de.zbit.io.CSVWriter;
import de.zbit.io.SBFileFilter;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-15
 * 
 */
public class SimulationUI extends JFrame implements ActionListener,
		ItemListener {

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
		 * Open file with experimental data.
		 */
		OPEN_DATA,
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
		 * Adjust user's preferences
		 */
		SETTINGS,
		/**
		 * Start a new simulation with the current settings.
		 */
		SIMULATION_START
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
	 * Compression factor for JPEG output
	 */
	private float compression;

	/**
	 * Pointer to the model
	 */
	private Model model;

	/**
	 * Standard directory to open data files.
	 */
	private String openDir;

	/**
	 * This is the quote character in CSV files
	 */
	private char quoteChar;

	/**
	 * 
	 */
	private String saveDir;

	/**
	 * This is the separator char in CSV files
	 */
	private char separatorChar;

	/**
	 * GUI element that lets the user run the simulation.
	 */
	private SimulationPanel simPanel;

	/**
	 * The toolbar of this element.
	 */
	private JToolBar toolbar;

	/**
	 * 
	 * @param owner
	 * @param model
	 */
	public SimulationUI(Model model) {
		this(model, new SimulationPanel(model));
	}

	/**
	 * 
	 * @param owner
	 * @param model
	 * @param settings
	 */
	public SimulationUI(Model model, Properties settings) {
		this(model, new SimulationPanel(model, settings));
	}

	/**
	 * 
	 * @param owner
	 * @param model
	 * @param simulationPanel
	 */
	public SimulationUI(Model model, SimulationPanel simulationPanel) {
		super("Simulation of model " + model.toString());

		// init properties
		this.openDir = System.getProperty("user.home");
		this.saveDir = System.getProperty("user.home");
		this.compression = 0.9f;
		this.quoteChar = '#';
		this.separatorChar = ',';
		setProperties(simulationPanel.getProperties());

		simPanel = simulationPanel;
		this.model = model;

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
		getContentPane().add(simPanel, BorderLayout.CENTER);
		// getContentPane().add(new StatusBar(), BorderLayout.SOUTH);
		pack();
		int maxSize = 700;
		if (getWidth() > 1.5 * maxSize) {
			this.setSize((int) Math.round(1.5 * maxSize), getHeight());
		}
		if (getHeight() > maxSize) {
			this.setSize(getWidth(), maxSize);
		}
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
		case OPEN_DATA:
			openExperimentalData();
			break;
		case SAVE_PLOT_IMAGE:
			savePlotImage();
			break;
		case SAVE_SIMULATION:
			saveSimulationResults();
			break;
		case SETTINGS:
			adjustPreferences();
			break;
		case EXIT:
			// TODO: try to save the settings
			System.exit(0);
		case HELP_ONLINE:
			GUITools.showMessage(Resource.class
					.getResource("html/online-help.html"), "SBMLsimulator "
					+ SBMLsimulator.getVersionNumber() + " online help", this,
					getIconHelp48());
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
							+ SBMLsimulator.getVersionNumber(), this,
					getIconLicense48());
			break;
		case OPTIMIZATION:
			startOptimization();
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
	 */
	private void adjustPreferences() {
		try {
			SettingsPanelSimulation ps = new SettingsPanelSimulation();
			ps.setProperties(getProperties());
			SettingsDialog dialog = new SettingsDialog("Simulatin Preferences");
			Properties p = new Properties();
			if (dialog.showSettingsDialog(getProperties(), ps) == SettingsDialog.APPROVE_OPTION) {
				for (Object key : dialog.getSettings().keySet()) {
					p.put(key, dialog.getSettings().get(key));
				}
				simPanel.setProperties(p);
			}
		} catch (Exception exc) {
			GUITools.showErrorMessage(this, exc);
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
				Command.OPEN_DATA, getIconFolder(), KeyStroke.getKeyStroke('O',
						InputEvent.CTRL_DOWN_MASK));
		JMenuItem saveSimItem = GUITools.createJMenuItem("Save simulation",
				this, Command.SAVE_SIMULATION, getIconSave(), KeyStroke
						.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK));
		JMenuItem savePlotItem = GUITools.createJMenuItem("Save plot", this,
				Command.SAVE_PLOT_IMAGE, getIconCamera(), KeyStroke
						.getKeyStroke('S', KeyEvent.ALT_DOWN_MASK));
		JMenuItem exitItem = GUITools.createJMenuItem("Exit", this,
				Command.EXIT, KeyStroke.getKeyStroke(KeyEvent.VK_F4,
						InputEvent.ALT_DOWN_MASK));

		JMenu fileMenu = GUITools.createJMenu("File", openItem, saveSimItem,
				savePlotItem, exitItem);

		/*
		 * Edit
		 */
		JMenuItem simulation = GUITools.createJMenuItem("Start simulation",
				this, Command.SIMULATION_START, getIconGear(), 'S');
		JMenuItem optimization = GUITools.createJMenuItem("Optimization", this,
				Command.OPTIMIZATION, getIconEvA2(), 'O');
		optimization.setEnabled(false);
		JMenu editMenu = GUITools.createJMenu("Edit", simulation, optimization);
		editMenu.addSeparator();

		JCheckBoxMenuItem item = new JCheckBoxMenuItem("Show options",
				simPanel != null ? simPanel.isShowSettingsPanel() : true);
		item.setName("OPTIONS");
		item.addItemListener(this);
		editMenu.add(item);

		JMenuItem settings = GUITools.createJMenuItem("Settings", this,
				Command.SETTINGS, getIconSettings(), KeyStroke.getKeyStroke(
						'P', InputEvent.ALT_DOWN_MASK));
		editMenu.add(settings);

		/*
		 * Help
		 */
		JMenuItem help = GUITools.createJMenuItem("Online help", this,
				Command.HELP_ONLINE, getIconHelp(), KeyStroke.getKeyStroke(
						KeyEvent.VK_F1, 0));
		JMenuItem about = GUITools.createJMenuItem("About", this,
				Command.HELP_ABOUT, getIconInfo(), KeyStroke.getKeyStroke(
						KeyEvent.VK_F2, 0));
		JMenuItem license = GUITools.createJMenuItem("License", this,
				Command.HELP_LICENSE, getIconLicense(), KeyStroke.getKeyStroke(
						'L', InputEvent.CTRL_DOWN_MASK), 'L');
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
		ImageIcon icon = getIconFolder();
		if (icon != null)
			toolbar.add(GUITools.createButton(icon, this, Command.OPEN_DATA,
					"Load  experimental data from file."));
		icon = getIconSave();
		if (icon != null)
			toolbar.add(GUITools
					.createButton(icon, this, Command.SAVE_SIMULATION,
							"Save simulation results to file."));
		icon = getIconCamera();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this,
					Command.SAVE_PLOT_IMAGE, "Save plot in an image."));
		}
		icon = getIconSettings();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this, Command.SETTINGS,
					"Adjust your preferences"));
		}
		GUITools.setEnabled(false, getJMenuBar(), toolbar,
				Command.SAVE_PLOT_IMAGE, Command.SAVE_SIMULATION);

		icon = getIconGear();
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this,
					Command.SIMULATION_START,
					"Perform a simulation run with the current settings."));
		}

		icon = getIconEvA2();
		if (icon != null) {
			JButton optimization = GUITools
					.createButton(icon, this, Command.OPTIMIZATION,
							"Starts the optimization of the model with respect to given experimental data.");
			optimization.setEnabled(false);
			toolbar.add(optimization);
		}

		return toolbar;
	}

	/**
	 * 
	 * @return
	 */
	private ImageIcon getIconCamera() {
		return new ImageIcon(Resource.class.getResource("img/camera_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	private ImageIcon getIconEvA2() {
		return new ImageIcon(Resource.class.getResource("/images/icon1.gif"));
	}

	/**
	 * 
	 * @return
	 */
	private ImageIcon getIconFolder() {
		return new ImageIcon(Resource.class.getResource("img/folder_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	private ImageIcon getIconGear() {
		return new ImageIcon(Resource.class.getResource("img/gear_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	private Icon getIconHelp() {
		return new ImageIcon(Resource.class.getResource("img/help_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	private Icon getIconHelp48() {
		return new ImageIcon(Resource.class.getResource("img/help_48.png"));
	}

	/**
	 * 
	 * @return
	 */
	private Icon getIconInfo() {
		return new ImageIcon(Resource.class.getResource("img/info_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	private Icon getIconLicense() {
		return new ImageIcon(Resource.class.getResource("img/licence_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	private Icon getIconLicense48() {
		return new ImageIcon(Resource.class.getResource("img/licence_48.png"));
	}

	/**
	 * 
	 * @return
	 */
	private ImageIcon getIconSave() {
		return new ImageIcon(Resource.class.getResource("img/save_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	private ImageIcon getIconSettings() {
		return new ImageIcon(Resource.class.getResource("img/settings_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public Properties getProperties() {

		Properties p = simPanel.getProperties();

		/*
		 * General
		 */
		p.put(CfgKeys.JPEG_COMPRESSION_FACTOR, Float.valueOf(compression));

		/*
		 * CSV file parsing
		 */
		p.put(CfgKeys.CSV_FILES_OPEN_DIR, openDir);
		p.put(CfgKeys.CSV_FILES_SAVE_DIR, saveDir);
		p.put(CfgKeys.CSV_FILES_SEPARATOR_CHAR, Character
				.valueOf(separatorChar));
		p.put(CfgKeys.CSV_FILES_QUOTE_CHAR, Character.valueOf(quoteChar));

		return p;
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
			if ((item.getName() != null) && (item.getName().equals("OPTIONS"))) {
				simPanel.setShowSettingsPanel(item.isSelected());
			}
		}
	}

	/**
	 * 
	 */
	public void openExperimentalData() {
		JFileChooser chooser = GUITools.createJFileChooser(openDir, false,
				false, JFileChooser.FILES_ONLY, SBFileFilter.CSV_FILE_FILTER);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				openExperimentalData(chooser.getSelectedFile());
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/**
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void openExperimentalData(File file) throws Exception {
		openDir = file.getParent();
		CSVDataImporter importer = new CSVDataImporter();
		MultiBlockTable data = importer.convert(model, file.getAbsolutePath());
		simPanel.setExperimentalData(data);
		GUITools.setEnabled(false, getJMenuBar(), toolbar, Command.OPEN_DATA);
		// Optimization should not be available if there is nothing to optimize.
		GUITools.setEnabled(model.getNumQuantities()
				- model.getNumSpeciesReferences() > 0, getJMenuBar(), toolbar,
				Command.OPTIMIZATION);
	}

	/**
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void openExperimentalData(String path) throws Exception {
		openExperimentalData(new File(path));
	}

	private void savePlotImage() {
		try {
			this.saveDir = simPanel.getPlot().savePlotImage(saveDir,
					compression);
		} catch (Exception exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * 
	 */
	private void saveSimulationResults() {
		try {
			TableModel simTabModel = simPanel.getSimulationResultsTable()
					.getModel();
			if (simTabModel.getRowCount() > 0) {
				JFileChooser fc = GUITools.createJFileChooser(saveDir, false,
						false, JFileChooser.FILES_ONLY,
						SBFileFilter.CSV_FILE_FILTER);
				if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					File out = fc.getSelectedFile();
					this.saveDir = out.getParent();
					if (!out.exists()
							|| GUITools.overwriteExistingFile(this, out)) {
						CSVWriter writer = new CSVWriter();
						writer.write(simTabModel, separatorChar, out);
					}
				}
			} else {
				String msg = "No simulation has been performed yet. Please run the simulation first.";
				JOptionPane.showMessageDialog(this, GUITools.toHTML(msg, 40));
			}
		} catch (IOException exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * 
	 * @param p
	 */
	public void setProperties(Properties p) {
		/*
		 * CSV file parsing
		 */
		if (p.containsKey(CfgKeys.CSV_FILES_OPEN_DIR)) {
			openDir = p.get(CfgKeys.CSV_FILES_OPEN_DIR).toString();
		}
		if (p.containsKey(CfgKeys.CSV_FILES_SEPARATOR_CHAR)) {
			separatorChar = ((Character) p
					.get(CfgKeys.CSV_FILES_SEPARATOR_CHAR)).charValue();
		}
		if (p.containsKey(CfgKeys.CSV_FILES_QUOTE_CHAR)) {
			quoteChar = ((Character) p.get(CfgKeys.CSV_FILES_QUOTE_CHAR))
					.charValue();
		}
		if (p.containsKey(CfgKeys.CSV_FILES_SAVE_DIR)) {
			saveDir = p.get(CfgKeys.CSV_FILES_SAVE_DIR).toString();
		}

		/*
		 * General
		 */
		if (p.containsKey(CfgKeys.JPEG_COMPRESSION_FACTOR)) {
			compression = Float.parseFloat(p.get(
					CfgKeys.JPEG_COMPRESSION_FACTOR).toString());
		}
	}

	/**
	 * 
	 * @param identifiers
	 */
	public void setVariables(String... identifiers) {
		simPanel.setVariables(identifiers);
	}

	/**
	 * 
	 */
	public void simulate() {
		try {
			simPanel.simulate();
			GUITools.setEnabled(true, getJMenuBar(), toolbar,
					Command.SAVE_SIMULATION, Command.SAVE_PLOT_IMAGE);
		} catch (Exception exc) {
			exc.printStackTrace();
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * 
	 */
	public void startOptimization() {
		QuantitySelectionPanel panel = new QuantitySelectionPanel(model);
		if (JOptionPane.showConfirmDialog(this, panel,
				"Select quantities for optimization",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
			try {
				EvA2GUIStarter.init(new EstimationProblem(simPanel.getSolver(),
						simPanel.getDistance(), model, simPanel
								.getExperimentalData(), panel
								.getSelectedQuantityRanges()));
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
		}
	}
}
