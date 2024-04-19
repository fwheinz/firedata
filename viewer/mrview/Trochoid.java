package mrview;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Trochoid {
    double a, b, ph, sign;
    Point offset;
    
    public List<Double> intersections = new LinkedList();

    public Trochoid(double a, double b, double ph, double sign, Point offset) {
        this.a = a;
        this.b = b;
        this.ph = ph;
        this.sign = sign;
        this.offset = offset;
    }
    
    public String toString() {
        return "a: "+a+"; b: "+b+"; ph: "+ph;
    }

    public double f(double x) {
        Trochoid m = this;
        return m.sign * (m.a * Math.acos(-x / m.b) - Math.sqrt(m.b * m.b - x * x)) + m.ph * m.a;
    }

    public double x1(Trochoid t, double x) {
        Trochoid m = this;

        double p = m.ph;
//        if (t.ph - p > Math.PI) {
//            p += 2 * Math.PI;
//        } else if (p - t.ph > Math.PI) {
//            p -= 2 * Math.PI;
//        }

        return (m.sign * (m.a * Math.acos(-x / m.b) - Math.sqrt(m.b * m.b - x * x)) + p * m.a)
                - (t.sign * (t.a * Math.acos(-x / t.b) - Math.sqrt(t.b * t.b - x * x)) + t.ph * t.a);
    }

    public double x1d(Trochoid t, double x) {
        Trochoid m = this;

        return m.sign * m.a / (m.b * Math.sqrt(1 - (x * x) / (m.b * m.b)))
                - t.sign * t.a / (t.b * Math.sqrt(1 - (x * x) / (t.b * t.b)))
                + x / Math.sqrt(m.b * m.b - x * x)
                - x / Math.sqrt(t.b * t.b - x * x);
    }

    public List<Point> newton(Trochoid t) {
        List<Point> ret = new LinkedList();
        int iterations = 0;

//        double x = 0;
//        double val = x1(t, x, sign1, sign2);
//        while (Math.abs(val) > 0.000001) {
//            double x1dval = x1d(t, x, sign1, sign2);
//            double delta = val / x1dval;
//            x -= delta;
//            val = x1(t, x, sign1, sign2);
//            System.out.println(x+" / "+val+"  Delta: "+delta+"  X1DVal: "+x1dval);
//            if (iterations++ > 50) {
//                val = Double.NaN;
//                break;
//            }
//        }
        
        double range = Math.min(Math.abs(b), Math.abs(t.b));
        double step = range / 100000;
        double x = -range+step;
        double val = Double.NaN;
        double prec = 0.01;
        double recur = 2*Math.PI*a;
        intersections.add(-Math.abs(b));
        while (x < range) {
            val = x1(t, x);
            while (val > recur)
                val -= recur;
            while (val < -recur)
                val += recur;
            if (Math.abs(val) <= prec) {
                intersections.add(x);
                t.intersections.add(x);
                break;
            }
            x += step;
        }
        intersections.add(Math.abs(b));

        if (Math.abs(val) > prec)
            val = Double.NaN;
        
//        double p = ph;
//        if (t.ph - p > Math.PI) {
//            p += 2 * Math.PI;
//        } else if (p - t.ph > Math.PI) {
//            p -= 2 * Math.PI;
//        }
//        System.out.print(sign1+"*("+a+"*acos(-x/"+b+") - sqrt("+b*b+"-x*x))+("+p+")*("+a+") , ");
//        System.out.println(sign2+"*("+t.a+"*acos(-x/"+t.b+") - sqrt("+t.b*t.b+"-x*x))+("+t.ph+")*("+t.a+")");
        if (!Double.isNaN(val)) {
            double y = f(x);
            System.out.println("Found intersection " + (offset.x+y) + "/" + (offset.y - x) + " (val: "+val+")");
            ret.add(new Point(offset.x+y, offset.y-x));
            
            double x1 = Math.abs(b);
            double y1 = f(x1);
            ret.add(new Point(offset.x+y1, offset.y-x1));
            double x2 = -Math.abs(b);
            double y2 = f(x2);
            ret.add(new Point(offset.x+y2, offset.y-x2));
        } else {
//            System.out.println("Found no intersection");
        }

        return ret;
    }

    public void cartesianGraph(String prefix, int linestyle) {
        Plot.add("set xr [-120:120]");
        Plot.add("set yr [-200:700]");
        Plot.defun("tro1(x,a,b,ph)", "a*acos(-x/b)-sqrt(b*b-x*x)+ph*a");
        Plot.defun("tro2(x,a,b,ph)", "-(a*acos(-x/b)-sqrt(b*b-x*x))+ph*a");

        String x = "tro1(x," + a + "," + b + "," + ph + ")";
        Plot.plot("c1" + prefix, x, 3);
        x = "tro2(x," + a + "," + b + "," + ph + ")";
        Plot.plot("c2" + prefix, x, 3);

        x = "tro1(x," + a + "," + b + "," + (ph + 2 * Math.PI) + ")";
        Plot.plot("c1a" + prefix, x, 3);
        x = "tro2(x," + a + "," + b + "," + (ph + 2 * Math.PI) + ")";
        Plot.plot("c2a" + prefix, x, 3);
    }
}
