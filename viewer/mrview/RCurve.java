package mrview;

import java.awt.Color;

public class RCurve extends Curve {

    private Point start;
    private double angle;
    private String type;
    private double[] params;

    public RCurve(Point start, double angle, String type, double[] params) {
        this.start = start;
        this.type = type;
        this.params = params;
        this.angle = angle;
    }
    
    public RCurve(Point start, String type, double[] params, Point center, double angle) {
        this.start = start;
        this.type = type;
        this.params = params;
        rotate (angle, center);
    }
    
    public final void rotate (double angle, Point center) {
        this.angle = angle;
        start = start.rotate(center, angle);
    }
    
    public Point straight(double t) {
        double xd = params[0];
        double yd = params.length == 2 ? params[1] : 0;
        
        Point off = new Point(xd, yd).mul(t);
        
        return start.add(off);
    }

    public Point trochoid(double t) {
        double a = params[0];
        double b = params[1];
        double toff = params[2];
        double rot = params[3];

        double x = a * t * rot - b * (Math.sin(t * rot + toff) - Math.sin(toff));
        double y = b * (Math.cos(t * rot + toff) - Math.cos(toff));
        
        Point off = new Point(x, y);

        return start.add(off);
    }

    public Point ravdoid(double t) {
        double hp = params[0];
        double cd = params[1];
        double toff = params[2];
        double rot = params[3];

        double x = hp * (2 * t * rot - Math.sin(2 * (t * rot + toff)) + Math.sin(2 * toff))
                + cd * (Math.cos(t * rot + toff) - Math.cos(toff));

        double y = hp * (Math.cos(2 * (t * rot + toff)) - Math.cos(2 * toff))
                + cd * (Math.sin(t * rot + toff) - Math.sin(toff));
        
        Point off = new Point(x, y);

        return start.add(off);
    }

    public Point f(double t) {
        Point ret = null;
        switch (type) {
            case "S":
                ret = straight(t);
                break;
            case "T":
                ret = trochoid(t);
                break;
            case "R":
                ret = ravdoid(t);
                break;
        }

        return ret.rotate(start, angle);
    }

    @Override
    public double fx(double t) {
        return f(t).x;
    }

    @Override
    public double fy(double t) {
        return f(t).y;
    }
    
    public void paint(MRGraphics g, boolean points) {
        double step = 0.001;
        Point prev = null;
        for (double t = 0; t <= 1; t += step) {
            double x = fx(t);
            double y = fy(t);
            Point cur = new Point(x,y);
            if (prev == null) {
                g.drawPoint(cur, 0, getColor());
            } else {
                g.drawLine(new Seg(prev, cur), getColor());
            }
            prev = cur;
        }
        if (points)
            g.drawPoint(getPoint(0), 3, Color.red);
    }

    public static RCurve fromString(String s) {
        Parser p = new Parser();
        p.setText(s);
        NL n = p.parse().get(0);
        
        return fromNL(n);
    }
    
    public static RCurve fromNL(NL n) {
        double sx = n.get(0).getNr();
        double sy = n.get(1).getNr();
        double angle = n.get(2).getNr();
        String type = n.get(3).getSym();
        double[] params = null;
        if (n.size() > 4) {
            params = new double[n.size() - 4];
            for (int i = 4; i < n.size(); i++) {
                params[i - 4] = n.get(i).getNr();
            }
        }

        return new RCurve(new Point(sx, sy), angle, type, params);
    }
    
    public NL toNL() {
        NL nl = new NL();
        nl.addNr(start.x);
        nl.addNr(start.y);
        nl.addNr(angle);
        nl.addSym(type);
        for (double d : params) {
            nl.addNr(d);
        }
        
        return nl;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("( ").append(start.x).append(" ")
                .append(start.y).append(" ").append(angle).append(" ").append(type);
        if (params != null) {
            for (double v : params) {
                sb.append(" ").append(v);
            }
        }
        sb.append(" )");
        return sb.toString();
    }

    /**
     * @return the angle
     */
    public double getAngle() {
        return angle;
    }

    /**
     * @param angle the angle to set
     */
    public void setAngle(double angle) {
        this.angle = angle;
    }

    /**
     * @return the startpoint
     */
    public Point getStart() {
        return start;
    }

    /**
     * @param angle the angle to set
     */
    public void setStart(Point start) {
        this.start = start;
    }
}
