package org.sbml.simulator.gui.graph;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import org.sbml.jsbml.ext.layout.Point;
import org.sbml.simulator.gui.graph.DynamicView.Manipulators;
import org.simulator.math.odes.MultiTable;

import de.zbit.graph.sbgn.FillLevelNodeRealizer;
import de.zbit.graph.sbgn.ShapeNodeRealizerSupportingCloneMarker;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.prefs.SBPreferences;
import y.base.Node;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;

/**
 * class that reads out the {@link GraphOptions.CAMERA_WAY_FILE} .csv file with the animation data for the
 * camera and calculates the animation for the next simulation or export of a video
 *
 * @author Lea Buchweitz
 *
 */

/*
 * .csv File: Coordinates in one line, coordinates separated by "," elements separated by tab
 *  1) Zoom of camera
 *  2) Start point of animation
 *  3)... All points on the animation way
 *  last: End point of animation
 */
public class CameraAnimationView implements IDynamicGraph {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(CameraAnimationView.class.getName());

  /**
   * Displayed 2D graph which is set in {@link DynamicCore} when camera animation should be displayed
   */
  private static Graph2D graph;

  /**
   * saves all points for camera animation way
   */
  private List<Point> points = new LinkedList<Point>();

  /**
   * core to unset this view
   */
  protected DynamicCore core;

  /**
   * current .csv file to check if a new one was loaded
   */
  private String currentCSV;

  /**
   * zoom amount to set back
   */
  private double DEFAULT_ZOOM = 0.060361216730038025;

  /**
   * zoom level of the .csv file
   */
  private double ZOOM_FROM_FILE;

  /**
   * position in the beginning of the simulation to set back
   */
  private java.awt.Point DEFAULT_POSITION;

  /**
   * number of timesteps for one point to another, dependent on the number of points of the camera animation
   */
  private double timeFromPointToPoint;

  /**
   * maxTime of simulation
   */
  private double maxTime;

  /**
   * counter for the correct points
   */
  private int counter = 0;

  /**
   * to which pixel size the nodes are changed during the overview animation
   */
  protected int overviewSize = 155;

  /**
   * list with all timesteps when the way of the camera animation changes to another point
   */
  private List<Double> moveTowardsNextPoint = new LinkedList<Double>();

  private static List<ShapeNodeRealizer> activeRealizerList = new LinkedList<ShapeNodeRealizer>();

  /**
   * prefs to get all infos about what to display (i.e. loop {@link DynamicCore} for longer simulation)
   */
  private SBPreferences prefs = SBPreferences.getPreferencesFor(GraphOptions.class);

  public CameraAnimationView() {

    currentCSV = GraphOptions.CAMERA_WAY_FILE.getValue(
      SBPreferences.getPreferencesFor(GraphOptions.class)).toString();

    if(!currentCSV.equals(GraphOptions.CAMERA_WAY_FILE.getDefaultValue().toString()))
    {
      points.clear();
      points = getPointsFromCSV(prefs.getFile(GraphOptions.CAMERA_WAY_FILE));
    }
  }

  @Override
  public void donePlay() {
  }

  // Does not exist for this view, just for animation
  @Override
  public BufferedImage takeGraphshot(int width, int height) {
    return null;
  }

  @Override
  public void updateGraph(double timepoint, MultiTable updateThem) {

    if(!core.playAgain) {

      if (graph.getViews().current() instanceof Graph2DView) {

        if(((Graph2DView) graph.getViews().current()).getZoom() != ZOOM_FROM_FILE) {
          ((Graph2DView) graph.getViews().current()).setZoom(ZOOM_FROM_FILE);
        }

        if(timepoint >= moveTowardsNextPoint.get(counter)) {
          if(moveTowardsNextPoint.size() >= (counter+2)) {
            counter++;
          } else {
            setCounterBack();
            return;
          }
        }

        Point newPos = interpolatePosition(timepoint, points.get(counter), points.get(counter+1));
        ((Graph2DView) graph.getViews().current()).setViewPoint((int)newPos.x(), (int)newPos.y());
      }
    }
  }

  public void setGraph(Graph2D graph) {
    this.graph = graph;
  }

  public Graph2D getGraph() {
    return graph;
  }

  /**
   * sets the view of the camera animation to the given parameter in the csv
   */
  public void setZoom() {
    //System.out.println(((Graph2DView) graph.getViews().current()).getZoom());
    ((Graph2DView) graph.getViews().current()).setZoom(ZOOM_FROM_FILE);
  }

  public void setTimeLine(double maxTime) {
    this.maxTime = maxTime;
    moveTowardsNextPoint = calculateWindowPositions(maxTime, points);
  }

  public void setCore(DynamicCore core) {
    this.core = core;
  }

  public void setDefaultPosition(Graph2D graph) {
    DEFAULT_POSITION = ((Graph2DView) graph.getViews().current()).getViewPoint();
    DEFAULT_ZOOM = ((Graph2DView) graph.getViews().current()).getZoom();
  }

  public void setCounterBack() {
    counter = 0;
  }

  /**
   * gets all timeSteps which will be fired from {@link DynamicCore} and calculates which point has to be
   * reached at what timeStep in the camera animation
   *
   * @param timePoint
   * @param points
   */
  private List<Double> calculateWindowPositions(double maxTime, List<Point> points) {
    timeFromPointToPoint = maxTime / (points.size()-1);

    List<Double> timePoints = new LinkedList<Double>();
    while(maxTime > 0) {
      timePoints.add(maxTime);
      maxTime -= timeFromPointToPoint;
    }

    Collections.reverse(timePoints);

    return timePoints;
  }

  /**
   * calulates the position of the window of each timepoint, given a start and an end point to interpolate
   * between
   *
   * @param currentStep
   * @param start
   * @param end
   * @return
   */
  private Point interpolatePosition(double currentStep, Point start, Point end) {
    Point direction = new Point(end.x()-start.x(),end.y()-start.y());
    return new Point(Math.round(start.x()+(((currentStep%timeFromPointToPoint)/
        timeFromPointToPoint)*direction.x())),
      Math.round(start.y()+(((currentStep%timeFromPointToPoint)/
          timeFromPointToPoint)*direction.y())));
  }

  /**
   * checks if another csv file was loaded and gets the point from it
   */
  public void checkForNewFile() {
    // if csv file is not the one loaded and not the default value
    if(!GraphOptions.CAMERA_WAY_FILE.getValue(SBPreferences.getPreferencesFor(GraphOptions.class)).
        toString().equals(currentCSV)
        && !GraphOptions.CAMERA_WAY_FILE.getValue(SBPreferences.getPreferencesFor(GraphOptions.class)).
        toString().equals(GraphOptions.CAMERA_WAY_FILE.getDefaultValue().toString()))
    {
      points.clear();
      points = getPointsFromCSV(prefs.getFile(GraphOptions.CAMERA_WAY_FILE));
      setTimeLine(maxTime);
      counter = 0;
      currentCSV = GraphOptions.CAMERA_WAY_FILE.getValue(
        SBPreferences.getPreferencesFor(GraphOptions.class)).toString();
    }
  }

  /**
   * gets the zoom factor and the points of the given csv file
   * @param csv
   * @return
   */
  public List<Point> getPointsFromCSV(File csv) {
    LinkedList<String> tmpCSVData = new LinkedList<String>();
    if(SBFileFilter.isCSVFile(csv)) {
      try {
        Scanner inputStream = new Scanner(csv);
        while(inputStream.hasNext()) {
          tmpCSVData.add(inputStream.next());
        }
        inputStream.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return null;
      }
    }

    if(!tmpCSVData.isEmpty()) {
      ZOOM_FROM_FILE = Double.parseDouble(tmpCSVData.removeFirst());

      while(!tmpCSVData.isEmpty()) {
        String[] parts = tmpCSVData.removeFirst().split(",");
        points.add(new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
      }
    }
    return points;
  }


  private double[] resetMinMaxSize;
  private static double[] resetSize = new double[] { -1 , -1};
  private static List<double[]> listOfResetSizes = new LinkedList<double[]>();
  private Collection<NodeRealizer> collection;
  private Collection<FillLevelNodeRealizer> collectionFillLevel;

  /**
   * shows the first loop of simulation just as an overview which metabolites are included
   * and where they are
   */
  protected void showOverviewAnimation() {

    activeRealizerList.clear();
    listOfResetSizes.clear();

    graph.fitGraph2DView();

    String visualizationMode = SBPreferences.getPreferencesFor(GraphOptions.class).get(GraphOptions.VISUALIZATION_STYLE);
    Manipulators currentManipulator = Manipulators.getManipulator(visualizationMode);

    collection = null;
    collectionFillLevel = null;

    if (currentManipulator.name().equals("FILL_LEVEL")) {
      Map<Node,FillLevelNodeRealizer> node2FillRealizerClone = AGraphManipulator.node2FillRealizer;
      ((HashMap<Node,FillLevelNodeRealizer>) node2FillRealizerClone).clone();
      collectionFillLevel = node2FillRealizerClone.values();
    } else {
      Map<Node,NodeRealizer> node2RealizerClone = AGraphManipulator.node2Realizer;
      ((HashMap<Node,NodeRealizer>) node2RealizerClone).clone();
      collection = node2RealizerClone.values();
    }

    if(collectionFillLevel != null) {
      for(NodeRealizer nr : collectionFillLevel) {
        if(nr instanceof FillLevelNodeRealizer) {
          activeRealizerList.add((ShapeNodeRealizer)nr);
          if((resetSize[0] == -1) && (resetSize[1] == -1)) {
            resetSize = ((FillLevelNodeRealizer) nr).getSize();
          }
        }
        nr.setSize(overviewSize, overviewSize);
      }
    } else {
      for(NodeRealizer nr : collection) {
        if(nr instanceof ShapeNodeRealizerSupportingCloneMarker) {
          activeRealizerList.add((ShapeNodeRealizer)nr);

          if(currentManipulator.name().equals("NODECOLOR_AND_SIZE") ||
              currentManipulator.name().equals("NODESIZE") ||
              currentManipulator.name().equals("NODESIZE_AND_COLOR")) {
            listOfResetSizes.add(((ShapeNodeRealizerSupportingCloneMarker) nr).getSize());
          } else {
            if((resetSize[0] == -1) && (resetSize[1] == -1)) {
              resetSize = ((ShapeNodeRealizerSupportingCloneMarker) nr).getSize();
            }
          }
        }
        nr.setSize(overviewSize, overviewSize);
      }
    }
    graph.updateViews();
  }

  /**
   * resizes the nodes for the second loop of simulation
   */
  public static void resizeNodes() {

    if(listOfResetSizes.isEmpty()) {
      for(ShapeNodeRealizer nr : activeRealizerList) {
        nr.setSize(resetSize[0],resetSize[1]);
      }
    } else {
      for(int i = 0 ; i < activeRealizerList.size() ; i++) {
        activeRealizerList.get(i).setSize(listOfResetSizes.get(i)[0], listOfResetSizes.get(i)[1]);
      }
    }
    graph.updateViews();
  }

  /**
   * resets the simulation to another loop start
   */
  public void resetToNextSimulationLoop() {

    core.playAgain = true;
  }

  /**
   * resets all values back so that the animation can start again
   */
  protected void resetCameraAnimation() {
    counter = 0;

    if(!prefs.getBoolean(GraphOptions.CYCLE_SIMULATION)) {
      ((Graph2DView) graph.getViews().current()).setZoom(DEFAULT_ZOOM);
      if(DEFAULT_POSITION == null) {
        graph.fitGraph2DView();
      } else {
        ((Graph2DView) graph.getViews().current()).setViewPoint((int)DEFAULT_POSITION.getX(), (int)DEFAULT_POSITION.getY());
      }
    }

    graph.updateViews();
  }

}
