package team.tool;

import repast.simphony.ui.plugin.TickCountFormatter;

import static team.GlobalSetting.*;
public class MyTickFormatter implements TickCountFormatter {

	@Override
	public String format(double tick) {
		// TODO Auto-generated method stub
		String time = engine.getRealTime().toString();
		if(time.length() <= 5) time = time + ":00";
		return "Time:" + time;
	}

	@Override
	public String getInitialValue() {
		// TODO Auto-generated method stub
		
		return "Time:" + runtimeStartingTime ;
	}

}
