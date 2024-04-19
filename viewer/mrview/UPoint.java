package mrview;

import java.awt.Color;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class UPoint extends Curve {

    private Interval iv = new Interval();
    private Point s, v;
    private Point c;
    private double rot;

    public UPoint(Point s, Point v, long startTime, long endTime) {
        this.s = s;
        this.v = v;
        iv.setStart(startTime);
        iv.setEnd(endTime);
        iv.setLeftClosed(true);
        iv.setRightClosed(true);
    }

    public UPoint(Point s, Point v, Interval iv) {
        this.s = s;
        this.v = v;
        this.iv = iv;
    }

    public UPoint restrict(long startTime, long endTime) {
        Point s = project(startTime, false);
        Point e = project(endTime, false);
        Interval iv = new Interval(startTime, endTime,
                (startTime == this.iv.getStart()) ? this.iv.isLeftClosed() : true,
                (endTime == this.iv.getEnd()) ? this.iv.isRightClosed() : true);
        UPoint ret = new UPoint(s, e.sub(s), iv);

        return ret;
    }

    public double fx(double t) {
        return project(t).x;
    }

    public double fy(double t) {
        return project(t).y;
    }

    public Point project(double t) {
        Point vec = v.mul(t);
        Point p = s;
        if (c != null) {
            p = p.rotate(c.sub(vec), rot * t);
        }
        return p.add(vec);
    }

    public Point project(long currentTime, boolean checkInside) {
        if (!checkInside || iv.inside(currentTime)) {
            return project(iv.getFrac(currentTime));
        }
        return null;
    }

    public Point project(long currentTime) {
        return project(currentTime, true);
    }

    public void transform(FMRegionTrans tu) {
        double a0 = tu.getA0();

        s = s.sub(tu.getV0()).rotate(tu.getCenter(), -a0);
        v = v.sub(tu.getV()).rotate(-a0);
        c = tu.getCenter();
        rot = -tu.getA();
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        Point p = project(currentTime);
        if (p != null) {
            g.drawPoint(p);
//            if (c != null) {
//                g.drawPoint(c.sub(v.mul(iv.getFrac(currentTime))), 3, Color.MAGENTA);
//            }
        }
    }
    
    public void paintTrajectory(MRGraphics g) {
        g.drawLine(new Seg(s, s.add(v)), Color.BLUE);
    }

    @Override
    public long getStartTime() {
        return iv.getStart();
    }

    @Override
    public long getEndTime() {
        return iv.getEnd();
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox ret = new BoundingBox();
        ret.update(new Seg(s, v.add(s)));
        return ret;
    }

    /**
     * @return the s
     */
    public Point getS() {
        return s;
    }

    /**
     * @param s the s to set
     */
    public void setS(Point s) {
        this.s = s;
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

    public NL toNL() {
        NL nl = new NL();
        nl.addNL(iv.toNL());
        NL p = nl.nest();
        p.addNr(s.x);
        p.addNr(s.y);
        p.addNr(s.x + v.x);
        p.addNr(s.y + v.y);

        return nl;
    }

    public static UPoint fromNL(NL nl) {
        Interval iv = Interval.fromNL(nl.get(0));
        Point fp = new Point(nl.get(1).get(0).getNr(), nl.get(1).get(1).getNr());
        Point sp = new Point(nl.get(1).get(2).getNr(), nl.get(1).get(3).getNr());

        UPoint ret = new UPoint(fp, sp.sub(fp), iv);

        return ret;
    }

    /**
     * @return the iv
     */
    public Interval getIv() {
        return iv;
    }

    /**
     * @param iv the iv to set
     */
    public void setIv(Interval iv) {
        this.iv = iv;
    }

    public Seg getSeg() {
        return new Seg(s, s.add(v));
    }
    
    
    public void printGraph(String prefix, int linestyle) {
        if (c == null) {
            return;
        }
        String x = "" + c.x + "+(" + (s.x - c.x) + "+t*(" + v.x + "))*cos(" + rot + "*t) - "
                + "(" + (s.y - c.y) + "+t*(" + v.y + "))*sin(" + rot + "*t)" //                           +" - ("+v.x+"*t)"
                ;
        String y = "" + c.y + "+(" + (s.x - c.x) + "+t*(" + v.x + "))*sin(" + rot + "*t) + "
                + "(" + (s.y - c.y) + "+t*(" + v.y + "))*cos(" + rot + "*t)" //                           +" - ("+v.y+"*t)"
                ;
        Plot.plot(prefix, x, y, linestyle);
    }

    public void printSegGraph(Seg seg, int derivative, String prefix, int linestyle) {
        if (c == null) {
            return;
        }
        Point S = seg.s, E = seg.e;
        double sincoeff, coscoeff, tsincoeff, tcoscoeff, fixed;
        if (S.x != E.x) {
            double m = ((double) (E.y - S.y)) / ((double) (E.x - S.x));
            double c2 = -m * S.x + S.y;
            
            sincoeff = s.x - c.x + m * s.y - m * c.y;
            coscoeff = s.y - c.y - m * s.x + m * c.x;
            tsincoeff = v.x + m * v.y;
            tcoscoeff = -m * v.x + v.y;
            fixed = c.y - m * c.x - c2;
        } else {
            System.out.println("Vertical segment: "+seg.toString());
            sincoeff = -s.y + c.y;
            coscoeff = s.x - c.x;
            tsincoeff = -v.y;
            tcoscoeff = v.x;
            fixed = c.x - S.x;
        }

        if (derivative == 0) {
            String x = "t";
            String y = "(" + sincoeff + ")*sin(" + rot + "*t)+(" + coscoeff + ")*cos(" + rot + "*t)"
                    + "+(" + tsincoeff + ")*t*sin(" + rot + "*t)"
                    + "+(" + tcoscoeff + ")*t*cos(" + rot + "*t)"
                    + "+(" + fixed + ")";
            Plot.plot(prefix, x, y, linestyle);
        } else if (derivative == 1) {
            String x = "t";
            String y = "-(" + rot + ")*(" + coscoeff + ")*sin(" + rot + "*t)+"
                    + "(" + rot + ")*(" + sincoeff + ")*cos(" + rot + "*t)";
            Plot.plot(prefix, x, y, linestyle);
        }
    }
    
    MPointOld getMPointOld () {
        return new MPointOld(s, v, iv.getStart(), iv.getEnd());
    }
    
}
