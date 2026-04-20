package emu.nebula.database;

import java.util.List;
import java.util.stream.Stream;

import emu.nebula.Config.DatabaseInfo;
import emu.nebula.Config.InternalMongoInfo;
import emu.nebula.Nebula;
import emu.nebula.Nebula.ServerType;
import emu.nebula.database.codecs.*;
import emu.nebula.util.Utils;

import org.bson.codecs.configuration.CodecRegistries;
import org.reflections.Reflections;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;

import de.bwaldvogel.mongo.MongoBackend;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.h2.H2Backend;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import dev.morphia.*;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperators;
import lombok.Getter;

@Getter
public final class DatabaseManager {
    @Getter
    private static MongoServer server;
    private Datastore datastore;

    private static final InsertOneOptions INSERT_OPTIONS = new InsertOneOptions();
    private static final DeleteOptions DELETE_OPTIONS = new DeleteOptions();
    private static final DeleteOptions DELETE_MANY = new DeleteOptions().multi(true);

    public DatabaseManager(DatabaseInfo info, ServerType type) {
        // Variables
        var internalConfig = Nebula.getConfig().getInternalMongoServer();
        String connectionString = info.getConnectionString();

        boolean useMongo = System.getenv("NEBULA_MONGODB_HOST") != null;

        // Start local mongo server
        if (info.isUseInternal() && !useMongo) {
            if (Utils.isPortOpen(internalConfig.getAddress(), internalConfig.getPort())) {
                connectionString = startInternalMongoServer(internalConfig);
                Nebula.getLogger().info("Started local MongoDB server at " + server.getConnectionString());
            } else {
                Nebula.getLogger().warn("Local MongoDB server could not be created because the port is in use.");
            }
        }

        // Initialize
        MongoClient mongoClient = MongoClients.create(connectionString);
        
        // Add our custom fastutil codecs
        var codecProvider = CodecRegistries.fromCodecs(
               new IntSetCodec(), new IntListCodec(), new Int2IntMapCodec(),
               new ItemParamMapCodec(), new String2IntMapCodec(), new BitsetCodec()
        );

        // Set mapper options
        MapperOptions mapperOptions = MapperOptions.builder()
                .storeEmpties(true)
                .storeNulls(false)
                .codecProvider(codecProvider)
                .build();

        // Create data store.
        datastore = Morphia.createDatastore(mongoClient, info.getCollection(), mapperOptions);

        // Map classes
        var entities = new Reflections(Nebula.class.getPackageName())
                .getTypesAnnotatedWith(Entity.class)
                .stream()
                .filter(cls -> {
                    Entity e = cls.getAnnotation(Entity.class);
                    return e != null;
                })
                .toList();

        if (type.runLogin()) {
            // Only map account related entities
            var map = entities.stream().filter(cls -> {
                return cls.getAnnotation(AccountDatabaseOnly.class) != null;
            }).toArray(Class<?>[]::new);

            datastore.getMapper().map(map);
        }
        if (type.runGame()) {
            // Only map game related entities
            var map = entities.stream().filter(cls -> {
                return cls.getAnnotation(AccountDatabaseOnly.class) == null;
            }).toArray(Class<?>[]::new);

            datastore.getMapper().map(map);
        }

        // Ensure indexes
        ensureIndexes();
        
        // Done
        Nebula.getLogger().info("Connected to the MongoDB database at " + connectionString);
    }

    public MongoDatabase getDatabase() {
        return getDatastore().getDatabase();
    }

    private void ensureIndexes() {
        try {
            datastore.ensureIndexes();
        } catch (MongoCommandException exception) {
            Nebula.getLogger().warn("Mongo index error: ", exception);
            // Duplicate index error
            if (exception.getCode() == 85) {
                // Drop all indexes and re add them
                MongoIterable<String> collections = datastore.getDatabase().listCollectionNames();
                for (String name : collections) {
                    datastore.getDatabase().getCollection(name).dropIndexes();
                }
                // Add back indexes
                datastore.ensureIndexes();
            }
        }
    }

    // Database Functions

    public boolean checkIfObjectExists(Class<?> cls, String filter, String value) {
        return getDatastore().find(cls).filter(Filters.eq(filter, value)).count() > 0;
    }

    public <T> T getObjectByUid(Class<T> cls, long uid) {
        return getDatastore().find(cls).filter(Filters.eq("_id", uid)).first();
    }

    public <T> T getObjectByField(Class<T> cls, String filter, Object value) {
        return getDatastore().find(cls).filter(Filters.eq(filter, value)).first();
    }

    public <T> T getObjectByField(Class<T> cls, String filter, long value) {
        return getDatastore().find(cls).filter(Filters.eq(filter, value)).first();
    }
    
    public <T> Stream<T> getObjects(Class<T> cls, String filter, Object value) {
        return getDatastore().find(cls).filter(Filters.eq(filter, value)).stream();
    }

    public <T> Stream<T> getObjects(Class<T> cls, String filter, long value) {
        return getDatastore().find(cls).filter(Filters.eq(filter, value)).stream();
    }

    public <T> Stream<T> getObjects(Class<T> cls) {
        return getDatastore().find(cls).stream();
    }
    
    public <T> List<T> getSortedObjects(Class<T> cls, String filter, int value, String sortBy, int limit) {
        var options = new FindOptions()
                .sort(Sort.descending(sortBy))
                .limit(limit);
        
        return getDatastore()
                .find(cls)
                .filter(Filters.eq(filter, value))
                .iterator(options)
                .toList();
    }

    public <T> void save(T obj) {
        getDatastore().save(obj, INSERT_OPTIONS);
    }

    public <T> boolean delete(T obj) {
        DeleteResult result = getDatastore().delete(obj, DELETE_OPTIONS);
        return result.getDeletedCount() > 0;
    }

    public boolean delete(Class<?> cls, String filter, long uid) {
        DeleteResult result = getDatastore().find(cls).filter(Filters.eq(filter, uid)).delete(DELETE_MANY);
        return result.getDeletedCount() > 0;
    }
    
    public void update(Object obj, int uid, String field, Object item) {
        update(obj, uid, field, item, false);
    }
    
    public void update(Object obj, int uid, String field, Object value, boolean upsert) {
        var opt = new UpdateOptions().upsert(upsert);
        
        getDatastore().find(obj.getClass())
            .filter(Filters.eq("_id", uid))
            .update(opt, UpdateOperators.set(field, value));
    }
    
    // TODO optimize to not require 2 db calls
    public void update(Object obj, int uid, String field, Object value, String field2, Object value2) {
        /*
        getDatastore().find(obj.getClass())
            .filter(Filters.eq("_id", uid))
            .update(UpdateOperators.set(field, value), UpdateOperators.set(field2, value2));
        */
        update(obj, uid, field, value);
        update(obj, uid, field2, value2);
    }
    
    public void updateUnset(Object obj, int uid, String field) {
        var opt = new UpdateOptions().upsert(false);
        
        getDatastore().find(obj.getClass())
            .filter(Filters.eq("_id", uid))
            .update(opt, UpdateOperators.unset(field));
    }
    
    public void updateNested(Object obj, int uid, String filter, int filterId, String field, Object item) {
        var opt = new UpdateOptions().upsert(false);
        
        getDatastore().find(obj.getClass())
            .filter(Filters.eq("_id", uid))
            .filter(Filters.eq(filter, filterId))
            .update(opt, UpdateOperators.set(field, item));
    }
    
    public void addToSet(Object obj, int uid, String field, Object item) {
        var opt = new UpdateOptions().upsert(false);
        
        getDatastore().find(obj.getClass())
            .filter(Filters.eq("_id", uid))
            .update(opt, UpdateOperators.addToSet(field, item));
    }
    
    // Database counter
    
    public synchronized int getNextObjectId(Class<?> c) {
        DatabaseCounter counter = getDatastore().find(DatabaseCounter.class).filter(Filters.eq("_id", c.getSimpleName())).first();
        if (counter == null) {
            counter = new DatabaseCounter(c.getSimpleName());
        }
        try {
            return counter.getNextId();
        } finally {
            getDatastore().save(counter);
        }
    }

    // Internal MongoDB server

    public static String startInternalMongoServer(InternalMongoInfo internalMongo) {
        // Get backend
        MongoBackend backend = null;

        if (internalMongo.filePath != null && internalMongo.filePath.length() > 0) {
            backend = new H2Backend(internalMongo.filePath);
        } else {
            backend = new MemoryBackend();
        }

        // Create the local mongo server and replace the connection string
        server = new MongoServer(backend);

        // Bind to address of it exists
        if (internalMongo.getAddress() != null && internalMongo.getPort() != 0) {
            server.bind(internalMongo.getAddress(), internalMongo.getPort());
        } else {
            server.bind(); // Binds to random port
        }

        return server.getConnectionString();
    }
}
