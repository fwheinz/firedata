package mrview;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import static mrview.MRViewWindow.m;

public class Util {

    static void changePerspective(MFace mf, MPointOld mp) {
        if (mp.getInnerVector() != null) {
            return; // Was already changed
        }
        FMSeg s = (FMSeg) mf.getSegs().get(0);
        mp.setCenter(s.getCenter());
        mp.setRotate(-s.getRotation());
        mp.setInnerVector(s.getVector());
        mp.setVector(mp.getVector().sub(s.getVector()));
        for (MovingSeg ms : mf.getSegs()) {
            s = (FMSeg) ms;
            s.setVector(new Point(0, 0));
            s.setRotation(0);
        }
    }

    static void restorePerspective(MFace mf, MPointOld mp) {
        if (mp.getInnerVector() == null) {
            return; // Perspective is not changed
        }
        double rotation = mp.getRotate();
        mp.setRotate(0);
        Point iv = mp.getInnerVector();
        mp.setInnerVector(null);
        mp.setCenter(null);
        mp.setVector(mp.getVector().add(iv));
        for (MovingSeg ms : mf.getSegs()) {
            if (!(ms instanceof FMSeg)) {
                continue;
            }
            FMSeg s = (FMSeg) ms;
            s.setVector(iv);
            s.setRotation(-rotation);
        }
    }

    static void inside(MFace mf, MPointOld mp) {
        Plot.reset("Before transformation");
        mf.printGraphs("f", 1);
        Point cp = mf.getFMSeg().getCenter();
        Point cv = mf.getFMSeg().getVector();
        Seg center = new Seg(cp, cp.add(cv));
        center.printGraph("c", 6, -1);
        Plot.arrow(center, 6);
        mp.printGraph("mp", 1);
        Plot.arrow(mp.getSeg(), 1);

        Plot.doPlot();
        Plot.doPlot("postscript eps enhanced color", "bt.eps");
        Util.changePerspective(mf, mp);
        Plot.reset("After transformation");
        mp.printGraph("mp", 1);
        int i = 1;
        for (MovingSeg ms : mf.getSegs()) {
            if (ms instanceof FMSeg) {
                FMSeg fms = (FMSeg) ms;
                fms.getInitial().printGraph("seg", 3 + i, i);
                Seg seg = fms.getInitial();
                i++;
            }
        }
        Plot.doPlot();
        Plot.doPlot("postscript eps enhanced color", "at.eps");
        Plot.reset("Segment Graphs");
        Plot.xlabel("t");
        Plot.xaxis();
        i = 1;
        for (MovingSeg ms : mf.getSegs()) {
            if (ms instanceof FMSeg) {
                FMSeg fms = (FMSeg) ms;
                Seg seg = fms.getInitial();
                mp.printSegGraph(seg, 0, "seg" + i, 3 + i);
                i++;
            }
        }
        Plot.doPlot();
        Plot.doPlot("postscript eps enhanced color", "sg.eps");
        List<Double> intersections = new LinkedList();
        for (MovingSeg ms : mf.getSegs()) {
            if (ms instanceof FMSeg) {
                System.out.println("Current segment: " + ms.toString());
                FMSeg mseg = (FMSeg) ms;
                Seg seg = mseg.project(mseg.getStartTime());
                List<Double> ret = mp.segmentIntersections(seg);
                intersections.addAll(ret);
            } else {
                System.out.println("Unknown class " + ms.getClass().getCanonicalName());
            }
        }
        intersections.add(0.0);
        intersections.add(1.0);
        Collections.sort(intersections);
        i = 0;
        while (i < intersections.size() - 1) {
            double ts = intersections.get(i);
            double te = intersections.get(i + 1);
            double t = (ts + te) / 2;
            Point p = mp.project(t);
            boolean inside = mf.project(t).inside(p);
            String boolstr = inside ? "TRUE" : "FALSE";
            long start = mp.getStartTime();
            long end = mp.getEndTime();
            long si = (long) (intersections.get(i) * (end - start) + start);
            long ei = (long) (intersections.get(i + 1) * (end - start) + start);
            System.out.println(ts + " " + te + " " + boolstr);
            i++;
        }
    }

    public static double distance(Point lp1, Point lp2, Point p) {
        return ((lp2.y - lp1.y) * p.x - (lp2.x - lp1.x) * p.y + lp2.x * lp1.y - lp2.y * lp1.x)
                / Math.sqrt((lp2.y - lp1.y) * (lp2.y - lp1.y) + (lp2.x - lp1.x) * (lp2.x - lp1.x));
    }

    public static MRegion interpolate(Region r1, Region r2, String params) {
        try {
            ImportExport.exportObject("/tmp/src", r1, true);
            ImportExport.exportObject("/tmp/dst", r2, true);
            System.out.println("Starting rip");
            Process p = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c",
                "~/librip/example/rip /tmp/src /tmp/dst \"1970-01-01 01:00\" \"1970-01-01 01:16:40\" \""+params+"\" > /tmp/interp 2>/dev/null"});
            System.out.println("Waiting for rip termination");
            p.waitFor();
            System.out.println("Done waiting, importing result...");
            MRegion mr = (MRegion) ImportExport.importObject("/tmp/interp");
            System.out.println("All done...");
            return mr;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return null;
    }
}
