package com.akiban.ais.io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

import com.akiban.ais.metamodel.MetaModel;
import com.akiban.ais.metamodel.ModelObject;
import com.akiban.ais.model.Target;

public class MySQLTarget extends Target
{
    // Target interface

    public void deleteAll() throws Exception
    {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(MetaModel.only().definition(type).cleanupQuery());
        stmt.executeUpdate(MetaModel.only().definition(group).cleanupQuery());
        stmt.executeUpdate(MetaModel.only().definition(table).cleanupQuery());
        stmt.executeUpdate(MetaModel.only().definition(column).cleanupQuery());
        stmt.executeUpdate(MetaModel.only().definition(join).cleanupQuery());
        stmt.executeUpdate(MetaModel.only().definition(joinColumn).cleanupQuery());
        stmt.executeUpdate(MetaModel.only().definition(index).cleanupQuery());
        stmt.executeUpdate(MetaModel.only().definition(indexColumn).cleanupQuery());
        stmt.close();
    }

    @Override
    public void writeCount(int count) throws Exception
    {
    }

    public void close() throws SQLException
    {
        connection.commit();
        connection.close();
    }

    // MySQLTarget interface

    public MySQLTarget(final String server, final String username, final String password)
        throws Exception
    {
        this(server, username, password, 3306);
    }

    public MySQLTarget(final String server, final String username, final String password, int port)
        throws Exception
    {
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://" + server + ":" + port, username, password);
        connection.setAutoCommit(false);
        Statement stmt = connection.createStatement();
        stmt.close();
    }
    
    public void writeType(Map<String, Object> map) throws Exception
    {
        // Don't write the Types table
    }

    // For use by this class

    @Override
    protected final void write(String typename, Map<String, Object> map) throws Exception
    {
        ModelObject modelObject = MetaModel.only().definition(typename);
        PreparedStatement stmt = connection.prepareStatement(modelObject.writeQuery());
        int c = 0;
        for (ModelObject.Attribute attribute : modelObject.attributes()) {
            c++;
            switch (attribute.type()) {
                case INTEGER:
                    bind(stmt, c, (Integer) map.get(attribute.name()));
                    break;
                case LONG:
                    bind(stmt, c, (Long) map.get(attribute.name()));
                    break;
                case STRING:
                    bind(stmt, c, (String) map.get(attribute.name()));
                    break;
                case BOOLEAN:
                    bind(stmt, c, (Boolean) map.get(attribute.name()));
                    break;
            }
        }
        int updateCount = stmt.executeUpdate();
        assert updateCount == 1;
    }

    private void bind(PreparedStatement stmt, int position, String value) throws SQLException
    {
        if (value == null) {
            stmt.setNull(position, Types.VARCHAR);
        } else {
            stmt.setString(position, value);
        }
    }

    private void bind(PreparedStatement stmt, int position, Integer value) throws SQLException
    {
        if (value == null) {
            stmt.setNull(position, Types.INTEGER);
        } else {
            stmt.setInt(position, value);
        }
    }

    private void bind(PreparedStatement stmt, int position, Long value) throws SQLException
    {
        if (value == null) {
            stmt.setNull(position, Types.BIGINT);
        } else {
            stmt.setLong(position, value);
        }
    }

    private void bind(PreparedStatement stmt, int position, Boolean value) throws SQLException
    {
        if (value == null) {
            stmt.setNull(position, Types.INTEGER);
        } else {
            stmt.setInt(position, value ? 1 : 0);
        }
    }

    // State

    private Connection connection;
    private PreparedStatement writeGroupStmt;
    private PreparedStatement writeTableStmt;
    private PreparedStatement writeColumnStmt;
    private PreparedStatement writeJoinStmt;
    private PreparedStatement writeJoinColumnStmt;
    private PreparedStatement writeIndexStmt;
    private PreparedStatement writeIndexColumnStmt;

    private static final String writeGroup =
        "insert into akiba_information_schema.groups(" +
        "    group_name" +
        ") values (?)";

    private static final String writeTable =
        "insert into akiba_information_schema.tables(" +
        "    schema_name, " +
        "    table_name, " +
        "    table_type, " +
        "    table_id, " +
        "    group_name" +
        ") values (?, ?, ?, ?, ?)";

    private static final String writeColumn =
        "insert into akiba_information_schema.columns(" +
        "    schema_name, " +
        "    table_name, " +
        "    column_name, " +
        "    position, " +
        "    type, " +
        "    type_param_1, " +
        "    type_param_2, " +
        "    nullable, " +
        "    initial_autoinc, " +
        "    group_schema_name, " +
        "    group_table_name, " +
        "    group_column_name " +
        ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String writeJoin =
        "insert into akiba_information_schema.joins(" +
        "    join_name, " +
        "    parent_schema_name, " +
        "    parent_table_name, " +
        "    child_schema_name, " +
        "    child_table_name, " +
        "    group_name, " +
        "    join_weight" +
        ") values (?, ?, ?, ?, ?, ?, ?)";

    private static final String writeJoinColumn =
        "insert into akiba_information_schema.join_columns(" +
        "    join_name, " +
        "    parent_schema_name, " +
        "    parent_table_name, " +
        "    parent_column_name, " +
        "    child_schema_name, " +
        "    child_table_name, " +
        "    child_column_name " +
        ") values (?, ?, ?, ?, ?, ?, ?)";

    private static final String writeIndex =
        "insert into akiba_information_schema.indexes(" +
        "    schema_name, " +
        "    table_name, " +
        "    index_name, " +
        "    index_id, " +
        "    table_constraint, " +
        "    is_unique " +
        ") values (?, ?, ?, ?, ?, ?)";

    private static final String writeIndexColumn =
        "insert into akiba_information_schema.index_columns(" +
        "    schema_name, " +
        "    table_name, " +
        "    index_name, " +
        "    column_name, " +
        "    ordinal_position, " +
        "    is_ascending " +
        ") values (?, ?, ?, ?, ?, ?)";
}
