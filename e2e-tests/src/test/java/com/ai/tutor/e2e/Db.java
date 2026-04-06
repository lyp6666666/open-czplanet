package com.ai.tutor.e2e;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Db implements AutoCloseable {

    private final Connection conn;

    public Db(E2eEnv env) throws SQLException {
        this.conn = DriverManager.getConnection(env.mysqlUrl, env.mysqlUser, env.mysqlPassword);
        this.conn.setAutoCommit(true);
    }

    public long insertAndReturnId(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("No generated key");
    }

    public int update(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    public Long queryLong(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long v = rs.getLong(1);
                    if (rs.wasNull()) return null;
                    return v;
                }
            }
        }
        return null;
    }

    public String queryString(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    public List<Long> queryLongList(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                List<Long> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(rs.getLong(1));
                }
                return out;
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}

