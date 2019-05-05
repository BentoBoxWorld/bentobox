package world.bentobox.bentobox.database;

import java.util.Arrays;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.mariadb.MariaDBDatabase;
import world.bentobox.bentobox.database.mongodb.MongoDBDatabase;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

/**
 * @author Poslovitch
 */
public interface DatabaseSetup {

    /**
     * Gets the type of database being used.
     * Currently supported options are YAML, JSON, MYSQL, MARIADB and MONGODB.
     * Default is YAML.
     * @return Database type
     */
    static DatabaseSetup getDatabase() {
        BentoBox plugin = BentoBox.getInstance();
        /*
         * @since 1.5.0
         */
        if (plugin.getSettings().getDatabaseType().equals(DatabaseType.YAML)) {
            plugin.logWarning("YAML database type is deprecated and may not work with all addons.");
        }
        return Arrays.stream(DatabaseType.values())
                .filter(plugin.getSettings().getDatabaseType()::equals)
                .findFirst()
                .map(t -> t.database)
                .orElse(DatabaseType.JSON.database);
    }

    enum DatabaseType {
        YAML(new YamlDatabase()),
        JSON(new JSONDatabase()),
        MYSQL(new MySQLDatabase()),
        /**
         * @since 1.1
         */
        MARIADB(new MariaDBDatabase()),
        MONGODB(new MongoDBDatabase());
        DatabaseSetup database;

        DatabaseType(DatabaseSetup database){
            this.database = database;
        }
    }

    /**
     * Gets a database handler that will store and retrieve classes of type dataObjectClass
     * @param <T> - Class type
     * @param dataObjectClass - class of the object to be stored in the database
     * @return handler for this database object
     */
    <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass);

}