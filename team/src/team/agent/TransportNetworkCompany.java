package team.agent;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import team.Task;
import team.vehicle.Vehicle;

public class TransportNetworkCompany extends BaseAgent {
	public static HashSet<TransportNetworkCompany> TransCompanies = new HashSet<>();
	public List<Task> tasksWaitedProcess = Collections.synchronizedList(new LinkedList<Task>());
	public HashSet<Vehicle>	commercialVehicles = new HashSet<Vehicle>();
	public Vehicle vehicleForTask = null;
	public Double anwserTask(Task task) {

		
		return (double) 0;
	}
	
	static public Vehicle receiveTask(Task task) {
		double minPrice = 0, price;
		TransportNetworkCompany minPriceCompany = null;
		for (TransportNetworkCompany company : TransCompanies) {
			price = company.anwserTask(task);
			minPriceCompany = minPrice > price? company:minPriceCompany;			
		}
		Vehicle pickupVehicle = minPriceCompany.vehicleForTask;
		return pickupVehicle;
	}
}
