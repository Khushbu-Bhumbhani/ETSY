/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class Etsy {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String url = "https://www.etsy.com/in-en/c/accessories/hats-and-caps/winter-hats?ref=catcard-a-36-169521578&explicit=1&page=1";

        mainLinkScraping(url);
    }

    private static void mainLinkScraping(String url) {
        boolean hasNextPage = false;
        do {
            try {
                // TODO code application logic here
                Document doc = Jsoup.connect(url)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(0).get();
                //  System.out.println("Doc:" + doc.text());
                Element result = doc.getElementsByClass("search-listings-group").first();

                // System.out.println("res:" + result.text());
                Elements liTags = result.getElementsByTag("li");
                for (Element li : liTags) {

                    if (li.text().contains("Bestseller")) {
                        Element a = li.getElementsByTag("a").first();
                        String alink = a.attr("href");
                        //  System.out.println("" + alink);
                        detailScrape(alink);
                        Thread.sleep(1000);
                        //  break;
                    }
                }
                //Pagination code  
                if (!doc.getElementsByAttributeValueContaining("data-appears-component-name", "search_pagination").isEmpty()) {
                    Element paginationDiv = doc.getElementsByAttributeValueContaining("data-appears-component-name", "search_pagination").first();
                    if (!paginationDiv.getElementsContainingOwnText("Next page").isEmpty()) {
                        Element nextLink = paginationDiv.getElementsContainingOwnText("Next page").first();
                        Element parent = nextLink.parent();
                        System.out.println("parent tag:" + parent.tagName());
                        Element apagelink = parent.getElementsByTag("a").first();
                        hasNextPage = true;
                        url = apagelink.attr("href");
                    } else {
                        hasNextPage = false;
                    }
                } else {
                    hasNextPage = false;
                }
            } catch (IOException ex) {
                Logger.getLogger(Etsy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(Etsy.class.getName()).log(Level.SEVERE, null, ex);
            }

        } while (hasNextPage);

    }

    private static void detailScrape(String alink) {
        try {
            Document doc = Jsoup.connect(alink)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(0).get();
            // System.out.println("text:"+doc.text());
            String name = "";
            String price = "";
            String noOfFav = "";
            if (!doc.getElementsByClass("override-listing-title").isEmpty()) {
                name = doc.getElementsByClass("override-listing-title").first().text();
            }
            if (!doc.getElementsByClass("override-listing-price").isEmpty()) {
                price = doc.getElementsByClass("override-listing-price").first().text();
            }
            if (doc.text().contains("Favourited by:")) {
                noOfFav = StringUtils.substringBetween(doc.html(), "Favourited by:", "</a>");
                noOfFav = html2text(noOfFav);
            }
            System.out.println("" + name + ";" + price + ";" + noOfFav + ";" + alink);
            
           // DBOperation.insertProduct(name,price,noOfFav,alink);
        } catch (IOException ex) {
            Logger.getLogger(Etsy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String html2text(String html) {
        if (html == null) {
            return "";
        }
        if (!html.equals("")) {
            return Jsoup.parse(html).text().trim();
        } else {
            return html;

        }
    }

}
