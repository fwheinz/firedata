package mrview;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class RotatingPlane {
    public Face sreg, dreg;
    public List<Face> scvs, dcvs;
    public MFace2 mface;
    
    public RotatingPlane (Face src, Face dst) {
        this.sreg = src;
        this.dreg = dst;
    }
    
    public MFace2 rotatingPlane () {
        System.out.println("Rotating Plane started");
        sreg.sort2();
        dreg.sort2();
        
        Face shull = sreg.getConvexHull();
        Face dhull = dreg.getConvexHull();
        scvs = new LinkedList();
        dcvs = new LinkedList();
        
        System.out.println(sreg.toString());
        System.out.println(shull.toString());
        
        mface = new MFace2();
        do {
            double asrc = shull.Cur().getAngleXAxis(), adst = dhull.Cur().getAngleXAxis();
            
            if (((asrc <= adst || dhull.End())) && !shull.End()) {
                Point is = new Point(shull.Cur().s);
                Point ie = new Point(shull.Cur().e);
                Point fs = new Point(dhull.Cur().s);
                Point fe = new Point(dhull.Cur().s);
                MSeg2 m = new MSeg2(new Seg(is, ie), new Seg(fs, fe));
                if (!shull.Cur().equals(sreg.Cur())) {
                    System.out.println("Source concavity:");
                    scvs.add(getConcavity(sreg, shull, dhull));
                } else {
                    sreg.Next();
                }
                shull.Next();
                mface.msegs.add(m);
           }
           if ( ((asrc >= adst) || shull.End()) && !dhull.End()) {
                Point is = new Point(shull.Cur().s); 
                Point ie = new Point(shull.Cur().s);
                Point fs = new Point(dhull.Cur().s);
                Point fe = new Point(dhull.Cur().e);
                MSeg2 m = new MSeg2(new Seg(is, ie), new Seg(fs, fe));
                if (!dhull.Cur().equals(dreg.Cur())) {
                    System.out.println("Destination concavity:");
                    dcvs.add(getConcavity(dreg, dhull, shull));
                } else {
                    dreg.Next();
                }
                dhull.Next();
                mface.msegs.add(m);
            }
        } while (!shull.End() || !dhull.End());
        
        System.out.println("Rotating Plane ended");
        
        return mface;
    }
    
    private Face getConcavity (Face face, Face hull, Face peer) {
        Face concavity = new Face();
        
        concavity.hullseg = hull.Cur();
        concavity.addPoint(face.Cur().s);
        while (!hull.Cur().e.equals(face.Cur().s)) {
            concavity.addPoint(face.Cur().e);
            face.Next();
        }
        concavity.close();
        concavity.sort2();
        concavity.peerPoint = peer.Cur().s;
        concavity.parent = face;
        
        System.out.println(concavity.toString());
        
        return concavity;
    }
    
    public static MRegion interpolate (Region src, Region dst) {
//        if (src.getFaces().size() > 1)
//            return interpolateMerge(src, dst);
        
        List<Face> sregs = new LinkedList();
        sregs.addAll(src.getFaces());
        List<Face> dregs = new LinkedList();
        dregs.addAll(dst.getFaces());
        return new MRegion(interpolate(sregs, dregs, 0));
    }
    
    public static List<MFace2> interpolate (Face src, Face dst) {
        List<Face> sregs = new LinkedList();
        sregs.add(src);
        List<Face> dregs = new LinkedList();
        dregs.add(dst);
        return interpolate(sregs, dregs, 0);
    }
    
    public static MRegion interpolateMerge (Region src, Region dst) {
        List<Face> sregs = src.getFaces();
        Face f = Face.connect(sregs.toArray(new Face[0]));
        MFace2 mf = f.meet();
        f = new Face(f).shrink();
        List<Face> sr = new LinkedList();
        Point c1 = f.getCenter();
        sr.add(f);
        List<Face> dregs = new LinkedList();
        dregs.addAll(dst.getFaces());
        List<MFace2> mf2 = interpolate(sr, dregs, 0);
        Point c2 = mf2.get(0).project(1.0).getCenter();
        
        Point dc = c2.sub(c1).mul(0.5);
        
        mf.translateEnd(dc);
        mf2.get(0).translateStart(dc);
        
        URegion ur1 = new URegion();
        ur1.iv = new Interval(0, 500000, true, true);
        ur1.mfaces.add(mf);
        
        URegion ur2 = new URegion();
        ur2.iv = new Interval(500000, 1000000, true, true);
        ur2.mfaces = mf2;
        
        MRegion mr = new MRegion();
        mr.uregions.add(ur1);
        mr.uregions.add(ur2);
        
        return mr;
    }
    
    public static List<MFace2> interpolate (List<Face> sregs, List<Face> dregs, int depth) {
        List<MFace2> ret = new LinkedList();
        
        List<Face[][]> matches;
        if (depth == 0)
//            matches = MatchFaces.create(sregs, dregs, depth).areaPair(90);
            matches = MatchFaces.create(sregs, dregs, depth).overlapPair();
        else
//            matches = MatchFaces.create(sregs, dregs, depth).areaPair(90);
            matches = MatchFaces.create(sregs, dregs, depth).nullPair();
        
        for (Face[][] pair : matches) {
            Face[] src = pair[0];
            Face[] dst = pair[1];
            
            if (src.length > 0 && dst.length > 0) {
                Face s, d;
//                s = Face.connect(src);
//                d = Face.connect(dst);
                s = src[0];
                d = dst[0];
                RotatingPlane rp = new RotatingPlane(s, d);
                MFace2 mf = rp.rotatingPlane();
                
                List<MFace2> fcs = interpolate(rp.scvs, rp.dcvs, depth+1);
                for (MFace2 fc : fcs) {
                    if (!mf.merge(fc))
                        mf.holes.add(fc);
                }
                ret.add(mf);
            } else {
                ret.add(src.length > 0 ? src[0].collapse() : dst[0].expand());
            }
        }
        
        return ret;
    }
}
