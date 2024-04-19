package mrview;

import java.awt.Color;

public abstract class MDot extends Curve {

    private final long startTime, endTime;

    public MDot(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    protected abstract Point fxy(double t);

    protected abstract Point fxy(double t, boolean debug, MRGraphics g);

    public void setAOff(double a) {
    }

    public double getAOff() {
        return 0.0;
    }

    private final boolean DEBUG = false;

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        super.paint(g, currentTime, highlight);
        if (DEBUG) {
            return;
        }
        if (currentTime < startTime || currentTime > endTime) {
            return;
        }
        double t = ((double) currentTime) / ((double) (endTime - startTime));
        g.drawPoint(fxy(t, true, g));
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
        return null;
    }

    public static MDot generate(final Point p1, final Point p2,
            final Point v, final Point c, final double rot) {
        MDot ret = new MDot(0, 1000000) {
            final private Point pp1, pp2;
            Double cd, xoff, yoff, hp, toff, tmin, tmax;

            {
                double angle = p1.sub(p2).getAngleRad() + Math.PI / 2;
                toff = angle / rot;

                hp = v.x / rot / 2;
                cd = distance(p2, p1, c);
                xoff = c.x - angle * hp * 2;
                yoff = c.y + hp;

                double c1 = ((p1.x - c.x) * Math.sin(-angle) + (p1.y - c.y) * Math.cos(-angle)) * rot / v.x;
                double c2 = ((p2.x - c.x) * Math.sin(-angle) + (p2.y - c.y) * Math.cos(-angle)) * rot / v.x;
                System.out.println("c1: " + c1 + "; c2: " + c2);
                System.out.println("rot: " + rot + "; period: " + 2.0 * Math.PI / rot);
                tmin = (Math.acos(c2) - angle) / rot;
                tmax = (Math.acos(c1) - angle) / rot;
                System.out.println("tmin: " + tmin + "; tmax: " + tmax);
                if (Double.isNaN(tmax) && !Double.isNaN(tmin)) {
                    tmax = -tmin;
                } else if (Double.isNaN(tmin) && !Double.isNaN(tmax)) {
                    tmin = -tmax;
                } else if (Double.isNaN(tmin) && Double.isNaN(tmax)) {
                    tmin = 0.0;
                    if ((c1 > 0 && c2 > 0) || (c1 < 0 && c2 < 0)) {
                        tmax = 0.0;
                    } else {
                        tmax = 2 * Math.PI/rot;
                    }
                }
                if (tmin > tmax) {
                    tmax += 2.0*Math.PI/rot;
//                    throw new RuntimeException
                    System.out.println
                    ("tmin > tmax!!");
                }

                while (tmax < 0) {
                    tmin += 2.0 * Math.PI / rot;
                    tmax += 2.0 * Math.PI / rot;
                }
                while (tmax > 2.0 * Math.PI / rot) {
                    tmin -= 2.0 * Math.PI / rot;
                    tmax -= 2.0 * Math.PI / rot;
                }
                System.out.println("cx: " + c.x + " cy: " + c.y + " p1x: " + p1.x + "; p1y: " + p1.y);
                System.out.println("hp: " + hp + "; cd: " + cd + "; toff: " + toff + "; xoff: " + xoff + "; yoff: " + yoff);
                System.out.println("angle: " + angle + "; tmin: " + tmin + "; tmax: " + tmax + "\n");

                pp1 = p1.rotate(c, -toff * rot).setY(c.y - v.x / rot).rotate(c, toff * rot);
                pp2 = p2.rotate(c, -toff * rot).setY(c.y + v.x / rot).rotate(c, toff * rot);

            }

            public double fx1(double t) {
                t = 2 * rot * (t + toff);
                return hp * (t - Math.sin(t))
                        + cd * Math.cos(t / 2)
                        + xoff;
            }

            public double fy1(double t) {
                t = 2 * rot * (t + toff);
                return hp * Math.cos(t)
                        + cd * Math.sin(t / 2)
                        + yoff;
            }

            protected Point fxy1(double t, boolean debug, MRGraphics g) {
                double t2 = t;
                while (t2 > tmax) {
                    t2 -= 2 * Math.PI / rot;
                }
                if (t2 < tmin || t2 > tmax) {
                    setColor(Color.GREEN);
                } else {
                    setColor(Color.BLUE);
                }
//                while ((t2 > 2.0 * Math.PI / rot) && ((t2 < tmin) || (t2 > tmax))) {
//                    t2 -= 2.0 * Math.PI / rot;
//                }
                if (debug) {
                }
                return new Point(fx1(t), fy1(t));
            }

            protected Point fxy1(double t) {
                return fxy1(t, false, null);
            }

            protected Point fxy2(double t, boolean debug, MRGraphics g) {
                double tt = (1 - Math.cos((t + toff) * rot)) / 2;
                Point m = pp2.add(pp1.sub(pp2).mul(tt)).rotate(c, rot * t).add(v.mul(t));
                double t2 = t;
                while (t2 > tmax) {
                    t2 -= 2 * Math.PI / rot;
                }
                if (t2 < tmin || t2 > tmax) {
                    setColor(Color.GREEN);
                } else {
                    setColor(Color.BLUE);
                }

                if (debug) {
                    Point p1p = pp1.rotate(c, rot * t).add(v.mul(t));
                    Point p2p = pp2.rotate(c, rot * t).add(v.mul(t));
                    g.drawLine(new Seg(p1p, p2p), Color.green, 2, 10);
                }

                return m;
            }

            public double fx2(double t) {
                return fxy2(t, false, null).x;
            }

            public double fy2(double t) {
                return fxy2(t, false, null).y;
            }

            protected Point fxy(double t, boolean debug, MRGraphics g) {
                return fxy2(t, debug, g);
            }

            protected Point fxy(double t) {
                return fxy(t, false, null);
            }

            public double fx(double t) {
                return fxy(t, false, null).x;
            }

            public double fy(double t) {
                return fxy(t, false, null).y;
            }
        };

        return ret;
    }

    private static double distance(Point lp1, Point lp2, Point p) {
        return ((lp2.y - lp1.y) * p.x - (lp2.x - lp1.x) * p.y + lp2.x * lp1.y - lp2.y * lp1.x)
                / Math.sqrt((lp2.y - lp1.y) * (lp2.y - lp1.y) + (lp2.x - lp1.x) * (lp2.x - lp1.x));
    }
}
