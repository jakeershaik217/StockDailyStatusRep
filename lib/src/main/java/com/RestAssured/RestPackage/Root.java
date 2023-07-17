package com.RestAssured.RestPackage;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Root {

	 @JsonProperty("MyArray") 
	    public ArrayList<JSONObjectCalls> jsonObjectCalls;
}
