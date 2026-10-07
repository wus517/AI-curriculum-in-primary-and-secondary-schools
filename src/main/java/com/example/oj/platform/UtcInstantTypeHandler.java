package com.example.oj.platform;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

@MappedTypes(Instant.class)
@MappedJdbcTypes(value = JdbcType.TIMESTAMP, includeNullJdbcType = true)
public class UtcInstantTypeHandler extends BaseTypeHandler<Instant> {
    private Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Instant value, JdbcType type) throws SQLException {
        statement.setTimestamp(index, Timestamp.from(value), utc());
    }
    @Override
    public Instant getNullableResult(ResultSet result, String column) throws SQLException {
        Timestamp value = result.getTimestamp(column, utc()); return value == null ? null : value.toInstant();
    }
    @Override
    public Instant getNullableResult(ResultSet result, int column) throws SQLException {
        Timestamp value = result.getTimestamp(column, utc()); return value == null ? null : value.toInstant();
    }
    @Override
    public Instant getNullableResult(CallableStatement result, int column) throws SQLException {
        Timestamp value = result.getTimestamp(column, utc()); return value == null ? null : value.toInstant();
    }
}
