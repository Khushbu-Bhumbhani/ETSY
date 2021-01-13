/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etsy;

import connectionManager.MyConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 *
 * @author Khushbu Bhumbhani
 */
public class CreateHotFile {

    public static final String FILE_EXTN = ".xls";
    static String xlsLocation = "E:\\Freelancing\\etsy\\etsy-scan6\\";
   // static String xlsLocation = "F:\\Khushbu\\etsy-scan5\\";
    static String mainCatId = "2,4,6,7,10";

    public static void main(String[] args) {
        if (args.length != 0) {
            mainCatId = args[0];
        }
        startGeneration();
    }

    private static void startGeneration() {
        String query = "SELECT\n"
                + " product_name,\n"
                + "  price,\n"
                + "  no_of_fav ,\n"
                + "  total_product_sales ,\n"
                + "  isClickable ,\n"
                + "  seller_name ,\n"
                + "  product_rank ,\n"
                + "  url,\n"
                + "  date_scanned ,\n"
                //  + "  date(date_scanned) ,\n"
                + "  sub_category_name ,\n"
                + "  category_name \n"
                + "  FROM\n"
                + "  product_master_dec_2020 p,\n"
                + "  product_url_master_dec_2020 u,\n"
                + "  sub_category_master_2020 s,\n"
                + "  main_category_master m\n"
                + "  WHERE\n"
                + "  p.url_id = u.url_master_id and\n"
                + "  u.sub_category_id=s.sub_category_master_id and \n"
                + "  s.main_category_id=m.main_category_master_id\n"
                + "  and seller_name!=''\n"
                + "and product_rank in (1,2,3)\n"
                + "and main_category_master_id in (" + mainCatId + ") "
                + "  order by product_rank;";
        MyConnection.getConnection("etsy");
        ResultSet rs = MyConnection.getResultSet(query);
        String savePath = xlsLocation;
        File dir = new File(savePath);
        dir.mkdirs();
        Workbook workbook = new HSSFWorkbook();
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
        try {
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
                //    currentRow.createCell(8).setCellValue(rs.getString("date(date_scanned)"));
                currentRow.createCell(9).setCellValue(rs.getString("sub_category_name"));
                currentRow.createCell(10).setCellValue(rs.getString("category_name"));
            }
            FileOutputStream fileOutputStream = new FileOutputStream(savePath.trim() + "\\" + "HOT1" + FILE_EXTN);
            workbook.write(fileOutputStream);
            fileOutputStream.close();
            System.out.println("File created!");
        } catch (SQLException ex) {
            Logger.getLogger(CreateHotFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CreateHotFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
