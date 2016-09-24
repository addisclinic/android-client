package org.sana.android.testHarness;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.sana.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Albert on 9/3/2016.
 */
public class NetworkTestFragment extends Fragment {

    private TextView loginResult;
    private Button  startTest;

    public static NetworkTestFragment getInstance() {
        NetworkTestFragment fragment = new NetworkTestFragment();
        Bundle args = new Bundle();
        //args.putInt(DIALOG_MSG_KEY, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.network_test_fragment, container, false);
        inflateViews(rootView);
        return rootView;
    }

    private void inflateViews(View rootView) {
        loginResult  = (TextView) rootView.findViewById(R.id.login_reult);
    }

    public void httpGETData() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:8080/RESTfulExample/json/product/get");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            StringBuffer result = new StringBuffer();
            System.out.println("Output from Server .... \n");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                result.append(line);
            }
            try {
                JSONObject data = new JSONObject(result.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    // Button Handler
    public void startTest(View view) {
        TestHarnessClient testHarness = TestHarnessClient.getInstance();
        testHarness.login();
    }
}
