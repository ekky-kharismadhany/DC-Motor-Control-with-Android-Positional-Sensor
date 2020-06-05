package com.example.proyekbesar;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

class HttpRequestTask extends AsyncTask<String, Void, String> {

    private MainActivity main = new MainActivity();
    private String serverAddress = main.serverAdress;
    private String serverResponse = "";

    @Override
    protected String doInBackground(String... strings) {
        String val = strings[0];
        final String url = "http://" + serverAddress + "/led/" + val;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet();
            getRequest.setURI(new URI(url));
            HttpResponse response = client.execute(getRequest);

            InputStream inputStream = null;
            inputStream = response.getEntity().getContent();
            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(inputStream));

            serverResponse = bufferedReader.readLine();
            inputStream.close();

        } catch (URISyntaxException e) {
            e.printStackTrace();
            serverResponse = e.getMessage();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            serverResponse = e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            serverResponse = e.getMessage();
        }

        return serverResponse;
    }
        @Override
        protected void onPostExecute(String s) {

        }

        @Override
        protected void onPreExecute() {

        }
    }