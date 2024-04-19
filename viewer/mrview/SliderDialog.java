package mrview;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class SliderDialog extends JDialog implements ChangeListener {
    private MDot mo;
    private JSlider js;
    private JComponent parent;

    public SliderDialog(MovingObject mo, JComponent parent) {
        this.mo = (MDot) mo;
        this.parent = parent;
        
        js = new JSlider(-1000, 1000, (int) Math.round(this.mo.getAOff()));
        js.addChangeListener(this);
        this.add(js);
        this.setSize(600, 100);
        this.setVisible(true);
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        int value = js.getValue();
        mo.setAOff(value);
        System.out.println("aoff: "+value);
        parent.repaint();
    }
    
}
