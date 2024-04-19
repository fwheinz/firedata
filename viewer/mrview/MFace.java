package mrview;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MFace extends MovingObject {
    private long startTime;
    private long endTime;
    private List<MovingSeg> segs = new LinkedList();
    List<MFace> holes = new LinkedList();
    private boolean trochoids, intersections, paintTraversed;
    private List<Point> is;
    private List<Curve> trochoidsData;

    public MFace(List<MovingSeg> ms, long startTime, long endTime) {
        this.segs = ms;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String[] getOperations() {
        return new String[]{
            "TraversedArea"
        };
    }

    public String[] getFlags() {
        return new String[]{
            "Trochoids",
            "Intersections",
            "PaintTraversed"
        };
    }

    public void addMSeg(MovingSeg ms) {
        getSegs().add(ms);
    }

    public void addHole(MFace hole) {
        holes.add(hole);
    }

    private void printGraph(FMSeg fms, String prefix, int linestyle) {
        Point C = fms.getCenter();
        Point S = fms.getInitial().s;
        Point v = fms.getVector();
        double rotate = fms.getRotation();

        long Cx = Math.round(C.x);
        long Cy = Math.round(C.y);
        long Sx = Math.round(S.x);
        long Sy = Math.round(S.y);
        long ro = Math.round(rotate);
        long vx = Math.round(v.x);
        long vy = Math.round(v.y);

        String x = "" + Cx + "+(" + (Sx - Cx) + ")*cos(" + ro + "*t) - "
                + "(" + (Sy - Cy) + ")*sin(" + ro + "*t)"
                + " + (" + vx + "*t)";
        String y = "" + Cy + "+(" + (Sx - Cx) + ")*sin(" + ro + "*t) + "
                + "(" + (Sy - Cy) + ")*cos(" + ro + "*t)"
                + " + (" + vy + "*t)";
        Plot.plot(prefix, x, y, linestyle);
    }

    public void printGraphs(String prefix, int linestyle) {
        int i = 1;
        FMSeg fms = null;
        for (MovingSeg ms : getSegs()) {
            if (ms instanceof FMSeg) {
                fms = (FMSeg) ms;
                printGraph(fms, prefix + i, 4 + i - 1);
                i++;
            }
        }
        if (fms == null) {
            return;
        }

        i = 1;
        for (MovingSeg ms : getSegs()) {
            if (ms instanceof FMSeg) {
                fms = (FMSeg) ms;
                Seg seg = fms.getInitial();
                seg.printGraph("seg", 5, i++);
            }
        }
    }

    public void paint(MRGraphics g, long currentTime, boolean highlight, boolean hole) {
        if (isPaintTraversed()) {
            paintTraversed(g);
        }
        List<Point> pts = new LinkedList();
        FMSeg fms = null;
        for (MovingSeg ms : getSegs()) {
            Seg s = ms.project(currentTime);
            if (s == null) {
                return;
            }
            pts.add(s.s);
            if (ms instanceof FMSeg) {
                fms = (FMSeg) ms;
            }
        }
        g.drawPolygon(pts, hole);
        for (MFace h : holes) {
            h.paint(g, currentTime, highlight, true);
        }
        if (fms != null) {
            g.drawPoint(fms.getProjectedCenter(currentTime));
        }
        if (highlight) {
            for (MovingSeg ms : getSegs()) {
                Seg s = ms.project(currentTime);
                g.drawLine(s, Color.red);
            }
        }
        if (trochoids) {
            if (trochoidsData == null) {
                trochoidsData = new LinkedList();
                for (MovingSeg ms : getSegs()) {
                    if (ms instanceof FMSeg) {
                        fms = (FMSeg) ms;
                        Curve tr = fms.getTroc();
                        Curve tr2 = fms.getRavd();
                        Curve si = fms.getSegT(true);
                        Curve se = fms.getSegT(false);
                        trochoidsData.add(tr);
                        trochoidsData.add(tr2);
                        trochoidsData.add(si);
                        trochoidsData.add(se);
                    }
                }
            }
            for (Curve c : trochoidsData) {
                c.paint(g, currentTime, highlight);
            }
            
        }
        if (isIntersections()) {
            if (is == null) {
                is = Intersections.calculatePoints(this);
            }
            for (Point p : is) {
                g.drawPoint(p, 3, Color.red);
            }
        }
    }

    public void paintTraversed(MRGraphics g) {
        int samples = 1000;
        for (double i = 0; i < 1.0; i += 1.0 / samples) {
            g.drawFace(this.project((double) i), false);
        }
        for (MovingSeg ms : getSegs()) {
            if (ms instanceof FMSeg) {
                FMSeg fms = (FMSeg) ms;
                Point p = fms.getInitial().s;
                Point center = fms.getCenter();
                Point vector = fms.getVector();
                double angle = fms.getRotation();
                for (double i = 0; i < 1.0; i += 1.0 / samples) {
                    Point pt = p.rotate(center, angle * i).add(vector.mul(i));
//                    g.drawPoint(pt, 1);
                }
            }
        }
    }

    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        paint(g, currentTime, highlight, false);
    }

    public BoundingBox getBoundingBox() {
        BoundingBox ret = new BoundingBox();
        for (MovingSeg s : getSegs()) {
            ret.update(s.getBoundingBox());
        }

        return ret;
    }

    public static MFace createFMFace(Face f, Point center, Point vector, double angle) {
        List<MovingSeg> ms = new LinkedList();
        if (center == null) {
            Seg first = f.getSegments().get(0);
            center = first.s;
        }
        for (Seg s : f.getSegments()) {
            ms.add(new FMSeg(s, vector, center, angle, 0, 1000000));
        }

        return new MFace(ms, 0, 1000000);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (MovingSeg ms : getSegs()) {
            sb.append(ms);
            sb.append(" ");
        }
        if (!holes.isEmpty()) {
            sb.append("\nHoles:\n");
            for (MFace mf : holes) {
                sb.append(mf);
                sb.append("\n");
            }
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the endTime
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * @return the segs
     */
    public List<MovingSeg> getSegs() {
        return segs;
    }
    
    public List<FMSeg> getFMSegs() {
        List<FMSeg> ret = new LinkedList();
        
        for (MovingSeg ms : getSegs()) {
            if (ms instanceof FMSeg)
                ret.add((FMSeg) ms);
        }
        
        return ret;
    }

    public FMSeg getFMSeg(int i) {
        for (MovingSeg ms : getSegs()) {
            if (ms instanceof FMSeg) {
                if (i-- == 0) {
                    return (FMSeg) ms;
                }
            }
        }
        return null;
    }

    public FMSeg getFMSeg() {
        return getFMSeg(0);
    }

    public void printGraph(String prefix, int linestyle) {
        int i = 1;
        for (MovingSeg ms : getSegs()) {
            if (ms instanceof FMSeg) {
                FMSeg mseg = (FMSeg) ms;
                Seg s = mseg.project(mseg.getStartTime());
                s.printGraph(prefix, linestyle, i++);
            }
        }
    }
    
    public void TraversedArea() {
        CRegion r2 = Intersections.calculateTraversedArea(this.getFMSegs());
        MRViewWindow.m.addMFace(r2);
    }

    public Face project(double t) {
        Face ret = new Face();
        for (MovingSeg ms : getSegs()) {
            Seg s = ms.project(t);
            ret.addPoint(s.s);
        }
        ret.close();

        return ret;
    }

    public Face project(long currentTime) {
        double frac = ((double) (currentTime - startTime)) / ((double) (endTime - startTime));
        return project(frac);
    }

    /**
     * @return the trochoids
     */
    public boolean isTrochoids() {
        return trochoids;
    }

    /**
     * @param trochoids the trochoids to set
     */
    public void setTrochoids(boolean trochoids) {
        this.trochoids = trochoids;
    }

    /**
     * @return the intersections
     */
    public boolean isIntersections() {
        return intersections;
    }

    /**
     * @param intersections the intersections to set
     */
    public void setIntersections(boolean intersections) {
        this.intersections = intersections;
    }

    /**
     * @return the paintTraversed
     */
    public boolean isPaintTraversed() {
        return paintTraversed;
    }

    /**
     * @param paintTraversed the paintTraversed to set
     */
    public void setPaintTraversed(boolean paintTraversed) {
        this.paintTraversed = paintTraversed;
    }
}
