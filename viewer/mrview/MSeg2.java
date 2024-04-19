package mrview;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MSeg2 {
    public Seg s, e;
    
    public MSeg2 (Seg s, Seg e) {
        this.s = s;
        this.e = e;
    }
    
    public Face getTraversedArea() {
        Face f = new Face();
        f.addPointRaw(s.s);
        if (!s.s.equals(s.e))
            f.addPointRaw(s.e);
        f.addPointRaw(e.e);
        if (!e.s.equals(e.e))
            f.addPointRaw(e.s);
        
        f.close();
        f.sort2();
        
        
        return f;
    }
    
    public Seg project (double frac) {
        Point ns = s.s.add(e.s.sub(s.s).mul(frac));
        Point ne = s.e.add(e.e.sub(s.e).mul(frac));
        return new Seg(ns, ne);
    }
    
    public void changeDirection () {
        s.changeDirection();
        e.changeDirection();
    }
    
    public void translateStart (Point v) {
        s.translate(v);
    }
    
    public void translateEnd (Point v) {
        e.translate(v);
    }
    
    public MSeg2 copy() {
        return new MSeg2(new Seg(s), new Seg(e));
    }
    
    public boolean equals (MSeg2 ms) {
        return s.equals(ms.s) && e.equals(ms.e);
    }
    
    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        bb.update(s);
        bb.update(e);
        
        return bb;
    }
    
    public String toString() {
        return s.toString()+" => "+e.toString();
    }
}
