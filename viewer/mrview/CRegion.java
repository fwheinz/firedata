package mrview;

import java.util.LinkedList;
import java.util.List;

public class CRegion extends MovingObject implements SecondoObject {
    private boolean points;
    private List<CFace> faces = new LinkedList();
    private BoundingBox bb;
    
    public CRegion () {
    }
    
    public CRegion (CFace f) {
        faces.add(f);
    }
    
    public void addFace (CFace f2) {
        bb = null;
        faces.add(f2);
    }
    
    @Override
    public String[] getFlags() {
        return new String[] { "Points" };
    }
    
    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        for (CFace f : faces) {
            f.paint(g, points, highlight);
        }
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
        if (bb == null) {
            bb = new BoundingBox();
            for (CFace f : faces) {
                bb.update(f.getBoundingBox());
            }
        }
        return bb;
    }

    /**
     * @return the points
     */
    public boolean isPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(boolean points) {
        this.points = points;
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        CRegion  r2 = new CRegion();
        
        for (int i = 0; i < nl.size(); i++) {
            r2.addFace(CFace.deserialize(nl.get(i)));
        }
        
        return r2;
    }

    @Override
    public NL serialize() {
        NL nl = new NL(), ret = nl;
        for (CFace f : faces) {
            nl.addNL(f.toNL());
        }
        
        return ret;
    }
    
    @Override
    public String getSecondoType() {
        return "cregion";
    }
}
