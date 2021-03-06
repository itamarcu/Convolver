import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

class Main extends JFrame implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, ComponentListener, WindowFocusListener
{
	// Constants
	final double				TAU						= 2 * Math.PI;
	final double				extraDistanceVertical	= 40;
	final double				extraDistanceHorizontal	= 10;

	// Variables
	boolean						leftMousePressed		= false;
	boolean						leftPressed, rightPressed, upPressed, downPressed;
	Point						camera;												// Marks the CENTER of the screen to be drawn, not the top
																					// left point

	double						timeThatPassed;

	List<Wave>					waves;
	List<Waver>					wavers;
	Player						player;
	List<Surfer>				enemySurfers;
	List<Tringler>				tringlers;
	double						eventTimeLeft			= 10, eventFrequency = 7;
	int							lastKeyPressed			= -1;
	double						timeSinceLastKeyPressed	= 999;
	boolean						dash					= false;
	double						dashTime				= -1;
	double						dashCooldown			= 1;

	// Stuff that should not be touched
	private static final long	serialVersionUID		= 1;
	int							frameHeight, frameWidth;
	PointerInfo					pin;												// Don't use this
	Point						incorrectMousePoint		= new Point();				// Don't use that
	int							mx						= 0;						// Mouse X coordinate relative to FRAME
	int							my						= 0;						// Mouse Y coordinate relative to FRAME
	int							bufferWidth;										// ignore
	int							bufferHeight;										// ignore
	Image						bufferImage;										// ignore, unless you want to do stuff with this
	Graphics					bufferGraphics;										// ignore
	boolean						test					= false;
	Color						bgColor					= new Color(50, 150, 255);

	// This is where the magic happens
	void gameFrame(double deltaTime)
	{
		timeThatPassed += deltaTime;
		synchronized (waves)
		{
			for (int i = 0; i < waves.size(); i++)
			{
				Wave w = waves.get(i);
				w.update(deltaTime);
				if (w.r1 > w.maxR1)
				{
					w.width -= 60 * deltaTime;
					w.r1 += 60 * deltaTime;
					if (w.width <= 0)
					{
						if (i == player.lastWaveIndex)
							player.lastWaveIndex = -1;
						if (i < player.lastWaveIndex)
							player.lastWaveIndex -= 1;
						for (Surfer s : enemySurfers)
						{
							if (i == s.lastWaveIndex)
								s.lastWaveIndex = -1;
							if (i < s.lastWaveIndex)
								s.lastWaveIndex -= 1;
						}
						waves.remove(i);
						i--;
						continue;
					}
				}
				if (test)
				{
					test = false;
					w.infest(Math.random() * TAU);
				}
				if (w.infested)
				{
					// System.out.println("123");
					w.intensify(deltaTime);
					for (int j = i + 1; j < waves.size(); j++)
					{
						Wave w2 = waves.get(j);
						if (!w2.infested)
						{
							double[] angles = w.intersectionAngles(w2);
							if (angles[0] != -1)
							{
								player.x = w.cx + w.r1 * Math.cos(angles[0]);
								player.y = w.cy + w.r1 * Math.sin(angles[0]);
								if (w.inInfectionRange(angles[0]))
									w2.infest(angles[0]);
								else if (w.inInfectionRange(angles[1]))
									w2.infest(angles[1]);
							}
						}
					}
				}
			}
		}

		if (dashTime <= 0)
		{
			double[] direction = moveByPlayerKeys();
			moveSurfer(player, direction, deltaTime);
			if (dash)
			{
				if (direction[1] != 0)
				{
					player.xVel = 200 * Math.cos(direction[0]);
					player.yVel = 200 * Math.sin(direction[0]);
					dashTime = 0.3;
					dash = false;
					player.shielded = true;
					player.lastWaveIndex = -1;
					player.ripple = 1;
					dashCooldown = 1;
				}
			}
		} else
		{
			double[] direction = moveByPlayerKeys();
			if (dashTime > 0.12)
			{

				player.xVel += deltaTime * 8 * direction[1] * Surfer.acceleration * Math.cos(direction[0]);
				player.yVel += deltaTime * 8 * direction[1] * Surfer.acceleration * Math.sin(direction[0]);
			}
			if (dashTime <= 0.1)
			{
				player.xVel -= deltaTime * player.xVel * 4;
				player.yVel -= deltaTime * player.yVel * 4;
			}
			if (player.ripple % 3 == 0)
				waves.add(new Wave(player.x, player.y, 70 - 2 * player.ripple, 6, Wave.purple, 30 - 1 * player.ripple));
			player.ripple++;
			dashTime -= deltaTime;
			player.x += deltaTime * player.xVel;
			player.y += deltaTime * player.yVel;
		}
		if (player.lastWaveIndex == -1)
			player.holdBreath(deltaTime);
		else if (player.underwaterTimer > 0)
			player.underwaterTimer -= deltaTime * 3;
		if (player.underwaterTimer < 0)
			player.underwaterTimer = 0;
		if (player.injureFlash < 1)
			player.injureFlash += deltaTime;
		timeSinceLastKeyPressed += deltaTime;
		if (dashCooldown > 0)
			dashCooldown -= deltaTime;
player.life.rotate(deltaTime);
		for (int i = 0; i < wavers.size(); i++)
		{
			Waver wr = wavers.get(i);
			wr.update(deltaTime);
			if (wr.timeLeft <= 0)
			{
				waves.add(wr.generateWave());
			}
		}
		synchronized (enemySurfers)
		{
			for (int i = 0; i < enemySurfers.size(); i++)
			{
				Surfer s = enemySurfers.get(i);
				if (Math.pow(s.x - player.x, 2) + Math.pow(s.y - player.y, 2) > 1200 * 1200)
				{
					enemySurfers.remove(i);
					i--;
					continue;
				}
				double extraradius = player.shielded ? 10 : 0;
				if (Math.pow(s.x - player.x, 2) + Math.pow(s.y - player.y, 2) < Math.pow(player.radius + s.radius + extraradius, 2))
				{
					// collision
					enemySurfers.remove(i);
					i--;
					if (!player.shielded)
						player.damage(10);
					continue;
				}
				moveSurfer(s, moveByFollowPlayer(s), deltaTime);
			}
		}
		synchronized (tringlers)
		{
			for (int i = 0; i < tringlers.size(); i++)
			{
				Tringler t = tringlers.get(i);
				if (Math.pow(t.x - player.x, 2) + Math.pow(t.y - player.y, 2) > 1200 * 1200)
				{
					tringlers.remove(i);
					i--;
					continue;
				}
				double extraradius = player.shielded ? 10 : 0;
				if ((!t.slowDown && Math.pow(t.x + 10 * Math.cos(t.rotation) - player.x, 2) + Math.pow(t.y + 10 * Math.sin(t.rotation) - player.y, 2) < Math.pow(player.radius + 4 + extraradius, 2))
						|| (t.slowDown && Math.pow(t.x - player.x, 2) + Math.pow(t.y - player.y, 2) < Math.pow(player.radius + 16 + extraradius, 2)))
				{
					// collision
					tringlers.remove(i);
					i--;
					if (!player.shielded)
						player.damage(10);
					continue;
				}
				t.chargeTimeLeft -= deltaTime;
				if (t.chargeTimeLeft < t.chargeDelay - 1.1) // not dashing
					if (t.chargeTimeLeft > 0.9) // not dashing
						t.rotation += deltaTime * t.chargeTimeLeft / 5 * 12; // variable rotation speed
					else
						t.rotation = lerpAngle(t.rotation, Math.atan2(player.y - t.y, player.x - t.x), deltaTime * 3.5);
				t.x += t.xVel * deltaTime;
				t.y += t.yVel * deltaTime;
				double speedPow2 = t.xVel * t.xVel + t.yVel * t.yVel;
				if (t.chargeTimeLeft >= t.chargeDelay - 1 && speedPow2 < 600 * 600) // dashing
				{

					t.xVel += 6 * t.xVel * deltaTime;
					t.yVel += 6 * t.yVel * deltaTime;
				}
				if (t.chargeTimeLeft < t.chargeDelay - 1 && speedPow2 > 0) // un-dashing
				{
					double distPow2 = Math.pow(player.x - t.x, 2) + Math.pow(player.y - t.y, 2);
					if (distPow2 < t.prevDistPow2)
						t.prevDistPow2 = distPow2;
					else if (distPow2 - 2500 > t.prevDistPow2)
					{
						t.slowDown = true;
					}
					if (t.slowDown)
					{
						t.xVel -= 3 * t.xVel * deltaTime;
						t.yVel -= 3 * t.yVel * deltaTime;
					} else
						t.chargeTimeLeft += deltaTime;
				}
				if (t.chargeTimeLeft < 0) // begin dash
				{
					t.chargeTimeLeft = t.chargeDelay;
					double angle = Math.atan2(player.y - t.y, player.x - t.x);
					t.xVel = 10 * Math.cos(angle);
					t.yVel = 10 * Math.sin(angle);
					t.slowDown = false;
					t.prevDistPow2 = 9999999;
				}
			}
		}
		eventTimeLeft -= deltaTime;
		synchronized (tringlers)
		{
			if (eventTimeLeft < 0)
			{
				eventTimeLeft = eventFrequency;
				double ayn = Math.random();
				if (ayn < 0.1)
				{
					if (enemySurfers.size() < 1) // maximum 1 enemy surfer
						enemySurfers.add(new Surfer(0, 0, 300));
				} else if (ayn < 1.0)
					if (tringlers.size() < 6) // maximum 6 enemy tringlers
						tringlers.add(new Tringler(Math.random() * frameWidth - frameWidth / 2, Math.random() * frameHeight - frameHeight / 2));
			}
		}
	}

	// This is what you use to call draw methods or to just draw. Is called in
	// the repaint() method which is called after every frame.
	void paintBuffer(final Graphics g)
	{
		Graphics2D buffer = (Graphics2D) g;

		// Move "camera" to position
		AffineTransform original = buffer.getTransform();
		buffer.translate(0.5 * frameWidth, 0.5 * frameHeight);
		buffer.translate(-camera.x, -camera.y);

		// Draw stuff

		// Waves
		synchronized (waves)
		{
			for (int i = 0; i < waves.size(); i++)
			{
				// Draw outlines
				Wave w = waves.get(i);
				buffer.setColor(w.color);
				if (w.r2 <= w.width)
				{
					buffer.fillOval((int) (w.cx - w.r2), (int) (w.cy - w.r2), (int) (2 * w.r2), (int) (2 * w.r2));
				} else
				{
					buffer.setStroke(new BasicStroke((int) (w.width)));
					buffer.drawOval((int) (w.cx - w.r2 + w.width / 2), (int) (w.cy - w.r2 + w.width / 2), (int) (2 * (w.r2 - w.width / 2)), (int) (2 * (w.r2 - w.width / 2)));
					if (w.infested)
					{
						buffer.setColor(Wave.infestation);
						buffer.setStroke(new BasicStroke((int) (w.width)));
						buffer.drawArc((int) (w.cx - w.r2 + w.width / 2), (int) (w.cy - w.r2 + w.width / 2), (int) (2 * (w.r2 - w.width / 2)), (int) (2 * (w.r2 - w.width / 2)),
								(int) (w.plagueStart * 180 / Math.PI), (int) ((w.plagueEnd - w.plagueStart) * 180 / Math.PI));
					}
				}
			}
		}
		// Wavers
		for (Waver wr : wavers)
		{
			// Draw plus sign
			buffer.setStroke(new BasicStroke(2));
			buffer.setColor(new Color(255 - (int) (255 * wr.timeLeft / wr.freq), 0, 0));
			buffer.drawLine((int) (wr.x), (int) (wr.y - 20), (int) (wr.x), (int) (wr.y + 20));
			buffer.drawLine((int) (wr.x - 20), (int) (wr.y), (int) (wr.x + 20), (int) (wr.y));
		}
		// Surfers
		for (Surfer s : enemySurfers)
		{
			buffer.setStroke(new BasicStroke(2));
			buffer.setColor(Color.BLACK);
			buffer.fillOval((int) (s.x - s.radius), (int) (s.y - s.radius), 2 * s.radius, 2 * s.radius);
			buffer.setColor(Color.WHITE);
			buffer.drawOval((int) (s.x - s.radius), (int) (s.y - s.radius), 2 * s.radius, 2 * s.radius);
		}
		// Tringlers
		synchronized (tringlers)
		{
			for (Tringler t : tringlers)
			{
				int[] xPoints = new int[3];
				int[] yPoints = new int[3];
				for (int i = 0; i < 3; i++)
				{
					if (!t.slowDown)
					{
						double speedRatio = (t.xVel * t.xVel + t.yVel * t.yVel) / (600 * 600);
						double amount = i == 0 ? 1 + 1 * speedRatio : 1 - 0.5 * speedRatio;
						xPoints[i] = (int) (t.x + Tringler.radius * amount * Math.cos(t.rotation + TAU / 3 * i));
						yPoints[i] = (int) (t.y + Tringler.radius * amount * Math.sin(t.rotation + TAU / 3 * i));
					} else
					{
						xPoints[i] = (int) (t.x + Tringler.radius * Math.cos(t.rotation + TAU / 3 * i));
						yPoints[i] = (int) (t.y + Tringler.radius * Math.sin(t.rotation + TAU / 3 * i));
					}
				}
				buffer.setStroke(new BasicStroke(2));
				buffer.setColor(Tringler.sicklyGreen);
				buffer.fillPolygon(xPoints, yPoints, 3);
				buffer.setColor(Tringler.radGreen);
				buffer.drawPolygon(xPoints, yPoints, 3);
			}
		}

		// Player
		if (dashTime <= 0)
		{
			double choking = player.underwaterTimer / Player.maxUnderwater;
			buffer.setColor(new Color(255, 200, (int) (200 * choking)));
			if (player.injureFlash <= 0.15)
				buffer.setColor(Color.RED);
			buffer.fillOval((int) (player.x - player.radius), (int) (player.y - player.radius), 2 * player.radius, 2 * player.radius);
			buffer.setColor(Color.BLACK);
			buffer.drawOval((int) (player.x - player.radius), (int) (player.y - player.radius), 2 * player.radius, 2 * player.radius);

		} else
		{
			// dashing diamond
			int[] xPoints = new int[4];
			int[] yPoints = new int[4];
			for (int i = 0; i < 4; i++)
			{
				double speedRatio = (player.xVel * player.xVel + player.yVel * player.yVel) / (800 * 800);
				double amount = i == 2 ? 1 + 1 * speedRatio : 1;
				double rotation = Math.atan2(player.yVel, player.xVel);
				xPoints[i] = (int) (player.x + player.radius * amount * Math.cos(rotation + TAU / 4 * i));
				yPoints[i] = (int) (player.y + player.radius * amount * Math.sin(rotation + TAU / 4 * i));
			}
			buffer.setStroke(new BasicStroke(3));
			buffer.setColor(Color.YELLOW);
			buffer.fillPolygon(xPoints, yPoints, 4);
			buffer.setColor(Color.MAGENTA);
			buffer.drawPolygon(xPoints, yPoints, 4);

		}
//Life Ring
		/*buffer.setColor(Color.BLACK);
		buffer.setStroke(new BasicStroke((int) (6)));
		buffer.drawArc((int)player.x-player.radius, (int)player.y-player.radius, player.radius*2, player.radius*2, 0, (int)360);*/
		buffer.setColor(player.life.getColor());
		buffer.setStroke(new BasicStroke((int) (4)));
		int gap = 5;
		buffer.drawArc((int)player.x-player.radius-gap, (int)player.y-player.radius-gap, (player.radius+gap)*2, (player.radius+gap)*2, (int)(player.life.rotation), (int)(player.life.getAngle()/TAU*180));
		buffer.drawArc((int)player.x-player.radius-gap, (int)player.y-player.radius-gap, (player.radius+gap)*2, (player.radius+gap)*2, (int)(player.life.rotation)+180, (int)(player.life.getAngle()/TAU*180));
		// Move camera back
		buffer.setTransform(original);
	}

	// Setup of everything! Put stuff in here, not in Main()!
	void restart()
	{
		test = false;
		camera = new Point(0, 0);
		leftMousePressed = false;
		leftPressed = false;
		rightPressed = false;
		upPressed = false;
		downPressed = false;
		Resources.initialize();

		waves = new ArrayList<Wave>();
		wavers = new ArrayList<Waver>();
		enemySurfers = new ArrayList<Surfer>();
		tringlers = new ArrayList<Tringler>();

		player = new Player(0, 0, 450);
		for (int i = 0; i < 5; i++)
			tringlers.add(new Tringler(300 * Math.cos(i * TAU / 5), 300 * Math.sin(i * TAU / 5), 0.2 * i));

		wavers.add(new Waver(400, 150, 60, 40, 6.0, Wave.purple));
		wavers.add(new Waver(-400, -150, 60, 40, 6.0, Wave.purple));
		wavers.add(new Waver(150, -400, 60, 40, 6.0, Wave.purple));
		wavers.add(new Waver(-150, 400, 60, 40, 6.0, Wave.purple));
		eventTimeLeft = 10;
		dashTime = 0;
		dashCooldown = 1;
	}

	double[] moveByPlayerKeys()
	{
		player.x = Math.min(player.x, frameWidth / 2 - extraDistanceHorizontal);
		player.y = Math.min(player.y, frameHeight / 2 - extraDistanceVertical + 20); // sorry
		player.x = Math.max(player.x, -frameWidth / 2 + extraDistanceHorizontal);
		player.y = Math.max(player.y, -frameHeight / 2 + extraDistanceVertical);
		double verticalMovement = 0, horizontalMovement = 0;
		if (upPressed)
			verticalMovement -= 1;
		if (downPressed)
			verticalMovement += 1;
		if (leftPressed)
			horizontalMovement -= 1;
		if (rightPressed)
			horizontalMovement += 1;
		if (horizontalMovement == 0 && verticalMovement == 0)
			return new double[]
			{ 0, 0 };
		return new double[]
		{ Math.atan2(verticalMovement, horizontalMovement), 1 };
	}

	double[] moveByFollowPlayer(Surfer surfer)
	{
		return new double[]
		{ Math.atan2(player.y - surfer.y, player.x - surfer.x), 1 };
	}

	void moveSurfer(Surfer surfer, double[] move, double deltaTime)
	{
		int numOfContainingWaves = 0;
		double avgPushesX = 0;
		double avgPushesY = 0;
		for (int i = 0; i < waves.size(); i++)
		{
			Wave w = waves.get(i);
			if (w.surfable && w.contains(surfer.x, surfer.y))
			{
				surfer.lastWaveIndex = i;
				// add velocity
				double angle = Math.atan2(surfer.y - w.cy, surfer.x - w.cx);
				avgPushesX += w.speed * Math.cos(angle);
				avgPushesY += w.speed * Math.sin(angle);
				numOfContainingWaves++;
			}
		}
		if (numOfContainingWaves > 1)
		{
			surfer.x += deltaTime * avgPushesX / numOfContainingWaves;
			surfer.y += deltaTime * avgPushesY / numOfContainingWaves;
			surfer.shielded = true;
		} else
			surfer.shielded = false;
		if (surfer.lastWaveIndex == -1) // outside of wave
			move[1] *= 3.0; // works wackishly
		surfer.xVel += move[1] * Surfer.acceleration * Math.cos(move[0]);
		surfer.yVel += move[1] * Surfer.acceleration * Math.sin(move[0]);
		double vel2 = surfer.xVel * surfer.xVel + surfer.yVel * surfer.yVel;
		double ratio = Math.abs(vel2 / surfer.maxSpeedPow2);
		if (ratio > 1)
		{
			surfer.xVel /= ratio;
			surfer.yVel /= ratio;
		} else if (move[1] == 0)
		{
			surfer.xVel *= 0.9;
			surfer.yVel *= 0.9;
		}

		surfer.x += deltaTime * surfer.xVel;
		surfer.y += deltaTime * surfer.yVel;
		if (surfer.lastWaveIndex != -1)
		{
			Wave w = waves.get(surfer.lastWaveIndex);
			double distFromWaveCenterPow2 = Math.pow(w.cx - surfer.x, 2) + Math.pow(w.cy - surfer.y, 2);
			double angle = Math.atan2(surfer.y - w.cy, surfer.x - w.cx);
			if (numOfContainingWaves == 0)
				if (distFromWaveCenterPow2 < w.r1 * w.r1)
				{
					// teleport surfer "into" wave
					surfer.x = w.cx + w.r1 * Math.cos(angle);
					surfer.y = w.cy + w.r1 * Math.sin(angle);
				} else if (distFromWaveCenterPow2 > w.r2 * w.r2)
				{
					// teleport surfer "into" wave
					surfer.x = w.cx + w.r2 * Math.cos(angle);
					surfer.y = w.cy + w.r2 * Math.sin(angle);
				}
		}
	}

	// Is called when you start to press a key, and then keeps being called
	// until you stop pressing the key

	public void keyPressed(final KeyEvent e)
	{
		if (lastKeyPressed != e.getKeyCode())
		{
			lastKeyPressed = e.getKeyCode();
			timeSinceLastKeyPressed = 0;
		} else if (player.lastWaveIndex != -1 && dashCooldown <= 0 && timeSinceLastKeyPressed <= 0.4)
			dash = true;
		switch (e.getKeyCode())
		{
		case KeyEvent.VK_R:// Restart
			restart();
			break;
		case KeyEvent.VK_ESCAPE:// Exit
			exitGame();
			break;
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_A:
			leftPressed = true;
			break;
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_D:
			rightPressed = true;
			break;
		case KeyEvent.VK_UP:
		case KeyEvent.VK_W:
			upPressed = true;
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_S:
			downPressed = true;
			break;
		default:
			// another key was pressed; do nothing.
			break;
		}
	}

	// Is called when you stop pressing a key
	public void keyReleased(final KeyEvent e)
	{
		if (lastKeyPressed == e.getKeyCode() && timeSinceLastKeyPressed > 0.3)
			lastKeyPressed = -1;
		switch (e.getKeyCode())
		{
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_A:
			leftPressed = false;
			break;
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_D:
			rightPressed = false;
			break;
		case KeyEvent.VK_UP:
		case KeyEvent.VK_W:
			upPressed = false;
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_S:
			downPressed = false;
			break;
		default:
			// another key was released; do nothing.
			break;
		}
	}

	// Start of the program. Set-up stuff happens in restart(), NOT in here!
	public Main()
	{
		setSize(640, 640); // will be the size if window stops being maximized
		setResizable(true);
		setFocusTraversalKeysEnabled(false);
		setExtendedState(Frame.MAXIMIZED_BOTH);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				exitGame();
			}
		});

		frameWidth = (int) this.getBounds().getWidth();
		frameHeight = (int) this.getBounds().getHeight();

		setVisible(true);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
		addWindowFocusListener(this);
		addMouseWheelListener(this);
	}

	// Just don't change the main() method
	public static void main(String[] args)
	{
		Main main = new Main();
		main.restart();
		main.mainLoop();
	}

	// Main loop. You probably shouldn't change the code here.
	void mainLoop()
	{
		long lastLoopTime = System.nanoTime();
		final int TARGET_FPS = 60;
		final long OPTIMAL_TIME = 1000000000 / TARGET_FPS;

		// keep looping until the game ends
		while (true)
		{
			// work out how long its been since the last update
			long now = System.nanoTime();
			long updateLength = now - lastLoopTime;
			lastLoopTime = now;
			double delta = updateLength / ((double) OPTIMAL_TIME);

			// update the game logic
			gameFrame(delta / TARGET_FPS);

			// draw everyting
			repaint();

			try
			{
				Thread.sleep((Math.max(0, lastLoopTime - System.nanoTime()) + OPTIMAL_TIME) / 1000000);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			;
		}
	}

	// When you exit the game
	void exitGame()
	{
		System.exit(0);
	}

	public static double lerpAngle(double angle, double target, double amount)
	{
		return angle + (((((target - angle) % (Math.PI * 2)) + (Math.PI * 3)) % (Math.PI * 2)) - Math.PI) * amount;
	}

	// IGNORE
	private void adjustbuffer()
	{
		// getting image size
		bufferWidth = getSize().width;
		bufferHeight = getSize().height;

		// clean buffered image
		if (bufferGraphics != null)
		{
			bufferGraphics.dispose();
			bufferGraphics = null;
		}
		if (bufferImage != null)
		{
			bufferImage.flush();
			bufferImage = null;
		}
		System.gc(); // Garbage cleaner

		// create the new image with the size of the panel
		bufferImage = createImage(bufferWidth, bufferHeight);
		bufferGraphics = bufferImage.getGraphics();
	}

	// IGNORE
	public void update(final Graphics g)
	{
		paint(g);
	}

	// IGNORE
	public void paint(final Graphics g)
	{
		// Resetting the buffered Image
		if (bufferWidth != getSize().width || bufferHeight != getSize().height || bufferImage == null || bufferGraphics == null)
			adjustbuffer();

		if (bufferGraphics != null)
		{
			// this clears the offscreen image, not the onscreen one
			bufferGraphics.setColor(bgColor);
			bufferGraphics.fillRect(0, 0, bufferWidth, bufferHeight);

			// calls the paintbuffer method with buffergraphics
			paintBuffer(bufferGraphics);

			// painting the buffered image on to the visible frame
			g.drawImage(bufferImage, 0, 0, this);
		}
	}

	// IGNORE
	public void keyTyped(final KeyEvent e)
	{

	}

	// IGNORE
	public void mouseDragged(final MouseEvent me)
	{
		// Getting mouse info
		pin = MouseInfo.getPointerInfo();
		incorrectMousePoint = pin.getLocation();
		mx = (int) (incorrectMousePoint.getX() - this.getX());
		my = (int) (incorrectMousePoint.getY() - this.getY());
	}

	// IGNORE
	public void mouseClicked(final MouseEvent me)
	{
	}

	// IGNORE
	public void mouseEntered(final MouseEvent me)
	{
	}

	// IGNORE
	public void componentHidden(ComponentEvent arg0)
	{

	}

	// When the window is resized by the user
	public void componentResized(ComponentEvent e)
	{
		frameWidth = (int) this.getBounds().getWidth();
		frameHeight = (int) this.getBounds().getHeight();
	}

	// IGNORE
	public void componentMoved(ComponentEvent arg0)
	{

	}

	// IGNORE
	public void componentShown(ComponentEvent arg0)
	{

	}

	// IGNORE
	public void mouseExited(final MouseEvent me)
	{

	}

	// Mouse position changed
	public void mouseMoved(final MouseEvent me)
	{
		// Getting mouse info
		pin = MouseInfo.getPointerInfo();
		incorrectMousePoint = pin.getLocation();
		mx = (int) (incorrectMousePoint.getX() - this.getX());
		my = (int) (incorrectMousePoint.getY() - this.getY());
	}

	// Mouse was pressed
	public void mousePressed(final MouseEvent me)
	{
		// BUTTON1 = left click
		// BUTTON2 = mid click (scroll wheel click),
		// BUTTON3 = right click
		// This will only trigger when you press the mouse, and only once.
		// Unlike the keys it won't repeatedly "click" the mouse again
		if (me.getButton() == MouseEvent.BUTTON1) // Left Click
		{
			leftMousePressed = true;
		}
	}

	// Mouse was unpressed
	public void mouseReleased(final MouseEvent me)
	{
		if (me.getButton() == MouseEvent.BUTTON1) // Left Click
		{
			leftMousePressed = false;
		}
	}

	// Window gained focus
	public void windowGainedFocus(WindowEvent arg0)
	{

	}

	// Window lost focus
	public void windowLostFocus(WindowEvent arg0)
	{

	}

	// Mouse was scrolled up/down
	public void mouseWheelMoved(MouseWheelEvent arg0)
	{
		@SuppressWarnings("unused")
		boolean direction = arg0.getWheelRotation() == 1 ? true : false;
	}

}
