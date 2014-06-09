package es.upc.lewis.quadadk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class HTTPCalls{

	static DefaultHttpClient httpclient;
	static int HTTPResponseOK=200;
	static String server_addr = "http://pbl1.webfactional.com/";
	
	/*** MISSION ***/
	public static boolean get_startmission(String quadid){
		HttpGet httpget = new HttpGet(server_addr+"start_mission.php?id="+quadid);
		HttpResponse response;
		try{
			response = httpclient.execute(httpget);
			int myresponsecode = response.getStatusLine().getStatusCode();	
			if(myresponsecode==HTTPResponseOK)
			{
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				content.close();
				entity.consumeContent();
				if(builder=="start")
					return true;
				else
					return false;
			}	
		}catch (ClientProtocolException e) {} catch (IOException e) {}
		return false;

	}
	//SYSTEM LOG
	public static boolean debug_data(String quadid, String data)
	{
		String params = URLEncoder.encode(data);
		HttpGet httpget = new HttpGet(server_addr+"send_logs.php?id="+quadid+"&data="+params);
		HttpResponse response;
		try{
			response = httpclient.execute(httpget);
			int myresponsecode=response.getStatusLine().getStatusCode();
			if(myresponsecode==HTTPResponseOK)
				return true;
			else
				return false;
		}catch (ClientProtocolException e) {} catch (IOException e) {}
		return false;
	}
	//SEND PICTURE
	public static boolean send_picture(ByteArrayEntity reqEntity, String quadid)
	{
		HttpPost httppost = new HttpPost(server_addr+"send_picture.php?id="+quadid);
		httppost.setHeader("Content-Type", "image/jpg");
		httppost.setEntity(reqEntity);
		HttpResponse response;
		StringBuilder builder = new StringBuilder();
		try {
			response = httpclient.execute(httppost);
			int myresponsecode = response.getStatusLine().getStatusCode();
			if(myresponsecode==HTTPResponseOK)
				return true;
			else
				return false;
			}
		catch(ClientProtocolException e){} catch(IOException e){}
		return false;
	}
	
}
