package mrview;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FMRegion extends MovingObject implements SecondoObject {
    /* Source definition */

    private Region region;
    private List<FMRegionTrans> transformations = new LinkedList();

    /* Compiled data */
//    private List<List<FMSeg>> fmsegs = new LinkedList();
//    private List<List<FMSeg>> holes = new LinkedList();
    private Long start, end;

    /* Display Attributes */
    private boolean border = true, trochoids, intersections, paintTraversed, ravdoidGeneration, filled = true, printcenter = true;
    List<Curve> trochoidsData;
    List<Point> is;

    public FMRegion() {
    }

    public FMRegion(Region region) {
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    private void createFMSegs(FMRegionTrans t) {
        if (region == null) {
            return;
        }
        t.createFMSegs(region);
        if (start == null || start > t.getStart()) {
            start = t.getStart();
        }
        if (end == null || end < t.getEnd()) {
            end = t.getEnd();
        }
    }
    
    private List<FMSeg> getAllFMSegs() {
        List<FMSeg> l = new LinkedList();
        
        for (FMRegionTrans t : transformations)
            for (List<List<FMSeg>> fll : t.getFmfaces())
                for (List<FMSeg> fl : fll)
                    l.addAll(fl);
        
        return l;
    }
    
    public MFace getMFace(int transformation) {
        List<MovingSeg> l = new LinkedList();
        FMRegionTrans t = transformations.get(transformation);
        for (List<List<FMSeg>> fll : t.getFmfaces()) {
            for (List<FMSeg> fl : fll) {
                for (FMSeg f : fl) {
                    l.add(f.copy());
                }
            }
        }
        
        return new MFace(l, t.getStart(), t.getEnd());
    }

    public void renewFMSegs() {
        start = end = null;
        for (FMRegionTrans t : transformations) {
            createFMSegs(t);
        }
    }

    public void addFMRegionTrans(FMRegionTrans t) {
        transformations.add(t);
        createFMSegs(t);
    }

    public void addFMRegionTrans(Point center, Point v, double a, long duration) {
        Point v0 = new Point(0, 0);
        double a0 = 0;
        long start = 0;
        int nrtrans = transformations.size();
        if (nrtrans > 0) {
            FMRegionTrans t = transformations.get(nrtrans - 1);
            v0 = t.getV0().add(t.getV());
            a0 = t.getA0() + t.getA();
            // Compensate change of center
            Point dc = center.rotate(t.getCenter(), a0).sub(center);
            v0 = v0.add(dc);
            start = t.getEnd();
            t.setRightClosed(false);
        }
        FMRegionTrans t = new FMRegionTrans(center, v0, v, a0, a, start, start + duration);
        transformations.add(t);
        createFMSegs(t);
    }

    @Override
    public String[] getOperations() {
        return new String[]{
            "TraversedArea",
            "AddUnit",
            "SplitTransformations"
        };
    }

    public String[] getFlags() {
        return new String[]{
            "Trochoids",
            "Intersections",
            "PaintTraversed",
            "RavdoidGeneration",
            "Filled",
            "Border",
            "Printcenter"
        };
    }

    public List<Curve> getTrochoidsData() {
        if (trochoidsData == null) {
            trochoidsData = new LinkedList();
            for (FMSeg ms : getAllFMSegs()) {
                ms.normalizeRotation();
                Curve tr = fixCurve(ms.getTroc());
                Curve tr2 = fixCurve(ms.getRavd());
                Curve si = fixCurve(ms.getSegT(true));
                Curve se = fixCurve(ms.getSegT(false));
                trochoidsData.add(tr);
                if (tr2 != null)
                    trochoidsData.add(tr2);
                trochoidsData.add(si);
                trochoidsData.add(se);
            }
        }
        return trochoidsData;
    }

    public static Curve fixCurve(Curve c) {
        Curve ret = c;

        if (c instanceof Troc) {
            Troc t = (Troc) c;
            if (Math.abs(t.b) < Newton.PRECISION) {
                Point p1 = new Point(t.fx(0d), t.fy(0d));
                Point p2 = new Point(t.fx(1d), t.fy(1d));
                ret = new SegT(new Seg(p1, p2));
                ret.setTransformation(t.center, t.angle);
                System.out.println("Fixing troc " + t + "; is now: " + ret);
            }
        } else if (c instanceof Ravd) {
            Ravd t2 = (Ravd) c;
            if (t2.tmin == 0 && t2.tmax == 0) {
                ret = null;
            }
        }

        return ret;
    }
    
    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        if (isPaintTraversed()) {
            paintTraversed(g);
        }
        double frac = 0.0;
        FMSeg fms = null;
        
        for (FMRegionTrans t : transformations) {
            if (!t.getIv().inside(currentTime))
                continue;
            frac = t.getIv().getFrac(currentTime);
            Region r = t.project(currentTime);
            r.paint(g, highlight?Color.RED:(isBorder()?Color.BLACK:null),
                    isFilled()?Color.BLUE:null,
                    isFilled()?Color.LIGHT_GRAY:null);
        }
        
        for (FMSeg ms : getAllFMSegs()) {
            if (isRavdoidGeneration())
                ms.paintRavdoid(g, currentTime, highlight);
            Seg s = ms.project(currentTime);
            if (s == null) {
                continue;
            }
            fms = ms;
        }
        
        if (fms != null && isPrintcenter()) {
            g.drawPoint(fms.getProjectedCenter(currentTime));
        }
        
        if (trochoids) {
            for (Curve c : getTrochoidsData()) {
                c.paint(g, currentTime, highlight);
            }
        }
        if (ravdoidGeneration) {
            for (Curve c : getTrochoidsData()) {
                if (c instanceof Ravd)
                    c.paint(g, currentTime, highlight, Color.LIGHT_GRAY);
            }
        }
        if (isIntersections()) {
            for (Point p : getIntersections()) {
                g.drawPoint(p, 3, Color.red);
            }
        }
    }

    public void paintTraversed(MRGraphics g) {
        int samples = 1000;
        for (FMSeg fms : getAllFMSegs()) {
            for (double i = 0; i < 1.0; i += 1.0 / samples) {
                g.drawLine(fms.project(i), Color.gray);
            }
        }
    }

    @Override
    public long getStartTime() {
        return start != null ? start : 0;
    }

    @Override
    public long getEndTime() {
        return end != null ? end : 0;
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        for (FMSeg ms : getAllFMSegs()) {
            bb.update(ms.project(0.0f));
            bb.update(ms.project(1.0f));
        }
        
        return bb;
    }

    @Override
    public String getSecondoType() {
        return "fmregion";
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        Region r = (Region) new Region().deserialize(nl.get(0));
        FMRegion ret = new FMRegion(r);
        NL trafos = nl.get(1);
        for (int i = 0; i < trafos.size(); i++) {
            FMRegionTrans t = FMRegionTrans.deserialize(trafos.get(i));
            ret.addFMRegionTrans(t);
        }

        return ret;
    }

    @Override
    public NL serialize() {
        NL nl = new NL(), ret = nl;
        nl.addNL(region.serialize());
        nl = nl.nest();
        for (FMRegionTrans t : transformations) {
            nl.addNL(t.serialize());
        }

        return ret;
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

    /**
     * @return the fmsegs
     */
    public List<FMSeg> getFMSegs() {
        return getAllFMSegs();
    }
    
    public FMRegionTrans getLastTransformation() {
        int nrtrans = transformations.size();
        if (nrtrans == 0)
            return new FMRegionTrans();
        FMRegionTrans t = transformations.get(nrtrans-1);
        return t;
    }
    
    public void SplitTransformations() {
        List<FMRegionTrans> trafos = new LinkedList();
        for (FMRegionTrans t : transformations) {
            long st = t.getStart();
            long en = t.getEnd();
            long middle = (en+st)/2;
            trafos.add(t.restrict(st, middle));
            trafos.add(t.restrict(middle, en));
        }
        transformations = trafos;
        this.renewFMSegs();
    }
    
    /**
     * @return the ravdoidGeneration
     */
    public boolean isRavdoidGeneration() {
        return ravdoidGeneration;
    }

    /**
     * @param ravdoidGeneration the ravdoidGeneration to set
     */
    public void setRavdoidGeneration(boolean ravdoidGeneration) {
        this.ravdoidGeneration = ravdoidGeneration;
    }

    /**
     * @return the transformations
     */
    public List<FMRegionTrans> getTransformations() {
        return transformations;
    }

    /**
     * @param transformations the transformations to set
     */
    public void setTransformations(List<FMRegionTrans> transformations) {
        this.transformations = transformations;
    }
    
    public void TraversedArea() {
        for (FMRegionTrans t : transformations) {
            try {
                List<FMSeg> fs = t.getFMSegs(region);
                for (FMSeg f : fs) {
                    f.normalizeRotation();
                }
                CRegion r2 = Intersections.calculateTraversedArea(fs);
                MRViewWindow.m.addMFace(r2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
        
    public void AddUnit() {
        ObjectCreator oc = new FMRegionCreator(this);
        MRViewWindow.m.setCreator(oc);
    }

    public List<Point> getIntersections() {
        if (is == null) {
            is = new LinkedList();
            for (FMRegionTrans t : transformations) {
                List<FMSeg> fs = t.getFMSegs(region);
                for (FMSeg f : fs) {
                    f.normalizeRotation();
                }
                List<Point> ps = Intersections.calculatePoints(fs);
                is.addAll(ps);
            }
        }
        return is;
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
     * @return the printcenter
     */
    public boolean isPrintcenter() {
        return printcenter;
    }

    /**
     * @param printcenter the printcenter to set
     */
    public void setPrintcenter(boolean printcenter) {
        this.printcenter = printcenter;
    }

    /**
     * @return the border
     */
    public boolean isBorder() {
        return border;
    }

    /**
     * @param border the border to set
     */
    public void setBorder(boolean border) {
        this.border = border;
    }
}
