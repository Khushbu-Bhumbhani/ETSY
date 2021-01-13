/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import connectionManager.MyConnection;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class SellerFromWebPage {

    static final int MAX_THREAD_COUNT = 1;
    static int CURRENT_THREAD_COUNT = 0;

    public static void main(String[] args) {

        //copyAvailableSellerInfo();
        // startSellerScraping();
       // tubemanRemainingscrape();
        startSellerScrapingThreads();
    }

    

    private static void startSellerScrapingThreads() {
        String selectQuery = "SELECT seller_name,seller_url FROM etsy.product_master_dec_2020 where total_product_sales is null group by seller_name;";
        //String selectQuery = "SELECT seller_name,seller_url FROM etsy.product_master where isClickable is null group by seller_name;";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            while (rs.next()) {
                String sellerName = rs.getString("seller_name");
                String sellerUrl = rs.getString("seller_url");
                if (!sellerName.trim().equals("")) {
                    // scrapeTotalSales(sellerName, sellerUrl);
                    SellerInfoThread th = new SellerInfoThread(sellerName, sellerUrl);
                    Thread thread = new Thread(th);
                    thread.start();

                    while (CURRENT_THREAD_COUNT > MAX_THREAD_COUNT) {
                        try {
                            Thread.sleep(1000);
                            //  System.out.println("Sleeping..");
                        } catch (InterruptedException ex) {
                            Logger.getLogger(DetailCrawler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

            }
        } catch (SQLException ex) {
            Logger.getLogger(SellerFromWebPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void startSellerScraping() {
        String selectQuery = "SELECT seller_name,seller_url FROM etsy.product_master_dec_2020 where total_product_sales is null group by seller_name;";
        //String selectQuery = "SELECT seller_name,seller_url FROM etsy.product_master where isClickable is null group by seller_name;";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            while (rs.next()) {
                String sellerName = rs.getString("seller_name");
                String sellerUrl = rs.getString("seller_url");
                if (!sellerName.trim().equals("")) {
                    scrapeTotalSales(sellerName, sellerUrl);
                }

            }
        } catch (SQLException ex) {
            Logger.getLogger(SellerFromWebPage.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void scrapeTotalSales(String sellerName, String sellerUrl) {
        try {
            System.out.println(sellerUrl);
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

    private static void updateTotalSales(String sellerName, String sales, String clickable, String clickURL) {
        String updateQuery = "update product_master_dec_2020 set total_product_sales='" + sales + "'"
                + ",isClickable='" + clickable + "',"
                + "clickable_url='" + clickURL + "'"
                + " where seller_name='" + sellerName + "'";
        MyConnection.getConnection("etsy");
        MyConnection.insertData(updateQuery);
        System.out.println("Updated!");
    }

    private static void copyAvailableSellerInfo() {
        String selectQuery = "SELECT seller_name,seller_url FROM etsy.product_master_nov_2019 where total_product_sales is null group by seller_name;";
        //        String selectQuery = "SELECT seller_name,seller_url FROM etsy.product_master where isClickable is null group by seller_name;";

        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            while (rs.next()) {
                String sellerName = rs.getString("seller_name");
                //String sellerUrl = rs.getString("seller_url");
                String selectSeller = "SELECT total_product_sales,isClickable,clickable_url FROM etsy.product_master where seller_name='" + sellerName + "' and total_product_sales is not null;";
                // MyConnection.getConnection("etsy");
                ResultSet rsset = MyConnection.getResultSet(selectSeller);
                if (rsset.next()) {
                    String updateQuery = "update product_master set total_product_sales='" + rsset.getString("total_product_sales") + "'"
                            + ",isClickable='" + rsset.getString("isClickable") + "',"
                            + "clickable_url='" + rsset.getString("clickable_url") + "'"
                            + " where seller_name='" + sellerName + "' and total_product_sales is null";
                    //MyConnection.getConnection("etsy");
                    MyConnection.insertData(updateQuery);
                    System.out.println("Updated!");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(SellerFromWebPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
