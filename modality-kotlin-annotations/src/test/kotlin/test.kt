import com.republicate.modality.kotlin.Entity
import com.republicate.modality.Model
import javax.naming.spi.InitialContextFactory
import javax.sql.DataSource
import org.apache.commons.dbcp2.BasicDataSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

@Entity(name = "book")
class Book {
}

class ModalityAnnotationTests {

    companion object {

        var dataSource : DataSource? = null

        @BeforeAll
        @JvmStatic
        fun initDatabase() {
            dataSource = BasicDataSource().apply {
                setUrl("jdbc:hsqldb:.;hsqldb.sqllog=3");
                setUsername("sa");
                setPassword("");
                val connection = getConnection()
                val statement = connection.createStatement()
                val sql = getResource("bookshelf.sql").readText()
                for (command in sql.split(";")) {
                    if (command.trim().isEmpty()) continue;
                    statement.executeUpdate(command);
                }
                statement.close();
                connection.close();
            }
        }

        @JvmStatic
        fun getResource(name : String) = ModalityAnnotationTests::class.java.getResource(name)
    }
    
    @Test
    fun testBookshelf() {
        val model = Model().apply {
            setDataSource(dataSource)
            initialize(getResource("bookshelf.xml"))
        }
    }
}
