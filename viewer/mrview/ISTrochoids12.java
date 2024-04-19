package mrview;

import java.util.LinkedList;
import java.util.List;

public class ISTrochoids12 extends Newton {

    private final Troc o1;
    private final Ravd o2;
    private boolean first;

    public ISTrochoids12(Troc o1, Ravd o2) {
        super(o1, o2);
        this.o1 = o1;
        this.o2 = o2;
    }

    public double getT1(double t2) {
        double ac = (o2.hp * Math.cos(2 * (o2.toff + t2 * o2.rot))
                + o2.cd * Math.sin(o2.toff + t2 * o2.rot)
                + o2.yoff
                - o1.yoff + o1.a) / o1.b;
        double val = (Math.acos(ac) - o1.toff) / o1.rot;
        if (!Double.isNaN(val) && !first) {
            val = -val - 2 * o1.toff / o1.rot;
        }

        return val;
    }

    @Override
    public double f(double t2) {
        double t1 = getT1(t2);

        t1 = o1.toff + t1 * o1.rot;
        t2 = o2.toff + t2 * o2.rot;

        return (o1.xoff + o1.a * t1 - o1.b * Math.sin(t1))
                - (o2.hp * (2 * t2 - Math.sin(2 * t2)) + o2.cd * Math.cos(t2) + o2.xoff);
    }

    public List<double[]> findIntersectionTimes(boolean first) {
        this.first = first;
        List<Double> roots = findroots(-1, 1);
        double pd = 2.0 * Math.PI / o1.rot;

        List<double[]> ret = new LinkedList();
        for (double root : roots) {
            double t2 = root, t1 = getT1(t2);
            while (t1 >= pd && t2 >= pd) {
                t1 -= pd;
                t2 -= pd;
            }
            while (t1 < 1 && t2 < 1) {
                if (t1 > 0 && t2 > 0 && o2.isValid(t2)) {
                    ret.add(new double[]{t1, t2});
                }
                t1 += pd;
                t2 += pd;
            }
        }

        return ret;
    }

    public List<double[]> findIntersectionTimesTouch() {
        Double tmin = o2.tmin / o2.rot;
        Double tmax = o2.tmax / o2.rot;
        Double tmin2 = o2.tmin2 != null ? o2.tmin2 / o2.rot : null;
        Double tmax2 = o2.tmax2 != null ? o2.tmax2 / o2.rot : null;
        double pd = 2.0 * Math.PI / o1.rot;
        while (tmin > pd) {
            tmin -= pd;
        }
        while (tmax > pd) {
            tmax -= pd;
        }

        List<double[]> ret = new LinkedList();

        for (Double tt : new Double[]{tmin, tmax, tmin2, tmax2}) {
            if (tt == null)
                continue;
            if (o1.getPoint(tt).nequals(o2.getPoint(tt))) {
                while (tt < 1) {
                    if (tt >= 0) {
                        ret.add(new double[]{tt, tt});
                    }
                    System.out.println("Added tt: " + tt);
                    tt += pd;
                }
            }
        }

        return ret;
    }

    @Override
    public List<double[]> findIntersectionTimes() {
        List<double[]> ret = new LinkedList();

        ret.addAll(findIntersectionTimes(true));
        ret.addAll(findIntersectionTimes(false));
        ret.addAll(findIntersectionTimesTouch());

        return ret;
    }
}
