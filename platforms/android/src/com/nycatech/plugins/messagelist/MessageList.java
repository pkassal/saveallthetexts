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
import java.io.FileOutputStream;
import java.io.InputStream;

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
	
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss a");
    static final String DIR_NAME = "SavedTexts";
	private static final String STYLE_MY_TEXT = "mytext";
	private static final String STYLE_THEIR_TEXT = "theirtext";
	
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
		String filename = DIR_NAME+"/index.html";
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
			writer.write("  	body { background-color: #D1D1E0; font-size: 1.125rem; font-family: \"Roboto\",\"Helvetica Neue\",Helvetica,Arial,sans-serif;   line-height: 1.5;  }\n");
			writer.write("  	.conversation { background-color: #fff; padding: 20px; margin: 40px  }\n");
			writer.write("  	.when { color: blue; margin-right: 20px; }\n");
			writer.write("  	.from { color: maroon; margin-right: 20px; }\n");
			writer.write("  	.mytext { color: gray; }\n");
			writer.write("  	.theirtext {  color: #444; }\n");
			writer.write("  	.sms_id { display: none }\n");
			writer.write("  	.mms_id { display: none}\n");
			writer.write("      .picframe { background: #EBE0CC; padding: 10px;  margin-left: 10px; margin-right: 10px; display: inline; }\n");
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
					writer.write("  <div class=\"conversation\"><h1>");
										
					StringBuilder sb = new StringBuilder();
					String conversationId = cursor.getString(cursor.getColumnIndex("_id"));
					sb.append("Converation ");
					sb.append(conversationId);
					sb.append(":");
					/*
					Need a way to convert these to names:
					
					String recip = cursor.getString(cursor.getColumnIndex("display_recipient_ids"));
					String[] ids = recip.split(" ");
					for (String s : ids)  {
						sb.append(" ");
						sb.append(whoIs(s, idToWho));
					}
					*/
					writer.write(sb.toString());
					writer.write("</h1>\n");
			
					convMessages.clear();
					convMessages.addAll(recordSmsConversation(conversationId));
					convMessages.addAll(recordMmsConversation(conversationId));
					Collections.sort(convMessages);
					for (Message m : convMessages) {
						writer.write(m.text);
					}
					writer.write("</div>\n");
					
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
				sb.append("<span class=\"");
				if (fromMe) sb.append(STYLE_MY_TEXT);
				else  sb.append(STYLE_THEIR_TEXT);
				sb.append("\">");
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

				boolean fromMe = recordMmsSender(m_id, sb);
				recordMmsParts(m_id, fromMe, sb);
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

	private void recordMmsParts(long mid, boolean fromMe, StringBuilder result) throws Exception {
		ContentResolver contentResolver = cordova.getActivity().getContentResolver();
		final String[] projection = new String[]{"_id","seq","ct","text"};
		final String selection = String.format("mid=%d",mid);
		final String order = "seq ASC";
		Uri uri = Uri.parse("content://mms/part");
		Cursor cursor = contentResolver.query(uri, projection, selection, null, order);
		try {
			StringBuilder sb = new StringBuilder();
			while (cursor.moveToNext()) {
				String body = cursor.getString(cursor.getColumnIndex("text"));
				String type = cursor.getString(cursor.getColumnIndex("ct"));			
				long id = cursor.getLong(cursor.getColumnIndex("_id"));			
				if ("text/plain".equals(type)) {
					sb.append(body);
				} else if ("application/smil".equals(type)) {
					// Skip
				} else if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                "image/gif".equals(type) || "image/jpg".equals(type) ||
                "image/png".equals(type)) {
					recordImage(id, type, sb);
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
				result.append("<span class=\"");
				result.append(fromMe ? STYLE_MY_TEXT : STYLE_THEIR_TEXT);
				result.append("\">");
				result.append(sb);
				result.append("</span>");
			}
		} finally {
			cursor.close();
		}
	}

	private void recordImage(long id, String type, StringBuilder result) throws Exception {
		String ext = "dat";
		if ("image/jpeg".equals(type) || "image/jpg".equals(type)) { ext = "jpg"; }
		if ("image/bmp".equals(type)) { ext = "bmp"; }
		if ("image/gif".equals(type)) { ext = "gif"; }
		if ("image/png".equals(type)) { ext = "png"; }
		
		String filename = String.format("%d.%s", id, ext);
		result.append("<a href=\"");
		result.append(filename);
		result.append("\"><img class='picframe' src=\"");
		result.append(filename);
		result.append("\" width='15%'/></a>");
		
		File myFile = new File(Environment
            .getExternalStorageDirectory(), DIR_NAME + "/" +filename);
		if (myFile.exists())
			myFile.delete();
		FileOutputStream writer = new FileOutputStream(myFile);
		
		InputStream is = null;
		byte[] buffer = new byte[1024];
		try {
			ContentResolver contentResolver = cordova.getActivity().getContentResolver();
			 Uri partURI = Uri.parse("content://mms/part/" + id);
			is = contentResolver.openInputStream(partURI);
			int read;
			while ((read = is.read(buffer)) != -1 ) {
				writer.write(buffer, 0, read);
			}
		} finally {
			writer.close();
			if (is != null) {
					is.close();
			}
		}
	}
	
	private boolean recordMmsSender(long mid, StringBuilder result) throws Exception {
		boolean fromMe = false;
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
					if ("insert-address-token".equals(address)) {
						sb.append("me");
						fromMe = true;
					} else
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
		return fromMe;
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