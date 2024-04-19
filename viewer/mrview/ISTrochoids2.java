package mrview;

import java.util.LinkedList;
import java.util.List;

public class ISTrochoids2 extends Newton {
    private Ravd o1;
    private Ravd o2;
    private boolean first, q2;

    public ISTrochoids2(Ravd o1, Ravd o2) {
        super(o1, o2);
        this.o1 = o1;
        this.o2 = o2;
    }
    
    public ISTrochoids2(Ravd o) {
        super(o, o);
        this.o1 = o;
        this.o2 = o;
    }
    
    private boolean isSame () {
        return (o2 == null || o1 == o2);
    }
    
    public double getT1(double t2) {
        double qq = o2.hp*Math.cos(2*(t2*o2.rot+o2.toff))+o2.cd*Math.sin(t2*o2.rot+o2.toff)+o2.yoff;
        double as = (- o1.cd + (q2?1:-1) * // +
                Math.sqrt(o1.cd*o1.cd+8*o1.hp*(o1.hp+o1.yoff-qq)))/(-4*o1.hp);
        double val = (Math.asin(as)-o1.toff)/o1.rot;
        if (!Double.isNaN(val) && !first) {
//            val = (Math.PI-Math.asin(as)-o1.toff)/o1.rot;
            val = Math.PI/o1.rot-val-2*o1.toff/o1.rot;
        }

        return val;
    }

    public double f(double t2) {
        double t1 = getT1(t2);
        
        t1 = o1.toff + t1 * o1.rot;
        t2 = o2.toff + t2 * o2.rot;
        
        double r =   (o1.hp * (2*t1 - Math.sin(2*t1)) + o1.cd * Math.cos(t1) + o1.xoff) 
                   - (o2.hp * (2*t2 - Math.sin(2*t2)) + o2.cd * Math.cos(t2) + o2.xoff);
        
        return r;
    }

    public List<double[]> findIntersectionTimes(boolean first, boolean q2) {
        this.first = first;
        this.q2 = q2;
        List<Double> roots = findroots(-1, 1);

        List<double[]> ret = new LinkedList();
        double pd = 2.0*Math.PI/o1.rot;
        for (double root : roots) {
            double t2 = root, t1 = getT1(t2);
            if (isSame() && (Math.abs(t1-t2) < 0.001 || (t1 > t2)))
                continue;
            while (t1 >= pd && t2 >= pd) {
                t1 -= pd;
                t2 -= pd;
            }
            while (t1 < 1 && t2 < 1) {
                if (t1 > 0 && t2 > 0 && (o1.isValid(t1) && o2.isValid(t2)) && (!isSame() || (t1 < t2))) { // XXX
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

        ret.addAll(findIntersectionTimes(false, false));
        ret.addAll(findIntersectionTimes(false, true));
        ret.addAll(findIntersectionTimes(true, false));
        ret.addAll(findIntersectionTimes(true, true));

        return ret;
    }
}
