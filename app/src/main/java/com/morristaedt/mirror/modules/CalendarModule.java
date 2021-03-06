package com.morristaedt.mirror.modules;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by alex on 12/09/15.
 *
 */
public class CalendarModule {
    /** CalendarListener is called by android framework to bring up the Calender view
     *
     */
    public interface CalendarListener {
        void onCalendarUpdate(String title, String details);
    }
    /**
     The getCalendarEvents method creates the UI thread for the calender.
     @param context Context class gives access to global information
     @param calendarListener
     */
    public static void getCalendarEvents(final Context context, final CalendarListener calendarListener) {
        new AsyncTask<Void, Void, Void>() {
            String title = null;
            String details = null;

            @Override
            protected void onPostExecute(Void aVoid) {
                calendarListener.onCalendarUpdate(title, details);
            }

            @Override
                protected Void doInBackground(Void... params) { //Obtains the calender entries from Android calender application
                Cursor cursor;
                ContentResolver contentResolver = context.getContentResolver();
                final String[] colsToQuery = new String[]{
                        CalendarContract.EventsEntity.TITLE,
                        CalendarContract.EventsEntity.DTSTART,
                        CalendarContract.EventsEntity.DTEND,
                        CalendarContract.EventsEntity.EVENT_LOCATION};

                Calendar now = Calendar.getInstance();
                SimpleDateFormat startFormat = new SimpleDateFormat("dd/MM/yy", Locale.US);
                String dateString = startFormat.format(now.getTime());
                long start = now.getTimeInMillis();

                SimpleDateFormat endFormat = new SimpleDateFormat("hh:mm:ss dd/MM/yy", Locale.US);
                Calendar endOfDay = Calendar.getInstance();
                Date endofDayDate;
                try {
                    endofDayDate = endFormat.parse("23:59:59 " + dateString);
                    endOfDay.setTime(endofDayDate);
                } catch (ParseException e) {
                    Log.e("CalendarModule", e.toString());
                }


                cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI, colsToQuery,
                        "( dtstart >" + start + ") and (dtend  <" + endOfDay.getTimeInMillis() + ")",
                        null, "dtstart ASC");

                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        title = cursor.getString(0);
                        Calendar startTime = Calendar.getInstance();
                        startTime.setTimeInMillis(cursor.getLong(1));
                        Calendar endTime = Calendar.getInstance();
                        endTime.setTimeInMillis(cursor.getLong(2));
                        DateFormat formatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
                        details = formatter.format(startTime.getTime()) + " - " + formatter.format(endTime.getTime());
                        if (!TextUtils.isEmpty(cursor.getString(3))) {
                            details += " ~ " + cursor.getString(3);
                        }
                    }

                    cursor.close();
                }
                return null;
            }
        }.execute();
    }
}
