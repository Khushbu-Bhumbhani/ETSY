/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import connectionManager.MyConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class DetailCrawler {

    static final int MAX_THREAD_COUNT = 0;
    static int CURRENT_THREAD_COUNT = 0;

    public static void main(String[] args) {
        // testReviewRank();
        String mainId = "2";
        if (args.length != 0) {
            mainId = args[0];
        }
        startDetailCrawler(mainId);
    }

    private static void startDetailCrawler(String mainID) {
        String selectProductUrls = "select url_master_id,url,product_id,s.main_category_id from etsy.product_url_master_dec_2020 u, sub_category_master_2020 s\n"
                + "                 where is_scraped=0\n"
                + "                 and u.sub_category_id=s.sub_category_master_id\n"
                + "                 and s.main_category_id=" + mainID;

        /*  String selectProductUrls = "SELECT url_master_id,url,product_id,s.main_category_id FROM etsy.product_url_master_august_2019 u,sub_category_master s where url_master_id not in (select url_id from product_master_august_2019 ) and \n"
                + "u.sub_category_id=s.sub_category_master_id and main_category_id=10;";*/
        //  String selectProductUrls = "select url_master_id,url,product_id from etsy.product_url_master u, product_master m where u.url_master_id=m.url_id and product_name='' and product_rank!=-1";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectProductUrls);
        try {
            while (rs.next()) {
                String url = rs.getString("url");
                String id = rs.getString("url_master_id");
                String productId = rs.getString("product_id");
                // updateProductNo(url,id);
                DetailScraping dc = new DetailScraping(url, id, productId, rs.getInt("s.main_category_id"));
                Thread thread = new Thread(dc);
                thread.start();
                while (CURRENT_THREAD_COUNT > MAX_THREAD_COUNT) {
                    try {
                        Thread.sleep(2000);
                        //  System.out.println("Sleeping..");
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DetailCrawler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DetailCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void testReviewRank() {
        String url = "https://www.etsy.com/uk/listing/490847825/sailor-moon-luna-artemis-black-lady?ga_order=highest_reviews&ga_search_type=all&ga_view_type=gallery&ga_search_query=&ref=sr_gallery-1-3&bes=1";
        DetailScraping dc = new DetailScraping(url, "1", "113739606", 2);
        Thread thread = new Thread(dc);
        thread.start();
    }

}
