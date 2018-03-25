package com.application.db.DAO.DAOImplementation;

import com.application.db.DatabaseUtil;
import com.application.db.TableNames;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.application.db.TableNames.CALL_TRACE_TABLE;

public class CallTraceDAOImpl {
    private static int currentSelectedThread;

    // public static boolean isTableCreated = false;
    public static boolean isTableCreated() {
        //        System.out.println("starting isTableCreated");
        // if (!isTableCreated) {// No need to call DatabaseUtil method every time. Save time this way.
        //                System.out.println("CallTraceDAOImpl:isTableCreated: " + isTableCreated);
        // isTableCreated = DatabaseUtil.isTableCreated(CALL_TRACE_TABLE);
        //            System.out.println("CallTraceDAOImpl:isTableCreated: " + isTableCreated);
        // }
        //        System.out.println("ending isTableCreated");
        // return isTableCreated;
        return DatabaseUtil.isTableCreated(TableNames.CALL_TRACE_TABLE);
    }

    public static void createTable() {
        //        System.out.println("starting createTable");
        //        System.out.println("CallTraceDAOImpl:createTable: " + isTableCreated());
        if (!isTableCreated()) {
            try (Connection c = DatabaseUtil.getConnection(); Statement ps = c.createStatement()) {
                sql = "CREATE TABLE " + CALL_TRACE_TABLE + " (" +
                        "id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                        "process_id INTEGER not null, " +
                        "thread_id INTEGER, " +
                        "method_id INTEGER, " +
                        "message VARCHAR(20), " +
                        "parameters VARCHAR(800), " +
                        "lockObjId VARCHAR(50), " +
                        // "time_instant VARCHAR(24)" +
                        "time_instant TIMESTAMP" +
                        ")";
                ps.execute(sql);
                System.out.println("** Creating table " + TableNames.CALL_TRACE_TABLE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //        System.out.println("ending createTable");
    }

    public static int insert(List<String> val)
            throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException{

        int autoIncrementedId = -1;
        // int processID = Integer.parseInt(readSoFar.get(0));
        // int threadID = Integer.parseInt(readSoFar.get(1));
        // int methodID = Integer.parseInt(readSoFar.get(2));
        // String eventType = readSoFar.get(3);
        // String parameters = readSoFar.get(4);
        // String time_instant = readSoFar.get(5);

        // TimeStamp       | ProcessID | ThreadID |  EventType |LockObjectID
        // utc time format | 40948     |    9     | Wait-Enter |3986916

        // TimeStamp                | ProcessID | ThreadID | EventType | MethodID  | Arguments
        // 2017-03-31T17:00:19.305Z | 40948     |    9     |   Enter   |     1     |    []
        // TimeStamp                | ProcessID | ThreadID | EventType | MethodID
        // 2017-03-31T17:00:19.305Z | 40948     |    9     |   Enter   |     1
        String time_instant = val.get(0);
        // Instant instant = Instant.parse(time_instant);
        // Timestamp timestamp = Timestamp.from(instant);
        // java.sql.Timestamp sqlTimeStamp = new Timestamp(instant.toEpochMilli());

        Timestamp timestamp = new Timestamp(Long.valueOf(time_instant));
        int processID = Integer.parseInt(val.get(1));
        int threadID = Integer.parseInt(val.get(2));
        String eventType = val.get(3);
        int methodID = 0;
        String parameters = "";
        String lockObjectId = "";

        if (eventType.equalsIgnoreCase("ENTER")) {
            methodID = Integer.parseInt(val.get(4));
            parameters = val.get(5);
            // } else if (eventType.equalsIgnoreCase("EXIT")) {
            //     methodID = Integer.parseInt(readSoFar.get(4));
        } else if (eventType.equalsIgnoreCase("WAIT-ENTER") || eventType.equalsIgnoreCase("WAIT-EXIT") ||
                eventType.equalsIgnoreCase("NOTIFY-ENTER") || eventType.equalsIgnoreCase("NOTIFY-EXIT") ||
                eventType.equalsIgnoreCase("NOTIFYALL-ENTER") || eventType.equalsIgnoreCase("NOTIFYALL-EXIT")) {
            lockObjectId = val.get(4);
        }
        //        System.out.println("starting insert");
        //        System.out.println("CallTraceDAOImpl:insert: " + isTableCreated());

        if (!isTableCreated())
            createTable();
        String sql = null;
        try (Connection c = DatabaseUtil.getConnection(); Statement ps = c.createStatement()) {

            //            System.out.println("value of callTraceTableCreated: " + callTraceTableCreated);
            sql = "INSERT INTO " + CALL_TRACE_TABLE +
                    "(" +
                    "process_id, " +
                    "thread_id, " +
                    "method_id, " +
                    "message, " +
                    "parameters, " +
                    "lockObjId, " +
                    "time_instant" +
                    ")" +
                    " VALUES("+
                    processID  + ", " +
                    threadID   + ", " +
                    methodID   + ", " +
                    "'" + eventType  + "', " +
                    "'" + parameters + "', " +
                    "'" + lockObjectId + "', " +
                    // "'" + time_instant  + "'" +
                    "{ts '" + timestamp + "'}" +
                    ")";

            //            System.out.println("Inserting into call trace the statement: " + sql);
            ps.execute(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                autoIncrementedId = rs.getInt(1);
            }

            // autoIncrementedId = rs.getInt("ID");
            //            System.out.println(TableNames.ELEMENT_TABLE + ": Inserted: " + sql);
        } catch (SQLException e) {
            System.out.println(" Exception caused by: " + sql);
            e.printStackTrace();
        }
        //        System.out.println("ending insert");
        return autoIncrementedId;
    }


    public static void dropTable() {
        //        System.out.println("starting dropTable");
        if (isTableCreated()) {
            try (Connection c = DatabaseUtil.getConnection(); Statement ps = c.createStatement()) {
                String sql= "Drop table " + TableNames.CALL_TRACE_TABLE;
                ps.executeUpdate(sql);
                ps.close();
                System.out.println(">> Dropping table " + TableNames.CALL_TRACE_TABLE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //        System.out.println("ending dropTable");
    }

    static Connection conn;

    static Statement ps;
    static String sql;
    public static ResultSet getWhere(String where) {
        if (isTableCreated()) try {
            conn = DatabaseUtil.getConnection();
            ps = conn.createStatement();
            sql = "SELECT * FROM " + CALL_TRACE_TABLE + " WHERE " + where;
            // System.out.println(">>> we got " + sql);
            ResultSet resultSet = ps.executeQuery(sql);
            //                resultSet.next();
            //                System.out.println(resultSet.getInt("id"));
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Line that threw error: " + sql);
            e.printStackTrace();
        }
        throw new IllegalStateException("Table does not exist. Hence cannot fetch any rows from it.");
    }
    public static List<Integer> getThreadIdsWhere(String where) {
        List<Integer> threadList = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection()) {
            String query = "SELECT DISTINCT(THREAD_ID) FROM " + TableNames.CALL_TRACE_TABLE + " where " + where;
            ResultSet rs = DatabaseUtil.executeQuery(conn, query);
            while (rs.next()) {
                threadList.add(rs.getInt("thread_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return threadList;
    }

    public static List<Integer> getDistinctThreadIds() {
        List<Integer> threadList = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection()) {
            String query = "SELECT DISTINCT(THREAD_ID) FROM " + TableNames.CALL_TRACE_TABLE;
            ResultSet rs = DatabaseUtil.executeQuery(conn, query);
            while (rs.next()) {
                threadList.add(rs.getInt("thread_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return threadList;
    }

    public static int getThreadIdByMethodNameAndPackageName(String methodName, String packageName) {

        String query = "SELECT thread_id " +
                "FROM " + TableNames.CALL_TRACE_TABLE + " " +
                "JOIN " + TableNames.METHOD_DEFINITION_TABLE + " ON " + TableNames.CALL_TRACE_TABLE + ".method_id " +
                "= " +
                TableNames.METHOD_DEFINITION_TABLE + ".id " +
                "AND " + TableNames.METHOD_DEFINITION_TABLE + ".METHOD_NAME = '" + methodName + "' " +
                "AND " + TableNames.METHOD_DEFINITION_TABLE + ".PACKAGE_NAME = '" + packageName + "'";

        try (Connection conn = DatabaseUtil.getConnection()) {
            ResultSet rs = DatabaseUtil.executeQuery(conn, query);
            if (rs.next()) {
                return rs.getInt("thread_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static int getCurrentSelectedThread() {
        return currentSelectedThread;
    }

    public static void setCurrentSelectedThread(int currentSelectedThread) {
        CallTraceDAOImpl.currentSelectedThread = currentSelectedThread;
    }
}