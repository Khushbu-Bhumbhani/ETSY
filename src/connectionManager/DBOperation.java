/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionManager;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class DBOperation {

    public static boolean insertProduct(String name, String price, String noOfFav, String id, String seller_name, String seller_url, int rank, String subcategory, int maincategoryid) {
        boolean result = false;
        String insertQuery = "INSERT INTO `etsy`.`product_master_dec_2020`\n"
                + "(`product_name`,\n"
                + "`price`,\n"
                + "`no_of_fav`,\n"
                + "`url_id`,"
                + "seller_name,"
                + "seller_url,product_rank,sub_category,main_category_id"
                + ")\n"
                + "VALUES\n"
                + "('"
                + prepareString(name) + "'," + (price.equals("") ? "NULL" : price) + ",'" + noOfFav + "'," + id
                + ",'" + prepareString(seller_name) + "','" + seller_url
                + "'," + rank + ",'" + prepareString(subcategory) + "'," + maincategoryid
                + ")";
        MyConnection.getConnection("etsy");
        result = MyConnection.insertData(insertQuery);
        System.out.println("Inserted!!");
        return result;
    }

    public static void updateScrapeStatus(String id,int status) {
        String updateQuery = "update product_url_master_dec_2020 set is_scraped="+status+" where url_master_id=" + id;
        MyConnection.getConnection("etsy");
        MyConnection.insertData(updateQuery);
    }

    public static String prepareString(String str) {
        if (str != null) {
            str = str.replaceAll("'", "''");
            str=str.replace("\"", "\"");
            str=str.replace("\\", "\\\\");
        }
        return str;
    }

    public static void updateProduct(String name, String price, String noOfFav, String id, String seller_name, String seller_url, int rank) {
        String insertQuery = "update `etsy`.`product_master_dec_2020` set \n"
                + "`product_name`='" + prepareString(name) + "',\n"
                + "`price`=" + (price.equals("") ? "NULL" : price) + ",\n"
                + "`no_of_fav`='" + noOfFav + "',\n"
                + "seller_name='" + prepareString(seller_name) + "',\n"
                + "seller_url='" + seller_url + "',"
                + "product_rank=" + rank
                + " where `url_id`=" + id + "";
        MyConnection.getConnection("etsy");
        MyConnection.insertData(insertQuery);
        System.out.println("updated!! URL id:" + id);
    }
}
