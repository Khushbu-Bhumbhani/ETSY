/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import connectionManager.MyConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author Dell
 */
public class AddProductsUrlsFromScrapedFiles {

    static int subCatId = 16;
    static int main_catId = 15;
    static String categoryFolderName = "weddings";

    public static void main(String[] args) {
        startURLCollection();
    }

    private static void startURLCollection() {

        try {
            // List<File> filesInFolder = Files.walk(Paths.get("E:\\Freelancing\\etsy\\etsy-ranking\\" + categoryFolderName + "\\"))
            List<File> filesInFolder = Files.walk(Paths.get("E:\\Freelancing\\etsy\\etsy-ranking\\"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            System.out.println("Total Files:" + filesInFolder.size());
            Iterator<File> iterator = filesInFolder.iterator();
            while (iterator.hasNext()) {
                File file = iterator.next();
                System.out.println("File Name:" + file.getName());
                // readExcelAndGetProductURLs(file);
                UpdatFoldePathinDB(file);
                // break;
            }
        } catch (IOException ex) {
            Logger.getLogger(AddProductsUrlsFromScrapedFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        MyConnection.getConnection("etsy");

    }

    private static void readExcelAndGetProductURLs(File file) {
        FileInputStream fin;
        try {
            fin = new FileInputStream(file);
            HSSFWorkbook workBook = new HSSFWorkbook(fin);

            HSSFSheet sheet = workBook.getSheet("sheet1");
            String insertQuery = "insert into `etsy`.`product_url_master` (url_master_id,`url`,\n"
                    + "`product_id`,\n"
                    + "`sub_category_id`,\n"
                    + "`page_url`,\n"
                    + "`is_scraped`) values ";
            String insertProductQuery = "INSERT INTO `etsy`.`product_master`\n"
                    + "(`product_name`,`price`,`no_of_fav`,`url_id`,`product_rank`,\n"
                    + "`seller_name`,`total_product_sales`,\n"
                    + "`isClickable`,`first_run_date`,main_category_id)\n"
                    + "values ";
            int lastd = getLastIdFromUrlMaster();
            if (lastd == 0) {
                lastd = 1;
            } else {
                lastd++;
            }
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                Cell cell = row.getCell(CellReference.convertColStringToIndex("H"));
                String url = cell.getStringCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("A"));
                String name = cell.getStringCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("B"));
                String price = cell.getStringCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("C"));
                String no_of_fav = cell.getStringCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("D"));
                String total_productsell = cell.getStringCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("E"));
                String isclickable = cell.getStringCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("F"));
                int rank = (int) cell.getNumericCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("G"));
                String sellerName = cell.getStringCellValue();
                cell = row.getCell(CellReference.convertColStringToIndex("I"));
                String firstScanDate = cell.getStringCellValue();

                String productId = StringUtils.substringBetween(url, "https://www.etsy.com/in-en/listing/", "/");
                insertQuery = insertQuery + "(" + lastd
                        + ",'" + url + "','" + productId + "'," + subCatId + ",'',1"
                        + "),";
                insertProductQuery = insertProductQuery + "("
                        + "'" + prepareString(name) + "'," + (price.equals("") ? "NULL" : price) + ",'" + no_of_fav + "'," + lastd + "," + rank
                        + ",'" + prepareString(sellerName) + "','" + total_productsell + "','" + isclickable + "','" + firstScanDate + "'"
                        + "," + main_catId
                        + "),";
                lastd++;
            }
            insertQuery = insertQuery.substring(0, insertQuery.length() - 1);
            insertProductQuery = insertProductQuery.substring(0, insertProductQuery.length() - 1);
            MyConnection.getConnection("etsy");
            MyConnection.insertData(insertQuery);
            MyConnection.insertData(insertProductQuery);
            System.out.println("Inserted!!");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AddProductsUrlsFromScrapedFiles.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AddProductsUrlsFromScrapedFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static int getLastIdFromUrlMaster() {
        MyConnection.getConnection("etsy");
        String selectQuery = "select max(url_master_id) from product_url_master";
        ResultSet rs = MyConnection.getResultSet(selectQuery);
        try {
            if (rs.next()) {
                return rs.getInt("max(url_master_id)");
            } else {
                return 1;
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddProductsUrlsFromScrapedFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public static String prepareString(String str) {
        if (str != null) {
            str = str.replaceAll("\'", "\''");
        } else {
            return "";
        }
        return str;
    }

    private static void UpdatFoldePathinDB(File file) {
        try {
            FileInputStream fin;

            fin = new FileInputStream(file);
            HSSFWorkbook workBook = new HSSFWorkbook(fin);

            HSSFSheet sheet = workBook.getSheet("sheet1");
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                Cell cell = row.getCell(CellReference.convertColStringToIndex("H"));
                String url = cell.getStringCellValue();
                int product_master_id = getProductMasterId(url);
              //  System.out.println(""+product_master_id);
                String filePath=file.getAbsolutePath();
                filePath=filePath.replace("E:\\Freelancing\\etsy\\etsy-ranking\\", "");
                filePath=filePath.replace(".xls", "");
                filePath=filePath.replace("\\", ">");
                filePath=filePath.replaceAll("'", "''");
                System.out.println(""+filePath);
                String updateQuery="update product_master set folder_path='"+filePath+"' where product_master_id="+product_master_id;
                MyConnection.getConnection("etsy");
                MyConnection.insertData(updateQuery);
                //System.out.println("");
                

            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AddProductsUrlsFromScrapedFiles.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AddProductsUrlsFromScrapedFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static int getProductMasterId(String url) {
        String selectQ = "select product_master_id from product_master m , product_url_master u where m.url_id=u.url_master_id and url='" + url + "';";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(selectQ);
        try {
            if (rs.next()) {
                return rs.getInt("product_master_id");
            } else {
                return -1;
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddProductsUrlsFromScrapedFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
}
