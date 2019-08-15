package org.sbml.simulator.gui.graph;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;

import de.zbit.io.OpenedFile;
import de.zbit.sbml.gui.SBMLWritingTask;
import de.zbit.util.ResourceManager;

/**
 * This class reads out all the objects which are given from a .txt file. All given objects are either
 * ReactionGlyphs or SpeciesGlyphs which are involved in a cycle.
 * If the objects are arranged, a new .xml document is saved to use with SBMLsimulator
 *
 * @author Lea Buchweitz
 */

public class TestCycleArranging {

  public static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.gui.graph.DynamicGraph");
  public static int allInfos = 0;
  public static SBMLDocument data;
  public static File file;

  public static void main(String[] args) throws XMLStreamException, IOException
  {
    file = new File(args[0]);
    data = SBMLReader.read(file);

    //new YGraphView(data.getSBMLDocument());

    TestCycleArranging start = new TestCycleArranging();
    start.getInfoAboutCycles();

  }

  private void getInfoAboutCycles() {

    ButtonPressed buttonPressed = new ButtonPressed();
    String circleRad = bundle.getString("RADIUS_OF_CIRCLE");

    JLabel pathReactions = new JLabel(bundle.getString("FILE_WITH_REACTIONNODES"));
    JButton searchReactions = new JButton("...");
    searchReactions.addActionListener(buttonPressed);

    JLabel pathSpecies = new JLabel(bundle.getString("FILE_WITH_SPECIES"));
    JButton searchSpecies = new JButton("...");
    searchSpecies.addActionListener(buttonPressed);

    JTextField radius = new JTextField();
    radius.setText(circleRad);

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    JFrame window = new JFrame();

    window.setSize(300,200);
    window.setTitle("Zykel Infos");
    window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    JButton ok = new JButton("OK");
    ok.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        List<String> allElements = new LinkedList<String>();
        double givenRadius = 0;
        if((allInfos == 2) && !radius.getText().equals(circleRad)) {
          allElements = buttonPressed.elements;
          givenRadius = Double.parseDouble(radius.getText());
          window.dispose();
          try {
            startRedrawingData(allElements, givenRadius);
          } catch (FileNotFoundException e1) {
            e1.printStackTrace();
          }
        }
      }
    });

    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    c.gridy = 0;
    panel.add(pathReactions, c);
    c.gridx = 0;
    c.gridy = 1;
    panel.add(pathSpecies, c);
    c.gridx = 1;
    c.gridy = 0;
    panel.add(searchReactions, c);
    c.gridx = 1;
    c.gridy = 1;
    panel.add(searchSpecies, c);
    c.gridx = 0;
    c.gridy = 2;
    panel.add(radius, c);
    c.fill = GridBagConstraints.CENTER;
    c.gridy = 3;
    panel.add(ok, c);

    window.add(panel, BorderLayout.CENTER);
    window.setVisible(true);

  }

  private void startRedrawingData(List<String> allElements, double radius) throws FileNotFoundException
  {
    Model model = data.getModel();

    LayoutModelPlugin plugin = (LayoutModelPlugin) model.getPlugin(LayoutConstants.shortLabel);

    ListOf<Layout> listOfLayouts = plugin.getListOfLayouts();

    Layout layout;

    for (int i=0; i < listOfLayouts.getChildCount(); i++)
    {
      layout = listOfLayouts.get(i);
      new RearrangeCycles(layout,allElements,radius);
      //new YGraphView(data.getSBMLDocument());

      // object keeps same Name
      SBMLWritingTask newSBML =
          new SBMLWritingTask(new OpenedFile(new File("C:/Users/Lea.B/Documents/Studium/Bachelorarbeit/Libs&Modelle/PLT_teilweise_optimiert.xml"), data),null);
      // oder: new OpenedFile(args[1],...);
      //newSBML.execute();
    }
  }

}
