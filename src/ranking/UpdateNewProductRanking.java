/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ranking;

import connectionManager.MyConnection;
import java.io.IOException;
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
 * @author Khushbu
 */
public class UpdateNewProductRanking implements Runnable {

    String productMasterid;
    String url;
    String productId;
    int urlId;

    public UpdateNewProductRanking(String productMasterid, String url, String productId, int urlId) {
        this.productMasterid = productMasterid;
        this.url = url;
        this.productId = productId;
        this.urlId = urlId;
        NewRankingCrawler.CURRENT_THREAD_COUNT++;
    }

    @Override
    public void run() {
        updateRank();
        NewRankingCrawler.CURRENT_THREAD_COUNT--;
    }

    private void updateRank() {
        try {
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .maxBodySize(0)
                    .get();
            Element div = doc.getElementById("reviews");
            String category = StringUtils.substringBetween(doc.html(), "\"category\":", "\",");
            if (category != null) {
                category = category.replace("\"", "").trim();
                category = category.replace("'", "''").trim();
            } else {
                category = "";
            }
            String seller_url = "";

            if (div == null) {
                System.out.println("Item sold out....");
                Element divContent = doc.getElementById("content");

                if (divContent == null || divContent.getElementsByClass("parent-hover-underline").isEmpty()) {
                    /*  if (!divContent.getElementsByClass("parent-hover-underline").isEmpty()) {
                        seller_url = divContent.getElementsByClass("parent-hover-underline").first().attr("href");
                        // seller_url=seller_url.replaceAll("'", "''");
                        System.out.println("" + seller_url);
                    }*/

                    System.out.println("Div not found");
                    String updateQuery = "update etsy.product_master set new_rank=-1, sub_category='" + category + "', seller_url_scraped='" + seller_url + "' "
                            + " where product_master_id=" + productMasterid;
                    MyConnection.getConnection("etsy");
                    MyConnection.insertData(updateQuery);
                    System.out.println("Updated!!");
                    String updateScrapeStatus = "update etsy.product_url_master set is_scraped=1 where url_master_id=" + urlId;
                    MyConnection.insertData(updateScrapeStatus);
                    return;
                }

                String newUrl = divContent.getElementsByClass("parent-hover-underline").first().attr("href");
                System.out.println("new URL:" + newUrl);
                doc = Jsoup.connect(newUrl)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(0).get();
                seller_url = newUrl;
            }
            int productRanking = getProductRanking(doc, productId);
            String noOfFav = "";
            if (!doc.getElementsContainingOwnText("favourites").isEmpty()) {
                for (Element e : doc.getElementsContainingOwnText("favourites")) {
                    if (e.parent().hasClass("list-inline-item") && e.tagName().equals("a")) {
                        noOfFav = e.text();
                    }
                }
                // noOfFav = html2text(noOfFav);
                noOfFav = noOfFav.replace("favourites", "").trim();
            }
            /* if(noOfFav.trim().equals(""))
            {
                
            }*/
            if (seller_url.equals("") && doc.getElementById("listing-page-cart") != null) {
                seller_url = doc.getElementById("listing-page-cart").getElementsByTag("a").first().attr("href");
                seller_url = seller_url.replaceAll("'", "''");
            }
            System.out.println("sler > " + seller_url);
            if (seller_url.length() > 345) {
                System.out.println("too long seller URL:" + seller_url);
                seller_url="";
                //return;
            }
            String updateQuery = "update etsy.product_master set new_rank=" + productRanking + ", sub_category='" + category + "', seller_url_scraped='" + seller_url + "' "
                    + ",new_no_of_fav=" + (noOfFav.trim().equals("") ? "NULL" : noOfFav) + " where product_master_id=" + productMasterid;

            MyConnection.getConnection("etsy");
            MyConnection.insertData(updateQuery);
            String updateScrapeStatus = "update etsy.product_url_master set is_scraped=1 where url_master_id=" + urlId;
            MyConnection.insertData(updateScrapeStatus);
            System.out.println("Updated!!");

        } catch (IOException ex) {
            Logger.getLogger(UpdateNewProductRanking.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("URL error:" + url);
        }
    }

    public int getProductRanking(Document doc, String productId) {
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
        for (Element li : ul.getElementsByTag("li")) {
            if (li.getElementsByClass("review-text-container").isEmpty()) {
                rank = -1; //Item not found
                System.out.println("Item not found:" + url);
                //   System.out.println("Item not found:" + li.html());
            } else {
                Element revieContainer = li.getElementsByClass("review-text-container").first();
                //  System.out.println("url:" + revieContainer.getElementsByTag("a").first().attr("href"));
                //    System.out.println("url:" + revieContainer.getElementsByClass("flag").first().attr("href"));
                //     System.out.println("Match:" + matchString);
                //  Element flag = revieContainer.getElementsByClass("flag").first();

                if (revieContainer.getElementsByClass("flag").isEmpty()) {
                    System.out.println("Flag is null");
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
                            Logger.getLogger(UpdateNewProductRanking.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        System.out.println("Flag body is empty");
                    }

                } else {
                    //System.out.println("No match:" + matchString + "\n" + revieContainer.getElementsByClass("flag").first().attr("href"));
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

    public String html2text(String html) {
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
