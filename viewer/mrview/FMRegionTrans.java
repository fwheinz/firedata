package mrview;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class FMRegionTrans {

    private Point center;
    private Point v0;
    private Point v;
    private double a0;
    private double a;
    private Interval iv = new Interval();
    private List<List<List<FMSeg>>> fmfaces;

    public FMRegionTrans() {
        center = new Point(0, 0);
        v0 = new Point(0, 0);
        v = new Point(0, 0);
    }

    public FMRegionTrans(Point center, Point v0, Point v, double a0, double a, long start, long end) {
        this.center = center;
        this.v0 = v0;
        this.v = v;
        this.a0 = a0;
        this.a = a;
        iv.setStart(start);
        iv.setEnd(end);
        iv.setLeftClosed(true);
        iv.setRightClosed(true);
    }

    public FMRegionTrans(Point center, Point v, double a, long start, long end) {
        this.center = center;
        this.v = v;
        this.v0 = new Point(0, 0);
        this.a = a;
        iv.setStart(start);
        iv.setEnd(end);
        iv.setLeftClosed(true);
        iv.setRightClosed(true);
    }
    
    public FMRegionTrans restrict (long startTime, long endTime) {
        Point nv0 = v0.add(v.mul(iv.getFrac(startTime)));
        Point nv  = v.mul(iv.getFrac(endTime) - iv.getFrac(startTime));
        double na0 = a0 + a * iv.getFrac(startTime);
        double na = a * (iv.getFrac(endTime) - iv.getFrac(startTime));
        FMRegionTrans frt = new FMRegionTrans(center, nv0, nv, na0, na, startTime, endTime);
        return frt;
    }
    
    public List<FMSeg> createFMSegs (Face f) {
        List<FMSeg> l = new LinkedList();
        
        for (Seg s : f.getSegments()) {
            s = new Seg(s.rotate(center, a0)).translate(v0);
            FMSeg fmseg = new FMSeg(s, v, center.add(v0), a, iv.getStart(), iv.getEnd());
            l.add(fmseg);
        }
        
        return l;
    }

    public void createFMSegs(Region region) {
        fmfaces = new LinkedList();
        
        for (Face f : region.getFaces()) {
            List<List<FMSeg>> fmface = new LinkedList();
            fmface.add(createFMSegs(f));
            for (Face h : f.getHoles()) {
                fmface.add(createFMSegs(h));
            }
            fmfaces.add(fmface);
        }
    }
    
    public List<FMSeg> getFMSegs(Region r) {
        List<FMSeg> l = new LinkedList();

        for (Face f : r.getFaces()) {
            for (Seg s : f.getSegments()) {
                s = new Seg(s.rotate(center, a0)).translate(v0);
                FMSeg fmseg = new FMSeg(s, v, center.add(v0), a, iv.getStart(), iv.getEnd());
                l.add(fmseg);
            }
        }

        return l;
    }
    
    public Face project (List<FMSeg> l, long t) {
        Face face = new Face();
        
        for (FMSeg fms : l) {
            Seg s = fms.project((long) t);
            face.addPointRaw(s.s);
        }
        face.close();
        face.sort2();
        
        return face;
    }
    
    public Region project (long t) {
        Region r = new Region();
        
        for (List<List<FMSeg>> fmface : fmfaces) {
            Face f = project(fmface.get(0), t);
            for (int i = 1; i < fmface.size(); i++)
                f.addHole(project(fmface.get(i), t));
            r.addFace(f);
        }
        
        return r;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public Point getV0() {
        return v0;
    }

    public void setV0(Point v0) {
        this.v0 = v0;
    }

    public Point getV() {
        return v;
    }

    public void setV(Point v) {
        this.v = v;
    }

    public double getA0() {
        return a0;
    }

    public void setA0(double a0) {
        this.a0 = a0;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public Long getStart() {
        return iv.getStart();
    }

    public void setStart(long start) {
        iv.setStart(start);
    }

    public Long getEnd() {
        return iv.getEnd();
    }

    public void setEnd(long end) {
        iv.setEnd(end);
    }

    public NL serialize() {
        NL nl = new NL();
        nl.addNL(center.toNL());
        nl.addNL(v0.toNL());
        nl.addNL(v.toNL());
        nl.addNr(a0);
        nl.addNr(a);
        nl.addNL(iv.toNL());

        return nl;
    }

    public static FMRegionTrans deserialize(NL nl) {
        FMRegionTrans ret = new FMRegionTrans();

        ret.setCenter(Point.fromNL(nl.get(0)));
        ret.setV0(Point.fromNL(nl.get(1)));
        ret.setV(Point.fromNL(nl.get(2)));
        ret.setA0(nl.get(3).getNr());
        ret.setA(nl.get(4).getNr());
        ret.setIv(Interval.fromNL(nl.get(5)));

        return ret;
    }

    /**
     * @return the leftClosed
     */
    public boolean isLeftClosed() {
        return iv.isLeftClosed();
    }

    /**
     * @param leftClosed the leftClosed to set
     */
    public void setLeftClosed(boolean leftClosed) {
        iv.setLeftClosed(leftClosed);
    }

    /**
     * @return the rightClosed
     */
    public boolean isRightClosed() {
        return iv.isRightClosed();
    }

    /**
     * @param rightClosed the rightClosed to set
     */
    public void setRightClosed(boolean rightClosed) {
        iv.setRightClosed(rightClosed);
    }

    /**
     * @return the iv
     */
    public Interval getIv() {
        return iv;
    }

    /**
     * @param iv the iv to set
     */
    public void setIv(Interval iv) {
        this.iv = iv;
    }

    /**
     * @return the fmfaces
     */
    public List<List<List<FMSeg>>> getFmfaces() {
        return fmfaces;
    }

}
