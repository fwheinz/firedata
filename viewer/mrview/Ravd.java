package mrview;

import java.awt.Color;

public class Ravd extends Curve {

    public double xoff, yoff, toff, tmin, tmax, hp, cd, rot;
    public Double tmax2, tmin2;

    {
        setType("R");
    }

    public Ravd(double hp, double cd, double xoff, double yoff, double toff, double tmin, double tmax, double rot) {
        this.xoff = xoff;
        this.yoff = yoff;
        this.hp = hp;
        this.cd = cd;
        this.toff = toff;
        this.tmin = tmin;
        this.tmax = tmax;
        this.rot = rot;
        setColor(Color.GREEN);
        System.out.println(this.toString());
    }

    public Ravd(FMSeg seg) {
        Point v = seg.getV();
        Point C = seg.getC();
        Seg initial = seg.getI();
        double rotate = seg.getRotate();
        double vangle = v.getAngleRad();
        double vx = v.length();

        Seg i = initial.rotate(C, -vangle);
        Point p1 = i.s;
        Point p2 = i.e;
        double angle = p1.sub(p2).getAngleRad() + Math.PI / 2;
        double toff = angle;
        Double tmax2 = null, tmin2 = null;
        boolean bothValid = false;

        double hp = vx / rotate / 2;
        double cd = Util.distance(p2, p1, C);
        double xoff = C.x - angle * hp * 2;
        double yoff = C.y + hp;

        double c1 = ((p1.x - C.x) * Math.sin(-angle) + (p1.y - C.y) * Math.cos(-angle)) * rotate / vx;
        double c2 = ((p2.x - C.x) * Math.sin(-angle) + (p2.y - C.y) * Math.cos(-angle)) * rotate / vx;
//        System.out.println("c1: " + c1 + "; c2: " + c2 + "; angle: " + angle * 180d / Math.PI);
        double tmin = (Math.acos(c2) - angle);
        double tmax = (Math.acos(c1) - angle);
        System.out.println("tmin: " + tmin + "; tmax: " + tmax);
        double retp = -toff;
        double boundary = Double.isNaN(tmax) ? tmin : tmax;
        if (!Double.isNaN(boundary)) {
            while (retp > boundary) {
                retp -= Math.PI;
            }
            while (retp < boundary) {
                retp += Math.PI;
            }
        }
        if (Double.isNaN(tmax) && !Double.isNaN(tmin)) {
            tmax = -tmin + 2 * retp;
        } else if (Double.isNaN(tmin) && !Double.isNaN(tmax)) {
            tmin = -tmax + 2 * retp;
        } else if (Double.isNaN(tmin) && Double.isNaN(tmax)) {
            tmin = 0;
            tmax = ((c1 > 0 && c2 > 0) || (c1 < 0 && c2 < 0)) ? 0 : 2.0 * Math.PI;
        } else {
            tmax2 = -tmin + 2 * retp;
            tmin2 = -tmax + 2 * retp;
        }
        while (tmin > tmax) {
            tmax += 2.0 * Math.PI;
        }
        while (tmax < 0) {
            tmin += 2.0 * Math.PI;
            tmax += 2.0 * Math.PI;
        }
        while (tmax > 2.0 * Math.PI) {
            tmin -= 2.0 * Math.PI;
            tmax -= 2.0 * Math.PI;
        }

        this.hp = hp;
        this.cd = cd;
        this.xoff = xoff;
        this.yoff = yoff;
        this.toff = toff;
        this.tmin = tmin;
        this.tmax = tmax;
        this.rot = rotate;
        this.tmax2 = tmax2;
        this.tmin2 = tmin2;
        setColor(Color.GREEN);
        System.out.println(this.toString());
        setTransformation(C, vangle);

    }

    public double[] getParams() {
        double[] times = getTimes();
        double t0 = times[0];
        double dt = times[1] - times[0];
        return new double[]{hp, cd, toff + t0 * rot, rot * dt};
    }

    @Override
    public boolean isValid(double t) {
        double t2 = t * rot;
        double diff = 2 * Math.PI;

        while (t2 > tmax) {
            t2 -= diff;
        }
        boolean valid1 = !(t2 + 0.0000001 < tmin || t2 - 0.0000001 > tmax);
        boolean valid2 = false;

        if (tmax2 != null) {
            t2 = t * rot;
            while (t2 > tmax2) {
                t2 -= diff;
            }
            valid2 = !(t2 + 0.0000001 < tmin2 || t2 - 0.0000001 > tmax2);
        }

        return valid1 || valid2;
    }

    @Override
    public double fx(double t) {
        if (!isValid(t)) {
//            setColor(Color.GREEN);
//            setVisible(false);
        } else {
//            setColor(Color.BLUE);
//            setVisible(true);
        }

        t = toff + t * rot;
        return hp * (2 * t - Math.sin(2 * t))
                + cd * Math.cos(t)
                + xoff;
    }

    @Override
    public double fy(double t) {
        t = toff + t * rot;
        return hp * Math.cos(2 * t)
                + cd * Math.sin(t)
                + yoff;
    }
    
    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public String toString() {
        return getObjName() + ": hp: " + hp + "; cd: " + cd + "; xoff: " + xoff + "; "
                + "yoff: " + yoff + "; toff: " + toff / rot + "; tmin: " + tmin / rot + "; tmax: " + tmax / rot + "; rot: " + rot + "; pd: " + 2 * Math.PI / rot;
    }

}
