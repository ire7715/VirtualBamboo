package com.sum.virtualbamboo;

public class cursorAction {
	int action, x, y;
	
	public cursorAction(){
		action = -1;
		x = y = 0;
	}
	
	public cursorAction(int action, int x, int y){
		this.action = action;
		this.x = x;
		this.y = y;
	}
	
	public cursorAction(int action, float x, float y){
		this.action = action;
		this.x = (int)x;
		this.y = (int)y;
	}
}
