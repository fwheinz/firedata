package mrview;

import java.awt.Color;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class FMSeg extends MovingObject implements MovingSeg {

    private long startTime, endTime;
    private Seg i, savedi;
    private double rotate;
    private Point C, v, savedv;
    private Point pp1, pp2;
    private double toff;

    public FMSeg(Seg i, Point vector, Point center, double rotation, long startTime, long endTime) {
        this.i = new Seg(i);
        this.startTime = startTime;
        this.endTime = endTime;
        this.v = vector;
        this.C = center;
        this.rotate = rotation;
        
        toff = (i.s.sub(i.e).getAngleRad() + Math.PI / 2)/rotate;
        pp1 = i.s.rotate(C, -toff * rotate).setY(C.y - v.x / rotate).rotate(C, toff * rotate);
        pp2 = i.e.rotate(C, -toff * rotate).setY(C.y + v.x / rotate).rotate(C, toff * rotate);
    }
    
    public FMSeg copy () {
        return new FMSeg(i, v, C, rotate, startTime, endTime);
    }
    
    public void normalizeRotation() {
        if (rotate >= 0)
            return;
        i = project(1d);
        C = C.add(v);
        v = v.mul(-1);
        rotate = -rotate;
    }

    public void orthogonalizeVector() {
        if (savedv != null) {
            return;
        }
        double angle = v.getAngleRad();
        savedi = i;
        i = i.rotate(C, -angle);
        savedv = v;
        v = new Point(v.length(), 0);
    }

    public void restoreVector() {
        if (savedv == null) {
            return;
        }
        v = savedv;
        savedv = null;
        i = savedi;
        savedi = null;
    }

    @Override
    public Seg project(long currentTime) {
        if (currentTime < startTime || currentTime > endTime
                || startTime >= endTime) {
            return null;
        }

        double frac = ((double) (currentTime - startTime)) / ((double) (endTime - startTime));

        return project(frac);
    }

    @Override
    public Seg project(double t) {
        double angle = rotate * t;
        Point vec = v.mul(t);

        return new Seg(i.rotate(C, angle)).translate(vec);
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox ret = new BoundingBox();
        double minx, maxx, miny, maxy;
        double sdist = i.s.dist(getCenter());
        double edist = i.e.dist(getCenter());

        Seg f = new Seg(i.s.add(v), i.e.add(v));
        
        maxx = i.s.x + sdist;
        minx = i.s.x - sdist;
        if (i.e.x - edist < minx) {
            minx = i.e.x - edist;
        }
        if (i.e.x + edist > maxx) {
            maxx = i.e.x + edist;
        }
        if (f.s.x - sdist < minx) {
            minx = f.s.x - sdist;
        }
        if (f.s.x + sdist > maxx) {
            maxx = f.s.x + sdist;
        }
        if (f.e.x - edist < minx) {
            minx = f.e.x - edist;
        }
        if (f.e.x + edist > maxx) {
            maxx = f.e.x + edist;
        }

        maxy = i.s.y + sdist;
        miny = i.s.y - sdist;
        if (i.e.y - edist < miny) {
            miny = i.e.y - edist;
        }
        if (i.e.y + edist > maxy) {
            maxy = i.e.y + edist;
        }
        if (f.s.y - sdist < miny) {
            miny = f.s.y - sdist;
        }
        if (f.s.y + sdist > maxy) {
            maxy = f.s.y + sdist;
        }
        if (f.e.y - edist < miny) {
            miny = f.e.y - edist;
        }
        if (f.e.y + edist > maxy) {
            maxy = f.e.y + edist;
        }

        ret.update(new Seg(new Point(minx, miny), new Point(maxx, maxy)));
        return ret;
    }

    public void paintRavdoid(MRGraphics g, long currentTime, boolean highlight) {
        if (currentTime < getStartTime() || currentTime > getEndTime()
                || getStartTime() >= getEndTime()) {
            return;
        }

        double t = ((double) (currentTime - getStartTime())) / ((double) (getEndTime() - getStartTime()));
        Seg s = project(t);
        if (s != null) {
            g.drawLine(s, highlight ? Color.RED : Color.BLACK);
        
            Point p1p = pp1.rotate(C, rotate * t).add(v.mul(t));
            Point p2p = pp2.rotate(C, rotate * t).add(v.mul(t));
            g.drawLine(new Seg(p1p, p2p), Color.green, 3, 10);
            double tt = (1 - Math.cos((t + toff) * rotate)) / 2;
            Point m = pp2.add(pp1.sub(pp2).mul(tt)).rotate(C, rotate * t).add(v.mul(t));
            g.drawPoint(m, 3, Color.BLUE);
        }
    }
    
    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        Seg s = project(currentTime);
        if (s != null) {
            g.drawLine(s, highlight ? Color.RED : Color.BLACK);
        }
    }

    public void printGraph(String prefix, int linestyle) {
        Point S = getI().s;

        long Cx = Math.round(getC().x);
        long Cy = Math.round(getC().y);
        long Sx = Math.round(S.x);
        long Sy = Math.round(S.y);
        long vx = Math.round(getV().x);
        long vy = Math.round(getV().y);
        long rot = Math.round(getRotate());

        String x = "" + Cx + "+(" + (Sx - Cx) + ")*cos(" + rot + "*t) - "
                + "(" + (Sy - Cy) + ")*sin(" + rot + "*t)"
                + " + (" + vx + "*t)";
        String y = "" + Cy + "+(" + (Sx - Cx) + ")*sin(" + rot + "*t) + "
                + "(" + (Sy - Cy) + ")*cos(" + rot + "*t)"
                + " + (" + vy + "*t)";
        Plot.plot(prefix, x, y, linestyle);
    }

    public void printGraphNormalized(String prefix, int linestyle) {
        Point S = getI().s;

        long Cx = Math.round(getC().x);
        long Cy = Math.round(getC().y);
        long Sx = Math.round(S.x);
        long Sy = Math.round(S.y);
        long vx = Math.round(getV().x);
        long vy = Math.round(getV().y);
        long rot = Math.round(getRotate());

        double a = ((double) vx) / ((double) rot);
        double b = Math.sqrt((Sx - Cx) * (Sx - Cx) + (Sy - Cy) * (Sy - Cy));
        double atan = Math.PI * 2.5 - Math.atan2(Sy - Cy, Sx - Cx);
        while (atan >= 2 * Math.PI) {
            atan -= 2 * Math.PI;
        }
//        rot = 1;

        System.out.println("Atan2 " + Sx + "/" + Sy + " is " + atan);
        double ph = 0;

//        String x = (a*rot)+"*(t+("+0+"))-("+b+")*sin("+rot+"*(t+("+ph+")))";
//        String y = (a*rot)+"-("+b+")*cos("+rot+"*(t+("+ph+")))*(-1)";
        String x = (a * rot) + "*(t+(" + 0 + "))-(" + b + ")*sin(" + rot + "*t+(" + ph + "))";
        String y = (a * 0) + "-(" + b + ")*cos(" + rot + "*t+(" + ph + "))*(-1)-200";

//        String x = a+" * arccos(1 - (t / " + a + ")) - sqrt(2 * " + a + " * t - t*t)";
//        String y = "t";
        Plot.plot(prefix, x, y, 3);
    }

    public Trochoid getTrochoid(double sign, Point offset) {
        Point P = getI().s;

        double a = getV().x / getRotate();
        double b = Math.sqrt((P.x - getC().x) * (P.x - getC().x) + (P.y - getC().y) * (P.y - getC().y));
        double ph = -Math.atan2(P.y - C.y, P.x - C.x);
//        double ph = Math.PI*2.5 - Math.atan2(P.y-C.y, P.x-C.x);
        while (ph >= 2 * Math.PI) {
            ph -= 2 * Math.PI;
        }
        
        return new Trochoid(a, b, ph, sign, offset);
    }

    public Troc getTroc(boolean start) {
        Point P = start ? getI().s : getI().e;
        double vangle = getV().getAngleRad();
        P = P.rotate(getC(), -vangle);
        double vx = getV().length();

        double a = vx / getRotate();
        double b = Math.sqrt((P.x - getC().x) * (P.x - getC().x) + (P.y - getC().y) * (P.y - getC().y));
        double ph = Math.atan2(P.y - getC().y, P.x - getC().x);
        if (ph < -Math.PI/2) {
//            ph += 2*Math.PI;
        }

        Troc ret = new Troc(a, b, -ph * a + getC().x + vx / getRotate() * Math.PI / 2, a + getC().y, ph - Math.PI / 2, getRotate());
        ret.setTransformation(getC(), vangle);
        ret.setStartTime(startTime);
        ret.setEndTime(endTime);
        return ret;
    }

    public Troc getTroc() {
        return getTroc(false);
    }
    
    public Ravd getRavd() {
        Ravd ret = new Ravd(this);
        ret.setStartTime(startTime);
        ret.setEndTime(endTime);
        return ret;
    }
    
    public SegT getSegT(boolean start) {
        Seg s = this.project(start ? 0.0d : 1.0d);
        double vangle = getV().getAngleRad();
        s = s.rotate(getC(), -vangle);
        SegT ret = new SegT(s);
        ret.setTransformation(getC(), vangle);
        return ret;
    }

    public void printCartesianGraph(String prefix, int linestyle) {
        getTrochoid(1, null).cartesianGraph(prefix, linestyle);
    }

    public String toString() {
        return " ( " + getInitial() + " => " + getVector().toString() + "@" + getRotation() * 360 / (2 * Math.PI) + "° ) ";
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
     * @return the rotation
     */
    public double getRotation() {
        return getRotate();
    }

    /**
     * @param rotation the rotation to set
     */
    public void setRotation(double rotation) {
        this.setRotate(rotation);
    }

    /**
     * @return the center
     */
    public Point getCenter() {
        return getC();
    }

    /**
     * @param center the center to set
     */
    public void setCenter(Point center) {
        this.setC(center);
    }

    /**
     * @return the vector
     */
    public Point getVector() {
        return getV();
    }

    /**
     * @param vector the vector to set
     */
    public void setVector(Point vector) {
        this.setV(vector);
    }

    Point getProjectedCenter(long currentTime) {
        double frac = ((double) (currentTime - getStartTime())) / ((double) (getEndTime() - getStartTime()));
        return getC().add(getV().mul(frac));
    }

    /**
     * @return the i
     */
    public Seg getInitial() {
        return getI();
    }

    /**
     * @param i the i to set
     */
    public void setInitial(Seg i) {
        this.setI(i);
    }

    /**
     * @return the i
     */
    public Seg getI() {
        return i;
    }

    /**
     * @param i the i to set
     */
    public void setI(Seg i) {
        this.i = i;
    }

    /**
     * @return the rotate
     */
    public double getRotate() {
        return rotate;
    }

    /**
     * @param rotate the rotate to set
     */
    public void setRotate(double rotate) {
        this.rotate = rotate;
    }

    /**
     * @return the C
     */
    public Point getC() {
        return C;
    }

    /**
     * @param C the C to set
     */
    public void setC(Point C) {
        this.C = C;
    }

    /**
     * @return the v
     */
    public Point getV() {
        return v;
    }

    /**
     * @param v the v to set
     */
    public void setV(Point v) {
        this.v = v;
    }
}
