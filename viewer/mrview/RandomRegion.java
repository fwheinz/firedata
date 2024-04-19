package mrview;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class RandomRegion {

    public static MFace2[] create(int nr) {
        BoundingBox bb1 = new BoundingBox(0, 0, 500, 500);
        BoundingBox bb2 = new BoundingBox(500, 0, 1000, 500);
        Face f1 = Face.createRandom2(nr, bb1);
        Face f2 = Face.createRandom2(nr, bb2);
        Face g1 = Face.createRandom2(nr, bb2);
        Face g2 = Face.createRandom2(nr, bb1);
        System.out.println("f1: " + f1.toString());
        System.out.println("f2: " + f2.toString());
        RotatingPlane rp = new RotatingPlane(f1, f2);
        MFace2 m1 = rp.rotatingPlane();
        rp = new RotatingPlane(g1, g2);
        MFace2 m2 = rp.rotatingPlane();

        return new MFace2[]{m1, m2};
    }

    public static void main(String[] args) {
        int i;
        for (i = 10; i < 1000; i++) {
            MFace2[] mf = create(i);
            MRegion mr1 = new MRegion(mf[0]);
            MRegion mr2 = new MRegion(mf[1]);
            ImportExport.exportObject("/home/sky/diplomarbeit/pmregion/tests/xmd"+i+"a", mr1);
            ImportExport.exportObject("/home/sky/diplomarbeit/pmregion/tests/xmd"+i+"b", mr2);
        }
    }

}
