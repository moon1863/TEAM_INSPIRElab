package team;

import com.vividsolutions.jts.geom.Coordinate;

import team.vehicle.Vehicle;

public class HailingAnswer {
	private Coordinate pickUpcoord;
	private Vehicle rentedVehicle;

	public Coordinate getPickUpcoord() {
		return pickUpcoord;
	}

	public void setPickUpcoord(Coordinate pickUpcoord) {
		this.pickUpcoord = pickUpcoord;
	}

	public Vehicle getRentedVehicle() {
		return rentedVehicle;
	}

	public void setRentedVehicle(Vehicle rentedVehicle) {
		this.rentedVehicle = rentedVehicle;
	}
}
