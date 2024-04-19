package mrview;

import com.sun.j3d.exp.swing.JCanvas3D;
import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.universe.SimpleUniverse;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Group;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TriangleArray;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Color3b;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRViewCanvas3D extends JPanel implements MRListener {

    static public final double RANGEX = 5,
            RANGEY = 5,
            RANGEZ = 5;

    static public final Color3b[] colors = {
        new Color3b(Color.RED),
        new Color3b(Color.BLUE),
        new Color3b(Color.YELLOW),
        new Color3b(Color.CYAN),
        new Color3b(Color.MAGENTA),
        new Color3b(Color.GRAY),
        new Color3b(Color.ORANGE),
        new Color3b(Color.GREEN)
    };

    private MRModel model;
    private final JScrollPane ScrollPane = new JScrollPane();
    private final SimpleUniverse universe;
    private BranchGroup bg = null;

    public MRViewCanvas3D(MRModel model) {
        setLayout(new BorderLayout());
        this.model = model;
        GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
        template.setDoubleBuffer(GraphicsConfigTemplate3D.PREFERRED);
        template.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);
        JCanvas3D j3d = new JCanvas3D(template);
        universe = new SimpleUniverse(j3d.getOffscreenCanvas3D());

        ScrollPane.add(j3d);
        ScrollPane.setViewportView(universe.getCanvas());

        this.add(BorderLayout.CENTER, ScrollPane);

        model.addChangeListener(this);

//        this.setBackground(Color.WHITE);
//        this.addMouseMotionListener(new MouseAdapter() {
//            @Override
//            public void mouseMoved(MouseEvent e) {
//                Point p = new Point(e.getPoint().x, e.getPoint().y);
//                if (MRGraphics.last == null) {
//                    return;
//                }
//                Point r = MRGraphics.last.positionInModel(p);
//                MRViewCanvas3D.this.model.cursorPosition(r);
//            }
//
//            @Override
//            public void mouseDragged(MouseEvent e) {
//                Point p = new Point(e.getPoint().x, e.getPoint().y);
//                if (MRGraphics.last == null) {
//                    return;
//                }
//                Point r = MRGraphics.last.positionInModel(p);
//                MRViewCanvas3D.this.model.dragEvent(r);
//                MRViewCanvas3D.this.repaint();
//            }
//
//            @Override
//            public void mouseReleased(MouseEvent e) {
//                Point p = new Point(e.getPoint().x, e.getPoint().y);
//                if (MRGraphics.last == null) {
//                    return;
//                }
//                MRViewCanvas3D.this.model.removeDragSegments();
//                MRViewCanvas3D.this.repaint();
//            }
//
//        });
//        this.addMouseWheelListener(new MouseWheelListener() {
//            @Override
//            public void mouseWheelMoved(MouseWheelEvent e) {
//                if (MRGraphics.last == null) {
//                    return;
//                }
//                MRViewCanvas3D.this.model.wheelEvent(e.getWheelRotation());
//                MRViewCanvas3D.this.repaint();
//            }
//        });
//        this.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseExited(MouseEvent e) {
//                MRViewCanvas3D.this.model.cursorPosition(null);
//            }
//        });
        MRegion3D mr3d = new MRegion3D(0, 3600000);
        Tri tri1 = new Tri(
                new Point3D(-1000, 0, 0), 
                new Point3D(-900, 0, 100), 
                new Point3D(-800, 100, 100)
        );
        Tri tri2 = new Tri(
                new Point3D(0, 100, 100),
                new Point3D(100, 20, 800),
                new Point3D(3000, 1000, 500)
        );
        Tri tri3 = new Tri(
                new Point3D(0, 0, 0),
                new Point3D(0, 0, 100),
                new Point3D(100, 50, 50)
        );
        Tri tri4 = new Tri(
                new Point3D(100, 0, 0),
                new Point3D(100, 0, 100),
                new Point3D(0, 50, 50)
        );
        mr3d.addMFacet3D(new MFacet3D(tri3, tri3));
        mr3d.addMFacet3D(new MFacet3D(tri4, tri4));
        mr3d.addMFacet3D(new MFacet3D(tri3, tri4));
        mr3d.addMFacet3D(tri3.interpolate(tri4));

	MRegion3D mr2 = new MRegion3D(0, 36000000);
	Tri t1a = new Tri(
			new Point3D(100,  100,   0),
			new Point3D(100, -100,   0),
			new Point3D(  0,    0, 100)
			);
	Tri t1b = new Tri(
			new Point3D(200,  200,   0),
			new Point3D(200, -200,   0),
			new Point3D(  0,    0, 200)
			);
	Tri t2a = new Tri(
			new Point3D( 100, -100,   0),
			new Point3D(-100, -100,   0),
			new Point3D(   0,    0, 100)
			);
	Tri t2b = new Tri(
			new Point3D( 200, -200,   0),
			new Point3D(-200, -200,   0),
			new Point3D(   0,    0, 200)
			);
	Tri t3a = new Tri(
			new Point3D(-100, -100,   0),
			new Point3D(-100,  100,   0),
			new Point3D(   0,    0, 100)
			);
	Tri t3b = new Tri(
			new Point3D(-200, -200,   0),
			new Point3D(-200,  200,   0),
			new Point3D(   0,    0, 200)
			);
	Tri t4a = new Tri(
			new Point3D(-100,  100,   0),
			new Point3D( 100,  100,   0),
			new Point3D(   0,    0, 100)
			);
	Tri t4b = new Tri(
			new Point3D(-200,  200,   0),
			new Point3D( 200,  200,   0),
			new Point3D(   0,    0, 200)
			);
	Tri t5a = new Tri(
			new Point3D( 100,  100, 0),
			new Point3D(-100,  100, 0),
			new Point3D(-100, -100, 0)
			);
	Tri t5b = new Tri(
			new Point3D( 200,  200, 0),
			new Point3D(-200,  200, 0),
			new Point3D(-200, -200, 0)
			);
	Tri t6a = new Tri(
			new Point3D( 100,  100, 0),
			new Point3D( 100, -100, 0),
			new Point3D(-100, -100, 0)
			);
	Tri t6b = new Tri(
			new Point3D( 200,  200, 0),
			new Point3D( 200, -200, 0),
			new Point3D(-200, -200, 0)
			);
	mr2.addMFacet3D(new MFacet3D(t1a, t1b));
	mr2.addMFacet3D(new MFacet3D(t2a, t2b));
	mr2.addMFacet3D(new MFacet3D(t3a, t3b));
	mr2.addMFacet3D(new MFacet3D(t4a, t4b));
	mr2.addMFacet3D(new MFacet3D(t5a, t5b));
	mr2.addMFacet3D(new MFacet3D(t6a, t6b));
//        model.addMFace(mr2);
        
      	MRegion3D mr3 = new MRegion3D(0, 36000000);
        Tri mr3a1 = new Tri(
                new Point3D(  0,   0,   0),
                new Point3D(100,   0,   0),
                new Point3D( 50,  87,   0)
        );
        Tri mr3a2 = new Tri(
                new Point3D(  0,   0,   0),
                new Point3D(100,   0,   0),
                new Point3D( 50,  87,   0)
        );
        
        Tri mr3b1 = new Tri(
                new Point3D(  0,   0,   0),
                new Point3D( 50,  43,   87),
                new Point3D(100,   0,   0)
        );
        Tri mr3b2 = new Tri(
                new Point3D(  0,   0,   0),
                new Point3D( 50,  43,   87),
                new Point3D(100,   0,   0)
        );
        
        Tri mr3c1 = new Tri(
                new Point3D(100,   0,   0),
                new Point3D( 50,  43,   87),
                new Point3D( 50,  87,   0)
        );
        Tri mr3c2 = new Tri(
                new Point3D(100,   0,   0),
                new Point3D( 50,  43,   87),
                new Point3D( 50,  87,   0)
        );
        
        Tri mr3d1 = new Tri(
                new Point3D( 50,  87,   0),
                new Point3D( 50,  43,   87),
                new Point3D(  0,   0,   0)
        );
        Tri mr3d2 = new Tri(
                new Point3D( 50,  87,   0),
                new Point3D( 50,  43,   87),
                new Point3D(  0,   0,   0)
        );
        
        mr3.addMFacet3D(new MFacet3D(mr3a1, mr3a2));
        mr3.addMFacet3D(new MFacet3D(mr3b1, mr3b2));
        mr3.addMFacet3D(new MFacet3D(mr3c1, mr3c2));
        mr3.addMFacet3D(new MFacet3D(mr3d1, mr3d2));
        
	model.addMFace(mr3);

    }

    BranchGroup shapebranch;
    TransformGroup tg;

    public void showTriangles(TriangleArray tri) {
        if (tg != null)
            tg.removeChild(shapebranch);
        Appearance app = new Appearance();
        PolygonAttributes pa = new PolygonAttributes();
        pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        app.setPolygonAttributes(pa);
        Shape3D shape = new Shape3D();
        shape.setAppearance(app);
        shape.setGeometry(tri);
        shapebranch = new BranchGroup();
        shapebranch.setCapability(BranchGroup.ALLOW_DETACH);
        shapebranch.addChild(shape);

        if (tg != null) {
            tg.addChild(shapebranch);
            bg.detach();
            bg.compile();
            universe.addBranchGraph(bg);
        } else {
            Transform3D viewtransform3d = new Transform3D();
            viewtransform3d.setTranslation(new Vector3f(0.0f, 0.0f, 1.0f));
            tg = new TransformGroup(viewtransform3d);
            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            tg.setCapability(Group.ALLOW_CHILDREN_EXTEND);
            tg.setCapability(Group.ALLOW_CHILDREN_WRITE);
            tg.addChild(shapebranch);
            MouseRotate rotor = new MouseRotate();
            rotor.setFactor(0.01f);
            rotor.setSchedulingBounds(new BoundingSphere());
            MouseTranslate trans = new MouseTranslate();
            trans.setSchedulingBounds(new BoundingSphere());
            trans.setTransformGroup(tg);
            rotor.setTransformGroup(tg);
            if (bg != null) {
                universe.getLocale().removeBranchGraph(bg);
            }
            bg = new BranchGroup();
//        if (MI_LightBackground.isSelected()) {
//            Background background = new Background(255, 255, 255);
//            background.setCapability(Background.ALLOW_COLOR_WRITE);
//            BoundingSphere sphere = new BoundingSphere(new Point3d(0.0, 0.0,
//                    0.0),
//                    1000.0);
//            background.setApplicationBounds(sphere);
//            bg.addChild(background);
//        }
            bg.addChild(tg);
            bg.addChild(rotor);
            bg.addChild(trans);
            MouseWheelZoom mwz = new MouseWheelZoom(MouseBehavior.INVERT_INPUT);
            mwz.setTransformGroup(universe.getViewingPlatform()
                    .getViewPlatformTransform());
            mwz.setSchedulingBounds(new BoundingSphere());
            bg.addChild(mwz);
            bg.setCapability(BranchGroup.ALLOW_DETACH);
            bg.compile();
            universe.addBranchGraph(bg);
            TransformGroup tg3 = universe.getViewingPlatform()
                    .getViewPlatformTransform();
//        universe.getViewer().getView()
//                .setSceneAntialiasingEnable(MI_AntiAliasing.isSelected());
            Transform3D t3d = new Transform3D();
            t3d.setTranslation(new Vector3f(0.0f, 0.0f, 25));
            tg3.setTransform(t3d);
        }
    }

    static Point3d getPoint(Point3D p) {
        return new Point3d(p.x, p.y, p.z);
    }

    TriangleArray getTriangles(MRegion3D mr, long time) {
        TriangleArray ret = new TriangleArray(mr.facets.size() * 3,
                TriangleArray.COORDINATES | TriangleArray.COLOR_3);
        List<Point3d> points = new LinkedList();
        for (Tri t : mr.project(time)) {
            points.add(getPoint(t.p1));
            points.add(getPoint(t.p2));
            points.add(getPoint(t.p3));
//            points.add(getPoint(t.p1));
//            points.add(getPoint(t.p3));
//            points.add(getPoint(t.p2));
        }
        FixCoordinates(points);
        int i = 0;
        Color3b cur = null;
        for (Point3d p : points) {
            ret.setCoordinate(i, p);
            if (i % 3 == 0) {
                cur = colors[(i / 3) % colors.length];
            }
            ret.setColor(i, cur);
            i++;
        }

        return ret;
    }

    @Override
    public void modelChanged(MRModel m) {
        this.model = m;

        for (MovingObject mo : m.getMfaces()) {
            if (mo instanceof MRegion3D) {
                MRegion3D mr3d = (MRegion3D) mo;
                showTriangles(getTriangles(mr3d, m.getCurrentTime()));
                break;
            }
        }

    }

    public void writeSnapshot(FileOutputStream fos) throws IOException {
//        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
//        Graphics2D graphics = img.createGraphics();
//        paintComponent(graphics);
//        graphics.dispose();
//        ImageIO.write(img, "png", fos);
    }

    public void writeSnapshot(FileOutputStream fos, double frac) throws IOException {
//        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
//        Graphics2D graphics = img.createGraphics();
//        long cur = model.getCurrentTime();
//        model.setCurrentFrac(frac);
//        paintComponent(graphics);
//        model.setCurrentTime(cur);
//        graphics.dispose();
//        ImageIO.write(img, "png", fos);
    }

    Double offx;
    Double offy;
    Double offz;
    Double scalex;
    Double scaley;
    Double scalez;

    private void FixCoordinates(List<Point3d> l) {
        if (l.isEmpty()) {
            return;
        }
        double minx, maxx, miny, maxy, minz, maxz;
        List<Point3d> ret = new LinkedList();
        Point3d fp = l.get(0);
        minx = maxx = fp.x;
        miny = maxy = fp.y;
        minz = maxz = fp.z;

        // Determine the minimum and maximum values of the coordinates for
        // each axis
        for (int i = 1; i < l.size(); i++) {
            Point3d p = l.get(i);
            if (p.x < minx) {
                minx = p.x;
            }
            if (p.x > maxx) {
                maxx = p.x;
            }
            if (p.y < miny) {
                miny = p.y;
            }
            if (p.y > maxy) {
                maxy = p.y;
            }
            if (p.z < minz) {
                minz = p.z;
            }
            if (p.z > maxz) {
                maxz = p.z;
            }
        }

        // Calculate offset and scale-factors from the result
	if (offx == null) {
		System.out.println("Setting offset and scale");
		offx = -minx;
		offy = -miny;
		offz = -minz;
		scalex = RANGEX / (maxx - minx);
		scaley = RANGEY / (maxy - miny);
		scalez = RANGEZ / (maxz - minz);
	}

        // and transform all points with that parameters
        for (int i = 0; i < l.size(); i++) {
            Point3d p = l.get(i);
            p.x = (p.x + offx) * scalex - RANGEX / 2;
            p.y = (p.y + offy) * scaley - RANGEY / 2;
            p.z = (p.z + offz) * scalez - RANGEZ / 2;
        }
    }
}
