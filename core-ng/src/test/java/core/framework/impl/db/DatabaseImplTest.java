package core.framework.impl.db;

import core.framework.db.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author neo
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseImplTest {
    private DatabaseImpl database;

    @BeforeAll
    void createDatabase() {
        database = new DatabaseImpl("db");
        database.url("jdbc:hsqldb:mem:.;sql.syntax_mys=true");
        database.view(EntityView.class);

        database.execute("CREATE TABLE database_test (id INT PRIMARY KEY, string_field VARCHAR(20), enum_field VARCHAR(10), date_field DATE, date_time_field TIMESTAMP)");
    }

    @AfterAll
    void cleanupDatabase() {
        database.execute("DROP TABLE database_test");
    }

    @BeforeEach
    void truncateTable() {
        database.execute("TRUNCATE TABLE database_test");
    }

    @Test
    void selectOneWithView() {
        insertRow(1, "string1", TestEnum.V1);

        EntityView view = database.selectOne("SELECT string_field as string_label, enum_field as enum_label FROM database_test where id = ?", EntityView.class, 1).get();

        assertEquals("string1", view.stringField);
        assertEquals(TestEnum.V1, view.enumField);
    }

    @Test
    void selectWithView() {
        insertRow(1, "string1", TestEnum.V1);
        insertRow(2, "string2", TestEnum.V2);

        List<EntityView> views = database.select("SELECT string_field as string_label, enum_field as enum_label FROM database_test", EntityView.class);

        assertEquals(2, views.size());
        assertEquals(TestEnum.V1, views.get(0).enumField);
        assertEquals(TestEnum.V2, views.get(1).enumField);
    }

    @Test
    void selectEmptyWithView() {
        List<EntityView> views = database.select("SELECT string_field, enum_field FROM database_test where id = -1", EntityView.class);

        assertTrue(views.isEmpty());
    }

    @Test
    void selectNullInt() {
        Optional<Integer> result = database.selectOne("SELECT max(id) FROM database_test", Integer.class);
        assertFalse(result.isPresent());
    }

    @Test
    void selectNumber() {
        assertEquals(0, database.selectOne("SELECT count(id) FROM database_test", Integer.class).get().intValue());
        assertEquals(0, database.selectOne("SELECT count(id) FROM database_test", Long.class).get().longValue());
        assertEquals(0, database.selectOne("SELECT count(id) FROM database_test", Double.class).get().doubleValue());
        assertEquals(BigDecimal.ZERO, database.selectOne("SELECT count(id) FROM database_test", BigDecimal.class).get());
    }

    @Test
    void selectDate() {
        LocalDate date = LocalDate.of(2017, 11, 22);
        LocalDateTime dateTime = LocalDateTime.of(2017, 11, 22, 13, 0, 0);
        database.execute("INSERT INTO database_test (id, date_field, date_time_field) VALUES (?, ?, ?)", 1, date, dateTime);

        assertEquals(date, database.selectOne("SELECT date_field FROM database_test where id = ?", LocalDate.class, 1).get());
        assertEquals(dateTime, database.selectOne("SELECT date_time_field FROM database_test where id = ?", LocalDateTime.class, 1).get());
        assertEquals(dateTime, database.selectOne("SELECT date_time_field FROM database_test where id = ?", ZonedDateTime.class, 1).get().toLocalDateTime());
    }

    @Test
    void selectString() {
        insertRow(1, "string1", TestEnum.V1);

        Optional<String> result = database.selectOne("SELECT string_field FROM database_test", String.class);
        assertTrue(result.isPresent());
        assertEquals("string1", result.get());
    }

    @Test
    void commitTransaction() {
        try (Transaction transaction = database.beginTransaction()) {
            insertRow(1, "string", TestEnum.V1);
            transaction.commit();
        }

        Optional<EntityView> result = database.selectOne("SELECT string_field, enum_field FROM database_test where id = ?", EntityView.class, 1);
        assertTrue(result.isPresent());
    }

    @Test
    void rollbackTransaction() {
        try (Transaction transaction = database.beginTransaction()) {
            insertRow(1, "string", TestEnum.V1);
            transaction.rollback();
        }

        Optional<EntityView> result = database.selectOne("SELECT string_field, enum_field FROM database_test where id = ?", EntityView.class, 1);
        assertFalse(result.isPresent());
    }

    private void insertRow(int id, String stringField, TestEnum enumField) {
        database.execute("INSERT INTO database_test (id, string_field, enum_field) VALUES (?, ?, ?)", id, stringField, enumField);
    }
}
