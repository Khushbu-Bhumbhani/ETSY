/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import connectionManager.DBOperation;
import connectionManager.MyConnection;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class UpdateNowAvailableItems {

    public static void main(String[] args) {
        // String selectProductUrls = "select url_master_id,url,product_id from etsy.product_url_master where is_scraped=0 limit 0,2;";
        String selectProductUrls = "select url_master_id,url,product_id "
                + "from etsy.product_url_master_dec_2020 u, product_master_dec_2020 m where u.url_master_id=m.url_id"
                + " and main_category_id in (" + args[0] + ")"
                + " and product_name='' #and url_id>62900";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectProductUrls);
        try {
            while (rs.next()) {
                String url = rs.getString("url");
                String id = rs.getString("url_master_id");
                String productId = rs.getString("product_id");
                detailScrape(url, id, productId);
                // updateProductNo(url,id);
                //DetailScraping dc = new DetailScraping(url, id,productId);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DetailCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void detailScrape(String url, String id, String productId) {
        try {
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(0).get();
            // System.out.println("text:"+doc.text());
            String name = "";
            String price = "";
            String noOfFav = "";
            String seller_name = "";
            String seller_url = "";

            if (!doc.getElementsByAttributeValue("data-component", "listing-page-title-component").isEmpty()) {
                name = doc.getElementsByAttributeValue("data-component", "listing-page-title-component").first().text().trim();
            } else {
                System.out.println("Item sold out");
                Element e = doc.getElementById("content");
                if (e != null && !e.getElementsByClass("anchor-listing").isEmpty()) {
                    url = e.getElementsByClass("anchor-listing").first().getElementsByClass("parent-hover-underline").attr("href");
                    System.out.println("Getting Sold url..." + url);
                    Thread.sleep(2000);
                    doc = Jsoup.connect(url)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .timeout(0).get();
                } else if (e != null && !e.getElementsByTag("a").isEmpty() && e.getElementsByTag("a").first().hasClass("parent-hover-underline")) {
                    url = e.getElementsByTag("a").first().attr("href");
                    System.out.println("Getting Sold url from a..." + url);
                    Thread.sleep(2000);
                    doc = Jsoup.connect(url)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .timeout(0).get();
                }/* else {
                    System.out.println("Trying to get created URL..");
                    url = StringUtils.substringBefore(url, "?");
                    url = url + "?show_sold_out_detail=1";
                    System.out.println("Getting Sold url from a..." + url);
                    Thread.sleep(2000);
                    doc = Jsoup.connect(url)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .timeout(0).get();
                }*/
                if (!doc.getElementsByClass("override-listing-title").isEmpty()) {
                    name = doc.getElementsByClass("override-listing-title").first().text().trim();
                }
            }

            if (!doc.getElementsByAttributeValue("data-buy-box-region", "price").isEmpty()
                    && !doc.getElementsByAttributeValue("data-buy-box-region", "price").first().getElementsByTag("h3").isEmpty()) {
                price = doc.getElementsByAttributeValue("data-buy-box-region", "price").first().getElementsByTag("h3").first().text().trim();
                price = price.replace("US$", "");
                price = price.replace("£", "");
                price = price.replaceAll(",", "");
                price = price.replace("+", "").trim();
            }

            String subcategory = StringUtils.substringBetween(doc.html(), "\"category\":", "\",");
            if (subcategory != null) {
                subcategory = subcategory.replace("\"", "").trim();
                subcategory = subcategory.replace("'", "''").trim();
            } else {
                subcategory = "";
            }

            /* if (doc.text().contains("Favourited by:")) {
                noOfFav = StringUtils.substringBetween(doc.html(), "Favourited by:", "</a>");
                noOfFav = html2text(noOfFav);
                noOfFav = noOfFav.replace("people", "").trim();
            } else {
                    System.out.println("Not found");
                System.out.println("Doc:"+doc.getElementById("item-overview").html());
                //   System.out.println("Doc:"+doc.getElementById("listing-page-cart").html());
            }*/
            if (!doc.getElementsByClass("list-inline").isEmpty()
                    && !doc.getElementsByClass("list-inline").first().getElementsContainingOwnText("favourites").isEmpty()) {
                noOfFav = doc.getElementsByClass("list-inline").first().getElementsContainingOwnText("favourites").text().replaceAll("favourites", "").trim();
            }
            /* if (!doc.getElementsByAttributeValue("itemprop", "title").isEmpty()) {
                seller_name = doc.getElementsByAttributeValue("itemprop", "title").first().text();
                Element aTag = doc.getElementsByAttributeValue("itemprop", "title").first().parent();
                seller_url = aTag.attr("href");
            }*/
            if (doc.getElementById("listing-page-cart") != null
                    && !doc.getElementById("listing-page-cart").getElementsByTag("a").isEmpty()) {
                seller_name = doc.getElementById("listing-page-cart").getElementsByTag("a").first().text();
                seller_url = doc.getElementById("listing-page-cart").getElementsByTag("a").first().attr("href");
            }

            if (!doc.getElementsContainingOwnText("favourites").isEmpty()) {
                for (Element e : doc.getElementsContainingOwnText("favourites")) {
                    if (e.parent().hasClass("list-inline-item") && e.tagName().equals("a")) {
                        noOfFav = e.text();
                    }
                }
                // noOfFav = html2text(noOfFav);
                noOfFav = noOfFav.replace("favourites", "").trim();
            }
            //    System.out.println("" + name + ";" + price + ";" + noOfFav + ";" + url);
            int rank = -1;
            if (!name.trim().equals("")) {
                rank = getProductRankingLatest(doc, productId);
            }
            DBOperation.updateProduct(name, price, noOfFav, id, seller_name, seller_url, rank);
            DBOperation.updateScrapeStatus(id,-1);
        } catch (IOException ex) {
            Logger.getLogger(Etsy.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(UpdateNowAvailableItems.class.getName()).log(Level.SEVERE, null, ex);
        }
        DetailCrawler.CURRENT_THREAD_COUNT--;
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

    public static int getProductRankingLatest(Document doc, String productId) {
        int rank = 11;
        SimpleDateFormat smt = new SimpleDateFormat("dd MMM, yyyy");
        long maxWeekDifference = 0;
        int totalProductReviews = 0;
        Element div = doc.getElementById("reviews");
        // System.out.println(""+div.html());
        if (div == null) {
            return -1;
        }
        div = div.getElementById("same-listing-reviews-panel");
        if (div == null && doc.getElementById("reviews").getElementsByClass("wt-flex-wrap").isEmpty()) {
            return -1;
        } else {
            div = doc.getElementById("reviews").getElementsByClass("wt-flex-wrap").first();
        }
        if (div == null) {
            System.out.println(">>Can't find Reviews...");
            return -1;
        }
        for (Element e : div.getElementsByClass("wt-display-flex-xs")) {
            if (!e.getElementsByTag("p").isEmpty()) {
                try {
                    //  System.out.println("" + e.html());
                    String date = e.getElementsByTag("p").first().html();
                    // System.out.println("Date:" + date);
                    //  System.out.println("================");
                    //   System.out.println(""+StringUtils.substringAfter(date,"</"));
                    if (date == null) {
                        date = StringUtils.substringBetween(e.html(), "Reviewed by Inactive", "<");
                    } else if (date.contains("Reviewed by Inactive")) {
                        date = StringUtils.substringBetween(date, "Reviewed by Inactive", "<");
                        if (date == null) {
                            date = StringUtils.substringAfter(date, "Reviewed by Inactive");
                        }
                    } else {
                        date = StringUtils.substringAfter(date, "</a>");
                    }
                    if (date == null) {
                        date = StringUtils.substringBetween(e.html(), "Reviewed by Inactive", "<");
                    }

                    //  System.out.println("Dt:" + date);
                    /*  if (date == null) {
                        System.out.println("Date is null");
                    }*/
                    date = date.trim();
                    if (!date.equals("")) {
                        Date feedBackDate = smt.parse(date);

                        Date today = new Date(System.currentTimeMillis());
                        // System.out.println("feedback Date:" + feedBackDate + " today:" + today);

                        Instant start = feedBackDate.toInstant();
                        Instant end = today.toInstant();

                        //Weeks week=Weeks.weeksBetween(start, end);
                        LocalDateTime startDate = LocalDateTime.ofInstant(start, ZoneId.systemDefault());
                        LocalDateTime endDate = LocalDateTime.ofInstant(end, ZoneId.systemDefault());

                        long diff = ChronoUnit.WEEKS.between(startDate, endDate);
                        //  System.out.println("Weeks:" + diff);
                        totalProductReviews++;
                        if (diff > maxWeekDifference) {
                            maxWeekDifference = diff;
                        }
                    } else {
                        System.out.println("No date: " + date);
                    }
                } catch (ParseException ex) {
                    Logger.getLogger(DetailScraping.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (totalProductReviews == 4) {
            if (maxWeekDifference == 0) {
                rank = 1;
            } else if (maxWeekDifference == 1) {
                rank = 2;
            } else if (maxWeekDifference == 2) {
                rank = 3;
            } else if (maxWeekDifference == 3) {
                rank = 4;
            } else if (maxWeekDifference > 3 && maxWeekDifference <= 7) {
                rank = 5;
            } else if (maxWeekDifference > 7 && maxWeekDifference <= 11) {
                rank = 6;
            } else if (maxWeekDifference > 11 && maxWeekDifference <= 15) {
                rank = 7;
            } else {
                rank = 8;
            }
        } else {
            System.out.println("TOTAL REVIEWS : " + totalProductReviews);
            if (rank != -1) {
                if (totalProductReviews == 0) {
                    rank = 0;
                } else {
                    rank = 8;
                }
            }
        }
        System.out.println("Rank=" + rank);

        return rank;
    }

    static public int getProductRanking(Document doc, String productId) {
        int rank = 11;
        Element div = doc.getElementById("reviews");
        Element ul = div.getElementsByTag("ul").first();
        String matchString = "/listing/" + productId + "/";
        SimpleDateFormat smt = new SimpleDateFormat("dd MMM, yyyy");
        long maxWeekDifference = 0;
        int totalProductReviews = 0;
        System.out.println("size:" + ul.getElementsByTag("li").size());
        if (ul.getElementsByTag("li").isEmpty()) {
            return 12;
        }
        //   System.out.println("size:" + ul.getElementsByTag("li").size());
        for (Element li : ul.getElementsByTag("li")) {
            if (li.getElementsByClass("review-text-container").isEmpty()) {
                rank = -1; //Item not found
                // System.out.println("Item not found:" + li.html());
            } else {
                Element revieContainer = li.getElementsByClass("review-text-container").first();
                //  System.out.println("url:" + revieContainer.getElementsByTag("a").first().attr("href"));
                //    System.out.println("url:" + revieContainer.getElementsByClass("flag").first().attr("href"));
                //     System.out.println("Match:" + matchString);
                //  Element flag = revieContainer.getElementsByClass("flag").first();

                if (revieContainer.getElementsByClass("flag").isEmpty()) {
                    //System.out.println("Flag is null");
                    // System.out.println(""+revieContainer.html());
                    //Do Nothing as there is no link to product
                } else if (revieContainer.getElementsByClass("flag").first().attr("href").contains(matchString)) {
                    if (!li.getElementsByClass("flag-body").isEmpty()) {
                        try {
                            String date = li.getElementsByClass("flag-body").html();
                            //  System.out.println("Date:"+date);
                            //  System.out.println("================");
                            //   System.out.println(""+StringUtils.substringAfter(date,"</"));
                            date = StringUtils.substringBetween(date, "</", "<div");
                            date = date.replace("a>", "");
                            date = date.replace("span>", "");
                            date = date.trim();
                            Date feedBackDate = smt.parse(date);

                            Date today = new Date(System.currentTimeMillis());
                            //           System.out.println("feedback Date:" + feedBackDate + " today:" + today);

                            Instant start = feedBackDate.toInstant();
                            Instant end = today.toInstant();

                            //Weeks week=Weeks.weeksBetween(start, end);
                            LocalDateTime startDate = LocalDateTime.ofInstant(start, ZoneId.systemDefault());
                            LocalDateTime endDate = LocalDateTime.ofInstant(end, ZoneId.systemDefault());

                            long diff = ChronoUnit.WEEKS.between(startDate, endDate);
                            //            System.out.println("Weeks:" + diff);
                            totalProductReviews++;
                            if (diff > maxWeekDifference) {
                                maxWeekDifference = diff;
                            }
                        } catch (ParseException ex) {
                            Logger.getLogger(DetailScraping.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        if (totalProductReviews == 4) {
            if (maxWeekDifference == 0) {
                rank = 1;
            } else if (maxWeekDifference == 1) {
                rank = 2;
            } else if (maxWeekDifference == 2) {
                rank = 3;
            } else if (maxWeekDifference == 3) {
                rank = 4;
            } else if (maxWeekDifference > 3 && maxWeekDifference <= 7) {
                rank = 5;
            } else if (maxWeekDifference > 7 && maxWeekDifference <= 11) {
                rank = 6;
            } else if (maxWeekDifference > 11 && maxWeekDifference <= 15) {
                rank = 7;
            } else {
                rank = 8;
            }
        } else {
            System.out.println("total product reviews:" + totalProductReviews + " and product Id:" + productId);
            if (rank != -1) {
                if (totalProductReviews == 0) {
                    rank = 0;
                } else {
                    rank = 8;
                }
            }
        }

        System.out.println("Rank=" + rank);
        return rank;
    }
}
