package com.nycatech.plugins;
 
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.os.Environment;

public class MessageList extends CordovaPlugin {
    public static final String ACTION_ECHO = "echo";
    public static final String ACTION_LIST = "list";
    public static final String ACTION_SAVE = "save";
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
    
	private static class Message implements Comparable<Message> {
		long date;
		String text;
		
		public int compareTo(Message o) {
			if (date<o.date) return -1;
			if (date>o.date) return 1;
			return 0;
		}
	}
	
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            if (ACTION_ECHO.equals(action)) { 
                String message = args.getString(0);
                this.echo(message, callbackContext);
                return true;
            } else if (ACTION_LIST.equals(action)) {
				this.conversationList(callbackContext);
				return true;
            } else if (ACTION_SAVE.equals(action)) {
				this.saveConversations(callbackContext);
				return true;			
			}
            callbackContext.error("Invalid action:"+action);
            return false;
        } catch(Exception e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        } 
    }
	
	
	private void echo(String message, CallbackContext callbackContext) {
		if (message != null && message.length() > 0) {
			callbackContext.success(message);
		} else {
			callbackContext.error("Expected one non-empty string argument.");
		}
	}
	
	private void conversationList(CallbackContext callbackContext) throws JSONException {
		ContentResolver contentResolver = cordova.getActivity().getContentResolver();
		final String[] projection = new String[]{"*"};
		Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
		
		JSONArray results = new JSONArray();
 		Cursor cursor = contentResolver.query(uri, projection, null, null, null);
		try {
			while (cursor.moveToNext()) {
				String current = cursor.getString(cursor.getColumnIndex("_id")) + ": " +
				cursor.getString(cursor.getColumnIndex("display_recipient_ids"));

				results.put(current);
			}
		} finally {
			cursor.close();
		}
		
		callbackContext.success(results); 
	}
	
	private void saveConversations(CallbackContext callbackContext) throws Exception {
		File newFolder = new File(Environment.getExternalStorageDirectory(), "SavedTexts");
		if (!newFolder.exists()) {
			newFolder.mkdir();
		}
		String filename = "SavedTexts/texts.html";
		File myFile = new File(Environment
            .getExternalStorageDirectory(), filename);
		if (myFile.exists())
			myFile.delete();
		FileWriter writer = new FileWriter(myFile);
		
		Map<String,String> idToWho = new HashMap<String,String>();
		
		try {
			writer.write("<!DOCTYPE html>\n");
			writer.write("<html lang=\"en\">");
			writer.write("<head>\n");
			writer.write("  <meta charset=\"utf-8\">\n");
			writer.write("  <title>Saved Texts</title>\n");	
			writer.write("  <style>\n");
			writer.write("  	.when { color: blue; margin-right: 20px; }\n");
			writer.write("  	.from { color: maroon; margin-right: 20px; }\n");
			writer.write("  	.text { color: black; }\n");
			writer.write("  	.sms_id { display: none }\n");
			writer.write("  	.mms_id { display: none}\n");
			writer.write("    </style>\n");
			writer.write("</head>\n");
			writer.write("<body>\n");
			
			ContentResolver contentResolver = cordova.getActivity().getContentResolver();
			final String[] projection = new String[]{"*"};
			Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
			Cursor cursor = contentResolver.query(uri, projection, null, null, null);
			List<Message> convMessages = new ArrayList<Message>();
			try {
				while (cursor.moveToNext()) {
					writer.write("  <h1>");
					
					String recip = cursor.getString(cursor.getColumnIndex("display_recipient_ids"));
					String[] ids = recip.split(" ");
					
					StringBuilder sb = new StringBuilder();
					String conversationId = cursor.getString(cursor.getColumnIndex("_id"));
					sb.append(conversationId);
					sb.append(":");
					for (String s : ids)  {
						sb.append(" ");
						sb.append(whoIs(s, idToWho));
					}
					writer.write(sb.toString());
					writer.write("</h1>\n");
			
					convMessages.clear();
					convMessages.addAll(recordSmsConversation(conversationId));
					convMessages.addAll(recordMmsConversation(conversationId));
					Collections.sort(convMessages);
					for (Message m : convMessages) {
						writer.write(m.text);
					}
					
				}

				writer.write("</body>\n");
				writer.write("</html>\n");
			} finally {
				cursor.close();
			}
			callbackContext.success("Created "+filename); 
		} finally {
			writer.close();
		}
	}

	private List<Message> recordSmsConversation(String convId) throws Exception {
		ContentResolver contentResolver = cordova.getActivity().getContentResolver();
		final String[] projection = new String[]{"_id","address","body","type","date"};
		final String selection = "thread_id="+convId;
		final String order = "date ASC";
		Uri uri = Uri.parse("content://sms");
		Cursor cursor = contentResolver.query(uri, projection, selection, null, order);
		List<Message> convMessages = new ArrayList<Message>();

		try { 
			while (cursor.moveToNext()) {
				long id = cursor.getLong(cursor.getColumnIndex("_id"));
				String address = cursor.getString(cursor.getColumnIndex("address"));
				String body = cursor.getString(cursor.getColumnIndex("body"));
				int type = cursor.getInt(cursor.getColumnIndex("type"));			
				long date = cursor.getLong(cursor.getColumnIndex("date"));
				String dateString = dateFormat.format(new Date(date));
				boolean fromMe = (type==2);
				StringBuilder sb = new StringBuilder();
				sb.append("<div id=\"msg\">");
				String who = fromMe ? "me" : address;
				sb.append("<span class=\"when\">");
				sb.append(dateString);
				sb.append("</span>");
				sb.append("<span class=\"from\">");
				sb.append(who);
				sb.append("</span>");
				sb.append("<span class=\"sms_id\">");
				sb.append(id);
				sb.append("</span>");
				sb.append("<span class=\"text\">");
				sb.append(body);
				sb.append("</span>");
				sb.append("</div>\n");
				Message m = new Message();
				m.date = date;
				m.text = sb.toString();
				convMessages.add(m);
			}
		} finally {
			cursor.close();
		}
		
		return convMessages;
	}

	private List<Message> recordMmsConversation(String convId) throws Exception {
		ContentResolver contentResolver = cordova.getActivity().getContentResolver();
		final String[] projection = new String[]{"_id","date"};
		final String selection = "thread_id="+convId;
		final String order = "date ASC";
		Uri uri = Uri.parse("content://mms");
		Cursor cursor = contentResolver.query(uri, projection, selection, null, order);
		List<Message> convMessages = new ArrayList<Message>();

		try {
			while (cursor.moveToNext()) {
				long m_id = cursor.getLong(cursor.getColumnIndex("_id"));
				// MMS dates need to be multiplied by 1000 to be correct apparently
				long date = cursor.getLong(cursor.getColumnIndex("date")) * 1000;
				String dateString = dateFormat.format(new Date(date));

				StringBuilder sb = new StringBuilder();
				sb.append("<div id=\"msg\">");
				sb.append("<span class=\"when\">");
				sb.append(dateString);
				sb.append("</span>");

				recordMmsSender(m_id, sb);
				recordMmsParts(m_id, sb);
				sb.append("</div>\n");

				Message m = new Message();
				m.date = date;
				m.text = sb.toString();
				convMessages.add(m);

			}
		} finally {
			cursor.close();
		}
		
		return convMessages;
	}

	private void recordMmsParts(long mid, StringBuilder result) throws Exception {
		ContentResolver contentResolver = cordova.getActivity().getContentResolver();
		final String[] projection = new String[]{"seq","ct","text"};
		final String selection = String.format("mid=%d",mid);
		final String order = "seq ASC";
		Uri uri = Uri.parse("content://mms/part");
		Cursor cursor = contentResolver.query(uri, projection, selection, null, order);
		try {
			StringBuilder sb = new StringBuilder();
			while (cursor.moveToNext()) {
				String body = cursor.getString(cursor.getColumnIndex("text"));
				String type = cursor.getString(cursor.getColumnIndex("ct"));			
				if ("text/plain".equals(type)) {
					sb.append(body);
				} else if ("application/smil".equals(type)) {
					// Skip
				} else {
					sb.append("/* ");
					sb.append(type);
					sb.append(" here */");
				}
			}
			if (sb.length()>0) {
				result.append("<span class=\"mms_id\">");
				result.append(Long.toString(mid));
				result.append("</span>");
				result.append("<span class=\"mytext\">");
				result.append(sb);
				result.append("</span>");
			}
		} finally {
			cursor.close();
		}
	}

	private void recordMmsSender(long mid, StringBuilder result) throws Exception {
		ContentResolver contentResolver = cordova.getActivity().getContentResolver();
		final String[] projection = new String[]{"address","type"};
		Uri uri = Uri.parse(String.format("content://mms/%d/addr",mid));
		Cursor cursor = contentResolver.query(uri, projection, null, null, null);
		try {
			StringBuilder sb = new StringBuilder();
			while (cursor.moveToNext()) {
				String address = cursor.getString(cursor.getColumnIndex("address"));
				int type = cursor.getInt(cursor.getColumnIndex("type"));			
				if (type==137) {
					if ("insert-address-token".equals(address)) 
						sb.append("me");
					else
						sb.append(address);
				}
			}
			if (sb.length()>0) {
				result.append("<span class=\"from\">");
				result.append(sb);
				result.append("</span>");
			}
		} finally {
			cursor.close();
		}
	}

	
	private String whoIs(String id, Map<String,String> cache) {
		return id; 
		/*
		if (cache.containsKey(id)) return cache.get(id);
		
		ContentResolver contentResolver = cordova.getActivity().getContentResolver();
		Cursor cursor = contentResolver.query(Uri.parse("content://mms-sms/canonical-address/"), null, "_id = "+id, null, null);
		
		String result = "?";
		
		if (cursor.moveToNext()){
			result = cursor.getString(0);
			cache.put(id,result);
		}

		cursor.close();
		return result;
		*/
	}
}