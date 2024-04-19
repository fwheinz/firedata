package mrview;

import java.io.File;

public class ImportExport {
    private static final SecondoObject[] types = new SecondoObject[] {
        new Region(),
        new CRegion(),
        new MPoint(),
        new FMRegion(),
        new MRegion(),
        new MBool(),
        new MReal(),
        new PMRegion()
    };
    
    public static SecondoObject importObject (String filename) {
        NL nl = Parser.readNL(filename);
        if (nl == null) {
            System.out.println("Warning: NL-Parser failed for "+filename);
            return null;
        }
        
        return NLToObject(nl, filename);
    }
    
    public static SecondoObject NLToObject (NL nl, String fname) {
        SecondoObject ret = null;
        
        System.out.println("Importing object...");
        
        String sym = nl.get(0).getSym();
        if (!sym.equalsIgnoreCase("OBJECT")) {
            System.out.println("Warning: Not an object: "+fname);
            return null;
        }
        String name = nl.get(1).getSym();
        String type = nl.get(3).getSym();
        NL def = nl.get(4);
        for (SecondoObject so : types) {
            if (so.getSecondoType().equals(type)) {
                ret = so.deserialize(def);
                if (ret == null)
                    System.out.println("Warning: Deserialization failed: "+fname);
//                ret.setObjName(new File(fname).getName());
                ret.setObjName(name);
                break;
            }
        }
        if (ret == null) {
            System.out.println("Warning: Invalid object type "
                    +type+": "+fname);
        }
        
        return ret;
    }
    
    private static NL objectToNL (SecondoObject ob) {
        NL nl = new NL();
        nl.addSym("OBJECT");
        nl.addSym(ob.getObjName());
        nl.nest();
        nl.addSym(ob.getSecondoType());
        nl.addNL(ob.serialize());
        
        return nl;
    }
    
    public static String exportObject (SecondoObject ob) {
        return objectToNL(ob).toString();
    }
    
    public static void exportObject (String filename, SecondoObject ob, boolean raw) {
        NL nl = raw ? ob.serialize() : objectToNL(ob);
        Parser.writeNL(filename, nl);
    }
    
    public static void exportObject (String filename, SecondoObject ob) {
        exportObject(filename, ob, false);
    }
    
    public static Object copy(SecondoObject ob) {
        return NLToObject(objectToNL(ob), ob.getObjName()+"_copy");
    }
    
}
