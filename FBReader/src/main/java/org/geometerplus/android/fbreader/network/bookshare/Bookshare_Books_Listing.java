package org.geometerplus.android.fbreader.network.bookshare;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.accessibility.ParentCloserDialog;
import org.benetech.android.R;
import org.bookshare.net.BookshareWebServiceClient;
import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * This ListActivity shows the search results
 * in form of a ListView.
 *
 */
public class Bookshare_Books_Listing extends ListActivity{

    private final static int LIST_RESPONSE = 1;
    private final static int METADATA_RESPONSE = 2;

    public static final String URI_BOOKSHARE_ID_SEARCH = Bookshare_Webservice_Login.BOOKSHARE_API_PROTOCOL + Bookshare_Webservice_Login.BOOKSHARE_API_HOST + "/book/id/";
    private String username;
    private String password;
    private String requestURI;
    private int requestType;
    private int responseType;
    private final int DATA_FETCHED = 99;
    private Vector<Bookshare_Result_Bean> vectorResults;
    private ProgressDialog pd_spinning;
    private final int START_BOOKSHARE_BOOK_DETAILS_ACTIVITY = 0;
    private final int BOOKSHARE_BOOK_DETAILS_FINISHED = 1;
    private final int BOOKSHARE_BOOKS_LISTING_FINISHED = 2;
    private final int PREVIOUS_PAGE_BOOK_ID = -1;
    private final int NEXT_PAGE_BOOK_ID = -2;
    private ArrayList<TreeMap<String,Object>> list = new ArrayList<TreeMap<String, Object>>();
    InputStream inputStream;
    final BookshareWebServiceClient bws = new BookshareWebServiceClient(Bookshare_Webservice_Login.BOOKSHARE_API_HOST);
    private int total_pages_result;
    private int current_result_page = 1;
    private boolean isFree = false;
    private String developerKey = BookshareDeveloperKey.DEVELOPER_KEY;
    private Resources resources;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        resources = getApplicationContext().getResources();

        Intent intent  = getIntent();
        username = intent.getStringExtra("username");
        password = intent.getStringExtra("password");

        if(username == null || password == null){
            isFree = true;
        }

        requestURI = intent.getStringExtra(Bookshare_Menu.REQUEST_URI);
        System.out.println("requestURI = "+requestURI);
        requestType = intent.getIntExtra(Bookshare_Menu.REQUEST_TYPE, Bookshare_Menu.TITLE_SEARCH_REQUEST);

        if(requestType == Bookshare_Menu.TITLE_SEARCH_REQUEST
                || requestType == Bookshare_Menu.AUTHOR_SEARCH_REQUEST
                || requestType == Bookshare_Menu.LATEST_REQUEST
                || requestType == Bookshare_Menu.POPULAR_REQUEST
                ){
            responseType = LIST_RESPONSE;
        }
        else if(requestType == Bookshare_Menu.ISBN_SEARCH_REQUEST){
            responseType  = METADATA_RESPONSE;
        }
        getListing(requestURI);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ((ZLAndroidApplication) getApplication()).startTracker(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        ((ZLAndroidApplication) getApplication()).stopTracker(this);
    }

    /*
     * Display voiceable message and then close
     */
    private void confirmAndClose(String msg, int timeout) {
        final ParentCloserDialog dialog = new ParentCloserDialog(this, this);
        dialog.popup(msg, timeout);
    }

    /*
     * Spawn a new Thread for carrying out the search
     */
    private void getListing(final String uri){

        vectorResults = new Vector<Bookshare_Result_Bean>();

        // Show a Progress Dialog before the book opens
        pd_spinning = ProgressDialog.show(this, null, resources.getString(R.string.fetching_books), Boolean.TRUE);

        final AsyncTask<Object, Void, Integer> bookResultsFetcher = new BookListingTask(uri);
        bookResultsFetcher.execute();
    }

    private class BookListingTask extends AsyncTask<Object, Void, Integer> {

        String uri;

        public BookListingTask(String requestUri) {
            uri = requestUri;
        }

        @Override
        protected Integer doInBackground(Object... params) {

            try{
                inputStream = bws.getResponseStream(password, uri);
                String response_HTML = bws.convertStreamToString(inputStream);

                // Cleanup the HTML formatted tags
                String response = response_HTML.replace("&apos;", "\'").replace("&quot;", "\"").replace("&amp;", "and").replace("&#xd;","").replace("&#x97;", "-");
        //              String response = response_HTML.replace("&apos;", "\'").replace("&quot;", "\"").replace("&#xd;","").replace("&#x97;", "-");

                System.out.println(response);
                // Parse the response of search result
                parseResponse(response);
                Log.w(FBReader.LOG_LABEL, "done with parseResponse in task");

            }
            catch(Exception e){
                Log.e(FBReader.LOG_LABEL, "problem getting results", e);
            }

            return 0;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer results) {
            super.onPostExecute(results);
            Log.w(FBReader.LOG_LABEL, "about to call on ResultsFetched");
            onResultsFetched();
        }

    }

    private void createPageChanger(String title, int id, int iconId) {
        TreeMap<String, Object> row_item = new TreeMap<String, Object>();
        row_item.put("title", title);
        row_item.put("authors", "");
        row_item.put("icon", iconId);
        row_item.put("book_id", String.valueOf(id));
        row_item.put("download_icon", iconId);
        list.add(row_item);
    }

    private void onResultsFetched()
    {
        pd_spinning.cancel();

        if(responseType == METADATA_RESPONSE){
            //Do nothing
        }

        // Returned response is of our use. Process it
        if(responseType == LIST_RESPONSE){
            list.clear();

            // For each bean object stored in the vector, create a row in the list
            for(Bookshare_Result_Bean bean : vectorResults){
                String authors = "";
                TreeMap<String, Object> row_item = new TreeMap<String, Object>();
                row_item.put("title", bean.getTitle());
                for(int i = 0; i < bean.getAuthor().length; i++){
                    if(i==0){
                        authors = bean.getAuthor()[i];
                    }
                    else{
                        authors = authors +", "+ bean.getAuthor()[i];
                    }
                }
                row_item.put("authors", authors);
                row_item.put("icon", R.drawable.titles);
                row_item.put("book_id", bean.getId());
                // Add a download icon if the book is available to download
/*                      if(!isFree && bean.getAvailableToDownload().equals("1")){
                    row_item.put("download_icon", R.drawable.download_icon);
                }
                else if(isFree && bean.getAvailableToDownload().equals("1") &&
                            bean.getFreelyAvailable().equals("1") ){
                    row_item.put("download_icon", R.drawable.download_icon);
                }*/
                if((isFree && bean.getAvailableToDownload().equals("1") &&
                        bean.getFreelyAvailable().equals("1")) ||
                        (!isFree && bean.getAvailableToDownload().equals("1"))){
                    row_item.put("download_icon", R.drawable.download_icon);
                }
                else{
                    row_item.put("download_icon", R.drawable.black_icon);
                }
                list.add(row_item);
            }

            if(current_result_page > 1 ){
                createPageChanger("Previous Page", PREVIOUS_PAGE_BOOK_ID, R.drawable.arrow_left_blue);
            }

            if(current_result_page < total_pages_result ){
                createPageChanger("Next Page", NEXT_PAGE_BOOK_ID, R.drawable.arrow_right_blue);
            }
        }

        // Instantiate the custom SimpleAdapter for populating the ListView
        // The bookId view in the layout file is used to store the id , but is not shown on screen
        MySimpleAdapter simpleadapter = new MySimpleAdapter(
                getApplicationContext(),list,
                R.layout.bookshare_menu_item,
                new String[]{"title","authors","icon","download_icon","book_id"},
                new int[]{R.id.text1, R.id.text2,R.id.row_icon, R.id.bookshare_download_icon,R.id.bookId});

        //Set the adapter for this view
        setListAdapter(simpleadapter);

        ListView lv = getListView();

        View decorView = getWindow().getDecorView();
        if (null != decorView) {
            String resultsMessage;
            if (vectorResults.isEmpty()) {
                resultsMessage = resources.getString(R.string.search_complete_no_books);
                setResult(InternalReturnCodes.NO_BOOKS_FOUND);
                confirmAndClose("no books found", 3000);
            } else {
                resultsMessage = resources.getString(R.string.search_complete_with_books);
            }
            decorView.setContentDescription(resultsMessage);
        }

        lv.setOnItemClickListener(new OnItemClickListener(){

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Obtain the layout for selected row
                LinearLayout row_view  = (LinearLayout)view;

                //Obtain the book ID
                TextView bookId = (TextView)row_view.findViewById(R.id.bookId);
                if (null != bookId.getText().toString() ) {
                    int numericId =  Integer.valueOf(bookId.getText().toString());
                    if (numericId < 0) {
                        pageChangeSelected(numericId);
                        return;
                    }
                }

                // Find the corresponding bean object for this row
                for(Bookshare_Result_Bean bean : vectorResults){

                    // Since book ID is unique, that can serve as comparison parameter
                    // Retrieve the book ID from the entry that is clicked
                    if(bean.getId().equalsIgnoreCase(bookId.getText().toString())){
                        String bookshare_ID = bean.getId();
                        Intent intent = new Intent(getApplicationContext(),OnlineBookDetailActivity.class);
                        String uri;
                        if(isFree)
                            uri = URI_BOOKSHARE_ID_SEARCH + bookshare_ID + "?api_key="+developerKey;
                        else
                            uri = URI_BOOKSHARE_ID_SEARCH + bookshare_ID +"/for/"+username+"?api_key="+developerKey;

                        if((isFree && bean.getAvailableToDownload().equals("1") &&
                                bean.getFreelyAvailable().equals("1")) ||
                                (!isFree && bean.getAvailableToDownload().equals("1"))){
                            intent.putExtra("isDownloadable", true);
                        }
                        else{
                            intent.putExtra("isDownloadable", false);
                        }
                        intent.putExtra("ID_SEARCH_URI", uri);
                        if(!isFree){
                            intent.putExtra("username", username);
                            intent.putExtra("password", password);
                        }

                        startActivityForResult(intent, START_BOOKSHARE_BOOK_DETAILS_ACTIVITY);
                        break;
                    }
                }
            }
        });
    }

    public void pageChangeSelected(int selectorId){
        if(selectorId == NEXT_PAGE_BOOK_ID){
            current_result_page++;
        }
        else if(selectorId == PREVIOUS_PAGE_BOOK_ID){
            current_result_page--;
        }
        list.clear();

        StringBuilder strBuilder = new StringBuilder(requestURI);
        int index;

        if((index = strBuilder.indexOf("?api_key=")) != -1){
            strBuilder.delete(index, strBuilder.length());
            strBuilder.append("/page/").append(current_result_page).append("?api_key=").append(developerKey);
        }
        getListing(strBuilder.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == START_BOOKSHARE_BOOK_DETAILS_ACTIVITY){
            if(resultCode == BOOKSHARE_BOOK_DETAILS_FINISHED){
                setResult(BOOKSHARE_BOOKS_LISTING_FINISHED);
                finish();
            } else if (resultCode == InternalReturnCodes.NO_BOOK_FOUND) {
                setResult(resultCode);
                finish();
            }
        }
    }


    /**
     * Uses a SAX parser to parse the response
     * @param response String representing the response
     */
    private void parseResponse(String response){

        InputSource is = new InputSource(new StringReader(response));

        try{
            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp;
            sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLReader parser = sp.getXMLReader();
            BookshareApiResultSaxHandler handler = new BookshareApiResultSaxHandler();
            parser.setContentHandler(handler);
            parser.parse(is);

            total_pages_result = handler.getTotal_pages_result();
            vectorResults = handler.getVectorResults();
        }
        catch(SAXException e){
            System.out.println(e);
        }
        catch (ParserConfigurationException e) {
            System.out.println(e);
        }
        catch(IOException ioe){
            System.out.println(ioe);
        }
    }

    // A custom SimpleAdapter class for providing data to the ListView
    private class MySimpleAdapter extends SimpleAdapter{
        public MySimpleAdapter(Context context, List<? extends Map<String, ?>> data,
                int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        /*
         * Retrieves view for the item in the adapter, at the
         * specified position and populates it with data.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.bookshare_menu_item, null);
            }

            TreeMap<String, Object> data = (TreeMap<String, Object>) getItem(position);

            ((TextView) convertView.findViewById(R.id.text1))
            .setText((String) data.get("title"));

            StringBuilder authorsBuilder = new StringBuilder("");
            if ( (data.get("authors") != null) &&   ((String)data.get("authors")).length() > 0) {
                authorsBuilder = new StringBuilder("by ");
                authorsBuilder.append((String) data.get("authors"));
                if((Integer)data.get("download_icon") == R.drawable.black_icon) {
                    authorsBuilder.append(" ( not downloadable )");
                }
            }

            // would have preferred to set this as setContentDescription, but that didn't voice
            ((TextView) convertView.findViewById(R.id.text2))
            .setText(authorsBuilder.toString());

            ((ImageView) convertView.findViewById(R.id.row_icon))
            .setImageResource((Integer) data.get("icon"));

            if(data.get("download_icon") != null){
                ((ImageView)convertView.findViewById(R.id.bookshare_download_icon))
                .setImageResource((Integer) data.get("download_icon"));

                ((TextView) convertView.findViewById(R.id.bookId))
                .setText((String) data.get("book_id"));
            }
            return convertView;
        }
    }
}
