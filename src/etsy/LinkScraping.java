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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class LinkScraping {

    public static void main(String[] args) {
        // String selectQuery = "SELECT sub_category_master_2020_id,sub_category_url,currentPage FROM etsy.sub_category_master_2020 where isScraped=0;";
        /*  String selectQuery = "SELECT sub_category_master_2020_id,sub_category_url,currentPage"
                + " FROM etsy.sub_category_master_2020 where isScraped=0 and sub_category_master_2020_id between "+args[0]+" and "+args[1];*/
        String id = "2";
        if (args.length != 0) {
            id = args[0];
        }
        //String sortType = "&order=highest_reviews";
        String sortType = "&order=date_desc";
        String selectQuery = "SELECT sub_category_master_id,sub_category_url,currentPage"
                + " FROM etsy.sub_category_master_2020 where isScraped=0 and main_category_id= " + id;
        startLinkScraping(selectQuery, sortType);

        /*for (int id = 2; id <= 6; id++) {
            String selectQuery = "SELECT sub_category_master_2020_id,sub_category_url,currentPage"
                    + " FROM etsy.sub_category_master_2020 where isScraped=0 and main_category_id= " + id;
            System.out.println("============>" + selectQuery);
            startLinkScraping(selectQuery);
        }*/
    }

    private static void startLinkScraping(String selectQuery, String sortType) {
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            while (rs.next()) {
                String url = rs.getString("sub_category_url") + sortType;
                scrapeProductLinks(rs.getString("sub_category_master_id"), url, rs.getInt("currentPage"));
                Thread.sleep(1000);
            }
        } catch (SQLException | InterruptedException ex) {
            Logger.getLogger(LinkScraping.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void scrapeProductLinks(String subCatId, String url, int currentPage) {

        boolean hasNextPage = false;
        int pageNo = 0;
        String mainUrl = url;
        if (currentPage != 0) {
            url = url + "&ref=pagination&page=" + currentPage;
            pageNo = currentPage;
        }
        do {
            System.out.println("Getting..." + url);
            try {
                // TODO code application logic here
                Document doc = Jsoup.connect(url)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(0).get();
                //  System.out.println("Doc:" + doc.text());
                //Element result = doc.getElementsByClass("search-listings-group").first();
                //   Element result = doc.getElementById("reorderable-listing-results");

                if (doc.getElementsByClass("responsive-listing-grid").isEmpty()) {
                    System.out.println("No more results found...");
                    hasNextPage = false;
                    break;
                }
                Element result = doc.getElementsByClass("responsive-listing-grid").first();
                // System.out.println("res:" + result.text());
                Elements liTags = result.getElementsByTag("li");
                for (Element li : liTags) {

                    if (li.text().contains("Bestseller")) {
                        Element a = li.getElementsByTag("a").first();
                        String alink = a.attr("href");
                        insertLink(alink, url, subCatId);
                    }
                }
                updateCurrentPageValue(subCatId, url);
                pageNo++;
                //Pagination code  
                if (!doc.getElementsByAttributeValueContaining("data-appears-component-name", "search_pagination").isEmpty()) {
                    Element paginationDiv = doc.getElementsByAttributeValueContaining("data-appears-component-name", "search_pagination").first();
                    if (!paginationDiv.getElementsContainingOwnText("Next page").isEmpty()) {
                        Element nextLink = paginationDiv.getElementsContainingOwnText("Next page").first();
                        Element parent = nextLink.parent();
                        // System.out.println("parent tag:" + parent.tagName());
                        Element apagelink = parent.getElementsByTag("a").first();
                        hasNextPage = true;
                        url = apagelink.attr("href");
                        if (url.trim().equals("")) {
                            url = mainUrl + "&ref=pagination&page=" + pageNo;
                        }
                        Thread.sleep(1000);
                    } else {
                        hasNextPage = false;
                    }
                } else {
                    hasNextPage = false;
                }
            } catch (IOException ex) {
                Logger.getLogger(Etsy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(LinkScraping.class.getName()).log(Level.SEVERE, null, ex);
            }

        } while (hasNextPage);
        updateScrapingStatus(subCatId);
    }

    private static void insertLink(String alink, String url, String subCatId) {
        MyConnection.getConnection("etsy");
        if (!isAvailable(alink)) {
            String no = StringUtils.substringBetween(alink, "/listing/", "/");
            String insertQuery = "INSERT INTO `etsy`.`product_url_master_dec_2020`\n"
                    + "(`url`,\n"
                    + "`sub_category_id`,\n"
                    + "`page_url`,product_id)\n"
                    + "VALUES\n"
                    + "("
                    + "'" + alink + "',"
                    + "'" + subCatId + "',"
                    + "'" + url + "',"
                    + "'" + no + "'"
                    + ")";
            MyConnection.insertData(insertQuery);
            System.out.println("Link Inserted!!");
        } else {
            System.out.println("Dupliate link...Skipped..");
        }
    }

    private static boolean isAvailable(String alink) {
        String no = StringUtils.substringBetween(alink, "https://www.etsy.com/in-en/listing/", "/");
        String selectQuery = "select `url_master_id` from  `product_url_master_dec_2020` where product_id='" + no + "'";
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(LinkScraping.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private static void updateScrapingStatus(String subCatId) {
        String updateQuery = "update etsy.sub_category_master_2020  set isScraped=1 where sub_category_master_id=" + subCatId;
        MyConnection.getConnection("etsy");
        MyConnection.insertData(updateQuery);
        System.out.println("Status Updated!!");
    }

    private static void updateCurrentPageValue(String subCatId, String url) {
        if (url.contains("&page=")) {
            String pageNo = StringUtils.substringAfter(url, "&page=");
            String updateQuery = "update etsy.sub_category_master_2020  set currentPage=" + pageNo + " where sub_category_master_id=" + subCatId;
            MyConnection.getConnection("etsy");
            MyConnection.insertData(updateQuery);
        }
        //  System.out.println("Status Updated!!");
    }

}
