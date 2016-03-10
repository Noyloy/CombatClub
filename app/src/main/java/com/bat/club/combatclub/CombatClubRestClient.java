package com.bat.club.combatclub;

/**
 * Created by Noyloy on 28-Jan-16.
 */

        import com.loopj.android.http.*;

        import org.w3c.dom.Document;
        import org.xml.sax.InputSource;
        import org.xml.sax.SAXException;

        import java.io.IOException;
        import java.io.StringReader;

        import javax.xml.parsers.DocumentBuilder;
        import javax.xml.parsers.DocumentBuilderFactory;
        import javax.xml.parsers.ParserConfigurationException;

        import cz.msebera.android.httpclient.client.HttpClient;


public class CombatClubRestClient {
    //private static final String BASE_URL = "http://212.29.201.146/newsims/ESacre/Content/AppService.asmx";
    private static final String BASE_URL = "pathtoservice.asmx";

    private static AsyncHttpClient client = new AsyncHttpClient();
    private static SyncHttpClient syncClient = new SyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void syncPost(String url, RequestParams params, ResponseHandlerInterface responseHandler) {
        syncClient.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public static String interprateResponse(String rawXmlResponse){
        String result = "";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(rawXmlResponse));
            try {
                Document doc = db.parse(is);
                result = doc.getDocumentElement().getTextContent();
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        }catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
