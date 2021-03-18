import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

public class UserServiceImpl implements UserService {
    UserDao userDao;
    MailSender mailSender;
    UserLevelUpgradePolicy userLevelUpgradePolicy;

    public void setUserDao(UserDao userDao){
        this.userDao=userDao;
    }
    public void setUserLevelUpgradePolicy(UserLevelUpgradePolicy userLevelUpgradePolicy){
        this.userLevelUpgradePolicy=userLevelUpgradePolicy;
    }
    public void setMailSender(MailSender mailSender){
        this.mailSender=mailSender;
    }
    public void add(User user){
        if(user.getLevel()==null) {
            user.setLevel(Level.BASIC);
        }
        userDao.add(user);
    }
    public void upgradeLevels(){
        List<User> users=userDao.getAll();
        for(User user:users){
            if(userLevelUpgradePolicy.canUpgradeLevel(user)){
                upgradeLevel(user);
            }
        }
    }
    public void upgradeLevel(User user){
        user.upgradeLevel();
        userDao.update(user);
        sendUpgradeEmail(user);
    }
    private void sendUpgradeEmail(User user){
        SimpleMailMessage mailMessage=new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setFrom("useradmin@ksug.org");
        mailMessage.setSubject("Upgrade 안내");
        mailMessage.setText("사용자님의 등급이 "+user.getLevel().name());

        this.mailSender.send(mailMessage);
    }
}
