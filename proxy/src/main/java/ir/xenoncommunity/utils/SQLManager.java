package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.Cleanup;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.sql.*;

@Getter public class SQLManager {
    private Connection connection;
    private final Logger logger = XenonCore.instance.getLogger();
    private File databaseFile;
    private String updateCMD;

    public SQLManager(final File databaseFile, final String updateCMD){
        try {
            if(!databaseFile.exists()) Files.createFile(databaseFile.toPath());
            Class.forName("org.sqlite.JDBC");
            this.databaseFile = databaseFile;
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile);
            this.updateCMD = updateCMD;
        } catch(Exception e){
            logger.error(e.getMessage());
        }
        init(updateCMD);
    }

    public void init(final String updateCmd) {
        XenonCore.instance.logdebuginfo("Initializing SQLManager...");
        try {
            if(this.connection == null) {
                XenonCore.instance.logdebuginfo("We don't know why but connection seems to be null.");
                XenonCore.instance.logdebuginfo("trying to set it manually.....");
                this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile);
            }
            @Cleanup final Statement statement = connection.createStatement();
            statement.executeUpdate(updateCmd);
        } catch (final Exception e) {
            logger.error(e.getMessage());
        }
        XenonCore.instance.logdebuginfo("Successfully initialized!");
    }

    public void updateDB(final PreparedStatement preparedStatement) {
        try {
            preparedStatement.executeUpdate();
        } catch (final Exception e) {
            logger.error(e.getMessage());
        }
    }
    public Object getData(final String ket, final String fieldName) {
        try {
            @Cleanup final PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + fieldName + " FROM Players WHERE username = ?");
            preparedStatement.setString(1, ket);
            @Cleanup final ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() ? resultSet.getObject(fieldName) : null;
        } catch (final Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }
//    public List<Map<String, Object>> getAllPlayers() {
//        final List<Map<String, Object>> players = new ArrayList<>();
//        try {
//            @Cleanup final Statement statement = connection.createStatement();
//            @Cleanup final ResultSet resultSet = statement.executeQuery("SELECT * FROM Players");
//            while (resultSet.next()) {
//                Map<String, Object> playerData = new HashMap<>();
//                playerData.put("ip", resultSet.getString("ip"));
//                playerData.put("uuid", resultSet.getString("uuid"));
//                playerData.put("username", resultSet.getString("username"));
//                playerData.put("isWhiteListed", resultSet.getBoolean("isWhiteListed"));
//                playerData.put("violationCount", resultSet.getInt("violationCount"));
//                playerData.put("lastBlacklist", resultSet.getString("lastBlacklist"));
//                players.add(playerData);
//            }
//        } catch (final Exception e) {
//            VelocityMain.getInstance().getLogger().error(e.getMessage());
//        }
//        return players;
//    }
}
