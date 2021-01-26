package team.geography.boundary;

import java.io.Serializable;

import team.geography.FixedGeography;

public class Zipcode extends FixedGeography implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4217101290075982776L;
	private String zipcode;
	private int number;	// initial vehicle number
	public String getZipcode() {
		return zipcode;
	}
	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	@Override
	public String toString() {
		return "Zipcode [zipcode=" + zipcode + ", number=" + number + "]";
	}
	
	
}

