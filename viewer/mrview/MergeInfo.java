package mrview;

import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MergeInfo implements Comparable {
    public Face f1, f2;
    public Seg s1, s2;
    public Point p1, p2;
    
    public MergeInfo (MergeInfo m) {
        f1 = new Face(f1);
        f2 = new Face(f2);
        s1 = new Seg(s1);
        s2 = new Seg(s2);
        p1 = new Point(p1);
        p2 = new Point(p2);
    }
    
    public MergeInfo (Face f1, Face f2, Seg s1, Seg s2, Point p1, Point p2) {
        this.f1 = f1;
        this.f2 = f2;
        this.s1 = s1;
        this.s2 = s2;
        this.p1 = p1;
        this.p2 = p2;
    }
    
    public double getDistance() {
        return p1.dist(p2);
    }
    
    private void splitSegs() {
        if (!s1.s.equals(p1) && !s1.e.equals(p1)) {
            int i;
            List<Seg> segs = f1.getSegments();
            for (i = 0; i < segs.size(); i++) {
                if (segs.get(i).equals(s1)) {
                   break;
                }
            }
            segs.remove(i);
            segs.add(i, new Seg(p1, s1.e));
            segs.add(i, new Seg(s1.s, p1));
        }
        
        if (!s2.s.equals(p2) && !s2.e.equals(p2)) {
            int i;
            List<Seg> segs = f2.getSegments();
            for (i = 0; i < segs.size(); i++) {
                if (segs.get(i).equals(s2)) {
                   break;
                }
            }
            segs.remove(i);
            segs.add(i, new Seg(p2, s2.e));
            segs.add(i, new Seg(s2.s, p2));
        }
    }
    
    private void translateSegs() {
        Point vec = p1.sub(p2);
        p2 = p1;
        f2.translate(vec);
    }
    
    public Face doMerge () {
        splitSegs();
        
        return f1.connect(p1, f2, p2);
    }
    
    public Face doMerge2() {
        splitSegs();
        translateSegs();
        
        return f1.connect(p1, f2, p2);
    }

    @Override
    public int compareTo(Object t) {
        MergeInfo mi = (MergeInfo) t;
        
        if (getDistance() < mi.getDistance())
            return -1;
        else if (getDistance() > mi.getDistance())
            return 1;
        else
            return 0;
    }
}
