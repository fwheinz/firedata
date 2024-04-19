package mrview;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class Tri {
    Point3D p1, p2, p3;
    
    public Tri (Point3D p1, Point3D p2, Point3D p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }
    
    public Tri (Point3D p) {
        this.p1 = p;
        this.p2 = p;
        this.p3 = p;
    }

    public Tri project (Tri t, double d) {
        return new Tri(
                p1.project(t.p1, d),
                p2.project(t.p2, d),
                p3.project(t.p3, d)
        );
    }
    
    @Override
    public String toString() {
        return p1.toString()+" "+p2.toString()+" "+p3.toString();
    }
    
    public double[] planeEquation () {
        double a = (p2.y-p1.y)*(p3.z-p1.z) - (p3.y-p1.y)*(p2.z-p1.z);
        double b = (p2.z-p1.z)*(p3.x-p1.x) - (p3.z-p1.z)*(p2.x-p1.x);
        double c = (p2.x-p1.x)*(p3.y-p1.y) - (p3.x-p1.x)*(p2.y-p1.y);
        double d = -(a*p1.x + b*p1.y + c*p1.z);

        return new double[] {a, b, c, d};
    }
    
    public double[] planeIntersection (Tri t) {
        double[] p1 = planeEquation();
        double[] p2 = t.planeEquation();
        
        double A1 = p1[0];
        double B1 = p1[1];
        double C1 = p1[2];
        double D1 = p1[3];
        
        double A2 = p2[0];
        double B2 = p2[1];
        double C2 = p2[2];
        double D2 = p2[3];
        
        double xt = (B1*D2 - B2*D1)/(A1*B2-A2*B1);
        double xm = (B1*C2-B2*C1);
        double yt = (A2*D1 - A1*D2)/(A1*B2-A2*B1);
        double ym = (A2*C1-A1*C2);
        double zt = 0;
        double zm = 1;
        
        return new double[] {xt, xm, yt, ym, zt, zm};
    }
    
    public Point3D intersectionVector (Tri t) {
        double[] p = planeIntersection(t);
        
        double x = p[1];
        double y = p[3];
        double z = p[5];
        
        return new Point3D(x, y, z);
    }
    
    public List<MFacet3D> interpolate (Tri t2) {
        List<MFacet3D> l = new LinkedList();
        
        Tri t1 = this;
        Point3D is = t1.intersectionVector(t2);
        System.out.println("Intersect: "+is.toString());
        
        Tri x = new Tri(t1.p3);
        l.add(new MFacet3D(x, t2));
        
        return l;
    }
}
