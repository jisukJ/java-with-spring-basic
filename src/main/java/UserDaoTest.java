import java.sql.SQLException;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserDaoTest {
    @Autowired
    private UserDao dao;
    @Autowired
    private DataSource dataSource;
    private User user1;
    private User user2;
    private User user3;

    @Before
    public void setUp() {
        this.user1 = new User("stopstone1", "정지석1", "123123","test1@ksug.org",Level.BASIC,1, 0);
        this.user2 = new User("stopstone2", "정지석2", "1231234","test2@ksug.org",Level.SILVER,55,10);
        this.user3 = new User("stopstone3", "정지석3", "12312345","test3@ksug.org",Level.GOLD,100,40);
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void getUserFailure() throws SQLException {
        dao.deleteAll();
        assertThat(dao.getCount(), is(0));
        dao.get("unknownForError");
    }

    @Test
    public void count() throws SQLException {
        dao.deleteAll();
        assertThat(dao.getCount(), is(0));
        dao.add(user1);
        assertThat(dao.getCount(), is(1));
        dao.add(user2);
        assertThat(dao.getCount(), is(2));
        dao.add(user3);
        assertThat(dao.getCount(), is(3));
    }

    @Test
    public void addAndGet() throws SQLException {
        dao.deleteAll();
        assertThat(dao.getCount(), is(0));

        dao.add(user1);
        assertThat(dao.getCount(), is(1));
        dao.add(user2);
        assertThat(dao.getCount(), is(2));

        User user1get = dao.get(user1.getId());
        checkSameUser(user1, user1get);
        User user2get = dao.get(user2.getId());
        checkSameUser(user2, user2get);
    }

    @Test
    public void getAll() throws SQLException {
        dao.deleteAll();
        List<User> users0 = dao.getAll();
        assertThat(users0.size(), is(0));
        dao.add(user1);
        List<User> users1 = dao.getAll();
        assertThat(users1.size(), is(1));
        checkSameUser(user1, users1.get(0));
        dao.add(user2);
        List<User> users2 = dao.getAll();
        assertThat(users2.size(), is(2));
        checkSameUser(user1, users2.get(0));
        checkSameUser(user2, users2.get(1));
        dao.add(user3);
        List<User> users3 = dao.getAll();
        assertThat(users3.size(), is(3));
        checkSameUser(user1, users3.get(0));
        checkSameUser(user2, users3.get(1));
        checkSameUser(user3, users3.get(2));
    }

    @Test
    public void sqlExceptionTranslate() {
        dao.deleteAll();
        try {
            dao.add(user1);
            dao.add(user1);
        }catch (DuplicateKeyException e){
            SQLException sqlException=(SQLException)e.getRootCause();
            SQLExceptionTranslator set=new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
            assertThat(set.translate(null,null,sqlException), CoreMatchers.<DataAccessException>instanceOf(DuplicateKeyException.class));
        }
    }

    @Test
    public void update(){
        dao.deleteAll();

        dao.add(user1);
        dao.add(user2);

        user1.setName("오민규");
        user1.setPassword("springno6");
        user1.setEmail("test@ksug.org");
        user1.setLevel(Level.GOLD);
        user1.setLogin(1000);
        user1.setRecommend(999);
        dao.update(user1);

        User user1updated=dao.get(user1.getId());
        checkSameUser(user1,user1updated);
        User user2same=dao.get(user2.getId());
        checkSameUser(user2,user2same);
    }

    private void checkSameUser(User user1, User user2) {
        assertThat(user1.getId(), is(user2.getId()));
        assertThat(user1.getName(), is(user2.getName()));
        assertThat(user1.getPassword(), is(user2.getPassword()));
        assertThat(user1.getEmail(),is(user2.getEmail()));
        assertThat(user1.getLevel(),is(user2.getLevel()));
        assertThat(user1.getLogin(),is(user2.getLogin()));
        assertThat(user1.getRecommend(),is(user2.getRecommend()));
    }
}
