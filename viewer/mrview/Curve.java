package mrview;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class Curve extends MovingObject implements Cloneable {

    protected SortedMap<Double, List<Peer>> is = new TreeMap();
    private String type = "U";
    private int steps = 2000;
    private double t0 = 0.0, tn = 1.0;
    private boolean visible = true;
    private Color color = Color.BLUE;
    public Point center = new Point(0, 0);
    public double angle = 0d;
    private long startTime;
    private long endTime;

    private int _instance;
    private static Map<Class, Integer> instances = new HashMap();

    {
        Integer instance = instances.get(getClass());
        instance = instances.get(null);
        if (instance == null) {
            instance = 1;
        }
        _instance = instance++;
        instances.put(getClass(), instance);
        instances.put(null, instance);
    }

    public abstract double fx(double t);

    public abstract double fy(double t);

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        double frac = 0.0;
        if (startTime == endTime) {
            if (startTime == currentTime)
                frac = 1.0;
        } else {
            frac = ((double)(currentTime-startTime))/((double)(endTime-startTime));
        }
        paint(g, frac, highlight, null);
    }
    
    public void paint(MRGraphics g, double frac, boolean highlight, Color invalidColor) {
        if (this instanceof SegT)
            frac = 1.0;
        double step = (tn - t0) / steps * frac;
        Point prev = null;
        for (int i = 0; i < steps; i++) {
            double t = t0 + step * i;
            double x = fx(t);
            double y = fy(t);
            boolean valid = isValid(t);
            if (!valid || color == null) {
                if (invalidColor == null) {
                    prev = null;
                    continue;
                }
            }
            Point cur = getPoint(t);
            if (prev == null) {
                g.drawPoint(cur, 0, valid ? color:invalidColor);
            } else {
                g.drawLine(new Seg(prev, cur), valid ? color:invalidColor);
            }
            prev = cur;
        }
    }

    @Override
    public long getStartTime() {
        return -1;
    }

    @Override
    public long getEndTime() {
        return -1;
    }

    public double[] getTimes() {
        return new double[]{t0, tn};
    }

    public void setTimes(double t0, double tn) {
        this.t0 = t0;
        this.tn = tn;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Point getPoint(double t) {
        return new Point(fx(t), fy(t)).rotate(center, angle);
    }

    public boolean isValid(double t) {
        return ((t >= -Newton.PRECISION) && (t <= 1d + Newton.PRECISION));
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setColorRandom() {
        Random r = new Random();
        this.color = new Color(r.nextInt(0xc0), r.nextInt(0xc0), r.nextInt(0xc0));
    }

    void addIntersection(Double t, Double t2, Curve c) {
        List<Peer> cl = is.get(t);
        if (cl == null) {
            cl = new LinkedList();
            is.put(t, cl);
        }
        cl.add(new Peer(t2, c));
    }

    public Curve restrict(double t1, double t2) {
        try {
            Curve c = (Curve) this.clone();
            c.setTimes(t1, t2);
            return c;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getObjName() {
        String ret = null;
        return ret == null ? getClass().getSimpleName() + "#" + _instance : ret;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    final double delta = 0.0000001;

    public double getAngle(double t, boolean fromearlier) {
        Point p1, p2;
        if (fromearlier) {
            p1 = getPoint(t - delta);
            p2 = getPoint(t);
        } else {
            p1 = getPoint(t + delta);
            p2 = getPoint(t);
        }
        
        double a = new Seg(p1, p2).getAngle();
//        System.out.println("A1: "+p1+"; 2: "+p2+" Angle: "+a);

        return a;
    }

    public double getAngle(double t, double t2) {
        double d = t2 > t ? delta : -delta;
        Point p1 = getPoint(t);
        Point p2 = getPoint(t + d);
        double a = new Seg(p1, p2).getAngle();
//        System.out.println("B1: "+p1+"; 2: "+p2+" Angle: "+a);
        
        return a;
    }

    public static double getDeltaAngleCW(double a1, double a2) {
        double ret = a1 + 180 - a2;
        if (ret < 0) {
            ret += 360;
        } else if (ret > 360) {
            ret -= 360;
        }
        return ret;
    }

    public Peer getNext(Double t1, Double t2) {
        double a1 = getAngle(t2, t1 < t2);
        Double bestangle = null;
        Peer bestPeer = null;

        List<Peer> peers = is.get(t2);
        for (Peer p : peers) {
            Double[] tts = p.c.getAdjacentIntersectionTimes(p.t);
            System.out.println("Candidate-Peer: "+p);
            for (int i = 0; i < 2; i++) {
                Double tt = tts[i];
                System.out.println("tt: "+tt);
                if (tt == null) {
                    continue;
                }
                double a2 = p.c.getAngle(p.t, tt);
                double a = getDeltaAngleCW(a1, a2);
                if (a < 0.0001)
                    continue;
                System.out.println("Angle: "+a+"; a1:"+a1+" a2:"+a2);
                if (bestangle == null || a < bestangle) {
                    bestangle = a;
                    bestPeer = new Peer(p.t, tt, p.c);
                }
            }
        }

        return bestPeer;
    }
    
    public Double[] getAdjacentIntersectionTimes(Double t) {
        Set<Double> keySet = is.keySet();
        Iterator<Double> i = keySet.iterator();
        Double tprev = null, tnext = null;
        boolean found = false;
        while (i.hasNext()) {
            Double tmp = i.next();
            if (tmp >= t) {
                if (tmp > t) {
                    tnext = tmp;
                }
                found = true;
                break;
            }
            tprev = tmp;
        }
        if (found && i.hasNext() && tnext == null) {
            tnext = i.next();
        }
        if (tprev != null && !isValid((t + tprev) / 2)) {
            tprev = null;
        }
        if (tnext != null && !isValid((t + tnext) / 2)) {
            tnext = null;
        }

        return new Double[]{tprev, tnext};
    }
    
    public List<Curve> getAdjacentCurves() {
        List<Curve> ret = new LinkedList();
        for (List<Peer> ps : is.values()) {
            for (Peer p : ps) {
                ret.add(p.c);
            }
        }
        
        return ret;
    }

    public double[] getParams() {
        return null;
    }

    public RCurve getRCurve() {
        if (type.equals("S") && false) { // Special case for straight lines; good idea?
            double[] p = getParams();
            Point a = new Point(p[0], p[1]);
            RCurve r = new RCurve(new Point(fx(t0), fy(t0)), type, new double[] {a.length()}, center, angle);
            r.setAngle(r.getAngle() + a.getAngleRad());
            
            return r;
        } else {
            return new RCurve(new Point(fx(t0), fy(t0)), type, getParams(), center, angle);
            
        }
    }

    public void setTransformation(Point center, double angle) {
        this.center = center;
        this.angle = angle;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public static class Peer {

        public double t, t2;
        public Curve c;

        public Peer(double t, Curve c) {
            this.t = t;
            this.c = c;
        }

        public Peer(double t, double t2, Curve c) {
            this.t = t;
            this.t2 = t2;
            this.c = c;
        }

        public String toString() {
            return c.getObjName() + ": t1: " + t + "; t2: " + t2;
        }
    }
}
