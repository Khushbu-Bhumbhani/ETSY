/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ranking;

import connectionManager.MyConnection;
;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Khushbu
 */


public class NewRankingCrawler {

    static int main_category_id = 1;

    static final int MAX_THREAD_COUNT = 3;
    static int CURRENT_THREAD_COUNT = 0;

    public static void main(String[] args) {

       // startNewRankScrape();
        startNewTotalSalesUpdate();
    }

    private static void startNewRankScrape() {
        String selectQuery = "SELECT \n"
                + "    product_master_id, url, product_id,url_master_id\n"
                + "FROM\n"
                + "    product_master m,\n"
                + "    product_url_master u\n"
                + "WHERE\n"
                + "    m.url_id = u.url_master_id\n"
                + "        AND is_scraped = 0\n"
                //  + "        AND new_rank=-1\n"
                //+ "        AND new_rank is null\n"
                // + "        AND new_no_of_fav is null\n"
                // + "        AND new_total_product_sales is null\n"
                //  + "        AND folder_path='weddings>invitations-and-paper>Invitations'\n"
                + "        AND main_category_id = " + main_category_id;
        //              + " limit 0,5";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            while (rs.next()) {
                String url = rs.getString("url");
                String productMasterid = rs.getString("product_master_id");
                String productId = rs.getString("product_id");
                // updateProductNo(url,id);
                UpdateNewProductRanking dc = new UpdateNewProductRanking(productMasterid, url, productId, rs.getInt("url_master_id"));
                Thread thread = new Thread(dc);
                thread.start();
                while (CURRENT_THREAD_COUNT > MAX_THREAD_COUNT) {
                    try {
                        Thread.sleep(1000);
                        //  System.out.println("Sleeping..");
                    } catch (InterruptedException ex) {
                        Logger.getLogger(NewRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Thread.sleep(1000);
            }
        } catch (SQLException ex) {
            Logger.getLogger(NewRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(NewRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void startNewTotalSalesUpdate() {
        String selectQuery = "SELECT seller_name,seller_url FROM etsy.product_master"
                + " where new_total_product_sales is null "
                //   + "        AND folder_path='weddings>invitations-and-paper>Invitations'\n"
                + "        AND main_category_id = " + main_category_id
        +" group by seller_url;";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            while (rs.next()) {
                String url = rs.getString("seller_url");
                String sellerName = rs.getString("seller_name");
                // String productId = rs.getString("product_id");
                // updateProductNo(url,id);
                UpdateNewTotalSales dc = new UpdateNewTotalSales(url, sellerName);
                Thread thread = new Thread(dc);
                thread.start();
                while (CURRENT_THREAD_COUNT > MAX_THREAD_COUNT) {
                    try {
                        Thread.sleep(1000);
                        //  System.out.println("Sleeping..");
                    } catch (InterruptedException ex) {
                        Logger.getLogger(NewRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Thread.sleep(1000);
            }
        } catch (SQLException ex) {
            Logger.getLogger(NewRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(NewRankingCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
