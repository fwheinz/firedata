package mrview;

import java.util.LinkedList;
import java.util.List;
import mrview.Curve.Peer;

public class Intersections {

    private final static int TYPE_POINTS = 0, TYPE_CURVE = 1;

    public static List calculate(List<FMSeg> msegs, int type) {
        List<Point> ps = new LinkedList();
        List<List<Curve>> css = new LinkedList();
        List<Curve> cs2 = new LinkedList();
        for (FMSeg fm : msegs) {
            fm.normalizeRotation();
            cs2.add(FMRegion.fixCurve(fm.getTroc(false)));
            cs2.add(FMRegion.fixCurve(fm.getRavd()));
            cs2.add(FMRegion.fixCurve(fm.getSegT(true)));
            cs2.add(FMRegion.fixCurve(fm.getSegT(false)));
            cs2.remove(null);
        }

        for (int i = 0; i < cs2.size(); i++) {
            for (int j = i; j < cs2.size(); j++) {
                Newton n = getNewton(cs2.get(i), cs2.get(j));
                if (n != null) {
                    ps.addAll(n.findIntersection());
                }
            }
        }

        if (type == TYPE_POINTS) {
            return ps;
        }

        List<List<Curve>> partitions = getPartitions(cs2);

        for (List<Curve> cs : partitions) {

            List<Curve> ta = getCycle(cs);
            css.add(ta);
        }
        return css;
    }

    private static List<List<Curve>> getPartitions(List<Curve> cs) {
        List<List<Curve>> partitions = new LinkedList();
//        if (true) { partitions.add(cs); return partitions; }

        while (!cs.isEmpty()) {
            Curve c = cs.get(0);
            cs.remove(c);
            if (c instanceof Ravd && c.getAdjacentCurves().isEmpty()) {
                continue;
            }
            System.out.println("\nCreating next partition!");
            List<Curve> partition = new LinkedList();
            partition.add(c);
            System.out.println("Starting with " + c);
            int added;
            do {
                added = 0;
                List<Curve> newp = new LinkedList();
                for (Curve t : partition) {
                    for (Curve adj : t.getAdjacentCurves()) {
                        if (!partition.contains(adj) && !newp.contains(adj)) {
                            System.out.println("Found " + adj);
                            newp.add(adj);
                            cs.remove(adj);
                            added++;
                        }
                    }
                }
                partition.addAll(newp);
            } while (added > 0);
            partitions.add(partition);
        }

        System.out.println("Found " + partitions.size() + " partitions!");
        for (List<Curve> partition : partitions) {
            System.out.println("\nPartition: ");
            for (Curve c : partition) {
                System.out.println("" + c);
            }
        }

        return partitions;
    }

    private static List<Curve> getCycle(List<Curve> cs) {
//        Peer p = getFirst2(cs, false);
        Peer p = getFirst3(cs);
        
        List<Curve> ta = new LinkedList();
        Point cur = null, start = null;
        int i = 100;
        do {
            if (start == null) {
                start = cur;
            }
            Curve cc = p.c;
            double t1 = p.t;
            double t2 = p.t2;
            Point p1 = cc.getPoint(t1);
            Point p2 = cc.getPoint(t2);
            ta.add(cc.restrict(t1, t2));
            System.out.println("P1: " + p1 + "; Via: " + p + " P2: " + p2 + "\n");
            cur = p2;
            p = cc.getNext(t1, t2);
        } while (i-- > 0 && !cur.nequals(start));
        
        return ta;
    }

    public static Peer getFirst3(List<Curve> cs) {
        Troc c = null;
        for (Curve o : cs) {
            if (o instanceof Troc) {
                Troc t = (Troc) o;
                if (c == null || t.b > c.b) {
                    c = t;
                }
            }
        }

        int index;
        double tt;
        double pd = 2*Math.PI/c.rot;
        double t1 = (-c.toff - Math.PI) / c.rot, t2, t3, t4;
        while (t1 < 0) {
            t1 += pd;
        }
        if (t1 > 1) {
            t1 -= pd;
        }
        t2 = t1 + pd/2;
        t3 = t2 + pd/2;
        t4 = t3 + pd/2;
        
        System.out.println("t1: "+t1);
        if (t1 > 0 && t1 < 1) {
            tt = t1;
            index = 1;
        } else if (t2 > 0 && t2 < 1) {
            tt = t2;
            index = c.b > c.a ? 1 : 0;
        } else if (t1 < 0 && 0 < t2) {
            tt = 0;
            if ((t1+t2)/2 < 0) {
                index = c.b > c.a ? 1 : 0;
            } else {
                index = 1;
            }
        } else if (t2 < 0 && 0 < t3) {
            tt = 0;
            if ((t2+t3)/2 < 0) {
                index = 1;
            } else {
                index = c.b > c.a ? 1 : 0;
            }
        } else if (t3 < 0 && 0 < t4) {
            tt = 0;
            if ((t3+t4)/2 < 0) {
                index = c.b > c.a ? 1 : 0;
            } else {
                index = 1;
            }
        } else
            throw new RuntimeException("Unforeseen constellation in getFirst3()");

        Double[] tx = c.getAdjacentIntersectionTimes(tt);
        Double tt2 = tx[index];
        System.out.println(c + "; T1: " + tt + "; T2: " + tt2);
        Point start = c.getPoint(tt2);
        System.out.println("Starting search with " + start + " (time " + tt2 + ")");
        Curve cc = c;
        return cc.getNext(tt, tt2);
    }
    
    public static Peer getFirst2(List<Curve> cs, boolean o) {
        Troc min = null, max = null;
        Double minv = null, maxv = null, mint = Double.NaN, maxt = Double.NaN;
        for (Curve c : cs) {
            if (!(c instanceof Troc)) {
                continue;
            }
            Troc t = (Troc) c;
            double[] val = t.getExtrema();
            System.out.println("Extrema of "+t+": ");
            System.out.println(t.getPoint(val[0])+"("+t.fy(val[0])+") "+t.getPoint(val[1])+"("+t.fy(val[1])+")");
            if (minv == null || minv > t.fy(val[0])) {
                minv = t.fy(val[0]);
                mint = val[0];
                min = t;
            }
            if (maxv == null || maxv < t.fy(val[1])) {
                maxv = t.fy(val[1]);
                maxt = val[1];
                max = t;
            }
        }

        System.out.println("Minimum: " + min + "v "+minv+" (" + min.getPoint(mint) + "@"+mint+")");
        System.out.println("Maximum: " + max + "v "+maxv+" (" + max.getPoint(maxt) + "@"+maxt+")");

        if (mint == 0)
            mint += Newton.PRECISION;
        else if (mint == 1)
            mint -= Newton.PRECISION;
        Double[] tt = min.getAdjacentIntersectionTimes(mint);
        Peer p = o ? min.getNext(mint, tt[1]):min.getNext(mint, tt[0]);

        return p;
    }

    public static Peer getFirst(List<Curve> cs) {
        Troc c = null;
        for (Curve o : cs) {
            if (o instanceof Troc) {
                Troc t = (Troc) o;
                if (c == null || t.b > c.b) {
                    c = t;
                }
            }
        }

        int index;
        double t1 = (-c.toff - Math.PI) / c.rot;
        while (t1 < 0) {
            t1 += 2 * Math.PI / c.rot;
        }
        if (t1 > 1) {
            System.out.println("t1 > 1 :" + t1);
            if (t1 > Math.PI / c.rot) {
                System.out.println("index0");
                t1 -= Math.PI / c.rot;
                index = 1;
            } else {
                if (t1 - 1 < Math.PI / c.rot - t1) {
                    index = 1;
                    t1 = 1d - Newton.PRECISION;
                } else {
                    index = 0;
                    t1 = 0d + Newton.PRECISION;
                }
            }
            if (c.a > c.b) {
                index = 1 - index;
            }
        } else {
            index = 1;
        }

        Double[] tt = c.getAdjacentIntersectionTimes(t1);
        Double t2 = tt[index];
        System.out.println(c + "; T1: " + t1 + "; T2: " + t2);
        Point start = c.getPoint(t2), cur = null;
        System.out.println("Starting search with " + start + " (time " + t2 + ")");
        Curve cc = c;
        return cc.getNext(t1, t2);
    }

    public static CRegion calculateTraversedArea(List<FMSeg> fmsegs) {
        CRegion r2 = new CRegion();

        for (List<Curve> cs : (List<List<Curve>>) calculate(fmsegs, TYPE_CURVE)) {
            CFace f2 = new CFace();
            for (Curve c : cs) {
                f2.addRCurve(c.getRCurve());
            }
            r2.addFace(f2);
        }

        return r2;
    }

    public static List<RCurve> calculateRCurves(List<FMSeg> fmsegs) {
        List<RCurve> ret = new LinkedList();
        double angle = fmsegs.get(0).getVector().getAngleRad();
        Point center = fmsegs.get(0).getCenter();
//        mf.orthogonalizeVector();
        for (Curve c : (List<Curve>) calculate(fmsegs, TYPE_CURVE)) {
            RCurve r = c.getRCurve();
            ret.add(r);
        }
//        mf.restoreVector();

        return ret;
    }

    public static List<Curve> calculateCurves(MFace mf) {
        return calculate(mf.getFMSegs(), TYPE_CURVE);
    }

    public static List<Point> calculatePoints(MFace mf) {
        return calculate(mf.getFMSegs(), TYPE_POINTS);
    }

    public static List<Curve> calculateCurves(FMRegion fm) {
        return calculate(fm.getFMSegs(), TYPE_CURVE);
    }

    public static List<Point> calculatePoints(FMRegion fm) {
        return calculate(fm.getFMSegs(), TYPE_POINTS);
    }

    public static List<Point> calculatePoints(List<FMSeg> fms) {
        return calculate(fms, TYPE_POINTS);
    }

    public static Newton getNewton(Curve c1, Curve c2) {

        if (false) {
            return null;
        } else if (c1 instanceof Troc && c2 instanceof Troc) {
            return new ISTrochoids((Troc) c1, (Troc) c2);
        } else if (c1 instanceof Troc && c2 instanceof Ravd) {
            return new ISTrochoids12((Troc) c1, (Ravd) c2);
        } else if (c1 instanceof Ravd && c2 instanceof Troc) {
            return new ISTrochoids12((Troc) c2, (Ravd) c1);
        } else if (c1 instanceof Ravd && c2 instanceof Ravd) {
            return new ISTrochoids2((Ravd) c2, (Ravd) c1);
        } else if ((c1 instanceof Troc || c1 instanceof Ravd) && (c2 instanceof SegT)) {
            return new ISCurveSeg(c1, (SegT) c2);
        } else if ((c2 instanceof Troc || c2 instanceof Ravd) && (c1 instanceof SegT)) {
            return new ISCurveSeg(c2, (SegT) c1);
        } else if ((c1 instanceof SegT) && (c2 instanceof SegT) && (c1 != c2)) {
            return new ISCurveSeg(c1, (SegT) c2);
        } else {
            return null;
        }
    }
}
