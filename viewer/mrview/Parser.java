package mrview;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class Parser {

    String text;

    public void read(String filename) throws IOException {
        text = readFile(filename, Charset.defaultCharset());
    }
    
    public void write (String filename) throws IOException {
        writeFile(filename, text, Charset.defaultCharset());
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    private static int getDouble(String text, int offset) {
        do {
            char ch = text.charAt(offset);
            if (ch >= '0' && ch <= '9' || ch == 'e' || ch == '+' || ch == '-' || ch == '.' || ch == 'E')
                offset++;
            else
                break;
        } while (true);
        
        return offset;
    }
    
    private static int getSymbol(String text, int offset) {
        do {
            char ch = text.charAt(offset);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == '_')
                offset++;
            else
                break;
        } while (true);
        
        return offset;
    }
    
    public NL parse() {
        int p = 0;
        NL ret = new NL();
        NL cur = ret;
        int mb = -1;
        do {
            char ch = text.charAt(p);
            int l = text.length() - p;
            if (ch == '(') {
                cur = cur.nest();
                p++;
            } else if (ch == ')') {
                cur = cur.getParent();
                p++;
            } else if (l >= 4 && text.substring(p, p+4).startsWith("TRUE")) {
                cur.addBoolean(Boolean.TRUE);
                p += 4;
            } else if (l >= 5 && text.substring(p, p+5).startsWith("FALSE")) {
                cur.addBoolean(Boolean.FALSE);
                p += 5;
            } else if (ch == '\"') {
                int start = p;
                p++;
                while (text.charAt(p) != '\"')
                    p++;
                cur.addStr(text.substring(start+1, p));
                p++;
            } else if (ch >= '0' && ch <= '9' || ch == '+' || ch == '-') {
                int off = getDouble(text, p);
                Double d = Double.parseDouble(text.substring(p, off));
                cur.addNr(d);
                p = off;
            } else if (ch == '\n' || ch == '\r' || ch == ' ' || ch == '\t') {
                p++;
            } else {
                int off = getSymbol(text, p);
                cur.addSym(text.substring(p, off));
                p = off;
            }
            int nmb = p / 1000000;
            if (nmb != mb) {
                System.out.println("read MB: "+nmb);
                mb = nmb;
            }
        } while (p < text.length()-1);
        
        return ret.get(0);
    }

    public NL parse2() {
        text = text.replace(")", " ) ").replace("(", " ( ");
        String[] nlt = text.split("[\r\n\t ]+");
        NL ret = new NL();
        NL cur = ret;
        for (String s : nlt) {
            if (s.startsWith("(") || s.startsWith(")")) {
                for (int i = 0; i < s.length(); i++) {
                    if (s.charAt(i) == '(') {
                        cur = cur.nest();
                    }
                    if (s.charAt(i) == ')') {
                        cur = cur.getParent();
                    }
                }
            } else if (s.equals("TRUE")) {
                cur.addBoolean(Boolean.TRUE);
            } else if (s.equals("FALSE")) {
                cur.addBoolean(Boolean.FALSE);
            } else if (s.startsWith("\"")) {
                cur.addStr(s.substring(1, s.length() - 1));
            } else if (s.matches("-?[0-9\\.e+-]{1,20}")) {
                cur.addNr(Double.parseDouble(s));
            } else if (!s.isEmpty()) {
                if (s.startsWith("#"))
                    s = s.substring(1);
                cur.addSym(s);
            }
        }

        return ret.get(0);
    }

    public NL parseMRegion() {
        return parse().get(4);
    }

    @SuppressWarnings("empty-statement")
    static String readFile(String path, Charset encoding)
            throws IOException {
        if (path.equals("-")) {
            ByteBuffer buf = ByteBuffer.allocate(1000000);
            ReadableByteChannel channel = Channels.newChannel(System.in);
            while (channel.read(buf) >= 0)
                ;
            buf.flip();
            byte[] bytes = Arrays.copyOf(buf.array(), buf.limit());
            return new String(bytes, encoding);

        } else {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            return new String(encoded, encoding);
        }
    }
    
    static void writeFile(String path, String text, Charset encoding)
            throws IOException {
        if (path.equals("-")) {
            ByteBuffer buf = ByteBuffer.wrap(text.getBytes());
            WritableByteChannel channel = Channels.newChannel(System.out);
            channel.write(buf);
        } else {
            Files.write(Paths.get(path), text.getBytes());
        }
    }
    
    public static NL readNL (String path) {
        NL nl = null;
        try {
            String text = readFile(path, Charset.defaultCharset());
            Parser p = new Parser();
            p.setText(text);
            nl = p.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return nl;
    }
    
    public static void writeNL (String path, NL nl) {
        try {
            String text = nl.toString();
            writeFile(path, text, Charset.defaultCharset());
            Parser p = new Parser();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Face> readRegionFile(String filename) throws IOException {
        Parser p = new Parser();
        p.read(filename);
        NL nl = p.parse();
        return parseRegion(nl);
    }

    public static List<MFace> readMRegionFile(String filename) throws IOException {
        Parser p = new Parser();
        p.read(filename);
        NL nl = p.parse();
        if (nl == null) {
            return null;
        }
        return parseURegions(nl);
    }
    
    public static MFace readFMFaceFile(String filename) throws IOException {
        Parser p = new Parser();
        p.read(filename);
        NL nl = p.parse();
        if (nl == null) {
            return null;
        }
        Face f = parseFaceWithHoles(nl.get(0));
        NL params = nl.get(1);
        System.out.println(params);
        Point c = parsePoint(params.get(0));
        Point v = parsePoint(params.get(1));
        double angle = params.get(2).getNr();
        NL timeinterval = params.get(3);
        long ts = parseDate(timeinterval.get(0));
        long te = parseDate(timeinterval.get(1));
        return MFace.createFMFace(f, c, v, angle);
    }

    private static List<Face> parseRegion(NL nl) {
        List<Face> ret = new LinkedList();

        for (NL l : nl.getNl()) {
            ret.add(parseFaceWithHoles(l));
        }

        return ret;
    }

    private static Face parseFaceWithHoles(NL nl) {
        Face face = null;
        try {
            face = parseFace(nl.get(0));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error parsing: \n" + nl.toString());
            return null;
        }

        face.sort();
        for (int i = 1; i < nl.size(); i++) {
            Face hole = parseFace(nl.get(i));
            hole.sort();
            face.addHole(hole);
        }

        return face;
    }

    private static Face parseFace(NL nl) {
        Face ret = new Face();

        for (NL l : nl.getNl()) {
            ret.addPointRaw(parsePoint(l));
        }

        ret.close();

        return ret;
    }
    
    private static Point parsePoint(NL nl) {
        double x = nl.get(0).getNr();
        double y = nl.get(1).getNr();
        Point pt = new Point(x, y);

        return pt;
    }

    public static List<MFace> parseURegions(NL nl) {
        List<MFace> ret = new LinkedList();
        for (NL l : nl.getNl()) {
            List<MFace> mfs = parseURegion(l);
            ret.addAll(mfs);
        }

        return ret;
    }

    private static Long parseDate(String date) {
        String[] formats = {
            "yyyy-MM-dd-HH:mm:ss.SS",
            "yyyy-MM-dd-HH:mm:ss",
            "yyyy-MM-dd-HH:mm",
            "yyyy-MM-dd",};

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                long ret = sdf.parse(date).getTime();
                System.out.println("Parsed date "+date+" to "+ret);

                return ret;
            } catch (Exception e) {
            }
        }

        return null;
    }
    
    public static String getDate (long t) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        Date d = new Date(t);
        
        return sdf.format(d);
    }
    
    public static Long parseDate (NL nl) {
        return parseDate(nl.getStr());
    }

    public static List<MFace> parseURegion(NL nl) {
        List<MFace> mfs = new LinkedList();
        NL interval = nl.get(0);

        long start = parseDate(interval.get(0).getStr());
        long end = parseDate(interval.get(1).getStr());

        NL faces = nl.get(1);
        for (NL l : faces.getNl()) {
            MFace mf = parseMFace(l, start, end);
            mfs.add(mf);
        }

        return mfs;
    }

    public static MFace parseMFace(NL nl, long start, long end) {
        List<MovingSeg> l = parseMCycle(nl.get(0), start, end);
        MFace mf = new MFace(l, start, end);
        for (int i = 1; i < nl.size(); i++) {
            l = parseMCycle(nl.get(i), start, end);
            mf.addHole(new MFace(l, start, end));
        }

        return mf;
    }

    public static List<MovingSeg> parseMCycle(NL nl, long start, long end) {
        List<MovingSeg> ret = new LinkedList();

        for (int i = 0; i < nl.size() - 1; i++) {
            double hs1[] = parseMHS(nl.get(i));
            double hs2[] = parseMHS(nl.get(i + 1));
            MSeg ms = new MSeg(
                    new Seg(hs1[0], hs1[1], hs1[2], hs1[3]),
                    new Seg(hs2[0], hs2[1], hs2[2], hs2[3]),
                    start, end
            );
            ret.add(ms);
        }
        double hs1[] = parseMHS(nl.get(nl.size() - 1));
        double hs2[] = parseMHS(nl.get(0));
        MSeg ms = new MSeg(
                new Seg(hs1[0], hs1[1], hs2[0], hs2[1]),
                new Seg(hs1[2], hs1[3], hs2[2], hs2[3]),
                start, end
        );
        ret.add(ms);

        return ret;
    }

    public static double[] parseMHS(NL nl) {
        double ret[] = new double[4];

        ret[0] = nl.get(0).getNr();
        ret[1] = nl.get(1).getNr();
        ret[2] = nl.get(2).getNr();
        ret[3] = nl.get(3).getNr();

        return ret;
    }

}
