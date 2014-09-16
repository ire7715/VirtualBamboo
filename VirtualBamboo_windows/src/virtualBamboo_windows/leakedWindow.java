package virtualBamboo_windows;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class leakedWindow extends JFrame {
	
	/**
	 *  auto generated serialVersionUID
	 */
	private static final long serialVersionUID = -6074135364468331042L;
	
	public int BOTTOM_LINE_HEIGHT = 50;
	public final static int RESIZE_PANEL_WIDTH = 32;
	public final static int HOLLOW_BORDER = 1;
	public final static Color BORDER_COLOR = Color.RED;
	private Dimension screenSize, boardSize;
	protected int orientation = 0;
	private Point hollowOrigin = new Point(0, 0);
	
	/*** BEGIN Components ***/
	protected JLabel tipLabel = null;
	protected JCheckBox connectionCheck = null;
	/*** END Component ***/
	
	/**
	 * set the text of tip label.
	 * @param s: the string given to set tip label.
	 */
	public void setTipText(String s){
		tipLabel.setText(s);
	}
	
	/**
	 * rotate the orientation.
	 * @return successful or not.
	 */
	public boolean rotateScreen(){
		Dimension size = new Dimension(this.screenSize.height, this.screenSize.width);
		return this.setScreenSize(size);
	}
	
	/**
	 * set the size of device.
	 * @param size
	 * @return successful or not.
	 */
	public boolean setScreenSize(Dimension size){
		this.screenSize = size;
		OnScreenSizeChanged();
		return true;
	}
	
	public Dimension getScreenSize(){
		return this.screenSize;
	}
	
	private void OnScreenSizeChanged(){
		// TODO maybe check the orientation and set the proper width or height, remove the size adjusting in OnBoardSizeChnaged 
		/*this.setLocation(new Point(0, 0));
		Dimension screenResol = Toolkit.getDefaultToolkit().getScreenSize();
		BigInteger width = new BigInteger(""+this.screenSize.width);
		int gcd = width.gcd(new BigInteger(""+this.screenSize.height)).intValue();
		Dimension leastSize = new Dimension(this.screenSize.width / gcd, this.screenSize.height / gcd);
		if(leastSize.width > screenResol.width || leastSize.height > screenResol.height){
			if(leastSize.width > leastSize.height){
				
			}else{
				
			}
		}else{
			this.setBoardSize(new Dimension((int)this.screenSize.getWidth(), (int)this.screenSize.getHeight()));
		}*/
		this.setBoardSize(new Dimension((int)this.screenSize.getWidth(), (int)this.screenSize.getHeight()));
	}
	
	/**
	 * set the hollow size to the given rate.
	 * @param rate 
	 * @return successfulness
	 */
	public boolean setBoardSize(double rate){
		this.boardSize = new Dimension((int)(this.screenSize.getWidth()*rate), (int)(this.screenSize.getHeight()*rate));
		this.OnBoardSizeChanged();
		return true;
	}
	
	/**
	 * set the hollow size to a specific size.
	 * @param size
	 * @return successfulness
	 */
	public boolean setBoardSize(Dimension size){
		this.boardSize = size;
		this.OnBoardSizeChanged();
		return true;
	}
	
	public Dimension getBoardSize(){
		return this.boardSize;
	}
	
	private void OnBoardSizeChanged(){
		//this.BOTTOM_LINE_HEIGHT = (this.boardSize.getHeight()/4 > 40)?((int)this.boardSize.getHeight()/4):(40);
		Dimension od = new Dimension((int)this.boardSize.getWidth() + leakedWindow.HOLLOW_BORDER*2, (int)this.boardSize.getHeight() + leakedWindow.HOLLOW_BORDER*2 + this.BOTTOM_LINE_HEIGHT);
		this.setSize(od);
		
		if(this.isShowing()){
			Dimension screenResol = Toolkit.getDefaultToolkit().getScreenSize();
			Point diagonal = new Point((int)(this.getLocationOnScreen().x + od.getWidth()), (int)(this.getLocationOnScreen().y + od.getHeight()));
			/*if(diagonal.getY() > screenResol.getHeight() && (diagonal.getY() - screenResol.getHeight()) > (diagonal.getX() - screenResol.getWidth())){
				this.setBoardSize((screenResol.getHeight() - this.getLocationOnScreen().getX() - leakedWindow.HOLLOW_BORDER*2 - this.BOTTOM_LINE_HEIGHT)/getScreenSize().getHeight());
			}else if(diagonal.getX() > screenResol.getWidth() && (diagonal.getY() - screenResol.getHeight()) < (diagonal.getX() - screenResol.getWidth())){
				this.setBoardSize((screenResol.getWidth() - this.getLocationOnScreen().getY() - leakedWindow.HOLLOW_BORDER*2)/getScreenSize().getHeight());
			}*/
			if(diagonal.getY() > screenResol.getHeight() || diagonal.getX() > screenResol.getWidth()){
				this.setBoardSize(new Dimension((int)(this.boardSize.width * .8), (int)(this.boardSize.height * .8)));
			}
		}
	}
	
	/**
	 * close the window and release resources.
	 */
	public void close(){
		dispose();
		System.exit(0);
	}
	
	/**
	 * set the hollow position in window.
	 * @param pos
	 */
	public void setHollowPosition(Point pos){
		this.hollowOrigin = pos;
		this.OnHollowMoved();
	}
	
	private void OnHollowMoved(){
		this.repaint();
	}
	
	/**
	 * get hollow rectangle relate to window.
	 * @return
	 */
	public Rectangle getHollow(){
		return new Rectangle(this.hollowOrigin.x + leakedWindow.HOLLOW_BORDER, this.hollowOrigin.y + leakedWindow.HOLLOW_BORDER, (int)this.getBoardSize().getWidth(), (int)this.getBoardSize().getHeight());
	}
	
	/**
	 * getHollow relate to screen version.
	 * @return  
	 */
	public Rectangle getHollowOnScreen(){
		Rectangle hollow = this.getHollow();
		hollow.x += this.getLocation().x;
		hollow.y += this.getLocation().y;
		return hollow;
	}
	
	@Override
	public void setVisible(boolean visibility){
		this.OnLoad();
		
		super.setVisible(visibility);
	}
	
	protected void OnLoad(){
		this.layoutWork();
	}
	
	/**
	 * deploy the window layout with GridBagLayout. (Actually, I'm not familiar with GridLayout before. It takes me quite long time.)
	 */
	private void layoutWork(){
        
        GridBagConstraints layoutCon = new GridBagConstraints();
        layoutCon.gridy = 0;
        layoutCon.gridx = 0;
        layoutCon.gridwidth = 3;
        layoutCon.gridheight = 1;
        layoutCon.weightx = layoutCon.weighty = 1;
		layoutCon.anchor = GridBagConstraints.LAST_LINE_START;
		layoutCon.fill = GridBagConstraints.HORIZONTAL;
		this.getContentPane().add(new JLabel(""), layoutCon);
		
		this.connectionCheck.setBorder(new EmptyBorder(2, 5, 3, 5));
		layoutCon = new GridBagConstraints();
		layoutCon.anchor = GridBagConstraints.FIRST_LINE_START;
		layoutCon.fill = GridBagConstraints.BOTH;
		layoutCon.gridx = 0;
        layoutCon.gridy = 1;
        layoutCon.gridwidth = layoutCon.gridheight = 1;
        layoutCon.weightx = layoutCon.weighty = 0;
		this.getContentPane().add(connectionCheck, layoutCon);
		
		this.tipLabel.setBorder(new EmptyBorder(2, 5, 3, 5));
		layoutCon = new GridBagConstraints();
		layoutCon.anchor = GridBagConstraints.FIRST_LINE_START;
		layoutCon.fill = GridBagConstraints.BOTH;
		layoutCon.gridx = 0;
        layoutCon.gridy = 2;
        layoutCon.gridwidth = layoutCon.gridheight = 1; 
        layoutCon.weightx = layoutCon.weighty = 0;
		this.getContentPane().add(tipLabel, layoutCon);
		
	}
	
	public leakedWindow(String title){
		super(title);
		this.setUndecorated(true);
		this.setAlwaysOnTop(true);
		this.setContentPane(new ContentPane(this));
		this.setScreenSize(new Dimension(300,200));
		//this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(new GridBagLayout());
		
		LWMouseAdapter listener = new LWMouseAdapter();
        this.addMouseMotionListener(listener);
        this.addMouseListener(listener);

		this.connectionCheck = new JCheckBox("Start connection");

		this.tipLabel = new JLabel("tip");
	}
	
	/**
	 * The mouse works.
	 * Drag the bottom-right corner for resizing.
	 * Drag the bottom space to move window.
	 * Double-click the bottom-right corner to close the window.
	 * @author Ire
	 *
	 */
	public class LWMouseAdapter extends MouseAdapter{
		int status = 0;
		Point offset = null;
		Point pressedLocation = null;
		protected final static int RESIZE_BLOCK = 1;
		protected final static int DRAG_BLOCK = 2;
		
		@Override
        public void mousePressed(MouseEvent e) {
            this.status = sectionTeller(e.getPoint());
            switch(this.status){
            case LWMouseAdapter.RESIZE_BLOCK:
            	this.pressedLocation = new Point(e.getX(), e.getY());
            	this.offset = new Point(getWidth() - e.getX(), getHeight() - e.getY());
            	break;
            case LWMouseAdapter.DRAG_BLOCK:
            	this.offset = this.pressedLocation = new Point(e.getX(), e.getY());
            	break;
            default:
            	break;
            }
        }

		/**
		 * control the action of dragging window and resizing.
		 */
        @Override
        public void mouseDragged(MouseEvent e) {
            switch(this.status){
            case LWMouseAdapter.RESIZE_BLOCK:
            	double rate = 1.0;
            	/*if(Math.abs(this.pressedLocation.x - e.getX()) > Math.abs(this.pressedLocation.y - e.getY())){
            		rate = (e.getXOnScreen() - getLocation().x)/getScreenSize().getWidth();
            	}else{
            		rate = (e.getYOnScreen() - getLocation().y)/getScreenSize().getHeight();
            	}*/
            	// I want to adjust by diagonal originally. But at the edge of zoom-in and zoom-out, it shows some unnatural act.
            	// Only depends on x for temporary replacement. 
            	rate = (e.getXOnScreen() - getLocation().x)/getScreenSize().getWidth();
            	setBoardSize(rate);
            	break;
            case LWMouseAdapter.DRAG_BLOCK:
            	setLocation(e.getXOnScreen() - this.pressedLocation.x, e.getYOnScreen() - this.pressedLocation.y);
            	break;
            default:
            	break;
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e){
        	this.status = sectionTeller(e.getPoint());
        	if(e.getClickCount() >= 2){
        		switch(this.status){
        		case LWMouseAdapter.RESIZE_BLOCK:
        			close();
        			break;
        		default:
        			break;
        		}
        	}else{
        	}
        }
        
        /**
         * tell the cursor point is in resize block, in drag block or neither.
         * @param mp: mouse point from MouseEvent.
         * @return return the block code.
         */
        private int sectionTeller(Point mp){
        	if(getHeight() - mp.y < BOTTOM_LINE_HEIGHT){
        		if(getWidth() - mp.x < leakedWindow.RESIZE_PANEL_WIDTH)
        			return LWMouseAdapter.RESIZE_BLOCK;
        		else
        			return LWMouseAdapter.DRAG_BLOCK;
        	}
        	return 0;
        }
	}
	
	public class ContentPane extends JPanel {
		/**
		 *  auto generated serialVersionUID
		 */
		private static final long serialVersionUID = -8735824267370280026L;
		
		leakedWindow thisWindow = null;

	    public ContentPane(leakedWindow window) {

	    	this.setOpaque(false);
	        this.thisWindow = window;

	    }

	    /**
	     * the hole dig job in here.
	     */
	    @Override
	    protected void paintComponent(Graphics g) {

	        // Allow super to paint
	        super.paintComponent(g);

	        g.setColor(leakedWindow.BORDER_COLOR);
	        g.drawRect(0, 0, (int)this.thisWindow.getBoardSize().getWidth() + leakedWindow.HOLLOW_BORDER, (int)this.thisWindow.getBoardSize().getHeight() + leakedWindow.HOLLOW_BORDER);
	        
	        Rectangle hollowV = this.thisWindow.getHollow();
	        Shape hollow = new Rectangle2D.Double(hollowV.getX(), hollowV.getY(), hollowV.getWidth(), hollowV.getHeight());	// refer to http://docstore.mik.ua/orelly/java-ent/jfc/ch04_04.htm , inspired by https://www.java.net/node/700779
	        Shape whole = new Rectangle2D.Double(0, 0, this.getWidth(), this.getHeight());
	        Area diff = new Area(whole);
	        diff.subtract(new Area(hollow));
	        this.thisWindow.setShape(diff);

	    }
	}
}
