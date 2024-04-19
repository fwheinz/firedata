package mrview;

import java.awt.Color;
import java.awt.geom.Area;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRegion extends MovingObject implements SecondoObject {

    public List<URegion> uregions = new LinkedList();
    private boolean wireframe = false, scalarfield = false, wireframeonly=false, filled=true;

    public MRegion() {
    }

    @Override
    public String[] getOperations() {
        return new String[]{
            "Analyze",
            "TraversedArea"
        };
    }

    @Override
    public String[] getFlags() {
        return new String[]{
            "Filled",
            "Wireframe",
            "Wireframeonly",
            "Scalarfield"
        };
    }

    public MRegion(List<MFace2> mfaces) {
        URegion u = new URegion();
        u.mfaces = mfaces;
        u.iv = new Interval(0, 1000000, true, true);
        uregions.add(u);
    }

    public MRegion(MFace2 mface) {
        URegion u = new URegion();
        u.mfaces.add(mface);
        uregions.add(u);
    }

    public Region project(long t) {
        Region r = null;
        for (URegion u : uregions) {
            r = u.project(t);
            if (r != null) {
                break;
            }
        }

        return r;
    }

    public Region project(double t) {
        Region r = null;
        for (URegion u : uregions) {
            r = u.project(t);
            if (r != null) {
                break;
            }
        }

        return r;
    }

    private Region wireframe() {
        Region wf = new Region();
        for (URegion u : uregions) {
            Region r = u.wireframe();
            wf.add(r);
        }

        return wf;
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        if (wireframe || wireframeonly) {
            Region r = wireframe();
            r.paint(g, highlight ? Color.RED : Color.BLACK, null, null);
        }
        if (!wireframeonly)
        {
            Region r = project(currentTime);
            if (r != null) {
                if (filled)
                    r.paint(g, highlight ? Color.RED : Color.BLACK, isRed() ? Color.RED : Color.BLUE, Color.LIGHT_GRAY);
                else
                    r.paint(g, highlight ? Color.RED : Color.BLACK, null, Color.LIGHT_GRAY);
            }
        }
        if (scalarfield) {
            paintScalarField(g);
        }
    }

    @Override
    public long getStartTime() {
        Long starttime = null;
        for (URegion uregion : uregions) {
            long st = uregion.iv.getStart();
            if (starttime == null || starttime > st) {
                starttime = st;
            }
        }
        
        return starttime == null ? -1 : starttime;
    }

    @Override
    public long getEndTime() {
        Long endtime = null;
        for (URegion uregion : uregions) {
            long et = uregion.iv.getEnd();
            if (endtime == null || endtime < et) {
                endtime = et;
            }
        }
        
        return endtime == null ? -1 : endtime;
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        for (URegion uregion : uregions) {
            bb.update(uregion.getBoundingBox());
        }

        return bb;
    }

    private int[][] field;
    private int max = 0;

    public void paintScalarField(MRGraphics g) {
        Long st = getStartTime();
        Long et = getEndTime();
        BoundingBox bb = getBoundingBox();

        double nrx = 1000, nry = 1000, nrt = 1000;

        nry = (nrx * (bb.ur.y - bb.ll.y) / (bb.ur.x - bb.ll.x));

        double dt = (et - st) / nrt;
        double dx = (bb.ur.x - bb.ll.x) / nrx;
        double dy = (bb.ur.y - bb.ll.y) / nry;
        double sz = (bb.ur.x - bb.ll.x) / nrx;

        if (field == null) {
            field = new int[(int) nrx + 1][(int) nry + 1];

            for (double t = st; t < et; t += dt) {
                Region r = project((long) t);
                for (int i = 0; i < nrx; i++) {
                    for (int j = 0; j < nry; j++) {
                        boolean inside = r.inside(new Point(bb.ll.x + dx * i, bb.ll.y + dy * j));
                        if (inside) {
                            field[i][j]++;
                            if (field[i][j] > max) {
                                max = field[i][j];
                            }
                        }
                    }
                }
            }
        }
        
        for (double i = 0; i < nrx; i++) {
            for (double j = 0; j < nry; j++) {
                int val = field[(int) i][(int) j];
                Point p = new Point(bb.ll.x + dx * i, bb.ll.y + dy * j);
                if (val > 0) {
                    int v = 255 - val * 255 / max;
                    g.drawRect(p, sz, sz, new Color(v, v, v, 200));
                }
                g.drawString(p, Integer.toString(val), Color.RED);
            }
        }
    }

    @Override
    public String getSecondoType() {
        return "mregion";
    }

    public void Analyze() {
        MRegionAnalysis ma = new MRegionAnalysis(this);
        MRegionAnalysisWindow maw = new MRegionAnalysisWindow(ma);
    }

    public Region TraversedArea() {
        Area a = new Area();

        for (URegion u : uregions) {
            Area ua = u._TraversedArea();
            a.add(ua);
        }

        return Region.area2region(a);

        /*
         Region ta = new Region();
         Set<Seg> segs = new HashSet();
        
         for (URegion u : uregions) {
         for (MFace2 mf : u.mfaces) {
         for (MSeg2 ms : mf.msegs) {
         ta.addFace(ms.getTraversedArea());
         segs.addAll(ms.getTraversedArea().getSegments());
         }
         }
         }
        
        
         Set<Seg> rev = new HashSet();
         for (Seg s : segs) {
         rev.add(s.reverse());
         }
         segs.addAll(rev);
        
         Point start = null;
         Map<Point, Set<Seg>> ps = new HashMap();
         for (Seg s : segs) {
         Point p = s.s;
         Set<Seg> l = ps.get(p);
         if (l == null) {
         l = new HashSet();
         ps.put(p, l);
         }
         l.add(s);
         if (start == null || (p.x < start.x) || (p.x == start.x && p.y < start.y)) {
         start = p;
         }
         }
        
         System.out.println("start: "+start.toString());
        
         Seg startseg = null;
         for (Seg s : ps.get(start)) {
         if (startseg == null || startseg.getAngle() < s.getAngle())
         startseg = s;
         }
        
         Seg curseg = startseg;
         do {
         Seg nextseg = null;
         double maxangle = -1;
         for (Seg s : ps.get(curseg.e)) {
         double a = curseg.reverse().getAngle(s);
         if (nextseg == null || a > maxangle) {
         nextseg = s;
         maxangle = a;
         }
         }
         } while (false);
        
         return ta;
         */
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        MRegion mregion = new MRegion();
        for (int i = 0; i < nl.size(); i++) {
            mregion.uregions.add(URegion.deserialize(nl.get(i)));
        }

        return mregion;
    }

    @Override
    public NL serialize() {
        NL nl = new NL();
        for (URegion uregion : uregions) {
            nl.addNL(uregion.serialize());
        }

        return nl;
    }

    /**
     * @return the wireframe
     */
    public boolean isWireframe() {
        return wireframe;
    }

    /**
     * @param wireframe the wireframe to set
     */
    public void setWireframe(boolean wireframe) {
        this.wireframe = wireframe;
    }

    /**
     * @return the scalarfield
     */
    public boolean isScalarfield() {
        return scalarfield;
    }

    /**
     * @param scalarfield the scalarfield to set
     */
    public void setScalarfield(boolean scalarfield) {
        this.scalarfield = scalarfield;
    }

    /**
     * @return the wireframeonly
     */
    public boolean isWireframeonly() {
        return wireframeonly;
    }

    /**
     * @param wireframeonly the wireframeonly to set
     */
    public void setWireframeonly(boolean wireframeonly) {
        this.wireframeonly = wireframeonly;
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
