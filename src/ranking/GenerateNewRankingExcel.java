/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ranking;

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
 * @author Khushbu
 */
public class GenerateNewRankingExcel {

    public static final String FILE_EXTN = ".xls";
    static String xlsLocation = "E:\\Freelancing\\etsy\\etsy-ranking-2\\";
    static int main_Category_ID = 1;

    public static void main(String[] args) {
        startExcelfileGeneration();
    }

    private static void startExcelfileGeneration() {
        String selectQuery = "select distinct folder_path from product_master"
                + " where main_category_id=" + main_Category_ID;
        //  + " AND folder_path='weddings>invitations-and-paper>Invitations'";
        MyConnection.getConnection("etsy");
        ResultSet rsCat = MyConnection.getResultSet(selectQuery);

        try {
            while (rsCat.next()) {
                String category = rsCat.getString("folder_path");
                System.out.println("" + category);
                String folderPath = StringUtils.substringBeforeLast(category, ">");
                String fileName = StringUtils.substringAfterLast(category, ">");
                folderPath = folderPath.replace(">", "\\");
                String savePath = xlsLocation + "\\" + folderPath + "\\";
                String idList = "";
                File dir = new File(savePath);
                dir.mkdirs();
                //DecimalFormat df = new DecimalFormat(".##");
                Workbook workbook = new HSSFWorkbook();
                String selectProduct = "SELECT \n"
                        + "    product_master_id,\n"
                        + "    product_name,\n"
                        + "    price,\n"
                        + "    no_of_fav,\n"
                        + "    product_rank,\n"
                        + "    total_product_sales,\n"
                        + "    seller_name,\n"
                        + "    isClickable,\n"
                        + "    url,\n"
                        + "    category_name,\n"
                        + "    folder_path,\n"
                        + "    new_rank,\n"
                        + "    date_scanned,\n"
                        + "    new_total_product_sales,"
                        + "    new_no_of_fav"
                        + "    FROM\n"
                        + "    product_master m,\n"
                        + "    product_url_master u,\n"
                        + "    main_category_master c\n"
                        + "WHERE\n"
                        + "    m.url_id = u.url_master_id\n"
                        + "        AND m.main_category_id = c.main_category_master_id\n"
                        + "        AND main_category_id = " + main_Category_ID + "\n"
                        + "        AND product_name != ''\n"
                        + "        and folder_path='" + category.replaceAll("'", "''") + "'\n"
                        + "ORDER BY new_rank;";
                // System.out.println("" + selectProduct);
                ResultSet rs = MyConnection.getResultSet(selectProduct);
                Sheet sheet = workbook.createSheet("sheet1");
                int rowNum = 0;
                Row firstRow = sheet.createRow(rowNum++);
                firstRow.createCell(0).setCellValue("Name");
                //firstRow.createCell(1).setCellType(Cell.CELL_TYPE_NUMERIC);
                firstRow.createCell(1).setCellValue("Price");
                firstRow.createCell(2).setCellValue("No of Favourites");
                firstRow.createCell(3).setCellValue("New No of Favourites");
                firstRow.createCell(4).setCellValue("Old/New No of Favourites");
                firstRow.createCell(5).setCellValue("Total Product Sales");
                firstRow.createCell(6).setCellValue("New Total Product Sales");
                firstRow.createCell(7).setCellValue("Old/New Total Product Sales");
                firstRow.createCell(8).setCellValue("Clickable");
                firstRow.createCell(9).setCellValue("Previous_Rank");
                firstRow.createCell(10).setCellValue("New_Rank");
                firstRow.createCell(11).setCellValue("Rank Up/Down");
                firstRow.createCell(12).setCellValue("Seller Name");
                firstRow.createCell(13).setCellValue("URL");
                firstRow.createCell(14).setCellValue("Date Scanned");
                firstRow.createCell(15).setCellValue("Sub Category");
                firstRow.createCell(16).setCellValue("Main Category");
                while (rs.next()) {
                    Row currentRow = sheet.createRow(rowNum++);
                    currentRow.createCell(0).setCellValue(rs.getString("product_name"));
                    currentRow.createCell(1).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(1).setCellValue(rs.getString("price"));
                    String noOfFav = rs.getString("no_of_fav");
                    float newNoOfFav = rs.getInt("new_no_of_fav");
                    float OldNoOfFav = convertToNumber(noOfFav);
                    currentRow.createCell(2).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(2).setCellValue(OldNoOfFav);
                    currentRow.createCell(3).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(3).setCellValue(newNoOfFav);
                    currentRow.createCell(4).setCellType(Cell.CELL_TYPE_NUMERIC);
                    if (newNoOfFav != 0) {
                        double no = (OldNoOfFav / newNoOfFav);
                        // System.out.println(""+no);
                        no = Math.round(no * 100.00) / 100.00;
                        currentRow.createCell(4).setCellValue(no);
                    } else {
                        currentRow.createCell(4).setCellValue("");

                    }
                    String totalSales = rs.getString("total_product_sales");
                    float newTotalSales = rs.getInt("new_total_product_sales");
                    float oldTotalSales = convertToNumber(totalSales);
                    currentRow.createCell(5).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(5).setCellValue(oldTotalSales);
                    currentRow.createCell(6).setCellType(Cell.CELL_TYPE_NUMERIC);
                    currentRow.createCell(6).setCellValue(newTotalSales);
                    currentRow.createCell(7).setCellType(Cell.CELL_TYPE_NUMERIC);
                    if (newTotalSales != 0) {
                        double no = oldTotalSales / newTotalSales;
                        no = Math.round(no * 100.00) / 100.00;
                        currentRow.createCell(7).setCellValue(no);
                    } else {
                        currentRow.createCell(7).setCellValue("");
                    }
                    currentRow.createCell(8).setCellValue(rs.getString("isClickable"));
                    currentRow.createCell(9).setCellType(Cell.CELL_TYPE_NUMERIC);
                    int prev_rank = rs.getInt("product_rank");
                    currentRow.createCell(9).setCellValue(prev_rank);
                    currentRow.createCell(10).setCellType(Cell.CELL_TYPE_NUMERIC);
                    int new_rank = rs.getInt("new_rank");
                    String status = "";

                    if (new_rank == prev_rank) {
                        status = "No Change";
                    } else if (new_rank > prev_rank) {
                        status = "DOWN";
                    } else {
                        status = "UP";
                    }
                    if (new_rank == -1 && prev_rank != -1) {
                        status = "DOWN";
                    }
                    currentRow.createCell(10).setCellValue(new_rank);

                    currentRow.createCell(11).setCellValue(status);
                    currentRow.createCell(12).setCellValue(rs.getString("seller_name"));
                    currentRow.createCell(13).setCellValue(rs.getString("url"));
                    currentRow.createCell(14).setCellValue(rs.getString("date_scanned"));
                    //  currentRow.createCell(8).setCellValue(rs.getString("date(date_scanned)"));
                    currentRow.createCell(15).setCellValue(fileName);
                    currentRow.createCell(16).setCellValue(rs.getString("category_name"));
                    idList = idList + rs.getInt("product_master_id") + ",";

                }
                idList = idList.substring(0, idList.length() - 1);
           //     System.out.println("" + category);
                FileOutputStream fileOutputStream = new FileOutputStream(savePath.trim() + "\\" + fileName + FILE_EXTN);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                String updateQuery = "update etsy.product_master set isDeliveredToClient=1 where product_master_id in (" + idList + ")";
                MyConnection.insertData(updateQuery);
            }
        } catch (SQLException ex) {
            Logger.getLogger(GenerateNewRankingExcel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GenerateNewRankingExcel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GenerateNewRankingExcel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static int convertToNumber(String str) {
        str = str.replaceAll("[^\\d.]", "").trim();
        if (str.equals("")) {
            str = "0";
        }
        return Integer.parseInt(str);
    }
}
