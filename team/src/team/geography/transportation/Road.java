package team.geography.transportation;


import java.io.Serializable;

import team.geography.FixedGeography;
import team.tool.Data;

public class Road extends FixedGeography implements Cloneable, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3721627635144284533L;
	private boolean isDirected = false;		// if the shape file didn't offer the direction information, we think it's bidirectional
	private boolean direction = true;       // true -> 0 is start, false 0 -> end, according this attribute to decide which coordinate starts
	private double geoLenNum;
	private String geoLenUnit;
	private Data geoLength;					// geography length the Unit may be kilometer or miles
	private double maxSpeNum;
	private String maxSpeUnit;
	private Data maxSpeed;					// stands for the max speed
	
	private Data instantSpeed;
	private NetworkEdge<Object> edge;
	
	private int selectNum = 0;

    public Road() {
	}


	@Override  
    public Object clone() {  
        Road cloneRoad = null;  
        try{  
        	cloneRoad = (Road)super.clone();  
        }catch(CloneNotSupportedException e) {  
            e.printStackTrace();  
        }  
        return cloneRoad;  
    } 


	public Data getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(Data maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public NetworkEdge<Object> getEdge() {
		return edge;
	}
	
	@SuppressWarnings("unchecked")
	public void setEdge(NetworkEdge<?> edge) {
		 this.edge = (NetworkEdge<Object>) edge;
	}

	public boolean isDirected() {
		return isDirected;
	}

	public void setDirected(boolean isDirected) {
		this.isDirected = isDirected;
	}

	public boolean getDirection() {
		return direction;
	}

	public void setDirection(boolean b) {
		this.direction = b;
	}

	public Data getInstantSpeed() {
		return instantSpeed;
	}

	public void setInstantSpeed(Data instantSpeed) {
		this.instantSpeed = instantSpeed;
	}


	public void setGeoLength(Data geoLength) {
		this.geoLength = geoLength;
	}
	
	public double getGeoLenNum() {
		return geoLenNum;
	}


	public String getGeoLenUnit() {
		return geoLenUnit;
	}


	public double getMaxSpeNum() {
		return maxSpeNum;
	}


	public String getMaxSpeUnit() {
		return maxSpeUnit;
	}


	public Data getGeoLength() {
		// TODO Auto-generated method stub
		return geoLength;
	}


	public void setGeoLenNum(double geoLenNum) {
		this.geoLenNum = geoLenNum;
	}


	public void setGeoLenUnit(String geoLenUnit) {
		this.geoLenUnit = geoLenUnit;
	}


	public void setMaxSpeNum(double maxSpeNum) {
		this.maxSpeNum = maxSpeNum;
	}


	public void setMaxSpeUnit(String maxSpeUnit) {
		this.maxSpeUnit = maxSpeUnit;
	}


	public int getSelectNum() {
		return selectNum;
	}


	public void setSelectNum(int selectNum) {
		this.selectNum = selectNum;
	}
	
}
