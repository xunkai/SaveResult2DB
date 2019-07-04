package common;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static utils.MyLogger.LOGGER;
import static utils.MyLogger.getTrace;

public class CuhkszSchool {
    public static int MAX_LOSS_DAY = 7;
    public static String SCHOOL_CODE = "CUHKSZ";

    public static boolean isLegalEPC(String epc) {
        //if (epc.length() == 24 && epc.substring(10).matches("[0-9a-fA-F]+")) {
        if (epc.length() == 24 && isLegalBarcode(getBarcodeFromEpc(epc))) {
            return true;
        }
        return false;
    }

    public static boolean isLegalBarcode(String barcode) {
        if (barcode.matches("[0-9jJkK]{7}")) {
            return true;
        }
        return false;
    }

    public static String getBarcodeFromEpc(String epc) {
        String hex = epc.substring(10);
        String barcode = asciiToString(hex);
        return barcode;
    }


    public static BookInfo getBookInfo(String barcode) {
        String url = "https://api-ap.hosted.exlibrisgroup.com/almaws/v1/items?item_barcode=" + barcode + "&apikey=l8xx004d38e90a564ddeb8f707edee0bb419";
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);
        BookInfo bookInfo = new BookInfo();
        HttpResponse res = null;
        try {
            res = client.execute(get);
            HttpEntity entity = res.getEntity();
            if (null != entity) {
                InputStream in = entity.getContent();//将返回的内容流入输入流内

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(in);//用输入流实例化Document

                Element rootElement = document.getDocumentElement();

                NodeList bibDatas = rootElement.getElementsByTagName("bib_data");
                Element bibData = (Element) bibDatas.item(0);
                //String newTitle = bibData.getAttribute("title");
                bookInfo.bookName = bibData.getElementsByTagName("title").item(0).getTextContent();

                NodeList holding_datas = rootElement.getElementsByTagName("holding_data");
                Element holding_data = (Element) holding_datas.item(0);
                bookInfo.bookIndex = holding_data.getElementsByTagName("call_number").item(0).getTextContent();

                NodeList item_datas = rootElement.getElementsByTagName("item_data");
                Element item_data = (Element) item_datas.item(0);
                bookInfo.barcode = item_data.getElementsByTagName("barcode").item(0).getTextContent();

            }
        } catch (Exception e) {
            LOGGER.error(getTrace(e));
            return null;
        }
        return bookInfo;
    }

    public static String asciiToString(String hex) {
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(getBarcodeFromEpc("010200A10730303136303033"));
    }
}
