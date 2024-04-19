package mrview;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRGraphics {

    public static MRGraphics last;
    private final static double PADDING = 10;
    public Graphics2D g;
    private BoundingBox bb;
    private double xs, ys;
    public int textline = 0;
    
    private MRGraphics() {
    }

    public MRGraphics(Graphics _g, int xs, int ys) {
        this.g = (Graphics2D) _g;
//        g.translate(0, ys);
//        g.scale(1, -1);
        this.xs = xs - PADDING * 2;
        this.ys = ys - PADDING * 2;
        last = this;
        bb = new BoundingBox();
        bb.update(new Seg(0, 0, 1000, 1000));
    }
    
    public Point positionInBox(Point p) {
        double x, y;

        if (p == null) {
            System.out.println("BB IS NULL!");
            return p;
        }
        
        double ratio  = xs / (bb.ur.x - bb.ll.x);
        double ratio2 = ys / (bb.ur.y - bb.ll.y);
        if (ratio2 < ratio)
            ratio = ratio2;
        
        x = (p.x - bb.ll.x) * ratio + PADDING;
        y = (bb.ur.y - p.y) * ratio + PADDING;
//        y = (p.y - bb.s.y) * ratio + PADDING;

        return new Point(x, y);
    }
    
    public AffineTransform getAffineTransform() {
        double ratio  = xs / (bb.ur.x - bb.ll.x);
        double ratio2 = ys / (bb.ur.y - bb.ll.y);
        if (ratio2 < ratio)
            ratio = ratio2;
        
        double dx = - bb.ll.x;
        double dy = bb.ur.y;
        
        AffineTransform at = new AffineTransform();
        at.translate(PADDING, PADDING);
        at.scale(ratio, ratio);
        at.translate(dx, dy);
        at.scale(1, -1);
        
        return at;
    }
    
    public Point scaleInBox(Point p) {
        double x, y;

        if (p == null) {
            System.out.println("BB IS NULL!");
            return p;
        }
                
        double ratio  = xs / (bb.ur.x - bb.ll.x);
        double ratio2 = ys / (bb.ur.y - bb.ll.y);
        if (ratio2 < ratio)
            ratio = ratio2;
        
        x = p.x * ratio;
        y = p.y * ratio;
//        y = (p.y - bb.s.y) * ratio + PADDING;

        return new Point(x, y);
    }

    public Point positionInModel(Point p) {
        double x, y;
        
        if (bb == null) {
            System.out.println("BB IS NULL!");
            return p;
        }

        double ratio  = xs / (bb.ur.x - bb.ll.x);
        double ratio2 = ys / (bb.ur.y - bb.ll.y);
        if (ratio2 < ratio)
            ratio = ratio2;
        
        x = ((p.x - PADDING) / ratio) + bb.ll.x;
        y = bb.ur.y - ((p.y - PADDING) / ratio);

        return new Point(x, y);
    }

    public void drawLine(Seg seg, Color c, float width, float dashlen) {
        g.setColor(c);
        Point s = positionInBox(seg.s);
        Point e = positionInBox(seg.e);
        Stroke oldstroke = g.getStroke();
        if (dashlen > 0) {
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {dashlen}, 0));
        } else {
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f));
        }
        g.drawLine((int) s.x, (int) s.y, (int) e.x, (int) e.y);
        g.setStroke(oldstroke);
    }
    
    public void drawArrow(Seg seg, Color c, float width, float dashlen) {
        drawLine(seg, c, width, dashlen);
        
    }
    
    public void drawLine(Seg seg, Color c) {
        drawLine(seg, c, 1, 0);
    }

    public void drawRect(Point _pt, double x, double y, Color c) {
        Point pt = positionInBox(_pt);
        Point pt2 = scaleInBox(new Point(x, y));
        Color p = g.getColor();
        g.setColor(c);
        g.fillRect((int) pt.x, (int) pt.y, (int) pt2.x+1, (int) pt2.y+1);
//        g.fillRect((int) pt.x, (int) pt.y, 5, 5);
        g.setColor(p);
    }
    
    public void drawPoint(Point _pt, double radius, Color c) {
        Point pt = positionInBox(_pt);
        Color p = g.getColor();
        g.setColor(c);
        if (radius > 0) {
            g.fillOval((int) Math.round(pt.x - radius), (int) Math.round(pt.y - radius),
                    (int) Math.round(radius * 2), (int) Math.round(radius * 2));
        } else {
            g.drawLine((int) Math.round(pt.x), (int) Math.round(pt.y),
                    (int) Math.round(pt.x), (int) Math.round(pt.y));
        }
        g.setColor(p);
    }
    
    public void drawPoint(Point _pt, double radius) {
        drawPoint(_pt, radius, Color.RED);
    }
    
    public void drawPoint(Point _pt) {
        drawPoint(_pt, 5, Color.RED);
    }
    
    public void drawBoundingBox (BoundingBox bb) {
        BoundingBox bb2 = new BoundingBox();
        bb2.update(positionInBox(bb.ll));
        bb2.update(positionInBox(bb.ur));
        Point ll = bb2.ll;
        Point ur = bb2.ur;
        g.drawRect((int)ll.x, (int)ll.y, (int)(ur.x-ll.x), (int)(ur.y-ll.y));
    }

    public void drawFace(Face f, boolean open) {
        List<Point> pts = new LinkedList();
        
        for (Seg s : f.getSegments()) {
            pts.add(s.s);
        }
        drawPolygon(pts, Color.LIGHT_GRAY, 0);
    }

    public void drawPolygon(List<Point> pts, Color border, Color fill, int bordersize, double transparent) {
        Polygon poly = new Polygon();
        for (Point pt : pts) {
            Point p = positionInBox(pt);
            poly.addPoint((int) p.x, (int) p.y);
        }        
        
        Stroke oldstroke = g.getStroke();
        if (transparent > 0) {
            g.setComposite(makeComposite(transparent));
        }
        g.setStroke(new BasicStroke(bordersize));
        if (fill != null) {
            g.setColor(fill);
            g.fillPolygon(poly);
        }
        if (border != null) {
            g.setColor(border);
            g.drawPolygon(poly);
        }
        g.setStroke(oldstroke);
    }
    
    public void drawShape(Shape s, Color c) {
        AffineTransform at = g.getTransform();
        g.setTransform(getAffineTransform());
        Color pc = g.getColor();
        g.setColor(c);
        g.fill(s);
        g.setColor(pc);
        g.setTransform(at);
    }
    
    public void drawPolygon(List<Point> pts, Color c, double transparent) {
        g.setColor(c);
        if (transparent > 0) {
            g.setComposite(makeComposite(transparent));
        }

        Polygon poly = new Polygon();
        for (Point pt : pts) {
            Point p = positionInBox(pt);
            poly.addPoint((int) p.x, (int) p.y);
        }
        Stroke oldstroke = g.getStroke();
        if (c != Color.BLACK) {
//            g.fillPolygon(poly);
        } else {
            g.setStroke(new BasicStroke(3));
        }
        if (transparent < 1.0)
            g.fillPolygon(poly);
        g.drawPolygon(poly);
        g.setStroke(oldstroke);
    }

    public void drawPolygon(List<Point> pts, boolean hole) {
        if (hole) {
            drawPolygon(pts, Color.WHITE, 0);
        } else {
            Random rand = new Random();
            float r = rand.nextFloat();
            float g = rand.nextFloat();
            float b = rand.nextFloat();
            Color rc = new Color(r, g, b);
            rc = Color.BLUE;
            drawPolygon(pts, rc, 0.9);
        }
    }
    
    public void drawString (String text) {
        g.drawString(text, 10, 20+textline*15);
        textline++;
    }
    
    public void drawString (Point _pt, String text, Color c) {
        Point pt = positionInBox(_pt);
        Color p = g.getColor();
        g.setColor(c);
        g.drawString(text, (int)pt.x, (int)pt.y);
        g.setColor(p);
    }

    public BoundingBox getBoundingBox() {
        return bb;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        bb = boundingBox;
        if (bb == null || bb.ur == null || bb.ll == null) {
            bb = new BoundingBox(0, 0, 1000, 1000);
            return;
        }
        double ratio  = xs / (bb.ur.x - bb.ll.x);
        double ratio2 = ys / (bb.ur.y - bb.ll.y);
        if (ratio2 < ratio)
            ratio = ratio2;
        
        bb.ur.x = bb.ll.x + xs/ratio;
        bb.ur.y = bb.ll.y + ys/ratio;
    }

    private AlphaComposite makeComposite(double alpha) {
        int type = AlphaComposite.SRC_OVER;
        return (AlphaComposite.getInstance(type, (float) alpha));
    }
}
