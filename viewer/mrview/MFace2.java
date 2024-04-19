package mrview;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MFace2 {
    public Point pp;
    public List<MSeg2> msegs = new LinkedList();
    public List<MFace2> holes = new LinkedList();
    
    public void paint (MRGraphics g, double frac, boolean highlight, boolean ishole) {
        List<Point> ps = new LinkedList();
        for (MSeg2 ms : msegs) {
            Seg s = ms.project(frac);
            ps.add(s.s);
            ps.add(s.e);
            g.drawLine(s, Color.BLUE);
        }
        if (highlight) {
            g.drawPolygon(ps, Color.BLACK, 0.0f);
        } else {
            g.drawPolygon(ps, ishole?Color.GRAY:Color.BLUE, 0.0f);
        }
        for (MFace2 hole : holes) {
            hole.paint(g, frac, highlight, true);
        }
    }
    
    public Face project (double frac) {
        Face ret = new Face();
        Point degeneratedCenter = null;
        
        for (MSeg2 ms : msegs) {
            Seg s = ms.project(frac);
            if (!s.isDegenerated())
                ret.addPointRaw(s.s);
            else
                degeneratedCenter = s.s;
        }
        ret.close();
        ret.sort2();
        if (ret.getSegments().isEmpty()) {
            ret.setDegeneratedCenter(degeneratedCenter);
        }
        for (MFace2 h : holes) {
            ret.addHole(h.project(frac));
        }
        
        return ret;
    }
    
    public Region wireframe () {
        Region wireframe = new Region();
        
        for (MSeg2 ms : msegs) {
            Face f = new Face();
            f.addPointRaw(ms.s.s);
            f.addPointRaw(ms.s.e);
            f.addPointRaw(ms.e.e);
            f.addPointRaw(ms.e.s);
            f.close();
            wireframe.addFace(f);
        }
        
        return wireframe;
    }
    
    public NL serialize() {
        NL nl = new NL();
        NL main = nl.nest();
        for (MSeg2 ms : msegs) {
            NL msnl = main.nest();
            msnl.addNr(ms.s.s.x);
            msnl.addNr(ms.s.s.y);
            msnl.addNr(ms.e.s.x);
            msnl.addNr(ms.e.s.y);
        }
        
        for (MFace2 h : holes) {
            nl.addNL(h.serialize().get(0));
        }
        
        return nl;
    }
    
    public static MFace2 deserializeCycle(NL nl) {
        MFace2 mface = new MFace2();
        NL first = nl.get(0);
        Point ps = new Point(first.get(0).getNr(), first.get(1).getNr());
        Point pe = new Point(first.get(2).getNr(), first.get(3).getNr());
        Point ls = ps;
        Point le = pe;
        for (int i = 1; i < nl.size(); i++) {
            NL next = nl.get(i);
            Point s = new Point(next.get(0).getNr(), next.get(1).getNr());
            Point e = new Point(next.get(2).getNr(), next.get(3).getNr());
            MSeg2 ms = new MSeg2(new Seg(ps, s), new Seg(pe, e));
            mface.msegs.add(ms);
            ps = s;
            pe = e;
        }
        MSeg2 ms = new MSeg2(new Seg(ps, ls), new Seg(pe, le));
        mface.msegs.add(ms);
        
        return mface;
    }
    
    public static MFace2 deserialize(NL nl) {
        MFace2 mface = deserializeCycle(nl.get(0));
        for (int i = 1; i < nl.size(); i++) {
            mface.holes.add(deserializeCycle(nl.get(i)));
        }
        
        return mface;
    }
    
    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();
        for (MSeg2 ms : msegs) {
            bb.update(ms.getBoundingBox());
        }
        
        return bb;
    }
    
    public boolean merge (MFace2 mf) {
        int i, j = 0, k;
        
        boolean found = false;
        for (i = 0; i < msegs.size(); i++) {
            for (j = 0; j < mf.msegs.size(); j++) {
                if (mf.msegs.get(j).equals(msegs.get(i))) {
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }
        
        if (j == mf.msegs.size()) {
            return false;
        }
        
        while (mf.msegs.get(j).equals(msegs.get(i))) {
            msegs.remove(i);
            mf.msegs.remove(j);
            if (i >= msegs.size())
                i = 0;
            if (j >= mf.msegs.size())
                j = 0;
        }
        for (k = 0; k < mf.msegs.size(); k++) {
            int idx = (j+k)%mf.msegs.size();
            MSeg2 ms2 = mf.msegs.get(idx).copy();
            ms2.changeDirection();
            msegs.add(i, ms2);
        }
        
        return true;
    }
    
    public void translateStart (Point v) {
        for (MSeg2 ms : msegs)
            ms.translateStart(v);
        for (MFace2 h : holes)
            h.translateStart(v);
    }
    
    public void translateEnd (Point v) {
        for (MSeg2 ms : msegs)
            ms.translateEnd(v);
        for (MFace2 h : holes)
            h.translateEnd(v);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MSeg2 ms : msegs) {
            sb.append(ms.s+" => "+ms.e+"\n");
        }
        
        return sb.toString();
    }
}
