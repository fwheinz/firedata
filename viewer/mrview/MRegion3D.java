/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mrview;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRegion3D extends MovingObject implements SecondoObject {
    public long startTime, endTime;
    public List<MFacet3D> facets = new LinkedList();
    
    public MRegion3D (long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public List<Tri> project (double d) {
        List<Tri> ret = new LinkedList();
        for (MFacet3D f : facets) {
            ret.add(f.project(d));
        }
        
        return ret;
    }
    
    public List<Tri> project (long currentTime) {
        double d = (currentTime-startTime)/((double)(endTime-startTime));
        
        return project(d);
    }
    
    public void addMFacet3D (MFacet3D mf3) {
        facets.add(mf3);
    }
    
    public void addMFacet3D (List<MFacet3D> mf3l) {
        for (MFacet3D mf3 : mf3l) {
            facets.add(mf3);
        }
    }

    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        return;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(0, 0, 1000,1000);
    }

    @Override
    public String getSecondoType() {
        return "mregion3d";
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NL serialize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
