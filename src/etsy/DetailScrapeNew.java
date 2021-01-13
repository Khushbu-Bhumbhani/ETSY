/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import connectionManager.DBOperation;
import connectionManager.MyConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Khushbu
 */
// this scraper changes reviews to newest first and then scrapes its rank
public class DetailScrapeNew {

    static ChromeDriver driver = null;
    static String chromePath = "C:\\Users\\Khushbu\\Downloads\\chromedriver_win32(2)\\chromedriver.exe";
    // static String chromePath = "";
    static String mainId = "";

    public static void main(String[] args) {

        mainId = "2";
        if (args.length != 0) {
            mainId = args[0];
            chromePath = args[1];
        }
        System.setProperty("webdriver.chrome.driver", chromePath);
        startCrawler(mainId);

    }

    private static void startCrawler(String mainId) {
        try {
            String selectProductUrls = "select url_master_id,url,product_id,s.main_category_id from etsy.product_url_master_dec_2020 u, sub_category_master_2020 s\n"
                    + "                 where is_scraped=0\n"
                    + "                 and u.sub_category_id=s.sub_category_master_id\n"
                    + "                 and s.main_category_id=" + mainId + "";

            MyConnection.getConnection("etsy");
            ResultSet rs = MyConnection.getResultSet(selectProductUrls);
            ChromeOptions options = new ChromeOptions();
            // options.addArguments("--headless");
            driver = new ChromeDriver(options);
            //ChromeDriver driver1 = new ChromeDriver(options);
            int count = 1;
            while (rs.next()) {
                String url = rs.getString("url");
                String id = rs.getString("url_master_id");
                String productId = rs.getString("product_id");

                detailSCrape(url, id, productId);
                System.out.println("sleeping...");
                Thread.sleep(3000);
                System.out.println("Count:" + count++);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DetailScrapeNew.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(DetailScrapeNew.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void detailSCrape(String url, String id, String productId) {
        System.out.println("" + url);
        driver.get(url);

        /* int statusCode = new HttpResponseCode().httpResponseCodeViaGet(href);

            if(200 != statusCode) {
                System.out.println(href + " gave a response code of " + statusCode);
            }*/
        waitForJSandJQueryToLoad(driver);
        String actualTitle = driver.getTitle();
        Document doc = Jsoup.parse(driver.getPageSource());
        if (doc.text().contains("This shop is taking a short break.")) {
            System.out.println("Shop is taking break...");
            DBOperation.updateScrapeStatus(id, -1);
            return;
        } else if (doc.text().contains("Sorry, this item is unavailable.")) {
            System.out.println("Sorry, this item is unavailable....");
            DBOperation.updateScrapeStatus(id, -1);
            return;
        } else if (doc.text().contains("Sorry, this item and shop are currently unavailable")) {
            System.out.println("Sorry, this item and shop are currently unavailable");
            DBOperation.updateScrapeStatus(id, -1);
            return;
        } else if (doc.text().contains("Sorry, this item is sold out")) {
            System.out.println("Item sold out...finidng detail url...");
            if (!doc.getElementsContainingOwnText("See item details").isEmpty()) {
                String newURL = doc.getElementsContainingOwnText("See item details").first().attr("href");
                System.out.println("New URL->" + newURL);
                driver.get(newURL);
                waitForJSandJQueryToLoad(driver);

                doc = Jsoup.parse(driver.getPageSource());
                isAlertPresent();
                detailScrapeProduct(doc, id, productId);
                return;
            }
        }
        if (!doc.getElementsByClass("anchor-listing").isEmpty()
                && !doc.getElementsByClass("anchor-listing").first().getElementsByTag("a").isEmpty()) {
            String soldItemURL = doc.getElementsByClass("anchor-listing").first().getElementsByTag("a").first().attr("href");
            System.out.println("Sold item URL:" + soldItemURL);
            driver.get(url);
            waitForJSandJQueryToLoad(driver);
        }
        //  driver.findElementById("from").sendKeys("value","2020-11-01");

        //scroll to bottom
        JavascriptExecutor jse = (JavascriptExecutor) driver;

        jse.executeScript("scroll(400, 0)"); // if the element is on top.
        //  jse.executeScript("scroll(0, 250)"); // if the element is on bottom.*/
        System.out.println("Checking for alert...");
        isAlertPresent();

        if (!doc.getElementsByClass("sort-reviews-trigger").isEmpty()) {
            WebDriverWait wait = new WebDriverWait(driver, 200);
            wait.until(ExpectedConditions.elementToBeClickable(By.className("sort-reviews-trigger")));

            driver.findElementByClassName("sort-reviews-trigger").click();
            waitForJSandJQueryToLoad(driver);

            wait = new WebDriverWait(driver, 200);
            wait.until(ExpectedConditions.elementToBeClickable(By.className("reviews-sort-by-item")));
            System.out.println("Waiting till privacy setting thing goes out...");
            /*try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(DetailScrapeNew.class.getName()).log(Level.SEVERE, null, ex);
        }*/
            wait = new WebDriverWait(driver, 2000);
            wait.until(ExpectedConditions.invisibilityOfElementWithText(By.tagName("p"), "Privacy settings saved"));
            List<WebElement> sortButtons = driver.findElementsByClassName("reviews-sort-by-item");
            sortButtons.forEach(button -> {
                if (button.getText().contains("Newest")) {
                    WebDriverWait wait1 = new WebDriverWait(driver, 500);
                    wait1.until(ExpectedConditions.elementToBeClickable(button));
                    button.click();
                    // break;
                }
            });
            waitForJSandJQueryToLoad(driver);

            doc = Jsoup.parse(driver.getPageSource());
        } else {
            System.out.println("No reviwes found...scraping info..");
        }
        //System.out.println("D:" + doc.text());
        detailScrapeProduct(doc, id, productId);
    }

    public static boolean waitForJSandJQueryToLoad(ChromeDriver driver) {

        WebDriverWait wait = new WebDriverWait(driver, 30);

        // wait for jQuery to load
        ExpectedCondition<Boolean> jQueryLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    // return ((Long) ((JavascriptExecutor) getDriver()).executeScript("return jQuery.active") == 0);
                    return ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0").equals(true);
                } catch (Exception e) {
                    // no jQuery present
                    return true;
                }
            }
        };

        // wait for Javascript to load
        ExpectedCondition<Boolean> jsLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                // return ((JavascriptExecutor) getDriver()).executeScript("return document.readyState")
                //        .toString().equals("complete");
                return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
            }
        };

        return wait.until(jQueryLoad) && wait.until(jsLoad);
    }

    private static void detailScrapeProduct(Document doc, String id, String productId) {

        //   System.out.println("" + url);
        // Thread.sleep(1000);
        // System.out.println("text:"+doc.text());
        String name = "";
        String price = "";
        String noOfFav = "";
        String seller_name = "";
        String seller_url = "";

        if (!doc.getElementsByAttributeValue("data-component", "listing-page-title-component").isEmpty()) {
            name = doc.getElementsByAttributeValue("data-component", "listing-page-title-component").first().text().trim();
        }
        if (!doc.getElementsByAttributeValue("data-buy-box-region", "price").isEmpty()
                && !doc.getElementsByAttributeValue("data-buy-box-region", "price").first().getElementsByTag("p").isEmpty()) {
            price = doc.getElementsByAttributeValue("data-buy-box-region", "price").first().getElementsByTag("p").first().text().trim();
            price = price.replace("US$", "");
            price = price.replace("£", "");
            price = price.replaceAll(",", "");
            price = price.replace("+", "").trim();
            price = price.replaceAll("[^0-9.]", "");

        } else if (!doc.getElementsByAttributeValue("data-buy-box-region", "price").isEmpty()
                && !doc.getElementsByAttributeValue("data-buy-box-region", "price").first().getElementsByTag("h3").isEmpty()) {
            price = doc.getElementsByAttributeValue("data-buy-box-region", "price").first().getElementsByTag("h3").first().text().trim();
            price = price.replace("US$", "");
            price = price.replace("£", "");
            price = price.replaceAll(",", "");
            price = price.replace("+", "").trim();
            price = price.replaceAll("[^0-9.]", "");
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
                /*if (e.parent().hasClass("list-inline-item") && e.tagName().equals("a")) {
                        noOfFav = e.text();
                    }*/
                if (e.tagName().equals("a")) {
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
        if (DBOperation.insertProduct(name, price, noOfFav, id, seller_name, seller_url, rank, subcategory, Integer.parseInt(mainId))) {
            DBOperation.updateScrapeStatus(id, 1);
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

    public static int getProductRanking(Document doc, String productId) {
        int rank = 11;
        Element div = doc.getElementById("reviews");
        if (div == null) {
            return -1;
        }
        Element ul = div.getElementsByTag("ul").first();

        String matchString = "/listing/" + productId + "/";
        SimpleDateFormat smt = new SimpleDateFormat("dd MMM, yyyy");
        long maxWeekDifference = 0;
        int totalProductReviews = 0;
        // System.out.println("size:" + ul.getElementsByTag("li").size());
        if (ul.getElementsByTag("li").isEmpty()) {
            return 12;
        }
        for (Element li : ul.getElementsByTag("li")) {
            if (li.getElementsByClass("review-text-container").isEmpty()) {
                rank = -1; //Item not found
                System.out.println("Item not found:" + doc.baseUri());
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
                            Logger.getLogger(DetailScraping.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        System.out.println("Flag body is empty");
                    }

                } else {
                    //   System.out.println("No match:" + matchString + "\n" + revieContainer.getElementsByClass("flag").first().attr("href"));
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
            //  System.out.println("total product reviews:" + totalProductReviews + " and product Id:" + productId);
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

    public static boolean isAlertPresent() {
        try {
            /*driver.switchTo().alert();
            driver.switchTo().alert().accept();*/
            WebElement acceptBtn = driver.findElement(By.xpath(".//button[@data-gdpr-single-choice-accept='true']"));
            if (acceptBtn != null && acceptBtn.isDisplayed() && acceptBtn.isEnabled()) {
                acceptBtn.click();
                System.out.println("Sleeping...");
                Thread.sleep(10 * 1000);
                return true;
            }

        } // try 
        catch (NoAlertPresentException Ex) {
            return false;
        } catch (InterruptedException ex) {
            Logger.getLogger(DetailScrapeNew.class.getName()).log(Level.SEVERE, null, ex);
        }   // catch 
        return false;
    }
}
