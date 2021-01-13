/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class Test {

    public static void main(String[] args) {
        //DetailScraping dc = new DetailScraping("https://www.etsy.com/in-en/listing/603949420/first-wedding-anniversary-card?show_sold_out_detail=1&bes=1"
        UpdateProductRanking upd=new UpdateProductRanking(
                "5678", //product master id
                "https://www.etsy.com/in-en/listing/573982944/personalised-wooden-guest-book-romantic?ga_order=most_relevant&ga_search_type=all&ga_view_type=gallery&ga_search_query=&ref=sc_gallery-12-6&plkey=41f863906b9b4820bae490ef328c14eb7ab91437%3A573982944&bes=1",
                "573982944"); //product id*/
        Thread thread = new Thread(upd);
        thread.start();
    }
}
