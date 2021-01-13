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
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class UpdateBlankNoOfFav {

    public static void main(String[] args) {
        stratUpdating();
    }

    private static void stratUpdating() {
        String selectQuery = "select url_master_id,url,product_id from etsy.product_url_master u,"
                + " product_master m where u.url_master_id=m.url_id and product_name!=''\n"
                + "    and no_of_fav=''  and url_id>70000 and product_rank<=3";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            while (rs.next()) {
                String url = rs.getString("url");
                String id = rs.getString("url_master_id");
                /*String productId=rs.getString("product_id");*/
                updateNoOfFav(url, id);
              /*  try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(UpdateBlankNoOfFav.class.getName()).log(Level.SEVERE, null, ex);
                }*/
            }
        } catch (SQLException ex) {
            Logger.getLogger(UpdateBlankNoOfFav.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void updateNoOfFav(String url, String id) {
        try {
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .userAgent("chrome")
                    .timeout(0).get();
            String noOfFav = "";
            if (doc.text().contains("Favourited by:")) {
                noOfFav = StringUtils.substringBetween(doc.html(), "Favourited by:", "</a>");
                noOfFav = html2text(noOfFav);
                noOfFav = noOfFav.replace("people", "").trim();
                String UpdateQ = "update product_master set no_of_fav='" + noOfFav + "' where url_id=" + id;
                MyConnection.getConnection("etsy");
                MyConnection.insertData(UpdateQ);
                System.out.println("Updated!! "+id);
            } else {
                System.out.println("Not found--------------------");
             //   System.out.println("Doc:" + doc.getElementById("item-overview").html());
                //   System.out.println("Doc:"+doc.getElementById("listing-page-cart").html());
            }

        } catch (IOException ex) {
            Logger.getLogger(UpdateBlankNoOfFav.class.getName()).log(Level.SEVERE, null, ex);
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
