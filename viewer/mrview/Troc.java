package mrview;

import java.awt.Color;

public class Troc extends Curve {
    public double xoff, yoff, toff, a, b, rot;
    public static Color[] colors = { Color.RED, Color.GREEN, Color.CYAN };
    public static int coloridx = 0;
    
    {
        setType("T");
    }

    public Troc (double a, double b, double xoff, double yoff, double toff, double rot) {
        this.xoff = xoff;
        this.yoff = yoff;
        this.a = a;
        this.b = b;
        this.toff = toff;
        this.rot = rot;
        setColor(Color.RED);
//        setColor(colors[(coloridx++)%colors.length]);
        System.out.println(this.toString());
    }
    
    public double[] getParams() {
        double[] times = getTimes();
        double t0 = times[0];
        double dt = times[1] - times[0];
        return new double[] {a, b, toff+t0*rot, rot*dt};
    }
        
    @Override
    public double fx (double t) {
        t = toff + t * rot;
        return xoff + (a*t - b * Math.sin(t));
    }
    
    @Override
    public double fy (double t) {
        t = toff + t * rot;
        return (yoff - (a - b*(Math.cos(t))));
    }
    public double[] getExtrema () {
        double toff2 = toff;
        if (toff2 < -Math.PI/2) {
            toff2 += Math.PI;
        }
        double t1 = -toff2/rot;
        Double y1 = null;
        double t2 = (Math.PI - toff2)/rot;
        Double y2 = null;
        
        System.out.println("T1: "+t1+"; T2: "+t2);
        
        if (t1 > 0 && t1 < 1) {
            y1 = fy(t1);
        }
        if (t2 > 0 && t2 < 1) {
            y2 = fy(t2);
        }
        
        if (y1 == null && y2 == null) {
            t1 = 0;
            y1 = fy(t1);
            t2 = 1;
            y2 = fy(t2);
        } else if (y1 == null) {
            double y1a = fy(0);
            double y1b = fy(1);
            if (y1a > y2) {
                t1 = y1a > y1b ? 0 : 1;
                y1 = y1a > y1b ? y1a : y1b;
            } else {
                t1 = y1a < y1b ? 0 : 1;
                y1 = y1a < y1b ? y1a : y1b;
            }
        } else if (y2 == null) {
            double y2a = fy(0);
            double y2b = fy(1);
            if (y2a > y1) {
                t2 = y2a > y2b ? 0 : 1;
                y2 = y2a > y2b ? y2a : y2b;
            } else {
                t2 = y2a < y2b ? 0 : 1;
                y2 = y2a < y2b ? y2a : y2b;
            }
        }
        if (y1 < y2)
            return new double[] { t1, t2 };
        else
            return new double[] { t2, t1 };
    }
    
    @Override
    public String toString() {
        return getObjName()+": a: "+a+"; b: "+b+"; xoff: "+xoff+"; yoff: "+yoff+"; toff: "+toff+"; rot: "+rot;
    }
}
