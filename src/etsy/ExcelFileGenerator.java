/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import connectionManager.MyConnection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class ExcelFileGenerator {

    public static final String FILE_EXTN = ".xls";
    static String xlsLocation = "E:\\Freelancing\\etsy\\etsy-scan6\\";
    //  static String xlsLocation = "F:\\Khushbu\\etsy-scan5\\";
    static String mainid = "5";

    public static void main(String[] args) {

        if (args.length != 0) {
            mainid = args[0];
        }
        startGeneration();
    }

    private static void startGeneration() {
        //  String startId = "3101";
        //String endId = "3188";
        String selectCategory = "SELECT \n"
                + "sub_category_url,"
                + "   s.sub_category_master_id,\n"
                + "    sub_category_name,\n"
                + "    category_name \n"
                + "FROM\n"
                + "    product_master_dec_2020 p,\n"
                + "    product_url_master_dec_2020 u,\n"
                + "   sub_category_master_2020 s,\n"
                + "    main_category_master m\n"
                + "WHERE\n"
                + "    p.url_id = u.url_master_id and\n"
                + "    u.sub_category_id=s.sub_category_master_id and \n"
                + "    s.main_category_id=m.main_category_master_id\n"
                //  + "    and seller_name!=''\n"
                + " and s.main_category_id=" + mainid
                //  + "   and s.sub_category_master_id between " + startId + " and " + endId + "\n"
                + " group by   sub_category_url\n"
                + "    ;";

        MyConnection.getConnection("etsy");
        ResultSet rscat = MyConnection.getResultSet(selectCategory);
        try {
            while (rscat.next()) {
                String catId = rscat.getString("s.sub_category_master_id");
                String catName = rscat.getString("sub_category_name");
                String catUrl = rscat.getString("sub_category_url");
                String idList = "";

                String str = StringUtils.substringBetween(catUrl, "https://www.etsy.com/uk/c/", "?explicit=1");
                // String splits[]=str.split("/");
                if (str.contains("/")) {
                    str = StringUtils.substringBeforeLast(str, "/");
                }
                String folderPath = str.replaceAll("/", "\\\\");
                String savePath = xlsLocation + "\\" + folderPath + "\\";
                File dir = new File(savePath);
                dir.mkdirs();
                Workbook workbook = new HSSFWorkbook();
                //===================================================================
                //Make Sheet for ranking
                String selectProduct = "SELECT \n"
                        + "    product_master_id,\n"
                        + "    product_name,\n"
                        + "    price,\n"
                        + "    no_of_fav ,\n"
                        + "    total_product_sales ,\n"
                        + "    isClickable ,\n"
                        + "    seller_name ,\n"
                        + "    product_rank ,\n"
                        + "    url,\n"
                        + "    date_scanned ,\n"
                        //     + "    date(date_scanned),\n"
                        + "    sub_category_name ,\n"
                        + "    category_name \n"
                        + "FROM\n"
                        + "    product_master_dec_2020 p,\n"
                        + "    product_url_master_dec_2020 u,\n"
                        + "   sub_category_master_2020 s,\n"
                        + "    main_category_master m\n"
                        + "WHERE\n"
                        + "    p.url_id = u.url_master_id and\n"
                        + "    u.sub_category_id=s.sub_category_master_id and \n"
                        + "    s.main_category_id=m.main_category_master_id\n"
                        //     + "    and product_name!=''\n"
                        + "    and s.sub_category_master_id=" + catId + "\n"
                        + "    order by product_rank;";
                ResultSet rs = MyConnection.getResultSet(selectProduct);
                Sheet sheet = workbook.createSheet("sheet1");
                int rowNum = 0;
                Row firstRow = sheet.createRow(rowNum++);
                firstRow.createCell(0).setCellValue("Name");
                firstRow.createCell(1).setCellType(Cell.CELL_TYPE_NUMERIC);
                firstRow.createCell(1).setCellValue("Price");
                firstRow.createCell(2).setCellValue("No of Favourites");
                firstRow.createCell(3).setCellValue("Total Product Sales");
                firstRow.createCell(4).setCellValue("Clickable");
                firstRow.createCell(5).setCellValue("Rank");
                firstRow.createCell(6).setCellValue("Seller Name");
                firstRow.createCell(7).setCellValue("URL");
                firstRow.createCell(8).setCellValue("Date Scanned");
                firstRow.createCell(9).setCellValue("Sub Category");
                firstRow.createCell(10).setCellValue("Main Category");

                while (rs.next()) {
                    Row currentRow = sheet.createRow(rowNum++);
                    currentRow.createCell(0).setCellValue(rs.getString("product_name"));
                    currentRow.createCell(1).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(1).setCellValue(rs.getString("price"));
                    currentRow.createCell(2).setCellValue(rs.getString("no_of_fav"));
                    currentRow.createCell(3).setCellValue(rs.getString("total_product_sales"));
                    currentRow.createCell(4).setCellValue(rs.getString("isClickable"));
                    currentRow.createCell(5).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(5).setCellValue(rs.getInt("product_rank"));
                    currentRow.createCell(6).setCellValue(rs.getString("seller_name"));
                    currentRow.createCell(7).setCellValue(rs.getString("url"));
                    currentRow.createCell(8).setCellValue(rs.getString("date_scanned"));
                    //  currentRow.createCell(8).setCellValue(rs.getString("date(date_scanned)"));
                    currentRow.createCell(9).setCellValue(rs.getString("sub_category_name"));
                    currentRow.createCell(10).setCellValue(rs.getString("category_name"));

                    //idList = idList + rs.getInt("product_master_dec_2020_id") + ",";
                }

                //==============================================================
                //  Make Sheet for Pricing
                selectProduct = "SELECT \n"
                        + "    product_master_id,\n"
                        + "    product_name,\n"
                        + "    price,\n"
                        + "    no_of_fav ,\n"
                        + "    total_product_sales ,\n"
                        + "    isClickable ,\n"
                        + "    seller_name ,\n"
                        + "    product_rank ,\n"
                        + "    url,\n"
                        + "    date_scanned ,\n"
                        //   + "    date(date_scanned) ,\n"
                        + "    sub_category_name ,\n"
                        + "    category_name \n"
                        + "FROM\n"
                        + "    product_master_dec_2020 p,\n"
                        + "    product_url_master_dec_2020 u,\n"
                        + "   sub_category_master_2020 s,\n"
                        + "    main_category_master m\n"
                        + "WHERE\n"
                        + "    p.url_id = u.url_master_id and\n"
                        + "    u.sub_category_id=s.sub_category_master_id and \n"
                        + "    s.main_category_id=m.main_category_master_id\n"
                        //       + "    and product_name!=''\n"
                        + "    and s.sub_category_master_id=" + catId + "\n"
                        + "    order by price desc;";
                rs = MyConnection.getResultSet(selectProduct);
                sheet = workbook.createSheet("sheet2");
                rowNum = 0;
                firstRow = sheet.createRow(rowNum++);

                firstRow.createCell(0).setCellValue("Name");
                firstRow.createCell(1).setCellType(Cell.CELL_TYPE_NUMERIC);
                firstRow.createCell(1).setCellValue("Price");
                firstRow.createCell(2).setCellValue("No of Favourites");
                firstRow.createCell(3).setCellValue("Total Product Sales");
                firstRow.createCell(4).setCellValue("Clickable");
                /*  firstRow.createCell(5).setCellValue("Seller Name");
                firstRow.createCell(6).setCellValue("URL");
                firstRow.createCell(7).setCellValue("Date Scanned");
                firstRow.createCell(8).setCellValue("Sub Category");
                firstRow.createCell(9).setCellValue("Main Category");*/
                firstRow.createCell(5).setCellValue("Rank");
                firstRow.createCell(6).setCellValue("Seller Name");
                firstRow.createCell(7).setCellValue("URL");
                firstRow.createCell(8).setCellValue("Date Scanned");
                firstRow.createCell(9).setCellValue("Sub Category");
                firstRow.createCell(10).setCellValue("Main Category");
                while (rs.next()) {
                    Row currentRow = sheet.createRow(rowNum++);

                    currentRow.createCell(0).setCellValue(rs.getString("product_name"));
                    currentRow.createCell(1).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(1).setCellValue(rs.getString("price"));
                    currentRow.createCell(2).setCellValue(rs.getString("no_of_fav"));
                    currentRow.createCell(3).setCellValue(rs.getString("total_product_sales"));
                    currentRow.createCell(4).setCellValue(rs.getString("isClickable"));
                    /* currentRow.createCell(5).setCellValue(rs.getString("seller_name"));
                    currentRow.createCell(6).setCellValue(rs.getString("url"));
                    currentRow.createCell(7).setCellValue(rs.getString("date_scanned"));
                    currentRow.createCell(8).setCellValue(rs.getString("sub_category_name"));
                    currentRow.createCell(9).setCellValue(rs.getString("category_name"));*/
                    currentRow.createCell(5).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(5).setCellValue(rs.getInt("product_rank"));
                    currentRow.createCell(6).setCellValue(rs.getString("seller_name"));
                    currentRow.createCell(7).setCellValue(rs.getString("url"));
                    currentRow.createCell(8).setCellValue(rs.getString("date_scanned"));
                    // currentRow.createCell(8).setCellValue(rs.getString("date(date_scanned)"));
                    currentRow.createCell(9).setCellValue(rs.getString("sub_category_name"));
                    currentRow.createCell(10).setCellValue(rs.getString("category_name"));
                    idList = idList + rs.getInt("product_master_id") + ",";
                }

                if (!idList.equals("")) {
                    idList = idList.substring(0, idList.length() - 1);
                }
                System.out.println("" + catName + " ; " + (rowNum - 1));

                FileOutputStream fileOutputStream = new FileOutputStream(savePath.trim() + "\\" + catName + FILE_EXTN);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                if (!idList.equals("")) {
                    String updateQuery = "update etsy.product_master_dec_2020 set isDeliveredToClient=1 where product_master_id in (" + idList + ")";
                    MyConnection.insertData(updateQuery);

                }
            }
            //System.out.println("Id list"+idList);

        } catch (SQLException ex) {
            Logger.getLogger(ExcelFileGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ExcelFileGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ExcelFileGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
