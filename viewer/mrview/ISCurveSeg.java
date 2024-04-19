package mrview;

import java.util.LinkedList;
import java.util.List;

public class ISCurveSeg extends Newton {

    final private Curve o;
    final private SegT s;
    final private double c, m;

    public ISCurveSeg(Curve o, SegT s) {
        super(o, s);
        this.o = o;
        this.s = s;
        this.m = s.getMC()[0];
        this.c = s.getMC()[1];
    }

    @Override
    protected double f(double t) {
        double ret;

        if (!Double.isNaN(m)) {
            ret = o.fy(t) - m * o.fx(t) - c;
        } else {
            ret = o.fx(t) - s.xoff;
        }

        return ret;
    }

    @Override
    public List<double[]> findIntersectionTimes() {
        List<double[]> ret = new LinkedList();
        if (o == s) {
            return ret;
        }
        List<Double> roots = findroots(0, 1);
        for (double root : roots) {
            Point p = new Point(o.fx(root), o.fy(root));
//            if (( s.pointInsideBBox(p) || (s.xd == 0 && between(p.y, s.yoff, s.yoff + s.yd))) && o.isValid(root)) {
            if (s.isValid(s.getTime(p)) && o.isValid(root)) {
                ret.add(new double[]{root, s.getTime(p)});
            }
        }

        for (double t1 : new double[] {0d, 1d}) {
            if (!o.isValid(t1))
                continue;
            if (Math.abs(f(t1)) < Newton.PRECISION) {
                Point p = new Point(o.fx(t1), o.fy(t1));
                double t2 = s.getTime(p);
                System.out.println(t1+": "+f(t1)+"; t2: "+t2);
                if (!s.isValid(t2))
                        continue;
                ret.add(new double[]{t1, t2});
                System.out.println(t1+": Added intersection point "+o.getPoint(t1)+" at times "+t1+"/"+t2);
            }
        }

        return ret;
    }

    public boolean between(double a, double b1, double b2) {
        return ((b1 <= a) && (a <= b2))
                || ((b2 <= a) && (a <= b1));
    }
}
