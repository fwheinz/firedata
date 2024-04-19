package mrview;

import javax.vecmath.Point3d;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class Point3D {
    public double x, y, z;
    
    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Point3D project(Point3D p2, double d) {
        Point3D ret = new Point3D(
                p2.x*d + x*(1.0-d),
                p2.y*d + y*(1.0-d),
                p2.z*d + z*(1.0-d)
        );
        
//        System.out.println("project: "+ret.toString());
        
        return ret;
    }
    
    public Point3D add (Point3D p) {
        return new Point3D(p.x+x, p.y+y, p.z+z);
    }
    
    public javax.vecmath.Point3d getPoint() {
        return new Point3d(x, y, z);
    }
    
    public Point3D() {
    }
    
    public String toString() {
        return "("+x+"/"+y+"/"+z+")";
    }
}
