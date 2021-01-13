/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import connectionManager.MyConnection;
import static etsy.SellerFromWebPage.CURRENT_THREAD_COUNT;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Dell
 */
public class SellerInfoThread implements Runnable {

    String sellerName;
    String sellerUrl;

    public SellerInfoThread(String sellerName, String sellerUrl) {
        this.sellerName = sellerName;
        this.sellerUrl = sellerUrl;
        CURRENT_THREAD_COUNT++;

    }

    @Override
    public void run() {
        scrapeTotalSales();
        CURRENT_THREAD_COUNT--;
    }

    private void scrapeTotalSales() {
        try {
            System.out.println(sellerUrl);
            if (sellerUrl.equals("")) {
                return;
            }

            Document doc = Jsoup.connect(sellerUrl)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(0).get();

            if (!doc.getElementsByClass("shop-sales").isEmpty()) {
                Element span = doc.getElementsByClass("shop-sales").first();
                String sales = span.text();
                String clickable = "No";
                String clickURL = "";
                if (!span.getElementsByTag("a").isEmpty()) {
                    clickable = "Yes";
                    clickURL = span.getElementsByTag("a").first().attr("href");

                }
                sales = sales.replace("Sales", "").trim();
                updateTotalSales(sellerName, sales, clickable, clickURL);
            }
        } catch (IOException ex) {
            Logger.getLogger(SellerFromWebPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateTotalSales(String sellerName, String sales, String clickable, String clickURL) {
        String updateQuery = "update product_master_dec_2020 set total_product_sales='" + sales + "'"
                + ",isClickable='" + clickable + "',"
                + "clickable_url='" + clickURL + "'"
                + " where seller_name='" + sellerName + "'";
        MyConnection.getConnection("etsy");
        MyConnection.insertData(updateQuery);
        System.out.println("Updated!");
    }

}
