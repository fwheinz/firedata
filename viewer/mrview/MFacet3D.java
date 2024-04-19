package mrview;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MFacet3D {
    private Tri i, f;
    
    public MFacet3D (Tri i, Tri f) {
        this.i = i;
        this.f = f;
    }
    
    Tri project (double d) {
        Tri ret = i.project(f, d);
	return ret;
    }
    
    static List<MFacet3D> interpolate (Tri t1, Tri t2) {
        return t1.interpolate(t2);
    }
    
}
