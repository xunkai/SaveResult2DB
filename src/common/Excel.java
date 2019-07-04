package common;

import jxl.*;
import jxl.format.CellFormat;
import jxl.format.PageOrientation;
import jxl.format.PaperSize;
import jxl.write.*;
import jxl.write.biff.RowsExceededException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.MyLogger.LOGGER;
import static utils.MyLogger.getTrace;

/**
 * Created by Wing on 2019/6/4
 * <p>
 * Project: SaveResult2DB
 */
public class Excel {

    /**
     * 表格列宽
     */
    private static int[] EXCEL_LENGTH = {20, 25, 80, 6, 6, 6, 6, 6, 15, 15};

    private WritableWorkbook book = null;
    private int sheetNum = 0;

    public Excel(String path) {
        try {
            book = Workbook.createWorkbook(new File(path));
        } catch (IOException e) {
            LOGGER.error(getTrace(e));
        }
    }

    public void createNewSheet(String sheetName) {
        book.createSheet(sheetName, sheetNum);
        sheetNum++;
    }


    /**
     * 向名为sheetName的sheet中添加一行
     *
     * @param isTitle   是否为标题行
     * @param sheetName sheet名
     * @param labels    写入的内容
     */
    public void addLine(boolean isTitle, String sheetName, String... labels) {
        WritableCellFormat format = isTitle ? getTitleFormat() : getContentFormat();
        WritableSheet sheet = book.getSheet(sheetName);
        if (sheet == null) {
            createNewSheet(sheetName);
            sheet = book.getSheet(sheetName);
        }
        int rowNo = sheet.getRows();
        try {
            for (int i = 0; i < labels.length; i++) {
                Label l = new Label(i, rowNo, labels[i], format);
                sheet.addCell(l);
            }
        } catch (WriteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向第i个sheet中添加一行
     *
     * @param isTitle 是否为标题
     * @param index   sheet下标
     * @param labels  写入的内容
     */
    public void addLine(boolean isTitle, int index, String... labels) {
        if (book.getNumberOfSheets() > index) {
            String sheetName = book.getSheetNames()[index];
            addLine(isTitle, sheetName, labels);
        } else {
            LOGGER.warn("下标" + index + "超过了Excel的sheet数量：" + book.getNumberOfSheets());
        }
    }

    /**
     * 向名为sheetName的sheet中添加一行
     *
     * @param isTitle   是否为标题行
     * @param sheetName sheet名
     * @param labels    写入的内容
     */
    public void addLine(boolean isTitle, String sheetName, List<String> labels) {
        addLine(isTitle, sheetName, labels.toArray(new String[labels.size()]));
    }

    public boolean hasSheet(String sheetName) {
        return book.getSheet(sheetName) != null;
    }

    public int getSheetRows(String sheetName) {
        return book.getSheet(sheetName) == null ? 0 : book.getSheet(sheetName).getRows();
    }

    public WritableCellFormat getTitleFormat() {
        //定义样式
        WritableCellFormat format = new WritableCellFormat();
        try {
            format.setAlignment(Alignment.CENTRE);
            format.setBackground(jxl.format.Colour.GRAY_25);
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
        return format;
    }

    public WritableCellFormat getContentFormat() {
        //定义样式
        WritableCellFormat format = new WritableCellFormat();
        try {
            format.setWrap(true);
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
        return format;
    }

    public void setColumnView(String sheetName, int... lengths) {
        WritableSheet sheet = book.getSheet(sheetName);
        //表格格式设置，固定列宽
        for (int i = 0; i < lengths.length; i++) {
            sheet.setColumnView(i, lengths[i]);
        }
    }

    public void saveExcel() {
        try {
            book.write();
            book.close();
        } catch (Exception e) {
            LOGGER.error(e.getStackTrace());
        }
    }

}
