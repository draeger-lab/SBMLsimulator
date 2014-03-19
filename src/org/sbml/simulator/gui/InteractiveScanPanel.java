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
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
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
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.util.TreeNodeChangeListener;
import org.sbml.jsbml.util.TreeNodeRemovedEvent;
import org.sbml.jsbml.util.compilers.HTMLFormula;
import org.sbml.simulator.SimulationOptions;

import de.zbit.gui.GUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.util.ResourceManager;
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
   * Supports localization of the application.
   */
  private static final ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");

  /**
   * @author Andreas Dr&auml;ger
   * @date 2010-09-17
   */
  public enum Command implements ActionCommand {
    /**
     * Resets all values to those defined in the model.
     */
    RESET;

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getName()
     */
    @Override
    public String getName() {
      return bundle.getString(toString());
    }

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    @Override
    public String getToolTip() {
      return bundle.getString(toString() + "_TOOLTIP");
    }

  }

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -8244482994475574724L;
  /**
   * 
   */
  private JSpinner spinQuantity[];
  /**
   * 
   */
  private List<ChangeListener> listOfChangeListeners;
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
  private QuantityWithUnit quantities[];
  /**
   * 
   */
  private boolean hasLocalParameters;
  /**
   * 
   */
  private JButton buttonReset;
  /**
   * Allows for fast access to all quantities by storing the index in the array
   * for the identifier.
   */
  private Map<String, Integer> quantitiesHash;
  /**
   * 
   */
  private JTabbedPane tab;
  /**
   * 
   */
  private double paramStepSize, minValue, maxCompartmentValue, maxParameterValue,
  maxSpeciesValue;

  /**
   * 
   * @param model
   * @param minValue
   * @param maxCompartmentValue
   * @param maxSpeciesValue
   * @param maxParameterValue
   * @param paramStepSize
   */
  public InteractiveScanPanel(Model model, double minValue, double maxCompartmentValue,
    double maxSpeciesValue, double maxParameterValue, double paramStepSize) {
    super(new BorderLayout());
    loadPreferences();
    this.paramStepSize = paramStepSize;
    this.minValue = minValue;
    this.maxCompartmentValue = maxCompartmentValue;
    this.maxParameterValue = maxParameterValue;
    this.maxSpeciesValue = maxSpeciesValue;
    listOfChangeListeners = new LinkedList<ChangeListener>();
    // create layout
    tab = new JTabbedPane();
    init(model);

    JPanel foot = new JPanel();
    buttonReset = GUITools.createJButton(this, Command.RESET);
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
      spinQuantity = new JSpinner[model.getQuantityWithUnitCount()];
      originalValues = new double[spinQuantity.length];
      quantities = new QuantityWithUnit[originalValues.length];
      quantitiesHash = new HashMap<String, Integer>();
      hasLocalParameters = false;
      tab.add(
        bundle.getString("COMPARTMENTS"),
        interactiveScanScrollPane(model.getListOfCompartments(), minValue,
          maxCompartmentValue, paramStepSize, offset));
      offset += model.getCompartmentCount();
      tab.setEnabledAt(0, model.getCompartmentCount() > 0);

      tab.add(
        bundle.getString("SPECIES"),
        interactiveScanScrollPane(model.getListOfSpecies(), minValue,
          maxParameterValue, paramStepSize, offset));
      offset += model.getSpeciesCount();
      tab.setEnabledAt(1, model.getSpeciesCount() > 0);

      tab.add(
        bundle.getString("GLOBAL_PARAMETERS"),
        interactiveScanScrollPane(model.getListOfParameters(), minValue,
          maxSpeciesValue, paramStepSize, offset));
      tab.setEnabledAt(2, model.getParameterCount() > 0);

      tab.add(
        bundle.getString("LOCAL_PARAMETERS"),
        new JScrollPane(interactiveScanLocalParameters(maxParameterValue, minValue,
          paramStepSize, model.getSymbolCount(), model.getListOfReactions())));
      tab.setEnabledAt(3, hasLocalParameters);
      for (int i = tab.getTabCount() - 1; i >= 0; i--) {
        if (tab.isEnabledAt(i)) {
          tab.setSelectedIndex(i);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
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
    return listOfChangeListeners.add(cl);
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
    p.put(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE,
      Double.valueOf(defaultCompartmentValue));
    p.put(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE,
      Double.valueOf(defaultSpeciesValue));
    p.put(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE,
      Double.valueOf(defaultParameterValue));
    return p;
  }

  /**
   * 
   * @param maxParameterValue
   * @param paramStepSize
   * @param minValue
   * @param offset
   * @param listOfReactions
   * @return
   */
  private JPanel interactiveScanLocalParameters(double maxParameterValue, double minValue,
    double paramStepSize, int offset, ListOf<Reaction> listOfReactions) {
    JPanel parameterPanel = new JPanel();
    parameterPanel
    .setLayout(new BoxLayout(parameterPanel, BoxLayout.PAGE_AXIS));
    for (Reaction r : listOfReactions) {
      if (r.isSetKineticLaw() && r.getKineticLaw().getLocalParameterCount() > 0) {
        hasLocalParameters = true;
        JPanel panel = interactiveScanTable(r.getKineticLaw().getListOfLocalParameters(), minValue, maxParameterValue, paramStepSize, offset);
        offset += r.getKineticLaw().getLocalParameterCount();
        panel.setBorder(BorderFactory.createTitledBorder(MessageFormat.format(
          bundle.getString("REACTION_ID"), r.getId())));
        parameterPanel.add(panel);
      }
    }
    return parameterPanel;
  }

  /**
   * 
   * @param list
   * @param minValue
   * @param maxValue
   * @param stepSize
   * @param offset
   * @return
   */
  private JScrollPane interactiveScanScrollPane(
    ListOf<? extends QuantityWithUnit> list, double minValue, double maxValue, double stepSize,
    int offset) {
    return new JScrollPane(interactiveScanTable(list, minValue, maxValue, stepSize, offset));
  }

  /**
   * 
   * @param list
   * @param minValue
   * @param maxValue
   * @param stepSize
   * @param offset
   * @return
   */
  private JPanel interactiveScanTable(ListOf<? extends QuantityWithUnit> list,
    double minValue, double maxValue, double stepSize, int offset) {
    JPanel panel = new JPanel();
    LayoutHelper lh = new LayoutHelper(panel);
    LinkedList<String> nans = new LinkedList<String>();
    double value = 0;
    String name = "";
    int index;
    for (int i = 0; i < list.size(); i++) {
      QuantityWithUnit p = list.get(i);
      value = p.getValue();
      if (!p.isSetValue()) {
        if (p instanceof Compartment) {
          name = "NAME_PARTS_COMPARTMENT";
          if (((Compartment) p).getSpatialDimensions() > 0) {
            value = defaultCompartmentValue;
            p.setValue(value);
            nans.add(p.isSetName() ? p.getName() : p.getId());
          }
        } else if (p instanceof Species) {
          name = "NAME_PARTS_SPECIES";
          value = defaultSpeciesValue;
          p.setValue(value);
          nans.add(p.isSetName() ? p.getName() : p.getId());
        } else if (p instanceof Parameter) {
          name = "NAME_PARTS_PARAMETER";
          value = defaultParameterValue;
          p.setValue(value);
          nans.add(p.isSetName() ? p.getName() : p.getId());
        }
      }
      maxValue = Math.max(value, maxValue);
      index = i + offset;
      spinQuantity[index] = new JSpinner(new SpinnerNumberModel(value, Math.min(minValue, value), maxValue, stepSize));
      originalValues[index] = value; // backup.
      quantities[index] = p;
      quantities[index].addTreeNodeChangeListener(this);
      list.addTreeNodeChangeListener(this);
      quantitiesHash.put(p.getId(), Integer.valueOf(index));

      spinQuantity[index].setName(p.getId());
      spinQuantity[index].addChangeListener(this);
      lh.add(new JLabel(StringUtil.toHTML(p.toString(), 40)), 0, i, 1, 1, 0d, 0d);
      lh.add(spinQuantity[index], 2, i, 1, 1, 0, 0);
      UnitDefinition ud = p.getDerivedUnitDefinition();
      if (ud != null) {
        lh.add(new JLabel(StringUtil.toHTML(HTMLFormula.toHTML(ud))), 4, i, 1,
          1, 0d, 0d);
      }
    }
    lh.add(new JPanel(), 1, 0, 1, 1, 0d, 0d);
    lh.add(new JPanel(), 3, 0, 1, 1, 0d, 0d);
    if (nans.size() > 0) {
      MessageFormat form = new MessageFormat(bundle.getString("REPLACEMENT_OF_UNDEFINED_VALUES"));
      double[] limits = { 1, 2 };
      String[] valuePart = bundle.getString("VALUE_PARTS").split(";");
      String[] namePart = bundle.getString(name).split(";");
      String[] hasBeen = bundle.getString("HAS_BEEN_PARTS").split(";");
      form.setFormatByArgumentIndex(0, new ChoiceFormat(limits, valuePart));
      form.setFormatByArgumentIndex(1, new ChoiceFormat(limits, namePart));
      form.setFormatByArgumentIndex(2, new ChoiceFormat(limits, hasBeen));
      Integer count = Integer.valueOf(nans.size());
      GUITools.showListMessage(
        this,
        StringUtil.toHTML(
          form.format(new Object[] { count, count, count, Double.valueOf(value) }), 80),
          bundle.getString("REPLACING_UNDEFINED_VALUES"), nans);
    }
    panel.setOpaque(true);
    return panel;
  }

  /**
   * @param cl
   * @return
   */
  public boolean removeChangeListener(ChangeListener cl) {
    return listOfChangeListeners.remove(cl);
  }

  /* (non-Javadoc)
   * @see org.sbml.jsbml.util.TreeNodeChangeListener#nodeAdded(javax.swing.tree.TreeNode)
   */
  @Override
  public void nodeAdded(TreeNode node) {
    if (node instanceof SBase) {
      SBase sb = (SBase) node;
      tab.removeAll();
      init(sb.getModel());
    }
  }

  /* (non-Javadoc)
   * @see org.sbml.jsbml.util.TreeNodeChangeListener#nodeRemoved(org.sbml.jsbml.util.TreeNodeRemovedEvent)
   */
  @Override
  public void nodeRemoved(TreeNodeRemovedEvent evt) {
    TreeNode node = evt.getSource();
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
    SBPreferences prefs = SBPreferences.getPreferencesFor(SimulationOptions.class);
    defaultCompartmentValue = prefs.getDouble(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE);
    defaultSpeciesValue = prefs.getDouble(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE);
    defaultParameterValue = prefs.getDouble(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE);
    updateUI();
  }

  /* (non-Javadoc)
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof JSpinner) {
      String id = ((JSpinner) e.getSource()).getName();
      if (id != null) {
        checkButton(id);
      }
    } else {
      checkButton();
    }
    for (ChangeListener cl : listOfChangeListeners) {
      cl.stateChanged(e);
    }
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
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
    if (quantities[index].getValue() != ((Number) spinQuantity[index].getValue()).doubleValue()) {
      spinQuantity[index].setValue(Double.valueOf(quantities[index].getValue()));
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
    Integer key = quantitiesHash.get(id);
    if (key != null) {
      quantities[key.intValue()].setValue(value);
    }
  }

}
