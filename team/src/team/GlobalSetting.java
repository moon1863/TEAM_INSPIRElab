package team;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.thoughtworks.xstream.XStream;
import com.vividsolutions.jts.geom.GeometryFactory;

import team.tool.Data;
import team.tool.Data.Unit;

// TeamRuntime stands for background engine for team project
public class GlobalSetting {
	public static BackgroundEngine engine;
	// global setting
	public final static String mainGeographyName = "mainGeography";
	public final static String roadNetworkName = "roadNetwork";
	public final static String mainContextName = "mainContext";
	public final static String runtimeStartingTime = "03:00:00";
	public final static String runtimeStartingDate = "2020-02-13";
	public final static LocalTime startingTime = LocalTime.parse(runtimeStartingTime);
	public final static LocalDate startingDate = LocalDate.parse(runtimeStartingDate);
	public final static LocalDateTime startingDateTime = LocalDateTime.of(startingDate, startingTime);
	public final static LocalDateTime endingDateTime = LocalDateTime.of(startingDate.plusDays(1), startingTime.minusSeconds(1));
	public final static Data maxWalkingDist = new Data(1, Unit.MILE);
	public final static Data safeRemainMileage = new Data(0.5, Unit.MILE);
	public final static GeometryFactory geomFac = new GeometryFactory();
	
	
	public final static String xmlDirectory = "data/data_processing/xmlFiles/";
	public final static String serDirectory = "data/SerFiles/";
	public static XStream xStream = new XStream();
	
	public final static double safeMileageRatio = 0.3;
	public final static int maxAnticipate = 3;
	
	public static final Data oneMinute = new Data(1,Unit.MINUTE);
	public static final Data fiveMinutes = new Data(5,Unit.MINUTE);
	public static final Data tenMinutes = new Data(10,Unit.MINUTE);
	public static final Data twentyMinutes = new Data(20,Unit.MINUTE);
	public static final Data halfAnHour = new Data(30,Unit.MINUTE);
	public static final Data oneHour = new Data(60,Unit.MINUTE);
	public static final Data oneDay = new Data(24,Unit.HOUR);
	
	public final static Data simIntervalInSecond  = oneMinute.transfer(Unit.SECOND);
	public final static Data simIntervalInMinute  = oneMinute;
	public final static Data simIntervalInHour  = simIntervalInSecond.transfer(Unit.HOUR);
//	public final static Data walkingSpeed = (new Data(1, Unit.METER).divide(new Data(1, Unit.SECOND))).transfer(Unit.MPH);
	
	public final static Data simulateSumDays  = new Data(10, Unit.DAY);
	public final static Data sumTick  = simulateSumDays.divide(simIntervalInSecond);
	public final static int updateTicks  = (int) oneDay.transfer(Unit.MINUTE).divide(simIntervalInMinute).num;
	
	public static final double LOWEST = Double.NEGATIVE_INFINITY;
	public static final double LEVEL0 = 0.0;
	public static final double LEVEL1 = 1.0;
	public static final double LEVEL2 = 2.0;
	public static final double LEVEL3 = 3.0;
	public static final double LEVEL4 = 4.0;
	public static final double LEVEL5 = 5.0;
	public static final double LEVEL6 = 6.0;
	public static final double LEVEL7 = 7.0;
	public static final double LEVEL8 = 8.0;
	public static final double LEVEL9 = 9.0;
	public static final double LEVEL10 = 10.0;
	public static final double HIGHEST = Double.POSITIVE_INFINITY;
	
	public static final double oneDayTicks =  24 * 60;
	
	public static int initialVehicleNumber;
	
	public static final boolean isHaveHPMap = false;   // global switch for this project whether the percentage of home plug map exist	
	
	
}
