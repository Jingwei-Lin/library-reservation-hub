package com.comp5619.libraryreservationreviewhub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class PostgresTest {
    public static void main(String[] args) {
        // 数据库连接信息
        String url = "jdbc:postgresql://localhost:5432/library_reservation";
        String user = "postgres";
        String password = "REMOVED_DB_PASSWORD";

        try {
            // 加载驱动（新版 JDBC 可以省略）
            Class.forName("org.postgresql.Driver");

            // 获取连接
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ 数据库连接成功！");

            // 创建 Statement
            Statement stmt = conn.createStatement();

            // 创建测试表
            String sql = "CREATE TABLE IF NOT EXISTS test_table (" +
                         "id SERIAL PRIMARY KEY, " +
                         "name VARCHAR(50) NOT NULL" +
                         ")";
            stmt.executeUpdate(sql);
            System.out.println("✅ 表 test_table 创建成功！");

            // 关闭连接
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ 数据库连接或建表失败！");
        }
    }
}
