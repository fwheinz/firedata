package mrview;

import java.util.LinkedList;
import java.util.List;

public abstract class Newton {
    private Curve c1, c2;
    protected static double PRECISION = 0.0000001, MAXITERATIONS = 100, DX = 0.02, STEP = 0.0001;

    abstract protected double f(double x);

    public Newton(Curve c1, Curve c2) {
        this.c1 = c1;
        this.c2 = c2;
    }

    /* Override this function with the real derivative to achieve better results */
    protected double df(double x) {
        double dy = f(x + DX / 2) - f(x - DX / 2);
        return dy / DX;
    }

    public Double findroot(double start) {
        int iterations = 0;
        double x = start;
        double y = f(x);

        System.out.println("" + iterations + ": " + x + "/" + y);
        while (Math.abs(y) > PRECISION) {
            x = x - y / df(x);
            y = f(x);
            if (iterations++ > MAXITERATIONS) {
                return null;
            }
            System.out.println("" + iterations + ": " + x + "/" + y);
        }

        return x;
    }

    private Double findRootBinary(double l, double r, boolean rising) {
        double val, m;
//        System.out.println("findRootBinary: l:"+l+"; r:"+r+"; rising:"+rising);
//        System.out.println("findRootBinary: f(l):"+f(l)+"; f(r):"+f(r)+"; rising:"+rising);
        int maxiterations = 100;
        do {
            m = (l + r) / 2;
            val = f(m);
            if (val < 0 && rising || val > 0 && !rising) {
                l = m;
            } else {
                r = m;
            }
//            System.out.println("f("+m+") = "+val);
            if (maxiterations-- <= 0)
                return null;
        } while (Math.abs(val) > PRECISION);

        return m;
    }

    public List<Double> findroots(double start, double max) {
        double cur = start;
        double val = f(start);
        List<Double> ret = new LinkedList();

        if (Math.abs(val) < PRECISION) {
            val = 0;
        }
        double lastval = val;
        while (cur < max) {
            if (val != 0)
                lastval = val;
            val = f(cur);
//            if (cur > 0.09 && cur < 0.10)
//                System.out.println("XXX f("+cur+") = "+val);
            if (Math.abs(val) < PRECISION) {
                cur += STEP;
                val = 0;
            }
            if ((lastval < 0 && val > 0) || (lastval > 0) && (val < 0)) {
//                System.out.println("lv: "+lastval+" v: "+val);
                Double root = findRootBinary(cur - STEP, cur, lastval < 0);
                if (root != null)
                    ret.add(root);
            }
            cur += STEP;
        }
        

//        for (double r : ret) {
//            System.out.println("f("+r+") = "+f(r));
//        }
        return ret;
    }

    public abstract List<double[]> findIntersectionTimes();

    public List<Point> findIntersection() {
        List<Point> ret = new LinkedList();
        
        if (!c1.center.equals(c2.center) || c1.angle != c2.angle)
            throw new RuntimeException("Curves do not have same orientation!");

        System.out.println("Intersections between " + c1.getObjName() + " and " + c2.getObjName());

        List<double[]> its = findIntersectionTimes();
        for (double[] ts : its) {
            if (ts.length != 2) {
                continue;
            }
            c1.addIntersection(ts[0], ts[1], c2);
            c2.addIntersection(ts[1], ts[0], c1);
            Point p = new Point(c1.fx(ts[0]), c1.fy(ts[0])).rotate(c1.center, c1.angle);
            System.out.println("Point " + p.toString() + " at t1:" + ts[0] + " t2:" + ts[1]);
            ret.add(p);
        }
        if (ret.size() == 0) {
//            System.out.println("No intersections found!");
        }

        return ret;
    }

}
