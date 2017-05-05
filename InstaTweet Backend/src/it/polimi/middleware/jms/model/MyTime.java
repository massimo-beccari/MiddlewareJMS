package it.polimi.middleware.jms.model;

import java.util.Calendar;
import java.util.Scanner;

public class MyTime {
	private int day;
	private int month;
	private int year;
	private int hours;
	private int minutes;
	private int seconds;
	private int millis;
	private long time;
	
	public MyTime(String time) throws Exception {
		Scanner sc = new Scanner(time);
		String s;
		sc.useDelimiter(":");
		//scan date
		s = sc.next();
		day = Integer.parseInt(s);
		if(day < 1 || day > 31) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		month = Integer.parseInt(s);
		if(month < 1 || month > 12) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		year = Integer.parseInt(s);
		if(year < 1970) {
			sc.close();
			throw new Exception();
		}
		//scan time
		s = sc.next();
		hours = Integer.parseInt(s);
		if(hours < 0) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		minutes = Integer.parseInt(s);
		if(minutes < 0 || minutes > 59) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		seconds = Integer.parseInt(s);
		if(seconds < 0 || seconds > 59) {
			sc.close();
			throw new Exception();
		}
		s = sc.next();
		millis = Integer.parseInt(s);
		if(millis < 0 || millis > 999) {
			sc.close();
			throw new Exception();
		}
		sc.close();
		Calendar cal = Calendar.getInstance();
		cal.set(year, month-1, day, hours, minutes, seconds);
		this.time = cal.getTimeInMillis() + millis;
	}
	
	public MyTime(long time) {
		this.time = time;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		day = cal.get(Calendar.DAY_OF_MONTH);
		month = cal.get(Calendar.MONTH) + 1;
		year = cal.get(Calendar.YEAR);
		hours = cal.get(Calendar.HOUR_OF_DAY);
		minutes = cal.get(Calendar.MINUTE);
		seconds = cal.get(Calendar.SECOND);
		millis = 0;
	}

	public long getHours() {
		return hours;
	}

	public int getMinutes() {
		return minutes;
	}

	public int getSeconds() {
		return seconds;
	}

	public int getMillis() {
		return millis;
	}
	
	public long getTime() {
		return time;
	}
	
	@Override
	public String toString() {
		String string = year + "/" + month + "/" + day + "-" + hours + ":" + minutes + ":" + seconds/* + ":" + millis*/;
		return string;
	}
	
	/* test main
	public static void main(String[] args) {
		ActionTime t = new ActionTime(41729512);
		System.out.println(t.toString());
		try {
			ActionTime t2 = new ActionTime(0,0,30,600);
			System.out.println(t2.toString());
			ActionTime t3 = t.sub(t2);
			System.out.println(t3.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
}
