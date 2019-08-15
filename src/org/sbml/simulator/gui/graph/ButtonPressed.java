package org.sbml.simulator.gui.graph;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;

/**
 * This class deals with the Buttons of the GUI of {@link TestCycleArranging}. The given Inputs are .txt files of 
 * ReactionGlyphs and SpeciesGlyphs
 * 
 * @author Lea Buchweitz
 */
public class ButtonPressed implements ActionListener {

  public List<String> elements = new LinkedList<String>();
  public File file;

  @Override
  public void actionPerformed(ActionEvent e) 
  {
    JFileChooser fileChooser = new JFileChooser();
    FileReader fr;
    BufferedReader br;

    if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 
    {
      file = fileChooser.getSelectedFile();
      try {
        fr = new FileReader(file);
        br = new BufferedReader(fr);

        String line;
        while((line = br.readLine()) != null) 
        {
          elements.add(line);
        }
      } catch (IOException e2) 
      {
        e2.printStackTrace();
      }

      TestCycleArranging.allInfos++;
    }
  }

}
