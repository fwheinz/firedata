package mrview;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

public class CFace {
    private List<CFace> holes = new LinkedList();
    private List<RCurve> rs = new LinkedList();
    private static final Color holeColor = Color.RED;
    private BoundingBox bb;
    
    public void addRCurve (RCurve r) {
        rs.add(r);
    }
    
    public void addHole(CFace hole) {
        for (RCurve r : hole.rs) {
            r.setColor(holeColor);
        }
        holes.add(hole);
    }

    private List<CFace> getHoles() {
        return holes;
    }
    
    public void paint (MRGraphics g, boolean points, boolean highlight) {
        for (RCurve r : rs) {
            r.paint(g, points);
        }
        
        for (CFace f : holes) {
            f.paint(g, points, highlight);
        }
    }
    
    public BoundingBox getBoundingBox() {
        if (bb == null) {
            bb = new BoundingBox();
            for (RCurve r : rs) {
                bb.update(r.getStart());
            }
        }

        return bb;
    }

    
    private static CFace deserializeSingleFace(NL nl) {
        CFace f2 = new CFace();
        for (int i = 0; i < nl.size(); i++) {
            f2.addRCurve(RCurve.fromNL(nl.get(i)));
        }
        
        return f2;
    }
        
    public static CFace deserialize(NL nl) {
        CFace f2 = deserializeSingleFace(nl.get(0));
        
        for (int i = 1; i < nl.size(); i++) {
            CFace hole = deserializeSingleFace(nl.get(i));
            f2.addHole(hole);
        }
        
        return f2;
    }
    
    public NL toNL () {
        NL nl = new NL();
        NL n = nl.nest();
        for (RCurve r : rs)
            n.addNL(r.toNL());
        for (CFace f : holes) {
            nl.addNL(f.toNL().get(0));
        }
        
        return nl;
    }
}
