package team.tool;

public enum SchedulePriority {
	LOWEST(Double.NEGATIVE_INFINITY),
	LEVEL0(0.0),
	LEVEL1(1.0),
	LEVEL2(2.0),
	LEVEL3(3.0),
	LEVEL4(4.0),
	LEVEL5(5.0),
	LEVEL6(6.0),
	LEVEL7(7.0),
	LEVEL8(8.0),
	LEVEL9(9.0),
	HIGHEST(Double.POSITIVE_INFINITY);
	
	private double level;
	SchedulePriority(double level) {
		this.setLevel(level);
	}
	public double getLevel() {
		return level;
	}
	public void setLevel(double level) {
		this.level = level;
	}
}
