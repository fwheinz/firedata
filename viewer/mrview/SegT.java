package mrview;

import java.awt.Color;
import static mrview.Newton.PRECISION;

public class SegT extends Curve {

    public double xoff, yoff, xd, yd;
    private Seg seg;
    
    {
        setType("S");
    }

    public SegT(Seg seg) {
        Point start = seg.s;
        Point end = seg.e;
        this.xoff = start.x;
        this.yoff = start.y;
        this.xd = end.x - start.x;
        this.yd = end.y - start.y;
        this.seg = seg;
        setColor(Color.BLUE);
        System.out.println(this.toString());
    }

    @Override
    public double fx(double t) {
        return xoff + xd * t;
    }

    @Override
    public double fy(double t) {
        return yoff + yd * t;
    }

    public double[] getMC() {
        return seg.getMC();
    }

    public boolean pointInsideBBox(Point p) {
        return seg.pointInsideBBox(p);
    }

    public double getTime(Point p) {
        double t;

        if (xd == 0 && yd == 0) {
            return 0;
        }

        if (Math.abs(xd) > Math.abs(yd)) {
            double x = p.x - xoff;
            t = x / xd;
        } else {
            double y = p.y - yoff;
            t = y / yd;
        }
        
        // Fix small rounding errors at the borders of the time interval
        if (Math.abs(t) < PRECISION) {
            t = 0d;
        } else if (Math.abs(1d - t) < PRECISION) {
            t = 1d;
        }

        return t;
    }
    
    public double[] getParams() {
        double[] times = getTimes();
        double dt = times[1] - times[0];
        return new double[] { xd*dt, yd*dt };
    }

    @Override
    public String toString() {
        return getObjName() + ": xoff: " + xoff + "; yoff: " + yoff + "; xdelta: " + xd + "; ydelta: " + yd + ";";
    }
}
