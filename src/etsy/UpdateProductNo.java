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
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class UpdateProductNo {

    public static void main(String[] args) {
        String selectProductUrls = "select url_master_id,url from etsy.product_url_master where product_id is null ";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectProductUrls);
        try {
            while (rs.next()) {
                String url = rs.getString("url");
                String id = rs.getString("url_master_id");
                updateProductNo(url, id);
            }
        } catch (SQLException ex) {
            Logger.getLogger(SellerFromWebPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void updateProductNo(String url, String id) {
        String no = StringUtils.substringBetween(url, "https://www.etsy.com/in-en/listing/", "/");
        String updateQuery = "update etsy.product_url_master set product_id=" + no + " where url_master_id=" + id;
        MyConnection.getConnection("etsy");
        MyConnection.insertData(updateQuery);
        System.out.println("Updated!");
    }
}
