/*
 * $Id$ $URL:
 * https:
 * //rarepos.cs.uni-tuebingen.de/svn-path/SBMLsimulator/trunk/src/org/sbml/
 * simulator/gui/InteractiveScanPanel.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeNode;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.QuantityWithUnit;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.util.TreeNodeChangeListener;
import org.sbml.jsbml.util.compilers.HTMLFormula;
import org.sbml.simulator.math.odes.SimulationOptions;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;

/**
 * An {@link InteractiveScanPanel} is an element that allows the user to change
 * the values of all instances of {@link Compartment}, {@link Species},
 * {@link Parameter}, and {@link LocalParameter} within a {@link Model}. To this
 * end, it contains a {@link JTabbedPane} for to structure these four groups of
 * {@link QuantityWithUnit}, each containing a {@link JScrollPane} with one
 * {@link JSpinner} for each element. On the bottom there is a reset button to
 * restore the original values.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-17
 * @version $Rev$
 * @since 1.0
 */
public class InteractiveScanPanel extends JPanel implements ActionListener,
    ChangeListener, TreeNodeChangeListener {
  
  /**
   * @author Andreas Dr&auml;ger
   * @date 2010-09-17
   */
  public enum Command {
    /**
     * Resets all values to those defined in the model.
     */
    RESET
  }
  
  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -8244482994475574724L;
  /**
	 * 
	 */
  private JSpinner[] spinQuantity;
  /**
	 * 
	 */
  private Set<ChangeListener> setOfChangeListeners;
  /**
   * Default values
   */
  private double defaultCompartmentValue, defaultParameterValue,
      defaultSpeciesValue;
  /**
   * Backup of the original values
   */
  private double originalValues[];
  /**
   * Direct pointers to all important quantities in the model.
   */
  private QuantityWithUnit[] quantities;
  /**
	 * 
	 */
  private boolean hasLocalParameters;
  /**
	 * 
	 */
  private static final String BUTTON_TOOL_TIP = "Sets all values to the original values from the model. However, previously undefined values will be set to the default values.";
  /**
	 * 
	 */
  private JButton buttonReset;
  /**
   * Allows for fast access to all quantities by storing the index in the array
   * for the identifier.
   */
  private HashMap<String, Integer> quantitiesHash;
  /**
	 * 
	 */
  private JTabbedPane tab;
  /**
	 * 
	 */
  private double paramStepSize, maxCompartmentValue, maxParameterValue,
      maxSpeciesValue;;
  
  /**
	 * 
	 */
  public InteractiveScanPanel(Model model, double maxCompartmentValue,
    double maxSpeciesValue, double maxParameterValue, double paramStepSize) {
    super(new BorderLayout());
    loadPreferences();
    this.paramStepSize = paramStepSize;
    this.maxCompartmentValue = maxCompartmentValue;
    this.maxParameterValue = maxParameterValue;
    this.maxSpeciesValue = maxSpeciesValue;
    this.setOfChangeListeners = new HashSet<ChangeListener>();
    // create layout
    tab = new JTabbedPane();
    init(model);
    
    JPanel foot = new JPanel();
    buttonReset = GUITools.createButton("Reset", null, this, Command.RESET,
      BUTTON_TOOL_TIP);
    buttonReset.setEnabled(false);
    foot.add(buttonReset);
    
    add(tab, BorderLayout.CENTER);
    add(foot, BorderLayout.SOUTH);
  }
  
  /**
   * @param model
   */
  private void init(Model model) {
    if (model != null) {
      int offset = 0;
      // initialize fields
      this.spinQuantity = new JSpinner[model.getNumQuantitiesWithUnit()];
      this.originalValues = new double[spinQuantity.length];
      this.quantities = new QuantityWithUnit[originalValues.length];
      this.quantitiesHash = new HashMap<String, Integer>();
      this.hasLocalParameters = false;
      this.tab.add(
        "Compartments",
        interactiveScanScrollPane(model.getListOfCompartments(),
          maxCompartmentValue, paramStepSize, offset));
      offset += model.getNumCompartments();
      tab.setEnabledAt(0, model.getNumCompartments() > 0);
      
      tab.add(
        "Species",
        interactiveScanScrollPane(model.getListOfSpecies(), maxParameterValue,
          paramStepSize, offset));
      offset += model.getNumSpecies();
      tab.setEnabledAt(1, model.getNumSpecies() > 0);
      
      tab.add(
        "Global Parameters",
        interactiveScanScrollPane(model.getListOfParameters(), maxSpeciesValue,
          paramStepSize, offset));
      tab.setEnabledAt(2, model.getNumParameters() > 0);
      
      tab.add(
        "Local Parameters",
        new JScrollPane(interactiveScanLocalParameters(maxParameterValue,
          paramStepSize, model.getNumSymbols(), model.getListOfReactions()),
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
      tab.setEnabledAt(3, hasLocalParameters);
      for (int i = tab.getTabCount() - 1; i >= 0; i--) {
        if (tab.isEnabledAt(i)) {
          tab.setSelectedIndex(i);
        }
      }
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    if ((e.getActionCommand() != null)
        && (e.getActionCommand().equals(Command.RESET.toString()))) {
      for (int i = 0; i < originalValues.length; i++) {
        spinQuantity[i].setValue(Double.valueOf(originalValues[i]));
        quantities[i].setValue(originalValues[i]);
      }
    }
  }
  
  /**
   * @param cl
   * @return
   */
  public boolean addChangeListener(ChangeListener cl) {
    return setOfChangeListeners.add(cl);
  }
  
  /**
   * Checks whether or not to enable the button to reset the values for each
   * {@link JSpinner}.
   */
  private void checkButton() {
    boolean allEqual = true;
    for (int i = 0; i < originalValues.length && allEqual; i++) {
      allEqual &= originalValues[i] == ((Number) spinQuantity[i].getValue())
          .doubleValue();
    }
    buttonReset.setEnabled(!allEqual);
  }
  
  /**
   * @param id
   */
  private void checkButton(String id) {
    int i = quantitiesHash.get(id).intValue();
    double newVal = ((Number) spinQuantity[i].getValue()).doubleValue();
    if (originalValues[i] != newVal) {
      buttonReset.setEnabled(true);
      quantities[i].setValue(newVal);
    }
  }
  
  /**
   * @return
   */
  public int getNumQuantities() {
    return quantities.length;
  }
  
  /**
   * @return
   */
  public Properties getProperties() {
    Properties p = new Properties();
    p.put(SimulationOptions.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE,
      Double.valueOf(defaultCompartmentValue));
    p.put(SimulationOptions.OPT_DEFAULT_SPECIES_INITIAL_VALUE,
      Double.valueOf(defaultSpeciesValue));
    p.put(SimulationOptions.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS,
      Double.valueOf(defaultParameterValue));
    return p;
  }
  
  /**
   * @param maxParameterValue
   * @param paramStepSize
   * @param offset
   * @param listOfReactions
   * @return
   */
  private JPanel interactiveScanLocalParameters(double maxParameterValue,
    double paramStepSize, int offset, ListOf<Reaction> listOfReactions) {
    JPanel parameterPanel = new JPanel();
    parameterPanel
        .setLayout(new BoxLayout(parameterPanel, BoxLayout.PAGE_AXIS));
    for (Reaction r : listOfReactions) {
      if (r.isSetKineticLaw() && r.getKineticLaw().getNumLocalParameters() > 0) {
        hasLocalParameters = true;
        JPanel panel = interactiveScanTable(r.getKineticLaw()
            .getListOfLocalParameters(), maxParameterValue, paramStepSize,
          offset);
        offset += r.getKineticLaw().getNumLocalParameters();
        panel.setBorder(BorderFactory.createTitledBorder(String.format(
          " Reaction %s ", r.getId())));
        parameterPanel.add(panel);
      }
    }
    return parameterPanel;
  }
  
  /**
   * @param list
   * @param maxValue
   * @param stepSize
   * @param offset
   * @return
   */
  private JScrollPane interactiveScanScrollPane(
    ListOf<? extends QuantityWithUnit> list, double maxValue, double stepSize,
    int offset) {
    return new JScrollPane(interactiveScanTable(list, maxValue, stepSize,
      offset), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }
  
  /**
   * @param list
   * @param maxValue
   * @param stepSize
   * @param offset
   *        index offset
   * @return
   */
  private JPanel interactiveScanTable(ListOf<? extends QuantityWithUnit> list,
    double maxValue, double stepSize, int offset) {
    JPanel panel = new JPanel();
    LayoutHelper lh = new LayoutHelper(panel);
    LinkedList<String> nans = new LinkedList<String>();
    double value = 0;
    String name = "";
    int index;
    for (int i = 0; i < list.size(); i++) {
      QuantityWithUnit p = list.get(i);
      value = p.getValue();
      if (Double.isNaN(p.getValue())) {
        name = p.getClass().getSimpleName().toLowerCase();
        if (p instanceof Compartment) {
          if(((Compartment)p).getSpatialDimensions()>0) {
            value = defaultCompartmentValue;
            p.setValue(value);
            nans.add(p.getId());
          }
        } else if (p instanceof Species) {
          value = defaultSpeciesValue;
          p.setValue(value);
          nans.add(p.getId());
        } else if (p instanceof Parameter) {
          value = defaultParameterValue;
          p.setValue(value);
          nans.add(p.getId());
        }
        if (!(p instanceof Species) && (list.size() > 1)) {
          name += "s";
        }
      }
      maxValue = Math.max(value, maxValue);
      index = i + offset;
      spinQuantity[index] = new JSpinner(new SpinnerNumberModel(value,
        Math.min(0d, value), maxValue, stepSize));
      originalValues[index] = value; // backup.
      quantities[index] = p;
      quantities[index].addTreeNodeChangeListener(this);
      list.addTreeNodeChangeListener(this);
      quantitiesHash.put(p.getId(), Integer.valueOf(index));
      
      spinQuantity[index].setName(p.getId());
      spinQuantity[index].addChangeListener(this);
      lh.add(new JLabel(StringUtil.toHTML(p.toString(), 40)), 0, i, 1, 1, 0, 0);
      lh.add(spinQuantity[index], 2, i, 1, 1, 0, 0);
      lh.add(
        new JLabel(StringUtil.toHTML(p.isSetUnits() ? HTMLFormula.toHTML(p
            .getUnitsInstance()) : "")), 4, i, 1, 1, 0, 0);
    }
    lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
    lh.add(new JPanel(), 3, 0, 1, 1, 0, 0);
    if (nans.size() > 0) {
      String l = nans.toString().substring(1);
      String msg = String
          .format(
            "Undefined value%s for the %s %s ha%s been replaced by its default value %.3f.",
            nans.size() > 1 ? "s" : "", name, l.substring(0, l.length() - 1),
            nans.size() > 1 ? "ve" : "s", value);
      JEditorPane label = new JEditorPane("text/html", StringUtil.toHTML(msg,
        80));
      label.setEditable(false);
      Component component;
      if (nans.size() > 20) {
        component = new JScrollPane(label,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        label.setPreferredSize(new Dimension(450, 450));
      } else {
        component = label;
      }
      JOptionPane.showMessageDialog(this, component,
        "Replacing undefined values", JOptionPane.INFORMATION_MESSAGE);
    }
    return panel;
  }
  
  /**
   * @param cl
   * @return
   */
  public boolean removeChangeListener(ChangeListener cl) {
    return setOfChangeListeners.remove(cl);
  }
  
  /*
   * (non-Javadoc)
   * @see org.sbml.jsbml.util.TreeNodeChangeListener#nodeAdded(javax.swing.tree.TreeNode)
   */
  public void nodeAdded(TreeNode node) {
    if (node instanceof SBase) {
    	SBase sb = (SBase) node;
      tab.removeAll();
      init(sb.getModel());
    }
  }
  
  /*
   * (non-Javadoc)
   * @see org.sbml.jsbml.util.TreeNodeChangeListener#nodeRemoved(javax.swing.tree.TreeNode)
   */
  public void nodeRemoved(TreeNode node) {
    if (node instanceof SBase) {
    	SBase sb = (SBase) node;
      tab.removeAll();
      init(sb.getModel());
    }
  }
  
  /**
   * Disables or enables user interaction on this element.
   * 
   * @param enabled
   */
  public void setAllEnabled(boolean enabled) {
    for (int i = 0; i < spinQuantity.length; i++) {
      spinQuantity[i].setEnabled(enabled);
    }
    if (!buttonReset.isEnabled() && enabled) {
      checkButton();
    } else {
      buttonReset.setEnabled(enabled);
    }
  }
  
  /**
   * @param properties
   */
  public void loadPreferences() {
    SBPreferences prefs = SBPreferences
        .getPreferencesFor(SimulationOptions.class);
    defaultCompartmentValue = prefs
        .getDouble(SimulationOptions.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE);
    defaultSpeciesValue = prefs
        .getDouble(SimulationOptions.OPT_DEFAULT_SPECIES_INITIAL_VALUE);
    defaultParameterValue = prefs
        .getDouble(SimulationOptions.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * 
   * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
   * )
   */
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof JSpinner) {
      String id = ((JSpinner) e.getSource()).getName();
      if (id != null) {
        checkButton(id);
      }
    } else {
      checkButton();
    }
    for (ChangeListener cl : setOfChangeListeners) {
      cl.stateChanged(e);
    }
  }
  
  /*
   * (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getSource() instanceof QuantityWithUnit) {
      QuantityWithUnit q = (QuantityWithUnit) evt.getSource();
      updateQuantitySpinner(q.getId());
    }
  }
  
  /**
   * @param index
   */
  private void updateQuantitySpinner(int index) {
    if (quantities[index].getValue() != ((Number) spinQuantity[index]
        .getValue()).doubleValue()) {
      spinQuantity[index]
          .setValue(Double.valueOf(quantities[index].getValue()));
      if (quantities[index].getValue() != originalValues[index]) {
        buttonReset.setEnabled(true);
      }
    }
  }
  
  /**
   * Updates the value for the spinner corresponding to the
   * {@link QuantityWithUnit} with the given identifier only.
   * 
   * @param id
   *        Identifier of the element to be updated.
   */
  private void updateQuantitySpinner(String id) {
    Integer key = quantitiesHash.get(id);
    if (key != null) {
      updateQuantitySpinner(key.intValue());
    }
  }
  
  /**
   * @param id
   * @param value
   */
  public void updateQuantity(String id, double value) {
    quantities[quantitiesHash.get(id).intValue()].setValue(value);
  }
  
}
