
public class Surfer {
	public static int radius = 10; //for drawing only
	public double maxSpeedPow2;
	public static double acceleration = 1000;
	double x;
	double y;
	int lastWaveIndex;
	double xVel, yVel;
	public Surfer(int x_, int y_, double maxSpeed)
	{
		lastWaveIndex = -1;
		x = x_;
		y = y_;
		xVel = 0;
		yVel = 0;
		maxSpeedPow2 = maxSpeed*maxSpeed;
	}

}