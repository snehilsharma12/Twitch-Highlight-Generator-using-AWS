package com.twitch.bot.db_utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class TwitchAWS_RDSConnection {
    private static final Logger LOG = Logger.getLogger(TwitchAWS_RDSConnection.class.getName());
    Connection rdsConnection;
    protected static final String AND = "AND";
    protected static final String OR = "OR";

    public enum USERS{
        TABLENAME("Users"),
        COLUMN_ID("USER_ID"),
        COLUMN_NAME("NAME"),
        COLUMN_EMAIL("EMAIL"),
        COLUMN_PASSWORD("PASSWORD"),
        COLUMN_PRIMARY("USER_ID"),
        CREATE_TABLE("CREATE TABLE " + TABLENAME.toString() + "(" + COLUMN_ID.toString() + " INTEGER AUTO_INCREMENT not NULL, " + COLUMN_NAME.toString() + " VARCHAR(255), " + COLUMN_EMAIL.toString() + " VARCHAR(255), " + COLUMN_PASSWORD.toString() + " VARCHAR(255), PRIMARY KEY ( " + COLUMN_PRIMARY.toString() + " ))"),
        DROP_TABLE("DROP TABLE " + TABLENAME.toString()),
        SELECT_RECORDS_WITHOUT_WHERE("SELECT {0} FROM " + TABLENAME.toString()),
        SELECT_RECORDS_WITH_WHERE("SELECT {0} FROM " + TABLENAME.toString() + " WHERE {1}"),
        CREATE_RECORDS("INSERT INTO " + TABLENAME.toString() + "( {0} )" + " VALUES " + " ( {1} )"),
        UPDATE_RECORDS("UPDATE " + TABLENAME.toString() + " SET {0}" + " WHERE " + " {1}"),
        DELETE_RECORDS("DELETE FROM " + TABLENAME.toString() + " WHERE " + " {0}");

        String attributeName;

        USERS(String attributeName){
            this.attributeName = attributeName;
        }       
        
        @Override
        public String toString(){
            return this.attributeName;
        }
    }

    public enum TWITCH_STREAMERS{
        TABLENAME("Twitch_Streamers"),
        COLUMN_ID("ID"),
        COLUMN_NAME("NAME"),
        COLUMN_TWITCH_ID("TWITCH_ID"),
        COLUMN_IS_LISTENING_TO_CHANNEL("IS_LISTENING_TO_CHANNEL"),
        COLUMN_PRIMARY("ID"),
        CREATE_TABLE("CREATE TABLE " + TABLENAME.toString() + "(" + COLUMN_ID.toString() + " INTEGER AUTO_INCREMENT not NULL, " + COLUMN_NAME.toString() + " VARCHAR(255), " + COLUMN_TWITCH_ID.toString() + " VARCHAR(255), " + COLUMN_IS_LISTENING_TO_CHANNEL.toString() + " VARCHAR(255), PRIMARY KEY ( " + COLUMN_PRIMARY.toString() + " ))"),
        DROP_TABLE("DROP TABLE " + TABLENAME.toString()),
        SELECT_RECORDS_WITHOUT_WHERE("SELECT {0} FROM " + TABLENAME.toString()),
        SELECT_RECORDS_WITH_WHERE("SELECT {0} FROM " + TABLENAME.toString() + " WHERE {1}"),
        CREATE_RECORDS("INSERT INTO " + TABLENAME.toString() + "( {0} )" + " VALUES " + " ( {1} )"),
        UPDATE_RECORDS("UPDATE " + TABLENAME.toString() + " SET {0}" + " WHERE " + " {1}"),
        DELETE_RECORDS("DELETE FROM " + TABLENAME.toString() + " WHERE " + " {0}");

        String attributeName;

        TWITCH_STREAMERS(String attributeName){
            this.attributeName = attributeName;
        }        

        @Override
        public String toString(){
            return this.attributeName;
        }
    }

    public enum USER_SUBSCRIPTION{
        TABLENAME("User_Subscription"),
        COLUMN_ID("ID"),
        COLUMN_USER_ID("USER_ID"),
        COLUMN_TWITCH_STREAMERS_ID("TWITCH_STREAMERS_ID"),
        COLUMN_PRIMARY("ID"),
        CREATE_TABLE("CREATE TABLE " + TABLENAME.toString() + "(" + COLUMN_ID.toString()  + " INTEGER AUTO_INCREMENT not NULL, " + COLUMN_USER_ID.toString() + " INTEGER not NULL, " + COLUMN_TWITCH_STREAMERS_ID.toString() + " INTEGER not NULL, FOREIGN KEY(" + COLUMN_USER_ID.toString() + ") references " + USERS.TABLENAME.toString() + "(" + USERS.COLUMN_ID.toString() + "),  FOREIGN KEY(" + COLUMN_TWITCH_STREAMERS_ID.toString() + ") references " + TWITCH_STREAMERS.TABLENAME.toString() + "(" + TWITCH_STREAMERS.COLUMN_ID.toString() + ")" + ", PRIMARY KEY ( " + COLUMN_PRIMARY.toString() + " ))"),
        DROP_TABLE("DROP TABLE " + TABLENAME.toString()),
        SELECT_RECORDS_WITHOUT_WHERE("SELECT {0} FROM " + TABLENAME.toString()),
        SELECT_RECORDS_WITH_WHERE("SELECT {0} FROM " + TABLENAME.toString() + " WHERE {1}"),
        CREATE_RECORDS("INSERT INTO " + TABLENAME.toString() + "( {0} )" + " VALUES " + " ( {1} )"),
        UPDATE_RECORDS("UPDATE " + TABLENAME.toString() + " SET {0}" + " WHERE " + " {1}"),
        DELETE_RECORDS("DELETE FROM " + TABLENAME.toString() + " WHERE " + " {0}");

        String attributeName;

        USER_SUBSCRIPTION(String attributeName){
            this.attributeName = attributeName;
        }
        
        @Override
        public String toString(){
            return this.attributeName;
        }
    }

    public TwitchAWS_RDSConnection() throws Exception{
        rdsConnection = make_RDS_JDBC_Connection();
        checkAndCreateTables();
    }

    private Connection make_RDS_JDBC_Connection() {
        if (System.getenv("RDS_HOSTNAME") != null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String dbName = System.getenv("RDS_DB_NAME");
                String userName = System.getenv("RDS_USERNAME");
                String password = System.getenv("RDS_PASSWORD");
                String hostname = System.getenv("RDS_HOSTNAME");
                String port = System.getenv("RDS_PORT");

                checkAndCreateDatabase();

                String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName
                        + "&password=" + password;
                LOG.log(Level.INFO, "Getting remote connection with connection string from environment variables");
                Connection con = DriverManager.getConnection(jdbcUrl);
                LOG.log(Level.INFO, "RDS JDBC Connection Successful");
                return con;
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.INFO, "Exception ::: " + ex.getMessage());
            } catch (SQLException ex) {
                LOG.log(Level.INFO, "Exception ::: " + ex.getMessage());
            }
        }
        return null;
    }

    public void checkAndCreateDatabase(){
        if (System.getenv("RDS_HOSTNAME") != null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String dbName = System.getenv("RDS_DB_NAME");
                String userName = System.getenv("RDS_USERNAME");
                String password = System.getenv("RDS_PASSWORD");
                String hostname = System.getenv("RDS_HOSTNAME");
                String port = System.getenv("RDS_PORT");
                Boolean isDbPresent = false;

                String myConnectionString = "jdbc:mysql://" + hostname + ":" + port + "?" +
                        "useUnicode=yes&characterEncoding=UTF-8";
                Connection conn = DriverManager.getConnection(myConnectionString, userName, password);
                Statement stmt = conn.createStatement();
                stmt.execute("SHOW DATABASES");
                ResultSet rs = stmt.getResultSet();
                while (rs.next()) {
                    if(dbName.equals(rs.getString(1))){
                        isDbPresent = true;
                    }
                }
                if(!isDbPresent){
                    stmt = conn.createStatement();
                    stmt.execute("CREATE DATABASE " + dbName);
                }
                rs.close();
                stmt.close();
                conn.close();
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.INFO, "Exception ::: " + ex.getMessage());
            } catch (SQLException ex) {
                LOG.log(Level.INFO, "Exception ::: " + ex.getMessage());
            }
        }
    }

    private void checkAndCreateTables() throws Exception{
        DatabaseMetaData meta = rdsConnection.getMetaData();
        List<String> tables = getRDSDbTableNames();
        List<String> existingTables = new ArrayList<>();
        for(String tableName: tables){
            ResultSet resultSet = meta.getTables(null, null, tableName, new String[] {"TABLE"});
            if(resultSet.next()){
                existingTables.add(tableName);
            }
        }
        tables.removeAll(existingTables);
        if(!tables.isEmpty()){
            LOG.log(Level.SEVERE, "Tables Not Found In RDS ::: "+ tables.toString());
            for (String tableName : tables) {
                LOG.log(Level.INFO, "Creating Table " + tableName);
                createTable(tableName);
            }
        }
    }

    private void createTable(String tableName) throws Exception{
        Statement statement = rdsConnection.createStatement();
        try{
            statement.executeUpdate(getCreateTableQueryBasedOnTableName(tableName));
        }catch(SQLSyntaxErrorException ex){
            LOG.log(Level.INFO, "Exception ::: " + ex);
            throw ex;
        }
        LOG.log(Level.INFO, "Table {0} Created in RDS", new Object[]{tableName});
    }

    private static String getCreateTableQueryBasedOnTableName(String tableName){
        if(USERS.TABLENAME.toString().equals(tableName)){
            return USERS.CREATE_TABLE.toString();
        }else if(TWITCH_STREAMERS.TABLENAME.toString().equals(tableName)){
            return TWITCH_STREAMERS.CREATE_TABLE.toString();
        }else if(USER_SUBSCRIPTION.TABLENAME.toString().equals(tableName)){
            return USER_SUBSCRIPTION.CREATE_TABLE.toString();
        }
        return null;
    }

    private static List<String> getRDSDbTableNames() {
        List<String> tableNames = new ArrayList<>();
        tableNames.add(USERS.TABLENAME.toString());
        tableNames.add(TWITCH_STREAMERS.TABLENAME.toString());
        tableNames.add(USER_SUBSCRIPTION.TABLENAME.toString());
        return tableNames;
    }

    protected List<String> getAllUsersColumns(){
        List<String> columns = new ArrayList<>();
        USERS[] userVal = USERS.values();
        for(USERS user : userVal){
            if(user.name().startsWith("COLUMN_") && !user.name().equals("COLUMN_PRIMARY")){
                columns.add(user.toString());
            }
        }
        return columns;
    }

    protected List<String> getAllTwitchStreamersColumns(){
        List<String> columns = new ArrayList<>();
        TWITCH_STREAMERS[] twitchStreamersVal = TWITCH_STREAMERS.values();
        for(TWITCH_STREAMERS twitchStream : twitchStreamersVal){
            if(twitchStream.name().startsWith("COLUMN_") && !twitchStream.name().equals("COLUMN_PRIMARY")){
                columns.add(twitchStream.toString());
            }
        }
        return columns;
    }

    protected List<String> getAllUsersSubscriptionColumns(){
        List<String> columns = new ArrayList<>();
        USER_SUBSCRIPTION[] userSubscriptionVal = USER_SUBSCRIPTION.values();
        for(USER_SUBSCRIPTION userSubscription : userSubscriptionVal){
            if(userSubscription.name().startsWith("COLUMN_") && !userSubscription.name().equals("COLUMN_PRIMARY")){
                columns.add(userSubscription.toString());
            }
        }
        return columns;
    }

    protected ResultSet getAllUsersRecord() throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = statement.executeQuery(USERS.SELECT_RECORDS_WITHOUT_WHERE.toString().replace("{0}", "*"));
        return result;
    }

    protected ResultSet getUsersRecordBasedOnCriteria(List<String> columnNames, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        List<String> validColumnNames =  getAllUsersColumns();
        String columnNamesStr = "";
        for(String column : columnNames){
            if(validColumnNames.contains(column)){
                columnNamesStr = buildColumnName(columnNamesStr, column);
            }
        }
        ResultSet result = null;
        if(!columnNames.isEmpty() && columnNamesStr.trim() != "" && whereCondition != null && whereCondition.trim() != ""){
            result = statement.executeQuery(USERS.SELECT_RECORDS_WITH_WHERE.toString().replace("{0}", columnNamesStr).replace("{1}", whereCondition));
        }
        return result;
    }

    protected Integer createUserRecord(String name, String email, String password) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        String values = "";
        if(name != null){
            columnNames = buildColumnName(columnNames, USERS.COLUMN_NAME.toString());
            values = buildColumnName(values, addStringLiteralToString(name));
        }
        if(email != null){
            columnNames = buildColumnName(columnNames, USERS.COLUMN_EMAIL.toString());
            values = buildColumnName(values, addStringLiteralToString(email));
        }
        if(password != null){
            columnNames = buildColumnName(columnNames, USERS.COLUMN_PASSWORD.toString());
            values = buildColumnName(values, addStringLiteralToString(password));
        }
        ResultSet result = null;
        if(columnNames.trim() != ""){
            int affectedRows =  statement.executeUpdate(USERS.CREATE_RECORDS.toString().replace("{0}", columnNames).replace("{1}", values), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Created.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return result.getInt(1);
            }
            else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }
        
        return null;
    }

    protected Boolean updateUsersRecord(JSONObject data, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        if(data.has(USERS.COLUMN_NAME.toString())){
            columnNames = buildColumnName(columnNames, USERS.COLUMN_NAME.toString() + " = " + addStringLiteralToString(data.get(USERS.COLUMN_NAME.toString()).toString()));
        }
        if(data.has(USERS.COLUMN_EMAIL.toString())){
            columnNames = buildColumnName(columnNames, USERS.COLUMN_EMAIL.toString() + " = " + addStringLiteralToString(data.get(USERS.COLUMN_EMAIL.toString()).toString()));
        }
        if(data.has(USERS.COLUMN_PASSWORD.toString())){
            columnNames = buildColumnName(columnNames, USERS.COLUMN_PASSWORD.toString() + " = " + addStringLiteralToString(data.get(USERS.COLUMN_PASSWORD.toString()).toString()));
        }
        if(columnNames.trim() != "" && whereCondition != null && whereCondition.trim() != ""){
            int affectedRows =  statement.executeUpdate(USERS.UPDATE_RECORDS.toString().replace("{0}", columnNames).replace("{1}", whereCondition), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Updated.");
            }
            return true;
        }
        return false;
    }

    protected Boolean deleteUsersRecord(String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = null;
        if(whereCondition != null && whereCondition.trim() != ""){
            int affectedRows =  statement.executeUpdate(USERS.DELETE_RECORDS.toString().replace("{0}", whereCondition), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Deleted.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return true;
            }
            else {
                throw new SQLException("Deleting user failed, no ID obtained.");
            }
        }
        return false;
    }

    protected ResultSet getAllTwitchStreamersRecord() throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = statement.executeQuery(TWITCH_STREAMERS.SELECT_RECORDS_WITHOUT_WHERE.toString().replace("{0}", "*"));
        return result;
    }

    protected ResultSet getTwitchStreamersRecordBasedOnCriteria(List<String> columnNames, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNamesStr = "";
        List<String> validColumnNames =  getAllTwitchStreamersColumns();
        for(String column : columnNames){
            if(validColumnNames.contains(column)){
                columnNamesStr = buildColumnName(columnNamesStr, column);
            }
        }
        ResultSet result = null;
        if(!columnNames.isEmpty() && columnNamesStr.trim() != "" && whereCondition != null && whereCondition.trim() != ""){
            result = statement.executeQuery(TWITCH_STREAMERS.SELECT_RECORDS_WITH_WHERE.toString().replace("{0}", columnNamesStr).replace("{1}", whereCondition));
        }
        return result;
    }

    protected Integer createTwitchStreamersRecord(String name, String twitchId, Boolean isListeningToChannel) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        String values = "";
        if(name != null){
            columnNames = buildColumnName(columnNames, TWITCH_STREAMERS.COLUMN_NAME.toString());
            values = buildColumnName(values, addStringLiteralToString(name));
        }
        if(twitchId != null){
            columnNames = buildColumnName(columnNames, TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString());
            values = buildColumnName(values, addStringLiteralToString(twitchId));
        }
        if(isListeningToChannel != null){
            columnNames = buildColumnName(columnNames, TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString());
            values = buildColumnName(values, addStringLiteralToString(isListeningToChannel.toString()));
        }
        ResultSet result = null;
        if(columnNames.trim() != ""){
            int affectedRows =  statement.executeUpdate(TWITCH_STREAMERS.CREATE_RECORDS.toString().replace("{0}", columnNames).replace("{1}", values), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Created.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return result.getInt(1);
            }
            else {
                throw new SQLException("Creating Twitch Streamers failed, no ID obtained.");
            }
        }
        
        return null;
    }

    protected Boolean updateTwitchStreamersRecord(JSONObject data, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        if(data.has(TWITCH_STREAMERS.COLUMN_NAME.toString())){
            columnNames = buildColumnName(columnNames, TWITCH_STREAMERS.COLUMN_NAME.toString() + " = " + addStringLiteralToString(data.get(TWITCH_STREAMERS.COLUMN_NAME.toString()).toString()));
        }
        if(data.has(TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString())){
            columnNames = buildColumnName(columnNames, TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString() + " = " + addStringLiteralToString(data.get(TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString()).toString()));
        }
        if(data.has(TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString())){
            columnNames = buildColumnName(columnNames, TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString() + " = " + addStringLiteralToString(data.get(TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString()).toString()));
        }
        if(columnNames.trim() != "" && whereCondition != null && whereCondition.trim() != ""){
            int affectedRows =  statement.executeUpdate(TWITCH_STREAMERS.UPDATE_RECORDS.toString().replace("{0}", columnNames).replace("{1}", whereCondition), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Updated.");
            }
            return true;
        }
        return false;
    }

    protected Boolean deleteTwitchStreamersRecord(String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = null;
        if(whereCondition != null && whereCondition.trim() != ""){
            int affectedRows =  statement.executeUpdate(TWITCH_STREAMERS.DELETE_RECORDS.toString().replace("{0}", whereCondition), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Deleted.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return true;
            }
            else {
                throw new SQLException("Deleting Twitch Streamers failed, no ID obtained.");
            }
        }
        return false;
    }

    protected ResultSet getAllUsersSubscriptionRecord() throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = statement.executeQuery(USER_SUBSCRIPTION.SELECT_RECORDS_WITHOUT_WHERE.toString().replace("{0}", "*"));
        return result;
    }

    protected ResultSet getUsersSubscriptionRecordBasedOnCriteria(List<String> columnNames, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNamesStr = "";
        List<String> validColumnNames =  getAllUsersSubscriptionColumns();
        for(String column : columnNames){
            if(validColumnNames.contains(column)){
                columnNamesStr = buildColumnName(columnNamesStr, column);
            }
        }
        ResultSet result = null;
        if(!columnNames.isEmpty() && columnNamesStr.trim() != "" && whereCondition != null && whereCondition.trim() != ""){
            result = statement.executeQuery(USER_SUBSCRIPTION.SELECT_RECORDS_WITH_WHERE.toString().replace("{0}", columnNamesStr).replace("{1}", whereCondition));
        }
        return result;
    }

    protected Integer createUsersSubscriptionRecord(Integer userId, Integer twitchId) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        String values = "";
        if(userId > 0){
            columnNames = buildColumnName(columnNames, USER_SUBSCRIPTION.COLUMN_USER_ID.toString());
            values = buildColumnName(values, userId);
        }
        if(twitchId > 0){
            columnNames = buildColumnName(columnNames, USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString());
            values = buildColumnName(values, twitchId);
        }

        ResultSet result = null;
        if(columnNames.trim() != ""){
            int affectedRows =  statement.executeUpdate(USER_SUBSCRIPTION.CREATE_RECORDS.toString().replace("{0}", columnNames).replace("{1}", values), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Created.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return result.getInt(1);
            }
            else {
                throw new SQLException("Creating User Subscriptions failed, no ID obtained.");
            }
        }
        
        return null;
    }

    protected Boolean updateUsersSubscriptionRecord(JSONObject data, String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        String columnNames = "";
        if(data.has(USER_SUBSCRIPTION.COLUMN_USER_ID.toString()) && Integer.parseInt(data.get(USER_SUBSCRIPTION.COLUMN_USER_ID.toString()).toString()) > 0){
            columnNames = buildColumnName(columnNames, USER_SUBSCRIPTION.COLUMN_USER_ID.toString() + " = " + Integer.parseInt(data.get(USER_SUBSCRIPTION.COLUMN_USER_ID.toString()).toString()));
        }
        if(data.has(USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString()) && Integer.parseInt(data.get(USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString()).toString()) > 0){
            columnNames = buildColumnName(columnNames, USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString() + " = " + Integer.parseInt(data.get(USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString()).toString()));
        }
        if(columnNames.trim() != "" && whereCondition != null && whereCondition.trim() != ""){
            int affectedRows =  statement.executeUpdate(USER_SUBSCRIPTION.UPDATE_RECORDS.toString().replace("{0}", columnNames).replace("{1}", whereCondition), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Updated.");
            }
            return true;
        }
        return false;
    }

    protected Boolean deleteUsersSubscriptionRecord(String whereCondition) throws Exception{
        Statement statement = rdsConnection.createStatement();
        ResultSet result = null;
        if(whereCondition != null && whereCondition.trim() != ""){
            int affectedRows =  statement.executeUpdate(USER_SUBSCRIPTION.DELETE_RECORDS.toString().replace("{0}", whereCondition), Statement.RETURN_GENERATED_KEYS);
           
            if (affectedRows == 0) {
                throw new SQLException("No Rows Deleted.");
            }
            result = statement.getGeneratedKeys();

            if (result.next()) {
                return true;
            }
            else {
                throw new SQLException("Deleting User Subscriptions failed, no ID obtained.");
            }
        }
        return false;
    }

    protected String buildColumnName(String columnNames, Object currentColumnName){
        if(columnNames.trim() != ""){
            columnNames += ", ";
        }
        columnNames += currentColumnName;
        return columnNames;
    }

    public String addStringLiteralToString(String data){
        return "'" + data + "'";
    }
}
