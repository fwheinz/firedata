package mrview;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class Region extends MovingObject implements SecondoObject {
    List<Face> fcs = new LinkedList();
    private boolean filled = false;
    
    public Region () {
    }
    
    public Region (Face f) {
        fcs.add(f);
    }
    
    public void addFace (Face f) {
        fcs.add(f);
    }
    
    public void fixHoles () {
        List<Face> holes = new LinkedList();
        for (Face f1 : fcs) {
            for (Face f2 : fcs) {
                if (f1 == f2)
                    continue;
                f2.Begin();
                if (f1.inside(f2.Cur().s)) {
                    f1.addHole(f2);
                    holes.add(f2);
                }
            }
        }
        for (Face h : holes) {
            fcs.remove(h);
        }
    }
    
    public void add (Region r) {
        for (Face f : r.getFaces()) {
            addFace(f);
        }
    }
    
    public List<Face> getFaces () {
        return fcs;
    }
    
    public String nl() {
        StringBuilder sb = new StringBuilder("(\n");
        for (Face f : fcs) {
            sb.append(f.nl());
        }
        sb.append(")\n");
        
        return sb.toString();
    }
    
    public String nlmf() {
        StringBuilder sb = new StringBuilder(
                "( OBJECT obj () mregion\n"+
                "(\n"+
                " (\n"+
                "  ( \"2014-01-01-00:00:00.000\" \"2014-01-02-00:00:00.000\" TRUE TRUE )\n"+
                "  (\n"
        );
        
        for (Face f : fcs) {
            sb.append(f.nlmf());
        }
        sb.append(
                "  )\n"+
                " )\n"+
                ")\n"+
                ")\n");
        
        return sb.toString();
    }

    @Override
    public String getSecondoType() {
        return "region";
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        Region r = new Region();
        for (int i = 0; i < nl.size(); i++) {
            Face f = Face.fromNL(nl.get(i));
            if (f != null) {
                f.sort2();
                r.addFace(f);
            }
        }
        
        return r;
    }

    @Override
    public NL serialize() {
        NL nl = new NL();
        
        for (Face f : fcs) {
            nl.addNL(f.toNL());
        }
        
        return nl;
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        double transp = 1.0f;
        for (Face f : fcs) {
            f.paint(g, highlight?Color.RED:Color.BLACK, isFilled()?Color.DARK_GRAY:null, transp);
            for (Face h : f.getHoles()) {
                h.paint(g, Color.BLACK, isFilled()?Color.LIGHT_GRAY:null, transp);
            }
        }
    }
    
    public void paint (MRGraphics g, Color border, Color fill, Color hole, double transp) {
        for (Face f : fcs) {
            f.paint(g, border, fill, transp);
            for (Face h : f.getHoles()) {
                hole = Color.WHITE;
                h.paint(g, border, hole, 0.0f);
            }
        }
    }
    
    public void paint2 (MRGraphics g, Color border, Color fill, Color hole, double transp) {
        Area a = new Area();
        for (Face f : fcs) {
		g.drawShape(f.getAreaObj(), fill);
                for (Face h : f.getHoles()) {
                    g.drawShape(h.getAreaObj(), Color.WHITE);
                }
//            a.add(f.getAreaObj());
        }
//        g.drawShape(a, fill);
    }
    
    public void paint (MRGraphics g, Color border, Color fill, Color hole) {
        paint(g, border, fill, hole, 0.0f);
    }

    @Override
    public long getStartTime() {
        return -1;
    }

    @Override
    public long getEndTime() {
        return -1;
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        for (Face f : fcs) {
            bb.update(f.getBoundingBox());
        }
        
        return bb;
    }
    
    public Point getCenter () {
        Point totalcenter = new Point(0, 0);
        double totalarea = 0;
        
        for (Face f : fcs) {
            double area = f.getArea();
            Point center = f.getCenter();
            if (center != null) {
                totalarea += area;
                totalcenter = totalcenter.add(center.mul(area));
            }
        }
        totalcenter = totalcenter.div(totalarea);
        
        return totalcenter;
    }
    
    @Override
    public String[] getOperations() {
        return new String[] {
            "MakeFMRegion",
            "MakeMRegion",
            "ConvexHull",
            "Concavities",
            "Connect",
            "Shrink",
            "Meet",
            "Relocate"
        };
    }
    
    @Override
    public String[] getFlags() {
        return new String[]{
            "Filled"
        };
    }
    
    public void MakeFMRegion () {
        ObjectCreator<FMRegion> oc = new FMRegionCreator(this);
        MRViewWindow.m.setCreator(oc);
    }
    
    public void MakeMRegion () {
        ObjectCreator<MRegion> oc = new MRegionCreator(this);
        MRViewWindow.m.setCreator(oc);
    }
    
    public void ConvexHull() {
        Region r = new Region();
        for (Face f : fcs) {
            r.addFace(f.getConvexHull());
        }
        MRViewWindow.m.addMFace(r);
    }
    
    public void Concavities() {
        Region r = new Region();
        for (Face f : fcs) {
            List<Face> cvs = f.getConcavities();
            for (Face f2 : cvs)
                r.addFace(f2);
        }
        MRViewWindow.m.addMFace(r);
    }
    
    public void transform (Point c, Point v, double a) {
        for (Face f : fcs) {
            f.transform(c, v, a);
        }
    }
    
    public boolean inside (Point p) {
        for (Face f : fcs) {
            if (f.inside(p))
                return true;
        }
        
        return false;
    }
    
    public void Connect() {
        Face f = Face.connect(fcs.toArray(new Face[0]));
        fcs.clear();
        fcs.add(f);
    }
    
    public void Shrink() {
        for (Face f : fcs)
            f.shrink();
    }
    
    public void Meet() {
        Connect();
        MRegion mf = new MRegion();
        URegion ur = new URegion();
        for (Face f : fcs)
            ur.mfaces.add(f.meet());
        mf.uregions.add(ur);
        MRViewWindow.m.addMFace(mf);
    }
    
    public double getArea() {
        double ret = 0;
        
        for (Face f : fcs) {
            ret += f.getArea();
        }

        return ret;
    }
    
    public Area toArea () {
        Area ret = new Area();
        for (Face f : fcs)
            ret.add(f.getAreaObj());
        
        return ret;
    }
    
    public Region unionArea (Region r) {
        Area me = toArea();
        Area them = r.toArea();
        me.add(them);
        
        return area2region(me);
    }
    
    public Region intersectArea (Region r) {
        Area me = toArea();
        Area them = r.toArea();
        me.intersect(them);
        
        return area2region(me);
    }
    
    public void Relocate () {
        Region r = (Region) ImportExport.copy(this);
//        r.transform(new Point(0, 0), r.getFaces().get(0).getCenter(), 0);
        r.transform(r.getFaces().get(0).getLastPoint(), new Point(1,1), Math.PI*0.6);
        MRViewWindow.m.addMFace(r);
    }
    
    public Area getAreaObj() {
        Area ret = new Area();
        
        for (Face f : fcs) {
            ret.add(f.getAreaObj());
        }
        
        return ret;
    }
    
    public static Region area2region (Area a) {
        PathIterator p = a.getPathIterator(null);
        Region ret = new Region();
        List<Face> nf = new LinkedList();
        Face fc = null;
        while (!p.isDone()) {
            double[] v = new double[6];
            int st = p.currentSegment(v);
            if (st == PathIterator.SEG_CLOSE) {
                fc.close();
                fc.sort2();
                System.out.println("Got face "+fc.toString());
                nf.add(fc);
            } else if (st == PathIterator.SEG_MOVETO) {
                fc = new Face();
                fc.addPointRaw(new Point(v[0], v[1]));
            } else if (st == PathIterator.SEG_LINETO) {
                fc.addPointRaw(new Point(v[0], v[1]));
            } else {
                throw new RuntimeException("Invalid segment type "+st);
            }
            p.next();
        }
        
        Iterator<Face> i = nf.iterator();
        while (i.hasNext()) {
            Face f = i.next();
            for (int j = 0; j < nf.size(); j++) {
                Face f2 = nf.get(j);
                if (f != f2) {
                    Point po = f.getSegments().get(0).s;
                    if (f2.inside(po)) {
                        f2.addHole(f);
                        i.remove();
                        break;
                    }
                }
            }
        }
        
        for (Face f : nf)
            ret.addFace(f);
        
        return ret;
    }
    
    public URegion move (Point p) {
        URegion ret = new URegion();
        
        for (Face f : fcs) {
            ret.mfaces.add(f.move(p));
        }
        
        return ret;
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
}
