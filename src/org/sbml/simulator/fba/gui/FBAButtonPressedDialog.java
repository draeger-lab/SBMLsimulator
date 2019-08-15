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
package org.sbml.simulator.fba.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Creates a dialog window where the user is requested to load a concentration and a Gibbs file
 * for flux balance analysis.
 * @author Meike Aichele
 * @version $Rev: 1159 $
 * @date 12.07.2012
 * @since 1.0
 */
public class FBAButtonPressedDialog extends JDialog implements ActionListener{

  /**
   * Cancel button to exit the dialog without loading a file
   */
  private JButton buttonCancel;

  /**
   * button which opens a file chooser to choose a file
   */
  private JButton buttonConcentrationsFile;

  /**
   * button which opens a file chooser to choose a file
   */
  private JButton buttonEnergieDatei;

  /**
   * OK button which closes the dialog, when files are loaded
   */
  private JButton buttonOk;

  /**
   * the incoming concentration file
   */
  private File concentrationFile;

  /**
   * the incoming Gibbs energie file
   */
  private File energieFile;

  /**
   * boolean that is true if files were loaded
   */
  private boolean hasFiles;

  /**
   * default serial version
   */
  private static final long serialVersionUID = 1L;



  /**
   * field to write in the concentration file path
   */
  private JTextField textFieldConcentrationsFile;

  /**
   * field to write in the Gibbs energie file path
   */
  private JTextField textFieldEnergieFile;

  /**
   * Creates a new dialog to load the files for flux balance analysis.
   * @param windowAncestor
   * @param documentModal
   */
  public FBAButtonPressedDialog(Window windowAncestor,
    ModalityType documentModal) {
    super(windowAncestor, documentModal);
    initialize();
    hasFiles = false;
  }

  /*
   * (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    JFileChooser filechooser = new JFileChooser();
    filechooser.setMultiSelectionEnabled(false);
    filechooser.setDialogType(JFileChooser.OPEN_DIALOG);
    filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    if (e.getSource().equals(buttonEnergieDatei)) {
      if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        energieFile = filechooser.getSelectedFile();
        textFieldEnergieFile.setText(energieFile.getPath());
      } else {
        energieFile = null;
        textFieldEnergieFile.setText("");
      }
      checkOk();
    }
    if (e.getSource().equals(buttonConcentrationsFile)) {
      if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        concentrationFile = filechooser.getSelectedFile();
        textFieldConcentrationsFile.setText(concentrationFile.getPath());
      } else {
        concentrationFile = null;
        textFieldConcentrationsFile.setText("");
      }
      checkOk();
    }
    if (e.getSource().equals(buttonCancel)) {
      dispose();
    }
    if (e.getSource().equals(buttonOk)) {
      hasFiles = true;
      dispose();
    }
  }

  /**
   * Sets the ok-button enabled if there is no file entered
   */
  private void checkOk() {
    if (energieFile != null || concentrationFile != null) {
      buttonOk.setEnabled(true);
    } else {
      buttonOk.setEnabled(false);
    }
  }

  /**
   * 
   * @return the incoming concentration file
   */
  public File getConcentrationFile() {
    return concentrationFile;
  }

  /**
   * @return the incoming Gibbs energie file
   */
  public File getEnergieFile() {
    return energieFile;
  }

  /**
   * @return hasFiles
   */
  public boolean hasFiles() {
    return hasFiles;
  }

  /**
   * Create the Buttons and Fields in the dialog
   */
  private void initialize() {
    setTitle("Please load the files to compute flux balance analysis");
    setSize(new Dimension(500, 300));
    setLayout(new BorderLayout());

    JPanel container = new JPanel(new GridLayout(3, 1));

    /*
     * fields and buttons for the gibbs file
     */
    JPanel panelEnergieFile = new JPanel(new BorderLayout());
    panelEnergieFile.setBorder(BorderFactory.createTitledBorder("Gibbs energie file: "));
    textFieldEnergieFile = new JTextField();
    buttonEnergieDatei = new JButton("...");
    buttonEnergieDatei.addActionListener(this);
    panelEnergieFile.add(textFieldEnergieFile, BorderLayout.CENTER);
    panelEnergieFile.add(buttonEnergieDatei, BorderLayout.EAST);
    container.add(panelEnergieFile);

    /*
     * fields and buttons for the concentrations file
     */
    JPanel panelConcFile = new JPanel(new BorderLayout());
    panelConcFile.setBorder(BorderFactory.createTitledBorder("Concentrations file:"));
    textFieldConcentrationsFile = new JTextField();
    buttonConcentrationsFile = new JButton("...");
    buttonConcentrationsFile.addActionListener(this);
    panelConcFile.add(textFieldConcentrationsFile, BorderLayout.CENTER);
    panelConcFile.add(buttonConcentrationsFile, BorderLayout.EAST);
    container.add(panelConcFile);

    /*
     * ok and cancel buttons
     */
    JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonOk = new JButton("Ok");
    buttonOk.setEnabled(false);
    buttonOk.addActionListener(this);
    buttonCancel = new JButton("Cancel");
    buttonCancel.addActionListener(this);
    panelButtons.add(buttonOk, BorderLayout.WEST);
    panelButtons.add(buttonCancel, BorderLayout.EAST);
    add(panelButtons, BorderLayout.SOUTH);

    add(container, BorderLayout.NORTH);
    setSize(new Dimension(450, 200));
  }
}
