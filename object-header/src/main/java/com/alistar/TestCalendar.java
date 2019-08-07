package com.alistar;

import java.util.Calendar;

public class TestCalendar {
	public static void main(String[] args) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR_OF_DAY, -6);
		System.out.println(calendar.getTime());
		Calendar calendar1 = Calendar.getInstance();
		calendar1.add(Calendar.HOUR_OF_DAY, -7);
		System.out.println(calendar1.getTime());
		System.out.println(calendar1.getTime().before(calendar.getTime()));
	}
}
