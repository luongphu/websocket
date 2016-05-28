package entity;

import java.util.ArrayList;

public class WsResponse {
	private int action;
	private ArrayList<String> data;
	
	public int getAction() {
		return action;
	}
	public void setAction(int action) {
		this.action = action;
	}
	public ArrayList<String> getData() {
		return data;
	}
	public void setData(ArrayList<String> data) {
		this.data = data;
	}
}
