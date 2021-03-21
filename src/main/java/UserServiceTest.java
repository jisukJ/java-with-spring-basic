import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserServiceTest {
    @Autowired
    ApplicationContext context;
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
    public void mockUpgradeLevels() throws Exception{
        UserServiceImpl userServiceImpl=new UserServiceImpl();
        UserDao mockUserDao=mock(UserDao.class);

        when(mockUserDao.getAll()).thenReturn(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MailSender mockMailSender=mock(MailSender.class);
        userServiceImpl.setMailSender(mockMailSender);
        userServiceImpl.setUserLevelUpgradePolicy(new BasicUserLevelUpgradePolicy());
        userServiceImpl.upgradeLevels();

        verify(mockUserDao,times(2)).update(any(User.class));
        verify(mockUserDao).update(users.get(1));
        assertThat(users.get(1).getLevel(),is(Level.SILVER));
        verify(mockUserDao).update(users.get(3));
        assertThat(users.get(3).getLevel(),is(Level.GOLD));

        ArgumentCaptor<SimpleMailMessage>mailMessageArg=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mockMailSender,times(2)).send(mailMessageArg.capture());
        List<SimpleMailMessage>mailMessages=mailMessageArg.getAllValues();
        assertThat(mailMessages.get(0).getTo()[0],is(users.get(1).getEmail()));
        assertThat(mailMessages.get(1).getTo()[0],is(users.get(3).getEmail()));
    }

    @Test
    public void upgradeLevels() throws Exception{
        UserServiceImpl userServiceImpl=new UserServiceImpl();

        MockUserDao mockUserDao=new MockUserDao(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MockMailSender mockMailSender=new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);
        userServiceImpl.setUserLevelUpgradePolicy(new BasicUserLevelUpgradePolicy());
        userServiceImpl.upgradeLevels();

       List<User>updated=mockUserDao.getUpdated();
       assertThat(updated.size(),is(2));
       checkUserAndLevel(updated.get(0),"joytouch",Level.SILVER);
       checkUserAndLevel(updated.get(1),"madnite1",Level.GOLD);

        List<String>request=mockMailSender.getRequest();
        assertThat(request.size(),is(2));
        assertThat(request.get(0),is(users.get(1).getEmail()));
        assertThat(request.get(1),is(users.get(3).getEmail()));
    }

    private void checkUserAndLevel(User updated,String expecetedId,Level expectedLevel){
        assertThat(updated.getId(),is(expecetedId));
        assertThat(updated.getLevel(),is(expectedLevel));
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
    @DirtiesContext
    public void upgradeAllOrNothing() throws Exception{
        TestUserService testUserService=new TestUserService(users.get(3).getId());
        testUserService.setUserDao(userDao);
        testUserService.setMailSender(mailSender);
        testUserService.setUserLevelUpgradePolicy(new BasicUserLevelUpgradePolicy());

        TxProxyFactoryBean txProxyFactoryBean=context.getBean("&userService",TxProxyFactoryBean.class);
        txProxyFactoryBean.setTarget(testUserService);

        UserService txUserService=(UserService) txProxyFactoryBean.getObject();
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

    static class MockUserDao implements UserDao{
        private List<User>users;
        private List<User>updated=new ArrayList<User>();

        private MockUserDao(List<User> users){
            this.users=users;
        }
        public List<User> getUpdated(){
            return this.updated;
        }
        public List<User>getAll(){
            return this.users;
        }
        public void update(User user){
            updated.add(user);
        }
        public void add(User user){
            throw new UnsupportedOperationException();
        }
        public void deleteAll(){
            throw new UnsupportedOperationException();
        }
        public User get(String id){
            throw new UnsupportedOperationException();
        }
        public int getCount(){
            throw new UnsupportedOperationException();
        }

    }

    static class TestUserServiceException extends RuntimeException{
    }
}

