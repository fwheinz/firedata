package mrview;

import java.awt.geom.Area;

public class MRegionAnalysis {
    public static final int NR = 0, SUM = 1, MEAN = 2, STDDEV = 3, VARIANCE = 4;
    /** Initial area */
    public double iarea;
    /** Final area */
    public double farea;
    public double carea;
    public double rarea;
    public double bbchange, bbchangerate;
    public double[] ifacesize, ffacesize, dfacesize;
    public double traversedarea;
    public double visitedarea;
    public double averagespeed, averagedistance;
    public double duration;
    public double[] angledev, lengthdev, speeddev;
    
    public Point vector;
    public BoundingBox bb, bbi, bbf;
    public Region iarea_reg;
    public Region farea_reg;
    public Region carea_reg;
    public Region rarea_reg;
    public Region traversedarea_reg;
    public Region visitedarea_reg;
    public MRegion mr;
    public URegion uregion;
    
    public static final double initial = 0.0f, final_ = 1.0f;
    
    public MRegionAnalysis (MRegion mr) {
        
        this.mr = mr;
        this.uregion = mr.uregions.get(0);
        duration = (uregion.iv.project(final_) - uregion.iv.project(initial))/1000.0f;
        
        /* Bounding boxes */
        
        bb  = uregion.getBoundingBox();
        bbi = uregion.project(initial).getBoundingBox();
        bbf = uregion.project(final_).getBoundingBox();
        
        bbchange = Math.sqrt(bbf.getArea()/bbi.getArea());
        bbchangerate = bbchange/duration;
        
        /* Static features, whole region */
        
        iarea_reg = uregion.project(initial);
        iarea = iarea_reg.getArea();
        
        farea_reg = uregion.project(final_);
        farea = farea_reg.getArea();
        
        rarea_reg = calculateDiffArea(initial);
        rarea = rarea_reg.getArea();
        
        carea_reg = calculateDiffArea(final_);
        carea = carea_reg.getArea();
        
        vector = farea_reg.getCenter().sub(iarea_reg.getCenter());
        
        
        /* Dynamic features, face-wise */
        
        int nrfaces = uregion.mfaces.size();
        double[] ifsz = new double[nrfaces];
        double[] ffsz = new double[nrfaces];
        double[] dfsz = new double[nrfaces];
        Point[] vectors = new Point[nrfaces];
        double[] angles = new double[nrfaces];
        double[] lengths = new double[nrfaces];
        double[] speeds = new double[nrfaces];
        
        for (int i = 0; i < nrfaces; i++) {
            MFace2 mface = uregion.mfaces.get(i);
            Face iface = mface.project(initial);
            Face fface = mface.project(final_);
            ifsz[i] = iface.getArea();
            ffsz[i] = fface.getArea();
            dfsz[i] = Math.abs(ffsz[i]-ifsz[i]);
            vectors[i] = fface.getCenter().sub(iface.getCenter());
            lengths[i] = vectors[i].length();
            speeds[i] = vectors[i].length()/duration;
            angles[i] = vectors[i].getAngleRad();
        }
        ifacesize = getDeviation(ifsz);
        ffacesize = getDeviation(ffsz);
        dfacesize = getDeviation(dfsz);
        angledev  = getDeviation(angles);
        lengthdev = getDeviation(lengths);
        speeddev = getDeviation(speeds);
        
        
        
        traversedarea_reg = uregion.TraversedArea();
        traversedarea = traversedarea_reg.getArea();
        Area a = traversedarea_reg.getAreaObj();
        a.subtract(iarea_reg.getAreaObj());
        a.subtract(farea_reg.getAreaObj());
        visitedarea_reg = Region.area2region(a);
        visitedarea = visitedarea_reg.getArea();
        
    }
    
    public final Region calculateDiffArea(double t) {
        Area ret = new Area();
        
        for (MFace2 mf : uregion.mfaces) {
            Area a = mf.project(t-initial).getAreaObj();
            a.subtract(mf.project(final_-t).getAreaObj());
            ret.add(a);
        }
        
        return Region.area2region(ret);
    }
    
    private double[] getDeviation (double[] data) {
        double mean = 0, sum = 0;
        for (double d : data) {
            System.out.println("Data: "+d);
            sum += d;
        }
        mean = sum / data.length;
        double variance = 0;
        for (double d : data) {
            variance += ((d - mean)*(d - mean));
        }
        variance /= data.length;
        
        double stddev = Math.sqrt(variance);
        
        System.out.println(mean+" "+stddev+" "+variance+"\n");
        
        return new double[] { data.length, sum, mean, (stddev*100)/mean, (variance*100)/(mean*mean) };
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
     * @return the mregion
     */
    public MRegion getMregion() {
        return mr;
    }
    
    /**
     * @return the averagespeed
     */
    public double getAveragespeed() {
        return averagespeed;
    }

    /**
     * @param averagespeed the averagespeed to set
     */
    public void setAveragespeed(double averagespeed) {
        this.averagespeed = averagespeed;
    }

    /**
     * @return the averagedistance
     */
    public double getAveragedistance() {
        return averagedistance;
    }

    /**
     * @param averagedistance the averagedistance to set
     */
    public void setAveragedistance(double averagedistance) {
        this.averagedistance = averagedistance;
    }

    /**
     * @return the duration
     */
    public double getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(double duration) {
        this.duration = duration;
    }

    /**
     * @return the uregion
     */
    public URegion getUregion() {
        return uregion;
    }

    /**
     * @param uregion the uregion to set
     */
    public void setUregion(URegion uregion) {
        this.uregion = uregion;
    }

    /**
     * @return the bb
     */
    public BoundingBox getBb() {
        return bb;
    }

    /**
     * @param bb the bb to set
     */
    public void setBb(BoundingBox bb) {
        this.bb = bb;
    }

    /**
     * @return the bbi
     */
    public BoundingBox getBbi() {
        return bbi;
    }

    /**
     * @param bbi the bbi to set
     */
    public void setBbi(BoundingBox bbi) {
        this.bbi = bbi;
    }

    /**
     * @return the bbf
     */
    public BoundingBox getBbf() {
        return bbf;
    }

    /**
     * @param bbf the bbf to set
     */
    public void setBbf(BoundingBox bbf) {
        this.bbf = bbf;
    }

    /**
     * @return the ifacesize
     */
    public double getIfacesize(int idx) {
        return ifacesize[idx];
    }

    /**
     * @return the ffacesize
     */
    public double getFfacesize(int idx) {
        return ffacesize[idx];
    }

    /**
     * @return the dfacesize
     */
    public double getDfacesize(int idx) {
        return dfacesize[idx];
    }
}
