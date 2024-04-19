package mrview;

import java.awt.geom.Area;

public class MFaceAnalysis {
    private double iarea;
    
    private double farea;
    private double carea;
    private double rarea;
    private double traversedarea;
    private double visitedarea;
    private Point vector;
    private Region iarea_reg;
    private Region farea_reg;
    private Region carea_reg;
    private Region rarea_reg;
    private Region traversedarea_reg;
    private Region visitedarea_reg;
    private MFace2 mface;
    
    public MFaceAnalysis (MFace2 f) {
        mface = f;
        Face iface = f.project(0);
        Face fface = f.project(1);
        
        iarea = iface.getArea();
        iarea_reg = new Region(iface);
        farea = fface.getArea();
        farea_reg = new Region(fface);
        
        Area a = fface.getAreaObj();
        a.subtract(iface.getAreaObj());
        carea_reg = Region.area2region(a);
        carea = carea_reg.getArea();
        
        a = iface.getAreaObj();
        a.subtract(fface.getAreaObj());
        rarea_reg = Region.area2region(a);
        rarea = rarea_reg.getArea();
        
        vector = fface.getCenter().sub(iface.getCenter());
        
        traversedarea_reg = new MRegion(f).TraversedArea();
        traversedarea = traversedarea_reg.getArea();
        a = traversedarea_reg.getAreaObj();
        a.subtract(iface.getAreaObj());
        a.subtract(fface.getAreaObj());
        visitedarea_reg = Region.area2region(a);
        visitedarea = visitedarea_reg.getArea();
    }

    /**
     * @return the iarea
     */
    public double getIarea() {
        return iarea;
    }

    /**
     * @param iarea the iarea to set
     */
    public void setIarea(double iarea) {
        this.iarea = iarea;
    }

    /**
     * @return the farea
     */
    public double getFarea() {
        return farea;
    }

    /**
     * @param farea the farea to set
     */
    public void setFarea(double farea) {
        this.farea = farea;
    }

    /**
     * @return the carea
     */
    public double getCarea() {
        return carea;
    }

    /**
     * @param carea the carea to set
     */
    public void setCarea(double carea) {
        this.carea = carea;
    }

    /**
     * @return the rarea
     */
    public double getRarea() {
        return rarea;
    }

    /**
     * @param rarea the rarea to set
     */
    public void setRarea(double rarea) {
        this.rarea = rarea;
    }

    /**
     * @return the traversedarea
     */
    public double getTraversedarea() {
        return traversedarea;
    }

    /**
     * @param traversedarea the traversedarea to set
     */
    public void setTraversedarea(double traversedarea) {
        this.traversedarea = traversedarea;
    }

    /**
     * @return the visitedarea
     */
    public double getVisitedarea() {
        return visitedarea;
    }

    /**
     * @param visitedarea the visitedarea to set
     */
    public void setVisitedarea(double visitedarea) {
        this.visitedarea = visitedarea;
    }

    /**
     * @return the vector
     */
    public Point getVector() {
        return vector;
    }

    /**
     * @param vector the vector to set
     */
    public void setVector(Point vector) {
        this.vector = vector;
    }

    /**
     * @return the iarea_reg
     */
    public Region getIarea_reg() {
        return iarea_reg;
    }

    /**
     * @param iarea_reg the iarea_reg to set
     */
    public void setIarea_reg(Region iarea_reg) {
        this.iarea_reg = iarea_reg;
    }

    /**
     * @return the farea_reg
     */
    public Region getFarea_reg() {
        return farea_reg;
    }

    /**
     * @param farea_reg the farea_reg to set
     */
    public void setFarea_reg(Region farea_reg) {
        this.farea_reg = farea_reg;
    }

    /**
     * @return the carea_reg
     */
    public Region getCarea_reg() {
        return carea_reg;
    }

    /**
     * @param carea_reg the carea_reg to set
     */
    public void setCarea_reg(Region carea_reg) {
        this.carea_reg = carea_reg;
    }

    /**
     * @return the rarea_reg
     */
    public Region getRarea_reg() {
        return rarea_reg;
    }

    /**
     * @param rarea_reg the rarea_reg to set
     */
    public void setRarea_reg(Region rarea_reg) {
        this.rarea_reg = rarea_reg;
    }

    /**
     * @return the traversedarea_reg
     */
    public Region getTraversedarea_reg() {
        return traversedarea_reg;
    }

    /**
     * @param traversedarea_reg the traversedarea_reg to set
     */
    public void setTraversedarea_reg(Region traversedarea_reg) {
        this.traversedarea_reg = traversedarea_reg;
    }

    /**
     * @return the visitedarea_reg
     */
    public Region getVisitedarea_reg() {
        return visitedarea_reg;
    }

    /**
     * @param visitedarea_reg the visitedarea_reg to set
     */
    public void setVisitedarea_reg(Region visitedarea_reg) {
        this.visitedarea_reg = visitedarea_reg;
    }

    /**
     * @return the mface
     */
    public MFace2 getMface() {
        return mface;
    }
}
