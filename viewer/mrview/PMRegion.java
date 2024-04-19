package mrview;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class PMRegion extends MovingObject implements SecondoObject {
    private List<Triangle> triangles = new ArrayList();
    private boolean filled = true;
    private boolean transparent = false;
    private Seg bb;
    public Double min, max;

    @Override
    public String[] getOperations() {
        return new String[]{
            "ProjectXY"
        };
    }

    @Override
    public String[] getFlags() {
        return new String[]{
            "Filled",
            "Transparent"
        };
    }
    
    Region project (double t, BoundingBox bb) {
        SegSet ss = new SegSet();
        
        for (Triangle tri : triangles) {
            Seg s = tri.project(t);
            if (s != null)
                ss.add(s);
        }
        
        int faces = 0;
        Region reg = new Region();
        while (!ss.isEmpty()) {
            Seg prev = ss.getSomeSeg();
            Point start = prev.s, last = null;
            Face f = new Face();
            f.addPointRaw(prev.e);
            while (!ss.isEmpty()) {
                Seg cur = ss.getSuccessor(prev);
                if (cur == null) {
                    System.err.println("ERROR: No successor found2!");
                    break;
                }
            
                f.addPointRaw(cur.e);
                if (start.equals(cur.e)) {
                    f.close();
                    if (bb != null && f.getBoundingBox().overlaps(bb)) {
                        f.ccw();
                        reg.addFace(f);
                        faces++;
                    }
                    break;
                }
                prev = cur;
            }
        }
        reg.fixHoles(); // FIXME!
        
        return reg;
    }
    
    Region project (double t) {
        return project(t, null);
    }
    
    Region projectold (double t) {
        List<Seg> segs = new LinkedList();
        for (Triangle tri : triangles) {
            Seg s = tri.project(t);
            if (s != null)
                segs.add(s);
        }
        
        int faces = 0;
        Region reg = new Region();
        while (!segs.isEmpty()) {
            Seg prev = segs.get(0);
            Point start = prev.s, last = null;
            Face f = new Face();
            f.addPoint(prev.e);
            segs.remove(prev);
            while (!segs.isEmpty()) {
                boolean found = false;
                for (int i = 0; i < segs.size(); i++) {
                    Seg cur = segs.get(i);
                    if (cur.s.equals(prev.e)) {
                        f.addPoint(cur.e);
                        prev = cur;
                        segs.remove(cur);
                        found = true;
                        last = cur.e;
                    } else if (cur.e.equals(prev.e)) {
                        f.addPoint(cur.s);
                        prev = new Seg(cur.e, cur.s);
                        segs.remove(cur);
                        found = true;
                        last = cur.s;
                    }
                }
                if (!found) {
                    System.err.println("ERROR: No successor found!");
                    return null;
                }
                if (start.equals(last)) {
                    f.close();
                    reg.addFace(f);
                    faces++;
                    break;
                }
            }
        }
        reg.fixHoles();
        
        return reg;
    }
    
    public List<Seg> getSegments() {
        List<Seg> segs = new LinkedList();
        for (Triangle tri : triangles) {
            Seg s1 = new Seg(new Point(tri.points[0].x, tri.points[0].y),
                             new Point(tri.points[1].x, tri.points[1].y));
            Seg s2 = new Seg(new Point(tri.points[1].x, tri.points[1].y),
                             new Point(tri.points[2].x, tri.points[2].y));
            Seg s3 = new Seg(new Point(tri.points[2].x, tri.points[2].y),
                             new Point(tri.points[0].x, tri.points[0].y));
            segs.add(s1);
            segs.add(s2);
            segs.add(s3);
        }
        
        return segs;
    }
    
    private static List<Seg> intersectAndSplitSegments(List<Seg> segs) {
        int i, j;
        for (Seg s : segs)
            s.clearIntersectionPoints();
        for (i = 0; i < segs.size(); i++) {
            Seg s1 = segs.get(i);
            for (j = i+1; j < segs.size(); j++) {
                Seg s2 = segs.get(j);
                Point p = s1.intersection(s2);
                if (p != null) {
                    s1.addIntersectionPoint(p);
                    s2.addIntersectionPoint(p);
                }
            }
        }

        List<Seg> ret = new LinkedList();
        for (Seg s : segs) {
            List<Seg> split = s.splitSegmentAtIntersectionPoints();
            ret.addAll(split);
        }
        
        return ret;
    }
    
    private List<Face> reconstructFaces (List<Seg> segs) {
        List<Face> faces = new LinkedList();
        MultiMap<Point, Seg> points = new MultiMap();
        for (Seg s : segs) {
            points.put(s.s, s);
            Seg s2 = s.reverse();
            points.put(s2.s, s2);
        }
        do {
            Seg s = points.getSomeValue();
            if (s == null)
                break;
            points.remove(s);
            Point start = s.s;
            
            Face f = new Face();
            f.addPointRaw(start);
            Seg chosen;
            do {
                chosen = null;
                Set<Seg> candidates = points.get(s.e);
                if (candidates == null)
                    break;
                double angle = 0;
                for (Seg s2 : candidates) {
                    if (s.s.equals(s2.e)) {
                        continue;
                    }
                    double tmp = s.getAngle(s2);
                    if (tmp >= 180)
                        continue;
                    if (chosen == null || tmp > angle) {
                        angle = tmp;
                        chosen = s2;
                    }
                }
                if (chosen == null)
                    break;
                f.addPointRaw(chosen.s);
                points.remove(chosen);
                s = chosen;
            } while (!chosen.e.equals(start));
            if (chosen != null) {
                f.close();
                faces.add(f);
            }
        } while (!points.empty());
        
        List<Face> holes = new LinkedList();
        for (Face f1 : faces) {
            for (Face f2 : faces) {
                f2.Begin();
                if (f1.inside(f2.Cur().s)) {
                    f1.addHole(f2);
                    holes.add(f2);
                }
            }
        }
        for (Face h : holes) {
            faces.remove(h);
        }
        
        return faces;
    }
    
    public Region ProjectXY () {
        Set<Seg> segs = new HashSet();
        for (Triangle tri : triangles) {
            Point prev = null, first = null;
            for (Point3D p3d : tri.points) {
                Point p = new Point(p3d.x, p3d.y);
                if (prev == null)
                    first = p;
                else
                    segs.add(new Seg(prev, p).orient());
                prev = p;
            }
            segs.add(new Seg(prev, first).orient());
        }
        List<Seg> seglist = new LinkedList(segs);
        seglist = intersectAndSplitSegments(seglist);
        List<Face> faces = reconstructFaces(seglist);
        Region r = new Region();
        for (Face f : faces) {
            r.addFace(f);
        }
        
        return r;
    }
    
    @Override
    public PMRegion deserialize (NL nl) {
        PMRegion ret = new PMRegion();
        NL pointslist = nl.get(0);
        NL facelist = nl.get(1);
        Point3D[] points = new Point3D[pointslist.size()];
        
        ret.bb = null;
       
        System.out.println("Deserialize points: "+pointslist.size());
        
        int i = 0;
        for (NL pl : pointslist.getNl()) {
            double x = pl.get(0).getNr();
            double y = pl.get(1).getNr();
            double z = pl.get(2).getNr();
            if (ret.bb == null) {
                ret.bb = new Seg(x, y, x, y);
            } else {
                if (ret.bb.s.x > x)
                    ret.bb.s.x = x;
                if (ret.bb.e.x < x)
                    ret.bb.e.x = x;
                if (ret.bb.s.y > y)
                    ret.bb.s.y = y;
                if (ret.bb.e.y < y)
                    ret.bb.e.y = y;
            }
            if (ret.min == null || ret.min > z)
                ret.min = z;
            if (ret.max == null || ret.max < z)
                ret.max = z;
            points[i++] = new Point3D(x, y, z);
            System.out.print("\rP: "+i);
        }
        
        i = 0;
        System.out.println("\nDeserialize faces: "+facelist.size());
        for (NL tl : facelist.getNl()) {
            int t1 = (int)Math.round(tl.get(0).getNr());
            int t2 = (int)Math.round(tl.get(1).getNr());
            int t3 = (int)Math.round(tl.get(2).getNr());
            ret.triangles.add(new Triangle(points[t1], points[t2], points[t3]));
            System.out.print("\rF: "+i++);
        }
        System.out.println("");

        return ret;
    }
    
    @Override
    public BoundingBox getBoundingBox () {
        return new BoundingBox(bb);
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        Region r = project(currentTime, g.getBoundingBox());
        Color c = Color.BLUE;
	if (isRed()) {
            c = Color.RED;
	}
        if (!filled) {
            c = null;
        }
        if (transparent) {
            c = new Color(0.0f, 0.0f, 1.0f, 0.7f);
        }
//        r.paint(g, Color.BLACK, c, Color.WHITE);
        r.paint2(g, Color.BLACK, c, Color.WHITE, 1.0f);
    }

    @Override
    public long getStartTime() {
        return (long)((double)min);
    }

    @Override
    public long getEndTime() {
        return (long)((double)max);
    }

    @Override
    public String getSecondoType() {
        return "pmregion";
    }

    @Override
    public NL serialize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the filled
     */
    public boolean isFilled() {
        return filled;
    }

    /**
     * @param filled the filled to set
     */
    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    /**
     * @return the transparent
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * @param transparent the transparent to set
     */
    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

}
