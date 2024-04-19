package mrview;

public interface MovingSeg {
    public Seg project (long currentTime);
    public Seg project (double t);
    public BoundingBox getBoundingBox ();
}
