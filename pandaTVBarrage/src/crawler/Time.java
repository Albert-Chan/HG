package crawler;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Time implements Comparable<Time>, Serializable {
	private static final long serialVersionUID = 8339895644550649179L;

	private GregorianCalendar calendar;
	private SimpleDateFormat sdf = new SimpleDateFormat();

	public Time(String time, String pattern) throws ParseException {
		sdf.applyPattern(pattern);
		Date date = sdf.parse(time);
		this.calendar = new GregorianCalendar();
		calendar.setTimeInMillis(date.getTime());
	}

	public Time(long timestamp) {
		// TODO check the performance with DateFormat -> parseInt.
		this.calendar = new GregorianCalendar();
		calendar.setTimeInMillis(timestamp);
	}

	public Time(Time t) {
		this(t.getTimestamp());
	}

	/**
	 * Formats the current time to the given pattern.
	 * @param pattern the format pattern
	 * @return
	 */
	public String format(String pattern) {
		sdf.applyPattern(pattern);
		return sdf.format(calendar.getTimeInMillis());
	}

	/**
	 * Gets the timestamp of this time.
	 * @return the timestamp of this time.
	 */
	public long getTimestamp() {
		return calendar.getTimeInMillis();
	}

	/**
	 * Gets the yyyyMMdd as a number.
	 * 
	 * @return yyyyMMdd
	 */
	public int getDay() {
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;
		int day = calendar.get(Calendar.DATE);
		int yyyyMMdd = year * 10000 + month * 100 + day;
		return yyyyMMdd;
	}

	/**
	 * Gets the yyyyMMddHH as a number.
	 * 
	 * @return yyyyMMddHH
	 */
	public long getTimeToHour() {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		return getDay() * 100L + hour;
	}

	/**
	 * Gets the yyyyMMddHHmm as a number.
	 * 
	 * @return yyyyMMddHHmm
	 */
	public long getTimeToMinute() {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		return getDay() * 10000L + hour * 100 + minute;
	}

	/**
	 * Gets the time index with the interval of 15 minutes, indexed from 1.
	 * 
	 * @return the time index
	 */
	public int getTimeIndex() {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int timeIndex = (hour * 60 + minute) / 15 + 1;
		return timeIndex;
	}

	/**
	 * Rounds to the beginning of the day.
	 * 
	 * @return the timestamp at the beginning of the day.
	 */
	public long roundToDayLeftEdge() {
		GregorianCalendar c = new GregorianCalendar();
		c.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTimeInMillis();
	}

	/**
	 * Rounds to the timestamp of the time index's left edge.
	 * 
	 * @return the timestamp of the time index's left edge.
	 */
	public long roundToTimeIndexLeftEdge() {
		int timeIndex = getTimeIndex();
		return roundToDayLeftEdge() + (timeIndex - 1) * 15 * 60 * 1000L;
	}

	/**
	 * Rounds to the timestamp of the time index's right edge.
	 * 
	 * @return the timestamp of the time index's right edge.
	 */
	public long roundToTimeIndexRightEdge() {
		int timeIndex = getTimeIndex();
		return roundToDayLeftEdge() + timeIndex * 15 * 60 * 1000L;
	}

	/**
	 * Gets the number of days from anotherDay to this.calendar
	 */
	public int getDaysDiff(Time anotherDay) {
		int yearDiff = this.calendar.get(Calendar.YEAR) - anotherDay.calendar.get(Calendar.YEAR);
		if (yearDiff == 0) {
			return this.calendar.get(Calendar.DAY_OF_YEAR) - anotherDay.calendar.get(Calendar.DAY_OF_YEAR);
		} else if (yearDiff < 0) {
			return this.calendar.getActualMaximum(Calendar.DAY_OF_YEAR) - this.calendar.get(Calendar.DAY_OF_YEAR)
					+ anotherDay.calendar.get(Calendar.DAY_OF_YEAR)
					- anotherDay.calendar.getActualMinimum(Calendar.DAY_OF_YEAR);
		} else {
			return anotherDay.calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
					- anotherDay.calendar.get(Calendar.DAY_OF_YEAR) + this.calendar.get(Calendar.DAY_OF_YEAR)
					- this.calendar.getActualMinimum(Calendar.DAY_OF_YEAR);
		}
	}

	/**
	 * Gets the max day of month.
	 * 
	 * @return
	 */
	public int getMaxDayOfMonth() {
		return this.calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Gets the seconds from 00:00:00 of this day to the time in this.calendar.
	 * 
	 * @return the seconds lapsed in this day.
	 */
	public int getSecondsInDay() {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		int secs = (hour * 60 + minute) * 60 + second;
		return secs;
	}

	/**
	 * Gets the day of the week.
	 * 
	 * @return 1 : SUNDAY; 2 : MONDAY; 3 : TUESDAY; 4 : WEDNESDAY; 5 : THURSDAY;
	 *         6 : FRIDAY; 7 : SATURDAY
	 */
	public int getDayOfWeek() {
		return calendar.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * Checks if the current time is in a working day or not.
	 */
	public boolean isWorkingDay() {
		if (getDayOfWeek() >= 2 && getDayOfWeek() <= 6) {
			return true;
		}
		return false;
	}

	/**
	 * Adds amount of minutes to the current time.
	 */
	public Time addMinutes(int amount) {
		calendar.add(GregorianCalendar.MINUTE, amount);
		return this;
	}

	/**
	 * Adds amount of hours to the current time.
	 */
	public Time addHours(int amount) {
		calendar.add(GregorianCalendar.HOUR_OF_DAY, amount);
		return this;
	}

	/**
	 * Adds amount of days to the current time.
	 */
	public Time addDays(int amount) {
		calendar.add(GregorianCalendar.DATE, amount);
		return this;
	}

	public String[] computeDateInWeeks(int desiredWeeks, String datePattern) {
		if (desiredWeeks <= 0) {
			return new String[0];
		}
		long timestamp = calendar.getTimeInMillis();
		String[] dateArray = new String[desiredWeeks];
		SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

		dateArray[desiredWeeks - 1] = sdf.format(timestamp);
		desiredWeeks--;
		while (desiredWeeks > 0) {
			timestamp -= 1000 * 60 * 60 * 24 * 7;
			Time timeIndex = new Time(timestamp);
			if ((isWorkingDay() && timeIndex.isWorkingDay()) || (!isWorkingDay() && !timeIndex.isWorkingDay())) {
				sdf.format(timestamp);
				dateArray[desiredWeeks - 1] = sdf.format(timestamp);
				desiredWeeks--;
			}
		}
		return dateArray;
	}

	/**
	 * Compute the desired number of dates which is before the timestamp,
	 * differentiate between working day and non-working day
	 * 
	 * @param timestamp
	 *            : till which date the computation ends
	 * @param desiredDays
	 *            : number of days to compute
	 * @param datePattern
	 *            : the date pattern for output
	 * @return an array of date which satisfy the datePattern
	 */
	public String[] computeDateInDays(int desiredDays, String datePattern) {
		if (desiredDays <= 0) {
			return new String[0];
		}
		long timestamp = calendar.getTimeInMillis();
		String[] dateArray = new String[desiredDays];
		SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

		dateArray[desiredDays - 1] = sdf.format(timestamp);
		desiredDays--;
		while (desiredDays > 0) {
			timestamp -= 1000 * 60 * 60 * 24;
			Time timeIndex = new Time(timestamp);
			if ((isWorkingDay() && timeIndex.isWorkingDay()) || (!isWorkingDay() && !timeIndex.isWorkingDay())) {
				sdf.format(timestamp);
				dateArray[desiredDays - 1] = sdf.format(timestamp);
				desiredDays--;
			}
		}
		return dateArray;
	}

	public String[] computeDateInDays(int desiredDays, String datePattern, boolean ignoreWorkingDay) {
		if (desiredDays <= 0) {
			return new String[0];
		}
		long timestamp = calendar.getTimeInMillis();
		String[] dateArray = new String[desiredDays];
		SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

		dateArray[desiredDays - 1] = sdf.format(timestamp);
		desiredDays--;
		while (desiredDays > 0) {
			timestamp -= 1000 * 60 * 60 * 24;
			Time timeIndex = new Time(timestamp);
			if (ignoreWorkingDay || (isWorkingDay() && timeIndex.isWorkingDay())
					|| (!isWorkingDay() && !timeIndex.isWorkingDay())) {
				sdf.format(timestamp);
				dateArray[desiredDays - 1] = sdf.format(timestamp);
				desiredDays--;
			}
		}
		return dateArray;
	}

	@Override
	public int compareTo(Time o) {
		return this.calendar.compareTo(o.calendar);
	}

}
