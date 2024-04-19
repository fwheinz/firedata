package mrview;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.*;
import java.beans.*;

import java.util.*;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.*;
import javax.accessibility.*;

class XJMenu extends JMenu {
	public XJMenu(String str) {
		super(str);
	}

	@Override
	protected Point getPopupMenuOrigin() {
		int x = 0;
		int y = 0;
		JPopupMenu pm = getPopupMenu();
		// Figure out the sizes needed to caclulate the menu position
		Dimension s = getSize();
		Dimension pmSize = pm.getSize();
		// For the first time the menu is popped up,
		// the size has not yet been initiated
		if (pmSize.width==0) {
			pmSize = pm.getPreferredSize();
		}
		Point position = getLocationOnScreen();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		GraphicsConfiguration gc = getGraphicsConfiguration();
		Rectangle screenBounds = new Rectangle(toolkit.getScreenSize());
		GraphicsEnvironment ge =
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		for(int i = 0; i < gd.length; i++) {
			if(gd[i].getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				GraphicsConfiguration dgc =
					gd[i].getDefaultConfiguration();
				if(dgc.getBounds().contains(position)) {
					gc = dgc;
					break;
				}
			}
		}


		if (gc != null) {
			screenBounds = gc.getBounds();
			// take screen insets (e.g. taskbar) into account
		//	Insets screenInsets = toolkit.getScreenInsets(gc);
			Insets screenInsets = new Insets(0, 0, 0, 0);

			screenBounds.width -=
				Math.abs(screenInsets.left + screenInsets.right);
			screenBounds.height -=
				Math.abs(screenInsets.top + screenInsets.bottom);
			position.x -= Math.abs(screenInsets.left);
			position.y -= Math.abs(screenInsets.top);
		}

		Container parent = getParent();
		if (parent instanceof JPopupMenu) {
			// We are a submenu (pull-right)
			int xOffset = UIManager.getInt("Menu.submenuPopupOffsetX");
			int yOffset = UIManager.getInt("Menu.submenuPopupOffsetY");

			// First determine x:
			x = s.width + xOffset;   // Prefer placement to the right
			if (position.x + x + pmSize.width >= screenBounds.width
					+ screenBounds.x &&
					// popup doesn't fit - place it wherever there's more room
					screenBounds.width - s.width < 2*(position.x
						- screenBounds.x)) {

				x = 0 - xOffset - pmSize.width;
			}
			// Then the y:
			y = yOffset;                     // Prefer dropping down
			if (position.y + y + pmSize.height >= screenBounds.height
					+ screenBounds.y &&
					// popup doesn't fit - place it wherever there's more room
					screenBounds.height - s.height < 2*(position.y
						- screenBounds.y)) {

				y = s.height - yOffset - pmSize.height;
			}
		} else {
			// We are a toplevel menu (pull-down)
			int xOffset = UIManager.getInt("Menu.menuPopupOffsetX");
			int yOffset = UIManager.getInt("Menu.menuPopupOffsetY");

			// First determine the x:
			x = xOffset;                   // Extend to the right
			if (position.x + x + pmSize.width >= screenBounds.width
					+ screenBounds.x &&
					// popup doesn't fit - place it wherever there's more room
					screenBounds.width - s.width < 2*(position.x
						- screenBounds.x)) {

				x = s.width - xOffset - pmSize.width;
			}
			// Then the y:
			y = s.height + yOffset;    // Prefer dropping down
			if (position.y + y + pmSize.height >= screenBounds.height &&
					// popup doesn't fit - place it wherever there's more room
					screenBounds.height - s.height < 2*(position.y
						- screenBounds.y)) {

				y = 0 - yOffset - pmSize.height;   // Otherwise drop 'up'
			}
		}
		return new Point(x,y);
	}
}
