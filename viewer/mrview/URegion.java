package mrview;

import java.awt.geom.Area;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class URegion {
    public Interval iv = new Interval(0, 1000000, true, true);
    public List<MFace2> mfaces = new LinkedList();
    
    public void paint (MRGraphics g, long currentTime, boolean highlight, boolean ishole) {
        if (!iv.inside(currentTime))
            return;
        for (MFace2 mface : mfaces) {
            mface.paint(g, iv.getFrac(currentTime), highlight, false);
        }
    }
    
    public Region project (long t) {
        Region r = new Region();
        if (!iv.inside(t))
            return null;
        for (MFace2 mf : mfaces) {
            Face f = mf.project(iv.getFrac(t));
            r.addFace(f);
        }
        
        return r;
    }
    
    public Region wireframe () {
        Region wireframe = new Region();
        for (MFace2 mf : mfaces) {
            Region r = mf.wireframe();
            wireframe.add(r);
        }
        
        return wireframe;
    }
    
    public Region project (double t) {
        Region r = new Region();
        for (MFace2 mf : mfaces) {
            Face f = mf.project(t);
            r.addFace(f);
        }
        
        return r;
    }
    
    public Area _TraversedArea() {
        Area a = new Area();
        
        for (MFace2 mf : mfaces) {
            a.add(mf.project(0).getAreaObj());
            a.add(mf.project(1).getAreaObj());

            for (MSeg2 ms : mf.msegs) {
                a.add(ms.getTraversedArea().getAreaObj());
            }
        }
        
        return a;
    }
    
    public Region TraversedArea() {
        return Region.area2region(_TraversedArea());
    }
    
    public static URegion deserialize (NL nl) {
        URegion uregion = new URegion();            
        uregion.iv = Interval.fromNL(nl.get(0));
        nl = nl.get(1);
        for (int i = 0; i < nl.size(); i++) {
            uregion.mfaces.add(MFace2.deserialize(nl.get(i)));
        }
        
        return uregion;
    }
    
    public NL serialize () {
        NL nl = new NL();
        nl.addNL(iv.toNL());
        NL n = nl.nest();
        for (MFace2 mface : mfaces) {
            n.addNL(mface.serialize());
        }
        
        return nl;
    }
    
    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        
        for (MFace2 mface : mfaces) {
            bb.update(mface.getBoundingBox());
        }
        
        return bb;
    }
}
