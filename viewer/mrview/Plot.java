package mrview;

import java.io.OutputStream;

public class Plot {

    static private StringBuilder sb;
    static private boolean firstplot;

    static {
        reset(null);
    }

    public static void add(String s) {
        sb.append(s).append("\n");
    }
    
    public static void addxfunction(String name, String definition) {
        add(name+"(x)="+definition);
    }

    public static void plot(String prefix, String x, String y, int linestyle) {
        add(prefix + "x(t)=" + x);
        add(prefix + "y(t)=" + y);
        add((firstplot ? "" : "re") + "plot " + prefix + "x(t)," + prefix + "y(t) linestyle " + linestyle);
        firstplot = false;
    }
    
    public static void plot(String prefix, String x, int linestyle) {
        add(prefix + "f(x)=" + x);
        add((firstplot ? "" : "re") + "plot " + prefix + "f(x) linestyle " + linestyle);
        firstplot = false;
    }
    
    public static void defun (String sig, String formula) {
        add(sig+"="+formula);
    }
    
    public static void arrow (Seg s, int linestyle) {
        arrow(s.s, s.e, linestyle);
    }
    
    public static void arrow (Point p1, Point p2, int linestyle) {
        add("set arrow from "+p1.x+","+p1.y+" to "+p2.x+","+p2.y+" linestyle "+linestyle);
    }
    
    public static void xlabel(String label) {
        add("set xlabel \""+label+"\"");
    }
    
    public static void xaxis () {
        add((firstplot ? "" : "re") + "plot t,0 title \"\" linestyle -1");
        firstplot = false;
    }

    static void reset(String title) {
        sb = new StringBuilder();
        firstplot = true;
        add("set parametric");
        add("set trange [0:1]");
        add("set samples 4000");
        add("set style line 2 lt 1 lc 4");
        add("set style line 4 lt 1 lc 2");
        add("set style line 5 lt 1 lc 3");
        add("set style line 6 lt 1 lc 4");
        if (title != null)
            add("set title \""+title+"\"");
    }
    
    static void noparametric() {
        add("unset parametric");
    }

    static void doPlot(String terminal, String filename, boolean wait) {
        System.out.println("Plotting: \n"+sb.toString()+"\n\n");
        try {
            Runtime r = Runtime.getRuntime();
            Process gp = r.exec("gnuplot");
            OutputStream os = gp.getOutputStream();
            os.write(("set terminal unknown\n").getBytes());
            os.write(sb.toString().getBytes());
            os.write(("set terminal "+terminal+"\n").getBytes());
            if (filename != null) {
                os.write(("set output '"+filename+"'\n").getBytes());
                os.write("unset title\n".getBytes());
            }
            os.write("replot\n".getBytes());
            if (terminal.equals("x11"))
                os.flush();
            else
                os.close();
            if (wait)
                gp.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void doPlot(String terminal, String filename) {
        doPlot(terminal, filename, false);
    }

    static void doPlot() {
        doPlot("x11", null, false);
    }
}
