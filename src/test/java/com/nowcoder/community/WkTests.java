package com.nowcoder.community;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args) {
        String cmd="D:\\Developer\\wkhtmltox\\wkhtmltopdf\\bin\\wkhtmltoimage --quality 75 https://www.nowcoder.com D:\\work\\data\\wk-images\\2.png";

        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
