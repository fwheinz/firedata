package mrview;

import java.util.LinkedList;
import java.util.List;

public class ISTrochoids extends Newton {
    final private Troc o1, o2;
    private boolean first;
    
    public ISTrochoids(Troc o1, Troc o2) {
        super(o1.b > o2.b ? o1 : o2, o1.b > o2.b ? o2 : o1);
        if (o1.b > o2.b) {
            this.o1 = o1;
            this.o2 = o2;
        } else {
            this.o1 = o2;
            this.o2 = o1;
        }
    }

    public ISTrochoids(Troc o) {
        super(o, o);
        this.o1 = o;
        this.o2 = o;
    }
    
    private boolean isSame () {
        return (o2 == null || o1 == o2);
    }

//    @Override
//    protected double df (double t) {
//        return ((t1.a-t1.b*Math.cos(t))* (t2.b*Math.sqrt(1- Math.pow((-t1.a+t1.b*Math.cos(t)+t2.a - t1.xoff + t2.xoff)/t2.b, 2)) -t1.b*Math.sin(t))) /
//                (t2.b*Math.sqrt(1-Math.pow((-t1.a+t1.b*Math.cos(t)+t2.a - t1.xoff + t2.xoff)/t2.b, 2)))
//                ;
//    }

    protected double getT1(double t2, boolean first) {
        if (o1 == o2 || o2 == null) {
            return ((first?0:2)*Math.PI-t2 * o1.rot - 2 * o1.toff) / o1.rot;
        } else {
            double ac = (o2.yoff - o1.yoff - o2.a + o1.a + o2.b * Math.cos(o2.toff + t2 * o2.rot)) / o1.b;
            double val = (Math.acos(ac)-o1.toff)/o1.rot;
            if (!first)
//                return (-Math.acos(ac)-o1.toff)/o1.rot;
                val = (-val*o1.rot-2*o1.toff)/o1.rot;
            while (val > 2*Math.PI/o1.rot)
                val -= 2*Math.PI/o1.rot;
            while (val < 0)
                val += 2*Math.PI/o1.rot;
            return val;
        }
    }
    
    @Override
    protected double f(double t2) {
        double ret;
        double period = 2*Math.PI/o1.rot;
        double t1 = getT1(t2, first);
//        System.out.print("t2: "+t2+"; t1: "+t1+" ");
        if (o1 == o2 || o2 == null) {
            ret = o1.a * (t1 - t2) * o1.rot + o1.b * (Math.sin(o1.toff + t2 * o1.rot) - Math.sin(o1.toff + t1 * o1.rot));
        } else {
            ret = (o1.xoff + o1.a * (o1.toff + t1 * o1.rot) - o1.b * Math.sin(o1.toff + t1 * o1.rot))
                    - (o2.xoff + o2.a * (o2.toff + t2 * o2.rot) - o2.b * Math.sin(o2.toff + t2 * o2.rot));
        }
        
        return ret;
    }

    public List<double[]> findIntersectionTimes(boolean first) {
        this.first = first;
        List<Double> roots = findroots(-1, 1);
        List<double[]> ret = new LinkedList();
        double pd = 2.0 * Math.PI / o1.rot;
        for (double root : roots) {
            double t2 = root, t1 = getT1(t2, first);
            if (isSame() && (Math.abs(t1-t2) < 0.001 || (t1 > t2)))
                continue;
            while (t1 > pd || t2 > pd) {
                t1 -= pd;
                t2 -= pd;
            }
            while (t1 < 1 && t2 < 1) {
                if (t1 > 0 && t2 > 0) {
                    ret.add(new double[] {t1, t2});
                }
                t1 += pd;
                t2 += pd;
            }
        }
        
        return ret;
    }
    
    @Override
    public List<double[]> findIntersectionTimes() {
        List<double[]> ret = new LinkedList();
        
        ret.addAll(findIntersectionTimes(true));
        ret.addAll(findIntersectionTimes(false));
        
        return ret;
    }
}