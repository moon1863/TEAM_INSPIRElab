package team.geography;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationNotFoundException;

import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import simphony.util.messages.MessageCenter;
import team.tool.FeatureProcess;

/**
 * Creates and sets agents properties from a features in shapefile.
 *
 * @author Nick Collier
 * @author Chuang Hou
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class TeamShapefileLoader<T> {

  private static final MessageCenter msg = MessageCenter.getMessageCenter(TeamShapefileLoader.class);
  private static Map<Class, Class> primToObject = new HashMap<Class, Class>();

  static {
    primToObject.put(int.class, Integer.class);
    primToObject.put(long.class, Long.class);
    primToObject.put(double.class, Double.class);
    primToObject.put(float.class, Float.class);
    primToObject.put(boolean.class, Boolean.class);
    primToObject.put(byte.class, Byte.class);
    primToObject.put(char.class, Character.class);
  }

  private Geography geography;
  private Context context;
  private Map<String, Method> attributeMethodMap = new HashMap<String, Method>();
  private Class agentClass;

  private Iterator<SimpleFeature>featureIterator;
  private FeatureProcess<? super T> featureProcess;
  private MathTransform transform;
  
  /**
   * Creates a shapefile loader for agents of the specified
   * class and whose data source is the specified shapefile. The
   * agents will be placed into the specified Geography according to
   * the geometry specified in the shapefile and transformed according
   * to the geography's CRS.
   *
   * @param clazz     the agent class
   * @param shapefile the shapefile that serves as the datasource for the agent
   *                  properties
   * @param geography the geography to hold spatial locations of the agents
   * @param context   the context to add the agents to
   */
  public TeamShapefileLoader(Class<T> clazz, URL shapefile, Geography geography, Context context, FeatureProcess<? super T> featureProcess) {
    this.geography = geography;
    this.agentClass = clazz;
    this.context = context;
    this.featureProcess = featureProcess;
    
    ShapefileDataStore store = null;
    SimpleFeatureIterator iter = null;
    try {
      BeanInfo info = Introspector.getBeanInfo(clazz, Object.class);
      Map<String, Method> methodMap = new HashMap<String, Method>();
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (PropertyDescriptor pd : pds) {
        if (pd.getWriteMethod() != null) {
          methodMap.put(pd.getName().toLowerCase(), pd.getWriteMethod());
        }
      }

      store = new ShapefileDataStore(shapefile);
      SimpleFeatureType schema = store.getSchema(store.getTypeNames()[0]);
      
      // First attribute at index 0 is always the Geometry
      AttributeType type = schema.getType(0);
      String name = type.getName().getLocalPart();
      initTransform(geography, type);
      
      // Loop over remaining type attributes
      for (int i = 1, n = schema.getAttributeCount(); i < n; i++) {
        type = schema.getType(i);
        name = type.getName().getLocalPart();
        
        Method method = methodMap.get(name.toLowerCase());
        if (method == null) method = methodMap.get(name.replace("_", "").toLowerCase());
        if (method != null && isCompatible(method.getParameterTypes()[0], (type.getBinding()))) {
            attributeMethodMap.put(name, method);  
        }
      }
      iter = store.getFeatureSource().getFeatures().features();

      List<SimpleFeature> features = new ArrayList<SimpleFeature>();
    	while(iter.hasNext()){
				features.add(iter.next());
			}
    	featureIterator = features.iterator();
      
    } catch (IntrospectionException ex) {
      msg.error("Error while introspecting class", ex);
    } catch (IOException e) {
      msg.error(String.format("Error opening shapefile '%S'", shapefile), e);
    } catch (FactoryException e) {
      msg.error(String.format("Error creating transform between shapefile CRS and Geography CRS"), e);
    }
    finally{
			iter.close();
			store.dispose();
		}
  }

  private boolean isCompatible(Class methodParam, Class attributeType) {
    if (methodParam.equals(attributeType)) return true;
    Class clazz = primToObject.get(methodParam);
    if (clazz != null) return clazz.equals(attributeType);
    return false;
  }

  private void initTransform(Geography geography, AttributeType type) throws FactoryException {
    GeometryType gType = (GeometryType) type;
    if (geography != null) {
      try {
        setTransform(ReferencingFactoryFinder.getCoordinateOperationFactory(null).createOperation(
        		gType.getCoordinateReferenceSystem(),geography.getCRS()).getMathTransform());
        
      } catch (OperationNotFoundException ex) {
        // bursa wolf params may be missing so try lenient.
        setTransform(CRS.findMathTransform(gType.getCoordinateReferenceSystem(), 
        		geography.getCRS(), true));
      }
    }
  }

  /**
   * Creates all the agents for the shapefile features,
   * setting each agent's properteis to the value of a
   * feature's relevant attributes.
   */
  public void load() {
    while (hasNext()) next();
  }

  /**
   * Creates the next agent from the next feature
   * in the shapefile, setting that agent's properties to the value of
   * that feature's relevant attributes.
   *
   * @return the created agent
   */
@SuppressWarnings("deprecation")
public T next() {
    T obj = null;
    try {
    	SimpleFeature feature = featureIterator.next();
        obj = (T) agentClass.newInstance();
        obj = fillAgent(feature, obj);
    	featureProcess.apply(obj, feature);
    } catch (Exception e) {
      msg.error("Exption", e);
    } 
    return obj;
  }

  @SuppressWarnings("unused")
private Constructor findConstructor(Object[] constructorArgs) {
    Class[] args = new Class[constructorArgs.length];
    int i = 0;
    for (Object obj : constructorArgs) {
      args[i++] = obj.getClass();
    }

    for (Constructor constructor : agentClass.getConstructors()) {
      Class[] params = constructor.getParameterTypes();
      if (params.length == args.length) {
        boolean pass = true;
        for (i = 0; i < args.length; i++) {
          if (!isCompatible(params[i], args[i])) {
            pass = false;
            break;
          }
        }
        if (pass) return constructor;
      }
    }

    return null;
  }

  /**
   * Returns true if there are more features left to process,
   * otherwise false.
   *
   * @return true if there are more features left to process,
   *         otherwise false.
   */
  public boolean hasNext() {
    return featureIterator.hasNext();
  }

  private T fillAgent(SimpleFeature feature, T agent) throws IllegalAccessException, InvocationTargetException {
    for (String attribName : attributeMethodMap.keySet()) {
      Object val = feature.getAttribute(attribName);
      Method write = attributeMethodMap.get(attribName);
      write.invoke(agent, val);
    }
    return agent;
  }

public MathTransform getTransform() {
	return transform;
}

public void setTransform(MathTransform transform) {
	this.transform = transform;
}

public Context getContext() {
	return context;
}

public Geography getGeography() {
	return geography;
}
}