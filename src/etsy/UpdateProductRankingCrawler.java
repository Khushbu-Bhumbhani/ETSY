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
public class UpdateProductRankingCrawler {

    static final int MAX_THREAD_COUNT = 0;
    static int CURRENT_THREAD_COUNT = 0;
    static String mainCategory = "";

    public static void main(String[] args) {
        mainCategory = args[0];
        startDetailCrawler();
    }

    private static void startDetailCrawler() {
        /*  String selectProductUrls = "select product_master_id,url,product_id from etsy.product_url_master u,product_master p where\n"
                + "p.url_id=u.url_master_id and\n"
                + " product_rank is null limit 0,100;";*/

        String selectProductUrls = "SELECT \n"
                + "                   product_master_id,url,product_id\n"
                + "               FROM\n"
                + "                   etsy.product_master_dec_2020 p,\n"
                + "                   etsy.product_url_master_dec_2020 u\n"
                + "		\n"
                + "                WHERE\n"
                + "                   p.url_id = u.url_master_id\n"
                + "                  \n"
                + "					and product_name!=''\n"
                + " and main_category_id in (" + mainCategory 
                + ")                   and product_rank=-1";
        // + "    and product_rank is null";
        // String selectProductUrls = "select url_master_id,url,product_id from etsy.product_url_master where is_scraped=0 and url_master_id between 41574 and 41600;";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectProductUrls);
        try {
            while (rs.next()) {
                String url = rs.getString("url");
                String productMasterid = rs.getString("product_master_id");
                String productId = rs.getString("product_id");
                // updateProductNo(url,id);
                UpdateProductRanking dc = new UpdateProductRanking(productMasterid, url, productId);
                Thread thread = new Thread(dc);
                thread.start();
                while (CURRENT_THREAD_COUNT > MAX_THREAD_COUNT) {
                    try {
                        Thread.sleep(1000);
                        //  System.out.println("Sleeping..");
                    } catch (InterruptedException ex) {
                        Logger.getLogger(UpdateProductRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Thread.sleep(1000);
            }
        } catch (SQLException ex) {
            Logger.getLogger(UpdateProductRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(UpdateProductRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
