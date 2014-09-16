package virtualBamboo_windows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.coobird.thumbnailator.Thumbnails;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class VirtualBamboo_windows {
	public static JFrame window = null;

	public static void main(String[] args){
		window = new virtualBamboo();
		window.setVisible(true);
	}


}

class mailMan{
	/*** BEGIN cursor variable ***/
	protected volatile Thread cursor = null;
	protected Socket cursorClient = null;
	/*** END cursor variable ***/
	
	/*** BEGIN animation variable ***/
	protected leakedWindow panel = null;
	protected volatile Thread animation = null;
	protected Socket animationClient = null;
	/*** END animation variable ***/
	
	/*** BEGIN constants ***/
	protected final static String TAG = "usb_pc_side.communicater";
	/*** END constants ***/
	
	protected String jarLocation = null;
	
	class animationThread extends Thread{ 
		public static final int LANDSCAPE_IMAGE = 1;
		public static final int PORTRAIT_IMAGE = 2;
		public static final int META_REQ_ING = 3;
		public static final int ROTATE_REQ_ED = 4;
		
		/**
		 * Requests meta-data first, then keeps sending hollow image and read the orientation of device.
		 */
		@Override
		public void run() {
			Thread thisThread = Thread.currentThread();
			ObjectInputStream fromDevice;
			OutputStream toDevice;
			try{
				panel.setTipText("Asking to establish a connection...");
				animationClient = new Socket("localhost", 33800);
				toDevice = animationClient.getOutputStream();
				toDevice.write(animationThread.META_REQ_ING);
				toDevice.flush();
				
				panel.setTipText("Connection established, aquiring mobile information...");
				fromDevice = new ObjectInputStream(animationClient.getInputStream());
				Dimension meta = new Dimension();
				while((panel.orientation = fromDevice.read()) == -1);
				if(panel.orientation == animationThread.PORTRAIT_IMAGE){
					meta.height = fromDevice.readInt();
					meta.width = fromDevice.readInt();
				}else if(panel.orientation == animationThread.LANDSCAPE_IMAGE){
					meta.width = fromDevice.readInt();
					meta.height = fromDevice.readInt();
				}
				panel.setScreenSize(meta);
				panel.setTipText("Screen size set-up.");
				
				toDevice.close();
				toDevice = null;
				fromDevice.close();
				fromDevice = null;
				animationClient.close();
				animationClient = null;
			}catch(IOException e){
				e.printStackTrace();
			}
			
			while(thisThread == animation){
				try {
					BufferedImage capture = captureScreen();
					if(capture != null){
						animationClient = new Socket("localhost", 33800);
						toDevice = animationClient.getOutputStream();
						toDevice.write(animationThread.LANDSCAPE_IMAGE);
						ImageIO.write(capture,  "jpg", toDevice);
						toDevice.flush();
						
						fromDevice = new ObjectInputStream(animationClient.getInputStream());
						int req = fromDevice.read();
						if((req == animationThread.LANDSCAPE_IMAGE || req == animationThread.PORTRAIT_IMAGE) && req != panel.orientation){
							panel.rotateScreen();
							panel.orientation = req;
						}
						
						fromDevice.close();
						fromDevice = null;
						toDevice.close();
						toDevice = null;
						animationClient.close();
						animationClient = null;
					}else
						System.err.println("screen-shot was null.");
					
					Thread.sleep(20);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e){
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * Capture the image below the hollow, orientation depends on panel.oientation. 
		 * Auto-size adjustment to device size, to decrease the device loading.
		 * @return the image in hollow. If some exception happened, return null.
		 */
		protected BufferedImage captureScreen(){
			try {
				BufferedImage result =  new Robot().createScreenCapture(panel.getHollowOnScreen());
				if(panel.orientation == animationThread.LANDSCAPE_IMAGE){
					
					// since thumbnails.rotate causes color distorted, I use the classic way.
					// refer to https://stackoverflow.com/questions/9749121/java-image-rotation-with-affinetransform-outputs-black-image-but-works-well-whe
					AffineTransform at = new AffineTransform();
					at.translate(result.getHeight() / 2, result.getWidth() / 2);
					at.rotate(Math.PI / 2);
					at.translate(-result.getWidth() / 2, -result.getHeight() / 2);
					AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
					BufferedImage dest = new BufferedImage((int)result.getHeight(), (int)result.getWidth(), BufferedImage.TYPE_INT_RGB);
					op.filter(result, dest);
					
					return Thumbnails.of(dest).size(panel.getScreenSize().height, panel.getScreenSize().width).asBufferedImage();
				}else{
					return Thumbnails.of(result).size(panel.getScreenSize().width, panel.getScreenSize().height).asBufferedImage();
				}
			} catch (AWTException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e){
				e.printStackTrace();
				return null;
			}
		}
		
		protected BufferedImage captureScreen(int scale){
			BufferedImage buff = captureScreen();
			BufferedImage result;
			try {
				result = Thumbnails.of(buff).size(buff.getWidth() / scale, buff.getHeight() / scale).asBufferedImage();
				
				return result;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	class cursorThread extends Thread{ 
		private final int ACTION_DOWN = 0x00;
		private final int ACTION_UP = 0x01;
		private final int ACTION_MOVE = 0x02;
		
		/**
		 * Keeps reading mouse order from device and perform action.
		 */
		@Override
		public void run() {
			Thread thisThread = Thread.currentThread();
			ObjectInputStream fromDevice = null;
			try {
				while(cursor == thisThread){
					// TODO fetch mouse action from queue.
					cursorClient = new Socket("localhost", 33801);
					fromDevice = new ObjectInputStream(cursorClient.getInputStream());
					int x, y, action;
					while((action = fromDevice.readInt()) != -1){
						x = fromDevice.readInt();
						y = fromDevice.readInt();
						this.performAction(action, x, y);
					}
					fromDevice.close();
					fromDevice = null;
					cursorClient.close();
					cursorClient = null;
					
					Thread.sleep(60);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e){
				System.out.println("cursor thread interrupted.");
			}
		}
		
		/**
		 * 
		 * @param action: action code, refer to MotionEvent from android
		 * @param x: x-coordinate on device, no need to transform. Transformation will be done by this function.
		 * @param y: y-coordinate on device. Same as last parameter description.
		 */
		protected void performAction(int action, int x, int y){
			Dimension scrSize = panel.getScreenSize(), brdSize = panel.getBoardSize();
			Point lefttopPnt = panel.getLocationOnScreen();
			
			//System.out.println("action: " + action + ", x: " + x + ", y: " + y);
			
			try {
				Robot bot = new Robot();
				if(panel.orientation == animationThread.PORTRAIT_IMAGE){
					bot.mouseMove(lefttopPnt.x + brdSize.width * x / scrSize.width, lefttopPnt.y + brdSize.height * y / scrSize.height);
				}else if(panel.orientation == animationThread.LANDSCAPE_IMAGE){
					bot.mouseMove(lefttopPnt.x + brdSize.width * y / scrSize.width, (int)(lefttopPnt.y + brdSize.height * (1.0 - (double)x / scrSize.height)));
				}
				switch(action){
				case ACTION_DOWN:
					bot.mousePress(InputEvent.BUTTON1_MASK);
					break;
				case ACTION_UP:
					bot.mouseRelease(InputEvent.BUTTON1_MASK);
					break;
				}
			} catch (AWTException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * An agent who transferring data between host and accessory.
	 * @param p: the panel with hollow.
	 */
	public mailMan(leakedWindow p){
		this.panel = p;
		this.jarLocation = virtualBamboo.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}
	
	/**
	 * same as new ProcessBuilder, only returns echo from that process.
	 * @param param: same as parameters of new ProcessBuilder
	 * @return echo from the process
	 * @throws IOException
	 */
	private String runProcess(String... param) throws IOException{
		Process adb = new ProcessBuilder(param).start();
		BufferedReader echoBuffer = new BufferedReader(new InputStreamReader(adb.getInputStream()));
		String echo = "", line;
		while((line = echoBuffer.readLine()) != null){
			echo += line + "\r\n";
		}
		return echo;
	}
	
	/**
	 * invoke adb.exe to open communication tunnel. Uses port 33800 and port 33801.
	 * @return
	 */
	private boolean openTunnel(){
		try {
			String echo = "";
			echo += runProcess(this.jarLocation + "adb.exe", "forward", "tcp:33800", "tcp:33800");
			echo += runProcess(this.jarLocation + "adb.exe", "forward", "tcp:33801", "tcp:33801");
			if(echo.length() != 0){
				JOptionPane.showMessageDialog(panel, echo + "\r\n -- Please restart connection. -- ", "adb echo", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean closeTunnel(){
		try{
			String echo = "";
			echo += runProcess(this.jarLocation + "adb.exe", "forward", "--remove-all");
			if(echo.length() != 0){
				JOptionPane.showMessageDialog(panel, echo + "\r\n -- Port binding not removed yet. -- ", "adb echo", JOptionPane.WARNING_MESSAGE);
				return false;
			}
		}catch(IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * start all thread to transfer data.
	 */
	public void start(){
		if(this.openTunnel()){
			this.animation = new animationThread();
			this.animation.start();
			this.cursor = new cursorThread();
			this.cursor.start();
		}
	}
	
	/**
	 * stop all data transferring works.
	 */
	public void stop(){
		this.animation = null;
		this.cursor = null;
	}
	
	protected void finalize(){
		this.stop();
		closeTunnel();
	}
}

class virtualBamboo extends leakedWindow{
	/**
	 *  auto generated serialVersionUID
	 */
	private static final long serialVersionUID = -6333643529029697871L;
	
	protected mailMan Rosa = null;
	
	public virtualBamboo(){
		super("Virtual Bamboo");
	}
	
	@Override
	protected void OnLoad(){
		super.OnLoad();
		
		Rosa = new mailMan(this);
		this.connectionCheck.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				if(((JCheckBox)e.getItem()).isSelected()){
					Rosa.start();
				}else{
					Rosa.stop();
				}
			}
			
			
		});
	}
	
	@Override
	public void close(){
		Rosa.stop();
		
		super.close();
	}
}