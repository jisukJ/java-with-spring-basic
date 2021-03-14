import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.*;
import java.util.List;
import javax.sql.*;
public class UserDaoJdbc implements UserDao{
    private RowMapper<User> userMapper=
            new RowMapper<User>() {
                public User mapRow(ResultSet resultSet, int i) throws SQLException {
                    User user=new User();
                    user.setId(resultSet.getString("id"));
                    user.setName(resultSet.getString("name"));
                    user.setPassword(resultSet.getString("password"));
                    user.setEmail(resultSet.getString("email"));
                    user.setLevel(Level.valueOf(resultSet.getInt("level")));
                    user.setLogin(resultSet.getInt("login"));
                    user.setRecommend(resultSet.getInt("recommend"));
                    return user;
                }
            };

    private JdbcTemplate jdbcTemplate;
    public void setJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate=new JdbcTemplate(dataSource);
    }

    public void add(final User user){
        this.jdbcTemplate.update("insert into users(id,name,password,email,level,login,recommend) values(?,?,?,?,?,?,?)",user.getId(),user.getName(),user.getPassword(),user.getEmail(),user.getLevel().intValue(),user.getLogin(),user.getRecommend());
    }

    public User get(String id){
        return this.jdbcTemplate.queryForObject("select * from users where id = ?", new Object[]{id}, this.userMapper);
    }

    public void update(User user) {
        this.jdbcTemplate.update("update users set name=?,password=?,email=?,level=?,login=?,recommend=? where id=?",user.getName(),user.getPassword(),user.getEmail(),user.getLevel().intValue(),user.getLogin(),user.getRecommend(),user.getId());
    }

    public List<User> getAll(){
        return this.jdbcTemplate.query("select * from users order by id", this.userMapper);
    }
    public void deleteAll(){
        this.jdbcTemplate.update("delete from users");
    }

    public int getCount(){
        return this.jdbcTemplate.queryForObject("select count(*) from users",int.class);
    }

}