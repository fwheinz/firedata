package mrview;

public interface SecondoObject {
    public String getSecondoType();
    public String getObjName();
    public void setObjName(String name);
    public SecondoObject deserialize (NL nl);
    public NL serialize();
}
