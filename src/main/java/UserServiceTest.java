import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserServiceTest {
    @Autowired
    private UserDao userDao;
    @Autowired
    UserService userService;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    MailSender mailSender;
    @Autowired
    UserServiceImpl userServiceImpl;
    List<User> users;

    @Before
    public void setUp(){
        users= Arrays.asList(
                new User("bumjin","박범진","p1","test1@ksug.org",Level.BASIC,BasicUserLevelUpgradePolicy.MIN_LOGCOUNT_FOR_SILVER-1,0),
                new User("joytouch","강명성","p2","test2@ksug.org",Level.BASIC,BasicUserLevelUpgradePolicy.MIN_LOGCOUNT_FOR_SILVER,0),
                new User("erwins","신승한","p3","test3@ksug.org",Level.SILVER,60,BasicUserLevelUpgradePolicy.MIN_RECOMMEND_FOR_GOLD-1),
                new User("madnite1","이상호","p4","test4@ksug.org",Level.SILVER,60,BasicUserLevelUpgradePolicy.MIN_RECOMMEND_FOR_GOLD),
                new User("green","오민규","p5","test5@ksug.org",Level.GOLD,100,Integer.MAX_VALUE)
        );
    }

    @Test
    @DirtiesContext
    public void upgradeLevels() throws Exception{
        userDao.deleteAll();
        for(User user:users){
            userDao.add(user);
        }

        MockMailSender mockMailSender=new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);
        userService.upgradeLevels();

        checkLevelUpgraded(users.get(0),false);
        checkLevelUpgraded(users.get(1),true);
        checkLevelUpgraded(users.get(2),false);
        checkLevelUpgraded(users.get(3),true);
        checkLevelUpgraded(users.get(4),false);

        List<String>request=mockMailSender.getRequest();
        assertThat(request.size(),is(2));
        assertThat(request.get(0),is(users.get(1).getEmail()));
        assertThat(request.get(1),is(users.get(3).getEmail()));
    }

    @Test
    public void add(){
        userDao.deleteAll();

        User userWithLevel=users.get(4);
        User userWithoutLevel=users.get(0);
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead=userDao.get(userWithLevel.getId());
        User userWithoutLevelRead=userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel(),is(userWithLevel.getLevel()));
        assertThat(userWithoutLevelRead.getLevel(),is(Level.BASIC));
    }

    @Test
    public void upgradeAllOrNothing() throws Exception{
        TestUserService testUserService=new TestUserService(users.get(3).getId());
        testUserService.setUserDao(userDao);
        testUserService.setMailSender(mailSender);
        testUserService.setUserLevelUpgradePolicy(new BasicUserLevelUpgradePolicy());

        UserServiceTx txUserService=new UserServiceTx();
        txUserService.setTransactionManager(transactionManager);
        txUserService.setUserService(testUserService);
        userDao.deleteAll();
        for(User user:users){
            userDao.add(user);
        }
        try{
            txUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        }catch (TestUserServiceException e){
        }
        checkLevelUpgraded(users.get(1),false);
    }

    private void checkLevelUpgraded(User user,boolean upgraded){
        User userUpdate=userDao.get(user.getId());
        if(upgraded){
            assertThat(userUpdate.getLevel(),is(user.getLevel().nextLevel()));
        }else{
            assertThat(userUpdate.getLevel(),is(user.getLevel()));
        }
    }

    static class TestUserService extends UserServiceImpl {
        private String id;
        private TestUserService(String id){
            this.id=id;
        }
        public void upgradeLevel(User user){
            if(user.getId().equals(this.id)){
                throw new TestUserServiceException();
            }
            super.upgradeLevel(user);
        }
    }

    static class TestUserServiceException extends RuntimeException{
    }
}

