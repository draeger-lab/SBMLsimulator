package org.sbml.simulator.gui.graph;

import java.util.ArrayList;
import java.util.List;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.CubicBezier;
import org.sbml.jsbml.ext.layout.Curve;
import org.sbml.jsbml.ext.layout.CurveSegment;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.GraphicalObject;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceRole;
import org.sbml.jsbml.ext.layout.TextGlyph;

/**
 * This class arranges all Species and its TextGlyphs in a correct circle which are involved in a cycle.
 * All curveSegments are deleted and new BezierCurves are calculated
 *
 * @author Lea Buchweitz
 */

public class RearrangeCycles implements Runnable{

  private List<GraphicalObject> allGraphicalElements;
  private List<BoundingBox> nodes;
  //private List<Object> startEndPoints;
  private List<CubicBezier> basePointsCycle;
  private List<CubicBezier> basePointsSides;
  private List<String> allElements;

  private Model model;
  private Layout layout;
  private Point centerOfCircle;

  private double radius;
  private double speciesSize;
  private double reactionSize;

  public RearrangeCycles(Layout layout, List<String> allElements, double radius) {

    allGraphicalElements = new ArrayList<GraphicalObject>();
    //startEndPoints = new ArrayList<Object>();
    nodes = new ArrayList<BoundingBox>();
    basePointsCycle = new ArrayList<CubicBezier>();
    basePointsSides = new ArrayList<CubicBezier>();

    // ist unn�tig, allGraphicalElements hat dieselben infos, nur nicht NUR als ID
    this.allElements = allElements;
    this.layout = layout;
    this.radius = radius;
    model = layout.getModel();

    for(String id : allElements)
    {
      SBase elem = model.findUniqueNamedSBase(id);
      if((elem != null) && (elem instanceof GraphicalObject))
      {
        allGraphicalElements.add((GraphicalObject) elem);
      }
    }
    run();
  }

  @Override
  public void run() {

    List<String> objWithText = new ArrayList<String>();

    // get text elements
    for(GraphicalObject obj : allGraphicalElements)
    {
      nodes.add(obj.getBoundingBox());

      if(obj instanceof ReactionGlyph) {
        objWithText.add(((ReactionGlyph) obj).getId());
        reactionSize = ((ReactionGlyph) obj).getBoundingBox().getDimensions().getWidth();
      }
      if(obj instanceof SpeciesGlyph) {
        objWithText.add(((SpeciesGlyph) obj).getId());
        speciesSize = ((SpeciesGlyph) obj).getBoundingBox().getDimensions().getWidth();
      }
    }

    centerOfCircle = calculateCircleCenter(allGraphicalElements, radius);

    List<Point> translateVectors = calculatePositionsOnCircle(nodes, centerOfCircle, radius);

    System.out.println("NEUE ANORDNUNG RUM");

    for(GraphicalObject obj : allGraphicalElements)
    {
      if(obj instanceof ReactionGlyph)
      {
        Point start = ((ReactionGlyph) obj).getBoundingBox().getPosition();

        ListOf<SpeciesReferenceGlyph> speciesRefList = ((ReactionGlyph) obj).getListOfSpeciesReferenceGlyphs();

        Curve curve = new Curve();

        // deletes every linesegment or curve from speciesreferences
        for(int i=0; i < speciesRefList.getChildCount(); i++)
        {
          curve = speciesRefList.get(i).getCurve();
          for(int n = curve.getCurveSegmentCount()-1; n >= 0; n--) {
            curve.removeCurveSegment(n);
          }
        }

        // traverse all species which are part of that reaction
        for(int i=0; i < speciesRefList.getChildCount(); i++)
        {
          boolean switchPoints = false;

          SBase species = model.findUniqueNamedSBase(speciesRefList.get(i).getSpeciesGlyph());
          Point end = ((SpeciesGlyph) species).getBoundingBox().getPosition();

          // if current speciesGlyph is part of the cycle
          if(allElements.contains(speciesRefList.get(i).getSpeciesGlyph()))
          {
            // if the speciesGlyph is the substrate of the reaction the beziercurve will
            // be drawn from there to the reactionnode
            if(speciesRefList.get(i).getRole() == SpeciesReferenceRole.SUBSTRATE) {
              switchPoints = true;
            }

            CubicBezier cubicBez = new CubicBezier(speciesRefList.get(i).getLevel(), speciesRefList.get(i).getVersion());
            if(switchPoints) // species is start, reaction is end
            {
              Point direction = new Point(start.x()-end.x(), start.y()-end.y(),start.z());
              double norm = 1/(Math.sqrt(Math.pow(direction.x(), 2)+Math.pow(direction.y(), 2)));

              cubicBez.createEnd((start.x()+(reactionSize/2))-(direction.x()*norm*15),
                (start.y()+(reactionSize/2))-(direction.y()*norm*15), start.z());
              cubicBez.createStart(end.x()+(speciesSize/2), end.y()+(speciesSize/2), end.z());
              cubicBez.createBasePoint2(-((start.y()-centerOfCircle.y())),
                ((start.x()-centerOfCircle.x())), start.z());
              cubicBez.createBasePoint1((end.y()-centerOfCircle.y()),
                -((end.x()-centerOfCircle.x())), end.z());
              //System.out.println("Bezier f�r Substrat " + speciesRefList.get(i));

            } else // species is end, reaction is start
            {
              Point direction = new Point(end.x()-start.x(),end.y()-start.y(),start.z());
              double norm = 1/(Math.sqrt(Math.pow(direction.x(), 2)+Math.pow(direction.y(), 2)));

              cubicBez.createStart((start.x()+(reactionSize/2))-(direction.x()*norm*15),
                (start.y()+(reactionSize/2))-(direction.y()*norm*15), start.z());
              cubicBez.createEnd(end.x()+(speciesSize/2), end.y()+(speciesSize/2), end.z());
              cubicBez.createBasePoint1((start.y()-centerOfCircle.y()),
                -((start.x()-centerOfCircle.x())), start.z());
              cubicBez.createBasePoint2(-((end.y()-centerOfCircle.y())),
                (end.x()-centerOfCircle.x()), end.z());
              //System.out.println("Bezier f�r Produkt " + speciesRefList.get(i));
            }
            curve = speciesRefList.get(i).getCurve();
            curve.addCurveSegment(cubicBez);
            basePointsCycle.add(cubicBez);

            if(speciesRefList.get(i).getSpeciesGlyph().equals("_1576570")) {
              //System.out.println("Ich " + speciesRefList.get(i).getSpeciesGlyph() +" habe "+speciesRefList.get(i).getCurve().getCurveSegmentCount());
            }
          }
        }
      }
    }

    calculateBezierBasePoints(basePointsCycle, centerOfCircle, radius);

    ReactionGlyph reac = new ReactionGlyph();

    for(GraphicalObject obj : allGraphicalElements)
    {
      if(obj instanceof ReactionGlyph)
      {
        reac = (ReactionGlyph) obj;

        Point start = ((ReactionGlyph) obj).getBoundingBox().getPosition();

        ListOf<SpeciesReferenceGlyph> speciesRefList = ((ReactionGlyph) obj).getListOfSpeciesReferenceGlyphs();

        Curve curve = new Curve();

        // gets new starting point of the sidesubstrates and sideproducts
        for(int i=0; i < speciesRefList.getChildCount(); i++)
          //evtl getspeciesreferenceglyphcount?!?! (-> reactionglyph)
        {
          double thirdX = 0;
          double thirdY = 0;

          SpeciesReferenceRole srr = SpeciesReferenceRole.UNDEFINED;

          Point end = new Point();

          if(speciesRefList.get(i).getRole() == SpeciesReferenceRole.SIDESUBSTRATE)
          {
            srr = SpeciesReferenceRole.SUBSTRATE;
          }

          if(speciesRefList.get(i).getRole() == SpeciesReferenceRole.SIDEPRODUCT) {
            srr = SpeciesReferenceRole.PRODUCT;
          }

          // search in whole speciesreferencelist for the substrate of sidesubstrate
          for(int k = speciesRefList.getChildCount()-1 ; k >= 0 ; k--)
          {
            if((speciesRefList.get(k).getRole() == srr) && (srr != SpeciesReferenceRole.UNDEFINED))
            {
              //System.out.println("srr muss produkt sein: " + srr+ " reaction ist: " + obj.getId());
              //System.out.println(srr+" ist "+speciesRefList.get(k)+ " zu "+speciesRefList.get(i));
              ListOf<CurveSegment> seg = speciesRefList.get(k).getCurve().getListOfCurveSegments();
              // Behelf f�r oben_links
              if(seg.size() != 0) {
                CubicBezier existingBez = (CubicBezier) seg.getFirst();

                // get sidesubstrate position
                SBase species = model.findUniqueNamedSBase(speciesRefList.get(i).getSpeciesGlyph());
                end = ((SpeciesGlyph) species).getBoundingBox().getPosition();

                double t = 0.2;

                // calculate point on bezier curve
                thirdX = (Math.pow((1-t),3)*existingBez.getStart().x())+
                    (3*Math.pow((1-t),2)*t*existingBez.getBasePoint1().x())+
                    (3*(1-t)*Math.pow(t,2)*existingBez.getBasePoint2().x())+
                    (Math.pow(t,3)*existingBez.getEnd().x());
                thirdY = (Math.pow((1-t),3)*existingBez.getStart().y())+
                    (3*Math.pow((1-t),2)*t*existingBez.getBasePoint1().y())+
                    (3*(1-t)*Math.pow(t,2)*existingBez.getBasePoint2().y())+
                    (Math.pow(t,3)*existingBez.getEnd().y());

                CubicBezier cubicBez = new CubicBezier(speciesRefList.get(i).getLevel(), speciesRefList.get(i).getVersion());

                if(speciesRefList.get(i).getRole() == SpeciesReferenceRole.SIDESUBSTRATE)
                {
                  cubicBez.createEnd(thirdX, thirdY, 0);
                  cubicBez.createStart(end.x()+(speciesSize/2), end.y()+(speciesSize/2), end.z());
                } else
                {
                  cubicBez.createStart(thirdX, thirdY, 0);
                  cubicBez.createEnd(end.x()+(speciesSize/2), end.y()+(speciesSize/2), end.z());
                }

                cubicBez.createBasePoint1(end.y()-thirdY, -(end.x()-thirdX), start.z());
                cubicBez.createBasePoint2(-(end.y()-thirdY), end.x()-thirdX, end.z());
                curve = speciesRefList.get(i).getCurve();
                curve.addCurveSegment(cubicBez);
                basePointsSides.add(cubicBez);
              } // neue if klammer
            }
          }
        }
      }
    }

    calculateBezierBasePoints(basePointsSides, centerOfCircle, radius);

    calculateTextGlyphPositions(objWithText, translateVectors, layout.getListOfTextGlyphs(), radius);

    //YGraphView windowView = new YGraphView(model.getSBMLDocument());

  }

  // calculates the center of all speciesGlyphs and reactionGlyphs
  private Point calculateCircleCenter(List<GraphicalObject> points , double radius)
  {
    double centerX = 0;
    double centerY = 0;

    for (int i = 0; i < points.size(); i++) {
      GraphicalObject obj = points.get(i);
      BoundingBox box = obj.getBoundingBox();
      Point p = box.getPosition();

      centerX += p.getX();
      centerY += p.getY();
    }

    centerX = centerX / points.size();
    centerY = centerY / points.size();

    return new Point(centerX, centerY, 0);
  }

  // transforms all speciesGlyphs and reactionGlyphs on the circle + half of BoundingBox
  private List<Point> calculatePositionsOnCircle(List<BoundingBox> points, Point center, double radius)
  {
    List<Point> translateVectors = new ArrayList<Point>();
    Point p;
    BoundingBox box = new BoundingBox();
    boolean isNode = false;

    for (BoundingBox obj : points) {
      box = obj;
      p = box.getPosition();
      isNode = true;

      double diffX = p.getX() - center.getX();
      double diffY = p.getY() - center.getY();
      double diffLength = Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffY, 2));


      if(isNode)
      {
        p.setX((((diffX / diffLength) * radius) + center.getX()) - middleOfElemX(box.getDimensions()));
        p.setY((((diffY / diffLength) * radius) + center.getY()) - middleOfElemY(box.getDimensions()));

        translateVectors.add(new Point(diffX,diffY,diffLength));
      }
    }

    return translateVectors;
  }

  // gets width of a speciesGlyph or reactionGlyph
  private double middleOfElemX(Dimensions dim) {
    return (dim.getWidth()/2);
  }

  // gets height of a speciesGlyph or reactionGlyph
  private double middleOfElemY(Dimensions dim) {
    return (dim.getHeight()/2);
  }

  // translates the text to the circle as well
  private void calculateTextGlyphPositions(List<String> ids, List<Point> translate, ListOf<TextGlyph> textElements,
    double radius) {

    for(int i = 0; i < ids.size(); i++) {

      Point p = translate.get(i);

      for(int k = 0; k < textElements.size(); k++) {

        if(textElements.get(k).getGraphicalObject().toString().equals(ids.get(i))) {
          textElements.get(k).getBoundingBox().getPosition().setX(textElements.get(k)
            .getBoundingBox().getPosition().getX()+((p.getX()/p.getZ())*(radius-p.getZ())));
          textElements.get(k).getBoundingBox().getPosition().setY(textElements.get(k)
            .getBoundingBox().getPosition().getY()+((p.getY()/p.getZ())*(radius-p.getZ())));
          break;
        }
      }
    }
  }


  // calculates the controlpoints of the new beziercurve
  private void calculateBezierBasePoints(List<CubicBezier> cubicBeziers, Point center, double radius) {

    for(CubicBezier seg : cubicBeziers) {

      double startCenterX = seg.getStart().getX()-center.getX();
      double startCenterY = seg.getStart().getY()-center.getY();

      double endCenterX = seg.getEnd().getX()-center.getX();
      double endCenterY = seg.getEnd().getY()-center.getY();

      double a = (4/3)*Math.tan(0.25*Math.acos(((startCenterX * endCenterX) + (startCenterY * endCenterY))/(Math.sqrt(Math.pow(startCenterX, 2)+
        Math.pow(startCenterY, 2))* (Math.sqrt(Math.pow(endCenterX, 2)+
          Math.pow(endCenterY, 2))))));

      if(!Double.isNaN(a)) {

        seg.getBasePoint1().setX((seg.getStart().getX()+(a*startCenterY)));
        seg.getBasePoint1().setY((seg.getStart().getY()+(a*-startCenterX)));

        seg.getBasePoint2().setX((seg.getEnd().getX()+(a*-endCenterY)));
        seg.getBasePoint2().setY((seg.getEnd().getY()+(a*endCenterX)));

      } else {
        System.out.println("a ist: "+a);
      }
    }
  }
}
