package team;

import java.io.File;
import java.io.FileInputStream;

import java.io.FileOutputStream;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.opengis.feature.simple.SimpleFeature;

import com.thoughtworks.xstream.XStream;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import static team.GlobalSetting.*;
import static team.tool.MyFunction.*;


import repast.simphony.context.Context;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.gis.IntersectsQuery;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.Network;
import team.agent.Traveler;
import team.agent.TransportNetworkCompany;
import team.geography.FixedGeography;
import team.geography.TeamShapefileLoader;
import team.geography.boundary.Tract;
import team.geography.boundary.Zipcode;
import team.geography.transportation.Road;
import team.geography.transportation.TeamShortestPath;
import team.tool.Data;
import team.tool.FeatureProcess;
import team.tool.Data.Unit;
import team.vehicle.Model;
import team.vehicle.Vehicle;
import team.vehicle.Vehicle.Type;
import team.transportion.energy.FuelingStation;
import team.transportion.energy.FuelingStyle;

public class BackgroundEngine {
	
	private Logger LOGGER;
	private ISchedule schedule;
	private LocalDateTime realDateTime;			//used to record real time 
	
	private HashMap<String, Long> lastEditTimes; // file last editing time
	
	private Context<Object> context;
	private Network<Object> roadNetwork;
	private Geography<Object> geography;
	
	public HashMap<String, List<Traveler>> personsGroupedByZipcode;   // source date used to group original person
	public HashMap<String, List<Traveler>> personsGroupedByTract;
	public HashMap<String, Traveler> persons;						  // the persons that is extracted from xml files
	public HashMap<String, Model> models;							  // vehicle model table
	public HashMap<String, FuelingStation> fuelingStations;			  // original fueling station
	public HashMap<String, Integer> vehiclesWithinZipcode;			  // the simulate source data
	public HashMap<Integer, Float> projections;						  // persons increasing
	public HashMap<String, Double> zipcodePlugOwnerPercentMap;
	
	public List<String> zipcodesWithPersonsList;
	public List<String> tractsWithPersonsList;
	public Set<String> tractsWithPerson = new HashSet<>();
	
	public TransportNetworkCompany company = new TransportNetworkCompany();
	
	public HashMap<Object, Geometry> roads;		
	public HashMap<Object, Geometry> tracts;
	public HashMap<Object, Geometry> zipcodes;
	
	public HashMap<String, Tract> identifiedTracts;
	public HashMap<String, Zipcode> identifiedZipcodes;
	public HashMap<String, Road> identifiedRoads;
	
	public TransportNetworkCompany getCompany() {
		return company;
	}

	public static TeamShortestPath<Object> shortestPath;
	
	@SuppressWarnings("unchecked")
	public BackgroundEngine(Context<Object> mainContext, Geography<Object> geography) throws CloneNotSupportedException {
		this.context = mainContext;
		this.geography = geography;
		this.schedule = RunEnvironment.getInstance().getCurrentSchedule();
		this.realDateTime = startingDateTime;
		this.LOGGER = Logger.getLogger(ContextManager.class.getName());
		
		this.personsGroupedByZipcode = new HashMap<String, List<Traveler>>();
		this.personsGroupedByTract = new HashMap<String, List<Traveler>>();
		
		this.zipcodesWithPersonsList = new ArrayList<String>();
		this.tractsWithPersonsList = new ArrayList<String>();
		
		this.roads = new HashMap<Object, Geometry>();		
		this.tracts = new HashMap<Object, Geometry>();
		this.zipcodes = new HashMap<Object, Geometry>();
		
		this.identifiedTracts = new HashMap<String, Tract>();
		this.identifiedZipcodes = new HashMap<String, Zipcode>();
		this.identifiedRoads = new HashMap<String, Road>();
		
		// serialize procedure
		File times = new File(serDirectory + "xmlLastEditTimes.ser");
		xStream.setClassLoader(this.getClass().getClassLoader());
		if(times.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(times))){
				lastEditTimes = (HashMap<String, Long>) ois.readObject();
			} catch (Exception e) {	e.printStackTrace(); }
		} else lastEditTimes = new HashMap<String, Long>();
		
		boolean updated = false;
		try {
			// *********************model updates***********************
			updated |= updateAgents("models", XMLProcess.modelsXStreamSetting);
			Model.calcuteRandomRange(models);
			
			// *********************fueling stations update***********************
			updated |= updateAgents("fuelingStations", XMLProcess.fuelingStationsXStreamSetting);
			fuelingStations.values().stream().forEach(fuelingStation -> {
				fuelingStation.fuelingStyles = new HashMap<FuelingStyle, Integer>();
				fuelingStation.originalFuelingStyles.forEach((k, v) -> 
					{ fuelingStation.fuelingStyles.put(Enum.valueOf(FuelingStyle.class, k.toUpperCase()), v); });
			    context.add(fuelingStation);
			    geography.move(fuelingStation, GlobalSetting.geomFac.createPoint(fuelingStation.getCoord()));
			    fuelingStation.initSales();
			});
			
			// *********************persons updates***********************
			updated |= updateAgents("persons", XMLProcess.personsXStreamSetting);
			Iterator<String> iterPerson = persons.keySet().iterator();
			while(iterPerson.hasNext()) {
				Traveler person = persons.get(iterPerson.next());
				person.getTasks().forEach(kTemp -> kTemp.convertTime());
				person.setBackupTasks(new LinkedList<Task>());
				for(int i = 0; i < person.getTasks().size(); i++)
					person.getBackupTasks().add((Task) person.getTasks().get(i).clone());
				
				String zipcode = person.getZipcode();
				if (!personsGroupedByZipcode.containsKey(zipcode)) 
					personsGroupedByZipcode.put(zipcode, new ArrayList<Traveler>());
				personsGroupedByZipcode.get(zipcode).add(person);
				
				String tract = person.getTract();
				if (!personsGroupedByTract.containsKey(tract)) 
					personsGroupedByTract.put(tract, new ArrayList<Traveler>());
				personsGroupedByTract.get(tract).add(person);
			}
			zipcodesWithPersonsList.addAll(personsGroupedByZipcode.keySet());
			tractsWithPersonsList.addAll(personsGroupedByTract.keySet());
			
			// *********************vehicles updates***********************
			updated |= updateAgents("vehiclesWithinZipcode", XMLProcess.vehiclesXStreamSetting);
			// *********************vehicles updates***********************
			updated |= updateAgents("projections", XMLProcess.projectionsXStreamSetting);
			if(isHaveHPMap)
				updated |= updateAgents("zipcodePlugOwnerPercentMap", XMLProcess.HPMapXStreamSetting);
			// *********************shape file update***********************
			updated |= readShapefile(Road.class, "data/data_processing/roads/road_with_distance.shp", geography, mainContext, roadFeatureProcess);
			updated |= readShapefile(Tract.class, "data/data_processing/tracts/tract.shp", geography, mainContext, tractFeatureProcess);
			updated |= readShapefile(Zipcode.class, "data/data_processing/zipcodes/zipcode.shp", geography, mainContext, zipcodeFeatureProcess);
			
			if(updated) {
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(times));
				oos.writeObject(lastEditTimes);
				oos.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			times.delete();
			e.printStackTrace();
		}
		
		List<Traveler> selectivePersons = new ArrayList<Traveler>();
		Iterator<String> iterZip = vehiclesWithinZipcode.keySet().iterator();
		String zipcode;	List<Traveler> personList; Traveler person; Random random = new Random(); double percentage;
		while(iterZip.hasNext()) {
			zipcode = iterZip.next();
			personList = personsGroupedByZipcode.get(zipcode);
			if(personList == null) continue;
			percentage = isHaveHPMap ? zipcodePlugOwnerPercentMap.get(zipcode) : 0.2;
			for (int i = 0; i < vehiclesWithinZipcode.get(zipcode); i++) {
				person = (Traveler) personList.get(random.nextInt(personList.size())).clone();
				tractsWithPerson.add(person.getTract());
				//TODO comment
				person.setHomePlugOwner(Math.random() < percentage);
//				person.setHomePlugOwner(true);
				if (person.isHomePlugOwner()) 
					addHomePlug(person);
				selectivePersons.add(person);
			}
		}
		addPersons(selectivePersons, (int) (schedule.getTickCount()/oneDayTicks));

		/*
		 * because fueling station doesn't include tract information, 
		 * we must use repast function to add fueling station information to the tract 
		 */
		tracts.forEach((t, geom) -> {
			Tract tract = (Tract) t;
			IntersectsQuery<Object> query  = new IntersectsQuery<Object>(geography, geom);
			for(Object object : query.query()) {
				if (object instanceof FuelingStation)
					tract.getFuelingStations().add((FuelingStation) object);
			}
		});
		
		initialVehicleNumber = geography.getLayer(Vehicle.class).getAgentSet().size();
	}
	
	
	private void addHomePlug(Traveler person) {
		FuelingStation privateStation = new FuelingStation();
		privateStation.fuelingStyles = new HashMap<FuelingStyle, Integer>();
		privateStation.fuelingStyles.put(FuelingStyle.L2, 1);
		Coordinate coord = person.getTasks().getFirst().getDepartCoord();
		privateStation.setCoord(coord);
		privateStation.initSales();
		Point p = GlobalSetting.geomFac.createPoint(coord);
		geography.move(privateStation, p);
		context.add(privateStation);
		person.setPrivateHomePlug(privateStation);
	}
	
	private <T extends FixedGeography> boolean readShapefile(Class<T> clazz, String shapefileLocation, 
			Geography<Object> geog, Context<Object> context,FeatureProcess<? super T> featureProcess) 
					throws NoSuchFieldException, SecurityException {
		File shapefile = null;
		TeamShapefileLoader<T> loader = null;
		shapefile = new File(shapefileLocation);
		String name = clazz.getSimpleName().toLowerCase() + "s";
		Field field = this.getClass().getField(name);
		File serFile = new File(serDirectory + name + ".ser");
		try {
			loader = new TeamShapefileLoader<T>(clazz, shapefile.toURI().toURL(), geog, context, featureProcess);
			while (loader.hasNext())
				loader.next();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serFile));
			oos.writeObject(field.get(this));
			oos.close();
			lastEditTimes.put(shapefileLocation, shapefile.lastModified());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			serFile.delete();
		}
		return false;
	}
	
	public boolean updateAgents(String name, Function<XStream, XStream> setting) throws NoSuchFieldException, SecurityException {
		Field field = this.getClass().getField(name);
		Class<?> clazz = field.getType();
		File serFile = new File(serDirectory + name + ".ser");
		String xmlPath = xmlDirectory + name + ".xml";
		File xmlFile = new File(xmlPath);
		try {
			if(checkFileUpdated(serFile, xmlPath)) {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serFile));
				field.set(this, clazz.cast(ois.readObject()));
				ois.close();
				return false; // not updated
			} else {
				xStream = setting.apply(xStream);
				var ins = new FileInputStream(new File(xmlDirectory + name + ".xml"));
				field.set(this, clazz.cast(xStream.fromXML(ins)));
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serFile));
				oos.writeObject(field.get(this));
				oos.close();
				lastEditTimes.put(xmlPath, xmlFile.lastModified());
				return true; // 
			}
		} catch (Exception e) {
			e.printStackTrace();
			serFile.delete();
		}
		return false;
	}

	private boolean checkFileUpdated(File backupFile, String originalFilePath) {
		File originalFile = new File(originalFilePath);
		return backupFile.exists() 
				&& lastEditTimes != null 
				&& lastEditTimes.get(originalFilePath) != null 
				&& lastEditTimes.get(originalFilePath) == originalFile.lastModified();
	}
	
	@ScheduledMethod(start=1 , interval=1, priority = HIGHEST)
	public void engineUpdate() {
		realDateTime = realDateTime.plusSeconds((long) simIntervalInSecond.num);
	}
	
	@SuppressWarnings("unchecked")
	@ScheduledMethod(start = oneDayTicks + 1, interval = oneDayTicks, priority = LEVEL10)
	public void tasksUpdate() throws CloneNotSupportedException {
		int years = (int) (schedule.getTickCount()/oneDayTicks);
		System.out.print("This is the" + years + "th year!\n\n");
		realDateTime = realDateTime.plusYears(1);
		realDateTime = realDateTime.minusDays(1);
		
		// remain tasks person update
		Iterator<Traveler> persons = geography.getLayer(Traveler.class).getAgentSet().iterator();
		Traveler person = null;
		while(persons.hasNext()) {
			person = persons.next();
			person.getTasks().clear();
			for(int i = 0; i < person.getBackupTasks().size(); i++) {
				Task tempTask = (Task) person.getBackupTasks().get(i).clone();
				tempTask.setDepartTime(tempTask.getDepartTime().plusYears(years));
				tempTask.setArriveTime(tempTask.getArriveTime().plusYears(years));
				person.getTasks().add(tempTask);
			}

			person.setCoord(person.getTasks().peekFirst().getDepartCoord());
			geography.move(person, GlobalSetting.geomFac.createPoint(person.getCoord()));
			Vehicle vehicle = person.getPrivateVehicle();
			vehicle.setCoord(person.getCoord());
			geography.move(vehicle, GlobalSetting.geomFac.createPoint(person.getCoord()));
			vehicle.tasks = person.getTasks();
			vehicle.modelInit();
		}
		
		// TODO dynamic update person
		List<Traveler> selectivePersons = new ArrayList<Traveler>(); Random random = new Random(); double sumUtility = 0;
		int newPersonNumber = (int) (geography.getLayer(Traveler.class).getAgentSet().size() * (projections.get(realDateTime.getYear()) - 1));
		for(String tractName : tractsWithPerson) {
			Tract tract = identifiedTracts.get(tractName);
			tract.computeParas();
			sumUtility += tract.computeVehicleUtility();
		}
		for(String tractName : tractsWithPerson) {
			Tract tract = identifiedTracts.get(tractName);
			List<Traveler> tractPersons = personsGroupedByTract.get(tractName);
			for (int i = 0; i < tract.getVehicleUtility()/sumUtility * newPersonNumber; i++) {
				person = (Traveler) tractPersons.get(random.nextInt(tractPersons.size())).clone();
				tractsWithPerson.add(person.getTract());
//				TODO add get a new parameter
//				double percentage = homePlugPercentMap.get(person.getZipcode());
				double percentage = 0.2;
				//TODO  comment test
				person.setHomePlugOwner(Math.random() < percentage);
//				person.setHomePlugOwner(true);
				if (person.isHomePlugOwner()) 
					addHomePlug(person);
				selectivePersons.add((Traveler) tractPersons.get(random.nextInt(tractPersons.size())).clone());
			}
				
		}
		addPersons(selectivePersons, years);
		
		// TODO dynamic update fueling station
		int temp = geography.getLayer(Vehicle.class).getAgentSet().size()  -  initialVehicleNumber;
		// TODO: need to be better estimate!! currently is regression over states data and make sure the regression passed through the current study area results
		int addFuelingStationNumber =  (int) (-2.226e-7 * Math.pow(temp, 2) + 0.0591 * temp);
		// dynamic add
		
		sumUtility = 0;
		for(Object tract : tracts.keySet())
			sumUtility += ((Tract) tract).computeFuelingStationUtility();
		
		List<String> listStation = new ArrayList<String>(fuelingStations.keySet());
		for(Object object : tracts.keySet()) {
			Tract tract = (Tract) object;
			int tempNumber = (int) (addFuelingStationNumber * tract.getFuelingStationUtility() / sumUtility);
			MultiPoint mtPoint = createRandomCoord(tracts.get(tract), tempNumber);
			for (Coordinate coord : mtPoint.getCoordinates()){
				Point pt = geomFac.createPoint(coord);
				String id = listStation.get(random.nextInt(listStation.size()));
				FuelingStation station = (FuelingStation) fuelingStations.get(id).clone();
				station.initSales();
				context.add(station);
				geography.move(station, pt);
				tract.getFuelingStations().add(station);
			}
		}
		
		tracts.forEach((t, geom) -> {
			Tract tract = (Tract) t;
			IntersectsQuery<Object> query  = new IntersectsQuery<Object>(geography, geom);
			for(Object object : query.query()) {
				if (object instanceof FuelingStation)
					tract.getFuelingStations().add((FuelingStation) object);
			}
		});
		
		// tract fuelings station reset
		for(Object object : tracts.keySet()) {
			Tract tract = (Tract) object;
			tract.getPersons().forEach(p -> p.setFinished(true));
			tract.getFuelingStations().forEach(station -> station.fuelingSales.forEach((style, sale) -> sale.num = 0));
		}
	}
	
	// timer need a parameter for time
	public boolean timer(Data timeInterval) {
		return schedule.getTickCount() % (timeInterval.transfer(Unit.SECOND).divide(simIntervalInSecond).num) == 0;
	}
	
	@SuppressWarnings("unchecked")
	public void addPersons(List<Traveler> persons, int years) {
		persons.forEach(person -> {
			context.add(person);
			person.setCoord(person.getTasks().peekFirst().getDepartCoord());
			// tract add persons 
			identifiedTracts.get(person.getTract()).getPersons().add(person);
			person.setFinished(true);
			geography.move(person, GlobalSetting.geomFac.createPoint(person.getCoord()));
//			TODO add get a new parameter
			Vehicle vehicle = new Vehicle(person, Model.randomModel(), person.getCoord(), comparator, Type.PRIVATE);
			person.setPrivateVehicle(vehicle);
			geography.move(person.getPrivateVehicle(), GlobalSetting.geomFac.createPoint(person.getCoord()));
			person.getPrivateVehicle().tasks = (LinkedList<Task>) person.getTasks().clone();
			person.getPrivateVehicle().tasks.forEach(t -> {
				t.setDepartTime(t.getDepartTime().plusYears(years));
				t.setArriveTime(t.getArriveTime().plusYears(years));
			});
			person.getPrivateVehicle().setFirstTask(person.getPrivateVehicle().tasks.getFirst());
			context.add(person.getPrivateVehicle());
			schedule.schedule(person);
		});
	}
	
	// we implement virtual Object to process shape file, for road we need to make sure that if we need to get two road agent from one
	public FeatureProcess<Road> roadFeatureProcess = new FeatureProcess<Road> () {
		@Override
		public void apply(Road road, SimpleFeature feature) throws Exception {
			if(road.getGeoLenNum() != 0 && road.getGeoLenUnit() != null) 
				road.setGeoLength((new Data(road.getGeoLenNum(), Data.unitOf(road.getGeoLenUnit()))).transfer(Unit.MILE));
			else throw new IllegalArgumentException("The road shape file feature didn't include the geography length attribute!");
			
			if(road.getMaxSpeNum() != 0 && road.getMaxSpeUnit() != null)
				road.setMaxSpeed((new Data(road.getMaxSpeNum(), Data.unitOf(road.getMaxSpeUnit()))).transfer(Unit.MPH));
			else road.setMaxSpeed(new Data(70, Unit.MPH));
			
			Geometry geom = (Geometry) feature.getDefaultGeometry();
		    if (geom instanceof MultiLineString){
		    	MultiLineString ml = (MultiLineString)feature.getDefaultGeometry();
				geom = (LineString)ml.getGeometryN(0);
		    }
		    road.setCoord(geom.getCentroid().getCoordinate());
		    roads.put(road, geom);
		    geography.move(road, geom);
		    context.add(road);
		    
			if(!road.isDirected()) { // the shape file has not the attribute of  
				road.setDirected(true);
				road.setDirection(true);
				Road cloneRoad = (Road) road.clone();
				Geometry cloneGeom = (LineString) geom.clone();
			    road.setDirected(true);
			    cloneRoad.setDirection(false);
				roads.put(cloneRoad, cloneGeom);
			    context.add(cloneRoad);
			    geography.move(cloneRoad, (LineString) geom.clone());
			} // else if the road has a direction, it's ok. need some process when building the network
			  // we need to make sure that every road added to the network has a direction attribute
		}		
	};
	// boundary process
	
	public FeatureProcess<FixedGeography> tractFeatureProcess = new FeatureProcess<FixedGeography> () {
		@Override
		public void apply(FixedGeography fixedGeog, SimpleFeature feature) {
		    Geometry geom = (Geometry) feature.getDefaultGeometry();
		    if (geom instanceof MultiPolygon){
				MultiPolygon mp = (MultiPolygon)feature.getDefaultGeometry();
				geom = (Polygon)mp.getGeometryN(0);
		    }
		    fixedGeog.setIdentifier((String) feature.getAttribute("NAME"));
		    fixedGeog.setCoord(geom.getCentroid().getCoordinate());
		    tracts.put(fixedGeog, geom);
		    context.add(fixedGeog);
		    geography.move(fixedGeog, geom);
		    identifiedTracts.put(fixedGeog.getIdentifier(), (Tract) fixedGeog);
		    ((Tract) fixedGeog).setPersons(new HashSet<Traveler>());
		    ((Tract) fixedGeog).initSales();
		}		
	};
	
	public FeatureProcess<FixedGeography> zipcodeFeatureProcess = new FeatureProcess<FixedGeography> () {
		@Override
		public void apply(FixedGeography fixedGeog, SimpleFeature feature) {
		    Geometry geom = (Geometry) feature.getDefaultGeometry();
		    if (geom instanceof MultiPolygon){
				MultiPolygon mp = (MultiPolygon)feature.getDefaultGeometry();
				geom = (Polygon)mp.getGeometryN(0);
		    }
		    fixedGeog.setCoord(geom.getCentroid().getCoordinate());
		    String zipcode = ((Zipcode)fixedGeog).getZipcode();
		    fixedGeog.setIdentifier(zipcode);
		    Zipcode zipcodeAgent = (Zipcode)fixedGeog; 
		    if(vehiclesWithinZipcode.get(zipcode) != null)
		    	zipcodeAgent.setNumber(vehiclesWithinZipcode.get(zipcode));
		    zipcodes.put(fixedGeog, geom);
		    context.add(fixedGeog);
		    geography.move(fixedGeog, geom);
		}		
	};
	

	public Logger getLOGGER() {
		return LOGGER;
	}

	public void setLOGGER(Logger lOGGER) {
		this.LOGGER = lOGGER;
	}
	
	public ISchedule getSchedule() {
		return schedule;
	}

	public void setSchedule(ISchedule schedule) {
		this.schedule = schedule;
	}

	public LocalDateTime getRealTime() {
		return realDateTime;
	}

	public void setRealTime(LocalDateTime realDateTime) {
		this.realDateTime = realDateTime;
	}

	public Context<Object> getContext() {
		return context;
	}

	public void setContext(Context<Object> context) {
		this.context = context;
	}

	public Network<Object> getRoadNetwork() {
		return roadNetwork;
	}

	public void setRoadNetwork(Network<Object> roadNetwork) {
		this.roadNetwork = roadNetwork;
	}

	public Geography<Object> getGeography() {
		return geography;
	}

	public void setMainGeography(Geography<Object> mainGeography) {
		this.geography = mainGeography;
	}
	
	public HashMap<String, Traveler> getPersons() {
		return persons;
	}

	public void setPersons(HashMap<String, Traveler> persons) {
		this.persons = persons;
	}

	public HashMap<String, Model> getModels() {
		return models;
	}

	public void setModels(HashMap<String, Model> models) {
		this.models = models;
	}

	public HashMap<String, FuelingStation> getFuelingStations() {
		return fuelingStations;
	}

	public void setFuelingStations(HashMap<String, FuelingStation> fuelingStations) {
		this.fuelingStations = fuelingStations;
	}

	public static TeamShortestPath<Object> getShortestPath() {
		return shortestPath;
	}

	public void setShortestPath(TeamShortestPath<Object> shortestPath) {
		BackgroundEngine.shortestPath = shortestPath;
	}
}
