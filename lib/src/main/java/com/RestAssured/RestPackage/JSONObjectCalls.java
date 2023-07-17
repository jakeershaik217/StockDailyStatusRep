package com.RestAssured.RestPackage;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JSONObjectCalls {
	
	public String getNewsid() {
		return Newsid;
	}
	public void setNewsid(String newsid) {
		Newsid = newsid;
	}
	public String getNewsSubj() {
		return NewsSubj;
	}
	public void setNewsSubj(String newsSubj) {
		NewsSubj = newsSubj;
	}
	public String getNewsdt() {
		return Newsdt;
	}
	public void setNewsdt(String newsdt) {
		Newsdt = newsdt;
	}
	
	JSONObjectCalls(@JsonProperty("Newsid")String Newsid,@JsonProperty("NewsSubj")String NewsSubj,@JsonProperty("Newsdt")String Newsdt) {
		
		this.Newsid=Newsid;
		this.NewsSubj=NewsSubj;
		this.Newsdt=Newsdt;
		
	}
	String Newsid;
	String NewsSubj;
	String Newsdt;

}
