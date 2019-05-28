package common;

/**
 * 图书索引号类
 *
 * @author Wing
 * @date 2018.07.19
 */
public class BookIndex implements Comparable {

    /**
     * 楼层起止索书号，分别对应2、3、4、5层
     */
    public static String[] FLOOR_BOOK_INDEX = {"A-E", "F-H", "I-K", "K-Z"};
    /**
     * 楼层起止索书号，分别对应2、3、4、5层
     */
    public static String[] FLOOR_START_INDEX = {"A", "F", "I", "K82"};
    /**
     * 除去类别号和著者号的其他信息起始值，其值为{@value}
     */
    private final int OTHERNO = 2;

    /**
     * 类别号
     */
    private ClassNo classNo;

    /**
     * 著者号
     */
    private AuthorNo authorNo;

    /**
     * 以/分割索书号的字符数组
     */
    private String[] lines;

    /**
     * lines数组的长度
     */
    private int lineNum;

    /**
     * @param bookIndex 索引号字符串
     */
    public BookIndex(String bookIndex) {
        if(bookIndex==null){
            bookIndex="";
        }
        lines = bookIndex.split("/");
        lineNum = lines.length;
        classNo = new ClassNo(lines[0]);
        if (lineNum >= OTHERNO) {
            authorNo = new AuthorNo(lines[1]);
        }
    }


    /**
     * 获取图书属于哪一层，在图书分隔有重合时，总是返回前一层
     *
     * @param bookIndex
     * @return
     */
    public static int getFloor(String bookIndex) {
        if(bookIndex == null){
            return 1;
        }
        int floor = -1;
        BookIndex b = new BookIndex(bookIndex);
        for (int i = 0; i < FLOOR_START_INDEX.length; i++) {
            BookIndex index = new BookIndex(FLOOR_START_INDEX[i]);
            if (b.compareTo(index) > 0) {
                floor = i;
            } else {
                break;
            }
        }
        return floor+2;
    }

    /**
     * 判断图书是否属于这一层
     *
     * @param bookIndex
     * @param floor
     * @return
     */
    public static boolean isFloor(String bookIndex, int floor) {
        return floor == getFloor(bookIndex);
    }
    /**
     * {@inheritDoc}
     * 比较两个索引号的大小
     *
     * @param o 索引号
     * @return 0为相等，正数为前者大，否则后者大
     */
    @Override
    public int compareTo(Object o) {
        BookIndex b2 = (BookIndex) o;
        int tmp = classNo.compareTo(b2.classNo);
        if (tmp != 0) {
            return tmp;
        }
        if (authorNo == null && b2.authorNo == null) {
            return 0;
        }
        if (authorNo == null) {
            return -1;
        }
        if (b2.authorNo == null) {
            return 1;
        }
        tmp = authorNo.compareTo(b2.authorNo);

        if (tmp != 0) {
            return tmp;
        }

        int count = 2;

        while ((lineNum >= count) && (b2.lineNum >= count)) {
            if ((lineNum == b2.lineNum) && (lineNum == count)) {
                return 0;
            }
            if ((lineNum == count) && (b2.lineNum > lineNum)) {
                return -1;
            }
            if ((b2.lineNum == count) && (b2.lineNum < lineNum)) {
                return -1;
            }

            VolumeOrYear vy1 = new VolumeOrYear(lines[count]);
            VolumeOrYear vy2 = new VolumeOrYear(b2.lines[count]);

            tmp = vy1.compareTo(vy2);

            if (tmp != 0) {
                return tmp;
            }

            count++;
        }

        return 0;
    }

    /**
     * Test Method
     *
     * @param args Param description
     */
    public static void main(String[] args) {
        System.out.println(getFloor("I78"));
//        List<String> bookIndexs = readFileByLine("data\\test_a.txt");
//        Collections.shuffle(bookIndexs);

//        List<BookIndex> bis = new ArrayList<>();
//        for (String bi : bookIndexs) {
//            bis.add(new BookIndex(bi));
//        }
//        Collections.sort(bis);
//        List<String> newStrs = new ArrayList<>();
//        for (BookIndex bi : bis) {
//            newStrs.add(bi.toString());
//        }
//        List<AuthorNo> bis = new ArrayList<>();
//        for (String bi : bookIndexs) {
//            bis.add(new AuthorNo(bi));
//        }
//        Collections.sort(bis);
//        List<String> newStrs = new ArrayList<>();
//        for (AuthorNo bi : bis) {
//            newStrs.add(bi.toString());
//        }
//
//        writeFile("data\\old_a.txt", false, bookIndexs);
//        writeFile("data\\new_a.txt", false, newStrs);
    }

    /**
     * {@inheritDoc}
     * 将索书号对象输出为字符串
     *
     * @return 索书号字符串
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        boolean isFirst = true;

        for (String line : lines) {
            if (isFirst) {
                str.append(line);
                isFirst = false;
            } else {
                str.append("/").append(line);
            }
        }

        return str.toString();
    }

    /**
     * 著者号类
     */
    private static class AuthorNo implements Comparable {

        /**
         * 著者号字符串
         */
        String anStr = "";

        /**
         * 著者号字符串长度
         */
        int len = 0;

        /**
         * Constructs ...
         *
         * @param authorNo Param description
         */
        AuthorNo(String authorNo) {
            anStr = authorNo;
            len = authorNo.length();
        }

        /**
         * 按照著者号规则来比较字符大小
         *
         * @param c1 字符1
         * @param c2 字符2
         * @return c1 &lt; c2 -1 <br> c1 == c2 0 <br> c1 &gt; c2 1
         */
        private int compareChar(char c1, char c2) {

//            String str1 = "abcdefghijklmnopqrstuvwxyz0123456789";
            String str2 = "()-+=0123456789abcdefghijklmnopqrstuvwxyz.";

            return str2.indexOf(c1) - str2.indexOf(c2);
        }

        @Override
        public String toString() {
            return anStr;
        }

        @Override
        public int compareTo(Object o) {
            AuthorNo authorNo1 = (AuthorNo) o;
            String s1 = anStr.toLowerCase(),
                    s2 = authorNo1.anStr.toLowerCase();

            s1 = s1.replaceAll("[^()-+=\\w\\d]", "");
            s2 = s2.replaceAll("[^()-+=\\w\\d]", "");

            int l1 = s1.length(),
                    l2 = s2.length();

            if (s1.compareTo(s2) == 0) {
                return 0;
            }

            int index1 = 0,
                    index2 = 0, tmp;

            // 是否在括号内
            boolean isIn = false;
            // 是否为第一段，字母在数字之前
            boolean isFirst = true,
                    isSecond = false;
            //第一段计数
            int countFirst = 0;
            while ((l1 > index1) && (l2 > index2)) {
                char c1 = s1.charAt(index1);
                char c2 = s2.charAt(index2);
                if (isFirst) {
                    if (Character.isLetter(c1) && Character.isLetter(c2)) {
                        countFirst++;
                        if (countFirst == 3) {
                            isFirst = false;
                            isSecond = true;
                        }
                    } else if (Character.isDigit(c1) && Character.isLetter(c2)) {
                        return 1;
                    } else if (Character.isDigit(c2) && Character.isLetter(c1)) {
                        return -1;
                    } else {
                        isFirst = false;
                        isSecond = true;
                    }
                }
                if (!Character.isLetterOrDigit(c1) || !Character.isLetterOrDigit(c2)) {
                    isFirst = false;
                }
                if ((isIn || isSecond) && Character.isDigit(c1) && Character.isDigit(c2)) {
                    // 括号内数值按大小排序
                    String tmpStr1 = s1.substring(index1).split("[^0-9]")[0];
                    String tmpStr2 = s2.substring(index1).split("[^0-9]")[0];
                    if (isSecond) {
                        //第二段至多三个数字
                        if (tmpStr1.length() > 3) {
                            tmpStr1 = tmpStr1.substring(0, 3);
                        }
                        if (tmpStr2.length() > 3) {
                            tmpStr2 = tmpStr2.substring(0, 3);
                        }
                    }
                    tmp = Integer.parseInt(tmpStr1) - Integer.parseInt(tmpStr2);

                    if (tmp != 0) {
                        return tmp;
                    }

                    index1 += tmpStr1.length();
                    index2 += tmpStr2.length();
                    isSecond = false;
                } else {
                    if (!isIn) {
                        // 忽略括号外的'-'
                        if (c1 == '-') {
                            index1++;
                            c1 = s1.charAt(index1);
                        }
                        if (c2 == '-') {
                            index2++;
                            c2 = s2.charAt(index2);
                        }
                    }
                    tmp = compareChar(c1, c2);

                    if (tmp != 0) {
                        return tmp;
                    }

                    if (c1 == '(') {
                        isIn = true;
                    }
                    index1++;
                    index2++;
                }
            }

            if ((l1 == index1) && (l2 == index2)) {
                return 0;
            }

            if ((l1 == index1) && (l2 > index2)) {
                return -1;
            }

            if ((l1 > index1) && (l2 == index2)) {
                return 1;
            }

            return 0;
        }
    }

    /**
     * 分类号类
     */
    private class ClassNo implements Comparable {

        /**
         * Field description
         */
        String cnStr = "";

        /**
         * Field description
         */
        int len = 0;

        /**
         * Constructs ...
         *
         * @param classNo Param description
         */
        ClassNo(String classNo) {
            cnStr = classNo;
            len = classNo.length();
        }

        /**
         * 按照分类号规则来比较字符大小
         *
         * @param c1 字符1
         * @param c2 字符2
         * @return c1 &lt; c2 -1 <br> c1 == c2 0 <br> c1 &gt; c2 1
         */
        private int compareChar(char c1, char c2) {
            String str = "-()\"\"=#+:0123456789abcdefghijklmnopqrstuvwxyz.";

            return str.indexOf(c1) - str.indexOf(c2);
        }

        @Override
        public int compareTo(Object o) {
            ClassNo classNo1 = (ClassNo) o;
            String s1 = cnStr.toLowerCase(),
                    s2 = classNo1.cnStr.toLowerCase();

            s1 = s1.replaceAll("[^-()\"\'=#+:\\w\\d]", "");
            s2 = s2.replaceAll("[^-()\"\'=#+:\\w\\d]", "");

            int l1 = s1.length(),
                    l2 = s2.length();

            if (s1.compareTo(s2) == 0) {
                return 0;
            }

            int index = 0, tmp;

            while ((l1 > index) && (l2 > index)) {
                char c1 = s1.charAt(index);
                char c2 = s2.charAt(index);

                tmp = compareChar(c1, c2);

                if (tmp != 0) {
                    return tmp;
                }

                index++;
            }

            if ((l1 == index) && (l2 == index)) {
                return 0;
            }

            if ((l1 == index) && (l2 > index)) {
                return -1;
            }

            if ((l1 > index) && (l2 == index)) {
                return 1;
            }

            return 0;
        }
    }

    /**
     * 卷册年类
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    private class VolumeOrYear implements Comparable {

        /**
         * Field description
         */
        String vyStr = "";

        /**
         * Field description
         */
        int len = 0;

        /**
         * Constructs ...
         *
         * @param authorNo Param description
         */
        VolumeOrYear(String authorNo) {
            vyStr = authorNo;
            len = authorNo.length();
        }

        /**
         * 按照卷册年规则来比较字符大小
         *
         * @param c1 字符1
         * @param c2 字符2
         * @return c1 &lt; c2 -1 <br> c1 == c2 0 <br> c1 &gt; c2 1
         */
        private int compareChar(char c1, char c2) {
            String str = "-.,?0123456789abcdefghijklmnopqrstuvwxyz.";

            return str.indexOf(c1) - str.indexOf(c2);
        }

        @Override
        public int compareTo(Object o) {
            VolumeOrYear vy1 = (VolumeOrYear) o;
            String s1 = vyStr.toLowerCase(),
                    s2 = vy1.vyStr.toLowerCase();

            s1 = s1.replaceAll("[()\\[\\]' ]", "");
            s2 = s2.replaceAll("[()\\[\\]' ]", "");
            s1 = s1.replaceAll("[^-.,?\\d\\w()\\[\\]']", " ");
            s2 = s2.replaceAll("[^-.,?\\d\\w()\\[\\]']", " ");

            int l1 = s1.length(),
                    l2 = s2.length();

            if (s1.compareTo(s2) == 0) {
                return 0;
            }

            String strY = "y";
            if (strY.equals(s1)) {
                return -1;
            }
            if (strY.equals(s2)) {
                return 1;
            }

            int index1 = 0,
                    index2 = 0, tmp;

            while ((l1 > index1) && (l2 > index2)) {
                char c1 = s1.charAt(index1);
                char c2 = s2.charAt(index2);

                if (Character.isDigit(c1) && Character.isDigit(c2)) {

                    // 数值按大小排序
                    String tmpStr1 = s1.substring(index1).split("[^0-9]")[0];
                    String tmpStr2 = s2.substring(index1).split("[^0-9]")[0];

                    tmp = Integer.parseInt(tmpStr1) - Integer.parseInt(tmpStr2);

                    if (tmp != 0) {
                        return tmp;
                    }

                    index1 += tmpStr1.length();
                    index2 += tmpStr2.length();
                } else {
                    tmp = compareChar(c1, c2);

                    if (tmp != 0) {
                        return tmp;
                    }

                    index1++;
                    index2++;
                }
            }

            if ((l1 == index1) && (l2 == index2)) {
                return 0;
            }

            if ((l1 == index1) && (l2 > index2)) {
                return -1;
            }

            if ((l1 > index1) && (l2 == index2)) {
                return 1;
            }

            return 0;
        }
    }
}


