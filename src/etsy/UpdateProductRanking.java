/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

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
 * @author Khushbu Bhumbhani
 */
public class UpdateProductRanking implements Runnable {

    String id;
    String url;
    String productId;

    public UpdateProductRanking(String id, String url, String productId) {
        this.id = id;
        this.url = url;
        this.productId = productId;
        UpdateProductRankingCrawler.CURRENT_THREAD_COUNT++;
    }

    @Override
    public void run() {
        updateRank();
        UpdateProductRankingCrawler.CURRENT_THREAD_COUNT--;

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
            if (div == null) {
                System.out.println("Item sold out.... " + url);
                Element divContent = doc.getElementById("content");
                if (divContent == null || divContent.getElementsByClass("parent-hover-underline").isEmpty()) {
                    System.out.println("Div not found " + url);
                    /*   String updateQuery = "update etsy.product_master_dec_2020 set product_rank=-1"
                            + " where product_master_id=" + id;
                    MyConnection.getConnection("etsy");
                    MyConnection.insertData(updateQuery);
                    System.out.println("Updated!!");*/
                    return;
                }

                String newUrl = divContent.getElementsByClass("parent-hover-underline").first().attr("href");
                System.out.println("new URL:" + newUrl);
                Thread.sleep(2000);
                doc = Jsoup.connect(newUrl)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(0).get();
            }
            int productRanking = getProductRankingLatest(doc, productId);
            String updateQuery = "update etsy.product_master_dec_2020 set product_rank=" + productRanking
                    + " where product_master_id=" + id;
            MyConnection.getConnection("etsy");
            MyConnection.insertData(updateQuery);
            System.out.println("Updated!!");
        } catch (IOException ex) {
            Logger.getLogger(UpdateProductRanking.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(UpdateProductRanking.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getProductRankingLatest(Document doc, String productId) {
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

    public int getProductRanking(Document doc, String productId) {
        int rank = -1;
        Element div = doc.getElementById("reviews");
        if (div != null && !div.getElementsByTag("ul").isEmpty()) {
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
                    System.out.println("Item not found:" + productId);
                } else {
                    Element revieContainer = li.getElementsByClass("review-text-container").first();

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
                                Logger.getLogger(DetailScraping.class.getName()).log(Level.SEVERE, null, ex);
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
        } else {
            System.out.println("Review div not found");
        }
        System.out.println("Rank=" + rank);
        return rank;

    }
}
