package mrview;

import java.awt.Color;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MSeg extends MovingObject implements MovingSeg {

    private long startTime;
    private long endTime;
    public Seg i, f;
    
    public MSeg (Seg i, Seg f, long startTime, long endTime) {
        this.i = new Seg(i);
        this.f = new Seg(f);
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    @Override
    public Seg project (long currentTime) {
        if (currentTime < getStartTime() || currentTime > getEndTime() ||
                getStartTime() >= getEndTime())
            return null;
        
        double frac = ((double)(currentTime-getStartTime()))/((double)(getEndTime()-getStartTime()));
        
        return project (frac);
    }
    
    @Override
    public Seg project (double t) {
        Point s = i.s.add(f.s.sub(i.s).mul(t));
        Point e = i.e.add(f.e.sub(i.e).mul(t));
        
        return new Seg(s, e);
    }
    
    @Override
    public void paint (MRGraphics g, long currentTime, boolean highlight) {
        Seg s = project(currentTime);
        if (s != null) {
            g.drawLine(s, highlight ? Color.RED : Color.BLACK);
        }
    }
    
    @Override
    public BoundingBox getBoundingBox() {
        double minx, maxx, miny, maxy;
        
        minx = maxx = i.s.x;
        if (i.e.x < minx) minx = i.e.x;
        if (i.e.x > maxx) maxx = i.e.x;
        if (f.s.x < minx) minx = f.s.x;
        if (f.s.x > maxx) maxx = f.s.x;
        if (f.e.x < minx) minx = f.e.x;
        if (f.e.x > maxx) maxx = f.e.x;
        
        miny = maxy = i.s.y;
        if (i.e.y < miny) minx = i.e.y;
        if (i.e.y > maxy) maxx = i.e.y;
        if (f.s.y < miny) minx = f.s.y;
        if (f.s.y > maxy) maxx = f.s.y;
        if (f.e.y < miny) minx = f.e.y;
        if (f.e.y > maxy) maxx = f.e.y;
        
        BoundingBox ret = new BoundingBox();
        ret.update(new Seg(new Point(minx, miny), new Point(maxx, maxy)));
        return ret;
    }
    
    public String toString() {
        return " ( "+i+" "+f+" ) ";
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the endTime
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
