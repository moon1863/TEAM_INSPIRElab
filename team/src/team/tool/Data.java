package team.tool;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings("serial")
public class Data implements Cloneable, Serializable{
    static final int basicUnitKinds =5;
    static public enum Unit {
    	// basic Unit
        METER(1.0, 0), KILOMETER(Unit.METER, 1000), MILE(Unit.METER, 1609.34),
        KILOWATTHOUR(1.0, 1), JOULE(Unit.KILOWATTHOUR, 1.0/3600000.0),
        SECOND(1.0, 2), MINUTE(Unit.SECOND, 60.0), HOUR(Unit.SECOND, 3600.0), DAY(Unit.SECOND, 86400.0),
        LITER(1.0, 3), GALLON(Unit.LITER, 3.78541178),
        KILOGRAM(1.0, 4), TONNE(Unit.KILOGRAM, 1000.0), POUND(Unit.KILOGRAM, 0.45359237),
        // composite Unit
        // driving speed
        MPH(Unit.MILE, "/", Unit.HOUR),
        // power charging speed
        KWHPM(Unit.KILOWATTHOUR, "/", Unit.MINUTE),
        // liquid charging speed
        GALPM(Unit.GALLON, "/", Unit.MINUTE),
        //  fuel efficiency
    	MPG(Unit.MILE, "/", Unit.GALLON),
    	MPKWH(Unit.MILE, "/", Unit.KILOWATTHOUR);
    	
        private double num;
        private int[] unitExp;

        private Unit(double num, int index) {
            this.num = num;
            this.unitExp = new int[basicUnitKinds];
            this.unitExp[index] = 1;
        }

        private Unit(Unit basicUnit, double multiple) {
            this.num = basicUnit.num * multiple;
            this.unitExp = Arrays.copyOf(basicUnit.unitExp, basicUnit.unitExp.length);
        }

        private Unit(Unit unit0, String operator0, Unit unit1) {
            this.unitExp = new int[basicUnitKinds];
            switch (operator0) {
                case "/":
                    for(int i = 0; i < unitExp.length; i++)
                        this.unitExp[i] = unit0.unitExp[i] - unit1.unitExp[i];
                    this.num = unit0.num / unit1.num;
                    break;
                case "*":
                    for(int i = 0; i < unitExp.length; i++)
                        this.unitExp[i] = unit0.unitExp[i] + unit1.unitExp[i];
                    this.num = unit0.num + unit1.num;
                    break;
                default:
                    break;
            }
        }

        public Unit multiply(Unit unit) {
            int[] resUnitExp = new int[basicUnitKinds];
            for(int i = 0; i < unitExp.length; i++) resUnitExp[i] = unitExp[i] + unit.unitExp[i];
            Optional<Unit> resUnit = Arrays.stream(Unit.values()).
                    filter(v -> v.num == num * unit.num && Arrays.equals(v.unitExp, resUnitExp)).findFirst();
            if (resUnit.isPresent()) return resUnit.get();
            else throw new IllegalArgumentException(this.name() + "could not multiply " + unit.name());
        }

        public Unit divide(Unit unit) {
            int[] resUnitExp = new int[basicUnitKinds];
            for(int i = 0; i < unitExp.length; i++) 
            	resUnitExp[i] = unitExp[i] - unit.unitExp[i];
            Optional<Unit> resUnit = Arrays.stream(Unit.values()).
                    filter(v -> v.num == num / unit.num && Arrays.equals(v.unitExp, resUnitExp)).findFirst();
            if (resUnit.isPresent()) return resUnit.get();
            else {
            	System.out.println("this is " + this + "unit is " + unit);
            	throw new IllegalArgumentException(this.name() + "could not divide " + unit.name());
            }
        }

        boolean isSameGroupUnit(Unit unit) {
            return Arrays.equals(this.unitExp, unit.unitExp); 
        }
        

    	@Override
    	public String toString() {
    		StringBuilder builder = new StringBuilder();
    		builder.append("name is " + this.name() + "num is " + num + " unitExp is " + unitExp);
    		return builder.toString();
    	}
    }

    public double num;
    public Unit unit;
    public Data(double num, Unit unit) {
        this.num = num;
        this.unit = unit;
    }
    
    public static Unit unitOf(String unitString) {
    	return Enum.valueOf(Unit.class, unitString);
    }
    // transfer procedure
    public Data transfer(Unit unit) {
        if (this.unit.isSameGroupUnit(unit)) return new Data(this.num * this.unit.num / unit.num, unit);
        else throw new IllegalArgumentException("units do not match");
    }

    // plus
    public Data plus(Data data) {
        if (!this.unit.isSameGroupUnit(data.unit))
            throw new IllegalArgumentException("For plus or minus, units do not match");
        else
            return new Data(this.num + data.num, this.unit);
    }

    // minus
    public Data minus(Data data) {
    	Data tempData = (Data) data.clone();
    	tempData.num = tempData.num * (-1);
        return this.plus(tempData);
    }

    public Data multiply(Data data) {
        return new Data(num * data.num, unit.multiply(data.unit));
    }

    public Data divide(Data data) {
    	if(this.unit.isSameGroupUnit(data.unit)) return new Data(num / data.transfer(unit).num, null);
        return new Data(num / data.num, unit.divide(data.unit));
    }
    
    public boolean bigger(Data data) {
    	Data resData = this.minus(data);
    	return resData.num > 0;
    }
    
    public boolean smaller(Data data) {
    	Data resData = this.minus(data);
    	return resData.num < 0;
    }
    
    public Duration transferToDuration() {
    	if(unit.isSameGroupUnit(Unit.SECOND)) new IllegalArgumentException("don't match!");
    	return Duration.ofSeconds((long) this.transfer(Unit.SECOND).num);
    }
    
    static public Data durationTransferToData(Duration duration) {
    	return new Data(duration.getSeconds(), Unit.SECOND);
    }
    @Override
    public boolean equals(Object data) {
    	if(data instanceof  Data && this.minus((Data) data).num == 0) return true;
    	else return false;
    }
    
	@Override
	public Object clone(){
		Object newData = null;
		try {
			newData = super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newData;
	}
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        StringBuilder builder = new StringBuilder();
        builder.append(num);
        builder.append(" ");
        builder.append(unit.name().toLowerCase());
        return builder.toString();
    }
}
