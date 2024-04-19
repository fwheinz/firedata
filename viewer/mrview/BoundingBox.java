package mrview;

import java.awt.Color;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class BoundingBox extends MovingObject implements Comparable {
    public Object parent;
    public Point ll, ur;
    
    public BoundingBox() {
    }

    public BoundingBox (int x1, int y1, int x2, int y2) {
        ll = new Point(x1, y1);
        ur = new Point(x2, y2);
    }
    
    public BoundingBox (Seg s) {
        update(s);
    }
        
    public void update (Point p) {
        if (p == null)
            return;
        if (ll == null)
            ll = new Point(p);
        else {
            if (ll.x > p.x)
                ll.x = p.x;
            if (ll.y > p.y)
                ll.y = p.y;
        }
        if (ur == null)
            ur = new Point(p);
        else {
            if (ur.x < p.x)
                ur.x = p.x;
            if (ur.y < p.y)
                ur.y = p.y;
        }
    }
    
    public void update (Seg s) {
        if (s == null)
            return;
        update(s.s);
        update(s.e);
    }
    
    public void update (BoundingBox bb) {
        if (bb == null)
            return;
        update(bb.ll);
        update(bb.ur);
    }
    
    public Point[] computeTransformation () {
        if (ll == null || ur == null) {
            return new Point[] {new Point(0,0), new Point(1,1)};
        }
        Point off = ll.mul(-1);
        Point p = ur.sub(ll);
        Point scale = new Point (1000.0/p.x, 1000.0/p.y);
        
        return new Point[] {off, scale};
    }
    
    public static BoundingBox calculate (List<Face> fcs) {
        BoundingBox ret = new BoundingBox();
        
        for (Face f : fcs) {
            ret.update(f.getBoundingBox());
        }
        
        return ret;
    }
    
    double getArea() {
        Point sz = ur.sub(ll);
        
        return sz.x * sz.y;
    }
    
    boolean overlaps (BoundingBox bb) {
        if (ur.x < bb.ll.x || bb.ur.x < ll.x ||
            ur.y < bb.ll.y || bb.ur.y < ll.y)
            return false;
        
        return true;
    }
    
    @Override
    public String toString() {
        if (ll == null || ur == null)
            return " NULL ";
        return ll.toString()+" => "+ur.toString();
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        g.drawLine(new Seg(ll, new Point(ll.x, ur.y)), Color.black, 3, 10);
        g.drawLine(new Seg(new Point(ll.x, ur.y), ur), Color.black, 3, 10);
        g.drawLine(new Seg(ur, new Point(ur.x, ll.y)), Color.black, 3, 10);
        g.drawLine(new Seg(new Point(ur.x, ll.y), ll), Color.black, 3, 10);
    }

    @Override
    public long getStartTime() {
        return -1;
    }

    @Override
    public long getEndTime() {
        return -1;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return this;
    }

    @Override
    public int compareTo(Object o) {
        BoundingBox b2 = (BoundingBox) o;
        if ((ll.x < b2.ll.x) || (ll.x == b2.ll.x && ll.y < b2.ll.y)) {
            return -1;
        } else if (ll.equals(b2.ll)) {
            return 0;
        } else {
            return 1;
        }
    }
}
