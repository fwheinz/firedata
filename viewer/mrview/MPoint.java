package mrview;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MPoint extends MovingObject implements SecondoObject {
    private boolean trajectory = false;
    private List<UPoint> units = new LinkedList();
    private Point lastPoint = null;
    private long lasttime;

    @Override
    public String[] getOperations() {
        return new String[]{
            "FMTransform",
            "MPointInside"
        };
    }
    @Override
    public String[] getFlags() {
        return new String[]{
            "Trajectory"
        };
    }

    public void addUPoint(UPoint p) {
        units.add(p);
    }

    public void addUPoint(Point p, long time) {
        if (lastPoint == null) {
            lastPoint = p;
            lasttime = time;
        } else if (units.isEmpty()) {
            units.add(new UPoint(lastPoint, p.sub(lastPoint), lasttime, time));
        } else {
            UPoint u = units.get(units.size() - 1);
            Point lp = u.getS().add(u.getV());
            units.add(new UPoint(lp, p.sub(lp), u.getEndTime(), time));
        }
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        for (UPoint u : units) {
            u.paint(g, currentTime, highlight);
        }
        if (trajectory) {
            for (UPoint u : units) {
                u.paintTrajectory(g);
            }
        }
    }

    @Override
    public long getStartTime() {
        Long minTime = null;
        for (UPoint u : units) {
            if (minTime == null || u.getStartTime() < minTime) {
                minTime = u.getStartTime();
            }
        }

        return minTime == null ? -1 : minTime;
    }

    @Override
    public long getEndTime() {
        Long maxTime = null;
        for (UPoint u : units) {
            if (maxTime == null || u.getEndTime() > maxTime) {
                maxTime = u.getEndTime();
            }
        }

        return maxTime == null ? -1 : maxTime;
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        for (UPoint u : units) {
            bb.update(u.getBoundingBox());
        }
        return bb;
    }

    /**
     * @return the units
     */
    public List<UPoint> getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(List<UPoint> units) {
        this.units = units;
    }

    /**
     * @return the lastPoint
     */
    public Point getLastPoint() {
        return lastPoint;
    }

    /**
     * @param lastPoint the lastPoint to set
     */
    public void setLastPoint(Point lastPoint) {
        this.lastPoint = lastPoint;
    }
    
    public void FMTransform() {
        FMRegion fmr = (FMRegion) MRViewWindow.m.getFirstObject(FMRegion.class);
        units = FMTransform(fmr);
        for (FMRegionTrans tu : fmr.getTransformations()) {
            tu.setA(0);
            tu.setA0(0);
            tu.setV(new Point(0, 0));
            tu.setV0(new Point(0, 0));
        }
        fmr.renewFMSegs();
    }

    public List<UPoint> FMTransform(FMRegion fmr) {
        List<UPoint> nunits = new LinkedList();
        List<FMRegionTrans> frt = fmr.getTransformations();
        
        Iterator<FMRegionTrans> frti = frt.iterator();
        Iterator<UPoint> ui = units.iterator();

        FMRegionTrans fr = frti.next();
        UPoint up = ui.next();
        try {
            do {
                Interval i = fr.getIv().intersect(up.getIv());
                if (i != null) {
                    UPoint up2 = up.restrict(i.getStart(), i.getEnd());
                    FMRegionTrans fr2 = fr.restrict(i.getStart(), i.getEnd());
                    up2.transform(fr2);
                    nunits.add(up2);
                }
                if (fr.getIv().getEnd() < up.getIv().getEnd()) {
                    fr = frti.next();
                } else if (up.getIv().getEnd() < fr.getIv().getEnd()) {
                    up = ui.next();
                } else {
                    fr = frti.next();
                    up = ui.next();
                }
            } while (true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return nunits;
    }

    public void MPointInside() {
        FMRegion fmr = (FMRegion) MRViewWindow.m.getFirstObject(FMRegion.class);
        MBool mb = new MBool();
        Region r = fmr.getRegion();
        for (UPoint u : FMTransform(fmr)) {
            List<Double> ps = new LinkedList();
            for (Face f : fmr.getRegion().getFaces()) {
                for (Seg seg : f.getSegments()) {
                    Newton n = new ISCurveSeg(u, new SegT(seg));
                    for (double[] d : n.findIntersectionTimes()) {
                        ps.add(d[0]);
                    }
                }
                for (Face h : f.getHoles()) {
                    for (Seg seg : h.getSegments()) {
                        Newton n = new ISCurveSeg(u, new SegT(seg));
                        for (double[] d : n.findIntersectionTimes()) {
                            ps.add(d[0]);
                        }
                    }
                }
            }
            ps.add(1d);
            Collections.sort(ps);
            double prev = 0d;
            Interval iv = u.getIv();
            for (Double p : ps) {
                double middle = (p+prev)/2;
                boolean lc = (prev != 0d) || iv.isLeftClosed();
                boolean rc = (p    == 1d) && iv.isRightClosed();
                Interval niv = new Interval(iv.project(prev), iv.project(p), lc, rc);
                boolean val = r.inside(u.project(middle));
                mb.addUnit(new UBool(niv, val));
                prev = p;
            }
        }
        MRViewWindow.m.addMFace(mb);
    }

    @Override
    public String getSecondoType() {
        return "mpoint";
    }

    @Override
    public NL serialize() {
        NL nl = new NL();

        for (UPoint u : units) {
            nl.addNL(u.toNL());
        }

        return nl;
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        MPoint mp = new MPoint();

        for (int i = 0; i < nl.size(); i++) {
            UPoint up = UPoint.fromNL(nl.get(i));
            mp.addUPoint(up);
        }

        return mp;
    }

    /**
     * @return the trajectory
     */
    public boolean isTrajectory() {
        return trajectory;
    }

    /**
     * @param trajectory the trajectory to set
     */
    public void setTrajectory(boolean trajectory) {
        this.trajectory = trajectory;
    }
}
