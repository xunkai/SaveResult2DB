package common;

public class BookLocation implements Comparable {
    public String areaNo;
    public int floorNo;
    public int columnNo;
    public int rowNo;
    public int shelfNo;
    public int layerNo;                         //层号,自上向下
    public int orderNo;
    public int totalNum;

    public BookLocation(String... location) {
        areaNo = location[0];
        floorNo = Integer.parseInt(location[1]);
        columnNo = Integer.parseInt(location[2]);
        rowNo = Integer.parseInt(location[3]);
        shelfNo = Integer.parseInt(location[4]);
        layerNo = Integer.parseInt(location[5]);
        if (location.length > 6) {
            orderNo = Integer.parseInt(location[6]);
        }
        if (location.length > 7) {
            totalNum = Integer.parseInt(location[7]);
        }
    }

    @Override
    public int compareTo(Object o) {
        BookLocation bl = (BookLocation) o;
        int re = 0;
        long i1 = 0, i2 = 0;
        //排序，从楼层号到书架层号
        if (floorNo == bl.floorNo) {
            if (columnNo == bl.columnNo) {
                if (rowNo == bl.rowNo) {
                    if (shelfNo == bl.shelfNo) {
                        if (layerNo == bl.layerNo) {
                            if (orderNo == bl.orderNo) re = 0;
                            else re = orderNo - bl.orderNo;
                        } else re = layerNo - bl.layerNo;
                    } else re = shelfNo - bl.shelfNo;
                } else re = rowNo - bl.rowNo;
            } else re = columnNo - bl.columnNo;
        } else re = floorNo - bl.floorNo;
        return re;
    }

    /**
     * 位置信息解码
     *
     * @param b
     * @return
     */
    public static String encodeLocation(BookLocation b) {
        return b.areaNo + " " + b.floorNo + " " + b.columnNo + " " + b.rowNo + " " + b.shelfNo + " " + b.layerNo;
    }

    public static BookLocation decodeLocation(String bookLocation) {
        String[] s = bookLocation.split(" ");
        BookLocation b = new BookLocation(s);
        return b;
    }
}
