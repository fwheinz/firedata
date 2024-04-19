/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mrview;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author sky
 */
public class MPointOld extends MovingObject {

    public static final double PRECISION = 0.0000000001;

    private final long startTime, endTime;
    private double rotate = 0.0f;
    private Point C = new Point(0, 0);
    private Point P, v, iv;

    public MPointOld(Point pt, Point vector, long startTime, long endTime) {
        this.P = pt;
        this.v = vector;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public MPointOld(Point pt) {
        this.P = pt;
        this.v = new Point(0,0);
        this.startTime = 0;
        this.endTime = 1000000;
    }

    public Point project(long currentTime) {
        if (currentTime < startTime || currentTime > endTime) {
            return null;
        }
        double frac = ((double) (currentTime - startTime)) / ((double) (endTime - startTime));

        return project(frac);
    }

    public Point project(double t) {
        double angle = getRotate() * t;
        Point v = getVector().mul(t);
        if (C == null) {
            return P.add(v);
        } else {
            return P.rotate(C.sub(v), angle).add(v);
        }
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        Point p = project(currentTime);
        if (p != null) {
            g.drawPoint(p);
        }
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public BoundingBox getBoundingBox() {
        double minx, miny, maxx, maxy;
        if (getVector().x > 0) {
            minx = getPt().x;
            maxx = getPt().x + getVector().x;
        } else {
            minx = getPt().x + getVector().x;
            maxx = getPt().x;
        }
        if (getVector().y > 0) {
            miny = getPt().y;
            maxy = getPt().y + getVector().y;
        } else {
            miny = getPt().y + getVector().y;
            maxy = getPt().y;
        }
        BoundingBox ret = new BoundingBox();
        ret.update(new Seg(new Point(minx, miny), new Point(maxx, maxy)));
        return ret;
    }

    public Seg getSeg() {
        return new Seg(P, P.add(v));
    }

    /**
     * @return the pt
     */
    public Point getPt() {
        return P;
    }

    /**
     * @param pt the pt to set
     */
    public void setPt(Point pt) {
        this.P = pt;
    }

    /**
     * @return the vector
     */
    public Point getVector() {
        return v;
    }

    /**
     * @param vector the vector to set
     */
    public void setVector(Point vector) {
        this.v = vector;
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
     * @return the center
     */
    public Point getCenter() {
        return C;
    }

    /**
     * @param center the center to set
     */
    public void setCenter(Point center) {
        this.C = center;
    }

    private double calcSine(Seg seg, double t, int derivative) {
        double sincoeff, coscoeff, tsincoeff, tcoscoeff, fixed;
        if (seg.e.x != seg.s.x) {
            double m = ((double) (seg.e.y - seg.s.y)) / ((double) (seg.e.x - seg.s.x));
            double c = -m * seg.s.x + seg.s.y;
            
            sincoeff = P.x - C.x + m * P.y - m * C.y;
            coscoeff = P.y - C.y - m * P.x + m * C.x;
            tsincoeff = v.x + m * v.y;
            tcoscoeff = -m * v.x + v.y;
            fixed = C.y - m * C.x - c;
        } else {
            sincoeff = -P.y + C.y;
            coscoeff = P.x - C.x;
            tsincoeff = -v.y;
            tcoscoeff = v.x;
            fixed = C.x - P.x;
        }
//        System.out.println("sc "+sincoeff+"  cc "+coscoeff+"  tc " + tcoeff 
//                + "  f "+fixed+"  rotate "+rotate);
//        System.out.println("(" + sincoeff + ")*sin(" + rotate + "*x)+(" + coscoeff + ")*cos(" + rotate + "*x)+("
//                + tcoeff + ")*t+(" + fixed + ")");

        if (derivative == 0) {
            return sincoeff * Math.sin(t * rotate) + coscoeff * Math.cos(t * rotate)
                    + t * tsincoeff * Math.sin(t * rotate) + t * tcoscoeff * Math.cos(t * rotate)
                    + fixed;
        } else if (derivative == 1) {
//            return t*((sincoeff+tsincoeff*t)*Math.cos(t*rotate) - (coscoeff + tcoscoeff*t)*Math.sin(t*rotate) );
            return -rotate * coscoeff * Math.sin(t * rotate) + rotate * sincoeff * Math.cos(rotate * t)
                    + tsincoeff * (Math.sin(t * rotate) + rotate * t * Math.cos(t * rotate))
                    + tcoscoeff * (Math.cos(t * rotate) - rotate * t * Math.sin(t * rotate));
        } else {
            return 0;
        }
    }

    private Double newton(Seg seg, double start) {
        double cur = start;
        double curval = calcSine(seg, cur, 0);
        int iterations = 0;
        while (Math.abs(curval) > PRECISION) {
            double delta = curval / calcSine(seg, cur, 1);
//            System.out.println("Newton: f("+cur+") = "+curval+" DELTA "+delta);
            cur = cur - delta;
            curval = calcSine(seg, cur, 0);
            if (iterations++ > 50) {
                return null;
            }
        }
//        System.out.println("Final : f("+cur+") = "+curval);

        return cur;
    }

    public void printGraph(String prefix, int linestyle) {
        if (C == null) {
            return;
        }
        String x = "" + C.x + "+(" + (P.x - C.x) + "+t*(" + v.x + "))*cos(" + rotate + "*t) - "
                + "(" + (P.y - C.y) + "+t*(" + v.y + "))*sin(" + rotate + "*t)" //                           +" - ("+v.x+"*t)"
                ;
        String y = "" + C.y + "+(" + (P.x - C.x) + "+t*(" + v.x + "))*sin(" + rotate + "*t) + "
                + "(" + (P.y - C.y) + "+t*(" + v.y + "))*cos(" + rotate + "*t)" //                           +" - ("+v.y+"*t)"
                ;
        Plot.plot(prefix, x, y, linestyle);
    }

    public void printSegGraph(Seg seg, int derivative, String prefix, int linestyle) {
        if (C == null) {
            return;
        }
        Point S = seg.s, E = seg.e;
        double sincoeff, coscoeff, tsincoeff, tcoscoeff, fixed;
        if (S.x != E.x) {
            double m = ((double) (E.y - S.y)) / ((double) (E.x - S.x));
            double c = -m * S.x + S.y;
            
            sincoeff = P.x - C.x + m * P.y - m * C.y;
            coscoeff = P.y - C.y - m * P.x + m * C.x;
            tsincoeff = v.x + m * v.y;
            tcoscoeff = -m * v.x + v.y;
            fixed = C.y - m * C.x - c;
        } else {
            System.out.println("Vertical segment: "+seg.toString());
            sincoeff = -P.y + C.y;
            coscoeff = P.x - C.x;
            tsincoeff = -v.y;
            tcoscoeff = v.x;
            fixed = C.x - S.x;
        }

        if (derivative == 0) {
            String x = "t";
            String y = "(" + sincoeff + ")*sin(" + rotate + "*t)+(" + coscoeff + ")*cos(" + rotate + "*t)"
                    + "+(" + tsincoeff + ")*t*sin(" + rotate + "*t)"
                    + "+(" + tcoscoeff + ")*t*cos(" + rotate + "*t)"
                    + "+(" + fixed + ")";
            Plot.plot(prefix, x, y, linestyle);
        } else if (derivative == 1) {
            String x = "t";
            String y = "-(" + rotate + ")*(" + coscoeff + ")*sin(" + rotate + "*t)+"
                    + "(" + rotate + ")*(" + sincoeff + ")*cos(" + rotate + "*t)";
            Plot.plot(prefix, x, y, linestyle);
        }
    }

    public List<Double> segmentIntersections(Seg seg) {
        if (C == null) {
            return new LinkedList();
        }
        List<Double> ret = new LinkedList();
        Point S = seg.s, E = seg.e;

        double init = -Math.PI;
        double step = Math.PI / Math.abs(rotate) / 2.0f;
        while (init < 1) {
            Double val = newton(seg, init);
            if (val != null && val >= 0 && val <= 1) {
                boolean inside = false;
                if (S.x == E.x) {
                    double y = C.y + (P.x - C.x + v.x * val) * Math.sin(rotate * val) + (P.y - C.y + v.y * val) * Math.cos(rotate * val);
                    double miny = Math.min(S.y, E.y);
                    double maxy = Math.max(S.y, E.y);
                    if (y >= miny && y <= maxy)
                        inside = true;
                } else {
                    double x = C.x + (P.x - C.x + v.x * val) * Math.cos(rotate * val) - (P.y - C.y + v.y * val) * Math.sin(rotate * val);
                    double minx = Math.min(S.x, E.x);
                    double maxx = Math.max(S.x, E.x);
                    if (x >= minx && x <= maxx)
                        inside = true;
                }
                if (inside) {
                    System.out.println("Found intersection at t=" + val);
                    boolean found = false;
                    for (Double d : ret) {
                        if (Math.abs(d - val) < PRECISION * 2) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        ret.add(val);
                    }
                } else {
                    System.out.println("Found out-of-range intersection at t=" + val);
//                    System.out.println(" Details: "+x+" outside ["+minx+":"+maxx+"]");
                }
            }
            init += step;
        }

        return ret;
    }

    /**
     * @return the iv
     */
    public Point getInnerVector() {
        return iv;
    }

    /**
     * @param iv the iv to set
     */
    public void setInnerVector(Point iv) {
        this.iv = iv;
    }

}
