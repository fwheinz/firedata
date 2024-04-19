package mrview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRViewWindow extends JFrame {

    public static final MRModel m = new MRModel();
    private String lastPath;
    private MRViewCanvas3D mvc3d = null;

    public MRViewWindow(String[] preload) {
        this.setSize(800, 600);
	JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        JPanel south = new JPanel(new GridLayout(2, 1));
        JLabel pos = new JLabel("0 x 0");
        pos.setHorizontalAlignment(JLabel.CENTER);
        south.add(pos);
        JSlider js = new JSlider();
        south.add(js);
        this.add(south, BorderLayout.SOUTH);

        for (String s : preload) {
            m.addMFace((MovingObject) ImportExport.importObject(s));
        }

        m.setPositionLabel(pos);
        m.setSlider(js);
        final MRViewCanvas mvc = new MRViewCanvas(m);
        try {
            mvc3d = new MRViewCanvas3D(m);
        } catch (NoClassDefFoundError e) {
            System.out.println("3D disabled");
        }


        JMenuBar menuBar = new JMenuBar();

        XJMenu actions = new XJMenu("Actions");
        menuBar.add(actions);
        JMenuItem undo = new JMenuItem("Undo");
        undo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m.undo();
            }
        });
        actions.add(undo);

        JMenuItem random = new JMenuItem("Random");
        random.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (m.getSrcdst() > 1) {
                    return;
                }
                boolean success;
                do {
                    success = m.addFace(Face.createRandom(5, m.getBoundingBox()));
                } while (!success);
            }
        });
        actions.add(random);

        ButtonGroup bg = new ButtonGroup();

        XJMenu view = new XJMenu("View");
        menuBar.add(view);
        final JRadioButtonMenuItem choose2d = new JRadioButtonMenuItem("2D");
        final JRadioButtonMenuItem choose3d = new JRadioButtonMenuItem("3D");
        bg.add(choose2d);
        if (mvc3d != null)
            bg.add(choose3d);
        choose2d.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MRViewWindow.this.remove(mvc);
                MRViewWindow.this.remove(mvc3d);
                MRViewWindow.this.add(mvc, BorderLayout.CENTER);
                m.set3DMode(false);
                repaint();
                System.out.println("Switching to 2D");
            }
        });
        view.add(choose2d);

        choose3d.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MRViewWindow.this.remove(mvc);
                MRViewWindow.this.remove(mvc3d);
                MRViewWindow.this.add(mvc3d, BorderLayout.CENTER);
                m.set3DMode(true);
                repaint();
                System.out.println("Switching to 3D");
            }
        });
        if (mvc3d != null)
            view.add(choose3d);
        

       final JCheckBoxMenuItem duration = new JCheckBoxMenuItem("Duration");
        duration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m.setDuration(duration.getState());
            }
        });
        view.add(duration);

        choose2d.setSelected(true);

        JMenuItem clear = new JMenuItem("Clear");
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m.clearFaces(m.getSrcdst());
            }
        });
        actions.add(clear);

        JMenuItem interp = new JMenuItem("Interpolate");
        interp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    try (PrintWriter pw = new PrintWriter("/tmp/src")) {
                        System.out.println("Foo:\n" + m.createFaceNl(0));
                        pw.print(m.createFaceNl(0));
                        pw.close();
                    }
                    try (PrintWriter pw = new PrintWriter("/tmp/dst")) {
                        pw.print(m.createFaceNl(1));
                        pw.close();
                    }
                    System.out.println("Starting rip");
                    Process p = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c",
                        "~/librip/example/rip /tmp/src \"1970-01-01 01:00\" /tmp/dst \"1970-01-01 01:16:40\" Wood > /tmp/interp 2>/dev/null"});
                    System.out.println("Waiting for rip termination");
                    p.waitFor();
                    System.out.println("Done waiting, importing result...");
                    m.importRegionFile("/tmp/interp", 2);
                    System.out.println("All done...");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        actions.add(interp);

        JMenuItem interp2 = new JMenuItem("Interpolate2");
        interp2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m.interp2();
            }
        });
        actions.add(interp2);
        
        final Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                int step = 50000;
                if (cmd.equals(">")) {
                    step = 50000;
                } else if (cmd.equals("<")) {
                    step = -50000;
                }
                
                JSlider j = m.getSlider();
                int val = j.getValue();
                val += step;
                if (val > j.getMaximum()) {
                    val = j.getMaximum();
                    ((Timer)e.getSource()).stop();
                } else if (val < j.getMinimum()) {
                    val = j.getMinimum();
                    ((Timer)e.getSource()).stop();
                }
                j.setValue(val);
            }
        });
        JMenuItem animate_fwd = new JMenuItem("Animate >");
        animate_fwd.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.setActionCommand(">");
                timer.setDelay(10);
                timer.setInitialDelay(1000);
                timer.start();
            }
        });
        actions.add(animate_fwd);

        XJMenu data = new XJMenu("Data");
        menuBar.add(data);

        JMenuItem export = new JMenuItem("Export");
        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (m.getSrcdst() > 1) {
                    return;
                }
                JFileChooser jfc = new JFileChooser();
                if (lastPath == null) {
                    lastPath = System.getProperty("user.dir");
                }
                if (lastPath != null) {
                    jfc.setCurrentDirectory(new File(lastPath));
                }
                int ret = jfc.showOpenDialog(MRViewWindow.this);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String filename = jfc.getSelectedFile().getAbsolutePath();
                    lastPath = jfc.getSelectedFile().getParent();
                    File save = new File(filename);
                    if (!save.canWrite() && !save.getParentFile().canWrite()) {
                        JOptionPane.showMessageDialog(MRViewWindow.this, "Write permission denied");
                    }
                    if (save.exists()) {
                        ret = JOptionPane.showConfirmDialog(MRViewWindow.this,
                                "File '" + jfc.getSelectedFile().getName()
                                + "' already exists! Overwrite?",
                                "File exists",
                                JOptionPane.YES_NO_OPTION);
                        if (ret != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                    try (PrintWriter pw = new PrintWriter(save.getAbsoluteFile())) {
                        pw.print(m.createFaceNl(m.getSrcdst()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(MRViewWindow.this, "Write to " + save.getAbsoluteFile().getAbsolutePath() + " failed: " + ex.getMessage());
                    }
                }
            }
        });
        data.add(export);

        JMenuItem _import = new JMenuItem("Import");
        _import.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                if (lastPath == null) {
                    lastPath = System.getProperty("user.dir");
                }
                if (lastPath != null) {
                    jfc.setCurrentDirectory(new File(lastPath));
                }
                int ret = jfc.showOpenDialog(MRViewWindow.this);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String filename = jfc.getSelectedFile().getAbsolutePath();
                    lastPath = jfc.getSelectedFile().getParent();
                    File load = new File(filename);
                    if (!load.canRead()) {
                        JOptionPane.showMessageDialog(MRViewWindow.this, "Read permission denied");
                    }
                    m.importRegionFile(filename);
                }
            }
        });
        data.add(_import);

        JMenuItem snapshot = new JMenuItem("Snapshot");
        snapshot.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                if (lastPath == null) {
                    lastPath = System.getProperty("user.dir");
                }
                if (lastPath != null) {
                    jfc.setCurrentDirectory(new File(lastPath));
                }
                String imgdir = System.getenv("IMGDIR");
                if (imgdir != null) {
                    jfc.setCurrentDirectory(new File(imgdir));
                }
                int ret = jfc.showSaveDialog(MRViewWindow.this);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String filename = jfc.getSelectedFile().getAbsolutePath();
                    lastPath = jfc.getSelectedFile().getParent();
                    File save = new File(filename);
                    if (!save.canWrite() && !save.getParentFile().canWrite()) {
                        JOptionPane.showMessageDialog(MRViewWindow.this, "Write permission denied");
                    }
                    if (save.exists()) {
                        ret = JOptionPane.showConfirmDialog(MRViewWindow.this,
                                "File '" + jfc.getSelectedFile().getName()
                                + "' already exists! Overwrite?",
                                "File exists",
                                JOptionPane.YES_NO_OPTION);
                        if (ret != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                    try (FileOutputStream fos = new FileOutputStream(save.getAbsoluteFile())) {
                        mvc.writeSnapshot(fos);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(MRViewWindow.this, "Write to " + save.getAbsoluteFile().getAbsolutePath() + " failed: " + ex.getMessage());
                    }
                }
            }

        });
        data.add(snapshot);

        JMenuItem snapshotseries = new JMenuItem("Snapshot series");
        snapshotseries.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                if (lastPath == null) {
                    lastPath = System.getProperty("user.dir");
                }
                if (lastPath != null) {
                    jfc.setCurrentDirectory(new File(lastPath));
                }
                String imgdir = System.getenv("IMGDIR");
                if (imgdir != null) {
                    jfc.setCurrentDirectory(new File(imgdir));
                }
                int ret = jfc.showSaveDialog(MRViewWindow.this);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String filename = jfc.getSelectedFile().getAbsolutePath();
                    lastPath = jfc.getSelectedFile().getParent();
                    int nrseries = 5;
                    nrseries--;
                    for (int i = 0; i <= nrseries; i++) {
                        File save = new File(filename+Integer.toString(i+1)+".png");
                        if (!save.canWrite() && !save.getParentFile().canWrite()) {
                            JOptionPane.showMessageDialog(MRViewWindow.this, "Write permission denied");
                        }
                        try (FileOutputStream fos = new FileOutputStream(save.getAbsoluteFile())) {
													  double t = i/nrseries;
														if (t == 0) t = 0.000001;
														if (t == 1) t = 0.999999;
                            mvc.writeSnapshot(fos, t);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(MRViewWindow.this, "Write to " + save.getAbsoluteFile().getAbsolutePath() + " failed: " + ex.getMessage());
                        }
                    }
                }
            }

        });
        data.add(snapshotseries);

        XJMenu create = new XJMenu("Create");
        JMenuItem mpoint = new JMenuItem("MPoint");
        mpoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ObjectCreator<MovingObject> oc = new MPointCreator();
                m.setCreator(oc);
                oc.checkAction();
            }
        });
        create.add(mpoint);

        JMenuItem fmface = new JMenuItem("FMFace");
        fmface.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ObjectCreator<MovingObject> oc = new FMFaceCreator();
                m.setCreator(oc);
                oc.checkAction();
            }
        });
//        create.add(fmface);

        JMenuItem face = new JMenuItem("Face");
        face.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ObjectCreator<Face> oc = new FaceCreator();
                m.setCreator(oc);
                oc.checkAction();
            }
        });
//        create.add(face);

        JMenuItem rc = new JMenuItem("Region");
        rc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ObjectCreator<Region> oc = new RegionCreator();
                m.setCreator(oc);
                oc.checkAction();
            }
        });
        create.add(rc);

        menuBar.add(create);

        XJMenu operations = new XJMenu("Operations");
        JMenuItem mpointInsideFmr = new JMenuItem("MPoint inside FMR");
        mpointInsideFmr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MFace mf = (MFace) m.getFirstObject(MFace.class);
                MPointOld mp = (MPointOld) m.getFirstObject(MPointOld.class);
                if (mp == null) {
                    MPoint mp2 = (MPoint) m.getFirstObject(MPoint.class);
                    if (mp2 != null)
                        mp = mp2.getUnits().get(0).getMPointOld();
                }
                if (mf == null) {
                    FMRegion fmr = (FMRegion) m.getFirstObject(FMRegion.class);
                    mf = fmr.getMFace(0);
                }
                Util.inside(mf, mp);
            }
        });
        operations.add(mpointInsideFmr);

        JMenuItem restorePerspective = new JMenuItem("Restore perspective");
        restorePerspective.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MFace mf = (MFace) m.getFirstObject(MFace.class);
                MPointOld mp = (MPointOld) m.getFirstObject(MPointOld.class);

                Util.restorePerspective(mf, mp);
            }

        });
        operations.add(restorePerspective);

        JMenuItem trochoids = new JMenuItem("Trochoids");
        trochoids.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m.trochoids();
                m.cartesiantrochoids();
            }

        });
        operations.add(trochoids);

        JMenuItem relocateFace = new JMenuItem("Relocate Face");
        relocateFace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m.relocateFace();
            }

        });
        operations.add(relocateFace);

        menuBar.add(operations);

        final XJMenu objects = new XJMenu("Objects");
        objects.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                objects.removeAll();
                int nrobjs = 0;
                for (final MovingObject o : m.getMfaces()) {
                    XJMenu j = new XJMenu(o.getClass().getSimpleName());

                    JCheckBoxMenuItem visible = new JCheckBoxMenuItem("Visible");
                    visible.setState(o.isVisible());
                    visible.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            o.setVisible(!o.isVisible());
                            m.modelChanged();
                        }
                    });
                    j.add(visible);
                    
                    JCheckBoxMenuItem red = new JCheckBoxMenuItem("Red");
                    red.setState(o.isRed());
                    red.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            o.setRed(!o.isRed());
                            m.modelChanged();
                        }
                    });
                    j.add(red);

                    JMenuItem remove = new JMenuItem("Remove");
                    remove.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            m.removeMFace(o);
                        }
                    });
                    j.add(remove);

                    if (o instanceof SecondoObject) {
                        JMenuItem save = new JMenuItem("Save");
                        save.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String filename = fileChooser(true);
                                if (filename != null) {
                                    ImportExport.exportObject(filename, (SecondoObject) o);
                                }
                            }
                        });
                        j.add(save);

                        JMenuItem showDef = new JMenuItem("Show definition");
                        showDef.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                JTextArea jt = new JTextArea(25, 80);
                                jt.setText(ImportExport.exportObject((SecondoObject) o));
                                JFrame jf = new JFrame();
                                jf.add(new JScrollPane(jt));
                                jf.setSize(1024, 768);
                                jf.show();

//                                JOptionPane.showMessageDialog(MRViewWindow.this, new JScrollPane(jt));
                            }
                        });
                        j.add(showDef);
                    }

                    j.addSeparator();

                    for (final String op : o.getOperations()) {
                        JMenuItem i = new JMenuItem(op);
                        i.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                o.invokeOperation(op);
                            }
                        });
                        j.add(i);
                    }

                    if (o instanceof Region) {
                        XJMenu i = new XJMenu("interpolate");
                        for (final MovingObject o2 : m.getMfaces()) {
                            if (o == o2 || !(o2 instanceof Region)) {
                                continue;
                            }
                            JMenuItem p = new JMenuItem(o2.getObjName());
                            p.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    o2.setHighlighted(false);
                                    String params = JOptionPane.showInputDialog("Interpolation parameters", "Distance");
                                    MRegion mr = Util.interpolate((Region) o, (Region) o2, params);
                                    m.addMFace(mr);
                                }
                            });
                            p.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    o2.setHighlighted(true);
                                    m.modelChanged();
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    o2.setHighlighted(false);
                                    m.modelChanged();
                                }

                            });
                            i.add(p);
                        }
                        j.add(i);
                    }

                    if (o instanceof Region) {
                        XJMenu i = new XJMenu("Rotatingplane");
                        for (final MovingObject o2 : m.getMfaces()) {
                            if (o == o2 || !(o2 instanceof Region)) {
                                continue;
                            }
                            JMenuItem p = new JMenuItem(o2.getObjName());
                            p.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    o2.setHighlighted(false);
//                                    MRegion mr = new MRegion(RotatingPlane.interpolate((Region)o, (Region)o2));
                                    MRegion mr = RotatingPlane.interpolate((Region) o, (Region) o2);
                                    m.addMFace(mr);
                                }
                            });
                            p.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    o2.setHighlighted(true);
                                    m.modelChanged();
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    o2.setHighlighted(false);
                                    m.modelChanged();
                                }

                            });
                            i.add(p);
                        }
                        j.add(i);
                    }

                    if (o instanceof Region) {
                        XJMenu i = createJMenu("Union", o, Region.class, new MOAction() {
                            @Override
                            public MovingObject action(MovingObject m1, MovingObject m2) {
                                Region r1 = (Region) m1;
                                Region r2 = (Region) m2;

                                return r1.unionArea(r2);
                            }
                        });
                        j.add(i);
                    }

                    j.addSeparator();
                    for (final String fl : o.getFlags()) {
                        JCheckBoxMenuItem i = new JCheckBoxMenuItem(fl);
                        i.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Boolean cur = o.getFlag(fl);
                                o.setFlag(fl, !cur);
                            }
                        });
                        i.setState(o.getFlag(fl));
                        j.add(i);
                    }
                    j.addMenuListener(new MenuListener() {
                        @Override
                        public void menuSelected(MenuEvent me) {
                            o.setHighlighted(true);
                            m.modelChanged();
                        }

                        @Override
                        public void menuDeselected(MenuEvent me) {
                            o.setHighlighted(false);
                            m.modelChanged();
                        }

                        @Override
                        public void menuCanceled(MenuEvent me) {
                            o.setHighlighted(false);
                            m.modelChanged();
                        }

                    });
                    objects.add(j);
                    nrobjs++;
                }
                if (nrobjs == 0) {
                    JMenuItem no = new JMenuItem("<no objects>");
                    no.setEnabled(false);
                    objects.add(no);
                }
                JMenuItem load = new JMenuItem("Load...");
                load.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String filename = fileChooser(false);
                        if (filename != null) {
                            SecondoObject o = ImportExport.importObject(filename);
                            if (o instanceof MovingObject) {
                                m.addMFace((MovingObject) o);
                            }
                        }
                    }
                });
                objects.add(load);

            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }

        });

        menuBar.add(objects);

        mvc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                m.addPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (MRGraphics.last == null) {
                    return;
                }
                m.removeDragSegments();
            }
        });

        this.setJMenuBar(menuBar);

        JLabel north = new JLabel();
        this.add(north, BorderLayout.NORTH);
        m.setStatusLabel(north);

//        this.add(mvc3d, BorderLayout.CENTER);
//        if (mvc3d != null)
//            this.add(mvc3d, BorderLayout.CENTER);
//        else
            this.add(mvc, BorderLayout.CENTER);

//        List<MovingObject> mos = m.getMfaces();
//        Region r1 = (Region) mos.get(0);
//        Region r2 = (Region) mos.get(1);

        /*        try {
         Face f1 = r1.getFaces().get(0);
         Face f2 = r1.getFaces().get(1);
         MergeInfo mi = f1.nearest(f2);
         Face f = mi.doMerge();
         Region r2 = new Region();
         r2.addFace(f);
         m.addMFace(r2);
         } catch (Exception e) {
         e.printStackTrace();
         }
         */
        this.setVisible(true);
    }

    private String fileChooser(boolean save) {
        String filename = null;
        JFileChooser jfc = new JFileChooser();
        if (lastPath == null) {
            lastPath = System.getProperty("user.dir");
        }
        if (lastPath != null) {
            jfc.setCurrentDirectory(new File(lastPath));
        }
        int ret = save ? jfc.showSaveDialog(MRViewWindow.this) : jfc.showOpenDialog(MRViewWindow.this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            filename = jfc.getSelectedFile().getAbsolutePath();
            lastPath = jfc.getSelectedFile().getParent();
        }

        return filename;
    }

    private XJMenu createJMenu(String name, final MovingObject o, Class matches, final MOAction action) {
        XJMenu i = new XJMenu(name);
        for (final MovingObject o2 : m.getMfaces()) {
            if (o == o2 || !(matches.isInstance(o2))) {
                continue;
            }
            JMenuItem p = new JMenuItem(o2.getObjName());
            p.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    MovingObject ret = action.action(o, o2);
                    if (ret != null) {
                        m.addMFace(ret);
                    }
                }
            });
            p.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    o2.setHighlighted(true);
                    m.modelChanged();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    o2.setHighlighted(false);
                    m.modelChanged();
                }

            });
            i.add(p);
        }
        return i;
    }
}
