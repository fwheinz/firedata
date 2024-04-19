package mrview;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRModel implements ChangeListener {

    private Point curpos, dragstartpos;
    private JSlider slider;
    private JLabel pos, status;
    private long minTime = 0, currentTime, maxTime = 1000000;
    private BoundingBox boundingBox;
    private Seg dragStartSeg, dragEndSeg;
    private final List<MovingObject> mfaces = new LinkedList();
    private final List<MRListener> listener = new LinkedList();
    private final List<Face>[] faces = new List[]{new LinkedList(), new LinkedList()};
    private int srcdst = 0, highlight = 0;
    private boolean replace = true, duration = false, _3dmode = false;
    private Object focus;
    private ObjectCreator creator;

    public void recalculateBoundingBox() {
        boundingBox = new BoundingBox();
        minTime = -1;
        maxTime = -1;
        for (MovingObject f : getMfaces()) {
            if (f instanceof MRegion3D && !_3dmode)
                continue;
            if (!(f instanceof MRegion3D) && _3dmode)
                continue;
            boundingBox.update(f.getBoundingBox());
            long st = f.getStartTime();
            if ((st >= 0) && ((st < minTime) || (minTime < 0))) {
                minTime = st;
            }
            long et = f.getEndTime();
            if ((et >= 0) && (et > maxTime || (minTime < 0))) {
                maxTime = et;
            }
        }
        for (int i = 0; i < 2; i++) {
            for (Face f : faces[i]) {
                if (f == null) {
                    continue;
                }
                for (Seg s : f.getSegments()) {
                    updateBoundingBox(s.s);
                }
            }
        }
        if (getBoundingBox() == null) {
            boundingBox = new BoundingBox();
            boundingBox.update(new Seg(new Point(-100, -500), new Point(1000, 500)));
        }
    }

    public MovingObject getFirstObject(Class c) {
        for (MovingObject mo : getMfaces()) {
            if (mo.getClass() == c) {
                return mo;
            }
        }

        return null;
    }

    public boolean addFace(Face f) {
        if (f.intersects(faces[srcdst])) {
            return false;
        }

        faces[srcdst].add(0, f);
        recalculateBoundingBox();
        modelChanged();

        return true;
    }

    public List<Face> getFaces(int srcdst) {
        return faces[srcdst];
    }

    public void addMFace(MovingObject s) {
        if (s == null) {
            System.out.println("Warning: adding NULL mface");
            return;
        }
        if (!mfaces.contains(s)) {
            getMfaces().add(s);
            recalculateBoundingBox();
            modelChanged();
        }
    }

    public void removeMFace(MovingObject o) {
        mfaces.remove(o);
        recalculateBoundingBox();
        modelChanged();
    }

    public void addMFace(Set<MovingObject> mos) {
        for (MovingObject mo : mos) {
            addMFace(mo);
        }
    }

    public void relocateFace() {
        if (focus instanceof Face) {
            Face f = (Face) focus;
            Point vector = Point.random(300);
            Random r = new Random();
            double rotation = r.nextDouble() * Math.PI;
            System.out.println("Vector: " + vector.toString() + " Rotation: " + rotation);

            MFace mf = MFace.createFMFace(f, f.getCenter(), vector, r.nextDouble() * Math.PI);
            Face nf = mf.project(1.0);
            addFace(nf);
            System.out.println("Added Face: " + nf.toString());
        }
    }

    public boolean importRegionFile(String filename, int sd) {
        System.out.println("Importing " + filename);
        if (sd < 2) {
            try {
                List<Face> fcs = Parser.readRegionFile(filename);
                if (replace) {
                    faces[sd] = fcs;
                } else {
                    fcs.addAll(faces[sd]);
                    faces[sd] = fcs;
                }
                recalculateBoundingBox();
                modelChanged();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            try {
                List<MFace> mfs = Parser.readMRegionFile(filename);
                if (replace) {
                    this.getMfaces().clear();
                }
                for (MFace mf : mfs) {
                    this.addMFace(mf);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public boolean importRegionFile(String filename) {
        return importRegionFile(filename, this.srcdst);
    }

    public void addPoint() {
        Face f;

        if (creator != null) {
            creator.click(curpos.x, curpos.y, ObjectCreator.LEFTCLICK);
            if (creator.nextAction() == ObjectCreator.READY) {
                Object o = creator.getObject(minTime, maxTime);
                if (o instanceof MovingObject) {
                    addMFace((MovingObject) creator.getObject(minTime, maxTime));
                } else if (o instanceof Face) {
                    faces[srcdst % 2].add((Face) o);
                }
                creator = null;
            }
            modelChanged();
        } else {
            focus = findObject(new Point(curpos.x, curpos.y));
            modelChanged();
        }
    }

    private boolean dragging = false;

    public void dragEvent(Point r) {
        if (dragging == false) {
            calculateDragSegments();
            dragstartpos = r;
            dragging = true;
        }
        cursorPosition(r);
    }

    private Seg findSegment(Point pt, boolean start) {
        if (getSrcdst() > 1) {
            return null;
        }
        for (Face f : faces[getSrcdst()]) {
            for (Seg s : f.getSegments()) {
                if ((start && s.s.near(pt)) || (!start && s.e.near(pt))) {
                    return s;
                }
            }
        }

        return null;
    }

    private Object findObject(Point pt) {

        for (Face f : faces[0]) {
            for (Seg seg : f.getSegments()) {
                if (seg.s.near(pt) || seg.e.near(pt)) {
                    return f;
                }
            }
        }

        for (Face f : faces[1]) {
            for (Seg seg : f.getSegments()) {
                if (seg.s.near(pt) || seg.e.near(pt)) {
                    return f;
                }
            }
        }

        for (MovingObject mo : getMfaces()) {
            if (mo instanceof MFace) {
                MFace mf = (MFace) mo;
                Face f = mf.project(currentTime);
                for (Seg seg : f.getSegments()) {
                    if (seg.s.near(pt) || seg.e.near(pt)) {
                        return mo;
                    }
                }
            }
        }

        return null;
    }

    private void calculateDragSegments() {
        dragStartSeg = findSegment(curpos, true);
        dragEndSeg = findSegment(curpos, false);
    }

    public void removeDragSegments() {
        dragStartSeg = null;
        dragEndSeg = null;
        dragstartpos = null;
        dragging = false;
    }

    public void closeFace() {
        if (srcdst > 1) {
            return;
        }
        if (faces[getSrcdst()].isEmpty()) {
            return;
        }
        Face f = faces[getSrcdst()].get(faces[getSrcdst()].size() - 1);
        if (f.getSegments().size() >= 2) {
            f.close();
        }
        modelChanged();
    }

    public void undo() {
        if (srcdst > 1) {
            return;
        }
        if (faces[getSrcdst()].isEmpty()) {
            return;
        }
        Face f = faces[getSrcdst()].get(faces[getSrcdst()].size() - 1);
        if (!f.removeLastPoint()) {
            faces[getSrcdst()].remove(faces[getSrcdst()].size() - 1);
        }
        modelChanged();
    }

    private void updateBoundingBox(Point p) {
        if (getBoundingBox() == null) {
            boundingBox = new BoundingBox();
        }
        boundingBox.update(p);
    }

    public void addChangeListener(MRListener mrl) {
        if (!listener.contains(mrl)) {
            listener.add(mrl);
            mrl.modelChanged(this);
        }
    }

    @SuppressWarnings("empty-statement")
    public void removeChangeListener(MRListener mrl) {
        while (listener.remove(mrl))
            ;
    }

    private static boolean inChange = false;

    public void modelChanged() {
        if (inChange) {
            return;
        }
        inChange = true;
        for (MRListener l : listener) {
            l.modelChanged(this);
        }
        if (status != null) {
            if (creator != null) {
                status.setText(creator.stateDescription());
            } else {
                status.setText("");
            }
        }
        if (slider != null) {
//            int delta = (int) (maxTime - minTime);
            slider.setMinimum(0);
            slider.setMaximum(100000000);
        }
        inChange = false;
    }

    public void paint(MRGraphics g) {
        g.textline = 0;
        BoundingBox b = getBoundingBox();
        g.setBoundingBox(b);
        for (MovingObject mf : getMfaces()) {
            if (!mf.isVisible()) {
                continue;
            }
            mf.paint(g, currentTime, focus == mf || mf.isHighlighted());
        }
        for (Face f : faces[0]) {
            f.paint(g, 0, focus == f);
        }
        for (Face f : faces[1]) {
            f.paint(g, 1, focus == f);
        }
        if (creator != null) {
            creator.paint(g);
        }
    }

    public JSlider getSlider() {
        return slider;
    }

    public void setSlider(JSlider slider) {
        this.slider = slider;
        slider.addChangeListener(this);
//        int delta = (int) (maxTime - minTime);
        slider.setMinimum(0);
        slider.setMaximum(100000000);
    }

    public void setPositionLabel(JLabel pos) {
        this.pos = pos;
    }

    public void setStatusLabel(JLabel status) {
        this.status = status;
    }
    
    private String getDuration (long time) {
        StringBuilder sb = new StringBuilder();
        long msec = time%1000;
        time /= 1000;
        long sec = time%60;
        time /= 60;
        long min = time%60;
        time /= 60;
        long hour = time%24;
        time /= 24;
        long day = time%7;
        time /= 7;
        long week = time%52;
        time /= 52;
        long year = time;
        
        if (year > 0)
            sb.append(String.format("%d", year)).append("y");
        if (week > 0 || year > 0)
            sb.append(String.format("%02d", week)).append("w");
        if (day > 0 || week > 0 || year > 0)
            sb.append(String.format("%d", day)).append("d");
        if (hour > 0 || day > 0 || week > 0 || year > 0)
            sb.append(String.format("%02d", hour)).append("h");
        if (min > 0 || hour > 0 || day > 0 || week > 0 || year > 0)
            sb.append(String.format("%02d", min)).append("m");
        sb.append(String.format("%02d", sec)).append("s");
        sb.append(String.format("%03d", msec));
        
        return sb.toString();
    }

    public void cursorPosition(Point p) {
        curpos = p;
        if (p != null) {
            if (dragStartSeg != null) {
                dragStartSeg.s = p;
            }
            if (dragEndSeg != null) {
                dragEndSeg.e = p;
            }
            if (dragStartSeg == null && dragEndSeg == null && dragstartpos != null) {
                Point delta = p.sub(dragstartpos);
                boundingBox.ll = boundingBox.ll.sub(delta);
                boundingBox.ur = boundingBox.ur.sub(delta);
            }
        }
        double frac = ((double) (currentTime - minTime)) / (maxTime - minTime);
//        pos.setText(((int) p.x) + " x " + ((int) p.y) + " @" + frac+" ("+currentTime+")"+Parser.getDate(currentTime));
        StringBuilder sb = new StringBuilder();
        if (p != null) {
            sb.append((int) p.x).append(" x ").append((int) p.y).append("    ");
        }
        String time = duration ? getDuration(currentTime) : Parser.getDate(currentTime);
        sb.append(time).append(" (").append(String.format("%.2f%%", frac * 100)).append(")");
        pos.setText(sb.toString());
        pos.getParent().revalidate();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        currentTime = (long) slider.getValue() * (maxTime - minTime) / 100000000 + minTime;
        highlight = 2;
        cursorPosition(curpos);
        modelChanged();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (MovingObject f : getMfaces()) {
            sb.append(f.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public String createFaceNl(int srcdst) {
        StringBuilder sb = new StringBuilder();
        sb.append("(\n");
        for (int i = 0; i < faces[srcdst].size(); i++) {
            Face f = faces[srcdst].get(i);
            if (f.isClosed()) {
                sb.append(faces[srcdst].get(i).nl());
            }
        }
        sb.append(")\n");

        return sb.toString();
    }

    public void interp2() {
        Face f1 = faces[0].get(0);
        Face f2 = faces[1].get(0);

        Seg.TransformParam tp = f1.findTransformParam(f2, null, 0.1);
        if (tp == null) {
            System.out.println("Interpolation failed!");
        } else {
            // Change the center point to the geometric gravity center of the face
//            Point newcenter = f1.getCenter();
//            Point oldcenterprojection = tp.center.rotate(newcenter, tp.angle);
//            Point vectorcorrection = tp.center.sub(oldcenterprojection);
//            Point newvector = tp.vector.add(vectorcorrection);
            MFace mf = MFace.createFMFace(f1, tp.center, tp.vector, tp.angle);
            addMFace(mf);
        }

    }

    /**
     * @return the srcdst
     */
    public int getSrcdst() {
        return srcdst;
    }

    /**
     * @param srcdst the srcdst to set
     */
    public void setSrcdst(int srcdst) {
        this.srcdst = srcdst;
        this.setHighlight(srcdst);
        modelChanged();
    }

    public void clearFaces(int srcdst) {
        if (srcdst < 2) {
            faces[srcdst].clear();
        } else {
            getMfaces().clear();
        }
        recalculateBoundingBox();
        modelChanged();
    }

    /**
     * @return the highlight
     */
    public int getHighlight() {
        return highlight;
    }

    /**
     * @param highlight the highlight to set
     */
    public void setHighlight(int highlight) {
        this.highlight = highlight;
    }

    /**
     * @return the boundingBox
     */
    public BoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = new BoundingBox(0, 0, 1000, 1000);
        }
        return boundingBox;
    }

    /**
     * @return the creator
     */
    public ObjectCreator<MovingObject> getCreator() {
        return creator;
    }

    /**
     * @param creator the creator to set
     */
    public void setCreator(ObjectCreator creator) {
        this.creator = creator;
        modelChanged();
    }

    void trochoids() {
        Plot.reset("Foo");
        for (MovingObject mf : getMfaces()) {
            if (mf instanceof MFace) {
                MFace m = (MFace) mf;
                List<MovingSeg> ms = m.getSegs();
                int i = 1;
                for (MovingSeg s : ms) {
                    if (s instanceof FMSeg) {
                        FMSeg f = (FMSeg) s;
                        f.printGraph("f" + i, 1);
                        f.printGraphNormalized("fn" + i, 1);
                        i++;
                    }
                }
            }
        }
        Plot.doPlot();
    }

    void cartesiantrochoids() {
        List<Trochoid> ts = new LinkedList();
        Plot.reset("Foo");
        Plot.noparametric();
        double period = 0;
        FMSeg fm = null;
        for (MovingObject mf : getMfaces()) {
            if (mf instanceof MFace) {
                MFace m = (MFace) mf;
                List<MovingSeg> ms = m.getSegs();
                int i = 1;
                for (MovingSeg s : ms) {
                    if (s instanceof FMSeg) {
                        FMSeg f = (FMSeg) s;
                        fm = f;
                        period = f.getVector().x / (f.getRotation() / (Math.PI * 2));
                        f.printCartesianGraph("f" + i, 1);
                        ts.add(f.getTrochoid(1, f.getCenter()));
                        ts.add(f.getTrochoid(-1, f.getCenter()));
                        i++;
                    }
                }
            }
        }
        Plot.doPlot();

        for (int i = 0; i < ts.size(); i++) {
            for (int j = i + 1; j < ts.size(); j++) {
                Trochoid t1 = ts.get(i);
                Trochoid t2 = ts.get(j);
                List<Point> pts = t1.newton(t2);
                for (Point p : pts) {
                    double off = 0;
                    int nrrot = (int) Math.floor(fm.getRotation() / Math.PI * 2);
                    for (int m = 0; m <= nrrot; m++) {
                        getMfaces().add(new MPointOld(new Point(p.x + m * period, p.y), new Point(0, 0), 0, 1000000));
                        off += period;
                    }

//                    mfaces.add(new MPoint(new Point(p.x+period, p.y), new Point(0,0), 0, 1000000));
//                    mfaces.add(new MPoint(new Point(p.x+period*2, p.y), new Point(0,0), 0, 1000000));
                }
            }
        }
        modelChanged();
    }

    void wheelEvent(int units) {
        double scale = 0.1;
        if (boundingBox != null) {
            Point bbsize = boundingBox.ur.sub(boundingBox.ll);
            bbsize = bbsize.mul(scale * ((double) units));
            boundingBox.ll = boundingBox.ll.sub(bbsize);
            boundingBox.ur = boundingBox.ur.add(bbsize);
        }
    }

    /**
     * @return the mfaces
     */
    public List<MovingObject> getMfaces() {
        return mfaces;
    }

    /**
     * @return the currentTime
     */
    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * @param currentTime the currentTime to set
     */
    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public void setCurrentFrac(double frac) {
        this.currentTime = (long) (minTime * (1.0 - frac) + maxTime * frac);
    }

    /**
     * @return the duration
     */
    public boolean isDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(boolean duration) {
        this.duration = duration;
    }
    
    public void set3DMode (boolean _3dmode) {
        this._3dmode = _3dmode;
        this.recalculateBoundingBox();
    }

}
