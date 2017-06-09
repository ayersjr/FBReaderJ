package org.geometerplus.android.fbreader.library;

import org.bookshare.net.BookshareHttpOauth2Client;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;


/**
 * Created by animal@martus.org on 3/28/16.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = org.geometerplus.android.fbreader.library.BuildConfig.class, sdk = 21)
public class TestBookshareHttpOauth2Client {

    private static final String BOOKSHARE_QA_AP_V2_USER_NAME = "QA.ApiTestIM_1@benetech.org";
    private static final String BOOKSHARE_QA_API_V2_PASSWORD = "gtlbegeu";

    @Test
    public void testConnection() throws Exception {
        BookshareHttpOauth2Client httpClient =  new BookshareHttpOauth2Client();
        HttpsURLConnection urlConnection = httpClient.createBookshareApiUrlConnection(BOOKSHARE_QA_AP_V2_USER_NAME, BOOKSHARE_QA_API_V2_PASSWORD);

        assertEquals("Url connection should be ok?", HttpURLConnection.HTTP_OK, urlConnection.getResponseCode());

        String response = httpClient.requestData(urlConnection);
        JSONObject jsonResponse = new JSONObject(response);
        String accessToken = jsonResponse.getString(BookshareHttpOauth2Client.ACCESS_TOKEN_CODE);
        JSONObject readingLists = httpClient.getReadingLists(accessToken);
        assertFalse("Should contain atleast one readingList?", readingLists.length() == 0);
    }
}
